package mega.privacy.android.feature.photos.presentation.timeline.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.banner.InlineInfoBanner
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun EnableCameraUploadsContent(
    onEnable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(vertical = 52.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Image(
                modifier = Modifier.size(180.dp),
                painter = painterResource(id = IconPackR.drawable.illustration_camera_backup_permission),
                contentDescription = "Enable camera uploads",
            )

            MegaText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                text = stringResource(sharedR.string.camera_backup_permission_screen_title),
                style = AppTheme.typography.titleLarge,
                textColor = TextColor.Primary,
                textAlign = TextAlign.Center,
            )

            MegaText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                text = stringResource(sharedR.string.camera_backup_permission_screen_description),
                style = AppTheme.typography.bodyLarge,
                textColor = TextColor.Secondary,
                textAlign = TextAlign.Center,
            )

            InlineInfoBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                body = SpannableText(
                    text = stringResource(sharedR.string.timeline_tab_enable_camera_uploads_banner_description),
                    annotations = mapOf(
                        SpanIndicator('A') to SpanStyleWithAnnotation(
                            megaSpanStyle = MegaSpanStyle.DefaultColorStyle(
                                spanStyle = SpanStyle(fontWeight = FontWeight.Bold)
                            ),
                            annotation = null
                        )
                    )
                ),
                showCancelButton = false,
            )

            PrimaryFilledButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .testTag(ENABLE_CAMERA_UPLOADS_CONTENT_ENABLE_BUTTON_TAG),
                text = stringResource(sharedR.string.camera_backup_permission_enable_button_text),
                onClick = onEnable
            )
        },
    )
}

internal const val ENABLE_CAMERA_UPLOADS_CONTENT_ENABLE_BUTTON_TAG =
    "enable_camera_uploads_content:button_enable"
