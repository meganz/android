package mega.privacy.android.app.presentation.photos.albums.importlink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.MegaDialog
import mega.privacy.android.core.ui.controls.textfields.GenericTextField
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.grey_alpha_087
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.core.ui.theme.white_alpha_087

@Composable
internal fun AlbumImportScreen(
    albumImportViewModel: AlbumImportViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val state by albumImportViewModel.stateFlow.collectAsStateWithLifecycle()

    val scaffoldState = rememberScaffoldState()

    LaunchedEffect(state.isInitialized) {
        if (!state.isInitialized) {
            albumImportViewModel.initialize()
        }
    }

    if (state.showInputDecryptionKeyDialog) {
        InputDecryptionKeyDialog(
            onDismiss = onBack,
            onDecrypt = { key ->
                albumImportViewModel.closeInputDecryptionKeyDialog()
                albumImportViewModel.decryptLink(key)
            },
        )
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            AlbumImportTopBar(
                onBack = onBack,
            )
        },
        content = {},
    )
}

@Composable
private fun AlbumImportTopBar(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    TopAppBar(
        title = {
            Column(
                verticalArrangement = Arrangement.Center,
                content = {
                    Text(
                        text = "${stringResource(id = R.string.title_mega_info_empty_screen)} - " + stringResource(
                            id = R.string.general_loading
                        ),
                        color = grey_alpha_087.takeIf { isLight } ?: white_alpha_087,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W500,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.subtitle1,
                    )
                },
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
private fun InputDecryptionKeyDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onDecrypt: (String) -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight
    var text by rememberSaveable { mutableStateOf("") }

    MegaDialog(
        modifier = modifier,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
        onDismissRequest = onDismiss,
        titleString = stringResource(id = R.string.album_import_input_decryption_key_title),
        fontWeight = FontWeight.W500,
        body = {
            Column(
                content = {
                    Text(
                        text = stringResource(id = R.string.album_import_input_decryption_key_description),
                        color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W400,
                        style = MaterialTheme.typography.subtitle1,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GenericTextField(
                        placeholder = "",
                        onTextChange = { text = it },
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions.Default,
                        text = text,
                    )
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onDecrypt(text.trim()) },
                modifier = Modifier.alpha(0.4f.takeIf { text.isBlank() } ?: 1f),
                enabled = text.isNotBlank(),
                content = {
                    Text(
                        text = stringResource(id = R.string.general_decryp),
                        color = teal_300,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        style = MaterialTheme.typography.button,
                    )
                },
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                content = {
                    Text(
                        text = stringResource(id = R.string.general_cancel),
                        color = teal_300,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        style = MaterialTheme.typography.button,
                    )
                },
            )
        },
    )
}
