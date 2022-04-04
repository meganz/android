package mega.privacy.android.app.myAccount

/**
 * The result of subscription check
 * @param typeID the type regarding subscription dialog
 * @param platformInfo the subscription platform information
 */
data class SubscriptionCheckResult (
    val typeID: Int,
    val platformInfo: PlatformInfo? = null
)

const val TYPE_ANDROID_PLATFORM = 11
const val TYPE_ANDROID_PLATFORM_NO_NAVIGATION = 12
const val TYPE_ITUNES = 13
