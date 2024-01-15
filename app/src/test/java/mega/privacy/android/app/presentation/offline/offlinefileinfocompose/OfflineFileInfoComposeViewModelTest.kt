package mega.privacy.android.app.presentation.offline.offlinefileinfocompose

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.file.IsImageFileUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFileTotalSizeUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFolderInformationUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationByIdUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OfflineFileInfoComposeViewModelTest {

    private lateinit var underTest: OfflineFileInfoComposeViewModel
    private val savedStateHandle = mock<SavedStateHandle>()
    private val getOfflineNodeInformationByIdUseCase = mock<GetOfflineNodeInformationByIdUseCase>()
    private val getOfflineFileUseCase = mock<GetOfflineFileUseCase>()
    private val getThumbnailUseCase = mock<GetThumbnailUseCase>()
    private val isImageFileUseCase = mock<IsImageFileUseCase>()
    private val getOfflineFolderInformationUseCase = mock<GetOfflineFolderInformationUseCase>()
    private val getOfflineFileTotalSizeUseCase = mock<GetOfflineFileTotalSizeUseCase>()
    private val removeOfflineNodeUseCase = mock<RemoveOfflineNodeUseCase>()

    @TempDir
    lateinit var temporaryFolder: File

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun initStubCommon() {
        runBlocking {
            stubCommon()
        }
    }

    private suspend fun stubCommon() {
        val offlineFile = File(temporaryFolder, "OfflineFile.jpg")
        offlineFile.createNewFile()
        whenever(savedStateHandle.get<Long>(OfflineFileInfoComposeViewModel.NODE_HANDLE)) doReturn (1)
        whenever(getOfflineFileUseCase(any())) doReturn (offlineFile)
        whenever(getOfflineFileTotalSizeUseCase(any())) doReturn (1000L)
    }

    private fun initUnderTest() {
        underTest = OfflineFileInfoComposeViewModel(
            savedStateHandle = savedStateHandle,
            getOfflineNodeInformationByIdUseCase = getOfflineNodeInformationByIdUseCase,
            getOfflineFileUseCase = getOfflineFileUseCase,
            getThumbnailUseCase = getThumbnailUseCase,
            isImageFileUseCase = isImageFileUseCase,
            getOfflineFolderInformationUseCase = getOfflineFolderInformationUseCase,
            getOfflineFileTotalSizeUseCase = getOfflineFileTotalSizeUseCase,
            removeOfflineNodeUseCase = removeOfflineNodeUseCase
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        initUnderTest()
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.title).isEmpty()
            assertThat(initial.totalSize).isEqualTo(0L)
            assertThat(initial.folderInfo).isNull()
            assertThat(initial.addedTime).isNull()
            assertThat(initial.thumbnail).isNull()
            assertThat(initial.isFolder).isFalse()
        }
    }

    @Test
    fun `test that folderInfo is set when node is a folder`() = runTest {
        val offlineNodeInformation = mock<OtherOfflineNodeInformation> {
            on { id } doReturn 3
            on { isFolder } doReturn true
            on { name } doReturn "title"
            on { lastModifiedTime } doReturn 5679
        }
        val folderInfo = OfflineFolderInfo(0, 2)
        whenever(getOfflineNodeInformationByIdUseCase(NodeId(any()))) doReturn offlineNodeInformation
        whenever(getOfflineFolderInformationUseCase(any())) doReturn (folderInfo)
        whenever(isImageFileUseCase(any())) doReturn (false)

        initUnderTest()
        assertThat(underTest.state.value.folderInfo).isEqualTo(folderInfo)
        assertThat(underTest.state.value.totalSize).isEqualTo(1000L)
    }

    @Test
    fun `test that file is used as thumbnail when node is an image`() = runTest {
        val offlineNodeInformation = mock<OtherOfflineNodeInformation> {
            on { id } doReturn 3
            on { isFolder } doReturn false
            on { name } doReturn "title.jpg"
            on { lastModifiedTime } doReturn 5679
        }
        whenever(getOfflineNodeInformationByIdUseCase(NodeId(any()))) doReturn offlineNodeInformation
        whenever(isImageFileUseCase(any())) doReturn (true)

        initUnderTest()
        assertThat(underTest.state.value.thumbnail).isEqualTo("file:${temporaryFolder.path}/OfflineFile.jpg")
    }

    @Test
    fun `test that thumbnail is set null when node is an image but file doesn't exist`() =
        runTest {
            val offlineNodeInformation = mock<OtherOfflineNodeInformation> {
                on { id } doReturn 3
                on { isFolder } doReturn false
                on { name } doReturn "title.jpg"
                on { lastModifiedTime } doReturn 5679
            }
            whenever(getOfflineNodeInformationByIdUseCase(NodeId(any()))) doReturn offlineNodeInformation
            whenever(getOfflineFileUseCase(any())) doReturn (File(temporaryFolder, "NonExistent"))
            whenever(isImageFileUseCase(any())) doReturn (true)

            initUnderTest()
            assertThat(underTest.state.value.thumbnail).isNull()
        }

    @Test
    fun `test that removeOfflineNodeUseCase is invoked when removeFromOffline is called`() =
        runTest {
            initUnderTest()
            underTest.removeFromOffline()

            verify(removeOfflineNodeUseCase).invoke(NodeId(1))
        }

    @Test
    fun `test that error event is sent when node is null`() = runTest {
        whenever(getOfflineNodeInformationByIdUseCase(NodeId(any()))) doReturn null

        initUnderTest()
        val event = underTest.state.value.errorEvent
        assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
        val content = (event as StateEventWithContentTriggered).content
        assertThat(content).isTrue()
    }

    @AfterEach
    fun resetMocks() {
        reset(
            savedStateHandle,
            getOfflineNodeInformationByIdUseCase,
            getOfflineFileUseCase,
            getThumbnailUseCase,
            isImageFileUseCase,
            getOfflineFolderInformationUseCase,
            getOfflineFileTotalSizeUseCase,
            removeOfflineNodeUseCase
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

}