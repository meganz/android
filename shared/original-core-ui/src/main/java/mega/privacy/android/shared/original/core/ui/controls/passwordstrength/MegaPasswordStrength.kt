package mega.privacy.android.shared.original.core.ui.controls.passwordstrength

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalThemeForPreviews

/**
 * Mega Password Strength
 */
@Composable
fun MegaPasswordStrength(
    passwordStrengthValue: Int,
    passwordStrengthText: String,
    modifier: Modifier = Modifier,
) {
    val colors = mapOf(
        0 to MegaOriginalTheme.colors.components.interactive, // Very Weak
        1 to MegaOriginalTheme.colors.indicator.yellow, // Weak
        2 to MegaOriginalTheme.colors.support.success, // Medium
        3 to MegaOriginalTheme.colors.indicator.green, // Good
        4 to MegaOriginalTheme.colors.indicator.blue  // Strong
    )

    val validPasswordStrengthValue = passwordStrengthValue.coerceIn(0, colors.size - 1)

    Column {
        PasswordStrengthIndicator(
            colors = colors,
            passwordStrengthValue = validPasswordStrengthValue,
            modifier = modifier
        )

        Spacer(modifier = Modifier.height(8.dp))

        PasswordStrengthLabel(
            passwordStrengthText = passwordStrengthText,
            colors = colors,
            passwordStrengthValue = validPasswordStrengthValue
        )
    }
}

@Composable
private fun PasswordStrengthLabel(
    passwordStrengthText: String,
    colors: Map<Int, Color>,
    passwordStrengthValue: Int,
) {
    Text(
        text = passwordStrengthText,
        color = colors[passwordStrengthValue] ?: MegaOriginalTheme.colors.text.primary,
        style = MaterialTheme.typography.caption
    )
}

@Composable
private fun PasswordStrengthIndicator(
    colors: Map<Int, Color>,
    passwordStrengthValue: Int,
    modifier: Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.onEachIndexed { index, _ ->
            StrengthSegment(
                isActive = index <= passwordStrengthValue,
                activeColor = colors[passwordStrengthValue]
                    ?: MegaOriginalTheme.colors.border.strong,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StrengthSegment(
    isActive: Boolean,
    activeColor: Color,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .height(4.dp)
            .background(
                color = if (isActive) activeColor else MegaOriginalTheme.colors.border.strong,
                shape = RoundedCornerShape(2.dp)
            )
    )
}


@CombinedThemePreviews
@Composable
private fun MegaPasswordStrengthPreview(@PreviewParameter(PasswordStrengthProvider::class) params: Pair<Int, String>) {
    OriginalThemeForPreviews {
        MegaPasswordStrength(params.first, params.second)
    }
}

private class PasswordStrengthProvider : PreviewParameterProvider<Pair<Int, String>> {
    override val values = listOf(
        Pair(0, "Very Weak"),
        Pair(1, "Weak"),
        Pair(2, "Medium"),
        Pair(3, "Good"),
        Pair(4, "Strong")
    ).asSequence()
}
