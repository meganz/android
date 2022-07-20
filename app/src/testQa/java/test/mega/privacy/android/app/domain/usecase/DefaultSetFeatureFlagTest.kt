package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.SetFeatureFlag
import mega.privacy.android.domain.repository.FeatureFlagRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class DefaultSetFeatureFlagTest {

    lateinit var underTest: SetFeatureFlag
    private val featureFlagRepository = mock<FeatureFlagRepository>()

    @Before
    fun setUp() {
        underTest = SetFeatureFlag(featureFlagRepository::setFeature)
    }

    @Test
    fun `test that feature record is inserted`() {
        runTest {
            underTest("Name", false)
            verify(featureFlagRepository, times(1)).setFeature("Name", false)
        }
    }
}