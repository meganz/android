package mega.privacy.android.data.mapper.account

import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import javax.inject.Inject

/**
 * Mapper for converting data into [AccountBlockedDetail].
 */
internal class AccountBlockedDetailMapper @Inject constructor(private val accountBlockedTypeMapper: AccountBlockedTypeMapper) {

    /**
     * Invoke.
     *
     * @param type Blocked account type.
     * @param text Message.
     */
    operator fun invoke(type: Long, text: String) = AccountBlockedDetail(
        type = accountBlockedTypeMapper(type),
        text = text
    )

}

