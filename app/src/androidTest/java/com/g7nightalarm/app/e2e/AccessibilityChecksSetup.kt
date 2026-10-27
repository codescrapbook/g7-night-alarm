package com.g7nightalarm.app.e2e
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
                val cls = Class.forName("com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityChecks")
                val enableMethod = cls.getMethod("enable")
                val instance = enableMethod.invoke(null)
                val setRoot = instance.javaClass.getMethod("setRunChecksFromRootView", Boolean::class.javaPrimitiveType)
                setRoot.invoke(instance, true)
            } catch (_: Throwable) {
                // If the framework is unavailable in a specific environment, ignore enabling.
            }
        }
    }
}

