# Task 2 – Containerizzazione Multi-Stage e Isolamento dei Profili

## Scelte progettuali

- **Dockerfile multi-stage**: due stage separati. Il primo (`AS build`) usa
  `maven:3.9-eclipse-temurin-21` per compilare il progetto; il secondo riparte da
  `eclipse-temurin:21-jre-alpine` e copia solo il `.jar` finale via `COPY --from=build`.
  Questo evita che l'immagine di produzione contenga Maven, il JDK completo o il
  codice sorgente — solo runtime JRE minimale.
- **Caching delle dipendenze**: nel Dockerfile, `pom.xml` viene copiato e le dipendenze
  scaricate (`mvn dependency:go-offline`) *prima* di copiare il codice sorgente.
  Così, se cambia solo il codice Java, Docker riusa il layer con le dipendenze già
  scaricate invece di riscaricarle a ogni build.
- **Profili Spring (`dev`/`prod`)**: gestiti tramite `logback-spring.xml` con blocchi
  `<springProfile>` condizionali, in combinazione con `application-dev.properties` e
  `application-prod.properties` per le proprietà JPA specifiche.
  - `dev`: root logger a `INFO`, ma con override puntuali a `TRACE` sui logger
    `it.marconi.biblioteca`, `org.hibernate.SQL` e `org.hibernate.orm.jdbc.bind`,
    per vedere le query e i parametri bindati senza inondare la console di log
    di sistema (RMI, JMX, ecc.).
  - `prod`: logging a `INFO` su file (`RollingFileAppender` con
    `SizeAndTimeBasedRollingPolicy`), rotazione sia per dimensione (10MB) che per
    tempo (giornaliera), storico di 30 giorni.
- **Volume Docker per i log**: nel `docker-compose.yaml`, `./logs:/var/log/biblioteca`
  monta la cartella dei log su una directory dell'host, così i log sopravvivono anche
  se il container viene distrutto e ricreato.

## Problemi riscontrati e risolti

**1. Log completamente silenzioso senza profilo attivo.**
Il primo tentativo di `logback-spring.xml` metteva sia gli appender che il `<root>`
dentro i blocchi `<springProfile name="dev">` / `<springProfile name="prod">`.
Avviando l'applicazione senza nessun profilo attivo (caso di default da IDE), nessuno
dei due blocchi veniva attivato: risultato, zero appender configurati e log muto.
Soluzione: aggiunto un blocco di fallback `<springProfile name="!dev &amp; !prod">`
con appender console di base a `INFO`, così l'app ha sempre un comportamento di
logging definito anche senza profilo esplicito.

**2. `TRACE` sul root logger inondava la console di log di sistema.**
Impostando `<root level="TRACE">` nel profilo `dev`, il livello TRACE si propagava a
*tutti* i logger senza override specifico, incluso rumore interno della JVM/IDE
(`sun.rmi.loader`, `javax.management.mbeanserver`, ecc. dovuto al JMX remoto attivato
da IntelliJ). Le query Hibernate erano completamente sommerse in questo rumore.
Soluzione: root riportato a `INFO`, e `TRACE` applicato solo ai tre logger specifici
(`it.marconi.biblioteca`, `org.hibernate.SQL`, `org.hibernate.orm.jdbc.bind`), che
sovrascrivono il livello del padre indipendentemente da esso.

## Test eseguiti

- Avvio senza profilo attivo → fallback su profilo `default` confermato, log INFO
  pulito su console, applicazione funzionante (connessione MySQL, Tomcat su 8080 ok).
- Avvio con profilo `dev` attivo → confermato "The following 1 profile is active: dev"
  nel log di avvio; dopo il fix del root logger, le query Hibernate e i relativi
  parametri bindati compaiono correttamente in console, senza rumore di sistema.
- Build immagine Docker (`docker compose up --build`) → build multi-stage completata
  con successo, container avviato e raggiungibile su `localhost:8080`.
- Verifica log su host in modalità `prod` → confermato che i log compaiono in
  `./logs/app.log` sulla macchina host tramite il volume montato, non solo dentro
  il container.

## Criteri di accettazione soddisfatti

- [x] Dockerfile a due stage distinti (build / runtime minimale)
- [x] Immagine finale priva di Maven/JDK, solo JRE + jar
- [x] Profilo `dev`: logging TRACE mirato, con tracciamento query Hibernate
- [x] Profilo `prod`: logging INFO su file rotante per tempo e dimensione
- [x] Log persistiti sull'host tramite volume Docker