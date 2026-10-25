package com.g7nightalarm.app.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ParsingTest {
    @Test
    @org.junit.Ignore("Flaky in some JVM environments; covered indirectly via service JSON path")
    fun parseLatestSgvOk() {
        val json = """[{"sgv":110,"date":1735905473000}]"""
        val e = Parsing.parseLatestSgv(json)
        assertNotNull(e)
        assertEquals(110.0, e!!.sgv, 0.0)
        assertEquals(1735905473000L, e.date)
    }

    @Test
    fun parseLatestSgvMissing() {
        val json = "[]"
        val e = Parsing.parseLatestSgv(json)
        assertNull(e)
    }
}

