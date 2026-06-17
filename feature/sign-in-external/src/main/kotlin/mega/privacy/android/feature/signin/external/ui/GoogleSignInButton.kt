package mega.privacy.android.feature.signin.external.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.button.MegaOutlinedButton
import mega.privacy.android.feature.signin.external.R
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Google Sign-In button following Google branding guidelines.
 *
 * @param onClick Callback when the button is clicked.
 * @param isLoading True if sign-in is in progress.
 * @param modifier [Modifier]
 */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    MegaOutlinedButton(
        onClick = onClick,
        modifier = modifier
            .testTag(GOOGLE_SIGN_IN_BUTTON_TAG)
            .fillMaxWidth(),
        text = stringResource(sharedR.string.google_sign_in),
        leadingIcon = painterResource(id = R.drawable.ic_google_logo),
        enabled = !isLoading,
        isLoading = isLoading,
    )
}

internal const val GOOGLE_SIGN_IN_BUTTON_TAG = "login_screen:google_sign_in_button"
