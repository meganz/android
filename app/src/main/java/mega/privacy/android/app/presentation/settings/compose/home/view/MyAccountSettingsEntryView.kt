package mega.privacy.android.app.presentation.settings.compose.home.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.util.shimmerEffect
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.presentation.settings.compose.home.model.MyAccountSettingsState
import mega.privacy.android.domain.entity.user.ContactAvatar
import mega.privacy.android.icon.pack.IconPack

internal fun LazyListScope.myAccountSettingsEntryView(
    data: MyAccountSettingsState,
) {

    item {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.wrapContentWidth()
            ) {
                AsyncImage(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(32.dp),
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(
                            ContactAvatar(id = data.userId)
                        )
                        .transformations(CircleCropTransformation())
                        .memoryCacheKey("${data.userId.id}_0}")
                        .build(),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Inside,
                )
                Column {
                    MegaText(text = data.name, textColor = TextColor.Primary)
                    MegaText(text = data.email, textColor = TextColor.Secondary)
                }
            }
            MegaIcon(
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ChevronRight),
                contentDescription = null,
            )
        }
    }
}

internal fun LazyListScope.myAccountSettingsEntryLoadingView() {
    item {
        Row {
            Spacer(
                modifier = Modifier
                    .padding(8.dp)
                    .size(32.dp),
            )
            Column {
                Spacer(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(100.dp))
                        .height(16.dp)
                        .width(120.dp)
                        .shimmerEffect()
                )
                Spacer(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(100.dp))
                        .height(16.dp)
                        .width(100.dp)
                        .shimmerEffect()
                )
            }
        }
    }
}

