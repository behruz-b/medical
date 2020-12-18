CREATE TABLE "Patient"(
    "id" SERIAL NOT NULL PRIMARY KEY,
    "created_at" DATE NOT NULL,
    "first_name" VARCHAR NOT NULL,
    "last_name" VARCHAR NOT NULL,
    "login"     VARCHAR NOT NULL,
    "password" VARCHAR NOT NULL,
    "passport_s_n" VARCHAR NOT NULL,
    "phone_number" VARCHAR NOT NULL,
    "email" VARCHAR NULL,
    "costumer_id" VARCHAR NOT NULL
);