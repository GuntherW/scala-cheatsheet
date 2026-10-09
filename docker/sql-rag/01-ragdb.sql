-- Init-Skript für den eigenständigen postgres-rag-Container (POSTGRES_DB=ragdb wird bereits beim
-- Containerstart angelegt, siehe docker-compose.yml) - läuft daher direkt gegen `ragdb`, kein `CREATE DATABASE`/`\c` nötig.

-- pgvector: fügt den Spaltentyp `vector` sowie Distanzoperatoren (<-> L2, <=> Cosine, <#> Inner Product) hinzu.
CREATE EXTENSION IF NOT EXISTS vector;

-- Feste Dimension der selbstgebauten Hashing-TF-IDF-Embeddings (siehe HashingTfIdfEmbedder.scala).
-- pgvector verlangt eine feste Dimension pro Spalte - das Hashing-Verfahren liefert diese unabhängig
-- vom tatsächlichen Vokabular des Korpus (siehe README, Abschnitt "Warum Hashing statt klassischem TF-IDF?").
CREATE TABLE document_chunks (
    id          serial PRIMARY KEY,
    source_file text NOT NULL,
    chunk_index integer NOT NULL,
    content     text NOT NULL,
    embedding   vector(512) NOT NULL
);

-- HNSW-Index für Cosine-Similarity-Suche: im Gegensatz zu ivfflat (siehe unten) ohne "Trainingsphase"/Mindestdatenmenge
-- zuverlässig nutzbar - wichtig für dieses Lernprojekt mit einem sehr kleinen Beispiel-Korpus (siehe README,
-- Abschnitt "ivfflat vs. hnsw"). ivfflat würde bei so wenigen Zeilen mit seinem Standard-Probing (1 von `lists`
-- Clustern) einen Großteil der Zeilen schlicht übersehen - kein Bug im eigenen Code, sondern ein bekanntes
-- pgvector-Verhalten bei zu kleinen Datensätzen.
CREATE INDEX document_chunks_embedding_idx ON document_chunks
    USING hnsw (embedding vector_cosine_ops);

-- IDF-Gewichte (ein Float pro Hash-Bucket) aus der Ingestion-Phase - werden beim Query-Embedding
-- der Nutzerfrage erneut benötigt, damit Ingestion- und Query-Vektoren im selben Vektorraum liegen.
CREATE TABLE tfidf_model (
    id         integer PRIMARY KEY DEFAULT 1,
    dims       integer NOT NULL,
    doc_count  integer NOT NULL,
    idf        double precision[] NOT NULL,
    CONSTRAINT single_row CHECK (id = 1)
);
