package com.mp.nsfwhub.utils;

import static com.mp.nsfwhub.utils.JobIds.JOB_TWO_STATUS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evernote.android.job.Job;

/**
 * @Author: Nasir
 * @Conatct: 8951195736
 * @Date: 02-10-2021
 */
public class JobCreator implements com.evernote.android.job.JobCreator {
    @Nullable
    @Override
    public Job create(@NonNull String tag) {
        switch (tag) {
            case JobIds.JOB_TWO_STATUS:
                return new TwoStatusJob();
        }
        return null;
    }
}

