CREATE TABLE "Users" ("id" SERIAL PRIMARY KEY,
                     "created_at" TIMESTAMP NOT NULL,
                     "firstname" VARCHAR NOT NULL,
                     "lastname" VARCHAR NOT NULL,
                     "phone" VARCHAR NOT NULL,
                     "email" VARCHAR NULL,
                     "role" VARCHAR NOT NULL,
                     "company_code" VARCHAR NOT NULL,
                     "login" VARCHAR NOT NULL,
                     "password" VARCHAR NOT NULL);


DROP TABLE "Users";