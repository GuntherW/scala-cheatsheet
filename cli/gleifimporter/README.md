# GLEIF Data Importer

This Scala-CLI script downloads GLEIF (Global Legal Entity Identifier Foundation) data and imports it into a PostgreSQL database.
It downloads an zip file, unzips the zipfile into an xml file, parses the xml file and imports it into the database.
If an zip file exists already, it should not be downloaded again.

## What Data is Imported

- **Level 1 Data**: Legal Entity Identifiers (LEI) with who-is-who information
- **Level 2 Data**: Relationship records (who owns whom)

## Data Format

The GLEIF provides data in multiple formats:
- **XML (LEI-CDF)**: Standard format - used here
- **CSV**: Available via data.world
- **RDF**: Graph format

The **Concatenated Files** contain **historical data** (all LEIs ever published), while Golden Copy Files contain only current snapshots.

## Usage

```bash
# Run with defaults (localhost:5434, gleif database)
scala-cli run GleifImporter.scala

# Custom database connection
scala-cli run GleifImporter.scala -- --db-url jdbc:postgresql://host:port/db --db-user user --db-password pass

# With custom data directory
scala-cli run GleifImporter.scala -- --data-dir /path/to/data
```

## Database Schema

The script uses Flyway migrations in `migrations/` to create:
- `gleif_lei_records` - Main LEI data (id, legal_name, legal_name_language, initial_registration_date, last_update_date, status, next_renewal_date, imported_at)

## Requirements

- Scala CLI 1.0+
- Java 25
- PostgreSQL 16+ with Flyway support
- Running PostgreSQL container from `~/projekte/ba/esap-hub-service/docker-compose.yml`
  - Database: `gleif` (created automatically on container init)
  - Credentials: `esap_user` / `esap_password`
  - Port: `5434`

## Data Source

- **LEI-CDF XSD Schema**: https://www.gleif.org/lei-data/access-and-use-lei-data/level-1-data-lei-cdf-3-1-format/2021-03-04_lei-cdf-v3-1.xsd
- **Concatenated Files API**: https://leidata.gleif.org/api/v1/concatenated-files/
- **LEI Data (latest)**: https://leidata.gleif.org/api/v1/concatenated-files/lei2/latest/zip
- **Files**: Updated daily, contain all historical LEI data
