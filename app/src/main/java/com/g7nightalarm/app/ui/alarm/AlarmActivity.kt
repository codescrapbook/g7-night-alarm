package com.g7nightalarm.app.ui.alarm

import android.os.Bundle
import android.os.Build
import android.app.KeyguardManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g7nightalarm.app.alarm.AlarmController
import com.g7nightalarm.app.ui.theme.G7Theme
import kotlinx.coroutines.delay
import com.g7nightalarm.app.receiver.AlarmActionReceiver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.g7nightalarm.app.data.AppDatabase
import com.g7nightalarm.app.data.GlucoseReading
import com.g7nightalarm.app.settings.SettingsRepository
import com.g7nightalarm.app.settings.Units
import com.g7nightalarm.app.util.UnitsUtil
import android.widget.TextView
import android.util.TypedValue
import androidx.compose.ui.graphics.toArgb
import android.view.Gravity
import android.graphics.Typeface
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.g7nightalarm.app.ui.a11y.FontSizeSpKey
import com.g7nightalarm.app.ui.a11y.fontSizeSp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextLayoutResult
import com.g7nightalarm.app.ui.trend.Trend
import com.g7nightalarm.app.ui.trend.TrendDirection
import com.g7nightalarm.app.ui.trend.InlineTrendReading
import com.g7nightalarm.app.ui.trend.contentDescription
import com.g7nightalarm.app.ui.trend.deriveTrend
import com.g7nightalarm.app.ui.trend.label
import com.g7nightalarm.app.alarm.SnoozeController
import com.g7nightalarm.app.alarm.SnoozePolicy
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {
    private lateinit var alarmController: AlarmController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(KeyguardManager::class.java)
            km?.requestDismissKeyguard(this, null)
        }
        // Keep screen on while alarm is showing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        alarmController = AlarmController(this)
        val type = intent?.getStringExtra(AlarmActionReceiver.EXTRA_TYPE)
        setContent {
            G7Theme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AlarmContent(
                        type = type,
                        onDismiss = {
                        alarmController.stopAlarm()
                        // notify service to resume voice
                        sendBroadcast(android.content.Intent(com.g7nightalarm.app.service.MonitorService.ACTION_ALARM_STOPPED))
                        finish()
                    },
                        onSnooze = {
                            lifecycleScope.launch {
                                val minutes = SettingsRepository(this@AlarmActivity).flow.first().snoozeLengthMinutes
                                val mgdl = AppDatabase.get(this@AlarmActivity).glucoseDao().latest()?.mgdl
                                SnoozeController.arm(this@AlarmActivity, type ?: "", mgdl, minutes)
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmContent(
    type: String?,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit = {},
    animationsEnabled: Boolean = true,
    previewLatest: GlucoseReading? = null,
    previewUnits: Units? = null,
    previewSnoozeMinutes: Int? = null,
    onReadingLayout: (TextLayoutResult) -> Unit = {}
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).glucoseDao() }
    val settingsRepo = remember { SettingsRepository(context) }
    val flowLatest by remember { dao.latestFlow() }.collectAsState(initial = null)
    val s by settingsRepo.flow.collectAsState(initial = com.g7nightalarm.app.settings.SettingsState())
    val latest = previewLatest ?: flowLatest
    val units = previewUnits ?: s.units
    val ageMs = latest?.let { (System.currentTimeMillis() - it.timestamp).coerceAtLeast(0) } ?: 0L
    val ageMin = (ageMs / 60000L).toInt()
    val readingTitle = if (latest == null) {
        "— ${if (units == Units.MG_DL) "mg/dL" else "mmol/L"}"
    } else {
        "${UnitsUtil.formatValue(latest!!.mgdl, units)} ${if (units == Units.MG_DL) "mg/dL" else "mmol/L"}"
    }
    val trend: Trend = deriveTrend(latest, previous = null)
    val agePhrase: String = if (latest == null) "" else when {
        ageMs < 60_000L -> "just now"
        ageMin in 1..59 -> {
            if (ageMin == 1) "1 min ago" else "$ageMin mins ago"
        }
        else -> {
            val hrs = ageMin / 60
            if (hrs == 1) "1 hr ago" else "$hrs hrs ago"
        }
    }

    val flashing = remember { Animatable(0f) }
    LaunchedEffect(animationsEnabled) {
        if (animationsEnabled) {
            while (true) {
                flashing.animateTo(1f, animationSpec = tween(500))
                flashing.animateTo(0f, animationSpec = tween(500))
            }
        }
    }
    val (cA, cB, titleColor, subtitleColor) = when (type) {
        // Use AAA-contrast text colors on each background
        AlarmActionReceiver.TYPE_URGENT -> Quad(Color(0xFF8E0000), Color(0xFFB71C1C), Color.White, Color.White)
        AlarmActionReceiver.TYPE_LOW -> Quad(Color(0xFFAA2E00), Color(0xFFD84315), Color.White, Color.White)
        AlarmActionReceiver.TYPE_HIGH -> Quad(Color(0xFFFFA000), Color(0xFFFF8F00), Color.Black, Color.Black)
        AlarmActionReceiver.TYPE_SIGNAL -> Quad(Color(0xFF37474F), Color(0xFF455A64), Color.White, Color.White)
        AlarmActionReceiver.TYPE_LOW_WARN -> Quad(Color(0xFF0D47A1), Color(0xFF1976D2), Color.White, Color.White)
        AlarmActionReceiver.TYPE_CHECKIN -> Quad(Color(0xFF1A237E), Color(0xFF283593), Color.White, Color.White)
        else -> Quad(Color(0xFF8E0000), Color(0xFFB71C1C), Color.White, Color.White)
    }
    val bg = if (animationsEnabled) androidx.compose.ui.graphics.lerp(cA, cB, flashing.value) else cB
    val holding = remember { mutableStateOf(false) }
    val holdMs = 1500L
    val (title, subtitle, dismissLabel) = when (type) {
        AlarmActionReceiver.TYPE_LOW_WARN -> Triple("LOW WARNING", "Press and hold to dismiss", "DISMISS")
        AlarmActionReceiver.TYPE_HIGH -> Triple("HIGH ALARM", "Press and hold to dismiss", "DISMISS")
        AlarmActionReceiver.TYPE_SIGNAL -> Triple("SIGNAL LOST", "Press and hold to dismiss", "DISMISS")
        AlarmActionReceiver.TYPE_LOW -> Triple("LOW ALARM", "Press and hold to dismiss", "HOLD")
        AlarmActionReceiver.TYPE_URGENT -> Triple("URGENT ALARM", "Press and hold to dismiss", "HOLD")
        AlarmActionReceiver.TYPE_CHECKIN -> Triple("CHECK-IN", "Tap I'm OK", "I'm OK")
        else -> Triple("URGENT ALARM", "Press and hold to dismiss", "HOLD")
    }
    val snoozeLabel = SnoozePolicy.buttonLabel(previewSnoozeMinutes ?: s.snoozeLengthMinutes)
    val checkInTap = type == AlarmActionReceiver.TYPE_CHECKIN
    val fontScale = LocalDensity.current.fontScale
    val screenPad = if (fontScale >= 1.5f) 16.dp else 48.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(screenPad),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val cfg = LocalConfiguration.current
        val titleTextStyle = if (cfg.screenWidthDp < 500) {
            MaterialTheme.typography.displayMedium
        } else {
            MaterialTheme.typography.displayLarge
        }
        Text(
            title,
            style = titleTextStyle.copy(fontFamily = FontFamily.Default, color = titleColor),
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        // Top content is scrollable to avoid clipping at large font scales
        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Current reading (large, high-contrast, plain font)
            val readingTextStyle = if (cfg.screenWidthDp < 500) {
                // Use a slightly smaller style on narrow widths to keep value + arrow on one line
                MaterialTheme.typography.displayMedium
            } else {
                MaterialTheme.typography.displayLarge
            }
            val spokenReading = if (latest == null) {
                "No recent reading"
            } else {
                val unitWords = if (units == Units.MG_DL) "milligrams per deciliter" else "millimoles per liter"
                val base = "${UnitsUtil.formatValue(latest.mgdl, units)} $unitWords"
                if (trend.direction == TrendDirection.UNKNOWN) {
                    base
                } else {
                    "$base, ${trend.contentDescription().replaceFirstChar { it.lowercase() }}"
                }
            }
            InlineTrendReading(
                reading = readingTitle,
                direction = trend.direction,
                color = titleColor,
                style = readingTextStyle,
                contentDescription = spokenReading,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth(),
                testTag = "alarmReadingRow",
                onTextLayout = onReadingLayout
            )
            // Prominent, centered trend label under the reading
            Text(
                text = trend.label(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    fontFamily = FontFamily.Default
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .semantics { contentDescription = trend.contentDescription() }
            )
            if (agePhrase.isNotBlank()) {
                // Build an annotated single-line age string with bold number at least 40sp
                val density = androidx.compose.ui.platform.LocalDensity.current
                val fs = density.fontScale
                // Ensure at least 40sp for number and 32sp for the suffix at display scale
                val numberSp = 40.sp
                val suffixSp = 32.sp
                val annotated = androidx.compose.ui.text.buildAnnotatedString {
                    // Extract number and suffix tokens
                    when {
                        ageMs < 60_000L -> {
                            // "just now" — treat as suffix portion
                            withStyle(androidx.compose.ui.text.SpanStyle(fontSize = suffixSp, fontWeight = FontWeight.SemiBold, color = subtitleColor)) {
                                append("just now")
                            }
                        }
                        ageMin in 1..59 -> {
                            val num = ageMin.toString()
                            val unit = if (ageMin == 1) " min ago" else " mins ago"
                            withStyle(androidx.compose.ui.text.SpanStyle(fontSize = numberSp, fontWeight = FontWeight.Bold, color = subtitleColor)) {
                                append(num)
                            }
                            withStyle(androidx.compose.ui.text.SpanStyle(fontSize = suffixSp, fontWeight = FontWeight.SemiBold, color = subtitleColor)) {
                                append(unit)
                            }
                        }
                        else -> {
                            val hrs = (ageMin / 60).toString()
                            val unit = if (ageMin / 60 == 1) " hr ago" else " hrs ago"
                            withStyle(androidx.compose.ui.text.SpanStyle(fontSize = numberSp, fontWeight = FontWeight.Bold, color = subtitleColor)) {
                                append(hrs)
                            }
                            withStyle(androidx.compose.ui.text.SpanStyle(fontSize = suffixSp, fontWeight = FontWeight.SemiBold, color = subtitleColor)) {
                                append(unit)
                            }
                        }
                    }
                }
                Text(
                    annotated,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .semantics {
                            fontSizeSp = suffixSp.value
                            contentDescription = when {
                                ageMs < 60_000L -> "just now"
                                ageMin in 1..59 -> if (ageMin == 1) "1 minute ago" else "$ageMin minutes ago"
                                else -> {
                                    val hrs = ageMin / 60
                                    if (hrs == 1) "1 hour ago" else "$hrs hours ago"
                                }
                            }
                        }
                        .testTag("alarmAge"),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        // Large, bold instruction placed directly above the action control
        val instructionStyle = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = subtitleColor,
            fontFamily = FontFamily.Default
        )
        val dismissInstruction = when {
            checkInTap -> "Tap I'm OK"
            dismissLabel == "DISMISS" -> "Tap to dismiss"
            cfg.screenWidthDp < 500 -> "Press and hold\nto dismiss"
            else -> "Press and hold to dismiss"
        }
        Text(
            text = "Tap Snooze to be woken again\n$dismissInstruction",
            style = instructionStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 8.dp)
                .semantics { fontSizeSp = instructionStyle.fontSize.value }
                .testTag("alarmInstruction")
        )
        val sideBySide = cfg.screenWidthDp >= 700
        if (sideBySide) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SnoozeButton(snoozeLabel, onSnooze, Modifier.weight(1f))
                DismissButton(
                    label = dismissLabel,
                    tapToDismiss = checkInTap,
                    holding = holding,
                    holdMs = holdMs,
                    onDismiss = onDismiss,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SnoozeButton(snoozeLabel, onSnooze, Modifier.fillMaxWidth())
                DismissButton(
                    label = dismissLabel,
                    tapToDismiss = checkInTap,
                    holding = holding,
                    holdMs = holdMs,
                    onDismiss = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SnoozeButton(label: String, onSnooze: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = 260.dp, minHeight = 120.dp)
            .clip(RoundedCornerShape(60.dp))
            .background(Color.White)
            .testTag("snoozeButton")
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .clickable(onClick = onSnooze)
            .padding(horizontal = 24.dp, vertical = if (LocalDensity.current.fontScale >= 1.5f) 8.dp else 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.displayMedium.copy(fontFamily = FontFamily.Default, fontSize = 32.sp),
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun DismissButton(
    label: String,
    tapToDismiss: Boolean,
    holding: androidx.compose.runtime.MutableState<Boolean>,
    holdMs: Long,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gesture = if (tapToDismiss) {
        Modifier.clickable(onClick = onDismiss)
    } else {
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    holding.value = true
                    val start = System.currentTimeMillis()
                    tryAwaitRelease()
                    val dur = System.currentTimeMillis() - start
                    holding.value = false
                    if (dur >= holdMs) {
                        onDismiss()
                    }
                }
            )
        }
    }
    Box(
        modifier = modifier
            .sizeIn(minWidth = 260.dp, minHeight = 120.dp)
            .clip(RoundedCornerShape(60.dp))
            .background(Color(0xFFE5E5E5))
            .then(gesture)
            .padding(horizontal = 24.dp, vertical = if (LocalDensity.current.fontScale >= 1.5f) 8.dp else 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.displayMedium.copy(fontFamily = FontFamily.Default, fontSize = 32.sp),
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

// Helper record for color tuple
private data class Quad(val a: Color, val b: Color, val title: Color, val subtitle: Color)
private operator fun Quad.component1() = a
private operator fun Quad.component2() = b
private operator fun Quad.component3() = title
private operator fun Quad.component4() = subtitle

