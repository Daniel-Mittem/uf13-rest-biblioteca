package it.marconi.biblioteca.controllers;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/debug")
@Profile("dev")
public class DebugController {

    @GetMapping("/crash")
    public void crash() {
        throw new RuntimeException("Errore simulato per demo monitoraggio");
    }
}