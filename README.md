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





















