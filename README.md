
Spring Biblioteca
Webservice REST in Springboot per la gestione di una biblioteca
Dependencies

* Spring Web
* Spring DevTools
* Spring Data JPA
* MySQL Driver
* Validator
* Lombok
* SpringDoc Open API
* Spring Boot Actuator
* Micrometer Registry Prometheus

Swagger UI
Swagger è un tool via browser che permette il testing delle nostre API.
L'interfaccia Swagger è disponibile al seguente indirizzo:

```
http://localhost:8080/swagger-ui/index.html

```

Monitoraggio (Prometheus & Grafana)
Con lo stack Docker avviato sono disponibili anche gli strumenti di monitoraggio:

* Prometheus: http://localhost:9090
* Grafana: http://localhost:3000 (login: admin / admin)

Grafana viene configurato automaticamente all'avvio (datasource Prometheus e alert rule sugli errori HTTP 500) tramite provisioning file-based, senza bisogno di setup manuale.

Documentazione
Relazioni tecniche relative alle task svolte per l'esame UF13:

* [Task 1 - Standardizzazione delle Risposte e Centralizzazione degli Errori](biblioteca/docs/task1-standardizzazione-risposte.md)
* [Task 2 - Containerizzazione Multi-Stage e Isolamento dei Profili](biblioteca/docs/task2-containerizzazione-profili.md)
* [Task 3 - Monitoraggio Proattivo e Sistemi di Alerting](biblioteca/docs/task3-monitoraggio-alerting.md)

Avvio del progetto
Locale (profilo dev, logging dettagliato):

```
mvn spring-boot:run -Dspring-boot.run.profiles=dev

```

Docker (profilo prod, build multi-stage, stack completo con Prometheus e Grafana):

```
docker compose up --build
```
