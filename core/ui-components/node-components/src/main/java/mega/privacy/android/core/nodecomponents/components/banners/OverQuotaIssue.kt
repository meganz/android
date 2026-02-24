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

    sealed class Severity(val priority: Int) {
        data object None : Severity(0)
        sealed class Warning(severity: Int) : Severity(severity) {
            data object NonBlocking : Warning(1)
            data object Blocking : Warning(2)
        }

        data object Error : Severity(3)
    }
}