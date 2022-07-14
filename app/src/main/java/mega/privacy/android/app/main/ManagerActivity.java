package mega.privacy.android.app.main;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_CLOSE_CHAT_AFTER_OPEN_TRANSFERS;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_TYPE;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CREDENTIALS;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_DISABLE_CU_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_DISABLE_CU_UI_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_FIRST_NAME;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_LAST_NAME;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_NICKNAME;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_PUSH_NOTIFICATION_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_CU_ATTR_CHANGE;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_TRANSFER_FINISH;
import static mega.privacy.android.app.constants.BroadcastConstants.COMPLETED_TRANSFER;
import static mega.privacy.android.app.constants.BroadcastConstants.EXTRA_USER_HANDLE;
import static mega.privacy.android.app.constants.BroadcastConstants.INVALID_ACTION;
import static mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ON_HOLD_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_FAILED_TRANSFERS;
import static mega.privacy.android.app.constants.EventConstants.EVENT_FINISH_ACTIVITY;
import static mega.privacy.android.app.constants.EventConstants.EVENT_MY_BACKUPS_FOLDER_CHANGED;
import static mega.privacy.android.app.constants.EventConstants.EVENT_NETWORK_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_REFRESH;
import static mega.privacy.android.app.constants.EventConstants.EVENT_REFRESH_PHONE_NUMBER;
import static mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_HOLD_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_TRANSFER_OVER_QUOTA;
import static mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_VIEW_MODE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_USER_EMAIL_UPDATED;
import static mega.privacy.android.app.constants.EventConstants.EVENT_USER_NAME_UPDATED;
import static mega.privacy.android.app.constants.IntentConstants.ACTION_OPEN_ACHIEVEMENTS;
import static mega.privacy.android.app.constants.IntentConstants.EXTRA_ACCOUNT_TYPE;
import static mega.privacy.android.app.constants.IntentConstants.EXTRA_ASK_PERMISSIONS;
import static mega.privacy.android.app.constants.IntentConstants.EXTRA_FIRST_LOGIN;
import static mega.privacy.android.app.constants.IntentConstants.EXTRA_NEW_ACCOUNT;
import static mega.privacy.android.app.constants.IntentConstants.EXTRA_UPGRADE_ACCOUNT;
import static mega.privacy.android.app.data.extensions.MegaTransferKt.isBackgroundTransfer;
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
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.AvatarUtil.getColorAvatar;
import static mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar;
import static mega.privacy.android.app.utils.CallUtil.hideCallMenuItem;
import static mega.privacy.android.app.utils.CallUtil.hideCallWidget;
import static mega.privacy.android.app.utils.CallUtil.isMeetingEnded;
import static mega.privacy.android.app.utils.CallUtil.isNecessaryDisableLocalCamera;
import static mega.privacy.android.app.utils.CallUtil.openMeetingToCreate;
import static mega.privacy.android.app.utils.CallUtil.participatingInACall;
import static mega.privacy.android.app.utils.CallUtil.returnActiveCall;
import static mega.privacy.android.app.utils.CallUtil.setCallMenuItem;
import static mega.privacy.android.app.utils.CallUtil.showCallLayout;
import static mega.privacy.android.app.utils.CallUtil.showConfirmationInACall;
import static mega.privacy.android.app.utils.CallUtil.showConfirmationOpenCamera;
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
import static mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION;
import static mega.privacy.android.app.utils.FileUtil.OLD_MK_FILE;
import static mega.privacy.android.app.utils.FileUtil.OLD_RK_FILE;
import static mega.privacy.android.app.utils.FileUtil.buildExternalStorageFile;
import static mega.privacy.android.app.utils.FileUtil.createTemporalTextFile;
import static mega.privacy.android.app.utils.FileUtil.getRecoveryKeyFileName;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.JobUtil.fireCameraUploadJob;
import static mega.privacy.android.app.utils.JobUtil.fireCancelCameraUploadJob;
import static mega.privacy.android.app.utils.JobUtil.fireStopCameraUploadJob;
import static mega.privacy.android.app.utils.JobUtil.stopCameraUploadSyncHeartbeatWorkers;
import static mega.privacy.android.app.utils.MDClickStatsUtil.fireMDStatsEvent;
import static mega.privacy.android.app.utils.MegaApiUtils.calculateDeepBrowserTreeIncoming;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_FAB;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_SHARE_FOLDER;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_MOVE_TO_BACKUP;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_ACTION_TYPE;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_DIALOG_WARN;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_HANDLED_ITEM;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_HANDLED_NODE;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NODE_TYPE;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.IS_NEW_FOLDER_DIALOG_SHOWN;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.IS_NEW_TEXT_FILE_SHOWN;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.NEW_FOLDER_DIALOG_TEXT;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.NEW_TEXT_FILE_TEXT;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.checkNewFolderDialogState;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.checkNewTextFileDialogState;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog;
import static mega.privacy.android.app.utils.MegaNodeUtil.isNodeInRubbish;
import static mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog;
import static mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog;
import static mega.privacy.android.app.utils.MegaProgressDialogUtil.showProcessFileDialog;
import static mega.privacy.android.app.utils.OfflineUtils.removeInitialOfflinePath;
import static mega.privacy.android.app.utils.OfflineUtils.removeOffline;
import static mega.privacy.android.app.utils.OfflineUtils.saveOffline;
import static mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.TimeUtils.getHumanizedTime;
import static mega.privacy.android.app.utils.UploadUtil.chooseFiles;
import static mega.privacy.android.app.utils.UploadUtil.chooseFolder;
import static mega.privacy.android.app.utils.UploadUtil.getFolder;
import static mega.privacy.android.app.utils.UploadUtil.getTemporalTakePictureFile;
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
import android.graphics.PorterDuff;
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
import androidx.viewpager2.widget.ViewPager2;

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
import com.google.android.material.tabs.TabLayoutMediator;
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
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.activities.OfflineFileInfoActivity;
import mega.privacy.android.app.activities.WebViewActivity;
import mega.privacy.android.app.components.CustomViewPager;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.attacher.MegaAttacher;
import mega.privacy.android.app.components.saver.NodeSaver;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.contacts.ContactsActivity;
import mega.privacy.android.app.contacts.usecase.InviteContactUseCase;
import mega.privacy.android.app.data.model.UserCredentials;
import mega.privacy.android.app.databinding.FabMaskChatLayoutBinding;
import mega.privacy.android.app.di.ApplicationScope;
import mega.privacy.android.app.exportRK.ExportRecoveryKeyActivity;
import mega.privacy.android.app.fragments.homepage.EventObserver;
import mega.privacy.android.app.fragments.homepage.HomepageSearchable;
import mega.privacy.android.app.fragments.homepage.documents.DocumentsFragment;
import mega.privacy.android.app.fragments.homepage.main.HomepageFragment;
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections;
import mega.privacy.android.app.fragments.managerFragments.cu.CustomHideBottomViewOnScrollBehaviour;
import mega.privacy.android.app.fragments.managerFragments.cu.PhotosFragment;
import mega.privacy.android.app.fragments.managerFragments.cu.album.AlbumContentFragment;
import mega.privacy.android.app.fragments.offline.OfflineFragment;
import mega.privacy.android.app.fragments.recent.RecentsFragment;
import mega.privacy.android.app.fragments.settingsFragments.cookie.CookieDialogHandler;
import mega.privacy.android.app.gallery.ui.MediaDiscoveryFragment;
import mega.privacy.android.app.generalusecase.FilePrepareUseCase;
import mega.privacy.android.app.globalmanagement.MyAccountInfo;
import mega.privacy.android.app.globalmanagement.SortOrderManagement;
import mega.privacy.android.app.imageviewer.ImageViewerActivity;
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
import mega.privacy.android.app.logging.LegacyLoggingSettings;
import mega.privacy.android.app.main.adapters.TransfersPageAdapter;
import mega.privacy.android.app.main.controllers.AccountController;
import mega.privacy.android.app.main.controllers.ContactController;
import mega.privacy.android.app.main.controllers.NodeController;
import mega.privacy.android.app.main.listeners.CreateGroupChatWithPublicLink;
import mega.privacy.android.app.main.listeners.FabButtonListener;
import mega.privacy.android.app.main.managerSections.CompletedTransfersFragment;
import mega.privacy.android.app.main.managerSections.InboxFragment;
import mega.privacy.android.app.main.managerSections.NotificationsFragment;
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
import mega.privacy.android.app.namecollision.data.NameCollision;
import mega.privacy.android.app.namecollision.data.NameCollisionType;
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase;
import mega.privacy.android.app.objects.PasscodeManagement;
import mega.privacy.android.app.presentation.clouddrive.FileBrowserFragment;
import mega.privacy.android.app.presentation.manager.ManagerViewModel;
import mega.privacy.android.app.presentation.manager.UnreadUserAlertsCheckType;
import mega.privacy.android.app.presentation.manager.model.SharesTab;
import mega.privacy.android.app.presentation.manager.model.Tab;
import mega.privacy.android.app.presentation.manager.model.TransfersTab;
import mega.privacy.android.app.presentation.rubbishbin.RubbishBinFragment;
import mega.privacy.android.app.presentation.search.SearchFragment;
import mega.privacy.android.app.presentation.search.SearchViewModel;
import mega.privacy.android.app.presentation.settings.model.TargetPreference;
import mega.privacy.android.app.presentation.shares.MegaNodeBaseFragment;
import mega.privacy.android.app.presentation.shares.SharesPageAdapter;
import mega.privacy.android.app.presentation.transfers.TransfersManagementActivity;
import mega.privacy.android.app.psa.Psa;
import mega.privacy.android.app.psa.PsaManager;
import mega.privacy.android.app.psa.PsaViewHolder;
import mega.privacy.android.app.service.iar.RatingHandlerImpl;
import mega.privacy.android.app.service.push.MegaMessageService;
import mega.privacy.android.app.smsVerification.SMSVerificationActivity;
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager;
import mega.privacy.android.app.sync.fileBackups.FileBackupManager;
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity;
import mega.privacy.android.app.usecase.CopyNodeUseCase;
import mega.privacy.android.app.usecase.DownloadNodeUseCase;
import mega.privacy.android.app.usecase.GetNodeUseCase;
import mega.privacy.android.app.usecase.MoveNodeUseCase;
import mega.privacy.android.app.usecase.RemoveNodeUseCase;
import mega.privacy.android.app.usecase.UploadUseCase;
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase;
import mega.privacy.android.app.usecase.data.CopyRequestResult;
import mega.privacy.android.app.usecase.data.MoveRequestResult;
import mega.privacy.android.app.usecase.exception.ForeignNodeException;
import mega.privacy.android.app.usecase.exception.MegaNodeException;
import mega.privacy.android.app.usecase.exception.NotEnoughQuotaMegaException;
import mega.privacy.android.app.usecase.exception.QuotaExceededMegaException;
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
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.contacts.MegaContactGetter;
import mega.privacy.android.app.zippreview.ui.ZipBrowserActivity;
import mega.privacy.android.domain.entity.ContactRequest;
import mega.privacy.android.domain.entity.ContactRequestStatus;
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
import timber.log.Timber;

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
    private static final String BOTTOM_ITEM_BEFORE_OPEN_FULLSCREEN_OFFLINE = "BOTTOM_ITEM_BEFORE_OPEN_FULLSCREEN_OFFLINE";

    private static final String BUSINESS_GRACE_ALERT_SHOWN = "BUSINESS_GRACE_ALERT_SHOWN";
    private static final String BUSINESS_CU_ALERT_SHOWN = "BUSINESS_CU_ALERT_SHOWN";

    public static final String NEW_CREATION_ACCOUNT = "NEW_CREATION_ACCOUNT";
    public static final String JOINING_CHAT_LINK = "JOINING_CHAT_LINK";
    public static final String LINK_JOINING_CHAT_LINK = "LINK_JOINING_CHAT_LINK";
    private static final String PROCESS_FILE_DIALOG_SHOWN = "PROGRESS_DIALOG_SHOWN";
    private static final String OPEN_LINK_DIALOG_SHOWN = "OPEN_LINK_DIALOG_SHOWN";
    private static final String OPEN_LINK_TEXT = "OPEN_LINK_TEXT";
    private static final String OPEN_LINK_ERROR = "OPEN_LINK_ERROR";
    private static final String COMES_FROM_NOTIFICATIONS_SHARED_INDEX = "COMES_FROM_NOTIFICATIONS_SHARED_INDEX";

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

    /**
     * Indicates that ManagerActivity was called from Image Viewer;
     * Transfers tab should go back to {@link mega.privacy.android.app.imageviewer.ImageViewerActivity}
     */
    private boolean transfersToImageViewer = false;

    private LastShowSMSDialogTimeChecker smsDialogTimeChecker;

    private ManagerViewModel viewModel;
    private SearchViewModel searchViewModel;

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
    @Inject
    CheckNameCollisionUseCase checkNameCollisionUseCase;
    @Inject
    UploadUseCase uploadUseCase;
    @Inject
    CopyNodeUseCase copyNodeUseCase;
    @Inject
    LegacyLoggingSettings loggingSettings;

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

    //Tabs in Shares
    private TabLayout tabLayoutShares;
    private SharesPageAdapter sharesPageAdapter;
    private ViewPager2 viewPagerShares;

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
    boolean isSearching = false;
    boolean openLink = false;

    long lastTimeOnTransferUpdate = Calendar.getInstance().getTimeInMillis();

    boolean firstLogin = false;
    private boolean askPermissions = false;

    boolean megaContacts = true;

    private HomepageScreen mHomepageScreen = HomepageScreen.HOMEPAGE;

    private enum HomepageScreen {
        HOMEPAGE, IMAGES, FAVOURITES, DOCUMENTS, AUDIO, VIDEO,
        FULLSCREEN_OFFLINE, OFFLINE_FILE_INFO, RECENT_BUCKET
    }

    public boolean isList = true;

    private String pathNavigationOffline;

    // Fragments
    private FileBrowserFragment fileBrowserFragment;
    private RubbishBinFragment rubbishBinFragment;
    private InboxFragment inboxFragment;
    private MegaNodeBaseFragment incomingSharesFragment;
    private MegaNodeBaseFragment outgoingSharesFragment;
    private MegaNodeBaseFragment linksFragment;
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
    public long comesFromNotificationHandle = INVALID_VALUE;
    public long comesFromNotificationHandleSaved = INVALID_VALUE;
    public int comesFromNotificationDeepBrowserTreeIncoming = INVALID_VALUE;
    public long[] comesFromNotificationChildNodeHandleList;
    public SharesTab comesFromNotificationSharedIndex = SharesTab.NONE;

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
                    Timber.d("BROADCAST TO UPDATE AFTER UPDATE_ACCOUNT_DETAILS");
                    if (isFinishing()) {
                        return;
                    }

                    updateAccountDetailsVisibleInfo();

                    if (megaApi.isBusinessAccount()) {
                        supportInvalidateOptionsMenu();
                    }
                } else if (actionType == UPDATE_PAYMENT_METHODS) {
                    Timber.d("BROADCAST TO UPDATE AFTER UPDATE_PAYMENT_METHODS");
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
            Timber.d("Network broadcast received!");
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
            MegaNode parentNode = megaApi.getNodeByHandle(viewModel.getState().getValue().getBrowserParentHandle());

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
                    Timber.d("TAKE_PICTURE_OPTION");
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
                    Timber.d("The first time");
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if (typesCameraPermission == TAKE_PICTURE_OPTION) {
                            Timber.d("TAKE_PICTURE_OPTION");
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
                        Timber.d("TAKE_PICTURE_OPTION");
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

            case REQUEST_BT_CONNECT:
                Timber.d("get Bluetooth Connect permission");
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

    public void setTypesCameraPermission(int typesCameraPermission) {
        this.typesCameraPermission = typesCameraPermission;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Timber.d("onSaveInstanceState");
        if (drawerItem != null) {
            Timber.d("DrawerItem = %s", drawerItem);
        } else {
            Timber.w("DrawerItem is null");
        }
        super.onSaveInstanceState(outState);
        outState.putSerializable("drawerItem", drawerItem);
        outState.putInt(BOTTOM_ITEM_BEFORE_OPEN_FULLSCREEN_OFFLINE,
                bottomItemBeforeOpenFullscreenOffline);
        outState.putBoolean(EXTRA_FIRST_LOGIN, firstLogin);
        outState.putBoolean(STATE_KEY_SMS_DIALOG, isSMSDialogShowing);

        outState.putString("pathNavigationOffline", pathNavigationOffline);

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
        outState.putSerializable(COMES_FROM_NOTIFICATIONS_SHARED_INDEX, comesFromNotificationSharedIndex);
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
        Timber.d("onStart");

        mStopped = false;

        super.onStart();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate");
//		Fragments are restored during the Activity's onCreate().
//		Importantly though, they are restored in the base Activity class's onCreate().
//		Thus if you call super.onCreate() first, all of the rest of your onCreate() method will execute after your Fragments have been restored.
        super.onCreate(savedInstanceState);
        Timber.d("onCreate after call super");

        viewModel = new ViewModelProvider(this).get(ManagerViewModel.class);
        searchViewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        viewModel.getUpdateUsers().observe(this,
                new EventObserver<>(users -> {
                    updateUsers(users);
                    return null;
                }));
        viewModel.getUpdateUserAlerts().observe(this,
                new EventObserver<>(userAlerts -> {
                    updateUserAlerts(userAlerts);
                    return null;
                }));
        viewModel.getUpdateNodes().observe(this,
                new EventObserver<>(nodes -> {
                    updateNodes(nodes);
                    return null;
                }));
        viewModel.getUpdateContactsRequests().observe(this,
                new EventObserver<>(contactRequests -> {
                    updateContactRequests(contactRequests);
                    return null;
                }));

        viewModel.onGetNumUnreadUserAlerts().observe(this, this::updateNumUnreadUserAlerts);

        viewModel.onInboxSectionUpdate().observe(this, this::updateInboxSectionVisibility);

        getTransfersViewModel().onGetShouldCompletedTab().observe(this, this::updateTransfersTab);

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
            Timber.d("Bundle is NOT NULL");
            isSMSDialogShowing = savedInstanceState.getBoolean(STATE_KEY_SMS_DIALOG, false);
            firstLogin = savedInstanceState.getBoolean(EXTRA_FIRST_LOGIN);
            askPermissions = savedInstanceState.getBoolean(EXTRA_ASK_PERMISSIONS);
            drawerItem = (DrawerItem) savedInstanceState.getSerializable("drawerItem");
            bottomItemBeforeOpenFullscreenOffline = savedInstanceState.getInt(BOTTOM_ITEM_BEFORE_OPEN_FULLSCREEN_OFFLINE);
            pathNavigationOffline = savedInstanceState.getString("pathNavigationOffline", pathNavigationOffline);
            Timber.d("savedInstanceState -> pathNavigationOffline: %s", pathNavigationOffline);
            selectedAccountType = savedInstanceState.getInt("selectedAccountType", -1);
            turnOnNotifications = savedInstanceState.getBoolean("turnOnNotifications", false);
            orientationSaved = savedInstanceState.getInt("orientationSaved");
            isEnable2FADialogShown = savedInstanceState.getBoolean("isEnable2FADialogShown", false);
            bottomNavigationCurrentItem = savedInstanceState.getInt("bottomNavigationCurrentItem", -1);
            searchExpand = savedInstanceState.getBoolean("searchExpand", false);
            comesFromNotifications = savedInstanceState.getBoolean("comesFromNotifications", false);
            comesFromNotificationsLevel = savedInstanceState.getInt("comesFromNotificationsLevel", 0);
            comesFromNotificationHandle = savedInstanceState.getLong("comesFromNotificationHandle", INVALID_VALUE);
            comesFromNotificationHandleSaved = savedInstanceState.getLong("comesFromNotificationHandleSaved", INVALID_VALUE);
            if(savedInstanceState.getSerializable(COMES_FROM_NOTIFICATIONS_SHARED_INDEX) != null)
                comesFromNotificationSharedIndex = (SharesTab) savedInstanceState.getSerializable(COMES_FROM_NOTIFICATIONS_SHARED_INDEX);
            else
                comesFromNotificationSharedIndex = SharesTab.NONE;
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
            comesFromNotificationDeepBrowserTreeIncoming = savedInstanceState.getInt("comesFromNotificationDeepBrowserTreeIncoming", INVALID_VALUE);
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
            Timber.d("Bundle is NULL");
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

        LiveEventBus.get(EVENT_TRANSFER_OVER_QUOTA, Boolean.class).observe(this, update -> {
            updateTransfersWidget(INVALID_VALUE);
            showTransfersTransferOverQuotaWarning();
        });

        LiveEventBus.get(EVENT_FAILED_TRANSFERS, Boolean.class).observe(this, failed -> {
            if (drawerItem == DrawerItem.TRANSFERS && getTabItemTransfers() == TransfersTab.COMPLETED_TAB) {
                retryTransfers.setVisible(failed);
            }
        });

        registerReceiver(transferFinishReceiver, new IntentFilter(BROADCAST_ACTION_TRANSFER_FINISH));

        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall.class).observe(this, callStatusObserver);
        LiveEventBus.get(EVENT_CALL_ON_HOLD_CHANGE, MegaChatCall.class).observe(this, callOnHoldObserver);
        LiveEventBus.get(EVENT_SESSION_ON_HOLD_CHANGE, Pair.class).observe(this, sessionOnHoldObserver);

        registerReceiver(chatRoomMuteUpdateReceiver, new IntentFilter(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING));

        LiveEventBus.get(EVENT_REFRESH, Boolean.class).observeForever(refreshObserver);

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
            Timber.d("retryChatPendingConnections()");
            megaChatApi.retryPendingConnections(false, null);
        }

        MegaApplication.getPushNotificationSettingManagement().getPushNotificationSetting();

        transfersInProgress = new ArrayList<Integer>();

        //sync local contacts to see who's on mega.
        if (hasPermissions(this, Manifest.permission.READ_CONTACTS) && app.getStorageState() != STORAGE_STATE_PAYWALL) {
            Timber.d("sync mega contacts");
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

        Timber.d("Preferred View List: %s", isList);

        LiveEventBus.get(EVENT_LIST_GRID_CHANGE, Boolean.class).post(isList);

        handler = new Handler();

        Timber.d("Set view");
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
            Timber.e(e, "Formatted string: %s", getProTextString);
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

        //TABS section Shared Items
        tabLayoutShares = findViewById(R.id.sliding_tabs_shares);
        viewPagerShares = findViewById(R.id.shares_tabs_pager);
        viewPagerShares.setOffscreenPageLimit(3);
        // Set an empty page transformer to override default animation when notifying the adapter
        viewPagerShares.setPageTransformer((page, position) -> { });

        viewPagerShares.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                Timber.d("selectDrawerItemSharedItems - TabId: %s", position);
                supportInvalidateOptionsMenu();
                checkScrollElevation();
                SharesTab selectedTab = SharesTab.Companion.fromPosition(position);
                switch (selectedTab) {
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

        tabLayoutShares.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        int tabIconColor = ContextCompat.getColor(getApplicationContext(), R.color.red_600_red_300);
                        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                        int tabIconColor = ContextCompat.getColor(getApplicationContext(), R.color.grey_300_grey_600);
                        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                    }
                }
        );

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

                TransfersTab selectedTab = TransfersTab.Companion.fromPosition(position);
                if (selectedTab == TransfersTab.PENDING_TAB && isTransfersInProgressAdded()) {
                    transfersFragment.setGetMoreQuotaViewVisibility();
                } else if (selectedTab == TransfersTab.COMPLETED_TAB) {
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
                transfersManagement.checkIfTransferIsPaused(tag);
            }

            for (int i = 0; i < transferData.getNumUploads(); i++) {
                int tag = transferData.getUploadTag(i);
                transfersInProgress.add(transferData.getUploadTag(i));
                transfersManagement.checkIfTransferIsPaused(tag);
            }
        }

        if (!isOnline(this)) {
            Timber.d("No network -> SHOW OFFLINE MODE");

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
        Timber.i("App version: %d", getVersion());
        final File fMKOld = buildExternalStorageFile(OLD_MK_FILE);
        final File fRKOld = buildExternalStorageFile(OLD_RK_FILE);
        if (isFileAvailable(fMKOld)) {
            Timber.d("Old MK file need to be renamed!");
            aC.renameRK(fMKOld);
        } else if (isFileAvailable(fRKOld)) {
            Timber.d("Old RK file need to be renamed!");
            aC.renameRK(fRKOld);
        }

        boolean isHeartBeatAlive = MegaApplication.isIsHeartBeatAlive();
        rootNode = megaApi.getRootNode();
        if (rootNode == null || LoginActivity.isBackFromLoginPage || isHeartBeatAlive) {
            if (getIntent() != null) {
                Timber.d("Action: %s", getIntent().getAction());
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
                        if (getIntent().getSerializableExtra(TRANSFERS_TAB) != null)
                            intent.putExtra(TRANSFERS_TAB, (TransfersTab) getIntent().getSerializableExtra(TRANSFERS_TAB));
                        else
                            intent.putExtra(TRANSFERS_TAB, TransfersTab.NONE);
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
                        Timber.d("Login");
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
                                Timber.d("megaApi.invalidateCache();");
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
                Timber.d("Run async task to check offline files");
                //Check the consistency of the offline nodes in the DB
                CheckOfflineNodesTask checkOfflineNodesTask = new CheckOfflineNodesTask(this);
                checkOfflineNodesTask.execute();
            }

            if (getIntent() != null) {
                if (getIntent().getAction() != null) {
                    if (getIntent().getAction().equals(ACTION_EXPORT_MASTER_KEY)) {
                        Timber.d("Intent to export Master Key - im logged in!");
                        startActivity(new Intent(this, ExportRecoveryKeyActivity.class));
                        return;
                    } else if (getIntent().getAction().equals(ACTION_CANCEL_ACCOUNT)) {
                        Uri link = getIntent().getData();
                        if (link != null) {
                            Timber.d("Link to cancel: %s", link);
                            showMyAccount(ACTION_CANCEL_ACCOUNT, link);
                        }
                    } else if (getIntent().getAction().equals(ACTION_CHANGE_MAIL)) {
                        Uri link = getIntent().getData();
                        if (link != null) {
                            Timber.d("Link to change mail: %s", link);
                            showMyAccount(ACTION_CHANGE_MAIL, link);
                        }
                    } else if (getIntent().getAction().equals(ACTION_OPEN_FOLDER)) {
                        Timber.d("Open after LauncherFileExplorerActivity ");
                        boolean locationFileInfo = getIntent().getBooleanExtra(INTENT_EXTRA_KEY_LOCATION_FILE_INFO, false);
                        long handleIntent = getIntent().getLongExtra(INTENT_EXTRA_KEY_PARENT_HANDLE, INVALID_HANDLE);

                        if (getIntent().getBooleanExtra(SHOW_MESSAGE_UPLOAD_STARTED, false)) {
                            int numberUploads = getIntent().getIntExtra(NUMBER_UPLOADS, 1);
                            showSnackbar(SNACKBAR_TYPE, getResources().getQuantityString(R.plurals.upload_began, numberUploads, numberUploads), -1);
                        }

                        String message = getIntent().getStringExtra(EXTRA_MESSAGE);
                        if (message != null) {
                            showSnackbar(SNACKBAR_TYPE, message, MEGACHAT_INVALID_HANDLE);
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
                                    viewModel.setBrowserParentHandle(handleIntent);
                                    selectDrawerItem(drawerItem);
                                    selectDrawerItemPending = false;
                                } else if (fragmentHandle == megaApi.getRubbishNode().getHandle()) {
                                    drawerItem = DrawerItem.RUBBISH_BIN;
                                    viewModel.setRubbishBinParentHandle(handleIntent);
                                    selectDrawerItem(drawerItem);
                                    selectDrawerItemPending = false;
                                } else if (fragmentHandle == megaApi.getInboxNode().getHandle()) {
                                    drawerItem = DrawerItem.INBOX;
                                    viewModel.setInboxParentHandle(handleIntent);
                                    selectDrawerItem(drawerItem);
                                    selectDrawerItemPending = false;
                                } else {
                                    //Incoming
                                    drawerItem = DrawerItem.SHARED_ITEMS;
                                    viewModel.setSharesTab(SharesTab.INCOMING_TAB);
                                    MegaNode parentIntentN = megaApi.getNodeByHandle(handleIntent);
                                    if (parentIntentN != null) {
                                        viewModel.setIncomingTreeDepth(calculateDeepBrowserTreeIncoming(parentIntentN, this));
                                    }
                                    viewModel.setIncomingParentHandle(handleIntent);
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
                        Timber.d("IPC link - go to received request in Contacts");
                        markNotificationsSeen(true);
                        navigateToContactRequests();
                        getIntent().setAction(null);
                        setIntent(null);
                    } else if (getIntent().getAction().equals(ACTION_CHAT_NOTIFICATION_MESSAGE)) {
                        Timber.d("Chat notitificacion received");
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
                        Timber.d("Chat notification: ACTION_CHAT_SUMMARY");
                        drawerItem = DrawerItem.CHAT;
                        selectDrawerItem(drawerItem);
                        selectDrawerItemPending = false;
                        getIntent().setAction(null);
                        setIntent(null);
                    } else if (getIntent().getAction().equals(ACTION_OPEN_CHAT_LINK)) {
                        Timber.d("ACTION_OPEN_CHAT_LINK: %s", getIntent().getDataString());
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
                        Timber.d("Chat notification: SHOW_SETTINGS");
                        selectDrawerItemPending = false;
                        moveToSettingsSection();
                        getIntent().setAction(null);
                        setIntent(null);
                    } else if (getIntent().getAction().equals(ACTION_SHOW_SETTINGS_STORAGE)) {
                        Timber.d("ACTION_SHOW_SETTINGS_STORAGE");
                        selectDrawerItemPending = false;
                        moveToSettingsSectionStorage();
                        getIntent().setAction(null);
                        setIntent(null);
                    } else if (getIntent().getAction().equals(ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION)) {
                        Timber.d("ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION");
                        markNotificationsSeen(true);

                        drawerItem = DrawerItem.SHARED_ITEMS;
                        viewModel.setSharesTab(SharesTab.INCOMING_TAB);
                        selectDrawerItem(drawerItem);
                        selectDrawerItemPending = false;
                    } else if (getIntent().getAction().equals(ACTION_SHOW_MY_ACCOUNT)) {
                        Timber.d("Intent from chat - show my account");

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
                                viewModel.setBrowserParentHandle(pN.getHandle());
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

            Timber.d("Check if there any unread chat");
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

            Timber.d("Check if there any INCOMING pendingRequest contacts");
            setContactTitleSection();

            viewModel.checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NOTIFICATIONS_TITLE);

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
                            Timber.d("First login. Go to Camera Uploads configuration.");
                            drawerItem = DrawerItem.PHOTOS;
                            setIntent(null);
                        }
                    }
                }
            } else {
                Timber.d("DRAWERITEM NOT NULL: %s", drawerItem);
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
                            Timber.d("Intent firstTimeCam==true");
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
                            if (intentRec.getSerializableExtra(TRANSFERS_TAB) != null)
                                viewModel.setTransfersTab((TransfersTab) intentRec.getSerializableExtra(TRANSFERS_TAB));
                            else
                                viewModel.setTransfersTab(TransfersTab.NONE);
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
                }, throwable -> Timber.e(throwable, "doUpdateProgressNotification onError")));

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

        Timber.d("END onCreate");
        new RatingHandlerImpl(this).showRatingBaseOnTransaction();

        // Show backup dialog
        if (backupDialogType == BACKUP_DIALOG_SHOW_WARNING) {
            fileBackupManager.actWithBackupTips(backupHandleList, megaApi.getNodeByHandle(backupNodeHandle), backupNodeType, backupActionType);
        } else if (backupDialogType == BACKUP_DIALOG_SHOW_CONFIRM) {
            fileBackupManager.confirmationActionForBackup(backupHandleList, megaApi.getNodeByHandle(backupNodeHandle), backupNodeType, backupActionType);
        } else {
            Timber.d("Backup warning dialog is not show");
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
        Timber.d("showBusinessGraceAlert");
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
                        Timber.w(e, "Exception dismissing businessGraceAlert");
                    }
                })
                .create();

        businessGraceAlert.setCanceledOnTouchOutside(false);
        try {
            businessGraceAlert.show();
        } catch (Exception e) {
            Timber.w(e, "Exception showing businessGraceAlert");
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
            Timber.w("Not valid contact handle");
            return;
        }

        dismissAlertDialogIfExists(openLinkDialog);
        Timber.d("Handle to invite a contact: %s", handle);

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
            Timber.d("mobile only portrait mode");
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
            Timber.d("Mobile only portrait mode");
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
            Timber.d("mobile, all orientation");
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
            Timber.d("Mobile, all orientation");
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
        Timber.d("queryIfNotificationsAreOn");

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
            Timber.d("Notifications Enabled: %s", nf.areNotificationsEnabled());
            if (!nf.areNotificationsEnabled()) {
                Timber.d("OFF");
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
        Timber.d("deleteTurnOnNotificationsFragment");
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
        Timber.d("setTurnOnNotificationsFragment");
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
            Timber.w("handleIntent is not valid");
            return;
        }

        MegaNode parentIntentN = megaApi.getNodeByHandle(handleIntent);
        if (parentIntentN == null) {
            Timber.w("parentIntentN is null");
            return;
        }

        switch (megaApi.getAccess(parentIntentN)) {
            case MegaShare.ACCESS_READ:
            case MegaShare.ACCESS_READWRITE:
            case MegaShare.ACCESS_FULL:
                viewModel.setIncomingParentHandle(handleIntent);
                viewModel.setIncomingTreeDepth(calculateDeepBrowserTreeIncoming(parentIntentN, this));
                drawerItem = DrawerItem.SHARED_ITEMS;
                break;

            default:
                if (megaApi.isInRubbish(parentIntentN)) {
                    viewModel.setRubbishBinParentHandle(handleIntent);
                    drawerItem = DrawerItem.RUBBISH_BIN;
                } else if (megaApi.isInInbox(parentIntentN)) {
                    viewModel.setInboxParentHandle(handleIntent);
                    drawerItem = DrawerItem.INBOX;
                } else {
                    viewModel.setBrowserParentHandle(handleIntent);
                    drawerItem = DrawerItem.CLOUD_DRIVE;
                }
                break;
        }
    }

    @Override
    protected void onPostResume() {
        Timber.d("onPostResume");
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
                Timber.d("Not credentials");
                if (intent != null) {
                    Timber.d("Not credentials -> INTENT");
                    if (intent.getAction() != null) {
                        Timber.d("Intent with ACTION: %s", intent.getAction());

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
            Timber.d("Intent not null! %s", intent.getAction());
            // Open folder from the intent
            if (intent.hasExtra(EXTRA_OPEN_FOLDER)) {
                Timber.d("INTENT: EXTRA_OPEN_FOLDER");

                viewModel.setBrowserParentHandle(intent.getLongExtra(EXTRA_OPEN_FOLDER, -1));
                intent.removeExtra(EXTRA_OPEN_FOLDER);
                setIntent(null);
            }

            if (intent.getAction() != null) {
                Timber.d("Intent action");

                if (getIntent().getAction().equals(ACTION_EXPLORE_ZIP)) {
                    Timber.d("Open zip browser");

                    String pathZip = intent.getExtras().getString(EXTRA_PATH_ZIP);
                    ZipBrowserActivity.Companion.start(this, pathZip);
                }

                if (getIntent().getAction().equals(ACTION_IMPORT_LINK_FETCH_NODES)) {
                    Timber.d("ACTION_IMPORT_LINK_FETCH_NODES");

                    Intent loginIntent = new Intent(managerActivity, LoginActivity.class);
                    intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                    loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    loginIntent.setAction(ACTION_IMPORT_LINK_FETCH_NODES);
                    loginIntent.setData(Uri.parse(getIntent().getDataString()));
                    startActivity(loginIntent);
                    finish();
                    return;
                } else if (getIntent().getAction().equals(ACTION_OPEN_MEGA_LINK)) {
                    Timber.d("ACTION_OPEN_MEGA_LINK");

                    Intent fileLinkIntent = new Intent(managerActivity, FileLinkActivity.class);
                    fileLinkIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    fileLinkIntent.setAction(ACTION_IMPORT_LINK_FETCH_NODES);
                    String data = getIntent().getDataString();
                    if (data != null) {
                        fileLinkIntent.setData(Uri.parse(data));
                        startActivity(fileLinkIntent);
                    } else {
                        Timber.w("getDataString is NULL");
                    }
                    finish();
                    return;
                } else if (intent.getAction().equals(ACTION_OPEN_MEGA_FOLDER_LINK)) {
                    Timber.d("ACTION_OPEN_MEGA_FOLDER_LINK");

                    Intent intentFolderLink = new Intent(managerActivity, FolderLinkActivity.class);
                    intentFolderLink.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intentFolderLink.setAction(ACTION_OPEN_MEGA_FOLDER_LINK);

                    String data = getIntent().getDataString();
                    if (data != null) {
                        intentFolderLink.setData(Uri.parse(data));
                        startActivity(intentFolderLink);
                    } else {
                        Timber.w("getDataString is NULL");
                    }
                    finish();
                } else if (intent.getAction().equals(ACTION_REFRESH_PARENTHANDLE_BROWSER)) {

                    viewModel.setBrowserParentHandle(intent.getLongExtra("parentHandle", -1));
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
                    Timber.d("Intent CHANGE AVATAR");

                    String path = intent.getStringExtra("IMAGE_PATH");
                    megaApi.setAvatar(path, this);
                } else if (intent.getAction().equals(ACTION_CANCEL_CAM_SYNC)) {
                    Timber.d("ACTION_CANCEL_UPLOAD or ACTION_CANCEL_DOWNLOAD or ACTION_CANCEL_CAM_SYNC");
                    drawerItem = DrawerItem.TRANSFERS;
                    if (intent.getSerializableExtra(TRANSFERS_TAB) != null)
                        viewModel.setTransfersTab((TransfersTab) intent.getSerializableExtra(TRANSFERS_TAB));
                    else
                        viewModel.setTransfersTab(TransfersTab.NONE);
                    selectDrawerItem(drawerItem);

                    String text = getString(R.string.cam_sync_cancel_sync);

                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
                    builder.setMessage(text);

                    builder.setPositiveButton(getString(R.string.general_yes),
                            (dialog, whichButton) -> {
                                fireStopCameraUploadJob(ManagerActivity.this);
                                dbH.setCamSyncEnabled(false);
                                sendBroadcast(new Intent(ACTION_UPDATE_DISABLE_CU_SETTING));
                            });

                    builder.setNegativeButton(getString(R.string.general_no), null);
                    final AlertDialog dialog = builder.create();
                    try {
                        dialog.show();
                    } catch (Exception ex) {
                        Timber.e(ex);
                    }
                } else if (intent.getAction().equals(ACTION_SHOW_TRANSFERS)) {
                    if (intent.getBooleanExtra(OPENED_FROM_CHAT, false)) {
                        sendBroadcast(new Intent(ACTION_CLOSE_CHAT_AFTER_OPEN_TRANSFERS));
                    }

                    drawerItem = DrawerItem.TRANSFERS;
                    if (intent.getSerializableExtra(TRANSFERS_TAB) != null)
                        viewModel.setTransfersTab((TransfersTab) intent.getSerializableExtra(TRANSFERS_TAB));
                    else
                        viewModel.setTransfersTab(TransfersTab.NONE);
                    selectDrawerItem(drawerItem);
                } else if (intent.getAction().equals(ACTION_TAKE_SELFIE)) {
                    Timber.d("Intent take selfie");
                    checkTakePicture(this, TAKE_PHOTO_CODE);
                } else if (intent.getAction().equals(SHOW_REPEATED_UPLOAD)) {
                    Timber.d("Intent SHOW_REPEATED_UPLOAD");
                    String message = intent.getStringExtra("MESSAGE");
                    showSnackbar(SNACKBAR_TYPE, message, -1);
                } else if (getIntent().getAction().equals(ACTION_IPC)) {
                    Timber.d("IPC - go to received request in Contacts");
                    markNotificationsSeen(true);
                    navigateToContactRequests();
                } else if (getIntent().getAction().equals(ACTION_CHAT_NOTIFICATION_MESSAGE)) {
                    Timber.d("ACTION_CHAT_NOTIFICATION_MESSAGE");

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
                    Timber.d("ACTION_CHAT_SUMMARY");
                    drawerItem = DrawerItem.CHAT;
                    selectDrawerItem(drawerItem);
                } else if (getIntent().getAction().equals(ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION)) {
                    Timber.d("ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION");
                    markNotificationsSeen(true);

                    drawerItem = DrawerItem.SHARED_ITEMS;
                    viewModel.setSharesTab(SharesTab.INCOMING_TAB);
                    selectDrawerItem(drawerItem);
                } else if (getIntent().getAction().equals(ACTION_OPEN_CONTACTS_SECTION)) {
                    Timber.d("ACTION_OPEN_CONTACTS_SECTION");
                    markNotificationsSeen(true);
                    openContactLink(getIntent().getLongExtra(CONTACT_HANDLE, -1));
                } else if (getIntent().getAction().equals(ACTION_RECOVERY_KEY_EXPORTED)) {
                    Timber.d("ACTION_RECOVERY_KEY_EXPORTED");
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
                    Timber.d("Open after LauncherFileExplorerActivity ");
                    long handleIntent = getIntent().getLongExtra(INTENT_EXTRA_KEY_PARENT_HANDLE, -1);

                    if (getIntent().getBooleanExtra(SHOW_MESSAGE_UPLOAD_STARTED, false)) {
                        int numberUploads = getIntent().getIntExtra(NUMBER_UPLOADS, 1);
                        showSnackbar(SNACKBAR_TYPE, getResources().getQuantityString(R.plurals.upload_began, numberUploads, numberUploads), -1);
                    }

                    String message = getIntent().getStringExtra(EXTRA_MESSAGE);
                    if (message != null) {
                        showSnackbar(SNACKBAR_TYPE, message, MEGACHAT_INVALID_HANDLE);
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
                    Timber.d("Case CLOUD DRIVE");
                    //Check the tab to shown and the title of the actionBar
                    setToolbarTitle();
                    setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV);
                    break;
                }
                case SHARED_ITEMS: {
                    Timber.d("Case SHARED ITEMS");
                    setBottomNavigationMenuItemChecked(SHARED_ITEMS_BNV);
                    try {
                        NotificationManager notificationManager =
                                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                        notificationManager.cancel(NOTIFICATION_PUSH_CLOUD_DRIVE);
                    } catch (Exception e) {
                        Timber.e(e, "Exception NotificationManager - remove contact notification");
                    }
                    setToolbarTitle();
                    break;
                }
                case SEARCH: {
                    if (searchExpand) {
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
        Timber.d("Chat ID: %s", chatId);

        if (chatId != -1) {
            MegaChatRoom chat = megaChatApi.getChatRoom(chatId);
            if (chat != null) {
                Timber.d("Open chat with id: %s", chatId);
                Intent intentToChat = new Intent(this, ChatActivity.class);
                intentToChat.setAction(ACTION_CHAT_SHOW_MESSAGES);
                intentToChat.putExtra(CHAT_ID, chatId);
                intentToChat.putExtra(SHOW_SNACKBAR, text);
                this.startActivity(intentToChat);
            } else {
                Timber.e("Error, chat is NULL");
            }
        } else {
            Timber.e("Error, chat id is -1");
        }
    }

    public void setProfileAvatar() {
        Timber.d("setProfileAvatar");
        Pair<Boolean, Bitmap> circleAvatar = AvatarUtil.getCircleAvatar(this, megaApi.getMyEmail());
        if (circleAvatar.first) {
            nVPictureProfile.setImageBitmap(circleAvatar.second);
        } else {
            File avatarFile = CacheFolderManager.buildAvatarFile(this, megaApi.getMyEmail() + JPG_EXTENSION);
            if (avatarFile != null && avatarFile.exists()) {
                megaApi.getUserAvatar(megaApi.getMyUser(), avatarFile.getAbsolutePath(), this);
            }
        }
    }

    public void setDefaultAvatar() {
        Timber.d("setDefaultAvatar");
        nVPictureProfile.setImageBitmap(getDefaultAvatar(getColorAvatar(megaApi.getMyUser()), megaChatApi.getMyFullname(), AVATAR_SIZE, true));
    }

    public void setOfflineAvatar(String email, long myHandle, String name) {
        Timber.d("setOfflineAvatar");
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
        Timber.d("onStop");

        mStopped = true;

        super.onStop();
    }

    @Override
    protected void onPause() {
        Timber.d("onPause");
        managerActivity = null;
        transfersManagement.setOnTransfersSection(false);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Timber.d("onDestroy()");

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
        unregisterReceiver(transferFinishReceiver);
        LiveEventBus.get(EVENT_REFRESH, Boolean.class).removeObserver(refreshObserver);
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
        searchViewModel.cancelSearch();
    }

    public void skipToMediaDiscoveryFragment(Fragment f, Long mediaHandle) {
        mediaDiscoveryFragment = (MediaDiscoveryFragment) f;
        replaceFragment(f, FragmentTag.MEDIA_DISCOVERY.getTag());
        fireMDStatsEvent(megaApi, this, mediaHandle);
        isInMDMode = true;
    }

    public void skipToAlbumContentFragment(Fragment f) {
        albumContentFragment = (AlbumContentFragment) f;
        replaceFragment(f, FragmentTag.ALBUM_CONTENT.getTag());
        isInAlbumContent = true;
        viewModel.setIsFirstNavigationLevel(false);

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
            Timber.d("Fragment %s refreshing", fTag);
            getSupportFragmentManager().beginTransaction().detach(f).commitNowAllowingStateLoss();
            getSupportFragmentManager().beginTransaction().attach(f).commitNowAllowingStateLoss();
        } else {
            Timber.w("Fragment == NULL. Not refresh");
        }
    }

    public boolean isFirstLogin() {
        return firstLogin;
    }

    public void selectDrawerItemCloudDrive() {
        Timber.d("selectDrawerItemCloudDrive");
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
            Timber.d("Its NOT first time");
            int dbContactsSize = dbH.getContactsSize();
            int sdkContactsSize = megaApi.getContacts().size();
            if (dbContactsSize != sdkContactsSize) {
                Timber.d("Contacts TABLE != CONTACTS SDK %d vs %d", dbContactsSize, sdkContactsSize);
                dbH.clearContacts();
                FillDBContactsTask fillDBContactsTask = new FillDBContactsTask(this);
                fillDBContactsTask.execute();
            }
        } else {
            Timber.d("Its first time");

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
        Timber.d("setToolbarTitle");
        if (drawerItem == null) {
            return;
        }

        switch (drawerItem) {
            case CLOUD_DRIVE: {
                aB.setSubtitle(null);
                Timber.d("Cloud Drive SECTION");
                MegaNode parentNode = megaApi.getNodeByHandle(viewModel.getState().getValue().getBrowserParentHandle());
                if (parentNode != null) {
                    if (megaApi.getRootNode() != null) {
                        if (parentNode.getHandle() == megaApi.getRootNode().getHandle() || viewModel.getState().getValue().getBrowserParentHandle() == -1) {
                            aB.setTitle(getString(R.string.section_cloud_drive));
                            viewModel.setIsFirstNavigationLevel(true);
                        } else {
                            aB.setTitle(parentNode.getName());
                            viewModel.setIsFirstNavigationLevel(false);
                        }
                    } else {
                        viewModel.setBrowserParentHandle(-1);
                    }
                } else {
                    if (megaApi.getRootNode() != null) {
                        viewModel.setBrowserParentHandle(megaApi.getRootNode().getHandle());
                        aB.setTitle(getString(R.string.title_mega_info_empty_screen));
                        viewModel.setIsFirstNavigationLevel(true);
                    } else {
                        viewModel.setBrowserParentHandle(-1);
                        viewModel.setIsFirstNavigationLevel(true);
                    }
                }
                break;
            }
            case RUBBISH_BIN: {
                aB.setSubtitle(null);
                MegaNode node = megaApi.getNodeByHandle(viewModel.getState().getValue().getRubbishBinParentHandle());
                MegaNode rubbishNode = megaApi.getRubbishNode();
                if (rubbishNode == null) {
                    viewModel.setRubbishBinParentHandle(INVALID_HANDLE);
                    viewModel.setIsFirstNavigationLevel(true);
                } else if (viewModel.getState().getValue().getRubbishBinParentHandle() == INVALID_HANDLE || node == null || node.getHandle() == rubbishNode.getHandle()) {
                    aB.setTitle(StringResourcesUtils.getString(R.string.section_rubbish_bin));
                    viewModel.setIsFirstNavigationLevel(true);
                } else {
                    aB.setTitle(node.getName());
                    viewModel.setIsFirstNavigationLevel(false);
                }
                break;
            }
            case SHARED_ITEMS: {
                Timber.d("Shared Items SECTION");
                aB.setSubtitle(null);
                SharesTab indexShares = getTabItemShares();
                if (indexShares == SharesTab.NONE) break;
                switch (indexShares) {
                    case INCOMING_TAB: {
                        if (isIncomingAdded()) {
                            if (viewModel.getState().getValue().getIncomingParentHandle() != -1) {
                                MegaNode node = megaApi.getNodeByHandle(viewModel.getState().getValue().getIncomingParentHandle());
                                if (node == null) {
                                    aB.setTitle(getResources().getString(R.string.title_shared_items));
                                } else {
                                    aB.setTitle(node.getName());
                                }

                                viewModel.setIsFirstNavigationLevel(false);
                            } else {
                                aB.setTitle(getResources().getString(R.string.title_shared_items));
                                viewModel.setIsFirstNavigationLevel(true);
                            }
                        } else {
                            Timber.d("selectDrawerItemSharedItems: inSFLol == null");
                        }
                        break;
                    }
                    case OUTGOING_TAB: {
                        Timber.d("setToolbarTitle: OUTGOING TAB");
                        if (isOutgoingAdded()) {
                            if (viewModel.getState().getValue().getOutgoingParentHandle() != -1) {
                                MegaNode node = megaApi.getNodeByHandle(viewModel.getState().getValue().getOutgoingParentHandle());
                                aB.setTitle(node.getName());
                                viewModel.setIsFirstNavigationLevel(false);
                            } else {
                                aB.setTitle(getResources().getString(R.string.title_shared_items));
                                viewModel.setIsFirstNavigationLevel(true);
                            }
                        }
                        break;
                    }
                    case LINKS_TAB:
                        if (isLinksAdded()) {
                            if (viewModel.getState().getValue().getLinksParentHandle() == INVALID_HANDLE) {
                                aB.setTitle(getResources().getString(R.string.title_shared_items));
                                viewModel.setIsFirstNavigationLevel(true);
                            } else {
                                MegaNode node = megaApi.getNodeByHandle(viewModel.getState().getValue().getLinksParentHandle());
                                aB.setTitle(node.getName());
                                viewModel.setIsFirstNavigationLevel(false);
                            }
                        }
                        break;
                    default: {
                        aB.setTitle(getResources().getString(R.string.title_shared_items));
                        viewModel.setIsFirstNavigationLevel(true);
                        break;
                    }
                }
                break;
            }
            case INBOX: {
                aB.setSubtitle(null);
                if (viewModel.getState().getValue().getInboxParentHandle() == megaApi.getInboxNode().getHandle() || viewModel.getState().getValue().getInboxParentHandle() == -1) {
                    aB.setTitle(getResources().getString(R.string.section_inbox));
                    viewModel.setIsFirstNavigationLevel(true);
                } else {
                    MegaNode node = megaApi.getNodeByHandle(viewModel.getState().getValue().getInboxParentHandle());
                    aB.setTitle(node.getName());
                    viewModel.setIsFirstNavigationLevel(false);
                }
                break;
            }
            case NOTIFICATIONS: {
                aB.setSubtitle(null);
                aB.setTitle(getString(R.string.title_properties_chat_contact_notifications));
                viewModel.setIsFirstNavigationLevel(true);
                break;
            }
            case CHAT: {
                abL.setVisibility(View.VISIBLE);
                aB.setTitle(getString(R.string.section_chat));

                viewModel.setIsFirstNavigationLevel(true);
                break;
            }
            case SEARCH: {
                aB.setSubtitle(null);
                if (searchViewModel.getState().getValue().getSearchParentHandle() == -1L) {
                    viewModel.setIsFirstNavigationLevel(true);
                    if (searchViewModel.getState().getValue().getSearchQuery() != null) {
                        searchViewModel.setTextSubmitted(true);
                        if (!searchViewModel.getState().getValue().getSearchQuery().isEmpty()) {
                            aB.setTitle(getString(R.string.action_search) + ": " + searchViewModel.getState().getValue().getSearchQuery());
                        } else {
                            aB.setTitle(getString(R.string.action_search) + ": " + "");
                        }
                    } else {
                        aB.setTitle(getString(R.string.action_search) + ": " + "");
                    }

                } else {
                    MegaNode parentNode = megaApi.getNodeByHandle(searchViewModel.getState().getValue().getSearchParentHandle());
                    if (parentNode != null) {
                        aB.setTitle(parentNode.getName());
                        viewModel.setIsFirstNavigationLevel(false);
                    }
                }
                break;
            }
            case TRANSFERS: {
                aB.setSubtitle(null);
                aB.setTitle(getString(R.string.section_transfers));
                setFirstNavigationLevel(true);
                break;
            }
            case PHOTOS: {
                aB.setSubtitle(null);
                if (getPhotosFragment() != null && photosFragment.isEnablePhotosFragmentShown()) {
                    setFirstNavigationLevel(false);
                    aB.setTitle(getString(R.string.settings_camera_upload_on));
                } else {
                    if (isInAlbumContent) {
                        aB.setTitle(getString(R.string.title_favourites_album));
                    } else {
                        setFirstNavigationLevel(true);
                        aB.setTitle(getString(R.string.sortby_type_photo_first));
                    }
                }
                break;
            }
            case HOMEPAGE: {
                setFirstNavigationLevel(false);
                int titleId = -1;

                switch (mHomepageScreen) {
                    case FAVOURITES:
                        titleId = R.string.favourites_category_title;
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
                    aB.setTitle(getString(titleId));
                }
            }
            default: {
                Timber.d("Default GONE");

                break;
            }
        }

        viewModel.checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NAVIGATION_TOOLBAR_ICON);
    }

    public void setToolbarTitleFromFullscreenOfflineFragment(String title,
                                                             boolean firstNavigationLevel, boolean showSearch) {
        aB.setSubtitle(null);
        aB.setTitle(title);
        viewModel.setIsFirstNavigationLevel(firstNavigationLevel);
        viewModel.checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NAVIGATION_TOOLBAR_ICON);
        searchViewModel.setTextSubmitted(true);
        if (searchMenuItem != null) {
            searchMenuItem.setVisible(showSearch);
        }
    }

    public void updateNavigationToolbarIcon(int numUnreadUserAlerts) {
        int totalIpc = 0;
        ArrayList<MegaContactRequest> requests = megaApi.getIncomingContactRequests();
        if (requests != null) {
            totalIpc = requests.size();
        }

        int totalNotifications = numUnreadUserAlerts + totalIpc;

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
        Timber.d("showOnlineMode");

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
                    Timber.w("showOnlineMode - Root is NULL");
                    if (getApplicationContext() != null) {
                        if (MegaApplication.getOpenChatId() != MEGACHAT_INVALID_HANDLE) {
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
        Timber.d("showConfirmationConnect");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        refreshSession();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        Timber.d("showConfirmationConnect: BUTTON_NEGATIVE");
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
        Timber.d("showOfflineMode");

        try {
            if (megaApi == null) {
                Timber.w("megaApi is Null in Offline mode");
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

            Timber.d("DrawerItem on start offline: %s", drawerItem);
            if (drawerItem == null) {
                Timber.w("drawerItem == null --> On start OFFLINE MODE");
                drawerItem = getStartDrawerItem(this);

                if (bNV != null) {
                    disableNavigationViewMenu(bNV.getMenu());
                }

                selectDrawerItem(drawerItem);
            } else {
                if (bNV != null) {
                    disableNavigationViewMenu(bNV.getMenu());
                }

                Timber.d("Change to OFFLINE MODE");
                clickDrawerItem(drawerItem);
            }

            supportInvalidateOptionsMenu();
        } catch (Exception e) {
        }
    }

    public void clickDrawerItem(DrawerItem item) {
        Timber.d("Item: %s", item);
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
        Timber.d("selectDrawerItemSharedItems");
        abL.setVisibility(View.VISIBLE);

        try {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.cancel(NOTIFICATION_PUSH_CLOUD_DRIVE);
        } catch (Exception e) {
            Timber.e(e, "Exception NotificationManager - remove contact notification");
        }

        if (sharesPageAdapter == null) {
            Timber.w("sharesPageAdapter is NULL");
            sharesPageAdapter = new SharesPageAdapter(this);
            viewPagerShares.setAdapter(sharesPageAdapter);
            new TabLayoutMediator(tabLayoutShares, viewPagerShares, (tab, position) -> {
                if (position == SharesTab.INCOMING_TAB.getPosition()) {
                    tab.setText(R.string.tab_incoming_shares);
                    tab.setIcon(R.drawable.ic_incoming_shares);
                } else if (position == SharesTab.OUTGOING_TAB.getPosition()) {
                    tab.setText(R.string.tab_outgoing_shares);
                    tab.setIcon(R.drawable.ic_outgoing_shares);
                } else if (position == SharesTab.LINKS_TAB.getPosition()) {
                    tab.setText(R.string.tab_links_shares);
                    tab.setIcon(R.drawable.link_ic);
                }
            }).attach();
        }

        updateSharesTab();
        setToolbarTitle();

        drawerLayout.closeDrawer(Gravity.LEFT);
    }

    public void selectDrawerItemNotifications() {
        Timber.d("selectDrawerItemNotifications");

        abL.setVisibility(View.VISIBLE);

        drawerItem = DrawerItem.NOTIFICATIONS;

        setBottomNavigationMenuItemChecked(NO_BNV);

        notificationsFragment = (NotificationsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.NOTIFICATIONS.getTag());
        if (notificationsFragment == null) {
            Timber.w("New NotificationsFragment");
            notificationsFragment = NotificationsFragment.newInstance();
        } else {
            refreshFragment(FragmentTag.NOTIFICATIONS.getTag());
        }
        replaceFragment(notificationsFragment, FragmentTag.NOTIFICATIONS.getTag());

        setToolbarTitle();

        showFabButton();
    }

    public void selectDrawerItemTransfers() {
        Timber.d("selectDrawerItemTransfers");

        abL.setVisibility(View.VISIBLE);
        hideTransfersWidget();

        drawerItem = DrawerItem.TRANSFERS;

        setBottomNavigationMenuItemChecked(NO_BNV);

        if (mTabsAdapterTransfers == null) {
            mTabsAdapterTransfers = new TransfersPageAdapter(getSupportFragmentManager(), this);
            viewPagerTransfers.setAdapter(mTabsAdapterTransfers);
            tabLayoutTransfers.setupWithViewPager(viewPagerTransfers);
        }

        getTransfersViewModel().checkIfShouldShowCompletedTab();
        setToolbarTitle();
        showFabButton();
        drawerLayout.closeDrawer(Gravity.LEFT);
    }

    /**
     * Updates the Transfers tab index.
     *
     * @param showCompleted True if should show the Completed tab, false otherwise.
     */
    private void updateTransfersTab(Boolean showCompleted) {
        if (viewPagerTransfers == null) {
            return;
        }


        viewModel.setTransfersTab(transfersManagement.getAreFailedTransfers() || showCompleted ? TransfersTab.COMPLETED_TAB : TransfersTab.PENDING_TAB);

        switch (viewModel.getState().getValue().getTransfersTab()) {
            case COMPLETED_TAB:
                refreshFragment(FragmentTag.COMPLETED_TRANSFERS.getTag());
                viewPagerTransfers.setCurrentItem(TransfersTab.COMPLETED_TAB.getPosition());
                break;

            default:
                refreshFragment(FragmentTag.TRANSFERS.getTag());
                viewPagerTransfers.setCurrentItem(TransfersTab.PENDING_TAB.getPosition());

                if (transfersManagement.getShouldShowNetworkWarning()) {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), MEGACHAT_INVALID_HANDLE);
                }

                break;
        }

        if (mTabsAdapterTransfers != null) {
            mTabsAdapterTransfers.notifyDataSetChanged();
        }

        viewModel.setTransfersTab(TransfersTab.Companion.fromPosition(viewPagerTransfers.getCurrentItem()));

        setToolbarTitle();
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
                SharesTab tabItemShares = getTabItemShares();

                if ((tabItemShares == SharesTab.INCOMING_TAB && viewModel.getState().getValue().getIncomingParentHandle() != INVALID_HANDLE)
                        || (tabItemShares == SharesTab.OUTGOING_TAB && viewModel.getState().getValue().getOutgoingParentHandle() != INVALID_HANDLE)
                        || (tabItemShares == SharesTab.LINKS_TAB && viewModel.getState().getValue().getLinksParentHandle() != INVALID_HANDLE)) {
                    tabLayoutShares.setVisibility(View.GONE);
                    viewPagerShares.setUserInputEnabled(false);
                } else {
                    tabLayoutShares.setVisibility(View.VISIBLE);
                    viewPagerShares.setUserInputEnabled(true);
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
    public void hideTabs(boolean hide, Tab currentTab) {
        int visibility = hide ? View.GONE : View.VISIBLE;

        switch (drawerItem) {
            case SHARED_ITEMS:
                switch ((SharesTab) currentTab) {
                    case INCOMING_TAB:
                        if (!isIncomingAdded() || (!hide && viewModel.getState().getValue().getIncomingParentHandle() != INVALID_HANDLE)) {
                            return;
                        }

                        break;

                    case OUTGOING_TAB:
                        if (!isOutgoingAdded() || (!hide && viewModel.getState().getValue().getOutgoingParentHandle() != INVALID_HANDLE)) {
                            return;
                        }

                        break;

                    case LINKS_TAB:
                        if (!isLinksAdded() || (!hide && viewModel.getState().getValue().getLinksParentHandle() != INVALID_HANDLE)) {
                            return;
                        }

                        break;
                }

                tabLayoutShares.setVisibility(visibility);
                viewPagerShares.setUserInputEnabled(!hide);
                break;

            case TRANSFERS:
                if (currentTab == TransfersTab.PENDING_TAB && !isTransfersInProgressAdded()) {
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

                updateTransfersWidget();
                setDrawerLockMode(false);
                return;
            } else if (destinationId == R.id.favouritesFragment) {
                mHomepageScreen = HomepageScreen.FAVOURITES;
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

            updateTransfersWidget();
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
        cuViewTypes.setVisibility(View.GONE);

        if (getPhotosFragment() != null) {
            photosFragment.setDefaultView();
            showBottomView();
        }
    }

    @SuppressLint("NewApi")
    public void selectDrawerItem(DrawerItem item) {
        if (item == null) {
            Timber.w("The selected DrawerItem is NULL. Using latest or default value.");
            item = drawerItem != null ? drawerItem : DrawerItem.CLOUD_DRIVE;
        }

        Timber.d("Selected DrawerItem: %s", item.name());

        // Homepage may hide the Appbar before
        abL.setVisibility(View.VISIBLE);

        drawerItem = item;
        MegaApplication.setRecentChatVisible(false);
        resetActionBar(aB);
        updateTransfersWidget();
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

        transfersManagement.setOnTransfersSection(item == DrawerItem.TRANSFERS);

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
                Timber.d("END for Cloud Drive");
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
                Timber.d("Chat selected");
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

        incomingSharesFragment = (MegaNodeBaseFragment) sharesPageAdapter.getFragment(SharesTab.INCOMING_TAB.getPosition());

        return incomingSharesFragment != null && incomingSharesFragment.isAdded();
    }

    private boolean isOutgoingAdded() {
        if (sharesPageAdapter == null) return false;

        outgoingSharesFragment = (MegaNodeBaseFragment) sharesPageAdapter.getFragment(SharesTab.OUTGOING_TAB.getPosition());

        return outgoingSharesFragment != null && outgoingSharesFragment.isAdded();
    }

    private boolean isLinksAdded() {
        if (sharesPageAdapter == null) return false;

        linksFragment = (MegaNodeBaseFragment)  sharesPageAdapter.getFragment(SharesTab.LINKS_TAB.getPosition());

        return linksFragment != null && linksFragment.isAdded();
    }

    private boolean isTransfersInProgressAdded() {
        if (mTabsAdapterTransfers == null) return false;

        transfersFragment = (TransfersFragment) mTabsAdapterTransfers.instantiateItem(viewPagerTransfers, TransfersTab.PENDING_TAB.getPosition());

        return transfersFragment.isAdded();
    }

    private boolean isTransfersCompletedAdded() {
        if (mTabsAdapterTransfers == null) return false;

        completedTransfersFragment = (CompletedTransfersFragment) mTabsAdapterTransfers.instantiateItem(viewPagerTransfers, TransfersTab.COMPLETED_TAB.getPosition());

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
                if (getTabItemShares() == SharesTab.INCOMING_TAB && isIncomingAdded())
                    incomingSharesFragment.checkScroll();
                else if (getTabItemShares() == SharesTab.OUTGOING_TAB && isOutgoingAdded())
                    outgoingSharesFragment.checkScroll();
                else if (getTabItemShares() == SharesTab.LINKS_TAB && isLinksAdded())
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
                if (getTabItemTransfers() == TransfersTab.PENDING_TAB && isTransfersInProgressAdded()) {
                    transfersFragment.checkScroll();
                } else if (getTabItemTransfers() == TransfersTab.COMPLETED_TAB && isTransfersCompletedAdded()) {
                    completedTransfersFragment.checkScroll();
                }
            }
        }
    }


    void showEnable2FADialog() {
        Timber.d("newAccount: %s", newAccount);
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
        searchViewModel.resetSearchQuery();
        drawerItem = searchViewModel.getState().getValue().getSearchDrawerItem();
        selectDrawerItem(drawerItem);
        searchViewModel.resetSearchDrawerItem();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Timber.d("onCreateOptionsMenu");
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
                Timber.d("onMenuItemActionExpand");
                searchExpand = true;
                if (drawerItem == DrawerItem.HOMEPAGE) {
                    if (mHomepageScreen == HomepageScreen.FULLSCREEN_OFFLINE) {
                        setFullscreenOfflineFragmentSearchQuery(searchViewModel.getState().getValue().getSearchQuery());
                    } else if (mHomepageSearchable != null) {
                        mHomepageSearchable.searchReady();
                    } else {
                        openSearchOnHomepage();
                    }
                } else if (drawerItem != DrawerItem.CHAT) {
                    viewModel.setIsFirstNavigationLevel(true);
                    searchViewModel.setSearchParentHandle(-1L);
                    searchViewModel.resetSearchDepth();
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
                Timber.d("onMenuItemActionCollapse()");
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
                        if (!searchViewModel.getState().getValue().getTextSubmitted()) {
                            setFullscreenOfflineFragmentSearchQuery(null);
                            searchViewModel.setTextSubmitted(true);
                        }
                        supportInvalidateOptionsMenu();
                    } else if (mHomepageSearchable != null) {
                        mHomepageSearchable.exitSearch();
                        searchViewModel.resetSearchQuery();
                        supportInvalidateOptionsMenu();
                    }
                } else {
                    cancelSearch();
                    searchViewModel.setTextSubmitted(true);
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
                        searchViewModel.setTextSubmitted(true);
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
                    searchViewModel.setSearchQuery("" + query);
                    setToolbarTitle();
                    Timber.d("Search query: %s", query);
                    searchViewModel.setTextSubmitted(true);
                    supportInvalidateOptionsMenu();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Timber.d("onQueryTextChange");
                if (drawerItem == DrawerItem.CHAT) {
                    searchViewModel.setSearchQuery(newText);
                    recentChatsFragment = (RecentChatsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
                    if (recentChatsFragment != null) {
                        recentChatsFragment.filterChats(newText, false);
                    }
                } else if (drawerItem == DrawerItem.HOMEPAGE) {
                    if (mHomepageScreen == HomepageScreen.FULLSCREEN_OFFLINE) {
                        if (searchViewModel.getState().getValue().getTextSubmitted()) {
                            searchViewModel.setTextSubmitted(false);
                            return true;
                        }

                        searchViewModel.setSearchQuery(newText);
                        setFullscreenOfflineFragmentSearchQuery(searchViewModel.getState().getValue().getSearchQuery());
                    } else if (mHomepageSearchable != null) {
                        searchViewModel.setSearchQuery(newText);
                        mHomepageSearchable.searchQuery(searchViewModel.getState().getValue().getSearchQuery());
                    }
                } else {
                    if (searchViewModel.getState().getValue().getTextSubmitted()) {
                        searchViewModel.setTextSubmitted(false);
                    } else {
                        searchViewModel.setSearchQuery(newText);
                        searchViewModel.performSearch(
                                viewModel.getState().getValue().getBrowserParentHandle(),
                                viewModel.getState().getValue().getRubbishBinParentHandle(),
                                viewModel.getState().getValue().getInboxParentHandle(),
                                viewModel.getState().getValue().getIncomingParentHandle(),
                                viewModel.getState().getValue().getOutgoingParentHandle(),
                                viewModel.getState().getValue().getLinksParentHandle(),
                                viewModel.getState().getValue().isFirstNavigationLevel()
                        );
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

                    if (getTabItemShares() == SharesTab.INCOMING_TAB && isIncomingAdded()) {
                        if (isIncomingAdded() && incomingSharesFragment.getItemCount() > 0) {
                            searchMenuItem.setVisible(true);
                        }
                    } else if (getTabItemShares() == SharesTab.OUTGOING_TAB && isOutgoingAdded()) {
                        if (isOutgoingAdded() && outgoingSharesFragment.getItemCount() > 0) {
                            searchMenuItem.setVisible(true);
                        }
                    } else if (getTabItemShares() == SharesTab.LINKS_TAB && isLinksAdded()) {
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
                    if (getTabItemTransfers() == TransfersTab.PENDING_TAB && isTransfersInProgressAdded() && transfersInProgress.size() > 0) {
                        if (megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD) || megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)) {
                            playTransfersMenuIcon.setVisible(true);
                        } else {
                            pauseTransfersMenuIcon.setVisible(true);
                        }

                        cancelAllTransfersMenuItem.setVisible(true);
                        enableSelectMenuItem.setVisible(true);
                    } else if (getTabItemTransfers() == TransfersTab.COMPLETED_TAB && isTransfersInProgressAdded() && completedTransfersFragment.isAnyTransferCompleted()) {
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

        Timber.d("Call to super onCreateOptionsMenu");
        return super.onCreateOptionsMenu(menu);
    }

    private void openSearchOnHomepage() {
        viewModel.setIsFirstNavigationLevel(true);
        searchViewModel.setSearchParentHandle(-1L);
        searchViewModel.resetSearchDepth();
        setSearchDrawerItem();
        selectDrawerItem(drawerItem);
        resetActionBar(aB);

        searchViewModel.performSearch(
                viewModel.getState().getValue().getBrowserParentHandle(),
                viewModel.getState().getValue().getRubbishBinParentHandle(),
                viewModel.getState().getValue().getInboxParentHandle(),
                viewModel.getState().getValue().getIncomingParentHandle(),
                viewModel.getState().getValue().getOutgoingParentHandle(),
                viewModel.getState().getValue().getLinksParentHandle(),
                viewModel.getState().getValue().isFirstNavigationLevel()
        );
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
        Timber.d("onOptionsItemSelected");
        typesCameraPermission = INVALID_TYPE_PERMISSIONS;

        if (megaApi == null) {
            megaApi = ((MegaApplication) getApplication()).getMegaApi();
        }

        if (megaApi != null) {
            Timber.d("retryPendingConnections");
            megaApi.retryPendingConnections();
        }

        if (megaChatApi != null) {
            megaChatApi.retryPendingConnections(false, null);
        }

        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                if (isFirstNavigationLevel() && drawerItem != DrawerItem.SEARCH) {
                    if (drawerItem == DrawerItem.RUBBISH_BIN || drawerItem == DrawerItem.INBOX
                            || drawerItem == DrawerItem.NOTIFICATIONS || drawerItem == DrawerItem.TRANSFERS) {

                        backToDrawerItem(bottomNavigationCurrentItem);
                        if (transfersToImageViewer) {
                            switchImageViewerToFront();
                        }
                    } else {
                        drawerLayout.openDrawer(nV);
                    }
                } else {
                    Timber.d("NOT firstNavigationLevel");
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
                        if (getTabItemShares() == SharesTab.INCOMING_TAB && isIncomingAdded()) {
                            incomingSharesFragment.onBackPressed();
                        } else if (getTabItemShares() == SharesTab.OUTGOING_TAB && isOutgoingAdded()) {
                            outgoingSharesFragment.onBackPressed();
                        } else if (getTabItemShares() == SharesTab.LINKS_TAB && isLinksAdded()) {
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
                        if (inboxFragment != null) {
                            inboxFragment.onBackPressed();
                            return true;
                        }
                    } else if (drawerItem == DrawerItem.SEARCH) {
                        if (getSearchFragment() != null) {
                            onBackPressed();
                            return true;
                        }
                    } else if (drawerItem == DrawerItem.TRANSFERS) {
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
            case R.id.action_search: {
                Timber.d("Action search selected");
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
                    Timber.d("Click on action_pause - play visible");
                    megaApi.pauseTransfers(true, this);
                    pauseTransfersMenuIcon.setVisible(false);
                    playTransfersMenuIcon.setVisible(true);
                }

                return true;
            }
            case R.id.action_play: {
                Timber.d("Click on action_play - pause visible");
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
                Timber.d("Action menu scan QR code pressed");
                //Check if there is a in progress call:
                checkBeforeOpeningQR(true);
                return true;
            }
            case R.id.action_return_call: {
                Timber.d("Action menu return to call in progress pressed");
                returnCall();
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
        searchViewModel.setTextSubmitted(false);

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

    /**
     * Method to return to an ongoing call
     */
    public void returnCall() {
        returnActiveCall(this, passcodeManagement);
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
        sharesPageAdapter.refreshFragment(SharesTab.INCOMING_TAB.getPosition());

        //Refresh shares section
        sharesPageAdapter.refreshFragment(SharesTab.OUTGOING_TAB.getPosition());

        //Refresh search section
        refreshFragment(FragmentTag.SEARCH.getTag());

        //Refresh inbox section
        refreshFragment(FragmentTag.INBOX.getTag());
    }

    public void refreshAfterMovingToRubbish() {
        Timber.d("refreshAfterMovingToRubbish");

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
                Timber.d("Updated nodes don't include CU/MU, return.");
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
            Timber.d("MU folder is deleted, backup settings and disable MU.");
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
            Timber.d("CU folder is deleted, backup settings and disable CU.");
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
            if (viewModel.getState().getValue().getRubbishBinParentHandle() == -1) {
                nodes = megaApi.getChildren(megaApi.getRubbishNode(), sortOrderManagement.getOrderCloud());
            } else {
                nodes = megaApi.getChildren(megaApi.getNodeByHandle(viewModel.getState().getValue().getRubbishBinParentHandle()),
                        sortOrderManagement.getOrderCloud());
            }

            rubbishBinFragment.hideMultipleSelect();
            rubbishBinFragment.setNodes(nodes);
            rubbishBinFragment.getRecyclerView().invalidate();
        }
    }

    public void refreshAfterMoving() {
        Timber.d("refreshAfterMoving");
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
        Timber.d("refreshAfterRemoving");

        rubbishBinFragment = (RubbishBinFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
        if (rubbishBinFragment != null) {
            rubbishBinFragment.hideMultipleSelect();
            refreshRubbishBin();
        }

        onNodesInboxUpdate();

        refreshSearch();
    }

    @Override
    public void onBackPressed() {
        Timber.d("onBackPressed");

        // Let the PSA web browser fragment (if visible) to consume the back key event
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return;

        retryConnectionsAndSignalPresence();

        if (drawerLayout.isDrawerOpen(nV)) {
            drawerLayout.closeDrawer(Gravity.LEFT);
            return;
        }

        dismissAlertDialogIfExists(statusDialog);

        Timber.d("DRAWERITEM: %s", drawerItem);

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

            if (transfersToImageViewer) {
                switchImageViewerToFront();
            }
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
     * This activity was called by {@link mega.privacy.android.app.imageviewer.ImageViewerActivity}
     * by putting itself to the front of the history stack. Switching back to the image viewer requires
     * the same process again (reordering and therefore putting the image viewer to the front).
     */
    private void switchImageViewerToFront() {
        transfersToImageViewer = false;
        startActivity(ImageViewerActivity.getIntentFromBackStack(this));
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
        Timber.d("onNavigationItemSelected");

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
                        Timber.e("Root node is null");
                    }

                    if (viewModel.getState().getValue().getBrowserParentHandle() != INVALID_HANDLE
                            && rootNode != null && viewModel.getState().getValue().getBrowserParentHandle() != rootNode.getHandle()) {
                        viewModel.setBrowserParentHandle(rootNode.getHandle());
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
                    if (getTabItemShares() == SharesTab.INCOMING_TAB && viewModel.getState().getValue().getIncomingParentHandle() != INVALID_HANDLE) {
                        viewModel.setIncomingParentHandle(INVALID_HANDLE);
                        viewModel.resetIncomingTreeDepth();
                        refreshIncomingShares();
                    } else if (getTabItemShares() == SharesTab.OUTGOING_TAB && viewModel.getState().getValue().getOutgoingParentHandle() != INVALID_HANDLE) {
                        viewModel.setOutgoingParentHandle(INVALID_HANDLE);
                        viewModel.resetOutgoingTreeDepth();
                        refreshOutgoingShares();
                    } else if (getTabItemShares() == SharesTab.LINKS_TAB && viewModel.getState().getValue().getLinksParentHandle() != INVALID_HANDLE) {
                        viewModel.setLinksParentHandle(INVALID_HANDLE);
                        viewModel.resetLinksTreeDepth();
                        refreshLinks();
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
        checkNameCollisionUseCase.checkRestorations(nodes)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result, throwable) -> {
                    if (throwable == null) {
                        ArrayList<NameCollision> collisions = result.getFirst();
                        if (!collisions.isEmpty()) {
                            nameCollisionActivityContract.launch(collisions);
                        }

                        List<MegaNode> nodesWithoutCollisions = result.getSecond();
                        if (!nodesWithoutCollisions.isEmpty()) {
                            proceedWithRestoration(nodesWithoutCollisions);
                        }
                    }
                });
    }

    private void proceedWithRestoration(List<MegaNode> nodes) {
        moveNodeUseCase.restore(nodes)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result, throwable) -> {
                    if (throwable == null) {
                        boolean notValidView = result.isSingleAction() && result.isSuccess()
                                && viewModel.getState().getValue().getRubbishBinParentHandle() == nodes.get(0).getHandle();

                        showRestorationOrRemovalResult(notValidView, result.getResultText());
                    } else if (throwable instanceof ForeignNodeException) {
                        launchForeignNodeError();
                    }
                });
    }

    /**
     * Shows the final result of a restoration or removal from Rubbish Bin section.
     *
     * @param notValidView True if should update the view, false otherwise.
     * @param message      Text message to show as the request result.
     */
    private void showRestorationOrRemovalResult(boolean notValidView, String message) {
        if (notValidView) {
            viewModel.setRubbishBinParentHandle(INVALID_HANDLE);
            setToolbarTitle();
            refreshRubbishBin();
        }

        dismissAlertDialogIfExists(statusDialog);
        showSnackbar(SNACKBAR_TYPE, message, MEGACHAT_INVALID_HANDLE);
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
        Timber.d("Handle: %s", handle);
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
        Timber.d("showKeyboardDelayed");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 50);
    }

    /**
     * Move folders or files that belong to "My backups"
     *
     * @param handleList handleList handles list of the nodes that selected
     */
    public void moveBackupNode(final ArrayList<Long> handleList) {
        Timber.d("MyBackup + NodeOptionsBottomSheetDialogFragment Move a backup folder or file");
        fileBackupManager.moveBackup(nC, handleList);
    }

    /**
     * Delete folders or files that included "My backup"
     *
     * @param handleList handleList handles list of the nodes that selected
     */
    public void askConfirmationMoveToRubbish(final ArrayList<Long> handleList) {
        Timber.d("askConfirmationMoveToRubbish");

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
                                                    && viewModel.getState().getValue().getRubbishBinParentHandle() == handleList.get(0);

                                            showRestorationOrRemovalResult(notValidView, result.getResultText());
                                        }
                                    }));

                    builder.setNegativeButton(R.string.general_cancel, null);
                    builder.show();
                }
            }
        } else {
            Timber.w("handleList NULL");
            return;
        }

    }

    public void showWarningDialogOfShare(final MegaNode p, int nodeType, int actionType) {
        Timber.d("showWarningDialogOfShareFolder");
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
                    viewModel.setBrowserParentHandle(result.getOldParentHandle());
                    refreshCloudDrive();
                    break;

                case INBOX:
                    viewModel.setInboxParentHandle(result.getOldParentHandle());
                    refreshInboxList();
                    break;

                case SHARED_ITEMS:
                    switch (getTabItemShares()) {
                        case INCOMING_TAB:
                            viewModel.decreaseIncomingTreeDepth();
                            viewModel.setIncomingParentHandle(
                                    viewModel.getState().getValue().getIncomingTreeDepth() == 0 ? INVALID_HANDLE : result.getOldParentHandle());
                            refreshIncomingShares();
                            break;

                        case OUTGOING_TAB:
                            viewModel.decreaseOutgoingTreeDepth();
                            viewModel.setOutgoingParentHandle(
                                    viewModel.getState().getValue().getOutgoingTreeDepth() == 0 ? INVALID_HANDLE : result.getOldParentHandle());

                            if (viewModel.getState().getValue().getOutgoingParentHandle() == INVALID_HANDLE) {
                                hideTabs(false, SharesTab.OUTGOING_TAB);
                            }

                            refreshOutgoingShares();
                            break;

                        case LINKS_TAB:
                            viewModel.decreaseLinksTreeDepth();
                            viewModel.setLinksParentHandle(
                                    viewModel.getState().getValue().getLinksTreeDepth() == 0 ? INVALID_HANDLE : result.getOldParentHandle());

                            if (viewModel.getState().getValue().getLinksParentHandle() == INVALID_HANDLE) {
                                hideTabs(false, SharesTab.LINKS_TAB);
                            }

                            refreshLinks();
                            break;
                    }

                case SEARCH:
                    searchViewModel.setSearchParentHandle(searchViewModel.getState().getValue().getSearchDepth() > 0 ? result.getOldParentHandle() : INVALID_HANDLE);
                    searchViewModel.decreaseSearchDepth();
                    refreshSearch();
                    break;

            }

            setToolbarTitle();
        }

        showSnackbar(SNACKBAR_TYPE, result.getResultText(), MEGACHAT_INVALID_HANDLE);
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
                        Timber.d("Open chat link: correct chat link");
                        // Identify the link is a meeting or normal chat link
                        megaChatApi.checkChatLink(link, new LoadPreviewListener(ManagerActivity.this, ManagerActivity.this, CHECK_LINK_TYPE_UNKNOWN_LINK));
                        dismissAlertDialogIfExists(openLinkDialog);
                        break;
                    }
                    case CONTACT_LINK: {
                        Timber.d("Open contact link: correct contact link");
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
                        Timber.d("Do nothing: correct file or folder link");
                        dismissAlertDialogIfExists(openLinkDialog);
                        break;
                    }
                    case CHAT_LINK:
                    case CONTACT_LINK:
                    case ERROR_LINK: {
                        Timber.w("Show error: invalid link or correct chat or contact link");
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
            Timber.e(e, "Exception showing Open Link dialog");
        }
    }

    public void showChatLink(String link) {
        Timber.d("Link: %s", link);
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
        Timber.d("showPresenceStatusDialog");

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
        MegaNode parent = getCurrentParentNode(getCurrentParentHandle(), INVALID_VALUE);
        if (parent == null) {
            return;
        }

        newFolderDialog = MegaNodeDialogUtil.showNewFolderDialog(this, this, parent, typedText);
    }

    @Override
    public void showNewTextFileDialog(String typedName) {
        MegaNode parent = getCurrentParentNode(getCurrentParentHandle(), INVALID_VALUE);
        if (parent == null) {
            return;
        }

        newTextFileDialog = MegaNodeDialogUtil.showNewTxtFileDialog(this, parent, typedName,
                drawerItem == DrawerItem.HOMEPAGE);
    }

    public long getParentHandleBrowser() {
        return viewModel.getSafeBrowserParentHandle();
    }

    private long getCurrentParentHandle() {
        long parentHandle = -1;

        switch (drawerItem) {
            case HOMEPAGE:
                // For home page, its parent is always the root of cloud drive.
                parentHandle = megaApi.getRootNode().getHandle();
                break;
            case CLOUD_DRIVE:
                parentHandle = viewModel.getSafeBrowserParentHandle();
                break;

            case INBOX:
                parentHandle = viewModel.getState().getValue().getInboxParentHandle();
                break;

            case RUBBISH_BIN:
                parentHandle = viewModel.getState().getValue().getRubbishBinParentHandle();
                break;

            case SHARED_ITEMS:
                if (viewPagerShares == null) break;

                if (getTabItemShares() == SharesTab.INCOMING_TAB) {
                    parentHandle = viewModel.getState().getValue().getIncomingParentHandle();
                } else if (getTabItemShares() == SharesTab.OUTGOING_TAB) {
                    parentHandle = viewModel.getState().getValue().getOutgoingParentHandle();
                } else if (getTabItemShares() == SharesTab.LINKS_TAB) {
                    parentHandle = viewModel.getState().getValue().getLinksParentHandle();
                }
                break;

            case SEARCH:
                if (searchViewModel.getState().getValue().getSearchParentHandle() != -1L) {
                    parentHandle = searchViewModel.getState().getValue().getSearchParentHandle();
                    break;
                }
                if (searchViewModel.getState().getValue().getSearchDrawerItem() != null) {
                    switch (searchViewModel.getState().getValue().getSearchDrawerItem()) {
                        case CLOUD_DRIVE:
                            parentHandle = viewModel.getSafeBrowserParentHandle();
                            break;
                        case SHARED_ITEMS:
                            switch (searchViewModel.getState().getValue().getSearchSharesTab()) {
                                case INCOMING_TAB:
                                    parentHandle = viewModel.getState().getValue().getIncomingParentHandle();
                                    break;
                                case OUTGOING_TAB:
                                    parentHandle = viewModel.getState().getValue().getOutgoingParentHandle();
                                    break;
                                case LINKS_TAB:
                                    parentHandle = viewModel.getState().getValue().getLinksParentHandle();
                                    break;
                            }
                            break;
                        case INBOX:
                            parentHandle = viewModel.getState().getValue().getInboxParentHandle();
                            break;
                    }
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
            Timber.d("%s: parentHandle == -1", errorString);
            return null;
        }

        MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);

        if (parentNode == null && errorString != null) {
            showSnackbar(SNACKBAR_TYPE, errorString, -1);
            Timber.d("%s: parentNode == null", errorString);
            return null;
        }

        return parentNode;
    }

    @Override
    public void createFolder(@NotNull String title) {
        Timber.d("createFolder");
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
                Timber.d("Folder not created: folder already exists");
                return;
            }
        }

        statusDialog = createProgressDialog(this, StringResourcesUtils.getString(R.string.context_creating_folder));
        megaApi.createFolder(title, parentNode, this);
    }

    public void showClearRubbishBinDialog() {
        Timber.d("showClearRubbishBinDialog");

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
        Timber.d("chooseAddContactDialog");
        if (megaApi != null && megaApi.getRootNode() != null) {
            Intent intent = new Intent(this, AddContactActivity.class);
            intent.putExtra("contactType", CONTACT_TYPE_MEGA);
            startActivityForResult(intent, REQUEST_CREATE_CHAT);
        } else {
            Timber.w("Online but not megaApi");
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
        windowContent.addView(fabMaskLayout);
    }

    /**
     * Removing the full screen mask
     */
    private void removeMask() {
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
        builder.setMessage(getQuantityString(R.plurals.alert_remove_several_shares, shares.size(), shares.size()))
                .setPositiveButton(R.string.shared_items_outgoing_unshare_confirm_dialog_button_yes, (dialog, which) -> nC.removeSeveralFolderShares(shares))
                .setNegativeButton(R.string.shared_items_outgoing_unshare_confirm_dialog_button_no, (dialog, which) -> {
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
        Timber.d("showConfirmationRemovePublicLink");

        if (showTakenDownNodeActionNotAvailableDialog(n, this)) {
            return;
        }

        ArrayList<MegaNode> nodes = new ArrayList<>();
        nodes.add(n);
        showConfirmationRemoveSeveralPublicLinks(nodes);
    }

    public void showConfirmationRemoveSeveralPublicLinks(ArrayList<MegaNode> nodes) {
        if (nodes == null) {
            Timber.w("nodes == NULL");
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
        Timber.d("cameraUplaodsClicked");
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
        Timber.d("showNodeOptionsPanel");

        if (node == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedNode = node;
        bottomSheetDialogFragment = new NodeOptionsBottomSheetDialogFragment(mode);
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void showNodeLabelsPanel(@NonNull MegaNode node) {
        Timber.d("showNodeLabelsPanel");

        if (isBottomSheetDialogShown(bottomSheetDialogFragment)) {
            bottomSheetDialogFragment.dismiss();
        }

        selectedNode = node;
        bottomSheetDialogFragment = NodeLabelBottomSheetDialogFragment.newInstance(node.getHandle());
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void showOptionsPanel(MegaOffline sNode) {
        Timber.d("showNodeOptionsPanel-Offline");

        if (sNode == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedOfflineNode = sNode;
        bottomSheetDialogFragment = new OfflineOptionsBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void showNewSortByPanel(int orderType) {
        if (isBottomSheetDialogShown(bottomSheetDialogFragment)) {
            return;
        }

        if (orderType == ORDER_OTHERS && viewModel.getState().getValue().getIncomingTreeDepth() > 0) {
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
        Timber.d("updateAccountDetailsVisibleInfo");
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
                    Timber.w(e, "Exception formatting string");
                }
                spaceTV.setText(HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY));
                int progress = myAccountInfo.getUsedPercentage();
                long usedSpace = myAccountInfo.getUsedStorage();
                Timber.d("Progress: %d, Used space: %d", progress, usedSpace);
                usedSpacePB.setProgress(progress);
                if (progress >= 0 && usedSpace >= 0) {
                    usedSpaceLayout.setVisibility(View.VISIBLE);
                } else {
                    usedSpaceLayout.setVisibility(View.GONE);
                }
            }
        } else {
            Timber.w("usedSpaceLayout is NULL");
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
            Timber.w("Root node is NULL. Maybe user is not logged in");
            return;
        }

        MegaNode parentNode = rootNode;

        if (isCloudAdded()) {
            ArrayList<MegaNode> nodes;
            if (viewModel.getState().getValue().getBrowserParentHandle() == -1) {
                nodes = megaApi.getChildren(parentNode, sortOrderManagement.getOrderCloud());
            } else {
                parentNode = megaApi.getNodeByHandle(viewModel.getState().getValue().getBrowserParentHandle());
                if (parentNode == null) return;

                nodes = megaApi.getChildren(parentNode, sortOrderManagement.getOrderCloud());
            }
            Timber.d("Nodes: %s", nodes.size());
            if (comesFromNotificationChildNodeHandleList == null) {
                fileBrowserFragment.hideMultipleSelect();
            }
            fileBrowserFragment.setNodes(nodes);
            fileBrowserFragment.getRecyclerView().invalidate();
        }
    }

    private void refreshSharesPageAdapter() {
        if (sharesPageAdapter != null) {
            sharesPageAdapter.refreshFragment(SharesTab.INCOMING_TAB.getPosition());
            sharesPageAdapter.refreshFragment(SharesTab.OUTGOING_TAB.getPosition());
            sharesPageAdapter.refreshFragment(SharesTab.LINKS_TAB.getPosition());
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
        Timber.d("Set value to: %s", firstNavigationLevel);
        viewModel.setIsFirstNavigationLevel(firstNavigationLevel);
    }

    public boolean isFirstNavigationLevel() {
        return viewModel.getState().getValue().isFirstNavigationLevel();
    }

    public void setParentHandleBrowser(long parentHandleBrowser) {
        Timber.d("Set value to:%s", parentHandleBrowser);

        viewModel.setBrowserParentHandle(parentHandleBrowser);
    }

    public void setParentHandleRubbish(long parentHandleRubbish) {
        Timber.d("setParentHandleRubbish");
        viewModel.setRubbishBinParentHandle(parentHandleRubbish);
    }

    public void setParentHandleIncoming(long parentHandleIncoming) {
        Timber.d("setParentHandleIncoming: %s", parentHandleIncoming);
        viewModel.setIncomingParentHandle(parentHandleIncoming);
    }

    public void setParentHandleInbox(long parentHandleInbox) {
        Timber.d("setParentHandleInbox: %s", parentHandleInbox);
        viewModel.setInboxParentHandle(parentHandleInbox);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Timber.d("onNewIntent");

        if (intent != null) {
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                searchViewModel.setSearchQuery(intent.getStringExtra(SearchManager.QUERY));
                searchViewModel.setSearchParentHandle(-1L);
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
                if (intent.getBooleanExtra(OPENED_FROM_IMAGE_VIEWER, false)) {
                    transfersToImageViewer = true;
                }

                drawerItem = DrawerItem.TRANSFERS;
                if (intent.getSerializableExtra(TRANSFERS_TAB) != null)
                    viewModel.setTransfersTab( (TransfersTab) intent.getSerializableExtra(TRANSFERS_TAB));
                else
                    viewModel.setTransfersTab(TransfersTab.NONE);
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
        Timber.d("navigateToAchievements");
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
        Timber.d("navigateToMyAccount");
        getProLayout.setVisibility(View.GONE);
        showMyAccount();
    }

    @Override
    public void onClick(View v) {
        Timber.d("onClick");

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
                Timber.d("Click on Upgrade in pro panel!");
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
                sectionClicked = true;
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
                returnCall();
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
        Timber.d("showConfirmationRemoveFromOffline");

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
        Timber.d("showConfirmationRemoveSomeFromOffline");

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
    protected boolean manageCopyMoveException(Throwable throwable) {
        if (throwable instanceof ForeignNodeException) {
            launchForeignNodeError();
            return true;
        } else if (throwable instanceof QuotaExceededMegaException) {
            showOverquotaAlert(false);
            return true;
        } else if (throwable instanceof NotEnoughQuotaMegaException) {
            showOverquotaAlert(true);
            return true;
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Timber.d("Request code: %d, Result code:%d", requestCode, resultCode);

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
                Timber.w("Intent NULL");
                return;
            }

            Timber.d("Intent action: %s", intent.getAction());
            Timber.d("Intent type: %s", intent.getType());

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
            if (intent != null && resultCode == RESULT_OK) {
                String result = intent.getStringExtra(EXTRA_ACTION_RESULT);
                if (isTextEmpty(result)) {
                    return;
                }

                showSnackbar(SNACKBAR_TYPE, result, MEGACHAT_INVALID_HANDLE);
            }
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
            Timber.d("Create the document : %s", treeUri);
            long handleToDownload = intent.getLongExtra("handleToDownload", -1);
            Timber.d("The recovered handle is: %s", handleToDownload);
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
            Timber.d("requestCode == REQUEST_CODE_SELECT_FILE");

            if (intent == null) {
                Timber.w("Intent NULL");
                return;
            }

            nodeAttacher.handleSelectFileResult(intent, this);
        } else if (requestCode == REQUEST_CODE_SELECT_FOLDER && resultCode == RESULT_OK) {
            Timber.d("REQUEST_CODE_SELECT_FOLDER");

            if (intent == null) {
                Timber.d("Intent NULL");
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
            Timber.d("onActivityResult REQUEST_CODE_SELECT_CONTACT OK");

            if (intent == null) {
                Timber.w("Intent NULL");
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
        } else if (requestCode == REQUEST_CODE_SELECT_FOLDER_TO_MOVE && resultCode == RESULT_OK) {

            if (intent == null) {
                Timber.d("Intent NULL");
                return;
            }

            final long[] moveHandles = intent.getLongArrayExtra("MOVE_HANDLES");
            final long toHandle = intent.getLongExtra("MOVE_TO", 0);

            if (fileBackupManager.moveToBackup(moveNodeUseCase, moveHandles, toHandle)) {
                return;
            }

            checkNameCollisionUseCase.checkHandleList(moveHandles, toHandle, NameCollisionType.MOVE)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((result, throwable) -> {
                        if (throwable == null) {
                            ArrayList<NameCollision> collisions = result.getFirst();

                            if (!collisions.isEmpty()) {
                                dismissAlertDialogIfExists(statusDialog);
                                nameCollisionActivityContract.launch(collisions);
                            }

                            long[] handlesWithoutCollision = result.getSecond();

                            if (handlesWithoutCollision.length > 0) {
                                moveNodeUseCase.move(handlesWithoutCollision, toHandle)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe((moveResult, moveThrowable) -> {
                                            if (!manageCopyMoveException(moveThrowable)) {
                                                showMovementResult(moveResult, handlesWithoutCollision[0]);
                                            }
                                        });
                            }
                        }
                    });
        } else if (requestCode == REQUEST_CODE_SELECT_FOLDER_TO_COPY && resultCode == RESULT_OK) {
            Timber.d("REQUEST_CODE_SELECT_COPY_FOLDER");

            if (intent == null) {
                Timber.w("Intent NULL");
                return;
            }
            final long[] copyHandles = intent.getLongArrayExtra("COPY_HANDLES");
            final long toHandle = intent.getLongExtra("COPY_TO", 0);

            if (fileBackupManager.copyNodesToBackups(nC, copyHandles, toHandle)) {
                return;
            }

            checkNameCollisionUseCase.checkHandleList(copyHandles, toHandle, NameCollisionType.COPY)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((result, throwable) -> {
                        if (throwable == null) {
                            ArrayList<NameCollision> collisions = result.getFirst();

                            if (!collisions.isEmpty()) {
                                dismissAlertDialogIfExists(statusDialog);
                                nameCollisionActivityContract.launch(collisions);
                            }

                            long[] handlesWithoutCollision = result.getSecond();

                            if (handlesWithoutCollision.length > 0) {
                                copyNodeUseCase.copy(handlesWithoutCollision, toHandle)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe((copyResult, copyThrowable) -> {
                                            dismissAlertDialogIfExists(statusDialog);
                                            if (!manageCopyMoveException(copyThrowable)) {
                                                showCopyResult(copyResult);
                                            }
                                        });
                            }
                        }
                    });
        } else if (requestCode == REQUEST_CODE_REFRESH_API_SERVER && resultCode == RESULT_OK) {
            Timber.d("Resfresh DONE");

            if (intent == null) {
                Timber.w("Intent NULL");
                return;
            }

            ((MegaApplication) getApplication()).askForFullAccountInfo();
            ((MegaApplication) getApplication()).askForExtendedAccountDetails();

            if (drawerItem == DrawerItem.CLOUD_DRIVE) {
                viewModel.setBrowserParentHandle(intent.getLongExtra(INTENT_EXTRA_KEY_PARENT_HANDLE, INVALID_HANDLE));
                MegaNode parentNode = megaApi.getNodeByHandle(viewModel.getState().getValue().getBrowserParentHandle());

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
            Timber.d("TAKE_PHOTO_CODE");
            if (resultCode == Activity.RESULT_OK) {
                long parentHandle = getCurrentParentHandle();
                File file = getTemporalTakePictureFile(this);
                if (file != null) {
                    checkNameCollisionUseCase.check(file.getName(), parentHandle)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(handle -> {
                                        ArrayList<NameCollision> list = new ArrayList<>();
                                        list.add(NameCollision.Upload.getUploadCollision(handle,
                                                file, parentHandle));
                                        nameCollisionActivityContract.launch(list);
                                    },
                                    throwable -> {
                                        if (throwable instanceof MegaNodeException.ParentDoesNotExistException) {
                                            showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.general_error), MEGACHAT_INVALID_HANDLE);
                                        } else if (throwable instanceof MegaNodeException.ChildDoesNotExistsException) {
                                            uploadUseCase.upload(this, file, parentHandle)
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(() -> Timber.d("Upload started"));
                                        }
                                    });
                }
            } else {
                Timber.w("TAKE_PHOTO_CODE--->ERROR!");
            }
        } else if (requestCode == REQUEST_CODE_SORT_BY && resultCode == RESULT_OK) {

            if (intent == null) {
                Timber.w("Intent NULL");
                return;
            }

            int orderGetChildren = intent.getIntExtra("ORDER_GET_CHILDREN", 1);
            if (drawerItem == DrawerItem.CLOUD_DRIVE) {
                MegaNode parentNode = megaApi.getNodeByHandle(viewModel.getState().getValue().getBrowserParentHandle());
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
            Timber.d("REQUEST_CREATE_CHAT OK");

            if (intent == null) {
                Timber.w("Intent NULL");
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
                    Timber.d("Create one to one chat");
                    MegaUser user = megaApi.getContact(contactsData.get(0));
                    if (user != null) {
                        Timber.d("Chat with contact: %s", contactsData.size());
                        startOneToOneChat(user);
                    }
                } else {
                    Timber.d("Create GROUP chat");
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
            Timber.d("REQUEST_INVITE_CONTACT_FROM_DEVICE OK");

            if (intent == null) {
                Timber.w("Intent NULL");
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

                Timber.d("REQUEST_DOWNLOAD_FOLDER:path to download: %s", path);
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
            Timber.w("No request code processed");
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    /**
     * Shows the copy result.
     *
     * @param result Object containing the request result.
     */
    private void showCopyResult(CopyRequestResult result) {
        showSnackbar(SNACKBAR_TYPE, result.getResultText(), MEGACHAT_INVALID_HANDLE);

        if (result.getSuccessCount() <= 0) {
            return;
        }

        if (drawerItem == DrawerItem.CLOUD_DRIVE) {
            if (isCloudAdded()) {
                ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(viewModel.getState().getValue().getBrowserParentHandle()),
                        sortOrderManagement.getOrderCloud());
                fileBrowserFragment.setNodes(nodes);
                fileBrowserFragment.getRecyclerView().invalidate();
            }
        } else if (drawerItem == DrawerItem.RUBBISH_BIN) {
            refreshRubbishBin();
        } else if (drawerItem == DrawerItem.INBOX) {
            refreshInboxList();
        }
    }

    public void createGroupChat(MegaChatPeerList peers, String chatTitle, boolean chatLink, boolean isEKR) {

        Timber.d("Create group chat with participants: %s", peers.size());

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
        Timber.d("User Handle: %s", user.getHandle());
        MegaChatRoom chat = megaChatApi.getChatRoomByUser(user.getHandle());
        MegaChatPeerList peers = MegaChatPeerList.createInstance();
        if (chat == null) {
            Timber.d("No chat, create it!");
            peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
            megaChatApi.createChat(false, peers, this);
        } else {
            Timber.d("There is already a chat, open it!");
            Intent intentOpenChat = new Intent(this, ChatActivity.class);
            intentOpenChat.setAction(ACTION_CHAT_SHOW_MESSAGES);
            intentOpenChat.putExtra(CHAT_ID, chat.getChatId());
            this.startActivity(intentOpenChat);
        }
    }

    void disableNavigationViewMenu(Menu menu) {
        Timber.d("disableNavigationViewMenu");

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

        viewModel.checkInboxSectionVisibility();

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
            viewModel.checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NOTIFICATIONS_TITLE);
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
        Timber.d("resetNavigationViewMenu()");

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
            viewModel.checkInboxSectionVisibility();
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
            viewModel.checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NOTIFICATIONS_TITLE);
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
        Timber.d("setInboxNavigationDrawer");
        if (nV != null && inboxSection != null) {
            viewModel.checkInboxSectionVisibility();
        }
    }

    public void showProPanel() {
        Timber.d("showProPanel");
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
                Timber.d("STORAGE STATE GREEN");

                intent.setAction(ACTION_STORAGE_STATE_CHANGED);

                // TODO: WORKAROUND, NEED TO IMPROVE AND REMOVE THE TRY-CATCH
                try {
                    ContextCompat.startForegroundService(this, intent);
                } catch (Exception e) {
                    Timber.e(e, "Exception starting UploadService");
                }

                if (myAccountInfo.getAccountType() == MegaAccountDetails.ACCOUNT_TYPE_FREE) {
                    Timber.d("ACCOUNT TYPE FREE");
                    if (showMessageRandom()) {
                        Timber.d("Show message random: TRUE");
                        showProPanel();
                    }
                }
                storageState = newStorageState;
                fireCameraUploadJob(ManagerActivity.this, false);
                break;

            case MegaApiJava.STORAGE_STATE_ORANGE:
                Timber.w("STORAGE STATE ORANGE");

                intent.setAction(ACTION_STORAGE_STATE_CHANGED);

                // TODO: WORKAROUND, NEED TO IMPROVE AND REMOVE THE TRY-CATCH
                try {
                    ContextCompat.startForegroundService(this, intent);
                } catch (Exception e) {
                    Timber.e(e, "Exception starting UploadService");
                    e.printStackTrace();
                }

                if (onCreate && isStorageStatusDialogShown) {
                    isStorageStatusDialogShown = false;
                    showStorageAlmostFullDialog();
                } else if (newStorageState > storageState) {
                    showStorageAlmostFullDialog();
                }
                storageState = newStorageState;
                Timber.d("Try to start CU, false.");
                fireCameraUploadJob(ManagerActivity.this, false);
                break;

            case MegaApiJava.STORAGE_STATE_RED:
                Timber.w("STORAGE STATE RED");
                if (onCreate && isStorageStatusDialogShown) {
                    isStorageStatusDialogShown = false;
                    showStorageFullDialog();
                } else if (newStorageState > storageState) {
                    showStorageFullDialog();
                }
                break;

            case MegaApiJava.STORAGE_STATE_PAYWALL:
                Timber.w("STORAGE STATE PAYWALL");
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
        Timber.d("showStorageAlmostFullDialog");
        showStorageStatusDialog(MegaApiJava.STORAGE_STATE_ORANGE, false, false);
    }

    /**
     * Show a dialog to indicate that the storage space is full.
     */
    public void showStorageFullDialog() {
        Timber.d("showStorageFullDialog");
        showStorageStatusDialog(MegaApiJava.STORAGE_STATE_RED, false, false);
    }

    /**
     * Show an overquota alert dialog.
     *
     * @param preWarning Flag to indicate if is a pre-overquota alert or not.
     */
    public void showOverquotaAlert(boolean preWarning) {
        Timber.d("preWarning: %s", preWarning);
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
        Timber.d("is achievement user: %s", isAchievementUser);
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
        Timber.d("showStorageStatusDialog");

        if (myAccountInfo.getAccountType() == -1) {
            Timber.w("Do not show dialog, not info of the account received yet");
            return;
        }

        if (isStorageStatusDialogShown) {
            Timber.d("Storage status dialog already shown");
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
                Timber.d("STORAGE STATE GREEN");
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
                Timber.w("STORAGE STATE INVALID VALUE: %d", storageState);
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
                Timber.d("Go to achievements section");
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
                Timber.d("Show storage status dialog for USER PRO III");
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
                Timber.d("Show storage status dialog for USER PRO");
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
                Timber.d("Show storage status dialog for FREE USER");
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
            Timber.w("Products haven't been initialized!");
        }
        return null;
    }

    private void refreshOfflineNodes() {
        Timber.d("updateOfflineView");
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
        Timber.d("onIntentProcessed");

        MegaNode parentNode = getCurrentParentNode(getCurrentParentHandle(), -1);
        if (parentNode == null) {
            dismissAlertDialogIfExists(statusDialog);
            dismissAlertDialogIfExists(processFileDialog);
            showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.error_temporary_unavaible), -1);
            return;
        }

        if (infos == null) {
            dismissAlertDialogIfExists(statusDialog);
            dismissAlertDialogIfExists(processFileDialog);
            showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.upload_can_not_open), MEGACHAT_INVALID_HANDLE);
            return;
        }

        if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
            dismissAlertDialogIfExists(statusDialog);
            dismissAlertDialogIfExists(processFileDialog);
            showOverDiskQuotaPaywallWarning();
            return;
        }

        checkNameCollisionUseCase.checkShareInfoList(infos, parentNode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result, throwable) -> {
                    dismissAlertDialogIfExists(statusDialog);
                    dismissAlertDialogIfExists(processFileDialog);

                    if (throwable != null) {
                        showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.error_temporary_unavaible), MEGACHAT_INVALID_HANDLE);
                    } else {
                        ArrayList<NameCollision> collisions = result.getFirst();
                        List<ShareInfo> withoutCollisions = result.getSecond();

                        if (!collisions.isEmpty()) {
                            nameCollisionActivityContract.launch(collisions);
                        }

                        if (!withoutCollisions.isEmpty()) {
                            showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getQuantityString(R.plurals.upload_began, withoutCollisions.size(), withoutCollisions.size()), MEGACHAT_INVALID_HANDLE);

                            for (ShareInfo info : withoutCollisions) {
                                if (info.isContact) {
                                    requestContactsPermissions(info, parentNode);
                                } else {
                                    uploadUseCase.upload(this, info, null, parentNode.getHandle())
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(() -> Timber.d("Upload started"));
                                }
                            }
                        }
                    }
                });
    }

    public void requestContactsPermissions(ShareInfo info, MegaNode parentNode) {
        Timber.d("requestContactsPermissions");
        if (!hasPermissions(this, Manifest.permission.READ_CONTACTS)) {
            Timber.w("No read contacts permission");
            infoManager = info;
            parentNodeManager = parentNode;
            requestPermission(this, REQUEST_UPLOAD_CONTACT, Manifest.permission.READ_CONTACTS);
        } else {
            uploadContactInfo(info, parentNode);
        }
    }

    public void uploadContactInfo(ShareInfo info, MegaNode parentNode) {
        Timber.d("Upload contact info");

        Cursor cursorID = getContentResolver().query(info.contactUri, null, null, null, null);

        if (cursorID != null) {
            if (cursorID.moveToFirst()) {
                Timber.d("It is a contact");

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
        if (file == null) {
            showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), MEGACHAT_INVALID_HANDLE);
            return;
        }

        checkNameCollisionUseCase.check(file.getName(), parentNode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(handle -> {
                            ArrayList<NameCollision> list = new ArrayList<>();
                            list.add(NameCollision.Upload.getUploadCollision(handle,
                                    file, parentNode.getHandle()));
                            nameCollisionActivityContract.launch(list);
                        },
                        throwable -> {
                            if (throwable instanceof MegaNodeException.ParentDoesNotExistException) {
                                showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.general_error), MEGACHAT_INVALID_HANDLE);
                            } else if (throwable instanceof MegaNodeException.ChildDoesNotExistsException) {
                                String text = StringResourcesUtils.getQuantityString(R.plurals.upload_began, 1, 1);

                                uploadUseCase.upload(this, file, parentNode.getHandle())
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(() -> showSnackbar(SNACKBAR_TYPE, text, MEGACHAT_INVALID_HANDLE));
                            }
                        });
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
        Timber.d("onRequestStart(CHAT): %s", request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        Timber.d("onRequestFinish(CHAT): %s_%d", request.getRequestString(), e.getErrorCode());

        if (request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM) {
            Timber.d("Create chat request finish");
            onRequestFinishCreateChat(e.getErrorCode(), request.getChatHandle());
        } else if (request.getType() == MegaChatRequest.TYPE_DISCONNECT) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                Timber.d("DISConnected from chat!");
            } else {
                Timber.e("ERROR WHEN DISCONNECTING %s", e.getErrorString());
            }
        } else if (request.getType() == MegaChatRequest.TYPE_LOGOUT) {
            Timber.e("onRequestFinish(CHAT): %d", MegaChatRequest.TYPE_LOGOUT);

            if (e.getErrorCode() != MegaError.API_OK) {
                Timber.e("MegaChatRequest.TYPE_LOGOUT:ERROR");
            }

            if (app != null) {
                app.disableMegaChatApi();
            }
            loggingSettings.resetLoggerSDK();
        } else if (request.getType() == MegaChatRequest.TYPE_SET_ONLINE_STATUS) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                Timber.d("Status changed to: %s", request.getNumber());
            } else if (e.getErrorCode() == MegaChatError.ERROR_ARGS) {
                Timber.w("Status not changed, the chosen one is the same");
            } else {
                Timber.e("ERROR WHEN TYPE_SET_ONLINE_STATUS %s", e.getErrorString());
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
                    Timber.d("Chat archived");
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.success_archive_chat, chatTitle), -1);
                } else {
                    Timber.d("Chat unarchived");
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.success_unarchive_chat, chatTitle), -1);
                }
            } else {
                if (request.getFlag()) {
                    Timber.e("ERROR WHEN ARCHIVING CHAT %s", e.getErrorString());
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_archive_chat, chatTitle), -1);
                } else {
                    Timber.e("ERROR WHEN UNARCHIVING CHAT %s", e.getErrorString());
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_unarchive_chat, chatTitle), -1);
                }
            }
        } else if (request.getType() == MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                Timber.d("MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE: %s", request.getFlag());
            } else {
                Timber.e("MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE:error: %d", e.getErrorType());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    public void onRequestFinishCreateChat(int errorCode, long chatHandle) {
        if (errorCode == MegaChatError.ERROR_OK) {
            Timber.d("Chat CREATED.");

            //Update chat view
            Timber.d("Open new chat: %s", chatHandle);
            Intent intent = new Intent(this, ChatActivity.class);
            intent.setAction(ACTION_CHAT_SHOW_MESSAGES);
            intent.putExtra(CHAT_ID, chatHandle);
            this.startActivity(intent);
        } else {
            Timber.e("ERROR WHEN CREATING CHAT %d", errorCode);
            showSnackbar(SNACKBAR_TYPE, getString(R.string.create_chat_error), -1);
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        Timber.d("onRequestStart: %s", request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        Timber.d("onRequestUpdate: %s", request.getRequestString());
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        Timber.d("onRequestFinish: %s_%d", request.getRequestString(), e.getErrorCode());
        if (request.getType() == MegaRequest.TYPE_LOGOUT) {
            Timber.d("onRequestFinish: %s", MegaRequest.TYPE_LOGOUT);

            if (e.getErrorCode() == MegaError.API_OK) {
                Timber.d("onRequestFinish:OK:%s", MegaRequest.TYPE_LOGOUT);
                Timber.d("END logout sdk request - wait chat logout");
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
                Timber.d("MK exported - USER_ATTR_PWD_REMINDER finished");
                if (e.getErrorCode() == MegaError.API_OK || e.getErrorCode() == MegaError.API_ENOENT) {
                    Timber.d("New value of attribute USER_ATTR_PWD_REMINDER: %s", request.getText());
                }
            }
        } else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER) {
            if (request.getParamType() == MegaApiJava.USER_ATTR_AVATAR) {
                Timber.d("Request avatar");
                if (e.getErrorCode() == MegaError.API_OK) {
                    setProfileAvatar();
                } else if (e.getErrorCode() == MegaError.API_ENOENT) {
                    setDefaultAvatar();
                } else if (e.getErrorCode() == MegaError.API_EARGS) {
                    Timber.e("Error changing avatar: ");
                }

                LiveEventBus.get(EVENT_AVATAR_CHANGE, Boolean.class).post(false);
            } else if (request.getParamType() == MegaApiJava.USER_ATTR_FIRSTNAME) {
                updateMyData(true, request.getText(), e);
            } else if (request.getParamType() == MegaApiJava.USER_ATTR_LASTNAME) {
                updateMyData(false, request.getText(), e);
            } else if (request.getParamType() == MegaApiJava.USER_ATTR_GEOLOCATION) {

                if (e.getErrorCode() == MegaError.API_OK) {
                    Timber.d("Attribute USER_ATTR_GEOLOCATION enabled");
                    MegaApplication.setEnabledGeoLocation(true);
                } else {
                    Timber.d("Attribute USER_ATTR_GEOLOCATION disabled");
                    MegaApplication.setEnabledGeoLocation(false);
                }
            } else if (request.getParamType() == MegaApiJava.USER_ATTR_DISABLE_VERSIONS) {
                MegaApplication.setDisableFileVersions(request.getFlag());
            } else if (request.getParamType() == USER_ATTR_MY_BACKUPS_FOLDER) {
                if (e.getErrorCode() == MegaError.API_OK) {
                    Timber.d("requesting myBackupHandle");
                    MegaNodeUtil.myBackupHandle = request.getNodeHandle();
                }
            }
        } else if (request.getType() == MegaRequest.TYPE_GET_CANCEL_LINK) {
            Timber.d("TYPE_GET_CANCEL_LINK");
            hideKeyboard(managerActivity, 0);

            if (e.getErrorCode() == MegaError.API_OK) {
                Timber.d("Cancelation link received!");
                showAlert(this, getString(R.string.email_verification_text), getString(R.string.email_verification_title));
            } else {
                Timber.e("Error when asking for the cancellation link: %s___%s", e.getErrorCode(), e.getErrorString());
                showAlert(this, getString(R.string.general_text_error), getString(R.string.general_error_word));
            }
        } else if (request.getType() == MegaRequest.TYPE_REMOVE_CONTACT) {

            if (e.getErrorCode() == MegaError.API_OK) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_removed), -1);
            } else if (e.getErrorCode() == MegaError.API_EMASTERONLY) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_remove_business_contact, request.getEmail()), -1);
            } else {
                Timber.e("Error deleting contact");
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_not_removed), -1);
            }
        } else if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT) {
            Timber.d("MegaRequest.TYPE_INVITE_CONTACT finished: %s", request.getNumber());

            dismissAlertDialogIfExists(statusDialog);

            if (request.getNumber() == MegaContactRequest.INVITE_ACTION_REMIND) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_invitation_resent), -1);
            } else {
                if (e.getErrorCode() == MegaError.API_OK) {
                    Timber.d("OK INVITE CONTACT: %s", request.getEmail());
                    if (request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_request_sent, request.getEmail()), -1);
                    } else if (request.getNumber() == MegaContactRequest.INVITE_ACTION_DELETE) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_invitation_deleted), -1);
                    }
                } else {
                    Timber.e("ERROR invite contact: %s___%s", e.getErrorCode(), e.getErrorString());
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
            Timber.d("MegaRequest.TYPE_PAUSE_TRANSFERS");
            //force update the pause notification to prevent missed onTransferUpdate
            sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION));

            if (e.getErrorCode() == MegaError.API_OK) {
                updateTransfersWidgetState();

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
            Timber.d("One MegaRequest.TYPE_PAUSE_TRANSFER");

            if (e.getErrorCode() == MegaError.API_OK) {
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
                transfersManagement.removePausedTransfers(request.getTransferTag());
                updateTransfersWidget();
                supportInvalidateOptionsMenu();
            } else {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_general_nodes), MEGACHAT_INVALID_HANDLE);
            }
        } else if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFERS) {
            Timber.d("MegaRequest.TYPE_CANCEL_TRANSFERS");
            //After cancelling all the transfers
            if (e.getErrorCode() == MegaError.API_OK) {
                hideTransfersWidget();

                if (drawerItem == DrawerItem.TRANSFERS && isTransfersInProgressAdded()) {
                    pauseTransfersMenuIcon.setVisible(false);
                    playTransfersMenuIcon.setVisible(false);
                    cancelAllTransfersMenuItem.setVisible(false);
                }

                transfersManagement.resetPausedTransfers();
            } else {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_general_nodes), -1);
            }
        } else if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER) {
            dismissAlertDialogIfExists(statusDialog);

            if (e.getErrorCode() == MegaError.API_OK) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_created), -1);

                MegaNode folderNode = megaApi.getNodeByHandle(request.getNodeHandle());
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
                Timber.e("TYPE_CREATE_FOLDER ERROR: %s___%s", e.getErrorCode(), e.getErrorString());
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_no_created), -1);
            }
        } else if (request.getType() == MegaRequest.TYPE_SUBMIT_PURCHASE_RECEIPT) {
            if (e.getErrorCode() == MegaError.API_OK) {
                Timber.d("PURCHASE CORRECT!");
                drawerItem = DrawerItem.CLOUD_DRIVE;
                selectDrawerItem(drawerItem);
            } else {
                Timber.e("PURCHASE WRONG: %s (%d)", e.getErrorString(), e.getErrorCode());
            }
        } else if (request.getType() == MegaRequest.TYPE_REGISTER_PUSH_NOTIFICATION) {
            if (e.getErrorCode() == MegaError.API_OK) {
                Timber.d("FCM OK TOKEN MegaRequest.TYPE_REGISTER_PUSH_NOTIFICATION");
            } else {
                Timber.e("FCM ERROR TOKEN TYPE_REGISTER_PUSH_NOTIFICATION: %d__%s", e.getErrorCode(), e.getErrorString());
            }
        } else if (request.getType() == MegaRequest.TYPE_FOLDER_INFO) {
            if (e.getErrorCode() == MegaError.API_OK) {
                MegaFolderInfo info = request.getMegaFolderInfo();
                int numVersions = info.getNumVersions();
                Timber.d("Num versions: %s", numVersions);
                long previousVersions = info.getVersionsSize();
                Timber.d("Previous versions: %s", previousVersions);

                myAccountInfo.setNumVersions(numVersions);
                myAccountInfo.setPreviousVersionsSize(previousVersions);

            } else {
                Timber.e("ERROR requesting version info of the account");
            }
        } else if (request.getType() == MegaRequest.TYPE_REMOVE) {
            if (versionsToRemove > 0) {
                Timber.d("Remove request finished");
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
                Timber.d("Remove request finished");
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
        Timber.d("updateAccountStorageInfo");
        megaApi.getFolderInfo(megaApi.getRootNode(), this);
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
                                        MegaError e) {
        Timber.w("onRequestTemporaryError: %s__%d__%s", request.getRequestString(), e.getErrorCode(), e.getErrorString());
    }

    public void updateUsers(List<MegaUser> users) {
        if (users != null) {
            Timber.d("users.size(): %s", users.size());
            for (int i = 0; i < users.size(); i++) {
                MegaUser user = users.get(i);

                if (user != null) {
                    // 0 if the change is external.
                    // >0 if the change is the result of an explicit request
                    // -1 if the change is the result of an implicit request made by the SDK internally

                    if (user.isOwnChange() > 0) {
                        Timber.d("isOwnChange!!!: %s", user.getEmail());
                        if (user.hasChanged(MegaUser.CHANGE_TYPE_RICH_PREVIEWS)) {
                            Timber.d("Change on CHANGE_TYPE_RICH_PREVIEWS");
                            megaApi.shouldShowRichLinkWarning(this);
                            megaApi.isRichPreviewsEnabled(this);
                        }
                    } else {
                        Timber.d("NOT OWN change");

                        Timber.d("Changes: %s", user.getChanges());

                        if (user.hasChanged(MegaUser.CHANGE_TYPE_FIRSTNAME)) {
                            if (user.getEmail().equals(megaApi.getMyUser().getEmail())) {
                                Timber.d("I change my first name");
                                megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_FIRSTNAME, this);
                            } else {
                                Timber.d("The user: %dchanged his first name", user.getHandle());
                                megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_FIRSTNAME, new GetAttrUserListener(this));
                            }
                        }

                        if (user.hasChanged(MegaUser.CHANGE_TYPE_LASTNAME)) {
                            if (user.getEmail().equals(megaApi.getMyUser().getEmail())) {
                                Timber.d("I change my last name");
                                megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_LASTNAME, this);
                            } else {
                                Timber.d("The user: %dchanged his last name", user.getHandle());
                                megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_LASTNAME, new GetAttrUserListener(this));
                            }
                        }

                        if (user.hasChanged(MegaUser.CHANGE_TYPE_ALIAS)) {
                            Timber.d("I changed the user: %d nickname", user.getHandle());
                            megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_ALIAS, new GetAttrUserListener(this));
                        }

                        if (user.hasChanged(MegaUser.CHANGE_TYPE_AVATAR)) {
                            Timber.d("The user: %dchanged his AVATAR", user.getHandle());

                            File avatar = CacheFolderManager.buildAvatarFile(this, user.getEmail() + ".jpg");
                            Bitmap bitmap = null;
                            if (isFileAvailable(avatar)) {
                                avatar.delete();
                            }

                            if (user.getEmail().equals(megaApi.getMyUser().getEmail())) {
                                Timber.d("I change my avatar");
                                String destinationPath = CacheFolderManager.buildAvatarFile(this, megaApi.getMyEmail() + ".jpg").getAbsolutePath();
                                megaApi.getUserAvatar(megaApi.getMyUser(), destinationPath, this);
                            }
                        }

                        if (user.hasChanged(MegaUser.CHANGE_TYPE_EMAIL)) {
                            Timber.d("CHANGE_TYPE_EMAIL");
                            if (user.getEmail().equals(megaApi.getMyUser().getEmail())) {
                                Timber.d("I change my mail");
                                updateMyEmail(user.getEmail());
                            } else {
                                Timber.d("The contact: %d changes the mail.", user.getHandle());
                                if (dbH.findContactByHandle(String.valueOf(user.getHandle())) == null) {
                                    Timber.w("The contact NOT exists -> DB inconsistency! -> Clear!");
                                    if (dbH.getContactsSize() != megaApi.getContacts().size()) {
                                        dbH.clearContacts();
                                        FillDBContactsTask fillDBContactsTask = new FillDBContactsTask(this);
                                        fillDBContactsTask.execute();
                                    }
                                } else {
                                    Timber.d("The contact already exists -> update");
                                    dbH.setContactMail(user.getHandle(), user.getEmail());
                                }
                            }
                        }
                    }
                } else {
                    Timber.w("user == null --> Continue...");
                    continue;
                }
            }
        }
    }

    /**
     * Open location based on where parent node is located
     *
     * @param nodeHandle          parent node handle
     * @param childNodeHandleList list of child nodes handles if comes from notfication about new added nodes to shared folder
     */
    public void openLocation(long nodeHandle, long[] childNodeHandleList) {
        Timber.d("Node handle: %s", nodeHandle);

        MegaNode node = megaApi.getNodeByHandle(nodeHandle);
        if (node == null) {
            return;
        }
        comesFromNotifications = true;
        comesFromNotificationHandle = nodeHandle;
        comesFromNotificationChildNodeHandleList = childNodeHandleList;
        MegaNode parent = nC.getParent(node);
        if (parent.getHandle() == megaApi.getRootNode().getHandle()) {
            //Cloud Drive
            drawerItem = DrawerItem.CLOUD_DRIVE;
            openFolderRefresh = true;
            comesFromNotificationHandleSaved = viewModel.getState().getValue().getBrowserParentHandle();
            viewModel.setBrowserParentHandle(nodeHandle);
            selectDrawerItem(drawerItem);
        } else if (parent.getHandle() == megaApi.getRubbishNode().getHandle()) {
            //Rubbish
            drawerItem = DrawerItem.RUBBISH_BIN;
            openFolderRefresh = true;
            comesFromNotificationHandleSaved = viewModel.getState().getValue().getRubbishBinParentHandle();
            viewModel.setRubbishBinParentHandle(nodeHandle);
            selectDrawerItem(drawerItem);
        } else if (parent.getHandle() == megaApi.getInboxNode().getHandle()) {
            //Inbox
            drawerItem = DrawerItem.INBOX;
            openFolderRefresh = true;
            comesFromNotificationHandleSaved = viewModel.getState().getValue().getInboxParentHandle();
            viewModel.setInboxParentHandle(nodeHandle);
            selectDrawerItem(drawerItem);
        } else {
            //Incoming Shares
            drawerItem = DrawerItem.SHARED_ITEMS;
            comesFromNotificationSharedIndex = SharesTab.Companion.fromPosition(viewPagerShares.getCurrentItem());
            viewModel.setSharesTab(SharesTab.INCOMING_TAB);
            comesFromNotificationDeepBrowserTreeIncoming = viewModel.getState().getValue().getIncomingTreeDepth();
            comesFromNotificationHandleSaved = viewModel.getState().getValue().getIncomingParentHandle();
            if (parent != null) {
                int depth = calculateDeepBrowserTreeIncoming(node, this);
                viewModel.setIncomingTreeDepth(depth);
                comesFromNotificationsLevel = depth;
            }
            openFolderRefresh = true;
            viewModel.setIncomingParentHandle(nodeHandle);
            selectDrawerItem(drawerItem);
        }
    }

    public void updateUserAlerts(List<MegaUserAlert> userAlerts) {
        viewModel.checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NOTIFICATIONS_TITLE_AND_TOOLBAR_ICON);
        notificationsFragment = (NotificationsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.NOTIFICATIONS.getTag());
        if (notificationsFragment != null && userAlerts != null) {
            notificationsFragment.updateNotifications(userAlerts);
        }
    }

    public void updateMyEmail(String email) {
        LiveEventBus.get(EVENT_USER_EMAIL_UPDATED, Boolean.class).post(true);

        Timber.d("New email: %s", email);
        nVEmail.setText(email);
        String oldEmail = dbH.getMyEmail();
        if (oldEmail != null) {
            Timber.d("Old email: %s", oldEmail);
            try {
                File avatarFile = CacheFolderManager.buildAvatarFile(this, oldEmail + ".jpg");
                if (isFileAvailable(avatarFile)) {
                    File newFile = CacheFolderManager.buildAvatarFile(this, email + ".jpg");
                    if (newFile != null) {
                        boolean result = avatarFile.renameTo(newFile);
                        if (result) {
                            Timber.d("The avatar file was correctly renamed");
                        }
                    }
                }
            } catch (Exception e) {
                Timber.e(e, "EXCEPTION renaming the avatar on changing email");
            }
        } else {
            Timber.e("ERROR: Old email is NULL");
        }

        dbH.saveMyEmail(email);
    }

    public void onNodesCloudDriveUpdate() {
        Timber.d("onNodesCloudDriveUpdate");

        rubbishBinFragment = (RubbishBinFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
        if (rubbishBinFragment != null) {
            rubbishBinFragment.hideMultipleSelect();

            refreshRubbishBin();
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
            searchViewModel.setTextSubmitted(true);
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
        Timber.d("onNodesSharedUpdate");

        refreshOutgoingShares();
        refreshIncomingShares();
        refreshLinks();

        refreshSharesPageAdapter();
    }

    public void updateNodes(@NonNull List<MegaNode> updatedNodes) {
        dismissAlertDialogIfExists(statusDialog);

        boolean updateContacts = false;

        //Verify is it is a new item to the inbox
        for (int i = 0; i < updatedNodes.size(); i++) {
            MegaNode updatedNode = updatedNodes.get(i);

            if (!updateContacts) {
                if (updatedNode.isInShare()) {
                    updateContacts = true;

                    if (drawerItem == DrawerItem.SHARED_ITEMS
                            && getTabItemShares() == SharesTab.INCOMING_TAB && viewModel.getState().getValue().getIncomingParentHandle() == updatedNode.getHandle()) {
                        getNodeUseCase.get(viewModel.getState().getValue().getIncomingParentHandle())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe((result, throwable) -> {
                                    if (throwable != null) {
                                        viewModel.decreaseIncomingTreeDepth();
                                        viewModel.setIncomingParentHandle(INVALID_HANDLE);
                                        hideTabs(false, SharesTab.INCOMING_TAB);
                                        refreshIncomingShares();
                                    }
                                });
                    }
                }
            }

            if (updatedNode.getParentHandle() == inboxNode.getHandle()) {
                Timber.d("New element to Inbox!!");
                setInboxNavigationDrawer();
            }
        }

        onNodesSharedUpdate();

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

    public void updateContactRequests(List<ContactRequest> requests) {
        Timber.d("onContactRequestsUpdate");

        if (requests != null) {
            for (int i = 0; i < requests.size(); i++) {
                ContactRequest req = requests.get(i);
                if (req.isOutgoing()) {
                    Timber.d("SENT REQUEST");
                    Timber.d("STATUS: %s, Contact Handle: %d", req.getStatus(), req.getHandle());
                    if (req.getStatus() == ContactRequestStatus.Accepted) {
                        cC.addContactDB(req.getTargetEmail());
                    }
                } else {
                    Timber.d("RECEIVED REQUEST");
                    setContactTitleSection();
                    Timber.d("STATUS: %s Contact Handle: %d", req.getStatus(), req.getHandle());
                    if (req.getStatus() == ContactRequestStatus.Accepted) {
                        cC.addContactDB(req.getSourceEmail());
                    }
                }
            }
        }

        viewModel.checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NAVIGATION_TOOLBAR_ICON);
    }

    /**
     * Pauses a transfer.
     *
     * @param mT the transfer to pause
     */
    public void pauseIndividualTransfer(MegaTransfer mT) {
        if (mT == null) {
            Timber.w("Transfer object is null.");
            return;
        }

        Timber.d("Resume transfer - Node handle: %s", mT.getNodeHandle());
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
        Timber.d("onTransferStart: %d-%d - %d", transfer.getNotificationNumber(), transfer.getNodeHandle(), transfer.getTag());

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
        Timber.d("onTransferFinish: %d - %d- %d", transfer.getNodeHandle(), transfer.getTag(), transfer.getNotificationNumber());
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
                    Timber.d("The transfer with index %d has been removed, left: %d", index, transfersInProgress.size());
                } else {
                    Timber.d("The transferInProgress is EMPTY");
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
            Timber.d("Update onTransferUpdate: %d - %d - %d", transfer.getNodeHandle(), transfer.getTag(), transfer.getNotificationNumber());
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
        Timber.w("onTransferTemporaryError: %d - %d", transfer.getNodeHandle(), transfer.getTag());

        if (e.getErrorCode() == MegaError.API_EOVERQUOTA) {
            if (e.getValue() != 0) {
                Timber.d("TRANSFER OVERQUOTA ERROR: %s", e.getErrorCode());
                updateTransfersWidget();
            } else {
                Timber.w("STORAGE OVERQUOTA ERROR: %d", e.getErrorCode());
                //work around - SDK does not return over quota error for folder upload,
                //so need to be notified from global listener
                if (transfer.getType() == MegaTransfer.TYPE_UPLOAD) {
                    if (transfer.isForeignOverquota()) {
                        return;
                    }

                    Timber.d("Over quota");
                    Intent intent = new Intent(this, UploadService.class);
                    intent.setAction(ACTION_OVERQUOTA_STORAGE);
                    ContextCompat.startForegroundService(this, intent);
                }
            }
        }
    }

    @Override
    public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer) {
        Timber.d("onTransferData");
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
        Timber.d("setPathNavigationOffline: %s", pathNavigationOffline);
        this.pathNavigationOffline = pathNavigationOffline;
    }

    public int getDeepBrowserTreeIncoming() {
        return viewModel.getState().getValue().getIncomingTreeDepth();
    }

    public void setDeepBrowserTreeIncoming(int deep) {
        viewModel.setIncomingTreeDepth(deep);
    }

    public int getDeepBrowserTreeOutgoing() {
        return viewModel.getState().getValue().getOutgoingTreeDepth();
    }

    public int getDeepBrowserTreeLinks() {
        return viewModel.getState().getValue().getLinksTreeDepth();
    }

    public DrawerItem getDrawerItem() {
        return drawerItem;
    }

    public void setDrawerItem(DrawerItem drawerItem) {
        this.drawerItem = drawerItem;
    }

    public SharesTab getTabItemShares() {
        return viewPagerShares == null ? SharesTab.NONE :
                SharesTab.Companion.fromPosition(viewPagerShares.getCurrentItem());
    }

    private TransfersTab getTabItemTransfers() {
        return viewPagerTransfers == null ? TransfersTab.NONE :
                TransfersTab.Companion.fromPosition(viewPagerTransfers.getCurrentItem());
    }

    public void setTabItemShares(int index) {
        viewPagerShares.setCurrentItem(index);
    }

    public void showChatPanel(MegaChatListItem chat) {
        Timber.d("showChatPanel");

        if (chat == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedChatItemId = chat.getChatId();
        bottomSheetDialogFragment = new ChatBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void updateUserNameNavigationView(String fullName) {
        Timber.d("updateUserNameNavigationView");

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

                        MegaNode parentNodeInSF = megaApi.getNodeByHandle(viewModel.getState().getValue().getIncomingParentHandle());
                        if (viewModel.getState().getValue().getIncomingTreeDepth() <= 0 || parentNodeInSF == null) {
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

                        if (viewModel.getState().getValue().getOutgoingTreeDepth() <= 0) {
                            hideFabButton();
                        } else {
                            updateFabAndShow();
                        }
                        break;

                    case LINKS_TAB:
                        if (!isLinksAdded()) break;

                        if (viewModel.getState().getValue().getLinksTreeDepth() <= 0) {
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
            Timber.d("Chat ID:%s", item.getChatId());
            if (item.isPreview()) {
                return;
            }
        } else {
            Timber.w("Item NULL");
            return;
        }

        recentChatsFragment = (RecentChatsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
        if (recentChatsFragment != null) {
            recentChatsFragment.listItemUpdate(item);
        }

        if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT)) {
            Timber.d("Change unread count: %s", item.getUnreadCount());
            setChatBadge();
            viewModel.checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NAVIGATION_TOOLBAR_ICON);
        }
    }

    private void onChatOnlineStatusUpdate(long userHandle, int status, boolean inProgress) {
        Timber.d("Status: %d, In Progress: %s", status, inProgress);
        if (inProgress) {
            status = -1;
        }

        if (megaChatApi != null) {
            recentChatsFragment = (RecentChatsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
            if (userHandle == megaChatApi.getMyUserHandle()) {
                Timber.d("My own status update");
                setContactStatus();
                if (drawerItem == DrawerItem.CHAT) {
                    if (recentChatsFragment != null) {
                        recentChatsFragment.onlineStatusUpdate(status);
                    }
                }
            } else {
                Timber.d("Status update for the user: %s", userHandle);
                recentChatsFragment = (RecentChatsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
                if (recentChatsFragment != null) {
                    Timber.d("Update Recent chats view");
                    recentChatsFragment.contactStatusUpdate(userHandle, status);
                }
            }
        }
    }

    private void onChatConnectionStateUpdate(long chatid, int newState) {
        Timber.d("Chat ID: %d, New state: %d", chatid, newState);
        if (newState == MegaChatApi.CHAT_CONNECTION_ONLINE && chatid == -1) {
            Timber.d("Online Connection: %s", chatid);
            recentChatsFragment = (RecentChatsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
            if (recentChatsFragment != null) {
                recentChatsFragment.setChats();
                if (drawerItem == DrawerItem.CHAT) {
                    recentChatsFragment.setStatus();
                }
            }
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

        if (mElevationCause > 0) {
            if (onlySetToolbar) {
                toolbar.setBackgroundColor(toolbarElevationColor);
            } else {
                toolbar.setBackgroundColor(transparentColor);
                abL.setElevation(elevation);
            }
        } else {
            toolbar.setBackgroundColor(transparentColor);
            abL.setElevation(0);
        }

        ColorUtils.changeStatusBarColorForElevation(this,
                mElevationCause > 0 && !isInMainHomePage());
    }

    public long getParentHandleInbox() {
        return viewModel.getState().getValue().getInboxParentHandle();
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
            Timber.e(e, "Formatted string: %s", textToShow);
        }

        Spanned result = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);
        }
        contactsSectionText.setText(result);
    }

    public void setNotificationsTitleSection(int unread) {
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
            Timber.e(e, "Formatted string: %s", textToShow);
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
        Timber.d("refreshMenu");
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
        Timber.d("fromAndroidNotification: %s", fromAndroidNotification);

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
        if (searchMenuItem != null) {
            searchMenuItem.expandActionView();
            if (searchView != null) {
                searchView.setQuery(searchViewModel.getState().getValue().getSearchQuery(), false);
            }
        }
    }

    public void clearSearchViewFocus() {
        if (searchView != null) {
            searchView.clearFocus();
        }
    }

    public void requestSearchViewFocus() {
        if (searchView == null || searchViewModel.getState().getValue().getTextSubmitted()) {
            return;
        }

        searchView.setIconified(false);
    }

    public void openSearchFolder(MegaNode node) {
        switch (drawerItem) {
            case HOMEPAGE:
                // Redirect to Cloud drive.
                selectDrawerItem(DrawerItem.CLOUD_DRIVE);
            case CLOUD_DRIVE:
                viewModel.setBrowserParentHandle(node.getHandle());
                refreshFragment(FragmentTag.CLOUD_DRIVE.getTag());
                break;
            case SHARED_ITEMS:
                if (viewPagerShares == null || sharesPageAdapter == null) break;

                if (getTabItemShares() == SharesTab.INCOMING_TAB) {
                    viewModel.setIncomingParentHandle(node.getHandle());
                    viewModel.increaseIncomingTreeDepth();
                } else if (getTabItemShares() == SharesTab.OUTGOING_TAB) {
                    viewModel.setOutgoingParentHandle(node.getHandle());
                    viewModel.increaseOutgoingTreeDepth();
                } else if (getTabItemShares() == SharesTab.LINKS_TAB) {
                    viewModel.setLinksParentHandle(node.getHandle());
                    viewModel.increaseLinksTreeDepth();
                }
                refreshSharesPageAdapter();

                break;
            case INBOX:
                viewModel.setInboxParentHandle(node.getHandle());
                refreshFragment(FragmentTag.INBOX.getTag());
                break;
        }
    }

    public void closeSearchView() {
        searchViewModel.setTextSubmitted(true);
        if (searchMenuItem != null && searchMenuItem.isActionViewExpanded()) {
            searchMenuItem.collapseActionView();
        }
    }

    public void setTextSubmitted() {
        if (searchView != null) {
            if (!searchViewModel.isSearchQueryValid()) return;
            searchView.setQuery(searchViewModel.getState().getValue().getSearchQuery(), true);
        }
    }

    public boolean isSearchOpen() {
        return searchViewModel.getState().getValue().getSearchQuery() != null && searchExpand;
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
        Timber.d("Level: %s", level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            Timber.w("Low memory");
            ThumbnailUtils.isDeviceMemoryLow = true;
        } else {
            Timber.d("Memory OK");
            ThumbnailUtils.isDeviceMemoryLow = false;
        }
    }

    private void setSearchDrawerItem() {
        if (drawerItem == DrawerItem.SEARCH) return;

        searchViewModel.setSearchDrawerItem(drawerItem);
        searchViewModel.setSearchSharedTab(getTabItemShares());

        drawerItem = DrawerItem.SEARCH;
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

    public void setSearchQuery(String searchQuery) {
        searchViewModel.setSearchQuery(searchQuery);
        if (this.searchView != null) {
            this.searchView.setQuery(searchQuery, false);
        }
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
                Timber.w("Node is null, not able to retry");
                return;
            }

            if (transfer.getIsOfflineFile()) {
                File offlineFile = new File(transfer.getOriginalPath());
                saveOffline(offlineFile.getParentFile(), node, ManagerActivity.this);
            } else {
                downloadNodeUseCase.download(this, node, transfer.getPath())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> Timber.d("Transfer retried: %s", node.getHandle()),
                                throwable -> Timber.e(throwable, "Retry transfer failed."));
            }
        } else if (transfer.getType() == MegaTransfer.TYPE_UPLOAD) {
            File file = new File(transfer.getOriginalPath());
            uploadUseCase.upload(this, file, transfer.getParentHandle())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> Timber.d("Transfer retried."));
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
            viewModel.setBrowserParentHandle(node.getParentHandle());
            refreshFragment(FragmentTag.CLOUD_DRIVE.getTag());
            selectDrawerItem(DrawerItem.CLOUD_DRIVE);
        } else if (parentNode.getHandle() == megaApi.getRubbishNode().getHandle()) {
            viewModel.setRubbishBinParentHandle(node.getParentHandle());
            refreshFragment(FragmentTag.RUBBISH_BIN.getTag());
            selectDrawerItem(DrawerItem.RUBBISH_BIN);
        } else if (parentNode.isInShare()) {
            viewModel.setIncomingParentHandle(node.getParentHandle());
            viewModel.setIncomingTreeDepth(calculateDeepBrowserTreeIncoming(megaApi.getParentNode(node), this));
            sharesPageAdapter.refreshFragment(SharesTab.INCOMING_TAB.getPosition());
            viewModel.setSharesTab(SharesTab.INCOMING_TAB);
            if (viewPagerShares != null) {
                viewPagerShares.setCurrentItem(viewModel.getState().getValue().getSharesTab().getPosition());
                refreshSharesPageAdapter();
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
        transfersManagement.setOnTransfersSection(drawerItem == DrawerItem.TRANSFERS);

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
            Timber.d("It's a meeting");
            boolean linkInvalid = TextUtil.isTextEmpty(link) && chatId == MEGACHAT_INVALID_HANDLE;
            if (linkInvalid) {
                Timber.e("Invalid link");
                return;
            }

            if (isMeetingEnded(request.getMegaHandleList())) {
                Timber.d("It's a meeting, open dialog: Meeting has ended");
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
            Timber.d("It's a chat");
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
                Timber.d("Nothing to do for actionType = %s", actionType);
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
                }, Timber::e);

        composite.add(chatSubscription);
    }

    /**
     * Create list of positions of each new node which was added to share folder
     *
     * @param nodes Share folder nodes' list
     * @return positions list
     */
    public ArrayList<Integer> getPositionsList(List<MegaNode> nodes) {
        ArrayList<Integer> positions = new ArrayList<>();
        if (comesFromNotificationChildNodeHandleList != null) {
            long[] childNodeHandleList = comesFromNotificationChildNodeHandleList;
            for (long childNodeHandle : childNodeHandleList) {
                for (int i = 1; i < nodes.size(); i++) {
                    var shareNode = nodes.get(i);
                    if (shareNode != null && shareNode.getHandle() == childNodeHandle) {
                        positions.add(i);
                    }
                }
            }
        }
        return positions;
    }

    /**
     * Updates the UI related to unread user alerts as per the [UnreadUserAlertsCheckType] received.
     *
     * @param result Pair containing the type of the request and the number of unread user alerts.
     */
    private void updateNumUnreadUserAlerts(kotlin.Pair<UnreadUserAlertsCheckType, Integer> result) {
        UnreadUserAlertsCheckType type = result.getFirst();
        int numUnreadUserAlerts = result.getSecond();

        if (type == UnreadUserAlertsCheckType.NAVIGATION_TOOLBAR_ICON) {
            updateNavigationToolbarIcon(numUnreadUserAlerts);
        } else if (type == UnreadUserAlertsCheckType.NOTIFICATIONS_TITLE) {
            setNotificationsTitleSection(numUnreadUserAlerts);
        } else {
            updateNavigationToolbarIcon(numUnreadUserAlerts);
            setNotificationsTitleSection(numUnreadUserAlerts);
        }
    }

    /**
     * Updates the Shares section tab as per the indexShares.
     */
    public void updateSharesTab() {
        if (viewModel.getState().getValue().getSharesTab() == SharesTab.NONE) {
            Timber.d("indexShares is -1");
            return;
        }

        Timber.d("The index of the TAB Shares is: %s", viewModel.getState().getValue().getSharesTab());

        if (viewPagerShares != null) {
            viewPagerShares.setCurrentItem(viewModel.getState().getValue().getSharesTab().getPosition());
        }

        viewModel.setSharesTab(SharesTab.NONE);
    }

    /**
     * Restores the Shares section after opening it from a notification in the Notifications section.
     */
    public void restoreSharesAfterComingFromNotifications() {
        selectDrawerItem(DrawerItem.NOTIFICATIONS);
        comesFromNotifications = false;
        comesFromNotificationsLevel = 0;
        comesFromNotificationHandle = INVALID_VALUE;
        viewModel.setSharesTab(comesFromNotificationSharedIndex);
        updateSharesTab();
        comesFromNotificationSharedIndex = SharesTab.NONE;
        setDeepBrowserTreeIncoming(comesFromNotificationDeepBrowserTreeIncoming);
        comesFromNotificationDeepBrowserTreeIncoming = INVALID_VALUE;
        setParentHandleIncoming(comesFromNotificationHandleSaved);
        comesFromNotificationHandleSaved = INVALID_VALUE;
        refreshIncomingShares();
    }

    /**
     * Updates Inbox section visibility depending on if it has children.
     *
     * @param hasChildren True if the Inbox node has children, false otherwise.
     */
    private void updateInboxSectionVisibility(boolean hasChildren) {
        if (inboxSection == null) {
            return;
        }

        if (hasChildren) {
            inboxSection.setEnabled(true);
            inboxSection.setVisibility(View.VISIBLE);
            ((TextView) inboxSection.findViewById(R.id.inbox_section_text)).setTextColor(
                    ColorUtils.getThemeColor(this, android.R.attr.textColorPrimary));
        } else {
            inboxSection.setVisibility(View.GONE);
        }
    }
}
