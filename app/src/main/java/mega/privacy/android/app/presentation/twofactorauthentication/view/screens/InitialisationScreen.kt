package mega.privacy.android.app.presentation.twofactorauthentication.view.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.drawableId
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_800
import mega.privacy.android.core.ui.theme.extensions.subtitle1medium
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

@Composable
internal fun InitialisationScreen(
    onNextClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.grey_020_grey_800)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_2fa),
                contentDescription = "Lock Image",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(vertical = 32.dp)
                    .semantics { drawableId = R.drawable.ic_2fa }
                    .testTag(LOCK_IMAGE_TEST_TAG)
            )
        }
        Spacer(modifier = Modifier.padding(top = 20.dp))
        Text(
            text = stringResource(id = R.string.title_2fa),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.textColorPrimary,
            style = MaterialTheme.typography.subtitle1medium
        )
        Spacer(modifier = Modifier.padding(top = 20.dp))
        Text(
            text = stringResource(id = R.string.two_factor_authentication_explain),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.textColorSecondary,
            style = MaterialTheme.typography.subtitle1,
        )
        Spacer(modifier = Modifier.padding(top = 32.dp))
        RaisedDefaultMegaButton(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(),
            textId = R.string.button_setup_2fa,
            onClick = {
                onNextClicked()
            }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@CombinedThemePreviews
@Composable
private fun PreviewInitialisationScreen() {
    InitialisationScreen(
        onNextClicked = {},
        modifier = Modifier.semantics { testTagsAsResourceId = true },
    )
}

internal const val LOCK_IMAGE_TEST_TAG = "initialisation_screen:image_lock"