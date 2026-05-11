-- Table for using database messaging service
CREATE TABLE IF NOT EXISTS "${tablePrefix}messaging" (
    "id" INTEGER PRIMARY KEY AUTOINCREMENT,
    "message" TEXT NOT NULL,
    "timestamp" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Table for storing player cooldowns
CREATE TABLE IF NOT EXISTS "${tablePrefix}cooldowns" (
    "uuid" BLOB NOT NULL,
    "cooldown_type" TEXT NOT NULL,
    "cooldown_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE("uuid", "cooldown_type")
);