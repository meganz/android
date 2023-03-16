package mega.privacy.android.data.mapper.login

import mega.privacy.android.domain.entity.account.AccountSession
import javax.inject.Inject

/**
 * Default implementation of [AccountSessionMapper].
 */
internal class AccountSessionMapperImpl @Inject constructor() : AccountSessionMapper {

    override fun invoke(email: String?, session: String?, myHandle: Long?) =
        AccountSession(email = email, session = session, myHandle = myHandle ?: -1)
}