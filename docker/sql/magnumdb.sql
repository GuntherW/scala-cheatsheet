-- Database
CREATE DATABASE magnumdb;
\c magnumdb;


CREATE TABLE IF NOT EXISTS person (
     id      UUID    PRIMARY KEY,
     name    VARCHAR        NOT NULL,
     email   VARCHAR UNIQUE NOT NULL,
     color   VARCHAR NOT NULL,
     created timestamptz NOT NULL default NOW()
);

INSERT INTO person (id, name, email, color) VALUES ('5e195287-37ad-494d-a73e-c2bca6bb9bda', 'Magnum', 'a@b.c', 'red' );