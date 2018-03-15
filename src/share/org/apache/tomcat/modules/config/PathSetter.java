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

package org.apache.tomcat.modules.config;

import org.apache.tomcat.core.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;

import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.log.Log;
import org.apache.tomcat.util.io.FileUtil;
import org.apache.tomcat.util.IntrospectionUtils;

/**
 * Set ( and guess ) the paths to absolute ( and canonical ) directories.
 * This module must be added first ( before even ServerXmlReader ).
 * 
 * If tomcat is embeded _and_ you are sure that all paths you set
 * are OK - you may not need this ( but better to be safe and add it ).
 *
 * You don't have to insert this in server.xml - it's better to add it
 * manually, to be sure it is first.
 *
 * Will set: tomcat.home, CM.home, CM.installDir, CM.workDir, Ctx.absolutePath
 * 
 * ( based on DefaultCMSetter )
 *
 * @author Costin Manolache
 */
public final class PathSetter extends BaseInterceptor {
    private static StringManager sm =
	StringManager.getManager("org.apache.tomcat.resources");

    /** Default work dir, relative to home
     */
    public static final String DEFAULT_WORK_DIR="work";
    
    public PathSetter() {
    }

    /** Adjust context manager paths. This happens before anything
     * 	else. 
     */
    public void addInterceptor(ContextManager cm, Context ctx,
			       BaseInterceptor module)
	throws TomcatException
    {
	if( this != module ) return;

	// Adjust paths in CM
	String home=cm.getHome();
	if( home==null ) {
	    // try system property
	    home=System.getProperty(ContextManager.TOMCAT_HOME);
	}

	// if "home" is not set, guess "install" and use as "home"
	if( home==null ) {
	    home=IntrospectionUtils.guessInstall(
				ContextManager.TOMCAT_INSTALL,
				ContextManager.TOMCAT_HOME,
				"tomcat_core.jar",
				"org/apache/tomcat/core/Request.class");
	}

	if (home != null) {
	    // Make it absolute
	    home=FileUtil.getCanonicalPath( home );
	    cm.setHome( home );
	}
	
	String installDir=cm.getInstallDir();
	// if "install" is not set, guess "install" if not already guessed
	if ( installDir==null ) {
	    installDir=IntrospectionUtils.guessInstall(
				ContextManager.TOMCAT_INSTALL,
				ContextManager.TOMCAT_HOME,
				"tomcat_core.jar",
				"org/apache/tomcat/core/Request.class");
	}
	if( installDir!= null ) {
	    installDir=FileUtil.getCanonicalPath( installDir );
	    cm.setInstallDir( installDir );
	}

	// if only one is set home==installDir

	if( home!=null && installDir == null ) {
	    cm.setInstallDir( home );
	    installDir=home;
	}

	if( home==null && installDir != null ) {
	    cm.setHome( installDir );
	    home=installDir;
	}

	// if neither home or install is set,
	// and no system property, try "."
	if( home==null && installDir==null ) {
	    home=FileUtil.getCanonicalPath( "." );
	    installDir=home;

	    cm.setHome( home );
	    cm.setInstallDir( home );
	}

	System.getProperties().put(ContextManager.TOMCAT_HOME, cm.getHome());
	System.getProperties().put(ContextManager.TOMCAT_INSTALL, cm.getInstallDir());
    }

    /** After server.xml is read - make sure the workDir is absolute,
     *  and all global loggers are set to absolute paths and open.
     */
    public void engineInit( ContextManager cm )
	throws TomcatException
    {
	// Adjust work dir
	String workDir=cm.getWorkDir();
	if( workDir==null ) {
	    workDir= DEFAULT_WORK_DIR;
	}

	if( ! FileUtil.isAbsolute( workDir )) {
	    workDir=FileUtil.
		getCanonicalPath(cm.getHome() + File.separator+
				 workDir);
	}
	cm.setWorkDir( workDir );

	if( debug>0 ||
	    ! cm.getHome().equals( cm.getInstallDir() ) ) {
	    log( "install=" + cm.getInstallDir());
	}
	log( "home=" +  cm.getHome());
	if( debug>0)
	    log( " work=" + workDir);
    }

    public void engineState( ContextManager cm , int state )
	throws TomcatException
    {
	if( state!=ContextManager.STATE_CONFIG ) return;
	Enumeration ctxsE= cm.getContexts();
	while( ctxsE.hasMoreElements() ) {
	    // Set the paths - we do this in advanced, at this stage we should be
	    // ready to do so.
	    Context context=(Context)ctxsE.nextElement();
	    addContext( cm, context);
	}
    }
    
    /** Adjust paths for a context - make the base and all loggers
     *  point to canonical paths.
     */
    public void addContext( ContextManager cm, Context ctx)
	throws TomcatException
    {
	// adjust context paths and loggers

	String docBase=ctx.getDocBase();
	String absPath=ctx.getAbsolutePath();
	if( absPath==null ) {
	    if (FileUtil.isAbsolute( docBase ) )
		absPath=docBase;
	    else
		absPath = cm.getHome() + File.separator + docBase;
	    try {
		absPath = new File(absPath).getCanonicalPath();
	    } catch (IOException npe) {
	    }
	    ctx.setAbsolutePath( absPath );
	}
	if( debug > 0 ) {
	    String h=ctx.getHost();
	    log( "addContext: " + ((h==null) ? "":h) + ":" +
		 ctx.getPath() + " " + docBase + " " + absPath + " " +
		 cm.getHome());
	}
	
    }
}

