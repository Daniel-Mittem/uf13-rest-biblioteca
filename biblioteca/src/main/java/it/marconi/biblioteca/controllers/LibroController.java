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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import it.marconi.biblioteca.domain.APIResponse;
import it.marconi.biblioteca.domain.LibroDTO;
import it.marconi.biblioteca.services.LibroService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/libri")
public class LibroController {

    @Autowired
    LibroService libroService;

    @GetMapping
    @Operation(summary = "Recupera la lista di tutti i libri")
    public ResponseEntity<APIResponse<List<LibroDTO>>> getAll() {
        return ResponseEntity.ok(APIResponse.success(libroService.findAll()));
    }

    @GetMapping("/{isbn}")
    @Operation(summary = "Cerca un libro dal suo ISBN")
    public ResponseEntity<APIResponse<LibroDTO>> getLibroByIsbn(@PathVariable String isbn) {

        LibroDTO libro = libroService.getByIsbn(isbn)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Libro non trovato"));

        return ResponseEntity.ok(APIResponse.success(libro));
    }

    @GetMapping("/libro")
    @Operation(summary = "Cerca un libro per titolo esatto")
    public ResponseEntity<APIResponse<LibroDTO>> getLibroByTitolo(@RequestParam("titolo") String titolo) {

        LibroDTO libro = libroService.getByTitolo(titolo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Libro non trovato"));

        return ResponseEntity.ok(APIResponse.success(libro));
    }

    @PostMapping("/add")
    @Operation(summary = "Aggiunge un nuovo libro, dato l'autore")
    public ResponseEntity<APIResponse<LibroDTO>> addLibro(@Valid @RequestBody LibroDTO libro) {

        LibroDTO salvato = libroService.save(libro)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Autore non trovato"));

        return ResponseEntity.ok(APIResponse.success(salvato));
    }

    @DeleteMapping("/{isbn}")
    @Operation(summary = "Elimina un libro dato il suo ISBN")
    public ResponseEntity<APIResponse<Void>> deleteLibro(@PathVariable String isbn) {

        boolean deleted = libroService.deleteByIsbn(isbn);

        if (!deleted)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Libro non trovato");

        return ResponseEntity.ok(APIResponse.success(null));
    }
}