package mega.privacy.android.app.main;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.components.saver.NodeSaver;
import mega.privacy.android.app.namecollision.data.NameCollision;
import mega.privacy.android.app.generalusecase.FilePrepareUseCase;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.interfaces.ActionNodeCallback;
import mega.privacy.android.app.interfaces.UploadBottomSheetDialogActionListener;
import mega.privacy.android.app.main.controllers.NodeController;
import mega.privacy.android.app.modalbottomsheet.ContactFileListBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.UploadBottomSheetDialogFragment;
import mega.privacy.android.app.namecollision.NameCollisionActivity;
import mega.privacy.android.app.usecase.GetNodeUseCase;
import mega.privacy.android.app.usecase.MoveNodeUseCase;
import mega.privacy.android.app.usecase.data.MoveRequestResult;
import mega.privacy.android.app.usecase.exception.MegaNodeException;
import mega.privacy.android.app.utils.AlertDialogUtil;
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase;
import mega.privacy.android.app.utils.AlertsAndWarnings;
import mega.privacy.android.app.utils.MegaNodeDialogUtil;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.documentscanner.DocumentScannerActivity;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;

import static mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists;
import static mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.*;
import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.IS_NEW_TEXT_FILE_SHOWN;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.NEW_TEXT_FILE_TEXT;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.checkNewTextFileDialogState;
import static mega.privacy.android.app.utils.permission.PermissionUtils.*;
import static mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.UploadUtil.*;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import javax.inject.Inject;

@AndroidEntryPoint
public class ContactFileListActivity extends PasscodeActivity
		implements MegaGlobalListenerInterface, MegaRequestListenerInterface,
		UploadBottomSheetDialogActionListener, ActionNodeCallback, SnackbarShower {

	@Inject
	FilePrepareUseCase filePrepareUseCase;
	@Inject
	MoveNodeUseCase moveNodeUseCase;
	@Inject
	GetNodeUseCase getNodeUseCase;
	@Inject
	CheckNameCollisionUseCase checkNameCollisionUseCase;

	FrameLayout fragmentContainer;

	String userEmail;
	MegaUser contact;
	String fullName = "";

	AlertDialog permissionsDialog;

	ContactFileListFragment contactFileListFragment;

	private final NodeSaver nodeSaver = new NodeSaver(this, this, this,
			AlertsAndWarnings.showSaveToDeviceConfirmDialog(this));

	CoordinatorLayout coordinatorLayout;
	Handler handler;

	MenuItem shareMenuItem;
	MenuItem viewSharedItem;

	private final static String PARENT_HANDLE = "parentHandle";
	static ContactFileListActivity contactPropertiesMainActivity;

	long parentHandle = -1;

	DatabaseHandler dbH;

	private AlertDialog newFolderDialog;
	DisplayMetrics outMetrics;

	private androidx.appcompat.app.AlertDialog renameDialog;
	AlertDialog statusDialog;

	MegaNode selectedNode = null;

	Toolbar tB;
	ActionBar aB;

	private BottomSheetDialogFragment bottomSheetDialogFragment;

	private AlertDialog newTextFileDialog;

	private BroadcastReceiver manageShareReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null) return;

			if (contactFileListFragment != null) {
				contactFileListFragment.clearSelections();
				contactFileListFragment.hideMultipleSelect();
			}

			if (statusDialog != null) {
				statusDialog.dismiss();
			}
		}
	};

	private BroadcastReceiver destroyActionModeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null || intent.getAction() == null
					|| !intent.getAction().equals(BROADCAST_ACTION_DESTROY_ACTION_MODE))
				return;

			if (contactFileListFragment != null && contactFileListFragment.isVisible()) {
				contactFileListFragment.clearSelections();
				contactFileListFragment.hideMultipleSelect();
			}
		}
	};

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putLong(PARENT_HANDLE, parentHandle);
		checkNewTextFileDialogState(newTextFileDialog, outState);
		nodeSaver.saveState(outState);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		logDebug("onOptionsItemSelected");

		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
		} else if (item.getItemId() == R.id.action_more) {
			showOptionsPanel(megaApi.getNodeByHandle(parentHandle));
		}

		return true;
	}

	@Override
	public void uploadFiles() {
		chooseFiles(this);
	}

	@Override
	public void uploadFolder() {
		chooseFolder(this);
	}

	@Override
	public void takePictureAndUpload() {
		if (!hasPermissions(this, Manifest.permission.CAMERA)) {
			requestPermission(this, REQUEST_CAMERA, Manifest.permission.CAMERA);
			return;
		}
		if (!hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			requestPermission(this, REQUEST_WRITE_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
			return;
		}
		checkTakePicture(this, TAKE_PHOTO_CODE);
	}

    @Override
    public void scanDocument() {
        String[] saveDestinations = {
				StringResourcesUtils.getString(R.string.section_cloud_drive),
				StringResourcesUtils.getString(R.string.section_chat)
        };
        Intent intent = DocumentScannerActivity.getIntent(this, saveDestinations);
        startActivityForResult(intent, REQUEST_CODE_SCAN_DOCUMENT);
    }

	@Override
	public void showNewFolderDialog() {
		newFolderDialog = MegaNodeDialogUtil.showNewFolderDialog(this, this);
	}

	@Override
	public void showNewTextFileDialog(String typedName) {
		newTextFileDialog = MegaNodeDialogUtil.showNewTxtFileDialog(this,
				megaApi.getNodeByHandle(parentHandle), typedName, false);
	}

	@Override
	public void createFolder(@NotNull String title) {

		logDebug("createFolder");
		if (!isOnline(this)) {
			showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
			return;
		}

		if (isFinishing()) {
			return;
		}

		long parentHandle = contactFileListFragment.getParentHandle();

		MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);

		if (parentNode != null) {
			logDebug("parentNode != null: " + parentNode.getName());
			boolean exists = false;
			ArrayList<MegaNode> nL = megaApi.getChildren(parentNode);
			for (int i = 0; i < nL.size(); i++) {
				if (title.compareTo(nL.get(i).getName()) == 0) {
					exists = true;
				}
			}

			if (!exists) {
				statusDialog = null;
				try {
					statusDialog = createProgressDialog(this, getString(R.string.context_creating_folder));
					statusDialog.show();
				} catch (Exception e) {
					return;
				}

				megaApi.createFolder(title, parentNode, this);
			} else {
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_already_exists));
			}
		} else {
			logWarning("parentNode == null: " + parentHandle);
			parentNode = megaApi.getRootNode();
			if (parentNode != null) {
				logDebug("megaApi.getRootNode() != null");
				boolean exists = false;
				ArrayList<MegaNode> nL = megaApi.getChildren(parentNode);
				for (int i = 0; i < nL.size(); i++) {
					if (title.compareTo(nL.get(i).getName()) == 0) {
						exists = true;
					}
				}

				if (!exists) {
					statusDialog = null;
					try {
						statusDialog = createProgressDialog(this, getString(R.string.context_creating_folder));
						statusDialog.show();
					} catch (Exception e) {
						return;
					}

					megaApi.createFolder(title, parentNode, this);
				} else {
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_already_exists));
				}
			} else {
				return;
			}
		}
	}

	private void showKeyboardDelayed(final View view) {
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		logDebug("onCreate first");
		super.onCreate(savedInstanceState);

		if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
			return;
		}

		if (savedInstanceState == null) {
			this.setParentHandle(-1);
		} else {
			this.setParentHandle(savedInstanceState.getLong(PARENT_HANDLE, -1));

			nodeSaver.restoreState(savedInstanceState);
		}

		megaApi.addGlobalListener(this);

		contactPropertiesMainActivity = this;

		registerReceiver(manageShareReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_MANAGE_SHARE));
		registerReceiver(destroyActionModeReceiver,
				new IntentFilter(BROADCAST_ACTION_DESTROY_ACTION_MODE));

		handler = new Handler();
		dbH = DatabaseHandler.getDbHandler(this);

		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = getResources().getDisplayMetrics().density;

		float scaleW = getScaleW(outMetrics, density);
		float scaleH = getScaleH(outMetrics, density);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			userEmail = extras.getString(NAME);
			int currNodePosition = extras.getInt("node_position", -1);

			setContentView(R.layout.activity_main_contact_properties);

			coordinatorLayout = (CoordinatorLayout) findViewById(R.id.contact_properties_main_activity_layout);
			coordinatorLayout.setFitsSystemWindows(false);

			//Set toolbar
			tB = (Toolbar) findViewById(R.id.toolbar_main_contact_properties);
			if (tB == null) {
				logWarning("Toolbar is NULL");
			}

			setSupportActionBar(tB);
			aB = getSupportActionBar();

			contact = megaApi.getContact(userEmail);
			if (contact == null) {
				finish();
			}
			fullName = getMegaUserNameDB(contact);

			if (aB != null) {
				aB.setDisplayHomeAsUpEnabled(true);
				aB.setDisplayShowHomeEnabled(true);
				setTitleActionBar(null);
			} else {
				logWarning("aB is NULL!!!!");
			}

			fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container_contact_properties);

			logDebug("Shared Folders are:");
			coordinatorLayout.setFitsSystemWindows(true);

			contactFileListFragment = (ContactFileListFragment) getSupportFragmentManager().findFragmentByTag("cflF");

			if (contactFileListFragment == null) {
				contactFileListFragment = new ContactFileListFragment();
			}
			contactFileListFragment.setUserEmail(userEmail);
			contactFileListFragment.setCurrNodePosition(currNodePosition);
			contactFileListFragment.setParentHandle(parentHandle);

			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_contact_properties, contactFileListFragment, "cflF").commitNow();
			coordinatorLayout.invalidate();

			if (savedInstanceState != null && savedInstanceState.getBoolean(IS_NEW_TEXT_FILE_SHOWN, false)) {
				showNewTextFileDialog(savedInstanceState.getString(NEW_TEXT_FILE_TEXT));
			}
		}
	}

	public void showUploadPanel() {
		logDebug("showUploadPanel");
		if (!hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			requestPermission(this, REQUEST_READ_WRITE_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
		} else {
			onGetReadWritePermission();
		}
	}

	private void onGetReadWritePermission() {
        if (isBottomSheetDialogShown(bottomSheetDialogFragment)) return;
		UploadBottomSheetDialogFragment bottomSheetDialogFragment = new UploadBottomSheetDialogFragment();
		bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
	}

	@Override
	protected void onResume() {
		logDebug("onResume");
		super.onResume();

		Intent intent = getIntent();

		if (intent != null) {
			intent.setAction(null);
			setIntent(null);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		logDebug("onRequestPermissionsResult");
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
			return;
		}
		switch (requestCode) {
			case REQUEST_CAMERA: {
				if (!hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
					requestPermission(this, REQUEST_WRITE_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
				} else {
					takePictureAndUpload();
				}
				break;
			}
			case REQUEST_READ_WRITE_STORAGE: {
				logDebug("REQUEST_READ_WRITE_STORAGE");
				onGetReadWritePermission();
				break;
			}
		}

		nodeSaver.handleRequestPermissionsResult(requestCode);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		logDebug("onNewIntent");
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	protected void onDestroy() {
		logDebug("onDestroy()");

		super.onDestroy();

		if (megaApi != null) {
			megaApi.removeGlobalListener(this);
			megaApi.removeRequestListener(this);
		}

		unregisterReceiver(manageShareReceiver);
		unregisterReceiver(destroyActionModeReceiver);

		nodeSaver.destroy();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		logDebug("onPrepareOptionsMenu----------------------------------");

		if (contactFileListFragment != null) {
			if (contactFileListFragment.isVisible()) {
				logDebug("visible ContacFileListProperties");
				if (shareMenuItem != null) {
					shareMenuItem.setVisible(true);
					viewSharedItem.setVisible(false);
				}
			}
		}

		super.onPrepareOptionsMenu(menu);
		return true;

	}

	public void setParentHandle(long parentHandle) {
		this.parentHandle = parentHandle;
	}

	public void downloadFile(List<MegaNode> nodes) {
		nodeSaver.saveNodes(nodes, true, false, false, false);
	}

	public void moveToTrash(final ArrayList<Long> handleList) {
		logDebug("moveToTrash: ");
		if (!isOnline(this)) {
			showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
			return;
		}

		moveNodeUseCase.moveToRubbishBin(handleList)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe((result, throwable) -> {
					if (throwable == null) {
						showMovementResult(result, handleList.get(0));
					}
				});
	}

	/**
	 * Shows the final result of a movement request.
	 *
	 * @param result Object containing the request result.
	 * @param handle Handle of the node to mode.
	 */
	private void showMovementResult(MoveRequestResult result, long handle) {
		AlertDialogUtil.dismissAlertDialogIfExists(statusDialog);
		actionConfirmed();

		if (result.isSingleAction() && result.isSuccess() &&  parentHandle == handle) {
			onBackPressed();
			setTitleActionBar(megaApi.getNodeByHandle(parentHandle).getName());
		}

		if (result.isForeignNode()) {
			showForeignStorageOverQuotaWarningDialog(this);
		} else {
			showSnackbar(SNACKBAR_TYPE, result.getResultText(), MEGACHAT_INVALID_HANDLE);
		}
	}

	public void showMove(ArrayList<Long> handleList) {
		Intent intent = new Intent(this, FileExplorerActivity.class);
		intent.setAction(FileExplorerActivity.ACTION_PICK_MOVE_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i = 0; i < handleList.size(); i++) {
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("MOVE_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER_TO_MOVE);
	}

	public void showCopy(ArrayList<Long> handleList) {

		Intent intent = new Intent(this, FileExplorerActivity.class);
		intent.setAction(FileExplorerActivity.ACTION_PICK_COPY_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i = 0; i < handleList.size(); i++) {
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("COPY_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER_TO_COPY);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

        if (nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)) {
            return;
        }

		if (requestCode == REQUEST_CODE_SELECT_FOLDER_TO_COPY && resultCode == RESULT_OK) {
			if (intent == null) {
				return;
			}
			if (!isOnline(this)) {
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
				return;
			}

			AlertDialog temp;
			try {
				temp = createProgressDialog(this, getString(R.string.context_copying));
				temp.show();
			} catch (Exception e) {
				return;
			}
			statusDialog = temp;

			final long[] copyHandles = intent.getLongArrayExtra("COPY_HANDLES");
			final long toHandle = intent.getLongExtra("COPY_TO", 0);
			final int totalCopy = copyHandles.length;

			MegaNode parent = megaApi.getNodeByHandle(toHandle);
			for (int i = 0;
				 i < copyHandles.length;
				 i++) {
				logDebug("NODE TO COPY: " + megaApi.getNodeByHandle(copyHandles[i]).getName());
				logDebug("WHERE: " + parent.getName());
				logDebug("NODES: " + copyHandles[i] + "_" + parent.getHandle());
				MegaNode cN = megaApi.getNodeByHandle(copyHandles[i]);
				if (cN != null) {
					logDebug("cN != null");
					megaApi.copyNode(cN, parent, this);
				} else {
					logWarning("cN == null");
					try {
						statusDialog.dismiss();
						if (contactFileListFragment != null && contactFileListFragment.isVisible()) {
							showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_sent_node));
						}
					} catch (Exception ex) {
					}
				}
			}
		} else if (requestCode == REQUEST_CODE_SELECT_FOLDER_TO_MOVE && resultCode == RESULT_OK) {
			if (intent == null) {
				return;
			}
			if (!isOnline(this)) {
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
				return;
			}

			final long[] moveHandles = intent.getLongArrayExtra("MOVE_HANDLES");
			final long toHandle = intent.getLongExtra("MOVE_TO", 0);

			AlertDialog temp;
			try {
				temp = createProgressDialog(this, getString(R.string.context_moving));
				temp.show();
			} catch (Exception e) {
				return;
			}
			statusDialog = temp;

			moveNodeUseCase.move(moveHandles, toHandle)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe((result, throwable) -> {
						if (throwable == null) {
							showMovementResult(result, moveHandles[0]);
						}
					});
		} else if (requestCode == REQUEST_CODE_GET_FILES && resultCode == RESULT_OK) {
			if (intent == null) {
				return;
			}
			intent.setAction(Intent.ACTION_GET_CONTENT);

			try {
				statusDialog = createProgressDialog(this, getQuantityString(R.plurals.upload_prepare, 1));
				statusDialog.show();
			} catch (Exception e) {
				return;
			}

			filePrepareUseCase.prepareFiles(intent)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe((shareInfo, throwable) -> {
						if (throwable == null) {
							onIntentProcessed(shareInfo);
						}
					});
		}  else if (requestCode == REQUEST_CODE_GET_FOLDER) {
			getFolder(this, resultCode, intent, parentHandle);
		} else if (requestCode == REQUEST_CODE_SELECT_FOLDER && resultCode == RESULT_OK) {
			if (intent == null) {
				return;
			}
			if (!isOnline(this)) {
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
				return;
			}

			final ArrayList<String> selectedContacts = intent.getStringArrayListExtra(SELECTED_CONTACTS);
			final long folderHandle = intent.getLongExtra("SELECT", 0);

			final MegaNode parent = megaApi.getNodeByHandle(folderHandle);

			if (parent.isFolder()) {
				MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
				dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
				final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
				dialogBuilder.setSingleChoiceItems(items, -1, (dialog, item) -> {
					statusDialog = createProgressDialog(contactPropertiesMainActivity, getString(R.string.context_sharing_folder));
					permissionsDialog.dismiss();
					new NodeController(this).shareFolder(parent, selectedContacts, item);
				});
				permissionsDialog = dialogBuilder.create();
				permissionsDialog.show();
			}
		} else if (requestCode == TAKE_PHOTO_CODE) {
			logDebug("TAKE_PHOTO_CODE");
			if (resultCode == Activity.RESULT_OK) {
				long parentHandle = contactFileListFragment.getParentHandle();
				File file = getTemporalTakePictureFile(this);
				if (file != null) {
					checkNameCollisionUseCase.check(file.getName(), parentHandle)
							.subscribeOn(Schedulers.io())
							.observeOn(AndroidSchedulers.mainThread())
							.subscribe(handle -> {
										NameCollision collision = NameCollision.Upload
												.getUploadCollision(handle, file, parentHandle);

										startActivity(NameCollisionActivity.getIntentForSingleItem(this, collision));
									},
									throwable -> {
										if (throwable instanceof MegaNodeException.ParentDoesNotExistException) {
											showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.general_error));
										} else if (throwable instanceof MegaNodeException.ChildDoesNotExistsException) {
											uploadFile(contactPropertiesMainActivity, file.getAbsolutePath(), parentHandle, megaApi);
										}
									});
				}
			} else {
				logWarning("TAKE_PHOTO_CODE--->ERROR!");
			}
        } else if (requestCode == REQUEST_CODE_SCAN_DOCUMENT) {
            if (resultCode == RESULT_OK) {
                String savedDestination = intent.getStringExtra(DocumentScannerActivity.EXTRA_PICKED_SAVE_DESTINATION);
                Intent fileIntent = new Intent(this, FileExplorerActivity.class);
                if (StringResourcesUtils.getString(R.string.section_chat).equals(savedDestination)) {
                    fileIntent.setAction(FileExplorerActivity.ACTION_UPLOAD_TO_CHAT);
                } else {
                    fileIntent.setAction(FileExplorerActivity.ACTION_SAVE_TO_CLOUD);
					fileIntent.putExtra(FileExplorerActivity.EXTRA_PARENT_HANDLE, getParentHandle());
                }
                fileIntent.putExtra(Intent.EXTRA_STREAM, intent.getData());
                fileIntent.setType(intent.getType());
                startActivity(fileIntent);
            }
        }
	}

	/**
	 * Handle processed upload intent.
	 *
	 * @param infos List<ShareInfo> containing all the upload info.
	 */
	private void onIntentProcessed(List<ShareInfo> infos) {
		MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
		if (parentNode == null) {
			dismissAlertDialogIfExists(statusDialog);
			showErrorAlertDialog(StringResourcesUtils
					.getString(R.string.error_temporary_unavaible), false, this);
			return;
		}

		if (infos == null) {
			dismissAlertDialogIfExists(statusDialog);
			showErrorAlertDialog(StringResourcesUtils.getString(R.string.upload_can_not_open),
					false, this);
			return;
		}

		if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
			dismissAlertDialogIfExists(statusDialog);
			showOverDiskQuotaPaywallWarning();
			return;
		}

		checkNameCollisionUseCase.check(infos, parentNode)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe((result, throwable) -> {
					dismissAlertDialogIfExists(statusDialog);

					if (throwable != null) {
						showErrorAlertDialog(StringResourcesUtils
								.getString(R.string.error_temporary_unavaible), false, this);
					} else {
						ArrayList<NameCollision> collisions = result.getFirst();
						List<ShareInfo> withoutCollisions = result.getSecond();

						if (!collisions.isEmpty()) {
							startActivity(NameCollisionActivity.getIntentForList(this, collisions));
						}

						if (!withoutCollisions.isEmpty()) {
							showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getQuantityString(R.plurals.upload_began, withoutCollisions.size(), withoutCollisions.size()));

							for (ShareInfo info : withoutCollisions) {
								if (transfersManagement.shouldBreakTransfersProcessing()) {
									break;
								}

								Intent intent = new Intent(this, UploadService.class);
								intent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
								intent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
								intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
								intent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
								startService(intent);
							}
						}
					}
				});
	}

	@Override
	public void onBackPressed() {
		if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return;
		retryConnectionsAndSignalPresence();

		if (contactFileListFragment != null && contactFileListFragment.isVisible() && contactFileListFragment.onBackPressed() == 0) {
			super.onBackPressed();
		}
	}

	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
		logDebug("onUserAlertsUpdate");
	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes) {
		for (MegaNode node : nodes) {
			if (node.isInShare() && parentHandle == node.getHandle()) {
				getNodeUseCase.get(parentHandle)
						.subscribeOn(Schedulers.io())
						.observeOn(AndroidSchedulers.mainThread())
						.subscribe((result, throwable) -> {
							if (throwable == null) {
								updateNodes();
							} else {
								finish();
							}
						});
			} else {
				updateNodes();
			}
		}
	}

	/**
	 * Update the nodes.
	 */
	private void updateNodes() {
		if (contactFileListFragment != null && contactFileListFragment.isVisible()){
			contactFileListFragment.setNodes(parentHandle);
		}
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		if (request.getType() == MegaRequest.TYPE_MOVE) {
			logDebug("Move request start");
		} else if (request.getType() == MegaRequest.TYPE_REMOVE) {
			logDebug("Remove request start");
		} else if (request.getType() == MegaRequest.TYPE_EXPORT) {
			logDebug("Export request start");
		} else if (request.getType() == MegaRequest.TYPE_COPY) {
			logDebug("Copy request start");
		} else if (request.getType() == MegaRequest.TYPE_SHARE) {
			logDebug("Share request start");
		}
	}

	public void askConfirmationMoveToRubbish(final ArrayList<Long> handleList) {
		logDebug("askConfirmationMoveToRubbish");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						moveToTrash(handleList);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		if (handleList != null) {

			if (handleList.size() > 0) {
				MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
				if (handleList.size() > 1) {
					builder.setMessage(getResources().getString(R.string.confirmation_move_to_rubbish_plural));
				} else {
					builder.setMessage(getResources().getString(R.string.confirmation_move_to_rubbish));
				}
				builder.setPositiveButton(R.string.general_move, dialogClickListener);
				builder.setNegativeButton(R.string.general_cancel, dialogClickListener);
				builder.show();
			}
		} else {
			logWarning("handleList NULL");
			return;
		}
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestUpdate");
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		logDebug("onRequestFinish");

		if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER) {
			try {
				statusDialog.dismiss();
			} catch (Exception ex) {
			}

			if (e.getErrorCode() == MegaError.API_OK) {
				if (contactFileListFragment != null && contactFileListFragment.isVisible()) {
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_created));
					contactFileListFragment.setNodes();
				}
			} else {
				if (contactFileListFragment != null && contactFileListFragment.isVisible()) {
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_no_created));
					contactFileListFragment.setNodes();
				}
			}
		} else if (request.getType() == MegaRequest.TYPE_COPY) {
			try {
				statusDialog.dismiss();
			} catch (Exception ex) {
			}

			if (e.getErrorCode() == MegaError.API_OK) {
				if (contactFileListFragment != null && contactFileListFragment.isVisible()) {
					contactFileListFragment.clearSelections();
					contactFileListFragment.hideMultipleSelect();
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_copied));
				}
			} else {
				if (e.getErrorCode() == MegaError.API_EOVERQUOTA) {
					if (api.isForeignNode(request.getParentHandle())) {
						showForeignStorageOverQuotaWarningDialog(this);
						return;
					}

					logWarning("OVERQUOTA ERROR: " + e.getErrorCode());
					Intent intent = new Intent(this, ManagerActivity.class);
					intent.setAction(ACTION_OVERQUOTA_STORAGE);
					startActivity(intent);
					finish();
				} else if (e.getErrorCode() == MegaError.API_EGOINGOVERQUOTA) {
					logWarning("PRE OVERQUOTA ERROR: " + e.getErrorCode());
					Intent intent = new Intent(this, ManagerActivity.class);
					intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
					startActivity(intent);
					finish();
				} else {
					if (contactFileListFragment != null && contactFileListFragment.isVisible()) {
						contactFileListFragment.clearSelections();
						contactFileListFragment.hideMultipleSelect();
						showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied));
					}
				}
			}

			logDebug("Copy nodes request finished");
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		logDebug("onRequestTemporaryError");
	}

	@Override
	public void onAccountUpdate(MegaApiJava api) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onContactRequestsUpdate(MegaApiJava api,
			ArrayList<MegaContactRequest> requests) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onEvent(MegaApiJava api, MegaEvent event) {

	}

	public void showOptionsPanel(MegaNode node) {
		logDebug("showOptionsPanel");
		if (node == null|| isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedNode = node;
        bottomSheetDialogFragment = new ContactFileListBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
	}

	public void showSnackbar(int type, String s) {
		CoordinatorLayout coordinatorFragment = (CoordinatorLayout) findViewById(R.id.contact_file_list_coordinator_layout);
		contactFileListFragment = (ContactFileListFragment) getSupportFragmentManager().findFragmentByTag("cflF");
		if (contactFileListFragment != null && contactFileListFragment.isVisible()) {
			if (coordinatorFragment != null) {
				showSnackbar(type, coordinatorFragment, s);
			} else {
				showSnackbar(type, fragmentContainer, s);
			}
		}
	}

	public MegaNode getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(MegaNode selectedNode) {
		this.selectedNode = selectedNode;
	}

	public boolean isEmptyParentHandleStack() {
		if (contactFileListFragment != null) {
			return contactFileListFragment.isEmptyParentHandleStack();
		}
		logDebug("Fragment NULL");
		return true;
	}

	public void setTitleActionBar(String title) {
		if (aB != null) {
			if (title == null) {
				logDebug("Reset title and subtitle");
				aB.setTitle(StringResourcesUtils.getString(R.string.title_incoming_shares_with_explorer)
						.toUpperCase(Locale.getDefault()));
				aB.setSubtitle(fullName);

			} else {
				aB.setTitle(title);
				aB.setSubtitle(null);
			}
		}
	}

	public long getParentHandle() {

		if (contactFileListFragment != null) {
			return contactFileListFragment.getParentHandle();
		}
		return -1;
	}

	public void openAdvancedDevices(long handleToDownload, boolean highPriority) {
		logDebug("handleToDownload: " + handleToDownload + ", highPriority: " + highPriority);
		String externalPath = getExternalCardPath();

		if (externalPath != null) {
			logDebug("ExternalPath for advancedDevices: " + externalPath);
			MegaNode node = megaApi.getNodeByHandle(handleToDownload);
			if (node != null) {

				File newFile = new File(node.getName());
				logDebug("File: " + newFile.getPath());
				Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

				// Filter to only show results that can be "opened", such as
				// a file (as opposed to a list of contacts or timezones).
				intent.addCategory(Intent.CATEGORY_OPENABLE);

				// Create a file with the requested MIME type.
				String mimeType = MimeTypeList.getMimeType(newFile);
				logDebug("Mimetype: " + mimeType);
				intent.setType(mimeType);
				intent.putExtra(Intent.EXTRA_TITLE, node.getName());
				intent.putExtra("handleToDownload", handleToDownload);
				intent.putExtra(HIGH_PRIORITY_TRANSFER, highPriority);
				try {
					startActivityForResult(intent, WRITE_SD_CARD_REQUEST_CODE);
				} catch (Exception e) {
					logError("Exception in External SDCARD", e);
					Environment.getExternalStorageDirectory();
					Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
					toast.show();
				}
			}
		} else {
			logWarning("No external SD card");
			Environment.getExternalStorageDirectory();
			Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
			toast.show();
		}
	}

	@Override
	public void showSnackbar(int type, @Nullable String content, long chatId) {
		showSnackbar(type, fragmentContainer, content, chatId);
	}

	@Override
	public void finishRenameActionWithSuccess(@NonNull String newName) {
		// No update needed
	}

	@Override
	public void actionConfirmed() {
		if (contactFileListFragment != null && contactFileListFragment.isVisible()) {
			contactFileListFragment.clearSelections();
			contactFileListFragment.hideMultipleSelect();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_contact_file_list, menu);
		return super.onCreateOptionsMenu(menu);
	}
}
