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
import de.palm.composestateevents.triggered
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.filecontact.model.FileContactListState
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.core.nodecomponents.mapper.RemoveShareResultMapper
import mega.privacy.android.core.nodecomponents.mapper.message.NodeMoveRequestMessageMapper
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.ResultCount
import mega.privacy.android.domain.entity.node.SensitiveNodeShareWarning
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.ShareRecipient
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.foldernode.ShareFolderUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.GetShareFolderSensitiveWarningUseCase
import mega.privacy.android.domain.usecase.shares.GetAllowedSharingPermissionsUseCase
import mega.privacy.android.domain.usecase.shares.MonitorShareRecipientsUseCase
import mega.privacy.android.feature_flags.AppFeatures
import timber.log.Timber

@HiltViewModel(assistedFactory = ShareRecipientsViewModel.Factory::class)
internal class ShareRecipientsViewModel @AssistedInject constructor(
    @Assisted private val args: Args,
    private val monitorShareRecipientsUseCase: MonitorShareRecipientsUseCase,
    private val shareFolderUseCase: ShareFolderUseCase,
    private val removeShareResultMapper: RemoveShareResultMapper,
    private val nodeMoveRequestMessageMapper: NodeMoveRequestMessageMapper,
    private val getAllowedSharingPermissionsUseCase: GetAllowedSharingPermissionsUseCase,
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getShareFolderSensitiveWarningUseCase: GetShareFolderSensitiveWarningUseCase,
) : ViewModel() {

    val state: StateFlow<FileContactListState> by lazy {
        combine(
            flow {
                emit(getAllowedSharingPermissionsUseCase(args.folderId))
            },
            monitorShareRecipientsUseCase(args.folderId),
            flow {
                emit(false)
                emit(getContactVerificationWarningUseCase())
            },
            eventsFlow,
            addContactFlow,
        ) { allowedPermissions: Set<AccessPermission>, recipients: List<ShareRecipient>, isContactVerificationWarningEnabled: Boolean, events: ShareEvents, addContact: AddContactState ->
            FileContactListState.Data(
                folderName = args.folderName,
                folderId = args.folderId,
                recipients = recipients.toImmutableList(),
                shareRemovedEvent = events.removeEvent,
                sharingInProgress = events.shareInProgress,
                sharingCompletedEvent = events.addEvent,
                accessPermissions = allowedPermissions.toImmutableSet(),
                isContactVerificationWarningEnabled = isContactVerificationWarningEnabled,
                sensitiveNodeShareWarning = addContact.warning,
                navigateToAddContactEvent = addContact.navigateEvent,
            )
        }.catch { error ->
            Timber.e(error)
        }.asUiStateFlow(
            viewModelScope,
            FileContactListState.Loading(
                folderName = args.folderName,
                folderId = args.folderId,
            )
        )
    }

    private val eventsFlow = MutableStateFlow<ShareEvents>(
        ShareEvents.Default
    )

    private val addContactFlow = MutableStateFlow(AddContactState())

    private data class AddContactState(
        val warning: SensitiveNodeShareWarning = SensitiveNodeShareWarning.None,
        val navigateEvent: StateEventWithContent<Long> = consumed(),
    )

    /**
     * Called when the user chooses to add contacts to the shared folder. On the Compose picker path
     * ([AppFeatures.ContactsComposeUI]) a hidden/sensitive-node warning is shown first when needed;
     * the legacy picker warns itself, so no warning is surfaced here for it.
     */
    fun onAddContactClicked() {
        viewModelScope.launch {
            val isComposeContactsPicker = runCatching {
                getFeatureFlagValueUseCase(AppFeatures.ContactsComposeUI)
            }.getOrDefault(false)
            val warning = if (isComposeContactsPicker) {
                runCatching {
                    getShareFolderSensitiveWarningUseCase(listOf(args.folderId))
                }.getOrDefault(SensitiveNodeShareWarning.None)
            } else {
                SensitiveNodeShareWarning.None
            }
            if (warning == SensitiveNodeShareWarning.None) {
                addContactFlow.value = AddContactState(
                    navigateEvent = triggered(args.folderHandle),
                )
            } else {
                addContactFlow.value = AddContactState(warning = warning)
            }
        }
    }

    /**
     * Called when the user confirms the hidden/sensitive-node warning; proceeds to the picker.
     */
    fun onShareHiddenNodeWarningConfirmed() {
        addContactFlow.value = AddContactState(
            navigateEvent = triggered(args.folderHandle),
        )
    }

    /**
     * Called when the user dismisses the hidden/sensitive-node warning; aborts adding contacts.
     */
    fun clearAddContactState() {
        addContactFlow.value = AddContactState()
    }

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
                    nodeIds = listOf(args.folderId),
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
                    nodeIds = listOf(args.folderId),
                    contactData = emailList,
                    accessPermission = permission,
                )
            }.recover { error ->
                Timber.e(error)
                MoveRequestResult.ShareMovement(
                    count = 0,
                    errorCount = emailList.size,
                    nodes = listOf(args.folderHandle),
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
                    nodeIds = listOf(args.folderId),
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
        fun create(args: Args): ShareRecipientsViewModel
    }

    data class Args(
        val folderHandle: Long,
        val folderName: String,
    ) {
        val folderId: NodeId get() = NodeId(folderHandle)
    }

}

private fun interface StateTransform {
    operator fun invoke(
        state: FileContactListState.Data,
    ): FileContactListState
}