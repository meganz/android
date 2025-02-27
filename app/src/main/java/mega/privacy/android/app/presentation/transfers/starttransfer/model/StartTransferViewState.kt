package mega.privacy.android.app.presentation.transfers.starttransfer.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import java.io.File

/**
 * Data class defining the state of the start transfer.
 *
 * @property oneOffViewEvent Event to trigger events in start transfer component, like messages or dialogs
 * @property promptSaveDestination event to prompt the user to save the destination once it's is set, this can run in parallel to the download so it will be consumed in a different way than [oneOffViewEvent]
 * @property jobInProgressState Job storing the transfer progress if any.
 * @property confirmLargeDownload user needs to confirm a large download if not null.
 * @property askDestinationForDownload user needs to choose a destination for this [TransferTriggerEvent.DownloadTriggerEvent]. Depending on Android version and user settings, download destination should be asked for each new download.
 * @property requestFilesPermissionDenied True if the user denied the files permission request.
 * @property triggerEventWithoutPermission user needs to answer requested permission after triggering this event
 * @property isStorageOverQuota True if the user is in storage over quota state.
 * @property previewFileToOpen
 * @property isOpenWithAction True if the user is opening a file with another app.
 * @property transferTagToCancel A transfer tag if there is a user's request to cancel it, null otherwise.
 * @constructor Create empty Start transfer view state
 */
data class StartTransferViewState(
    val oneOffViewEvent: StateEventWithContent<StartTransferEvent> = consumed(),
    val promptSaveDestination: StateEventWithContent<SaveDestinationInfo> = consumed(),
    val jobInProgressState: StartTransferJobInProgress? = null,
    val confirmLargeDownload: ConfirmLargeDownloadInfo? = null,
    val askDestinationForDownload: TransferTriggerEvent.DownloadTriggerEvent? = null,
    val requestFilesPermissionDenied: Boolean = false,
    val triggerEventWithoutPermission: TransferTriggerEvent? = null,
    val isStorageOverQuota: Boolean = false,
    val previewFileToOpen: File? = null,
    val isOpenWithAction: Boolean = false,
    val transferTagToCancel: Int? = null,
)

/**
 * Data class defining the info required to prompt the save destination dialog.
 *
 * @property destination The destination path.
 * @property destinationName The destination name.
 */
data class SaveDestinationInfo(val destination: String, val destinationName: String)