package mega.privacy.android.core.theme.tokens

import androidx.compose.runtime.Composable
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Android theme with MEGA app specific color tokens
 *
 * @param isDark
 * @param content
 */
@Composable
fun MegaAppTheme(
    isDark: Boolean,
    content: @Composable () -> Unit,
) = AndroidTheme(
    isDark = isDark,
    content = content,
    darkColorTokens = MegaAppSemanticTokensDark,
    lightColorTokens = MegaAppSemanticTokensLight
)