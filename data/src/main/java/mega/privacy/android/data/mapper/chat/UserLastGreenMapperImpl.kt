package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.user.UserLastGreen
import javax.inject.Inject

/**
 * Implementation of [UserLastGreenMapper].
 */
internal class UserLastGreenMapperImpl @Inject constructor() : UserLastGreenMapper {

    override fun invoke(handle: Long, lastGreen: Int) = UserLastGreen(handle, lastGreen)
}