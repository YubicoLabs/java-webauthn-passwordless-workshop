# Module 3: Registration
In this module, you will enable a user to register a security key.

## WebAuthn Registration Overview

<details>
<summary><strong>Expand for high level overview of WebAuthn registration</strong></summary><p>

### Registration

Initiate a registration ceremony:

```java
byte[] userHandle = new byte[64];
random.nextBytes(userHandle);

PublicKeyCredentialCreationOptions request = rp.startRegistration(StartRegistrationOptions.builder()
    .user(UserIdentity.builder()
        .name("alice")
        .displayName("Alice Hypothetical")
        .id(new ByteArray(userHandle))
        .build())
    .build());
```

Serialize `request` to JSON and send it to the client:

```java
import com.fasterxml.jackson.databind.ObjectMapper;

@Bean 
public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.setSerializationInclusion(Include.NON_NULL);
    mapper.setSerializationInclusion(Include.NON_ABSENT);
    return mapper;
}
```

Get the response from the client:

```java
String responseJson = /* ... */;
PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc =
    jsonMapper.readValue(responseJson, new TypeReference<PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs>>(){});
```

Validate the response:

```java
try {
    RegistrationResult result = rp.finishRegistration(FinishRegistrationOptions.builder()
        .request(request)
        .response(pkc)
        .build());
} catch (RegistrationFailedException e) { /* ... */ }
```

Update your database:

```java
storeCredential("alice", result.getKeyId(), result.getPublicKeyCose());
```

</p></details>


## Implementation overview
This section will walk through how to customize the WebAuthn Server registration process, Serialize JSON, expose the registration methods as REST endpoints, and consume the endpoints from the frontend.

### Update WebAuthn server registration

<details>
<summary><strong>Step by step instructions</strong></summary><p>

The webauthn-server-demo project has the concept of `AuthenticatedActions`. We will not be using `AuthenticatedActions` in this demo. Instead, we will use the Spring Security user session. First, a user will log in with a traditional username and password then register resident credential on a security key. This resident credential will enable usernameless passwordless authentication in the next module.

The current startRegistration() method only allows a single security key to be registered. Let's update it so that a user can add multiple security keys.

1. Open the `./src/main/java/com/example/demo/WebAuthnServer.java` class in your editor and 
2. Add the following import:
   ```
   import com.yubico.webauthn.data.AuthenticatorAttachment;
   ```
3. Modify the startRegistration() method to look like this:
    ```java
        public Either<String, RegistrationRequest> startRegistration(
            @NonNull String username,
            @NonNull String displayName,
            Optional<String> credentialNickname,
            boolean requireResidentKey
        ) {
            logger.trace("startRegistration username: {}, credentialNickname: {}", username, credentialNickname);

            if (username == null || username.isEmpty()) {
                return Either.left("username must not be empty.");
            }

            Collection<CredentialRegistration> registrations = userStorage.getRegistrationsByUsername(username);

            UserIdentity user;

            if (registrations.isEmpty()) {
                user = UserIdentity.builder()
                    .name(username)
                    .displayName(displayName)
                    .id(generateRandom(32))
                    .build();
            } else {
                user = registrations.stream().findAny().get().getUserIdentity();
            }

            RegistrationRequest request = new RegistrationRequest(
                username,
                credentialNickname,
                generateRandom(32),
                rp.startRegistration(
                    StartRegistrationOptions.builder()
                        .user(user)
                        .authenticatorSelection(Optional.of(AuthenticatorSelectionCriteria.builder()
                            .requireResidentKey(requireResidentKey)
                            .authenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM)    // Default to roaming security keys (CROSS_PLATFORM). Comment out this line to enable either PLATFORM or CROSS_PLATFORM authenticators
                            .build()
                        ))
                        .build()
                )
            );

            registerRequestStorage.put(request.getRequestId(), request);

            return Either.right(request);
        }
    ```

</p></details>

### Configure JSON rendering with in Spring Boot 

<details>
<summary><strong>Step by step instructions</strong></summary><p>

1. Add the following to the import section of the `./src/main/java/com/example/demo/WebAuthnServer.java` class
```java
import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.SerializationFeature;
import static com.fasterxml.jackson.annotation.JsonInclude.Include;
```
2. Add the following method to the `./src/main/java/com/example/demo/WebAuthnServer.java` class. The ObjectMapper is configured to handle Optional types and not serialize fields to JSON that are null or absent.
```java
@Bean 
public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.setSerializationInclusion(Include.NON_NULL);
    mapper.setSerializationInclusion(Include.NON_ABSENT);
    return mapper;
}
```

</p></details>

### [Optional but not recommended at this time] Configure the WebAuthn Server to accept platform authenticators
One of the implications of implementing an in-memory credential repository is that credentials on the server disappear each time the application restarts. Since we are registering a resident credential on the authenticator itself, those credentials will be invalid after an application restart. The risk is that the authenticator will fill up with invalid resident credentials. To mitigate this risk we configured the WebAuthn Server to only allow the registration of roaming authenticators a.k.a. "cross-platform" authenticators. 

Using a YubiKey is ideal for development environments such as this. If your YubiKey fills up with invalid credentials, you can remove all FIDO credentials by using the [YubiKey Manager](https://www.yubico.com/products/services-software/download/yubikey-manager/). Basically it resets the internal FIDO application back to the factory default settings. 

<details>
<summary><strong>Step-by-step instructions (expand to enable platform authenticator registration)</strong></summary><p>

To configure the WebAuthn Server to accept platform authenticators, such as Windows Hello comment out the `.authenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM)` line. This workshop recommends that only test platform authenticators be registered as the instructions to remove invalid credentials from platform authenticators are not available at this time. E.g. Don't use your primary Windows Hello platform authenticator in this workshop.
```java
    ...
    RegistrationRequest request = new RegistrationRequest(
                username,
                credentialNickname,
                generateRandom(32),
                rp.startRegistration(
                    StartRegistrationOptions.builder()
                        .user(user)
                        .authenticatorSelection(Optional.of(AuthenticatorSelectionCriteria.builder()
                            .requireResidentKey(requireResidentKey)
                            //.authenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM)    // Default to roaming security keys (CROSS_PLATFORM). Comment out this line to enable either PLATFORM or CROSS_PLATFORM authenticators
                            .build()
                        ))
                        .build()
                )
            );

            registerRequestStorage.put(request.getRequestId(), request);

            return Either.right(request);
        }
```
</p></details>

### Create a WebAuthn REST controller and expose the registration operation endpoints
Let's expose two REST endpoints to start and finish the WebAuthn registration operation.

<details>
<summary><strong>Step by step instructions</strong></summary><p>

1. Create WebAuthnController.java
   ```
   echo '' > ./src/main/java/com/example/demo/WebAuthnController.java
   ```
2. Open `./src/main/java/com/example/demo/WebAuthnController.java` in an editor and create the following class
    ```java
    package com.example.demo;

    import java.net.MalformedURLException;
    import java.util.Optional;
    import java.util.List;

    import com.example.demo.WebAuthnServer.SuccessfulAuthenticationResult;
    import com.example.demo.data.RegistrationRequest;
    import com.yubico.util.Either;

    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.security.access.prepost.PreAuthorize;
    import org.springframework.web.bind.annotation.PostMapping;
    import org.springframework.web.bind.annotation.RequestBody;
    import org.springframework.web.bind.annotation.RequestParam;
    import org.springframework.web.bind.annotation.RestController;
    import org.springframework.web.server.ResponseStatusException;

    import lombok.extern.slf4j.Slf4j;

    @RestController
    @Slf4j
    class WebAuthnController {

        @Autowired
        private WebAuthnServer webAuthnServer;

        @PostMapping("/register")
        @PreAuthorize("#username == authentication.principal.username")
        ResponseEntity<RegistrationRequest> startRegistration(@RequestParam("username") String username,
                @RequestParam("displayName") String displayName,
                @RequestParam("credentialNickname") Optional<String> credentialNickname,
                @RequestParam(value = "requireResidentKey", defaultValue = "false") boolean requireResidentKey)
                throws MalformedURLException {
                    log.trace("startRegistration username: {}, displayName: {}, credentialNickname: {}, requireResidentKey: {}", username, displayName, credentialNickname, requireResidentKey);

                    Either<String, RegistrationRequest> result = webAuthnServer.startRegistration(username, displayName, credentialNickname, requireResidentKey);

                    if (result.isRight()) {
                        return ResponseEntity.status(HttpStatus.OK).body(result.right().get());
                    } else {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, result.left().get());
                    }

        }

        @PostMapping("/register/finish")
        ResponseEntity<WebAuthnServer.SuccessfulRegistrationResult> finishRegistration(@RequestBody String responseJson) {
            log.trace("finishRegistration responseJson: {}", responseJson);

            Either<List<String>, WebAuthnServer.SuccessfulRegistrationResult> result = webAuthnServer.finishRegistration(responseJson);

            if (result.isRight()) {
                return ResponseEntity.status(HttpStatus.OK).body(result.right().get());
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, result.left().get().toString());
            }

        }
    }
    ```

</p></details>

### Call registration REST endpoints from account page

<details>
<summary><strong>Step by step instructions</strong></summary><p>

1. Open the `./src/main/resources/templates/account.html` page in an editor
2. In the header section add cross site request forgery token details. Our JavaScript requires the CSRF token to call our REST endpoints
    ```javascript
    <meta th:name="_csrf" th:content="${_csrf.token}"/>
    <meta th:name="_csrf_header" th:content="${_csrf.headerName}"/>
    ```
3. In the header section also add the following JavaScript references
    ```javascript
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.4.0/jquery.min.js"></script>
    <script type="module" src="/lib/fetch/fetch-3.0.0.js"></script>
    <script src="/lib/base64js/base64js-1.3.0.min.js"></script>
    <script src="/js/base64url.js"></script>
    <script src="/js/webauthn.js"></script>
    ```
4. In the header section add the following javascript to call our REST endpoints and register a security key
    ```javascript
        <script>
            $(function () {
                $('#takeAction').hide();
            });

            function setStatus(statusText) {
                $('#status').text(statusText);
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
                }).then(response => response.json())
                ;
            }
        
            function register() {
                $('#takeAction').show();
                const username = '[[${#authentication.getPrincipal().getUsername()}]]';
                const displayName = '[[${#authentication.getPrincipal().getUsername()}]]';
                const credentialNickname = $("#inputNickname").val();
                const requireResidentKey = true;
        
                var token = $("meta[name='_csrf']").attr("content"); 
        
                return fetch('/register', {
                    method: 'POST',
                    headers: {
                        'X-CSRF-TOKEN': token
                    },
                    body: new URLSearchParams({
                    username,
                    displayName,
                    credentialNickname,
                    requireResidentKey,
                    })
                    
                })
                .then(response => response.json())
                .then(function(request) {
                    console.log('request succeeded with JSON response', request)
                    
                    return webauthn.createCredential(request.publicKeyCredentialCreationOptions)
                    .then(webauthn.responseToObject)
                    .then(function (publicKeyCredential) { 
                        console.log("publicKeyCredential ", publicKeyCredential);
            
                        url = '/register/finish';
                        return submitResponse(url, request.requestId, publicKeyCredential);
                    })
                })
                .then(data => {
                    if (data && data.success) {
                        console.log("Success!");
                        setStatus("Success!");
                    } else {
                        console.log("Error!");
                        setStatus('Error!');
                    }
                    $('#takeAction').hide();
                    console.log(data);
                    return data;
                })
                ;
            }
         </script>
    ```
5. In the body section add the following UI to register a security key and get a handle on the username of the currently signed in user.
    ```html
        <sec:authentication property="name" var="username" />
        <div class="container">
            <h4>Register a Security Key</h4>
            <div class="form-inline">
                <div class="form-group mx-sm-3 mb-2">
                    <label for="inputNickname" class="sr-only">Nickname</label>
                    <input type="text" class="form-control" id="inputNickname" placeholder="Nickname">
                </div>
                <button onclick="register()" class="btn btn-primary mb-2">Register</button>
            </div>
            <p id="status"></p>
            <div id="takeAction">
                <p class="text-justify">Please insert and take action on the security key.</p>
                <div class="spinner-border" role="status">
                    <span class="sr-only">Loading...</span>
                </div>
            </div>
        </div>
    ```
</p></details>

### Validate Registration functionality
1. Run the web app
    
    Locally
    ```
    mvn clean package spring-boot:run
    ```

    Azure Cloud Shell
    ```
    mvn clean package azure-webapp:deploy
    ```
   
2. Open the web app local URL https://localhost:8443/ or the cloud URL.
3. Sign in to the web app
4. Register a security key.
5. Refresh browser to see the security key show up in the list.

## Recap
* Customize the WebAuthn Server registration settings to your needs
* Remember to serialize the JSON data
* Expose registration endpoints
* Consume registration endpoints in frontend and call the WebAuthn API

## Next
Once you have finished this section, you may proceed to the next module, [Authentication](../4_Authentication/README.md)