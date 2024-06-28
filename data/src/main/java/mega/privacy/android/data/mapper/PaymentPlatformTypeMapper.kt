package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.PaymentMethodType
import mega.privacy.android.domain.entity.PaymentPlatformType
import javax.inject.Inject


/**
 * Map [PaymentMethodType] to [PaymentPlatformType]
 */
internal class PaymentPlatformTypeMapper @Inject constructor() {
    /**
     * Convert [PaymentMethodType] to [PaymentPlatformType]
     *
     * @param type [PaymentMethodType]
     * @return     [PaymentPlatformType]
     */
    operator fun invoke(type: PaymentMethodType): PaymentPlatformType = when (type) {
        PaymentMethodType.ITUNES -> PaymentPlatformType.SUBSCRIPTION_FROM_ITUNES
        PaymentMethodType.GOOGLE_WALLET -> PaymentPlatformType.SUBSCRIPTION_FROM_GOOGLE_PLATFORM
        PaymentMethodType.HUAWEI_WALLET -> PaymentPlatformType.SUBSCRIPTION_FROM_HUAWEI_PLATFORM
        PaymentMethodType.STRIPE -> PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM
        PaymentMethodType.STRIPE2 -> PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM
        PaymentMethodType.ECP -> PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM
        else -> PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM
    }
}