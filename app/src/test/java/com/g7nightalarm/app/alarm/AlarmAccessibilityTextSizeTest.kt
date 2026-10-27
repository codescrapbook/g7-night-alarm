package com.g7nightalarm.app.alarm

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import com.g7nightalarm.app.data.GlucoseReading
import com.g7nightalarm.app.settings.Units
import com.g7nightalarm.app.ui.alarm.AlarmContent
import com.g7nightalarm.app.ui.theme.G7Theme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w1280dp-h800dp-land-mdpi")
class AlarmAccessibilityTextSizeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val FontSizeSpKey = com.g7nightalarm.app.ui.a11y.FontSizeSpKey

    @Test
    fun instruction_and_age_have_min_sizes() {
        val latest = GlucoseReading(
            timestamp = System.currentTimeMillis() - 60_000L,
            mgdl = 120.0,
            slopeMgdlPerMin = 0.0,
            slopeName = "→",
            source = "test"
        )
        composeRule.setContent {
            G7Theme {
                // Render at fontScale 2x to ensure no clipping path causes size regressions
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                    Box(Modifier.size(1280.dp, 800.dp)) {
                        AlarmContent(
                            type = com.g7nightalarm.app.receiver.AlarmActionReceiver.TYPE_HIGH,
                            onDismiss = {},
                            animationsEnabled = false,
                            previewLatest = latest,
                            previewUnits = Units.MG_DL
                        )
                    }
                }
            }
        }
        val instNode = composeRule.onNodeWithTag("alarmInstruction").fetchSemanticsNode()
        val ageNode = composeRule.onNodeWithTag("alarmAge").fetchSemanticsNode()
        val instSp = try { instNode.config[FontSizeSpKey] } catch (_: Throwable) { 0f }
        val ageSp = try { ageNode.config[FontSizeSpKey] } catch (_: Throwable) { 0f }
        assertTrue("Instruction should be at least 28sp, was $instSp", instSp >= 28f)
        assertTrue("Age line should be at least 24sp, was $ageSp", ageSp >= 24f)
    }
}

