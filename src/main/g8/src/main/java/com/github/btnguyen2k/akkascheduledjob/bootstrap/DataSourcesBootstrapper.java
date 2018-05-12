package com.github.btnguyen2k.akkascheduledjob.bootstrap;

import com.github.btnguyen2k.akkascheduledjob.RegistryGlobal;
import com.github.ddth.commons.utils.DPathUtils;
import com.github.ddth.commons.utils.TypesafeConfigUtils;
import com.typesafe.config.Config;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Bootstrap data sources.
 *
 * <p>Data sources are defined in {@code datasources} section in {@code application.conf}. Built
 * data sources are registered in {@link RegistryGlobal} as key {@code datasources} as a map
 * {ds-name:DataSource}</p>
 *
 * <pre>
 * # Section to define data sources
 * datasources {
 *   # Name of the data source, in this case, the name is "default"
 *   default {
 *     jdbc-url      = "${JDBC_URL}"
 *     jdbc-username = "${JDBC_USERNAME}"
 *     jdbc-password = "${JDBC_PASSWORD}"
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
        ds.setJdbcUrl(DPathUtils.getValue(dsConf, "jdbc-url", String.class));
        ds.setUsername(DPathUtils.getValue(dsConf, "jdbc-username", String.class));
        ds.setPassword(DPathUtils.getValue(dsConf, "jdbc-password", String.class));

        long connectionTimeoutMs = DPathUtils
                .getValueOptional(dsConf, "connection-timeout", Long.class).orElse(5000L);
        ds.setConnectionTimeout(connectionTimeoutMs);

        long idleTimeout = DPathUtils.getValueOptional(dsConf, "idle-timeout", Long.class)
                .orElse(900000L);
        ds.setIdleTimeout(idleTimeout);

        long maxLifetime = DPathUtils.getValueOptional(dsConf, "max-lifetime", Long.class)
                .orElse(1800000L);
        ds.setMaxLifetime(maxLifetime);

        int maxPoolSize = DPathUtils.getValueOptional(dsConf, "max-pool-size", Integer.class)
                .orElse(Runtime.getRuntime().availableProcessors());
        int minIdle = DPathUtils.getValueOptional(dsConf, "min-idle", Integer.class).orElse(1);
        ds.setMaximumPoolSize(maxPoolSize);
        ds.setMinimumIdle(minIdle);

        long leakDetectionThreshold = DPathUtils
                .getValueOptional(dsConf, "leak-detection-threshold", Long.class).orElse(900000L);
        ds.setLeakDetectionThreshold(leakDetectionThreshold);

        String connectionTestQuery = DPathUtils
                .getValue(dsConf, "connection-test-query", String.class);
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
                dataSources.put("datasources." + dsName, buildDataSource(dsName, dsConf));
            });
            RegistryGlobal.putToGlobalStorage("datasources", dataSources);
        } else {
            LOGGER.info("No datasource defined! Defined datasources at config key [datasources]!");
        }
    }
}
