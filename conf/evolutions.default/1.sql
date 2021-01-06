CREATE ROLE m_user WITH CREATEDB CREATEROLE LOGIN ENCRYPTED PASSWORD '123';
CREATE DATABASE medical;
ALTER DATABASE medical OWNER TO m_user;
CREATE TABLE "Patients" ("id" SERIAL PRIMARY KEY,
                         "created_at" TIMESTAMP NOT NULL,
                         "firstname" VARCHAR NOT NULL,
                         "lastname" VARCHAR NOT NULL,
                         "phone" VARCHAR NOT NULL,
                         "email" VARCHAR NULL,
                         "passport" VARCHAR NOT NULL,
                         "customer_id" VARCHAR NOT NULL,
                         "login" VARCHAR NOT NULL,
                         "password" VARCHAR NOT NULL);

ALTER TABLE "Patients" ADD COLUMN analysis_image_name VARCHAR NULL;
