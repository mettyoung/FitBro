package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Verifies the goal-dialog wiring (not just the math): typing macros drives the
 * calorie field + per-macro percent labels via [MacroGoalsDialog]'s onValueChange.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class MacroGoalsDialogTest {

    @Test
    fun typingMacrosAutoFillsCaloriesAndPercents() = runComposeUiTest {
        setContent {
            MacroGoalsDialog(
                proteinGoal = 0.0,
                carbsGoal = 0.0,
                fatGoal = 0.0,
                calorieGoal = 0.0,
                onDismiss = {},
                onSave = { _, _, _, _ -> }
            )
        }

        onNodeWithText("Protein (g)").performTextReplacement("100")
        onNodeWithText("Carbohydrates (g)").performTextReplacement("200")
        onNodeWithText("Total Fats (g)").performTextReplacement("50")

        // 100*4 + 200*4 + 50*9 = 1650 kcal, auto-filled into the calories field.
        onNodeWithText("Calories (kcal)").assert(hasText("1650"))

        // protein 400/1650 = 24%, carbs 800/1650 = 48%, fat 450/1650 = 27%
        onNodeWithText("24%").assertIsDisplayed()
        onNodeWithText("48%").assertIsDisplayed()
        onNodeWithText("27%").assertIsDisplayed()
    }
}
