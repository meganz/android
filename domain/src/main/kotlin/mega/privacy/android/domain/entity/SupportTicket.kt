package mega.privacy.android.domain.entity

/**
 * Support ticket
 *
 * @property androidAppVersion
 * @property sdkVersion
 * @property device
 * @property currentLanguage
 * @property accountEmail
 * @property accountType
 * @property description
 * @property logFileName
 * @property deviceSdkVersionInt
 * @property deviceSdkVersionName
 */
data class SupportTicket(
    val androidAppVersion: String,
    val sdkVersion: String?,
    val device: String,
    val currentLanguage: String,
    val accountEmail: String,
    val accountType: String,
    val description: String,
    val logFileName: String?,
    val deviceSdkVersionInt: Int,
    val deviceSdkVersionName: String,
)

