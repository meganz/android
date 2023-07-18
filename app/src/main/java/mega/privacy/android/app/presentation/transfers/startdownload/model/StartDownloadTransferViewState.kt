package mega.privacy.android.app.presentation.transfers.startdownload.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

/**
 * Data class defining the state of a download transfer.
 *
 * @property oneOffViewEvent Event for showing any snackbar update.
 * @property jobInProgressState Job storing the download progress if any.
 * @constructor Create empty Start download transfer view state
 */
data class StartDownloadTransferViewState(
    val oneOffViewEvent: StateEventWithContent<StartDownloadTransferEvent> = consumed(),
    val jobInProgressState: StartDownloadTransferJobInProgress? = null,
)