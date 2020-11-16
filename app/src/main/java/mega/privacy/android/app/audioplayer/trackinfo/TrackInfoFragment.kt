package mega.privacy.android.app.audioplayer.trackinfo

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.audioplayer.AudioPlayerActivity
import mega.privacy.android.app.databinding.FragmentAudioTrackInfoBinding
import mega.privacy.android.app.utils.AlertsAndWarnings.Companion.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.autoCleared
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL

@AndroidEntryPoint
class TrackInfoFragment : Fragment() {
    private val args: TrackInfoFragmentArgs by navArgs()
    private var binding by autoCleared<FragmentAudioTrackInfoBinding>()
    private val viewModel by viewModels<TrackInfoViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAudioTrackInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AudioPlayerActivity).showToolbar(false)

        viewModel.metadata.observe(viewLifecycleOwner) {
            val metadata = it.first

            binding.title.text = metadata.title ?: metadata.nodeName

            if (metadata.title != null && metadata.artist != null) {
                binding.artist.isVisible = true
                binding.artist.text = metadata.artist
            } else {
                binding.artist.isVisible = false
            }

            if (metadata.album != null) {
                binding.album.isVisible = true
                binding.album.text = metadata.album
            } else {
                binding.album.isVisible = false
            }

            binding.duration.text = it.second
        }

        viewModel.nodeInfo.observe(viewLifecycleOwner) {
            if (it.thumbnail.exists()) {
                binding.cover.setImageURI(Uri.fromFile(it.thumbnail))
            }

            binding.availableOfflineSwitch.isEnabled = true
            binding.availableOfflineSwitch.isChecked = it.availableOffline
            binding.sizeValue.text = it.size
            binding.locationValue.text = it.location
            binding.addedValue.text = it.added
            binding.lastModifiedValue.text = it.lastModified
        }

        binding.availableOfflineSwitch.setOnClickListener {
            val isChecked = binding.availableOfflineSwitch.isChecked

            if (MegaApplication.getInstance().storageState == STORAGE_STATE_PAYWALL) {
                showOverDiskQuotaPaywallWarning()
                binding.availableOfflineSwitch.isChecked = !isChecked
                return@setOnClickListener
            }

            viewModel.toggleAvailableOffline(isChecked)
        }

        viewModel.loadTrackInfo(args)
    }
}
