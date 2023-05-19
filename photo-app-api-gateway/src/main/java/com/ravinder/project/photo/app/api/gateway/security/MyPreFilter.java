package com.ravinder.project.photo.app.api.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
public class MyPreFilter implements GlobalFilter {

    private final Logger LOG = LoggerFactory.getLogger(MyPreFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        LOG.info("My first Pre-Filter is executed ....");

        String requestPath = exchange.getRequest().getPath().toString();
        LOG.info("Request path = " + requestPath);

        HttpHeaders headers = exchange.getRequest().getHeaders();
        Set<String> headerNames = headers.keySet();

        headerNames.forEach(headerName-> {
            String headerValue = headers.getFirst(headerName);
            LOG.info(headerName + " " + headerValue);
        });

        return chain.filter(exchange);
    }
}
