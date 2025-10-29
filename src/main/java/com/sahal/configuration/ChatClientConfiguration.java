package com.sahal.configuration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.context.annotation.Primary;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

@Configuration
public class ChatClientConfiguration {

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new OpenRouterInterceptor());
        return restTemplate;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("You are a helpful AI assistant.")
                .defaultUser("User")
                .build();
    }

    private static class OpenRouterInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(
                org.springframework.http.HttpRequest request,
                byte[] body,
                ClientHttpRequestExecution execution) throws IOException {
            
            HttpHeaders headers = request.getHeaders();
            headers.add("HTTP-Referer", "http://localhost:8080");
            headers.add("X-Title", "AI Demo");
            
            return execution.execute(request, body);
        }
    }
}
