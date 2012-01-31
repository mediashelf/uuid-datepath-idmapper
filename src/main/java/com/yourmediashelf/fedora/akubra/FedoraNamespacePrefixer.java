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


public class FedoraNamespacePrefixer implements IdMapperPrefixer {
    private static final String fedoraRDFNamespace = "info:fedora/";
    
    @Override
    public String getPrefix(String identifier) {
        if (identifier.startsWith(fedoraRDFNamespace)) {
            identifier = identifier.substring(fedoraRDFNamespace.length());
            String[] split = identifier.split(":");
            if (split.length == 2) {
                return split[0];
            }
        }
        // identifier was not recognized as a Fedora PID, so return empty string.
        return "";
    }
}
