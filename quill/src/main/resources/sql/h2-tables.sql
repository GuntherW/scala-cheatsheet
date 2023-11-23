CREATE TABLE person (
   id INT PRIMARY KEY NOT NULL,
   first_name VARCHAR NOT NULL,
   last_name VARCHAR,
   age INTEGER
);

CREATE TABLE book (
    id INT PRIMARY KEY NOT NULL,
    owner_id INT NOT NULL,
    title VARCHAR,
    CONSTRAINT fk_book_owner
                  FOREIGN KEY (owner_id) REFERENCES person(id)
                  ON DELETE CASCADE
                  ON UPDATE RESTRICT
);