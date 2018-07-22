package com.fftools.utils;


import java.io.IOException;

/**
 * Specialized IOException that get thrown if SMPT's QUIT command fails.
 *
 * <p>This seems to happen with some version of MS Exchange that
 * doesn't respond with a 221 code immediately.  See <a
 * href="http://nagoya.apache.org/bugzilla/show_bug.cgi?id=5273">Bug
 * report 5273</a>.</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 * @version $Revision: 1.2 $
 */
public class ErrorInQuitException extends IOException {
	
	static final long serialVersionUID = 0;
	
    public ErrorInQuitException(IOException e) {
        super(e.getMessage());
    }

}
