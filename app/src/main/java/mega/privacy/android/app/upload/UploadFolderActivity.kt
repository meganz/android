package mega.privacy.android.app.upload

import android.app.Activity
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.databinding.ActivityUploadFolderBinding
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.modalbottomsheet.SortByBottomSheetDialogFragment.Companion.newInstance
import mega.privacy.android.app.upload.list.adapter.FolderContentListAdapter
import mega.privacy.android.app.upload.list.data.FolderContent
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util

/**
 * Activity which shows the content of a local folder picked via system picker to upload all its content
 * or part of it.
 */
class UploadFolderActivity : PasscodeActivity() {

    private val viewModel: UploadFolderViewModel by viewModels()
    private val sortByHeaderViewModel: SortByHeaderViewModel by viewModels()
    private lateinit var binding: ActivityUploadFolderBinding

    private val folderContentAdapter by lazy {
        FolderContentListAdapter(sortByHeaderViewModel, ::onFolderClick, ::onLongClick)
    }

    private val toolbarElevation by lazy { resources.getDimension(R.dimen.toolbar_elevation) }
    private val itemDecoration by lazy { PositionDividerItemDecoration(this, resources.displayMetrics) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUploadFolderBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setupView()
        setupObservers()

        if (savedInstanceState == null) {
            intent.data?.let { uri ->
                DocumentFile.fromTreeUri(this@UploadFolderActivity, uri)?.let { documentFile ->
                    viewModel.retrieveFolderContent(documentFile, sortByHeaderViewModel.order.third)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (viewModel.back()) {
            super.onBackPressed()
        }
    }

    fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        binding.list.apply {
            addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val showElevation = recyclerView.canScrollVertically(RecyclerView.NO_POSITION)
                    binding.toolbar.elevation = if (showElevation) toolbarElevation else 0F
                    if (Util.isDarkMode(this@UploadFolderActivity)) {
                        val color =
                            if (showElevation) R.color.action_mode_background else R.color.dark_grey
                        window.statusBarColor =
                            ContextCompat.getColor(this@UploadFolderActivity, color)
                    }
                }
            })

            adapter = folderContentAdapter
            setHasFixedSize(true)
        }

        binding.fastscroll.setRecyclerView(binding.list)

        switchListGrid(sortByHeaderViewModel.isList)

        binding.cancelButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.uploadButton.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    fun setupObservers() {
        viewModel.getCurrentFolder().observe(this, ::showCurrentFolder)
        viewModel.getFolderContent().observe(this, ::showFolderContent)
        sortByHeaderViewModel.showDialogEvent.observe(this, EventObserver {
            newInstance(Constants.ORDER_OFFLINE, false).apply {
                show(supportFragmentManager, this.tag)
            }
        })

        sortByHeaderViewModel.orderChangeEvent.observe(this, EventObserver { order ->
            folderContentAdapter.notifyItemChanged(0)
            viewModel.setOrder(order.third)
        })

        sortByHeaderViewModel.listGridChangeEvent.observe(this, EventObserver { isList ->
            switchListGrid(isList)
        })
    }

    private fun showCurrentFolder(currentFolder: FolderContent.Data) {
        supportActionBar?.title = currentFolder.name
    }

    private fun showFolderContent(folderContent: List<FolderContent>) {
        val isEmpty = folderContent.isEmpty()
        binding.emptyHintImage.isVisible = isEmpty
        binding.emptyHintText.isVisible = isEmpty
        binding.list.isVisible = !isEmpty
        folderContentAdapter.submitList(folderContent)
    }

    private fun onFolderClick(folderClicked: FolderContent.Data) {
        viewModel.folderClick(folderClicked)
    }

    private fun onLongClick(itemClicked: FolderContent.Data) {

    }

    private fun switchListGrid(isList: Boolean) {
        binding.list.apply {
            if (isList) {
                switchToLinear()

                if (itemDecorationCount == 0) {
                    addItemDecoration(itemDecoration)
                }
            } else {
                switchBackToGrid()
                (layoutManager as CustomizedGridLayoutManager).spanSizeLookup =
                    folderContentAdapter.getSpanSizeLookup((layoutManager as CustomizedGridLayoutManager).spanCount)

                if (itemDecorationCount > 0) {
                    post {
                        removeItemDecoration(itemDecoration)
                    }
                }
            }
        }
    }
}