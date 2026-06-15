package mega.privacy.mobile.home.presentation.home.widget.domore

import androidx.compose.ui.graphics.vector.ImageVector
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.icon.pack.IconPack
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineMainDispatcherExtension::class)
class DoMoreWithMegaWidgetViewModelTest {

    private lateinit var underTest: DoMoreWithMegaWidgetViewModel

    private fun initViewModel(items: Set<DoMoreWithMegaItem>) {
        underTest = DoMoreWithMegaWidgetViewModel(items = items)
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

    private fun fakeItem(identifier: DoMoreWithMegaItem.Identifier) = object : DoMoreWithMegaItem {
        override val identifier: DoMoreWithMegaItem.Identifier = identifier
        override val icon: ImageVector = IconPack.Medium.Thin.Outline.Camera
        override val labelRes: Int = 0
    }
}
