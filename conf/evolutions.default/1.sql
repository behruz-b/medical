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
    "analysis_type"       VARCHAR NOT NULL,
    "receive_method"      VARCHAR   NULL,
    "doc_full_name"       VARCHAR   NULL,
    "doc_phone"           VARCHAR   NULL,
    "delivery_status"     VARCHAR   NULL,
    "analysis_image_name" VARCHAR   NULL
);

ALTER TABLE "Patients" ADD COLUMN sms_link_click VARCHAR NULL;
ALTER TABLE "Patients" ADD COLUMN analysis_group VARCHAR NOT NULL DEFAULT '-';
ALTER TABLE "Patients" ADD CONSTRAINT customer_id_unique UNIQUE (customer_id);
ALTER TABLE "Patients" ADD COLUMN patients_doc_id INT NULL CONSTRAINT "PatientsFkPatientsId" REFERENCES "Patients_doc" ("id") ON UPDATE CASCADE ON DELETE CASCADE

DROP TABLE "Patients";