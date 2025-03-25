package mega.privacy.android.app.presentation.settings.filesettings.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

data class FilePreferencesState(
    val numberOfPreviousVersions: Int? = null,
    val sizeOfPreviousVersionsInBytes: Long? = null,
    val isFileVersioningEnabled: Boolean = true,
    val updateCacheSizeSetting: Long? = null,
    val updateOfflineSize: Long? = null,
    val deleteAllVersionsEvent: StateEventWithContent<Throwable?> = consumed()
)