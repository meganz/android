package test.mega.privacy.android.app.presentation.favourites

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.entity.FeatureFlag
import mega.privacy.android.app.domain.usecase.GetAllFeatureFlags
import mega.privacy.android.app.domain.usecase.InsertFeatureFlag
import mega.privacy.android.app.presentation.featureflag.FeatureFlagMenuViewModel
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
    private val insertFeatureFlag = mock<InsertFeatureFlag>()
    private val getAllFeatureFlags = mock<GetAllFeatureFlags>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = FeatureFlagMenuViewModel(insertFeatureFlag, getAllFeatureFlags)
    }

    @Test
    fun `test that features list is not empty`() {
        runTest {
            whenever(getAllFeatureFlags()).thenReturn(flowOf(
                    FeatureFlag(
                            "featureName",
                            false,
                    )))
            underTest.getAllFeatures().collect {
                assertEquals(it.featureName, "featureName")
                Assert.assertEquals(it.isEnabled, false)
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
}