package mega.privacy.android.app.presentation.audiosection

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.images.ThumbnailView
import mega.privacy.android.core.ui.controls.text.MiddleEllipsisText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.background_white_alpha_005
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.red_800_red_400
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.grey_alpha_040
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.core.ui.theme.transparent
import mega.privacy.android.shared.theme.MegaAppTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AudioGridViewItem(
    isSelected: Boolean,
    name: String,
    thumbnailData: Any?,
    duration: String?,
    isTakenDown: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isSelected)
                    MaterialTheme.colors.secondary
                else
                    MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                shape = RoundedCornerShape(5.dp)
            )
            .background(MaterialTheme.colors.background_white_alpha_005)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongClick() }
            )
    ) {
        Box(contentAlignment = Alignment.TopStart) {
            ThumbnailView(
                modifier = Modifier
                    .height(172.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                    .padding(1.dp),
                contentDescription = "File",
                data = thumbnailData,
                defaultImage = iconPackR.drawable.ic_audio_list,
                contentScale = ContentScale.Crop,
            )
            duration?.let {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    transparent,
                                    grey_alpha_040
                                )
                            )
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_play_arrow_white_24dp),
                        contentDescription = "Audio duration",
                        colorFilter = ColorFilter.tint(color = MaterialTheme.colors.textColorPrimary)
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = it,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.textColorPrimary
                    )
                }
            }
            if (isSelected) {
                Image(
                    painter = painterResource(id = mega.privacy.android.core.R.drawable.ic_select_folder),
                    contentDescription = "checked",
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        Divider(
            color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
            modifier = Modifier.height(1.dp)
        )
        ConstraintLayout(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            val (menuImage, txtTitle, takenDownImage) = createRefs()
            Image(
                painter = painterResource(id = mega.privacy.android.core.R.drawable.ic_dots_vertical_grey),
                contentDescription = "3 dots",
                modifier = Modifier
                    .clickable { onMenuClick() }
                    .constrainAs(menuImage) {
                        end.linkTo(parent.end)
                    }
            )
            Image(
                modifier = Modifier
                    .constrainAs(takenDownImage) {
                        end.linkTo(menuImage.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        visibility =
                            if (isTakenDown) Visibility.Visible else Visibility.Gone
                    }
                    .height(16.dp)
                    .width(16.dp),
                painter = painterResource(id = R.drawable.ic_taken_down),
                colorFilter = ColorFilter.tint(MaterialTheme.colors.red_800_red_400),
                contentDescription = "Taken Down")
            MiddleEllipsisText(
                text = name,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .constrainAs(txtTitle) {
                        start.linkTo(parent.start)
                        end.linkTo(takenDownImage.start)
                        width = Dimension.fillToConstraints
                    },
                style = MaterialTheme.typography.subtitle2,
                color = TextColor.Secondary
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun AudioGridItemViewWithoutIconsAndUnselectedPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        AudioGridViewItem(
            isSelected = false,
            name = "Audio Testing name",
            thumbnailData = null,
            duration = "1:30",
            isTakenDown = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun AudioGridItemViewWithAllIconsAndSelectedPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        AudioGridViewItem(
            isSelected = true,
            name = "Audio Testing name",
            thumbnailData = null,
            duration = "1:30",
            isTakenDown = true
        )
    }
}