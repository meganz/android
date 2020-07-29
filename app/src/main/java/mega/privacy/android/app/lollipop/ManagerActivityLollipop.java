package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader.TileMode;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.ContactsContract;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener;
import com.google.android.material.tabs.TabLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.view.MenuItemCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ActionMode;
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
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mega.privacy.android.app.AndroidCompletedTransfer;
import mega.privacy.android.app.BucketSaved;
import mega.privacy.android.app.BusinessExpiredAlertActivity;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.OpenPasswordLinkActivity;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.SMSVerificationActivity;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.components.CustomViewPager;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.components.EditTextPIN;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiEditText;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder;
import mega.privacy.android.app.fcm.ContactsAdvancedNotificationBuilder;
import mega.privacy.android.app.fragments.managerFragments.LinksFragment;
import mega.privacy.android.app.interfaces.UploadBottomSheetDialogActionListener;
import mega.privacy.android.app.listeners.ExportListener;
import mega.privacy.android.app.listeners.GetAttrUserListener;
import mega.privacy.android.app.lollipop.adapters.CloudPageAdapter;
import mega.privacy.android.app.lollipop.adapters.ContactsPageAdapter;
import mega.privacy.android.app.lollipop.adapters.MyAccountPageAdapter;
import mega.privacy.android.app.lollipop.adapters.SharesPageAdapter;
import mega.privacy.android.app.lollipop.adapters.TransfersPageAdapter;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.listeners.CreateChatListener;
import mega.privacy.android.app.lollipop.listeners.CreateGroupChatWithPublicLink;
import mega.privacy.android.app.lollipop.listeners.FabButtonListener;
import mega.privacy.android.app.lollipop.listeners.MultipleAttachChatListener;
import mega.privacy.android.app.lollipop.managerSections.CameraUploadFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.CentiliFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.CompletedTransfersFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.ContactsFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.CreditCardFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.ExportRecoveryKeyFragment;
import mega.privacy.android.app.lollipop.managerSections.FileBrowserFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.FortumoFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.InboxFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.IncomingSharesFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.MyAccountFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.MyStorageFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.NotificationsFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.OfflineFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.OutgoingSharesFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.ReceivedRequestsFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.RecentsFragment;
import mega.privacy.android.app.lollipop.managerSections.RubbishBinFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.SearchFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.SentRequestsFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.SettingsFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.TransfersFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.TurnOnNotificationsFragment;
import mega.privacy.android.app.lollipop.managerSections.UpgradeAccountFragmentLollipop;
import mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity;
import mega.privacy.android.app.lollipop.megachat.BadgeDrawerArrowDrawable;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.lollipop.megachat.RecentChatsFragmentLollipop;
import mega.privacy.android.app.lollipop.qrcode.QRCodeActivity;
import mega.privacy.android.app.lollipop.qrcode.ScanCodeFragment;
import mega.privacy.android.app.lollipop.tasks.CheckOfflineNodesTask;
import mega.privacy.android.app.lollipop.tasks.FilePrepareTask;
import mega.privacy.android.app.lollipop.tasks.FillDBContactsTask;
import mega.privacy.android.app.modalbottomsheet.ContactsBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.MyAccountBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.OfflineOptionsBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.ReceivedRequestBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.SentRequestBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.TransfersBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.UploadBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ChatBottomSheetDialogFragment;
import mega.privacy.android.app.utils.LastShowSMSDialogTimeChecker;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.billing.BillingManager;
import mega.privacy.android.app.utils.contacts.MegaContactGetter;
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
import nz.mega.sdk.MegaChatListenerInterface;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatPresenceConfig;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaFolderInfo;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferData;
import nz.mega.sdk.MegaTransferListenerInterface;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;
import nz.mega.sdk.MegaUtilsAndroid;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.constants.SettingsConstants.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.PermissionUtils.*;
import static mega.privacy.android.app.utils.billing.PaymentUtils.*;
import static mega.privacy.android.app.lollipop.FileInfoActivityLollipop.NODE_HANDLE;
import static mega.privacy.android.app.lollipop.qrcode.MyCodeFragment.QR_IMAGE_FILE_NAME;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.CameraUploadUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.DBUtil.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.JobUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.ProgressDialogUtil.*;
import static mega.privacy.android.app.utils.UploadUtil.*;
import static mega.privacy.android.app.utils.Util.*;

import static nz.mega.sdk.MegaApiJava.*;

public class ManagerActivityLollipop extends DownloadableActivity implements MegaRequestListenerInterface, MegaChatListenerInterface, MegaChatRequestListenerInterface, OnNavigationItemSelectedListener, MegaGlobalListenerInterface, MegaTransferListenerInterface, OnClickListener, View.OnFocusChangeListener, View.OnLongClickListener, BottomNavigationView.OnNavigationItemSelectedListener, UploadBottomSheetDialogActionListener, BillingManager.BillingUpdatesListener {

	public static final String TRANSFERS_TAB = "TRANSFERS_TAB";
	private static final String SEARCH_SHARED_TAB = "SEARCH_SHARED_TAB";
	private static final String SEARCH_DRAWER_ITEM = "SEARCH_DRAWER_ITEM";
	private static final String OFFLINE_SEARCH_PATHS = "OFFLINE_SEARCH_PATHS";
	public static final String OFFLINE_SEARCH_QUERY = "OFFLINE_SEARCH_QUERY:";
	private static final String MK_LAYOUT_VISIBLE = "MK_LAYOUT_VISIBLE";

    private static final String BUSINESS_GRACE_ALERT_SHOWN = "BUSINESS_GRACE_ALERT_SHOWN";
	private static final String BUSINESS_CU_ALERT_SHOWN = "BUSINESS_CU_ALERT_SHOWN";
	private static final String BUSINESS_CU_FRAGMENT = "BUSINESS_CU_FRAGMENT";
	public static final String BUSINESS_CU_FRAGMENT_SETTINGS = "BUSINESS_CU_FRAGMENT_SETTINGS";
	public static final String BUSINESS_CU_FRAGMENT_CU = "BUSINESS_CU_FRAGMENT_CU";
	private static final String BUSINESS_CU_FIRST_TIME = "BUSINESS_CU_FIRST_TIME";

	private static final String DEEP_BROWSER_TREE_LINKS = "DEEP_BROWSER_TREE_LINKS";
    private static final String PARENT_HANDLE_LINKS = "PARENT_HANDLE_LINKS";
    private static final String DEEP_BROWSER_TREE_RECENTS = "DEEP_BROWSER_TREE_RECENTS";
	private static final String INDEX_CLOUD = "INDEX_CLOUD";
    public static final String NEW_CREATION_ACCOUNT = "NEW_CREATION_ACCOUNT";

	public static final int ERROR_TAB = -1;
	public static final int CLOUD_TAB = 0;
	public static final int RECENTS_TAB = 1;
	public static final int INCOMING_TAB = 0;
	public static final int OUTGOING_TAB = 1;
	public static final int LINKS_TAB = 2;
	public static final int CONTACTS_TAB = 0;
	public static final int SENT_REQUESTS_TAB = 1;
	public static final int RECEIVED_REQUESTS_TAB = 2;
	public static final int GENERAL_TAB = 0;
	public static final int STORAGE_TAB = 1;
	public static final int PENDING_TAB = 0;
	public static final int COMPLETED_TAB = 1;

	private static final int CLOUD_DRIVE_BNV = 0;
	private static final int CAMERA_UPLOADS_BNV = 1;
	private static final int CHAT_BNV = 2;
	private static final int SHARED_BNV = 3;
	private static final int OFFLINE_BNV = 4;
	private static final int HIDDEN_BNV = 5;
	private static final int MEDIA_UPLOADS_BNV = 6;

	private LastShowSMSDialogTimeChecker smsDialogTimeChecker;

	public int accountFragment;

	private long handleInviteContact = -1;

	public ArrayList<Integer> transfersInProgress;
	public MegaTransferData transferData;

	public long transferCallback = 0;

	String regex = "[*|\\?:\"<>\\\\\\\\/]";

	TransfersBottomSheetDialogFragment transfersBottomSheet = null;

	//GET PRO ACCOUNT PANEL
	LinearLayout getProLayout=null;
	TextView getProText;
	TextView leftCancelButton;
	TextView rightUpgradeButton;
	TextView addPhoneNumberButton;
	TextView addPhoneNumberLabel;
	FloatingActionButton fabButton;

	AlertDialog evaluateAppDialog;

	MegaNode inboxNode = null;

	private boolean mkLayoutVisible = false;

	MegaNode rootNode = null;

	NodeController nC;
	ContactController cC;
	AccountController aC;

	long[] searchDate = null;

	MegaNode selectedNode;
	MegaOffline selectedOfflineNode;
	MegaContactAdapter selectedUser;
	MegaContactRequest selectedRequest;

	public long selectedChatItemId;
//	String fullNameChat;

	private BadgeDrawerArrowDrawable badgeDrawable;

	//COLLECTION FAB BUTTONS
	CoordinatorLayout fabButtonsLayout;
	FloatingActionButton mainFabButtonChat;
	FloatingActionButton firstFabButtonChat;
	FloatingActionButton secondFabButtonChat;
	FloatingActionButton thirdFabButtonChat;
	private Animation openFabAnim,closeFabAnim,rotateLeftAnim,rotateRightAnim, collectionFABLayoutOut;
	boolean isFabOpen=false;
	//

	MegaPreferences prefs = null;
	ChatSettings chatSettings = null;
	MegaAttributes attr = null;
	static ManagerActivityLollipop managerActivity = null;
	MegaApplication app = null;
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	Handler handler;
	DisplayMetrics outMetrics;
    float scaleText;
    FrameLayout fragmentContainer;
//	boolean tranfersPaused = false;
	public Toolbar tB;
    ActionBar aB;
    AppBarLayout abL;

	int selectedPaymentMethod;
	int selectedAccountType;
	int displayedAccountType;

	int countUserAttributes=0;
	int errorUserAttibutes=0;

	ShareInfo infoManager;
	MegaNode parentNodeManager;

	boolean firstNavigationLevel = true;
    DrawerLayout drawerLayout;
    ArrayList<MegaUser> contacts = new ArrayList<>();
    ArrayList<MegaUser> visibleContacts = new ArrayList<>();

    public boolean openFolderRefresh = false;

    public boolean openSettingsStorage = false;
    public boolean openSettingsQR = false;
	boolean newAccount = false;
	public boolean newCreationAccount;

	private int storageState = MegaApiJava.STORAGE_STATE_UNKNOWN; //Default value
	private int storageStateFromBroadcast = MegaApiJava.STORAGE_STATE_UNKNOWN; //Default value
    private boolean showStorageAlertWithDelay;

	private boolean isStorageStatusDialogShown = false;

	private boolean userNameChanged;
	private boolean userEmailChanged;

	private AlertDialog reconnectDialog;

	private LinearLayout navigationDrawerAddPhoneContainer;
    int orientationSaved;

    float elevation = 0;
    private boolean isSMSDialogShowing;
    private String bonusStorageSMS = "GB";
    private final static String STATE_KEY_SMS_DIALOG =  "isSMSDialogShowing";
    private final static String STATE_KEY_SMS_BONUS =  "bonusStorageSMS";
	private BillingManager mBillingManager;
	private List<SkuDetails> mSkuDetailsList;

    public enum FragmentTag {
		CLOUD_DRIVE, RECENTS, OFFLINE, CAMERA_UPLOADS, MEDIA_UPLOADS, INBOX, INCOMING_SHARES, OUTGOING_SHARES, CONTACTS, RECEIVED_REQUESTS, SENT_REQUESTS, SETTINGS, MY_ACCOUNT, MY_STORAGE, SEARCH, TRANSFERS, COMPLETED_TRANSFERS, RECENT_CHAT, RUBBISH_BIN, NOTIFICATIONS, UPGRADE_ACCOUNT, FORTUMO, CENTILI, CREDIT_CARD, TURN_ON_NOTIFICATIONS, EXPORT_RECOVERY_KEY, PERMISSIONS, SMS_VERIFICATION, LINKS;

		public String getTag () {
			switch (this) {
				case CLOUD_DRIVE: return "fbFLol";
				case RECENTS: return "rF";
				case RUBBISH_BIN: return "rubbishBinFLol";
				case OFFLINE: return "oFLol";
				case CAMERA_UPLOADS: return "cuFLol";
				case MEDIA_UPLOADS: return "muFLol";
				case INBOX: return "iFLol";
				case INCOMING_SHARES: return "isF";
				case OUTGOING_SHARES: return "osF";
				case CONTACTS: return "android:switcher:" + R.id.contact_tabs_pager + ":" + 0;
				case SENT_REQUESTS: return "android:switcher:" + R.id.contact_tabs_pager + ":" + 1;
				case RECEIVED_REQUESTS: return "android:switcher:" + R.id.contact_tabs_pager + ":" + 2;
				case SETTINGS: return "sttF";
				case MY_ACCOUNT: return "android:switcher:" + R.id.my_account_tabs_pager + ":" + 0;
				case MY_STORAGE: return "android:switcher:" + R.id.my_account_tabs_pager + ":" + 1;
				case SEARCH: return "sFLol";
				case TRANSFERS: return "android:switcher:" + R.id.transfers_tabs_pager + ":" + 0;
				case COMPLETED_TRANSFERS: return "android:switcher:" + R.id.transfers_tabs_pager + ":" + 1;
				case RECENT_CHAT: return "rChat";
				case NOTIFICATIONS: return "notificFragment";
				case UPGRADE_ACCOUNT: return "upAFL";
				case FORTUMO: return "fF";
				case CENTILI: return "ctF";
				case CREDIT_CARD: return "ccF";
				case TURN_ON_NOTIFICATIONS: return "tonF";
				case EXPORT_RECOVERY_KEY: return "eRKeyF";
				case PERMISSIONS: return "pF";
                case SMS_VERIFICATION: return "svF";
				case LINKS: return "lF";
			}
			return null;
		}
	}

	public enum DrawerItem {
		CLOUD_DRIVE, SAVED_FOR_OFFLINE, CAMERA_UPLOADS, INBOX, SHARED_ITEMS, CONTACTS, SETTINGS, ACCOUNT, SEARCH, TRANSFERS, MEDIA_UPLOADS, CHAT, RUBBISH_BIN, NOTIFICATIONS;

		public String getTitle(Context context) {
			switch(this)
			{
				case CLOUD_DRIVE: return context.getString(R.string.section_cloud_drive);
				case SAVED_FOR_OFFLINE: return context.getString(R.string.section_saved_for_offline_new);
				case CAMERA_UPLOADS: return context.getString(R.string.section_photo_sync);
				case INBOX: return context.getString(R.string.section_inbox);
				case SHARED_ITEMS: return context.getString(R.string.title_shared_items);
				case CONTACTS: {
					context.getString(R.string.section_contacts);
				}
				case SETTINGS: return context.getString(R.string.action_settings);
				case ACCOUNT: return context.getString(R.string.section_account);
				case SEARCH: return context.getString(R.string.action_search);
				case TRANSFERS: return context.getString(R.string.section_transfers);
				case MEDIA_UPLOADS: return context.getString(R.string.section_secondary_media_uploads);
				case CHAT: return context.getString(R.string.section_chat);
				case RUBBISH_BIN: return context.getString(R.string.section_rubbish_bin);
				case NOTIFICATIONS: return context.getString(R.string.title_properties_chat_contact_notifications);
			}
			return null;
		}
	}

	public boolean turnOnNotifications = false;

	private int searchSharedTab = -1;
	private DrawerItem searchDrawerItem = null;
	static DrawerItem drawerItem = null;
	DrawerItem drawerItemPreUpgradeAccount = null;
	int accountFragmentPreUpgradeAccount = -1;
	static MenuItem drawerMenuItem = null;
	LinearLayout fragmentLayout;
	BottomNavigationViewEx bNV;
	NavigationView nV;
	RelativeLayout usedSpaceLayout;
    RelativeLayout accountInfoFrame;
	private EmojiTextView nVDisplayName;
	TextView nVEmail;
	TextView businessLabel;
	RoundedImageView nVPictureProfile;
	TextView spaceTV;
	ProgressBar usedSpacePB;

	//Tabs in Shares
	private TabLayout tabLayoutCloud;
	private CloudPageAdapter cloudPageAdapter;
	private CustomViewPager viewPagerCloud;

    //Tabs in Shares
	private TabLayout tabLayoutShares;
	private SharesPageAdapter sharesPageAdapter;
	private ViewPager viewPagerShares;

	//Tabs in Contacts
	private TabLayout tabLayoutContacts;
	private ContactsPageAdapter contactsPageAdapter;
	private ViewPager viewPagerContacts;

	//Tabs in My Account
	private TabLayout tabLayoutMyAccount;
	private MyAccountPageAdapter mTabsAdapterMyAccount;
	private ViewPager viewPagerMyAccount;

	//Tabs in Transfers
	private TabLayout tabLayoutTransfers;
	private TransfersPageAdapter mTabsAdapterTransfers;
	private ViewPager viewPagerTransfers;

	private RelativeLayout transfersOverViewLayout;
	private LinearLayout transfersTextLayout;
	private TextView transfersTitleText;
	private TextView transfersNumberText;
	private ImageView playButton;
	private RelativeLayout actionLayout;
	private RelativeLayout dotsOptionsTransfersLayout;
	private ProgressBar progressBarTransfers;

	private RelativeLayout callInProgressLayout;
	private Chronometer callInProgressChrono;
	private TextView callInProgressText;

	boolean firstTimeAfterInstallation = true;
	private ArrayList<String> offlineSearchPaths = new ArrayList<>();
	SearchView searchView;
	public boolean searchExpand = false;
	private String searchQuery = "";
	public boolean textSubmitted = false;
	public boolean textsearchQuery = false;
	boolean isSearching = false;
	ArrayList<MegaNode> searchNodes;
	public int levelsSearch = -1;
	boolean openLink = false;

	long lastTimeOnTransferUpdate = Calendar.getInstance().getTimeInMillis();

	public int orderCloud = ORDER_DEFAULT_ASC;
	public int orderContacts = ORDER_DEFAULT_ASC;
	public int orderOthers = ORDER_DEFAULT_ASC;
	public int orderCamera = MegaApiJava.ORDER_MODIFICATION_DESC;
//	private int orderOffline = MegaApiJava.ORDER_DEFAULT_ASC;
//	private int orderOutgoing = MegaApiJava.ORDER_DEFAULT_ASC;
//	private int orderIncoming = MegaApiJava.ORDER_DEFAULT_ASC;

	boolean firstLogin = false;
	private boolean isGetLink = false;
	private boolean isClearRubbishBin = false;
	private boolean moveToRubbish = false;
	private boolean restoreFromRubbish = false;

	private List<ShareInfo> filePreparedInfos;
	boolean megaContacts = true;
	String feedback;

//	private boolean isListCloudDrive = true;
//	private boolean isListOffline = true;
//	private boolean isListRubbishBin = true;
	public boolean isListCameraUploads = false;
//	public boolean isLargeGridCameraUploads = true;
	public boolean isSmallGridCameraUploads = false;

	//	private boolean isListInbox = true;
//	private boolean isListContacts = true;
//	private boolean isListIncoming = true;
//	private boolean isListOutgoing = true;
	public boolean passwordReminderFromMyAccount = false;

	public boolean isList = true;

	private long parentHandleBrowser;
	private long parentHandleRubbish;
	private long parentHandleIncoming;
	private long parentHandleLinks;
	private boolean isSearchEnabled;
	private long parentHandleOutgoing;
	private long parentHandleSearch;
	private long parentHandleInbox;
	private String pathNavigationOffline;
	private int deepBrowserTreeRecents = 0;
	private BucketSaved bucketSaved;
	public int deepBrowserTreeIncoming = 0;
	public int deepBrowserTreeOutgoing = 0;
	private int deepBrowserTreeLinks;
	int indexCloud = -1;
	int indexShares = -1;
	int indexContacts = -1;
	int indexAccount = -1;
	int indexTransfers = -1;

	//LOLLIPOP FRAGMENTS
    private FileBrowserFragmentLollipop fbFLol;
    private RecentsFragment rF;
    private RubbishBinFragmentLollipop rubbishBinFLol;
    private OfflineFragmentLollipop oFLol;
    private InboxFragmentLollipop iFLol;
    private IncomingSharesFragmentLollipop inSFLol;
	private OutgoingSharesFragmentLollipop outSFLol;
	private LinksFragment lF;
	private ContactsFragmentLollipop cFLol;
	private ReceivedRequestsFragmentLollipop rRFLol;
	private SentRequestsFragmentLollipop sRFLol;
	private MyAccountFragmentLollipop maFLol;
	private MyStorageFragmentLollipop mStorageFLol;
	private TransfersFragmentLollipop tFLol;
	private CompletedTransfersFragmentLollipop completedTFLol;
	private SearchFragmentLollipop sFLol;
	private SettingsFragmentLollipop sttFLol;
	private CameraUploadFragmentLollipop muFLol;
	private UpgradeAccountFragmentLollipop upAFL;
	private FortumoFragmentLollipop fFL;
	private CentiliFragmentLollipop ctFL;
	private CreditCardFragmentLollipop ccFL;
	private CameraUploadFragmentLollipop cuFL;
	private RecentChatsFragmentLollipop rChatFL;
	private NotificationsFragmentLollipop notificFragment;
	private TurnOnNotificationsFragment tonF;
	private ExportRecoveryKeyFragment eRKeyF;
	private PermissionsFragment pF;
	private SMSVerificationFragment svF;

	ProgressDialog statusDialog;

	private AlertDialog renameDialog;
	private AlertDialog newFolderDialog;
	private AlertDialog addContactDialog;
	private AlertDialog permissionsDialog;
	private AlertDialog presenceStatusDialog;
	private AlertDialog openLinkDialog;
	private boolean openLinkDialogIsShown = false;
	private boolean openLinkDialogIsErrorShown = false;
	private AlertDialog alertNotPermissionsUpload;
	private AlertDialog clearRubbishBinDialog;
	private AlertDialog downloadConfirmationDialog;
	private AlertDialog insertPassDialog;
	private AlertDialog changeUserAttributeDialog;
	private AlertDialog generalDialog;
	private AlertDialog setPinDialog;
	private AlertDialog alertDialogTransferOverquota;
	private AlertDialog alertDialogStorageStatus;
	private AlertDialog alertDialogSMSVerification;

	private MenuItem searchMenuItem;
	private MenuItem gridSmallLargeMenuItem;
	private MenuItem addContactMenuItem;
	private MenuItem addMenuItem;
//	private MenuItem pauseRestartTransfersItem;
	private MenuItem createFolderMenuItem;
	private MenuItem importLinkMenuItem;
	private MenuItem selectMenuItem;
	private MenuItem unSelectMenuItem;
	private MenuItem thumbViewMenuItem;
	private MenuItem refreshMenuItem;
	private MenuItem sortByMenuItem;
	private MenuItem helpMenuItem;
	private MenuItem upgradeAccountMenuItem;
	private MenuItem clearRubbishBinMenuitem;
	private MenuItem changePass;
	private MenuItem exportMK;
	private MenuItem takePicture;
	private MenuItem searchByDate;
	private MenuItem cancelSubscription;
	private MenuItem killAllSessions;
	private MenuItem cancelAllTransfersMenuItem;
	private MenuItem playTransfersMenuIcon;
	private MenuItem pauseTransfersMenuIcon;
	private MenuItem logoutMenuItem;
	private MenuItem forgotPassMenuItem;
	private MenuItem inviteMenuItem;
	private MenuItem clearCompletedTransfers;
	private MenuItem scanQRcodeMenuItem;
	private MenuItem rubbishBinMenuItem;
	private MenuItem returnCallMenuItem;

	private Chronometer chronometerMenuItem;
	private LinearLayout layoutCallMenuItem;

	private int typesCameraPermission = -1;
	AlertDialog enable2FADialog;
	boolean isEnable2FADialogShown = false;
	Button enable2FAButton;
	Button skip2FAButton;
	AlertDialog verify2FADialog;
	boolean verify2FADialogIsShown = false;
	int verifyPin2FADialogType;
	private boolean is2FAEnabled = false;
	InputMethodManager imm;
	private EditTextPIN firstPin;
	private EditTextPIN secondPin;
	private EditTextPIN thirdPin;
	private EditTextPIN fourthPin;
	private EditTextPIN fifthPin;
	private EditTextPIN sixthPin;
	private StringBuilder sb = new StringBuilder();
	private String pin = null;
	private String newMail = null;
	private TextView pinError;
	private ProgressBar verify2faProgressBar;
	private RelativeLayout lostYourDeviceButton;

	private boolean isFirstTime2fa = true;
	private boolean isErrorShown = false;
	private boolean pinLongClick = false;
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
	RelativeLayout settingsSection;
	Button upgradeAccount;
	TextView contactsSectionText;
	TextView notificationsSectionText;
	int bottomNavigationCurrentItem = -1;
	View chatBadge;
	View callBadge;

	private boolean joiningToChatLink = false;
	private long idJoinToChatLink = -1;

	private boolean onAskingPermissionsFragment = false;
	public boolean onAskingSMSVerificationFragment = false;

	private BroadcastReceiver refreshAddPhoneNumberButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context,Intent intent) {
            if (intent != null && intent.getAction() == BROADCAST_ACTION_INTENT_REFRESH_ADD_PHONE_NUMBER) {
                if(drawerLayout != null) {
                    drawerLayout.closeDrawer(Gravity.LEFT);
                }
                refreshAddPhoneNumberButton();
            }
        }
    };
	private EditText openLinkText;
	private RelativeLayout openLinkError;
	private TextView openLinkErrorText;
	private Button openLinkOpenButton;

	private boolean isBusinessGraceAlertShown = false;
	private AlertDialog businessGraceAlert;
	private boolean isBusinessCUAlertShown;
	private AlertDialog businessCUAlert;
	private String businessCUF;
	private boolean businessCUFirstTime;

	private BottomSheetDialogFragment bottomSheetDialogFragment;

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
			if (intent != null){
                if (ACTION_STORAGE_STATE_CHANGED.equals(intent.getAction())) {
                    storageStateFromBroadcast = intent.getIntExtra(EXTRA_STORAGE_STATE, MegaApiJava.STORAGE_STATE_UNKNOWN);
                    if (!showStorageAlertWithDelay) {
                        checkStorageStatus(storageStateFromBroadcast, false);
                    }
                    updateAccountDetailsVisibleInfo();
                    return;
				}

				int actionType = intent.getIntExtra("actionType", -1);

				if(actionType == UPDATE_GET_PRICING){
					logDebug("BROADCAST TO UPDATE AFTER GET PRICING");
					//UPGRADE_ACCOUNT_FRAGMENT
					upAFL = (UpgradeAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.UPGRADE_ACCOUNT.getTag());
					if(upAFL!=null){
						upAFL.setPricing();
					}

					//CENTILI_FRAGMENT
					ctFL = (CentiliFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CENTILI.getTag());
					if(ctFL!=null){
						ctFL.getPaymentId();
					}

					//FORTUMO_FRAGMENT
					fFL = (FortumoFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.FORTUMO.getTag());
					if(fFL!=null){
						fFL.getPaymentId();
					}
				}
				else if(actionType == UPDATE_ACCOUNT_DETAILS){
					logDebug("BROADCAST TO UPDATE AFTER UPDATE_ACCOUNT_DETAILS");
					if(!isFinishing()){

						updateAccountDetailsVisibleInfo();

						//Check if myAccount section is visible
						maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
						if(maFLol!=null){
							logDebug("Update the account fragment");
							maFLol.setAccountDetails();
						}

						mStorageFLol = (MyStorageFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_STORAGE.getTag());
						if(mStorageFLol!=null){
							logDebug("Update the account fragment");
							mStorageFLol.setAccountDetails();
						}

						upAFL = (UpgradeAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.UPGRADE_ACCOUNT.getTag());
						if (upAFL != null) {
							if (drawerItem == DrawerItem.ACCOUNT && accountFragment == UPGRADE_ACCOUNT_FRAGMENT && megaApi.isBusinessAccount()) {
								closeUpgradeAccountFragment();
							} else {
								upAFL.showAvailableAccount();
							}
						}

						if(getSettingsFragment() != null){
							sttFLol.setRubbishInfo();
						}

						checkBusinessStatus();

						if (megaApi.isBusinessAccount()) {
							supportInvalidateOptionsMenu();
						}
					}
				}
				else if(actionType == UPDATE_CREDIT_CARD_SUBSCRIPTION){
					logDebug("BROADCAST TO UPDATE AFTER UPDATE_CREDIT_CARD_SUBSCRIPTION");
					updateCancelSubscriptions();
				}
				else if(actionType == UPDATE_PAYMENT_METHODS){
					logDebug("BROADCAST TO UPDATE AFTER UPDATE_PAYMENT_METHODS");
				}
			}
		}
	};

	private BroadcastReceiver receiverUpdate2FA = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null) {
				boolean enabled = intent.getBooleanExtra("enabled", false);
				is2FAEnabled = enabled;
				if (getSettingsFragment() != null) {
					sttFLol.update2FAPreference(enabled);
				}
			}
		}
	};

	private BroadcastReceiver receiverUpdateOrder = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null) {
				boolean cloudOrder = intent.getBooleanExtra("cloudOrder", true);
				int order = intent.getIntExtra("order", ORDER_DEFAULT_ASC);
				if (cloudOrder) {
					refreshCloudOrder(order);
				}
				else {
					refreshOthersOrder(order);
				}
			}
		}
	};

    private BroadcastReceiver receiverUpdateView = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                updateView(intent.getBooleanExtra("isList", true));
				supportInvalidateOptionsMenu();
            }
        }
    };

	private BroadcastReceiver receiverCUAttrChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			synchronized (this){
				long handleInUserAttr = intent.getLongExtra(EXTRA_NODE_HANDLE, -1);
				boolean isSecondary = intent.getBooleanExtra(EXTRA_IS_CU_SECONDARY_FOLDER, false);
				if (isSecondary && drawerItem == DrawerItem.MEDIA_UPLOADS) {
					secondaryMediaUploadsClicked();
				} else if (!isSecondary && drawerItem == DrawerItem.CAMERA_UPLOADS) {
					cameraUploadsClicked();
				}

				//refresh settings if user is on that page
				if (getSettingsFragment() != null) {
					getSettingsFragment().setCUDestinationFolder(isSecondary, handleInUserAttr);
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

			if (intent != null){
				actionType = intent.getIntExtra("actionType", -1);

				if(actionType == GO_OFFLINE){
				    //stop cu process
                    stopRunningCameraUploadService(ManagerActivityLollipop.this);
					showOfflineMode();
				}
				else if(actionType == GO_ONLINE){
					showOnlineMode();
				}
				else if(actionType == START_RECONNECTION){
					startConnection();
				}
			}
		}
	};

	private BroadcastReceiver cameraUploadLauncherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context,Intent intent) {
            try {
				logDebug("cameraUploadLauncherReceiver: Start service here");
                startCameraUploadServiceIgnoreAttr(ManagerActivityLollipop.this);
            } catch (Exception e) {
				logError("cameraUploadLauncherReceiver: Exception", e);
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
				if (getContactsFragment() != null) {
					cFLol.updateContact(userHandle);
				}

				if (isIncomingAdded() && inSFLol.getItemCount() > 0) {
					inSFLol.updateContact(userHandle);
				}

				if (isOutgoingAdded() && outSFLol.getItemCount() > 0) {
					outSFLol.updateContact(userHandle);
				}
			} else if (intent.getAction().equals(ACTION_UPDATE_CREDENTIALS) && getContactsFragment() != null) {
				cFLol.updateContact(userHandle);
			}
		}
	};

	private BroadcastReceiver receiverUpdatePosition = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int position;
			int adapterType;
			int actionType;
			int placeholderCount;
			ImageView imageDrag = null;

			if (intent != null){
				actionType = intent.getIntExtra("actionType", -1);
				position = intent.getIntExtra("position", -1);
				placeholderCount = intent.getIntExtra("placeholder", 0);
				adapterType = intent.getIntExtra("adapterType", 0);

				if (position != -1){
					if (adapterType == RUBBISH_BIN_ADAPTER){
						rubbishBinFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
						if (rubbishBinFLol != null){
							if (actionType == UPDATE_IMAGE_DRAG) {
								imageDrag = rubbishBinFLol.getImageDrag(position + placeholderCount);
								if (rubbishBinFLol.imageDrag != null){
									rubbishBinFLol.imageDrag.setVisibility(View.VISIBLE);
								}
								if (imageDrag != null){
									rubbishBinFLol.imageDrag = imageDrag;
									rubbishBinFLol.imageDrag.setVisibility(View.GONE);
								}
							}
							else if (actionType == SCROLL_TO_POSITION) {
								rubbishBinFLol.updateScrollPosition(position + placeholderCount);
							}
						}
					}
					else if (adapterType == INBOX_ADAPTER){
						iFLol = (InboxFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.INBOX.getTag());
						if (iFLol != null){
							if (actionType == UPDATE_IMAGE_DRAG) {
								imageDrag = iFLol.getImageDrag(position  + placeholderCount);
								if (iFLol.imageDrag != null){
									iFLol.imageDrag.setVisibility(View.VISIBLE);
								}
								if (imageDrag != null){
									iFLol.imageDrag = imageDrag;
									iFLol.imageDrag.setVisibility(View.GONE);
								}
							}
							else if (actionType == SCROLL_TO_POSITION) {
								iFLol.updateScrollPosition(position + placeholderCount);
							}
						}
					}
					else if (adapterType == INCOMING_SHARES_ADAPTER){
						if (isIncomingAdded()) {
							if (actionType == UPDATE_IMAGE_DRAG) {
								imageDrag = inSFLol.getImageDrag(position + placeholderCount);
								if (inSFLol.imageDrag != null) {
									inSFLol.imageDrag.setVisibility(View.VISIBLE);
								}
								if (imageDrag != null) {
									inSFLol.imageDrag = imageDrag;
									inSFLol.imageDrag.setVisibility(View.GONE);
								}
							}
							if (actionType == SCROLL_TO_POSITION) {
								inSFLol.updateScrollPosition(position + placeholderCount);
							}
						}
					}
					else if (adapterType == OUTGOING_SHARES_ADAPTER){
						if (isOutgoingAdded()) {
							if (actionType == UPDATE_IMAGE_DRAG) {
								imageDrag = outSFLol.getImageDrag(position + placeholderCount);
								if (outSFLol.imageDrag != null) {
									outSFLol.imageDrag.setVisibility(View.VISIBLE);
								}
								if (imageDrag != null) {
									outSFLol.imageDrag = imageDrag;
									outSFLol.imageDrag.setVisibility(View.GONE);
								}
							}
							else if (actionType == SCROLL_TO_POSITION) {
								outSFLol.updateScrollPosition(position + placeholderCount);
							}
						}
					} else if (adapterType == LINKS_ADAPTER) {
						if (isLinksAdded()) {
							if (actionType == UPDATE_IMAGE_DRAG) {
								imageDrag = lF.getImageDrag(position + placeholderCount);
								if (lF.imageDrag != null) {
									lF.imageDrag.setVisibility(View.VISIBLE);
								}
								if (imageDrag != null) {
									lF.imageDrag = imageDrag;
									lF.imageDrag.setVisibility(View.GONE);
								}
							}
							if (actionType == SCROLL_TO_POSITION) {
								lF.updateScrollPosition(position + placeholderCount);
							}
						}
					}
					else if (adapterType == SEARCH_ADAPTER){
						Long handle = intent.getLongExtra("handle", -1);
						if (getSearchFragment() != null){
							ArrayList<MegaNode> listNodes = sFLol.getNodes();
							for (int i=0; i<listNodes.size(); i++){
								if (listNodes.get(i).getHandle() == handle){
									position = i + placeholderCount;
									break;
								}
							}

							if (actionType == UPDATE_IMAGE_DRAG) {
								imageDrag = sFLol.getImageDrag(position);
								if (sFLol.imageDrag != null){
									sFLol.imageDrag.setVisibility(View.VISIBLE);
								}
								if (imageDrag != null){
									sFLol.imageDrag = imageDrag;
									sFLol.imageDrag.setVisibility(View.GONE);
								}
							}
							else if (actionType == SCROLL_TO_POSITION) {
								sFLol.updateScrollPosition(position);
							}
						}
					}
					else if (adapterType == FILE_BROWSER_ADAPTER){
						if (getTabItemCloud() == CLOUD_TAB && isCloudAdded()){
							if (actionType == UPDATE_IMAGE_DRAG) {
								imageDrag = fbFLol.getImageDrag(position + placeholderCount);
								if (fbFLol.imageDrag != null){
									fbFLol.imageDrag.setVisibility(View.VISIBLE);
								}
								if (imageDrag != null){
									fbFLol.imageDrag = imageDrag;
									fbFLol.imageDrag.setVisibility(View.GONE);
								}
							}
							else if (actionType == SCROLL_TO_POSITION) {
								fbFLol.updateScrollPosition(position + placeholderCount);
							}
						}
					}
					else if (adapterType == PHOTO_SYNC_ADAPTER || adapterType == SEARCH_BY_ADAPTER) {
						Long handle = intent.getLongExtra("handle", -1);
						cuFL = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CAMERA_UPLOADS.getTag());
						if (cuFL != null){

							if (isListCameraUploads){
								ArrayList<CameraUploadFragmentLollipop.PhotoSyncHolder> listNodes = cuFL.getNodesArray();
								for (int i=0; i<listNodes.size(); i++){
									if (listNodes.get(i).getHandle() == handle){
										position = i + placeholderCount;
										break;
									}
								}
							}
							else {
								ArrayList<MegaMonthPicLollipop> listNodes = cuFL.getMonthPics();
								ArrayList<Long> handles;
								int count = 0;
								boolean found = false;
								for (int i=0; i<listNodes.size(); i++){
									handles = listNodes.get(i).getNodeHandles();
									for (int j=0; j<handles.size(); j++){
										count++;
										String h1 = handles.get(j).toString();
										String h2 = handle.toString();
										if (h1.equals(h2)){
											position = count + placeholderCount;
											found = true;
											break;
										}
									}
									count++;
									if (found){
										break;
									}
								}
							}

							if (actionType == UPDATE_IMAGE_DRAG) {
								imageDrag = cuFL.getImageDrag(position);
								if (cuFL.imageDrag != null){
									cuFL.imageDrag.setVisibility(View.VISIBLE);
								}
								if (imageDrag != null){
									cuFL.imageDrag = imageDrag;
									cuFL.imageDrag.setVisibility(View.GONE);
								}
							}
							else if (actionType == SCROLL_TO_POSITION) {
								cuFL.updateScrollPosition(position);
							}
						}
						muFLol = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MEDIA_UPLOADS.getTag());
						if (muFLol != null){
							if (isListCameraUploads){
								ArrayList<CameraUploadFragmentLollipop.PhotoSyncHolder> listNodes = muFLol.getNodesArray();
								for (int i=0; i<listNodes.size(); i++){
									if (listNodes.get(i).getHandle() == handle){
										position = i + placeholderCount;
										break;
									}
								}
							}
							else {
								ArrayList<MegaMonthPicLollipop> listNodes = muFLol.getMonthPics();
								ArrayList<Long> handles;
								int count = 0;
								boolean found = false;
								for (int i=0; i<listNodes.size(); i++){
									handles = listNodes.get(i).getNodeHandles();
									for (int j=0; j<handles.size(); j++){
										count++;
										String h1 = handles.get(j).toString();
										String h2 = String.valueOf(handle);
										if (h1.equals(h2)){
											position = count + placeholderCount;
											found = true;
											break;
										}
									}
									count++;
									if (found){
										break;
									}
								}
							}

							if (actionType == UPDATE_IMAGE_DRAG) {
								imageDrag = muFLol.getImageDrag(position);
								if (muFLol.imageDrag != null){
									muFLol.imageDrag.setVisibility(View.VISIBLE);
								}
								if (imageDrag != null){
									muFLol.imageDrag = imageDrag;
									muFLol.imageDrag.setVisibility(View.GONE);
								}
							}
							else if (actionType == SCROLL_TO_POSITION) {
								muFLol.updateScrollPosition(position);
							}
						}
					}
					else if (adapterType == OFFLINE_ADAPTER){
						oFLol = (OfflineFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.OFFLINE.getTag());
						if (oFLol != null){
							if (actionType == UPDATE_IMAGE_DRAG) {
								imageDrag = oFLol.getImageDrag(position + placeholderCount);
								if (oFLol.imageDrag != null){
									oFLol.imageDrag.setVisibility(View.VISIBLE);
								}
								if (imageDrag != null){
									oFLol.imageDrag = imageDrag;
									oFLol.imageDrag.setVisibility(View.GONE);
								}
							}
							else if (actionType == SCROLL_TO_POSITION) {
								oFLol.updateScrollPosition(position + placeholderCount);
							}
						}
					}

					if (imageDrag != null){
						int[] positionDrag = new int[2];
						int[] screenPosition = new int[4];
						imageDrag.getLocationOnScreen(positionDrag);

						screenPosition[0] = (imageDrag.getWidth() / 2) + positionDrag[0];
						screenPosition[1] = (imageDrag.getHeight() / 2) + positionDrag[1];
						screenPosition[2] = imageDrag.getWidth();
						screenPosition[3] = imageDrag.getHeight();

						Intent intent1 =  new Intent(BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG);
						intent1.putExtra("screenPosition", screenPosition);
						LocalBroadcastManager.getInstance(managerActivity).sendBroadcast(intent1);
					}
				}
			}
		}
	};

	private BroadcastReceiver chatCallUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null || intent.getAction() == null)
				return;

			if (intent.getAction().equals(ACTION_CALL_STATUS_UPDATE)) {
				long chatIdReceived = intent.getLongExtra(UPDATE_CHAT_CALL_ID, -1);

				if (chatIdReceived == -1)
					return;

				int callStatus = intent.getIntExtra(UPDATE_CALL_STATUS, -1);
				switch (callStatus) {
					case MegaChatCall.CALL_STATUS_REQUEST_SENT:
					case MegaChatCall.CALL_STATUS_RING_IN:
					case MegaChatCall.CALL_STATUS_IN_PROGRESS:
					case MegaChatCall.CALL_STATUS_RECONNECTING:
					case MegaChatCall.CALL_STATUS_JOINING:
					case MegaChatCall.CALL_STATUS_DESTROYED:
					case MegaChatCall.CALL_STATUS_USER_NO_PRESENT:
						rChatFL = (RecentChatsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
						if (rChatFL != null && rChatFL.isVisible()) {
							rChatFL.refreshNode(megaChatApi.getChatListItem(chatIdReceived));
						}

						if (isScreenInPortrait(ManagerActivityLollipop.this)) {
							setCallWidget();
						} else {
							supportInvalidateOptionsMenu();
						}

						break;
				}
			}
		}
	};

    private BroadcastReceiver updateCUSettingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                switch (intent.getAction()) {
                    case ACTION_REFRESH_CAMERA_UPLOADS_SETTING:
                        if (getSettingsFragment() != null) {
                            sttFLol.refreshCameraUploadsSettings();
                        }
                        break;
                    case ACTION_REFRESH_CAMERA_UPLOADS_MEDIA_SETTING:
                        // disable UI elements of media upload omly
                        if (getSettingsFragment() != null) {
                            sttFLol.disableMediaUploadUIProcess();
                        }
                        break;
                    case ACTION_REFRESH_CLEAR_OFFLINE_SETTING:
                        if (getSettingsFragment() != null) {
                            sttFLol.taskGetSizeOffline();
                        }
                        break;
                }
            }
        }
    };

    public void launchPayment(String productId) {
        //start purchase/subscription flow
        SkuDetails skuDetails = getSkuDetails(mSkuDetailsList, productId);
        Purchase purchase = app.getMyAccountInfo().getActiveGooglePlaySubscription();
        String oldSku = purchase == null ? null : purchase.getSku();
        String token = purchase == null ? null : purchase.getPurchaseToken();
        if (mBillingManager != null) {
            mBillingManager.initiatePurchaseFlow(oldSku, token, skuDetails);
        }
    }

	private SkuDetails getSkuDetails(List<SkuDetails> list, String key) {
		if (list == null || list.isEmpty()) {
			return null;
		}

		for (SkuDetails details : list) {
			if (details.getSku().equals(key)) {
				return details;
			}
		}
		return null;
	}

	private void getInventory() {
		SkuDetailsResponseListener listener = new SkuDetailsResponseListener() {
			@Override
			public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
				if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
					logWarning("Failed to get SkuDetails, error code is " + billingResult.getResponseCode());
				}
				if (skuDetailsList != null && skuDetailsList.size() > 0) {
					mSkuDetailsList = skuDetailsList;
					app.getMyAccountInfo().setAvailableSkus(skuDetailsList);
				}
			}
		};

		List<String> inAppSkus = new ArrayList<>();
		inAppSkus.add(SKU_PRO_I_MONTH);
		inAppSkus.add(SKU_PRO_I_YEAR);
		inAppSkus.add(SKU_PRO_II_MONTH);
		inAppSkus.add(SKU_PRO_II_YEAR);
		inAppSkus.add(SKU_PRO_III_MONTH);
		inAppSkus.add(SKU_PRO_III_YEAR);
		inAppSkus.add(SKU_PRO_LITE_MONTH);
		inAppSkus.add(SKU_PRO_LITE_YEAR);

		//we only support subscription for google pay
		mBillingManager.querySkuDetailsAsync(BillingClient.SkuType.SUBS, inAppSkus, listener);
	}

	public void initGooglePlayPayments() {
		//make sure user logged in
		MegaUser user = megaApi.getMyUser();
		if (user != null) {
			String payload = String.valueOf(user.getHandle());
			mBillingManager = new BillingManager(this, this, payload);
		}
	}

	@Override
	public void onBillingClientSetupFinished() {
		logInfo("Google play billing client setup finished");
		getInventory();
	}

	@Override
	public void onQueryPurchasesFinished(int resultCode, List<Purchase> purchases) {
		if (resultCode != BillingClient.BillingResponseCode.OK || purchases == null) {
			logWarning("Query of purchases failed, result code is " + resultCode + ", is purchase null: " + (purchases == null));
			return;
		}

		updateAccountInfo(purchases);
		updateSubscriptionLevel(app.getMyAccountInfo());
	}

	@Override
	public void onPurchasesUpdated(int resultCode, List<Purchase> purchases) {
        if (resultCode == BillingClient.BillingResponseCode.OK) {
            String message;
            if (purchases != null && !purchases.isEmpty()) {
                Purchase purchase = purchases.get(0);
                //payment may take time to process, we will not give privilege until it has been fully processed
                String sku = purchase.getSku();
                String subscriptionType = getSubscriptionType(this, sku);
                String subscriptionRenewalType = getSubscriptionRenewalType(this, sku);
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    //payment has been processed
                    updateAccountInfo(purchases);
                    logDebug("Purchase " + sku + " successfully, subscription type is: " + subscriptionType + ", subscription renewal type is: " + subscriptionRenewalType);
                    message = getString(R.string.message_user_purchased_subscription, subscriptionType, subscriptionRenewalType);
                    updateSubscriptionLevel(app.getMyAccountInfo());
                } else {
                    //payment is being processed or in unknown state
                    logDebug("Purchase " + sku + " is being processed or in unknown state.");
                    message = getString(R.string.message_user_payment_pending);
                }
                showAlert(this, message, null);
            } else {
                //down grade case
                logDebug("Downgrade, the new subscription takes effect when the old one expires.");
                message = getString(R.string.message_user_purchased_subscription_down_grade);
                showAlert(this, message, null);
            }
            drawerItem = DrawerItem.CLOUD_DRIVE;
            selectDrawerItemLollipop(drawerItem);
        } else {
            logWarning("Update purchase failed, with result code: " + resultCode);
        }
	}

	private void updateAccountInfo(List<Purchase> purchases) {
		MyAccountInfo myAccountInfo = app.getMyAccountInfo();
		int highest = -1;
		int temp = -1;
		Purchase max = null;
		for (Purchase purchase : purchases) {
			logDebug(purchase.getSku() + " (JSON): " + purchase.getOriginalJson());
			switch (purchase.getSku()) {
				case SKU_PRO_LITE_MONTH:
				case SKU_PRO_LITE_YEAR:
					temp = 0;
					break;
				case SKU_PRO_I_MONTH:
				case SKU_PRO_I_YEAR:
                    temp = 1;
					break;
				case SKU_PRO_II_MONTH:
				case SKU_PRO_II_YEAR:
                    temp = 2;
					break;
				case SKU_PRO_III_MONTH:
				case SKU_PRO_III_YEAR:
                    temp = 3;
					break;
			}

			if(temp >= highest){
			    highest = temp;
			    max = purchase;
            }
		}

        if(max != null && mBillingManager.isPayloadValid(max.getDeveloperPayload())){
            myAccountInfo.setActiveGooglePlaySubscription(max);
        }

		myAccountInfo.setLevelInventory(highest);
		myAccountInfo.setInventoryFinished(true);

		upAFL = (UpgradeAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.UPGRADE_ACCOUNT.getTag());
		if (upAFL != null) {
			upAFL.setPricing();
		}
	}

	private void updateSubscriptionLevel(MyAccountInfo myAccountInfo) {
		Purchase highestGooglePlaySubscription = myAccountInfo.getActiveGooglePlaySubscription();
		if (!myAccountInfo.isAccountDetailsFinished() || highestGooglePlaySubscription == null) {
			return;
		}

		String json = highestGooglePlaySubscription.getOriginalJson();
		logDebug("ORIGINAL JSON:" + json); //Print JSON in logs to help debug possible payments issues

		MegaAttributes attributes = dbH.getAttributes();
		long lastPublicHandle = attributes.getLastPublicHandle();

		if (myAccountInfo.getLevelInventory() > myAccountInfo.getLevelAccountDetails()) {
			if (lastPublicHandle == INVALID_HANDLE) {
				megaApi.submitPurchaseReceipt(MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET, json, this);
			} else {
				megaApi.submitPurchaseReceipt(MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET, json, lastPublicHandle,
						attributes.getLastPublicHandleType(), attributes.getLastPublicHandleTimeStamp(), this);
			}
		}
	}

	@Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		switch(requestCode){
			case REQUEST_READ_CONTACTS:{
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
					if (checkPermission(Manifest.permission.READ_CONTACTS)){
						Intent phoneContactIntent = new Intent(this, PhoneContactsActivityLollipop.class);
						this.startActivity(phoneContactIntent);
					}
				}
				break;
			}
			case REQUEST_UPLOAD_CONTACT:{
				uploadContactInfo(infoManager, parentNodeManager);
				break;
			}
	        case REQUEST_CAMERA:{
				if (typesCameraPermission == TAKE_PICTURE_OPTION) {
					logDebug("TAKE_PICTURE_OPTION");
		        	if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
		        		if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
		        			ActivityCompat.requestPermissions(this,
					                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
									REQUEST_WRITE_STORAGE);
		        		}
		        		else{
							checkTakePicture(this, TAKE_PHOTO_CODE);
							typesCameraPermission = -1;
		        		}
		        	}
	        	} else if (typesCameraPermission == TAKE_PROFILE_PICTURE) {
					logDebug("TAKE_PROFILE_PICTURE");
					if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
						if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
							ActivityCompat.requestPermissions(this,
									new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
									REQUEST_WRITE_STORAGE);
						}
						else{
							this.takeProfilePicture();
							typesCameraPermission = -1;
						}
					}
				}else if(typesCameraPermission == START_CALL_PERMISSIONS){
					if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
						if(checkPermissionsCall()){
							returnCall(this);
						}
						typesCameraPermission = -1;
					}
				}
	        	break;
	        }
			case REQUEST_READ_WRITE_STORAGE:{
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
					showUploadPanel();
				}
				break;
			}
	        case REQUEST_WRITE_STORAGE:{
	        	if (firstLogin){
					logDebug("The first time");
	        		if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

//						if (firstLogin){
//							firstLogin = false;
//						}

						if (typesCameraPermission==TAKE_PICTURE_OPTION){
							logDebug("TAKE_PICTURE_OPTION");
							if (!checkPermission(Manifest.permission.CAMERA)){
								ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
							}
							else{
								checkTakePicture(this, TAKE_PHOTO_CODE);
								typesCameraPermission = -1;
							}
						}
						else if (typesCameraPermission==TAKE_PROFILE_PICTURE){
							logDebug("TAKE_PROFILE_PICTURE");
							if (!checkPermission(Manifest.permission.CAMERA)){
								ActivityCompat.requestPermissions(this,
										new String[]{Manifest.permission.CAMERA},
										REQUEST_CAMERA);
							}
							else{
								this.takeProfilePicture();
								typesCameraPermission = -1;
							}
						}
		        	}
	        	}
	        	else{
					if (typesCameraPermission==TAKE_PICTURE_OPTION){
						logDebug("TAKE_PICTURE_OPTION");
						if (!checkPermission(Manifest.permission.CAMERA)){
							ActivityCompat.requestPermissions(this,
									new String[]{Manifest.permission.CAMERA},
									REQUEST_CAMERA);
						}
						else{
							checkTakePicture(this, TAKE_PHOTO_CODE);
							typesCameraPermission = -1;
						}
					}
					else if (typesCameraPermission==TAKE_PROFILE_PICTURE){
						logDebug("TAKE_PROFILE_PICTURE");
						if (!checkPermission(Manifest.permission.CAMERA)){
							ActivityCompat.requestPermissions(this,
									new String[]{Manifest.permission.CAMERA},
									REQUEST_CAMERA);
						}
						else{
							this.takeProfilePicture();
							typesCameraPermission = -1;
						}
					}
					else{
						oFLol = (OfflineFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.OFFLINE.getTag());
						if(oFLol != null){
							oFLol.notifyDataSetChanged();
						}
					}
				}
	        	break;
	        }

            case REQUEST_CAMERA_UPLOAD:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    checkIfShouldShowBusinessCUAlert(BUSINESS_CU_FRAGMENT_SETTINGS, false);
                } else {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.on_refuse_storage_permission), INVALID_HANDLE);
                }

                break;
            }

            case REQUEST_CAMERA_ON_OFF: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkIfShouldShowBusinessCUAlert(BUSINESS_CU_FRAGMENT_CU, false);
                } else {
					showSnackbar(SNACKBAR_TYPE, getString(R.string.on_refuse_storage_permission), INVALID_HANDLE);
                }
                break;
            }

            case REQUEST_CAMERA_ON_OFF_FIRST_TIME:{
                if(permissions.length == 0) {
                    return;
                }
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    checkIfShouldShowBusinessCUAlert(BUSINESS_CU_FRAGMENT_CU, true);
                } else {
                    if(!ActivityCompat.shouldShowRequestPermissionRationale(this,permissions[0])){
                        cuFL = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CAMERA_UPLOADS.getTag());
                        if(cuFL != null){
                            cuFL.onStoragePermissionRefused();
                        }
                    } else {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.on_refuse_storage_permission), INVALID_HANDLE);
                    }
                }

                break;
            }

			case PermissionsFragment.PERMISSIONS_FRAGMENT: {
				pF = (PermissionsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.PERMISSIONS.getTag());
				if (pF != null) {
					pF.setNextPermission();
				}
				break;
			}

			case RECORD_AUDIO: {
				if(typesCameraPermission == START_CALL_PERMISSIONS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
					if(checkPermissionsCall()){
						returnCall(this);
					}
					typesCameraPermission = -1;
				}
				break;


			}
        }
    }

	public void setTypesCameraPermission(int typesCameraPermission) {
		this.typesCameraPermission = typesCameraPermission;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		logDebug("onSaveInstanceState");
		if (drawerItem != null){
			logDebug("DrawerItem = " + drawerItem);
		}
		else{
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
		outState.putSerializable(SEARCH_DRAWER_ITEM, searchDrawerItem);
		outState.putSerializable(SEARCH_SHARED_TAB, searchSharedTab);
		outState.putBoolean("firstLogin", firstLogin);

		outState.putBoolean("isSearchEnabled", isSearchEnabled);
		outState.putLongArray("searchDate",searchDate);
		outState.putBoolean(STATE_KEY_SMS_DIALOG, isSMSDialogShowing);
		outState.putString(STATE_KEY_SMS_BONUS, bonusStorageSMS);

		if (parentHandleIncoming != INVALID_HANDLE) {
			outState.putInt("deepBrowserTreeIncoming", deepBrowserTreeIncoming);
		}

		if (parentHandleOutgoing != INVALID_HANDLE) {
			outState.putInt("deepBrowserTreeOutgoing", deepBrowserTreeOutgoing);
		}

		if (parentHandleLinks != INVALID_HANDLE) {
			outState.putInt(DEEP_BROWSER_TREE_LINKS, deepBrowserTreeLinks);
		}

		if (deepBrowserTreeRecents > 0 && isRecentsAdded() && getBucketSaved() != null) {
			outState.putSerializable("bucketSaved", getBucketSaved());
		}
		else {
			setDeepBrowserTreeRecents(0);
		}
		outState.putInt(DEEP_BROWSER_TREE_RECENTS, deepBrowserTreeRecents);

		if (viewPagerCloud != null) {
			indexCloud = viewPagerCloud.getCurrentItem();
		}
		outState.putInt(INDEX_CLOUD, indexCloud);

		if (viewPagerShares != null) {
			indexShares = viewPagerShares.getCurrentItem();
		}
		outState.putInt("indexShares", indexShares);

		if (viewPagerContacts != null) {
			indexContacts = viewPagerContacts.getCurrentItem();
		}
		outState.putInt("indexContacts", indexContacts);

		oFLol = (OfflineFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.OFFLINE.getTag());
		if(oFLol!=null){
			pathNavigationOffline = oFLol.getPathNavigation();
		}
		outState.putString("pathNavigationOffline", pathNavigationOffline);
//		outState.putParcelable("obj", myClass);
		if(drawerItem==DrawerItem.ACCOUNT){
			outState.putInt("accountFragment", accountFragment);
		}
		outState.putBoolean(MK_LAYOUT_VISIBLE, mkLayoutVisible);

		outState.putStringArrayList(OFFLINE_SEARCH_PATHS, offlineSearchPaths);
		if(searchQuery!=null){
			outState.putInt("levelsSearch", levelsSearch);
			outState.putString("searchQuery", searchQuery);
			textsearchQuery = true;
			outState.putBoolean("textsearchQuery", textsearchQuery);
		}else {
			textsearchQuery = false;
		}
		if (passwordReminderFromMyAccount){
			outState.putBoolean("passwordReminderFromMyAccount", true);
		}
		if (turnOnNotifications){
			outState.putBoolean("turnOnNotifications", turnOnNotifications);
		}

		outState.putInt("orientationSaved", orientationSaved);
		outState.putBoolean("verify2FADialogIsShown", verify2FADialogIsShown);
		outState.putInt("verifyPin2FADialogType", verifyPin2FADialogType);
		outState.putBoolean("isEnable2FADialogShown", isEnable2FADialogShown);
		outState.putInt("bottomNavigationCurrentItem", bottomNavigationCurrentItem);
		outState.putBoolean("searchExpand", searchExpand);
		outState.putBoolean("comesFromNotifications", comesFromNotifications);
		outState.putInt("comesFromNotificationsLevel", comesFromNotificationsLevel);
		outState.putLong("comesFromNotificationHandle", comesFromNotificationHandle);
		outState.putLong("comesFromNotificationHandleSaved", comesFromNotificationHandleSaved);
		outState.putBoolean("onAskingPermissionsFragment", onAskingPermissionsFragment);
		pF = (PermissionsFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.PERMISSIONS.getTag());
		if (onAskingPermissionsFragment && pF != null) {
			getSupportFragmentManager().putFragment(outState, FragmentTag.PERMISSIONS.getTag(), pF);
		}
        outState.putBoolean("onAskingSMSVerificationFragment", onAskingSMSVerificationFragment);
        svF = (SMSVerificationFragment) getSupportFragmentManager().findFragmentByTag(FragmentTag.SMS_VERIFICATION.getTag());
        if (onAskingSMSVerificationFragment && svF != null) {
            getSupportFragmentManager().putFragment(outState, FragmentTag.SMS_VERIFICATION.getTag(), svF);
        }
		outState.putFloat("elevation", abL.getElevation());
		outState.putInt("storageState", storageState);
		outState.putBoolean("isStorageStatusDialogShown", isStorageStatusDialogShown);
		outState.putSerializable("drawerItemPreUpgradeAccount", drawerItemPreUpgradeAccount);
		outState.putInt("accountFragmentPreUpgradeAccount", accountFragmentPreUpgradeAccount);
		outState.putInt("comesFromNotificationDeepBrowserTreeIncoming", comesFromNotificationDeepBrowserTreeIncoming);
		outState.putBoolean("openLinkDialogIsShown", openLinkDialogIsShown);
		if (openLinkDialogIsShown) {
			if (openLinkText != null && openLinkText.getText() != null) {
				outState.putString("openLinkText", openLinkText.getText().toString());
			}
			else {
				outState.putString("openLinkText", "");
			}
			outState.putBoolean("openLinkDialogIsErrorShown", openLinkDialogIsErrorShown);
		}

		outState.putBoolean(BUSINESS_GRACE_ALERT_SHOWN, isBusinessGraceAlertShown);
		if (isBusinessCUAlertShown) {
			outState.putBoolean(BUSINESS_CU_ALERT_SHOWN, isBusinessCUAlertShown);
			outState.putString(BUSINESS_CU_FRAGMENT, businessCUF);
			outState.putBoolean(BUSINESS_CU_FIRST_TIME, businessCUFirstTime);
		}
	}

	@Override
	public void onStart(){
		logDebug("onStart");
		super.onStart();
	}

	@SuppressLint("NewApi") @Override
    protected void onCreate(Bundle savedInstanceState) {
		logDebug("onCreate");
//		Fragments are restored during the Activity's onCreate().
//		Importantly though, they are restored in the base Activity class's onCreate().
//		Thus if you call super.onCreate() first, all of the rest of your onCreate() method will execute after your Fragments have been restored.
		super.onCreate(savedInstanceState);
		logDebug("onCreate after call super");

		boolean selectDrawerItemPending = true;
		//upload from device, progress dialog should show when screen orientation changes.
        if (shouldShowDialog) {
            showProcessFileDialog(this,null);
        }
		if(savedInstanceState!=null){
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
			deepBrowserTreeRecents = savedInstanceState.getInt(DEEP_BROWSER_TREE_RECENTS, 0);
			if (deepBrowserTreeRecents > 0) {
				setBucketSaved((BucketSaved) savedInstanceState.getSerializable("bucketSaved"));
			}
			deepBrowserTreeIncoming = savedInstanceState.getInt("deepBrowserTreeIncoming", 0);
			deepBrowserTreeOutgoing = savedInstanceState.getInt("deepBrowserTreeOutgoing", 0);
			deepBrowserTreeLinks = savedInstanceState.getInt(DEEP_BROWSER_TREE_LINKS, 0);
			isSearchEnabled = savedInstanceState.getBoolean("isSearchEnabled");
			isSMSDialogShowing = savedInstanceState.getBoolean(STATE_KEY_SMS_DIALOG, false);
			bonusStorageSMS = savedInstanceState.getString(STATE_KEY_SMS_BONUS);
			searchDate = savedInstanceState.getLongArray("searchDate");
			firstLogin = savedInstanceState.getBoolean("firstLogin");
			drawerItem = (DrawerItem) savedInstanceState.getSerializable("drawerItem");
			searchDrawerItem = (DrawerItem) savedInstanceState.getSerializable(SEARCH_DRAWER_ITEM);
			searchSharedTab = savedInstanceState.getInt(SEARCH_SHARED_TAB);
			indexCloud = savedInstanceState.getInt(INDEX_CLOUD, indexCloud);
			indexShares = savedInstanceState.getInt("indexShares", indexShares);
			logDebug("savedInstanceState -> indexShares: " + indexShares);
			indexContacts = savedInstanceState.getInt("indexContacts", 0);
			pathNavigationOffline = savedInstanceState.getString("pathNavigationOffline", pathNavigationOffline);
			logDebug("savedInstanceState -> pathNavigationOffline: " + pathNavigationOffline);
			accountFragment = savedInstanceState.getInt("accountFragment", -1);
			mkLayoutVisible = savedInstanceState.getBoolean(MK_LAYOUT_VISIBLE, false);
			selectedAccountType = savedInstanceState.getInt("selectedAccountType", -1);
			selectedPaymentMethod = savedInstanceState.getInt("selectedPaymentMethod", -1);
			offlineSearchPaths = savedInstanceState.getStringArrayList(OFFLINE_SEARCH_PATHS);
			searchQuery = savedInstanceState.getString("searchQuery");
			textsearchQuery = savedInstanceState.getBoolean("textsearchQuery");
			levelsSearch = savedInstanceState.getInt("levelsSearch");
			passwordReminderFromMyAccount = savedInstanceState.getBoolean("passwordReminderFromaMyAccount", false);
			turnOnNotifications = savedInstanceState.getBoolean("turnOnNotifications", false);
			orientationSaved = savedInstanceState.getInt("orientationSaved");
			verify2FADialogIsShown = savedInstanceState.getBoolean("verify2FADialogIsShown", false);
			verifyPin2FADialogType = savedInstanceState.getInt("verifyPin2FADialogType");
			isEnable2FADialogShown = savedInstanceState.getBoolean("isEnable2FADialogShown", false);
			bottomNavigationCurrentItem = savedInstanceState.getInt("bottomNavigationCurrentItem", -1);
			searchExpand = savedInstanceState.getBoolean("searchExpand", false);
			comesFromNotifications = savedInstanceState.getBoolean("comesFromNotifications", false);
			comesFromNotificationsLevel = savedInstanceState.getInt("comesFromNotificationsLevel", 0);
			comesFromNotificationHandle = savedInstanceState.getLong("comesFromNotificationHandle", -1);
			comesFromNotificationHandleSaved = savedInstanceState.getLong("comesFromNotificationHandleSaved", -1);
			onAskingPermissionsFragment = savedInstanceState.getBoolean("onAskingPermissionsFragment", false);
			if (onAskingPermissionsFragment) {
				pF = (PermissionsFragment) getSupportFragmentManager().getFragment(savedInstanceState, FragmentTag.PERMISSIONS.getTag());
			}
            onAskingSMSVerificationFragment = savedInstanceState.getBoolean("onAskingSMSVerificationFragment", false);
            if (onAskingSMSVerificationFragment) {
                svF = (SMSVerificationFragment) getSupportFragmentManager().getFragment(savedInstanceState, FragmentTag.SMS_VERIFICATION.getTag());
            }
			elevation = savedInstanceState.getFloat("elevation", 0);
			storageState = savedInstanceState.getInt("storageState", MegaApiJava.STORAGE_STATE_UNKNOWN);
			isStorageStatusDialogShown = savedInstanceState.getBoolean("isStorageStatusDialogShown", false);
			drawerItemPreUpgradeAccount = (DrawerItem) savedInstanceState.getSerializable("drawerItemPreUpgradeAccount");
			accountFragmentPreUpgradeAccount = savedInstanceState.getInt("accountFragmentPreUpgradeAccount", -1);
			comesFromNotificationDeepBrowserTreeIncoming = savedInstanceState.getInt("comesFromNotificationDeepBrowserTreeIncoming", -1);
			openLinkDialogIsShown = savedInstanceState.getBoolean("openLinkDialogIsShown", false);
			isBusinessGraceAlertShown = savedInstanceState.getBoolean(BUSINESS_GRACE_ALERT_SHOWN, false);
			isBusinessCUAlertShown = savedInstanceState.getBoolean(BUSINESS_CU_ALERT_SHOWN, false);
			if (isBusinessCUAlertShown) {
				businessCUF = savedInstanceState.getString(BUSINESS_CU_FRAGMENT);
				businessCUFirstTime = savedInstanceState.getBoolean(BUSINESS_CU_FIRST_TIME, false);
			}
		}
		else{
			logDebug("Bundle is NULL");
			parentHandleBrowser = -1;
			parentHandleRubbish = -1;
			parentHandleIncoming = -1;
			parentHandleOutgoing = -1;
			parentHandleLinks = INVALID_HANDLE;
			isSearchEnabled= false;
			parentHandleSearch = -1;
			parentHandleInbox = -1;
			indexContacts = -1;
			deepBrowserTreeIncoming = 0;
			deepBrowserTreeOutgoing = 0;
			deepBrowserTreeLinks = 0;
			this.setPathNavigationOffline("/");
		}

		LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
		if (localBroadcastManager != null) {
			IntentFilter contactUpdateFilter = new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE);
			contactUpdateFilter.addAction(ACTION_UPDATE_NICKNAME);
			contactUpdateFilter.addAction(ACTION_UPDATE_FIRST_NAME);
			contactUpdateFilter.addAction(ACTION_UPDATE_LAST_NAME);
			contactUpdateFilter.addAction(ACTION_UPDATE_CREDENTIALS);
			localBroadcastManager.registerReceiver(contactUpdateReceiver, contactUpdateFilter);

			localBroadcastManager.registerReceiver(receiverUpdatePosition,
					new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_UPDATE_POSITION));

			IntentFilter filter = new IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS);
			filter.addAction(ACTION_STORAGE_STATE_CHANGED);
			localBroadcastManager.registerReceiver(updateMyAccountReceiver, filter);

			localBroadcastManager.registerReceiver(receiverUpdate2FA,
					new IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_2FA_SETTINGS));

			localBroadcastManager.registerReceiver(networkReceiver,
					new IntentFilter(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE));

			localBroadcastManager.registerReceiver(receiverCUAttrChanged,
					new IntentFilter(BROADCAST_ACTION_INTENT_CU_ATTR_CHANGE));

			localBroadcastManager.registerReceiver(receiverUpdateOrder, new IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_ORDER));
            localBroadcastManager.registerReceiver(receiverUpdateView, new IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_VIEW));
            localBroadcastManager.registerReceiver(chatArchivedReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_CHAT_ARCHIVED));

            localBroadcastManager.registerReceiver(refreshAddPhoneNumberButtonReceiver,
                    new IntentFilter(BROADCAST_ACTION_INTENT_REFRESH_ADD_PHONE_NUMBER));

			IntentFilter filterCall = new IntentFilter(BROADCAST_ACTION_INTENT_CALL_UPDATE);
			filterCall.addAction(ACTION_CALL_STATUS_UPDATE);
			localBroadcastManager.registerReceiver(chatCallUpdateReceiver, filterCall);
		}
        registerReceiver(cameraUploadLauncherReceiver, new IntentFilter(Intent.ACTION_POWER_CONNECTED));

        IntentFilter filter = new IntentFilter(BROADCAST_ACTION_INTENT_SETTINGS_UPDATED);
        filter.addAction(ACTION_REFRESH_CAMERA_UPLOADS_SETTING);
        filter.addAction(ACTION_REFRESH_CAMERA_UPLOADS_MEDIA_SETTING);
        filter.addAction(ACTION_REFRESH_CLEAR_OFFLINE_SETTING);
        registerReceiver(updateCUSettingsReceiver, filter);

        smsDialogTimeChecker = new LastShowSMSDialogTimeChecker(this);
        nC = new NodeController(this);
		cC = new ContactController(this);
		aC = new AccountController(this);

        createCacheFolders(this);

        dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		managerActivity = this;
		app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();

		megaChatApi = app.getMegaChatApi();
		logDebug("addChatListener");
		megaChatApi.addChatListener(this);

		if (megaChatApi != null){
			logDebug("retryChatPendingConnections()");
			megaChatApi.retryPendingConnections(false, null);
		}

		transfersInProgress = new ArrayList<Integer>();

		//sync local contacts to see who's on mega.
		if (hasPermissions(this, Manifest.permission.READ_CONTACTS)) {
		    logDebug("sync mega contacts");
			MegaContactGetter getter = new MegaContactGetter(this);
			getter.getMegaContacts(megaApi, MegaContactGetter.WEEK);
		}

		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;

	    float scaleW = getScaleW(outMetrics, density);
	    float scaleH = getScaleH(outMetrics, density);
	    if (scaleH < scaleW){
	    	scaleText = scaleH;
	    }
	    else{
	    	scaleText = scaleW;
	    }

	    if (dbH.getEphemeral() != null){
            Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
            intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
		}

	    if (dbH.getCredentials() == null){
	    	Intent newIntent = getIntent();

	    	if (newIntent != null){
		    	if (newIntent.getAction() != null){
		    		if (newIntent.getAction().equals(ACTION_EXPORT_MASTER_KEY) || newIntent.getAction().equals(ACTION_OPEN_MEGA_LINK) || newIntent.getAction().equals(ACTION_OPEN_MEGA_FOLDER_LINK)){
		    			openLink = true;
		    		}
		    		else if (newIntent.getAction().equals(ACTION_CANCEL_CAM_SYNC)){
                        stopRunningCameraUploadService(getApplicationContext());
		    			finish();
		    			return;
		    		}
		    	}
		    }

	    	if (!openLink){
//				megaApi.localLogout();
//				AccountController aC = new AccountController(this);
//				aC.logout(this, megaApi, megaChatApi, false);
				Intent intent = new Intent(this, LoginActivityLollipop.class);
				intent.putExtra(VISIBLE_FRAGMENT,  TOUR_FRAGMENT);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
					startActivity(intent);
					finish();
				}

		    }

	    	return;
	    }

	    prefs = dbH.getPreferences();
		if (prefs == null){
			firstTimeAfterInstallation = true;
			isList=true;
			isListCameraUploads=false;
			isSmallGridCameraUploads = false;
		}
		else{

			if (prefs.getFirstTime() == null){
				firstTimeAfterInstallation = true;
				isListCameraUploads=false;
			}else{
				firstTimeAfterInstallation = Boolean.parseBoolean(prefs.getFirstTime());
			}
			if (prefs.getPreferredViewList() == null){
				isList = true;
			}
			else{
				isList = Boolean.parseBoolean(prefs.getPreferredViewList());
			}
			if (prefs.getPreferredViewListCameraUploads() == null){
				isListCameraUploads = false;
			}
			else{
				isListCameraUploads = Boolean.parseBoolean(prefs.getPreferredViewListCameraUploads());
			}

			isSmallGridCameraUploads = dbH.isSmallGridCamera();
		}
		logDebug("Preferred View List: " + isList);
		logDebug("Preferred View List for camera uploads: " + isListCameraUploads);

		if(prefs!=null){
			if(prefs.getPreferredSortCloud()!=null){
				orderCloud = Integer.parseInt(prefs.getPreferredSortCloud());
				logDebug("The orderCloud preference is: " + orderCloud);
			}
			else{
				orderCloud = ORDER_DEFAULT_ASC;
				logDebug("Preference orderCloud is NULL -> ORDER_DEFAULT_ASC");
			}
			if(prefs.getPreferredSortContacts()!=null){
				orderContacts = Integer.parseInt(prefs.getPreferredSortContacts());
				logDebug("The orderContacts preference is: " + orderContacts);
			}
			else{
				orderContacts = ORDER_DEFAULT_ASC;
				logDebug("Preference orderContacts is NULL -> ORDER_DEFAULT_ASC");
			}
            if (prefs.getPreferredSortCameraUpload() != null) {
                orderCamera = Integer.parseInt(prefs.getPreferredSortCameraUpload());
                logDebug("The orderCamera preference is: " + orderCamera);
            } else {
                logDebug("Preference orderCamera is NULL -> ORDER_MODIFICATION_DESC");
            }
			if(prefs.getPreferredSortOthers()!=null){
				orderOthers = Integer.parseInt(prefs.getPreferredSortOthers());
				logDebug("The orderOthers preference is: " + orderOthers);
			}
			else{
				orderOthers = ORDER_DEFAULT_ASC;
				logDebug("Preference orderOthers is NULL -> ORDER_DEFAULT_ASC");
			}
		}
		else {
			logDebug("Prefs is NULL -> ORDER_DEFAULT_ASC");
			orderCloud = ORDER_DEFAULT_ASC;
			orderContacts = ORDER_DEFAULT_ASC;
			orderOthers = ORDER_DEFAULT_ASC;
		}

		handler = new Handler();

		logDebug("Set view");
		setContentView(R.layout.activity_manager);
//		long num = 11179220468180L;
//		dbH.setSecondaryFolderHandle(num);
		//Set toolbar
		abL = (AppBarLayout) findViewById(R.id.app_bar_layout);

		tB = (Toolbar) findViewById(R.id.toolbar);
		if(tB==null){
			logWarning("Tb is Null");
			return;
		}

		tB.setVisibility(View.VISIBLE);
		setSupportActionBar(tB);
		aB = getSupportActionBar();

		aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

        fragmentLayout = (LinearLayout) findViewById(R.id.fragment_layout);

        bNV = (BottomNavigationViewEx) findViewById(R.id.bottom_navigation_view);
		bNV.setOnNavigationItemSelectedListener(this);
		bNV.enableAnimation(false);
		bNV.enableItemShiftingMode(false);
		bNV.enableShiftingMode(false);
		bNV.setTextVisibility(false);

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
				if (!isOnline(managerActivity) || megaApi==null || megaApi.getRootNode()==null) {
					disableNavigationViewLayout();
				}
				else {
					resetNavigationViewLayout();
				}

				setContactStatus();

				if (!refreshStorageInfo) return;
                showAddPhoneNumberInMenu();
				refreshAccountInfo();
			}
		});
        nV = (NavigationView) findViewById(R.id.navigation_view);

		myAccountHeader = (RelativeLayout) findViewById(R.id.navigation_drawer_account_section);
		myAccountHeader.setOnClickListener(this);
		contactStatus = (ImageView) findViewById(R.id.contact_state);
        myAccountSection = (RelativeLayout) findViewById(R.id.my_account_section);
        myAccountSection.setOnClickListener(this);
        inboxSection = (RelativeLayout) findViewById(R.id.inbox_section);
        inboxSection.setOnClickListener(this);
        contactsSection = (RelativeLayout) findViewById(R.id.contacts_section);
        contactsSection.setOnClickListener(this);
		notificationsSection = (RelativeLayout) findViewById(R.id.notifications_section);
		notificationsSection.setOnClickListener(this);
		notificationsSectionText = (TextView) findViewById(R.id.notification_section_text);
        contactsSectionText = (TextView) findViewById(R.id.contacts_section_text);
        settingsSection = (RelativeLayout) findViewById(R.id.settings_section);
        settingsSection.setOnClickListener(this);
        upgradeAccount = (Button) findViewById(R.id.upgrade_navigation_view);
        upgradeAccount.setBackground(ContextCompat.getDrawable(this, R.drawable.background_button_white));
        upgradeAccount.setOnClickListener(this);

        navigationDrawerAddPhoneContainer = findViewById(R.id.navigation_drawer_add_phone_number_container);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) navigationDrawerAddPhoneContainer.getLayoutParams();
        if (isScreenInPortrait(this)) {
        	params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        	params.removeRule(RelativeLayout.BELOW);
		} else {
        	params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			params.addRule(RelativeLayout.BELOW, R.id.sections_layout);
		}
        navigationDrawerAddPhoneContainer.setLayoutParams(params);

        addPhoneNumberButton = (TextView)findViewById(R.id.navigation_drawer_add_phone_number_button);
        addPhoneNumberButton.setOnClickListener(this);

        addPhoneNumberLabel = findViewById(R.id.navigation_drawer_add_phone_number_label);
        megaApi.getAccountAchievements(this);

//		badgeDrawable = new BadgeDrawerArrowDrawable(getSupportActionBar().getThemedContext());
		badgeDrawable = new BadgeDrawerArrowDrawable(managerActivity);
		BottomNavigationMenuView menuView = (BottomNavigationMenuView) bNV.getChildAt(0);
		BottomNavigationItemView itemView = (BottomNavigationItemView) menuView.getChildAt(2);
		chatBadge = LayoutInflater.from(this).inflate(R.layout.bottom_chat_badge, menuView, false);
		itemView.addView(chatBadge);
		setChatBadge();

		callBadge = LayoutInflater.from(this).inflate(R.layout.bottom_call_badge, menuView, false);
		itemView.addView(callBadge);
		callBadge.setVisibility(View.GONE);
		setCallBadge();

		usedSpaceLayout = (RelativeLayout) findViewById(R.id.nv_used_space_layout);

		//FAB buttonaB.
		fabButton = (FloatingActionButton) findViewById(R.id.floating_button);
		fabButton.setOnClickListener(new FabButtonListener(this));

		//Collection of FAB for CHAT
		fabButtonsLayout = (CoordinatorLayout) findViewById(R.id.fab_collection_layout);
		mainFabButtonChat = (FloatingActionButton) findViewById(R.id.main_fab_chat);
		mainFabButtonChat.setOnClickListener(new FabButtonListener(this));
		firstFabButtonChat = (FloatingActionButton) findViewById(R.id.first_fab_chat);
		firstFabButtonChat.setOnClickListener(new FabButtonListener(this));
		secondFabButtonChat = (FloatingActionButton) findViewById(R.id.second_fab_chat);
		secondFabButtonChat.setOnClickListener(new FabButtonListener(this));
		thirdFabButtonChat = (FloatingActionButton) findViewById(R.id.third_fab_chat);
		thirdFabButtonChat.setOnClickListener(new FabButtonListener(this));

		collectionFABLayoutOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.collection_fab_layout_out);
		collectionFABLayoutOut.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				logDebug("onAnimationEnd");
				fabButtonsLayout.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});
		openFabAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.open_fab);
		closeFabAnim = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.close_fab);
		closeFabAnim.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				logDebug("onAnimationEnd");
//				mainFabButtonChat.setVisibility(View.GONE);
//				fabButtonsLayout.startAnimation(collectionFABLayoutOut);
				fabButtonsLayout.setVisibility(View.GONE);
				fabButton.show();
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});

		rotateRightAnim = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_right);
		rotateLeftAnim = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_left);

		//PRO PANEL
		getProLayout=(LinearLayout) findViewById(R.id.get_pro_account);
		String getProTextString = getString(R.string.get_pro_account);
		try {
			getProTextString = getProTextString.replace("[A]", "\n");
		}
		catch(Exception e){
			logError("Formatted string: " + getProTextString, e);
		}

		getProText= (TextView) findViewById(R.id.get_pro_account_text);
		getProText.setText(getProTextString);
		rightUpgradeButton = (TextView) findViewById(R.id.btnRight_upgrade);
		leftCancelButton = (TextView) findViewById(R.id.btnLeft_cancel);

        nVDisplayName = findViewById(R.id.navigation_drawer_account_information_display_name);
        nVDisplayName.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));

		nVEmail = (TextView) findViewById(R.id.navigation_drawer_account_information_email);
        nVPictureProfile = (RoundedImageView) findViewById(R.id.navigation_drawer_user_account_picture_profile);

		businessLabel = findViewById(R.id.business_label);
		businessLabel.setVisibility(View.GONE);

        fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container);
        spaceTV = (TextView) findViewById(R.id.navigation_drawer_space);
        usedSpacePB = (ProgressBar) findViewById(R.id.manager_used_space_bar);
        //TABS section Contacts
		tabLayoutContacts =  (TabLayout) findViewById(R.id.sliding_tabs_contacts);
		viewPagerContacts = (ViewPager) findViewById(R.id.contact_tabs_pager);
		viewPagerContacts.setOffscreenPageLimit(3);

		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			tabLayoutContacts.setTabMode(TabLayout.MODE_FIXED);
		}
		else {
			tabLayoutContacts.setTabMode(TabLayout.MODE_SCROLLABLE);
		}

		//TABS section Cloud Drive
		tabLayoutCloud = (TabLayout) findViewById(R.id.sliding_tabs_cloud);
		viewPagerCloud = (CustomViewPager) findViewById(R.id.cloud_tabs_pager);
		viewPagerCloud.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

			}

			@Override
			public void onPageSelected(int position) {
				supportInvalidateOptionsMenu();
				checkScrollElevation();
				if(position == 1 && isCloudAdded() && fbFLol.isMultipleselect()){
					fbFLol.actionMode.finish();
				}
				setToolbarTitle();
				showFabButton();
			}

			@Override
			public void onPageScrollStateChanged(int state) {

			}
		});

		//TABS section Shared Items
		tabLayoutShares =  (TabLayout) findViewById(R.id.sliding_tabs_shares);
		viewPagerShares = (ViewPager) findViewById(R.id.shares_tabs_pager);
		viewPagerShares.setOffscreenPageLimit(3);

		viewPagerShares.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			@Override
			public void onPageSelected(int position) {
				logDebug("selectDrawerItemSharedItems - TabId: " +  position);
				supportInvalidateOptionsMenu();
				checkScrollElevation();
				setSharesTabIcons(position);
				switch (position) {
					case INCOMING_TAB:
						if (isOutgoingAdded() && outSFLol.isMultipleSelect()) {
							outSFLol.getActionMode().finish();
						} else if (isLinksAdded() && lF.isMultipleSelect()) {
							lF.getActionMode().finish();
						}
						break;
					case OUTGOING_TAB:
						if (isIncomingAdded() && inSFLol.isMultipleSelect()) {
							inSFLol.getActionMode().finish();
						}  else if (isLinksAdded() && lF.isMultipleSelect()) {
							lF.getActionMode().finish();
						}
						break;
					case LINKS_TAB:
						if (isIncomingAdded() && inSFLol.isMultipleSelect()) {
							inSFLol.getActionMode().finish();
						} else if (isOutgoingAdded() && outSFLol.isMultipleSelect()) {
							outSFLol.getActionMode().finish();
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

		//Tab section MyAccount
		tabLayoutMyAccount =  (TabLayout) findViewById(R.id.sliding_tabs_my_account);
		viewPagerMyAccount = (ViewPager) findViewById(R.id.my_account_tabs_pager);

		//Tab section Transfers
		tabLayoutTransfers =  (TabLayout) findViewById(R.id.sliding_tabs_transfers);
		viewPagerTransfers = (ViewPager) findViewById(R.id.transfers_tabs_pager);

		callInProgressLayout = findViewById(R.id.call_in_progress_layout);
		callInProgressLayout.setOnClickListener(this);
		callInProgressChrono = findViewById(R.id.call_in_progress_chrono);
		callInProgressText = findViewById(R.id.call_in_progress_text);
		callInProgressLayout.setVisibility(View.GONE);

        if (!isOnline(this)){
			logDebug("No network -> SHOW OFFLINE MODE");

			if(drawerItem==null){
				drawerItem = DrawerItem.SAVED_FOR_OFFLINE;
			}

			selectDrawerItemLollipop(drawerItem);

			showOfflineMode();

			UserCredentials credentials = dbH.getCredentials();
			if (credentials != null) {
				String gSession = credentials.getSession();
				int ret = megaChatApi.getInitState();
				logDebug("In Offline mode - Init chat is: " + ret);
				if (ret == 0 || ret == MegaChatApi.INIT_ERROR) {
					ret = megaChatApi.init(gSession);
					logDebug("After init: " + ret);
					if (ret == MegaChatApi.INIT_NO_CACHE) {
						logDebug("condition ret == MegaChatApi.INIT_NO_CACHE");
					} else if (ret == MegaChatApi.INIT_ERROR) {
						logWarning("condition ret == MegaChatApi.INIT_ERROR");
					} else {
						logDebug("Chat correctly initialized");
					}
				} else {
					logDebug("Offline mode: Do not init, chat already initialized");
				}
			}

			return;
        }

		transfersOverViewLayout = findViewById(R.id.transfers_overview_item_layout);
		transfersOverViewLayout.setOnClickListener(this);
		transfersTextLayout = findViewById(R.id.transfers_text_layout);
		transfersTitleText = findViewById(R.id.transfers_overview_title);
		transfersNumberText = findViewById(R.id.transfers_overview_number);

		playButton = findViewById(R.id.transfers_overview_button);
		actionLayout = findViewById(R.id.transfers_overview_action_layout);
		actionLayout.setOnClickListener(this);
		dotsOptionsTransfersLayout = findViewById(R.id.transfers_overview_three_dots_layout);
		dotsOptionsTransfersLayout.setOnClickListener(this);
		progressBarTransfers = findViewById(R.id.transfers_overview_progress_bar);

		putTransfersWidget();

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
		if (rootNode == null || LoginActivityLollipop.isBackFromLoginPage){
			 if (getIntent() != null){
				 logDebug("Action: " + getIntent().getAction());
				if (getIntent().getAction() != null){
					if (getIntent().getAction().equals(ACTION_IMPORT_LINK_FETCH_NODES)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ACTION_IMPORT_LINK_FETCH_NODES);
						intent.setData(Uri.parse(getIntent().getDataString()));
						startActivity(intent);
						finish();
						return;
					}
					else if (getIntent().getAction().equals(ACTION_OPEN_MEGA_LINK)){
						Intent intent = new Intent(managerActivity, FileLinkActivityLollipop.class);
						intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ACTION_IMPORT_LINK_FETCH_NODES);
						intent.setData(Uri.parse(getIntent().getDataString()));
						startActivity(intent);
						finish();
						return;
					}
					else if (getIntent().getAction().equals(ACTION_OPEN_MEGA_FOLDER_LINK)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ACTION_OPEN_MEGA_FOLDER_LINK);
						intent.setData(Uri.parse(getIntent().getDataString()));
						startActivity(intent);
						finish();
						return;
					}
					else if(getIntent().getAction().equals(ACTION_OPEN_CHAT_LINK)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ACTION_OPEN_CHAT_LINK);
						intent.setData(Uri.parse(getIntent().getDataString()));
						startActivity(intent);
						finish();
						return;
					}
					else if (getIntent().getAction().equals(ACTION_CANCEL_CAM_SYNC)){
                        stopRunningCameraUploadService(getApplicationContext());
						finish();
						return;
					}
					else if (getIntent().getAction().equals(ACTION_EXPORT_MASTER_KEY)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(getIntent().getAction());
						startActivity(intent);
						finish();
						return;
					}
					else if (getIntent().getAction().equals(ACTION_SHOW_TRANSFERS)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ACTION_SHOW_TRANSFERS);
						intent.putExtra(TRANSFERS_TAB, getIntent().getIntExtra(TRANSFERS_TAB, ERROR_TAB));
						startActivity(intent);
						finish();
						return;
					}
					else if (getIntent().getAction().equals(ACTION_IPC)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ACTION_IPC);
						startActivity(intent);
						finish();
						return;
					}
					else if (getIntent().getAction().equals(ACTION_CHAT_NOTIFICATION_MESSAGE)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ACTION_CHAT_NOTIFICATION_MESSAGE);
						startActivity(intent);
						finish();
						return;
					}
                    else if(getIntent().getAction().equals(ACTION_CHAT_SUMMARY)) {
                        Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
                        intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setAction(ACTION_CHAT_SUMMARY);
                        startActivity(intent);
                        finish();
                        return;
                    }
					else if (getIntent().getAction().equals(ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION);
						startActivity(intent);
						finish();
						return;
					}
					else if (getIntent().getAction().equals(ACTION_OPEN_HANDLE_NODE)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ACTION_OPEN_HANDLE_NODE);
						intent.setData(Uri.parse(getIntent().getDataString()));
						startActivity(intent);
						finish();
						return;
					}
					else if (getIntent().getAction().equals(ACTION_OVERQUOTA_TRANSFER)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ACTION_OVERQUOTA_TRANSFER);
						startActivity(intent);
						finish();
						return;
					}
					else if (getIntent().getAction().equals(ACTION_OVERQUOTA_STORAGE)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ACTION_OVERQUOTA_STORAGE);
						startActivity(intent);
						finish();
						return;
					}
					else if (getIntent().getAction().equals(ACTION_OPEN_CONTACTS_SECTION)){
						logDebug("Login");
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra(CONTACT_HANDLE, getIntent().getLongExtra(CONTACT_HANDLE, -1));
						intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ACTION_OPEN_CONTACTS_SECTION);
						startActivity(intent);
						finish();
						return;
					}
					else if (getIntent().getAction().equals(ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE);
						startActivity(intent);
						finish();
						return;
					}
				}
			}
			Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
			intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}
		else{
			inboxNode = megaApi.getInboxNode();
			attr = dbH.getAttributes();
			if (attr != null){
				if (attr.getInvalidateSdkCache() != null){
					if (attr.getInvalidateSdkCache().compareTo("") != 0) {
						try {
							if (Boolean.parseBoolean(attr.getInvalidateSdkCache())){
								logDebug("megaApi.invalidateCache();");
								megaApi.invalidateCache();
							}
						}
						catch(Exception e){}
					}
				}
			}

			dbH.setInvalidateSdkCache(false);

			String token = FirebaseInstanceId.getInstance().getToken();
			if (token != null) {
				logDebug("FCM TOKEN: " + token);
				megaApi.registerPushNotifications(DEVICE_ANDROID, token, this);
//				Log.d("TOKEN___", token);

//				Toast.makeText(this, "TOKEN: _" + token + "_", Toast.LENGTH_LONG).show();
			}


			nVEmail.setVisibility(View.VISIBLE);
			nVEmail.setText(megaApi.getMyEmail());
//				megaApi.getUserData(this);
			megaApi.getUserAttribute(MegaApiJava.USER_ATTR_FIRSTNAME, this);
			megaApi.getUserAttribute(MegaApiJava.USER_ATTR_LASTNAME, this);

			this.setDefaultAvatar();

			this.setProfileAvatar();

			initGooglePlayPayments();

			megaApi.addGlobalListener(this);

			megaApi.shouldShowRichLinkWarning(this);
			megaApi.isRichPreviewsEnabled(this);
			megaApi.isGeolocationEnabled(this);

			transferData = megaApi.getTransferData(this);
			int downloadsInProgress = transferData.getNumDownloads();
			int uploadsInProgress = transferData.getNumUploads();

            for(int i=0;i<downloadsInProgress;i++){
                transfersInProgress.add(transferData.getDownloadTag(i));
            }
            for(int i=0;i<uploadsInProgress;i++){
                transfersInProgress.add(transferData.getUploadTag(i));
            }

			if(savedInstanceState==null) {
				logDebug("Run async task to check offline files");
				//Check the consistency of the offline nodes in the DB
				CheckOfflineNodesTask checkOfflineNodesTask = new CheckOfflineNodesTask(this);
				checkOfflineNodesTask.execute();
			}

	        if (getIntent() != null){
				if (getIntent().getAction() != null){
			        if (getIntent().getAction().equals(ACTION_EXPORT_MASTER_KEY)){
						logDebug("Intent to export Master Key - im logged in!");
						drawerItem=DrawerItem.ACCOUNT;
						showMKLayout();
						selectDrawerItemLollipop(drawerItem);
						selectDrawerItemPending=false;
						return;
					}
					else if(getIntent().getAction().equals(ACTION_CANCEL_ACCOUNT)){
						String link = getIntent().getDataString();
						if(link!=null){
							logDebug("Link to cancel: " + link);
							drawerItem=DrawerItem.ACCOUNT;
							selectDrawerItemLollipop(drawerItem);
							selectDrawerItemPending=false;
							megaApi.queryCancelLink(link, this);
						}
					}
					else if(getIntent().getAction().equals(ACTION_CHANGE_MAIL)){
						String link = getIntent().getDataString();
						if(link!=null){
							logDebug("Link to change mail: " + link);
							drawerItem=DrawerItem.ACCOUNT;
							selectDrawerItemLollipop(drawerItem);
							selectDrawerItemPending=false;
							showDialogInsertPassword(link, false);
						}
					}
					else if (getIntent().getAction().equals(ACTION_OPEN_FOLDER)) {
						logDebug("Open after LauncherFileExplorerActivityLollipop ");
						boolean locationFileInfo = getIntent().getBooleanExtra("locationFileInfo", false);
						long handleIntent = getIntent().getLongExtra("PARENT_HANDLE", -1);

						if (getIntent().getBooleanExtra(SHOW_MESSAGE_UPLOAD_STARTED, false)) {
							int numberUploads = getIntent().getIntExtra(NUMBER_UPLOADS, 1);
							showSnackbar(SNACKBAR_TYPE, getResources().getQuantityString(R.plurals.upload_began, numberUploads, numberUploads), -1);
						}

						if (locationFileInfo){
							boolean offlineAdapter = getIntent().getBooleanExtra("offline_adapter", false);
							if (offlineAdapter){
								drawerItem = DrawerItem.SAVED_FOR_OFFLINE;
								String pathNavigation = getIntent().getStringExtra("pathNavigation");
								setPathNavigationOffline(pathNavigation);
								selectDrawerItemLollipop(drawerItem);
								selectDrawerItemPending=false;
							}
							else {
								long fragmentHandle = getIntent().getLongExtra("fragmentHandle", -1);

								if (fragmentHandle == megaApi.getRootNode().getHandle()){
									drawerItem = DrawerItem.CLOUD_DRIVE;
									setParentHandleBrowser(handleIntent);
									selectDrawerItemLollipop(drawerItem);
									selectDrawerItemPending=false;
								}
								else if (fragmentHandle == megaApi.getRubbishNode().getHandle()){
									drawerItem = DrawerItem.RUBBISH_BIN;
									setParentHandleRubbish(handleIntent);
									selectDrawerItemLollipop(drawerItem);
									selectDrawerItemPending=false;
								}
								else if (fragmentHandle == megaApi.getInboxNode().getHandle()){
									drawerItem = DrawerItem.INBOX;
									setParentHandleInbox(handleIntent);
									selectDrawerItemLollipop(drawerItem);
									selectDrawerItemPending=false;
								}
								else {
									//Incoming
									drawerItem = DrawerItem.SHARED_ITEMS;
									indexShares = 0;
									MegaNode parentIntentN = megaApi.getNodeByHandle(handleIntent);
									if (parentIntentN != null){
										deepBrowserTreeIncoming = calculateDeepBrowserTreeIncoming(parentIntentN, this);
									}
									setParentHandleIncoming(handleIntent);
									selectDrawerItemLollipop(drawerItem);
									selectDrawerItemPending=false;
								}
							}
						}
						else {
							actionOpenFolder(handleIntent);
						}
					}
					else if(getIntent().getAction().equals(ACTION_PASS_CHANGED)){
						int result = getIntent().getIntExtra("RESULT",-20);
						if(result==0){
							drawerItem=DrawerItem.ACCOUNT;
							selectDrawerItemLollipop(drawerItem);
							selectDrawerItemPending=false;
							logDebug("Show success mesage");
							showAlert(this, getString(R.string.pass_changed_alert), null);
						}
						else if(result==MegaError.API_EARGS){
							drawerItem=DrawerItem.ACCOUNT;
							selectDrawerItemLollipop(drawerItem);
							selectDrawerItemPending=false;
							logWarning("Error when changing pass - the current password is not correct");
							showAlert(this,getString(R.string.old_password_provided_incorrect), getString(R.string.general_error_word));
						}
						else{
							drawerItem=DrawerItem.ACCOUNT;
							selectDrawerItemLollipop(drawerItem);
							selectDrawerItemPending=false;
							logError("Error when changing pass - show error message");
							showAlert(this,getString(R.string.general_text_error), getString(R.string.general_error_word));
						}
					}
					else if(getIntent().getAction().equals(ACTION_RESET_PASS)){
						String link = getIntent().getDataString();
						if(link!=null){
							logDebug("Link to resetPass: " + link);
							drawerItem=DrawerItem.ACCOUNT;
							selectDrawerItemLollipop(drawerItem);
							selectDrawerItemPending=false;
							showConfirmationResetPassword(link);
						}
					}
					else if(getIntent().getAction().equals(ACTION_IPC)){
						logDebug("IPC link - go to received request in Contacts");
						markNotificationsSeen(true);
						drawerItem=DrawerItem.CONTACTS;
						indexContacts=2;
						selectDrawerItemLollipop(drawerItem);
						selectDrawerItemPending=false;
					}
					else if(getIntent().getAction().equals(ACTION_CHAT_NOTIFICATION_MESSAGE)){
						logDebug("Chat notitificacion received");
						drawerItem=DrawerItem.CHAT;
						selectDrawerItemLollipop(drawerItem);
						long chatId = getIntent().getLongExtra("CHAT_ID", -1);
						if (getIntent().getBooleanExtra("moveToChatSection", false)){
							moveToChatSection(chatId);
						}
						else {
							String text = getIntent().getStringExtra("showSnackbar");
							if (chatId != -1) {
								openChat(chatId, text);
							}
						}
						selectDrawerItemPending=false;
						getIntent().setAction(null);
						setIntent(null);
					}
					else if(getIntent().getAction().equals(ACTION_CHAT_SUMMARY)) {
						logDebug("Chat notification: ACTION_CHAT_SUMMARY");
						drawerItem=DrawerItem.CHAT;
						selectDrawerItemLollipop(drawerItem);
						selectDrawerItemPending=false;
						getIntent().setAction(null);
						setIntent(null);
					}
					else if(getIntent().getAction().equals(ACTION_OPEN_CHAT_LINK)){
						logDebug("ACTION_OPEN_CHAT_LINK: " + getIntent().getDataString());
						drawerItem=DrawerItem.CHAT;
						selectDrawerItemLollipop(drawerItem);
						selectDrawerItemPending=false;
						megaChatApi.checkChatLink(getIntent().getDataString(), this);
						getIntent().setAction(null);
						setIntent(null);
					}
					else if (getIntent().getAction().equals(ACTION_JOIN_OPEN_CHAT_LINK)) {
						drawerItem=DrawerItem.CHAT;
						selectDrawerItemLollipop(drawerItem);
						selectDrawerItemPending = false;

						megaChatApi.checkChatLink(getIntent().getDataString(), this);
						idJoinToChatLink = getIntent().getLongExtra("idChatToJoin", -1);
						joiningToChatLink = true;
						if (idJoinToChatLink == -1) {
							showSnackbar(SNACKBAR_TYPE, getString(R.string.error_chat_link_init_error), -1);
						}

						getIntent().setAction(null);
						setIntent(null);
					}
					else if(getIntent().getAction().equals(ACTION_SHOW_SETTINGS)) {
						logDebug("Chat notification: SHOW_SETTINGS");
						selectDrawerItemPending=false;
						moveToSettingsSection();
						getIntent().setAction(null);
						setIntent(null);
					}
					else if (getIntent().getAction().equals(ACTION_SHOW_SETTINGS_STORAGE)) {
						logDebug("ACTION_SHOW_SETTINGS_STORAGE");
						selectDrawerItemPending=false;
						moveToSettingsSectionStorage();
						getIntent().setAction(null);
						setIntent(null);
					}
					else if(getIntent().getAction().equals(ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION)){
						logDebug("ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION");
						markNotificationsSeen(true);

						drawerItem=DrawerItem.SHARED_ITEMS;
						indexShares=0;
						selectDrawerItemLollipop(drawerItem);
						selectDrawerItemPending=false;
					}
					else if(getIntent().getAction().equals(ACTION_SHOW_MY_ACCOUNT)){
						logDebug("Intent from chat - show my account");

						drawerItem=DrawerItem.ACCOUNT;
						accountFragment=MY_ACCOUNT_FRAGMENT;
						selectDrawerItemLollipop(drawerItem);
						selectDrawerItemPending=false;
					}
					else if(getIntent().getAction().equals(ACTION_SHOW_UPGRADE_ACCOUNT)){
						logDebug("Intent from chat - show my account");
						drawerItemPreUpgradeAccount = drawerItem;
						drawerItem=DrawerItem.ACCOUNT;
						accountFragment=UPGRADE_ACCOUNT_FRAGMENT;
						selectDrawerItemLollipop(drawerItem);
						selectDrawerItemPending=false;
					}
					else if(getIntent().getAction().equals(ACTION_OVERQUOTA_TRANSFER)){
						logDebug("Intent overquota transfer alert!!");
						if(alertDialogTransferOverquota==null){
							showTransferOverquotaDialog();
						}
						else{
							if(!(alertDialogTransferOverquota.isShowing())){
								showTransferOverquotaDialog();
							}
						}
					}
					else if (getIntent().getAction().equals(ACTION_OPEN_HANDLE_NODE)){
						String link = getIntent().getDataString();
						String [] s = link.split("#");
						if (s.length > 1){
							String nodeHandleLink = s[1];
							String [] sSlash = s[1].split("/");
							if (sSlash.length > 0){
								nodeHandleLink = sSlash[0];
							}
							long nodeHandleLinkLong = MegaApiAndroid.base64ToHandle(nodeHandleLink);
							MegaNode nodeLink = megaApi.getNodeByHandle(nodeHandleLinkLong);
							if (nodeLink == null){
								showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error_file_not_found), -1);
							}
							else{
								MegaNode pN = megaApi.getParentNode(nodeLink);
								if (pN == null){
									pN = megaApi.getRootNode();
								}
								parentHandleBrowser = pN.getHandle();
								drawerItem = DrawerItem.CLOUD_DRIVE;
								selectDrawerItemLollipop(drawerItem);
								selectDrawerItemPending = false;

								Intent i = new Intent(this, FileInfoActivityLollipop.class);
								i.putExtra("handle", nodeLink.getHandle());
								i.putExtra(NAME, nodeLink.getName());
								startActivity(i);
							}
						}
						else{
							drawerItem = DrawerItem.CLOUD_DRIVE;
							selectDrawerItemLollipop(drawerItem);
						}
					}
					else if (getIntent().getAction().equals(ACTION_IMPORT_LINK_FETCH_NODES)){
						getIntent().setAction(null);
						setIntent(null);
					}
					else if (getIntent().getAction().equals(ACTION_OPEN_CONTACTS_SECTION)){
						markNotificationsSeen(true);
						openContactLink(getIntent().getLongExtra(CONTACT_HANDLE, -1));
					}
					else if (getIntent().getAction().equals(ACTION_REFRESH_STAGING)){
						update2FASetting();
					}
					else if(getIntent().getAction().equals(ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE)){
						long chatId = getIntent().getLongExtra("CHAT_ID", -1);
						showSnackbar(MESSAGE_SNACKBAR_TYPE, null, chatId);
						getIntent().setAction(null);
						setIntent(null);
					}
				}
	        }

			logDebug("Check if there any unread chat");
			if (megaChatApi != null) {
				logDebug("Connect to chat!: " + megaChatApi.getInitState());
				if ((megaChatApi.getInitState() != MegaChatApi.INIT_ERROR)) {
					logDebug("Connection goes!!!");
					megaChatApi.connect(this);
				} else {
					logWarning("Not launch connect: " + megaChatApi.getInitState());
				}
			} else {
				logError("megaChatApi is NULL");
			}
			setChatBadge();

			logDebug("Check if there any INCOMING pendingRequest contacts");
			setContactTitleSection();

			setNotificationsTitleSection();

			if (drawerItem == null) {
	        	drawerItem = DrawerItem.CLOUD_DRIVE;
	        	Intent intent = getIntent();
	        	if (intent != null){
	        		boolean upgradeAccount = getIntent().getBooleanExtra("upgradeAccount", false);
					newAccount = getIntent().getBooleanExtra("newAccount", false);
					newCreationAccount = getIntent().getBooleanExtra(NEW_CREATION_ACCOUNT, false);

                    //reset flag to fix incorrect view loaded when orientation changes
                    getIntent().removeExtra("newAccount");
                    getIntent().removeExtra("upgradeAccount");
	        		if(upgradeAccount){
	        			drawerLayout.closeDrawer(Gravity.LEFT);
						int accountType = getIntent().getIntExtra("accountType", 0);

						switch (accountType){
							case 0:{
								logDebug("Intent firstTimeAfterInstallation==true");
								firstLogin = true;
								drawerItem = DrawerItem.CAMERA_UPLOADS;
								setIntent(null);
								displayedAccountType = -1;
								return;
							}
							case PRO_I:{
								drawerItem = DrawerItem.ACCOUNT;
								accountFragment = UPGRADE_ACCOUNT_FRAGMENT;
								displayedAccountType = PRO_I;
								selectDrawerItemLollipop(drawerItem);
								selectDrawerItemPending=false;
								return;
							}
							case PRO_II:{
								drawerItem = DrawerItem.ACCOUNT;
								accountFragment = UPGRADE_ACCOUNT_FRAGMENT;
								selectDrawerItemPending=false;
								displayedAccountType = PRO_II;
								selectDrawerItemLollipop(drawerItem);
								return;
							}
							case PRO_III:{
								drawerItem = DrawerItem.ACCOUNT;
								accountFragment = UPGRADE_ACCOUNT_FRAGMENT;
								selectDrawerItemPending=false;
								displayedAccountType = PRO_III;
								selectDrawerItemLollipop(drawerItem);
								return;
							}
							case PRO_LITE:{
								drawerItem = DrawerItem.ACCOUNT;
								accountFragment = UPGRADE_ACCOUNT_FRAGMENT;
								selectDrawerItemPending=false;
								displayedAccountType = PRO_LITE;
								selectDrawerItemLollipop(drawerItem);
								return;
							}
						}
	        		}
	        		else{
						firstLogin = getIntent().getBooleanExtra("firstLogin", firstLogin);
                        if (firstLogin){
							logDebug("intent firstLogin==true");
							drawerItem = DrawerItem.CAMERA_UPLOADS;
							setIntent(null);
						}
					}
	        	}
	        }
	        else{
				logDebug("DRAWERITEM NOT NULL: " + drawerItem);
				Intent intentRec = getIntent();
	        	if (intentRec != null){
					boolean upgradeAccount = getIntent().getBooleanExtra("upgradeAccount", false);
					newAccount = getIntent().getBooleanExtra("newAccount", false);
                    newCreationAccount = getIntent().getBooleanExtra(NEW_CREATION_ACCOUNT, false);
					//reset flag to fix incorrect view loaded when orientation changes
                    getIntent().removeExtra("newAccount");
                    getIntent().removeExtra("upgradeAccount");
					firstLogin = intentRec.getBooleanExtra("firstLogin", firstLogin);
                    if(upgradeAccount){
						drawerLayout.closeDrawer(Gravity.LEFT);
						int accountType = getIntent().getIntExtra("accountType", 0);

						switch (accountType){
							case FREE:{
								logDebug("Intent firstTimeAfterInstallation==true");

								firstLogin = true;
								drawerItem = DrawerItem.CAMERA_UPLOADS;
								displayedAccountType = -1;
								setIntent(null);
								return;
							}
							case PRO_I:{
								drawerItem = DrawerItem.ACCOUNT;
								accountFragment = UPGRADE_ACCOUNT_FRAGMENT;
								selectDrawerItemPending=false;
								displayedAccountType = PRO_I;
								selectDrawerItemLollipop(drawerItem);
								return;
							}
							case PRO_II:{
								drawerItem = DrawerItem.ACCOUNT;
								accountFragment = UPGRADE_ACCOUNT_FRAGMENT;
								selectDrawerItemPending=false;
								displayedAccountType = PRO_II;
								selectDrawerItemLollipop(drawerItem);
								return;
							}
							case PRO_III:{
								drawerItem = DrawerItem.ACCOUNT;
								accountFragment = UPGRADE_ACCOUNT_FRAGMENT;
								selectDrawerItemPending=false;
								displayedAccountType = PRO_III;
								selectDrawerItemLollipop(drawerItem);
								return;
							}
							case PRO_LITE:{
								drawerItem = DrawerItem.ACCOUNT;
								accountFragment = UPGRADE_ACCOUNT_FRAGMENT;
								selectDrawerItemPending=false;
								displayedAccountType = PRO_LITE;
								selectDrawerItemLollipop(drawerItem);
								return;
							}
						}
					}
					else{
						if (firstLogin && !joiningToChatLink) {
							logDebug("Intent firstTimeCam==true");
							if (prefs != null){
								if (prefs.getCamSyncEnabled() != null){
									firstLogin = false;
								}
								else{
									firstLogin = true;
									drawerItem = DrawerItem.CAMERA_UPLOADS;
								}
							}
							else{
								firstLogin = true;
								drawerItem = DrawerItem.CAMERA_UPLOADS;
							}
							setIntent(null);
						}
					}

	        		if (intentRec.getAction() != null){
	        			if (intentRec.getAction().equals(ACTION_SHOW_TRANSFERS)){
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

	        //INITIAL FRAGMENT
			if(selectDrawerItemPending){
				selectDrawerItemLollipop(drawerItem);
			}
		}
		megaApi.shouldShowPasswordReminderDialog(false, this);

		if (verify2FADialogIsShown){
			showVerifyPin2FA(verifyPin2FADialogType);
		}

		updateAccountDetailsVisibleInfo();

		setContactStatus();

        if (firstTimeAfterInstallation) {
            //haven't verified phone number
            if (canVoluntaryVerifyPhoneNumber() && !onAskingPermissionsFragment && !newCreationAccount) {
                askForSMSVerification();
            } else {
                askForAccess();
            }
        } else if (firstLogin && !newCreationAccount) {
            if (canVoluntaryVerifyPhoneNumber() && !onAskingPermissionsFragment) {
                askForSMSVerification();
            }
        }

		if (openLinkDialogIsShown) {
			showOpenLinkDialog();
			String text = savedInstanceState.getString("openLinkText", "");
			openLinkText.setText(text);
			openLinkText.setSelection(text.length());
			boolean openLinkDialogIsErrorShown = savedInstanceState.getBoolean("openLinkDialogIsErrorShown", false);
			if (openLinkDialogIsErrorShown) {
				openLink(text);
			}
		}

		if (mkLayoutVisible) {
			showMKLayout();
		}

		checkBusinessStatus();

		logDebug("END onCreate");
	}

    private void checkBusinessStatus() {
        if (isBusinessGraceAlertShown) {
            showBusinessGraceAlert();
            return;
        }

        if (isBusinessCUAlertShown) {
        	showBusinessCUAlert();
        	return;
		}

        MyAccountInfo myAccountInfo = ((MegaApplication) getApplication()).getMyAccountInfo();

        if (myAccountInfo != null && myAccountInfo.shouldShowBusinessAlert()) {
            int status = megaApi.getBusinessStatus();
            if (status == BUSINESS_STATUS_EXPIRED) {
                myAccountInfo.setShouldShowBusinessAlert(false);
                startActivity(new Intent(this, BusinessExpiredAlertActivity.class));
            } else if (megaApi.isMasterBusinessAccount() && status == BUSINESS_STATUS_GRACE_PERIOD) {
                myAccountInfo.setShouldShowBusinessAlert(false);
                showBusinessGraceAlert();
            }
        }
    }

    private void showBusinessGraceAlert() {
    	logDebug("showBusinessGraceAlert");
    	if (businessGraceAlert != null && businessGraceAlert.isShowing()) {
    		return;
		}

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyleNormal);
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_business_grace_alert, null);
        builder.setView(v);

        Button dismissButton = v.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
            	isBusinessGraceAlertShown = false;
                try {
                    businessGraceAlert.dismiss();
                } catch (Exception e) {
                    logWarning("Exception dismissing businessGraceAlert", e);
                }
            }
        });

        businessGraceAlert = builder.create();
        businessGraceAlert.setCanceledOnTouchOutside(false);
        try {
            businessGraceAlert.show();
        }catch (Exception e){
            logWarning("Exception showing businessGraceAlert", e);
        }
        isBusinessGraceAlertShown = true;
    }

	public void checkIfShouldShowBusinessCUAlert(String f, boolean firstTime) {
    	businessCUF = f;
    	businessCUFirstTime = firstTime;
		if (megaApi.isBusinessAccount() && !megaApi.isMasterBusinessAccount()) {
			showBusinessCUAlert();
		} else {
		    enableCU();
		}
	}

    private void showBusinessCUAlert() {
        if (businessCUAlert != null && businessCUAlert.isShowing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyleNormal);
        builder.setTitle(R.string.section_photo_sync)
                .setMessage(R.string.camera_uploads_business_alert)
                .setNegativeButton(R.string.general_cancel, (dialog, which) -> {})
                .setPositiveButton(R.string.general_enable, (dialog, which) -> enableCU())
				.setCancelable(false)
				.setOnDismissListener(dialog -> isBusinessCUAlertShown = false);
        businessCUAlert = builder.create();
        businessCUAlert.show();
        isBusinessCUAlertShown = true;
    }

    private void enableCU() {
        if (businessCUF.equals(BUSINESS_CU_FRAGMENT_SETTINGS)) {
            if (getSettingsFragment() != null) {
                sttFLol.enableCameraUpload();
            }
        } else if (businessCUF.equals(BUSINESS_CU_FRAGMENT_CU)) {
            cuFL = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CAMERA_UPLOADS.getTag());
            if (cuFL == null) {
                return;
            }

            if (businessCUFirstTime) {
                cuFL.cameraOnOffFirstTime();
            } else {
                cuFL.cameraOnOff();
            }
        }
    }

	private void openContactLink (long handle) {
    	if (handle == -1) {
			logWarning("Not valid contact handle");
    		return;
		}

		handleInviteContact = handle;
    	dismissOpenLinkDialog();
		logDebug("Handle to invite a contact: " + handle);
		drawerItem = DrawerItem.CONTACTS;
		indexContacts = 0;
		selectDrawerItemLollipop(drawerItem);
	}

	private void askForSMSVerification() {
        if(!smsDialogTimeChecker.shouldShow()) return;
        showStorageAlertWithDelay = true;
        //If mobile device, only portrait mode is allowed
        if (!isTablet(this)) {
            logDebug("mobile only portrait mode");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        smsDialogTimeChecker.update();
        onAskingSMSVerificationFragment = true;
        if (svF == null) {
            svF = new SMSVerificationFragment();
        }
        replaceFragment(svF, FragmentTag.SMS_VERIFICATION.getTag());
        tabLayoutContacts.setVisibility(View.GONE);
        viewPagerContacts.setVisibility(View.GONE);
        tabLayoutShares.setVisibility(View.GONE);
        viewPagerShares.setVisibility(View.GONE);
        tabLayoutMyAccount.setVisibility(View.GONE);
        viewPagerMyAccount.setVisibility(View.GONE);
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

	public void askForAccess () {
        showStorageAlertWithDelay = true;
    	//If mobile device, only portrait mode is allowed
		if (!isTablet(this)) {
			logDebug("Mobile only portrait mode");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    	boolean writeStorageGranted = checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		boolean readStorageGranted = checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
    	boolean cameraGranted = checkPermission(Manifest.permission.CAMERA);
		boolean microphoneGranted = checkPermission(Manifest.permission.RECORD_AUDIO);
//		boolean writeCallsGranted = checkPermission(Manifest.permission.WRITE_CALL_LOG);

		if (!writeStorageGranted || !readStorageGranted || !cameraGranted || !microphoneGranted/* || !writeCallsGranted*/) {
			deleteCurrentFragment();

			if (pF == null) {
				pF = new PermissionsFragment();
			}

			replaceFragment(pF, FragmentTag.PERMISSIONS.getTag());

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
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }
        onAskingSMSVerificationFragment = false;
        svF = null;
        // For Android devices which have Android below 6, no need to go to request permission fragment.
        if(!firstTimeAfterInstallation || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            tB.setVisibility(View.VISIBLE);
            abL.setVisibility(View.VISIBLE);

            deleteCurrentFragment();


            changeStatusBarColor(COLOR_STATUS_BAR_ZERO);

            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            supportInvalidateOptionsMenu();
            selectDrawerItemLollipop(drawerItem);
        }
    }

	public void destroyPermissionsFragment () {
		//In mobile, allow all orientation after permission screen
		if (!isTablet(this)) {
			logDebug("Mobile, all orientation");
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
		}

		turnOnNotifications = false;

		tB.setVisibility(View.VISIBLE);
		abL.setVisibility(View.VISIBLE);

		deleteCurrentFragment();

		onAskingPermissionsFragment = false;

		pF = null;

		changeStatusBarColor(COLOR_STATUS_BAR_ZERO);

		drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
		supportInvalidateOptionsMenu();
		selectDrawerItemLollipop(drawerItem);
	}

	void setContactStatus() {
		if (megaChatApi == null) {
			megaChatApi = app.getMegaChatApi();
			megaChatApi.addChatListener(this);
		}

		int chatStatus = megaChatApi.getOnlineStatus();
		if (contactStatus != null) {
			switch (chatStatus) {
				case MegaChatApi.STATUS_ONLINE: {
					logDebug("Online");
					contactStatus.setVisibility(View.VISIBLE);
					contactStatus.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_online));
					break;
				}
				case MegaChatApi.STATUS_AWAY: {
					logDebug("Away");
					contactStatus.setVisibility(View.VISIBLE);
					contactStatus.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_away));
					break;
				}
				case MegaChatApi.STATUS_BUSY: {
					logDebug("Busy");
					contactStatus.setVisibility(View.VISIBLE);
					contactStatus.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_busy));
					break;
				}
				case MegaChatApi.STATUS_OFFLINE: {
					logDebug("Offline");
					contactStatus.setVisibility(View.VISIBLE);
					contactStatus.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_offline));
					break;
				}
				case MegaChatApi.STATUS_INVALID: {
					logWarning("Invalid");
					contactStatus.setVisibility(View.GONE);
					break;
				}
				default: {
					logDebug("Default");
					contactStatus.setVisibility(View.GONE);
					break;
				}
			}
		}
	}

	void passwordReminderDialogBlocked(){
		megaApi.passwordReminderDialogBlocked(this);
	}

	void passwordReminderDialogSkiped(){
		megaApi.passwordReminderDialogSkipped(this);
	}

	@Override
	protected void onResume(){
		if (drawerItem == DrawerItem.SEARCH && getSearchFragment() != null) {
			sFLol.setWaitingForSearchedNodes(true);
		}

		super.onResume();

//		dbH.setShowNotifOff(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			queryIfNotificationsAreOn();
		}

		if (getResources().getConfiguration().orientation != orientationSaved) {
			orientationSaved = getResources().getConfiguration().orientation;
			drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
		}

        checkScrollElevation();

		if (drawerItem == DrawerItem.ACCOUNT ) {
			app.refreshAccountInfo();
		}
	}

	void queryIfNotificationsAreOn(){
		logDebug("queryIfNotificationsAreOn");

		if (dbH == null){
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		}

		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}

		if (turnOnNotifications){
			setTurnOnNotificationsFragment();
		}
		else {
			NotificationManagerCompat nf = NotificationManagerCompat.from(this);
			logDebug ("Notifications Enabled: " + nf.areNotificationsEnabled());
			if (!nf.areNotificationsEnabled()){
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

	public void deleteTurnOnNotificationsFragment(){
		logDebug("deleteTurnOnNotificationsFragment");
		turnOnNotifications = false;

		tB.setVisibility(View.VISIBLE);
		abL.setVisibility(View.VISIBLE);

		tonF = null;

		changeStatusBarColor(COLOR_STATUS_BAR_ZERO);

		drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
		supportInvalidateOptionsMenu();
		selectDrawerItemLollipop(drawerItem);
	}

	void deleteCurrentFragment () {
		Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
		if (currentFragment != null){
			getSupportFragmentManager().beginTransaction().remove(currentFragment).commitNowAllowingStateLoss();
		}
	}

	void setTurnOnNotificationsFragment(){
		logDebug("setTurnOnNotificationsFragment");
		aB.setSubtitle(null);
		tB.setVisibility(View.GONE);

		deleteCurrentFragment();

		if (tonF == null){
			tonF = new TurnOnNotificationsFragment();
		}
		replaceFragment(tonF, FragmentTag.TURN_ON_NOTIFICATIONS.getTag());

		setTabsVisibility();
		abL.setVisibility(View.GONE);

        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.turn_on_notifications_statusbar));

		drawerLayout.closeDrawer(Gravity.LEFT);
		drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
		supportInvalidateOptionsMenu();
		hideFabButton();
		showHideBottomNavigationView(true);
	}

	void actionOpenFolder (long handleIntent) {
		logDebug("Handle Intent: " + handleIntent);
		int access = -1;
		if (handleIntent != -1) {
			MegaNode parentIntentN = megaApi.getNodeByHandle(handleIntent);
			if (parentIntentN != null) {
				access = megaApi.getAccess(parentIntentN);
				switch (access) {
					case MegaShare.ACCESS_OWNER:
					case MegaShare.ACCESS_UNKNOWN: {
						logDebug("The intent set the parentHandleBrowser to " + handleIntent);
						parentHandleBrowser = handleIntent;
						drawerItem = DrawerItem.CLOUD_DRIVE;
						break;
					}
					case MegaShare.ACCESS_READ:
					case MegaShare.ACCESS_READWRITE:
					case MegaShare.ACCESS_FULL: {
						logDebug("The intent set the parentHandleIncoming to " + handleIntent);
						parentHandleIncoming = handleIntent;
						drawerItem = DrawerItem.SHARED_ITEMS;
						deepBrowserTreeIncoming = calculateDeepBrowserTreeIncoming(parentIntentN, this);
						logDebug("After calculate deepBrowserTreeIncoming: " + deepBrowserTreeIncoming);
						break;
					}
					default: {
						logDebug("DEFAULT: The intent set the parentHandleBrowser to " + handleIntent);
						parentHandleBrowser = handleIntent;
						drawerItem = DrawerItem.CLOUD_DRIVE;
						break;
					}
				}
			}
		}
	}

	@Override
	protected void onPostResume() {
		logDebug("onPostResume");
    	super.onPostResume();

		if (isSearching){
			selectDrawerItemLollipop(DrawerItem.SEARCH);
			isSearching = false;
			return;
		}

    	managerActivity = this;

    	Intent intent = getIntent();

//    	dbH = new DatabaseHandler(getApplicationContext());
    	dbH = DatabaseHandler.getDbHandler(getApplicationContext());
    	if(dbH.getCredentials() == null){
    		if (!openLink){
//				megaApi.localLogout();
//				AccountController aC = new AccountController(this);
//				aC.logout(this, megaApi, megaChatApi, false);
    			return;
    		}
    		else{
				logDebug("Not credentials");
    			if (intent != null) {
					logDebug("Not credentials -> INTENT");
    				if (intent.getAction() != null){
						logDebug("Intent with ACTION: " + intent.getAction());

    					if (getIntent().getAction().equals(ACTION_EXPORT_MASTER_KEY)){
    						Intent exportIntent = new Intent(managerActivity, LoginActivityLollipop.class);
							intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
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

    		if (intent.getAction() != null){
				logDebug("Intent action");

    			if(getIntent().getAction().equals(ACTION_EXPLORE_ZIP)){
					logDebug("Open zip browser");

    				String pathZip=intent.getExtras().getString(EXTRA_PATH_ZIP);

    				Intent intentZip = new Intent(managerActivity, ZipBrowserActivityLollipop.class);
    				intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_PATH_ZIP, pathZip);
    			    startActivity(intentZip);
    			}
//    			else if(getIntent().getAction().equals(ManagerActivityLollipop.ACTION_OPEN_PDF)){
//
//    				String pathPdf=intent.getExtras().getString(EXTRA_PATH_PDF);
//
//    			    File pdfFile = new File(pathPdf);
//
//    			    Intent intentPdf = new Intent();
//    			    intentPdf.setDataAndType(Uri.fromFile(pdfFile), "application/pdf");
//    			    intentPdf.setClass(this, OpenPDFActivity.class);
//    			    intentPdf.setAction("android.intent.action.VIEW");
//    				this.startActivity(intentPdf);
//
//    			}
    			if (getIntent().getAction().equals(ACTION_IMPORT_LINK_FETCH_NODES)){
					logDebug("ACTION_IMPORT_LINK_FETCH_NODES");

					Intent loginIntent = new Intent(managerActivity, LoginActivityLollipop.class);
					intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
					loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					loginIntent.setAction(ACTION_IMPORT_LINK_FETCH_NODES);
					loginIntent.setData(Uri.parse(getIntent().getDataString()));
					startActivity(loginIntent);
					finish();
					return;
				}
				else if (getIntent().getAction().equals(ACTION_OPEN_MEGA_LINK)){
					logDebug("ACTION_OPEN_MEGA_LINK");

					Intent fileLinkIntent = new Intent(managerActivity, FileLinkActivityLollipop.class);
					fileLinkIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					fileLinkIntent.setAction(ACTION_IMPORT_LINK_FETCH_NODES);
					String data = getIntent().getDataString();
					if(data!=null){
						fileLinkIntent.setData(Uri.parse(data));
						startActivity(fileLinkIntent);
					}
					else{
						logWarning("getDataString is NULL");
					}
					finish();
					return;
				}
    			else if (intent.getAction().equals(ACTION_OPEN_MEGA_FOLDER_LINK)){
					logDebug("ACTION_OPEN_MEGA_FOLDER_LINK");

    				Intent intentFolderLink = new Intent(managerActivity, FolderLinkActivityLollipop.class);
    				intentFolderLink.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    				intentFolderLink.setAction(ACTION_OPEN_MEGA_FOLDER_LINK);

					String data = getIntent().getDataString();
					if(data!=null){
						intentFolderLink.setData(Uri.parse(data));
						startActivity(intentFolderLink);
					}
					else{
						logWarning("getDataString is NULL");
					}
					finish();
    			}
    			else if (intent.getAction().equals(ACTION_REFRESH_PARENTHANDLE_BROWSER)){

    				parentHandleBrowser = intent.getLongExtra("parentHandle", -1);
    				intent.removeExtra("parentHandle");

					//Refresh Cloud Fragment
					refreshCloudDrive();

					//Refresh Rubbish Fragment
					refreshRubbishBin();
    			}
    			else if(intent.getAction().equals(ACTION_OVERQUOTA_STORAGE)){
	    			showOverquotaAlert(false);
	    		}
				else if(intent.getAction().equals(ACTION_PRE_OVERQUOTA_STORAGE)){
					showOverquotaAlert(true);
				}
	    		else if(intent.getAction().equals(ACTION_OVERQUOTA_TRANSFER)){
					logWarning("Show overquota transfer alert!!");

					if(alertDialogTransferOverquota==null){
						showTransferOverquotaDialog();
					}
					else{
						if(!(alertDialogTransferOverquota.isShowing())){
							showTransferOverquotaDialog();
						}
					}
				}
				else if (intent.getAction().equals(ACTION_CHANGE_AVATAR)){
					logDebug("Intent CHANGE AVATAR");

					String path = intent.getStringExtra("IMAGE_PATH");
					megaApi.setAvatar(path, this);
				} else if (intent.getAction().equals(ACTION_CANCEL_CAM_SYNC)) {
					logDebug("ACTION_CANCEL_UPLOAD or ACTION_CANCEL_DOWNLOAD or ACTION_CANCEL_CAM_SYNC");
					drawerItem = DrawerItem.TRANSFERS;
					indexTransfers = intent.getIntExtra(TRANSFERS_TAB, ERROR_TAB);
					selectDrawerItemLollipop(drawerItem);

                    String text = getString(R.string.cam_sync_cancel_sync);

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(text);

                    builder.setPositiveButton(getString(R.string.general_yes),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    stopRunningCameraUploadService(ManagerActivityLollipop.this);
                                    dbH.setCamSyncEnabled(false);
									if(sttFLol != null  && sttFLol.isResumed()){
										sttFLol.disableCameraUpload();
									}
									if(cuFL != null && cuFL.isResumed()){
										cuFL.resetSwitchButtonLabel();
									}
                                }
                            });
                    builder.setNegativeButton(getString(R.string.general_no), null);
                    final AlertDialog dialog = builder.create();
                    try {
                        dialog.show();
                    } catch (Exception ex) {
						logError("EXCEPTION", ex);
                    }
				}
    			else if (intent.getAction().equals(ACTION_SHOW_TRANSFERS)){
					logDebug("Intent show transfers");

    				drawerItem = DrawerItem.TRANSFERS;
					indexTransfers = intent.getIntExtra(TRANSFERS_TAB, ERROR_TAB);
    				selectDrawerItemLollipop(drawerItem);
    			}
    			else if (intent.getAction().equals(ACTION_TAKE_SELFIE)){
					logDebug("Intent take selfie");
					checkTakePicture(this, TAKE_PHOTO_CODE);
    			}
				else if (intent.getAction().equals(SHOW_REPEATED_UPLOAD)){
					logDebug("Intent SHOW_REPEATED_UPLOAD");
					String message = intent.getStringExtra("MESSAGE");
					showSnackbar(SNACKBAR_TYPE, message, -1);
				}
				else if(getIntent().getAction().equals(ACTION_IPC)){
					logDebug("IPC - go to received request in Contacts");
					markNotificationsSeen(true);
					drawerItem=DrawerItem.CONTACTS;
					indexContacts=2;
					selectDrawerItemLollipop(drawerItem);
				}
				else if(getIntent().getAction().equals(ACTION_CHAT_NOTIFICATION_MESSAGE)){
					logDebug("ACTION_CHAT_NOTIFICATION_MESSAGE");

					long chatId = getIntent().getLongExtra("CHAT_ID", -1);
					if (getIntent().getBooleanExtra("moveToChatSection", false)){
						moveToChatSection(chatId);
					}
					else {
						String text = getIntent().getStringExtra("showSnackbar");
						if (chatId != -1) {
							openChat(chatId, text);
						}
					}
				}
				else if(getIntent().getAction().equals(ACTION_CHAT_SUMMARY)) {
					logDebug("ACTION_CHAT_SUMMARY");
					drawerItem=DrawerItem.CHAT;
					selectDrawerItemLollipop(drawerItem);
				}
				else if(getIntent().getAction().equals(ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION)){
					logDebug("ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION");
					markNotificationsSeen(true);

					drawerItem=DrawerItem.SHARED_ITEMS;
					indexShares = 0;
					selectDrawerItemLollipop(drawerItem);
				}
				else if(getIntent().getAction().equals(ACTION_OPEN_CONTACTS_SECTION)){
					logDebug("ACTION_OPEN_CONTACTS_SECTION");
					markNotificationsSeen(true);
					openContactLink(getIntent().getLongExtra(CONTACT_HANDLE, -1));
				}
				else if (getIntent().getAction().equals(ACTION_RECOVERY_KEY_EXPORTED)){
					logDebug("ACTION_RECOVERY_KEY_EXPORTED");
					exportRecoveryKey();
				}
				else if (getIntent().getAction().equals(ACTION_REQUEST_DOWNLOAD_FOLDER_LOGOUT)){
					String parentPath = intent.getStringExtra("parentPath");
					if (parentPath != null){
						AccountController ac = new AccountController(this);
						ac.exportMK(parentPath);
					}
				}
				else  if (getIntent().getAction().equals(ACTION_RECOVERY_KEY_COPY_TO_CLIPBOARD)){
					AccountController ac = new AccountController(this);
					if (getIntent().getBooleanExtra("logout", false)) {
						ac.copyMK(true);
					}
					else {
						ac.copyMK(false);
					}
				}
				else if (getIntent().getAction().equals(ACTION_REFRESH_STAGING)){
					update2FASetting();
				}
				else if (getIntent().getAction().equals(ACTION_OPEN_FOLDER)) {
					logDebug("Open after LauncherFileExplorerActivityLollipop ");
					long handleIntent = getIntent().getLongExtra("PARENT_HANDLE", -1);

					if (getIntent().getBooleanExtra(SHOW_MESSAGE_UPLOAD_STARTED, false)) {
						int numberUploads = getIntent().getIntExtra(NUMBER_UPLOADS, 1);
						showSnackbar(SNACKBAR_TYPE, getResources().getQuantityString(R.plurals.upload_began, numberUploads, numberUploads), -1);
					}

					actionOpenFolder(handleIntent);
					selectDrawerItemLollipop(drawerItem);
				}
				else if(getIntent().getAction().equals(ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE)){
					long chatId = getIntent().getLongExtra("CHAT_ID", -1);
					showSnackbar(MESSAGE_SNACKBAR_TYPE, null, chatId);
				}

    			intent.setAction(null);
				setIntent(null);
    		}
    	}


    	if (bNV != null){
            Menu nVMenu = bNV.getMenu();
            resetNavigationViewMenu(nVMenu);

    		switch(drawerItem){
	    		case CLOUD_DRIVE:{
					logDebug("Case CLOUD DRIVE");
					//Check the tab to shown and the title of the actionBar
					setToolbarTitle();
					setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV);
	    			break;
	    		}
	    		case SHARED_ITEMS:{
					logDebug("Case SHARED ITEMS");
					setBottomNavigationMenuItemChecked(SHARED_BNV);
					try {
						NotificationManager notificationManager =
								(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

						notificationManager.cancel(NOTIFICATION_PUSH_CLOUD_DRIVE);
					}
					catch (Exception e){
						logError("Exception NotificationManager - remove contact notification", e);
					}
					setToolbarTitle();
		    		break;
	    		}
				case SETTINGS:{
					setToolbarTitle();
					setBottomNavigationMenuItemChecked(HIDDEN_BNV);
					break;
				}
				case CONTACTS:{
					setBottomNavigationMenuItemChecked(HIDDEN_BNV);
					try {
						ContactsAdvancedNotificationBuilder notificationBuilder;
						notificationBuilder =  ContactsAdvancedNotificationBuilder.newInstance(this, megaApi);

						notificationBuilder.removeAllIncomingContactNotifications();
						notificationBuilder.removeAllAcceptanceContactNotifications();
					}
					catch (Exception e){
						logError("Exception NotificationManager - remove all CONTACT notifications", e);
					}

					setToolbarTitle();
					break;
				}
				case SEARCH:{
					setBottomNavigationMenuItemChecked(HIDDEN_BNV);
					setToolbarTitle();
					break;
				}
				case CHAT:{
					setBottomNavigationMenuItemChecked(CHAT_BNV);
					rChatFL = (RecentChatsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
					if (rChatFL != null){
						rChatFL.setChats();
						rChatFL.setStatus();

						try {
							ChatAdvancedNotificationBuilder notificationBuilder;
							notificationBuilder =  ChatAdvancedNotificationBuilder.newInstance(this, megaApi, megaChatApi);

							notificationBuilder.removeAllChatNotifications();
						}
						catch (Exception e){
							logError("Exception NotificationManager - remove all notifications", e);
						}

						MegaApplication.setRecentChatVisible(true);
					}
					break;
				}
				case ACCOUNT:{
					setBottomNavigationMenuItemChecked(HIDDEN_BNV);
					setToolbarTitle();
					try {
						NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
						notificationManager.cancel(NOTIFICATION_STORAGE_OVERQUOTA);
					}
					catch (Exception e){
						logError("Exception NotificationManager - remove all notifications", e);
					}

					break;
				}
				case CAMERA_UPLOADS: {
					setBottomNavigationMenuItemChecked(CAMERA_UPLOADS_BNV);
				}
				case NOTIFICATIONS: {
					notificFragment = (NotificationsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.NOTIFICATIONS.getTag());
					if(notificFragment!=null){
						notificFragment.setNotifications();
					}
				}
    		}
    	}
	}

	public void openChat(long chatId, String text){
		logDebug("Chat ID: " + chatId);
//		drawerItem=DrawerItem.CHAT;
//		selectDrawerItemLollipop(drawerItem);

		if(chatId!=-1){
			MegaChatRoom chat = megaChatApi.getChatRoom(chatId);
			if(chat!=null){
				logDebug("Open chat with id: " + chatId);
				Intent intentToChat = new Intent(this, ChatActivityLollipop.class);
				intentToChat.setAction(ACTION_CHAT_SHOW_MESSAGES);
				intentToChat.putExtra("CHAT_ID", chatId);
				if(text!=null){
					intentToChat.putExtra("showSnackbar", text);
				}
				this.startActivity(intentToChat);
			}
			else{
				logError("Error, chat is NULL");
			}
		}
		else{
			logError("Error, chat id is -1");
		}
	}

	public void showMuteIcon(MegaChatListItem item){
		logDebug("showMuteIcon");
		rChatFL = (RecentChatsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
		if (rChatFL != null) {
			rChatFL.showMuteIcon(item);
		}
	}

	public void setProfileAvatar() {
		logDebug("setProfileAvatar");
		File avatar = buildAvatarFile(this, megaApi.getMyEmail() + ".jpg");
		Bitmap imBitmap;
		if (isFileAvailable(avatar) && avatar.length() > 0) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(avatar.getAbsolutePath(), options);

			// Calculate inSampleSize
			options.inSampleSize = calculateInSampleSize(options, 250, 250);

			// Decode bitmap with inSampleSize set
			options.inJustDecodeBounds = false;

			imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), options);
			if (imBitmap == null) {
				avatar.delete();
				megaApi.getUserAvatar(megaApi.getMyUser(), buildAvatarFile(this, megaApi.getMyEmail() + ".jpg").getAbsolutePath(), this);
			} else {
				Bitmap circleBitmap = Bitmap.createBitmap(imBitmap.getWidth(), imBitmap.getHeight(), Bitmap.Config.ARGB_8888);

				BitmapShader shader = new BitmapShader(imBitmap, TileMode.CLAMP, TileMode.CLAMP);
				Paint paint = new Paint();
				paint.setShader(shader);

				Canvas c = new Canvas(circleBitmap);
				int radius;
				if (imBitmap.getWidth() < imBitmap.getHeight())
					radius = imBitmap.getWidth() / 2;
				else
					radius = imBitmap.getHeight() / 2;

				c.drawCircle(imBitmap.getWidth() / 2, imBitmap.getHeight() / 2, radius, paint);
				nVPictureProfile.setImageBitmap(circleBitmap);
			}
		} else {
			megaApi.getUserAvatar(megaApi.getMyUser(), buildAvatarFile(this, megaApi.getMyEmail() + ".jpg").getAbsolutePath(), this);
		}
	}

	public void setDefaultAvatar(){
		logDebug("setDefaultAvatar");
		nVPictureProfile.setImageBitmap(getDefaultAvatar(getColorAvatar(megaApi.getMyUser()), MegaApplication.getInstance().getMyAccountInfo().getFullName(), AVATAR_SIZE, true));
	}

	public void setOfflineAvatar(String email, long myHandle, String name){
		logDebug("setOfflineAvatar");

		File avatar = buildAvatarFile(this, email + ".jpg");
		Bitmap imBitmap = null;
		if (isFileAvailable(avatar)) {
			if (avatar.length() > 0) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(avatar.getAbsolutePath(), options);

				// Calculate inSampleSize
				options.inSampleSize = calculateInSampleSize(options, 250, 250);

				// Decode bitmap with inSampleSize set
				options.inJustDecodeBounds = false;

				imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), options);
				if (imBitmap != null) {
					Bitmap circleBitmap = Bitmap.createBitmap(imBitmap.getWidth(), imBitmap.getHeight(), Bitmap.Config.ARGB_8888);

					BitmapShader shader = new BitmapShader(imBitmap, TileMode.CLAMP, TileMode.CLAMP);
					Paint paint = new Paint();
					paint.setShader(shader);

					Canvas c = new Canvas(circleBitmap);
					int radius;
					if (imBitmap.getWidth() < imBitmap.getHeight())
						radius = imBitmap.getWidth() / 2;
					else
						radius = imBitmap.getHeight() / 2;

					c.drawCircle(imBitmap.getWidth() / 2, imBitmap.getHeight() / 2, radius, paint);
					if (nVPictureProfile != null){
						nVPictureProfile.setImageBitmap(circleBitmap);
					}
					return;
				}
			}
		}

		if (nVPictureProfile != null){
			nVPictureProfile.setImageBitmap(getDefaultAvatar(getColorAvatar(myHandle), name, AVATAR_SIZE, true));
		}
	}

	public void showDialogChangeUserAttribute(){
		userNameChanged = false;
		userEmailChanged = false;

		megaApi.multiFactorAuthCheck(megaApi.getMyEmail(), this);

		ScrollView scrollView = new ScrollView(this);

		LinearLayout layout = new LinearLayout(this);

		scrollView.addView(layout);

		layout.setOrientation(LinearLayout.VERTICAL);
//        layout.setNestedScrollingEnabled(true);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);

		LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params1.setMargins(scaleWidthPx(20, outMetrics), 0, scaleWidthPx(17, outMetrics), 0);

		final EmojiEditText inputFirstName = new EmojiEditText(this);
		inputFirstName.getBackground().mutate().clearColorFilter();
		inputFirstName.getBackground().mutate().setColorFilter(ContextCompat.getColor(this, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
		layout.addView(inputFirstName, params);

		final RelativeLayout error_layout_firtName = new RelativeLayout(ManagerActivityLollipop.this);
		layout.addView(error_layout_firtName, params1);

		final ImageView error_icon_firtName = new ImageView(ManagerActivityLollipop.this);
		error_icon_firtName.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_input_warning));
		error_layout_firtName.addView(error_icon_firtName);
		RelativeLayout.LayoutParams params_icon_firtName = (RelativeLayout.LayoutParams) error_icon_firtName.getLayoutParams();

		params_icon_firtName.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		error_icon_firtName.setLayoutParams(params_icon_firtName);

		error_icon_firtName.setColorFilter(ContextCompat.getColor(ManagerActivityLollipop.this, R.color.login_warning));

		final TextView textError_firtName = new TextView(ManagerActivityLollipop.this);
		error_layout_firtName.addView(textError_firtName);
		RelativeLayout.LayoutParams params_text_error_firtName = (RelativeLayout.LayoutParams) textError_firtName.getLayoutParams();
		params_text_error_firtName.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error_firtName.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error_firtName.addRule(RelativeLayout.CENTER_VERTICAL);
		params_text_error_firtName.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params_text_error_firtName.setMargins(scaleWidthPx(3, outMetrics), 0,0,0);
		textError_firtName.setLayoutParams(params_text_error_firtName);

		textError_firtName.setTextColor(ContextCompat.getColor(ManagerActivityLollipop.this, R.color.login_warning));

		error_layout_firtName.setVisibility(View.GONE);

		final EmojiEditText inputLastName = new EmojiEditText(this);
		inputLastName.getBackground().mutate().clearColorFilter();
		inputLastName.getBackground().mutate().setColorFilter(ContextCompat.getColor(this, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
		layout.addView(inputLastName, params);

		final RelativeLayout error_layout_lastName = new RelativeLayout(ManagerActivityLollipop.this);
		layout.addView(error_layout_lastName, params1);

		final ImageView error_icon_lastName = new ImageView(ManagerActivityLollipop.this);
		error_icon_lastName.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_input_warning));
		error_layout_lastName.addView(error_icon_lastName);
		RelativeLayout.LayoutParams params_icon_lastName = (RelativeLayout.LayoutParams) error_icon_lastName.getLayoutParams();


		params_icon_lastName.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		error_icon_lastName.setLayoutParams(params_icon_lastName);

		error_icon_lastName.setColorFilter(ContextCompat.getColor(ManagerActivityLollipop.this, R.color.login_warning));

		final TextView textError_lastName = new TextView(ManagerActivityLollipop.this);
		error_layout_lastName.addView(textError_lastName);
		RelativeLayout.LayoutParams params_text_error_lastName = (RelativeLayout.LayoutParams) textError_lastName.getLayoutParams();
		params_text_error_lastName.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error_lastName.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error_lastName.addRule(RelativeLayout.CENTER_VERTICAL);
		params_text_error_lastName.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params_text_error_lastName.setMargins(scaleWidthPx(3, outMetrics), 0,0,0);
		textError_lastName.setLayoutParams(params_text_error_lastName);

		textError_lastName.setTextColor(ContextCompat.getColor(ManagerActivityLollipop.this, R.color.login_warning));

		error_layout_lastName.setVisibility(View.GONE);

		final EditText inputMail = new EditText(this);
		inputMail.getBackground().mutate().clearColorFilter();
		inputMail.getBackground().mutate().setColorFilter(ContextCompat.getColor(this, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
		layout.addView(inputMail, params);

		final RelativeLayout error_layout_email = new RelativeLayout(ManagerActivityLollipop.this);
		layout.addView(error_layout_email, params1);

		final ImageView error_icon_email = new ImageView(ManagerActivityLollipop.this);
		error_icon_email.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_input_warning));
		error_layout_email.addView(error_icon_email);
		RelativeLayout.LayoutParams params_icon_email = (RelativeLayout.LayoutParams) error_icon_email.getLayoutParams();


		params_icon_email.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		error_icon_email.setLayoutParams(params_icon_email);

		error_icon_email.setColorFilter(ContextCompat.getColor(ManagerActivityLollipop.this, R.color.login_warning));

		final TextView textError_email = new TextView(ManagerActivityLollipop.this);
		error_layout_email.addView(textError_email);
		RelativeLayout.LayoutParams params_text_error_email = (RelativeLayout.LayoutParams) textError_email.getLayoutParams();
		params_text_error_email.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error_email.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error_email.addRule(RelativeLayout.CENTER_VERTICAL);
		params_text_error_email.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params_text_error_email.setMargins(scaleWidthPx(3, outMetrics), 0,scaleWidthPx(20, outMetrics),0);
		textError_email.setLayoutParams(params_text_error_email);

		textError_email.setTextColor(ContextCompat.getColor(ManagerActivityLollipop.this, R.color.login_warning));

		error_layout_email.setVisibility(View.GONE);

		final OnEditorActionListener editorActionListener = new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String valueFirstName = inputFirstName.getText().toString().trim();
					String valueLastName = inputLastName.getText().toString().trim();
					String value = inputMail.getText().toString().trim();
					String emailError = getEmailError(value, managerActivity);
					if (emailError == null && userEmailChanged && !userNameChanged) {
						emailError = comparedToCurrentEmail(value, managerActivity);
					}
					if (emailError != null) {
//						inputMail.setError(emailError);
						inputMail.getBackground().setColorFilter(ContextCompat.getColor(managerActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError_email.setText(emailError);
						error_layout_email.setVisibility(View.VISIBLE);
						inputMail.requestFocus();
					} else if (valueFirstName.equals("") || valueFirstName.isEmpty()) {
						logWarning("First name input is empty");
//						inputFirstName.setError(getString(R.string.invalid_string));
						inputFirstName.getBackground().setColorFilter(ContextCompat.getColor(managerActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError_firtName.setText(getString(R.string.invalid_string));
						error_layout_firtName.setVisibility(View.VISIBLE);
						inputFirstName.requestFocus();
					} else if (valueLastName.equals("") || valueLastName.isEmpty()) {
						logWarning("Last name input is empty");
//						inputLastName.setError(getString(R.string.invalid_string));
						inputLastName.getBackground().setColorFilter(ContextCompat.getColor(managerActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError_lastName.setText(getString(R.string.invalid_string));
						error_layout_lastName.setVisibility(View.VISIBLE);
						inputLastName.requestFocus();
					} else {
						logDebug("Positive button pressed - change user attribute(s)");
						countUserAttributes = aC.updateUserAttributes(((MegaApplication) getApplication()).getMyAccountInfo().getFirstNameText(), valueFirstName, ((MegaApplication) getApplication()).getMyAccountInfo().getLastNameText(), valueLastName, megaApi.getMyEmail(), value);
						changeUserAttributeDialog.dismiss();
					}
				} else {
					logDebug("Other IME" + actionId);
				}
				return false;
			}
		};

		inputFirstName.setSingleLine();
		inputFirstName.setText(((MegaApplication) getApplication()).getMyAccountInfo().getFirstNameText());
		inputFirstName.setTextColor(getResources().getColor(R.color.text_secondary));
		inputFirstName.setImeOptions(EditorInfo.IME_ACTION_DONE);
		inputFirstName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		inputFirstName.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				userNameChanged = true;
				if(error_layout_firtName.getVisibility() == View.VISIBLE){
					error_layout_firtName.setVisibility(View.GONE);
					inputFirstName.getBackground().mutate().clearColorFilter();
					inputFirstName.getBackground().mutate().setColorFilter(ContextCompat.getColor(managerActivity, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
				}
			}
		});
		inputFirstName.setOnEditorActionListener(editorActionListener);
		inputFirstName.setImeActionLabel(getString(R.string.save_action),EditorInfo.IME_ACTION_DONE);
		inputFirstName.requestFocus();

		inputLastName.setSingleLine();
		inputLastName.setText(((MegaApplication) getApplication()).getMyAccountInfo().getLastNameText());
		inputLastName.setTextColor(getResources().getColor(R.color.text_secondary));
		inputLastName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		inputLastName.setImeOptions(EditorInfo.IME_ACTION_DONE);
		inputLastName.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				userNameChanged = true;
				if(error_layout_lastName.getVisibility() == View.VISIBLE){
					error_layout_lastName.setVisibility(View.GONE);
					inputLastName.getBackground().mutate().clearColorFilter();
					inputLastName.getBackground().mutate().setColorFilter(ContextCompat.getColor(managerActivity, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
				}
			}
		});
		inputLastName.setOnEditorActionListener(editorActionListener);
		inputLastName.setImeActionLabel(getString(R.string.save_action),EditorInfo.IME_ACTION_DONE);

		inputMail.getBackground().mutate().clearColorFilter();
		inputMail.setSingleLine();
		inputMail.setText(megaApi.getMyUser().getEmail());
		inputMail.setTextColor(getResources().getColor(R.color.text_secondary));
		inputMail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		inputMail.setImeOptions(EditorInfo.IME_ACTION_DONE);
		inputMail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		inputMail.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				userEmailChanged = true;
				if(error_layout_email.getVisibility() == View.VISIBLE){
					error_layout_email.setVisibility(View.GONE);
					inputMail.getBackground().mutate().clearColorFilter();
					inputMail.getBackground().mutate().setColorFilter(ContextCompat.getColor(managerActivity, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);

				}
			}
		});
		inputMail.setOnEditorActionListener(editorActionListener);
		inputMail.setImeActionLabel(getString(R.string.save_action),EditorInfo.IME_ACTION_DONE);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.title_edit_profile_info));

		builder.setPositiveButton(getString(R.string.save_action), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				inputFirstName.getBackground().clearColorFilter();
				inputLastName.getBackground().clearColorFilter();
				inputMail.getBackground().clearColorFilter();
			}
		});
		builder.setView(scrollView);

		changeUserAttributeDialog = builder.create();
		changeUserAttributeDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		changeUserAttributeDialog.show();
		changeUserAttributeDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				logDebug("OK BTTN PASSWORD");
				String valueFirstName = inputFirstName.getText().toString().trim();
				String valueLastName = inputLastName.getText().toString().trim();
				String value = inputMail.getText().toString().trim();
				String emailError = getEmailError(value, managerActivity);
				if (emailError == null && userEmailChanged && !userNameChanged) {
					emailError = comparedToCurrentEmail(value, managerActivity);
				}
				if (emailError != null) {
//					inputMail.setError(emailError);
					inputMail.getBackground().setColorFilter(ContextCompat.getColor(managerActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
					textError_email.setText(emailError);
					error_layout_email.setVisibility(View.VISIBLE);
					inputMail.requestFocus();
				}
				else if(valueFirstName.equals("")||valueFirstName.isEmpty()){
					logWarning("Input is empty");
//					inputFirstName.setError(getString(R.string.invalid_string));
					inputFirstName.getBackground().setColorFilter(ContextCompat.getColor(managerActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
					textError_firtName.setText(getString(R.string.invalid_string));
					error_layout_firtName.setVisibility(View.VISIBLE);
					inputFirstName.requestFocus();
				}
				else if(valueLastName.equals("")||valueLastName.isEmpty()){
					logWarning("Input is empty");
//					inputLastName.setError(getString(R.string.invalid_string));
					inputLastName.getBackground().setColorFilter(ContextCompat.getColor(managerActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
					textError_lastName.setText(getString(R.string.invalid_string));
					error_layout_lastName.setVisibility(View.VISIBLE);
					inputLastName.requestFocus();
				}
				else {
					logDebug("Positive button pressed - change user attribute");
					countUserAttributes = aC.updateUserAttributes(((MegaApplication) getApplication()).getMyAccountInfo().getFirstNameText(), valueFirstName, ((MegaApplication) getApplication()).getMyAccountInfo().getLastNameText(), valueLastName, megaApi.getMyEmail(), value);
					changeUserAttributeDialog.dismiss();
				}
			}
		});
	}

	@Override
	protected void onStop(){
		logDebug("onStop");
		super.onStop();
	}

	@Override
	protected void onPause() {
		logDebug("onPause");
    	managerActivity = null;
    	super.onPause();
    }

	@Override
    protected void onDestroy(){
		logDebug("onDestroy()");

		dbH.removeSentPendingMessages();

    	if (megaApi.getRootNode() != null){
    		megaApi.removeGlobalListener(this);
    		megaApi.removeTransferListener(this);
    		megaApi.removeRequestListener(this);
    	}

		if (megaChatApi != null){
			megaChatApi.removeChatListener(this);
		}
        if (alertDialogSMSVerification != null) {
            alertDialogSMSVerification.dismiss();
        }
		isStorageStatusDialogShown = false;

		LocalBroadcastManager.getInstance(this).unregisterReceiver(contactUpdateReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverUpdatePosition);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(updateMyAccountReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverUpdate2FA);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(networkReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverUpdateOrder);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverUpdateView);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(chatArchivedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshAddPhoneNumberButtonReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(chatCallUpdateReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverCUAttrChanged);

        unregisterReceiver(cameraUploadLauncherReceiver);
        unregisterReceiver(updateCUSettingsReceiver);

		if (mBillingManager != null) {
			mBillingManager.destroy();
		}
		cancelSearch();
        if(reconnectDialog != null) {
            reconnectDialog.cancel();
        }
    	super.onDestroy();
	}

	private void cancelSearch() {
		if (getSearchFragment() != null) {
			sFLol.cancelPreviousAsyncTask();
		}
	}

	void replaceFragment (Fragment f, String fTag) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.fragment_container, f, fTag);
		ft.commitNowAllowingStateLoss();
		// refresh manually
		if (f instanceof RecentChatsFragmentLollipop) {
			RecentChatsFragmentLollipop rcf = (RecentChatsFragmentLollipop) f;
			if (rcf.isResumed()) {
				rcf.refreshMegaContactsList();
				rcf.setCustomisedActionBar();
			}
		}
	}

	void refreshFragment (String fTag) {
		Fragment f = getSupportFragmentManager().findFragmentByTag(fTag);
		if (f != null) {
			logDebug("Fragment " + fTag + " refreshing");
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.detach(f);
			if (fTag.equals(FragmentTag.CLOUD_DRIVE.getTag())) {
				((FileBrowserFragmentLollipop) f).headerItemDecoration = null;
			}
			else if (fTag.equals(FragmentTag.RUBBISH_BIN.getTag())) {
				((RubbishBinFragmentLollipop) f).headerItemDecoration = null;
			}
			else if (fTag.equals(FragmentTag.INCOMING_SHARES.getTag())) {
				((IncomingSharesFragmentLollipop) f).headerItemDecoration = null;
			}
			else if (fTag.equals(FragmentTag.OUTGOING_SHARES.getTag())) {
				((OutgoingSharesFragmentLollipop) f).headerItemDecoration = null;
			}
            else if (fTag.equals(FragmentTag.LINKS.getTag())) {
                ((LinksFragment) f).headerItemDecoration = null;
            }
			else if (fTag.equals(FragmentTag.OFFLINE.getTag())) {
				((OfflineFragmentLollipop) f).setHeaderItemDecoration(null);
				((OfflineFragmentLollipop) f).setPathNavigation(pathNavigationOffline);
			}
			else if (fTag.equals(FragmentTag.INBOX.getTag())) {
				((InboxFragmentLollipop) f).headerItemDecoration = null;
			}
			else if (fTag.equals(FragmentTag.SEARCH.getTag())) {
				((SearchFragmentLollipop) f).setHeaderItemDecoration(null);
			}

			ft.attach(f);
			ft.commitNowAllowingStateLoss();
		}
		else {
			logWarning("Fragment == NULL. Not refresh");
		}
	}

	public boolean isFirstLogin() {
        return firstLogin;
    }

	public void selectDrawerItemCloudDrive(){
		logDebug("selectDrawerItemCloudDrive");
        if (showStorageAlertWithDelay) {
            showStorageAlertWithDelay = false;
            checkStorageStatus(storageStateFromBroadcast, false);
        }
		tB.setVisibility(View.VISIBLE);

		if (cloudPageAdapter == null){
			logDebug("selectDrawerItemCloudDrive: cloudPageAdapter is NULL");
			cloudPageAdapter = new CloudPageAdapter(getSupportFragmentManager(), this);
			viewPagerCloud.setAdapter(cloudPageAdapter);
			tabLayoutCloud.setupWithViewPager(viewPagerCloud);

			//Force on CreateView, addTab do not execute onCreateView
			if(indexCloud!=-1){
				logDebug("selectDrawerItemCloudDrive: The index of the TAB Cloud is: "+indexCloud);
				if (viewPagerCloud != null){
					if(indexCloud==0){
						logDebug("selectDrawerItemCloudDrive: after creating tab in CLOUD TAB: "+parentHandleBrowser);
						viewPagerCloud.setCurrentItem(0);
					}
					else{
						viewPagerCloud.setCurrentItem(1);
					}
				}
				indexCloud=-1;
			}
			else {
				//No bundle, no change of orientation
				logDebug("selectDrawerItemCloudDrive: indexCloud is NOT -1");
			}
		}

		if (!firstTimeAfterInstallation){
			logDebug("Its NOT first time");
			int dbContactsSize = dbH.getContactsSize();
			int sdkContactsSize = megaApi.getContacts().size();
			if (dbContactsSize != sdkContactsSize){
				logDebug("Contacts TABLE != CONTACTS SDK "+ dbContactsSize + " vs " +sdkContactsSize);
				dbH.clearContacts();
				FillDBContactsTask fillDBContactsTask = new FillDBContactsTask(this);
				fillDBContactsTask.execute();
			}
		}
		else{
			logDebug("Its first time");

			//Fill the contacts DB
			FillDBContactsTask fillDBContactsTask = new FillDBContactsTask(this);
			fillDBContactsTask.execute();
			firstTimeAfterInstallation = false;
            dbH.setFirstTime(false);
		}
        checkBeforeShow();
    }

    public void checkBeforeShow() {
        //This account hasn't verified a phone number and first login.
        if (canVoluntaryVerifyPhoneNumber() && (smsDialogTimeChecker.shouldShow() || isSMSDialogShowing) && !newCreationAccount) {
            showSMSVerificationDialog();
        }
    }

	public void setToolbarTitle(){
		logDebug("setToolbarTitle");
		if(drawerItem==null){
			return;
		}

		switch (drawerItem){
			case CLOUD_DRIVE:{

                aB.setSubtitle(null);
				if (getTabItemCloud() == CLOUD_TAB) {
					logDebug("Cloud Drive SECTION");
					MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
					if (parentNode != null) {
						if (megaApi.getRootNode() != null) {
							if (parentNode.getHandle() == megaApi.getRootNode().getHandle() || parentHandleBrowser == -1) {
								aB.setTitle(getString(R.string.title_mega_info_empty_screen).toUpperCase());
								firstNavigationLevel = true;
							}
							else {
								aB.setTitle(parentNode.getName());
								firstNavigationLevel = false;
							}
						}
						else {
							parentHandleBrowser = -1;
						}
					}
					else {
						if (megaApi.getRootNode() != null) {
							parentHandleBrowser = megaApi.getRootNode().getHandle();
							aB.setTitle(getString(R.string.title_mega_info_empty_screen).toUpperCase());
							firstNavigationLevel = true;
						}
						else {
							parentHandleBrowser = -1;
							firstNavigationLevel = true;
						}
					}
				}
				else if (getTabItemCloud() == RECENTS_TAB) {
					if (isRecentsAdded() && getDeepBrowserTreeRecents() > 0
							&& rF.getBucketSelected() != null) {
						aB.setTitle((rF.getBucketSelected().getNodes().size() + " " + getString(R.string.general_files)).toUpperCase());
						firstNavigationLevel = false;
						break;
					}
					firstNavigationLevel = true;
					aB.setTitle(getString(R.string.title_mega_info_empty_screen).toUpperCase());
				}
				break;
			}
			case RUBBISH_BIN: {
				aB.setSubtitle(null);
				if(parentHandleRubbish == megaApi.getRubbishNode().getHandle() || parentHandleRubbish == -1){
					aB.setTitle(getResources().getString(R.string.section_rubbish_bin).toUpperCase());
					firstNavigationLevel = true;
				}
				else{
					MegaNode node = megaApi.getNodeByHandle(parentHandleRubbish);
					if(node==null){
						logWarning("Node NULL - cannot be recovered");
						aB.setTitle(getResources().getString(R.string.section_rubbish_bin).toUpperCase());
					}
					else{
						aB.setTitle(node.getName());
					}

					firstNavigationLevel = false;
				}
				break;
			}
			case SHARED_ITEMS:{
				logDebug("Shared Items SECTION");
				aB.setSubtitle(null);
				int indexShares = getTabItemShares();
				if (indexShares == ERROR_TAB) break;
				switch(indexShares){
					case INCOMING_TAB:{
						if (isIncomingAdded()) {
							if (parentHandleIncoming != -1) {
								MegaNode node = megaApi.getNodeByHandle(parentHandleIncoming);
								if (node == null) {
									aB.setTitle(getResources().getString(R.string.title_shared_items).toUpperCase());
								}
								else {
									aB.setTitle(node.getName());
								}

								firstNavigationLevel = false;
							}
							else {
								aB.setTitle(getResources().getString(R.string.title_shared_items).toUpperCase());
								firstNavigationLevel = true;
							}
						}
						else {
							logDebug("selectDrawerItemSharedItems: inSFLol == null");
							}
						break;
					}
					case OUTGOING_TAB:{
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
			case SAVED_FOR_OFFLINE: {
				aB.setSubtitle(null);

				oFLol = (OfflineFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.OFFLINE.getTag());
				if (oFLol != null && oFLol.getSearchString() != null) {
					String title = getString(R.string.action_search).toUpperCase() + ": " + oFLol.getSearchString();
					aB.setTitle(title);
					firstNavigationLevel = false;
				} else if(pathNavigationOffline != null){
					logDebug("AFTER PathNavigation is: " + pathNavigationOffline);
					if (pathNavigationOffline.equals("/")){
						aB.setTitle(getString(R.string.section_saved_for_offline_new).toUpperCase());
						firstNavigationLevel=true;
					}
					else{
						logDebug("The pathNavigation is: " + pathNavigationOffline);
						String title = pathNavigationOffline;
						int index=title.lastIndexOf("/");
						title=title.substring(0,index);
						index=title.lastIndexOf("/");
						title=title.substring(index+1,title.length());
						aB.setTitle(title);
						firstNavigationLevel=false;
					}
				}
				else{
					logWarning("PathNavigation is NULL");
					aB.setTitle(getString(R.string.section_saved_for_offline_new).toUpperCase());
					firstNavigationLevel=true;
				}

				break;
			}
			case INBOX:{
				aB.setSubtitle(null);
				if(parentHandleInbox==megaApi.getInboxNode().getHandle()||parentHandleInbox==-1){
					aB.setTitle(getResources().getString(R.string.section_inbox).toUpperCase());
					firstNavigationLevel = true;
				}
				else{
					MegaNode node = megaApi.getNodeByHandle(parentHandleInbox);
					aB.setTitle(node.getName());
					firstNavigationLevel = false;
				}
				break;
			}
			case CONTACTS:{
				aB.setSubtitle(null);
				aB.setTitle(getString(R.string.section_contacts).toUpperCase());
				firstNavigationLevel = true;
				break;
			}
			case NOTIFICATIONS:{
				aB.setSubtitle(null);
				aB.setTitle(getString(R.string.title_properties_chat_contact_notifications).toUpperCase());
				firstNavigationLevel = true;
				break;
			}
			case CHAT:{
				tB.setVisibility(View.VISIBLE);
				aB.setTitle(getString(R.string.section_chat).toUpperCase());

				firstNavigationLevel = true;
				break;
			}
			case SEARCH:{
				aB.setSubtitle(null);
				if(textsearchQuery){
					if (getSearchFragment() != null) {
						sFLol.setAllowedMultiselect(true);
					}
				}
				if(parentHandleSearch==-1){
					firstNavigationLevel = true;
					if(searchQuery!=null){
						textSubmitted = true;
						if(!searchQuery.isEmpty()){
							aB.setTitle(getString(R.string.action_search).toUpperCase()+": "+searchQuery);
						}else{
							aB.setTitle(getString(R.string.action_search).toUpperCase()+": "+"");
						}
					}else{
						aB.setTitle(getString(R.string.action_search).toUpperCase()+": "+"");
					}

				}else{
					MegaNode parentNode = megaApi.getNodeByHandle(parentHandleSearch);
					if (parentNode != null){
						aB.setTitle(parentNode.getName());
						firstNavigationLevel = false;
					}
				}
				break;
			}
			case SETTINGS:{
				aB.setSubtitle(null);
				aB.setTitle(getString(R.string.action_settings).toUpperCase());
				firstNavigationLevel = true;
				break;
			}
			case ACCOUNT:{
				aB.setSubtitle(null);
				if(accountFragment==MY_ACCOUNT_FRAGMENT){
					aB.setTitle(getString(R.string.section_account).toUpperCase());
					setFirstNavigationLevel(true);
				}
				else if(accountFragment==UPGRADE_ACCOUNT_FRAGMENT){
					aB.setTitle(getString(R.string.action_upgrade_account).toUpperCase());
					setFirstNavigationLevel(false);
				}
				else{
					aB.setTitle(getString(R.string.section_account).toUpperCase());
					setFirstNavigationLevel(true);
				}
				break;
			}
			case TRANSFERS:{
				aB.setSubtitle(null);
				aB.setTitle(getString(R.string.section_transfers).toUpperCase());
				setFirstNavigationLevel(true);
				break;
			}
			case CAMERA_UPLOADS:{
				aB.setSubtitle(null);
				if(isSearchEnabled){
					setFirstNavigationLevel(false);
				}
				else{
					setFirstNavigationLevel(true);
					aB.setTitle(getString(R.string.section_photo_sync).toUpperCase());
				}
				break;
			}
			case MEDIA_UPLOADS:{
				aB.setSubtitle(null);
				if(isSearchEnabled){
					setFirstNavigationLevel(false);
				}
				else{
					setFirstNavigationLevel(true);
				}
				aB.setTitle(getString(R.string.section_secondary_media_uploads).toUpperCase());
				break;
			}
			default:{
				logDebug("Default GONE");

				break;
			}
		}

		updateNavigationToolbarIcon();
	}

	public void updateNavigationToolbarIcon(){
		int totalHistoric = megaApi.getNumUnreadUserAlerts();
		int totalIpc = 0;
		ArrayList<MegaContactRequest> requests = megaApi.getIncomingContactRequests();
		if(requests!=null) {
			totalIpc = requests.size();
		}

		int totalNotifications = totalHistoric + totalIpc;

		if(totalNotifications==0){
			if(isFirstNavigationLevel()){
				if (drawerItem == DrawerItem.SEARCH || drawerItem == DrawerItem.ACCOUNT || drawerItem == DrawerItem.INBOX || drawerItem == DrawerItem.CONTACTS || drawerItem == DrawerItem.NOTIFICATIONS
						|| drawerItem == DrawerItem.SETTINGS || drawerItem == DrawerItem.RUBBISH_BIN || drawerItem == DrawerItem.MEDIA_UPLOADS || drawerItem == DrawerItem.TRANSFERS){
					aB.setHomeAsUpIndicator(mutateIcon(this, R.drawable.ic_arrow_back_white, R.color.black));
				}
				else {
					aB.setHomeAsUpIndicator(mutateIcon(this, R.drawable.ic_menu_white, R.color.black));
				}
			}
			else{
				aB.setHomeAsUpIndicator(mutateIcon(this, R.drawable.ic_arrow_back_white, R.color.black));
			}
		}
		else{
			if(isFirstNavigationLevel()){
				if (drawerItem == DrawerItem.SEARCH || drawerItem == DrawerItem.ACCOUNT || drawerItem == DrawerItem.INBOX || drawerItem == DrawerItem.CONTACTS || drawerItem == DrawerItem.NOTIFICATIONS
						|| drawerItem == DrawerItem.SETTINGS || drawerItem == DrawerItem.RUBBISH_BIN || drawerItem == DrawerItem.MEDIA_UPLOADS || drawerItem == DrawerItem.TRANSFERS){
					badgeDrawable.setProgress(1.0f);
				}
				else {
					badgeDrawable.setProgress(0.0f);
				}
			}
			else{
				badgeDrawable.setProgress(1.0f);
			}

			if(totalNotifications>9){
				badgeDrawable.setText("9+");
			}
			else{
				badgeDrawable.setText(totalNotifications+"");
			}

			aB.setHomeAsUpIndicator(badgeDrawable);
		}
	}

	public void showOnlineMode(){
		logDebug("showOnlineMode");

		try {
			if (usedSpaceLayout != null) {

				if (rootNode != null) {
					Menu bNVMenu = bNV.getMenu();
					if (bNVMenu != null) {
						resetNavigationViewMenu(bNVMenu);
					}
					clickDrawerItemLollipop(drawerItem);

					if (getSettingsFragment() != null) {
						sttFLol.setOnlineOptions(true);
					}

					supportInvalidateOptionsMenu();

//					if (rChatFL != null) {
//						if (rChatFL.isAdded()) {
//							logDebug("ONLINE: Update screen RecentChats");
//							if (!isChatEnabled()) {
//								rChatFL.showDisableChatScreen();
//							}
//						}
//					}		updateAccountDetailsVisibleInfo();

					updateAccountDetailsVisibleInfo();
					checkCurrentStorageStatus(false);
				} else {
					logWarning("showOnlineMode - Root is NULL");
					if (getApplicationContext() != null) {
						if(((MegaApplication) getApplication()).getOpenChatId()!=-1){
							Intent intent = new Intent(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE_DIALOG);
							LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
						}
						else{
							showConfirmationConnect();
						}
					}
				}
			}
		}catch (Exception e){}
	}

	public void showConfirmationConnect(){
		logDebug("showConfirmationConnect");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						startConnection();
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						logDebug("showConfirmationConnect: BUTTON_NEGATIVE");
                        setToolbarTitle();
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		try {
			builder.setMessage(R.string.confirmation_to_reconnect).setPositiveButton(R.string.general_ok, dialogClickListener)
					.setNegativeButton(R.string.general_cancel, dialogClickListener);
            reconnectDialog = builder.create();
            reconnectDialog.setCanceledOnTouchOutside(false);
            reconnectDialog.show();
		}
		catch (Exception e){}
	}

	public void startConnection(){
		logDebug("startConnection");
		Intent intent = new Intent(this, LoginActivityLollipop.class);
		intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		finish();
	}

	public void showOfflineMode() {
		logDebug("showOfflineMode");

		try{
			if (megaApi == null) {
				logWarning("megaApi is Null in Offline mode");
			}

			if (usedSpaceLayout != null) {
				usedSpaceLayout.setVisibility(View.GONE);
			}

			UserCredentials credentials = dbH.getCredentials();
			if (credentials != null) {
				String emailCredentials = credentials.getEmail();
				if (emailCredentials != null) {
					if (nVEmail != null) {
						nVEmail.setText(emailCredentials);
					}
				}

				String myHandleCredentials = credentials.getMyHandle();
				long myHandle = -1;
				if (myHandleCredentials != null) {
					if (!myHandleCredentials.isEmpty()) {
						myHandle = Long.parseLong(myHandleCredentials);
					}
				}

				String firstNameText = credentials.getFirstName();
				String lastNameText = credentials.getLastName();
				String fullName = "";
				if (firstNameText == null) {
					firstNameText = "";
				}
				if (lastNameText == null) {
					lastNameText = "";
				}
				if (firstNameText.trim().length() <= 0) {
					fullName = lastNameText;
				} else {
					fullName = firstNameText + " " + lastNameText;
				}

				if (fullName.trim().length() <= 0) {
					logDebug("Put email as fullname");
					String[] splitEmail = emailCredentials.split("[@._]");
					fullName = splitEmail[0];
				}

				if (fullName.trim().length() <= 0) {
					fullName = getString(R.string.name_text) + " " + getString(R.string.lastname_text);
					logDebug("Full name set by default");
				}

				if (nVDisplayName != null) {
					nVDisplayName.setText(fullName);
				}

				setOfflineAvatar(emailCredentials, myHandle, fullName);
			}

			if (getSettingsFragment() != null) {
				sttFLol.setOnlineOptions(false);
			}

			logDebug("DrawerItem on start offline: " + drawerItem);
			if (drawerItem == null) {
				logWarning("drawerItem == null --> On start OFFLINE MODE");
				drawerItem = DrawerItem.SAVED_FOR_OFFLINE;
				if (bNV != null) {
					Menu bNVMenu = bNV.getMenu();
					if (bNVMenu != null) {
						disableNavigationViewMenu(bNVMenu);
					}
				}
				setBottomNavigationMenuItemChecked(OFFLINE_BNV);
				selectDrawerItemLollipop(drawerItem);
			} else {
				if (bNV != null) {
					Menu bNVMenu = bNV.getMenu();
					if (bNVMenu != null) {
						disableNavigationViewMenu(bNVMenu);
					}
				}
				logDebug("Change to OFFLINE MODE");
				clickDrawerItemLollipop(drawerItem);
			}

			supportInvalidateOptionsMenu();
		}catch(Exception e){}
	}

	public void clickDrawerItemLollipop(DrawerItem item){
		logDebug("Item: " + item);
		Menu bNVMenu = bNV.getMenu();
		if (bNVMenu != null){
			if(item==null){
				drawerMenuItem = bNVMenu.findItem(R.id.bottom_navigation_item_cloud_drive);
				onNavigationItemSelected(drawerMenuItem);
				return;
			}

			drawerLayout.closeDrawer(Gravity.LEFT);

			switch (item){
				case CLOUD_DRIVE:{
					setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV);
					break;
				}
				case SAVED_FOR_OFFLINE:{
					setBottomNavigationMenuItemChecked(OFFLINE_BNV);
					break;
				}
				case CAMERA_UPLOADS:{
					setBottomNavigationMenuItemChecked(CAMERA_UPLOADS_BNV);
					break;
				}
				case SHARED_ITEMS:{
					setBottomNavigationMenuItemChecked(SHARED_BNV);
					break;
				}
				case CHAT:{
					setBottomNavigationMenuItemChecked(CHAT_BNV);
					break;
				}
				case CONTACTS:
				case SETTINGS:
				case SEARCH:
				case ACCOUNT:
				case TRANSFERS:
				case MEDIA_UPLOADS:
				case NOTIFICATIONS:
				case INBOX:{
					setBottomNavigationMenuItemChecked(HIDDEN_BNV);
					break;
				}
			}
		}
	}

	public void selectDrawerItemSharedItems(){
		logDebug("selectDrawerItemSharedItems");
		tB.setVisibility(View.VISIBLE);

		try {
			NotificationManager notificationManager =
					(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

			notificationManager.cancel(NOTIFICATION_PUSH_CLOUD_DRIVE);
		}
		catch (Exception e){
			logError("Exception NotificationManager - remove contact notification", e);
		}

		if (sharesPageAdapter == null){
			logWarning("sharesPageAdapter is NULL");
			sharesPageAdapter = new SharesPageAdapter(getSupportFragmentManager(),this);
			viewPagerShares.setAdapter(sharesPageAdapter);
			tabLayoutShares.setupWithViewPager(viewPagerShares);
			setSharesTabIcons(indexShares);

			//Force on CreateView, addTab do not execute onCreateView
			if (indexShares != ERROR_TAB) {
				logDebug("The index of the TAB Shares is: " + indexShares);
				if (viewPagerShares != null){
					switch (indexShares) {
						case INCOMING_TAB:
						case OUTGOING_TAB:
						case LINKS_TAB:
							viewPagerShares.setCurrentItem(indexShares);
							break;
					}
				}
				indexShares = ERROR_TAB;
			}
			else {
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

		switch (tabSelected) {
			case OUTGOING_TAB:
				tabLayoutShares.getTabAt(INCOMING_TAB).setIcon(mutateIcon(getApplicationContext(), R.drawable.ic_incoming_shares, R.color.mail_my_account));
				tabLayoutShares.getTabAt(OUTGOING_TAB).setIcon(mutateIconSecondary(getApplicationContext(), R.drawable.ic_outgoing_shares, R.color.dark_primary_color));
				tabLayoutShares.getTabAt(LINKS_TAB).setIcon(mutateIcon(getApplicationContext(), R.drawable.link_ic, R.color.mail_my_account));
				break;
			case LINKS_TAB:
				tabLayoutShares.getTabAt(INCOMING_TAB).setIcon(mutateIcon(getApplicationContext(), R.drawable.ic_incoming_shares, R.color.mail_my_account));
				tabLayoutShares.getTabAt(OUTGOING_TAB).setIcon(mutateIcon(getApplicationContext(), R.drawable.ic_outgoing_shares, R.color.mail_my_account));
				tabLayoutShares.getTabAt(LINKS_TAB).setIcon(mutateIconSecondary(getApplicationContext(), R.drawable.link_ic, R.color.dark_primary_color));
				break;
			default:
				tabLayoutShares.getTabAt(INCOMING_TAB).setIcon(mutateIconSecondary(getApplicationContext(), R.drawable.ic_incoming_shares, R.color.dark_primary_color));
				tabLayoutShares.getTabAt(OUTGOING_TAB).setIcon(mutateIcon(getApplicationContext(), R.drawable.ic_outgoing_shares, R.color.mail_my_account));
				tabLayoutShares.getTabAt(LINKS_TAB).setIcon(mutateIcon(getApplicationContext(), R.drawable.link_ic, R.color.mail_my_account));

		}
	}

	public void selectDrawerItemContacts (){
		logDebug("selectDrawerItemContacts");
		tB.setVisibility(View.VISIBLE);

		try {
			ContactsAdvancedNotificationBuilder notificationBuilder;
			notificationBuilder =  ContactsAdvancedNotificationBuilder.newInstance(this, megaApi);

			notificationBuilder.removeAllIncomingContactNotifications();
			notificationBuilder.removeAllAcceptanceContactNotifications();
		}
		catch (Exception e){
			logError("Exception NotificationManager - remove all CONTACT notifications", e);
		}

		if (aB == null){
			aB = getSupportActionBar();
		}
		setToolbarTitle();

		if (contactsPageAdapter == null){
			logWarning("contactsPageAdapter == null");
			contactsPageAdapter = new ContactsPageAdapter(getSupportFragmentManager(),this);
			viewPagerContacts.setAdapter(contactsPageAdapter);
			tabLayoutContacts.setupWithViewPager(viewPagerContacts);

			logDebug("The index of the TAB CONTACTS is: " + indexContacts);
			if(indexContacts==-1) {
				logWarning("The index os contacts is -1");
				ArrayList<MegaContactRequest> requests = megaApi.getIncomingContactRequests();
				if(requests!=null) {
					int pendingRequest = requests.size();
					if (pendingRequest != 0) {
						indexContacts = 2;
					}
				}
			}

			if (viewPagerContacts != null) {
				switch (indexContacts){
					case SENT_REQUESTS_TAB:{
						viewPagerContacts.setCurrentItem(SENT_REQUESTS_TAB);
						logDebug("Select Sent Requests TAB");
						break;
					}
					case RECEIVED_REQUESTS_TAB:{
						viewPagerContacts.setCurrentItem(RECEIVED_REQUESTS_TAB);
						logDebug("Select Received Request TAB");
						break;
					}
					default:{
						viewPagerContacts.setCurrentItem(CONTACTS_TAB);
						logDebug("Select Contacts TAB");
						break;
					}
				}
			}
		}
		else {
			logDebug("contactsPageAdapter NOT null");
			cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CONTACTS.getTag());
			sRFLol = (SentRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.SENT_REQUESTS.getTag());
			rRFLol = (ReceivedRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECEIVED_REQUESTS.getTag());

			logDebug("The index of the TAB CONTACTS is: " + indexContacts);
			if (viewPagerContacts != null) {
				switch (indexContacts) {
					case SENT_REQUESTS_TAB: {
						viewPagerContacts.setCurrentItem(SENT_REQUESTS_TAB);
						logDebug("Select Sent Requests TAB");
						break;
					}
					case RECEIVED_REQUESTS_TAB: {
						viewPagerContacts.setCurrentItem(RECEIVED_REQUESTS_TAB);
						logDebug("Select Received Request TAB");
						break;
					}
					default: {
						viewPagerContacts.setCurrentItem(CONTACTS_TAB);
						logDebug("Select Contacts TAB");
						break;
					}
				}
			}
		}

		viewPagerContacts.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				indexContacts = position;
			}

			@Override
			public void onPageSelected(int position) {
				logDebug("onPageSelected");
				checkScrollElevation();
				indexContacts = position;
				cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CONTACTS.getTag());
				if(cFLol!=null){
					cFLol.hideMultipleSelect();
					cFLol.clearSelectionsNoAnimations();
				}
				sRFLol = (SentRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.SENT_REQUESTS.getTag());
				if(sRFLol!=null){
					sRFLol.clearSelections();
					sRFLol.hideMultipleSelect();
				}
				rRFLol = (ReceivedRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECEIVED_REQUESTS.getTag());
				if(rRFLol!=null){
					rRFLol.clearSelections();
					rRFLol.hideMultipleSelect();
				}
				supportInvalidateOptionsMenu();
				showFabButton();
			}

			@Override
			public void onPageScrollStateChanged(int state) {

			}
		});

		supportInvalidateOptionsMenu();
		drawerLayout.closeDrawer(Gravity.LEFT);
	}

	public void selectDrawerItemAccount(){

		if(((MegaApplication) getApplication()).getMyAccountInfo()!=null && ((MegaApplication) getApplication()).getMyAccountInfo().getNumVersions() == -1){
			megaApi.getFolderInfo(megaApi.getRootNode(), this);
		}

		switch(accountFragment){
			case UPGRADE_ACCOUNT_FRAGMENT:{
				showUpAF();
				break;
			}
			default:{
				app.refreshAccountInfo();
				accountFragment=MY_ACCOUNT_FRAGMENT;

				if (mTabsAdapterMyAccount == null){
					mTabsAdapterMyAccount = new MyAccountPageAdapter(getSupportFragmentManager(), this);
					viewPagerMyAccount.setAdapter(mTabsAdapterMyAccount);
					tabLayoutMyAccount.setupWithViewPager(viewPagerMyAccount);
				} else{
					maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
					mStorageFLol = (MyStorageFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_STORAGE.getTag());
				}

				if(viewPagerMyAccount != null) {
					switch (indexAccount){
						case STORAGE_TAB:{
							viewPagerMyAccount.setCurrentItem(STORAGE_TAB);
							break;
						}
						default:{
							indexAccount = GENERAL_TAB;
							viewPagerMyAccount.setCurrentItem(GENERAL_TAB);
							updateLogoutWarnings();
						}
					}
				}

				viewPagerMyAccount.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
					@Override
					public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
					}

					@Override
					public void onPageSelected(int position) {
						supportInvalidateOptionsMenu();
						checkScrollElevation();
					}

					@Override
					public void onPageScrollStateChanged(int state) {
					}
				});
				setToolbarTitle();
				supportInvalidateOptionsMenu();
				showFabButton();
				break;
			}
		}
	}

	public void selectDrawerItemNotifications(){
		logDebug("selectDrawerItemNotifications");

		tB.setVisibility(View.VISIBLE);

		drawerItem = DrawerItem.NOTIFICATIONS;

		setBottomNavigationMenuItemChecked(HIDDEN_BNV);

		notificFragment = (NotificationsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.NOTIFICATIONS.getTag());
		if (notificFragment == null){
			logWarning("New NotificationsFragment");
			notificFragment = NotificationsFragmentLollipop.newInstance();
		}
		else {
			refreshFragment(FragmentTag.NOTIFICATIONS.getTag());
		}
        replaceFragment(notificFragment, FragmentTag.NOTIFICATIONS.getTag());

		setToolbarTitle();

		showFabButton();
	}

	public void selectDrawerItemTransfers(){
		logDebug("selectDrawerItemTransfers");

		tB.setVisibility(View.VISIBLE);

		drawerItem = DrawerItem.TRANSFERS;

		setBottomNavigationMenuItemChecked(HIDDEN_BNV);

		if (mTabsAdapterTransfers == null) {
			mTabsAdapterTransfers = new TransfersPageAdapter(getSupportFragmentManager(), this);
			viewPagerTransfers.setAdapter(mTabsAdapterTransfers);
			tabLayoutTransfers.setupWithViewPager(viewPagerTransfers);
		} else {
			tFLol = (TransfersFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.TRANSFERS.getTag());
			completedTFLol = (CompletedTransfersFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.COMPLETED_TRANSFERS.getTag());
		}

		if (viewPagerTransfers != null) {
			switch (indexTransfers) {
				case COMPLETED_TAB:
					viewPagerTransfers.setCurrentItem(COMPLETED_TAB);
					break;

				default:
					viewPagerTransfers.setCurrentItem(PENDING_TAB);
					break;
			}
		}

		setToolbarTitle();

		showFabButton();

		drawerLayout.closeDrawer(Gravity.LEFT);
	}

	public void selectDrawerItemChat(){
		logDebug("selectDrawerItemChat");

		((MegaApplication)getApplication()).setRecentChatVisible(true);

		try {
			ChatAdvancedNotificationBuilder notificationBuilder;
			notificationBuilder =  ChatAdvancedNotificationBuilder.newInstance(this, megaApi, megaChatApi);

			notificationBuilder.removeAllChatNotifications();
		}
		catch (Exception e){
			logError("Exception NotificationManager - remove all notifications", e);
		}

		setToolbarTitle();

		rChatFL = (RecentChatsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
		if (rChatFL == null){
			rChatFL = RecentChatsFragmentLollipop.newInstance();
			replaceFragment(rChatFL, FragmentTag.RECENT_CHAT.getTag());
		}
		else{
			refreshFragment(FragmentTag.RECENT_CHAT.getTag());
			replaceFragment(rChatFL, FragmentTag.RECENT_CHAT.getTag());
		}

		drawerLayout.closeDrawer(Gravity.LEFT);
	}

	public void setBottomNavigationMenuItemChecked (int item) {
		if (bNV != null && bNV.getMenu() != null) {
			if(item == HIDDEN_BNV) {
				showHideBottomNavigationView(true);
			}
			else if (bNV.getMenu().getItem(item) != null) {
				if (!bNV.getMenu().getItem(item).isChecked()) {
					bNV.getMenu().getItem(item).setChecked(true);
				}
			}
		}
	}

	private void setTabsVisibility() {
    	tabLayoutCloud.setVisibility(View.GONE);
    	viewPagerCloud.setVisibility(View.GONE);
		tabLayoutContacts.setVisibility(View.GONE);
		viewPagerContacts.setVisibility(View.GONE);
		tabLayoutShares.setVisibility(View.GONE);
		viewPagerShares.setVisibility(View.GONE);
		tabLayoutMyAccount.setVisibility(View.GONE);
		viewPagerMyAccount.setVisibility(View.GONE);
		tabLayoutTransfers.setVisibility(View.GONE);
		viewPagerTransfers.setVisibility(View.GONE);

    	fragmentContainer.setVisibility(View.GONE);

    	if (turnOnNotifications) {
			fragmentContainer.setVisibility(View.VISIBLE);
			drawerLayout.closeDrawer(Gravity.LEFT);
			return;
		}

    	switch (drawerItem) {
			case CLOUD_DRIVE: {
				if (getTabItemCloud() == CLOUD_TAB
						|| (getTabItemCloud() == RECENTS_TAB && getDeepBrowserTreeRecents() == 0)) {
					tabLayoutCloud.setVisibility(View.VISIBLE);
				}
				viewPagerCloud.setVisibility(View.VISIBLE);
				break;
			}
			case SHARED_ITEMS: {
				tabLayoutShares.setVisibility(View.VISIBLE);
				viewPagerShares.setVisibility(View.VISIBLE);
				break;
			}
			case CONTACTS: {
				tabLayoutContacts.setVisibility(View.VISIBLE);
				viewPagerContacts.setVisibility(View.VISIBLE);
				break;
			}
			case ACCOUNT: {
				switch(accountFragment){
					case CENTILI_FRAGMENT:
					case FORTUMO_FRAGMENT:
					case CC_FRAGMENT:
					case UPGRADE_ACCOUNT_FRAGMENT:
					case BACKUP_RECOVERY_KEY_FRAGMENT:{
						fragmentContainer.setVisibility(View.VISIBLE);
						break;
					}
					default:{
						tabLayoutMyAccount.setVisibility(View.VISIBLE);
						viewPagerMyAccount.setVisibility(View.VISIBLE);
						break;
					}
				}
				break;
			}
			case TRANSFERS: {
				tabLayoutTransfers.setVisibility(View.VISIBLE);
				viewPagerTransfers.setVisibility(View.VISIBLE);
				break;
			}
			default: {
				fragmentContainer.setVisibility(View.VISIBLE);
				break;
			}
		}

		drawerLayout.closeDrawer(Gravity.LEFT);
	}

	private void removeFragment(Fragment fragment) {
		if (fragment != null && fragment.isAdded()) {
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.remove(fragment);
			ft.commit();
		}
	}

	@SuppressLint("NewApi")
	public void selectDrawerItemLollipop(DrawerItem item) {
    	if (item == null) {
    		logWarning("The selected DrawerItem is NULL. Using latest or default value.");
    		item = drawerItem != null ? drawerItem : DrawerItem.CLOUD_DRIVE;
		}

    	logDebug("Selected DrawerItem: " + item.name());

    	drawerItem = item;
		((MegaApplication)getApplication()).setRecentChatVisible(false);
		resetActionBar(aB);
		setTransfersWidget();
		setCallWidget();

		if (item != DrawerItem.CHAT) {
			//remove recent chat fragment as its life cycle get triggered unexpectedly, e.g. rotate device while not on recent chat page
			removeFragment(rChatFL);
		}

    	switch (item){
			case CLOUD_DRIVE:{
				selectDrawerItemCloudDrive();
				if (openFolderRefresh){
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
				logDebug("END for Cloud Drive");
    			break;
    		}
			case RUBBISH_BIN:{
				showHideBottomNavigationView(true);
				tB.setVisibility(View.VISIBLE);
				rubbishBinFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
				if (rubbishBinFLol == null) {
					rubbishBinFLol = RubbishBinFragmentLollipop.newInstance();
				}

				setBottomNavigationMenuItemChecked(HIDDEN_BNV);

				replaceFragment(rubbishBinFLol, FragmentTag.RUBBISH_BIN.getTag());

				if (openFolderRefresh){
					onNodesCloudDriveUpdate();
					openFolderRefresh = false;
				}
				supportInvalidateOptionsMenu();
				setToolbarTitle();
				showFabButton();
				break;
			}
    		case SAVED_FOR_OFFLINE:{

				tB.setVisibility(View.VISIBLE);

				oFLol = (OfflineFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.OFFLINE.getTag());
    			if (oFLol == null){
					logWarning("New OfflineFragment");
    				oFLol = new OfflineFragmentLollipop();
    			}

				replaceFragment(oFLol, FragmentTag.OFFLINE.getTag());

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
						ActivityCompat.requestPermissions(this,
								new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
								REQUEST_WRITE_STORAGE);
					}
				}

    			supportInvalidateOptionsMenu();
    			setToolbarTitle();
				showFabButton();
				showHideBottomNavigationView(false);
				if (!comesFromNotifications) {
					bottomNavigationCurrentItem = OFFLINE_BNV;
				}
				setBottomNavigationMenuItemChecked(OFFLINE_BNV);
    			break;
    		}
    		case CAMERA_UPLOADS:{
				tB.setVisibility(View.VISIBLE);
				cuFL = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CAMERA_UPLOADS.getTag());
				if (cuFL == null) {
					cuFL = CameraUploadFragmentLollipop.newInstance(CameraUploadFragmentLollipop.TYPE_CAMERA);
				} else {
					refreshFragment(FragmentTag.CAMERA_UPLOADS.getTag());
				}

				replaceFragment(cuFL, FragmentTag.CAMERA_UPLOADS.getTag());

				setToolbarTitle();
    			supportInvalidateOptionsMenu();
				showFabButton();
				showHideBottomNavigationView(false);
				if (!comesFromNotifications) {
					bottomNavigationCurrentItem = CAMERA_UPLOADS_BNV;
				}
				setBottomNavigationMenuItemChecked(CAMERA_UPLOADS_BNV);
      			break;
    		}
    		case MEDIA_UPLOADS:{
				tB.setVisibility(View.VISIBLE);

				setBottomNavigationMenuItemChecked(HIDDEN_BNV);

				muFLol = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MEDIA_UPLOADS.getTag());
				if (muFLol == null) {
					muFLol = CameraUploadFragmentLollipop.newInstance(CameraUploadFragmentLollipop.TYPE_MEDIA);
				} else {
					refreshFragment(FragmentTag.MEDIA_UPLOADS.getTag());
				}

				replaceFragment(muFLol, FragmentTag.MEDIA_UPLOADS.getTag());

    			supportInvalidateOptionsMenu();
    			setToolbarTitle();
				showFabButton();
				showHideBottomNavigationView(false);
				if (!comesFromNotifications) {
					bottomNavigationCurrentItem = MEDIA_UPLOADS_BNV;
				}
				setBottomNavigationMenuItemChecked(HIDDEN_BNV);
      			break;
    		}
    		case INBOX:{
				showHideBottomNavigationView(true);
				tB.setVisibility(View.VISIBLE);
				iFLol = (InboxFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.INBOX.getTag());
				if (iFLol == null) {
					iFLol = InboxFragmentLollipop.newInstance();
				}

				replaceFragment(iFLol, FragmentTag.INBOX.getTag());

				if (openFolderRefresh){
					onNodesInboxUpdate();
					openFolderRefresh = false;
				}
    			supportInvalidateOptionsMenu();
				setToolbarTitle();
				showFabButton();
    			break;
    		}
    		case SHARED_ITEMS:{
				selectDrawerItemSharedItems();
				if (openFolderRefresh){
					onNodesSharedUpdate();
					openFolderRefresh = false;
				}
    			supportInvalidateOptionsMenu();

				showFabButton();
				showHideBottomNavigationView(false);
				if (!comesFromNotifications) {
					bottomNavigationCurrentItem = SHARED_BNV;
				}
				setBottomNavigationMenuItemChecked(SHARED_BNV);
    			break;
    		}
    		case CONTACTS:{
				showHideBottomNavigationView(true);
				selectDrawerItemContacts();
				showFabButton();
    			break;
    		}
			case NOTIFICATIONS:{
				showHideBottomNavigationView(true);
				selectDrawerItemNotifications();
				supportInvalidateOptionsMenu();
				showFabButton();
				break;
			}
    		case SETTINGS:{
				showHideBottomNavigationView(true);
				if(((MegaApplication) getApplication()).getMyAccountInfo()!=null && ((MegaApplication) getApplication()).getMyAccountInfo().getNumVersions() == -1){
					megaApi.getFolderInfo(megaApi.getRootNode(), this);
				}

				aB.setSubtitle(null);
				tB.setVisibility(View.VISIBLE);

    			supportInvalidateOptionsMenu();

    			if (getSettingsFragment() != null){
					if (openSettingsStorage){
						sttFLol.goToCategoryStorage();
					}
					else if (openSettingsQR){
						logDebug ("goToCategoryQR");
						sttFLol.goToCategoryQR();
					}
				}
				else {
					sttFLol = new SettingsFragmentLollipop();
				}

				replaceFragment(sttFLol, FragmentTag.SETTINGS.getTag());

				setToolbarTitle();
				supportInvalidateOptionsMenu();
				showFabButton();

				if (sttFLol != null){
					sttFLol.update2FAVisibility();
				}
				break;
    		}
    		case SEARCH:{
				showHideBottomNavigationView(true);

				setBottomNavigationMenuItemChecked(HIDDEN_BNV);

    			drawerItem = DrawerItem.SEARCH;
				if (getSearchFragment() == null) {
					sFLol = SearchFragmentLollipop.newInstance();
				} else {
					refreshFragment(FragmentTag.SEARCH.getTag());
				}

				replaceFragment(sFLol, FragmentTag.SEARCH.getTag());
				showFabButton();

    			break;
    		}
			case ACCOUNT:{
				showHideBottomNavigationView(true);
				logDebug("Case ACCOUNT: " + accountFragment);
//    			tB.setVisibility(View.GONE);
				aB.setSubtitle(null);
				selectDrawerItemAccount();
				supportInvalidateOptionsMenu();
				break;
			}
    		case TRANSFERS:{
				showHideBottomNavigationView(true);
				aB.setSubtitle(null);
				selectDrawerItemTransfers();
    			supportInvalidateOptionsMenu();
				showFabButton();
    			break;
    		}
			case CHAT:{
				logDebug("Chat selected");
				if (megaApi != null) {
					contacts = megaApi.getContacts();
					for (int i=0;i<contacts.size();i++){
						if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){
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

	public boolean isOnRecents() {
		if (getDrawerItem() == DrawerItem.CLOUD_DRIVE && getTabItemCloud() == RECENTS_TAB ) return true;

		return false;
	}

	private boolean isCloudAdded () {
    	if (cloudPageAdapter == null) return false;

		fbFLol = (FileBrowserFragmentLollipop) cloudPageAdapter.instantiateItem(viewPagerCloud, 0);
		if (fbFLol != null && fbFLol.isAdded()) return true;

		return false;
	}

	public boolean isRecentsAdded () {
		if (cloudPageAdapter == null) return false;

		rF = (RecentsFragment) cloudPageAdapter.instantiateItem(viewPagerCloud, 1);
		if (rF != null && rF.isAdded()) return true;

    	return false;
	}

	private boolean isIncomingAdded () {
    	if (sharesPageAdapter == null) return false;

		inSFLol = (IncomingSharesFragmentLollipop) sharesPageAdapter.instantiateItem(viewPagerShares, INCOMING_TAB);

    	return inSFLol != null && inSFLol.isAdded();
	}

	private boolean isOutgoingAdded() {
    	if (sharesPageAdapter == null) return false;

		outSFLol = (OutgoingSharesFragmentLollipop) sharesPageAdapter.instantiateItem(viewPagerShares, OUTGOING_TAB);

		return outSFLol != null && outSFLol.isAdded();
	}

	private boolean isLinksAdded() {
    	if (sharesPageAdapter == null) return false;

    	lF = (LinksFragment) sharesPageAdapter.instantiateItem(viewPagerShares, LINKS_TAB);

    	return lF != null && lF.isAdded();
	}

    public void checkScrollElevation() {
    	if(drawerItem==null){
    		return;
		}

        switch (drawerItem) {
            case CLOUD_DRIVE: {
            	if (getTabItemCloud() == CLOUD_TAB && isCloudAdded()) fbFLol.checkScroll();
				else if (getTabItemCloud() == RECENTS_TAB && isRecentsAdded()) rF.checkScroll();
                break;
            }
            case SAVED_FOR_OFFLINE: {
				oFLol = (OfflineFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.OFFLINE.getTag());
                if (oFLol != null) {
                    oFLol.checkScroll();
                }
                break;
            }
            case CAMERA_UPLOADS: {
				cuFL = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CAMERA_UPLOADS.getTag());
                if (cuFL != null) {
                    cuFL.checkScroll();
                }
                break;
            }
            case INBOX: {
            	iFLol = (InboxFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.INBOX.getTag());
                if (iFLol != null) {
                    iFLol.checkScroll();
                }
                break;
            }
            case SHARED_ITEMS: {
            	if (getTabItemShares() == INCOMING_TAB && isIncomingAdded()) inSFLol.checkScroll();
            	else if (getTabItemShares() == OUTGOING_TAB && isOutgoingAdded()) outSFLol.checkScroll();
            	else if (getTabItemShares() == LINKS_TAB && isLinksAdded()) lF.checkScroll();
                break;
            }
            case CONTACTS: {
				cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CONTACTS.getTag());
				rRFLol = (ReceivedRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECEIVED_REQUESTS.getTag());
				sRFLol = (SentRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.SENT_REQUESTS.getTag());
                if (getTabItemContacts() == CONTACTS_TAB && cFLol != null) {
                    cFLol.checkScroll();
                }
                else if (getTabItemContacts() == SENT_REQUESTS_TAB && sRFLol != null) {
                    sRFLol.checkScroll();
                }
                else if (getTabItemContacts() == RECEIVED_REQUESTS_TAB && rRFLol != null) {
                    rRFLol.checkScroll();
                }
                break;
            }
            case SETTINGS: {
                if (getSettingsFragment() != null) {
                    sttFLol.checkScroll();
                }
                break;
            }
            case ACCOUNT: {
				mStorageFLol = (MyStorageFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_STORAGE.getTag());
				maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
                if (getTabItemMyAccount() == GENERAL_TAB && maFLol != null) {
                    maFLol.checkScroll();
                }
                else if (getTabItemMyAccount() == STORAGE_TAB && mStorageFLol != null) {
                    mStorageFLol.checkScroll();
                }
                break;
            }
            case SEARCH: {
				if (getSearchFragment() != null) {
                    sFLol.checkScroll();
                }
                break;
            }
            case MEDIA_UPLOADS: {
				muFLol = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MEDIA_UPLOADS.getTag());
                if (muFLol != null) {
                    muFLol.checkScroll();
                }
                break;
            }
            case CHAT: {
				rChatFL = (RecentChatsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
                if (rChatFL != null) {
                    rChatFL.checkScroll();
                }
                break;
            }
            case RUBBISH_BIN: {
				rubbishBinFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
                if (rubbishBinFLol != null) {
                    rubbishBinFLol.checkScroll();
                }
                break;
            }
        }
    }


    void showEnable2FADialog () {
		logDebug ("newAccount: "+newAccount);
		newAccount = false;

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
		}catch (Exception e){};
		isEnable2FADialogShown = true;
	}

	public void moveToSettingsSection(){
		drawerItem=DrawerItem.SETTINGS;
		selectDrawerItemLollipop(drawerItem);
	}

	public void moveToSettingsSectionStorage(){
		openSettingsStorage = true;
		drawerItem=DrawerItem.SETTINGS;
		selectDrawerItemLollipop(drawerItem);
	}

	public void moveToSettingsSectionQR(){
		openSettingsQR = true;
		drawerItem=DrawerItem.SETTINGS;
		selectDrawerItemLollipop(drawerItem);
	}

	public void moveToChatSection (long idChat) {
		if (idChat != -1) {
			Intent intent = new Intent(this, ChatActivityLollipop.class);
			intent.setAction(ACTION_CHAT_SHOW_MESSAGES);
			intent.putExtra("CHAT_ID", idChat);
			this.startActivity(intent);
		}
    	drawerItem = DrawerItem.CHAT;
    	selectDrawerItemLollipop(drawerItem);
	}

	public void showMyAccount(){
		drawerItem = DrawerItem.ACCOUNT;
		selectDrawerItemLollipop(drawerItem);
	}

	public void showCC(int type, int payMonth, boolean refresh){
		accountFragment = CC_FRAGMENT;

		ccFL = (CreditCardFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CREDIT_CARD.getTag());
		if (ccFL == null) {
			ccFL = new CreditCardFragmentLollipop();
			ccFL.setInfo(type, payMonth);
			replaceFragment(ccFL, FragmentTag.CREDIT_CARD.getTag());
		}
		else if (refresh) {
			refreshFragment(FragmentTag.CREDIT_CARD.getTag());
		}
		else {
			ccFL.setInfo(type, payMonth);
			replaceFragment(ccFL, FragmentTag.CREDIT_CARD.getTag());
		}

        setTabsVisibility();
	}

	public void updateInfoNumberOfSubscriptions(){
        if (cancelSubscription != null){
            cancelSubscription.setVisible(false);
        }
        if (((MegaApplication) getApplication()).getMyAccountInfo()!= null && ((MegaApplication) getApplication()).getMyAccountInfo().getNumberOfSubscriptions() > 0){
            if (cancelSubscription != null){
                if (drawerItem == DrawerItem.ACCOUNT){
					maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
                    if (maFLol != null){
                        cancelSubscription.setVisible(true);
                    }
                }
            }
        }
    }

	public void showFortumo(){
		fFL = (FortumoFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.FORTUMO.getTag());
		if (fFL == null){
			fFL = new FortumoFragmentLollipop();
		}
		replaceFragment(fFL, FragmentTag.FORTUMO.getTag());
		setTabsVisibility();
	}

	public void showCentili(){
		accountFragment = CENTILI_FRAGMENT;

		ctFL = (CentiliFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CENTILI.getTag());
		if (ctFL == null){
			ctFL = new CentiliFragmentLollipop();
		}
		replaceFragment(ctFL, FragmentTag.CENTILI.getTag());
		setTabsVisibility();
	}

	public void showUpAF() {
		logDebug("showUpAF");
		accountFragment = UPGRADE_ACCOUNT_FRAGMENT;
		setToolbarTitle();
		upAFL = new UpgradeAccountFragmentLollipop();
		replaceFragment(upAFL, FragmentTag.UPGRADE_ACCOUNT.getTag());
		setTabsVisibility();
		supportInvalidateOptionsMenu();
		showFabButton();
	}

	private void closeSearchSection() {
    	searchQuery = "";
		drawerItem = searchDrawerItem;
		selectDrawerItemLollipop(drawerItem);
		searchDrawerItem = null;
	}

	private void offlineSearch() {
		oFLol = (OfflineFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.OFFLINE.getTag());
		if (oFLol != null) {
			oFLol.filterOffline(searchQuery);
		}
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		logDebug("onCreateOptionsMenuLollipop");
		// Force update the toolbar title to make the the tile length to be updated
		setToolbarTitle();
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_manager, menu);

		final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchMenuItem = menu.findItem(R.id.action_search);
		searchMenuItem.setIcon(mutateIcon(this, R.drawable.ic_menu_search, R.color.black));

		searchView = (SearchView) searchMenuItem.getActionView();

		SearchView.SearchAutoComplete searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
		searchAutoComplete.setTextColor(ContextCompat.getColor(this, R.color.black));
		searchAutoComplete.setHintTextColor(ContextCompat.getColor(this, R.color.status_bar_login));
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
				if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE) {
					if (!isOfflineSearchPathEmpty()) {
						offlineSearchPaths.clear();
					}
					offlineSearch();
				} else if (drawerItem != DrawerItem.CHAT) {
					textsearchQuery = false;
					firstNavigationLevel = true;
					parentHandleSearch = -1;
					levelsSearch = -1;
					setSearchDrawerItem();
					selectDrawerItemLollipop(drawerItem);
				} else if (drawerItem == DrawerItem.CHAT){
					resetActionBar(aB);
				}
				hideCallMenuItem();
				hideCallWidget();
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				logDebug("onMenuItemActionCollapse()");
				searchExpand = false;
				setCallWidget();
				setCallMenuItem();
				if (drawerItem == DrawerItem.CHAT) {
					rChatFL = (RecentChatsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
					if (rChatFL != null) {
						rChatFL.closeSearch();
						rChatFL.setCustomisedActionBar();
						supportInvalidateOptionsMenu();
					}
				} else if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE) {
					oFLol = (OfflineFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.OFFLINE.getTag());
					if (oFLol != null) {
						oFLol.closeSearch();
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
				} else if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE) {
					searchExpand = false;
					textSubmitted = true;
					hideKeyboard(managerActivity, 0);
					oFLol = (OfflineFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.OFFLINE.getTag());
					if (oFLol != null) {
						addOfflineSearchPath(oFLol.getPathNavigation());
						addOfflineSearchPath(OFFLINE_SEARCH_QUERY + searchQuery);
						setToolbarTitle();
						supportInvalidateOptionsMenu();
					}
				} else {
					searchExpand = false;
					searchQuery = "" + query;
					setToolbarTitle();
					logDebug("Search query: " + query);
					textSubmitted = true;
					showFabButton();
					supportInvalidateOptionsMenu();
				}
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				logDebug("onQueryTextChange");
				if (drawerItem == DrawerItem.CHAT) {
					searchQuery = newText;
					rChatFL = (RecentChatsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
					if (rChatFL != null) {
						rChatFL.filterChats(newText);
					}
				} else if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE) {
					if (textSubmitted) {
						textSubmitted = false;
						return true;
					}

					searchQuery = newText;
					offlineSearch();
				} else {
					getSearchFragment();

					if (textSubmitted) {
						if (sFLol != null) {
							sFLol.setAllowedMultiselect(true);
						}
						textSubmitted = false;
					} else {
						if (!textsearchQuery) {
							searchQuery = newText;
						}
						if (sFLol != null) {
							sFLol.newSearchNodesTask();
						}
					}
				}
				return true;
			}
		});

		gridSmallLargeMenuItem = menu.findItem(R.id.action_grid_view_large_small);
		addContactMenuItem = menu.findItem(R.id.action_add_contact);
		addMenuItem = menu.findItem(R.id.action_add);
		createFolderMenuItem = menu.findItem(R.id.action_new_folder);
		importLinkMenuItem = menu.findItem(R.id.action_import_link);
		selectMenuItem = menu.findItem(R.id.action_select);
		unSelectMenuItem = menu.findItem(R.id.action_unselect);
		thumbViewMenuItem = menu.findItem(R.id.action_grid);
		refreshMenuItem = menu.findItem(R.id.action_menu_refresh);
		sortByMenuItem = menu.findItem(R.id.action_menu_sort_by);
		helpMenuItem = menu.findItem(R.id.action_menu_help);
		upgradeAccountMenuItem = menu.findItem(R.id.action_menu_upgrade_account);
		rubbishBinMenuItem = menu.findItem(R.id.action_menu_rubbish_bin);
		clearRubbishBinMenuitem = menu.findItem(R.id.action_menu_clear_rubbish_bin);
		cancelAllTransfersMenuItem = menu.findItem(R.id.action_menu_cancel_all_transfers);
		clearCompletedTransfers = menu.findItem(R.id.action_menu_clear_completed_transfers);
		playTransfersMenuIcon = menu.findItem(R.id.action_play);
		playTransfersMenuIcon.setIcon(mutateIconSecondary(this, R.drawable.ic_play_white, R.color.black));
		pauseTransfersMenuIcon = menu.findItem(R.id.action_pause);
		pauseTransfersMenuIcon.setIcon(mutateIconSecondary(this, R.drawable.ic_pause_white, R.color.black));
		scanQRcodeMenuItem = menu.findItem(R.id.action_scan_qr);
		takePicture = menu.findItem(R.id.action_take_picture);
		searchByDate = menu.findItem(R.id.action_search_by_date);
		cancelSubscription = menu.findItem(R.id.action_menu_cancel_subscriptions);
		exportMK = menu.findItem(R.id.action_menu_export_MK);
		killAllSessions = menu.findItem(R.id.action_menu_kill_all_sessions);
		logoutMenuItem = menu.findItem(R.id.action_menu_logout);
		forgotPassMenuItem = menu.findItem(R.id.action_menu_forgot_pass);
		inviteMenuItem = menu.findItem(R.id.action_menu_invite);
		returnCallMenuItem = menu.findItem(R.id.action_return_call);
		RelativeLayout rootView = (RelativeLayout) returnCallMenuItem.getActionView();
		layoutCallMenuItem = rootView.findViewById(R.id.layout_menu_call);
		chronometerMenuItem = rootView.findViewById(R.id.chrono_menu);
		chronometerMenuItem.setVisibility(View.GONE);

		rootView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onOptionsItemSelected(returnCallMenuItem);
			}
		});

		changePass = menu.findItem(R.id.action_menu_change_pass);

		if (bNV != null) {
			Menu bNVMenu = bNV.getMenu();
			if (bNVMenu != null) {
				if (drawerItem == null) {
					drawerItem = DrawerItem.CLOUD_DRIVE;
				}

				if (drawerItem == DrawerItem.CLOUD_DRIVE) {
					setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV);
				}
			}
		}

		setCallMenuItem();

		if (isOnline(this)) {
			switch (drawerItem) {
				case CLOUD_DRIVE:
					rubbishBinMenuItem.setVisible(true);
					upgradeAccountMenuItem.setVisible(true);
					importLinkMenuItem.setVisible(true);
					addMenuItem.setEnabled(true);
					addMenuItem.setVisible(true);
					takePicture.setVisible(true);

					if (getTabItemCloud() == CLOUD_TAB) {
						createFolderMenuItem.setVisible(true);

						if (isCloudAdded() && fbFLol.getItemCount() > 0) {
							thumbViewMenuItem.setVisible(true);
							setGridListIcon();
							searchMenuItem.setVisible(true);
							sortByMenuItem.setVisible(true);
						}
					}
					break;

				case RUBBISH_BIN:
					if (getRubbishBinFragment() != null && rubbishBinFLol.getItemCount() > 0) {
						thumbViewMenuItem.setVisible(true);
						setGridListIcon();
						clearRubbishBinMenuitem.setVisible(true);
						sortByMenuItem.setVisible(true);
						searchMenuItem.setVisible(true);
					}
					break;

				case SAVED_FOR_OFFLINE:
					if (searchExpand) {
						openSearchView();
					} else {
						rubbishBinMenuItem.setVisible(true);

						if (getOfflineFragment() != null && oFLol.getItemCount() > 0) {
							thumbViewMenuItem.setVisible(true);
							setGridListIcon();
							sortByMenuItem.setVisible(true);
							searchMenuItem.setVisible(true);
						}
					}
					break;

				case CAMERA_UPLOADS:
				case MEDIA_UPLOADS:
					gridSmallLargeMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
					rubbishBinMenuItem.setVisible(true);

					if ((drawerItem == DrawerItem.CAMERA_UPLOADS && getCameraUploadFragment() != null && cuFL.getItemCount() > 0)
							|| (drawerItem == DrawerItem.MEDIA_UPLOADS && getMediaUploadFragment() != null && muFLol.getItemCount() > 0)) {
						sortByMenuItem.setVisible(true);
						thumbViewMenuItem.setVisible(true);

						if (firstNavigationLevel) {
							searchByDate.setVisible(true);
						}

						if (isListCameraUploads) {
							thumbViewMenuItem.setTitle(getString(R.string.action_grid));
							thumbViewMenuItem.setIcon(mutateIcon(this, R.drawable.ic_thumbnail_view, R.color.black));
						} else {
							thumbViewMenuItem.setTitle(getString(R.string.action_list));
							thumbViewMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
							if (isSmallGridCameraUploads) {
								gridSmallLargeMenuItem.setIcon(mutateIcon(this, R.drawable.ic_thumbnail_view, R.color.black));
							} else {
								gridSmallLargeMenuItem.setIcon(mutateIcon(this, R.drawable.ic_menu_gridview_small, R.color.black));
							}
							gridSmallLargeMenuItem.setVisible(true);
						}
					}
					break;

				case INBOX:
					if (getInboxFragment() != null && iFLol.getItemCount() > 0) {
						selectMenuItem.setVisible(true);
						sortByMenuItem.setVisible(true);
						searchMenuItem.setVisible(true);
						thumbViewMenuItem.setVisible(true);
						setGridListIcon();
					}
					break;

				case SHARED_ITEMS:
					if (getTabItemShares() == INCOMING_TAB && isIncomingAdded()) {
						addMenuItem.setEnabled(true);
						rubbishBinMenuItem.setVisible(true);

						if (isIncomingAdded() && inSFLol.getItemCount() > 0) {
							thumbViewMenuItem.setVisible(true);
							setGridListIcon();
							sortByMenuItem.setVisible(true);
							searchMenuItem.setVisible(true);

							if (parentHandleIncoming != INVALID_HANDLE) {
								MegaNode node = megaApi.getNodeByHandle(parentHandleIncoming);
								if (node != null) {
									int accessLevel = megaApi.getAccess(node);

									switch (accessLevel) {
										case MegaShare.ACCESS_OWNER:
										case MegaShare.ACCESS_READWRITE:
										case MegaShare.ACCESS_FULL: {
											addMenuItem.setVisible(true);
											createFolderMenuItem.setVisible(true);
											break;
										}
									}
								}
							}
						}
					} else if (getTabItemShares() == OUTGOING_TAB && isOutgoingAdded()) {
						rubbishBinMenuItem.setVisible(true);

						if (parentHandleOutgoing != INVALID_HANDLE) {
							addMenuItem.setVisible(true);
							createFolderMenuItem.setVisible(true);
						}

						if (isOutgoingAdded() && outSFLol.getItemCount() > 0) {
							thumbViewMenuItem.setVisible(true);
							setGridListIcon();
							sortByMenuItem.setVisible(true);
							searchMenuItem.setVisible(true);
						}
					} else if (getTabItemShares() == LINKS_TAB && isLinksAdded()) {
						rubbishBinMenuItem.setVisible(true);

						if (isLinksAdded() && lF.getItemCount() > 0) {
							sortByMenuItem.setVisible(true);
							searchMenuItem.setVisible(true);
						}
					}
					break;

				case CONTACTS:
					if (getTabItemContacts() == CONTACTS_TAB) {
						scanQRcodeMenuItem.setVisible(true);
						addContactMenuItem.setVisible(true);

						if (getContactsFragment() != null && cFLol.getItemCount() > 0) {
							thumbViewMenuItem.setVisible(true);
							setGridListIcon();
							sortByMenuItem.setVisible(true);

						}

						if (handleInviteContact != -1 && cFLol != null) {
							cFLol.invite(handleInviteContact);
						}
					} else if (getTabItemContacts() == SENT_REQUESTS_TAB) {
						addContactMenuItem.setVisible(true);
						upgradeAccountMenuItem.setVisible(true);
						scanQRcodeMenuItem.setVisible(true);
					}
					break;

				case SEARCH:
					if (searchExpand) {
						openSearchView();
					} else {
						rubbishBinMenuItem.setVisible(true);
						if (getSearchFragment() != null
								&& getSearchFragment().getNodes() != null
								&& getSearchFragment().getNodes().size() > 0) {
							sortByMenuItem.setVisible(true);
							thumbViewMenuItem.setVisible(true);
							setGridListIcon();
						}
					}
					break;

				case ACCOUNT:
					rubbishBinMenuItem.setVisible(true);
					if (accountFragment == MY_ACCOUNT_FRAGMENT) {
						refreshMenuItem.setVisible(true);
						killAllSessions.setVisible(true);
						upgradeAccountMenuItem.setVisible(true);
						changePass.setVisible(true);
						logoutMenuItem.setVisible(true);

						if (getTabItemMyAccount() == GENERAL_TAB) {
							exportMK.setVisible(true);
						}

						if (app.getMyAccountInfo() != null && app.getMyAccountInfo().getNumberOfSubscriptions() > 0) {
							cancelSubscription.setVisible(true);
						}
					} else {
						refreshMenuItem.setVisible(true);
						logoutMenuItem.setVisible(true);
					}
					break;

				case TRANSFERS:
					cancelAllTransfersMenuItem.setVisible(true);

					if (getCompletedTransfersFragment() != null && completedTFLol.isAnyTransferCompleted()) {
						clearCompletedTransfers.setVisible(true);
					}

					if (getTransfersFragment() != null && transfersInProgress.size() > 0) {
						if (megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD) || megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)) {
							playTransfersMenuIcon.setVisible(true);
							cancelAllTransfersMenuItem.setVisible(true);
						} else {
							pauseTransfersMenuIcon.setVisible(true);
							cancelAllTransfersMenuItem.setVisible(true);
						}
					}
					break;

				case SETTINGS:
					upgradeAccountMenuItem.setVisible(true);
					break;

				case CHAT:
					if (searchExpand) {
						openSearchView();
					} else {
						inviteMenuItem.setVisible(true);
						if (getChatsFragment() != null && rChatFL.getItemCount() > 0) {
							searchMenuItem.setVisible(true);
						}
						importLinkMenuItem.setVisible(true);
						importLinkMenuItem.setTitle(getString(R.string.action_open_chat_link));
					}
					break;

				case NOTIFICATIONS:
					break;
			}
		}

		if (megaApi.isBusinessAccount()) {
			upgradeAccountMenuItem.setVisible(false);
		}

		logDebug("Call to super onCreateOptionsMenu");
		return super.onCreateOptionsMenu(menu);
	}

	private void setGridListIcon() {
		if (isList){
			thumbViewMenuItem.setTitle(getString(R.string.action_grid));
			thumbViewMenuItem.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_thumbnail_view));
		}
		else{
			thumbViewMenuItem.setTitle(getString(R.string.action_list));
			thumbViewMenuItem.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_list_view));
		}
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		logDebug("onOptionsItemSelected");
		typesCameraPermission = -1;

		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}

		if (megaApi != null){
			logDebug("retryPendingConnections");
			megaApi.retryPendingConnections();
		}

		if (megaChatApi != null){
			megaChatApi.retryPendingConnections(false, null);
		}

		int id = item.getItemId();
		switch(id){
			case android.R.id.home:{
				if (firstNavigationLevel && drawerItem != DrawerItem.SEARCH){
					if (drawerItem == DrawerItem.RUBBISH_BIN || drawerItem == DrawerItem.ACCOUNT || drawerItem == DrawerItem.INBOX || drawerItem == DrawerItem.CONTACTS
							|| drawerItem == DrawerItem.NOTIFICATIONS|| drawerItem == DrawerItem.SETTINGS || drawerItem == DrawerItem.MEDIA_UPLOADS || drawerItem == DrawerItem.TRANSFERS) {
						if (drawerItem == DrawerItem.MEDIA_UPLOADS) {
							backToDrawerItem(CLOUD_DRIVE_BNV);
						}
						else if (drawerItem == DrawerItem.ACCOUNT && comesFromNotifications) {
							comesFromNotifications = false;
							selectDrawerItemLollipop(DrawerItem.NOTIFICATIONS);
						}
						else {
							backToDrawerItem(bottomNavigationCurrentItem);
						}
					}
					else {
						drawerLayout.openDrawer(nV);
					}
				}
				else{
					logDebug("NOT firstNavigationLevel");
		    		if (drawerItem == DrawerItem.CLOUD_DRIVE){
						//Cloud Drive
						if (getTabItemCloud() == RECENTS_TAB && isRecentsAdded()) {
							rF.onBackPressed();
						}
						else if (getTabItemCloud() == CLOUD_TAB && isCloudAdded()) {
							fbFLol.onBackPressed();
						}
		    		}
					else if (drawerItem == DrawerItem.RUBBISH_BIN){
		    			rubbishBinFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
						if (rubbishBinFLol != null){
							rubbishBinFLol.onBackPressed();
						}
					}
		    		else if (drawerItem == DrawerItem.SHARED_ITEMS){
						if (getTabItemShares() == INCOMING_TAB && isIncomingAdded()) {
							inSFLol.onBackPressed();
						} else if (getTabItemShares() == OUTGOING_TAB && isOutgoingAdded()) {
							outSFLol.onBackPressed();
						} else if (getTabItemShares() == LINKS_TAB && isLinksAdded()) {
							lF.onBackPressed();
						}
		    		}
					else if (drawerItem == DrawerItem.CAMERA_UPLOADS){
						cuFL = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CAMERA_UPLOADS.getTag());
						if (cuFL != null){
							long cameraUploadHandle = cuFL.getPhotoSyncHandle();
							MegaNode nps = megaApi.getNodeByHandle(cameraUploadHandle);
							if (nps != null){
								ArrayList<MegaNode> nodes = megaApi.getChildren(nps, orderCamera);
								cuFL.setNodes(nodes);
								isSearchEnabled=false;
								setToolbarTitle();
								invalidateOptionsMenu();
							}
							return true;
						}
					}else if (drawerItem == DrawerItem.MEDIA_UPLOADS){
						muFLol = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MEDIA_UPLOADS.getTag());
						if (muFLol != null){
							long cameraUploadHandle = muFLol.getPhotoSyncHandle();
							MegaNode nps = megaApi.getNodeByHandle(cameraUploadHandle);
							if (nps != null){
								ArrayList<MegaNode> nodes = megaApi.getChildren(nps, orderCamera);
								muFLol.setNodes(nodes);
								setToolbarTitle();
								isSearchEnabled=false;
								invalidateOptionsMenu();
							}
							return true;
						}
					}
		    		else if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE){
						oFLol = (OfflineFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.OFFLINE.getTag());
		    			if (oFLol != null){
		    				oFLol.onBackPressed();
		    				return true;
		    			}
		    		}
					else if (drawerItem == DrawerItem.INBOX){
						iFLol = (InboxFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.INBOX.getTag());
						if (iFLol != null){
							iFLol.onBackPressed();
							return true;
						}
					}
		    		else if (drawerItem == DrawerItem.SEARCH){
		    			if (getSearchFragment() != null){
//		    				sFLol.onBackPressed();
		    				onBackPressed();
		    				return true;
		    			}
		    		}
		    		else if (drawerItem == DrawerItem.TRANSFERS){

						drawerItem = DrawerItem.CLOUD_DRIVE;
						setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV);
						selectDrawerItemLollipop(drawerItem);
						return true;
		    		}
					else if (drawerItem == DrawerItem.ACCOUNT){
						switch (accountFragment) {
							case UPGRADE_ACCOUNT_FRAGMENT:
								closeUpgradeAccountFragment();
								return true;

							case CC_FRAGMENT:
								ccFL = (CreditCardFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CREDIT_CARD.getTag());
								if (ccFL != null) {
									displayedAccountType = ccFL.getParameterType();
								}
								showUpAF();
								return true;
						}
					}
				}
		    	return true;
		    }
			case R.id.action_search:{
				logDebug("Action search selected");
				textSubmitted = false;
				if (createFolderMenuItem != null){
					upgradeAccountMenuItem.setVisible(false);
					cancelAllTransfersMenuItem.setVisible(false);
					clearCompletedTransfers.setVisible(false);
					pauseTransfersMenuIcon.setVisible(false);
					playTransfersMenuIcon.setVisible(false);
					createFolderMenuItem.setVisible(false);
					addContactMenuItem.setVisible(false);
					addMenuItem.setVisible(false);
					refreshMenuItem.setVisible(false);
					sortByMenuItem.setVisible(false);
					unSelectMenuItem.setVisible(false);
					changePass.setVisible(false);
					rubbishBinMenuItem.setVisible(false);
					clearRubbishBinMenuitem.setVisible(false);
					importLinkMenuItem.setVisible(false);
					takePicture.setVisible(false);
					refreshMenuItem.setVisible(false);
					helpMenuItem.setVisible(false);
					gridSmallLargeMenuItem.setVisible(false);
					logoutMenuItem.setVisible(false);
					forgotPassMenuItem.setVisible(false);
					inviteMenuItem.setVisible(false);
					selectMenuItem.setVisible(false);
					thumbViewMenuItem.setVisible(false);
					searchMenuItem.setVisible(false);
				}
				return true;
			}
		    case R.id.action_import_link:{
				showOpenLinkDialog();
		    	return true;
		    }
		    case R.id.action_take_picture:{
		    	typesCameraPermission = TAKE_PICTURE_OPTION;
		    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					boolean hasStoragePermission = checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
					if (!hasStoragePermission) {
						ActivityCompat.requestPermissions(this,
				                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
								REQUEST_WRITE_STORAGE);
					}

					boolean hasCameraPermission = checkPermission(Manifest.permission.CAMERA);
					if (!hasCameraPermission) {
						ActivityCompat.requestPermissions(this,
				                new String[]{Manifest.permission.CAMERA},
								REQUEST_CAMERA);
					}

					if (hasStoragePermission && hasCameraPermission){
						checkTakePicture(this, TAKE_PHOTO_CODE);
					}
				}
		    	else{
					checkTakePicture(this, TAKE_PHOTO_CODE);
		    	}

		    	return true;
		    }
		    case R.id.action_menu_cancel_all_transfers:{
		    	showConfirmationCancelAllTransfers();
		    	return true;
		    }
			case R.id.action_menu_clear_completed_transfers:{
				showConfirmationClearCompletedTransfers();
				return true;
			}
	        case R.id.action_pause:{
	        	if (drawerItem == DrawerItem.TRANSFERS){
					logDebug("Click on action_pause - play visible");
	        		megaApi.pauseTransfers(true, this);
	        		pauseTransfersMenuIcon.setVisible(false);
	        		playTransfersMenuIcon.setVisible(true);
	        	}

	        	return true;
	        }
	        case R.id.action_play:{
				logDebug("Click on action_play - pause visible");
				pauseTransfersMenuIcon.setVisible(true);
				playTransfersMenuIcon.setVisible(false);
    			megaApi.pauseTransfers(false, this);

	        	return true;
	        }
	        case R.id.action_add_contact:{
	        	if (drawerItem == DrawerItem.CONTACTS||drawerItem == DrawerItem.CHAT){
					chooseAddContactDialog(false);
	        	}
	        	return true;
	        }
			case R.id.action_menu_invite:{
				if (drawerItem == DrawerItem.CHAT){
                    logDebug("to InviteContactActivity");
                    startActivityForResult(new Intent(getApplicationContext(), InviteContactActivity.class), REQUEST_INVITE_CONTACT_FROM_DEVICE);
				}

				return true;
			}
	        case R.id.action_menu_kill_all_sessions:{
				showConfirmationCloseAllSessions();
	        	return true;
	        }
	        case R.id.action_new_folder:{
	        	if (drawerItem == DrawerItem.CLOUD_DRIVE){
	        		showNewFolderDialog();
	        	}
	        	else if(drawerItem == DrawerItem.SHARED_ITEMS){
	        		showNewFolderDialog();
	        	}
	        	else if (drawerItem == DrawerItem.CONTACTS){
					chooseAddContactDialog(false);
	        	}
	        	return true;
	        }
	        case R.id.action_add:{
	        	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
	    			if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
	    				ActivityCompat.requestPermissions(this,
	    		                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
								REQUEST_WRITE_STORAGE);
	    			}
	    		}

	        	if (drawerItem == DrawerItem.SHARED_ITEMS){
	        		if (viewPagerShares.getCurrentItem()==0){

						MegaNode checkNode = megaApi.getNodeByHandle(parentHandleIncoming);

						if((megaApi.checkAccess(checkNode, MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK)){
							this.showUploadPanel();
						}
						else if(megaApi.checkAccess(checkNode, MegaShare.ACCESS_READWRITE).getErrorCode() == MegaError.API_OK){
							this.showUploadPanel();
						}
						else if(megaApi.checkAccess(checkNode, MegaShare.ACCESS_READ).getErrorCode() == MegaError.API_OK){
							logWarning("Not permissions to upload");
							AlertDialog.Builder builder = new AlertDialog.Builder(this);
							builder.setMessage(getString(R.string.no_permissions_upload));
//								builder.setTitle(R.string.op_not_allowed);
							builder.setCancelable(false).setPositiveButton(R.string.general_ok, new DialogInterface.OnClickListener() {
								   public void onClick(DialogInterface dialog, int id) {
										//do things
									   alertNotPermissionsUpload.dismiss();
								   }
							   });

							alertNotPermissionsUpload = builder.create();
							alertNotPermissionsUpload.show();
//								brandAlertDialog(alertNotPermissionsUpload);
						}

	        		}
	        		else if(viewPagerShares.getCurrentItem()==1){
						this.showUploadPanel();
					}
	        	}
	        	else {
        			this.showUploadPanel();
	        	}

	        	return true;
	        }
			case R.id.action_menu_rubbish_bin:{
				drawerItem = DrawerItem.RUBBISH_BIN;
				selectDrawerItemLollipop(DrawerItem.RUBBISH_BIN);
				return true;
			}
	        case R.id.action_select:{
	        	switch (drawerItem) {
					case CLOUD_DRIVE:
						if (isCloudAdded()){
							fbFLol.selectAll();
						}
						break;

					case RUBBISH_BIN:
						if (getRubbishBinFragment() != null){
							rubbishBinFLol.selectAll();
						}
						break;

					case CONTACTS:
						switch(getTabItemContacts()){
							case CONTACTS_TAB:{
								if (getContactsFragment() != null){
									cFLol.selectAll();
								}
								break;
							}
							case SENT_REQUESTS_TAB:{
								if (getSentRequestFragment() != null){
									sRFLol.selectAll();
								}
								break;
							}
							case RECEIVED_REQUESTS_TAB:{
								if (getReceivedRequestFragment() != null){
									rRFLol.selectAll();
								}
								break;
							}
						}
						break;

					case SHARED_ITEMS:
						switch (getTabItemShares()) {
							case INCOMING_TAB:
								if (isIncomingAdded()) {
									inSFLol.selectAll();
								}
								break;

							case OUTGOING_TAB:
								if (isOutgoingAdded()) {
									outSFLol.selectAll();
								}
								break;

							case LINKS_TAB:
								if (isLinksAdded()) {
									lF.selectAll();
								}
								break;
						}
						break;

					case SAVED_FOR_OFFLINE:
						if (getOfflineFragment() != null) {
							oFLol.selectAll();
						}
						break;

					case CHAT:
						if (getChatsFragment() != null) {
							rChatFL.selectAll();
						}
						break;

					case INBOX:
						if (getInboxFragment() != null) {
							iFLol.selectAll();
						}
						break;

					case SEARCH:
						if (getSearchFragment() != null) {
							sFLol.selectAll();
						}
						break;

					case MEDIA_UPLOADS:
						if (getMediaUploadFragment() != null) {
							muFLol.selectAll();
						}
						break;

					case CAMERA_UPLOADS:
						if (getCameraUploadFragment() != null) {
							cuFL.selectAll();
						}
						break;
				}

	        	return true;
	        }
	        case R.id.action_grid_view_large_small:{
				if (drawerItem == DrawerItem.CAMERA_UPLOADS){
					isSmallGridCameraUploads = !isSmallGridCameraUploads;
					dbH.setSmallGridCamera(isSmallGridCameraUploads);

					if (isSmallGridCameraUploads){
						gridSmallLargeMenuItem.setIcon(mutateIcon(this, R.drawable.ic_thumbnail_view, R.color.black));
					}else{
						gridSmallLargeMenuItem.setIcon(mutateIcon(this, R.drawable.ic_menu_gridview_small, R.color.black));
					}

					refreshFragment(FragmentTag.CAMERA_UPLOADS.getTag());
	        	}
	        	else if (drawerItem == DrawerItem.MEDIA_UPLOADS){
					isSmallGridCameraUploads = !isSmallGridCameraUploads;
					dbH.setSmallGridCamera(isSmallGridCameraUploads);

					if (isSmallGridCameraUploads){
						gridSmallLargeMenuItem.setIcon(mutateIcon(this, R.drawable.ic_thumbnail_view, R.color.black));
					}else{
						gridSmallLargeMenuItem.setIcon(mutateIcon(this, R.drawable.ic_menu_gridview_small, R.color.black));
					}
					refreshFragment(FragmentTag.MEDIA_UPLOADS.getTag());
	        	}
	        	return true;
	        }
	        case R.id.action_grid:{
				logDebug("action_grid selected");
	        	if (drawerItem == DrawerItem.CAMERA_UPLOADS){
					logDebug("action_grid_list in CameraUploads");
	        		isListCameraUploads = !isListCameraUploads;
	    			dbH.setPreferredViewListCamera(isListCameraUploads);
					logDebug("dbH.setPreferredViewListCamera: " + isListCameraUploads);
					if (isListCameraUploads){
						thumbViewMenuItem.setTitle(getString(R.string.action_grid));
						gridSmallLargeMenuItem.setVisible(false);
						searchMenuItem.setVisible(true);
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
						if(!firstLogin) {
							gridSmallLargeMenuItem.setVisible(true);
						}else{
							gridSmallLargeMenuItem.setVisible(false);
						}
						searchMenuItem.setVisible(false);

					}
					refreshFragment(FragmentTag.CAMERA_UPLOADS.getTag());
	        	}
	        	else if (drawerItem == DrawerItem.MEDIA_UPLOADS){
					logDebug("action_grid_list in MediaUploads");
	        		isListCameraUploads = !isListCameraUploads;
	    			dbH.setPreferredViewListCamera(isListCameraUploads);
					logDebug("dbH.setPreferredViewListCamera: " + isListCameraUploads);

					if (isListCameraUploads){
						thumbViewMenuItem.setTitle(getString(R.string.action_grid));
						gridSmallLargeMenuItem.setVisible(false);
						searchMenuItem.setVisible(true);
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
						if(!firstLogin) {
							gridSmallLargeMenuItem.setVisible(true);
						}else{
							gridSmallLargeMenuItem.setVisible(false);
						}
						searchMenuItem.setVisible(false);

					}

					refreshFragment(FragmentTag.MEDIA_UPLOADS.getTag());
        		}
	        	else{
	    			updateView(!isList);
	        	}
	        	supportInvalidateOptionsMenu();

	        	return true;
	        }
	        case R.id.action_menu_clear_rubbish_bin:{
	        	showClearRubbishBinDialog();
	        	return true;
	        }
	        case R.id.action_menu_refresh:{
	        	switch(drawerItem){
		        	case ACCOUNT:{
						//Refresh all the info of My Account
		        		Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
			    		intent.setAction(ACTION_REFRESH);
			    		intent.putExtra("PARENT_HANDLE", parentHandleBrowser);
			    		startActivityForResult(intent, REQUEST_CODE_REFRESH);
			    		break;
		        	}
	        	}
	        	return true;
	        }
	        case R.id.action_menu_sort_by:{
				showSortOptions(managerActivity, outMetrics);
	        	return true;
	        }
			case R.id.action_search_by_date:{
				Intent intent = new Intent(this, SearchByDateActivityLollipop.class);
				startActivityForResult(intent, ACTION_SEARCH_BY_DATE);
				return  true;
			}
	        case R.id.action_menu_help:{
	        	Intent intent = new Intent();
	            intent.setAction(Intent.ACTION_VIEW);
	            intent.addCategory(Intent.CATEGORY_BROWSABLE);
	            intent.setData(Uri.parse("https://mega.co.nz/#help/android"));
	            startActivity(intent);

	    		return true;
	    	}
	        case R.id.action_menu_upgrade_account:{
	        	accountFragmentPreUpgradeAccount = accountFragment;
	        	drawerItemPreUpgradeAccount = drawerItem;
	        	drawerItem = DrawerItem.ACCOUNT;
	        	setBottomNavigationMenuItemChecked(HIDDEN_BNV);
				accountFragment = UPGRADE_ACCOUNT_FRAGMENT;
				displayedAccountType = -1;
				selectDrawerItemLollipop(drawerItem);
				return true;
	        }

	        case R.id.action_menu_change_pass:{
	        	Intent intent = new Intent(this, ChangePasswordActivityLollipop.class);
				startActivity(intent);
				return true;
	        }
	        case R.id.action_menu_export_MK:{
				logDebug("Export MK option selected");

				showMKLayout();
	        	return true;
	        }
	        case R.id.action_menu_logout:{
				logDebug("Action menu logout pressed");
				passwordReminderFromMyAccount = true;
				megaApi.shouldShowPasswordReminderDialog(true, this);
	        	return true;
	        }
	        case R.id.action_menu_cancel_subscriptions:{
				logDebug("Action menu cancel subscriptions pressed");
	        	if (megaApi != null){
	        		//Show the message
	        		showCancelMessage();
	        	}
	        	return true;
	        }
			case R.id.action_menu_forgot_pass:{
				logDebug("Action menu forgot pass pressed");
				maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
				if(maFLol!=null){
					showConfirmationResetPasswordFromMyAccount();
				}
				return true;
			}
			case R.id.action_scan_qr: {
				logDebug("Action menu scan QR code pressed");
                //Check if there is a in progress call:
				checkBeforeOpeningQR(true);
				return true;
			}
			case R.id.action_return_call:{
				logDebug("Action menu return to call in progress pressed");
				if(checkPermissionsCall()){
					returnCall(this);
				}
				return true;
			}
            default:{
	            return super.onOptionsItemSelected(item);
            }
		}
	}

	public void checkBeforeOpeningQR(boolean openScanQR){
		if (isNecessaryDisableLocalCamera() != -1) {
			showConfirmationOpenCamera(this, ACTION_OPEN_QR, openScanQR);
			return;
		}
		openQR(openScanQR);
	}

	public void openQR(boolean openScanQr){
		ScanCodeFragment fragment = new ScanCodeFragment();
		getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commitNowAllowingStateLoss();
		Intent intent = new Intent(this, QRCodeActivity.class);
		intent.putExtra(OPEN_SCAN_QR, openScanQr);
		startActivity(intent);
	}

	private void updateView (boolean isList) {
        if (this.isList != isList) {
            this.isList = isList;
            dbH.setPreferredViewList(isList);
        }

        //Refresh Cloud Fragment
        refreshFragment(FragmentTag.CLOUD_DRIVE.getTag());

        if(cloudPageAdapter != null) {
            cloudPageAdapter.notifyDataSetChanged();
        }

        //Refresh Rubbish Fragment
        refreshFragment(FragmentTag.RUBBISH_BIN.getTag());


        //Refresh OfflineFragmentLollipop layout even current fragment isn't OfflineFragmentLollipop.
        refreshFragment(FragmentTag.OFFLINE.getTag());

        //Refresh ContactsFragmentLollipop layout even current fragment isn't ContactsFragmentLollipop.
        refreshFragment(FragmentTag.CONTACTS.getTag());

        if (contactsPageAdapter != null) {
            contactsPageAdapter.notifyDataSetChanged();
        }

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

	public void hideMKLayout(){
		logDebug("hideMKLayout");
		mkLayoutVisible= false;

		tB.setVisibility(View.VISIBLE);
		abL.setVisibility(View.VISIBLE);

		eRKeyF = null;
		changeStatusBarColor(COLOR_STATUS_BAR_ZERO);

		drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
		supportInvalidateOptionsMenu();
		selectDrawerItemLollipop(drawerItem);
	}

	public void showMKLayout(){
		logDebug("showMKLayout");

		accountFragment = BACKUP_RECOVERY_KEY_FRAGMENT;
		mkLayoutVisible=true;

		aB.setSubtitle(null);
		tB.setVisibility(View.GONE);

		deleteCurrentFragment();

		if (eRKeyF == null){
			eRKeyF = new ExportRecoveryKeyFragment();
		}
		replaceFragment(eRKeyF, FragmentTag.EXPORT_RECOVERY_KEY.getTag());

		abL.setVisibility(View.GONE);

		Window window = this.getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
		window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		window.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_login));

		setTabsVisibility();
		drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
		supportInvalidateOptionsMenu();
		hideFabButton();
	}


	public void refreshAfterMovingToRubbish(){
		logDebug("refreshAfterMovingToRubbish");

		if (drawerItem == DrawerItem.CLOUD_DRIVE) {
			refreshCloudDrive();
		} else if (drawerItem == DrawerItem.INBOX) {
			onNodesInboxUpdate();
		} else if (drawerItem == DrawerItem.SHARED_ITEMS) {
			onNodesSharedUpdate();
		} else if (drawerItem == DrawerItem.SEARCH) {
			refreshSearch();
		}

        checkCameraUploadFolder(true,null);
		refreshRubbishBin();
		setToolbarTitle();
	}

    /**
     * After nodes on Cloud Drive changed or some nodes are moved to rubbish bin,
     * need to check CU and MU folders' status.
     *
     * @param shouldDisable If CU or MU folder is deleted by current client, then CU should be disabled. Otherwise not.
     * @param updatedNodes Nodes which have changed.
     */
    private void checkCameraUploadFolder(boolean shouldDisable, ArrayList<MegaNode> updatedNodes) {
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
                if (getSettingsFragment() != null) {
                    sttFLol.disableMediaUploadUIProcess();
                }
            } else {
                // Just stop the upload process.
                stopRunningCameraUploadService(app);
            }
        } else if (isPrimaryFolderInRubbish) {
            // If CU folder is in rubbish bin.
            logDebug("CU folder is deleted, backup settings and disable CU.");
            if (shouldDisable) {
                // Disable both CU and MU.
                backupTimestampsAndFolderHandle();
                disableCameraUploadSettingProcess(false);
                if (getSettingsFragment() != null) {
                    sttFLol.disableCameraUploadUIProcess();
                }
            } else {
                // Just stop the upload process.
                stopRunningCameraUploadService(app);
            }
        }
    }

	public void refreshRubbishBin () {
		rubbishBinFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
		if (rubbishBinFLol != null){
			ArrayList<MegaNode> nodes;
			if(parentHandleRubbish == -1){
				nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderCloud);
			}
			else{
				nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleRubbish), orderCloud);
			}

			rubbishBinFLol.hideMultipleSelect();
			rubbishBinFLol.setNodes(nodes);
			rubbishBinFLol.getRecyclerView().invalidate();
		}
	}

	public void refreshAfterMoving() {
		logDebug("refreshAfterMoving");
		if (drawerItem == DrawerItem.CLOUD_DRIVE) {

			//Refresh Cloud Fragment
			refreshCloudDrive();

			//Refresh Rubbish Fragment
			refreshRubbishBin();
		}
		else if(drawerItem == DrawerItem.RUBBISH_BIN){
			//Refresh Rubbish Fragment
			refreshRubbishBin();
		}
		else if (drawerItem == DrawerItem.INBOX) {
			onNodesInboxUpdate();

			refreshCloudDrive();
		}
		else if(drawerItem == DrawerItem.SHARED_ITEMS) {
			onNodesSharedUpdate();

			//Refresh Cloud Fragment
			refreshCloudDrive();

			//Refresh Rubbish Fragment
			refreshRubbishBin();
		}else if(drawerItem == DrawerItem.SEARCH){
			refreshSearch();
		}

		setToolbarTitle();
	}

	public void refreshSearch() {
		if (getSearchFragment() != null){
			sFLol.hideMultipleSelect();
			sFLol.refresh();
		}
	}

	public void refreshAfterRemoving(){
		logDebug("refreshAfterRemoving");

		rubbishBinFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
		if (rubbishBinFLol != null){
			rubbishBinFLol.hideMultipleSelect();

			if (isClearRubbishBin){
				isClearRubbishBin = false;
				parentHandleRubbish = megaApi.getRubbishNode().getHandle();
				ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderCloud);
				rubbishBinFLol.setNodes(nodes);
				rubbishBinFLol.getRecyclerView().invalidate();
			}
			else{
				refreshRubbishBin();
			}
		}

		onNodesInboxUpdate();

		refreshSearch();
	}

	@Override
	public void onBackPressed() {
		logDebug("onBackPressed");

		retryConnectionsAndSignalPresence();

		if (drawerLayout.isDrawerOpen(nV)){
    		drawerLayout.closeDrawer(Gravity.LEFT);
    		return;
    	}

		try {
			statusDialog.dismiss();
		}
		catch (Exception ex) {}

		logDebug("DRAWERITEM: " + drawerItem);

		if (turnOnNotifications){
			deleteTurnOnNotificationsFragment();
			return;
		}

		if (onAskingPermissionsFragment || onAskingSMSVerificationFragment) {
			return;
		}

		if (mkLayoutVisible){
			hideMKLayout();
			return;
		}

		if (drawerItem == DrawerItem.CLOUD_DRIVE){
			if (getTabItemCloud() == RECENTS_TAB && isRecentsAdded() ) {
				if (rF.onBackPressed() == 0) {
					viewPagerCloud.setCurrentItem(CLOUD_TAB);
					return;
				}
			}
			else if (isCloudAdded() && fbFLol.onBackPressed() == 0) {
				super.onBackPressed();
			}
		}
		else if (drawerItem == DrawerItem.RUBBISH_BIN){
			rubbishBinFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
			if (rubbishBinFLol != null) {
				if (rubbishBinFLol.onBackPressed() == 0) {
					backToDrawerItem(bottomNavigationCurrentItem);
				}
                return;
			}
			else{
				super.onBackPressed();
			}
		}
		else if (drawerItem == DrawerItem.TRANSFERS){
			backToDrawerItem(bottomNavigationCurrentItem);
			return;

    	}
		else if (drawerItem == DrawerItem.INBOX){
			iFLol = (InboxFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.INBOX.getTag());
			if (iFLol != null && iFLol.onBackPressed() == 0){
				backToDrawerItem(bottomNavigationCurrentItem);
				return;
			}
		}
		else if (drawerItem == DrawerItem.NOTIFICATIONS){
			backToDrawerItem(bottomNavigationCurrentItem);
			return;
		}
		else if (drawerItem == DrawerItem.SETTINGS){

			if (!isOnline(this)){
				showOfflineMode();
			}
			backToDrawerItem(bottomNavigationCurrentItem);
			return;
		}
		else if (drawerItem == DrawerItem.SHARED_ITEMS){
			if ((getTabItemShares() == INCOMING_TAB && isIncomingAdded() && inSFLol.onBackPressed() == 0)
					|| (getTabItemShares() == OUTGOING_TAB && isOutgoingAdded() && outSFLol.onBackPressed() == 0)
					|| (getTabItemShares() == LINKS_TAB && isLinksAdded() && lF.onBackPressed() == 0)) {
				drawerItem = DrawerItem.CLOUD_DRIVE;
				selectDrawerItemLollipop(drawerItem);
			}
			return;
		}
		else if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE){
			oFLol = (OfflineFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.OFFLINE.getTag());
			if (oFLol != null && oFLol.onBackPressed() == 0){
				if (!isOnline(this)){
					super.onBackPressed();
					return;
				}

				if (isCloudAdded()){
					drawerItem = DrawerItem.CLOUD_DRIVE;
					selectDrawerItemLollipop(drawerItem);
				}
				else{
					super.onBackPressed();
				}

				return;
			}
		}
		else if (drawerItem == DrawerItem.CHAT){
			if (!isOnline(this)){
				super.onBackPressed();
				return;
			}
			else{
				if(megaApi!=null && megaApi.getRootNode()!=null){
					drawerItem = DrawerItem.CLOUD_DRIVE;
					selectDrawerItemLollipop(drawerItem);
				}
				else{
					drawerItem = DrawerItem.SAVED_FOR_OFFLINE;
					selectDrawerItemLollipop(DrawerItem.SAVED_FOR_OFFLINE);
				}
			}
		}
		else if (drawerItem == DrawerItem.CONTACTS){
			int index = getTabItemContacts();
			switch (index) {
				case CONTACTS_TAB:{
					//CONTACTS FRAGMENT
		    		cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CONTACTS.getTag());
		    		if (cFLol != null && cFLol.onBackPressed() == 0){
						backToDrawerItem(bottomNavigationCurrentItem);
						return;
		    		}
					break;
				}
				case SENT_REQUESTS_TAB:{
					sRFLol = (SentRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.SENT_REQUESTS.getTag());
		    		if (sRFLol != null){
						backToDrawerItem(bottomNavigationCurrentItem);
                        return;
		    		}
					break;
				}
				case RECEIVED_REQUESTS_TAB:{
					rRFLol = (ReceivedRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECEIVED_REQUESTS.getTag());
		    		if (rRFLol != null){
						backToDrawerItem(bottomNavigationCurrentItem);
						return;
		    		}
					break;
				}
			}
		}
		else if (drawerItem == DrawerItem.ACCOUNT){
			logDebug("MyAccountSection");
			logDebug("The accountFragment is: " + accountFragment);
    		switch(accountFragment){

	    		case MY_ACCOUNT_FRAGMENT:{
					maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
	    			if (maFLol != null && maFLol.onBackPressed() == 0){
						if (comesFromNotifications) {
							comesFromNotifications = false;
							selectDrawerItemLollipop(DrawerItem.NOTIFICATIONS);
						}
						else {
							backToDrawerItem(bottomNavigationCurrentItem);
						}
	    			}
	    			return;
	    		}
	    		case UPGRADE_ACCOUNT_FRAGMENT:{
					logDebug("Back to MyAccountFragment -> drawerItemPreUpgradeAccount");
					closeUpgradeAccountFragment();
	    			return;
	    		}
	    		case CC_FRAGMENT:{
					ccFL = (CreditCardFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CREDIT_CARD.getTag());
	    			if (ccFL != null){
						displayedAccountType = ccFL.getParameterType();
	    			}
					showUpAF();
	    			return;
	    		}
	    		case OVERQUOTA_ALERT:{
	    			backToDrawerItem(bottomNavigationCurrentItem);
	    			return;
	    		}
	    		default:{
	    			if (fbFLol != null){
						backToDrawerItem(bottomNavigationCurrentItem);
	    			}
	    		}
    		}
    	}
		else if (drawerItem == DrawerItem.CAMERA_UPLOADS){
			cuFL = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CAMERA_UPLOADS.getTag());
			if (cuFL != null && cuFL.onBackPressed() == 0){
				visibilitySearch(false);
				backToDrawerItem(-1);
				return;
			}
    	}
		else if (drawerItem == DrawerItem.MEDIA_UPLOADS){
			muFLol = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MEDIA_UPLOADS.getTag());
			if (muFLol != null && muFLol.onBackPressed() == 0){
				visibilitySearch(false);
				backToDrawerItem(CLOUD_DRIVE_BNV);
				return;
    		}
    	}
		else if (drawerItem == DrawerItem.SEARCH){
			if (getSearchFragment() != null && sFLol.onBackPressed() == 0){
    			closeSearchSection();
				return;
    		}
    	}
		else{
			super.onBackPressed();
			return;
		}
	}

	private void closeUpgradeAccountFragment() {
		setFirstNavigationLevel(true);
		displayedAccountType = -1;

		if (drawerItemPreUpgradeAccount != null) {
			if (drawerItemPreUpgradeAccount == DrawerItem.ACCOUNT) {
				if (accountFragmentPreUpgradeAccount == -1) {
					accountFragment = MY_ACCOUNT_FRAGMENT;
				} else {
					accountFragment = accountFragmentPreUpgradeAccount;
				}
			}

			drawerItem = drawerItemPreUpgradeAccount;
		} else {
			accountFragment = MY_ACCOUNT_FRAGMENT;
			drawerItem = DrawerItem.ACCOUNT;
		}

		selectDrawerItemLollipop(drawerItem);
	}

	public void backToDrawerItem(int item) {
    	if (item == CLOUD_DRIVE_BNV || item == -1) {
    		drawerItem = DrawerItem.CLOUD_DRIVE;
		}
		else if (item == CAMERA_UPLOADS_BNV) {
			drawerItem = DrawerItem.CAMERA_UPLOADS;
		}
		else if (item == CHAT_BNV) {
			drawerItem = DrawerItem.CHAT;
		}
		else if (item == SHARED_BNV) {
			drawerItem = DrawerItem.SHARED_ITEMS;
		}
		else if (item == OFFLINE_BNV) {
			drawerItem = DrawerItem.SAVED_FOR_OFFLINE;
		}
		else if (item == MEDIA_UPLOADS_BNV) {
    		drawerItem = DrawerItem.MEDIA_UPLOADS;
		}
		selectDrawerItemLollipop(drawerItem);
	}

	void isFirstTimeCam() {
		if(firstLogin){
			firstLogin = false;
			dbH.setCamSyncEnabled(false);
			bottomNavigationCurrentItem = CLOUD_DRIVE_BNV;
		}
	}

	private void checkIfShouldCloseSearchView(DrawerItem oldDrawerItem, DrawerItem newDrawerItem) {
    	if (!searchExpand || oldDrawerItem == newDrawerItem) return;

		if (oldDrawerItem == DrawerItem.CHAT || oldDrawerItem == DrawerItem.SAVED_FOR_OFFLINE) {
			searchExpand = false;
		}
	}

	@Override
	public boolean onNavigationItemSelected(MenuItem menuItem) {
		logDebug("onNavigationItemSelected");

		if (nV != null){
			Menu nVMenu = nV.getMenu();
			resetNavigationViewMenu(nVMenu);
		}

		DrawerItem oldDrawerItem = drawerItem;

		switch (menuItem.getItemId()){
			case R.id.bottom_navigation_item_cloud_drive: {
				if (drawerItem == DrawerItem.CLOUD_DRIVE) {
					if (getTabItemCloud() == CLOUD_TAB) {
						long rootHandle = megaApi.getRootNode().getHandle();
						if (parentHandleBrowser != -1 && parentHandleBrowser != rootHandle) {
							parentHandleBrowser = rootHandle;
							refreshFragment(FragmentTag.CLOUD_DRIVE.getTag());
							if (cloudPageAdapter != null) {
								cloudPageAdapter.notifyDataSetChanged();
							}
							if (isCloudAdded()) {
								fbFLol.scrollToFirstPosition();
							}
						}
					} else if (getDeepBrowserTreeRecents() > 0 && isRecentsAdded()) {
                        rF.onBackPressed();
					}
				}
				else {
					drawerItem = DrawerItem.CLOUD_DRIVE;
					setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV);
				}
				break;
			}
			case R.id.bottom_navigation_item_offline: {
				if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE) {
					if (!pathNavigationOffline.equals("/")){
						pathNavigationOffline = "/";
						refreshFragment(FragmentTag.OFFLINE.getTag());
					}
				} else {
					drawerItem = DrawerItem.SAVED_FOR_OFFLINE;
					setBottomNavigationMenuItemChecked(OFFLINE_BNV);
				}
				break;
			}
			case R.id.bottom_navigation_item_camera_uploads: {
				drawerItem = DrawerItem.CAMERA_UPLOADS;
				setBottomNavigationMenuItemChecked(CAMERA_UPLOADS_BNV);
				break;
			}
			case R.id.bottom_navigation_item_shared_items: {
				if (drawerItem == DrawerItem.SHARED_ITEMS) {
					if (getTabItemShares() == 0 && parentHandleIncoming != -1) {
						parentHandleIncoming = -1;
						refreshFragment(FragmentTag.INCOMING_SHARES.getTag());
					} else if (getTabItemShares() == 1 && parentHandleOutgoing != -1){
						parentHandleOutgoing = -1;
						refreshFragment(FragmentTag.OUTGOING_SHARES.getTag());
					}
					refreshSharesPageAdapter();
				} else {
					drawerItem = DrawerItem.SHARED_ITEMS;
					setBottomNavigationMenuItemChecked(SHARED_BNV);
				}
				break;
			}
			case R.id.bottom_navigation_item_chat: {
				drawerItem = DrawerItem.CHAT;
				setBottomNavigationMenuItemChecked(CHAT_BNV);
				break;
			}
		}

		checkIfShouldCloseSearchView(oldDrawerItem, drawerItem);
		selectDrawerItemLollipop(drawerItem);
		drawerLayout.closeDrawer(Gravity.LEFT);

		return true;
	}

	public void showSnackbar(int type, String s, long idChat){
    	showSnackbar(type, fragmentContainer, s, idChat);
	}

	public void askConfirmationNoAppInstaledBeforeDownload (String parentPath, String url, long size, long [] hashes, String nodeToDownload, final boolean highPriority){
        logDebug("askConfirmationNoAppInstaledBeforeDownload");

		final String parentPathC = parentPath;
		final String urlC = url;
		final long [] hashesC = hashes;
		final long sizeC=size;

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LinearLayout confirmationLayout = new LinearLayout(this);
		confirmationLayout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(10, outMetrics), scaleWidthPx(17, outMetrics), 0);

		final CheckBox dontShowAgain =new CheckBox(this);
		dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
		dontShowAgain.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

		confirmationLayout.addView(dontShowAgain, params);

		builder.setView(confirmationLayout);

//				builder.setTitle(getString(R.string.confirmation_required));
		builder.setMessage(getString(R.string.alert_no_app, nodeToDownload));
		builder.setPositiveButton(getString(R.string.general_save_to_device),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if(dontShowAgain.isChecked()){
							dbH.setAttrAskNoAppDownload("false");
						}
						nC.download(parentPathC, urlC, sizeC, hashesC, highPriority);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				if(dontShowAgain.isChecked()){
					dbH.setAttrAskNoAppDownload("false");
				}
			}
		});
		downloadConfirmationDialog = builder.create();
		downloadConfirmationDialog.show();
	}


	public void askSizeConfirmationBeforeDownload(String parentPath,String url, long size, long [] hashes, final boolean highPriority){
        logDebug("askSizeConfirmationBeforeDownload");

		final String parentPathC = parentPath;
		final String urlC = url;
		final long [] hashesC = hashes;
		final long sizeC=size;

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LinearLayout confirmationLayout = new LinearLayout(this);
		confirmationLayout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(10, outMetrics), scaleWidthPx(17, outMetrics), 0);

		final CheckBox dontShowAgain =new CheckBox(this);
		dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
		dontShowAgain.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

		confirmationLayout.addView(dontShowAgain, params);

		builder.setView(confirmationLayout);

//				builder.setTitle(getString(R.string.confirmation_required));

		builder.setMessage(getString(R.string.alert_larger_file, getSizeString(sizeC)));
		builder.setPositiveButton(getString(R.string.general_save_to_device),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if(dontShowAgain.isChecked()){
							dbH.setAttrAskSizeDownload("false");
						}
						nC.checkInstalledAppBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				if(dontShowAgain.isChecked()){
					dbH.setAttrAskSizeDownload("false");
				}
			}
		});

		downloadConfirmationDialog = builder.create();
		downloadConfirmationDialog.show();
	}

	public void askSizeConfirmationBeforeChatDownload(String parentPath, ArrayList<MegaNode> nodeList, long size){
		logDebug("askSizeConfirmationBeforeChatDownload");

		final String parentPathC = parentPath;
		final ArrayList<MegaNode> nodeListC = nodeList;
		final long sizeC = size;
		final ChatController chatC = new ChatController(this);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LinearLayout confirmationLayout = new LinearLayout(this);
		confirmationLayout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(10, outMetrics), scaleWidthPx(17, outMetrics), 0);

		final CheckBox dontShowAgain =new CheckBox(this);
		dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
		dontShowAgain.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

		confirmationLayout.addView(dontShowAgain, params);

		builder.setView(confirmationLayout);

//				builder.setTitle(getString(R.string.confirmation_required));

		builder.setMessage(getString(R.string.alert_larger_file, getSizeString(sizeC)));
		builder.setPositiveButton(getString(R.string.general_save_to_device),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if(dontShowAgain.isChecked()){
							dbH.setAttrAskSizeDownload("false");
						}
						chatC.download(parentPathC, nodeListC);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				if(dontShowAgain.isChecked()){
					dbH.setAttrAskSizeDownload("false");
				}
			}
		});

		downloadConfirmationDialog = builder.create();
		downloadConfirmationDialog.show();
	}

	public void restoreFromRubbish(final MegaNode node) {
		logDebug("Node Handle: " + node.getHandle());

		restoreFromRubbish = true;

		MegaNode newParent = megaApi.getNodeByHandle(node.getRestoreHandle());
		if(newParent !=null){
			megaApi.moveNode(node, newParent, this);
		}
		else{
			logDebug("The restore folder no longer exists");
		}
	}

	public void showRenameDialog(final MegaNode document, String text){
		logDebug("showRenameDialog");
		LinearLayout layout = new LinearLayout(this);
	    layout.setOrientation(LinearLayout.VERTICAL);
	    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);
//	    layout.setLayoutParams(params);

		final EditTextCursorWatcher input = new EditTextCursorWatcher(this, document.isFolder());
//		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
//		input.setHint(getString(R.string.context_new_folder_name));
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);

		input.setImeActionLabel(getString(R.string.context_rename),EditorInfo.IME_ACTION_DONE);
		input.setText(text);
		input.requestFocus();

		if (document.isFolder()) {
			input.setSelection(0, input.getText().length());
		} else {
			String[] s = document.getName().split("\\.");
			if (s != null) {

				int numParts = s.length;
				int lastSelectedPos = 0;
				if (numParts == 1) {
					input.setSelection(0, input.getText().length());
				} else if (numParts > 1) {
					for (int i = 0; i < (numParts - 1); i++) {
						lastSelectedPos += s[i].length();
						lastSelectedPos++;
					}
					lastSelectedPos--; //The last point should not be selected)
					input.setSelection(0, lastSelectedPos);
				}
			}
		}

	    layout.addView(input, params);

		LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params1.setMargins(scaleWidthPx(20, outMetrics), 0, scaleWidthPx(17, outMetrics), 0);

		final RelativeLayout error_layout = new RelativeLayout(ManagerActivityLollipop.this);
		layout.addView(error_layout, params1);

		final ImageView error_icon = new ImageView(ManagerActivityLollipop.this);
		error_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_input_warning));
		error_layout.addView(error_icon);
		RelativeLayout.LayoutParams params_icon = (RelativeLayout.LayoutParams) error_icon.getLayoutParams();

		params_icon.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		error_icon.setLayoutParams(params_icon);

		error_icon.setColorFilter(ContextCompat.getColor(ManagerActivityLollipop.this, R.color.login_warning));

		final TextView textError = new TextView(ManagerActivityLollipop.this);
		error_layout.addView(textError);
		RelativeLayout.LayoutParams params_text_error = (RelativeLayout.LayoutParams) textError.getLayoutParams();
		params_text_error.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error.addRule(RelativeLayout.CENTER_VERTICAL);
		params_text_error.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params_text_error.setMargins(scaleWidthPx(3, outMetrics), 0,0,0);
		textError.setLayoutParams(params_text_error);

		textError.setTextColor(ContextCompat.getColor(ManagerActivityLollipop.this, R.color.login_warning));

		error_layout.setVisibility(View.GONE);

		input.getBackground().mutate().clearColorFilter();
		input.getBackground().mutate().setColorFilter(ContextCompat.getColor(this, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
		input.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				if(error_layout.getVisibility() == View.VISIBLE){
					error_layout.setVisibility(View.GONE);
					input.getBackground().mutate().clearColorFilter();
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(managerActivity, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
				}
			}
		});

		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
										  KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {

					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						input.getBackground().mutate().setColorFilter(ContextCompat.getColor(managerActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError.setText(getString(R.string.invalid_string));
						error_layout.setVisibility(View.VISIBLE);
						input.requestFocus();

					}else{
						boolean result=matches(regex, value);
						if(result){
							input.getBackground().mutate().setColorFilter(ContextCompat.getColor(managerActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
							textError.setText(getString(R.string.invalid_characters));
							error_layout.setVisibility(View.VISIBLE);
							input.requestFocus();

						}else{
							nC.renameNode(document, value);
							renameDialog.dismiss();
						}
					}
					return true;
				}
				return false;
			}
		});

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.context_rename) + " "	+ new String(document.getName()));
		builder.setPositiveButton(getString(R.string.context_rename),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				input.getBackground().clearColorFilter();
			}
		});
		builder.setView(layout);
		renameDialog = builder.create();
		renameDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		renameDialog.show();
		renameDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new   View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				String value = input.getText().toString().trim();

				if (value.length() == 0) {
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(managerActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
					textError.setText(getString(R.string.invalid_string));
					error_layout.setVisibility(View.VISIBLE);
					input.requestFocus();
				}
				else{
					boolean result=matches(regex, value);
					if(result){
						input.getBackground().mutate().setColorFilter(ContextCompat.getColor(managerActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError.setText(getString(R.string.invalid_characters));
						error_layout.setVisibility(View.VISIBLE);
						input.requestFocus();

					}else{
						nC.renameNode(document, value);
						renameDialog.dismiss();
					}
				}
			}
		});
	}

	public static boolean matches(String regex, CharSequence input) {
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(input);
		return m.find();
	}

	public void showGetLinkActivity(long handle){
		logDebug("Handle: " + handle);
		MegaNode node = megaApi.getNodeByHandle(handle);
		if (showTakenDownNodeActionNotAvailableDialog(node, this)) {
			return;
		}

		Intent linkIntent = new Intent(this, GetLinkActivityLollipop.class);
		linkIntent.putExtra("handle", handle);
		startActivity(linkIntent);

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

	public void setIsGetLink(boolean value){
		this.isGetLink = value;
	}

	public void setIsClearRubbishBin(boolean value){
		this.isClearRubbishBin = value;
	}

	public void setMoveToRubbish(boolean value){
		this.moveToRubbish = value;
	}

	public void askConfirmationMoveToRubbish(final ArrayList<Long> handleList){
		logDebug("askConfirmationMoveToRubbish");
		isClearRubbishBin=false;

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						//TODO remove the outgoing shares
						nC.moveToTrash(handleList, moveToRubbish);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		if(handleList!=null){

			if (handleList.size() > 0){
				Long handle = handleList.get(0);
				MegaNode p = megaApi.getNodeByHandle(handle);
				while (megaApi.getParentNode(p) != null){
					p = megaApi.getParentNode(p);
				}
				if (p.getHandle() != megaApi.getRubbishNode().getHandle()){

					setMoveToRubbish(true);

					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					if (getPrimaryFolderHandle() == handle ) {
						builder.setMessage(getResources().getString(R.string.confirmation_move_cu_folder_to_rubbish));
					} else if (getSecondaryFolderHandle() == handle ) {
						builder.setMessage(R.string.confirmation_move_mu_folder_to_rubbish);
					} else {
						builder.setMessage(getResources().getString(R.string.confirmation_move_to_rubbish));
					}

					builder.setPositiveButton(R.string.general_move, dialogClickListener);
					builder.setNegativeButton(R.string.general_cancel, dialogClickListener);
					builder.show();
				}
				else{

					setMoveToRubbish(false);

					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setMessage(getResources().getString(R.string.confirmation_delete_from_mega));

					//builder.setPositiveButton(R.string.context_delete, dialogClickListener);
					builder.setPositiveButton(R.string.context_remove, dialogClickListener);
					builder.setNegativeButton(R.string.general_cancel, dialogClickListener);
					builder.show();
				}
			}
		}
		else{
			logWarning("handleList NULL");
			return;
		}

	}

	public void showDialogInsertPassword(String link, boolean cancelAccount){
		logDebug("showDialogInsertPassword");

		final String confirmationLink = link;
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);

		final EditText input = new EditText(this);
		layout.addView(input, params);

//		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setHint(getString(R.string.edit_text_insert_pass));
		input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
		input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
//		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if(cancelAccount){
			logDebug("cancelAccount action");
			input.setOnEditorActionListener(new OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						String pass = input.getText().toString().trim();
						if(pass.equals("")||pass.isEmpty()){
							logWarning("Input is empty");
							input.setError(getString(R.string.invalid_string));
							input.requestFocus();
						}
						else {
							logDebug("Action DONE ime - cancel account");
							aC.confirmDeleteAccount(confirmationLink, pass);
							insertPassDialog.dismiss();
						}
					}
					else{
						logDebug("Other IME" + actionId);
					}
					return false;
				}
			});
			input.setImeActionLabel(getString(R.string.delete_account),EditorInfo.IME_ACTION_DONE);
			builder.setTitle(getString(R.string.delete_account));
			builder.setMessage(getString(R.string.delete_account_text_last_step));
			builder.setNegativeButton(getString(R.string.general_dismiss), null);
			builder.setPositiveButton(getString(R.string.delete_account),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

						}
					});
		}
		else{
			logDebug("changeMail action");
			input.setOnEditorActionListener(new OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						String pass = input.getText().toString().trim();
						if(pass.equals("")||pass.isEmpty()){
							logWarning("Input is empty");
							input.setError(getString(R.string.invalid_string));
							input.requestFocus();
						}
						else {
							logDebug("Action DONE ime - change mail");
							aC.confirmChangeMail(confirmationLink, pass);
							insertPassDialog.dismiss();
						}
					}
					else{
						logDebug("Other IME" + actionId);
					}
					return false;
				}
			});
			input.setImeActionLabel(getString(R.string.change_pass),EditorInfo.IME_ACTION_DONE);
			builder.setTitle(getString(R.string.change_mail_title_last_step));
			builder.setMessage(getString(R.string.change_mail_text_last_step));
			builder.setNegativeButton(getString(android.R.string.cancel), null);
			builder.setPositiveButton(getString(R.string.change_pass),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

						}
					});
		}

		builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				hideKeyboard(managerActivity, InputMethodManager.HIDE_NOT_ALWAYS);
			}
		});

		builder.setView(layout);
		insertPassDialog = builder.create();
		insertPassDialog.show();
		if(cancelAccount){
			builder.setNegativeButton(getString(R.string.general_dismiss), null);
			insertPassDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					logDebug("OK BTTN PASSWORD");
					String pass = input.getText().toString().trim();
					if(pass.equals("")||pass.isEmpty()){
						logWarning("Input is empty");
						input.setError(getString(R.string.invalid_string));
						input.requestFocus();
					}
					else {
						logDebug("Positive button pressed - cancel account");
						aC.confirmDeleteAccount(confirmationLink, pass);
						insertPassDialog.dismiss();
					}
				}
			});
		}
		else{
			builder.setNegativeButton(getString(android.R.string.cancel), null);
			insertPassDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					logDebug("OK BTTN PASSWORD");
					String pass = input.getText().toString().trim();
					if(pass.equals("")||pass.isEmpty()){
						logWarning("Input is empty");
						input.setError(getString(R.string.invalid_string));
						input.requestFocus();
					}
					else {
						logDebug("Positive button pressed - change mail");
						aC.confirmChangeMail(confirmationLink, pass);
						insertPassDialog.dismiss();
					}
				}
			});
		}
	}

	public void askConfirmationDeleteAccount(){
		logDebug("askConfirmationDeleteAccount");
		megaApi.multiFactorAuthCheck(megaApi.getMyEmail(), this);

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						aC.deleteAccount();
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.delete_account));

		builder.setMessage(getResources().getString(R.string.delete_account_text));

		builder.setPositiveButton(R.string.delete_account, dialogClickListener);
		builder.setNegativeButton(R.string.general_dismiss, dialogClickListener);
		builder.show();
	}

	void verify2FA(int type) {
		if (type == CANCEL_ACCOUNT_2FA) {
			megaApi.multiFactorAuthCancelAccount(pin, this);
		}
		else if (type == CHANGE_MAIL_2FA){
			megaApi.multiFactorAuthChangeEmail(newMail, pin, this);
		}
		else if (type ==  DISABLE_2FA) {
			megaApi.multiFactorAuthDisable(pin, this);
		}
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		switch (v.getId()) {
			case R.id.pin_first_verify:{
				if (hasFocus) {
					firstPin.setText("");
				}
				break;
			}
			case R.id.pin_second_verify:{
				if (hasFocus) {
					secondPin.setText("");
				}
				break;
			}
			case R.id.pin_third_verify:{
				if (hasFocus) {
					thirdPin.setText("");
				}
				break;
			}
			case R.id.pin_fouth_verify:{
				if (hasFocus) {
					fourthPin.setText("");
				}
				break;
			}
			case R.id.pin_fifth_verify:{
				if (hasFocus) {
					fifthPin.setText("");
				}
				break;
			}
			case R.id.pin_sixth_verify:{
				if (hasFocus) {
					sixthPin.setText("");
				}
				break;
			}
		}
	}

	@Override
	public boolean onLongClick(View v) {
		switch (v.getId()){
			case R.id.pin_first_verify:
			case R.id.pin_second_verify:
			case R.id.pin_third_verify:
			case R.id.pin_fouth_verify:
			case R.id.pin_fifth_verify:
			case R.id.pin_sixth_verify: {
				pinLongClick = true;
				v.requestFocus();
			}
		}
		return false;
	}

	void pasteClipboard() {
		logDebug("pasteClipboard");
		pinLongClick = false;
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		ClipData clipData = clipboard.getPrimaryClip();
		if (clipData != null) {
			String code = clipData.getItemAt(0).getText().toString();
			if (code != null && code.length() == 6) {
				boolean areDigits = true;
				for (int i=0; i<6; i++) {
					if (!Character.isDigit(code.charAt(i))) {
						areDigits = false;
						break;
					}
				}
				if (areDigits) {
					firstPin.setText(""+code.charAt(0));
					secondPin.setText(""+code.charAt(1));
					thirdPin.setText(""+code.charAt(2));
					fourthPin.setText(""+code.charAt(3));
					fifthPin.setText(""+code.charAt(4));
					sixthPin.setText(""+code.charAt(5));
				}
				else {
					firstPin.setText("");
					secondPin.setText("");
					thirdPin.setText("");
					fourthPin.setText("");
					fifthPin.setText("");
					sixthPin.setText("");
				}
			}
		}
	}

	public void showVerifyPin2FA(final int type){
		verifyPin2FADialogType = type;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
		View v = inflater.inflate(R.layout.dialog_verify_2fa, null);
		builder.setView(v);

		TextView titleDialog = (TextView) v.findViewById(R.id.title_dialog_verify);
		if (type == CANCEL_ACCOUNT_2FA){
			titleDialog.setText(getString(R.string.cancel_account_verification));
		}
		else if (type == CHANGE_MAIL_2FA){
			titleDialog.setText(getString(R.string.change_mail_verification));
		}
		else if (type == DISABLE_2FA) {
			titleDialog.setText(getString(R.string.disable_2fa_verification));
		}
		lostYourDeviceButton = (RelativeLayout) v.findViewById(R.id.lost_authentication_device);
		lostYourDeviceButton.setOnClickListener(this);
		verify2faProgressBar = (ProgressBar) v.findViewById(R.id.progressbar_verify_2fa);

		pinError = (TextView) v.findViewById(R.id.pin_2fa_error_verify);
		pinError.setVisibility(View.GONE);

		imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

		firstPin = (EditTextPIN) v.findViewById(R.id.pin_first_verify);
		firstPin.setOnLongClickListener(this);
		firstPin.setOnFocusChangeListener(this);
		imm.showSoftInput(firstPin, InputMethodManager.SHOW_FORCED);
		firstPin.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				if(firstPin.length() != 0){
					secondPin.requestFocus();
					secondPin.setCursorVisible(true);

					if (isFirstTime2fa && !pinLongClick){
						secondPin.setText("");
						thirdPin.setText("");
						fourthPin.setText("");
						fifthPin.setText("");
						sixthPin.setText("");
					}
					else if (pinLongClick) {
						pasteClipboard();
					}
					else  {
						permitVerify(type);
					}
				}
				else {
					if (isErrorShown){
						verifyQuitError();
					}
				}
			}
		});

		secondPin = (EditTextPIN) v.findViewById(R.id.pin_second_verify);
		secondPin.setOnLongClickListener(this);
		secondPin.setOnFocusChangeListener(this);
		imm.showSoftInput(secondPin, InputMethodManager.SHOW_FORCED);
		secondPin.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				if (secondPin.length() != 0){
					thirdPin.requestFocus();
					thirdPin.setCursorVisible(true);

					if (isFirstTime2fa && !pinLongClick) {
						thirdPin.setText("");
						fourthPin.setText("");
						fifthPin.setText("");
						sixthPin.setText("");
					}
					else if (pinLongClick) {
						pasteClipboard();
					}
					else  {
						permitVerify(type);
					}
				}
				else {
					if (isErrorShown){
						verifyQuitError();
					}
				}
			}
		});

		thirdPin = (EditTextPIN) v.findViewById(R.id.pin_third_verify);
		thirdPin.setOnLongClickListener(this);
		thirdPin.setOnFocusChangeListener(this);
		imm.showSoftInput(thirdPin, InputMethodManager.SHOW_FORCED);
		thirdPin.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				if (thirdPin.length()!= 0){
					fourthPin.requestFocus();
					fourthPin.setCursorVisible(true);

					if (isFirstTime2fa && !pinLongClick) {
						fourthPin.setText("");
						fifthPin.setText("");
						sixthPin.setText("");
					}
					else if (pinLongClick) {
						pasteClipboard();
					}
					else  {
						permitVerify(type);
					}
				}
				else {
					if (isErrorShown){
						verifyQuitError();
					}
				}
			}
		});

		fourthPin = (EditTextPIN) v.findViewById(R.id.pin_fouth_verify);
		fourthPin.setOnLongClickListener(this);
		fourthPin.setOnFocusChangeListener(this);
		imm.showSoftInput(fourthPin, InputMethodManager.SHOW_FORCED);
		fourthPin.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				if (fourthPin.length()!=0){
					fifthPin.requestFocus();
					fifthPin.setCursorVisible(true);

					if (isFirstTime2fa && !pinLongClick) {
						fifthPin.setText("");
						sixthPin.setText("");
					}
					else if (pinLongClick) {
						pasteClipboard();
					}
					else  {
						permitVerify(type);
					}
				}
				else {
					if (isErrorShown){
						verifyQuitError();
					}
				}
			}
		});

		fifthPin = (EditTextPIN) v.findViewById(R.id.pin_fifth_verify);
		fifthPin.setOnLongClickListener(this);
		fifthPin.setOnFocusChangeListener(this);
		imm.showSoftInput(fifthPin, InputMethodManager.SHOW_FORCED);
		fifthPin.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				if (fifthPin.length()!=0){
					sixthPin.requestFocus();
					sixthPin.setCursorVisible(true);

					if (isFirstTime2fa && !pinLongClick) {
						sixthPin.setText("");
					}
					else if (pinLongClick) {
						pasteClipboard();
					}
					else  {
						permitVerify(type);
					}
				}
				else {
					if (isErrorShown){
						verifyQuitError();
					}
				}
			}
		});

		sixthPin = (EditTextPIN) v.findViewById(R.id.pin_sixth_verify);
		sixthPin.setOnLongClickListener(this);
		sixthPin.setOnFocusChangeListener(this);
		imm.showSoftInput(sixthPin, InputMethodManager.SHOW_FORCED);
		sixthPin.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				if (sixthPin.length()!=0){
					sixthPin.setCursorVisible(true);
					hideKeyboard(managerActivity, 0);

					if (pinLongClick) {
						pasteClipboard();
					}
					else {
						permitVerify(type);
					}
				}
				else {
					if (isErrorShown){
						verifyQuitError();
					}
				}
			}
		});

		firstPin.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb1 = firstPin.getLayoutParams();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			paramsb1.width = scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb1.width = scaleWidthPx(25, outMetrics);
		}
		firstPin.setLayoutParams(paramsb1);
		LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)firstPin.getLayoutParams();
		textParams.setMargins(0, 0, scaleWidthPx(8, outMetrics), 0);
		firstPin.setLayoutParams(textParams);

		secondPin.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb2 = secondPin.getLayoutParams();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			paramsb2.width = scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb2.width = scaleWidthPx(25, outMetrics);
		}
		secondPin.setLayoutParams(paramsb2);
		textParams = (LinearLayout.LayoutParams)secondPin.getLayoutParams();
		textParams.setMargins(0, 0, scaleWidthPx(8, outMetrics), 0);
		secondPin.setLayoutParams(textParams);
		secondPin.setEt(firstPin);

		thirdPin.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb3 = thirdPin.getLayoutParams();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			paramsb3.width = scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb3.width = scaleWidthPx(25, outMetrics);
		}
		thirdPin.setLayoutParams(paramsb3);
		textParams = (LinearLayout.LayoutParams)thirdPin.getLayoutParams();
		textParams.setMargins(0, 0, scaleWidthPx(25, outMetrics), 0);
		thirdPin.setLayoutParams(textParams);
		thirdPin.setEt(secondPin);

		fourthPin.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb4 = fourthPin.getLayoutParams();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			paramsb4.width = scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb4.width = scaleWidthPx(25, outMetrics);
		}
		fourthPin.setLayoutParams(paramsb4);
		textParams = (LinearLayout.LayoutParams)fourthPin.getLayoutParams();
		textParams.setMargins(0, 0, scaleWidthPx(8, outMetrics), 0);
		fourthPin.setLayoutParams(textParams);
		fourthPin.setEt(thirdPin);

		fifthPin.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb5 = fifthPin.getLayoutParams();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			paramsb5.width = scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb5.width = scaleWidthPx(25, outMetrics);
		}
		fifthPin.setLayoutParams(paramsb5);
		textParams = (LinearLayout.LayoutParams)fifthPin.getLayoutParams();
		textParams.setMargins(0, 0, scaleWidthPx(8, outMetrics), 0);
		fifthPin.setLayoutParams(textParams);
		fifthPin.setEt(fourthPin);

		sixthPin.setGravity(Gravity.CENTER_HORIZONTAL);
		android.view.ViewGroup.LayoutParams paramsb6 = sixthPin.getLayoutParams();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			paramsb6.width = scaleWidthPx(42, outMetrics);
		}
		else {
			paramsb6.width = scaleWidthPx(25, outMetrics);
		}
		sixthPin.setLayoutParams(paramsb6);
		textParams = (LinearLayout.LayoutParams)sixthPin.getLayoutParams();
		textParams.setMargins(0, 0, 0, 0);
		sixthPin.setLayoutParams(textParams);
		sixthPin.setEt(fifthPin);

		verify2FADialog = builder.create();
		verify2FADialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				verify2FADialogIsShown = false;
			}
		});
		verify2FADialog.show();
		verify2FADialogIsShown = true;
	}

	void verifyQuitError(){
		isErrorShown = false;
		pinError.setVisibility(View.GONE);
		firstPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
		secondPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
		thirdPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
		fourthPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
		fifthPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
		sixthPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
	}

	void verifyShowError(){
		logWarning("Pin not correct verifyShowError");
		isFirstTime2fa = false;
		isErrorShown = true;
		pinError.setVisibility(View.VISIBLE);
		firstPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
		secondPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
		thirdPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
		fourthPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
		fifthPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
		sixthPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
	}

	void permitVerify(int type){
		logDebug("permitVerify");
		if (firstPin.length() == 1 && secondPin.length() == 1 && thirdPin.length() == 1 && fourthPin.length() == 1 && fifthPin.length() == 1 && sixthPin.length() == 1){
			hideKeyboard(managerActivity, 0);
			if (sb.length()>0) {
				sb.delete(0, sb.length());
			}
			sb.append(firstPin.getText());
			sb.append(secondPin.getText());
			sb.append(thirdPin.getText());
			sb.append(fourthPin.getText());
			sb.append(fifthPin.getText());
			sb.append(sixthPin.getText());
			pin = sb.toString();
			if (!isErrorShown && pin != null) {
				verify2faProgressBar.setVisibility(View.VISIBLE);
				verify2FA(type);
			}
		}
	}

	private void showOpenLinkError(boolean show, int error) {
		if (openLinkDialog != null) {
			if (show) {
				openLinkDialogIsErrorShown = true;
				openLinkText.setTextColor(ContextCompat.getColor(this, R.color.dark_primary_color));
				openLinkText.getBackground().mutate().clearColorFilter();
				openLinkText.getBackground().mutate().setColorFilter(ContextCompat.getColor(this, R.color.dark_primary_color), PorterDuff.Mode.SRC_ATOP);
				openLinkError.setVisibility(View.VISIBLE);
				if (drawerItem == DrawerItem.CLOUD_DRIVE) {
					if (openLinkText.getText().toString().isEmpty()) {
						openLinkErrorText.setText(R.string.invalid_file_folder_link_empty);
						return;
					}
                    switch (error) {
                        case CHAT_LINK: {
							openLinkText.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
                            openLinkErrorText.setText(R.string.valid_chat_link);
                            openLinkOpenButton.setText(R.string.action_open_chat_link);
                            break;
                        }
                        case CONTACT_LINK: {
							openLinkText.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
                            openLinkErrorText.setText(R.string.valid_contact_link);
                            openLinkOpenButton.setText(R.string.action_open_contact_link);
                            break;
                        }
                        case ERROR_LINK: {
                            openLinkErrorText.setText(R.string.invalid_file_folder_link);
                            break;
                        }
                    }
                }
                else if (drawerItem == DrawerItem.CHAT) {
					if (openLinkText.getText().toString().isEmpty()) {
						openLinkErrorText.setText(R.string.invalid_chat_link_empty);
						return;
					}
                    openLinkErrorText.setText(R.string.invalid_chat_link_args);
                }
			}
			else {
				openLinkDialogIsErrorShown = false;
				if (openLinkError.getVisibility() == View.VISIBLE) {
					openLinkText.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
					openLinkText.getBackground().mutate().clearColorFilter();
					openLinkText.getBackground().mutate().setColorFilter(ContextCompat.getColor(this, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
					openLinkError.setVisibility(View.GONE);
					openLinkOpenButton.setText(R.string.context_open_link);
				}
			}
		}
	}

	private void dismissOpenLinkDialog() {
		try {
			openLinkDialog.dismiss();
			openLinkDialogIsShown = false;
		} catch (Exception e) {}
	}

	private void openLink (String link) {
		// Password link
		if (matchRegexs(link, PASSWORD_LINK_REGEXS)) {
			dismissOpenLinkDialog();
			Intent openLinkIntent = new Intent(this, OpenPasswordLinkActivity.class);
			openLinkIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openLinkIntent.setData(Uri.parse(link));
			startActivity(openLinkIntent);
			return;
		}

		if (drawerItem == DrawerItem.CLOUD_DRIVE) {
			int error = nC.importLink(link);
			if (openLinkError.getVisibility() == View.VISIBLE) {
                switch (error) {
                    case CHAT_LINK: {
						logDebug("Open chat link: correct chat link");
                        showChatLink(link);
                        dismissOpenLinkDialog();
                        break;
                    }
                    case CONTACT_LINK: {
						logDebug("Open contact link: correct contact link");
                        String[] s = link.split("C!");
                        if (s!= null && s.length>1) {
                            long handle = MegaApiAndroid.base64ToHandle(s[1].trim());
                            openContactLink(handle);
                            dismissOpenLinkDialog();
                        }
                        break;
                    }
                }
            }
            else {
                switch (error) {
                    case FILE_LINK:
                    case FOLDER_LINK: {
						logDebug("Do nothing: correct file or folder link");
                        dismissOpenLinkDialog();
                        break;
                    }
                    case CHAT_LINK:
                    case CONTACT_LINK:
                    case ERROR_LINK: {
						logWarning("Show error: invalid link or correct chat or contact link");
                        showOpenLinkError(true, error);
                        break;
                    }
                }
            }
		}
		else if (drawerItem == DrawerItem.CHAT) {
			megaChatApi.checkChatLink(link, managerActivity);
		}
	}

	private void showOpenLinkDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this,
				R.style.AppCompatAlertDialogStyle);
		LayoutInflater inflater = getLayoutInflater();
		View v = inflater.inflate(R.layout.dialog_error_hint, null);
		builder.setView(v).setPositiveButton(R.string.context_open_link, null)
				.setNegativeButton(R.string.general_cancel, null);

		openLinkText = v.findViewById(R.id.text);
		openLinkText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) { }

			@Override
			public void afterTextChanged(Editable s) {
				showOpenLinkError(false, 0);
			}
		});

		openLinkText.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					hideKeyboardView(managerActivity, v, 0);
					openLink(openLinkText.getText().toString());
					return true;
				}
				return false;
			}
		});

		openLinkError = v.findViewById(R.id.error);
		openLinkErrorText = v.findViewById(R.id.error_text);

		if (drawerItem == DrawerItem.CLOUD_DRIVE) {
			builder.setTitle(R.string.action_open_link);
			openLinkText.setHint(R.string.hint_paste_link);
		}
		else if (drawerItem == DrawerItem.CHAT) {
			builder.setTitle(R.string.action_open_chat_link);
			openLinkText.setHint(R.string.hint_enter_chat_link);
		}

		openLinkDialog = builder.create();
		openLinkDialog.setCanceledOnTouchOutside(false);

		try {
			openLinkDialog.show();
			openLinkText.requestFocus();
			openLinkDialogIsShown = true;

			// Set onClickListeners for buttons after showing the dialog would prevent
			// the dialog from dismissing automatically on clicking the buttons
			openLinkOpenButton = openLinkDialog.getButton(AlertDialog.BUTTON_POSITIVE);
			openLinkOpenButton.setOnClickListener((view) -> {
				hideKeyboard(managerActivity, 0);
				openLink(openLinkText.getText().toString());
			});
			openLinkDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener((view) ->
					dismissOpenLinkDialog());
		}catch (Exception e){}
	}

	public void showChatLink(String link){
		logDebug("Link: " + link);
		Intent openChatLinkIntent = new Intent(this, ChatActivityLollipop.class);
		if (joiningToChatLink) {
			openChatLinkIntent.setAction(ACTION_JOIN_OPEN_CHAT_LINK);
		}
		else {
			openChatLinkIntent.setAction(ACTION_OPEN_CHAT_LINK);
		}
		openChatLinkIntent.setData(Uri.parse(link));
		startActivity(openChatLinkIntent);
		drawerItem = DrawerItem.CHAT;
		selectDrawerItemLollipop(drawerItem);
	}

	public void checkPermissions(){
		logDebug("checkPermissionsCall");

		typesCameraPermission = TAKE_PROFILE_PICTURE;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						REQUEST_WRITE_STORAGE);
			}

			boolean hasCameraPermission = checkPermission(Manifest.permission.CAMERA);
			if (!hasCameraPermission) {
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.CAMERA},
						REQUEST_CAMERA);
			}

			if (hasStoragePermission && hasCameraPermission){
				this.takeProfilePicture();
			}
		}
		else{
			this.takeProfilePicture();
		}
	}

	public void takeProfilePicture(){
		checkTakePicture(this, TAKE_PICTURE_PROFILE_CODE);
	}

	public void showCancelMessage(){
		logDebug("showCancelMessage");
		AlertDialog cancelDialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		builder.setTitle(getString(R.string.title_cancel_subscriptions));

		LayoutInflater inflater = getLayoutInflater();
		View dialogLayout = inflater.inflate(R.layout.dialog_cancel_subscriptions, null);
		TextView message = (TextView) dialogLayout.findViewById(R.id.dialog_cancel_text);
		final EditText text = (EditText) dialogLayout.findViewById(R.id.dialog_cancel_feedback);

		float density = getResources().getDisplayMetrics().density;

		float scaleW = getScaleW(outMetrics, density);

		message.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleW));
		text.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleW));

		builder.setView(dialogLayout);

		builder.setPositiveButton(getString(R.string.send_cancel_subscriptions), new android.content.DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				feedback = text.getText().toString();
				if(feedback.matches("")||feedback.isEmpty()){
					showSnackbar(SNACKBAR_TYPE, getString(R.string.reason_cancel_subscriptions), -1);
				}
				else{
					showCancelConfirmation(feedback);
				}
			}
		});

		builder.setNegativeButton(getString(R.string.general_dismiss), new android.content.DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});

		cancelDialog = builder.create();
		cancelDialog.show();
//		brandAlertDialog(cancelDialog);
	}

	public void showPresenceStatusDialog(){
		logDebug("showPresenceStatusDialog");

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		final CharSequence[] items = {getString(R.string.online_status), getString(R.string.away_status), getString(R.string.busy_status), getString(R.string.offline_status)};
		int statusToShow = megaChatApi.getOnlineStatus();
		switch(statusToShow){
			case MegaChatApi.STATUS_ONLINE:{
				statusToShow = 0;
				break;
			}
			case MegaChatApi.STATUS_AWAY:{
				statusToShow = 1;
				break;
			}
			case MegaChatApi.STATUS_BUSY:{
				statusToShow = 2;
				break;
			}
			case MegaChatApi.STATUS_OFFLINE:{
				statusToShow = 3;
				break;
			}
		}
		dialogBuilder.setSingleChoiceItems(items, statusToShow, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {

				presenceStatusDialog.dismiss();
				switch(item) {
					case 0:{
						megaChatApi.setOnlineStatus(MegaChatApi.STATUS_ONLINE, managerActivity);
						break;
					}
					case 1:{
						megaChatApi.setOnlineStatus(MegaChatApi.STATUS_AWAY, managerActivity);
						break;
					}
					case 2:{
						megaChatApi.setOnlineStatus(MegaChatApi.STATUS_BUSY, managerActivity);
						break;
					}
					case 3:{
						megaChatApi.setOnlineStatus(MegaChatApi.STATUS_OFFLINE, managerActivity);
						break;
					}
				}
			}
		});
		dialogBuilder.setTitle(getString(R.string.status_label));
		presenceStatusDialog = dialogBuilder.create();
//		presenceStatusDialog.se
		presenceStatusDialog.show();
	}

	public void showCancelConfirmation(final String feedback){
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
			        case DialogInterface.BUTTON_POSITIVE:
			        {
						logDebug("Feedback: " + feedback);
			        	megaApi.creditCardCancelSubscriptions(feedback, managerActivity);
			        	break;
			        }
			        case DialogInterface.BUTTON_NEGATIVE:
			        {
			            //No button clicked
						logDebug("Feedback: " + feedback);
			            break;
			        }
		        }
		    }
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.confirmation_cancel_subscriptions).setPositiveButton(R.string.general_yes, dialogClickListener)
		    .setNegativeButton(R.string.general_no, dialogClickListener).show();

	}

	@Override
	public void uploadFromDevice() {
		chooseFromDevice(this);
	}

	@Override
	public void uploadFromSystem() {
		chooseFromSystem(this);
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
	public void showNewFolderDialog() {
		logDebug("showNewFolderDialogKitLollipop");

		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(scaleWidthPx(20, outMetrics), scaleWidthPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);

		final EditText input = new EditText(this);
		layout.addView(input, params);

		LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params1.setMargins(scaleWidthPx(20, outMetrics), 0, scaleWidthPx(17, outMetrics), 0);

		final RelativeLayout error_layout = new RelativeLayout(ManagerActivityLollipop.this);
		layout.addView(error_layout, params1);

		final ImageView error_icon = new ImageView(ManagerActivityLollipop.this);
		error_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_input_warning));
		error_layout.addView(error_icon);
		RelativeLayout.LayoutParams params_icon = (RelativeLayout.LayoutParams) error_icon.getLayoutParams();


		params_icon.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		error_icon.setLayoutParams(params_icon);

		error_icon.setColorFilter(ContextCompat.getColor(ManagerActivityLollipop.this, R.color.login_warning));

		final TextView textError = new TextView(ManagerActivityLollipop.this);
		error_layout.addView(textError);
		RelativeLayout.LayoutParams params_text_error = (RelativeLayout.LayoutParams) textError.getLayoutParams();
		params_text_error.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error.width = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error.addRule(RelativeLayout.CENTER_VERTICAL);
		params_text_error.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params_text_error.setMargins(scaleWidthPx(3, outMetrics), 0, 0, 0);
		textError.setLayoutParams(params_text_error);

		textError.setTextColor(ContextCompat.getColor(ManagerActivityLollipop.this, R.color.login_warning));

		error_layout.setVisibility(View.GONE);

		input.getBackground().mutate().clearColorFilter();
		input.getBackground().mutate().setColorFilter(ContextCompat.getColor(this, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
		input.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				if (error_layout.getVisibility() == View.VISIBLE) {
					error_layout.setVisibility(View.GONE);
					input.getBackground().mutate().clearColorFilter();
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(managerActivity, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
				}
			}
		});

		input.setSingleLine();
		input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
		input.setHint(getString(R.string.context_new_folder_name));
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						input.getBackground().mutate().setColorFilter(ContextCompat.getColor(managerActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError.setText(getString(R.string.invalid_string));
						error_layout.setVisibility(View.VISIBLE);
						input.requestFocus();

					} else {
						boolean result = matches(regex, value);
						if (result) {
							input.getBackground().mutate().setColorFilter(ContextCompat.getColor(managerActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
							textError.setText(getString(R.string.invalid_characters));
							error_layout.setVisibility(View.VISIBLE);
							input.requestFocus();

						} else {
							createFolder(value);
							newFolderDialog.dismiss();
						}
					}
					return true;
				}
				return false;
			}
		});
		input.setImeActionLabel(getString(R.string.general_create), EditorInfo.IME_ACTION_DONE);
		input.requestFocus();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.menu_new_folder));
		builder.setPositiveButton(getString(R.string.general_create),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() == 0) {
							return;
						}
						createFolder(value);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				input.getBackground().clearColorFilter();
			}
		});
		builder.setView(layout);
		newFolderDialog = builder.create();
		newFolderDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		newFolderDialog.show();
		newFolderDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String value = input.getText().toString().trim();
				 if (value.length() == 0) {
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(managerActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
					textError.setText(getString(R.string.invalid_string));
					error_layout.setVisibility(View.VISIBLE);
					input.requestFocus();

				} else {
					boolean result = matches(regex, value);
					if (result) {
						input.getBackground().mutate().setColorFilter(ContextCompat.getColor(managerActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError.setText(getString(R.string.invalid_characters));
						error_layout.setVisibility(View.VISIBLE);
						input.requestFocus();

					} else {
						createFolder(value);
						newFolderDialog.dismiss();
					}
				}
			}
		});
	}

	public void showRBNotDisabledDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = this.getLayoutInflater();
		View v = inflater.inflate(R.layout.dialog_two_vertical_buttons, null);
		builder.setView(v);

		TextView title = (TextView) v.findViewById(R.id.dialog_title);
		title.setText(getString(R.string.settings_rb_scheduler_enable_title));
		TextView text = (TextView) v.findViewById(R.id.dialog_text);
		text.setText(getString(R.string.settings_rb_scheduler_alert_disabling));

		Button firstButton = (Button) v.findViewById(R.id.dialog_first_button);
		firstButton.setText(getString(R.string.button_plans_almost_full_warning));
		firstButton.setOnClickListener(new   View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				generalDialog.dismiss();
				//Show UpgradeAccountActivity
				drawerItemPreUpgradeAccount = drawerItem;
				drawerItem = DrawerItem.ACCOUNT;
				accountFragment=UPGRADE_ACCOUNT_FRAGMENT;
				selectDrawerItemAccount();
				setTabsVisibility();
			}
		});

		Button secondButton = (Button) v.findViewById(R.id.dialog_second_button);
		secondButton.setText(getString(R.string.button_not_now_rich_links));
		secondButton.setOnClickListener(new   View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				generalDialog.dismiss();
			}
		});

		generalDialog = builder.create();
		generalDialog.show();
	}

	public void showRbSchedulerValueDialog(final boolean isEnabling){
		logDebug("showRbSchedulerValueDialog");

		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(scaleWidthPx(20, outMetrics), scaleWidthPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);

		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		layout.addView(input, params);

//		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
		input.setHint(getString(R.string.hint_days));
//		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						return true;
					}

					try{
						int daysCount = Integer.parseInt(value);

						if(((MegaApplication) getApplication()).getMyAccountInfo().getAccountType()>MegaAccountDetails.ACCOUNT_TYPE_FREE) {
							//PRO account
							if(daysCount>6){
								//Set new value
								setRBSchedulerValue(value);
								newFolderDialog.dismiss();
							}
							else{
								//Show again the dialog
								input.setText("");
								input.requestFocus();
							}
						}
						else{
							//PRO account
							if(daysCount>6 && daysCount<31){
								//Set new value
								setRBSchedulerValue(value);
								newFolderDialog.dismiss();
							}
							else{
								//Show again the dialog
								input.setText("");
								input.requestFocus();
							}
						}
					}
					catch (Exception e){
						//Show again the dialog
						input.setText("");
						input.requestFocus();
					}

					return true;
				}
				return false;
			}
		});
		input.setImeActionLabel(getString(R.string.general_create),EditorInfo.IME_ACTION_DONE);
		input.requestFocus();

		final TextView text = new TextView(ManagerActivityLollipop.this);

		if(((MegaApplication) getApplication()).getMyAccountInfo().getAccountType()>MegaAccountDetails.ACCOUNT_TYPE_FREE) {
			text.setText(getString(R.string.settings_rb_scheduler_enable_period_PRO));
		}
		else{
			text.setText(getString(R.string.settings_rb_scheduler_enable_period_FREE));
		}

		float density = getResources().getDisplayMetrics().density;
		float scaleW = getScaleW(outMetrics, density);
		text.setTextSize(TypedValue.COMPLEX_UNIT_SP, (11*scaleW));
		layout.addView(text);

		LinearLayout.LayoutParams params_text_error = (LinearLayout.LayoutParams) text.getLayoutParams();
		params_text_error.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error.width = ViewGroup.LayoutParams.WRAP_CONTENT;
//		params_text_error.addRule(RelativeLayout.CENTER_VERTICAL);
//		params_text_error.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params_text_error.setMargins(scaleWidthPx(25, outMetrics), 0,scaleWidthPx(25, outMetrics),0);
		text.setLayoutParams(params_text_error);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.settings_rb_scheduler_select_days_title));
		builder.setPositiveButton(getString(R.string.general_ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), new android.content.DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(isEnabling && getSettingsFragment() != null){
					sttFLol.updateRBScheduler(0);
				}
			}
		});
		builder.setView(layout);
		newFolderDialog = builder.create();
		newFolderDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		newFolderDialog.show();

		newFolderDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{

				String value = input.getText().toString().trim();

				if (value.length() == 0) {
					return;
				}

				try{
					int daysCount = Integer.parseInt(value);

					if(((MegaApplication) getApplication()).getMyAccountInfo().getAccountType()>MegaAccountDetails.ACCOUNT_TYPE_FREE) {
						//PRO account
						if(daysCount>6){
							//Set new value
							setRBSchedulerValue(value);
							newFolderDialog.dismiss();
						}
						else{
							//Show again the dialog
							input.setText("");
							input.requestFocus();
						}
					}
					else{
						//PRO account
						if(daysCount>6 && daysCount<31){
							//Set new value
							setRBSchedulerValue(value);
							newFolderDialog.dismiss();
						}
						else{
							//Show again the dialog
							input.setText("");
							input.requestFocus();
						}
					}
				}
				catch (Exception e){
					//Show again the dialog
					input.setText("");
					input.requestFocus();
				}
			}
		});
	}

	public void setRBSchedulerValue(String value){
		logDebug("Value: "+ value);
		int intValue = Integer.parseInt(value);

		if(megaApi!=null){
			megaApi.setRubbishBinAutopurgePeriod(intValue, this);
		}
	}

	public void enableLastGreen(boolean enable){
		logDebug("Enable Last Green: "+ enable);

		if(megaChatApi!=null){
			megaChatApi.setLastGreenVisible(enable, this);
		}
	}

	public void showAutoAwayValueDialog(){
		logDebug("showAutoAwayValueDialog");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_autoaway, null);
        builder.setView(v);

        final EditText input = v.findViewById(R.id.autoaway_edittext);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
		input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = validateAutoAway(v.getText());
					if (value != null) {
						setAutoAwayValue(value, false);
					}
					newFolderDialog.dismiss();
					return true;
				}
				return false;
			}
		});
		input.setImeActionLabel(getString(R.string.general_create),EditorInfo.IME_ACTION_DONE);
		input.requestFocus();

		builder.setTitle(getString(R.string.title_dialog_set_autoaway_value));
		Button set = (Button) v.findViewById(R.id.autoaway_set_button);
		set.setOnClickListener(new OnClickListener() {
            @Override
			public void onClick(View v) {
				String value = validateAutoAway(input.getText());
				if (value != null) {
					setAutoAwayValue(value, false);
				}
				newFolderDialog.dismiss();
			}
        });
		Button cancel = (Button) v.findViewById(R.id.autoaway_cancel_button);
	    cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setAutoAwayValue("-1", true);
				newFolderDialog.dismiss();
            }
        });

		newFolderDialog = builder.create();
		newFolderDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		newFolderDialog.show();
	}

	private String validateAutoAway(CharSequence value) {
		int timeout;
		try {
			timeout = Integer.parseInt(value.toString().trim());
			if (timeout <= 0) {
				timeout = 1;
			} else if (timeout > MAX_AUTOAWAY_TIMEOUT) {
				timeout = MAX_AUTOAWAY_TIMEOUT;
			}
			return String.valueOf(timeout);
		} catch (Exception e) {
			logWarning("Unable to parse user input, user entered: '" + value + "'");
			return null;
		}
	}

	public void setAutoAwayValue(String value, boolean cancelled){
		logDebug("Value: " + value);
		if(cancelled){
			if(getSettingsFragment() != null){
				sttFLol.updatePresenceConfigChat(true, null);
			}
		}
		else{
			int timeout = Integer.parseInt(value);
			if(megaChatApi!=null){
				megaChatApi.setPresenceAutoaway(true, timeout*60);
			}
		}
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
			case CLOUD_DRIVE:
				parentHandle = getParentHandleBrowser();
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

		if (parentNode == null && errorString != null){
			showSnackbar(SNACKBAR_TYPE, errorString, -1);
			logDebug(errorString + ": parentNode == null");
			return null;
		}

		return parentNode;
	}

	private void createFolder(String title) {
		logDebug("createFolder");
		if (!isOnline(this)){
			showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
			return;
		}

		if(isFinishing()){
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

		statusDialog = null;
		try {
			statusDialog = new ProgressDialog(this);
			statusDialog.setMessage(getString(R.string.context_creating_folder));
			statusDialog.show();
		}
		catch(Exception e){
			logDebug("Exception showing 'Creating folder' dialog");
			e.printStackTrace();
			return;
		}

		megaApi.createFolder(title, parentNode, this);
	}

	public void showClearRubbishBinDialog(){
		logDebug("showClearRubbishBinDialog");

		rubbishBinFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
		if (rubbishBinFLol != null) {
			if (rubbishBinFLol.isVisible()) {
				rubbishBinFLol.notifyDataSetChanged();
			}
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.context_clear_rubbish));
		builder.setMessage(getString(R.string.clear_rubbish_confirmation));
		/*builder.setPositiveButton(getString(R.string.context_delete),new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						nC.cleanRubbishBin();
					}
				});*/
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

	public void showConfirmationClearAllVersions(){
		logDebug("showConfirmationClearAllVersions");

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.settings_file_management_delete_versions));
		builder.setMessage(getString(R.string.text_confirmation_dialog_delete_versions));

		builder.setPositiveButton(getString(R.string.context_delete),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						nC.clearAllVersions();
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		clearRubbishBinDialog = builder.create();
		clearRubbishBinDialog.show();
	}

	public void showPanelSetPinLock(){
		logDebug("showPanelSetPinLock");

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		final CharSequence[] items = {getString(R.string.four_pin_lock), getString(R.string.six_pin_lock), getString(R.string.AN_pin_lock)};

		getSettingsFragment();

		dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				setPinDialog.dismiss();
				switch(item) {
					case 0:{
						dbH.setPinLockType(PIN_4);
						if(sttFLol!=null){
							sttFLol.intentToPinLock();
						}
						break;
					}
					case 1:{
						dbH.setPinLockType(PIN_6);
						if(sttFLol!=null){
							sttFLol.intentToPinLock();
						}
						break;
					}
					case 2:{
						dbH.setPinLockType(PIN_ALPHANUMERIC);
						if(sttFLol!=null){
							sttFLol.intentToPinLock();
						}
						break;
					}
				}
			}
		});
		dialogBuilder.setTitle(getString(R.string.pin_lock_type));

		dialogBuilder.setOnKeyListener(new Dialog.OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
				logDebug("onKeyListener: " + keyCode);
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					logDebug("Cancel set pin action");
					setPinDialog.dismiss();
					if(sttFLol!=null){
						sttFLol.cancelSetPinLock();
					}
				}
				return true;
			}
		});

		dialogBuilder.setOnCancelListener(
				new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						logDebug("setOnCancelListener setPin");
						setPinDialog.dismiss();
						if(sttFLol!=null){
							sttFLol.cancelSetPinLock();
						}
					}
				}
		);

		setPinDialog = dialogBuilder.create();
		setPinDialog.setCanceledOnTouchOutside(true);
		setPinDialog.show();
	}

	public void chooseAddContactDialog(boolean isMegaContact) {
		logDebug("chooseAddContactDialog");
		if (isMegaContact) {
			if (megaApi != null && megaApi.getRootNode() != null) {
				Intent intent = new Intent(this, AddContactActivityLollipop.class);
				intent.putExtra("contactType", CONTACT_TYPE_MEGA);
				startActivityForResult(intent, REQUEST_CREATE_CHAT);
			}
			else{
				logWarning("Online but not megaApi");
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
			}
		}
		else{
			addContactFromPhone();
		}
	}

	public void addContactFromPhone() {
		Intent in = new Intent(this, InviteContactActivity.class);
		in.putExtra("contactType", CONTACT_TYPE_DEVICE);
		startActivityForResult(in, REQUEST_INVITE_CONTACT_FROM_DEVICE);
	}

	public void showNewContactDialog(){
		logDebug("showNewContactDialog");

		cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CONTACTS.getTag());
		if (cFLol != null){
			if (drawerItem == DrawerItem.CONTACTS){
				cFLol.setPositionClicked(-1);
				cFLol.notifyDataSetChanged();
			}
		}

		LinearLayout layout = new LinearLayout(this);
	    layout.setOrientation(LinearLayout.VERTICAL);
	    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);

		final EditText input = new EditText(this);
		layout.addView(input, params);

		LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params1.setMargins(scaleWidthPx(20, outMetrics), 0, scaleWidthPx(17, outMetrics), 0);

		final RelativeLayout error_layout_email = new RelativeLayout(ManagerActivityLollipop.this);
		layout.addView(error_layout_email, params1);

		final ImageView error_icon_email = new ImageView(ManagerActivityLollipop.this);
		error_icon_email.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_input_warning));
		error_layout_email.addView(error_icon_email);
		RelativeLayout.LayoutParams params_icon = (RelativeLayout.LayoutParams) error_icon_email.getLayoutParams();


		params_icon.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		error_icon_email.setLayoutParams(params_icon);

		error_icon_email.setColorFilter(ContextCompat.getColor(ManagerActivityLollipop.this, R.color.login_warning));

		final TextView textError_email = new TextView(ManagerActivityLollipop.this);
		error_layout_email.addView(textError_email);
		RelativeLayout.LayoutParams params_text_error = (RelativeLayout.LayoutParams) textError_email.getLayoutParams();
		params_text_error.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error.addRule(RelativeLayout.CENTER_VERTICAL);
		params_text_error.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params_text_error.addRule(RelativeLayout.LEFT_OF, error_icon_email.getId());
		params_text_error.setMargins(scaleWidthPx(3, outMetrics), 0,0,0);
		textError_email.setLayoutParams(params_text_error);

		textError_email.setTextColor(ContextCompat.getColor(ManagerActivityLollipop.this, R.color.login_warning));

		error_layout_email.setVisibility(View.GONE);

//		input.setId(EDIT_TEXT_ID);
		input.getBackground().mutate().clearColorFilter();
		input.getBackground().mutate().setColorFilter(ContextCompat.getColor(this, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
		input.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				if(error_layout_email.getVisibility() == View.VISIBLE){
					error_layout_email.setVisibility(View.GONE);
					input.getBackground().mutate().clearColorFilter();
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(managerActivity, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
				}
			}
		});
		input.setSingleLine();
		input.setHint(getString(R.string.context_new_contact_name));
		input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
//		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = input.getText().toString().trim();
					String emailError = getEmailError(value, managerActivity);
					if (emailError != null) {
//                        input.setError(emailError);
						input.getBackground().mutate().setColorFilter(ContextCompat.getColor(managerActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError_email.setText(emailError);
						error_layout_email.setVisibility(View.VISIBLE);
					} else {
						cC.inviteContact(value);
						addContactDialog.dismiss();
					}
				}
				else{
					logDebug("Other IME" + actionId);
				}
				return false;
			}
		});
		input.setImeActionLabel(getString(R.string.general_add),EditorInfo.IME_ACTION_DONE);
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showKeyboardDelayed(v);
				}
			}
		});

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.menu_add_contact));
		builder.setPositiveButton(getString(R.string.general_add),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				input.getBackground().clearColorFilter();
			}
		});
		builder.setView(layout);
		addContactDialog = builder.create();
		addContactDialog.show();
		addContactDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String value = input.getText().toString().trim();
				String emailError = getEmailError(value, managerActivity);
				if (emailError != null) {
//					input.setError(emailError);
					input.getBackground().mutate().setColorFilter(ContextCompat.getColor(managerActivity, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
					textError_email.setText(emailError);
					error_layout_email.setVisibility(View.VISIBLE);
				} else {
					cC.inviteContact(value);
					addContactDialog.dismiss();
				}
			}
		});
	}

	public void showConfirmationRemoveContact(final MegaUser c){
		logDebug("showConfirmationRemoveContact");
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						cC.removeContact(c);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String title = getResources().getQuantityString(R.plurals.title_confirmation_remove_contact, 1);
		builder.setTitle(title);
		String message= getResources().getQuantityString(R.plurals.confirmation_remove_contact, 1);
		builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();

	}

	public void showConfirmationRemoveContacts(final ArrayList<MegaUser> c){
		logDebug("showConfirmationRemoveContacts");
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						cC.removeMultipleContacts(c);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String title = getResources().getQuantityString(R.plurals.title_confirmation_remove_contact, c.size());
		builder.setTitle(title);
		String message= getResources().getQuantityString(R.plurals.confirmation_remove_contact, c.size());
		builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();

	}

	public void showConfirmationRemoveContactRequest(final MegaContactRequest r){
		logDebug("showConfirmationRemoveContactRequest");
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						cC.removeInvitationContact(r);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String message= getResources().getString(R.string.confirmation_delete_contact_request,r.getTargetEmail());
		builder.setMessage(message).setPositiveButton(R.string.context_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();

	}

	public void showConfirmationRemoveContactRequests(final List<MegaContactRequest> r){
		logDebug("showConfirmationRemoveContactRequests");
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						cC.deleteMultipleSentRequestContacts(r);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		String message="";
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if(r.size()==1){
			message= getResources().getString(R.string.confirmation_delete_contact_request,r.get(0).getTargetEmail());
		}else{
			message= getResources().getString(R.string.confirmation_remove_multiple_contact_request,r.size());
		}

		builder.setMessage(message).setPositiveButton(R.string.context_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();

	}

	public void showConfirmationLeaveMultipleShares (final ArrayList<Long> handleList){
		logDebug("showConfirmationleaveMultipleShares");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		        	nC.leaveMultipleIncomingShares(handleList);
		            break;

		        case DialogInterface.BUTTON_NEGATIVE:
		            //No button clicked
		            break;
		        }
		    }
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		builder.setTitle(getResources().getString(R.string.alert_leave_share));
		String message= getResources().getString(R.string.confirmation_leave_share_folder);
		builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
	    	.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationRemoveAllSharingContacts(final List<MegaNode> shares) {
		if (shares.size() == 1) {
			showConfirmationRemoveAllSharingContacts(megaApi.getOutShares(shares.get(0)), shares.get(0));
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setMessage(getString(R.string.alert_remove_several_shares, shares.size()))
				.setPositiveButton(R.string.general_remove, (dialog, which) -> nC.removeSeveralFolderShares(shares))
				.setNegativeButton(R.string.general_cancel, (dialog, which) -> {})
				.show();
	}

	public void showConfirmationRemoveAllSharingContacts (final ArrayList<MegaShare> shareList, final MegaNode n){
		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		int size = shareList.size();
		String message = getResources().getQuantityString(R.plurals.confirmation_remove_outgoing_shares, size, size);

		builder.setMessage(message)
				.setPositiveButton(R.string.general_remove, (dialog, which) -> nC.removeShares(shareList, n))
				.setNegativeButton(R.string.general_cancel, (dialog, which) -> {})
				.show();
	}

	public void showConfirmationRemovePublicLink (final MegaNode n){
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

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		MegaNode finalNode = node;
		builder.setMessage(message)
				.setPositiveButton(R.string.general_remove, (dialog, which) -> {
					if (finalNode != null) {
						if (!isOnline(managerActivity)){
							showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
							return;
						}
						nC.removeLink(finalNode, new ExportListener(managerActivity, true, 1));
					} else {
						nC.removeLinks(nodes);
					}
				})
				.setNegativeButton(R.string.general_cancel, (dialog, which) -> {})
				.show();

		refreshAfterMovingToRubbish();
	}

	public void showConfirmationLeaveIncomingShare (final MegaNode n){
		logDebug("showConfirmationLeaveIncomingShare");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE: {
					nC.leaveIncomingShare(managerActivity, n);
					break;
				}
		        case DialogInterface.BUTTON_NEGATIVE:
		            //No button clicked
		            break;
		        }
		    }
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		builder.setTitle(getResources().getString(R.string.alert_leave_share));
		String message= getResources().getString(R.string.confirmation_leave_share_folder);
		builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
	    	.setNegativeButton(R.string.general_cancel, dialogClickListener).show();

	}

	public void showConfirmationLeaveChat (final MegaChatRoom c){
		logDebug("showConfirmationLeaveChat");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {
						ChatController chatC = new ChatController(managerActivity);
						chatC.leaveChat(c);
						break;
					}
					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getString(R.string.title_confirmation_leave_group_chat));
		String message= getResources().getString(R.string.confirmation_leave_group_chat);
		builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationLeaveChat (final MegaChatListItem c){
		logDebug("showConfirmationLeaveChat");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {
						ChatController chatC = new ChatController(managerActivity);
						chatC.leaveChat(c.getChatId());
						break;
					}
					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getString(R.string.title_confirmation_leave_group_chat));
		String message= getResources().getString(R.string.confirmation_leave_group_chat);
		builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}
	public void showConfirmationLeaveChats (final  ArrayList<MegaChatListItem> cs){
		logDebug("showConfirmationLeaveChats");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {
						ChatController chatC = new ChatController(managerActivity);

						for(int i=0;i<cs.size();i++){
							MegaChatListItem chat = cs.get(i);
							if(chat!=null){
								chatC.leaveChat(chat.getChatId());
							}
						}

						break;
					}
					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						rChatFL = (RecentChatsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
						if(rChatFL!=null){
							rChatFL.clearSelections();
							rChatFL.hideMultipleSelect();
						}
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getString(R.string.title_confirmation_leave_group_chat));
		String message= getResources().getString(R.string.confirmation_leave_group_chat);
		builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationClearChat(final MegaChatListItem c){
		logDebug("showConfirmationClearChat");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						logDebug("Clear chat!");
//						megaChatApi.truncateChat(chatHandle, MegaChatHandle.MEGACHAT_INVALID_HANDLE);
						logDebug("Clear history selected!");
						ChatController chatC = new ChatController(managerActivity);
						chatC.clearHistory(c.getChatId());
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
		String message= getResources().getString(R.string.confirmation_clear_group_chat);
		builder.setTitle(R.string.title_confirmation_clear_group_chat);
		builder.setMessage(message).setPositiveButton(R.string.general_clear, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationResetPasswordFromMyAccount (){
		logDebug("showConfirmationResetPasswordFromMyAccount");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {
						maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
						if(maFLol!=null){
							maFLol.resetPass();
						}
						break;
					}
					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String message= getResources().getString(R.string.email_verification_text_change_pass);
		builder.setMessage(message).setPositiveButton(R.string.general_ok, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationResetPassword (final String link){
		logDebug("Link: " + link);

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {
						Intent intent = new Intent(managerActivity, ChangePasswordActivityLollipop.class);
						intent.setAction(ACTION_RESET_PASS_FROM_LINK);
						intent.setData(Uri.parse(link));
						String key = megaApi.exportMasterKey();
						intent.putExtra("MK", key);
						startActivity(intent);
						break;
					}
					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getString(R.string.title_dialog_insert_MK));
		String message= getResources().getString(R.string.text_reset_pass_logged_in);
		builder.setMessage(message).setPositiveButton(R.string.pin_lock_enter, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void cameraUploadsClicked(){
		logDebug("cameraUplaodsClicked");
		drawerItem = DrawerItem.CAMERA_UPLOADS;
		setBottomNavigationMenuItemChecked(CAMERA_UPLOADS_BNV);
		selectDrawerItemLollipop(drawerItem);
	}

	public void secondaryMediaUploadsClicked(){
		logDebug("secondaryMediaUploadsClicked");
		drawerItem = DrawerItem.MEDIA_UPLOADS;
		setBottomNavigationMenuItemChecked(HIDDEN_BNV);
		selectDrawerItemLollipop(drawerItem);
	}

	public void setInitialCloudDrive (){
		drawerItem = DrawerItem.CLOUD_DRIVE;
		setBottomNavigationMenuItemChecked(CLOUD_DRIVE_BNV);
		//firstTimeAfterInstallation = true;
		selectDrawerItemLollipop(drawerItem);
	}

	public void refreshCameraUpload(){
		drawerItem = DrawerItem.CAMERA_UPLOADS;
		setBottomNavigationMenuItemChecked(CAMERA_UPLOADS_BNV);

		refreshFragment(FragmentTag.CAMERA_UPLOADS.getTag());
	}

	public void showNodeOptionsPanel(MegaNode node){
		logDebug("showNodeOptionsPanel");

		if (node == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

		selectedNode = node;
		bottomSheetDialogFragment = new NodeOptionsBottomSheetDialogFragment();
		bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
	}

	public void showOptionsPanel(MegaOffline sNode){
		logDebug("showNodeOptionsPanel-Offline");

		if (sNode == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

		selectedOfflineNode = sNode;
		bottomSheetDialogFragment = new OfflineOptionsBottomSheetDialogFragment();
		bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
	}

	public void showContactOptionsPanel(MegaContactAdapter user){
		logDebug("showContactOptionsPanel");

		if(!isOnline(this)){
			showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
			return;
		}

		if (user == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

		selectedUser = user;
		bottomSheetDialogFragment = new ContactsBottomSheetDialogFragment();
		bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
	}

	public void showSentRequestOptionsPanel(MegaContactRequest request){
		logDebug("showSentRequestOptionsPanel");
		if (request == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

		selectedRequest = request;
		bottomSheetDialogFragment = new SentRequestBottomSheetDialogFragment();
		bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
	}

	public void showReceivedRequestOptionsPanel(MegaContactRequest request){
		logDebug("showReceivedRequestOptionsPanel");

		if (request == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

		selectedRequest = request;
		bottomSheetDialogFragment = new ReceivedRequestBottomSheetDialogFragment();
		bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
	}

	public void showMyAccountOptionsPanel() {
		logDebug("showMyAccountOptionsPanel");

		if (isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

		bottomSheetDialogFragment = new MyAccountBottomSheetDialogFragment();
		bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
	}

	public void showUploadPanel() {
		logDebug("showUploadPanel");
		if (!hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			requestPermission(this, REQUEST_READ_WRITE_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
			return;
		}

		if (isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        bottomSheetDialogFragment = new UploadBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
	}

	public void updateAccountDetailsVisibleInfo(){
		logDebug("updateAccountDetailsVisibleInfo");
		if(isFinishing()){
			return;
		}

		if (app == null || app.getMyAccountInfo() == null) {
			return;
		}

		MyAccountInfo info = app.getMyAccountInfo();
		View settingsSeparator = null;

		if (nV != null) {
			settingsSeparator = nV.findViewById(R.id.settings_separator);
		}

		if (usedSpaceLayout != null) {
			if (!info.isBusinessStatusReceived() || megaApi.isBusinessAccount()) {
				usedSpaceLayout.setVisibility(View.GONE);
				upgradeAccount.setVisibility(View.GONE);
				if (settingsSeparator != null) {
					settingsSeparator.setVisibility(View.GONE);
				}
				if (megaApi.isBusinessAccount()) {
					businessLabel.setVisibility(View.VISIBLE);
				}

				if (getSettingsFragment() != null) {
					sttFLol.updateCancelAccountSetting();
				}
			} else {
				businessLabel.setVisibility(View.GONE);
				upgradeAccount.setVisibility(View.VISIBLE);
				if (settingsSeparator != null) {
					settingsSeparator.setVisibility(View.GONE);
				}

				String textToShow = String.format(getResources().getString(R.string.used_space), info.getUsedFormatted(), info.getTotalFormatted());
				try {
					textToShow = textToShow.replace("[A]", "<font color=\'#00bfa5\'>");
					textToShow = textToShow.replace("[/A]", "</font>");
					textToShow = textToShow.replace("[B]", "<font color=\'#000000\'>");
					textToShow = textToShow.replace("[/B]", "</font>");
				} catch (Exception e) {
					logWarning("Exception formatting string", e);
				}
				spaceTV.setText(getSpannedHtmlText(textToShow));
				int progress = info.getUsedPerc();
				long usedSpace = info.getUsedStorage();
				logDebug("Progress: " + progress + ", Used space: " + usedSpace);
				usedSpacePB.setProgress(progress);
				if (progress >= 0 && usedSpace >= 0) {
					usedSpaceLayout.setVisibility(View.VISIBLE);
				} else {
					usedSpaceLayout.setVisibility(View.GONE);
				}
			}
		}
		else{
			logWarning("usedSpaceLayout is NULL");
		}

		updateSubscriptionLevel(app.getMyAccountInfo());

		switch (storageState) {
			case MegaApiJava.STORAGE_STATE_GREEN:
				usedSpacePB.setProgressDrawable(getResources().getDrawable(
						R.drawable.custom_progress_bar_horizontal_ok));
				break;

			case MegaApiJava.STORAGE_STATE_ORANGE:
				usedSpacePB.setProgressDrawable(getResources().getDrawable(
						R.drawable.custom_progress_bar_horizontal_warning));
				break;

			case MegaApiJava.STORAGE_STATE_RED:
				((MegaApplication) getApplication()).getMyAccountInfo().setUsedPerc(100);
				usedSpacePB.setProgressDrawable(getResources().getDrawable(
						R.drawable.custom_progress_bar_horizontal_exceed));
				break;
		}
	}

	public void selectSortByContacts(int _orderContacts){
		logDebug("selectSortByContacts");

		if (getOrderContacts() == _orderContacts) {
			return;
		}

		this.setOrderContacts(_orderContacts);
		cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CONTACTS.getTag());
		if (cFLol != null){
			cFLol.sortBy();
		}
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
				nodes = megaApi.getChildren(parentNode, orderCloud);
			} else {
				parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
				if (parentNode == null) return;

				nodes = megaApi.getChildren(parentNode, orderCloud);
			}
			logDebug("Nodes: " + nodes.size());
			fbFLol.hideMultipleSelect();
			fbFLol.setNodes(nodes);
			fbFLol.getRecyclerView().invalidate();
		}
	}

	private void refreshSharesPageAdapter() {
		if (sharesPageAdapter != null) {
			sharesPageAdapter.notifyDataSetChanged();
			setSharesTabIcons(getTabItemShares());
		}
	}

	public void refreshCloudOrder(int newOrderCloud){
		logDebug("New order: " + newOrderCloud);
		this.setOrderCloud(newOrderCloud);
		//Refresh Cloud Fragment
		refreshCloudDrive();

		//Refresh Rubbish Fragment
		refreshRubbishBin();

		onNodesSharedUpdate();

		if (getInboxFragment() != null){
			MegaNode inboxNode = megaApi.getInboxNode();
			if(inboxNode!=null){
				ArrayList<MegaNode> nodes = megaApi.getChildren(inboxNode, orderCloud);
				iFLol.setNodes(nodes);
				iFLol.getRecyclerView().invalidate();
			}
		}

		if (getOfflineFragment() != null){
			oFLol.setOrder(orderCloud);
		}
	}

	public void refreshOthersOrder(int newOrderOthers){
		if (getOrderOthers() == newOrderOthers) {
			return;
		}

		logDebug("New order: " + newOrderOthers);

		this.setOrderOthers(newOrderOthers);

		refreshSharesPageAdapter();
	}

	public void selectSortUploads(int orderCamera){
		logDebug("selectSortUploads");

        setOrderCamera(orderCamera);
		cuFL = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CAMERA_UPLOADS.getTag());
		if (cuFL != null){
			ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(cuFL.getPhotoSyncHandle()), orderCamera);
			cuFL.setNodes(nodes);
			cuFL.setOrderBy(orderCamera);
			cuFL.getRecyclerView().invalidate();
		}

		muFLol = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MEDIA_UPLOADS.getTag());
		if (muFLol != null){
			ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(muFLol.getPhotoSyncHandle()), orderCamera);
			muFLol.setNodes(nodes);
            muFLol.setOrderBy(orderCamera);
			muFLol.getRecyclerView().invalidate();
		}
	}

	public void showStatusDialog(String text){
		ProgressDialog temp = null;
		try{
			temp = new ProgressDialog(managerActivity);
			temp.setMessage(text);
			temp.show();
		}
		catch(Exception e){
			return;
		}
		statusDialog = temp;
	}

	public void dismissStatusDialog(){
		if (statusDialog != null){
			try{
				statusDialog.dismiss();
			}
			catch(Exception ex){}
		}
	}

	public void setFirstNavigationLevel(boolean firstNavigationLevel){
		logDebug("Set value to: " + firstNavigationLevel);
		this.firstNavigationLevel = firstNavigationLevel;
	}

	public boolean isFirstNavigationLevel() {
		return firstNavigationLevel;
	}

	public void setParentHandleBrowser(long parentHandleBrowser){
		logDebug("Set value to:" + parentHandleBrowser);

		this.parentHandleBrowser = parentHandleBrowser;
	}

	public void setParentHandleRubbish(long parentHandleRubbish){
		logDebug("setParentHandleRubbish");
		this.parentHandleRubbish = parentHandleRubbish;
	}

	public void setParentHandleSearch(long parentHandleSearch){
		logDebug("setParentHandleSearch");
		this.parentHandleSearch = parentHandleSearch;
	}

	public void setParentHandleIncoming(long parentHandleIncoming){
		logDebug("setParentHandleIncoming: " + parentHandleIncoming);
		this.parentHandleIncoming = parentHandleIncoming;
	}

	public void setParentHandleInbox(long parentHandleInbox){
		logDebug("setParentHandleInbox: " + parentHandleInbox);
		this.parentHandleInbox = parentHandleInbox;
	}

	public void setParentHandleOutgoing(long parentHandleOutgoing){
		logDebug("Outgoing parent handle: " + parentHandleOutgoing);
		this.parentHandleOutgoing = parentHandleOutgoing;
	}

	@Override
	protected void onNewIntent(Intent intent){
		logDebug("onNewIntent");

    	if(intent != null) {
			if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
				searchQuery = intent.getStringExtra(SearchManager.QUERY);
				parentHandleSearch = -1;
				setToolbarTitle();
				isSearching = true;

				if (searchMenuItem != null) {
					MenuItemCompat.collapseActionView(searchMenuItem);
				}
				return;
			}
//			When the user clicks on settings option in QR section, set drawerItem to SETTINGS and scroll to auto-accept setting
			else if (intent.getBooleanExtra("fromQR", false)){
				Bundle bundle = intent.getExtras();
				if (bundle.getSerializable("drawerItemQR") != null){
					if (DrawerItem.SETTINGS.equals(bundle.getSerializable("drawerItemQR"))){
						logDebug("From QR Settings");
						moveToSettingsSectionQR();
					}
				}
				return;
			}

		}
     	super.onNewIntent(intent);
    	setIntent(intent);
    	return;
	}

	public void navigateToUpgradeAccount(){
		logDebug("navigateToUpgradeAccount");
		setBottomNavigationMenuItemChecked(HIDDEN_BNV);

		getProLayout.setVisibility(View.GONE);
		drawerItemPreUpgradeAccount = drawerItem;
		drawerItem = DrawerItem.ACCOUNT;
		accountFragment = UPGRADE_ACCOUNT_FRAGMENT;
		displayedAccountType = -1;
		selectDrawerItemLollipop(drawerItem);
	}

	public void navigateToAchievements(){
		logDebug("navigateToAchievements");
		drawerItem = DrawerItem.ACCOUNT;
		setBottomNavigationMenuItemChecked(HIDDEN_BNV);

		getProLayout.setVisibility(View.GONE);
		drawerItem = DrawerItem.ACCOUNT;
		accountFragment = MY_ACCOUNT_FRAGMENT;
		displayedAccountType = -1;
		selectDrawerItemLollipop(drawerItem);

		Intent intent = new Intent(this, AchievementsActivity.class);
		startActivity(intent);
	}

	public void navigateToContacts(int index){
		drawerItem = DrawerItem.CONTACTS;
		indexContacts = index;
		selectDrawerItemLollipop(drawerItem);
	}

	public void navigateToMyAccount(){
		logDebug("navigateToMyAccount");
		drawerItem = DrawerItem.ACCOUNT;
		setBottomNavigationMenuItemChecked(HIDDEN_BNV);

		getProLayout.setVisibility(View.GONE);
		drawerItem = DrawerItem.ACCOUNT;
		accountFragment = MY_ACCOUNT_FRAGMENT;
		displayedAccountType = -1;
		comesFromNotifications = true;
		selectDrawerItemLollipop(drawerItem);
	}

	@Override
	public void onClick(View v) {
		logDebug("onClick");

		DrawerItem oldDrawerItem = drawerItem;

		switch(v.getId()){
			case R.id.navigation_drawer_add_phone_number_button:{
                Intent intent = new Intent(this,SMSVerificationActivity.class) ;
                startActivity(intent);
				break;
			}
			case R.id.btnLeft_cancel:{
				getProLayout.setVisibility(View.GONE);
				break;
			}
			case R.id.btnRight_upgrade:{
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
				intent.putExtra("newAccount", true);
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
				isFirstTimeCam();
				if (isOnline(this) && megaApi.getRootNode()!=null) {
					drawerItem = DrawerItem.ACCOUNT;
					accountFragment = MY_ACCOUNT_FRAGMENT;
					setBottomNavigationMenuItemChecked(HIDDEN_BNV);
					checkIfShouldCloseSearchView(oldDrawerItem, drawerItem);
					selectDrawerItemLollipop(drawerItem);
				}
				break;
			}
			case R.id.inbox_section: {
				isFirstTimeCam();
				drawerItem = DrawerItem.INBOX;
				checkIfShouldCloseSearchView(oldDrawerItem, drawerItem);
				selectDrawerItemLollipop(drawerItem);
				break;
			}
			case R.id.contacts_section: {
				isFirstTimeCam();
				drawerItem = DrawerItem.CONTACTS;
				checkIfShouldCloseSearchView(oldDrawerItem, drawerItem);
				selectDrawerItemLollipop(drawerItem);
				break;
			}
			case R.id.notifications_section: {
				isFirstTimeCam();
				drawerItem = DrawerItem.NOTIFICATIONS;
				checkIfShouldCloseSearchView(oldDrawerItem, drawerItem);
				selectDrawerItemLollipop(drawerItem);
				break;
			}
			case R.id.settings_section: {
				isFirstTimeCam();
				drawerItem = DrawerItem.SETTINGS;
				checkIfShouldCloseSearchView(oldDrawerItem, drawerItem);
				selectDrawerItemLollipop(drawerItem);
				break;
			}
			case R.id.upgrade_navigation_view: {
				isFirstTimeCam();
				drawerLayout.closeDrawer(Gravity.LEFT);
				drawerItemPreUpgradeAccount = drawerItem;
				drawerItem = DrawerItem.ACCOUNT;
				accountFragment = UPGRADE_ACCOUNT_FRAGMENT;
				displayedAccountType = -1;
				checkIfShouldCloseSearchView(oldDrawerItem, drawerItem);
				selectDrawerItemLollipop(drawerItem);
				break;
			}
			case R.id.lost_authentication_device: {
				try {
					String url = "https://mega.nz/recovery";
					Intent openTermsIntent = new Intent(this, WebViewActivityLollipop.class);
					openTermsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					openTermsIntent.setData(Uri.parse(url));
					startActivity(openTermsIntent);
				}
				catch (Exception e){
					Intent viewIntent = new Intent(Intent.ACTION_VIEW);
					viewIntent.setData(Uri.parse("https://mega.nz/recovery"));
					startActivity(viewIntent);
				}
				break;
			}
			case R.id.transfers_overview_item_layout: {
				logDebug("click transfers layout");
				drawerItem = DrawerItem.TRANSFERS;
				selectDrawerItemLollipop(drawerItem);
				break;
			}
			case R.id.transfers_overview_three_dots_layout: {
				logDebug("click show options");
				showTransfersPanel();
				break;
			}
			case R.id.transfers_overview_action_layout: {
				logDebug("click play/pause");
				changeTransfersStatus();
				break;
			}

			case R.id.call_in_progress_layout:{
				if(checkPermissionsCall()){
					returnCall(this);
				}
				break;
			}
		}
	}

	void exportRecoveryKey (){
		AccountController aC = new AccountController(this);
		aC.saveRkToFileSystem();
	}

	public void showConfirmationCloseAllSessions(){
		logDebug("showConfirmationCloseAllSessions");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						AccountController aC = new AccountController(managerActivity);
						aC.killAllSessions(managerActivity);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle(R.string.confirmation_close_sessions_title);

		builder.setMessage(R.string.confirmation_close_sessions_text).setPositiveButton(R.string.contact_accept, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();

	}

	public void showConfirmationRemoveFromOffline(){
		logDebug("showConfirmationRemoveFromOffline");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						REQUEST_WRITE_STORAGE);
			}
		}

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {
						MegaOffline mOff = getSelectedOfflineNode();

						NodeController nC = new NodeController(managerActivity);
						nC.deleteOffline(mOff);

                        if(isCloudAdded()){
                            String handle = mOff.getHandle();
                            if(handle != null && !handle.equals("")){
                                fbFLol.refresh(Long.parseLong(handle));
                            }
                        }

						onNodesSharedUpdate();

                        if (getSettingsFragment() != null) {
                        	sttFLol.taskGetSizeOffline();
                        }
						break;
					}
					case DialogInterface.BUTTON_NEGATIVE: {
						//No button clicked
						break;
					}
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setMessage(R.string.confirmation_delete_from_save_for_offline).setPositiveButton(R.string.general_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationRemoveSomeFromOffline(final List<MegaOffline> documents){
		logDebug("showConfirmationRemoveSomeFromOffline");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
			}
		}

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {

						String pathNavigation = getPathNavigationOffline();
						NodeController nC = new NodeController(managerActivity);
						for (int i=0;i<documents.size();i++){
							nC.deleteOffline(documents.get(i));
						}
						updateOfflineView(documents.get(0));
						if (getSettingsFragment() != null) {
							sttFLol.taskGetSizeOffline();
						}
						break;
					}
					case DialogInterface.BUTTON_NEGATIVE: {
						break;
					}
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setMessage(R.string.confirmation_delete_from_save_for_offline).setPositiveButton(R.string.general_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	@Override
	public void showConfirmationEnableLogsSDK(){
		if(getSettingsFragment() != null){
			sttFLol.numberOfClicksSDK = 0;
		}
		super.showConfirmationEnableLogsSDK();
	}

	@Override
	public void showConfirmationEnableLogsKarere(){
		if(getSettingsFragment() != null){
			sttFLol.numberOfClicksKarere = 0;
		}
		super.showConfirmationEnableLogsKarere();
	}

	public void showConfirmationDeleteAvatar(){
		logDebug("showConfirmationDeleteAvatar");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						AccountController aC = new AccountController(managerActivity);
						aC.removeAvatar();
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setMessage(R.string.confirmation_delete_avatar).setPositiveButton(R.string.context_delete, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void update2FASetting(){
		logDebug("update2FAVisibility");
		if (getSettingsFragment() != null) {
			try {
				sttFLol.update2FAVisibility();
			}catch (Exception e){}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		logDebug("Request code: " + requestCode + ", Result code:" + resultCode);

		if (resultCode == RESULT_FIRST_USER){
			showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_destination_folder), -1);
			return;
		}

        if (requestCode == REQUEST_CODE_GET && resultCode == RESULT_OK) {
			if (intent == null) {
				logWarning("Intent NULL");
				return;
			}

			logDebug("Intent action: " + intent.getAction());
			logDebug("Intent type: " + intent.getType());

			intent.setAction(Intent.ACTION_GET_CONTENT);
			FilePrepareTask filePrepareTask = new FilePrepareTask(this);
			filePrepareTask.execute(intent);
			showProcessFileDialog(this,intent);
		}
		else if (requestCode == CHOOSE_PICTURE_PROFILE_CODE && resultCode == RESULT_OK) {

			if (resultCode == RESULT_OK) {
				if (intent == null) {
					logWarning("Intent NULL");
					return;
				}

				boolean isImageAvailable = checkProfileImageExistence(intent.getData());
				if(!isImageAvailable){
					logError("Error when changing avatar: image not exist");
					showSnackbar(SNACKBAR_TYPE, getString(R.string.error_changing_user_avatar_image_not_available), -1);
					return;
				}

				intent.setAction(Intent.ACTION_GET_CONTENT);
				FilePrepareTask filePrepareTask = new FilePrepareTask(this);
				filePrepareTask.execute(intent);
				ProgressDialog temp = null;
				try{
					temp = new ProgressDialog(this);
					temp.setMessage(getString(R.string.upload_prepare));
					temp.show();
				}
				catch(Exception e){
					return;
				}
				statusDialog = temp;

			}
			else {
				logWarning("resultCode for CHOOSE_PICTURE_PROFILE_CODE: " + resultCode);
			}
		}
		else if (requestCode == REQUEST_CODE_SELECT_CHAT && resultCode == RESULT_OK){
			logDebug("Attach nodes to chats: REQUEST_CODE_SELECT_CHAT");

			new ChatController(this).checkIntentToShareSomething(intent);
		}
		else if (requestCode == WRITE_SD_CARD_REQUEST_CODE && resultCode == RESULT_OK) {

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
					ActivityCompat.requestPermissions(this,
			                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
							REQUEST_WRITE_STORAGE);
				}
			}

			Uri treeUri = intent.getData();
			logDebug("Create the document : " + treeUri);
			long handleToDownload = intent.getLongExtra("handleToDownload", -1);
			logDebug("The recovered handle is: " + handleToDownload);
			//Now, call to the DownloadService

			if(handleToDownload!=0 && handleToDownload!=-1){
				Intent service = new Intent(this, DownloadService.class);
				service.putExtra(DownloadService.EXTRA_HASH, handleToDownload);
				service.putExtra(DownloadService.EXTRA_CONTENT_URI, treeUri.toString());
				File tempFolder = getCacheFolder(this, TEMPORAL_FOLDER);
				if (!isFileAvailable(tempFolder)) {
				    showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error), -1);
				    return;
                }
				service.putExtra(DownloadService.EXTRA_PATH, tempFolder.getAbsolutePath());
				startService(service);
			}
        } else if (requestCode == REQUEST_CODE_TREE) {
            onRequestSDCardWritePermission(intent, resultCode, false, nC);
        }
		else if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {
			logDebug("requestCode == REQUEST_CODE_SELECT_FILE");

			if (intent == null) {
				logWarning("Intent NULL");
				return;
			}

			cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CONTACTS.getTag());
			if(cFLol!=null && cFLol.isMultipleselect()){
				cFLol.hideMultipleSelect();
				cFLol.clearSelectionsNoAnimations();
			}

			ArrayList<String> selectedContacts = intent.getStringArrayListExtra(SELECTED_CONTACTS);
			long fileHandles[] = intent.getLongArrayExtra(NODE_HANDLES);

			//Send file to contacts
			//Check if all contacts have a chat created

			ArrayList<MegaChatRoom> chats = null;
			ArrayList<MegaUser> usersNoChat = null;

			for(int i=0; i<selectedContacts.size(); i++){

				MegaUser contact = megaApi.getContact(selectedContacts.get(i));

				MegaChatRoom chatRoom = megaChatApi.getChatRoomByUser(contact.getHandle());
				if(chatRoom!=null){
					if(chats==null){
						chats = new ArrayList<MegaChatRoom>();
					}
					chats.add(chatRoom);

				}
				else{
					if(usersNoChat==null){
						usersNoChat = new ArrayList<MegaUser>();
					}
					usersNoChat.add(contact);
				}
			}

			if(usersNoChat==null || usersNoChat.isEmpty()){
				new ChatController(this).checkIfNodesAreMineAndAttachNodes(fileHandles, getChatHandles(chats, null));
			}
			else{
				//Create first the chats
				CreateChatListener listener = new CreateChatListener(chats, usersNoChat, fileHandles, this, CreateChatListener.SEND_FILES, -1);

				for(int i=0; i<usersNoChat.size(); i++){
					MegaChatPeerList peers = MegaChatPeerList.createInstance();
					peers.addPeer(usersNoChat.get(i).getHandle(), MegaChatPeerList.PRIV_STANDARD);
					megaChatApi.createChat(false, peers, listener);
				}
			}
		}
		else if(requestCode == ACTION_SEARCH_BY_DATE && resultCode == RESULT_OK){
			if (intent == null) {
				logWarning("Intent NULL");
				return;
			}
			searchDate = intent.getLongArrayExtra("SELECTED_DATE");
			cuFL = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CAMERA_UPLOADS.getTag());
			if (cuFL != null){
				long cameraUploadHandle = cuFL.getPhotoSyncHandle();
				MegaNode nps = megaApi.getNodeByHandle(cameraUploadHandle);
				if (nps != null){
					ArrayList<MegaNode> nodes = megaApi.getChildren(nps, orderCamera);
					if((searchByDate) != null && (searchDate!=null)){
						ArrayList<MegaNode> nodesSearch = cuFL.searchDate(searchDate, nodes);
						cuFL.setNodes(nodesSearch);
						if (nodesSearch.size() == 0) {
							cuFL.showEmptySearchResults();
						}
						isSearchEnabled = true;
					}else{
						cuFL.setNodes(nodes);

					}

				}
			}

			muFLol = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MEDIA_UPLOADS.getTag());
			if (muFLol != null){
				long cameraUploadHandle = muFLol.getPhotoSyncHandle();
				MegaNode nps = megaApi.getNodeByHandle(cameraUploadHandle);
				if (nps != null){
					ArrayList<MegaNode> nodes = megaApi.getChildren(nps, orderCamera);
					if((searchByDate) != null && (searchDate!=null)){
						ArrayList<MegaNode> nodesSearch = muFLol.searchDate(searchDate, nodes);
						muFLol.setNodes(nodesSearch);
						isSearchEnabled = true;
					}else{
						muFLol.setNodes(nodes);

					}
				}
			}

		}
		else if (requestCode == REQUEST_CODE_SELECT_FOLDER && resultCode == RESULT_OK) {
			logDebug("REQUEST_CODE_SELECT_FOLDER");

			if (intent == null) {
				logDebug("Intent NULL");
				return;
			}

			final ArrayList<String> selectedContacts = intent.getStringArrayListExtra(SELECTED_CONTACTS);
			final long folderHandle = intent.getLongExtra("SELECT", 0);

			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
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

		}
		else if (requestCode == REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK){
			logDebug("onActivityResult REQUEST_CODE_SELECT_CONTACT OK");

			if (intent == null) {
				logWarning("Intent NULL");
				return;
			}

			final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);
			megaContacts = intent.getBooleanExtra(AddContactActivityLollipop.EXTRA_MEGA_CONTACTS, true);

			final int multiselectIntent = intent.getIntExtra("MULTISELECT", -1);

			//if (megaContacts){

			if(multiselectIntent==0){
				//One file to share
				final long nodeHandle = intent.getLongExtra(AddContactActivityLollipop.EXTRA_NODE_HANDLE, -1);

				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
				dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
				final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
                dialogBuilder.setSingleChoiceItems(items, -1, (dialog, item) -> {
                    permissionsDialog.dismiss();
                    nC.shareFolder(megaApi.getNodeByHandle(nodeHandle), contactsData, item);
                });
				dialogBuilder.setTitle(getString(R.string.dialog_select_permissions));
				permissionsDialog = dialogBuilder.create();
				permissionsDialog.show();
			}
			else if(multiselectIntent==1){
				//Several folders to share
				final long[] nodeHandles = intent.getLongArrayExtra(AddContactActivityLollipop.EXTRA_NODE_HANDLE);

				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
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
		}
		else if (requestCode == REQUEST_CODE_GET_LOCAL && resultCode == RESULT_OK) {

			if (intent == null) {
				logDebug("Intent NULL");
				return;
			}

			String folderPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
			ArrayList<String> paths = intent.getStringArrayListExtra(FileStorageActivityLollipop.EXTRA_FILES);

			UploadServiceTask uploadServiceTask = new UploadServiceTask(folderPath, paths, getCurrentParentHandle());
			uploadServiceTask.start();
		}
		else if (requestCode == REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK) {

			if (intent == null) {
				logDebug("Intent NULL");
				return;
			}

			moveToRubbish = false;

			final long[] moveHandles = intent.getLongArrayExtra("MOVE_HANDLES");
			final long toHandle = intent.getLongExtra("MOVE_TO", 0);

			nC.moveNodes(moveHandles, toHandle);

		}
		else if (requestCode ==  REQUEST_CODE_SELECT_COPY_FOLDER && resultCode == RESULT_OK){
			logDebug("REQUEST_CODE_SELECT_COPY_FOLDER");

			if (intent == null) {
				logWarning("Intent NULL");
				return;
			}
			final long[] copyHandles = intent.getLongArrayExtra("COPY_HANDLES");
			final long toHandle = intent.getLongExtra("COPY_TO", 0);

			nC.copyNodes(copyHandles, toHandle);
		}
		else if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
			logDebug("REQUEST_CODE_SELECT_LOCAL_FOLDER");

			if (intent == null) {
				logWarning("Intent NULL");
				return;
			}

			String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
			String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
			logDebug("Size: " + size);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
			logDebug("Hashes size: " + hashes.length);

			boolean highPriority = intent.getBooleanExtra(HIGH_PRIORITY_TRANSFER, false);

			nC.checkSizeBeforeDownload(parentPath,url, size, hashes, highPriority);
		}
		else if (requestCode == REQUEST_CODE_REFRESH && resultCode == RESULT_OK) {
			logDebug("Resfresh DONE");

			if (intent == null) {
				logWarning("Intent NULL");
				return;
			}

			((MegaApplication) getApplication()).askForFullAccountInfo();
			((MegaApplication) getApplication()).askForExtendedAccountDetails();

			if (drawerItem == DrawerItem.CLOUD_DRIVE){
				parentHandleBrowser = intent.getLongExtra("PARENT_HANDLE", -1);
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
				if (parentNode != null){
					if (isCloudAdded()){
						ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderCloud);
						fbFLol.setNodes(nodes);
						fbFLol.getRecyclerView().invalidate();
					}
				}
				else{
					if (isCloudAdded()){
						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRootNode(), orderCloud);
						fbFLol.setNodes(nodes);
						fbFLol.getRecyclerView().invalidate();
					}
				}
			}
			else if (drawerItem == DrawerItem.SHARED_ITEMS){
				refreshIncomingShares();
			}
		}
		else if (requestCode == REQUEST_CODE_REFRESH_STAGING && resultCode == RESULT_OK) {
			logDebug("Resfresh DONE");

			if (intent == null) {
				logWarning("Intent NULL");
				return;
			}

			((MegaApplication) getApplication()).askForFullAccountInfo();
			((MegaApplication) getApplication()).askForExtendedAccountDetails();

			if (drawerItem == DrawerItem.CLOUD_DRIVE){
				parentHandleBrowser = intent.getLongExtra("PARENT_HANDLE", -1);
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
				if (parentNode != null){
					if (isCloudAdded()){
						ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderCloud);
						fbFLol.setNodes(nodes);
						fbFLol.getRecyclerView().invalidate();
					}
				}
				else{
					if (isCloudAdded()){
						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRootNode(), orderCloud);
						fbFLol.setNodes(nodes);
						fbFLol.getRecyclerView().invalidate();
					}
				}
			}
			else if (drawerItem == DrawerItem.SHARED_ITEMS){
				refreshIncomingShares();
			}

			if (getSettingsFragment() != null) {
				try {
					sttFLol.update2FAVisibility();
				}catch (Exception e){}
			}
		}
		else if (requestCode == TAKE_PHOTO_CODE) {
			logDebug("TAKE_PHOTO_CODE");
            if (resultCode == Activity.RESULT_OK) {
                uploadTakePicture(this, getCurrentParentHandle(), megaApi);
            } else {
                logWarning("TAKE_PHOTO_CODE--->ERROR!");
            }
		}
		else if (requestCode == TAKE_PICTURE_PROFILE_CODE){
			logDebug("TAKE_PICTURE_PROFILE_CODE");

			if(resultCode == Activity.RESULT_OK){
				String myEmail =  megaApi.getMyUser().getEmail();
				File imgFile = getCacheFile(this, TEMPORAL_FOLDER, "picture.jpg");
				if (!isFileAvailable(imgFile)) {
					showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error), -1);
					return;
				}

				File qrFile = buildQrFile(this, myEmail + QR_IMAGE_FILE_NAME);
                File newFile = buildAvatarFile(this,myEmail + "Temp.jpg");
				if (isFileAvailable(qrFile)) {
					qrFile.delete();
				}

                if (newFile != null) {
                    MegaUtilsAndroid.createAvatar(imgFile,newFile);
                    megaApi.setAvatar(newFile.getAbsolutePath(),this);
                } else {
					logError("ERROR! Destination PATH is NULL");
                }
			}else{
				logError("TAKE_PICTURE_PROFILE_CODE--->ERROR!");
			}

		}
		else if (requestCode == REQUEST_CODE_SORT_BY && resultCode == RESULT_OK){

			if (intent == null) {
				logWarning("Intent NULL");
				return;
			}

			int orderGetChildren = intent.getIntExtra("ORDER_GET_CHILDREN", 1);
			if (drawerItem == DrawerItem.CLOUD_DRIVE){
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
				if (parentNode != null){
					if (isCloudAdded()){
						ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderGetChildren);
						fbFLol.setNodes(nodes);
						fbFLol.getRecyclerView().invalidate();
					}
				}
				else{
					if (isCloudAdded()){
						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRootNode(), orderGetChildren);
						fbFLol.setNodes(nodes);
						fbFLol.getRecyclerView().invalidate();
					}
				}
			}
			else if (drawerItem == DrawerItem.SHARED_ITEMS){
				onNodesSharedUpdate();
			}
		}
		else if (requestCode == REQUEST_CREATE_CHAT && resultCode == RESULT_OK) {
			logDebug("REQUEST_CREATE_CHAT OK");

			if (intent == null) {
				logWarning("Intent NULL");
				return;
			}

			final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);

			final boolean isGroup = intent.getBooleanExtra(AddContactActivityLollipop.EXTRA_GROUP_CHAT, false);

			if (contactsData != null){
				if(!isGroup){
					logDebug("Create one to one chat");
					MegaUser user = megaApi.getContact(contactsData.get(0));
					if(user!=null){
						logDebug("Chat with contact: " + contactsData.size());
						startOneToOneChat(user);
					}
				}
				else{
					logDebug("Create GROUP chat");
					MegaChatPeerList peers = MegaChatPeerList.createInstance();
					for (int i=0; i<contactsData.size(); i++){
						MegaUser user = megaApi.getContact(contactsData.get(i));
						if(user!=null){
							peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
						}
					}
					final String chatTitle = intent.getStringExtra(AddContactActivityLollipop.EXTRA_CHAT_TITLE);
					boolean isEKR = intent.getBooleanExtra(AddContactActivityLollipop.EXTRA_EKR, false);
					boolean chatLink = false;
					if (!isEKR) {
						chatLink = intent.getBooleanExtra(AddContactActivityLollipop.EXTRA_CHAT_LINK, false);
					}

					createGroupChat(peers, chatTitle, chatLink, isEKR);
				}
			}
		}
		else if (requestCode == REQUEST_INVITE_CONTACT_FROM_DEVICE && resultCode == RESULT_OK) {
			logDebug("REQUEST_INVITE_CONTACT_FROM_DEVICE OK");

			if (intent == null) {
				logWarning("Intent NULL");
				return;
			}

			final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);
			megaContacts = intent.getBooleanExtra(AddContactActivityLollipop.EXTRA_MEGA_CONTACTS, true);

			if (contactsData != null){
				cC.inviteMultipleContacts(contactsData);
			}
		}
		else if (requestCode == REQUEST_DOWNLOAD_FOLDER && resultCode == RESULT_OK){
			String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
			if (parentPath != null){
				String path = parentPath + File.separator + getRecoveryKeyFileName();

				logDebug("REQUEST_DOWNLOAD_FOLDER:path to download: "+path);
				AccountController ac = new AccountController(this);
				ac.exportMK(path);
			}
		}

		else if(requestCode == REQUEST_CODE_FILE_INFO && resultCode == RESULT_OK){
		    if(isCloudAdded()){
                long handle = intent.getLongExtra(NODE_HANDLE, -1);
                fbFLol.refresh(handle);
            }

			onNodesSharedUpdate();
        }
		else{
			logWarning("No requestcode");
			super.onActivityResult(requestCode, resultCode, intent);
		}
	}

	public void createGroupChat(MegaChatPeerList peers, String chatTitle, boolean chatLink, boolean isEKR){

		logDebug("Create group chat with participants: " + peers.size());

		if (isEKR) {
			megaChatApi.createChat(true, peers, chatTitle, this);
		}
		else {
			if(chatLink){
				if(chatTitle!=null && !chatTitle.isEmpty()){
					CreateGroupChatWithPublicLink listener = new CreateGroupChatWithPublicLink(this, chatTitle);
					megaChatApi.createPublicChat(peers, chatTitle, listener);
				}
				else{
					showAlert(this, getString(R.string.message_error_set_title_get_link), null);
				}
			}
			else{
				megaChatApi.createPublicChat(peers, chatTitle, this);
			}
		}
	}

	private long[] getChatHandles(ArrayList<MegaChatRoom> chats, long[] _chatHandles) {
		if (_chatHandles != null && chats == null) {
			return _chatHandles;
		}

		long[] chatHandles = new long[chats.size()];

		for (int i = 0; i < chats.size(); i++) {
			chatHandles[i] = chats.get(i).getChatId();
		}

		return chatHandles;
	}

	public void sendFilesToChats(ArrayList<MegaChatRoom> chats, long[] _chatHandles, long[] nodeHandles) {
		long[] chatHandles = getChatHandles(chats, _chatHandles);

		int countChat = chatHandles.length;
		int counter = chatHandles.length * nodeHandles.length;
		long chatId = -1;

		if (countChat == 1) {
			chatId = chatHandles[0];
		}

		MultipleAttachChatListener listener = new MultipleAttachChatListener(this, chatId, counter);

		for (int i = 0; i < chatHandles.length; i++) {
			for (int j = 0; j < nodeHandles.length; j++) {
				megaChatApi.attachNode(chatHandles[i], nodeHandles[j], listener);
			}
		}
	}

	public void startOneToOneChat(MegaUser user){
		logDebug("User Handle: " + user.getHandle());
		MegaChatRoom chat = megaChatApi.getChatRoomByUser(user.getHandle());
		MegaChatPeerList peers = MegaChatPeerList.createInstance();
		if(chat==null){
			logDebug("No chat, create it!");
			peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
			megaChatApi.createChat(false, peers, this);
		}
		else{
			logDebug("There is already a chat, open it!");
			Intent intentOpenChat = new Intent(this, ChatActivityLollipop.class);
			intentOpenChat.setAction(ACTION_CHAT_SHOW_MESSAGES);
			intentOpenChat.putExtra("CHAT_ID", chat.getChatId());
			this.startActivity(intentOpenChat);
		}
	}

	public void startGroupConversation(ArrayList<Long> userHandles){
		logDebug("startGroupConversation");
		MegaChatPeerList peers = MegaChatPeerList.createInstance();

		for(int i=0;i<userHandles.size();i++){
			long handle = userHandles.get(i);
			peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD);
		}

		megaChatApi.createChat(false, peers, this);
	}


	/*
	 * Background task to get files on a folder for uploading
	 */
	private class UploadServiceTask extends Thread {

		String folderPath;
		ArrayList<String> paths;
		long parentHandle;

		UploadServiceTask(String folderPath, ArrayList<String> paths, long parentHandle){
			this.folderPath = folderPath;
			this.paths = paths;
			this.parentHandle = parentHandle;
		}

		@Override
		public void run(){

			logDebug("Run Upload Service Task");

			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if (parentNode == null){
				parentNode = megaApi.getRootNode();
			}

			showSnackbar(SNACKBAR_TYPE, getResources().getQuantityString(R.plurals.upload_began, paths.size(), paths.size()), -1);
			for (String path : paths) {
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				Intent uploadServiceIntent;
				if(managerActivity != null)
				{
					uploadServiceIntent = new Intent (managerActivity, UploadService.class);
				}
				else
				{
					uploadServiceIntent = new Intent (ManagerActivityLollipop.this, UploadService.class);
				}

				File file = new File (path);
				if (file.isDirectory()){
					uploadServiceIntent.putExtra(UploadService.EXTRA_FILEPATH, file.getAbsolutePath());
					uploadServiceIntent.putExtra(UploadService.EXTRA_NAME, file.getName());
				}
				else{
					ShareInfo info = ShareInfo.infoFromFile(file);
					if (info == null){
						continue;
					}
					uploadServiceIntent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
					uploadServiceIntent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
					uploadServiceIntent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
				}

				uploadServiceIntent.putExtra(UploadService.EXTRA_FOLDERPATH, folderPath);
				uploadServiceIntent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
				uploadServiceIntent.putExtra(UploadService.EXTRA_UPLOAD_COUNT, paths.size());
				startService(uploadServiceIntent);
			}
		}
	}

	void disableNavigationViewMenu(Menu menu){
		logDebug("disableNavigationViewMenu");

		MenuItem mi = menu.findItem(R.id.bottom_navigation_item_cloud_drive);
		if (mi != null){
			mi.setChecked(false);
			mi.setEnabled(false);
		}
		mi = menu.findItem(R.id.bottom_navigation_item_camera_uploads);
		if (mi != null){
			mi.setChecked(false);
			mi.setEnabled(false);
		}
		mi = menu.findItem(R.id.bottom_navigation_item_chat);
		if (mi != null){
			mi.setChecked(false);
		}
		mi = menu.findItem(R.id.bottom_navigation_item_shared_items);
		if (mi != null){
			mi.setChecked(false);
			mi.setEnabled(false);
		}
		mi = menu.findItem(R.id.bottom_navigation_item_offline);
		if (mi != null){
			mi.setChecked(false);
		}

		disableNavigationViewLayout();
	}

	void disableNavigationViewLayout() {
		if (myAccountSection != null) {
			myAccountSection.setEnabled(false);
			((TextView) myAccountSection.findViewById(R.id.my_account_section_text)).setTextColor(ContextCompat.getColor(this, R.color.black_15_opacity));
		}

		if (inboxSection != null){
			if(inboxNode==null){
				inboxSection.setVisibility(View.GONE);
			}
			else{
				boolean hasChildren = megaApi.hasChildren(inboxNode);
				if(hasChildren){
					inboxSection.setEnabled(false);
					inboxSection.setVisibility(View.VISIBLE);
					((TextView) inboxSection.findViewById(R.id.inbox_section_text)).setTextColor(ContextCompat.getColor(this, R.color.black_15_opacity));
				}
				else{
					inboxSection.setVisibility(View.GONE);
				}
			}
		}

		if (contactsSection != null) {
			contactsSection.setEnabled(false);
			if (contactsSectionText == null) {
				contactsSectionText = (TextView) contactsSection.findViewById(R.id.contacts_section_text);
			}
			String contactsText = contactsSectionText.getText().toString();
			if (!contactsText.equals(getString(R.string.section_contacts))){
				int start = contactsText.indexOf('(');
				start++;
				int end = contactsText.indexOf(')');
				if (start>0 && end>0) {
					int pendingRequest = Integer.parseInt(contactsText.substring(start, end));
					setFormattedContactTitleSection(pendingRequest, false);
				}
			}

			contactsSectionText.setTextColor(ContextCompat.getColor(this, R.color.black_15_opacity));
		}

		if (notificationsSection != null) {
			notificationsSection.setEnabled(false);
			if (notificationsSectionText == null) {
				notificationsSectionText = (TextView) notificationsSection.findViewById(R.id.contacts_section_text);
			}

			int unread = megaApi.getNumUnreadUserAlerts();

			if(unread == 0){
				notificationsSectionText.setText(getString(R.string.title_properties_chat_contact_notifications));
			}
			else{
				setFormattedNotificationsTitleSection(unread, false);
			}

			notificationsSectionText.setTextColor(ContextCompat.getColor(this, R.color.black_15_opacity));
		}

		if (upgradeAccount != null) {
			upgradeAccount.setEnabled(false);
			upgradeAccount.setBackground(ContextCompat.getDrawable(this, R.drawable.background_button_disable));
			upgradeAccount.setTextColor(ContextCompat.getColor(this, R.color.accent_color_30_opacity));
		}
	}

	void resetNavigationViewMenu(Menu menu){
		logDebug("resetNavigationViewMenu()");

		if(!isOnline(this) || megaApi==null || megaApi.getRootNode()==null){
			disableNavigationViewMenu(menu);
			return;
		}

		MenuItem mi = menu.findItem(R.id.bottom_navigation_item_cloud_drive);

		if (mi != null){
			mi.setChecked(false);
			mi.setEnabled(true);
		}
		mi = menu.findItem(R.id.bottom_navigation_item_camera_uploads);
		if (mi != null){
			mi.setChecked(false);
			mi.setEnabled(true);
		}
		mi = menu.findItem(R.id.bottom_navigation_item_chat);
		if (mi != null){
			mi.setChecked(false);
			mi.setEnabled(true);
		}
		mi = menu.findItem(R.id.bottom_navigation_item_shared_items);
		if (mi != null){
			mi.setChecked(false);
			mi.setEnabled(true);
		}
		mi = menu.findItem(R.id.bottom_navigation_item_offline);
		if (mi != null){
			mi.setChecked(false);
			mi.setEnabled(true);
		}

		resetNavigationViewLayout();
	}

	public void resetNavigationViewLayout() {
		if (myAccountSection != null) {
			myAccountSection.setEnabled(true);
			((TextView) myAccountSection.findViewById(R.id.my_account_section_text)).setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
		}

		if (inboxSection != null){
			if(inboxNode==null){
				inboxSection.setVisibility(View.GONE);
				logDebug("Inbox Node is NULL");
			}
			else{
				boolean hasChildren = megaApi.hasChildren(inboxNode);
				if(hasChildren){
					inboxSection.setEnabled(true);
					inboxSection.setVisibility(View.VISIBLE);
					((TextView) inboxSection.findViewById(R.id.inbox_section_text)).setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
				}
				else{
					logDebug("Inbox Node NO children");
					inboxSection.setVisibility(View.GONE);
				}
			}
		}

		if (contactsSection != null) {
			contactsSection.setEnabled(true);
			if (contactsSectionText == null) {
				contactsSectionText = (TextView) contactsSection.findViewById(R.id.contacts_section_text);
			}
			contactsSectionText.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
			setContactTitleSection();
		}

		if (notificationsSection != null) {
			notificationsSection.setEnabled(true);
			if (notificationsSectionText == null) {
				notificationsSectionText = (TextView) notificationsSection.findViewById(R.id.notification_section_text);
			}
			notificationsSectionText.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
			setNotificationsTitleSection();
		}

		if (upgradeAccount != null) {
			upgradeAccount.setEnabled(true);
			upgradeAccount.setBackground(ContextCompat.getDrawable(this, R.drawable.background_button_white));
			upgradeAccount.setTextColor(ContextCompat.getColor(this, R.color.accentColor));
		}
	}

	public void setInboxNavigationDrawer() {
		logDebug("setInboxNavigationDrawer");
		if (nV != null && inboxSection != null){
			if(inboxNode==null){
				inboxSection.setVisibility(View.GONE);
				logDebug("Inbox Node is NULL");
			}
			else{
				boolean hasChildren = megaApi.hasChildren(inboxNode);
				if(hasChildren){
					inboxSection.setEnabled(true);
					inboxSection.setVisibility(View.VISIBLE);
				}
				else{
					logDebug("Inbox Node NO children");
					inboxSection.setVisibility(View.GONE);
				}
			}
		}
	}

	public void showProPanel(){
		logDebug("showProPanel");
		//Left and Right margin
		LinearLayout.LayoutParams proTextParams = (LinearLayout.LayoutParams)getProText.getLayoutParams();
		proTextParams.setMargins(scaleWidthPx(24, outMetrics), scaleHeightPx(23, outMetrics), scaleWidthPx(24, outMetrics), scaleHeightPx(23, outMetrics));
		getProText.setLayoutParams(proTextParams);

		rightUpgradeButton.setOnClickListener(this);
		android.view.ViewGroup.LayoutParams paramsb2 = rightUpgradeButton.getLayoutParams();
		//Left and Right margin
		LinearLayout.LayoutParams optionTextParams = (LinearLayout.LayoutParams)rightUpgradeButton.getLayoutParams();
		optionTextParams.setMargins(scaleWidthPx(6, outMetrics), 0, scaleWidthPx(8, outMetrics), 0);
		rightUpgradeButton.setLayoutParams(optionTextParams);

		leftCancelButton.setOnClickListener(this);
		android.view.ViewGroup.LayoutParams paramsb1 = leftCancelButton.getLayoutParams();
		leftCancelButton.setLayoutParams(paramsb1);
		//Left and Right margin
		LinearLayout.LayoutParams cancelTextParams = (LinearLayout.LayoutParams)leftCancelButton.getLayoutParams();
		cancelTextParams.setMargins(scaleWidthPx(6, outMetrics), 0, scaleWidthPx(6, outMetrics), 0);
		leftCancelButton.setLayoutParams(cancelTextParams);

		getProLayout.setVisibility(View.VISIBLE);
		getProLayout.bringToFront();
	}

	public void showTransferOverquotaDialog(){
		logDebug("showTransferOverquotaDialog");

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

		LayoutInflater inflater = this.getLayoutInflater();
		View dialogView = inflater.inflate(R.layout.transfer_overquota_layout, null);
		dialogBuilder.setView(dialogView);

		TextView title = (TextView) dialogView.findViewById(R.id.transfer_overquota_title);
		title.setText(getString(R.string.title_depleted_transfer_overquota));

		ImageView icon = (ImageView) dialogView.findViewById(R.id.image_transfer_overquota);
		icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.transfer_quota_empty));

		TextView text = (TextView) dialogView.findViewById(R.id.text_transfer_overquota);
		text.setText(getString(R.string.text_depleted_transfer_overquota));

		Button continueButton = (Button) dialogView.findViewById(R.id.transfer_overquota_button_dissmiss);

		Button paymentButton = (Button) dialogView.findViewById(R.id.transfer_overquota_button_payment);
		if(((MegaApplication) getApplication()).getMyAccountInfo().getAccountType()>MegaAccountDetails.ACCOUNT_TYPE_FREE){
			logDebug("USER PRO");
			paymentButton.setText(getString(R.string.action_upgrade_account));
		}
		else{
			logDebug("FREE USER");
			paymentButton.setText(getString(R.string.plans_depleted_transfer_overquota));
		}

		alertDialogTransferOverquota = dialogBuilder.create();

		continueButton.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				alertDialogTransferOverquota.dismiss();
			}

		});

		paymentButton.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				alertDialogTransferOverquota.dismiss();
				navigateToUpgradeAccount();
			}

		});

		alertDialogTransferOverquota.setCancelable(false);
		alertDialogTransferOverquota.setCanceledOnTouchOutside(false);
		alertDialogTransferOverquota.show();
	}

	/**
	 * Check the current storage state.
	 * @param onCreate Flag to indicate if the method was called from "onCreate" or not.
	 */
	private void checkCurrentStorageStatus(boolean onCreate) {
		// If the current storage state is not initialized is because the app received the
		// event informing about the storage state  during login, the ManagerActivityLollipop
		// wasn't active and for this reason the value is stored in the MegaApplication object.
		int storageStateToCheck = (storageState != MegaApiJava.STORAGE_STATE_UNKNOWN) ?
				storageState : app.getStorageState();

		checkStorageStatus(storageStateToCheck, onCreate);
	}

	/**
	 * Check the storage state provided as first parameter.
	 * @param newStorageState Storage state to check.
	 * @param onCreate Flag to indicate if the method was called from "onCreate" or not.
	 */
	private void checkStorageStatus(int newStorageState, boolean onCreate) {
        Intent intent = new Intent(this,UploadService.class);
        switch (newStorageState) {
            case MegaApiJava.STORAGE_STATE_GREEN:
				logDebug("STORAGE STATE GREEN");

                intent.setAction(ACTION_STORAGE_STATE_CHANGED);

                // TODO: WORKAROUND, NEED TO IMPROVE AND REMOVE THE TRY-CATCH
                try {
					startService(intent);
				}
				catch (Exception e) {
					logError("Exception starting UploadService", e);
					e.printStackTrace();
				}

				int accountType = app.getMyAccountInfo().getAccountType();
				if(accountType == MegaAccountDetails.ACCOUNT_TYPE_FREE){
					logDebug("ACCOUNT TYPE FREE");
					if(showMessageRandom()){
						logDebug("Show message random: TRUE");
						showProPanel();
					}
				}
				storageState = newStorageState;
                startCameraUploadService(ManagerActivityLollipop.this);
				break;

			case MegaApiJava.STORAGE_STATE_ORANGE:
				logWarning("STORAGE STATE ORANGE");

                intent.setAction(ACTION_STORAGE_STATE_CHANGED);

				// TODO: WORKAROUND, NEED TO IMPROVE AND REMOVE THE TRY-CATCH
                try {
					startService(intent);
				}
				catch (Exception e) {
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
                startCameraUploadService(ManagerActivityLollipop.this);
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

			default:
				return;
		}

		app.setStorageState(storageState);
		storageState = newStorageState;
	}

	/**
	 * Show a dialog to indicate that the storage space is almost full.
	 */
	public void showStorageAlmostFullDialog(){
		logDebug("showStorageAlmostFullDialog");
		showStorageStatusDialog(MegaApiJava.STORAGE_STATE_ORANGE, false, false);
	}

	/**
	 * Show a dialog to indicate that the storage space is full.
	 */
	public void showStorageFullDialog(){
		logDebug("showStorageFullDialog");
		showStorageStatusDialog(MegaApiJava.STORAGE_STATE_RED, false, false);
	}

	/**
	 * Show an overquota alert dialog.
	 * @param preWarning Flag to indicate if is a pre-overquota alert or not.
	 */
	public void showOverquotaAlert(boolean preWarning){
		logDebug("preWarning: " + preWarning);
		showStorageStatusDialog(
				preWarning ? MegaApiJava.STORAGE_STATE_ORANGE : MegaApiJava.STORAGE_STATE_RED,
				true, preWarning);
	}

	public void showSMSVerificationDialog() {
	    isSMSDialogShowing = true;
        smsDialogTimeChecker.update();
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.sms_verification_dialog_layout,null);
        dialogBuilder.setView(dialogView);

        TextView msg = dialogView.findViewById(R.id.sv_dialog_msg);
        boolean isAchievementUser = megaApi.isAchievementsEnabled();
        logDebug("is achievement user: " + isAchievementUser);
        if (isAchievementUser) {
            String message = String.format(getString(R.string.sms_add_phone_number_dialog_msg_achievement_user), bonusStorageSMS);
            msg.setText(message);
        } else {
            msg.setText(R.string.sms_add_phone_number_dialog_msg_non_achievement_user);
        }

        dialogView.findViewById(R.id.sv_btn_horizontal_not_now).setOnClickListener(v -> alertDialogSMSVerification.dismiss());
        dialogView.findViewById(R.id.sv_btn_horizontal_add).setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(),SMSVerificationActivity.class));
            alertDialogSMSVerification.dismiss();
        });
        if(alertDialogSMSVerification == null) {
            alertDialogSMSVerification = dialogBuilder.create();
            alertDialogSMSVerification.setCancelable(false);
            alertDialogSMSVerification.setOnDismissListener(dialog -> isSMSDialogShowing = false);
            alertDialogSMSVerification.setCanceledOnTouchOutside(false);
        }
        alertDialogSMSVerification.show();
    }

	/**
	 * Method to show a dialog to indicate the storage status.
	 * @param storageState Storage status.
	 * @param overquotaAlert Flag to indicate that is an overquota alert or not.
	 * @param preWarning Flag to indicate if is a pre-overquota alert or not.
	 */
	private void showStorageStatusDialog(int storageState, boolean overquotaAlert, boolean preWarning){
		logDebug("showStorageStatusDialog");

		if(((MegaApplication) getApplication()).getMyAccountInfo()==null || ((MegaApplication) getApplication()).getMyAccountInfo().getAccountType()==-1){
			logWarning("Do not show dialog, not info of the account received yet");
			return;
		}

		if(isStorageStatusDialogShown){
			logDebug("Storage status dialog already shown");
			return;
		}

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

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
        if(pro3 != null) {
            storageString = getSizeStringGBBased(pro3.getStorage());
            transferString = getSizeStringGBBased(pro3.getTransfer());
        }

		switch (storageState) {
			case MegaApiJava.STORAGE_STATE_GREEN:
				logDebug("STORAGE STATE GREEN");
				return;

			case MegaApiJava.STORAGE_STATE_ORANGE:
				image.setImageResource(R.drawable.ic_storage_almost_full);
				text.setText(String.format(getString(R.string.text_almost_full_warning), storageString,transferString));
				break;

			case MegaApiJava.STORAGE_STATE_RED:
				image.setImageResource(R.drawable.ic_storage_full);
				text.setText(String.format(getString(R.string.text_storage_full_warning), storageString,transferString));
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

		final OnClickListener upgradeClickListener = new OnClickListener(){
			public void onClick(View v) {
				alertDialogStorageStatus.dismiss();
				isStorageStatusDialogShown = false;
				navigateToUpgradeAccount();
			}
		};

		final OnClickListener achievementsClickListener = new OnClickListener(){
			public void onClick(View v) {
				alertDialogStorageStatus.dismiss();
				isStorageStatusDialogShown = false;
				logDebug("Go to achievements section");
				navigateToAchievements();
			}
		};

		final OnClickListener customPlanClickListener = new OnClickListener(){
			public void onClick(View v) {
				alertDialogStorageStatus.dismiss();
				isStorageStatusDialogShown = false;
				askForCustomizedPlan();
			}
		};

		Button verticalDismissButton = (Button) dialogView.findViewById(R.id.vertical_storage_status_button_dissmiss);
		verticalDismissButton.setOnClickListener(dismissClickListener);
		Button horizontalDismissButton = (Button) dialogView.findViewById(R.id.horizontal_storage_status_button_dissmiss);
		horizontalDismissButton.setOnClickListener(dismissClickListener);

		Button verticalActionButton = (Button) dialogView.findViewById(R.id.vertical_storage_status_button_action);
		Button horizontalActionButton = (Button) dialogView.findViewById(R.id.horizontal_storage_status_button_payment);

		Button achievementsButton = (Button) dialogView.findViewById(R.id.vertical_storage_status_button_achievements);
		achievementsButton.setOnClickListener(achievementsClickListener);

		switch (((MegaApplication) getApplication()).getMyAccountInfo().getAccountType()) {
			case MegaAccountDetails.ACCOUNT_TYPE_PROIII:
				logDebug("Show storage status dialog for USER PRO III");
				if (!overquotaAlert) {
					if (storageState == MegaApiJava.STORAGE_STATE_ORANGE) {
						text.setText(getString(R.string.text_almost_full_warning_pro3_account));
					} else if (storageState == MegaApiJava.STORAGE_STATE_RED){
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
						text.setText(String.format(getString(R.string.text_almost_full_warning_pro_account),storageString,transferString));
					} else if (storageState == MegaApiJava.STORAGE_STATE_RED){
						text.setText(String.format(getString(R.string.text_storage_full_warning_pro_account),storageString,transferString));
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

		if(megaApi.isAchievementsEnabled()){
			horizontalButtonsLayout.setVisibility(View.GONE);
			verticalButtonsLayout.setVisibility(View.VISIBLE);
		}
		else{
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
		List<Product> products = MegaApplication.getInstance().getMyAccountInfo().productAccounts;
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

	public void askForCustomizedPlan(){
		logDebug("askForCustomizedPlan");

		StringBuilder body = new StringBuilder();
		body.append(getString(R.string.subject_mail_upgrade_plan));
		body.append("\n\n\n\n\n\n\n");
		body.append(getString(R.string.settings_about_app_version)+" v"+getString(R.string.app_version)+"\n");
		body.append(getString(R.string.user_account_feedback)+"  "+megaApi.getMyEmail());

		if(((MegaApplication) getApplication()).getMyAccountInfo()!=null){
			if(((MegaApplication) getApplication()).getMyAccountInfo().getAccountType()<0||((MegaApplication) getApplication()).getMyAccountInfo().getAccountType()>4){
				body.append(" ("+getString(R.string.my_account_free)+")");
			}
			else{
				switch(((MegaApplication) getApplication()).getMyAccountInfo().getAccountType()){
					case 0:{
						body.append(" ("+getString(R.string.my_account_free)+")");
						break;
					}
					case 1:{
						body.append(" ("+getString(R.string.my_account_pro1)+")");
						break;
					}
					case 2:{
						body.append(" ("+getString(R.string.my_account_pro2)+")");
						break;
					}
					case 3:{
						body.append(" ("+getString(R.string.my_account_pro3)+")");
						break;
					}
					case 4:{
						body.append(" ("+getString(R.string.my_account_prolite_feedback_email)+")");
						break;
					}
				}
			}
		}

		String emailAndroid = MAIL_SUPPORT;
		String subject = getString(R.string.title_mail_upgrade_plan);

		Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + emailAndroid));
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
		emailIntent.putExtra(Intent.EXTRA_TEXT, body.toString());
		startActivity(Intent.createChooser(emailIntent, " "));

	}

	public void updateCancelSubscriptions(){
		logDebug("updateCancelSubscriptions");
		if (cancelSubscription != null){
			cancelSubscription.setVisible(false);
		}
		if (((MegaApplication) getApplication()).getMyAccountInfo().getNumberOfSubscriptions() > 0){
			if (cancelSubscription != null){
				if (drawerItem == DrawerItem.ACCOUNT){
					maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
					if (maFLol != null){
						cancelSubscription.setVisible(true);
					}
				}
			}
		}
	}

	public void updateOfflineView(MegaOffline mOff){
		logDebug("updateOfflineView");
		oFLol = (OfflineFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.OFFLINE.getTag());
		if(oFLol!=null){
			oFLol.hideMultipleSelect();
			if(mOff==null){
				oFLol.refresh();
			}
			else{
				oFLol.refreshPaths(mOff);
			}
			supportInvalidateOptionsMenu();
		}
	}

	public void updateContactsView(boolean contacts, boolean sentRequests, boolean receivedRequests){
		logDebug("updateContactsView");

		if(contacts){
			logDebug("Update Contacts Fragment");
			cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CONTACTS.getTag());
			if (cFLol != null){
				cFLol.hideMultipleSelect();
				cFLol.updateView();
			}
		}

		if(sentRequests){
			logDebug("Update SentRequests Fragment");
			sRFLol = (SentRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.SENT_REQUESTS.getTag());
			if (sRFLol != null){
				sRFLol.hideMultipleSelect();
				sRFLol.updateView();
			}
		}

		if(receivedRequests){
			logDebug("Update ReceivedRequest Fragment");
			rRFLol = (ReceivedRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECEIVED_REQUESTS.getTag());
			if (rRFLol != null){
				rRFLol.hideMultipleSelect();
				rRFLol.updateView();
			}
		}
	}

	/*
	 * Handle processed upload intent
	 */
	public void onIntentProcessed(List<ShareInfo> infos) {
		logDebug("onIntentProcessedLollipop");
//		List<ShareInfo> infos = filePreparedInfos;
		if (statusDialog != null) {
			try {
				statusDialog.dismiss();
			}
			catch(Exception ex){}
		}
		dissmisDialog();

		MegaNode parentNode = getCurrentParentNode(getCurrentParentHandle(), -1);

		if(drawerItem == DrawerItem.ACCOUNT){
			if(infos!=null){
				for (ShareInfo info : infos) {
					String avatarPath = info.getFileAbsolutePath();
					if(avatarPath!=null){
						logDebug("Chosen picture to change the avatar");
						File imgFile = new File(avatarPath);
						File qrFile = buildQrFile(this, megaApi.getMyUser().getEmail() + QR_IMAGE_FILE_NAME);
						File newFile = buildAvatarFile(this, megaApi.getMyUser().getEmail() + "Temp.jpg");


						if (isFileAvailable(qrFile)) {
							qrFile.delete();
						}
                        if (newFile != null) {
                            MegaUtilsAndroid.createAvatar(imgFile,newFile);
                            maFLol = (MyAccountFragmentLollipop)getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
                            if (maFLol != null) {
                                megaApi.setAvatar(newFile.getAbsolutePath(),this);
                            }
                        } else {
							logError("ERROR! Destination PATH is NULL");
                        }
					}
					else{
						logError("The chosen avatar path is NULL");
					}
				}
			}
			else{
				logWarning("infos is NULL");
			}
			return;
		}

		if(parentNode == null){
			showSnackbar(SNACKBAR_TYPE, getString(R.string.error_temporary_unavaible), -1);
			return;
		}

		if (infos == null) {
			showSnackbar(SNACKBAR_TYPE, getString(R.string.upload_can_not_open), -1);
		}
		else {
			showSnackbar(SNACKBAR_TYPE, getResources().getQuantityString(R.plurals.upload_began, infos.size(), infos.size()), -1);

			for (ShareInfo info : infos) {
				if(info.isContact){
					requestContactsPermissions(info, parentNode);
				}
				else{
					Intent intent = new Intent(this, UploadService.class);
					intent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
					intent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
					intent.putExtra(UploadService.EXTRA_LAST_MODIFIED, info.getLastModified());
					intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
					intent.putExtra(UploadService.EXTRA_UPLOAD_COUNT, infos.size());
					startService(intent);
				}
			}
		}
	}

	public void requestContactsPermissions(ShareInfo info, MegaNode parentNode){
		logDebug("requestContactsPermissions");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!checkPermission(Manifest.permission.READ_CONTACTS)) {
				logWarning("No read contacts permission");
				infoManager = info;
				parentNodeManager = parentNode;
				ActivityCompat.requestPermissions(this,	new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_UPLOAD_CONTACT);
			} else {
				uploadContactInfo(info, parentNode);
			}
		}
		else{
			uploadContactInfo(info, parentNode);
		}
	}

	public void uploadContactInfo(ShareInfo info, MegaNode parentNode){
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
				if(phone!=null){
					data.append(", "+phone);
				}

				if(email!=null){
					data.append(", "+email);
				}

				createFile(name, data.toString(), parentNode);
			}
			cursorID.close();
		}
		else{
			showSnackbar(SNACKBAR_TYPE, getString(R.string.error_temporary_unavaible), -1);
		}
	}

	private void createFile(String name, String data, MegaNode parentNode){

		File file = createTemporalTextFile(this, name, data);
		if(file!=null){
			showSnackbar(SNACKBAR_TYPE, getResources().getQuantityString(R.plurals.upload_began, 1, 1), -1);

			Intent intent = new Intent(this, UploadService.class);
			intent.putExtra(UploadService.EXTRA_FILEPATH, file.getAbsolutePath());
			intent.putExtra(UploadService.EXTRA_NAME, file.getName());
			intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
			intent.putExtra(UploadService.EXTRA_SIZE, file.getTotalSpace());
			startService(intent);
		}
		else{
			showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
		}
	}

	@Override
	public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
		logDebug("onRequestStart(CHAT): "+ request.getRequestString());
//		if (request.getType() == MegaChatRequest.TYPE_INITIALIZE){
//			MegaApiAndroid.setLoggerObject(new AndroidLogger());
////			MegaChatApiAndroid.setLoggerObject(new AndroidChatLogger());
//		}
	}

	@Override
	public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		logDebug("onRequestFinish(CHAT): " + request.getRequestString()+"_"+e.getErrorCode());

		if(request.getType() == MegaChatRequest.TYPE_TRUNCATE_HISTORY){
			logDebug("Truncate history request finish.");
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				showSnackbar(SNACKBAR_TYPE, getString(R.string.clear_history_success), -1);
			}
			else{
				showSnackbar(SNACKBAR_TYPE, getString(R.string.clear_history_error), -1);
				logError("Error clearing history: "+e.getErrorString());
			}
		}
		else if(request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM){
			logDebug("Create chat request finish");
			onRequestFinishCreateChat(e.getErrorCode(), request.getChatHandle());
		}
		else if(request.getType() == MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM){
			logDebug("Remove from chat finish!!!");
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				//Update chat view
//				if(rChatFL!=null){
//					rChatFL.setChats();
//				}
			}
			else{
				logError("ERROR WHEN leaving CHAT " + e.getErrorString());
				showSnackbar(SNACKBAR_TYPE, getString(R.string.leave_chat_error), -1);
			}
		}
		else if (request.getType() == MegaChatRequest.TYPE_CONNECT){
			logDebug("Connecting chat finished");

			if (MegaApplication.isFirstConnect()){
				logDebug("Set first connect to false");
				MegaApplication.setFirstConnect(false);
			}

			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				logDebug("CONNECT CHAT finished ");
				if (joiningToChatLink && idJoinToChatLink != -1) {
					megaChatApi.autojoinPublicChat(idJoinToChatLink, this);
				}
				if(drawerItem == DrawerItem.CHAT){
					rChatFL = (RecentChatsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
					if(rChatFL!=null){
						rChatFL.onlineStatusUpdate(megaChatApi.getOnlineStatus());
					}
				}
			}
			else{
				logError("ERROR WHEN CONNECTING " + e.getErrorString());
//				showSnackbar(getString(R.string.chat_connection_error));
			}
		}
		else if (request.getType() == MegaChatRequest.TYPE_DISCONNECT){
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				logDebug("DISConnected from chat!");
			}
			else{
				logError("ERROR WHEN DISCONNECTING " + e.getErrorString());
			}
		}
		else if (request.getType() == MegaChatRequest.TYPE_LOGOUT){
			logDebug("onRequestFinish(CHAT): " + MegaChatRequest.TYPE_LOGOUT);

			if (e.getErrorCode() != MegaError.API_OK){
				logError("MegaChatRequest.TYPE_LOGOUT:ERROR");
			}

			if(getSettingsFragment() != null){
				sttFLol.hidePreferencesChat();
			}

			if (app != null){
				app.disableMegaChatApi();
			}
			resetLoggerSDK();
		}
		else if(request.getType() == MegaChatRequest.TYPE_SET_ONLINE_STATUS){
			if(e.getErrorCode()==MegaChatError.ERROR_OK) {
				logDebug("Status changed to: " + request.getNumber());
			} else if (e.getErrorCode() == MegaChatError.ERROR_ARGS) {
				logWarning("Status not changed, the chosen one is the same");
			} else {
				logError("ERROR WHEN TYPE_SET_ONLINE_STATUS " + e.getErrorString());
				showSnackbar(SNACKBAR_TYPE, getString(R.string.changing_status_error), -1);
			}
		}
		else if(request.getType() == MegaChatRequest.TYPE_ARCHIVE_CHATROOM){
			long chatHandle = request.getChatHandle();
			MegaChatRoom chat = megaChatApi.getChatRoom(chatHandle);
			String chatTitle = getTitleChat(chat);

			if(chatTitle==null){
				chatTitle = "";
			}
			else if(!chatTitle.isEmpty() && chatTitle.length()>60){
				chatTitle = chatTitle.substring(0,59)+"...";
			}

			if(!chatTitle.isEmpty() && chat.isGroup() && !chat.hasCustomTitle()){
				chatTitle = "\""+chatTitle+"\"";
			}

			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				if(request.getFlag()){
					logDebug("Chat archived");
					showSnackbar(SNACKBAR_TYPE, getString(R.string.success_archive_chat, chatTitle), -1);
				}
				else{
					logDebug("Chat unarchived");
					showSnackbar(SNACKBAR_TYPE, getString(R.string.success_unarchive_chat, chatTitle), -1);
				}
			}
			else{
				if(request.getFlag()){
					logError("ERROR WHEN ARCHIVING CHAT " + e.getErrorString());
					showSnackbar(SNACKBAR_TYPE, getString(R.string.error_archive_chat, chatTitle), -1);
				}
				else{
					logError("ERROR WHEN UNARCHIVING CHAT " + e.getErrorString());
					showSnackbar(SNACKBAR_TYPE, getString(R.string.error_unarchive_chat, chatTitle), -1);
				}
			}
		}
		else if(request.getType() == MegaChatRequest.TYPE_LOAD_PREVIEW){
			if(e.getErrorCode()==MegaChatError.ERROR_OK || e.getErrorCode() == MegaChatError.ERROR_EXIST){
				showChatLink(request.getLink());
				dismissOpenLinkDialog();
			}
			else {
				if(e.getErrorCode()==MegaChatError.ERROR_NOENT){
					dismissOpenLinkDialog();
					showAlert(this, getString(R.string.invalid_chat_link), getString(R.string.title_alert_chat_link_error));
				}
				else {
					showOpenLinkError(true, 0);
				}
			}
		}
		else if(request.getType() == MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE){
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				logDebug("MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE: " + request.getFlag());
            }
            else{
				logError("MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE:error: " + e.getErrorType());
			}
		}
		else if (request.getType() == MegaChatRequest.TYPE_AUTOJOIN_PUBLIC_CHAT) {
			joiningToChatLink = false;
			if (e.getErrorCode()==MegaChatError.ERROR_OK) {
				showSnackbar(MESSAGE_SNACKBAR_TYPE, getString(R.string.message_joined_successfully), request.getChatHandle());
			}
			else{
				logError("Error joining to chat: " + e.getErrorString());
				MegaChatRoom chatRoom = megaChatApi.getChatRoom(request.getChatHandle());
				if (chatRoom != null && (chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR
						|| chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_STANDARD || chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_RO)) {
					logWarning("Error joining to chat: I'm already a participant");
					return;
				}
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_chat_link_init_error), -1);
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

	}

	public void onRequestFinishCreateChat(int errorCode, long chatHandle){
		if(errorCode==MegaChatError.ERROR_OK){
			logDebug("Chat CREATED.");

			//Update chat view
			rChatFL = (RecentChatsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
			if(rChatFL!=null){

				if(selectMenuItem!=null){
					selectMenuItem.setVisible(true);
				}
			}

			logDebug("Open new chat: " + chatHandle);
			Intent intent = new Intent(this, ChatActivityLollipop.class);
			intent.setAction(ACTION_CHAT_SHOW_MESSAGES);
			intent.putExtra("CHAT_ID", chatHandle);
			this.startActivity(intent);
		}
		else{
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

	@SuppressLint("NewApi") @Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		logDebug("onRequestFinish: " + request.getRequestString()+"_"+e.getErrorCode());

		if (request.getType() == MegaRequest.TYPE_CREDIT_CARD_CANCEL_SUBSCRIPTIONS){
			if (e.getErrorCode() == MegaError.API_OK){
				showSnackbar(SNACKBAR_TYPE, getString(R.string.cancel_subscription_ok), -1);
			}
			else{
				showSnackbar(SNACKBAR_TYPE, getString(R.string.cancel_subscription_error), -1);
			}
			((MegaApplication) getApplication()).askForCCSubscriptions();
		}
		else if (request.getType() == MegaRequest.TYPE_LOGOUT){
			logDebug("onRequestFinish: " + MegaRequest.TYPE_LOGOUT);

			if (e.getErrorCode() == MegaError.API_OK) {
				logDebug("onRequestFinish:OK:" + MegaRequest.TYPE_LOGOUT);
				logDebug("END logout sdk request - wait chat logout");
			} else if (e.getErrorCode() != MegaError.API_ESID) {
				showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
			}
		} else if(request.getType() == MegaRequest.TYPE_GET_ACHIEVEMENTS) {
            if (e.getErrorCode() == MegaError.API_OK) {
                bonusStorageSMS = getSizeString(request.getMegaAchievementsDetails().getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE));
            }
            showAddPhoneNumberInMenu();
            checkBeforeShow();
        }
		else if(request.getType() == MegaRequest.TYPE_SET_ATTR_USER) {
			logDebug("TYPE_SET_ATTR_USER");
			if(request.getParamType()==MegaApiJava.USER_ATTR_FIRSTNAME){
				logDebug("request.getText(): "+request.getText());
				countUserAttributes--;
				if(((MegaApplication) getApplication()).getMyAccountInfo() == null){
					logError("ERROR: MyAccountInfo is NULL");
				}
				((MegaApplication) getApplication()).getMyAccountInfo().setFirstNameText(request.getText());
				if (e.getErrorCode() == MegaError.API_OK){
					logDebug("The first name has changed");
					maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
					if(maFLol!=null){
						maFLol.updateNameView(((MegaApplication) getApplication()).getMyAccountInfo().getFullName());
					}
					updateUserNameNavigationView(((MegaApplication) getApplication()).getMyAccountInfo().getFullName());
				}
				else{
					logError("Error with first name");
					errorUserAttibutes++;
				}

				if(countUserAttributes==0){
					if(errorUserAttibutes==0){
						logDebug("All user attributes changed!");
						showSnackbar(SNACKBAR_TYPE, getString(R.string.success_changing_user_attributes), -1);
					}
					else{
						logWarning("Some error ocurred when changing an attribute: " + errorUserAttibutes);
						showSnackbar(SNACKBAR_TYPE, getString(R.string.error_changing_user_attributes), -1);
					}
					AccountController aC = new AccountController(this);
					errorUserAttibutes=0;
					aC.setCount(0);
				}
			}
			else if(request.getParamType()==MegaApiJava.USER_ATTR_LASTNAME){
				logDebug("request.getText(): " + request.getText());
				countUserAttributes--;
				if(((MegaApplication) getApplication()).getMyAccountInfo() == null){
					logError("ERROR: MyAccountInfo is NULL");
				}
				((MegaApplication) getApplication()).getMyAccountInfo().setLastNameText(request.getText());
				if (e.getErrorCode() == MegaError.API_OK){
					logDebug("The last name has changed");
					maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
					if(maFLol!=null){
						maFLol.updateNameView(((MegaApplication) getApplication()).getMyAccountInfo().getFullName());
					}
					updateUserNameNavigationView(((MegaApplication) getApplication()).getMyAccountInfo().getFullName());
				}
				else{
					logError("Error with last name");
					errorUserAttibutes++;
				}

				if(countUserAttributes==0){
					if(errorUserAttibutes==0){
						logDebug("All user attributes changed!");
						showSnackbar(SNACKBAR_TYPE, getString(R.string.success_changing_user_attributes), -1);
					}
					else{
						logWarning("Some error ocurred when changing an attribute: " + errorUserAttibutes);
						showSnackbar(SNACKBAR_TYPE, getString(R.string.error_changing_user_attributes), -1);
					}
					AccountController aC = new AccountController(this);
					errorUserAttibutes=0;
					aC.setCount(0);
				}
			}
			else if(request.getParamType() == MegaApiJava.USER_ATTR_PWD_REMINDER){
				logDebug("MK exported - USER_ATTR_PWD_REMINDER finished");
				if (e.getErrorCode() == MegaError.API_OK || e.getErrorCode() == MegaError.API_ENOENT) {
					logDebug("New value of attribute USER_ATTR_PWD_REMINDER: " + request.getText());
				}
			}
			else if (request.getParamType() == MegaApiJava.USER_ATTR_AVATAR) {
				if (e.getErrorCode() == MegaError.API_OK){
					logDebug("Avatar changed!!");
                    if (request.getFile() != null) {
                        File oldFile = new File(request.getFile());
                        if (isFileAvailable(oldFile)) {
                            File newFile = buildAvatarFile(this,megaApi.getMyEmail() + ".jpg");
                            boolean result = oldFile.renameTo(newFile);
                            if (result) {
								logDebug("The avatar file was correctly renamed");
                            }
                        }
						logDebug("User avatar changed!");
						showSnackbar(SNACKBAR_TYPE, getString(R.string.success_changing_user_avatar), -1);
					}
					else{
						logDebug("User avatar deleted!");
						showSnackbar(SNACKBAR_TYPE, getString(R.string.success_deleting_user_avatar), -1);
					}
					setProfileAvatar();

					maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
					if(maFLol!=null){
						maFLol.updateAvatar(false);
					}
				}
				else{
					if(request.getFile()!=null) {
						logError("Some error ocurred when changing avatar: " + e.getErrorString() + " " + e.getErrorCode());
						showSnackbar(SNACKBAR_TYPE, getString(R.string.error_changing_user_avatar), -1);
					} else {
						logError("Some error ocurred when deleting avatar: " + e.getErrorString() + " " + e.getErrorCode());
						showSnackbar(SNACKBAR_TYPE, getString(R.string.error_deleting_user_avatar), -1);
					}

				}
			}
			else if (request.getParamType() == MegaApiJava.USER_ATTR_RICH_PREVIEWS) {
				logDebug("change isRickLinkEnabled - USER_ATTR_RICH_PREVIEWS finished");
				if (e.getErrorCode() != MegaError.API_OK){
					logError("ERROR:USER_ATTR_RICH_PREVIEWS");
					if(getSettingsFragment() != null){
						sttFLol.updateEnabledRichLinks();
					}
				}
			}
			else if (request.getParamType() == MegaApiJava.USER_ATTR_CONTACT_LINK_VERIFICATION) {
				logDebug("change QR autoaccept - USER_ATTR_CONTACT_LINK_VERIFICATION finished");
				if (e.getErrorCode() == MegaError.API_OK) {
					logDebug("OK setContactLinkOption: " + request.getText());
					if (getSettingsFragment() != null) {
						sttFLol.setSetAutoaccept(false);
						if (sttFLol.getAutoacceptSetting()) {
							sttFLol.setAutoacceptSetting(false);
						} else {
							sttFLol.setAutoacceptSetting(true);
						}
						sttFLol.setValueOfAutoaccept(sttFLol.getAutoacceptSetting());
						logDebug("Autoacept: " + sttFLol.getAutoacceptSetting());
					}
				} else {
					logError("Error setContactLinkOption");
				}
			}
			else if(request.getParamType() == MegaApiJava.USER_ATTR_DISABLE_VERSIONS){
				MegaApplication.setDisableFileVersions(Boolean.valueOf(request.getText()));

				if (e.getErrorCode() != MegaError.API_OK) {
					logError("ERROR:USER_ATTR_DISABLE_VERSIONS");
					if(getSettingsFragment() != null){
						sttFLol.updateEnabledFileVersions();
					}

					mStorageFLol = (MyStorageFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_STORAGE.getTag());
					if(mStorageFLol!=null){
						mStorageFLol.refreshVersionsInfo();
					}
				}
				else{
					logDebug("File versioning attribute changed correctly");
				}
			}
			else if(request.getParamType() == MegaApiJava.USER_ATTR_RUBBISH_TIME){
				logDebug("change RB scheduler - USER_ATTR_RUBBISH_TIME finished");
				if(getSettingsFragment() != null){
					if (e.getErrorCode() != MegaError.API_OK){
						showSnackbar(SNACKBAR_TYPE, getString(R.string.error_general_nodes), -1);
					}
					else{
						sttFLol.updateRBScheduler(request.getNumber());
					}
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER){
			if(request.getParamType() == MegaApiJava.USER_ATTR_PWD_REMINDER){
				//Listener from logout menu
				logDebug("TYPE_GET_ATTR_USER. PasswordReminderFromMyAccount: "+getPasswordReminderFromMyAccount());
				if (e.getErrorCode() == MegaError.API_OK || e.getErrorCode() == MegaError.API_ENOENT){
					logDebug("New value of attribute USER_ATTR_PWD_REMINDER: " +request.getText());
					if (request.getFlag()){
						Intent intent = new Intent(this, TestPasswordActivity.class);
						intent.putExtra("logout", getPasswordReminderFromMyAccount());
						startActivity(intent);
					}
					else if (getPasswordReminderFromMyAccount()){
						if (aC == null){
							aC = new AccountController(this);
						}
						aC.logout(this, megaApi);
					}
				}
				setPasswordReminderFromMyAccount(false);
			}
			else if(request.getParamType()==MegaApiJava.USER_ATTR_AVATAR){
				logDebug("Request avatar");
				if (e.getErrorCode() == MegaError.API_OK){
					setProfileAvatar();
					//refresh MyAccountFragment if visible
					maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
					if(maFLol!=null){
						logDebug("Update the account fragment");
						maFLol.updateAvatar(false);
					}
				}
				else{
					if(e.getErrorCode()==MegaError.API_ENOENT) {
						setDefaultAvatar();
					}

					if(e.getErrorCode()==MegaError.API_EARGS){
						logError("Error changing avatar: ");
					}

					//refresh MyAccountFragment if visible
					maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
					if(maFLol!=null){
						logDebug("Update the account fragment");
						maFLol.updateAvatar(false);
					}
				}
			}
			else if(request.getParamType()==MegaApiJava.USER_ATTR_FIRSTNAME){
				if (e.getErrorCode() == MegaError.API_OK){
					logDebug("request.getText(): " + request.getText());
					if(((MegaApplication) getApplication()).getMyAccountInfo()!=null){
						((MegaApplication) getApplication()).getMyAccountInfo().setFirstNameText(request.getText());
					}
					dbH.saveMyFirstName(request.getText());
				}
				else{
					logError("ERROR - request.getText(): " + request.getText());
					if(((MegaApplication) getApplication()).getMyAccountInfo()!=null){
						((MegaApplication) getApplication()).getMyAccountInfo().setFirstNameText("");
					}
				}

				if(((MegaApplication) getApplication()).getMyAccountInfo()!=null){

					((MegaApplication) getApplication()).getMyAccountInfo().setFullName();
					updateUserNameNavigationView(((MegaApplication) getApplication()).getMyAccountInfo().getFullName());

					//refresh MyAccountFragment if visible
					maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
					if(maFLol!=null){
						logDebug("Update the account fragment");
						maFLol.updateNameView(((MegaApplication) getApplication()).getMyAccountInfo().getFullName());
					}
				}
			}
			else if(request.getParamType()==MegaApiJava.USER_ATTR_LASTNAME){
				if (e.getErrorCode() == MegaError.API_OK){
					logDebug("request.getText(): " + request.getText());
					if(((MegaApplication) getApplication()).getMyAccountInfo()!=null){
						((MegaApplication) getApplication()).getMyAccountInfo().setLastNameText(request.getText());
					}

					dbH.saveMyLastName(request.getText());
				}
				else{
					logError("ERROR - request.getText(): " + request.getText());
					if(((MegaApplication) getApplication()).getMyAccountInfo()!=null){
						((MegaApplication) getApplication()).getMyAccountInfo().setLastNameText("");
					}
				}

				if(((MegaApplication) getApplication()).getMyAccountInfo()!=null){

					((MegaApplication) getApplication()).getMyAccountInfo().setFullName();
					updateUserNameNavigationView(((MegaApplication) getApplication()).getMyAccountInfo().getFullName());
					//refresh MyAccountFragment if visible
					maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
					if(maFLol!=null){
						logDebug("Update the account fragment");
						maFLol.updateNameView(((MegaApplication) getApplication()).getMyAccountInfo().getFullName());
					}
				}
			}
            else if(request.getParamType() == MegaApiJava.USER_ATTR_RICH_PREVIEWS){

				if(e.getErrorCode() == MegaError.API_ENOENT){
					logWarning("Attribute USER_ATTR_RICH_PREVIEWS not set");
				}

				if(request.getNumDetails()==1){
					logDebug("USER_ATTR_RICH_PREVIEWS:shouldShowRichLinkWarning:");

					long counter = request.getNumber();
					boolean flag = request.getFlag();

					MegaApplication.setShowRichLinkWarning(request.getFlag());
					MegaApplication.setCounterNotNowRichLinkWarning((int) request.getNumber());
				}
				else if(request.getNumDetails()==0){

					logDebug("USER_ATTR_RICH_PREVIEWS:isRichPreviewsEnabled:" + request.getFlag());

					MegaApplication.setEnabledRichLinks(request.getFlag());

                    if(getSettingsFragment() != null){
						sttFLol.updateEnabledRichLinks();
                    }
				}
            }
			else if(request.getParamType() == MegaApiJava.USER_ATTR_GEOLOCATION){

				if(e.getErrorCode() == MegaError.API_OK){
					logDebug("Attribute USER_ATTR_GEOLOCATION enabled");
					MegaApplication.setEnabledGeoLocation(true);
				}
				else{
					logDebug("Attribute USER_ATTR_GEOLOCATION disabled");
					MegaApplication.setEnabledGeoLocation(false);
				}
			}
            else if (request.getParamType() == MegaApiJava.USER_ATTR_CONTACT_LINK_VERIFICATION) {
				logDebug("Type: GET_ATTR_USER ParamType: USER_ATTR_CONTACT_LINK_VERIFICATION --> getContactLinkOption");
				if (e.getErrorCode() == MegaError.API_OK) {
					if (getSettingsFragment() != null) {
						sttFLol.setAutoacceptSetting(request.getFlag());
						logDebug("OK getContactLinkOption: " + request.getFlag());
//						If user request to set QR autoaccept
						if (sttFLol.getSetAutoaccept()) {
							if (sttFLol.getAutoacceptSetting()) {
								logDebug("setAutoaccept false");
//								If autoaccept is enabled -> request to disable
								megaApi.setContactLinksOption(true, this);
							} else {
								logDebug("setAutoaccept true");
//								If autoaccept is disabled -> request to enable
								megaApi.setContactLinksOption(false, this);
							}
						} else {
							sttFLol.setValueOfAutoaccept(sttFLol.getAutoacceptSetting());
						}
						logDebug("Autoacept: " + sttFLol.getAutoacceptSetting());
					}
				} else if (e.getErrorCode() == MegaError.API_ENOENT) {
					logError("Error MegaError.API_ENOENT getContactLinkOption: " + request.getFlag());
					if (getSettingsFragment() != null) {
						sttFLol.setAutoacceptSetting(request.getFlag());
					}
					megaApi.setContactLinksOption(false, this);
				} else {
					logError("Error getContactLinkOption: " + e.getErrorString());
				}
			}
            else if(request.getParamType() == MegaApiJava.USER_ATTR_DISABLE_VERSIONS){
				MegaApplication.setDisableFileVersions(request.getFlag());
				if(getSettingsFragment() != null){
					sttFLol.updateEnabledFileVersions();
				}

				mStorageFLol = (MyStorageFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_STORAGE.getTag());
				if(mStorageFLol!=null){
					mStorageFLol.refreshVersionsInfo();
				}
			}
			else if(request.getParamType() == MegaApiJava.USER_ATTR_RUBBISH_TIME){
				if(getSettingsFragment() != null){
					if (e.getErrorCode() == MegaError.API_ENOENT){
						if(((MegaApplication) getApplication()).getMyAccountInfo().getAccountType()==MegaAccountDetails.ACCOUNT_TYPE_FREE){
							sttFLol.updateRBScheduler(30);
						}
						else{
							sttFLol.updateRBScheduler(90);
						}
					}
					else{
						sttFLol.updateRBScheduler(request.getNumber());
					}
				}
			}
		}
		else if(request.getType() == MegaRequest.TYPE_GET_CHANGE_EMAIL_LINK) {
			logDebug("TYPE_GET_CHANGE_EMAIL_LINK: " + request.getEmail());
			if (verify2faProgressBar != null) {
				verify2faProgressBar.setVisibility(View.GONE);
			}
			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("The change link has been sent");

				hideKeyboard(managerActivity, 0);
				if (verify2FADialog != null && verify2FADialog.isShowing()) {
					verify2FADialog.dismiss();
				}
				showAlert(this, getString(R.string.email_verification_text_change_mail), getString(R.string.email_verification_title));
			}
			else if(e.getErrorCode() == MegaError.API_EACCESS){
				logWarning("The new mail already exists");

				hideKeyboard(managerActivity, 0);
				if (verify2FADialog != null && verify2FADialog.isShowing()) {
					verify2FADialog.dismiss();
				}
				showAlert(this, getString(R.string.mail_already_used), getString(R.string.email_verification_title));
			}
			else if(e.getErrorCode() == MegaError.API_EEXIST){
				logWarning("Email change already requested (confirmation link already sent).");
				hideKeyboard(managerActivity, 0);
				if (verify2FADialog != null && verify2FADialog.isShowing()) {
					verify2FADialog.dismiss();
				}
				showAlert(this, getString(R.string.mail_changed_confirm_requested), getString(R.string.email_verification_title));
			}
			else if (e.getErrorCode() == MegaError.API_EFAILED || e.getErrorCode() == MegaError.API_EEXPIRED){
				if (is2FAEnabled()){
					verifyShowError();
				}
			}
			else{
				logError("Error when asking for change mail link: " + e.getErrorString() + "___" + e.getErrorCode());

				hideKeyboard(managerActivity, 0);
				if (verify2FADialog != null && verify2FADialog.isShowing()) {
					verify2FADialog.dismiss();
				}
				showAlert(this, getString(R.string.general_text_error), getString(R.string.general_error_word));
			}
		}
		else if(request.getType() == MegaRequest.TYPE_CONFIRM_CHANGE_EMAIL_LINK){
			logDebug("CONFIRM_CHANGE_EMAIL_LINK: " + request.getEmail());
			if(e.getErrorCode() == MegaError.API_OK){
				logDebug("Email changed");
				updateMyEmail(request.getEmail());
				showSnackbar(SNACKBAR_TYPE, getString(R.string.email_changed, request.getEmail()), INVALID_HANDLE);
			}
			else if(e.getErrorCode() == MegaError.API_EEXIST){
				logWarning("The new mail already exists");
				showAlert(this, getString(R.string.mail_already_used), getString(R.string.general_error_word));
			}
			else if(e.getErrorCode() == MegaError.API_ENOENT){
				logError("Email not changed -- API_ENOENT");
				showAlert(this, "Email not changed!" + getString(R.string.old_password_provided_incorrect), getString(R.string.general_error_word));
			}
			else{
				logError("Error when asking for change mail link: " + e.getErrorString() + "___" + e.getErrorCode());
				showAlert(this, getString(R.string.general_text_error), getString(R.string.general_error_word));
			}
		}
		else if(request.getType() == MegaRequest.TYPE_QUERY_RECOVERY_LINK) {
			logDebug("TYPE_GET_RECOVERY_LINK");
			if (e.getErrorCode() == MegaError.API_OK){
				String url = request.getLink();
				logDebug("Cancel account url");
				String myEmail = request.getEmail();
				if(myEmail!=null){
					if(myEmail.equals(megaApi.getMyEmail())){
						logDebug("The email matchs!!!");
						showDialogInsertPassword(url, true);
					}
					else{
						logWarning("Not logged with the correct account: " + e.getErrorString() + "___" + e.getErrorCode());
						showAlert(this, getString(R.string.error_not_logged_with_correct_account), getString(R.string.general_error_word));
					}
				}
				else{
					logError("My email is NULL in the request");
				}
			}
			else if(e.getErrorCode() == MegaError.API_EEXPIRED){
				logError("Error expired link: " + e.getErrorString() + "___" + e.getErrorCode());
				showAlert(this, getString(R.string.cancel_link_expired), getString(R.string.general_error_word));
			}
			else{
				logError("Error when asking for recovery pass link: " + e.getErrorString() + "___" + e.getErrorCode());
				showAlert(this, getString(R.string.general_text_error), getString(R.string.general_error_word));
			}
		}
		else if(request.getType() == MegaRequest.TYPE_GET_CANCEL_LINK){
			logDebug("TYPE_GET_CANCEL_LINK");
			if (verify2faProgressBar != null) {
				verify2faProgressBar.setVisibility(View.GONE);
			}
			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("Cancelation link received!");

				hideKeyboard(managerActivity, 0);
				if (verify2FADialog != null && verify2FADialog.isShowing()) {
					verify2FADialog.dismiss();
				}
				showAlert(this, getString(R.string.email_verification_text), getString(R.string.email_verification_title));
			}
			else if (e.getErrorCode() == MegaError.API_EFAILED || e.getErrorCode() == MegaError.API_EEXPIRED){
				if (is2FAEnabled()){
					verifyShowError();
				}
			}
			else{
				logError("Error when asking for the cancelation link: " + e.getErrorString() + "___" + e.getErrorCode());

				hideKeyboard(managerActivity, 0);
				if (verify2FADialog != null && verify2FADialog.isShowing()){
					verify2FADialog.dismiss();
				}
				showAlert(this, getString(R.string.general_text_error), getString(R.string.general_error_word));
			}
        }
		else if(request.getType() == MegaRequest.TYPE_CONFIRM_CANCEL_LINK){
			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("ACCOUNT CANCELED");
			}
			else if (e.getErrorCode() == MegaError.API_ENOENT){
				logError("Error cancelling account - API_ENOENT: " + e.getErrorString() + "___" + e.getErrorCode());
				showAlert(this, getString(R.string.old_password_provided_incorrect), getString(R.string.general_error_word));
			}
			else{
				logError("Error cancelling account: " + e.getErrorString() + "___" + e.getErrorCode());
				showAlert(this, getString(R.string.general_text_error), getString(R.string.general_error_word));
			}
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE_CONTACT){

			if (e.getErrorCode() == MegaError.API_OK) {
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_removed), -1);
			} else if (e.getErrorCode() == MegaError.API_EMASTERONLY) {
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_remove_business_contact, request.getEmail()), -1);
			} else{
				logError("Error deleting contact");
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_not_removed), -1);
			}
			updateContactsView(true, false, false);
		}
		else if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT){
			logDebug("MegaRequest.TYPE_INVITE_CONTACT finished: " + request.getNumber());

			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}


			if(request.getNumber()==MegaContactRequest.INVITE_ACTION_REMIND){
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_invitation_resent), -1);
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					logDebug("OK INVITE CONTACT: " + request.getEmail());
					if(request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD)
					{
						showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_request_sent, request.getEmail()), -1);
					}
					else if(request.getNumber()==MegaContactRequest.INVITE_ACTION_DELETE)
					{
						showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_invitation_deleted), -1);
					}
				}
				else{
					logError("ERROR invite contact: " + e.getErrorCode() + "___" + e.getErrorString());
					if(e.getErrorCode()==MegaError.API_EEXIST)
					{
						boolean found = false;
						ArrayList<MegaContactRequest> outgoingContactRequests = megaApi.getOutgoingContactRequests();
						if (outgoingContactRequests != null){
							for (int i=0; i< outgoingContactRequests.size(); i++) {
								if (outgoingContactRequests.get(i).getTargetEmail().equals(request.getEmail())) {
									found = true;
									break;
								}
							}
						}
						if (found) {
							showSnackbar(SNACKBAR_TYPE, getString(R.string.invite_not_sent_already_sent, request.getEmail()), -1);
						}
						else {
							showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_already_exists, request.getEmail()), -1);
						}
					}
					else if(request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD && e.getErrorCode()==MegaError.API_EARGS)
					{
						showSnackbar(SNACKBAR_TYPE, getString(R.string.error_own_email_as_contact), -1);
					}
					else{
						showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error), -1);
					}
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_REPLY_CONTACT_REQUEST){
			logDebug("MegaRequest.TYPE_REPLY_CONTACT_REQUEST finished: " + request.getType());

			if (e.getErrorCode() == MegaError.API_OK){

				if(request.getNumber()==MegaContactRequest.REPLY_ACTION_ACCEPT){
					logDebug("I've accepted the invitation");
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_invitacion_reply_accepted), -1);
					MegaContactRequest contactRequest = megaApi.getContactRequestByHandle(request.getNodeHandle());
					logDebug("Handle of the request: " + request.getNodeHandle());
					if(contactRequest!=null){
						//Get the data of the user (avatar and name)
						MegaContactDB contactDB = dbH.findContactByEmail(contactRequest.getSourceEmail());
						if(contactDB==null){
							logWarning("Contact " + contactRequest.getHandle() + " not found! Will be added to DB!");
							cC.addContactDB(contactRequest.getSourceEmail());
						}
						//Update view to get avatar
						cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CONTACTS.getTag());
						if (cFLol != null){
							cFLol.updateView();
						}
					}
					else{
						logError("ContactRequest is NULL");
					}
				}
				else if(request.getNumber()==MegaContactRequest.REPLY_ACTION_DENY){
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_invitacion_reply_declined), -1);
				}
				else if(request.getNumber()==MegaContactRequest.REPLY_ACTION_IGNORE){
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_invitacion_reply_ignored), -1);
				}
			}
			else{
				showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error), -1);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
//				Toast.makeText(this, getString(R.string.context_correctly_moved), Toast.LENGTH_LONG).show();

					if (moveToRubbish){
						//Update both tabs
        				//Rubbish bin
						logDebug("Move to Rubbish");
						refreshAfterMovingToRubbish();
						showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_moved_to_rubbish), -1);
						if (drawerItem == DrawerItem.INBOX) {
							setInboxNavigationDrawer();
						}
						moveToRubbish = false;
						resetAccountDetailsTimeStamp();
					}
					else if(restoreFromRubbish){
						logDebug("Restore from rubbish");
						MegaNode destination = megaApi.getNodeByHandle(request.getParentHandle());
						showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_node_restored, destination.getName()), -1);
						restoreFromRubbish = false;
						resetAccountDetailsTimeStamp();
					}
					else{
						logDebug("Not moved to rubbish");
						refreshAfterMoving();
						showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_moved), -1);
					}
			}
			else {
				if(restoreFromRubbish){
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_restored), -1);
					restoreFromRubbish = false;
				}
				else{
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_moved), -1);
					moveToRubbish = false;
				}
			}

			logDebug("SINGLE move nodes request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS){
			logDebug("MegaRequest.TYPE_PAUSE_TRANSFERS");
			//force update the pause notification to prevent missed onTransferUpdate
			LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION));

			if (e.getErrorCode() == MegaError.API_OK) {

				if(drawerItem == DrawerItem.CLOUD_DRIVE){
					updateTransferButton();
				}

				if(megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD)||megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)){
					logDebug("Show PLAY button");

					tFLol = (TransfersFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.TRANSFERS.getTag());
					if (tFLol != null){
						if (drawerItem == DrawerItem.TRANSFERS) {
							pauseTransfersMenuIcon.setVisible(false);
							playTransfersMenuIcon.setVisible(true);
						}
					}
    			}
    			else{
					logDebug("Show PAUSE button");
					tFLol = (TransfersFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.TRANSFERS.getTag());
					if (tFLol != null){
						if (drawerItem == DrawerItem.TRANSFERS) {
							pauseTransfersMenuIcon.setVisible(true);
							playTransfersMenuIcon.setVisible(false);
						}
					}
    			}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFER) {
			logDebug("One MegaRequest.TYPE_PAUSE_TRANSFER");

			if (e.getErrorCode() == MegaError.API_OK){
				int pendingTransfers = megaApi.getNumPendingDownloads() + megaApi.getNumPendingUploads();

				tFLol = (TransfersFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.TRANSFERS.getTag());
				if (tFLol != null){
					tFLol.changeStatusButton(request.getTransferTag());
				}
			}
			else{
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_general_nodes), -1);
			}
		}
		else if(request.getType() == MegaRequest.TYPE_CANCEL_TRANSFERS){
			logDebug("MegaRequest.TYPE_CANCEL_TRANSFERS");
			//After cancelling all the transfers
			if (e.getErrorCode() == MegaError.API_OK){
				if (drawerItem == DrawerItem.CLOUD_DRIVE){
					setTransfersWidget();
				}

				if (drawerItem == DrawerItem.TRANSFERS) {
					tFLol = (TransfersFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.TRANSFERS.getTag());
					if (tFLol != null){
						pauseTransfersMenuIcon.setVisible(false);
						playTransfersMenuIcon.setVisible(false);
						cancelAllTransfersMenuItem.setVisible(false);
					}
				}
			}
			else{
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_general_nodes), -1);
			}

		}
		else if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFER){
			logDebug("One MegaRequest.TYPE_CANCEL_TRANSFER");

			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("REQUEST OK - wait for onTransferFinish()");
				if (drawerItem == DrawerItem.CLOUD_DRIVE){
					setTransfersWidget();
				}
				supportInvalidateOptionsMenu();
			}
			else{
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_general_nodes), -1);
			}

		}
		else if (request.getType() == MegaRequest.TYPE_KILL_SESSION){
			logDebug("requestFinish TYPE_KILL_SESSION"+MegaRequest.TYPE_KILL_SESSION);
			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("Success kill sessions");
				showSnackbar(SNACKBAR_TYPE, getString(R.string.success_kill_all_sessions), -1);
			}
			else
			{
				logError("Error when killing sessions: " + e.getErrorString());
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_kill_all_sessions), -1);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE){
			logDebug("requestFinish " + MegaRequest.TYPE_REMOVE);
			if (e.getErrorCode() == MegaError.API_OK){
				if (statusDialog != null){
					if (statusDialog.isShowing()){
						try {
							statusDialog.dismiss();
						}
						catch (Exception ex) {}
					}
				}
				refreshAfterRemoving();
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_removed), -1);
				resetAccountDetailsTimeStamp();
			} else if (e.getErrorCode() == MegaError.API_EMASTERONLY) {
				showSnackbar(SNACKBAR_TYPE, e.getErrorString(), -1);
			} else{
			    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_removed), -1);
			}
			logDebug("Remove request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_RENAME){

			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_renamed), -1);
				if (drawerItem == DrawerItem.CLOUD_DRIVE){
					refreshCloudDrive();
				}
				else if (drawerItem == DrawerItem.RUBBISH_BIN){
					refreshRubbishBin();
				}
				else if (drawerItem == DrawerItem.INBOX){
					refreshInboxList();
				}
				else if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE){

					oFLol = (OfflineFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.OFFLINE.getTag());
					if (oFLol != null){
						oFLol.getRecyclerView().invalidate();
					}
				}
				else if (drawerItem == DrawerItem.SHARED_ITEMS){
    				onNodesSharedUpdate();
				}
			}
			else{
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_renamed), -1);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			logDebug("TYPE_COPY");

			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("Show snackbar!!!!!!!!!!!!!!!!!!!");
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_copied), -1);

				if (drawerItem == DrawerItem.CLOUD_DRIVE){
					if (isCloudAdded()){
						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleBrowser), orderCloud);
						fbFLol.setNodes(nodes);
						fbFLol.getRecyclerView().invalidate();
					}
				}
				else if (drawerItem == DrawerItem.RUBBISH_BIN){
					refreshRubbishBin();
				}
				else if (drawerItem == DrawerItem.INBOX){
					refreshInboxList();
				}

				resetAccountDetailsTimeStamp();
			}
			else{
				if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
					logWarning("OVERQUOTA ERROR: " + e.getErrorCode());
					showOverquotaAlert(false);
				}
				else if(e.getErrorCode()==MegaError.API_EGOINGOVERQUOTA){
					logDebug("OVERQUOTA ERROR: " + e.getErrorCode());
					showOverquotaAlert(true);
				}
				else
				{
					showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied), -1);
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER){
			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}
            if (e.getErrorCode() == MegaError.API_OK){
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_created), -1);
				if (drawerItem == DrawerItem.CLOUD_DRIVE){
					if (isCloudAdded()){
						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleBrowser), orderCloud);
						fbFLol.setNodes(nodes);
						fbFLol.getRecyclerView().invalidate();
					}
				} else if (drawerItem == DrawerItem.SHARED_ITEMS){
					onNodesSharedUpdate();
				} else if (drawerItem == DrawerItem.SEARCH) {
					refreshFragment(FragmentTag.SEARCH.getTag());
				}
			}
			else{
				logError("TYPE_CREATE_FOLDER ERROR: " + e.getErrorCode() + " " + e.getErrorString());
				showSnackbar(SNACKBAR_TYPE, getString(R.string.context_folder_no_created), -1);
			}
		} else if (request.getType() == MegaRequest.TYPE_SUBMIT_PURCHASE_RECEIPT){
			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("PURCHASE CORRECT!");
				drawerItem = DrawerItem.CLOUD_DRIVE;
				selectDrawerItemLollipop(drawerItem);
			}
			else{
				logError("PURCHASE WRONG: " + e.getErrorString() + " (" + e.getErrorCode() + ")");
			}
		}
		else if (request.getType() == MegaRequest.TYPE_CLEAN_RUBBISH_BIN){
			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("OK MegaRequest.TYPE_CLEAN_RUBBISH_BIN");
				showSnackbar(SNACKBAR_TYPE, getString(R.string.rubbish_bin_emptied), -1);
				resetAccountDetailsTimeStamp();
				if (getSettingsFragment() != null) {
					sttFLol.resetRubbishInfo();
				}
			}
			else{
				showSnackbar(SNACKBAR_TYPE, getString(R.string.rubbish_bin_no_emptied), -1);
			}
		}
		else if(request.getType() == MegaRequest.TYPE_REMOVE_VERSIONS){
			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("OK MegaRequest.TYPE_REMOVE_VERSIONS");
				showSnackbar(SNACKBAR_TYPE, getString(R.string.success_delete_versions), -1);

				if(getSettingsFragment() != null) {
					sttFLol.resetVersionsInfo();
				}
				//Get info of the version again (after 10 seconds)
				final Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						updateAccountStorageInfo();
					}
				}, 8000);
			}
			else{
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_delete_versions), -1);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_REGISTER_PUSH_NOTIFICATION){
			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("FCM OK TOKEN MegaRequest.TYPE_REGISTER_PUSH_NOTIFICATION");
			}
			else{
				logError("FCM ERROR TOKEN TYPE_REGISTER_PUSH_NOTIFICATION: " + e.getErrorCode() + "__" + e.getErrorString());
			}
		}
		else if (request.getType() == MegaRequest.TYPE_MULTI_FACTOR_AUTH_CHECK) {
			if (e.getErrorCode() == MegaError.API_OK) {
				if (request.getFlag()) {
					is2FAEnabled = true;
				} else {
					is2FAEnabled = false;
				}
				if (getSettingsFragment() != null) {
					sttFLol.update2FAPreference(is2FAEnabled);
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_MULTI_FACTOR_AUTH_SET){
			logDebug("TYPE_MULTI_FACTOR_AUTH_SET: " + e.getErrorCode());
			if (verify2faProgressBar != null) {
				verify2faProgressBar.setVisibility(View.GONE);
			}
			if (!request.getFlag() && e.getErrorCode() == MegaError.API_OK){
				logDebug("Pin correct: Two-Factor Authentication disabled");
				is2FAEnabled = false;
				if (getSettingsFragment() != null) {
					sttFLol.update2FAPreference(false);
					showSnackbar(SNACKBAR_TYPE, getString(R.string.label_2fa_disabled), -1);
				}
				hideKeyboard(managerActivity, 0);
				if (verify2FADialog != null) {
					verify2FADialog.dismiss();
				}
			}
			else if (e.getErrorCode() == MegaError.API_EFAILED){
				logWarning("Pin not correct");
				verifyShowError();
			}
			else {
				hideKeyboard(managerActivity, 0);
				if (verify2FADialog != null) {
					verify2FADialog.dismiss();
				}
				showSnackbar(SNACKBAR_TYPE, getString(R.string.error_disable_2fa), -1);
				logError("An error ocurred trying to disable Two-Factor Authentication");
			}

			megaApi.multiFactorAuthCheck(megaApi.getMyEmail(), this);
		}
		else if(request.getType() == MegaRequest.TYPE_FOLDER_INFO) {
			if (e.getErrorCode() == MegaError.API_OK) {
				MegaFolderInfo info = request.getMegaFolderInfo();
				int numVersions = info.getNumVersions();
				logDebug("Num versions: " + numVersions);
				long previousVersions = info.getVersionsSize();
				logDebug("Previous versions: " + previousVersions);

				if(((MegaApplication) getApplication()).getMyAccountInfo()!=null){
					((MegaApplication) getApplication()).getMyAccountInfo().setNumVersions(numVersions);
					((MegaApplication) getApplication()).getMyAccountInfo().setPreviousVersionsSize(previousVersions);
				}

			} else {
				logError("ERROR requesting version info of the account");
			}

			//Refresh My Storage if it is shown
			mStorageFLol = (MyStorageFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_STORAGE.getTag());
			if(mStorageFLol!=null){
				mStorageFLol.refreshVersionsInfo();
			}

			//Refresh Settings if it is shown
			if(getSettingsFragment() != null) {
				sttFLol.setVersionsInfo();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_CONTACT_LINK_CREATE) {
			maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
			if (maFLol != null) {
				maFLol.initCreateQR(request, e);
			}
		}
	}

	public void updateAccountStorageInfo(){
		logDebug("updateAccountStorageInfo");
		megaApi.getFolderInfo(megaApi.getRootNode(), this);
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		logWarning("onRequestTemporaryError: " + request.getRequestString() + "__" + e.getErrorCode() + "__" + e.getErrorString());
	}

	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
		logDebug("onUsersUpdateLollipop");

		if (users != null){
			logDebug("users.size(): "+users.size());
			for(int i=0; i<users.size();i++){
				MegaUser user=users.get(i);

				if(user!=null){
					// 0 if the change is external.
					// >0 if the change is the result of an explicit request
					// -1 if the change is the result of an implicit request made by the SDK internally

					if(user.isOwnChange()>0){
						logDebug("isOwnChange!!!: " + user.getEmail());
						if (user.hasChanged(MegaUser.CHANGE_TYPE_RICH_PREVIEWS)){
							logDebug("Change on CHANGE_TYPE_RICH_PREVIEWS");
							megaApi.shouldShowRichLinkWarning(this);
							megaApi.isRichPreviewsEnabled(this);
						}
					}
					else{
						logDebug("NOT OWN change");

						logDebug("Changes: " + user.getChanges());

						if(megaApi.getMyUser()!=null) {
							if (user.getHandle() == megaApi.getMyUser().getHandle()) {
								logDebug("Change on my account from another client");
								if (user.hasChanged(MegaUser.CHANGE_TYPE_DISABLE_VERSIONS)) {
									logDebug("Change on CHANGE_TYPE_DISABLE_VERSIONS");
									megaApi.getFileVersionsOption(this);
								}

								if (user.hasChanged(MegaUser.CHANGE_TYPE_CONTACT_LINK_VERIFICATION)) {
									logDebug("Change on CHANGE_TYPE_CONTACT_LINK_VERIFICATION");
									megaApi.getContactLinksOption(this);
								} else if (user.hasChanged(MegaUser.CHANGE_TYPE_RUBBISH_TIME)) {
									logDebug("Change on CHANGE_TYPE_RUBBISH_TIME");
									megaApi.getRubbishBinAutopurgePeriod(this);
								}
							}
						}

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

						if (user.hasChanged(MegaUser.CHANGE_TYPE_AVATAR)){
							logDebug("The user: " + user.getHandle() + "changed his AVATAR");

							File avatar = buildAvatarFile(this, user.getEmail() + ".jpg");
							Bitmap bitmap = null;
							if (isFileAvailable(avatar)){
								avatar.delete();
							}

							if(user.getEmail().equals(megaApi.getMyUser().getEmail())){
								logDebug("I change my avatar");
                                String destinationPath = buildAvatarFile(this,megaApi.getMyEmail() + ".jpg").getAbsolutePath();
								megaApi.getUserAvatar(megaApi.getMyUser(),destinationPath,this);
							}
							else {
								logDebug("Update de ContactsFragment");
								cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CONTACTS.getTag());
								if (cFLol != null) {
									if (drawerItem == DrawerItem.CONTACTS) {
										cFLol.updateView();
									}
								}
							}
						}

						if (user.hasChanged(MegaUser.CHANGE_TYPE_EMAIL)){
							logDebug("CHANGE_TYPE_EMAIL");
							if(user.getEmail().equals(megaApi.getMyUser().getEmail())){
								logDebug("I change my mail");
								updateMyEmail(user.getEmail());
							}
							else{
								logDebug("The contact: " + user.getHandle() + " changes the mail.");
								if(dbH.findContactByHandle(String.valueOf(user.getHandle()))==null){
									logWarning("The contact NOT exists -> DB inconsistency! -> Clear!");
									if (dbH.getContactsSize() != megaApi.getContacts().size()){
										dbH.clearContacts();
										FillDBContactsTask fillDBContactsTask = new FillDBContactsTask(this);
										fillDBContactsTask.execute();
									}
								}
								else{
									logDebug("The contact already exists -> update");
									dbH.setContactMail(user.getHandle(),user.getEmail());
								}
							}
						}

						cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CONTACTS.getTag());
						if(cFLol!=null){
							updateContactsView(true, false, false);
						}
						//When last contact changes avatar, update view.
						maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
						if(maFLol != null) {
							maFLol.updateContactsCount();
							maFLol.updateView();
                        }

						if (isRecentsAdded()) {
							rF.setVisibleContacts();
						}
					}
				}
				else{
					logWarning("user == null --> Continue...");
					continue;
				}
			}
		}
	}

	public void openLocation(long nodeHandle){
		logDebug("Node handle: " + nodeHandle);

		MegaNode node = megaApi.getNodeByHandle(nodeHandle);
		if(node == null){
			return;
		}
		comesFromNotifications = true;
		comesFromNotificationHandle = nodeHandle;
		MegaNode parent = nC.getParent(node);
		if (parent.getHandle() == megaApi.getRootNode().getHandle()){
			//Cloud Drive
			drawerItem = DrawerItem.CLOUD_DRIVE;
			openFolderRefresh = true;
			comesFromNotificationHandleSaved = parentHandleBrowser;
			setParentHandleBrowser(nodeHandle);
			selectDrawerItemLollipop(drawerItem);
		}
		else if (parent.getHandle() == megaApi.getRubbishNode().getHandle()){
			//Rubbish
			drawerItem = DrawerItem.RUBBISH_BIN;
			openFolderRefresh = true;
			comesFromNotificationHandleSaved = parentHandleRubbish;
			setParentHandleRubbish(nodeHandle);
			selectDrawerItemLollipop(drawerItem);
		}
		else if (parent.getHandle() == megaApi.getInboxNode().getHandle()){
			//Inbox
			drawerItem = DrawerItem.INBOX;
			openFolderRefresh = true;
			comesFromNotificationHandleSaved = parentHandleInbox;
			setParentHandleInbox(nodeHandle);
			selectDrawerItemLollipop(drawerItem);
		}
		else{
			//Incoming Shares
			drawerItem = DrawerItem.SHARED_ITEMS;
			indexShares = 0;
			comesFromNotificationDeepBrowserTreeIncoming = deepBrowserTreeIncoming;
			comesFromNotificationHandleSaved = parentHandleIncoming;
			if (parent != null){
				comesFromNotificationsLevel = deepBrowserTreeIncoming = calculateDeepBrowserTreeIncoming(node, this);
			}
			openFolderRefresh = true;
			setParentHandleIncoming(nodeHandle);
			selectDrawerItemLollipop(drawerItem);
		}
	}

	@Override
	public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
		logDebug("onUserAlertsUpdate");

		setNotificationsTitleSection();
		notificFragment = (NotificationsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.NOTIFICATIONS.getTag());
		if(notificFragment!=null){
            notificFragment.updateNotifications(userAlerts);
		}

		updateNavigationToolbarIcon();
	}

	@Override
	public void onEvent(MegaApiJava api, MegaEvent event) {

	}

	public void updateMyEmail(String email){
		logDebug("New email: " + email);
		nVEmail.setText(email);
		String oldEmail = dbH.getMyEmail();
		if(oldEmail!=null){
			logDebug("Old email: " + oldEmail);
            try {
                File avatarFile = buildAvatarFile(this,oldEmail + ".jpg");
                if (isFileAvailable(avatarFile)) {
                    File newFile = buildAvatarFile(this, email + ".jpg");
                    if(newFile != null) {
                        boolean result = avatarFile.renameTo(newFile);
                        if (result) {
							logDebug("The avatar file was correctly renamed");
                        }
                    }
                }
            }
			catch(Exception e){
				logError("EXCEPTION renaming the avatar on changing email", e);
			}
		}
		else{
			logError("ERROR: Old email is NULL");
		}

		dbH.saveMyEmail(email);

		maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
		if(maFLol!=null){
			maFLol.updateMailView(email);
		}
	}

	public long[] getTypeOfSearch(){
		return  searchDate;
	}

	public boolean getIsSearchEnabled(){
		return  isSearchEnabled;
	}
	public void setIsSearchEnabled(boolean isSearchEnabled){
		this.isSearchEnabled = isSearchEnabled;
	}

	public void onNodesCloudDriveUpdate() {
		logDebug("onNodesCloudDriveUpdate");

		rubbishBinFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
		if (rubbishBinFLol != null) {
			rubbishBinFLol.hideMultipleSelect();

			if (isClearRubbishBin) {
				isClearRubbishBin = false;
				parentHandleRubbish = megaApi.getRubbishNode().getHandle();
				ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderCloud);
				rubbishBinFLol.setNodes(nodes);
				rubbishBinFLol.getRecyclerView().invalidate();
			} else {
				refreshRubbishBin();
			}
		}

		refreshCloudDrive();
	}

	public void onNodesInboxUpdate() {
		iFLol = (InboxFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.INBOX.getTag());
		if (iFLol != null){
		    iFLol.hideMultipleSelect();
			iFLol.refresh();
		}
	}

	public void onNodesSearchUpdate() {
		if (getSearchFragment() != null){
			//stop from query for empty string.
			textSubmitted = true;
			sFLol.refresh();
		}
	}

	public void refreshIncomingShares () {
		if (!isIncomingAdded()) return;

		inSFLol.hideMultipleSelect();
		inSFLol.refresh();
	}

	private void refreshOutgoingShares () {
		if (!isOutgoingAdded()) return;

		outSFLol.hideMultipleSelect();
		outSFLol.refresh();
    }

    private void refreshLinks () {
        if (!isLinksAdded()) return;

        lF.refresh();
    }

	public void refreshInboxList () {
		iFLol = (InboxFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.INBOX.getTag());
		if (iFLol != null){
			iFLol.getRecyclerView().invalidate();
		}
	}

	public void onNodesSharedUpdate() {
		logDebug("onNodesSharedUpdate");

		refreshOutgoingShares();
		refreshIncomingShares();
		refreshLinks();

		refreshSharesPageAdapter();
	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> updatedNodes) {
		logDebug("onNodesUpdateLollipop");

		try {
			statusDialog.dismiss();
		}
		catch (Exception ex) {}

		boolean updateContacts = false;

		if(updatedNodes!=null){
			//Verify is it is a new item to the inbox
			for(int i=0;i<updatedNodes.size(); i++){
				MegaNode updatedNode = updatedNodes.get(i);

				if(!updateContacts){
					if(updatedNode.isInShare()){
						updateContacts = true;
					}
				}

				if(updatedNode.getParentHandle()==inboxNode.getHandle()){
					logDebug("New element to Inbox!!");
					setInboxNavigationDrawer();
				}
			}
		}

		if(updateContacts){
			cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CONTACTS.getTag());
			if (cFLol != null){
				logDebug("Incoming update - update contacts section");
				cFLol.updateShares();
			}
		}

		refreshFragment(FragmentTag.RECENTS.getTag());

		onNodesCloudDriveUpdate();

		if(cloudPageAdapter!=null){
			cloudPageAdapter.notifyDataSetChanged();
		}

		onNodesSearchUpdate();

		onNodesSharedUpdate();

		onNodesInboxUpdate();

		checkCameraUploadFolder(false,updatedNodes);

		cuFL = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CAMERA_UPLOADS.getTag());
		if (cuFL != null){
			long cameraUploadHandle = cuFL.getPhotoSyncHandle();
			MegaNode nps = megaApi.getNodeByHandle(cameraUploadHandle);
			logDebug("Camera Uploads Handle: " + cameraUploadHandle);
			if (nps != null){
				logDebug("nps != null");
				ArrayList<MegaNode> nodes = megaApi.getChildren(nps, orderCamera);

				if(firstNavigationLevel){
					cuFL.setNodes(nodes);
				}else{
					if(getIsSearchEnabled()){
						if((searchByDate != null)&&(searchDate !=null)){
							ArrayList<MegaNode> nodesSearch = cuFL.searchDate(searchDate, nodes);
							cuFL.setNodes(nodesSearch);
							isSearchEnabled = true;
						}else{
							cuFL.setNodes(nodes);

						}
					}else{
						cuFL.setNodes(nodes);

					}


				}
			}
		}

		muFLol = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MEDIA_UPLOADS.getTag());
		if (muFLol != null){
			long cameraUploadHandle = muFLol.getPhotoSyncHandle();
			MegaNode nps = megaApi.getNodeByHandle(cameraUploadHandle);
			logDebug("Media Uploads Handle: " + cameraUploadHandle);
			if (nps != null){
				logDebug("nps != null");
				ArrayList<MegaNode> nodes = megaApi.getChildren(nps, orderCamera);
				if(firstNavigationLevel){
					muFLol.setNodes(nodes);
				}else{
					if(getIsSearchEnabled()){
						if((searchByDate != null)&&(searchDate !=null)){
							ArrayList<MegaNode> nodesSearch = muFLol.searchDate(searchDate, nodes);
							muFLol.setNodes(nodesSearch);
							isSearchEnabled = true;
						}else{
							muFLol.setNodes(nodes);
						}
					}else{
						muFLol.setNodes(nodes);

					}

				}
			}
		}

		setToolbarTitle();
		supportInvalidateOptionsMenu();
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		logDebug("onReloadNeeded");
	}

	@Override
	public void onAccountUpdate(MegaApiJava api) {
		logDebug("onAccountUpdate");
	}

	@Override
	public void onContactRequestsUpdate(MegaApiJava api,ArrayList<MegaContactRequest> requests) {
		logDebug("onContactRequestsUpdate");

		if(requests!=null){
			for(int i=0; i<requests.size();i++){
				MegaContactRequest req = requests.get(i);
				if(req.isOutgoing()){
					logDebug("SENT REQUEST");
					logDebug("STATUS: " + req.getStatus() + ", Contact Handle: " + req.getHandle());
					if(req.getStatus()==MegaContactRequest.STATUS_ACCEPTED){
						cC.addContactDB(req.getTargetEmail());
					}
					updateContactsView(true, true, false);
				}
				else{
					logDebug("RECEIVED REQUEST");
					setContactTitleSection();
					logDebug("STATUS: " + req.getStatus() + " Contact Handle: " + req.getHandle());
					if(req.getStatus()==MegaContactRequest.STATUS_ACCEPTED){
						cC.addContactDB(req.getSourceEmail());
					}
					updateContactsView(true, false, true);
				}
			}
		}

		updateNavigationToolbarIcon();
	}

	////TRANSFERS/////

	public void changeTransfersStatus(){
		logDebug("changeTransfersStatus");
		if(megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD)||megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)){
			logDebug("Show PLAY button");
			megaApi.pauseTransfers(false, this);
		}
		else{
			logDebug("Transfers are play -> pause");
			megaApi.pauseTransfers(true, this);
		}
	}

	public void pauseIndividualTransfer(MegaTransfer mT) {
		if (mT == null) {
			logWarning("Transfer object is null.");
			return;
		}

		if (mT.getState() == MegaTransfer.STATE_PAUSED) {
			logDebug("Resume transfer - Node handle: " + mT.getNodeHandle());
			megaApi.pauseTransfer(mT, false, managerActivity);
		} else {
			logDebug("Pause transfer - Node handle: " + mT.getNodeHandle());
			megaApi.pauseTransfer(mT, true, managerActivity);
		}
	}

	public void showConfirmationClearCompletedTransfers (){
		logDebug("showConfirmationClearCompletedTransfers");

		//Show confirmation message
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {
						logDebug("Pressed button positive to clear transfers");
						dbH.emptyCompletedTransfers();
						completedTFLol = (CompletedTransfersFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.COMPLETED_TRANSFERS.getTag());
						if (completedTFLol != null) {
							completedTFLol.updateCompletedTransfers();
						}
						supportInvalidateOptionsMenu();
						break;
					}
					case DialogInterface.BUTTON_NEGATIVE: {
						break;
					}
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		builder.setTitle(getResources().getString(R.string.cancel_transfer_title));

		builder.setMessage(getResources().getString(R.string.confirmation_to_clear_completed_transfers));
		builder.setPositiveButton(R.string.general_clear, dialogClickListener);
		builder.setNegativeButton(R.string.general_cancel, dialogClickListener);

		builder.show();
	}

	public void showConfirmationCancelTransfer (MegaTransfer t, boolean cancelValue){
		logDebug("showConfirmationCancelTransfer");
		final MegaTransfer mT = t;
		final boolean cancel = cancelValue;

		//Show confirmation message
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						logDebug("Pressed button positive to cancel transfer");
						if(cancel){
							megaApi.cancelTransfer(mT, managerActivity);
						}
						else{
							pauseIndividualTransfer(mT);
						}

						break;

					case DialogInterface.BUTTON_NEGATIVE:
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		builder.setTitle(getResources().getString(R.string.cancel_transfer_title));
		if(cancel){

			builder.setMessage(getResources().getString(R.string.cancel_transfer_confirmation));
			builder.setPositiveButton(R.string.context_delete, dialogClickListener);
			builder.setNegativeButton(R.string.general_cancel, dialogClickListener);
		}
		else {

			if(mT.getState()==MegaTransfer.STATE_PAUSED){
				builder.setMessage(getResources().getString(R.string.menu_resume_individual_transfer));
				builder.setPositiveButton(R.string.button_resume_individual_transfer, dialogClickListener);
				builder.setNegativeButton(R.string.general_cancel, dialogClickListener);
			}
			else{
				builder.setMessage(getResources().getString(R.string.menu_pause_individual_transfer));
				builder.setPositiveButton(R.string.action_pause, dialogClickListener);
				builder.setNegativeButton(R.string.general_cancel, dialogClickListener);
			}

		}
		builder.show();
	}

	public void showConfirmationCancelAllTransfers (){
		logDebug("showConfirmationCancelAllTransfers");

		//Show confirmation message
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						logDebug("Pressed button positive to cancel transfer");
						megaApi.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD);
						megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD);
                        cancelAllUploads(ManagerActivityLollipop.this);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		builder.setTitle(getResources().getString(R.string.cancel_transfer_title));

		builder.setMessage(getResources().getString(R.string.cancel_all_transfer_confirmation));
		builder.setPositiveButton(R.string.context_delete, dialogClickListener);
		builder.setNegativeButton(R.string.general_cancel, dialogClickListener);

		builder.show();
	}

	public void addCompletedTransfer(MegaTransfer transfer){
		logDebug("Node Handle: " + transfer.getNodeHandle());

		String size = getSizeString(transfer.getTotalBytes());
		AndroidCompletedTransfer completedTransfer = new AndroidCompletedTransfer(transfer.getFileName(), transfer.getType(), transfer.getState(), size, transfer.getNodeHandle()+"", transfer.getParentPath());

		completedTFLol = (CompletedTransfersFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.COMPLETED_TRANSFERS.getTag());
		if(completedTFLol!=null){
			completedTFLol.transferFinish(completedTransfer);
		}
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		logDebug("onTransferStart: " + transfer.getNotificationNumber()+ "-" + transfer.getNodeHandle() + " - " + transfer.getTag());

		if (!existOngoingTransfers(megaApi)) {
			updateLogoutWarnings();
		}

		if(transfer.isStreamingTransfer()){
			return;
		}

		if(transferCallback<transfer.getNotificationNumber()) {
			transferCallback = transfer.getNotificationNumber();
			long now = Calendar.getInstance().getTimeInMillis();
			lastTimeOnTransferUpdate = now;

			if(!transfer.isFolderTransfer()){
				transfersInProgress.add(transfer.getTag());

				if (drawerItem == DrawerItem.CLOUD_DRIVE){
					setTransfersWidget();
				}

				tFLol = (TransfersFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.TRANSFERS.getTag());
				if (tFLol != null){
					tFLol.transferStart(transfer);
				}
			}
		}
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError e) {
		logDebug("onTransferFinish: " + transfer.getNodeHandle() + " - " + transfer.getTag() + "- " +transfer.getNotificationNumber());

		if(transfer.isStreamingTransfer()){
			return;
		}

		//workaround - can not get folder transfer children detail except using global listener
        if(transfer.getType()==MegaTransfer.TYPE_UPLOAD && transfer.getFolderTransferTag() > 0) {
            Intent intent = new Intent(this,UploadService.class);
            if (e.getErrorCode() == MegaError.API_OK) {
                intent.setAction(ACTION_CHILD_UPLOADED_OK);
                startService(intent);
            }else{
                intent.setAction(ACTION_CHILD_UPLOADED_FAILED);
                startService(intent);
            }
        }

        //workaround - can not get folder transfer children detail except using global listener
        if(transfer.getType()==MegaTransfer.TYPE_UPLOAD && transfer.getFolderTransferTag() > 0) {
            Intent intent = new Intent(this,UploadService.class);
            if (e.getErrorCode() == MegaError.API_OK) {
                intent.setAction(ACTION_CHILD_UPLOADED_OK);
                startService(intent);
            }else{
                intent.setAction(ACTION_CHILD_UPLOADED_FAILED);
                startService(intent);
            }
        }

		if(transferCallback<transfer.getNotificationNumber()) {

			transferCallback = transfer.getNotificationNumber();
			long now = Calendar.getInstance().getTimeInMillis();
			lastTimeOnTransferUpdate = now;

			if(!transfer.isFolderTransfer()){
				ListIterator li = transfersInProgress.listIterator();
				int index = 0;
				while(li.hasNext()) {
					Integer next = (Integer) li.next();
					if(next == transfer.getTag()){
						index=li.previousIndex();
						break;
					}
				}

				if(!transfersInProgress.isEmpty()){
					transfersInProgress.remove(index);
					logDebug("The transfer with index " + index + " has been removed, left: " + transfersInProgress.size());
				}
				else{
					logDebug("The transferInProgress is EMPTY");
				}

				if(transfer.getState()==MegaTransfer.STATE_COMPLETED){
					addCompletedTransfer(transfer);
				}

				int pendingTransfers = 	megaApi.getNumPendingDownloads() + megaApi.getNumPendingUploads();

				if(pendingTransfers<=0){
					if(transfersBottomSheet!=null){
						if(transfersBottomSheet.isAdded()){
							transfersBottomSheet.dismiss();
						}
					}
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

				tFLol = (TransfersFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.TRANSFERS.getTag());
				if (tFLol != null){
					tFLol.transferFinish(transfer.getTag());
				}
				else{
					logWarning("tF is null!");
				}

				if (drawerItem == DrawerItem.CLOUD_DRIVE){
					setTransfersWidget();
				}
			}
		}

		if (!existOngoingTransfers(megaApi)) {
			updateLogoutWarnings();
		}
	}

	private void updateLogoutWarnings() {
		if (getMyAccountFragment() != null) {
			maFLol.checkLogoutWarnings();
		}
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {

		if(transfer.isStreamingTransfer()){
			return;
		}

		long now = Calendar.getInstance().getTimeInMillis();
		if((now - lastTimeOnTransferUpdate)>ONTRANSFERUPDATE_REFRESH_MILLIS){
			logDebug("Update onTransferUpdate: " + transfer.getNodeHandle() + " - " + transfer.getTag()+ " - "+ transfer.getNotificationNumber());
			lastTimeOnTransferUpdate = now;

			if (!transfer.isFolderTransfer()){
				if(transferCallback<transfer.getNotificationNumber()){
					transferCallback = transfer.getNotificationNumber();

					if (drawerItem == DrawerItem.CLOUD_DRIVE){
						setTransfersWidget();
					}

					tFLol = (TransfersFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.TRANSFERS.getTag());
					if (tFLol != null){
						tFLol.transferUpdate(transfer);
					}

				}
			}
		}
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e) {
		logWarning("onTransferTemporaryError: " + transfer.getNodeHandle() + " - " + transfer.getTag());

		if(e.getErrorCode() == MegaError.API_EOVERQUOTA){
			if (e.getValue() != 0) {
				logDebug("TRANSFER OVERQUOTA ERROR: " + e.getErrorCode());
				if (drawerItem == DrawerItem.CLOUD_DRIVE){
					setTransfersWidget();
				}
			}
			else {
				logWarning("STORAGE OVERQUOTA ERROR: " + e.getErrorCode());
                //work around - SDK does not return over quota error for folder upload,
                //so need to be notified from global listener
                if (transfer.getType() == MegaTransfer.TYPE_UPLOAD) {
					logDebug("Over quota");
                    Intent intent = new Intent(this,UploadService.class);
                    intent.setAction(ACTION_OVERQUOTA_STORAGE);
                    startService(intent);
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
		return isListCameraUploads;
	}
	public boolean isSmallGridCameraUploads() {
		return isSmallGridCameraUploads;
	}
	public void setSmallGridCameraUploads(boolean isSmallGridCameraUploads) {
		this.isSmallGridCameraUploads = isSmallGridCameraUploads;
	}

	public boolean getFirstLogin() {
		return firstLogin;
	}
	public void setFirstLogin(boolean flag){
		firstLogin = flag;
	}

	public void setListCameraUploads(boolean isListCameraUploads) {
		this.isListCameraUploads = isListCameraUploads;
	}

	public void setOrderCloud(int orderCloud) {
		logDebug("setOrderCloud");
		this.orderCloud = orderCloud;
		if(prefs!=null){
			prefs.setPreferredSortCloud(String.valueOf(orderCloud));
		}
		dbH.setPreferredSortCloud(String.valueOf(orderCloud));
	}

	public int getOrderContacts() {
		return orderContacts;
	}

	public void setOrderContacts(int orderContacts) {
		logDebug("setOrderContacts");
		this.orderContacts = orderContacts;
		if(prefs!=null) {
			prefs.setPreferredSortContacts(String.valueOf(orderContacts));
		}
		dbH.setPreferredSortContacts(String.valueOf(orderContacts));
	}

    public void setOrderCamera(int orderCamera) {
        logDebug("setOrderCamera");
        this.orderCamera = orderCamera;
        if (prefs != null) {
            prefs.setPreferredSortCameraUpload(String.valueOf(orderCamera));
        }
        dbH.setPreferredSortCameraUpload(String.valueOf(orderCamera));
    }

	public int getOrderOthers() {
		return orderOthers;
	}

	public void setOrderOthers(int orderOthers) {
		logDebug("setOrderOthers");
		this.orderOthers = orderOthers;
		if(prefs!=null) {
			prefs.setPreferredSortOthers(String.valueOf(orderOthers));
		}
		dbH.setPreferredSortOthers(String.valueOf(orderOthers));
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
		deepBrowserTreeIncoming=deep;
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

	public static DrawerItem getDrawerItem() {
		return drawerItem;
	}

	public static void setDrawerItem(DrawerItem drawerItem) {
		ManagerActivityLollipop.drawerItem = drawerItem;
	}

	public int getTabItemCloud() {
		if (viewPagerCloud == null) return ERROR_TAB;

		return viewPagerCloud.getCurrentItem();
	}

	public int getTabItemShares(){
		if (viewPagerShares == null) return ERROR_TAB;

		return viewPagerShares.getCurrentItem();
	}

	public int getTabItemContacts(){
		if (viewPagerContacts == null) return ERROR_TAB;

		return viewPagerContacts.getCurrentItem();
	}

	private int getTabItemMyAccount () {
		if (viewPagerMyAccount == null) return ERROR_TAB;

		return viewPagerMyAccount.getCurrentItem();
	}

	public void setTabItemShares(int index){
		viewPagerShares.setCurrentItem(index);
	}

	public void setTabItemContacts(int index){
		viewPagerContacts.setCurrentItem(index);
	}

	public void showChatPanel(MegaChatListItem chat){
		logDebug("showChatPanel");

		if (chat == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

		selectedChatItemId = chat.getChatId();
		bottomSheetDialogFragment = new ChatBottomSheetDialogFragment();
		bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
	}

	private void showTransfersPanel(){
		logDebug("showChatPanel");

		int pendingTransfers = megaApi.getNumPendingUploads()+megaApi.getNumPendingDownloads();

		if(pendingTransfers>0 && !isBottomSheetDialogShown(transfersBottomSheet)){
			transfersBottomSheet = new TransfersBottomSheetDialogFragment();
			transfersBottomSheet.show(getSupportFragmentManager(), transfersBottomSheet.getTag());
		}
	}

	public void updateUserNameNavigationView(String fullName){
		logDebug("updateUserNameNavigationView");

		nVDisplayName.setText(fullName);
		setProfileAvatar();
	}

	public void updateMailNavigationView(String email){
		logDebug("updateMailNavigationView");
		nVEmail.setText(megaApi.getMyEmail());
	}

	public void animateFABCollection(){
		logDebug("animateFABCollection");

		if(isFabOpen){
			mainFabButtonChat.startAnimation(rotateLeftAnim);
			firstFabButtonChat.startAnimation(closeFabAnim);
			secondFabButtonChat.startAnimation(closeFabAnim);
			thirdFabButtonChat.startAnimation(closeFabAnim);
			firstFabButtonChat.setClickable(false);
			secondFabButtonChat.setClickable(false);
			thirdFabButtonChat.setClickable(false);
			isFabOpen = false;
			logDebug("Close COLLECTION FAB");

		} else {
			mainFabButtonChat.startAnimation(rotateRightAnim);
			firstFabButtonChat.startAnimation(openFabAnim);
			secondFabButtonChat.startAnimation(openFabAnim);
			thirdFabButtonChat.startAnimation(openFabAnim);
			firstFabButtonChat.setClickable(true);
			secondFabButtonChat.setClickable(true);
			thirdFabButtonChat.setClickable(true);
			isFabOpen = true;
			fabButton.hide();
			fabButtonsLayout.setVisibility(View.VISIBLE);
			logDebug("Open COLLECTION FAB");
		}
	}

	public void hideFabButton(){
		fabButton.hide();
	}

	public void showFabButton(){
		logDebug("showFabButton");
		if(drawerItem==null){
			return;
		}
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) fabButton.getLayoutParams();
		fabButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_add_white));
		switch (drawerItem){
			case CLOUD_DRIVE:{
				logDebug("Cloud Drive SECTION");
				lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
				fabButton.show();
				break;
			}
			case RUBBISH_BIN:{
				fabButton.hide();
				break;
			}
			case SHARED_ITEMS:{
				logDebug("Shared Items SECTION");
				int indexShares = getTabItemShares();

				switch (indexShares) {
					case INCOMING_TAB: {
						logDebug("showFabButton: INCOMING TAB");
						if (isIncomingAdded()) {
							if (deepBrowserTreeIncoming <= 0) {
								logDebug("showFabButton: fabButton GONE");
								fabButton.hide();
							}
							else {
								//Check the folder's permissions
								MegaNode parentNodeInSF = megaApi.getNodeByHandle(parentHandleIncoming);
								if (parentNodeInSF != null) {
									int accessLevel = megaApi.getAccess(parentNodeInSF);
									logDebug("showFabButton: Node: " + parentNodeInSF.getName());

									switch (accessLevel) {
										case MegaShare.ACCESS_OWNER:
										case MegaShare.ACCESS_READWRITE:
										case MegaShare.ACCESS_FULL: {
											lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
											fabButton.show();
											break;
										}
										case MegaShare.ACCESS_READ: {
											fabButton.hide();
											break;
										}
									}
								}
								else {
									fabButton.hide();
								}
							}
						}
						break;
					}
					case OUTGOING_TAB: {
						logDebug("showFabButton: OUTGOING TAB");
						if (isOutgoingAdded()) {
							if (deepBrowserTreeOutgoing <= 0) {
								fabButton.hide();
							}
							else {
								lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
								fabButton.show();
							}
						}
						break;
					}
					case LINKS_TAB:
						if (isLinksAdded()) {
							if (deepBrowserTreeLinks <= 0) {
								fabButton.hide();
							} else {
								lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
								fabButton.show();
							}
						}
						break;

					default: {
						fabButton.hide();
						break;
					}
				}
				break;
			}
			case CONTACTS:{
				int indexContacts = getTabItemContacts();
				switch(indexContacts){
					case 0:
					case 1:{
						lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
						fabButton.show();
						break;
					}
					default:{
						fabButton.hide();
						break;
					}
				}
				break;
			}
			case CHAT:{
				if(megaChatApi!=null){
					fabButton.setImageDrawable(mutateIconSecondary(this, R.drawable.ic_chat, R.color.white));
					lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
					fabButton.show();
				}
				else{
					fabButton.hide();
				}
				break;
			}
			case SEARCH: {
				if (shouldShowFabWhenSearch()){
					lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
					fabButton.show();
				} else {
					fabButton.hide();
				}
				break;
			}
			default:{
				logDebug("Default GONE fabButton");
				fabButton.hide();
				break;
			}
		}
		fabButton.setLayoutParams(lp);
	}

	private boolean shouldShowFabWhenSearch() {
		if (searchDrawerItem == null) {
			return false;
		}

		switch (searchDrawerItem) {
			case RUBBISH_BIN:
			case INBOX:
				return false;
			case CLOUD_DRIVE:
				return true;
			case SHARED_ITEMS:
				if (isFirstNavigationLevel()) return false;

				if (searchSharedTab == INCOMING_TAB) {
					if (parentHandleIncoming == INVALID_HANDLE) return false;

					MegaNode node;
					if (parentHandleSearch == INVALID_HANDLE) {
						node = megaApi.getNodeByHandle(parentHandleIncoming);
					} else {
						node = megaApi.getNodeByHandle(parentHandleSearch);
					}
					if (node == null) return false;

					int accessLevel = megaApi.getAccess(node);
					if (accessLevel == MegaShare.ACCESS_FULL || accessLevel == MegaShare.ACCESS_OWNER || accessLevel == MegaShare.ACCESS_READWRITE) {
						return true;
					}
				} else if ((searchSharedTab == OUTGOING_TAB && parentHandleOutgoing != INVALID_HANDLE)
						|| (searchSharedTab == LINKS_TAB && parentHandleLinks != INVALID_HANDLE)) {
					return true;
				}
			default:
				return false;
		}
	}

	public void openAdvancedDevices (long handleToDownload, boolean highPriority){
		logDebug("openAdvancedDevices");
//		handleToDownload = handle;
		String externalPath = getExternalCardPath();

		if(externalPath!=null){
			MegaNode node = megaApi.getNodeByHandle(handleToDownload);
			if(node!=null){

//				File newFile =  new File(externalPath+"/"+node.getName());
				File newFile =  new File(node.getName());
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

				try{
					startActivityForResult(intent, WRITE_SD_CARD_REQUEST_CODE);
				}
				catch(Exception e){
					logError("Exception in External SDCARD", e);
					Environment.getExternalStorageDirectory();
					Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
					toast.show();
				}
			}
		}
		else{
			logWarning("No external SD card");
			Environment.getExternalStorageDirectory();
			Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
			toast.show();
		}
	}

	public MegaNode getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(MegaNode selectedNode) {
		this.selectedNode = selectedNode;
	}


	public ContactsFragmentLollipop getContactsFragment() {
		return cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CONTACTS.getTag());
	}

	public MyAccountFragmentLollipop getMyAccountFragment() {
		return maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_ACCOUNT.getTag());
	}

	public MyStorageFragmentLollipop getMyStorageFragment() {
		return mStorageFLol = (MyStorageFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MY_STORAGE.getTag());
	}

	public UpgradeAccountFragmentLollipop getUpgradeAccountFragment() {
		return upAFL;
	}

	public CentiliFragmentLollipop getCentiliFragment() {
		return ctFL;
	}

	public FortumoFragmentLollipop getFortumoFragment() {
		return fFL;
	}

	public void setContactsFragment(ContactsFragmentLollipop cFLol) {
		this.cFLol = cFLol;
	}

	public SettingsFragmentLollipop getSettingsFragment() {
		return sttFLol = (SettingsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.SETTINGS.getTag());
	}

	public void setSettingsFragment(SettingsFragmentLollipop sttFLol) {
		this.sttFLol = sttFLol;
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

	public void setSelectedPaymentMethod(int selectedPaymentMethod) {
		this.selectedPaymentMethod = selectedPaymentMethod;
	}
	public void visibilitySearch(boolean visibility){
		searchByDate.setVisible(visibility);
	}

	public void setSelectedAccountType(int selectedAccountType) {
		this.selectedAccountType = selectedAccountType;
	}


	public int getDisplayedAccountType() {
		return displayedAccountType;
	}

	public void setDisplayedAccountType(int displayedAccountType) {
		this.displayedAccountType = displayedAccountType;
	}

	@Override
	public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {
		if (item != null){
			logDebug("Chat ID:" + item.getChatId());
			if (item.isPreview()) {
				return;
			}
		}
		else{
			logWarning("Item NULL");
			return;
		}

		rChatFL = (RecentChatsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
		if(rChatFL!=null){
			rChatFL.listItemUpdate(item);
		}

		if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT)) {
			logDebug("Change unread count: " + item.getUnreadCount());
			setChatBadge();
			updateNavigationToolbarIcon();
		}
	}

	@Override
	public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {
		logDebug("New state: " + newState);
		if (newState == MegaChatApi.INIT_ERROR) {
			// chat cannot initialize, disable chat completely
//			logDebug("newState == MegaChatApi.INIT_ERROR");
//			if (chatSettings == null) {
//				logDebug("1 - onChatInitStateUpdate: ERROR----> Switch OFF chat");
//				chatSettings = new ChatSettings(false + "", true + "", "", true + "");
//				dbH.setChatSettings(chatSettings);
//			} else {
//				logDebug("2 - onChatInitStateUpdate: ERROR----> Switch OFF chat");
//				dbH.setEnabledChat(false + "");
//			}
//			if(megaChatApi!=null){
//				megaChatApi.logout(null);
//			}
		}
	}

	@Override
	public void onChatOnlineStatusUpdate(MegaChatApiJava api, long userHandle, int status, boolean inProgress) {
		logDebug("Status: " + status + ", In Progress: " + inProgress);
		if(inProgress){
			status = -1;
		}

		if (megaChatApi != null) {
			rChatFL = (RecentChatsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
			if (userHandle == megaChatApi.getMyUserHandle()) {
				logDebug("My own status update");
				setContactStatus();
				if (drawerItem == DrawerItem.CHAT) {
					if (rChatFL != null) {
						rChatFL.onlineStatusUpdate(status);
					}
				}
			} else {
				logDebug("Status update for the user: " + userHandle);
				rChatFL = (RecentChatsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
				if (rChatFL != null) {
					logDebug("Update Recent chats view");
					rChatFL.contactStatusUpdate(userHandle, status);
				}

				cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CONTACTS.getTag());
				if (cFLol != null) {
					logDebug("Update Contacts view");
					cFLol.contactPresenceUpdate(userHandle, status);
				}
			}
		}
	}

	@Override
	public void onChatPresenceConfigUpdate(MegaChatApiJava api, MegaChatPresenceConfig config) {
		logDebug("onChatPresenceConfigUpdate");
		if(config!=null){
			logDebug("Config status: " + config.getOnlineStatus());
			logDebug("Config autoway: " + config.isAutoawayEnabled());
			logDebug("Config persist: " + config.isPersist());
			logDebug("Config lastGreen: " + config.isLastGreenVisible());
			boolean isLastGreen = config.isLastGreenVisible();
			if(config.isPending()){
				logDebug("Config is pending - do not update UI");
			}
			else{
				if(getSettingsFragment() != null){
					sttFLol.updatePresenceConfigChat(false, config);
				}
				else{
					logWarning("sttFLol no added or null");
				}
			}
		}
		else{
			logWarning("Config is null");
		}
	}

	@Override
	public void onChatConnectionStateUpdate(MegaChatApiJava api, long chatid, int newState) {
		logDebug("Chat ID: " + chatid + ", New state: " + newState);

		if(newState==MegaChatApi.CHAT_CONNECTION_ONLINE && chatid==-1){
			logDebug("Online Connection: " + chatid);
			rChatFL = (RecentChatsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
			if (rChatFL != null){
				rChatFL.setChats();
				if(drawerItem == DrawerItem.CHAT){
					rChatFL.setStatus();
				}
			}
		}
	}

    @Override
    public void onChatPresenceLastGreen(MegaChatApiJava api, long userhandle, int lastGreen) {
		logDebug("User Handle: " + userhandle + ", Last green: " + lastGreen);

		cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CONTACTS.getTag());
		if(cFLol!=null){
			logDebug("Update Contacts view");
			cFLol.contactLastGreenUpdate(userhandle, lastGreen);
		}
    }

	public void copyError(){
		try {
			statusDialog.dismiss();
			showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied), -1);
		}
		catch (Exception ex) {}
	}

	public void changeStatusBarColor(int option) {
		logDebug("changeStatusBarColor");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			final Window window = this.getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			if (option ==  COLOR_STATUS_BAR_ACCENT) {
				window.setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.accentColorDark));
				changeActionBarElevation(true);
			}
			else if (option == COLOR_STATUS_BAR_ZERO_DELAY){
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						window.setStatusBarColor(0);
					}
				}, 300);
			}
			else if (option == COLOR_STATUS_BAR_ZERO) {
				window.setStatusBarColor(0);
			}
            else if (option == COLOR_STATUS_BAR_SMS_VERIFICATION) {
                window.setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.status_bar_sms_verification));
            }
			else if (option == COLOR_STATUS_BAR_SEARCH_DELAY){
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						window.setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.status_bar_search));
					}
				}, 300);
			}
		}
		if (option == COLOR_STATUS_BAR_ACCENT){
			drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
		}
		else if (option == COLOR_STATUS_BAR_ZERO_DELAY){
			drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
		}

	}

	public void setDrawerLockMode (boolean locked) {
        if (locked){
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
        else{
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

	public void changeActionBarElevation(boolean withElevation){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			if (withElevation) {
				abL.setElevation(px2dp(4, outMetrics));
				if (elevation > 0) {
					elevation = 0;
					abL.postDelayed(new Runnable() {
						@Override
						public void run() {
							abL.setElevation(px2dp(4, outMetrics));
						}
					}, 100);
				}
			}
			else {
				abL.setElevation(0);
			}
		}
	}

	public long getParentHandleInbox() {
		return parentHandleInbox;
	}

	public void setContactTitleSection(){
		ArrayList<MegaContactRequest> requests = megaApi.getIncomingContactRequests();

		if (contactsSectionText != null) {
			if(requests!=null){
				int pendingRequest = requests.size();
				if(pendingRequest==0){
					contactsSectionText.setText(getString(R.string.section_contacts));
				}
				else{
					setFormattedContactTitleSection(pendingRequest, true);
				}
			}
		}
	}

	void setFormattedContactTitleSection (int pendingRequest, boolean enable) {
		String textToShow = String.format(getString(R.string.section_contacts_with_notification), pendingRequest);
		try {
			if (enable) {
				textToShow = textToShow.replace("[A]", "<font color=\'#ff333a\'>");
			}
			else {
				textToShow = textToShow.replace("[A]", "<font color=\'#ffcccc\'>");
			}
			textToShow = textToShow.replace("[/A]", "</font>");
		}
		catch(Exception e){
			logError("Formatted string: " + textToShow, e);
		}

		Spanned result = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
			result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
		} else {
			result = Html.fromHtml(textToShow);
		}
		contactsSectionText.setText(result);
	}

	public void setNotificationsTitleSection(){
		int unread = megaApi.getNumUnreadUserAlerts();

		if(unread == 0){
			notificationsSectionText.setText(getString(R.string.title_properties_chat_contact_notifications));
		}
		else{
			setFormattedNotificationsTitleSection(unread, true);
		}
	}

	void setFormattedNotificationsTitleSection (int unread, boolean enable) {
		String textToShow = String.format(getString(R.string.section_notification_with_unread), unread);
		try {
			if (enable) {
				textToShow = textToShow.replace("[A]", "<font color=\'#ff333a\'>");
			}
			else {
				textToShow = textToShow.replace("[A]", "<font color=\'#ffcccc\'>");
			}
			textToShow = textToShow.replace("[/A]", "</font>");
		}
		catch(Exception e){
			logError("Formatted string: " + textToShow, e);
		}

		Spanned result = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
			result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
		} else {
			result = Html.fromHtml(textToShow);
		}
		notificationsSectionText.setText(result);
	}

	@Override
	public void onActionModeStarted(ActionMode mode) {
		super.onActionModeStarted(mode);
		getTheme().applyStyle(R.style.ActionOverflowButtonStyle, true);
	}

	public void setChatBadge() {
		if(megaChatApi != null) {
			int numberUnread = megaChatApi.getUnreadChats();
			if (numberUnread == 0) {
				chatBadge.setVisibility(View.GONE);
			}
			else {
				chatBadge.setVisibility(View.VISIBLE);
				if (numberUnread > 9) {
					((TextView) chatBadge.findViewById(R.id.chat_badge_text)).setText("9+");
				}
				else {
					((TextView) chatBadge.findViewById(R.id.chat_badge_text)).setText("" + numberUnread);
				}
			}
		}
		else {
			chatBadge.setVisibility(View.GONE);
		}
	}

	private void setCallBadge(){
		if (!isOnline(this) || megaChatApi == null || megaChatApi.getNumCalls() <= 0 || (megaChatApi.getNumCalls() == 1 && participatingInACall())) {
			callBadge.setVisibility(View.GONE);
			return;
		}
		callBadge.setVisibility(View.VISIBLE);
	}

	public void showEvaluatedAppDialog(){

		LayoutInflater inflater = getLayoutInflater();
		View dialoglayout = inflater.inflate(R.layout.evaluate_the_app_dialog, null);

		final CheckedTextView rateAppCheck = (CheckedTextView) dialoglayout.findViewById(R.id.rate_the_app);
		rateAppCheck.setText(getString(R.string.rate_the_app_panel));
		rateAppCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
		rateAppCheck.setCompoundDrawablePadding(scaleWidthPx(10, outMetrics));
		ViewGroup.MarginLayoutParams rateAppMLP = (ViewGroup.MarginLayoutParams) rateAppCheck.getLayoutParams();
		rateAppMLP.setMargins(scaleWidthPx(15, outMetrics), scaleHeightPx(10, outMetrics), 0, scaleHeightPx(10, outMetrics));

		final CheckedTextView sendFeedbackCheck = (CheckedTextView) dialoglayout.findViewById(R.id.send_feedback);
		sendFeedbackCheck.setText(getString(R.string.send_feedback_panel));
		sendFeedbackCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
		sendFeedbackCheck.setCompoundDrawablePadding(scaleWidthPx(10, outMetrics));
		ViewGroup.MarginLayoutParams sendFeedbackMLP = (ViewGroup.MarginLayoutParams) sendFeedbackCheck.getLayoutParams();
		sendFeedbackMLP.setMargins(scaleWidthPx(15, outMetrics), scaleHeightPx(10, outMetrics), 0, scaleHeightPx(10, outMetrics));

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setView(dialoglayout);

		builder.setTitle(getString(R.string.title_evaluate_the_app_panel));
		evaluateAppDialog = builder.create();

		evaluateAppDialog.show();

		rateAppCheck.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				logDebug("Rate the app");
				//Rate the app option:
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=mega.privacy.android.app") ) );

				if (evaluateAppDialog!= null){
					evaluateAppDialog.dismiss();
				}
			}
		});

		sendFeedbackCheck.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				logDebug("Send Feedback");

				//Send feedback option:
				StringBuilder body = new StringBuilder();
				body.append(getString(R.string.setting_feedback_body));
				body.append("\n\n\n\n\n\n\n\n\n\n\n");
				body.append(getString(R.string.settings_feedback_body_device_model)+"  "+getDeviceName()+"\n");
				body.append(getString(R.string.settings_feedback_body_android_version)+"  "+Build.VERSION.RELEASE+" "+Build.DISPLAY+"\n");
				body.append(getString(R.string.user_account_feedback)+"  "+megaApi.getMyEmail());

				if(((MegaApplication) getApplication()).getMyAccountInfo()!=null){
					if(((MegaApplication) getApplication()).getMyAccountInfo().getAccountType()<0||((MegaApplication) getApplication()).getMyAccountInfo().getAccountType()>4){
						body.append(" ("+getString(R.string.my_account_free)+")");
					}
					else{
						switch(((MegaApplication) getApplication()).getMyAccountInfo().getAccountType()){
							case 0:{
								body.append(" ("+getString(R.string.my_account_free)+")");
								break;
							}
							case 1:{
								body.append(" ("+getString(R.string.my_account_pro1)+")");
								break;
							}
							case 2:{
								body.append(" ("+getString(R.string.my_account_pro2)+")");
								break;
							}
							case 3:{
								body.append(" ("+getString(R.string.my_account_pro3)+")");
								break;
							}
							case 4:{
								body.append(" ("+getString(R.string.my_account_prolite_feedback_email)+")");
								break;
							}
						}
					}
				}

				String emailAndroid = MAIL_ANDROID;
				String versionApp = (getString(R.string.app_version));
				String subject = getString(R.string.setting_feedback_subject)+" v"+versionApp;

				Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + emailAndroid));
				emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
				emailIntent.putExtra(Intent.EXTRA_TEXT, body.toString());
				startActivity(Intent.createChooser(emailIntent, " "));

				if (evaluateAppDialog != null){
					evaluateAppDialog.dismiss();
				}
			}
		});

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

	public boolean getPasswordReminderFromMyAccount() {
		return passwordReminderFromMyAccount;
	}

	public void setPasswordReminderFromMyAccount(boolean passwordReminderFromMyAccount) {
		this.passwordReminderFromMyAccount = passwordReminderFromMyAccount;
	}

	public void refreshMenu(){
		logDebug("refreshMenu");
		supportInvalidateOptionsMenu();
	}

	public boolean is2FAEnabled (){
		return is2FAEnabled;
	}

	public void setNewMail (String newMail) {
		this.newMail = newMail;
	}

	//need to check image existence before use due to android content provider issue.
	//Can not check query count - still get count = 1 even file does not exist
	private boolean checkProfileImageExistence(Uri uri){
		boolean isFileExist = false;
		InputStream inputStream;
		try {
			inputStream = this.getContentResolver().openInputStream(uri);
			if(inputStream != null){
				isFileExist = true;
			}
			inputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return isFileExist;
	}

	public void showHideBottomNavigationView(boolean hide) {

		final CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

		if (bNV != null) {
			if (hide && bNV.getVisibility() == View.VISIBLE) {
				params.setMargins(0, 0, 0, 0);
				fragmentLayout.setLayoutParams(params);
				bNV.animate().translationY(220).setDuration(400L).withEndAction(new Runnable() {
					@Override
					public void run() {
						bNV.setVisibility(View.GONE);
					}
				}).start();
			}
			else if (!hide && bNV.getVisibility() == View.GONE){
				params.setMargins(0, 0, 0, px2dp(56, outMetrics));
				bNV.setVisibility(View.VISIBLE);
				bNV.animate().translationY(0).setDuration(400L).withEndAction(new Runnable() {
					@Override
					public void run() {
						fragmentLayout.setLayoutParams(params);
					}
				}).start();
			}
		}
	}

	public void markNotificationsSeen(boolean fromAndroidNotification){
		logDebug("fromAndroidNotification: " + fromAndroidNotification);

		if(fromAndroidNotification){
			megaApi.acknowledgeUserAlerts();
		}
		else{
			if(drawerItem == ManagerActivityLollipop.DrawerItem.NOTIFICATIONS && app.isActivityVisible()){
				megaApi.acknowledgeUserAlerts();
			}
		}
	}

	public void openSearchView () {
		String querySaved = searchQuery;
		if (searchMenuItem != null) {
			searchMenuItem.expandActionView();
			if (searchView != null) {
				searchView.setQuery(querySaved, false);
			}
		}
	}

	public boolean checkPermission(String permission) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return true;
		}

		try {
			return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
		} catch (IllegalArgumentException ex) {
			logWarning("IllegalArgument Exception is thrown");
			return false;
		}
	}

	public boolean isValidSearchQuery() {
		return searchQuery != null && !searchQuery.isEmpty();
	}

	public void openSearchNode(MegaNode node, int position, int[] screenPosition, ImageView imageView) {
		if (node.isFolder()) {
			switch (drawerItem) {
				case CLOUD_DRIVE: {
					setParentHandleBrowser(node.getHandle());
					refreshFragment(FragmentTag.CLOUD_DRIVE.getTag());
					if (cloudPageAdapter != null) {
						cloudPageAdapter.notifyDataSetChanged();
					}
					break;
				}
				case SHARED_ITEMS: {
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
				}
				case INBOX: {
					setParentHandleInbox(node.getHandle());
					refreshFragment(FragmentTag.INBOX.getTag());
					break;
				}
			}
		} else {
			switch (drawerItem) {
				case CLOUD_DRIVE: {
					if (isCloudAdded()) {
						fbFLol.openFile(node, position, screenPosition, imageView);
					}
					break;
				}
				case SHARED_ITEMS: {
					if (viewPagerShares == null || sharesPageAdapter == null) break;

					if (getTabItemShares() == INCOMING_TAB && isIncomingAdded()) {
						inSFLol.openFile(node, INCOMING_SHARES_ADAPTER, position, screenPosition, imageView);
					} else if (getTabItemShares() == OUTGOING_TAB && isOutgoingAdded()) {
						outSFLol.openFile(node, OUTGOING_SHARES_ADAPTER, position, screenPosition, imageView);
					} else if (getTabItemShares() == LINKS_TAB && isLinksAdded()) {
					    lF.openFile(node, LINKS_ADAPTER, position, screenPosition, imageView);
                    }
					break;
				}
				case INBOX: {
					if (getInboxFragment() != null) {
						iFLol.openFile(node, position, screenPosition, imageView);
					}
					break;
				}
			}
		}
	}

	public boolean isSearchViewExpanded() {
		if (searchMenuItem != null) {
			return searchMenuItem.isActionViewExpanded();
		}

		return false;
	}

	public void closeSearchView () {
	    if (searchMenuItem != null && searchMenuItem.isActionViewExpanded()) {
	        searchMenuItem.collapseActionView();
        }
    }

	public void setTextSubmitted () {
	    if (searchView != null) {
	    	if (!isValidSearchQuery()) {
				searchExpand = false;
				return;
			}
	        searchView.setQuery(searchQuery, true);
        }
    }

	public boolean isSearchOpen() {
		return searchQuery != null && searchExpand;
	}

    public void setAccountFragmentPreUpgradeAccount (int accountFragment) {
		this.accountFragmentPreUpgradeAccount = accountFragment;
	}

    private void refreshAddPhoneNumberButton(){
        navigationDrawerAddPhoneContainer.setVisibility(View.GONE);
        if(maFLol != null){
            maFLol.updateAddPhoneNumberLabel();
        }
    }

    public void showAddPhoneNumberInMenu(){
	    if(megaApi == null){
	        return;
        }
        if(canVoluntaryVerifyPhoneNumber()) {
            if(megaApi.isAchievementsEnabled()) {
                String message = String.format(getString(R.string.sms_add_phone_number_dialog_msg_achievement_user), bonusStorageSMS);
                addPhoneNumberLabel.setText(message);
            } else {
                addPhoneNumberLabel.setText(R.string.sms_add_phone_number_dialog_msg_non_achievement_user);
            }
            navigationDrawerAddPhoneContainer.setVisibility(View.VISIBLE);
        } else {
            navigationDrawerAddPhoneContainer.setVisibility(View.GONE);
        }
    }

	public void deleteInviteContactHandle(){
		handleInviteContact = -1;
	}

    @Override
    public void onTrimMemory(int level){
        // Determine which lifecycle or system event was raised.
        //we will stop creating thumbnails while the phone is running low on memory to prevent OOM
		logDebug("Level: " + level);
        if(level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL){
			logWarning("Low memory");
			ThumbnailUtilsLollipop.isDeviceMemoryLow = true;
        }else{
			logDebug("Memory OK");
			ThumbnailUtilsLollipop.isDeviceMemoryLow = false;
        }
    }

	private void setSearchDrawerItem() {
		if (drawerItem == DrawerItem.SEARCH) return;

		searchDrawerItem = drawerItem;
		searchSharedTab = getTabItemShares();

		drawerItem = DrawerItem.SEARCH;
	}

	public String getOfflineSearchPath() {
		if (isOfflineSearchPathEmpty()) return null;

		return offlineSearchPaths.get(offlineSearchPaths.size() - 1);
	}

	public String getInitialSearchPath() {
		if (isOfflineSearchPathEmpty()) return null;

		return offlineSearchPaths.get(0);
	}

	public void addOfflineSearchPath(String path) {
		if (offlineSearchPaths == null) {
			offlineSearchPaths = new ArrayList<>();
		}
		offlineSearchPaths.add(path);
	}

	public void removeOfflineSearchPath() {
		if (isOfflineSearchPathEmpty()) return;

		offlineSearchPaths.remove(offlineSearchPaths.size() - 1);
	}

	public boolean isOfflineSearchPathEmpty() {
		return offlineSearchPaths == null || offlineSearchPaths.isEmpty();
	}

    public DrawerItem getSearchDrawerItem(){
		return searchDrawerItem;
	}

	/**
	 * This method show or hide the tabs of Cloud Drive section and blocks or not
	 * respectively the swipe of the ViewPager
	 *
	 * @param show if true, it shows the tabs and enable the swipe, else hide the tabs
	 *             and disable the swipe
	 */
	public void showTabCloud(boolean show) {
		if (drawerItem != DrawerItem.CLOUD_DRIVE) return;

		if (show || getTabItemCloud() == CLOUD_TAB) {
			tabLayoutCloud.setVisibility(View.VISIBLE);
			viewPagerCloud.disableSwipe(false);
		} else {
			tabLayoutCloud.setVisibility(View.GONE);
			viewPagerCloud.disableSwipe(true);
		}
	}

	public void setBucketSaved(BucketSaved bucketSaved) {
		this.bucketSaved = bucketSaved;
	}

	public BucketSaved getBucketSaved() {
		return bucketSaved;
	}

	public void setDeepBrowserTreeRecents(int deepBrowserTreeRecents) {
		this.deepBrowserTreeRecents = deepBrowserTreeRecents;
	}

	public int getDeepBrowserTreeRecents() {
		return deepBrowserTreeRecents;
	}

	/**
	 * This method sets the transfers widget when there are transfers in progress
	 * and it is in Cloud Drive section or Recents section
	 */
	public void setTransfersWidget() {
		logDebug("setTransfersWidget");

		if (!isOnline(this) || transfersOverViewLayout == null) return;

		if (drawerItem != DrawerItem.CLOUD_DRIVE) {
			transfersOverViewLayout.setVisibility(View.GONE);
			return;
		}

		//Check transfers in progress
		int pendingTransfers = megaApi.getNumPendingDownloads() + megaApi.getNumPendingUploads();
		int totalTransfers = megaApi.getTotalDownloads() + megaApi.getTotalUploads();

		long totalSizePendingTransfer = megaApi.getTotalDownloadBytes() + megaApi.getTotalUploadBytes();
		long totalSizeTransfered = megaApi.getTotalDownloadedBytes() + megaApi.getTotalUploadedBytes();

		if (pendingTransfers > 0) {
			logDebug("Transfers in progress");
			transfersOverViewLayout.setVisibility(View.VISIBLE);
			dotsOptionsTransfersLayout.setOnClickListener(this);
			actionLayout.setOnClickListener(this);

			updateTransferButton();

			int progressPercent = (int) Math.round((double) totalSizeTransfered / totalSizePendingTransfer * 100);
			progressBarTransfers.setProgress(progressPercent);
			logDebug("Progress Percent: " + progressPercent);

			long delay = megaApi.getBandwidthOverquotaDelay();
			if (delay == 0) {
//				transfersTitleText.setText(getString(R.string.section_transfers));
			} else {
				logDebug("Overquota delay activated until: " + delay);
				transfersTitleText.setText(getString(R.string.title_depleted_transfer_overquota));
			}

			int inProgress = totalTransfers - pendingTransfers + 1;

			String progressText = getResources().getQuantityString(R.plurals.text_number_transfers, totalTransfers, inProgress, totalTransfers);
			transfersNumberText.setText(progressText);
		} else {
			logDebug("NO TRANSFERS in progress");
			if (transfersOverViewLayout != null) {
				transfersOverViewLayout.setVisibility(View.GONE);
			}
			dotsOptionsTransfersLayout.setOnClickListener(null);
			actionLayout.setOnClickListener(null);
		}
	}

	private void putTransfersWidget(){
		if(isScreenInPortrait(this)) {
			transfersOverViewLayout.getLayoutParams().height = px2dp(72, outMetrics);
			transfersTextLayout.setOrientation(LinearLayout.VERTICAL);
		}else{
			transfersOverViewLayout.getLayoutParams().height = px2dp(50, outMetrics);
			transfersTextLayout.setOrientation(LinearLayout.HORIZONTAL);
		}
		transfersOverViewLayout.requestLayout();
	}

	/**
	 * This method sets "Tap to return to call" banner when there is a call in progress
	 * and it is in Cloud Drive section, Recents section, Incoming section, Outgoing section or in the chats list.
	 */
	private void setCallWidget() {
		setCallBadge();

		if (drawerItem != DrawerItem.CLOUD_DRIVE && drawerItem != DrawerItem.SHARED_ITEMS && drawerItem != DrawerItem.CHAT || !isScreenInPortrait(this)) {
			hideCallWidget();
			return;
		}

		showCallLayout(this, callInProgressLayout, callInProgressChrono, callInProgressText);
	}

	private void hideCallWidget() {
		if (callInProgressChrono != null) {
			activateChrono(false, callInProgressChrono, null);
		}
		if (callInProgressLayout != null && callInProgressLayout.getVisibility() == View.VISIBLE) {
			callInProgressLayout.setVisibility(View.GONE);
		}
	}

	/**
	 * This method shows or hides the toolbar icon to return a call when a call is in progress
	 * and it is in Cloud Drive section, Recents section, Incoming section, Outgoing section or in the chats list.
	 */
	private void setCallMenuItem(){
		if((drawerItem == DrawerItem.CHAT || drawerItem == DrawerItem.CLOUD_DRIVE || drawerItem == DrawerItem.SHARED_ITEMS)
				&& !isScreenInPortrait(this) && participatingInACall() && getChatCallInProgress() != -1){
			returnCallMenuItem.setVisible(true);

			MegaChatCall call = megaChatApi.getChatCall(getChatCallInProgress());
			int callStatus = call.getStatus();

			if(callStatus == MegaChatCall.CALL_STATUS_RECONNECTING){
				layoutCallMenuItem.setBackground(ContextCompat.getDrawable(this,R.drawable.reconnection_rounded));
			}else{
				layoutCallMenuItem.setBackground(ContextCompat.getDrawable(this,R.drawable.dark_rounded_chat_own_message));
			}

			if(chronometerMenuItem != null && (callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS || callStatus == MegaChatCall.CALL_STATUS_JOINING)){
				if(chronometerMenuItem.getVisibility() == View.VISIBLE) return;
				chronometerMenuItem.setVisibility(View.VISIBLE);
				chronometerMenuItem.setBase(SystemClock.elapsedRealtime() - (call.getDuration() * 1000));
				chronometerMenuItem.start();
				chronometerMenuItem.setFormat(" %s");
			}else{
				if(chronometerMenuItem.getVisibility() == View.GONE) return;
				chronometerMenuItem.stop();
				chronometerMenuItem.setVisibility(View.GONE);
			}
		}else {
			hideCallMenuItem();
		}
	}

	private void hideCallMenuItem() {
		if (chronometerMenuItem != null) {
			chronometerMenuItem.stop();
		}
		if (returnCallMenuItem != null) {
			returnCallMenuItem.setVisible(false);
		}
	}

	/**
	 * This method updates the action button, play or pause, of the
	 * transfers widget
	 */
	private void updateTransferButton() {
		logDebug("updateTransferButton");

		if (transfersOverViewLayout.getVisibility() == View.VISIBLE) {
			if (megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD) || megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)) {
				logDebug("show PLAY button");
				playButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_play));
				transfersTitleText.setText(getString(R.string.paused_transfers_title));
			} else {
				logDebug("show PAUSE button");
				playButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pause));
				transfersTitleText.setText(getString(R.string.section_transfers));
			}
		} else {
			logDebug("Transfer panel not visible");
		}
	}

	private boolean checkPermissionsCall(){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasCameraPermission = (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
			if (!hasCameraPermission) {
				typesCameraPermission = START_CALL_PERMISSIONS;

				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
				return false;
			}
			boolean hasRecordAudioPermission = (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
			if (!hasRecordAudioPermission) {
				typesCameraPermission = START_CALL_PERMISSIONS;
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO);
				return false;
			}
		}
		return true;
	}


	public String getSearchQuery() {
		return searchQuery;
	}

	public int getSearchSharedTab() {
		return searchSharedTab;
	}

	public void setSearchQuery(String searchQuery) {
		this.searchQuery = searchQuery;
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

	private SearchFragmentLollipop getSearchFragment() {
		return sFLol = (SearchFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.SEARCH.getTag());
	}

	private RubbishBinFragmentLollipop getRubbishBinFragment() {
		return rubbishBinFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RUBBISH_BIN.getTag());
	}

	private OfflineFragmentLollipop getOfflineFragment() {
		return oFLol = (OfflineFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.OFFLINE.getTag());
	}

	private CameraUploadFragmentLollipop getCameraUploadFragment() {
		return cuFL = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.CAMERA_UPLOADS.getTag());
	}

	private CameraUploadFragmentLollipop getMediaUploadFragment() {
		return muFLol = (CameraUploadFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.MEDIA_UPLOADS.getTag());
	}

	private InboxFragmentLollipop getInboxFragment() {
		return iFLol = (InboxFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.INBOX.getTag());
	}

	private SentRequestsFragmentLollipop getSentRequestFragment() {
		return sRFLol = (SentRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.SENT_REQUESTS.getTag());
	}

	private ReceivedRequestsFragmentLollipop getReceivedRequestFragment() {
		return rRFLol = (ReceivedRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECEIVED_REQUESTS.getTag());
	}

	private RecentChatsFragmentLollipop getChatsFragment() {
		return rChatFL = (RecentChatsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.RECENT_CHAT.getTag());
	}

	private CompletedTransfersFragmentLollipop getCompletedTransfersFragment() {
		return completedTFLol = (CompletedTransfersFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.COMPLETED_TRANSFERS.getTag());
	}

	private TransfersFragmentLollipop getTransfersFragment() {
		return tFLol = (TransfersFragmentLollipop) getSupportFragmentManager().findFragmentByTag(FragmentTag.TRANSFERS.getTag());
	}
}
