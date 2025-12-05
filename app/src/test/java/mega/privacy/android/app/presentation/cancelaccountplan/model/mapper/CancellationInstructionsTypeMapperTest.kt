package mega.privacy.android.app.presentation.cancelaccountplan.model.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.cancelaccountplan.model.CancellationInstructionsType
import mega.privacy.android.app.presentation.cancelaccountplan.model.mapper.CancellationInstructionsTypeMapper
import mega.privacy.android.domain.entity.PaymentMethod
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CancellationInstructionsTypeMapperTest {
    private val underTest = CancellationInstructionsTypeMapper()

    private fun provideParameters() = Stream.of(
        Arguments.of(PaymentMethod.ITUNES, CancellationInstructionsType.AppStore),
        Arguments.of(PaymentMethod.GOOGLE_WALLET, CancellationInstructionsType.PlayStore),
        Arguments.of(PaymentMethod.STRIPE, CancellationInstructionsType.WebClient),
        Arguments.of(PaymentMethod.STRIPE2, CancellationInstructionsType.WebClient),
        Arguments.of(PaymentMethod.ECP, CancellationInstructionsType.WebClient),
        Arguments.of(PaymentMethod.HUAWEI_WALLET, null)
    )

    @ParameterizedTest(name = "when current payment method is {0} then cancellation instructions type is {1}")
    @MethodSource("provideParameters")
    fun `test that mapper returns correct cancellation instructions type`(
        paymentMethod: PaymentMethod,
        cancellationInstructionsType: CancellationInstructionsType?,
    ) {
        assertThat(underTest(paymentMethod)).isEqualTo(cancellationInstructionsType)
    }
}