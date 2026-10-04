package com.miapp.dndcompanion.fragua;

import androidx.annotation.NonNull;
import androidx.room.*;

@Database(
    entities = {SnapshotDatabase.Entry.class},
    version = 1,
    exportSchema = false)
public abstract class SnapshotDatabase extends RoomDatabase {
  public abstract Cache cache();

  @Entity(
      tableName = "snapshots",
      primaryKeys = {"account", "key"})
  public static class Entry {
    @NonNull public String account = "";
    @NonNull public String key = "";
    public String json = "";
    public long savedAt;
  }

  @Dao
  public interface Cache {
    @Query("SELECT * FROM snapshots WHERE account=:account AND `key`=:key LIMIT 1")
    Entry read(String account, String key);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void put(Entry entry);

    @Query("DELETE FROM snapshots WHERE account=:account")
    void clear(String account);
  }
}
