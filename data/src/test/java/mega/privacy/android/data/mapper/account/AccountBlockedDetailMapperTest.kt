package mega.privacy.android.data.mapper.account

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import mega.privacy.android.domain.entity.account.AccountBlockedType
import nz.mega.sdk.MegaApiJava
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountBlockedDetailMapperTest {

    private lateinit var underTest: AccountBlockedDetailMapper

    private val text = "text"

    @BeforeAll
    fun setup() {
        underTest = AccountBlockedDetailMapper()
    }

    @ParameterizedTest(name = "when type number is {0}")
    @MethodSource("provideParameters")
    fun `test that account blocked detail returns correctly`(
        type: Long,
        expectedType: AccountBlockedType,
    ) {
        Truth.assertThat(underTest.invoke(type, text))
            .isEqualTo(AccountBlockedDetail(expectedType, text))
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(
            MegaApiJava.ACCOUNT_NOT_BLOCKED.toLong(),
            AccountBlockedType.NOT_BLOCKED
        ),
        Arguments.of(
            100L,
            AccountBlockedType.NOT_BLOCKED
        ),
        Arguments.of(
            MegaApiJava.ACCOUNT_BLOCKED_TOS_COPYRIGHT.toLong(),
            AccountBlockedType.TOS_COPYRIGHT
        ),
        Arguments.of(
            MegaApiJava.ACCOUNT_BLOCKED_TOS_NON_COPYRIGHT.toLong(),
            AccountBlockedType.TOS_NON_COPYRIGHT
        ),
        Arguments.of(
            MegaApiJava.ACCOUNT_BLOCKED_SUBUSER_DISABLED.toLong(),
            AccountBlockedType.SUBUSER_DISABLED
        ),
        Arguments.of(
            MegaApiJava.ACCOUNT_BLOCKED_SUBUSER_REMOVED.toLong(),
            AccountBlockedType.SUBUSER_REMOVED
        ),
        Arguments.of(
            MegaApiJava.ACCOUNT_BLOCKED_VERIFICATION_SMS.toLong(),
            AccountBlockedType.VERIFICATION_SMS
        ),
        Arguments.of(
            MegaApiJava.ACCOUNT_BLOCKED_VERIFICATION_EMAIL.toLong(),
            AccountBlockedType.VERIFICATION_EMAIL
        ),
    )
}