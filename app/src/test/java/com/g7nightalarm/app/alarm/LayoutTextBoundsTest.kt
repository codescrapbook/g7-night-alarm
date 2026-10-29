package com.g7nightalarm.app.alarm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.g7nightalarm.app.data.GlucoseReading
import com.g7nightalarm.app.receiver.AlarmActionReceiver
import com.g7nightalarm.app.settings.Units
import com.g7nightalarm.app.ui.alarm.AlarmContent
import com.g7nightalarm.app.ui.screens.HomeScreen
import com.g7nightalarm.app.ui.theme.G7Theme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.atomic.AtomicReference

/**
 * Banner, instruction, and reading lines are separate text nodes whose layout
 * bounds must not paint over each other. Instruction lines also have to fit on
 * one line at fontScale 1 on a 411dp phone; wrapping is allowed at fontScale 2.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LayoutTextBoundsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val densityPx = AtomicReference(1f)

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun phone_snooze_banner_texts_do_not_overlap() {
        showBanner(411.dp, 891.dp, fontScale = 1f)
        assertBannerSeparated()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun tablet_snooze_banner_texts_do_not_overlap() {
        showBanner(1280.dp, 800.dp, fontScale = 1f)
        assertBannerSeparated()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun fs2_phone_snooze_banner_texts_do_not_overlap() {
        showBanner(411.dp, 891.dp, fontScale = 2f)
        assertBannerSeparated()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun phone_urgent_instruction_and_reading_lines_do_not_overlap() {
        showAlarm(411.dp, 891.dp, AlarmActionReceiver.TYPE_URGENT, fontScale = 1f)
        composeRule.onNodeWithText("Tap Snooze to wake").assertExists()
        composeRule.onNodeWithText("Hold to dismiss").assertExists()
        assertInstructionsSeparated(requireOneLine = true)
        assertReadingTrendAgeSeparated()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun phone_tap_to_dismiss_instruction_fits_one_line() {
        showAlarm(411.dp, 891.dp, AlarmActionReceiver.TYPE_HIGH, fontScale = 1f)
        composeRule.onNodeWithText("Tap to dismiss").assertExists()
        assertInstructionsSeparated(requireOneLine = true)
        assertReadingTrendAgeSeparated()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun phone_checkin_instruction_fits_one_line() {
        showAlarm(411.dp, 891.dp, AlarmActionReceiver.TYPE_CHECKIN, fontScale = 1f)
        composeRule.onNodeWithText("Tap I'm OK").assertExists()
        assertInstructionsSeparated(requireOneLine = true)
        assertReadingTrendAgeSeparated()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun tablet_alarm_text_lines_do_not_overlap() {
        showAlarm(1280.dp, 800.dp, AlarmActionReceiver.TYPE_URGENT, fontScale = 1f)
        assertInstructionsSeparated(requireOneLine = true)
        assertReadingTrendAgeSeparated()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun fs2_phone_alarm_text_lines_do_not_overlap() {
        showAlarm(411.dp, 891.dp, AlarmActionReceiver.TYPE_URGENT, fontScale = 2f)
        assertInstructionsSeparated(requireOneLine = false)
        assertReadingTrendAgeSeparated()
    }

    private fun showBanner(width: Dp, height: Dp, fontScale: Float) {
        val latest = reading()
        composeRule.setContent {
            val base = LocalDensity.current
            densityPx.set(base.density)
            G7Theme {
                CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                    Box(Modifier.size(width, height)) {
                        HomeScreen(
                            onOpenSettings = {},
                            previewLatest = latest,
                            previewHistory = emptyList(),
                            previewIsSleep = true,
                            previewSnoozeTimeText = "3:42 AM"
                        )
                    }
                }
            }
        }
        settle()
    }

    private fun showAlarm(width: Dp, height: Dp, type: String, fontScale: Float) {
        composeRule.setContent {
            val base = LocalDensity.current
            densityPx.set(base.density)
            G7Theme {
                CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                    Box(Modifier.size(width, height)) {
                        AlarmContent(
                            type = type,
                            onDismiss = {},
                            animationsEnabled = false,
                            previewLatest = reading(),
                            previewUnits = Units.MG_DL,
                            previewSnoozeMinutes = 10
                        )
                    }
                }
            }
        }
        settle()
    }

    private fun settle() {
        composeRule.waitForIdle()
        repeat(20) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            composeRule.waitForIdle()
        }
    }

    private fun assertBannerSeparated() {
        val label = bounds("snoozeBannerLabel")
        val time = bounds("snoozeWakeTime")
        assertStacked("snooze label", label, "wake time", time)
    }

    private fun assertInstructionsSeparated(requireOneLine: Boolean) {
        val snooze = bounds("alarmInstruction")
        val dismiss = bounds("alarmDismissInstruction")
        assertStacked("snooze instruction", snooze, "dismiss instruction", dismiss)
        if (requireOneLine) {
            assertOneLine("snooze instruction", snooze)
            assertOneLine("dismiss instruction", dismiss)
        }
    }

    private fun assertReadingTrendAgeSeparated() {
        val reading = bounds("alarmReadingRow")
        val trend = bounds("alarmTrendLabel")
        val age = bounds("alarmAge")
        assertStacked("reading", reading, "trend", trend)
        assertStacked("trend", trend, "age", age)
        assertStacked("reading", reading, "age", age)
    }

    private fun bounds(tag: String): Rect =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun assertStacked(upperName: String, upper: Rect, lowerName: String, lower: Rect) {
        assertFalse(
            "$upperName overlaps $lowerName: $upper vs $lower",
            upper.overlaps(lower)
        )
        assertTrue(
            "$lowerName should sit below $upperName: $upper vs $lower",
            lower.top >= upper.bottom
        )
    }

    private fun assertOneLine(name: String, bounds: Rect) {
        // 28sp text with a 36sp line height. Two lines are about twice that.
        val maxHeight = 28f * densityPx.get() * 1.55f
        assertTrue(
            "$name should fit on one line, height=${bounds.height} max=$maxHeight bounds=$bounds",
            bounds.height <= maxHeight
        )
    }

    private fun reading() = GlucoseReading(
        timestamp = System.currentTimeMillis() - 60_000L,
        mgdl = 58.0,
        slopeMgdlPerMin = -1.2,
        slopeName = "↘",
        source = "test"
    )
}
