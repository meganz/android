package mega.privacy.android.feature.myaccount.presentation.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.feature.myaccount.R
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class AccountTypeNameMapperTest {

    private val underTest = AccountTypeNameMapper()

    @ParameterizedTest(name = "accountType: {0} -> expectedRes: {1}")
    @MethodSource("accountTypeMappings")
    fun `test account name mapping`(accountType: AccountType?, expectedRes: Int) {
        val result = underTest.invoke(accountType)
        assertThat(result).isEqualTo(expectedRes)
    }

    @Test
    fun `test null account type returns recovering info`() {
        val result = underTest.invoke(null)
        assertThat(result).isEqualTo(R.string.recovering_info)
    }

    companion object Companion {
        @JvmStatic
        fun accountTypeMappings(): Stream<Arguments> = Stream.of(
            Arguments.of(AccountType.FREE, R.string.free_account),
            Arguments.of(AccountType.PRO_LITE, R.string.prolite_account),
            Arguments.of(AccountType.PRO_I, R.string.pro1_account),
            Arguments.of(AccountType.PRO_II, R.string.pro2_account),
            Arguments.of(AccountType.PRO_III, R.string.pro3_account),
            Arguments.of(AccountType.PRO_FLEXI, R.string.pro_flexi_account),
            Arguments.of(AccountType.BUSINESS, R.string.business_label),
            Arguments.of(
                AccountType.STARTER,
                mega.privacy.android.shared.resources.R.string.general_low_tier_plan_starter_label
            ),
            Arguments.of(
                AccountType.BASIC,
                mega.privacy.android.shared.resources.R.string.general_low_tier_plan_basic_label
            ),
            Arguments.of(
                AccountType.ESSENTIAL,
                mega.privacy.android.shared.resources.R.string.general_low_tier_plan_essential_label
            ),
        )
    }
}
