package mega.privacy.android.domain.usecase.sortorder

import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrderUseCase
import mega.privacy.android.domain.usecase.GetOfflineSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetSortOrderByNodeSourceTypeUseCaseTest {

    private lateinit var underTest: GetSortOrderByNodeSourceTypeUseCase

    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val getLinksSortOrderUseCase: GetLinksSortOrderUseCase = mock()
    private val getOthersSortOrder: GetOthersSortOrder = mock()
    private val getOfflineSortOrder: GetOfflineSortOrder = mock()

    @BeforeEach
    fun setUp() {
        runBlocking {
            whenever(getCloudSortOrder()).thenReturn(mockCloudSortOrder)
            whenever(getLinksSortOrderUseCase(any())).thenReturn(mockLinksSortOrder)
            whenever(getOthersSortOrder()).thenReturn(mockOthersSortOrder)
            whenever(getOfflineSortOrder()).thenReturn(mockOfflineSortOrder)
        }
        underTest = GetSortOrderByNodeSourceTypeUseCase(
            getCloudSortOrder = getCloudSortOrder,
            getLinksSortOrderUseCase = getLinksSortOrderUseCase,
            getOthersSortOrder = getOthersSortOrder,
            getOfflineSortOrder = getOfflineSortOrder,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(getCloudSortOrder, getLinksSortOrderUseCase, getOthersSortOrder, getOfflineSortOrder)
    }

    @ParameterizedTest
    @MethodSource("provideNodeSourceTypeAndExpectedSortOrder")
    fun `test sort order by node source type for legacy implementation`(
        nodeSourceType: NodeSourceType,
        expectedSortOrder: SortOrder,
    ) = runBlocking {
        val result = underTest(nodeSourceType, false)
        assertEquals(expectedSortOrder, result)
    }

    @ParameterizedTest
    @MethodSource("provideNodeSourceTypeAndExpectedSortOrder")
    fun `test sort order by node source type for revamp`(
        nodeSourceType: NodeSourceType,
        expectedSortOrder: SortOrder,
    ) = runBlocking {
        val result = underTest(nodeSourceType, true)
        assertEquals(expectedSortOrder, result)
    }

    fun provideNodeSourceTypeAndExpectedSortOrder(): Stream<Arguments> = Stream.of(
        Arguments.of(NodeSourceType.LINKS, mockLinksSortOrder),
        Arguments.of(NodeSourceType.INCOMING_SHARES, mockOthersSortOrder),
        Arguments.of(NodeSourceType.CLOUD_DRIVE, mockCloudSortOrder),
        Arguments.of(NodeSourceType.HOME, mockCloudSortOrder),
        Arguments.of(NodeSourceType.RUBBISH_BIN, mockCloudSortOrder),
        Arguments.of(NodeSourceType.BACKUPS, mockCloudSortOrder),
        Arguments.of(NodeSourceType.DOCUMENTS, mockCloudSortOrder),
        Arguments.of(NodeSourceType.AUDIO, mockCloudSortOrder),
        Arguments.of(NodeSourceType.FAVOURITES, mockCloudSortOrder),
        Arguments.of(NodeSourceType.OUTGOING_SHARES, mockCloudSortOrder),
        Arguments.of(NodeSourceType.OTHER, mockCloudSortOrder),
        Arguments.of(NodeSourceType.VIDEOS, mockCloudSortOrder),
        Arguments.of(NodeSourceType.SEARCH, mockCloudSortOrder),
        Arguments.of(NodeSourceType.OFFLINE, mockOfflineSortOrder),
        Arguments.of(NodeSourceType.VIDEO_PLAYLISTS, mockCloudSortOrder),
    )

    companion object {
        private val mockCloudSortOrder = SortOrder.ORDER_DEFAULT_ASC
        private val mockLinksSortOrder = SortOrder.ORDER_DEFAULT_DESC
        private val mockOthersSortOrder = SortOrder.ORDER_SIZE_DESC
        private val mockOfflineSortOrder = SortOrder.ORDER_LABEL_ASC
    }
}