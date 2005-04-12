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

package org.eclipse.birt.report.model.api.metadata;


/**
 * Represents the definition of argument. The argument definition includes the
 * data type, internal name, and display name.
 */

public interface IArgumentInfo
{

	/**
	 * Returns the internal name for the argument.
	 * 
	 * @return the internal (non-localized) name for the argument
	 */

	public String getName( );

	/**
	 * Returns the display name for the property if the resource key of display
	 * name is defined. Otherwise, return empty string.
	 * 
	 * @return the user-visible, localized display name for the property
	 */

	public String getDisplayName( ); 
	

	/**
	 * Returns the resource key for the display name.
	 * 
	 * @return The display name message ID.
	 */

	public String getDisplayNameKey( );
	
	/**
	 * Returns the script type of this argument.
	 * 
	 * @return the script type to set
	 */

	public String getType( );
	
}
