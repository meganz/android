package mega.privacy.android.feature.photos.presentation.albums.decryptionkey

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.photos.R
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun AlbumDecryptionKeyScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    MegaScaffold(
        modifier = modifier.systemBarsPadding(),
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.album_decryption_key_title),
                navigationType = AppBarNavigationType.Back(onBack)
            )
        },
        content = { innerPaddings ->
            EmptyStateView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPaddings)
                    .verticalScroll(rememberScrollState()),
                title = stringResource(id = sharedR.string.album_decryption_key_title),
                description = SpannableText(stringResource(id = sharedR.string.album_decryption_key_description)),
                illustration = R.drawable.ic_decrypted_key
            )
        }
    )
}

@CombinedThemePreviews
@Composable
private fun AlbumDecryptionKeyScreenPreview() {
    AndroidThemeForPreviews {
        AlbumDecryptionKeyScreen { }
    }
}
