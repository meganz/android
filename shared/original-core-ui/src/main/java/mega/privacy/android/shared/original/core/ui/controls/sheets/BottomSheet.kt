package mega.privacy.android.shared.original.core.ui.controls.sheets

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.privacy.android.shared.original.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * BottomSheet
 *
 * @param modalSheetState state of [ModalBottomSheetLayout]
 * @param sheetHeader header composable for the bottom sheet
 * @param content scaffold/layout in which bottom sheet is shown
 * @param sheetBody list of composable which will be included in sheet content below sheet header
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheet(
    modalSheetState: ModalBottomSheetState,
    sheetHeader: @Composable () -> Unit,
    sheetBody: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dividerType: DividerType = DividerType.SmallStartPadding,
    content: (@Composable () -> Unit)? = null,
) {
    BottomSheet(
        modalSheetState = modalSheetState,
        sheetBody = {
            sheetHeader()
            MegaDivider(dividerType = dividerType)
            sheetBody()
        },
        modifier = modifier,
        content = content
    )
}

/**
 * BottomSheet
 *
 * @param modalSheetState state of [ModalBottomSheetLayout]
 * @param content scaffold/layout in which bottom sheet is shown
 * @param sheetBody list of composable which will be included in sheet content below sheet header
 * @param expandedRoundedCorners rounded corners even when the state is expanded
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheet(
    modalSheetState: ModalBottomSheetState,
    sheetBody: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    sheetGesturesEnabled: Boolean = true,
    expandedRoundedCorners: Boolean = false,
    content: (@Composable () -> Unit)? = null,
) {
    val roundedCornerRadius by animateDpAsState(
        if (modalSheetState.currentValue == ModalBottomSheetValue.Expanded && !expandedRoundedCorners) 0.dp else 12.dp,
        label = "rounded corner radius animation"
    )

    ModalBottomSheetLayout(
        modifier = modifier,
        sheetShape = RoundedCornerShape(
            topStart = roundedCornerRadius,
            topEnd = roundedCornerRadius
        ),
        sheetState = modalSheetState,
        sheetGesturesEnabled = sheetGesturesEnabled,
        scrimColor = MegaOriginalTheme.colors.background.blur,
        sheetBackgroundColor = MegaOriginalTheme.colors.background.surface1,
        sheetContent = {
            sheetBody()
            Spacer(
                Modifier.windowInsetsBottomHeight(
                    WindowInsets.systemBars
                )
            )
        },
    ) {
        content?.invoke()
    }
}

@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
private fun BottomSheetPreview() {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.HalfExpanded,
        skipHalfExpanded = false,
    )
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        BottomSheet(
            modalSheetState = modalSheetState,
            sheetHeader = {
                MegaText(
                    modifier = Modifier
                        .padding(16.dp),
                    text = "Header",
                    textColor = TextColor.Primary,
                )
            },
            sheetBody = {
                LazyColumn {
                    items(100) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                modifier = Modifier
                                    .padding(start = 16.dp, end = 32.dp)
                                    .size(size = 24.dp),
                                painter = painterResource(id = IconPackR.drawable.ic_folder_medium_solid),
                                contentDescription = null,
                            )
                            MegaText(
                                modifier = Modifier
                                    .padding(vertical = 6.dp),
                                text = "title $it",
                                textColor = TextColor.Primary,
                            )
                        }
                    }
                }
            }) {
            MegaScaffold { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    OutlinedMegaButton(
                        onClick = {
                            coroutineScope.launch {
                                modalSheetState.show()
                            }
                        },
                        rounded = true,
                        text = "Show modal sheet",
                    )
                }
            }
        }
    }
}

