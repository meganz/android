package mega.privacy.android.app.presentation.transfers.startdownload.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

/**
 * Data class defining the state of a download transfer.
 *
 * @property oneOffViewEvent Event to trigger events in download component, like messages or dialogs
 * @property promptSaveDestination event to prompt the user to save the destination once it's is set, this can run in parallel to the download so it will be consumed in a different way than [oneOffViewEvent]
 * @property jobInProgressState Job storing the download progress if any.
 * @property transferTriggerEvent The event that triggered the start of the transfer.
 * @constructor Create empty Start download transfer view state
 */
data class StartDownloadTransferViewState(
    val oneOffViewEvent: StateEventWithContent<StartDownloadTransferEvent> = consumed(),
    val promptSaveDestination: StateEventWithContent<String> = consumed(),
    val jobInProgressState: StartDownloadTransferJobInProgress? = null,
    val transferTriggerEvent: TransferTriggerEvent? = null,
)