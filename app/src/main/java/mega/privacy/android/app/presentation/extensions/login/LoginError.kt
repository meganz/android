package mega.privacy.android.app.presentation.extensions.login

import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.model.LoginError

internal val LoginError.error: Int
    get() = when (this) {
        LoginError.EmptyEmail -> R.string.error_enter_email
        LoginError.NotValidEmail -> R.string.error_invalid_email
        LoginError.EmptyPassword -> R.string.error_enter_password
    }