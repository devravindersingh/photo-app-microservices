package com.ravinder.project.photo.app.api.users.security;

import com.ravinder.project.photo.app.api.users.service.UsersService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class WebSecurity {

    private UsersService usersService;
    private BCryptPasswordEncoder passwordEncoder;

    private Environment env;

    public WebSecurity(UsersService usersService, BCryptPasswordEncoder passwordEncoder, Environment env) {
        this.usersService = usersService;
        this.passwordEncoder = passwordEncoder;
        this.env = env;
    }

    @Bean
    protected SecurityFilterChain configure(HttpSecurity httpSecurity) throws Exception{

        //configure AuthenticationManagerBuilder
        AuthenticationManagerBuilder authenticationManagerBuilder =
                httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);

        authenticationManagerBuilder
                .userDetailsService(usersService)
                .passwordEncoder(passwordEncoder);

        AuthenticationManager authenticationManager = authenticationManagerBuilder.build();

        //create AuthenticationFilter
        AuthenticationFilter authenticationFilter =
                new AuthenticationFilter(authenticationManager, usersService, env);
        //custom url for login
        authenticationFilter.setFilterProcessesUrl("/signin");

        httpSecurity.csrf().disable();

        httpSecurity.authorizeHttpRequests()
                .requestMatchers(HttpMethod.POST, "/users")
                .access(
                        new WebExpressionAuthorizationManager(
                                "hasIpAddress('"+ env.getProperty("gateway.ip") +"')"))
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
                .requestMatchers(HttpMethod.GET, "/users/status/check").permitAll()
                .and()
                .addFilter(authenticationFilter)
                .authenticationManager(authenticationManager)
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        httpSecurity.headers().frameOptions().disable();
        return httpSecurity.build();

    }
}
