package mega.privacy.android.app.presentation.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetContactVerificationWarningUseCase
import mega.privacy.android.app.domain.usecase.shares.GetOutShares
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.shares.CreateShareKeyUseCase
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for [mega.privacy.android.app.main.FileContactListActivity]
 */
@HiltViewModel
class FileContactListViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val createShareKeyUseCase: CreateShareKeyUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase,
    private val getOutShares: GetOutShares,
) : ViewModel() {

    private val _megaShares = MutableStateFlow<List<MegaShare>?>(null)

    /**
     * Flow of mega shares
     */
    val megaShare = _megaShares.asStateFlow()

    private val _showNotVerifiedContactBanner = MutableStateFlow(false)

    /**
     * Flow of show not verified contact banner
     */
    val showNotVerifiedContactBanner = _showNotVerifiedContactBanner.asStateFlow()

    /**
     * Get latest [StorageState] from [MonitorStorageStateEventUseCase] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEventUseCase.getState()

    /**
     * Init share key
     *
     * @param node
     */
    suspend fun initShareKey(node: MegaNode) = runCatching {
        val typedNode = getNodeByIdUseCase(NodeId(node.handle))
        require(typedNode is FolderNode) { "Cannot create a share key for a non-folder node" }
        createShareKeyUseCase(typedNode)
    }.onFailure {
        Timber.e(it)
    }

    /**
     * function to check if contact verification feature flag is enabled
     * On every other case than success false is returned
     */
    private suspend fun isContactVerificationWarningOn() =
        getContactVerificationWarningUseCase()

    /**
     * Function to check if any contact is not verified
     * [showNotVerifiedContactBanner] is updated based on check
     */
    private fun checkIfContactNotVerifiedBannerShouldBeShown(sharesList: List<MegaShare>?) =
        viewModelScope.launch {
            runCatching {
                if (isContactVerificationWarningOn()) {
                    val showUnVerifiedContactBanner = sharesList?.any {
                        !areCredentialsVerifiedUseCase(it.user)
                    } ?: false
                    _showNotVerifiedContactBanner.update { showUnVerifiedContactBanner }
                }
            }.onFailure {
                Timber.e("All contacts verified check failed $it")
            }
        }

    /**
     * Get mega shares list
     *
     * @param node current mega node which is shared to the users
     */
    fun getMegaShares(node: MegaNode?) = viewModelScope.launch {
        node ?: return@launch
        runCatching {
            getOutShares(NodeId(node.handle))
        }.onSuccess { shares ->
            _megaShares.update { shares }
            checkIfContactNotVerifiedBannerShouldBeShown(shares)
        }.onFailure {
            Timber.e("Get mega shares failed $it")
        }
    }
}