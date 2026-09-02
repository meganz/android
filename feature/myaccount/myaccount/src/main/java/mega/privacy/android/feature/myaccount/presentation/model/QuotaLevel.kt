package mega.privacy.android.feature.myaccount.presentation.model

/**
 * Quota level for storage/transfer usage indicating severity
 */
enum class QuotaLevel {
    /** Normal usage - green color SupportColor.Success */
    Success,

    /** High usage warning - amber color SupportColor.Warning */
    Warning,

    /** Over quota or critical - red color SupportColor.Error */
    Error
}
