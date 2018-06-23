package com.github.btnguyen2k.akkascheduledjob.bootstrap;

import com.github.btnguyen2k.akkascheduledjob.RegistryGlobal;
import com.github.ddth.commons.utils.DPathUtils;
import com.github.ddth.commons.utils.TypesafeConfigUtils;
import com.typesafe.config.Config;
import com.zaxxer.hikari.HikariDataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Bootstrap data sources.
 *
 * <p>
 * Data sources are defined in {@code datasources} section in {@code application.conf}. Built
 * data sources are registered in {@link RegistryGlobal} as key {@code datasources} as a map
 * {ds-name:DataSource}
 * </p>
 *
 * <pre>
 * # Section to define data sources
 * datasources {
 *   # Name of the data source, in this case, the name is "default"
 *   default {
 *     jdbc-driver        = ${?JDBC_DRIVER}    #optional, JDBC driver class name (JDBC4: try without setting JDBC driver name, driver can be detected from connection url)
 *     jdbc-url           = ${?JDBC_URL}       #JDBC connection url
 *     jdbc-username      = ${?JDBC_USERNAME}  #optional, DB username
 *     jdbc-password      = ${?JDBC_PASSWORD}  #optional, DB password
 *     max-pool-size      = 4                  #optional, max number of connections, default value = number of cores
 *     min-idle           = 1                  #optional, min number of idle connections, default value = 1
 *     connection-timeout = 5000               #optional, connection timeout in milliseconds, default value = 5000
 *     idle-timeout       = 0                  #optional, max number of milliseconds that a connection can sit idle in the pool, default value = 0 (idle connections stay forever)
 *     max-lifetime       = 0                  #optional, max number of milliseconds that a connection can stay in the pool, default value = 0 (connections stay forever)
 *     leak-detection-threshold = 900000       #optional, max number of milliseconds that a connection can stay outside the pool before being considered a leaked connection, default value = 900000
 *     connection-test-query    = ""           #optional, the SQL query to be executed to test the validity of connection, default value = "" (JDBC4: try without setting test query, as Connection.isValid() is recommended)
 *     connection-init-sql      = ""           #optional, the SQL string that will be executed on all new connections when they are created, default value = ""
 *   }
 * }
 * </pre>
 *
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.1.1
 */
public class DataSourcesBootstrapper implements Runnable {

    private final Logger LOGGER = LoggerFactory.getLogger(DataSourcesBootstrapper.class);

    private DataSource buildDataSource(String dsName, Object dsConf) {
        LOGGER.info("Building DataSource [" + dsName + "]...");
        HikariDataSource ds = new HikariDataSource();
        RegistryGlobal.addShutdownHook(() -> ds.close());
        String driverClass = DPathUtils.getValue(dsConf, "jdbc-driver", String.class);
        if (!StringUtils.isBlank(driverClass)) {
            ds.setDriverClassName(driverClass);
        }
        ds.setJdbcUrl(DPathUtils.getValue(dsConf, "jdbc-url", String.class));
        ds.setUsername(DPathUtils.getValue(dsConf, "jdbc-username", String.class));
        ds.setPassword(DPathUtils.getValue(dsConf, "jdbc-password", String.class));

        long connectionTimeoutMs = DPathUtils
                .getValueOptional(dsConf, "connection-timeout", Long.class).orElse(5000L);
        ds.setConnectionTimeout(connectionTimeoutMs);

        long idleTimeout = DPathUtils.getValueOptional(dsConf, "idle-timeout", Long.class)
                .orElse(0L);
        ds.setIdleTimeout(idleTimeout);

        long maxLifetime = DPathUtils.getValueOptional(dsConf, "max-lifetime", Long.class)
                .orElse(0L);
        ds.setMaxLifetime(maxLifetime);

        int maxPoolSize = DPathUtils.getValueOptional(dsConf, "max-pool-size", Integer.class)
                .orElse(Runtime.getRuntime().availableProcessors());
        int minIdle = DPathUtils.getValueOptional(dsConf, "min-idle", Integer.class).orElse(1);
        ds.setMaximumPoolSize(maxPoolSize);
        ds.setMinimumIdle(minIdle);

        long leakDetectionThreshold = DPathUtils
                .getValueOptional(dsConf, "leak-detection-threshold", Long.class).orElse(900000L);
        ds.setLeakDetectionThreshold(leakDetectionThreshold);

        String connectionTestQuery = DPathUtils.getValue(dsConf, "connection-test-query",
                String.class);
        if (connectionTestQuery != null) {
            ds.setConnectionTestQuery(connectionTestQuery);
        }

        String connectionInitSql = DPathUtils.getValue(dsConf, "connection-init-sql", String.class);
        if (connectionInitSql != null) {
            ds.setConnectionInitSql(connectionInitSql);
        }

        ds.setInitializationFailTimeout(-1);

        return ds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        Config config = RegistryGlobal.getAppConfig();
        Map<?, ?> confDataSources = TypesafeConfigUtils.getObject(config, "datasources", Map.class);
        if (confDataSources != null) {
            Map<String, DataSource> dataSources = new HashMap<>();
            confDataSources.entrySet().forEach(entry -> {
                String dsName = entry.getKey().toString();
                Object dsConf = entry.getValue();
                dataSources.put(dsName, buildDataSource(dsName, dsConf));
            });
            RegistryGlobal.putToGlobalStorage("datasources", dataSources);
        } else {
            LOGGER.info("No datasource defined! Defined datasources at config key [datasources]!");
        }
    }
}
