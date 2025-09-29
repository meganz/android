package mega.privacy.android.app.presentation.photos.widget

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.timeout
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetPhotosByIdsUseCase
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.contract.home.HomeWidgetViewHolder
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
class MultiCardExampleHomeWidget(
    override val identifier: String,
    val getPhotosByIdsUseCase: GetPhotosByIdsUseCase,
) : HomeWidget {
    override val defaultOrder: Int = 50
    override fun getWidget() = flow {
        val nodeId = identifier.toLongOrNull()?.let { NodeId(it) }
        val photo = nodeId?.let { getPhotosByIdsUseCase(listOf(nodeId)).firstOrNull() }
        emit(photo)
    }.filterNotNull()
        .timeout(1.seconds)
        .catch { Timber.e("Failed to get photo for home widget: ${it.message}") }
        .map { photo ->
            HomeWidgetViewHolder(
                widgetFunction = { modifier ->
                    PhotoHomeWidgetCard(photo = photo, modifier = modifier)
                },
            )
        }
}