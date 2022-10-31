package mega.privacy.android.app.presentation.photos.view

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.ZoomLevel

internal fun Modifier.isSelected(isSelected: Boolean): Modifier = composed {
    if (isSelected) Modifier
        .border(BorderStroke(
            width = 2.dp,
            color = colorResource(id = R.color.teal_300)),
            shape = RoundedCornerShape(4.dp)
        )
        .clip(RoundedCornerShape(4.dp)) else Modifier
}

internal fun isDownloadPreview(
    configuration: Configuration,
    currentZoomLevel: ZoomLevel,
) = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        && currentZoomLevel.portrait == ZoomLevel.Grid_1.portrait
