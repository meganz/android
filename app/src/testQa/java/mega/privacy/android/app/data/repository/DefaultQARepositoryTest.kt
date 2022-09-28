package mega.privacy.android.app.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.repository.QARepository
import mega.privacy.android.domain.entity.Feature
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultQARepositoryTest{
    private lateinit var underTest: QARepository

    private val features = (0..4).map {
        object : Feature {
            override val name: String = it.toString()
            override val description: String = it.toString()
        }
    }

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        underTest = DefaultQARepository(
            distributionGateway = mock(),
            ioDispatcher = dispatcher,
            features = features.toSet(),
            featureFlagPreferencesGateway = mock(),
            booleanPreferenceMapper = mock()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that all features are returned`() = runTest {
        assertThat(underTest.getAllFeatures()).containsExactlyElementsIn(features)
    }

}