package mega.privacy.android.app.upgradeAccount.model

/**
 * FormattedTransferSize model to share correct string for units (e.g. GB or TB) and properly formatted size as string (for storage or transfer)
 *
 * @property unit [Int]       string id for units (GB/TB)
 * @property size [String]    string with formatted size
 */
data class FormattedSize(
    val unit: Int,
    val size: String,
)
