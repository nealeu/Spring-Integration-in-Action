package com.manning.siia.http;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URI;

import javax.servlet.http.HttpServlet;
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
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.manning.siia.mock.ServletWrappingClientHttpRequest;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:http-applicationContext.xml","classpath:http-servlet-applicationContext-TEST.xml"})
public class RestTemplateBasedSmsSubscriptionTest {

	protected Log log = LogFactory.getLog(getClass());
	
    @Autowired
    private HttpServlet smsServlet;
 
    private MessagingTemplate channelTemplate;


    @Autowired @Qualifier("validatedSmsSubscriptionRequests")
    public void setChannel(AbstractMessageChannel messageChannel){
        this.channelTemplate = new MessagingTemplate(messageChannel);
        this.channelTemplate.setReceiveTimeout(100);
    }

  
	/**
	 * An out of container test
	 */
	@Test
    public void testViaRestTemplate() {
		MultiValueMap<String, Object> multipartMap = new LinkedMultiValueMap<String, Object>();
		multipartMap.add("mobileNumber", "+1-612-555-1234");
		multipartMap.add("flightNumber", "BA123");             
		RestTemplate restTemplate = getRestTemplate(smsServlet);
		ResponseEntity<?> response = doRequest(restTemplate, null, multipartMap); // our servlet mock doesn't have the facilities to handle JSP - use Run On Server and execute main here to see response HTML
    	assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

	@Test
    public void testExceptionViaRestTemplate() {
		MultiValueMap<String, Object> multipartMap = new LinkedMultiValueMap<String, Object>();
		multipartMap.add("mobileNumber", "x1-612-555-1234");
		multipartMap.add("flightNumber", "BA123");             
		
		RestTemplate restTemplate = getRestTemplate(smsServlet);
		ResponseEntity<?> response = doRequest(restTemplate, null, multipartMap);
    	assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }
	
	static private <T> ResponseEntity<T> doRequest(RestTemplate restTemplate, Class<T> bodyClass, MultiValueMap<String, Object> multipartMap) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("multipart", "form-data"));
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
		MultiValueMap<String, Object> multipartMap = new LinkedMultiValueMap<String, Object>();
		multipartMap.add("mobileNumber", "x1-612-555-1234");
		multipartMap.add("flightNumber", "BA123");             
		ResponseEntity<?> response = doRequest(new RestTemplate(), String.class, multipartMap);
    	assertThat(response.getStatusCode(), is(HttpStatus.OK));
		System.out.println(response.getBody());
	}
}
