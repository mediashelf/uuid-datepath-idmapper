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

import java.net.URI;

import org.akubraproject.map.IdMapper;

/**
 * A trivial implementation of the Akubra IdMapper interface.
 * 
 * This IdMapper is only intended for testing or the most trivial use cases.
 * 
 * @author Edwin Shin
 *
 */
public class TrivialIdMapper implements IdMapper {
	
	private static final String internalScheme = "file";

	/* (non-Javadoc)
	 * @see org.akubraproject.map.IdMapper#getExternalId(java.net.URI)
	 */
	public URI getExternalId(URI internalId) throws NullPointerException {
		String fullPath = internalId.toString().substring(
                internalScheme.length() + 1);
        int i = fullPath.lastIndexOf('/');
        String encodedURI;
        if (i == -1)
            encodedURI = fullPath;
        else
            encodedURI = fullPath.substring(i + 1);
        return URI.create(IdMapperUtil.decode(encodedURI));
	}

	/* (non-Javadoc)
	 * @see org.akubraproject.map.IdMapper#getInternalId(java.net.URI)
	 */
	public URI getInternalId(URI externalId) throws NullPointerException {
		if (externalId == null) {
            throw new NullPointerException();
        }
        String uri = externalId.toString();
        
        return URI.create(internalScheme + ":" + IdMapperUtil.encode(uri));
	}

	/* (non-Javadoc)
	 * @see org.akubraproject.map.IdMapper#getInternalPrefix(java.lang.String)
	 */
	public String getInternalPrefix(String externalPrefix) throws NullPointerException {
		if (externalPrefix == null) {
            throw new NullPointerException();
        }
		// TODO
		return null;
	}
}
