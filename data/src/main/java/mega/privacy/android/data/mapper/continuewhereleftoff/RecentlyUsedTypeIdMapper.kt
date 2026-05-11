package mega.privacy.android.data.mapper.continuewhereleftoff

import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import javax.inject.Inject

/**
 * Bidirectional mapper between [RecentlyUsedType] and its database type ID.
 */
internal class RecentlyUsedTypeIdMapper @Inject constructor() {

    /**
     * Converts a [RecentlyUsedType] to its database type ID.
     *
     * @param type
     */
    operator fun invoke(type: RecentlyUsedType): Int = when (type) {
        RecentlyUsedType.PDF -> 1
        RecentlyUsedType.Video -> 2
        RecentlyUsedType.Audio -> 3
        RecentlyUsedType.TextEditor -> 4
    }

    /**
     * Converts a database type ID to a [RecentlyUsedType].
     *
     * @param typeId
     */
    operator fun invoke(typeId: Int): RecentlyUsedType = when (typeId) {
        1 -> RecentlyUsedType.PDF
        2 -> RecentlyUsedType.Video
        3 -> RecentlyUsedType.Audio
        4 -> RecentlyUsedType.TextEditor
        else -> error("Unknown type ID: $typeId")
    }
}
