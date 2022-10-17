package com.mp.nsfwhub.utils;

import androidx.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.concurrent.TimeUnit;

/**
 * @Author: Nasir
 * @Conatct: 8951195736
 * @Date: 30-12-2021
 */
public class TwoStatusJob extends Job {


    //schedule a job to delete the status after 24 hours
    public static void schedule(int time) {
        new JobRequest.Builder(JobIds.JOB_TWO_STATUS)
                .setExact(TimeUnit.HOURS.toHours(time))
                .build()
                .schedule();
    }


    /*public static void scheduleJob() {
        new JobRequest.Builder(JobIds.JOB_TWO_STATUS)
                .setExact(TimeUnit.HOURS.toHours(2))
                .build()
                .schedule();
    }*/

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        Prefs.putBoolean("two", true);
        return Result.SUCCESS;
    }


}

