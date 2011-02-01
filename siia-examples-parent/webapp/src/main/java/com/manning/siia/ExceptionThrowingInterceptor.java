package com.manning.siia;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.ChannelInterceptor;

/**
 * Throws and exception if we've set a header as such
 *
 */
public class ExceptionThrowingInterceptor implements ChannelInterceptor {

	public static final String THROW_AN_EXCEPTION = "throwException";

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		if (message.getHeaders().get("http_requestUrl").toString().contains(THROW_AN_EXCEPTION)){
			throw new MessagingException("Doh.  Something failed.");
		}
		
		return message;
	}

	@Override
	public void postSend(Message<?> message, MessageChannel channel,
			boolean sent) {
	}

	@Override
	public boolean preReceive(MessageChannel channel) {
		return true;
	}

	@Override
	public Message<?> postReceive(Message<?> message, MessageChannel channel) {
		return message;
	}

}
