package mega.privacy.android.feature.signin.external.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.button.MegaOutlinedButton
import mega.android.core.ui.modifiers.shimmerEffect
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

/**
 * Shimmer placeholder shown while the Google Sign-In feature flag is resolving.
 */
@Composable
fun GoogleSignInButtonPlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .testTag(GOOGLE_SIGN_IN_PLACEHOLDER_TAG)
            .fillMaxWidth()
            .height(48.dp)
            .shimmerEffect(),
    )
}

internal const val GOOGLE_SIGN_IN_BUTTON_TAG = "login_screen:google_sign_in_button"
internal const val GOOGLE_SIGN_IN_PLACEHOLDER_TAG = "login_screen:google_sign_in_placeholder"
