package test.mega.privacy.android.app.upgradeAccount.model.mapper

import mega.privacy.android.app.upgradeAccount.model.mapper.toFormattedPriceString
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class FormattedPriceStringMapperTest {
    @Test
    fun `test that mapper returns correctly formatted price string`() {
        val expectedResult = "€4.99"
        val currencyAmount = CurrencyAmount(4.99.toFloat(), Currency("EUR"))

        assertEquals(expectedResult, toFormattedPriceString(currencyAmount, Locale.US))
    }
}