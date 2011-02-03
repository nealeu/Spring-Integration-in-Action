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


/**
 * Provides controller tests demonstrating happy and sad path responses.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:http-applicationContext.xml","classpath:http-servlet-applicationContext-TEST.xml"})
public class SmsSubscriptionTest {

	protected Log log = LogFactory.getLog(getClass());
	
    @Autowired
    private HttpRequestHandlingController httpSmsSubscriptionInboundChannelAdapter;
    
    private MessagingTemplate channelTemplate;


    @Autowired @Qualifier("validatedSmsSubscriptionRequests")
    public void setChannel(AbstractMessageChannel messageChannel){
        this.channelTemplate = new MessagingTemplate(messageChannel);
        this.channelTemplate.setReceiveTimeout(100);
    }

	@Test
    public void testFormSubmissionSucceedsAndCreatesMessage() throws Exception {
		
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sms/subscribe");
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
		
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sms/subscribe");
    	request.addParameter("mobileNumber", "x1-612-555-1234");
    	request.addParameter("flightNumber", "BA123");

		MockHttpServletResponse response = doRequest(request);
    	assertEquals(200, response.getStatus());
    	
    	// Expect no message
    	Message<Object> message = this.channelTemplate.receive();
    	assertNull(message);
    }

	

	private MockHttpServletResponse doRequest(HttpServletRequest request) throws Exception {
    	
    	MockHttpServletResponse response = new MockHttpServletResponse();
    	ModelAndView mav = this.httpSmsSubscriptionInboundChannelAdapter.handleRequest(request, response);
    	if (mav.getModelMap().containsKey("errors")) {
    		Errors errors = (Errors)mav.getModelMap().get("errors");
    		log.error("Errors found: " + errors);
    	}
		return response;
	}
    
	

	static private <T> ResponseEntity<T> doRequest(Class<T> bodyClass, MultiValueMap<String, Object> multipartMap) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("multipart", "form-data"));
		HttpEntity<Object> request = new HttpEntity<Object>(multipartMap, headers);
		
		return new RestTemplate().exchange("http://localhost:8080/siia-webapp/sms/subscribe", HttpMethod.POST, request, bodyClass);
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
