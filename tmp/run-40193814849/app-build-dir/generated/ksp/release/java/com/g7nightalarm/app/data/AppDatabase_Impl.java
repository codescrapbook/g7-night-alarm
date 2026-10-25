package com.g7nightalarm.app.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.g7nightalarm.app.mode.ModeDao;
import com.g7nightalarm.app.mode.ModeDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile GlucoseDao _glucoseDao;

  private volatile ModeDao _modeDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `glucose_readings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `mgdl` REAL NOT NULL, `slopeMgdlPerMin` REAL, `slopeName` TEXT, `source` TEXT NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_glucose_readings_timestamp` ON `glucose_readings` (`timestamp`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `mode_change_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `mode` TEXT NOT NULL, `trigger` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e4d1b4a89611611eced2896f2a11808e')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `glucose_readings`");
        db.execSQL("DROP TABLE IF EXISTS `mode_change_history`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsGlucoseReadings = new HashMap<String, TableInfo.Column>(6);
        _columnsGlucoseReadings.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGlucoseReadings.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGlucoseReadings.put("mgdl", new TableInfo.Column("mgdl", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGlucoseReadings.put("slopeMgdlPerMin", new TableInfo.Column("slopeMgdlPerMin", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGlucoseReadings.put("slopeName", new TableInfo.Column("slopeName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGlucoseReadings.put("source", new TableInfo.Column("source", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGlucoseReadings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGlucoseReadings = new HashSet<TableInfo.Index>(1);
        _indicesGlucoseReadings.add(new TableInfo.Index("index_glucose_readings_timestamp", true, Arrays.asList("timestamp"), Arrays.asList("ASC")));
        final TableInfo _infoGlucoseReadings = new TableInfo("glucose_readings", _columnsGlucoseReadings, _foreignKeysGlucoseReadings, _indicesGlucoseReadings);
        final TableInfo _existingGlucoseReadings = TableInfo.read(db, "glucose_readings");
        if (!_infoGlucoseReadings.equals(_existingGlucoseReadings)) {
          return new RoomOpenHelper.ValidationResult(false, "glucose_readings(com.g7nightalarm.app.data.GlucoseReading).\n"
                  + " Expected:\n" + _infoGlucoseReadings + "\n"
                  + " Found:\n" + _existingGlucoseReadings);
        }
        final HashMap<String, TableInfo.Column> _columnsModeChangeHistory = new HashMap<String, TableInfo.Column>(4);
        _columnsModeChangeHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsModeChangeHistory.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsModeChangeHistory.put("mode", new TableInfo.Column("mode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsModeChangeHistory.put("trigger", new TableInfo.Column("trigger", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysModeChangeHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesModeChangeHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoModeChangeHistory = new TableInfo("mode_change_history", _columnsModeChangeHistory, _foreignKeysModeChangeHistory, _indicesModeChangeHistory);
        final TableInfo _existingModeChangeHistory = TableInfo.read(db, "mode_change_history");
        if (!_infoModeChangeHistory.equals(_existingModeChangeHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "mode_change_history(com.g7nightalarm.app.mode.ModeChangeEvent).\n"
                  + " Expected:\n" + _infoModeChangeHistory + "\n"
                  + " Found:\n" + _existingModeChangeHistory);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "e4d1b4a89611611eced2896f2a11808e", "0fb93bab8164915f075e78831bab1123");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "glucose_readings","mode_change_history");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `glucose_readings`");
      _db.execSQL("DELETE FROM `mode_change_history`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(GlucoseDao.class, GlucoseDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ModeDao.class, ModeDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public GlucoseDao glucoseDao() {
    if (_glucoseDao != null) {
      return _glucoseDao;
    } else {
      synchronized(this) {
        if(_glucoseDao == null) {
          _glucoseDao = new GlucoseDao_Impl(this);
        }
        return _glucoseDao;
      }
    }
  }

  @Override
  public ModeDao modeDao() {
    if (_modeDao != null) {
      return _modeDao;
    } else {
      synchronized(this) {
        if(_modeDao == null) {
          _modeDao = new ModeDao_Impl(this);
        }
        return _modeDao;
      }
    }
  }
}
