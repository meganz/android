package mega.privacy.android.app.presentation.photos.mediadiscovery

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.PhotoDownloaderViewModel
import mega.privacy.android.app.presentation.photos.mediadiscovery.actionMode.MediaDiscoveryActionModeCallback
import mega.privacy.android.app.presentation.photos.mediadiscovery.model.MediaDiscoveryViewState
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.presentation.photos.view.EmptyView
import mega.privacy.android.app.presentation.photos.view.CardListView
import mega.privacy.android.app.presentation.photos.view.FilterDialog
import mega.privacy.android.app.presentation.photos.view.PhotosGridView
import mega.privacy.android.app.presentation.photos.view.SortByDialog
import mega.privacy.android.app.presentation.photos.view.TimeSwitchBar
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.AUTHORITY_STRING_FILE_PROVIDER
import mega.privacy.android.app.utils.Constants.BUFFER_COMP
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetThemeMode
import java.io.File
import javax.inject.Inject

/**
 * New Album Content View
 */
@AndroidEntryPoint
class MediaDiscoveryFragment : Fragment() {

    private val photoDownloaderViewModel: PhotoDownloaderViewModel by viewModels()
    internal val mediaDiscoveryViewModel: MediaDiscoveryViewModel by viewModels()
    private val mediaDiscoveryZoomViewModel: MediaDiscoveryZoomViewModel by activityViewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode
    internal lateinit var managerActivity: ManagerActivity
    private var menu: Menu? = null

    // Action mode
    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: MediaDiscoveryActionModeCallback

    companion object {
        @JvmStatic
        fun getNewInstance(
            mediaHandle: Long,
            isOpenByMDIcon: Boolean = false,
        ): MediaDiscoveryFragment {
            return MediaDiscoveryFragment().apply {
                arguments = bundleOf(
                    INTENT_KEY_CURRENT_FOLDER_ID to mediaHandle,
                    INTENT_KEY_OPEN_MEDIA_DISCOVERY_BY_MD_ICON to isOpenByMDIcon
                )
            }
        }

        internal const val INTENT_KEY_CURRENT_FOLDER_ID = "CURRENT_FOLDER_ID"
        private const val INTENT_KEY_OPEN_MEDIA_DISCOVERY_BY_MD_ICON =
            "OPEN_MEDIA_DISCOVERY_BY_MD_ICON"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        managerActivity = activity as ManagerActivity
        actionModeCallback =
            MediaDiscoveryActionModeCallback(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val mode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                AndroidTheme(isDark = mode.isDarkMode()) {
                    MDContentBody(mediaDiscoveryViewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        setupFlow()
        setupParentActivityUI()
    }

    /**
     * Setup ManagerActivity UI
     */
    private fun setupParentActivityUI() {
        managerActivity.run {
            setToolbarTitle()
            invalidateOptionsMenu()
            hideFabButton()
        }
        managerActivity.invalidateOptionsMenu()
        managerActivity.hideFabButton()
    }

    private fun setupFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    mediaDiscoveryViewModel.state.collect { state ->
                        if (state.selectedPhotoIds.isEmpty()) {
                            if (actionMode != null) {
                                exitActionMode()
                            }
                        } else {
                            if (actionMode == null) {
                                enterActionMode()
                            }
                            actionMode?.title = state.selectedPhotoIds.size.toString()
                        }
                        menu?.let {
                            handleMenuIconsVisibility(
                                isShowing = state.selectedTimeBarTab == TimeBarTab.All
                                        && state.uiPhotoList.isNotEmpty()
                            )
                            if (state.selectedTimeBarTab == TimeBarTab.All && state.uiPhotoList.isNotEmpty()) {
                                handleZoomMenuEnableStatus()
                            }
                        }
                    }
                }

                launch {
                    mediaDiscoveryZoomViewModel.state.collect { zoomLevel ->
                        mediaDiscoveryViewModel.updateZoomLevel(zoomLevel)
                    }
                }
            }
        }
    }

    @Composable
    private fun Back() {
        val onBackPressedDispatcher =
            LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        LaunchedEffect(key1 = true) {
            onBackPressedDispatcher?.onBackPressed()
        }
    }

    @Composable
    fun MDContentBody(
        viewModel: MediaDiscoveryViewModel = viewModel(),
    ) {
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        if (uiState.shouldBack)
            Back()

        if (uiState.showSortByDialog) {
            SortByDialog(
                onDialogDismissed = {
                    viewModel.showSortByDialog(showSortByDialog = false)
                },
                selectedOption = uiState.currentSort,
                onOptionSelected = {
                    viewModel.setCurrentSort(it)
                }
            )
        }

        if (uiState.showFilterDialog) {
            FilterDialog(
                onDialogDismissed = {
                    viewModel.showFilterDialog(showFilterDialog = false)
                },
                selectedOption = uiState.currentMediaType,
                onOptionSelected = {
                    viewModel.setCurrentMediaType(it)
                }
            )
        }

        if (uiState.uiPhotoList.isNotEmpty()) {
            MDView(uiState)
        } else {
            EmptyView(uiState.currentMediaType)
        }
    }

    @Composable
    private fun MDView(
        uiState: MediaDiscoveryViewState,
    ) {
        val lazyGridState: LazyGridState =
            rememberSaveable(
                uiState.scrollStartIndex,
                uiState.scrollStartOffset,
                saver = LazyGridState.Saver,
            ) {
                LazyGridState(
                    uiState.scrollStartIndex,
                    uiState.scrollStartOffset,
                )
            }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (uiState.mediaDiscoveryViewSettings == MediaDiscoveryViewSettings.INITIAL.ordinal
                    && arguments?.getBoolean(
                        INTENT_KEY_OPEN_MEDIA_DISCOVERY_BY_MD_ICON,
                        false
                    ) == false
                ) {
                    MediaDiscoveryDialog()
                }
                if (uiState.selectedTimeBarTab == TimeBarTab.All) {
                    PhotosGridView(uiState = uiState, lazyGridState = lazyGridState)
                } else {
                    val dateCards = when (uiState.selectedTimeBarTab) {
                        TimeBarTab.Years -> uiState.yearsCardList
                        TimeBarTab.Months -> uiState.monthsCardList
                        TimeBarTab.Days -> uiState.daysCardList
                        else -> uiState.daysCardList
                    }
                    CardListView(dateCards = dateCards, lazyGridState = lazyGridState)
                }
            }
            if (uiState.selectedPhotoIds.isEmpty()) {
                TimeSwitchBar(uiState = uiState)
            }
        }
    }

    @Composable
    fun CardListView(
        dateCards: List<DateCard>,
        lazyGridState: LazyGridState,
    ) = CardListView(
        dateCards = dateCards,
        photoDownload = photoDownloaderViewModel::downloadPhoto,
        onCardClick = mediaDiscoveryViewModel::onCardClick,
        state = lazyGridState,
    )

    @Composable
    fun PhotosGridView(uiState: MediaDiscoveryViewState, lazyGridState: LazyGridState) =
        PhotosGridView(
            currentZoomLevel = uiState.currentZoomLevel,
            photoDownland = photoDownloaderViewModel::downloadPhoto,
            lazyGridState = lazyGridState,
            onClick = this::onClick,
            onLongPress = this::onLongPress,
            selectedPhotoIds = uiState.selectedPhotoIds,
            uiPhotoList = uiState.uiPhotoList,
        )

    @Composable
    fun TimeSwitchBar(uiState: MediaDiscoveryViewState) = TimeSwitchBar(
        selectedTimeBarTab = uiState.selectedTimeBarTab,
        onTimeBarTabSelected = mediaDiscoveryViewModel::onTimeBarTabSelected
    )

    /**
     * Media discovery view dialog
     */
    @Composable
    fun MediaDiscoveryDialog() {
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 3.dp)
                .shadow(
                    elevation = 10.dp,
                    ambientColor = colorResource(id = R.color.black),
                    spotColor = colorResource(id = R.color.black)
                )
                .zIndex(2f),
            color = Color.Transparent
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 30.dp, end = 20.dp),
            text = getString(R.string.cloud_drive_media_discovery_banner_context),
            color = colorResource(id = R.color.grey_alpha_087_white)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            SettingsButton()
            OKButton()
        }
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            color = colorResource(id = R.color.grey_012_white_015)
        )
    }

    /**
     * Settings button for media discovery view dialog
     */
    @Composable
    fun SettingsButton() {
        TextButton(modifier = Modifier.padding(end = 10.dp),
            colors = ButtonDefaults.buttonColors(
                contentColor = MaterialTheme.colors.secondary,
                backgroundColor = Color.Transparent
            ),
            onClick = {
                mediaDiscoveryViewModel.setMediaDiscoveryViewSettings(
                    MediaDiscoveryViewSettings.ENABLED.ordinal
                )
                requireContext().startActivity(
                    Intent(
                        requireActivity(),
                        SettingsActivity::class.java
                    )
                )
            }) {
            Text(
                text = getString(R.string.cloud_drive_media_discovery_banner_settings),
                fontSize = 16.sp
            )
        }
    }

    /**
     * Ok button for media discovery view dialog
     */
    @Composable
    fun OKButton() {
        TextButton(modifier = Modifier.padding(end = 8.dp),
            colors = ButtonDefaults.buttonColors(
                contentColor = MaterialTheme.colors.secondary,
                backgroundColor = Color.Transparent
            ),
            onClick = {
                mediaDiscoveryViewModel.setMediaDiscoveryViewSettings(
                    MediaDiscoveryViewSettings.ENABLED.ordinal
                )
            }) {
            Text(text = getString(R.string.cloud_drive_media_discovery_banner_ok), fontSize = 16.sp)
        }
    }

    private fun openPhoto(photo: Photo) {
        ImageViewerActivity.getIntentForChildren(
            requireContext(),
            mediaDiscoveryViewModel.getAllPhotoIds().toLongArray(),
            photo.id,
        ).run {
            startActivity(this)
        }
        managerActivity.overridePendingTransition(0, 0)
    }

    private fun onClick(photo: Photo) {
        if (mediaDiscoveryViewModel.state.value.selectedPhotoIds.isEmpty()) {
            if (photo is Photo.Video) {
                viewLifecycleOwner.lifecycleScope.launch {
                    launchVideoScreen(photo)
                }
            } else {
                openPhoto(photo)
            }
        } else if (actionMode != null) {
            mediaDiscoveryViewModel.togglePhotoSelection(photo.id)
        }
    }

    /**
     * Launch video player
     *
     * @param photo Photo item
     */
    private suspend fun launchVideoScreen(photo: Photo) {
        val nodeHandle = photo.id
        val nodeName = photo.name
        val intent = Util.getMediaIntent(requireActivity(), nodeName).apply {
            putExtra(Constants.INTENT_EXTRA_KEY_POSITION, 0)
            putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, nodeHandle)
            putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, nodeName)
            putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.FROM_MEDIA_DISCOVERY)
            putExtra(
                Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                mediaDiscoveryViewModel.getNodeParentHandle(nodeHandle)
            )
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(
            mediaDiscoveryViewModel.isLocalFile(nodeHandle)?.let { localPath ->
                File(localPath).let { mediaFile ->
                    kotlin.runCatching {
                        FileProvider.getUriForFile(
                            requireActivity(),
                            AUTHORITY_STRING_FILE_PROVIDER,
                            mediaFile
                        )
                    }.onFailure {
                        Uri.fromFile(mediaFile)
                    }.map { mediaFileUri ->
                        intent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(nodeName).type)
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                }
                intent
            } ?: let {
                val memoryInfo = ActivityManager.MemoryInfo()
                (requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                    .getMemoryInfo(memoryInfo)
                mediaDiscoveryViewModel.updateIntent(
                    handle = nodeHandle,
                    name = nodeName,
                    isNeedsMoreBufferSize = memoryInfo.totalMem > BUFFER_COMP,
                    intent = intent,
                )
            }
        )
    }

    private fun onLongPress(photo: Photo) {
        handleActionMode(photo)
    }

    private fun enterActionMode() {
        actionMode = managerActivity.startSupportActionMode(
            actionModeCallback
        )
        managerActivity.showHideBottomNavigationView(true)
    }

    private fun exitActionMode() {
        actionMode?.finish()
        actionMode = null
        managerActivity.showHideBottomNavigationView(false)
    }

    private fun handleActionMode(photo: Photo) {
        if (mediaDiscoveryViewModel.state.value.selectedPhotoIds.isEmpty()) {
            if (actionMode == null) {
                enterActionMode()
            }
            mediaDiscoveryViewModel.togglePhotoSelection(photo.id)
        } else {
            onClick(photo)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (!managerActivity.isInMDMode()) {
            return
        }
        inflater.inflate(R.menu.fragment_media_discovery_toolbar, menu)
        super.onCreateOptionsMenu(menu, inflater)
        this.menu = menu
        handleMenuIconsVisibility(
            isShowing = mediaDiscoveryViewModel.state.value.selectedTimeBarTab == TimeBarTab.All
                    && mediaDiscoveryViewModel.state.value.uiPhotoList.isNotEmpty()
        )
    }

    private fun handleMenuIconsVisibility(isShowing: Boolean) {
        this.menu?.apply {
            findItem(R.id.action_zoom_in)?.isVisible = isShowing
            findItem(R.id.action_zoom_out)?.isVisible = isShowing
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_zoom_in -> {
                mediaDiscoveryZoomViewModel.zoomIn()
            }

            R.id.action_zoom_out -> {
                mediaDiscoveryZoomViewModel.zoomOut()
            }

            R.id.action_menu_sort_by -> {
                mediaDiscoveryViewModel.showSortByDialog(showSortByDialog = true)
            }

            R.id.action_menu_filter -> {
                mediaDiscoveryViewModel.showFilterDialog(showFilterDialog = true)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleZoomMenuEnableStatus() {
        val isZoomInValid =
            mediaDiscoveryViewModel.state.value.currentZoomLevel != ZoomLevel.values()
                .first()
        val isZoomOutValid =
            mediaDiscoveryViewModel.state.value.currentZoomLevel != ZoomLevel.values()
                .last()
        this.menu?.let { menu ->
            menu.findItem(R.id.action_zoom_in)?.let {
                it.isEnabled = isZoomInValid
                it.icon?.alpha = if (isZoomInValid) 255 else 125
            }
            menu.findItem(R.id.action_zoom_out)?.let {
                it.isEnabled = isZoomOutValid
                it.icon?.alpha = if (isZoomOutValid) 255 else 125
            }
        }
    }
}

