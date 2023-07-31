package mega.privacy.android.app.presentation.filelink.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalElevationOverlay
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.subtitle1medium

internal const val SHARE_BUTTON_TAG = "file_link_top_bar:icon_share"
internal const val BACK_BUTTON_TAG = "file_link_top_bar:icon_back"
internal const val TITLE_TAG = "file_link_top_bar:text_title"

@Composable
internal fun FileLinkTopBar(
    title: String,
    onBackPressed: () -> Unit,
    onShareClicked: () -> Unit,
    opacityTransitionDelta: Float,
    tintColor: Color,
    modifier: Modifier = Modifier,
    titleDisplacement: Dp = 0.dp,
    titleAlpha: Float = 1f,
) {
    val backgroundAlpha = (opacityTransitionDelta * 10).coerceIn(0f, 1f)
    val elevationFactor = ((opacityTransitionDelta - 0.1f) * 2).coerceIn(0f, 1f)
    val elevation = (AppBarDefaults.TopAppBarElevation.value * elevationFactor).dp
    val systemUiController = rememberSystemUiController()
    val statusColor = MaterialTheme.colors.surface
        .surfaceColorAtElevation(absoluteElevation = elevation)
        .copy(alpha = backgroundAlpha)
    systemUiController.setStatusBarColor(
        color = statusColor,
        darkIcons = systemUiController.statusBarDarkContentEnabled
    )
    TopAppBar(
        title = {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(titleAlpha)
                    .padding(top = titleDisplacement * 2) //double because it's centered
                    .testTag(TITLE_TAG),
                text = title,
                style = MaterialTheme.typography.subtitle1medium.copy(color = tintColor),
                fontWeight = FontWeight.Medium,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(modifier = Modifier.testTag(BACK_BUTTON_TAG), onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.general_back_button),
                    tint = tintColor
                )
            }
        },
        actions = {
            IconButton(modifier = Modifier.testTag(SHARE_BUTTON_TAG), onClick = onShareClicked) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_social_share_white),
                    contentDescription = stringResource(id = R.string.general_share),
                    tint = tintColor
                )
            }
        },
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = backgroundAlpha),
        elevation = elevation,
        modifier = modifier.fillMaxWidth()
    )
}


@Composable
private fun Color.surfaceColorAtElevation(
    absoluteElevation: Dp,
): Color = LocalElevationOverlay.current?.apply(this, absoluteElevation) ?: this

@CombinedTextAndThemePreviews
@Composable
private fun FileInfoTopBarPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FileLinkTopBar(
            title = "Title",
            onBackPressed = {},
            onShareClicked = {},
            opacityTransitionDelta = 1f,
            tintColor = MaterialTheme.colors.onSurface
        )
    }
}