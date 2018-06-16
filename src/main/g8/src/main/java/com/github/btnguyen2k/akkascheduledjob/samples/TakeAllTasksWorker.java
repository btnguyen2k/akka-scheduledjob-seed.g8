package com.github.btnguyen2k.akkascheduledjob.samples;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.ddth.akka.scheduling.BaseWorker;
import com.github.ddth.akka.scheduling.TickMessage;
import com.github.ddth.akka.scheduling.WorkerCoordinationPolicy;
import com.github.ddth.akka.scheduling.annotation.Scheduling;
import com.github.ddth.commons.utils.DateFormatUtils;

/**
 * Execute task every 5 secs. Take all tasks unconditionally.
 * 
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since template-0.1.2
 */

@Scheduling(value = "*/5 * *", workerCoordinationPolicy = WorkerCoordinationPolicy.TAKE_ALL_TASKS)
public class TakeAllTasksWorker extends BaseWorker {
    private final Logger LOGGER = LoggerFactory.getLogger(TakeAllTasksWorker.class);

    @Override
    protected void doJob(String lockId, TickMessage tick) {
        /*
         * take-all-tasks worker does not lock, hence "lockId" is ignored!
         */

        Date now = new Date();
        LOGGER.info("{" + getActorPath().name() + "}: " + tick.getId() + " / "
                + DateFormatUtils.toString(now, DateFormatUtils.DF_ISO8601) + " / "
                + DateFormatUtils.toString(tick.getTimestamp(), DateFormatUtils.DF_ISO8601) + " / "
                + (now.getTime() - tick.getTimestamp().getTime()));
    }
}
