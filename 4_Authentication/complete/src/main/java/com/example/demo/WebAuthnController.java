package com.example.demo;

import java.net.MalformedURLException;
import java.util.Optional;
import java.util.List;

import com.example.demo.WebAuthnServer.SuccessfulAuthenticationResult;
import com.example.demo.data.AssertionRequestWrapper;
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

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
class WebAuthnController {

    @Autowired
    private WebAuthnServer webAuthnServer;

    @Autowired
    private UserDetailsService userDetailsService;

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
    
}
