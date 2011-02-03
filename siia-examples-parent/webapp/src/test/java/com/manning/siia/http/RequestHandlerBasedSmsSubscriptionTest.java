package com.manning.siia.http;

import static junit.framework.Assert.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.AssertThrows;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.client.RestTemplate;


/**
 * Provides controller tests demonstrating happy and sad path responses.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:http-applicationContext.xml","classpath:http-servlet-applicationContext-TEST.xml"})
public class RequestHandlerBasedSmsSubscriptionTest {

	private static final String REQUEST_PATH = "/sms-api/subscribe";

	protected Log log = LogFactory.getLog(getClass());
	
    @Autowired
    private HttpRequestHandler httpSmsSubscriptionInboundChannelAdapter;
    
    private MessagingTemplate channelTemplate;


    @Autowired @Qualifier("validatedSmsSubscriptionRequests")
    public void setChannel(AbstractMessageChannel messageChannel){
        this.channelTemplate = new MessagingTemplate(messageChannel);
        this.channelTemplate.setReceiveTimeout(100);
    }

	@Test
    public void testFormSubmissionSucceedsAndCreatesMessage() throws Exception {
		
		MockHttpServletRequest request = new MockHttpServletRequest("GET", REQUEST_PATH);
    	request.addParameter("mobileNumber", "+1-612-555-1234");
    	request.addParameter("flightNumber", "BA123");

		MockHttpServletResponse response = doRequest(request);
    	assertEquals(200, response.getStatus());
    	
    	Message<Object> message = this.channelTemplate.receive();
    	assertNotNull(message);
    	Object payload = message.getPayload();
    	assertNotNull(payload);
    }

	@Test
    public void testFormSubmissionFailsWithInvalidMobileNumber() throws Exception {
		
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", REQUEST_PATH);
    	request.addParameter("mobileNumber", "x1-612-555-1234");
    	request.addParameter("flightNumber", "BA123");

    	new AssertThrows(MessagingException.class) {
    		@Override
    		public void test() throws Exception {
    			doRequest(request);
    		}
    		
    		@Override
    		protected void checkExceptionExpectations(Exception actualException) {
    			super.checkExceptionExpectations(actualException);
    			log.info(actualException);
    		}
    	}.runTest();
    	
    	// Expect no message
    	Message<Object> message = this.channelTemplate.receive();
    	assertNull(message);
    }

	

	private MockHttpServletResponse doRequest(HttpServletRequest request) throws Exception {
    	
    	MockHttpServletResponse response = new MockHttpServletResponse();
    	this.httpSmsSubscriptionInboundChannelAdapter.handleRequest(request, response);
		return response;
	}
    
	

	static private <T> ResponseEntity<T> doRequest(Class<T> bodyClass, MultiValueMap<String, Object> multipartMap) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("multipart", "form-data"));
		HttpEntity<Object> request = new HttpEntity<Object>(multipartMap, headers);
		
		return new RestTemplate().exchange("http://localhost:8080/siia-webapp" + REQUEST_PATH, HttpMethod.POST, request, bodyClass);
	}

    
	/**
	 * Uses real RestTemplate when have done mvn jetty:run (or Eclipse/STS Run-> Run on Server)
	 */
	public static void main(String[] args) {
		MultiValueMap<String, Object> multipartMap = new LinkedMultiValueMap<String, Object>();
		multipartMap.add("mobileNumber", "x1-612-555-1234");
		multipartMap.add("flightNumber", "BA123");             
		ResponseEntity<?> response = doRequest(String.class, multipartMap);
    	assertThat(response.getStatusCode(), is(HttpStatus.OK));
		System.out.println(response.getBody());
	}
}
