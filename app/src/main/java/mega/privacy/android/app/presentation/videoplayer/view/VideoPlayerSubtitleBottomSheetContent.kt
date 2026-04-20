package mega.privacy.android.app.presentation.videoplayer.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.button.MegaRadioButton
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.icon.pack.IconPack

/**
 * Body of the subtitle modal bottom sheet for the revamped video player.
 */
@Composable
internal fun VideoPlayerSubtitleBottomSheetContent(
    rows: List<VideoPlayerSubtitleSheetAction>,
    selectOptionState: Int,
    onOffClicked: () -> Unit,
    onAddedSubtitleClicked: () -> Unit,
    onAutoMatch: (SubtitleFileInfo) -> Unit,
    onToSelectSubtitle: () -> Unit,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        rows.forEach { action ->
            val title = action.sheetTitle()
            when (action) {
                VideoPlayerSubtitleSheetAction.AddFromCloud -> {
                    FlexibleLineListItem(
                        modifier = Modifier.testTag(action.testTag),
                        title = title,
                        trailingElement = {
                            MegaIcon(
                                modifier = Modifier.size(24.dp),
                                painter = rememberVectorPainter(
                                    IconPack.Medium.Thin.Outline.ChevronRight,
                                ),
                                contentDescription = null,
                                tint = IconColor.Secondary,
                            )
                        },
                        onClickListener = {
                            handleSubtitleSheetAction(
                                action,
                                onOffClicked,
                                onAddedSubtitleClicked,
                                onAutoMatch,
                                onToSelectSubtitle,
                            )
                        },
                    )
                }

                else -> {
                    FlexibleLineListItem(
                        modifier = Modifier.testTag(action.testTag),
                        title = title,
                        trailingElement = {
                            MegaRadioButton(
                                identifier = action,
                                selected = action.isRadioSelected(selectOptionState),
                                onOptionSelected = { id ->
                                    (id as? VideoPlayerSubtitleSheetAction)?.let {
                                        handleSubtitleSheetAction(
                                            it,
                                            onOffClicked,
                                            onAddedSubtitleClicked,
                                            onAutoMatch,
                                            onToSelectSubtitle,
                                        )
                                    }
                                },
                            )
                        },
                        onClickListener = {
                            handleSubtitleSheetAction(
                                action,
                                onOffClicked,
                                onAddedSubtitleClicked,
                                onAutoMatch,
                                onToSelectSubtitle,
                            )
                        },
                    )
                }
            }
        }
    }
}
