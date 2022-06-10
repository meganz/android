package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.entity.FeatureFlag
import mega.privacy.android.app.domain.repository.FeatureFlagRepository
import mega.privacy.android.app.domain.usecase.DefaultGetAllFeatureFlags
import mega.privacy.android.app.domain.usecase.GetAllFeatureFlags
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class DefaultGetAllFeatureFlagsTest {

    lateinit var underTest: GetAllFeatureFlags
    private val featureFlagRepository = mock<FeatureFlagRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetAllFeatureFlags(featureFlagRepository)
    }

    @Test
    fun `test that features list is not empty`() {
        runTest {
            val list = mutableListOf<FeatureFlag>()
            list.add(FeatureFlag(
                "featureName",
                false,
            ))
            whenever(featureFlagRepository.getAllFeatures()).thenReturn(flowOf(list))
            underTest().collect {
                assertEquals(it[0].featureName, "featureName")
                Assert.assertEquals(it[0].isEnabled, false)
            }
        }
    }

    @Test
    fun `test that features list is empty`() {
        runTest {
            whenever(featureFlagRepository.getAllFeatures()).thenReturn(emptyFlow())
            assertEquals(underTest().count(), 0)
        }
    }
}