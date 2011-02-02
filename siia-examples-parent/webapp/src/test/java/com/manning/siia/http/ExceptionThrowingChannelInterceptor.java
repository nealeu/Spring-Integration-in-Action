package com.manning.siia.http;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;

/**
 * Allows a developer to use an interceptor to simulate an exception occurring during
 * message processing.
 */
public class ExceptionThrowingChannelInterceptor implements ChannelInterceptor {

	private Class<? extends RuntimeException> exceptionClassToThrow = null;
	
	
	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		if (exceptionClassToThrow == null) {
			return message;
		}
		
		RuntimeException exception = null;
		try {
			exception = exceptionClassToThrow.newInstance();
		}
		catch (Exception e) {
			// TODO: log something too..?
			throw new RuntimeException(e);
		}
		throw exception;
	}

	@Override
	public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
	}

	@Override
	public boolean preReceive(MessageChannel channel) {
		return true;
	}

	@Override
	public Message<?> postReceive(Message<?> message, MessageChannel channel) {
		return message;
	}

	public void setExceptionClassToThrow(Class<? extends RuntimeException> exceptionClassToThrow) {
		this.exceptionClassToThrow = exceptionClassToThrow;
	}
}
