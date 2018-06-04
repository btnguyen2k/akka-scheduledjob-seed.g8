package com.github.btnguyen2k.akkascheduledjob;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.ddth.akka.AkkaUtils;
import com.github.ddth.akka.scheduling.tickfanout.MultiNodePubSubBasedTickFanOutActor;
import com.github.ddth.akka.scheduling.tickfanout.SingleNodeTickFanOutActor;
import com.github.ddth.commons.utils.TypesafeConfigUtils;
import com.github.ddth.commons.utils.ValueUtils;
import com.github.ddth.dlock.IDLock;
import com.github.ddth.dlock.IDLockFactory;
import com.github.ddth.dlock.impl.AbstractDLockFactory;
import com.github.ddth.dlock.impl.inmem.InmemDLockFactory;
import com.github.ddth.dlock.impl.redis.RedisDLockFactory;
import com.github.ddth.pubsub.IPubSubHub;
import com.github.ddth.pubsub.impl.AbstractPubSubHub;
import com.github.ddth.pubsub.impl.universal.idint.UniversalInmemPubSubHub;
import com.github.ddth.pubsub.impl.universal.idint.UniversalRedisPubSubHub;
import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

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
        IDLockFactory dlockFactory = getFromGlobalStorage("dlock-factory", IDLockFactory.class);
        if (dlockFactory == null) {
            String dlockPrefix = TypesafeConfigUtils
                    .getStringOptional(config, "ddth-akka-scheduling.dlock-backend.lock-prefix")
                    .orElse(TypesafeConfigUtils.getString(config, "app.shortname"));
            AbstractDLockFactory factory;
            String type = TypesafeConfigUtils
                    .getStringOptional(config, "ddth-akka-scheduling.dlock-backend.type")
                    .orElse(null);
            if (StringUtils.equalsAnyIgnoreCase("redis", type)) {
                String redisHostAndPort = TypesafeConfigUtils
                        .getStringOptional(config,
                                "ddth-akka-scheduling.dlock-backend.redis-host-and-port")
                        .orElse("localhost:6379");
                String redisPassword = TypesafeConfigUtils.getStringOptional(config,
                        "ddth-akka-scheduling.dlock-backend.redis-password").orElse(null);
                LOGGER.info("Creating Redis dlock factory [" + redisHostAndPort + "]...");
                factory = new RedisDLockFactory().setRedisHostAndPort(redisHostAndPort)
                        .setRedisPassword(redisPassword).setLockNamePrefix(dlockPrefix).init();
            } else {
                LOGGER.info("Creating in-memory dlock factory...");
                factory = new InmemDLockFactory().setLockNamePrefix(dlockPrefix).init();
            }
            addShutdownHook(() -> factory.destroy());
            putToGlobalStorage("dlock-factory", factory);
            dlockFactory = factory;
        }
        return dlockFactory;
    }

    /**
     * 
     * @param config
     * @return
     * @since 0.1.2
     */
    @SuppressWarnings({ "resource", "unchecked" })
    private static IPubSubHub<?, byte[]> buildPubSubHub(Config config) {
        IPubSubHub<?, byte[]> pubSubHub = getFromGlobalStorage("pubsub-hub", IPubSubHub.class);
        if (pubSubHub == null) {
            String type = TypesafeConfigUtils
                    .getStringOptional(config, "ddth-akka-scheduling.pubsub-backend.type")
                    .orElse(null);
            AbstractPubSubHub<?, byte[]> hub;
            if (StringUtils.equalsAnyIgnoreCase("redis", type)) {
                String redisHostAndPort = TypesafeConfigUtils
                        .getStringOptional(config,
                                "ddth-akka-scheduling.pubsub-backend.redis-host-and-port")
                        .orElse("localhost:6379");
                String redisPassword = TypesafeConfigUtils.getStringOptional(config,
                        "ddth-akka-scheduling.pubsub-backend.redis-password").orElse(null);
                LOGGER.info("Creating Redis pub/sub hub [" + redisHostAndPort + "]...");
                hub = new UniversalRedisPubSubHub().setRedisHostAndPort(redisHostAndPort)
                        .setRedisPassword(redisPassword).init();
            } else {
                LOGGER.info("Creating in-memory queue factory...");
                hub = new UniversalInmemPubSubHub().init();
            }
            addShutdownHook(() -> hub.destroy());
            putToGlobalStorage("pubsub-hub", hub);
            pubSubHub = hub;
        }
        return pubSubHub;
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

            long dlockTimeMs = TypesafeConfigUtils
                    .getLongOptional(config, "ddth-akka-scheduling.dlock-time-ms")
                    .orElse(MultiNodePubSubBasedTickFanOutActor.DEFAULT_DLOCK_TIME_MS).longValue();

            IPubSubHub<?, byte[]> pubSubHub = buildPubSubHub(config);
            String channelName = TypesafeConfigUtils
                    .getStringOptional(config, "ddth-akka-scheduling.pubsub-backend.channel-name")
                    .orElse("akka-scheduled-jobs");

            tickFanOut = MultiNodePubSubBasedTickFanOutActor.newInstance(actorSystem, dlock,
                    dlockTimeMs, pubSubHub, channelName);
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

    private static Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            return clazz.getConstructor(parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            return null;
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
            IDLockFactory dlockFactory = buildDlockFactory(config);
            for (String cl : workerClazzs) {
                String[] tokens = cl.trim().split("[,; ]+");
                String clazzName = tokens[0];
                String actorName = tokens.length > 1 ? tokens[1] : null;
                String dlockName = tokens.length > 2 ? tokens[2] : null;
                String dlockTimeMsStr = tokens.length > 3 ? tokens[3] : null;
                try {
                    Class<?> clazz = Class.forName(clazzName);
                    Props props = null;
                    Constructor<?> constructor = getConstructor(clazz, IDLock.class, Long.class);
                    if (constructor == null) {
                        constructor = getConstructor(clazz, IDLock.class, long.class);
                    }
                    if (constructor != null) {
                        IDLock dlock = dlockFactory != null && !StringUtils.isBlank(dlockName)
                                ? dlockFactory.createLock(dlockName) : null;
                        long dlockTimeMs = NumberUtils.toLong(dlockTimeMsStr,
                                IDLock.DEFAULT_LOCK_DURATION_MS);
                        props = Props.create(clazz, dlock, dlockTimeMs);
                    } else {
                        props = Props.create(clazz);
                    }
                    if (StringUtils.isBlank(actorName)) {
                        LOGGER.info("Created worker " + actorSystem.actorOf(props));
                    } else {
                        LOGGER.info("Created worker " + actorSystem.actorOf(props, actorName));
                    }
                } catch (ClassNotFoundException cnfe) {
                    LOGGER.error("Error: Class [" + cl + "] not found!", cnfe);
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
