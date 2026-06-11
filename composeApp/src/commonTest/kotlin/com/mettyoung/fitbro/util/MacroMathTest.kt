package com.mettyoung.fitbro.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MacroMathTest {

    @Test
    fun caloriesFromMacrosUsesAtwater() {
        // 250c*4 + 150p*4 + 65f*9 = 1000 + 600 + 585 = 2185
        assertEquals(2185.0, MacroMath.caloriesFromMacros(carbG = 250.0, proteinG = 150.0, fatG = 65.0))
    }

    @Test
    fun caloriesZeroWhenAllMacrosZero() {
        assertEquals(0.0, MacroMath.caloriesFromMacros(0.0, 0.0, 0.0))
    }

    @Test
    fun percentsSplitByKcalContribution() {
        // kcal: p=600, c=1000, f=585, total=2185
        val p = MacroMath.macroPercents(carbG = 250.0, proteinG = 150.0, fatG = 65.0)!!
        assertEquals(27, p.proteinPct) // 600/2185 = 27.5 -> 27
        assertEquals(46, p.carbPct)    // 1000/2185 = 45.8 -> 46
        assertEquals(27, p.fatPct)     // 585/2185 = 26.8 -> 27
    }

    @Test
    fun percentsNullWhenNoMacros() {
        assertNull(MacroMath.macroPercents(0.0, 0.0, 0.0))
    }

    @Test
    fun fatWeightedNineKcalPerGram() {
        // pure fat: should be 100% fat
        val p = MacroMath.macroPercents(carbG = 0.0, proteinG = 0.0, fatG = 10.0)!!
        assertEquals(0, p.proteinPct)
        assertEquals(0, p.carbPct)
        assertEquals(100, p.fatPct)
    }
}
