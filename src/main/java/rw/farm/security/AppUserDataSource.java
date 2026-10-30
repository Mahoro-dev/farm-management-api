package rw.farm.security;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

/**
 * Stamps the MySQL session variable read by the animal audit triggers.
 */
public class AppUserDataSource extends DelegatingDataSource {

    private static final Pattern UUID_TEXT = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    public AppUserDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        bind(connection);
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = super.getConnection(username, password);
        bind(connection);
        return connection;
    }

    private void bind(Connection connection) throws SQLException {
        String userId = SecurityUtils.userIdOrNull();
        String sql = userId != null && UUID_TEXT.matcher(userId).matches()
                ? "SET @app_user_id = '" + userId + "'"
                : "SET @app_user_id = NULL";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
