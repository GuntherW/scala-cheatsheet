-- Database
CREATE DATABASE booksdb;
\c booksdb;

-- Directors
CREATE TABLE authors (
   id serial NOT NULL, PRIMARY KEY (id),
   first_name character varying NOT NULL,
   last_name character varying NOT NULL,
   birth_year integer
);

-- Movies
CREATE TABLE books (
    id serial NOT NULL,
    title character varying NOT NULL,
    author_id integer NOT NULL
);

ALTER TABLE books
    ADD FOREIGN KEY (author_id) REFERENCES authors (id);

INSERT INTO authors (first_name, last_name, birth_year) VALUES ('Hans', 'GuckInDieLuft', 1990);
INSERT INTO authors (first_name, last_name, birth_year) VALUES ('Peter', 'Lustig', 1980);
INSERT INTO books (title, author_id) VALUES ('Scala ist super', 1);
INSERT INTO books (title, author_id) VALUES ('Programmieren ist super', 1);
INSERT INTO books (title, author_id) VALUES ('Das Leben des Werthers', 2);