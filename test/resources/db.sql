CREATE TABLE "Patients_doc" ("id" SERIAL PRIMARY KEY,
                             "fullname" VARCHAR NOT NULL,
                             "phone" VARCHAR NOT NULL);

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
    "analysis_type"       VARCHAR   NOT NULL,
    "receive_method"      VARCHAR   NULL,
    "doc_full_name"       VARCHAR   NULL,
    "doc_phone"           VARCHAR   NULL,
    "delivery_status"     VARCHAR   NULL,
    "analysis_image_name" VARCHAR   NULL,
    "sms_link_click"      VARCHAR   NULL,
    "analysis_group"      VARCHAR   NOT NULL DEFAULT '-',
    patients_doc_id       INT       NULL
        CONSTRAINT "PatientsFkPatientsId" REFERENCES "Patients_doc" ("id") ON UPDATE CASCADE ON DELETE CASCADE,
    UNIQUE (customer_id)
);
