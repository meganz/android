package mega.privacy.android.feature.sync.presentation

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.feature.sync.domain.usecase.SetOnboardingShownUseCase
import mega.privacy.android.feature.sync.ui.synclist.SyncListViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncListViewModelTest {

    private lateinit var underTest: SyncListViewModel
    private val setOnboardingShownUseCase: SetOnboardingShownUseCase = mock()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun resetAndTearDown() {
        Dispatchers.resetMain()
        reset(setOnboardingShownUseCase)
    }

    @Test
    fun `test that stalled issues count is fetched when viewmodel is loaded`() = runTest {
        initViewModel()

        assertThat(underTest.state.value.stalledIssuesCount).isEqualTo(3)
    }

    @Test
    fun `test that view model initialization sets onboarding shown to true`() = runTest {
        initViewModel()

        verify(setOnboardingShownUseCase).invoke(true)
    }


    private fun initViewModel() {
        underTest = SyncListViewModel(setOnboardingShownUseCase)
    }
}