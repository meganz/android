package mega.privacy.android.app.presentation.filecontact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.filecontact.model.FileContactListState
import mega.privacy.android.core.nodecomponents.mapper.RemoveShareResultMapper
import mega.privacy.android.core.nodecomponents.mapper.message.NodeMoveRequestMessageMapper
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.ResultCount
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.ShareRecipient
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.foldernode.ShareFolderUseCase
import mega.privacy.android.domain.usecase.shares.GetAllowedSharingPermissionsUseCase
import mega.privacy.android.domain.usecase.shares.MonitorShareRecipientsUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import mega.privacy.android.navigation.destination.FileContactInfoNavKey
import timber.log.Timber

@HiltViewModel(assistedFactory = ShareRecipientsViewModel.Factory::class)
internal class ShareRecipientsViewModel @AssistedInject constructor(
    @Assisted private val navKey: FileContactInfoNavKey,
    private val monitorShareRecipientsUseCase: MonitorShareRecipientsUseCase,
    private val shareFolderUseCase: ShareFolderUseCase,
    private val removeShareResultMapper: RemoveShareResultMapper,
    private val nodeMoveRequestMessageMapper: NodeMoveRequestMessageMapper,
    private val getAllowedSharingPermissionsUseCase: GetAllowedSharingPermissionsUseCase,
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
) : ViewModel() {
    private val folderInfo = navKey

    val state: StateFlow<FileContactListState> by lazy {
        combine(
            flow {
                emit(getAllowedSharingPermissionsUseCase(folderInfo.folderId))
            },
            monitorShareRecipientsUseCase(folderInfo.folderId),
            flow {
                emit(false)
                emit(getContactVerificationWarningUseCase())
            },
            eventsFlow
        ) { allowedPermissions: Set<AccessPermission>, recipients: List<ShareRecipient>, isContactVerificationWarningEnabled: Boolean, events: ShareEvents ->
            FileContactListState.Data(
                folderName = folderInfo.folderName,
                folderId = folderInfo.folderId,
                recipients = recipients.toImmutableList(),
                shareRemovedEvent = events.removeEvent,
                sharingInProgress = events.shareInProgress,
                sharingCompletedEvent = events.addEvent,
                accessPermissions = allowedPermissions.toImmutableSet(),
                isContactVerificationWarningEnabled = isContactVerificationWarningEnabled,
            )
        }.catch { error ->
            Timber.e(error)
        }.asUiStateFlow(
            viewModelScope,
            FileContactListState.Loading(
                folderName = folderInfo.folderName,
                folderId = folderInfo.folderId,
            )
        )
    }

    private val eventsFlow = MutableStateFlow<ShareEvents>(
        ShareEvents.Default
    )

    sealed interface ShareEvents {
        val addEvent: StateEventWithContent<String>
        val removeEvent: StateEventWithContent<String>
        val shareInProgress: Boolean

        data object Default : ShareEvents {
            override val addEvent: StateEventWithContent<String> = consumed()
            override val removeEvent: StateEventWithContent<String> = consumed()
            override val shareInProgress: Boolean = false
        }

        data object ShareStarted : ShareEvents {
            override val addEvent: StateEventWithContent<String> = consumed()
            override val removeEvent: StateEventWithContent<String> = consumed()
            override val shareInProgress: Boolean = true
        }

        class ShareTriggered(content: String) : ShareEvents {
            override val addEvent: StateEventWithContent<String> =
                StateEventWithContentTriggered(content)
            override val removeEvent: StateEventWithContent<String> = consumed()
            override val shareInProgress: Boolean = false
        }

        class RemoveTriggered(content: String) : ShareEvents {
            override val addEvent: StateEventWithContent<String> = consumed()
            override val removeEvent: StateEventWithContent<String> =
                StateEventWithContentTriggered(content)
            override val shareInProgress: Boolean = false
        }
    }

    fun removeShare(list: List<ShareRecipient>) {
        viewModelScope.launch {
            runCatching {
                val result: MoveRequestResult.ShareMovement = shareFolderUseCase(
                    nodeIds = listOf(folderInfo.folderId),
                    contactData = list.map { it.email },
                    accessPermission = AccessPermission.UNKNOWN,
                )
                ResultCount(
                    successCount = result.successCount,
                    errorCount = result.errorCount,
                )
            }.recover { error ->
                Timber.e(error)
                ResultCount(
                    successCount = 0,
                    errorCount = list.size,
                )
            }.onSuccess { result ->
                eventsFlow.emit(ShareEvents.RemoveTriggered(removeShareResultMapper(result)))
            }
        }
    }

    fun onShareRemovedEventHandled() {
        viewModelScope.launch {
            eventsFlow.emit(ShareEvents.Default)
        }
    }

    fun shareFolder(emailList: List<String>, permission: AccessPermission) {
        viewModelScope.launch {
            runCatching {
                eventsFlow.emit(ShareEvents.ShareStarted)
                shareFolderUseCase(
                    nodeIds = listOf(folderInfo.folderId),
                    contactData = emailList,
                    accessPermission = permission,
                )
            }.recover { error ->
                Timber.e(error)
                MoveRequestResult.ShareMovement(
                    count = 0,
                    errorCount = emailList.size,
                    nodes = listOf(folderInfo.folderHandle),
                )
            }.onSuccess { result ->
                eventsFlow.emit(ShareEvents.ShareTriggered(nodeMoveRequestMessageMapper(result)))
            }
        }
    }

    fun onSharingCompletedEventHandled() {
        viewModelScope.launch {
            eventsFlow.emit(ShareEvents.Default)
        }
    }

    fun changePermissions(list: List<ShareRecipient>, permission: AccessPermission) {
        viewModelScope.launch {
            runCatching {
                shareFolderUseCase(
                    nodeIds = listOf(folderInfo.folderId),
                    contactData = list.map { it.email },
                    accessPermission = permission,
                )
            }.onFailure { error ->
                Timber.e(error)
            }
        }
    }


    @AssistedFactory
    interface Factory {
        fun create(navKey: FileContactInfoNavKey): ShareRecipientsViewModel
    }

}

internal val FileContactInfoNavKey.folderId: NodeId
    get() = NodeId(folderHandle)

private fun interface StateTransform {
    operator fun invoke(
        state: FileContactListState.Data,
    ): FileContactListState
}