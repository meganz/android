package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.PaymentMethodType
import nz.mega.sdk.MegaApiJava
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentMethodTypeMapperTest {
    @Test
    fun `test that payment method type can be mapped correctly`() {
        val unknownPaymentMethodType = 21
        val expectedResults = HashMap<Int, PaymentMethodType?>().apply {
            put(MegaApiJava.PAYMENT_METHOD_BALANCE, PaymentMethodType.BALANCE)
            put(MegaApiJava.PAYMENT_METHOD_PAYPAL, PaymentMethodType.PAYPAL)
            put(MegaApiJava.PAYMENT_METHOD_ITUNES, PaymentMethodType.ITUNES)
            put(MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET, PaymentMethodType.GOOGLE_WALLET)
            put(MegaApiJava.PAYMENT_METHOD_BITCOIN, PaymentMethodType.BITCOIN)
            put(MegaApiJava.PAYMENT_METHOD_UNIONPAY, PaymentMethodType.UNIONPAY)
            put(MegaApiJava.PAYMENT_METHOD_FORTUMO, PaymentMethodType.FORTUMO)
            put(MegaApiJava.PAYMENT_METHOD_STRIPE, PaymentMethodType.STRIPE)
            put(MegaApiJava.PAYMENT_METHOD_CREDIT_CARD, PaymentMethodType.CREDIT_CARD)
            put(MegaApiJava.PAYMENT_METHOD_CENTILI, PaymentMethodType.CENTILI)
            put(MegaApiJava.PAYMENT_METHOD_PAYSAFE_CARD, PaymentMethodType.PAYSAFE_CARD)
            put(MegaApiJava.PAYMENT_METHOD_ASTROPAY, PaymentMethodType.ASTROPAY)
            put(MegaApiJava.PAYMENT_METHOD_RESERVED, PaymentMethodType.RESERVED)
            put(MegaApiJava.PAYMENT_METHOD_WINDOWS_STORE, PaymentMethodType.WINDOWS_STORE)
            put(MegaApiJava.PAYMENT_METHOD_TPAY, PaymentMethodType.TPAY)
            put(MegaApiJava.PAYMENT_METHOD_DIRECT_RESELLER, PaymentMethodType.DIRECT_RESELLER)
            put(MegaApiJava.PAYMENT_METHOD_ECP, PaymentMethodType.ECP)
            put(MegaApiJava.PAYMENT_METHOD_SABADELL, PaymentMethodType.SABADELL)
            put(MegaApiJava.PAYMENT_METHOD_HUAWEI_WALLET, PaymentMethodType.HUAWEI_WALLET)
            put(MegaApiJava.PAYMENT_METHOD_STRIPE2, PaymentMethodType.STRIPE2)
            put(MegaApiJava.PAYMENT_METHOD_WIRE_TRANSFER, PaymentMethodType.WIRE_TRANSFER)
            put(unknownPaymentMethodType, null)
        }

        expectedResults.forEach { (key, value) ->
            val actual = toPaymentMethodType(key)
            assertEquals(value, actual)
        }
    }
}