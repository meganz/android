package mega.privacy.android.feature.sync.ui.createnewfolder

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.message.NodeNameErrorMessageMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.InvalidNameType
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.CheckForValidNameUseCase
import mega.privacy.android.shared.resources.R as sharedR
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
internal class CreateNewFolderViewModelTest {

    private lateinit var underTest: CreateNewFolderViewModel

    private val checkForValidNameUseCase = mock<CheckForValidNameUseCase>()
    private val nodeNameErrorMessageMapper = mock<NodeNameErrorMessageMapper>()

    private val currentFolderMock = mock<FolderNode> {
        whenever(it.id).thenReturn(NodeId(1234L))
    }

    @BeforeAll
    fun setUp() {
        underTest = CreateNewFolderViewModel(
            checkForValidNameUseCase = checkForValidNameUseCase,
            nodeNameErrorMessageMapper = nodeNameErrorMessageMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(checkForValidNameUseCase, nodeNameErrorMessageMapper)
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
                checkForValidNameUseCase(
                    newName = newFolderName,
                    node = currentFolderMock
                )
            ).thenReturn(InvalidNameType.BLANK_NAME)
            whenever(nodeNameErrorMessageMapper(InvalidNameType.BLANK_NAME, true))
                .thenReturn(sharedR.string.create_new_folder_dialog_error_message_empty_folder_name)

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
            checkForValidNameUseCase(
                newName = newFolderName,
                node = currentFolderMock
            )
        ).thenReturn(InvalidNameType.INVALID_NAME)
        whenever(nodeNameErrorMessageMapper(InvalidNameType.INVALID_NAME, true))
            .thenReturn(sharedR.string.general_invalid_characters_defined)

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
                checkForValidNameUseCase(
                    newName = newFolderName,
                    node = currentFolderMock
                )
            ).thenReturn(InvalidNameType.NAME_ALREADY_EXISTS)
            whenever(nodeNameErrorMessageMapper(InvalidNameType.NAME_ALREADY_EXISTS, true))
                .thenReturn(sharedR.string.create_new_folder_dialog_error_existing_folder)

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
