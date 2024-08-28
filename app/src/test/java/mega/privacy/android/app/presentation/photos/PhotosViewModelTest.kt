package mega.privacy.android.app.presentation.photos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.photos.PhotosViewModel
import mega.privacy.android.app.presentation.photos.model.PhotosTab
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * Test class for [PhotosViewModel]
 */
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PhotosViewModelTest {

    private lateinit var underTest: PhotosViewModel

    @ParameterizedTest(name = "photos tab: {0}")
    @EnumSource(value = PhotosTab::class)
    fun `test that the selected photos tab is updated`(selectedTab: PhotosTab) = runTest {
        initializeViewModel()
        underTest.onTabSelected(selectedTab)
        underTest.state.map { it.selectedTab }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isEqualTo(selectedTab)
            }
    }

    @ParameterizedTest(name = "is menu showing: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the menu visibility is updated`(isMenuShowing: Boolean) = runTest {
        initializeViewModel()
        underTest.setMenuShowing(isMenuShowing)
        underTest.state.map { it.isMenuShowing }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isEqualTo(isMenuShowing)
            }
    }

    private fun initializeViewModel() {
        underTest = PhotosViewModel()
    }
}