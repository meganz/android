package mega.privacy.android.app.presentation.extensions.login

import mega.privacy.android.shared.resources.R as shareR
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.model.LoginError

internal val LoginError.error: Int
    get() = when (this) {
        LoginError.EmptyEmail -> R.string.error_enter_email
        LoginError.NotValidEmail -> R.string.error_invalid_email
        LoginError.EmptyPassword -> R.string.error_enter_password
    }

internal val LoginError.newError: Int
    get() = when (this) {
        LoginError.EmptyEmail,
        LoginError.NotValidEmail,
            -> shareR.string.login_invalid_email_error_message

        LoginError.EmptyPassword -> shareR.string.login_invalid_password_error_message
    }