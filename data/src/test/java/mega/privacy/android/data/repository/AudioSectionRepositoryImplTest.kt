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
import mega.privacy.android.data.mapper.audios.TypedAudioNodeMapper
import mega.privacy.android.data.mapper.node.FileNodeMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.repository.AudioSectionRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSearchFilter
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AudioSectionRepositoryImplTest {
    private lateinit var underTest: AudioSectionRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val sortOrderIntMapper = mock<SortOrderIntMapper>()
    private val fileNodeMapper = mock<FileNodeMapper>()
    private val typedAudioNodeMapper = mock<TypedAudioNodeMapper>()
    private val cancelTokenProvider = mock<CancelTokenProvider>()
    private val megaLocalRoomGateway = mock<MegaLocalRoomGateway>()
    private val megaSearchFilterMapper = mock<MegaSearchFilterMapper>()

    @BeforeAll
    fun setUp() {
        underTest = AudioSectionRepositoryImpl(
            megaApiGateway = megaApiGateway,
            sortOrderIntMapper = sortOrderIntMapper,
            fileNodeMapper = fileNodeMapper,
            typedAudioNodeMapper = typedAudioNodeMapper,
            cancelTokenProvider = cancelTokenProvider,
            megaLocalRoomGateway = megaLocalRoomGateway,
            megaSearchFilterMapper = megaSearchFilterMapper,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiGateway,
            sortOrderIntMapper,
            fileNodeMapper,
            typedAudioNodeMapper,
            megaLocalRoomGateway
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that get all audios returns successfully`() = runTest {
        val node = mock<MegaNode> {
            on { isFile }.thenReturn(true)
            on { isFolder }.thenReturn(false)
            on { duration }.thenReturn(100)
        }
        val fileNode = mock<FileNode>()
        val filter = mock<MegaSearchFilter>()
        val token = mock<MegaCancelToken>()
        val typedVideoNode = mock<TypedAudioNode> {
            on { thumbnailPath }.thenReturn(null)
        }
        whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
        whenever(sortOrderIntMapper(SortOrder.ORDER_MODIFICATION_DESC))
            .thenReturn(MegaApiJava.ORDER_DEFAULT_DESC)
        whenever(
            megaSearchFilterMapper(
                searchTarget = SearchTarget.ROOT_NODES,
                searchCategory = SearchCategory.AUDIO
            )
        ).thenReturn(filter)
        whenever(
            megaApiGateway.searchWithFilter(
                filter,
                sortOrderIntMapper(SortOrder.ORDER_MODIFICATION_DESC),
                token
            )
        ).thenReturn(listOf(node, node))
        whenever(megaLocalRoomGateway.getAllOfflineInfo()).thenReturn(null)
        whenever(
            fileNodeMapper(
                megaNode = node,
                requireSerializedData = false,
                offline = null
            )
        ).thenReturn(fileNode)
        whenever(typedAudioNodeMapper(fileNode, node.duration)).thenReturn(typedVideoNode)

        val actual = underTest.getAllAudios(SortOrder.ORDER_MODIFICATION_DESC)
        assertThat(actual.size).isEqualTo(2)
    }
}