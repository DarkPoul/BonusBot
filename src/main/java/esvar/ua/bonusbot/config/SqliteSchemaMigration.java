package esvar.ua.bonusbot.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SqliteSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public SqliteSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isSqlite()) {
            return;
        }

        if (!tableExists("tickets") || !isTicketsUserIdNotNull()) {
            return;
        }

        jdbcTemplate.execute((connection) -> {
            try (var statement = connection.createStatement()) {
                statement.execute("BEGIN TRANSACTION");
                statement.execute("""
                        CREATE TABLE tickets_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            campaign_id BIGINT NOT NULL,
                            user_id BIGINT,
                            code VARCHAR(4) NOT NULL,
                            created_at TIMESTAMP NOT NULL,
                            CONSTRAINT uk_ticket_campaign_code UNIQUE (campaign_id, code),
                            CONSTRAINT fk_tickets_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
                            CONSTRAINT fk_tickets_user FOREIGN KEY (user_id) REFERENCES users(id)
                        )
                        """);
                statement.execute("""
                        INSERT INTO tickets_new (id, campaign_id, user_id, code, created_at)
                        SELECT id, campaign_id, user_id, code, created_at FROM tickets
                        """);
                statement.execute("DROP TABLE tickets");
                statement.execute("ALTER TABLE tickets_new RENAME TO tickets");
                statement.execute("COMMIT");
            } catch (Exception e) {
                connection.createStatement().execute("ROLLBACK");
                throw e;
            }
            return null;
        });
    }

    private boolean isSqlite() {
        String url = jdbcTemplate.execute((connection) -> connection.getMetaData().getURL());
        return url != null && url.startsWith("jdbc:sqlite:");
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sqlite_master WHERE type='table' AND name = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private boolean isTicketsUserIdNotNull() {
        return jdbcTemplate.query(
                "PRAGMA table_info('tickets')",
                (rs) -> {
                    while (rs.next()) {
                        String name = rs.getString("name");
                        if (!"user_id".equals(name)) {
                            continue;
                        }
                        return rs.getInt("notnull") == 1;
                    }
                    return false;
                }
        );
    }
}
