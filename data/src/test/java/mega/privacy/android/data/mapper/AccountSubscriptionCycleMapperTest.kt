package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountSubscriptionCycleMapperTest {
    private val underTest: AccountSubscriptionCycleMapper = AccountSubscriptionCycleMapper()

    @ParameterizedTest(name = "test that string {0} is mapped correctly to AccountSubscriptionCycle.{1}")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(
        subscriptionCycleString: String,
        expected: AccountSubscriptionCycle,
    ) {
        val actual = underTest(subscriptionCycleString)
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of("1 M", AccountSubscriptionCycle.MONTHLY),
        Arguments.of("1 Y", AccountSubscriptionCycle.YEARLY),
        Arguments.of("", AccountSubscriptionCycle.UNKNOWN)
    )
}