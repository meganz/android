package mega.privacy.android.feature.sync.ui.mapper.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.toImmutableList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncCompletedTransfer
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.ui.model.progress.SyncCompletedTransferUiModel
import mega.privacy.android.feature.sync.ui.model.progress.SyncProgressCompletedBody
import mega.privacy.android.feature.sync.ui.progress.entry
import mega.privacy.android.feature.sync.ui.progress.finished
import mega.privacy.android.feature.sync.ui.progress.list
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncProgressCompletedBodyMapperTest {

    private lateinit var underTest: SyncProgressCompletedBodyMapper

    private val completedMapper = mock<SyncCompletedTransferUiModelMapper>()

    @BeforeEach
    fun setUp() {
        whenever(completedMapper.invoke(any())).thenAnswer {
            model(tag = (it.arguments[0] as SyncCompletedTransfer).entry.tag)
        }
        underTest = SyncProgressCompletedBodyMapper(completedMapper = completedMapper)
    }

    @AfterEach
    fun tearDown() {
        reset(completedMapper)
    }

    @Test
    fun `test that invoke returns Loading when the list has not arrived`() {
        assertThat(underTest(list = null, isRootNodeReady = true))
            .isEqualTo(SyncProgressCompletedBody.Loading)
    }

    @Test
    fun `test that invoke returns Loading when the store is not seeded`() {
        val result = underTest(
            list = list(listOf(finished(entry(tag = 1))), isSeeded = false),
            isRootNodeReady = true,
        )

        assertThat(result).isEqualTo(SyncProgressCompletedBody.Loading)
    }

    @Test
    fun `test that invoke returns Loading when the root node is not ready`() {
        val result = underTest(
            list = list(listOf(finished(entry(tag = 1)))),
            isRootNodeReady = false,
        )

        assertThat(result).isEqualTo(SyncProgressCompletedBody.Loading)
    }

    @Test
    fun `test that invoke returns Empty when nothing completed`() {
        assertThat(underTest(list = list(), isRootNodeReady = true))
            .isEqualTo(SyncProgressCompletedBody.Empty)
    }

    @Test
    fun `test that invoke returns the completed rows in the list order`() {
        val result = underTest(
            list = list(listOf(finished(entry(tag = 3)), finished(entry(tag = 1)))),
            isRootNodeReady = true,
        )

        assertThat(result).isEqualTo(
            SyncProgressCompletedBody.Transfers(
                items = listOf(model(tag = 3), model(tag = 1)).toImmutableList(),
            )
        )
        assertThat((result as SyncProgressCompletedBody.Transfers).items.map { it.tag })
            .containsExactly(3, 1)
            .inOrder()
    }

    private fun model(tag: Int) = SyncCompletedTransferUiModel(
        tag = tag,
        fileName = "IMG_$tag.jpg",
        direction = SyncTransferDirection.Upload,
        location = "",
        sizeBytes = 1_000L,
        finishedAtMillis = 1_727_700_000_123L,
        averageSpeed = 1_000L,
    )
}
