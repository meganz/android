package mega.privacy.android.core.nodecomponents.components.banners

/**
 * Class to represent the over quota status
 */
data class OverQuotaStatus(
    val storage: OverQuotaIssue.Storage = OverQuotaIssue.Storage.None,
    val transfer: OverQuotaIssue.Transfer = OverQuotaIssue.Transfer.None,
) {
    val severity = listOf(storage.severity, transfer.severity).maxBy { it.ordinal }
    val hasStorageIssue = storage != OverQuotaIssue.Storage.None
    val hasTransferIssue = transfer != OverQuotaIssue.Transfer.None
}

/**
 * Interface class to represent an over quota issue
 */
interface OverQuotaIssue {
    val severity: Severity

    sealed class Storage(override val severity: Severity) : OverQuotaIssue {
        object None : Storage(Severity.None)
        object AlmostFull : Storage(Severity.Warning)
        object Full : Storage(Severity.Error)
    }

    sealed class Transfer(override val severity: Severity) : OverQuotaIssue {
        object None : Transfer(Severity.None)
        object TransferOverQuotaFreeUser : Transfer(Severity.Warning)
        object TransferOverQuota : Transfer(Severity.Error)
    }

    enum class Severity {
        None, Warning, Error
    }
}