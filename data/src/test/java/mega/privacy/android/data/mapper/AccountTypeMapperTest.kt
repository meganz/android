package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.AccountType
import nz.mega.sdk.MegaAccountDetails
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountTypeMapperTest {
    private val underTest = AccountTypeMapper()

    @ParameterizedTest(name = "test that mega account type from SDK {0} is mapped correctly to Account type {1}")
    @MethodSource("provideParameters")
    fun `test account type mapped correctly`(type: Int, expected: AccountType) {
        val actual = underTest(type)
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters() = listOf(
        arrayOf(MegaAccountDetails.ACCOUNT_TYPE_FREE, AccountType.FREE),
        arrayOf(MegaAccountDetails.ACCOUNT_TYPE_PROI, AccountType.PRO_I),
        arrayOf(MegaAccountDetails.ACCOUNT_TYPE_PROII, AccountType.PRO_II),
        arrayOf(MegaAccountDetails.ACCOUNT_TYPE_PROIII, AccountType.PRO_III),
        arrayOf(MegaAccountDetails.ACCOUNT_TYPE_LITE, AccountType.PRO_LITE),
        arrayOf(MegaAccountDetails.ACCOUNT_TYPE_PRO_FLEXI, AccountType.PRO_FLEXI),
        arrayOf(MegaAccountDetails.ACCOUNT_TYPE_BUSINESS, AccountType.BUSINESS),
        arrayOf(MegaAccountDetails.ACCOUNT_TYPE_STARTER, AccountType.STARTER),
        arrayOf(MegaAccountDetails.ACCOUNT_TYPE_BASIC, AccountType.BASIC),
        arrayOf(MegaAccountDetails.ACCOUNT_TYPE_ESSENTIAL, AccountType.ESSENTIAL),
        arrayOf(MegaAccountDetails.ACCOUNT_TYPE_FEATURE, AccountType.FREE),
        arrayOf(999, AccountType.UNKNOWN)
    )
}