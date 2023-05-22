package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

class SaveEphemeralCredentialsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(ephemeral: EphemeralCredentials) =
        accountRepository.saveEphemeral(ephemeral)
}