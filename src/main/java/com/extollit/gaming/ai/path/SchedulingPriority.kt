package com.extollit.gaming.ai.path

import com.extollit.gaming.ai.path.model.INode
import com.extollit.gaming.ai.path.model.NodeLinkedList
import kotlin.jvm.JvmOverloads
import com.extollit.gaming.ai.path.model.Passibility
import com.extollit.gaming.ai.path.model.Gravitation
import java.lang.StringBuilder
import java.text.MessageFormat
import java.util.Objects
import com.extollit.gaming.ai.path.model.IPath
import com.extollit.gaming.ai.path.model.IPathingEntity
import com.extollit.gaming.ai.path.model.Logic
import com.extollit.gaming.ai.path.model.IInstanceSpace
import com.extollit.gaming.ai.path.model.INodeCalculator
import com.extollit.gaming.ai.path.model.IOcclusionProviderFactory
import com.extollit.collect.SparseSpatialMap
import com.extollit.gaming.ai.path.model.IGraphNodeFilter
import com.extollit.gaming.ai.path.model.IOcclusionProvider
import com.extollit.gaming.ai.path.model.SortedNodeQueue
import com.extollit.linalg.immutable.IntAxisAlignedBox
import com.extollit.gaming.ai.path.model.FlagSampler
import java.lang.ArrayIndexOutOfBoundsException
import com.extollit.collect.ArrayIterable
import com.extollit.gaming.ai.path.model.PathObject
import com.extollit.num.FloatRange
import com.extollit.gaming.ai.path.IConfigModel
import com.extollit.gaming.ai.path.model.IncompletePath
import com.extollit.gaming.ai.path.model.IBlockDescription
import com.extollit.gaming.ai.path.model.ColumnarOcclusionFieldList
import com.extollit.gaming.ai.path.model.IBlockObject
import com.extollit.gaming.ai.path.model.IColumnarSpace
import com.extollit.gaming.ai.path.model.IDynamicMovableObject
import java.lang.NullPointerException
import java.lang.UnsupportedOperationException
import com.extollit.collect.CollectionsExt
import com.extollit.linalg.immutable.VertexOffset
import com.extollit.gaming.ai.path.model.OcclusionField.AreaInit
import com.extollit.gaming.ai.path.model.OcclusionField
import com.extollit.gaming.ai.path.model.TreeTransitional
import java.util.LinkedList
import java.util.Collections
import java.lang.IllegalStateException
import java.util.HashSet
import java.util.Deque
import com.extollit.gaming.ai.path.model.TreeTransitional.RotateNodeOp
import com.extollit.gaming.ai.path.SchedulingPriority
import com.extollit.gaming.ai.path.IConfigModel.Schedule
import com.extollit.gaming.ai.path.PassibilityHelpers
import java.lang.IllegalArgumentException
import com.extollit.gaming.ai.path.model.IPathProcessor
import com.extollit.gaming.ai.path.AreaOcclusionProviderFactory
import com.extollit.gaming.ai.path.HydrazinePathFinder
import com.extollit.gaming.ai.path.FluidicNodeCalculator
import com.extollit.gaming.ai.path.GroundNodeCalculator
import com.extollit.gaming.ai.path.AbstractNodeCalculator
import java.lang.Math
import com.extollit.gaming.ai.path.model.AreaOcclusionProvider

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