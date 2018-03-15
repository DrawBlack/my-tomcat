package params;

import java.io.*;
import java.text.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 *
 */
public class InputStreamParams extends HttpServlet {

    public void service(HttpServletRequest request,
			HttpServletResponse response)
        throws IOException, ServletException
    {
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

	ServletUtil.printBody( request, out );
	out.flush();

	String pi=(String)request.
	    getAttribute( "javax.servlet.include.path_info");
	if( pi==null ) pi="";
	ServletUtil.printParamValues( "", " ]",
				      pi + ":", " = [ ",
				      "", "",
				      " , ",
				      request, out );
    }

}



