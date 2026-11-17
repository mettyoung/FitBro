# Workout type mapping (draft)

Shared `WorkoutType` (commonMain) ← Health Connect `ExerciseSessionRecord.EXERCISE_TYPE_*` (connect-client 1.1.0-rc01, 58 types) and HealthKit `HKWorkoutActivityType*` (iOS SDK, ~85 types).
Each `WorkoutType` carries a `category`. Default-hidden categories: **STRENGTH**, **MIND_BODY**.

## CARDIO
| WorkoutType | Health Connect | HealthKit |
|---|---|---|
| RUNNING | RUNNING, RUNNING_TREADMILL | Running |
| WALKING | WALKING | Walking |
| HIKING | HIKING, SNOWSHOEING | Hiking |
| CYCLING | BIKING, BIKING_STATIONARY | Cycling, HandCycling |
| SWIMMING | SWIMMING_POOL, SWIMMING_OPEN_WATER | Swimming |
| ELLIPTICAL | ELLIPTICAL | Elliptical |
| ROWING | ROWING, ROWING_MACHINE | Rowing |
| STAIR_CLIMBING | STAIR_CLIMBING, STAIR_CLIMBING_MACHINE | StairClimbing, Stairs, StepTraining |
| HIIT | HIGH_INTENSITY_INTERVAL_TRAINING | HighIntensityIntervalTraining, MixedCardio, MixedMetabolicCardioTraining* |
| JUMP_ROPE | — | JumpRope |
| DANCE | DANCING | CardioDance, SocialDance, Dance*, DanceInspiredTraining* |
| CROSS_TRAINING | BOOT_CAMP, EXERCISE_CLASS | CrossTraining |
| WHEELCHAIR | WHEELCHAIR | WheelchairWalkPace, WheelchairRunPace |
| PADDLING | PADDLING | PaddleSports |
| SKIING | SKIING | CrossCountrySkiing, DownhillSkiing, SnowSports |
| SNOWBOARDING | SNOWBOARDING | Snowboarding |
| SKATING | SKATING, ICE_SKATING | SkatingSports |
| MULTISPORT | — | SwimBikeRun, Transition |

## STRENGTH (hidden by default)
| WorkoutType | Health Connect | HealthKit |
|---|---|---|
| STRENGTH_TRAINING | STRENGTH_TRAINING | TraditionalStrengthTraining, FunctionalStrengthTraining |
| WEIGHTLIFTING | WEIGHTLIFTING | — |
| CALISTHENICS | CALISTHENICS | CoreTraining |
| GYMNASTICS | GYMNASTICS | Gymnastics |

## MIND_BODY (hidden by default)
| WorkoutType | Health Connect | HealthKit |
|---|---|---|
| YOGA | YOGA | Yoga |
| PILATES | PILATES | Pilates, Barre |
| STRETCHING | STRETCHING | Flexibility, Cooldown, PreparationAndRecovery |
| MINDFULNESS | GUIDED_BREATHING | MindAndBody, TaiChi |

## SPORT
| WorkoutType | Health Connect | HealthKit |
|---|---|---|
| BADMINTON | BADMINTON | Badminton |
| TENNIS | TENNIS | Tennis |
| PICKLEBALL | — | Pickleball |
| TABLE_TENNIS | TABLE_TENNIS | TableTennis |
| SQUASH | SQUASH | Squash |
| RACQUETBALL | RACQUETBALL | Racquetball |
| BASKETBALL | BASKETBALL | Basketball |
| SOCCER | SOCCER | Soccer |
| VOLLEYBALL | VOLLEYBALL | Volleyball |
| BASEBALL_SOFTBALL | BASEBALL, SOFTBALL | Baseball, Softball |
| CRICKET | CRICKET | Cricket |
| RUGBY | RUGBY | Rugby |
| AMERICAN_FOOTBALL | FOOTBALL_AMERICAN | AmericanFootball |
| AUSTRALIAN_FOOTBALL | FOOTBALL_AUSTRALIAN | AustralianFootball |
| HANDBALL | HANDBALL | Handball |
| HOCKEY | ICE_HOCKEY, ROLLER_HOCKEY | Hockey |
| LACROSSE | — | Lacrosse |
| WATER_POLO | WATER_POLO | WaterPolo |
| DISC_SPORTS | FRISBEE_DISC | DiscSports |
| BOXING | BOXING | Boxing, Kickboxing |
| MARTIAL_ARTS | MARTIAL_ARTS, FENCING | MartialArts, Wrestling, Fencing |
| CLIMBING | ROCK_CLIMBING | Climbing |
| GOLF | GOLF | Golf |
| SURFING | SURFING | SurfingSports |
| WATER_SPORTS | — | WaterSports, WaterFitness |
| TRACK_AND_FIELD | — | TrackAndField |

## OTHER
| WorkoutType | Health Connect | HealthKit |
|---|---|---|
| OTHER | OTHER_WORKOUT, SAILING, SCUBA_DIVING, PARAGLIDING, *any unknown/new code* | Other, Sailing, UnderwaterDiving, Archery, Bowling, Curling, EquestrianSports, Fishing, Hunting, Play, FitnessGaming, *any unknown/new value* |

\* deprecated HealthKit values, still mapped so older workouts read correctly.

Not modelled here: the workout title. Mi Fitness puts niche sports (e.g. padel) under OTHER_WORKOUT with the real name only in the title.
