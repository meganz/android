package mega.privacy.android.app.mediaplayer

/**
 * Pure decision logic for applying a filter-change queue rebuild to the current timeline.
 *
 * A rebuilt queue carries fresh media IDs, so items are matched by node handle instead. Kept
 * free of Media3 types so the branch selection and the survivor search are unit-testable.
 */
internal object AudioPlayQueueFilterDiff {

    /** How the player timeline must change to adopt the rebuilt queue. */
    internal sealed interface Action {

        /** Nothing survived the filter: playback must stop entirely. */
        data object StopPlayback : Action

        /**
         * The playing item survived: keep its MediaItem instance and rebuild the items around
         * it, so playback and position stay untouched.
         *
         * @property newIndexOfCurrent the playing item's index within the rebuilt queue
         */
        data class KeepCurrent(val newIndexOfCurrent: Int) : Action

        /**
         * The playing item was filtered out: reset the queue onto the nearest surviving
         * successor.
         *
         * @property startIndex index in the rebuilt queue to start from
         */
        data class ResetToSurvivor(val startIndex: Int) : Action
    }

    /**
     * Decides how the rebuilt queue applies to the current timeline.
     *
     * @param oldTimelineHandles handles of the current timeline in order (null where unknown)
     * @param oldCurrentIndex index of the playing item within [oldTimelineHandles]
     * @param newHandles handles of the rebuilt queue in order (null where unknown)
     */
    fun resolve(
        oldTimelineHandles: List<Long?>,
        oldCurrentIndex: Int,
        newHandles: List<Long?>,
    ): Action {
        if (newHandles.isEmpty()) return Action.StopPlayback
        val currentHandle = oldTimelineHandles.getOrNull(oldCurrentIndex)
        val newIndexOfCurrent = if (currentHandle == null) {
            -1
        } else {
            newHandles.indexOf(currentHandle)
        }
        if (newIndexOfCurrent >= 0) return Action.KeepCurrent(newIndexOfCurrent)
        return Action.ResetToSurvivor(
            nearestSurvivorIndex(oldTimelineHandles, oldCurrentIndex, newHandles)
        )
    }

    /**
     * Index in [newHandles] of the first item after [oldCurrentIndex] in the old timeline that
     * survived the filter, falling back to the queue start when none did.
     */
    private fun nearestSurvivorIndex(
        oldTimelineHandles: List<Long?>,
        oldCurrentIndex: Int,
        newHandles: List<Long?>,
    ): Int {
        val handleToNewIndex = buildMap {
            newHandles.forEachIndexed { index, handle ->
                if (handle != null) putIfAbsent(handle, index)
            }
        }
        for (i in oldCurrentIndex + 1 until oldTimelineHandles.size) {
            val handle = oldTimelineHandles[i] ?: continue
            handleToNewIndex[handle]?.let { return it }
        }
        return 0
    }
}
