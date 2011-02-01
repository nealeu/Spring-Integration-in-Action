package com.manning.siia.mock;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.multipart.MultipartResolver;

/**
 * {@link ClientHttpRequest} implementation that wraps a Spring {@link HttpRequestHandler}
 *
 * @author Neale Upstone
 */
final class HandlerWrappingClientHttpRequest extends AbstractClientHttpRequest {

	private HttpRequestHandler handler;
	private URI uri;
	private HttpMethod method;
	private MultipartResolver multipartResolver;


	HandlerWrappingClientHttpRequest(URI uri, HttpMethod httpMethod, HttpRequestHandler handler, MultipartResolver multipartResolver) {
		this.uri = uri;
		this.method = httpMethod;
		this.handler = handler;
		this.multipartResolver = multipartResolver;
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

		
		
		if (multipartResolver.isMultipart(request)) {
			request = multipartResolver.resolveMultipart(request);
		}
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		try {
			handler.handleRequest(request, response);
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
		MockHttpServletRequest request = new MockHttpServletRequest(method.toString(), uri.getPath());

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
