CREATE DATABASE macro;
\c macro;

CREATE TABLE users (
	id INT not null,
	name VARCHAR(255) not null,
	age INT,
	hobbies VARCHAR(100) ARRAY
);
INSERT INTO users(id, name, age, hobbies) 
VALUES
	(1, 'John', 30, ARRAY['Reading','Painting','Gardening']),
	(2, 'Alice', 25, ARRAY['Swimming','Hiking','Photography']),
	(3, 'Bob', 35, ARRAY['Cooking','Traveling','Music']),
	(4, 'Emma', 28, ARRAY['Yoga','Reading','Dancing']),
	(5, 'Mike', 32, ARRAY['Sports','Gaming','Cooking'])
;

CREATE TABLE employees (
	id INT not null,
	position VARCHAR(255) not null,
	active BOOLEAN not null
);
INSERT INTO employees(id, position, active)
VALUES
	(1,'Engineer',TRUE),
	(2,'Manager',FALSE),
	(3,'Team Lead',FALSE),
	(4,'Intern',TRUE),
	(5,'Sales Rep',FALSE)
;