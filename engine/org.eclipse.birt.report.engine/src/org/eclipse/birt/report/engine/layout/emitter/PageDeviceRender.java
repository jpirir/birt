/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.engine.layout.emitter;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.script.IReportContext;
import org.eclipse.birt.report.engine.content.IImageContent;
import org.eclipse.birt.report.engine.content.IReportContent;
import org.eclipse.birt.report.engine.content.IStyle;
import org.eclipse.birt.report.engine.css.engine.StyleConstants;
import org.eclipse.birt.report.engine.css.engine.value.FloatValue;
import org.eclipse.birt.report.engine.emitter.IEmitterServices;
import org.eclipse.birt.report.engine.layout.TextStyle;
import org.eclipse.birt.report.engine.layout.area.IArea;
import org.eclipse.birt.report.engine.layout.area.IAreaVisitor;
import org.eclipse.birt.report.engine.layout.area.IContainerArea;
import org.eclipse.birt.report.engine.layout.area.IImageArea;
import org.eclipse.birt.report.engine.layout.area.ITemplateArea;
import org.eclipse.birt.report.engine.layout.area.ITextArea;
import org.eclipse.birt.report.engine.layout.area.impl.PageArea;
import org.eclipse.birt.report.engine.layout.pdf.font.FontInfo;
import org.eclipse.birt.report.engine.layout.pdf.util.PropertyUtil;
import org.eclipse.birt.report.model.api.IResourceLocator;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.core.IModuleModel;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.CSSValue;

import com.lowagie.text.Image;

public abstract class PageDeviceRender implements IAreaVisitor
{

	/**
	 * The default image folder
	 */
	public static final String IMAGE_FOLDER = "image"; //$NON-NLS-1$

	public static final float LAYOUT_TO_POINT_RATIO = 1 / 1000f;

	public static final int H_TEXT_SPACE = 70;

	public static final int V_TEXT_SPACE = 100;

	protected float scale;

	protected int hTextSpace = 70;

	protected int vTextSpace = 100;

	protected float actualRatio;

	float pageHeight;

	float pageWidth;

	protected IReportRunnable reportRunnable;
	
	protected ReportDesignHandle reportDesign;

	protected IReportContext context;

	protected IEmitterServices services;

	private Stack containerStack = new Stack( );

	protected Logger logger = Logger.getLogger( PageDeviceRender.class
			.getName( ) );

	protected IPageDevice pageDevice;

	protected IPage pageGraphic;

	protected class ContainerPosition
	{
		public float x;
		public float y;

		public ContainerPosition( float x, float y )
		{
			this.x = x;
			this.y = y;
		}
	}

	/**
	 * Gets the output format. always returns "postscript".
	 */
	public abstract String getOutputFormat( );

	public abstract IPageDevice createPageDevice( String title, IReportContext context, IReportContent report )
			throws Exception;

	/**
	 * Creates a document and create a PdfWriter
	 * 
	 * @param rc
	 *            the report content.
	 */
	public void start( IReportContent rc )
	{
		ReportDesignHandle designHandle = rc.getDesign( ).getReportDesign( );
		String title = designHandle.getStringProperty( IModuleModel.TITLE_PROP );
		try
		{
			pageDevice = createPageDevice( title, context, rc );
		}
		catch ( Exception e )
		{
			log( e, Level.SEVERE );
		}
	}

	protected void log( Throwable t, Level level )
	{
		logger.log( level, t.getMessage( ), t );
	}

	/**
	 * Closes the document.
	 * 
	 * @param rc
	 *            the report content.
	 */
	public void end( IReportContent rc )
	{
		try
		{
			pageDevice.close( );
		}
		catch ( Exception e )
		{
			log( e, Level.WARNING );
		}
	}

	public void setTotalPage( ITextArea totalPage )
	{
	}

	public void visitText( ITextArea textArea )
	{
		ContainerPosition curPos = getContainerPosition( );
		float x = curPos.x + getX( textArea );
		float y = curPos.y + getY( textArea );
		drawTextAt( textArea, x, y );
	}

	public void visitImage( IImageArea imageArea )
	{
		drawImage( imageArea );
	}

	public void visitAutoText( ITemplateArea templateArea )
	{
	}

	public void visitContainer( IContainerArea container )
	{
		startContainer( container );
		Iterator iter = container.getChildren( );
		while ( iter.hasNext( ) )
		{
			IArea child = (IArea) iter.next( );
			child.accept( this );
		}
		endContainer( container );
	}
	
	/**
	 * If the container is a PageArea, this method creates a pdf page. If the
	 * container is the other containerAreas, such as TableArea, or just the
	 * border of textArea/imageArea this method draws the border and background
	 * of the given container.
	 * 
	 * @param container
	 *            the ContainerArea specified from layout
	 */
	protected void startContainer( IContainerArea container )
	{
		if ( container instanceof PageArea )
		{
			scale = container.getScale( );
			actualRatio = LAYOUT_TO_POINT_RATIO * scale;
			hTextSpace = (int) ( H_TEXT_SPACE * scale );
			vTextSpace = (int) ( V_TEXT_SPACE * scale );
			newPage( container );
			containerStack.push( new ContainerPosition( 0, 0 ) );
		}
		else
		{
			drawContainer( container );
			ContainerPosition pos;
			if ( !containerStack.isEmpty( ) )
			{
				pos = (ContainerPosition) containerStack.peek( );
				ContainerPosition current = new ContainerPosition( pos.x
						+ getX( container ), pos.y + getY( container ) );
				containerStack.push( current );
			}
			else
			{
				containerStack.push( new ContainerPosition( getX( container ),
						getY( container ) ) );
			}
		}
	}

	/**
	 * This method will be invoked while a containerArea ends.
	 * 
	 * @param container
	 *            the ContainerArea specified from layout
	 */
	protected void endContainer( IContainerArea container )
	{
		if ( container instanceof PageArea )
		{
			pageGraphic.dispose( );
		}
		if ( !containerStack.isEmpty( ) )
		{
			containerStack.pop( );
		}
	}

	/**
	 * Creates a new PDF page
	 * 
	 * @param page
	 *            the PageArea specified from layout
	 */
	protected void newPage( IContainerArea page )
	{
		pageHeight = getHeight( page );
		pageWidth = getWidth( page );

		Color backgroundColor = PropertyUtil.getColor( page.getStyle( )
				.getProperty( StyleConstants.STYLE_BACKGROUND_COLOR ) );
		pageGraphic = pageDevice.newPage( pageWidth, pageHeight,
				backgroundColor );

		// Draws background image for the new page. if the background image is
		// NOT set, draw nothing.
		drawBackgroundImage( page.getStyle( ), 0, 0, pageWidth, pageHeight );
	}

	protected ContainerPosition getContainerPosition( )
	{
		ContainerPosition curPos;
		if ( !containerStack.isEmpty( ) )
			curPos = (ContainerPosition) containerStack.peek( );
		else
			curPos = new ContainerPosition( 0, 0 );
		return curPos;
	}

	/**
	 * draw background image for the container
	 * 
	 * @param containerStyle
	 *            the style of the container we draw background image for
	 * @param startX
	 *            the absolute horizontal position of the container
	 * @param startY
	 *            the absolute vertical position of the container
	 * @param width
	 *            container width
	 * @param height
	 *            container height
	 */
	private void drawBackgroundImage( IStyle containerStyle, float startX,
			float startY, float width, float height )
	{
		String imageUri = PropertyUtil.getBackgroundImage( containerStyle
				.getProperty( StyleConstants.STYLE_BACKGROUND_IMAGE ) );
		if ( imageUri == null )
		{
			return;
		}
		String imageUrl = imageUri;
		if ( reportDesign != null )
		{
			URL url = reportDesign.findResource( imageUri,
					IResourceLocator.IMAGE );
			if ( url != null )
			{
				imageUrl = url.toExternalForm( );
			}
		}

		if ( imageUrl == null || "".equals( imageUrl ) ) //$NON-NLS-1$
		{
			return;
		}

		FloatValue positionValX = (FloatValue) containerStyle
				.getProperty( StyleConstants.STYLE_BACKGROUND_POSITION_X );
		FloatValue positionValY = (FloatValue) containerStyle
				.getProperty( StyleConstants.STYLE_BACKGROUND_POSITION_Y );

		if ( positionValX == null || positionValY == null )
			return;
		boolean xMode, yMode;
		float positionX, positionY;
		if ( positionValX.getPrimitiveType( ) == CSSPrimitiveValue.CSS_PERCENTAGE )
		{
			positionX = PropertyUtil.getPercentageValue( positionValX );
			xMode = true;
		}
		else
		{
			positionX = getPointValue( positionValX );
			xMode = false;
		}
		if ( positionValY.getPrimitiveType( ) == CSSPrimitiveValue.CSS_PERCENTAGE )
		{
			positionY = PropertyUtil.getPercentageValue( positionValY );
			yMode = true;
		}
		else
		{
			positionY = getPointValue( positionValY );
			yMode = false;
		}

		drawBackgroundImage( imageUrl, startX, startY, width, height,
				positionX, positionY, containerStyle.getBackgroundRepeat( ),
				xMode, yMode );
	}

	private class BorderInfo
	{

		public static final int TOP_BORDER = 0;
		public static final int RIGHT_BORDER = 1;
		public static final int BOTTOM_BORDER = 2;
		public static final int LEFT_BORDER = 3;
		public float startX, startY, endX, endY;
		public float borderWidth;
		public Color borderColor;
		public CSSValue borderStyle;
		public int borderType;

		public BorderInfo( float startX, float startY, float endX, float endY,
				float borderWidth, Color borderColor, CSSValue borderStyle,
				int borderType )
		{
			this.startX = startX;
			this.startY = startY;
			this.endX = endX;
			this.endY = endY;
			this.borderWidth = borderWidth;
			this.borderColor = borderColor;
			this.borderStyle = borderStyle;
			this.borderType = borderType;
		}
	}

	/**
	 * Draws a container's border, and its background color/image if there is
	 * any.
	 * 
	 * @param container
	 *            the containerArea whose border and background need to be
	 *            drawed
	 */
	protected void drawContainer( IContainerArea container )
	{
		// get the style of the container
		IStyle style = container.getStyle( );
		if ( null == style )
		{
			return;
		}
		ContainerPosition curPos = getContainerPosition( );
		// content is null means it is the internal line area which has no
		// content mapping, so it has no background/border etc.
		if ( container.getContent( ) != null )
		{
			float layoutX = curPos.x + getX( container );
			float layoutY = curPos.y + getY( container );
			// the container's start position (the left top corner of the
			// container)
			float startX = layoutX;
			float startY = layoutY;

			// the dimension of the container
			float width = getWidth( container );
			float height = getHeight( container );

			// Draws background color for the container, if the backgound
			// color is NOT set, draw nothing.
			Color bc = PropertyUtil.getColor( style
					.getProperty( StyleConstants.STYLE_BACKGROUND_COLOR ) );
			pageGraphic.drawBackgroundColor( bc, startX, startY, width, height );

			// Draws background image for the container. if the background
			// image is NOT set, draw nothing.
			drawBackgroundImage( style, startX, startY, width, height );

			// the width of each border
			float borderTopWidth = getPointValue( style
					.getProperty( StyleConstants.STYLE_BORDER_TOP_WIDTH ) );
			if ( borderTopWidth > 0 )
			{
				Color borderTopColor = PropertyUtil.getColor( style
						.getProperty( StyleConstants.STYLE_BORDER_TOP_COLOR ) );
				drawBorder( new BorderInfo( layoutX, layoutY + borderTopWidth
						/ 2, layoutX + getWidth( container ), layoutY
						+ borderTopWidth / 2, borderTopWidth, borderTopColor,
						style.getProperty( IStyle.STYLE_BORDER_TOP_STYLE ),
						BorderInfo.TOP_BORDER ) );
			}
			float borderLeftWidth = getPointValue( style
					.getProperty( StyleConstants.STYLE_BORDER_LEFT_WIDTH ) );
			if ( borderLeftWidth > 0 )
			{
				Color borderLeftColor = PropertyUtil.getColor( style
						.getProperty( StyleConstants.STYLE_BORDER_LEFT_COLOR ) );
				drawBorder( new BorderInfo( layoutX + borderLeftWidth / 2,
						layoutY, layoutX + borderLeftWidth / 2, layoutY
								+ getHeight( container ), borderLeftWidth,
						borderLeftColor, style
								.getProperty( IStyle.STYLE_BORDER_LEFT_STYLE ),
						BorderInfo.LEFT_BORDER ) );

			}
			float borderBottomWidth = getPointValue( style
					.getProperty( StyleConstants.STYLE_BORDER_BOTTOM_WIDTH ) );
			if ( borderBottomWidth > 0 )
			{
				Color borderBottomColor = PropertyUtil
						.getColor( style
								.getProperty( StyleConstants.STYLE_BORDER_BOTTOM_COLOR ) );
				drawBorder( new BorderInfo( layoutX, layoutY
						+ getHeight( container ) - borderBottomWidth / 2,
						layoutX + getWidth( container ), layoutY
								+ getHeight( container ) - borderBottomWidth
								/ 2, borderBottomWidth, borderBottomColor,
						style.getProperty( IStyle.STYLE_BORDER_BOTTOM_STYLE ),
						BorderInfo.BOTTOM_BORDER ) );
			}
			float borderRightWidth = getPointValue( style
					.getProperty( StyleConstants.STYLE_BORDER_RIGHT_WIDTH ) );
			if ( borderRightWidth > 0 )
			{
				Color borderRightColor = PropertyUtil
						.getColor( style
								.getProperty( StyleConstants.STYLE_BORDER_RIGHT_COLOR ) );
				drawBorder( new BorderInfo( layoutX + getWidth( container )
						- borderRightWidth / 2, layoutY, layoutX
						+ getWidth( container ) - borderRightWidth / 2, layoutY
						+ getHeight( container ), borderRightWidth,
						borderRightColor,
						style.getProperty( IStyle.STYLE_BORDER_RIGHT_STYLE ),
						BorderInfo.RIGHT_BORDER ) );
			}
		}
	}

	/**
	 * Draws a chunk of text at the pdf.
	 * 
	 * @param text
	 *            the textArea to be drawed.
	 * @param textX
	 *            the X position of the textArea relative to current page.
	 * @param textY
	 *            the Y position of the textArea relative to current page.
	 * @param contentByte
	 *            the content byte to draw the text.
	 * @param contentByteHeight
	 *            the height of the content byte.
	 */
	protected void drawTextAt( ITextArea text, float textX, float textY )
	{
		IStyle style = text.getStyle( );
		assert style != null;

		// style.getFontVariant(); small-caps or normal
		float x = textX
				+ convertToPoint( (int) ( text.getFontInfo( ).getFontSize( ) * hTextSpace ) );
		float y = textY
				+ convertToPoint( (int) ( text.getFontInfo( ).getFontSize( ) * vTextSpace ) );
		FontInfo fontInfo = new FontInfo( text.getFontInfo( ) );
		fontInfo.setFontSize( fontInfo.getFontSize( ) * scale );
		float characterSpacing = convertToPoint( PropertyUtil
				.getDimensionValue( style
						.getProperty( StyleConstants.STYLE_LETTER_SPACING ) ) );
		float wordSpacing = convertToPoint( PropertyUtil
				.getDimensionValue( style
						.getProperty( StyleConstants.STYLE_WORD_SPACING ) ) );

		Color color = PropertyUtil.getColor( style
				.getProperty( StyleConstants.STYLE_COLOR ) );

		CSSValue align = text.getStyle( ).getProperty(
				StyleConstants.STYLE_TEXT_ALIGN );

		// draw the overline,throughline or underline for the text if it has
		// any.
		boolean linethrough = IStyle.LINE_THROUGH_VALUE.equals( style
				.getProperty( IStyle.STYLE_TEXT_LINETHROUGH ) );
		boolean overline = IStyle.OVERLINE_VALUE.equals( style
				.getProperty( IStyle.STYLE_TEXT_OVERLINE ) );
		boolean underline = IStyle.UNDERLINE_VALUE.equals( style
				.getProperty( IStyle.STYLE_TEXT_UNDERLINE ) );
		float width = convertToPoint( text.getWidth( ) );
		float height = convertToPoint( text.getHeight( ) );
		pageGraphic.clipSave( );
		pageGraphic.clip( textX, textY, width, height );
		TextStyle textStyle = new TextStyle( fontInfo, characterSpacing,
				wordSpacing, color, linethrough, overline, underline, align );
		drawTextAt( text, x, y, width, height, textStyle );
		pageGraphic.clipRestore( );
	}

	protected void drawTextAt( ITextArea text, float x, float y, float width,
			float height, TextStyle textStyle )
	{
		pageGraphic.drawText( text.getText( ), x, y, width, height,
					textStyle );
	}

	/**
	 * Draws image at the contentByte
	 * 
	 * @param image
	 *            the ImageArea specified from the layout
	 */
	protected void drawImage( IImageArea image )
	{
		// TODO: drawimage
		ContainerPosition curPos = getContainerPosition( );
		float imageX = curPos.x + getX( image );
		float imageY = curPos.y + getY( image );
		IImageContent imageContent = ( (IImageContent) image.getContent( ) );

		InputStream in = null;
		boolean isSvg = false;
		float height = getHeight( image );
		float width = getWidth( image );
		String helpText = imageContent.getHelpText( );
		try
		{
			// lookup the source type of the image area
			String uri = imageContent.getURI( );
			switch ( imageContent.getImageSource( ) )
			{
				case IImageContent.IMAGE_FILE :
				case IImageContent.IMAGE_URL :
					if ( null == uri )
						return;
					if ( uri != null && uri.endsWith( ".svg" ) )
					{
						isSvg = true;
					}
					if ( isSvg )
					{
						pageGraphic.drawImage( transSvgToArray( uri ), imageX,
								imageY, height, width, helpText );
					}
					else
					{
						pageGraphic.drawImage( uri, imageX, imageY, height,
								width, helpText );
					}
					break;
				case IImageContent.IMAGE_NAME :
				case IImageContent.IMAGE_EXPRESSION :
					byte[] data = imageContent.getData( );
					if ( null == data )
						return;
					in = new ByteArrayInputStream( data );
					isSvg = ( ( imageContent.getMIMEType( ) != null ) && imageContent
							.getMIMEType( ).equalsIgnoreCase( "image/svg+xml" ) ) //$NON-NLS-1$
							|| ( ( uri != null ) && uri.toLowerCase( ).endsWith( ".svg" ) ) //$NON-NLS-1$
							|| ( ( imageContent.getExtension( ) != null ) && imageContent
									.getExtension( ).toLowerCase( ).endsWith(
											".svg" ) ); //$NON-NLS-1$
					if ( isSvg ) data = transSvgToArray( in );
					pageGraphic.drawImage( data, imageX, imageY, height,
							width, helpText );
					break;
			}
			if ( in == null )
				return;
		}
		catch ( Throwable t )
		{
			log( t, Level.WARNING );
		}
		finally
		{
			if ( in != null )
			{
				try
				{
					in.close( );
					in = null;
				}
				catch ( IOException e )
				{
					log( e, Level.WARNING );
				}
			}
		}
	}

	private byte[] transSvgToArray( String uri ) throws	IOException
	{
		InputStream in = null;
		in = new URL( uri ).openStream( );
		return transSvgToArray( in );
	}

	private byte[] transSvgToArray( InputStream inputStream )
			throws IOException
	{
		JPEGTranscoder transcoder = new JPEGTranscoder( );
		// set the transcoding hints
		transcoder.addTranscodingHint( JPEGTranscoder.KEY_QUALITY, new Float(
				.8 ) );
		// create the transcoder input
		TranscoderInput input = new TranscoderInput( inputStream );
		// create the transcoder output
		ByteArrayOutputStream ostream = new ByteArrayOutputStream( );
		TranscoderOutput output = new TranscoderOutput( ostream );
		try
		{
			transcoder.transcode( input, output );
		}
		catch ( TranscoderException e )
		{
		}
		// flush the stream
		ostream.flush( );
		// use the outputstream as Image input stream.
		return ostream.toByteArray( );
	}

	/**
	 * Draws the borders of a container.
	 * 
	 * @param borders
	 *            the border info
	 */
	private void drawBorder( BorderInfo border )
	{
		if ( IStyle.DOTTED_VALUE.equals( border.borderStyle ) )
		{
			pageGraphic.drawLine( border.startX, border.startY, border.endX,
					border.endY, border.borderWidth, border.borderColor,
					"dotted" ); //$NON-NLS-1$
			return;
		}
		if ( IStyle.DASHED_VALUE.equals( border.borderStyle ) )
		{
			pageGraphic.drawLine( border.startX, border.startY, border.endX,
					border.endY, border.borderWidth, border.borderColor,
					"dashed" ); //$NON-NLS-1$
			return;
		}
		if ( IStyle.DOUBLE_VALUE.equals( border.borderStyle ) )
		{
			float outerBorderWidth = border.borderWidth / 3;
			float innerBorderWidth = border.borderWidth / 3;

			switch ( border.borderType )
			{
				case BorderInfo.TOP_BORDER :
					pageGraphic.drawLine( border.startX, border.startY
							- border.borderWidth / 2 + outerBorderWidth / 2,
							border.endX, border.endY - border.borderWidth / 2
									+ outerBorderWidth / 2, outerBorderWidth,
							border.borderColor, "solid" ); //$NON-NLS-1$
					pageGraphic.drawLine( border.startX + 2
							* border.borderWidth / 3, border.startY
							+ border.borderWidth / 2 - innerBorderWidth / 2,
							border.endX - 2 * border.borderWidth / 3,
							border.endY + border.borderWidth / 2
									- innerBorderWidth / 2, innerBorderWidth,
							border.borderColor, "solid" ); //$NON-NLS-1$
					return;
				case BorderInfo.RIGHT_BORDER :
					pageGraphic.drawLine( border.startX + border.borderWidth
							/ 2 - outerBorderWidth / 2, border.startY,
							border.endX + border.borderWidth / 2
									- outerBorderWidth / 2, border.endY,
							outerBorderWidth, border.borderColor, "solid" ); //$NON-NLS-1$
					pageGraphic.drawLine( border.startX - border.borderWidth
							/ 2 + innerBorderWidth / 2, border.startY + 2
							* border.borderWidth / 3, border.endX
							- border.borderWidth / 2 + innerBorderWidth / 2,
							border.endY - 2 * border.borderWidth / 3,
							innerBorderWidth, border.borderColor, "solid" ); //$NON-NLS-1$
					return;
				case BorderInfo.BOTTOM_BORDER :
					pageGraphic.drawLine( border.startX, border.startY
							+ border.borderWidth / 2 - outerBorderWidth / 2,
							border.endX, border.endY + border.borderWidth / 2
									- outerBorderWidth / 2, outerBorderWidth,
							border.borderColor, "solid" ); //$NON-NLS-1$
					pageGraphic.drawLine( border.startX + 2
							* border.borderWidth / 3, border.startY
							- border.borderWidth / 2 + innerBorderWidth / 2,
							border.endX - 2 * border.borderWidth / 3,
							border.endY - border.borderWidth / 2
									+ innerBorderWidth / 2, innerBorderWidth,
							border.borderColor, "solid" ); //$NON-NLS-1$
					return;
				case BorderInfo.LEFT_BORDER :
					pageGraphic.drawLine( border.startX - border.borderWidth
							/ 2 + outerBorderWidth / 2, border.startY,
							border.endX - border.borderWidth / 2
									+ outerBorderWidth / 2, border.endY,
							outerBorderWidth, border.borderColor, "solid" ); //$NON-NLS-1$
					pageGraphic.drawLine( border.startX + border.borderWidth
							/ 2 - innerBorderWidth / 2, border.startY + 2
							* border.borderWidth / 3, border.endX
							+ border.borderWidth / 2 - innerBorderWidth / 2,
							border.endY - 2 * border.borderWidth / 3,
							innerBorderWidth, border.borderColor, "solid" ); //$NON-NLS-1$
					return;
			}
		}
		pageGraphic.drawLine( border.startX, border.startY, border.endX,
				border.endY, border.borderWidth, border.borderColor, "solid" ); //$NON-NLS-1$
	}

	/**
	 * Draws the backgound image at the contentByteUnder of the pdf with the
	 * given offset
	 * 
	 * @param imageURI
	 *            the URI referring the image
	 * @param x
	 *            the start X coordinate at the pdf where the image is
	 *            positioned
	 * @param y
	 *            the start Y coordinate at the pdf where the image is
	 *            positioned
	 * @param width
	 *            the width of the background dimension
	 * @param height
	 *            the height of the background dimension
	 * @param positionX
	 *            the offset X percentage relating to start X
	 * @param positionY
	 *            the offset Y percentage relating to start Y
	 * @param repeat
	 *            the background-repeat property
	 * @param xMode
	 *            whether the horizontal position is a percentage value or not
	 * @param yMode
	 *            whether the vertical position is a percentage value or not
	 */
	private void drawBackgroundImage( String imageURI, float x, float y,
			float width, float height, float positionX, float positionY,
			String repeat, boolean xMode, boolean yMode )
	{
		// the image URI is empty, ignore it.
		if ( null == imageURI )
		{
			return;
		}
		String imageUrl = getImageUrl( imageURI );
		if ( imageUrl == null || "".equals( imageUrl ) ) //$NON-NLS-1$
		{
			return;
		}

		// the background-repeat property is empty, use "repeat".
		if ( null == repeat )
		{
			repeat = "repeat"; //$NON-NLS-1$
		}

		Image img = null;
		try
		{
			img = Image.getInstance( imageUrl );
			float absPosX, absPosY;
			if ( xMode )
			{
				absPosX = ( width - img.scaledWidth( ) ) * positionX;
			}
			else
			{
				absPosX = positionX;
			}
			if ( yMode )
			{
				absPosY = ( height - img.scaledHeight( ) ) * positionY;
			}
			else
			{
				absPosY = positionY;
			}
			pageGraphic.drawBackgroundImage( x, y, width, height, repeat,
					imageUrl, absPosX, absPosY );
		}
		catch ( Exception e )
		{
			log( e, Level.WARNING );
		}
	}

	private String getImageUrl( String imageUri )
	{
		String imageUrl = imageUri;
		if ( reportDesign != null )
		{
			URL url = reportDesign.findResource( imageUri,
					IResourceLocator.IMAGE );
			if ( url != null )
			{
				imageUrl = url.toExternalForm( );
			}
		}
		return imageUrl;
	}

	protected float getX( IArea area )
	{
		return convertToPoint( area.getX( ) );
	}

	protected float getY( IArea area )
	{
		return convertToPoint( area.getY( ) );
	}

	protected float getWidth( IArea area )
	{
		return convertToPoint( area.getWidth( ) );
	}

	protected float getHeight( IArea area )
	{
		return convertToPoint( area.getHeight( ) );
	}

	protected float convertToPoint( float value )
	{
		return value * actualRatio;
	}

	private float getPointValue( CSSValue cssValue )
	{
		return convertToPoint( PropertyUtil.getDimensionValue( cssValue ) );
	}
}
