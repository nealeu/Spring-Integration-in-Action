package com.manning.siia.domain.notification;

import org.springframework.util.Assert;

/**
 * Request to receive notifications of flight delays via text message
 */
public class SmsSubscriptionRequest {

	private String mobileNumber;
	private String flightNumber;

	public String getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(String mobileNumber) {
		Assert.isTrue(mobileNumber.startsWith("+"), "mobileNumber must start with +");
		this.mobileNumber = mobileNumber;
	}

	public String getFlightNumber() {
		return flightNumber;
	}

	public void setFlightNumber(String flightNumber) {
		this.flightNumber = flightNumber;
	}

}
