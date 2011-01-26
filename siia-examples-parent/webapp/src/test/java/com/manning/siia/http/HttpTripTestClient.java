package com.manning.siia.http;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;


public class HttpTripTestClient {
	
	
	static private ResponseEntity<?> doRequest(RestTemplate restTemplate) {
		
		// TripQuoteRequest request = new TripQuoteRequest( )
		
		String request = "need to decide what we're using here";
		
		ResponseEntity<?> response = restTemplate.postForEntity("http://localhost:8080/siia-webapp/httpquote", request, String.class);
    	assertThat(response.getStatusCode(), is(HttpStatus.OK));
    	return response;
	}

	
	/**
	 * Uses real RestTemplate when have done mvn jetty:run (or Eclipse/STS Run-> Run on Server)
	 */
	public static void main(String[] args) {
		ResponseEntity<?> response = doRequest(new RestTemplate());
		System.out.println(response.getBody());
	}

}
