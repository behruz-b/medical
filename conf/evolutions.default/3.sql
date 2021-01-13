CREATE TABLE "Stats" ("id" SERIAL PRIMARY KEY,
                     "created_at" TIMESTAMP NOT NULL,
                     "company_code" VARCHAR NOT NULL,
                     "action" VARCHAR NOT NULL,
                     "ip_address" VARCHAR NOT NULL,
                     "user_agent" VARCHAR NOT NULL);

DROP TABLE "Stats";