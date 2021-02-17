CREATE TABLE "Patients_Analysis" ("id" SERIAL PRIMARY KEY,
                     "analysis_type" VARCHAR NOT NULL,
                     "analysis_group" VARCHAR NOT NULL,
                     "created_at" TIMESTAMP NOT NULL,
                     "customer_id" VARCHAR NOT NULL);

DROP TABLE "Patients_Analysis";