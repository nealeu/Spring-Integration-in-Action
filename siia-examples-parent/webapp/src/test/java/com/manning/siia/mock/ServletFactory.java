package com.manning.siia.mock;

import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.HttpRequestHandlerServlet;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Provides ability to create and init(ServletConfig) a DispatcherServlet.
 * 
 * e.g.
 * <pre>
 *     &lt;bean id="servletBean" class="com.manning.siia.http.DispatcherServletFactory" factory-method="getServlet">
        &lt;constructor-arg name="servletName" value="name"/>
        &lt;constructor-arg name="contextPath" value="/webapp"/>
        &lt;constructor-arg name="initParams" >
            &lt;map>
                &lt;entry key="contextConfigLocation"  value="classpath:applicationContext.xml, classpath:applicationContext-TEST.xml"/>
            &lt;/map>
        &lt;/constructor-arg>
    &lt;/bean>
    </pre>

 * @author Neale
 *
 */
public class ServletFactory {
	
	public static HttpServlet getServlet(final String servletName, String contextPath, final Map<String,String> initParams) throws ServletException {
		return getConfiguredServlet(servletName, contextPath, initParams, new DispatcherServlet());
	}

	public static HttpServlet getHandlerServlet(final String servletName, String contextPath, final Map<String,String> initParams) throws ServletException {
		return getConfiguredServlet(servletName, contextPath, initParams, new HttpRequestHandlerServlet());
	}

	private static HttpServlet getConfiguredServlet(final String servletName, String contextPath,
			final Map<String, String> initParams, HttpServlet servlet) throws ServletException {
		
		MockServletContext context = new MockServletContext();
		context.setContextPath(contextPath);
		
		MockServletConfig config = new MockServletConfig(context, servletName);
		for(Entry<String,String> entry : initParams.entrySet()){
			config.addInitParameter(entry.getKey(), entry.getValue());
		}
		
		servlet.init(config);
		return servlet;
	}
	
	
}
