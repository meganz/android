package mega.privacy.android.app.presentation.photos.timeline.view

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.app.presentation.photos.timeline.model.ApplyFilterMediaType
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import mega.privacy.android.app.presentation.photos.view.CardListView
import mega.privacy.android.app.presentation.photos.view.TimeSwitchBar
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.presentation.theme.AndroidTheme

/**
 * Base Compose Timeline View
 */
@Composable
fun TimelineView(
    photoDownload: PhotoDownload,
    timelineViewState: TimelineViewState,
    lazyGridState: LazyGridState,
    onTextButtonClick: () -> Unit,
    onFABClick: () -> Unit,
    onCardClick: (DateCard) -> Unit,
    onTimeBarTabSelected: (TimeBarTab) -> Unit,
    enableCUView: @Composable () -> Unit,
    photosGridView: @Composable () -> Unit,
    emptyView: @Composable () -> Unit,
) {
    if (timelineViewState.enableCameraUploadPageShowing) {
        enableCUView()
    } else if (timelineViewState.loadPhotosDone && timelineViewState.currentShowingPhotos.isEmpty()) {
        emptyView()
    } else {
        // Load Photos
        Box {
            when (timelineViewState.selectedTimeBarTab) {
                TimeBarTab.All -> {
                    Column {
                        val isBarVisible by remember {
                            derivedStateOf { lazyGridState.firstVisibleItemIndex == 0 }
                        }

                        val isScrollingDown = lazyGridState.isScrollingDown()
                        if (timelineViewState.enableCameraUploadButtonShowing && timelineViewState.selectedPhotoCount == 0) {
                            EnableCameraUploadButton(onClick = onTextButtonClick) {
                                isBarVisible || !isScrollingDown
                            }
                        }

                        if (timelineViewState.progressBarShowing) {
                            CameraUploadProgressBar(timelineViewState = timelineViewState) {
                                isBarVisible || !isScrollingDown
                            }
                        }

                        photosGridView()
                    }
                }
                else -> {
                    val dateCards = when (timelineViewState.selectedTimeBarTab) {
                        TimeBarTab.Years -> timelineViewState.yearsCardPhotos
                        TimeBarTab.Months -> timelineViewState.monthsCardPhotos
                        TimeBarTab.Days -> timelineViewState.daysCardPhotos
                        else -> timelineViewState.daysCardPhotos
                    }
                    CardListView(
                        state = lazyGridState,
                        dateCards = dateCards,
                        photoDownload = photoDownload,
                        onCardClick = onCardClick,
                    )
                }
            }

            if (timelineViewState.selectedPhotoCount == 0) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    val scrollInProgress by remember {
                        derivedStateOf { lazyGridState.isScrollInProgress }
                    }
                    // We don't want to show the FAB if filter and source are default
                    if (
                        timelineViewState.applyFilterMediaType != ApplyFilterMediaType.ALL_MEDIA_IN_CD_AND_CU
                    ) {
                        FilterFAB(timelineViewState = timelineViewState, onClick = onFABClick) {
                            !scrollInProgress
                        }
                    }

                    TimeSwitchBar(
                        timeBarTabs = timelineViewState.timeBarTabs,
                        onTimeBarTabSelected = onTimeBarTabSelected,
                        selectedTimeBarTab = timelineViewState.selectedTimeBarTab,
                    ) {
                        timelineViewState.selectedTimeBarTab == TimeBarTab.All || !scrollInProgress
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FilterFAB(
    timelineViewState: TimelineViewState = TimelineViewState(),
    onClick: () -> Unit = {},
    isVisible: () -> Boolean = { true },
) {
    val orientation = LocalConfiguration.current.orientation

    Row(
        modifier = Modifier.padding(
            bottom = if (
                orientation == Configuration.ORIENTATION_PORTRAIT
                && timelineViewState.currentShowingPhotos.isNotEmpty()
            ) 68.dp
            else 16.dp,
            end = 16.dp
        )
    ) {
        AnimatedVisibility(
            visible = isVisible(),
            exit = scaleOut(),
            enter = scaleIn()
        ) {
            FloatingActionButton(
                onClick = onClick,
                modifier = Modifier

                    .size(40.dp)
                    .padding(all = 0.dp)
            ) {
                Icon(
                    painter = painterResource(id = if (MaterialTheme.colors.isLight) {
                        R.drawable.ic_filter_light
                    } else {
                        R.drawable.ic_filter_dark
                    }),
                    contentDescription = "Exit filter",
                    tint = if (!MaterialTheme.colors.isLight) {
                        Color.Black
                    } else {
                        Color.White
                    }
                )
            }
        }
    }
}

/**
 * Camera Upload Progress Bar
 */
@Composable
fun CameraUploadProgressBar(
    timelineViewState: TimelineViewState = TimelineViewState(),
    isVisible: () -> Boolean = { true },
) {
    AnimatedVisibility(
        visible = isVisible(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = StringResourcesUtils.getQuantityString(
                        R.plurals.cu_upload_progress,
                        timelineViewState.pending,
                        timelineViewState.pending
                    ),
                    color = colorResource(id = R.color.grey_087_white_087),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
                )

                LinearProgressIndicator(
                    progress = timelineViewState.progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = colorResource(id = R.color.teal_300),
                    backgroundColor = colorResource(id = R.color.grey_alpha_012)
                )
            }
        }
    }
}

/**
 * Enable Camera Upload button for when it is disabled
 */
@Composable
private fun EnableCameraUploadButton(onClick: () -> Unit, isVisible: () -> Boolean) {
    AnimatedVisibility(
        visible = isVisible(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                ),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                shape = RectangleShape
            ) {
                Text(
                    text = stringResource(id = R.string.settings_camera_upload_on),
                    color = colorResource(id = R.color.teal_300),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

/**
 * CU Progress Bar Preview
 */
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "DarkPreviewProgressBar"
)
@Preview
@Composable
fun PreviewProgressBar() {
    AndroidTheme(isSystemInDarkTheme()) {
        CameraUploadProgressBar(
            timelineViewState = TimelineViewState(
                progressBarShowing = true,
                pending = 50,
                progress = 0.5f,
            )
        )
    }
}

@Composable
private fun LazyGridState.isScrollingDown(): Boolean {
    var nextIndex by remember(this) { mutableStateOf(firstVisibleItemIndex) }
    var nextScrollOffset by remember(this) { mutableStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (nextIndex != firstVisibleItemIndex) {
                nextIndex < firstVisibleItemIndex
            } else {
                nextScrollOffset <= firstVisibleItemScrollOffset
            }.also {
                nextIndex = firstVisibleItemIndex
                nextScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}