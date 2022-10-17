package com.mp.nsfwhub.data.dbs;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.mp.nsfwhub.data.daos.DraftDao;
import com.mp.nsfwhub.data.entities.Draft;

@Database(entities = {Draft.class}, version = 4, exportSchema = false)
public abstract class ClientDatabase extends RoomDatabase {

    public abstract DraftDao drafts();
}
