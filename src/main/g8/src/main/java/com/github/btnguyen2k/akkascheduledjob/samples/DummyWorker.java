package com.github.btnguyen2k.akkascheduledjob.samples;

import com.github.ddth.akka.scheduling.BaseWorker;
import com.github.ddth.akka.scheduling.TickMessage;
import com.github.ddth.akka.scheduling.annotation.Scheduling;
import com.github.ddth.commons.utils.DateFormatUtils;

import java.util.Date;

@Scheduling("*/6 * *")
public class DummyWorker extends BaseWorker {
    @Override
    protected void doJob(TickMessage tick) {
        Date now = new Date();
        System.out.println("{" + self().path() + "}: " + tick.getId() + " / " + DateFormatUtils
                .toString(now, DateFormatUtils.DF_ISO8601) + " / " + DateFormatUtils
                .toString(tick.getTimestamp(), DateFormatUtils.DF_ISO8601) + " / " + (now.getTime()
                - tick.getTimestamp().getTime()));
    }
}
