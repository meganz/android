package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.UserSetMapper
import mega.privacy.android.data.mapper.node.FileNodeMapper
import mega.privacy.android.data.mapper.videos.TypedVideoNodeMapper
import mega.privacy.android.data.mapper.videosection.VideoPlaylistMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.repository.VideoSectionRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_DESC
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElementList
import nz.mega.sdk.MegaSetList
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
    private val typedVideoNodeMapper = mock<TypedVideoNodeMapper>()
    private val cancelTokenProvider = mock<CancelTokenProvider>()
    private val megaCancelToken = mock<MegaCancelToken>()
    private val megaLocalRoomGateway = mock<MegaLocalRoomGateway>()
    private val userSetMapper: UserSetMapper = ::createUserSet
    private val videoPlaylistMapper = mock<VideoPlaylistMapper>()

    @BeforeAll
    fun setUp() {
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = VideoSectionRepositoryImpl(
            megaApiGateway = megaApiGateway,
            sortOrderIntMapper = sortOrderIntMapper,
            fileNodeMapper = fileNodeMapper,
            typedVideoNodeMapper = typedVideoNodeMapper,
            cancelTokenProvider = cancelTokenProvider,
            megaLocalRoomGateway = megaLocalRoomGateway,
            userSetMapper = userSetMapper,
            videoPlaylistMapper = videoPlaylistMapper,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiGateway,
            sortOrderIntMapper,
            fileNodeMapper,
            typedVideoNodeMapper,
            megaLocalRoomGateway,
            videoPlaylistMapper
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
        whenever(megaLocalRoomGateway.getAllOfflineInfo()).thenReturn(emptyList())
        whenever(typedVideoNodeMapper(any(), any())).thenReturn(mock())

        val actual = underTest.getAllVideos(SortOrder.ORDER_MODIFICATION_DESC)
        assertThat(actual.isNotEmpty()).isTrue()
        assertThat(actual.size).isEqualTo(2)
    }

    @Test
    fun `test that get video playlists returns correctly when all set types are SET_TYPE_PLAYLIST`() =
        runTest {
            val megaSet1 = createMegaSet(1L)
            val megaSet2 = createMegaSet(2L)

            val megaSetList = mock<MegaSetList> {
                on { size() }.thenReturn(2L)
                on { get(0) }.thenReturn(megaSet1)
                on { get(1) }.thenReturn(megaSet2)
            }

            val megaSetElementList = mock<MegaSetElementList> {
                on { size() }.thenReturn(0L)
            }

            initReturnValues(megaSetList, megaSetElementList)

            val actual = underTest.getVideoPlaylists()
            assertThat(actual.isNotEmpty()).isTrue()
            assertThat(actual.size).isEqualTo(2)
        }

    @Test
    fun `test that get video playlists returns correctly when all set types are not SET_TYPE_PLAYLIST`() =
        runTest {
            val megaSet1 = createMegaSet(1L, MegaSet.SET_TYPE_ALBUM)
            val megaSet2 = createMegaSet(2L, MegaSet.SET_TYPE_ALBUM)

            val megaSetList = mock<MegaSetList> {
                on { size() }.thenReturn(2L)
                on { get(0) }.thenReturn(megaSet1)
                on { get(1) }.thenReturn(megaSet2)
            }

            val megaSetElementList = mock<MegaSetElementList> {
                on { size() }.thenReturn(0L)
            }

            initReturnValues(megaSetList, megaSetElementList)

            val actual = underTest.getVideoPlaylists()
            assertThat(actual.isEmpty()).isTrue()
        }

    @Test
    fun `test that get video playlists returns correctly when all set types are not all SET_TYPE_PLAYLIST`() =
        runTest {
            val megaSet1 = createMegaSet(1L)
            val megaSet2 = createMegaSet(2L, MegaSet.SET_TYPE_ALBUM)

            val megaSetList = mock<MegaSetList> {
                on { size() }.thenReturn(2L)
                on { get(0) }.thenReturn(megaSet1)
                on { get(1) }.thenReturn(megaSet2)
            }

            val megaSetElementList = mock<MegaSetElementList> {
                on { size() }.thenReturn(0L)
            }

            initReturnValues(megaSetList, megaSetElementList)

            val actual = underTest.getVideoPlaylists()
            assertThat(actual.isNotEmpty()).isTrue()
            assertThat(actual.size).isEqualTo(1)
        }

    private suspend fun initReturnValues(
        megaSetList: MegaSetList,
        megaSetElementList: MegaSetElementList,
    ) {
        val megaNode = mock<MegaNode> {
            on { duration }.thenReturn(100)
        }
        val typedVideoNode = mock<TypedVideoNode> {
            on { thumbnailPath }.thenReturn(null)
            on { duration.inWholeSeconds }.thenReturn(100L)
        }
        whenever(megaApiGateway.getSets()).thenReturn(megaSetList)
        whenever(megaApiGateway.getSetElements(any())).thenReturn(megaSetElementList)
        whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(megaNode)
        whenever(typedVideoNodeMapper(any(), any())).thenReturn(typedVideoNode)
    }

    @Test
    fun `test that the created video playlist has the correct title`() = runTest {
        val api = mock<MegaApiJava>()

        val testMegaSet = mock<MegaSet> {
            on { id() }.thenReturn(1L)
            on { name() }.thenReturn("video playlist title")
        }

        val userSet = createUserSet(
            testMegaSet.id(),
            testMegaSet.name(),
            MegaSet.SET_TYPE_PLAYLIST,
            null,
            testMegaSet.cts(),
            testMegaSet.ts(),
            false,
        )

        val expectedVideoPlaylist = mock<VideoPlaylist> {
            on { id }.thenReturn(NodeId(userSet.id))
            on { title }.thenReturn(userSet.name)
        }

        val request = mock<MegaRequest> {
            on { megaSet }.thenReturn(testMegaSet)
        }
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        whenever(megaApiGateway.createSet(any(), any(), any())).thenAnswer {
            (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                api,
                request,
                error
            )
        }
        whenever(videoPlaylistMapper(any(), any())).thenReturn(expectedVideoPlaylist)

        val actual = underTest.createVideoPlaylist(userSet.name)
        assertThat(actual.id.longValue).isEqualTo(userSet.id)
        assertThat(actual.title).isEqualTo(userSet.name)
    }

    private fun createMegaSet(id: Long, type: Int = MegaSet.SET_TYPE_PLAYLIST) = mock<MegaSet> {
        on { id() }.thenReturn(id)
        on { name() }.thenReturn("MegaSet")
        on { type() }.thenReturn(type)
        on { cover() }.thenReturn(-1L)
        on { cts() }.thenReturn(2L)
        on { ts() }.thenReturn(3L)
        on { isExported }.thenReturn(false)
    }

    private fun createUserSet(
        id: Long,
        name: String,
        type: Int,
        cover: Long?,
        creationTime: Long,
        modificationTime: Long,
        isExported: Boolean,
    ): UserSet = object : UserSet {
        override val id: Long = id

        override val name: String = name

        override val type: Int = type

        override val cover: Long? = cover

        override val creationTime: Long = creationTime

        override val modificationTime: Long = modificationTime

        override val isExported: Boolean = isExported

        override fun equals(other: Any?): Boolean {
            val otherSet = other as? UserSet ?: return false
            return id == otherSet.id
                    && name == otherSet.name
                    && cover == otherSet.cover
                    && modificationTime == otherSet.modificationTime
                    && isExported == otherSet.isExported
        }
    }

    @Test
    fun `test that addVideosToPlaylist returns correctly`() =
        runTest {
            val testPlaylistId = NodeId(1L)
            val testVideoIDs = listOf(NodeId(1L), NodeId(2L))

            whenever(megaApiGateway.createSetElement(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    mock {
                        on { errorCode }.thenReturn(MegaError.API_OK)
                    }
                )
            }

            initUnderTest()
            val actual =
                underTest.addVideosToPlaylist(playlistID = testPlaylistId, videoIDs = testVideoIDs)
            assertThat(actual).isEqualTo(testVideoIDs.size)
        }

    @Test
    fun `test that addVideosToPlaylist returns 0 when createSetElement returns a MegaError`() =
        runTest {
            val testPlaylistId = NodeId(1L)
            val testVideoIDs = listOf(NodeId(1L), NodeId(2L))

            whenever(megaApiGateway.createSetElement(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    mock {
                        on { errorCode }.thenReturn(MegaError.API_EBLOCKED)
                    }
                )
            }

            initUnderTest()
            val actual =
                underTest.addVideosToPlaylist(playlistID = testPlaylistId, videoIDs = testVideoIDs)
            assertThat(actual).isEqualTo(0)
        }
}