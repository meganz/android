package mega.privacy.android.domain.usecase.folderpreference

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.node.HandleToBase64UseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFolderSortOrderUseCaseTest {
    private lateinit var underTest: GetFolderSortOrderUseCase

    private val monitorFolderSortOrderUseCase = mock<MonitorFolderSortOrderUseCase>()
    private val handleToBase64UseCase = mock<HandleToBase64UseCase>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()

    @BeforeAll
    fun setUp() {
        underTest = GetFolderSortOrderUseCase(
            monitorFolderSortOrderUseCase = monitorFolderSortOrderUseCase,
            handleToBase64UseCase = handleToBase64UseCase,
            getCloudSortOrder = getCloudSortOrder,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(monitorFolderSortOrderUseCase, handleToBase64UseCase, getCloudSortOrder)
    }

    @Test
    fun `test that invoke returns the folder sort order for the folder key`() = runTest {
        whenever(handleToBase64UseCase(FOLDER_ID.longValue)).thenReturn(KEY)
        whenever(monitorFolderSortOrderUseCase(eq(KEY), any()))
            .thenReturn(flowOf(SortOrder.ORDER_SIZE_DESC))

        assertThat(underTest(FOLDER_ID)).isEqualTo(SortOrder.ORDER_SIZE_DESC)
    }

    @Test
    fun `test that invoke falls back to the cloud sort order when per folder sorting is off`() =
        runTest {
            whenever(handleToBase64UseCase(FOLDER_ID.longValue)).thenReturn(KEY)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
            whenever(monitorFolderSortOrderUseCase(eq(KEY), any())).thenAnswer {
                it.getArgument<Flow<SortOrder>>(1)
            }

            assertThat(underTest(FOLDER_ID)).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
        }

    private companion object {
        val FOLDER_ID = NodeId(123L)
        const val KEY = "folderKey"
    }
}
