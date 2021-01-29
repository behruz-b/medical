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

INSERT INTO "Roles" ("id", "name", "code")
 VALUES (6, 'Manager', 'manager.role');

-- pass doc123
INSERT INTO "Users" ("id","created_at","firstname","lastname", "phone", "role", "company_code", "login", "password")
 VALUES (5,'2021-01-12 15:32:22.196016','doctor','superuser','998994461230','doctor.role','all','doc','c7ef8fc860e6b06ce37526b3e361700d');
-- pass reg123
INSERT INTO "Users" ("id","created_at","firstname","lastname", "phone", "role", "company_code", "login", "password")
 VALUES (6,'2021-01-12 15:32:22.196016','register','superuser','998994461230','register.role','all','reg','5c769a1e38d1af34a22a4fdf3e334409');
-- pass admin123
INSERT INTO "Users" ("id","created_at","firstname","lastname", "phone", "role", "company_code", "login", "password")
 VALUES (7,'2021-01-12 15:32:22.196016','admin','superuser','998994461230','admin.role','all','admin','0192023a7bbd73250516f069df18b500');

DROP TABLE "Users";
DROP TABLE "Roles";


