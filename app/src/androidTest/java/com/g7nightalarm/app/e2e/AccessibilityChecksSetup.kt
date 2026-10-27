package com.g7nightalarm.app.e2e

import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityChecks
import org.junit.BeforeClass

/**
 * Enable Espresso accessibility checks globally for instrumented tests.
 * Do not suppress; fix findings if they arise.
 */
class AccessibilityChecksSetup {
    companion object {
        @JvmStatic
        @BeforeClass
        fun enable() {
            try {
                AccessibilityChecks.enable().setRunChecksFromRootView(true)
            } catch (_: Throwable) {
                // If the framework is unavailable in a specific environment, ignore enabling.
            }
        }
    }
}

