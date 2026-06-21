package com.cloudys.requirementanalysis.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequirementAnalysisFlywayBaselineRegressionTest {

    @Test
    void configuredFlywayShouldApplyInitialSchemaInNonEmptySharedDatabase() throws Exception {
        DataSource dataSource = createSharedSchemaDataSource();
        createExistingSharedTable(dataSource);
        Properties properties = loadApplicationProperties();

        assertEquals("0", properties.getProperty("spring.flyway.baseline-version"),
                "requirement-analysis-service should baseline shared schemas at version 0");

        Flyway flyway = configuredFlyway(dataSource, properties);
        flyway.migrate();

        assertTrue(historyContainsVersion(dataSource, "requirement_analysis_flyway_schema_history", "0"),
                "configured Flyway should record the explicit version-0 baseline in a non-empty shared schema");
        assertTrue(historyContainsVersion(dataSource, "requirement_analysis_flyway_schema_history", "1"),
                "configured Flyway should baseline at version 0 and execute V1 in a non-empty shared schema");
        assertTrue(tableExists(dataSource, "spans"),
                "spans should exist even when the shared schema already contains tables");
        assertTrue(tableExists(dataSource, "context_runs"),
                "context_runs should exist even when the shared schema already contains tables");
    }

    @Test
    void backfillMigrationShouldRepairSchemaThatWasPreviouslyBaselinedAtVersionOne() throws Exception {
        DataSource dataSource = createSharedSchemaDataSource();
        createExistingSharedTable(dataSource);
        createBrokenFlywayHistory(dataSource);

        Flyway flyway = Flyway.configure()
                .cleanDisabled(true)
                .dataSource(dataSource)
                .locations("classpath:flyway/requirement-analysis-baseline-regression")
                .table("requirement_analysis_flyway_schema_history")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("0"))
                .load();

        flyway.migrate();

        assertTrue(tableExists(dataSource, "spans"),
                "repair migration should recreate spans for previously baselined databases");
        assertTrue(tableExists(dataSource, "context_runs"),
                "repair migration should recreate context_runs for previously baselined databases");
    }

    @Test
    void productionBackfillMigrationShouldExistForSkippedRequirementAnalysisSchema() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/migration/V2__requirement_analysis_schema_backfill.sql");

        assertTrue(resource.exists(), "production requirement-analysis backfill migration should be packaged with the service");

        String sql = new String(resource.getInputStream().readAllBytes());
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS spans"),
                "requirement-analysis backfill migration should recreate spans");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS context_runs"),
                "requirement-analysis backfill migration should recreate context_runs");
    }

    private static Flyway configuredFlyway(DataSource dataSource, Properties properties) {
        var configuration = Flyway.configure()
                .cleanDisabled(true)
                .dataSource(dataSource)
                .locations("classpath:flyway/requirement-analysis-baseline-regression")
                .table(properties.getProperty("spring.flyway.table"))
                .baselineOnMigrate(Boolean.parseBoolean(
                        properties.getProperty("spring.flyway.baseline-on-migrate", "false")));

        String baselineVersion = properties.getProperty("spring.flyway.baseline-version");
        if (baselineVersion != null && !baselineVersion.isBlank()) {
            configuration.baselineVersion(MigrationVersion.fromVersion(baselineVersion));
        }

        return configuration.load();
    }

    private static Properties loadApplicationProperties() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));
        Properties properties = factory.getObject();
        return properties != null ? properties : new Properties();
    }

    private static DataSource createSharedSchemaDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private static void createExistingSharedTable(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "CREATE TABLE manage_projects(id INT PRIMARY KEY)")) {
            statement.executeUpdate();
        }
    }

    private static void createBrokenFlywayHistory(DataSource dataSource) {
        Flyway oldConfiguration = Flyway.configure()
                .cleanDisabled(true)
                .dataSource(dataSource)
                .locations("classpath:flyway/requirement-analysis-baseline-regression")
                .table("requirement_analysis_flyway_schema_history")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("1"))
                .target(MigrationVersion.fromVersion("1"))
                .load();
        oldConfiguration.migrate();
    }

    private static boolean tableExists(DataSource dataSource, String tableName) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM information_schema.tables WHERE lower(table_name) = ?")) {
            statement.setString(1, tableName.toLowerCase());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    private static boolean historyContainsVersion(DataSource dataSource, String tableName, String version) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + tableName + " WHERE version = ?")) {
            statement.setString(1, version);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }
}
