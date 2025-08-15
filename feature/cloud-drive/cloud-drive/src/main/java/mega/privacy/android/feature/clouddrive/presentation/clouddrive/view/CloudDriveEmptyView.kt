package mega.privacy.android.feature.clouddrive.presentation.clouddrive.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.LinkSpannedText
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.icon.pack.R as iconPackR

@Composable
fun CloudDriveEmptyView(
    modifier: Modifier = Modifier,
    isRootCloudDrive: Boolean = false,
) {
    val imageDrawable = if (isRootCloudDrive) {
        iconPackR.drawable.ic_empty_cloud_glass
    } else {
        iconPackR.drawable.ic_empty_folder_glass
    }
    val textId = if (isRootCloudDrive) {
        R.string.context_empty_cloud_drive
    } else {
        R.string.file_browser_empty_folder_new
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier.size(120.dp),
            painter = painterResource(imageDrawable),
            contentDescription = "Empty",
        )
        Spacer(modifier = Modifier.height(6.dp))
        LinkSpannedText(
            value = stringResource(textId),
            spanStyles = mapOf(
                SpanIndicator('A') to SpanStyleWithAnnotation(
                    megaSpanStyle = MegaSpanStyle.TextColorStyle(
                        spanStyle = SpanStyle(),
                        textColor = TextColor.Primary
                    ),
                    annotation = "A"
                ),
                SpanIndicator('B') to SpanStyleWithAnnotation(
                    megaSpanStyle = MegaSpanStyle.TextColorStyle(
                        spanStyle = SpanStyle(),
                        textColor = TextColor.Secondary
                    ),
                    annotation = "B"
                )
            ),
            baseStyle = AppTheme.typography.bodyLarge,
            baseTextColor = TextColor.Secondary,
            onAnnotationClick = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CloudDriveEmptyViewPreview() {
    AndroidThemeForPreviews {
        CloudDriveEmptyView(
            isRootCloudDrive = true
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FolderEmptyViewPreview() {
    AndroidThemeForPreviews {
        CloudDriveEmptyView(
            isRootCloudDrive = false
        )
    }
}