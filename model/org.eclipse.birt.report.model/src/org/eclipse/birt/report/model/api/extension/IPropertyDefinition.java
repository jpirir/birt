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

package org.eclipse.birt.report.model.api.extension;

import java.util.List;

/**
 * Defines a property or structure member provided by an peer extension. Most
 * fields are optional except for type and internal name.
 */

public interface IPropertyDefinition
{

	/**
	 * Returns the resource key for display name of the property group. Property
	 * groups are used in the generic property sheet to organize properties. If
	 * the resource key is null, then no group is used.
	 * 
	 * @return the optional resource key for property group name
	 */

	String getGroupNameID( );

	/**
	 * Returns the internal name of the property. This is a non-localized,
	 * unique name used in the get/set property methods. It is required. BIRT
	 * encourages names that match the BIRT property syntax: propName, so that
	 * the properties are easy to use in scripts.
	 * 
	 * @return the internal property name
	 */

	String getName( );

	/**
	 * Returns the resource key for the localized display name of the property.
	 * This is the name that appears in the property sheet UI. It is optional.
	 * If omitted, the internal name will be displayed instead.
	 * 
	 * @return the optional resource key for the localized display name of the
	 *         property
	 */

	String getDisplayNameID( );

	/**
	 * Returns the property type using one of the types defined in the
	 * {@link org.eclipse.birt.report.model.metadata.PropertyType}class. It is
	 * required. If the model does not provide a suitable type, then either map
	 * the property to one of the supported types, or don't expose it though the
	 * generic property mechanism.
	 * 
	 * @return the property type using one of the model's types
	 */

	int getType( );

	/**
	 * Returns whether this property represents a list of properties instead of
	 * a single property.
	 * 
	 * @return true if the property is a list, false if not
	 */

	boolean isList( );

	/**
	 * Returns a list of choices if the property is a choice (type is
	 * CHOICE_TYPE). Should return null for non-choice properties.
	 * 
	 * @return a list of {@link IChoiceDefinition}objects
	 */

	List getChoices( );

	/**
	 * Returns a list of member definitions if the property is a structure (type
	 * is TBD). Should return null for non-structure properties.
	 * 
	 * @return the list of members as a collection of IPropertyDefn objects
	 */

	List getMembers( );

	/**
	 * Returns the default value for the property. Needed only if the element
	 * supports styles or inheritance. Not needed otherwise. No default is
	 * needed for a structure or list property.
	 * 
	 * @return the default value of the property
	 */

	Object getDefaultValue( );
}