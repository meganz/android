package test.mega.privacy.android.app.upgradeAccount.model.mapper

import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import com.google.common.truth.Truth.assertThat
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