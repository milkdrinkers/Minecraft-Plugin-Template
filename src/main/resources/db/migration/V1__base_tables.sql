-- Table for using database messaging service
CREATE TABLE IF NOT EXISTS "${tablePrefix}messaging" (
    "id" INT AUTO_INCREMENT NOT NULL,
    "message" TEXT NOT NULL,
    "timestamp" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY ("id")
);

-- Table for storing player cooldowns
CREATE TABLE IF NOT EXISTS "${tablePrefix}cooldowns" (
    "uuid" BINARY(16) NOT NULL,
    "cooldown_type" VARCHAR(255) NOT NULL,
    "cooldown_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY "${tablePrefix}unique_uuid_type" ("uuid", "cooldown_type")
);