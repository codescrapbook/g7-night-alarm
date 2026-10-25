package com.g7nightalarm.app.util

import com.g7nightalarm.app.settings.Units

object UnitsUtil {
    fun mgdlToMmol(mgdl: Double): Double = mgdl / 18.0
    fun formatValue(mgdl: Double, units: Units): String {
        return if (units == Units.MG_DL) {
            java.text.DecimalFormat("###0.#").format(mgdl)
        } else {
            java.text.DecimalFormat("##0.0").format(mgdlToMmol(mgdl))
        }
    }
    fun convertThreshold(mgdl: Double, units: Units): Double =
        if (units == Units.MG_DL) mgdl else mgdlToMmol(mgdl)
}

