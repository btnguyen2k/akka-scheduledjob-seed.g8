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

/**
 * Execute task every 2 secs. Global singleton: once worker takes a task, all of
 * its instances on all nodes are marked "busy" and can not take any more task
 * until free.
 * 
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since template-0.1.2
 */

@Scheduling(value = "*/5 * *", workerCoordinationPolicy = WorkerCoordinationPolicy.GLOBAL_SINGLETON)
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

    private AtomicLong COUNTER_EXEC = new AtomicLong(0), COUNTER_BUSY = new AtomicLong(0);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void logBusy(TickMessage tick, boolean isGlobal) {
        COUNTER_BUSY.incrementAndGet();
    }

    @Override
    protected void doJob(String dlockId, TickMessage tick) throws InterruptedException {
        Date now = new Date();
        try {
            long numExec = COUNTER_EXEC.incrementAndGet();
            long numBusy = COUNTER_BUSY.get();
            long numTotal = numBusy + numExec;
            LOGGER.info("\t{" + getActorPath().name() + "} " + numExec + " / " + numBusy + " / "
                    + Math.round(numExec * 100.0 / numTotal));

            int sleepMs = 1400 + RAND.nextInt(1000);
            Thread.sleep(sleepMs);
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
