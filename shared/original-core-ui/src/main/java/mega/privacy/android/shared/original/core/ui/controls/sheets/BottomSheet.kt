package mega.privacy.android.shared.original.core.ui.controls.sheets

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme

/**
 * BottomSheet
 *
 * @param modalSheetState state of [ModalBottomSheetLayout]
 * @param sheetHeader header composable for the bottom sheet
 * @param content scaffold/layout in which bottom sheet is shown
 * @param sheetBody list of composable which will be included in sheet content below sheet header
 * @param scrimColor when bottom sheet displayed this color will overlay on background content
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheet(
    modalSheetState: ModalBottomSheetState,
    sheetHeader: @Composable () -> Unit,
    sheetBody: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    scrimColor: Color = MegaOriginalTheme.colors.background.blur,
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
        scrimColor = scrimColor,
        content = content
    )
}

/**
 * BottomSheet
 *
 * @param modalSheetState state of [ModalBottomSheetLayout]
 * @param content scaffold/layout in which bottom sheet is shown
 * @param sheetBody list of composable which will be included in sheet content below sheet header
 * @param scrimColor when bottom sheet displayed this color will overlay on background content
 * @param expandedRoundedCorners rounded corners even when the state is expanded
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheet(
    modalSheetState: ModalBottomSheetState,
    sheetBody: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    sheetGesturesEnabled: Boolean = true,
    scrimColor: Color = MegaOriginalTheme.colors.background.blur,
    expandedRoundedCorners: Boolean = false,
    content: (@Composable () -> Unit)? = null,
) {
    val roundedCornerRadius by animateDpAsState(
        if (modalSheetState.currentValue == ModalBottomSheetValue.Expanded && !expandedRoundedCorners) 0.dp else 12.dp,
        label = "rounded corner radius animation"
    )

    val statusBarColor = remember {
        derivedStateOf {
            val alpha = when (modalSheetState.targetValue) {
                ModalBottomSheetValue.Hidden -> 1f - modalSheetState.progress
                ModalBottomSheetValue.Expanded -> 1f
                ModalBottomSheetValue.HalfExpanded -> {
                    if (modalSheetState.currentValue == ModalBottomSheetValue.Hidden) {
                        modalSheetState.progress
                    } else {
                        1f
                    }
                }
            }
            Color.Black.copy(alpha = 0.32f * alpha)
        }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        val systemUiController = rememberSystemUiController()
        systemUiController.setStatusBarColor(
            statusBarColor.value,
            MegaOriginalTheme.colors.isLight
        )
    }

    ModalBottomSheetLayout(
        modifier = modifier.navigationBarsPadding(),
        sheetShape = RoundedCornerShape(
            topStart = roundedCornerRadius,
            topEnd = roundedCornerRadius
        ),
        sheetState = modalSheetState,
        sheetGesturesEnabled = sheetGesturesEnabled,
        scrimColor = scrimColor,
        sheetContent = sheetBody,
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
                Text(text = "Header")
            },
            sheetBody = {
                LazyColumn {
                    items(100) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                modifier = Modifier
                                    .padding(end = 32.dp)
                                    .size(size = 24.dp),
                                painter = painterResource(id = IconPackR.drawable.ic_folder_medium_solid),
                                contentDescription = null,
                            )
                            Text(
                                text = "title $it",
                            )
                        }
                    }
                }
            }) {
            Scaffold { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .background(Color.Yellow),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Button(onClick = {
                        coroutineScope.launch {
                            modalSheetState.show()
                        }
                    }) {
                        Text(
                            text = "Show modal sheet",
                            style = MaterialTheme.typography.body2,
                            color = MegaOriginalTheme.colors.text.primary
                        )
                    }
                }
            }
        }
    }
}

