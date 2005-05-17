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

import org.eclipse.birt.report.model.api.elements.structures.ComputedColumn;

/**
 * Represents the handle of computed column. A computed column is a ¡°virtual¡±
 * column produced as an expression of other columns within the data set. It
 * includes the column name and the expression used to define a computed column.
 *  
 */

public class ComputedColumnHandle extends StructureHandle
{

	/**
	 * Constructs the handle of computed column.
	 * 
	 * @param valueHandle
	 *            the value handle for computed column list of one property
	 * @param index
	 *            the position of this computed column in the list
	 */

	public ComputedColumnHandle( SimpleValueHandle valueHandle, int index )
	{
		super( valueHandle, index );
	}

	/**
	 * Returns the column name.
	 * 
	 * @return the column name
     * @deprecated using {@link #getName()} instead.
	 */

	public String getColumnName( )
	{
		return getName();
	}

    /**
     * Returns the column name.
     * 
     * @return the column name
     */

    
    public String getName()
    {
        return getStringProperty( ComputedColumn.NAME_MEMBER );
    }
    
    
	/**
	 * Sets the column name.
	 * 
	 * @param columnName
	 *            the column name to set
     * @deprecated using {@link #setName(String)} instead.
	 */

	public void setColumnName( String columnName )
	{
        setName( columnName );
	}

    /**
     * Sets the column name.
     * @param columnName
     *              the column name to set.
     */
    
    public void setName( String columnName )
    {
    	setPropertySilently( ComputedColumn.NAME_MEMBER, columnName );
    }
    
    
	/**
	 * Returns the expression used to define this computed column.
	 * 
	 * @return the expression used to define this computed column
	 */

	public String getExpression( )
	{
		return getStringProperty( ComputedColumn.EXPRESSION_MEMBER );
	}

	/**
	 * Sets the expression used to define this computed column.
	 * 
	 * @param expression
	 *            the expression to set
	 */

	public void setExpression( String expression )
	{
        setPropertySilently( ComputedColumn.EXPRESSION_MEMBER, expression );
	}
}