package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.entity.FeatureFlag
import mega.privacy.android.app.domain.repository.FeatureFlagRepository
import mega.privacy.android.app.domain.usecase.DefaultInsertFeatureFlag
import mega.privacy.android.app.domain.usecase.InsertFeatureFlag
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class DefaultInsertFeatureFlagTest {

    lateinit var underTest: InsertFeatureFlag
    private val featureFlagRepository = mock<FeatureFlagRepository>()

    @Before
    fun setUp() {
        underTest = DefaultInsertFeatureFlag(featureFlagRepository)
    }

    @Test
    fun `test that feature record is inserted`()  {
        runTest {
            underTest("Name", false)
            verify(featureFlagRepository, times(1)).setFeature("Name", false)
        }
    }
}