package com.g7nightalarm.app.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class MusicSettingsTest {
    @Test
    fun defaults_musicMode_off_and_uris_null() {
        val s = SettingsState()
        assertFalse(s.musicModeEnabled)
        assertNull(s.musicLowUri)
        assertNull(s.musicHighUri)
        assertNull(s.musicSignalUri)
    }
}

