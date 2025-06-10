package mega.privacy.android.domain.usecase.sortorder

import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetSortOrderByNodeSourceTypeUseCaseTest {

    private lateinit var underTest: GetSortOrderByNodeSourceTypeUseCase

    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val getLinksSortOrder: GetLinksSortOrder = mock()
    private val getOthersSortOrder: GetOthersSortOrder = mock()

    @BeforeEach
    fun setUp() {
        runBlocking {
            whenever(getCloudSortOrder()).thenReturn(mockCloudSortOrder)
            whenever(getLinksSortOrder()).thenReturn(mockLinksSortOrder)
            whenever(getOthersSortOrder()).thenReturn(mockOthersSortOrder)
        }
        underTest = GetSortOrderByNodeSourceTypeUseCase(
            getCloudSortOrder = getCloudSortOrder,
            getLinksSortOrder = getLinksSortOrder,
            getOthersSortOrder = getOthersSortOrder
        )
    }

    @AfterEach
    fun tearDown() {
        reset(getCloudSortOrder, getLinksSortOrder, getOthersSortOrder)
    }

    @ParameterizedTest
    @MethodSource("provideNodeSourceTypeAndExpectedSortOrder")
    fun `test sort order by node source type`(
        nodeSourceType: NodeSourceType,
        expectedSortOrder: SortOrder,
    ) = runBlocking {
        val result = underTest(nodeSourceType)
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
        Arguments.of(NodeSourceType.OTHER, mockCloudSortOrder)
    )

    companion object {
        private val mockCloudSortOrder = SortOrder.ORDER_DEFAULT_ASC
        private val mockLinksSortOrder = SortOrder.ORDER_DEFAULT_DESC
        private val mockOthersSortOrder = SortOrder.ORDER_SIZE_DESC
    }
}