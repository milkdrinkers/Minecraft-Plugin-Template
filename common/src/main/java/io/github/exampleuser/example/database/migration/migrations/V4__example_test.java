package io.github.exampleuser.example.database.migration.migrations;

import io.github.exampleuser.example.database.migration.MigrationUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;

import java.sql.Connection;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.SQLDataType.VARCHAR;

/**
 * Example Java Flyway migration using jOOQ.
 * This is useful if you don't like sql files or you want do programmatic processing of data when migrating.
 */
@SuppressWarnings("unused")
public class V4__example_test extends BaseJavaMigration {
    @Override
    public void migrate(final Context flywayContext) throws Exception {
        final Connection connection = flywayContext.getConnection();
        final DSLContext context = MigrationUtils.getContext(connection); // get migration specific context

        // Don't use generated jOOQ results here as they may not exist in future java migrations (jOOQ only generates such from the flyway migration results)
        context
            .alterTable(name("some_list"))
            .addColumn(field("awdvawv", VARCHAR((32))))
            .execute();

        context
            .alterTable(name("some_list"))
            .addColumn(field("column_name", VARCHAR(32).notNull().defaultValue("default")))
            .execute();
    }
}