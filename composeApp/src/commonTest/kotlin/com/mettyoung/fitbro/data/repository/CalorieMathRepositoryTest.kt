package com.mettyoung.fitbro.data.repository

import com.mettyoung.fitbro.data.model.ActivityBurn
import com.mettyoung.fitbro.data.model.DailyIntake
import com.mettyoung.fitbro.data.model.Metabolism
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class CalorieMathRepositoryTest {

    private val repo: CalorieMathRepository = CalorieMathRepositoryImpl()

    private val intake = DailyIntake(date = "2026-05-06", totalCalories = 2000.0)
    private val metabolism = Metabolism(date = "2026-05-06", bmr = 1600.0, tef = 100.0)
    private val activity = ActivityBurn(date = "2026-05-06", neat = 200.0, eat = 300.0)

    @Test
    fun fullDataProducesCorrectBalance() {
        val result = repo.computeDailyBalance(intake, metabolism, activity)
        val success = assertIs<CalorieResult.Success<*>>(result)
        val balance = success.value
        assertIs<com.mettyoung.fitbro.data.model.DailyBalance>(balance)
        assertEquals(2000.0, balance.intake)
        // burn = NEAT(200) + EAT(300) + BMR(1600) + TEF(100) = 2200
        assertEquals(2200.0, balance.burn)
        // balance = 2000 - 2200 = -200 (deficit)
        assertEquals(-200.0, balance.balance)
        assertEquals("2026-05-06", balance.date)
    }

    @Test
    fun missingActivityFallsBackToBmrPlusTef() {
        val result = repo.computeDailyBalance(intake, metabolism, activity = null)
        val success = assertIs<CalorieResult.Success<*>>(result)
        val balance = success.value
        assertIs<com.mettyoung.fitbro.data.model.DailyBalance>(balance)
        // burn = BMR(1600) + TEF(100) = 1700
        assertEquals(1700.0, balance.burn)
        // balance = 2000 - 1700 = 300 (surplus)
        assertEquals(300.0, balance.balance)
    }

    @Test
    fun zeroIntakeProducesNegativeBalance() {
        val zeroIntake = DailyIntake(date = "2026-05-06", totalCalories = 0.0)
        val result = repo.computeDailyBalance(zeroIntake, metabolism, activity)
        val success = assertIs<CalorieResult.Success<*>>(result)
        val balance = success.value
        assertIs<com.mettyoung.fitbro.data.model.DailyBalance>(balance)
        assertEquals(0.0, balance.intake)
        assertEquals(-2200.0, balance.balance)
    }

    @Test
    fun zeroActivityValuesIncludedInBurn() {
        val zeroActivity = ActivityBurn(date = "2026-05-06", neat = 0.0, eat = 0.0)
        val result = repo.computeDailyBalance(intake, metabolism, zeroActivity)
        val success = assertIs<CalorieResult.Success<*>>(result)
        val balance = success.value
        assertIs<com.mettyoung.fitbro.data.model.DailyBalance>(balance)
        // burn = 0 + 0 + 1600 + 100 = 1700 (same as missing activity)
        assertEquals(1700.0, balance.burn)
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
        val badMetab = Metabolism(date = "2026-05-06", bmr = 0.0, tef = 100.0)
        val result = repo.computeDailyBalance(intake, badMetab, activity)
        val failure = assertIs<CalorieResult.Failure>(result)
        val error = assertIs<CalorieMathError.InvalidInput>(failure.error)
        assertEquals("metabolism.bmr", error.field)
    }

    @Test
    fun negativeBmrReturnsFailure() {
        val badMetab = Metabolism(date = "2026-05-06", bmr = -100.0, tef = 100.0)
        val result = repo.computeDailyBalance(intake, badMetab, activity)
        val failure = assertIs<CalorieResult.Failure>(result)
        assertIs<CalorieMathError.InvalidInput>(failure.error)
    }

    @Test
    fun negativeTefReturnsFailure() {
        val badMetab = Metabolism(date = "2026-05-06", bmr = 1600.0, tef = -1.0)
        val result = repo.computeDailyBalance(intake, badMetab, activity)
        val failure = assertIs<CalorieResult.Failure>(result)
        val error = assertIs<CalorieMathError.InvalidInput>(failure.error)
        assertEquals("metabolism.tef", error.field)
    }

    @Test
    fun negativeNeatReturnsFailure() {
        val badActivity = ActivityBurn(date = "2026-05-06", neat = -1.0, eat = 0.0)
        val result = repo.computeDailyBalance(intake, metabolism, badActivity)
        val failure = assertIs<CalorieResult.Failure>(result)
        val error = assertIs<CalorieMathError.InvalidInput>(failure.error)
        assertEquals("activity.neat", error.field)
    }

    @Test
    fun negativeEatReturnsFailure() {
        val badActivity = ActivityBurn(date = "2026-05-06", neat = 0.0, eat = -1.0)
        val result = repo.computeDailyBalance(intake, metabolism, badActivity)
        val failure = assertIs<CalorieResult.Failure>(result)
        val error = assertIs<CalorieMathError.InvalidInput>(failure.error)
        assertEquals("activity.eat", error.field)
    }
}
