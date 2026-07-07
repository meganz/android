package mega.privacy.android.feature.payment.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.feature.payment.model.LocalisedProductPrice
import mega.privacy.android.feature.payment.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import org.junit.Test
import java.util.Locale

class LocalisedPriceCurrencyCodeStringMapperTest {
    private val underTest = LocalisedPriceCurrencyCodeStringMapper()

    @Test
    fun `test that mapper returns correctly pair of formatted price and currency code strings`() {
        val expectedResult = LocalisedProductPrice("€4.99", "EUR")
        val currencyAmount = CurrencyAmount(4.99.toFloat(), Currency("EUR"))
        assertThat(underTest(currencyAmount, Locale.US)).isEqualTo(expectedResult)
    }

    @Test
    fun `test that whole amounts drop the trailing zero fraction digits`() {
        val expectedResult = LocalisedProductPrice("€120", "EUR")
        val currencyAmount = CurrencyAmount(120.00.toFloat(), Currency("EUR"))
        assertThat(underTest(currencyAmount, Locale.US)).isEqualTo(expectedResult)
    }

    @Test
    fun `test that amounts with cents keep the fraction digits`() {
        val expectedResult = LocalisedProductPrice("€872,744.75", "EUR")
        val currencyAmount = CurrencyAmount(872_744.75.toFloat(), Currency("EUR"))
        assertThat(underTest(currencyAmount, Locale.US)).isEqualTo(expectedResult)
    }

    @Test
    fun `test that a single trailing zero decimal is dropped`() {
        val expectedResult = LocalisedProductPrice("€4.9", "EUR")
        val currencyAmount = CurrencyAmount(4.90.toFloat(), Currency("EUR"))
        assertThat(underTest(currencyAmount, Locale.US)).isEqualTo(expectedResult)
    }
}