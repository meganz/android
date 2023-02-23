package mega.privacy.android.data.mapper.verification

import mega.privacy.android.domain.entity.verification.OptInVerification
import mega.privacy.android.domain.entity.verification.Unblock
import mega.privacy.android.domain.exception.mapper.UnknownMapperParameterException
import javax.inject.Inject

/**
 * Sms permission mapper impl
 */
internal class SmsPermissionMapperImpl @Inject constructor() : SmsPermissionMapper {
    override fun invoke(state: Int) = when (state) {
        0 -> emptyList()
        1 -> listOf(Unblock)
        2 -> listOf(OptInVerification, Unblock)
        else -> throw UnknownMapperParameterException(
            SmsPermissionMapperImpl::class.simpleName,
            state.toString()
        )
    }
}