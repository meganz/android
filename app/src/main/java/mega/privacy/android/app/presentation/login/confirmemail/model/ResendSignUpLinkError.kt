package mega.privacy.android.app.presentation.login.confirmemail.model

import androidx.annotation.StringRes
import mega.privacy.android.shared.resources.R as sharedR

enum class ResendSignUpLinkError(@StringRes val messageId: Int) {
    AccountExists(messageId = sharedR.string.sign_up_account_existed_error_message),
    TooManyAttempts(messageId = sharedR.string.resend_signup_link_error),
    Unknown(messageId = sharedR.string.general_request_failed_message)
}
