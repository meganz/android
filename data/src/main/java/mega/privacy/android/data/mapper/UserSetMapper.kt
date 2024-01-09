package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.set.UserSet

/**
 * Map MegaSet's properties to [UserSet]
 */
typealias UserSetMapper = (
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards String,
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards Long?,
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Boolean,
) -> @JvmSuppressWildcards UserSet

internal fun toUserSet(
    id: Long,
    name: String,
    type: Int,
    cover: Long?,
    creationTime: Long,
    modificationTime: Long,
    isExported: Boolean,
): UserSet = object : UserSet {
    override val id: Long = id

    override val name: String = name

    override val type: Int = type

    override val cover: Long? = cover

    override val creationTime: Long = creationTime

    override val modificationTime: Long = modificationTime

    override val isExported: Boolean = isExported
}
