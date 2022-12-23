package mega.privacy.android.domain.entity

/**
 * Enum class for subscription platform types to pay (e.g Android platform, Itunes etc)
 */
enum class PaymentPlatformType {

    /**
     * Payment from iTunes
     */
    SUBSCRIPTION_FROM_ITUNES,

    /**
     * Payment from Android platform
     */
    SUBSCRIPTION_FROM_ANDROID_PLATFORM,

    /**
     * Payment from other platforms
     */
    SUBSCRIPTION_FROM_OTHER_PLATFORM
}