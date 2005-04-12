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

package org.eclipse.birt.report.model.activity;

import org.eclipse.birt.report.model.api.activity.ActivityStack;
import org.eclipse.birt.report.model.core.RootElement;
import org.eclipse.birt.report.model.elements.ReportDesign;

/**
 * This class is the base class for commands that modify elements. Provides
 * utility methods needed by derived classes.
 * <p>
 * A command is an application-level operation. A command may translate into any
 * number of activity records, possibly none. Commands are not directly undone
 * or redone; instead the activity records that implement the command are undone
 * and redone.
 * 
 */

public abstract class Command
{

	/**
	 * The root element that provides access to the command stack.
	 */

	protected ReportDesign design = null;

	/**
	 * Optional UI hint. The sender identifies the UI that issued this command.
	 * This allows the UI to ignore notifications for change that it, itself,
	 * made. This behavior is optional, and the need for it depends on the
	 * implementation of any particular part of the UI.
	 */

	protected Object sender = null;

	/**
	 * Constructor.
	 * 
	 * @param theDesign
	 *            the report design
	 */

	public Command( ReportDesign theDesign )
	{
		design = theDesign;
	}

	/**
	 * Returns the activity stack.
	 * 
	 * @return the activity stack.
	 */

	public ActivityStack getActivityStack( )
	{
		return design.getActivityStack( );
	}

	/**
	 * Returns the root element.
	 * 
	 * @return the root element.
	 */

	public RootElement getRootElement( )
	{
		return design;
	}

	/**
	 * Returns the UI element that issued this command.
	 * 
	 * @return the sender.
	 */

	public Object getSender( )
	{
		return sender;
	}

	/**
	 * Sets the optional UI hint for this command.
	 * 
	 * @param hint
	 *            The sender to set.
	 */

	public void setSender( Object hint )
	{
		sender = hint;
	}

}
