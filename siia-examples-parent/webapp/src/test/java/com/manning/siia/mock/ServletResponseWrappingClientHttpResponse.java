package com.manning.siia.mock;



import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;

final public class ServletResponseWrappingClientHttpResponse implements ClientHttpResponse {


	private HttpHeaders headers;
	private MockHttpServletResponse response;


	ServletResponseWrappingClientHttpResponse(MockHttpServletResponse response) {
		this.response = response;
	}


	public HttpStatus getStatusCode() throws IOException {
		return HttpStatus.valueOf(this.response.getStatus());
	}

	public String getStatusText() throws IOException {
		return getStatusCode().toString();
	}

	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
			
			for (String name: response.getHeaderNames()) {
//				if (!StringUtils.hasLength(name)) {
//					break;
//				}
				for(Object value: response.getHeaders(name)) {
					this.headers.add(name, value.toString());
				}
			}
		}
		return this.headers;
	}

	public InputStream getBody() throws IOException {
		return new ByteArrayInputStream(response.getContentAsByteArray());
	}

	public void close() {
		// not needed as BAOS doesn't need closing
	}

}
