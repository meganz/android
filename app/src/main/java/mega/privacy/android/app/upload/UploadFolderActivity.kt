package mega.privacy.android.app.upload

import android.app.Activity
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.databinding.ActivityUploadFolderBinding
import mega.privacy.android.app.upload.list.adapter.FolderContentListAdapter
import mega.privacy.android.app.upload.list.data.FolderContent

/**
 * Activity which shows the content of a local folder picked via system picker to upload all its content
 * or part of it.
 */
class UploadFolderActivity : PasscodeActivity() {

    private val viewModel: UploadFolderViewModel by viewModels()
    private lateinit var binding: ActivityUploadFolderBinding

    private val folderContentAdapter by lazy {
        FolderContentListAdapter(::onFolderClick, ::onLongClick)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUploadFolderBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setupView()
        setupObservers()

        if (savedInstanceState == null) {
            intent.data?.let { uri ->
                DocumentFile.fromTreeUri(this@UploadFolderActivity, uri)?.let { documentFile ->
                    viewModel.retrieveFolderContent(documentFile)
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
                }
            })

            adapter = folderContentAdapter
            setHasFixedSize(true)
        }

        binding.fastscroll.setRecyclerView(binding.list)

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
    }

    private fun showCurrentFolder(currentFolder: FolderContent.Data) {
        supportActionBar?.title = currentFolder.document.name
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
}