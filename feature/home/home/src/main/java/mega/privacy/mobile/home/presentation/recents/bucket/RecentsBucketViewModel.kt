package mega.privacy.mobile.home.presentation.recents.bucket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.usecase.recentactions.GetRecentActionBucketByIdUseCase
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RecentsBucketUiState(
            fileCount = args.fileCount,
            timestamp = args.timestamp,
            parentFolderName = LocalizedText.Literal(args.folderName)
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadBucket()
    }

    private fun loadBucket() {
        viewModelScope.launch {
            runCatching {
                val bucket = getRecentActionBucketByIdUseCase(
                    bucketIdentifier = args.identifier,
                    excludeSensitives = false
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
                            nodeUiItems = nodeUiItems,
                            fileCount = bucket.nodes.size,
                            timestamp = bucket.timestamp,
                            parentFolderName = parentFolderName,
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

    @AssistedFactory
    interface Factory {
        fun create(args: Args): RecentsBucketViewModel
    }

    data class Args(
        val identifier: String,
        val folderName: String,
        val nodeSourceType: NodeSourceType,
        val timestamp: Long,
        val fileCount: Int,
    )
}