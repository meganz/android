package mega.privacy.android.app.upgradeAccount

import nz.mega.sdk.MegaApiJava

/**
 *  Subscription method enum
 *  @param methodId subscription method id
 *  @param methodName subscription method name
 *  @param platformType the platform type of the subscription
 */
enum class SubscriptionMethod(
    val methodId: Int,
    val methodName: String,
    val platformType: Int
) {
    // Currently, only check 5 platforms. (iTunes, Google, Huawei, Stripe, and ECP)
    ITUNES(
        MegaApiJava.PAYMENT_METHOD_ITUNES,
        "iTunes",
        UpgradeAccountActivity.SUBSCRIPTION_FROM_ITUNES
    ),
    GOOGLE_WALLET(
        MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET,
        "Google Play",
        UpgradeAccountActivity.SUBSCRIPTION_FROM_ANDROID_PLATFORM
    ),
    STRIPE(
        MegaApiJava.PAYMENT_METHOD_STRIPE,
        "Stripe",
        UpgradeAccountActivity.SUBSCRIPTION_FROM_OTHER_PLATFORM
    ),
    ECP(
        MegaApiJava.PAYMENT_METHOD_ECP,
        "ECP",
        UpgradeAccountActivity.SUBSCRIPTION_FROM_OTHER_PLATFORM
    ),
    STRIPE2(
        MegaApiJava.PAYMENT_METHOD_STRIPE2,
        "Stripe2",
        UpgradeAccountActivity.SUBSCRIPTION_FROM_OTHER_PLATFORM
    ),
    HUAWEI_WALLET(
        MegaApiJava.PAYMENT_METHOD_HUAWEI_WALLET,
        "Huawei AppGallery",
        UpgradeAccountActivity.SUBSCRIPTION_FROM_ANDROID_PLATFORM
    )
}