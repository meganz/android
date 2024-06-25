package mega.privacy.android.domain.usecase.offline


import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.file.GetFileTypeInfoUseCase
import mega.privacy.android.domain.usecase.file.IsImageFileUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetOfflineFileInformationUseCaseTest {

    private lateinit var underTest: GetOfflineFileInformationUseCase
    private val getOfflineFileUseCase = mock<GetOfflineFileUseCase>()
    private val getThumbnailUseCase = mock<GetThumbnailUseCase>()
    private val getOfflineFolderInformationUseCase = mock<GetOfflineFolderInformationUseCase>()
    private val getOfflineFileTotalSizeUseCase = mock<GetOfflineFileTotalSizeUseCase>()
    private val isImageFileUseCase = mock<IsImageFileUseCase>()
    private val getFileTypeInfoUseCase = mock<GetFileTypeInfoUseCase>()

    @TempDir
    lateinit var temporaryFolder: File

    @BeforeAll
    fun setUp() {
        underTest = GetOfflineFileInformationUseCase(
            getOfflineFileUseCase,
            getThumbnailUseCase,
            getOfflineFolderInformationUseCase,
            getOfflineFileTotalSizeUseCase,
            isImageFileUseCase,
            getFileTypeInfoUseCase
        )
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
        whenever(getOfflineFileUseCase(any())) doReturn (offlineFile)
        whenever(getOfflineFileTotalSizeUseCase(any())) doReturn (1000L)
        whenever(getFileTypeInfoUseCase(any())) doReturn mock<UnknownFileTypeInfo>()
    }

    @Test
    fun `test that folderInfo is set when node is a folder`() = runTest {
        val offlineNodeInformation = mock<OtherOfflineNodeInformation> {
            on { id } doReturn 3
            on { isFolder } doReturn true
            on { name } doReturn "title"
            on { lastModifiedTime } doReturn 5679
            on { handle } doReturn "3"
            on { path } doReturn "path"
        }
        val folderInfo = OfflineFolderInfo(0, 2)
        whenever(getOfflineFolderInformationUseCase(any())) doReturn (folderInfo)

        val result = underTest(offlineNodeInformation)

        verifyNoInteractions(getFileTypeInfoUseCase)
        assertThat(result.fileTypeInfo).isNull()
        assertThat(result.folderInfo).isEqualTo(folderInfo)
        assertThat(result.totalSize).isEqualTo(1000L)
    }

    @Test
    fun `test that fileTypeInfo is set when node is a file`() =
        runTest {
            val offlineNodeInformation = mock<OtherOfflineNodeInformation> {
                on { id } doReturn 3
                on { isFolder } doReturn false
                on { name } doReturn "title.pdf"
                on { lastModifiedTime } doReturn 5679
                on { handle } doReturn "3"
                on { path } doReturn "path"
            }
            whenever(getFileTypeInfoUseCase(any())) doReturn PdfFileTypeInfo

            val result = underTest(offlineNodeInformation)

            assertThat(result.fileTypeInfo).isEqualTo(PdfFileTypeInfo)
        }

    @Test
    fun `test that thumbnail is set null when node is a folder`() =
        runTest {
            val offlineNodeInformation = mock<OtherOfflineNodeInformation> {
                on { id } doReturn 3
                on { isFolder } doReturn true
                on { name } doReturn "title.jpg"
                on { lastModifiedTime } doReturn 5679
                on { handle } doReturn "3"
                on { path } doReturn "path"
            }
            whenever(getOfflineFileUseCase(any())) doReturn (File(temporaryFolder, "NonExistent"))

            val result = underTest(offlineNodeInformation)

            assertThat(result.thumbnail).isNull()
        }

    @Test
    fun `test that file is used as thumbnail when node is an image`() = runTest {
        val offlineNodeInformation = mock<OtherOfflineNodeInformation> {
            on { id } doReturn 3
            on { isFolder } doReturn false
            on { name } doReturn "title.jpg"
            on { lastModifiedTime } doReturn 5679
            on { handle } doReturn "3"
            on { path } doReturn "path"
        }
        whenever(isImageFileUseCase(any())) doReturn (true)

        val result = underTest(offlineNodeInformation, true)

        assertThat(result.thumbnail).isEqualTo("file:${temporaryFolder.path}/OfflineFile.jpg")
    }

    @Test
    fun `test that thumbnail is set null when node is an image but file doesn't exist`() =
        runTest {
            val offlineNodeInformation = mock<OtherOfflineNodeInformation> {
                on { id } doReturn 3
                on { isFolder } doReturn false
                on { name } doReturn "title.jpg"
                on { lastModifiedTime } doReturn 5679
                on { handle } doReturn "3"
                on { path } doReturn "path"
            }
            whenever(getOfflineFileUseCase(any())) doReturn (File(temporaryFolder, "NonExistent"))
            whenever(isImageFileUseCase(any())) doReturn (true)

            val result = underTest(offlineNodeInformation, true)

            assertThat(result.thumbnail).isNull()
        }

    @AfterEach
    fun resetMocks() {
        reset(
            getOfflineFileUseCase,
            getThumbnailUseCase,
            getOfflineFolderInformationUseCase,
            getOfflineFileTotalSizeUseCase,
            isImageFileUseCase,
            getFileTypeInfoUseCase
        )
    }
}