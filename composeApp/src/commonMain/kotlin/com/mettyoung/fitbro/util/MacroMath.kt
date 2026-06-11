package com.mettyoung.fitbro.util

import kotlin.math.roundToInt

/**
 * Atwater calorie math shared by the custom-food form and the macro-goals dialog.
 * 4 kcal/g carb, 4 kcal/g protein, 9 kcal/g fat.
 */
object MacroMath {

    const val KCAL_PER_G_CARB = 4
    const val KCAL_PER_G_PROTEIN = 4
    const val KCAL_PER_G_FAT = 9

    /** Total kcal from macro grams. */
    fun caloriesFromMacros(carbG: Double, proteinG: Double, fatG: Double): Double =
        carbG * KCAL_PER_G_CARB + proteinG * KCAL_PER_G_PROTEIN + fatG * KCAL_PER_G_FAT

    /**
     * Each macro's share of total macro-derived kcal, rounded to whole percent,
     * in (protein, carb, fat) order. Null when total kcal is 0 (nothing to split).
     */
    fun macroPercents(carbG: Double, proteinG: Double, fatG: Double): MacroPercents? {
        val pKcal = proteinG * KCAL_PER_G_PROTEIN
        val cKcal = carbG * KCAL_PER_G_CARB
        val fKcal = fatG * KCAL_PER_G_FAT
        val total = pKcal + cKcal + fKcal
        if (total <= 0) return null
        return MacroPercents(
            proteinPct = (pKcal * 100.0 / total).roundToInt(),
            carbPct = (cKcal * 100.0 / total).roundToInt(),
            fatPct = (fKcal * 100.0 / total).roundToInt()
        )
    }
}

data class MacroPercents(val proteinPct: Int, val carbPct: Int, val fatPct: Int)
