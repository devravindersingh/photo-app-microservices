### 1. Auto config routing from api gateway

```
spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      routes:
        - id: users-status-check
          uri: lb://users-ws
          predicates:
            - Path=/users/status/check
            - Method=GET
          filters:
            - RemoveRequestHeader=Cookie
```

- **predicates.Path** is the incoming request to handle
- client sends request - **localhost:9090/users/status/check**
- if request matches with Path, api gateway routes request to <mark>**uri + Path**</mark>

### 2. Manual routing 
- update below changes
```
          predicates:
            - Path=/user-ms/users/status/check
          filters:
            - RewritePath=/user-ms/users/status/check, /users/status/check            
```
- **user-ms** could be anything
- RewritePath=**incoming request, rewrite path**
- rewrite path to should match to the exact endpoint of the microservice
![](@attachment/Clipboard_2023-04-26-13-35-30.png)

- **Both automatic and manual routing can be used simultaneosly**



# Intermediate notes from course


## Section 10

```
  instance:
    instance-id: ${spring.application.name}:${spring.application.instance_id:${random.value}}
```
- to use application instance id or any random value if not provided.
- this makes every application instance to have a unique id, which is required to get registered in api gateway

- to pass above config from command prompt
`mvn spring-boot:run -D spring-boot.run.arguments="--spring.application.instance_id=ravinder2 --server.port=8999"`


## Section 12

- Random Unique Id can be generated with help of Java Util package UUID class. <mark>Probably the easiest way</mark>
```
UUID.randomUUID().toString()
```
- Adding xml support to project- so that it can return xml as response
  - Add Jackson Dataformat XML dependency in project
  - Use below header variables 
  ```
  Headers
  - Content-Type - application/json
  - Accept - application/xml
  ```

- Spring Security Steps
  1. add Starter spring security dependency
  2. create a standalone class for handling security configuration
      - add <mark>@Configuration</mark> and <mark>@EnableWebSecurity</mark> on class
  3. configure <b>SecurityFilterChain</b> Bean
      - add a method to return securityFilterChain bean
      - cofigure routes here with matchers
      ```
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
      ```
  4. `.access(new WebExpressionAuthorizationManager("hasIpAddress('"+ env.getProperty("gateway.ip") +"')"))`
      - allows to configure incoming traffic ip, use specific ip only
  5. `httpSecurity.headers().frameOptions().disable();`
      - allows frames to display, here its sole purpose is to allow h2-console to show its content without issue.
      - [https://stackoverflow.com/a/65956355](notable.md "Source")


## Section 13

- **AuthenticationFilter** extends **UsernamePasswordAuthenticationFilter** class will help in providing user login request data to <mark>AuthenticationManager</mark> class.
- Overide attemptAuthentication method, create UsernamePasswordAuthenticationToken for
  `getAuthenticationManager().authenticate(UsernamePasswordAuthenticationTokenObj)`
- add AuthenticationManager in SecurityFilterChain by 
  ```
  AuthenticationManagerBuilder authenticationManagerBuilder =
                httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);

        AuthenticationManager authenticationManager = authenticationManagerBuilder.build();
  

  .addFilter(new AuthenticationFilter(authenticationManager))
                .authenticationManager(authenticationManager)

  ```
- After successfull authentication Spring security will automatically invoke <mark>successfulAuthentication</mark> method from <mark>UsernamePasswordAuthenticationFilter</mark> where we can add some bussiness logic.
  ```
  @Override
    protected void successfulAuthentication
  ```

- UsersDetailService is have loadUserByUsername method that we can overide and do our bussiness logic.
  - loadUserByUsername will return a User object from `org.springframework.security.core.userdetails` package

## Section 14

- Spring Cloud Api Gateway will be a centralized logging ,validation and routes point for whole system.
    - it can do intial validations for the JWT tokens signature, expiration checking etc.
    - More granular validation can be done on service side.
- Need to create a JWT filter.
- Not all http request will need validation like creat users.

- Check http request headers for specific header. Authorization header for checking if token is present or not. Gateway will neglect request immediately if header not present.
- add <mark>Header</mark> predicate to routes in API Gateway config. take 2 values 1 header name , 2 general pattern if needs to follow
```
      routes:
        - id: users-status-check
          uri: lb://users-ws
          predicates:
            - Path=/users-ws/users/status
            - Method=GET
            - Header=Authorization, Bearer (.*)
          filters:
            - RemoveRequestHeader=Cookie
            - RewritePath=/users-ws/users/status, /users/status/check
```
- to filter http request at API gateway add support for jwt to gateway service.
- a seperate class to filter http requests before api gateway routes them to destination.
- To create a ***Custom Filter*** we can use spring cloud abstract <mark>AbstractGatewayFilterFactory</mark> class.
  ```
  public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config>
  ```
- AbstractGatewayFilterFactory have GatewayFilter apply method which returns a chain of exchange.
  ```
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest serverHttpRequest = exchange.getRequest();

            if (!serverHttpRequest.getHeaders().containsKey(HttpHeaders.AUTHORIZATION))
                return onError(exchange, "No Authorization header", HttpStatus.UNAUTHORIZED);

            String authorizationHeader = serverHttpRequest.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            String jwt = authorizationHeader.replace("Bearer", "");

            if (!isJwtValid(jwt)) {
                return onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED);
            }

            return chain.filter(exchange);
        };
    }
  ```
- AbstractGatewayFilterFactory requires a static Config class to add additional filter configurations. But usually its empty.
  ```
    public static class Config{
        //put configurations here
    }
  ```
- this filter will be added to application.yml config for a specific route.
  ```
            filters:
            - RemoveRequestHeader=Cookie
            - RewritePath=/users-ws/users/status, /users/status/check
            - AuthorizationHeaderFilter
  ```


### Section 15
- To add Global filter we can use spring <mark>GlobalFilter interface</mark>.
  ```public class MyPostFilter implements GlobalFilter```
- GlobalFilter interface have filter method which is used to manage filter process.
  ```
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return chain.filter(exchange).then(Mono.fromRunnable(()->{
            LOG.info("Global post-filter executed....");
        }));
    }
  ```
- We can also use <mark>Ordered interface</mark> from spring core to make class execution ordered.
  ```public class MyPostFilter implements GlobalFilter, Ordered```
- Ordered interface have getOrder which return integer denoting order value.
  ```
    @Override
    public int getOrder() {
        return 1;
    }
  ```
- lowest the value highest the precedence.
- Pre filters always have low value high precedence and post filters have high value high precedence.
- We can also create beans of global filters and combine both pre and post at the same time.
- <mark>@Order</mark> annotation to make bean execution ordered.
  ```
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
  ```





















