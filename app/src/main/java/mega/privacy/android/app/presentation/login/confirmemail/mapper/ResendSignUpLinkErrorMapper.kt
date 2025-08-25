package mega.privacy.android.app.presentation.login.confirmemail.mapper

import mega.privacy.android.app.presentation.login.confirmemail.model.ResendSignUpLinkError
import mega.privacy.android.domain.exception.account.CreateAccountException
import javax.inject.Inject

class ResendSignUpLinkErrorMapper @Inject constructor() {

    operator fun invoke(exception: Throwable) = when (exception) {
        is CreateAccountException.AccountAlreadyExists -> ResendSignUpLinkError.AccountExists

        is CreateAccountException.TooManyAttemptsException -> ResendSignUpLinkError.TooManyAttempts

        else -> ResendSignUpLinkError.Unknown
    }
}
