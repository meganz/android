package mega.privacy.android.app.presentation.clouddrive

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Colored Folders Onboarding Bottom Sheet
 * This bottom sheet introduces the new colorful folders feature to users.
 * @param onDismiss Callback to be invoked when the bottom sheet is dismissed.
 * @param modifier [Modifier] to be applied to the bottom sheet.
 */
@Composable
fun ColoredFoldersOnboardingBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            painter = painterResource(id = iconPackR.drawable.ic_label_colored_folders),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        MegaText(
            text = stringResource(sharedR.string.sync_promotion_bottom_sheet_whats_new_label),
            textColor = TextColor.Brand,
            style = AppTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        MegaText(
            text = stringResource(sharedR.string.colored_folders_onboarding_title),
            textColor = TextColor.Primary,
            textAlign = TextAlign.Center,
            style = AppTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        MegaText(
            text = stringResource(sharedR.string.colored_folders_onboarding_description),
            textColor = TextColor.Secondary,
            textAlign = TextAlign.Center,
            style = AppTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        PrimaryFilledButton(
            text = stringResource(R.string.cloud_drive_media_discovery_banner_ok),
            modifier = Modifier.fillMaxWidth(),
            onClick = onDismiss,
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@CombinedThemePreviews
@Composable
private fun ColoredFoldersOnboardingBottomSheetPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ColoredFoldersOnboardingBottomSheet(
            onDismiss = {}
        )
    }
}
