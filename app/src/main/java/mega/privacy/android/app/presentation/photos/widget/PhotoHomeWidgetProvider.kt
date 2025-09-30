package mega.privacy.android.app.presentation.photos.widget

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.timeout
import mega.privacy.android.domain.usecase.GetPhotosByIdsUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadPublicNodePreviewUseCase
import mega.privacy.android.navigation.contract.home.HomeWidgetProvider
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
class PhotoHomeWidgetProvider @Inject constructor(
    private val monitorTimelinePhotosUseCase: GetTimelinePhotosUseCase,
    private val getPhotosByIdsUseCase: GetPhotosByIdsUseCase,
    private val downloadPublicNodePreviewUseCase: DownloadPublicNodePreviewUseCase,
) : HomeWidgetProvider {
    override suspend fun getWidgets() =
        monitorTimelinePhotosUseCase()
            .timeout(1.seconds)
            .catch {
                Timber.e("Failed to get timeline photos for home widget: ${it.message}")
            }
            .firstOrNull()?.take(2)
            ?.onEach {
                downloadPublicNodePreviewUseCase(it.id)
            }
            ?.map { photo -> photo.id.toString() }
            ?.map { id ->
                MultiCardExampleHomeWidget(
                    identifier = id,
                    getPhotosByIdsUseCase = getPhotosByIdsUseCase
                )
            }?.toSet() ?: emptySet()

    override suspend fun deleteWidget(identifier: String): Boolean {
        // In a real provider we would check and delete the widget here and return true if found and deleted successfully
        return identifier.toLongOrNull() != null
    }
}