package mega.privacy.android.app.main;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_CLOSE_CHAT_AFTER_OPEN_TRANSFERS;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_TRANSFER_OVER_QUOTA;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_TYPE;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CREDENTIALS;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CU;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_DISABLE_CU_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_DISABLE_CU_UI_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_FIRST_NAME;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_LAST_NAME;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_NICKNAME;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_PUSH_NOTIFICATION_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_CU_ATTR_CHANGE;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_TRANSFER_UPDATE;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_TRANSFER_FINISH;
import static mega.privacy.android.app.constants.BroadcastConstants.COMPLETED_TRANSFER;
import static mega.privacy.android.app.constants.BroadcastConstants.EXTRA_USER_HANDLE;
import static mega.privacy.android.app.constants.BroadcastConstants.INVALID_ACTION;
import static mega.privacy.android.app.constants.BroadcastConstants.PENDING_TRANSFERS;
import static mega.privacy.android.app.constants.BroadcastConstants.PROGRESS;
import static mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ON_HOLD_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_FINISH_ACTIVITY;
import static mega.privacy.android.app.constants.EventConstants.EVENT_MY_BACKUPS_FOLDER_CHANGED;
import static mega.privacy.android.app.constants.EventConstants.EVENT_NETWORK_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_REFRESH;
import static mega.privacy.android.app.constants.EventConstants.EVENT_REFRESH_PHONE_NUMBER;
import static mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_HOLD_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_VIEW_MODE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_USER_EMAIL_UPDATED;
import static mega.privacy.android.app.constants.EventConstants.EVENT_USER_NAME_UPDATED;
import static mega.privacy.android.app.constants.IntentConstants.ACTION_OPEN_ACHIEVEMENTS;
import static mega.privacy.android.app.constants.IntentConstants.EXTRA_ACCOUNT_TYPE;
import static mega.privacy.android.app.constants.IntentConstants.EXTRA_ASK_PERMISSIONS;
import static mega.privacy.android.app.constants.IntentConstants.EXTRA_FIRST_LOGIN;
import static mega.privacy.android.app.constants.IntentConstants.EXTRA_NEW_ACCOUNT;
import static mega.privacy.android.app.constants.IntentConstants.EXTRA_UPGRADE_ACCOUNT;
import static mega.privacy.android.app.fragments.settingsFragments.startSceen.util.StartScreenUtil.CHAT_BNV;
import static mega.privacy.android.app.fragments.settingsFragments.startSceen.util.StartScreenUtil.CLOUD_DRIVE_BNV;
import static mega.privacy.android.app.fragments.settingsFragments.startSceen.util.StartScreenUtil.HOME_BNV;
import static mega.privacy.android.app.fragments.settingsFragments.startSceen.util.StartScreenUtil.NO_BNV;
import static mega.privacy.android.app.fragments.settingsFragments.startSceen.util.StartScreenUtil.PHOTOS_BNV;
import static mega.privacy.android.app.fragments.settingsFragments.startSceen.util.StartScreenUtil.SHARED_ITEMS_BNV;
import static mega.privacy.android.app.fragments.settingsFragments.startSceen.util.StartScreenUtil.getStartBottomNavigationItem;
import static mega.privacy.android.app.fragments.settingsFragments.startSceen.util.StartScreenUtil.getStartDrawerItem;
import static mega.privacy.android.app.fragments.settingsFragments.startSceen.util.StartScreenUtil.setStartScreenTimeStamp;
import static mega.privacy.android.app.fragments.settingsFragments.startSceen.util.StartScreenUtil.shouldCloseApp;
import static mega.privacy.android.app.main.FileInfoActivity.NODE_HANDLE;
import static mega.privacy.android.app.main.PermissionsFragment.PERMISSIONS_FRAGMENT;
import static mega.privacy.android.app.meeting.activity.MeetingActivity.MEETING_ACTION_CREATE;
import static mega.privacy.android.app.meeting.activity.MeetingActivity.MEETING_ACTION_JOIN;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown;
import static mega.privacy.android.app.modalbottomsheet.UploadBottomSheetDialogFragment.GENERAL_UPLOAD;
import static mega.privacy.android.app.modalbottomsheet.UploadBottomSheetDialogFragment.HOMEPAGE_UPLOAD;
import static mega.privacy.android.app.sync.fileBackups.FileBackupManager.BackupDialogState.BACKUP_DIALOG_SHOW_CONFIRM;
import static mega.privacy.android.app.sync.fileBackups.FileBackupManager.BackupDialogState.BACKUP_DIALOG_SHOW_NONE;
import static mega.privacy.android.app.sync.fileBackups.FileBackupManager.BackupDialogState.BACKUP_DIALOG_SHOW_WARNING;
import static mega.privacy.android.app.sync.fileBackups.FileBackupManager.OperationType.OPERATION_EXECUTE;
import static mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists;
import static mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown;
import static mega.privacy.android.app.utils.AlertsAndWarnings.askForCustomizedPlan;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.AvatarUtil.getColorAvatar;
import static mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar;
import static mega.privacy.android.app.utils.CallUtil.checkPermissionsCall;
import static mega.privacy.android.app.utils.CallUtil.hideCallMenuItem;
import static mega.privacy.android.app.utils.CallUtil.hideCallWidget;
import static mega.privacy.android.app.utils.CallUtil.isChatConnectedInOrderToInitiateACall;
import static mega.privacy.android.app.utils.CallUtil.isMeetingEnded;
import static mega.privacy.android.app.utils.CallUtil.isNecessaryDisableLocalCamera;
import static mega.privacy.android.app.utils.CallUtil.openMeetingToCreate;
import static mega.privacy.android.app.utils.CallUtil.participatingInACall;
import static mega.privacy.android.app.utils.CallUtil.returnActiveCall;
import static mega.privacy.android.app.utils.CallUtil.setCallMenuItem;
import static mega.privacy.android.app.utils.CallUtil.showCallLayout;
import static mega.privacy.android.app.utils.CallUtil.showConfirmationInACall;
import static mega.privacy.android.app.utils.CallUtil.showConfirmationOpenCamera;
import static mega.privacy.android.app.utils.CallUtil.startCallWithChatOnline;
import static mega.privacy.android.app.utils.CameraUploadUtil.backupTimestampsAndFolderHandle;
import static mega.privacy.android.app.utils.CameraUploadUtil.disableCameraUploadSettingProcess;
import static mega.privacy.android.app.utils.CameraUploadUtil.disableMediaUploadProcess;
import static mega.privacy.android.app.utils.CameraUploadUtil.getPrimaryFolderHandle;
import static mega.privacy.android.app.utils.CameraUploadUtil.getSecondaryFolderHandle;
import static mega.privacy.android.app.utils.ChatUtil.StatusIconLocation;
import static mega.privacy.android.app.utils.ChatUtil.createMuteNotificationsChatAlertDialog;
import static mega.privacy.android.app.utils.ChatUtil.getGeneralNotification;
import static mega.privacy.android.app.utils.ChatUtil.getTitleChat;
import static mega.privacy.android.app.utils.ColorUtils.tintIcon;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ConstantsUrl.RECOVERY_URL;
import static mega.privacy.android.app.utils.DBUtil.resetAccountDetailsTimeStamp;
import static mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION;
import static mega.privacy.android.app.utils.FileUtil.OLD_MK_FILE;
import static mega.privacy.android.app.utils.FileUtil.OLD_RK_FILE;
import static mega.privacy.android.app.utils.FileUtil.buildExternalStorageFile;
import static mega.privacy.android.app.utils.FileUtil.createTemporalTextFile;
import static mega.privacy.android.app.utils.FileUtil.getRecoveryKeyFileName;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.JobUtil.fireCancelCameraUploadJob;
import static mega.privacy.android.app.utils.JobUtil.fireCameraUploadJob;
import static mega.privacy.android.app.utils.JobUtil.fireStopCameraUploadJob;
import static mega.privacy.android.app.utils.JobUtil.stopCameraUploadSyncHeartbeatWorkers;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.LogUtil.logInfo;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.LogUtil.resetLoggerSDK;
import static mega.privacy.android.app.utils.MegaApiUtils.calculateDeepBrowserTreeIncoming;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_FAB;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_SHARE_FOLDER;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_MOVE_TO_BACKUP;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_ACTION_TYPE;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_DIALOG_WARN;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_HANDLED_ITEM;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_HANDLED_NODE;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NODE_TYPE;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.IS_NEW_TEXT_FILE_SHOWN;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.IS_NEW_FOLDER_DIALOG_SHOWN;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.NEW_FOLDER_DIALOG_TEXT;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.NEW_TEXT_FILE_TEXT;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.checkNewFolderDialogState;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.checkNewTextFileDialogState;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog;
import static mega.privacy.android.app.utils.MegaNodeUtil.isNodeInRubbish;
import static mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog;
import static mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog;
import static mega.privacy.android.app.utils.MegaProgressDialogUtil.showProcessFileDialog;
import static mega.privacy.android.app.utils.MegaTransferUtils.isBackgroundTransfer;
import static mega.privacy.android.app.utils.OfflineUtils.removeInitialOfflinePath;
import static mega.privacy.android.app.utils.OfflineUtils.removeOffline;
import static mega.privacy.android.app.utils.OfflineUtils.saveOffline;
import static mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.TimeUtils.getHumanizedTime;
import static mega.privacy.android.app.utils.UploadUtil.chooseFiles;
import static mega.privacy.android.app.utils.UploadUtil.chooseFolder;
import static mega.privacy.android.app.utils.UploadUtil.getFolder;
import static mega.privacy.android.app.utils.UploadUtil.uploadTakePicture;
import static mega.privacy.android.app.utils.Util.ONTRANSFERUPDATE_REFRESH_MILLIS;
import static mega.privacy.android.app.utils.Util.canVoluntaryVerifyPhoneNumber;
import static mega.privacy.android.app.utils.Util.checkTakePicture;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.getSizeString;
import static mega.privacy.android.app.utils.Util.getSizeStringGBBased;
import static mega.privacy.android.app.utils.Util.getVersion;
import static mega.privacy.android.app.utils.Util.hideKeyboard;
import static mega.privacy.android.app.utils.Util.hideKeyboardView;
import static mega.privacy.android.app.utils.Util.isOnline;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;
import static mega.privacy.android.app.utils.Util.isTablet;
import static mega.privacy.android.app.utils.Util.matchRegexs;
import static mega.privacy.android.app.utils.Util.mutateIconSecondary;
import static mega.privacy.android.app.utils.Util.resetActionBar;
import static mega.privacy.android.app.utils.Util.scaleHeightPx;
import static mega.privacy.android.app.utils.Util.scaleWidthPx;
import static mega.privacy.android.app.utils.Util.setStatusBarColor;
import static mega.privacy.android.app.utils.Util.showAlert;
import static mega.privacy.android.app.utils.Util.showMessageRandom;
import static mega.privacy.android.app.utils.billing.PaymentUtils.updateSubscriptionLevel;
import static mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions;
import static mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission;
import static nz.mega.sdk.MegaApiJava.BUSINESS_STATUS_EXPIRED;
import static nz.mega.sdk.MegaApiJava.BUSINESS_STATUS_GRACE_PERIOD;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_MY_BACKUPS_FOLDER;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;
import static nz.mega.sdk.MegaShare.ACCESS_READ;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener;
import com.google.android.material.tabs.TabLayout;
import com.jeremyliao.liveeventbus.LiveEventBus;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Unit;
import kotlinx.coroutines.CoroutineScope;
import mega.privacy.android.app.AndroidCompletedTransfer;
import mega.privacy.android.app.BusinessExpiredAlertActivity;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.OpenPasswordLinkActivity;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.databinding.FabMaskChatLayoutBinding;
import mega.privacy.android.app.di.ApplicationScope;
import mega.privacy.android.app.fragments.managerFragments.cu.PhotosFragment;
import mega.privacy.android.app.fragments.managerFragments.cu.album.AlbumContentFragment;
import mega.privacy.android.app.gallery.ui.MediaDiscoveryFragment;
import mega.privacy.android.app.objects.PasscodeManagement;
import mega.privacy.android.app.fragments.homepage.documents.DocumentsFragment;
import mega.privacy.android.app.generalusecase.FilePrepareUseCase;
import mega.privacy.android.app.smsVerification.SMSVerificationActivity;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.TransfersManagementActivity;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.activities.OfflineFileInfoActivity;
import mega.privacy.android.app.activities.WebViewActivity;
import mega.privacy.android.app.components.CustomViewPager;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.attacher.MegaAttacher;
import mega.privacy.android.app.components.saver.NodeSaver;
import mega.privacy.android.app.components.transferWidget.TransfersManagement;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.contacts.ContactsActivity;
import mega.privacy.android.app.contacts.usecase.InviteContactUseCase;
import mega.privacy.android.app.exportRK.ExportRecoveryKeyActivity;
import mega.privacy.android.app.fragments.homepage.HomepageSearchable;
import mega.privacy.android.app.fragments.homepage.main.HomepageFragment;
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections;
import mega.privacy.android.app.fragments.managerFragments.LinksFragment;
import mega.privacy.android.app.fragments.managerFragments.cu.CustomHideBottomViewOnScrollBehaviour;
import mega.privacy.android.app.fragments.offline.OfflineFragment;
import mega.privacy.android.app.fragments.recent.RecentsFragment;
import mega.privacy.android.app.fragments.settingsFragments.cookie.CookieDialogHandler;
import mega.privacy.android.app.globalmanagement.MyAccountInfo;
import mega.privacy.android.app.globalmanagement.SortOrderManagement;
import mega.privacy.android.app.interfaces.ActionNodeCallback;
import mega.privacy.android.app.interfaces.ChatManagementCallback;
import mega.privacy.android.app.interfaces.MeetingBottomSheetDialogActionListener;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.interfaces.UploadBottomSheetDialogActionListener;
import mega.privacy.android.app.listeners.CancelTransferListener;
import mega.privacy.android.app.listeners.ExportListener;
import mega.privacy.android.app.listeners.GetAttrUserListener;
import mega.privacy.android.app.listeners.LoadPreviewListener;
import mega.privacy.android.app.listeners.RemoveFromChatRoomListener;
import mega.privacy.android.app.main.adapters.SharesPageAdapter;
import mega.privacy.android.app.main.adapters.TransfersPageAdapter;
import mega.privacy.android.app.main.controllers.AccountController;
import mega.privacy.android.app.main.controllers.ContactController;
import mega.privacy.android.app.main.controllers.NodeController;
import mega.privacy.android.app.main.listeners.CreateGroupChatWithPublicLink;
import mega.privacy.android.app.main.listeners.FabButtonListener;
import mega.privacy.android.app.main.managerSections.CompletedTransfersFragment;
import mega.privacy.android.app.main.managerSections.FileBrowserFragment;
import mega.privacy.android.app.main.managerSections.InboxFragment;
import mega.privacy.android.app.main.managerSections.IncomingSharesFragment;
import mega.privacy.android.app.main.managerSections.NotificationsFragment;
import mega.privacy.android.app.main.managerSections.OutgoingSharesFragment;
import mega.privacy.android.app.main.managerSections.RubbishBinFragment;
import mega.privacy.android.app.main.managerSections.SearchFragment;
import mega.privacy.android.app.main.managerSections.TransfersFragment;
import mega.privacy.android.app.main.managerSections.TurnOnNotificationsFragment;
import mega.privacy.android.app.main.megachat.BadgeDrawerArrowDrawable;
import mega.privacy.android.app.main.megachat.ChatActivity;
import mega.privacy.android.app.main.megachat.RecentChatsFragment;
import mega.privacy.android.app.main.qrcode.QRCodeActivity;
import mega.privacy.android.app.main.qrcode.ScanCodeFragment;
import mega.privacy.android.app.main.tasks.CheckOfflineNodesTask;
import mega.privacy.android.app.main.tasks.FillDBContactsTask;
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController;
import mega.privacy.android.app.meeting.fragments.MeetingHasEndedDialogFragment;
import mega.privacy.android.app.meeting.fragments.MeetingParticipantBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.ManageTransferBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.MeetingBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.OfflineOptionsBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.SortByBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.UploadBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ChatBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.nodelabel.NodeLabelBottomSheetDialogFragment;
import mega.privacy.android.app.myAccount.MyAccountActivity;
import mega.privacy.android.app.myAccount.usecase.CheckPasswordReminderUseCase;
import mega.privacy.android.app.presentation.manager.ManagerViewModel;
import mega.privacy.android.app.presentation.settings.model.TargetPreference;
import mega.privacy.android.app.psa.Psa;
import mega.privacy.android.app.psa.PsaManager;
import mega.privacy.android.app.psa.PsaViewHolder;
import mega.privacy.android.app.service.iar.RatingHandlerImpl;
import mega.privacy.android.app.service.push.MegaMessageService;
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager;
import mega.privacy.android.app.sync.fileBackups.FileBackupManager;
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity;
import mega.privacy.android.app.usecase.DownloadNodeUseCase;
import mega.privacy.android.app.usecase.GetNodeUseCase;
import mega.privacy.android.app.usecase.MoveNodeUseCase;
import mega.privacy.android.app.usecase.RemoveNodeUseCase;
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase;
import mega.privacy.android.app.usecase.data.MoveRequestResult;
import mega.privacy.android.app.utils.AlertsAndWarnings;
import mega.privacy.android.app.utils.AvatarUtil;
import mega.privacy.android.app.utils.CacheFolderManager;
import mega.privacy.android.app.utils.CallUtil;
import mega.privacy.android.app.utils.CameraUploadUtil;
import mega.privacy.android.app.utils.ChatUtil;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.ContactUtil;
import mega.privacy.android.app.utils.LastShowSMSDialogTimeChecker;
import mega.privacy.android.app.utils.LinksUtil;
import mega.privacy.android.app.utils.MegaNodeDialogUtil;
import mega.privacy.android.app.utils.MegaNodeUtil;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.TextUtil;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.TimeUtils;
import mega.privacy.android.app.utils.UploadUtil;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.contacts.MegaContactGetter;
import mega.privacy.android.app.zippreview.ui.ZipBrowserActivity;
import nz.mega.documentscanner.DocumentScannerActivity;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaAchievementsDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaFolderInfo;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferData;
import nz.mega.sdk.MegaTransferListenerInterface;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;

@AndroidEntryPoint
@SuppressWarnings("deprecation")
public class ManagerActivity extends TransfersManagementActivity
		implements MegaRequestListenerInterface, MegaChatRequestListenerInterface, OnNavigationItemSelectedListener,
        MegaTransferListenerInterface, OnClickListener,
		BottomNavigationView.OnNavigationItemSelectedListener, UploadBottomSheetDialogActionListener,
		ChatManagementCallback, ActionNodeCallback, SnackbarShower,
		MeetingBottomSheetDialogActionListener, LoadPreviewListener.OnPreviewLoadedCallback {

    private static final String TRANSFER_OVER_QUOTA_SHOWN = "TRANSFER_OVER_QUOTA_SHOWN";

    public static final String TRANSFERS_TAB = "TRANSFERS_TAB";
    private static final String SEARCH_SHARED_TAB = "SEARCH_SHARED_TAB";
    private static final String SEARCH_DRAWER_ITEM = "SEARCH_DRAWER_ITEM";
    private static final String BOTTOM_ITEM_BEFORE_OPEN_FULLSCREEN_OFFLINE = "BOTTOM_ITEM_BEFORE_OPEN_FULLSCREEN_OFFLINE";

    private static final String BUSINESS_GRACE_ALERT_SHOWN = "BUSINESS_GRACE_ALERT_SHOWN";
    private static final String BUSINESS_CU_ALERT_SHOWN = "BUSINESS_CU_ALERT_SHOWN";

    private static final String DEEP_BROWSER_TREE_LINKS = "DEEP_BROWSER_TREE_LINKS";
    private static final String PARENT_HANDLE_LINKS = "PARENT_HANDLE_LINKS";
    public static final String NEW_CREATION_ACCOUNT = "NEW_CREATION_ACCOUNT";
    public static final String JOINING_CHAT_LINK = "JOINING_CHAT_LINK";
    public static final String LINK_JOINING_CHAT_LINK = "LINK_JOINING_CHAT_LINK";
    private static final String PROCESS_FILE_DIALOG_SHOWN = "PROGRESS_DIALOG_SHOWN";
    private static final String OPEN_LINK_DIALOG_SHOWN = "OPEN_LINK_DIALOG_SHOWN";
    private static final String OPEN_LINK_TEXT = "OPEN_LINK_TEXT";
    private static final String OPEN_LINK_ERROR = "OPEN_LINK_ERROR";

    public static final int ERROR_TAB = -1;
    public static final int INCOMING_TAB = 0;
    public static final int OUTGOING_TAB = 1;
    public static final int LINKS_TAB = 2;
    public static final int PENDING_TAB = 0;
    public static final int COMPLETED_TAB = 1;

    // 8dp + 56dp(Fab's size) + 8dp
    public static final int TRANSFER_WIDGET_MARGIN_BOTTOM = 72;

    /**
     * The causes of elevating the app bar
     */
    public static final int ELEVATION_SCROLL = 0x01;
    public static final int ELEVATION_CALL_IN_PROGRESS = 0x02;
    /**
     * The cause bitmap of elevating the app bar
     */
    private int mElevationCause;
    /**
     * True if any TabLayout is visible
     */
    private boolean mShowAnyTabLayout;

    private LastShowSMSDialogTimeChecker smsDialogTimeChecker;

    private ManagerViewModel viewModel;

    @Inject
    CheckPasswordReminderUseCase checkPasswordReminderUseCase;
    @Inject
    CookieDialogHandler cookieDialogHandler;
    @Inject
    SortOrderManagement sortOrderManagement;
    @Inject
    MyAccountInfo myAccountInfo;
    @Inject
    InviteContactUseCase inviteContactUseCase;
    @Inject
    FilePrepareUseCase filePrepareUseCase;
    @Inject
    PasscodeManagement passcodeManagement;
    @Inject
    MoveNodeUseCase moveNodeUseCase;
    @Inject
    RemoveNodeUseCase removeNodeUseCase;
    @Inject
    GetNodeUseCase getNodeUseCase;
    @Inject
    GetChatChangesUseCase getChatChangesUseCase;
    @Inject
    DownloadNodeUseCase downloadNodeUseCase;
    @ApplicationScope
    @Inject
    CoroutineScope sharingScope;

    public ArrayList<Integer> transfersInProgress;
    public MegaTransferData transferData;

    public long transferCallback = 0;

    //GET PRO ACCOUNT PANEL
    LinearLayout getProLayout = null;
    TextView getProText;
    TextView leftCancelButton;
    TextView rightUpgradeButton;
    Button addPhoneNumberButton;
    TextView addPhoneNumberLabel;
    FloatingActionButton fabButton;
    FloatingActionButton fabMaskButton;

    MegaNode inboxNode = null;

    MegaNode rootNode = null;

    NodeController nC;
    ContactController cC;
    AccountController aC;

    private final MegaAttacher nodeAttacher = new MegaAttacher(this);
    private final NodeSaver nodeSaver = new NodeSaver(this, this, this,
            AlertsAndWarnings.showSaveToDeviceConfirmDialog(this));

    private AndroidCompletedTransfer selectedTransfer;
    MegaNode selectedNode;
    MegaOffline selectedOfflineNode;
    MegaContactAdapter selectedUser;
    MegaContactRequest selectedRequest;

    public long selectedChatItemId;

    private BadgeDrawerArrowDrawable badgeDrawable;

    MegaPreferences prefs = null;
    MegaAttributes attr = null;
    static ManagerActivity managerActivity = null;
    MegaApplication app = null;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    Handler handler;
    DisplayMetrics outMetrics;
    FragmentContainerView fragmentContainer;
    ActionBar aB;
    MaterialToolbar toolbar;
    AppBarLayout abL;

    int selectedAccountType;

    ShareInfo infoManager;
    MegaNode parentNodeManager;

    boolean firstNavigationLevel = true;
    public DrawerLayout drawerLayout;
    ArrayList<MegaUser> contacts = new ArrayList<>();
    ArrayList<MegaUser> visibleContacts = new ArrayList<>();

    public boolean openFolderRefresh = false;

    public boolean openSettingsStartScreen;
    public boolean openSettingsStorage = false;
    public boolean openSettingsQR = false;
    boolean newAccount = false;
    public boolean newCreationAccount;

    private int storageState = MegaApiJava.STORAGE_STATE_UNKNOWN; //Default value
    private int storageStateFromBroadcast = MegaApiJava.STORAGE_STATE_UNKNOWN; //Default value
    private boolean showStorageAlertWithDelay;

    private boolean isStorageStatusDialogShown = false;

    private boolean isTransferOverQuotaWarningShown;
    private AlertDialog transferOverQuotaWarning;
    private AlertDialog confirmationTransfersDialog;

    private AlertDialog reconnectDialog;
    private AlertDialog inviteContactDialog;

    private RelativeLayout navigationDrawerAddPhoneContainer;
    int orientationSaved;

    private boolean isSMSDialogShowing;
    private final static String STATE_KEY_SMS_DIALOG = "isSMSDialogShowing";

    // Determine if open this activity from meeting page, if true, will finish this activity when user click back icon
    private boolean isFromMeeting = false;

    private static final String STATE_KEY_IS_IN_MD_MODE = "isInMDMode";
    // Determine if in Media discovery page, if it is true, it must in CD drawerItem tab
    private boolean isInMDMode = false;

    private static final String STATE_KEY_IS_IN_ALBUM_CONTENT = "isInAlbumContent";
    private boolean isInAlbumContent;
    public boolean fromAlbumContent = false;

    public enum FragmentTag {
        CLOUD_DRIVE, HOMEPAGE, PHOTOS, INBOX, INCOMING_SHARES, OUTGOING_SHARES, SEARCH, TRANSFERS, COMPLETED_TRANSFERS,
        RECENT_CHAT, RUBBISH_BIN, NOTIFICATIONS, TURN_ON_NOTIFICATIONS, PERMISSIONS, SMS_VERIFICATION,
        LINKS, MEDIA_DISCOVERY, ALBUM_CONTENT;

        public String getTag() {
            switch (this) {
                case CLOUD_DRIVE:
                    return "fileBrowserFragment";
                case HOMEPAGE:
                    return "homepageFragment";
                case RUBBISH_BIN:
                    return "rubbishBinFragment";
                case PHOTOS:
                    return "photosFragment";
                case INBOX:
                    return "inboxFragment";
                case INCOMING_SHARES:
                    return "incomingSharesFragment";
                case OUTGOING_SHARES:
                    return "outgoingSharesFragment";
                case SEARCH:
                    return "searchFragment";
                case TRANSFERS:
                    return "android:switcher:" + R.id.transfers_tabs_pager + ":" + 0;
                case COMPLETED_TRANSFERS:
                    return "android:switcher:" + R.id.transfers_tabs_pager + ":" + 1;
                case RECENT_CHAT:
                    return "recentChatsFragment";
                case NOTIFICATIONS:
                    return "notificationsFragment";
                case TURN_ON_NOTIFICATIONS:
                    return "turnOnNotificationsFragment";
                case PERMISSIONS:
                    return "permissionsFragment";
                case SMS_VERIFICATION:
                    return "smsVerificationFragment";
                case LINKS:
                    return "linksFragment";
                case MEDIA_DISCOVERY:
                    return "mediaDiscoveryFragment";
                case ALBUM_CONTENT:
                    return "fragmentAlbumContent";
            }
            return null;
        }
    }

    public boolean turnOnNotifications = false;

    private int searchSharedTab = -1;
    private DrawerItem searchDrawerItem = null;
    private DrawerItem drawerItem;
    static MenuItem drawerMenuItem = null;
    LinearLayout fragmentLayout;
    BottomNavigationView bNV;
    NavigationView nV;
    RelativeLayout usedSpaceLayout;
    private EmojiTextView nVDisplayName;
    TextView nVEmail;
    TextView businessLabel;
    RoundedImageView nVPictureProfile;
    TextView spaceTV;
    ProgressBar usedSpacePB;

    private MiniAudioPlayerController miniAudioPlayerController;

    private LinearLayout cuViewTypes;
    private TextView cuYearsButton;
    private TextView cuMonthsButton;
    private TextView cuDaysButton;
    private TextView cuAllButton;
    private LinearLayout cuLayout;
    private Button enableCUButton;
    private ProgressBar cuProgressBar;

    //Tabs in Shares
    private TabLayout tabLayoutShares;
    private SharesPageAdapter sharesPageAdapter;
    private CustomViewPager viewPagerShares;

    //Tabs in Transfers
    private TabLayout tabLayoutTransfers;
    private TransfersPageAdapter mTabsAdapterTransfers;
    private CustomViewPager viewPagerTransfers;

    private RelativeLayout callInProgressLayout;
    private Chronometer callInProgressChrono;
    private TextView callInProgressText;
    private LinearLayout microOffLayout;
    private LinearLayout videoOnLayout;

    boolean firstTimeAfterInstallation = true;
    SearchView searchView;
    public boolean searchExpand = false;
    private String searchQuery = "";
    public boolean textSubmitted = false;
    public boolean textsearchQuery = false;
    boolean isSearching = false;
    public int levelsSearch = -1;
    boolean openLink = false;

    long lastTimeOnTransferUpdate = Calendar.getInstance().getTimeInMillis();

    boolean firstLogin = false;
    private boolean askPermissions = false;
    private boolean isClearRubbishBin = false;

    boolean megaContacts = true;

    private HomepageScreen mHomepageScreen = HomepageScreen.HOMEPAGE;

    private enum HomepageScreen {
        HOMEPAGE, IMAGES, DOCUMENTS, AUDIO, VIDEO,
        FULLSCREEN_OFFLINE, OFFLINE_FILE_INFO, RECENT_BUCKET
    }

    public boolean isList = true;

    private long parentHandleBrowser;
    private long parentHandleRubbish;
    private long parentHandleIncoming;
    private long parentHandleLinks;
    private long parentHandleOutgoing;
    private long parentHandleSearch;
    private long parentHandleInbox;
    private String pathNavigationOffline;
    public int deepBrowserTreeIncoming = 0;
    public int deepBrowserTreeOutgoing = 0;
    private int deepBrowserTreeLinks;

    int indexShares = -1;
    int indexTransfers = -1;

    // Fragments
    private FileBrowserFragment fileBrowserFragment;
    private RubbishBinFragment rubbishBinFragment;
    private InboxFragment inboxFragment;
    private IncomingSharesFragment incomingSharesFragment;
    private OutgoingSharesFragment outgoingSharesFragment;
    private LinksFragment linksFragment;
    private TransfersFragment transfersFragment;
    private CompletedTransfersFragment completedTransfersFragment;
    private SearchFragment searchFragment;
    private PhotosFragment photosFragment;
    private AlbumContentFragment albumContentFragment;
    private RecentChatsFragment recentChatsFragment;
    private NotificationsFragment notificationsFragment;
    private TurnOnNotificationsFragment turnOnNotificationsFragment;
    private PermissionsFragment permissionsFragment;
    private SMSVerificationFragment smsVerificationFragment;
    private MediaDiscoveryFragment mediaDiscoveryFragment;

    private boolean mStopped = true;
    private int bottomItemBeforeOpenFullscreenOffline = INVALID_VALUE;
    private OfflineFragment fullscreenOfflineFragment;
    private OfflineFragment pagerOfflineFragment;
    private RecentsFragment pagerRecentsFragment;

    AlertDialog statusDialog;
    private AlertDialog processFileDialog;

    private AlertDialog permissionsDialog;
    private AlertDialog presenceStatusDialog;
    private AlertDialog alertNotPermissionsUpload;
    private AlertDialog clearRubbishBinDialog;
    private AlertDialog insertPassDialog;
    private AlertDialog changeUserAttributeDialog;
    private AlertDialog alertDialogStorageStatus;
    private AlertDialog alertDialogSMSVerification;
    private AlertDialog newTextFileDialog;
    private AlertDialog newFolderDialog;

    private MenuItem searchMenuItem;
    private MenuItem enableSelectMenuItem;
    private MenuItem doNotDisturbMenuItem;
    private MenuItem clearRubbishBinMenuitem;
    private MenuItem cancelAllTransfersMenuItem;
    private MenuItem playTransfersMenuIcon;
    private MenuItem pauseTransfersMenuIcon;
    private MenuItem retryTransfers;
    private MenuItem clearCompletedTransfers;
    private MenuItem scanQRcodeMenuItem;
    private MenuItem returnCallMenuItem;
    private MenuItem openMeetingMenuItem;
    private MenuItem openLinkMenuItem;
    private Chronometer chronometerMenuItem;
    private LinearLayout layoutCallMenuItem;

    private int typesCameraPermission = INVALID_TYPE_PERMISSIONS;
    AlertDialog enable2FADialog;
    boolean isEnable2FADialogShown = false;
    Button enable2FAButton;
    Button skip2FAButton;

    private boolean is2FAEnabled = false;

    public boolean comesFromNotifications = false;
    public int comesFromNotificationsLevel = 0;
    public long comesFromNotificationHandle = -1;
    public long comesFromNotificationHandleSaved = -1;
    public int comesFromNotificationDeepBrowserTreeIncoming = -1;

    RelativeLayout myAccountHeader;
    ImageView contactStatus;
    RelativeLayout myAccountSection;
    RelativeLayout inboxSection;
    RelativeLayout contactsSection;
    RelativeLayout notificationsSection;
    private RelativeLayout rubbishBinSection;
    RelativeLayout settingsSection;
    Button upgradeAccount;
    TextView contactsSectionText;
    TextView notificationsSectionText;
    int bottomNavigationCurrentItem = -1;
    View chatBadge;
    View callBadge;

    private boolean joiningToChatLink;
    private String linkJoinToChatLink;

    private boolean onAskingPermissionsFragment = false;
    public boolean onAskingSMSVerificationFragment = false;

    private View mNavHostView;
    private NavController mNavController;
    private HomepageSearchable mHomepageSearchable;

    private Boolean initFabButtonShow = false;

    private Observer<Boolean> fabChangeObserver = isShow -> {
        if (drawerItem == DrawerItem.HOMEPAGE) {
            controlFabInHomepage(isShow);
        } else if (isInMDMode) {
            hideFabButton();
        } else {
            if (initFabButtonShow) {
                if (isShow) {
                    showFabButtonAfterScrolling();
                } else {
                    hideFabButtonWhenScrolling();
                }
            }
        }
    };

    private final Observer<Boolean> refreshAddPhoneNumberButtonObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            if (drawerLayout != null) {
                drawerLayout.closeDrawer(Gravity.LEFT);
            }
            refreshAddPhoneNumberButton();
        }
    };

    private final Observer<Boolean> fileBackupChangedObserver = change -> {
        if (change) {
            megaApi.getMyBackupsFolder(this);
        }
    };

    private boolean isBusinessGraceAlertShown;
    private AlertDialog businessGraceAlert;
    private boolean isBusinessCUAlertShown;
    private AlertDialog businessCUAlert;

    private BottomSheetDialogFragment bottomSheetDialogFragment;
    private PsaViewHolder psaViewHolder;

    private AlertDialog openLinkDialog;
    private boolean openLinkDialogIsErrorShown = false;
    private EditText openLinkText;
    private RelativeLayout openLinkError;
    private TextView openLinkErrorText;
    private Button openLinkOpenButton;
    private static final int LINK_DIALOG_MEETING = 1;
    private static final int LINK_DIALOG_CHAT = 2;
    private int chatLinkDialogType = LINK_DIALOG_CHAT;

    // for Meeting
    boolean isFabExpanded = false;
    private static long FAB_ANIM_DURATION = 200L;
    private static long FAB_MASK_OUT_DELAY = 200L;
    private static float ALPHA_TRANSPARENT = 0f;
    private static float ALPHA_OPAQUE = 1f;
    private static float FAB_DEFAULT_ANGEL = 0f;
    private static float FAB_ROTATE_ANGEL = 135f;
    private static String KEY_IS_FAB_EXPANDED = "isFabExpanded";
    private static String MEETING_TYPE = MEETING_ACTION_CREATE;
    private View fabMaskLayout;
    private ViewGroup windowContent;
    private final ArrayList<View> fabs = new ArrayList<>();
    // end for Meeting

    // Backup warning dialog
    private AlertDialog backupWarningDialog;
    private ArrayList<Long> backupHandleList;
    private int backupDialogType = BACKUP_DIALOG_SHOW_NONE;
    private Long backupNodeHandle;
    private int backupNodeType;
    private int backupActionType;

    // Version removed
    private int versionsToRemove = 0;
    private int versionsRemoved = 0;
    private int errorVersionRemove = 0;

    /**
     * Broadcast to update the completed transfers tab.
     */
    private BroadcastReceiver transferFinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isTransfersCompletedAdded()) {
                return;
            }

            if (intent == null || intent.getAction() == null
                    || !intent.getAction().equals(BROADCAST_ACTION_TRANSFER_FINISH)) {
                return;
            }

            AndroidCompletedTransfer completedTransfer = intent.getParcelableExtra(COMPLETED_TRANSFER);
            if (completedTransfer == null) {
                return;
            }

            completedTransfersFragment.transferFinish(completedTransfer);
        }
    };


    /**
     * Broadcast to show a "transfer over quota" warning if it is on Transfers section.
     */
    private BroadcastReceiver transferOverQuotaUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWidget(intent);

            if (intent == null) return;

            if (intent.getAction() != null && intent.getAction().equals(ACTION_TRANSFER_OVER_QUOTA) && drawerItem == DrawerItem.TRANSFERS && isActivityInForeground()) {
                showTransfersTransferOverQuotaWarning();
            }

            if (MegaApplication.getTransfersManagement().thereAreFailedTransfers() && drawerItem == DrawerItem.TRANSFERS && getTabItemTransfers() == COMPLETED_TAB && !retryTransfers.isVisible()) {
                retryTransfers.setVisible(true);
            }
        }
    };

    private BroadcastReceiver chatArchivedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            String title = intent.getStringExtra(CHAT_TITLE);
            if (title != null) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.success_archive_chat, title), -1);
            }
        }
    };

    private BroadcastReceiver updateMyAccountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (ACTION_STORAGE_STATE_CHANGED.equals(intent.getAction())) {
                    storageStateFromBroadcast = intent.getIntExtra(EXTRA_STORAGE_STATE, MegaApiJava.STORAGE_STATE_UNKNOWN);
                    if (!showStorageAlertWithDelay) {
                        checkStorageStatus(storageStateFromBroadcast != MegaApiJava.STORAGE_STATE_UNKNOWN ?
                                storageStateFromBroadcast : app.getStorageState(), false);
                    }
                    updateAccountDetailsVisibleInfo();
                    return;
                }

                int actionType = intent.getIntExtra(ACTION_TYPE, INVALID_ACTION);

                if (actionType == UPDATE_ACCOUNT_DETAILS) {
                    logDebug("BROADCAST TO UPDATE AFTER UPDATE_ACCOUNT_DETAILS");
                    if (isFinishing()) {
                        return;
                    }

                    updateAccountDetailsVisibleInfo();

                    if (megaApi.isBusinessAccount()) {
                        supportInvalidateOptionsMenu();
                    }
                } else if (actionType == UPDATE_PAYMENT_METHODS) {
                    logDebug("BROADCAST TO UPDATE AFTER UPDATE_PAYMENT_METHODS");
                }
            }
        }
    };

    private final BroadcastReceiver receiverUpdateOrder = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !BROADCAST_ACTION_INTENT_UPDATE_ORDER.equals(intent.getAction())) {
                return;
            }

            if (intent.getBooleanExtra(IS_CLOUD_ORDER, true)) {
                refreshCloudOrder(intent.getIntExtra(NEW_ORDER, ORDER_DEFAULT_ASC));
            } else {
                refreshOthersOrder();
            }
        }
    };

    private BroadcastReceiver receiverCUAttrChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            synchronized (this) {
                if (drawerItem == DrawerItem.PHOTOS) {
                    cameraUploadsClicked();
                }

                //update folder icon
                onNodesCloudDriveUpdate();
            }
        }
    };

    private BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logDebug("Network broadcast received!");
            int actionType;

            if (intent != null) {
                actionType = intent.getIntExtra(ACTION_TYPE, INVALID_ACTION);

                if (actionType == GO_OFFLINE) {
                    //stop cu process
                    fireStopCameraUploadJob(ManagerActivity.this);
                    showOfflineMode();
                    LiveEventBus.get(EVENT_NETWORK_CHANGE, Boolean.class).post(false);
                } else if (actionType == GO_ONLINE) {
                    showOnlineMode();
                    LiveEventBus.get(EVENT_NETWORK_CHANGE, Boolean.class).post(true);
                } else if (actionType == START_RECONNECTION) {
                    refreshSession();
                }
            }
        }
    };

    private BroadcastReceiver contactUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null)
                return;


            long userHandle = intent.getLongExtra(EXTRA_USER_HANDLE, INVALID_HANDLE);

            if (intent.getAction().equals(ACTION_UPDATE_NICKNAME)
                    || intent.getAction().equals(ACTION_UPDATE_FIRST_NAME)
                    || intent.getAction().equals(ACTION_UPDATE_LAST_NAME)) {

                if (isIncomingAdded() && incomingSharesFragment.getItemCount() > 0) {
                    incomingSharesFragment.updateContact(userHandle);
                }

                if (isOutgoingAdded() && outgoingSharesFragment.getItemCount() > 0) {
                    outgoingSharesFragment.updateContact(userHandle);
                }
            }
        }
    };

    private final Observer<MegaChatCall> callStatusObserver = call -> {
        int callStatus = call.getStatus();
        switch (callStatus) {
            case MegaChatCall.CALL_STATUS_CONNECTING:
            case MegaChatCall.CALL_STATUS_IN_PROGRESS:
            case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION:
            case MegaChatCall.CALL_STATUS_DESTROYED:
            case MegaChatCall.CALL_STATUS_USER_NO_PRESENT:
                updateVisibleCallElements(call.getChatid());
                if (call.getStatus() == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION &&
                        call.getTermCode() == MegaChatCall.TERM_CODE_TOO_MANY_PARTICIPANTS) {
                    showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.call_error_too_many_participants), MEGACHAT_INVALID_HANDLE);
                }
                break;
        }
    };

    private final Observer<MegaChatCall> callOnHoldObserver = call -> updateVisibleCallElements(call.getChatid());

    private final Observer<Pair> sessionOnHoldObserver = sessionAndCall -> {
        MegaChatCall call = megaChatApi.getChatCallByCallId((long) sessionAndCall.first);
        updateVisibleCallElements(call.getChatid());
    };

    private BroadcastReceiver chatRoomMuteUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null)
                return;

            if (intent.getAction().equals(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING)) {
                if (getChatsFragment() != null) {
                    recentChatsFragment.notifyPushChanged();
                }
            }
        }
    };

    private final Observer<Boolean> refreshObserver = refreshed -> {
        if (!refreshed) {
            return;
        }

        if (drawerItem == DrawerItem.CLOUD_DRIVE) {
            MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);

            ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode != null
                            ? parentNode
                            : megaApi.getRootNode(),
                    sortOrderManagement.getOrderCloud());

            fileBrowserFragment.setNodes(nodes);
            fileBrowserFragment.getRecyclerView().invalidate();
        } else if (drawerItem == DrawerItem.SHARED_ITEMS) {
            refreshIncomingShares();
        }
    };

    private final BroadcastReceiver cuUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !ACTION_UPDATE_CU.equals(intent.getAction())) {
                return;
            }

            updateCUProgress(intent.getIntExtra(PROGRESS, 0),
                    intent.getIntExtra(PENDING_TRANSFERS, 0));
        }
    };

    private final Observer<Boolean> finishObserver = finish -> {
        if (finish) finish();
    };

    private FileBackupManager fileBackupManager;

    /**
     * Method for updating the visible elements related to a call.
     *
     * @param chatIdReceived The chat ID of a call.
     */
    private void updateVisibleCallElements(long chatIdReceived) {
        if (getChatsFragment() != null && recentChatsFragment.isVisible()) {
            recentChatsFragment.refreshNode(megaChatApi.getChatListItem(chatIdReceived));
        }

        if (isScreenInPortrait(ManagerActivity.this)) {
            setCallWidget();
        } else {
            supportInvalidateOptionsMenu();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_UPLOAD_CONTACT: {
                uploadContactInfo(infoManager, parentNodeManager);
                break;
            }
            case REQUEST_CAMERA: {
                if (typesCameraPermission == TAKE_PICTURE_OPTION) {
                    logDebug("TAKE_PICTURE_OPTION");
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if (!hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            requestPermission(this,
                                    REQUEST_WRITE_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        } else {
                            checkTakePicture(this, TAKE_PHOTO_CODE);
                            typesCameraPermission = INVALID_TYPE_PERMISSIONS;
                        }
                    }
                } else if ((typesCameraPermission == RETURN_CALL_PERMISSIONS || typesCameraPermission == START_CALL_PERMISSIONS) &&
                        grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    controlCallPermissions();
                }
                break;
            }
            case REQUEST_READ_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showUploadPanel();
                }
                break;
            }
            case REQUEST_WRITE_STORAGE: {
                if (firstLogin) {
                    logDebug("The first time");
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if (typesCameraPermission == TAKE_PICTURE_OPTION) {
                            logDebug("TAKE_PICTURE_OPTION");
                            if (!hasPermissions(this, Manifest.permission.CAMERA)) {
                                requestPermission(this, REQUEST_CAMERA, Manifest.permission.CAMERA);
                            } else {
                                checkTakePicture(this, TAKE_PHOTO_CODE);
                                typesCameraPermission = INVALID_TYPE_PERMISSIONS;
                            }

                            break;
                        }
                    }
                } else {
                    if (typesCameraPermission == TAKE_PICTURE_OPTION) {
                        logDebug("TAKE_PICTURE_OPTION");
                        if (!hasPermissions(this, Manifest.permission.CAMERA)) {
                            requestPermission(this,
                                    REQUEST_CAMERA,
                                    Manifest.permission.CAMERA);
                        } else {
                            checkTakePicture(this, TAKE_PHOTO_CODE);
                            typesCameraPermission = INVALID_TYPE_PERMISSIONS;
                        }
                    } else {
                        refreshOfflineNodes();
                    }

                    break;
                }

                nodeSaver.handleRequestPermissionsResult(requestCode);
                break;
            }

            case REQUEST_CAMERA_UPLOAD:
            case REQUEST_CAMERA_ON_OFF:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkIfShouldShowBusinessCUAlert();
                } else {
                    stopCameraUploadSyncHeartbeatWorkers(this);
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.on_refuse_storage_permission), INVALID_HANDLE);
                }

                break;

            case REQUEST_CAMERA_ON_OFF_FIRST_TIME:
                if (permissions.length == 0) {
                    return;
                }
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkIfShouldShowBusinessCUAlert();
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                        if (getPhotosFragment() != null) {
                            photosFragment.onStoragePermissionRefused();
                        }
                    } else {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.on_refuse_storage_permission), INVALID_HANDLE);
                    }
                }

                break;

            case PERMISSIONS_FRAGMENT: {
                if (getPermissionsFragment() != null) {
                    permissionsFragment.setNextPermission();
                }
                break;
            }

            case REQUEST_RECORD_AUDIO:
                if ((typesCameraPermission == RETURN_CALL_PERMISSIONS || typesCameraPermission == START_CALL_PERMISSIONS) &&
                        grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    controlCallPermissions();
                }
                break;

            case REQUEST_BT_CONNECT:
                logDebug("get Bluetooth Connect permission");
                if (permissions.length == 0) {
                    return;
                }
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (MEETING_TYPE.equals(MEETING_ACTION_CREATE)) {
                        openMeetingToCreate(this);
                    }
                } else {
                    showSnackbar(PERMISSIONS_TYPE, getString(R.string.meeting_bluetooth_connect_required_permissions_warning), INVALID_HANDLE);
                }
                break;
        }
    }

    /**
     * Method for checking the necessary actions when you have permission to start a call or return to one in progress.
     */
    private void controlCallPermissions() {
        if (checkPermissionsCall(this, typesCameraPermission)) {
            switch (typesCameraPermission) {
                case RETURN_CALL_PERMISSIONS:
                    returnActiveCall(this, passcodeManagement);
                    break;

                case START_CALL_PERMISSIONS:
                    MegaChatRoom chat = megaChatApi.getChatRoomByUser(MegaApplication.getUserWaitingForCall());
                    if (chat != null) {
                        startCallWithChatOnline(this, chat);
                    }
                    break;
            }
            typesCameraPermission = INVALID_TYPE_PERMISSIONS;
        }
    }

    public void setTypesCameraPermission(int typesCameraPermission) {
        this.typesCameraPermission = typesCameraPermission;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        logDebug("onSaveInstanceState");
        if (drawerItem != null) {
            logDebug("DrawerItem = " + drawerItem);
        } else {
            logWarning("DrawerItem is null");
        }
        super.onSaveInstanceState(outState);
        outState.putLong("parentHandleBrowser", parentHandleBrowser);
        outState.putLong("parentHandleRubbish", parentHandleRubbish);
        outState.putLong("parentHandleIncoming", parentHandleIncoming);
        logDebug("IN BUNDLE -> parentHandleOutgoing: " + parentHandleOutgoing);
        outState.putLong(PARENT_HANDLE_LINKS, parentHandleLinks);
        outState.putLong("parentHandleOutgoing", parentHandleOutgoing);
        outState.putLong("parentHandleSearch", parentHandleSearch);
        outState.putLong("parentHandleInbox", parentHandleInbox);
        outState.putSerializable("drawerItem", drawerItem);
        outState.putInt(BOTTOM_ITEM_BEFORE_OPEN_FULLSCREEN_OFFLINE,
                bottomItemBeforeOpenFullscreenOffline);
        outState.putSerializable(SEARCH_DRAWER_ITEM, searchDrawerItem);
        outState.putSerializable(SEARCH_SHARED_TAB, searchSharedTab);
        outState.putBoolean(EXTRA_FIRST_LOGIN, firstLogin);
        outState.putBoolean(STATE_KEY_SMS_DIALOG, isSMSDialogShowing);

        if (parentHandleIncoming != INVALID_HANDLE) {
            outState.putInt("deepBrowserTreeIncoming", deepBrowserTreeIncoming);
        }

        if (parentHandleOutgoing != INVALID_HANDLE) {
            outState.putInt("deepBrowserTreeOutgoing", deepBrowserTreeOutgoing);
        }

        if (parentHandleLinks != INVALID_HANDLE) {
            outState.putInt(DEEP_BROWSER_TREE_LINKS, deepBrowserTreeLinks);
        }

        if (viewPagerShares != null) {
            indexShares = viewPagerShares.getCurrentItem();
        }
        outState.putInt("indexShares", indexShares);

        outState.putString("pathNavigationOffline", pathNavigationOffline);

        if (searchQuery != null) {
            outState.putInt("levelsSearch", levelsSearch);
            outState.putString("searchQuery", searchQuery);
            textsearchQuery = true;
            outState.putBoolean("textsearchQuery", textsearchQuery);
        } else {
            textsearchQuery = false;
        }

        if (turnOnNotifications) {
            outState.putBoolean("turnOnNotifications", turnOnNotifications);
        }

        outState.putInt("orientationSaved", orientationSaved);
        outState.putBoolean("isEnable2FADialogShown", isEnable2FADialogShown);
        outState.putInt("bottomNavigationCurrentItem", bottomNavigationCurrentItem);
        outState.putBoolean("searchExpand", searchExpand);
        outState.putBoolean("comesFromNotifications", comesFromNotifications);
        outState.putInt("comesFromNotificationsLevel", comesFromNotificationsLevel);
        outState.putLong("comesFromNotificationHandle", comesFromNotificationHandle);
        outState.putLong("comesFromNotificationHandleSaved", comesFromNotificationHandleSaved);
        outState.putBoolean("onAskingPermissionsFragment", onAskingPermissionsFragment);
        permissionsFragment = (PermissionsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.PERMISSIONS.getTag());
        if (onAskingPermissionsFragment && permissionsFragment != null) {
            getSupportFragmentManager().putFragment(outState, FragmentTag.PERMISSIONS.getTag(), permissionsFragment);
        }
        outState.putBoolean("onAskingSMSVerificationFragment", onAskingSMSVerificationFragment);
        smsVerificationFragment = (SMSVerificationFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.SMS_VERIFICATION.getTag());
        if (onAskingSMSVerificationFragment && smsVerificationFragment != null) {
            getSupportFragmentManager().putFragment(outState, FragmentTag.SMS_VERIFICATION.getTag(), smsVerificationFragment);
        }
        outState.putInt("elevation", mElevationCause);
        outState.putInt("storageState", storageState);
        outState.putBoolean("isStorageStatusDialogShown", isStorageStatusDialogShown);
        outState.putInt("comesFromNotificationDeepBrowserTreeIncoming", comesFromNotificationDeepBrowserTreeIncoming);

        if (isAlertDialogShown(openLinkDialog)) {
            outState.putBoolean(OPEN_LINK_DIALOG_SHOWN, true);
            outState.putBoolean(OPEN_LINK_ERROR, openLinkDialogIsErrorShown);
            outState.putString(OPEN_LINK_TEXT, openLinkText != null && openLinkText.getText() != null
                    ? openLinkText.getText().toString() : "");
        }

        outState.putBoolean(BUSINESS_GRACE_ALERT_SHOWN, isBusinessGraceAlertShown);
        if (isBusinessCUAlertShown) {
            outState.putBoolean(BUSINESS_CU_ALERT_SHOWN, isBusinessCUAlertShown);
        }

        outState.putBoolean(TRANSFER_OVER_QUOTA_SHOWN, isTransferOverQuotaWarningShown);
        outState.putInt(TYPE_CALL_PERMISSION, typesCameraPermission);
        outState.putBoolean(JOINING_CHAT_LINK, joiningToChatLink);
        outState.putString(LINK_JOINING_CHAT_LINK, linkJoinToChatLink);
        outState.putBoolean(KEY_IS_FAB_EXPANDED, isFabExpanded);

        if (getPhotosFragment() != null) {
            getSupportFragmentManager().putFragment(outState, FragmentTag.PHOTOS.getTag(), photosFragment);
        }

        checkNewTextFileDialogState(newTextFileDialog, outState);

        nodeAttacher.saveState(outState);
        nodeSaver.saveState(outState);

        outState.putBoolean(PROCESS_FILE_DIALOG_SHOWN, isAlertDialogShown(processFileDialog));

        outState.putBoolean(STATE_KEY_IS_IN_MD_MODE, isInMDMode);
        mediaDiscoveryFragment = (MediaDiscoveryFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.MEDIA_DISCOVERY.getTag());
        if (mediaDiscoveryFragment != null) {
            getSupportFragmentManager().putFragment(outState, FragmentTag.MEDIA_DISCOVERY.getTag(), mediaDiscoveryFragment);
        }

        outState.putBoolean(STATE_KEY_IS_IN_ALBUM_CONTENT, isInAlbumContent);
        albumContentFragment = (AlbumContentFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.ALBUM_CONTENT.getTag());
        if (albumContentFragment != null) {
            getSupportFragmentManager().putFragment(outState, FragmentTag.ALBUM_CONTENT.getTag(), albumContentFragment);
        }

        backupWarningDialog = fileBackupManager.getBackupWarningDialog();
        if (backupWarningDialog != null && backupWarningDialog.isShowing()) {
            backupHandleList = fileBackupManager.getBackupHandleList();
            backupNodeHandle = fileBackupManager.getBackupNodeHandle();
            backupNodeType = fileBackupManager.getBackupNodeType();
            backupActionType = fileBackupManager.getBackupActionType();
            backupDialogType = fileBackupManager.getBackupDialogType();

            if (backupHandleList != null) {
                outState.putSerializable(BACKUP_HANDLED_ITEM, backupHandleList);
            }

            outState.putLong(BACKUP_HANDLED_NODE, backupNodeHandle);
            outState.putInt(BACKUP_NODE_TYPE, backupNodeType);
            outState.putInt(BACKUP_ACTION_TYPE, backupActionType);
            outState.putInt(BACKUP_DIALOG_WARN, backupDialogType);
            backupWarningDialog.dismiss();
        }

        checkNewFolderDialogState(newFolderDialog, outState);
    }

    @Override
    public void onStart() {
        logDebug("onStart");

        mStopped = false;

        super.onStart();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        logDebug("onCreate");
//		Fragments are restored during the Activity's onCreate().
//		Importantly though, they are restored in the base Activity class's onCreate().
//		Thus if you call super.onCreate() first, all of the rest of your onCreate() method will execute after your Fragments have been restored.
        super.onCreate(savedInstanceState);
        logDebug("onCreate after call super");

        viewModel = new ViewModelProvider(this).get(ManagerViewModel.class);
        viewModel.getUpdateUsers().observe(this, this::updateUsers);
        viewModel.getUpdateUserAlerts().observe(this, this::updateUserAlerts);
        viewModel.getUpdateNodes().observe(this, this::updateNodes);
        viewModel.getUpdateContactsRequests().observe(this, this::updateContactRequests);

        // This block for solving the issue below:
        // Android is installed for the first time. Press the “Open” button on the system installation dialog, press the home button to switch the app to background,
        // and then switch the app to foreground, causing the app to create a new instantiation.
        if (!isTaskRoot()) {
            Intent intent = getIntent();
            if (intent != null) {
                String action = intent.getAction();
                if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(action)) {
                    finish();
                    return;
                }
            }
        }

        boolean selectDrawerItemPending = true;

        getLifecycle().addObserver(cookieDialogHandler);

        boolean openLinkDialogIsShown = false;

        if (savedInstanceState != null) {
            logDebug("Bundle is NOT NULL");
            parentHandleBrowser = savedInstanceState.getLong("parentHandleBrowser", -1);
            logDebug("savedInstanceState -> parentHandleBrowser: " + parentHandleBrowser);
            parentHandleRubbish = savedInstanceState.getLong("parentHandleRubbish", -1);
            parentHandleIncoming = savedInstanceState.getLong("parentHandleIncoming", -1);
            logDebug("savedInstanceState -> parentHandleIncoming: " + parentHandleIncoming);
            parentHandleOutgoing = savedInstanceState.getLong("parentHandleOutgoing", -1);
            logDebug("savedInstanceState -> parentHandleOutgoing: " + parentHandleOutgoing);
            parentHandleLinks = savedInstanceState.getLong(PARENT_HANDLE_LINKS, INVALID_HANDLE);
            parentHandleSearch = savedInstanceState.getLong("parentHandleSearch", -1);
            parentHandleInbox = savedInstanceState.getLong("parentHandleInbox", -1);
            deepBrowserTreeIncoming = savedInstanceState.getInt("deepBrowserTreeIncoming", 0);
            deepBrowserTreeOutgoing = savedInstanceState.getInt("deepBrowserTreeOutgoing", 0);
            deepBrowserTreeLinks = savedInstanceState.getInt(DEEP_BROWSER_TREE_LINKS, 0);
            isSMSDialogShowing = savedInstanceState.getBoolean(STATE_KEY_SMS_DIALOG, false);
            firstLogin = savedInstanceState.getBoolean(EXTRA_FIRST_LOGIN);
            askPermissions = savedInstanceState.getBoolean(EXTRA_ASK_PERMISSIONS);
            drawerItem = (DrawerItem) savedInstanceState.getSerializable("drawerItem");
            bottomItemBeforeOpenFullscreenOffline = savedInstanceState.getInt(BOTTOM_ITEM_BEFORE_OPEN_FULLSCREEN_OFFLINE);
            searchDrawerItem = (DrawerItem) savedInstanceState.getSerializable(SEARCH_DRAWER_ITEM);
            searchSharedTab = savedInstanceState.getInt(SEARCH_SHARED_TAB);
            indexShares = savedInstanceState.getInt("indexShares", indexShares);
            logDebug("savedInstanceState -> indexShares: " + indexShares);
            pathNavigationOffline = savedInstanceState.getString("pathNavigationOffline", pathNavigationOffline);
            logDebug("savedInstanceState -> pathNavigationOffline: " + pathNavigationOffline);
            selectedAccountType = savedInstanceState.getInt("selectedAccountType", -1);
            searchQuery = savedInstanceState.getString("searchQuery");
            textsearchQuery = savedInstanceState.getBoolean("textsearchQuery");
            levelsSearch = savedInstanceState.getInt("levelsSearch");
            turnOnNotifications = savedInstanceState.getBoolean("turnOnNotifications", false);
            orientationSaved = savedInstanceState.getInt("orientationSaved");
            isEnable2FADialogShown = savedInstanceState.getBoolean("isEnable2FADialogShown", false);
            bottomNavigationCurrentItem = savedInstanceState.getInt("bottomNavigationCurrentItem", -1);
            searchExpand = savedInstanceState.getBoolean("searchExpand", false);
            comesFromNotifications = savedInstanceState.getBoolean("comesFromNotifications", false);
            comesFromNotificationsLevel = savedInstanceState.getInt("comesFromNotificationsLevel", 0);
            comesFromNotificationHandle = savedInstanceState.getLong("comesFromNotificationHandle", -1);
            comesFromNotificationHandleSaved = savedInstanceState.getLong("comesFromNotificationHandleSaved", -1);
            onAskingPermissionsFragment = savedInstanceState.getBoolean("onAskingPermissionsFragment", false);
            if (onAskingPermissionsFragment) {
                permissionsFragment = (PermissionsFragment) getSupportFragmentManager().getFragment(savedInstanceState, FragmentTag.PERMISSIONS.getTag());
            }
            onAskingSMSVerificationFragment = savedInstanceState.getBoolean("onAskingSMSVerificationFragment", false);
            if (onAskingSMSVerificationFragment) {
                smsVerificationFragment = (SMSVerificationFragment) getSupportFragmentManager().getFragment(savedInstanceState, FragmentTag.SMS_VERIFICATION.getTag());
            }
            mElevationCause = savedInstanceState.getInt("elevation", 0);
            storageState = savedInstanceState.getInt("storageState", MegaApiJava.STORAGE_STATE_UNKNOWN);
            isStorageStatusDialogShown = savedInstanceState.getBoolean("isStorageStatusDialogShown", false);
            comesFromNotificationDeepBrowserTreeIncoming = savedInstanceState.getInt("comesFromNotificationDeepBrowserTreeIncoming", -1);
            openLinkDialogIsShown = savedInstanceState.getBoolean(OPEN_LINK_DIALOG_SHOWN, false);
            isBusinessGraceAlertShown = savedInstanceState.getBoolean(BUSINESS_GRACE_ALERT_SHOWN, false);
            isBusinessCUAlertShown = savedInstanceState.getBoolean(BUSINESS_CU_ALERT_SHOWN, false);
            isTransferOverQuotaWarningShown = savedInstanceState.getBoolean(TRANSFER_OVER_QUOTA_SHOWN, false);
            typesCameraPermission = savedInstanceState.getInt(TYPE_CALL_PERMISSION, INVALID_TYPE_PERMISSIONS);
            joiningToChatLink = savedInstanceState.getBoolean(JOINING_CHAT_LINK, false);
            linkJoinToChatLink = savedInstanceState.getString(LINK_JOINING_CHAT_LINK);
            isFabExpanded = savedInstanceState.getBoolean(KEY_IS_FAB_EXPANDED, false);
            isInMDMode = savedInstanceState.getBoolean(STATE_KEY_IS_IN_MD_MODE, false);
            isInAlbumContent = savedInstanceState.getBoolean(STATE_KEY_IS_IN_ALBUM_CONTENT, false);

            nodeAttacher.restoreState(savedInstanceState);
            nodeSaver.restoreState(savedInstanceState);

            //upload from device, progress dialog should show when screen orientation changes.
            if (savedInstanceState.getBoolean(PROCESS_FILE_DIALOG_SHOWN, false)) {
                processFileDialog = showProcessFileDialog(this, null);
            }

            // Backup warning dialog
            backupHandleList = (ArrayList<Long>) savedInstanceState.getSerializable(BACKUP_HANDLED_ITEM);
            backupNodeHandle = savedInstanceState.getLong(BACKUP_HANDLED_NODE, -1);
            backupNodeType = savedInstanceState.getInt(BACKUP_NODE_TYPE, -1);
            backupActionType = savedInstanceState.getInt(BACKUP_ACTION_TYPE, -1);
            backupDialogType = savedInstanceState.getInt(BACKUP_DIALOG_WARN, BACKUP_DIALOG_SHOW_NONE);

            if (savedInstanceState.getBoolean(IS_NEW_FOLDER_DIALOG_SHOWN, false)) {
                showNewFolderDialog(savedInstanceState.getString(NEW_FOLDER_DIALOG_TEXT));
            }
        } else {
            logDebug("Bundle is NULL");
            parentHandleBrowser = -1;
            parentHandleRubbish = -1;
            parentHandleIncoming = -1;
            parentHandleOutgoing = -1;
            parentHandleLinks = INVALID_HANDLE;
            parentHandleSearch = -1;
            parentHandleInbox = -1;
            deepBrowserTreeIncoming = 0;
            deepBrowserTreeOutgoing = 0;
            deepBrowserTreeLinks = 0;
            this.setPathNavigationOffline(OFFLINE_ROOT);
        }

        IntentFilter contactUpdateFilter = new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE);
        contactUpdateFilter.addAction(ACTION_UPDATE_NICKNAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_FIRST_NAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_LAST_NAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_CREDENTIALS);
        registerReceiver(contactUpdateReceiver, contactUpdateFilter);

        IntentFilter filter = new IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS);
        filter.addAction(ACTION_STORAGE_STATE_CHANGED);
        registerReceiver(updateMyAccountReceiver, filter);

        registerReceiver(networkReceiver,
                new IntentFilter(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE));

        registerReceiver(receiverCUAttrChanged,
                new IntentFilter(BROADCAST_ACTION_INTENT_CU_ATTR_CHANGE));

        registerReceiver(receiverUpdateOrder, new IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_ORDER));

        LiveEventBus.get(EVENT_UPDATE_VIEW_MODE, Boolean.class)
                .observe(this, this::updateView);

        registerReceiver(chatArchivedReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_CHAT_ARCHIVED));

        LiveEventBus.get(EVENT_REFRESH_PHONE_NUMBER, Boolean.class)
                .observeForever(refreshAddPhoneNumberButtonObserver);

        IntentFilter filterTransfers = new IntentFilter(BROADCAST_ACTION_INTENT_TRANSFER_UPDATE);
        filterTransfers.addAction(ACTION_TRANSFER_OVER_QUOTA);
        registerReceiver(transferOverQuotaUpdateReceiver, filterTransfers);

        registerReceiver(transferFinishReceiver, new IntentFilter(BROADCAST_ACTION_TRANSFER_FINISH));

        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall.class).observe(this, callStatusObserver);
        LiveEventBus.get(EVENT_CALL_ON_HOLD_CHANGE, MegaChatCall.class).observe(this, callOnHoldObserver);
        LiveEventBus.get(EVENT_SESSION_ON_HOLD_CHANGE, Pair.class).observe(this, sessionOnHoldObserver);

        registerReceiver(chatRoomMuteUpdateReceiver, new IntentFilter(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING));

        registerTransfersReceiver();

        LiveEventBus.get(EVENT_REFRESH, Boolean.class).observeForever(refreshObserver);

        registerReceiver(cuUpdateReceiver, new IntentFilter(ACTION_UPDATE_CU));

        LiveEventBus.get(EVENT_FINISH_ACTIVITY, Boolean.class).observeForever(finishObserver);

        LiveEventBus.get(EVENT_MY_BACKUPS_FOLDER_CHANGED, Boolean.class).observeForever(fileBackupChangedObserver);

        smsDialogTimeChecker = new LastShowSMSDialogTimeChecker(this);
        nC = new NodeController(this);
        cC = new ContactController(this);
        aC = new AccountController(this);

        CacheFolderManager.createCacheFolders(this);

        dbH = DatabaseHandler.getDbHandler(getApplicationContext());

        managerActivity = this;
        app = (MegaApplication) getApplication();
        megaApi = app.getMegaApi();

        megaChatApi = app.getMegaChatApi();

        checkChatChanges();

        if (megaChatApi != null) {
            logDebug("retryChatPendingConnections()");
            megaChatApi.retryPendingConnections(false, null);
        }

        MegaApplication.getPushNotificationSettingManagement().getPushNotificationSetting();

        transfersInProgress = new ArrayList<Integer>();

        //sync local contacts to see who's on mega.
        if (hasPermissions(this, Manifest.permission.READ_CONTACTS) && app.getStorageState() != STORAGE_STATE_PAYWALL) {
            logDebug("sync mega contacts");
            MegaContactGetter getter = new MegaContactGetter(this);
            getter.getMegaContacts(megaApi, TimeUtils.WEEK, this);
        }

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = getResources().getDisplayMetrics().density;

        initFileBackupManager();

        if (dbH.getEphemeral() != null) {
            refreshSession();
            return;
        }

        if (dbH.getCredentials() == null) {
            Intent newIntent = getIntent();

            if (newIntent != null) {
                if (newIntent.getAction() != null) {
                    if (newIntent.getAction().equals(ACTION_EXPORT_MASTER_KEY) || newIntent.getAction().equals(ACTION_OPEN_MEGA_LINK) || newIntent.getAction().equals(ACTION_OPEN_MEGA_FOLDER_LINK)) {
                        openLink = true;
                    } else if (newIntent.getAction().equals(ACTION_CANCEL_CAM_SYNC)) {
                        fireStopCameraUploadJob(getApplicationContext());
                        finish();
                        return;
                    }
                }
            }

            if (!openLink) {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.putExtra(VISIBLE_FRAGMENT, TOUR_FRAGMENT);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            return;
        }

        prefs = dbH.getPreferences();
        if (prefs == null) {
            firstTimeAfterInstallation = true;
            isList = true;
        } else {
            if (prefs.getFirstTime() == null) {
                firstTimeAfterInstallation = true;
            } else {
                firstTimeAfterInstallation = Boolean.parseBoolean(prefs.getFirstTime());
            }
            if (prefs.getPreferredViewList() == null) {
                isList = true;
            } else {
                isList = Boolean.parseBoolean(prefs.getPreferredViewList());
            }
        }

        if (firstTimeAfterInstallation) {
            setStartScreenTimeStamp(this);
        }

        logDebug("Preferred View List: " + isList);

        LiveEventBus.get(EVENT_LIST_GRID_CHANGE, Boolean.class).post(isList);

        handler = new Handler();

        logDebug("Set view");
        setContentView(R.layout.activity_manager);

        observePsa();

        megaApi.getMyBackupsFolder(this);

        //Set toolbar
        abL = (AppBarLayout) findViewById(R.id.app_bar_layout);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        aB = getSupportActionBar();

        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

        fragmentLayout = (LinearLayout) findViewById(R.id.fragment_layout);

        bNV = (BottomNavigationView) findViewById(R.id.bottom_navigation_view);
        bNV.setOnNavigationItemSelectedListener(this);

        miniAudioPlayerController = new MiniAudioPlayerController(
                findViewById(R.id.mini_audio_player),
                () -> {
                    // we need update fragmentLayout's layout params when player view is closed.
                    if (bNV.getVisibility() == View.VISIBLE) {
                        showBNVImmediate();
                    }

                    return Unit.INSTANCE;
                });
        getLifecycle().addObserver(miniAudioPlayerController);

        //Set navigation view
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                refreshDrawerInfo(false);
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                refreshDrawerInfo(storageState == MegaApiAndroid.STORAGE_STATE_UNKNOWN);

                // Sync the account info after changing account information settings to keep the data the same
                updateAccountDetailsVisibleInfo();
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }

            /**
             * Method to refresh the info displayed in the drawer menu.
             *
             * @param refreshStorageInfo Parameter to indicate if refresh the storage info.
             */
            private void refreshDrawerInfo(boolean refreshStorageInfo) {
                if (!isOnline(managerActivity) || megaApi == null || megaApi.getRootNode() == null) {
                    disableNavigationViewLayout();
                } else {
                    resetNavigationViewLayout();
                }

                setContactStatus();

                if (!refreshStorageInfo) return;
                showAddPhoneNumberInMenu();
                refreshAccountInfo();
            }
        });
        nV = (NavigationView) findViewById(R.id.navigation_view);

        myAccountHeader = findViewById(R.id.navigation_drawer_account_section);
        myAccountHeader.setOnClickListener(this);
        contactStatus = (ImageView) findViewById(R.id.contact_state);
        myAccountSection = findViewById(R.id.my_account_section);
        myAccountSection.setOnClickListener(this);
        inboxSection = findViewById(R.id.inbox_section);
        inboxSection.setOnClickListener(this);
        contactsSection = findViewById(R.id.contacts_section);
        contactsSection.setOnClickListener(this);
        notificationsSection = findViewById(R.id.notifications_section);
        notificationsSection.setOnClickListener(this);
        notificationsSectionText = (TextView) findViewById(R.id.notification_section_text);
        contactsSectionText = (TextView) findViewById(R.id.contacts_section_text);
        findViewById(R.id.offline_section).setOnClickListener(this);
        RelativeLayout transfersSection = findViewById(R.id.transfers_section);
        transfersSection.setOnClickListener(this);
        rubbishBinSection = findViewById(R.id.rubbish_bin_section);
        rubbishBinSection.setOnClickListener(this);
        settingsSection = findViewById(R.id.settings_section);
        settingsSection.setOnClickListener(this);
        upgradeAccount = (Button) findViewById(R.id.upgrade_navigation_view);
        upgradeAccount.setOnClickListener(this);

        navigationDrawerAddPhoneContainer = findViewById(R.id.navigation_drawer_add_phone_number_container);

        addPhoneNumberButton = findViewById(R.id.navigation_drawer_add_phone_number_button);
        addPhoneNumberButton.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        Layout buttonLayout = addPhoneNumberButton.getLayout();
                        if (buttonLayout != null) {
                            if (buttonLayout.getLineCount() > 1) {
                                findViewById(R.id.navigation_drawer_add_phone_number_icon).setVisibility(View.GONE);
                            }
                            addPhoneNumberButton.getViewTreeObserver().removeOnPreDrawListener(this);
                        }

                        return true;
                    }
                }
        );
        addPhoneNumberButton.setOnClickListener(this);

        addPhoneNumberLabel = findViewById(R.id.navigation_drawer_add_phone_number_label);
        megaApi.getAccountAchievements(this);

        badgeDrawable = new BadgeDrawerArrowDrawable(managerActivity, R.color.red_600_red_300,
                R.color.white_dark_grey, R.color.white_dark_grey);

        BottomNavigationMenuView menuView = (BottomNavigationMenuView) bNV.getChildAt(0);
        // Navi button Chat
        BottomNavigationItemView itemView = (BottomNavigationItemView) menuView.getChildAt(3);
        chatBadge = LayoutInflater.from(this).inflate(R.layout.bottom_chat_badge, menuView, false);
        itemView.addView(chatBadge);
        setChatBadge();

        callBadge = LayoutInflater.from(this).inflate(R.layout.bottom_call_badge, menuView, false);
        itemView.addView(callBadge);
        callBadge.setVisibility(View.GONE);
        setCallBadge();

        usedSpaceLayout = findViewById(R.id.nv_used_space_layout);

        //FAB Button
        fabButton = (FloatingActionButton) findViewById(R.id.floating_button);
        fabButton.setOnClickListener(new FabButtonListener(this));
        setupFabs();

        //PRO PANEL
        getProLayout = (LinearLayout) findViewById(R.id.get_pro_account);
        getProLayout.setBackgroundColor(Util.isDarkMode(this)
                ? ColorUtils.getColorForElevation(this, 8f) : Color.WHITE);
        String getProTextString = getString(R.string.get_pro_account);
        try {
            getProTextString = getProTextString.replace("[A]", "\n");
        } catch (Exception e) {
            logError("Formatted string: " + getProTextString, e);
        }

        getProText = (TextView) findViewById(R.id.get_pro_account_text);
        getProText.setText(getProTextString);
        rightUpgradeButton = (TextView) findViewById(R.id.btnRight_upgrade);
        leftCancelButton = (TextView) findViewById(R.id.btnLeft_cancel);

        nVDisplayName = findViewById(R.id.navigation_drawer_account_information_display_name);
        nVDisplayName.setMaxWidthEmojis(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));

        nVEmail = (TextView) findViewById(R.id.navigation_drawer_account_information_email);
        nVPictureProfile = (RoundedImageView) findViewById(R.id.navigation_drawer_user_account_picture_profile);

        businessLabel = findViewById(R.id.business_label);
        businessLabel.setVisibility(View.GONE);

        fragmentContainer = findViewById(R.id.fragment_container);
        spaceTV = (TextView) findViewById(R.id.navigation_drawer_space);
        usedSpacePB = (ProgressBar) findViewById(R.id.manager_used_space_bar);

        cuViewTypes = findViewById(R.id.cu_view_type);
        cuYearsButton = findViewById(R.id.years_button);
        cuMonthsButton = findViewById(R.id.months_button);
        cuDaysButton = findViewById(R.id.days_button);
        cuAllButton = findViewById(R.id.all_button);
        cuLayout = findViewById(R.id.cu_layout);
        cuProgressBar = findViewById(R.id.cu_progress_bar);
        enableCUButton = findViewById(R.id.enable_cu_button);
        enableCUButton.setOnClickListener(v -> {
            if (getPhotosFragment() != null) {
                photosFragment.enableCameraUploadClick();
            }
        });

        //TABS section Shared Items
        tabLayoutShares = findViewById(R.id.sliding_tabs_shares);
        viewPagerShares = findViewById(R.id.shares_tabs_pager);
        viewPagerShares.setOffscreenPageLimit(3);

        viewPagerShares.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                logDebug("selectDrawerItemSharedItems - TabId: " + position);
                supportInvalidateOptionsMenu();
                checkScrollElevation();
                setSharesTabIcons(position);
                switch (position) {
                    case INCOMING_TAB:
                        if (isOutgoingAdded() && outgoingSharesFragment.isMultipleSelect()) {
                            outgoingSharesFragment.getActionMode().finish();
                        } else if (isLinksAdded() && linksFragment.isMultipleSelect()) {
                            linksFragment.getActionMode().finish();
                        }
                        break;
                    case OUTGOING_TAB:
                        if (isIncomingAdded() && incomingSharesFragment.isMultipleSelect()) {
                            incomingSharesFragment.getActionMode().finish();
                        } else if (isLinksAdded() && linksFragment.isMultipleSelect()) {
                            linksFragment.getActionMode().finish();
                        }
                        break;
                    case LINKS_TAB:
                        if (isIncomingAdded() && incomingSharesFragment.isMultipleSelect()) {
                            incomingSharesFragment.getActionMode().finish();
                        } else if (isOutgoingAdded() && outgoingSharesFragment.isMultipleSelect()) {
                            outgoingSharesFragment.getActionMode().finish();
                        }
                        break;
                }
                setToolbarTitle();
                showFabButton();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        //Tab section Transfers
        tabLayoutTransfers = (TabLayout) findViewById(R.id.sliding_tabs_transfers);
        viewPagerTransfers = findViewById(R.id.transfers_tabs_pager);
        viewPagerTransfers.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                supportInvalidateOptionsMenu();
                checkScrollElevation();

                if (position == PENDING_TAB && isTransfersInProgressAdded()) {
                    transfersFragment.setGetMoreQuotaViewVisibility();
                } else if (position == COMPLETED_TAB) {
                    if (isTransfersCompletedAdded()) {
                        completedTransfersFragment.setGetMoreQuotaViewVisibility();
                    }

                    if (isTransfersInProgressAdded()) {
                        transfersFragment.checkSelectModeAfterChangeTabOrDrawerItem();
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        callInProgressLayout = findViewById(R.id.call_in_progress_layout);
        callInProgressLayout.setOnClickListener(this);
        callInProgressChrono = findViewById(R.id.call_in_progress_chrono);
        callInProgressText = findViewById(R.id.call_in_progress_text);
        microOffLayout = findViewById(R.id.micro_off_layout);
        videoOnLayout = findViewById(R.id.video_on_layout);
        callInProgressLayout.setVisibility(View.GONE);

        if (mElevationCause > 0) {
            // A work around: mElevationCause will be changed unexpectedly shortly
            int elevationCause = mElevationCause;
            // Apply the previous Appbar elevation(e.g. before rotation) after all views have been created
            handler.postDelayed(() -> changeAppBarElevation(true, elevationCause), 100);
        }

        mNavHostView = findViewById(R.id.nav_host_fragment);
        setupNavDestListener();

        setTransfersWidgetLayout(findViewById(R.id.transfers_widget_layout), this);

        transferData = megaApi.getTransferData(this);
        if (transferData != null) {
            for (int i = 0; i < transferData.getNumDownloads(); i++) {
                int tag = transferData.getDownloadTag(i);
                transfersInProgress.add(tag);
                MegaApplication.getTransfersManagement().checkIfTransferIsPaused(tag);
            }

            for (int i = 0; i < transferData.getNumUploads(); i++) {
                int tag = transferData.getUploadTag(i);
                transfersInProgress.add(transferData.getUploadTag(i));
                MegaApplication.getTransfersManagement().checkIfTransferIsPaused(tag);
            }
        }

        if (!isOnline(this)) {
            logDebug("No network -> SHOW OFFLINE MODE");

            if (drawerItem == null) {
                drawerItem = DrawerItem.HOMEPAGE;
            }

            selectDrawerItem(drawerItem);
            showOfflineMode();

            UserCredentials credentials = dbH.getCredentials();
            if (credentials != null) {
                String gSession = credentials.getSession();
                ChatUtil.initMegaChatApi(gSession, this);
            }

            return;
        }

        ///Check the MK or RK file
        logInfo("App version: " + getVersion());
        final File fMKOld = buildExternalStorageFile(OLD_MK_FILE);
        final File fRKOld = buildExternalStorageFile(OLD_RK_FILE);
        if (isFileAvailable(fMKOld)) {
            logDebug("Old MK file need to be renamed!");
            aC.renameRK(fMKOld);
        } else if (isFileAvailable(fRKOld)) {
            logDebug("Old RK file need to be renamed!");
            aC.renameRK(fRKOld);
        }

        rootNode = megaApi.getRootNode();
        if (rootNode == null || LoginActivity.isBackFromLoginPage) {
            if (getIntent() != null) {
                logDebug("Action: " + getIntent().getAction());
                if (getIntent().getAction() != null) {
                    if (getIntent().getAction().equals(ACTION_IMPORT_LINK_FETCH_NODES)) {
                        Intent intent = new Intent(managerActivity, LoginActivity.class);
                        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setAction(ACTION_IMPORT_LINK_FETCH_NODES);
                        intent.setData(Uri.parse(getIntent().getDataString()));
                        startActivity(intent);
                        finish();
                        return;
                    } else if (getIntent().getAction().equals(ACTION_OPEN_MEGA_LINK)) {
                        Intent intent = new Intent(managerActivity, FileLinkActivity.class);
                        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setAction(ACTION_IMPORT_LINK_FETCH_NODES);
                        intent.setData(Uri.parse(getIntent().getDataString()));
                        startActivity(intent);
                        finish();
                        return;
                    } else if (getIntent().getAction().equals(ACTION_OPEN_MEGA_FOLDER_LINK)) {
                        Intent intent = new Intent(managerActivity, LoginActivity.class);
                        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setAction(ACTION_OPEN_MEGA_FOLDER_LINK);
                        intent.setData(Uri.parse(getIntent().getDataString()));
                        startActivity(intent);
                        finish();
                        return;
                    } else if (getIntent().getAction().equals(ACTION_OPEN_CHAT_LINK)) {
                        Intent intent = new Intent(managerActivity, LoginActivity.class);
                        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setAction(ACTION_OPEN_CHAT_LINK);
                        intent.setData(Uri.parse(getIntent().getDataString()));
                        startActivity(intent);
                        finish();
                        return;
                    } else if (getIntent().getAction().equals(ACTION_CANCEL_CAM_SYNC)) {
                        fireStopCameraUploadJob(getApplicationContext());
                        finish();
                        return;
                    } else if (getIntent().getAction().equals(ACTION_EXPORT_MASTER_KEY)) {
                        Intent intent = new Intent(managerActivity, LoginActivity.class);
                        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setAction(getIntent().getAction());
                        startActivity(intent);
                        finish();
                        return;
                    } else if (getIntent().getAction().equals(ACTION_SHOW_TRANSFERS)) {
                        Intent intent = new Intent(managerActivity, LoginActivity.class);
                        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setAction(ACTION_SHOW_TRANSFERS);
                        intent.putExtra(TRANSFERS_TAB, getIntent().getIntExtra(TRANSFERS_TAB, ERROR_TAB));
                        startActivity(intent);
                        finish();
                        return;
                    } else if (getIntent().getAction().equals(ACTION_IPC)) {
                        Intent intent = new Intent(managerActivity, LoginActivity.class);
                        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setAction(ACTION_IPC);
                        startActivity(intent);
                        finish();
                        return;
                    } else if (getIntent().getAction().equals(ACTION_CHAT_NOTIFICATION_MESSAGE)) {
                        Intent intent = new Intent(managerActivity, LoginActivity.class);
                        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setAction(ACTION_CHAT_NOTIFICATION_MESSAGE);
                        startActivity(intent);
                        finish();
                        return;
                    } else if (getIntent().getAction().equals(ACTION_CHAT_SUMMARY)) {
                        Intent intent = new Intent(managerActivity, LoginActivity.class);
                        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setAction(ACTION_CHAT_SUMMARY);
                        startActivity(intent);
                        finish();
                        return;
                    } else if (getIntent().getAction().equals(ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION)) {
                        Intent intent = new Intent(managerActivity, LoginActivity.class);
                        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setAction(ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION);
                        startActivity(intent);
                        finish();
                        return;
                    } else if (getIntent().getAction().equals(ACTION_OPEN_HANDLE_NODE)) {
                        Intent intent = new Intent(managerActivity, LoginActivity.class);
                        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setAction(ACTION_OPEN_HANDLE_NODE);
                        intent.setData(Uri.parse(getIntent().getDataString()));
                        startActivity(intent);
                        finish();
                        return;
                    } else if (getIntent().getAction().equals(ACTION_OVERQUOTA_TRANSFER)) {
                        Intent intent = new Intent(managerActivity, LoginActivity.class);
                        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setAction(ACTION_OVERQUOTA_TRANSFER);
                        startActivity(intent);
                        finish();
                        return;
                    } else if (getIntent().getAction().equals(ACTION_OVERQUOTA_STORAGE)) {
                        Intent intent = new Intent(managerActivity, LoginActivity.class);
                        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setAction(ACTION_OVERQUOTA_STORAGE);
                        startActivity(intent);
                        finish();
                        return;
                    } else if (getIntent().getAction().equals(ACTION_OPEN_CONTACTS_SECTION)) {
                        logDebug("Login");
                        Intent intent = new Intent(managerActivity, LoginActivity.class);
                        intent.putExtra(CONTACT_HANDLE, getIntent().getLongExtra(CONTACT_HANDLE, -1));
                        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setAction(ACTION_OPEN_CONTACTS_SECTION);
                        startActivity(intent);
                        finish();
                        return;
                    } else if (getIntent().getAction().equals(ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE)) {
                        Intent intent = new Intent(managerActivity, LoginActivity.class);
                        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setAction(ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE);
                        startActivity(intent);
                        finish();
                        return;
                    } else if (getIntent().getAction().equals(ACTION_SHOW_UPGRADE_ACCOUNT)) {
                        Intent intent = new Intent(managerActivity, LoginActivity.class);
                        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setAction(ACTION_SHOW_UPGRADE_ACCOUNT);
                        startActivity(intent);
                        finish();
                        return;
                    }
                }
            }

            refreshSession();
            return;
        } else {
            inboxNode = megaApi.getInboxNode();
            attr = dbH.getAttributes();
            if (attr != null) {
                if (attr.getInvalidateSdkCache() != null) {
                    if (attr.getInvalidateSdkCache().compareTo("") != 0) {
                        try {
                            if (Boolean.parseBoolean(attr.getInvalidateSdkCache())) {
                                logDebug("megaApi.invalidateCache();");
                                megaApi.invalidateCache();
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }

            dbH.setInvalidateSdkCache(false);
            MegaMessageService.getToken(this);
            nVEmail.setVisibility(View.VISIBLE);
            nVEmail.setText(megaApi.getMyEmail());
            megaApi.getUserAttribute(MegaApiJava.USER_ATTR_FIRSTNAME, this);
            megaApi.getUserAttribute(MegaApiJava.USER_ATTR_LASTNAME, this);

            this.setDefaultAvatar();

            this.setProfileAvatar();

            initPayments();

            megaApi.isGeolocationEnabled(this);

            if (savedInstanceState == null) {
                logDebug("Run async task to check offline files");
                //Check the consistency of the offline nodes in the DB
                CheckOfflineNodesTask checkOfflineNodesTask = new CheckOfflineNodesTask(this);
                checkOfflineNodesTask.execute();
            }

            if (getIntent() != null) {
                if (getIntent().getAction() != null) {
                    if (getIntent().getAction().equals(ACTION_EXPORT_MASTER_KEY)) {
                        logDebug("Intent to export Master Key - im logged in!");
                        startActivity(new Intent(this, ExportRecoveryKeyActivity.class));
                        return;
                    } else if (getIntent().getAction().equals(ACTION_CANCEL_ACCOUNT)) {
                        Uri link = getIntent().getData();
                        if (link != null) {
                            logDebug("Link to cancel: " + link);
                            showMyAccount(ACTION_CANCEL_ACCOUNT, link);
                        }
                    } else if (getIntent().getAction().equals(ACTION_CHANGE_MAIL)) {
                        Uri link = getIntent().getData();
                        if (link != null) {
                            logDebug("Link to change mail: " + link);
                            showMyAccount(ACTION_CHANGE_MAIL, link);
                        }
                    } else if (getIntent().getAction().equals(ACTION_OPEN_FOLDER)) {
                        logDebug("Open after LauncherFileExplorerActivity ");
                        boolean locationFileInfo = getIntent().getBooleanExtra(INTENT_EXTRA_KEY_LOCATION_FILE_INFO, false);
                        long handleIntent = getIntent().getLongExtra("PARENT_HANDLE", -1);

                        if (getIntent().getBooleanExtra(SHOW_MESSAGE_UPLOAD_STARTED, false)) {
                            int numberUploads = getIntent().getIntExtra(NUMBER_UPLOADS, 1);
                            showSnackbar(SNACKBAR_TYPE, getResources().getQuantityString(R.plurals.upload_began, numberUploads, numberUploads), -1);
                        }

                        if (locationFileInfo) {
                            boolean offlineAdapter = getIntent().getBooleanExtra("offline_adapter", false);
                            if (offlineAdapter) {
                                drawerItem = DrawerItem.HOMEPAGE;
                                selectDrawerItem(drawerItem);
                                selectDrawerItemPending = false;
                                openFullscreenOfflineFragment(
                                        getIntent().getStringExtra(INTENT_EXTRA_KEY_PATH_NAVIGATION));
                            } else {
                                long fragmentHandle = getIntent().getLongExtra("fragmentHandle", -1);

                                if (fragmentHandle == megaApi.getRootNode().getHandle()) {
                                    drawerItem = DrawerItem.CLOUD_DRIVE;
                                    setParentHandleBrowser(handleIntent);
                                    selectDrawerItem(drawerItem);
                                    selectDrawerItemPending = false;
                                } else if (fragmentHandle == megaApi.getRubbishNode().getHandle()) {
                                    drawerItem = DrawerItem.RUBBISH_BIN;
                                    setParentHandleRubbish(handleIntent);
                                    selectDrawerItem(drawerItem);
                                    selectDrawerItemPending = false;
                                } else if (fragmentHandle == megaApi.getInboxNode().getHandle()) {
                                    drawerItem = DrawerItem.INBOX;
                                    setParentHandleInbox(handleIntent);
                                    selectDrawerItem(drawerItem);
                                    selectDrawerItemPending = false;
                                } else {
                                    //Incoming
                                    drawerItem = DrawerItem.SHARED_ITEMS;
                                    indexShares = 0;
                                    MegaNode parentIntentN = megaApi.getNodeByHandle(handleIntent);
                                    if (parentIntentN != null) {
                                        deepBrowserTreeIncoming = calculateDeepBrowserTreeIncoming(parentIntentN, this);
                                    }
                                    setParentHandleIncoming(handleIntent);
                                    selectDrawerItem(drawerItem);
                                    selectDrawerItemPending = false;
                                }
                            }
                        } else {
                            actionOpenFolder(handleIntent);
                        }

                        setIntent(null);
                    } else if (getIntent().getAction().equals(ACTION_PASS_CHANGED)) {
                        showMyAccount(ACTION_PASS_CHANGED, null,
                                new Pair<>(RESULT, getIntent().getIntExtra(RESULT, MegaError.API_OK)));
                    } else if (getIntent().getAction().equals(ACTION_RESET_PASS)) {
                        Uri link = getIntent().getData();
                        if (link != null) {
                            showMyAccount(ACTION_RESET_PASS, link);
                        }
                    } else if (getIntent().getAction().equals(ACTION_IPC)) {
                        logDebug("IPC link - go to received request in Contacts");
                        markNotificationsSeen(true);
                        navigateToContactRequests();
                        getIntent().setAction(null);
                        setIntent(null);
                    } else if (getIntent().getAction().equals(ACTION_CHAT_NOTIFICATION_MESSAGE)) {
                        logDebug("Chat notitificacion received");
                        drawerItem = DrawerItem.CHAT;
                        selectDrawerItem(drawerItem);
                        long chatId = getIntent().getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE);
                        if (getIntent().getBooleanExtra(EXTRA_MOVE_TO_CHAT_SECTION, false)) {
                            moveToChatSection(chatId);
                        } else {
                            String text = getIntent().getStringExtra(SHOW_SNACKBAR);
                            if (chatId != -1) {
                                openChat(chatId, text);
                            }
                        }
                        selectDrawerItemPending = false;
                        getIntent().setAction(null);
                        setIntent(null);
                    } else if (getIntent().getAction().equals(ACTION_CHAT_SUMMARY)) {
                        logDebug("Chat notification: ACTION_CHAT_SUMMARY");
                        drawerItem = DrawerItem.CHAT;
                        selectDrawerItem(drawerItem);
                        selectDrawerItemPending = false;
                        getIntent().setAction(null);
                        setIntent(null);
                    } else if (getIntent().getAction().equals(ACTION_OPEN_CHAT_LINK)) {
                        logDebug("ACTION_OPEN_CHAT_LINK: " + getIntent().getDataString());
                        drawerItem = DrawerItem.CHAT;
                        selectDrawerItem(drawerItem);
                        selectDrawerItemPending = false;
                        megaChatApi.checkChatLink(getIntent().getDataString(), new LoadPreviewListener(ManagerActivity.this, ManagerActivity.this, CHECK_LINK_TYPE_UNKNOWN_LINK));
                        getIntent().setAction(null);
                        setIntent(null);
                    } else if (getIntent().getAction().equals(ACTION_JOIN_OPEN_CHAT_LINK)) {
                        linkJoinToChatLink = getIntent().getDataString();
                        joiningToChatLink = true;

                        if (megaChatApi.getConnectionState() == MegaChatApi.CONNECTED) {
                            megaChatApi.checkChatLink(linkJoinToChatLink, new LoadPreviewListener(ManagerActivity.this, ManagerActivity.this, CHECK_LINK_TYPE_UNKNOWN_LINK));
                        }

                        getIntent().setAction(null);
                        setIntent(null);
                    } else if (getIntent().getAction().equals(ACTION_SHOW_SETTINGS)) {
                        logDebug("Chat notification: SHOW_SETTINGS");
                        selectDrawerItemPending = false;
                        moveToSettingsSection();
                        getIntent().setAction(null);
                        setIntent(null);
                    } else if (getIntent().getAction().equals(ACTION_SHOW_SETTINGS_STORAGE)) {
                        logDebug("ACTION_SHOW_SETTINGS_STORAGE");
                        selectDrawerItemPending = false;
                        moveToSettingsSectionStorage();
                        getIntent().setAction(null);
                        setIntent(null);
                    } else if (getIntent().getAction().equals(ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION)) {
                        logDebug("ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION");
                        markNotificationsSeen(true);

                        drawerItem = DrawerItem.SHARED_ITEMS;
                        indexShares = 0;
                        selectDrawerItem(drawerItem);
                        selectDrawerItemPending = false;
                    } else if (getIntent().getAction().equals(ACTION_SHOW_MY_ACCOUNT)) {
                        logDebug("Intent from chat - show my account");

                        if (getIntent().hasExtra(MeetingParticipantBottomSheetDialogFragment.EXTRA_FROM_MEETING)) {
                            isFromMeeting = getIntent().getBooleanExtra(MeetingParticipantBottomSheetDialogFragment.EXTRA_FROM_MEETING, false);
                        }

                        showMyAccount();
                        selectDrawerItemPending = false;
                    } else if (getIntent().getAction().equals(ACTION_SHOW_UPGRADE_ACCOUNT)) {
                        navigateToUpgradeAccount();
                        selectDrawerItemPending = false;
                    } else if (getIntent().getAction().equals(ACTION_OPEN_HANDLE_NODE)) {
                        String link = getIntent().getDataString();
                        String[] s = link.split("#");
                        if (s.length > 1) {
                            String nodeHandleLink = s[1];
                            String[] sSlash = s[1].split("/");
                            if (sSlash.length > 0) {
                                nodeHandleLink = sSlash[0];
                            }
                            long nodeHandleLinkLong = MegaApiAndroid.base64ToHandle(nodeHandleLink);
                            MegaNode nodeLink = megaApi.getNodeByHandle(nodeHandleLinkLong);
                            if (nodeLink == null) {
                                showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error_file_not_found), -1);
                            } else {
                                MegaNode pN = megaApi.getParentNode(nodeLink);
                                if (pN == null) {
                                    pN = megaApi.getRootNode();
                                }
                                parentHandleBrowser = pN.getHandle();
                                drawerItem = DrawerItem.CLOUD_DRIVE;
                                selectDrawerItem(drawerItem);
                                selectDrawerItemPending = false;

                                Intent i = new Intent(this, FileInfoActivity.class);
                                i.putExtra("handle", nodeLink.getHandle());
                                i.putExtra(NAME, nodeLink.getName());
                                startActivity(i);
                            }
                        } else {
                            drawerItem = DrawerItem.CLOUD_DRIVE;
                            selectDrawerItem(drawerItem);
                        }
                    } else if (getIntent().getAction().equals(ACTION_IMPORT_LINK_FETCH_NODES)) {
                        getIntent().setAction(null);
                        setIntent(null);
                    } else if (getIntent().getAction().equals(ACTION_OPEN_CONTACTS_SECTION)) {
                        markNotificationsSeen(true);
                        openContactLink(getIntent().getLongExtra(CONTACT_HANDLE, -1));
                    } else if (getIntent().getAction().equals(ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE)) {
                        long chatId = getIntent().getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE);
                        showSnackbar(MESSAGE_SNACKBAR_TYPE, null, chatId);
                        getIntent().setAction(null);
                        setIntent(null);
                    }
                }
            }

            logDebug("Check if there any unread chat");
            if (joiningToChatLink && !isTextEmpty(linkJoinToChatLink)) {
                megaChatApi.checkChatLink(linkJoinToChatLink, new LoadPreviewListener(ManagerActivity.this, ManagerActivity.this, CHECK_LINK_TYPE_UNKNOWN_LINK));
            }

            if (drawerItem == DrawerItem.CHAT) {
                recentChatsFragment = (RecentChatsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
                if (recentChatsFragment != null) {
                    recentChatsFragment.onlineStatusUpdate(megaChatApi.getOnlineStatus());
                }
            }
            setChatBadge();

            logDebug("Check if there any INCOMING pendingRequest contacts");
            setContactTitleSection();

            setNotificationsTitleSection();

            if (drawerItem == null) {
                drawerItem = getStartDrawerItem(this);

                Intent intent = getIntent();
                if (intent != null) {
                    boolean upgradeAccount = getIntent().getBooleanExtra(EXTRA_UPGRADE_ACCOUNT, false);
                    newAccount = getIntent().getBooleanExtra(EXTRA_NEW_ACCOUNT, false);
                    newCreationAccount = getIntent().getBooleanExtra(NEW_CREATION_ACCOUNT, false);
                    firstLogin = getIntent().getBooleanExtra(EXTRA_FIRST_LOGIN, firstLogin);
                    askPermissions = getIntent().getBooleanExtra(EXTRA_ASK_PERMISSIONS, askPermissions);

                    //reset flag to fix incorrect view loaded when orientation changes
                    getIntent().removeExtra(EXTRA_NEW_ACCOUNT);
                    getIntent().removeExtra(EXTRA_UPGRADE_ACCOUNT);
                    getIntent().removeExtra(EXTRA_FIRST_LOGIN);
                    getIntent().removeExtra(EXTRA_ASK_PERMISSIONS);
                    if (upgradeAccount) {
                        int accountType = getIntent().getIntExtra(EXTRA_ACCOUNT_TYPE, 0);

                        if (accountType != FREE) {
                            showMyAccount(new Pair<>(EXTRA_ACCOUNT_TYPE, accountType));
                        } else if (firstLogin && app.getStorageState() != STORAGE_STATE_PAYWALL) {
                            drawerItem = DrawerItem.PHOTOS;
                        } else {
                            showMyAccount();
                        }
                    } else {
                        if (firstLogin && app.getStorageState() != STORAGE_STATE_PAYWALL) {
                            logDebug("First login. Go to Camera Uploads configuration.");
                            drawerItem = DrawerItem.PHOTOS;
                            setIntent(null);
                        }
                    }
                }
            } else {
                logDebug("DRAWERITEM NOT NULL: " + drawerItem);
                Intent intentRec = getIntent();
                if (intentRec != null) {
                    boolean upgradeAccount = getIntent().getBooleanExtra(EXTRA_UPGRADE_ACCOUNT, false);
                    newAccount = getIntent().getBooleanExtra(EXTRA_NEW_ACCOUNT, false);
                    newCreationAccount = getIntent().getBooleanExtra(NEW_CREATION_ACCOUNT, false);
                    //reset flag to fix incorrect view loaded when orientation changes
                    getIntent().removeExtra(EXTRA_NEW_ACCOUNT);
                    getIntent().removeExtra(EXTRA_UPGRADE_ACCOUNT);
                    firstLogin = intentRec.getBooleanExtra(EXTRA_FIRST_LOGIN, firstLogin);
                    askPermissions = intentRec.getBooleanExtra(EXTRA_ASK_PERMISSIONS, askPermissions);
                    if (upgradeAccount) {
                        drawerLayout.closeDrawer(Gravity.LEFT);
                        int accountType = getIntent().getIntExtra(EXTRA_ACCOUNT_TYPE, 0);

                        if (accountType != FREE) {
                            showMyAccount(new Pair<>(EXTRA_ACCOUNT_TYPE, accountType));
                        } else if (firstLogin && app.getStorageState() != STORAGE_STATE_PAYWALL) {
                            drawerItem = DrawerItem.PHOTOS;
                        } else {
                            showMyAccount();
                        }
                    } else {
                        if (firstLogin && !joiningToChatLink) {
                            logDebug("Intent firstTimeCam==true");
                            if (prefs != null && prefs.getCamSyncEnabled() != null) {
                                firstLogin = false;
                            } else {
                                firstLogin = true;
                                if (app.getStorageState() != STORAGE_STATE_PAYWALL && isInPhotosPage()) {
                                    drawerItem = DrawerItem.PHOTOS;
                                }
                            }
                            setIntent(null);
                        }
                    }

                    if (intentRec.getAction() != null) {
                        if (intentRec.getAction().equals(ACTION_SHOW_TRANSFERS)) {
                            if (intentRec.getBooleanExtra(OPENED_FROM_CHAT, false)) {
                                sendBroadcast(new Intent(ACTION_CLOSE_CHAT_AFTER_OPEN_TRANSFERS));
                            }

                            drawerItem = DrawerItem.TRANSFERS;
                            indexTransfers = intentRec.getIntExtra(TRANSFERS_TAB, ERROR_TAB);
                            setIntent(null);
                        } else if (intentRec.getAction().equals(ACTION_REFRESH_AFTER_BLOCKED)) {
                            drawerItem = DrawerItem.CLOUD_DRIVE;
                            setIntent(null);
                        }
                    }
                }
                drawerLayout.closeDrawer(Gravity.LEFT);
            }

            checkCurrentStorageStatus(true);
            fireCameraUploadJob(ManagerActivity.this, false);

            //INITIAL FRAGMENT
            if (selectDrawerItemPending) {
                selectDrawerItem(drawerItem);
            }
        }

        new CompositeDisposable().add(checkPasswordReminderUseCase.check(false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(show -> {
                    if (show) {
                        startActivity(new Intent(this, TestPasswordActivity.class));
                    }
                }, throwable -> logError("doUpdateProgressNotification onError", throwable)));

        updateAccountDetailsVisibleInfo();

        setContactStatus();

        checkInitialScreens();

        if (openLinkDialogIsShown) {
            showOpenLinkDialog();
            String text = savedInstanceState.getString(OPEN_LINK_TEXT, "");
            openLinkText.setText(text);
            openLinkText.setSelection(text.length());
            if (savedInstanceState.getBoolean(OPEN_LINK_ERROR, false)) {
                openLink(text);
            }
        }

        if (drawerItem == DrawerItem.TRANSFERS && isTransferOverQuotaWarningShown) {
            showTransfersTransferOverQuotaWarning();
        }

        PsaManager.INSTANCE.startChecking();

        if (savedInstanceState != null && savedInstanceState.getBoolean(IS_NEW_TEXT_FILE_SHOWN, false)) {
            showNewTextFileDialog(savedInstanceState.getString(NEW_TEXT_FILE_TEXT));
        }

        logDebug("END onCreate");
        new RatingHandlerImpl(this).showRatingBaseOnTransaction();

        // Show backup dialog
        if (backupDialogType == BACKUP_DIALOG_SHOW_WARNING) {
            fileBackupManager.actWithBackupTips(backupHandleList, megaApi.getNodeByHandle(backupNodeHandle), backupNodeType, backupActionType);
        } else if (backupDialogType == BACKUP_DIALOG_SHOW_CONFIRM) {
            fileBackupManager.confirmationActionForBackup(backupHandleList, megaApi.getNodeByHandle(backupNodeHandle), backupNodeType, backupActionType);
        } else {
            logDebug("Backup warning dialog is not show");
        }
    }

    /**
     * Checks which screen should be shown when an user is logins.
     * There are three different screens or warnings:
     * - Business warning: it takes priority over the other two
     * - SMS verification screen: it takes priority over the other one
     * - Onboarding permissions screens: it has to be only shown when account is logged in after the installation,
     * some of the permissions required have not been granted
     * and the business warnings and SMS verification have not to be shown.
     */
    private void checkInitialScreens() {
        if (checkBusinessStatus()) {
            myAccountInfo.setBusinessAlertShown(true);
            return;
        }

        if (firstTimeAfterInstallation || askPermissions) {
            //haven't verified phone number
            if (canVoluntaryVerifyPhoneNumber() && !onAskingPermissionsFragment && !newCreationAccount) {
                askForSMSVerification();
            } else {
                drawerItem = DrawerItem.ASK_PERMISSIONS;
                askForAccess();
            }
        } else if (firstLogin && !newCreationAccount && canVoluntaryVerifyPhoneNumber() && !onAskingPermissionsFragment) {
            askForSMSVerification();
        }
    }

    /**
     * Checks if some business warning has to be shown due to the status of the account.
     *
     * @return True if some warning has been shown, false otherwise.
     */
    private boolean checkBusinessStatus() {
        if (!megaApi.isBusinessAccount()) {
            return false;
        }

        if (isBusinessGraceAlertShown) {
            showBusinessGraceAlert();
            return true;
        }

        if (isBusinessCUAlertShown) {
            showBusinessCUAlert();
            return true;
        }

        if (myAccountInfo.isBusinessAlertShown()) {
            return false;
        }

        if (firstLogin && myAccountInfo.wasNotBusinessAlertShownYet()) {
            int status = megaApi.getBusinessStatus();

            if (status == BUSINESS_STATUS_EXPIRED) {
                myAccountInfo.setBusinessAlertShown(true);
                startActivity(new Intent(this, BusinessExpiredAlertActivity.class));
                return true;
            } else if (megaApi.isMasterBusinessAccount() && status == BUSINESS_STATUS_GRACE_PERIOD) {
                myAccountInfo.setBusinessAlertShown(true);
                showBusinessGraceAlert();
                return true;
            }
        }

        return false;
    }

    private void showBusinessGraceAlert() {
        logDebug("showBusinessGraceAlert");
        if (businessGraceAlert != null && businessGraceAlert.isShowing()) {
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_business_grace_alert, null);

        businessGraceAlert = builder.setView(v)
                .setPositiveButton(R.string.general_dismiss, (dialog, which) -> {
                    myAccountInfo.setBusinessAlertShown(isBusinessGraceAlertShown = false);
                    try {
                        businessGraceAlert.dismiss();
                    } catch (Exception e) {
                        logWarning("Exception dismissing businessGraceAlert", e);
                    }
                })
                .create();

        businessGraceAlert.setCanceledOnTouchOutside(false);
        try {
            businessGraceAlert.show();
        } catch (Exception e) {
            logWarning("Exception showing businessGraceAlert", e);
        }
        isBusinessGraceAlertShown = true;
    }

    /**
     * If the account is business and not a master user, it shows a warning.
     * Otherwise proceeds to enable CU.
     */
    public void checkIfShouldShowBusinessCUAlert() {
        if (megaApi.isBusinessAccount() && !megaApi.isMasterBusinessAccount()) {
            showBusinessCUAlert();
        } else {
            enableCUClicked();
        }
    }

    /**
     * Proceeds to enable CU action.
     */
    private void enableCUClicked() {
        if (getPhotosFragment() != null) {
            if (photosFragment.isEnablePhotosFragmentShown()) {
                photosFragment.enableCameraUpload();
            } else {
                photosFragment.enableCameraUploadClick();
            }
        }
    }

    /**
     * Shows a warning to business users about the risks of enabling CU.
     */
    private void showBusinessCUAlert() {
        if (businessCUAlert != null && businessCUAlert.isShowing()) {
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.section_photo_sync)
                .setMessage(R.string.camera_uploads_business_alert)
                .setNegativeButton(R.string.general_cancel, (dialog, which) -> {
                })
                .setPositiveButton(R.string.general_enable, (dialog, which) -> {
                    if (getPhotosFragment() != null) {
                        photosFragment.enableCameraUploadClick();
                    }
                })
                .setCancelable(false)
                .setOnDismissListener(dialog -> isBusinessCUAlertShown = false);

        businessCUAlert = builder.create();
        businessCUAlert.show();
        isBusinessCUAlertShown = true;
    }

    private void openContactLink(long handle) {
        if (handle == INVALID_HANDLE) {
            logWarning("Not valid contact handle");
            return;
        }

        dismissAlertDialogIfExists(openLinkDialog);
        logDebug("Handle to invite a contact: " + handle);

        inviteContactUseCase.getContactLink(handle)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result, throwable) -> {
                    if (throwable == null) {
                        showContactInviteDialog(result.getContactLinkHandle(), result.getFullName(), result.getEmail(), result.isContact());
                    }
                });
    }

    /**
     * Show contact invite dialog.
     *
     * @param linkHandle User link handle for the invitation
     * @param fullName   User full name
     * @param email      User email
     * @param isContact  Flag to check wether is contact or not
     */
    private void showContactInviteDialog(Long linkHandle, String fullName, String email, boolean isContact) {
        if (inviteContactDialog != null && inviteContactDialog.isShowing()) return;

        String message;
        String buttonText;

        if (isContact) {
            message = getString(R.string.context_contact_already_exists, email);
            buttonText = getString(R.string.contact_view);
        } else {
            message = getString(R.string.invite_not_sent);
            buttonText = getString(R.string.contact_invite);
        }

        inviteContactDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(fullName)
                .setMessage(message)
                .setNegativeButton(R.string.general_cancel, null)
                .setPositiveButton(buttonText, (dialog, which) -> {
                    if (isContact) {
                        ContactUtil.openContactInfoActivity(this, email);
                    } else {
                        sendContactInvitation(linkHandle, email);
                    }

                    dialog.dismiss();
                    inviteContactDialog = null;
                })
                .create();
        inviteContactDialog.show();
    }

    /**
     * Send contact invitation to specific user and show specific SnackBar.
     *
     * @param contactLinkHandle User link handle for invitation
     * @param email             User email
     */
    private void sendContactInvitation(Long contactLinkHandle, String email) {
        inviteContactUseCase.invite(contactLinkHandle, email)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result, throwable) -> {
                    String snackbarMessage = getString(R.string.general_error);
                    if (throwable == null) {
                        switch (result) {
                            case SENT:
                                snackbarMessage = getString(R.string.context_contact_request_sent, email);
                                break;
                            case RESENT:
                                snackbarMessage = getString(R.string.context_contact_invitation_resent);
                                break;
                            case DELETED:
                                snackbarMessage = getString(R.string.context_contact_invitation_deleted);
                                break;
                            case ALREADY_SENT:
                                snackbarMessage = getString(R.string.invite_not_sent_already_sent, email);
                                break;
                            case ALREADY_CONTACT:
                                snackbarMessage = getString(R.string.context_contact_already_exists, email);
                                break;
                            case INVALID_EMAIL:
                                snackbarMessage = getString(R.string.error_own_email_as_contact);
                                break;
                        }
                    }
                    showSnackbar(SNACKBAR_TYPE, snackbarMessage, MEGACHAT_INVALID_HANDLE);
                });
    }

    private void askForSMSVerification() {
        if (!smsDialogTimeChecker.shouldShow()) return;
        showStorageAlertWithDelay = true;
        //If mobile device, only portrait mode is allowed
        if (!isTablet(this)) {
            logDebug("mobile only portrait mode");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        smsDialogTimeChecker.update();
        onAskingSMSVerificationFragment = true;
        if (smsVerificationFragment == null) {
            smsVerificationFragment = new SMSVerificationFragment();
        }
        replaceFragment(smsVerificationFragment, FragmentTag.SMS_VERIFICATION.getTag());
        tabLayoutShares.setVisibility(View.GONE);
        viewPagerShares.setVisibility(View.GONE);
        tabLayoutTransfers.setVisibility(View.GONE);
        viewPagerTransfers.setVisibility(View.GONE);
        abL.setVisibility(View.GONE);

        fragmentContainer.setVisibility(View.VISIBLE);
        drawerLayout.closeDrawer(Gravity.LEFT);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        supportInvalidateOptionsMenu();
        hideFabButton();
        showHideBottomNavigationView(true);
    }

    public void askForAccess() {
        askPermissions = false;
        showStorageAlertWithDelay = true;
        //If mobile device, only portrait mode is allowed
        if (!isTablet(this)) {
            logDebug("Mobile only portrait mode");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        boolean writeStorageGranted = hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        boolean readStorageGranted = hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        boolean cameraGranted = hasPermissions(this, Manifest.permission.CAMERA);
        boolean microphoneGranted = hasPermissions(this, Manifest.permission.RECORD_AUDIO);

        if (!writeStorageGranted || !readStorageGranted || !cameraGranted || !microphoneGranted/* || !writeCallsGranted*/) {
            deleteCurrentFragment();

            if (permissionsFragment == null) {
                permissionsFragment = new PermissionsFragment();
            }

            replaceFragment(permissionsFragment, FragmentTag.PERMISSIONS.getTag());

            onAskingPermissionsFragment = true;

            abL.setVisibility(View.GONE);
            setTabsVisibility();
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            supportInvalidateOptionsMenu();
            hideFabButton();
            showHideBottomNavigationView(true);
        }
    }

    public void destroySMSVerificationFragment() {
        if (!isTablet(this)) {
            logDebug("mobile, all orientation");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        }
        onAskingSMSVerificationFragment = false;
        smsVerificationFragment = null;

        if (!firstTimeAfterInstallation) {
            abL.setVisibility(View.VISIBLE);

            deleteCurrentFragment();

            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            supportInvalidateOptionsMenu();
            selectDrawerItem(drawerItem);
        }
    }

    public void destroyPermissionsFragment() {
        //In mobile, allow all orientation after permission screen
        if (!isTablet(this)) {
            logDebug("Mobile, all orientation");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        }

        turnOnNotifications = false;

        abL.setVisibility(View.VISIBLE);

        deleteCurrentFragment();

        onAskingPermissionsFragment = false;

        permissionsFragment = null;

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        supportInvalidateOptionsMenu();

        if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
            drawerItem = DrawerItem.CLOUD_DRIVE;
        } else {
            firstLogin = true;
            drawerItem = DrawerItem.PHOTOS;
        }

        selectDrawerItem(drawerItem);
    }

    void setContactStatus() {
        if (megaChatApi == null) {
            megaChatApi = app.getMegaChatApi();
            composite.clear();
            checkChatChanges();
        }

        int chatStatus = megaChatApi.getOnlineStatus();
        if (contactStatus != null) {
            ChatUtil.setContactStatus(chatStatus, contactStatus, StatusIconLocation.DRAWER);
        }
    }

    @Override
    protected void onResume() {
        if (drawerItem == DrawerItem.SEARCH && getSearchFragment() != null) {
            searchFragment.setWaitingForSearchedNodes(true);
        }

        super.onResume();
        queryIfNotificationsAreOn();

        if (getResources().getConfiguration().orientation != orientationSaved) {
            orientationSaved = getResources().getConfiguration().orientation;
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }

        checkScrollElevation();
        checkTransferOverQuotaOnResume();
        LiveEventBus.get(EVENT_FAB_CHANGE, Boolean.class).observeForever(fabChangeObserver);
    }

    void queryIfNotificationsAreOn() {
        logDebug("queryIfNotificationsAreOn");

        if (dbH == null) {
            dbH = DatabaseHandler.getDbHandler(getApplicationContext());
        }

        if (megaApi == null) {
            megaApi = ((MegaApplication) getApplication()).getMegaApi();
        }

        if (turnOnNotifications) {
            setTurnOnNotificationsFragment();
        } else {
            NotificationManagerCompat nf = NotificationManagerCompat.from(this);
            logDebug("Notifications Enabled: " + nf.areNotificationsEnabled());
            if (!nf.areNotificationsEnabled()) {
                logDebug("OFF");
                if (dbH.getShowNotifOff() == null || dbH.getShowNotifOff().equals("true")) {
                    if (megaChatApi == null) {
                        megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
                    }
                    if ((megaApi.getContacts().size() >= 1) || (megaChatApi.getChatListItems().size() >= 1)) {
                        setTurnOnNotificationsFragment();
                    }
                }
            }
        }
    }

    public void deleteTurnOnNotificationsFragment() {
        logDebug("deleteTurnOnNotificationsFragment");
        turnOnNotifications = false;

        abL.setVisibility(View.VISIBLE);

        turnOnNotificationsFragment = null;

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        supportInvalidateOptionsMenu();
        selectDrawerItem(drawerItem);

        setStatusBarColor(this, android.R.color.transparent);
    }

    void deleteCurrentFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(currentFragment).commitNowAllowingStateLoss();
        }
    }

    void setTurnOnNotificationsFragment() {
        logDebug("setTurnOnNotificationsFragment");
        aB.setSubtitle(null);
        abL.setVisibility(View.GONE);

        deleteCurrentFragment();

        if (turnOnNotificationsFragment == null) {
            turnOnNotificationsFragment = new TurnOnNotificationsFragment();
        }
        replaceFragment(turnOnNotificationsFragment, FragmentTag.TURN_ON_NOTIFICATIONS.getTag());

        setTabsVisibility();
        abL.setVisibility(View.GONE);

        drawerLayout.closeDrawer(Gravity.LEFT);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        supportInvalidateOptionsMenu();
        hideFabButton();
        showHideBottomNavigationView(true);

        setStatusBarColor(this, R.color.teal_500_teal_400);
    }

    void actionOpenFolder(long handleIntent) {
        if (handleIntent == INVALID_HANDLE) {
            logWarning("handleIntent is not valid");
            return;
        }

        MegaNode parentIntentN = megaApi.getNodeByHandle(handleIntent);
        if (parentIntentN == null) {
            logWarning("parentIntentN is null");
            return;
        }

        switch (megaApi.getAccess(parentIntentN)) {
            case MegaShare.ACCESS_READ:
            case MegaShare.ACCESS_READWRITE:
            case MegaShare.ACCESS_FULL:
                parentHandleIncoming = handleIntent;
                deepBrowserTreeIncoming = calculateDeepBrowserTreeIncoming(parentIntentN, this);
                drawerItem = DrawerItem.SHARED_ITEMS;
                break;

            default:
                if (megaApi.isInRubbish(parentIntentN)) {
                    parentHandleRubbish = handleIntent;
                    drawerItem = DrawerItem.RUBBISH_BIN;
                } else if (megaApi.isInInbox(parentIntentN)) {
                    parentHandleInbox = handleIntent;
                    drawerItem = DrawerItem.INBOX;
                } else {
                    parentHandleBrowser = handleIntent;
                    drawerItem = DrawerItem.CLOUD_DRIVE;
                }
                break;
        }
    }

    @Override
    protected void onPostResume() {
        logDebug("onPostResume");
        super.onPostResume();

        if (isSearching) {
            selectDrawerItem(DrawerItem.SEARCH);
            isSearching = false;
            return;
        }

        managerActivity = this;

        Intent intent = getIntent();

        dbH = DatabaseHandler.getDbHandler(getApplicationContext());
        if (dbH.getCredentials() == null) {
            if (!openLink) {
                return;
            } else {
                logDebug("Not credentials");
                if (intent != null) {
                    logDebug("Not credentials -> INTENT");
                    if (intent.getAction() != null) {
                        logDebug("Intent with ACTION: " + intent.getAction());

                        if (getIntent().getAction().equals(ACTION_EXPORT_MASTER_KEY)) {
                            Intent exportIntent = new Intent(managerActivity, LoginActivity.class);
                            intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                            exportIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            exportIntent.setAction(getIntent().getAction());
                            startActivity(exportIntent);
                            finish();
                            return;
                        }
                    }
                }
            }
        }

        if (intent != null) {
            logDebug("Intent not null! " + intent.getAction());
            // Open folder from the intent
            if (intent.hasExtra(EXTRA_OPEN_FOLDER)) {
                logDebug("INTENT: EXTRA_OPEN_FOLDER");

                parentHandleBrowser = intent.getLongExtra(EXTRA_OPEN_FOLDER, -1);
                intent.removeExtra(EXTRA_OPEN_FOLDER);
                setIntent(null);
            }

            if (intent.getAction() != null) {
                logDebug("Intent action");

                if (getIntent().getAction().equals(ACTION_EXPLORE_ZIP)) {
                    logDebug("Open zip browser");

                    String pathZip = intent.getExtras().getString(EXTRA_PATH_ZIP);
                    ZipBrowserActivity.Companion.start(this, pathZip);
                }

                if (getIntent().getAction().equals(ACTION_IMPORT_LINK_FETCH_NODES)) {
                    logDebug("ACTION_IMPORT_LINK_FETCH_NODES");

                    Intent loginIntent = new Intent(managerActivity, LoginActivity.class);
                    intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                    loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    loginIntent.setAction(ACTION_IMPORT_LINK_FETCH_NODES);
                    loginIntent.setData(Uri.parse(getIntent().getDataString()));
                    startActivity(loginIntent);
                    finish();
                    return;
                } else if (getIntent().getAction().equals(ACTION_OPEN_MEGA_LINK)) {
                    logDebug("ACTION_OPEN_MEGA_LINK");

                    Intent fileLinkIntent = new Intent(managerActivity, FileLinkActivity.class);
                    fileLinkIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    fileLinkIntent.setAction(ACTION_IMPORT_LINK_FETCH_NODES);
                    String data = getIntent().getDataString();
                    if (data != null) {
                        fileLinkIntent.setData(Uri.parse(data));
                        startActivity(fileLinkIntent);
                    } else {
                        logWarning("getDataString is NULL");
                    }
                    finish();
                    return;
                } else if (intent.getAction().equals(ACTION_OPEN_MEGA_FOLDER_LINK)) {
                    logDebug("ACTION_OPEN_MEGA_FOLDER_LINK");

                    Intent intentFolderLink = new Intent(managerActivity, FolderLinkActivity.class);
                    intentFolderLink.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intentFolderLink.setAction(ACTION_OPEN_MEGA_FOLDER_LINK);

                    String data = getIntent().getDataString();
                    if (data != null) {
                        intentFolderLink.setData(Uri.parse(data));
                        startActivity(intentFolderLink);
                    } else {
                        logWarning("getDataString is NULL");
                    }
                    finish();
                } else if (intent.getAction().equals(ACTION_REFRESH_PARENTHANDLE_BROWSER)) {

                    parentHandleBrowser = intent.getLongExtra("parentHandle", -1);
                    intent.removeExtra("parentHandle");

                    //Refresh Cloud Fragment
                    refreshCloudDrive();

                    //Refresh Rubbish Fragment
                    refreshRubbishBin();
                } else if (intent.getAction().equals(ACTION_OVERQUOTA_STORAGE)) {
                    showOverquotaAlert(false);
                } else if (intent.getAction().equals(ACTION_PRE_OVERQUOTA_STORAGE)) {
                    showOverquotaAlert(true);
                } else if (intent.getAction().equals(ACTION_CHANGE_AVATAR)) {
                    logDebug("Intent CHANGE AVATAR");

                    String path = intent.getStringExtra("IMAGE_PATH");
                    megaApi.setAvatar(path, this);
                } else if (intent.getAction().equals(ACTION_CANCEL_CAM_SYNC)) {
                    logDebug("ACTION_CANCEL_UPLOAD or ACTION_CANCEL_DOWNLOAD or ACTION_CANCEL_CAM_SYNC");
                    drawerItem = DrawerItem.TRANSFERS;
                    indexTransfers = intent.getIntExtra(TRANSFERS_TAB, ERROR_TAB);
                    selectDrawerItem(drawerItem);

                    String text = getString(R.string.cam_sync_cancel_sync);

                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
                    builder.setMessage(text);

                    builder.setPositiveButton(getString(R.string.general_yes),
                            (dialog, whichButton) -> {
                                fireStopCameraUploadJob(ManagerActivity.this);
                                dbH.setCamSyncEnabled(false);
                                sendBroadcast(new Intent(ACTION_UPDATE_DISABLE_CU_SETTING));

                                if (drawerItem == DrawerItem.PHOTOS) {
                                    cuLayout.setVisibility(View.VISIBLE);
                                }
                            });

                    builder.setNegativeButton(getString(R.string.general_no), null);
                    final AlertDialog dialog = builder.create();
                    try {
                        dialog.show();
                    } catch (Exception ex) {
                        logError("EXCEPTION", ex);
                    }
                } else if (intent.getAction().equals(ACTION_SHOW_TRANSFERS)) {
                    if (intent.getBooleanExtra(OPENED_FROM_CHAT, false)) {
                        sendBroadcast(new Intent(ACTION_CLOSE_CHAT_AFTER_OPEN_TRANSFERS));
                    }

                    drawerItem = DrawerItem.TRANSFERS;
                    indexTransfers = intent.getIntExtra(TRANSFERS_TAB, ERROR_TAB);
                    selectDrawerItem(drawerItem);
                } else if (intent.getAction().equals(ACTION_TAKE_SELFIE)) {
                    logDebug("Intent take selfie");
                    checkTakePicture(this, TAKE_PHOTO_CODE);
                } else if (intent.getAction().equals(SHOW_REPEATED_UPLOAD)) {
                    logDebug("Intent SHOW_REPEATED_UPLOAD");
                    String message = intent.getStringExtra("MESSAGE");
                    showSnackbar(SNACKBAR_TYPE, message, -1);
                } else if (getIntent().getAction().equals(ACTION_IPC)) {
                    logDebug("IPC - go to received request in Contacts");
                    markNotificationsSeen(true);
                    navigateToContactRequests();
                } else if (getIntent().getAction().equals(ACTION_CHAT_NOTIFICATION_MESSAGE)) {
                    logDebug("ACTION_CHAT_NOTIFICATION_MESSAGE");

                    long chatId = getIntent().getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE);
                    if (getIntent().getBooleanExtra(EXTRA_MOVE_TO_CHAT_SECTION, false)) {
                        moveToChatSection(chatId);
                    } else {
                        String text = getIntent().getStringExtra(SHOW_SNACKBAR);
                        if (chatId != -1) {
                            openChat(chatId, text);
                        }
                    }
                } else if (getIntent().getAction().equals(ACTION_CHAT_SUMMARY)) {
                    logDebug("ACTION_CHAT_SUMMARY");
                    drawerItem = DrawerItem.CHAT;
                    selectDrawerItem(drawerItem);
                } else if (getIntent().getAction().equals(ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION)) {
                    logDebug("ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION");
                    markNotificationsSeen(true);

                    drawerItem = DrawerItem.SHARED_ITEMS;
                    indexShares = 0;
                    selectDrawerItem(drawerItem);
                } else if (getIntent().getAction().equals(ACTION_OPEN_CONTACTS_SECTION)) {
                    logDebug("ACTION_OPEN_CONTACTS_SECTION");
                    markNotificationsSeen(true);
                    openContactLink(getIntent().getLongExtra(CONTACT_HANDLE, -1));
                } else if (getIntent().getAction().equals(ACTION_RECOVERY_KEY_EXPORTED)) {
                    logDebug("ACTION_RECOVERY_KEY_EXPORTED");
                    exportRecoveryKey();
                } else if (getIntent().getAction().equals(ACTION_REQUEST_DOWNLOAD_FOLDER_LOGOUT)) {
                    String parentPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);

                    if (parentPath != null) {
                        AccountController ac = new AccountController(this);
                        ac.exportMK(parentPath);
                    }
                } else if (getIntent().getAction().equals(ACTION_RECOVERY_KEY_COPY_TO_CLIPBOARD)) {
                    AccountController ac = new AccountController(this);
                    if (getIntent().getBooleanExtra("logout", false)) {
                        ac.copyMK(true, sharingScope);
                    } else {
                        ac.copyMK(false, sharingScope);
                    }
                } else if (getIntent().getAction().equals(ACTION_OPEN_FOLDER)) {
                    logDebug("Open after LauncherFileExplorerActivity ");
                    long handleIntent = getIntent().getLongExtra(INTENT_EXTRA_KEY_PARENT_HANDLE, -1);

                    if (getIntent().getBooleanExtra(SHOW_MESSAGE_UPLOAD_STARTED, false)) {
                        int numberUploads = getIntent().getIntExtra(NUMBER_UPLOADS, 1);
                        showSnackbar(SNACKBAR_TYPE, getResources().getQuantityString(R.plurals.upload_began, numberUploads, numberUploads), -1);
                    }

                    actionOpenFolder(handleIntent);
                    selectDrawerItem(drawerItem);
                } else if (getIntent().getAction().equals(ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE)) {
                    long chatId = getIntent().getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE);
                    showSnackbar(MESSAGE_SNACKBAR_TYPE, null, chatId);
                }

                intent.setAction(null);
                setIntent(null);
            }
        }


        if (bNV != null) {
            Menu nVMenu = bNV.getMenu();
            resetNavigationViewMenu(nVMenu);

            switch (drawerItem) {
                case CLOUD_DRIVE: {
                    logDebug("Case CLOUD DRIVE");
                    //Check the tab to shown and the title of the actionBar
                    setToolbarTitle();
                    setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV);
                    break;
                }
                case SHARED_ITEMS: {
                    logDebug("Case SHARED ITEMS");
                    setBottomNavigationMenuItemChecked(SHARED_ITEMS_BNV);
                    try {
                        NotificationManager notificationManager =
                                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                        notificationManager.cancel(NOTIFICATION_PUSH_CLOUD_DRIVE);
                    } catch (Exception e) {
                        logError("Exception NotificationManager - remove contact notification", e);
                    }
                    setToolbarTitle();
                    break;
                }
                case SEARCH: {
                    if (searchExpand) {
                        textsearchQuery = false;
                        break;
                    }

                    setBottomNavigationMenuItemChecked(NO_BNV);
                    setToolbarTitle();
                    break;
                }
                case CHAT:
                    setBottomNavigationMenuItemChecked(CHAT_BNV);
                    if (getChatsFragment() != null && recentChatsFragment.isVisible()) {
                        recentChatsFragment.setChats();
                        recentChatsFragment.setStatus();
                    }
                    MegaApplication.setRecentChatVisible(true);
                    break;

                case PHOTOS: {
                    setBottomNavigationMenuItemChecked(PHOTOS_BNV);
                    break;
                }
                case NOTIFICATIONS: {
                    notificationsFragment = (NotificationsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.NOTIFICATIONS.getTag());
                    if (notificationsFragment != null) {
                        notificationsFragment.setNotifications();
                    }
                    break;
                }
                case HOMEPAGE:
                default:
                    setBottomNavigationMenuItemChecked(HOME_BNV);
                    break;
            }
        }
    }

    public void openChat(long chatId, String text) {
        logDebug("Chat ID: " + chatId);

        if (chatId != -1) {
            MegaChatRoom chat = megaChatApi.getChatRoom(chatId);
            if (chat != null) {
                logDebug("Open chat with id: " + chatId);
                Intent intentToChat = new Intent(this, ChatActivity.class);
                intentToChat.setAction(ACTION_CHAT_SHOW_MESSAGES);
                intentToChat.putExtra(CHAT_ID, chatId);
                intentToChat.putExtra(SHOW_SNACKBAR, text);
                this.startActivity(intentToChat);
            } else {
                logError("Error, chat is NULL");
            }
        } else {
            logError("Error, chat id is -1");
        }
    }

    public void setProfileAvatar() {
        logDebug("setProfileAvatar");
        Pair<Boolean, Bitmap> circleAvatar = AvatarUtil.getCircleAvatar(this, megaApi.getMyEmail());
        if (circleAvatar.first) {
            nVPictureProfile.setImageBitmap(circleAvatar.second);
        } else {
            File avatarFile = CacheFolderManager.buildAvatarFile(this, megaApi.getMyEmail() + JPG_EXTENSION);
            if(avatarFile != null && avatarFile.exists()) {
                megaApi.getUserAvatar(megaApi.getMyUser(), avatarFile.getAbsolutePath(), this);
            }
        }
    }

    public void setDefaultAvatar() {
        logDebug("setDefaultAvatar");
        nVPictureProfile.setImageBitmap(getDefaultAvatar(getColorAvatar(megaApi.getMyUser()), myAccountInfo.getFullName(), AVATAR_SIZE, true));
    }

    public void setOfflineAvatar(String email, long myHandle, String name) {
        logDebug("setOfflineAvatar");
        if (nVPictureProfile == null) {
            return;
        }

        Pair<Boolean, Bitmap> circleAvatar = AvatarUtil.getCircleAvatar(this, email);
        if (circleAvatar.first) {
            nVPictureProfile.setImageBitmap(circleAvatar.second);
        } else {
            nVPictureProfile.setImageBitmap(
                    getDefaultAvatar(getColorAvatar(myHandle), name, AVATAR_SIZE, true));
        }
    }

    @Override
    protected void onStop() {
        logDebug("onStop");

        mStopped = true;

        super.onStop();
    }

    @Override
    protected void onPause() {
        logDebug("onPause");
        managerActivity = null;
        MegaApplication.getTransfersManagement().setIsOnTransfersSection(false);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        logDebug("onDestroy()");

        dbH.removeSentPendingMessages();

        if (megaApi != null && megaApi.getRootNode() != null) {
            megaApi.removeTransferListener(this);
            megaApi.removeRequestListener(this);
        }

        composite.clear();

        if (alertDialogSMSVerification != null) {
            alertDialogSMSVerification.dismiss();
        }
        isStorageStatusDialogShown = false;

        unregisterReceiver(chatRoomMuteUpdateReceiver);
        unregisterReceiver(contactUpdateReceiver);
        unregisterReceiver(updateMyAccountReceiver);
        unregisterReceiver(networkReceiver);
        unregisterReceiver(receiverUpdateOrder);
        unregisterReceiver(chatArchivedReceiver);
        LiveEventBus.get(EVENT_REFRESH_PHONE_NUMBER, Boolean.class)
                .removeObserver(refreshAddPhoneNumberButtonObserver);
        unregisterReceiver(receiverCUAttrChanged);
        unregisterReceiver(transferOverQuotaUpdateReceiver);
        unregisterReceiver(transferFinishReceiver);
        LiveEventBus.get(EVENT_REFRESH, Boolean.class).removeObserver(refreshObserver);
        unregisterReceiver(cuUpdateReceiver);
        LiveEventBus.get(EVENT_FINISH_ACTIVITY, Boolean.class).removeObserver(finishObserver);
        LiveEventBus.get(EVENT_MY_BACKUPS_FOLDER_CHANGED, Boolean.class).removeObserver(fileBackupChangedObserver);

        destroyPayments();

        cancelSearch();
        if (reconnectDialog != null) {
            reconnectDialog.cancel();
        }

        if (confirmationTransfersDialog != null) {
            confirmationTransfersDialog.dismiss();
        }

        if (newTextFileDialog != null) {
            newTextFileDialog.dismiss();
        }

        dismissAlertDialogIfExists(processFileDialog);
        dismissAlertDialogIfExists(openLinkDialog);
        dismissAlertDialogIfExists(newFolderDialog);

        nodeSaver.destroy();

        super.onDestroy();
    }

    private void cancelSearch() {
        if (getSearchFragment() != null) {
            searchFragment.cancelSearch();
        }
    }

    public void skipToMediaDiscoveryFragment(Fragment f) {
        mediaDiscoveryFragment = (MediaDiscoveryFragment) f;
        replaceFragment(f, FragmentTag.MEDIA_DISCOVERY.getTag());
        isInMDMode = true;
    }

    public void skipToAlbumContentFragment(Fragment f) {
        albumContentFragment = (AlbumContentFragment) f;
        replaceFragment(f, FragmentTag.ALBUM_CONTENT.getTag());
        isInAlbumContent = true;
        firstNavigationLevel = false;

        cuLayout.setVisibility(View.GONE);
        showHideBottomNavigationView(true);
    }

    public void changeMDMode(boolean targetMDMode) {
        isInMDMode = targetMDMode;
    }

    void replaceFragment(Fragment f, String fTag) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, f, fTag);
        ft.commitNowAllowingStateLoss();
        // refresh manually
        if (f instanceof RecentChatsFragment) {
            RecentChatsFragment rcf = (RecentChatsFragment) f;
            if (rcf.isResumed()) {
                rcf.refreshMegaContactsList();
                rcf.setCustomisedActionBar();
            }
        }
    }

    private void refreshFragment(String fTag) {
        Fragment f = getSupportFragmentManager().findFragmentByTag(fTag);
        if (f != null) {
            logDebug("Fragment " + fTag + " refreshing");
            getSupportFragmentManager().beginTransaction().detach(f).commitNowAllowingStateLoss();
            getSupportFragmentManager().beginTransaction().attach(f).commitNowAllowingStateLoss();
        } else {
            logWarning("Fragment == NULL. Not refresh");
        }
    }

    public boolean isFirstLogin() {
        return firstLogin;
    }

    public void selectDrawerItemCloudDrive() {
        logDebug("selectDrawerItemCloudDrive");
        abL.setVisibility(View.VISIBLE);

        tabLayoutShares.setVisibility(View.GONE);
        viewPagerShares.setVisibility(View.GONE);
        tabLayoutTransfers.setVisibility(View.GONE);
        viewPagerTransfers.setVisibility(View.GONE);

        fragmentContainer.setVisibility(View.VISIBLE);
        fileBrowserFragment = (FileBrowserFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.CLOUD_DRIVE.getTag());
        if (fileBrowserFragment == null) {
            fileBrowserFragment = FileBrowserFragment.newInstance();
        }

        replaceFragment(fileBrowserFragment, FragmentTag.CLOUD_DRIVE.getTag());
    }

    private void showGlobalAlertDialogsIfNeeded() {
        if (showStorageAlertWithDelay) {
            showStorageAlertWithDelay = false;
            checkStorageStatus(storageStateFromBroadcast != MegaApiJava.STORAGE_STATE_UNKNOWN ?
                    storageStateFromBroadcast : app.getStorageState(), false);
        }

        if (!firstTimeAfterInstallation) {
            logDebug("Its NOT first time");
            int dbContactsSize = dbH.getContactsSize();
            int sdkContactsSize = megaApi.getContacts().size();
            if (dbContactsSize != sdkContactsSize) {
                logDebug("Contacts TABLE != CONTACTS SDK " + dbContactsSize + " vs " + sdkContactsSize);
                dbH.clearContacts();
                FillDBContactsTask fillDBContactsTask = new FillDBContactsTask(this);
                fillDBContactsTask.execute();
            }
        } else {
            logDebug("Its first time");

            //Fill the contacts DB
            FillDBContactsTask fillDBContactsTask = new FillDBContactsTask(this);
            fillDBContactsTask.execute();
            firstTimeAfterInstallation = false;
            dbH.setFirstTime(false);
        }

        checkBeforeShowSMSVerificationDialog();

        cookieDialogHandler.showDialogIfNeeded(this);
    }

    /**
     * Observe LiveData for PSA, and show PSA view when get it.
     */
    private void observePsa() {
        psaViewHolder = new PsaViewHolder(findViewById(R.id.psa_layout), PsaManager.INSTANCE);

        LiveEventBus.get(EVENT_PSA, Psa.class).observe(this, psa -> {
            if (psa.getUrl() == null) {
                showPsa(psa);
            }
        });
    }

    /**
     * Show PSA view for old PSA type.
     *
     * @param psa the PSA to show
     */
    private void showPsa(Psa psa) {
        if (psa == null || drawerItem != DrawerItem.HOMEPAGE
                || mHomepageScreen != HomepageScreen.HOMEPAGE) {
            updateHomepageFabPosition();
            return;
        }

        if (getLifecycle().getCurrentState() == Lifecycle.State.RESUMED
                && getProLayout.getVisibility() == View.GONE
                && TextUtils.isEmpty(psa.getUrl())) {
            psaViewHolder.bind(psa);
            handler.post(this::updateHomepageFabPosition);
        }
    }

    public void checkBeforeShowSMSVerificationDialog() {
        //This account hasn't verified a phone number and first login.

        if (myAccountInfo.isBusinessAlertShown()) {
            //The business alerts has priority over SMS verification
            return;
        }

        if (canVoluntaryVerifyPhoneNumber() && (smsDialogTimeChecker.shouldShow() || isSMSDialogShowing) && !newCreationAccount) {
            showSMSVerificationDialog();
        }
    }

    public void setToolbarTitle(String title) {
        aB.setTitle(title);
    }

    public void setToolbarTitle() {
        logDebug("setToolbarTitle");
        if (drawerItem == null) {
            return;
        }

        switch (drawerItem) {
            case CLOUD_DRIVE: {
                aB.setSubtitle(null);
                logDebug("Cloud Drive SECTION");
                MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
                if (parentNode != null) {
                    if (megaApi.getRootNode() != null) {
                        if (parentNode.getHandle() == megaApi.getRootNode().getHandle() || parentHandleBrowser == -1) {
                            aB.setTitle(getString(R.string.section_cloud_drive).toUpperCase());
                            firstNavigationLevel = true;
                        } else {
                            aB.setTitle(parentNode.getName());
                            firstNavigationLevel = false;
                        }
                    } else {
                        parentHandleBrowser = -1;
                    }
                } else {
                    if (megaApi.getRootNode() != null) {
                        parentHandleBrowser = megaApi.getRootNode().getHandle();
                        aB.setTitle(getString(R.string.title_mega_info_empty_screen).toUpperCase());
                        firstNavigationLevel = true;
                    } else {
                        parentHandleBrowser = -1;
                        firstNavigationLevel = true;
                    }
                }
                break;
            }
            case RUBBISH_BIN: {
                aB.setSubtitle(null);
                MegaNode node = megaApi.getNodeByHandle(parentHandleRubbish);
                MegaNode rubbishNode = megaApi.getRubbishNode();
                if (rubbishNode == null) {
                    parentHandleRubbish = INVALID_HANDLE;
                    firstNavigationLevel = true;
                } else if (parentHandleRubbish == INVALID_HANDLE || node == null || node.getHandle() == rubbishNode.getHandle()) {
                    aB.setTitle(StringResourcesUtils.getString(R.string.section_rubbish_bin).toUpperCase());
                    firstNavigationLevel = true;
                } else {
                    aB.setTitle(node.getName());
                    firstNavigationLevel = false;
                }
                break;
            }
            case SHARED_ITEMS: {
                logDebug("Shared Items SECTION");
                aB.setSubtitle(null);
                int indexShares = getTabItemShares();
                if (indexShares == ERROR_TAB) break;
                switch (indexShares) {
                    case INCOMING_TAB: {
                        if (isIncomingAdded()) {
                            if (parentHandleIncoming != -1) {
                                MegaNode node = megaApi.getNodeByHandle(parentHandleIncoming);
                                if (node == null) {
                                    aB.setTitle(getResources().getString(R.string.title_shared_items).toUpperCase());
                                } else {
                                    aB.setTitle(node.getName());
                                }

                                firstNavigationLevel = false;
                            } else {
                                aB.setTitle(getResources().getString(R.string.title_shared_items).toUpperCase());
                                firstNavigationLevel = true;
                            }
                        } else {
                            logDebug("selectDrawerItemSharedItems: inSFLol == null");
                        }
                        break;
                    }
                    case OUTGOING_TAB: {
                        logDebug("setToolbarTitle: OUTGOING TAB");
                        if (isOutgoingAdded()) {
                            if (parentHandleOutgoing != -1) {
                                MegaNode node = megaApi.getNodeByHandle(parentHandleOutgoing);
                                aB.setTitle(node.getName());
                                firstNavigationLevel = false;
                            } else {
                                aB.setTitle(getResources().getString(R.string.title_shared_items).toUpperCase());
                                firstNavigationLevel = true;
                            }
                        }
                        break;
                    }
                    case LINKS_TAB:
                        if (isLinksAdded()) {
                            if (parentHandleLinks == INVALID_HANDLE) {
                                aB.setTitle(getResources().getString(R.string.title_shared_items).toUpperCase());
                                firstNavigationLevel = true;
                            } else {
                                MegaNode node = megaApi.getNodeByHandle(parentHandleLinks);
                                aB.setTitle(node.getName());
                                firstNavigationLevel = false;
                            }
                        }
                        break;
                    default: {
                        aB.setTitle(getResources().getString(R.string.title_shared_items).toUpperCase());
                        firstNavigationLevel = true;
                        break;
                    }
                }
                break;
            }
            case INBOX: {
                aB.setSubtitle(null);
                if (parentHandleInbox == megaApi.getInboxNode().getHandle() || parentHandleInbox == -1) {
                    aB.setTitle(getResources().getString(R.string.section_inbox).toUpperCase());
                    firstNavigationLevel = true;
                } else {
                    MegaNode node = megaApi.getNodeByHandle(parentHandleInbox);
                    aB.setTitle(node.getName());
                    firstNavigationLevel = false;
                }
                break;
            }
            case NOTIFICATIONS: {
                aB.setSubtitle(null);
                aB.setTitle(getString(R.string.title_properties_chat_contact_notifications).toUpperCase());
                firstNavigationLevel = true;
                break;
            }
            case CHAT: {
                abL.setVisibility(View.VISIBLE);
                aB.setTitle(getString(R.string.section_chat).toUpperCase());

                firstNavigationLevel = true;
                break;
            }
            case SEARCH: {
                aB.setSubtitle(null);
                if (parentHandleSearch == -1) {
                    firstNavigationLevel = true;
                    if (searchQuery != null) {
                        textSubmitted = true;
                        if (!searchQuery.isEmpty()) {
                            aB.setTitle(getString(R.string.action_search).toUpperCase() + ": " + searchQuery);
                        } else {
                            aB.setTitle(getString(R.string.action_search).toUpperCase() + ": " + "");
                        }
                    } else {
                        aB.setTitle(getString(R.string.action_search).toUpperCase() + ": " + "");
                    }

                } else {
                    MegaNode parentNode = megaApi.getNodeByHandle(parentHandleSearch);
                    if (parentNode != null) {
                        aB.setTitle(parentNode.getName());
                        firstNavigationLevel = false;
                    }
                }
                break;
            }
            case TRANSFERS: {
                aB.setSubtitle(null);
                aB.setTitle(getString(R.string.section_transfers).toUpperCase());
                setFirstNavigationLevel(true);
                break;
            }
            case PHOTOS: {
                aB.setSubtitle(null);
                if (getPhotosFragment() != null && photosFragment.isEnablePhotosFragmentShown()) {
                    setFirstNavigationLevel(false);
                    aB.setTitle(getString(R.string.settings_camera_upload_on).toUpperCase());
                } else {
                    if (isInAlbumContent) {
                        aB.setTitle(getString(R.string.title_favourites_album));
                    } else {
                        setFirstNavigationLevel(true);
                        aB.setTitle(getString(R.string.sortby_type_photo_first).toUpperCase());
                    }
                }
                break;
            }
            case HOMEPAGE: {
                setFirstNavigationLevel(false);
                int titleId = -1;

                switch (mHomepageScreen) {
                    case IMAGES:
                        titleId = R.string.section_images;
                        break;
                    case DOCUMENTS:
                        titleId = R.string.section_documents;
                        break;
                    case AUDIO:
                        titleId = R.string.upload_to_audio;
                        break;
                    case VIDEO:
                        titleId = R.string.sortby_type_video_first;
                        break;
                }

                if (titleId != -1) {
                    aB.setTitle(getString(titleId).toUpperCase(Locale.getDefault()));
                }
            }
            default: {
                logDebug("Default GONE");

                break;
            }
        }

        updateNavigationToolbarIcon();
    }

    public void setToolbarTitleFromFullscreenOfflineFragment(String title,
                                                             boolean firstNavigationLevel, boolean showSearch) {
        aB.setSubtitle(null);
        aB.setTitle(title);
        this.firstNavigationLevel = firstNavigationLevel;
        updateNavigationToolbarIcon();
        textSubmitted = true;
        if (searchMenuItem != null) {
            searchMenuItem.setVisible(showSearch);
        }
    }

    public void updateNavigationToolbarIcon() {
        int totalHistoric = megaApi.getNumUnreadUserAlerts();
        int totalIpc = 0;
        ArrayList<MegaContactRequest> requests = megaApi.getIncomingContactRequests();
        if (requests != null) {
            totalIpc = requests.size();
        }

        int totalNotifications = totalHistoric + totalIpc;

        if (totalNotifications == 0) {
            if (isFirstNavigationLevel()) {
                if (drawerItem == DrawerItem.SEARCH || drawerItem == DrawerItem.INBOX || drawerItem == DrawerItem.NOTIFICATIONS
                        || drawerItem == DrawerItem.RUBBISH_BIN || drawerItem == DrawerItem.TRANSFERS) {
                    aB.setHomeAsUpIndicator(tintIcon(this, R.drawable.ic_arrow_back_white));
                } else {
                    aB.setHomeAsUpIndicator(tintIcon(this, R.drawable.ic_menu_white));
                }
            } else {
                aB.setHomeAsUpIndicator(tintIcon(this, R.drawable.ic_arrow_back_white));
            }
        } else {
            if (isFirstNavigationLevel()) {
                if (drawerItem == DrawerItem.SEARCH || drawerItem == DrawerItem.INBOX || drawerItem == DrawerItem.NOTIFICATIONS
                        || drawerItem == DrawerItem.RUBBISH_BIN || drawerItem == DrawerItem.TRANSFERS) {
                    badgeDrawable.setProgress(1.0f);
                } else {
                    badgeDrawable.setProgress(0.0f);
                }
            } else {
                badgeDrawable.setProgress(1.0f);
            }

            if (totalNotifications > 9) {
                badgeDrawable.setText("9+");
            } else {
                badgeDrawable.setText(totalNotifications + "");
            }

            aB.setHomeAsUpIndicator(badgeDrawable);
        }

        if (drawerItem == DrawerItem.CLOUD_DRIVE && isInMDMode) {
            aB.setHomeAsUpIndicator(tintIcon(this, R.drawable.ic_close_white));
        }

        if (drawerItem == DrawerItem.PHOTOS && isInAlbumContent) {
            aB.setHomeAsUpIndicator(tintIcon(this, R.drawable.ic_arrow_back_white));
        }
    }

    public void showOnlineMode() {
        logDebug("showOnlineMode");

        try {
            if (usedSpaceLayout != null) {

                if (rootNode != null) {
                    Menu bNVMenu = bNV.getMenu();
                    if (bNVMenu != null) {
                        resetNavigationViewMenu(bNVMenu);
                    }
                    clickDrawerItem(drawerItem);

                    supportInvalidateOptionsMenu();
                    updateAccountDetailsVisibleInfo();
                    checkCurrentStorageStatus(false);
                } else {
                    logWarning("showOnlineMode - Root is NULL");
                    if (getApplicationContext() != null) {
                        if (((MegaApplication) getApplication()).getOpenChatId() != -1) {
                            Intent intent = new Intent(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE_DIALOG);
                            sendBroadcast(intent);
                        } else {
                            showConfirmationConnect();
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public void showConfirmationConnect() {
        logDebug("showConfirmationConnect");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        refreshSession();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        logDebug("showConfirmationConnect: BUTTON_NEGATIVE");
                        setToolbarTitle();
                        break;
                }
            }
        };

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        try {
            builder.setMessage(R.string.confirmation_to_reconnect).setPositiveButton(R.string.general_ok, dialogClickListener)
                    .setNegativeButton(R.string.general_cancel, dialogClickListener);
            reconnectDialog = builder.create();
            reconnectDialog.setCanceledOnTouchOutside(false);
            reconnectDialog.show();
        } catch (Exception e) {
        }
    }

    public void showOfflineMode() {
        logDebug("showOfflineMode");

        try {
            if (megaApi == null) {
                logWarning("megaApi is Null in Offline mode");
            }

            if (usedSpaceLayout != null) {
                usedSpaceLayout.setVisibility(View.GONE);
            }
            if (nVEmail != null) {
                nVEmail.setText(megaChatApi.getMyEmail());
            }
            if (nVDisplayName != null) {
                nVDisplayName.setText(megaChatApi.getMyFullname());
            }

            setOfflineAvatar(megaChatApi.getMyEmail(), megaChatApi.getMyUserHandle(),
                    megaChatApi.getMyFullname());

            logDebug("DrawerItem on start offline: " + drawerItem);
            if (drawerItem == null) {
                logWarning("drawerItem == null --> On start OFFLINE MODE");
                drawerItem = getStartDrawerItem(this);

                if (bNV != null) {
                    disableNavigationViewMenu(bNV.getMenu());
                }

                selectDrawerItem(drawerItem);
            } else {
                if (bNV != null) {
                    disableNavigationViewMenu(bNV.getMenu());
                }

                logDebug("Change to OFFLINE MODE");
                clickDrawerItem(drawerItem);
            }

            supportInvalidateOptionsMenu();
        } catch (Exception e) {
        }
    }

    public void clickDrawerItem(DrawerItem item) {
        logDebug("Item: " + item);
        Menu bNVMenu = bNV.getMenu();

        if (item == null) {
            drawerMenuItem = bNVMenu.findItem(R.id.bottom_navigation_item_cloud_drive);
            onNavigationItemSelected(drawerMenuItem);
            return;
        }

        drawerLayout.closeDrawer(Gravity.LEFT);

        switch (item) {
            case CLOUD_DRIVE: {
                setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV);
                break;
            }
            case HOMEPAGE: {
                setBottomNavigationMenuItemChecked(HOME_BNV);
                break;
            }
            case PHOTOS: {
                setBottomNavigationMenuItemChecked(PHOTOS_BNV);
                break;
            }
            case SHARED_ITEMS: {
                setBottomNavigationMenuItemChecked(SHARED_ITEMS_BNV);
                break;
            }
            case CHAT: {
                setBottomNavigationMenuItemChecked(CHAT_BNV);
                break;
            }
            case SEARCH:
            case TRANSFERS:
            case NOTIFICATIONS:
            case INBOX: {
                setBottomNavigationMenuItemChecked(NO_BNV);
                break;
            }
        }
    }


    public void selectDrawerItemSharedItems() {
        logDebug("selectDrawerItemSharedItems");
        abL.setVisibility(View.VISIBLE);

        try {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.cancel(NOTIFICATION_PUSH_CLOUD_DRIVE);
        } catch (Exception e) {
            logError("Exception NotificationManager - remove contact notification", e);
        }

        if (sharesPageAdapter == null) {
            logWarning("sharesPageAdapter is NULL");
            sharesPageAdapter = new SharesPageAdapter(getSupportFragmentManager(), this);
            viewPagerShares.setAdapter(sharesPageAdapter);
            tabLayoutShares.setupWithViewPager(viewPagerShares);
            setSharesTabIcons(indexShares);

            //Force on CreateView, addTab do not execute onCreateView
            if (indexShares != ERROR_TAB) {
                logDebug("The index of the TAB Shares is: " + indexShares);
                if (viewPagerShares != null) {
                    switch (indexShares) {
                        case INCOMING_TAB:
                        case OUTGOING_TAB:
                        case LINKS_TAB:
                            viewPagerShares.setCurrentItem(indexShares);
                            break;
                    }
                }
                indexShares = ERROR_TAB;
            } else {
                //No bundle, no change of orientation
                logDebug("indexShares is NOT -1");
            }

        }

        setToolbarTitle();

        drawerLayout.closeDrawer(Gravity.LEFT);
    }

    private void setSharesTabIcons(int tabSelected) {
        if (tabLayoutShares == null
                || tabLayoutShares.getTabAt(INCOMING_TAB) == null
                || tabLayoutShares.getTabAt(OUTGOING_TAB) == null
                || tabLayoutShares.getTabAt(LINKS_TAB) == null) {
            return;
        }

        // The TabLayout style sets the default icon tint
        switch (tabSelected) {
            case OUTGOING_TAB:
                tabLayoutShares.getTabAt(INCOMING_TAB).setIcon(R.drawable.ic_incoming_shares);
                tabLayoutShares.getTabAt(OUTGOING_TAB).setIcon(mutateIconSecondary(this, R.drawable.ic_outgoing_shares, R.color.red_600_red_300));
                tabLayoutShares.getTabAt(LINKS_TAB).setIcon(R.drawable.link_ic);
                break;
            case LINKS_TAB:
                tabLayoutShares.getTabAt(INCOMING_TAB).setIcon(R.drawable.ic_incoming_shares);
                tabLayoutShares.getTabAt(OUTGOING_TAB).setIcon(R.drawable.ic_outgoing_shares);
                tabLayoutShares.getTabAt(LINKS_TAB).setIcon(mutateIconSecondary(this, R.drawable.link_ic, R.color.red_600_red_300));
                break;
            default:
                tabLayoutShares.getTabAt(INCOMING_TAB).setIcon(mutateIconSecondary(this, R.drawable.ic_incoming_shares, R.color.red_600_red_300));
                tabLayoutShares.getTabAt(OUTGOING_TAB).setIcon(R.drawable.ic_outgoing_shares);
                tabLayoutShares.getTabAt(LINKS_TAB).setIcon(R.drawable.link_ic);
        }
    }

    public void selectDrawerItemNotifications() {
        logDebug("selectDrawerItemNotifications");

        abL.setVisibility(View.VISIBLE);

        drawerItem = DrawerItem.NOTIFICATIONS;

        setBottomNavigationMenuItemChecked(NO_BNV);

        notificationsFragment = (NotificationsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.NOTIFICATIONS.getTag());
        if (notificationsFragment == null) {
            logWarning("New NotificationsFragment");
            notificationsFragment = NotificationsFragment.newInstance();
        } else {
            refreshFragment(FragmentTag.NOTIFICATIONS.getTag());
        }
        replaceFragment(notificationsFragment, FragmentTag.NOTIFICATIONS.getTag());

        setToolbarTitle();

        showFabButton();
    }

    public void selectDrawerItemTransfers() {
        logDebug("selectDrawerItemTransfers");

        abL.setVisibility(View.VISIBLE);
        transfersWidget.hide();

        drawerItem = DrawerItem.TRANSFERS;

        setBottomNavigationMenuItemChecked(NO_BNV);

        if (mTabsAdapterTransfers == null) {
            mTabsAdapterTransfers = new TransfersPageAdapter(getSupportFragmentManager(), this);
            viewPagerTransfers.setAdapter(mTabsAdapterTransfers);
            tabLayoutTransfers.setupWithViewPager(viewPagerTransfers);
        }

        boolean showCompleted = !dbH.getCompletedTransfers().isEmpty() && transfersWidget.getPendingTransfers() <= 0;

        TransfersManagement transfersManagement = MegaApplication.getTransfersManagement();
        indexTransfers = transfersManagement.thereAreFailedTransfers() || showCompleted ? COMPLETED_TAB : PENDING_TAB;

        if (viewPagerTransfers != null) {
            switch (indexTransfers) {
                case COMPLETED_TAB:
                    refreshFragment(FragmentTag.COMPLETED_TRANSFERS.getTag());
                    viewPagerTransfers.setCurrentItem(COMPLETED_TAB);
                    break;

                default:
                    refreshFragment(FragmentTag.TRANSFERS.getTag());
                    viewPagerTransfers.setCurrentItem(PENDING_TAB);

                    if (transfersManagement.shouldShowNetWorkWarning()) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), MEGACHAT_INVALID_HANDLE);
                    }

                    break;
            }

            if (mTabsAdapterTransfers != null) {
                mTabsAdapterTransfers.notifyDataSetChanged();
            }

            indexTransfers = viewPagerTransfers.getCurrentItem();
        }

        setToolbarTitle();
        showFabButton();
        drawerLayout.closeDrawer(Gravity.LEFT);
    }

    public void selectDrawerItemChat() {
        ((MegaApplication) getApplication()).setRecentChatVisible(true);
        setToolbarTitle();

        recentChatsFragment = (RecentChatsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
        if (recentChatsFragment == null) {
            recentChatsFragment = RecentChatsFragment.newInstance();
        } else {
            refreshFragment(FragmentTag.RECENT_CHAT.getTag());
        }

        replaceFragment(recentChatsFragment, FragmentTag.RECENT_CHAT.getTag());

        drawerLayout.closeDrawer(Gravity.LEFT);
    }

    public void setBottomNavigationMenuItemChecked(int item) {
        if (bNV != null) {
            if (item == NO_BNV) {
                showHideBottomNavigationView(true);
            } else if (bNV.getMenu().getItem(item) != null) {
                if (!bNV.getMenu().getItem(item).isChecked()) {
                    bNV.getMenu().getItem(item).setChecked(true);
                }
            }
        }

        boolean isCameraUploadItem = item == PHOTOS_BNV;
        updateMiniAudioPlayerVisibility(!isCameraUploadItem);
    }

    private void setTabsVisibility() {
        tabLayoutShares.setVisibility(View.GONE);
        viewPagerShares.setVisibility(View.GONE);
        tabLayoutTransfers.setVisibility(View.GONE);
        viewPagerTransfers.setVisibility(View.GONE);
        mShowAnyTabLayout = false;

        fragmentContainer.setVisibility(View.GONE);
        mNavHostView.setVisibility(View.GONE);

        updatePsaViewVisibility();

        if (turnOnNotifications) {
            fragmentContainer.setVisibility(View.VISIBLE);
            drawerLayout.closeDrawer(Gravity.LEFT);
            return;
        }

        switch (drawerItem) {
            case SHARED_ITEMS: {
                int tabItemShares = getTabItemShares();

                if ((tabItemShares == INCOMING_TAB && parentHandleIncoming != INVALID_HANDLE)
                        || (tabItemShares == OUTGOING_TAB && parentHandleOutgoing != INVALID_HANDLE)
                        || (tabItemShares == LINKS_TAB && parentHandleLinks != INVALID_HANDLE)) {
                    tabLayoutShares.setVisibility(View.GONE);
                    viewPagerShares.disableSwipe(true);
                } else {
                    tabLayoutShares.setVisibility(View.VISIBLE);
                    viewPagerShares.disableSwipe(false);
                }

                viewPagerShares.setVisibility(View.VISIBLE);
                mShowAnyTabLayout = true;
                break;
            }
            case TRANSFERS: {
                tabLayoutTransfers.setVisibility(View.VISIBLE);
                viewPagerTransfers.setVisibility(View.VISIBLE);
                mShowAnyTabLayout = true;
                break;
            }
            case HOMEPAGE:
                mNavHostView.setVisibility(View.VISIBLE);
                break;
            default: {
                fragmentContainer.setVisibility(View.VISIBLE);
                break;
            }
        }

        LiveEventBus.get(EVENT_HOMEPAGE_VISIBILITY, Boolean.class)
                .post(drawerItem == DrawerItem.HOMEPAGE);

        drawerLayout.closeDrawer(Gravity.LEFT);
    }

    /**
     * Hides or shows tabs of a section depending on the navigation level
     * and if select mode is enabled or not.
     *
     * @param hide       If true, hides the tabs, else shows them.
     * @param currentTab The current tab where the action happens.
     */
    public void hideTabs(boolean hide, int currentTab) {
        int visibility = hide ? View.GONE : View.VISIBLE;

        switch (drawerItem) {
            case SHARED_ITEMS:
                switch (currentTab) {
                    case INCOMING_TAB:
                        if (!isIncomingAdded() || (!hide && parentHandleIncoming != INVALID_HANDLE)) {
                            return;
                        }

                        break;

                    case OUTGOING_TAB:
                        if (!isOutgoingAdded() || (!hide && parentHandleOutgoing != INVALID_HANDLE)) {
                            return;
                        }

                        break;

                    case LINKS_TAB:
                        if (!isLinksAdded() || (!hide && parentHandleLinks != INVALID_HANDLE)) {
                            return;
                        }

                        break;
                }

                tabLayoutShares.setVisibility(visibility);
                viewPagerShares.disableSwipe(hide);
                break;

            case TRANSFERS:
                if (currentTab == PENDING_TAB && !isTransfersInProgressAdded()) {
                    return;
                }

                tabLayoutTransfers.setVisibility(visibility);
                viewPagerTransfers.disableSwipe(hide);
                break;
        }
    }

    private void removeFragment(Fragment fragment) {
        if (fragment != null && fragment.isAdded()) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(fragment);
            ft.commitAllowingStateLoss();
        }
    }

    /**
     * Set up a listener for navigating to a new destination (screen)
     * This only for Homepage for the time being since it is the only module to
     * which Jetpack Navigation applies.
     * It updates the status variable such as mHomepageScreen, as well as updating
     * BNV, Toolbar title, etc.
     */
    private void setupNavDestListener() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        mNavController = navHostFragment.getNavController();

        mNavController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();
            mHomepageSearchable = null;

            if (destinationId == R.id.homepageFragment) {
                mHomepageScreen = HomepageScreen.HOMEPAGE;
                updatePsaViewVisibility();
                // Showing the bottom navigation view immediately because the initial dimension
                // of Homepage bottom sheet is calculated based on it
                showBNVImmediate();
                if (bottomNavigationCurrentItem == HOME_BNV) {
                    abL.setVisibility(View.GONE);
                }

                if (transfersWidget != null) {
                    transfersWidget.update();
                }

                setDrawerLockMode(false);
                return;
            } else if (destinationId == R.id.photosFragment) {
                mHomepageScreen = HomepageScreen.IMAGES;
            } else if (destinationId == R.id.documentsFragment) {
                mHomepageScreen = HomepageScreen.DOCUMENTS;
            } else if (destinationId == R.id.audioFragment) {
                mHomepageScreen = HomepageScreen.AUDIO;
            } else if (destinationId == R.id.videoFragment) {
                mHomepageScreen = HomepageScreen.VIDEO;
            } else if (destinationId == R.id.fullscreen_offline) {
                mHomepageScreen = HomepageScreen.FULLSCREEN_OFFLINE;
            } else if (destinationId == R.id.offline_file_info) {
                mHomepageScreen = HomepageScreen.OFFLINE_FILE_INFO;
                updatePsaViewVisibility();
                abL.setVisibility(View.GONE);
                showHideBottomNavigationView(true);
                return;
            } else if (destinationId == R.id.recentBucketFragment) {
                mHomepageScreen = HomepageScreen.RECENT_BUCKET;
            }

            if (transfersWidget != null) {
                transfersWidget.update();
            }
            updatePsaViewVisibility();
            abL.setVisibility(View.VISIBLE);
            showHideBottomNavigationView(true);
            supportInvalidateOptionsMenu();
            setToolbarTitle();
            setDrawerLockMode(true);
        });
    }

    /**
     * Hides all views only related to CU section and sets the CU default view.
     */
    private void resetCUFragment() {
        cuLayout.setVisibility(View.GONE);
        cuViewTypes.setVisibility(View.GONE);

        if (getPhotosFragment() != null) {
            photosFragment.setDefaultView();
            showBottomView();
        }
    }

    @SuppressLint("NewApi")
    public void selectDrawerItem(DrawerItem item) {
        if (item == null) {
            logWarning("The selected DrawerItem is NULL. Using latest or default value.");
            item = drawerItem != null ? drawerItem : DrawerItem.CLOUD_DRIVE;
        }

        logDebug("Selected DrawerItem: " + item.name());

        // Homepage may hide the Appbar before
        abL.setVisibility(View.VISIBLE);

        drawerItem = item;
        MegaApplication.setRecentChatVisible(false);
        resetActionBar(aB);
        transfersWidget.update();
        setCallWidget();

        if (item != DrawerItem.CHAT) {
            //remove recent chat fragment as its life cycle get triggered unexpectedly, e.g. rotate device while not on recent chat page
            removeFragment(getChatsFragment());
        }

        if (item != DrawerItem.PHOTOS) {
            resetCUFragment();
        }

        if (item != DrawerItem.TRANSFERS && isTransfersInProgressAdded()) {
            transfersFragment.checkSelectModeAfterChangeTabOrDrawerItem();
        }

        MegaApplication.getTransfersManagement().setIsOnTransfersSection(item == DrawerItem.TRANSFERS);

        switch (item) {
            case CLOUD_DRIVE: {
                if (isInMDPage()) {
                    mediaDiscoveryFragment = (MediaDiscoveryFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.MEDIA_DISCOVERY.getTag());

                    if (mediaDiscoveryFragment == null) {
                        selectDrawerItemCloudDrive();
                        mediaDiscoveryFragment = fileBrowserFragment.showMediaDiscovery(Unit.INSTANCE);
                    } else {
                        refreshFragment(FragmentTag.MEDIA_DISCOVERY.getTag());
                    }

                    replaceFragment(mediaDiscoveryFragment, FragmentTag.MEDIA_DISCOVERY.getTag());
                } else {
                    selectDrawerItemCloudDrive();
                }

                if (openFolderRefresh) {
                    onNodesCloudDriveUpdate();
                    openFolderRefresh = false;
                }
                supportInvalidateOptionsMenu();
                setToolbarTitle();
                showFabButton();
                showHideBottomNavigationView(false);
                if (!comesFromNotifications) {
                    bottomNavigationCurrentItem = CLOUD_DRIVE_BNV;
                }
                setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV);
                if (getIntent() != null && getIntent().getBooleanExtra(INTENT_EXTRA_KEY_LOCATION_FILE_INFO, false)) {
                    fileBrowserFragment.refreshNodes();
                }
                logDebug("END for Cloud Drive");
                break;
            }
            case RUBBISH_BIN: {
                showHideBottomNavigationView(true);
                abL.setVisibility(View.VISIBLE);
                rubbishBinFragment = (RubbishBinFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
                if (rubbishBinFragment == null) {
                    rubbishBinFragment = RubbishBinFragment.newInstance();
                }

                setBottomNavigationMenuItemChecked(NO_BNV);

                replaceFragment(rubbishBinFragment, FragmentTag.RUBBISH_BIN.getTag());

                if (openFolderRefresh) {
                    onNodesCloudDriveUpdate();
                    openFolderRefresh = false;
                }
                supportInvalidateOptionsMenu();
                setToolbarTitle();
                showFabButton();
                break;
            }
            case HOMEPAGE: {
                // Don't use fabButton.hide() here.
                fabButton.setVisibility(View.GONE);
                if (mHomepageScreen == HomepageScreen.HOMEPAGE) {
                    showBNVImmediate();
                    abL.setVisibility(View.GONE);
                    showHideBottomNavigationView(false);
                } else {
                    // For example, back from Rubbish Bin to Photos
                    setToolbarTitle();
                    invalidateOptionsMenu();
                    showHideBottomNavigationView(true);
                }

                setBottomNavigationMenuItemChecked(HOME_BNV);

                if (!comesFromNotifications) {
                    bottomNavigationCurrentItem = HOME_BNV;
                }

                showGlobalAlertDialogsIfNeeded();

                if (mHomepageScreen == HomepageScreen.HOMEPAGE) {
                    changeAppBarElevation(false);
                }
                break;
            }
            case PHOTOS: {
                if (isInAlbumContent) {
                    skipToAlbumContentFragment(AlbumContentFragment.getInstance());
                } else {
                    abL.setVisibility(View.VISIBLE);
                    if (getPhotosFragment() == null) {
                        photosFragment = new PhotosFragment();
                    } else {
                        refreshFragment(FragmentTag.PHOTOS.getTag());
                    }

                    replaceFragment(photosFragment, FragmentTag.PHOTOS.getTag());
                    setToolbarTitle();
                    supportInvalidateOptionsMenu();
                    showFabButton();
                    showHideBottomNavigationView(false);
                    refreshCUNodes();
                    if (!comesFromNotifications) {
                        bottomNavigationCurrentItem = PHOTOS_BNV;
                    }
                    setBottomNavigationMenuItemChecked(PHOTOS_BNV);
                }
                break;
            }
            case INBOX: {
                showHideBottomNavigationView(true);
                abL.setVisibility(View.VISIBLE);
                inboxFragment = (InboxFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.INBOX.getTag());
                if (inboxFragment == null) {
                    inboxFragment = InboxFragment.newInstance();
                }

                replaceFragment(inboxFragment, FragmentTag.INBOX.getTag());

                if (openFolderRefresh) {
                    onNodesInboxUpdate();
                    openFolderRefresh = false;
                }
                supportInvalidateOptionsMenu();
                setToolbarTitle();
                showFabButton();
                break;
            }
            case SHARED_ITEMS: {
                selectDrawerItemSharedItems();
                if (openFolderRefresh) {
                    onNodesSharedUpdate();
                    openFolderRefresh = false;
                }
                supportInvalidateOptionsMenu();

                showFabButton();
                showHideBottomNavigationView(false);
                if (!comesFromNotifications) {
                    bottomNavigationCurrentItem = SHARED_ITEMS_BNV;
                }
                setBottomNavigationMenuItemChecked(SHARED_ITEMS_BNV);
                break;
            }
            case NOTIFICATIONS: {
                showHideBottomNavigationView(true);
                selectDrawerItemNotifications();
                supportInvalidateOptionsMenu();
                showFabButton();
                break;
            }
            case SEARCH: {
                showHideBottomNavigationView(true);

                setBottomNavigationMenuItemChecked(NO_BNV);

                drawerItem = DrawerItem.SEARCH;
                if (getSearchFragment() == null) {
                    searchFragment = SearchFragment.newInstance();
                }

                replaceFragment(searchFragment, FragmentTag.SEARCH.getTag());
                showFabButton();

                break;
            }
            case TRANSFERS: {
                showHideBottomNavigationView(true);
                aB.setSubtitle(null);
                selectDrawerItemTransfers();
                supportInvalidateOptionsMenu();
                showFabButton();
                break;
            }
            case CHAT: {
                logDebug("Chat selected");
                if (megaApi != null) {
                    contacts = megaApi.getContacts();
                    for (int i = 0; i < contacts.size(); i++) {
                        if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                            visibleContacts.add(contacts.get(i));
                        }
                    }
                }
                selectDrawerItemChat();
                supportInvalidateOptionsMenu();
                showHideBottomNavigationView(false);
                if (!comesFromNotifications) {
                    bottomNavigationCurrentItem = CHAT_BNV;
                }
                setBottomNavigationMenuItemChecked(CHAT_BNV);
                break;
            }
        }

        setTabsVisibility();
        checkScrollElevation();

        if (megaApi.multiFactorAuthAvailable()) {
            if (newAccount || isEnable2FADialogShown) {
                showEnable2FADialog();
            }
        }
    }

    private void navigateToSettingsActivity(TargetPreference targetPreference) {
        if (nV != null && drawerLayout != null && drawerLayout.isDrawerOpen(nV)) {
            drawerLayout.closeDrawer(Gravity.LEFT);
        }
        Intent settingsIntent = mega.privacy.android.app.presentation.settings.SettingsActivity.Companion.getIntent(this, targetPreference);
        startActivity(settingsIntent);
    }

    public void openFullscreenOfflineFragment(String path) {
        drawerItem = DrawerItem.HOMEPAGE;
        mNavController.navigate(
                HomepageFragmentDirections.Companion.actionHomepageToFullscreenOffline(path, false),
                new NavOptions.Builder().setLaunchSingleTop(true).build());
    }

    public void fullscreenOfflineFragmentOpened(OfflineFragment fragment) {
        fullscreenOfflineFragment = fragment;

        showFabButton();
        setBottomNavigationMenuItemChecked(HOME_BNV);
        abL.setVisibility(View.VISIBLE);
        setToolbarTitle();
        supportInvalidateOptionsMenu();
    }

    public void fullscreenOfflineFragmentClosed(OfflineFragment fragment) {
        if (fragment == fullscreenOfflineFragment) {
            if (bottomItemBeforeOpenFullscreenOffline != INVALID_VALUE && !mStopped) {
                backToDrawerItem(bottomItemBeforeOpenFullscreenOffline);
                bottomItemBeforeOpenFullscreenOffline = INVALID_VALUE;
            }

            setPathNavigationOffline("/");
            fullscreenOfflineFragment = null;
            // workaround for flicker of AppBarLayout: if we go back to homepage from fullscreen
            // offline, and hide AppBarLayout when immediately on go back, we will see the flicker
            // of AppBarLayout, hide AppBarLayout when fullscreen offline is closed is better.
            if (isInMainHomePage()) {
                abL.setVisibility(View.GONE);
            }
        }
    }

    public void pagerOfflineFragmentOpened(OfflineFragment fragment) {
        pagerOfflineFragment = fragment;
    }

    public void pagerOfflineFragmentClosed(OfflineFragment fragment) {
        if (fragment == pagerOfflineFragment) {
            pagerOfflineFragment = null;
        }
    }

    public void pagerRecentsFragmentOpened(RecentsFragment fragment) {
        pagerRecentsFragment = fragment;
    }

    public void pagerRecentsFragmentClosed(RecentsFragment fragment) {
        if (fragment == pagerRecentsFragment) {
            pagerRecentsFragment = null;
        }
    }

    private void showBNVImmediate() {
        updateMiniAudioPlayerVisibility(true);

        bNV.setTranslationY(0);
        bNV.animate().cancel();
        bNV.clearAnimation();
        if (bNV.getVisibility() != View.VISIBLE) {
            bNV.setVisibility(View.VISIBLE);
        }
        bNV.setVisibility(View.VISIBLE);
        final CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(0, 0, 0,
                getResources().getDimensionPixelSize(R.dimen.bottom_navigation_view_height));
        fragmentLayout.setLayoutParams(params);
    }

    /**
     * Update whether we should display the mini audio player. It should only
     * be visible when BNV is visible.
     *
     * @param shouldVisible whether we should display the mini audio player
     * @return is the mini player visible after this update
     */
    private boolean updateMiniAudioPlayerVisibility(boolean shouldVisible) {
        if (miniAudioPlayerController != null) {
            miniAudioPlayerController.setShouldVisible(shouldVisible);

            handler.post(this::updateHomepageFabPosition);

            return miniAudioPlayerController.visible();
        }

        return false;
    }

    /**
     * Update homepage FAB position, considering the visibility of PSA layout and mini audio player.
     */
    private void updateHomepageFabPosition() {
        HomepageFragment fragment = getFragmentByType(HomepageFragment.class);
        if (isInMainHomePage() && fragment != null) {
            fragment.updateFabPosition(psaViewHolder.visible() ? psaViewHolder.psaLayoutHeight() : 0,
                    miniAudioPlayerController.visible() ? miniAudioPlayerController.playerHeight() : 0);
        }
    }

    private boolean isCloudAdded() {
        fileBrowserFragment = (FileBrowserFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.CLOUD_DRIVE.getTag());
        return fileBrowserFragment != null && fileBrowserFragment.isAdded();
    }

    private boolean isIncomingAdded() {
        if (sharesPageAdapter == null) return false;

        incomingSharesFragment = (IncomingSharesFragment) sharesPageAdapter.instantiateItem(viewPagerShares, INCOMING_TAB);

        return incomingSharesFragment != null && incomingSharesFragment.isAdded();
    }

    private boolean isOutgoingAdded() {
        if (sharesPageAdapter == null) return false;

        outgoingSharesFragment = (OutgoingSharesFragment) sharesPageAdapter.instantiateItem(viewPagerShares, OUTGOING_TAB);

        return outgoingSharesFragment != null && outgoingSharesFragment.isAdded();
    }

    private boolean isLinksAdded() {
        if (sharesPageAdapter == null) return false;

        linksFragment = (LinksFragment) sharesPageAdapter.instantiateItem(viewPagerShares, LINKS_TAB);

        return linksFragment != null && linksFragment.isAdded();
    }

    private boolean isTransfersInProgressAdded() {
        if (mTabsAdapterTransfers == null) return false;

        transfersFragment = (TransfersFragment) mTabsAdapterTransfers.instantiateItem(viewPagerTransfers, PENDING_TAB);

        return transfersFragment.isAdded();
    }

    private boolean isTransfersCompletedAdded() {
        if (mTabsAdapterTransfers == null) return false;

        completedTransfersFragment = (CompletedTransfersFragment) mTabsAdapterTransfers.instantiateItem(viewPagerTransfers, COMPLETED_TAB);

        return completedTransfersFragment.isAdded();
    }

    public void checkScrollElevation() {
        if (drawerItem == null) {
            return;
        }

        switch (drawerItem) {
            case CLOUD_DRIVE: {
                if (fileBrowserFragment != null && fileBrowserFragment.isResumed()) {
                    fileBrowserFragment.checkScroll();
                }
                break;
            }
            case HOMEPAGE: {
                if (fullscreenOfflineFragment != null) {
                    fullscreenOfflineFragment.checkScroll();
                }
                break;
            }
            case PHOTOS: {
                if (getPhotosFragment() != null) {
                    photosFragment.checkScroll();
                }
                break;
            }
            case INBOX: {
                inboxFragment = (InboxFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.INBOX.getTag());
                if (inboxFragment != null) {
                    inboxFragment.checkScroll();
                }
                break;
            }
            case SHARED_ITEMS: {
                if (getTabItemShares() == INCOMING_TAB && isIncomingAdded())
                    incomingSharesFragment.checkScroll();
                else if (getTabItemShares() == OUTGOING_TAB && isOutgoingAdded())
                    outgoingSharesFragment.checkScroll();
                else if (getTabItemShares() == LINKS_TAB && isLinksAdded())
                    linksFragment.checkScroll();
                break;
            }
            case SEARCH: {
                if (getSearchFragment() != null) {
                    searchFragment.checkScroll();
                }
                break;
            }
            case CHAT: {
                recentChatsFragment = (RecentChatsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
                if (recentChatsFragment != null) {
                    recentChatsFragment.checkScroll();
                }
                break;
            }
            case RUBBISH_BIN: {
                rubbishBinFragment = (RubbishBinFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
                if (rubbishBinFragment != null) {
                    rubbishBinFragment.checkScroll();
                }
                break;
            }

            case TRANSFERS: {
                if (getTabItemTransfers() == PENDING_TAB && isTransfersInProgressAdded()) {
                    transfersFragment.checkScroll();
                } else if (getTabItemTransfers() == COMPLETED_TAB && isTransfersCompletedAdded()) {
                    completedTransfersFragment.checkScroll();
                }
            }
        }
    }


    void showEnable2FADialog() {
        logDebug("newAccount: " + newAccount);
        newAccount = false;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_enable_2fa_create_account, null);
        builder.setView(v);

        enable2FAButton = (Button) v.findViewById(R.id.enable_2fa_button);
        enable2FAButton.setOnClickListener(this);
        skip2FAButton = (Button) v.findViewById(R.id.skip_enable_2fa_button);
        skip2FAButton.setOnClickListener(this);

        enable2FADialog = builder.create();
        enable2FADialog.setCanceledOnTouchOutside(false);
        try {
            enable2FADialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        isEnable2FADialogShown = true;
    }

    /**
     * Opens the settings section.
     */
    public void moveToSettingsSection() {
        navigateToSettingsActivity(null);
    }

    /**
     * Opens the settings section and scrolls to storage category.
     */
    public void moveToSettingsSectionStorage() {
        navigateToSettingsActivity(TargetPreference.Storage.INSTANCE);
    }

    /**
     * Opens the settings section and scrolls to start screen setting.
     */
    public void moveToSettingsSectionStartScreen() {
        navigateToSettingsActivity(TargetPreference.StartScreen.INSTANCE);
    }

    public void moveToChatSection(long idChat) {
        if (idChat != -1) {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.setAction(ACTION_CHAT_SHOW_MESSAGES);
            intent.putExtra(CHAT_ID, idChat);
            this.startActivity(intent);
        }
        drawerItem = DrawerItem.CHAT;
        selectDrawerItem(drawerItem);
    }

    /**
     * Launches a MyAccountActivity intent without any intent action, data and extra.
     */
    public void showMyAccount() {
        showMyAccount(null, null, null);
    }

    /**
     * Launches a MyAccountActivity intent without any extra.
     *
     * @param action The intent action.
     * @param data   The intent data.
     */
    private void showMyAccount(String action, Uri data) {
        showMyAccount(action, data, null);
    }

    /**
     * Launches a MyAccountActivity intent without any intent action and data.
     *
     * @param extra Pair<String, Integer> The intent extra. First is the extra key, second the value.
     */
    private void showMyAccount(Pair<String, Integer> extra) {
        showMyAccount(null, null, extra);
    }

    /**
     * Launches a MyAccountActivity intent.
     *
     * @param action The intent action.
     * @param data   The intent data.
     * @param extra  Pair<String, Integer> The intent extra. First is the extra key, second the value.
     */
    private void showMyAccount(String action, Uri data, Pair<String, Integer> extra) {
        if (nV != null && drawerLayout != null && drawerLayout.isDrawerOpen(nV)) {
            drawerLayout.closeDrawer(Gravity.LEFT);
        }

        Intent intent = new Intent(this, MyAccountActivity.class)
                .setAction(action)
                .setData(data);

        if (extra != null) {
            intent.putExtra(extra.first, extra.second);
        }

        startActivity(intent);
    }

    private void closeSearchSection() {
        searchQuery = "";
        drawerItem = searchDrawerItem;
        selectDrawerItem(drawerItem);
        searchDrawerItem = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        logDebug("onCreateOptionsMenu");
        // Force update the toolbar title to make the the tile length to be updated
        setToolbarTitle();
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_manager, menu);

        searchMenuItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchMenuItem.getActionView();

        SearchView.SearchAutoComplete searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchAutoComplete.setHint(getString(R.string.hint_action_search));
        View v = searchView.findViewById(androidx.appcompat.R.id.search_plate);
        v.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));

        if (searchView != null) {
            searchView.setIconifiedByDefault(true);
        }

        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                logDebug("onMenuItemActionExpand");
                searchQuery = "";
                searchExpand = true;
                if (drawerItem == DrawerItem.HOMEPAGE) {
                    if (mHomepageScreen == HomepageScreen.FULLSCREEN_OFFLINE) {
                        setFullscreenOfflineFragmentSearchQuery(searchQuery);
                    } else if (mHomepageSearchable != null) {
                        mHomepageSearchable.searchReady();
                    } else {
                        openSearchOnHomepage();
                    }
                } else if (drawerItem != DrawerItem.CHAT) {
                    textsearchQuery = false;
                    firstNavigationLevel = true;
                    parentHandleSearch = -1;
                    levelsSearch = -1;
                    setSearchDrawerItem();
                    selectDrawerItem(drawerItem);
                } else {
                    resetActionBar(aB);
                }
                hideCallMenuItem(chronometerMenuItem, returnCallMenuItem);
                hideCallWidget(ManagerActivity.this, callInProgressChrono, callInProgressLayout);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                logDebug("onMenuItemActionCollapse()");
                searchExpand = false;
                setCallWidget();
                setCallMenuItem(returnCallMenuItem, layoutCallMenuItem, chronometerMenuItem);
                if (drawerItem == DrawerItem.CHAT) {
                    if (getChatsFragment() != null) {
                        recentChatsFragment.closeSearch();
                        recentChatsFragment.setCustomisedActionBar();
                        supportInvalidateOptionsMenu();
                    }
                } else if (drawerItem == DrawerItem.HOMEPAGE) {
                    if (mHomepageScreen == HomepageScreen.FULLSCREEN_OFFLINE) {
                        if (!textSubmitted) {
                            setFullscreenOfflineFragmentSearchQuery(null);
                            textSubmitted = true;
                        }
                        supportInvalidateOptionsMenu();
                    } else if (mHomepageSearchable != null) {
                        mHomepageSearchable.exitSearch();
                        searchQuery = "";
                        supportInvalidateOptionsMenu();
                    }
                } else {
                    cancelSearch();
                    textSubmitted = true;
                    closeSearchSection();
                }
                return true;
            }
        });

        searchView.setMaxWidth(Integer.MAX_VALUE);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (drawerItem == DrawerItem.CHAT) {
                    hideKeyboard(managerActivity, 0);
                } else if (drawerItem == DrawerItem.HOMEPAGE) {
                    if (mHomepageScreen == HomepageScreen.FULLSCREEN_OFFLINE) {
                        searchExpand = false;
                        textSubmitted = true;
                        hideKeyboard(managerActivity, 0);
                        if (fullscreenOfflineFragment != null) {
                            fullscreenOfflineFragment.onSearchQuerySubmitted();
                        }
                        setToolbarTitle();
                        supportInvalidateOptionsMenu();
                    } else {
                        hideKeyboard(ManagerActivity.this);
                    }
                } else {
                    searchExpand = false;
                    searchQuery = "" + query;
                    setToolbarTitle();
                    logDebug("Search query: " + query);
                    textSubmitted = true;
                    supportInvalidateOptionsMenu();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                logDebug("onQueryTextChange");
                if (drawerItem == DrawerItem.CHAT) {
                    searchQuery = newText;
                    recentChatsFragment = (RecentChatsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
                    if (recentChatsFragment != null) {
                        recentChatsFragment.filterChats(newText, false);
                    }
                } else if (drawerItem == DrawerItem.HOMEPAGE) {
                    if (mHomepageScreen == HomepageScreen.FULLSCREEN_OFFLINE) {
                        if (textSubmitted) {
                            textSubmitted = false;
                            return true;
                        }

                        searchQuery = newText;
                        setFullscreenOfflineFragmentSearchQuery(searchQuery);
                    } else if (mHomepageSearchable != null) {
                        searchQuery = newText;
                        mHomepageSearchable.searchQuery(searchQuery);
                    }
                } else {
                    if (textSubmitted) {
                        textSubmitted = false;
                    } else {
                        if (!textsearchQuery) {
                            searchQuery = newText;
                        }
                        if (getSearchFragment() != null) {
                            searchFragment.newSearchNodesTask();
                        }
                    }
                }
                return true;
            }
        });

        enableSelectMenuItem = menu.findItem(R.id.action_enable_select);
        doNotDisturbMenuItem = menu.findItem(R.id.action_menu_do_not_disturb);
        clearRubbishBinMenuitem = menu.findItem(R.id.action_menu_clear_rubbish_bin);
        cancelAllTransfersMenuItem = menu.findItem(R.id.action_menu_cancel_all_transfers);
        clearCompletedTransfers = menu.findItem(R.id.action_menu_clear_completed_transfers);
        retryTransfers = menu.findItem(R.id.action_menu_retry_transfers);
        playTransfersMenuIcon = menu.findItem(R.id.action_play);
        pauseTransfersMenuIcon = menu.findItem(R.id.action_pause);
        scanQRcodeMenuItem = menu.findItem(R.id.action_scan_qr);
        returnCallMenuItem = menu.findItem(R.id.action_return_call);
        openMeetingMenuItem = menu.findItem(R.id.action_menu_open_meeting);
        RelativeLayout rootView = (RelativeLayout) returnCallMenuItem.getActionView();
        layoutCallMenuItem = rootView.findViewById(R.id.layout_menu_call);
        chronometerMenuItem = rootView.findViewById(R.id.chrono_menu);
        chronometerMenuItem.setVisibility(View.GONE);
        MenuItem moreMenuItem = menu.findItem(R.id.action_more);
        openLinkMenuItem = menu.findItem(R.id.action_open_link);

        rootView.setOnClickListener(v1 -> onOptionsItemSelected(returnCallMenuItem));

        if (bNV != null) {
            Menu bNVMenu = bNV.getMenu();
            if (bNVMenu != null) {
                if (drawerItem == null) {
                    drawerItem = getStartDrawerItem(this);
                }

                if (drawerItem == DrawerItem.CLOUD_DRIVE) {
                    setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV);
                }
            }
        }

        setCallMenuItem(returnCallMenuItem, layoutCallMenuItem, chronometerMenuItem);

        if (isOnline(this)) {
            switch (drawerItem) {
                case CLOUD_DRIVE:
                    if (!isInMDMode) {
                        openLinkMenuItem.setVisible(isFirstNavigationLevel());
                        moreMenuItem.setVisible(!isFirstNavigationLevel());

                        if (isCloudAdded() && fileBrowserFragment.getItemCount() > 0) {
                            searchMenuItem.setVisible(true);
                        }
                    }
                    break;
                case HOMEPAGE:
                    if (mHomepageScreen == HomepageScreen.FULLSCREEN_OFFLINE) {
                        updateFullscreenOfflineFragmentOptionMenu(true);
                    }

                    break;
                case RUBBISH_BIN:
                    moreMenuItem.setVisible(!isFirstNavigationLevel());

                    if (getRubbishBinFragment() != null && rubbishBinFragment.getItemCount() > 0) {
                        clearRubbishBinMenuitem.setVisible(isFirstNavigationLevel());
                        searchMenuItem.setVisible(true);
                    }
                    break;
                case PHOTOS:
                    break;

                case INBOX:
                    moreMenuItem.setVisible(!isFirstNavigationLevel());

                    if (getInboxFragment() != null && inboxFragment.getItemCount() > 0) {
                        searchMenuItem.setVisible(true);
                    }
                    break;

                case SHARED_ITEMS:
                    moreMenuItem.setVisible(!isFirstNavigationLevel());

                    if (getTabItemShares() == INCOMING_TAB && isIncomingAdded()) {
                        if (isIncomingAdded() && incomingSharesFragment.getItemCount() > 0) {
                            searchMenuItem.setVisible(true);
                        }
                    } else if (getTabItemShares() == OUTGOING_TAB && isOutgoingAdded()) {
                        if (isOutgoingAdded() && outgoingSharesFragment.getItemCount() > 0) {
                            searchMenuItem.setVisible(true);
                        }
                    } else if (getTabItemShares() == LINKS_TAB && isLinksAdded()) {
                        if (isLinksAdded() && linksFragment.getItemCount() > 0) {
                            searchMenuItem.setVisible(true);
                        }
                    }
                    break;

                case SEARCH:
                    if (searchExpand) {
                        openSearchView();
                        searchFragment.checkSelectMode();
                    } else {
                        moreMenuItem.setVisible(!isFirstNavigationLevel());
                    }

                    break;

                case TRANSFERS:
                    if (getTabItemTransfers() == PENDING_TAB && isTransfersInProgressAdded() && transfersInProgress.size() > 0) {
                        if (megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD) || megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)) {
                            playTransfersMenuIcon.setVisible(true);
                        } else {
                            pauseTransfersMenuIcon.setVisible(true);
                        }

                        cancelAllTransfersMenuItem.setVisible(true);
                        enableSelectMenuItem.setVisible(true);
                    } else if (getTabItemTransfers() == COMPLETED_TAB && isTransfersInProgressAdded() && completedTransfersFragment.isAnyTransferCompleted()) {
                        clearCompletedTransfers.setVisible(true);
                        retryTransfers.setVisible(thereAreFailedOrCancelledTransfers());
                    }

                    break;

                case CHAT:
                    if (searchExpand) {
                        openSearchView();
                    } else {
                        openMeetingMenuItem.setVisible(true);
                        doNotDisturbMenuItem.setVisible(true);
                        openLinkMenuItem.setVisible(true);

                        if (getChatsFragment() != null && recentChatsFragment.getItemCount() > 0) {
                            searchMenuItem.setVisible(true);
                        }
                    }
                    break;

                case NOTIFICATIONS:
                    break;
            }
        }

        if (drawerItem == DrawerItem.HOMEPAGE) {
            // Get the Searchable again at onCreateOptionsMenu() after screen rotation
            mHomepageSearchable = findHomepageSearchable();

            if (searchExpand) {
                openSearchView();
            } else {
                if (mHomepageSearchable != null) {
                    searchMenuItem.setVisible(mHomepageSearchable.shouldShowSearchMenu());
                }
            }
        }

        logDebug("Call to super onCreateOptionsMenu");
        return super.onCreateOptionsMenu(menu);
    }

    private void openSearchOnHomepage() {
        textsearchQuery = false;
        firstNavigationLevel = true;
        parentHandleSearch = -1;
        levelsSearch = -1;
        setSearchDrawerItem();
        selectDrawerItem(drawerItem);
        resetActionBar(aB);

        if (searchFragment != null) {
            searchFragment.newSearchNodesTask();
        }
    }

    private void setFullscreenOfflineFragmentSearchQuery(String searchQuery) {
        if (fullscreenOfflineFragment != null) {
            fullscreenOfflineFragment.setSearchQuery(searchQuery);
        }
    }

    public void updateFullscreenOfflineFragmentOptionMenu(boolean openSearchView) {
        if (fullscreenOfflineFragment == null) {
            return;
        }

        if (searchExpand && openSearchView) {
            openSearchView();
        } else if (!searchExpand) {
            if (isOnline(this)) {
                if (fullscreenOfflineFragment.getItemCount() > 0
                        && !fullscreenOfflineFragment.searchMode() && searchMenuItem != null) {
                    searchMenuItem.setVisible(true);
                }
            } else {
                supportInvalidateOptionsMenu();
            }

            fullscreenOfflineFragment.refreshActionBarTitle();
        }
    }

    private HomepageSearchable findHomepageSearchable() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment navHostFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null && navHostFragment.getChildFragmentManager() != null) {
            for (Fragment fragment : navHostFragment.getChildFragmentManager().getFragments()) {
                if (fragment instanceof HomepageSearchable) {
                    return (HomepageSearchable) fragment;
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public <F extends Fragment> F getFragmentByType(Class<F> fragmentClass) {
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            return null;
        }

        for (Fragment fragment : navHostFragment.getChildFragmentManager().getFragments()) {
            if (fragment.getClass() == fragmentClass) {
                return (F) fragment;
            }
        }

        return null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        logDebug("onOptionsItemSelected");
        typesCameraPermission = INVALID_TYPE_PERMISSIONS;

        if (megaApi == null) {
            megaApi = ((MegaApplication) getApplication()).getMegaApi();
        }

        if (megaApi != null) {
            logDebug("retryPendingConnections");
            megaApi.retryPendingConnections();
        }

        if (megaChatApi != null) {
            megaChatApi.retryPendingConnections(false, null);
        }

        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                if (firstNavigationLevel && drawerItem != DrawerItem.SEARCH) {
                    if (drawerItem == DrawerItem.RUBBISH_BIN || drawerItem == DrawerItem.INBOX
                            || drawerItem == DrawerItem.NOTIFICATIONS || drawerItem == DrawerItem.TRANSFERS) {

                        backToDrawerItem(bottomNavigationCurrentItem);
                    } else {
                        drawerLayout.openDrawer(nV);
                    }
                } else {
                    logDebug("NOT firstNavigationLevel");
                    if (drawerItem == DrawerItem.CLOUD_DRIVE) {
                        //Check media discovery mode
                        if (isInMDMode) {
                            onBackPressed();
                        } else {
                            //Cloud Drive
                            if (isCloudAdded()) {
                                fileBrowserFragment.onBackPressed();
                            }
                        }
                    } else if (drawerItem == DrawerItem.RUBBISH_BIN) {
                        rubbishBinFragment = (RubbishBinFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
                        if (rubbishBinFragment != null) {
                            rubbishBinFragment.onBackPressed();
                        }
                    } else if (drawerItem == DrawerItem.SHARED_ITEMS) {
                        if (getTabItemShares() == INCOMING_TAB && isIncomingAdded()) {
                            incomingSharesFragment.onBackPressed();
                        } else if (getTabItemShares() == OUTGOING_TAB && isOutgoingAdded()) {
                            outgoingSharesFragment.onBackPressed();
                        } else if (getTabItemShares() == LINKS_TAB && isLinksAdded()) {
                            linksFragment.onBackPressed();
                        }
                    } else if (drawerItem == DrawerItem.PHOTOS) {
                        if (getPhotosFragment() != null) {
                            if (photosFragment.isEnablePhotosFragmentShown()) {
                                photosFragment.onBackPressed();
                                return true;
                            }

                            setToolbarTitle();
                            invalidateOptionsMenu();
                            return true;
                        } else if (isInAlbumContent) {
                            // When current fragment is AlbumContentFragment, the photosFragment will be null due to replaceFragment.
                            onBackPressed();
                        }
					} else if (drawerItem == DrawerItem.INBOX) {
						inboxFragment = (InboxFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.INBOX.getTag());
						if (inboxFragment != null){
							inboxFragment.onBackPressed();
							return true;
						}
					}
		    		else if (drawerItem == DrawerItem.SEARCH) {
		    			if (getSearchFragment() != null){
		    				onBackPressed();
		    				return true;
		    			}
		    		}
		    		else if (drawerItem == DrawerItem.TRANSFERS) {
						drawerItem = getStartDrawerItem(this);
						selectDrawerItem(drawerItem);
						return true;
		    		} else if (drawerItem == DrawerItem.HOMEPAGE) {
						if (mHomepageScreen == HomepageScreen.FULLSCREEN_OFFLINE) {
							handleBackPressIfFullscreenOfflineFragmentOpened();
						} else if (mNavController.getCurrentDestination() != null &&
                                mNavController.getCurrentDestination().getId() == R.id.favouritesFolderFragment) {
                            onBackPressed();
                        } else {
							mNavController.navigateUp();
						}
					} else {
						super.onBackPressed();
					}
				}
		    	return true;
		    }
			case R.id.action_search:{
				logDebug("Action search selected");
                hideItemsWhenSearchSelected();
                return true;
            }
            case R.id.action_open_link:
                showOpenLinkDialog();
                return true;

            case R.id.action_menu_cancel_all_transfers: {
                showConfirmationCancelAllTransfers();
                return true;
            }
            case R.id.action_menu_clear_completed_transfers: {
                showConfirmationClearCompletedTransfers();
                return true;
            }
            case R.id.action_pause: {
                if (drawerItem == DrawerItem.TRANSFERS) {
                    logDebug("Click on action_pause - play visible");
                    megaApi.pauseTransfers(true, this);
                    pauseTransfersMenuIcon.setVisible(false);
                    playTransfersMenuIcon.setVisible(true);
                }

                return true;
            }
            case R.id.action_play: {
                logDebug("Click on action_play - pause visible");
                pauseTransfersMenuIcon.setVisible(true);
                playTransfersMenuIcon.setVisible(false);
                megaApi.pauseTransfers(false, this);

                return true;
            }
            case R.id.action_menu_do_not_disturb:
                if (drawerItem == DrawerItem.CHAT) {
                    if (getGeneralNotification().equals(NOTIFICATIONS_ENABLED)) {
                        createMuteNotificationsChatAlertDialog(this, null);
                    } else {
                        showSnackbar(MUTE_NOTIFICATIONS_SNACKBAR_TYPE, null, -1);
                    }
                }
                return true;

            case R.id.action_select: {
                switch (drawerItem) {
                    case CLOUD_DRIVE:
                        if (isCloudAdded()) {
                            fileBrowserFragment.selectAll();
                        }
                        break;

                    case RUBBISH_BIN:
                        if (getRubbishBinFragment() != null) {
                            rubbishBinFragment.selectAll();
                        }
                        break;

                    case SHARED_ITEMS:
                        switch (getTabItemShares()) {
                            case INCOMING_TAB:
                                if (isIncomingAdded()) {
                                    incomingSharesFragment.selectAll();
                                }
                                break;

                            case OUTGOING_TAB:
                                if (isOutgoingAdded()) {
                                    outgoingSharesFragment.selectAll();
                                }
                                break;

                            case LINKS_TAB:
                                if (isLinksAdded()) {
                                    linksFragment.selectAll();
                                }
                                break;
                        }
                        break;
                    case HOMEPAGE:
                        if (fullscreenOfflineFragment != null) {
                            fullscreenOfflineFragment.selectAll();
                        }
                        break;
                    case CHAT:
                        if (getChatsFragment() != null) {
                            recentChatsFragment.selectAll();
                        }
                        break;

                    case INBOX:
                        if (getInboxFragment() != null) {
                            inboxFragment.selectAll();
                        }
                        break;

                    case SEARCH:
                        if (getSearchFragment() != null) {
                            searchFragment.selectAll();
                        }
                        break;
                }

                return true;
            }
            case R.id.action_menu_clear_rubbish_bin:
                showClearRubbishBinDialog();
                return true;

            case R.id.action_scan_qr: {
                logDebug("Action menu scan QR code pressed");
                //Check if there is a in progress call:
                checkBeforeOpeningQR(true);
                return true;
            }
            case R.id.action_return_call: {
                logDebug("Action menu return to call in progress pressed");
                returnCallWithPermissions();
                return true;
            }
            case R.id.action_menu_retry_transfers:
                retryAllTransfers();
                return true;

            case R.id.action_enable_select:
                if (isTransfersInProgressAdded()) {
                    transfersFragment.activateActionMode();
                }
                return true;
            case R.id.action_menu_open_meeting:
                // Click to enter "create meeting"
                onCreateMeeting();
                return true;

            case R.id.action_more:
                showNodeOptionsPanel(getCurrentParentNode(getCurrentParentHandle(), INVALID_VALUE));
                return true;

            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    private void hideItemsWhenSearchSelected() {
        textSubmitted = false;

        if (searchMenuItem != null) {
            doNotDisturbMenuItem.setVisible(false);
            cancelAllTransfersMenuItem.setVisible(false);
            clearCompletedTransfers.setVisible(false);
            pauseTransfersMenuIcon.setVisible(false);
            playTransfersMenuIcon.setVisible(false);
            clearRubbishBinMenuitem.setVisible(false);
            searchMenuItem.setVisible(false);
            openMeetingMenuItem.setVisible(false);
            openLinkMenuItem.setVisible(false);
        }
    }

    public void returnCallWithPermissions() {
        if (checkPermissionsCall(this, RETURN_CALL_PERMISSIONS)) {
            returnActiveCall(this, passcodeManagement);
        }
    }

    public void checkBeforeOpeningQR(boolean openScanQR) {
        if (isNecessaryDisableLocalCamera() != MEGACHAT_INVALID_HANDLE) {
            showConfirmationOpenCamera(this, ACTION_OPEN_QR, openScanQR);
            return;
        }
        openQR(openScanQR);
    }

    public void openQR(boolean openScanQr) {
        if (openScanQr) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ScanCodeFragment()).commitNowAllowingStateLoss();
        }

        Intent intent = new Intent(this, QRCodeActivity.class);
        intent.putExtra(OPEN_SCAN_QR, openScanQr);
        startActivity(intent);
    }

    private void updateView(boolean isList) {
        if (this.isList != isList) {
            this.isList = isList;
            dbH.setPreferredViewList(isList);
        }

        LiveEventBus.get(EVENT_LIST_GRID_CHANGE, Boolean.class).post(isList);

        //Refresh Cloud Fragment
        refreshFragment(FragmentTag.CLOUD_DRIVE.getTag());

        //Refresh Rubbish Fragment
        refreshFragment(FragmentTag.RUBBISH_BIN.getTag());

        //Refresh shares section
        refreshFragment(FragmentTag.INCOMING_SHARES.getTag());

        //Refresh shares section
        refreshFragment(FragmentTag.OUTGOING_SHARES.getTag());

        refreshSharesPageAdapter();

        //Refresh search section
        refreshFragment(FragmentTag.SEARCH.getTag());

        //Refresh inbox section
        refreshFragment(FragmentTag.INBOX.getTag());
    }

    public void refreshAfterMovingToRubbish() {
        logDebug("refreshAfterMovingToRubbish");

        if (drawerItem == DrawerItem.CLOUD_DRIVE) {
            refreshCloudDrive();
        } else if (drawerItem == DrawerItem.INBOX) {
            onNodesInboxUpdate();
        } else if (drawerItem == DrawerItem.SHARED_ITEMS) {
            onNodesSharedUpdate();
        } else if (drawerItem == DrawerItem.SEARCH) {
            refreshSearch();
        } else if (drawerItem == DrawerItem.HOMEPAGE) {
            LiveEventBus.get(EVENT_NODES_CHANGE).post(false);
        }

        checkCameraUploadFolder(true, null);
        refreshRubbishBin();
        setToolbarTitle();
    }

    /**
     * After nodes on Cloud Drive changed or some nodes are moved to rubbish bin,
     * need to check CU and MU folders' status.
     *
     * @param shouldDisable If CU or MU folder is deleted by current client, then CU should be disabled. Otherwise not.
     * @param updatedNodes  Nodes which have changed.
     */
    private void checkCameraUploadFolder(boolean shouldDisable, List<MegaNode> updatedNodes) {
        // Get CU and MU folder hanlde from local setting.
        long primaryHandle = getPrimaryFolderHandle();
        long secondaryHandle = getSecondaryFolderHandle();

        if (updatedNodes != null) {
            List<Long> handles = new ArrayList<>();
            for (MegaNode node : updatedNodes) {
                handles.add(node.getHandle());
            }
            // If CU and MU folder don't change then return.
            if (!handles.contains(primaryHandle) && !handles.contains(secondaryHandle)) {
                logDebug("Updated nodes don't include CU/MU, return.");
                return;
            }
        }

        MegaPreferences prefs = dbH.getPreferences();
        boolean isSecondaryEnabled = false;
        if (prefs != null) {
            isSecondaryEnabled = Boolean.parseBoolean(prefs.getSecondaryMediaFolderEnabled());
        }

        // Check if CU and MU folder are moved to rubbish bin.
        boolean isPrimaryFolderInRubbish = isNodeInRubbish(primaryHandle);
        boolean isSecondaryFolderInRubbish = isSecondaryEnabled && isNodeInRubbish(secondaryHandle);

        // If only MU folder is in rubbish bin.
        if (isSecondaryFolderInRubbish && !isPrimaryFolderInRubbish) {
            logDebug("MU folder is deleted, backup settings and disable MU.");
            if (shouldDisable) {
                // Back up timestamps and disabled MU upload.
                backupTimestampsAndFolderHandle();
                disableMediaUploadProcess();
            } else {
                // Just stop the upload process.
                fireStopCameraUploadJob(app);
            }
        } else if (isPrimaryFolderInRubbish) {
            // If CU folder is in rubbish bin.
            logDebug("CU folder is deleted, backup settings and disable CU.");
            if (shouldDisable) {
                // Disable both CU and MU.
                backupTimestampsAndFolderHandle();
                disableCameraUploadSettingProcess(false);
                sendBroadcast(new Intent(ACTION_UPDATE_DISABLE_CU_UI_SETTING));
            } else {
                // Just stop the upload process.
                fireStopCameraUploadJob(app);
            }
        }
    }

    public void refreshRubbishBin() {
        rubbishBinFragment = (RubbishBinFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
        if (rubbishBinFragment != null) {
            ArrayList<MegaNode> nodes;
            if (parentHandleRubbish == -1) {
                nodes = megaApi.getChildren(megaApi.getRubbishNode(), sortOrderManagement.getOrderCloud());
            } else {
                nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleRubbish),
                        sortOrderManagement.getOrderCloud());
            }

            rubbishBinFragment.hideMultipleSelect();
            rubbishBinFragment.setNodes(nodes);
            rubbishBinFragment.getRecyclerView().invalidate();
        }
    }

    public void refreshAfterMoving() {
        logDebug("refreshAfterMoving");
        if (drawerItem == DrawerItem.CLOUD_DRIVE) {

            //Refresh Cloud Fragment
            refreshCloudDrive();

            //Refresh Rubbish Fragment
            refreshRubbishBin();
        } else if (drawerItem == DrawerItem.RUBBISH_BIN) {
            //Refresh Rubbish Fragment
            refreshRubbishBin();
        } else if (drawerItem == DrawerItem.INBOX) {
            onNodesInboxUpdate();

            refreshCloudDrive();
        } else if (drawerItem == DrawerItem.SHARED_ITEMS) {
            onNodesSharedUpdate();

            //Refresh Cloud Fragment
            refreshCloudDrive();

            //Refresh Rubbish Fragment
            refreshRubbishBin();
        } else if (drawerItem == DrawerItem.SEARCH) {
            refreshSearch();
        }

        setToolbarTitle();
    }

    public void refreshSearch() {
        if (getSearchFragment() != null) {
            searchFragment.hideMultipleSelect();
            searchFragment.refresh();
        }
    }

    public void refreshAfterRemoving() {
        logDebug("refreshAfterRemoving");

        rubbishBinFragment = (RubbishBinFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
        if (rubbishBinFragment != null) {
            rubbishBinFragment.hideMultipleSelect();

            if (isClearRubbishBin) {
                isClearRubbishBin = false;
                parentHandleRubbish = megaApi.getRubbishNode().getHandle();
                ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRubbishNode(),
                        sortOrderManagement.getOrderCloud());
                rubbishBinFragment.setNodes(nodes);
                rubbishBinFragment.getRecyclerView().invalidate();
            } else {
                refreshRubbishBin();
            }
        }

        onNodesInboxUpdate();

        refreshSearch();
    }

    @Override
    public void onBackPressed() {
        logDebug("onBackPressed");

        // Let the PSA web browser fragment (if visible) to consume the back key event
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return;

        retryConnectionsAndSignalPresence();

        if (drawerLayout.isDrawerOpen(nV)) {
            drawerLayout.closeDrawer(Gravity.LEFT);
            return;
        }

        dismissAlertDialogIfExists(statusDialog);

        logDebug("DRAWERITEM: " + drawerItem);

        if (turnOnNotifications) {
            deleteTurnOnNotificationsFragment();
            return;
        }
        if (onAskingPermissionsFragment || onAskingSMSVerificationFragment) {
            return;
        }

        if (mNavController.getCurrentDestination() != null &&
            mNavController.getCurrentDestination().getId() == R.id.favouritesFolderFragment) {
            super.onBackPressed();
            return;
        }

        if (drawerItem == DrawerItem.CLOUD_DRIVE) {
            if (isInMDMode) {
                changeMDMode(false);
                backToDrawerItem(bottomNavigationCurrentItem);
            } else {
                if (!isCloudAdded() || fileBrowserFragment.onBackPressed() == 0) {
                    performOnBack();
                }
            }
        } else if (drawerItem == DrawerItem.RUBBISH_BIN) {
            rubbishBinFragment = (RubbishBinFragment) getSupportFragmentManager()
                    .findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
            if (rubbishBinFragment == null || rubbishBinFragment.onBackPressed() == 0) {
                backToDrawerItem(bottomNavigationCurrentItem);
            }
        } else if (drawerItem == DrawerItem.TRANSFERS) {
            backToDrawerItem(bottomNavigationCurrentItem);

        } else if (drawerItem == DrawerItem.INBOX) {
            inboxFragment = (InboxFragment) getSupportFragmentManager()
                    .findFragmentByTag(FragmentTag.INBOX.getTag());
            if (inboxFragment == null || inboxFragment.onBackPressed() == 0) {
                backToDrawerItem(bottomNavigationCurrentItem);
            }
        } else if (drawerItem == DrawerItem.NOTIFICATIONS) {
            backToDrawerItem(bottomNavigationCurrentItem);
        } else if (drawerItem == DrawerItem.SHARED_ITEMS) {
            switch (getTabItemShares()) {
                case INCOMING_TAB:
                    if (!isIncomingAdded() || incomingSharesFragment.onBackPressed() == 0) {
                        performOnBack();
                    }
                    break;
                case OUTGOING_TAB:
                    if (!isOutgoingAdded() || outgoingSharesFragment.onBackPressed() == 0) {
                        performOnBack();
                    }
                    break;
                case LINKS_TAB:
                    if (!isLinksAdded() || linksFragment.onBackPressed() == 0) {
                        performOnBack();
                    }
                    break;
                default:
                    performOnBack();
                    break;
            }
        } else if (drawerItem == DrawerItem.CHAT) {
            if (getChatsFragment() != null && isFabExpanded) {
                collapseFab();
            } else {
                performOnBack();
            }
        } else if (drawerItem == DrawerItem.PHOTOS) {
            if (isInAlbumContent) {
                fromAlbumContent = true;
                isInAlbumContent = false;

                backToDrawerItem(bottomNavigationCurrentItem);
                if (photosFragment == null) {
                    backToDrawerItem(bottomNavigationCurrentItem);
                } else {
                    photosFragment.switchToAlbum();
                }
            } else if (getPhotosFragment() == null || photosFragment.onBackPressed() == 0) {
                performOnBack();
            }
        } else if (drawerItem == DrawerItem.SEARCH) {
            if (getSearchFragment() == null || searchFragment.onBackPressed() == 0) {
                closeSearchSection();
            }
        } else if (isInMainHomePage()) {
            HomepageFragment fragment = getFragmentByType(HomepageFragment.class);
            if (fragment != null && fragment.isFabExpanded()) {
                fragment.collapseFab();
            } else {
                performOnBack();
            }
        } else {
            handleBackPressIfFullscreenOfflineFragmentOpened();
        }
    }

    /**
     * Closes the app if the current DrawerItem is the same as the preferred one.
     * If not, sets the current DrawerItem as the preferred one.
     */
    private void performOnBack() {
        int startItem = getStartBottomNavigationItem(this);

        if (shouldCloseApp(startItem, drawerItem)) {
            // The Psa requires the activity to load the new PSA even though the app is on the
            // background. So don't call super.onBackPressed() since it will destroy this activity
            // and its embedded web browser fragment.
            moveTaskToBack(false);
        } else {
            backToDrawerItem(startItem);
        }
    }

    private void handleBackPressIfFullscreenOfflineFragmentOpened() {
        if (fullscreenOfflineFragment == null || fullscreenOfflineFragment.onBackPressed() == 0) {
            // workaround for flicker of AppBarLayout: if we go back to homepage from fullscreen
            // offline, and hide AppBarLayout when immediately on go back, we will see the flicker
            // of AppBarLayout, hide AppBarLayout when fullscreen offline is closed is better.
            if (bottomNavigationCurrentItem != HOME_BNV) {
                backToDrawerItem(bottomNavigationCurrentItem);
            } else {
                drawerItem = DrawerItem.HOMEPAGE;
            }
            super.onBackPressed();
        }
    }

    public void adjustTransferWidgetPositionInHomepage() {
        if (isInMainHomePage()) {
            RelativeLayout transfersWidgetLayout = findViewById(R.id.transfers_widget_layout);
            if (transfersWidgetLayout == null) return;

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) transfersWidgetLayout.getLayoutParams();
            params.bottomMargin = Util.dp2px(TRANSFER_WIDGET_MARGIN_BOTTOM, outMetrics);
            params.gravity = Gravity.END;
            transfersWidgetLayout.setLayoutParams(params);
        }
    }

    /**
     * Update the PSA view visibility. It should only visible in root homepage tab.
     */
    private void updatePsaViewVisibility() {
        psaViewHolder.toggleVisible(isInMainHomePage());
        if (psaViewHolder.visible()) {
            handler.post(this::updateHomepageFabPosition);
        } else {
            updateHomepageFabPosition();
        }
    }

    public void backToDrawerItem(int item) {
        if (item == CLOUD_DRIVE_BNV) {
            drawerItem = DrawerItem.CLOUD_DRIVE;
            if (isCloudAdded()) {
                fileBrowserFragment.setTransferOverQuotaBannerVisibility();
            }
        } else if (item == PHOTOS_BNV) {
            drawerItem = DrawerItem.PHOTOS;
        } else if (item == CHAT_BNV) {
            drawerItem = DrawerItem.CHAT;
        } else if (item == SHARED_ITEMS_BNV) {
            drawerItem = DrawerItem.SHARED_ITEMS;
        } else if (item == HOME_BNV || item == -1) {
            drawerItem = DrawerItem.HOMEPAGE;
        }

        selectDrawerItem(drawerItem);
    }

    void isFirstTimeCam() {
        if (firstLogin) {
            firstLogin = false;
            dbH.setCamSyncEnabled(false);
            bottomNavigationCurrentItem = CLOUD_DRIVE_BNV;
        }
    }

    private void checkIfShouldCloseSearchView(DrawerItem oldDrawerItem) {
        if (!searchExpand) return;

        if (oldDrawerItem == DrawerItem.CHAT
                || (oldDrawerItem == DrawerItem.HOMEPAGE
                && mHomepageScreen == HomepageScreen.FULLSCREEN_OFFLINE)) {
            searchExpand = false;
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        logDebug("onNavigationItemSelected");

        if (nV != null) {
            Menu nVMenu = nV.getMenu();
            resetNavigationViewMenu(nVMenu);
        }

        DrawerItem oldDrawerItem = drawerItem;

        switch (menuItem.getItemId()) {
            case R.id.bottom_navigation_item_cloud_drive: {
                if (drawerItem == DrawerItem.CLOUD_DRIVE) {
                    if (isInMDMode) {
                        changeMDMode(false);
                    }
                    MegaNode rootNode = megaApi.getRootNode();
                    if (rootNode == null) {
                        logError("Root node is null");
                    }

                    if (parentHandleBrowser != INVALID_HANDLE
                            && rootNode != null && parentHandleBrowser != rootNode.getHandle()) {
                        parentHandleBrowser = rootNode.getHandle();
                        refreshFragment(FragmentTag.CLOUD_DRIVE.getTag());
                        if (isCloudAdded()) {
                            fileBrowserFragment.scrollToFirstPosition();
                        }
                    }
                } else {
                    drawerItem = DrawerItem.CLOUD_DRIVE;
                    setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV);
                }
                break;
            }
            case R.id.bottom_navigation_item_homepage: {
                drawerItem = DrawerItem.HOMEPAGE;
                if (fullscreenOfflineFragment != null) {
                    super.onBackPressed();
                    return true;
                } else {
                    setBottomNavigationMenuItemChecked(HOME_BNV);
                }
                break;
            }
            case R.id.bottom_navigation_item_camera_uploads: {
                // if pre fragment is the same one, do nothing.
                if (oldDrawerItem != DrawerItem.PHOTOS) {
                    drawerItem = DrawerItem.PHOTOS;
                    setBottomNavigationMenuItemChecked(PHOTOS_BNV);
                }
                break;
            }
            case R.id.bottom_navigation_item_shared_items: {
                if (drawerItem == DrawerItem.SHARED_ITEMS) {
                    if (getTabItemShares() == INCOMING_TAB && parentHandleIncoming != INVALID_HANDLE) {
                        parentHandleIncoming = INVALID_HANDLE;
                        refreshFragment(FragmentTag.INCOMING_SHARES.getTag());
                    } else if (getTabItemShares() == OUTGOING_TAB && parentHandleOutgoing != INVALID_HANDLE) {
                        parentHandleOutgoing = INVALID_HANDLE;
                        refreshFragment(FragmentTag.OUTGOING_SHARES.getTag());
                    } else if (getTabItemShares() == LINKS_TAB && parentHandleLinks != INVALID_HANDLE) {
                        parentHandleLinks = INVALID_HANDLE;
                        refreshFragment(FragmentTag.LINKS.getTag());
                    }

                    refreshSharesPageAdapter();
                } else {
                    drawerItem = DrawerItem.SHARED_ITEMS;
                    setBottomNavigationMenuItemChecked(SHARED_ITEMS_BNV);
                }
                break;
            }
            case R.id.bottom_navigation_item_chat: {
                drawerItem = DrawerItem.CHAT;
                setBottomNavigationMenuItemChecked(CHAT_BNV);
                break;
            }
        }

        checkIfShouldCloseSearchView(oldDrawerItem);
        selectDrawerItem(drawerItem);
        drawerLayout.closeDrawer(Gravity.LEFT);

        return true;
    }

    @Override
    public void showSnackbar(int type, String content, long chatId) {
        showSnackbar(type, fragmentContainer, content, chatId);
    }

    /**
     * Restores a list of nodes from Rubbish Bin to their original parent.
     *
     * @param nodes List of nodes.
     */
    public void restoreFromRubbish(final List<MegaNode> nodes) {
        moveNodeUseCase.restore(nodes)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result, throwable) -> {
                    if (throwable == null) {
                        boolean notValidView = result.isSingleAction() && result.isSuccess()
                                && parentHandleRubbish == nodes.get(0).getHandle();

                        showRestorationOrRemovalResult(notValidView, result.isForeignNode(),
                                result.getResultText());
                    }
                });
    }

    /**
     * Shows the final result of a restoration or removal from Rubbish Bin section.
     *
     * @param notValidView  True if should update the view, false otherwise.
     * @param isForeignNode True if should show a foreign warning, false otherwise.
     * @param message       Text message to show as the request result.
     */
    private void showRestorationOrRemovalResult(boolean notValidView, boolean isForeignNode, String message) {
        if (notValidView) {
            parentHandleRubbish = INVALID_HANDLE;
            setToolbarTitle();
            refreshRubbishBin();
        }

        if (isForeignNode) {
            showForeignStorageOverQuotaWarningDialog(this);
        } else {
            showSnackbar(SNACKBAR_TYPE, message, MEGACHAT_INVALID_HANDLE);
        }
    }

    public void showRenameDialog(final MegaNode document) {
        showRenameNodeDialog(this, document, this, this);
    }

    /**
     * Launches an intent to get the links of the nodes received.
     *
     * @param nodes List of nodes to get their links.
     */
    public void showGetLinkActivity(List<MegaNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), MEGACHAT_INVALID_HANDLE);
            return;
        }

        if (nodes.size() == 1) {
            showGetLinkActivity(nodes.get(0).getHandle());
            return;
        }

        long[] handles = new long[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            MegaNode node = nodes.get(i);
            if (showTakenDownNodeActionNotAvailableDialog(node, this)) {
                return;
            }

            handles[i] = node.getHandle();
        }

        LinksUtil.showGetLinkActivity(this, handles);
    }

    public void showGetLinkActivity(long handle) {
        logDebug("Handle: " + handle);
        MegaNode node = megaApi.getNodeByHandle(handle);
        if (node == null) {
            showSnackbar(SNACKBAR_TYPE, getString(R.string.warning_node_not_exists_in_cloud), MEGACHAT_INVALID_HANDLE);
            return;
        }


        if (showTakenDownNodeActionNotAvailableDialog(node, this)) {
            return;
        }

        LinksUtil.showGetLinkActivity(this, handle);

        refreshAfterMovingToRubbish();
    }

    /*
     * Display keyboard
     */
    private void showKeyboardDelayed(final View view) {
        logDebug("showKeyboardDelayed");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 50);
    }

    public void setIsClearRubbishBin(boolean value) {
        this.isClearRubbishBin = value;
    }

    /**
     * Move folders or files that belong to "My backups"
     *
     * @param handleList handleList handles list of the nodes that selected
     */
    public void moveBackupNode(final ArrayList<Long> handleList) {
        logDebug("MyBackup + NodeOptionsBottomSheetDialogFragment Move a backup folder or file");
        fileBackupManager.moveBackup(nC, handleList);
    }

    /**
     * Delete folders or files that included "My backup"
     *
     * @param handleList handleList handles list of the nodes that selected
     */
    public void askConfirmationMoveToRubbish(final ArrayList<Long> handleList) {
        logDebug("askConfirmationMoveToRubbish");
        isClearRubbishBin = false;

        if (handleList != null) {

            if (handleList.size() > 0) {
                Long handle = handleList.get(0);
                MegaNode p = megaApi.getNodeByHandle(handle);
                while (megaApi.getParentNode(p) != null) {
                    p = megaApi.getParentNode(p);
                }
                if (p.getHandle() != megaApi.getRubbishNode().getHandle()) {
                    if (fileBackupManager.removeBackup(moveNodeUseCase, handleList)) {
                        return;
                    }

                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
                    if (getPrimaryFolderHandle() == handle && CameraUploadUtil.isPrimaryEnabled()) {
                        builder.setMessage(getResources().getString(R.string.confirmation_move_cu_folder_to_rubbish));
                    } else if (getSecondaryFolderHandle() == handle && CameraUploadUtil.isSecondaryEnabled()) {
                        builder.setMessage(R.string.confirmation_move_mu_folder_to_rubbish);
                    } else {
                        builder.setMessage(getResources().getString(R.string.confirmation_move_to_rubbish));
                    }

                    builder.setPositiveButton(R.string.general_move, (dialog, which) ->
                            moveNodeUseCase.moveToRubbishBin(handleList)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe((result, throwable) -> {
                                        if (throwable == null) {
                                            showMovementResult(result, handleList.get(0));
                                        }
                                    }));

                    builder.setNegativeButton(R.string.general_cancel, null);
                    builder.show();
                } else {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
                    builder.setMessage(getResources().getString(R.string.confirmation_delete_from_mega));

                    builder.setPositiveButton(R.string.rubbish_bin_delete_confirmation_dialog_button_delete, (dialog, which) ->
                            removeNodeUseCase.remove(handleList)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe((result, throwable) -> {
                                        if (throwable == null) {
                                            boolean notValidView = result.isSingleAction()
                                                    && result.isSuccess()
                                                    && parentHandleRubbish == handleList.get(0);

                                            showRestorationOrRemovalResult(notValidView, false,
                                                    result.getResultText());
                                        }
                                    }));

                    builder.setNegativeButton(R.string.general_cancel, null);
                    builder.show();
                }
            }
        } else {
            logWarning("handleList NULL");
            return;
        }

    }

    public void showWarningDialogOfShare(final MegaNode p, int nodeType, int actionType) {
        logDebug("showWarningDialogOfShareFolder");
        if (actionType == ACTION_BACKUP_SHARE_FOLDER) {
            fileBackupManager.shareBackupFolder(nC, p, nodeType, actionType);
        }
    }

    /**
     * Shows the final result of a movement request.
     *
     * @param result Object containing the request result.
     * @param handle Handle of the node to mode.
     */
    private void showMovementResult(MoveRequestResult result, long handle) {
        if (result.isSingleAction() && result.isSuccess() && getCurrentParentHandle() == handle) {
            switch (drawerItem) {
                case CLOUD_DRIVE:
                    parentHandleBrowser = result.getOldParentHandle();
                    refreshCloudDrive();
                    break;

                case INBOX:
                    parentHandleInbox = result.getOldParentHandle();
                    refreshInboxList();
                    break;

                case SHARED_ITEMS:
                    switch (getTabItemShares()) {
                        case INCOMING_TAB:
                            decreaseDeepBrowserTreeIncoming();
                            parentHandleIncoming = deepBrowserTreeIncoming == 0 ? INVALID_HANDLE : result.getOldParentHandle();
                            refreshIncomingShares();
                            break;

                        case OUTGOING_TAB:
                            decreaseDeepBrowserTreeOutgoing();
                            parentHandleOutgoing = deepBrowserTreeOutgoing == 0 ? INVALID_HANDLE : result.getOldParentHandle();

                            if (parentHandleOutgoing == INVALID_HANDLE) {
                                hideTabs(false, OUTGOING_TAB);
                            }

                            refreshOutgoingShares();
                            break;

                        case LINKS_TAB:
                            decreaseDeepBrowserTreeLinks();
                            parentHandleLinks = deepBrowserTreeLinks == 0 ? INVALID_HANDLE : result.getOldParentHandle();

                            if (parentHandleLinks == INVALID_HANDLE) {
                                hideTabs(false, LINKS_TAB);
                            }

                            refreshLinks();
                            break;
                    }

                case SEARCH:
                    parentHandleSearch = levelsSearch > 0 ? result.getOldParentHandle() : INVALID_HANDLE;
                    levelsSearch--;
                    refreshSearch();
                    break;

            }

            setToolbarTitle();
        }

        if (result.isForeignNode()) {
            showForeignStorageOverQuotaWarningDialog(this);
        } else {
            showSnackbar(SNACKBAR_TYPE, result.getResultText(), MEGACHAT_INVALID_HANDLE);
        }
    }

    /**
     * Shows an error in the Open link dialog.
     *
     * @param show  True if should show an error.
     * @param error Error value to identify and show the corresponding error.
     */
    private void showOpenLinkError(boolean show, int error) {
        if (openLinkDialog != null) {
            if (show) {
                openLinkDialogIsErrorShown = true;
                ColorUtils.setErrorAwareInputAppearance(openLinkText, true);
                openLinkError.setVisibility(View.VISIBLE);
                if (drawerItem == DrawerItem.CLOUD_DRIVE) {
                    if (openLinkText.getText().toString().isEmpty()) {
                        openLinkErrorText.setText(R.string.invalid_file_folder_link_empty);
                        return;
                    }
                    switch (error) {
                        case CHAT_LINK: {
                            openLinkText.setTextColor(ColorUtils.getThemeColor(this,
                                    android.R.attr.textColorPrimary));
                            openLinkErrorText.setText(R.string.valid_chat_link);
                            openLinkOpenButton.setText(R.string.action_open_chat_link);
                            break;
                        }
                        case CONTACT_LINK: {
                            openLinkText.setTextColor(ColorUtils.getThemeColor(this,
                                    android.R.attr.textColorPrimary));
                            openLinkErrorText.setText(R.string.valid_contact_link);
                            openLinkOpenButton.setText(R.string.action_open_contact_link);
                            break;
                        }
                        case ERROR_LINK: {
                            openLinkErrorText.setText(R.string.invalid_file_folder_link);
                            break;
                        }
                    }
                } else if (drawerItem == DrawerItem.CHAT) {
                    if (openLinkText.getText().toString().isEmpty()) {
                        openLinkErrorText.setText(chatLinkDialogType == LINK_DIALOG_CHAT ?
                                R.string.invalid_chat_link_empty : R.string.invalid_meeting_link_empty);
                        return;
                    }
                    openLinkErrorText.setText(chatLinkDialogType == LINK_DIALOG_CHAT ?
                            R.string.invalid_chat_link_args : R.string.invalid_meeting_link_args);
                }
            } else {
                openLinkDialogIsErrorShown = false;
                if (openLinkError.getVisibility() == View.VISIBLE) {
                    ColorUtils.setErrorAwareInputAppearance(openLinkText, false);
                    openLinkError.setVisibility(View.GONE);
                    openLinkOpenButton.setText(R.string.context_open_link);
                }
            }
        }
    }

    /**
     * Opens a links via Open link dialog.
     *
     * @param link The link to open.
     */
    private void openLink(String link) {
        // Password link
        if (matchRegexs(link, PASSWORD_LINK_REGEXS)) {
            dismissAlertDialogIfExists(openLinkDialog);
            Intent openLinkIntent = new Intent(this, OpenPasswordLinkActivity.class);
            openLinkIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            openLinkIntent.setData(Uri.parse(link));
            startActivity(openLinkIntent);
            return;
        }

        if (drawerItem == DrawerItem.CLOUD_DRIVE) {
            int linkType = nC.importLink(link);
            if (openLinkError.getVisibility() == View.VISIBLE) {
                switch (linkType) {
                    case CHAT_LINK: {
                        logDebug("Open chat link: correct chat link");
                        // Identify the link is a meeting or normal chat link
                        megaChatApi.checkChatLink(link, new LoadPreviewListener(ManagerActivity.this, ManagerActivity.this, CHECK_LINK_TYPE_UNKNOWN_LINK));
                        dismissAlertDialogIfExists(openLinkDialog);
                        break;
                    }
                    case CONTACT_LINK: {
                        logDebug("Open contact link: correct contact link");
                        String[] s = link.split("C!");
                        if (s.length > 1) {
                            long handle = MegaApiAndroid.base64ToHandle(s[1].trim());
                            openContactLink(handle);
                            dismissAlertDialogIfExists(openLinkDialog);
                        }
                        break;
                    }
                }
            } else {
                switch (linkType) {
                    case FILE_LINK:
                    case FOLDER_LINK: {
                        logDebug("Do nothing: correct file or folder link");
                        dismissAlertDialogIfExists(openLinkDialog);
                        break;
                    }
                    case CHAT_LINK:
                    case CONTACT_LINK:
                    case ERROR_LINK: {
                        logWarning("Show error: invalid link or correct chat or contact link");
                        showOpenLinkError(true, linkType);
                        break;
                    }
                }
            }
        } else if (drawerItem == DrawerItem.CHAT) {
            megaChatApi.checkChatLink(link, new LoadPreviewListener(ManagerActivity.this, ManagerActivity.this, CHECK_LINK_TYPE_UNKNOWN_LINK));
        }
    }

    /**
     * Shows an Open link dialog.
     */
    private void showOpenLinkDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_error_hint, null);
        builder.setView(v).setPositiveButton(R.string.context_open_link, null)
                .setNegativeButton(R.string.general_cancel, null);

        openLinkText = v.findViewById(R.id.text);

        openLinkText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                showOpenLinkError(false, 0);
            }
        });

        openLinkText.setOnEditorActionListener((v1, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboardView(managerActivity, v1, 0);
                openLink(openLinkText.getText().toString());
                return true;
            }
            return false;
        });

        Util.showKeyboardDelayed(openLinkText);

        openLinkError = v.findViewById(R.id.error);
        openLinkErrorText = v.findViewById(R.id.error_text);

        if (drawerItem == DrawerItem.CLOUD_DRIVE) {
            builder.setTitle(R.string.action_open_link);
            openLinkText.setHint(R.string.hint_paste_link);
        } else if (drawerItem == DrawerItem.CHAT) {
            Fragment fragment = getSupportFragmentManager()
                    .findFragmentByTag(MeetingBottomSheetDialogFragment.TAG);
            if (fragment != null) {
                builder.setTitle(R.string.paste_meeting_link_guest_dialog_title)
                        .setMessage(StringResourcesUtils.getString(
                                R.string.paste_meeting_link_guest_instruction));
                openLinkText.setHint(R.string.meeting_link);
                chatLinkDialogType = LINK_DIALOG_MEETING;
            } else {
                builder.setTitle(R.string.action_open_chat_link);
                openLinkText.setHint(R.string.hint_enter_chat_link);
                chatLinkDialogType = LINK_DIALOG_CHAT;
            }
        }

        openLinkDialog = builder.create();
        openLinkDialog.setCanceledOnTouchOutside(false);

        try {
            openLinkDialog.show();
            openLinkText.requestFocus();

            // Set onClickListeners for buttons after showing the dialog would prevent
            // the dialog from dismissing automatically on clicking the buttons
            openLinkOpenButton = openLinkDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            openLinkOpenButton.setOnClickListener((view) -> {
                hideKeyboard(managerActivity, 0);
                openLink(openLinkText.getText().toString());
            });
            openLinkDialog.setOnKeyListener((dialog, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                    dismissAlertDialogIfExists(openLinkDialog);
                    return true;
                }

                return false;
            });
        } catch (Exception e) {
            logError("Exception showing Open Link dialog", e);
        }
    }

    public void showChatLink(String link) {
        logDebug("Link: " + link);
        Intent openChatLinkIntent = new Intent(this, ChatActivity.class);

        if (joiningToChatLink) {
            openChatLinkIntent.setAction(ACTION_JOIN_OPEN_CHAT_LINK);
            resetJoiningChatLink();
        } else {
            openChatLinkIntent.setAction(ACTION_OPEN_CHAT_LINK);
        }

        openChatLinkIntent.setData(Uri.parse(link));
        startActivity(openChatLinkIntent);

        drawerItem = DrawerItem.CHAT;
        selectDrawerItem(drawerItem);
    }

    /**
     * Initializes the variables to join chat by default.
     */
    private void resetJoiningChatLink() {
        joiningToChatLink = false;
        linkJoinToChatLink = null;
    }

    public void showPresenceStatusDialog() {
        logDebug("showPresenceStatusDialog");

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        final CharSequence[] items = {getString(R.string.online_status), getString(R.string.away_status), getString(R.string.busy_status), getString(R.string.offline_status)};
        int statusToShow = megaChatApi.getOnlineStatus();
        switch (statusToShow) {
            case MegaChatApi.STATUS_ONLINE: {
                statusToShow = 0;
                break;
            }
            case MegaChatApi.STATUS_AWAY: {
                statusToShow = 1;
                break;
            }
            case MegaChatApi.STATUS_BUSY: {
                statusToShow = 2;
                break;
            }
            case MegaChatApi.STATUS_OFFLINE: {
                statusToShow = 3;
                break;
            }
        }
        dialogBuilder.setSingleChoiceItems(items, statusToShow, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {

                presenceStatusDialog.dismiss();
                switch (item) {
                    case 0: {
                        megaChatApi.setOnlineStatus(MegaChatApi.STATUS_ONLINE, managerActivity);
                        break;
                    }
                    case 1: {
                        megaChatApi.setOnlineStatus(MegaChatApi.STATUS_AWAY, managerActivity);
                        break;
                    }
                    case 2: {
                        megaChatApi.setOnlineStatus(MegaChatApi.STATUS_BUSY, managerActivity);
                        break;
                    }
                    case 3: {
                        megaChatApi.setOnlineStatus(MegaChatApi.STATUS_OFFLINE, managerActivity);
                        break;
                    }
                }
            }
        });
        dialogBuilder.setTitle(getString(R.string.status_label));
        presenceStatusDialog = dialogBuilder.create();
        presenceStatusDialog.show();
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
            setTypesCameraPermission(TAKE_PICTURE_OPTION);
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
    public void showNewFolderDialog(String typedText) {
        newFolderDialog = MegaNodeDialogUtil.showNewFolderDialog(this, this, typedText);
    }

    @Override
    public void showNewTextFileDialog(String typedName) {
        newTextFileDialog = MegaNodeDialogUtil.showNewTxtFileDialog(this,
                getCurrentParentNode(getCurrentParentHandle(), INVALID_VALUE), typedName,
                drawerItem == DrawerItem.HOMEPAGE);
    }

    public long getParentHandleBrowser() {
        if (parentHandleBrowser == -1) {
            MegaNode rootNode = megaApi.getRootNode();
            parentHandleBrowser = rootNode != null ? rootNode.getParentHandle() : parentHandleBrowser;
        }

        return parentHandleBrowser;
    }

    private long getCurrentParentHandle() {
        long parentHandle = -1;

        switch (drawerItem) {
            case HOMEPAGE:
                // For home page, its parent is always the root of cloud drive.
                parentHandle = megaApi.getRootNode().getHandle();
                break;
            case CLOUD_DRIVE:
                parentHandle = getParentHandleBrowser();
                break;

            case INBOX:
                parentHandle = parentHandleInbox;
                break;

            case RUBBISH_BIN:
                parentHandle = parentHandleRubbish;
                break;

            case SHARED_ITEMS:
                if (viewPagerShares == null) break;

                if (getTabItemShares() == INCOMING_TAB) {
                    parentHandle = parentHandleIncoming;
                } else if (getTabItemShares() == OUTGOING_TAB) {
                    parentHandle = parentHandleOutgoing;
                } else if (getTabItemShares() == LINKS_TAB) {
                    parentHandle = parentHandleLinks;
                }
                break;

            case SEARCH:
                if (parentHandleSearch != -1) {
                    parentHandle = parentHandleSearch;
                    break;
                }
                switch (searchDrawerItem) {
                    case CLOUD_DRIVE:
                        parentHandle = getParentHandleBrowser();
                        break;
                    case SHARED_ITEMS:
                        if (searchSharedTab == INCOMING_TAB) {
                            parentHandle = parentHandleIncoming;
                        } else if (searchSharedTab == OUTGOING_TAB) {
                            parentHandle = parentHandleOutgoing;
                        } else if (searchSharedTab == LINKS_TAB) {
                            parentHandle = parentHandleLinks;
                        }
                        break;
                    case INBOX:
                        parentHandle = getParentHandleInbox();
                        break;
                }
                break;

            default:
                return parentHandle;
        }

        return parentHandle;
    }

    private MegaNode getCurrentParentNode(long parentHandle, int error) {
        String errorString = null;

        if (error != -1) {
            errorString = getString(error);
        }

        if (parentHandle == -1 && errorString != null) {
            showSnackbar(SNACKBAR_TYPE, errorString, -1);
            logDebug(errorString + ": parentHandle == -1");
            return null;
        }

        MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);

        if (parentNode == null && errorString != null) {
            showSnackbar(SNACKBAR_TYPE, errorString, -1);
            logDebug(errorString + ": parentNode == null");
            return null;
        }

        return parentNode;
    }

    @Override
    public void createFolder(@NotNull String title) {
        logDebug("createFolder");
        if (!isOnline(this)) {
            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
            return;
        }

        if (isFinishing()) {
            return;
        }

        MegaNode parentNode = getCurrentParentNode(getCurrentParentHandle(), R.string.context_folder_no_created);
        if (parentNode == null) return;

        ArrayList<MegaNode> nL = megaApi.getChildren(parentNode);
        for (int i = 0; i < nL.size(); i++) {
            if (title.compareTo(nL.get(i).getName()) == 0) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_already_exists), -1);
                logDebug("Folder not created: folder already exists");
                return;
            }
        }

        statusDialog = createProgressDialog(this, StringResourcesUtils.getString(R.string.context_creating_folder));
        megaApi.createFolder(title, parentNode, this);
    }

    public void showClearRubbishBinDialog() {
        logDebug("showClearRubbishBinDialog");

        rubbishBinFragment = (RubbishBinFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
        if (rubbishBinFragment != null) {
            if (rubbishBinFragment.isVisible()) {
                rubbishBinFragment.notifyDataSetChanged();
            }
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.context_clear_rubbish));
        builder.setMessage(getString(R.string.clear_rubbish_confirmation));
        builder.setPositiveButton(getString(R.string.general_clear),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        nC.cleanRubbishBin();
                    }
                });
        builder.setNegativeButton(getString(android.R.string.cancel), null);
        clearRubbishBinDialog = builder.create();
        clearRubbishBinDialog.show();
    }

    public void chooseAddContactDialog() {
        logDebug("chooseAddContactDialog");
        if (megaApi != null && megaApi.getRootNode() != null) {
            Intent intent = new Intent(this, AddContactActivity.class);
            intent.putExtra("contactType", CONTACT_TYPE_MEGA);
            startActivityForResult(intent, REQUEST_CREATE_CHAT);
        } else {
            logWarning("Online but not megaApi");
            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), MEGACHAT_INVALID_HANDLE);
        }
    }

    /**
     * Method to make appropriate actions when clicking on the FAB button
     */
    public void fabMainClickCallback() {
        if (isFabExpanded) {
            collapseFab();
        } else {
            expandFab();
        }
    }

    private void setupFabs() {
        windowContent = this.getWindow().findViewById(Window.ID_ANDROID_CONTENT);
        fabMaskLayout = FabMaskChatLayoutBinding.inflate(getLayoutInflater(), windowContent, false).getRoot();
        fabMaskButton = fabMaskLayout.findViewById(R.id.fab_main);

        fabs.add(fabMaskLayout.findViewById(R.id.fab_chat));
        fabs.add(fabMaskLayout.findViewById(R.id.fab_meeting));
        fabs.add(fabMaskLayout.findViewById(R.id.text_chat));
        fabs.add(fabMaskLayout.findViewById(R.id.text_meeting));

        fabMaskLayout.setOnClickListener(l -> fabMainClickCallback());
        fabMaskButton.setOnClickListener(l -> fabMainClickCallback());

        fabMaskLayout.findViewById(R.id.fab_chat).setOnClickListener(l -> {
            fabMainClickCallback();
            handler.postDelayed(() -> chooseAddContactDialog(), FAB_MASK_OUT_DELAY);
        });

        fabMaskLayout.findViewById(R.id.text_chat).setOnClickListener(l -> {
            fabMainClickCallback();
            handler.postDelayed(() -> chooseAddContactDialog(), FAB_MASK_OUT_DELAY);
        });

        fabMaskLayout.findViewById(R.id.fab_meeting).setOnClickListener(l -> {
            fabMainClickCallback();
            handler.postDelayed(this::showMeetingOptionsPanel, FAB_MASK_OUT_DELAY);
        });

        fabMaskLayout.findViewById(R.id.text_meeting).setOnClickListener(l -> {
            fabMainClickCallback();
            handler.postDelayed(this::showMeetingOptionsPanel, FAB_MASK_OUT_DELAY);
        });

        if (isFabExpanded) {
            expandFab();
        }
    }

    private void collapseFab() {
        rotateFab(false);
        showOut(fabs);
        // After animation completed, then remove mask.
        handler.postDelayed(() -> {
            removeMask();
            fabButton.setVisibility(View.VISIBLE);
            isFabExpanded = false;
        }, FAB_MASK_OUT_DELAY);
    }

    private void expandFab() {
        fabButton.setVisibility(View.GONE);
        addMask();
        // Need to do so, otherwise, fabMaskMain.background is null.
        handler.post(() -> {
            rotateFab(true);
            showIn(fabs);
            isFabExpanded = true;
        });
    }

    /**
     * Showing the full screen mask by adding the mask layout to the window content
     */
    private void addMask() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.grey_600_085_dark_grey_070));
        windowContent.addView(fabMaskLayout);
    }

    /**
     * Removing the full screen mask
     */
    private void removeMask() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.transparent));
        windowContent.removeView(fabMaskLayout);
    }

    private void rotateFab(boolean isExpand) {
        float rotate = FAB_DEFAULT_ANGEL;
        int color = Color.WHITE;
        int bkColor = ColorUtils.getThemeColor(this, R.attr.colorSecondary);
        if (isExpand) {
            rotate = FAB_ROTATE_ANGEL;
            color = Color.BLACK;
            bkColor = Color.WHITE;
        }

        ObjectAnimator rotateAnim = ObjectAnimator.ofFloat(
                fabMaskButton, "rotation", rotate);


        // The tint of the icon in the middle of the FAB
        ObjectAnimator tintAnim = ObjectAnimator.ofArgb(
                fabMaskButton.getDrawable().mutate(), "tint", color);

        // The background tint of the FAB
        ObjectAnimator backgroundTintAnim = ObjectAnimator.ofArgb(
                fabMaskButton.getBackground().mutate(), "tint", bkColor);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(FAB_ANIM_DURATION);
        animatorSet.playTogether(rotateAnim, backgroundTintAnim, tintAnim);
        animatorSet.start();
    }

    /**
     * Hide the expanded FABs with animated transition
     */
    private void showOut(ArrayList<View> fabs) {
        for (int i = 0; i < fabs.size(); i++) {
            View fab = fabs.get(i);
            fab.animate()
                    .setDuration(FAB_ANIM_DURATION)
                    .translationY(fab.getHeight())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            fab.setVisibility(View.GONE);
                            super.onAnimationEnd(animation);
                        }
                    }).alpha(ALPHA_TRANSPARENT)
                    .start();
        }
    }

    /**
     * Present the expanded FABs with animated transition
     */
    private void showIn(ArrayList<View> fabs) {
        for (int i = 0; i < fabs.size(); i++) {
            View fab = fabs.get(i);
            fab.setVisibility(View.VISIBLE);
            fab.setAlpha(ALPHA_TRANSPARENT);
            fab.setTranslationY(fab.getHeight());

            fab.animate()
                    .setDuration(FAB_ANIM_DURATION)
                    .translationY(0f)
                    .setListener(new AnimatorListenerAdapter() {
                    })
                    .alpha(ALPHA_OPAQUE)
                    .start();
        }
    }

    @Override
    public void onJoinMeeting() {
        MEETING_TYPE = MEETING_ACTION_JOIN;
        if (CallUtil.participatingInACall()) {
            showConfirmationInACall(this, StringResourcesUtils.getString(R.string.text_join_call), passcodeManagement);
        } else {
            showOpenLinkDialog();
        }
    }

    @Override
    public void onCreateMeeting() {
        MEETING_TYPE = MEETING_ACTION_CREATE;
        if (CallUtil.participatingInACall()) {
            showConfirmationInACall(this, StringResourcesUtils.getString(R.string.ongoing_call_content), passcodeManagement);
        } else {
            // For android 12, need android.permission.BLUETOOTH_CONNECT permission
            if (requestBluetoothPermission()) return;
            openMeetingToCreate(this);
        }
    }

    /**
     * Request Bluetooth Connect Permission for Meeting and Call when SDK >= 31
     *
     * @return false : permission granted, needn't request / true: should request permission
     */
    private boolean requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean hasPermission = hasPermissions(this, Manifest.permission.BLUETOOTH_CONNECT);
            if (!hasPermission) {
                requestPermission(this, REQUEST_BT_CONNECT, Manifest.permission.BLUETOOTH_CONNECT);
                return true;
            }
        }
        return false;
    }

    public void showConfirmationRemoveAllSharingContacts(final List<MegaNode> shares) {
        if (shares.size() == 1) {
            showConfirmationRemoveAllSharingContacts(megaApi.getOutShares(shares.get(0)), shares.get(0));
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setMessage(getString(R.string.alert_remove_several_shares, shares.size()))
                .setPositiveButton(R.string.general_remove, (dialog, which) -> nC.removeSeveralFolderShares(shares))
                .setNegativeButton(R.string.general_cancel, (dialog, which) -> {
                })
                .show();
    }

    public void showConfirmationRemoveAllSharingContacts(final ArrayList<MegaShare> shareList, final MegaNode n) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        int size = shareList.size();
        String message = getResources().getQuantityString(R.plurals.confirmation_remove_outgoing_shares, size, size);

        builder.setMessage(message)
                .setPositiveButton(R.string.general_remove, (dialog, which) -> nC.removeShares(shareList, n))
                .setNegativeButton(R.string.general_cancel, (dialog, which) -> {
                })
                .show();
    }

    /**
     * Save nodes to device.
     *
     * @param nodes           nodes to save
     * @param highPriority    whether this download is high priority or not
     * @param isFolderLink    whether this download is a folder link
     * @param fromMediaViewer whether this download is from media viewer
     * @param fromChat        whether this download is from chat
     */
    public void saveNodesToDevice(List<MegaNode> nodes, boolean highPriority, boolean isFolderLink,
                                  boolean fromMediaViewer, boolean fromChat) {
        nodeSaver.saveNodes(nodes, highPriority, isFolderLink, fromMediaViewer, fromChat);
    }

    /**
     * Upon a node is tapped, if it cannot be previewed in-app,
     * then download it first, this download will be marked as "download by tap".
     *
     * @param node Node to be downloaded.
     */
    public Unit saveNodeByTap(MegaNode node) {
        nodeSaver.saveNodes(Collections.singletonList(node), true, false, false, false, true);
        return null;
    }

    /**
     * Save nodes to device.
     *
     * @param handles         handles of nodes to save
     * @param highPriority    whether this download is high priority or not
     * @param isFolderLink    whether this download is a folder link
     * @param fromMediaViewer whether this download is from media viewer
     * @param fromChat        whether this download is from chat
     */
    public void saveHandlesToDevice(List<Long> handles, boolean highPriority, boolean isFolderLink,
                                    boolean fromMediaViewer, boolean fromChat) {
        nodeSaver.saveHandles(handles, highPriority, isFolderLink, fromMediaViewer, fromChat);
    }

    /**
     * Save offline nodes to device.
     *
     * @param nodes nodes to save
     */
    public void saveOfflineNodesToDevice(List<MegaOffline> nodes) {
        nodeSaver.saveOfflineNodes(nodes, false);
    }

    /**
     * Attach node to chats, only used by NodeOptionsBottomSheetDialogFragment.
     *
     * @param node node to attach
     */
    public void attachNodeToChats(MegaNode node) {
        nodeAttacher.attachNode(node);
    }

    /**
     * Attach nodes to chats, used by ActionMode of manager fragments.
     *
     * @param nodes nodes to attach
     */
    public void attachNodesToChats(List<MegaNode> nodes) {
        nodeAttacher.attachNodes(nodes);
    }

    public void showConfirmationRemovePublicLink(final MegaNode n) {
        logDebug("showConfirmationRemovePublicLink");

        if (showTakenDownNodeActionNotAvailableDialog(n, this)) {
            return;
        }

        ArrayList<MegaNode> nodes = new ArrayList<>();
        nodes.add(n);
        showConfirmationRemoveSeveralPublicLinks(nodes);
    }

    public void showConfirmationRemoveSeveralPublicLinks(ArrayList<MegaNode> nodes) {
        if (nodes == null) {
            logWarning("nodes == NULL");
        }

        String message;
        MegaNode node = null;

        if (nodes.size() == 1) {
            node = nodes.get(0);
            message = getResources().getQuantityString(R.plurals.remove_links_warning_text, 1);
        } else {
            message = getResources().getQuantityString(R.plurals.remove_links_warning_text, nodes.size());
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        MegaNode finalNode = node;
        builder.setMessage(message)
                .setPositiveButton(R.string.general_remove, (dialog, which) -> {
                    if (finalNode != null) {
                        if (!isOnline(managerActivity)) {
                            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                            return;
                        }
                        nC.removeLink(finalNode, new ExportListener(managerActivity, 1));
                    } else {
                        nC.removeLinks(nodes);
                    }
                })
                .setNegativeButton(R.string.general_cancel, (dialog, which) -> {
                })
                .show();

        refreshAfterMovingToRubbish();
    }

    @Override
    public void confirmLeaveChat(long chatId) {
        megaChatApi.leaveChat(chatId, new RemoveFromChatRoomListener(this));
    }

    @Override
    public void confirmLeaveChats(@NotNull List<? extends MegaChatListItem> chats) {
        if (getChatsFragment() != null) {
            recentChatsFragment.clearSelections();
            recentChatsFragment.hideMultipleSelect();
        }

        for (MegaChatListItem chat : chats) {
            if (chat != null) {
                megaChatApi.leaveChat(chat.getChatId(), new RemoveFromChatRoomListener(this));
            }
        }
    }

    @Override
    public void leaveChatSuccess() {
        // No update needed.
    }

    public void cameraUploadsClicked() {
        logDebug("cameraUplaodsClicked");
        drawerItem = DrawerItem.PHOTOS;
        setBottomNavigationMenuItemChecked(PHOTOS_BNV);
        selectDrawerItem(drawerItem);
    }

    public void skipInitialCUSetup() {
        setFirstLogin(false);
        drawerItem = getStartDrawerItem(this);
        selectDrawerItem(drawerItem);
    }

    /**
     * Refresh PhotosFragment's UI after CU is enabled.
     */
    public void refreshTimelineFragment() {
        drawerItem = DrawerItem.PHOTOS;
        setBottomNavigationMenuItemChecked(PHOTOS_BNV);
        setToolbarTitle();

        PhotosFragment f = (PhotosFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.PHOTOS.getTag());
        if (f != null) {
            f.refreshViewLayout();
        }
    }

    /**
     * Checks if should update some cu view visibility.
     *
     * @param visibility New requested visibility update.
     * @return True if should apply the visibility update, false otherwise.
     */
    private boolean rightCUVisibilityChange(int visibility) {
        return drawerItem == DrawerItem.PHOTOS || visibility == View.GONE;
    }

    /**
     * Updates cuViewTypes view visibility.
     *
     * @param visibility New visibility value to set.
     */
    public void updateCUViewTypes(int visibility) {
        if (rightCUVisibilityChange(visibility)) {
            cuViewTypes.setVisibility(visibility);
        }
    }

    /**
     * Updates cuLayout view visibility.
     *
     * @param visibility New visibility value to set.
     */
    public void updateCULayout(int visibility) {
        if (rightCUVisibilityChange(visibility)) {
            cuLayout.setVisibility(visibility);
        }
    }

    /**
     * Updates enableCUButton view visibility and cuLayout if needed.
     *
     * @param visibility New visibility value to set.
     */
    public void updateEnableCUButton(int visibility) {
        if (enableCUButton.getVisibility() == visibility) {
            if (enableCUButton.getVisibility() == View.VISIBLE) {
                updateCULayout(visibility);
            }
            return;
        }

        if ((visibility == View.GONE && cuProgressBar.getVisibility() == View.GONE)
                || (visibility == View.VISIBLE && cuLayout.getVisibility() == View.GONE)) {
            updateCULayout(visibility);
        }

        if (rightCUVisibilityChange(visibility)) {
            enableCUButton.setVisibility(visibility);
        }
    }

    /**
     * Hides the CU progress bar.
     */
    public void hideCUProgress() {
        cuProgressBar.setVisibility(View.GONE);
    }

    /**
     * Updates the CU progress view.
     *
     * @param progress The current progress.
     * @param pending  The number of pending uploads.
     */
    public void updateCUProgress(int progress, int pending) {
        if (drawerItem != DrawerItem.PHOTOS || getPhotosFragment() == null
                || !photosFragment.shouldShowFullInfoAndOptions()) {
            return;
        }

        boolean visible = pending > 0;
        int visibility = visible ? View.VISIBLE : View.GONE;

        if ((!visible && enableCUButton.getVisibility() == View.GONE)
                || (visible && cuLayout.getVisibility() == View.GONE)) {
            updateCULayout(visibility);
        }

        if (getPhotosFragment() != null) {
            photosFragment.updateProgress(visibility, pending);
        }

        if (cuProgressBar.getVisibility() != visibility) {
            cuProgressBar.setVisibility(visibility);
        }

        cuProgressBar.setProgress(progress);
    }

    /**
     * Shows or hides the cuLayout and animates the transition.
     *
     * @param hide True if should hide it, false if should show it.
     */
    public void animateCULayout(boolean hide) {
        boolean visible = cuLayout.getVisibility() == View.VISIBLE;
        if ((hide && !visible) || !hide && visible) {
            return;
        }

        if (hide) {
            cuLayout.animate().translationY(-100).setDuration(ANIMATION_DURATION)
                    .withEndAction(() -> cuLayout.setVisibility(View.GONE)).start();
        } else if (drawerItem == DrawerItem.PHOTOS) {
            cuLayout.setVisibility(View.VISIBLE);
            cuLayout.animate().translationY(0).setDuration(ANIMATION_DURATION).start();
        }
    }

    /**
     * Shows the bottom sheet to manage a completed transfer.
     *
     * @param transfer the completed transfer to manage.
     */
    public void showManageTransferOptionsPanel(AndroidCompletedTransfer transfer) {
        if (transfer == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedTransfer = transfer;
        bottomSheetDialogFragment = new ManageTransferBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void showNodeOptionsPanel(MegaNode node) {
        showNodeOptionsPanel(node, NodeOptionsBottomSheetDialogFragment.DEFAULT_MODE);
    }

    public void showNodeOptionsPanel(MegaNode node, int mode) {
        logDebug("showNodeOptionsPanel");

        if (node == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedNode = node;
        bottomSheetDialogFragment = new NodeOptionsBottomSheetDialogFragment(mode);
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void showNodeLabelsPanel(@NonNull MegaNode node) {
        logDebug("showNodeLabelsPanel");

        if (isBottomSheetDialogShown(bottomSheetDialogFragment)) {
            bottomSheetDialogFragment.dismiss();
        }

        selectedNode = node;
        bottomSheetDialogFragment = NodeLabelBottomSheetDialogFragment.newInstance(node.getHandle());
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void showOptionsPanel(MegaOffline sNode) {
        logDebug("showNodeOptionsPanel-Offline");

        if (sNode == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedOfflineNode = sNode;
        bottomSheetDialogFragment = new OfflineOptionsBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void showNewSortByPanel(int orderType) {
        if (isBottomSheetDialogShown(bottomSheetDialogFragment)) {
            return;
        }

        if (orderType == ORDER_OTHERS && deepBrowserTreeIncoming > 0) {
            orderType = ORDER_CLOUD;
        }

        bottomSheetDialogFragment = SortByBottomSheetDialogFragment.newInstance(orderType);

        bottomSheetDialogFragment.show(getSupportFragmentManager(),
                bottomSheetDialogFragment.getTag());
    }

    public void showOfflineFileInfo(MegaOffline node) {
        Intent intent = new Intent(this, OfflineFileInfoActivity.class);
        intent.putExtra(HANDLE, node.getHandle());
        startActivity(intent);
    }

    public void showMeetingOptionsPanel() {
        if (CallUtil.participatingInACall()) {
            showConfirmationInACall(this, StringResourcesUtils.getString(R.string.ongoing_call_content), passcodeManagement);
        } else {
            bottomSheetDialogFragment = new MeetingBottomSheetDialogFragment();
            bottomSheetDialogFragment.show(getSupportFragmentManager(), MeetingBottomSheetDialogFragment.TAG);
        }
    }

    /**
     * Shows the GENERAL_UPLOAD upload bottom sheet fragment.
     */
    public void showUploadPanel() {
        showUploadPanel(drawerItem == DrawerItem.HOMEPAGE ? HOMEPAGE_UPLOAD : GENERAL_UPLOAD);
    }

    /**
     * Shows the upload bottom sheet fragment taking into account the upload type received as param.
     *
     * @param uploadType Indicates the type of upload:
     *                   - GENERAL_UPLOAD if nothing special has to be taken into account.
     *                   - DOCUMENTS_UPLOAD if an upload from Documents section.
     */
    public void showUploadPanel(int uploadType) {
        if (!hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermission(this, REQUEST_READ_WRITE_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
            return;
        }

        if (isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        bottomSheetDialogFragment = UploadBottomSheetDialogFragment.newInstance(uploadType);
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    /**
     * Shows the upload bottom sheet fragment taking into account the upload type received as param.
     *
     * @param uploadType Indicates the type of upload:
     *                   - GENERAL_UPLOAD if nothing special has to be taken into account.
     *                   - DOCUMENTS_UPLOAD if an upload from Documents section.
     * @param actionType Indicates the action to backup folder or file (move, remove, add, create etc.)
     */
    public void showUploadPanelForBackup(int uploadType, int actionType) {
        if (!hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermission(this, REQUEST_READ_WRITE_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
            return;
        }

        // isInBackup Indicates if the current node is under "My backup"
        if (fileBackupManager.fabForBackup(fileBrowserFragment.getNodeList(), getCurrentParentNode(getCurrentParentHandle(), INVALID_VALUE), actionType)) {
            return;
        }

        showUploadPanel(uploadType);
    }

    public void updateAccountDetailsVisibleInfo() {
        logDebug("updateAccountDetailsVisibleInfo");
        if (isFinishing()) {
            return;
        }

        View settingsSeparator = null;

        if (nV != null) {
            settingsSeparator = nV.findViewById(R.id.settings_separator);
        }

        if (usedSpaceLayout != null) {
            if (megaApi.isBusinessAccount()) {
                usedSpaceLayout.setVisibility(View.GONE);
                upgradeAccount.setVisibility(View.GONE);
                if (settingsSeparator != null) {
                    settingsSeparator.setVisibility(View.GONE);
                }
                if (megaApi.isBusinessAccount()) {
                    businessLabel.setVisibility(View.VISIBLE);
                }

            } else {
                businessLabel.setVisibility(View.GONE);
                upgradeAccount.setVisibility(View.VISIBLE);
                if (settingsSeparator != null) {
                    settingsSeparator.setVisibility(View.GONE);
                }

                String textToShow = String.format(getResources().getString(R.string.used_space), myAccountInfo.getUsedFormatted(), myAccountInfo.getTotalFormatted());
                String colorString = ColorUtils.getThemeColorHexString(this, R.attr.colorSecondary);
                switch (storageState) {
                    case MegaApiJava.STORAGE_STATE_GREEN:
                        break;
                    case MegaApiJava.STORAGE_STATE_ORANGE:
                        colorString = ColorUtils.getColorHexString(this, R.color.amber_600_amber_300);
                        break;
                    case MegaApiJava.STORAGE_STATE_RED:
                    case MegaApiJava.STORAGE_STATE_PAYWALL:
                        myAccountInfo.setUsedPercentage(100);
                        colorString = ColorUtils.getColorHexString(this, R.color.red_600_red_300);
                        break;
                }

                try {
                    textToShow = textToShow.replace("[A]", "<font color=\'"
                            + colorString
                            + "\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'"
                            + ColorUtils.getThemeColorHexString(this, android.R.attr.textColorPrimary)
                            + "\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                } catch (Exception e) {
                    logWarning("Exception formatting string", e);
                }
                spaceTV.setText(HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY));
                int progress = myAccountInfo.getUsedPercentage();
                long usedSpace = myAccountInfo.getUsedStorage();
                logDebug("Progress: " + progress + ", Used space: " + usedSpace);
                usedSpacePB.setProgress(progress);
                if (progress >= 0 && usedSpace >= 0) {
                    usedSpaceLayout.setVisibility(View.VISIBLE);
                } else {
                    usedSpaceLayout.setVisibility(View.GONE);
                }
            }
        } else {
            logWarning("usedSpaceLayout is NULL");
        }

        updateSubscriptionLevel(myAccountInfo, dbH, megaApi);

        int resId = R.drawable.custom_progress_bar_horizontal_ok;
        switch (storageState) {
            case MegaApiJava.STORAGE_STATE_GREEN:
                break;
            case MegaApiJava.STORAGE_STATE_ORANGE:
                resId = R.drawable.custom_progress_bar_horizontal_warning;
                break;
            case MegaApiJava.STORAGE_STATE_RED:
            case MegaApiJava.STORAGE_STATE_PAYWALL:
                myAccountInfo.setUsedPercentage(100);
                resId = R.drawable.custom_progress_bar_horizontal_exceed;
                break;
        }
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), resId, null);
        usedSpacePB.setProgressDrawable(drawable);
    }

    public void refreshCloudDrive() {
        if (rootNode == null) {
            rootNode = megaApi.getRootNode();
        }

        if (rootNode == null) {
            logWarning("Root node is NULL. Maybe user is not logged in");
            return;
        }

        MegaNode parentNode = rootNode;

        if (isCloudAdded()) {
            ArrayList<MegaNode> nodes;
            if (parentHandleBrowser == -1) {
                nodes = megaApi.getChildren(parentNode, sortOrderManagement.getOrderCloud());
            } else {
                parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
                if (parentNode == null) return;

                nodes = megaApi.getChildren(parentNode, sortOrderManagement.getOrderCloud());
            }
            logDebug("Nodes: " + nodes.size());
            fileBrowserFragment.hideMultipleSelect();
            fileBrowserFragment.setNodes(nodes);
            fileBrowserFragment.getRecyclerView().invalidate();
        }
    }

    private void refreshSharesPageAdapter() {
        if (sharesPageAdapter != null) {
            sharesPageAdapter.notifyDataSetChanged();
            setSharesTabIcons(getTabItemShares());
        }
    }

    public void refreshCloudOrder(int order) {
        //Refresh Cloud Fragment
        refreshCloudDrive();

        //Refresh Rubbish Fragment
        refreshRubbishBin();

        onNodesSharedUpdate();

        if (getInboxFragment() != null) {
            MegaNode inboxNode = megaApi.getInboxNode();
            if (inboxNode != null) {
                ArrayList<MegaNode> nodes = megaApi.getChildren(inboxNode, order);
                inboxFragment.setNodes(nodes);
                inboxFragment.getRecyclerView().invalidate();
            }
        }

        refreshSearch();
    }

    public void refreshOthersOrder() {
        refreshSharesPageAdapter();
        refreshSearch();
    }

    public void refreshCUNodes() {
        if (getPhotosFragment() != null) {
            photosFragment.loadPhotos();
        }
    }

    public void setFirstNavigationLevel(boolean firstNavigationLevel) {
        logDebug("Set value to: " + firstNavigationLevel);
        this.firstNavigationLevel = firstNavigationLevel;
    }

    public boolean isFirstNavigationLevel() {
        return firstNavigationLevel;
    }

    public void setParentHandleBrowser(long parentHandleBrowser) {
        logDebug("Set value to:" + parentHandleBrowser);

        this.parentHandleBrowser = parentHandleBrowser;
    }

    public void setParentHandleRubbish(long parentHandleRubbish) {
        logDebug("setParentHandleRubbish");
        this.parentHandleRubbish = parentHandleRubbish;
    }

    public void setParentHandleSearch(long parentHandleSearch) {
        logDebug("setParentHandleSearch");
        this.parentHandleSearch = parentHandleSearch;
    }

    public void setParentHandleIncoming(long parentHandleIncoming) {
        logDebug("setParentHandleIncoming: " + parentHandleIncoming);
        this.parentHandleIncoming = parentHandleIncoming;
    }

    public void setParentHandleInbox(long parentHandleInbox) {
        logDebug("setParentHandleInbox: " + parentHandleInbox);
        this.parentHandleInbox = parentHandleInbox;
    }

    public void setParentHandleOutgoing(long parentHandleOutgoing) {
        logDebug("Outgoing parent handle: " + parentHandleOutgoing);
        this.parentHandleOutgoing = parentHandleOutgoing;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        logDebug("onNewIntent");

        if (intent != null) {
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                searchQuery = intent.getStringExtra(SearchManager.QUERY);
                parentHandleSearch = -1;
                setToolbarTitle();
                isSearching = true;

                if (searchMenuItem != null) {
                    MenuItemCompat.collapseActionView(searchMenuItem);
                }
                return;
            } else if (ACTION_SHOW_UPGRADE_ACCOUNT.equals(intent.getAction())) {
                navigateToUpgradeAccount();
                return;
            } else if (ACTION_SHOW_TRANSFERS.equals(intent.getAction())) {
                if (intent.getBooleanExtra(OPENED_FROM_CHAT, false)) {
                    sendBroadcast(new Intent(ACTION_CLOSE_CHAT_AFTER_OPEN_TRANSFERS));
                }

                drawerItem = DrawerItem.TRANSFERS;
                indexTransfers = intent.getIntExtra(TRANSFERS_TAB, ERROR_TAB);
                selectDrawerItem(drawerItem);
                return;
            }

        }
        super.onNewIntent(intent);
        setIntent(intent);
    }

    public void navigateToUpgradeAccount() {
        if (nV != null && drawerLayout != null && drawerLayout.isDrawerOpen(nV)) {
            drawerLayout.closeDrawer(Gravity.LEFT);
        }

        startActivity(new Intent(this, UpgradeAccountActivity.class));
        myAccountInfo.setUpgradeOpenedFrom(MyAccountInfo.UpgradeFrom.MANAGER);
    }

    public void navigateToAchievements() {
        logDebug("navigateToAchievements");
        getProLayout.setVisibility(View.GONE);
        showMyAccount(ACTION_OPEN_ACHIEVEMENTS, null, null);
    }

    public void navigateToContacts() {
        drawerLayout.closeDrawer(Gravity.LEFT);
        startActivity(ContactsActivity.getListIntent(this));
    }

    public void navigateToContactRequests() {
        drawerLayout.closeDrawer(Gravity.LEFT);
        startActivity(ContactsActivity.getReceivedRequestsIntent(this));
    }

    public void navigateToMyAccount() {
        logDebug("navigateToMyAccount");
        getProLayout.setVisibility(View.GONE);
        showMyAccount();
    }

    @Override
    public void onClick(View v) {
        logDebug("onClick");

        DrawerItem oldDrawerItem = drawerItem;
        boolean sectionClicked = false;

        switch (v.getId()) {
            case R.id.navigation_drawer_add_phone_number_button: {
                Intent intent = new Intent(this, SMSVerificationActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.btnLeft_cancel: {
                getProLayout.setVisibility(View.GONE);
                break;
            }
            case R.id.btnRight_upgrade: {
                //Add navigation to Upgrade Account
                logDebug("Click on Upgrade in pro panel!");
                navigateToUpgradeAccount();
                break;
            }
            case R.id.enable_2fa_button: {
                if (enable2FADialog != null) {
                    enable2FADialog.dismiss();
                }
                isEnable2FADialogShown = false;
                Intent intent = new Intent(this, TwoFactorAuthenticationActivity.class);
                intent.putExtra(EXTRA_NEW_ACCOUNT, true);
                startActivity(intent);
                break;
            }
            case R.id.skip_enable_2fa_button: {
                isEnable2FADialogShown = false;
                if (enable2FADialog != null) {
                    enable2FADialog.dismiss();
                }
                break;
            }
            case R.id.navigation_drawer_account_section:
            case R.id.my_account_section: {
                if (isOnline(this) && megaApi.getRootNode() != null) {
                    showMyAccount();
                }
                break;
            }
            case R.id.inbox_section: {
                sectionClicked = true;
                drawerItem = DrawerItem.INBOX;
                break;
            }
            case R.id.contacts_section: {
                navigateToContacts();
                break;
            }
            case R.id.notifications_section: {
                sectionClicked = true;
                drawerItem = DrawerItem.NOTIFICATIONS;
                break;
            }
            case R.id.offline_section: {
                sectionClicked = true;
                bottomItemBeforeOpenFullscreenOffline = bottomNavigationCurrentItem;
                openFullscreenOfflineFragment(getPathNavigationOffline());
                break;
            }
            case R.id.transfers_section:
                sectionClicked = true;
                drawerItem = DrawerItem.TRANSFERS;
                break;

            case R.id.rubbish_bin_section:
                sectionClicked = true;
                drawerItem = DrawerItem.RUBBISH_BIN;
                break;

            case R.id.settings_section: {
                navigateToSettingsActivity(null);
                break;
            }
            case R.id.upgrade_navigation_view: {
                navigateToUpgradeAccount();
                break;
            }
            case R.id.lost_authentication_device: {
                try {
                    Intent openTermsIntent = new Intent(this, WebViewActivity.class);
                    openTermsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    openTermsIntent.setData(Uri.parse(RECOVERY_URL));
                    startActivity(openTermsIntent);
                } catch (Exception e) {
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                    viewIntent.setData(Uri.parse(RECOVERY_URL));
                    startActivity(viewIntent);
                }
                break;
            }

            case R.id.call_in_progress_layout: {
                returnCallWithPermissions();
                break;
            }
        }

        if (sectionClicked) {
            isFirstTimeCam();
            checkIfShouldCloseSearchView(oldDrawerItem);
            selectDrawerItem(drawerItem);
        }
    }

    void exportRecoveryKey() {
        AccountController.saveRkToFileSystem(this);
    }

    public void showConfirmationRemoveFromOffline(MegaOffline node, Runnable onConfirmed) {
        logDebug("showConfirmationRemoveFromOffline");

        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.confirmation_delete_from_save_for_offline)
                .setPositiveButton(R.string.general_remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeOffline(node, dbH, managerActivity);
                        onConfirmed.run();
                        refreshOfflineNodes();

                        if (isCloudAdded()) {
                            String handle = node.getHandle();
                            if (handle != null && !handle.equals("")) {
                                fileBrowserFragment.refresh(Long.parseLong(handle));
                            }
                        }

                        onNodesSharedUpdate();
                        LiveEventBus.get(EVENT_NODES_CHANGE).post(false);
                        Util.showSnackbar(ManagerActivity.this,
                                getResources().getString(R.string.file_removed_offline));
                    }
                })
                .setNegativeButton(R.string.general_cancel, null)
                .show();
    }

    public void showConfirmationRemoveSomeFromOffline(List<MegaOffline> documents,
                                                      Runnable onConfirmed) {
        logDebug("showConfirmationRemoveSomeFromOffline");

        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.confirmation_delete_from_save_for_offline)
                .setPositiveButton(R.string.general_remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (MegaOffline node : documents) {
                            removeOffline(node, dbH, managerActivity);
                        }

                        refreshOfflineNodes();
                        onConfirmed.run();
                    }
                })
                .setNegativeButton(R.string.general_cancel, null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        logDebug("Request code: " + requestCode + ", Result code:" + resultCode);

        if (nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)) {
            return;
        }

        if (nodeAttacher.handleActivityResult(requestCode, resultCode, intent, this)) {
            return;
        }

        if (resultCode == RESULT_FIRST_USER) {
            showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_destination_folder), -1);
            return;
        }

        if (requestCode == REQUEST_CODE_GET_FILES && resultCode == RESULT_OK) {
            if (intent == null) {
                logWarning("Intent NULL");
                return;
            }

            logDebug("Intent action: " + intent.getAction());
            logDebug("Intent type: " + intent.getType());

            intent.setAction(Intent.ACTION_GET_CONTENT);
            processFileDialog = showProcessFileDialog(this, intent);

            filePrepareUseCase.prepareFiles(intent)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((shareInfo, throwable) -> {
                        if (throwable == null) {
                            onIntentProcessed(shareInfo);
                        }
                    });
        } else if (requestCode == REQUEST_CODE_GET_FOLDER) {
            getFolder(this, resultCode, intent, getCurrentParentHandle());
        } else if (requestCode == REQUEST_CODE_GET_FOLDER_CONTENT) {
            UploadUtil.uploadFolder(this, resultCode, intent);
        } else if (requestCode == WRITE_SD_CARD_REQUEST_CODE && resultCode == RESULT_OK) {

            if (!hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                requestPermission(this,
                        REQUEST_WRITE_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }

            if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
                showOverDiskQuotaPaywallWarning();
                return;
            }

            Uri treeUri = intent.getData();
            logDebug("Create the document : " + treeUri);
            long handleToDownload = intent.getLongExtra("handleToDownload", -1);
            logDebug("The recovered handle is: " + handleToDownload);
            //Now, call to the DownloadService

            if (handleToDownload != 0 && handleToDownload != -1) {
                Intent service = new Intent(this, DownloadService.class);
                service.putExtra(DownloadService.EXTRA_HASH, handleToDownload);
                service.putExtra(DownloadService.EXTRA_CONTENT_URI, treeUri.toString());
                File tempFolder = CacheFolderManager.getCacheFolder(this, CacheFolderManager.TEMPORARY_FOLDER);
                if (!isFileAvailable(tempFolder)) {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error), -1);
                    return;
                }
                service.putExtra(DownloadService.EXTRA_PATH, tempFolder.getAbsolutePath());
                startService(service);
            }
        } else if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {
            logDebug("requestCode == REQUEST_CODE_SELECT_FILE");

            if (intent == null) {
                logWarning("Intent NULL");
                return;
            }

            nodeAttacher.handleSelectFileResult(intent, this);
        } else if (requestCode == REQUEST_CODE_SELECT_FOLDER && resultCode == RESULT_OK) {
            logDebug("REQUEST_CODE_SELECT_FOLDER");

            if (intent == null) {
                logDebug("Intent NULL");
                return;
            }

            final ArrayList<String> selectedContacts = intent.getStringArrayListExtra(SELECTED_CONTACTS);
            final long folderHandle = intent.getLongExtra("SELECT", 0);

            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
            dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
            final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
            dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    permissionsDialog.dismiss();
                    nC.shareFolder(megaApi.getNodeByHandle(folderHandle), selectedContacts, item);
                }
            });
            dialogBuilder.setTitle(getString(R.string.dialog_select_permissions));
            permissionsDialog = dialogBuilder.create();
            permissionsDialog.show();

        } else if (requestCode == REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK) {
            logDebug("onActivityResult REQUEST_CODE_SELECT_CONTACT OK");

            if (intent == null) {
                logWarning("Intent NULL");
                return;
            }

            final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS);
            megaContacts = intent.getBooleanExtra(AddContactActivity.EXTRA_MEGA_CONTACTS, true);

            final int multiselectIntent = intent.getIntExtra("MULTISELECT", -1);

            if (multiselectIntent == 0) {
                //One file to share
                final long nodeHandle = intent.getLongExtra(AddContactActivity.EXTRA_NODE_HANDLE, -1);

                if (fileBackupManager.shareFolder(nC, new long[]{nodeHandle}, contactsData, ACCESS_READ)) {
                    return;
                }

                MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
                dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
                final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
                dialogBuilder.setSingleChoiceItems(items, -1, (dialog, item) -> {
                    permissionsDialog.dismiss();
                    nC.shareFolder(megaApi.getNodeByHandle(nodeHandle), contactsData, item);
                });
                dialogBuilder.setTitle(getString(R.string.dialog_select_permissions));
                permissionsDialog = dialogBuilder.create();
                permissionsDialog.show();
            } else if (multiselectIntent == 1) {
                //Several folders to share
                final long[] nodeHandles = intent.getLongArrayExtra(AddContactActivity.EXTRA_NODE_HANDLE);

                if (fileBackupManager.shareFolder(nC, nodeHandles, contactsData, ACCESS_READ)) {
                    return;
                }

                MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
                dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
                final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
                dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        permissionsDialog.dismiss();
                        nC.shareFolders(nodeHandles, contactsData, item);
                    }
                });
                dialogBuilder.setTitle(getString(R.string.dialog_select_permissions));
                permissionsDialog = dialogBuilder.create();
                permissionsDialog.show();
            }
        } else if (requestCode == REQUEST_CODE_GET_LOCAL && resultCode == RESULT_OK) {

            if (intent == null) {
                logDebug("Intent NULL");
                return;
            }

            String folderPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
            ArrayList<String> paths = intent.getStringArrayListExtra(FileStorageActivity.EXTRA_FILES);

            UploadServiceTask uploadServiceTask = new UploadServiceTask(folderPath, paths, getCurrentParentHandle());
            uploadServiceTask.start();
        } else if (requestCode == REQUEST_CODE_SELECT_FOLDER_TO_MOVE && resultCode == RESULT_OK) {

            if (intent == null) {
                logDebug("Intent NULL");
                return;
            }

            final long[] moveHandles = intent.getLongArrayExtra("MOVE_HANDLES");
            final long toHandle = intent.getLongExtra("MOVE_TO", 0);

            if (fileBackupManager.moveToBackup(moveNodeUseCase, moveHandles, toHandle)) {
                return;
            }

            moveNodeUseCase.move(moveHandles, toHandle)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((result, throwable) -> {
                        if (throwable == null) {
                            showMovementResult(result, moveHandles[0]);
                        }
                    });

        } else if (requestCode == REQUEST_CODE_SELECT_FOLDER_TO_COPY && resultCode == RESULT_OK) {
            logDebug("REQUEST_CODE_SELECT_COPY_FOLDER");

            if (intent == null) {
                logWarning("Intent NULL");
                return;
            }
            final long[] copyHandles = intent.getLongArrayExtra("COPY_HANDLES");
            final long toHandle = intent.getLongExtra("COPY_TO", 0);

            if (fileBackupManager.copyNodesToBackups(nC, copyHandles, toHandle)) {
                return;
            }

            nC.copyNodes(copyHandles, toHandle);
        } else if (requestCode == REQUEST_CODE_REFRESH_API_SERVER && resultCode == RESULT_OK) {
            logDebug("Resfresh DONE");

            if (intent == null) {
                logWarning("Intent NULL");
                return;
            }

            ((MegaApplication) getApplication()).askForFullAccountInfo();
            ((MegaApplication) getApplication()).askForExtendedAccountDetails();

            if (drawerItem == DrawerItem.CLOUD_DRIVE) {
                parentHandleBrowser = intent.getLongExtra("PARENT_HANDLE", -1);
                MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);

                ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode != null
                                ? parentNode
                                : megaApi.getRootNode(),
                        sortOrderManagement.getOrderCloud());

                fileBrowserFragment.setNodes(nodes);
                fileBrowserFragment.getRecyclerView().invalidate();
            } else if (drawerItem == DrawerItem.SHARED_ITEMS) {
                refreshIncomingShares();
            }

        } else if (requestCode == TAKE_PHOTO_CODE) {
            logDebug("TAKE_PHOTO_CODE");
            if (resultCode == Activity.RESULT_OK) {
                uploadTakePicture(this, getCurrentParentHandle(), megaApi);
            } else {
                logWarning("TAKE_PHOTO_CODE--->ERROR!");
            }
        } else if (requestCode == REQUEST_CODE_SORT_BY && resultCode == RESULT_OK) {

            if (intent == null) {
                logWarning("Intent NULL");
                return;
            }

            int orderGetChildren = intent.getIntExtra("ORDER_GET_CHILDREN", 1);
            if (drawerItem == DrawerItem.CLOUD_DRIVE) {
                MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
                if (parentNode != null) {
                    if (isCloudAdded()) {
                        ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderGetChildren);
                        fileBrowserFragment.setNodes(nodes);
                        fileBrowserFragment.getRecyclerView().invalidate();
                    }
                } else {
                    if (isCloudAdded()) {
                        ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRootNode(), orderGetChildren);
                        fileBrowserFragment.setNodes(nodes);
                        fileBrowserFragment.getRecyclerView().invalidate();
                    }
                }
            } else if (drawerItem == DrawerItem.SHARED_ITEMS) {
                onNodesSharedUpdate();
            }
        } else if (requestCode == REQUEST_CREATE_CHAT && resultCode == RESULT_OK) {
            logDebug("REQUEST_CREATE_CHAT OK");

            if (intent == null) {
                logWarning("Intent NULL");
                return;
            }
            boolean isMeeting = intent.getBooleanExtra(AddContactActivity.EXTRA_MEETING, false);
            if (isMeeting) {
                handler.post(() -> showMeetingOptionsPanel());
                return;
            }
            final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS);

            final boolean isGroup = intent.getBooleanExtra(AddContactActivity.EXTRA_GROUP_CHAT, false);

            if (contactsData != null) {
                if (!isGroup) {
                    logDebug("Create one to one chat");
                    MegaUser user = megaApi.getContact(contactsData.get(0));
                    if (user != null) {
                        logDebug("Chat with contact: " + contactsData.size());
                        startOneToOneChat(user);
                    }
                } else {
                    logDebug("Create GROUP chat");
                    MegaChatPeerList peers = MegaChatPeerList.createInstance();
                    for (int i = 0; i < contactsData.size(); i++) {
                        MegaUser user = megaApi.getContact(contactsData.get(i));
                        if (user != null) {
                            peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
                        }
                    }
                    final String chatTitle = intent.getStringExtra(AddContactActivity.EXTRA_CHAT_TITLE);
                    boolean isEKR = intent.getBooleanExtra(AddContactActivity.EXTRA_EKR, false);
                    boolean chatLink = false;
                    if (!isEKR) {
                        chatLink = intent.getBooleanExtra(AddContactActivity.EXTRA_CHAT_LINK, false);
                    }

                    createGroupChat(peers, chatTitle, chatLink, isEKR);
                }
            }
        } else if (requestCode == REQUEST_INVITE_CONTACT_FROM_DEVICE && resultCode == RESULT_OK) {
            logDebug("REQUEST_INVITE_CONTACT_FROM_DEVICE OK");

            if (intent == null) {
                logWarning("Intent NULL");
                return;
            }

            final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS);
            megaContacts = intent.getBooleanExtra(AddContactActivity.EXTRA_MEGA_CONTACTS, true);

            if (contactsData != null) {
                cC.inviteMultipleContacts(contactsData);
            }
        } else if (requestCode == REQUEST_DOWNLOAD_FOLDER && resultCode == RESULT_OK) {
            String parentPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);

            if (parentPath != null) {
                String path = parentPath + File.separator + getRecoveryKeyFileName();

                logDebug("REQUEST_DOWNLOAD_FOLDER:path to download: " + path);
                AccountController ac = new AccountController(this);
                ac.exportMK(path);
            }
        } else if (requestCode == REQUEST_CODE_FILE_INFO && resultCode == RESULT_OK) {
            if (isCloudAdded()) {
                long handle = intent.getLongExtra(NODE_HANDLE, -1);
                fileBrowserFragment.refresh(handle);
            }

            onNodesSharedUpdate();
        } else if (requestCode == REQUEST_CODE_SCAN_DOCUMENT) {
            if (resultCode == RESULT_OK) {
                String savedDestination = intent.getStringExtra(DocumentScannerActivity.EXTRA_PICKED_SAVE_DESTINATION);
                Intent fileIntent = new Intent(this, FileExplorerActivity.class);
                if (StringResourcesUtils.getString(R.string.section_chat).equals(savedDestination)) {
                    fileIntent.setAction(FileExplorerActivity.ACTION_UPLOAD_TO_CHAT);
                } else {
                    fileIntent.setAction(FileExplorerActivity.ACTION_SAVE_TO_CLOUD);
                    fileIntent.putExtra(FileExplorerActivity.EXTRA_PARENT_HANDLE, getCurrentParentHandle());
                }
                fileIntent.putExtra(Intent.EXTRA_STREAM, intent.getData());
                fileIntent.setType(intent.getType());
                startActivity(fileIntent);
            }
        } else if (requestCode == REQUEST_WRITE_STORAGE || requestCode == REQUEST_READ_WRITE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                switch (requestCode) {
                    case REQUEST_WRITE_STORAGE:
                        // Take picture scenarios
                        if (typesCameraPermission == TAKE_PICTURE_OPTION) {
                            if (!hasPermissions(this, Manifest.permission.CAMERA)) {
                                requestPermission(this, REQUEST_CAMERA, Manifest.permission.CAMERA);
                            } else {
                                checkTakePicture(this, TAKE_PHOTO_CODE);
                                typesCameraPermission = INVALID_TYPE_PERMISSIONS;
                            }
                            break;
                        }

                        // General download scenario
                        nodeSaver.handleRequestPermissionsResult(requestCode);
                        break;

                    case REQUEST_READ_WRITE_STORAGE:
                        // Upload scenario
                        new Handler(Looper.getMainLooper()).post(this::showUploadPanel);
                        break;
                }
            }
        } else if (requestCode == REQUEST_CODE_DELETE_VERSIONS_HISTORY && resultCode == RESULT_OK) {
            if (!isOnline(this)) {
                Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
                return;
            }
            if (intent.getBooleanExtra(VersionsFileActivity.KEY_DELETE_VERSION_HISTORY, false)) {
                MegaNode node = megaApi.getNodeByHandle(intent.getLongExtra(VersionsFileActivity.KEY_DELETE_NODE_HANDLE, 0));
                ArrayList<MegaNode> versions = megaApi.getVersions(node);
                versionsToRemove = versions.size() - 1;
                for (int i = 1; i < versions.size(); i++) {
                    megaApi.removeVersion(versions.get(i), this);
                }
            }
        } else {
            logWarning("No request code processed");
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    public void createGroupChat(MegaChatPeerList peers, String chatTitle, boolean chatLink, boolean isEKR) {

        logDebug("Create group chat with participants: " + peers.size());

        if (isEKR) {
            megaChatApi.createChat(true, peers, chatTitle, this);
        } else {
            if (chatLink) {
                if (chatTitle != null && !chatTitle.isEmpty()) {
                    CreateGroupChatWithPublicLink listener = new CreateGroupChatWithPublicLink(this, chatTitle);
                    megaChatApi.createPublicChat(peers, chatTitle, listener);
                } else {
                    showAlert(this, getString(R.string.message_error_set_title_get_link), null);
                }
            } else {
                megaChatApi.createPublicChat(peers, chatTitle, this);
            }
        }
    }

    public void startOneToOneChat(MegaUser user) {
        logDebug("User Handle: " + user.getHandle());
        MegaChatRoom chat = megaChatApi.getChatRoomByUser(user.getHandle());
        MegaChatPeerList peers = MegaChatPeerList.createInstance();
        if (chat == null) {
            logDebug("No chat, create it!");
            peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
            megaChatApi.createChat(false, peers, this);
        } else {
            logDebug("There is already a chat, open it!");
            Intent intentOpenChat = new Intent(this, ChatActivity.class);
            intentOpenChat.setAction(ACTION_CHAT_SHOW_MESSAGES);
            intentOpenChat.putExtra(CHAT_ID, chat.getChatId());
            this.startActivity(intentOpenChat);
        }
    }

    /*
     * Background task to get files on a folder for uploading
     */
    private class UploadServiceTask extends Thread {

        String folderPath;
        ArrayList<String> paths;
        long parentHandle;

        UploadServiceTask(String folderPath, ArrayList<String> paths, long parentHandle) {
            this.folderPath = folderPath;
            this.paths = paths;
            this.parentHandle = parentHandle;
        }

        @Override
        public void run() {

            logDebug("Run Upload Service Task");

            MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
            if (parentNode == null) {
                parentNode = megaApi.getRootNode();
            }

            if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
                showOverDiskQuotaPaywallWarning();
                return;
            }

            showSnackbar(SNACKBAR_TYPE, getResources().getQuantityString(R.plurals.upload_began, paths.size(), paths.size()), -1);
            for (String path : paths) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Intent uploadServiceIntent = new Intent(ManagerActivity.this, UploadService.class);
                File file = new File(path);
                if (file.isDirectory()) {
                    uploadServiceIntent.putExtra(UploadService.EXTRA_FILEPATH, file.getAbsolutePath());
                    uploadServiceIntent.putExtra(UploadService.EXTRA_NAME, file.getName());
                } else {
                    ShareInfo info = ShareInfo.infoFromFile(file);
                    if (info == null) {
                        continue;
                    }
                    uploadServiceIntent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
                    uploadServiceIntent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
                    uploadServiceIntent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
                }

                uploadServiceIntent.putExtra(UploadService.EXTRA_FOLDERPATH, folderPath);
                uploadServiceIntent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
                ContextCompat.startForegroundService(ManagerActivity.this, uploadServiceIntent);
            }
        }
    }

    void disableNavigationViewMenu(Menu menu) {
        logDebug("disableNavigationViewMenu");

        MenuItem mi = menu.findItem(R.id.bottom_navigation_item_cloud_drive);
        if (mi != null) {
            mi.setChecked(false);
            mi.setEnabled(false);
        }
        mi = menu.findItem(R.id.bottom_navigation_item_camera_uploads);
        if (mi != null) {
            mi.setChecked(false);
            mi.setEnabled(false);
        }
        mi = menu.findItem(R.id.bottom_navigation_item_chat);
        if (mi != null) {
            mi.setChecked(false);
        }
        mi = menu.findItem(R.id.bottom_navigation_item_shared_items);
        if (mi != null) {
            mi.setChecked(false);
            mi.setEnabled(false);
        }
        mi = menu.findItem(R.id.bottom_navigation_item_homepage);
        if (mi != null) {
            mi.setChecked(false);
        }

        disableNavigationViewLayout();
    }

    void disableNavigationViewLayout() {
        if (myAccountSection != null) {
            myAccountSection.setEnabled(false);
            ((TextView) myAccountSection.findViewById(R.id.my_account_section_text)).setTextColor(ContextCompat.getColor(this, R.color.grey_038_white_038));
        }

        if (inboxSection != null) {
            if (inboxNode == null) {
                inboxSection.setVisibility(View.GONE);
            } else {
                boolean hasChildren = megaApi.hasChildren(inboxNode);
                if (hasChildren) {
                    inboxSection.setEnabled(false);
                    inboxSection.setVisibility(View.VISIBLE);
                    ((TextView) inboxSection.findViewById(R.id.inbox_section_text)).setTextColor(ContextCompat.getColor(this, R.color.grey_038_white_038));
                } else {
                    inboxSection.setVisibility(View.GONE);
                }
            }
        }

        if (contactsSection != null) {
            contactsSection.setEnabled(false);

            if (contactsSectionText == null) {
                contactsSectionText = contactsSection.findViewById(R.id.contacts_section_text);
            }

            contactsSectionText.setAlpha(0.38F);
            setContactTitleSection();
        }

        if (notificationsSection != null) {
            notificationsSection.setEnabled(false);

            if (notificationsSectionText == null) {
                notificationsSectionText = notificationsSection.findViewById(R.id.contacts_section_text);
            }

            notificationsSectionText.setAlpha(0.38F);
            setNotificationsTitleSection();
        }

        if (rubbishBinSection != null) {
            rubbishBinSection.setEnabled(false);
            ((TextView) rubbishBinSection.findViewById(R.id.rubbish_bin_section_text)).setAlpha(0.38F);
        }

        if (upgradeAccount != null) {
            upgradeAccount.setEnabled(false);
        }
    }

    void resetNavigationViewMenu(Menu menu) {
        logDebug("resetNavigationViewMenu()");

        if (!isOnline(this) || megaApi == null || megaApi.getRootNode() == null) {
            disableNavigationViewMenu(menu);
            return;
        }

        MenuItem mi = menu.findItem(R.id.bottom_navigation_item_cloud_drive);

        if (mi != null) {
            mi.setChecked(false);
            mi.setEnabled(true);
        }
        mi = menu.findItem(R.id.bottom_navigation_item_camera_uploads);
        if (mi != null) {
            mi.setChecked(false);
            mi.setEnabled(true);
        }
        mi = menu.findItem(R.id.bottom_navigation_item_chat);
        if (mi != null) {
            mi.setChecked(false);
            mi.setEnabled(true);
        }
        mi = menu.findItem(R.id.bottom_navigation_item_shared_items);
        if (mi != null) {
            mi.setChecked(false);
            mi.setEnabled(true);
        }

        resetNavigationViewLayout();
    }

    public void resetNavigationViewLayout() {
        if (myAccountSection != null) {
            myAccountSection.setEnabled(true);
            ((TextView) myAccountSection.findViewById(R.id.my_account_section_text)).setTextColor(
                    ColorUtils.getThemeColor(this, android.R.attr.textColorPrimary));
        }

        if (inboxSection != null) {
            if (inboxNode == null) {
                inboxSection.setVisibility(View.GONE);
                logDebug("Inbox Node is NULL");
            } else {
                boolean hasChildren = megaApi.hasChildren(inboxNode);
                if (hasChildren) {
                    inboxSection.setEnabled(true);
                    inboxSection.setVisibility(View.VISIBLE);
                    ((TextView) inboxSection.findViewById(R.id.inbox_section_text)).setTextColor(
                            ColorUtils.getThemeColor(this, android.R.attr.textColorPrimary));
                } else {
                    logDebug("Inbox Node NO children");
                    inboxSection.setVisibility(View.GONE);
                }
            }
        }

        if (contactsSection != null) {
            contactsSection.setEnabled(true);

            if (contactsSectionText == null) {
                contactsSectionText = contactsSection.findViewById(R.id.contacts_section_text);
            }

            contactsSectionText.setAlpha(1F);
            setContactTitleSection();
        }

        if (notificationsSection != null) {
            notificationsSection.setEnabled(true);

            if (notificationsSectionText == null) {
                notificationsSectionText = notificationsSection.findViewById(R.id.notification_section_text);
            }

            notificationsSectionText.setAlpha(1F);
            setNotificationsTitleSection();
        }

        if (rubbishBinSection != null) {
            rubbishBinSection.setEnabled(true);
            ((TextView) rubbishBinSection.findViewById(R.id.rubbish_bin_section_text)).setAlpha(1F);
        }

        if (upgradeAccount != null) {
            upgradeAccount.setEnabled(true);
        }
    }

    public void setInboxNavigationDrawer() {
        logDebug("setInboxNavigationDrawer");
        if (nV != null && inboxSection != null) {
            if (inboxNode == null) {
                inboxSection.setVisibility(View.GONE);
                logDebug("Inbox Node is NULL");
            } else {
                boolean hasChildren = megaApi.hasChildren(inboxNode);
                if (hasChildren) {
                    inboxSection.setEnabled(true);
                    inboxSection.setVisibility(View.VISIBLE);
                } else {
                    logDebug("Inbox Node NO children");
                    inboxSection.setVisibility(View.GONE);
                }
            }
        }
    }

    public void showProPanel() {
        logDebug("showProPanel");
        //Left and Right margin
        LinearLayout.LayoutParams proTextParams = (LinearLayout.LayoutParams) getProText.getLayoutParams();
        proTextParams.setMargins(scaleWidthPx(24, outMetrics), scaleHeightPx(23, outMetrics), scaleWidthPx(24, outMetrics), scaleHeightPx(23, outMetrics));
        getProText.setLayoutParams(proTextParams);

        rightUpgradeButton.setOnClickListener(this);
        android.view.ViewGroup.LayoutParams paramsb2 = rightUpgradeButton.getLayoutParams();
        //Left and Right margin
        LinearLayout.LayoutParams optionTextParams = (LinearLayout.LayoutParams) rightUpgradeButton.getLayoutParams();
        optionTextParams.setMargins(scaleWidthPx(6, outMetrics), 0, scaleWidthPx(8, outMetrics), 0);
        rightUpgradeButton.setLayoutParams(optionTextParams);

        leftCancelButton.setOnClickListener(this);
        android.view.ViewGroup.LayoutParams paramsb1 = leftCancelButton.getLayoutParams();
        leftCancelButton.setLayoutParams(paramsb1);
        //Left and Right margin
        LinearLayout.LayoutParams cancelTextParams = (LinearLayout.LayoutParams) leftCancelButton.getLayoutParams();
        cancelTextParams.setMargins(scaleWidthPx(6, outMetrics), 0, scaleWidthPx(6, outMetrics), 0);
        leftCancelButton.setLayoutParams(cancelTextParams);

        getProLayout.setVisibility(View.VISIBLE);
        getProLayout.bringToFront();
    }

    /**
     * Check the current storage state.
     *
     * @param onCreate Flag to indicate if the method was called from "onCreate" or not.
     */
    private void checkCurrentStorageStatus(boolean onCreate) {
        // If the current storage state is not initialized is because the app received the
        // event informing about the storage state  during login, the ManagerActivity
        // wasn't active and for this reason the value is stored in the MegaApplication object.
        int storageStateToCheck = (storageState != MegaApiJava.STORAGE_STATE_UNKNOWN) ?
                storageState : app.getStorageState();

        checkStorageStatus(storageStateToCheck, onCreate);
    }

    /**
     * Check the storage state provided as first parameter.
     *
     * @param newStorageState Storage state to check.
     * @param onCreate        Flag to indicate if the method was called from "onCreate" or not.
     */
    private void checkStorageStatus(int newStorageState, boolean onCreate) {
        Intent intent = new Intent(this, UploadService.class);
        switch (newStorageState) {
            case MegaApiJava.STORAGE_STATE_GREEN:
                logDebug("STORAGE STATE GREEN");

                intent.setAction(ACTION_STORAGE_STATE_CHANGED);

                // TODO: WORKAROUND, NEED TO IMPROVE AND REMOVE THE TRY-CATCH
                try {
                    ContextCompat.startForegroundService(this, intent);
                } catch (Exception e) {
                    logError("Exception starting UploadService", e);
                    e.printStackTrace();
                }

                if (myAccountInfo.getAccountType() == MegaAccountDetails.ACCOUNT_TYPE_FREE) {
                    logDebug("ACCOUNT TYPE FREE");
                    if (showMessageRandom()) {
                        logDebug("Show message random: TRUE");
                        showProPanel();
                    }
                }
                storageState = newStorageState;
                fireCameraUploadJob(ManagerActivity.this, false);
                break;

            case MegaApiJava.STORAGE_STATE_ORANGE:
                logWarning("STORAGE STATE ORANGE");

                intent.setAction(ACTION_STORAGE_STATE_CHANGED);

                // TODO: WORKAROUND, NEED TO IMPROVE AND REMOVE THE TRY-CATCH
                try {
                    ContextCompat.startForegroundService(this, intent);
                } catch (Exception e) {
                    logError("Exception starting UploadService", e);
                    e.printStackTrace();
                }

                if (onCreate && isStorageStatusDialogShown) {
                    isStorageStatusDialogShown = false;
                    showStorageAlmostFullDialog();
                } else if (newStorageState > storageState) {
                    showStorageAlmostFullDialog();
                }
                storageState = newStorageState;
                logDebug("Try to start CU, false.");
                fireCameraUploadJob(ManagerActivity.this, false);
				break;

            case MegaApiJava.STORAGE_STATE_RED:
                logWarning("STORAGE STATE RED");
                if (onCreate && isStorageStatusDialogShown) {
                    isStorageStatusDialogShown = false;
                    showStorageFullDialog();
                } else if (newStorageState > storageState) {
                    showStorageFullDialog();
                }
                break;

            case MegaApiJava.STORAGE_STATE_PAYWALL:
                logWarning("STORAGE STATE PAYWALL");
                break;

            default:
                return;
        }

        storageState = newStorageState;
        app.setStorageState(storageState);
    }

    /**
     * Show a dialog to indicate that the storage space is almost full.
     */
    public void showStorageAlmostFullDialog() {
        logDebug("showStorageAlmostFullDialog");
        showStorageStatusDialog(MegaApiJava.STORAGE_STATE_ORANGE, false, false);
    }

    /**
     * Show a dialog to indicate that the storage space is full.
     */
    public void showStorageFullDialog() {
        logDebug("showStorageFullDialog");
        showStorageStatusDialog(MegaApiJava.STORAGE_STATE_RED, false, false);
    }

    /**
     * Show an overquota alert dialog.
     *
     * @param preWarning Flag to indicate if is a pre-overquota alert or not.
     */
    public void showOverquotaAlert(boolean preWarning) {
        logDebug("preWarning: " + preWarning);
        showStorageStatusDialog(
                preWarning ? MegaApiJava.STORAGE_STATE_ORANGE : MegaApiJava.STORAGE_STATE_RED,
                true, preWarning);
    }

    public void showSMSVerificationDialog() {
        isSMSDialogShowing = true;
        smsDialogTimeChecker.update();
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.sms_verification_dialog_layout, null);
        dialogBuilder.setView(dialogView);

        TextView msg = dialogView.findViewById(R.id.sv_dialog_msg);
        boolean isAchievementUser = megaApi.isAchievementsEnabled();
        logDebug("is achievement user: " + isAchievementUser);
        if (isAchievementUser) {
            String message = String.format(getString(R.string.sms_add_phone_number_dialog_msg_achievement_user), myAccountInfo.getBonusStorageSMS());
            msg.setText(message);
        } else {
            msg.setText(R.string.sms_add_phone_number_dialog_msg_non_achievement_user);
        }

        dialogBuilder.setPositiveButton(R.string.general_add, (dialog, which) -> {
            startActivity(new Intent(getApplicationContext(), SMSVerificationActivity.class));
            alertDialogSMSVerification.dismiss();
        }).setNegativeButton(R.string.verify_account_not_now_button, (dialog, which) -> {
            alertDialogSMSVerification.dismiss();
        });

        if (alertDialogSMSVerification == null) {
            alertDialogSMSVerification = dialogBuilder.create();
            alertDialogSMSVerification.setCancelable(false);
            alertDialogSMSVerification.setOnDismissListener(dialog -> isSMSDialogShowing = false);
            alertDialogSMSVerification.setCanceledOnTouchOutside(false);
        }
        alertDialogSMSVerification.show();
    }

    /**
     * Method to show a dialog to indicate the storage status.
     *
     * @param storageState   Storage status.
     * @param overquotaAlert Flag to indicate that is an overquota alert or not.
     * @param preWarning     Flag to indicate if is a pre-overquota alert or not.
     */
    private void showStorageStatusDialog(int storageState, boolean overquotaAlert, boolean preWarning) {
        logDebug("showStorageStatusDialog");

        if (myAccountInfo.getAccountType() == -1) {
            logWarning("Do not show dialog, not info of the account received yet");
            return;
        }

        if (isStorageStatusDialogShown) {
            logDebug("Storage status dialog already shown");
            return;
        }

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.storage_status_dialog_layout, null);
        dialogBuilder.setView(dialogView);

        TextView title = (TextView) dialogView.findViewById(R.id.storage_status_title);
        title.setText(getString(R.string.action_upgrade_account));

        ImageView image = (ImageView) dialogView.findViewById(R.id.image_storage_status);
        TextView text = (TextView) dialogView.findViewById(R.id.text_storage_status);

        Product pro3 = getPRO3OneMonth();
        String storageString = "";
        String transferString = "";
        if (pro3 != null) {
            storageString = getSizeStringGBBased(pro3.getStorage());
            transferString = getSizeStringGBBased(pro3.getTransfer());
        }

        switch (storageState) {
            case MegaApiJava.STORAGE_STATE_GREEN:
                logDebug("STORAGE STATE GREEN");
                return;

            case MegaApiJava.STORAGE_STATE_ORANGE:
                image.setImageResource(R.drawable.ic_storage_almost_full);
                text.setText(String.format(getString(R.string.text_almost_full_warning), storageString, transferString));
                break;

            case MegaApiJava.STORAGE_STATE_RED:
                image.setImageResource(R.drawable.ic_storage_full);
                text.setText(String.format(getString(R.string.text_storage_full_warning), storageString, transferString));
                break;

            default:
                logWarning("STORAGE STATE INVALID VALUE: " + storageState);
                return;
        }

        if (overquotaAlert) {
            if (!preWarning)
                title.setText(getString(R.string.overquota_alert_title));

            text.setText(getString(preWarning ? R.string.pre_overquota_alert_text :
                    R.string.overquota_alert_text));
        }

        LinearLayout horizontalButtonsLayout = (LinearLayout) dialogView.findViewById(R.id.horizontal_buttons_storage_status_layout);
        LinearLayout verticalButtonsLayout = (LinearLayout) dialogView.findViewById(R.id.vertical_buttons_storage_status_layout);

        final OnClickListener dismissClickListener = new OnClickListener() {
            public void onClick(View v) {
                alertDialogStorageStatus.dismiss();
                isStorageStatusDialogShown = false;
            }
        };

        final OnClickListener upgradeClickListener = new OnClickListener() {
            public void onClick(View v) {
                alertDialogStorageStatus.dismiss();
                isStorageStatusDialogShown = false;
                navigateToUpgradeAccount();
            }
        };

        final OnClickListener achievementsClickListener = new OnClickListener() {
            public void onClick(View v) {
                alertDialogStorageStatus.dismiss();
                isStorageStatusDialogShown = false;
                logDebug("Go to achievements section");
                navigateToAchievements();
            }
        };

        final OnClickListener customPlanClickListener = v -> {
            alertDialogStorageStatus.dismiss();
            isStorageStatusDialogShown = false;
            askForCustomizedPlan(this, megaApi.getMyEmail(), myAccountInfo.getAccountType());
        };

        Button verticalDismissButton = (Button) dialogView.findViewById(R.id.vertical_storage_status_button_dissmiss);
        verticalDismissButton.setOnClickListener(dismissClickListener);
        Button horizontalDismissButton = (Button) dialogView.findViewById(R.id.horizontal_storage_status_button_dissmiss);
        horizontalDismissButton.setOnClickListener(dismissClickListener);

        Button verticalActionButton = (Button) dialogView.findViewById(R.id.vertical_storage_status_button_action);
        Button horizontalActionButton = (Button) dialogView.findViewById(R.id.horizontal_storage_status_button_payment);

        Button achievementsButton = (Button) dialogView.findViewById(R.id.vertical_storage_status_button_achievements);
        achievementsButton.setOnClickListener(achievementsClickListener);

        switch (myAccountInfo.getAccountType()) {
            case MegaAccountDetails.ACCOUNT_TYPE_PROIII:
                logDebug("Show storage status dialog for USER PRO III");
                if (!overquotaAlert) {
                    if (storageState == MegaApiJava.STORAGE_STATE_ORANGE) {
                        text.setText(getString(R.string.text_almost_full_warning_pro3_account));
                    } else if (storageState == MegaApiJava.STORAGE_STATE_RED) {
                        text.setText(getString(R.string.text_storage_full_warning_pro3_account));
                    }
                }
                horizontalActionButton.setText(getString(R.string.button_custom_almost_full_warning));
                horizontalActionButton.setOnClickListener(customPlanClickListener);
                verticalActionButton.setText(getString(R.string.button_custom_almost_full_warning));
                verticalActionButton.setOnClickListener(customPlanClickListener);
                break;

            case MegaAccountDetails.ACCOUNT_TYPE_LITE:
            case MegaAccountDetails.ACCOUNT_TYPE_PROI:
            case MegaAccountDetails.ACCOUNT_TYPE_PROII:
                logDebug("Show storage status dialog for USER PRO");
                if (!overquotaAlert) {
                    if (storageState == MegaApiJava.STORAGE_STATE_ORANGE) {
                        text.setText(String.format(getString(R.string.text_almost_full_warning_pro_account), storageString, transferString));
                    } else if (storageState == MegaApiJava.STORAGE_STATE_RED) {
                        text.setText(String.format(getString(R.string.text_storage_full_warning_pro_account), storageString, transferString));
                    }
                }
                horizontalActionButton.setText(getString(R.string.my_account_upgrade_pro));
                horizontalActionButton.setOnClickListener(upgradeClickListener);
                verticalActionButton.setText(getString(R.string.my_account_upgrade_pro));
                verticalActionButton.setOnClickListener(upgradeClickListener);
                break;

            case MegaAccountDetails.ACCOUNT_TYPE_FREE:
            default:
                logDebug("Show storage status dialog for FREE USER");
                horizontalActionButton.setText(getString(R.string.button_plans_almost_full_warning));
                horizontalActionButton.setOnClickListener(upgradeClickListener);
                verticalActionButton.setText(getString(R.string.button_plans_almost_full_warning));
                verticalActionButton.setOnClickListener(upgradeClickListener);
                break;
        }

        if (megaApi.isAchievementsEnabled()) {
            horizontalButtonsLayout.setVisibility(View.GONE);
            verticalButtonsLayout.setVisibility(View.VISIBLE);
        } else {
            horizontalButtonsLayout.setVisibility(View.VISIBLE);
            verticalButtonsLayout.setVisibility(View.GONE);
        }

        alertDialogStorageStatus = dialogBuilder.create();
        alertDialogStorageStatus.setCancelable(false);
        alertDialogStorageStatus.setCanceledOnTouchOutside(false);

        isStorageStatusDialogShown = true;

        alertDialogStorageStatus.show();
    }

    private Product getPRO3OneMonth() {
        List<Product> products = myAccountInfo.getProductAccounts();
        if (products != null) {
            for (Product product : products) {
                if (product != null && product.getLevel() == PRO_III && product.getMonths() == 1) {
                    return product;
                }
            }
        } else {
            // Edge case: when this method is called, TYPE_GET_PRICING hasn't finished yet.
            logWarning("Products haven't been initialized!");
        }
        return null;
    }

    private void refreshOfflineNodes() {
        logDebug("updateOfflineView");
        if (fullscreenOfflineFragment != null) {
            fullscreenOfflineFragment.refreshNodes();
        } else if (pagerOfflineFragment != null) {
            pagerOfflineFragment.refreshNodes();
        }
    }

    /**
     * Handle processed upload intent.
     *
     * @param infos List<ShareInfo> containing all the upload info.
     */
    private void onIntentProcessed(List<ShareInfo> infos) {
        logDebug("onIntentProcessed");

        dismissAlertDialogIfExists(statusDialog);
        dismissAlertDialogIfExists(processFileDialog);

        MegaNode parentNode = getCurrentParentNode(getCurrentParentHandle(), -1);
        if (parentNode == null) {
            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_temporary_unavaible), -1);
            return;
        }

        if (infos == null) {
            showSnackbar(SNACKBAR_TYPE, getString(R.string.upload_can_not_open), -1);
        } else {
            if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
                showOverDiskQuotaPaywallWarning();
                return;
            }

            showSnackbar(SNACKBAR_TYPE, getResources().getQuantityString(R.plurals.upload_began, infos.size(), infos.size()), -1);

            for (ShareInfo info : infos) {
                if (info.isContact) {
                    requestContactsPermissions(info, parentNode);
                } else {
                    Intent intent = new Intent(this, UploadService.class);
                    intent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
                    intent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
                    intent.putExtra(UploadService.EXTRA_LAST_MODIFIED, info.getLastModified());
                    intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
                    ContextCompat.startForegroundService(this, intent);
                }
            }
        }
    }

    public void requestContactsPermissions(ShareInfo info, MegaNode parentNode) {
        logDebug("requestContactsPermissions");
        if (!hasPermissions(this, Manifest.permission.READ_CONTACTS)) {
            logWarning("No read contacts permission");
            infoManager = info;
            parentNodeManager = parentNode;
            requestPermission(this, REQUEST_UPLOAD_CONTACT, Manifest.permission.READ_CONTACTS);
        } else {
            uploadContactInfo(info, parentNode);
        }
    }

    public void uploadContactInfo(ShareInfo info, MegaNode parentNode) {
        logDebug("Upload contact info");

        Cursor cursorID = getContentResolver().query(info.contactUri, null, null, null, null);

        if (cursorID != null) {
            if (cursorID.moveToFirst()) {
                logDebug("It is a contact");

                String id = cursorID.getString(cursorID.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cursorID.getString(cursorID.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                int hasPhone = cursorID.getInt(cursorID.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                // get the user's email address
                String email = null;
                Cursor ce = getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", new String[]{id}, null);
                if (ce != null && ce.moveToFirst()) {
                    email = ce.getString(ce.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                    ce.close();
                }

                // get the user's phone number
                String phone = null;
                if (hasPhone > 0) {
                    Cursor cp = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                    if (cp != null && cp.moveToFirst()) {
                        phone = cp.getString(cp.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        cp.close();
                    }
                }

                StringBuilder data = new StringBuilder();
                data.append(name);
                if (phone != null) {
                    data.append(", " + phone);
                }

                if (email != null) {
                    data.append(", " + email);
                }

                createFile(name, data.toString(), parentNode);
            }
            cursorID.close();
        } else {
            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_temporary_unavaible), -1);
        }
    }

    private void createFile(String name, String data, MegaNode parentNode) {

        if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
            showOverDiskQuotaPaywallWarning();
            return;
        }

        File file = createTemporalTextFile(this, name, data);
        if (file != null) {
            showSnackbar(SNACKBAR_TYPE, getResources().getQuantityString(R.plurals.upload_began, 1, 1), -1);

            Intent intent = new Intent(this, UploadService.class);
            intent.putExtra(UploadService.EXTRA_FILEPATH, file.getAbsolutePath());
            intent.putExtra(UploadService.EXTRA_NAME, file.getName());
            intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
            intent.putExtra(UploadService.EXTRA_SIZE, file.getTotalSpace());
            ContextCompat.startForegroundService(this, intent);
        } else {
            showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
        }
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
        logDebug("onRequestStart(CHAT): " + request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        logDebug("onRequestFinish(CHAT): " + request.getRequestString() + "_" + e.getErrorCode());

        if (request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM) {
            logDebug("Create chat request finish");
            onRequestFinishCreateChat(e.getErrorCode(), request.getChatHandle());
        } else if (request.getType() == MegaChatRequest.TYPE_DISCONNECT) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("DISConnected from chat!");
            } else {
                logError("ERROR WHEN DISCONNECTING " + e.getErrorString());
            }
        } else if (request.getType() == MegaChatRequest.TYPE_LOGOUT) {
            logDebug("onRequestFinish(CHAT): " + MegaChatRequest.TYPE_LOGOUT);

            if (e.getErrorCode() != MegaError.API_OK) {
                logError("MegaChatRequest.TYPE_LOGOUT:ERROR");
            }

            if (app != null) {
                app.disableMegaChatApi();
            }
            resetLoggerSDK();
        } else if (request.getType() == MegaChatRequest.TYPE_SET_ONLINE_STATUS) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("Status changed to: " + request.getNumber());
            } else if (e.getErrorCode() == MegaChatError.ERROR_ARGS) {
                logWarning("Status not changed, the chosen one is the same");
            } else {
                logError("ERROR WHEN TYPE_SET_ONLINE_STATUS " + e.getErrorString());
                showSnackbar(SNACKBAR_TYPE, getString(R.string.changing_status_error), -1);
            }
        } else if (request.getType() == MegaChatRequest.TYPE_ARCHIVE_CHATROOM) {
            long chatHandle = request.getChatHandle();
            MegaChatRoom chat = megaChatApi.getChatRoom(chatHandle);
            String chatTitle = getTitleChat(chat);

            if (chatTitle == null) {
                chatTitle = "";
            } else if (!chatTitle.isEmpty() && chatTitle.length() > 60) {
                chatTitle = chatTitle.substring(0, 59) + "...";
            }

            if (!chatTitle.isEmpty() && chat.isGroup() && !chat.hasCustomTitle()) {
                chatTitle = "\"" + chatTitle + "\"";
            }

            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                if (request.getFlag()) {
                    logDebug("Chat archived");
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.success_archive_chat, chatTitle), -1);
                } else {
                    logDebug("Chat unarchived");
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.success_unarchive_chat, chatTitle), -1);
                }
            } else {
                if (request.getFlag()) {
                    logError("ERROR WHEN ARCHIVING CHAT " + e.getErrorString());
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_archive_chat, chatTitle), -1);
                } else {
                    logError("ERROR WHEN UNARCHIVING CHAT " + e.getErrorString());
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_unarchive_chat, chatTitle), -1);
                }
            }
        } else if (request.getType() == MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE: " + request.getFlag());
            } else {
                logError("MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE:error: " + e.getErrorType());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    public void onRequestFinishCreateChat(int errorCode, long chatHandle) {
        if (errorCode == MegaChatError.ERROR_OK) {
            logDebug("Chat CREATED.");

            //Update chat view
            logDebug("Open new chat: " + chatHandle);
            Intent intent = new Intent(this, ChatActivity.class);
            intent.setAction(ACTION_CHAT_SHOW_MESSAGES);
            intent.putExtra(CHAT_ID, chatHandle);
            this.startActivity(intent);
        } else {
            logError("ERROR WHEN CREATING CHAT " + errorCode);
            showSnackbar(SNACKBAR_TYPE, getString(R.string.create_chat_error), -1);
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        logDebug("onRequestStart: " + request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        logDebug("onRequestUpdate: " + request.getRequestString());
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        logDebug("onRequestFinish: " + request.getRequestString() + "_" + e.getErrorCode());
        if (request.getType() == MegaRequest.TYPE_LOGOUT) {
            logDebug("onRequestFinish: " + MegaRequest.TYPE_LOGOUT);

            if (e.getErrorCode() == MegaError.API_OK) {
                logDebug("onRequestFinish:OK:" + MegaRequest.TYPE_LOGOUT);
                logDebug("END logout sdk request - wait chat logout");
            } else if (e.getErrorCode() != MegaError.API_ESID) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
            }
        } else if (request.getType() == MegaRequest.TYPE_GET_ACHIEVEMENTS) {
            if (e.getErrorCode() == MegaError.API_OK) {
                myAccountInfo.setBonusStorageSMS(getSizeString(request.getMegaAchievementsDetails()
                        .getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE)));
            }
            showAddPhoneNumberInMenu();
            checkBeforeShowSMSVerificationDialog();
        } else if (request.getType() == MegaRequest.TYPE_SET_ATTR_USER) {
            if (request.getParamType() == MegaApiJava.USER_ATTR_PWD_REMINDER) {
                logDebug("MK exported - USER_ATTR_PWD_REMINDER finished");
                if (e.getErrorCode() == MegaError.API_OK || e.getErrorCode() == MegaError.API_ENOENT) {
                    logDebug("New value of attribute USER_ATTR_PWD_REMINDER: " + request.getText());
                }
            }
        } else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER) {
            if (request.getParamType() == MegaApiJava.USER_ATTR_AVATAR) {
                logDebug("Request avatar");
                if (e.getErrorCode() == MegaError.API_OK) {
                    setProfileAvatar();
                } else if (e.getErrorCode() == MegaError.API_ENOENT) {
                    setDefaultAvatar();
                } else if (e.getErrorCode() == MegaError.API_EARGS) {
                    logError("Error changing avatar: ");
                }

                LiveEventBus.get(EVENT_AVATAR_CHANGE, Boolean.class).post(false);
            } else if (request.getParamType() == MegaApiJava.USER_ATTR_FIRSTNAME) {
                updateMyData(true, request.getText(), e);
            } else if (request.getParamType() == MegaApiJava.USER_ATTR_LASTNAME) {
                updateMyData(false, request.getText(), e);
            } else if (request.getParamType() == MegaApiJava.USER_ATTR_GEOLOCATION) {

                if (e.getErrorCode() == MegaError.API_OK) {
                    logDebug("Attribute USER_ATTR_GEOLOCATION enabled");
                    MegaApplication.setEnabledGeoLocation(true);
                } else {
                    logDebug("Attribute USER_ATTR_GEOLOCATION disabled");
                    MegaApplication.setEnabledGeoLocation(false);
                }
            } else if (request.getParamType() == MegaApiJava.USER_ATTR_DISABLE_VERSIONS) {
                MegaApplication.setDisableFileVersions(request.getFlag());
            } else if (request.getParamType() == USER_ATTR_MY_BACKUPS_FOLDER) {
                if (e.getErrorCode() == MegaError.API_OK) {
                    logDebug("requesting myBackupHandle");
                    MegaNodeUtil.myBackupHandle = request.getNodeHandle();
                }
            }
        } else if (request.getType() == MegaRequest.TYPE_GET_CANCEL_LINK) {
            logDebug("TYPE_GET_CANCEL_LINK");
            hideKeyboard(managerActivity, 0);

            if (e.getErrorCode() == MegaError.API_OK) {
                logDebug("Cancelation link received!");
                showAlert(this, getString(R.string.email_verification_text), getString(R.string.email_verification_title));
            } else {
                logError("Error when asking for the cancelation link: " + e.getErrorString() + "___" + e.getErrorCode());
                showAlert(this, getString(R.string.general_text_error), getString(R.string.general_error_word));
            }
        } else if (request.getType() == MegaRequest.TYPE_REMOVE_CONTACT) {

            if (e.getErrorCode() == MegaError.API_OK) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_removed), -1);
            } else if (e.getErrorCode() == MegaError.API_EMASTERONLY) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_remove_business_contact, request.getEmail()), -1);
            } else {
                logError("Error deleting contact");
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_not_removed), -1);
            }
        } else if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT) {
            logDebug("MegaRequest.TYPE_INVITE_CONTACT finished: " + request.getNumber());

            dismissAlertDialogIfExists(statusDialog);

            if (request.getNumber() == MegaContactRequest.INVITE_ACTION_REMIND) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_invitation_resent), -1);
            } else {
                if (e.getErrorCode() == MegaError.API_OK) {
                    logDebug("OK INVITE CONTACT: " + request.getEmail());
                    if (request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_request_sent, request.getEmail()), -1);
                    } else if (request.getNumber() == MegaContactRequest.INVITE_ACTION_DELETE) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_invitation_deleted), -1);
                    }
                } else {
                    logError("ERROR invite contact: " + e.getErrorCode() + "___" + e.getErrorString());
                    if (e.getErrorCode() == MegaError.API_EEXIST) {
                        boolean found = false;
                        ArrayList<MegaContactRequest> outgoingContactRequests = megaApi.getOutgoingContactRequests();
                        if (outgoingContactRequests != null) {
                            for (int i = 0; i < outgoingContactRequests.size(); i++) {
                                if (outgoingContactRequests.get(i).getTargetEmail().equals(request.getEmail())) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (found) {
                            showSnackbar(SNACKBAR_TYPE, getString(R.string.invite_not_sent_already_sent, request.getEmail()), -1);
                        } else {
                            showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_already_exists, request.getEmail()), -1);
                        }
                    } else if (request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD && e.getErrorCode() == MegaError.API_EARGS) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.error_own_email_as_contact), -1);
                    } else {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error), -1);
                    }
                }
            }
        } else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS) {
            logDebug("MegaRequest.TYPE_PAUSE_TRANSFERS");
            //force update the pause notification to prevent missed onTransferUpdate
            sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION));

            if (e.getErrorCode() == MegaError.API_OK) {
                transfersWidget.updateState();

                if (drawerItem == DrawerItem.TRANSFERS && isTransfersInProgressAdded()) {
                    boolean paused = megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD) || megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD);
                    refreshFragment(FragmentTag.TRANSFERS.getTag());
                    mTabsAdapterTransfers.notifyDataSetChanged();

                    pauseTransfersMenuIcon.setVisible(!paused);
                    playTransfersMenuIcon.setVisible(paused);
                }

                // Update CU backup state.
                int newBackupState = megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)
                        ? CameraUploadSyncManager.State.CU_SYNC_STATE_PAUSE_UP
                        : CameraUploadSyncManager.State.CU_SYNC_STATE_ACTIVE;

                CameraUploadSyncManager.INSTANCE.updatePrimaryBackupState(newBackupState);
                CameraUploadSyncManager.INSTANCE.updateSecondaryBackupState(newBackupState);
            }
        } else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFER) {
            logDebug("One MegaRequest.TYPE_PAUSE_TRANSFER");

            if (e.getErrorCode() == MegaError.API_OK) {
                TransfersManagement transfersManagement = MegaApplication.getTransfersManagement();
                int transferTag = request.getTransferTag();

                if (request.getFlag()) {
                    transfersManagement.addPausedTransfers(transferTag);
                } else {
                    transfersManagement.removePausedTransfers(transferTag);
                }

                if (isTransfersInProgressAdded()) {
                    transfersFragment.changeStatusButton(request.getTransferTag());
                }
            } else {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_general_nodes), -1);
            }
        } else if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFER) {
            if (e.getErrorCode() == MegaError.API_OK) {
                MegaApplication.getTransfersManagement().removePausedTransfers(request.getTransferTag());
                transfersWidget.update();
                supportInvalidateOptionsMenu();
            } else {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_general_nodes), MEGACHAT_INVALID_HANDLE);
            }
        } else if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFERS) {
            logDebug("MegaRequest.TYPE_CANCEL_TRANSFERS");
            //After cancelling all the transfers
            if (e.getErrorCode() == MegaError.API_OK) {
                transfersWidget.hide();

                if (drawerItem == DrawerItem.TRANSFERS && isTransfersInProgressAdded()) {
                    pauseTransfersMenuIcon.setVisible(false);
                    playTransfersMenuIcon.setVisible(false);
                    cancelAllTransfersMenuItem.setVisible(false);
                }

                MegaApplication.getTransfersManagement().resetPausedTransfers();
            } else {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_general_nodes), -1);
            }

        } else if (request.getType() == MegaRequest.TYPE_COPY) {
            logDebug("TYPE_COPY");

            dismissAlertDialogIfExists(statusDialog);

            if (e.getErrorCode() == MegaError.API_OK) {
                logDebug("Show snackbar!!!!!!!!!!!!!!!!!!!");
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_copied), -1);

                if (drawerItem == DrawerItem.CLOUD_DRIVE) {
                    if (isCloudAdded()) {
                        ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleBrowser),
                                sortOrderManagement.getOrderCloud());
                        fileBrowserFragment.setNodes(nodes);
                        fileBrowserFragment.getRecyclerView().invalidate();
                    }
                } else if (drawerItem == DrawerItem.RUBBISH_BIN) {
                    refreshRubbishBin();
                } else if (drawerItem == DrawerItem.INBOX) {
                    refreshInboxList();
                }

                resetAccountDetailsTimeStamp();
            } else {
                if (e.getErrorCode() == MegaError.API_EOVERQUOTA) {
                    logWarning("OVERQUOTA ERROR: " + e.getErrorCode());
                    if (api.isForeignNode(request.getParentHandle())) {
                        showForeignStorageOverQuotaWarningDialog(this);
                    } else {
                        showOverquotaAlert(false);
                    }
                } else if (e.getErrorCode() == MegaError.API_EGOINGOVERQUOTA) {
                    logDebug("OVERQUOTA ERROR: " + e.getErrorCode());
                    showOverquotaAlert(true);
                } else {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied), -1);
                }
            }
        } else if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER) {
            dismissAlertDialogIfExists(statusDialog);

            if (e.getErrorCode() == MegaError.API_OK) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_created), -1);
                MegaNode folderNode =  megaApi.getNodeByHandle(request.getNodeHandle());
                if (folderNode == null) {
                    return;
                }

                if (drawerItem == DrawerItem.CLOUD_DRIVE) {
                    if (isCloudAdded()) {
                        fileBrowserFragment.setFolderInfoNavigation(folderNode);
                    }
                } else if (drawerItem == DrawerItem.SHARED_ITEMS) {
                    switch (getTabItemShares()) {
                        case INCOMING_TAB:
                            if (isIncomingAdded()) {
                                incomingSharesFragment.navigateToFolder(folderNode);
                            }
                            break;

                        case OUTGOING_TAB:
                            if (isOutgoingAdded()) {
                                outgoingSharesFragment.navigateToFolder(folderNode);
                            }
                            break;

                        case LINKS_TAB:
                            if (isLinksAdded()) {
                                linksFragment.navigateToFolder(folderNode);
                            }
                            break;
                    }
                }
            } else {
                logError("TYPE_CREATE_FOLDER ERROR: " + e.getErrorCode() + " " + e.getErrorString());
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_no_created), -1);
            }
        } else if (request.getType() == MegaRequest.TYPE_SUBMIT_PURCHASE_RECEIPT) {
            if (e.getErrorCode() == MegaError.API_OK) {
                logDebug("PURCHASE CORRECT!");
                drawerItem = DrawerItem.CLOUD_DRIVE;
                selectDrawerItem(drawerItem);
            } else {
                logError("PURCHASE WRONG: " + e.getErrorString() + " (" + e.getErrorCode() + ")");
            }
        } else if (request.getType() == MegaRequest.TYPE_REGISTER_PUSH_NOTIFICATION) {
            if (e.getErrorCode() == MegaError.API_OK) {
                logDebug("FCM OK TOKEN MegaRequest.TYPE_REGISTER_PUSH_NOTIFICATION");
            } else {
                logError("FCM ERROR TOKEN TYPE_REGISTER_PUSH_NOTIFICATION: " + e.getErrorCode() + "__" + e.getErrorString());
            }
        } else if (request.getType() == MegaRequest.TYPE_FOLDER_INFO) {
            if (e.getErrorCode() == MegaError.API_OK) {
                MegaFolderInfo info = request.getMegaFolderInfo();
                int numVersions = info.getNumVersions();
                logDebug("Num versions: " + numVersions);
                long previousVersions = info.getVersionsSize();
                logDebug("Previous versions: " + previousVersions);

                myAccountInfo.setNumVersions(numVersions);
                myAccountInfo.setPreviousVersionsSize(previousVersions);

            } else {
                logError("ERROR requesting version info of the account");
            }
        } else if (request.getType() == MegaRequest.TYPE_REMOVE) {
            if (versionsToRemove > 0) {
                logDebug("Remove request finished");
                if (e.getErrorCode() == MegaError.API_OK) {
                    versionsRemoved++;
                } else {
                    errorVersionRemove++;
                }

                if (versionsRemoved + errorVersionRemove == versionsToRemove) {
                    if (versionsRemoved == versionsToRemove) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.version_history_deleted), -1);
                    } else {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.version_history_deleted_erroneously)
                                        + "\n" + getQuantityString(R.plurals.versions_deleted_succesfully, versionsRemoved, versionsRemoved)
                                        + "\n" + getQuantityString(R.plurals.versions_not_deleted, errorVersionRemove, errorVersionRemove),
                                MEGACHAT_INVALID_HANDLE);
                    }
                    versionsToRemove = 0;
                    versionsRemoved = 0;
                    errorVersionRemove = 0;
                }
            } else {
                logDebug("Remove request finished");
                if (e.getErrorCode() == MegaError.API_OK) {
                    finish();
                } else if (e.getErrorCode() == MegaError.API_EMASTERONLY) {
                    showSnackbar(SNACKBAR_TYPE, e.getErrorString(), -1);
                } else {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_removed), -1);
                }
            }
        }
    }

    /**
     * Updates own firstName/lastName and fullName data in UI and DB.
     *
     * @param firstName True if the update makes reference to the firstName, false it to the lastName.
     * @param newName   New firstName/lastName text.
     * @param e         MegaError of the request.
     */
    private void updateMyData(boolean firstName, String newName, MegaError e) {
        myAccountInfo.updateMyData(firstName, newName, e);
        updateUserNameNavigationView(myAccountInfo.getFullName());
        LiveEventBus.get(EVENT_USER_NAME_UPDATED, Boolean.class).post(true);
    }

    public void updateAccountStorageInfo() {
        logDebug("updateAccountStorageInfo");
        megaApi.getFolderInfo(megaApi.getRootNode(), this);
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
                                        MegaError e) {
        logWarning("onRequestTemporaryError: " + request.getRequestString() + "__" + e.getErrorCode() + "__" + e.getErrorString());
    }

    public void updateUsers(List<MegaUser> users) {
        if (users != null) {
            logDebug("users.size(): " + users.size());
            for (int i = 0; i < users.size(); i++) {
                MegaUser user = users.get(i);

                if (user != null) {
                    // 0 if the change is external.
                    // >0 if the change is the result of an explicit request
                    // -1 if the change is the result of an implicit request made by the SDK internally

                    if (user.isOwnChange() > 0) {
                        logDebug("isOwnChange!!!: " + user.getEmail());
                        if (user.hasChanged(MegaUser.CHANGE_TYPE_RICH_PREVIEWS)) {
                            logDebug("Change on CHANGE_TYPE_RICH_PREVIEWS");
                            megaApi.shouldShowRichLinkWarning(this);
                            megaApi.isRichPreviewsEnabled(this);
                        }
                    } else {
                        logDebug("NOT OWN change");

                        logDebug("Changes: " + user.getChanges());

                        if (user.hasChanged(MegaUser.CHANGE_TYPE_FIRSTNAME)) {
                            if (user.getEmail().equals(megaApi.getMyUser().getEmail())) {
                                logDebug("I change my first name");
                                megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_FIRSTNAME, this);
                            } else {
                                logDebug("The user: " + user.getHandle() + "changed his first name");
                                megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_FIRSTNAME, new GetAttrUserListener(this));
                            }
                        }

                        if (user.hasChanged(MegaUser.CHANGE_TYPE_LASTNAME)) {
                            if (user.getEmail().equals(megaApi.getMyUser().getEmail())) {
                                logDebug("I change my last name");
                                megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_LASTNAME, this);
                            } else {
                                logDebug("The user: " + user.getHandle() + "changed his last name");
                                megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_LASTNAME, new GetAttrUserListener(this));
                            }
                        }

                        if (user.hasChanged(MegaUser.CHANGE_TYPE_ALIAS)) {
                            logDebug("I changed the user: " + user.getHandle() + " nickname");
                            megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_ALIAS, new GetAttrUserListener(this));
                        }

                        if (user.hasChanged(MegaUser.CHANGE_TYPE_AVATAR)) {
                            logDebug("The user: " + user.getHandle() + "changed his AVATAR");

                            File avatar = CacheFolderManager.buildAvatarFile(this, user.getEmail() + ".jpg");
                            Bitmap bitmap = null;
                            if (isFileAvailable(avatar)) {
                                avatar.delete();
                            }

                            if (user.getEmail().equals(megaApi.getMyUser().getEmail())) {
                                logDebug("I change my avatar");
                                String destinationPath = CacheFolderManager.buildAvatarFile(this, megaApi.getMyEmail() + ".jpg").getAbsolutePath();
                                megaApi.getUserAvatar(megaApi.getMyUser(), destinationPath, this);
                            }
                        }

                        if (user.hasChanged(MegaUser.CHANGE_TYPE_EMAIL)) {
                            logDebug("CHANGE_TYPE_EMAIL");
                            if (user.getEmail().equals(megaApi.getMyUser().getEmail())) {
                                logDebug("I change my mail");
                                updateMyEmail(user.getEmail());
                            } else {
                                logDebug("The contact: " + user.getHandle() + " changes the mail.");
                                if (dbH.findContactByHandle(String.valueOf(user.getHandle())) == null) {
                                    logWarning("The contact NOT exists -> DB inconsistency! -> Clear!");
                                    if (dbH.getContactsSize() != megaApi.getContacts().size()) {
                                        dbH.clearContacts();
                                        FillDBContactsTask fillDBContactsTask = new FillDBContactsTask(this);
                                        fillDBContactsTask.execute();
                                    }
                                } else {
                                    logDebug("The contact already exists -> update");
                                    dbH.setContactMail(user.getHandle(), user.getEmail());
                                }
                            }
                        }
                    }
                } else {
                    logWarning("user == null --> Continue...");
                    continue;
                }
            }
        }
    }

    public void openLocation(long nodeHandle) {
        logDebug("Node handle: " + nodeHandle);

        MegaNode node = megaApi.getNodeByHandle(nodeHandle);
        if (node == null) {
            return;
        }
        comesFromNotifications = true;
        comesFromNotificationHandle = nodeHandle;
        MegaNode parent = nC.getParent(node);
        if (parent.getHandle() == megaApi.getRootNode().getHandle()) {
            //Cloud Drive
            drawerItem = DrawerItem.CLOUD_DRIVE;
            openFolderRefresh = true;
            comesFromNotificationHandleSaved = parentHandleBrowser;
            setParentHandleBrowser(nodeHandle);
            selectDrawerItem(drawerItem);
        } else if (parent.getHandle() == megaApi.getRubbishNode().getHandle()) {
            //Rubbish
            drawerItem = DrawerItem.RUBBISH_BIN;
            openFolderRefresh = true;
            comesFromNotificationHandleSaved = parentHandleRubbish;
            setParentHandleRubbish(nodeHandle);
            selectDrawerItem(drawerItem);
        } else if (parent.getHandle() == megaApi.getInboxNode().getHandle()) {
            //Inbox
            drawerItem = DrawerItem.INBOX;
            openFolderRefresh = true;
            comesFromNotificationHandleSaved = parentHandleInbox;
            setParentHandleInbox(nodeHandle);
            selectDrawerItem(drawerItem);
        } else {
            //Incoming Shares
            drawerItem = DrawerItem.SHARED_ITEMS;
            indexShares = 0;
            comesFromNotificationDeepBrowserTreeIncoming = deepBrowserTreeIncoming;
            comesFromNotificationHandleSaved = parentHandleIncoming;
            if (parent != null) {
                comesFromNotificationsLevel = deepBrowserTreeIncoming = calculateDeepBrowserTreeIncoming(node, this);
            }
            openFolderRefresh = true;
            setParentHandleIncoming(nodeHandle);
            selectDrawerItem(drawerItem);
        }
    }

    public void updateUserAlerts(List<MegaUserAlert> userAlerts) {
        setNotificationsTitleSection();
        notificationsFragment = (NotificationsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.NOTIFICATIONS.getTag());
        if (notificationsFragment != null && userAlerts != null) {
            notificationsFragment.updateNotifications(userAlerts);
        }

        updateNavigationToolbarIcon();
    }

    public void updateMyEmail(String email) {
        LiveEventBus.get(EVENT_USER_EMAIL_UPDATED, Boolean.class).post(true);

        logDebug("New email: " + email);
        nVEmail.setText(email);
        String oldEmail = dbH.getMyEmail();
        if (oldEmail != null) {
            logDebug("Old email: " + oldEmail);
            try {
                File avatarFile = CacheFolderManager.buildAvatarFile(this, oldEmail + ".jpg");
                if (isFileAvailable(avatarFile)) {
                    File newFile = CacheFolderManager.buildAvatarFile(this, email + ".jpg");
                    if (newFile != null) {
                        boolean result = avatarFile.renameTo(newFile);
                        if (result) {
                            logDebug("The avatar file was correctly renamed");
                        }
                    }
                }
            } catch (Exception e) {
                logError("EXCEPTION renaming the avatar on changing email", e);
            }
        } else {
            logError("ERROR: Old email is NULL");
        }

        dbH.saveMyEmail(email);
    }

    public void onNodesCloudDriveUpdate() {
        logDebug("onNodesCloudDriveUpdate");

        rubbishBinFragment = (RubbishBinFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
        if (rubbishBinFragment != null) {
            rubbishBinFragment.hideMultipleSelect();

            if (isClearRubbishBin) {
                isClearRubbishBin = false;
                parentHandleRubbish = megaApi.getRubbishNode().getHandle();
                ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRubbishNode(),
                        sortOrderManagement.getOrderCloud());
                rubbishBinFragment.setNodes(nodes);
                rubbishBinFragment.getRecyclerView().invalidate();
            } else {
                refreshRubbishBin();
            }
        }
        if (pagerOfflineFragment != null) {
            pagerOfflineFragment.refreshNodes();
        }

        refreshCloudDrive();
    }

    public void onNodesInboxUpdate() {
        inboxFragment = (InboxFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.INBOX.getTag());
        if (inboxFragment != null) {
            inboxFragment.hideMultipleSelect();
            inboxFragment.refresh();
        }
    }

    public void onNodesSearchUpdate() {
        if (getSearchFragment() != null) {
            //stop from query for empty string.
            textSubmitted = true;
            searchFragment.refresh();
        }
    }

    public void refreshIncomingShares() {
        if (!isIncomingAdded()) return;

        incomingSharesFragment.hideMultipleSelect();
        incomingSharesFragment.refresh();
    }

    private void refreshOutgoingShares() {
        if (!isOutgoingAdded()) return;

        outgoingSharesFragment.hideMultipleSelect();
        outgoingSharesFragment.refresh();
    }

    private void refreshLinks() {
        if (!isLinksAdded()) return;

        linksFragment.refresh();
    }

    public void refreshInboxList() {
        inboxFragment = (InboxFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.INBOX.getTag());
        if (inboxFragment != null) {
            inboxFragment.getRecyclerView().invalidate();
        }
    }

    public void onNodesSharedUpdate() {
        logDebug("onNodesSharedUpdate");

        refreshOutgoingShares();
        refreshIncomingShares();
        refreshLinks();

        refreshSharesPageAdapter();
    }

    public void updateNodes(List<MegaNode> updatedNodes) {
        dismissAlertDialogIfExists(statusDialog);

        boolean updateContacts = false;

        if (updatedNodes != null) {
            //Verify is it is a new item to the inbox
            for (int i = 0; i < updatedNodes.size(); i++) {
                MegaNode updatedNode = updatedNodes.get(i);

                if (!updateContacts) {
                    if (updatedNode.isInShare()) {
                        updateContacts = true;

                        if (drawerItem == DrawerItem.SHARED_ITEMS
                                && getTabItemShares() == INCOMING_TAB && parentHandleIncoming == updatedNode.getHandle()) {
                            getNodeUseCase.get(parentHandleIncoming)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe((result, throwable) -> {
                                        if (throwable != null) {
                                            decreaseDeepBrowserTreeIncoming();
                                            parentHandleIncoming = INVALID_HANDLE;
                                            hideTabs(false, INCOMING_TAB);
                                            refreshIncomingShares();
                                        }
                                    });
                        }
                    }
                }

                if (updatedNode.getParentHandle() == inboxNode.getHandle()) {
                    logDebug("New element to Inbox!!");
                    setInboxNavigationDrawer();
                }
            }
        }

        onNodesCloudDriveUpdate();

        onNodesSearchUpdate();

        onNodesSharedUpdate();

        onNodesInboxUpdate();

        checkCameraUploadFolder(false, updatedNodes);

        refreshCUNodes();

        LiveEventBus.get(EVENT_NODES_CHANGE).post(true);

        // Invalidate the menu will collapse/expand the search view and set the query text to ""
        // (call onQueryTextChanged) (BTW, SearchFragment uses textSubmitted to avoid the query
        // text changed to "" for once)
        if (drawerItem == DrawerItem.HOMEPAGE) return;

        setToolbarTitle();
        supportInvalidateOptionsMenu();
    }

    public void updateContactRequests(List<MegaContactRequest> requests) {
        logDebug("onContactRequestsUpdate");

        if (requests != null) {
            for (int i = 0; i < requests.size(); i++) {
                MegaContactRequest req = requests.get(i);
                if (req.isOutgoing()) {
                    logDebug("SENT REQUEST");
                    logDebug("STATUS: " + req.getStatus() + ", Contact Handle: " + req.getHandle());
                    if (req.getStatus() == MegaContactRequest.STATUS_ACCEPTED) {
                        cC.addContactDB(req.getTargetEmail());
                    }
                } else {
                    logDebug("RECEIVED REQUEST");
                    setContactTitleSection();
                    logDebug("STATUS: " + req.getStatus() + " Contact Handle: " + req.getHandle());
                    if (req.getStatus() == MegaContactRequest.STATUS_ACCEPTED) {
                        cC.addContactDB(req.getSourceEmail());
                    }
                }
            }
        }

        updateNavigationToolbarIcon();
    }

    /**
     * Pauses a transfer.
     *
     * @param mT the transfer to pause
     */
    public void pauseIndividualTransfer(MegaTransfer mT) {
        if (mT == null) {
            logWarning("Transfer object is null.");
            return;
        }

        logDebug("Resume transfer - Node handle: " + mT.getNodeHandle());
        megaApi.pauseTransfer(mT, mT.getState() != MegaTransfer.STATE_PAUSED, managerActivity);
    }

    /**
     * Shows a warning to ensure if it is sure of remove all completed transfers.
     */
    public void showConfirmationClearCompletedTransfers() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setMessage(R.string.confirmation_to_clear_completed_transfers)
                .setPositiveButton(R.string.general_clear, (dialog, which) -> {
                    dbH.emptyCompletedTransfers();

                    if (isTransfersCompletedAdded()) {
                        completedTransfersFragment.clearCompletedTransfers();
                    }
                    supportInvalidateOptionsMenu();
                })
                .setNegativeButton(R.string.general_dismiss, null);

        confirmationTransfersDialog = builder.create();
        setConfirmationTransfersDialogNotCancellableAndShow();
    }

    /**
     * Shows a warning to ensure if it is sure of cancel selected transfers.
     */
    public void showConfirmationCancelSelectedTransfers(List<MegaTransfer> selectedTransfers) {
        if (selectedTransfers == null || selectedTransfers.isEmpty()) {
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setMessage(getResources().getQuantityString(R.plurals.cancel_selected_transfers, selectedTransfers.size()))
                .setPositiveButton(R.string.button_continue, (dialog, which) -> {
                    CancelTransferListener cancelTransferListener = new CancelTransferListener(managerActivity);
                    cancelTransferListener.cancelTransfers(selectedTransfers);

                    if (isTransfersInProgressAdded()) {
                        transfersFragment.destroyActionMode();
                    }
                })
                .setNegativeButton(R.string.general_dismiss, null);

        confirmationTransfersDialog = builder.create();
        setConfirmationTransfersDialogNotCancellableAndShow();
    }

    /**
     * Shows a warning to ensure if it is sure of cancel all transfers.
     */
    public void showConfirmationCancelAllTransfers() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setMessage(getResources().getString(R.string.cancel_all_transfer_confirmation))
                .setPositiveButton(R.string.cancel_all_action, (dialog, which) -> {
                    megaApi.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD, managerActivity);
                    megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, managerActivity);
                    fireCancelCameraUploadJob(ManagerActivity.this);
                    refreshFragment(FragmentTag.TRANSFERS.getTag());
                    refreshFragment(FragmentTag.COMPLETED_TRANSFERS.getTag());
                })
                .setNegativeButton(R.string.general_dismiss, null);

        confirmationTransfersDialog = builder.create();
        setConfirmationTransfersDialogNotCancellableAndShow();
    }

    private void setConfirmationTransfersDialogNotCancellableAndShow() {
        if (confirmationTransfersDialog != null) {
            confirmationTransfersDialog.setCancelable(false);
            confirmationTransfersDialog.setCanceledOnTouchOutside(false);
            confirmationTransfersDialog.show();
        }
    }

    @Override
    public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
        logDebug("onTransferStart: " + transfer.getNotificationNumber() + "-" + transfer.getNodeHandle() + " - " + transfer.getTag());

        if (transfer.isStreamingTransfer() || isBackgroundTransfer(transfer)) {
            return;
        }

        if (transferCallback < transfer.getNotificationNumber()) {
            transferCallback = transfer.getNotificationNumber();
            long now = Calendar.getInstance().getTimeInMillis();
            lastTimeOnTransferUpdate = now;

            if (!transfer.isFolderTransfer()) {
                transfersInProgress.add(transfer.getTag());

                if (isTransfersInProgressAdded()) {
                    transfersFragment.transferStart(transfer);
                }
            }
        }
    }

    @Override
    public void onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError e) {
        logDebug("onTransferFinish: " + transfer.getNodeHandle() + " - " + transfer.getTag() + "- " + transfer.getNotificationNumber());
        if (transfer.isStreamingTransfer() || isBackgroundTransfer(transfer)) {
            return;
        }

        if (transferCallback < transfer.getNotificationNumber()) {

            transferCallback = transfer.getNotificationNumber();
            long now = Calendar.getInstance().getTimeInMillis();
            lastTimeOnTransferUpdate = now;

            if (!transfer.isFolderTransfer()) {
                ListIterator li = transfersInProgress.listIterator();
                int index = 0;
                while (li.hasNext()) {
                    Integer next = (Integer) li.next();
                    if (next == transfer.getTag()) {
                        index = li.previousIndex();
                        break;
                    }
                }

                if (!transfersInProgress.isEmpty()) {
                    transfersInProgress.remove(index);
                    logDebug("The transfer with index " + index + " has been removed, left: " + transfersInProgress.size());
                } else {
                    logDebug("The transferInProgress is EMPTY");
                }

                int pendingTransfers = megaApi.getNumPendingDownloads() + megaApi.getNumPendingUploads();

                if (pendingTransfers <= 0) {
                    if (pauseTransfersMenuIcon != null) {
                        pauseTransfersMenuIcon.setVisible(false);
                        playTransfersMenuIcon.setVisible(false);
                        cancelAllTransfersMenuItem.setVisible(false);
                    }
                }

                onNodesCloudDriveUpdate();
                onNodesInboxUpdate();
                onNodesSearchUpdate();
                onNodesSharedUpdate();
                LiveEventBus.get(EVENT_NODES_CHANGE).post(false);

                if (isTransfersInProgressAdded()) {
                    transfersFragment.transferFinish(transfer.getTag());
                }
            }
        }
    }

    @Override
    public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {

        if (transfer.isStreamingTransfer() || isBackgroundTransfer(transfer)) {
            return;
        }

        long now = Calendar.getInstance().getTimeInMillis();
        if ((now - lastTimeOnTransferUpdate) > ONTRANSFERUPDATE_REFRESH_MILLIS) {
            logDebug("Update onTransferUpdate: " + transfer.getNodeHandle() + " - " + transfer.getTag() + " - " + transfer.getNotificationNumber());
            lastTimeOnTransferUpdate = now;

            if (!transfer.isFolderTransfer() && transferCallback < transfer.getNotificationNumber()) {
                transferCallback = transfer.getNotificationNumber();

                if (isTransfersInProgressAdded()) {
                    transfersFragment.transferUpdate(transfer);
                }
            }
        }
    }

    @Override
    public void onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e) {
        logWarning("onTransferTemporaryError: " + transfer.getNodeHandle() + " - " + transfer.getTag());

        if (e.getErrorCode() == MegaError.API_EOVERQUOTA) {
            if (e.getValue() != 0) {
                logDebug("TRANSFER OVERQUOTA ERROR: " + e.getErrorCode());
                transfersWidget.update();
            } else {
                logWarning("STORAGE OVERQUOTA ERROR: " + e.getErrorCode());
                //work around - SDK does not return over quota error for folder upload,
                //so need to be notified from global listener
                if (transfer.getType() == MegaTransfer.TYPE_UPLOAD) {
                    if (transfer.isForeignOverquota()) {
                        return;
                    }

                    logDebug("Over quota");
                    Intent intent = new Intent(this, UploadService.class);
                    intent.setAction(ACTION_OVERQUOTA_STORAGE);
                    ContextCompat.startForegroundService(this, intent);
                }
            }
        }
    }

    @Override
    public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer) {
        logDebug("onTransferData");
        return true;
    }

    public boolean isList() {
        return isList;
    }

    public void setList(boolean isList) {
        this.isList = isList;
    }

    public boolean isListCameraUploads() {
        return false;
    }

    public boolean getFirstLogin() {
        return firstLogin;
    }

    public void setFirstLogin(boolean flag) {
        firstLogin = flag;
    }

    public boolean getAskPermissions() {
        return askPermissions;
    }

    public String getPathNavigationOffline() {
        return pathNavigationOffline;
    }

    public void setPathNavigationOffline(String pathNavigationOffline) {
        logDebug("setPathNavigationOffline: " + pathNavigationOffline);
        this.pathNavigationOffline = pathNavigationOffline;
    }

    public int getDeepBrowserTreeIncoming() {
        return deepBrowserTreeIncoming;
    }

    public void setDeepBrowserTreeIncoming(int deep) {
        deepBrowserTreeIncoming = deep;
    }

    public void increaseDeepBrowserTreeIncoming() {
        deepBrowserTreeIncoming++;
    }

    public void decreaseDeepBrowserTreeIncoming() {
        deepBrowserTreeIncoming--;
    }

    public int getDeepBrowserTreeOutgoing() {
        return deepBrowserTreeOutgoing;
    }

    public void setDeepBrowserTreeOutgoing(int deep) {
        this.deepBrowserTreeOutgoing = deep;
    }

    public void increaseDeepBrowserTreeOutgoing() {
        deepBrowserTreeOutgoing++;
    }

    public void decreaseDeepBrowserTreeOutgoing() {
        deepBrowserTreeOutgoing--;
    }

    public void setDeepBrowserTreeLinks(int deepBrowserTreeLinks) {
        this.deepBrowserTreeLinks = deepBrowserTreeLinks;
    }

    public int getDeepBrowserTreeLinks() {
        return deepBrowserTreeLinks;
    }

    public void increaseDeepBrowserTreeLinks() {
        deepBrowserTreeLinks++;
    }

    public void decreaseDeepBrowserTreeLinks() {
        deepBrowserTreeLinks--;
    }

    public DrawerItem getDrawerItem() {
        return drawerItem;
    }

    public void setDrawerItem(DrawerItem drawerItem) {
        this.drawerItem = drawerItem;
    }

    public int getTabItemShares() {
        if (viewPagerShares == null) return ERROR_TAB;

        return viewPagerShares.getCurrentItem();
    }

    private int getTabItemTransfers() {
        return viewPagerTransfers == null ? ERROR_TAB : viewPagerTransfers.getCurrentItem();
    }

    public void setTabItemShares(int index) {
        viewPagerShares.setCurrentItem(index);
    }

    public void showChatPanel(MegaChatListItem chat) {
        logDebug("showChatPanel");

        if (chat == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedChatItemId = chat.getChatId();
        bottomSheetDialogFragment = new ChatBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void updateUserNameNavigationView(String fullName) {
        logDebug("updateUserNameNavigationView");

        nVDisplayName.setText(fullName);
        setProfileAvatar();
    }

    public void hideFabButton() {
        initFabButtonShow = false;
        fabButton.hide();
    }

    /**
     * Hides the fabButton icon when scrolling.
     */
    public void hideFabButtonWhenScrolling() {
        fabButton.hide();
    }

    /**
     * Shows the fabButton icon.
     */
    public void showFabButtonAfterScrolling() {
        fabButton.show();
    }

    /**
     * Updates the fabButton icon and shows it.
     */
    private void updateFabAndShow() {
        fabButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_add_white));
        fabButton.show();
    }

    /**
     * Shows or hides the fabButton depending on the current section.
     */
    public void showFabButton() {
        initFabButtonShow = true;

        if (drawerItem == null) {
            return;
        }

        switch (drawerItem) {
            case CLOUD_DRIVE:
                if (!isInMDMode) {
                    updateFabAndShow();
                }
                break;

            case SHARED_ITEMS:
                switch (getTabItemShares()) {
                    case INCOMING_TAB:
                        if (!isIncomingAdded()) break;

                        MegaNode parentNodeInSF = megaApi.getNodeByHandle(parentHandleIncoming);
                        if (deepBrowserTreeIncoming <= 0 || parentNodeInSF == null) {
                            hideFabButton();
                            break;
                        }

                        switch (megaApi.getAccess(parentNodeInSF)) {
                            case MegaShare.ACCESS_OWNER:
                            case MegaShare.ACCESS_READWRITE:
                            case MegaShare.ACCESS_FULL:
                                updateFabAndShow();
                                break;

                            case ACCESS_READ:
                                hideFabButton();
                                break;
                        }
                        break;

                    case OUTGOING_TAB:
                        if (!isOutgoingAdded()) break;

                        if (deepBrowserTreeOutgoing <= 0) {
                            hideFabButton();
                        } else {
                            updateFabAndShow();
                        }
                        break;

                    case LINKS_TAB:
                        if (!isLinksAdded()) break;

                        if (deepBrowserTreeLinks <= 0) {
                            hideFabButton();
                        } else {
                            updateFabAndShow();
                        }
                        break;

                    default:
                        hideFabButton();
                }
                break;

            case CHAT:
                if (megaChatApi == null) {
                    hideFabButton();
                    break;
                }

                updateFabAndShow();
                break;

            default:
                hideFabButton();
        }
    }

    public AndroidCompletedTransfer getSelectedTransfer() {
        return selectedTransfer;
    }

    public MegaNode getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(MegaNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    public MegaContactAdapter getSelectedUser() {
        return selectedUser;
    }


    public MegaContactRequest getSelectedRequest() {
        return selectedRequest;
    }

    public MegaOffline getSelectedOfflineNode() {
        return selectedOfflineNode;
    }

    public void setSelectedAccountType(int selectedAccountType) {
        this.selectedAccountType = selectedAccountType;
    }

    private void onChatListItemUpdate(MegaChatListItem item) {
        if (item != null) {
            logDebug("Chat ID:" + item.getChatId());
            if (item.isPreview()) {
                return;
            }
        } else {
            logWarning("Item NULL");
            return;
        }

        recentChatsFragment = (RecentChatsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
        if (recentChatsFragment != null) {
            recentChatsFragment.listItemUpdate(item);
        }

        if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT)) {
            logDebug("Change unread count: " + item.getUnreadCount());
            setChatBadge();
            updateNavigationToolbarIcon();
        }
    }

    private void onChatOnlineStatusUpdate(long userHandle, int status, boolean inProgress) {
        logDebug("Status: " + status + ", In Progress: " + inProgress);
        if (inProgress) {
            status = -1;
        }

        if (megaChatApi != null) {
            recentChatsFragment = (RecentChatsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
            if (userHandle == megaChatApi.getMyUserHandle()) {
                logDebug("My own status update");
                setContactStatus();
                if (drawerItem == DrawerItem.CHAT) {
                    if (recentChatsFragment != null) {
                        recentChatsFragment.onlineStatusUpdate(status);
                    }
                }
            } else {
                logDebug("Status update for the user: " + userHandle);
                recentChatsFragment = (RecentChatsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
                if (recentChatsFragment != null) {
                    logDebug("Update Recent chats view");
                    recentChatsFragment.contactStatusUpdate(userHandle, status);
                }
            }
        }
    }

	private void onChatConnectionStateUpdate(long chatid, int newState) {
		logDebug("Chat ID: " + chatid + ", New state: " + newState);
		if (newState == MegaChatApi.CHAT_CONNECTION_ONLINE && chatid == -1) {
			logDebug("Online Connection: " + chatid);
			recentChatsFragment = (RecentChatsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
			if (recentChatsFragment != null) {
				recentChatsFragment.setChats();
				if(drawerItem == DrawerItem.CHAT) {
					recentChatsFragment.setStatus();
				}
			}
		}

        MegaChatApiJava api = MegaApplication.getInstance().getMegaChatApi();
        MegaChatRoom chatRoom = api.getChatRoom(chatid);
        if (isChatConnectedInOrderToInitiateACall(newState, chatRoom)) {
            startCallWithChatOnline(this, api.getChatRoom(chatid));
        }
    }

    public void copyError() {
        try {
            dismissAlertDialogIfExists(statusDialog);
            showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied), -1);
        } catch (Exception ex) {
        }
    }

    public void setDrawerLockMode(boolean locked) {
        if (locked) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    /**
     * This method is used to change the elevation of the AppBarLayout when
     * scrolling the RecyclerView
     *
     * @param withElevation true if need elevation, false otherwise
     */
    public void changeAppBarElevation(boolean withElevation) {
        changeAppBarElevation(withElevation, ELEVATION_SCROLL);
    }

    /**
     * This method is used to change the elevation of the AppBarLayout for some reason
     *
     * @param withElevation true if need elevation, false otherwise
     * @param cause         for what cause adding/removing elevation. Only if mElevationCause(cause bitmap)
     *                      is zero will the elevation being eliminated
     */
    public void changeAppBarElevation(boolean withElevation, int cause) {
        if (withElevation) {
            mElevationCause |= cause;
        } else if ((mElevationCause & cause) > 0) {
            mElevationCause ^= cause;
        }

        // In landscape mode, if no call in progress layout ("Tap to return call"), then don't show elevation
        if (mElevationCause == ELEVATION_CALL_IN_PROGRESS && callInProgressLayout.getVisibility() != View.VISIBLE)
            return;

        // If any Tablayout is visible, set the background of the toolbar to transparent (or its elevation
        // overlay won't be correctly set via AppBarLayout) and then set the elevation of AppBarLayout,
        // in this way, both Toolbar and TabLayout would have expected elevation overlay.
        // If TabLayout is invisible, directly set toolbar's color for the elevation effect. Set AppBarLayout
        // elevation in this case, a crack would appear between toolbar and ChatRecentFragment's Appbarlayout, for example.
        float elevation = getResources().getDimension(R.dimen.toolbar_elevation);
        int toolbarElevationColor = ColorUtils.getColorForElevation(this, elevation);
        int transparentColor = ContextCompat.getColor(this, android.R.color.transparent);
        boolean onlySetToolbar = Util.isDarkMode(this) && !mShowAnyTabLayout;
        boolean enableCUVisible = cuLayout.getVisibility() == View.VISIBLE;

        if (mElevationCause > 0) {
            if (onlySetToolbar) {
                toolbar.setBackgroundColor(toolbarElevationColor);
                if (enableCUVisible) cuLayout.setBackgroundColor(toolbarElevationColor);
            } else {
                toolbar.setBackgroundColor(transparentColor);
                if (enableCUVisible) cuLayout.setBackground(null);
                abL.setElevation(elevation);
            }
        } else {
            toolbar.setBackgroundColor(transparentColor);
            if (enableCUVisible) cuLayout.setBackground(null);
            abL.setElevation(0);
        }

        ColorUtils.changeStatusBarColorForElevation(this,
                mElevationCause > 0 && !isInMainHomePage());
    }

    public long getParentHandleInbox() {
        return parentHandleInbox;
    }

    public void setContactTitleSection() {
        ArrayList<MegaContactRequest> requests = megaApi.getIncomingContactRequests();

        if (contactsSectionText != null) {
            if (requests != null) {
                int pendingRequest = requests.size();
                if (pendingRequest == 0) {
                    contactsSectionText.setText(getString(R.string.section_contacts));
                } else {
                    setFormattedContactTitleSection(pendingRequest, true);
                }
            }
        }
    }

    void setFormattedContactTitleSection(int pendingRequest, boolean enable) {
        String textToShow = String.format(getString(R.string.section_contacts_with_notification), pendingRequest);
        try {
            if (enable) {
                textToShow = textToShow.replace("[A]", "<font color=\'" + ColorUtils.getColorHexString(this, R.color.red_600_red_300) + "\'>");
            } else {
                textToShow = textToShow.replace("[A]", "<font color=\'#ffcccc\'>");
            }
            textToShow = textToShow.replace("[/A]", "</font>");
        } catch (Exception e) {
            logError("Formatted string: " + textToShow, e);
        }

        Spanned result = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);
        }
        contactsSectionText.setText(result);
    }

    public void setNotificationsTitleSection() {
        int unread = megaApi.getNumUnreadUserAlerts();

        if (unread == 0) {
            notificationsSectionText.setText(getString(R.string.title_properties_chat_contact_notifications));
        } else {
            setFormattedNotificationsTitleSection(unread, true);
        }
    }

    void setFormattedNotificationsTitleSection(int unread, boolean enable) {
        String textToShow = String.format(getString(R.string.section_notification_with_unread), unread);
        try {
            if (enable) {
                textToShow = textToShow.replace("[A]", "<font color=\'"
                        + ColorUtils.getColorHexString(this, R.color.red_600_red_300)
                        + "\'>");
            } else {
                textToShow = textToShow.replace("[A]", "<font color=\'#ffcccc\'>");
            }
            textToShow = textToShow.replace("[/A]", "</font>");
        } catch (Exception e) {
            logError("Formatted string: " + textToShow, e);
        }

        Spanned result = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);
        }
        notificationsSectionText.setText(result);
    }

    public void setChatBadge() {
        if (megaChatApi != null) {
            int numberUnread = megaChatApi.getUnreadChats();
            if (numberUnread == 0) {
                chatBadge.setVisibility(View.GONE);
            } else {
                chatBadge.setVisibility(View.VISIBLE);
                if (numberUnread > 9) {
                    ((TextView) chatBadge.findViewById(R.id.chat_badge_text)).setText("9+");
                } else {
                    ((TextView) chatBadge.findViewById(R.id.chat_badge_text)).setText("" + numberUnread);
                }
            }
        } else {
            chatBadge.setVisibility(View.GONE);
        }
    }

    private void setCallBadge() {
        if (!isOnline(this) || megaChatApi == null || megaChatApi.getNumCalls() <= 0 || (megaChatApi.getNumCalls() == 1 && participatingInACall())) {
            callBadge.setVisibility(View.GONE);
            return;
        }

        callBadge.setVisibility(View.VISIBLE);
    }

    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    public void refreshMenu() {
        logDebug("refreshMenu");
        supportInvalidateOptionsMenu();
    }

    public boolean is2FAEnabled() {
        return is2FAEnabled;
    }

    /**
     * Sets or removes the layout behaviour to hide the bottom view when scrolling.
     *
     * @param enable True if should set the behaviour, false if should remove it.
     */
    public void enableHideBottomViewOnScroll(boolean enable) {
        LinearLayout layout = findViewById(R.id.container_bottom);
        if (layout == null || isInImagesPage()) {
            return;
        }

        final CoordinatorLayout.LayoutParams fParams
                = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        fParams.setMargins(0, 0, 0, enable ? 0 : getResources().getDimensionPixelSize(R.dimen.bottom_navigation_view_height));
        fragmentLayout.setLayoutParams(fParams);

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) layout.getLayoutParams();
        params.setBehavior(enable ? new CustomHideBottomViewOnScrollBehaviour<LinearLayout>() : null);
        layout.setLayoutParams(params);
    }

    /**
     * Shows all the content of bottom view.
     */
    public void showBottomView() {
        LinearLayout bottomView = findViewById(R.id.container_bottom);
        if (bottomView == null || fragmentLayout == null || isInImagesPage()) {
            return;
        }

        bottomView.animate().translationY(0).setDuration(175)
                .withStartAction(() -> bottomView.setVisibility(View.VISIBLE))
                .start();
    }

    /**
     * Shows or hides the bottom view and animates the transition.
     *
     * @param hide True if should hide it, false if should show it.
     */
    public void animateBottomView(boolean hide) {
        LinearLayout bottomView = findViewById(R.id.container_bottom);
        if (bottomView == null || fragmentLayout == null) {
            return;
        }

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) fragmentLayout.getLayoutParams();

        if (hide && bottomView.getVisibility() == View.VISIBLE) {
            bottomView.animate().translationY(bottomView.getHeight()).setDuration(ANIMATION_DURATION)
                    .withStartAction(() -> params.bottomMargin = 0)
                    .withEndAction(() -> bottomView.setVisibility(View.GONE)).start();
        } else if (!hide && bottomView.getVisibility() == View.GONE) {
            int bottomMargin = getResources().getDimensionPixelSize(R.dimen.bottom_navigation_view_height);

            bottomView.animate().translationY(0).setDuration(ANIMATION_DURATION)
                    .withStartAction(() -> bottomView.setVisibility(View.VISIBLE))
                    .withEndAction(() -> params.bottomMargin = bottomMargin)
                    .start();
        }
    }

    public void showHideBottomNavigationView(boolean hide) {
        if (bNV == null) return;

        final CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        int height = getResources().getDimensionPixelSize(R.dimen.bottom_navigation_view_height);

        if (hide && bNV.getVisibility() == View.VISIBLE) {
            updateMiniAudioPlayerVisibility(false);
            params.setMargins(0, 0, 0, 0);
            fragmentLayout.setLayoutParams(params);
            bNV.animate().translationY(height).setDuration(ANIMATION_DURATION).withEndAction(() ->
                    bNV.setVisibility(View.GONE)
            ).start();
        } else if (!hide && bNV.getVisibility() == View.GONE) {
            bNV.animate().translationY(0).setDuration(ANIMATION_DURATION).withStartAction(() ->
                    bNV.setVisibility(View.VISIBLE)
            ).withEndAction(() -> {
                updateMiniAudioPlayerVisibility(true);
                params.setMargins(0, 0, 0, height);
                fragmentLayout.setLayoutParams(params);
            }).start();
        }

        updateTransfersWidgetPosition(hide);
    }

    public void markNotificationsSeen(boolean fromAndroidNotification) {
        logDebug("fromAndroidNotification: " + fromAndroidNotification);

        if (fromAndroidNotification) {
            megaApi.acknowledgeUserAlerts();
        } else {
            if (drawerItem == DrawerItem.NOTIFICATIONS && app.isActivityVisible()) {
                megaApi.acknowledgeUserAlerts();
            }
        }
    }

    public void showKeyboardForSearch() {
        if (searchView != null) {
            showKeyboardDelayed(searchView.findViewById(R.id.search_src_text));
            searchView.requestFocus();
        }
    }

    public void hideKeyboardSearch() {
        hideKeyboard(this);
        if (searchView != null) {
            searchView.clearFocus();
        }
    }

    public void openSearchView() {
        String querySaved = searchQuery;
        if (searchMenuItem != null) {
            searchMenuItem.expandActionView();
            if (searchView != null) {
                searchView.setQuery(querySaved, false);
            }
        }
    }

    public void clearSearchViewFocus() {
        if (searchView != null) {
            searchView.clearFocus();
        }
    }

    public void requestSearchViewFocus() {
        if (searchView == null || textSubmitted) {
            return;
        }

        searchView.setIconified(false);
    }

    public boolean isValidSearchQuery() {
        return searchQuery != null && !searchQuery.isEmpty();
    }

    public void openSearchFolder(MegaNode node) {
        switch (drawerItem) {
            case HOMEPAGE:
                // Redirect to Cloud drive.
                selectDrawerItem(DrawerItem.CLOUD_DRIVE);
            case CLOUD_DRIVE:
                setParentHandleBrowser(node.getHandle());
                refreshFragment(FragmentTag.CLOUD_DRIVE.getTag());
                break;
            case SHARED_ITEMS:
                if (viewPagerShares == null || sharesPageAdapter == null) break;

                if (getTabItemShares() == INCOMING_TAB) {
                    setParentHandleIncoming(node.getHandle());
                    increaseDeepBrowserTreeIncoming();
                } else if (getTabItemShares() == OUTGOING_TAB) {
                    setParentHandleOutgoing(node.getHandle());
                    increaseDeepBrowserTreeOutgoing();
                } else if (getTabItemShares() == LINKS_TAB) {
                    setParentHandleLinks(node.getHandle());
                    increaseDeepBrowserTreeLinks();
                }
                refreshSharesPageAdapter();

                break;
            case INBOX:
                setParentHandleInbox(node.getHandle());
                refreshFragment(FragmentTag.INBOX.getTag());
                break;
        }
    }

    public void closeSearchView() {
        if (searchMenuItem != null && searchMenuItem.isActionViewExpanded()) {
            searchMenuItem.collapseActionView();
        }
    }

    public void setTextSubmitted() {
        if (searchView != null) {
            if (!isValidSearchQuery()) return;
            searchView.setQuery(searchQuery, true);
        }
    }

    public boolean isSearchOpen() {
        return searchQuery != null && searchExpand;
    }

    private void refreshAddPhoneNumberButton() {
        navigationDrawerAddPhoneContainer.setVisibility(View.GONE);
    }

    public void showAddPhoneNumberInMenu() {
        if (megaApi == null) {
            return;
        }
        if (canVoluntaryVerifyPhoneNumber()) {
            if (megaApi.isAchievementsEnabled()) {
                String message = String.format(getString(R.string.sms_add_phone_number_dialog_msg_achievement_user), myAccountInfo.getBonusStorageSMS());
                addPhoneNumberLabel.setText(message);
            } else {
                addPhoneNumberLabel.setText(R.string.sms_add_phone_number_dialog_msg_non_achievement_user);
            }
            navigationDrawerAddPhoneContainer.setVisibility(View.VISIBLE);
        } else {
            navigationDrawerAddPhoneContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onTrimMemory(int level) {
        // Determine which lifecycle or system event was raised.
        //we will stop creating thumbnails while the phone is running low on memory to prevent OOM
        logDebug("Level: " + level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            logWarning("Low memory");
            ThumbnailUtils.isDeviceMemoryLow = true;
        } else {
            logDebug("Memory OK");
            ThumbnailUtils.isDeviceMemoryLow = false;
        }
    }

    private void setSearchDrawerItem() {
        if (drawerItem == DrawerItem.SEARCH) return;

        searchDrawerItem = drawerItem;
        searchSharedTab = getTabItemShares();

        drawerItem = DrawerItem.SEARCH;
    }

    public DrawerItem getSearchDrawerItem() {
        return searchDrawerItem;
    }

    /**
     * This method sets "Tap to return to call" banner when there is a call in progress
     * and it is in Cloud Drive section, Recents section, Incoming section, Outgoing section or in the chats list.
     */
    private void setCallWidget() {
        setCallBadge();

        if (drawerItem == DrawerItem.SEARCH
                || drawerItem == DrawerItem.TRANSFERS || drawerItem == DrawerItem.NOTIFICATIONS
                || drawerItem == DrawerItem.HOMEPAGE || !isScreenInPortrait(this)) {
            hideCallWidget(this, callInProgressChrono, callInProgressLayout);
            return;
        }

        showCallLayout(this, callInProgressLayout, callInProgressChrono, callInProgressText);
    }

    public void homepageToSearch() {
        hideItemsWhenSearchSelected();
        searchMenuItem.expandActionView();
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public int getSearchSharedTab() {
        return searchSharedTab;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
        if (this.searchView != null) {
            this.searchView.setQuery(searchQuery, false);
        }
    }

    public long getParentHandleIncoming() {
        return parentHandleIncoming;
    }

    public long getParentHandleOutgoing() {
        return parentHandleOutgoing;
    }

    public long getParentHandleRubbish() {
        return parentHandleRubbish;
    }

    public long getParentHandleSearch() {
        return parentHandleSearch;
    }

    public long getParentHandleLinks() {
        return parentHandleLinks;
    }

    public void setParentHandleLinks(long parentHandleLinks) {
        this.parentHandleLinks = parentHandleLinks;
    }

    private SearchFragment getSearchFragment() {
        return searchFragment = (SearchFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.SEARCH.getTag());
    }

    /**
     * Removes a completed transfer from Completed tab in Transfers section.
     *
     * @param transfer the completed transfer to remove
     */
    public void removeCompletedTransfer(AndroidCompletedTransfer transfer) {
        dbH.deleteTransfer(transfer.getId());

        if (isTransfersCompletedAdded()) {
            completedTransfersFragment.transferRemoved(transfer);
        }
    }

    /**
     * Retries a transfer that finished wrongly.
     *
     * @param transfer the transfer to retry
     */
    private void retryTransfer(AndroidCompletedTransfer transfer) {
        if (transfer.getType() == MegaTransfer.TYPE_DOWNLOAD) {
            MegaNode node = megaApi.getNodeByHandle(Long.parseLong(transfer.getNodeHandle()));
            if (node == null) {
                logWarning("Node is null, not able to retry");
                return;
            }

            if (transfer.getIsOfflineFile()) {
                File offlineFile = new File(transfer.getOriginalPath());
                saveOffline(offlineFile.getParentFile(), node, ManagerActivity.this);
            } else {
                downloadNodeUseCase.download(this, node, transfer.getPath())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> logDebug("Transfer retried: " + node.getHandle()),
                                throwable -> logError("Retry transfer failed.", throwable));
            }
        } else if (transfer.getType() == MegaTransfer.TYPE_UPLOAD) {
            String originalPath = transfer.getOriginalPath();
            int lastSeparator = originalPath.lastIndexOf(SEPARATOR);
            String parentFolder = "";
            if (lastSeparator != -1) {
                parentFolder = originalPath.substring(0, lastSeparator + 1);
            }

            ArrayList<String> paths = new ArrayList<>();
            paths.add(originalPath);

            UploadServiceTask uploadServiceTask = new UploadServiceTask(parentFolder, paths, transfer.getParentHandle());
            uploadServiceTask.start();
        }

        removeCompletedTransfer(transfer);
    }

    /**
     * Opens a location of a transfer.
     *
     * @param transfer the transfer to open its location
     */
    public void openTransferLocation(AndroidCompletedTransfer transfer) {
        if (transfer.getType() == MegaTransfer.TYPE_DOWNLOAD) {
            if (transfer.getIsOfflineFile()) {
                selectDrawerItem(drawerItem = DrawerItem.HOMEPAGE);
                openFullscreenOfflineFragment(
                        removeInitialOfflinePath(transfer.getPath()) + SEPARATOR);
            } else {
                File file = new File(transfer.getPath());

                if (!isFileAvailable(file)) {
                    showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.location_not_exist), MEGACHAT_INVALID_HANDLE);
                    return;
                }

                Intent intent = new Intent(this, FileStorageActivity.class);
                intent.setAction(FileStorageActivity.Mode.BROWSE_FILES.getAction());
                intent.putExtra(FileStorageActivity.EXTRA_PATH, transfer.getPath());
                startActivity(intent);
            }
        } else if (transfer.getType() == MegaTransfer.TYPE_UPLOAD) {
            MegaNode node = megaApi.getNodeByHandle(Long.parseLong(transfer.getNodeHandle()));
            if (node == null) {
                showSnackbar(SNACKBAR_TYPE, getString(!isOnline(this) ? R.string.error_server_connection_problem
                        : R.string.warning_folder_not_exists), MEGACHAT_INVALID_HANDLE);
                return;
            }

            viewNodeInFolder(node);
        }
    }

    /**
     * Opens the location of a node.
     *
     * @param node the node to open its location
     */
    public void viewNodeInFolder(MegaNode node) {
        MegaNode parentNode = MegaNodeUtil.getRootParentNode(megaApi, node);
        if (parentNode.getHandle() == megaApi.getRootNode().getHandle()) {
            parentHandleBrowser = node.getParentHandle();
            refreshFragment(FragmentTag.CLOUD_DRIVE.getTag());
            selectDrawerItem(DrawerItem.CLOUD_DRIVE);
        } else if (parentNode.getHandle() == megaApi.getRubbishNode().getHandle()) {
            parentHandleRubbish = node.getParentHandle();
            refreshFragment(FragmentTag.RUBBISH_BIN.getTag());
            selectDrawerItem(DrawerItem.RUBBISH_BIN);
        } else if (parentNode.isInShare()) {
            parentHandleIncoming = node.getParentHandle();
            deepBrowserTreeIncoming = calculateDeepBrowserTreeIncoming(megaApi.getParentNode(node),
                    this);
            refreshFragment(FragmentTag.INCOMING_SHARES.getTag());
            indexShares = INCOMING_TAB;
            if (viewPagerShares != null) {
                viewPagerShares.setCurrentItem(indexShares);
                if (sharesPageAdapter != null) {
                    sharesPageAdapter.notifyDataSetChanged();
                }
            }
            selectDrawerItem(DrawerItem.SHARED_ITEMS);
        }
    }

    public int getStorageState() {
        return storageState;
    }

    /**
     * Shows a "transfer over quota" warning.
     */
    public void showTransfersTransferOverQuotaWarning() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        int messageResource = R.string.warning_transfer_over_quota;

        transferOverQuotaWarning = builder.setTitle(R.string.label_transfer_over_quota)
                .setMessage(getString(messageResource, getHumanizedTime(megaApi.getBandwidthOverquotaDelay())))
                .setPositiveButton(R.string.my_account_upgrade_pro, (dialog, which) -> {
                    navigateToUpgradeAccount();
                })
                .setNegativeButton(R.string.general_dismiss, null)
                .setCancelable(false)
                .setOnDismissListener(dialog -> isTransferOverQuotaWarningShown = false)
                .create();

        transferOverQuotaWarning.setCanceledOnTouchOutside(false);
        TimeUtils.createAndShowCountDownTimer(messageResource, transferOverQuotaWarning);
        transferOverQuotaWarning.show();
        isTransferOverQuotaWarningShown = true;
    }

    /**
     * Updates the position of the transfers widget.
     *
     * @param bNVHidden true if the bottom navigation view is hidden, false otherwise
     */
    public void updateTransfersWidgetPosition(boolean bNVHidden) {
        RelativeLayout transfersWidgetLayout = findViewById(R.id.transfers_widget_layout);
        if (transfersWidgetLayout == null) return;

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) transfersWidgetLayout.getLayoutParams();
        params.gravity = Gravity.END;

        if (!bNVHidden && isInMainHomePage()) {
            params.bottomMargin = Util.dp2px(TRANSFER_WIDGET_MARGIN_BOTTOM, outMetrics);
        } else {
            params.bottomMargin = 0;
        }

        transfersWidgetLayout.setLayoutParams(params);
    }

    /**
     * Updates values of TransfersManagement object after the activity comes from background.
     */
    private void checkTransferOverQuotaOnResume() {
        TransfersManagement transfersManagement = MegaApplication.getTransfersManagement();
        transfersManagement.setIsOnTransfersSection(drawerItem == DrawerItem.TRANSFERS);
        if (transfersManagement.isTransferOverQuotaNotificationShown()) {
            transfersManagement.setTransferOverQuotaBannerShown(true);
            transfersManagement.setTransferOverQuotaNotificationShown(false);
        }
    }

    /**
     * Gets the failed and cancelled transfers.
     *
     * @return A list with the failed and cancelled transfers.
     */
    public ArrayList<AndroidCompletedTransfer> getFailedAndCancelledTransfers() {
        return dbH.getFailedOrCancelledTransfers();
    }

    /**
     * Retries all the failed and cancelled transfers.
     */
    private void retryAllTransfers() {
        ArrayList<AndroidCompletedTransfer> failedOrCancelledTransfers = getFailedAndCancelledTransfers();
        dbH.removeFailedOrCancelledTransfers();
        for (AndroidCompletedTransfer transfer : failedOrCancelledTransfers) {
            if (isTransfersCompletedAdded()) {
                completedTransfersFragment.transferRemoved(transfer);
            }

            retryTransfer(transfer);
        }
    }


    /**
     * Retry a single transfer.
     *
     * @param transfer AndroidCompletedTransfer to retry.
     */
    public void retrySingleTransfer(AndroidCompletedTransfer transfer) {
        removeCompletedTransfer(transfer);
        retryTransfer(transfer);
    }

    /**
     * Checks if there are failed or cancelled transfers.
     *
     * @return True if there are failed or cancelled transfers, false otherwise.
     */
    private boolean thereAreFailedOrCancelledTransfers() {
        ArrayList<AndroidCompletedTransfer> failedOrCancelledTransfers = getFailedAndCancelledTransfers();
        return failedOrCancelledTransfers.size() > 0;
    }

    private RubbishBinFragment getRubbishBinFragment() {
        return rubbishBinFragment = (RubbishBinFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
    }

    private PhotosFragment getPhotosFragment() {
        return photosFragment = (PhotosFragment) getSupportFragmentManager()
                .findFragmentByTag(FragmentTag.PHOTOS.getTag());
    }

    private InboxFragment getInboxFragment() {
        return inboxFragment = (InboxFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.INBOX.getTag());
    }

    private RecentChatsFragment getChatsFragment() {
        return recentChatsFragment = (RecentChatsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
    }

    private PermissionsFragment getPermissionsFragment() {
        return permissionsFragment = (PermissionsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.PERMISSIONS.getTag());
    }

    public MediaDiscoveryFragment getMDFragment() {
        return mediaDiscoveryFragment;
    }

    public AlbumContentFragment getAlbumContentFragment() {
        return albumContentFragment;
    }

    /**
     * Checks whether the current screen is the main of Homepage or Documents.
     * Video / Audio / Photos do not need Fab button
     *
     * @param isShow True if Fab button should be display, False if Fab button should be hidden
     */
    private void controlFabInHomepage(Boolean isShow) {
        if (mHomepageScreen == HomepageScreen.HOMEPAGE) {
            // Control the Fab in homepage
            HomepageFragment fragment = getFragmentByType(HomepageFragment.class);
            if (fragment != null) {
                if (isShow) {
                    fragment.showFabButton();
                } else {
                    fragment.hideFabButton();
                }
            }
        } else if (mHomepageScreen == HomepageScreen.DOCUMENTS) {
            // Control the Fab in documents
            DocumentsFragment docFragment = getFragmentByType(DocumentsFragment.class);
            if (docFragment != null) {
                if (isShow) {
                    docFragment.showFabButton();
                } else {
                    docFragment.hideFabButton();
                }
            }
        }
    }

    @Override
    public void finishRenameActionWithSuccess(@NonNull String newName) {
        switch (drawerItem) {
            case CLOUD_DRIVE:
                refreshCloudDrive();
                break;
            case RUBBISH_BIN:
                refreshRubbishBin();
                break;
            case INBOX:
                refreshInboxList();
                break;
            case SHARED_ITEMS:
                onNodesSharedUpdate();
                break;
            case HOMEPAGE:
                refreshOfflineNodes();
        }
    }

    @Override
    public void actionConfirmed() {
        //No update needed
    }

    @Override
    public void onPreviewLoaded(MegaChatRequest request, boolean alreadyExist) {
        long chatId = request.getChatHandle();
        boolean isFromOpenChatPreview = request.getFlag();
        int type = request.getParamType();
        String link = request.getLink();
        if (joiningToChatLink && isTextEmpty(link) && chatId == MEGACHAT_INVALID_HANDLE) {
            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_chat_link_init_error), MEGACHAT_INVALID_HANDLE);
            resetJoiningChatLink();
            return;
        }

        if (type == LINK_IS_FOR_MEETING) {
            logDebug("It's a meeting");
            boolean linkInvalid = TextUtil.isTextEmpty(link) && chatId == MEGACHAT_INVALID_HANDLE;
            if (linkInvalid) {
                logError("Invalid link");
                return;
            }

            if (isMeetingEnded(request.getMegaHandleList())) {
                logDebug("It's a meeting, open dialog: Meeting has ended");
                new MeetingHasEndedDialogFragment(new MeetingHasEndedDialogFragment.ClickCallback() {
                    @Override
                    public void onViewMeetingChat() {
                        showChatLink(link);
                    }

                    @Override
                    public void onLeave() {
                    }
                }, false).show(getSupportFragmentManager(),
                        MeetingHasEndedDialogFragment.TAG);
            } else {
                CallUtil.checkMeetingInProgress(ManagerActivity.this, ManagerActivity.this, chatId, isFromOpenChatPreview, link, request.getMegaHandleList(), request.getText(), alreadyExist, request.getUserHandle(), passcodeManagement);
            }
        } else {
            logDebug("It's a chat");
            showChatLink(link);
        }

        dismissAlertDialogIfExists(openLinkDialog);
    }

    @Override
    public void onErrorLoadingPreview(int errorCode) {
        if (errorCode == MegaChatError.ERROR_NOENT) {
            dismissAlertDialogIfExists(openLinkDialog);
            showAlert(this, getString(R.string.invalid_chat_link), getString(R.string.title_alert_chat_link_error));
        } else {
            showOpenLinkError(true, 0);
        }
    }

    /**
     * Checks if the current screen is the main of Home.
     *
     * @return True if the current screen is the main of Home, false otherwise.
     */
    public boolean isInMainHomePage() {
        return drawerItem == DrawerItem.HOMEPAGE && mHomepageScreen == HomepageScreen.HOMEPAGE;
    }

    /**
     * Checks if the current screen is photos section of Homepage.
     *
     * @return True if the current screen is the photos, false otherwise.
     */
    public boolean isInImagesPage() {
        return drawerItem == DrawerItem.HOMEPAGE && mHomepageScreen == HomepageScreen.IMAGES;
    }

    /**
     * Checks if the current screen is Album content page.
     *
     * @return True if the current screen is Album content page, false otherwise.
     */
    public boolean isInAlbumContentPage() {
        return drawerItem == DrawerItem.PHOTOS && isInAlbumContent;
    }

    /**
     * Checks if the current screen is Photos.
     *
     * @return True if the current screen is Photos, false otherwise.
     */
    public boolean isInPhotosPage() {
        return drawerItem == DrawerItem.PHOTOS;
    }

    /**
     * Checks if the current screen is Media discovery page.
     *
     * @return True if the current screen is Media discovery page, false otherwise.
     */
    public boolean isInMDPage() {
        return drawerItem == DrawerItem.CLOUD_DRIVE && isInMDMode;
    }

    public void hideTransferWidget() {
        if (transfersWidget != null) {
            transfersWidget.hide();
        }
    }

	/**
	 * Create the instance of FileBackupManager
	 */
	private void initFileBackupManager() {
		fileBackupManager = new FileBackupManager(this, (actionType, operationType, result, handle) -> {
			if (actionType == ACTION_MOVE_TO_BACKUP) {
				if (operationType == OPERATION_EXECUTE) {
					showMovementResult(result, handle);
				}
			} else if (actionType == ACTION_BACKUP_FAB) {
				if (operationType == OPERATION_EXECUTE) {
					if (isBottomSheetDialogShown(bottomSheetDialogFragment)) return;
					bottomSheetDialogFragment = UploadBottomSheetDialogFragment.newInstance(GENERAL_UPLOAD);
					bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
				}
			} else {
				logDebug("Nothing to do for actionType = " + actionType);
			}
		});
	}

    /**
     * Receive changes to OnChatListItemUpdate, OnChatOnlineStatusUpdate and OnChatConnectionStateUpdate and make the necessary changes
     */
    private void checkChatChanges() {
        Disposable chatSubscription = getChatChangesUseCase.get()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((next) -> {
                    if (next instanceof GetChatChangesUseCase.Result.OnChatListItemUpdate) {
                        MegaChatListItem item = ((GetChatChangesUseCase.Result.OnChatListItemUpdate) next).component1();
                        onChatListItemUpdate(item);
                    }

                    if (next instanceof GetChatChangesUseCase.Result.OnChatOnlineStatusUpdate) {
                        long userHandle = ((GetChatChangesUseCase.Result.OnChatOnlineStatusUpdate) next).component1();
                        int status = ((GetChatChangesUseCase.Result.OnChatOnlineStatusUpdate) next).component2();
                        boolean inProgress = ((GetChatChangesUseCase.Result.OnChatOnlineStatusUpdate) next).component3();
                        onChatOnlineStatusUpdate(userHandle, status, inProgress);
                    }

                    if (next instanceof GetChatChangesUseCase.Result.OnChatConnectionStateUpdate) {
                        long chatid = ((GetChatChangesUseCase.Result.OnChatConnectionStateUpdate) next).component1();
                        int newState = ((GetChatChangesUseCase.Result.OnChatConnectionStateUpdate) next).component2();
                        onChatConnectionStateUpdate(chatid, newState);
                    }
                }, (error) -> logError("Error " + error));

        composite.add(chatSubscription);
    }
}
