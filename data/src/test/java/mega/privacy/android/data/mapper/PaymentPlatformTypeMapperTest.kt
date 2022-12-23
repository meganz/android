package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.PaymentMethodType
import mega.privacy.android.domain.entity.PaymentPlatformType
import org.junit.Assert.*
import org.junit.Test

class PaymentPlatformTypeMapperTest {
    @Test
    fun `test that subscription platform type can be mapped correctly`() {
        val otherPaymentMethodType = PaymentMethodType.WIRE_TRANSFER
        val expectedResults = HashMap<PaymentMethodType, PaymentPlatformType>().apply {
            put(PaymentMethodType.ITUNES, PaymentPlatformType.SUBSCRIPTION_FROM_ITUNES)
            put(PaymentMethodType.GOOGLE_WALLET,
                PaymentPlatformType.SUBSCRIPTION_FROM_ANDROID_PLATFORM)
            put(PaymentMethodType.HUAWEI_WALLET,
                PaymentPlatformType.SUBSCRIPTION_FROM_ANDROID_PLATFORM)
            put(PaymentMethodType.STRIPE, PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM)
            put(PaymentMethodType.STRIPE2, PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM)
            put(PaymentMethodType.ECP, PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM)
            put(otherPaymentMethodType,
                PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM)
        }

        expectedResults.forEach { (key, value) ->
            val actual = toPaymentPlatformType(key)
            assertEquals(value, actual)
        }
    }
}