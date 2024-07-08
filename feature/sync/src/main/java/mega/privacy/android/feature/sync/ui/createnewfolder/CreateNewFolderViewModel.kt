package mega.privacy.android.feature.sync.ui.createnewfolder

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.feature.sync.ui.createnewfolder.model.CreateNewFolderState
import mega.privacy.android.shared.resources.R as sharedR
import androidx.lifecycle.viewModelScope
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.usecase.node.CheckForValidNameUseCase
import mega.privacy.android.domain.usecase.node.ValidNameType
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
            when (checkForValidNameUseCase.newFolderCreation(
                newName = newFolderName,
                node = parentNode
            )) {
                ValidNameType.BLANK_NAME -> {
                    _state.update { it.copy(errorMessage = sharedR.string.create_new_folder_dialog_error_message_empty_folder_name) }
                }

                ValidNameType.INVALID_NAME -> {
                    _state.update { it.copy(errorMessage = sharedR.string.general_invalid_characters_defined) }
                }

                ValidNameType.NAME_ALREADY_EXISTS -> {
                    _state.update { it.copy(errorMessage = sharedR.string.create_new_folder_dialog_error_existing_folder) }
                }

                else -> {
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