package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SubscriptionStatus
import nz.mega.sdk.MegaAccountDetails
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SubscriptionStatusMapperTest {
    private val underTest = SubscriptionStatusMapper()

    @ParameterizedTest(name = "test that subscription status from SDK {0} is mapped correctly Subscription Status {1}")
    @MethodSource("provideParameters")
    fun `test that subscription status is mapped correctly`(
        sdkSubscriptionStatus: Int,
        expectedSubscriptionStatus: SubscriptionStatus?,
    ) = runTest {
        assertThat(underTest(sdkSubscriptionStatus)).isEqualTo(expectedSubscriptionStatus)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(MegaAccountDetails.SUBSCRIPTION_STATUS_NONE, SubscriptionStatus.NONE),
        Arguments.of(MegaAccountDetails.SUBSCRIPTION_STATUS_VALID, SubscriptionStatus.VALID),
        Arguments.of(MegaAccountDetails.SUBSCRIPTION_STATUS_INVALID, SubscriptionStatus.INVALID),
        Arguments.of(999, null)
    )
}