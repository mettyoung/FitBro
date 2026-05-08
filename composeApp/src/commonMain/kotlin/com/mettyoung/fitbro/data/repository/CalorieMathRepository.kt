package com.mettyoung.fitbro.data.repository

import com.mettyoung.fitbro.data.model.ActivityBurn
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.data.model.DailyIntake
import com.mettyoung.fitbro.data.model.Metabolism

interface CalorieMathRepository {
    fun computeDailyBalance(
        intake: DailyIntake,
        metabolism: Metabolism,
        activity: ActivityBurn? = null
    ): CalorieResult<DailyBalance>
}

class CalorieMathRepositoryImpl : CalorieMathRepository {
    override fun computeDailyBalance(
        intake: DailyIntake,
        metabolism: Metabolism,
        activity: ActivityBurn?
    ): CalorieResult<DailyBalance> {
        if (intake.totalCalories < 0) return CalorieResult.Failure(
            CalorieMathError.InvalidInput("intake.totalCalories", "must be non-negative")
        )
        if (metabolism.bmr <= 0) return CalorieResult.Failure(
            CalorieMathError.InvalidInput("metabolism.bmr", "must be positive")
        )
        if (activity != null) {
            if (activity.neat < 0) return CalorieResult.Failure(
                CalorieMathError.InvalidInput("activity.neat", "must be non-negative")
            )
            if (activity.eat < 0) return CalorieResult.Failure(
                CalorieMathError.InvalidInput("activity.eat", "must be non-negative")
            )
        }

        val tef = computeTef(intake)
        if (tef < 0) return CalorieResult.Failure(
            CalorieMathError.InvalidInput("computed.tef", "must be non-negative")
        )

        val neat = activity?.neat ?: 0.0
        val eat = activity?.eat ?: 0.0
        val burn = neat + eat + metabolism.bmr + tef
        val balance = intake.totalCalories - burn

        return CalorieResult.Success(
            DailyBalance(
                date = intake.date,
                intake = intake.totalCalories,
                burn = burn,
                balance = balance,
                bmr = metabolism.bmr,
                tef = tef,
                neat = neat,
                eat = eat,
                proteinG = intake.proteinG,
                carbG = intake.carbG,
                fatG = intake.fatG
            )
        )
    }

    private fun computeTef(intake: DailyIntake): Double {
        val allMacrosZero = intake.proteinG == 0.0 && intake.carbG == 0.0 && intake.fatG == 0.0
        return if (allMacrosZero) {
            intake.totalCalories * 0.10
        } else {
            (intake.proteinG * 4 * 0.25) + (intake.carbG * 4 * 0.08) + (intake.fatG * 9 * 0.03)
        }
    }
}
