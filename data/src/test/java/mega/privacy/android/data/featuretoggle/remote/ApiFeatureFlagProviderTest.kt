package mega.privacy.android.data.featuretoggle.remote

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.featureflag.FlagMapper
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.ApiFeature
import mega.privacy.android.domain.entity.featureflag.Flag
import mega.privacy.android.domain.entity.featureflag.GroupFlagTypes
import nz.mega.sdk.MegaFlag
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiFeatureFlagProviderTest {

    private lateinit var underTest: ApiFeatureFlagProvider

    private val megaApiGateway = mock<MegaApiGateway>()
    private val flagMapper = mock<FlagMapper>()
    private val appEventGateway = mock<AppEventGateway>()

    @BeforeEach
    internal fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = ApiFeatureFlagProvider(
            ioDispatcher = UnconfinedTestDispatcher(),
            megaApiGateway = megaApiGateway,
            flagMapper = flagMapper,
            appEventGateway = appEventGateway
        )
    }

    @AfterEach
    internal fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    internal fun `test that null is returned if feature flag is not ApiFeature`() = runTest {
        val feature = mock<Feature> {
            on { name } doReturn "feature"
        }
        assertThat(underTest.isEnabled(feature)).isNull()
    }

    @Test
    internal fun `test that null is returned if check remote is false`() = runTest {
        val feature = mock<ApiFeature> {
            on { checkRemote } doReturn false
        }
        assertThat(underTest.isEnabled(feature)).isNull()
    }

    @Test
    internal fun `test that true  is returned when feature flag in enabled`() = runTest {
        val feature = mock<ApiFeature> {
            on { experimentName } doReturn "chmon"
            on { checkRemote } doReturn true
            on { mapValue(GroupFlagTypes.Enabled) } doReturn true
        }
        val megaFlag = mock<MegaFlag> {
            on { group } doReturn 1L
        }
        val flag = mock<Flag> {
            on { group } doReturn GroupFlagTypes.Enabled
        }
        whenever(megaApiGateway.getFlag(feature.experimentName, true)).thenReturn(megaFlag)
        whenever(flagMapper(megaFlag)).thenReturn(flag)
        appEventGateway.stub { on { monitorMiscLoaded() } doReturn flowOf(true) }

        val expected = underTest.isEnabled(feature)
        assertThat(expected).isTrue()
    }

    @Test
    internal fun `test that false  is returned when feature flag in disabled`() = runTest {
        val feature = mock<ApiFeature> {
            on { experimentName } doReturn "chmon"
            on { checkRemote } doReturn true
            on { mapValue(GroupFlagTypes.Disabled) } doReturn false
        }
        val megaFlag = mock<MegaFlag> {
            on { group } doReturn 0L
        }
        val flag = mock<Flag> {
            on { group } doReturn GroupFlagTypes.Disabled
        }
        appEventGateway.stub { on { monitorMiscLoaded() } doReturn flowOf(true) }

        whenever(megaApiGateway.getFlag(feature.experimentName, true)).thenReturn(megaFlag)
        whenever(flagMapper(megaFlag)).thenReturn(flag)

        val expected = underTest.isEnabled(feature)
        assertThat(expected).isFalse()
    }

    @Test
    fun `test that sdk is only called once misc flags have been loaded`() = runTest {
        val feature = mock<ApiFeature> {
            on { experimentName } doReturn "chmon"
            on { checkRemote } doReturn true
            on { mapValue(GroupFlagTypes.Enabled) } doReturn true
        }

        whenever(megaApiGateway.getFlag(feature.experimentName, true)).thenReturn(mock<MegaFlag>())
        whenever(flagMapper(any())).thenReturn(mock<Flag>())

        val miscLoadedFlow = MutableStateFlow(false)
        appEventGateway.stub { on { monitorMiscLoaded() } doReturn miscLoadedFlow }

        val job = launch { underTest.isEnabled(feature) }

        verifyNoInteractions(megaApiGateway)

        miscLoadedFlow.emit(true)
        advanceUntilIdle()

        verify(megaApiGateway).getFlag(feature.experimentName, true)

        job.cancel()
    }

    @Test
    fun `test that null is returned if misc flag does not return before timeout expires`() =
        runTest {
            val feature = mock<ApiFeature> {
                on { experimentName } doReturn "chmon"
                on { checkRemote } doReturn true
                on { mapValue(GroupFlagTypes.Enabled) } doReturn true
            }

            whenever(
                megaApiGateway.getFlag(
                    feature.experimentName,
                    true
                )
            ).thenReturn(mock<MegaFlag>())
            whenever(flagMapper(any())).thenReturn(mock<Flag>())

            val miscLoadedFlow = MutableStateFlow(false)
            appEventGateway.stub { on { monitorMiscLoaded() } doReturn miscLoadedFlow }

            val result = underTest.isEnabled(feature)
            advanceTimeBy(underTest.timeOut)

            assertThat(result).isNull()
        }
}
