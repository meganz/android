package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.photos.TimelinePhotosRequest
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import javax.inject.Inject

class MonitorTimelineMediaUseCase @Inject constructor(
    @DefaultDispatcher val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val getTimelineFilterPreferencesUseCase: GetTimelineFilterPreferencesUseCase,
    private val filterCloudDriveMediaUseCase: FilterCloudDriveMediaUseCase,
    private val filterCameraUploadMediaUseCase: FilterCameraUploadMediaUseCase,
    private val monitorMediaTypedNodesUseCase: MonitorMediaTypedNodesUseCase,
) {

    operator fun invoke(request: TimelinePhotosRequest): Flow<List<TypedFileNode>> =
        combine(
            flow = monitorMediaTypedNodesUseCase().distinctUntilChanged(),
            flow2 = monitorShowHiddenItemsUseCase().distinctUntilChanged(),
            flow3 = monitorHiddenNodesEnabledUseCase().distinctUntilChanged(),
            flow4 = request.selectedFilterFlow.distinctUntilChanged()
        ) { nodes, showHiddenItems, isHiddenNodesEnabled, newFilter ->
            processTimelineMedia(
                nodes = nodes,
                showHiddenItems = showHiddenItems,
                isHiddenNodesEnabled = isHiddenNodesEnabled,
                shouldApplyFilterFromPreference = newFilter != null
            )
        }.flowOn(defaultDispatcher)

    private suspend fun processTimelineMedia(
        nodes: List<TypedFileNode>,
        showHiddenItems: Boolean?,
        isHiddenNodesEnabled: Boolean,
        shouldApplyFilterFromPreference: Boolean,
    ): List<TypedFileNode> = nodes.filterMedia(
        isHiddenNodesEnabled = isHiddenNodesEnabled,
        shouldApplyFilterFromPreference = shouldApplyFilterFromPreference,
        showHiddenItems = showHiddenItems ?: true,
    )

    private suspend fun List<TypedFileNode>.filterMedia(
        isHiddenNodesEnabled: Boolean,
        shouldApplyFilterFromPreference: Boolean,
        showHiddenItems: Boolean,
    ): List<TypedFileNode> {
        val preferences = runCatching {
            getTimelineFilterPreferencesUseCase()
        }.getOrNull()
        val arePreferencesRemembered = preferences
            ?.get(TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value)
            .toBoolean()
        val shouldFilterMedia = arePreferencesRemembered || shouldApplyFilterFromPreference
        val mediaSource = if (shouldFilterMedia) {
            preferences?.get(TimelinePreferencesJSON.JSON_KEY_LOCATION.value)
                ?: TimelinePreferencesJSON.JSON_VAL_LOCATION_ALL_LOCATION.value
        } else null
        val mediaType = if (shouldFilterMedia) {
            preferences?.get(TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value)
                ?: TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_ALL_MEDIA.value
        } else null
        val shouldSkipNonSensitiveCheck = showHiddenItems || !isHiddenNodesEnabled

        return asSequence()
            .let { node ->
                when (mediaSource) {
                    TimelinePreferencesJSON.JSON_VAL_LOCATION_CLOUD_DRIVE.value ->
                        filterCloudDriveMediaUseCase(source = node.toList()).asSequence()

                    TimelinePreferencesJSON.JSON_VAL_LOCATION_CAMERA_UPLOAD.value ->
                        filterCameraUploadMediaUseCase(source = node.toList()).asSequence()

                    else -> node
                }
            }
            .let { node ->
                when (mediaType) {
                    TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_IMAGES.value ->
                        node.filter { it.type is ImageFileTypeInfo }

                    TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_VIDEOS.value ->
                        node.filter { it.type is VideoFileTypeInfo }

                    else -> node
                }
            }
            .filter { node ->
                // isMarkedSensitive is intentionally not used directly here —
                // it controls the sensitive design overlay, not visibility.
                val isNotSensitive = !node.isMarkedSensitive && !node.isSensitiveInherited
                shouldSkipNonSensitiveCheck || isNotSensitive
            }
            .toList()
    }

    suspend fun sortMedia(
        nodes: List<TypedFileNode>,
        sortOrder: SortOrder,
    ): List<TypedFileNode> =
        withContext(defaultDispatcher) {
            when (sortOrder) {
                SortOrder.ORDER_MODIFICATION_DESC ->
                    nodes.sortedWith(
                        compareByDescending<TypedFileNode> { it.modificationTime }
                            .thenByDescending { it.id.longValue }
                    )

                SortOrder.ORDER_MODIFICATION_ASC ->
                    nodes.sortedWith(
                        compareBy<TypedFileNode> { it.modificationTime }
                            .thenByDescending { it.id.longValue }
                    )

                else -> nodes
            }
        }
}
