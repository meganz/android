package mega.privacy.android.app.uploadFolder

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.databinding.ActivityUploadFolderBinding
import mega.privacy.android.app.extensions.consumeInsetsWithToolbar
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.modalbottomsheet.SortByBottomSheetDialogFragment.Companion.newInstance
import mega.privacy.android.app.namecollision.NameCollisionActivity
import mega.privacy.android.app.namecollision.data.NameCollisionResultUiEntity
import mega.privacy.android.app.namecollision.data.NameCollisionUiEntity
import mega.privacy.android.core.sharedcomponents.serializable
import mega.privacy.android.app.presentation.settings.model.storageTargetPreference
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.view.createStartTransferView
import mega.privacy.android.app.uploadFolder.list.adapter.FolderContentAdapter
import mega.privacy.android.app.uploadFolder.list.data.FolderContent
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_COLLISION_RESULTS
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.ORDER_OFFLINE
import mega.privacy.android.app.utils.MenuUtils.setupSearchView
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.ExtraConstant.EXTRA_ACTION_RESULT
import mega.privacy.android.navigation.MegaNavigator
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity which shows the content of a local folder picked via system picker to upload all its content
 * or part of it.
 */
@AndroidEntryPoint
class UploadFolderActivity : PasscodeActivity(), Scrollable {

    companion object {
        private const val WAIT_TIME_TO_UPDATE = 150L
        private const val SHADOW = 0.5f

        /**
         * Upload Folder Type
         */
        const val UPLOAD_FOLDER_TYPE = "UPLOAD_FOLDER_TYPE"
    }

    /**
     * Mega navigator
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

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

    private val rootFolderUri by lazy(LazyThreadSafetyMode.NONE) {
        Uri.fromFile(Environment.getExternalStorageDirectory())
    }

    private val deviceName by lazy(LazyThreadSafetyMode.NONE) {
        Util.getDeviceName()
    }

    private lateinit var searchMenuItem: MenuItem

    private lateinit var collisionsForResult: ActivityResultLauncher<Intent>

    private var onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (viewModel.back()) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private val type by lazy {
        intent.serializable<UploadFolderType>(UPLOAD_FOLDER_TYPE)
            ?: UploadFolderType.SELECT_AND_UPLOAD
    }

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        collisionsForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                when (result.resultCode) {
                    Activity.RESULT_OK -> {
                        val collisionsResult =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                result.data?.getParcelableArrayListExtra(
                                    INTENT_EXTRA_COLLISION_RESULTS,
                                    NameCollisionResultUiEntity::class.java
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                result.data?.getParcelableArrayListExtra(
                                    INTENT_EXTRA_COLLISION_RESULTS
                                )
                            }

                        viewModel.proceedWithUpload(collisionsResult)
                    }

                    Activity.RESULT_CANCELED -> {
                        finish()
                    }

                    else -> {
                        Timber.w("resultCode: ${result.resultCode}")
                    }
                }
            }
        enableEdgeToEdge()
        binding = ActivityUploadFolderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        consumeInsetsWithToolbar(customToolbar = binding.toolbar)
        setupView()
        setupObservers()

        if (savedInstanceState == null) {
            intent.data?.let { uri ->
                val documentFile = if (DocumentsContract.isTreeUri(uri)) {
                    DocumentFile.fromTreeUri(this, uri)
                } else {
                    DocumentFile.fromFile(uri.toFile())
                }
                documentFile?.let {
                    viewModel.retrieveFolderContent(
                        documentFile = documentFile,
                        parentHandle = intent.getLongExtra(
                            INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                            INVALID_HANDLE
                        ),
                        order = sortByHeaderViewModel.order.offlineSortOrder,
                        isList = sortByHeaderViewModel.isListView(),
                    )
                }
            }
        }
    }

    /**
     * onCreateOptionsMenu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_upload_folder, menu)
        searchMenuItem = menu.findItem(R.id.action_search).apply {
            isVisible = binding.progressBar.isVisible

            setupSearchView { query ->
                showProgress(true)
                viewModel.search(query)
                binding.uploadButton.isEnabled =
                    type != UploadFolderType.SINGLE_SELECT || query.isNullOrEmpty()
            }

            val query = viewModel.query

            if (!isActionViewExpanded && query != null) {
                expandActionView()
                (actionView as SearchView).setQuery(query, false)
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    /**
     * onOptionsItemSelected
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressedCallback.handleOnBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * checkScroll
     */
    override fun checkScroll() {
        val showElevation =
            binding.list.canScrollVertically(RecyclerView.NO_POSITION) || actionMode != null
                    || binding.progressBar.isVisible

        binding.toolbar.elevation = if (showElevation) elevation else 0F
    }

    /**
     * Setup the Views of the Activity
     */
    private fun setupView() {
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

        binding.cancelButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        if (type == UploadFolderType.SINGLE_SELECT) {
            binding.uploadButton.text =
                getString(mega.privacy.android.shared.resources.R.string.general_select_folder)
        }
        binding.uploadButton.setOnClickListener {
            if (type == UploadFolderType.SINGLE_SELECT) {
                setResult(
                    Activity.RESULT_OK, Intent().apply {
                        data = viewModel.getCurrentFolder().value?.uri
                    }
                )
                finish()
            } else {
                showProgress(true)
                viewModel.upload()
                actionMode?.finish()
                invalidateOptionsMenu()
            }
        }

        showProgress(true)
        binding.mainLayout.addView(
            createStartTransferView(
                this,
                viewModel.uiState.map { it.transferTriggerEvent },
                viewModel::consumeTransferTriggerEvent,
                navigateToStorageSettings = {
                    megaNavigator.openSettings(
                        this,
                        storageTargetPreference
                    )
                },
                onScanningFinished = { event ->
                    if (event is StartTransferEvent.FinishUploadProcessing) {
                        val total = ((event.triggerEvent as? TransferTriggerEvent.StartUpload.Files)
                            ?.pathsAndNames?.size ?: 0)
                        if (total > 0) {
                            resources.getQuantityString(
                                R.plurals.upload_began,
                                total,
                                total,
                            )
                        } else {
                            getString(R.string.no_uploads_empty_folder)
                        }.let { onActivityResult(it) }
                    }
                },
                onCancelNotEnoughSpaceForUploadDialog = { finish() }
            )
        )
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
            isEnabled = if (type == UploadFolderType.SINGLE_SELECT) {
                viewModel.query.isNullOrEmpty() && !show
            } else {
                !show
            }
        }

        if (this::searchMenuItem.isInitialized && !searchMenuItem.isActionViewExpanded) {
            searchMenuItem.isVisible = !show
        }

        checkScroll()
    }

    /**
     * Sets up the Observers
     */
    private fun setupObservers() {
        collectFlow(sortByHeaderViewModel.state) { state ->
            val viewType = state.viewType
            viewModel.setIsList(viewType == ViewType.LIST)
        }

        viewModel.getCurrentFolder().observe(this, ::showCurrentFolder)
        viewModel.getFolderItems().observe(this, ::showFolderContent)
        viewModel.getSelectedItems().observe(this, ::updateActionMode)
        viewModel.getCollisions().observe(this, ::manageCollisions)
        viewModel.onActionResult().observe(this, ::onActivityResult)

        sortByHeaderViewModel.showDialogEvent.observe(this, EventObserver {
            newInstance(ORDER_OFFLINE).apply { show(supportFragmentManager, this.tag) }
        })

        collectFlow(sortByHeaderViewModel.orderChangeState) { order ->
            folderContentAdapter.notifyItemChanged(0)
            viewModel.setOrder(order.offlineSortOrder)
        }
    }

    /**
     * Updates the action bar title with the current folder name.
     *
     * @param currentFolder Current folder.
     */
    private fun showCurrentFolder(currentFolder: FolderContent.Data) {
        supportActionBar?.title = if (currentFolder.uri == rootFolderUri) {
            deviceName
        } else {
            currentFolder.name
        }
    }

    /**
     * Shows the content of the current folder.
     *
     * @param folderContent Content to show.
     */
    private fun showFolderContent(folderContent: List<FolderContent>) {
        val isEmpty = folderContent.isEmpty()
        binding.emptyHintImage.isVisible = isEmpty
        binding.emptyHintText.isVisible = isEmpty
        folderContentAdapter.submitList(folderContent)
        binding.list.apply {
            isVisible = !isEmpty
            postDelayed({
                showProgress(false)
                if (isInList != viewModel.isInList()) {
                    switchViewType(if (viewModel.isInList()) ViewType.LIST else ViewType.GRID)
                }
            }, WAIT_TIME_TO_UPDATE)
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
                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean =
                    true

                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    actionMode = mode
                    checkScroll()
                    return true
                }

                override fun onActionItemClicked(
                    mode: ActionMode?,
                    item: MenuItem?,
                ): Boolean = true

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
     * @param collisions    List of [NameCollisionUiEntity] to manage.
     */
    private fun manageCollisions(collisions: ArrayList<NameCollision>) {
        collisionsForResult.launch(
            NameCollisionActivity.getIntentForFolderUpload(
                this,
                collisions = collisions
            )
        )
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
                if (this::searchMenuItem.isInitialized && searchMenuItem.isActionViewExpanded) {
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
        if (type == UploadFolderType.SINGLE_SELECT) return
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
                    sortByHeaderViewModel.isListView() -> itemView.findViewById(R.id.selected_icon)
                    else -> {
                        itemView.setBackgroundResource(R.drawable.background_item_grid_selected)
                        itemView.findViewById(R.id.selected_icon)
                    }
                }

                imageView.run {
                    setImageResource(CoreUiR.drawable.ic_select_folder)
                    isVisible = true

                    val animator =
                        AnimatorInflater.loadAnimator(context, R.animator.icon_select)
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
     * Switches how Items are being displayed
     *
     * @param viewType The View Type
     */
    private fun switchViewType(viewType: ViewType) {
        binding.list.apply {
            when (viewType) {
                ViewType.LIST -> {
                    switchToLinear()

                    if (itemDecorationCount == 0) {
                        addItemDecoration(itemDecoration)
                    }
                }

                ViewType.GRID -> {
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
}