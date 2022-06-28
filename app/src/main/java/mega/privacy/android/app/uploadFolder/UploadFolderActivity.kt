package mega.privacy.android.app.uploadFolder

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.TransfersManagementActivity
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.constants.EventConstants.EVENT_SCANNING_TRANSFERS_CANCELLED
import mega.privacy.android.app.databinding.ActivityUploadFolderBinding
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.modalbottomsheet.SortByBottomSheetDialogFragment.Companion.newInstance
import mega.privacy.android.app.namecollision.NameCollisionActivity
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.uploadFolder.list.adapter.FolderContentAdapter
import mega.privacy.android.app.uploadFolder.list.data.FolderContent
import mega.privacy.android.app.utils.Constants.EXTRA_ACTION_RESULT
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_COLLISION_RESULTS
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.LONG_SNACKBAR_DURATION
import mega.privacy.android.app.utils.Constants.ORDER_OFFLINE
import mega.privacy.android.app.utils.MenuUtils.setupSearchView
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber

/**
 * Activity which shows the content of a local folder picked via system picker to upload all its content
 * or part of it.
 */
class UploadFolderActivity : TransfersManagementActivity(), Scrollable {

    companion object {
        private const val WAIT_TIME_TO_UPDATE = 150L
        private const val SHADOW = 0.5f
    }

    private val viewModel: UploadFolderViewModel by viewModels()
    private val sortByHeaderViewModel: SortByHeaderViewModel by viewModels()
    private lateinit var binding: ActivityUploadFolderBinding

    private var actionMode: ActionMode? = null
    private var animatorSet: AnimatorSet? = null

    private val folderContentAdapter by lazy {
        FolderContentAdapter(sortByHeaderViewModel, ::onClick, ::onLongClick)
    }

    private val elevation by lazy { resources.getDimension(R.dimen.toolbar_elevation) }
    private val elevationColor by lazy {
        ContextCompat.getColor(this, R.color.action_mode_background)
    }
    private val noElevationColor by lazy { ContextCompat.getColor(this, R.color.dark_grey) }
    private val itemDecoration by lazy {
        PositionDividerItemDecoration(
            this,
            resources.displayMetrics
        )
    }

    private lateinit var searchMenuItem: MenuItem

    private lateinit var collisionsForResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        collisionsForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                when (result.resultCode) {
                    Activity.RESULT_OK -> {
                        @Suppress("UNCHECKED_CAST")
                        val collisionsResult =
                            result.data?.getSerializableExtra(INTENT_EXTRA_COLLISION_RESULTS)
                                    as List<NameCollisionResult>?
                        viewModel.proceedWithUpload(this, collisionsResult)
                    }
                    Activity.RESULT_CANCELED -> {
                        finish()
                    }
                    else -> {
                        Timber.w("resultCode: ${result.resultCode}")
                    }
                }
            }

        binding = ActivityUploadFolderBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setupView()
        setupObservers()

        if (savedInstanceState == null) {
            intent.data?.let { uri ->
                DocumentFile.fromTreeUri(this@UploadFolderActivity, uri)?.let { documentFile ->
                    viewModel.retrieveFolderContent(
                        documentFile,
                        intent.getLongExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE),
                        sortByHeaderViewModel.order.third,
                        sortByHeaderViewModel.isList
                    )
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_upload_folder, menu)
        searchMenuItem = menu.findItem(R.id.action_search).apply {
            isVisible = binding.progressBar.isVisible

            setupSearchView { query ->
                showProgress(true)
                viewModel.search(query)
            }

            val query = viewModel.query

            if (!isActionViewExpanded && query != null) {
                expandActionView()
                (actionView as SearchView).setQuery(query, false)
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        when {
            viewModel.back() -> super.onBackPressed()
        }
    }

    fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        binding.emptyHintImage.isVisible = false
        binding.emptyHintText.isVisible = false

        binding.list.apply {
            addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    checkScroll()
                }
            })

            adapter = folderContentAdapter
            setHasFixedSize(true)
            isVisible = false
        }

        binding.fastscroll.setRecyclerView(binding.list)

        switchListGrid(sortByHeaderViewModel.isList)

        binding.cancelButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.uploadButton.setOnClickListener {
            showProgress(true)
            viewModel.upload()
            actionMode?.finish()
            invalidateOptionsMenu()
        }

        showProgress(true)
    }

    /**
     * Shows or hides the progress view.
     *
     * @param show True if should show it, false otherwise.
     */
    private fun showProgress(show: Boolean) {
        binding.progressBar.isVisible = show
        val shadow = if (show) SHADOW else 1f
        binding.list.alpha = shadow
        binding.actionsView.alpha = shadow
        binding.cancelButton.apply {
            alpha = shadow
            isEnabled = !show
        }
        binding.uploadButton.apply {
            alpha = shadow
            isEnabled = !show
        }

        if (this::searchMenuItem.isInitialized && !searchMenuItem.isActionViewExpanded) {
            searchMenuItem.isVisible = !show
        }

        checkScroll()
    }

    fun setupObservers() {
        viewModel.getCurrentFolder().observe(this, ::showCurrentFolder)
        viewModel.getFolderItems().observe(this, ::showFolderContent)
        viewModel.getSelectedItems().observe(this, ::updateActionMode)
        viewModel.getCollisions().observe(this, ::manageCollisions)
        viewModel.onActionResult().observe(this, ::onActivityResult)

        sortByHeaderViewModel.showDialogEvent.observe(this, EventObserver {
            newInstance(ORDER_OFFLINE).apply { show(supportFragmentManager, this.tag) }
        })

        sortByHeaderViewModel.orderChangeEvent.observe(this, EventObserver { order ->
            folderContentAdapter.notifyItemChanged(0)
            viewModel.setOrder(order.third)
        })

        sortByHeaderViewModel.listGridChangeEvent.observe(this, EventObserver { isList ->
            switchListGrid(isList)
            viewModel.setIsList(isList)
        })

        LiveEventBus.get(EVENT_SCANNING_TRANSFERS_CANCELLED, Boolean::class.java)
            .observe(this) { cancelled ->
                if (cancelled) {
                    viewModel.cancelUpload()
                    Handler(Looper.getMainLooper()).postDelayed(::finish, LONG_SNACKBAR_DURATION)
                }
            }
    }

    /**
     * Updates the action bar title with the current folder name.
     *
     * @param currentFolder Current folder.
     */
    private fun showCurrentFolder(currentFolder: FolderContent.Data) {
        supportActionBar?.title = currentFolder.name
    }

    /**
     * Shows the content of the current folder.
     *
     * @param folderContent Content to show.
     */
    private fun showFolderContent(folderContent: List<FolderContent>) {
        if (viewModel.isSearchInProgress()) {
            return
        }

        val isEmpty = folderContent.isEmpty()
        binding.emptyHintImage.isVisible = isEmpty
        binding.emptyHintText.isVisible = isEmpty
        folderContentAdapter.submitList(folderContent)
        binding.list.apply {
            isVisible = !isEmpty
            postDelayed({ showProgress(false) }, WAIT_TIME_TO_UPDATE)
        }

        if (viewModel.query == null && this::searchMenuItem.isInitialized) {
            searchMenuItem.isVisible = !isEmpty
        }
    }

    /**
     * Updates the action mode depending on if there are selected items or not.
     *
     * @param selectedItems List of selected items.
     */
    private fun updateActionMode(selectedItems: List<Int>) {
        when {
            selectedItems.isEmpty() -> {
                actionMode?.finish()
            }
            actionMode == null -> startSupportActionMode(object : ActionMode.Callback {
                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = true

                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    actionMode = mode
                    checkScroll()
                    return true
                }

                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean = true

                override fun onDestroyActionMode(mode: ActionMode?) {
                    actionMode = null
                    animate(viewModel.clearSelected())
                    checkScroll()
                }
            })
        }

        actionMode?.title = selectedItems.size.toString()
    }

    /**
     * Manages name collisions if any. Proceeds with the upload if not.
     *
     * @param collisions    List of [NameCollision] to manage.
     */
    private fun manageCollisions(collisions: ArrayList<NameCollision>) {
        if (collisions.isEmpty()) {
            viewModel.proceedWithUpload(this)
        } else {
            collisionsForResult.launch(
                NameCollisionActivity.getIntentForFolderUpload(
                    this,
                    collisions = collisions
                )
            )
        }
    }

    /**
     * Sets the result and finishes the activity.
     *
     * @return result   Action result.
     */
    private fun onActivityResult(result: String?) {
        setResult(RESULT_OK, Intent().putExtra(EXTRA_ACTION_RESULT, result))
        finish()
    }

    /**
     * Performs the click on some item.
     *
     * @param itemClicked   Clicked item.
     * @param position      Position of the clicked item in the adapter.
     */
    private fun onClick(itemClicked: FolderContent.Data, position: Int) {
        when {
            binding.progressBar.isVisible -> return
            actionMode != null -> onLongClick(itemClicked, position)
            itemClicked.isFolder -> {
                showProgress(true)
                if (this::searchMenuItem.isInitialized) {
                    searchMenuItem.collapseActionView()
                }
                viewModel.folderClick(itemClicked)
            }
        }
    }

    /**
     * Performs the long click on some item.
     *
     * @param itemClicked   Clicked item.
     * @param position      Position of the clicked item in the adapter.
     */
    private fun onLongClick(itemClicked: FolderContent.Data, position: Int) {
        when {
            binding.progressBar.isVisible -> return
            else -> {
                animate(listOf(position))
                viewModel.itemLongClick(itemClicked)
            }
        }
    }

    /**
     * Animates some positions of the adapter when they are selected or un-selected.
     *
     * @param positions Adapter positions to animate.
     */
    private fun animate(positions: List<Int>) {
        if (positions.isEmpty()) {
            return
        }

        animatorSet?.run { if (isStarted) end() }
        val animatorList = mutableListOf<Animator>()

        positions.forEach { position ->
            binding.list.findViewHolderForAdapterPosition(position)?.apply {
                val imageView: ImageView = when {
                    sortByHeaderViewModel.isList -> itemView.findViewById(R.id.selected_icon)
                    else -> {
                        itemView.setBackgroundResource(R.drawable.background_item_grid_selected)
                        itemView.findViewById(R.id.selected_icon)
                    }
                }

                imageView.run {
                    setImageResource(R.drawable.ic_select_folder)
                    isVisible = true

                    val animator = AnimatorInflater.loadAnimator(context, R.animator.icon_select)
                    animator.setTarget(this)
                    animatorList.add(animator)
                }
            }
        }

        animatorSet = AnimatorSet().apply {
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {
                    viewModel.checkSelection()
                }

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationRepeat(animation: Animator) {}

            })

            playTogether(animatorList)
            start()
        }
    }

    /**
     * Switches the view from list to grid and vice versa.
     *
     * @param isList True if the view should be list, false if should be grid.
     */
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

    override fun checkScroll() {
        val showElevation =
            binding.list.canScrollVertically(RecyclerView.NO_POSITION) || actionMode != null
                    || binding.progressBar.isVisible

        binding.appBar.elevation = if (showElevation) elevation else 0F

        if (Util.isDarkMode(this@UploadFolderActivity)) {
            val color = if (showElevation) elevationColor else noElevationColor
            window.statusBarColor = color
            binding.toolbar.setBackgroundColor(color)
        }
    }
}