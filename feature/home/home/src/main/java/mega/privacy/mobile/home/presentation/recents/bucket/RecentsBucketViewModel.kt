package mega.privacy.mobile.home.presentation.recents.bucket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.recentactions.GetRecentActionBucketByIdUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.mobile.home.presentation.recents.bucket.model.RecentsBucketUiState
import mega.privacy.mobile.home.presentation.recents.mapper.RecentsParentFolderNameMapper
import timber.log.Timber

/**
 * ViewModel for RecentsBucketScreen
 */
@HiltViewModel(assistedFactory = RecentsBucketViewModel.Factory::class)
class RecentsBucketViewModel @AssistedInject constructor(
    @Assisted val args: Args,
    private val getRecentActionBucketByIdUseCase: GetRecentActionBucketByIdUseCase,
    private val nodeUiItemMapper: NodeUiItemMapper,
    private val recentsParentFolderNameMapper: RecentsParentFolderNameMapper,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RecentsBucketUiState(
            isMediaBucket = args.isMediaBucket,
            fileCount = args.fileCount,
            timestamp = args.timestamp,
            parentFolderName = LocalizedText.Literal(args.folderName),
            nodeSourceType = args.nodeSourceType
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        monitorHiddenNodesState()
    }

    private fun loadBucket(
        excludeSensitives: Boolean = uiState.value.excludeSensitives,
    ) {
        viewModelScope.launch {
            runCatching {
                val bucket = getRecentActionBucketByIdUseCase(
                    bucketIdentifier = args.identifier,
                    excludeSensitives = excludeSensitives,
                )
                if (bucket != null) {
                    val nodeUiItems = nodeUiItemMapper(
                        nodeList = bucket.nodes,
                        nodeSourceType = args.nodeSourceType,
                    )
                    val parentFolderName = recentsParentFolderNameMapper(bucket)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = nodeUiItems,
                            fileCount = bucket.nodes.size,
                            timestamp = bucket.timestamp,
                            parentFolderName = parentFolderName,
                            excludeSensitives = excludeSensitives
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                        )
                    }
                }
            }.onFailure { throwable ->
                Timber.e(throwable, "Failed to load recent action bucket")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                    )
                }
            }
        }
    }

    private fun monitorHiddenNodesState() {
        viewModelScope.launch {
            combine(
                monitorHiddenNodesEnabledUseCase(),
                monitorShowHiddenItemsUseCase()
            ) { isHiddenNodesEnabled, showHiddenNodes ->
                isHiddenNodesEnabled to showHiddenNodes
            }
                .catch {
                    Timber.e(it, "Failed to monitor hidden nodes state")
                }
                .collectLatest { (isHiddenNodesEnabled, showHiddenNodes) ->
                    loadBucket(excludeSensitives = isHiddenNodesEnabled && !showHiddenNodes)
                }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(args: Args): RecentsBucketViewModel
    }

    data class Args(
        val identifier: String,
        val isMediaBucket: Boolean,
        val folderName: String,
        val nodeSourceType: NodeSourceType,
        val timestamp: Long,
        val fileCount: Int,
    )
}