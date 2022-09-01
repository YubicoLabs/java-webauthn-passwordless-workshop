# Module 4: Authentication
In this module, you will add a passwordless authentication to the application.

## WebAuthn Authentication Overview
<details>
<summary><strong>Expand for high level overview of WebAuthn authentication</strong></summary><p>

### Authentication

Initiate a authentication ceremony:

```java
AssertionRequest request = rp.startAssertion(StartAssertionOptions.builder()
    .username(Optional.of("alice"))
    .build());
String json = jsonMapper.writeValueAsString(request);
return json;
```

Validate the response:

```java
String responseJson = /* ... */;

PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc =
    jsonMapper.readValue(responseJson, new TypeReference<PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs>>() {
});

try {
    AssertionResult result = rp.finishAssertion(FinishAssertionOptions.builder()
        .request(request)
        .response(pkc)
        .build());

    if (result.isSuccess()) {
        return result.getUsername();
    }
} catch (AssertionFailedException e) { /* ... */ }
throw new RuntimeException("Authentication failed");
```

Authenticate the user:

```java
// Manually authenticate user
String username = result.right().get().getRegistrations().iterator().next().getUserIdentity().getName();

Authentication auth = SecurityContextHolder.getContext().getAuthentication();

UserDetails u = userDetailsService.loadUserByUsername(username);

Authentication newAuth = new UsernamePasswordAuthenticationToken(u, auth.getCredentials(),u.getAuthorities());

SecurityContextHolder.getContext().setAuthentication(newAuth);

return ResponseEntity.status(HttpStatus.OK).body(result.right().get());
```

</p></details>

## Implementation overview
This section will walk through how to expose the authentication REST endpoints, manually authenticate a user in Spring Security, and consume the authentication endpoints in the frontend.

### Add authentication REST endpoints
Let's expose two REST endpoints to start and finish the WebAuthn authentication operation.

<details>
<summary><strong>Step by step instructions</strong></summary><p>

1. Open the `./src/main/java/com/example/demo/WebAuthnController.java` class in your editor
2. Add the following imports in the import section
    ```java
    import com.example.demo.data.AssertionRequestWrapper;
    
    import org.springframework.security.core.userdetails.UserDetails;
    import org.springframework.security.core.userdetails.UserDetailsService;
    import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
    import org.springframework.security.core.Authentication;
    import org.springframework.security.core.context.SecurityContextHolder;
    ```
3. In the `WebAuthnController` class add a reference to the `UserDetailsService`. This is the service which loads user-specific data.
    ```java
    @Autowired
    private UserDetailsService userDetailsService;
    ```
4. Add the `/authenticate` and `/authenticate/finish` REST endpoints
    ```java
    @PostMapping("/authenticate")
    public ResponseEntity<AssertionRequestWrapper> startAuthentication(@RequestParam("username") Optional<String> username) {

        Either<List<String>, AssertionRequestWrapper> result = webAuthnServer.startAuthentication(username);

        if (result.isRight()) {
            return ResponseEntity.status(HttpStatus.OK).body(result.right().get());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, result.left().get().toString());
        }

    }

    @PostMapping("/authenticate/finish")
    public ResponseEntity<WebAuthnServer.SuccessfulAuthenticationResult> finishAuthentication(
            @RequestBody String responseJson) {

        Either<List<String>, WebAuthnServer.SuccessfulAuthenticationResult> result = webAuthnServer
                .finishAuthentication(responseJson);

        if (result.isRight()) {
            // Manually authenticate user
            String username = result.right().get().getRegistrations().iterator().next().getUserIdentity().getName();

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            UserDetails u = userDetailsService.loadUserByUsername(username);

            Authentication newAuth = new UsernamePasswordAuthenticationToken(u, auth.getCredentials(),
                    u.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(newAuth);

            return ResponseEntity.status(HttpStatus.OK).body(result.right().get());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, result.left().get().toString());
        }

    }
    ```

</p></details>

### Make the authentication REST endpoints accessible

<details>
<summary><strong>Step by step instructions</strong></summary><p>

1. Open the `./src/main/java/com/example/demo/WebSecurityConfig.java` in your editor
2. Modify the `antMatchers()` method to include the new REST endpoints and javascript libraries so that they are accessible to users before they sign in.
```java
 http
    .authorizeRequests()
        .antMatchers("/", "/home", "/css/**", "/images/**", "/js/**", "/lib/**", "/authenticate", "/authenticate/finish").permitAll()
        .anyRequest().authenticated()
```

### Integrate WebAuthn API into application
1. Open the `./src/main/resources/templates/login.html` template
2. Add the following code in the header section
    ```javascript
        <meta th:name="_csrf" th:content="${_csrf.token}" />
        <meta th:name="_csrf_header" th:content="${_csrf.headerName}" />

        <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.4.0/jquery.min.js"></script>
        <script type="module" src="/lib/fetch/fetch-3.0.0.js"></script>
        <script src="/lib/base64js/base64js-1.3.0.min.js"></script>
        <script src="/js/base64url.js"></script>
        <script src="/js/webauthn.js"></script>

        <script>

            function setStatus(statusText, success) {
                $('#status').text(statusText);
                if (success) {
                    $('#status').removeClass('error');
                } else {
                    $('#status').addClass('error');
                }
            }

            function submitResponse(url, requestId, response) {
                console.log('submitResponse', url, requestId, response);
        
                var token = $("meta[name='_csrf']").attr("content"); 
        
                const body = {
                    requestId,
                    credential: response,
                };
                console.log('body', JSON.stringify(body));
                
                return fetch(url, {
                    method: 'POST',
                    headers: {
                        'X-CSRF-TOKEN': token
                    },
                    body: JSON.stringify(body),
                }).then(response => response.json());
                ;
            }

            function authenticate() {
                const username = "";
                const token = $("meta[name='_csrf']").attr("content");

                return fetch('/authenticate', {
                    method: 'POST',
                    headers: {
                        'X-CSRF-TOKEN': token
                    },
                    })  
                    .then(response => response.json())
                    .then(function (request) {
                        console.log('request succeeded with JSON response', request)

                        return webauthn.getAssertion(request.publicKeyCredentialRequestOptions)
                            .then(webauthn.responseToObject)
                            .then(function (publicKeyCredential) {
                                console.log("publicKeyCredential ", publicKeyCredential);

                                url = '/authenticate/finish';
                                return submitResponse(url, request.requestId, publicKeyCredential);
                            })
                            .catch(error => {
                                throw error;
                            })
                            ;
                        })
                    .then(data => {
                        console.log("Success!");
                        window.location.href = "/account"
                        console.log(data);
                        return data;
                    })
                    .catch(error => {
                        console.log('authenticate', error);
                        setStatus(error.message, false);
                    })
                    ;
            }

        </script>
    ```
3. Add the Passwordless sign in button to the body section.
    ```html
    <hr />

    <h2 class="form-signin-heading">Passwordless sign in</h2>
    <p>Sign in with your previously registered security key</p>
    <p id="status"></p>
    <p><button onclick="authenticate()">Passwordless Sign in</button><br />
    </p>
    <p><a href="/index" th:href="@{/home}">Back to home page</a></p>
    ```

</p></details>

### Validate passwordless authentication functionality and integration into the application
1. Run the web app
    
    Locally
    ```
    mvn clean package spring-boot:run
    ```

    Azure Cloud Shell
    ```
    mvn clean package azure-webapp:deploy
    ```
   
2. Open the web app URL local https://localhost:8443/ or in the cloud.
3. Sign in to the web app
4. Register a security key
5. Sign out
6. Click the Passwordless sign in button to sign in without typing a username or password
7. You should signed in and redirected to the account page

## Recap
* Expose authentication endpoints
* Make sure the authentication endpoints are accessible to anonymous users
* Consume the authentication endpoints in the frontend and call the WebAuthn API
* After the assertion is verified, authenticate the user's session

## Next
Congratulations! You've completed the passwordless workshop. We hope that this time has been valuable for you. For further learning on this topic, please see [Yubico's developer site](https://developers.yubico.com/FIDO2).

Please remember to run through the [Clean up steps](../5_Clean_Up/README.md) to ensure you decommission all resources spun up during the workshop today.

Thank you for participating in this workshop!
