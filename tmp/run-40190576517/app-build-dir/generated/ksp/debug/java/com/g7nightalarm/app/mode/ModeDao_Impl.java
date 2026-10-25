package com.g7nightalarm.app.mode;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ModeDao_Impl implements ModeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ModeChangeEvent> __insertionAdapterOfModeChangeEvent;

  public ModeDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfModeChangeEvent = new EntityInsertionAdapter<ModeChangeEvent>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `mode_change_history` (`id`,`timestamp`,`mode`,`trigger`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ModeChangeEvent entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindString(3, __AppMode_enumToString(entity.getMode()));
        statement.bindString(4, __ModeTrigger_enumToString(entity.getTrigger()));
      }
    };
  }

  @Override
  public Object insert(final ModeChangeEvent event, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfModeChangeEvent.insert(event);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ModeChangeEvent>> recent(final int limit) {
    final String _sql = "SELECT * FROM mode_change_history ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"mode_change_history"}, new Callable<List<ModeChangeEvent>>() {
      @Override
      @NonNull
      public List<ModeChangeEvent> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfTrigger = CursorUtil.getColumnIndexOrThrow(_cursor, "trigger");
          final List<ModeChangeEvent> _result = new ArrayList<ModeChangeEvent>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ModeChangeEvent _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final AppMode _tmpMode;
            _tmpMode = __AppMode_stringToEnum(_cursor.getString(_cursorIndexOfMode));
            final ModeTrigger _tmpTrigger;
            _tmpTrigger = __ModeTrigger_stringToEnum(_cursor.getString(_cursorIndexOfTrigger));
            _item = new ModeChangeEvent(_tmpId,_tmpTimestamp,_tmpMode,_tmpTrigger);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __AppMode_enumToString(@NonNull final AppMode _value) {
    switch (_value) {
      case SLEEP: return "SLEEP";
      case AWAKE: return "AWAKE";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private String __ModeTrigger_enumToString(@NonNull final ModeTrigger _value) {
    switch (_value) {
      case BUTTON: return "BUTTON";
      case VOICE: return "VOICE";
      case SCHEDULE: return "SCHEDULE";
      case BOOT: return "BOOT";
      case UNKNOWN: return "UNKNOWN";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private AppMode __AppMode_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "SLEEP": return AppMode.SLEEP;
      case "AWAKE": return AppMode.AWAKE;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }

  private ModeTrigger __ModeTrigger_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "BUTTON": return ModeTrigger.BUTTON;
      case "VOICE": return ModeTrigger.VOICE;
      case "SCHEDULE": return ModeTrigger.SCHEDULE;
      case "BOOT": return ModeTrigger.BOOT;
      case "UNKNOWN": return ModeTrigger.UNKNOWN;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
