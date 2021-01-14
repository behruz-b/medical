CREATE ROLE m_user WITH CREATEDB CREATEROLE LOGIN ENCRYPTED PASSWORD '123';
CREATE DATABASE medical;
ALTER DATABASE medical OWNER TO m_user;
CREATE TABLE "Patients"
(
    "id"                  SERIAL PRIMARY KEY,
    "created_at"          TIMESTAMP NOT NULL,
    "firstname"           VARCHAR   NOT NULL,
    "lastname"            VARCHAR   NOT NULL,
    "phone"               VARCHAR   NOT NULL,
    "email"               VARCHAR   NULL,
    "passport"            VARCHAR   NOT NULL,
    "customer_id"         VARCHAR   NOT NULL,
    "company_code"        VARCHAR   NOT NULL,
    "login"               VARCHAR   NOT NULL,
    "password"            VARCHAR   NOT NULL,
    "analysis_image_name" VARCHAR   NULL
);

ALTER TABLE "Patients"
    ADD COLUMN receive_method VARCHAR NULL;