package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.PaymentMethodType
import mega.privacy.android.domain.entity.PaymentPlatformType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentPlatformTypeMapperTest {

    private lateinit var underTest: PaymentPlatformTypeMapper

    @BeforeAll
    fun setup() {
        underTest = PaymentPlatformTypeMapper()
    }

    @ParameterizedTest(name = "when PaymentMethodType is {0}, PaymentPlatformType is {1}")
    @MethodSource("providePaymentMethodPlatformTypeParameters")
    fun `test that subscription platform type can be mapped correctly`(
        paymentMethodType: PaymentMethodType,
        paymentPlatformType: PaymentPlatformType,
    ) = runTest {
        assertThat(underTest.invoke(paymentMethodType)).isEqualTo(paymentPlatformType)
    }

    private fun providePaymentMethodPlatformTypeParameters() = Stream.of(
        Arguments.of(PaymentMethodType.ITUNES, PaymentPlatformType.SUBSCRIPTION_FROM_ITUNES),
        Arguments.of(
            PaymentMethodType.GOOGLE_WALLET,
            PaymentPlatformType.SUBSCRIPTION_FROM_GOOGLE_PLATFORM
        ),
        Arguments.of(
            PaymentMethodType.HUAWEI_WALLET,
            PaymentPlatformType.SUBSCRIPTION_FROM_HUAWEI_PLATFORM
        ),
        Arguments.of(
            PaymentMethodType.STRIPE,
            PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM
        ),
        Arguments.of(
            PaymentMethodType.STRIPE2,
            PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM
        ),
        Arguments.of(PaymentMethodType.ECP, PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM),
        Arguments.of(
            PaymentMethodType.WIRE_TRANSFER,
            PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM
        )
    )
}