# Spring Biblioteca

Webservice REST in Springboot per la gestione di una biblioteca

## Dependencies
- Spring Web
- Spring DevTools
- Spring Data JPA
- MySQL Driver
- Validator
- Lombok
- SpringDoc Open API

## Swagger UI

Swagger è un tool via browser che permette il testing delle nostre API.  

L'interfaccia Swagger è disponibile al seguente indirizzo:

```
http://localhost:8080/swagger-ui/index.html
```

## Documentazione

Relazioni tecniche relative alle task svolte per l'esame UF13:

- [Task 2 - Containerizzazione Multi-Stage e Isolamento dei Profili](docs/task2-containerizzazione-profili.md)


## Avvio del progetto

**Locale (profilo dev, logging dettagliato):**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Docker (profilo prod, build multi-stage):**

```bash
docker compose up --build
```