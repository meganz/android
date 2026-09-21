package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Primary icon colour of the mini player.
 *
 * Exposed from the snowflake module so the bar can tint a canvas-drawn child, such as the loading
 * throbber, with the same token `MegaIcon` resolves for `IconColor.Primary`. A hardcoded white
 * would disappear on the light theme, which the always-dark full-screen player never has to face.
 *
 * The bar's own backdrop needs nothing from here: it uses core-ui's `BoxSurface`.
 */
@Composable
fun audioMiniPlayerIconColor(): Color = DSTokens.colors.icon.primary
