package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.FileNodeMapper
import mega.privacy.android.data.mapper.videos.VideoNodeMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.VideoSectionRepository
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_DESC
import nz.mega.sdk.MegaCancelToken
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoSectionRepositoryImplTest {
    private lateinit var underTest: VideoSectionRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val sortOrderIntMapper = mock<SortOrderIntMapper>()
    private val fileNodeMapper = mock<FileNodeMapper>()
    private val cacheGateway = mock<CacheGateway>()
    private val videoNodeMapper = mock<VideoNodeMapper>()
    private val cancelTokenProvider = mock<CancelTokenProvider>()
    private val megaCancelToken = mock<MegaCancelToken>()

    @BeforeAll
    fun setUp() {
        underTest = VideoSectionRepositoryImpl(
            megaApiGateway = megaApiGateway,
            sortOrderIntMapper = sortOrderIntMapper,
            fileNodeMapper = fileNodeMapper,
            cacheGateway = cacheGateway,
            videoNodeMapper = videoNodeMapper,
            cancelTokenProvider = cancelTokenProvider,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiGateway,
            sortOrderIntMapper,
            fileNodeMapper,
            cacheGateway,
            videoNodeMapper
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that get all videos returns successfully`() = runTest {
        whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(megaCancelToken)

        whenever(sortOrderIntMapper(SortOrder.ORDER_MODIFICATION_DESC))
            .thenReturn(ORDER_DEFAULT_DESC)

        whenever(megaApiGateway.searchByType(any(), any(), any(), any()))
            .thenReturn(listOf(mock(), mock()))

        whenever(cacheGateway.getOrCreateCacheFolder(CacheFolderConstant.THUMBNAIL_FOLDER))
            .thenReturn(mock())

        whenever(videoNodeMapper(any(), any())).thenReturn(mock())

        val actual = underTest.getAllVideos(SortOrder.ORDER_MODIFICATION_DESC)
        assertThat(actual.isNotEmpty()).isTrue()
        assertThat(actual.size).isEqualTo(2)
    }
}