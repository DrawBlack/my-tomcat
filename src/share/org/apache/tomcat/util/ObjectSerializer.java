/*
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * [Additional notices, if required by prior licensing conditions]
 *
 */

package org.apache.tomcat.util;

import java.io.*;
import java.util.*;

// from tomcat.session.SessionSerializer

/**
 * Helper for object reloading. Will change the classloader of a
 * serializable object. 
 *
 * The object must be serializable - the caller should do special
 * actions for all non-serializable objects ( like using their
 *  specific protocol ).
 * 
 * Old comment: ( code works now for any object, not only HttpSession )
 *
 * This class manages the serialization of HttpSession object across
 * classloader reloads. It does this by first getting a copy of the 
 * HttpSessions hashtable from the SessionManager and then using a
 * special internal class ObjectInputStream that uses the newly created
 * classloader to de-serialize the sessions. This class is called from
 * within the Handler.handleReload() method. Much of this code
 * is essentially the same as what I put into the Apache JServ release
 * so it is pretty well tested. It also depends on having HttpSession
 * implement the read/writeObject methods properly to only accept 
 * objects that are serializable.
 * 
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author cmanolache@yahoo.com
 */
public final class ObjectSerializer
{
    /**
       This is the method that does the serialization.
    */
    public static final Object doSerialization(ClassLoader cl,
					       Object sessions)
    {
	// get the hashtable of sessions
	try {
	    // writes the session data out, but loses the contexts
	    // because they cannot be serialized
	    ByteArrayOutputStream b = new ByteArrayOutputStream();
	    ObjectOutputStream o = new ObjectOutputStream(b);
	    
	    // write out the hashtable to the OOS
	    o.writeObject(sessions);
	    o.flush();
	    
	    // create the streams to read the sessions back in from.
	    byte data[]=b.toByteArray();
	    ByteArrayInputStream bIn = new ByteArrayInputStream (data);
	    ObjectInputStream oOut= new ACLObjectInputStream(cl, bIn);
			
	    // unserialize the sessions
	    sessions = oOut.readObject();

	    return sessions;
	} catch (Exception e) {
	    // log the error. there shouldn't be one here though.
	    // XXX We should call Logger.log - this is a problem, but
	    // it's better to have a bug ( writing to out instead of log)
	    // than adding dependencies to context.
	    System.out.println( "SessionSerializer: " + e );
	}
	return sessions;
    }
	
    /**
     * When deserializing the sessions during a class
     * loader reload, override the resolveClass() method 
     * so that it uses the AdaptiveClassLoader to deserialize
     * the sessions. This has the benefit of allowing 
     * objects that are only within the ACL's classpath 
     * to be found and deserialized.
     */
    private static final class ACLObjectInputStream extends ObjectInputStream {
	ClassLoader loader;
	
        ACLObjectInputStream(ClassLoader loader, InputStream bIn)
	    throws IOException
	{
            super(bIn);
	    this.loader=loader;
        }

	protected Class resolveClass(ObjectStreamClass v)
            throws IOException, ClassNotFoundException
	{
	    // use our new loader instead of the system loader
            return loader.loadClass(v.getName());
        }
    }
}	
