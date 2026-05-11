-- Create a user table with a auto-incrementing column
CREATE TABLE IF NOT EXISTS "${tablePrefix}colors" (
    "color_id" INT AUTO_INCREMENT, -- Your auto-incrementing column cannot be "NOT NULL" because of SQLite
    "some_field" TINYTEXT NOT NULL,
    "enabled" TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY ("color_id")
);

-- The below examples are all the same with the foreign key being different

-- Create a user table with a auto-incrementing column, and unique column, and named foreign key constraint on column
CREATE TABLE IF NOT EXISTS "${tablePrefix}users" (
    "user_id" INT AUTO_INCREMENT,
    "user_uuid" BINARY(16) NOT NULL,
    "user_color" INT NOT NULL,
    PRIMARY KEY ("user_id"),
    CONSTRAINT "${tablePrefix}some_foreign_key_name_here" FOREIGN KEY ("user_color") REFERENCES "${tablePrefix}colors" ("color_id") ON DELETE CASCADE
);
CREATE UNIQUE INDEX "${tablePrefix}some_index_name_here" ON "${tablePrefix}users" ("user_uuid"); -- Indexes and Unique indexed must be created in separate statements due to SQLite

-- Create a user table with a auto-incrementing column, and unique column, and named foreign key constraint on column
CREATE TABLE IF NOT EXISTS "${tablePrefix}users2" (
    "user_id" INT AUTO_INCREMENT,
    "user_uuid" BINARY(16) NOT NULL,
    "user_color" INT NOT NULL,
    PRIMARY KEY ("user_id"),
    CONSTRAINT "${tablePrefix}some_foreign_key_name_here2" FOREIGN KEY ("user_color") REFERENCES "${tablePrefix}colors" ("color_id")
);
CREATE UNIQUE INDEX "${tablePrefix}some_index_name_here2" ON "${tablePrefix}users2" ("user_uuid"); -- Indexes and Unique indexed must be created in separate statements due to SQLite

-- Create a user table with a auto-incrementing column, and unique column, and unnamed foreign key on column
CREATE TABLE IF NOT EXISTS "${tablePrefix}users3" (
    "user_id" INT AUTO_INCREMENT,
    "user_uuid" BINARY(16) NOT NULL,
    "user_color" INT NOT NULL,
    PRIMARY KEY ("user_id"),
    FOREIGN KEY ("user_color") REFERENCES "${tablePrefix}colors" ("color_id")
);
CREATE UNIQUE INDEX "${tablePrefix}some_index_name_here3" ON "${tablePrefix}users3" ("user_uuid"); -- Indexes and Unique indexed must be created in separate statements due to SQLite
