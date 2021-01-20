CREATE TABLE "Users" ("id" SERIAL PRIMARY KEY,
                     "created_at" TIMESTAMP NOT NULL,
                     "firstname" VARCHAR NOT NULL,
                     "lastname" VARCHAR NOT NULL,
                     "phone" VARCHAR NOT NULL,
                     "role" VARCHAR NOT NULL,
                     "company_code" VARCHAR NOT NULL,
                     "login" VARCHAR NOT NULL,
                     "password" VARCHAR NOT NULL);

INSERT INTO "Users" ("id","created_at","firstname","lastname", "phone", "role", "company_code", "login", "password")
 VALUES (5,'2021-01-12 15:32:22.196016','doc1','superuser','998994461230','doctor','all','shifokor1','doc123');

INSERT INTO "Users" ("id","created_at","firstname","lastname", "phone", "role", "company_code", "login", "password")
 VALUES (6,'2021-01-12 15:32:22.196016','reg1','superuser','998994461230','reg','all','admin1','reg123');


DROP TABLE "Users";