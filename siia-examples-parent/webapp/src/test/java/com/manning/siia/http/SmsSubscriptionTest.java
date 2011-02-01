package com.manning.siia.http;

import static junit.framework.Assert.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URI;

import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
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
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.http.inbound.HttpRequestHandlingController;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.Errors;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import com.manning.siia.mock.ServletWrappingClientHttpRequest;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:http-applicationContext.xml","classpath:http-servlet-applicationContext-TEST.xml"})
public class SmsSubscriptionTest {

	protected Log log = LogFactory.getLog(getClass());
	
    @Autowired
    private HttpServlet smsServlet;

    @Autowired
    private HttpRequestHandlingController httpSmsSubscriptionInboundChannelAdapter;
    
    private MessagingTemplate channelTemplate;

    private final ExceptionThrowingChannelInterceptor testInterceptor = new ExceptionThrowingChannelInterceptor();
    
    @Before
    public void reset() {
    	this.testInterceptor.setExceptionClassToThrow(null); // normally don't throw anything
    }
    

    @Autowired @Qualifier("validatedSmsSubscriptionRequests")
    public void setChannel(AbstractMessageChannel messageChannel){
//		messageChannel.addInterceptor(testInterceptor);
        this.channelTemplate = new MessagingTemplate(messageChannel);
        this.channelTemplate.setReceiveTimeout(100);
    }

	@Test
    public void testFormSubmissionSucceedsAndCreatesMessage() throws Exception {
		MockHttpServletResponse response = doRequest();
    	assertEquals(200, response.getStatus());
    	
    	Message<Object> message = this.channelTemplate.receive();
    	assertNotNull(message);
    	Object payload = message.getPayload();
    }

	@Test
    public void testFormSubmissionFailsOnMessagingException() throws Exception {
		this.testInterceptor.setExceptionClassToThrow(MessagingException.class);
		MockHttpServletResponse response = doRequest();
    	assertEquals(200, response.getStatus());
    	
    	Message<Object> message = this.channelTemplate.receive();
    	assertNotNull(message);
    	Object payload = message.getPayload();
    }

	

	private MockHttpServletResponse doRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sms/subscribe");
		MockHttpServletResponse response = new MockHttpServletResponse();
    	request.addParameter("mobileNumber", "+1-612-555-1234");
    	request.addParameter("flightNumber", "BA123");
    	
    	ModelAndView mav = this.httpSmsSubscriptionInboundChannelAdapter.handleRequest(request, response);
    	if (mav.getModelMap().containsKey("errors")) {
    		Errors errors = (Errors)mav.getModelMap().get("errors");
    		log.error("Errors found: " + errors);
    	}
		return response;
	}
    
	/**
	 * An out of container test
	 */
	@Test
    public void testViaRestTemplate() {
		RestTemplate restTemplate = getRestTemplate(smsServlet);
		ResponseEntity<?> response = doRequest(restTemplate, null); // our servlet mock doesn't have the facilities to handle JSP - use Run On Server and execute main here to see response HTML
    	assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

	@Test
    public void testExceptionViaRestTemplate() {
		this.testInterceptor.setExceptionClassToThrow(MessagingException.class);
		
		RestTemplate restTemplate = getRestTemplate(smsServlet);
		ResponseEntity<?> response = doRequest(restTemplate, null);
    	assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }
	
	static private ResponseEntity<?> doRequest(RestTemplate restTemplate, Class<?> bodyClass) {
		MultiValueMap<String, Object> multipartMap = new LinkedMultiValueMap<String, Object>();
		multipartMap.add("mobileNumber", "+1-612-555-1234");
		multipartMap.add("flightNumber", "BA123");             
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("multipart", "form-data"));
		headers.set("throwException", "throwException");
		HttpEntity<Object> request = new HttpEntity<Object>(multipartMap, headers);
		
		return restTemplate.exchange("http://localhost:8080/siia-webapp/sms/subscribe", HttpMethod.POST, request, bodyClass);
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
    	assertThat(response.getStatusCode(), is(HttpStatus.OK));
		System.out.println(response.getBody());
	}
}
