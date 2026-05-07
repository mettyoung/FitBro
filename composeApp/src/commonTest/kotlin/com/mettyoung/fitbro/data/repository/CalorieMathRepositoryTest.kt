package com.mettyoung.fitbro.data.repository

import com.mettyoung.fitbro.data.model.ActivityBurn
import com.mettyoung.fitbro.data.model.DailyIntake
import com.mettyoung.fitbro.data.model.Metabolism
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CalorieMathRepositoryTest {

    private val repo: CalorieMathRepository = CalorieMathRepositoryImpl()

    private val metabolism = Metabolism(date = "2026-05-06", bmr = 1600.0, tef = 0.0)
    private val activity = ActivityBurn(date = "2026-05-06", neat = 200.0, eat = 300.0)

    @Test
    fun macroPathComputesTefFromProteinCarbsFat() {
        val intake = DailyIntake(
            date = "2026-05-06",
            totalCalories = 2000.0,
            proteinG = 150.0,
            carbG = 250.0,
            fatG = 65.0
        )
        val result = repo.computeDailyBalance(intake, metabolism, activity)
        val success = assertIs<CalorieResult.Success<*>>(result)
        val balance = success.value
        assertIs<com.mettyoung.fitbro.data.model.DailyBalance>(balance)
        // TEF = (150*4*0.25) + (250*4*0.08) + (65*9*0.03) = 150 + 80 + 17.55 = 247.55
        val expectedTef = (150.0 * 4 * 0.25) + (250.0 * 4 * 0.08) + (65.0 * 9 * 0.03)
        assertEquals(expectedTef, balance.tef, 0.01)
        // burn = NEAT(200) + EAT(300) + BMR(1600) + TEF(247.55) ≈ 2347.55
        val expectedBurn = 200.0 + 300.0 + 1600.0 + expectedTef
        assertEquals(expectedBurn, balance.burn, 0.01)
        // balance = 2000 - 2347.55 ≈ -347.55 (deficit)
        assertEquals(2000.0 - expectedBurn, balance.balance, 0.01)
    }

    @Test
    fun fallbackPathUsesFlatPercentageWhenAllMacrosZero() {
        val intake = DailyIntake(
            date = "2026-05-06",
            totalCalories = 2000.0,
            proteinG = 0.0,
            carbG = 0.0,
            fatG = 0.0
        )
        val result = repo.computeDailyBalance(intake, metabolism, activity)
        val success = assertIs<CalorieResult.Success<*>>(result)
        val balance = success.value
        assertIs<com.mettyoung.fitbro.data.model.DailyBalance>(balance)
        // TEF = 2000 * 0.10 = 200
        assertEquals(200.0, balance.tef)
        // burn = 200 + 300 + 1600 + 200 = 2300
        assertEquals(2300.0, balance.burn)
        // balance = 2000 - 2300 = -300
        assertEquals(-300.0, balance.balance)
    }

    @Test
    fun fullDataWithMacrosProducesCorrectBalance() {
        val intake = DailyIntake(
            date = "2026-05-06",
            totalCalories = 2000.0,
            proteinG = 100.0,
            carbG = 200.0,
            fatG = 50.0
        )
        val result = repo.computeDailyBalance(intake, metabolism, activity)
        val success = assertIs<CalorieResult.Success<*>>(result)
        val balance = success.value
        assertIs<com.mettyoung.fitbro.data.model.DailyBalance>(balance)
        assertEquals(2000.0, balance.intake)
        // TEF = (100*4*0.25) + (200*4*0.08) + (50*9*0.03) = 100 + 64 + 13.5 = 177.5
        assertEquals(177.5, balance.tef, 0.01)
    }

    @Test
    fun missingActivityFallsBackToBmrPlusTef() {
        val intake = DailyIntake(
            date = "2026-05-06",
            totalCalories = 2000.0,
            proteinG = 100.0,
            carbG = 200.0,
            fatG = 50.0
        )
        val result = repo.computeDailyBalance(intake, metabolism, activity = null)
        val success = assertIs<CalorieResult.Success<*>>(result)
        val balance = success.value
        assertIs<com.mettyoung.fitbro.data.model.DailyBalance>(balance)
        // TEF = (100*4*0.25) + (200*4*0.08) + (50*9*0.03) = 177.5
        // burn = BMR(1600) + TEF(177.5) = 1777.5
        assertEquals(1777.5, balance.burn, 0.01)
        // balance = 2000 - 1777.5 = 222.5 (surplus)
        assertEquals(222.5, balance.balance, 0.01)
    }

    @Test
    fun zeroIntakeProducesNegativeBalance() {
        val zeroIntake = DailyIntake(
            date = "2026-05-06",
            totalCalories = 0.0,
            proteinG = 0.0,
            carbG = 0.0,
            fatG = 0.0
        )
        val result = repo.computeDailyBalance(zeroIntake, metabolism, activity)
        val success = assertIs<CalorieResult.Success<*>>(result)
        val balance = success.value
        assertIs<com.mettyoung.fitbro.data.model.DailyBalance>(balance)
        assertEquals(0.0, balance.intake)
        // TEF = 0 * 0.10 = 0; burn = 200 + 300 + 1600 + 0 = 2100
        assertEquals(2100.0, balance.burn)
        assertEquals(-2100.0, balance.balance)
    }

    @Test
    fun zeroActivityValuesIncludedInBurn() {
        val zeroActivity = ActivityBurn(date = "2026-05-06", neat = 0.0, eat = 0.0)
        val intake = DailyIntake(
            date = "2026-05-06",
            totalCalories = 2000.0,
            proteinG = 100.0,
            carbG = 200.0,
            fatG = 50.0
        )
        val result = repo.computeDailyBalance(intake, metabolism, zeroActivity)
        val success = assertIs<CalorieResult.Success<*>>(result)
        val balance = success.value
        assertIs<com.mettyoung.fitbro.data.model.DailyBalance>(balance)
        // TEF = 177.5; burn = 0 + 0 + 1600 + 177.5 = 1777.5
        assertEquals(1777.5, balance.burn, 0.01)
    }

    @Test
    fun negativeIntakeReturnsFailure() {
        val badIntake = DailyIntake(date = "2026-05-06", totalCalories = -1.0)
        val result = repo.computeDailyBalance(badIntake, metabolism, activity)
        val failure = assertIs<CalorieResult.Failure>(result)
        val error = assertIs<CalorieMathError.InvalidInput>(failure.error)
        assertEquals("intake.totalCalories", error.field)
    }

    @Test
    fun zeroBmrReturnsFailure() {
        val badMetab = Metabolism(date = "2026-05-06", bmr = 0.0, tef = 0.0)
        val intake = DailyIntake(date = "2026-05-06", totalCalories = 2000.0)
        val result = repo.computeDailyBalance(intake, badMetab, activity)
        val failure = assertIs<CalorieResult.Failure>(result)
        val error = assertIs<CalorieMathError.InvalidInput>(failure.error)
        assertEquals("metabolism.bmr", error.field)
    }

    @Test
    fun negativeBmrReturnsFailure() {
        val badMetab = Metabolism(date = "2026-05-06", bmr = -100.0, tef = 0.0)
        val intake = DailyIntake(date = "2026-05-06", totalCalories = 2000.0)
        val result = repo.computeDailyBalance(intake, badMetab, activity)
        val failure = assertIs<CalorieResult.Failure>(result)
        assertIs<CalorieMathError.InvalidInput>(failure.error)
    }

    @Test
    fun negativeNeatReturnsFailure() {
        val badActivity = ActivityBurn(date = "2026-05-06", neat = -1.0, eat = 0.0)
        val intake = DailyIntake(date = "2026-05-06", totalCalories = 2000.0)
        val result = repo.computeDailyBalance(intake, metabolism, badActivity)
        val failure = assertIs<CalorieResult.Failure>(result)
        val error = assertIs<CalorieMathError.InvalidInput>(failure.error)
        assertEquals("activity.neat", error.field)
    }

    @Test
    fun negativeEatReturnsFailure() {
        val badActivity = ActivityBurn(date = "2026-05-06", neat = 0.0, eat = -1.0)
        val intake = DailyIntake(date = "2026-05-06", totalCalories = 2000.0)
        val result = repo.computeDailyBalance(intake, metabolism, badActivity)
        val failure = assertIs<CalorieResult.Failure>(result)
        val error = assertIs<CalorieMathError.InvalidInput>(failure.error)
        assertEquals("activity.eat", error.field)
    }
}
