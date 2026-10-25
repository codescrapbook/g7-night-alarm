package com.g7nightalarm.app.net

import org.json.JSONArray

data class SgvEntry(val sgv: Double, val date: Long)

object Parsing {
    fun parseLatestSgv(json: String): SgvEntry? {
        val arr = JSONArray(json)
        if (arr.length() == 0) return null
        val obj = arr.getJSONObject(0)
        val sgv = obj.optDouble("sgv", Double.NaN)
        val date = obj.optLong("date", System.currentTimeMillis())
        if (sgv.isNaN()) return null
        return SgvEntry(sgv, date)
    }
}

