# Task 3 — Monitoraggio Proattivo e Sistemi di Alerting

## Contesto

L'applicazione `biblioteca`, containerizzata tramite Docker (vedi Task 2), necessitava di uno
strumento per monitorare in tempo reale lo stato di salute interno (memoria JVM, thread attivi,
latenza HTTP) senza dover attendere una segnalazione manuale in caso di malfunzionamento.

L'obiettivo era duplice:

1. Raccogliere ed esporre le metriche dell'applicazione in formato consultabile via browser.
2. Attivare un allarme automatico visibile quando il sistema registra un numero anomalo di
   errori interni (HTTP 500).

## Scelte progettuali

### 1. Esposizione delle metriche (Spring Boot Actuator + Micrometer)

Sono state aggiunte al `pom.xml` le dipendenze:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

e in `application.properties` è stato esposto l'endpoint dedicato:

```properties
management.endpoints.web.exposure.include=health,prometheus
management.metrics.tags.application=${spring.application.name}
```

Questo rende disponibile `/actuator/prometheus`, che espone tutte le metriche (JVM, HTTP
request count/latency per status code, connection pool, ecc.) in formato testuale leggibile da
Prometheus.

### 2. Raccolta metriche (Prometheus)

Il file `monitoring/prometheus/prometheus.yml` configura lo scraping automatico ogni 5 secondi:

```yaml
global:
  scrape_interval: 5s

scrape_configs:
  - job_name: 'biblioteca-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['spring-backend:8080']
```

Il target usa il nome del servizio Docker (`spring-backend`) invece di `localhost`, poiché
Prometheus e il backend girano in container separati sulla stessa rete Docker (`app-network`) e
devono risolversi tramite DNS interno di Compose.

### 3. Estensione dello stack Docker (Prometheus + Grafana)

Il `docker-compose.yaml` è stato esteso con due nuovi servizi:

- **`prometheus`**: monta il file di configurazione sopra descritto e scrapa il backend.
- **`grafana`**: espone la dashboard visiva sulla porta `3000`, con volume persistente
  (`grafana-data`) per non perdere la configurazione ad ogni riavvio.

Entrambi collegati alla stessa rete `app-network` degli altri servizi, con `depends_on` verso
`spring-backend` e `prometheus` rispettivamente.

### 4. Provisioning automatico di Grafana (datasource + alert rule)

Per evitare configurazione manuale via UI (non riproducibile, persa ad ogni ricreazione del
container), Grafana è stato configurato via **provisioning file-based**, montando una cartella
dedicata:

```yaml
  grafana:
    volumes:
      - grafana-data:/var/lib/grafana
      - ./monitoring/grafana/provisioning:/etc/grafana/provisioning
```

Struttura creata:

```
monitoring/grafana/provisioning/
├── datasources/
│   └── datasource.yml     # collega Grafana a Prometheus automaticamente
└── alerting/
    └── rules.yml          # regola di allarme sugli errori 500
```

**Datasource** (`datasource.yml`): registra Prometheus (`http://prometheus:9090`) come
datasource di default all'avvio, senza bisogno di configurazione manuale.

**Alert Rule** (`rules.yml`): implementa il criterio di accettazione richiesto dall'esame. La
query PromQL utilizzata è:

```
increase(http_server_requests_seconds_count{status="500"}[5m])
```

che calcola il numero di richieste con status `500` ricevute negli ultimi 5 minuti. La condizione
di allarme (`threshold`) scatta quando questo valore supera **10**, con un `for: 1m` che richiede
la condizione stabile per almeno un minuto prima di passare allo stato `Firing` (evita falsi
allarmi su picchi istantanei).

Stati previsti dalla regola:

- **Normal** → valore ≤ 10
- **Pending** → valore > 10, in attesa di conferma (1 minuto)
- **Firing** → valore > 10 confermato, allarme attivo

### 5. Endpoint di simulazione errore (`DebugController`)

Per poter dimostrare il comportamento del sistema di allarme senza dover attendere un errore
reale, è stato creato un endpoint dedicato:

```java
@RestController
@RequestMapping("/debug")
@Profile("dev")
public class DebugController {

    @GetMapping("/crash")
    public void crash() {
        throw new RuntimeException("Errore simulato per demo monitoraggio");
    }
}
```

L'endpoint è vincolato al profilo `dev` per non essere raggiungibile in un ambiente di produzione
reale. Per renderlo comunque disponibile durante la demo (che gira con lo stack Docker in
configurazione `prod` per mantenere il logging su file rotante della Task 2), il profilo attivo
del backend in `docker-compose.yaml` è stato impostato su:

```yaml
SPRING_PROFILES_ACTIVE: prod,dev
```

Spring Boot supporta profili multipli attivi contemporaneamente: questo mantiene il comportamento
di logging di produzione mentre registra comunque i bean marcati `@Profile("dev")`.

> **Nota**: l'endpoint `/debug/crash` lancia una `RuntimeException` non intercettata dagli
> `@ExceptionHandler` specifici della Task 1 (dedicati a `MethodArgumentNotValidException` e
> `ResponseStatusException`), quindi la risposta al client segue il formato di default di Spring
> Boot (Whitelabel Error Page) e non lo standard `APIResponse`/JSend. Lo status HTTP resta
> comunque `500`, che è il dato rilevante ai fini della metrica raccolta da Prometheus e della
> regola di allarme.

## Test eseguiti

### Test 1 — Verifica scraping Prometheus

Con lo stack avviato (`docker-compose up --build -d`), verificato su
`http://localhost:9090/targets` che il job `biblioteca-app` risultasse in stato `UP`.

**Esito:** ✅ superato.

### Test 2 — Provisioning automatico Grafana

Verificato tramite log del container:

```powershell
docker logs grafana --tail 100
```

Individuato inizialmente un errore di provisioning (`no such file or directory`) dovuto alla
mancata creazione della cartella `monitoring/grafana/provisioning/` sull'host. Corretto creando
la struttura di cartelle e i due file di configurazione, poi ricreando il container con:

```powershell
docker-compose down
docker-compose up --build -d
```

Verificato che al successivo avvio Grafana caricasse automaticamente il datasource Prometheus
(visibile in **Connections → Data sources**) e la regola di allarme (visibile in
**Alerting → Alert rules**), senza necessità di configurazione manuale.

**Esito:** ✅ superato.

### Test 3 — Simulazione errori 500 e verifica transizione dell'allarme

Generati 11 errori consecutivi contro l'endpoint di debug (soglia della regola: 10):

```powershell
1..11 | ForEach-Object { curl.exe -s http://localhost:8080/debug/crash }
```

Tutte le 11 risposte hanno restituito status `500`, confermato dal body:

```json
{"timestamp":"...","status":500,"error":"Internal Server Error","path":"/debug/crash"}
```

Verificato tramite query PromQL diretta su Prometheus (`http://localhost:9090/graph`):

```
increase(http_server_requests_seconds_count{status="500"}[5m])
```

risultato: valore superiore a 10, confermando che la metrica viene correttamente incrementata ad
ogni chiamata.

Verificata su Grafana (**Alerting → Alert rules**) la transizione di stato della regola
`Troppi errori HTTP 500`:

```
Normal → Pending (condizione superata, in attesa conferma) → Firing (dopo ~1 minuto)
```

**Esito:** ✅ superato — criterio di accettazione della Task 3 soddisfatto: l'allarme si attiva
(stato *Firing*) quando il numero di errori HTTP 500 supera la soglia critica di 10 chiamate.

## Criteri di accettazione soddisfatti

- [X] Dipendenza Spring Boot Actuator + Micrometer Prometheus integrata.
- [X] `docker-compose.yml` esteso con servizi Prometheus e Grafana, con scraping automatico
  dell'endpoint Actuator ad intervalli regolari (5s).
- [X] Datasource Grafana → Prometheus configurato (via provisioning automatico, riproducibile).
- [X] Alert Rule basata su query PromQL che monitora l'incremento temporale delle richieste HTTP
  con stato 500.
- [X] Allarme che si attiva (stato *Firing*) quando il valore supera la soglia critica di 10
  chiamate di errore — dimostrato tramite simulazione controllata (`DebugController`).
