package mega.privacy.android.app.upload

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.databinding.ActivityUploadFolderBinding
import mega.privacy.android.app.interfaces.Scrollable

/**
 * Activity which shows the content of a local folder picked via system picker to upload all its content
 * or part of it.
 */
class UploadFolderActivity : PasscodeActivity(), Scrollable {

    private val viewModel: UploadFolderViewModel by viewModels()
    private lateinit var binding: ActivityUploadFolderBinding

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

    fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                checkScroll()
            }
        })
    }

    fun setupObservers() {
        viewModel.getCurrentFolder().observe(this, ::showCurrentFolder)
        viewModel.getFolderContent().observe(this, ::showFolderContent)
    }

    private fun showCurrentFolder(currentFolder: String) {
        supportActionBar?.title = currentFolder
    }

    private fun showFolderContent(folderContent: List<DocumentFile>) {

    }

    override fun checkScroll() {
        TODO("Not yet implemented")
    }
}