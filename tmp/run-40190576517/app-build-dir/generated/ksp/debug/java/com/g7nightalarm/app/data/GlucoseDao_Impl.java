package com.g7nightalarm.app.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class GlucoseDao_Impl implements GlucoseDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GlucoseReading> __insertionAdapterOfGlucoseReading;

  private final SharedSQLiteStatement __preparedStmtOfPrune;

  public GlucoseDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGlucoseReading = new EntityInsertionAdapter<GlucoseReading>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `glucose_readings` (`id`,`timestamp`,`mgdl`,`slopeMgdlPerMin`,`slopeName`,`source`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GlucoseReading entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindDouble(3, entity.getMgdl());
        if (entity.getSlopeMgdlPerMin() == null) {
          statement.bindNull(4);
        } else {
          statement.bindDouble(4, entity.getSlopeMgdlPerMin());
        }
        if (entity.getSlopeName() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getSlopeName());
        }
        statement.bindString(6, entity.getSource());
      }
    };
    this.__preparedStmtOfPrune = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM glucose_readings WHERE timestamp < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final GlucoseReading reading, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfGlucoseReading.insertAndReturnId(reading);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object prune(final long before, final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPrune.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, before);
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfPrune.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<GlucoseReading>> streamSince(final long since) {
    final String _sql = "SELECT * FROM glucose_readings WHERE timestamp >= ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, since);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"glucose_readings"}, new Callable<List<GlucoseReading>>() {
      @Override
      @NonNull
      public List<GlucoseReading> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfMgdl = CursorUtil.getColumnIndexOrThrow(_cursor, "mgdl");
          final int _cursorIndexOfSlopeMgdlPerMin = CursorUtil.getColumnIndexOrThrow(_cursor, "slopeMgdlPerMin");
          final int _cursorIndexOfSlopeName = CursorUtil.getColumnIndexOrThrow(_cursor, "slopeName");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final List<GlucoseReading> _result = new ArrayList<GlucoseReading>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GlucoseReading _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final double _tmpMgdl;
            _tmpMgdl = _cursor.getDouble(_cursorIndexOfMgdl);
            final Double _tmpSlopeMgdlPerMin;
            if (_cursor.isNull(_cursorIndexOfSlopeMgdlPerMin)) {
              _tmpSlopeMgdlPerMin = null;
            } else {
              _tmpSlopeMgdlPerMin = _cursor.getDouble(_cursorIndexOfSlopeMgdlPerMin);
            }
            final String _tmpSlopeName;
            if (_cursor.isNull(_cursorIndexOfSlopeName)) {
              _tmpSlopeName = null;
            } else {
              _tmpSlopeName = _cursor.getString(_cursorIndexOfSlopeName);
            }
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            _item = new GlucoseReading(_tmpId,_tmpTimestamp,_tmpMgdl,_tmpSlopeMgdlPerMin,_tmpSlopeName,_tmpSource);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<GlucoseReading> latestFlow() {
    final String _sql = "SELECT * FROM glucose_readings ORDER BY timestamp DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"glucose_readings"}, new Callable<GlucoseReading>() {
      @Override
      @Nullable
      public GlucoseReading call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfMgdl = CursorUtil.getColumnIndexOrThrow(_cursor, "mgdl");
          final int _cursorIndexOfSlopeMgdlPerMin = CursorUtil.getColumnIndexOrThrow(_cursor, "slopeMgdlPerMin");
          final int _cursorIndexOfSlopeName = CursorUtil.getColumnIndexOrThrow(_cursor, "slopeName");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final GlucoseReading _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final double _tmpMgdl;
            _tmpMgdl = _cursor.getDouble(_cursorIndexOfMgdl);
            final Double _tmpSlopeMgdlPerMin;
            if (_cursor.isNull(_cursorIndexOfSlopeMgdlPerMin)) {
              _tmpSlopeMgdlPerMin = null;
            } else {
              _tmpSlopeMgdlPerMin = _cursor.getDouble(_cursorIndexOfSlopeMgdlPerMin);
            }
            final String _tmpSlopeName;
            if (_cursor.isNull(_cursorIndexOfSlopeName)) {
              _tmpSlopeName = null;
            } else {
              _tmpSlopeName = _cursor.getString(_cursorIndexOfSlopeName);
            }
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            _result = new GlucoseReading(_tmpId,_tmpTimestamp,_tmpMgdl,_tmpSlopeMgdlPerMin,_tmpSlopeName,_tmpSource);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object latest(final Continuation<? super GlucoseReading> $completion) {
    final String _sql = "SELECT * FROM glucose_readings ORDER BY timestamp DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<GlucoseReading>() {
      @Override
      @Nullable
      public GlucoseReading call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfMgdl = CursorUtil.getColumnIndexOrThrow(_cursor, "mgdl");
          final int _cursorIndexOfSlopeMgdlPerMin = CursorUtil.getColumnIndexOrThrow(_cursor, "slopeMgdlPerMin");
          final int _cursorIndexOfSlopeName = CursorUtil.getColumnIndexOrThrow(_cursor, "slopeName");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final GlucoseReading _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final double _tmpMgdl;
            _tmpMgdl = _cursor.getDouble(_cursorIndexOfMgdl);
            final Double _tmpSlopeMgdlPerMin;
            if (_cursor.isNull(_cursorIndexOfSlopeMgdlPerMin)) {
              _tmpSlopeMgdlPerMin = null;
            } else {
              _tmpSlopeMgdlPerMin = _cursor.getDouble(_cursorIndexOfSlopeMgdlPerMin);
            }
            final String _tmpSlopeName;
            if (_cursor.isNull(_cursorIndexOfSlopeName)) {
              _tmpSlopeName = null;
            } else {
              _tmpSlopeName = _cursor.getString(_cursorIndexOfSlopeName);
            }
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            _result = new GlucoseReading(_tmpId,_tmpTimestamp,_tmpMgdl,_tmpSlopeMgdlPerMin,_tmpSlopeName,_tmpSource);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
