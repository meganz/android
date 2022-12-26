package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.PaymentMethodType
import mega.privacy.android.domain.entity.PaymentPlatformType


/**
 * Map [PaymentMethodType] to [PaymentPlatformType]
 */
typealias PaymentPlatformTypeMapper = (@JvmSuppressWildcards PaymentMethodType) -> @JvmSuppressWildcards PaymentPlatformType

/**
 * Map [PaymentMethodType] to [PaymentPlatformType]. Return value can be subclass of [PaymentPlatformType]
 */
internal fun toPaymentPlatformType(type: PaymentMethodType): PaymentPlatformType = when (type) {
    PaymentMethodType.ITUNES -> PaymentPlatformType.SUBSCRIPTION_FROM_ITUNES
    PaymentMethodType.GOOGLE_WALLET -> PaymentPlatformType.SUBSCRIPTION_FROM_ANDROID_PLATFORM
    PaymentMethodType.HUAWEI_WALLET -> PaymentPlatformType.SUBSCRIPTION_FROM_ANDROID_PLATFORM
    PaymentMethodType.STRIPE -> PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM
    PaymentMethodType.STRIPE2 -> PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM
    PaymentMethodType.ECP -> PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM
    else -> PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM
}