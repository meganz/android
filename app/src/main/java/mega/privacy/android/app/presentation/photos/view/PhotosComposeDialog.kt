package mega.privacy.android.app.presentation.photos.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.core.ui.controls.MegaDialog
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.grey_alpha_087
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.core.ui.theme.white_alpha_087

@Composable
internal fun SortByDialog(
    selectedOption: Sort,
    onDialogDismissed: () -> Unit,
    onOptionSelected: (Sort) -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    MegaDialog(
        body = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.size(12.dp))
                listOf(
                    Sort.NEWEST,
                    Sort.OLDEST,
                ).forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (option != selectedOption) {
                                    onOptionSelected(option)
                                }
                                onDialogDismissed()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(
                            text = option.text(),
                            color = if (isLight) {
                                grey_alpha_087.takeIf {
                                    option == selectedOption
                                } ?: grey_alpha_054
                            } else {
                                white_alpha_087.takeIf {
                                    option == selectedOption
                                } ?: white_alpha_054
                            },
                            fontWeight = FontWeight.W400,
                            style = MaterialTheme.typography.subtitle2,
                        )
                    }
                }
            }
        },
        onDismissRequest = onDialogDismissed,
        titleString = stringResource(id = R.string.action_sort_by),
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDialogDismissed,
            ) {
                Text(
                    text = stringResource(id = R.string.general_cancel).uppercase(),
                    style = MaterialTheme.typography.button,
                    color = if (!MaterialTheme.colors.isLight) colorResource(id = R.color.teal_200) else colorResource(
                        id = R.color.teal_300
                    )
                )
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
    )
}

@Composable
internal fun FilterDialog(
    selectedOption: FilterMediaType,
    onDialogDismissed: () -> Unit,
    onOptionSelected: (FilterMediaType) -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    MegaDialog(
        body = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.size(12.dp))
                FilterMediaType.values().forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (option != selectedOption) {
                                    onOptionSelected(option)
                                }
                                onDialogDismissed()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(
                            text = option.text(),
                            color = if (isLight) {
                                grey_alpha_087.takeIf {
                                    option == selectedOption
                                } ?: grey_alpha_054
                            } else {
                                white_alpha_087.takeIf {
                                    option == selectedOption
                                } ?: white_alpha_054
                            },
                            fontWeight = FontWeight.W400,
                            style = MaterialTheme.typography.subtitle2,
                        )
                    }
                }
            }
        },
        onDismissRequest = onDialogDismissed,
        titleString = stringResource(id = R.string.photos_action_filter),
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDialogDismissed,
            ) {
                Text(
                    text = stringResource(id = R.string.general_cancel).uppercase(),
                    style = MaterialTheme.typography.button,
                    color = if (!MaterialTheme.colors.isLight) colorResource(id = R.color.teal_200) else colorResource(
                        id = R.color.teal_300
                    )
                )
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
    )
}

@Composable
internal fun RemovePhotosFromAlbumDialog(
    onDialogDismissed: () -> Unit = {},
    onPositiveButtonClick: () -> Unit = {},
) {
    val isLight = MaterialTheme.colors.isLight

    MegaDialog(
        body = {
            Text(
                text = stringResource(
                    id = R.string.photos_album_remove_from_album_confirmation_text
                )
            )
        },
        onDismissRequest = onDialogDismissed,
        confirmButton = {
            TextButton(
                onClick = onPositiveButtonClick,
            ) {
                Text(
                    text = stringResource(id = R.string.general_remove),
                    style = MaterialTheme.typography.button,
                    color = if (!MaterialTheme.colors.isLight) colorResource(id = R.color.teal_200) else colorResource(
                        id = R.color.teal_300
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDialogDismissed,
            ) {
                Text(
                    text = stringResource(id = R.string.general_cancel),
                    style = MaterialTheme.typography.button,
                    color = if (!MaterialTheme.colors.isLight) colorResource(id = R.color.teal_200) else colorResource(
                        id = R.color.teal_300
                    )
                )
            }
        },
    )
}

@Composable
private fun Sort.text(): String = when (this) {
    Sort.NEWEST -> stringResource(id = R.string.sortby_date_newest)
    Sort.OLDEST -> stringResource(id = R.string.sortby_date_oldest)
    Sort.PHOTOS -> stringResource(id = R.string.sortby_type_photo_first)
    Sort.VIDEOS -> stringResource(id = R.string.sortby_type_video_first)
}

@Composable
private fun FilterMediaType.text(): String = when (this) {
    FilterMediaType.ALL_MEDIA -> stringResource(id = R.string.filter_button_all_media_type)
    FilterMediaType.IMAGES -> stringResource(id = R.string.section_images)
    FilterMediaType.VIDEOS -> stringResource(id = R.string.sortby_type_video_first)
}
