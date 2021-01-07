CREATE TABLE "Users" ("id" SERIAL PRIMARY KEY,
                     "firstname" VARCHAR NOT NULL,
                     "lastname" VARCHAR NOT NULL,
                     "phone" VARCHAR NOT NULL,
                     "email" VARCHAR NULL,
                     "role" VARCHAR NOT NULL,
                     "login" VARCHAR NOT NULL,
                     "password" VARCHAR NOT NULL);
