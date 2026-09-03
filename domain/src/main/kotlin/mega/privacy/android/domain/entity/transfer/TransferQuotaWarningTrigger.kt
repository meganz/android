package mega.privacy.android.domain.entity.transfer

/**
 * The kind of transfer that raised a transfer quota warning. Each trigger carries its own daily
 * allowance: starting a download and starting a stream are separate journeys, and a warning about
 * one says nothing about the other.
 */
enum class TransferQuotaWarningTrigger {

    /**
     * A download the user started.
     */
    Download,

    /**
     * Media the user is streaming.
     */
    Streaming,
}
