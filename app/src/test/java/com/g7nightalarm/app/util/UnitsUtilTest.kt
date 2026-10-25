package com.g7nightalarm.app.util

import com.g7nightalarm.app.settings.Units
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsUtilTest {
    @Test
    fun mgdlToMmol() {
        assertEquals(5.6, UnitsUtil.mgdlToMmol(100.8), 0.0001)
    }

    @Test
    fun formatMgdl() {
        val s = UnitsUtil.formatValue(123.4, Units.MG_DL)
        assertEquals("123.4", s)
    }

    @Test
    fun formatMmol() {
        val s = UnitsUtil.formatValue(90.0, Units.MMOL_L)
        assertEquals("5.0", s)
    }
}

