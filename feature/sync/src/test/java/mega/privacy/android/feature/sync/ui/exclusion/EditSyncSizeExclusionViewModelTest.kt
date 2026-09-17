package mega.privacy.android.feature.sync.ui.exclusion

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncSizeExclusionPrefillMapper
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EditSyncSizeExclusionViewModelTest {

    private fun underTest(exclusion: SyncSizeExclusion) = EditSyncSizeExclusionViewModel(
        exclusion = exclusion,
        prefillMapper = SyncSizeExclusionPrefillMapper(),
    )

    @Test
    fun `test that init prefills the form from the bounds the file declares`() = runTest {
        val viewModel = underTest(
            SyncSizeExclusion(excludeLargerThanBytes = 100L * 1024 * 1024),
        )

        viewModel.uiState.test {
            val initial = awaitItem()

            assertThat(initial.bySize.condition).isEqualTo(SyncSizeCondition.BiggerThan)
            assertThat(initial.bySize.firstBound.value).isEqualTo("100")
            assertThat(initial.bySize.firstBound.unit).isEqualTo(SyncSizeUnit.Megabytes)
            assertThat(initial.showsBlockingError).isFalse()
        }
    }

    @Test
    fun `test that init opens an empty form when the file declares no bound`() = runTest {
        val viewModel = underTest(SyncSizeExclusion())

        viewModel.uiState.test {
            val initial = awaitItem()

            assertThat(initial.bySize.condition).isNull()
            assertThat(initial.showsBlockingError).isFalse()
        }
    }

    @Test
    fun `test that init blocks saving when the file already leaves out every file`() = runTest {
        val viewModel = underTest(
            SyncSizeExclusion(excludeSmallerThanBytes = 1024, excludeLargerThanBytes = 512),
        )

        viewModel.uiState.test {
            assertThat(awaitItem().showsBlockingError).isTrue()
        }
    }

    @Test
    fun `test that onConditionSelected replaces the condition`() = runTest {
        val viewModel = underTest(SyncSizeExclusion(excludeLargerThanBytes = 1024))

        viewModel.uiState.test {
            awaitItem()

            viewModel.onConditionSelected(SyncSizeCondition.NotBetween)

            assertThat(awaitItem().bySize.condition).isEqualTo(SyncSizeCondition.NotBetween)
        }
    }

    @Test
    fun `test that onFirstBoundValueChanged replaces the first value`() = runTest {
        val viewModel = underTest(SyncSizeExclusion(excludeLargerThanBytes = 1024))

        viewModel.uiState.test {
            awaitItem()

            viewModel.onFirstBoundValueChanged("7")

            assertThat(awaitItem().bySize.firstBound.value).isEqualTo("7")
        }
    }

    @Test
    fun `test that onFirstBoundUnitSelected replaces the first unit`() = runTest {
        val viewModel = underTest(SyncSizeExclusion(excludeLargerThanBytes = 1024))

        viewModel.uiState.test {
            awaitItem()

            viewModel.onFirstBoundUnitSelected(SyncSizeUnit.Gigabytes)

            assertThat(awaitItem().bySize.firstBound.unit).isEqualTo(SyncSizeUnit.Gigabytes)
        }
    }

    @Test
    fun `test that onSecondBoundValueChanged replaces the second value`() = runTest {
        val viewModel = underTest(SyncSizeExclusion(excludeLargerThanBytes = 1024))

        viewModel.uiState.test {
            awaitItem()

            viewModel.onSecondBoundValueChanged("9")

            assertThat(awaitItem().bySize.secondBound.value).isEqualTo("9")
        }
    }

    @Test
    fun `test that onSecondBoundUnitSelected replaces the second unit`() = runTest {
        val viewModel = underTest(SyncSizeExclusion(excludeLargerThanBytes = 1024))

        viewModel.uiState.test {
            awaitItem()

            viewModel.onSecondBoundUnitSelected(SyncSizeUnit.Bytes)

            assertThat(awaitItem().bySize.secondBound.unit).isEqualTo(SyncSizeUnit.Bytes)
        }
    }

    @Test
    fun `test that editing into a downward range blocks saving`() = runTest {
        val viewModel = underTest(
            SyncSizeExclusion(
                excludeSmallerThanBytes = 1024,
                excludeLargerThanBytes = 2L * 1024 * 1024 * 1024,
            )
        )

        viewModel.uiState.test {
            assertThat(awaitItem().showsBlockingError).isFalse()

            viewModel.onSecondBoundValueChanged("1")
            viewModel.onSecondBoundUnitSelected(SyncSizeUnit.Bytes)

            awaitItem()
            assertThat(awaitItem().showsBlockingError).isTrue()
        }
    }
}
