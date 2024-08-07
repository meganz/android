package mega.privacy.android.app.presentation.transfers.starttransfer.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

/**
 * Data class defining the state of the start transfer.
 *
 * @property oneOffViewEvent Event to trigger events in start transfer component, like messages or dialogs
 * @property promptSaveDestination event to prompt the user to save the destination once it's is set, this can run in parallel to the download so it will be consumed in a different way than [oneOffViewEvent]
 * @property jobInProgressState Job storing the transfer progress if any.
 * @property confirmLargeDownload user needs to confirm a large download if not null.
 * @property askDestinationForDownload user needs to choose a destination for this [TransferTriggerEvent.DownloadTriggerEvent]. Depending on Android version and user settings, download destination should be asked for each new download.
 * @constructor Create empty Start transfer view state
 */
data class StartTransferViewState(
    val oneOffViewEvent: StateEventWithContent<StartTransferEvent> = consumed(),
    val promptSaveDestination: StateEventWithContent<String> = consumed(),
    val jobInProgressState: StartTransferJobInProgress? = null,
    val confirmLargeDownload: ConfirmLargeDownloadInfo? = null,
    val askDestinationForDownload: TransferTriggerEvent.DownloadTriggerEvent? = null,
)