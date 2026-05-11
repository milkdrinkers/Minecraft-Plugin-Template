package io.github.exampleuser.example.database.migration;

import io.github.exampleuser.example.utility.DB;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.sql.Connection;

/**
 * Utility class for Java based Flyway migrations.
 */
public final class MigrationUtils {
    /**
     * A safe way to get DSLContext inside Java migrations as they need to access different contexts during flyway migration build step and programmatic flyway migration.
     *
     * @return dsl context
     */
    public static DSLContext getContext(Connection connection) {
        DSLContext context;
        try {
            context = DB.getContext(connection); // Throws during the "flywayMigrate" build step, but works when running migration programmatically
        } catch (Exception e) {
            context = DSL.using(connection); // Used during flyway build step
        }
        return context;
    }
}
