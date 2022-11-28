package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.AccountType
import nz.mega.sdk.MegaAccountDetails
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountTypeMapperTest {
    @Test
    fun `test that account type can be mapped correctly`() {
        val unknownAccountType = 999
        val expectedResults = HashMap<Int, AccountType?>().apply {
            put(MegaAccountDetails.ACCOUNT_TYPE_FREE, AccountType.FREE)
            put(MegaAccountDetails.ACCOUNT_TYPE_PROI, AccountType.PRO_I)
            put(MegaAccountDetails.ACCOUNT_TYPE_PROII, AccountType.PRO_II)
            put(MegaAccountDetails.ACCOUNT_TYPE_PROIII, AccountType.PRO_III)
            put(MegaAccountDetails.ACCOUNT_TYPE_LITE, AccountType.PRO_LITE)
            put(MegaAccountDetails.ACCOUNT_TYPE_PRO_FLEXI, AccountType.PRO_FLEXI)
            put(MegaAccountDetails.ACCOUNT_TYPE_BUSINESS, AccountType.BUSINESS)
            put(unknownAccountType, AccountType.UNKNOWN)
        }

        expectedResults.forEach { (key, value) ->
            val actual = toAccountType(key)
            assertEquals(value, actual)
        }
    }
}