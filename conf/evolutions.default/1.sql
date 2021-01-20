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
    "customer_id"         VARCHAR   NOT NULL,
    "company_code"        VARCHAR   NOT NULL,
    "login"               VARCHAR   NOT NULL,
    "password"            VARCHAR   NOT NULL,
    "address"             VARCHAR   NOT NULL,
    "date_of_birth"       TIMESTAMP NOT NULL,
    "analyse_type"        VARCHAR NOT NULL,
    "receive_method"      VARCHAR   NULL,
    "doc_full_name"       VARCHAR   NULL,
    "doc_phone"           VARCHAR   NULL,
    "delivery_status"     VARCHAR   NULL,
    "analysis_image_name" VARCHAR   NULL
);

DROP TABLE "Patients";