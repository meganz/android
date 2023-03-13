package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.domain.exception.LoginException
import mega.privacy.android.domain.exception.LoginRequireValidation
import mega.privacy.android.domain.exception.LoginTooManyAttempts
import mega.privacy.android.domain.exception.LoginUnknownStatus
import mega.privacy.android.domain.exception.LoginWrongEmailOrPassword

internal val LoginException.error: Int
    get() = when (this) {
        is LoginRequireValidation -> R.string.account_not_validated_login
        is LoginTooManyAttempts -> R.string.too_many_attempts_login
        is LoginWrongEmailOrPassword -> R.string.error_incorrect_email_or_password
        is LoginUnknownStatus -> megaException.getErrorStringId()
        else -> R.string.general_error
    }