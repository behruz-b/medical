CREATE TABLE "Analysis_Results" ("id" SERIAL PRIMARY KEY,
                     "analysis_image_name" VARCHAR NOT NULL,
                     "created_at" TIMESTAMP NOT NULL,
                     "customer_id" VARCHAR NOT NULL);

ALTER TABLE "Analysis_Results" ADD COLUMN sms_link_click VARCHAR NULL;
ALTER TABLE "Analysis_Results" ADD COLUMN analysis_type VARCHAR NOT NULL DEFAULT '-';
ALTER TABLE "Analysis_Results" ADD COLUMN analysis_group VARCHAR NOT NULL DEFAULT '-';

DROP TABLE "Analysis_Results";