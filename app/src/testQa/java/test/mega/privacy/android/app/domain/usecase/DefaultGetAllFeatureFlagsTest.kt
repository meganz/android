package test.mega.privacy.android.app.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.repository.QARepository
import mega.privacy.android.app.domain.usecase.DefaultGetAllFeatureFlags
import mega.privacy.android.app.domain.usecase.GetAllFeatureFlags
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.usecase.GetFeatureFlagValue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultGetAllFeatureFlagsTest {

    lateinit var underTest: GetAllFeatureFlags
    private val getFeatureFlagValue = mock<GetFeatureFlagValue> { onBlocking { invoke(any()) }.thenReturn(false)}
    private val qaRepository = mock<QARepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetAllFeatureFlags(
            getFeatureFlagValue = getFeatureFlagValue,
            qaRepository = qaRepository
        )
    }

    @Test
    fun `test that features list is empty if no features are returned`() =
        runTest {
            whenever(qaRepository.getAllFeatures()).thenReturn(emptyList())
            whenever(qaRepository.monitorLocalFeatureFlags()).thenReturn(flowOf(mapOf("test" to false)))

            underTest().test {
                assertThat(awaitItem().size).isEqualTo(0)
                awaitComplete()
            }
        }

    @Test
    fun `test that stored value is returned if found`() = runTest {
        val featureName = "featureName"
        val feature = mock<Feature> { on { name }.thenReturn(featureName) }
        val expectedValue = true
        whenever(qaRepository.getAllFeatures()).thenReturn(listOf(feature))
        whenever(qaRepository.monitorLocalFeatureFlags()).thenReturn(flowOf(mapOf(featureName to expectedValue)))

        underTest().test {
            val map = awaitItem()
            assertThat(map.size).isEqualTo(1)
            assertThat(map[feature]).isEqualTo(expectedValue)
            awaitComplete()
        }
    }

    @Test
    fun `test that current value is returned if no stored value is found`() = runTest {
        val featureName = "featureName"
        val feature = mock<Feature> { on { name }.thenReturn(featureName) }
        val expectedValue = true
        whenever(qaRepository.getAllFeatures()).thenReturn(listOf(feature))
        whenever(getFeatureFlagValue(feature)).thenReturn(expectedValue)
        whenever(qaRepository.monitorLocalFeatureFlags()).thenReturn(flowOf(emptyMap()))

        underTest().test {
            val map = awaitItem()
            assertThat(map.size).isEqualTo(1)
            assertThat(map[feature]).isEqualTo(expectedValue)
            awaitComplete()
        }
    }

    @Test
    fun `test that false is returned if no local or default value set`() = runTest {
        val featureName = "featureName"
        val feature = mock<Feature> { on { name }.thenReturn(featureName) }
        whenever(qaRepository.getAllFeatures()).thenReturn(listOf(feature))
        whenever(qaRepository.monitorLocalFeatureFlags()).thenReturn(flowOf(emptyMap()))

        underTest().test {
            val map = awaitItem()
            assertThat(map.size).isEqualTo(1)
            assertThat(map[feature]).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `test that all features are returned regardless of values`() = runTest {
        val localValueFeatureName = "localValueFeatureName"
        val defaultValueFeatureName = "defaultValueFeatureName"
        val localValueFeature = mock<Feature> { on { name }.thenReturn(localValueFeatureName) }
        val defaultValueFeature = mock<Feature> { on { name }.thenReturn(defaultValueFeatureName) }
        val expectedValue = true
        val features = listOf(
            localValueFeature,
            defaultValueFeature,
        )
        whenever(qaRepository.getAllFeatures()).thenReturn(features)
        whenever(getFeatureFlagValue(defaultValueFeature)).thenReturn(
            expectedValue)
        whenever(qaRepository.monitorLocalFeatureFlags()).thenReturn(flowOf(mapOf(
            localValueFeatureName to expectedValue)))

        underTest().test {
            val map = awaitItem()
            assertThat(map.size).isEqualTo(features.size)
            assertWithMessage("Expected local value is $expectedValue").that(map[localValueFeature])
                .isEqualTo(expectedValue)
            assertWithMessage("Expected default value is $expectedValue").that(map[defaultValueFeature])
                .isEqualTo(expectedValue)
            awaitComplete()
        }
    }

    @Test
    fun `test that value is updated if set`() = runTest {
        val featureName = "featureName"
        val feature = mock<Feature> { on { name }.thenReturn(featureName) }
        val expectedValue = true

        val localFlow = MutableStateFlow<Map<String, Boolean>>(emptyMap())
        whenever(qaRepository.getAllFeatures()).thenReturn(listOf(feature))
        whenever(getFeatureFlagValue(feature)).thenReturn(expectedValue)
        whenever(qaRepository.monitorLocalFeatureFlags()).thenReturn(localFlow)

        underTest().test {
            assertWithMessage("Expected initial value is $expectedValue").that(awaitItem()[feature])
                .isEqualTo(expectedValue)
            localFlow.emit(mapOf(featureName to !expectedValue))
            assertWithMessage("Expected updated value is ${!expectedValue}").that(awaitItem()[feature])
                .isEqualTo(!expectedValue)
        }
    }

}
