package mega.privacy.android.app.presentation.photos.albums.importdeeplink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.textfields.GenericTextField
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.white

@Composable
internal fun AlbumImportDeeplinkScreen(
    onApply: (String) -> Unit,
) {
    val placeholder = "https://mega.nz/collection/"
    var textFieldState by rememberSaveable { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (textFieldState.isBlank()) {
                        coroutineScope.launch {
                            scaffoldState.snackbarHostState.showSnackbar("Link is empty")
                        }
                    } else if (!textFieldState.trim().contains(placeholder)) {
                        coroutineScope.launch {
                            scaffoldState.snackbarHostState.showSnackbar("Invalid link")
                        }
                    } else {
                        onApply(textFieldState.trim())
                    }
                },
                backgroundColor = teal_300,
            ) {
                Icon(
                    modifier = Modifier.scale(scaleX = -1f, scaleY = 1f),
                    painter = painterResource(id = R.drawable.ic_arrow_back_white),
                    contentDescription = null,
                    tint = white,
                )
            }
        },
        content = { innerPaddings ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPaddings),
                verticalArrangement = Arrangement.Center,
                content = {
                    GenericTextField(
                        placeholder = placeholder,
                        onTextChange = { textFieldState = it },
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions.Default,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        text = textFieldState,
                    )
                },
            )
        },
    )
}
