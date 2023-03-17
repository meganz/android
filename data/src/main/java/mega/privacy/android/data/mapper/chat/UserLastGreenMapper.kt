package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.user.UserLastGreen

/**
 * Mapper to convert data into [UserLastGreen].
 */
internal fun interface UserLastGreenMapper {

    /**
     * Invoke.
     *
     * @param handle User handle.
     * @param lastGreen User last green.
     * @return [UserLastGreen]
     */
    operator fun invoke(handle: Long, lastGreen: Int): UserLastGreen
}