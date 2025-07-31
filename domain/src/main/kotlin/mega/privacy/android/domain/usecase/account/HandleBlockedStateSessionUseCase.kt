package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.usecase.login.DisableChatApiUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutUseCase
import javax.inject.Inject

/**
 * Handle blocked state session use case
 *
 * @property localLogoutUseCase
 * @property disableChatApiUseCase
 */
class HandleBlockedStateSessionUseCase @Inject constructor(
    private val localLogoutUseCase: LocalLogoutUseCase,
    private val disableChatApiUseCase: DisableChatApiUseCase,
) {
    suspend operator fun invoke(event: AccountBlockedEvent) {
        when (event.type) {
            AccountBlockedType.NOT_BLOCKED,
            AccountBlockedType.VERIFICATION_SMS,
            AccountBlockedType.VERIFICATION_EMAIL,
                -> return

            AccountBlockedType.TOS_COPYRIGHT,
            AccountBlockedType.TOS_NON_COPYRIGHT,
            AccountBlockedType.SUBUSER_DISABLED,
            AccountBlockedType.SUBUSER_REMOVED,
                -> {
                localLogoutUseCase(disableChatApiUseCase)
            }
        }
    }
}