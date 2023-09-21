package mega.privacy.android.domain.entity.node

/**
 * Result count
 *
 * @property successCount
 * @property errorCount
 */
data class ResultCount(
    val successCount: Int,
    val errorCount: Int,
)