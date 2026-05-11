CREATE TABLE IF NOT EXISTS "${tablePrefix}some_list" (
    "uuid" BINARY(16) NOT NULL,
    "_name" TINYTEXT NOT NULL,
    PRIMARY KEY (uuid)
);

CREATE TABLE IF NOT EXISTS "${tablePrefix}test" (
    "player_name" TINYTEXT NOT NULL
);

ALTER TABLE "${tablePrefix}test"
	ADD COLUMN "test" INT NULL;
