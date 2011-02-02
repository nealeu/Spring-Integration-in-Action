package com.manning.siia.integration;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:blog-endpoint.xml"})
public class BlogUpdatesTest {
	
	protected Log log = LogFactory.getLog(getClass());

    private MessagingTemplate channelTemplate;

    @Autowired
    private HttpRequestExecutingMessageHandler blogAdapter;

	private RestTemplate restTemplateSpy = spy(new RestTemplate());
	
	static private final String payload = 
		"<entry xmlns='http://www.w3.org/2005/Atom'>" +
		"  <title type='text'>Flight xxx delayed</title>" +
		"  <content type='xhtml'>" +
		"    <div xmlns=\"http://www.w3.org/1999/xhtml\">" +
		"      <p>blah</p>" +
		"    </div>" +
		"  </content>" +
		"  <category scheme=\"http://www.blogger.com/atom/ns#\" term='flights' />" +
		"</entry>";
    
	@Before 
    public void setUpMocks() throws IOException {

		// For SI >= 2.0.2, RestTemplate can be provided when constructing HttpRequestExecutingMessageHandler, but for creating spies, this
		// is probably the easiest way to approach things, as we use the application context.
		// The other approach would be to provide a default restTemplate to autowire in our main application context, and to override that bean
		// in a test scoped application context
		new DirectFieldAccessor(blogAdapter).setPropertyValue("restTemplate", restTemplateSpy);

    	reset(restTemplateSpy);
    }
    

    @Autowired @Qualifier("flightUpdates")
    public void setChannel(MessageChannel messageChannel){
        this.channelTemplate = new MessagingTemplate(messageChannel);
    }


    @SuppressWarnings("unchecked")
	@Test
    public void testPostSucceedsWhenServerAvailable(){
    	final ResponseEntity<String> responseEntity = new ResponseEntity<String>("Ok", HttpStatus.CREATED);
		Answer<ResponseEntity<String>> answer = new Answer<ResponseEntity<String>>() {
			@Override
			public ResponseEntity<String> answer(InvocationOnMock invocation) throws Throwable {
				HttpEntity<?> request = (HttpEntity<?>) invocation.getArguments()[2];
				log.info("HTTP Request Body = " + request.getBody());
				assertTrue("Body should contain our payload", request.getBody().equals(payload));
				return responseEntity;
			}
		};
		doAnswer(answer).when(restTemplateSpy).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class), any(Map.class));

		postFlightUpdate();
		
		verify(this.restTemplateSpy, times(1)).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class), any(Map.class));
    }

    
    @SuppressWarnings("unchecked")
	@Test(expected=MessagingException.class)
    public void testConnectionFailureProducesMessagingException() {
		doThrow(new ResourceAccessException("Couldn't connect")).when(restTemplateSpy).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class), any(Map.class));

		postFlightUpdate();
    }

    
	private void postFlightUpdate() {
		HttpHeaders headers = new HttpHeaders();
    	headers.setContentType(MediaType.APPLICATION_ATOM_XML);
        
		Message<String> message = MessageBuilder.withPayload(payload)
        	.copyHeaders(headers.toSingleValueMap())
        	.build();
			channelTemplate.send(message);
	}
}
