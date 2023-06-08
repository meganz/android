package mega.privacy.android.app.presentation.photos.timeline.photosfilter.view

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import mega.privacy.android.core.ui.theme.AndroidTheme

@Composable
fun PhotosFilterView(
    timelineViewState: TimelineViewState = TimelineViewState(),
    onMediaTypeSelected: (FilterMediaType) -> Unit = {},
    onSourceSelected: (TimelinePhotosSource) -> Unit = {},
    applyFilter: () -> Unit = {},
    isRememberTimelinePreferencesEnabled: suspend () -> Boolean = { false },
    onCheckboxClicked: (Boolean) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    val isRememberTimelinePreferenceAppFeatureEnabled by produceState(initialValue = false) {
        value = isRememberTimelinePreferencesEnabled()
    }

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            MediaTypeView(timelineViewState, onMediaTypeSelected)

            MediaSourceView(
                timelineViewState = timelineViewState,
                onSourceSelected = onSourceSelected
            )

            if (isRememberTimelinePreferenceAppFeatureEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(id = R.string.photos_timeline_filter_remember_preferences),
                        color = MaterialTheme.colors.onPrimary,
                    )

                    Checkbox(
                        checked = timelineViewState.rememberFilter,
                        onCheckedChange = { onCheckboxClicked(!timelineViewState.rememberFilter) },
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Button(
                onClick = applyFilter,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(id = R.color.teal_300_teal_200)
                )
            ) {
                Icon(
                    painter = painterResource(id = if (MaterialTheme.colors.isLight) {
                            R.drawable.ic_filter_light
                        } else {
                            R.drawable.ic_filter_dark
                        }
                    ),
                    contentDescription = "Exit filter",
                    tint = if (!MaterialTheme.colors.isLight) {
                        Color.Black
                    } else {
                        Color.White
                    },
                )
                Text(
                    text = stringResource(id = R.string.photos_action_filter),
                    color = if (!MaterialTheme.colors.isLight) {
                        Color.Black
                    } else {
                        Color.White
                    },
                )
            }
        }
    }


}

@Composable
fun MediaTypeView(
    timelineViewState: TimelineViewState,
    onMediaTypeSelected: (FilterMediaType) -> Unit,
) {
    Text(
        text = stringResource(id = R.string.filter_prompt_media_type),
        style = MaterialTheme.typography.subtitle1,
        color = colorResource(id = R.color.grey_087_white_087),
        modifier = Modifier.padding(all = 16.dp)
    )
    FlowRow(
        modifier = Modifier
            .padding(all = 16.dp)
            .width(IntrinsicSize.Max)
    ) {
        FilterMediaType.values().forEach {
            val selected = it == timelineViewState.currentFilterMediaType
            OutlinedButton(
                onClick = { onMediaTypeSelected(it) },
                border = BorderStroke(
                    1.dp,
                    if (selected) colorResource(id = R.color.teal_300_teal_200)
                    else Color.Gray
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = if (selected) colorResource(id = R.color.teal_300_teal_200)
                    else Color.Transparent,
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.toggleable(
                    value = selected,
                    role = Role.Button,
                    onValueChange = {},
                )
            ) {
                AnimatedVisibility(
                    visible = selected,
                    enter = slideInHorizontally(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(id = R.string.filter_prompt_media_type),
                        tint = if (MaterialTheme.colors.isLight) Color.White else Color.Black,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 6.dp)
                    )
                }

                Text(
                    text = when (it) {
                        FilterMediaType.ALL_MEDIA -> stringResource(id = R.string.filter_button_all_media_type)
                        FilterMediaType.IMAGES -> stringResource(id = R.string.section_images)
                        FilterMediaType.VIDEOS -> stringResource(id = R.string.sortby_type_video_first)
                    },
                    color = if (selected && MaterialTheme.colors.isLight) Color.White
                    else if (selected && !MaterialTheme.colors.isLight) Color.Black
                    else Color.Gray
                )
            }
            Spacer(Modifier.width(5.dp))
        }
    }
}

@Composable
fun MediaSourceView(
    timelineViewState: TimelineViewState,
    onSourceSelected: (TimelinePhotosSource) -> Unit,
) {

    Text(
        text = stringResource(id = R.string.filter_prompt_media_source),
        style = MaterialTheme.typography.subtitle1,
        color = colorResource(id = R.color.grey_087_white_087),
        modifier = Modifier.padding(all = 16.dp)
    )

    TimelinePhotosSource.values().forEach {
        val selected = it == timelineViewState.currentMediaSource
        Row(modifier = Modifier
            .toggleable(
                value = selected,
                role = Role.RadioButton,
                onValueChange = {}
            )
            .clickable { onSourceSelected(it) }
            .padding(16.dp)
        ) {
            RadioButton(selected = selected, onClick = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (it) {
                        TimelinePhotosSource.ALL_PHOTOS -> stringResource(R.string.filter_button_all_source)
                        TimelinePhotosSource.CLOUD_DRIVE -> stringResource(R.string.filter_button_cd_only)
                        TimelinePhotosSource.CAMERA_UPLOAD -> stringResource(R.string.photos_filter_camera_uploads)
                    },
                    modifier = Modifier.padding(start = 16.dp),
                    color = if (selected)
                        colorResource(id = R.color.grey_087_white_087)
                    else {
                        colorResource(id = R.color.grey_054_white_054)
                    },
                )
            }
        }
    }

}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "DarkPreviewPhotosFilterView"
)
@Preview
@Composable
fun PreviewPhotosFilterView() {
    var selectedType by remember { mutableStateOf(FilterMediaType.ALL_MEDIA) }
    var selectedSource by remember { mutableStateOf(TimelinePhotosSource.ALL_PHOTOS) }
    AndroidTheme(isSystemInDarkTheme()) {
        PhotosFilterView(
            timelineViewState = TimelineViewState(
                currentFilterMediaType = selectedType,
                currentMediaSource = selectedSource
            ),
            onMediaTypeSelected = { selectedType = it },
            onSourceSelected = { selectedSource = it },
            applyFilter = {},
        )
    }
}
