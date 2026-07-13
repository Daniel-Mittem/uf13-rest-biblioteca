package it.marconi.biblioteca.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import it.marconi.biblioteca.domain.APIResponse;
import it.marconi.biblioteca.domain.AutoreDTO;
import it.marconi.biblioteca.domain.LibroDTO;
import it.marconi.biblioteca.services.AutoreService;
import it.marconi.biblioteca.services.LibroService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/autori")
public class AutoreController {

    @Autowired
    LibroService libroService;

    @Autowired
    AutoreService autoreService;

    @GetMapping
    @Operation(summary = "Recupera tutti gli autori")
    public ResponseEntity<APIResponse<List<AutoreDTO>>> getAll() {
        return ResponseEntity.ok(APIResponse.success(autoreService.findAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Cerca un autore dato il suo ID")
    public ResponseEntity<APIResponse<AutoreDTO>> getAutore(@PathVariable Integer id) {

        AutoreDTO autore = autoreService.getById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Autore non trovato"));

        return ResponseEntity.ok(APIResponse.success(autore));
    }

    @GetMapping("/{id}/libri")
    @Operation(summary = "Recupera tutti i libri di un dato autore")
    public ResponseEntity<APIResponse<List<LibroDTO>>> getLibriByAutore(@PathVariable Integer id) {
        return ResponseEntity.ok(APIResponse.success(libroService.getByAutoreId(id)));
    }

    @PostMapping("/add")
    @Operation(summary = "Aggiunge un nuovo autore")
    public ResponseEntity<APIResponse<AutoreDTO>> addAutore(@Valid @RequestBody AutoreDTO autore) {

        AutoreDTO salvato = autoreService.save(autore);
        return ResponseEntity.ok(APIResponse.success(salvato));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Rimuove un autore dal database, e anche tutti i suoi libri")
    public ResponseEntity<APIResponse<Void>> deleteAutore(@PathVariable Integer id) {

        boolean deleted = autoreService.deleteById(id);

        if (!deleted)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Autore non trovato");

        return ResponseEntity.ok(APIResponse.success(null));
    }
}