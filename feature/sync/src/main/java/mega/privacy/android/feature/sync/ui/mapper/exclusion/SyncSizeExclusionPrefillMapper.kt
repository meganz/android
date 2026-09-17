package mega.privacy.android.feature.sync.ui.mapper.exclusion

import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionBySizeFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeBoundInput
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit
import javax.inject.Inject

/**
 * Fills the Edit exclusion by-size form from the bounds a `.megaignore` declares.
 *
 * The bounds are a property of the file rather than of any one rule: the SDK applies the last
 * `exclude-smaller` and the last `exclude-larger` line, so a file has at most one bound in each
 * direction and neither carries a position the form could edit in place.
 */
internal class SyncSizeExclusionPrefillMapper @Inject constructor() {

    /**
     * The form [exclusion] opens with, or an empty form when the file declares no bound.
     *
     * A file may hold a pair the form calls an error — bounds that between them leave out every
     * file. That is reported rather than repaired: it is the same pair the by-size form already
     * refuses to author, and silently rewriting it would change what the sync excludes.
     */
    operator fun invoke(exclusion: SyncSizeExclusion): AddExclusionBySizeFormState {
        val smaller = exclusion.excludeSmallerThanBytes
        val larger = exclusion.excludeLargerThanBytes
        return when {
            smaller != null && larger != null -> AddExclusionBySizeFormState(
                condition = SyncSizeCondition.NotBetween,
                firstBound = boundInput(smaller),
                secondBound = boundInput(larger),
            )

            larger != null -> AddExclusionBySizeFormState(
                condition = SyncSizeCondition.BiggerThan,
                firstBound = boundInput(larger),
            )

            smaller != null -> AddExclusionBySizeFormState(
                condition = SyncSizeCondition.SmallerThan,
                firstBound = boundInput(smaller),
            )

            else -> AddExclusionBySizeFormState()
        }
    }

    /**
     * [bytes] read back in the largest unit that divides it exactly, so a bound the user typed as
     * 1 GB does not reopen as 1024 MB. Dividing rather than multiplying also keeps the conversion
     * exact: no bound that fits a `Long` can fail to be expressed.
     */
    private fun boundInput(bytes: Long): SyncSizeBoundInput {
        val unit = SyncSizeUnit.entries
            .lastOrNull { bytes >= it.multiplierBytes && bytes % it.multiplierBytes == 0L }
            ?: SyncSizeUnit.Bytes
        return SyncSizeBoundInput(
            value = (bytes / unit.multiplierBytes).toString(),
            unit = unit,
        )
    }
}
