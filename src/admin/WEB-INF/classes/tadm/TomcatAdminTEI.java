package tadm;
import java.util.Vector;
import java.util.Enumeration;
import java.io.File;
import java.net.URL;
import javax.servlet.http.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import org.apache.tomcat.core.Request;
import org.apache.tomcat.core.Context;
import org.apache.tomcat.core.ContextManager;


import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

public class TomcatAdminTEI extends TagExtraInfo {

    public VariableInfo[] getVariableInfo(TagData data) {
	return (new VariableInfo[] {
	    new VariableInfo("cm", "org.apache.tomcat.core.ContextManager",
			     true,  VariableInfo.AT_BEGIN),
	    new VariableInfo("ctx", "org.apache.tomcat.core.Context",
			     true,  VariableInfo.AT_BEGIN),
	    new VariableInfo("module",
			     "org.apache.tomcat.core.BaseInterceptor",
			     true,  VariableInfo.AT_BEGIN)
	});

    }


}
