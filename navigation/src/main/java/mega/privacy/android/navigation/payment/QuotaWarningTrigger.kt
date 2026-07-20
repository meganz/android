package mega.privacy.android.navigation.payment

/**
 * The user action that triggered the quota-warning upsell screen. Together with the
 * [QuotaWarningType] it selects the exact dialog copy (e.g. download vs media-streaming
 * subtitles, or the "trying to upload more" storage subtitle).
 */
enum class QuotaWarningTrigger {
    /**
     * No specific action — e.g. the screen was shown on login or reload rather than as a result
     * of a download, upload or streaming attempt.
     */
    General,

    /**
     * The user was downloading files.
     */
    Download,

    /**
     * The user was uploading files.
     */
    Upload,

    /**
     * The user was streaming media.
     */
    Streaming,
}
