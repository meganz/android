package mega.privacy.android.app.presentation.filecontact

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.main.dialog.shares.RemoveShareResultMapper
import mega.privacy.android.app.presentation.filecontact.model.FileContactListState
import mega.privacy.android.app.presentation.filecontact.navigation.FileContactInfo
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.ResultCount
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.ShareRecipient
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.foldernode.ShareFolderUseCase
import mega.privacy.android.domain.usecase.shares.GetAllowedSharingPermissionsUseCase
import mega.privacy.android.domain.usecase.shares.MonitorShareRecipientsUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ShareRecipientsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val monitorShareRecipientsUseCase: MonitorShareRecipientsUseCase,
    private val shareFolderUseCase: ShareFolderUseCase,
    private val removeShareResultMapper: RemoveShareResultMapper,
    private val moveRequestMessageMapper: MoveRequestMessageMapper,
    private val getAllowedSharingPermissionsUseCase: GetAllowedSharingPermissionsUseCase,
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
) : ViewModel() {
    private val folderInfo = savedStateHandle.toRoute<FileContactInfo>()
    val state: StateFlow<FileContactListState>
        field: MutableStateFlow<FileContactListState> = MutableStateFlow(
            FileContactListState.Loading(
                folderName = folderInfo.folderName,
                folderId = folderInfo.folderId,
            )
        )

    init {
        viewModelScope.launch {
            combine(
                flow {
                    emit(getAllowedSharingPermissionsUseCase(folderInfo.folderId))
                },
                monitorShareRecipientsUseCase(folderInfo.folderId),
            ) { allowedPermissions: Set<AccessPermission>, recipients: List<ShareRecipient> ->
                StateTransform {
                    it.copy(
                        recipients = recipients.toImmutableList(),
                        accessPermissions = allowedPermissions.toImmutableSet(),
                    )
                }
            }.catch { error ->
                Timber.e(error)
            }.collect { transformer: StateTransform ->
                state.updateToData(transformer::invoke)
            }
        }
        checkVerificationWarning()
    }

    private fun checkVerificationWarning() {
        viewModelScope.launch {
            runCatching {
                getContactVerificationWarningUseCase()
            }.onSuccess { isEnabled ->
                state.updateToData {
                    it.copy(
                        isContactVerificationWarningEnabled = isEnabled,
                    )
                }
            }.onFailure { error ->
                Timber.e(error)
            }
        }
    }

    private fun MutableStateFlow<FileContactListState>.updateToData(function: (FileContactListState.Data) -> FileContactListState) {
        update {
            if (it is FileContactListState.Data) {
                function(it)
            } else {
                function(
                    FileContactListState.Data(
                        folderName = folderInfo.folderName,
                        folderId = folderInfo.folderId,
                        recipients = emptyList<ShareRecipient>().toImmutableList(),
                        shareRemovedEvent = consumed(),
                        sharingInProgress = false,
                        sharingCompletedEvent = consumed(),
                        accessPermissions = emptySet<AccessPermission>().toImmutableSet(),
                        isContactVerificationWarningEnabled = false,
                    )
                )
            }
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
                state.updateToData {
                    it.copy(
                        shareRemovedEvent = StateEventWithContentTriggered<String>(
                            removeShareResultMapper(result)
                        )
                    )
                }
            }
        }
    }

    fun onShareRemovedEventHandled() {
        state.updateToData {
            it.copy(
                shareRemovedEvent = consumed(),
            )
        }
    }

    fun shareFolder(emailList: List<String>, permission: AccessPermission) {
        viewModelScope.launch {
            runCatching {
                state.updateToData {
                    it.copy(
                        sharingInProgress = true,
                    )
                }
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
                state.updateToData {
                    it.copy(
                        sharingCompletedEvent = StateEventWithContentTriggered<String>(
                            moveRequestMessageMapper(result)
                        ),
                        sharingInProgress = false,
                    )
                }
            }
        }
    }

    fun onSharingCompletedEventHandled() {
        state.updateToData {
            it.copy(
                sharingCompletedEvent = consumed(),
            )
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

}

private fun interface StateTransform {
    operator fun invoke(
        state: FileContactListState.Data,
    ): FileContactListState
}