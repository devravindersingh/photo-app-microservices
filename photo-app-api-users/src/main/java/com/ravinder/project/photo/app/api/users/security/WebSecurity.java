package com.ravinder.project.photo.app.api.users.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class WebSecurity {

    private Environment env;

    public WebSecurity(Environment env) {
        this.env = env;
    }

    @Bean
    protected SecurityFilterChain configure(HttpSecurity httpSecurity) throws Exception{
        httpSecurity.csrf().disable();

        httpSecurity.authorizeHttpRequests()
                .requestMatchers(HttpMethod.POST, "/users")
                .access(
                        new WebExpressionAuthorizationManager(
                                "hasIpAddress('"+ env.getProperty("gateway.ip") +"')"))
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        httpSecurity.headers().frameOptions().disable();
        return httpSecurity.build();

    }
}
