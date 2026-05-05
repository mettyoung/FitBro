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
        if (metabolism.tef < 0) return CalorieResult.Failure(
            CalorieMathError.InvalidInput("metabolism.tef", "must be non-negative")
        )
        if (activity != null) {
            if (activity.neat < 0) return CalorieResult.Failure(
                CalorieMathError.InvalidInput("activity.neat", "must be non-negative")
            )
            if (activity.eat < 0) return CalorieResult.Failure(
                CalorieMathError.InvalidInput("activity.eat", "must be non-negative")
            )
        }

        val neat = activity?.neat ?: 0.0
        val eat = activity?.eat ?: 0.0
        val burn = neat + eat + metabolism.bmr + metabolism.tef
        val balance = intake.totalCalories - burn

        return CalorieResult.Success(
            DailyBalance(
                date = intake.date,
                intake = intake.totalCalories,
                burn = burn,
                balance = balance
            )
        )
    }
}
