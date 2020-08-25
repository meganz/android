package mega.privacy.android.app.fragments.offline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentOfflineFileInfoBinding
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.autoCleared

@AndroidEntryPoint
class OfflineFileInfoFragment : Fragment() {
    private val args: OfflineFileInfoFragmentArgs by navArgs()
    private var binding by autoCleared<FragmentOfflineFileInfoBinding>()
    private val viewModel: OfflineFileInfoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOfflineFileInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        observeLiveData()
        viewModel.loadNode(args.handle)
    }

    private fun observeLiveData() {
        viewModel.node.observe(viewLifecycleOwner) {
            if (it == null) {
                findNavController().navigateUp()
            } else {
                if (it.isFolder) {
                    binding.fileIcon.setImageResource(R.drawable.ic_folder_list)
                } else {
                    binding.fileIcon.setImageResource(MimeTypeList.typeForName(it.name).iconResourceId)

                    binding.containsTitle.isVisible = false
                    binding.containsValue.isVisible = false
                }
                binding.filename.text = it.name

                binding.availableOfflineSwitch.setOnClickListener { _ ->
                    binding.availableOfflineSwitch.isChecked = true
                    (requireActivity() as ManagerActivityLollipop)
                        .showConfirmationRemoveFromOffline(it) {
                            findNavController().navigateUp()
                        }
                }
            }
        }
        viewModel.totalSize.observe(viewLifecycleOwner) {
            binding.totalSizeValue.text = it
        }
        viewModel.contains.observe(viewLifecycleOwner) {
            binding.containsValue.text = it
        }
        viewModel.added.observe(viewLifecycleOwner) {
            binding.addedValue.text = it
        }
    }
}
