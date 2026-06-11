package com.mettyoung.fitbro.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.mettyoung.fitbro.data.cache.UserSettingsDataSource
import com.mettyoung.fitbro.data.food.FoodDatabase
import com.mettyoung.fitbro.data.model.MacroDataSource
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Verifies the Settings goal-editor wiring: typing macros auto-fills the calorie
 * target and renders each macro's percent of total kcal via [MacroGoalsSettings].
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class MacroGoalsSettingsTest {

    private class FakeUserSettings : UserSettingsDataSource {
        var protein = 0.0
        var carbs = 0.0
        var fat = 0.0
        var calorie = 0.0
        override fun getProteinGoalG() = protein
        override fun setProteinGoalG(grams: Double) { protein = grams }
        override fun getCarbsGoalG() = carbs
        override fun setCarbsGoalG(grams: Double) { carbs = grams }
        override fun getFatGoalG() = fat
        override fun setFatGoalG(grams: Double) { fat = grams }
        override fun getCalorieGoalKcal() = calorie
        override fun setCalorieGoalKcal(kcal: Double) { calorie = kcal }
        override fun getFoodDatabase() = FoodDatabase.FATSECRET
        override fun setFoodDatabase(db: FoodDatabase) {}
        override fun getMacroDataSourceForDate(date: String) = MacroDataSource.HEALTH_CONNECT
        override fun setMacroDataSourceForDate(date: String, source: MacroDataSource) {}
    }

    @Test
    fun typingMacrosAutoFillsCalorieTargetAndPercents() = runComposeUiTest {
        setContent {
            MacroGoalsSettings(userSettingsDataSource = FakeUserSettings())
        }

        // Field order (document order): calorie, protein, carbs, fat.
        val fields = onAllNodes(hasSetTextAction())
        fields[1].performTextReplacement("100") // protein
        fields[2].performTextReplacement("200") // carbs
        fields[3].performTextReplacement("50")  // fat

        // 100*4 + 200*4 + 50*9 = 1650 kcal auto-filled into the calorie target.
        fields[0].assert(hasText("1650"))

        // protein 400/1650 = 24%, carbs 800/1650 = 48%, fat 450/1650 = 27%
        onNodeWithText("24%").assertIsDisplayed()
        onNodeWithText("48%").assertIsDisplayed()
        onNodeWithText("27%").assertIsDisplayed()
    }
}
