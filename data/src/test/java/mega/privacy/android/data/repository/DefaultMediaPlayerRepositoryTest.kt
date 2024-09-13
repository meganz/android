package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.gateway.preferences.MediaPlayerPreferencesGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.audios.TypedAudioNodeMapper
import mega.privacy.android.data.mapper.mediaplayer.RepeatToggleModeMapper
import mega.privacy.android.data.mapper.mediaplayer.SubtitleFileInfoMapper
import mega.privacy.android.data.mapper.node.FileNodeMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.data.mapper.videos.TypedVideoNodeMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.repository.MediaPlayerRepository
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSearchFilter
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultMediaPlayerRepositoryTest {
    private lateinit var underTest: MediaPlayerRepository

    private val megaApi = mock<MegaApiGateway>()
    private val megaApiFolder = mock<MegaApiFolderGateway>()
    private val dbHandler = mock<DatabaseHandler>()
    private val fileGateway = mock<FileGateway>()
    private val sortOrderIntMapper = mock<SortOrderIntMapper>()
    private val appPreferencesGateway = mock<AppPreferencesGateway>()
    private val subtitleFileInfoMapper = mock<SubtitleFileInfoMapper>()
    private val mediaPlayerPreferencesGateway = mock<MediaPlayerPreferencesGateway>()
    private val megaLocalRoomGateway = mock<MegaLocalRoomGateway>()
    private val fileNodeMapper = mock<FileNodeMapper>()
    private val typedAudioNodeMapper = mock<TypedAudioNodeMapper>()
    private val typedVideoNodeMapper = mock<TypedVideoNodeMapper>()
    private val cancelTokenProvider = mock<CancelTokenProvider>()
    private val searchFilterMapper = mock<MegaSearchFilterMapper>()

    private val expectedHandle = 100L
    private val expectedMediaId: Long = 1234567
    private val expectedTotalDuration: Long = 200000
    private val expectedCurrentPosition: Long = 16000

    @BeforeAll
    fun initialise() {
        underTest = DefaultMediaPlayerRepository(
            megaApi = megaApi,
            megaApiFolder = megaApiFolder,
            megaLocalRoomGateway = megaLocalRoomGateway,
            dbHandler = { dbHandler },
            fileNodeMapper = fileNodeMapper,
            typedAudioNodeMapper = typedAudioNodeMapper,
            typedVideoNodeMapper = typedVideoNodeMapper,
            fileGateway = fileGateway,
            sortOrderIntMapper = sortOrderIntMapper,
            appPreferencesGateway = appPreferencesGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            subtitleFileInfoMapper = subtitleFileInfoMapper,
            mediaPlayerPreferencesGateway = mediaPlayerPreferencesGateway,
            repeatToggleModeMapper = RepeatToggleModeMapper(),
            searchFilterMapper = searchFilterMapper,
            cancelTokenProvider = cancelTokenProvider
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApi,
            megaApiFolder,
            dbHandler,
            fileGateway,
            sortOrderIntMapper,
            appPreferencesGateway,
            subtitleFileInfoMapper,
            mediaPlayerPreferencesGateway,
            fileNodeMapper,
            typedAudioNodeMapper,
            typedVideoNodeMapper,
            megaLocalRoomGateway
        )
    }

    @Test
    fun `test that get local link for folder link using MegaApi`() = runTest {
        val node = mock<MegaNode>()
        val expectedLocalLink = "local link"
        whenever(megaApiFolder.getMegaNodeByHandle(expectedHandle)).thenReturn(node)
        whenever(megaApiFolder.authorizeNode(node)).thenReturn(node)
        whenever(megaApi.httpServerGetLocalLink(any())).thenReturn(expectedLocalLink)

        val actual = underTest.getLocalLinkForFolderLinkFromMegaApi(expectedHandle)

        assertThat(actual).isEqualTo(expectedLocalLink)
    }

    @Test
    fun `test that get local link for folder link using MegaApiFolder`() = runTest {
        val node = mock<MegaNode>()
        val expectedLocalLink = "local link"
        whenever(megaApiFolder.getMegaNodeByHandle(expectedHandle)).thenReturn(node)
        whenever(megaApiFolder.authorizeNode(node)).thenReturn(node)
        whenever(megaApiFolder.httpServerGetLocalLink(any())).thenReturn(expectedLocalLink)

        val actual = underTest.getLocalLinkForFolderLinkFromMegaApiFolder(expectedHandle)

        assertThat(actual).isEqualTo(expectedLocalLink)
    }

    @Test
    fun `test that get local link from mega api`() = runTest {
        val expectedLocalLink = "local link"
        whenever(megaApi.getMegaNodeByHandle(expectedHandle)).thenReturn(mock())
        whenever(megaApi.httpServerGetLocalLink(any())).thenReturn(expectedLocalLink)

        val actual = underTest.getLocalLinkFromMegaApi(expectedHandle)

        assertThat(actual).isEqualTo(expectedLocalLink)
    }

    @Test
    fun `test that updatePlayback information that there is no local data`() = runTest {
        val expectedPlaybackInfo = createPlaybackInformation()

        underTest.updatePlaybackInformation(expectedPlaybackInfo)
        whenever(
            appPreferencesGateway.monitorString(
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(flowOf("{}"))
        val actual = underTest.monitorPlaybackTimes().firstOrNull()

        assertThat(actual?.get(expectedMediaId)?.mediaId).isEqualTo(expectedMediaId)
        assertThat(actual?.get(expectedMediaId)?.totalDuration).isEqualTo(expectedTotalDuration)
        assertThat(actual?.get(expectedMediaId)?.currentPosition).isEqualTo(expectedCurrentPosition)
    }

    @Test
    fun `test that monitorPlaybackTimes`() = runTest {
        val expectedPlaybackInfo = createPlaybackInformation()

        whenever(
            appPreferencesGateway.monitorString(
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(
            flowOf(
                Gson().toJson(
                    mapOf(
                        Pair(
                            expectedMediaId,
                            expectedPlaybackInfo
                        )
                    )
                )
            )
        )
        val actual = underTest.monitorPlaybackTimes().firstOrNull()

        assertThat(actual?.get(expectedMediaId)?.mediaId).isEqualTo(expectedMediaId)
        assertThat(actual?.get(expectedMediaId)?.totalDuration).isEqualTo(expectedTotalDuration)
        assertThat(actual?.get(expectedMediaId)?.currentPosition).isEqualTo(expectedCurrentPosition)
    }

    @Test
    fun `test that deletePlaybackInformation that playbackInfoMap doesn't include deleted item even local data includes it`() =
        runTest {
            val expectedPlaybackInfo = createPlaybackInformation()
            val expectedDeleteMediaId: Long = 7654321
            val expectedDeleteTotalDuration: Long = 300000
            val expectedDeleteCurrentPosition: Long = 20000
            val expectedDeletePlaybackInfo = PlaybackInformation(
                expectedDeleteMediaId,
                expectedDeleteTotalDuration,
                expectedDeleteCurrentPosition
            )

            val expectedPlaybackInfoMap = mapOf(
                Pair(expectedMediaId, expectedPlaybackInfo),
                Pair(expectedDeleteMediaId, expectedDeletePlaybackInfo)
            )

            underTest.updatePlaybackInformation(expectedPlaybackInfo)
            underTest.updatePlaybackInformation(expectedDeletePlaybackInfo)
            underTest.deletePlaybackInformation(expectedDeleteMediaId)

            whenever(appPreferencesGateway.monitorString(anyOrNull(), anyOrNull())).thenReturn(
                flowOf(
                    Gson().toJson(expectedPlaybackInfoMap)
                )
            )
            val actual = underTest.monitorPlaybackTimes().firstOrNull()

            assertThat(actual?.containsKey(expectedDeleteMediaId)).isFalse()
        }

    @Test
    fun `test that clearPlaybackInformation function is invoked as expected`() =
        runTest {
            underTest.clearPlaybackInformation()
            verify(appPreferencesGateway).putString(
                "PREFERENCE_KEY_VIDEO_EXIT_TIME",
                Json.encodeToString(emptyMap<Long, PlaybackInformation>())
            )
        }

    @ParameterizedTest(name = "when audio repeatMode is {0}, the result of monitorAudioRepeatMode is {1}")
    @MethodSource("provideRepeatModeParameters")
    fun `test that the result of monitorAudioRepeatMode functions are correct`(
        repeatMode: Int,
        repeatToggleMode: RepeatToggleMode,
    ) =
        runTest {
            whenever(mediaPlayerPreferencesGateway.monitorAudioRepeatMode()).thenReturn(
                flowOf(
                    repeatMode
                )
            )
            assertThat(
                underTest.monitorAudioRepeatMode().firstOrNull()
            ).isEqualTo(repeatToggleMode)
        }

    @ParameterizedTest(name = "when video repeatMode is {0}, the result of monitorVideoRepeatMode is {1}")
    @MethodSource("provideRepeatModeParameters")
    fun `test that the result of monitorVideoRepeatMode functions are correct`(
        repeatMode: Int,
        repeatToggleMode: RepeatToggleMode,
    ) = runTest {
        whenever(mediaPlayerPreferencesGateway.monitorVideoRepeatMode()).thenReturn(
            flowOf(
                repeatMode
            )
        )
        assertThat(
            underTest.monitorVideoRepeatMode().firstOrNull()
        ).isEqualTo(repeatToggleMode)
    }

    @Test
    fun `test that getAudioNodes returns list of audio nodes`() = runTest {
        setUpMocksForGettingVideOrAudioNode(isVideo = false)
        val actual = underTest.getAudioNodes(SortOrder.ORDER_DEFAULT_DESC)

        assertThat(actual).isNotEmpty()
        assertThat(actual[0].id.longValue).isEqualTo(expectedHandle)
    }

    @Test
    fun `test that getVideoNodes returns list of video nodes`() = runTest {
        setUpMocksForGettingVideOrAudioNode(isVideo = true)
        val actual = underTest.getVideoNodes(SortOrder.ORDER_DEFAULT_DESC)

        assertThat(actual).isNotEmpty()
        assertThat(actual[0].id.longValue).isEqualTo(expectedHandle)
    }

    @Test
    fun `test that getAudioNodesByParentHandle returns list of audio nodes`() = runTest {
        setUpMocksForGettingVideOrAudioNode(
            isVideo = false,
            parentId = NodeId(expectedHandle)
        )
        val actual =
            underTest.getAudioNodesByParentHandle(expectedHandle, SortOrder.ORDER_DEFAULT_DESC)

        assertThat(actual).isNotEmpty()
        assertThat(actual[0].id.longValue).isEqualTo(expectedHandle)
    }

    @Test
    fun `test that getVideoNodesByParentHandle by parent handle returns list of video nodes`() =
        runTest {
            setUpMocksForGettingVideOrAudioNode(
                isVideo = true,
                parentId = NodeId(expectedHandle)
            )
            val actual =
                underTest.getVideoNodesByParentHandle(expectedHandle, SortOrder.ORDER_DEFAULT_DESC)

            assertThat(actual).isNotEmpty()
            assertThat(actual[0].id.longValue).isEqualTo(expectedHandle)
        }

    @Test
    fun `test that getAudiosByParentHandleFromMegaApiFolder returns list of audio nodes`() =
        runTest {
            setUpMocksForGettingVideOrAudioNode(
                isVideo = false,
                parentId = NodeId(expectedHandle)
            )
            val actual =
                underTest.getAudiosByParentHandleFromMegaApiFolder(
                    expectedHandle,
                    SortOrder.ORDER_DEFAULT_DESC
                )

            assertThat(actual).isNotEmpty()
            assertThat(actual[0].id.longValue).isEqualTo(expectedHandle)
        }

    @Test
    fun `test that getVideosByParentHandleFromMegaApiFolder by parent handle returns list of video nodes`() =
        runTest {
            setUpMocksForGettingVideOrAudioNode(
                isVideo = true,
                parentId = NodeId(expectedHandle)
            )
            val actual =
                underTest.getVideosByParentHandleFromMegaApiFolder(
                    expectedHandle,
                    SortOrder.ORDER_DEFAULT_DESC
                )

            assertThat(actual).isNotEmpty()
            assertThat(actual[0].id.longValue).isEqualTo(expectedHandle)
        }

    @Test
    fun `test that getAudioNodesFromPublicLinks returns list of audio nodes`() =
        runTest {
            setUpMocksForGettingVideOrAudioNode(
                isVideo = false,
                target = SearchTarget.LINKS_SHARE,
            )
            val actual = underTest.getAudioNodesFromPublicLinks(SortOrder.ORDER_DEFAULT_DESC)
            assertThat(actual).isNotEmpty()
            assertThat(actual[0].id.longValue).isEqualTo(expectedHandle)
        }

    @Test
    fun `test that getVideoNodesFromPublicLinks returns list of video nodes`() =
        runTest {
            setUpMocksForGettingVideOrAudioNode(
                isVideo = true,
                target = SearchTarget.LINKS_SHARE,
            )
            val actual = underTest.getVideoNodesFromPublicLinks(SortOrder.ORDER_DEFAULT_DESC)

            assertThat(actual).isNotEmpty()
            assertThat(actual[0].id.longValue).isEqualTo(expectedHandle)
        }

    private suspend fun setUpMocksForGettingVideOrAudioNode(
        isVideo: Boolean,
        target: SearchTarget = SearchTarget.ROOT_NODES,
        parentId: NodeId? = null,
    ) {
        val query = ""
        val searchCategory = if (isVideo) SearchCategory.VIDEO else SearchCategory.AUDIO
        val token = mock<MegaCancelToken>()
        val filter = mock<MegaSearchFilter>()
        val megaNode = mock<MegaNode> {
            on { handle } doReturn expectedHandle
            on { duration } doReturn 100
        }
        val fileNode = mock<FileNode>()
        val typedVideoNode = mock<TypedVideoNode> {
            on { id } doReturn NodeId(expectedHandle)
        }
        val typedAudioNode = mock<TypedAudioNode> {
            on { id } doReturn NodeId(expectedHandle)
        }
        val expectedNodes = listOf(megaNode)
        whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
        whenever(
            searchFilterMapper(
                searchQuery = query,
                parentHandle = parentId,
                searchTarget = target,
                searchCategory = searchCategory
            )
        ).thenReturn(filter)
        whenever(
            megaApi.searchWithFilter(
                filter,
                sortOrderIntMapper(SortOrder.ORDER_DEFAULT_DESC),
                token
            )
        ).thenReturn(expectedNodes)
        whenever(
            megaApi.getChildren(
                filter,
                sortOrderIntMapper(SortOrder.ORDER_DEFAULT_DESC),
                token
            )
        ).thenReturn(expectedNodes)
        whenever(
            megaApiFolder.search(
                filter,
                sortOrderIntMapper(SortOrder.ORDER_DEFAULT_DESC),
                token
            )
        ).thenReturn(expectedNodes)
        whenever(
            megaApiFolder.getChildren(
                filter,
                sortOrderIntMapper(SortOrder.ORDER_DEFAULT_DESC),
                token
            )
        ).thenReturn(expectedNodes)
        whenever(fileNodeMapper(megaNode, false, null)).thenReturn(fileNode)
        whenever(typedVideoNodeMapper(fileNode, 100)).thenReturn(typedVideoNode)
        whenever(typedAudioNodeMapper(fileNode, 100)).thenReturn(typedAudioNode)
    }

    @Test
    fun `test that getAudioNodesFromInShares returns list of audio nodes`() =
        runTest {
            setUpMocksForGettingVideOrAudioNode(
                isVideo = false,
                target = SearchTarget.INCOMING_SHARE
            )
            val actual = underTest.getAudioNodesFromInShares(SortOrder.ORDER_DEFAULT_DESC)
            assertThat(actual).isNotEmpty()
            assertThat(actual[0].id.longValue).isEqualTo(expectedHandle)
        }

    @Test
    fun `test that getVideoNodesFromInShares returns list of video nodes`() =
        runTest {
            setUpMocksForGettingVideOrAudioNode(
                isVideo = true,
                target = SearchTarget.INCOMING_SHARE
            )
            val actual = underTest.getVideoNodesFromInShares(SortOrder.ORDER_DEFAULT_DESC)

            assertThat(actual).isNotEmpty()
            assertThat(actual[0].id.longValue).isEqualTo(expectedHandle)
        }

    @Test
    fun `test that getAudioNodesFromOutShares returns list of audio nodes`() =
        runTest {
            setUpMocksForGettingVideOrAudioNode(
                isVideo = false,
                parentId = NodeId(expectedHandle),
                target = SearchTarget.OUTGOING_SHARE
            )
            val actual =
                underTest.getAudioNodesFromOutShares(expectedHandle, SortOrder.ORDER_DEFAULT_DESC)
            assertThat(actual).isNotEmpty()
            assertThat(actual[0].id.longValue).isEqualTo(expectedHandle)
        }

    @Test
    fun `test that getVideoNodesFromOutShares returns list of video nodes`() =
        runTest {
            setUpMocksForGettingVideOrAudioNode(
                isVideo = true,
                parentId = NodeId(expectedHandle),
                target = SearchTarget.OUTGOING_SHARE
            )
            val actual =
                underTest.getVideoNodesFromOutShares(expectedHandle, SortOrder.ORDER_DEFAULT_DESC)

            assertThat(actual).isNotEmpty()
            assertThat(actual[0].id.longValue).isEqualTo(expectedHandle)
        }

    @Test
    fun `test that megaApiHttpServerStop invokes repository function`() = runTest {
        underTest.megaApiHttpServerStop()
        verify(megaApi).httpServerStop()
    }

    @Test
    fun `test that megaApiFolderHttpServerStop invokes repository function`() = runTest {
        underTest.megaApiFolderHttpServerStop()
        verify(megaApiFolder).httpServerStop()
    }

    @Test
    fun `test that megaApiHttpServerStart invokes repository function`() = runTest {
        underTest.megaApiHttpServerStart()
        verify(megaApi).httpServerStart()
    }

    @Test
    fun `test that megaApiFolderHttpServerStart invokes repository function`() = runTest {
        underTest.megaApiFolderHttpServerStart()
        verify(megaApiFolder).httpServerStart()
    }

    @Test
    fun `test that megaApiHttpServerIsRunning invokes repository function`() = runTest {
        underTest.megaApiHttpServerIsRunning()
        verify(megaApi).httpServerIsRunning()
    }

    @Test
    fun `test that megaApiFolderHttpServerIsRunning invokes repository function`() = runTest {
        underTest.megaApiFolderHttpServerIsRunning()
        verify(megaApiFolder).httpServerIsRunning()
    }

    @Test
    fun `test that megaApiHttpServerSetMaxBufferSize invokes repository function`() = runTest {
        val bufferSize = 100
        underTest.megaApiHttpServerSetMaxBufferSize(bufferSize)
        verify(megaApi).httpServerSetMaxBufferSize(bufferSize)
    }

    @Test
    fun `test that megaApiFolderHttpServerSetMaxBufferSize invokes repository function`() =
        runTest {
            val bufferSize = 100
            underTest.megaApiFolderHttpServerSetMaxBufferSize(bufferSize)
            verify(megaApiFolder).httpServerSetMaxBufferSize(bufferSize)
        }


    /**
     * Provides parameters for the test that the result of monitorAudioRepeatMode
     *
     * The parameters:
     * 1. Int value of repeat mode
     * 2. RepeatToggleMode
     */
    private fun provideRepeatModeParameters() = Stream.of(
        Arguments.of(RepeatToggleMode.REPEAT_ALL.ordinal, RepeatToggleMode.REPEAT_ALL),
        Arguments.of(RepeatToggleMode.REPEAT_NONE.ordinal, RepeatToggleMode.REPEAT_NONE),
        Arguments.of(RepeatToggleMode.REPEAT_ONE.ordinal, RepeatToggleMode.REPEAT_ONE),
    )

    private fun createPlaybackInformation() = PlaybackInformation(
        mediaId = expectedMediaId,
        totalDuration = expectedTotalDuration,
        currentPosition = expectedCurrentPosition
    )
}
