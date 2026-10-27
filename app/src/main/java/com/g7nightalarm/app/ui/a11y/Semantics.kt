package com.g7nightalarm.app.ui.a11y

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

// Expose font size in sp to tests via semantics
val FontSizeSpKey = SemanticsPropertyKey<Float>("fontSizeSp")
var SemanticsPropertyReceiver.fontSizeSp by FontSizeSpKey

