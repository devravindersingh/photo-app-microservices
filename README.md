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

## Section 15

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


### Section 16
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
### Section 17
- Centralized configuration <mark>Config Server</mark>
- Sensitive information is better to be placed at a centralized location which config server helps to get.
- makes changes in application properties file without effecting all microservices.
- changes can be done on a fly, no problem to client side.
- To create a config server we need a separate service that will act like config server.
  ```
  	<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-config-server</artifactId>
		</dependency>
  ```
- A git repo for storing application properties files.
- config server files will have priority over local application properties.
- Grant config server access to git repo for config files - 
  ```
  spring:
    application:
      name: PhotoAppApiConfigServer
    cloud:
      config:
        server:
          git:
            uri: https://github.com/devravindersingh/photo-app-config-files
            username: devravindersingh
            password: your Access token
            clone-on-start: true
            default-label: master
  server:
    port: 8012
  ```
- create access token in git in ***setting/developer settings/personal access token*** 
  - permission will be contents - read and write if both absolutely required
- make required services config server clients by adding config server client dependency.
  ```
  	<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-config</artifactId>
		</dependency>
  ```
- make sure application name is defined in the application.yml file
  ```
  spring:
    application:
      name: users-ws
  ```
- add config-server property into client
  ```
  spring:
    config:
      import: optional:configserver:http://localhost:8012
  ```
- above in older version it was bootstrap config
- run config server and we can access config files by 
  `localhost:8012/application-name/profile`
  `localhost:8012/users-ws/default`
- default is automatically set by spring if none profile provided.
- resources with file names in application are shared between all client applications `(so application.properties, application.yml, application-*.properties etc.)`.

### Section 18
- Config server only fetch changes to config files at the start of the service. To retrieve changes we may need to re-start all services and then config files changes gets done.
- to overcome this we can use spring cloud bus amqp(advanced messaging queue protocol) with rabbitmq to broadcast changes to all config server clients.
- it can be done by hitting actuator endpoint `https://localhost:port/busrefresh` of config server.
-  add spring cloud starter bus and actuator dependencies to config server and only spring cloud starter bus dependency to config server clients.
  ```
  	<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-bus-amqp</artifactId>
		</dependency>
    <dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
  ```
- enable busrefresh endpoint in config server
  ```
  management:
    endpoints:
      web:
        exposure:
          include: busrefresh
  ```
- provide rabbitmq config 
  - start rabbitmq in local, with docker easiest way
    - `docker run -d --hostname my-rabbit --name some-rabbit -e RABBITMQ_DEFAULT_USER=user -e RABBITMQ_DEFAULT_PASS=password -p 5671:5672 -p 15671:15672 rabbitmq:3-management`
    - 5671 of rabbitmq and 15671 of rabbitmq-ui are port forwarded to local outside docker
  - add rabbitmq config to both server and clients
    ```
    spring:
      rabbitmq:
        host: localhost
        port: 5671
        username: user
        password: password
    ``` 
- start service and when changes made to conifg files in remote repos, hit actuator busrefresh endpoint to reflect changes to all associated services.

### Section 19
- We can use local setup instead of remote git setup for config server.
- we can store config files in local also and point config server to fetch from local folder.
- add these configs in congfig server application properties
  ```
  spring.profile.active=native
  spring.cloud.config.server.native.search-locations=file://${user.home}/config-repo
  ```
- above will behave like git repo but in local file system with config-repo being folder containing all config files.

### Section 23
- Encrypting sensitive information is suggested when using config server. Add one more layer of security.
- Spring cloud config provides 2 types of encryptions 
  - Symmetric Encryption (shared)
    - a same Unique key to encrypt and decrypt
    - easier to setup
  - Asymmetric Encryption (RSA Keypair)
    - superior encryption 
    - unique public and private key
    - public key for encrypting
    - private key for decrypting
- for older java verisons older 9 only , latest versions have it installed by default 
  - download jce java cryptography extension from oracle
  - extract zip file and copy in jre security folder
  - check readme file provided in zip file
  - reload jvm , close all java programs or just restart system.
- Symmetric Encryption 
  - add below config with a unique random key
    ```
    encrypt:
      key: your_key
    ```
  - start config server and make post request `http:localhost:port/encrypt` with body raw with just string for encryption
  - same if you want to decrypt make post request `http:localhost:port/decrypt` with body raw string to decrypt
  - add prefix for encrypted value in application properties
    ` '{cipher}.......value' `

- Asymmetric Encryption
  - generate keypair file by 
    ```
    keytool -genkeypair -alias photoAppKey -keyalg RSA -dname "CN=Ravinder Singh ,OU=RavinderProject,O=RavinderProject.com,L=Sangrur,S=Punjab,C=IN" -keypass a1b2c3d4 -keystore photoAppKey.jks -storepass a1b2c3d4
    ```
  - it output 
    ```
    Generating 2,048 bit RSA key pair and self-signed certificate (SHA256withRSA) with a validity of 90 days
        for: CN=Ravinder Singh, OU=RavinderProject, O=RavinderProject.com, L=Sangrur, ST=Punjab, C=IN
    ```
  - same directory a file will be generated with name photoAppKey.jks
  - copy the file to config server project classpath 
  - add below config in bootstrap file, passowrd is keypass and alias is as it is from above command
    ```
    encrypt:
      key-store:
        location: classpath:key/photoAppKey.jks
        password: a1b2c3d4
        alias:  photoAppKey
    ```
  - encrypt and decrypt as same as symmertic methods.


















