package mega.privacy.android.core.ui.theme.extensions

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.white_alpha_054

/**
 * Text Color Secondary for Composable
 */
@Composable
fun MaterialTheme.textColorSecondary() = if (colors.isLight) grey_alpha_054 else white_alpha_054