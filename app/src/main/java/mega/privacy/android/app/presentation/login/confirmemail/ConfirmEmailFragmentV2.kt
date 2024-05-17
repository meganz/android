package mega.privacy.android.app.presentation.login.confirmemail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.login.confirmemail.view.ConfirmEmailRoute
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.theme.MegaAppTheme
import javax.inject.Inject

/**
 * The new confirm email fragment to replace the old [ConfirmEmailFragment] until fully-tested.
 */
@AndroidEntryPoint
class ConfirmEmailFragmentV2 : Fragment() {

    /**
     * Current theme
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * the temporary email set by the [LoginActivity]
     */
    var emailTemp: String? = null

    /**
     * the temporary first name set by the [LoginActivity]
     */
    var firstNameTemp: String? = null

    internal var onShowPendingFragment: ((fragmentType: LoginFragmentType) -> Unit)? = null
    internal var onSetTemporalEmail: ((email: String) -> Unit)? = null
    internal var onCancelConfirmationAccount: (() -> Unit)? = null

    /**
     * Called to have the fragment instantiate its user interface view.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            MegaAppTheme(isDark = themeMode.isDarkMode()) {
                ConfirmEmailRoute(
                    modifier = Modifier.fillMaxSize(),
                    email = emailTemp.orEmpty(),
                    fullName = firstNameTemp,
                    onShowPendingFragment = {
                        onShowPendingFragment?.invoke(it)
                    },
                    onSetTemporalEmail = {
                        onSetTemporalEmail?.invoke(it)
                    },
                    onCancelConfirmationAccount = {
                        onCancelConfirmationAccount?.invoke()
                    }
                )
            }
        }
    }

    /**
     * Called when the view previously created by onCreateView has been detached from the fragment.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        onShowPendingFragment = null
        onSetTemporalEmail = null
        onCancelConfirmationAccount = null
    }
}
