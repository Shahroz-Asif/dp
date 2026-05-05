package com.example.recipemaker.config;

import com.example.recipemaker.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configures STOMP over WebSocket.
 *
 * Clients connect to {@code /ws} (SockJS-enabled), subscribe to
 * {@code /user/queue/notifications} for personal pushes, and authenticate
 * by sending their Basic-auth credentials in the STOMP CONNECT frame's
 * {@code login} / {@code passcode} headers.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple in-memory broker for personal queues and topic broadcasts
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Channel interceptor that authenticates STOMP CONNECT frames.
     * Reads {@code login} and {@code passcode} headers, verifies them against
     * the database, and sets the authenticated {@link java.security.Principal}
     * on the accessor so Spring can route {@code /user/…} destinations correctly.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String login    = accessor.getLogin();
                    String passcode = accessor.getPasscode();

                    if (login != null && passcode != null) {
                        try {
                            UserDetails user = userDetailsService.loadUserByUsername(login);
                            if (passwordEncoder.matches(passcode, user.getPassword())) {
                                UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(
                                                user, null, user.getAuthorities());
                                accessor.setUser(auth);
                            }
                        } catch (Exception ignored) {
                            // Leave accessor.user null — server will treat as unauthenticated
                        }
                    }
                }
                return message;
            }
        });
    }
}
