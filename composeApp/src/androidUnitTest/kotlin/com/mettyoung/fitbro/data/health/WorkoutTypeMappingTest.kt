package com.mettyoung.fitbro.data.health

import androidx.health.connect.client.records.ExerciseSessionRecord as E
import com.mettyoung.fitbro.data.model.WorkoutType
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkoutTypeMappingTest {

    @Test
    fun mapsCommonCardioTypes() {
        assertEquals(WorkoutType.RUNNING, exerciseTypeToWorkoutType(E.EXERCISE_TYPE_RUNNING))
        assertEquals(WorkoutType.RUNNING, exerciseTypeToWorkoutType(E.EXERCISE_TYPE_RUNNING_TREADMILL))
        assertEquals(WorkoutType.CYCLING, exerciseTypeToWorkoutType(E.EXERCISE_TYPE_BIKING_STATIONARY))
        assertEquals(WorkoutType.SWIMMING, exerciseTypeToWorkoutType(E.EXERCISE_TYPE_SWIMMING_OPEN_WATER))
        assertEquals(WorkoutType.WALKING, exerciseTypeToWorkoutType(E.EXERCISE_TYPE_WALKING))
    }

    @Test
    fun mapsStrengthAndMindBody() {
        assertEquals(WorkoutType.STRENGTH_TRAINING, exerciseTypeToWorkoutType(E.EXERCISE_TYPE_STRENGTH_TRAINING))
        assertEquals(WorkoutType.WEIGHTLIFTING, exerciseTypeToWorkoutType(E.EXERCISE_TYPE_WEIGHTLIFTING))
        assertEquals(WorkoutType.YOGA, exerciseTypeToWorkoutType(E.EXERCISE_TYPE_YOGA))
        assertEquals(WorkoutType.MINDFULNESS, exerciseTypeToWorkoutType(E.EXERCISE_TYPE_GUIDED_BREATHING))
    }

    @Test
    fun otherAndUnknownCodesBecomeOther() {
        assertEquals(WorkoutType.OTHER, exerciseTypeToWorkoutType(E.EXERCISE_TYPE_OTHER_WORKOUT))
        assertEquals(WorkoutType.OTHER, exerciseTypeToWorkoutType(E.EXERCISE_TYPE_SAILING))
        assertEquals(WorkoutType.OTHER, exerciseTypeToWorkoutType(9999))
    }
}
