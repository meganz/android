package mega.privacy.android.feature.sync.ui.createnewfolder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.message.NodeNameErrorMessageMapper
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.usecase.node.CheckForValidNameUseCase
import mega.privacy.android.feature.sync.ui.createnewfolder.model.CreateNewFolderState
import timber.log.Timber
import javax.inject.Inject

/**
 * [ViewModel] containing all functionalities for [CreateNewFolderDialog]
 *
 * @param checkForValidNameUseCase  [CheckForValidNameUseCase]
 */
@HiltViewModel
internal class CreateNewFolderViewModel @Inject constructor(
    private val checkForValidNameUseCase: CheckForValidNameUseCase,
    private val nodeNameErrorMessageMapper: NodeNameErrorMessageMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateNewFolderState())

    /**
     * The State of [CreateNewFolderDialog]
     */
    val state: StateFlow<CreateNewFolderState> = _state.asStateFlow()

    /**
     * Checks if the name for the new folder is valid and sets the proper error
     *
     * @param newFolderName The new folder name
     * @param parentNode    Parent node under which the folder should be created
     * @return              True if the name is valid or False otherwise
     */
    fun checkIsValidName(
        newFolderName: String,
        parentNode: Node,
    ) = viewModelScope.launch {
        runCatching {
            checkForValidNameUseCase(newName = newFolderName, node = parentNode)
                .let { invalidNameType ->
                    nodeNameErrorMessageMapper(invalidNameType, true)?.let {
                        _state.update { state -> state.copy(errorMessage = it) }
                    } ?: run {
                        _state.update {
                            it.copy(
                                errorMessage = null,
                                validNameConfirmed = triggered(newFolderName)
                            )
                        }
                    }
                }
        }.onFailure { Timber.e(it) }
    }

    /**
     * Clears the Error Message from the Dialog
     */
    fun clearErrorMessage() = _state.update { it.copy(errorMessage = null) }

    /**
     * Notifies [CreateNewFolderState.validNameConfirmed] that it has been consumed
     */
    fun resetValidNameConfirmedEvent() = _state.update { it.copy(validNameConfirmed = consumed()) }
}