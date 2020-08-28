package mega.privacy.android.app.fragments.offline

import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog.Builder
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.navArgs
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentOfflineFileInfoBinding
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.utils.autoCleared
import kotlin.math.abs

@AndroidEntryPoint
class OfflineFileInfoFragment : Fragment() {
    private val args: OfflineFileInfoFragmentArgs by navArgs()
    private var binding by autoCleared<FragmentOfflineFileInfoBinding>()
    private val viewModel: OfflineFileInfoViewModel by viewModels()

    private lateinit var upArrow: Drawable

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

        upArrow =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back_white)!!.mutate()
        binding.toolbar.setNavigationOnClickListener { requireActivity().finish() }

        observeLiveData()
        viewModel.loadNode(args.handle)
    }

    private fun observeLiveData() {
        viewModel.node.observe(viewLifecycleOwner) {
            if (it == null) {
                requireActivity().finish()
            } else {

                if (it.node.isFolder || it.thumbnail == null) {
                    binding.toolbarNodeIcon.setImageResource(
                        if (it.node.isFolder) {
                            R.drawable.ic_folder_list
                        } else {
                            MimeTypeList.typeForName(it.node.name).iconResourceId
                        }
                    )

                    binding.toolbarNodeIcon.isVisible = true
                    binding.containsTitle.isVisible = true
                    binding.containsValue.isVisible = true

                    requireActivity().window.statusBarColor =
                        ContextCompat.getColor(requireContext(), R.color.status_bar_search)
                    setColorFilterBlack()
                } else {
                    binding.toolbarFilePreview.isVisible = true
                    binding.preview.setImageURI(Uri.fromFile(it.thumbnail))

                    binding.collapseToolbar.setCollapsedTitleTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.name_my_account
                        )
                    )
                    binding.collapseToolbar.setExpandedTitleColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.white
                        )
                    )
                    binding.collapseToolbar.setStatusBarScrimColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.status_bar_search
                        )
                    )

                    binding.appBar.addOnOffsetChangedListener(OnOffsetChangedListener { appBarLayout, offset ->
                        if (offset == 0) {
                            // Expanded
                            setColorFilterWhite()
                        } else {
                            if (offset < 0 && abs(offset) >= appBarLayout.totalScrollRange / 2) {
                                // Collapsed
                                setColorFilterBlack()
                            } else {
                                setColorFilterWhite()
                            }
                        }
                    })
                }
                binding.collapseToolbar.title = it.node.name

                binding.availableOfflineSwitch.setOnClickListener { _ ->
                    binding.availableOfflineSwitch.isChecked = true
                    removeFromOffline(it.node) {
                        requireActivity().finish()
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

    private fun removeFromOffline(node: MegaOffline, onConfirmed: () -> Unit) {
        val dialogClickListener = OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    NodeController(requireContext()).deleteOffline(node)
                    onConfirmed()
                }
            }
        }

        Builder(requireContext())
            .setMessage(R.string.confirmation_delete_from_save_for_offline)
            .setPositiveButton(R.string.general_remove, dialogClickListener)
            .setNegativeButton(R.string.general_cancel, dialogClickListener)
            .show()
    }

    private fun setColorFilterBlack() {
        upArrow.colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP)
        binding.toolbar.navigationIcon = upArrow
    }

    private fun setColorFilterWhite() {
        upArrow.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        binding.toolbar.navigationIcon = upArrow
    }
}
