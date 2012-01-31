/**
 * Copyright (C) 2012 MediaShelf <http://www.yourmediashelf.com/>
 *
 * This file is part of uuid-datepath-idmapper.
 *
 * uuid-datepath-idmapper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * uuid-datepath-idmapper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with uuid-datepath-idmapper.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.yourmediashelf.fedora.akubra;

/**
 * 
 * 
 * @author Edwin Shin
 *
 */
public interface IdMapperPrefixer {
    /**
     * Get the prefix for the supplied identifier. This method must not return 
     * <code>null</code>. If there is no prefix for the supplied identifier, the
     * return value must be the empty string ("").
     * 
     * @param identifier the identifier
     * @return the prefix or the empty string.
     */
    public String getPrefix(String identifier);
}
