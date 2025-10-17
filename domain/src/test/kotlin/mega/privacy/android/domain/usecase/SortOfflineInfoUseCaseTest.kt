package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SortOfflineInfoUseCaseTest {

    private lateinit var underTest: SortOfflineInfoUseCase

    private val getOfflineSortOrder = mock<GetOfflineSortOrder>()

    @BeforeEach
    fun setUp() {
        underTest = SortOfflineInfoUseCase(
            getOfflineSortOrder = getOfflineSortOrder,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getOfflineSortOrder)
    }

    @Test
    fun `test that folders are sorted before files regardless of sort order`() = runTest {
        val offlineInfoList = listOf(
            createMockOfflineFileInformation(name = "file1.txt", isFolder = false),
            createMockOfflineFileInformation(name = "folder1", isFolder = true),
            createMockOfflineFileInformation(name = "file2.txt", isFolder = false),
            createMockOfflineFileInformation(name = "folder2", isFolder = true),
        )

        whenever(getOfflineSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)

        val result = underTest(offlineInfoList)

        assertThat(result[0].isFolder).isTrue()
        assertThat(result[1].isFolder).isTrue()
        assertThat(result[2].isFolder).isFalse()
        assertThat(result[3].isFolder).isFalse()
    }

    @ParameterizedTest(name = "test sort order {0}")
    @MethodSource("provideSortOrderTestCases")
    fun `test that offline info is sorted correctly by different sort orders`(
        sortOrder: SortOrder,
        expectedFirstItemName: String,
        expectedSecondItemName: String,
    ) = runTest {
        val offlineInfoList = listOf(
            createMockOfflineFileInformation(
                name = "zebra.txt",
                totalSize = 1000L,
                lastModifiedTime = 1000L
            ),
            createMockOfflineFileInformation(
                name = "apple.txt",
                totalSize = 2000L,
                lastModifiedTime = 2000L
            ),
        )

        whenever(getOfflineSortOrder()).thenReturn(sortOrder)

        val result = underTest(offlineInfoList)

        assertThat(result[0].name).isEqualTo(expectedFirstItemName)
        assertThat(result[1].name).isEqualTo(expectedSecondItemName)
    }

    @Test
    fun `test that empty list returns empty list`() = runTest {
        val emptyList = emptyList<OfflineFileInformation>()

        whenever(getOfflineSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)

        val result = underTest(emptyList)

        assertThat(result).isEmpty()
    }

    @Test
    fun `test that single item list returns same list`() = runTest {
        val singleItemList = listOf(
            createMockOfflineFileInformation(name = "single_file.txt"),
        )

        whenever(getOfflineSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)

        val result = underTest(singleItemList)

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("single_file.txt")
    }

    @Test
    fun `test that folders are sorted by name when sort order is default asc`() = runTest {
        val offlineInfoList = listOf(
            createMockOfflineFileInformation(name = "zebra_folder", isFolder = true),
            createMockOfflineFileInformation(name = "apple_folder", isFolder = true),
        )

        whenever(getOfflineSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)

        val result = underTest(offlineInfoList)

        assertThat(result[0].name).isEqualTo("apple_folder")
        assertThat(result[1].name).isEqualTo("zebra_folder")
    }

    @Test
    fun `test that folders are sorted by name desc when sort order is default desc`() = runTest {
        val offlineInfoList = listOf(
            createMockOfflineFileInformation(name = "apple_folder", isFolder = true),
            createMockOfflineFileInformation(name = "zebra_folder", isFolder = true),
        )

        whenever(getOfflineSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_DESC)

        val result = underTest(offlineInfoList)

        assertThat(result[0].name).isEqualTo("zebra_folder")
        assertThat(result[1].name).isEqualTo("apple_folder")
    }

    @Test
    fun `test that files are sorted by size when sort order is size asc`() = runTest {
        val offlineInfoList = listOf(
            createMockOfflineFileInformation(
                name = "large_file.txt",
                totalSize = 2000L,
                isFolder = false
            ),
            createMockOfflineFileInformation(
                name = "small_file.txt",
                totalSize = 1000L,
                isFolder = false
            ),
        )

        whenever(getOfflineSortOrder()).thenReturn(SortOrder.ORDER_SIZE_ASC)

        val result = underTest(offlineInfoList)

        assertThat(result[0].name).isEqualTo("small_file.txt")
        assertThat(result[1].name).isEqualTo("large_file.txt")
    }

    @Test
    fun `test that files are sorted by size desc when sort order is size desc`() = runTest {
        val offlineInfoList = listOf(
            createMockOfflineFileInformation(
                name = "small_file.txt",
                totalSize = 1000L,
                isFolder = false
            ),
            createMockOfflineFileInformation(
                name = "large_file.txt",
                totalSize = 2000L,
                isFolder = false
            ),
        )

        whenever(getOfflineSortOrder()).thenReturn(SortOrder.ORDER_SIZE_DESC)

        val result = underTest(offlineInfoList)

        assertThat(result[0].name).isEqualTo("large_file.txt")
        assertThat(result[1].name).isEqualTo("small_file.txt")
    }

    @Test
    fun `test that files are sorted by modification time when sort order is modification asc`() =
        runTest {
            val offlineInfoList = listOf(
                createMockOfflineFileInformation(
                    name = "new_file.txt",
                    lastModifiedTime = 2000L,
                    isFolder = false
                ),
                createMockOfflineFileInformation(
                    name = "old_file.txt",
                    lastModifiedTime = 1000L,
                    isFolder = false
                ),
            )

            whenever(getOfflineSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_ASC)

            val result = underTest(offlineInfoList)

            assertThat(result[0].name).isEqualTo("old_file.txt")
            assertThat(result[1].name).isEqualTo("new_file.txt")
        }

    @Test
    fun `test that files are sorted by modification time desc when sort order is modification desc`() =
        runTest {
            val offlineInfoList = listOf(
                createMockOfflineFileInformation(
                    name = "old_file.txt",
                    lastModifiedTime = 1000L,
                    isFolder = false
                ),
                createMockOfflineFileInformation(
                    name = "new_file.txt",
                    lastModifiedTime = 2000L,
                    isFolder = false
                ),
            )

            whenever(getOfflineSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)

            val result = underTest(offlineInfoList)

            assertThat(result[0].name).isEqualTo("new_file.txt")
            assertThat(result[1].name).isEqualTo("old_file.txt")
        }

    @Test
    fun `test that unknown sort order defaults to name asc`() = runTest {
        val offlineInfoList = listOf(
            createMockOfflineFileInformation(name = "zebra.txt", isFolder = false),
            createMockOfflineFileInformation(name = "apple.txt", isFolder = false),
        )

        whenever(getOfflineSortOrder()).thenReturn(SortOrder.ORDER_LABEL_ASC)

        val result = underTest(offlineInfoList)

        assertThat(result[0].name).isEqualTo("apple.txt")
        assertThat(result[1].name).isEqualTo("zebra.txt")
    }

    private fun provideSortOrderTestCases(): Stream<Arguments> = Stream.of(
        Arguments.of(SortOrder.ORDER_DEFAULT_ASC, "apple.txt", "zebra.txt"),
        Arguments.of(SortOrder.ORDER_DEFAULT_DESC, "zebra.txt", "apple.txt"),
        Arguments.of(
            SortOrder.ORDER_SIZE_ASC,
            "zebra.txt",
            "apple.txt"
        ),
        Arguments.of(
            SortOrder.ORDER_SIZE_DESC,
            "apple.txt",
            "zebra.txt"
        ),
        Arguments.of(
            SortOrder.ORDER_MODIFICATION_ASC,
            "zebra.txt",
            "apple.txt"
        ),
        Arguments.of(
            SortOrder.ORDER_MODIFICATION_DESC,
            "apple.txt",
            "zebra.txt"
        ),
    )

    private fun createMockOfflineFileInformation(
        name: String = "test_file.txt",
        totalSize: Long = 1024L,
        isFolder: Boolean = false,
        lastModifiedTime: Long? = 1234567890L,
    ): OfflineFileInformation = OfflineFileInformation(
        nodeInfo = createMockOfflineNodeInformation(
            name = name,
            isFolder = isFolder,
            lastModifiedTime = lastModifiedTime,
        ),
        totalSize = totalSize,
        folderInfo = null,
        fileTypeInfo = null,
        thumbnail = null,
        absolutePath = "/test/path/$name",
    )

    private fun createMockOfflineNodeInformation(
        id: Int = 1,
        name: String = "test_file.txt",
        path: String = "/test/path",
        handle: String = "123456789",
        isFolder: Boolean = false,
        lastModifiedTime: Long? = 1234567890L,
        parentId: Int = 0,
    ): OfflineNodeInformation = OtherOfflineNodeInformation(
        id = id,
        path = path,
        name = name,
        handle = handle,
        isFolder = isFolder,
        lastModifiedTime = lastModifiedTime,
        parentId = parentId,
    )
}