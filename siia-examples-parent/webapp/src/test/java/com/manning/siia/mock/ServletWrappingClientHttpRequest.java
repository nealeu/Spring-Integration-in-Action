package com.manning.siia.mock;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartResolver;

/**
 * {@link ClientHttpRequest} implementation that wraps {@link HttpServlet}
 *
 * @author Neale Upstone
 */
public final class ServletWrappingClientHttpRequest extends AbstractClientHttpRequest {

	private HttpServlet servlet;
	private URI uri;
	private HttpMethod method;


	public ServletWrappingClientHttpRequest(URI uri, HttpMethod httpMethod, HttpServlet servlet) {
		this.uri = uri;
		this.method = httpMethod;
		this.servlet = servlet;
	}


	public HttpMethod getMethod() {
		return method;
	}

	public URI getURI() {
		return uri;
	}

	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		
		HttpServletRequest request = getRequest(headers, bufferedOutput);

		MockHttpServletResponse response = new MockHttpServletResponse();
		try {
			servlet.service(request, response);
		}
		catch (ServletException e) {
			// relatively dumb but should do the job
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace(response.getWriter());
			e.printStackTrace();
		}
		
		return new ServletResponseWrappingClientHttpResponse(response);
	}

	private MockHttpServletRequest getRequest(HttpHeaders headers, byte[] bufferedOutput) {
		String contextPath = servlet.getServletContext().getContextPath();
		String path = uri.getPath();
		Assert.state(path.startsWith(contextPath), "request path must start with the context path of the servlet");
		path = path.substring(contextPath.length());
		MockHttpServletRequest request = new MockHttpServletRequest(method.toString(), path);

		request.setContentType(headers.getContentType().toString());
		request.setContent(bufferedOutput);
		
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String headerName = entry.getKey();
			for (String headerValue : entry.getValue()) {
				request.addHeader(headerName, headerValue);
			}
		}

		return request;
	}
}
