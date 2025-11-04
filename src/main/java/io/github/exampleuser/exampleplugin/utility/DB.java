package io.github.exampleuser.exampleplugin.utility;

import io.github.exampleuser.exampleplugin.database.handler.DatabaseHandler;
import io.github.exampleuser.exampleplugin.database.jooq.JooqContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Convenience class for accessing methods in {@link DatabaseHandler}
 */
public final class DB {
    private static DB INSTANCE;

    private static DB getInstance() {
        if (INSTANCE == null)
            INSTANCE = new DB();
        return INSTANCE;
    }

    private DatabaseHandler databaseHandler;

    private DatabaseHandler getDatabaseHandler() {
        return databaseHandler;
    }

    private void setDatabaseHandler(DatabaseHandler handler) {
        this.databaseHandler = handler;
    }

    /**
     * Used to set the globally used database handler instance for the plugin
     */
    @ApiStatus.Internal
    public static void init(DatabaseHandler handler) {
        getInstance().setDatabaseHandler(handler);
    }

    /**
     * Convenience method for {@link DatabaseHandler#isStarted()}
     *
     * @return if the database is ready
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isStarted() {
        DatabaseHandler handler = getInstance().getDatabaseHandler();
        if (handler == null)
            return false;

        return handler.isStarted();
    }

    /**
     * Convenience method for {@link DatabaseHandler#getConnection} to get a {@link Connection}
     *
     * @return the connection
     * @throws SQLException the sql exception
     */
    @NotNull
    public static Connection getConnection() throws SQLException {
        return getInstance().getDatabaseHandler().getConnection();
    }

    /**
     * Convenience method for {@link JooqContext#createContext(Connection)} to getConnection {@link DSLContext}
     *
     * @param con the con
     * @return the context
     */
    @NotNull
    public static DSLContext getContext(Connection con) {
        return getInstance().getDatabaseHandler().getJooqContext().createContext(con);
    }

    /**
     * Convenience method for accessing the {@link DatabaseHandler} instance
     *
     * @return the database handler
     */
    @NotNull
    public static DatabaseHandler getHandler() {
        return getInstance().getDatabaseHandler();
    }
}
