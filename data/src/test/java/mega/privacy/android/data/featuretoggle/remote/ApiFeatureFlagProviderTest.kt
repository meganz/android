package mega.privacy.android.data.featuretoggle.remote

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.featureflag.FlagMapper
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.ApiFeature
import mega.privacy.android.domain.entity.featureflag.Flag
import mega.privacy.android.domain.entity.featureflag.GroupFlagTypes
import mega.privacy.android.domain.entity.featureflag.MiscLoadedState
import mega.privacy.android.domain.repository.AccountRepository
import nz.mega.sdk.MegaFlag
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
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
    private val accountRepository = mock<AccountRepository>()
    private val featureFlagCache = hashMapOf<Feature, Boolean?>()

    @BeforeEach
    internal fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        featureFlagCache.clear()
        underTest = ApiFeatureFlagProvider(
            ioDispatcher = UnconfinedTestDispatcher(),
            megaApiGateway = megaApiGateway,
            flagMapper = flagMapper,
            accountRepository = accountRepository,
            featureFlagCache = featureFlagCache
        )
    }

    @AfterEach
    internal fun tearDown() {
        Dispatchers.resetMain()
        featureFlagCache.clear()
        reset(megaApiGateway, flagMapper, accountRepository)
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
            on { singleCheckPerRun } doReturn false
        }
        assertThat(underTest.isEnabled(feature)).isNull()
    }

    @Test
    internal fun `test that true  is returned when feature flag in enabled`() = runTest {
        val feature = mock<ApiFeature> {
            on { experimentName } doReturn "chmon"
            on { checkRemote } doReturn true
            on { singleCheckPerRun } doReturn false
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
        accountRepository.stub {
            on { monitorMiscState() } doReturn flowOf(MiscLoadedState.FlagsReady)
            on { getCurrentMiscState() } doReturn MiscLoadedState.FlagsReady
        }

        val expected = underTest.isEnabled(feature)
        assertThat(expected).isTrue()
    }

    @Test
    internal fun `test that false  is returned when feature flag in disabled`() = runTest {
        val feature = mock<ApiFeature> {
            on { experimentName } doReturn "chmon"
            on { checkRemote } doReturn true
            on { singleCheckPerRun } doReturn false
            on { mapValue(GroupFlagTypes.Disabled) } doReturn false
        }
        val megaFlag = mock<MegaFlag> {
            on { group } doReturn 0L
        }
        val flag = mock<Flag> {
            on { group } doReturn GroupFlagTypes.Disabled
        }
        accountRepository.stub {
            on { monitorMiscState() } doReturn flowOf(MiscLoadedState.FlagsReady)
            on { getCurrentMiscState() } doReturn MiscLoadedState.FlagsReady
        }

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
            on { singleCheckPerRun } doReturn false
            on { mapValue(GroupFlagTypes.Enabled) } doReturn true
        }

        whenever(megaApiGateway.getFlag(feature.experimentName, true)).thenReturn(mock<MegaFlag>())
        whenever(flagMapper(any())).thenReturn(mock<Flag>())

        val miscLoadedFlow = MutableStateFlow<MiscLoadedState>(MiscLoadedState.NotLoaded)
        accountRepository.stub {
            on { monitorMiscState() } doReturn miscLoadedFlow
            on { getCurrentMiscState() } doReturn MiscLoadedState.NotLoaded
        }

        val job = launch { underTest.isEnabled(feature) }

        verifyNoInteractions(megaApiGateway)

        miscLoadedFlow.emit(MiscLoadedState.FlagsReady)
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
                on { singleCheckPerRun } doReturn false
                on { mapValue(GroupFlagTypes.Enabled) } doReturn true
            }

            whenever(
                megaApiGateway.getFlag(
                    feature.experimentName,
                    true
                )
            ).thenReturn(mock<MegaFlag>())
            whenever(flagMapper(any())).thenReturn(mock<Flag>())

            val miscLoadedFlow = MutableStateFlow<MiscLoadedState>(MiscLoadedState.NotLoaded)
            accountRepository.stub {
                on { monitorMiscState() } doReturn miscLoadedFlow
                on { getCurrentMiscState() } doReturn MiscLoadedState.NotLoaded
            }

            val result = underTest.isEnabled(feature)
            advanceTimeBy(underTest.timeOut)

            assertThat(result).isNull()
        }

    @Test
    fun `test that fail safe triggers user data request when misc flags not loaded`() = runTest {
        val feature = mock<ApiFeature> {
            on { experimentName } doReturn "chmon"
            on { checkRemote } doReturn true
            on { singleCheckPerRun } doReturn false
            on { mapValue(GroupFlagTypes.Enabled) } doReturn true
        }

        val miscLoadedFlow = MutableStateFlow<MiscLoadedState>(MiscLoadedState.NotLoaded)
        accountRepository.stub {
            on { monitorMiscState() } doReturn miscLoadedFlow
            on { getCurrentMiscState() } doReturn MiscLoadedState.NotLoaded
        }

        val megaFlag = mock<MegaFlag> {
            on { group } doReturn 1L
        }
        val flag = mock<Flag> {
            on { group } doReturn GroupFlagTypes.Enabled
        }
        whenever(megaApiGateway.getFlag(feature.experimentName, true)).thenReturn(megaFlag)
        whenever(flagMapper(megaFlag)).thenReturn(flag)

        val job = async { underTest.isEnabled(feature) }

        advanceUntilIdle()

        verify(accountRepository).getUserData()

        miscLoadedFlow.emit(MiscLoadedState.FlagsReady)
        advanceUntilIdle()

        job.await()
    }

    @Test
    fun `test that cached value is returned when singleCheckPerRun is true and feature is in cache`() =
        runTest {
            val feature = mock<ApiFeature> {
                on { experimentName } doReturn "chmon"
                on { checkRemote } doReturn true
                on { singleCheckPerRun } doReturn true
            }

            // Pre-populate cache
            featureFlagCache[feature] = true

            val result = underTest.isEnabled(feature)

            assertThat(result).isTrue()
            verifyNoInteractions(megaApiGateway)
            verifyNoInteractions(accountRepository)
        }

    @Test
    fun `test that remote is checked when singleCheckPerRun is false even if feature is in cache`() =
        runTest {
            val feature = mock<ApiFeature> {
                on { experimentName } doReturn "chmon"
                on { checkRemote } doReturn true
                on { singleCheckPerRun } doReturn false
                on { mapValue(GroupFlagTypes.Enabled) } doReturn true
            }

            // Pre-populate cache
            featureFlagCache[feature] = false

            val megaFlag = mock<MegaFlag> {
                on { group } doReturn 1L
            }
            val flag = mock<Flag> {
                on { group } doReturn GroupFlagTypes.Enabled
            }
            whenever(megaApiGateway.getFlag(feature.experimentName, true)).thenReturn(megaFlag)
            whenever(flagMapper(megaFlag)).thenReturn(flag)
            accountRepository.stub {
                on { monitorMiscState() } doReturn flowOf(MiscLoadedState.FlagsReady)
                on { getCurrentMiscState() } doReturn MiscLoadedState.FlagsReady
            }

            val result = underTest.isEnabled(feature)

            assertThat(result).isTrue()
            verify(megaApiGateway).getFlag(feature.experimentName, true)
        }

    @Test
    fun `test that result is cached when singleCheckPerRun is true and result is not null`() =
        runTest {
            val feature = mock<ApiFeature> {
                on { experimentName } doReturn "chmon"
                on { checkRemote } doReturn true
                on { singleCheckPerRun } doReturn true
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
            accountRepository.stub {
                on { monitorMiscState() } doReturn flowOf(MiscLoadedState.FlagsReady)
                on { getCurrentMiscState() } doReturn MiscLoadedState.FlagsReady
            }

            val result = underTest.isEnabled(feature)

            assertThat(result).isTrue()
            assertThat(featureFlagCache[feature]).isTrue()
        }

    @Test
    fun `test that result is not cached when singleCheckPerRun is false`() = runTest {
        val feature = mock<ApiFeature> {
            on { experimentName } doReturn "chmon"
            on { checkRemote } doReturn true
            on { singleCheckPerRun } doReturn false
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
        accountRepository.stub {
            on { monitorMiscState() } doReturn flowOf(MiscLoadedState.FlagsReady)
            on { getCurrentMiscState() } doReturn MiscLoadedState.FlagsReady
        }

        val result = underTest.isEnabled(feature)

        assertThat(result).isTrue()
        assertThat(featureFlagCache.containsKey(feature)).isFalse()
    }

    @Test
    fun `test that result is not cached when singleCheckPerRun is true but result is null`() =
        runTest {
            val feature = mock<ApiFeature> {
                on { experimentName } doReturn "chmon"
                on { checkRemote } doReturn true
                on { singleCheckPerRun } doReturn true
            }

            whenever(megaApiGateway.getFlag(feature.experimentName, true)).thenReturn(null)
            accountRepository.stub {
                on { monitorMiscState() } doReturn flowOf(MiscLoadedState.FlagsReady)
                on { getCurrentMiscState() } doReturn MiscLoadedState.FlagsReady
            }

            val result = underTest.isEnabled(feature)

            assertThat(result).isNull()
            assertThat(featureFlagCache.containsKey(feature)).isFalse()
        }

    @Test
    fun `test that remote is not called when cached value exists for singleCheckPerRun feature`() =
        runTest {
            val feature = mock<ApiFeature> {
                on { experimentName } doReturn "chmon"
                on { checkRemote } doReturn true
                on { singleCheckPerRun } doReturn true
            }

            // Pre-populate cache with false
            featureFlagCache[feature] = false

            val result = underTest.isEnabled(feature)

            assertThat(result).isFalse()
            verifyNoInteractions(megaApiGateway)
            verifyNoInteractions(accountRepository)
        }
}
