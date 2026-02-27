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
# Run with defaults (localhost:5434, esap_hub database)
scala-cli run gleif-importer.sc

# Custom database connection
scala-cli run gleif-importer.sc -- --db-url jdbc:postgresql://host:port/db --db-user user --db-password pass

# With custom data directory
scala-cli run gleif-importer.sc -- --data-dir /path/to/data
```

## Database Schema

The script creates the following tables:
- `gleif_lei_records` - Main LEI data
- `gleif_lei_history` - Historical changes
- `gleif_registration_authorities` - LOU information
- `gleif_legal_forms` - Legal form codes
- `gleif_relationships` - Ownership relationships
- `gleif_import_log` - Import metadata

## Requirements

- Scala CLI 1.0+
- Java 25
- PostgreSQL 16+ with Flyway support

## Data Source

- **URL**: https://www.gleif.org/en/lei-data/gleif-concatenated-file
- **API**: https://database.gleif.org/api/v1/concatenated-files
- **Files**: Updated daily, contain all historical LEI data
