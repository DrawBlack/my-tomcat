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


package org.apache.tomcat.modules.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import org.apache.tomcat.core.*;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.buf.*;
import org.apache.tomcat.util.http.*;
import org.apache.tomcat.util.net.*;
import org.apache.tomcat.util.net.ServerSocketFactory;
import org.apache.tomcat.util.log.*;
import org.apache.tomcat.util.compat.*;

/** Standalone http.
 *
 *  Connector properties:
 *  - secure - will load a SSL socket factory and act as https server
 *
 *  Properties passed to the net layer:
 *  - timeout
 *  - backlog
 *  - address
 *  - port
 * Thread pool properties:
 *  - minSpareThreads
 *  - maxSpareThreads
 *  - maxThreads
 *  - poolOn
 * Properties for HTTPS:
 *  - keystore - certificates - default to ~/.keystore
 *  - keypass - password
 *  - clientauth - true if the server should authenticate the client using certs
 * Properties for HTTP:
 *  - reportedname - name of server sent back to browser (security purposes)
 */
public class Http10Interceptor extends PoolTcpConnector
    implements  TcpConnectionHandler
{
    private int	timeout = 300000;	// 5 minutes as in Apache HTTPD server
    private String reportedname;
    private int socketCloseDelay=-1;

    public Http10Interceptor() {
	super();
        super.setSoLinger( 100 );
	// defaults:
	this.setPort( 8080 );
    }

    // -------------------- PoolTcpConnector --------------------

    protected void localInit() throws Exception {
	ep.setConnectionHandler( this );
    }

    // -------------------- Attributes --------------------
    public void setTimeout( int timeouts ) {
	timeout = timeouts * 1000;
    }
    public void setReportedname( String reportedName) {
    reportedname = reportedName;
    }

    public void setSocketCloseDelay( int d ) {
        socketCloseDelay=d;
    }

    public void setProperty( String prop, String value ) {
        setAttribute( prop, value );
    }

    // -------------------- Handler implementation --------------------
    public void setServer( Object o ) {
	this.cm=(ContextManager)o;
    }
    
    public Object[] init() {
	Object thData[]=new Object[3];
	HttpRequest reqA=new HttpRequest();
	HttpResponse resA=new HttpResponse();
	if (reportedname != null)
	    resA.setReported(reportedname);
	cm.initRequest( reqA, resA );
	thData[0]=reqA;
	thData[1]=resA;
	thData[2]=null;
	return  thData;
    }

    public void processConnection(TcpConnection connection, Object thData[]) {
	Socket socket=null;
	HttpRequest reqA=null;
	HttpResponse resA=null;

	try {
	    reqA=(HttpRequest)thData[0];
	    resA=(HttpResponse)thData[1];

	    socket=connection.getSocket();
		socket.setSoTimeout(timeout);

	    reqA.setSocket( socket );
	    resA.setSocket( socket );

	    reqA.readNextRequest(resA);
	    if( secure ) {
		reqA.scheme().setString( "https" );
 
 		// Load up the SSLSupport class
		if(sslImplementation != null)
		    reqA.setSSLSupport(sslImplementation.getSSLSupport(socket));
	    }
	    
	    cm.service( reqA, resA );

            // If unread input arrives after the shutdownInput() call
            // below and before or during the socket.close(), an error
            // may be reported to the client.  To help troubleshoot this
            // type of error, provide a configurable delay to give the
            // unread input time to arrive so it can be successfully read
            // and discarded by shutdownInput().
            if( socketCloseDelay >= 0 ) {
                try {
                    Thread.sleep(socketCloseDelay);
                } catch (InterruptedException ie) { /* ignore */ }
            }

        // XXX didn't honor HTTP/1.0 KeepAlive, should be fixed
	    TcpConnection.shutdownInput( socket );
	}
	catch(java.net.SocketException e) {
	    // SocketExceptions are normal
	    log( "SocketException reading request, ignored", null,
		 Log.INFORMATION);
	    log( "SocketException reading request:", e, Log.DEBUG);
	}
	catch (java.io.InterruptedIOException ioe) {
		// We have armed a timeout on read as does apache httpd server.
		// Just to avoid staying with inactive connection
		// BUG#1006
		ioe.printStackTrace();
		log( "Timeout reading request, aborting", ioe, Log.ERROR);
	}
	catch (java.io.IOException e) {
	    // IOExceptions are normal 
	    log( "IOException reading request, ignored", null,
		 Log.INFORMATION);
	    log( "IOException reading request:", e, Log.DEBUG);
	}
	// Future developers: if you discover any other
	// rare-but-nonfatal exceptions, catch them here, and log as
	// above.
	catch (Throwable e) {
	    // any other exception or error is odd. Here we log it
	    // with "ERROR" level, so it will show up even on
	    // less-than-verbose logs.
	    e.printStackTrace();
	    log( "Error reading request, ignored", e, Log.ERROR);
	} 
	finally {
	    // recycle kernel sockets ASAP
        // XXX didn't honor HTTP/1.0 KeepAlive, should be fixed
	    try { if (socket != null) socket.close (); }
	    catch (IOException e) { /* ignore */ }
        }
    }
 
     /**
       getInfo calls for SSL data
 
       @return the requested data
     */
     public Object getInfo( Context ctx, Request request,
 			   int id, String key ) {
       // The following code explicitly assumes that the only
       // attributes hand;ed here are HTTP. If you change that
       // you MUST change the test for sslSupport==null --EKR
 
       HttpRequest httpReq;

       
       try {
 	httpReq=(HttpRequest)request;
       } catch (ClassCastException e){
 	return null;
       }
 
       if(key!=null && httpReq!=null && httpReq.sslSupport!=null){
 	  try {
 	      if(key.equals("javax.servlet.request.cipher_suite"))
 		  return httpReq.sslSupport.getCipherSuite();
 	      if(key.equals("javax.servlet.request.X509Certificate"))
 		  return httpReq.sslSupport.getPeerCertificateChain();
 	  } catch (Exception e){
 	      log("Exception getting SSL attribute " + key,e,Log.WARNING);
 	      return null;
 	  }
       }
       return super.getInfo(ctx,request,id,key);
     }
}

class HttpRequest extends Request {
    Http10 http=new Http10();
    private boolean moreRequests = false;
    Socket socket;
    SSLSupport sslSupport=null;
    
    public HttpRequest() {
        super();

        // recycle these to remove the defaults
        remoteAddrMB.recycle();
        remoteHostMB.recycle();
    }

    public void recycle() {
	super.recycle();
	if( http!=null) http.recycle();
        // recycle these to remove the defaults
        remoteAddrMB.recycle();
        remoteHostMB.recycle();
	sslSupport=null;
    }

    public void setSocket(Socket socket) throws IOException {
	http.setSocket( socket );
	this.socket=socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public int doRead() throws IOException {
	if( available == 0 ) 
	    return -1;
	// #3745
	// if available == -1: unknown length, we'll read until end of stream.
	if( available!= -1 )
	    available--;
	return http.doRead();
    }

    public int doRead(byte[] b, int off, int len) throws IOException {
	if( available == 0 )
	    return -1;
	// if available == -1: unknown length, we'll read until end of stream.
	int rd=http.doRead( b, off, len );
	if( rd==-1) {
	    available=0;
	    return -1;
	}
	if( available!= -1 )
	    available -= rd;
	return rd;
    }
    

    public void readNextRequest(Response response) throws IOException {
	int status=http.processRequestLine( methodMB, uriMB,queryMB, protoMB );
	// XXX remove this after we swich to MB
	// 	method=methodMB.toString();
	// 	requestURI=uriMB.toString();
	// 	queryString=queryMB.toString();
	// 	protocol=protoMB.toString();
	
	if( status > 200 ) {
	    response.setStatus( status );
	    return;
	}

	// for 0.9, we don't have headers!
	if (! protoMB.equals("")) {
	    // all HTTP versions with protocol also have headers
	    // ( 0.9 has no HTTP/0.9 !)
	    status=http.readHeaders( headers  );
	    if( status >200 ) {
		response.setStatus( status );
		return;
	    }
	}

	// XXX detect for real whether or not we have more requests
	// coming
	moreRequests = false;
    }

    // -------------------- override special methods

    public MessageBytes remoteAddr() {
	// WARNING: On some linux configurations, this call may get you in
	// trubles... Big trubles...
	if( remoteAddrMB.isNull() ) {
	    remoteAddrMB.setString(socket.getInetAddress().getHostAddress());
	}
	return remoteAddrMB;
    }

    public MessageBytes remoteHost() {
	if( remoteHostMB.isNull() ) {
	    remoteHostMB.setString( socket.getInetAddress().getHostName() );
	}
	return remoteHostMB;
    }

    public String getLocalHost() {
	InetAddress localAddress = socket.getLocalAddress();
	localHost = localAddress.getHostName();
	return localHost;
    }

    public MessageBytes serverName(){
        if(! serverNameMB.isNull()) return serverNameMB;
        parseHostHeader();
        return serverNameMB;
    }

    public int getServerPort(){
        if(serverPort!=-1) return serverPort;
        parseHostHeader();
        return serverPort;
    }

    protected void parseHostHeader() {
	MessageBytes hH=getMimeHeaders().getValue("host");
        serverPort = socket.getLocalPort();
	if (hH != null) {
	    // XXX use MessageBytes
	    String hostHeader = hH.toString();
	    int i = hostHeader.indexOf(':');
	    if (i > -1) {
		serverNameMB.setString( hostHeader.substring(0,i));
                hostHeader = hostHeader.substring(i+1);
                try{
                    serverPort=Integer.parseInt(hostHeader);
                }catch(NumberFormatException  nfe){
                }
	    }else serverNameMB.setString( hostHeader);
        return;
	}
	if( localHost != null ) {
	    serverNameMB.setString( localHost );
	}
	// default to localhost - and warn
	//	log("No server name, defaulting to localhost");
        serverNameMB.setString( getLocalHost() );
    }
 
    void setSSLSupport(SSLSupport s){
        sslSupport=s;
    }
 
}


class HttpResponse extends  Response {
    Http10 http;
    String reportedname;
    DateFormat dateFormat;
    
    public HttpResponse() {
        super();
    }

    public void init() {
	super.init();
	dateFormat=new SimpleDateFormat(DateTool.RFC1123_PATTERN,
					Locale.US);
	dateFormat.setTimeZone(DateTool.GMT_ZONE);
    }
    
    public void setSocket( Socket s ) {
	http=((HttpRequest)request).http;
    }

    public void recycle() {
	super.recycle();
    }

    public void setReported(String reported) {
        reportedname = reported;
    }

    public void endHeaders()  throws IOException {
	super.endHeaders();
	if(request.protocol().isNull() ||
	   request.protocol().equals("") ) // HTTP/0.9 
	    return;

	http.sendStatus( status, HttpMessages.getMessage( status ));

	// Check if a Date is to be added
	MessageBytes dateH=getMimeHeaders().getValue("Date");
	if( dateH == null ) {
	    // no date header set by user
	    MessageBytes dateHeader=getMimeHeaders().setValue(  "Date" );
	    dateHeader.setTime( System.currentTimeMillis(), dateFormat);
	}

	// return server name (or the reported one)
	if (reportedname == null) {
	    Context ctx = request.getContext();
	    String server = ctx != null ? ctx.getEngineHeader() : 
                ContextManager.TOMCAT_NAME + "/" + ContextManager.TOMCAT_VERSION;    
	    getMimeHeaders().setValue(  "Server" ).setString(server);
	} else {
	    if (reportedname.length() != 0)
		getMimeHeaders().setValue(  "Server" ).setString(reportedname);
	}
	
	http.sendHeaders( getMimeHeaders() );
    }

    public void doWrite( byte buffer[], int pos, int count)
	throws IOException
    {
	http.doWrite( buffer, pos, count);
    }
}
