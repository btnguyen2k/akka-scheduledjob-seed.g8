package com.github.btnguyen2k.akkascheduledjob;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.github.ddth.akka.AkkaUtils;
import com.github.ddth.akka.scheduling.tickfanout.MultiNodeQueueBasedTickFanOutActor;
import com.github.ddth.akka.scheduling.tickfanout.SingleNodeTickFanOutActor;
import com.github.ddth.commons.utils.TypesafeConfigUtils;
import com.github.ddth.dlock.IDLock;
import com.github.ddth.dlock.IDLockFactory;
import com.github.ddth.dlock.impl.AbstractDLockFactory;
import com.github.ddth.dlock.impl.inmem.InmemDLockFactory;
import com.github.ddth.dlock.impl.redis.RedisDLockFactory;
import com.github.ddth.queue.IQueue;
import com.github.ddth.queue.IQueueFactory;
import com.github.ddth.queue.QueueSpec;
import com.github.ddth.queue.impl.AbstractQueueFactory;
import com.github.ddth.queue.impl.universal.idstr.UniversalInmemQueueFactory;
import com.github.ddth.queue.impl.universal.idstr.UniversalRedisQueueFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigResolveOptions;
import io.undertow.Undertow;
import io.undertow.util.Headers;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Application's bootstrap class.
 *
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since template-0.1.0
 */
public class Bootstrap {

    private static Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

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

    private static Stack<Runnable> shutdownHooks = new Stack<>();

    private static ActorSystem buildActorSystem(Config config) {
        ActorSystem actorSystem = AkkaUtils.createActorSystem("default", config);
        LOGGER.info("Actor system: " + actorSystem);
        shutdownHooks.push(() -> {
            LOGGER.info("Shutting down: " + actorSystem);
            actorSystem.terminate();
        });
        return actorSystem;
    }

    private static Undertow buildUndertowServer(Config config) {
        Undertow server = Undertow.builder().addHttpListener(8080, "localhost")
                .setHandler(exchange -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("Hello World");
                }).build();
        shutdownHooks.push(() -> {
            LOGGER.info("Shutting down: " + server);
            server.stop();
        });
        return server;
    }

    private static IDLockFactory buildDlockFactory(Config config) {
        String type = TypesafeConfigUtils
                .getStringOptional(config, "ddth-akka-scheduling.dlock-backend.type").orElse(null);
        AbstractDLockFactory dLockFactory;
        if (StringUtils.equalsAnyIgnoreCase("redis", type)) {
            String redisHostAndPort = TypesafeConfigUtils.getStringOptional(config,
                    "ddth-akka-scheduling.dlock-backend.redis-host-and-port")
                    .orElse("localhost:6379");
            String redisPassword = TypesafeConfigUtils
                    .getStringOptional(config, "ddth-akka-scheduling.dlock-backend.redis-password")
                    .orElse(null);
            LOGGER.info("Creating Redis dlock factory [" + redisHostAndPort + "]...");

            dLockFactory = new RedisDLockFactory().setRedisHostAndPort(redisHostAndPort)
                    .setRedisPassword(redisPassword);
            dLockFactory.init();
        } else {
            LOGGER.info("Creating in-memory dlock factory...");
            dLockFactory = new InmemDLockFactory();
            dLockFactory.init();
        }
        shutdownHooks.push(() -> dLockFactory.destroy());

        return dLockFactory;
    }

    private static IQueueFactory<?, byte[]> buildQueue(Config config) {
        String type = TypesafeConfigUtils
                .getStringOptional(config, "ddth-akka-scheduling.queue-backend.type").orElse(null);
        AbstractQueueFactory queueFactory;
        if (StringUtils.equalsAnyIgnoreCase("redis", type)) {
            String redisHostAndPort = TypesafeConfigUtils.getStringOptional(config,
                    "ddth-akka-scheduling.queue-backend.redis-host-and-port")
                    .orElse("localhost:6379");
            String redisPassword = TypesafeConfigUtils
                    .getStringOptional(config, "ddth-akka-scheduling.queue-backend.redis-password")
                    .orElse(null);
            LOGGER.info("Creating Redis queue factory [" + redisHostAndPort + "]...");

            UniversalRedisQueueFactory qFactory = new UniversalRedisQueueFactory();
            qFactory.setDefaultHostAndPort(redisHostAndPort);
            qFactory.setDefaultPassword(redisPassword);
            qFactory.init();
            queueFactory = qFactory;
        } else {
            LOGGER.info("Creating in-memory queue factory...");
            queueFactory = new UniversalInmemQueueFactory();
            queueFactory.init();
        }
        shutdownHooks.push(() -> queueFactory.destroy());

        return queueFactory;
    }

    private static void initTickFanOutActor(Config config, ActorSystem actorSystem) {
        boolean isMultiNodeMode = TypesafeConfigUtils
                .getBooleanOptional(config, "ddth-akka-scheduling.multi-node-mode")
                .orElse(Boolean.FALSE).booleanValue();
        ActorRef tickFanOut;
        if (!isMultiNodeMode) {
            //single-node mode
            tickFanOut = SingleNodeTickFanOutActor.newInstance(actorSystem);
        } else {
            //multi-node mode
            IDLockFactory dlockFactory = buildDlockFactory(config);
            String dlockName = TypesafeConfigUtils
                    .getStringOptional(config, "ddth-akka-scheduling.dlock-backend.lock-name")
                    .orElse("akka-scheduled-jobs");
            LOGGER.info("Creating dlock instance [" + dlockName + "]...");
            IDLock dlock = dlockFactory.createLock(dlockName);

            IQueueFactory<?, byte[]> queueFactory = buildQueue(config);
            String queueName = TypesafeConfigUtils
                    .getStringOptional(config, "ddth-akka-scheduling.queue-backend.queue-name")
                    .orElse("akka-scheduled-jobs");
            LOGGER.info("Creating queue instance [" + queueName + "]...");
            IQueue<?, byte[]> queue = queueFactory.getQueue(new QueueSpec(queueName));

            long queuePollSleepMs = TypesafeConfigUtils
                    .getLongOptional(config, "ddth-akka-scheduling.queue-poll-sleep-ms")
                    .orElse(MultiNodeQueueBasedTickFanOutActor.DEFAULT_QUEUE_POLL_SLEEP_MS)
                    .longValue();
            long dlockTimeMs = TypesafeConfigUtils
                    .getLongOptional(config, "ddth-akka-scheduling.dlock-time-ms")
                    .orElse(MultiNodeQueueBasedTickFanOutActor.DEFAULT_DLOCK_TIME_MS).longValue();

            tickFanOut = MultiNodeQueueBasedTickFanOutActor.newInstance(actorSystem, dlock,
                    queue, queuePollSleepMs, dlockTimeMs);
        }

        LOGGER.info("Tick fan-out: " + tickFanOut);
        shutdownHooks.push(() -> actorSystem.stop(tickFanOut));
    }

    private static void initWorkers(Config config, ActorSystem actorSystem) {
        List<String> workerClazzs = TypesafeConfigUtils
                .getStringListOptional(config, "ddth-akka-scheduling.workers")
                .orElse(Collections.emptyList());
        if (workerClazzs.size() != 0) {
            for (String clazz : workerClazzs) {
                try {
                    Props props = Props.create(Class.forName(clazz));
                    LOGGER.info("Created worker " + actorSystem.actorOf(props));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } else {
            LOGGER.warn("No worker defined! Defined list of workers in configuration key "
                    + "[ddth-akka-scheduling.workers]!");
        }
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            while (!shutdownHooks.isEmpty()) {
                try {
                    shutdownHooks.pop().run();
                } catch (Exception e) {
                    LOGGER.warn(e.getMessage(), e);
                }
            }
        }));

        Config appConfig = loadConfig();

        Undertow server = buildUndertowServer(appConfig);
        server.start();

        ActorSystem actorSystem = buildActorSystem(appConfig);
        initTickFanOutActor(appConfig, actorSystem);
        initWorkers(appConfig, actorSystem);
    }
}
