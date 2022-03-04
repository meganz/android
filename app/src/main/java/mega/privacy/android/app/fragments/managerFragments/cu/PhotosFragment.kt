package mega.privacy.android.app.fragments.managerFragments.cu

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.databinding.FragmentPhotosBinding
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.fragment.BaseZoomFragment
import mega.privacy.android.app.jobservices.CameraUploadsService
import mega.privacy.android.app.lollipop.FileStorageActivity
import mega.privacy.android.app.lollipop.ManagerActivity
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.ColorUtils.DARK_IMAGE_ALPHA
import mega.privacy.android.app.utils.ColorUtils.setImageViewAlphaIfDark
import mega.privacy.android.app.utils.Constants.PHOTO_SYNC_ADAPTER
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.ZoomUtil.PHOTO_ZOOM_LEVEL
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import nz.mega.sdk.MegaChatApiJava

@AndroidEntryPoint
class PhotosFragment : BaseZoomFragment() {

    override val viewModel by viewModels<PhotosViewModel>()

    private lateinit var binding: FragmentPhotosBinding

    /**
     * Current order.
     */
    private var order = 0

    private var alertDialog: AlertDialog? = null
    private var dialogIsShowing = false

    private lateinit var startForResult: ActivityResultLauncher<Intent>

    companion object {
        private const val DIALOG_IS_SHOWING = "DIALOG_IS_SHOWING"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        order = viewModel.getOrder()
        if (savedInstanceState != null) {
            dialogIsShowing = savedInstanceState.getBoolean(
                DIALOG_IS_SHOWING,
                false
            )
        }

        startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                logDebug("resultCode = ${result.resultCode}")
                setCameraUploadLocalPath(result)
                startCU()
            } else {
                // Set false to unblock the camera upload service
                CameraUploadsService.setInCameraUploadsSetting(false)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(
            DIALOG_IS_SHOWING,
            dialogIsShowing
        )
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPhotosBinding.inflate(inflater, container, false)

        if (mManagerActivity.firstLogin || viewModel.isEnableCUShown()) {
            viewModel.setEnableCUShown(true)
            createCameraUploadsViewForFirstLogin()
        } else {
            showPhotosGrid()
            setupBinding()
        }

        adapterType = PHOTO_SYNC_ADAPTER
        listView = binding.cuList
        scroller = binding.scroller
        viewTypePanel = mManagerActivity.findViewById(R.id.cu_view_type)
        yearsButton = viewTypePanel.findViewById(R.id.years_button)
        monthsButton = viewTypePanel.findViewById(R.id.months_button)
        daysButton = viewTypePanel.findViewById(R.id.days_button)
        allButton = viewTypePanel.findViewById(R.id.all_button)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAfterViewCreated()
    }

    fun onBackPressed() = when {
        mManagerActivity.isFirstNavigationLevel -> {
            if (selectedView != ALL_VIEW) {
                mManagerActivity.enableHideBottomViewOnScroll(false)
                mManagerActivity.showBottomView()
            }
            0
        }

        isEnablePhotosFragmentShown() -> {
            skipCUSetup()
            1
        }

        else -> {
            mManagerActivity.invalidateOptionsMenu()
            mManagerActivity.setToolbarTitle()
            1
        }
    }

    fun onStoragePermissionRefused() {
        Util.showSnackbar(context, getString(R.string.on_refuse_storage_permission))
        skipCUSetup()
    }

    private fun skipCUSetup() {
        viewModel.setEnableCUShown(false)
        viewModel.setCamSyncEnabled(false)
        mManagerActivity.isFirstNavigationLevel = false
        if (mManagerActivity.isFirstLogin) {
            mManagerActivity.skipInitialCUSetup()
        } else {
            mManagerActivity.refreshPhotosFragment()
        }
    }

    private fun requestCameraUploadPermission(permissions: Array<String>, requestCode: Int) {
        requestPermission(mManagerActivity, requestCode, *permissions)
    }

    fun enableCu() {
        viewModel.enableCu(
            binding.fragmentPhotosFirstLogin.cellularConnectionSwitch.isChecked,
            binding.fragmentPhotosFirstLogin.uploadVideosSwitch.isChecked
        )

        showCameraUploadSettingDialog()
    }

    private fun startCU() {
        callManager {
            it.refreshPhotosFragment()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            logDebug("Starting CU")
            JobUtil.startCameraUploadService(context)
        }, 1000)
    }

    /**
     * Set the local path for camera uploading
     *
     * @param result An instance obtained form Activity.onActivityResult that contains the path info
     */
    private fun setCameraUploadLocalPath(result: ActivityResult) {
        val prefs = dbH.preferences
        // Set false to unblock the camera upload service
        CameraUploadsService.setInCameraUploadsSetting(false)

        if (result.data != null) {
            // Set isFirstLogin to false after selecting a folder for upload
            mManagerActivity.isFirstLogin = false
            viewModel.setEnableCUShown(false)

            val cameraPath: String? =
                result.data?.getStringExtra(FileStorageActivity.EXTRA_PATH)

            cameraPath?.let {
                val isExternalSDCardCU = SDCardUtils.isLocalFolderOnSDCard(
                    context,
                    cameraPath
                ) && !FileUtil.isBasedOnFileStorage()

                val camSyncLocalPath =
                    if (isExternalSDCardCU) SDCardUtils.getSDCardDirName(
                        Uri.parse(
                            prefs.uriExternalSDCard
                        )
                    ) else cameraPath

                prefs.camSyncLocalPath = camSyncLocalPath
                dbH.setCamSyncLocalPath(camSyncLocalPath)
                dbH.setCamSyncEnabled(true)
            }
        }
    }

    /**
     * Refresh view and layout after CU enabled or disabled.
     */
    fun refreshViewLayout() {
        if (isEnablePhotosFragmentShown()) {
            showEnablePage()
            createCameraUploadsViewForFirstLogin()
        } else {
            showPhotosGrid()
        }
        initAfterViewCreated()
    }

    /**
     * Show photos view.
     */
    private fun showPhotosGrid() {
        binding.fragmentPhotosFirstLogin.root.visibility = View.GONE
        binding.fragmentPhotosGrid.visibility = View.VISIBLE
    }

    /**
     * Show enable CU page.
     */
    private fun showEnablePage() {
        binding.fragmentPhotosFirstLogin.root.visibility = View.VISIBLE
        binding.fragmentPhotosGrid.visibility = View.GONE
    }

    private fun createCameraUploadsViewForFirstLogin() {
        viewModel.setInitialPreferences()
        ListenScrollChangesHelper().addViewToListen(
            binding.fragmentPhotosFirstLogin.camSyncScrollView
        ) { _, _, _, _, _ ->
            mManagerActivity
                .changeAppBarElevation(
                    binding.fragmentPhotosFirstLogin.camSyncScrollView.canScrollVertically(
                        Constants.SCROLLING_UP_DIRECTION
                    )
                )
        }
        binding.fragmentPhotosFirstLogin.enableButton.setOnClickListener {
            MegaApplication.getInstance().sendSignalPresenceActivity()
            val permissions =
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (hasPermissions(context, *permissions)) {
                mManagerActivity.checkIfShouldShowBusinessCUAlert()
            } else {
                requestCameraUploadPermission(
                    permissions,
                    Constants.REQUEST_CAMERA_ON_OFF_FIRST_TIME
                )
            }
        }
    }

    /**
     * Init UI and view model when view is created or refreshed.
     */
    private fun initAfterViewCreated() {
        if (viewModel.isEnableCUShown()) {
            mManagerActivity.updateCULayout(View.GONE)
            mManagerActivity.updateCUViewTypes(View.GONE)
            binding.fragmentPhotosFirstLogin.uploadVideosSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    mManagerActivity.showSnackbar(
                        Constants.DISMISS_ACTION_SNACKBAR,
                        StringResourcesUtils.getString(R.string.video_quality_info),
                        MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                    )
                }
                binding.fragmentPhotosFirstLogin.qualityText.visibility =
                    if (isChecked) View.VISIBLE else View.GONE
            }

            setupCameraUploadDialog()
            handlePhotosMenuUpdate(false)
            return
        }

        mManagerActivity.updateCUViewTypes(View.VISIBLE)
        val currentZoom = PHOTO_ZOOM_LEVEL
        zoomViewModel.setCurrentZoom(currentZoom)
        zoomViewModel.setZoom(currentZoom)
        viewModel.mZoom = currentZoom

        setupOtherViews()
        setupListView()
        setupTimePanel()
        setupListAdapter(currentZoom, viewModel.items.value)
        subscribeObservers()
    }

    /**
     * Check the status of dialog and show it when needed.
     */
    private fun setupCameraUploadDialog() {
        if (dialogIsShowing) {
            showCameraUploadSettingDialog()
        }
    }

    private fun setupBinding() {
        binding.apply {
            viewModel = this@PhotosFragment.viewModel
            lifecycleOwner = viewLifecycleOwner
        }
    }

    private fun setupOtherViews() {
        binding.emptyEnableCuButton.setOnClickListener { enableCUClick() }
        setImageViewAlphaIfDark(context, binding.emptyHintImage, DARK_IMAGE_ALPHA)
        binding.emptyHintText.text = HtmlCompat.fromHtml(
            TextUtil.formatEmptyScreenText(
                context,
                StringResourcesUtils.getString(R.string.photos_empty)
            ),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    fun enableCUClick() {
        ((context as Activity).application as MegaApplication).sendSignalPresenceActivity()
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (hasPermissions(context, *permissions)) {
            viewModel.setEnableCUShown(true)
            mManagerActivity.refreshPhotosFragment()
        } else {
            requestCameraUploadPermission(permissions, Constants.REQUEST_CAMERA_ON_OFF)
        }
    }

    private fun subscribeObservers() {
        viewModel.items.observe(viewLifecycleOwner) {
            // On enable CU page, don't update layout and view.
            if (isEnablePhotosFragmentShown() || !mManagerActivity.isInPhotosPage) return@observe

            // Order changed.
            if (order != viewModel.getOrder()) {
                setupListAdapter(getCurrentZoom(), it)
                order = viewModel.getOrder()
            }

            actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.type != GalleryItem.TYPE_HEADER })
            if (it.isEmpty()) {
                handleOptionsMenuUpdate(false)
                viewTypePanel.visibility = View.GONE
            } else {
                handleOptionsMenuUpdate(shouldShowZoomMenuItem())
                viewTypePanel.visibility = View.VISIBLE
            }

            updateEnableCUButtons(gridAdapterHasData = it.isNotEmpty(), viewModel.isCUEnabled())
            binding.emptyHint.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            listView.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
            binding.scroller.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
            mManagerActivity.updateCUViewTypes(if (it.isEmpty()) View.GONE else View.VISIBLE)
        }

        viewModel.camSyncEnabled().observe(
            viewLifecycleOwner
        ) {
            updateEnableCUButtons(cuEnabled = it)
        }
    }

    /**
     * Updates CU enable buttons visibility depending on if CU is enabled/disabled
     * and if the view contains some node.
     *
     * @param cuEnabled True if CU is enabled, false otherwise.
     */
    private fun updateEnableCUButtons(
        gridAdapterHasData: Boolean = gridAdapterHasData(),
        cuEnabled: Boolean
    ) {
        binding.emptyEnableCuButton.visibility =
            if (!cuEnabled && !gridAdapterHasData) View.VISIBLE else View.GONE
        mManagerActivity.updateEnableCUButton(
            if (selectedView == ALL_VIEW && !cuEnabled
                && gridAdapterHasData && actionMode == null
            ) View.VISIBLE else View.GONE
        )
        if (!cuEnabled) {
            hideCUProgress()
        }
    }

    /**
     * this method handle is show menu.
     *
     * @return false, when no photo here or in the action mode or not in all view, then will hide the menu.
     * Otherwise, true, show menu.
     */
    private fun isShowMenu() =
        gridAdapterHasData() && actionMode == null && selectedView == ALL_VIEW && !viewModel.isEnableCUShown()

    fun isEnablePhotosFragmentShown() = viewModel.isEnableCUShown()

    fun shouldShowFullInfoAndOptions() =
        !isEnablePhotosFragmentShown() && selectedView == ALL_VIEW

    /**
     * First make all the buttons unselected,
     * then apply selected style for the selected button regarding to the selected view.
     */
    override fun updateViewSelected() {
        super.updateViewSelected()
        updateFastScrollerVisibility()
        mManagerActivity.enableHideBottomViewOnScroll(selectedView != ALL_VIEW)
        mManagerActivity.updateEnableCUButton(
            if (selectedView == ALL_VIEW && gridAdapterHasData() && !viewModel.isCUEnabled()
            ) View.VISIBLE else View.GONE
        )
        if (selectedView != ALL_VIEW) {
            hideCUProgress()
            binding.uploadProgress.visibility = View.GONE
        }
    }

    override fun setHideBottomViewScrollBehaviour() {
        mManagerActivity.showBottomView()
        mManagerActivity.enableHideBottomViewOnScroll(selectedView != ALL_VIEW)
    }

    /**
     * Hides CU progress bar and checks the scroll
     * in order to hide elevation if the list is not scrolled.
     */
    private fun hideCUProgress() {
        mManagerActivity.hideCUProgress()
        checkScroll()
    }

    fun checkScroll() {
        if (!this::binding.isInitialized || !listViewInitialized()) return

        val isScrolled = listView.canScrollVertically(Constants.SCROLLING_UP_DIRECTION)
        mManagerActivity.changeAppBarElevation(binding.uploadProgress.isVisible || isScrolled)
    }

    fun updateProgress(visibility: Int, pending: Int) {
        if (binding.uploadProgress.visibility != visibility) {
            binding.uploadProgress.visibility = visibility
            checkScroll()
        }
        binding.uploadProgress.text = StringResourcesUtils
            .getQuantityString(R.plurals.cu_upload_progress, pending, pending)
    }

    fun setDefaultView() {
        newViewClicked(ALL_VIEW)
    }

    override fun handleZoomChange(zoom: Int, needReload: Boolean) {
        PHOTO_ZOOM_LEVEL = zoom
        handleZoomAdapterLayoutChange(zoom)
        if (needReload) {
            loadPhotos()
        }
    }

    fun loadPhotos() {
        viewModel.loadPhotos(true)
    }

    private fun handleZoomAdapterLayoutChange(zoom: Int) {
        if (!viewModel.isEnableCUShown()) {
            viewModel.mZoom = zoom
            PHOTO_ZOOM_LEVEL = zoom
            if (layoutManagerInitialized()) {
                val state = layoutManager.onSaveInstanceState()
                setupListAdapter(zoom, viewModel.items.value)
                layoutManager.onRestoreInstanceState(state)
            }
        }
    }

    override fun handleOnCreateOptionsMenu() {
        handleOptionsMenuUpdate(isShowMenu())
    }

    override fun animateBottomView() {
        val hide = actionMode != null
        mManagerActivity.animateCULayout(hide || viewModel.isCUEnabled())
        mManagerActivity.animateBottomView(hide)
        mManagerActivity.setDrawerLockMode(hide)
        checkScroll()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (!isInPhotosPage()) {
            return
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (!isInPhotosPage()) {
            true
        } else super.onOptionsItemSelected(item)
    }

    fun isInPhotosPage(): Boolean {
        return activity as ManagerActivity? != null && (activity as ManagerActivity?)!!.isInPhotosPage
    }

    private fun handlePhotosMenuUpdate(isShowMenu: Boolean) {
        if (!isInPhotosPage()) {
            return
        }
        handleOptionsMenuUpdate(isShowMenu)
    }

    override fun getOrder() = viewModel.getOrder()

    /**
     * Show the dialog that can direct to the Camera Upload settings.
     */
    private fun showCameraUploadSettingDialog() {
        // Set true to block the camera upload service
        CameraUploadsService.setInCameraUploadsSetting(true)

        if (alertDialog != null) {
            alertDialog?.show()
            dialogIsShowing = true
        } else {
            alertDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_dcim_folder_dialog)
                .setMessage(R.string.content_dcim_folder_dialog)
                .setPositiveButton(R.string.button_continue) { dialog, _ ->
                    enableCameraUpload(
                        dialog,
                        false
                    )
                }
                .setNegativeButton(R.string.general_select_to_download) { dialog, _ ->
                    enableCameraUpload(
                        dialog,
                        true
                    )
                }
                .create()
                .also {
                    it.show()
                    it.setCanceledOnTouchOutside(false)
                    dialogIsShowing = true
                }
        }
    }

    /**
     * Enable camera upload
     *
     * @param dialog The dialog that was dismissed will be passed into the method
     * @param needSettings Need redirect to Settings or not
     */
    private fun enableCameraUpload(dialog: DialogInterface, needSettings: Boolean) {
        if (needSettings) {
            val intent = Intent(activity, FileStorageActivity::class.java)
            intent.action = FileStorageActivity.Mode.PICK_FOLDER.action
            intent.putExtra(
                FileStorageActivity.PICK_FOLDER_TYPE,
                FileStorageActivity.PickFolderType.CU_FOLDER.folderType
            )
            startForResult.launch(intent)
        } else {
            // Set isFirstLogin to false when the user cancels the dialog, then start uploading
            mManagerActivity.isFirstLogin = false
            viewModel.setEnableCUShown(false)
            CameraUploadsService.setInCameraUploadsSetting(false)
            startCU()
        }
        dialog.dismiss()
        dialogIsShowing = false
    }

}