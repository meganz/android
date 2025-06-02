package mega.privacy.android.app.presentation.imagepreview.slideshow.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.lists.NumberedListView
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedResR

/**
 * Secure Slideshow Tutorial Bottom Sheet
 * This bottom sheet provides a tutorial on how to securely play a slideshow on MEGA.
 * @param onDismiss Callback to be invoked when the bottom sheet is dismissed.
 * @param modifier [Modifier] to be applied to the bottom sheet.
 */
@Composable
fun SecureSlideshowTutorialBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Image(
            painter = painterResource(id = mega.privacy.android.icon.pack.R.drawable.illustration_connected_locks),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .size(120.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        MegaText(
            text = stringResource(sharedResR.string.slideshow_tutorial_title),
            textColor = TextColor.Primary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h5.copy(
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        NumberedListView(
            list = listOf(
                stringResource(sharedResR.string.slideshow_tutorial_step1),
                stringResource(sharedResR.string.slideshow_tutorial_step2),
                stringResource(sharedResR.string.slideshow_tutorial_step3),
                stringResource(sharedResR.string.slideshow_tutorial_note)
            ),
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
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
private fun SecureSlideshowTutorialBottomSheetPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SecureSlideshowTutorialBottomSheet(
            onDismiss = {}
        )
    }
}
