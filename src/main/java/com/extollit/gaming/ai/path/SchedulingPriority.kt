package com.extollit.gaming.ai.path

/**
 * Preset scheduling priorities for the co-routine-like behavior of the engine's A* triage process.  This determines
 * how many iterations (per cycle) a path-finding engine instance for an entity dedicates to the A* algorithm.
 *
 * @see IConfigModel.Schedule
 */
enum class SchedulingPriority(var initComputeIterations: Int, var periodicComputeIterations: Int) {
    /**
     * Indicates extreme-priority scheduling, entities with engines configured for this rating complete path-finding the
     * soonest.  While this results in the most fluid and deterministic pathing behavior it is also the most
     * computationally expensive and should only be used for special circumstances.
     * This is initialized with default values:
     * - 24 initial compute iterations
     * - 18 subsequent compute iterations
     */
    extreme(24, 18),

    /**
     * Indicates high-priority scheduling, entities with engines configured for this rating complete path-finding sooner.
     * This is initialized with default values:
     * - 12 initial compute iterations
     * - 7 subsequent compute iterations
     */
    high(12, 7),

    /**
     * Indicates low-priority scheduling, entities with engines configured for this rating complete path-finding later.
     * While the results of pathing for mobs with this scheduling priority can appear erratic or even stupid it is also
     * the least computationally expensive.  This scheduling priority is most suitable for mindless animals.
     * This is initialized with default values:
     * - 3 initial compute iterations
     * - 2 subsequent compute iterations
     */
    low(3, 2);

    companion object {
        /**
         * Used to configure the co-routine-like compute cycles for each of these priority ratings.
         *
         * @param IConfigModel source containing the appropriate configuration parameters
         * @see IConfigModel.scheduleFor
         */
        fun configureFrom(IConfigModel: IConfigModel) {
            for (priority in values()) {
                val schedule = IConfigModel.scheduleFor(priority)
                priority.initComputeIterations = schedule!!.init
                priority.periodicComputeIterations = schedule.period
            }
        }
    }
}