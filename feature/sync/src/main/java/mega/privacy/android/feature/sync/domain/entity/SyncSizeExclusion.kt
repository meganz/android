package mega.privacy.android.feature.sync.domain.entity

import kotlinx.serialization.Serializable

/**
 * The size bounds of a sync, as expressed by the `exclude-smaller` and
 * `exclude-larger` lines of a `.megaignore` file.
 *
 * Both bounds are independent and optional; a null bound means the file declares
 * no limit in that direction.
 *
 * Two SDK rules are worth knowing before validating a pair of bounds: equal bounds
 * are rejected, while a lower bound above the upper bound is legal and excludes
 * everything between the two. The rejection is applied line by line as the file is
 * read, not to the bounds this type ends up reporting, so a file can be refused
 * over an intermediate pair that the last of each bound would have made valid.
 *
 * @property excludeSmallerThanBytes files smaller than this are excluded
 * @property excludeLargerThanBytes files larger than this are excluded
 */
@Serializable
data class SyncSizeExclusion(
    val excludeSmallerThanBytes: Long? = null,
    val excludeLargerThanBytes: Long? = null,
) {

    /**
     * True when the file declares neither bound.
     */
    val isUnbounded: Boolean
        get() = excludeSmallerThanBytes == null && excludeLargerThanBytes == null
}
