package mega.privacy.android.app.presentation.shares.links

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.shares.links.model.LinksState
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.usecase.node.publiclink.MonitorPublicLinksUseCase
import javax.inject.Inject

/**
 * ViewModel associated to LinksFragment
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LinksViewModel @Inject constructor(
    private val monitorPublicLinksUseCase: MonitorPublicLinksUseCase,
) : ViewModel() {

    private val currentFlow = Channel<Flow<LinksState>>()
    private val _state = MutableStateFlow<LinksState>(LinksState.Loading)

    val state: StateFlow<LinksState> = _state

    init {
        viewModelScope.launch {
            _state.emitAll(currentFlow.consumeAsFlow().flatMapLatest { it })
        }
        observeFlow(publicLinks())
    }

    private fun observeFlow(flow: Flow<LinksState>) {
        viewModelScope.launch {
            currentFlow.send(flow)
        }
    }

    private fun publicLinks() = monitorPublicLinksUseCase().map {
        if (it.isEmpty()) {
            LinksState.NoPublicLinks
        } else {
            LinksState.Data(it)
        }
    }

    fun openFolder(parentNode: PublicLinkFolder) {
        observeFlow(childLinks(parentNode))
    }

    fun closeFolder(currentFolder: PublicLinkFolder) {
        observeFlow(currentFolder.parent?.let { childLinks(it) } ?: publicLinks())
    }

    private fun childLinks(parentNode: PublicLinkFolder) =
        parentNode.children.map {
            LinksState.ChildData(currentFolder = parentNode, links = it)
        }
}