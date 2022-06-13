package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.repository.FeatureFlagRepository
import mega.privacy.android.app.domain.usecase.DefaultGetFeatureFlag
import mega.privacy.android.app.domain.usecase.GetFeatureFlag
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultGetFeatureFlagTest {

    lateinit var underTest: GetFeatureFlag
    private val featureFlagRepository = mock<FeatureFlagRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetFeatureFlag(featureFlagRepository)
    }

    @Test
    fun `test that return value is true` () {

    }

    @Test
    fun `test that return value is false` () {

    }
}