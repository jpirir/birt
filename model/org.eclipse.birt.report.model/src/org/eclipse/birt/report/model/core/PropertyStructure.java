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

package org.eclipse.birt.report.model.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.birt.report.model.api.metadata.IPropertyDefn;
import org.eclipse.birt.report.model.elements.ReportDesign;
import org.eclipse.birt.report.model.metadata.PropertyDefn;
import org.eclipse.birt.report.model.metadata.PropertyType;

/**
 * Base class for structures that store some or all of their properties in a
 * hash table. Such properties can have an "unset" state.
 *  
 */

public abstract class PropertyStructure extends Structure
{

	/**
	 * The Hashmap to store value for non-intrinsic property values. The
	 * contents are of type Object.
	 */

	protected HashMap propValues = new HashMap( );

	/**
	 * Gets the value of the property specified by the property definition. If
	 * the property value is not set, then the default value for it will be
	 * returned.
	 * 
	 * @param design
	 *            report design
	 * 
	 * @param propDefn
	 *            the property definition
	 * 
	 * @return the value of the property, null if no value was set
	 */

	public final Object getProperty( ReportDesign design, PropertyDefn propDefn )
	{
		assert propDefn != null;

		Object value = null;
		if ( propDefn.isIntrinsic( ) )
			value = getIntrinsicProperty( propDefn.getName( ) );
		else
			value = propValues.get( propDefn.getName( ) );

		if ( value == null )
			return propDefn.getDefault( );

		return value;
	}

	/**
	 * Sets the value of the property.
	 * 
	 * @param prop
	 *            the property definition
	 * 
	 * @param value
	 *            the value to set.
	 *  
	 */

	public final void setProperty( PropertyDefn prop, Object value )
	{
		assert prop != null;

		if ( prop.isIntrinsic( ) )
			setIntrinsicProperty( prop.getName( ), value );
		else if ( value == null )
			propValues.remove( prop.getName( ) );
		else
			propValues.put( prop.getName( ), value );
	}

	/**
	 * Makes a copy of this property structure map.
	 * 
	 * @return IStructure of this property.
	 * @throws CloneNotSupportedException
	 *  
	 */

	public Object clone( ) throws CloneNotSupportedException
	{
		PropertyStructure clone = (PropertyStructure) super.clone( );
		clone.propValues = new HashMap();
        
		for ( Iterator iter = propValues.keySet( ).iterator( ); iter.hasNext( ); )
		{
			String memberName = (String) iter.next( );
			IPropertyDefn memberDefn = getDefn( ).getMember( memberName );

			Object value = null;
			if ( memberDefn.getTypeCode( ) == PropertyType.STRUCT_TYPE )
			{
				if ( memberDefn.isList( ) )
				{
					value = cloneStructList( (ArrayList) propValues
							.get( memberName ) );
				}
				else
				{
                    // must be a structure.
                    
					value = ( (Structure) propValues.get( memberName ) ).copy( );
				}
			}
			else
			{
				// Primitive or immutable values

				value = propValues.get( memberName );
			}

			clone.propValues.put( memberName, value );
		}

		return clone;
	}

	/**
	 * Clone the structure list, a list value contains a list of structures.
	 * 
	 * @param list
	 *            The structure list to be cloned.
	 * @return The cloned structure list.
	 */

	private ArrayList cloneStructList( ArrayList list )
	{
		if ( list == null )
			return null;

		ArrayList returnList = new ArrayList( );
		for ( int i = 0; i < list.size( ); i++ )
		{
			Object item = list.get( i );
			if ( item instanceof Structure )
			{
				returnList.add( ( (Structure) item ).copy( ) );
			}
			else
			{
				assert false;
			}
		}
		return returnList;
	}

}