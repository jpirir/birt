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

package org.eclipse.birt.report.model.api;

import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.core.DesignElement;
import org.eclipse.birt.report.model.elements.GraphicMasterPage;
import org.eclipse.birt.report.model.elements.ReportDesign;

/**
 * Represents a graphic master page in the design. A graphic master page
 * describes a physical page "decoration". The decoration can include simple
 * headers and footers, but can also include content within the left and right
 * margins, as well as watermarks under the content area. The page can contain
 * multiple columns. In a multi-column report, the content area is the area
 * inside the margins defined by each column.
 * <p>
 * Note that each page has only one content area, though that content area can
 * be divided into multiple columns. That is, a page has one content area. If a
 * page has multiple columns, the column layout is overlayed on top of the
 * content area.
 */

public class GraphicMasterPageHandle extends MasterPageHandle
{

	/**
	 * Constructs a handle with the given design and the design element. The
	 * application generally does not create handles directly. Instead, it uses
	 * one of the navigation methods available on other element handles.
	 * 
	 * @param design
	 *            the report design
	 * @param element
	 *            the model representation of the element
	 */

	public GraphicMasterPageHandle( ReportDesign design, DesignElement element )
	{
		super( design, element );
	}

	/**
	 * Returns the slot handle for the content. The items in this slot appear on
	 * the page itself, usually as headers, footers, margins, watermarks, etc.
	 * 
	 * @return a handle to the content slot
	 * @see SlotHandle
	 */

	public SlotHandle getContent( )
	{
		return getSlot( GraphicMasterPage.CONTENT_SLOT );
	}

	/**
	 * Returns the number of columns in the report.
	 * 
	 * @return the number of columns in the report
	 */

	public int getColumnCount( )
	{
		return getIntProperty( GraphicMasterPage.COLUMNS_PROP );
	}

	/**
	 * Sets the number of columns in the report.
	 * 
	 * @param count
	 *            the number of columns in the report
	 * @throws SemanticException
	 *             if the property is locked.
	 */

	public void setColumnCount( int count ) throws SemanticException
	{
		setIntProperty( GraphicMasterPage.COLUMNS_PROP, count );
	}

	/**
	 * Returns a handle to work with the the space between columns.
	 * 
	 * @return a DimensionHandle to deal with the space between columns.
	 */

	public DimensionHandle getColumnSpacing( )
	{
		return super
				.getDimensionProperty( GraphicMasterPage.COLUMN_SPACING_PROP );
	}

}