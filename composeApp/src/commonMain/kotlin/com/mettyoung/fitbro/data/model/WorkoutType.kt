package com.mettyoung.fitbro.data.model

enum class WorkoutCategory(val label: String) {
    CARDIO("Cardio"),
    STRENGTH("Strength"),
    MIND_BODY("Mind & body"),
    SPORT("Sport"),
    OTHER("Other")
}

/**
 * Platform-neutral workout type. Health Connect and HealthKit types are mapped onto this list
 * (see tasks/workout-type-mapping.md); anything unmapped becomes [OTHER].
 *
 * Entry **names** are persisted (hidden workout types) — don't rename an entry, or it silently un-hides.
 */
enum class WorkoutType(val label: String, val category: WorkoutCategory) {
    // Cardio
    RUNNING("Running", WorkoutCategory.CARDIO),
    WALKING("Walking", WorkoutCategory.CARDIO),
    HIKING("Hiking", WorkoutCategory.CARDIO),
    CYCLING("Cycling", WorkoutCategory.CARDIO),
    SWIMMING("Swimming", WorkoutCategory.CARDIO),
    ELLIPTICAL("Elliptical", WorkoutCategory.CARDIO),
    ROWING("Rowing", WorkoutCategory.CARDIO),
    STAIR_CLIMBING("Stair climbing", WorkoutCategory.CARDIO),
    HIIT("HIIT", WorkoutCategory.CARDIO),
    JUMP_ROPE("Jump rope", WorkoutCategory.CARDIO),
    DANCE("Dance", WorkoutCategory.CARDIO),
    CROSS_TRAINING("Cross training", WorkoutCategory.CARDIO),
    WHEELCHAIR("Wheelchair", WorkoutCategory.CARDIO),
    PADDLING("Paddling", WorkoutCategory.CARDIO),
    SKIING("Skiing", WorkoutCategory.CARDIO),
    SNOWBOARDING("Snowboarding", WorkoutCategory.CARDIO),
    SKATING("Skating", WorkoutCategory.CARDIO),
    MULTISPORT("Multisport", WorkoutCategory.CARDIO),

    // Strength
    STRENGTH_TRAINING("Strength training", WorkoutCategory.STRENGTH),
    WEIGHTLIFTING("Weightlifting", WorkoutCategory.STRENGTH),
    CALISTHENICS("Calisthenics", WorkoutCategory.STRENGTH),
    GYMNASTICS("Gymnastics", WorkoutCategory.STRENGTH),

    // Mind & body
    YOGA("Yoga", WorkoutCategory.MIND_BODY),
    PILATES("Pilates", WorkoutCategory.MIND_BODY),
    STRETCHING("Stretching", WorkoutCategory.MIND_BODY),
    MINDFULNESS("Mindfulness", WorkoutCategory.MIND_BODY),

    // Sport
    BADMINTON("Badminton", WorkoutCategory.SPORT),
    TENNIS("Tennis", WorkoutCategory.SPORT),
    PICKLEBALL("Pickleball", WorkoutCategory.SPORT),
    TABLE_TENNIS("Table tennis", WorkoutCategory.SPORT),
    SQUASH("Squash", WorkoutCategory.SPORT),
    RACQUETBALL("Racquetball", WorkoutCategory.SPORT),
    BASKETBALL("Basketball", WorkoutCategory.SPORT),
    SOCCER("Football", WorkoutCategory.SPORT),
    VOLLEYBALL("Volleyball", WorkoutCategory.SPORT),
    BASEBALL_SOFTBALL("Baseball / softball", WorkoutCategory.SPORT),
    CRICKET("Cricket", WorkoutCategory.SPORT),
    RUGBY("Rugby", WorkoutCategory.SPORT),
    AMERICAN_FOOTBALL("American football", WorkoutCategory.SPORT),
    AUSTRALIAN_FOOTBALL("Australian football", WorkoutCategory.SPORT),
    HANDBALL("Handball", WorkoutCategory.SPORT),
    HOCKEY("Hockey", WorkoutCategory.SPORT),
    LACROSSE("Lacrosse", WorkoutCategory.SPORT),
    WATER_POLO("Water polo", WorkoutCategory.SPORT),
    DISC_SPORTS("Disc sports", WorkoutCategory.SPORT),
    BOXING("Boxing", WorkoutCategory.SPORT),
    MARTIAL_ARTS("Martial arts", WorkoutCategory.SPORT),
    CLIMBING("Climbing", WorkoutCategory.SPORT),
    GOLF("Golf", WorkoutCategory.SPORT),
    SURFING("Surfing", WorkoutCategory.SPORT),
    WATER_SPORTS("Water sports", WorkoutCategory.SPORT),
    TRACK_AND_FIELD("Track & field", WorkoutCategory.SPORT),

    // Other
    OTHER("Other workout", WorkoutCategory.OTHER);

    companion object {
        /** FR-8: hidden until the user changes it. */
        val DEFAULT_HIDDEN: Set<WorkoutType> = entries
            .filter { it.category == WorkoutCategory.STRENGTH || it.category == WorkoutCategory.MIND_BODY }
            .toSet()

        /** FR-7: null (never saved) → defaults; unknown names are ignored. */
        fun parseHidden(stored: String?): Set<WorkoutType> {
            if (stored == null) return DEFAULT_HIDDEN
            val names = stored.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            return entries.filter { it.name in names }.toSet()
        }

        fun serializeHidden(hidden: Set<WorkoutType>): String =
            hidden.map { it.name }.sorted().joinToString(",")
    }
}

/** FR-9 */
fun List<CardioSession>.visible(hidden: Set<WorkoutType>): List<CardioSession> =
    if (hidden.isEmpty()) this else filterNot { it.type in hidden }
