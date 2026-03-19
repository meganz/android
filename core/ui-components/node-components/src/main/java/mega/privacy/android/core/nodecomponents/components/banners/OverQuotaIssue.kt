package mega.privacy.android.core.nodecomponents.components.banners

/**
 * Class to represent the over quota status
 */
data class OverQuotaStatus(
    val storage: OverQuotaIssue.Storage = OverQuotaIssue.Storage.None,
    val transfer: OverQuotaIssue.Transfer = OverQuotaIssue.Transfer.None,
) {
    val severity = listOf(storage.severity, transfer.severity).maxBy { it.priority }
    val hasStorageIssue = storage != OverQuotaIssue.Storage.None
    val hasTransferIssue = transfer != OverQuotaIssue.Transfer.None
    val hasIssues = hasStorageIssue || hasTransferIssue

    /**
     * Uploads are blocked due to a blocking issue in the storage quota
     */
    val isUploadBlocked = storage.severity.isBlocking

    /**
     * Downloads are blocked due to a blocking issue in the transfer quota
     */
    val isDownloadBlocked = transfer.severity.isBlocking
}

/**
 * Interface class to represent an over quota issue
 */
interface OverQuotaIssue {
    val severity: Severity

    sealed class Storage(override val severity: Severity) : OverQuotaIssue {
        object None : Storage(Severity.None)
        object AlmostFull : Storage(Severity.Warning.NonBlocking)
        object Full : Storage(Severity.Error)
    }

    sealed class Transfer(override val severity: Severity) : OverQuotaIssue {
        object None : Transfer(Severity.None)
        object TransferOverQuotaFreeUser : Transfer(Severity.Warning.Blocking)
        object TransferOverQuota : Transfer(Severity.Error)
    }

    sealed class Severity(val priority: Int, val isBlocking: Boolean) {
        data object None : Severity(0, false)
        sealed class Warning(severity: Int, isBlocking: Boolean) : Severity(severity, isBlocking) {
            data object NonBlocking : Warning(1, false)
            data object Blocking : Warning(2, true)
        }

        data object Error : Severity(3, true)
    }
}
