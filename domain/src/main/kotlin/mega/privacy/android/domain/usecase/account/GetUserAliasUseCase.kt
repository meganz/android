package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for getting an user alias.
 */
class GetUserAliasUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    suspend operator fun invoke(userHandle: Long) =
        accountRepository.getUserAliasFromCache(userHandle)
            ?: accountRepository.getUserAlias(userHandle)
}