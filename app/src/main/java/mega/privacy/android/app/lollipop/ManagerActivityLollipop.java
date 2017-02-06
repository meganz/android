package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader.TileMode;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.MegaContact;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.OldPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.adapters.CloudDrivePagerAdapter;
import mega.privacy.android.app.lollipop.adapters.ContactsPageAdapter;
import mega.privacy.android.app.lollipop.adapters.MyAccountPageAdapter;
import mega.privacy.android.app.lollipop.adapters.SharesPageAdapter;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.listeners.ContactNameListener;
import mega.privacy.android.app.lollipop.listeners.FabButtonListener;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.lollipop.megachat.RecentChatsFragmentLollipop;
import mega.privacy.android.app.lollipop.tasks.CheckOfflineNodesTask;
import mega.privacy.android.app.lollipop.tasks.FilePrepareTask;
import mega.privacy.android.app.lollipop.tasks.FillDBContactsTask;
import mega.privacy.android.app.modalbottomsheet.ChatBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.ContactsBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.MyAccountBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.OfflineOptionsBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.ReceivedRequestBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.SentRequestBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.UploadBottomSheetDialogFragment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.billing.IabHelper;
import mega.privacy.android.app.utils.billing.IabResult;
import mega.privacy.android.app.utils.billing.Inventory;
import mega.privacy.android.app.utils.billing.Purchase;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatListenerInterface;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUtilsAndroid;

public class ManagerActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface, MegaChatListenerInterface, MegaChatRequestListenerInterface, OnNavigationItemSelectedListener, MegaGlobalListenerInterface, MegaTransferListenerInterface, OnClickListener, DatePickerDialog.OnDateSetListener {

	public int accountFragment;

	Button expiryDateButton;
	SwitchCompat switchGetLink;

	long totalSizeToDownload=0;
	long totalSizeDownloaded=0;
	private SparseArray<Long> transfersDownloadedSize;
	int progressPercent = 0;

	//OVERQUOTA WARNING
	LinearLayout outSpaceLayout=null;
	TextView outSpaceTextFirst;
	TextView outSpaceTextSecond;
	TextView outSpaceButtonUpgrade;
	TextView outSpaceButtonCancel;

	//GET PRO ACCOUNT PANEL
	LinearLayout getProLayout=null;
	TextView getProText;
	TextView leftCancelButton;
	TextView rightUpgradeButton;
	FloatingActionButton fabButton;

	boolean mkLayoutVisible = false;

	int statusToConnect = -1;

	NodeController nC;
	ContactController cC;
	AccountController aC;
	MyAccountInfo myAccountInfo;

	MegaNode selectedNode;
	MegaOffline selectedOfflineNode;
	MegaUser selectedUser;
	MegaContactRequest selectedRequest;
	MegaChatListItem selectedChatItem;
//	String fullNameChat;

	//COLLECTION FAB BUTTONS
	CoordinatorLayout fabButtonsLayout;
	FloatingActionButton mainFabButtonChat;
	FloatingActionButton firstFabButtonChat;
	FloatingActionButton secondFabButtonChat;
	FloatingActionButton thirdFabButtonChat;
	private Animation openFabAnim,closeFabAnim,rotateLeftAnim,rotateRightAnim, collectionFABLayoutOut;
	boolean isFabOpen=false;
	//

	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;
	ChatSettings chatSettings = null;
	MegaAttributes attr = null;
	static ManagerActivityLollipop managerActivity = null;
	MegaApplication app = null;
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	Handler handler;
    ArrayList<MegaTransfer> tL;
	DisplayMetrics outMetrics;
    float scaleText;
    FrameLayout fragmentContainer;
//	boolean tranfersPaused = false;
    Toolbar tB;
    ActionBar aB;

	int selectedPaymentMethod;
	int selectedAccountType;
	int displayedAccountType;

	int countUserAttributes=0;
	int errorUserAttibutes=0;

	boolean firstNavigationLevel = true;
    DrawerLayout drawerLayout;

	public enum DrawerItem {
		CLOUD_DRIVE, SAVED_FOR_OFFLINE, CAMERA_UPLOADS, INBOX, SHARED_ITEMS, CONTACTS, SETTINGS, ACCOUNT, SEARCH, TRANSFERS, MEDIA_UPLOADS, CHAT;

		public String getTitle(Context context) {
			switch(this)
			{
				case CLOUD_DRIVE: return context.getString(R.string.section_cloud_drive);
				case SAVED_FOR_OFFLINE: return context.getString(R.string.section_saved_for_offline);
				case CAMERA_UPLOADS: return context.getString(R.string.section_photo_sync);
				case INBOX: return context.getString(R.string.section_inbox);
				case SHARED_ITEMS: return context.getString(R.string.section_shared_items);
				case CONTACTS: return context.getString(R.string.section_contacts);
				case SETTINGS: return context.getString(R.string.action_settings);
				case ACCOUNT: return context.getString(R.string.section_account);
				case SEARCH: return context.getString(R.string.action_search);
				case TRANSFERS: return context.getString(R.string.section_transfers);
				case MEDIA_UPLOADS: return context.getString(R.string.section_secondary_media_uploads);
				case CHAT: return context.getString(R.string.section_chat);
			}
			return null;
		}
	}

	static DrawerItem drawerItem = null;
	static DrawerItem lastDrawerItem = null;
	static MenuItem drawerMenuItem = null;
	NavigationView nV;
	RelativeLayout usedSpaceLayout;
	FrameLayout accountInfoFrame;
	TextView nVDisplayName;
	TextView nVEmail;
	RoundedImageView nVPictureProfile;
	TextView nVPictureProfileTextView;
	TextView usedSpaceTV;
	TextView totalSpaceTV;
	ProgressBar usedSpacePB;

    //Tabs in Shares
	TabLayout tabLayoutShares;
	LinearLayout sharesSectionLayout;
	SharesPageAdapter mTabsAdapterShares;
    ViewPager viewPagerShares;

    //Tabs in Cloud
	TabLayout tabLayoutCloud;
	LinearLayout cloudSectionLayout;
	CloudDrivePagerAdapter mTabsAdapterCDrive;
    ViewPager viewPagerCDrive;

	//Tabs in Contacts
	TabLayout tabLayoutContacts;
	LinearLayout contactsSectionLayout;
	ContactsPageAdapter mTabsAdapterContacts;
	ViewPager viewPagerContacts;

	//Tabs in My Account
	TabLayout tabLayoutMyAccount;
	LinearLayout myAccountSectionLayout;
	MyAccountPageAdapter mTabsAdapterMyAccount;
	ViewPager viewPagerMyAccount;

	boolean firstTime = true;
//	String pathNavigation = "/";
	String searchQuery = null;
	boolean isSearching = false;
	ArrayList<MegaNode> searchNodes;
	int levelsSearch = -1;
	boolean openLink = false;
	boolean sendToInbox = false;
//	long handleToDownload=0;
	long lastTimeOnTransferUpdate = -1;

	private int orderCloud = MegaApiJava.ORDER_DEFAULT_ASC;
	private int orderContacts = MegaApiJava.ORDER_DEFAULT_ASC;
	private int orderOthers = MegaApiJava.ORDER_DEFAULT_ASC;
//	private int orderOffline = MegaApiJava.ORDER_DEFAULT_ASC;
//	private int orderOutgoing = MegaApiJava.ORDER_DEFAULT_ASC;
//	private int orderIncoming = MegaApiJava.ORDER_DEFAULT_ASC;

	boolean firstTimeCam = false;
	private boolean isGetLink = false;
	private boolean isExpiredDateLink = false;
	private boolean isClearRubbishBin = false;
	private boolean moveToRubbish = false;

	private List<ShareInfo> filePreparedInfos;
	boolean megaContacts = true;
	String feedback;

//	private boolean isListCloudDrive = true;
//	private boolean isListOffline = true;
//	private boolean isListRubbishBin = true;
	private boolean isListCameraUploads = false;
	private boolean isLargeGridCameraUploads = true;
//	private boolean isListInbox = true;
//	private boolean isListContacts = true;
//	private boolean isListIncoming = true;
//	private boolean isListOutgoing = true;

	private boolean isList = true;

	long parentHandleBrowser;
	long parentHandleRubbish;
	long parentHandleIncoming;
	long parentHandleOutgoing;
	long parentHandleSearch;
	long parentHandleInbox;
	String pathNavigationOffline;
	int deepBrowserTreeIncoming;
	int deepBrowserTreeOutgoing;
	int indexShares = -1;
	int indexCloud = -1;
	int indexContacts = -1;
//	int indexChat = -1;
	int indexAccount = -1;

	//LOLLIPOP FRAGMENTS
    private FileBrowserFragmentLollipop fbFLol;
    private RubbishBinFragmentLollipop rbFLol;
    private OfflineFragmentLollipop oFLol;
    private InboxFragmentLollipop iFLol;
    private IncomingSharesFragmentLollipop inSFLol;
	private OutgoingSharesFragmentLollipop outSFLol;
	private ContactsFragmentLollipop cFLol;
	private ReceivedRequestsFragmentLollipop rRFLol;
	private SentRequestsFragmentLollipop sRFLol;
	private MyAccountFragmentLollipop maFLol;
	private MyStorageFragmentLollipop mStorageFLol;
	private TransfersFragmentLollipop tFLol;
	private SearchFragmentLollipop sFLol;
	private SettingsFragmentLollipop sttFLol;
	private CameraUploadFragmentLollipop muFLol;
	private UpgradeAccountFragmentLollipop upAFL;
	private MonthlyAnnualyFragmentLollipop myFL;
	private FortumoFragmentLollipop fFL;
	private CentiliFragmentLollipop ctFL;
	private CreditCardFragmentLollipop ccFL;
	private CameraUploadFragmentLollipop cuFL;

	private RecentChatsFragmentLollipop rChatFL;

	ProgressDialog statusDialog;

	private AlertDialog renameDialog;
	private AlertDialog newFolderDialog;
	private AlertDialog addContactDialog;
	private AlertDialog overquotaDialog;
	private AlertDialog permissionsDialog;
	private AlertDialog openLinkDialog;
	private AlertDialog alertNotPermissionsUpload;
	private AlertDialog clearRubbishBinDialog;
	private AlertDialog downloadConfirmationDialog;
	private AlertDialog insertPassDialog;
	private AlertDialog changeUserAttributeDialog;
	private AlertDialog getLinkDialog;

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
	private MenuItem rubbishBinMenuItem;
	private MenuItem clearRubbishBinMenuitem;
	private MenuItem changePass;
	private MenuItem exportMK;
	private MenuItem removeMK;
	private MenuItem takePicture;
	private MenuItem cancelSubscription;
	private MenuItem killAllSessions;
	private MenuItem cancelAllTransfersMenuItem;
	private MenuItem playTransfersMenuIcon;
	private MenuItem pauseTransfersMenuIcon;
	private MenuItem logoutMenuItem;
	private MenuItem forgotPassMenuItem;
	private MenuItem newChatMenuItem;

	boolean fromTakePicture = false;

	//Billing

	// (arbitrary) request code for the purchase flow
    public static final int RC_REQUEST = 10001;
    String orderId = "";

	IabHelper mHelper;
	// SKU for our subscription PRO_I monthly
    static final String SKU_PRO_I_MONTH = "mega.android.pro1.onemonth";
    // SKU for our subscription PRO_I yearly
    static final String SKU_PRO_I_YEAR = "mega.android.pro1.oneyear";
    // SKU for our subscription PRO_II monthly
    static final String SKU_PRO_II_MONTH = "mega.android.pro2.onemonth";
    // SKU for our subscription PRO_II yearly
    static final String SKU_PRO_II_YEAR = "mega.android.pro2.oneyear";
    // SKU for our subscription PRO_III monthly
    static final String SKU_PRO_III_MONTH = "mega.android.pro3.onemonth";
    // SKU for our subscription PRO_III yearly
    static final String SKU_PRO_III_YEAR = "mega.android.pro3.oneyear";
    // SKU for our subscription PRO_LITE monthly
    static final String SKU_PRO_LITE_MONTH = "mega.android.prolite.onemonth";
    // SKU for our subscription PRO_LITE yearly
    static final String SKU_PRO_LITE_YEAR = "mega.android.prolite.oneyear";

    Purchase proLiteMonthly;
    Purchase proLiteYearly;
    Purchase proIMonthly;
    Purchase proIYearly;
    Purchase proIIMonthly;
    Purchase proIIYearly;
    Purchase proIIIMonthly;
    Purchase proIIIYearly;
    Purchase maxP;

 // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            log("Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                log("Error purchasing: " + result);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                log("Error purchasing. Authenticity verification failed.");
                return;
            }

            log("Purchase successful.");
            log("ORIGINAL JSON: ****_____" + purchase.getOriginalJson() + "____****");

            orderId = purchase.getOrderId();
//            Toast.makeText(getApplicationContext(), "ORDERID WHEN FINISHED: ****_____" + purchase.getOrderId() + "____*****", Toast.LENGTH_LONG).show();
            log("ORDERID WHEN FINISHED: ***____" + purchase.getOrderId() + "___***");
            if (purchase.getSku().equals(SKU_PRO_I_MONTH)) {
                log("PRO I Monthly subscription purchased.");
				if (managerActivity != null){
					Util.showAlert(managerActivity, "Thank you for subscribing to PRO I Monthly!", null);
				}
            }
            else if (purchase.getSku().equals(SKU_PRO_I_YEAR)) {
                log("PRO I Yearly subscription purchased.");
				if (managerActivity != null){
					Util.showAlert(managerActivity, "Thank you for subscribing to PRO I Yearly!", null);
				}
            }
            else if (purchase.getSku().equals(SKU_PRO_II_MONTH)) {
                log("PRO II Monthly subscription purchased.");
				if (managerActivity != null){
					Util.showAlert(managerActivity, "Thank you for subscribing to PRO II Monthly!", null);
				}
            }
            else if (purchase.getSku().equals(SKU_PRO_II_YEAR)) {
                log("PRO II Yearly subscription purchased.");
				if (managerActivity != null){
					Util.showAlert(managerActivity, "Thank you for subscribing to PRO II Yearly!", null);
				}
            }
            else if (purchase.getSku().equals(SKU_PRO_III_MONTH)) {
                log("PRO III Monthly subscription purchased.");
				if (managerActivity != null){
					Util.showAlert(managerActivity, "Thank you for subscribing to PRO III Monthly!", null);
				}
            }
            else if (purchase.getSku().equals(SKU_PRO_III_YEAR)) {
                log("PRO III Yearly subscription purchased.");
				if (managerActivity != null){
					Util.showAlert(managerActivity, "Thank you for subscribing to PRO III Yearly!", null);
				}
            }
            else if (purchase.getSku().equals(SKU_PRO_LITE_MONTH)) {
                log("PRO LITE Monthly subscription purchased.");
				if (managerActivity != null){
					Util.showAlert(managerActivity, "Thank you for subscribing to PRO LITE Monthly!", null);
				}
            }
            else if (purchase.getSku().equals(SKU_PRO_LITE_YEAR)) {
                log("PRO LITE Yearly subscription purchased.");
				if (managerActivity != null){
					Util.showAlert(managerActivity, "Thank you for subscribing to PRO LITE Yearly!", null);
				}
            }

            if (managerActivity != null){
            	log("ORIGINAL JSON3:" + purchase.getOriginalJson() + ":::");
            	megaApi.submitPurchaseReceipt(purchase.getOriginalJson(), managerActivity);
            }
            else{
            	log("ORIGINAL JSON4:" + purchase.getOriginalJson() + ":::");
            	megaApi.submitPurchaseReceipt(purchase.getOriginalJson());
            }
        }
    };

    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }
	// Listener that's called when we finish querying the items and subscriptions we own
	IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
			log("Query inventory finished.");

			// Have we been disposed of in the meantime? If so, quit.
			if (mHelper == null) return;

			// Is it a failure?
			if (result.isFailure()) {
				log("Failed to query inventory: " + result);
				return;
			}

			log("Query inventory was successful.");

			proLiteMonthly = inventory.getPurchase(SKU_PRO_LITE_MONTH);
			proLiteYearly = inventory.getPurchase(SKU_PRO_LITE_YEAR);
			proIMonthly = inventory.getPurchase(SKU_PRO_I_MONTH);
			proIYearly = inventory.getPurchase(SKU_PRO_I_YEAR);
			proIIMonthly = inventory.getPurchase(SKU_PRO_II_MONTH);
			proIIYearly = inventory.getPurchase(SKU_PRO_II_YEAR);
			proIIIMonthly = inventory.getPurchase(SKU_PRO_III_MONTH);
			proIIIYearly = inventory.getPurchase(SKU_PRO_III_YEAR);

			if (proLiteMonthly != null){
//            	if (megaApi.getMyUser().getEmail() != null){
//	        		if (proLiteMonthly.getDeveloperPayload().compareTo(megaApi.getMyUser().getEmail()) == 0){
				myAccountInfo.setLevelInventory(0);
				myAccountInfo.setProLiteMonthly(proLiteMonthly);
				maxP = proLiteMonthly;
//	        		}
//            	}
				log("PRO LITE MONTHLY (JSON): __*" + proLiteMonthly.getOriginalJson() + "*__");
			}

			if (proLiteYearly != null){
//            	if (megaApi.getMyUser().getEmail() != null){
//	            	if (proLiteYearly.getDeveloperPayload().compareTo(megaApi.getMyUser().getEmail()) == 0){
				myAccountInfo.setLevelInventory(0);
				myAccountInfo.setProLiteYearly(proLiteYearly);
				maxP = proLiteYearly;
//	        		}
//            	}
				log("PRO LITE ANNUALY (JSON): __*" + proLiteYearly.getOriginalJson() + "*__");
			}

			if (proIMonthly != null){
//            	if (megaApi.getMyUser().getEmail() != null){
//	            	if (proIMonthly.getDeveloperPayload().compareTo(megaApi.getMyUser().getEmail()) == 0){
				myAccountInfo.setLevelInventory(1);
				myAccountInfo.setProIMonthly(proIMonthly);
				maxP = proIMonthly;
//	        		}
//            	}
				log("PRO I MONTHLY (JSON): __*" + proIMonthly.getOriginalJson() + "*__");
			}

			if (proIYearly != null){
//            	if (megaApi.getMyUser().getEmail() != null){
//	            	if (proIYearly.getDeveloperPayload().compareTo(megaApi.getMyUser().getEmail()) == 0){
				myAccountInfo.setLevelInventory(1);
				myAccountInfo.setProIYearly(proIYearly);
				maxP = proIYearly;
//	        		}
//            	}
				log("PRO I ANNUALY (JSON): __*" + proIYearly.getOriginalJson() + "*__");
			}

			if (proIIMonthly != null){
//            	if (megaApi.getMyUser().getEmail() != null){
//	            	if (proIIMonthly.getDeveloperPayload().compareTo(megaApi.getMyUser().getEmail()) == 0){
				myAccountInfo.setLevelInventory(2);
				myAccountInfo.setProIIMonthly(proIIMonthly);
				maxP = proIIMonthly;
//	        		}
//            	}
				log("PRO II MONTHLY (JSON): __*" + proIIMonthly.getOriginalJson() + "*__");
			}

			if (proIIYearly != null){
//            	if (megaApi.getMyUser().getEmail() != null){
//	            	if (proIIYearly.getDeveloperPayload().compareTo(megaApi.getMyUser().getEmail()) == 0){
				myAccountInfo.setLevelInventory(2);
				myAccountInfo.setProIIYearly(proIIYearly);
				maxP = proIIYearly;
//	        		}
//            	}
				log("PRO II ANNUALY (JSON): __*" + proIIYearly.getOriginalJson() + "*__");
			}

			if (proIIIMonthly != null){
//            	if (megaApi.getMyUser().getEmail() != null){
//	            	if (proIIIMonthly.getDeveloperPayload().compareTo(megaApi.getMyUser().getEmail()) == 0){
				myAccountInfo.setLevelInventory(3);
				maxP = proIIIMonthly;
				myAccountInfo.setProIIIMonthly(proIIIMonthly);
//	        		}
//            	}
				log("PRO III MONTHLY (JSON): __*" + proIIIMonthly.getOriginalJson() + "*__");
			}

			if (proIIIYearly != null){
//            	if (megaApi.getMyUser().getEmail() != null){
//	            	if (proIIIYearly.getDeveloperPayload().compareTo(megaApi.getMyUser().getEmail()) == 0){
				myAccountInfo.setLevelInventory(3);
				myAccountInfo.setProIIIYearly(proIIIYearly);
				maxP = proIIIYearly;
//	        		}
//            	}
				log("PRO III ANNUALY (JSON): __*" + proIIIYearly.getOriginalJson() + "*__");
			}

			myAccountInfo.setInventoryFinished(true);

			log("LEVELACCOUNTDETAILS: " + myAccountInfo.getLevelAccountDetails() + "; LEVELINVENTORY: " + myAccountInfo.getLevelInventory() + "; ACCOUNTDETAILSFINISHED: " + myAccountInfo.isAccountDetailsFinished());

			if (myAccountInfo.isAccountDetailsFinished()){
				if (myAccountInfo.getLevelInventory() > myAccountInfo.getLevelAccountDetails()){
					if (maxP != null){
						log("ORIGINAL JSON1:" + maxP.getOriginalJson() + ":::");
						megaApi.submitPurchaseReceipt(maxP.getOriginalJson(), managerActivity);
					}
				}
			}

			boolean isProLiteMonthly = false;
			if (proLiteMonthly != null){
				isProLiteMonthly = true;
			}
			if (isProLiteMonthly){
				log("PRO LITE IS SUBSCRIPTED: ORDERID: ***____" + proLiteMonthly.getOrderId() + "____*****");
			}
			else{
				log("PRO LITE IS NOT SUBSCRIPTED");
			}

			if (!mHelper.subscriptionsSupported()) {
				log("SUBSCRIPTIONS NOT SUPPORTED");
			}
			else{
				log("SUBSCRIPTIONS SUPPORTED");
			}


			log("Initial inventory query finished.");
		}
	};

    void launchPayment(String productId){
    	/* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */
    	String payload = megaApi.getMyUser().getEmail();

    	if (mHelper == null){
    		initGooglePlayPayments();
    	}

    	if (productId.compareTo(SKU_PRO_I_MONTH) == 0){
    		mHelper.launchPurchaseFlow(this,
        			SKU_PRO_I_MONTH, IabHelper.ITEM_TYPE_SUBS,
                    RC_REQUEST, mPurchaseFinishedListener, payload);
    	}
    	else if (productId.compareTo(SKU_PRO_I_YEAR) == 0){
    		mHelper.launchPurchaseFlow(this,
    				SKU_PRO_I_YEAR, IabHelper.ITEM_TYPE_SUBS,
                    RC_REQUEST, mPurchaseFinishedListener, payload);
    	}
    	else if (productId.compareTo(SKU_PRO_II_MONTH) == 0){
    		mHelper.launchPurchaseFlow(this,
    				SKU_PRO_II_MONTH, IabHelper.ITEM_TYPE_SUBS,
                    RC_REQUEST, mPurchaseFinishedListener, payload);
    	}
    	else if (productId.compareTo(SKU_PRO_II_YEAR) == 0){
    		mHelper.launchPurchaseFlow(this,
    				SKU_PRO_II_YEAR, IabHelper.ITEM_TYPE_SUBS,
                    RC_REQUEST, mPurchaseFinishedListener, payload);
    	}
    	else if (productId.compareTo(SKU_PRO_III_MONTH) == 0){
    		mHelper.launchPurchaseFlow(this,
    				SKU_PRO_III_MONTH, IabHelper.ITEM_TYPE_SUBS,
                    RC_REQUEST, mPurchaseFinishedListener, payload);
    	}
    	else if (productId.compareTo(SKU_PRO_III_YEAR) == 0){
    		mHelper.launchPurchaseFlow(this,
    				SKU_PRO_III_YEAR, IabHelper.ITEM_TYPE_SUBS,
                    RC_REQUEST, mPurchaseFinishedListener, payload);
    	}
    	else if (productId.compareTo(SKU_PRO_LITE_MONTH) == 0){
    		log("LAUNCH PURCHASE FLOW!");
    		mHelper.launchPurchaseFlow(this,
    				SKU_PRO_LITE_MONTH, IabHelper.ITEM_TYPE_SUBS,
                    RC_REQUEST, mPurchaseFinishedListener, payload);
    	}
    	else if (productId.compareTo(SKU_PRO_LITE_YEAR) == 0){
    		mHelper.launchPurchaseFlow(this,
    				SKU_PRO_LITE_YEAR, IabHelper.ITEM_TYPE_SUBS,
                    RC_REQUEST, mPurchaseFinishedListener, payload);
    	}

    }

    public void initGooglePlayPayments(){
		String base64EncodedPublicKey = Util.base64EncodedPublicKey_1 + Util.base64EncodedPublicKey_2 + Util.base64EncodedPublicKey_3 + Util.base64EncodedPublicKey_4 + Util.base64EncodedPublicKey_5;

		log ("Creating IAB helper.");
		mHelper = new IabHelper(this, base64EncodedPublicKey);
		mHelper.enableDebugLogging(true);

		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                log("Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    log("Problem setting up in-app billing: " + result);
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                log("Setup successful. Querying inventory.");
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
	}

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		log("onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
			case Constants.REQUEST_READ_CONTACTS:{
				log("REQUEST_READ_CONTACTS");
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
					boolean hasReadContactsPermissions = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
					if (hasReadContactsPermissions){
						Intent phoneContactIntent = new Intent(this, PhoneContactsActivityLollipop.class);
						this.startActivity(phoneContactIntent);
					}
				}
				break;
			}
	        case Constants.REQUEST_CAMERA:{
//	        	if (firstTimeCam){
//	        		if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
//		        		boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
//		        		if (hasStoragePermission){
//		        			if (firstTimeCam){
//		        				firstTimeCam = false;
//		        			}
//		        		}
//		        		else{
//		        			ActivityCompat.requestPermissions(this,
//					                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//					                ManagerActivityLollipop.REQUEST_WRITE_STORAGE);
//		        		}
//		        	}
//	        	}


	        	if (fromTakePicture){
		        	if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
		        		boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
		        		if (!hasStoragePermission){
		        			ActivityCompat.requestPermissions(this,
					                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
									Constants.REQUEST_WRITE_STORAGE);
		        		}
		        		else{
		        			this.takePicture();
		        			fromTakePicture = false;
		        		}
		        	}
	        	}
	        	break;
	        }
	        case Constants.REQUEST_WRITE_STORAGE:{
	        	if (firstTimeCam){
	        		if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
//		        		boolean hasCameraPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
//		        		if (hasCameraPermission){
		        			if (firstTimeCam){
		        				firstTimeCam = false;
		        			}

		        			if (fromTakePicture){
		        				boolean hasCameraPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
				        		if (!hasCameraPermission){
				        			ActivityCompat.requestPermissions(this,
							                new String[]{Manifest.permission.CAMERA},
											Constants.REQUEST_CAMERA);
				        		}
				        		else{
				        			this.takePicture();
				        			fromTakePicture = false;
				        		}
		        			}
//		        		}
//		        		else{
//		        			ActivityCompat.requestPermissions(this,
//					                new String[]{Manifest.permission.CAMERA},
//					                ManagerActivityLollipop.REQUEST_CAMERA);
//		        		}
		        	}
	        	}
	        	break;
	        }
        }
//        switch (requestCode)
//        {
//            case REQUEST_WRITE_STORAGE: {
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
//                {
//                    //reload my activity with permission granted or use the features what required the permission
//                } else
//                {
//                    Toast.makeText(this, "The app was not allowed to write to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
//                }
//            }
//        }

    }

	@Override
	public void onSaveInstanceState(Bundle outState) {
		log("onSaveInstanceState");
		if (drawerItem != null){
			log("DrawerItem = " + drawerItem);
		}
		else{
			log("DrawerItem is null");
		}
		super.onSaveInstanceState(outState);
		int deepBrowserTreeIncoming = 0;
		int deepBrowserTreeOutgoing = 0;
		int indexShares = 0;
		int indexCloud = 0;
		int indexContacts = 0;
		outState.putLong("parentHandleBrowser", parentHandleBrowser);
		outState.putLong("parentHandleRubbish", parentHandleRubbish);
		outState.putLong("parentHandleIncoming", parentHandleIncoming);
		log("IN BUNDLE -> parentHandleOutgoing: "+parentHandleOutgoing);
		outState.putLong("parentHandleOutgoing", parentHandleOutgoing);
		outState.putLong("parentHandleSearch", parentHandleSearch);
		outState.putLong("parentHandleInbox", parentHandleInbox);
		outState.putSerializable("drawerItem", drawerItem);

		if(parentHandleIncoming!=-1){
			if(inSFLol!=null){
				deepBrowserTreeIncoming = inSFLol.getDeepBrowserTree();
			}
		}
		outState.putInt("deepBrowserTreeIncoming", deepBrowserTreeIncoming);

		if(parentHandleOutgoing!=-1){
			if(outSFLol!=null){
				deepBrowserTreeOutgoing = outSFLol.getDeepBrowserTree();
			}
		}
		outState.putInt("deepBrowserTreeOutgoing", deepBrowserTreeOutgoing);

		if (viewPagerShares != null) {
			indexShares = viewPagerShares.getCurrentItem();
		}
		outState.putInt("indexShares", indexShares);

		if (viewPagerCDrive != null) {
			indexCloud = viewPagerCDrive.getCurrentItem();
		}
		outState.putInt("indexCloud", indexCloud);

		if (viewPagerContacts != null) {
			indexContacts = viewPagerContacts.getCurrentItem();
		}
		outState.putInt("indexContacts", indexContacts);

		if(oFLol!=null){
			pathNavigationOffline = oFLol.getPathNavigation();
		}
		outState.putString("pathNavigationOffline", pathNavigationOffline);
//		outState.putParcelable("obj", myClass);
		if(drawerItem==DrawerItem.ACCOUNT){
			outState.putInt("accountFragment", accountFragment);
			if(accountFragment==Constants.MONTHLY_YEARLY_FRAGMENT){
				outState.putInt("selectedAccountType", selectedAccountType);
				outState.putInt("selectedPaymentMethod", selectedPaymentMethod);
			}
		}
		if(myAccountInfo==null){
			log("My AccountInfo is Null");
		}
	}
	@SuppressLint("NewApi") @Override
    protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
//		Fragments are restored during the Activity's onCreate().
//		Importantly though, they are restored in the base Activity class's onCreate().
//		Thus if you call super.onCreate() first, all of the rest of your onCreate() method will execute after your Fragments have been restored.
		super.onCreate(savedInstanceState);
		log("onCreate after call super");

		boolean selectDrawerItemPending = true;

		if(savedInstanceState!=null){
			log("Bundle is NOT NULL");
			parentHandleBrowser = savedInstanceState.getLong("parentHandleBrowser", -1);
			log("savedInstanceState -> parentHandleBrowser: "+parentHandleBrowser);
			parentHandleRubbish = savedInstanceState.getLong("parentHandleRubbish", -1);
			parentHandleIncoming = savedInstanceState.getLong("parentHandleIncoming", -1);
			parentHandleOutgoing = savedInstanceState.getLong("parentHandleOutgoing", -1);
			log("savedInstanceState -> parentHandleOutgoing: "+parentHandleOutgoing);
			parentHandleSearch = savedInstanceState.getLong("parentHandleSearch", -1);
			parentHandleInbox = savedInstanceState.getLong("parentHandleInbox", -1);
			deepBrowserTreeIncoming = savedInstanceState.getInt("deepBrowserTreeIncoming", deepBrowserTreeIncoming);
			deepBrowserTreeOutgoing = savedInstanceState.getInt("deepBrowserTreeOutgoing", deepBrowserTreeOutgoing);
			drawerItem = (DrawerItem) savedInstanceState.getSerializable("drawerItem");
			log("DrawerItem onCreate = " + drawerItem);
			log("savedInstanceState -> drawerItem: "+drawerItem);
			indexShares = savedInstanceState.getInt("indexShares", indexShares);
			log("savedInstanceState -> indexShares: "+indexShares);
			indexCloud = savedInstanceState.getInt("indexCloud", indexCloud);
			log("savedInstanceState -> indexCloud: "+indexCloud);
			indexContacts = savedInstanceState.getInt("indexContacts", indexContacts);
			pathNavigationOffline = savedInstanceState.getString("pathNavigationOffline", pathNavigationOffline);
			log("savedInstanceState -> pathNavigationOffline: "+pathNavigationOffline);
			accountFragment = savedInstanceState.getInt("accountFragment", -1);
			selectedAccountType = savedInstanceState.getInt("selectedAccountType", -1);
			selectedPaymentMethod = savedInstanceState.getInt("selectedPaymentMethod", -1);
		}
		else{
			log("Bundle is NULL");
			parentHandleBrowser = -1;
			parentHandleRubbish = -1;
			parentHandleIncoming = -1;
			parentHandleOutgoing = -1;
			parentHandleSearch = -1;
			parentHandleInbox = -1;

			this.setPathNavigationOffline("/");
		}

		nC = new NodeController(this);
		cC = new ContactController(this);
		aC = new AccountController(this);
		myAccountInfo = new MyAccountInfo(this);

		File thumbDir;
		if (getExternalCacheDir() != null){
			thumbDir = new File (getExternalCacheDir(), "thumbnailsMEGA");
			thumbDir.mkdirs();
			log("------------------ThumbnailsMEGA folder created: "+thumbDir.getAbsolutePath());
		}
		else{
			thumbDir = getDir("thumbnailsMEGA", 0);
		}
		File previewDir;
		if (getExternalCacheDir() != null){
			previewDir = new File (getExternalCacheDir(), "previewsMEGA");
			previewDir.mkdirs();
		}
		else{
			previewDir = getDir("previewsMEGA", 0);
		}

		dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		if (Util.isOnline(this)){
			dbH.setAttrOnline(true);
		}
		else{
			dbH.setAttrOnline(false);
		}
		managerActivity = this;
		app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		if(Util.isChatEnabled()){
			megaChatApi = app.getMegaChatApi();
			log("addChatListener");
			megaChatApi.addChatListener(this);
		}
		else{
			megaChatApi=null;
		}

		log("retryPendingConnections()");
		if (megaApi != null){
			log("---------retryPendingConnections");
			megaApi.retryPendingConnections();
		}

		transfersDownloadedSize = new SparseArray<Long>();

		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;

	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
	    if (scaleH < scaleW){
	    	scaleText = scaleH;
	    }
	    else{
	    	scaleText = scaleW;
	    }

	    if (dbH.getCredentials() == null){
	    	if (OldPreferences.getOldCredentials(this) != null){
	    		Intent loginWithOldCredentials = new Intent(this, LoginActivityLollipop.class);
				loginWithOldCredentials.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
	    		startActivity(loginWithOldCredentials);
	    		finish();
	    		return;
		    }

	    	Intent newIntent = getIntent();

	    	if (newIntent != null){
		    	if (newIntent.getAction() != null){
		    		if (newIntent.getAction().equals(Constants.ACTION_EXPORT_MASTER_KEY) || newIntent.getAction().equals(Constants.ACTION_OPEN_MEGA_LINK) || newIntent.getAction().equals(Constants.ACTION_OPEN_MEGA_FOLDER_LINK)){
		    			openLink = true;
		    		}
		    		else if (newIntent.getAction().equals(Constants.ACTION_CANCEL_UPLOAD) || newIntent.getAction().equals(Constants.ACTION_CANCEL_DOWNLOAD) || newIntent.getAction().equals(Constants.ACTION_CANCEL_CAM_SYNC)){
						Intent cancelTourIntent = new Intent(this, LoginActivityLollipop.class);
						cancelTourIntent.putExtra("visibleFragment", Constants. TOUR_FRAGMENT);
						cancelTourIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    			cancelTourIntent.setAction(newIntent.getAction());
		    			startActivity(cancelTourIntent);
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
				intent.putExtra("visibleFragment", Constants. TOUR_FRAGMENT);
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
			firstTime = true;
			isList=true;
			isListCameraUploads=false;
		}
		else{
			if (prefs.getFirstTime() == null){
				firstTime = true;
				isListCameraUploads=false;
			}
			else{
				firstTime = Boolean.parseBoolean(prefs.getFirstTime());
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
		}
		log("Preferred View List: "+isList);

		if(prefs!=null){
			if(prefs.getPreferredSortCloud()!=null){
				orderCloud = Integer.parseInt(prefs.getPreferredSortCloud());
				log("The orderCloud preference is: "+orderCloud);
			}
			else{
				orderCloud = megaApi.ORDER_DEFAULT_ASC;
				log("Preference orderCloud is NULL -> ORDER_DEFAULT_ASC");
			}
			if(prefs.getPreferredSortContacts()!=null){
				orderContacts = Integer.parseInt(prefs.getPreferredSortContacts());
				log("The orderContacts preference is: "+orderContacts);
			}
			else{
				orderContacts = megaApi.ORDER_DEFAULT_ASC;
				log("Preference orderContacts is NULL -> ORDER_DEFAULT_ASC");
			}
			if(prefs.getPreferredSortOthers()!=null){
				orderOthers = Integer.parseInt(prefs.getPreferredSortOthers());
				log("The orderOthers preference is: "+orderOthers);
			}
			else{
				orderOthers = megaApi.ORDER_DEFAULT_ASC;
				log("Preference orderOthers is NULL -> ORDER_DEFAULT_ASC");
			}
		}
		else {
			log("Prefs is NULL -> ORDER_DEFAULT_ASC");
			orderCloud = megaApi.ORDER_DEFAULT_ASC;
			orderContacts = megaApi.ORDER_DEFAULT_ASC;
			orderOthers = megaApi.ORDER_DEFAULT_ASC;
		}
		getOverflowMenu();

		handler = new Handler();

		log("Set view");
		setContentView(R.layout.activity_manager);
//		long num = 11179220468180L;
//		dbH.setSecondaryFolderHandle(num);
		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar);
		if(tB==null){
			log("Tb is Null");
			return;
		}

		tB.setVisibility(View.VISIBLE);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		log("aB.setHomeAsUpIndicator_1");
        aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
//        aB.setDisplayShowHomeEnabled(true);
//        getSupportActionBar().setCustomView(R.layout.custom_action_bar_top);

        //Set navigation view
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        nV = (NavigationView) findViewById(R.id.navigation_view);
        nV.setNavigationItemSelectedListener(this);

		usedSpaceLayout = (RelativeLayout) findViewById(R.id.nv_used_space_layout);

		View nVHeader = LayoutInflater.from(this).inflate(R.layout.nav_header, null);
		nV.addHeaderView(nVHeader);

		//FAB button
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
				log("onAnimationEnd");
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
				log("onAnimationEnd");
//				mainFabButtonChat.setVisibility(View.GONE);
//				fabButtonsLayout.startAnimation(collectionFABLayoutOut);
				fabButtonsLayout.setVisibility(View.GONE);
				fabButton.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});

		rotateRightAnim = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_right);
		rotateLeftAnim = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_left);

		//OVERQUOTA WARNING PANEL
		outSpaceLayout = (LinearLayout) findViewById(R.id.overquota_alert);
		outSpaceTextFirst =  (TextView) findViewById(R.id.overquota_alert_text_first);
		outSpaceTextSecond =  (TextView) findViewById(R.id.overquota_alert_text_second);
		outSpaceButtonUpgrade = (TextView) findViewById(R.id.overquota_alert_btnRight_upgrade);
		outSpaceButtonCancel = (TextView) findViewById(R.id.overquota_alert_btnLeft_cancel);

		//PRO PANEL
		getProLayout=(LinearLayout) findViewById(R.id.get_pro_account);
		getProText= (TextView) findViewById(R.id.get_pro_account_text);
		rightUpgradeButton = (TextView) findViewById(R.id.btnRight_upgrade);
		leftCancelButton = (TextView) findViewById(R.id.btnLeft_cancel);

		accountInfoFrame = (FrameLayout) nVHeader.findViewById(R.id.navigation_drawer_account_view);
        accountInfoFrame.setOnClickListener(this);

        nVDisplayName = (TextView) nVHeader.findViewById(R.id.navigation_drawer_account_information_display_name);
		nVEmail = (TextView) nVHeader.findViewById(R.id.navigation_drawer_account_information_email);
        nVPictureProfile = (RoundedImageView) nVHeader.findViewById(R.id.navigation_drawer_user_account_picture_profile);
        nVPictureProfileTextView = (TextView) nVHeader.findViewById(R.id.navigation_drawer_user_account_picture_profile_textview);

        fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container);
        usedSpaceTV = (TextView) findViewById(R.id.navigation_drawer_used_space);
        totalSpaceTV = (TextView) findViewById(R.id.navigation_drawer_total_space);
        usedSpacePB = (ProgressBar) findViewById(R.id.manager_used_space_bar);

		//TABS section Cloud Drive
		cloudSectionLayout= (LinearLayout)findViewById(R.id.tabhost_cloud_drive);
		tabLayoutCloud =  (TabLayout) findViewById(R.id.sliding_tabs_cloud_drive);
		viewPagerCDrive = (ViewPager) findViewById(R.id.cloud_drive_tabs_pager);

		//TABS section Contacts
		contactsSectionLayout= (LinearLayout)findViewById(R.id.tabhost_contacts);
		tabLayoutContacts =  (TabLayout) findViewById(R.id.sliding_tabs_contacts);
		viewPagerContacts = (ViewPager) findViewById(R.id.contact_tabs_pager);

		//TABS section Shared Items
		sharesSectionLayout= (LinearLayout)findViewById(R.id.tabhost_shares);
		tabLayoutShares =  (TabLayout) findViewById(R.id.sliding_tabs_shares);
		viewPagerShares = (ViewPager) findViewById(R.id.shares_tabs_pager);

		//Tab section MyAccount
		myAccountSectionLayout= (LinearLayout)findViewById(R.id.tabhost_my_account);
		tabLayoutMyAccount =  (TabLayout) findViewById(R.id.sliding_tabs_my_account);
		viewPagerMyAccount = (ViewPager) findViewById(R.id.my_account_tabs_pager);

        if (!Util.isOnline(this)){
        	log("No network: intent to OfflineActivityLollipop");
//        	Intent offlineIntent = new Intent(this, OfflineActivityLollipop.class);
//			startActivity(offlineIntent);
//			finish();
			showOfflineMode();
			if(Util.isChatEnabled()){
				UserCredentials credentials = dbH.getCredentials();
				if(credentials!=null){
					String gSession = credentials.getSession();
					int ret = megaChatApi.init(gSession);
					log("In Offline mode: init chat is: "+ret);
				}
			}
        	return;
        }

        dbH.setAttrOnline(true);

		///Check the MK file
		int versionApp = Util.getVersion(this);
		log("-------------------Version app: "+versionApp);
		final String pathOldMK = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.oldMKFile;
		final File fMKOld = new File(pathOldMK);
		if (fMKOld != null) {
			if (fMKOld.exists()) {
				log("Old MK file need to be renamed!");
				aC.renameMK();
			}
		}

        MegaNode rootNode = megaApi.getRootNode();
		if (rootNode == null){
			log("Root node is NULL");
			 if (getIntent() != null){
				if (getIntent().getAction() != null){
					if (getIntent().getAction().equals(Constants.ACTION_IMPORT_LINK_FETCH_NODES)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(Constants.ACTION_IMPORT_LINK_FETCH_NODES);
						intent.setData(Uri.parse(getIntent().getDataString()));
						startActivity(intent);
						finish();
						return;
					}
					else if (getIntent().getAction().equals(Constants.ACTION_OPEN_MEGA_LINK)){
						Intent intent = new Intent(managerActivity, FileLinkActivityLollipop.class);
						intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(Constants.ACTION_IMPORT_LINK_FETCH_NODES);
						intent.setData(Uri.parse(getIntent().getDataString()));
						startActivity(intent);
						finish();
						return;
					}
					else if (getIntent().getAction().equals(Constants.ACTION_OPEN_MEGA_FOLDER_LINK)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(Constants.ACTION_OPEN_MEGA_FOLDER_LINK);
						intent.setData(Uri.parse(getIntent().getDataString()));
						startActivity(intent);
						finish();
						return;
					}
					else if (getIntent().getAction().equals(Constants.ACTION_CANCEL_UPLOAD) || getIntent().getAction().equals(Constants.ACTION_CANCEL_DOWNLOAD) || getIntent().getAction().equals(Constants.ACTION_CANCEL_CAM_SYNC)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(getIntent().getAction());
						startActivity(intent);
						finish();
						return;
					}
					else if (getIntent().getAction().equals(Constants.ACTION_EXPORT_MASTER_KEY)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(getIntent().getAction());
						startActivity(intent);
						finish();
						return;
					}
				}
			}
			Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
			intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}
		else{
			log("rootNode != null");

			String token = FirebaseInstanceId.getInstance().getToken();
			if (token != null) {
				log("FCM TOKEN: " + token);
				megaApi.registerPushNotifications(Constants.DEVICE_ANDROID, token, this);
//				Log.d("TOKEN___", token);

//				Toast.makeText(this, "TOKEN: _" + token + "_", Toast.LENGTH_LONG).show();
			}

			if(myAccountInfo==null){
				myAccountInfo = new MyAccountInfo(this);
			}

			myAccountInfo.setMyUser(megaApi.getMyUser());
			if (myAccountInfo.getMyUser() != null){
				nVEmail.setVisibility(View.VISIBLE);
				nVEmail.setText(myAccountInfo.getMyUser().getEmail());
//				megaApi.getUserData(this);
				log("getUserAttribute FirstName");
				myAccountInfo.setFirstName(false);
				megaApi.getUserAttribute(MegaApiJava.USER_ATTR_FIRSTNAME, myAccountInfo);
				log("getUserAttribute LastName");
				myAccountInfo.setLastName(false);
				megaApi.getUserAttribute(MegaApiJava.USER_ATTR_LASTNAME, myAccountInfo);

				this.setDefaultAvatar();

				this.setProfileAvatar();
			}


			if(myAccountInfo==null){
				myAccountInfo=new MyAccountInfo(this);
			}
			megaApi.getPaymentMethods(myAccountInfo);
			megaApi.getAccountDetails(myAccountInfo);
			megaApi.getPricing(myAccountInfo);
			megaApi.creditCardQuerySubscriptions(myAccountInfo);
			dbH.resetExtendedAccountDetailsTimestamp();

			initGooglePlayPayments();

			megaApi.addGlobalListener(this);
			megaApi.addTransferListener(this);

			if(savedInstanceState==null) {
				log("Run async task to check offline files");
				//Check the consistency of the offline nodes in the DB
				CheckOfflineNodesTask checkOfflineNodesTask = new CheckOfflineNodesTask(this);
				checkOfflineNodesTask.execute();
			}

	        if (getIntent() != null){
				if (getIntent().getAction() != null){
			        if (getIntent().getAction().equals(Constants.ACTION_EXPORT_MASTER_KEY)){
			        	log("Intent to export Master Key - im logged in!");
						drawerItem=DrawerItem.ACCOUNT;
						mkLayoutVisible = true;
						selectDrawerItemLollipop(drawerItem);
						selectDrawerItemPending=false;
						return;
					}
					else if(getIntent().getAction().equals(Constants.ACTION_CANCEL_ACCOUNT)){
						String link = getIntent().getDataString();
						if(link!=null){
							log("link to cancel: "+link);
							drawerItem=DrawerItem.ACCOUNT;
							selectDrawerItemLollipop(drawerItem);
							selectDrawerItemPending=false;
							megaApi.queryCancelLink(link, this);
						}
					}
					else if(getIntent().getAction().equals(Constants.ACTION_CHANGE_MAIL)){
						String link = getIntent().getDataString();
						if(link!=null){
							log("link to change mail: "+link);
							drawerItem=DrawerItem.ACCOUNT;
							selectDrawerItemLollipop(drawerItem);
							selectDrawerItemPending=false;
							showDialogInsertPassword(link, false);
						}
					}
					else if (getIntent().getAction().equals(Constants.ACTION_OPEN_FOLDER)) {
						log("Open after LauncherFileExplorerActivityLollipop ");
						long handleIntent = getIntent().getLongExtra("PARENT_HANDLE", -1);
						int access = -1;
						if (handleIntent != -1) {
							MegaNode parentIntentN = megaApi.getNodeByHandle(handleIntent);
							if (parentIntentN != null) {
								access = megaApi.getAccess(parentIntentN);
								switch (access) {
									case MegaShare.ACCESS_OWNER:
									case MegaShare.ACCESS_UNKNOWN: {
										log("The intent set the parentHandleBrowser to " + handleIntent);
										parentHandleBrowser = handleIntent;
										break;
									}
									case MegaShare.ACCESS_READ:
									case MegaShare.ACCESS_READWRITE:
									case MegaShare.ACCESS_FULL: {
										log("The intent set the parentHandleIncoming to " + handleIntent);
										parentHandleIncoming = handleIntent;
										drawerItem = DrawerItem.SHARED_ITEMS;
										deepBrowserTreeIncoming = MegaApiUtils.calculateDeepBrowserTreeIncoming(parentIntentN, this);
										log("After calculate deepBrowserTreeIncoming: "+deepBrowserTreeIncoming);
										break;
									}
									default: {
										log("DEFAULT: The intent set the parentHandleBrowser to " + handleIntent);
										parentHandleBrowser = handleIntent;
										break;
									}
								}
							}
						}
					}
					else if(getIntent().getAction().equals(Constants.ACTION_PASS_CHANGED)){
						int result = getIntent().getIntExtra("RESULT",-20);
						if(result==0){
							drawerItem=DrawerItem.ACCOUNT;
							selectDrawerItemLollipop(drawerItem);
							selectDrawerItemPending=false;
							log("Show success mesage");
							Util.showAlert(this, getString(R.string.pass_changed_alert), null);
						}
						else if(result==MegaError.API_EARGS){
							drawerItem=DrawerItem.ACCOUNT;
							selectDrawerItemLollipop(drawerItem);
							selectDrawerItemPending=false;
							log("Error when changing pass - the current password is not correct");
							Util.showAlert(this,getString(R.string.old_password_provided_incorrect), getString(R.string.general_error_word));
						}
						else{
							drawerItem=DrawerItem.ACCOUNT;
							selectDrawerItemLollipop(drawerItem);
							selectDrawerItemPending=false;
							log("Error when changing pass - show error message");
							Util.showAlert(this,getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
						}
					}
					else if(getIntent().getAction().equals(Constants.ACTION_RESET_PASS)){
						String link = getIntent().getDataString();
						if(link!=null){
							log("link to resetPass: "+link);
							drawerItem=DrawerItem.ACCOUNT;
							selectDrawerItemLollipop(drawerItem);
							selectDrawerItemPending=false;
							showConfirmationResetPassword(link);
						}
					}
					else if(getIntent().getAction().equals(Constants.ACTION_IPC)){
						log("IPC link - go to received request in Contacts");
						drawerItem=DrawerItem.CONTACTS;
						indexContacts=2;
						selectDrawerItemLollipop(drawerItem);
						selectDrawerItemPending=false;
					}
					else if(getIntent().getAction().equals(Constants.ACTION_SHOW_MY_ACCOUNT)){
						log("intent from chat - show my account");
						drawerItem=DrawerItem.ACCOUNT;
						accountFragment=Constants.MY_ACCOUNT_FRAGMENT;
						selectDrawerItemLollipop(drawerItem);
						selectDrawerItemPending=false;
					}
				}
	        }

			log("onCreate - Check if there any unread chat");
			if(Util.isChatEnabled()){
				if (nV != null){
					Menu nVMenu = nV.getMenu();
					MenuItem chat = nVMenu.findItem(R.id.navigation_item_chat);
					int numberUnread = megaChatApi.getUnreadChats();
					if(numberUnread==0){
						chat.setTitle(getString(R.string.section_chat));
					}
					else{
						String textToShow = String.format(getString(R.string.section_chat_with_notification), numberUnread);
						Spanned result = null;
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
							result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
						} else {
							result = Html.fromHtml(textToShow);
						}
						chat.setTitle(result);
					}
				}
			}


			if (drawerItem == null) {
	        	log("DRAWERITEM NULL");
	        	drawerItem = DrawerItem.CLOUD_DRIVE;
	        	Intent intent = getIntent();
	        	if (intent != null){
	        		boolean upgradeAccount = getIntent().getBooleanExtra("upgradeAccount", false);
	        		if(upgradeAccount){
	        			log("upgradeAccount true");
	        			drawerLayout.closeDrawer(Gravity.LEFT);
						int accountType = getIntent().getIntExtra("accountType", 0);
						log("accountType: "+accountType);
						long paymentBitSetLong = getIntent().getLongExtra("paymentBitSetLong", 0);
						BitSet paymentBitSet = Util.convertToBitSet(paymentBitSetLong);;
						switch (accountType){
							case 0:{
								log("intent firstTime==true");
								firstTimeCam = true;
								drawerItem = DrawerItem.CAMERA_UPLOADS;
								setIntent(null);
								displayedAccountType = -1;
								return;
							}
							case Constants.PRO_I:{
								drawerItem = DrawerItem.ACCOUNT;
								accountFragment = Constants.UPGRADE_ACCOUNT_FRAGMENT;
								displayedAccountType = Constants.PRO_I;
								selectDrawerItemLollipop(drawerItem);
								selectDrawerItemPending=false;
								return;
							}
							case Constants.PRO_II:{
								drawerItem = DrawerItem.ACCOUNT;
								accountFragment = Constants.UPGRADE_ACCOUNT_FRAGMENT;
								selectDrawerItemPending=false;
								displayedAccountType = Constants.PRO_II;
								selectDrawerItemLollipop(drawerItem);
								return;
							}
							case Constants.PRO_III:{
								drawerItem = DrawerItem.ACCOUNT;
								accountFragment = Constants.UPGRADE_ACCOUNT_FRAGMENT;
								selectDrawerItemPending=false;
								displayedAccountType = Constants.PRO_III;
								selectDrawerItemLollipop(drawerItem);
								return;
							}
							case Constants.PRO_LITE:{
								drawerItem = DrawerItem.ACCOUNT;
								accountFragment = Constants.UPGRADE_ACCOUNT_FRAGMENT;
								selectDrawerItemPending=false;
								displayedAccountType = Constants.PRO_LITE;
								selectDrawerItemLollipop(drawerItem);
								return;
							}
						}
	        		}
	        		else{
						log("upgradeAccount false");
						firstTimeCam = getIntent().getBooleanExtra("firstTimeCam", false);
						if (firstTimeCam){
							log("intent firstTimeCam==true");
							firstTimeCam = true;
							drawerItem = DrawerItem.CAMERA_UPLOADS;
							setIntent(null);
						}
					}
	        	}
	        }
	        else{
	        	log("DRAWERITEM NOT NULL1: " + drawerItem);
				Intent intentRec = getIntent();
	        	if (intentRec != null){
					boolean upgradeAccount = getIntent().getBooleanExtra("upgradeAccount", false);
					firstTimeCam = intentRec.getBooleanExtra("firstTimeCam", false);
					if(upgradeAccount){
						log("upgradeAccount true");
						drawerLayout.closeDrawer(Gravity.LEFT);
						int accountType = getIntent().getIntExtra("accountType", 0);
						log("accountType: "+accountType);
						long paymentBitSetLong = getIntent().getLongExtra("paymentBitSetLong", 0);
						switch (accountType){
							case Constants.FREE:{
								log("intent firstTime==true");
								firstTimeCam = true;
								drawerItem = DrawerItem.CAMERA_UPLOADS;
								displayedAccountType = -1;
								setIntent(null);
								return;
							}
							case Constants.PRO_I:{
								drawerItem = DrawerItem.ACCOUNT;
								accountFragment = Constants.UPGRADE_ACCOUNT_FRAGMENT;
								selectDrawerItemPending=false;
								displayedAccountType = Constants.PRO_I;
								selectDrawerItemLollipop(drawerItem);
								return;
							}
							case Constants.PRO_II:{
								drawerItem = DrawerItem.ACCOUNT;
								accountFragment = Constants.UPGRADE_ACCOUNT_FRAGMENT;
								selectDrawerItemPending=false;
								displayedAccountType = Constants.PRO_II;
								selectDrawerItemLollipop(drawerItem);
								return;
							}
							case Constants.PRO_III:{
								drawerItem = DrawerItem.ACCOUNT;
								accountFragment = Constants.UPGRADE_ACCOUNT_FRAGMENT;
								selectDrawerItemPending=false;
								displayedAccountType = Constants.PRO_III;
								selectDrawerItemLollipop(drawerItem);
								return;
							}
							case Constants.PRO_LITE:{
								drawerItem = DrawerItem.ACCOUNT;
								accountFragment = Constants.UPGRADE_ACCOUNT_FRAGMENT;
								selectDrawerItemPending=false;
								displayedAccountType = Constants.PRO_LITE;
								selectDrawerItemLollipop(drawerItem);
								return;
							}
						}
					}
					else{
						log("upgradeAccount false");
						if (firstTimeCam) {
							log("intent firstTimeCam2==true");
							if (prefs != null){
								if (prefs.getCamSyncEnabled() != null){
									firstTimeCam = false;
								}
								else{
									firstTimeCam = true;
									drawerItem = DrawerItem.CAMERA_UPLOADS;
								}
							}
							else{
								firstTimeCam = true;
								drawerItem = DrawerItem.CAMERA_UPLOADS;
							}

							setIntent(null);
						}
					}

	        		if (intentRec.getAction() != null){
	        			if (intentRec.getAction().equals(Constants.ACTION_SHOW_TRANSFERS)){
	        				drawerItem = DrawerItem.TRANSFERS;
	        				setIntent(null);
	        			}
	        		}
	        	}
	        	log("DRAWERITEM NOT NULL2: " + drawerItem);
				drawerLayout.closeDrawer(Gravity.LEFT);
			}

	        //INITIAL FRAGMENT
			if(selectDrawerItemPending){
				selectDrawerItemLollipop(drawerItem);
			}
		}
		log("END onCreate");
	}

	@Override
	protected void onResume(){
		log("onResume");
		super.onResume();

		MegaApplication.activityResumed();
	}

	@Override
	protected void onPostResume() {
		log("onPostResume");
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
    			log("onPostResume: not credentials");
    			if (intent != null) {
    				log("onPostResume: not credentials -> INTENT");
    				if (intent.getAction() != null){
    					log("onPostResume: intent with ACTION: "+intent.getAction());
    					if (getIntent().getAction().equals(Constants.ACTION_EXPORT_MASTER_KEY)){
    						Intent exportIntent = new Intent(managerActivity, LoginActivityLollipop.class);
							intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
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
    		log("onPostResume: intent not null! "+intent.getAction());
    		// Open folder from the intent
			if (intent.hasExtra(Constants.EXTRA_OPEN_FOLDER)) {
				log("onPostResume: INTENT: EXTRA_OPEN_FOLDER");
				parentHandleBrowser = intent.getLongExtra(Constants.EXTRA_OPEN_FOLDER, -1);
				intent.removeExtra(Constants.EXTRA_OPEN_FOLDER);
				setIntent(null);
			}

    		if (intent.getAction() != null){
    			log("onPostResume: intent action");

    			if(getIntent().getAction().equals(Constants.ACTION_EXPLORE_ZIP)){
					log("onPostResume: open zip browser");
    				String pathZip=intent.getExtras().getString(Constants.EXTRA_PATH_ZIP);

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
    			if (getIntent().getAction().equals(Constants.ACTION_IMPORT_LINK_FETCH_NODES)){
					log("onPostResume: ACTION_IMPORT_LINK_FETCH_NODES");
					Intent loginIntent = new Intent(managerActivity, LoginActivityLollipop.class);
					intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
					loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					loginIntent.setAction(Constants.ACTION_IMPORT_LINK_FETCH_NODES);
					loginIntent.setData(Uri.parse(getIntent().getDataString()));
					startActivity(loginIntent);
					finish();
					return;
				}
				else if (getIntent().getAction().equals(Constants.ACTION_OPEN_MEGA_LINK)){
					log("onPostResume: ACTION_OPEN_MEGA_LINK");
					Intent fileLinkIntent = new Intent(managerActivity, FileLinkActivityLollipop.class);
					fileLinkIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					fileLinkIntent.setAction(Constants.ACTION_IMPORT_LINK_FETCH_NODES);
					String data = getIntent().getDataString();
					if(data!=null){
						fileLinkIntent.setData(Uri.parse(data));
						startActivity(fileLinkIntent);
					}
					else{
						log("onPostResume: getDataString is NULL");
					}
					finish();
					return;
				}
    			else if (intent.getAction().equals(Constants.ACTION_OPEN_MEGA_FOLDER_LINK)){
					log("onPostResume: ACTION_OPEN_MEGA_FOLDER_LINK");
    				Intent intentFolderLink = new Intent(managerActivity, FolderLinkActivityLollipop.class);
    				intentFolderLink.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    				intentFolderLink.setAction(Constants.ACTION_OPEN_MEGA_FOLDER_LINK);

					String data = getIntent().getDataString();
					if(data!=null){
						intentFolderLink.setData(Uri.parse(data));
						startActivity(intentFolderLink);
					}
					else{
						log("onPostResume: getDataString is NULL");
					}
					finish();
    			}
    			else if (intent.getAction().equals(Constants.ACTION_REFRESH_PARENTHANDLE_BROWSER)){

    				parentHandleBrowser = intent.getLongExtra("parentHandle", -1);
    				intent.removeExtra("parentHandle");
    				setParentHandleBrowser(parentHandleBrowser);

    				if (fbFLol != null){
						fbFLol.setParentHandle(parentHandleBrowser);
    					fbFLol.setIsList(isList);
    					fbFLol.setOrder(orderCloud);
    					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleBrowser), orderCloud);
    					fbFLol.setNodes(nodes);
    					if (!fbFLol.isVisible()){
    						getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fbFLol, "fbFLol").commitNow();
    					}
    				}
    				else{
    					fbFLol = new FileBrowserFragmentLollipop();
    					fbFLol.setParentHandle(parentHandleBrowser);
    					fbFLol.setIsList(isList);
    					fbFLol.setOrder(orderCloud);
    					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleBrowser), orderCloud);
    					fbFLol.setNodes(nodes);
    					if (!fbFLol.isVisible()){
    						getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fbFLol, "fbFLol").commitNow();
    					}
    				}
    			}
    			else if(intent.getAction().equals(Constants.ACTION_OVERQUOTA_ALERT)){
	    			showOverquotaAlert();
	    		}
				else if (intent.getAction().equals(Constants.ACTION_CHANGE_AVATAR)){
					log("onPostResume: Intent CHANGE AVATAR");
					String path = intent.getStringExtra("IMAGE_PATH");
					log("onPostResume: Path of the avatar: "+path);
					megaApi.setAvatar(path, this);
				}
    			else if (intent.getAction().equals(Constants.ACTION_CANCEL_UPLOAD) || intent.getAction().equals(Constants.ACTION_CANCEL_DOWNLOAD) || intent.getAction().equals(Constants.ACTION_CANCEL_CAM_SYNC)){
    				log("onPostResume: ACTION_CANCEL_UPLOAD or ACTION_CANCEL_DOWNLOAD or ACTION_CANCEL_CAM_SYNC");
					Intent tempIntent = null;
					String title = null;
					String text = null;
					if(intent.getAction().equals(Constants.ACTION_CANCEL_UPLOAD)){
						tempIntent = new Intent(this, UploadService.class);
						tempIntent.setAction(UploadService.ACTION_CANCEL);
						title = getString(R.string.upload_uploading);
						text = getString(R.string.upload_cancel_uploading);
					}
					else if (intent.getAction().equals(Constants.ACTION_CANCEL_DOWNLOAD)){
						tempIntent = new Intent(this, DownloadService.class);
						tempIntent.setAction(DownloadService.ACTION_CANCEL);
						title = getString(R.string.download_downloading);
						text = getString(R.string.download_cancel_downloading);
					}
					else if (intent.getAction().equals(Constants.ACTION_CANCEL_CAM_SYNC)){
						tempIntent = new Intent(this, CameraSyncService.class);
						tempIntent.setAction(CameraSyncService.ACTION_CANCEL);
						title = getString(R.string.cam_sync_syncing);
						text = getString(R.string.cam_sync_cancel_sync);
					}

					final Intent cancelIntent = tempIntent;
					AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//					builder.setTitle(title);
		            builder.setMessage(text);

					builder.setPositiveButton(getString(R.string.general_yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									if (tFLol != null){
										if (tFLol.isVisible()){
											tFLol.setNoActiveTransfers();
											supportInvalidateOptionsMenu();
										}
									}
									startService(cancelIntent);
								}
							});
					builder.setNegativeButton(getString(R.string.general_no), null);
					final AlertDialog dialog = builder.create();
					try {
						dialog.show();
					}
					catch(Exception ex)	{
						startService(cancelIntent);
					}
				}
    			else if (intent.getAction().equals(Constants.ACTION_SHOW_TRANSFERS)){
    				log("onPostResume: intent show transfers");
    				drawerItem = DrawerItem.TRANSFERS;
    				selectDrawerItemLollipop(drawerItem);
    			}
    			else if (intent.getAction().equals(Constants.ACTION_TAKE_SELFIE)){
    				log("onPostResume: Intent take selfie");
    				takePicture();
    			}
				else if (intent.getAction().equals(Constants.SHOW_REPEATED_UPLOAD)){
					log("onPostResume: Intent SHOW_REPEATED_UPLOAD");
					String message = intent.getStringExtra("MESSAGE");
					showSnackbar(message);
				}

    			intent.setAction(null);
				setIntent(null);
    		}
    	}

    	if (nV != null){
    		switch(drawerItem){
	    		case CLOUD_DRIVE:{
	    			log("onPostResume: case CLOUD DRIVE");
					//Check the tab to shown and the title of the actionBar
					int index = viewPagerCDrive.getCurrentItem();
					if(index==0) {
						log("onPostResume: TAB CLOUD DRIVE");
						if (parentHandleBrowser == -1||parentHandleBrowser==megaApi.getRootNode().getHandle()){
							log("onPostResume: Parent -1 or ROOTNODE");
							parentHandleBrowser = megaApi.getRootNode().getHandle();
							aB.setTitle(getString(R.string.section_cloud_drive));
							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
						}
						else{
							MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
							aB.setTitle(parentNode.getName());
							aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
						}
						String cloudTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
						fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cloudTag);
						if (fbFLol != null){
							getSupportFragmentManager()
									.beginTransaction()
									.detach(fbFLol)
									.attach(fbFLol)
									.commitNowAllowingStateLoss();
						}
					}
					else{
						log("onPostResume: TAB RUBBISH NODE");
						if (parentHandleRubbish == -1||parentHandleRubbish==megaApi.getRubbishNode().getHandle()){
							log("onPostResume: Parent -1 or RUBBISHNODE");
							aB.setTitle(getString(R.string.section_rubbish_bin));
							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
						}
						else{
							MegaNode parentNode = megaApi.getNodeByHandle(parentHandleRubbish);
							if (parentNode == null){
								parentNode = megaApi.getRubbishNode();
							}
							aB.setTitle(parentNode.getName());
							aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
						}
						String cloudTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);
						rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cloudTag);
						if (rbFLol != null){
//								outSFLol.refresh(parentHandleOutgoing);
							getSupportFragmentManager()
									.beginTransaction()
									.detach(rbFLol)
									.attach(rbFLol)
									.commitNowAllowingStateLoss();
						}
					}
	    			break;
	    		}
				case ACCOUNT:{
					log("onPostResume: case ACCOUNT");
					aB.setTitle(getString(R.string.section_account));
					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
					if (nV != null){
						Menu nVMenu = nV.getMenu();
						MenuItem hidden = nVMenu.findItem(R.id.navigation_item_hidden);
						resetNavigationViewMenu(nVMenu);
						hidden.setChecked(true);
					}
					break;
				}
	    		case SHARED_ITEMS:{
	    			log("onPostResume: case SHARED ITEMS");
	    			if (viewPagerShares != null){
	    				int index = viewPagerShares.getCurrentItem();
	        			if(index==0){
	        				String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 0);
	        				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
	        				if (inSFLol != null){
		   						inSFLol.refresh(parentHandleIncoming);
	        				}
	        			}
	        			else{
	        				String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 1);
	        				outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
	        				if (outSFLol != null){
	        					outSFLol.refresh(parentHandleOutgoing);
	        				}
	        			}
		    		}
					log("onPostResume: shared tabs visible");
					sharesSectionLayout.setVisibility(View.VISIBLE);
					tabLayoutShares.setVisibility(View.VISIBLE);
					viewPagerShares.setVisibility(View.VISIBLE);
		    		break;
	    		}
				case SETTINGS:{
					aB.setTitle(getString(R.string.action_settings));
					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
					break;
				}
				case CONTACTS:{
					aB.setTitle(getString(R.string.section_contacts));
					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
					break;
				}
				case CHAT:{
					if (nV != null){
						Menu nVMenu = nV.getMenu();
						resetNavigationViewMenu(nVMenu);
						MenuItem menuItem = nVMenu.findItem(R.id.navigation_item_chat);
						menuItem.setChecked(true);
						menuItem.setIcon(getResources().getDrawable(R.drawable.ic_menu_chat_red));
					}

					if (rChatFL != null){
						if(rChatFL.isAdded()){
							rChatFL.setChats();
						}
					}
					break;
				}
    		}
    	}
	}

	public void setChats(){
		if(drawerItem==DrawerItem.CHAT){
			if (rChatFL != null) {
				if (rChatFL.isAdded()) {
					rChatFL.setChats();
				}

			}
		}
	}

	public void showMuteIcon(MegaChatListItem item){
		log("showMuteIcon");
		if (rChatFL != null) {
			if (rChatFL.isAdded()) {
				rChatFL.showMuteIcon(item);
			}
		}
	}

	public void setProfileAvatar(){
		log("setProfileAvatar");
		File avatar = null;
		if (getExternalCacheDir() != null){
			avatar = new File(getExternalCacheDir().getAbsolutePath(), myAccountInfo.getMyUser().getEmail() + ".jpg");
		}
		else{
			avatar = new File(getCacheDir().getAbsolutePath(), myAccountInfo.getMyUser().getEmail() + ".jpg");
		}
		Bitmap imBitmap = null;
		if (avatar.exists()){
			if (avatar.length() > 0){
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(avatar.getAbsolutePath(), options);

				// Calculate inSampleSize
				options.inSampleSize = Util.calculateInSampleSize(options, 250, 250);

				// Decode bitmap with inSampleSize set
				options.inJustDecodeBounds = false;

				imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), options);
				if (imBitmap == null) {
					avatar.delete();
					if (getExternalCacheDir() != null){
						megaApi.getUserAvatar(myAccountInfo.getMyUser(), getExternalCacheDir().getAbsolutePath() + "/" + myAccountInfo.getMyUser().getEmail() + ".jpg", myAccountInfo);
					}
					else{
						megaApi.getUserAvatar(myAccountInfo.getMyUser(), getCacheDir().getAbsolutePath() + "/" + myAccountInfo.getMyUser().getEmail() + ".jpg", myAccountInfo);
					}
				}
				else{
					Bitmap circleBitmap = Bitmap.createBitmap(imBitmap.getWidth(), imBitmap.getHeight(), Bitmap.Config.ARGB_8888);

					BitmapShader shader = new BitmapShader (imBitmap,  TileMode.CLAMP, TileMode.CLAMP);
					Paint paint = new Paint();
					paint.setShader(shader);

					Canvas c = new Canvas(circleBitmap);
					int radius;
					if (imBitmap.getWidth() < imBitmap.getHeight())
						radius = imBitmap.getWidth()/2;
					else
						radius = imBitmap.getHeight()/2;

					c.drawCircle(imBitmap.getWidth()/2, imBitmap.getHeight()/2, radius, paint);
					nVPictureProfile.setImageBitmap(circleBitmap);
					nVPictureProfileTextView.setVisibility(View.GONE);
				}
			}
			else{
				if (getExternalCacheDir() != null){
					megaApi.getUserAvatar(myAccountInfo.getMyUser(), getExternalCacheDir().getAbsolutePath() + "/" + myAccountInfo.getMyUser().getEmail() + ".jpg", myAccountInfo);
				}
				else{
					megaApi.getUserAvatar(myAccountInfo.getMyUser(), getCacheDir().getAbsolutePath() + "/" + myAccountInfo.getMyUser().getEmail() + ".jpg", myAccountInfo);
				}
			}
		}
		else{
			if (getExternalCacheDir() != null){
				megaApi.getUserAvatar(myAccountInfo.getMyUser(), getExternalCacheDir().getAbsolutePath() + "/" + myAccountInfo.getMyUser().getEmail() + ".jpg", myAccountInfo);
			}
			else{
				megaApi.getUserAvatar(myAccountInfo.getMyUser(), getCacheDir().getAbsolutePath() + "/" + myAccountInfo.getMyUser().getEmail() + ".jpg", myAccountInfo);
			}
		}
	}

	public void setDefaultAvatar(){
		log("setDefaultAvatar");
		float density  = getResources().getDisplayMetrics().density;
		Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		String color = megaApi.getUserAvatarColor(myAccountInfo.getMyUser());
		if(color!=null){
			log("The color to set the avatar is "+color);
			p.setColor(Color.parseColor(color));
		}
		else{
			log("Default color to the avatar");
			p.setColor(getResources().getColor(R.color.lollipop_primary_color));
		}

		int radius;
		if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
			radius = defaultAvatar.getWidth()/2;
		else
			radius = defaultAvatar.getHeight()/2;

		c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
		nVPictureProfile.setImageBitmap(defaultAvatar);

		int avatarTextSize = Util.getAvatarTextSize(density);
		log("DENSITY: " + density + ":::: " + avatarTextSize);

		String firstLetter = myAccountInfo.getFirstLetter();
		nVPictureProfileTextView.setText(firstLetter);
		nVPictureProfileTextView.setTextSize(32);
		nVPictureProfileTextView.setTextColor(Color.WHITE);
		nVPictureProfileTextView.setVisibility(View.VISIBLE);
	}

	public void setOfflineAvatar(String email, String firstLetter){
		log("setOfflineAvatar");

		File avatar = null;
		if (getExternalCacheDir() != null){
			avatar = new File(getExternalCacheDir().getAbsolutePath(), email + ".jpg");
		}
		else{
			avatar = new File(getCacheDir().getAbsolutePath(), email + ".jpg");
		}
		Bitmap imBitmap = null;
		if (avatar.exists()) {
			if (avatar.length() > 0) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(avatar.getAbsolutePath(), options);

				// Calculate inSampleSize
				options.inSampleSize = Util.calculateInSampleSize(options, 250, 250);

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
					nVPictureProfile.setImageBitmap(circleBitmap);
					nVPictureProfileTextView.setVisibility(View.GONE);
					return;
				}
			}
		}

		log("setDefaultAvatar");
		float density  = getResources().getDisplayMetrics().density;
		Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		String color = megaApi.getUserAvatarColor(myAccountInfo.getMyUser());
		if(color!=null){
			log("The color to set the avatar is "+color);
			p.setColor(Color.parseColor(color));
		}
		else{
			log("Default color to the avatar");
			p.setColor(getResources().getColor(R.color.lollipop_primary_color));
		}

		int radius;
		if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
			radius = defaultAvatar.getWidth()/2;
		else
			radius = defaultAvatar.getHeight()/2;

		c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
		nVPictureProfile.setImageBitmap(defaultAvatar);

		int avatarTextSize = Util.getAvatarTextSize(density);
		log("DENSITY: " + density + ":::: " + avatarTextSize);

		nVPictureProfileTextView.setText(firstLetter);
		nVPictureProfileTextView.setTextSize(32);
		nVPictureProfileTextView.setTextColor(Color.WHITE);
		nVPictureProfileTextView.setVisibility(View.VISIBLE);

	}

//	@Override
//	protected void onPostResume() {
//		log("onPostResume");
//	    super.onPostResume();
//	    if (isSearching){
//			selectDrawerItemLollipop(DrawerItem.SEARCH);
//    		isSearching = false;
//	    }
//	}

	public void showDialogChangeUserAttribute(){
		log("showDialogChangeUserAttribute");

		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

		final EditText inputFirstName = new EditText(this);
		layout.addView(inputFirstName, params);
		final EditText inputLastName = new EditText(this);
		layout.addView(inputLastName, params);
		final EditText inputMail = new EditText(this);
		layout.addView(inputMail, params);

		inputFirstName.setSingleLine();
		inputFirstName.setText(myAccountInfo.getFirstNameText());
		inputFirstName.setTextColor(getResources().getColor(R.color.text_secondary));
		inputFirstName.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		inputFirstName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		inputFirstName.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_NEXT) {
					String value = inputFirstName.getText().toString().trim();
					if(value.equals("")||value.isEmpty()){
						log("input is empty");
						inputFirstName.setError(getString(R.string.invalid_string));
						inputFirstName.requestFocus();
					} else {
						log("action NEXT ime - change user attribute");
						inputLastName.requestFocus();
					}
				}
				else{
					log("other IME" + actionId);
				}
				return false;
			}
		});
		inputFirstName.setImeActionLabel(getString(R.string.next_ime_action),EditorInfo.IME_ACTION_NEXT);
		inputLastName.setSingleLine();
		inputLastName.setText(myAccountInfo.getLastNameText());
		inputLastName.setTextColor(getResources().getColor(R.color.text_secondary));
		inputLastName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		inputLastName.setImeOptions(EditorInfo.IME_ACTION_NEXT);

		inputLastName.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_NEXT) {
					String value = inputLastName.getText().toString().trim();
					if(value.equals("")||value.isEmpty()){
						log("input is empty");
						inputLastName.setError(getString(R.string.invalid_string));
						inputLastName.requestFocus();
					} else {
						log("action NEXT ime - change user attribute");
						inputMail.requestFocus();
					}
				}
				else{
					log("other IME" + actionId);
				}
				return false;
			}
		});
		inputLastName.setImeActionLabel(getString(R.string.next_ime_action),EditorInfo.IME_ACTION_NEXT);

		inputMail.setSingleLine();
		inputMail.setText(myAccountInfo.getMyUser().getEmail());
		inputMail.setTextColor(getResources().getColor(R.color.text_secondary));
		inputMail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		inputMail.setImeOptions(EditorInfo.IME_ACTION_DONE);
		inputMail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		inputMail.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String valueFirstName = inputFirstName.getText().toString().trim();
					String valueLastName = inputLastName.getText().toString().trim();
					String value = inputMail.getText().toString().trim();
					String emailError = Util.getEmailError(value, managerActivity);
					if (emailError != null) {
						inputMail.setError(emailError);
						inputMail.requestFocus();
					}
					else if(valueFirstName.equals("")||valueFirstName.isEmpty()){
						log("input is empty");
						inputFirstName.setError(getString(R.string.invalid_string));
						inputFirstName.requestFocus();
					}
					else if(valueLastName.equals("")||valueLastName.isEmpty()){
						log("input is empty");
						inputLastName.setError(getString(R.string.invalid_string));
						inputLastName.requestFocus();
					}
					else {
						log("positive button pressed - change user attribute");
						countUserAttributes = aC.updateUserAttributes(myAccountInfo.getFirstNameText(), valueFirstName, myAccountInfo.getLastNameText(), valueLastName, myAccountInfo.getMyUser().getEmail(), value);
						changeUserAttributeDialog.dismiss();
					}
				}
				else{
					log("other IME" + actionId);
				}
				return false;
			}
		});
		inputMail.setImeActionLabel(getString(R.string.title_edit_profile_info),EditorInfo.IME_ACTION_DONE);

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setTitle(getString(R.string.title_edit_profile_info));

		builder.setPositiveButton(getString(R.string.title_edit_profile_info),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		builder.setView(layout);

		changeUserAttributeDialog = builder.create();
		changeUserAttributeDialog.show();
		changeUserAttributeDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				log("OK BTTN PASSWORD");
				String valueFirstName = inputFirstName.getText().toString().trim();
				String valueLastName = inputLastName.getText().toString().trim();
				String value = inputMail.getText().toString().trim();
				String emailError = Util.getEmailError(value, managerActivity);
				if (emailError != null) {
					inputMail.setError(emailError);
					inputMail.requestFocus();
				}
				else if(valueFirstName.equals("")||valueFirstName.isEmpty()){
					log("input is empty");
					inputFirstName.setError(getString(R.string.invalid_string));
					inputFirstName.requestFocus();
				}
				else if(valueLastName.equals("")||valueLastName.isEmpty()){
					log("input is empty");
					inputLastName.setError(getString(R.string.invalid_string));
					inputLastName.requestFocus();
				}
				else {
					log("positive button pressed - change user attribute");
					countUserAttributes = aC.updateUserAttributes(myAccountInfo.getFirstNameText(), valueFirstName, myAccountInfo.getLastNameText(), valueLastName, myAccountInfo.getMyUser().getEmail(), value);
					changeUserAttributeDialog.dismiss();
				}
			}
		});
		showKeyboardDelayed(inputFirstName);
	}

	@Override
	protected void onStop(){
		log("onStop");
		super.onStop();
	}

	@Override
	protected void onPause() {
    	log("onPause");
    	managerActivity = null;
    	super.onPause();

		MegaApplication.activityPaused();
    }

	@Override
    protected void onDestroy(){
		log("onDestroy()");

    	if (megaApi.getRootNode() != null){
    		megaApi.removeGlobalListener(this);
    		megaApi.removeTransferListener(this);
    		megaApi.removeRequestListener(this);
    	}

		if (megaChatApi != null){
			megaChatApi.removeChatListener(this);
		}

    	super.onDestroy();
	}

	public void selectDrawerItemCloudDrive(){
		log("selectDrawerItemCloudDrive");

		tB.setVisibility(View.VISIBLE);
		contactsSectionLayout.setVisibility(View.GONE);
		viewPagerContacts.setVisibility(View.GONE);
		sharesSectionLayout.setVisibility(View.GONE);
		viewPagerShares.setVisibility(View.GONE);
		myAccountSectionLayout.setVisibility(View.GONE);
		viewPagerMyAccount.setVisibility(View.GONE);

		if (mTabsAdapterCDrive == null){
			log("mTabsAdapterCloudDrive == null");
			cloudSectionLayout.setVisibility(View.VISIBLE);
			viewPagerCDrive.setVisibility(View.VISIBLE);
			mTabsAdapterCDrive = new CloudDrivePagerAdapter(getSupportFragmentManager(),this);
			viewPagerCDrive.setAdapter(mTabsAdapterCDrive);
			tabLayoutCloud.setupWithViewPager(viewPagerCDrive);

			//Force on CreateView, addTab do not execute onCreateView
			if(indexCloud!=-1){
				log("The index of the TAB CLOUD is: "+indexCloud);
				if (viewPagerCDrive != null){
					if(indexCloud==0){
						log("after creating tab in CLOUD TAB: "+parentHandleBrowser);

						String cloudTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
						fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cloudTag);
						if (fbFLol != null){
							getSupportFragmentManager()
									.beginTransaction()
									.detach(fbFLol)
									.attach(fbFLol)
									.commitNowAllowingStateLoss();
						}
						viewPagerCDrive.setCurrentItem(0);
					}
					else{
						log("after creating tab in RUBBISH TAB: "+parentHandleRubbish);

						String cloudTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);
						rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cloudTag);
						if (rbFLol != null){
//								outSFLol.refresh(parentHandleOutgoing);
							getSupportFragmentManager()
									.beginTransaction()
									.detach(rbFLol)
									.attach(rbFLol)
									.commitNowAllowingStateLoss();
						}
						viewPagerCDrive.setCurrentItem(1);
					}
					aB.setTitle(getString(R.string.section_cloud_drive));
				}
				indexCloud=-1;
			}
			else{
				//No bundle, no change of orientation
				log("indexCloud is NOT -1");
			}
		}
		else{
			log("mTabsAdapterCloudDrive NOT null");
			cloudSectionLayout.setVisibility(View.VISIBLE);
			viewPagerCDrive.setVisibility(View.VISIBLE);

			String sharesTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
			fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);

			sharesTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);
			rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);

			//Check viewPager to determine the tab shown

			if(viewPagerCDrive!=null){
				int index = viewPagerCDrive.getCurrentItem();
				log("Fragment Index: " + index);
				if(index == 1){
					//Rubbish Bin TAB
					MegaNode parentNode = megaApi.getNodeByHandle(parentHandleRubbish);
					if (parentNode != null){
						if (parentNode.getHandle() == megaApi.getRubbishNode().getHandle()){
							aB.setTitle(getString(R.string.section_rubbish_bin));
							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
							firstNavigationLevel = true;
						}
						else{
							aB.setTitle(parentNode.getName());
							log("indicator_arrow_back_886");
							aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
							firstNavigationLevel = false;
						}
					}
					else{
						parentHandleRubbish = megaApi.getRubbishNode().getHandle();
						parentNode = megaApi.getRootNode();
						aB.setTitle(getString(R.string.section_rubbish_bin));
						aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
						firstNavigationLevel = true;
					}
					ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderCloud);
					rbFLol.setNodes(nodes);
				}
				else{
					//Cloud Drive TAB
					MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
					if (parentNode != null){
						if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
							aB.setTitle(getString(R.string.section_cloud_drive));
							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
							firstNavigationLevel = true;
						}
						else{
							aB.setTitle(parentNode.getName());
							log("indicator_arrow_back_887");
							aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
							firstNavigationLevel = false;
						}
					}
					else{
						parentHandleBrowser = megaApi.getRootNode().getHandle();
						parentNode = megaApi.getRootNode();
						aB.setTitle(getString(R.string.section_cloud_drive));
						aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
						firstNavigationLevel = true;
					}
					ArrayList<MegaNode> nodes = new ArrayList<MegaNode>();
					if(parentNode==null){
						nodes =	megaApi.getChildren(megaApi.getRootNode(), orderCloud);
					}
					else{
						nodes =	megaApi.getChildren(parentNode, orderCloud);
					}
					String cloudTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
					fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cloudTag);
					if(fbFLol!=null){
						log("FileBrowserFragmentLollipop recovered twice!");
						fbFLol.setNodes(nodes);
						fbFLol.setParentHandle(parentHandleBrowser);
					}
				}
			}
			else{
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
				if (parentNode != null){
					if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
						aB.setTitle(getString(R.string.section_cloud_drive));
						aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
						firstNavigationLevel = true;
					}
					else{
						aB.setTitle(parentNode.getName());
						log("indicator_arrow_back_890");
						aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
						firstNavigationLevel = false;
					}
				}
				else{
					parentHandleBrowser = megaApi.getRootNode().getHandle();
					parentNode = megaApi.getRootNode();
					aB.setTitle(getString(R.string.section_cloud_drive));
					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
					firstNavigationLevel = true;
				}
				ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderCloud);
				String cloudTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cloudTag);
				if(fbFLol!=null){
					log("FileBrowserFragmentLollipop recovered once more!");
					fbFLol.setNodes(nodes);
				}
			}
		}

		viewPagerCDrive.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			public void onPageScrollStateChanged(int state) {}
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

			public void onPageSelected(int position) {
				log("onTabChanged TabId :"+ position);
				supportInvalidateOptionsMenu();
				if(position == 0){
					String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
					fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
					if (fbFLol != null){
						log("parentHandleCloud: "+ parentHandleBrowser);
						if(parentHandleBrowser==megaApi.getRootNode().getHandle()||parentHandleBrowser==-1){
							log("aB.setTitle2");
							aB.setTitle(getResources().getString(R.string.section_cloud_drive));
							log("aB.setHomeAsUpIndicator_11");
							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
							fbFLol.setNodes(megaApi.getChildren(megaApi.getRootNode(), orderCloud));
							firstNavigationLevel = true;
						}
						else {
							MegaNode node = megaApi.getNodeByHandle(parentHandleBrowser);
							aB.setTitle(node.getName());
							log("indicator_arrow_back_891");
							aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
							fbFLol.setNodes(megaApi.getChildren(node, orderCloud));
							firstNavigationLevel = false;
						}
					}
				}
				else if(position == 1){
					String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);
					rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
					if (rbFLol != null){
						log("parentHandleRubbish: "+ parentHandleRubbish);
						if(parentHandleRubbish == megaApi.getRubbishNode().getHandle() || parentHandleRubbish == -1){
							aB.setTitle(getResources().getString(R.string.section_rubbish_bin));
							log("aB.setHomeAsUpIndicator_13");
							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
							rbFLol.setNodes(megaApi.getChildren(megaApi.getRubbishNode(), orderCloud));
							firstNavigationLevel = true;
						}
						else{
							MegaNode node = megaApi.getNodeByHandle(parentHandleRubbish);
							aB.setTitle(node.getName());
							log("indicator_arrow_back_892");
							aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
							rbFLol.setNodes(megaApi.getChildren(node, orderCloud));
							firstNavigationLevel = false;
						}
					}

				}
				showFabButton();
			}
		});

		if (!firstTime){
			log("Its NOT first time");
			drawerLayout.closeDrawer(Gravity.LEFT);

			if (dbH.getContactsSize() != megaApi.getContacts().size()){
				dbH.clearContacts();
				FillDBContactsTask fillDBContactsTask = new FillDBContactsTask(this);
				fillDBContactsTask.execute();
			}
		}
		else{
			log("Its first time");

			drawerLayout.openDrawer(Gravity.LEFT);
			//Fill the contacts DB
			FillDBContactsTask fillDBContactsTask = new FillDBContactsTask(this);
			fillDBContactsTask.execute();
			firstTime = false;
		}

		viewPagerContacts.setVisibility(View.GONE);
	}

	public void showOfflineMode(){
		log("showOfflineMode");

		usedSpaceLayout.setVisibility(View.GONE);

		Menu nVMenu = nV.getMenu();
		drawerMenuItem = nVMenu.findItem(R.id.navigation_item_saved_for_offline);
		if (drawerMenuItem != null){
			disableNavigationViewMenu(nVMenu);
			drawerMenuItem.setChecked(true);
			drawerMenuItem.setIcon(getResources().getDrawable(R.drawable.saved_for_offline_red));
		}

		UserCredentials credentials = dbH.getCredentials();
		if(credentials!=null){
			String emailCredentials = credentials.getEmail();
			if(emailCredentials!=null){
				nVEmail.setText(emailCredentials);
			}

			String firstNameText = credentials.getFirstName();
			String lastNameText = credentials.getLastName();
			String fullName = "";
			if(firstNameText==null){
				firstNameText="";
			}
			if(lastNameText==null){
				lastNameText="";
			}
			if (firstNameText.trim().length() <= 0){
				fullName = lastNameText;
			}
			else{
				fullName = firstNameText + " " + lastNameText;
			}

			if (fullName.trim().length() <= 0){
				log("Put email as fullname");
				String[] splitEmail = emailCredentials.split("[@._]");
				fullName = splitEmail[0];
			}

			if (fullName.trim().length() <= 0){
				fullName = getString(R.string.name_text)+" "+getString(R.string.lastname_text);
				log("Full name set by default: "+fullName);
			}

			nVDisplayName.setText(fullName);

			String firstLetter = fullName.charAt(0) + "";
			firstLetter = firstLetter.toUpperCase(Locale.getDefault());

			setOfflineAvatar(emailCredentials, firstLetter);
		}

		drawerItem=DrawerItem.SAVED_FOR_OFFLINE;
		selectDrawerItemLollipop(drawerItem);
	}

	public void selectDrawerItemSharedItems(){
		log("selectDrawerItemSharedItems");
		tB.setVisibility(View.VISIBLE);

		if (aB == null){
			aB = getSupportActionBar();
		}

		contactsSectionLayout.setVisibility(View.GONE);
		viewPagerContacts.setVisibility(View.GONE);
		cloudSectionLayout.setVisibility(View.GONE);
		viewPagerCDrive.setVisibility(View.GONE);
		myAccountSectionLayout.setVisibility(View.GONE);
		viewPagerMyAccount.setVisibility(View.GONE);

		if (mTabsAdapterShares == null){
			log("selectDrawerItemSharedItems: mTabsAdapterShares is NULL");

			mTabsAdapterShares = new SharesPageAdapter(getSupportFragmentManager(),this);
			viewPagerShares.setAdapter(mTabsAdapterShares);
			tabLayoutShares.setupWithViewPager(viewPagerShares);

			//Force on CreateView, addTab do not execute onCreateView
			if(indexShares!=-1){
				log("selectDrawerItemSharedItems: The index of the TAB Shares is: "+indexShares);
				if (viewPagerShares != null){
					if(indexShares==0){
						log("selectDrawerItemSharedItems: after creating tab in INCOMING TAB: "+parentHandleIncoming);
						log("selectDrawerItemSharedItems: deepBrowserTreeIncoming: "+deepBrowserTreeIncoming);

						String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 0);
						inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
						if (inSFLol != null){
//								inSFLol.refresh(parentHandleIncoming);
							getSupportFragmentManager()
									.beginTransaction()
									.detach(inSFLol)
									.attach(inSFLol)
									.commitNowAllowingStateLoss();
						}
						viewPagerShares.setCurrentItem(0);
					}
					else{
						log("selectDrawerItemSharedItems: after creating tab in OUTGOING TAB: "+parentHandleOutgoing);

						String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 1);
						outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
						if (outSFLol != null){
//								outSFLol.refresh(parentHandleOutgoing);
							getSupportFragmentManager()
									.beginTransaction()
									.detach(outSFLol)
									.attach(outSFLol)
									.commitNowAllowingStateLoss();
						}
						viewPagerShares.setCurrentItem(1);
					}
				}
				indexShares=-1;
			}
			else {
				//No bundle, no change of orientation
				log("selectDrawerItemSharedItems: indexShares is NOT -1");
			}
			sharesSectionLayout.setVisibility(View.VISIBLE);
			viewPagerShares.setVisibility(View.VISIBLE);
		}
		else{
			log("selectDrawerItemSharedItems: mTabsAdapterShares NOT null");
			sharesSectionLayout.setVisibility(View.VISIBLE);
			viewPagerShares.setVisibility(View.VISIBLE);

			String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 0);
			inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
			sharesTag = getFragmentTag(R.id.shares_tabs_pager, 1);
			outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);

			//Needed when changing list<->grid
//    				if (inSFLol != null){
//	        			inSFLol.setOrder(orderOthers);
//	        			inSFLol.setIsList(isList);
//
//	        			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
//	        			fragTransaction.detach(inSFLol);
//	        			fragTransaction.commit();
//
//	        			fragTransaction = getSupportFragmentManager().beginTransaction();
//	        			fragTransaction.attach(inSFLol);
//	        			fragTransaction.commit();
//    				}
//
//    				if (outSFLol != null){
//    					outSFLol.setOrder(orderOthers);
//    					outSFLol.setIsList(isList);
//
//    					FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
//	        			fragTransaction.detach(outSFLol);
//	        			fragTransaction.commit();
//
//	        			fragTransaction = getSupportFragmentManager().beginTransaction();
//	        			fragTransaction.attach(outSFLol);
//	        			fragTransaction.commit();
//    				}

			int index = viewPagerShares.getCurrentItem();
			log("selectDrawerItemSharedItems: Fragment Index Shared Items: " + index);
			if(index==0){
				//INCOMING TAB
//        				String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 0);
//        				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
				if (inSFLol != null){
					MegaNode node = megaApi.getNodeByHandle(parentHandleIncoming);
					log("selectDrawerItemSharedItems: Selected Incoming with parent: "+parentHandleIncoming);
					log("selectDrawerItemSharedItems: inSFLol deepBrowserTreeIncoming: "+deepBrowserTreeIncoming);
					if (node != null){
						inSFLol.setNodes(megaApi.getChildren(node, orderOthers));
						inSFLol.setParentHandle(parentHandleIncoming);
						inSFLol.setDeepBrowserTree(deepBrowserTreeIncoming);
						aB.setTitle(node.getName());
						aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
						firstNavigationLevel = false;
					}
					else{
						log("selectDrawerItemSharedItems: The Node is NULL");
						inSFLol.findNodes();
						aB.setTitle(getResources().getString(R.string.section_shared_items));
						aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
						firstNavigationLevel = true;
					}
				}
			}
			else{
//        				String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 1);
//        				outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
				if (outSFLol != null){
//        					outSFLol.refresh(parentHandleIncoming);
					MegaNode node = megaApi.getNodeByHandle(parentHandleOutgoing);
					if (node != null){
						outSFLol.setNodes(megaApi.getChildren(node, orderOthers));
						aB.setTitle(node.getName());
						aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
						firstNavigationLevel = false;
					}
					else{
						outSFLol.refresh();
						aB.setTitle(getResources().getString(R.string.section_shared_items));
						aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
						firstNavigationLevel = true;
					}
				}
			}
		}

		sharesSectionLayout.setVisibility(View.VISIBLE);
		viewPagerShares.setVisibility(View.VISIBLE);

		viewPagerShares.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			@Override
			public void onPageSelected(int position) {
				log("selectDrawerItemSharedItems: TabId :"+ position);
				supportInvalidateOptionsMenu();
				if(position == 1){
					String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 1);
					outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
					if (outSFLol != null){

						if(parentHandleOutgoing!=-1){
							MegaNode node = megaApi.getNodeByHandle(parentHandleOutgoing);
							aB.setTitle(node.getName());
							log("indicator_arrow_back_895");
							aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
							firstNavigationLevel = false;
							outSFLol.setNodes(megaApi.getChildren(node, orderOthers));
						}
						else{
							aB.setTitle(getResources().getString(R.string.section_shared_items));
							log("aB.setHomeAsUpIndicator_20");
							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
							firstNavigationLevel = true;
						}
					}
					else{
						log("selectDrawerItemSharedItems: outSFLol == null");
					}
				}
				else if(position == 0){
					String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 0);
					inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
					if (inSFLol != null){

						if(parentHandleIncoming!=-1){
							MegaNode node = megaApi.getNodeByHandle(parentHandleIncoming);
							aB.setTitle(node.getName());
							log("indicator_arrow_back_896");
							aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
							firstNavigationLevel = false;
							inSFLol.setNodes(megaApi.getChildren(node, orderOthers));
						}
						else{
							aB.setTitle(getResources().getString(R.string.section_shared_items));
							log("aB.setHomeAsUpIndicator_22");
							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
							firstNavigationLevel = true;
						}
					}
					else{
						log("selectDrawerItemSharedItems: inSFLol == null");
					}
				}
				showFabButton();
			}

			@Override
			public void onPageScrollStateChanged(int state) {
			}
		});

		drawerLayout.closeDrawer(Gravity.LEFT);
	}

	public void selectDrawerItemContacts (){
		log("selectDrawerItemContacts");
		tB.setVisibility(View.VISIBLE);

		if (aB == null){
			aB = getSupportActionBar();
		}
		aB.setTitle(getString(R.string.section_contacts));
		aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
		firstNavigationLevel = true;

		sharesSectionLayout.setVisibility(View.GONE);
		viewPagerShares.setVisibility(View.GONE);
		cloudSectionLayout.setVisibility(View.GONE);
		viewPagerCDrive.setVisibility(View.GONE);
		myAccountSectionLayout.setVisibility(View.GONE);
		viewPagerMyAccount.setVisibility(View.GONE);

		Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
		if (currentFragment != null){
			getSupportFragmentManager().beginTransaction().remove(currentFragment).commitNow();
		}
		contactsSectionLayout.setVisibility(View.VISIBLE);
		viewPagerContacts.setVisibility(View.VISIBLE);

		if (mTabsAdapterContacts == null){
			log("mTabsAdapterContacts == null");

			contactsSectionLayout.setVisibility(View.VISIBLE);
			viewPagerContacts.setVisibility(View.VISIBLE);
			mTabsAdapterContacts = new ContactsPageAdapter(getSupportFragmentManager(),this);
			viewPagerContacts.setAdapter(mTabsAdapterContacts);
			tabLayoutContacts.setupWithViewPager(viewPagerContacts);

			log("The index of the TAB CONTACTS is: " + indexContacts);
			if(indexContacts!=-1) {
				if (viewPagerContacts != null) {
					switch (indexContacts){
						case 1:{
							viewPagerContacts.setCurrentItem(1);
							log("Select Sent Requests TAB");
							break;
						}
						case 2:{
							viewPagerContacts.setCurrentItem(2);
							log("Select Received Request TAB");
							break;
						}
						default:{
							viewPagerContacts.setCurrentItem(0);
							log("Select Contacts TAB");
							break;
						}
					}
				}
			}
			else{
				//No bundle, no change of orientation
				log("indexContacts is NOT -1");
			}
		}
		else{
			log("mTabsAdapterContacts NOT null");
			String sharesTag = getFragmentTag(R.id.contact_tabs_pager, 0);
			cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
			sharesTag = getFragmentTag(R.id.contact_tabs_pager, 1);
			sRFLol = (SentRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
			sharesTag = getFragmentTag(R.id.contact_tabs_pager, 2);
			rRFLol = (ReceivedRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);

			if(indexContacts!=-1) {
				log("The index of the TAB CONTACTS is: " + indexContacts);
				if (viewPagerContacts != null) {
					switch (indexContacts) {
						case 1: {
							viewPagerContacts.setCurrentItem(1);
							log("Select Sent Requests TAB");
							break;
						}
						case 2: {
							viewPagerContacts.setCurrentItem(2);
							log("Select Received Request TAB");
							break;
						}
						default: {
							viewPagerContacts.setCurrentItem(0);
							log("Select Contacts TAB");
							break;
						}
					}
				}
			}
		}

		viewPagerContacts.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

			}

			@Override
			public void onPageSelected(int position) {
				supportInvalidateOptionsMenu();
				showFabButton();
			}

			@Override
			public void onPageScrollStateChanged(int state) {

			}
		});

		drawerLayout.closeDrawer(Gravity.LEFT);
	}

	public void selectDrawerItemAccount(){
		log("selectDrawerItemAccount");

		contactsSectionLayout.setVisibility(View.GONE);
		viewPagerContacts.setVisibility(View.GONE);
		sharesSectionLayout.setVisibility(View.GONE);
		viewPagerShares.setVisibility(View.GONE);
		cloudSectionLayout.setVisibility(View.GONE);
		viewPagerCDrive.setVisibility(View.GONE);

		switch(accountFragment){
			case Constants.UPGRADE_ACCOUNT_FRAGMENT:{
				log("Show upgrade FRAGMENT");
				myAccountSectionLayout.setVisibility(View.GONE);
				viewPagerMyAccount.setVisibility(View.GONE);
				showUpAF();
				break;
			}
			case Constants.MONTHLY_YEARLY_FRAGMENT:{
				log("Show monthly yearly FRAGMENT");
				myAccountSectionLayout.setVisibility(View.GONE);
				viewPagerMyAccount.setVisibility(View.GONE);
				showmyF(selectedPaymentMethod, selectedAccountType);
				showFabButton();
				break;
			}
			default:{
				log("Show myAccount Fragment");
				accountFragment=Constants.MY_ACCOUNT_FRAGMENT;

				if(aB!=null){
					aB.setTitle(getString(R.string.section_account));
					log("indicator_menu_white_559");
					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
					setFirstNavigationLevel(true);
				}

				Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
				if (currentFragment != null){
					getSupportFragmentManager().beginTransaction().remove(currentFragment).commitNow();
				}
				myAccountSectionLayout.setVisibility(View.VISIBLE);
				viewPagerMyAccount.setVisibility(View.VISIBLE);

				if (mTabsAdapterMyAccount == null){
					log("mTabsAdapterMyAccount == null");

					mTabsAdapterMyAccount = new MyAccountPageAdapter(getSupportFragmentManager(),this);
					viewPagerMyAccount.setAdapter(mTabsAdapterMyAccount);
					tabLayoutMyAccount.setupWithViewPager(viewPagerMyAccount);

					log("The index of the TAB ACCOUNT is: " + indexAccount);
					if(indexAccount!=-1) {
						if (viewPagerMyAccount != null) {
							switch (indexAccount){
								case 0:{
									viewPagerMyAccount.setCurrentItem(0);
									log("General TAB");
									break;
								}
								case 1:{
									viewPagerMyAccount.setCurrentItem(1);
									log("Storage TAB");
									break;
								}
								default:{
									viewPagerContacts.setCurrentItem(0);
									log("Default general TAB");
									break;
								}
							}
						}
					}
					else{
						//No bundle, no change of orientation
						log("indexAccount is NOT -1");
					}
				}
				else{
					log("mTabsAdapterMyAccount NOT null");
					String myAccountTag = getFragmentTag(R.id.my_account_tabs_pager, 0);
					maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(myAccountTag);

					myAccountTag = getFragmentTag(R.id.my_account_tabs_pager, 1);
					mStorageFLol = (MyStorageFragmentLollipop) getSupportFragmentManager().findFragmentByTag(myAccountTag);

					if(indexAccount!=-1) {
						log("The index of the TAB MyAccount is: " + indexAccount);
						if (viewPagerMyAccount != null) {
							switch (indexAccount) {
								case 1: {
									viewPagerMyAccount.setCurrentItem(1);
									log("Select Storage TAB");
									break;
								}
								default: {
									viewPagerMyAccount.setCurrentItem(0);
									log("Select General TAB");
									break;
								}
							}
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
					}

					@Override
					public void onPageScrollStateChanged(int state) {
					}
				});

				drawerLayout.closeDrawer(Gravity.LEFT);

				supportInvalidateOptionsMenu();
				showFabButton();
				break;
			}
		}
	}

	public void selectDrawerItemChat(){
		log("selectDrawerItemChat");

		tB.setVisibility(View.VISIBLE);

		if (aB == null){
			aB = getSupportActionBar();
		}
		aB.setTitle(getString(R.string.section_chat));
		aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
		firstNavigationLevel = true;

		sharesSectionLayout.setVisibility(View.GONE);
		viewPagerShares.setVisibility(View.GONE);
		cloudSectionLayout.setVisibility(View.GONE);
		viewPagerCDrive.setVisibility(View.GONE);
		contactsSectionLayout.setVisibility(View.GONE);
		viewPagerContacts.setVisibility(View.GONE);
		myAccountSectionLayout.setVisibility(View.GONE);
		viewPagerMyAccount.setVisibility(View.GONE);

		if (rChatFL == null){
			log("New REcentChatFragment");
			rChatFL = new RecentChatsFragmentLollipop();
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.fragment_container, rChatFL, "rChat");
			ft.commitNow();
//			maFLol.setMyEmail(megaApi.getMyUser().getEmail());
//			if(myAccountInfo==null){
//				log("Not possibleeeeeee!!");
//			}
//			else{
//				maFLol.setMyAccountInfo(myAccountInfo);
//			}
//			maFLol.setMKLayoutVisible(mkLayoutVisible);
		}
		else{
			log("REcentChatFragment is not null");
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.fragment_container, rChatFL, "rChat");
			ft.commitNow();
			rChatFL.setChats();
//			rChatFL.setMyEmail(megaApi.getMyUser().getEmail());
//			if(myAccountInfo==null){
//				log("Not possibleeeeeee!!");
//			}
//			else{
//				maFLol.setMyAccountInfo(myAccountInfo);
//			}
//
//			maFLol.setMKLayoutVisible(mkLayoutVisible);
		}
		log("show chats");

		drawerLayout.closeDrawer(Gravity.LEFT);
	}
	@SuppressLint("NewApi")
	public void selectDrawerItemLollipop(DrawerItem item){
    	log("selectDrawerItemLollipop: "+item);

    	switch (item){
    		case CLOUD_DRIVE:{
				selectDrawerItemCloudDrive();
    			supportInvalidateOptionsMenu();
				showFabButton();
				log("END selectDrawerItem for Cloud Drive");
    			break;
    		}
    		case SAVED_FOR_OFFLINE:{

    			tB.setVisibility(View.VISIBLE);

    			if (oFLol == null){
					log("New OfflineFragment");
    				oFLol = new OfflineFragmentLollipop();
    				oFLol.setIsList(isList);
//    				oFLol.setPathNavigation("/");
    			}
    			else{
					log("OfflineFragment exist");
//    				oFLol.setPathNavigation("/");
    				oFLol.setIsList(isList);
					oFLol.findNodes();
					oFLol.setTitle();
    			}

    			cloudSectionLayout.setVisibility(View.GONE);
    			viewPagerCDrive.setVisibility(View.GONE);
				contactsSectionLayout.setVisibility(View.GONE);
    			viewPagerContacts.setVisibility(View.GONE);
    			viewPagerShares.setVisibility(View.GONE);
    			sharesSectionLayout.setVisibility(View.GONE);
				myAccountSectionLayout.setVisibility(View.GONE);
				viewPagerMyAccount.setVisibility(View.GONE);

				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, oFLol, "oFLol");
    			ft.commitNow();

    			drawerLayout.closeDrawer(Gravity.LEFT);

    			supportInvalidateOptionsMenu();
				showFabButton();
    			break;
    		}
    		case CAMERA_UPLOADS:{

    			tB.setVisibility(View.VISIBLE);

				log("FirstTimeCam: " + firstTimeCam);
    			if (cuFL == null){
    				cuFL = new CameraUploadFragmentLollipop();
    				cuFL.setIsList(isListCameraUploads);
    				cuFL.setIsLargeGrid(isLargeGridCameraUploads);
    				cuFL.setFirstTimeCam(firstTimeCam);
				}
				else{
					cuFL.setIsList(isListCameraUploads);
					cuFL.setIsLargeGrid(isLargeGridCameraUploads);
					cuFL.setFirstTimeCam(firstTimeCam);
				}

    			cloudSectionLayout.setVisibility(View.GONE);
    			viewPagerCDrive.setVisibility(View.GONE);
				contactsSectionLayout.setVisibility(View.GONE);
    			viewPagerContacts.setVisibility(View.GONE);
    			sharesSectionLayout.setVisibility(View.GONE);
    			viewPagerShares.setVisibility(View.GONE);
				myAccountSectionLayout.setVisibility(View.GONE);
				viewPagerMyAccount.setVisibility(View.GONE);

				FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
				Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("cuFLol");
				if (currentFragment != null) {
					fragTransaction.detach(currentFragment);
					fragTransaction.commitNowAllowingStateLoss();

					fragTransaction = getSupportFragmentManager().beginTransaction();
					fragTransaction.attach(currentFragment);
					fragTransaction.commitNowAllowingStateLoss();
				}
				else{
					fragTransaction.replace(R.id.fragment_container, cuFL, "cuFLol");
					fragTransaction.commitNowAllowingStateLoss();
				}

    			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
					if (!hasStoragePermission) {
						ActivityCompat.requestPermissions(this,
				                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
								Constants.REQUEST_WRITE_STORAGE);
					}

//					boolean hasCameraPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
//					if (!hasCameraPermission) {
//						ActivityCompat.requestPermissions(this,
//				                new String[]{Manifest.permission.CAMERA},
//				                ManagerActivityLollipop.REQUEST_CAMERA);
//					}

//					if (hasStoragePermission && hasCameraPermission){
					if (hasStoragePermission){
						firstTimeCam = false;
					}
				}
				else{
					firstTimeCam = false;
				}

				drawerLayout.closeDrawer(Gravity.LEFT);

    			supportInvalidateOptionsMenu();
				showFabButton();
      			break;
    		}
    		case MEDIA_UPLOADS:{

    			tB.setVisibility(View.VISIBLE);

    			if (muFLol == null){
//    				cuF = new CameraUploadFragmentLollipop(CameraUploadFragmentLollipop.TYPE_MEDIA);
    				muFLol = CameraUploadFragmentLollipop.newInstance(CameraUploadFragmentLollipop.TYPE_MEDIA);
    				muFLol.setIsList(isListCameraUploads);
    				muFLol.setIsLargeGrid(isLargeGridCameraUploads);
				}
				else{
					muFLol.setIsList(isListCameraUploads);
					muFLol.setIsLargeGrid(isLargeGridCameraUploads);
				}

    			cloudSectionLayout.setVisibility(View.GONE);
    			viewPagerCDrive.setVisibility(View.GONE);
				contactsSectionLayout.setVisibility(View.GONE);
    			viewPagerContacts.setVisibility(View.GONE);
    			sharesSectionLayout.setVisibility(View.GONE);
    			viewPagerShares.setVisibility(View.GONE);
				myAccountSectionLayout.setVisibility(View.GONE);
				viewPagerMyAccount.setVisibility(View.GONE);

				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, muFLol, "muFLol");
    			ft.commitNow();

				drawerLayout.closeDrawer(Gravity.LEFT);

    			supportInvalidateOptionsMenu();
				showFabButton();
      			break;
    		}
    		case INBOX:{

    			tB.setVisibility(View.VISIBLE);

    			if (iFLol == null){
    				iFLol = new InboxFragmentLollipop();
    			}
    			else{
					log("Inbox Fragment is not NULL");
//    				iFLol.setParentHandle(parentHandleInbox);
					MegaNode node = megaApi.getNodeByHandle(parentHandleInbox);
					log("Selected Inbox with parent: "+parentHandleInbox);
//					log("inSFLol deepBrowserTreeIncoming: "+deepBrowserTreeInbox);
					if (node != null){
						log("Go to inbox node: "+node.getName());
						iFLol.setParentHandle(parentHandleInbox);

						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleInbox), orderCloud);
						iFLol.setNodes(nodes);

						if(parentHandleInbox==megaApi.getInboxNode().getHandle()){
							aB.setTitle(getResources().getString(R.string.section_inbox));
							log("aB.setHomeAsUpIndicator_886");
							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
							firstNavigationLevel = true;
						}
						else{
							aB.setTitle(node.getName());
							log("indicator_arrow_back_893");
							aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
							firstNavigationLevel = false;
						}
					}
					else{
						log("The Node is NULL");
						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getInboxNode(), orderCloud);
						iFLol.setNodes(nodes);
						aB.setTitle(getResources().getString(R.string.section_inbox));
						log("aB.setHomeAsUpIndicator_16");
						aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
						firstNavigationLevel = true;
					}
				}

    			cloudSectionLayout.setVisibility(View.GONE);
    			viewPagerCDrive.setVisibility(View.GONE);
				contactsSectionLayout.setVisibility(View.GONE);
    			viewPagerContacts.setVisibility(View.GONE);
    			sharesSectionLayout.setVisibility(View.GONE);
    			viewPagerShares.setVisibility(View.GONE);
				myAccountSectionLayout.setVisibility(View.GONE);
				viewPagerMyAccount.setVisibility(View.GONE);

				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, iFLol, "iFLol");
    			ft.commitNow();

    			viewPagerContacts.setVisibility(View.GONE);
    			drawerLayout.closeDrawer(Gravity.LEFT);

    			supportInvalidateOptionsMenu();
				showFabButton();
    			break;
    		}
    		case SHARED_ITEMS:{

				selectDrawerItemSharedItems();
    			supportInvalidateOptionsMenu();
    			break;
    		}
    		case CONTACTS:{
				selectDrawerItemContacts();

				supportInvalidateOptionsMenu();
				showFabButton();
    			break;
    		}
    		case SETTINGS:{

    			tB.setVisibility(View.VISIBLE);

    			drawerLayout.closeDrawer(Gravity.LEFT);
    			aB.setTitle(getString(R.string.action_settings));
    			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
    			setFirstNavigationLevel(true);
    			supportInvalidateOptionsMenu();

				contactsSectionLayout.setVisibility(View.GONE);
    			viewPagerContacts.setVisibility(View.GONE);
    			sharesSectionLayout.setVisibility(View.GONE);
    			viewPagerShares.setVisibility(View.GONE);
    			cloudSectionLayout.setVisibility(View.GONE);
    			viewPagerCDrive.setVisibility(View.GONE);
				myAccountSectionLayout.setVisibility(View.GONE);
				viewPagerMyAccount.setVisibility(View.GONE);

    			Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    			if (currentFragment != null){
    				getSupportFragmentManager().beginTransaction().remove(currentFragment).commitNow();
    			}

    			if(sttFLol==null){
    				sttFLol = new SettingsFragmentLollipop();
    			}

    			android.app.FragmentTransaction ft = getFragmentManager().beginTransaction();
    			ft.replace(R.id.fragment_container, sttFLol, "sttF");
    			ft.commit();

				supportInvalidateOptionsMenu();
				showFabButton();
				break;
    		}
    		case SEARCH:{

    			tB.setVisibility(View.VISIBLE);

    			if (sFLol == null){
    				sFLol = new SearchFragmentLollipop();
        		}

				if (nV != null){
					Menu nVMenu = nV.getMenu();
					MenuItem hidden = nVMenu.findItem(R.id.navigation_item_hidden);
					resetNavigationViewMenu(nVMenu);
					hidden.setChecked(true);
				}

				log("SEARCH NODES: " + searchQuery);
    			searchNodes = megaApi.search(searchQuery);
				log("SEARCH NODES.size = " + searchNodes.size());

				for(int j=0; j<searchNodes.size();j++){
					MegaNode node = searchNodes.get(j);
					MegaNode parent = megaApi.getParentNode(node);
					if(parent!=null){
						if(parent.getHandle()==megaApi.getRootNode().getHandle()){
							log("The node: "+node.getName()+"_"+node.getHandle()+" is IN CLOUD");
						}
						else if(parent.getHandle()==megaApi.getInboxNode().getHandle()){
							log("The node: "+node.getName()+"_"+node.getHandle()+" is IN INBOX");
						}
						else{
							log("The node: "+node.getName()+"_"+node.getHandle()+"is ??");
						}
					}
					else{
						log("The node: "+node.getName()+"_"+node.getHandle()+" HAS null parent");
					}
				}
    			drawerItem = DrawerItem.SEARCH;

    			sFLol.setSearchNodes(searchNodes);
    			sFLol.setNodes(searchNodes);
    			sFLol.setSearchQuery(searchQuery);
    			sFLol.setParentHandle(parentHandleSearch);
    			sFLol.setLevels(levelsSearch);

    			cloudSectionLayout.setVisibility(View.GONE);
    			viewPagerCDrive.setVisibility(View.GONE);
				contactsSectionLayout.setVisibility(View.GONE);
    			viewPagerContacts.setVisibility(View.GONE);
    			sharesSectionLayout.setVisibility(View.GONE);
    			viewPagerShares.setVisibility(View.GONE);
				myAccountSectionLayout.setVisibility(View.GONE);
				viewPagerMyAccount.setVisibility(View.GONE);

				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, sFLol, "sFLol");
    			ft.commitNow();

				supportInvalidateOptionsMenu();
				showFabButton();
    			break;
    		}
			case ACCOUNT:{
				log("case ACCOUNT: "+accountFragment);
//    			tB.setVisibility(View.GONE);
				selectDrawerItemAccount();
				supportInvalidateOptionsMenu();
				break;
			}
    		case TRANSFERS:{

    			tB.setVisibility(View.VISIBLE);

				log("select TRANSFERS");

				drawerItem = DrawerItem.TRANSFERS;

				if (nV != null){
					Menu nVMenu = nV.getMenu();
					MenuItem hidden = nVMenu.findItem(R.id.navigation_item_hidden);
					resetNavigationViewMenu(nVMenu);
					hidden.setChecked(true);
				}

				contactsSectionLayout.setVisibility(View.GONE);
    			viewPagerContacts.setVisibility(View.GONE);
    			sharesSectionLayout.setVisibility(View.GONE);
    			viewPagerShares.setVisibility(View.GONE);
    			cloudSectionLayout.setVisibility(View.GONE);
    			viewPagerCDrive.setVisibility(View.GONE);
				myAccountSectionLayout.setVisibility(View.GONE);
				viewPagerMyAccount.setVisibility(View.GONE);

    			if (tFLol == null){
    				tFLol = new TransfersFragmentLollipop();
    			}

    			tFLol.setTransfers(megaApi.getTransfers());

    			if(megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD)||megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)){
    				tFLol.setPause(true);
    			}
    			else{
    				tFLol.setPause(false);
    			}

				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, tFLol, "tFLol");
    			ft.commitNow();

    			drawerLayout.closeDrawer(Gravity.LEFT);

    			supportInvalidateOptionsMenu();
				showFabButton();
    			break;
    		}
			case CHAT:{
				log("chat selected");
				selectDrawerItemChat();
				supportInvalidateOptionsMenu();
				showFabButton();
				break;
			}
    	}
	}

	private String getFragmentTag(int viewPagerId, int fragmentPosition){
	     return "android:switcher:" + viewPagerId + ":" + fragmentPosition;
	}

	private void getOverflowMenu() {
		log("getOverflowMenu");
	     try {
	        ViewConfiguration config = ViewConfiguration.get(this);
	        Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
	        if(menuKeyField != null) {
	            menuKeyField.setAccessible(true);
	            menuKeyField.setBoolean(config, false);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	public void showMyAccount(){
		drawerItem = DrawerItem.ACCOUNT;
		selectDrawerItemLollipop(drawerItem);
	}

	public void showCC(int type, MyAccountInfo myAccountInfo, int payMonth, boolean refresh){

		accountFragment = Constants.CC_FRAGMENT;
		contactsSectionLayout.setVisibility(View.GONE);
		viewPagerContacts.setVisibility(View.GONE);
		sharesSectionLayout.setVisibility(View.GONE);
		viewPagerShares.setVisibility(View.GONE);
		myAccountSectionLayout.setVisibility(View.GONE);
		viewPagerMyAccount.setVisibility(View.GONE);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		if (!refresh){
			if (ccFL == null){
				ccFL = new CreditCardFragmentLollipop();
				ccFL.setInfo(type, myAccountInfo, payMonth);
				ft.replace(R.id.fragment_container, ccFL, "ccF");
				ft.commitNow();
			}
			else{
				ccFL.setInfo(type, myAccountInfo, payMonth);
				ft.replace(R.id.fragment_container, ccFL, "ccF");
				ft.commitNow();
			}
		}
		else{
			Fragment tempF = getSupportFragmentManager().findFragmentByTag("ccF");
			if (tempF != null){
				ft.detach(tempF);
				ft.attach(tempF);
				ft.commitNowAllowingStateLoss();
			}
			else{
				if (ccFL == null){
					ccFL = new CreditCardFragmentLollipop();
					ccFL.setInfo(type, myAccountInfo, payMonth);
					ft.replace(R.id.fragment_container, ccFL, "ccF");
					ft.commitNow();
				}
				else{
					ccFL.setInfo(type, myAccountInfo, payMonth);
					ft.replace(R.id.fragment_container, ccFL, "ccF");
					ft.commitNow();
				}
			}
		}
	}

	public void updateInfoNumberOfSubscriptions(){
        if (cancelSubscription != null){
            cancelSubscription.setVisible(false);
        }
        if (myAccountInfo.getNumberOfSubscriptions() > 0){
            if (cancelSubscription != null){
                if (drawerItem == DrawerItem.ACCOUNT){
                    if (maFLol != null){
                        cancelSubscription.setVisible(true);
                    }
                }
            }
        }
    }

	public void showFortumo(){
		accountFragment = Constants.FORTUMO_FRAGMENT;
		contactsSectionLayout.setVisibility(View.GONE);
		viewPagerContacts.setVisibility(View.GONE);
		sharesSectionLayout.setVisibility(View.GONE);
		viewPagerShares.setVisibility(View.GONE);
		myAccountSectionLayout.setVisibility(View.GONE);
		viewPagerMyAccount.setVisibility(View.GONE);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		if (fFL == null){
			fFL = new FortumoFragmentLollipop();
			fFL.setMyAccountInfo(myAccountInfo);
			ft.replace(R.id.fragment_container,  fFL, "fF");
			ft.commitNow();
		}
		else{
			fFL.setMyAccountInfo(myAccountInfo);
			ft.replace(R.id.fragment_container, fFL, "fF");
			ft.commitNow();
		}
	}

	public void showCentili(){
		accountFragment = Constants.CENTILI_FRAGMENT;
		contactsSectionLayout.setVisibility(View.GONE);
		viewPagerContacts.setVisibility(View.GONE);
		sharesSectionLayout.setVisibility(View.GONE);
		viewPagerShares.setVisibility(View.GONE);
		myAccountSectionLayout.setVisibility(View.GONE);
		viewPagerMyAccount.setVisibility(View.GONE);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		if (ctFL == null){
			ctFL = new CentiliFragmentLollipop();
			ctFL.setMyAccountInfo(myAccountInfo);
			ft.replace(R.id.fragment_container,  ctFL, "ctF");
			ft.commitNow();
		}
		else{
			ctFL.setMyAccountInfo(myAccountInfo);
			ft.replace(R.id.fragment_container, ctFL, "ctF");
			ft.commitNow();
		}
	}

	public void showmyF(int paymentMethod, int type){
		log("showmyF");

		accountFragment = Constants.MONTHLY_YEARLY_FRAGMENT;

		cloudSectionLayout.setVisibility(View.GONE);
		viewPagerCDrive.setVisibility(View.GONE);
		contactsSectionLayout.setVisibility(View.GONE);
		viewPagerContacts.setVisibility(View.GONE);
		sharesSectionLayout.setVisibility(View.GONE);
		viewPagerShares.setVisibility(View.GONE);
		myAccountSectionLayout.setVisibility(View.GONE);
		viewPagerMyAccount.setVisibility(View.GONE);

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		if (myFL == null){
			myFL = new MonthlyAnnualyFragmentLollipop();
			myFL.setInfo(paymentMethod, type, myAccountInfo);
			ft.replace(R.id.fragment_container, myFL, "myF");
			ft.commitNow();
		}
		else{
			myFL.setInfo(paymentMethod, type, myAccountInfo);
			ft.replace(R.id.fragment_container, myFL, "myF");
			ft.commitNow();
		}
	}

	public void showUpAF(){
		log("showUpAF");

		accountFragment=Constants.UPGRADE_ACCOUNT_FRAGMENT;

		contactsSectionLayout.setVisibility(View.GONE);
		viewPagerContacts.setVisibility(View.GONE);
		sharesSectionLayout.setVisibility(View.GONE);
		viewPagerShares.setVisibility(View.GONE);
		cloudSectionLayout.setVisibility(View.GONE);
		viewPagerCDrive.setVisibility(View.GONE);
		myAccountSectionLayout.setVisibility(View.GONE);
		viewPagerMyAccount.setVisibility(View.GONE);

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		if(upAFL==null){
			upAFL = new UpgradeAccountFragmentLollipop();
			upAFL.setMyAccountInfo(myAccountInfo);
			ft.replace(R.id.fragment_container, upAFL, "upAFL");
			ft.commitNowAllowingStateLoss();
		}
		else{
			upAFL.setMyAccountInfo(myAccountInfo);
			ft.replace(R.id.fragment_container, upAFL, "upAFL");
			ft.commitNowAllowingStateLoss();
		}

		invalidateOptionsMenu();
		showFabButton();
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		log("onCreateOptionsMenuLollipop");

		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_manager, menu);
//	    getSupportActionBar().setDisplayShowCustomEnabled(true);

	    final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchMenuItem = menu.findItem(R.id.action_search);
		final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);

		if (searchView != null){
			searchView.setIconifiedByDefault(true);
		}

		gridSmallLargeMenuItem = menu.findItem(R.id.action_grid_view_large_small);
		addContactMenuItem =menu.findItem(R.id.action_add_contact);
		addMenuItem = menu.findItem(R.id.action_add);
		createFolderMenuItem = menu.findItem(R.id.action_new_folder);
		importLinkMenuItem = menu.findItem(R.id.action_import_link);
		selectMenuItem = menu.findItem(R.id.action_select);
		unSelectMenuItem = menu.findItem(R.id.action_unselect);
		thumbViewMenuItem= menu.findItem(R.id.action_grid);

		refreshMenuItem = menu.findItem(R.id.action_menu_refresh);
		sortByMenuItem = menu.findItem(R.id.action_menu_sort_by);
		helpMenuItem = menu.findItem(R.id.action_menu_help);
		upgradeAccountMenuItem = menu.findItem(R.id.action_menu_upgrade_account);
		rubbishBinMenuItem = menu.findItem(R.id.action_rubbish_bin);
		clearRubbishBinMenuitem = menu.findItem(R.id.action_menu_clear_rubbish_bin);
		cancelAllTransfersMenuItem = menu.findItem(R.id.action_menu_cancel_all_transfers);
		playTransfersMenuIcon = menu.findItem(R.id.action_play);
		pauseTransfersMenuIcon = menu.findItem(R.id.action_pause);
		cancelAllTransfersMenuItem.setVisible(false);

		changePass = menu.findItem(R.id.action_menu_change_pass);

		takePicture = menu.findItem(R.id.action_take_picture);

		cancelSubscription = menu.findItem(R.id.action_menu_cancel_subscriptions);
		cancelSubscription.setVisible(false);

		exportMK = menu.findItem(R.id.action_menu_export_MK);
		exportMK.setVisible(false);
		removeMK = menu.findItem(R.id.action_menu_remove_MK);
		removeMK.setVisible(false);

		killAllSessions = menu.findItem(R.id.action_menu_kill_all_sessions);
		killAllSessions.setVisible(false);

		logoutMenuItem = menu.findItem(R.id.action_menu_logout);
		logoutMenuItem.setVisible(false);

		forgotPassMenuItem = menu.findItem(R.id.action_menu_forgot_pass);
		forgotPassMenuItem.setVisible(false);

		newChatMenuItem= menu.findItem(R.id.action_menu_new_chat);

	    if (drawerItem == null){
	    	if (nV != null){
	    		Menu nVMenu = nV.getMenu();
	    		if (nVMenu != null){
	    			drawerItem = DrawerItem.CLOUD_DRIVE;
	    			resetNavigationViewMenu(nVMenu);
	    			drawerMenuItem = nVMenu.findItem(R.id.navigation_item_cloud_drive);
	    			if (drawerMenuItem != null){
	    				resetNavigationViewMenu(nVMenu);
	    				drawerMenuItem.setChecked(true);
	    				drawerMenuItem.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
	    				if (drawerLayout != null){
	    					drawerLayout.openDrawer(Gravity.LEFT);
	    				}
	    			}
	    		}

	    	}
	    	else{

	    	}
	    }
	    else{
	    	if (nV != null){
	    		Menu nVMenu = nV.getMenu();
	    		switch(drawerItem){
		    		case CLOUD_DRIVE:{
		    			drawerMenuItem = nVMenu.findItem(R.id.navigation_item_cloud_drive);
		    			if (drawerMenuItem != null){
		    				resetNavigationViewMenu(nVMenu);
		    				drawerMenuItem.setChecked(true);
		    				drawerMenuItem.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
		    			}
		    			break;
		    		}
	    		}
	    	}
	    }

	    if (drawerItem == DrawerItem.CLOUD_DRIVE){
			int index = viewPagerCDrive.getCurrentItem();
			log("----------------------------------------INDEX: "+index);
			if(index==1){
				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);
				rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
				if (rbFLol != null){
					//Show
	    			sortByMenuItem.setVisible(true);
	    			selectMenuItem.setVisible(true);
	    			thumbViewMenuItem.setVisible(true);
	    			clearRubbishBinMenuitem.setVisible(true);
	    			searchMenuItem.setVisible(true);

					//Hide
	    			refreshMenuItem.setVisible(false);
					pauseTransfersMenuIcon.setVisible(false);
					playTransfersMenuIcon.setVisible(false);
					log("createFolderMenuItem.setVisible_13");
					createFolderMenuItem.setVisible(false);
	    			addMenuItem.setVisible(false);
	    			addContactMenuItem.setVisible(false);
	    			upgradeAccountMenuItem.setVisible(true);
	    			unSelectMenuItem.setVisible(false);
	    			addMenuItem.setEnabled(false);
	    			changePass.setVisible(false);
	    			importLinkMenuItem.setVisible(false);
	    			takePicture.setVisible(false);
	    			refreshMenuItem.setVisible(false);
					helpMenuItem.setVisible(false);
					logoutMenuItem.setVisible(false);
					forgotPassMenuItem.setVisible(false);

	    			if (isList){
	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    			}

					rbFLol.setIsList(isList);
					rbFLol.setParentHandle(parentHandleRubbish);

					if(rbFLol.getItemCount()>0){
						selectMenuItem.setVisible(true);
						clearRubbishBinMenuitem.setVisible(true);
					}
					else{
						selectMenuItem.setVisible(false);
						clearRubbishBinMenuitem.setVisible(false);
					}

	    			rubbishBinMenuItem.setVisible(false);
	    			rubbishBinMenuItem.setTitle(getString(R.string.section_cloud_drive));
	    			gridSmallLargeMenuItem.setVisible(false);
				}
			}
			else{
				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
				if (fbFLol!=null){
					log("in CloudDrive");
					//Cloud Drive
					//Show
					addMenuItem.setEnabled(true);
					addMenuItem.setVisible(true);
					log("createFolderMenuItem.setVisible_14");
					createFolderMenuItem.setVisible(true);
					sortByMenuItem.setVisible(true);
					thumbViewMenuItem.setVisible(true);
					rubbishBinMenuItem.setVisible(false);
	    			upgradeAccountMenuItem.setVisible(true);
	    			importLinkMenuItem.setVisible(true);
	    			takePicture.setVisible(true);
	    			selectMenuItem.setVisible(true);
	    			searchMenuItem.setVisible(true);

					//Hide
					pauseTransfersMenuIcon.setVisible(false);
					playTransfersMenuIcon.setVisible(false);
	    			addContactMenuItem.setVisible(false);
	    			unSelectMenuItem.setVisible(false);
	    			clearRubbishBinMenuitem.setVisible(false);
	    			changePass.setVisible(false);
	    			refreshMenuItem.setVisible(false);
					helpMenuItem.setVisible(false);
					killAllSessions.setVisible(false);
					logoutMenuItem.setVisible(false);
					forgotPassMenuItem.setVisible(false);

					if(fbFLol.getItemCount()>0){
						selectMenuItem.setVisible(true);
					}
					else{
						selectMenuItem.setVisible(false);
					}

	    			if (isList){
	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    			}
	    			gridSmallLargeMenuItem.setVisible(false);
				}
			}
			newChatMenuItem.setVisible(false);
			return super.onCreateOptionsMenu(menu);
		}

	    else if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE){
			if (oFLol != null){
				//Show
    			sortByMenuItem.setVisible(true);
    			thumbViewMenuItem.setVisible(true);

    			if(oFLol.getItemCount()>0){
					selectMenuItem.setVisible(true);
				}
				else{
					selectMenuItem.setVisible(false);
				}
    			searchMenuItem.setVisible(true);

    			upgradeAccountMenuItem.setVisible(false);
				refreshMenuItem.setVisible(false);
				pauseTransfersMenuIcon.setVisible(false);
				playTransfersMenuIcon.setVisible(false);
				log("createFolderMenuItem.setVisible_15");
				createFolderMenuItem.setVisible(false);
				addContactMenuItem.setVisible(false);
    			addMenuItem.setVisible(false);
    			unSelectMenuItem.setVisible(false);
    			addMenuItem.setEnabled(false);
    			changePass.setVisible(false);
    			rubbishBinMenuItem.setVisible(false);
    			clearRubbishBinMenuitem.setVisible(false);
    			importLinkMenuItem.setVisible(false);
    			takePicture.setVisible(false);
    			refreshMenuItem.setVisible(false);
				helpMenuItem.setVisible(false);
				logoutMenuItem.setVisible(false);
				forgotPassMenuItem.setVisible(false);

    			if (isList){
    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
				}
				else{
					thumbViewMenuItem.setTitle(getString(R.string.action_list));
    			}
    			gridSmallLargeMenuItem.setVisible(false);
			}
			newChatMenuItem.setVisible(false);
		}

	    else if (drawerItem == DrawerItem.CAMERA_UPLOADS){
	    	if (cuFL != null){

				//Show
    			upgradeAccountMenuItem.setVisible(true);
    			selectMenuItem.setVisible(true);
    			takePicture.setVisible(true);

				//Hide
				pauseTransfersMenuIcon.setVisible(false);
				playTransfersMenuIcon.setVisible(false);
				log("createFolderMenuItem.setVisible_16");
				createFolderMenuItem.setVisible(false);
				addContactMenuItem.setVisible(false);
    			addMenuItem.setVisible(false);
    			refreshMenuItem.setVisible(false);
    			sortByMenuItem.setVisible(false);
    			unSelectMenuItem.setVisible(false);
    			thumbViewMenuItem.setVisible(true);
    			changePass.setVisible(false);
    			rubbishBinMenuItem.setVisible(false);
    			clearRubbishBinMenuitem.setVisible(false);
    			importLinkMenuItem.setVisible(false);
    			refreshMenuItem.setVisible(false);
				helpMenuItem.setVisible(false);
				logoutMenuItem.setVisible(false);
				forgotPassMenuItem.setVisible(false);

    			if (isListCameraUploads){
    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
    				gridSmallLargeMenuItem.setVisible(false);
    				searchMenuItem.setVisible(true);
				}
				else{
					thumbViewMenuItem.setTitle(getString(R.string.action_list));
					if (isLargeGridCameraUploads){
        				gridSmallLargeMenuItem.setIcon(getResources().getDrawable(R.drawable.ic_menu_gridview_small));
        			}
        			else{
        				gridSmallLargeMenuItem.setIcon(getResources().getDrawable(R.drawable.ic_menu_gridview));
        			}
					gridSmallLargeMenuItem.setVisible(true);
					searchMenuItem.setVisible(false);
    			}
			}
			newChatMenuItem.setVisible(false);
		}
	    else if (drawerItem == DrawerItem.MEDIA_UPLOADS){
	    	if (muFLol != null){

				//Show
    			upgradeAccountMenuItem.setVisible(true);
    			selectMenuItem.setVisible(true);
    			takePicture.setVisible(true);

				//Hide
				pauseTransfersMenuIcon.setVisible(false);
				playTransfersMenuIcon.setVisible(false);
				log("createFolderMenuItem.setVisible_17");
				createFolderMenuItem.setVisible(false);
				addContactMenuItem.setVisible(false);
    			addMenuItem.setVisible(false);
    			refreshMenuItem.setVisible(false);
    			sortByMenuItem.setVisible(false);
    			unSelectMenuItem.setVisible(false);
    			thumbViewMenuItem.setVisible(true);
    			changePass.setVisible(false);
    			rubbishBinMenuItem.setVisible(false);
    			clearRubbishBinMenuitem.setVisible(false);
    			importLinkMenuItem.setVisible(false);
    			refreshMenuItem.setVisible(false);
				helpMenuItem.setVisible(false);
				logoutMenuItem.setVisible(false);
				forgotPassMenuItem.setVisible(false);

    			if (isListCameraUploads){
    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
    				gridSmallLargeMenuItem.setVisible(false);
    				searchMenuItem.setVisible(true);
				}
				else{
					thumbViewMenuItem.setTitle(getString(R.string.action_list));
					if (isLargeGridCameraUploads){
        				gridSmallLargeMenuItem.setIcon(getResources().getDrawable(R.drawable.ic_menu_gridview_small));
        			}
        			else{
        				gridSmallLargeMenuItem.setIcon(getResources().getDrawable(R.drawable.ic_menu_gridview));
        			}
					gridSmallLargeMenuItem.setVisible(true);
					searchMenuItem.setVisible(false);
    			}
			}
			newChatMenuItem.setVisible(false);
		}

	    else if (drawerItem == DrawerItem.INBOX){
			if (iFLol != null){
				//Show
    			sortByMenuItem.setVisible(true);

    			if(iFLol.getItemCount()>0){
					selectMenuItem.setVisible(true);
				}
				else{
					selectMenuItem.setVisible(false);
				}

    			if (isList){
    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
				}
				else{
					thumbViewMenuItem.setTitle(getString(R.string.action_list));
    			}

    			searchMenuItem.setVisible(true);
    			thumbViewMenuItem.setVisible(true);

				//Hide
    			refreshMenuItem.setVisible(false);
				pauseTransfersMenuIcon.setVisible(false);
				playTransfersMenuIcon.setVisible(false);
				log("createFolderMenuItem.setVisible_18");
				createFolderMenuItem.setVisible(false);
    			addMenuItem.setVisible(false);
    			addContactMenuItem.setVisible(false);
    			upgradeAccountMenuItem.setVisible(true);
    			unSelectMenuItem.setVisible(false);
    			addMenuItem.setEnabled(false);
    			changePass.setVisible(false);
    			importLinkMenuItem.setVisible(false);
    			takePicture.setVisible(false);
    			refreshMenuItem.setVisible(false);
				helpMenuItem.setVisible(false);
    			clearRubbishBinMenuitem.setVisible(false);
    			rubbishBinMenuItem.setVisible(false);
    			gridSmallLargeMenuItem.setVisible(false);
				logoutMenuItem.setVisible(false);
				forgotPassMenuItem.setVisible(false);
				newChatMenuItem.setVisible(false);
			}
		}

	    else if (drawerItem == DrawerItem.SHARED_ITEMS){
			//Lollipop
			int index = viewPagerShares.getCurrentItem();
			if(index==0){
				String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 0);
				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
				if (inSFLol != null){
					sortByMenuItem.setVisible(true);
					thumbViewMenuItem.setVisible(true);

					addMenuItem.setEnabled(true);

					log("onCreateOptionsMenu parentHandleIncoming: "+parentHandleIncoming);
					if(parentHandleIncoming==-1){
						addMenuItem.setVisible(false);
						createFolderMenuItem.setVisible(false);
					}
					else{
						MegaNode node = megaApi.getNodeByHandle(parentHandleIncoming);
						if(node!=null){
							//Check the folder's permissions
							int accessLevel= megaApi.getAccess(node);
							log("onCreateOptionsMenu Node: "+node.getName());

							switch(accessLevel){
								case MegaShare.ACCESS_OWNER:
								case MegaShare.ACCESS_READWRITE:
								case MegaShare.ACCESS_FULL:{
									addMenuItem.setVisible(true);
									createFolderMenuItem.setVisible(true);
									break;
								}
								case MegaShare.ACCESS_READ:{
									addMenuItem.setVisible(false);
									createFolderMenuItem.setVisible(false);
									break;
								}
							}
						}
						else{
							addMenuItem.setVisible(false);
							createFolderMenuItem.setVisible(false);
						}
					}

					if(inSFLol.getItemCount()>0){
						selectMenuItem.setVisible(true);
					}
					else{
						selectMenuItem.setVisible(false);
					}
					searchMenuItem.setVisible(true);

					//Hide
					pauseTransfersMenuIcon.setVisible(false);
					playTransfersMenuIcon.setVisible(false);
					addContactMenuItem.setVisible(false);
					unSelectMenuItem.setVisible(false);
					rubbishBinMenuItem.setVisible(false);
					clearRubbishBinMenuitem.setVisible(false);
					changePass.setVisible(false);
					importLinkMenuItem.setVisible(false);
					takePicture.setVisible(false);
	    			refreshMenuItem.setVisible(false);
					helpMenuItem.setVisible(false);
					upgradeAccountMenuItem.setVisible(true);
					gridSmallLargeMenuItem.setVisible(false);
					logoutMenuItem.setVisible(false);
					forgotPassMenuItem.setVisible(false);

					if (isList){
	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    			}
				}
			}
			else if(index==1){
				String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 1);
				outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
				if (outSFLol != null){

					sortByMenuItem.setVisible(true);
					thumbViewMenuItem.setVisible(true);

					log("parentHandleOutgoing: "+parentHandleOutgoing);
					if(parentHandleOutgoing==-1){
						addMenuItem.setVisible(false);
						createFolderMenuItem.setVisible(false);
					}
					else{
						addMenuItem.setVisible(true);
						createFolderMenuItem.setVisible(true);
					}

					if(outSFLol.getItemCount()>0){
						selectMenuItem.setVisible(true);
					}
					else{
						selectMenuItem.setVisible(false);
					}
					searchMenuItem.setVisible(true);

					//Hide
					upgradeAccountMenuItem.setVisible(true);
					pauseTransfersMenuIcon.setVisible(false);
					playTransfersMenuIcon.setVisible(false);
					addContactMenuItem.setVisible(false);
					unSelectMenuItem.setVisible(false);
					rubbishBinMenuItem.setVisible(false);
					clearRubbishBinMenuitem.setVisible(false);
					changePass.setVisible(false);
					importLinkMenuItem.setVisible(false);
					takePicture.setVisible(false);
	    			refreshMenuItem.setVisible(false);
					helpMenuItem.setVisible(false);
					gridSmallLargeMenuItem.setVisible(false);
					logoutMenuItem.setVisible(false);
					forgotPassMenuItem.setVisible(false);

					if (isList){
	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    			}
				}
			}
			newChatMenuItem.setVisible(false);
		}

	    else if (drawerItem == DrawerItem.CONTACTS){
			log("createOptions CONTACTS");
			int index = viewPagerContacts.getCurrentItem();
			if (index == 0){
				log("createOptions TAB CONTACTS");
				//Show
				addContactMenuItem.setVisible(true);
				selectMenuItem.setVisible(true);
				sortByMenuItem.setVisible(true);
				thumbViewMenuItem.setVisible(true);
				upgradeAccountMenuItem.setVisible(true);
				searchMenuItem.setVisible(true);
				newChatMenuItem.setVisible(true);

				//Hide
				pauseTransfersMenuIcon.setVisible(false);
				playTransfersMenuIcon.setVisible(false);
				log("createFolderMenuItem.setVisible_21");
				createFolderMenuItem.setVisible(false);
				addMenuItem.setVisible(false);
				unSelectMenuItem.setVisible(false);
				addMenuItem.setEnabled(false);
				changePass.setVisible(false);
				rubbishBinMenuItem.setVisible(false);
				clearRubbishBinMenuitem.setVisible(false);
				importLinkMenuItem.setVisible(false);
				takePicture.setVisible(false);
				refreshMenuItem.setVisible(false);
				helpMenuItem.setVisible(false);
				logoutMenuItem.setVisible(false);
				changePass.setVisible(false);
				killAllSessions.setVisible(false);
				forgotPassMenuItem.setVisible(false);

				if (isList){
					thumbViewMenuItem.setTitle(getString(R.string.action_grid));
				}
				else{
					thumbViewMenuItem.setTitle(getString(R.string.action_list));
				}

				gridSmallLargeMenuItem.setVisible(false);
			}
			else if (index == 1){
				log("createOptions TAB SENT requests");
				//Show
				selectMenuItem.setVisible(true);
				addContactMenuItem.setVisible(true);
				upgradeAccountMenuItem.setVisible(true);

				//Hide
				sortByMenuItem.setVisible(false);
				thumbViewMenuItem.setVisible(false);
				searchMenuItem.setVisible(false);
				pauseTransfersMenuIcon.setVisible(false);
				playTransfersMenuIcon.setVisible(false);
				log("createFolderMenuItem.setVisible_21");
				createFolderMenuItem.setVisible(false);
				addMenuItem.setVisible(false);
				unSelectMenuItem.setVisible(false);
				addMenuItem.setEnabled(false);
				changePass.setVisible(false);
				rubbishBinMenuItem.setVisible(false);
				clearRubbishBinMenuitem.setVisible(false);
				importLinkMenuItem.setVisible(false);
				takePicture.setVisible(false);
				refreshMenuItem.setVisible(false);
				helpMenuItem.setVisible(false);
				logoutMenuItem.setVisible(false);
				changePass.setVisible(false);
				killAllSessions.setVisible(false);
				thumbViewMenuItem.setVisible(false);
				gridSmallLargeMenuItem.setVisible(false);
				forgotPassMenuItem.setVisible(false);
			}
			else{
				log("createOptions TAB RECEIVED requests");
				//Show
				selectMenuItem.setVisible(true);
    			upgradeAccountMenuItem.setVisible(true);

    			//Hide
				searchMenuItem.setVisible(false);
    			addContactMenuItem.setVisible(false);
				sortByMenuItem.setVisible(false);
				thumbViewMenuItem.setVisible(false);
				pauseTransfersMenuIcon.setVisible(false);
				playTransfersMenuIcon.setVisible(false);
				log("createFolderMenuItem.setVisible_22");
				createFolderMenuItem.setVisible(false);
    			addMenuItem.setVisible(false);
    			unSelectMenuItem.setVisible(false);
    			addMenuItem.setEnabled(false);
    			changePass.setVisible(false);
    			rubbishBinMenuItem.setVisible(false);
    			clearRubbishBinMenuitem.setVisible(false);
    			importLinkMenuItem.setVisible(false);
    			takePicture.setVisible(false);
    			refreshMenuItem.setVisible(false);
				helpMenuItem.setVisible(false);
				gridSmallLargeMenuItem.setVisible(false);
				thumbViewMenuItem.setVisible(false);
				logoutMenuItem.setVisible(false);
				killAllSessions.setVisible(false);
				logoutMenuItem.setVisible(false);
				forgotPassMenuItem.setVisible(false);
			}
		}

	    else if (drawerItem == DrawerItem.SEARCH){
			log("createOptions search");
	    	if (sFLol != null){
				if (createFolderMenuItem != null){

					//Hide
	    			upgradeAccountMenuItem.setVisible(true);
					cancelAllTransfersMenuItem.setVisible(false);
					pauseTransfersMenuIcon.setVisible(false);
					playTransfersMenuIcon.setVisible(false);
					log("createFolderMenuItem.setVisible_23");
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
					newChatMenuItem.setVisible(false);

					//Show
					if(sFLol.getNodes()!=null){
						if(sFLol.getNodes().size()!=0){
//							log("size after search: "+sFLol.getNodes().size());
							selectMenuItem.setVisible(true);
							thumbViewMenuItem.setVisible(true);
							if (isList){
								thumbViewMenuItem.setTitle(getString(R.string.action_grid));
							}
							else{
								thumbViewMenuItem.setTitle(getString(R.string.action_list));
							}
						}
						else{
							selectMenuItem.setVisible(false);
							thumbViewMenuItem.setVisible(false);
						}
					}
				}
			}
		}

	    else if (drawerItem == DrawerItem.ACCOUNT){
			log("createOptions ACCOUNT");

			if (createFolderMenuItem != null) {

				//Hide
				helpMenuItem.setVisible(false);
				pauseTransfersMenuIcon.setVisible(false);
				playTransfersMenuIcon.setVisible(false);
				log("createFolderMenuItem.setVisible_24");
				createFolderMenuItem.setVisible(false);
				addContactMenuItem.setVisible(false);
				addMenuItem.setVisible(false);
				sortByMenuItem.setVisible(false);
				selectMenuItem.setVisible(false);
				unSelectMenuItem.setVisible(false);
				thumbViewMenuItem.setVisible(false);
				rubbishBinMenuItem.setVisible(false);
				clearRubbishBinMenuitem.setVisible(false);
				importLinkMenuItem.setVisible(false);
				takePicture.setVisible(false);
				cancelAllTransfersMenuItem.setVisible(false);
				gridSmallLargeMenuItem.setVisible(false);
				newChatMenuItem.setVisible(false);

				if(accountFragment==Constants.MY_ACCOUNT_FRAGMENT){
					//Show
					refreshMenuItem.setVisible(true);
					killAllSessions.setVisible(true);
					upgradeAccountMenuItem.setVisible(true);
					changePass.setVisible(true);
					logoutMenuItem.setVisible(true);
					forgotPassMenuItem.setVisible(true);

					int index = viewPagerMyAccount.getCurrentItem();
					if(index==0){
						String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.rKFile;
						log("Exists MK in: "+path);
						File file= new File(path);
						if(file.exists()){
							removeMK.setVisible(true);
							exportMK.setVisible(false);
						}
						else{
							removeMK.setVisible(false);
							exportMK.setVisible(true);
						}
					}
					else{
						removeMK.setVisible(false);
						exportMK.setVisible(false);
					}

					if (myAccountInfo.getNumberOfSubscriptions() > 0) {
						cancelSubscription.setVisible(true);
					}
					else{
						cancelSubscription.setVisible(false);
					}

				}
				else{

					refreshMenuItem.setVisible(true);
					killAllSessions.setVisible(false);
					upgradeAccountMenuItem.setVisible(false);
					changePass.setVisible(false);
					logoutMenuItem.setVisible(true);
					forgotPassMenuItem.setVisible(false);

					cancelSubscription.setVisible(false);
					removeMK.setVisible(false);
					exportMK.setVisible(false);
				}
			}
		}

	    else if (drawerItem == DrawerItem.TRANSFERS){
	    	log("in Transfers Section");
			if (tFLol != null){
				searchMenuItem.setVisible(false);
				//Hide
				createFolderMenuItem.setVisible(false);
				addContactMenuItem.setVisible(false);
    			addMenuItem.setVisible(false);
    			sortByMenuItem.setVisible(false);
    			selectMenuItem.setVisible(false);
    			unSelectMenuItem.setVisible(false);
    			thumbViewMenuItem.setVisible(false);
    			rubbishBinMenuItem.setVisible(false);
    			clearRubbishBinMenuitem.setVisible(false);
    			importLinkMenuItem.setVisible(false);
    			takePicture.setVisible(false);
				refreshMenuItem.setVisible(false);
				helpMenuItem.setVisible(false);
				upgradeAccountMenuItem.setVisible(true);
				changePass.setVisible(false);
				cancelSubscription.setVisible(false);
				killAllSessions.setVisible(false);
				logoutMenuItem.setVisible(false);
				forgotPassMenuItem.setVisible(false);
				newChatMenuItem.setVisible(false);

				cancelAllTransfersMenuItem.setVisible(true);

//				if(tranfersPaused){
//					playTransfersMenuIcon.setVisible(true);
//					pauseTransfersMenuIcon.setVisible(false);
//				}
//				else{
//					playTransfersMenuIcon.setVisible(true);
//					pauseTransfersMenuIcon.setVisible(false);
//				}
				ArrayList<MegaTransfer> transfersInProgress = megaApi.getTransfers();
				if(transfersInProgress!=null){
					if(transfersInProgress.size()>0){

		        		if(megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD)||megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)){
		        			log("Any transfer is paused");
		        			playTransfersMenuIcon.setVisible(true);
							pauseTransfersMenuIcon.setVisible(false);
							cancelAllTransfersMenuItem.setVisible(true);
		        		}
		        		else{
		        			log("No transfers paused");
		        			playTransfersMenuIcon.setVisible(false);
							pauseTransfersMenuIcon.setVisible(true);
							cancelAllTransfersMenuItem.setVisible(true);
		        		}
					}
					else{
						playTransfersMenuIcon.setVisible(false);
						pauseTransfersMenuIcon.setVisible(false);
						cancelAllTransfersMenuItem.setVisible(false);
					}
				}
				else{
					playTransfersMenuIcon.setVisible(false);
					pauseTransfersMenuIcon.setVisible(false);
					cancelAllTransfersMenuItem.setVisible(false);
				}
			}
	    }

	    else if (drawerItem == DrawerItem.SETTINGS){
	    	log("in Settings Section");
			if (sttFLol != null){
				searchMenuItem.setVisible(false);
				//Hide
				log("createFolderMenuItem.setVisible_settings");
				createFolderMenuItem.setVisible(false);
				addContactMenuItem.setVisible(false);
    			addMenuItem.setVisible(false);
    			sortByMenuItem.setVisible(false);
    			selectMenuItem.setVisible(false);
    			unSelectMenuItem.setVisible(false);
    			thumbViewMenuItem.setVisible(false);
    			addMenuItem.setEnabled(false);
    			rubbishBinMenuItem.setVisible(false);
    			clearRubbishBinMenuitem.setVisible(false);
    			importLinkMenuItem.setVisible(false);
    			takePicture.setVisible(false);
				refreshMenuItem.setVisible(false);
				helpMenuItem.setVisible(false);
				upgradeAccountMenuItem.setVisible(true);
				changePass.setVisible(false);
				cancelSubscription.setVisible(false);
				killAllSessions.setVisible(false);
				logoutMenuItem.setVisible(false);
				cancelAllTransfersMenuItem.setVisible(false);
				forgotPassMenuItem.setVisible(false);
				playTransfersMenuIcon.setVisible(false);
				pauseTransfersMenuIcon.setVisible(false);
				newChatMenuItem.setVisible(false);
			}
	    }
		else if (drawerItem == DrawerItem.CHAT){
			log("in Chat Section");
			ChatController chatController = new ChatController(this);
			if(Util.isChatEnabled()){

				if (rChatFL != null){
					newChatMenuItem.setVisible(true);
					addContactMenuItem.setVisible(true);
					selectMenuItem.setVisible(true);
				}

				//Hide
				searchMenuItem.setVisible(false);
				createFolderMenuItem.setVisible(false);
				addMenuItem.setVisible(false);
				sortByMenuItem.setVisible(false);
				unSelectMenuItem.setVisible(false);
				thumbViewMenuItem.setVisible(false);
				addMenuItem.setEnabled(false);
				rubbishBinMenuItem.setVisible(false);
				clearRubbishBinMenuitem.setVisible(false);
				importLinkMenuItem.setVisible(false);
				takePicture.setVisible(false);
				refreshMenuItem.setVisible(false);
				helpMenuItem.setVisible(false);
				upgradeAccountMenuItem.setVisible(false);
				changePass.setVisible(false);
				cancelSubscription.setVisible(false);
				killAllSessions.setVisible(false);
				logoutMenuItem.setVisible(false);
				cancelAllTransfersMenuItem.setVisible(false);
				forgotPassMenuItem.setVisible(false);
				playTransfersMenuIcon.setVisible(false);
				pauseTransfersMenuIcon.setVisible(false);
			}
			else{
				//Hide ALL
				newChatMenuItem.setVisible(false);
				addContactMenuItem.setVisible(false);
				selectMenuItem.setVisible(false);
				searchMenuItem.setVisible(false);
				createFolderMenuItem.setVisible(false);
				addMenuItem.setVisible(false);
				sortByMenuItem.setVisible(false);
				unSelectMenuItem.setVisible(false);
				thumbViewMenuItem.setVisible(false);
				addMenuItem.setEnabled(false);
				rubbishBinMenuItem.setVisible(false);
				clearRubbishBinMenuitem.setVisible(false);
				importLinkMenuItem.setVisible(false);
				takePicture.setVisible(false);
				refreshMenuItem.setVisible(false);
				helpMenuItem.setVisible(false);
				upgradeAccountMenuItem.setVisible(false);
				changePass.setVisible(false);
				cancelSubscription.setVisible(false);
				killAllSessions.setVisible(false);
				logoutMenuItem.setVisible(false);
				cancelAllTransfersMenuItem.setVisible(false);
				forgotPassMenuItem.setVisible(false);
				playTransfersMenuIcon.setVisible(false);
				pauseTransfersMenuIcon.setVisible(false);
			}
		}
		log("Call to super onCreateOptionsMenu");
	    return super.onCreateOptionsMenu(menu);
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		fromTakePicture = false;
		log("onOptionsItemSelectedLollipop");
		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}

		if (megaApi != null){
			log("---------retryPendingConnections");
			megaApi.retryPendingConnections();
		}

		int id = item.getItemId();
		switch(id){
			case android.R.id.home:{
				if (firstNavigationLevel){
					log("firstNavigationLevel is TRUE");
					drawerLayout.openDrawer(nV);
				}
				else{
					log("NOT firstNavigationLevel");
		    		if (drawerItem == DrawerItem.CLOUD_DRIVE){
		    			int index = viewPagerCDrive.getCurrentItem();
		    			if(index==1){
		    				//Rubbish Bin
		    				String cFTag2 = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);
		    				log("Tag: "+ cFTag2);
		    				rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag2);
		    				if (rbFLol != null){
		    					rbFLol.onBackPressed();
		    					return true;
		    				}
		    			}
		    			else{
		    				//Cloud Drive
		    				String cFTag1 = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
		    				log("Tag: "+ cFTag1);
		    				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag1);
		    				if (fbFLol != null){
		    					fbFLol.onBackPressed();
		    				}
		    			}
		    		}
		    		else if (drawerItem == DrawerItem.SHARED_ITEMS){
		    			int index = viewPagerShares.getCurrentItem();
		    			if(index==1){
		    				//OUTGOING
		    				String cFTag2 = getFragmentTag(R.id.shares_tabs_pager, 1);
		    				log("Tag: "+ cFTag2);
		    				outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag2);
		    				if (outSFLol != null){
		    					outSFLol.onBackPressed();
		    				}
		    			}
		    			else{
		    				//InCOMING
		    				String cFTag1 = getFragmentTag(R.id.shares_tabs_pager, 0);
		    				log("Tag: "+ cFTag1);
		    				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag1);
		    				log("deepBrowserTreeIncoming: "+deepBrowserTreeIncoming);

							if (inSFLol != null){
								log("deepBrowserTree get from inSFlol: "+inSFLol.getDeepBrowserTree());
		    					inSFLol.onBackPressed();
		    				}
		    			}
		    		}
		    		else if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE){
		    			if (oFLol != null){
		    				oFLol.onBackPressed();
		    				return true;
		    			}
		    		}
					else if (drawerItem == DrawerItem.INBOX){
						if (iFLol != null){
							iFLol.onBackPressed();
							return true;
						}
					}
		    		else if (drawerItem == DrawerItem.SEARCH){
		    			if (sFLol != null){
		    				sFLol.onBackPressed();
		    				return true;
		    			}
		    		}
		    		else if (drawerItem == DrawerItem.TRANSFERS){
		    			if (tFLol != null){
		        			if (tFLol.onBackPressed() == 0){
		        				drawerItem = DrawerItem.CLOUD_DRIVE;
		    					if (nV != null){
		    						Menu nVMenu = nV.getMenu();
		    						MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
		    						resetNavigationViewMenu(nVMenu);
		    						cloudDrive.setChecked(true);
		    						cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
		    					}
		        				selectDrawerItemLollipop(drawerItem);
		        				return true;
		        			}
		        		}
		    		}
					else if (drawerItem == DrawerItem.ACCOUNT){
						if(accountFragment==Constants.UPGRADE_ACCOUNT_FRAGMENT){

						}
						else if(accountFragment==Constants.MONTHLY_YEARLY_FRAGMENT){

						}

						switch(accountFragment){
							case Constants.UPGRADE_ACCOUNT_FRAGMENT:{
								log("Back to MyAccountFragment");
								setFirstNavigationLevel(true);
								displayedAccountType=-1;
								if (upAFL != null){
									drawerItem = DrawerItem.ACCOUNT;
									accountFragment=Constants.MY_ACCOUNT_FRAGMENT;
									selectDrawerItemLollipop(drawerItem);
									if (nV != null){
										Menu nVMenu = nV.getMenu();
										MenuItem hidden = nVMenu.findItem(R.id.navigation_item_hidden);
										resetNavigationViewMenu(nVMenu);
										hidden.setChecked(true);
									}
								}
								return true;
							}
							case Constants.CC_FRAGMENT:{
								if (ccFL != null){
									displayedAccountType = ccFL.getParameterType();
								}
								showUpAF();
								return true;
							}
							case Constants.MONTHLY_YEARLY_FRAGMENT:{
								if (myFL != null){
									myFL.onBackPressed();
								}
								return true;
							}
						}


//						if (tFLol != null){
//							if (tFLol.onBackPressed() == 0){
//								drawerItem = DrawerItem.CLOUD_DRIVE;
//								if (nV != null){
//									Menu nVMenu = nV.getMenu();
//									MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
//									resetNavigationViewMenu(nVMenu);
//									cloudDrive.setChecked(true);
//									cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
//								}
//								selectDrawerItemLollipop(drawerItem);
//								return true;
//							}
//						}
					}
				}
		    	return true;
		    }
		    case R.id.action_import_link:{
		    	showImportLinkDialog();
		    	return true;
		    }
		    case R.id.action_take_picture:{
		    	fromTakePicture = true;
		    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
					if (!hasStoragePermission) {
						ActivityCompat.requestPermissions(this,
				                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
								Constants.REQUEST_WRITE_STORAGE);
					}

					boolean hasCameraPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
					if (!hasCameraPermission) {
						ActivityCompat.requestPermissions(this,
				                new String[]{Manifest.permission.CAMERA},
								Constants.REQUEST_CAMERA);
					}

					if (hasStoragePermission && hasCameraPermission){
						this.takePicture();
					}
				}
		    	else{
		    		this.takePicture();
		    	}

		    	return true;
		    }
		    case R.id.action_menu_cancel_all_transfers:{

		    	Intent tempIntentDownload = null;
		    	Intent tempIntentUpload = null;
				String title = null;
				String text = null;

				tempIntentUpload = new Intent(this, UploadService.class);
				tempIntentUpload.setAction(UploadService.ACTION_CANCEL);
				tempIntentDownload = new Intent(this, DownloadService.class);
				tempIntentDownload.setAction(DownloadService.ACTION_CANCEL);
				title = getString(R.string.menu_cancel_all_transfers);
				text = getString(R.string.cancel_all_transfer_confirmation);

				final Intent cancelIntentDownload = tempIntentDownload;
				final Intent cancelIntentUpload = tempIntentUpload;
				AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//				builder.setTitle(title);
	            builder.setMessage(text);
				builder.setPositiveButton(getString(R.string.general_yes),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								if (tFLol != null){
									if (tFLol.isVisible()){
										tFLol.setNoActiveTransfers();
									}
								}
								startService(cancelIntentDownload);
								startService(cancelIntentUpload);
							}
						});
				builder.setNegativeButton(getString(R.string.general_no), null);
				final AlertDialog dialog = builder.create();
				try {
					dialog.show();
				}
				catch(Exception ex)	{
					startService(cancelIntentDownload);
					startService(cancelIntentUpload);
				}

		    	return true;
		    }
	        case R.id.action_pause:{
	        	if (drawerItem == DrawerItem.TRANSFERS){
	        		log("Click on action_pause - play visible");
	        		megaApi.pauseTransfers(true, this);
	        		pauseTransfersMenuIcon.setVisible(false);
	        		playTransfersMenuIcon.setVisible(true);
//    				if(!tranfersPaused)
//    				{
//    					tranfersPaused = true;
//    					pauseTransfersMenuIcon.setVisible(false);
//    					playTransfersMenuIcon.setVisible(true);
//
//    					//Update the progress in fragments
//    					if (fbFLol != null){
//    						fbFLol.updateProgressBar(progressPercent);
//    					}
//    					if (rbFLol != null){
//    						rbFLol.updateProgressBar(progressPercent);
//    					}
//    					if (iFLol != null){
//    						iFLol.updateProgressBar(progressPercent);
//    					}
//    					if (outSFLol != null){
//    						outSFLol.updateProgressBar(progressPercent);
//    					}
//    					if (inSFLol != null){
//    						inSFLol.updateProgressBar(progressPercent);
//    					}
//    					if (tFLol != null){
//    						tFLol.updateProgressBar(progressPercent);
//    					}
//
//    	    			megaApi.pauseTransfers(true, this);
//    				}
	        	}

	        	return true;
	        }
	        case R.id.action_play:{
	        	log("Click on action_play - pause visible");
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
			case R.id.action_menu_new_chat:{
				if (drawerItem == DrawerItem.CHAT){
					log("Create new chat");
					chooseAddContactDialog(true);
				}

				return true;
			}
	        case R.id.action_menu_kill_all_sessions:{
				aC.killAllSessions(this);
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
	    			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
	    			if (!hasStoragePermission) {
	    				ActivityCompat.requestPermissions(this,
	    		                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
								Constants.REQUEST_WRITE_STORAGE);
	    			}
	    		}

	        	if (drawerItem == DrawerItem.SHARED_ITEMS){
	        		String swmTag = getFragmentTag(R.id.shares_tabs_pager, 0);
	        		inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(swmTag);
	        		if (viewPagerShares.getCurrentItem()==0){
		        		if (inSFLol != null){
		        			Long checkHandle = inSFLol.getParentHandle();
		        			MegaNode checkNode = megaApi.getNodeByHandle(checkHandle);

		        			if((megaApi.checkAccess(checkNode, MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK)){
		        				this.showUploadPanel();
							}
							else if(megaApi.checkAccess(checkNode, MegaShare.ACCESS_READWRITE).getErrorCode() == MegaError.API_OK){
								this.showUploadPanel();
							}
							else if(megaApi.checkAccess(checkNode, MegaShare.ACCESS_READ).getErrorCode() == MegaError.API_OK){
								log("Not permissions to upload");
								AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
					            builder.setMessage(getString(R.string.no_permissions_upload));
//								builder.setTitle(R.string.op_not_allowed);
								builder.setCancelable(false).setPositiveButton(R.string.cam_sync_ok, new DialogInterface.OnClickListener() {
							           public void onClick(DialogInterface dialog, int id) {
							                //do things
							        	   alertNotPermissionsUpload.dismiss();
							           }
							       });

								alertNotPermissionsUpload = builder.create();
								alertNotPermissionsUpload.show();
//								Util.brandAlertDialog(alertNotPermissionsUpload);
							}
		        		}
	        		}
	        		swmTag = getFragmentTag(R.id.shares_tabs_pager, 1);
	        		outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(swmTag);
	        		if (viewPagerShares.getCurrentItem()==1){
		        		if (outSFLol != null){
		        			this.showUploadPanel();
		        		}
	        		}
	        	}
	        	else {
        			this.showUploadPanel();
	        	}

	        	return true;
	        }
	        case R.id.action_select:{
	        	//TODO: multiselect

        		if (drawerItem == DrawerItem.CLOUD_DRIVE){
        			int index = viewPagerCDrive.getCurrentItem();
        			log("----------------------------------------INDEX: "+index);
        			if(index==1){
        				//Rubbish bin
        				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);
        				rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
        				if (rbFLol != null){
            				rbFLol.selectAll();
            				if (rbFLol.showSelectMenuItem()){
            					selectMenuItem.setVisible(true);
            					unSelectMenuItem.setVisible(false);
            				}
            				else{
            					selectMenuItem.setVisible(false);
            					unSelectMenuItem.setVisible(true);
            				}
            			}
        			}
        			else{
        				//Cloud Drive
        				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
        				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
        				if (fbFLol != null){
        					fbFLol.selectAll();
                			if (fbFLol.showSelectMenuItem()){
                				selectMenuItem.setVisible(true);
                				unSelectMenuItem.setVisible(false);
                			}
                			else{
                				selectMenuItem.setVisible(false);
                				unSelectMenuItem.setVisible(true);
                			}
        				}
        			}

        			return true;
	        	}
	        	if (drawerItem == DrawerItem.CONTACTS){
					int index = viewPagerContacts.getCurrentItem();
					log("----------------------------------------INDEX: "+index);
					switch(index){
						case 0:{
							String cFTag = getFragmentTag(R.id.contact_tabs_pager, 0);
							cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
							if (cFLol != null){
								cFLol.selectAll();
								if (cFLol.showSelectMenuItem()){
									selectMenuItem.setVisible(true);
									unSelectMenuItem.setVisible(false);
								}
								else{
									selectMenuItem.setVisible(false);
									unSelectMenuItem.setVisible(true);
								}
							}
							break;
						}
						case 1:{
							String cFTag = getFragmentTag(R.id.contact_tabs_pager, 1);
							sRFLol = (SentRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
							if (sRFLol != null){
								sRFLol.selectAll();
								if (sRFLol.showSelectMenuItem()){
									selectMenuItem.setVisible(true);
									unSelectMenuItem.setVisible(false);
								}
								else{
									selectMenuItem.setVisible(false);
									unSelectMenuItem.setVisible(true);
								}
							}
							break;
						}
						case 2:{
							String cFTag = getFragmentTag(R.id.contact_tabs_pager, 2);
							rRFLol = (ReceivedRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
							if (rRFLol != null){
								rRFLol.selectAll();
								if (rRFLol.showSelectMenuItem()){
									selectMenuItem.setVisible(true);
									unSelectMenuItem.setVisible(false);
								}
								else{
									selectMenuItem.setVisible(false);
									unSelectMenuItem.setVisible(true);
								}
							}
							break;
						}

					}
	        	}
	        	if (drawerItem == DrawerItem.SHARED_ITEMS){
	        		String swmTag = getFragmentTag(R.id.shares_tabs_pager, 0);
	        		inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(swmTag);
	        		if (viewPagerShares.getCurrentItem()==0){
		        		if (inSFLol != null){
		        			inSFLol.selectAll();
		        			if (inSFLol.showSelectMenuItem()){
		        				selectMenuItem.setVisible(true);
		        				unSelectMenuItem.setVisible(false);
		        			}
		        			else{
		        				selectMenuItem.setVisible(false);
		        				unSelectMenuItem.setVisible(true);
		        			}
		        		}
	        		}
	        		swmTag = getFragmentTag(R.id.shares_tabs_pager, 1);
	        		outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(swmTag);
	        		if (viewPagerShares.getCurrentItem()==1){
		        		if (outSFLol != null){
		        			outSFLol.selectAll();
		        			if (outSFLol.showSelectMenuItem()){
		        				selectMenuItem.setVisible(true);
		        				unSelectMenuItem.setVisible(false);
		        			}
		        			else{
		        				selectMenuItem.setVisible(false);
		        				unSelectMenuItem.setVisible(true);
		        			}
	        			}
	        		}
        			return true;
	        	}
	        	if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE){
	        		if (oFLol != null){
	    				oFLol.selectAll();
	    				if (oFLol.showSelectMenuItem()){
	        				selectMenuItem.setVisible(true);
	        				unSelectMenuItem.setVisible(false);
	        			}
	        			else{
	        				selectMenuItem.setVisible(false);
	        				unSelectMenuItem.setVisible(true);
	        			}
	        		}
    			}
				if (drawerItem == DrawerItem.CHAT){
					if (rChatFL != null){
						rChatFL.selectAll();
						if (rChatFL.showSelectMenuItem()){
							selectMenuItem.setVisible(true);
							unSelectMenuItem.setVisible(false);
						}
						else{
							selectMenuItem.setVisible(false);
							unSelectMenuItem.setVisible(true);
						}
					}
				}
	        	if (drawerItem == DrawerItem.INBOX){
	        		if (iFLol != null){
	        			iFLol.selectAll();
	    				if (iFLol.showSelectMenuItem()){
	        				selectMenuItem.setVisible(true);
	        				unSelectMenuItem.setVisible(false);
	        			}
	        			else{
	        				selectMenuItem.setVisible(false);
	        				unSelectMenuItem.setVisible(true);
	        			}
	        		}
    			}
	        	if (drawerItem == DrawerItem.SEARCH){
	        		if (sFLol != null){
	        			sFLol.selectAll();
	    				if (sFLol.showSelectMenuItem()){
	        				selectMenuItem.setVisible(true);
	        				unSelectMenuItem.setVisible(false);
	        			}
	        			else{
	        				selectMenuItem.setVisible(false);
	        				unSelectMenuItem.setVisible(true);
	        			}
	        		}
    			}
	        	if (drawerItem == DrawerItem.MEDIA_UPLOADS){
	        		if (muFLol != null){
	        			muFLol.selectAll();
	        			if (muFLol.showSelectMenuItem()){
	        				selectMenuItem.setVisible(true);
	        				unSelectMenuItem.setVisible(false);
	        			}
	        			else{
	        				selectMenuItem.setVisible(false);
	        				unSelectMenuItem.setVisible(true);
	        			}
	        		}
	        	}
	        	if (drawerItem == DrawerItem.CAMERA_UPLOADS){
	        		if (cuFL != null){
	        			cuFL.selectAll();
	        			if (cuFL.showSelectMenuItem()){
	        				selectMenuItem.setVisible(true);
	        				unSelectMenuItem.setVisible(false);
	        			}
	        			else{
	        				selectMenuItem.setVisible(false);
	        				unSelectMenuItem.setVisible(true);
	        			}
	        		}
	        	}
	        	return true;
	        }
	        case R.id.action_grid_view_large_small:{
	        	if (drawerItem == DrawerItem.CAMERA_UPLOADS){
	        		if (cuFL != null){
	        			Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("cuFLol");
	        			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
	        			fragTransaction.detach(currentFragment);
	        			fragTransaction.commitNowAllowingStateLoss();

	        			isLargeGridCameraUploads = !isLargeGridCameraUploads;
	        			if (isLargeGridCameraUploads){
	        				gridSmallLargeMenuItem.setIcon(getResources().getDrawable(R.drawable.ic_menu_gridview_small));
	        			}
	        			else{
	        				gridSmallLargeMenuItem.setIcon(getResources().getDrawable(R.drawable.ic_menu_gridview));
	        			}
	        			cuFL.setIsLargeGrid(isLargeGridCameraUploads);

	        			fragTransaction = getSupportFragmentManager().beginTransaction();
	        			fragTransaction.attach(currentFragment);
	        			fragTransaction.commitNowAllowingStateLoss();
	        		}
	        	}
	        	if (drawerItem == DrawerItem.MEDIA_UPLOADS){
	        		if (muFLol != null){
	        			Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("muFLol");
	        			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
	        			fragTransaction.detach(currentFragment);
	        			fragTransaction.commitNowAllowingStateLoss();

	        			isLargeGridCameraUploads = !isLargeGridCameraUploads;
	        			if (isLargeGridCameraUploads){
	        				gridSmallLargeMenuItem.setIcon(getResources().getDrawable(R.drawable.ic_menu_gridview_small));
	        			}
	        			else{
	        				gridSmallLargeMenuItem.setIcon(getResources().getDrawable(R.drawable.ic_menu_gridview));
	        			}
	        			muFLol.setIsLargeGrid(isLargeGridCameraUploads);

	        			fragTransaction = getSupportFragmentManager().beginTransaction();
	        			fragTransaction.attach(currentFragment);
	        			fragTransaction.commitNow();
	        		}
	        	}
	        	return true;
	        }
	        case R.id.action_grid:{
	        	log("action_grid selected");

	        	if (drawerItem == DrawerItem.CAMERA_UPLOADS){
	        		log("action_grid_list in CameraUploads");
	        		isListCameraUploads = !isListCameraUploads;
	    			dbH.setPreferredViewListCamera(isListCameraUploads);
	    			log("dbH.setPreferredViewListCamera: "+isListCameraUploads);
	    			if (isListCameraUploads){
	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    			}
	        		if (cuFL != null){
        				Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("cuFLol");
        				FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
        				fragTransaction.detach(currentFragment);
        				fragTransaction.commitNowAllowingStateLoss();

        				if (isListCameraUploads){
    	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
    	    				gridSmallLargeMenuItem.setVisible(false);
    	    				searchMenuItem.setVisible(true);
    					}
    					else{
    						thumbViewMenuItem.setTitle(getString(R.string.action_list));
    						gridSmallLargeMenuItem.setVisible(true);
    						searchMenuItem.setVisible(false);

    	    			}
        				cuFL.setIsList(isListCameraUploads);

        				fragTransaction = getSupportFragmentManager().beginTransaction();
        				fragTransaction.attach(currentFragment);
        				fragTransaction.commitNowAllowingStateLoss();

        			}
	        	}
	        	else if (drawerItem == DrawerItem.MEDIA_UPLOADS){
	        		log("action_grid_list in MediaUploads");
	        		isListCameraUploads = !isListCameraUploads;
	    			dbH.setPreferredViewListCamera(isListCameraUploads);
	    			log("dbH.setPreferredViewListCamera: "+isListCameraUploads);
	    			if (isListCameraUploads){
	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    			}
	        		if (muFLol != null){
        				Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("muFLol");
        				FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
        				fragTransaction.detach(currentFragment);
        				fragTransaction.commitNowAllowingStateLoss();

        				if (isListCameraUploads){
    	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
    	    				gridSmallLargeMenuItem.setVisible(false);
    	    				searchMenuItem.setVisible(true);
    					}
    					else{
    						thumbViewMenuItem.setTitle(getString(R.string.action_list));
    						gridSmallLargeMenuItem.setVisible(true);
    						searchMenuItem.setVisible(false);

    	    			}
        				muFLol.setIsList(isListCameraUploads);

        				fragTransaction = getSupportFragmentManager().beginTransaction();
        				fragTransaction.attach(currentFragment);
        				fragTransaction.commitNowAllowingStateLoss();
        			}
        		}
	        	else{
		        	isList = !isList;
	    			dbH.setPreferredViewList(isList);

//					updateAliveFragments();

	    			if (isList){
	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    			}
	    			//Rubbish Bin
	    			if (drawerItem == DrawerItem.CLOUD_DRIVE){
	    				String cFTagRB = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);
	    				rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTagRB);
	    				if (rbFLol != null){
							log("rbFLol != null");
//							rbFLol.setIsList(isList);
//							rbFLol.setParentHandle(parentHandleRubbish);
//
//							getSupportFragmentManager()
//									.beginTransaction()
//									.detach(rbFLol)
//									.attach(rbFLol)
//									.commit();


		        			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.detach(rbFLol);
		        			fragTransaction.commitNowAllowingStateLoss();

		        			rbFLol.setIsList(isList);
		        			rbFLol.setParentHandle(parentHandleRubbish);

		        			fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.attach(rbFLol);
		        			fragTransaction.commitNowAllowingStateLoss();
	    				}
	    				//Cloud Drive
	    				String cFTagCD = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
	    				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTagCD);
	    				if (fbFLol != null){
							log("fbFLol != null");

//							fbFLol.setIsList(isList);
//		        			fbFLol.setParentHandle(parentHandleBrowser);
//
//							getSupportFragmentManager()
//									.beginTransaction()
//									.detach(fbFLol)
//									.attach(fbFLol)
//									.commit();


		        			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.detach(fbFLol);
		        			fragTransaction.commitNowAllowingStateLoss();

		        			fbFLol.setIsList(isList);
		        			fbFLol.setParentHandle(parentHandleBrowser);

		        			fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.attach(fbFLol);
		        			fragTransaction.commitNowAllowingStateLoss();
	    				}
	    			}
	    			else if(drawerItem == DrawerItem.INBOX){
	    				if (iFLol != null){
	        				Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("iFLol");
	        				FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
	        				fragTransaction.detach(currentFragment);
	        				fragTransaction.commitNowAllowingStateLoss();

	        				iFLol.setIsList(isList);

	        				fragTransaction = getSupportFragmentManager().beginTransaction();
	        				fragTransaction.attach(currentFragment);
	        				fragTransaction.commitNowAllowingStateLoss();

		        		}
	    			}
	    			else if (drawerItem == DrawerItem.CONTACTS){
	    				String cFTagC = getFragmentTag(R.id.contact_tabs_pager, 0);
			    		cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTagC);
			        	if (cFLol != null){

		        			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.detach(cFLol);
		        			fragTransaction.commitNowAllowingStateLoss();

		        			cFLol.setIsList(isList);

		        			fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.attach(cFLol);
		        			fragTransaction.commitNowAllowingStateLoss();

		        		}
	    			}
	    			else if (drawerItem == DrawerItem.SHARED_ITEMS){
			        	//Incoming
	    				String cFTagIN = getFragmentTag(R.id.shares_tabs_pager, 0);
	    				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTagIN);
	    				if (inSFLol != null){
		        			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.detach(inSFLol);
		        			fragTransaction.commitNowAllowingStateLoss();

		        			inSFLol.setIsList(isList);
		        			inSFLol.setParentHandle(parentHandleIncoming);

		        			fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.attach(inSFLol);
		        			fragTransaction.commitNowAllowingStateLoss();
	    				}

	    				//Outgoing
	    				String cFTagOUT = getFragmentTag(R.id.shares_tabs_pager, 1);
	    				outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTagOUT);
	    				if (outSFLol != null){
		        			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.detach(outSFLol);
		        			fragTransaction.commitNowAllowingStateLoss();

		        			outSFLol.setIsList(isList);
		        			outSFLol.setParentHandle(parentHandleOutgoing);

		        			fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.attach(outSFLol);
		        			fragTransaction.commitNowAllowingStateLoss();
	    				}
	    			}
	    			else if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE){
	    				if (oFLol != null){
	        				Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("oFLol");
	        				FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
	        				fragTransaction.detach(currentFragment);
	        				fragTransaction.commitNowAllowingStateLoss();

	        				oFLol.setIsList(isList);
	        				oFLol.setPathNavigation(pathNavigationOffline);
	        				//oFLol.setGridNavigation(false);
	        				//oFLol.setParentHandle(parentHandleSharedWithMe);

	        				fragTransaction = getSupportFragmentManager().beginTransaction();
	        				fragTransaction.attach(currentFragment);
	        				fragTransaction.commitNowAllowingStateLoss();

		        		}
	    			}
	    			else if (drawerItem == DrawerItem.SEARCH){
	    				if (sFLol != null){
	    					Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("sFLol");
	    					FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
	        				fragTransaction.detach(currentFragment);
	        				fragTransaction.commitNowAllowingStateLoss();

	        				sFLol.setIsList(isList);
	        				sFLol.setParentHandle(parentHandleSearch);

	        				fragTransaction = getSupportFragmentManager().beginTransaction();
	        				fragTransaction.attach(currentFragment);
	        				fragTransaction.commitNowAllowingStateLoss();
	    				}
	    			}

//		        	if (drawerItem == DrawerItem.CLOUD_DRIVE){
//
//		        	}
//		        	if (drawerItem == DrawerItem.INBOX){
//
//		        	}
//		        	if (drawerItem == DrawerItem.CONTACTS){
//
//		        	}
//		        	if (drawerItem == DrawerItem.SHARED_ITEMS){
//
//
//
//		        	}
//		        	if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE){
//
//	        		}
	        	}

	        	return true;
	        }
//	        case R.id.action_rubbish_bin:{
//	        	if (drawerItem == DrawerItem.RUBBISH_BIN){
//	        		drawerItem = DrawerItem.CLOUD_DRIVE;
//	        		selectDrawerItem(drawerItem);
//	        	}
//	        	else if (drawerItem == DrawerItem.CLOUD_DRIVE){
//	        		drawerItem = DrawerItem.RUBBISH_BIN;
//	        		selectDrawerItem(drawerItem);
//	        	}
//	        	return true;
//	        }
	        case R.id.action_menu_clear_rubbish_bin:{
	        	if (drawerItem == DrawerItem.CLOUD_DRIVE){
	        		showClearRubbishBinDialog(null);
	        	}
	        	return true;
	        }
	        case R.id.action_menu_refresh:{
	        	switch(drawerItem){
		        	case CLOUD_DRIVE:{
		        		Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
			    		intent.setAction(Constants.ACTION_REFRESH);
			    		intent.putExtra("PARENT_HANDLE", parentHandleBrowser);
			    		startActivityForResult(intent, Constants.REQUEST_CODE_REFRESH);
		        		break;
		        	}
		        	case CONTACTS:{
		        		Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
			    		intent.setAction(Constants.ACTION_REFRESH);
			    		intent.putExtra("PARENT_HANDLE", parentHandleBrowser);
			    		startActivityForResult(intent, Constants.REQUEST_CODE_REFRESH);
			    		break;
		        	}
		        	case SHARED_ITEMS:{

		        		int index = viewPagerShares.getCurrentItem();
		    			if(index==1){
		    				//OUTGOING
		    				String cFTag2 = getFragmentTag(R.id.shares_tabs_pager, 1);
		    				log("Tag: "+ cFTag2);
		    				outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag2);
		    				if (outSFLol != null){
		    					Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
								intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
					    		intent.setAction(Constants.ACTION_REFRESH);
					    		intent.putExtra("PARENT_HANDLE", parentHandleOutgoing);
					    		startActivityForResult(intent, Constants.REQUEST_CODE_REFRESH);
					    		break;
		    				}
		    			}
		    			else{
		    				//InCOMING
		    				String cFTag1 = getFragmentTag(R.id.shares_tabs_pager, 0);
		    				log("Tag: "+ cFTag1);
		    				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag1);
		    				if (inSFLol != null){
		    					Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
								intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
					    		intent.setAction(Constants.ACTION_REFRESH);
					    		intent.putExtra("PARENT_HANDLE", parentHandleIncoming);
					    		startActivityForResult(intent, Constants.REQUEST_CODE_REFRESH);
					    		break;
		    				}
		    			}
		        	}
		        	case ACCOUNT:{
						//Refresh all the info of My Account
		        		Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
			    		intent.setAction(Constants.ACTION_REFRESH);
			    		intent.putExtra("PARENT_HANDLE", parentHandleBrowser);
			    		startActivityForResult(intent, Constants.REQUEST_CODE_REFRESH);
			    		break;
		        	}
	        	}
	        	return true;
	        }
	        case R.id.action_menu_sort_by:{

        		AlertDialog sortByDialog;
        		LayoutInflater inflater = getLayoutInflater();
        		View dialoglayout = inflater.inflate(R.layout.sortby_dialog, null);

        		TextView sortByNameTV = (TextView) dialoglayout.findViewById(R.id.sortby_dialog_name_text);
        		sortByNameTV.setText(getString(R.string.sortby_name));
        		ViewGroup.MarginLayoutParams nameMLP = (ViewGroup.MarginLayoutParams) sortByNameTV.getLayoutParams();
        		sortByNameTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        		nameMLP.setMargins(Util.scaleWidthPx(25, outMetrics), Util.scaleHeightPx(15, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        		TextView sortByDateTV = (TextView) dialoglayout.findViewById(R.id.sortby_dialog_date_text);
        		sortByDateTV.setText(getString(R.string.sortby_creation_date));
        		ViewGroup.MarginLayoutParams dateMLP = (ViewGroup.MarginLayoutParams) sortByDateTV.getLayoutParams();
        		sortByDateTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        		dateMLP.setMargins(Util.scaleWidthPx(25, outMetrics), Util.scaleHeightPx(15, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

				TextView sortByModificationDateTV = (TextView) dialoglayout.findViewById(R.id.sortby_dialog_date_modification_text);
				sortByModificationDateTV.setText(getString(R.string.sortby_modification_date));
				ViewGroup.MarginLayoutParams dateModMLP = (ViewGroup.MarginLayoutParams) sortByModificationDateTV.getLayoutParams();
				sortByModificationDateTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
				dateModMLP.setMargins(Util.scaleWidthPx(25, outMetrics), Util.scaleHeightPx(15, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        		TextView sortBySizeTV = (TextView) dialoglayout.findViewById(R.id.sortby_dialog_size_text);
        		sortBySizeTV.setText(getString(R.string.sortby_size));
        		ViewGroup.MarginLayoutParams sizeMLP = (ViewGroup.MarginLayoutParams) sortBySizeTV.getLayoutParams();
        		sortBySizeTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        		sizeMLP.setMargins(Util.scaleWidthPx(25, outMetrics), Util.scaleHeightPx(15, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        		final CheckedTextView ascendingCheck = (CheckedTextView) dialoglayout.findViewById(R.id.sortby_dialog_ascending_check);
        		ascendingCheck.setText(getString(R.string.sortby_name_ascending));
        		ascendingCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        		ascendingCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
        		ViewGroup.MarginLayoutParams ascendingMLP = (ViewGroup.MarginLayoutParams) ascendingCheck.getLayoutParams();
        		ascendingMLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        		final CheckedTextView descendingCheck = (CheckedTextView) dialoglayout.findViewById(R.id.sortby_dialog_descending_check);
        		descendingCheck.setText(getString(R.string.sortby_name_descending));
        		descendingCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        		descendingCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
        		ViewGroup.MarginLayoutParams descendingMLP = (ViewGroup.MarginLayoutParams) descendingCheck.getLayoutParams();
        		descendingMLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        		final CheckedTextView newestCheck = (CheckedTextView) dialoglayout.findViewById(R.id.sortby_dialog_newest_check);
        		newestCheck.setText(getString(R.string.sortby_date_newest));
        		newestCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        		newestCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
        		ViewGroup.MarginLayoutParams newestMLP = (ViewGroup.MarginLayoutParams) newestCheck.getLayoutParams();
        		newestMLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        		final CheckedTextView oldestCheck = (CheckedTextView) dialoglayout.findViewById(R.id.sortby_dialog_oldest_check);
        		oldestCheck.setText(getString(R.string.sortby_date_oldest));
        		oldestCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        		oldestCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
        		ViewGroup.MarginLayoutParams oldestMLP = (ViewGroup.MarginLayoutParams) oldestCheck.getLayoutParams();
        		oldestMLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

				final CheckedTextView newestModificationCheck = (CheckedTextView) dialoglayout.findViewById(R.id.sortby_dialog_newest_modification_check);
				newestModificationCheck.setText(getString(R.string.sortby_date_newest));
				newestModificationCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
				newestModificationCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
				ViewGroup.MarginLayoutParams newestModMLP = (ViewGroup.MarginLayoutParams) newestModificationCheck.getLayoutParams();
				newestModMLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

				final CheckedTextView oldestModificationCheck = (CheckedTextView) dialoglayout.findViewById(R.id.sortby_dialog_oldest_modification_check);
				oldestModificationCheck.setText(getString(R.string.sortby_date_oldest));
				oldestModificationCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
				oldestModificationCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
				ViewGroup.MarginLayoutParams oldestModMLP = (ViewGroup.MarginLayoutParams) oldestModificationCheck.getLayoutParams();
				oldestModMLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        		final CheckedTextView largestCheck = (CheckedTextView) dialoglayout.findViewById(R.id.sortby_dialog_largest_first_check);
        		largestCheck.setText(getString(R.string.sortby_size_largest_first));
        		largestCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        		largestCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
        		ViewGroup.MarginLayoutParams largestMLP = (ViewGroup.MarginLayoutParams) largestCheck.getLayoutParams();
        		largestMLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        		final CheckedTextView smallestCheck = (CheckedTextView) dialoglayout.findViewById(R.id.sortby_dialog_smallest_first_check);
        		smallestCheck.setText(getString(R.string.sortby_size_smallest_first));
        		smallestCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        		smallestCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
        		ViewGroup.MarginLayoutParams smallestMLP = (ViewGroup.MarginLayoutParams) smallestCheck.getLayoutParams();
        		smallestMLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        		builder.setView(dialoglayout);
        		builder.setTitle(getString(R.string.action_sort_by));

        		sortByDialog = builder.create();
        		sortByDialog.show();
        		if(drawerItem==DrawerItem.CONTACTS){
        			switch(orderContacts){
		        		case MegaApiJava.ORDER_DEFAULT_ASC:{
		        			ascendingCheck.setChecked(true);
		        			descendingCheck.setChecked(false);
		        			break;
		        		}
		        		case MegaApiJava.ORDER_DEFAULT_DESC:{
		        			ascendingCheck.setChecked(false);
		        			descendingCheck.setChecked(true);
		        			break;
		        		}
	        		}
        		}
        		else if(drawerItem==DrawerItem.SAVED_FOR_OFFLINE||drawerItem==DrawerItem.SHARED_ITEMS){
        			log("orderOthers: "+orderOthers);
        			switch(orderOthers){
		        		case MegaApiJava.ORDER_DEFAULT_ASC:{
		        			log("ASCE");
		        			ascendingCheck.setChecked(true);
		        			descendingCheck.setChecked(false);
		        			break;
		        		}
		        		case MegaApiJava.ORDER_DEFAULT_DESC:{
		        			log("DESC");
		        			ascendingCheck.setChecked(false);
		        			descendingCheck.setChecked(true);
		        			break;
		        		}
        			}
        		}
        		else{
					log("orderCloud: "+orderCloud);
	        		switch(orderCloud){
		        		case MegaApiJava.ORDER_DEFAULT_ASC:{
		        			ascendingCheck.setChecked(true);
		        			descendingCheck.setChecked(false);
		        			newestCheck.setChecked(false);
		        			oldestCheck.setChecked(false);
							newestModificationCheck.setChecked(false);
							oldestModificationCheck.setChecked(false);
		        			largestCheck.setChecked(false);
		        			smallestCheck.setChecked(false);
		        			break;
		        		}
		        		case MegaApiJava.ORDER_DEFAULT_DESC:{
		        			ascendingCheck.setChecked(false);
		        			descendingCheck.setChecked(true);
		        			newestCheck.setChecked(false);
		        			oldestCheck.setChecked(false);
							newestModificationCheck.setChecked(false);
							oldestModificationCheck.setChecked(false);
		        			largestCheck.setChecked(false);
		        			smallestCheck.setChecked(false);
		        			break;
		        		}
		        		case MegaApiJava.ORDER_CREATION_DESC:{
		        			ascendingCheck.setChecked(false);
		        			descendingCheck.setChecked(false);
		        			newestCheck.setChecked(true);
		        			oldestCheck.setChecked(false);
							newestModificationCheck.setChecked(false);
							oldestModificationCheck.setChecked(false);
		        			largestCheck.setChecked(false);
		        			smallestCheck.setChecked(false);
		        			break;
		        		}
		        		case MegaApiJava.ORDER_CREATION_ASC:{
		        			ascendingCheck.setChecked(false);
		        			descendingCheck.setChecked(false);
		        			newestCheck.setChecked(false);
		        			oldestCheck.setChecked(true);
							newestModificationCheck.setChecked(false);
							oldestModificationCheck.setChecked(false);
		        			largestCheck.setChecked(false);
		        			smallestCheck.setChecked(false);
		        			break;
		        		}
						case MegaApiJava.ORDER_MODIFICATION_ASC:{
							ascendingCheck.setChecked(false);
							descendingCheck.setChecked(false);
							newestCheck.setChecked(false);
							oldestCheck.setChecked(false);
							newestModificationCheck.setChecked(false);
							oldestModificationCheck.setChecked(true);
							largestCheck.setChecked(false);
							smallestCheck.setChecked(false);
							break;
						}
						case MegaApiJava.ORDER_MODIFICATION_DESC:{
							ascendingCheck.setChecked(false);
							descendingCheck.setChecked(false);
							newestCheck.setChecked(false);
							oldestCheck.setChecked(false);
							newestModificationCheck.setChecked(true);
							oldestModificationCheck.setChecked(false);
							largestCheck.setChecked(false);
							smallestCheck.setChecked(false);
							break;
						}
		        		case MegaApiJava.ORDER_SIZE_ASC:{
		        			ascendingCheck.setChecked(false);
		        			descendingCheck.setChecked(false);
		        			newestCheck.setChecked(false);
		        			oldestCheck.setChecked(false);
							newestModificationCheck.setChecked(false);
							oldestModificationCheck.setChecked(false);
		        			largestCheck.setChecked(false);
		        			smallestCheck.setChecked(true);
		        			break;
		        		}
		        		case MegaApiJava.ORDER_SIZE_DESC:{
		        			ascendingCheck.setChecked(false);
		        			descendingCheck.setChecked(false);
		        			newestCheck.setChecked(false);
		        			oldestCheck.setChecked(false);
							newestModificationCheck.setChecked(false);
							oldestModificationCheck.setChecked(false);
		        			largestCheck.setChecked(true);
		        			smallestCheck.setChecked(false);
		        			break;
		        		}
	        		}
	        	}

        		final AlertDialog dialog = sortByDialog;
	        	switch(drawerItem){
		        	case CONTACTS:{
						sortByNameTV.setText(getString(R.string.email_text));
		        		sortByDateTV.setVisibility(View.GONE);
		        		newestCheck.setVisibility(View.GONE);
		        		oldestCheck.setVisibility(View.GONE);
						sortByModificationDateTV.setVisibility(View.GONE);
						newestModificationCheck.setVisibility(View.GONE);
						oldestModificationCheck.setVisibility(View.GONE);
		        		sortBySizeTV.setVisibility(View.GONE);
		        		largestCheck.setVisibility(View.GONE);
		        		smallestCheck.setVisibility(View.GONE);

		        		ascendingCheck.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(true);
			        			descendingCheck.setChecked(false);
								log("order contacts value _ "+orderContacts);
								if(orderContacts!=MegaApiJava.ORDER_DEFAULT_ASC){
									log("call to selectSortByContacts ASC _ "+orderContacts);
									selectSortByContacts(MegaApiJava.ORDER_DEFAULT_ASC);
								}
			        			if (dialog != null){
			        				dialog.dismiss();
			        			}
							}
						});

		        		descendingCheck.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(false);
			        			descendingCheck.setChecked(true);
								log("order contacts value _ "+orderContacts);
								if(orderContacts!=MegaApiJava.ORDER_DEFAULT_DESC) {
									log("call to selectSortByContacts DESC _ "+orderContacts);
									selectSortByContacts(MegaApiJava.ORDER_DEFAULT_DESC);
								}
			        			if (dialog != null){
			        				dialog.dismiss();
			        			}
							}
						});

		        		break;
		        	}
		        	case SAVED_FOR_OFFLINE: {

		        		sortByDateTV.setVisibility(View.GONE);
		        		newestCheck.setVisibility(View.GONE);
		        		oldestCheck.setVisibility(View.GONE);
						sortByModificationDateTV.setVisibility(View.GONE);
						newestModificationCheck.setVisibility(View.GONE);
						oldestModificationCheck.setVisibility(View.GONE);
		        		sortBySizeTV.setVisibility(View.GONE);
		        		largestCheck.setVisibility(View.GONE);
		        		smallestCheck.setVisibility(View.GONE);

		        		ascendingCheck.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(true);
			        			descendingCheck.setChecked(false);
								if(orderOthers!=MegaApiJava.ORDER_DEFAULT_ASC) {
									selectSortByOffline(MegaApiJava.ORDER_DEFAULT_ASC);
								}
			        			if (dialog != null){
			        				dialog.dismiss();
			        			}
							}
						});

		        		descendingCheck.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(false);
			        			descendingCheck.setChecked(true);
								if(orderOthers!=MegaApiJava.ORDER_DEFAULT_DESC) {
									selectSortByOffline(MegaApiJava.ORDER_DEFAULT_DESC);
								}
			        			if (dialog != null){
			        				dialog.dismiss();
			        			}
							}
						});

		        		break;

		        	}
		        	case SHARED_ITEMS: {

		        		sortByDateTV.setVisibility(View.GONE);
		        		newestCheck.setVisibility(View.GONE);
		        		oldestCheck.setVisibility(View.GONE);
						sortByModificationDateTV.setVisibility(View.GONE);
						newestModificationCheck.setVisibility(View.GONE);
						oldestModificationCheck.setVisibility(View.GONE);
		        		sortBySizeTV.setVisibility(View.GONE);
		        		largestCheck.setVisibility(View.GONE);
		        		smallestCheck.setVisibility(View.GONE);


		        		if (viewPagerShares.getCurrentItem()==0){

							sortByNameTV.setText(getString(R.string.sortby_owner_mail));
		        		}
		        		else{

		        		}

		        		ascendingCheck.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(true);
			        			descendingCheck.setChecked(false);
								if(orderOthers!=MegaApiJava.ORDER_DEFAULT_ASC){
									selectSortByIncoming(MegaApiJava.ORDER_DEFAULT_ASC);
									selectSortByOutgoing(MegaApiJava.ORDER_DEFAULT_ASC);

								}

			        			if (dialog != null){
			        				dialog.dismiss();
			        			}
							}
						});

		        		descendingCheck.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(false);
			        			descendingCheck.setChecked(true);
								if(orderOthers!=MegaApiJava.ORDER_DEFAULT_DESC){
										selectSortByIncoming(MegaApiJava.ORDER_DEFAULT_DESC);
										selectSortByOutgoing(MegaApiJava.ORDER_DEFAULT_DESC);
								}

			        			if (dialog != null){
			        				dialog.dismiss();
			        			}
							}
						});

		        		break;

		        	}
		        	case CLOUD_DRIVE:
		        	case INBOX:{

		        		ascendingCheck.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(true);
			        			descendingCheck.setChecked(false);
			        			newestCheck.setChecked(false);
			        			oldestCheck.setChecked(false);
			        			largestCheck.setChecked(false);
			        			smallestCheck.setChecked(false);
			        			if(drawerItem==DrawerItem.CLOUD_DRIVE){
			        				selectSortByCloudDrive(MegaApiJava.ORDER_DEFAULT_ASC);
			        			}
			        			else{
			        				selectSortByInbox(MegaApiJava.ORDER_DEFAULT_ASC);
			        			}

			        			if (dialog != null){
			        				dialog.dismiss();
			        			}
							}
						});

		        		descendingCheck.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(false);
			        			descendingCheck.setChecked(true);
			        			newestCheck.setChecked(false);
			        			oldestCheck.setChecked(false);
			        			largestCheck.setChecked(false);
			        			smallestCheck.setChecked(false);
			        			if(drawerItem==DrawerItem.CLOUD_DRIVE){
			        				selectSortByCloudDrive(MegaApiJava.ORDER_DEFAULT_DESC);
			        			}
			        			else{
			        				selectSortByInbox(MegaApiJava.ORDER_DEFAULT_DESC);
			        			}

			        			if (dialog != null){
			        				dialog.dismiss();
			        			}
							}
						});

		        		newestCheck.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(false);
			        			descendingCheck.setChecked(false);
			        			newestCheck.setChecked(true);
			        			oldestCheck.setChecked(false);
			        			largestCheck.setChecked(false);
			        			smallestCheck.setChecked(false);
			        			if(drawerItem==DrawerItem.CLOUD_DRIVE){
			        				selectSortByCloudDrive(MegaApiJava.ORDER_CREATION_DESC);
			        			}
			        			else{
			        				selectSortByInbox(MegaApiJava.ORDER_CREATION_DESC);
			        			}

			        			if (dialog != null){
			        				dialog.dismiss();
			        			}
							}
						});

		        		oldestCheck.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(false);
			        			descendingCheck.setChecked(false);
			        			newestCheck.setChecked(false);
			        			oldestCheck.setChecked(true);
			        			largestCheck.setChecked(false);
			        			smallestCheck.setChecked(false);
			        			if(drawerItem==DrawerItem.CLOUD_DRIVE){
			        				selectSortByCloudDrive(MegaApiJava.ORDER_CREATION_ASC);
			        			}
			        			else{
			        				selectSortByInbox(MegaApiJava.ORDER_CREATION_ASC);
			        			}

			        			if (dialog != null){
			        				dialog.dismiss();
			        			}
							}
						});

						newestModificationCheck.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(false);
								descendingCheck.setChecked(false);
								newestCheck.setChecked(false);
								oldestCheck.setChecked(false);
								newestModificationCheck.setChecked(true);
								oldestModificationCheck.setChecked(false);
								largestCheck.setChecked(false);
								smallestCheck.setChecked(false);
								if(drawerItem==DrawerItem.CLOUD_DRIVE){
									selectSortByCloudDrive(MegaApiJava.ORDER_MODIFICATION_DESC);
								}
								else{
									selectSortByInbox(MegaApiJava.ORDER_MODIFICATION_DESC);
								}

								if (dialog != null){
									dialog.dismiss();
								}
							}
						});

						oldestModificationCheck.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(false);
								descendingCheck.setChecked(false);
								newestCheck.setChecked(false);
								oldestCheck.setChecked(false);
								newestModificationCheck.setChecked(false);
								oldestModificationCheck.setChecked(true);
								largestCheck.setChecked(false);
								smallestCheck.setChecked(false);
								if(drawerItem==DrawerItem.CLOUD_DRIVE){
									selectSortByCloudDrive(MegaApiJava.ORDER_MODIFICATION_ASC);
								}
								else{
									selectSortByInbox(MegaApiJava.ORDER_MODIFICATION_ASC);
								}

								if (dialog != null){
									dialog.dismiss();
								}
							}
						});

		        		largestCheck.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(false);
			        			descendingCheck.setChecked(false);
			        			newestCheck.setChecked(false);
			        			oldestCheck.setChecked(false);
			        			largestCheck.setChecked(true);
			        			smallestCheck.setChecked(false);
			        			if(drawerItem==DrawerItem.CLOUD_DRIVE){
			        				selectSortByCloudDrive(MegaApiJava.ORDER_SIZE_DESC);
			        			}
			        			else{
			        				selectSortByInbox(MegaApiJava.ORDER_SIZE_DESC);
			        			}

			        			if (dialog != null){
			        				dialog.dismiss();
			        			}
							}
						});

		        		smallestCheck.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(false);
			        			descendingCheck.setChecked(false);
			        			newestCheck.setChecked(false);
			        			oldestCheck.setChecked(false);
			        			largestCheck.setChecked(false);
			        			smallestCheck.setChecked(true);
			        			if(drawerItem==DrawerItem.CLOUD_DRIVE){
			        				selectSortByCloudDrive(MegaApiJava.ORDER_SIZE_ASC);
			        			}
			        			else{
			        				selectSortByInbox(MegaApiJava.ORDER_SIZE_ASC);
			        			}

			        			if (dialog != null){
			        				dialog.dismiss();
			        			}
							}
						});

		        		break;
	        		}
//		        	default:{
//		        		Intent intent = new Intent(managerActivity, SortByDialogActivity.class);
//			    		intent.setAction(SortByDialogActivity.ACTION_SORT_BY);
//			    		startActivityForResult(intent, REQUEST_CODE_SORT_BY);
//			    		break;
//		        	}
	        	}
	        	return true;
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
	        	drawerItem = DrawerItem.ACCOUNT;
	        	if (nV != null){
					Menu nVMenu = nV.getMenu();
					MenuItem hidden = nVMenu.findItem(R.id.navigation_item_hidden);
					resetNavigationViewMenu(nVMenu);
					hidden.setChecked(true);
				}
				drawerItem = DrawerItem.ACCOUNT;
				accountFragment = Constants.UPGRADE_ACCOUNT_FRAGMENT;
				displayedAccountType = -1;
				selectDrawerItemLollipop(drawerItem);
				return true;
	        }

	        case R.id.action_menu_change_pass:{
	        	Intent intent = new Intent(this, ChangePasswordActivityLollipop.class);
				startActivity(intent);
				return true;
	        }
	        case R.id.action_menu_remove_MK:{
				log("remove MK option selected");
				showConfirmationRemoveMK();
				return true;
	        }
	        case R.id.action_menu_export_MK:{
	        	log("export MK option selected");
				String myAccountTag = getFragmentTag(R.id.my_account_tabs_pager, 0);
				maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(myAccountTag);
				if(maFLol!=null){
					showMKLayout(false);
				}
	        	return true;
	        }
	        case R.id.action_menu_logout:{
				log("action menu logout pressed");
				AccountController aC = new AccountController(this);
				aC.logout(this, megaApi, megaChatApi, false);
	        	return true;
	        }
	        case R.id.action_menu_cancel_subscriptions:{
				log("action menu cancel subscriptions pressed");
	        	if (megaApi != null){
	        		//Show the message
	        		showCancelMessage();
	        	}
	        	return true;
	        }
			case R.id.action_menu_forgot_pass:{
				log("action menu forgot pass pressed");
				String myAccountTag = getFragmentTag(R.id.my_account_tabs_pager, 0);
				maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(myAccountTag);
				if(maFLol!=null){
					showConfirmationResetPasswordFromMyAccount();
				}
				return true;
			}
            default:{
	            return super.onOptionsItemSelected(item);
            }
		}
	}

	public void showMKLayout(boolean fromFragment){

		tabLayoutMyAccount.setVisibility(View.GONE);
		mkLayoutVisible=true;
		aB.hide();
		if(!fromFragment){
			maFLol.showMKLayout();
		}
	}

	public void hideMKLayout(){
		tabLayoutMyAccount.setVisibility(View.VISIBLE);
		mkLayoutVisible= false;
		aB.show();
	}

	public void updateAliveFragments(){
		log("updateAliveFragments");
		//Needed to update view when changing list<->grid from other section
		String cloudTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
		fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cloudTag);
		if (fbFLol != null){
			log("FileBrowserFragment is not NULL -> UPDATE");
			fbFLol.setIsList(isList);

			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
			fragTransaction.detach(fbFLol);
			fragTransaction.commitNowAllowingStateLoss();

			fragTransaction = getSupportFragmentManager().beginTransaction();
			fragTransaction.attach(fbFLol);
			fragTransaction.commitNowAllowingStateLoss();
		}

		cloudTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);
		rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cloudTag);
		if (rbFLol != null){
			log("RubbishBinFragment is not NULL -> UPDATE");
			rbFLol.setIsList(isList);

			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
			fragTransaction.detach(rbFLol);
			fragTransaction.commitNowAllowingStateLoss();

			fragTransaction = getSupportFragmentManager().beginTransaction();
			fragTransaction.attach(rbFLol);
			fragTransaction.commitNowAllowingStateLoss();
		}

		String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 0);
		inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
		if (inSFLol != null){
			log("IncomingFragment is not NULL -> UPDATE");
			inSFLol.setIsList(isList);

			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
			fragTransaction.detach(inSFLol);
			fragTransaction.commitNowAllowingStateLoss();

			fragTransaction = getSupportFragmentManager().beginTransaction();
			fragTransaction.attach(inSFLol);
			fragTransaction.commitNowAllowingStateLoss();
		}

		sharesTag = getFragmentTag(R.id.shares_tabs_pager, 1);
		outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
		if (outSFLol != null){
			log("OutgoingFragment is not NULL -> UPDATE");
			outSFLol.setIsList(isList);

			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
			fragTransaction.detach(outSFLol);
			fragTransaction.commitNowAllowingStateLoss();

			fragTransaction = getSupportFragmentManager().beginTransaction();
			fragTransaction.attach(outSFLol);
			fragTransaction.commitNowAllowingStateLoss();
		}

		String contactsTag = getFragmentTag(R.id.contact_tabs_pager, 0);
		cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(contactsTag);
		if (cFLol != null){
			log("ContactsFragment is not NULL -> UPDATE");
			cFLol.setIsList(isList);

			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
			fragTransaction.detach(cFLol);
			fragTransaction.commitNowAllowingStateLoss();

			fragTransaction = getSupportFragmentManager().beginTransaction();
			fragTransaction.attach(cFLol);
			fragTransaction.commitNowAllowingStateLoss();
		}
	}

	@Override
	public void onBackPressed() {
		log("onBackPressedLollipop");

		if (drawerLayout.isDrawerOpen(nV)){
    		drawerLayout.closeDrawer(Gravity.LEFT);
    		return;
    	}

		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}

		if (megaApi != null){
			log("---------retryPendingConnections");
			megaApi.retryPendingConnections();
		}
		try {
			statusDialog.dismiss();
		}
		catch (Exception ex) {}

		log("DRAWERITEM: " + drawerItem);

		if (drawerItem == DrawerItem.CLOUD_DRIVE){

			int index = viewPagerCDrive.getCurrentItem();
			if(index==1){
				//Rubbish Bin
				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);
				rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
				if (rbFLol != null){
					if (rbFLol.onBackPressed() == 0){
						viewPagerCDrive.setCurrentItem(0);
					}
					return;
				}
			}
			else if(index==0){
				//Cloud Drive
				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
				if (fbFLol != null){
					if (fbFLol.onBackPressed() == 0){
						super.onBackPressed();
					}
					return;
				}
			}

			super.onBackPressed();
		}
		else if (drawerItem == DrawerItem.TRANSFERS){
			if (tFLol != null){
    			if (tFLol.onBackPressed() == 0){
    				drawerItem = DrawerItem.CLOUD_DRIVE;
					if (nV != null){
						Menu nVMenu = nV.getMenu();
						MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
						resetNavigationViewMenu(nVMenu);
						cloudDrive.setChecked(true);
						cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
					}
    				selectDrawerItemLollipop(drawerItem);
    				return;
    			}
    		}
    	}
		else if (drawerItem == DrawerItem.INBOX){
			if (iFLol != null){
				if (iFLol.onBackPressed() == 0){
					drawerItem = DrawerItem.CLOUD_DRIVE;
					if (nV != null){
						Menu nVMenu = nV.getMenu();
						MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
						resetNavigationViewMenu(nVMenu);
						cloudDrive.setChecked(true);
						cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
					}
					selectDrawerItemLollipop(drawerItem);
					return;
				}
			}
		}
		else if (drawerItem == DrawerItem.SETTINGS){

			if (!Util.isOnline(this)){
				showOfflineMode();
			}
			else{
				drawerItem = DrawerItem.CLOUD_DRIVE;
				if (nV != null){
					Menu nVMenu = nV.getMenu();
					MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
					resetNavigationViewMenu(nVMenu);
					cloudDrive.setChecked(true);
					cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
				}
				selectDrawerItemLollipop(drawerItem);
			}

			return;
		}
		else if (drawerItem == DrawerItem.SHARED_ITEMS){
			int index = viewPagerShares.getCurrentItem();
			if(index==1){
				//OUTGOING
				String cFTag2 = getFragmentTag(R.id.shares_tabs_pager, 1);
				log("Tag: "+ cFTag2);
				outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag2);
				if (outSFLol != null){
					if (outSFLol.onBackPressed() == 0){
						drawerItem = DrawerItem.CLOUD_DRIVE;
						if (nV != null){
							Menu nVMenu = nV.getMenu();
							MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
							resetNavigationViewMenu(nVMenu);
							cloudDrive.setChecked(true);
							cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
						}
						selectDrawerItemLollipop(drawerItem);
						return;
					}
				}
			}
			else{
				//InCOMING
				String cFTag1 = getFragmentTag(R.id.shares_tabs_pager, 0);
				log("Tag: "+ cFTag1);
				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag1);
				if (inSFLol != null){
					if (inSFLol.onBackPressed() == 0){
						drawerItem = DrawerItem.CLOUD_DRIVE;
						if (nV != null){
							Menu nVMenu = nV.getMenu();
							MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
							resetNavigationViewMenu(nVMenu);
							cloudDrive.setChecked(true);
							cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
						}
						selectDrawerItemLollipop(drawerItem);
						return;
					}
				}
			}
		}
		else if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE){
			if (oFLol != null){
				if (oFLol.onBackPressed() == 0){
					attr = dbH.getAttributes();
					if (attr != null){
						if (attr.getOnline() != null){
							if (!Boolean.parseBoolean(attr.getOnline())){
								super.onBackPressed();
								return;
							}
						}
					}

					if (fbFLol != null){
						drawerItem = DrawerItem.CLOUD_DRIVE;
						if (nV != null){
							Menu nVMenu = nV.getMenu();
							MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
							resetNavigationViewMenu(nVMenu);
							cloudDrive.setChecked(true);
							cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
						}
						selectDrawerItemLollipop(drawerItem);
					}
					else{
						super.onBackPressed();
					}
					return;
				}
			}
		}
		else if (drawerItem == DrawerItem.CHAT){

			if (!Util.isOnline(this)){
				showOfflineMode();
			}
			else{
				drawerItem = DrawerItem.CLOUD_DRIVE;
				if (nV != null){
					Menu nVMenu = nV.getMenu();
					MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
					resetNavigationViewMenu(nVMenu);
					cloudDrive.setChecked(true);
					cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
				}
				selectDrawerItemLollipop(drawerItem);
			}
		}
		else if (drawerItem == DrawerItem.CONTACTS){
			int index = viewPagerContacts.getCurrentItem();
			switch (index) {
				case 0:{
					//CONTACTS FRAGMENT
		    		String cFTag = getFragmentTag(R.id.contact_tabs_pager, 0);
		    		cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
		    		if (cFLol != null){
		    			if (cFLol.onBackPressed() == 0){
		    				drawerItem = DrawerItem.CLOUD_DRIVE;
		    				if (nV != null){
								Menu nVMenu = nV.getMenu();
								MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
								resetNavigationViewMenu(nVMenu);
								cloudDrive.setChecked(true);
								cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
							}
							selectDrawerItemLollipop(drawerItem);
		    				return;
		    			}
		    		}
					break;
				}
				case 1:{
					String cFTag = getFragmentTag(R.id.contact_tabs_pager, 1);
					sRFLol = (SentRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
		    		if (sRFLol != null){
		    			if (sRFLol.onBackPressed() == 0){
		    				drawerItem = DrawerItem.CLOUD_DRIVE;
		    				if (nV != null){
								Menu nVMenu = nV.getMenu();
								MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
								resetNavigationViewMenu(nVMenu);
								cloudDrive.setChecked(true);
								cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
							}
							selectDrawerItemLollipop(drawerItem);
		    				return;
		    			}
		    		}
					break;
				}
				case 2:{
					String cFTag = getFragmentTag(R.id.contact_tabs_pager, 2);
					rRFLol = (ReceivedRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
		    		if (rRFLol != null){
		    			if (rRFLol.onBackPressed() == 0){
		    				drawerItem = DrawerItem.CLOUD_DRIVE;
		    				if (nV != null){
								Menu nVMenu = nV.getMenu();
								MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
								resetNavigationViewMenu(nVMenu);
								cloudDrive.setChecked(true);
								cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
							}
							selectDrawerItemLollipop(drawerItem);
		    				return;
		    			}
		    		}
					break;
				}
			}
		}
		else if (drawerItem == DrawerItem.ACCOUNT){
			log("MyAccountSection");
			log("The accountFragment is: "+accountFragment);
    		switch(accountFragment){

	    		case Constants.MY_ACCOUNT_FRAGMENT:{
	    			if (maFLol != null){
	    				if (maFLol.onBackPressed() == 0){
		    				drawerItem = DrawerItem.CLOUD_DRIVE;
		    				if (nV != null){
								Menu nVMenu = nV.getMenu();
								MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
								resetNavigationViewMenu(nVMenu);
								cloudDrive.setChecked(true);
								cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
							}
							selectDrawerItemLollipop(drawerItem);
	    				}
	    			}
	    			return;
	    		}
	    		case Constants.UPGRADE_ACCOUNT_FRAGMENT:{
					log("Back to MyAccountFragment");
					displayedAccountType=-1;
	    			if (upAFL != null){
	    				drawerItem = DrawerItem.ACCOUNT;
						accountFragment=Constants.MY_ACCOUNT_FRAGMENT;
	    				selectDrawerItemLollipop(drawerItem);
	    				if (nV != null){
	    					Menu nVMenu = nV.getMenu();
	    					MenuItem hidden = nVMenu.findItem(R.id.navigation_item_hidden);
	    					resetNavigationViewMenu(nVMenu);
	    					hidden.setChecked(true);
	    				}
	    			}
	    			return;
	    		}
	    		case Constants.CC_FRAGMENT:{
	    			if (ccFL != null){
						displayedAccountType = ccFL.getParameterType();
	    			}
					showUpAF();
	    			return;
	    		}
	    		case Constants.OVERQUOTA_ALERT:{
	    			if (upAFL != null){
	    				drawerItem = DrawerItem.CLOUD_DRIVE;
	    				if (nV != null){
							Menu nVMenu = nV.getMenu();
							MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
							resetNavigationViewMenu(nVMenu);
							cloudDrive.setChecked(true);
							cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
						}
						selectDrawerItemLollipop(drawerItem);
	    			}
	    			return;
	    		}
	    		case Constants.MONTHLY_YEARLY_FRAGMENT:{
	    			if (myFL != null){
	    				myFL.onBackPressed();
	    			}
	    			return;
	    		}
	    		default:{
	    			if (fbFLol != null){
	    				drawerItem = DrawerItem.CLOUD_DRIVE;
	    				if (nV != null){
							Menu nVMenu = nV.getMenu();
							MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
							resetNavigationViewMenu(nVMenu);
							cloudDrive.setChecked(true);
							cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
						}
						selectDrawerItemLollipop(drawerItem);
	    			}
	    		}
    		}
    	}
		else if (drawerItem == DrawerItem.CAMERA_UPLOADS){
			if (cuFL != null){
    			if (cuFL.onBackPressed() == 0){
    				drawerItem = DrawerItem.CLOUD_DRIVE;
    				if (nV != null){
						Menu nVMenu = nV.getMenu();
						MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
						resetNavigationViewMenu(nVMenu);
						cloudDrive.setChecked(true);
						cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
					}
					selectDrawerItemLollipop(drawerItem);
    				return;
    			}
    		}
    	}
		else if (drawerItem == DrawerItem.MEDIA_UPLOADS){
			if (muFLol != null){
    			if (muFLol.onBackPressed() == 0){
    				drawerItem = DrawerItem.CLOUD_DRIVE;
    				if (nV != null){
						Menu nVMenu = nV.getMenu();
						MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
						resetNavigationViewMenu(nVMenu);
						cloudDrive.setChecked(true);
						cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
					}
					selectDrawerItemLollipop(drawerItem);
    				return;
    			}
    		}
    	}
		else if (drawerItem == DrawerItem.SEARCH){
    		if (sFLol != null){
    			if (sFLol.onBackPressed() == 0){
    				drawerItem = DrawerItem.CLOUD_DRIVE;
    				if (nV != null){
						Menu nVMenu = nV.getMenu();
						MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
						resetNavigationViewMenu(nVMenu);
						cloudDrive.setChecked(true);
						cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
					}
    				selectDrawerItemLollipop(drawerItem);
    				return;
    			}
    		}
    	}
		else{
			super.onBackPressed();
			return;
		}
	}

	@Override
	public boolean onNavigationItemSelected(MenuItem menuItem) {

		switch (menuItem.getItemId()){
			case R.id.navigation_item_cloud_drive:{
//				Snackbar.make(fragmentContainer, menuItem.getTitle() + " (" + menuItem.getItemId() + ")", Snackbar.LENGTH_LONG).show();
				drawerMenuItem = menuItem;
				drawerItem = DrawerItem.CLOUD_DRIVE;
				if (nV != null){
					Menu nVMenu = nV.getMenu();
					resetNavigationViewMenu(nVMenu);
				}
				menuItem.setChecked(true);
				menuItem.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
				selectDrawerItemLollipop(drawerItem);
				break;
			}
			case R.id.navigation_item_saved_for_offline:{
//				Snackbar.make(fragmentContainer, menuItem.getTitle() + " (" + menuItem.getItemId() + ")", Snackbar.LENGTH_LONG).show();
				drawerMenuItem = menuItem;
				drawerItem = DrawerItem.SAVED_FOR_OFFLINE;
				if (nV != null){
					Menu nVMenu = nV.getMenu();
					resetNavigationViewMenu(nVMenu);
				}
				menuItem.setChecked(true);
				menuItem.setIcon(getResources().getDrawable(R.drawable.saved_for_offline_red));
				selectDrawerItemLollipop(drawerItem);
				break;
			}
			case R.id.navigation_item_camera_uploads:{
//				Snackbar.make(fragmentContainer, menuItem.getTitle() + " (" + menuItem.getItemId() + ")", Snackbar.LENGTH_LONG).show();
				drawerMenuItem = menuItem;
				drawerItem = DrawerItem.CAMERA_UPLOADS;
				if (nV != null){
					Menu nVMenu = nV.getMenu();
					resetNavigationViewMenu(nVMenu);
				}
				menuItem.setChecked(true);
				menuItem.setIcon(getResources().getDrawable(R.drawable.camera_uploads_red));
				selectDrawerItemLollipop(drawerItem);
				break;
			}
			case R.id.navigation_item_inbox:{
//				Snackbar.make(fragmentContainer, menuItem.getTitle() + " (" + menuItem.getItemId() + ")", Snackbar.LENGTH_LONG).show();
				drawerMenuItem = menuItem;
				drawerItem = DrawerItem.INBOX;
				if (nV != null){
					Menu nVMenu = nV.getMenu();
					resetNavigationViewMenu(nVMenu);
				}
				menuItem.setChecked(true);
				menuItem.setIcon(getResources().getDrawable(R.drawable.inbox_red));
				selectDrawerItemLollipop(drawerItem);
				break;
			}
			case R.id.navigation_item_shared_items:{
//				Snackbar.make(fragmentContainer, menuItem.getTitle() + " (" + menuItem.getItemId() + ")", Snackbar.LENGTH_LONG).show();
				drawerMenuItem = menuItem;
				drawerItem = DrawerItem.SHARED_ITEMS;
				if (nV != null){
					Menu nVMenu = nV.getMenu();
					resetNavigationViewMenu(nVMenu);
				}
				menuItem.setChecked(true);
				menuItem.setIcon(getResources().getDrawable(R.drawable.shared_items_red));
				selectDrawerItemLollipop(drawerItem);
				break;
			}
			case R.id.navigation_item_chat:{
				drawerMenuItem = menuItem;
				drawerItem = DrawerItem.CHAT;
				if (nV != null){
					Menu nVMenu = nV.getMenu();
					resetNavigationViewMenu(nVMenu);
				}
				menuItem.setChecked(true);
				menuItem.setIcon(getResources().getDrawable(R.drawable.ic_menu_chat_red));
				selectDrawerItemLollipop(drawerItem);
				break;
			}
			case R.id.navigation_item_contacts:{
//				Snackbar.make(fragmentContainer, menuItem.getTitle() + " (" + menuItem.getItemId() + ")", Snackbar.LENGTH_LONG).show();
				drawerMenuItem = menuItem;
				drawerItem = DrawerItem.CONTACTS;
				if (nV != null){
					Menu nVMenu = nV.getMenu();
					resetNavigationViewMenu(nVMenu);
				}
				menuItem.setChecked(true);
				menuItem.setIcon(getResources().getDrawable(R.drawable.contacts_red));
				selectDrawerItemLollipop(drawerItem);
				break;
			}
			case R.id.navigation_item_settings:{
//				Snackbar.make(fragmentContainer, menuItem.getTitle() + " (" + menuItem.getItemId() + ")", Snackbar.LENGTH_LONG).show();
				lastDrawerItem = drawerItem;
				drawerItem = DrawerItem.SETTINGS;
				if (nV != null){
					Menu nVMenu = nV.getMenu();
					resetNavigationViewMenu(nVMenu);
				}
				menuItem.setChecked(true);
				menuItem.setIcon(getResources().getDrawable(R.drawable.settings_red));
				selectDrawerItemLollipop(drawerItem);
				break;
			}
		}
		drawerLayout.closeDrawer(Gravity.LEFT);

		return true;
	}

	public void showSnackbar(String s){
		log("showSnackbar");
		Snackbar snackbar = Snackbar.make(fragmentContainer, s, Snackbar.LENGTH_LONG);
		TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
		snackbarTextView.setMaxLines(5);
		snackbar.show();
	}

	public void askConfirmationNoAppInstaledBeforeDownload (String parentPath, String url, long size, long [] hashes, String nodeToDownload){
		log("askConfirmationNoAppInstaledBeforeDownload");

		final String parentPathC = parentPath;
		final String urlC = url;
		final long [] hashesC = hashes;
		final long sizeC=size;

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		LinearLayout confirmationLayout = new LinearLayout(this);
		confirmationLayout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(10, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

		final CheckBox dontShowAgain =new CheckBox(this);
		dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
		dontShowAgain.setTextColor(getResources().getColor(R.color.text_secondary));

		confirmationLayout.addView(dontShowAgain, params);

		builder.setView(confirmationLayout);

//				builder.setTitle(getString(R.string.confirmation_required));
		builder.setMessage(getString(R.string.alert_no_app, nodeToDownload));
		builder.setPositiveButton(getString(R.string.general_download),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if(dontShowAgain.isChecked()){
							dbH.setAttrAskNoAppDownload("false");
						}
						nC.download(parentPathC, urlC, sizeC, hashesC);
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


	public void askSizeConfirmationBeforeDownload(String parentPath, String url, long size, long [] hashes){
		log("askSizeConfirmationBeforeDownload");

		final String parentPathC = parentPath;
		final String urlC = url;
		final long [] hashesC = hashes;
		final long sizeC=size;

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		LinearLayout confirmationLayout = new LinearLayout(this);
		confirmationLayout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(10, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

		final CheckBox dontShowAgain =new CheckBox(this);
		dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
		dontShowAgain.setTextColor(getResources().getColor(R.color.text_secondary));

		confirmationLayout.addView(dontShowAgain, params);

		builder.setView(confirmationLayout);

//				builder.setTitle(getString(R.string.confirmation_required));

		builder.setMessage(getString(R.string.alert_larger_file, Util.getSizeString(sizeC)));
		builder.setPositiveButton(getString(R.string.general_download),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if(dontShowAgain.isChecked()){
							dbH.setAttrAskSizeDownload("false");
						}
						nC.checkInstalledAppBeforeDownload(parentPathC, urlC, sizeC, hashesC);
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

	public void showRenameDialog(final MegaNode document, String text){
		log("showRenameDialog");

		LinearLayout layout = new LinearLayout(this);
	    layout.setOrientation(LinearLayout.VERTICAL);
	    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);
//	    layout.setLayoutParams(params);

		final EditTextCursorWatcher input = new EditTextCursorWatcher(this, document.isFolder());
//		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setTextColor(getResources().getColor(R.color.text_secondary));
//		input.setHint(getString(R.string.context_new_folder_name));
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);

		input.setImeActionLabel(getString(R.string.context_rename),EditorInfo.IME_ACTION_DONE);
		input.setText(text);
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(final View v, boolean hasFocus) {
				if (hasFocus) {
					if (document.isFolder()){
						input.setSelection(0, input.getText().length());
					}
					else{
						String [] s = document.getName().split("\\.");
						if (s != null){
							int numParts = s.length;
							int lastSelectedPos = 0;
							if (numParts == 1){
								input.setSelection(0, input.getText().length());
							}
							else if (numParts > 1){
								for (int i=0; i<(numParts-1);i++){
									lastSelectedPos += s[i].length();
									lastSelectedPos++;
								}
								lastSelectedPos--; //The last point should not be selected)
								input.setSelection(0, lastSelectedPos);
							}
						}
						showKeyboardDelayed(v);
					}
				}
			}
		});

		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					renameDialog.dismiss();
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						return true;
					}
					nC.renameNode(document, value);
					return true;
				}
				return false;
			}
		});

	    layout.addView(input, params);

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setTitle(getString(R.string.context_rename) + " "	+ new String(document.getName()));
		builder.setPositiveButton(getString(R.string.context_rename),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() == 0) {
							return;
						}
						nC.renameNode(document, value);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		builder.setView(layout);
		renameDialog = builder.create();
		renameDialog.show();

	}

	public void showGetLinkPanel(final String link, long expirationTimestamp){
		log("showGetLinkPanel: "+link);

		final Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DAY_OF_MONTH);

		final DatePickerDialog datePickerDialog = new DatePickerDialog(managerActivity, this, year, month, day);
		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);

		builder.setTitle(getString(R.string.context_get_link_menu));

		LayoutInflater inflater = getLayoutInflater();
		View dialoglayout = inflater.inflate(R.layout.panel_get_link, null);

		final CheckedTextView linkWithoutKeyCheck = (CheckedTextView) dialoglayout.findViewById(R.id.link_without_key);
		linkWithoutKeyCheck.setChecked(true);
		linkWithoutKeyCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
		linkWithoutKeyCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
		ViewGroup.MarginLayoutParams linkWOK = (ViewGroup.MarginLayoutParams) linkWithoutKeyCheck.getLayoutParams();
		linkWOK.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(14, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

		final CheckedTextView linkDecryptionKeyCheck = (CheckedTextView) dialoglayout.findViewById(R.id.link_decryption_key);
		linkDecryptionKeyCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
		linkDecryptionKeyCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
		ViewGroup.MarginLayoutParams linkDecry = (ViewGroup.MarginLayoutParams) linkDecryptionKeyCheck.getLayoutParams();
		linkDecry.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

		final CheckedTextView linkWithKeyCheck = (CheckedTextView) dialoglayout.findViewById(R.id.link_with_key);
		linkWithKeyCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
		linkWithKeyCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
		ViewGroup.MarginLayoutParams linkWK = (ViewGroup.MarginLayoutParams) linkWithKeyCheck.getLayoutParams();
		linkWK.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

		RelativeLayout expiryDateLayout = (RelativeLayout) dialoglayout.findViewById(R.id.expiry_date_layout);
		LinearLayout.LayoutParams paramsDateLayout = (LinearLayout.LayoutParams)expiryDateLayout.getLayoutParams();
		paramsDateLayout.setMargins(Util.scaleWidthPx(26, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, 0);
		expiryDateLayout.setLayoutParams(paramsDateLayout);

		TextView expiryDateTitle = (TextView) dialoglayout.findViewById(R.id.title_set_expiry_date);
		expiryDateTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));

		TextView expiryDateSubtitle = (TextView) dialoglayout.findViewById(R.id.subtitle_set_expiry_date);
		expiryDateSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleText));

		expiryDateButton = (Button) dialoglayout.findViewById(R.id.expiry_date);
		LinearLayout.LayoutParams paramsExpiryDate = (LinearLayout.LayoutParams)expiryDateButton.getLayoutParams();
		paramsExpiryDate.setMargins(Util.scaleWidthPx(20, outMetrics), 0, 0, 0);
		expiryDateButton.setLayoutParams(paramsExpiryDate);

		final TextView linkText = (TextView) dialoglayout.findViewById(R.id.link);
		linkText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleText));
		LinearLayout.LayoutParams paramsLink = (LinearLayout.LayoutParams)linkText.getLayoutParams();
		paramsLink.setMargins(Util.scaleWidthPx(26, outMetrics), Util.scaleHeightPx(3, outMetrics), Util.scaleWidthPx(16, outMetrics), Util.scaleHeightPx(6, outMetrics));
		linkText.setLayoutParams(paramsLink);

		switchGetLink = (SwitchCompat) dialoglayout.findViewById(R.id.switch_set_expiry_date);
		RelativeLayout.LayoutParams paramsSwitch = (RelativeLayout.LayoutParams)switchGetLink.getLayoutParams();
		paramsSwitch.setMargins(0, 0, Util.scaleWidthPx(16, outMetrics), 0);
		switchGetLink.setLayoutParams(paramsSwitch);

		linkWithoutKeyCheck.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				linkWithoutKeyCheck.setChecked(true);
				linkDecryptionKeyCheck.setChecked(false);
				linkWithKeyCheck.setChecked(false);
				String urlString="";
				String [] s = link.split("!");
				if (s.length == 3){
					urlString = s[0] + "!" + s[1];
				}
				linkText.setText(urlString);
			}
		});

		linkDecryptionKeyCheck.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				linkWithoutKeyCheck.setChecked(false);
				linkDecryptionKeyCheck.setChecked(true);
				linkWithKeyCheck.setChecked(false);
				String keyString="!";
				String [] s = link.split("!");
				if (s.length == 3){
					keyString = keyString+s[2];
				}
				linkText.setText(keyString);
			}
		});

		linkWithKeyCheck.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				linkWithoutKeyCheck.setChecked(false);
				linkDecryptionKeyCheck.setChecked(false);
				linkWithKeyCheck.setChecked(true);
				linkText.setText(link);
			}
		});

		datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.general_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_NEGATIVE) {
					log("Negative button of DatePicker clicked");
					switchGetLink.setChecked(false);
					expiryDateButton.setVisibility(View.INVISIBLE);
				}
			}
		});
		//Set by default, link without key
		String urlString="";
		String [] s = link.split("!");
		if (s.length == 3){
			urlString = s[0] + "!" + s[1];
		}
		linkText.setText(urlString);
		linkWithoutKeyCheck.setChecked(true);

		builder.setView(dialoglayout);
//
		builder.setPositiveButton(getString(R.string.context_send), new android.content.DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_TEXT, linkText.getText());
				startActivity(Intent.createChooser(intent, getString(R.string.context_get_link)));
			}
		});

		builder.setNegativeButton(getString(R.string.context_copy), new android.content.DialogInterface.OnClickListener() {

			@SuppressLint("NewApi")
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
					android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					clipboard.setText(link);
				} else {
					android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", linkText.getText());
					clipboard.setPrimaryClip(clip);
				}
				Snackbar.make(fragmentContainer, getString(R.string.file_properties_get_link), Snackbar.LENGTH_LONG).show();
			}
		});

		getLinkDialog = builder.create();

		expiryDateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				datePickerDialog.show();
			}
		});

		if(myAccountInfo.getAccountType()>MegaAccountDetails.ACCOUNT_TYPE_FREE){
			log("The user is PRO - enable expiration date");

			if(expirationTimestamp<=0){
				switchGetLink.setChecked(false);
				expiryDateButton.setVisibility(View.INVISIBLE);
			}
			else{
				switchGetLink.setChecked(true);
				java.text.DateFormat df = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault());
				Calendar cal = Util.calculateDateFromTimestamp(expirationTimestamp);
				TimeZone tz = cal.getTimeZone();
				df.setTimeZone(tz);
				Date date = cal.getTime();
				String formattedDate = df.format(date);
				expiryDateButton.setText(formattedDate);
				expiryDateButton.setVisibility(View.VISIBLE);
			}

			switchGetLink.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(switchGetLink.isChecked()){
						datePickerDialog.show();
					}
					else{
						isExpiredDateLink=true;
						nC.exportLink(selectedNode);
					}
				}
			});
		}
		else{
			log("The is user is not PRO");
			switchGetLink.setEnabled(false);
			expiryDateButton.setVisibility(View.INVISIBLE);
		}

		log("show getLinkDialog");
		getLinkDialog.show();
	}

	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		log("onDateSet: "+year+monthOfYear+dayOfMonth);

		Calendar cal = Calendar.getInstance();
		cal.set(year, monthOfYear, dayOfMonth);
		Date date = cal.getTime();
		SimpleDateFormat dfTimestamp = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
		String dateString = dfTimestamp.format(date);
		dateString = dateString + "2359";
		log("the date string is: "+dateString);
		int timestamp = (int) Util.calculateTimestamp(dateString);
		log("the TIMESTAMP is: "+timestamp);
		isExpiredDateLink=true;
		nC.exportLinkTimestamp(selectedNode, timestamp);
	}

	/*
	 * Display keyboard
	 */
	private void showKeyboardDelayed(final View view) {
		log("showKeyboardDelayed");
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}

	public void setSendToInbox(boolean value){
		log("setSendToInbox: "+value);
		sendToInbox = value;
	}

	public void setIsGetLink(boolean value){
		this.isGetLink = value;
	}

	public void cancelTransfer (MegaTransfer t){
		log("cancelTransfer");
		final MegaTransfer mT = t;

		//Show confirmation message
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		        	log("Pressed button positive to cancel transfer");
		    		megaApi.cancelTransfer(mT, managerActivity);
		            break;

		        case DialogInterface.BUTTON_NEGATIVE:
		            break;
		        }
		    }
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//		builder.setTitle(getResources().getString(R.string.cancel_transfer_title));
        builder.setMessage(getResources().getString(R.string.cancel_transfer_confirmation));
        builder.setPositiveButton(R.string.general_yes, dialogClickListener);
        builder.setNegativeButton(R.string.general_no, dialogClickListener);
        builder.show();

	}

	public void setIsClearRubbishBin(boolean value){
		this.isClearRubbishBin = value;
	}

	public void setMoveToRubbish(boolean value){
		this.moveToRubbish = value;
	}

	public void askConfirmationMoveToRubbish(final ArrayList<Long> handleList){
		log("askConfirmationMoveToRubbish");
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
				MegaNode p = megaApi.getNodeByHandle(handleList.get(0));
				while (megaApi.getParentNode(p) != null){
					p = megaApi.getParentNode(p);
				}
				if (p.getHandle() != megaApi.getRubbishNode().getHandle()){
					setMoveToRubbish(true);
					AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//				builder.setTitle(getResources().getString(R.string.section_rubbish_bin));
					if (handleList.size() > 1){
						builder.setMessage(getResources().getString(R.string.confirmation_move_to_rubbish_plural));
					}
					else{
						builder.setMessage(getResources().getString(R.string.confirmation_move_to_rubbish));
					}
					builder.setPositiveButton(R.string.general_move, dialogClickListener);
					builder.setNegativeButton(R.string.general_cancel, dialogClickListener);
					builder.show();
				}
				else{
					setMoveToRubbish(false);
					AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//				builder.setTitle(getResources().getString(R.string.title_delete_from_mega));
					if (handleList.size() > 1){
						builder.setMessage(getResources().getString(R.string.confirmation_delete_from_mega_plural));
					}
					else{
						builder.setMessage(getResources().getString(R.string.confirmation_delete_from_mega));
					}
					builder.setPositiveButton(R.string.context_delete, dialogClickListener);
					builder.setNegativeButton(R.string.general_cancel, dialogClickListener);
					builder.show();
				}
			}
		}
		else{
			log("handleList NULL");
			return;
		}

	}

	public void showDialogInsertPassword(String link, boolean cancelAccount){
		log("showDialogInsertPassword");

		final String confirmationLink = link;
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

		final EditText input = new EditText(this);
		layout.addView(input, params);

//		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setHint(getString(R.string.edit_text_insert_pass));
		input.setTextColor(getResources().getColor(R.color.text_secondary));
		input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
//		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		if(cancelAccount){
			log("cancelAccount action");
			input.setOnEditorActionListener(new OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						String pass = input.getText().toString().trim();
						if(pass.equals("")||pass.isEmpty()){
							log("input is empty");
							input.setError(getString(R.string.invalid_string));
							input.requestFocus();
						}
						else {
							log("action DONE ime - cancel account");
							aC.confirmDeleteAccount(confirmationLink, pass);
							insertPassDialog.dismiss();
						}
					}
					else{
						log("other IME" + actionId);
					}
					return false;
				}
			});
			input.setImeActionLabel(getString(R.string.context_delete),EditorInfo.IME_ACTION_DONE);
			builder.setTitle(getString(R.string.delete_account));
			builder.setMessage(getString(R.string.delete_account_text_last_step));
			builder.setPositiveButton(getString(R.string.context_delete),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

						}
					});
		}
		else{
			log("changeMail action");
			input.setOnEditorActionListener(new OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						String pass = input.getText().toString().trim();
						if(pass.equals("")||pass.isEmpty()){
							log("input is empty");
							input.setError(getString(R.string.invalid_string));
							input.requestFocus();
						}
						else {
							log("action DONE ime - change mail");
							aC.confirmChangeMail(confirmationLink, pass);
							insertPassDialog.dismiss();
						}
					}
					else{
						log("other IME" + actionId);
					}
					return false;
				}
			});
			input.setImeActionLabel(getString(R.string.change_pass),EditorInfo.IME_ACTION_DONE);
			builder.setTitle(getString(R.string.change_mail_title_last_step));
			builder.setMessage(getString(R.string.change_mail_text_last_step));
			builder.setPositiveButton(getString(R.string.change_pass),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

						}
					});
		}

		builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				View view = getCurrentFocus();
				if (view != null) {
					InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				}
			}
		});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		builder.setView(layout);
		insertPassDialog = builder.create();
		insertPassDialog.show();
		if(cancelAccount){
			insertPassDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					log("OK BTTN PASSWORD");
					String pass = input.getText().toString().trim();
					if(pass.equals("")||pass.isEmpty()){
						log("input is empty");
						input.setError(getString(R.string.invalid_string));
						input.requestFocus();
					}
					else {
						log("positive button pressed - cancel account");
						aC.confirmDeleteAccount(confirmationLink, pass);
						insertPassDialog.dismiss();
					}
				}
			});
		}
		else{
			insertPassDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					log("OK BTTN PASSWORD");
					String pass = input.getText().toString().trim();
					if(pass.equals("")||pass.isEmpty()){
						log("input is empty");
						input.setError(getString(R.string.invalid_string));
						input.requestFocus();
					}
					else {
						log("positive button pressed - change mail");
						aC.confirmChangeMail(confirmationLink, pass);
						insertPassDialog.dismiss();
					}
				}
			});
		}

	}

	public void askConfirmationDeleteAccount(){
		log("askConfirmationDeleteAccount");

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

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setTitle(getString(R.string.delete_account));

		builder.setMessage(getResources().getString(R.string.delete_account_text));

		builder.setPositiveButton(R.string.context_delete, dialogClickListener);
		builder.setNegativeButton(R.string.general_cancel, dialogClickListener);
		builder.show();
	}


	public void showImportLinkDialog(){
		log("showImportLinkDialog");
		LinearLayout layout = new LinearLayout(this);
	    layout.setOrientation(LinearLayout.VERTICAL);
	    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

	    final EditText input = new EditText(this);
//		input.setId(EDIT_TEXT_ID);
		input.setSingleLine(false);

		input.setTextColor(getResources().getColor(R.color.text_secondary));
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
	    layout.addView(input, params);
		input.setImeActionLabel(getString(R.string.context_open_link_title),EditorInfo.IME_ACTION_DONE);

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setTitle(getString(R.string.context_open_link_title));
		builder.setPositiveButton(getString(R.string.context_open_link),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() == 0) {
							return;
						}

						try{
							openLinkDialog.dismiss();
						}
						catch(Exception e){}
						nC.importLink(value);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		builder.setView(layout);
		openLinkDialog = builder.create();
		openLinkDialog.show();

		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					try{
						openLinkDialog.dismiss();
					}
					catch(Exception e){}

					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						return true;
					}
					nC.importLink(value);
					return true;
				}
				return false;
			}
		});
	}

	public void takePicture(){
		log("takePicture");
		String path = Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+ Util.temporalPicDIR;
        File newFolder = new File(path);
        newFolder.mkdirs();

        String file = path + "/picture.jpg";
        File newFile = new File(file);
        try {
        	newFile.createNewFile();
        } catch (IOException e) {}

        Uri outputFileUri = Uri.fromFile(newFile);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        startActivityForResult(cameraIntent, Constants.TAKE_PHOTO_CODE);
	}

	public void showCancelMessage(){
		AlertDialog cancelDialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//		builder.setTitle(getString(R.string.title_cancel_subscriptions));

		LayoutInflater inflater = getLayoutInflater();
		View dialogLayout = inflater.inflate(R.layout.dialog_cancel_subscriptions, null);
		TextView message = (TextView) dialogLayout.findViewById(R.id.dialog_cancel_text);
		final EditText text = (EditText) dialogLayout.findViewById(R.id.dialog_cancel_feedback);

		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		message.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleW));
		text.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleW));

		builder.setView(dialogLayout);

		builder.setPositiveButton(getString(R.string.send_cancel_subscriptions), new android.content.DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				feedback = text.getText().toString();
				if(feedback.matches("")||feedback.isEmpty()){
					Snackbar.make(fragmentContainer, getString(R.string.reason_cancel_subscriptions), Snackbar.LENGTH_LONG).show();
				}
				else{
					showCancelConfirmation(feedback);
				}
			}
		});

		builder.setNegativeButton(getString(R.string.dismiss_cancel_subscriptions), new android.content.DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});

		cancelDialog = builder.create();
		cancelDialog.show();
//		Util.brandAlertDialog(cancelDialog);
	}

	public void showCancelConfirmation(final String feedback){
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
			        case DialogInterface.BUTTON_POSITIVE:
			        {
			        	log("Feedback: "+feedback);
			        	megaApi.creditCardCancelSubscriptions(feedback, managerActivity);
			        	break;
			        }
			        case DialogInterface.BUTTON_NEGATIVE:
			        {
			            //No button clicked
			        	log("Feedback: "+feedback);
			            break;
			        }
		        }
		    }
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setMessage(R.string.confirmation_cancel_subscriptions).setPositiveButton(R.string.general_yes, dialogClickListener)
		    .setNegativeButton(R.string.general_no, dialogClickListener).show();

	}

	public void showNewFolderDialog(){
		log("showNewFolderDialogKitLollipop");
		if (drawerItem == DrawerItem.CLOUD_DRIVE){
			fbFLol.setPositionClicked(-1);
			fbFLol.notifyDataSetChanged();
		}

		LinearLayout layout = new LinearLayout(this);
	    layout.setOrientation(LinearLayout.VERTICAL);
	    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

	    final EditText input = new EditText(this);
	    layout.addView(input, params);

//		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setTextColor(getResources().getColor(R.color.text_secondary));
		input.setHint(getString(R.string.context_new_folder_name));
//		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						return true;
					}
					createFolder(value);
					newFolderDialog.dismiss();
					return true;
				}
				return false;
			}
		});
		input.setImeActionLabel(getString(R.string.general_create),EditorInfo.IME_ACTION_DONE);
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showKeyboardDelayed(v);
				}
			}
		});

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
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
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		builder.setView(layout);
		newFolderDialog = builder.create();
		newFolderDialog.show();
	}

	private void createFolder(String title) {
		log("createFolder");
		if (!Util.isOnline(this)){
			Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
			return;
		}

		if(isFinishing()){
			return;
		}

		long parentHandle=-1;
		if (drawerItem == DrawerItem.CLOUD_DRIVE){
			parentHandle = fbFLol.getParentHandle();
			if(parentHandle==-1){
				parentHandle= megaApi.getRootNode().getHandle();
			}
		}
		else if(drawerItem == DrawerItem.SHARED_ITEMS){
			int index = viewPagerShares.getCurrentItem();
			if (index == 0){
				String cFTag = getFragmentTag(R.id.shares_tabs_pager, 0);
				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
				if (inSFLol != null){
					parentHandle = inSFLol.getParentHandle();
				}
			}
			else{
				String cFTag = getFragmentTag(R.id.shares_tabs_pager, 1);
				outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
				if (outSFLol != null){
					parentHandle = outSFLol.getParentHandle();
				}
			}
		}
		else{
			return;
		}

		if(parentHandle!=-1){
			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);

			if (parentNode == null){
				parentNode = megaApi.getRootNode();
			}

			boolean exists = false;
			ArrayList<MegaNode> nL = megaApi.getChildren(parentNode);
			for (int i=0;i<nL.size();i++){
				if (title.compareTo(nL.get(i).getName()) == 0){
					exists = true;
				}
			}

			if (!exists){
				statusDialog = null;
				try {
					statusDialog = new ProgressDialog(this);
					statusDialog.setMessage(getString(R.string.context_creating_folder));
					statusDialog.show();
				}
				catch(Exception e){
					return;
				}

				megaApi.createFolder(title, parentNode, this);
			}
			else{
				Snackbar.make(fragmentContainer, getString(R.string.context_folder_already_exists), Snackbar.LENGTH_LONG).show();
			}
		}
		else{
			log("Incorrect parentHandle");
		}
	}

	public void showClearRubbishBinDialog(String editText){
		log("showClearRubbishBinDialog");

		if (rbFLol.isVisible()){
			rbFLol.setPositionClicked(-1);
			rbFLol.notifyDataSetChanged();
		}

		String text;
		if ((editText == null) || (editText.compareTo("") == 0)){
			text = getString(R.string.context_clear_rubbish);
		}
		else{
			text = editText;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//		builder.setTitle(getString(R.string.context_clear_rubbish));
		builder.setMessage(getString(R.string.clear_rubbish_confirmation));
		builder.setPositiveButton(getString(R.string.context_delete),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						nC.cleanRubbishBin();
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		clearRubbishBinDialog = builder.create();
		clearRubbishBinDialog.show();
	}



//	public void upgradeAccountButton(){
//		log("upgradeAccountButton");
//		drawerItem = DrawerItem.ACCOUNT;
//		if (accountInfo != null){
//			if ((accountInfo.getSubscriptionStatus() == MegaAccountDetails.SUBSCRIPTION_STATUS_NONE) || (accountInfo.getSubscriptionStatus() == MegaAccountDetails.SUBSCRIPTION_STATUS_INVALID)){
//				Time now = new Time();
//				now.setToNow();
//				if (accountType != 0){
//					log("accountType != 0");
//					if (now.toMillis(false) >= (accountInfo.getProExpiration()*1000)){
//						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD) || Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_FORTUMO) || Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CENTILI)){
//							log("SUBSCRIPTION INACTIVE: CHECKBITSET --> CC || FORT || INFO");
//							showUpAF(null);
//						}
//						else{
//							Snackbar.make(fragmentContainer, getString(R.string.not_upgrade_is_possible), Snackbar.LENGTH_LONG).show();
//
//						}
//					}
//					else{
//						log("CURRENTLY ACTIVE SUBSCRIPTION");
//						Snackbar.make(fragmentContainer, getString(R.string.not_upgrade_is_possible), Snackbar.LENGTH_LONG).show();
//					}
//				}
//				else{
//					log("accountType == 0");
//					if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD) || Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_FORTUMO) || Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CENTILI)){
//						log("CHECKBITSET --> CC || FORT || INFO");
//						showUpAF(null);
//					}
//					else{
//						Snackbar.make(fragmentContainer, getString(R.string.not_upgrade_is_possible), Snackbar.LENGTH_LONG).show();
//					}
//				}
//			}
//			else{
//				Snackbar.make(fragmentContainer, getString(R.string.not_upgrade_is_possible), Snackbar.LENGTH_LONG).show();
//			}
//		}
//		else{
//			Snackbar.make(fragmentContainer, getString(R.string.not_upgrade_is_possible), Snackbar.LENGTH_LONG).show();
//		}
//	}


	public void showPanelSetPinLock(){
		log("showPanelSetPinLock");

		AlertDialog setPinDialog;
		LayoutInflater inflater = getLayoutInflater();
		View dialoglayout = inflater.inflate(R.layout.choose_pin_type_dialog, null);

		final CheckedTextView pin4Check = (CheckedTextView) dialoglayout.findViewById(R.id.choose_pin_4_check);
		pin4Check.setText(getString(R.string.four_pin_lock));
		pin4Check.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
		pin4Check.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
		ViewGroup.MarginLayoutParams pin4MLP = (ViewGroup.MarginLayoutParams) pin4Check.getLayoutParams();
		pin4MLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));
		pin4Check.setChecked(true);


		final CheckedTextView pin6Check = (CheckedTextView) dialoglayout.findViewById(R.id.choose_pin_6_check);
		pin6Check.setText(getString(R.string.six_pin_lock));
		pin6Check.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
		pin6Check.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
		ViewGroup.MarginLayoutParams pin6MLP = (ViewGroup.MarginLayoutParams) pin6Check.getLayoutParams();
		pin6MLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

		final CheckedTextView pinANCheck = (CheckedTextView) dialoglayout.findViewById(R.id.choose_pin_alphaN_check);
		pinANCheck.setText(getString(R.string.AN_pin_lock));
		pinANCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
		pinANCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
		ViewGroup.MarginLayoutParams pinANMLP = (ViewGroup.MarginLayoutParams) pinANCheck.getLayoutParams();
		pinANMLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setView(dialoglayout);
		builder.setTitle(getString(R.string.pin_lock_type));

		setPinDialog = builder.create();
		setPinDialog.show();

		final AlertDialog dialog = setPinDialog;

		pin4Check.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dbH.setPinLockType(Constants.PIN_4);
    			if (dialog != null){
    				dialog.dismiss();
    			}
				if(sttFLol!=null){
					sttFLol.intentToPinLock();
				}
			}
		});

		pin6Check.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				pin6Check.setChecked(true);
				pin4Check.setChecked(false);
				dbH.setPinLockType(Constants.PIN_6);
    			if (dialog != null){
    				dialog.dismiss();
    			}
				if(sttFLol!=null){
					sttFLol.intentToPinLock();
				}
			}
		});

		pinANCheck.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				pinANCheck.setChecked(true);
				pin4Check.setChecked(false);
				dbH.setPinLockType(Constants.PIN_ALPHANUMERIC);
    			if (dialog != null){
    				dialog.dismiss();
    			}
				if(sttFLol!=null){
					sttFLol.intentToPinLock();
				}
			}
		});
	}

	public void chooseAddContactDialog(boolean isMegaContact){
		log("chooseAddContactDialog");

		Intent in = new Intent(this, AddContactActivityLollipop.class);
//		in.putExtra("contactType", Constants.CONTACT_TYPE_MEGA);
		if(isMegaContact){
			in.putExtra("contactType", Constants.CONTACT_TYPE_MEGA);
			startActivityForResult(in, Constants.REQUEST_CREATE_CHAT);
		}
		else{

			Dialog addContactDialog;
			String[] addContactOptions = getResources().getStringArray(R.array.add_contact_array);
			AlertDialog.Builder b=new AlertDialog.Builder(this);

			b.setTitle(getResources().getString(R.string.menu_add_contact));
			b.setItems(addContactOptions, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch(which){
						case 0:{
							showNewContactDialog();
							break;
						}
						case 1:{
							addContactFromPhone();
							break;
						}
					}
				}
			});
			b.setNegativeButton(getResources().getString(R.string.general_cancel), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
			addContactDialog = b.create();
			addContactDialog.show();
		}

//		Dialog addContactDialog;
//		String[] addContactOptions = getResources().getStringArray(R.array.add_contact_array);
//		AlertDialog.Builder b=new AlertDialog.Builder(this);
//
//		b.setTitle(getResources().getString(R.string.menu_add_contact));
//		b.setItems(addContactOptions, new DialogInterface.OnClickListener() {
//
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				switch(which){
//					case 0:{
//						showNewContactDialog();
//						break;
//					}
//					case 1:{
//						addContactFromPhone();
//						break;
//					}
//				}
//			}
//		});
//		b.setNegativeButton(getResources().getString(R.string.general_cancel), new DialogInterface.OnClickListener() {
//
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				dialog.cancel();
//			}
//		});
//		addContactDialog = b.create();
//		addContactDialog.show();
	}

	public void addContactFromPhone(){

		Intent in = new Intent(this, AddContactActivityLollipop.class);
		in.putExtra("contactType", Constants.CONTACT_TYPE_DEVICE);
		startActivityForResult(in, Constants.REQUEST_INVITE_CONTACT_FROM_DEVICE);

//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//			boolean hasReadContactsPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
//			if (!hasReadContactsPermission) {
//				log("No read contacts permission");
//				ActivityCompat.requestPermissions((ManagerActivityLollipop)this,
//						new String[]{Manifest.permission.READ_CONTACTS},
//						Constants.REQUEST_READ_CONTACTS);
//				return;
//			}
//		}
//
//		Intent phoneContactIntent = new Intent(this, PhoneContactsActivityLollipop.class);
//		this.startActivity(phoneContactIntent);
	}

	public void showNewContactDialog(){
		log("showNewContactDialog");

		String cFTag = getFragmentTag(R.id.contact_tabs_pager, 0);
		cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
		if (cFLol != null){
			if (drawerItem == DrawerItem.CONTACTS){
				cFLol.setPositionClicked(-1);
				cFLol.notifyDataSetChanged();
			}
		}

		LinearLayout layout = new LinearLayout(this);
	    layout.setOrientation(LinearLayout.VERTICAL);
	    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

		final EditText input = new EditText(this);
	    layout.addView(input, params);

//		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setHint(getString(R.string.context_new_contact_name));
		input.setTextColor(getResources().getColor(R.color.text_secondary));
//		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = input.getText().toString().trim();
					String emailError = Util.getEmailError(value, managerActivity);
					if (emailError != null) {
						input.setError(emailError);
						input.requestFocus();
					} else {
						cC.inviteContact(value);
						addContactDialog.dismiss();
					}
				}
				else{
					log("other IME" + actionId);
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

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setTitle(getString(R.string.menu_add_contact));
		builder.setPositiveButton(getString(R.string.general_add),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		builder.setView(layout);
		addContactDialog = builder.create();
		addContactDialog.show();
		addContactDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String value = input.getText().toString().trim();
				String emailError = Util.getEmailError(value, managerActivity);
				if (emailError != null) {
					input.setError(emailError);
				} else {
					cC.inviteContact(value);
					addContactDialog.dismiss();
				}
			}
		});
	}

	public void showConfirmationRemoveContact(final MegaUser c){
		log("showConfirmationRemoveContact");
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

		AlertDialog.Builder builder = new AlertDialog.Builder(managerActivity, R.style.AppCompatAlertDialogStyle);
		String title = getResources().getQuantityString(R.plurals.title_confirmation_remove_contact, 1);
		builder.setTitle(title);
		String message= getResources().getQuantityString(R.plurals.confirmation_remove_contact, 1);
		builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();

	}

	public void showConfirmationRemoveContacts(final ArrayList<MegaUser> c){
		log("showConfirmationRemoveContactssssss");
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

		AlertDialog.Builder builder = new AlertDialog.Builder(managerActivity, R.style.AppCompatAlertDialogStyle);
		String title = getResources().getQuantityString(R.plurals.title_confirmation_remove_contact, c.size());
		builder.setTitle(title);
		String message= getResources().getQuantityString(R.plurals.confirmation_remove_contact, c.size());
		builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();

	}

	public void showConfirmationRemoveContactRequest(final MegaContactRequest r){
		log("showConfirmationRemoveContactRequest");
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

		AlertDialog.Builder builder = new AlertDialog.Builder(managerActivity, R.style.AppCompatAlertDialogStyle);
		String message= getResources().getString(R.string.confirmation_delete_contact_request,r.getTargetEmail());
		builder.setMessage(message).setPositiveButton(R.string.context_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();

	}

	public void showConfirmationRemoveContactRequests(final List<MegaContactRequest> r){
		log("showConfirmationRemoveContactRequests");
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
		AlertDialog.Builder builder = new AlertDialog.Builder(managerActivity, R.style.AppCompatAlertDialogStyle);
		if(r.size()==1){
			message= getResources().getString(R.string.confirmation_delete_contact_request,r.get(0).getTargetEmail());
		}else{
			message= getResources().getString(R.string.confirmation_remove_multiple_contact_request,r.size());
		}

		builder.setMessage(message).setPositiveButton(R.string.context_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();

	}

	public void showConfirmationLeaveMultipleShares (final ArrayList<Long> handleList){
		log("showConfirmationleaveMultipleShares");

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

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//		builder.setTitle(getResources().getString(R.string.alert_leave_share));
		String message= getResources().getString(R.string.confirmation_leave_share_folder);
		builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
	    	.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationRemoveAllSharingContacts (final ArrayList<MegaShare> shareList, final MegaNode n){
		log("showConfirmationRemoveAllSharingContacts");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						nC.removeAllSharingContacts(shareList, n);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//		builder.setTitle(getResources().getString(R.string.alert_leave_share));
		String message= getResources().getString(R.string.confirmation_remove_outgoing_shares);
		builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationRemovePublicLink (final MegaNode n){
		log("showConfirmationRemovePublicLink");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						nC.removeLink(n);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//		builder.setTitle(getResources().getString(R.string.alert_leave_share));
		String message= getResources().getString(R.string.context_remove_link_warning_text);
		builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
		}

	public void showConfirmationLeaveIncomingShare (final MegaNode n){
		log("showConfirmationLeaveIncomingShare");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE: {
					//TODO remove the incoming shares
					nC.leaveIncomingShare(n);
					break;
				}
		        case DialogInterface.BUTTON_NEGATIVE:
		            //No button clicked
		            break;
		        }
		    }
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//		builder.setTitle(getResources().getString(R.string.alert_leave_share));
		String message= getResources().getString(R.string.confirmation_leave_share_folder);
		builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
	    	.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationLeaveChat (final MegaChatRoom c){
		log("showConfirmationLeaveChat");

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

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setTitle(getResources().getString(R.string.title_confirmation_leave_group_chat));
		String message= getResources().getString(R.string.confirmation_leave_group_chat);
		builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationLeaveChat (final MegaChatListItem c){
		log("showConfirmationLeaveChat");

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

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setTitle(getResources().getString(R.string.title_confirmation_leave_group_chat));
		String message= getResources().getString(R.string.confirmation_leave_group_chat);
		builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationClearChat(final MegaChatListItem c){
		log("showConfirmationClearChat");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						log("Clear chat!");
//						megaChatApi.truncateChat(chatHandle, MegaChatHandle.MEGACHAT_INVALID_HANDLE);
						log("Clear history selected!");
						ChatController chatC = new ChatController(managerActivity);
						chatC.clearHistory(c.getChatId());
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		String message= getResources().getString(R.string.confirmation_clear_group_chat);
		builder.setTitle(R.string.title_confirmation_clear_group_chat);
		builder.setMessage(message).setPositiveButton(R.string.general_clear, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationResetPasswordFromMyAccount (){
		log("showConfirmationResetPasswordFromMyAccount: ");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {
						String myAccountTag = getFragmentTag(R.id.my_account_tabs_pager, 0);
						maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(myAccountTag);
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

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		String message= getResources().getString(R.string.email_verification_text_change_pass);
		builder.setMessage(message).setPositiveButton(R.string.cam_sync_ok, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationResetPassword (final String link){
		log("showConfirmationResetPassword: "+link);

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {
						Intent intent = new Intent(managerActivity, ChangePasswordActivityLollipop.class);
						intent.setAction(Constants.ACTION_RESET_PASS_FROM_LINK);
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

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setTitle(getResources().getString(R.string.title_dialog_insert_MK));
		String message= getResources().getString(R.string.text_reset_pass_logged_in);
		builder.setMessage(message).setPositiveButton(R.string.pin_lock_enter, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void cameraUploadsClicked(){
		log("cameraUplaodsClicked");
		drawerItem = DrawerItem.CAMERA_UPLOADS;
		if (nV != null){
			Menu nVMenu = nV.getMenu();
			MenuItem cameraUploadsItem = nVMenu.findItem(R.id.navigation_item_camera_uploads);
			drawerMenuItem = cameraUploadsItem;
			resetNavigationViewMenu(nVMenu);
			cameraUploadsItem.setChecked(true);
			cameraUploadsItem.setIcon(getResources().getDrawable(R.drawable.camera_uploads_red));
		}
		selectDrawerItemLollipop(drawerItem);
	}

	public void secondaryMediaUploadsClicked(){
		log("secondaryMediaUploadsClicked");
		drawerItem = DrawerItem.MEDIA_UPLOADS;
//		if (nV != null){
//			Menu nVMenu = nV.getMenu();
//			MenuItem cameraUploadsItem = nVMenu.findItem(R.id.navigation_item_cloud_drive);
//			drawerMenuItem = cameraUploadsItem;
//			resetNavigationViewMenu(nVMenu);
//			cameraUploadsItem.setChecked(true);
//			cameraUploadsItem.setIcon(getResources().getDrawable(R.drawable.camera_uploads_red));
//		}
		selectDrawerItemLollipop(drawerItem);
	}

	public void setInitialCloudDrive (){
		drawerItem = DrawerItem.CLOUD_DRIVE;
		if (nV != null){
			Menu nVMenu = nV.getMenu();
			MenuItem cloudDrive = nVMenu.findItem(R.id.navigation_item_cloud_drive);
			resetNavigationViewMenu(nVMenu);
			cloudDrive.setChecked(true);
			cloudDrive.setIcon(getResources().getDrawable(R.drawable.cloud_drive_red));
		}
		firstTime = true;
		selectDrawerItemLollipop(drawerItem);
		drawerLayout.openDrawer(Gravity.LEFT);
	}

	public void refreshCameraUpload(){
		drawerItem = DrawerItem.CAMERA_UPLOADS;
		if (nV != null){
			Menu nVMenu = nV.getMenu();
			MenuItem cameraUploads = nVMenu.findItem(R.id.navigation_item_camera_uploads);
			resetNavigationViewMenu(nVMenu);
			cameraUploads.setChecked(true);
			cameraUploads.setIcon(getResources().getDrawable(R.drawable.camera_uploads_red));
		}

		Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("cuFLol");
		FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
		fragTransaction.detach(currentFragment);
		fragTransaction.commitNowAllowingStateLoss();

		fragTransaction = getSupportFragmentManager().beginTransaction();
		fragTransaction.attach(currentFragment);
		fragTransaction.commitNowAllowingStateLoss();
	}

	public void showNodeOptionsPanel(MegaNode node){
		log("showNodeOptionsPanel");
		this.selectedNode=node;
		if(node!=null){
			this.selectedNode = node;
			NodeOptionsBottomSheetDialogFragment bottomSheetDialogFragment = new NodeOptionsBottomSheetDialogFragment();
			bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
		}
	}

	public void showOptionsPanel(MegaOffline sNode){
		log("showNodeOptionsPanel-Offline");
		if(sNode!=null){
			this.selectedOfflineNode = sNode;
			OfflineOptionsBottomSheetDialogFragment bottomSheetDialogFragment = new OfflineOptionsBottomSheetDialogFragment();
			bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
		}
	}

	public void showContactOptionsPanel(MegaUser user){
		log("showContactOptionsPanel");
		if(user!=null){
			this.selectedUser = user;
			ContactsBottomSheetDialogFragment bottomSheetDialogFragment = new ContactsBottomSheetDialogFragment();
			bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
		}
	}

	public void showSentRequestOptionsPanel(MegaContactRequest request){
		log("showSentRequestOptionsPanel");
		if(request!=null){
			this.selectedRequest = request;
			SentRequestBottomSheetDialogFragment bottomSheetDialogFragment = new SentRequestBottomSheetDialogFragment();
			bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
		}
	}

	public void showReceivedRequestOptionsPanel(MegaContactRequest request){
		log("showReceivedRequestOptionsPanel");
		if(request!=null){
			this.selectedRequest = request;
			ReceivedRequestBottomSheetDialogFragment bottomSheetDialogFragment = new ReceivedRequestBottomSheetDialogFragment();
			bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
		}
	}

	public void showMyAccountOptionsPanel() {
		log("showMyAccountOptionsPanel");
		MyAccountBottomSheetDialogFragment bottomSheetDialogFragment = new MyAccountBottomSheetDialogFragment();
		bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
	}

	public void showUploadPanel(){
		log("showUploadPanel");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions((ManagerActivityLollipop)this,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						Constants.REQUEST_WRITE_STORAGE);
			}
		}

		UploadBottomSheetDialogFragment bottomSheetDialogFragment = new UploadBottomSheetDialogFragment();
		bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
	}

	private void showOverquotaAlert(){
		log("showOverquotaAlert");
		dbH.setCamSyncEnabled(false);

		if(overquotaDialog==null){
			AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
			builder.setTitle(getString(R.string.overquota_alert_title));
			LayoutInflater inflater = getLayoutInflater();
			View dialoglayout = inflater.inflate(R.layout.dialog_overquota_error, null);
			TextView textOverquota = (TextView) dialoglayout.findViewById(R.id.dialog_overquota);
			builder.setView(dialoglayout);

			builder.setPositiveButton(getString(R.string.my_account_upgrade_pro), new android.content.DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					//Show UpgradeAccountActivity
					FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
					if(upAFL==null){
						upAFL = new UpgradeAccountFragmentLollipop();
						ft.replace(R.id.fragment_container, upAFL, "upAFL");
						drawerItem = DrawerItem.ACCOUNT;
						accountFragment=Constants.OVERQUOTA_ALERT;
						ft.commitNow();
					}
					else{
						ft.replace(R.id.fragment_container, upAFL, "upAFL");
						drawerItem = DrawerItem.ACCOUNT;
						accountFragment=Constants.OVERQUOTA_ALERT;
						ft.commitNow();
					}
				}
			});
			builder.setNegativeButton(getString(R.string.general_cancel), new android.content.DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					overquotaDialog=null;
				}
			});

			overquotaDialog = builder.create();
			overquotaDialog.show();
//			Util.brandAlertDialog(overquotaDialog);
		}
	}

	public void updateAccountDetailsVisibleInfo(){
		log("updateAccountDetailsVisibleInfo");

		usedSpacePB.setProgress(myAccountInfo.getUsedPerc());

//				String usedSpaceString = getString(R.string.used_space, used, total);
		usedSpaceTV.setText(myAccountInfo.getUsedFormatted());
		totalSpaceTV.setText(myAccountInfo.getTotalFormatted());

		usedSpacePB.setProgress(myAccountInfo.getUsedPerc());

//				String usedSpaceString = getString(R.string.used_space, used, total);
		usedSpaceTV.setText(myAccountInfo.getUsedFormatted());
		totalSpaceTV.setText(myAccountInfo.getTotalFormatted());

		if (myAccountInfo.isInventoryFinished()){
			if (myAccountInfo.getLevelAccountDetails() < myAccountInfo.getLevelInventory()){
				if (maxP != null){
					log("ORIGINAL JSON2:" + maxP.getOriginalJson() + ":::");
					megaApi.submitPurchaseReceipt(maxP.getOriginalJson(), this);
				}
			}
		}

		if(myAccountInfo.getUsedPerc()>=95){
			showOverquotaPanel();
		}
		else{
			outSpaceLayout.setVisibility(View.GONE);
			if(myAccountInfo.getAccountType()==0){
				log("usedSpacePerc<95");
				if(Util.showMessageRandom()){
					log("Random: TRUE");
					showProPanel();
				}
			}
		}
//		showOverquotaPanel();

		if (getUsedPerc() < 90){
			usedSpacePB.setProgressDrawable(getResources().getDrawable(R.drawable.custom_progress_bar_horizontal_ok));
//		        	wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.used_space_ok)), 0, used.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		        	usedSpaceWarning.setVisibility(View.INVISIBLE);
		}
		else if ((getUsedPerc() >= 90) && (getUsedPerc() <= 95)){
			usedSpacePB.setProgressDrawable(getResources().getDrawable(R.drawable.custom_progress_bar_horizontal_warning));
//		        	wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.used_space_warning)), 0, used.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		        	usedSpaceWarning.setVisibility(View.VISIBLE);
		}
		else{
			if (getUsedPerc() > 100){
				myAccountInfo.setUsedPerc(100);
			}
			usedSpacePB.setProgressDrawable(getResources().getDrawable(R.drawable.custom_progress_bar_horizontal_exceed));
//		        	wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.used_space_exceed)), 0, used.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		        	usedSpaceWarning.setVisibility(View.VISIBLE);
		}

		if(drawerItem==DrawerItem.CLOUD_DRIVE){
			if (myAccountInfo.getUsedPerc() > 95){
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.detach(fbFLol);
				ft.attach(fbFLol);
				ft.commitAllowingStateLoss();
			}
		}
	}

	public void selectSortByContacts(int _orderContacts){
		log("selectSortByContacts");

		this.orderContacts = _orderContacts;
		this.setOrderContacts(orderContacts);
		String cFTag = getFragmentTag(R.id.contact_tabs_pager, 0);
		cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
		if (cFLol != null){
			cFLol.setOrder(orderContacts);
			if (orderContacts == MegaApiJava.ORDER_DEFAULT_ASC){
				cFLol.sortByNameAscending();
			}
			else{
				cFLol.sortByNameDescending();
			}
		}
	}

	public void selectSortByOffline(int _orderOthers){
		log("selectSortByOffline");

		this.orderOthers = _orderOthers;
		this.setOrderOthers(orderOthers);
		if (oFLol != null){
			oFLol.setOrder(orderOthers);
			if (orderOthers == MegaApiJava.ORDER_DEFAULT_ASC){
				oFLol.sortByNameAscending();
			}
			else{
				oFLol.sortByNameDescending();
			}
		}
	}

	public void selectSortByIncoming(int _orderOthers){
		log("selectSortByIncoming");

		this.orderOthers = _orderOthers;
		this.setOrderOthers(orderOthers);
		if (inSFLol != null){
			inSFLol.setOrder(orderOthers);
			inSFLol.findNodes();
		}
	}

	public void selectSortByOutgoing(int _orderOthers){
		log("selectSortByOutgoing");

		this.orderOthers = _orderOthers;
		this.setOrderOthers(orderOthers);
		if (outSFLol != null){
			outSFLol.setOrder(orderOthers);
			if (orderOthers == MegaApiJava.ORDER_DEFAULT_ASC){
				outSFLol.sortByNameAscending();
			}
			else{
				outSFLol.sortByNameDescending();
			}
		}
	}

	public void selectSortByCloudDrive(int _orderCloud){
		log("selectSortByCloudDrive");

		this.orderCloud = _orderCloud;
		this.setOrderCloud(orderCloud);
		MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
		if (parentNode != null){
			if (fbFLol != null){
				ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderCloud);
				fbFLol.setOrder(orderCloud);
				fbFLol.setNodes(nodes);
				fbFLol.getRecyclerView().invalidate();
			}
		}
		else{
			if (fbFLol != null){
				ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRootNode(), orderCloud);
				fbFLol.setOrder(orderCloud);
				fbFLol.setNodes(nodes);
				fbFLol.getRecyclerView().invalidate();
			}
		}
	}

	public void selectSortByInbox(int _orderCloud){
		log("selectSortByInbox");

		this.orderCloud = _orderCloud;
		this.setOrderCloud(orderCloud);
		MegaNode inboxNode = megaApi.getInboxNode();
		if(inboxNode!=null){
			ArrayList<MegaNode> nodes = megaApi.getChildren(inboxNode, orderCloud);
			if (iFLol != null){
				iFLol.setOrder(orderCloud);
				iFLol.setNodes(nodes);
				iFLol.getRecyclerView().invalidate();
			}
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
		log("setFirstNavigationLevel: set value to "+firstNavigationLevel);
		this.firstNavigationLevel = firstNavigationLevel;
	}

	public boolean isFirstNavigationLevel() {
		return firstNavigationLevel;
	}

	public int getUsedPerc(){
		if(myAccountInfo!=null){
			return myAccountInfo.getUsedPerc();
		}
		return 0;
	}

	public long getParentHandleBrowser() {
		return parentHandleBrowser;
	}

	public long getParentHandleRubbish() {
		return parentHandleRubbish;
	}

	public long getParentHandleIncoming() {
		return parentHandleIncoming;
	}

	public long getParentHandleOutgoing() {
		return parentHandleOutgoing;
	}

	public long getParentHandleSearch() {
		return parentHandleSearch;
	}

	public long getParentHandleInbox() {
		return parentHandleInbox;
	}

	public void setParentHandleBrowser(long parentHandleBrowser){
		log("setParentHandleBrowser: set value to:"+parentHandleBrowser);

		this.parentHandleBrowser = parentHandleBrowser;

		HashMap<Long, MegaTransfer> mTHash = new HashMap<Long, MegaTransfer>();

		//Update transfer list
		tL = megaApi.getTransfers();

		//Update File Browser Fragment
		if (fbFLol != null){
			for(int i=0; i<tL.size(); i++){

				MegaTransfer tempT = tL.get(i);
				if (tempT.getType() == MegaTransfer.TYPE_DOWNLOAD){
					long handleT = tempT.getNodeHandle();
					MegaNode nodeT = megaApi.getNodeByHandle(handleT);
					MegaNode parentT = megaApi.getParentNode(nodeT);

					if (parentT != null){
						if(parentT.getHandle() == this.parentHandleBrowser){
							mTHash.put(handleT,tempT);
						}
					}
				}
			}

			fbFLol.setTransfers(mTHash);
		}
	}

	public void setParentHandleRubbish(long parentHandleRubbish){
		log("setParentHandleRubbish");
		this.parentHandleRubbish = parentHandleRubbish;
	}

	public void setParentHandleSearch(long parentHandleSearch){
		log("setParentHandleSearch");
		this.parentHandleSearch = parentHandleSearch;
	}

	public void setParentHandleIncoming(long parentHandleIncoming){
		log("setParentHandleIncoming: " + parentHandleIncoming);
		this.parentHandleIncoming = parentHandleIncoming;
	}

	public void setParentHandleInbox(long parentHandleInbox){
		log("setParentHandleInbox: " + parentHandleInbox);
		this.parentHandleInbox = parentHandleInbox;
	}

	public void setParentHandleOutgoing(long parentHandleOutgoing){
		log("setParentHandleOutgoing: " + parentHandleOutgoing);
		this.parentHandleOutgoing = parentHandleOutgoing;
	}

	@Override
	protected void onNewIntent(Intent intent){
    	log("onNewIntent");

    	if ((intent != null) && Intent.ACTION_SEARCH.equals(intent.getAction())){
    		searchQuery = intent.getStringExtra(SearchManager.QUERY);
    		parentHandleSearch = -1;
    		aB.setTitle(getString(R.string.action_search)+": "+searchQuery);

    		isSearching = true;

    		if (searchMenuItem != null) {
    			MenuItemCompat.collapseActionView(searchMenuItem);
			}
    		return;
    	}
     	super.onNewIntent(intent);
    	setIntent(intent);
    	return;
	}

	@Override
	public void onClick(View v) {
		log("onClick");
		switch(v.getId()){
			case R.id.custom_search:{
				if (searchMenuItem != null) {
					MenuItemCompat.expandActionView(searchMenuItem);
				}
				else{
					log("searchMenuItem == null");
				}
				break;
			}
			case R.id.btnLeft_cancel:{
				getProLayout.setVisibility(View.GONE);
				break;
			}
			case R.id.overquota_alert_btnLeft_cancel:{
				log("outSpace Layout gone!");
				outSpaceLayout.setVisibility(View.GONE);
				break;
			}
			case R.id.btnRight_upgrade:
			case R.id.overquota_alert_btnRight_upgrade:{
				//Add navigation to Upgrade Account
				log("layout PRO gone!");
				drawerItem = DrawerItem.ACCOUNT;
				if (nV != null){
					Menu nVMenu = nV.getMenu();
					MenuItem hidden = nVMenu.findItem(R.id.navigation_item_hidden);
					resetNavigationViewMenu(nVMenu);
					hidden.setChecked(true);
				}
				outSpaceLayout.setVisibility(View.GONE);
				getProLayout.setVisibility(View.GONE);
				drawerItem = DrawerItem.ACCOUNT;
				accountFragment = Constants.UPGRADE_ACCOUNT_FRAGMENT;
				displayedAccountType = -1;
				selectDrawerItemLollipop(drawerItem);
				break;
			}

			case R.id.navigation_drawer_account_view:{
//				Snackbar.make(fragmentContainer, "MyAccount", Snackbar.LENGTH_LONG).show();
				if (Util.isOnline(this)){
					drawerItem = DrawerItem.ACCOUNT;
					accountFragment=Constants.MY_ACCOUNT_FRAGMENT;
					if (nV != null){
						Menu nVMenu = nV.getMenu();
						MenuItem hidden = nVMenu.findItem(R.id.navigation_item_hidden);
						resetNavigationViewMenu(nVMenu);
						hidden.setChecked(true);
					}
					selectDrawerItemLollipop(drawerItem);
				}

				break;
			}
//			case R.id.top_control_bar:{
//				if (nDALol != null){
//					nDALol.setPositionClicked(-1);
//				}
//				drawerItem = DrawerItem.ACCOUNT;
//				titleAB = drawerItem.getTitle(this);
//
//				selectDrawerItemLollipop(drawerItem);
//
//				break;
//			}
//			case R.id.bottom_control_bar:{
//				if (nDALol != null){
//					nDALol.setPositionClicked(-1);
//				}
//				drawerItem = DrawerItem.ACCOUNT;
//				titleAB = drawerItem.getTitle(this);
//
//				selectDrawerItemLollipop(drawerItem);
//
//				break;
//			}
		}
	}

	public void showConfirmationRemoveMK(){
		log("showConfirmationRemoveMK");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						Constants.REQUEST_WRITE_STORAGE);
			}
		}

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						AccountController aC = new AccountController(managerActivity);
						aC.removeMK();
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		}
		else{
			builder = new AlertDialog.Builder(this);
		}
		builder.setMessage(R.string.remove_key_confirmation).setPositiveButton(R.string.general_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationRemoveFromOffline(){
		log("showConfirmationRemoveFromOffline");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						Constants.REQUEST_WRITE_STORAGE);
			}
		}

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE: {
						String pathNavigation = getPathNavigationOffline();
						MegaOffline mOff = getSelectedOfflineNode();
						NodeController nC = new NodeController(managerActivity);
						nC.deleteOffline(mOff, pathNavigation);
						break;
					}
					case DialogInterface.BUTTON_NEGATIVE: {
						//No button clicked
						break;
					}
				}
			}
		};

		AlertDialog.Builder builder;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		}
		else{
			builder = new AlertDialog.Builder(this);
		}
		builder.setMessage(R.string.confirmation_delete_from_save_for_offline).setPositiveButton(R.string.general_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void showConfirmationDeleteAvatar(){
		log("showConfirmationDeleteAvatar");

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

		AlertDialog.Builder builder;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		}
		else{
			builder = new AlertDialog.Builder(this);
		}
		builder.setMessage(R.string.confirmation_delete_avatar).setPositiveButton(R.string.context_delete, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		log("-------------------onActivityResult "+requestCode + "____" + resultCode);

		if (resultCode == RESULT_FIRST_USER){
			Snackbar.make(fragmentContainer, getString(R.string.context_no_destination_folder), Snackbar.LENGTH_LONG).show();
			return;
		}

		if (requestCode == Constants.REQUEST_CODE_TREE && resultCode == RESULT_OK){
			if (intent == null){
				log("intent NULL");
				return;
			}

			Uri treeUri = intent.getData();
	        DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
		}
		if (requestCode == Constants.ENABLE_CHAT && resultCode == RESULT_OK){
			log("OnActivityResult-->connect");

		}
		else if (requestCode == Constants.REQUEST_CODE_GET && resultCode == RESULT_OK) {
			if (intent == null) {
				log("Return.....");
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
		else if (requestCode == Constants.CHOOSE_PICTURE_PROFILE_CODE && resultCode == RESULT_OK) {

			if (resultCode == RESULT_OK) {
				if (intent == null) {
					log("Return.....");
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
				log("resultCode for CHOOSE_PICTURE_PROFILE_CODE: "+resultCode);
			}
		}
		else if (requestCode == Constants.WRITE_SD_CARD_REQUEST_CODE && resultCode == RESULT_OK) {

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
				if (!hasStoragePermission) {
					ActivityCompat.requestPermissions(this,
			                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
							Constants.REQUEST_WRITE_STORAGE);
				}
			}

			Uri treeUri = intent.getData();
			log("--------------Create the document : "+treeUri);
			long handleToDownload = intent.getLongExtra("handleToDownload", -1);
			log("The recovered handle is: "+handleToDownload);
			//Now, call to the DownloadService

			if(handleToDownload!=0 && handleToDownload!=-1){
				Intent service = new Intent(this, DownloadService.class);
				service.putExtra(DownloadService.EXTRA_HASH, handleToDownload);
				service.putExtra(DownloadService.EXTRA_CONTENT_URI, treeUri.toString());
				String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.advancesDevicesDIR + "/";
				File tempDownDirectory = new File(path);
				if(!tempDownDirectory.exists()){
					tempDownDirectory.mkdirs();
				}
				service.putExtra(DownloadService.EXTRA_PATH, path);
				startService(service);
			}
		}
		else if (requestCode == Constants.REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {
			log("requestCode == REQUEST_CODE_SELECT_FILE");
			if (intent == null) {
				log("Return.....");
				return;
			}

			final ArrayList<String> selectedContacts = intent.getStringArrayListExtra("SELECTED_CONTACTS");
			final long fileHandle = intent.getLongExtra("SELECT", 0);

			nC.sendToInbox(fileHandle, selectedContacts);
		}
		else if (requestCode == Constants.REQUEST_CODE_SELECT_FOLDER && resultCode == RESULT_OK) {
			log("REQUEST_CODE_SELECT_FOLDER");

			if (intent == null) {
				log("Return.....");
				return;
			}

			final ArrayList<String> selectedContacts = intent.getStringArrayListExtra("SELECTED_CONTACTS");
			final long folderHandle = intent.getLongExtra("SELECT", 0);

			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
			dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
			final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
			dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {

					permissionsDialog.dismiss();
					switch(item) {
						case 0:{
							nC.shareFolder(folderHandle, selectedContacts, MegaShare.ACCESS_READ);
							break;
						}
						case 1:{
							nC.shareFolder(folderHandle, selectedContacts, MegaShare.ACCESS_READWRITE);
							break;
						}
						case 2:{
							nC.shareFolder(folderHandle, selectedContacts, MegaShare.ACCESS_FULL);
							break;
						}
					}
				}
			});
			dialogBuilder.setTitle(getString(R.string.dialog_select_permissions));
			permissionsDialog = dialogBuilder.create();
			permissionsDialog.show();

		}
		else if (requestCode == Constants.REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK){
			log("onActivityResult REQUEST_CODE_SELECT_CONTACT OK");

			if (intent == null) {
				log("Return.....");
				return;
			}

			final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);
			megaContacts = intent.getBooleanExtra(AddContactActivityLollipop.EXTRA_MEGA_CONTACTS, true);

			final int multiselectIntent = intent.getIntExtra("MULTISELECT", -1);
			final int sentToInbox = intent.getIntExtra("SEND_FILE", -1);

			if (megaContacts){

				if(sentToInbox==0){

					if(multiselectIntent==0){
						//One file to share
						final long nodeHandle = intent.getLongExtra(ContactsExplorerActivityLollipop.EXTRA_NODE_HANDLE, -1);

						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
						dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
						final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
						dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {

							permissionsDialog.dismiss();

							switch(item) {
								case 0:{
									nC.shareFolder(nodeHandle, contactsData, MegaShare.ACCESS_READ);
									break;
								}
								case 1:{
									nC.shareFolder(nodeHandle, contactsData, MegaShare.ACCESS_READWRITE);
									break;
								}
								case 2:{
									nC.shareFolder(nodeHandle, contactsData, MegaShare.ACCESS_FULL);
									break;
								}
							}
							}
						});
						dialogBuilder.setTitle(getString(R.string.dialog_select_permissions));
						permissionsDialog = dialogBuilder.create();
						permissionsDialog.show();
					}
					else if(multiselectIntent==1){
						//Several folders to share
						final long[] nodeHandles = intent.getLongArrayExtra(ContactsExplorerActivityLollipop.EXTRA_NODE_HANDLE);

						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
						dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
						final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
						dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {

								permissionsDialog.dismiss();
								switch(item) {
				                    case 0:{
				                    	log("ACCESS_READ");
										nC.shareFolders(nodeHandles, contactsData, MegaShare.ACCESS_READ);
				                    	break;
				                    }
				                    case 1:{
				                    	log("ACCESS_READWRITE");
										nC.shareFolders(nodeHandles, contactsData, MegaShare.ACCESS_READWRITE);
				                        break;
				                    }
				                    case 2:{
				                    	log("ACCESS_FULL");
										nC.shareFolders(nodeHandles, contactsData, MegaShare.ACCESS_FULL);

				                        break;
									}
								}
							}
						});
						dialogBuilder.setTitle(getString(R.string.dialog_select_permissions));
						permissionsDialog = dialogBuilder.create();
						permissionsDialog.show();
					}

				}
				else if (sentToInbox==1){
					if(multiselectIntent==0){
						//Send one file to one contact
						final long nodeHandle = intent.getLongExtra(ContactsExplorerActivityLollipop.EXTRA_NODE_HANDLE, -1);
						nC.sendToInbox(nodeHandle, contactsData);
					}
					else{
						//Send multiple files to one contact
						final long[] nodeHandles = intent.getLongArrayExtra(ContactsExplorerActivityLollipop.EXTRA_NODE_HANDLE);
						nC.sendToInbox(nodeHandles, contactsData);
					}
				}
			}
			else{

				for (int i=0; i < contactsData.size();i++){
					String type = contactsData.get(i);
					if (type.compareTo(ContactsExplorerActivityLollipop.EXTRA_EMAIL) == 0){
						i++;
						Snackbar.make(fragmentContainer, getString(R.string.general_not_yet_implemented), Snackbar.LENGTH_LONG).show();
//						Toast.makeText(this, "Sharing a folder: An email will be sent to the email address: " + contactsData.get(i) + ".\n", Toast.LENGTH_LONG).show();
					}
					else if (type.compareTo(ContactsExplorerActivityLollipop.EXTRA_PHONE) == 0){
						i++;
						Snackbar.make(fragmentContainer, getString(R.string.general_not_yet_implemented), Snackbar.LENGTH_LONG).show();
//						Toast.makeText(this, "Sharing a folder: A Text Message will be sent to the phone number: " + contactsData.get(i) , Toast.LENGTH_LONG).show();
					}
				}

			}
		}
		else if (requestCode == Constants.REQUEST_CODE_GET_LOCAL && resultCode == RESULT_OK) {

			if (intent == null) {
				log("Return.....");
				return;
			}

			String folderPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
			ArrayList<String> paths = intent.getStringArrayListExtra(FileStorageActivityLollipop.EXTRA_FILES);

			int i = 0;
			long parentHandleUpload=-1;
			if (drawerItem == DrawerItem.CLOUD_DRIVE){
				if(fbFLol!=null)
				{
					parentHandleUpload = fbFLol.getParentHandle();
				}
			}
			else if(drawerItem == DrawerItem.SHARED_ITEMS){
				int index = viewPagerShares.getCurrentItem();
				if(index==0){
					//INCOMING
					String cFTag1 = getFragmentTag(R.id.shares_tabs_pager, 0);
//					log("Tag: "+ cFTag1);
					inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag1);
					if (inSFLol != null){
						parentHandleUpload=inSFLol.getParentHandle();
					}
				}
				else if(index==1){
					//OUTGOING
					String cFTag1 = getFragmentTag(R.id.shares_tabs_pager, 1);
//					log("Tag: "+ cFTag1);
					outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag1);
					if (outSFLol != null){
						parentHandleUpload=outSFLol.getParentHandle();
					}
				}
			}
			else{
				return;
			}

			UploadServiceTask uploadServiceTask = new UploadServiceTask(folderPath, paths, parentHandleUpload);
			uploadServiceTask.start();
		}
		else if (requestCode == Constants.REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK) {

			if (intent == null) {
				log("Return.....");
				return;
			}

			moveToRubbish = false;

			final long[] moveHandles = intent.getLongArrayExtra("MOVE_HANDLES");
			final long toHandle = intent.getLongExtra("MOVE_TO", 0);

			nC.moveNodes(moveHandles, toHandle);

		}
		else if (requestCode ==  Constants.REQUEST_CODE_SELECT_COPY_FOLDER && resultCode == RESULT_OK){
			log("onActivityResult: REQUEST_CODE_SELECT_COPY_FOLDER");
			if (intent == null) {
				log("Return.....");
				return;
			}
			final long[] copyHandles = intent.getLongArrayExtra("COPY_HANDLES");
			final long toHandle = intent.getLongExtra("COPY_TO", 0);

			nC.copyNodes(copyHandles, toHandle);
		}
		else if (requestCode == Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
			log("onActivityResult: REQUEST_CODE_SELECT_LOCAL_FOLDER");
			if (intent == null) {
				log("Return.....");
				return;
			}

			String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
			log("parentPath: "+parentPath);
			String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
			log("url: "+url);
			long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
			log("size: "+size);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
			log("hashes size: "+hashes.length);

			nC.checkSizeBeforeDownload(parentPath, url, size, hashes);
			Snackbar.make(fragmentContainer, getString(R.string.download_began), Snackbar.LENGTH_LONG).show();
		}
		else if (requestCode == Constants.REQUEST_CODE_REFRESH && resultCode == RESULT_OK) {
			log("Resfresh DONE onActivityResult");

			if (intent == null) {
				log("Return.....");
				return;
			}

			if(myAccountInfo==null){
				myAccountInfo = new MyAccountInfo(this);
			}

			megaApi.getExtendedAccountDetails(true, false, false, myAccountInfo);
			megaApi.getPaymentMethods(myAccountInfo);
			megaApi.getPricing(myAccountInfo);

			if (drawerItem == DrawerItem.CLOUD_DRIVE){
				parentHandleBrowser = intent.getLongExtra("PARENT_HANDLE", -1);
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
				if (parentNode != null){
					if (fbFLol != null){
						ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderCloud);
						fbFLol.setNodes(nodes);
						fbFLol.getRecyclerView().invalidate();
					}
				}
				else{
					if (fbFLol != null){
						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRootNode(), orderCloud);
						fbFLol.setNodes(nodes);
						fbFLol.getRecyclerView().invalidate();
					}
				}
			}
//			else if (drawerItem == DrawerItem.RUBBISH_BIN){
//				parentHandleRubbish = intent.getLongExtra("PARENT_HANDLE", -1);
//				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleRubbish);
//				if (parentNode != null){
//					if (rbFLol != null){
//						ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderGetChildren);
//						rbFLol.setNodes(nodes);
//						rbFLol.getListView().invalidateViews();
//					}
//				}
//				else{
//					if (rbFLol != null){
//						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderGetChildren);
//						rbFLol.setNodes(nodes);
//						rbFLol.getListView().invalidateViews();
//					}
//				}
//			}
			else if (drawerItem == DrawerItem.SHARED_ITEMS){
				parentHandleIncoming = intent.getLongExtra("PARENT_HANDLE", -1);
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleIncoming);
				if (parentNode != null){
					if (inSFLol != null){
//						ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderGetChildren);
						//TODO: ojo con los hijos
//							inSFLol.setNodes(nodes);
						inSFLol.getRecyclerView().invalidate();
					}
				}
				else{
					if (inSFLol != null){
//						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getInboxNode(), orderGetChildren);
						//TODO: ojo con los hijos
//							inSFLol.setNodes(nodes);
						inSFLol.getRecyclerView().invalidate();
					}
				}
			}
		}
		else if (requestCode == Constants.TAKE_PHOTO_CODE){
			log("TAKE_PHOTO_CODE");
			if(resultCode == Activity.RESULT_OK){
				Intent intentPicture = new Intent(this, SecureSelfiePreviewActivityLollipop.class);
				startActivity(intentPicture);
			}
			else{
				log("TAKE_PHOTO_CODE--->ERROR!");
			}

	    }
		else if (requestCode == Constants.TAKE_PICTURE_PROFILE_CODE){
			log("TAKE_PICTURE_PROFILE_CODE");
			if(resultCode == Activity.RESULT_OK){
				Intent intentPicture = new Intent(this, SecureSelfiePreviewActivityLollipop.class);
				intentPicture.putExtra("PICTURE_PROFILE", 1);
				intentPicture.putExtra("MY_MAIL", myAccountInfo.getMyUser().getEmail());
				startActivity(intentPicture);
			}
			else{
				log("TAKE_PICTURE_PROFILE_CODE--->ERROR!");
			}

		}
		else if (requestCode == Constants.REQUEST_CODE_SORT_BY && resultCode == RESULT_OK){

			if (intent == null) {
				log("Return.....");
				return;
			}

			int orderGetChildren = intent.getIntExtra("ORDER_GET_CHILDREN", 1);
			if (drawerItem == DrawerItem.CLOUD_DRIVE){
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
				if (parentNode != null){
					if (fbFLol != null){
						ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderGetChildren);
						fbFLol.setOrder(orderGetChildren);
						fbFLol.setNodes(nodes);
						fbFLol.getRecyclerView().invalidate();
					}
				}
				else{
					if (fbFLol != null){
						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRootNode(), orderGetChildren);
						fbFLol.setOrder(orderGetChildren);
						fbFLol.setNodes(nodes);
						fbFLol.getRecyclerView().invalidate();
					}
				}
			}
//			else if (drawerItem == DrawerItem.RUBBISH_BIN){
//				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleRubbish);
//				if (parentNode != null){
//					if (rbFLol != null){
//						ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderGetChildren);
//						rbFLol.setOrder(orderGetChildren);
//						rbFLol.setNodes(nodes);
//						rbFLol.getListView().invalidateViews();
//					}
//				}
//				else{
//					if (rbFLol != null){
//						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderGetChildren);
//						rbFLol.setOrder(orderGetChildren);
//						rbFLol.setNodes(nodes);
//						rbFLol.getListView().invalidateViews();
//					}
//				}
//			}
			else if (drawerItem == DrawerItem.SHARED_ITEMS){
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleIncoming);
				if (parentNode != null){
					if (inSFLol != null){
						ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderGetChildren);
						inSFLol.setOrder(orderGetChildren);
						//TODO: ojo con los hijos
//							inSFLol.setNodes(nodes);
						inSFLol.getRecyclerView().invalidate();
					}
				}
				else{
					if (inSFLol != null){
//						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getInboxNode(), orderGetChildren);
						inSFLol.setOrder(orderGetChildren);
						//TODO: ojo con los hijos
//							inSFLol.setNodes(nodes);
						inSFLol.getRecyclerView().invalidate();
					}
				}
			}
		}
		else if (requestCode == Constants.REQUEST_CREATE_CHAT && resultCode == RESULT_OK) {
			log("onActivityResult REQUEST_CREATE_CHAT OK");

			if (intent == null) {
				log("Return.....");
				return;
			}

			final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);

			if (contactsData != null){

				MegaChatPeerList peers = MegaChatPeerList.createInstance();
				if(contactsData.size()==1){
					MegaUser user = megaApi.getContact(contactsData.get(0));
					if(user!=null){
						log("Chat with contact: "+contactsData.size());
						MegaChatRoom chat = megaChatApi.getChatRoomByUser(user.getHandle());
						if(chat==null){
							log("No chat, create it!");
							peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
							megaChatApi.createChat(false, peers, this);
						}
						else{
							log("There is already a chat, open it!");
							Intent intentOpenChat = new Intent(this, ChatActivityLollipop.class);
							intentOpenChat.setAction(Constants.ACTION_CHAT_SHOW_MESSAGES);
							intentOpenChat.putExtra("CHAT_ID", chat.getChatId());
							this.startActivity(intentOpenChat);
						}
					}
				}
				else{
					for (int i=0; i<contactsData.size(); i++){
						MegaUser user = megaApi.getContact(contactsData.get(i));
						if(user!=null){
							peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
						}
					}
					log("create group chat with participants: "+peers.size());
					megaChatApi.createChat(true, peers, this);
				}
			}
		}
		else if (requestCode == Constants.REQUEST_INVITE_CONTACT_FROM_DEVICE && resultCode == RESULT_OK) {
			log("onActivityResult REQUEST_INVITE_CONTACT_FROM_DEVICE OK");

			if (intent == null) {
				log("Return.....");
				return;
			}

			final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);
			megaContacts = intent.getBooleanExtra(AddContactActivityLollipop.EXTRA_MEGA_CONTACTS, true);

			if (contactsData != null){
				cC.inviteMultipleContacts(contactsData);
			}
		}
		else if (requestCode == RC_REQUEST){
			// Pass on the activity result to the helper for handling
	        if (!mHelper.handleActivityResult(requestCode, resultCode, intent)) {
	            // not handled, so handle it ourselves (here's where you'd
	            // perform any handling of activity results not related to in-app
	            // billing...

	        	super.onActivityResult(requestCode, resultCode, intent);
	        }
	        else {
	            log("onActivityResult handled by IABUtil.");
	            drawerItem = DrawerItem.ACCOUNT;
//	            Toast.makeText(this, "HURRAY!: ORDERID: **__" + orderId + "__**", Toast.LENGTH_LONG).show();
	            log("HURRAY!: ORDERID: **__" + orderId + "__**");
	        }
		}
		else{
			log("No requestcode");
			super.onActivityResult(requestCode, resultCode, intent);
		}
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

			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if (parentNode == null){
				parentNode = megaApi.getRootNode();
			}

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
					log("EXTRA_FILE_PATH_dir:" + file.getAbsolutePath());
				}
				else{
					ShareInfo info = ShareInfo.infoFromFile(file);
					if (info == null){
						continue;
					}
					uploadServiceIntent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
					uploadServiceIntent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
					uploadServiceIntent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
					log("EXTRA_FILE_PATH_file:" + info.getFileAbsolutePath());
				}

				log("EXTRA_FOLDER_PATH:" + folderPath);
				uploadServiceIntent.putExtra(UploadService.EXTRA_FOLDERPATH, folderPath);
				uploadServiceIntent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
				startService(uploadServiceIntent);
			}
		}

	}

	void disableNavigationViewMenu(Menu menu){
		MenuItem mi = menu.findItem(R.id.navigation_item_cloud_drive);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.cloud_drive_grey));
			mi.setChecked(false);
			mi.setEnabled(false);
		}
		mi = menu.findItem(R.id.navigation_item_saved_for_offline);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.saved_for_offline_grey));
			mi.setChecked(false);
		}
		mi = menu.findItem(R.id.navigation_item_camera_uploads);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.camera_uploads_grey));
			mi.setChecked(false);
			mi.setEnabled(false);
		}
		mi = menu.findItem(R.id.navigation_item_inbox);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.inbox_grey));
			mi.setChecked(false);
			mi.setEnabled(false);
		}
		mi = menu.findItem(R.id.navigation_item_shared_items);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.shared_items_grey));
			mi.setChecked(false);
			mi.setEnabled(false);
		}
		mi = menu.findItem(R.id.navigation_item_chat);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.ic_menu_chat));
			mi.setChecked(false);
		}
		mi = menu.findItem(R.id.navigation_item_contacts);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.contacts_grey));
			mi.setChecked(false);
			mi.setEnabled(false);
		}
		mi = menu.findItem(R.id.navigation_item_settings);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.settings_grey));
			mi.setChecked(false);
		}
	}

	void resetNavigationViewMenu(Menu menu){
		MenuItem mi = menu.findItem(R.id.navigation_item_cloud_drive);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.cloud_drive_grey));
			mi.setChecked(false);
		}
		mi = menu.findItem(R.id.navigation_item_saved_for_offline);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.saved_for_offline_grey));
			mi.setChecked(false);
		}
		mi = menu.findItem(R.id.navigation_item_camera_uploads);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.camera_uploads_grey));
			mi.setChecked(false);
		}
		mi = menu.findItem(R.id.navigation_item_inbox);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.inbox_grey));
			mi.setChecked(false);
		}
		mi = menu.findItem(R.id.navigation_item_shared_items);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.shared_items_grey));
			mi.setChecked(false);
		}
		mi = menu.findItem(R.id.navigation_item_chat);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.ic_menu_chat));
			mi.setChecked(false);
		}
		mi = menu.findItem(R.id.navigation_item_contacts);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.contacts_grey));
			mi.setChecked(false);
		}
		mi = menu.findItem(R.id.navigation_item_settings);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.settings_grey));
			mi.setChecked(false);
		}
	}

	public void showProPanel(){
		log("showProPanel");
		//Left and Right margin
		LinearLayout.LayoutParams proTextParams = (LinearLayout.LayoutParams)getProText.getLayoutParams();
		proTextParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(23, outMetrics), Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(23, outMetrics));
		getProText.setLayoutParams(proTextParams);

		rightUpgradeButton.setOnClickListener(this);
		android.view.ViewGroup.LayoutParams paramsb2 = rightUpgradeButton.getLayoutParams();
		//Left and Right margin
		LinearLayout.LayoutParams optionTextParams = (LinearLayout.LayoutParams)rightUpgradeButton.getLayoutParams();
		optionTextParams.setMargins(Util.scaleWidthPx(6, outMetrics), 0, Util.scaleWidthPx(8, outMetrics), 0);
		rightUpgradeButton.setLayoutParams(optionTextParams);

		leftCancelButton.setOnClickListener(this);
		android.view.ViewGroup.LayoutParams paramsb1 = leftCancelButton.getLayoutParams();
		leftCancelButton.setLayoutParams(paramsb1);
		//Left and Right margin
		LinearLayout.LayoutParams cancelTextParams = (LinearLayout.LayoutParams)leftCancelButton.getLayoutParams();
		cancelTextParams.setMargins(Util.scaleWidthPx(6, outMetrics), 0, Util.scaleWidthPx(6, outMetrics), 0);
		leftCancelButton.setLayoutParams(cancelTextParams);

		getProLayout.setVisibility(View.VISIBLE);
		getProLayout.bringToFront();
	}

	public void showOverquotaPanel(){
		log("showOverquotaAlert");

		//Left and Right margin
		LinearLayout.LayoutParams proTextParams = (LinearLayout.LayoutParams)outSpaceTextFirst.getLayoutParams();
		proTextParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(16, outMetrics), Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(0, outMetrics));
		outSpaceTextFirst.setLayoutParams(proTextParams);

		//Left and Right margin
		LinearLayout.LayoutParams proTextParams2 = (LinearLayout.LayoutParams)outSpaceTextSecond.getLayoutParams();
		proTextParams2.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(0, outMetrics), Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(23, outMetrics));
		outSpaceTextSecond.setLayoutParams(proTextParams2);

		outSpaceButtonUpgrade.setOnClickListener(this);
		android.view.ViewGroup.LayoutParams paramsb2 = outSpaceButtonUpgrade.getLayoutParams();
		//Left and Right margin
		LinearLayout.LayoutParams optionTextParams = (LinearLayout.LayoutParams)outSpaceButtonUpgrade.getLayoutParams();
		optionTextParams.setMargins(Util.scaleWidthPx(6, outMetrics), 0, Util.scaleWidthPx(8, outMetrics), 0);
		outSpaceButtonUpgrade.setLayoutParams(optionTextParams);

		outSpaceButtonCancel.setOnClickListener(this);
		android.view.ViewGroup.LayoutParams paramsb1 = outSpaceButtonCancel.getLayoutParams();
		outSpaceButtonCancel.setLayoutParams(paramsb1);
		//Left and Right margin
		LinearLayout.LayoutParams cancelTextParams = (LinearLayout.LayoutParams)outSpaceButtonCancel.getLayoutParams();
		cancelTextParams.setMargins(Util.scaleWidthPx(6, outMetrics), 0, Util.scaleWidthPx(6, outMetrics), 0);
		outSpaceButtonCancel.setLayoutParams(cancelTextParams);

//		outSpaceButton.setOnClickListener(this);
//		android.view.ViewGroup.LayoutParams paramsb2 = outSpaceButton.getLayoutParams();
//		paramsb2.height = Util.scaleHeightPx(48, outMetrics);
//		outSpaceButton.setText(getString(R.string.my_account_upgrade_pro).toUpperCase(Locale.getDefault()));
////		paramsb2.width = Util.scaleWidthPx(73, outMetrics);
//		//Left and Right margin
//		LinearLayout.LayoutParams optionTextParams = (LinearLayout.LayoutParams)outSpaceButton.getLayoutParams();
//		optionTextParams.setMargins(Util.scaleWidthPx(6, outMetrics), 0, Util.scaleWidthPx(20, outMetrics), 0);
//		outSpaceButton.setLayoutParams(optionTextParams);

		outSpaceLayout.setVisibility(View.VISIBLE);
		outSpaceLayout.bringToFront();

		Runnable r = new Runnable() {

			@Override
			public void run() {
				log("BUTTON DISAPPEAR");
				log("altura: "+outSpaceLayout.getHeight());

				TranslateAnimation animTop = new TranslateAnimation(0, 0, 0, outSpaceLayout.getHeight());
				animTop.setDuration(4000);
				animTop.setFillAfter(true);
				outSpaceLayout.setAnimation(animTop);

				outSpaceLayout.setVisibility(View.GONE);
				outSpaceLayout.invalidate();
//				RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
//				p.addRule(RelativeLayout.ABOVE, R.id.buttons_layout);
//				listView.setLayoutParams(p);
			}
		};

		Handler handler = new Handler();
		handler.postDelayed(r,10000);
	}

	public void updateCancelSubscriptions(){
		log("updateCancelSubscriptions");
		if (cancelSubscription != null){
			cancelSubscription.setVisible(false);
		}
		if (myAccountInfo.getNumberOfSubscriptions() > 0){
			if (cancelSubscription != null){
				if (drawerItem == DrawerItem.ACCOUNT){
					if (maFLol != null){
						cancelSubscription.setVisible(true);
					}
				}
			}
		}
	}

	public void updateOfflineView(MegaOffline mOff){
		log("updateOfflineView");
		if(oFLol!=null){
			if(mOff==null){
				oFLol.refresh();
			}
			else{
				oFLol.refreshPaths(mOff);
			}
		}
	}

	public void updateContactsView(boolean contacts, boolean sentRequests, boolean receivedRequests){
		log("updateContactsView");

		if(contacts){
			log("Update Contacts Fragment");
			String cFTag = getFragmentTag(R.id.contact_tabs_pager, 0);
			cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
			if (cFLol != null){
				if (drawerItem == DrawerItem.CONTACTS){
					cFLol.updateView();
				}
			}
		}

		if(sentRequests){
			log("Update SentRequests Fragment");
			String cFTagSR = getFragmentTag(R.id.contact_tabs_pager, 1);
			sRFLol = (SentRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTagSR);
			if (sRFLol != null){
				if (drawerItem == DrawerItem.CONTACTS){
					sRFLol.updateView();
				}
			}
		}

		if(receivedRequests){
			log("Update ReceivedRequest Fragment");
			String cFTagRR = getFragmentTag(R.id.contact_tabs_pager, 2);
			rRFLol = (ReceivedRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTagRR);
			if (rRFLol != null){
				if (drawerItem == DrawerItem.CONTACTS){
					rRFLol.updateView();
				}
			}
		}
	}

	/*
	 * Handle processed upload intent
	 */
	public void onIntentProcessed(List<ShareInfo> infos) {
		log("onIntentProcessedLollipop");
//		List<ShareInfo> infos = filePreparedInfos;
		if (statusDialog != null) {
			try {
				statusDialog.dismiss();
			}
			catch(Exception ex){}
		}

		long parentHandle = -1;
		MegaNode parentNode = null;
		if (drawerItem == DrawerItem.CLOUD_DRIVE){
			parentHandle = fbFLol.getParentHandle();
			parentNode = megaApi.getNodeByHandle(parentHandle);
		}
		else if (drawerItem == DrawerItem.SHARED_ITEMS){
			int index = viewPagerShares.getCurrentItem();
			if(index==1){
				//OUTGOING
				String cFTag2 = getFragmentTag(R.id.shares_tabs_pager, 1);
				log("Tag: "+ cFTag2);
				outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag2);
				if (outSFLol != null){
					parentHandleOutgoing = outSFLol.getParentHandle();
					parentNode = megaApi.getNodeByHandle(parentHandleOutgoing);
				}
			}
			else{
				//InCOMING
				String cFTag1 = getFragmentTag(R.id.shares_tabs_pager, 0);
				log("Tag: "+ cFTag1);
				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag1);
				if (inSFLol != null){
					parentHandleIncoming = inSFLol.getParentHandle();
					parentNode = megaApi.getNodeByHandle(parentHandleIncoming);
				}
			}
		}
		else if(drawerItem == DrawerItem.ACCOUNT){
			if(infos!=null){
				for (ShareInfo info : infos) {
					String avatarPath = info.getFileAbsolutePath();
					if(avatarPath!=null){
						log("Chosen picture to change the avatar: "+avatarPath);
						File imgFile = new File(avatarPath);
//						String name = Util.getPhotoSyncName(imgFile.lastModified(), imgFile.getAbsolutePath());
						String newPath = null;
						if (getExternalCacheDir() != null){
							newPath = getExternalCacheDir().getAbsolutePath() + "/" + myAccountInfo.getMyUser().getEmail() + "Temp.jpg";
						}
						else{
							log("getExternalCacheDir() is NULL");
							newPath = getCacheDir().getAbsolutePath() + "/" + myAccountInfo.getMyUser().getEmail() + "Temp.jpg";
						}

						if(newPath!=null){
							File newFile = new File(newPath);
							log("NEW - the destination of the avatar is: "+newPath);
							if(newFile!=null){
								MegaUtilsAndroid.createAvatar(imgFile, newFile);
								String myAccountTag = getFragmentTag(R.id.my_account_tabs_pager, 0);
								maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(myAccountTag);
								if(maFLol!=null){
									megaApi.setAvatar(newFile.getAbsolutePath(), this);
								}

							}
							else{
								log("Error new path avatar!!");
							}
						}
						else{
							log("ERROR! Destination PATH is NULL");
						}


//						String newPath = Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+ Util.profilePicDIR + "/"+name;
//						log("----NEW Name: "+newPath);
//						File newFile = new File(newPath);
//						MegaUtilsAndroid.createAvatar(imgFile, newFile);

					}
					else{
						log("The chosen avatar path is NULL");
					}
				}
			}
			else{
				log("infos is NULL");
			}
			return;
		}

		if(parentNode == null){
			Snackbar.make(fragmentContainer, getString(R.string.error_temporary_unavaible), Snackbar.LENGTH_LONG).show();
			return;
		}

		if (infos == null) {
			Snackbar.make(fragmentContainer, getString(R.string.upload_can_not_open), Snackbar.LENGTH_LONG).show();
		}
		else {
			Snackbar.make(fragmentContainer, getString(R.string.upload_began), Snackbar.LENGTH_LONG).show();
			for (ShareInfo info : infos) {
				Intent intent = new Intent(this, UploadService.class);
				intent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
				intent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
				intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
				intent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
				startService(intent);
			}
		}
	}


	@Override
	public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
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
		log("onRequestFinish(CHAT)");

		if(request.getType() == MegaChatRequest.TYPE_TRUNCATE_HISTORY){
			log("Truncate history request finisf!!!");
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				showSnackbar(getString(R.string.clear_history_success));
			}
			else{
				showSnackbar(getString(R.string.clear_history_error));
				log("Error clearing history: "+e.getErrorString());
			}
		}
		else if(request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM){
			log("Create chat request finish!!!");
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				log("Chat CREATEDD!!!");

				//Update chat view
				if(rChatFL!=null){
//					rChatFL.setChats();
				}

				log("open new chat");
				Intent intent = new Intent(this, ChatActivityLollipop.class);
				intent.setAction(Constants.ACTION_CHAT_NEW);
				intent.putExtra("CHAT_ID", request.getChatHandle());
				this.startActivity(intent);

//				log("open new chat");
//				Intent intent = new Intent(this, ChatActivityLollipop.class);
//				intent.setAction(Constants.ACTION_CHAT_NEW);
//				String myMail = getMyAccountInfo().getMyUser().getEmail();
//				intent.putExtra("CHAT_ID", request.getChatHandle());
//				intent.putExtra("MY_MAIL", myMail);
//
//				boolean isGroup = request.getFlag();
//				if(isGroup){
//					log("GROUP");
//					MegaChatPeerList list = request.getMegaChatPeerList();
//					log("Size: "+list.size());
//
//				}
//				else{
//					log("NOT group");
//				}
//
//				this.startActivity(intent);
			}
			else{
				log("EEEERRRRROR WHEN CREATING CHAT " + e.getErrorString());
				showSnackbar(getString(R.string.create_chat_error));
			}
		}
		else if(request.getType() == MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM){
			log("remove from chat finish!!!");
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				//Update chat view
//				if(rChatFL!=null){
//					rChatFL.setChats();
//				}
			}
			else{
				log("EEEERRRRROR WHEN leaving CHAT " + e.getErrorString());
				showSnackbar(getString(R.string.leave_chat_error));
			}
		}
		else if (request.getType() == MegaChatRequest.TYPE_CONNECT){
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				log("Connected to chat!: statusToConnect: "+statusToConnect);
				megaChatApi.setOnlineStatus(statusToConnect, this);
			}
			else{
				log("EEEERRRRROR WHEN CONNECTING " + e.getErrorString());
//				showSnackbar(getString(R.string.chat_connection_error));
			}
		}
		else if (request.getType() == MegaChatRequest.TYPE_DISCONNECT){
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				log("DISConnected from chat!");
			}
			else{
				log("EEEERRRRROR WHEN DISCONNECTING " + e.getErrorString());
			}
		}
		else if (request.getType() == MegaChatRequest.TYPE_LOGOUT){
//            loginLoggingIn.setVisibility(View.GONE);
//            loginLogin.setVisibility(View.VISIBLE);
//            scrollView.setBackgroundColor(getResources().getColor(R.color.background_create_account));
//            loginDelimiter.setVisibility(View.VISIBLE);
//            loginCreateAccount.setVisibility(View.VISIBLE);
//            queryingSignupLinkText.setVisibility(View.GONE);
//            confirmingAccountText.setVisibility(View.GONE);
//            generatingKeysText.setVisibility(View.GONE);
//            loggingInText.setVisibility(View.GONE);
//            fetchingNodesText.setVisibility(View.GONE);
//            prepareNodesText.setVisibility(View.GONE);
//            initizalizingChatText.setVisibility(View.GONE);
//            serversBusyText.setVisibility(View.GONE);

			megaChatApi = null;
			if (app != null){
				app.disableMegaChatApi();
			}
			Util.resetAndroidLogger();
		}
		else if(request.getType() == MegaChatRequest.TYPE_SET_ONLINE_STATUS){
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				log("Status changed to: "+request.getNumber());
				int status = (int) request.getNumber();
				switch(status){
					case MegaChatApi.STATUS_ONLINE:{
						showSnackbar(getString(R.string.changing_status_to_online_success));
						dbH.setStatusChat(MegaChatApi.STATUS_ONLINE+"");
						break;
					}
					case MegaChatApi.STATUS_AWAY:{
						showSnackbar(getString(R.string.changing_status_to_invisible_success));
						dbH.setStatusChat(MegaChatApi.STATUS_AWAY+"");
						break;
					}
					case MegaChatApi.STATUS_OFFLINE:{
						showSnackbar(getString(R.string.changing_status_to_offline_success));
						dbH.setStatusChat(MegaChatApi.STATUS_OFFLINE+"");
						megaChatApi.disconnect(this);
						break;
					}
				}
				if(sttFLol!=null){
					if(sttFLol.isAdded()){
						sttFLol.verifyStatusChat(status);
					}
				}
			}
			else{
				log("EEEERRRRROR WHEN TYPE_SET_ONLINE_STATUS " + e.getErrorString());
				showSnackbar(getString(R.string.changing_status_error));
			}
		}
		else if(request.getType() == MegaChatRequest.TYPE_LOGOUT){
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				log("Logout from chat");
				megaChatApi = null;
				((MegaApplication) getApplication()).disableMegaChatApi();
				Util.resetAndroidLogger();
			}
			else{
				log("ERROR logout CHAT " + e.getErrorString());
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate: " + request.getRequestString());
	}

	@SuppressLint("NewApi") @Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		log("onRequestFinish: " + request.getRequestString());

		if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			log("fecthnodes request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_CREDIT_CARD_CANCEL_SUBSCRIPTIONS){
			if (e.getErrorCode() == MegaError.API_OK){
				Snackbar.make(fragmentContainer, getString(R.string.cancel_subscription_ok), Snackbar.LENGTH_LONG).show();
			}
			else{
				Snackbar.make(fragmentContainer, getString(R.string.cancel_subscription_error), Snackbar.LENGTH_LONG).show();
			}
			megaApi.creditCardQuerySubscriptions(myAccountInfo);
		}
		else if (request.getType() == MegaRequest.TYPE_LOGOUT){
			log("logout finished");

			if(megaChatApi!=null){
				megaChatApi.logout(null);
			}

			Intent tourIntent = new Intent(this, LoginActivityLollipop.class);
			startActivity(tourIntent);
			finish();

//			if (recentChatsFragmentLollipopListener != null){
//				log("remove chatlistener");
//				megaChatApi.removeChatListener(recentChatsFragmentLollipopListener);
//			}
//
//			megaChatApi.logout(this);

//			if (request.getType() == MegaRequest.TYPE_LOGOUT){
//				log("type_logout");
//				if (e.getErrorCode() == MegaError.API_ESID){
//					log("calling ManagerActivityLollipop.logout");
//					MegaApiAndroid megaApi = app.getMegaApi();
//					ManagerActivityLollipop.logout(managerActivity, app, megaApi, false);
//				}
//			}
		}
		else if(request.getType() == MegaRequest.TYPE_SET_ATTR_USER) {
			log("TYPE_SET_ATTR_USER");
			if(request.getParamType()==MegaApiJava.USER_ATTR_FIRSTNAME){
				log("(1)request.getText(): "+request.getText());
				countUserAttributes--;
				myAccountInfo.setFirstNameText(request.getText());
				if (e.getErrorCode() == MegaError.API_OK){
					log("The first name has changed");
					String myAccountTag = getFragmentTag(R.id.my_account_tabs_pager, 0);
					maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(myAccountTag);
					if(maFLol!=null){
						if(maFLol.isAdded()){
							maFLol.updateNameView(myAccountInfo.getFullName());
						}
					}
					updateUserNameNavigationView(myAccountInfo.getFullName(), myAccountInfo.getFirstLetter());
				}
				else{
					log("Error with first name");
					errorUserAttibutes++;
				}
			}
			else if(request.getParamType()==MegaApiJava.USER_ATTR_LASTNAME){
				log("(2)request.getText(): "+request.getText());
				countUserAttributes--;
				myAccountInfo.setLastNameText(request.getText());
				if (e.getErrorCode() == MegaError.API_OK){
					log("The last name has changed");
					String myAccountTag = getFragmentTag(R.id.my_account_tabs_pager, 0);
					maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(myAccountTag);
					if(maFLol!=null){
						if(maFLol.isAdded()){
							maFLol.updateNameView(myAccountInfo.getFullName());
						}
					}
					updateUserNameNavigationView(myAccountInfo.getFullName(), myAccountInfo.getFirstLetter());
				}
				else{
					log("Error with last name");
					errorUserAttibutes++;
				}
			}
			if (request.getParamType() == MegaApiJava.USER_ATTR_AVATAR) {

				if (e.getErrorCode() == MegaError.API_OK){
					log("Avatar changed!!");
					if(request.getFile()!=null){
						log("old path: "+request.getFile());
						File oldFile = new File(request.getFile());
						if(oldFile!=null){
							if(oldFile.exists()){
								String newPath = null;
								if (getExternalCacheDir() != null){
									newPath = getExternalCacheDir().getAbsolutePath() + "/" + myAccountInfo.getMyUser().getEmail() + ".jpg";
								}
								else{
									log("getExternalCacheDir() is NULL");
									newPath = getCacheDir().getAbsolutePath() + "/" + myAccountInfo.getMyUser().getEmail() + ".jpg";
								}
								File newFile = new File(newPath);
								oldFile.renameTo(newFile);
							}
						}
					}
					setProfileAvatar();

					String myAccountTag = getFragmentTag(R.id.my_account_tabs_pager, 0);
					maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(myAccountTag);
					if(maFLol!=null){
						if(maFLol.isAdded()){
							maFLol.updateAvatar(false);
						}
					}
				}
				else{
					log("Error when changing avatar: "+e.getErrorString()+" "+e.getErrorCode());
				}
			}

			if(countUserAttributes==0){
				if(errorUserAttibutes==0){
					log("All user attributes changed!");
					showSnackbar(getString(R.string.success_changing_user_attributes));
				}
				else{
					log("Some error ocurred when changing an attribute: "+errorUserAttibutes);
					showSnackbar(getString(R.string.error_changing_user_attributes));
				}
				AccountController aC = new AccountController(this);
				errorUserAttibutes=0;
				aC.setCount(0);
			}
		}
		if(request.getType() == MegaRequest.TYPE_GET_CHANGE_EMAIL_LINK) {
			log("TYPE_GET_CHANGE_EMAIL_LINK: "+request.getEmail());
			if (e.getErrorCode() == MegaError.API_OK){
				log("The change link has been sent");
				Util.showAlert(this, getString(R.string.email_verification_text_change_mail), getString(R.string.email_verification_title));
			}
			else if(e.getErrorCode() == MegaError.API_EEXIST){
				log("The new mail already exists");
				Util.showAlert(this, getString(R.string.mail_already_used), getString(R.string.email_verification_title));
			}
			else{
				log("Error when asking for change mail link");
				log(e.getErrorString() + "___" + e.getErrorCode());
				Util.showAlert(this, getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
			}
		}
		else if(request.getType() == MegaRequest.TYPE_QUERY_RECOVERY_LINK) {
			log("TYPE_GET_RECOVERY_LINK");
			if (e.getErrorCode() == MegaError.API_OK){
				String url = request.getLink();
				log("cancel account url");
				String myEmail = request.getEmail();
				if(myEmail!=null){
					if(myEmail.equals(myAccountInfo.getMyUser().getEmail())){
						log("The email matchs!!!");
						showDialogInsertPassword(url, true);
					}
					else{
						log("Not logged with the correct account");
						log(e.getErrorString() + "___" + e.getErrorCode());
						Util.showAlert(this, getString(R.string.error_not_logged_with_correct_account), getString(R.string.general_error_word));
					}
				}
				else{
					log("My email is NULL in the request");
				}
			}
			else if(e.getErrorCode() == MegaError.API_EEXPIRED){
				log("Error expired link");
				log(e.getErrorString() + "___" + e.getErrorCode());
				Util.showAlert(this, getString(R.string.cancel_link_expired), getString(R.string.general_error_word));
			}
			else{
				log("Error when asking for recovery pass link");
				log(e.getErrorString() + "___" + e.getErrorCode());
				Util.showAlert(this, getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
			}
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE_CONTACT){

			if (e.getErrorCode() == MegaError.API_OK){
				Snackbar.make(fragmentContainer, getString(R.string.context_contact_removed), Snackbar.LENGTH_LONG).show();
			}
			else{
				log("Error deleting contact");
				Snackbar.make(fragmentContainer, getString(R.string.context_contact_not_removed), Snackbar.LENGTH_LONG).show();
			}
			updateContactsView(true, false, false);
		}
		else if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT){
			log("MegaRequest.TYPE_INVITE_CONTACT finished: "+request.getNumber());

			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}


			if(request.getNumber()==MegaContactRequest.INVITE_ACTION_REMIND){
				showSnackbar(getString(R.string.context_contact_invitation_resent));
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					log("OK INVITE CONTACT: "+request.getEmail());
					if(request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD)
					{
						showSnackbar(getString(R.string.context_contact_request_sent, request.getEmail()));
					}
					else if(request.getNumber()==MegaContactRequest.INVITE_ACTION_DELETE)
					{
						showSnackbar(getString(R.string.context_contact_invitation_deleted));
					}
				}
				else{
					log("Code: "+e.getErrorString());
					if(e.getErrorCode()==MegaError.API_EEXIST)
					{
						showSnackbar(getString(R.string.context_contact_already_exists, request.getEmail()));
					}
					else{
						showSnackbar(getString(R.string.general_error));
					}
					log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_REPLY_CONTACT_REQUEST){
			log("MegaRequest.TYPE_REPLY_CONTACT_REQUEST finished: "+request.getType());

			if (e.getErrorCode() == MegaError.API_OK){
				showSnackbar(getString(R.string.context_invitacion_reply));
				if(request.getNumber()==MegaContactRequest.REPLY_ACTION_ACCEPT){
					log("I've accepted the invitation");

					MegaContactRequest contactRequest = megaApi.getContactRequestByHandle(request.getNodeHandle());
					log("Handle of the rquest: "+request.getNodeHandle());
					if(contactRequest!=null){
						log("Source: "+contactRequest.getSourceEmail());
						//Get the data of the user (avatar and name)
						MegaContact contactDB = dbH.findContactByEmail(contactRequest.getSourceEmail());
						if(contactDB==null){
							log("The contact: "+contactRequest.getSourceEmail()+" not found! Will be added to DB!");
							cC.addContactDB(contactRequest.getSourceEmail());
						}
						//Update view to get avatar
						String cFTag = getFragmentTag(R.id.contact_tabs_pager, 0);
						cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
						if (cFLol != null){
							cFLol.updateView();
						}
					}
					else{
						log("ContactRequest is NULL");
					}
				}
			}
			else{
				showSnackbar(getString(R.string.general_error));
			}
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}


			if (e.getErrorCode() == MegaError.API_OK){
//				Toast.makeText(this, getString(R.string.context_correctly_moved), Toast.LENGTH_LONG).show();
				if (drawerItem == DrawerItem.CLOUD_DRIVE){
					if (moveToRubbish){
						//Update both tabs
        				//Rubbish bin
        				if (rbFLol != null){
							ArrayList<MegaNode> nodes;
							if(rbFLol.getParentHandle()==-1){
								nodes = megaApi.getChildren(megaApi.getNodeByHandle(megaApi.getRubbishNode().getHandle()), orderCloud);
							}
							else{
								nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbFLol.getParentHandle()), orderCloud);
							}
    						rbFLol.setNodes(nodes);
    						rbFLol.getRecyclerView().invalidate();
            			}

        				//Cloud Drive
        				if (fbFLol != null){
							ArrayList<MegaNode> nodes;
							if(fbFLol.getParentHandle()==-1){
								nodes = megaApi.getChildren(megaApi.getNodeByHandle(megaApi.getRootNode().getHandle()), orderCloud);
							}
							else{
								nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderCloud);
							}
    						fbFLol.setNodes(nodes);
    						fbFLol.getRecyclerView().invalidate();
        				}
					}
					else{
						log("Not moved to rubbish");
						int index = viewPagerCDrive.getCurrentItem();
	        			log("----------------------------------------INDEX: "+index);
	        			if(index==1){
	        				//Rubbish bin
	        				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);
	        				rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	        				if (rbFLol != null){
								ArrayList<MegaNode> nodes;
								if(rbFLol.getParentHandle()==-1){
									nodes = megaApi.getChildren(megaApi.getNodeByHandle(megaApi.getRubbishNode().getHandle()), orderCloud);
								}
								else{
									nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbFLol.getParentHandle()), orderCloud);
								}
	    						rbFLol.setNodes(nodes);
	    						rbFLol.getRecyclerView().invalidate();
	            			}
	        			}
	        			else{
	        				//Cloud Drive
	        				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
	        				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	        				if (fbFLol != null){
								ArrayList<MegaNode> nodes;
								if(fbFLol.getParentHandle()==-1){
									nodes = megaApi.getChildren(megaApi.getNodeByHandle(megaApi.getRootNode().getHandle()), orderCloud);
								}
								else{
									nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderCloud);
								}
	    						log("nodes: "+nodes.size());
								fbFLol.setNodes(nodes);
	    						fbFLol.getRecyclerView().invalidate();
	        				}
							else{
								log("FileBrowser is NULL after move");
							}
	        			}
					}
				}
				else if (drawerItem == DrawerItem.INBOX){
					if (iFLol != null){
//							ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(iFLol.getParentHandle()), orderGetChildren);
//							rbFLol.setNodes(nodes);
						iFLol.refresh();
						if (moveToRubbish){
							//Refresh Rubbish Fragment
							String cFTagRb = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);
	        				rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTagRb);
	        				if (rbFLol != null){
	        					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbFLol.getParentHandle()), orderCloud);
	    						rbFLol.setNodes(nodes);
	    						rbFLol.getRecyclerView().invalidate();
	            			}
						}
						else{
							//Refresh Cloud Drive
							String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
	        				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	        				if (fbFLol != null){
								ArrayList<MegaNode> nodes;
								if(fbFLol.getParentHandle()==-1){
									nodes = megaApi.getChildren(megaApi.getNodeByHandle(megaApi.getRootNode().getHandle()), orderCloud);
								}
								else{
									nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderCloud);
								}
	    						fbFLol.setNodes(nodes);
	    						fbFLol.getRecyclerView().invalidate();
	        				}
						}
					}
				}
				else if (drawerItem == DrawerItem.SHARED_ITEMS){
					String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 0);
    				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
					if (inSFLol != null){
						//TODO: ojo con los hijos
//							ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(inSFLol.getParentHandle()), orderGetChildren);
//							inSFLol.setNodes(nodes);
						inSFLol.getRecyclerView().invalidate();
					}
	    			sharesTag = getFragmentTag(R.id.shares_tabs_pager, 1);
	        		outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
					if (outSFLol != null){
						//TODO: ojo con los hijos
//							ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(outSFLol.getParentHandle()), orderGetChildren);
//							inSFLol.setNodes(nodes);
						outSFLol.getRecyclerView().invalidate();
					}

					if (moveToRubbish){
						//Refresh Rubbish Fragment
						String cFTagRb = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);
        				rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTagRb);
        				if (rbFLol != null){
        					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbFLol.getParentHandle()), orderCloud);
    						rbFLol.setNodes(nodes);
    						rbFLol.getRecyclerView().invalidate();
            			}
					}
					else{
						//Refresh Cloud Drive
						String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
        				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
        				if (fbFLol != null){
							ArrayList<MegaNode> nodes;
							if(fbFLol.getParentHandle()==-1){
								nodes = megaApi.getChildren(megaApi.getNodeByHandle(megaApi.getRootNode().getHandle()), orderCloud);
							}
							else{
								nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderCloud);
							}
    						fbFLol.setNodes(nodes);
    						fbFLol.getRecyclerView().invalidate();
        				}
					}
				}
				else if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE){
					if (oFLol != null){
//							ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(iFLol.getParentHandle()), orderGetChildren);
//							rbFLol.setNodes(nodes);
						oFLol.refreshPaths();
						//Refresh Cloud Drive
						String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
        				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
        				if (fbFLol != null){
							ArrayList<MegaNode> nodes;
							if(fbFLol.getParentHandle()==-1){
								nodes = megaApi.getChildren(megaApi.getNodeByHandle(megaApi.getRootNode().getHandle()), orderCloud);
							}
							else{
								nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderCloud);
							}
    						fbFLol.setNodes(nodes);
    						fbFLol.getRecyclerView().invalidate();
        				}
					}
				}
			}
			else{
				log("MOVE ERROR "+e.getErrorString());
			}
			if (moveToRubbish){
				if (e.getErrorCode() == MegaError.API_OK){
					showSnackbar(getString(R.string.context_correctly_moved_to_rubbish));
				}
				else{
					showSnackbar(getString(R.string.context_no_moved));
				}
				log("SINGLE move to rubbish request finished");
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					showSnackbar(getString(R.string.context_correctly_moved));
				}
				else{
					showSnackbar(getString(R.string.context_no_moved));
				}

				log("SINGLE move nodes request finished");
			}
		}
		else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS){
			log("MegaRequest.TYPE_PAUSE_TRANSFERS");
			if (e.getErrorCode() == MegaError.API_OK) {

				if(megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD)||megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)){
					log("show PLAY button");
					pauseTransfersMenuIcon.setVisible(false);
					playTransfersMenuIcon.setVisible(true);
					if (tFLol != null){
						tFLol.setPause(true);
					}
    			}
    			else{
    				log("show PAUSE button");
					pauseTransfersMenuIcon.setVisible(true);
					playTransfersMenuIcon.setVisible(false);
					if (tFLol != null){
						tFLol.setPause(false);
					}
    			}
			}
		}
		else if(request.getType() == MegaRequest.TYPE_CANCEL_TRANSFERS){
			log("MegaRequest.TYPE_CANCEL_TRANSFERS");
			//After cancelling all the transfers
			totalSizeToDownload = 0;
			//Hide Transfer ProgressBar
			if (fbFLol != null){
				fbFLol.hideProgressBar();
			}
			if (rbFLol != null){
				rbFLol.hideProgressBar();
			}
			if (iFLol != null){
				iFLol.hideProgressBar();
			}
			if (outSFLol != null){
				outSFLol.hideProgressBar();
			}
			if (inSFLol != null){
				inSFLol.hideProgressBar();
			}

			pauseTransfersMenuIcon.setVisible(false);
			playTransfersMenuIcon.setVisible(false);
		}
		else if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFER){
			log("one MegaRequest.TYPE_CANCEL_TRANSFER");
			//After cancelling ONE transfer
			if (e.getErrorCode() == MegaError.API_OK){
				tL = megaApi.getTransfers();
				if (tFLol != null){
					if (drawerItem == DrawerItem.TRANSFERS){
						tFLol.setTransfers(tL);
					}
				}
				//Update File Browser Fragment
				if (fbFLol != null){

					HashMap<Long, MegaTransfer> mTHash = new HashMap<Long, MegaTransfer>();
					for(int i=0; i<tL.size(); i++){

						MegaTransfer tempT = tL.get(i);
						if (tempT.getType() == MegaTransfer.TYPE_DOWNLOAD){
							long handleT = tempT.getNodeHandle();
							MegaNode nodeT = megaApi.getNodeByHandle(handleT);
							MegaNode parentT = megaApi.getParentNode(nodeT);

							if (parentT != null){
								if(parentT.getHandle() == this.parentHandleBrowser){
									mTHash.put(handleT,tempT);
								}
							}
						}
					}

					fbFLol.setTransfers(mTHash);
				}
			}
			supportInvalidateOptionsMenu();
		}
		else if (request.getType() == MegaRequest.TYPE_KILL_SESSION){
			log("requestFinish TYPE_KILL_SESSION"+MegaRequest.TYPE_REMOVE);
			if (e.getErrorCode() == MegaError.API_OK){
				log("success kill sessions");
				showSnackbar(getString(R.string.success_kill_all_sessions));
			}
			else
			{
				log("error when killing sessions: "+e.getErrorString());
				showSnackbar(getString(R.string.error_kill_all_sessions));
			}
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE){

			log("requestFinish "+MegaRequest.TYPE_REMOVE);
			if (e.getErrorCode() == MegaError.API_OK){
				if (statusDialog != null){
					if (statusDialog.isShowing()){
						try {
							statusDialog.dismiss();
						}
						catch (Exception ex) {}
					}
				}

				showSnackbar(getString(R.string.context_correctly_removed));
				if (drawerItem == DrawerItem.CLOUD_DRIVE){

					int index = viewPagerCDrive.getCurrentItem();
        			log("----------------------------------------INDEX: "+index);
        			if(index==1){
        				//Rubbish bin
        				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);
        				rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
        				if (rbFLol != null){
        					if (isClearRubbishBin){
    							isClearRubbishBin = false;
    							parentHandleRubbish = megaApi.getRubbishNode().getHandle();
    							rbFLol.setParentHandle(megaApi.getRubbishNode().getHandle());
    							ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderCloud);
    							rbFLol.setNodes(nodes);
    							rbFLol.getRecyclerView().invalidate();
    							aB.setTitle(getString(R.string.section_rubbish_bin));
    							log("aB.setHomeAsUpIndicator_23");
    							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
    							this.firstNavigationLevel = true;
    						}
    						else{
								ArrayList<MegaNode> nodes;
								if(rbFLol.getParentHandle()==-1){
									nodes = megaApi.getChildren(megaApi.getNodeByHandle(megaApi.getRubbishNode().getHandle()), orderCloud);
								}
								else{
									nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbFLol.getParentHandle()), orderCloud);
								}
    							rbFLol.setNodes(nodes);
    							rbFLol.getRecyclerView().invalidate();
    						}
            			}
        			}
        			else{
        				//Cloud Drive
        				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
        				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
        				if (fbFLol != null){
        					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderCloud);
    						fbFLol.setNodes(nodes);
    						fbFLol.getRecyclerView().invalidate();
        				}
        			}
				}
			}
			else{
				showSnackbar(getString(R.string.context_no_removed));
			}
			log("remove request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_EXPORT){
			log("export request finished");

			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){

				if (isGetLink){
					final String link = request.getLink();
					MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());
					log("EXPIRATION DATE: "+node.getExpirationTime());
					if(isExpiredDateLink){
						log("change the expiration date");

						if(node.getExpirationTime()<=0){
							switchGetLink.setChecked(false);
							expiryDateButton.setVisibility(View.INVISIBLE);
						}
						else{
							switchGetLink.setChecked(true);
							java.text.DateFormat df = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault());
							Calendar cal = Util.calculateDateFromTimestamp(node.getExpirationTime());
							TimeZone tz = cal.getTimeZone();
							df.setTimeZone(tz);
							Date date = cal.getTime();
							String formattedDate = df.format(date);
							expiryDateButton.setText(formattedDate);
							expiryDateButton.setVisibility(View.VISIBLE);
						}
					}
					else{
						showGetLinkPanel(link, node.getExpirationTime());
					}
				}
				log("link: "+request.getLink());
			}
			else{
				log("Error: "+e.getErrorString());
				showSnackbar(getString(R.string.context_no_link));
			}
			isGetLink=false;
			isExpiredDateLink=false;
		}
		else if (request.getType() == MegaRequest.TYPE_RENAME){

			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
				showSnackbar(getString(R.string.context_correctly_renamed));
				if (drawerItem == DrawerItem.CLOUD_DRIVE){

					int index = viewPagerCDrive.getCurrentItem();
        			log("----------------------------------------INDEX: "+index);
        			if(index==0){
        		        //Cloud Drive
        				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
        				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
        				if (fbFLol != null){
							ArrayList<MegaNode> nodes;
							if(fbFLol.getParentHandle()==-1){
								nodes = megaApi.getChildren(megaApi.getNodeByHandle(megaApi.getRootNode().getHandle()), orderCloud);
							}
							else{
								nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderCloud);
							}
    						fbFLol.setNodes(nodes);
    						fbFLol.getRecyclerView().invalidate();
    					}
        			}
				}
				else if (drawerItem == DrawerItem.INBOX){

					if (iFLol != null){
//						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(inSFLol.getParentHandle()), orderGetChildren);
						//TODO: ojo con los hijos
//						inSFLol.setNodes(nodes);
						iFLol.getRecyclerView().invalidate();
					}
				}
				else if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE){

					if (oFLol != null){
//						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(inSFLol.getParentHandle()), orderGetChildren);
						//TODO: ojo con los hijos
//						inSFLol.setNodes(nodes);
						oFLol.getRecyclerView().invalidate();
					}
				}
				else if (drawerItem == DrawerItem.SHARED_ITEMS){
					String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 0);
    				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
					if (inSFLol != null){
//						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(inSFLol.getParentHandle()), orderGetChildren);
						//TODO: ojo con los hijos
//						inSFLol.setNodes(nodes);
						inSFLol.getRecyclerView().invalidate();
					}
	    			sharesTag = getFragmentTag(R.id.shares_tabs_pager, 1);
	        		outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
					if (outSFLol != null){
//						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(inSFLol.getParentHandle()), orderGetChildren);
						//TODO: ojo con los hijos
//						inSFLol.setNodes(nodes);
						outSFLol.getRecyclerView().invalidate();
					}
				}
			}
			else{
				showSnackbar(getString(R.string.context_no_renamed));
			}
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			log("TYPE_COPY");
			if(sendToInbox){
				log("sendToInbox: "+e.getErrorCode()+" "+e.getErrorString());
				setSendToInbox(false);
				if (e.getErrorCode() == MegaError.API_OK){
					log("OK");
					showSnackbar(getString(R.string.context_correctly_sent_node));
				}
				else if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
					log("OVERQUOTA ERROR: "+e.getErrorCode());
					showOverquotaAlert();
				}
				else
				{
					log("NO SENT");
					showSnackbar(getString(R.string.context_no_sent_node));
				}
			}
			else{
				try {
					statusDialog.dismiss();
				}
				catch (Exception ex) {}

				if (e.getErrorCode() == MegaError.API_OK){
					log("Show snackbar!!!!!!!!!!!!!!!!!!!");
					showSnackbar(getString(R.string.context_correctly_copied));

					if (drawerItem == DrawerItem.CLOUD_DRIVE){

						int index = viewPagerCDrive.getCurrentItem();
	        			log("----------------------------------------INDEX: "+index);
	        			if(index==1){
	        				//Rubbish bin
	        				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);
	        				rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	        				if (rbFLol != null){
								ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbFLol.getParentHandle()), orderCloud);
								rbFLol.setNodes(nodes);
								rbFLol.getRecyclerView().invalidate();
							}
	        			}
	        			else{
	        				//Cloud Drive
	        				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
	        				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	        				if (fbFLol != null){
								ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderCloud);
								fbFLol.setNodes(nodes);
								fbFLol.getRecyclerView().invalidate();
							}
	        			}
					}
					else if (drawerItem == DrawerItem.INBOX){
						if (iFLol != null){
							iFLol.getRecyclerView().invalidate();
						}
					}
				}
				else{
					if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
						log("OVERQUOTA ERROR: "+e.getErrorCode());
						showOverquotaAlert();
					}
					else
					{
						showSnackbar(getString(R.string.context_no_copied));
					}
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER){
			try {
				statusDialog.dismiss();
			}
			catch (Exception ex) {}

			if (e.getErrorCode() == MegaError.API_OK){
				showSnackbar(getString(R.string.context_folder_created));
				if (fbFLol != null){
					if (drawerItem == DrawerItem.CLOUD_DRIVE){
						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderCloud);
						fbFLol.setNodes(nodes);
						fbFLol.getRecyclerView().invalidate();
					}
				}
			}
			else{
				log("TYPE_CREATE_FOLDER ERROR: "+e.getErrorCode()+" "+e.getErrorString());
				showSnackbar(getString(R.string.context_folder_no_created));
			}
		}
		else if (request.getType() == MegaRequest.TYPE_SHARE){
			try {
				statusDialog.dismiss();
				log("Dismiss");
			}
			catch (Exception ex) {log("Exception");}
			if (e.getErrorCode() == MegaError.API_OK){
				log("OK MegaRequest.TYPE_SHARE");
				if(request.getAccess()==MegaShare.ACCESS_UNKNOWN){
					showSnackbar(getString(R.string.context_remove_sharing));
				}
				else{
					showSnackbar(getString(R.string.context_correctly_shared));
				}
			}
			else{
//				log("ERROR MegaRequest.TYPE_SHARE: "+request.getEmail()+" : "+request.getName());
				if(request.getAccess()==MegaShare.ACCESS_UNKNOWN){
					showSnackbar(getString(R.string.context_no_removed_shared));
				}
				else{
					showSnackbar(getString(R.string.context_no_shared));
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_SUBMIT_PURCHASE_RECEIPT){
			if (e.getErrorCode() == MegaError.API_OK){
				log("PURCHASE CORRECT!");
				drawerItem = DrawerItem.CLOUD_DRIVE;
				selectDrawerItemLollipop(drawerItem);
			}
			else{
				log("PURCHASE WRONG: " + e.getErrorString() + " (" + e.getErrorCode() + ")");
//				Snackbar.make(fragmentContainer, "PURCHASE WRONG: " + e.getErrorString() + " (" + e.getErrorCode() + ")", Snackbar.LENGTH_LONG).show();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_CLEAN_RUBBISH_BIN){
			if (e.getErrorCode() == MegaError.API_OK){
				log("OK MegaRequest.TYPE_CLEAN_RUBBISH_BIN");
				showSnackbar(getString(R.string.rubbish_bin_emptied));
			}
			else{
				showSnackbar(getString(R.string.rubbish_bin_no_emptied));
			}
		}
		else if (request.getType() == MegaRequest.TYPE_REGISTER_PUSH_NOTIFICATION){
			if (e.getErrorCode() == MegaError.API_OK){
				log("FCM OK TOKEN MegaRequest.TYPE_REGISTER_PUSH_NOTIFICATION");
			}
			else{
				log("FCM ERROR TOKEN TYPE_REGISTER_PUSH_NOTIFICATION: " + e.getErrorCode() + "__" + e.getErrorString());
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString() + "__" + e.getErrorCode() + "__" + e.getErrorString());
	}

	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
		log("onUsersUpdateLollipop-----------------------------------------------");

		if (users != null){
			log("users.size(): "+users.size());
			for(int i=0; i<users.size();i++){
				MegaUser user=users.get(i);

				if(user!=null){
					if(user.isOwnChange()>0){
						log("isOwnChange!!!: "+user.isOwnChange());
						continue;
					}
					log("NOT OWN change: "+user.isOwnChange());

					if (user.hasChanged(MegaUser.CHANGE_TYPE_FIRSTNAME)){
						log("The user: "+user.getEmail()+"changed his first name");
						if(user.getEmail().equals(megaApi.getMyUser().getEmail())){
							log("I change my first name");
							myAccountInfo.setFirstName(false);
							megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_FIRSTNAME, myAccountInfo);
						}
						else{
							myAccountInfo.setFirstName(false);
							megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_FIRSTNAME, new ContactNameListener(this));
						}
					}
					if (user.hasChanged(MegaUser.CHANGE_TYPE_LASTNAME)){
						log("The user: "+user.getEmail()+"changed his last name");
						if(user.getEmail().equals(megaApi.getMyUser().getEmail())){
							log("I change my last name");
							myAccountInfo.setLastName(false);
							megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_LASTNAME, myAccountInfo);
						}
						else{
							myAccountInfo.setLastName(false);
							megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_LASTNAME, new ContactNameListener(this));
						}
					}
					if (user.hasChanged(MegaUser.CHANGE_TYPE_AVATAR)){
						log("The user: "+user.getEmail()+"changed his AVATAR");

						File avatar = null;
						if (this.getExternalCacheDir() != null){
							avatar = new File(this.getExternalCacheDir().getAbsolutePath(), user.getEmail() + ".jpg");
						}
						else{
							avatar = new File(this.getCacheDir().getAbsolutePath(), user.getEmail() + ".jpg");
						}
						Bitmap bitmap = null;
						if (avatar.exists()){
							avatar.delete();
						}

						if(user.getEmail().equals(megaApi.getMyUser().getEmail())){
							log("I change my avatar");
							if (getExternalCacheDir() != null){
								String destinationPath = null;
								destinationPath = getExternalCacheDir().getAbsolutePath() + "/" + myAccountInfo.getMyUser().getEmail() + ".jpg";
								if(destinationPath!=null){
									log("The destination of the avatar is: "+destinationPath);
									megaApi.getUserAvatar(myAccountInfo.getMyUser(), destinationPath, myAccountInfo);
								}
								else{
									log("ERROR! Destination PATH is NULL");
								}
							}
							else{
								log("getExternalCacheDir() is NULL");
								megaApi.getUserAvatar(myAccountInfo.getMyUser(), getCacheDir().getAbsolutePath() + "/" + myAccountInfo.getMyUser().getEmail() + ".jpg", myAccountInfo);
							}
						}
						else{
							log("Update de ContactsFragment");
							String cFTag = getFragmentTag(R.id.contact_tabs_pager, 0);
							cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
							if (cFLol != null) {
								if (drawerItem == DrawerItem.CONTACTS) {
									cFLol.updateView();
								}
							}
						}
					}
					if (user.hasChanged(MegaUser.CHANGE_TYPE_EMAIL)){
						log("CHANGE_TYPE_EMAIL");
						if(user.getEmail().equals(megaApi.getMyUser().getEmail())){
							log("I change my mail");
							nVEmail.setText(user.getEmail());
						}
						else{
							log("The contact: "+user.getHandle()+" changes the mail: "+user.getEmail());
							if(dbH.findContactByHandle(String.valueOf(user.getHandle()))==null){
								log("The contact NOT exists -> DB inconsistency! -> Clear!");
								if (dbH.getContactsSize() != megaApi.getContacts().size()){
									dbH.clearContacts();
									FillDBContactsTask fillDBContactsTask = new FillDBContactsTask(this);
									fillDBContactsTask.execute();
								}
							}
							else{
								log("The contact already exists -> update");
								dbH.setContactMail(user.getHandle(),user.getEmail());
							}
						}
					}
				}
				else{
					log("Continue...");
					continue;
				}
			}
		}
	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> updatedNodes) {
		log("onNodesUpdateLollipop");
		try {
			statusDialog.dismiss();
		}
		catch (Exception ex) {}

		if (drawerItem == DrawerItem.CLOUD_DRIVE){
			log("DrawerItem.CLOUD_DRIVE");

			if(viewPagerCDrive!=null) {
				int index = viewPagerCDrive.getCurrentItem();
				log("Fragment Index: " + index);
				if (index == 1) {
					log("Rubbish bin shown");
					String cloudTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
					fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cloudTag);
					if (fbFLol != null){
						log("FileBrowser is not NULL");
						ArrayList<MegaNode> nodes;
						if(parentHandleBrowser==-1||parentHandleBrowser==megaApi.getRootNode().getHandle()){
							nodes = megaApi.getChildren(megaApi.getRootNode(), orderCloud);
						}
						else{
							nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderCloud);
						}
						fbFLol.setNodes(nodes);
						fbFLol.setContentText();
						fbFLol.getRecyclerView().invalidate();
					}

					String rubbishTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);
					rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(rubbishTag);
					if (rbFLol != null){
						if (isClearRubbishBin){
							isClearRubbishBin = false;
							parentHandleRubbish = megaApi.getRubbishNode().getHandle();
							aB.setTitle(getString(R.string.section_rubbish_bin));
							log("aB.setHomeAsUpIndicator_24");
							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
							this.firstNavigationLevel = true;

							ArrayList<MegaNode> nodes;
							if(rbFLol.getParentHandle()==-1){
								nodes = megaApi.getChildren(megaApi.getNodeByHandle(megaApi.getRubbishNode().getHandle()), orderCloud);
							}
							else{
								nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbFLol.getParentHandle()), orderCloud);
							}
							rbFLol.setParentHandle(megaApi.getRubbishNode().getHandle());
							rbFLol.setNodes(nodes);
							rbFLol.getRecyclerView().invalidate();

						}
						else{

							ArrayList<MegaNode> nodes;
							if(rbFLol.getParentHandle()==-1){
								nodes = megaApi.getChildren(megaApi.getNodeByHandle(megaApi.getRubbishNode().getHandle()), orderCloud);
							}
							else{
								nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbFLol.getParentHandle()), orderCloud);
							}
							rbFLol.setNodes(nodes);
							rbFLol.setContentText();
							rbFLol.getRecyclerView().invalidate();

						}
					}
				}
				else {
					//Cloud Drive TAB
					log("Cloud drive shown");

					String rubbishTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);
					rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(rubbishTag);
					if (rbFLol != null){
						if (isClearRubbishBin){
							isClearRubbishBin = false;
							parentHandleRubbish = megaApi.getRubbishNode().getHandle();
							aB.setTitle(getString(R.string.section_rubbish_bin));
							log("aB.setHomeAsUpIndicator_24");
							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
							this.firstNavigationLevel = true;

							ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderCloud);
							rbFLol.setParentHandle(megaApi.getRubbishNode().getHandle());
							rbFLol.setNodes(nodes);
							rbFLol.getRecyclerView().invalidate();

						}
						else{
							log("NOT clearRubbish");
							ArrayList<MegaNode> nodes;
							if(parentHandleRubbish==-1||parentHandleRubbish==megaApi.getRubbishNode().getHandle()){
								nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderCloud);
							}
							else{
								nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleRubbish), orderCloud);
							}
							if(nodes!=null){
								rbFLol.setNodes(nodes);
								rbFLol.setContentText();
								rbFLol.getRecyclerView().invalidate();
							}
						}
					}

					String cloudTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);
					fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cloudTag);
					if (fbFLol != null){
						log("FileBrowser is not NULL");
						ArrayList<MegaNode> nodes;
						if(parentHandleBrowser==-1||parentHandleBrowser==megaApi.getRootNode().getHandle()){
							nodes = megaApi.getChildren(megaApi.getRootNode(), orderCloud);
						}
						else{
							nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderCloud);
						}
						if(nodes!=null){
							fbFLol.setNodes(nodes);
							fbFLol.setContentText();
							fbFLol.getRecyclerView().invalidate();
						}
					}
				}
			}
		}
		else if (drawerItem == DrawerItem.SEARCH){
			log("SEARCH shown");
			if (sFLol != null){
				sFLol.refresh();
			}
		}
		else if (drawerItem == DrawerItem.INBOX){
			log("INBOX shown");
			if (iFLol != null){
				iFLol.refresh();
//				iFLol.getListView().invalidateViews();
			}
		}

		else if (drawerItem == DrawerItem.SHARED_ITEMS){
			int index = viewPagerShares.getCurrentItem();
			if(index==1){
				//OUTGOING
				String cFTag2 = getFragmentTag(R.id.shares_tabs_pager, 1);
				log("DrawerItem.SHARED_ITEMS Tag: "+ cFTag2);
				outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag2);
				if (outSFLol != null){
					MegaNode node = megaApi.getNodeByHandle(parentHandleOutgoing);
					if (node != null){
						outSFLol.setNodes(megaApi.getChildren(node, orderOthers));
						aB.setTitle(node.getName());
						log("indicator_arrow_back_888");
    					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
    					firstNavigationLevel = false;
					}
					else{
						outSFLol.refresh();
						aB.setTitle(getResources().getString(R.string.section_shared_items));
						log("aB.setHomeAsUpIndicator_26");
    					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
    					firstNavigationLevel = true;
					}
				}
			}
			else{
				//InCOMING
				String cFTag1 = getFragmentTag(R.id.shares_tabs_pager, 0);
				log("DrawerItem.SHARED_ITEMS Tag Incoming: "+ cFTag1);
				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag1);
				if (inSFLol != null){
					MegaNode node = megaApi.getNodeByHandle(parentHandleIncoming);
					if (node != null){
						inSFLol.setNodes(megaApi.getChildren(node, orderOthers));
						aB.setTitle(node.getName());
						log("indicator_arrow_back_889");
    					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
    					firstNavigationLevel = false;
					}
					else{
						inSFLol.findNodes();
						aB.setTitle(getResources().getString(R.string.section_shared_items));
						log("aB.setHomeAsUpIndicator_28");
    					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
    					firstNavigationLevel = true;
					}
				}
			}
		}
		else if (drawerItem == DrawerItem.CAMERA_UPLOADS){
			if (cuFL != null){
				if(cuFL.isAdded()){
					long cameraUploadHandle = cuFL.getPhotoSyncHandle();
					MegaNode nps = megaApi.getNodeByHandle(cameraUploadHandle);
					log("cameraUploadHandle: " + cameraUploadHandle);
					if (nps != null){
						log("nps != null");
						ArrayList<MegaNode> nodes = megaApi.getChildren(nps, MegaApiJava.ORDER_MODIFICATION_DESC);
						cuFL.setNodes(nodes);
					}
				}
			}
		}
		else if (drawerItem == DrawerItem.MEDIA_UPLOADS){
			if (muFLol != null){
				if(muFLol.isAdded()){
					long cameraUploadHandle = muFLol.getPhotoSyncHandle();
					MegaNode nps = megaApi.getNodeByHandle(cameraUploadHandle);
					log("mediaUploadsHandle: " + cameraUploadHandle);
					if (nps != null){
						log("nps != null");
						ArrayList<MegaNode> nodes = megaApi.getChildren(nps, MegaApiJava.ORDER_MODIFICATION_DESC);
						muFLol.setNodes(nodes);
					}
				}
			}
		}
		else if (drawerItem == DrawerItem.CONTACTS){
			int index = viewPagerContacts.getCurrentItem();
			if (index == 0){
				String cFTag = getFragmentTag(R.id.contact_tabs_pager, 0);
				cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
				if (cFLol != null){
					log("Share finish");
					cFLol.updateShares();
				}
			}
		}
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		log("onReloadNeeded");
	}

	@Override
	public void onAccountUpdate(MegaApiJava api) {
		log("onAccountUpdate");
	}

	@Override
	public void onContactRequestsUpdate(MegaApiJava api,ArrayList<MegaContactRequest> requests) {
		log("---------------------onContactRequestsUpdate");

		if(requests!=null){
			for(int i=0; i<requests.size();i++){
				MegaContactRequest req = requests.get(i);
				if(req.isOutgoing()){
					log("SENT REQUEST");
					updateContactsView(true, true, false);
				}
				else{
					log("RECEIVED REQUEST");
					log("STATUS: "+req.getStatus()+" sourceEmail: "+req.getSourceEmail()+" contactHandle: "+req.getHandle());
					if(req.getStatus()==MegaContactRequest.STATUS_ACCEPTED){
						cC.addContactDB(req.getSourceEmail());
					}
					updateContactsView(true, false, true);
				}
			}
		}
	}

	public void setTransfers(ArrayList<MegaTransfer> transfersList){
		log("setTransfers");
		if (tFLol != null){
			tFLol.setTransfers(transfersList);
		}
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		log("onTransferStart");

		HashMap<Long, MegaTransfer> mTHash = new HashMap<Long, MegaTransfer>();

		totalSizeToDownload += transfer.getTotalBytes();
		progressPercent = (int) Math.round((double) totalSizeDownloaded / totalSizeToDownload * 100);
		log(progressPercent + " " + totalSizeDownloaded + " " + totalSizeToDownload);

		//Update transfer list
		tL = megaApi.getTransfers();
		if (tL != null){
			if(tL.size()>0){
				//Show Transfer ProgressBar
				if (fbFLol != null){
					fbFLol.showProgressBar();
					fbFLol.updateProgressBar(progressPercent);
				}
				if (rbFLol != null){
					rbFLol.showProgressBar();
					rbFLol.updateProgressBar(progressPercent);
				}
				if (iFLol != null){
					iFLol.showProgressBar();
					iFLol.updateProgressBar(progressPercent);
				}
				if (outSFLol != null){
					outSFLol.showProgressBar();
					outSFLol.updateProgressBar(progressPercent);
				}
				if (inSFLol != null){
					inSFLol.showProgressBar();
					inSFLol.updateProgressBar(progressPercent);
				}
				if (tFLol != null){
					tFLol.updateProgressBar(progressPercent);
				}
			}
		}

		//Update File Browser Fragment
		if (fbFLol != null){
			for(int i=0; i<tL.size(); i++){

				MegaTransfer tempT = tL.get(i);
				if (tempT.getType() == MegaTransfer.TYPE_DOWNLOAD){
					long handleT = tempT.getNodeHandle();
					MegaNode nodeT = megaApi.getNodeByHandle(handleT);
					MegaNode parentT = megaApi.getParentNode(nodeT);

					if (parentT != null){
						if(parentT.getHandle() == this.parentHandleBrowser){
							mTHash.put(handleT,tempT);
						}
					}
				}
			}

			fbFLol.setTransfers(mTHash);
		}

		if (inSFLol != null){
			for(int i=0; i<tL.size(); i++){

				MegaTransfer tempT = tL.get(i);
				if (tempT.getType() == MegaTransfer.TYPE_DOWNLOAD){
					long handleT = tempT.getNodeHandle();

					mTHash.put(handleT,tempT);
				}
			}

			inSFLol.setTransfers(mTHash);
		}

		if (tFLol != null){
			tFLol.setTransfers(tL);
		}

		log("onTransferStart: " + transfer.getFileName() + " - " + transfer.getTag());
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError e) {
		log("onTransferFinish: "+transfer.getPath());

		HashMap<Long, MegaTransfer> mTHash = new HashMap<Long, MegaTransfer>();

		if (e.getErrorCode() == MegaError.API_OK) {

//			if(Util.isVideoFile(transfer.getPath())){
//				log("Is video!!!");
//				ThumbnailUtilsLollipop.createThumbnailVideo(this, transfer.getPath(), megaApi, transfer.getNodeHandle());
//			}
//			else{
//				log("NOT video!");
//			}

			long currentSizeDownloaded = 0;
			if (transfersDownloadedSize.get(transfer.getTag()) != null){
				currentSizeDownloaded = transfersDownloadedSize.get(transfer.getTag());
			}

			totalSizeDownloaded += (transfer.getTotalBytes()-currentSizeDownloaded);
			transfersDownloadedSize.put(transfer.getTag(), transfer.getTotalBytes());

			progressPercent = (int) Math.round((double) totalSizeDownloaded / totalSizeToDownload * 100);
			log(progressPercent + " " + totalSizeDownloaded + " " + totalSizeToDownload);
			if (fbFLol != null){
				fbFLol.updateProgressBar(progressPercent);
			}
			if (rbFLol != null){
				rbFLol.updateProgressBar(progressPercent);
			}
			if (iFLol != null){
				iFLol.updateProgressBar(progressPercent);
			}
			if (outSFLol != null){
				outSFLol.updateProgressBar(progressPercent);
			}
			if (inSFLol != null){
				inSFLol.updateProgressBar(progressPercent);
			}
			if (tFLol != null){
				tFLol.updateProgressBar(progressPercent);
			}
		}
		else if(e.getErrorCode() == MegaError.API_EINCOMPLETE){
			log("API_EINCOMPLETE: " + transfer.getFileName());
			totalSizeToDownload -= transfer.getTotalBytes();
			Long currentSizeDownloaded = transfersDownloadedSize.get(transfer.getTag());
			if (currentSizeDownloaded != null){
				totalSizeDownloaded -= currentSizeDownloaded;
			}

			progressPercent = (int) Math.round((double) totalSizeDownloaded / totalSizeToDownload * 100);
			log(progressPercent + " " + totalSizeDownloaded + " " + totalSizeToDownload);
			if (fbFLol != null){
				fbFLol.updateProgressBar(progressPercent);
			}
			if (rbFLol != null){
				rbFLol.updateProgressBar(progressPercent);
			}
			if (iFLol != null){
				iFLol.updateProgressBar(progressPercent);
			}
			if (outSFLol != null){
				outSFLol.updateProgressBar(progressPercent);
			}
			if (inSFLol != null){
				inSFLol.updateProgressBar(progressPercent);
			}
			if (tFLol != null){
				tFLol.updateProgressBar(progressPercent);
			}
		}

		//Update transfer list
		tL = megaApi.getTransfers();

		if (tL != null){
			if(tL.size()<=0){
				log("Hide Transfer ProgressBar: "+tL.size());
				supportInvalidateOptionsMenu();
				//Hide Transfer ProgressBar
				if (fbFLol != null){
					fbFLol.hideProgressBar();
				}
				if (rbFLol != null){
					rbFLol.hideProgressBar();
				}
				if (iFLol != null){
					iFLol.hideProgressBar();
				}
				if (outSFLol != null){
					outSFLol.hideProgressBar();
				}
				if (inSFLol != null){
					inSFLol.hideProgressBar();
				}
			}
		}
		else{
			log("megaApi Transfers NULL - Hide Transfer ProgressBar: ");
			supportInvalidateOptionsMenu();
			//Hide Transfer ProgressBar
			if (fbFLol != null){
				fbFLol.hideProgressBar();
			}
			if (rbFLol != null){
				rbFLol.hideProgressBar();
			}
			if (iFLol != null){
				iFLol.hideProgressBar();
			}
			if (outSFLol != null){
				outSFLol.hideProgressBar();
			}
			if (inSFLol != null){
				inSFLol.hideProgressBar();
			}
		}

		if (tFLol != null){
			tFLol.setTransfers(tL);
		}

		//Update File Browser Fragment
		if (fbFLol != null){
			for(int i=0; i<tL.size(); i++){

				MegaTransfer tempT = tL.get(i);
				long handleT = tempT.getNodeHandle();
				MegaNode nodeT = megaApi.getNodeByHandle(handleT);
				MegaNode parentT = megaApi.getParentNode(nodeT);

				if (parentT != null){
					if(parentT.getHandle() == this.parentHandleBrowser){
						mTHash.put(handleT,tempT);
					}
				}
			}
			fbFLol.setTransfers(mTHash);
		}

		if (inSFLol != null){
			for(int i=0; i<tL.size(); i++){

				MegaTransfer tempT = tL.get(i);
				if (tempT.getType() == MegaTransfer.TYPE_DOWNLOAD){
					long handleT = tempT.getNodeHandle();

					mTHash.put(handleT,tempT);
				}
			}

			inSFLol.setTransfers(mTHash);
		}

		log("END onTransferFinish: " + transfer.getFileName() + " - " + transfer.getTag());
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
//		log("onTransferUpdate: " + transfer.getFileName() + " - " + transfer.getTag());

		long currentSizeDownloaded = 0;
		if (transfersDownloadedSize.get(transfer.getTag()) != null){
			currentSizeDownloaded = transfersDownloadedSize.get(transfer.getTag());
		}
		totalSizeDownloaded += (transfer.getTransferredBytes()-currentSizeDownloaded);
		transfersDownloadedSize.put(transfer.getTag(), transfer.getTransferredBytes());

		progressPercent = (int) Math.round((double) totalSizeDownloaded / totalSizeToDownload * 100);
//		log(progressPercent + " " + totalSizeDownloaded + " " + totalSizeToDownload);
		if (fbFLol != null){
			fbFLol.updateProgressBar(progressPercent);
		}
		if (rbFLol != null){
			rbFLol.updateProgressBar(progressPercent);
		}
		if (iFLol != null){
			iFLol.updateProgressBar(progressPercent);
		}
		if (outSFLol != null){
			outSFLol.updateProgressBar(progressPercent);
		}
		if (inSFLol != null){
			inSFLol.updateProgressBar(progressPercent);
		}
		if (tFLol != null){
			tFLol.updateProgressBar(progressPercent);
		}

		if (drawerItem == DrawerItem.CLOUD_DRIVE){
			if (fbFLol != null){
				if (transfer.getType() == MegaTransfer.TYPE_DOWNLOAD){
					Time now = new Time();
					now.setToNow();
					long nowMillis = now.toMillis(false);
					if (lastTimeOnTransferUpdate < 0){
						lastTimeOnTransferUpdate = now.toMillis(false);
						fbFLol.setCurrentTransfer(transfer);
					}
					else if ((nowMillis - lastTimeOnTransferUpdate) > Util.ONTRANSFERUPDATE_REFRESH_MILLIS){
						lastTimeOnTransferUpdate = nowMillis;
						fbFLol.setCurrentTransfer(transfer);
					}
				}
			}
		}
		else if (drawerItem == DrawerItem.SHARED_ITEMS){
			if (inSFLol != null){
				if (transfer.getType() == MegaTransfer.TYPE_DOWNLOAD){
					Time now = new Time();
					now.setToNow();
					long nowMillis = now.toMillis(false);
					if (lastTimeOnTransferUpdate < 0){
						lastTimeOnTransferUpdate = now.toMillis(false);
						inSFLol.setCurrentTransfer(transfer);
					}
					else if ((nowMillis - lastTimeOnTransferUpdate) > Util.ONTRANSFERUPDATE_REFRESH_MILLIS){
						lastTimeOnTransferUpdate = nowMillis;
						inSFLol.setCurrentTransfer(transfer);
					}
				}
			}
		}
		else if (drawerItem == DrawerItem.TRANSFERS){
			if (tFLol != null){
				Time now = new Time();
				now.setToNow();
				long nowMillis = now.toMillis(false);
				log("on transfers update... "+transfer.getTransferredBytes());
				if (lastTimeOnTransferUpdate < 0){
					lastTimeOnTransferUpdate = now.toMillis(false);
					tFLol.setCurrentTransfer(transfer);
				}
				else if ((nowMillis - lastTimeOnTransferUpdate) > Util.ONTRANSFERUPDATE_REFRESH_MILLIS){
					lastTimeOnTransferUpdate = nowMillis;
					tFLol.setCurrentTransfer(transfer);
				}

			}
		}
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {
		log("onTransferTemporaryError: " + transfer.getFileName() + " - " + transfer.getTag());
	}

	@Override
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer) {
		log("onTransferData");

//		if(Util.isVideoFile(transfer.getPath())){
//		log("Is video!!!");
//		ThumbnailUtilsLollipop.createThumbnailVideo(this, transfer.getPath(), megaApi, transfer.getNodeHandle());
//	}
//	else{
//		log("NOT video!");
//	}

		return true;
	}

	public boolean isTransferInProgress(){
		//Update transfer list
		tL = megaApi.getTransfers();
		if (tL != null){
			if(tL.size()<=0){
				return false;
			}
		}
		else{
			return false;
		}
		return true;
	}

	public static void log(String message) {
		Util.log("ManagerActivityLollipop", message);
	}

	public int getProgressPercent() {
		return progressPercent;
	}

	public void setProgressPercent(int progressPercent) {
		this.progressPercent = progressPercent;
	}

	public MegaAccountDetails getAccountInfo() {
		if(myAccountInfo!=null){
			return myAccountInfo.getAccountInfo();
		}
		return null;
	}

	public void setAccountInfo(MegaAccountDetails accountInfo) {
		if(myAccountInfo!=null){
			myAccountInfo.setAccountInfo(accountInfo);
		}
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

	public void setListCameraUploads(boolean isListCameraUploads) {
		this.isListCameraUploads = isListCameraUploads;
	}

	public int getOrderCloud() {
		return orderCloud;
	}

	public void setOrderCloud(int orderCloud) {
		log("setOrderCloud");
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
		log("setOrderContacts");
		this.orderContacts = orderContacts;
		if(prefs!=null) {
			prefs.setPreferredSortContacts(String.valueOf(orderContacts));
		}
		dbH.setPreferredSortContacts(String.valueOf(orderContacts));
	}

	public int getOrderOthers() {
		return orderOthers;
	}

	public void setOrderOthers(int orderOthers) {
		log("setOrderOthers");
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
		log("setPathNavigationOffline: "+pathNavigationOffline);
		this.pathNavigationOffline = pathNavigationOffline;
	}

	public int getDeepBrowserTreeIncoming() {
		String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 0);
		inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
		if(inSFLol!=null) {
			return inSFLol.getDeepBrowserTree();
		}
		else{
			return -1;
		}
	}

	public void setDeepBrowserTreeIncoming(int deepBrowserTreeIncoming) {
		log("setDeepBrowserTreeIncoming: "+deepBrowserTreeIncoming);
		this.deepBrowserTreeIncoming = deepBrowserTreeIncoming;
	}

	public int getDeepBrowserTreeOutgoing() {
		String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 1);
		outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
		if(outSFLol!=null) {
			return outSFLol.getDeepBrowserTree();
		}
		else{
			return -1;
		}
	}

	public void setDeepBrowserTreeOutgoing(int deepBrowserTreeOutgoing) {
		this.deepBrowserTreeOutgoing = deepBrowserTreeOutgoing;
	}

	public static DrawerItem getDrawerItem() {
		return drawerItem;
	}

	public static void setDrawerItem(DrawerItem drawerItem) {
		ManagerActivityLollipop.drawerItem = drawerItem;
	}

	public int getTabItemCloud(){
		if(viewPagerCDrive!=null){
			return viewPagerCDrive.getCurrentItem();
		}
		return -1;
	}

	public int getTabItemShares(){
		if(viewPagerShares!=null){
			return viewPagerShares.getCurrentItem();
		}
		return -1;
	}

	public int getTabItemContacts(){
		if(viewPagerContacts!=null){
			return viewPagerContacts.getCurrentItem();
		}
		return -1;
	}

	public void setTabItemCloud(int index){
		viewPagerCDrive.setCurrentItem(index);
	}

	public void setTabItemShares(int index){
		viewPagerShares.setCurrentItem(index);
	}

	public void setTabItemContacts(int index){
		viewPagerContacts.setCurrentItem(index);
	}

	public void showChatPanel(MegaChatListItem chat){
		log("showChatPanel");

		if(chat!=null){
			this.selectedChatItem = chat;
			ChatBottomSheetDialogFragment bottomSheetDialogFragment = new ChatBottomSheetDialogFragment();
			bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
		}
	}

	public void updateUserNameNavigationView(String fullName, String firstLetter){
		log("updateUserNameNavigationView");

		nVDisplayName.setText(fullName);

		nVPictureProfileTextView.setText(firstLetter);
		nVPictureProfileTextView.setTextSize(32);
		nVPictureProfileTextView.setTextColor(Color.WHITE);
	}

	public void updateMailNavigationView(String email){
		log("updateMailNavigationView: "+email);
		nVEmail.setText(myAccountInfo.getMyUser().getEmail());
	}

	public void animateFABCollection(){
		log("animateFABCollection");

		if(isFabOpen){
			mainFabButtonChat.startAnimation(rotateLeftAnim);
			firstFabButtonChat.startAnimation(closeFabAnim);
			secondFabButtonChat.startAnimation(closeFabAnim);
			thirdFabButtonChat.startAnimation(closeFabAnim);
			firstFabButtonChat.setClickable(false);
			secondFabButtonChat.setClickable(false);
			thirdFabButtonChat.setClickable(false);
			isFabOpen = false;
			log("close COLLECTION FAB");

		} else {
			mainFabButtonChat.startAnimation(rotateRightAnim);
			firstFabButtonChat.startAnimation(openFabAnim);
			secondFabButtonChat.startAnimation(openFabAnim);
			thirdFabButtonChat.startAnimation(openFabAnim);
			firstFabButtonChat.setClickable(true);
			secondFabButtonChat.setClickable(true);
			thirdFabButtonChat.setClickable(true);
			isFabOpen = true;
			fabButton.setVisibility(View.GONE);
			fabButtonsLayout.setVisibility(View.VISIBLE);
			log("open COLLECTION FAB");
		}
	}

	public void hideFabButton(){
		fabButton.setVisibility(View.GONE);
	}

	public void showFabButton(){
		log("showFabButton");
		if(drawerItem==null){
			return;
		}
		fabButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_add_white));
		switch (drawerItem){
			case CLOUD_DRIVE:{
				log("showFabButton: Cloud Drive SECTION");
				int indexCloud = getTabItemCloud();
				switch(indexCloud){
					case 0:{
						log("showFabButton: cloud TAB");
						fabButton.setVisibility(View.VISIBLE);
						break;
					}
					case 1:{
						log("showFabButton: rubbish TAB");
						fabButton.setVisibility(View.GONE);
						break;
					}
					default: {
						fabButton.setVisibility(View.GONE);
						break;
					}
				}
				break;
			}
			case SHARED_ITEMS:{
				log("showFabButton: Shared Items SECTION");
				int indexShares = getTabItemShares();
				switch(indexShares){
					case 0:{
						log("showFabButton: INCOMING TAB");
						String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 0);
						inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
						if(inSFLol!=null){
							int deepBrowserTreeIn = inSFLol.getDeepBrowserTree();
							if(deepBrowserTreeIn<=0){
								log("showFabButton: fabButton GONE");
								fabButton.setVisibility(View.GONE);
							}
							else {
								//Check the folder's permissions
								long handle = inSFLol.getParentHandle();
								log("showFabButton: handle from incoming: "+handle);
								MegaNode parentNodeInSF = megaApi.getNodeByHandle(handle);
								if(parentNodeInSF!=null){
									int accessLevel= megaApi.getAccess(parentNodeInSF);
									log("showFabButton: Node: "+parentNodeInSF.getName());

									switch(accessLevel) {
										case MegaShare.ACCESS_OWNER:
										case MegaShare.ACCESS_READWRITE:
										case MegaShare.ACCESS_FULL: {
											fabButton.setVisibility(View.VISIBLE);
											break;
										}
										case MegaShare.ACCESS_READ: {
											fabButton.setVisibility(View.GONE);
											break;
										}
									}
								}
								else{
									fabButton.setVisibility(View.GONE);
								}
							}
						}
						break;
					}
					case 1:{
						log("showFabButton: OUTGOING TAB");
						String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 1);
						outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
						if(outSFLol!=null){
							int deepBrowserTreeOut = outSFLol.getDeepBrowserTree();
							if(deepBrowserTreeOut<=0){
								fabButton.setVisibility(View.GONE);
							}
							else {
								fabButton.setVisibility(View.VISIBLE);
							}
						}
						break;
					}
					default: {
						fabButton.setVisibility(View.GONE);
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
						fabButton.setVisibility(View.VISIBLE);
						break;
					}
					default:{
						fabButton.setVisibility(View.GONE);
						break;
					}
				}
				break;
			}
			case CHAT:{
				if(megaChatApi!=null){
					if(megaChatApi.getChatRooms().size()==0){
						fabButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_chat_white));
					}
					fabButton.setVisibility(View.VISIBLE);
				}
				else{
					fabButton.setVisibility(View.GONE);
				}

				break;
			}
			default:{
				log("showFabButton: default GONE fabButton");
				fabButton.setVisibility(View.GONE);
				break;
			}
		}
	}

	public void openAdvancedDevices (long handleToDownload){
		log("openAdvancedDevices");
//		handleToDownload = handle;
		String externalPath = Util.getExternalCardPath();

		if(externalPath!=null){
			log("ExternalPath for advancedDevices: "+externalPath);
			MegaNode node = megaApi.getNodeByHandle(handleToDownload);
			if(node!=null){

//				File newFile =  new File(externalPath+"/"+node.getName());
				File newFile =  new File(node.getName());
				log("File: "+newFile.getPath());
				Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

				// Filter to only show results that can be "opened", such as
				// a file (as opposed to a list of contacts or timezones).
				intent.addCategory(Intent.CATEGORY_OPENABLE);

				// Create a file with the requested MIME type.
				String mimeType = MimeTypeList.getMimeType(newFile);
				log("Mimetype: "+mimeType);
				intent.setType(mimeType);
				intent.putExtra(Intent.EXTRA_TITLE, node.getName());
				intent.putExtra("handleToDownload", handleToDownload);
				try{
					startActivityForResult(intent, Constants.WRITE_SD_CARD_REQUEST_CODE);
				}
				catch(Exception e){
					log("Exception in External SDCARD");
					Environment.getExternalStorageDirectory();
					Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
					toast.show();
				}
			}
		}
		else{
			log("No external SD card");
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
		String cFTag = getFragmentTag(R.id.contact_tabs_pager, 0);
		cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
		return cFLol;
	}

	public MyAccountFragmentLollipop getMyAccountFragment() {
		String myAccountTag = getFragmentTag(R.id.my_account_tabs_pager, 0);
		maFLol = (MyAccountFragmentLollipop) getSupportFragmentManager().findFragmentByTag(myAccountTag);
		if(maFLol!=null){
			return maFLol;
		}
		return null;
	}

	public MyStorageFragmentLollipop getMyStorageFragment() {
		String myStorageTag = getFragmentTag(R.id.my_account_tabs_pager, 1);
		mStorageFLol = (MyStorageFragmentLollipop) getSupportFragmentManager().findFragmentByTag(myStorageTag);
		if(mStorageFLol!=null){
			return mStorageFLol;
		}
		return null;
	}

	public UpgradeAccountFragmentLollipop getUpgradeAccountFragment() {
		return upAFL;
	}

	public MonthlyAnnualyFragmentLollipop getMonthlyAnnualyFragment() {
		return myFL;
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
		return sttFLol;
	}

	public void setSettingsFragment(SettingsFragmentLollipop sttFLol) {
		this.sttFLol = sttFLol;
	}

	public MegaUser getSelectedUser() {
		return selectedUser;
	}

	public void setSelectedUser(MegaUser selectedUser) {
		this.selectedUser = selectedUser;
	}


	public MegaContactRequest getSelectedRequest() {
		return selectedRequest;
	}

	public void setSelectedRequest(MegaContactRequest selectedRequest) {
		this.selectedRequest = selectedRequest;
	}

	public int getAccountFragment() {
		return accountFragment;
	}

	public void setAccountFragment(int accountFragment) {
		this.accountFragment = accountFragment;
	}

	public MyAccountInfo getMyAccountInfo() {
		return myAccountInfo;
	}

	public void setMyAccountInfo(MyAccountInfo myAccountInfo) {
		this.myAccountInfo = myAccountInfo;
	}


	public boolean isExpiredDateLink() {
		return isExpiredDateLink;
	}

	public void setExpiredDateLink(boolean expiredDateLink) {
		isExpiredDateLink = expiredDateLink;
	}


	public MegaOffline getSelectedOfflineNode() {
		return selectedOfflineNode;
	}

	public void setSelectedOfflineNode(MegaOffline selectedOfflineNode) {
		this.selectedOfflineNode = selectedOfflineNode;
	}


	public int getSelectedPaymentMethod() {
		return selectedPaymentMethod;
	}

	public void setSelectedPaymentMethod(int selectedPaymentMethod) {
		this.selectedPaymentMethod = selectedPaymentMethod;
	}

	public int getSelectedAccountType() {
		return selectedAccountType;
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

	public MegaChatListItem getSelectedChat() {
		return selectedChatItem;
	}

	public void setSelectedChat(MegaChatListItem selectedChatItem) {
		this.selectedChatItem = selectedChatItem;
	}

	public void enableChat(){
		Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
		intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
		intent.setAction(Constants.ACTION_ENABLE_CHAT);
		startActivity(intent);
		finish();
//		UserCredentials credentials = dbH.getCredentials();
//		String gSession = credentials.getSession();
//		int ret = megaChatApi.init(gSession);
//		megaApi.fetchNodes(this);
	}

	public void connectChat(int status){
		log("connectChat");
		statusToConnect = status;
		megaChatApi.connect(this);
	}


	public void disableChat(){
		log("disableChat");

		drawerItem = DrawerItem.SETTINGS;
		if (nV != null){
			Menu nVMenu = nV.getMenu();
			MenuItem chat = nVMenu.findItem(R.id.navigation_item_chat);
			chat.setTitle(getString(R.string.section_chat));
			MenuItem settings = nVMenu.findItem(R.id.navigation_item_settings);
			settings.setChecked(true);
			settings.setIcon(getResources().getDrawable(R.drawable.settings_red));
		}

		if (megaChatApi != null){
			megaChatApi.removeChatListener(this);
		}

		megaChatApi.logout(this);
		app.disableMegaChatApi();
		megaChatApi=null;
	}

	@Override
	public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {
		if(rChatFL!=null){
			if(rChatFL.isAdded()){
				rChatFL.listItemUpdate(item);
			}
		}

		if(Util.isChatEnabled()){
			if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT)) {
				log("Change unread count: " + item.getTitle());
				if (nV != null){
					Menu nVMenu = nV.getMenu();
					MenuItem chat = nVMenu.findItem(R.id.navigation_item_chat);
					int numberUnread = megaChatApi.getUnreadChats();
					if(numberUnread==0){
						chat.setTitle(getString(R.string.section_chat));
					}
					else{
						String textToShow = String.format(getString(R.string.section_chat_with_notification), numberUnread);
						Spanned result = null;
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
							result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
						} else {
							result = Html.fromHtml(textToShow);
						}
						chat.setTitle(result);
					}
				}
			}
		}
	}

	@Override
	public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {

	}

	@Override
	public void onChatOnlineStatusUpdate(MegaChatApiJava api, int status) {
		if(rChatFL!=null){
			if(rChatFL.isAdded()){
				rChatFL.onlineStatusUpdate(status);
			}
		}
	}

	public boolean isMkLayoutVisible() {
		return mkLayoutVisible;
	}

	public void setMkLayoutVisible(boolean mkLayoutVisible) {
		this.mkLayoutVisible = mkLayoutVisible;
	}
}
