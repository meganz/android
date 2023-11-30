package mega.privacy.android.legacy.core.ui.controls.chips

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.body2medium
import mega.privacy.android.core.ui.theme.extensions.white_black
import mega.privacy.android.core.ui.theme.grey_alpha_012
import mega.privacy.android.core.ui.theme.grey_alpha_020
import mega.privacy.android.core.ui.theme.white_alpha_020

/**
 * Photo chip
 */
@Composable
fun PhotoChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isChecked: Boolean = false,
) {
    TextButton(
        modifier = modifier
            .height(36.dp)
            .testTag(TEST_TAG_TEXT_PHOTO_CHIP),
        onClick = onClick,
        shape = RoundedCornerShape(size = 18.dp),
        border = BorderStroke(
            1.dp, chipBorderColor(isChecked)
        ),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
        colors = chipColours(isChecked),
    ) {
        Text(
            text = text,
            style = if (isChecked) {
                MaterialTheme.typography.body2medium
            } else {
                MaterialTheme.typography.body2
            },
        )
    }
}

@Composable
private fun chipColours(checked: Boolean) = if (checked) {
    ButtonDefaults.buttonColors(
        backgroundColor = MaterialTheme.colors.black_white,
        contentColor = MaterialTheme.colors.white_black,
    )
} else {
    ButtonDefaults.buttonColors(
        backgroundColor = MaterialTheme.colors.white_black,
        contentColor = MaterialTheme.colors.black_white,
    )
}

@Composable
private fun chipBorderColor(isChecked: Boolean): Color {
    val blackChip =
        (isChecked && !isSystemInDarkTheme()) || (isSystemInDarkTheme() && !isChecked)
    val whiteChipInDarkTheme = isChecked && isSystemInDarkTheme()
    val whiteChipInLightTheme = !isChecked && !isSystemInDarkTheme()

    return when {
        blackChip -> {
            white_alpha_020
        }

        whiteChipInLightTheme -> {
            grey_alpha_012
        }

        whiteChipInDarkTheme -> {
            grey_alpha_020
        }

        else -> {
            white_alpha_020
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewPhotoChip(
    @PreviewParameter(BooleanProvider::class) isChecked: Boolean,
) {
    var checked by remember { mutableStateOf(isChecked) }

    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        PhotoChip(
            onClick = { checked = !checked },
            text = "Chip",
            modifier = Modifier,
            isChecked = checked,
        )
    }
}

internal const val TEST_TAG_TEXT_PHOTO_CHIP = "photo_chip:text_title"
