package mega.privacy.android.feature.payment.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.feature.payment.model.mapper.LocalisedPriceStringMapper
import org.junit.Test
import java.util.Locale

class LocalisedPriceStringMapperTest {
    private val underTest = LocalisedPriceStringMapper()

    @Test
    fun `test that mapper returns correctly formatted price string`() {
        val expectedResult = "€4.99"
        val currencyAmount = CurrencyAmount(4.99.toFloat(), Currency("EUR"))
        assertThat(underTest(currencyAmount, Locale.US)).isEqualTo(expectedResult)
    }
}