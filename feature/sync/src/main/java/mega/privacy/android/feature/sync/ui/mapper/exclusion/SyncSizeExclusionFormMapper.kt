package mega.privacy.android.feature.sync.ui.mapper.exclusion

import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionBySizeFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import javax.inject.Inject

/**
 * Reads the bounds a by-size form has authored, the way back from
 * [SyncSizeExclusionPrefillMapper].
 *
 * Only the bounds the chosen condition draws are read, so a bound left over from a condition the
 * user moved away from cannot reach the file.
 */
internal class SyncSizeExclusionFormMapper @Inject constructor() {

    /**
     * The bounds [form] declares, or null while it declares none the file could hold: no condition
     * picked yet, a bound the condition reads that is empty, zero or too large to express, or a
     * range whose first bound is not below its second.
     *
     * The range is judged by [AddExclusionBySizeFormState.showsRangeError], so the form that draws
     * the error and the gate that refuses the save read the same predicate.
     */
    operator fun invoke(form: AddExclusionBySizeFormState): SyncSizeExclusion? {
        val condition = form.condition ?: return null
        if (form.activeBounds.any { it.bytes == null }) return null
        if (form.showsRangeError) return null
        return when (condition) {
            SyncSizeCondition.SmallerThan ->
                SyncSizeExclusion(excludeSmallerThanBytes = form.firstBound.bytes)

            SyncSizeCondition.BiggerThan ->
                SyncSizeExclusion(excludeLargerThanBytes = form.firstBound.bytes)

            SyncSizeCondition.NotBetween -> SyncSizeExclusion(
                excludeSmallerThanBytes = form.firstBound.bytes,
                excludeLargerThanBytes = form.secondBound.bytes,
            )
        }
    }
}
