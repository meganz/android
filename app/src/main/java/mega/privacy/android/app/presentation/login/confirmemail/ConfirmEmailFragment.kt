package mega.privacy.android.app.presentation.login.confirmemail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.confirmemail.view.ConfirmEmailRoute
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

/**
 * Confirm email fragment.
 */
@AndroidEntryPoint
class ConfirmEmailFragment : Fragment() {

    /**
     * Current theme
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    internal var onShowPendingFragment: ((fragmentType: LoginFragmentType) -> Unit)? = null
    internal var onSetTemporalEmail: ((email: String) -> Unit)? = null
    internal var onCancelConfirmationAccount: (() -> Unit)? = null

    private var emailTemp: String? = null
    private var firstNameTemp: String? = null

    /**
     * Called to do initial creation of a fragment.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        emailTemp = arguments?.getString(TEMPORARY_EMAIL_ARG)
        firstNameTemp = arguments?.getString(TEMPORARY_FIRST_NAME_ARG)
    }

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

            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                ConfirmEmailRoute(
                    modifier = Modifier
                        .systemBarsPadding()
                        .fillMaxSize(),
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

    companion object {
        internal const val TEMPORARY_EMAIL_ARG = "TEMPORARY_EMAIL_ARG"
        internal const val TEMPORARY_FIRST_NAME_ARG = "TEMPORARY_FIRST_NAME_ARG"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param tempEmail The temporary email.
         * @param tempFirstName The temporary first name.
         * @return A new instance of fragment ConfirmEmailFragmentV2.
         */
        @JvmStatic
        fun newInstance(tempEmail: String?, tempFirstName: String?) =
            ConfirmEmailFragment().apply {
                arguments = Bundle().apply {
                    putString(TEMPORARY_EMAIL_ARG, tempEmail)
                    putString(TEMPORARY_FIRST_NAME_ARG, tempFirstName)
                }
            }
    }
}
