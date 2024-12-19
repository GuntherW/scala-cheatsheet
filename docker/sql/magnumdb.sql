-- Database
CREATE DATABASE magnumdb;
\c magnumdb;


CREATE TABLE IF NOT EXISTS person (
     id      UUID           PRIMARY KEY,
     name    VARCHAR        NOT NULL,
     email   VARCHAR UNIQUE NOT NULL,
     color   VARCHAR        NOT NULL,
     created timestamptz    NOT NULL default NOW(),
     ts timestamp,
     date date
);
INSERT INTO person (id, name, email, color) VALUES ('5e195287-37ad-494d-a73e-c2bca6bb9bda', 'Magnum', 'a@b.c', 'red' );

CREATE TABLE geotest (
    id bigint  primary key,
    pnts point not null
);

CREATE TABLE IF NOT EXISTS mult_id (
      id      UUID           NOT NULL,
      name    VARCHAR        NOT NULL,
      email   VARCHAR UNIQUE NOT NULL,
      primary key (id, name)
);
INSERT INTO mult_id (id, name, email) VALUES ('5e195287-37ad-494d-a73e-c2bca6bb9bda', 'Magnum', 'a@b.c');