package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.mediaplayer.SubtitleFileInfoMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.repository.MediaPlayerRepository
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMediaPlayerRepositoryTest {
    private lateinit var underTest: MediaPlayerRepository

    private val megaApi = mock<MegaApiGateway>()
    private val megaApiFolder = mock<MegaApiFolderGateway>()
    private val dbHandler = mock<DatabaseHandler>()
    private val fileGateway = mock<FileGateway>()
    private val sortOrderIntMapper = mock<SortOrderIntMapper>()
    private val appPreferencesGateway = mock<AppPreferencesGateway>()
    private val subtitleFileInfoMapper = mock<SubtitleFileInfoMapper>()

    private val expectedHandle = 100L
    private val expectedMediaId: Long = 1234567
    private val expectedTotalDuration: Long = 200000
    private val expectedCurrentPosition: Long = 16000
    private val nodeMapper = mock<NodeMapper>()

    @Before
    fun setUp() {
        underTest = DefaultMediaPlayerRepository(
            megaApi = megaApi,
            megaApiFolder = megaApiFolder,
            dbHandler = dbHandler,
            nodeMapper = nodeMapper,
            fileGateway = fileGateway,
            sortOrderIntMapper = sortOrderIntMapper,
            appPreferencesGateway = appPreferencesGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            subtitleFileInfoMapper = subtitleFileInfoMapper,
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
    fun `test that searchSubtitleFileInfoList return the empty list`() =
        runTest {
            val expectedName = "SubtitleTestName.srt"
            val expectedUrl = "subtitleUrl.com"
            val expectedMegaNode = mock<MegaNode> {
                on { name }.thenReturn(expectedName)
            }
            whenever(megaApi.httpServerGetLocalLink(expectedMegaNode)).thenReturn(expectedUrl)
            whenever(
                megaApi.search(
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(listOf(expectedMegaNode))

            val actual = underTest.getSubtitleFileInfoList(".srt")

            assertThat(actual).isEmpty()
        }

    private fun createPlaybackInformation() = PlaybackInformation(
        mediaId = expectedMediaId,
        totalDuration = expectedTotalDuration,
        currentPosition = expectedCurrentPosition
    )
}
