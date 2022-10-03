package mega.privacy.android.app.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import mega.privacy.android.app.R
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.constants.StringsConstants.INVALID_CHARACTERS
import mega.privacy.android.app.databinding.FragmentImportFilesBinding
import mega.privacy.android.app.main.adapters.ImportFilesAdapter
import mega.privacy.android.app.main.adapters.ImportFilesAdapter.OnImportFilesAdapterFooterListener
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.domain.entity.ShareTextInfo

/**
 * Fragment for importing files.
 */
class ImportFilesFragment : Fragment(), OnImportFilesAdapterFooterListener {

    private lateinit var binding: FragmentImportFilesBinding

    private val viewModel: FileExplorerViewModel by activityViewModels()

    private var adapter: ImportFilesAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentImportFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupObservers()
        setupView()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        changeActionBarElevation()
    }

    private fun setupObservers() {
        with(viewModel) {
            filesInfo.observe(viewLifecycleOwner) { info: List<ShareInfo> ->
                showFilesInfo(info)
            }
            textInfo.observe(viewLifecycleOwner) { info: ShareTextInfo ->
                showImportingTextInfo(info)
            }
        }
    }

    /**
     * Shows the view when it is importing text.
     *
     * @param info ShareTextInfo containing all the required info to share the text.
     */
    private fun showImportingTextInfo(info: ShareTextInfo) {
        var setNames = true

        if (adapter == null) {
            adapter = ImportFilesAdapter(requireActivity(), info, nameFiles)
            setNames = false
        }

        val headerText: String = if (info.isUrl) {
            StringResourcesUtils.getString(R.string.file_properties_shared_folder_public_link_name)
        } else {
            StringResourcesUtils.getQuantityString(R.plurals.general_num_files,
                1)
        }

        setupAdapter(setNames, headerText)
    }

    /**
     * Shows the view when it is importing files.
     *
     * @param info List of ShareInfo containing all the required info to share the files.
     */
    private fun showFilesInfo(info: List<ShareInfo>) {
        var setNames = true

        if (adapter == null) {
            adapter = ImportFilesAdapter(requireActivity(), info, nameFiles)
            setNames = false
        }

        val headerText =
            StringResourcesUtils.getQuantityString(R.plurals.general_num_files, info.size)
        setupAdapter(setNames, headerText)
    }

    /**
     * Sets the adapter.
     *
     * @param setNames   True if should set the file names to the adapter, false otherwise.
     * @param headerText The text to show as the header of the content.
     */
    private fun setupAdapter(setNames: Boolean, headerText: String) {
        binding.contentText.text = headerText

        if (setNames) {
            adapter?.setImportNameFiles(nameFiles)
        }

        binding.fileListView.adapter = adapter
        adapter?.setFooterListener(this)
    }

    private fun setupView() {
        with(binding) {
            scrollContainerImport.setOnScrollChangeListener { _: View?, _: Int, _: Int, _: Int, _: Int -> changeActionBarElevation() }
            fileListView.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    /**
     * Checks if all the files to import have a right name.
     * - If so, shows the fragment which the user chose to import.
     * - If not, shows an error warning.
     *
     * @param fragment The fragment chosen by the user.
     */
    private fun confirmImport(fragment: Int) {
        adapter?.updateCurrentFocusPosition(binding.fileListView)

        val nameFiles = nameFiles
        var emptyNames = 0
        var wrongNames = 0

        for (name in nameFiles.values) {
            if (name.trim { it <= ' ' }.isEmpty()) {
                emptyNames++
            } else if (Constants.NODE_NAME_REGEX.matcher(name).find()) {
                wrongNames++
            }
        }

        if (wrongNames > 0 || emptyNames > 0) {
            val warning: String = when {
                emptyNames > 0 && wrongNames > 0 -> {
                    StringResourcesUtils.getString(R.string.general_incorrect_names)
                }
                emptyNames > 0 -> {
                    StringResourcesUtils.getQuantityString(R.plurals.empty_names, emptyNames)
                }
                else -> {
                    StringResourcesUtils.getString(R.string.invalid_characters_defined,
                        INVALID_CHARACTERS)
                }
            }

            (requireActivity() as FileExplorerActivity).showSnackbar(warning)
        } else {
            (requireActivity() as FileExplorerActivity).chooseFragment(fragment)
        }
    }

    /**
     * Changes action bar elevation.
     */
    fun changeActionBarElevation() {
        (requireActivity() as FileExplorerActivity).changeActionBarElevation(
            binding.scrollContainerImport.canScrollVertically(Constants.SCROLLING_UP_DIRECTION),
            FileExplorerActivity.IMPORT_FRAGMENT)
    }

    private val nameFiles: HashMap<String, String>
        get() = viewModel.fileNames.value!!

    override fun onClickCloudDriveButton() {
        confirmImport(FileExplorerActivity.CLOUD_FRAGMENT)
    }

    override fun onClickChatButton() {
        confirmImport(FileExplorerActivity.CHAT_FRAGMENT)
    }

    companion object {

        /**
         * Constant for temporary folder in which the thumbnails are stored.
         */
        const val THUMB_FOLDER = "ImportFilesThumb"

        /**
         * Creates a new instance of [ImportFilesFragment].
         *
         * @return The new instance.
         */
        fun newInstance(): ImportFilesFragment = ImportFilesFragment()
    }
}