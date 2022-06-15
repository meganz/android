package test.mega.privacy.android.app.presentation.featureflag

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.entity.FeatureFlag
import mega.privacy.android.app.domain.usecase.GetAllFeatureFlags
import mega.privacy.android.app.domain.usecase.SetFeatureFlag
import mega.privacy.android.app.presentation.featureflag.FeatureFlagMenuViewModel
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class FeatureFlagMenuViewModelTest {
    private lateinit var underTest: FeatureFlagMenuViewModel
    private val insertFeatureFlag = mock<SetFeatureFlag>()
    private val getAllFeatureFlags = mock<GetAllFeatureFlags>()
    private val scheduler = TestCoroutineScheduler()
    private val standardDispatcher = StandardTestDispatcher(scheduler)

    @Before
    fun setUp() {
        Dispatchers.setMain(standardDispatcher)
        underTest =
            FeatureFlagMenuViewModel(insertFeatureFlag, getAllFeatureFlags, standardDispatcher)
    }

    @Test
    fun `test that features list is not empty`() {
        runTest {
            val list = mutableListOf<FeatureFlag>()
            list.add(FeatureFlag("featureName",
                false))
            whenever(getAllFeatureFlags()).thenReturn(flowOf(list))
            underTest.getAllFeatures().collect {
                assertEquals(it[0].featureName, "featureName")
                Assert.assertEquals(it[0].isEnabled, false)
            }
        }
    }

    @Test
    fun `test that insert feature flag use case gets called`() {
        runTest {
            underTest.setFeatureEnabled("featureName", false)
            verify(insertFeatureFlag, times(1))("featureName", false)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}