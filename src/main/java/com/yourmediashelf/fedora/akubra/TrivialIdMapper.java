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
		
		return null;
	}
}
