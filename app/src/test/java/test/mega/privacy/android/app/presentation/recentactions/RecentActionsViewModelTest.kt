package test.mega.privacy.android.app.presentation.recentactions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.recentactions.RecentActionsViewModel
import org.junit.Before

@ExperimentalCoroutinesApi
class RecentActionsViewModelTest {
    private lateinit var underTest: RecentActionsViewModel


    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = RecentActionsViewModel(
        )
    }
}