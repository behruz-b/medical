CREATE TABLE "Analysis_Results" ("id" SERIAL PRIMARY KEY,
                     "analysis_image_name" VARCHAR NOT NULL,
                     "created_at" TIMESTAMP NOT NULL,
                     "customer_id" VARCHAR NOT NULL);

DROP TABLE "Analysis_Results";