package com.github.btnguyen2k.akkascheduledjob;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.github.ddth.akka.AkkaUtils;
import com.github.ddth.akka.scheduling.tickfanout.MultiNodeQueueBasedTickFanOutActor;
import com.github.ddth.akka.scheduling.tickfanout.SingleNodeTickFanOutActor;
import com.github.ddth.commons.utils.TypesafeConfigUtils;
import com.github.ddth.commons.utils.ValueUtils;
import com.github.ddth.dlock.IDLock;
import com.github.ddth.dlock.IDLockFactory;
import com.github.ddth.dlock.impl.AbstractDLockFactory;
import com.github.ddth.dlock.impl.inmem.InmemDLockFactory;
import com.github.ddth.dlock.impl.redis.RedisDLockFactory;
import com.github.ddth.queue.IQueue;
import com.github.ddth.queue.QueueSpec;
import com.github.ddth.queue.impl.AbstractQueueFactory;
import com.github.ddth.queue.impl.BaseRedisQueueFactory;
import com.github.ddth.queue.impl.universal.idint.UniversalInmemQueueFactory;
import com.github.ddth.queue.impl.universal.idint.UniversalRedisQueueFactory;
import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Application's global registry.
 *
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since template-0.1.1
 */
public class RegistryGlobal {
    private final static Logger LOGGER = LoggerFactory.getLogger(RegistryGlobal.class);

    private final static Stack<Runnable> shutdownHooks = new Stack<>();

    /**
     * Add a shutdown hook, which to be called right before application's
     * shutdown.
     *
     * @param r
     */
    public static void addShutdownHook(Runnable r) {
        shutdownHooks.add(r);
    }

    /*----------------------------------------------------------------------*/

    private static ConcurrentMap<String, Object> globalStorage = new ConcurrentHashMap<>();

    /**
     * Remove an item from application's global storage.
     *
     * @param key
     * @return the previous value associated with {@code key}, or {@code null}
     *         if there was no mapping for {@code key}.
     */
    public static Object removeFromGlobalStorage(String key) {
        return globalStorage.remove(key);
    }

    /**
     * Put an item to application's global storage.
     *
     * @param key
     * @param value
     * @return the previous value associated with {@code key}, or {@code null}
     *         if there was no mapping for {@code key}.
     */
    public static Object putToGlobalStorage(String key, Object value) {
        if (value == null) {
            return removeFromGlobalStorage(key);
        } else {
            return globalStorage.put(key, value);
        }
    }

    /**
     * Get an item from application's global storage.
     *
     * @param key
     * @return
     */
    public static Object getFromGlobalStorage(String key) {
        return globalStorage.get(key);
    }

    /**
     * Get an item from application's global storage.
     *
     * @param key
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getFromGlobalStorage(String key, Class<T> clazz) {
        Object value = getFromGlobalStorage(key);
        return ValueUtils.convertValue(value, clazz);
    }

    /*----------------------------------------------------------------------*/
    private static ActorSystem actorSystem;
    private static Config appConfig;

    /**
     * Get application's configuration object.
     *
     * @return
     * @since 0.1.1
     */
    public static Config getAppConfig() {
        return appConfig;
    }

    /**
     * Get the {@link ActorSystem} instance.
     *
     * @return
     * @since 0.1.1
     */
    public static ActorSystem getActorSystem() {
        return actorSystem;
    }

    /**
     * Build the {@link ActorSystem}.
     *
     * @param config
     * @return
     */
    private static ActorSystem buildActorSystem(Config config) {
        ActorSystem actorSystem = AkkaUtils.createActorSystem("default", config);
        addShutdownHook(() -> actorSystem.terminate());
        LOGGER.info("Actor system: " + actorSystem);
        return actorSystem;
    }

    @SuppressWarnings("resource")
    private static IDLockFactory buildDlockFactory(Config config) {
        String type = TypesafeConfigUtils
                .getStringOptional(config, "ddth-akka-scheduling.dlock-backend.type").orElse(null);
        AbstractDLockFactory dLockFactory;
        if (StringUtils.equalsAnyIgnoreCase("redis", type)) {
            String redisHostAndPort = TypesafeConfigUtils
                    .getStringOptional(config,
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
        addShutdownHook(() -> dLockFactory.destroy());
        return dLockFactory;
    }

    @SuppressWarnings("resource")
    private static IQueue<?, byte[]> buildQueue(Config config) {
        String type = TypesafeConfigUtils
                .getStringOptional(config, "ddth-akka-scheduling.queue-backend.type").orElse(null);
        AbstractQueueFactory<?, ?, byte[]> queueFactory;
        if (StringUtils.equalsAnyIgnoreCase("redis", type)) {
            String redisHostAndPort = TypesafeConfigUtils
                    .getStringOptional(config,
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
        addShutdownHook(() -> queueFactory.destroy());

        String queueName = TypesafeConfigUtils
                .getStringOptional(config, "ddth-akka-scheduling.queue-backend.queue-name")
                .orElse("akka-scheduled-jobs");
        LOGGER.info("Creating queue instance [" + queueName + "]...");
        QueueSpec spec = new QueueSpec(queueName);
        spec.setField(QueueSpec.FIELD_EPHEMERAL_DISABLED, true);
        if (StringUtils.equalsAnyIgnoreCase("redis", type)) {
            spec.setField(BaseRedisQueueFactory.SPEC_FIELD_HASH_NAME, queueName + "_h")
                    .setField(BaseRedisQueueFactory.SPEC_FIELD_LIST_NAME, queueName + "_l")
                    .setField(BaseRedisQueueFactory.SPEC_FIELD_SORTED_SET_NAME, queueName + "_s");
        }
        return queueFactory.getQueue(spec);
    }

    /**
     * Initialize the tick fan-out actor.
     *
     * @param config
     * @param actorSystem
     */
    private static void initTickFanOutActor(Config config, ActorSystem actorSystem) {
        if (!config.hasPath("ddth-akka-scheduling")) {
            throw new RuntimeException("No configuration [ddth-akka-scheduling] found!");
        }

        boolean isMultiNodeMode = TypesafeConfigUtils
                .getBooleanOptional(config, "ddth-akka-scheduling.multi-node-mode")
                .orElse(Boolean.FALSE).booleanValue();
        ActorRef tickFanOut;
        if (!isMultiNodeMode) {
            // single-node mode
            tickFanOut = SingleNodeTickFanOutActor.newInstance(actorSystem);
        } else {
            // multi-node mode
            IDLockFactory dlockFactory = buildDlockFactory(config);
            String dlockName = TypesafeConfigUtils
                    .getStringOptional(config, "ddth-akka-scheduling.dlock-backend.lock-name")
                    .orElse("akka-scheduled-jobs");
            LOGGER.info("Creating dlock instance [" + dlockName + "]...");
            IDLock dlock = dlockFactory.createLock(dlockName);

            IQueue<?, byte[]> queue = buildQueue(config);

            long queuePollSleepMs = TypesafeConfigUtils
                    .getLongOptional(config, "ddth-akka-scheduling.queue-poll-sleep-ms")
                    .orElse(MultiNodeQueueBasedTickFanOutActor.DEFAULT_QUEUE_POLL_SLEEP_MS)
                    .longValue();
            long dlockTimeMs = TypesafeConfigUtils
                    .getLongOptional(config, "ddth-akka-scheduling.dlock-time-ms")
                    .orElse(MultiNodeQueueBasedTickFanOutActor.DEFAULT_DLOCK_TIME_MS).longValue();

            tickFanOut = MultiNodeQueueBasedTickFanOutActor.newInstance(actorSystem, dlock, queue,
                    queuePollSleepMs, dlockTimeMs);
        }
        LOGGER.info("Tick fan-out: " + tickFanOut);
        addShutdownHook(() -> actorSystem.stop(tickFanOut));
    }

    /**
     * Initialize application's bootstrappers.
     *
     * @param config
     * @since 0.1.1
     */
    private static void initBootstrappers(Config config) {
        List<String> classNames = TypesafeConfigUtils.getStringListOptional(config, "bootstrappers")
                .orElse(Collections.emptyList());
        if (classNames != null && classNames.size() != 0) {
            for (String className : classNames) {
                try {
                    Class<?> clazz = Class.forName(className);
                    if (Runnable.class.isAssignableFrom(clazz)) {
                        LOGGER.info("Bootstrapping [" + className + "]...");
                        ((Runnable) clazz.newInstance()).run();
                    } else {
                        LOGGER.warn("Bootstrapper [" + className + "] must implement ["
                                + Runnable.class + "]!");
                    }
                } catch (ClassNotFoundException cnfe) {
                    LOGGER.error("Error: Class [" + className + "] not found!", cnfe);
                } catch (IllegalAccessException | InstantiationException e) {
                    LOGGER.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
        } else {
            LOGGER.info("No bootstrapper defined! Defined list of bootstrappers at config key "
                    + "[bootstrappers]!");
        }
    }

    /**
     * Initialize workers.
     *
     * @param config
     * @param actorSystem
     */
    private static void initWorkers(Config config, ActorSystem actorSystem) {
        List<String> workerClazzs = TypesafeConfigUtils
                .getStringListOptional(config, "ddth-akka-scheduling.workers")
                .orElse(Collections.emptyList());
        if (workerClazzs != null && workerClazzs.size() != 0) {
            for (String clazz : workerClazzs) {
                try {
                    Props props = Props.create(Class.forName(clazz));
                    LOGGER.info("Created worker " + actorSystem.actorOf(props));
                } catch (ClassNotFoundException cnfe) {
                    LOGGER.error("Error: Class [" + clazz + "] not found!", cnfe);
                }
            }
        } else {
            LOGGER.warn("No worker defined! Defined list of workers at config key "
                    + "[ddth-akka-scheduling.workers]!");
        }
    }

    /*----------------------------------------------------------------------*/

    /**
     * Initializing method.
     *
     * @param config
     */
    public static void init(Config config) {
        appConfig = config;
        actorSystem = buildActorSystem(config);
        initTickFanOutActor(config, actorSystem);
        initBootstrappers(config);
        initWorkers(config, actorSystem);
    }

    /**
     * Clean-up method.
     */
    public static void destroy() {
        while (!shutdownHooks.isEmpty()) {
            try {
                shutdownHooks.pop().run();
            } catch (Exception e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }
    }
}
