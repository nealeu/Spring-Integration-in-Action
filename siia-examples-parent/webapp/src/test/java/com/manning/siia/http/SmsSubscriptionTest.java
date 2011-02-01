package com.manning.siia.http;

import static junit.framework.Assert.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URI;

import javax.servlet.http.HttpServlet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.http.inbound.HttpRequestHandlingController;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.manning.siia.mock.ServletWrappingClientHttpRequest;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:http-applicationContext.xml","classpath:http-servlet-applicationContext-TEST.xml"})
public class SmsSubscriptionTest {

    @Autowired
    private HttpServlet smsServlet;

    @Autowired
    private HttpRequestHandlingController httpSmsSubscriptionInboundChannelAdapter;
    
    private MessagingTemplate channelTemplate;
    

    @Autowired @Qualifier("smsSubscriptionsChannel")
    public void setChannel(MessageChannel messageChannel){
        this.channelTemplate = new MessagingTemplate(messageChannel);
        this.channelTemplate.setReceiveTimeout(100);
    }

	@Test
    public void testFormSubmissionSucceedsAndCreatesMessage() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sms/subscribe");
		MockHttpServletResponse response = new MockHttpServletResponse();
    	request.addParameter("smsNumber", "+1-612-555-1234");
    	request.addParameter("flight", "BA123");
    	request.addParameter("date", "2009-12-01");
    	
    	httpSmsSubscriptionInboundChannelAdapter.handleRequest(request, response);
    	assertEquals(200, response.getStatus());
    	
    	Message<Object> message = channelTemplate.receive();
    	assertNotNull(message);
    	Object payload = message.getPayload();
    }
    
	/**
	 * An out of container test
	 */
	@Test
    public void testViaRestTemplate() {
		RestTemplate restTemplate = getRestTemplate(smsServlet);
		doRequest(restTemplate, null); // our servlet mock doesn't have the facilities to handle JSP - use Run On Server and execute main here to see response HTML
    }

	
	static private ResponseEntity<?> doRequest(RestTemplate restTemplate, Class<?> bodyClass) {
		MultiValueMap<String, Object> multipartMap = new LinkedMultiValueMap<String, Object>();
		multipartMap.add("smsNumber", "+1-612-555-1234");
		multipartMap.add("flight", "BA123");             
		multipartMap.add("date", "2009-12-01");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("multipart", "form-data"));
		HttpEntity<Object> request = new HttpEntity<Object>(multipartMap, headers);
		
		ResponseEntity<?> response = restTemplate.exchange("http://localhost:8080/siia-webapp/sms/subscribe", HttpMethod.POST, request, bodyClass);
    	assertThat(response.getStatusCode(), is(HttpStatus.OK));
    	return response;
	}

	/**
	 * Get restTemplate that runs directly against httpServlet object
	 */
	private RestTemplate getRestTemplate(final HttpServlet servlet) {
		return new RestTemplate(new ClientHttpRequestFactory() {
			@Override
			public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
				return new ServletWrappingClientHttpRequest(uri, httpMethod, servlet);
			}
		});
	}
    
	/**
	 * Uses real RestTemplate when have done mvn jetty:run (or Eclipse/STS Run-> Run on Server)
	 */
	public static void main(String[] args) {
		ResponseEntity<?> response = doRequest(new RestTemplate(), String.class);
		System.out.println(response.getBody());
	}
}
