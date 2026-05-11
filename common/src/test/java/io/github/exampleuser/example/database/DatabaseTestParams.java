package io.github.exampleuser.example.database;

import io.github.exampleuser.example.database.handler.DatabaseType;

@SuppressWarnings("unused")
public record DatabaseTestParams(DatabaseType databaseType, DatabaseType requiredDatabaseType, String tablePrefix) {
    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private DatabaseType databaseType;
        private DatabaseType requiredDatabaseType;
        private String tablePrefix;

        private Builder() {}

        public Builder withDatabaseType(DatabaseType databaseType) {
            this.databaseType = databaseType;
            return this;
        }

        public Builder withRequiredDatabaseType(DatabaseType requiredDatabaseType) {
            this.requiredDatabaseType = requiredDatabaseType;
            return this;
        }

        public Builder withTablePrefix(String tablePrefix) {
            this.tablePrefix = tablePrefix;
            return this;
        }

        public DatabaseTestParams build() {
            return new DatabaseTestParams(databaseType, requiredDatabaseType, tablePrefix);
        }
    }
}
