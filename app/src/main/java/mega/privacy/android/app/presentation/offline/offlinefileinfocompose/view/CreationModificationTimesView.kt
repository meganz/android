package mega.privacy.android.app.presentation.offline.offlinefileinfocompose.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import java.time.Instant.now
import kotlin.time.Duration.Companion.days

/**
 * Shows titled text with creation and modification (if any) times
 */
@Composable
internal fun CreationModificationTimesView(
    creationTimeInSeconds: Long,
    modificationTimeInSeconds: Long?,
    modifier: Modifier = Modifier,
) =
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        OfflineFileInfoTitledText(
            title = stringResource(R.string.file_properties_info_added),
            text = TimeUtils.formatLongDateTime(creationTimeInSeconds),
            modifier = Modifier.testTag(TEST_TAG_CREATION_TIME),
        )
        modificationTimeInSeconds?.let {
            OfflineFileInfoTitledText(
                title = stringResource(R.string.file_properties_info_last_modified),
                text = TimeUtils.formatLongDateTime(it),
                modifier = Modifier.testTag(TEST_TAG_MODIFICATION_TIME),
            )
        }
    }


/**
 * Preview for [CreationModificationTimesView]
 */
@CombinedTextAndThemePreviews
@Composable
private fun CreationModificationTimesPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CreationModificationTimesView(
            creationTimeInSeconds = now().epochSecond - 10.days.inWholeSeconds,
            modificationTimeInSeconds = now().epochSecond,
        )
    }
}

internal const val TEST_TAG_CREATION_TIME = "creationTime"
internal const val TEST_TAG_MODIFICATION_TIME = "modificationTime"
