# AWS Lambda mit Scala 3 + Scala.js

Eine AWS Lambda Function, geschrieben in **Scala 3.8.4** und kompiliert mit **Scala.js 1.22.0** zu JavaScript
(CommonJS). Deployed auf der Node.js 22.x Runtime ohne API Gateway – über eine direkte **Lambda Function URL**.

## Versionen

| Komponente      | Version |
|-----------------|---------|
| Scala           | 3.8.4   |
| Scala.js        | 1.22.0  |
| Node.js Runtime | 22.x    |
| Terraform       | >= 1.9  |

## Projektstruktur

```
awsLambdaScalaJS/
├── handler.scala     # Lambda Handler (Scala 3 / Scala.js)
├── build.sh          # Build-Skript (Compile + ZIP)
├── main.tf           # Terraform: Lambda + IAM + Function URL
├── dist/
│   └── handler.js    # Generiertes JS (nach build.sh)
└── lambda.zip        # Deployment-Artefakt (nach build.sh)
```

## Voraussetzungen

- [Scala CLI](https://scala-cli.virtuslab.org/) installiert
- [Terraform](https://developer.hashicorp.com/terraform/install) >= 1.9
- AWS Credentials in `~/.aws/credentials` (z.B. via `aws configure`)
- Node.js (nur für lokale Tests)

## Build

```bash
./build.sh
```

Das Skript:

1. Kompiliert `handler.scala` via Scala CLI zu `dist/handler.js` (CommonJS)
2. Packt es in `lambda.zip`

## Deployment

```bash
# Einmalig initialisieren
terraform init
```

```bash
# Plan prüfen
terraform plan
```

```bash
# Deployen
terraform apply

# Nach dem Apply wird die URL ausgegeben:
# function_url = "https://<id>.lambda-url.eu-central-1.on.aws/"
```

## Aufruf

```bash
# Ohne Parameter
curl "$(terraform output -raw function_url)"
```

```bash
# Mit name-Parameter
curl "$(terraform output -raw function_url)?name=Gunther"
```

Antwort:

```json
{"message":"Hello, Gunther! You called: / (requestId: abc-123)","path":"/","name":"Gunther"}
```

## Lokaler Test

Nach dem Build kann der Handler lokal mit Node.js getestet werden:

```bash
node -e "
const h = require('./dist/handler.js');
h.handler(
  { rawPath: '/test', rawQueryString: 'name=Local' },
  { functionName: 'local', functionVersion: '1', awsRequestId: 'test-123', memoryLimitInMB: '256' }
).then(r => console.log(r));
"
```

## Aufräumen

```bash
terraform destroy
```

## Konfiguration

In `main.tf` können folgende Variablen angepasst werden:

| Variable          | Default          | Beschreibung            |
|-------------------|------------------|-------------------------|
| `aws_region`      | `eu-central-1`   | AWS Region              |
| `function_name`   | `scala-js-hello` | Name der Lambda         |
| `lambda_zip_path` | `./lambda.zip`   | Pfad zum Deployment-ZIP |

---

## AWS Kosten & Free Tier

### Entstehen Kosten?

**Ja**, sobald du über den Free Tier hinausgehst. Für dieses Projekt sind die Kosten aber **minimal bis null**.

### AWS Lambda Free Tier (dauerhaft, nicht nur 12 Monate)

| Ressource          | Free Tier pro Monat     |
|--------------------|-------------------------|
| Requests           | **1.000.000** Aufrufe   |
| Compute (GB-s)     | **400.000 GB-Sekunden** |
| Response Streaming | 100 GB                  |

Bei einer Lambda mit 256 MB Speicher und 30ms Ausführungszeit:

- 400.000 GB-s / (256/1024 MB * 0.03s) = **~51 Millionen** kostenlose Aufrufe/Monat

### Weitere mögliche Kosten

| Service             | Free Tier                | Nach Free Tier          |
|---------------------|--------------------------|-------------------------|
| CloudWatch Logs     | 5 GB Ingest/Monat        | $0.50/GB                |
| Lambda Function URL | Kostenlos (kein API GW!) | —                       |
| Data Transfer OUT   | 1 GB/Monat               | $0.09/GB (eu-central-1) |

### Fazit

Für persönliche Nutzung / Tests: **praktisch kostenlos** dank dauerhaftem Free Tier. Kosten entstehen erst bei sehr
hohem Traffic (>1 Mio. Requests/Monat).

> **Hinweis**: IAM Role, Lambda Function und CloudWatch Log Group sind in `terraform destroy` enthalten und werden
> vollständig gelöscht.
