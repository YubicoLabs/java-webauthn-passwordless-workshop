package com.example.demo;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountController {

    @Autowired
    private WebAuthnServer webAuthnServer;

    @GetMapping("/account")
    public String registerAll(Principal principal, Model model) {
        model.addAttribute("registrations", webAuthnServer.getRegistrationsByUsername(principal.getName()));
        return "account";
    }
}
