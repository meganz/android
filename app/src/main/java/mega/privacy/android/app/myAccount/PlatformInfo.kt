package mega.privacy.android.app.myAccount

import nz.mega.sdk.MegaApiJava

/**
 * The info regarding platform.
 * @param subscriptionMethodId subscription method id
 * @param platformName platform name
 * @param platformStoreAbbrName platform abbr name
 * @param platformStoreName name of platform app store
 */
enum class PlatformInfo(
    val subscriptionMethodId: Int,
    val platformName: String = "",
    val platformStoreAbbrName: String = "",
    val platformStoreName: String = ""
) {
    GOOGLE_WALLET(
        subscriptionMethodId = MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET,
        platformName = "Google",
        platformStoreAbbrName = "Google Play",
        platformStoreName = "Google Play"
    ),
    HUAWEI_WALLET(
        subscriptionMethodId = MegaApiJava.PAYMENT_METHOD_HUAWEI_WALLET,
        platformName = "Huawei",
        platformStoreAbbrName = "AppGallery",
        platformStoreName = "Huawei AppGallery"
    ),
    ITUNES(
        subscriptionMethodId = MegaApiJava.PAYMENT_METHOD_ITUNES,
        platformName = "Apple"
    ),
    ECP(
        subscriptionMethodId = MegaApiJava.PAYMENT_METHOD_ECP
    ),
    STRIPE(
        subscriptionMethodId = MegaApiJava.PAYMENT_METHOD_STRIPE
    ),
    STRIPE2(
        subscriptionMethodId = MegaApiJava.PAYMENT_METHOD_STRIPE2
    )
}