package mega.privacy.android.app.mediaplayer

/**
 * Pure decision logic for applying an in-place queue rebuild (hidden-nodes filter change, or a
 * queue item moved to another folder) to the current timeline.
 *
 * A rebuilt queue carries fresh media IDs, so items are matched by node handle instead. Kept
 * free of Media3 types so the branch selection and the survivor search are unit-testable.
 */
internal object AudioPlayQueueFilterDiff {

    /** How the player timeline must change to adopt the rebuilt queue. */
    internal sealed interface Action {

        /** The rebuilt queue matches the timeline item for item: leave the player alone. */
        data object NoChange : Action

        /**
         * Nothing survived and the playing item cannot be kept: playback must stop entirely.
         */
        data object StopPlayback : Action

        /**
         * The playing item survived: keep its MediaItem instance and rebuild the items around
         * it, so playback and position stay untouched.
         *
         * @property newIndexOfCurrent the playing item's index within the rebuilt queue
         */
        data class KeepCurrent(val newIndexOfCurrent: Int) : Action

        /**
         * The playing item left the queue's source (moved to another folder) but must keep
         * playing: keep its MediaItem instance, replace everything else with the rebuilt queue,
         * and slot it in ahead of its nearest surviving successor so playback continues into
         * the rebuilt queue when it ends.
         *
         * @property insertBeforeCount how many rebuilt items go ahead of the playing item
         */
        data class KeepCurrentDetached(val insertBeforeCount: Int) : Action

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
     * @param keepDetachedCurrent whether a playing item missing from [newHandles] keeps playing
     * ([Action.KeepCurrentDetached]) instead of being replaced ([Action.ResetToSurvivor]); true
     * for a folder move, false for a hidden-nodes filter change
     */
    fun resolve(
        oldTimelineHandles: List<Long?>,
        oldCurrentIndex: Int,
        newHandles: List<Long?>,
        keepDetachedCurrent: Boolean = false,
    ): Action {
        val currentHandle = oldTimelineHandles.getOrNull(oldCurrentIndex)
        if (newHandles.isEmpty()) {
            // The playing item was the folder's only (or last) track and has just been moved out
            // of it: the rebuilt queue is empty, but the item itself is still playable and must
            // keep playing on its own rather than tear the session down.
            return if (keepDetachedCurrent && currentHandle != null) {
                Action.KeepCurrentDetached(insertBeforeCount = 0)
            } else {
                Action.StopPlayback
            }
        }
        if (newHandles == oldTimelineHandles) return Action.NoChange
        val newIndexOfCurrent = if (currentHandle == null) {
            -1
        } else {
            newHandles.indexOf(currentHandle)
        }
        if (newIndexOfCurrent >= 0) return Action.KeepCurrent(newIndexOfCurrent)
        val survivorIndex = nearestSurvivorIndex(oldTimelineHandles, oldCurrentIndex, newHandles)
        return if (keepDetachedCurrent && currentHandle != null) {
            Action.KeepCurrentDetached(insertBeforeCount = survivorIndex)
        } else {
            Action.ResetToSurvivor(survivorIndex)
        }
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
