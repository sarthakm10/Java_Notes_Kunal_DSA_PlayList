package com.notes.ms.tasks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

@Configuration
public class ClientConfig {

    /**
     * The PLAIN builder, for everyone else. Without it, the @LoadBalanced builder below would be the only
     * RestClient.Builder bean (Boot's default backs off), and the Eureka client would get it too: it then
     * treated "localhost" in http://localhost:8761/eureka/ as a service name and never registered
     * ("No servers available for service: localhost"). @Primary makes plain injection points get this one.
     */
    @Bean
    @Primary
    RestClient.Builder restClientBuilder(RestClientBuilderConfigurer configurer) {
        return configurer.configure(RestClient.builder());
    }

    /**
     * @LoadBalanced: Spring Cloud adds an interceptor that turns http://user-service into the address of one
     * registered instance, chosen per request (round robin by default).
     * configurer.configure(...) applies Boot's customizations, including the observation (tracing) one,
     * so the trace id travels to user-service in the request headers.
     */
    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer,
                                                     @Value("${app.user-service.timeout:1s}") Duration timeout) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(500));
        factory.setReadTimeout(timeout);                     // a slow user-service fails the call after 1 s
        return configurer.configure(RestClient.builder()).requestFactory(factory);
    }

    @Bean
    UserClient userClient(@LoadBalanced RestClient.Builder builder) {
        RestClient client = builder.baseUrl("http://user-service").build();   // a service NAME, not a host
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client)).build()
                .createClient(UserClient.class);
    }
}
