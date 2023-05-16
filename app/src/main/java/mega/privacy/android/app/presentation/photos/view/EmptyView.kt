package mega.privacy.android.app.presentation.photos.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.FilterMediaType

/**
 * Compose function to handle empty states
 */
@Composable
internal fun EmptyView(
    currentFilterMediaType: FilterMediaType,
) {
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                imageVector = when (currentFilterMediaType) {
                    FilterMediaType.ALL_MEDIA, FilterMediaType.IMAGES ->
                        ImageVector.vectorResource(id = R.drawable.ic_no_images)

                    FilterMediaType.VIDEOS ->
                        ImageVector.vectorResource(id = R.drawable.ic_no_videos)
                },
                contentDescription = "Empty",
                colorFilter = ColorFilter.tint(
                    color = if (MaterialTheme.colors.isLight) {
                        Color(0xFFDADADA)
                    } else {
                        Color(0xFFEAEFEF)
                    }
                ),
                alpha = if (MaterialTheme.colors.isLight) 1F else 0.16F
            )

            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(top = 42.dp)
            ) {

                val placeHolderStart = "[B]"
                val placeHolderEnd = "[/B]"

                val text: String = when (currentFilterMediaType) {
                    FilterMediaType.ALL_MEDIA ->
                        stringResource(id = R.string.timeline_empty_media)

                    FilterMediaType.IMAGES ->
                        stringResource(id = R.string.timeline_empty_images)

                    FilterMediaType.VIDEOS ->
                        stringResource(id = R.string.timeline_empty_videos)
                }.uppercase()

                Text(
                    text = text.substring(0, text.indexOf(placeHolderStart)),
                    color = colorResource(id = R.color.grey_054_white_054),
                )

                Text(
                    text = text.substring(
                        text.indexOf(placeHolderStart),
                        text.indexOf(placeHolderEnd)
                    ).replace("[B]", ""),
                    color = colorResource(id = R.color.grey_087_white_087),
                    fontWeight = FontWeight.ExtraBold,
                )

                Text(
                    text = text.substring(text.indexOf(placeHolderEnd)).replace("[/B]", ""),
                    color = colorResource(id = R.color.grey_054_white_054),
                )
            }
        }
    }
}
