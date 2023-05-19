package com.ravinder.project.photo.app.api.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

@Configuration
public class GlobalFiltersConfiguration{

    private final Logger LOG = LoggerFactory.getLogger(GlobalFiltersConfiguration.class);

    @Order(1)
    @Bean
    public GlobalFilter secondPreFilter(){
        return ((exchange, chain) -> {
            LOG.info("My second global pre-filter is executed....");
            return chain.filter(exchange).then(Mono.fromRunnable(()->{
                LOG.info("My second post-filter was executed");
            }));
        });
    }

    @Order(2)
    @Bean
    public GlobalFilter thirdPreFilter(){
        return ((exchange, chain) -> {
            LOG.info("My third global pre-filter is executed....");
            return chain.filter(exchange).then(Mono.fromRunnable(()->{
                LOG.info("My third post-filter was executed");
            }));
        });
    }



}
