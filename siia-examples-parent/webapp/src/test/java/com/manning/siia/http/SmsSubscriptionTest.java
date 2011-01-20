package com.manning.siia.http;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URI;

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
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartResolver;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:http-applicationContext.xml"})
public class SmsSubscriptionTest {


    @Autowired
    private HttpRequestHandler httpSmsSubscriptionInboundChannelAdapter;
    
    @Autowired
    private MultipartResolver multipartResolver;

    private MessagingTemplate channelTemplate;
    
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    
	@Before 
    public void setUpMocks() throws IOException {
		request = new MockHttpServletRequest("GET", "/updates/subscribe");
		response = new MockHttpServletResponse();
    }
    

    @Autowired @Qualifier("smsSubscriptionsChannel")
    public void setChannel(MessageChannel messageChannel){
        this.channelTemplate = new MessagingTemplate(messageChannel);
        this.channelTemplate.setReceiveTimeout(100);
    }


	@Test
    public void testFormSubmissionSucceedsAndCreatesMessage() throws Exception {
    	request.addParameter("smsNumber", "+1-612-555-1234");
    	request.addParameter("flight", "BA123");
    	request.addParameter("date", "2009-12-01");
    	
    	httpSmsSubscriptionInboundChannelAdapter.handleRequest(request, response);
    	assertEquals(200, response.getStatus());
    	
    	Message<Object> message = channelTemplate.receive();
    	assertNotNull(message);
    	Object payload = message.getPayload();
    }

    
	@Test
    public void test() {
		MultiValueMap<String, Object> multipartMap = new LinkedMultiValueMap<String, Object>();
		multipartMap.add("smsNumber", "+1-612-555-1234");
		multipartMap.add("flight", "BA123");             
		multipartMap.add("date", "2009-12-01");          
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("multipart", "form-data"));
		HttpEntity<Object> request = new HttpEntity<Object>(multipartMap, headers);
		
		ResponseEntity<String> response = getRestTemplate(httpSmsSubscriptionInboundChannelAdapter).exchange("http://localhost/blah", HttpMethod.POST, request, String.class);
    	assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

	
	private RestTemplate getRestTemplate(final HttpRequestHandler handler) {
		
		RestTemplate result = new RestTemplate(new ClientHttpRequestFactory() {
			@Override
			public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
				return new HandlerWrappingClientHttpRequest(uri, httpMethod, handler, multipartResolver);
			}
		});
		
		
		return result;
	}

    
}
