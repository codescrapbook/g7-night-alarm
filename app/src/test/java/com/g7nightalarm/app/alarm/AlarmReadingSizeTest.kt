package com.g7nightalarm.app.alarm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import com.g7nightalarm.app.data.GlucoseReading
import com.g7nightalarm.app.receiver.AlarmActionReceiver
import com.g7nightalarm.app.settings.Units
import com.g7nightalarm.app.ui.alarm.AlarmContent
import com.g7nightalarm.app.ui.theme.G7Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicReference

/**
 * fontScale 1 keeps the beta10 reading size: displayMedium on a 411dp phone,
 * including the wider "260 mg/dL" plus a single arrow.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-port-xxhdpi")
class AlarmReadingSizeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun phone_fontScale1_260_keeps_displayMedium() {
        val layoutRef = AtomicReference<TextLayoutResult>()
        val expectedSp = AtomicReference(0f)
        val latest = GlucoseReading(
            timestamp = System.currentTimeMillis() - 60_000L,
            mgdl = 260.0,
            slopeMgdlPerMin = null,
            slopeName = "FortyFiveUp",
            source = "test"
        )
        composeRule.setContent {
            val base = LocalDensity.current
            G7Theme {
                expectedSp.set(MaterialTheme.typography.displayMedium.fontSize.value)
                CompositionLocalProvider(
                    LocalDensity provides Density(base.density, fontScale = 1f)
                ) {
                    Box(Modifier.size(411.dp, 891.dp)) {
                        AlarmContent(
                            type = AlarmActionReceiver.TYPE_HIGH,
                            onDismiss = {},
                            animationsEnabled = false,
                            previewLatest = latest,
                            previewUnits = Units.MG_DL,
                            onReadingLayout = { layoutRef.set(it) }
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        val layout = layoutRef.get()
        org.junit.Assert.assertNotNull(layout)
        assertEquals(1, layout!!.lineCount)
        assertEquals(expectedSp.get(), layout.layoutInput.style.fontSize.value, 0.51f)
    }
}
