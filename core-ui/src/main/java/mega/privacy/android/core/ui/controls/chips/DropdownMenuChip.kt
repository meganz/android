package mega.privacy.android.core.ui.controls.chips

import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.preview.TextFieldProvider
import mega.privacy.android.core.ui.preview.TextFieldState
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_038_white_alpha_038

/**
 * Dropdown menu Chip
 *
 * @param options                   Options to be shown, can be of any type [T]
 * @param onOptionSelected          Detects when option is selected
 * @param onDropdownExpanded        Detects when dropdown is expanded
 * @param modifier                  [Modifier]
 * @param isDisabled                True if it's disabled. False, if not
 * @param initialSelectedOption     Initial option selected
 * @param iconId                    Icon id resource
 * @param interactionSource         [MutableInteractionSource]
 * @param optionDescriptionMapper   Can be used to map each option to the text that represents it, toString() will be used by default
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T> DropdownMenuChip(
    options: List<T>?,
    onOptionSelected: (T) -> Unit,
    onDropdownExpanded: () -> Unit,
    modifier: Modifier = Modifier,
    isDisabled: Boolean = false,
    initialSelectedOption: T? = null,
    @DrawableRes iconId: Int? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    optionDescriptionMapper: @Composable (T) -> String = { it.toString() },
) = Box {

    var expanded by remember { mutableStateOf(false) }

    if (expanded) {
        onDropdownExpanded()
    }

    val customTextSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colors.secondary,
        backgroundColor = MaterialTheme.colors.secondary
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        Box {
            initialSelectedOption?.let { optionSelected ->
                BasicTextField(
                    value = optionDescriptionMapper(optionSelected),
                    onValueChange = {},
                    modifier = modifier
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colors.onPrimary,
                            shape = RoundedCornerShape(size = 8.dp)
                        )
                        .width(IntrinsicSize.Min)
                        .height(32.dp)
                        .widthIn(min = 20.dp),
                    enabled = false,
                    readOnly = true,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    textStyle = MaterialTheme.typography.button.copy(color = if (isDisabled) MaterialTheme.colors.grey_alpha_038_white_alpha_038 else MaterialTheme.colors.onPrimary),
                ) {
                    TextFieldDefaults.TextFieldDecorationBox(
                        value = optionDescriptionMapper(optionSelected),
                        innerTextField = it,
                        singleLine = true,
                        enabled = true,
                        visualTransformation = VisualTransformation.None,
                        trailingIcon = {
                            iconId?.let { id ->
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = id),
                                    contentDescription = "Arrow",
                                    modifier = Modifier
                                        .clickable { expanded = !expanded }
                                        .width(9.dp)
                                        .height(9.dp),
                                    tint = if (isDisabled) MaterialTheme.colors.grey_alpha_038_white_alpha_038 else MaterialTheme.colors.onPrimary
                                )
                            }
                        },
                        interactionSource = interactionSource,
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 0.dp,
                            top = 6.dp,
                            bottom = 6.dp
                        )
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                ) {
                    options?.forEach { item ->
                        DropdownMenuItem(modifier = modifier.widthIn(min = 128.dp), onClick = {
                            expanded = false
                            onOptionSelected(item)
                        }) {
                            Text(text = optionDescriptionMapper(item))
                        }
                    }
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            }
        ) {}
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewDropdownMenuChip(
    @PreviewParameter(TextFieldProvider::class) state: TextFieldState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DropdownMenuChip(
            options = listOf("day", "week", "month"),
            initialSelectedOption = "day",
            modifier = Modifier,
            onOptionSelected = { },
            iconId = null,
            isDisabled = true,
            onDropdownExpanded = { }
        )
    }
}
