package mega.privacy.android.domain.entity

/**
 * Payment method enum class
 *
 * @param methodId Payment method id
 * @param methodName Payment method name
 * @param platformType the platform type of the payment method
 */

enum class PaymentMethod(
    val methodId: PaymentMethodType,
    val methodName: String,
    val platformType: PaymentPlatformType,
) {
    // Currently, only check 5 platforms. (iTunes, Google, Huawei, Stripe, and ECP)
    ITUNES(
        PaymentMethodType.ITUNES,
        "iTunes",
        PaymentPlatformType.SUBSCRIPTION_FROM_ITUNES
    ),
    GOOGLE_WALLET(
        PaymentMethodType.GOOGLE_WALLET,
        "Google Play",
        PaymentPlatformType.SUBSCRIPTION_FROM_ANDROID_PLATFORM
    ),
    STRIPE(
        PaymentMethodType.STRIPE,
        "Stripe",
        PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM
    ),
    ECP(
        PaymentMethodType.ECP,
        "ECP",
        PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM
    ),
    STRIPE2(
        PaymentMethodType.STRIPE2,
        "Stripe2",
        PaymentPlatformType.SUBSCRIPTION_FROM_OTHER_PLATFORM
    ),
    HUAWEI_WALLET(
        PaymentMethodType.HUAWEI_WALLET,
        "Huawei AppGallery",
        PaymentPlatformType.SUBSCRIPTION_FROM_ANDROID_PLATFORM
    )
}
