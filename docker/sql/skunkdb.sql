-- Database
CREATE DATABASE skunkdb;
\c skunkdb;


CREATE TABLE IF NOT EXISTS users (
     id      UUID    PRIMARY KEY,
     name    VARCHAR        NOT NULL,
     email   VARCHAR UNIQUE NOT NULL
);

INSERT INTO users (id, name, email) VALUES ('5e195287-37ad-494d-a73e-c2bca6bb9bda', 'Gunther', 'a@b.c' );