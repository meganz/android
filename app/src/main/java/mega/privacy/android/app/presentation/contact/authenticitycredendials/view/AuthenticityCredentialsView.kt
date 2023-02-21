package mega.privacy.android.app.presentation.contact.authenticitycredendials.view

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.authenticitycredendials.model.AuthenticityCredentialsState
import mega.privacy.android.core.ui.controls.SimpleTopAppBar
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.dark_grey
import mega.privacy.android.core.ui.theme.grey_020
import mega.privacy.android.core.ui.theme.grey_800
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.grey_alpha_087
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.core.ui.theme.white_alpha_087
import mega.privacy.android.core.ui.theme.yellow_100

/**
 * Authenticity credentials view.
 *
 * @param state           [AuthenticityCredentialsState]
 * @param onButtonClicked Action for button pressed.
 * @param onBackPressed   Action for on back pressed.
 * @param onScrollChange  Action for on scroll change.
 * @param onErrorShown    Action for doing after error was shown.
 */
@Composable
fun AuthenticityCredentialsView(
    state: AuthenticityCredentialsState,
    onButtonClicked: () -> Unit,
    onBackPressed: () -> Unit,
    onScrollChange: (Boolean) -> Unit,
    onErrorShown: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val isScrolled = scrollState.value > 0
    val snackbarHostState = remember { SnackbarHostState() }
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            SimpleTopAppBar(titleId = R.string.authenticity_credentials_label,
                elevation = isScrolled,
                onBackPressed = onBackPressed)
        },
        backgroundColor = if (MaterialTheme.colors.isLight) grey_020 else grey_800
    ) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .verticalScroll(state = scrollState)) {
            ContactCredentials(state = state, onButtonClicked = onButtonClicked)
            MyCredentials(state = state)
        }

        if (state.error != null) {
            val error = stringResource(id = state.error)
            LaunchedEffect(scaffoldState.snackbarHostState) {
                val s = scaffoldState.snackbarHostState.showSnackbar(message = error,
                    duration = SnackbarDuration.Long)

                if (s == SnackbarResult.Dismissed) {
                    onErrorShown()
                }
            }
        }
    }

    SnackbarHost(modifier = Modifier.padding(8.dp), hostState = snackbarHostState)

    onScrollChange(isScrolled)
}

/**
 * View for showing contact credentials.
 *
 * @param state           [AuthenticityCredentialsState]
 * @param onButtonClicked Action for button pressed.
 */
@Composable
fun ContactCredentials(
    state: AuthenticityCredentialsState,
    onButtonClicked: () -> Unit,
) {
    var isBannerVisible by remember { mutableStateOf(true) }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = if (MaterialTheme.colors.isLight) white else dark_grey)) {

        Column {
            if (state.showContactVerificationBanner && isBannerVisible) {
                Box(modifier = Modifier
                    .testTag("CONTACT_VERIFICATION_BANNER_VIEW")
                    .fillMaxWidth()
                    .background(color = yellow_100),
                    contentAlignment = Alignment.CenterEnd) {
                    Text(modifier = Modifier.padding(start = 24.dp,
                        top = 14.dp,
                        bottom = 14.dp,
                        end = 48.dp),
                        style = MaterialTheme.typography.body2,
                        color = black,
                        text = stringResource(id = R.string.shared_items_verify_credentials_verify_person_banner_label))

                    IconButton(
                        onClick = { isBannerVisible = false },
                        modifier = Modifier.padding(start = 310.dp),
                        enabled = true,
                        content = {
                            Icon(painter = painterResource(id = R.drawable.ic_remove_chat_toolbar),
                                contentDescription = "")
                        }
                    )
                }
            }

            Text(modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp),
                style = MaterialTheme.typography.body2,
                color = if (MaterialTheme.colors.isLight) grey_alpha_087 else white_alpha_087,
                text = stringResource(id = R.string.shared_items_verify_credentials_header_outgoing))

            Text(modifier = Modifier.padding(top = 19.dp, start = 72.dp, end = 72.dp),
                style = MaterialTheme.typography.subtitle1,
                color = if (MaterialTheme.colors.isLight) grey_alpha_087 else white_alpha_087,
                text = stringResource(id = R.string.label_contact_credentials,
                    state.contactCredentials?.name ?: ""))

            Text(modifier = Modifier.padding(start = 72.dp, end = 72.dp),
                style = MaterialTheme.typography.body2,
                color = if (MaterialTheme.colors.isLight) grey_alpha_054 else white_alpha_054,
                text = state.contactCredentials?.email ?: "")

            Spacer(modifier = Modifier.height(25.dp))

            CredentialsView(modifier = Modifier.padding(start = 72.dp, end = 24.dp),
                credentials = state.contactCredentials?.credentials)

            Button(onClick = onButtonClicked,
                modifier = Modifier
                    .padding(start = 72.dp, top = 28.dp, bottom = 32.dp)
                    .alpha(if (state.isVerifyingCredentials) 0.5f else 1f),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)) {
                val actionTextId =
                    if (state.areCredentialsVerified) R.string.action_reset
                    else R.string.general_verify

                Text(modifier = Modifier.padding(horizontal = 5.dp),
                    style = MaterialTheme.typography.button,
                    letterSpacing = 0.sp,
                    text = stringResource(id = actionTextId))
            }
        }
    }
}

/**
 * View for showing my credentials.
 *
 * @param state [AuthenticityCredentialsState]
 */
@Composable
fun MyCredentials(state: AuthenticityCredentialsState) {

    val credentialsExplanation = stringResource(id = R.string.authenticity_credentials_explanation)
    val yourCredentials = stringResource(id = R.string.label_your_credentials)

    Text(modifier = Modifier.padding(start = 24.dp, top = 32.dp, end = 24.dp),
        style = MaterialTheme.typography.body2,
        color = if (MaterialTheme.colors.isLight) grey_alpha_087 else white_alpha_087,
        letterSpacing = 0.sp,
        lineHeight = 15.sp,
        text = credentialsExplanation)

    Card(modifier = Modifier
        .padding(start = 50.dp, top = 32.dp, end = 50.dp, bottom = 32.dp),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colors.secondary),
        backgroundColor = if (MaterialTheme.colors.isLight) white else black) {
        Column {
            Text(modifier = Modifier
                .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 16.dp),
                text = yourCredentials.uppercase(),
                color = if (MaterialTheme.colors.isLight) grey_alpha_087 else white_alpha_087)

            CredentialsView(modifier = Modifier
                .padding(start = 24.dp, end = 24.dp),
                credentials = state.myAccountCredentials?.credentials,
                myCredentials = true)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * General credentials view.
 *
 * @param modifier      [Modifier]
 * @param credentials   List of values for showing credentials.
 * @param myCredentials True if the view corresponds to my credentials, false if to contact credentials.
 */
@Composable
fun CredentialsView(
    modifier: Modifier,
    credentials: List<String>?,
    myCredentials: Boolean = false,
) {
    modifier.fillMaxWidth()

    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
        Row(modifier = modifier) {
            for (i in 0..4) {
                CredentialsItem(modifier = Modifier.weight(1f),
                    text = credentials?.get(i) ?: "",
                    myCredentials = myCredentials)
            }
        }
        Row(modifier = modifier.padding(top = 12.dp)) {
            for (i in 5..9) {
                CredentialsItem(modifier = Modifier.weight(1f),
                    text = credentials?.get(i) ?: "",
                    myCredentials = myCredentials)
            }
        }
    } else {
        Row(modifier = modifier) {
            credentials?.forEach { item ->
                CredentialsItem(modifier = Modifier.weight(1f),
                    text = item,
                    myCredentials = myCredentials)
            }
        }
    }
}

/**
 * Credentials view for showing each credential.
 *
 * @param modifier      [Modifier]
 * @param text          Credential text.
 * @param myCredentials True if the view corresponds to my credentials, false if to contact credentials.
 */
@Composable
fun CredentialsItem(modifier: Modifier, text: String, myCredentials: Boolean) {
    Text(modifier = modifier,
        style = if (myCredentials) MaterialTheme.typography.caption else MaterialTheme.typography.subtitle2,
        fontWeight = if (myCredentials) FontWeight.Normal else FontWeight.Medium,
        fontSize = if (myCredentials) 13.sp else 14.sp,
        color = if (MaterialTheme.colors.isLight) grey_alpha_054 else white_alpha_054,
        text = text)
}

/**
 * Authenticity credentials preview.
 */
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewAuthenticityCredentialsView")
@Composable
fun PreviewAuthenticityCredentialsView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        AuthenticityCredentialsView(state = AuthenticityCredentialsState(),
            onButtonClicked = { },
            onBackPressed = { },
            onScrollChange = { },
            onErrorShown = {})
    }
}
