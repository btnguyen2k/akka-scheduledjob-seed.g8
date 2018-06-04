package com.github.btnguyen2k.akkascheduledjob.samples;

import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.ddth.akka.scheduling.BaseWorker;
import com.github.ddth.akka.scheduling.TickMessage;
import com.github.ddth.akka.scheduling.WorkerCoordinationPolicy;
import com.github.ddth.akka.scheduling.annotation.Scheduling;
import com.github.ddth.dlock.IDLock;

/**
 * Execute task every 2 secs. Global singleton: once worker takes a task, all of
 * its instances on all nodes are marked "busy" and can not take any more task
 * until free.
 * 
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since template-0.1.2
 */

@Scheduling(value = "*/3 * *", getWorkerCoordinationPolicy = WorkerCoordinationPolicy.GLOBAL_SINGLETON)
public class GlobalSingletonWorker extends BaseWorker {

    private final Logger LOGGER = LoggerFactory.getLogger(GlobalSingletonWorker.class);
    private final Random RAND = new Random(System.currentTimeMillis());

    /**
     * {@inheritDoc}
     */
    @Override
    protected String generateDLockId() {
        return UUID.randomUUID().toString();
    }

    private AtomicLong COUNTER_TASK = new AtomicLong(0), COUNTER_BUSY = new AtomicLong(0);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void logBusy(TickMessage tick, boolean isGlobal) {
        COUNTER_BUSY.incrementAndGet();
    }

    public GlobalSingletonWorker(IDLock dlock, long dlockTimeMs) {
        super(dlock, dlockTimeMs);
    }

    @Override
    protected void doJob(String dlockId, TickMessage tick) throws InterruptedException {
        Date now = new Date();
        try {
            COUNTER_TASK.incrementAndGet();
            // LOGGER.info("{" + self().path() + "}: " + tick.getId() + " / "
            // + DateFormatUtils.toString(now, DateFormatUtils.DF_ISO8601) + " /
            // "
            // + DateFormatUtils.toString(tick.getTimestamp(),
            // DateFormatUtils.DF_ISO8601)
            // + " / " + (now.getTime() - tick.getTimestamp().getTime()));
            int sleepMs = 1400 + RAND.nextInt(1000);
            Thread.sleep(sleepMs);
            long numTask = COUNTER_TASK.get();
            long numBusy = COUNTER_BUSY.get();
            LOGGER.info("\t{" + self().path() + "} " + numTask + " / " + numBusy + " / "
                    + (numTask * 100.0 / (numTask + numBusy)));
        } finally {
            if (!StringUtils.isBlank(dlockId)
                    && System.currentTimeMillis() - now.getTime() > 1000) {
                /*
                 * It's generally a good idea to release lock after finishing
                 * task. However, if task is ultra-fast, let the caller (i.e.
                 * BaseWorker) release the lock.
                 */
                unlock(dlockId);
            }
        }
    }
}
