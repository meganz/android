package mega.privacy.android.shared.theme

import androidx.compose.runtime.Composable
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.shared.theme.tokens.MegaAppSemanticTokensDark
import mega.privacy.android.shared.theme.tokens.MegaAppSemanticTokensLight

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