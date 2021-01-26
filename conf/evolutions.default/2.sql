CREATE TABLE "Roles" ("id" SERIAL PRIMARY KEY,
                      "name" VARCHAR NOT NULL,
                      "code" VARCHAR NOT NULL,
                      UNIQUE(code));

CREATE TABLE "Users" ("id" SERIAL PRIMARY KEY,
                     "created_at" TIMESTAMP NOT NULL,
                     "firstname" VARCHAR NOT NULL,
                     "lastname" VARCHAR NOT NULL,
                     "phone" VARCHAR NOT NULL,
                     "role" VARCHAR CONSTRAINT  "UsersFkRoleCode" REFERENCES "Roles" ("code") ON UPDATE CASCADE ON DELETE CASCADE,
                     "company_code" VARCHAR NOT NULL,
                     "login" VARCHAR NOT NULL,
                     "password" VARCHAR NOT NULL,
                     UNIQUE(login));

INSERT INTO "Roles" ("id", "name", "code")
 VALUES (1, 'Doctor', 'doctor.role');

INSERT INTO "Roles" ("id", "name", "code")
 VALUES (2, 'Admin', 'admin.role');

INSERT INTO "Roles" ("id", "name", "code")
 VALUES (3, 'Register', 'register.role');

INSERT INTO "Users" ("id","created_at","firstname","lastname", "phone", "role", "company_code", "login", "password")
 VALUES (5,'2021-01-12 15:32:22.196016','doctor','superuser','998994461230','doctor.role','all','doc','doc123');

INSERT INTO "Users" ("id","created_at","firstname","lastname", "phone", "role", "company_code", "login", "password")
 VALUES (6,'2021-01-12 15:32:22.196016','register','superuser','998994461230','register.role','all','reg','reg123');

INSERT INTO "Users" ("id","created_at","firstname","lastname", "phone", "role", "company_code", "login", "password")
 VALUES (7,'2021-01-12 15:32:22.196016','admin','superuser','998994461230','admin.role','all','admin','reg123');

DROP TABLE "Users";
DROP TABLE "Roles";
