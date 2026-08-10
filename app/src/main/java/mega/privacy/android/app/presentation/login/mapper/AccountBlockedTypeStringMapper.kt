package mega.privacy.android.app.presentation.login.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Maps [AccountBlockedEvent] to a display string using context resources for known blocked types,
 * or the event's [AccountBlockedEvent.text] for other types.
 */
class AccountBlockedTypeStringMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Returns the localized string for the event's type, or the event's text for unmapped types.
     *
     * @param event [AccountBlockedEvent] to map
     * @return Display string from context for TOS_COPYRIGHT, TOS_NON_COPYRIGHT, SUBUSER_DISABLED,
     * VERIFICATION_EMAIL; otherwise [AccountBlockedEvent.text]
     */
    operator fun invoke(event: AccountBlockedEvent): String = when (event.type) {
        AccountBlockedType.TOS_COPYRIGHT ->
            context.getString(sharedR.string.dialog_account_suspended_ToS_copyright_message)

        AccountBlockedType.TOS_NON_COPYRIGHT ->
            context.getString(sharedR.string.dialog_account_suspended_ToS_non_copyright_message)

        AccountBlockedType.SUBUSER_DISABLED ->
            context.getString(sharedR.string.error_business_disabled)

        AccountBlockedType.VERIFICATION_EMAIL ->
            context.getString(sharedR.string.login_account_suspension_email_verification_message)

        else -> event.text
    }
}
