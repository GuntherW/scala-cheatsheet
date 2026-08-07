# AWS Lambda mit Scala 3 + Scala Native

Eine AWS Lambda Function, geschrieben in **Scala 3.3.8** und kompiliert mit **Scala Native 0.5.12** zu einer nativen Linux-Binary. Deployed auf der **`provided.al2023` Custom Runtime** (Amazon Linux 2023) – kein JVM, kein Node.js.

## Versionen

| Komponente       | Version               |
|------------------|-----------------------|
| Scala            | 3.3.8 LTS             |
| Scala Native     | 0.5.12                |
| Lambda Runtime   | provided.al2023       |
| Build-Umgebung   | lokal (clang 18+)     |
| Terraform        | >= 1.9                |

## Architektur

```
handler.scala
    │
    ▼ scala-cli --native (Docker: amazonlinux:2023 + clang 15)
    │
dist/bootstrap          ← native ELF x86_64 Binary (~2.6 MB)
    │
lambda.zip              ← Deployment-Artefakt (~756 KB)
    │
    ▼ terraform apply
    │
AWS Lambda (provided.al2023)
    └── HTTP-Loop: GET /invocation/next → handle → POST /response
```

Die Binary kommuniziert direkt über POSIX-Sockets mit dem [Lambda Runtime API](https://docs.aws.amazon.com/lambda/latest/dg/runtimes-api.html).

## Projektstruktur

```
awsLambdaNative/
├── handler.scala     # Lambda Handler (Scala 3 / Scala Native, POSIX HTTP)
├── build.sh          # Build-Skript via Docker (amazonlinux:2023)
├── main.tf           # Terraform: Lambda + IAM + Function URL
├── dist/
│   └── bootstrap     # Generierte native Binary (nach build.sh)
└── lambda.zip        # Deployment-Artefakt (nach build.sh)
```

## Voraussetzungen

### Lokaler Build (Linux/Ubuntu)

```bash
# Einmalig installieren:
sudo apt install -y clang libcurl4-openssl-dev libidn2-dev zlib1g-dev
```

| Paket                  | Zweck                                      |
|------------------------|--------------------------------------------|
| `clang`                | C/LLVM Compiler (von Scala Native benötigt) |
| `libcurl4-openssl-dev` | STTP curl-Backend (HTTP-Client)            |
| `libidn2-dev`          | Internationale Domain-Namen (curl-Abhängigkeit) |
| `zlib1g-dev`           | Komprimierung (Scala Native Runtime)       |

Außerdem:
- [Scala CLI](https://scala-cli.virtuslab.org/) installiert
- Java 17+
- [Terraform](https://developer.hashicorp.com/terraform/install) >= 1.9
- AWS Credentials in `~/.aws/credentials`

## Build

```bash
./build.sh
```

Das Skript kompiliert `handler.scala` via Scala CLI direkt lokal zu `dist/bootstrap` (native ELF x86_64) und packt es als `lambda.zip`.

## Deployment

```bash
terraform init
terraform plan
terraform apply
# Output: function_url = "https://<id>.lambda-url.eu-central-1.on.aws/"
```

## Aufruf

```bash
curl "$(terraform output -raw function_url)?name=Gunther"
```

Antwort:
```json
{
  "statusCode": 200,
  "headers": {"Content-Type": "application/json", "X-Powered-By": "Scala 3.3.8 / Scala Native 0.5.12"},
  "body": "{\"message\":\"Hello, Gunther! You called: /\",\"path\":\"/\",\"name\":\"Gunther\"}"
}
```

## Vergleich mit ScalaJS-Version

| Eigenschaft        | awsLambdaScalaJS       | awsLambdaNative             |
|--------------------|------------------------|-----------------------------|
| Runtime            | Node.js 22.x           | provided.al2023 (kein JVM)  |
| Binary-Größe (ZIP) | ~40 KB                 | ~756 KB                     |
| Cold Start         | ~150ms                 | **<10ms** (kein VM-Start)   |
| Memory             | 256 MB                 | **128 MB** (ausreichend)    |
| Build-Tool         | scala-cli direkt       | scala-cli direkt (lokal) |
| HTTP-Client        | `java.net.URL`         | POSIX Sockets               |

## Aufräumen

```bash
terraform destroy
```
