package mega.privacy.android.app.presentation.photos.albums.decryptionkey

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.theme.grey_alpha_087
import mega.privacy.android.core.ui.theme.white_alpha_087

@Composable
fun AlbumDecryptionKeyScreen(
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            AlbumDecryptionKeyTopBar(
                onBack = onBack,
            )
        },
        content = { innerPaddings ->
            AlbumDecryptionKeyContent(
                modifier = Modifier.padding(innerPaddings),
            )
        },
    )
}

@Composable
private fun AlbumDecryptionKeyTopBar(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.option_decryption_key),
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
                style = MaterialTheme.typography.subtitle1,
            )
        },
        modifier = modifier,
        navigationIcon = {
            IconButton(
                onClick = onBack,
                content = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back_white),
                        contentDescription = null,
                        tint = grey_alpha_087.takeIf { isLight } ?: white_alpha_087,
                    )
                },
            )
        },
        elevation = 0.dp,
    )
}

@Composable
private fun AlbumDecryptionKeyContent(
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Image(
                painter = painterResource(id = R.drawable.ic_decrypted_key),
                contentDescription = null,
                modifier = Modifier.padding(top = 72.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(id = R.string.option_decryption_key),
                fontSize = 18.sp,
                fontWeight = FontWeight.W500,
                style = MaterialTheme.typography.subtitle1,
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(id = R.string.decryption_key_explanation),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.subtitle2,
            )
        },
    )
}
