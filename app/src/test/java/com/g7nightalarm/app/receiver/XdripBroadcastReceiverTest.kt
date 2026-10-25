package com.g7nightalarm.app.receiver

import org.junit.Assert.assertEquals
import org.junit.Test

class XdripBroadcastReceiverTest {
    @Test
    fun actionConstantsMatch() {
        assertEquals("com.eveningoutpost.dexdrip.BgEstimate", XdripBroadcastReceiver.ACTION_BG_ESTIMATE)
        assertEquals("com.eveningoutpost.dexdrip.BgEstimateNoData", XdripBroadcastReceiver.ACTION_BG_NO_DATA)
        assertEquals("com.eveningoutpost.dexdrip.Extras.BgEstimate", XdripBroadcastReceiver.EXTRA_BG_ESTIMATE)
        assertEquals("com.eveningoutpost.dexdrip.Extras.BgSlope", XdripBroadcastReceiver.EXTRA_BG_SLOPE)
        assertEquals("com.eveningoutpost.dexdrip.Extras.BgSlopeName", XdripBroadcastReceiver.EXTRA_BG_SLOPE_NAME)
        assertEquals("com.eveningoutpost.dexdrip.Extras.Time", XdripBroadcastReceiver.EXTRA_TIMESTAMP)
    }
}

