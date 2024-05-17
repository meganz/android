package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.repository.DocumentSectionRepository
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_DESC
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSearchFilter
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentSectionRepositoryImplTest {
    private lateinit var underTest: DocumentSectionRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val sortOrderIntMapper = mock<SortOrderIntMapper>()
    private val nodeMapper = mock<NodeMapper>()
    private val cancelTokenProvider = mock<CancelTokenProvider>()
    private val megaLocalRoomGateway = mock<MegaLocalRoomGateway>()
    private val megaSearchFilterMapper = mock<MegaSearchFilterMapper>()

    @BeforeAll
    fun setUp() {
        underTest = DocumentSectionRepositoryImpl(
            megaApiGateway = megaApiGateway,
            sortOrderIntMapper = sortOrderIntMapper,
            nodeMapper = nodeMapper,
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
            nodeMapper,
            megaLocalRoomGateway
        )
    }

    @Test
    fun `test that get all documents returns successfully`() = runTest {
        val node = mock<MegaNode> {
            on { isFile }.thenReturn(true)
            on { isFolder }.thenReturn(false)
        }
        val fileNode = mock<FileNode>()
        val filter = mock<MegaSearchFilter>()
        val token = mock<MegaCancelToken>()
        whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
        whenever(sortOrderIntMapper(SortOrder.ORDER_MODIFICATION_DESC))
            .thenReturn(ORDER_DEFAULT_DESC)
        whenever(
            megaSearchFilterMapper(
                searchTarget = SearchTarget.ROOT_NODES,
                searchCategory = SearchCategory.ALL_DOCUMENTS
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
        whenever(nodeMapper(megaNode = node, offline = null)).thenReturn(fileNode)

        val actual = underTest.getAllDocuments(SortOrder.ORDER_MODIFICATION_DESC)
        assertThat(actual.isNotEmpty()).isTrue()
        assertThat(actual.size).isEqualTo(2)
    }
}