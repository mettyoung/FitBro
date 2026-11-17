@file:Suppress("DEPRECATION")

package com.mettyoung.fitbro.data.health

import com.mettyoung.fitbro.data.model.WorkoutType
import platform.HealthKit.*

/** FR-4: HealthKit workout activity type → shared [WorkoutType]. See tasks/workout-type-mapping.md. */
internal fun HKWorkoutActivityType.toWorkoutType(): WorkoutType = when (this) {
    HKWorkoutActivityTypeRunning -> WorkoutType.RUNNING
    HKWorkoutActivityTypeWalking -> WorkoutType.WALKING
    HKWorkoutActivityTypeHiking -> WorkoutType.HIKING
    HKWorkoutActivityTypeCycling, HKWorkoutActivityTypeHandCycling -> WorkoutType.CYCLING
    HKWorkoutActivityTypeSwimming -> WorkoutType.SWIMMING
    HKWorkoutActivityTypeElliptical -> WorkoutType.ELLIPTICAL
    HKWorkoutActivityTypeRowing -> WorkoutType.ROWING
    HKWorkoutActivityTypeStairClimbing, HKWorkoutActivityTypeStairs,
    HKWorkoutActivityTypeStepTraining -> WorkoutType.STAIR_CLIMBING
    HKWorkoutActivityTypeHighIntensityIntervalTraining, HKWorkoutActivityTypeMixedCardio,
    HKWorkoutActivityTypeMixedMetabolicCardioTraining -> WorkoutType.HIIT
    HKWorkoutActivityTypeJumpRope -> WorkoutType.JUMP_ROPE
    HKWorkoutActivityTypeCardioDance, HKWorkoutActivityTypeSocialDance,
    HKWorkoutActivityTypeDance, HKWorkoutActivityTypeDanceInspiredTraining -> WorkoutType.DANCE
    HKWorkoutActivityTypeCrossTraining -> WorkoutType.CROSS_TRAINING
    HKWorkoutActivityTypeWheelchairWalkPace, HKWorkoutActivityTypeWheelchairRunPace -> WorkoutType.WHEELCHAIR
    HKWorkoutActivityTypePaddleSports -> WorkoutType.PADDLING
    HKWorkoutActivityTypeCrossCountrySkiing, HKWorkoutActivityTypeDownhillSkiing,
    HKWorkoutActivityTypeSnowSports -> WorkoutType.SKIING
    HKWorkoutActivityTypeSnowboarding -> WorkoutType.SNOWBOARDING
    HKWorkoutActivityTypeSkatingSports -> WorkoutType.SKATING
    HKWorkoutActivityTypeSwimBikeRun, HKWorkoutActivityTypeTransition -> WorkoutType.MULTISPORT

    HKWorkoutActivityTypeTraditionalStrengthTraining,
    HKWorkoutActivityTypeFunctionalStrengthTraining -> WorkoutType.STRENGTH_TRAINING
    HKWorkoutActivityTypeCoreTraining -> WorkoutType.CALISTHENICS
    HKWorkoutActivityTypeGymnastics -> WorkoutType.GYMNASTICS

    HKWorkoutActivityTypeYoga -> WorkoutType.YOGA
    HKWorkoutActivityTypePilates, HKWorkoutActivityTypeBarre -> WorkoutType.PILATES
    HKWorkoutActivityTypeFlexibility, HKWorkoutActivityTypeCooldown,
    HKWorkoutActivityTypePreparationAndRecovery -> WorkoutType.STRETCHING
    HKWorkoutActivityTypeMindAndBody, HKWorkoutActivityTypeTaiChi -> WorkoutType.MINDFULNESS

    HKWorkoutActivityTypeBadminton -> WorkoutType.BADMINTON
    HKWorkoutActivityTypeTennis -> WorkoutType.TENNIS
    HKWorkoutActivityTypePickleball -> WorkoutType.PICKLEBALL
    HKWorkoutActivityTypeTableTennis -> WorkoutType.TABLE_TENNIS
    HKWorkoutActivityTypeSquash -> WorkoutType.SQUASH
    HKWorkoutActivityTypeRacquetball -> WorkoutType.RACQUETBALL
    HKWorkoutActivityTypeBasketball -> WorkoutType.BASKETBALL
    HKWorkoutActivityTypeSoccer -> WorkoutType.SOCCER
    HKWorkoutActivityTypeVolleyball -> WorkoutType.VOLLEYBALL
    HKWorkoutActivityTypeBaseball, HKWorkoutActivityTypeSoftball -> WorkoutType.BASEBALL_SOFTBALL
    HKWorkoutActivityTypeCricket -> WorkoutType.CRICKET
    HKWorkoutActivityTypeRugby -> WorkoutType.RUGBY
    HKWorkoutActivityTypeAmericanFootball -> WorkoutType.AMERICAN_FOOTBALL
    HKWorkoutActivityTypeAustralianFootball -> WorkoutType.AUSTRALIAN_FOOTBALL
    HKWorkoutActivityTypeHandball -> WorkoutType.HANDBALL
    HKWorkoutActivityTypeHockey -> WorkoutType.HOCKEY
    HKWorkoutActivityTypeLacrosse -> WorkoutType.LACROSSE
    HKWorkoutActivityTypeWaterPolo -> WorkoutType.WATER_POLO
    HKWorkoutActivityTypeDiscSports -> WorkoutType.DISC_SPORTS
    HKWorkoutActivityTypeBoxing, HKWorkoutActivityTypeKickboxing -> WorkoutType.BOXING
    HKWorkoutActivityTypeMartialArts, HKWorkoutActivityTypeWrestling,
    HKWorkoutActivityTypeFencing -> WorkoutType.MARTIAL_ARTS
    HKWorkoutActivityTypeClimbing -> WorkoutType.CLIMBING
    HKWorkoutActivityTypeGolf -> WorkoutType.GOLF
    HKWorkoutActivityTypeSurfingSports -> WorkoutType.SURFING
    HKWorkoutActivityTypeWaterSports, HKWorkoutActivityTypeWaterFitness -> WorkoutType.WATER_SPORTS
    HKWorkoutActivityTypeTrackAndField -> WorkoutType.TRACK_AND_FIELD

    // Other, Sailing, UnderwaterDiving, Archery, Bowling, Curling, EquestrianSports, Fishing,
    // Hunting, Play, FitnessGaming and any future/unknown value.
    else -> WorkoutType.OTHER
}
