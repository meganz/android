package mega.privacy.android.data.mapper.account

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import mega.privacy.android.domain.entity.account.AccountBlockedType
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
        Arguments.of(0L, AccountBlockedType.NOT_BLOCKED),
        Arguments.of(100L, AccountBlockedType.NOT_BLOCKED),
        Arguments.of(200L, AccountBlockedType.TOS_COPYRIGHT),
        Arguments.of(300L, AccountBlockedType.TOS_NON_COPYRIGHT),
        Arguments.of(400L, AccountBlockedType.SUBUSER_DISABLED),
        Arguments.of(401L, AccountBlockedType.SUBUSER_REMOVED),
        Arguments.of(500L, AccountBlockedType.VERIFICATION_SMS),
        Arguments.of(700L, AccountBlockedType.VERIFICATION_EMAIL),
    )
}