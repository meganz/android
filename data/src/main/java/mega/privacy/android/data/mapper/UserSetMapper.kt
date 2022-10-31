package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.set.UserSet

/**
 * Map MegaSet's properties to [UserSet]
 */
typealias UserSetMapper = (
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards String,
    @JvmSuppressWildcards Long?,
) -> UserSet

internal fun toUserSet(
    id: Long,
    name: String,
    cover: Long?,
): UserSet = object : UserSet {
    override val id: Long = id

    override val name: String = name

    override val cover: Long? = cover
}
