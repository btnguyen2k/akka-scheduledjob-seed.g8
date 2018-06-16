package com.github.btnguyen2k.akkascheduledjob;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigResolveOptions;

/**
 * Application's bootstrap class.
 *
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since template-0.1.0
 */
public class Bootstrap {

    static {
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.showLogName", "false");
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "false");
    }

    private static Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

    /**
     * Load application's configuration from file.
     *
     * <p>
     * Configuration file is specified by system environment
     * {@code config.file}. If not specified, default configuration file
     * {@code conf/application.conf} will be used.
     * </p>
     *
     * @return
     */
    private static Config loadConfig() {
        final String DEFAULT_CONF_FILE = "conf/application.conf";
        String cmdConfigFile = System.getProperty("config.file", DEFAULT_CONF_FILE);
        File configFile = new File(cmdConfigFile);
        if (!configFile.isFile() || !configFile.canRead()) {
            if (StringUtils.equals(cmdConfigFile, DEFAULT_CONF_FILE)) {
                String msg = "Cannot read from config file [" + configFile.getAbsolutePath() + "]!";
                LOGGER.error(msg);
                throw new RuntimeException(msg);
            } else {
                LOGGER.warn("Configuration file [" + configFile.getAbsolutePath()
                        + "], is invalid or not readable, fallback to default!");
                configFile = new File(DEFAULT_CONF_FILE);
            }
        }
        LOGGER.info("Loading configuration from [" + configFile + "]...");
        if (!configFile.isFile() || !configFile.canRead()) {
            String msg = "Cannot read from config file [" + configFile.getAbsolutePath() + "]!";
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }
        Config config = ConfigFactory.parseFile(configFile)
                .resolve(ConfigResolveOptions.defaults().setUseSystemEnvironment(true));
        LOGGER.info("Application config: " + config);
        return config;
    }

    public static void main(String[] args) {
        Config appConfig = loadConfig();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> RegistryGlobal.destroy()));
        RegistryGlobal.init(appConfig);
    }
}
