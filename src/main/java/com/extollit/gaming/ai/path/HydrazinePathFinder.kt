package com.extollit.gaming.ai.path

import com.extollit.gaming.ai.path.model.*
import com.extollit.linalg.immutable.Vec3i
import com.extollit.linalg.mutable.AxisAlignedBBox
import com.extollit.linalg.mutable.Vec3d
import com.extollit.num.FloatRange
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * This is the primary path-finding object and root instance for the library.  There is precisely one instance of this
 * class for each and every entity that requires path-finding.  As such it is a good idea to maintain an instance
 * of this class in a member field of the associated pathing entity.
 *
 * This algorithm is iterative, callers call an update method each tick to progressively compute a path as the entity
 * traverses along it ultimately distributing computation across time.
 *
 * To use this class, first initiate path-finding using one of the initiation methods, then call [.updatePathFor]
 * each tick to iterate on the path until it is completed.  To abort path-finding call [.reset]
 */
class HydrazinePathFinder internal constructor(
    /** The entity that uses this object for path-finding operations */
    val subject: IPathingEntity,
    /** The instance space that the entity is contained within and should path-find in */
    private val instanceSpace: IInstanceSpace,
    occlusionProviderFactory: IOcclusionProviderFactory = AreaOcclusionProviderFactory
) {
    @kotlin.jvm.JvmField
    val queue: SortedNodeQueue = SortedNodeQueue()
    val nodeMap: NodeMap = NodeMap(instanceSpace, occlusionProviderFactory)
    private val unreachableFromSource: MutableSet<Vec3i> = HashSet(3)
    private var sourcePosition: Vec3d? = null
    private var destinationPosition: Vec3d? = null
    private var destinationEntity: IDynamicMovableObject? = null
    private var pathPointCalculator: INodeCalculator? = null
    private var pathProcessor: IPathProcessor? = null
    private var currentPath: IPath? = null
    private var capabilities: IPathingEntity.Capabilities? = null
    private var flying = false
    private var aqua = false
    private var pathPointCalculatorChanged = false
    private var trimmedToCurrent = false
    private var bestEffort = false
    private var current: Node? = null
    private var source: Node? = null
    private var target: Node? = null
    private var closest: Node? = null
    private var initComputeIterations = 0
    private var periodicComputeIterations = 0
    private var faultCount = 0
    private var nextGraphResetFailureCount = 0
    private var searchRangeSquared = 0f
    private var passiblePointPathTimeLimit = 0f
    private var nextGraphCacheReset = 0f
    private var actualSize = 0f

    /**
     * Random used for fuzzy logic.
     */
    var random: Random = Random()

    /**
     * Applies a scheduling priority to this path-finder (and associated entity)
     *
     * @param schedulingPriority priority to use for path-finding with this object's bound entity
     */
    fun schedulingPriority(schedulingPriority: SchedulingPriority) {
        schedulingPriority(schedulingPriority.initComputeIterations, schedulingPriority.periodicComputeIterations)
    }

    fun schedulingPriority(initComputeIterations: Int, periodicComputeIterations: Int) {
        this.initComputeIterations = initComputeIterations
        this.periodicComputeIterations = periodicComputeIterations
    }

    /**
     * If the last operation called on this path-finder was to track path-finding to some other entity then this will
     * provide the current destination the pathing entity is trying to reach, which will be within close proximity of the
     * tracked entity.
     *
     * @return an approximate (rounded) position near the destination entity being tracked, null if this is not tracking an entity destination
     */
    fun trackingDestination(): Vec3i? {
        return if (destinationEntity != null && destinationPosition != null) {
            val pointAtDestination = edgeAtDestination()
            pointAtDestination?.coordinates
        } else null
    }

    /**
     * Current immediate path-finding target of the pathing entity.  This is not necessarily the final destination of
     * the path, it can also be an intermediary step toward the final destination as well.
     *
     * @return the current target destination, can be null if there is no target available.
     */
    fun currentTarget(): Vec3i? = if (target == null) null else target!!.coordinates

    /**
     * Begin path-finding to a destination entity and update the path as necessary as the destination entity changes
     * it's location.  This state is retained until a call to one of the other path-finding initiation methods
     *
     * @param target destination / target entity to track and path-find to
     * @return the best path available toward the destination, the complete path to the destination, or null if a path
     * cannot be computed at all from the current location
     * @see .updatePathFor
     */
    fun trackPathTo(target: IDynamicMovableObject): IPath? {
        destinationEntity = target
        return initiatePathTo(target.coordinates(), true)
    }

    /**
     * Completely computes a path to the specified location.  This is the traditional A* search algorithm, which
     * trades-off performance for accuracy.
     *
     * NOTE: This method can be significantly more expensive than the others since it does not return until all search
     * options have been exhausted.
     *
     * @param coordinates the target destination to path-find to
     * @return the complete path to the destination, or null if the destination is unreachable from the current location
     */
    fun computePathTo(coordinates: com.extollit.linalg.immutable.Vec3d): IPath? =
        computePathTo(coordinates.x, coordinates.y, coordinates.z)

    /**
     * Completely computes a path to the specified location.  This is the traditional A* search algorithm, which
     * trades-off performance for accuracy.
     *
     * NOTE: This method can be significantly more expensive than the others since it does not return until all search
     * options have been exhausted.
     *
     * @param x x-coordinate of the destination
     * @param y y-coordinate of the destination
     * @param z z-coordinate of the destination
     * @return the complete path to the destination, or null if the destination is unreachable from the current location
     */
    fun computePathTo(x: Double, y: Double, z: Double): IPath? {
        destinationEntity = null
        bestEffort = false
        initializeOperation()
        if (tooFarTo(x, y, z)) return null
        updateDestination(x, y, z)
        if (!graphTimeout()) resetTriage()
        return triage(Int.MAX_VALUE)
    }

    /**
     * Starts path-finding to the specified destination using the best-effort algorithm.
     *
     * After an initial call to this method, the caller should make subsequent calls to
     * [.updatePathFor] until path-finding is completed or exhausted.
     *
     * @param coordinates the coordinates to start path-finding toward
     * @return the best path available toward the destination, the complete path to the destination, or null if a path
     * cannot be computed at all from the current location
     * @see .initiatePathTo
     */
    fun initiatePathTo(coordinates: com.extollit.linalg.immutable.Vec3d): IPath? =
        initiatePathTo(coordinates.x, coordinates.y, coordinates.z)

    /**
     * Starts path-finding to the specified destination using either best-effort or not.
     * Best-effort means that the algorithm will try it's best to get as close as possible to the target
     * destination even if it is unreachable.
     *
     * After an initial call to this method, the caller should make subsequent calls to
     * [.updatePathFor] until path-finding is completed or exhausted.
     *
     * @param coordinates the coordinates to start path-finding toward
     * @param bestEffort whether to use the best-effort algorithm to the destination, if false then this method is more likely to return immediately
     * @return the best path available toward the destination, the complete path to the destination, or if the best-effort
     * algorithm is not used then this is more likely to return immediately with null if it is determined the
     * destination is unreachable.
     */
    fun initiatePathTo(coordinates: com.extollit.linalg.immutable.Vec3d?, bestEffort: Boolean): IPath? =
        initiatePathTo(coordinates!!.x, coordinates.y, coordinates.z, bestEffort)
    /**
     * Starts path-finding to the specified destination using either best-effort or not.
     * Best-effort means that the algorithm will try it's best to get as close as possible to the target
     * destination even if it is unreachable.
     *
     * After an initial call to this method, the caller should make subsequent calls to
     * [.updatePathFor] until path-finding is completed or exhausted.
     *
     * @param x x-coordinate of the destination
     * @param y y-coordinate of the destination
     * @param z z-coordinate of the destination
     * @param bestEffort whether to use the best-effort algorithm to the destination, if false then this method is more likely to return immediately
     * @return the best path available toward the destination, the complete path to the destination, or if the best-effort
     * algorithm is not used then this is more likely to return immediately with null if it is determined the
     * destination is unreachable.
     */
    @JvmOverloads
    fun initiatePathTo(x: Double, y: Double, z: Double, bestEffort: Boolean = true): IPath? {
        this.bestEffort = bestEffort
        initializeOperation()
        if (!bestEffort && tooFarTo(x, y, z)) return null
        val initiate = updateDestination(x, y, z) && queue.isEmpty
        if (!graphTimeout() && (initiate || reachedTarget() || triageTimeout() || destinationDeviatedFromTarget())) resetTriage()
        return triage(initComputeIterations)
    }

    private fun tooFarTo(x: Double, y: Double, z: Double): Boolean {
        val rangeSquared = searchRangeSquared
        val sourcePos = com.extollit.linalg.immutable.Vec3d(sourcePosition)
        return sourcePos.subOf(x, y, z).mg2() > rangeSquared
    }

    private fun initializeOperation() {
        applySubject()
        updateSourcePosition()
        resetFaultTimings()
    }

    /**
     * After a path-finding operation has been initiated by one of the initiation methods, sub-sequent calls to this
     * method are used to refine path-finding and continue path-finding to the destination requested when the initiation
     * method was called.  This method also drives the entity along its path.
     *
     * @param pathingEntity the pathing entity that will receive movement commands along the path
     * @return the next and updated / refined path or null if the destination is unreachable
     */
    fun updatePathFor(pathingEntity: IPathingEntity): IPath? {
        val path = update() ?: return null
        if (!path.done()) {
            path.update(pathingEntity)
            if (!path.done()) return path
        }
        val last = path.last()
        return if (last != null) {
            val dd = Vec3d(destinationPosition)
            dd.sub(last.coordinates)
            if (dd.mg2() < 1) null else IncompletePath(last)
        } else null
    }

    /**
     * Optionally apply a graph node filter to this object which will be applied to all path-points computed by this path-finder.
     * This allows a caller to modify the passibility of points as they are computed.  For example, vampires are
     * vulnerable to sunlight, so a filter used here could mark all sunlit points as [Passibility.dangerous].
     *
     * @param filter a caller-supplied callback for altering node passibility
     * @return this
     */
    fun withGraphNodeFilter(filter: IGraphNodeFilter?): HydrazinePathFinder {
        nodeMap.filter(filter)
        return this
    }

    /**
     * Retrieve the current graph node filter (if one was set)
     *
     * @return current graph node filter, null if not set
     */
    fun graphNodeFilter(): IGraphNodeFilter? = nodeMap.filter()

    /**
     * Apply a path processor to this object which will be applied to all computed paths by this path-finder.
     * This allows a caller to modify computed paths before they are returned to the caller, typically this would
     * involve trimming a path shorter if desired.
     *
     * @param trimmer a callback used to modify paths computed by this engine
     * @return this
     */
    fun withPathProcessor(trimmer: IPathProcessor?): HydrazinePathFinder {
        pathProcessor = trimmer
        return this
    }

    /**
     * Retrieve the current path processor (if one was set)
     *
     * @return current path processor, null if not set
     */
    fun pathProcessor(): IPathProcessor? = pathProcessor

    fun update(): IPath? {
        if (destinationEntity != null) updateDestination(destinationEntity!!.coordinates())
        if (destinationPosition == null) return currentPath
        updateSourcePosition()
        graphTimeout()
        if (faultCount >= FAULT_LIMIT || reachedTarget()) {
            resetTriage()
            return null
        }
        if (triageTimeout() || destinationDeviatedFromTarget()) resetTriage()
        return triage(periodicComputeIterations)
    }

    private fun destinationDeviatedFromTarget(): Boolean {
        val dt = Vec3d(target!!.coordinates)
        val dd = Vec3d(destinationPosition)
        dd.x = floor(dd.x)
        dd.y = ceil(dd.y)
        dd.z = floor(dd.z)
        val source = source!!.coordinates
        dt.sub(source)
        dd.sub(source)
        if (dt.mg2() > dd.mg2()) return true
        dt.normalize()
        dd.normalize()
        return dt.dot(dd) < DOT_THRESHOLD
    }

    private fun triageTimeout(): Boolean {
        val currentPath = currentPath
        val status = PathObject.active(currentPath) && currentPath!!.length() > 0 && currentPath.stagnantFor(
            subject
        ) > passiblePointPathTimeLimit
        if (status) {
            if (++faultCount == 1) nextGraphCacheReset = pathTimeAge() + PROBATIONARY_TIME_LIMIT!!.next(
                random
            )
            val culprit = currentPath!!.current()
            nodeMap.cullBranchAt(culprit.coordinates, queue)
            passiblePointPathTimeLimit += PASSIBLE_POINT_TIME_LIMIT!!.next(random)
        }
        return status
    }

    private fun graphTimeout(): Boolean {
        val failureCount = faultCount
        if (failureCount >= nextGraphResetFailureCount
            && pathTimeAge() > nextGraphCacheReset
        ) {
            nextGraphResetFailureCount = failureCount + FAULT_COUNT_THRESHOLD
            resetGraph()
            return true
        }
        if (pathPointCalculatorChanged) {
            resetGraph()
            return true
        }
        return false
    }

    fun resetTriage() {
        val sourcePosition = sourcePosition
        val destinationPosition = destinationPosition
        updateFieldWindow(
            floor(sourcePosition!!.x).toInt(),
            floor(sourcePosition.z).toInt(),
            floor(destinationPosition!!.x).toInt(),
            floor(destinationPosition.z).toInt(), true
        )
        applySubject()
        source = pointAtSource()
        current = source
        val source = current
        source!!.length(0)
        source.isolate()
        trimmedToCurrent = true
        refinePassibility(source.coordinates)
        setTargetFor(source)
        nodeMap.reset(queue)
        queue.add(source)
        closest = null
        passiblePointPathTimeLimit = PASSIBLE_POINT_TIME_LIMIT!!.next(random)
    }

    protected fun refinePassibility(sourcePoint: Vec3i?): Boolean {
        unreachableFromSource.clear()
        if (!fuzzyPassibility(sourcePoint!!.x, sourcePoint.y, sourcePoint.z)) return false
        val blockObject = instanceSpace.blockObjectAt(sourcePoint.x, sourcePoint.y, sourcePoint.z)
        if (!blockObject.impeding) return false
        val bounds = blockObject.bounds()
        val c = Vec3d(subject.coordinates())
        c.sub(sourcePoint)
        val delta = com.extollit.linalg.immutable.Vec3d(c)
        c.sub(bounds.center())
        var mutated = false
        if (delta.z >= bounds.min.z && delta.z <= bounds.max.z) {
            val x = sourcePoint.x + if (c.x < 0) +1 else -1
            for (dz in -1..+1) unreachableFromSource.add(
                Vec3i(x, sourcePoint.y, sourcePoint.z + dz)
            )
            mutated = true
        }
        if (delta.x >= bounds.min.x && delta.x <= bounds.max.x) {
            val z = sourcePoint.z + if (c.z < 0) +1 else -1
            for (dx in -1..+1) unreachableFromSource.add(
                Vec3i(sourcePoint.x + dx, sourcePoint.y, z)
            )
            mutated = true
        }
        return mutated
    }

    private fun createPassibilityCalculator(capabilities: IPathingEntity.Capabilities?): INodeCalculator {
        val calculator: INodeCalculator
        val flyer = capabilities!!.avian()
        val swimmer = capabilities.swimmer()
        val gilled = capabilities.aquatic()
        calculator =
            if (flyer || swimmer && gilled) FluidicNodeCalculator(instanceSpace) else GroundNodeCalculator(instanceSpace)
        return calculator
    }

    private fun applySubject() {
        capabilities = subject.capabilities()
        val capabilities = capabilities
        val flying = capabilities!!.avian()
        val aqua = capabilities.swimmer() && capabilities.aquatic()
        val initPathPointCalculator = pathPointCalculator == null
        if (initPathPointCalculator || flying != this.flying || aqua != this.aqua) {
            pathPointCalculatorChanged = !initPathPointCalculator
            nodeMap.calculator(createPassibilityCalculator(capabilities).apply { pathPointCalculator = this })
            this.flying = flying
            this.aqua = aqua
        }
        actualSize = subject.width()
        pathPointCalculator!!.applySubject(subject)
        val pathSearchRange = subject.searchRange()
        searchRangeSquared = pathSearchRange * pathSearchRange
    }

    private fun setTargetFor(source: Node?): Boolean {
        val destinationPosition = destinationPosition
        when {
            null == edgeAtDestination().also { target = it } -> return false
            bestEffort -> {
                var distance: Int = Node.MAX_PATH_DISTANCE.toInt()
                while (distance > 0 && !source!!.target(target!!.coordinates)) {
                    val v = Vec3d(destinationPosition)
                    val init = Vec3d(source.coordinates)
                    distance--
                    v.sub(init)
                    v.normalize()
                    v.mul(distance.toDouble())
                    v.add(init)
                    target = edgeAtTarget(v.x, v.y, v.z)
                }
                return if (distance == 0) source!!.target(this.source.also { target = it }!!.coordinates) else true
            }
            source!!.target(target!!.coordinates) -> return true
            else -> {
                target = null
                return false
            }
        }
    }

    private fun resetGraph() {
        nodeMap.clear()
        resetTriage()
        nextGraphCacheReset = 0f
        pathPointCalculatorChanged = false
    }

    private fun updateFieldWindow(path: IPath) {
        var node: INode? = path.last() ?: return
        var pp = node?.coordinates
        val min = com.extollit.linalg.mutable.Vec3i(pp!!.x, pp.y, pp.z)
        val max = com.extollit.linalg.mutable.Vec3i(pp.x, pp.y, pp.z)
        if (!path.done()) for (c in path.cursor() until path.length()) {
            node = path.at(c)
            pp = node.coordinates
            if (pp.x < min.x) min.x = pp.x
            if (pp.y < min.y) min.y = pp.y
            if (pp.z < min.z) min.z = pp.z
            if (pp.x > max.x) max.x = pp.x
            if (pp.y > max.y) max.y = pp.y
            if (pp.z > max.z) max.z = pp.z
        }
        updateFieldWindow(min.x, min.z, max.x, max.z, true)
    }

    private fun updateFieldWindow(sourceX: Int, sourceZ: Int, targetX: Int, targetZ: Int, cull: Boolean) {
        var x0: Int
        var xN: Int
        var z0: Int
        var zN: Int
        if (sourceX > targetX) {
            x0 = targetX
            xN = sourceX
        } else {
            x0 = sourceX
            xN = targetX
        }
        if (sourceZ > targetZ) {
            z0 = targetZ
            zN = sourceZ
        } else {
            z0 = sourceZ
            zN = targetZ
        }
        val destinationEntity = destinationEntity
        val entityWidth = subject.width()
        val entitySize = ceil(
            if (destinationEntity != null)
                max(entityWidth, destinationEntity.width()).toDouble()
            else
                entityWidth.toDouble()
        ).toInt()
        val searchAreaRange = subject.searchRange()
        x0 -= (searchAreaRange + entitySize).toInt()
        z0 -= (searchAreaRange + entitySize).toInt()
        xN += (searchAreaRange + entitySize).toInt()
        zN += (searchAreaRange + entitySize).toInt()
        nodeMap.updateFieldWindow(x0, z0, xN, zN, cull)
    }

    private fun edgeAtTarget(x: Double, y: Double, z: Double): Node? {
        val nx = floor(x).toInt()
        val ny = floor(y).toInt()
        val nz = floor(z).toInt()
        val node: Node?
        if (bestEffort) {
            node = nodeMap.cachedPointAt(nx, ny, nz)
            if (node.passibility() == Passibility.impassible) node.passibility(Passibility.dangerous)
        } else {
            node = nodeMap.cachedPassiblePointNear(nx, ny, nz)
            if (impassible(node)) return null
            val dl = Vec3d(node.coordinates)
            dl.sub(destinationPosition)
            if (dl.mg2() > 1) return null
        }
        return node
    }

    private fun edgeAtDestination(): Node? {
        val destinationPosition = destinationPosition ?: return null
        return edgeAtTarget(destinationPosition.x, destinationPosition.y, destinationPosition.z)
    }

    private fun pointAtSource(): Node? {
        val sourcePosition = sourcePosition
        val x = floor(sourcePosition!!.x).toInt()
        val y = floor(sourcePosition.y).toInt()
        val z = floor(sourcePosition.z).toInt()
        val candidate = cachedPassiblePointNear(x, y, z)
        if (impassible(candidate)) candidate!!.passibility(if (capabilities!!.cautious()) Passibility.passible else Passibility.risky)
        return candidate
    }

    private fun updateDestination(x: Double, y: Double, z: Double): Boolean {
        return if (destinationPosition != null) {
            val destinationPosition = destinationPosition
            val modified = differs(x, y, z, destinationPosition)
            destinationPosition!!.x = x
            destinationPosition.y = y
            destinationPosition.z = z
            modified
        } else {
            destinationPosition = Vec3d(x, y, z)
            true
        }
    }

    private fun updateDestination(coordinates: com.extollit.linalg.immutable.Vec3d?): Boolean {
        return if (destinationPosition != null) {
            val modified = differs(coordinates, destinationPosition)
            destinationPosition!!.set(coordinates)
            modified
        } else {
            destinationPosition = Vec3d(coordinates)
            true
        }
    }

    private fun reachedTarget(): Boolean {
        val flag = target == null || source === target || current === target
        if (flag) resetFaultTimings()
        return flag
    }

    private fun updateSourcePosition() {
        val coordinates = subject.coordinates()
        if (sourcePosition != null) {
            val sourcePosition: Vec3d? = sourcePosition
            sourcePosition!!.x = coordinates!!.x
            sourcePosition.y = coordinates.y
            sourcePosition.z = coordinates.z
        } else sourcePosition = Vec3d(coordinates!!.x, coordinates.y, coordinates.z)
        val x = floor(coordinates.x).toInt()
        val z = floor(coordinates.z).toInt()
        updateFieldWindow(x, z, x, z, false)
        val last = current
        current = pointAtSource()
        if (last != null && last !== current) trimmedToCurrent = false
    }

    /**
     * Reset this path-finder to a state suitable for initiating path-finding to some other destination.  This will also
     * purge all previously cached data and reset fault timings.  Call this to recover used memory and abort path-finding,
     * this would be most suitable if the entity is going to stop path-finding and rest for awhile.
     */
    fun reset() {
        currentPath = null
        queue.clear()
        nodeMap.reset()
        unreachableFromSource.clear()
        closest = null
        source = closest
        target = source
        current = null
        trimmedToCurrent = false
        destinationPosition = null
        sourcePosition = destinationPosition
        destinationEntity = null
        resetFaultTimings()
    }

    private fun resetFaultTimings() {
        faultCount = 0
        nextGraphResetFailureCount = FAULT_COUNT_THRESHOLD.toInt()
        passiblePointPathTimeLimit = PASSIBLE_POINT_TIME_LIMIT!!.next(random)
        nextGraphCacheReset = 0f
    }

    private fun updatePath(newPath: IPath?): IPath? {
        var mutableNewPath = newPath
        if (mutableNewPath == null) return null.also { currentPath = it }

        val currentPath = currentPath

        if (currentPath != null) {
            if (currentPath.sameAs(mutableNewPath)) mutableNewPath =
                currentPath else if (!currentPath.done() && mutableNewPath is PathObject) mutableNewPath.adjustPathPosition(
                currentPath,
                subject
            )
        }
        if (nodeMap.needsOcclusionProvider()) updateFieldWindow(mutableNewPath)
        if (mutableNewPath.done()) {
            var last = mutableNewPath.last()
            if (last == null) last = pointAtSource()
            return last?.let { lastElement -> IncompletePath(lastElement).also { this.currentPath =
                it
            } }
        }
        return mutableNewPath.also { this.currentPath = it }
    }

    private fun triage(iterations: Int): IPath? {
        var mutableIterations = iterations
        val currentPath = currentPath
        val queue = queue
        if (queue.isEmpty) if (currentPath == null) return null else if (!currentPath.done()) return currentPath else resetTriage()
        if (target == null) return null
        var nextPath: IPath? = null
        var trimmedToSource = trimmedToCurrent
        while (!queue.isEmpty && mutableIterations-- > 0) {
            val source = current
            if (!trimmedToSource && !queue.nextContains(source)) {
                if (source != null) {
                    this.queue.trimFrom(source)
                    trimmedToSource = true
                    trimmedToCurrent = trimmedToSource
                    mutableIterations++
                }
                continue
            }
            val current = queue.dequeue()
            val closest = closest
            if (closest == null || closest.orphaned()
                || Node.squareDelta(current, target!!) < Node.squareDelta(closest, target!!)
            ) this.closest = current
            if (current === target) {
                nextPath = createPath(current)
                if (PathObject.active(nextPath)) {
                    this.queue.clear()
                    break
                }
                resetTriage()
                if (target == null) {
                    nextPath = null
                    break
                }
            } else processNode(current)
        }
        val closest = closest
        if (nextPath == null && closest != null && !queue.isEmpty) nextPath = createPath(closest)
        return updatePath(nextPath)
    }

    private fun createPath(head: Node): IPath {
        val capabilities = capabilities
        val path: IPath = PathObject.fromHead(capabilities!!.speed(), random, head)
        if (pathProcessor != null) pathProcessor!!.processPath(path)
        return path
    }

    private fun processNode(current: Node?) {
        current!!.visited(true)
        val coords = current.coordinates
        val west = cachedPassiblePointNear(coords.x - 1, coords.y, coords.z, coords)
        val east = cachedPassiblePointNear(coords.x + 1, coords.y, coords.z, coords)
        val north = cachedPassiblePointNear(coords.x, coords.y, coords.z - 1, coords)
        val south = cachedPassiblePointNear(coords.x, coords.y, coords.z + 1, coords)
        val up: Node?
        val down: Node?
        val omnidirectional = pathPointCalculator!!.omnidirectional()
        if (omnidirectional) {
            up = cachedPassiblePointNear(coords.x, coords.y + 1, coords.z, coords)
            down = cachedPassiblePointNear(coords.x, coords.y - 1, coords.z, coords)
        } else {
            down = null
            up = down
        }
        val found = applyPointOptions(current, up, down, west, east, north, south)
        if (!found) {
            val southBounds = blockBounds(coords, 0, 0, +1)
            val northBounds = blockBounds(coords, 0, 0, -1)
            val eastBounds = blockBounds(coords, +1, 0, 0)
            val westBounds = blockBounds(coords, -1, 0, 0)
            val actualSizeSquared = actualSize * actualSize
            val pointOptions: Array<Node?>
            if (omnidirectional) {
                val upBounds = blockBounds(coords, 0, +1, 0)
                val downBounds = blockBounds(coords, 0, -1, 0)
                pointOptions = arrayOf(
                    if (northBounds == null || upBounds == null || northBounds.mg2(upBounds) >= actualSizeSquared) cachedPassiblePointNear(
                        coords.x, coords.y + 1, coords.z - 1, coords
                    ) else null,
                    if (eastBounds == null || upBounds == null || eastBounds.mg2(upBounds) >= actualSizeSquared) cachedPassiblePointNear(
                        coords.x + 1, coords.y + 1, coords.z, coords
                    ) else null,
                    if (southBounds == null || upBounds == null || southBounds.mg2(upBounds) >= actualSizeSquared) cachedPassiblePointNear(
                        coords.x, coords.y + 1, coords.z + 1, coords
                    ) else null,
                    if (westBounds == null || upBounds == null || westBounds.mg2(upBounds) >= actualSizeSquared) cachedPassiblePointNear(
                        coords.x - 1, coords.y + 1, coords.z, coords
                    ) else null,
                    if (northBounds == null || downBounds == null || northBounds.mg2(downBounds) >= actualSizeSquared) cachedPassiblePointNear(
                        coords.x, coords.y - 1, coords.z - 1, coords
                    ) else null,
                    if (eastBounds == null || downBounds == null || eastBounds.mg2(downBounds) >= actualSizeSquared) cachedPassiblePointNear(
                        coords.x + 1, coords.y - 1, coords.z, coords
                    ) else null,
                    if (southBounds == null || downBounds == null || southBounds.mg2(downBounds) >= actualSizeSquared) cachedPassiblePointNear(
                        coords.x, coords.y - 1, coords.z + 1, coords
                    ) else null,
                    if (westBounds == null || downBounds == null || westBounds.mg2(downBounds) >= actualSizeSquared) cachedPassiblePointNear(
                        coords.x - 1, coords.y - 1, coords.z, coords
                    ) else null
                )
                applyPointOptions(current, *pointOptions)
            } else pointOptions = arrayOfNulls(4)
            pointOptions[0] =
                if (westBounds == null || northBounds == null || westBounds.mg2(northBounds) >= actualSizeSquared) cachedPassiblePointNear(
                    coords.x - 1, coords.y, coords.z - 1, coords
                ) else null
            pointOptions[1] =
                if (eastBounds == null || southBounds == null || eastBounds.mg2(southBounds) >= actualSizeSquared) cachedPassiblePointNear(
                    coords.x + 1, coords.y, coords.z + 1, coords
                ) else null
            pointOptions[2] =
                if (eastBounds == null || northBounds == null || eastBounds.mg2(northBounds) >= actualSizeSquared) cachedPassiblePointNear(
                    coords.x + 1, coords.y, coords.z - 1, coords
                ) else null
            pointOptions[3] =
                if (westBounds == null || southBounds == null || westBounds.mg2(southBounds) >= actualSizeSquared) cachedPassiblePointNear(
                    coords.x - 1, coords.y, coords.z + 1, coords
                ) else null
            applyPointOptions(current, *pointOptions)
        }
    }

    private fun blockBounds(coords: Vec3i?, dx: Int, dy: Int, dz: Int): AxisAlignedBBox? {
        val x = coords!!.x + dx
        val y = coords.y + dy
        val z = coords.z + dz
        val bounds: com.extollit.linalg.immutable.AxisAlignedBBox
        val flags = nodeMap.flagsAt(x, y, z)
        bounds = when {
            fuzzyPassibility(flags) -> {
                val block = instanceSpace.blockObjectAt(x, y, z)
                if (!block.impeding) return null
                block.bounds()
            }
            PassibilityHelpers.impedesMovement(
                flags,
                capabilities
            ) -> FULL_BOUNDS
            else -> return null
        }
        val result = AxisAlignedBBox(bounds)
        result.add(dx.toDouble(), dy.toDouble(), dz.toDouble())
        return result
    }

    fun applyPointOptions(current: Node, vararg pointOptions: Node?): Boolean {
        var found = false
        for (alternative in pointOptions) {
            if (impassible(alternative) || alternative?.visited() == true || Node.squareDelta(
                    alternative,
                    target
                ) >= searchRangeSquared
            ) continue
            found = true
            alternative?.sterilize()
            if (alternative != null) {
                queue.appendTo(alternative, current, target!!.coordinates)
            }
        }
        return found
    }

    private fun impassible(alternative: Node?): Boolean =
        alternative == null || alternative.passibility().impassible(capabilities)

    private fun cachedPassiblePointNear(x0: Int, y0: Int, z0: Int, origin: Vec3i? = null): Node? {
        val coords0 = Vec3i(x0, y0, z0)
        val result = nodeMap.cachedPassiblePointNear(coords0, origin)
        return if (Node.passible(result) && origin != null && unreachableFromSource(
                origin,
                coords0
            )
        ) null else result
    }

    private fun fuzzyPassibility(x: Int, y: Int, z: Int): Boolean = fuzzyPassibility(nodeMap.flagsAt(x, y, z))

    private fun fuzzyPassibility(flags: Byte): Boolean {
        return PassibilityHelpers.impedesMovement(
            flags,
            capabilities
        ) && (Logic.fuzzy.flagsIn(flags) || Logic.doorway.flagsIn(flags))
    }

    fun unreachableFromSource(current: Vec3i, target: Vec3i): Boolean {
        val sourcePoint = source!!.coordinates
        return current == sourcePoint && unreachableFromSource.contains(target)
    }

    fun sameDestination(delegate: IPath, target: com.extollit.linalg.immutable.Vec3d): Boolean {
        if (currentPath == null) return false
        if (currentPath !== delegate && !currentPath!!.sameAs(delegate)) return false
        val dest = destinationPosition ?: return false
        return floor(target.x) == floor(dest.x) && floor(target.y) == floor(dest.y) && floor(
            target.z
        ) == floor(dest.z)
    }

    private fun pathTimeAge(): Float = subject.age() * capabilities!!.speed()

    fun passibilityNear(tx: Int, ty: Int, tz: Int): Pair<Passibility, Vec3i> {
        updateSourcePosition()
        val x = floor(sourcePosition!!.x).toInt()
        val z = floor(sourcePosition!!.z).toInt()
        applySubject()
        updateFieldWindow(x, z, tx, tz, false)
        val point = nodeMap.cachedPassiblePointNear(tx, ty, tz)
        return point.passibility() to point.coordinates
    }

    companion object {
        private val FULL_BOUNDS = com.extollit.linalg.immutable.AxisAlignedBBox(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
        private var DOT_THRESHOLD = 0.6
        private var PROBATIONARY_TIME_LIMIT: FloatRange? = FloatRange(36f, 64f)
        private var PASSIBLE_POINT_TIME_LIMIT: FloatRange? = FloatRange(24f, 48f)
        private var FAULT_COUNT_THRESHOLD: Byte = 3
        private var FAULT_LIMIT = 23

        /**
         * Configures the path-finding library.  All instances of this class derive configuration from here
         *
         * @param configModel source of the configuration to apply to all instances of this class
         */
        fun configureFrom(configModel: IConfigModel) {
            FAULT_COUNT_THRESHOLD = configModel.faultCountThreshold()
            FAULT_LIMIT = configModel.faultLimit()
            PROBATIONARY_TIME_LIMIT = configModel.probationaryTimeLimit()
            PASSIBLE_POINT_TIME_LIMIT = configModel.passiblePointTimeLimit()
            DOT_THRESHOLD = configModel.dotThreshold().toDouble()
            GroundNodeCalculator.configureFrom(configModel)
        }

        private fun differs(a: com.extollit.linalg.immutable.Vec3d?, b: Vec3d?): Boolean = differs(a!!.x, a.y, a.z, b)

        private fun differs(x: Double, y: Double, z: Double, other: Vec3d?): Boolean {
            return floor(other!!.x) != floor(x) || floor(other.y) != floor(y) || floor(
                other.z
            ) != floor(z)
        }
    }

    init {
        applySubject()
        schedulingPriority(SchedulingPriority.low)
        resetFaultTimings()
    }
}