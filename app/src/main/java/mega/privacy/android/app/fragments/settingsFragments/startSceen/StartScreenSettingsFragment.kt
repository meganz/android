package mega.privacy.android.app.fragments.settingsFragments.startSceen

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import mega.privacy.android.app.constants.SettingsConstants.USER_INTERFACE_PREFERENCES
import mega.privacy.android.app.databinding.FragmentStartScreenSettingsBinding

class StartScreenSettingsFragment : Fragment() {

    companion object {
        private const val CLOUD_DRIVE = 0
        private const val CAMERA_UPLOADS = 1
        private const val HOME = 2
        private const val CHAT = 3
        private const val SHARED_ITEMS = 4
    }

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

        binding.cloudLayout.setOnClickListener { viewModel.newScreenClicked(CLOUD_DRIVE) }
        binding.cuLayout.setOnClickListener { viewModel.newScreenClicked(CAMERA_UPLOADS) }
        binding.homeLayout.setOnClickListener { viewModel.newScreenClicked(HOME) }
        binding.chatLayout.setOnClickListener { viewModel.newScreenClicked(CHAT) }
        binding.sharedLayout.setOnClickListener { viewModel.newScreenClicked(SHARED_ITEMS) }
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
            CLOUD_DRIVE -> binding.cloudCheck.isVisible = true
            CAMERA_UPLOADS -> binding.cuCheck.isVisible = true
            HOME -> binding.homeCheck.isVisible = true
            CHAT -> binding.chatCheck.isVisible = true
            SHARED_ITEMS -> binding.sharedCheck.isVisible = true
        }
    }
}