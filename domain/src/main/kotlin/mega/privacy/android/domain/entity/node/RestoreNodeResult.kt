package mega.privacy.android.domain.entity.node

/**
 * Restore node result
 *
 */
sealed interface RestoreNodeResult

/**
 * Single node restore result
 *
 * @property successCount
 * @property destinationFolderName
 * @property destinationHandle The handle of the destination folder where the node was restored
 */
data class SingleNodeRestoreResult(
    val successCount: Int,
    val destinationFolderName: String?,
    val destinationHandle: Long? = null,
) : RestoreNodeResult

/**
 * Multiple nodes restore result
 *
 * @property successCount
 * @property errorCount
 */
data class MultipleNodesRestoreResult(
    val successCount: Int,
    val errorCount: Int,
) : RestoreNodeResult

/**
 * Temporary mapper to Convert [RestoreNodeResult] to [MoveRequestResult]
 */
fun RestoreNodeResult.toMovementResult() = when (this) {
    is SingleNodeRestoreResult -> {
        MoveRequestResult.Restore(
            count = successCount,
            errorCount = 0,
        )
    }

    is MultipleNodesRestoreResult -> {
        MoveRequestResult.Restore(
            count = successCount + errorCount,
            errorCount = errorCount,
        )
    }
}
