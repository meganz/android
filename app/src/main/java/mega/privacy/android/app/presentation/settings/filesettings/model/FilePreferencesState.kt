package mega.privacy.android.app.presentation.settings.filesettings.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.AccountType

/**
 * State of the File Preferences screen.
 * @property numberOfPreviousVersions Number of previous versions to keep.
 * @property sizeOfPreviousVersionsInBytes Size of previous versions in bytes.
 * @property isFileVersioningEnabled Whether file versioning is enabled.
 * @property updateCacheSizeSetting Update cache size setting.
 * @property updateOfflineSize Update offline size.
 * @property deleteAllVersionsEvent Event triggered when all versions are deleted.
 * @property emptyRubbishBinEvent Event triggered when the rubbish bin is emptied.
 * @property isRubbishBinAutopurgeEnabled Whether the rubbish bin autopurge is enabled.
 * @property rubbishBinAutopurgePeriod Period of the rubbish bin autopurge in days.
 * @property errorMessageId Id of the error message.
 * @property accountType Type of the current account.
 */
data class FilePreferencesState(
    val numberOfPreviousVersions: Int? = null,
    val sizeOfPreviousVersionsInBytes: Long? = null,
    val isFileVersioningEnabled: Boolean = true,
    val updateCacheSizeSetting: Long? = null,
    val updateOfflineSize: Long? = null,
    val deleteAllVersionsEvent: StateEventWithContent<Throwable?> = consumed(),
    val emptyRubbishBinEvent: StateEventWithContent<Throwable?>,
    val isRubbishBinAutopurgeEnabled: Boolean = false,
    val rubbishBinAutopurgePeriod: Int = 0,
    val errorMessageId: Int = 0,
    val accountType: AccountType? = null,
)