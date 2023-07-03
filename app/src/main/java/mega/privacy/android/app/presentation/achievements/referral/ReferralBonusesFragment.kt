package mega.privacy.android.app.presentation.achievements.referral

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.achievements.referral.view.ReferralBonusRoute
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetThemeMode
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * ReferralBonusesFragment
 */
@AndroidEntryPoint
class ReferralBonusesFragment : Fragment() {
    /**
     * [MegaApiAndroid] injection
     */
    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    /**
     * ioDispatcher as [CoroutineDispatcher] injection
     */
    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    /**
     * Get system's default theme mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            AndroidTheme(isDark = themeMode.isDarkMode()) {
                ReferralBonusRoute()
            }
        }
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Activity actionbar has been created which might be accessed by UpdateUI().
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title =
            getString(R.string.title_referral_bonuses)
    }
}