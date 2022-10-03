package mega.privacy.android.app.data.mapper

import mega.privacy.android.domain.entity.user.UserLastGreen

/**
 * Mapper to convert data to [UserLastGreen]
 */
typealias UserLastGreenMapper = (
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Int,
) -> UserLastGreen

internal fun toUserUserLastGreen(handle: Long, lastGreen: Int): UserLastGreen =
    UserLastGreen(handle, lastGreen)