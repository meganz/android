package mega.privacy.android.app.presentation.settings.compose.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.settings.compose.home.model.SettingsHomeState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.navigation.settings.FeatureSettingEntryPoint
import mega.privacy.android.navigation.settings.MoreSettingEntryPoint
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineMainDispatcherExtension::class)
class SettingHomeViewModelTest {
    private lateinit var underTest: SettingHomeViewModel

    private fun initUnderTest(
        featureEntryPoints: Set<FeatureSettingEntryPoint> = setOf(),
        moreEntryPoints: Set<MoreSettingEntryPoint> = setOf(),
    ) {
        underTest = SettingHomeViewModel(
            featureEntryPoints = featureEntryPoints,
            moreEntryPoints = moreEntryPoints
        )
    }


    @Test
    fun `test that an empty set of settings returns loading state`() = runTest {
        initUnderTest()
        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(
                SettingsHomeState.Loading(
                    featureEntryPoints = persistentListOf(),
                    moreEntryPoints = persistentListOf(),
                )
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

}