package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.account.CurrencyPoint
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DefaultCalculateCurrencyAmountTest {
    private lateinit var underTest: CalculateCurrencyAmount

    private val systemCurrencyPoint = CurrencyPoint.SystemCurrencyPoint(999.toLong())
    private val localCurrencyPoint = CurrencyPoint.LocalCurrencyPoint(9990000.toLong())
    private val currency = Currency("EUR")
    private val currencyAmountResult = CurrencyAmount(9.99.toFloat(), currency)


    @Before
    fun setUp() {
        underTest = DefaultCalculateCurrencyAmount()
    }

    @Test
    fun `test that CalculateCurrencyAmount return Currency Amount  correctly from SystemCurrencyPoint`() {
        assertEquals(currencyAmountResult, underTest.invoke(systemCurrencyPoint, currency))
    }

    @Test
    fun `test that CalculateCurrencyAmount return Currency Amount  correctly from LocalCurrencyPoint`() {
        assertEquals(currencyAmountResult, underTest.invoke(localCurrencyPoint, currency))
    }
}