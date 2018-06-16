package com.github.btnguyen2k.akkascheduledjob.samples;

import java.util.Date;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.ddth.akka.scheduling.BaseWorker;
import com.github.ddth.akka.scheduling.TickMessage;
import com.github.ddth.akka.scheduling.WorkerCoordinationPolicy;
import com.github.ddth.akka.scheduling.annotation.Scheduling;
import com.github.ddth.commons.utils.DateFormatUtils;

/**
 * Execute task every 3 secs. Local singleton: on one node, worker can take only
 * one task as a time. But workers on two or more nodes can execute tasks
 * simultaneously.
 * 
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since template-0.1.2
 */

@Scheduling(value = "*/3 * *", workerCoordinationPolicy = WorkerCoordinationPolicy.LOCAL_SINGLETON)
public class LocalSingletonWorker extends BaseWorker {
    private final Logger LOGGER = LoggerFactory.getLogger(LocalSingletonWorker.class);
    private Random RAND = new Random(System.currentTimeMillis());

    @Override
    protected void doJob(String dlockId, TickMessage tick) throws InterruptedException {
        /*
         * local-singleton worker does not lock globally, hence "dlockId" is
         * ignored!
         */

        Date now = new Date();
        LOGGER.info("{" + getActorPath().name() + "}: " + tick.getId() + " / "
                + DateFormatUtils.toString(now, DateFormatUtils.DF_ISO8601) + " / "
                + DateFormatUtils.toString(tick.getTimestamp(), DateFormatUtils.DF_ISO8601) + " / "
                + (now.getTime() - tick.getTimestamp().getTime()));

        int sleepMs = 2400 + RAND.nextInt(1000);
        Thread.sleep(sleepMs);
    }
}
