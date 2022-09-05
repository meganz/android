package mega.privacy.android.app.presentation.settings.startSceen

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import mega.privacy.android.app.databinding.FragmentStartScreenSettingsBinding
import mega.privacy.android.app.presentation.settings.startSceen.util.StartScreenUtil.PHOTOS_BNV
import mega.privacy.android.app.presentation.settings.startSceen.util.StartScreenUtil.CHAT_BNV
import mega.privacy.android.app.presentation.settings.startSceen.util.StartScreenUtil.CLOUD_DRIVE_BNV
import mega.privacy.android.app.presentation.settings.startSceen.util.StartScreenUtil.HOME_BNV
import mega.privacy.android.app.presentation.settings.startSceen.util.StartScreenUtil.SHARED_ITEMS_BNV
import mega.privacy.android.app.utils.SharedPreferenceConstants.USER_INTERFACE_PREFERENCES

/**
 * Settings fragment to choose the preferred start screen.
 */
class StartScreenSettingsFragment : Fragment() {

    private val viewModel by activityViewModels<StartScreenViewModel>()

    private lateinit var binding: FragmentStartScreenSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStartScreenSettingsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initPreferences(
            requireContext().getSharedPreferences(USER_INTERFACE_PREFERENCES, Context.MODE_PRIVATE)
        )

        setupView()
        setupObservers()
    }

    private fun setupView() {
        hideChecks()

        binding.cloudLayout.setOnClickListener { viewModel.newScreenClicked(CLOUD_DRIVE_BNV) }
        binding.cuLayout.setOnClickListener { viewModel.newScreenClicked(PHOTOS_BNV) }
        binding.homeLayout.setOnClickListener { viewModel.newScreenClicked(HOME_BNV) }
        binding.chatLayout.setOnClickListener { viewModel.newScreenClicked(CHAT_BNV) }
        binding.sharedLayout.setOnClickListener { viewModel.newScreenClicked(SHARED_ITEMS_BNV) }
    }

    private fun setupObservers() {
        viewModel.onScreenChecked().observe(viewLifecycleOwner, ::setScreenChecked)
    }

    private fun hideChecks() {
        binding.cloudCheck.isVisible = false
        binding.cuCheck.isVisible = false
        binding.homeCheck.isVisible = false
        binding.chatCheck.isVisible = false
        binding.sharedCheck.isVisible = false
    }

    /**
     * Updates the screen checked.
     *
     * @param screenChecked New screen checked.
     */
    private fun setScreenChecked(screenChecked: Int) {
        hideChecks()

        when (screenChecked) {
            CLOUD_DRIVE_BNV -> binding.cloudCheck.isVisible = true
            PHOTOS_BNV -> binding.cuCheck.isVisible = true
            HOME_BNV -> binding.homeCheck.isVisible = true
            CHAT_BNV -> binding.chatCheck.isVisible = true
            SHARED_ITEMS_BNV -> binding.sharedCheck.isVisible = true
        }
    }
}