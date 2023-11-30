package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
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
        FileInfoTitledText(
            title = stringResource(R.string.file_properties_info_added),
            text = TimeUtils.formatLongDateTime(creationTimeInSeconds),
            modifier = Modifier.testTag(TEST_TAG_CREATION_TIME),
        )
        modificationTimeInSeconds?.let {
            FileInfoTitledText(
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
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        CreationModificationTimesView(
            creationTimeInSeconds = now().epochSecond - 10.days.inWholeSeconds,
            modificationTimeInSeconds = now().epochSecond,
        )
    }
}