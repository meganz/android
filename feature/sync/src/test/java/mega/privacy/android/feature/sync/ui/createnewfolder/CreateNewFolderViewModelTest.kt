package mega.privacy.android.feature.sync.ui.createnewfolder

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.CheckForValidNameUseCase
import mega.privacy.android.shared.resources.R as sharedR
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.node.ValidNameType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test Class for [CreateNewFolderViewModel]
 */
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
class CreateNewFolderViewModelTest {

    private lateinit var underTest: CreateNewFolderViewModel

    private val checkForValidNameUseCase = mock<CheckForValidNameUseCase>()

    private val currentFolderMock = mock<FolderNode> {
        whenever(it.id).thenReturn(NodeId(1234L))
    }

    @BeforeAll
    fun setUp() {
        underTest = CreateNewFolderViewModel(checkForValidNameUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        reset(checkForValidNameUseCase)
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        underTest.state.test {
            val initialState = awaitItem()
            assertThat(initialState.errorMessage).isNull()
        }
    }

    @Test
    fun `test that the error message is cleared`() = runTest {
        underTest.clearErrorMessage()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isNull()
        }
    }

    @Test
    fun `test that create folder fails when the new folder name is empty`() =
        runTest {
            val newFolderName = ""

            whenever(
                checkForValidNameUseCase.newFolderCreation(
                    newName = newFolderName,
                    node = currentFolderMock
                )
            ).thenReturn(
                ValidNameType.BLANK_NAME
            )

            underTest.checkIsValidName(
                newFolderName = newFolderName,
                parentNode = currentFolderMock,
            )
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.errorMessage).isEqualTo(sharedR.string.create_new_folder_dialog_error_message_empty_folder_name)
            }
        }

    @ParameterizedTest(name = "New folder name: {0}")
    @ValueSource(
        strings = [
            "Folder\"", "Folder*", "Folder/", "Folder:", "Folder<", "Folder>", "Folder?",
            "Folder\\", "Folder|",
        ]
    )
    fun `test that create folder fails when the new folder name contains invalid characters`(
        newFolderName: String,
    ) = runTest {
        whenever(
            checkForValidNameUseCase.newFolderCreation(
                newName = newFolderName,
                node = currentFolderMock
            )
        ).thenReturn(
            ValidNameType.INVALID_NAME
        )

        underTest.checkIsValidName(
            newFolderName = newFolderName,
            parentNode = currentFolderMock,
        )
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isEqualTo(sharedR.string.general_invalid_characters_defined)
        }
    }

    @Test
    fun `test that create folder fails when the new name folder name already exists in parent folder`() =
        runTest {
            val newFolderName = "Folder"

            whenever(
                checkForValidNameUseCase.newFolderCreation(
                    newName = newFolderName,
                    node = currentFolderMock
                )
            ).thenReturn(
                ValidNameType.NAME_ALREADY_EXISTS
            )

            underTest.checkIsValidName(
                newFolderName = newFolderName,
                parentNode = currentFolderMock,
            )
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.errorMessage).isEqualTo(sharedR.string.create_new_folder_dialog_error_existing_folder)
            }
        }
}