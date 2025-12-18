package mega.privacy.android.app.presentation.photos.widget

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.home.HomeWidget

class MultiCardExampleHomeWidget(
    val photo: Photo,
) : HomeWidget {
    override val identifier: String = "MultiCardExampleHomeWidget:${photo.id}"
    override val defaultOrder: Int = 50
    override val canDelete: Boolean = true

    override suspend fun getWidgetName(): LocalizedText {
        return LocalizedText.Literal("Photo: ${photo.name}")
    }

    @Composable
    override fun DisplayWidget(
        modifier: Modifier,
        navigationHandler: NavigationHandler,
        transferHandler: TransferHandler,
    ) {
        PhotoHomeWidgetCard(
            photo = photo,
            modifier = modifier.padding(horizontal = 16.dp)
        )
    }
}