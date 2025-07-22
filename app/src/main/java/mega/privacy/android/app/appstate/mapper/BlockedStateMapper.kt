package mega.privacy.android.app.appstate.mapper

import mega.privacy.android.app.appstate.model.BlockedState
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.account.AccountBlockedType
import javax.inject.Inject

/**
 * Blocked state mapper
 *
 */
class BlockedStateMapper @Inject constructor() {
    operator fun invoke(
        event: AccountBlockedEvent,
        session: String?,
    ): BlockedState = when (event.type) {
        AccountBlockedType.NOT_BLOCKED -> BlockedState.NotBlocked(
            session = session,
        )

        AccountBlockedType.TOS_COPYRIGHT -> BlockedState.Copyright(
            text = event.text,
        )

        AccountBlockedType.TOS_NON_COPYRIGHT -> BlockedState.TermsOfService(
            text = event.text,
        )

        AccountBlockedType.SUBUSER_DISABLED -> BlockedState.BusinessAccountDisabled(
            event.text,
        )

        AccountBlockedType.SUBUSER_REMOVED -> BlockedState.BusinessAccountRemoved(
            event.text,
        )

        AccountBlockedType.VERIFICATION_SMS -> BlockedState.SmsVerificationRequired(
            session = session,
            text = event.text,
        )

        AccountBlockedType.VERIFICATION_EMAIL -> BlockedState.EmailVerificationRequired(
            session = session,
            text = event.text,
        )
    }
}