# Task 1 – Standardizzazione delle Risposte e Centralizzazione degli Errori

## Scelte progettuali

- **`APIResponse<T>`**: record generico in `domain/`, con tre metodi factory statici
  (`success`, `fail`, `error`) che rispecchiano lo standard JSend. Un solo record
  copre tutti i casi di risposta, evitando classi duplicate per ogni endpoint.
- **`GlobalExceptionHandler`**: `@RestControllerAdvice` in un package dedicato `web/`,
  separato dai controller perché gestisce logica trasversale e non endpoint specifici.
  Intercetta:
    - `MethodArgumentNotValidException` → 400, con mappa campo→messaggio di errore
    - `ResponseStatusException` → status corrispondente (es. 404), formato `fail`
    - `Exception` generica → 500, formato `error`, senza esporre lo stack trace
- **Controller**: rimossi tutti gli `if/else` manuali su `Optional`/`boolean` per il
  "not found"; sostituiti con `orElseThrow(() -> new ResponseStatusException(...))`.
  Questo elimina ogni `try-catch` nei controller, rispettando il vincolo di isolamento
  delle eccezioni.
- La `deleteAutore`/`deleteLibro` non restituiscono più una stringa di testo semplice,
  ma un `APIResponse<Void>` con status `success` — coerente con tutti gli altri endpoint.

## Test eseguiti

- POST `/autori/add` con campo `nome` mancante → risposta 400:
```json
  {"status":"fail","data":{"nome":"Il nome è obbligatorio"},"message":null}
```
- GET `/autori/{id}` con id inesistente → risposta 404:
```json
  {"status":"fail","data":{"error":"Autore non trovato"},"message":null}
```
- GET `/autori` e `/libri` → dati restituiti dentro `data` con `status: "success"`

## Criteri di accettazione soddisfatti

- [x] Un unico contratto di risposta (`APIResponse`) su tutti gli endpoint
- [x] Errori di validazione → 400 con mappa campo/messaggio
- [x] Nessuno stack trace esposto al client
- [x] Nessun `try-catch` nei controller per formattazione errori