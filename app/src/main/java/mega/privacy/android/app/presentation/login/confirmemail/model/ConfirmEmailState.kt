package mega.privacy.android.app.presentation.login.confirmemail.model

import mega.privacy.android.app.presentation.login.model.LoginFragmentType

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.login.LoginFragment].
 *
 * @property isPendingToShowFragment [LoginFragmentType] if pending, null otherwise.
 */
data class ConfirmEmailState(
    val isPendingToShowFragment: LoginFragmentType? = null,
)
