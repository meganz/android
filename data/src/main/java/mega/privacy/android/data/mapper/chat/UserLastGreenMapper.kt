package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.user.UserLastGreen
import javax.inject.Inject

/**
 * Mapper to convert data into [UserLastGreen].
 */
internal class UserLastGreenMapper @Inject constructor() {

    /**
     * Invoke.
     *
     * @param handle User handle.
     * @param lastGreen User last green.
     * @return [UserLastGreen]
     */
    operator fun invoke(handle: Long, lastGreen: Int) = UserLastGreen(handle, lastGreen)
}