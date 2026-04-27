package mega.privacy.android.shared.nodes.sheet

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Public link types
 */
enum class PublicLinkType {
    Folder,
    File,
    Album,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicLinkAuthAlertBottomSheet(
    type: PublicLinkType,
    onSignupClicked: () -> Unit,
    onLoginClicked: () -> Unit,
    onDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val title = stringResource(
        when (type) {
            PublicLinkType.Folder -> sharedR.string.public_link_auth_alert_title_folder
            PublicLinkType.File -> sharedR.string.public_link_auth_alert_title_file
            PublicLinkType.Album -> sharedR.string.public_link_auth_alert_title_album
        }
    )
    val description = stringResource(
        when (type) {
            PublicLinkType.Folder,
            PublicLinkType.File,
                -> sharedR.string.public_link_auth_alert_description_default

            PublicLinkType.Album -> sharedR.string.public_link_auth_alert_description_album
        }
    )
    MegaModalBottomSheet(
        bottomSheetBackground = MegaModalBottomSheetBackground.PageBackground,
        onDismissRequest = onDismissSheet,
        modifier = modifier
            .statusBarsPadding()
            .semantics { testTagsAsResourceId = true },
        sheetState = sheetState,
        windowInsets = WindowInsets.navigationBars
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onDismissSheet,
                modifier = Modifier
                    .align(Alignment.End)
            ) {
                MegaIcon(
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
                    tint = IconColor.Primary,
                    contentDescription = null,
                )
            }

            Image(
                painter = painterResource(id = iconPackR.drawable.ic_empty_folder),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            MegaText(
                text = title,
                textColor = TextColor.Primary,
                textAlign = TextAlign.Center,
                style = AppTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            MegaText(
                text = description,
                textColor = TextColor.Secondary,
                textAlign = TextAlign.Center,
                style = AppTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            PrimaryFilledButton(
                text = stringResource(sharedR.string.general_label_create_account),
                modifier = Modifier.fillMaxWidth(),
                onClick = onSignupClicked
            )
            Spacer(modifier = Modifier.height(16.dp))

            TextOnlyButton(
                text = stringResource(sharedR.string.login_text),
                modifier = Modifier.fillMaxWidth(),
                onClick = onLoginClicked
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun PublicLinkAuthAlertBottomSheetPreview() {
    AndroidThemeForPreviews {
        PublicLinkAuthAlertBottomSheet(
            type = PublicLinkType.Folder,
            onSignupClicked = {},
            onLoginClicked = {},
            onDismissSheet = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun PublicAlbumLinkAuthAlertBottomSheetPreview() {
    AndroidThemeForPreviews {
        PublicLinkAuthAlertBottomSheet(
            type = PublicLinkType.Album,
            onSignupClicked = {},
            onLoginClicked = {},
            onDismissSheet = {}
        )
    }
}
