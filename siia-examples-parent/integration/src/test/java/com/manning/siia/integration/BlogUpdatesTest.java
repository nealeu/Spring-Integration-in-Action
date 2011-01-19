package com.manning.siia.integration;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    private MessagingTemplate channelTemplate;

    @Autowired
    private HttpRequestExecutingMessageHandler blogAdapter;

	private RestTemplate restTemplateSpy = spy(new RestTemplate());
	
	private String payload = 
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

		// TODO: This really should be exposed as is this is a common scenario
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
    	ResponseEntity<String> responseEntity = new ResponseEntity<String>("Ok", HttpStatus.CREATED);
		doReturn(responseEntity).when(restTemplateSpy).exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class), any(Map.class));

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
