package mega.privacy.android.domain.usecase.transfers.uploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.file.IsImageFileUseCase
import mega.privacy.android.domain.usecase.file.IsPdfFileUseCase
import mega.privacy.android.domain.usecase.file.IsVideoFileUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreateImageOrVideoPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreateImageOrVideoThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreatePdfPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreatePdfThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPublicNodeThumbnailUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream
import kotlin.test.Test

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetNodeAttributesAfterUploadUseCaseTest {

    private lateinit var underTest: SetNodeAttributesAfterUploadUseCase

    private val createImageOrVideoThumbnailUseCase: CreateImageOrVideoThumbnailUseCase = mock()
    private val createImageOrVideoPreviewUseCase: CreateImageOrVideoPreviewUseCase = mock()
    private val setNodeCoordinatesUseCase: SetNodeCoordinatesUseCase = mock()
    private val isVideoFileUseCase: IsVideoFileUseCase = mock()
    private val isImageFileUseCase: IsImageFileUseCase = mock()
    private val isPdfFileUseCase: IsPdfFileUseCase = mock()
    private val createPdfThumbnailUseCase: CreatePdfThumbnailUseCase = mock()
    private val createPdfPreviewUseCase: CreatePdfPreviewUseCase = mock()
    private val getPublicNodeThumbnailUseCase = mock<GetPublicNodeThumbnailUseCase>()

    private val nodeHandle = 1L
    private val uriPath = UriPath("test/local/testName")

    @BeforeAll
    fun setUp() {
        underTest = SetNodeAttributesAfterUploadUseCase(
            createImageOrVideoThumbnailUseCase = createImageOrVideoThumbnailUseCase,
            createImageOrVideoPreviewUseCase = createImageOrVideoPreviewUseCase,
            setNodeCoordinatesUseCase = setNodeCoordinatesUseCase,
            isVideoFileUseCase = isVideoFileUseCase,
            isImageFileUseCase = isImageFileUseCase,
            isPdfFileUseCase = isPdfFileUseCase,
            createPdfThumbnailUseCase = createPdfThumbnailUseCase,
            createPdfPreviewUseCase = createPdfPreviewUseCase,
            getPublicNodeThumbnailUseCase = getPublicNodeThumbnailUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            createImageOrVideoThumbnailUseCase,
            createImageOrVideoPreviewUseCase,
            setNodeCoordinatesUseCase,
            isVideoFileUseCase,
            isImageFileUseCase,
            isPdfFileUseCase,
            createPdfThumbnailUseCase,
            createPdfPreviewUseCase,
            getPublicNodeThumbnailUseCase,
        )
    }

    @ParameterizedTest(
        name = " is image = {0}, is video = {1} and is pdf = {2}"
    )
    @MethodSource("provideParameters")
    fun `test that SetNodeCoordinatesUseCase behaves correctly if`(
        isVideoFile: Boolean,
        isImageFile: Boolean,
        isPdfFile: Boolean,
    ) = runTest {
        whenever(isVideoFileUseCase(uriPath)).thenReturn(isVideoFile)
        whenever(isImageFileUseCase(uriPath)).thenReturn(isImageFile)
        whenever(isPdfFileUseCase(uriPath)).thenReturn(isPdfFile)
        whenever(createImageOrVideoThumbnailUseCase(nodeHandle, uriPath)).thenReturn(Unit)
        whenever(createPdfThumbnailUseCase(nodeHandle, uriPath)).thenReturn(Unit)
        whenever(createImageOrVideoPreviewUseCase(nodeHandle, uriPath)).thenReturn(Unit)
        whenever(createPdfPreviewUseCase(nodeHandle, uriPath)).thenReturn(Unit)
        whenever(setNodeCoordinatesUseCase(uriPath, nodeHandle)).thenReturn(Unit)

        underTest.invoke(nodeHandle, uriPath, null)

        when {
            isVideoFile || isImageFile -> {
                verify(createImageOrVideoThumbnailUseCase).invoke(nodeHandle, uriPath)
                verify(createPdfThumbnailUseCase, never()).invoke(nodeHandle, uriPath)
                verify(createImageOrVideoPreviewUseCase).invoke(nodeHandle, uriPath)
                verify(createPdfPreviewUseCase, never()).invoke(nodeHandle, uriPath)
                verify(setNodeCoordinatesUseCase).invoke(uriPath, nodeHandle)
            }

            isPdfFile -> {
                verify(createImageOrVideoThumbnailUseCase, never()).invoke(nodeHandle, uriPath)
                verify(createPdfThumbnailUseCase).invoke(nodeHandle, uriPath)
                verify(createImageOrVideoPreviewUseCase, never()).invoke(nodeHandle, uriPath)
                verify(createPdfPreviewUseCase).invoke(nodeHandle, uriPath)
                verify(setNodeCoordinatesUseCase, never()).invoke(uriPath, nodeHandle)
            }

            else -> {
                verify(createImageOrVideoThumbnailUseCase, never()).invoke(nodeHandle, uriPath)
                verify(createPdfThumbnailUseCase, never()).invoke(nodeHandle, uriPath)
                verify(createImageOrVideoPreviewUseCase, never()).invoke(nodeHandle, uriPath)
                verify(createPdfPreviewUseCase, never()).invoke(nodeHandle, uriPath)
                verify(setNodeCoordinatesUseCase, never()).invoke(uriPath, nodeHandle)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that geolocation appdata parameter is set when is an image or video file and app data contains it`(
        isVideoFile: Boolean,
    ) = runTest {
        whenever(isVideoFileUseCase(uriPath)).thenReturn(isVideoFile)
        whenever(isImageFileUseCase(uriPath)).thenReturn(!isVideoFile)
        whenever(isPdfFileUseCase(uriPath)).thenReturn(false)
        val geolocation = TransferAppData.Geolocation(34.354, 45.435)
        val appData = listOf(geolocation)
        underTest.invoke(nodeHandle, uriPath, appData = appData)

        verify(setNodeCoordinatesUseCase).invoke(uriPath, nodeHandle, geolocation)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that thumbnail is not generated if already exists`(
        isVideoFile: Boolean,
    ) = runTest {
        whenever(getPublicNodeThumbnailUseCase(nodeHandle)).thenReturn(mock())
        whenever(isVideoFileUseCase(uriPath)).thenReturn(isVideoFile)
        whenever(isImageFileUseCase(uriPath)).thenReturn(!isVideoFile)
        whenever(isPdfFileUseCase(uriPath)).thenReturn(false)
        underTest.invoke(nodeHandle, uriPath, appData = null)
        verifyNoInteractions(createImageOrVideoThumbnailUseCase)
        verifyNoInteractions(createImageOrVideoPreviewUseCase)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(false, false, false),
        Arguments.of(true, false, false),
        Arguments.of(false, true, false),
        Arguments.of(false, false, true),
    )
}