package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Checks if 2FA alert dialog should be shown to the user
 */
class DefaultCheck2FADialog @Inject constructor(
    private val accountRepository: AccountRepository,
) : Check2FADialog {

    /**
     * Use-case returns true only for the second launch after sign up
     */
    override suspend fun invoke(newAccount: Boolean, firstLogin: Boolean) = when {
        newAccount -> {
            accountRepository.update2FADialogPreference(true)
            false
        }
        accountRepository.get2FADialogPreference() && !firstLogin -> {
            accountRepository.update2FADialogPreference(false)
            !accountRepository.is2FAEnabled()
        }
        else -> false
    }

}
