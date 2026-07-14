package mega.privacy.mobile.home.presentation.home.widget.domore

import androidx.compose.ui.graphics.vector.ImageVector
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.camerauploads.HasCameraSyncEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.icon.pack.IconPack
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class DoMoreWithMegaWidgetViewModelTest {

    private lateinit var underTest: DoMoreWithMegaWidgetViewModel

    private val isCameraUploadsEnabledUseCase = mock<IsCameraUploadsEnabledUseCase>()
    private val hasCameraSyncEnabledUseCase = mock<HasCameraSyncEnabledUseCase>()

    private fun initViewModel(
        items: Set<DoMoreWithMegaItem>,
        isCameraUploadsEnabled: Boolean = false,
        hasPreviouslyEnabledCameraUploads: Boolean = false,
    ) {
        whenever(isCameraUploadsEnabledUseCase.monitorCameraUploadsEnabled)
            .thenReturn(flowOf(isCameraUploadsEnabled))
        wheneverBlocking { hasCameraSyncEnabledUseCase() }
            .thenReturn(hasPreviouslyEnabledCameraUploads)
        underTest = DoMoreWithMegaWidgetViewModel(
            items = items,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            hasCameraSyncEnabledUseCase = hasCameraSyncEnabledUseCase,
        )
    }

    @Test
    fun `test that uiState emits all provided items`() = runTest {
        val items = DoMoreWithMegaItem.Identifier.entries.map { fakeItem(it) }.toSet()
        initViewModel(items)

        underTest.uiState.test {
            assertThat(awaitItem().items.map { it.identifier })
                .containsExactlyElementsIn(DoMoreWithMegaItem.Identifier.entries)
                .inOrder()
        }
    }

    @Test
    fun `test that uiState emits empty items when no items are provided`() = runTest {
        initViewModel(emptySet())

        underTest.uiState.test {
            assertThat(awaitItem().items).isEmpty()
        }
    }

    @Test
    fun `test that uiState emits isCameraUploadsEnabled true when camera uploads is enabled`() =
        runTest {
            initViewModel(emptySet(), isCameraUploadsEnabled = true)
            backgroundScope.launch { underTest.uiState.collect {} }
            advanceUntilIdle()

            assertThat(underTest.uiState.value.isCameraUploadsEnabled).isTrue()
        }

    @Test
    fun `test that uiState emits isCameraUploadsEnabled false when camera uploads is disabled`() =
        runTest {
            initViewModel(emptySet(), isCameraUploadsEnabled = false)
            backgroundScope.launch { underTest.uiState.collect {} }
            advanceUntilIdle()

            assertThat(underTest.uiState.value.isCameraUploadsEnabled).isFalse()
        }

    @Test
    fun `test that uiState emits hasPreviouslyEnabledCameraUploads true when camera uploads was previously enabled`() =
        runTest {
            initViewModel(emptySet(), hasPreviouslyEnabledCameraUploads = true)
            backgroundScope.launch { underTest.uiState.collect {} }
            advanceUntilIdle()

            assertThat(underTest.uiState.value.hasPreviouslyEnabledCameraUploads).isTrue()
        }

    @Test
    fun `test that uiState emits hasPreviouslyEnabledCameraUploads false when camera uploads was never enabled`() =
        runTest {
            initViewModel(emptySet(), hasPreviouslyEnabledCameraUploads = false)
            backgroundScope.launch { underTest.uiState.collect {} }
            advanceUntilIdle()

            assertThat(underTest.uiState.value.hasPreviouslyEnabledCameraUploads).isFalse()
        }

    private fun fakeItem(identifier: DoMoreWithMegaItem.Identifier) = object : DoMoreWithMegaItem {
        override val identifier: DoMoreWithMegaItem.Identifier = identifier
        override val icon: ImageVector = IconPack.Medium.Thin.Outline.Camera
        override val labelRes: Int = 0
    }
}
