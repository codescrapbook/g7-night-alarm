package com.g7nightalarm.app.alarm

import com.google.common.truth.Truth.assertThat
import com.g7nightalarm.app.receiver.AlarmActionReceiver
import org.junit.Test

class AlarmUiTypeMapperTest {
    @Test
    fun maps_all_alarm_events_exhaustively() {
        assertThat(AlarmUiTypeMapper.map(AlarmEvent.UrgentLow(55.0))).isEqualTo(AlarmActionReceiver.TYPE_URGENT)
        assertThat(AlarmUiTypeMapper.map(AlarmEvent.Low(69.0))).isEqualTo(AlarmActionReceiver.TYPE_LOW)
        assertThat(AlarmUiTypeMapper.map(AlarmEvent.High(260.0))).isEqualTo(AlarmActionReceiver.TYPE_HIGH)
        assertThat(AlarmUiTypeMapper.map(AlarmEvent.NoData(35))).isEqualTo(AlarmActionReceiver.TYPE_SIGNAL)
        assertThat(AlarmUiTypeMapper.map(AlarmEvent.RapidFall(-3.5))).isEqualTo(AlarmActionReceiver.TYPE_LOW)
        assertThat(AlarmUiTypeMapper.map(AlarmEvent.None)).isNull()
    }
}

