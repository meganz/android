package mega.privacy.android.app.lollipop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.ContactsExplorerActivity;
import mega.privacy.android.app.CreditCardFragment;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.OldPreferences;
import mega.privacy.android.app.PaymentFragment;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.SettingsActivity;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.SortByDialogActivity;
import mega.privacy.android.app.TabsAdapter;
import mega.privacy.android.app.UpgradeAccountFragment;
import mega.privacy.android.app.UploadHereDialog;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.ZipBrowserActivity;
import mega.privacy.android.app.ManagerActivity.DrawerItem;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.Mode;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.billing.IabHelper;
import mega.privacy.android.app.utils.billing.IabResult;
import mega.privacy.android.app.utils.billing.Inventory;
import mega.privacy.android.app.utils.billing.Purchase;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
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
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class ManagerActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface, OnNavigationItemSelectedListener, MegaGlobalListenerInterface, MegaTransferListenerInterface, OnClickListener{
	
	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 250; //in pixels
	
	private static int EDIT_TEXT_ID = 1;  
	
	public static String ACTION_OPEN_MEGA_LINK = "OPEN_MEGA_LINK";
	public static String ACTION_OPEN_MEGA_FOLDER_LINK = "OPEN_MEGA_FOLDER_LINK";
	public static String ACTION_CANCEL_DOWNLOAD = "CANCEL_DOWNLOAD";
	public static String ACTION_CANCEL_UPLOAD = "CANCEL_UPLOAD";
	public static String ACTION_CANCEL_CAM_SYNC = "CANCEL_CAM_SYNC";
	public static String ACTION_IMPORT_LINK_FETCH_NODES = "IMPORT_LINK_FETCH_NODES";
	public static String ACTION_FILE_EXPLORER_UPLOAD = "FILE_EXPLORER_UPLOAD";
	public static String ACTION_FILE_PROVIDER = "ACTION_FILE_PROVIDER";
	public static String ACTION_EXPLORE_ZIP = "EXPLORE_ZIP";
	public static String EXTRA_PATH_ZIP = "PATH_ZIP";
	public static String EXTRA_OPEN_FOLDER = "EXTRA_OPEN_FOLER";
	public static String ACTION_REFRESH_PARENTHANDLE_BROWSER = "REFRESH_PARENTHANDLE_BROWSER";
	public static String ACTION_OVERQUOTA_ALERT = "OVERQUOTA_ALERT";
	public static String ACTION_TAKE_SELFIE = "TAKE_SELFIE";
	public static String ACTION_SHOW_TRANSFERS = "SHOW_TRANSFERS";
	
	final public static int FILE_BROWSER_ADAPTER = 2000;
	final public static int CONTACT_FILE_ADAPTER = 2001;
	final public static int RUBBISH_BIN_ADAPTER = 2002;
	final public static int SHARED_WITH_ME_ADAPTER = 2003;
	final public static int OFFLINE_ADAPTER = 2004;
	final public static int FOLDER_LINK_ADAPTER = 2005;
	final public static int SEARCH_ADAPTER = 2006;
	final public static int PHOTO_SYNC_ADAPTER = 2007;
	final public static int ZIP_ADAPTER = 2008;
	final public static int OUTGOING_SHARES_ADAPTER = 2009;
	final public static int INCOMING_SHARES_ADAPTER = 2010;
	final public static int INBOX_ADAPTER = 2011;
	final public static int INCOMING_REQUEST_ADAPTER = 2012;
	final public static int OUTGOING_REQUEST_ADAPTER = 2013;
	final public static int CAMERA_UPLOAD_ADAPTER = 2014;
		
	public static int REQUEST_CODE_GET = 1000;
	public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
	public static int REQUEST_CODE_GET_LOCAL = 1003;
	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	public static int REQUEST_CODE_REFRESH = 1005;
	public static int REQUEST_CODE_SORT_BY = 1006;
	public static int REQUEST_CODE_SELECT_IMPORT_FOLDER = 1007;
	public static int REQUEST_CODE_SELECT_FOLDER = 1008;
	public static int REQUEST_CODE_SELECT_CONTACT = 1009;
	public static int TAKE_PHOTO_CODE = 1010;
	private static int WRITE_SD_CARD_REQUEST_CODE = 1011;
	private static int REQUEST_CODE_SELECT_FILE = 1012;
	private static int SET_PIN = 1013;
	
	public int accountFragment;
	final public static int MY_ACCOUNT_FRAGMENT = 5000;
	final public static int UPGRADE_ACCOUNT_FRAGMENT = 5001;
	final public static int PAYMENT_FRAGMENT = 5002;
	final public static int OVERQUOTA_ALERT = 5003;
	final public static int CC_FRAGMENT = 5004;
	final public static int FORTUMO_FRAGMENT = 5005;
	
	long totalSizeToDownload=0;
	long totalSizeDownloaded=0;
	private SparseArray<Long> transfersDownloadedSize;
	int progressPercent = 0;
		
	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;
	MegaAttributes attr = null;
	static ManagerActivityLollipop managerActivity = null;
	MegaApplication app = null;
	MegaApiAndroid megaApi;
	Handler handler;
    ArrayList<MegaTransfer> tL;	
	DisplayMetrics outMetrics;
    float scaleText;
    FrameLayout fragmentContainer;
	boolean tranfersPaused = false;	
    Toolbar tB;
    ActionBar aB;
    boolean firstNavigationLevel = true;
    DrawerLayout drawerLayout;
    public enum DrawerItem {
		CLOUD_DRIVE, SAVED_FOR_OFFLINE, CAMERA_UPLOADS, INBOX, SHARED_ITEMS, CONTACTS, SETTINGS, ACCOUNT, SEARCH, TRANSFERS;

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
			}
			return null;			
		}
	}
	static DrawerItem drawerItem = null;
	static DrawerItem lastDrawerItem = null;
	static MenuItem drawerMenuItem = null;
	NavigationView nV;
	FrameLayout accountInfoFrame;
	TextView nVDisplayName;
	TextView nVEmail;
	RoundedImageView nVPictureProfile;
	TextView nVPictureProfileTextView;
	TextView usedSpaceTV;
	TextView totalSpaceTV;
	ProgressBar usedSpacePB;
	
	TextView textViewBrowser; 
	TextView textViewRubbish;
    TextView textViewIncoming; 
	TextView textViewOutgoing;
	
	//Tabs in Contacts
    private TabHost mTabHostContacts;
    //private Fragment contactTabFragment;	
	TabsAdapter mTabsAdapterContacts;
    ViewPager viewPagerContacts;  
    //Tabs in Shares
    private TabHost mTabHostShares;
	TabsAdapter mTabsAdapterShares;
    ViewPager viewPagerShares;     
    //Tabs in Cloud
    private TabHost mTabHostCDrive;
	TabsAdapter mTabsAdapterCDrive;
    ViewPager viewPagerCDrive;
	
	boolean firstTime = true;
	String pathNavigation = "/";
	MegaUser contact = null;
	String searchQuery = null;
	boolean isSearching = false;
	ArrayList<MegaNode> searchNodes;
	int levelsSearch = -1;
	boolean openLink = false;
	boolean sendToInbox = false;
	long handleToDownload=0;
	long lastTimeOnTransferUpdate = -1;	
	
	String nameText = "";
	String firstNameText = "";
	boolean name = false;
	boolean firstName = false;
	
	private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	private int orderContacts = MegaApiJava.ORDER_DEFAULT_ASC;
	private int orderOffline = MegaApiJava.ORDER_DEFAULT_ASC;
	private int orderOutgoing = MegaApiJava.ORDER_DEFAULT_ASC;
	private int orderIncoming = MegaApiJava.ORDER_DEFAULT_ASC;
	
	boolean firstTimeCam = false;	
	private boolean isGetLink = false;
	private boolean isClearRubbishBin = false;
	private boolean moveToRubbish = false;
	private int usedPerc = 0;
	int accountType = -1;
	MegaAccountDetails accountInfo = null;
	BitSet paymentBitSet = null;
	long numberOfSubscriptions = -1;
	long usedGbStorage = -1;
	private List<ShareInfo> filePreparedInfos;
	ArrayList<String> contactsData;
	boolean megaContacts = true;
	String feedback;
	
	private boolean isListCloudDrive = true;
	private boolean isListOffline = true;
	private boolean isListRubbishBin = true;
	private boolean isListCameraUploads = true;
	private boolean isLargeGridCameraUploads = true; 
	private boolean isListInbox = true;
	private boolean isListContacts = true;
	private boolean isListIncoming = true;
	private boolean isListOutgoing = true;
	
	long parentHandleBrowser;
	long parentHandleRubbish;
	long parentHandleIncoming;
	long parentHandleOutgoing;
	long parentHandleSearch;
	long parentHandleInbox;
	
	//NON LOLLIPOP FRAGMENTS
	private UpgradeAccountFragment upAF;
	private PaymentFragment pF;
	private CreditCardFragment ccF;
	private CameraUploadFragmentLollipop cuF;
	
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
	private TransfersFragmentLollipop tFLol;
	private SearchFragmentLollipop sFLol;
	private SettingsFragmentLollipop sttFLol;
	
	ProgressDialog statusDialog;
	
	public UploadHereDialog uploadDialog;	
	private AlertDialog renameDialog;
	private AlertDialog newFolderDialog;
	private AlertDialog addContactDialog;
	private AlertDialog overquotaDialog;
	private AlertDialog permissionsDialog;
	private AlertDialog openLinkDialog;
	private AlertDialog alertNotPermissionsUpload;
	private AlertDialog clearRubbishBinDialog;
	
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
	private MenuItem settingsMenuItem;
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
    // SKU for our subscription PRO_III monthly
    static final String SKU_PRO_III_MONTH = "mega.android.pro3.onemonth";
    // SKU for our subscription PRO_LITE monthly
    static final String SKU_PRO_LITE_MONTH = "mega.android.prolite.onemonth";
    // SKU for our subscription PRO_LITE yearly
    static final String SKU_PRO_LITE_YEAR = "mega.android.prolite.oneyear";
    
    Purchase proLiteMonthly;
    Purchase proLiteYearly;
    Purchase proIMonthly;
    Purchase proIYearly;
    Purchase proIIMonthly;
    Purchase proIIIMonthly;
    Purchase maxP;
    
    int levelInventory = -1;
    int levelAccountDetails = -1;
    
    boolean inventoryFinished = false;
    boolean accountDetailsFinished = false;
    
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
                showAlert("Thank you for subscribing to PRO I Monthly!");
            }
            else if (purchase.getSku().equals(SKU_PRO_I_YEAR)) {
                log("PRO I Yearly subscription purchased.");
                showAlert("Thank you for subscribing to PRO I Yearly!");
            }
            else if (purchase.getSku().equals(SKU_PRO_II_MONTH)) {
                log("PRO II Monthly subscription purchased.");
                showAlert("Thank you for subscribing to PRO II Monthly!");
            }
            else if (purchase.getSku().equals(SKU_PRO_III_MONTH)) {
                log("PRO III Monthly subscription purchased.");
                showAlert("Thank you for subscribing to PRO III Monthly!");
            }
            else if (purchase.getSku().equals(SKU_PRO_LITE_MONTH)) {
                log("PRO LITE Monthly subscription purchased.");
                showAlert("Thank you for subscribing to PRO LITE Monthly!");
            }
            else if (purchase.getSku().equals(SKU_PRO_LITE_YEAR)) {
                log("PRO LITE Yearly subscription purchased.");
                showAlert("Thank you for subscribing to PRO LITE Yearly!");
            }
            
            if (managerActivity != null){
            	megaApi.submitPurchaseReceipt(purchase.getOriginalJson(), managerActivity);
            }
            else{
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
            proIIIMonthly = inventory.getPurchase(SKU_PRO_III_MONTH);
           
            if (proLiteMonthly != null){
            	if (megaApi.getMyEmail() != null){
	        		if (proLiteMonthly.getDeveloperPayload().compareTo(megaApi.getMyEmail()) == 0){
	        			levelInventory = 0;	
	        			maxP = proLiteMonthly;
	        		}
            	}
        	}
            
            if (proLiteYearly != null){
            	if (megaApi.getMyEmail() != null){
	            	if (proLiteYearly.getDeveloperPayload().compareTo(megaApi.getMyEmail()) == 0){
	        			levelInventory = 0;
	        			maxP = proLiteYearly;
	        		}
            	}
        	}
            
            if (proIMonthly != null){
            	if (megaApi.getMyEmail() != null){
	            	if (proIMonthly.getDeveloperPayload().compareTo(megaApi.getMyEmail()) == 0){
	        			levelInventory = 1;	
	        			maxP = proIMonthly;
	        		}
            	}
        	}
            
            if (proIYearly!= null){
            	if (megaApi.getMyEmail() != null){
	            	if (proIYearly.getDeveloperPayload().compareTo(megaApi.getMyEmail()) == 0){
	        			levelInventory = 1;
	        			maxP = proIYearly;
	        		}
            	}
        	}
            
            if (proIIMonthly != null){
            	if (megaApi.getMyEmail() != null){
	            	if (proIIMonthly.getDeveloperPayload().compareTo(megaApi.getMyEmail()) == 0){
	        			levelInventory = 2;
	        			maxP = proIIMonthly;
	        		}
            	}
            }
            
            if (proIIIMonthly != null){
            	if (megaApi.getMyEmail() != null){
	            	if (proIIIMonthly.getDeveloperPayload().compareTo(megaApi.getMyEmail()) == 0){
	        			levelInventory = 3;	
	        			maxP = proIIIMonthly;
	        		}
            	}
            }
            
            inventoryFinished = true;
            
            if (accountDetailsFinished){
            	if (levelInventory > levelAccountDetails){
            		if (maxP != null){
            			megaApi.submitPurchaseReceipt(maxP.getOriginalJson(), managerActivity);
            		}
            	}
            }
            
            
            boolean isProIMonthly = false;
            if (proIMonthly != null){
            	isProIMonthly = true;
            }
            if (isProIMonthly){
            	log("PRO I IS SUBSCRIPTED: ORDERID: ***____" + proIMonthly.getOrderId() + "____*****");
            }
            else{
            	log("PRO I IS NOT SUBSCRIPTED");
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
    	String payload = megaApi.getMyEmail();
    	
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
    	else if (productId.compareTo(SKU_PRO_III_MONTH) == 0){
    		mHelper.launchPurchaseFlow(this,
    				SKU_PRO_III_MONTH, IabHelper.ITEM_TYPE_SUBS,
                    RC_REQUEST, mPurchaseFinishedListener, payload);	
    	}
    	else if (productId.compareTo(SKU_PRO_LITE_MONTH) == 0){
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
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		File thumbDir;
		if (getExternalCacheDir() != null){
			thumbDir = new File (getExternalCacheDir(), "thumbnailsMEGA");
			thumbDir.mkdirs();
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
		
		log("retryPendingConnections()");
		if (megaApi != null){
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
	    		startActivity(loginWithOldCredentials);
	    		finish();
	    		return;
		    }
	    	
	    	Intent newIntent = getIntent();
	    	
	    	if (newIntent != null){
		    	if (newIntent.getAction() != null){
		    		if (newIntent.getAction().equals(ManagerActivityLollipop.ACTION_OPEN_MEGA_LINK) || newIntent.getAction().equals(ManagerActivityLollipop.ACTION_OPEN_MEGA_FOLDER_LINK)){
		    			openLink = true;
		    		}
		    		else if (newIntent.getAction().equals(ACTION_CANCEL_UPLOAD) || newIntent.getAction().equals(ACTION_CANCEL_DOWNLOAD) || newIntent.getAction().equals(ACTION_CANCEL_CAM_SYNC)){
		    			Intent cancelTourIntent = new Intent(this, TourActivityLollipop.class);
		    			cancelTourIntent.setAction(newIntent.getAction());
		    			startActivity(cancelTourIntent);
		    			finish();
		    			return;		    			
		    		}
		    	}
		    }
	    	
	    	if (!openLink){
		    	logout(this, megaApi, false);
		    }
	    	
	    	return;
	    }
	    
	    prefs = dbH.getPreferences();
		if (prefs == null){
			firstTime = true;
		}
		else{
			if (prefs.getFirstTime() == null){
				firstTime = true;
			}
			else{
				firstTime = Boolean.parseBoolean(prefs.getFirstTime());
			}
		}
		
		getOverflowMenu();
		
		handler = new Handler();
		
		setContentView(R.layout.activity_manager);
		
		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
        aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
//        aB.setDisplayShowHomeEnabled(true);
//        getSupportActionBar().setCustomView(R.layout.custom_action_bar_top);
        
        //Set navigation view
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        nV = (NavigationView) findViewById(R.id.navigation_view);
        nV.setNavigationItemSelectedListener(this);
        
        accountInfoFrame = (FrameLayout) findViewById(R.id.navigation_drawer_account_view);
        accountInfoFrame.setOnClickListener(this);
        
        nVDisplayName = (TextView) findViewById(R.id.navigation_drawer_account_information_display_name);
        nVEmail = (TextView) findViewById(R.id.navigation_drawer_account_information_email);
        nVPictureProfile = (RoundedImageView) findViewById(R.id.navigation_drawer_user_account_picture_profile);
        nVPictureProfileTextView = (TextView) findViewById(R.id.navigation_drawer_user_account_picture_profile_textview);
        
        fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container);
        usedSpaceTV = (TextView) findViewById(R.id.navigation_drawer_used_space);
        totalSpaceTV = (TextView) findViewById(R.id.navigation_drawer_total_space);
        usedSpacePB = (ProgressBar) findViewById(R.id.manager_used_space_bar);
        
        mTabHostCDrive = (TabHost)findViewById(R.id.tabhost_cloud_drive);        
        mTabHostCDrive.setup();
        mTabHostCDrive.getTabWidget().setDividerDrawable(null);
        
//        BackgroundColor(getResources().getColor(R.color.tab_text_color));
                      
        mTabHostContacts = (TabHost)findViewById(R.id.tabhost_contacts);
        mTabHostContacts.setup();
        mTabHostContacts.getTabWidget().setDividerDrawable(null);
        
        mTabHostShares = (TabHost)findViewById(R.id.tabhost_shares);
        mTabHostShares.setup();
        mTabHostShares.getTabWidget().setDividerDrawable(null);
        
        viewPagerContacts = (ViewPager) findViewById(R.id.contact_tabs_pager);  
        viewPagerShares = (ViewPager) findViewById(R.id.shares_tabs_pager);  
        viewPagerCDrive = (ViewPager) findViewById(R.id.cloud_drive_tabs_pager);
        
        if (!Util.isOnline(this)){
        	
        	Intent offlineIntent = new Intent(this, OfflineActivityLollipop.class);
			startActivity(offlineIntent);
			finish();
        	return;
        }
        
        dbH.setAttrOnline(true);
        this.setPathNavigationOffline(pathNavigation);
        
        MegaNode rootNode = megaApi.getRootNode();
		if (rootNode == null){
			 if (getIntent() != null){
				if (getIntent().getAction() != null){
					if (getIntent().getAction().equals(ManagerActivityLollipop.ACTION_IMPORT_LINK_FETCH_NODES)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ManagerActivityLollipop.ACTION_IMPORT_LINK_FETCH_NODES);
						intent.setData(Uri.parse(getIntent().getDataString()));
						startActivity(intent);
						finish();	
						return;
					}
					else if (getIntent().getAction().equals(ManagerActivityLollipop.ACTION_OPEN_MEGA_LINK)){
						Intent intent = new Intent(managerActivity, FileLinkActivityLollipop.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ManagerActivityLollipop.ACTION_IMPORT_LINK_FETCH_NODES);
						intent.setData(Uri.parse(getIntent().getDataString()));
						startActivity(intent);
						finish();	
						return;
					}
					else if (getIntent().getAction().equals(ManagerActivityLollipop.ACTION_OPEN_MEGA_FOLDER_LINK)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ManagerActivityLollipop.ACTION_OPEN_MEGA_FOLDER_LINK);
						intent.setData(Uri.parse(getIntent().getDataString()));
						startActivity(intent);
						finish();	
						return;
					}
					else if (getIntent().getAction().equals(ACTION_CANCEL_UPLOAD) || getIntent().getAction().equals(ACTION_CANCEL_DOWNLOAD) || getIntent().getAction().equals(ACTION_CANCEL_CAM_SYNC)){
						Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(getIntent().getAction());
						startActivity(intent);
						finish();
						return;
					}
				}
			}
			Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}
		else{
			log("rootNode != null");
	        
			initGooglePlayPayments();
			
			megaApi.addGlobalListener(this);
			megaApi.addTransferListener(this);
			
			contact = megaApi.getContact(megaApi.getMyEmail());
			if (contact != null){
				nVEmail.setVisibility(View.VISIBLE);
				nVEmail.setText(contact.getEmail());
//				megaApi.getUserData(this);
				megaApi.getUserAttribute(1, this);
				megaApi.getUserAttribute(2, this);
				
				Bitmap defaultAvatar = Bitmap.createBitmap(DEFAULT_AVATAR_WIDTH_HEIGHT,DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
				Canvas c = new Canvas(defaultAvatar);
				Paint p = new Paint();
				p.setAntiAlias(true);
				p.setColor(getResources().getColor(R.color.color_default_avatar_mega));
				
				int radius; 
		        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
		        	radius = defaultAvatar.getWidth()/2;
		        else
		        	radius = defaultAvatar.getHeight()/2;
		        
				c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
				nVPictureProfile.setImageBitmap(defaultAvatar);
				
			    int avatarTextSize = getAvatarTextSize(density);
			    log("DENSITY: " + density + ":::: " + avatarTextSize);
			    if (contact.getEmail() != null){
				    if (contact.getEmail().length() > 0){
				    	log("TEXT: " + contact.getEmail());
				    	log("TEXT AT 0: " + contact.getEmail().charAt(0));
				    	String firstLetter = contact.getEmail().charAt(0) + "";
				    	firstLetter = firstLetter.toUpperCase(Locale.getDefault());
				    	nVPictureProfileTextView.setText(firstLetter);
				    	nVPictureProfileTextView.setTextSize(32);
				    	nVPictureProfileTextView.setTextColor(Color.WHITE);
				    	nVPictureProfileTextView.setVisibility(View.VISIBLE);
				    }
			    }
			    
				File avatar = null;
				if (getExternalCacheDir() != null){
					avatar = new File(getExternalCacheDir().getAbsolutePath(), contact.getEmail() + ".jpg");
				}
				else{
					avatar = new File(getCacheDir().getAbsolutePath(), contact.getEmail() + ".jpg");
				}
				Bitmap imBitmap = null;
				if (avatar.exists()){
					if (avatar.length() > 0){
						BitmapFactory.Options options = new BitmapFactory.Options();
						options.inJustDecodeBounds = true;
						BitmapFactory.decodeFile(avatar.getAbsolutePath(), options);
						int imageHeight = options.outHeight;
						int imageWidth = options.outWidth;
						String imageType = options.outMimeType;
						
						// Calculate inSampleSize
					    options.inSampleSize = calculateInSampleSize(options, 250, 250);
					    
					    // Decode bitmap with inSampleSize set
					    options.inJustDecodeBounds = false;

						imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), options);
						if (imBitmap == null) {
							avatar.delete();
							if (getExternalCacheDir() != null){
								megaApi.getUserAvatar(contact, getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", this);
							}
							else{
								megaApi.getUserAvatar(contact, getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", this);
							}
						}
						else{
							Bitmap circleBitmap = Bitmap.createBitmap(imBitmap.getWidth(), imBitmap.getHeight(), Bitmap.Config.ARGB_8888);
							
							BitmapShader shader = new BitmapShader (imBitmap,  TileMode.CLAMP, TileMode.CLAMP);
					        Paint paint = new Paint();
					        paint.setShader(shader);
					
					        c = new Canvas(circleBitmap);
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
							megaApi.getUserAvatar(contact, getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", this);
						}
						else{
							megaApi.getUserAvatar(contact, getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", this);
						}
					}
				}
				else{
					if (getExternalCacheDir() != null){
						megaApi.getUserAvatar(contact, getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", this);
					}
					else{
						megaApi.getUserAvatar(contact, getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", this);
					}
				}
			}
			
			megaApi.getPaymentMethods(this);
	        megaApi.getAccountDetails(this);
	        megaApi.creditCardQuerySubscriptions(this);
	        
	        if (drawerItem == null) {
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
								return;
							}
							case 1:{	
								drawerItem = DrawerItem.ACCOUNT;
								selectDrawerItemLollipop(drawerItem);
								showpF(1, null, paymentBitSet);
								return;
							}
							case 2:{
								drawerItem = DrawerItem.ACCOUNT;
								selectDrawerItemLollipop(drawerItem);
								showpF(2, null, paymentBitSet);
								return;
							}
							case 3:{
								drawerItem = DrawerItem.ACCOUNT;
								selectDrawerItemLollipop(drawerItem);
								showpF(3, null, paymentBitSet);
								return;
							}	
							case 4:{
								drawerItem = DrawerItem.ACCOUNT;
								selectDrawerItemLollipop(drawerItem);
								showpF(4, null, paymentBitSet);
								return;
							}	
						}
	        		}
	        		else{
						log("upgradeAccount false");
						firstTimeCam = getIntent().getBooleanExtra("firstTimeCam", false);
						if (firstTimeCam){
							log("intent firstTime==true");
							firstTimeCam = true;
							drawerItem = DrawerItem.CAMERA_UPLOADS;
							setIntent(null);
						}
					}
	        	}
	        }
	        else{
				drawerLayout.closeDrawer(Gravity.LEFT);
			}	        
	        
	        parentHandleBrowser = -1;
	        parentHandleRubbish = -1;
	    	parentHandleIncoming = -1;
	    	parentHandleOutgoing = -1;
	    	parentHandleSearch = -1;
	    	parentHandleInbox = -1;
	        
	        //INITIAL FRAGMENT
	        selectDrawerItemLollipop(drawerItem);
		}
	}
	
	@Override
	public void onPostCreate(Bundle savedInstanceState){
		log("onPostCreate");
		super.onPostCreate(savedInstanceState);
	}
	
	@Override
	protected void onResume() {
		log("onResume ");
    	super.onResume();
    	managerActivity = this;
    	
    	Intent intent = getIntent(); 
    	
//    	dbH = new DatabaseHandler(getApplicationContext());
    	dbH = DatabaseHandler.getDbHandler(getApplicationContext());
    	if(dbH.getCredentials() == null){	
    		if (!openLink){
    			logout(this, megaApi, false);
    			return;
    		}			
		}  	
    	   	
    	if (intent != null) {  
    		log("intent not null! "+intent.getAction());
    		// Open folder from the intent
			if (intent.hasExtra(EXTRA_OPEN_FOLDER)) {
				log("INTENT: EXTRA_OPEN_FOLDER");
				parentHandleBrowser = intent.getLongExtra(EXTRA_OPEN_FOLDER, -1);
				intent.removeExtra(EXTRA_OPEN_FOLDER);
				setIntent(null);
			}
    					
    		if (intent.getAction() != null){ 
    			log("intent action");
    			
    			if(getIntent().getAction().equals(ManagerActivityLollipop.ACTION_EXPLORE_ZIP)){  

    				String pathZip=intent.getExtras().getString(EXTRA_PATH_ZIP);    				
    				
    				Intent intentZip = new Intent(managerActivity, ZipBrowserActivity.class);    				
    				intentZip.putExtra(ZipBrowserActivity.EXTRA_PATH_ZIP, pathZip);
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
    			else if (getIntent().getAction().equals(ManagerActivityLollipop.ACTION_IMPORT_LINK_FETCH_NODES)){
					Intent loginIntent = new Intent(managerActivity, LoginActivityLollipop.class);
					loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					loginIntent.setAction(ManagerActivityLollipop.ACTION_IMPORT_LINK_FETCH_NODES);
					loginIntent.setData(Uri.parse(getIntent().getDataString()));
					startActivity(loginIntent);
					finish();	
					return;
				}
				else if (getIntent().getAction().equals(ManagerActivityLollipop.ACTION_OPEN_MEGA_LINK)){
					Intent fileLinkIntent = new Intent(managerActivity, FileLinkActivityLollipop.class);
					fileLinkIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					fileLinkIntent.setAction(ManagerActivityLollipop.ACTION_IMPORT_LINK_FETCH_NODES);
					fileLinkIntent.setData(Uri.parse(getIntent().getDataString()));
					startActivity(fileLinkIntent);
					finish();	
					return;
				}
    			else if (intent.getAction().equals(ACTION_OPEN_MEGA_FOLDER_LINK)){
    				Intent intentFolderLink = new Intent(managerActivity, FolderLinkActivityLollipop.class);
    				intentFolderLink.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    				intentFolderLink.setAction(ManagerActivityLollipop.ACTION_OPEN_MEGA_FOLDER_LINK);
    				intentFolderLink.setData(Uri.parse(getIntent().getDataString()));
					startActivity(intentFolderLink);
					finish();
    			}
    			else if (intent.getAction().equals(ACTION_REFRESH_PARENTHANDLE_BROWSER)){
    				
    				parentHandleBrowser = intent.getLongExtra("parentHandle", -1);    				
    				intent.removeExtra("parentHandle");
    				setParentHandleBrowser(parentHandleBrowser);
    				
    				if (fbFLol != null){
						fbFLol.setParentHandle(parentHandleBrowser);
    					fbFLol.setIsList(isListCloudDrive);
    					fbFLol.setOrder(orderGetChildren);
    					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleBrowser), orderGetChildren);
    					fbFLol.setNodes(nodes);
    					if (!fbFLol.isVisible()){
    						getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fbFLol, "fbFLol").commit();
    					}
    				}	
    				else{
    					fbFLol = new FileBrowserFragmentLollipop();
    					fbFLol.setParentHandle(parentHandleBrowser);
    					fbFLol.setIsList(isListCloudDrive);
    					fbFLol.setOrder(orderGetChildren);
    					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleBrowser), orderGetChildren);
    					fbFLol.setNodes(nodes);
    					if (!fbFLol.isVisible()){
    						getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fbFLol, "fbFLol").commit();
    					}
    				}
    			}
    			else if(intent.getAction().equals(ACTION_OVERQUOTA_ALERT)){
	    			showOverquotaAlert();
	    		}
    			else if (intent.getAction().equals(ACTION_CANCEL_UPLOAD) || intent.getAction().equals(ACTION_CANCEL_DOWNLOAD) || intent.getAction().equals(ACTION_CANCEL_CAM_SYNC)){
    				log("ACTION_CANCEL_UPLOAD or ACTION_CANCEL_DOWNLOAD or ACTION_CANCEL_CAM_SYNC");
					Intent tempIntent = null;
					String title = null;
					String text = null;
					if(intent.getAction().equals(ACTION_CANCEL_UPLOAD)){
						tempIntent = new Intent(this, UploadService.class);
						tempIntent.setAction(UploadService.ACTION_CANCEL);
						title = getString(R.string.upload_uploading);
						text = getString(R.string.upload_cancel_uploading);
					} 
					else if (intent.getAction().equals(ACTION_CANCEL_DOWNLOAD)){
						tempIntent = new Intent(this, DownloadService.class);
						tempIntent.setAction(DownloadService.ACTION_CANCEL);
						title = getString(R.string.download_downloading);
						text = getString(R.string.download_cancel_downloading);
					}
					else if (intent.getAction().equals(ACTION_CANCEL_CAM_SYNC)){
						tempIntent = new Intent(this, CameraSyncService.class);
						tempIntent.setAction(CameraSyncService.ACTION_CANCEL);
						title = getString(R.string.cam_sync_syncing);
						text = getString(R.string.cam_sync_cancel_sync);
					}
					
					final Intent cancelIntent = tempIntent;
					AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
					builder.setTitle(title);
		            builder.setMessage(text);
					builder.setPositiveButton(getString(R.string.general_yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									if (tFLol != null){
										if (tFLol.isVisible()){
											tFLol.setNoActiveTransfers();
											tranfersPaused = false;
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
    			else if (intent.getAction().equals(ACTION_SHOW_TRANSFERS)){
    				log("intent show transfers");
    				selectDrawerItemLollipop(DrawerItem.TRANSFERS);
    			}
    			else if (intent.getAction().equals(ACTION_TAKE_SELFIE)){
    				log("Intent take selfie");
    				takePicture();
    			}
    			intent.setAction(null);
				setIntent(null);
    		}
    	}    	
    	
    	if (nV != null){
    		switch(drawerItem){
	    		case CLOUD_DRIVE:{
	    			log("onResume - case CLOUD DRIVE");
	    			if(fbFLol!=null){
	    				fbFLol.notifyDataSetChanged();	    				
	    			}
	    			break;
	    		}
	    		case SHARED_ITEMS:{
	    			log("onResume - case SHARED ITEMS");
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
		    		break;	
	    		}
    		}
    	}
	}
	
	@Override
	protected void onPostResume() {
		log("onPostResume");
	    super.onPostResume();
	    if (isSearching){	    	
			selectDrawerItemLollipop(DrawerItem.SEARCH);       		
    		isSearching = false;
	    } 
	}
	
	@Override
	protected void onPause() {
    	log("onPause");
    	managerActivity = null;
    	super.onPause();
    }
	
	@Override
    protected void onDestroy(){
		log("onDestroy()");

    	if (megaApi.getRootNode() != null){
    		megaApi.removeGlobalListener(this);
    		megaApi.removeTransferListener(this);
    		megaApi.removeRequestListener(this);
    	} 
    	
    	super.onDestroy();
	}
	
	@SuppressLint("NewApi")
	public void selectDrawerItemLollipop(DrawerItem item){
    	log("selectDrawerItemLollipop");
    	
    	switch (item){
    		case CLOUD_DRIVE:{
    			mTabHostContacts.setVisibility(View.GONE);    			
    			viewPagerContacts.setVisibility(View.GONE); 
    			mTabHostShares.setVisibility(View.GONE);
    			viewPagerShares.setVisibility(View.GONE);
    			
    			Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    			if (currentFragment != null){
    				getSupportFragmentManager().beginTransaction().remove(currentFragment).commit();
    			}
    			
    			if (mTabsAdapterCDrive == null){
    				log("mTabsAdapterCloudDrive == null");
    				mTabHostCDrive.setVisibility(View.VISIBLE);    			
        			viewPagerCDrive.setVisibility(View.VISIBLE);
        	        mTabHostCDrive.getTabWidget().setDividerDrawable(null);
    				
    				mTabsAdapterCDrive= new TabsAdapter(this, mTabHostCDrive, viewPagerCDrive);   	
    				
        			TabHost.TabSpec tabSpec5 = mTabHostCDrive.newTabSpec("fbFLol");
        			String titleTab5 = getString(R.string.section_cloud_drive);
        			tabSpec5.setIndicator(getTabIndicator(mTabHostCDrive.getContext(), titleTab5.toUpperCase(Locale.getDefault()))); // new function to inject our own tab layout
        	        TabHost.TabSpec tabSpec6 = mTabHostCDrive.newTabSpec("rBFLol");
        	        String titleTab6 = getString(R.string.section_rubbish_bin);
        	        tabSpec6.setIndicator(getTabIndicator(mTabHostCDrive.getContext(), titleTab6.toUpperCase(Locale.getDefault()))); // new function to inject our own tab layout   	                      	   
        	        
        	        mTabsAdapterCDrive.addTab(tabSpec5, FileBrowserFragmentLollipop.class, null);
        	        mTabsAdapterCDrive.addTab(tabSpec6, RubbishBinFragmentLollipop.class, null);
        	        
        	        viewPagerCDrive.setCurrentItem(0);
        	        log("aB.setTitle1");
        	        aB.setTitle(getResources().getString(R.string.section_cloud_drive));
        	        firstNavigationLevel = true;
        	        
        			textViewBrowser = (TextView) mTabHostCDrive.getTabWidget().getChildAt(0).findViewById(R.id.textView); 
//        			textViewBrowser.setBackgroundColor(R.color.tab_text_color);
        			textViewRubbish = (TextView) mTabHostCDrive.getTabWidget().getChildAt(1).findViewById(R.id.textView); 
        			textViewBrowser.setTypeface(null, Typeface.BOLD);
    				textViewRubbish.setTypeface(null, Typeface.NORMAL); 
    				
    			}
    			else{
    				log("mTabsAdapterCloudDrive NOT null");
        			mTabHostCDrive.setVisibility(View.VISIBLE);    			
        			viewPagerCDrive.setVisibility(View.VISIBLE);
    				
    				fbFLol.setIsList(isListCloudDrive);
    				fbFLol.setParentHandle(parentHandleBrowser);
    				fbFLol.setOrder(orderGetChildren);
    				//Check viewPager to determine the tab shown
    				
    				if(viewPagerCDrive!=null){
    					int index = viewPagerCDrive.getCurrentItem();
            			log("Fragment Index: " + index);
            			if(index == 1){
            				//Rubbish Bin TAB
            				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleRubbish);
        					if (parentNode != null){
        						if (parentNode.getHandle() == megaApi.getRubbishNode().getHandle()){
        							log("aB.setTitle3");
        							aB.setTitle(getString(R.string.section_rubbish_bin));
        							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
        							firstNavigationLevel = true;
        						}
        						else{
        							aB.setTitle(parentNode.getName());
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
        					ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderGetChildren);
        					rbFLol.setNodes(nodes);
            			}
            			else{
            				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
        					if (parentNode != null){
        						if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
        							log("aB.setTitle3");
        							aB.setTitle(getString(R.string.section_cloud_drive));
        							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
        							firstNavigationLevel = true;
        						}
        						else{
        							aB.setTitle(parentNode.getName());
        							aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        							firstNavigationLevel = false;
        						}
        					}
        					else{
        						parentHandleBrowser = megaApi.getRootNode().getHandle();
        						parentNode = megaApi.getRootNode();
        						log("aB.setTitle4");
        						aB.setTitle(getString(R.string.section_cloud_drive));
        						aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
        						firstNavigationLevel = true;
        					}
        					ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderGetChildren);
        					fbFLol.setNodes(nodes);
            			}
    				}
    				else{
    					MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
    					if (parentNode != null){
    						if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
    							log("aB.setTitle3");
    							aB.setTitle(getString(R.string.section_cloud_drive));
    							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
    							firstNavigationLevel = true;
    						}
    						else{
    							aB.setTitle(parentNode.getName());
    							aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
    							firstNavigationLevel = false;
    						}
    					}
    					else{
    						parentHandleBrowser = megaApi.getRootNode().getHandle();
    						parentNode = megaApi.getRootNode();
    						log("aB.setTitle4");
    						aB.setTitle(getString(R.string.section_cloud_drive));
    						aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
    						firstNavigationLevel = true;
    					}
    					ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderGetChildren);
    					fbFLol.setNodes(nodes);
    				}    				
    			} 
    			
    			mTabHostCDrive.setOnTabChangedListener(new OnTabChangeListener(){
                    @Override
                    public void onTabChanged(String tabId) {
                    	log("TabId :"+ tabId);
                    	supportInvalidateOptionsMenu();
                        if(tabId.compareTo("fbFLol") == 0){                         	
                        	String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);		
            				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
                			if (fbFLol != null){ 
                				textViewBrowser.setTypeface(null, Typeface.BOLD);
                				textViewRubbish.setTypeface(null, Typeface.NORMAL);
                				log("parentHandleCloud: "+ parentHandleBrowser);
                				if(parentHandleBrowser==megaApi.getRootNode().getHandle()||parentHandleBrowser==-1){
                					log("aB.setTitle2");
                					aB.setTitle(getResources().getString(R.string.section_cloud_drive));
                					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
                					fbFLol.setNodes(megaApi.getChildren(megaApi.getRootNode(), orderGetChildren));
                					firstNavigationLevel = true;
                				}
                				else {
	                				MegaNode node = megaApi.getNodeByHandle(parentHandleBrowser);
	            					aB.setTitle(node.getName());
	            					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
	            					fbFLol.setNodes(megaApi.getChildren(node, orderGetChildren));
	            					firstNavigationLevel = false;
            					}          					   				
                			}
                        }
                        else if(tabId.compareTo("rBFLol") == 0){                        	
                        	String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);		
                        	rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);                			
                        	if (rbFLol != null){  
                        		textViewBrowser.setTypeface(null, Typeface.NORMAL);
                				textViewRubbish.setTypeface(null, Typeface.BOLD);
                        		log("parentHandleRubbish: "+ parentHandleRubbish);
                        		if(parentHandleRubbish == megaApi.getRubbishNode().getHandle() || parentHandleRubbish == -1){
                        			aB.setTitle(getResources().getString(R.string.section_rubbish_bin));
                        			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
                        			rbFLol.setNodes(megaApi.getChildren(megaApi.getRubbishNode(), orderGetChildren));
                    				firstNavigationLevel = true;
                        		}
                        		else{                        			
                        			MegaNode node = megaApi.getNodeByHandle(parentHandleRubbish);
                					aB.setTitle(node.getName());
                					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
                					rbFLol.setNodes(megaApi.getChildren(node, orderGetChildren));
                					firstNavigationLevel = false;
            					}		
                			}                           	
                                          	
                        }
                     }
    			});
    			
    			for (int i=0;i<mTabsAdapterCDrive.getCount();i++){
					final int index = i;
					mTabHostCDrive.getTabWidget().getChildAt(i).setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							viewPagerCDrive.setCurrentItem(index);	
						}
					});
				}
    			
    			if (!firstTime){
    				drawerLayout.closeDrawer(Gravity.LEFT);
    			}
    			else{
    				drawerLayout.openDrawer(Gravity.LEFT);
    				firstTime = false;
    			}
    			
    			viewPagerContacts.setVisibility(View.GONE);
    			
    			//OncreateOptionsMenu
    			int index = viewPagerCDrive.getCurrentItem();
    			log("Fragment Index: " + index);
    			if(index == 1){
    				if (rbFLol != null){	
    					if (createFolderMenuItem != null){
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
	    					createFolderMenuItem.setVisible(false);
	    	    			addMenuItem.setVisible(false);
	    	    			addContactMenuItem.setVisible(false);
	    	    			upgradeAccountMenuItem.setVisible(false);
	    	    			unSelectMenuItem.setVisible(false);
	    	    			addMenuItem.setEnabled(false);
	    	    			changePass.setVisible(false); 
	    	    			exportMK.setVisible(false); 
	    	    			removeMK.setVisible(false); 
	    	    			importLinkMenuItem.setVisible(false);
	    	    			takePicture.setVisible(false);
	    	    			refreshMenuItem.setVisible(false);
	    					helpMenuItem.setVisible(false);
	    					settingsMenuItem.setVisible(false);
	    					gridSmallLargeMenuItem.setVisible(false);
	    					cancelAllTransfersMenuItem.setVisible(false);
	    	    			
	    	    			if (isListRubbishBin){	
	    	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
	    					}
	    					else{
	    						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    	    			}
	
	    					rbFLol.setIsList(isListRubbishBin);	        			
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
	    				}
    				}
    			}
    			else{
    				if (fbFLol!=null){
    					if (createFolderMenuItem != null){
    					//Cloud Drive
    					//Show
	    					addMenuItem.setEnabled(true);
	    					addMenuItem.setVisible(true);
	    					createFolderMenuItem.setVisible(true);				
	    					sortByMenuItem.setVisible(true);
	    					thumbViewMenuItem.setVisible(true);
	    					rubbishBinMenuItem.setVisible(false);				
	    	    			upgradeAccountMenuItem.setVisible(false);    			
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
	    	    			exportMK.setVisible(false); 
	    	    			removeMK.setVisible(false); 
	    	    			refreshMenuItem.setVisible(false);
	    					helpMenuItem.setVisible(false);
	    					settingsMenuItem.setVisible(false);
	    					killAllSessions.setVisible(false);		
	    					gridSmallLargeMenuItem.setVisible(false);
	    					cancelAllTransfersMenuItem.setVisible(false);
	
	    					if(fbFLol.getItemCount()>0){
	    						selectMenuItem.setVisible(true);
	    					}
	    					else{
	    						selectMenuItem.setVisible(true);
	    					}
	    	    			
	    	    			if (isListCloudDrive){	
	    	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
	    					}
	    					else{
	    						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    	    			}   	    			
	
	    				}
    				}
    			}
    			
    			break;
    		}
    		case SAVED_FOR_OFFLINE:{
    			
    			if (oFLol == null){
    				oFLol = new OfflineFragmentLollipop();
    				oFLol.setIsList(isListOffline);
    				oFLol.setPathNavigation("/");
    			}
    			else{
    				oFLol.setPathNavigation("/");
    				oFLol.setIsList(isListOffline);
    			}
    			
    			mTabHostCDrive.setVisibility(View.GONE);    			
    			viewPagerCDrive.setVisibility(View.GONE);
    			mTabHostContacts.setVisibility(View.GONE);    			
    			viewPagerContacts.setVisibility(View.GONE); 
    			mTabHostShares.setVisibility(View.GONE);    			
    			mTabHostShares.setVisibility(View.GONE);
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, oFLol, "oFLol");
    			ft.commit();
    			
    			drawerLayout.closeDrawer(Gravity.LEFT);    			

    			if (createFolderMenuItem != null){
	    			createFolderMenuItem.setVisible(false);
	    			addMenuItem.setVisible(false);
	    			sortByMenuItem.setVisible(false);
	    			upgradeAccountMenuItem.setVisible(false);
	    			selectMenuItem.setVisible(true);
	    			unSelectMenuItem.setVisible(false);
	    			addMenuItem.setEnabled(false);
	    			createFolderMenuItem.setEnabled(false);
	    			changePass.setVisible(false); 
	    			exportMK.setVisible(false); 
	    			removeMK.setVisible(false); 
	    			if (isListOffline){	
	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    			}
	    			rubbishBinMenuItem.setVisible(false);
	    			clearRubbishBinMenuitem.setVisible(false);
        			settingsMenuItem.setVisible(false);
    				refreshMenuItem.setVisible(false);
    				cancelAllTransfersMenuItem.setVisible(false);
    				helpMenuItem.setVisible(false);
    				gridSmallLargeMenuItem.setVisible(false);
    				searchMenuItem.setVisible(true);
    				thumbViewMenuItem.setVisible(true);
    			}
    			
    			break;
    		}
    		case CAMERA_UPLOADS:{
    			if (cuF == null){
    				cuF = new CameraUploadFragmentLollipop();
    				cuF.setIsList(isListCameraUploads);
    				cuF.setIsLargeGrid(isLargeGridCameraUploads);
    				cuF.setFirstTimeCam(firstTimeCam);
				}
				else{
					cuF.setIsList(isListCameraUploads);
					cuF.setIsLargeGrid(isLargeGridCameraUploads);
					cuF.setFirstTimeCam(firstTimeCam);
				}
				
				
    			mTabHostCDrive.setVisibility(View.GONE);
    			viewPagerCDrive.setVisibility(View.GONE);
    			mTabHostContacts.setVisibility(View.GONE);    			
    			viewPagerContacts.setVisibility(View.GONE); 
    			mTabHostShares.setVisibility(View.GONE);    			
    			mTabHostShares.setVisibility(View.GONE);
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, cuF, "cuFLol");
    			ft.commit();
    			
    			
    			firstTimeCam = false;
    			
				drawerLayout.closeDrawer(Gravity.LEFT);
    			
    			if (createFolderMenuItem != null){
	    			createFolderMenuItem.setVisible(false);
	    			addMenuItem.setVisible(false);
	    			sortByMenuItem.setVisible(false);
	    			upgradeAccountMenuItem.setVisible(false);
	    			selectMenuItem.setVisible(false);
	    			unSelectMenuItem.setVisible(false);
	    			thumbViewMenuItem.setVisible(true);
	    			addMenuItem.setEnabled(false);
	    			createFolderMenuItem.setEnabled(false);
	    			changePass.setVisible(false); 
	    			exportMK.setVisible(false); 
	    			removeMK.setVisible(false); 
        			settingsMenuItem.setVisible(false);
    				refreshMenuItem.setVisible(false);
    				cancelAllTransfersMenuItem.setVisible(false);
    				helpMenuItem.setVisible(false);
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
	    			rubbishBinMenuItem.setVisible(false);
	    			clearRubbishBinMenuitem.setVisible(false);	    			
    			}
      			break;
    		}
    		case INBOX:{
    			if (iFLol == null){
    				iFLol = new InboxFragmentLollipop();
    				iFLol.setParentHandle(megaApi.getInboxNode().getHandle());
    				parentHandleInbox = megaApi.getInboxNode().getHandle();
    				iFLol.setIsList(isListInbox);
    				iFLol.setOrder(orderGetChildren);
    				ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getInboxNode(), orderGetChildren);
    				iFLol.setNodes(nodes);
    			}
    			else{
    				iFLol.setIsList(isListInbox);
    				iFLol.setParentHandle(parentHandleInbox);
    				iFLol.setOrder(orderGetChildren);
    				ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleInbox), orderGetChildren);
    				iFLol.setNodes(nodes);
    			}
    			    
    			mTabHostCDrive.setVisibility(View.GONE);    			
    			viewPagerCDrive.setVisibility(View.GONE);
    			mTabHostContacts.setVisibility(View.GONE);    			
    			viewPagerContacts.setVisibility(View.GONE); 
    			mTabHostShares.setVisibility(View.GONE);    			
    			viewPagerShares.setVisibility(View.GONE);
    			
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, iFLol, "iFLol");
    			ft.commit();
    			
    			viewPagerContacts.setVisibility(View.GONE);
    			drawerLayout.closeDrawer(Gravity.LEFT);
    			
    			if (createFolderMenuItem != null){
    				//Show				
        			sortByMenuItem.setVisible(true);
        			if(iFLol.getItemCount()>0){
						selectMenuItem.setVisible(true);
					}
					else{
						selectMenuItem.setVisible(true);
					}
        			searchMenuItem.setVisible(true);
        			thumbViewMenuItem.setVisible(true);
        			
    				//Hide
        			refreshMenuItem.setVisible(false);
        			pauseTransfersMenuIcon.setVisible(false);
					playTransfersMenuIcon.setVisible(false);
    				createFolderMenuItem.setVisible(false);
        			addMenuItem.setVisible(false);
        			addContactMenuItem.setVisible(false);        			
        			unSelectMenuItem.setVisible(false);
        			addMenuItem.setEnabled(false);
        			changePass.setVisible(false); 
        			exportMK.setVisible(false); 
        			removeMK.setVisible(false); 
        			importLinkMenuItem.setVisible(false);
        			takePicture.setVisible(false);
        			refreshMenuItem.setVisible(false);
    				helpMenuItem.setVisible(false);
    				settingsMenuItem.setVisible(false);
        			clearRubbishBinMenuitem.setVisible(false);
        			rubbishBinMenuItem.setVisible(false);
        			upgradeAccountMenuItem.setVisible(false);
        			gridSmallLargeMenuItem.setVisible(false);
        			cancelAllTransfersMenuItem.setVisible(false);
        			
        			if (isListInbox){	
	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    			}
	    		}

    			break;
    		}
    		case SHARED_ITEMS:{    			    			
    			if (aB == null){
    				aB = getSupportActionBar();
    			}
    			log("aB.setTitle SHARED_ITEMS");
    			aB.setTitle(getString(R.string.section_shared_items));
    			
    			mTabHostContacts.setVisibility(View.GONE);    			
    			viewPagerContacts.setVisibility(View.GONE); 
    			mTabHostCDrive.setVisibility(View.GONE);    			
    			viewPagerCDrive.setVisibility(View.GONE);
    			
    			Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    			if (currentFragment != null){
    				getSupportFragmentManager().beginTransaction().remove(currentFragment).commit();
    			}
    			    			
    			if (mTabsAdapterShares == null){
    				mTabHostShares.setVisibility(View.VISIBLE);    			
        			viewPagerShares.setVisibility(View.VISIBLE);
    				mTabsAdapterShares= new TabsAdapter(this, mTabHostShares, viewPagerShares);   
    				mTabHostShares.getTabWidget().setDividerDrawable(null);
    				
        			TabHost.TabSpec tabSpec3 = mTabHostShares.newTabSpec("incomingSharesFragment");
        			String titleTab3 = getString(R.string.tab_incoming_shares);
        			tabSpec3.setIndicator(getTabIndicator(mTabHostShares.getContext(), titleTab3.toUpperCase(Locale.getDefault()))); // new function to inject our own tab layout  			
         			
        	        TabHost.TabSpec tabSpec4 = mTabHostShares.newTabSpec("outgoingSharesFragment");
        	        String titleTab4 = getString(R.string.tab_outgoing_shares);
        	        tabSpec4.setIndicator(getTabIndicator(mTabHostShares.getContext(), titleTab4.toUpperCase(Locale.getDefault()))); // new function to inject our own tab layout
        	                	          				
    				mTabsAdapterShares.addTab(tabSpec3, IncomingSharesFragmentLollipop.class, null);
    				mTabsAdapterShares.addTab(tabSpec4, OutgoingSharesFragmentLollipop.class, null); 
    				
    				viewPagerShares.setCurrentItem(0);
        			textViewIncoming = (TextView) mTabHostShares.getTabWidget().getChildAt(0).findViewById(R.id.textView); 
        			textViewOutgoing = (TextView) mTabHostShares.getTabWidget().getChildAt(1).findViewById(R.id.textView); 
        			textViewIncoming.setTypeface(null, Typeface.BOLD);
        			textViewOutgoing.setTypeface(null, Typeface.NORMAL);
    			}
    			else{
    				log("mTabsAdapterShares NOT null");
    				mTabHostShares.setVisibility(View.VISIBLE);    			
        			viewPagerShares.setVisibility(View.VISIBLE);
        			
        			textViewIncoming = (TextView) mTabHostShares.getTabWidget().getChildAt(0).findViewById(R.id.textView); 
        			textViewOutgoing = (TextView) mTabHostShares.getTabWidget().getChildAt(1).findViewById(R.id.textView);        			
        			
        			int index = viewPagerShares.getCurrentItem();
        			if(index==0){	
        				textViewOutgoing.setTypeface(null, Typeface.NORMAL);
        				textViewIncoming.setTypeface(null, Typeface.BOLD);
        				String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 0);		
        				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
        				if (inSFLol != null){
                			inSFLol.setParentHandle(parentHandleIncoming);
                			inSFLol.setOrder(orderGetChildren);
                			MegaNode node = megaApi.getNodeByHandle(parentHandleIncoming);
        					if (node != null){
        						inSFLol.setNodes(megaApi.getChildren(node, orderGetChildren));
        						aB.setTitle(node.getName());
            					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
            					firstNavigationLevel = false;
        					}
        					else{
        						inSFLol.refresh();
        						aB.setTitle(getResources().getString(R.string.section_shared_items));
            					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
            					firstNavigationLevel = true;
        					}
        				}
        			}
        			else{
        				textViewOutgoing.setTypeface(null, Typeface.BOLD);
        				textViewIncoming.setTypeface(null, Typeface.NORMAL);
        				String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 1);		
        				outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
        				if (outSFLol != null){
        					outSFLol.setParentHandle(parentHandleOutgoing);
        					outSFLol.setOrder(orderGetChildren);
//        					outSFLol.refresh(parentHandleIncoming);
        					MegaNode node = megaApi.getNodeByHandle(parentHandleOutgoing);
        					if (node != null){
        						outSFLol.setNodes(megaApi.getChildren(node, orderGetChildren));
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
    			
    			mTabHostShares.setVisibility(View.VISIBLE);
    			
    			mTabHostShares.setOnTabChangedListener(new OnTabChangeListener(){
    				@Override
                    public void onTabChanged(String tabId) {
                    	log("TabId :"+ tabId);
                    	supportInvalidateOptionsMenu();
                        if(tabId.compareTo("outgoingSharesFragment") == 0){
                        	String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 1);		
            				outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
                			if (outSFLol != null){    
                				textViewOutgoing.setTypeface(null, Typeface.BOLD);
                				textViewIncoming.setTypeface(null, Typeface.NORMAL);
            					
            					if(parentHandleOutgoing!=-1){
	                				MegaNode node = megaApi.getNodeByHandle(parentHandleOutgoing);
	            					aB.setTitle(node.getName());
	            					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
	            					firstNavigationLevel = false;
	            					outSFLol.setNodes(megaApi.getChildren(node, orderGetChildren));
            					}
                				else{
                					aB.setTitle(getResources().getString(R.string.section_shared_items));
                					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
                					firstNavigationLevel = true;
                				}
                			}
                			else{
                				log("outSFLol == null");
                			}
                        }
                        else if(tabId.compareTo("incomingSharesFragment") == 0){         
                        	String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 0);		
            				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
                        	if (inSFLol != null){    
                        		textViewOutgoing.setTypeface(null, Typeface.NORMAL);
                				textViewIncoming.setTypeface(null, Typeface.BOLD);
            					
            					if(parentHandleIncoming!=-1){
                        			MegaNode node = megaApi.getNodeByHandle(parentHandleIncoming);
                					aB.setTitle(node.getName());	
                					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
	            					firstNavigationLevel = false;
	            					inSFLol.setNodes(megaApi.getChildren(node, orderGetChildren));
            					}
                				else{
                					aB.setTitle(getResources().getString(R.string.section_shared_items));
                					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
                					firstNavigationLevel = true;
                				}
                			}   
                        	else{
                        		log("inSFLol == null");
                        	}
                        }
                     }
    			});
    			
				for (int i=0;i<mTabsAdapterShares.getCount();i++){
					final int index = i;
					mTabHostShares.getTabWidget().getChildAt(i).setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							viewPagerShares.setCurrentItem(index);	
						}
					});
				}
   			
    			drawerLayout.closeDrawer(Gravity.LEFT);
    			
    			//onCreateOptionsMenu
    			int index = viewPagerShares.getCurrentItem();
    			if(index==0){	
    				String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 0);		
    				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);
    				if (inSFLol != null){
    					sortByMenuItem.setVisible(true);
    					thumbViewMenuItem.setVisible(true); 

    					addMenuItem.setEnabled(true);
    					addMenuItem.setVisible(true);

    					log("parentHandleIncoming: "+parentHandleIncoming);
    					if(parentHandleIncoming==-1){
    						addMenuItem.setVisible(false);
    					}
    					else{
    						addMenuItem.setVisible(true);
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
    					createFolderMenuItem.setVisible(false);
    					addContactMenuItem.setVisible(false);
    					unSelectMenuItem.setVisible(false);  				
    					rubbishBinMenuItem.setVisible(false);
    					createFolderMenuItem.setVisible(false);
    					rubbishBinMenuItem.setVisible(false);
    					clearRubbishBinMenuitem.setVisible(false);
    					changePass.setVisible(false); 
    					exportMK.setVisible(false); 
    					removeMK.setVisible(false); 
    					importLinkMenuItem.setVisible(false);
    					takePicture.setVisible(false);					
    	    			refreshMenuItem.setVisible(false);
    					helpMenuItem.setVisible(false);
    					settingsMenuItem.setVisible(false);
    					upgradeAccountMenuItem.setVisible(false);
    					gridSmallLargeMenuItem.setVisible(false);
    					cancelAllTransfersMenuItem.setVisible(false);
    					
    					if (isListIncoming){	
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
    					}
    					else{
    						addMenuItem.setVisible(true);
    					}
    					
    					if(outSFLol.getItemCount()>0){
    						selectMenuItem.setVisible(true);
    					}
    					else{
    						selectMenuItem.setVisible(false);
    					}
    					searchMenuItem.setVisible(true);
    					
    					//Hide
    					upgradeAccountMenuItem.setVisible(false);
    					pauseTransfersMenuIcon.setVisible(false);
    					playTransfersMenuIcon.setVisible(false);
    					createFolderMenuItem.setVisible(false);
    					addContactMenuItem.setVisible(false);
    					unSelectMenuItem.setVisible(false);  				
    					rubbishBinMenuItem.setVisible(false);
    					createFolderMenuItem.setVisible(false);
    					rubbishBinMenuItem.setVisible(false);
    					clearRubbishBinMenuitem.setVisible(false);
    					changePass.setVisible(false); 
    					exportMK.setVisible(false); 
    					removeMK.setVisible(false); 
    					importLinkMenuItem.setVisible(false);
    					takePicture.setVisible(false);					
    	    			refreshMenuItem.setVisible(false);
    					helpMenuItem.setVisible(false);
    					settingsMenuItem.setVisible(false);
    					gridSmallLargeMenuItem.setVisible(false);
    					cancelAllTransfersMenuItem.setVisible(false);
    					
    					if (isListOutgoing){	
    	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
    					}
    					else{
    						thumbViewMenuItem.setTitle(getString(R.string.action_list));
    	    			}
    				}
    			}   			
//    			String sharesTag = getFragmentTag(R.id.shares_tabs_pager, 0);		
//				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);    			
//    			if (inSFLol != null){
//    				aB.setTitle(getString(R.string.section_shared_items));	
//    				inSFLol.refresh();			
//    				
//    			} 
//    			sharesTag = getFragmentTag(R.id.shares_tabs_pager, 1);		
//        		outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sharesTag);	
//    			if (outSFLol != null){    				
//					aB.setTitle(getString(R.string.section_shared_items));				
//					outSFLol.refresh();    				
//    			}
    			
    			break;
    		}
    		case CONTACTS:{
      			
    			if (aB == null){
    				aB = getSupportActionBar();
    			}
    			aB.setTitle(getString(R.string.section_contacts));
    			
    			mTabHostShares.setVisibility(View.GONE);    			
    			mTabHostShares.setVisibility(View.GONE);
    			mTabHostCDrive.setVisibility(View.GONE);    			
    			viewPagerCDrive.setVisibility(View.GONE);
    			
    			Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    			if (currentFragment != null){
    				getSupportFragmentManager().beginTransaction().remove(currentFragment).commit();
    			}
    			mTabHostContacts.setVisibility(View.VISIBLE);    			
    			viewPagerContacts.setVisibility(View.VISIBLE);
    			    			    			
    			if (mTabsAdapterContacts == null){
    				mTabsAdapterContacts = new TabsAdapter(this, mTabHostContacts, viewPagerContacts);   	
    				
        			TabHost.TabSpec tabSpec1 = mTabHostContacts.newTabSpec("contactsFragment");
        	        tabSpec1.setIndicator(getTabIndicator(mTabHostContacts.getContext(), getString(R.string.tab_contacts))); // new function to inject our own tab layout
        	        //tabSpec.setContent(contentID);
        	        //mTabHostContacts.addTab(tabSpec);
        	        TabHost.TabSpec tabSpec2 = mTabHostContacts.newTabSpec("sentRequests");
        	        tabSpec2.setIndicator(getTabIndicator(mTabHostContacts.getContext(), getString(R.string.tab_sent_requests))); // new function to inject our own tab layout
       	        
        	        TabHost.TabSpec tabSpec3 = mTabHostContacts.newTabSpec("receivedRequests");
        	        tabSpec3.setIndicator(getTabIndicator(mTabHostContacts.getContext(), getString(R.string.tab_received_requests))); // new function to inject our own tab layout
    				
    				mTabsAdapterContacts.addTab(tabSpec1, ContactsFragmentLollipop.class, null);
    				mTabsAdapterContacts.addTab(tabSpec2, SentRequestsFragmentLollipop.class, null);
    				mTabsAdapterContacts.addTab(tabSpec3, ReceivedRequestsFragmentLollipop.class, null);
    			}		
    			
    			mTabHostContacts.setOnTabChangedListener(new OnTabChangeListener(){
                    @Override
                    public void onTabChanged(String tabId) {
                    	managerActivity.supportInvalidateOptionsMenu();
                    }
    			});
    			
    			for (int i=0;i<mTabsAdapterContacts.getCount();i++){
    				final int index = i;
    				mTabHostContacts.getTabWidget().getChildAt(i).setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							viewPagerContacts.setCurrentItem(index);
						}
					});
    			}
    			
    			drawerLayout.closeDrawer(Gravity.LEFT);

    			if (createFolderMenuItem != null){
    				changePass.setVisible(false); 
        			exportMK.setVisible(false); 
        			removeMK.setVisible(false); 
    				createFolderMenuItem.setVisible(false);
    				addContactMenuItem.setVisible(true);
	    			addMenuItem.setVisible(false);
	    			refreshMenuItem.setVisible(false);
	    			sortByMenuItem.setVisible(true);
	    			helpMenuItem.setVisible(false);
	    			upgradeAccountMenuItem.setVisible(false);
	    			settingsMenuItem.setVisible(false);
	    			selectMenuItem.setVisible(true);
	    			unSelectMenuItem.setVisible(false);
	    			thumbViewMenuItem.setVisible(true);
	    			addMenuItem.setEnabled(false);	
	    			rubbishBinMenuItem.setVisible(false);
	    			clearRubbishBinMenuitem.setVisible(false);
	    			cancelAllTransfersMenuItem.setVisible(false);
	    			
	    			if (isListContacts){	
	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    			}
	    			searchMenuItem.setVisible(true);
	    			gridSmallLargeMenuItem.setVisible(false);
    			}
    			break;
    		}
    		case SETTINGS:{
    			
    			drawerLayout.closeDrawer(Gravity.LEFT);
    			aB.setTitle(getString(R.string.action_settings));
//    			startActivity(new Intent(this, SettingsActivity.class));
    			mTabHostContacts.setVisibility(View.GONE);    			
    			viewPagerContacts.setVisibility(View.GONE); 
    			mTabHostShares.setVisibility(View.GONE);    			
    			mTabHostShares.setVisibility(View.GONE);
    			mTabHostCDrive.setVisibility(View.GONE);    			
    			viewPagerCDrive.setVisibility(View.GONE);
    			
    			Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    			if (currentFragment != null){
    				getSupportFragmentManager().beginTransaction().remove(currentFragment).commit();
    			}
    			
    			if(sttFLol==null){
    				sttFLol = new SettingsFragmentLollipop();
    			} 
    			
    			android.app.FragmentTransaction ft = getFragmentManager().beginTransaction();
    			ft.replace(R.id.fragment_container, sttFLol, "sttF");
    			ft.commit();
    			
//    			drawerItem = lastDrawerItem;
//    			selectDrawerItemLollipop(drawerItem);
    			
    			break;
    		}
    		case SEARCH:{
    					
    			if (sFLol == null){
    				sFLol = new SearchFragmentLollipop();
        		}
    			
				if (nV != null){
					Menu nVMenu = nV.getMenu();
					MenuItem hidden = nVMenu.findItem(R.id.navigation_item_hidden);
					resetNavigationViewMenu(nVMenu);
					hidden.setChecked(true);
				}
    			
    			searchNodes = megaApi.search(megaApi.getRootNode(), searchQuery, true);
    			
    			drawerItem = DrawerItem.SEARCH;
    			
    			sFLol.setSearchNodes(searchNodes);
    			sFLol.setNodes(searchNodes);
    			sFLol.setSearchQuery(searchQuery);
    			sFLol.setParentHandle(parentHandleSearch);
    			sFLol.setLevels(levelsSearch);
    			
    			mTabHostCDrive.setVisibility(View.GONE);    			
    			viewPagerCDrive.setVisibility(View.GONE);
    			mTabHostContacts.setVisibility(View.GONE);    			
    			viewPagerContacts.setVisibility(View.GONE); 
    			mTabHostShares.setVisibility(View.GONE);    			
    			mTabHostShares.setVisibility(View.GONE);
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, sFLol, "sFLol");
    			ft.commit();
    			
    			if (createFolderMenuItem != null){
    				searchMenuItem.setVisible(true);
        			createFolderMenuItem.setVisible(false);
        			addMenuItem.setVisible(false);
        			sortByMenuItem.setVisible(false);
        			upgradeAccountMenuItem.setVisible(false);
	    			selectMenuItem.setVisible(true);
	    			unSelectMenuItem.setVisible(false);
	    			thumbViewMenuItem.setVisible(true);
        			addMenuItem.setEnabled(true);
        			rubbishBinMenuItem.setVisible(false); 
        			clearRubbishBinMenuitem.setVisible(false);
        			changePass.setVisible(false); 
        			exportMK.setVisible(false); 
        			removeMK.setVisible(false); 
        			settingsMenuItem.setVisible(false);
    				refreshMenuItem.setVisible(false);
    				helpMenuItem.setVisible(false);
    				gridSmallLargeMenuItem.setVisible(false);
    				cancelAllTransfersMenuItem.setVisible(false);
    			}
    			break;
    		}
    		case ACCOUNT:{
    			
    			Intent myAccountIntent = new Intent(this, MyAccountMainActivityLollipop.class);
    			startActivity(myAccountIntent);
    			
    			accountFragment=MY_ACCOUNT_FRAGMENT;
    			
    			if (maFLol == null){
    				maFLol = new MyAccountFragmentLollipop();
    			}
    			
    			mTabHostContacts.setVisibility(View.GONE);    			
    			viewPagerContacts.setVisibility(View.GONE); 
    			mTabHostShares.setVisibility(View.GONE);    			
    			mTabHostShares.setVisibility(View.GONE);
    			mTabHostCDrive.setVisibility(View.GONE);    			
    			viewPagerCDrive.setVisibility(View.GONE);
    			
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, maFLol, "maF");
    			ft.commit();
    			
    			drawerLayout.closeDrawer(Gravity.LEFT);
    			
    			if (createFolderMenuItem != null){
    				createFolderMenuItem.setVisible(false);
	    			addMenuItem.setVisible(false);
	    			refreshMenuItem.setVisible(true);
	    			sortByMenuItem.setVisible(false);
	    			helpMenuItem.setVisible(true);
	    			upgradeAccountMenuItem.setVisible(false);
	    			selectMenuItem.setVisible(false);
	    			unSelectMenuItem.setVisible(false);
	    			thumbViewMenuItem.setVisible(false);
	    			changePass.setVisible(true); 
	    			if (numberOfSubscriptions > 0){
	    				cancelSubscription.setVisible(true);
	    			}
	    			killAllSessions.setVisible(true);
	    			
	    			String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
	    			log("Export in: "+path);
	    			File file= new File(path);
	    			if(file.exists()){
	    				exportMK.setVisible(false); 
		    			removeMK.setVisible(true); 
	    			}
	    			else{
	    				exportMK.setVisible(true); 
		    			removeMK.setVisible(false); 		
	    			}
	    			
	    			addMenuItem.setEnabled(false);
	    			createFolderMenuItem.setEnabled(false);
	    			rubbishBinMenuItem.setVisible(false);
	    			clearRubbishBinMenuitem.setVisible(false);
        			settingsMenuItem.setVisible(false);
        			gridSmallLargeMenuItem.setVisible(false);
        			searchMenuItem.setVisible(false);
        			cancelAllTransfersMenuItem.setVisible(false);
	    		}
    			break;
    		}
    		case TRANSFERS:{
				log("select TRANSFERS");
								
				drawerItem = DrawerItem.TRANSFERS;

				if (nV != null){
					Menu nVMenu = nV.getMenu();
					MenuItem hidden = nVMenu.findItem(R.id.navigation_item_hidden);
					resetNavigationViewMenu(nVMenu);
					hidden.setChecked(true);
				}
				
    			mTabHostContacts.setVisibility(View.GONE);    			
    			viewPagerContacts.setVisibility(View.GONE); 
    			mTabHostShares.setVisibility(View.GONE);    			
    			mTabHostShares.setVisibility(View.GONE);
    			mTabHostCDrive.setVisibility(View.GONE);    			
    			viewPagerCDrive.setVisibility(View.GONE);
				
    			if (tFLol == null){
    				tFLol = new TransfersFragmentLollipop();
    			}
    			
    			tFLol.setTransfers(megaApi.getTransfers());
    			tFLol.setPause(tranfersPaused);
				
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, tFLol, "tFLol");
    			ft.commit();
    			
    			drawerLayout.closeDrawer(Gravity.LEFT);
    			if (createFolderMenuItem != null){
        			//Options menu
    				searchMenuItem.setVisible(false);				
    				//Hide
    				createFolderMenuItem.setVisible(false);
    				addContactMenuItem.setVisible(false);
        			addMenuItem.setVisible(false);
        			sortByMenuItem.setVisible(false);
        			selectMenuItem.setVisible(false);
        			unSelectMenuItem.setVisible(false);
        			thumbViewMenuItem.setVisible(false);
        			addMenuItem.setEnabled(false);
        			createFolderMenuItem.setEnabled(false);
        			rubbishBinMenuItem.setVisible(false);
        			clearRubbishBinMenuitem.setVisible(false);
        			importLinkMenuItem.setVisible(false);
        			takePicture.setVisible(false);
    				settingsMenuItem.setVisible(false);
    				refreshMenuItem.setVisible(false);
    				helpMenuItem.setVisible(false);
    				upgradeAccountMenuItem.setVisible(false);
    				changePass.setVisible(false);
    				cancelSubscription.setVisible(false);				
    				killAllSessions.setVisible(false);
    				
    				cancelAllTransfersMenuItem.setVisible(true);
    				
    				if(tranfersPaused){
    					playTransfersMenuIcon.setVisible(true);
    					pauseTransfersMenuIcon.setVisible(false);
    				}
    				else{
    					playTransfersMenuIcon.setVisible(true);
    					pauseTransfersMenuIcon.setVisible(false);
    				}
    			}

    			break;
    		}
    	}
	}
	
	private String getFragmentTag(int viewPagerId, int fragmentPosition){
	     return "android:switcher:" + viewPagerId + ":" + fragmentPosition;
	}
	
	private View getTabIndicator(Context context, String title) {
        View view = LayoutInflater.from(context).inflate(R.layout.tab_layout, null);

        TextView tv = (TextView) view.findViewById(R.id.textView);
        tv.setText(title);
        return view;
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
	
	static public void logout(Context context, MegaApiAndroid megaApi, boolean confirmAccount) {
		logout(context, megaApi, confirmAccount, false);
	}
	
	static public void logout(Context context, MegaApiAndroid megaApi, boolean confirmAccount, boolean logoutBadSession) {
		log("logout");
		
		File offlineDirectory = null;
		if (Environment.getExternalStorageDirectory() != null){
			offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR);
		}
		else{
			offlineDirectory = context.getFilesDir();
		}
		
		try {
			Util.deleteFolderAndSubfolders(context, offlineDirectory);
		} catch (IOException e) {}
		
		File thumbDir = ThumbnailUtils.getThumbFolder(context);
		File previewDir = PreviewUtils.getPreviewFolder(context);
		
		try {
			Util.deleteFolderAndSubfolders(context, thumbDir);
		} catch (IOException e) {}
		
		try {
			Util.deleteFolderAndSubfolders(context, previewDir);
		} catch (IOException e) {}
		
		File externalCacheDir = context.getExternalCacheDir();
		File cacheDir = context.getCacheDir();
		try {
			Util.deleteFolderAndSubfolders(context, externalCacheDir);
		} catch (IOException e) {}
		
		try {
			Util.deleteFolderAndSubfolders(context, cacheDir);
		} catch (IOException e) {}
		
		PackageManager m = context.getPackageManager();
		String s = context.getPackageName();
		try {
		    PackageInfo p = m.getPackageInfo(s, 0);
		    s = p.applicationInfo.dataDir;
		} catch (NameNotFoundException e) {
		    log("Error Package name not found " + e);
		}
		
		File appDir = new File(s);
		
		for (File c : appDir.listFiles()){
			if (c.isFile()){
				c.delete();
			}
		}
		
		Intent cancelTransfersIntent = new Intent(context, DownloadService.class);
		cancelTransfersIntent.setAction(DownloadService.ACTION_CANCEL);
		context.startService(cancelTransfersIntent);
		cancelTransfersIntent = new Intent(context, UploadService.class);
		cancelTransfersIntent.setAction(UploadService.ACTION_CANCEL);
		context.startService(cancelTransfersIntent);
		
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
		dbH.clearCredentials();
		
		if (dbH.getPreferences() != null){
			dbH.clearPreferences();
			dbH.setFirstTime(false);
			Intent stopIntent = null;
			stopIntent = new Intent(context, CameraSyncService.class);
			stopIntent.setAction(CameraSyncService.ACTION_LOGOUT);
			context.startService(stopIntent);
		}
		dbH.clearOffline();
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		if (!logoutBadSession){
			megaApi.logout();
		}
		drawerItem = null;
		
		if (!confirmAccount){		
			if(managerActivity != null)	{
				Intent intent = new Intent(managerActivity, TourActivityLollipop.class);
		        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
				managerActivity.startActivity(intent);
				managerActivity.finish();
				managerActivity = null;
			}
			else{
//				Intent intent = new Intent (context, TourActivityLollipop.class);
//				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
//		        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//				context.startActivity(intent);
//				if (context instanceof Activity){
//					((Activity)context).finish();
//				}
//				context = null;
			}
		}
		else{
			if (managerActivity != null){
				managerActivity.finish();
			}
			else{
				((Activity)context).finish();
			}
		}
	}
	
	public void showpF(int type, ArrayList<Product> accounts, BitSet paymentBitSet){
		showpF(type, accounts, false, paymentBitSet);
	}
	
	public void showpF(int type, ArrayList<Product> accounts, boolean refresh, BitSet paymentBitSet){
		log("showpF");
		
		if (paymentBitSet == null){
			if (this.paymentBitSet != null){
				paymentBitSet = this.paymentBitSet;
			}
		}
		
		accountFragment=PAYMENT_FRAGMENT;
		
		mTabHostContacts.setVisibility(View.GONE);    			
		viewPagerContacts.setVisibility(View.GONE); 
		mTabHostShares.setVisibility(View.GONE);    			
		mTabHostShares.setVisibility(View.GONE);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		if (!refresh){
			if(pF==null){			
				pF = new PaymentFragment();
				pF.setInfo(type, accounts, paymentBitSet);
				ft.replace(R.id.fragment_container, pF, "pF");
				ft.commit();
			}
			else{			
				pF.setInfo(type, accounts, paymentBitSet);			
				ft.replace(R.id.fragment_container, pF, "pF");
				ft.commit();
			}
		}
		else{
			Fragment tempF = getSupportFragmentManager().findFragmentByTag("pF");
			if (tempF != null){
				ft.detach(tempF);
				ft.attach(tempF);
				ft.commit();
			}
			else{
				if(pF==null){			
					pF = new PaymentFragment();
					pF.setInfo(type, accounts, paymentBitSet);
					ft.replace(R.id.fragment_container, pF, "pF");
					ft.commit();
				}
				else{			
					pF.setInfo(type, accounts, paymentBitSet);			
					ft.replace(R.id.fragment_container, pF, "pF");
					ft.commit();
				}
			}
		}
	}
	
	public void showUpAF(BitSet paymentBitSet){
		
//		Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("maF");
//        FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
//        fragTransaction.detach(currentFragment);
//        fragTransaction.commit();
//
//        fragTransaction = getSupportFragmentManager().beginTransaction();
//        fragTransaction.attach(currentFragment);
//        fragTransaction.commit();
		
		if (paymentBitSet == null){
			if (this.paymentBitSet != null){
				paymentBitSet = this.paymentBitSet;
			}
		}
		
		accountFragment=UPGRADE_ACCOUNT_FRAGMENT;
		
		mTabHostContacts.setVisibility(View.GONE);    			
		viewPagerContacts.setVisibility(View.GONE); 
		mTabHostShares.setVisibility(View.GONE);    			
		mTabHostShares.setVisibility(View.GONE);
		
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		if(upAF==null){
			upAF = new UpgradeAccountFragment();
			upAF.setInfo(paymentBitSet);
			ft.replace(R.id.fragment_container, upAF, "upAF");
			ft.commit();
		}
		else{
			upAF.setInfo(paymentBitSet);
			ft.replace(R.id.fragment_container, upAF, "upAF");
			ft.commit();
		}
	}	
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		log("onCreateOptionsMenuLollipop");
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_manager, menu);
	    getSupportActionBar().setDisplayShowCustomEnabled(true);
	    
	    final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchMenuItem = menu.findItem(R.id.action_search);
		final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
		
		if (searchView != null){
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
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
		settingsMenuItem = menu.findItem(R.id.action_menu_settings);
		rubbishBinMenuItem = menu.findItem(R.id.action_rubbish_bin);
		clearRubbishBinMenuitem = menu.findItem(R.id.action_menu_clear_rubbish_bin);
		cancelAllTransfersMenuItem = menu.findItem(R.id.action_menu_cancel_all_transfers);
		playTransfersMenuIcon = menu.findItem(R.id.action_play);
		pauseTransfersMenuIcon = menu.findItem(R.id.action_pause);
		cancelAllTransfersMenuItem.setVisible(false);
		
		changePass = menu.findItem(R.id.action_menu_change_pass);
		exportMK = menu.findItem(R.id.action_menu_export_MK);
		removeMK = menu.findItem(R.id.action_menu_remove_MK);
		
		takePicture = menu.findItem(R.id.action_take_picture);
		
		cancelSubscription = menu.findItem(R.id.action_menu_cancel_subscriptions);
		cancelSubscription.setVisible(false);
		
		killAllSessions = menu.findItem(R.id.action_menu_kill_all_sessions);
		killAllSessions.setVisible(false);		
	    
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
					createFolderMenuItem.setVisible(false);
	    			addMenuItem.setVisible(false);
	    			addContactMenuItem.setVisible(false);
	    			upgradeAccountMenuItem.setVisible(false);
	    			unSelectMenuItem.setVisible(false);
	    			addMenuItem.setEnabled(false);
	    			changePass.setVisible(false); 
	    			exportMK.setVisible(false); 
	    			removeMK.setVisible(false); 
	    			importLinkMenuItem.setVisible(false);
	    			takePicture.setVisible(false);
	    			refreshMenuItem.setVisible(false);
					helpMenuItem.setVisible(false);
					settingsMenuItem.setVisible(false);
	    			
	    			if (isListRubbishBin){	
	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    			}

					rbFLol.setIsList(isListRubbishBin);	        			
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
					//Cloud Drive
					//Show
					addMenuItem.setEnabled(true);
					addMenuItem.setVisible(true);
					createFolderMenuItem.setVisible(true);				
					sortByMenuItem.setVisible(true);
					thumbViewMenuItem.setVisible(true);
					rubbishBinMenuItem.setVisible(false);				
	    			upgradeAccountMenuItem.setVisible(false);    			
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
	    			exportMK.setVisible(false); 
	    			removeMK.setVisible(false); 
	    			refreshMenuItem.setVisible(false);
					helpMenuItem.setVisible(false);
					settingsMenuItem.setVisible(false);
					killAllSessions.setVisible(false);					

					if(fbFLol.getItemCount()>0){
						selectMenuItem.setVisible(true);
					}
					else{
						selectMenuItem.setVisible(false);
					}
	    			
	    			if (isListCloudDrive){	
	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    			}
	    			gridSmallLargeMenuItem.setVisible(false);
				}
			}
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
    			
				//Hide
    			upgradeAccountMenuItem.setVisible(false);
				refreshMenuItem.setVisible(false);
				pauseTransfersMenuIcon.setVisible(false);
				playTransfersMenuIcon.setVisible(false);
				createFolderMenuItem.setVisible(false);
				addContactMenuItem.setVisible(false);
    			addMenuItem.setVisible(false);
    			unSelectMenuItem.setVisible(false);
    			addMenuItem.setEnabled(false);
    			createFolderMenuItem.setEnabled(false);
    			changePass.setVisible(false); 
    			exportMK.setVisible(false); 
    			removeMK.setVisible(false); 
    			rubbishBinMenuItem.setVisible(false);
    			clearRubbishBinMenuitem.setVisible(false);
    			importLinkMenuItem.setVisible(false);
    			takePicture.setVisible(false);					
    			refreshMenuItem.setVisible(false);
				helpMenuItem.setVisible(false);
				settingsMenuItem.setVisible(false);
    			
    			if (isListOffline){	
    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
				}
				else{
					thumbViewMenuItem.setTitle(getString(R.string.action_list));
    			}    	
    			gridSmallLargeMenuItem.setVisible(false);
			}
		}
	    
	    else if (drawerItem == DrawerItem.CAMERA_UPLOADS){
	    	if (cuF != null){			
				
				//Show
    			upgradeAccountMenuItem.setVisible(false);
    			selectMenuItem.setVisible(true);
    			takePicture.setVisible(true);

				//Hide
				pauseTransfersMenuIcon.setVisible(false);
				playTransfersMenuIcon.setVisible(false);
				createFolderMenuItem.setVisible(false);
				addContactMenuItem.setVisible(false);
    			addMenuItem.setVisible(false);
    			refreshMenuItem.setVisible(false);
    			sortByMenuItem.setVisible(false);
    			unSelectMenuItem.setVisible(false);
    			thumbViewMenuItem.setVisible(true);
    			addMenuItem.setEnabled(false);
    			createFolderMenuItem.setEnabled(false);
    			changePass.setVisible(false); 
    			exportMK.setVisible(false); 
    			removeMK.setVisible(false); 
    			rubbishBinMenuItem.setVisible(false);
    			clearRubbishBinMenuitem.setVisible(false);
    			importLinkMenuItem.setVisible(false);					
    			refreshMenuItem.setVisible(false);
				helpMenuItem.setVisible(false);
				settingsMenuItem.setVisible(false);

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
    			
    			if (isListInbox){	
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
				createFolderMenuItem.setVisible(false);
    			addMenuItem.setVisible(false);
    			addContactMenuItem.setVisible(false);
    			upgradeAccountMenuItem.setVisible(false);
    			unSelectMenuItem.setVisible(false);
    			addMenuItem.setEnabled(false);
    			changePass.setVisible(false); 
    			exportMK.setVisible(false); 
    			removeMK.setVisible(false); 
    			importLinkMenuItem.setVisible(false);
    			takePicture.setVisible(false);
    			refreshMenuItem.setVisible(false);
				helpMenuItem.setVisible(false);
				settingsMenuItem.setVisible(false);
    			clearRubbishBinMenuitem.setVisible(false);
    			rubbishBinMenuItem.setVisible(false);
    			gridSmallLargeMenuItem.setVisible(false);
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
					addMenuItem.setVisible(true);

					log("parentHandleIncoming: "+parentHandleIncoming);
					if(parentHandleIncoming==-1){
						addMenuItem.setVisible(false);
					}
					else{
						addMenuItem.setVisible(true);
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
					createFolderMenuItem.setVisible(false);
					addContactMenuItem.setVisible(false);
					unSelectMenuItem.setVisible(false);  				
					rubbishBinMenuItem.setVisible(false);
					createFolderMenuItem.setVisible(false);
					rubbishBinMenuItem.setVisible(false);
					clearRubbishBinMenuitem.setVisible(false);
					changePass.setVisible(false); 
					exportMK.setVisible(false); 
					removeMK.setVisible(false); 
					importLinkMenuItem.setVisible(false);
					takePicture.setVisible(false);					
	    			refreshMenuItem.setVisible(false);
					helpMenuItem.setVisible(false);
					settingsMenuItem.setVisible(false);
					upgradeAccountMenuItem.setVisible(false);
					gridSmallLargeMenuItem.setVisible(false);
					
					if (isListIncoming){	
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
					}
					else{
						addMenuItem.setVisible(true);
					}
					
					if(outSFLol.getItemCount()>0){
						selectMenuItem.setVisible(true);
					}
					else{
						selectMenuItem.setVisible(false);
					}
					searchMenuItem.setVisible(true);
					
					//Hide
					upgradeAccountMenuItem.setVisible(false);
					pauseTransfersMenuIcon.setVisible(false);
					playTransfersMenuIcon.setVisible(false);
					createFolderMenuItem.setVisible(false);
					addContactMenuItem.setVisible(false);
					unSelectMenuItem.setVisible(false);  				
					rubbishBinMenuItem.setVisible(false);
					createFolderMenuItem.setVisible(false);
					rubbishBinMenuItem.setVisible(false);
					clearRubbishBinMenuitem.setVisible(false);
					changePass.setVisible(false); 
					exportMK.setVisible(false); 
					removeMK.setVisible(false); 
					importLinkMenuItem.setVisible(false);
					takePicture.setVisible(false);					
	    			refreshMenuItem.setVisible(false);
					helpMenuItem.setVisible(false);
					settingsMenuItem.setVisible(false);
					gridSmallLargeMenuItem.setVisible(false);
					
					if (isListOutgoing){	
	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
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
					//Show
					addContactMenuItem.setVisible(true);
					selectMenuItem.setVisible(true);
					sortByMenuItem.setVisible(true);
					thumbViewMenuItem.setVisible(true);
	    			upgradeAccountMenuItem.setVisible(false);
	    			searchMenuItem.setVisible(true);
	    			
	    			//Hide	
					pauseTransfersMenuIcon.setVisible(false);
					playTransfersMenuIcon.setVisible(false);
					createFolderMenuItem.setVisible(false);				
	    			addMenuItem.setVisible(false);
	    			unSelectMenuItem.setVisible(false);    			
	    			addMenuItem.setEnabled(false);
	    			changePass.setVisible(false); 
	    			exportMK.setVisible(false); 
	    			removeMK.setVisible(false); 
	    			rubbishBinMenuItem.setVisible(false);
	    			clearRubbishBinMenuitem.setVisible(false);
	    			importLinkMenuItem.setVisible(false);
	    			takePicture.setVisible(false);
	    			refreshMenuItem.setVisible(false);
					helpMenuItem.setVisible(false);
					settingsMenuItem.setVisible(false);
	    			
	    			if (isListContacts){	
	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    			}    
	    			
	    			gridSmallLargeMenuItem.setVisible(false);
				}
			}
			else{
				//Show
    			upgradeAccountMenuItem.setVisible(false);
    			searchMenuItem.setVisible(false);
    			
    			//Hide	
    			addContactMenuItem.setVisible(false);
				selectMenuItem.setVisible(false);
				sortByMenuItem.setVisible(false);
				thumbViewMenuItem.setVisible(false);
				pauseTransfersMenuIcon.setVisible(false);
				playTransfersMenuIcon.setVisible(false);
				createFolderMenuItem.setVisible(false);				
    			addMenuItem.setVisible(false);
    			unSelectMenuItem.setVisible(false);    			
    			addMenuItem.setEnabled(false);
    			changePass.setVisible(false); 
    			exportMK.setVisible(false); 
    			removeMK.setVisible(false); 
    			rubbishBinMenuItem.setVisible(false);
    			clearRubbishBinMenuitem.setVisible(false);
    			importLinkMenuItem.setVisible(false);
    			takePicture.setVisible(false);
    			refreshMenuItem.setVisible(false);
				helpMenuItem.setVisible(false);
				settingsMenuItem.setVisible(false);
				gridSmallLargeMenuItem.setVisible(false);
			}
		}
	    
	    else if (drawerItem == DrawerItem.SEARCH){
	    	if (sFLol != null){			
				if (createFolderMenuItem != null){
					//Hide
	    			upgradeAccountMenuItem.setVisible(false);	    			
					cancelAllTransfersMenuItem.setVisible(false);
	    			thumbViewMenuItem.setVisible(false);
					pauseTransfersMenuIcon.setVisible(false);
					playTransfersMenuIcon.setVisible(false);
	    			createFolderMenuItem.setVisible(false);
	    			addContactMenuItem.setVisible(false);
	    			addMenuItem.setVisible(false);
	    			refreshMenuItem.setVisible(false);
	    			sortByMenuItem.setVisible(false);
	    			unSelectMenuItem.setVisible(false);
	    			changePass.setVisible(false); 
	    			exportMK.setVisible(false); 
	    			removeMK.setVisible(false); 
	    			addMenuItem.setEnabled(false);
	    			createFolderMenuItem.setEnabled(false);
	    			rubbishBinMenuItem.setVisible(false);
	    			clearRubbishBinMenuitem.setVisible(false);
	    			importLinkMenuItem.setVisible(false);
	    			takePicture.setVisible(false);					
	    			refreshMenuItem.setVisible(false);
					helpMenuItem.setVisible(false);
					settingsMenuItem.setVisible(false);
					gridSmallLargeMenuItem.setVisible(false);
					
					//Show
	    			selectMenuItem.setVisible(true);
				}
			}
		}
	    
	    else if (drawerItem == DrawerItem.ACCOUNT){
			if (maFLol != null){
					
				//Show
				refreshMenuItem.setVisible(true);
				helpMenuItem.setVisible(true);
				upgradeAccountMenuItem.setVisible(false);
				changePass.setVisible(true); 
				
				//Hide
				pauseTransfersMenuIcon.setVisible(false);
				playTransfersMenuIcon.setVisible(false);
				createFolderMenuItem.setVisible(false);
				addContactMenuItem.setVisible(false);
    			addMenuItem.setVisible(false);
    			sortByMenuItem.setVisible(false);
    			selectMenuItem.setVisible(false);
    			unSelectMenuItem.setVisible(false);
    			thumbViewMenuItem.setVisible(false);
    			addMenuItem.setEnabled(false);
    			createFolderMenuItem.setEnabled(false);
    			rubbishBinMenuItem.setVisible(false);
    			clearRubbishBinMenuitem.setVisible(false);
    			importLinkMenuItem.setVisible(false);
    			takePicture.setVisible(false);
				settingsMenuItem.setVisible(false);
				cancelAllTransfersMenuItem.setVisible(false);
				
				if (numberOfSubscriptions > 0){
					cancelSubscription.setVisible(true);
				}
				
				killAllSessions.setVisible(true);
    			
    			String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
    			log("Export in: "+path);
    			File file= new File(path);
    			if(file.exists()){
    				exportMK.setVisible(false); 
	    			removeMK.setVisible(true); 
    			}
    			else{
    				exportMK.setVisible(true); 
	    			removeMK.setVisible(false); 		
    			}
    			
    			gridSmallLargeMenuItem.setVisible(false);
 
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
    			addMenuItem.setEnabled(false);
    			createFolderMenuItem.setEnabled(false);
    			rubbishBinMenuItem.setVisible(false);
    			clearRubbishBinMenuitem.setVisible(false);
    			importLinkMenuItem.setVisible(false);
    			takePicture.setVisible(false);
				settingsMenuItem.setVisible(false);
				refreshMenuItem.setVisible(false);
				helpMenuItem.setVisible(false);
				upgradeAccountMenuItem.setVisible(false);
				changePass.setVisible(false);
				cancelSubscription.setVisible(false);				
				killAllSessions.setVisible(false);
				
				cancelAllTransfersMenuItem.setVisible(true);
				
				if(tranfersPaused){
					playTransfersMenuIcon.setVisible(true);
					pauseTransfersMenuIcon.setVisible(false);
				}
				else{
					playTransfersMenuIcon.setVisible(true);
					pauseTransfersMenuIcon.setVisible(false);
				}

			}
	    }
	    
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		log("onOptionsItemSelectedLollipop");
		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}
		
		if (megaApi != null){
			megaApi.retryPendingConnections();
		}
		
		int id = item.getItemId();
		switch(id){
			case android.R.id.home:{
				if (firstNavigationLevel){
					drawerLayout.openDrawer(nV);
				}
				else{
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
		    		if (drawerItem == DrawerItem.SHARED_ITEMS){
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
		    				if (inSFLol != null){					
		    					inSFLol.onBackPressed();					
		    				}				
		    			}	
		    		}
		    		if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE){
		    			if (oFLol != null){
		    				oFLol.onBackPressed();
		    				return true;
		    			}
		    		}
		    		if (sFLol != null){
		    			if (drawerItem == DrawerItem.SEARCH){
		    				sFLol.onBackPressed();
		    				return true;
		    			}
		    		}
				}
		    	return true;
		    }
		    case R.id.action_import_link:{
		    	showImportLinkDialog();
		    	return true;
		    }
		    case R.id.action_take_picture:{
		    	
		    	this.takePicture();
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
				builder.setTitle(title);
	            builder.setMessage(text);
				builder.setPositiveButton(getString(R.string.general_yes),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								if (tFLol != null){
									if (tFLol.isVisible()){
										tFLol.setNoActiveTransfers();
										tranfersPaused = false;
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
	    			
    				if(!tranfersPaused)
    				{
    					tranfersPaused = true;
    					pauseTransfersMenuIcon.setVisible(false);
    					playTransfersMenuIcon.setVisible(true);
    	    			
    	    			megaApi.pauseTransfers(true, this);
    				}
	        	}
	        	
	        	return true;
	        }
	        case R.id.action_play:{
	        	if (drawerItem == DrawerItem.TRANSFERS){	    			

	        		if(tranfersPaused){
	        			tranfersPaused = false;
						pauseTransfersMenuIcon.setVisible(true);
						playTransfersMenuIcon.setVisible(false);
	
		    			megaApi.pauseTransfers(false, this);
	        		}    				
	        	}	        	
	        	return true;
	        }
	        case R.id.action_add_contact:{
	        	if (drawerItem == DrawerItem.CONTACTS){
	        		showNewContactDialog(null);
	        	}
	        	
	        	return true;
	        }
	        case R.id.action_menu_kill_all_sessions:{
	        	megaApi.killSession(-1, this);
	        	return true;
	        }
	        case R.id.action_new_folder:{
	        	if (drawerItem == DrawerItem.CLOUD_DRIVE){
	        		showNewFolderDialog(null);
	        	}
	        	else if (drawerItem == DrawerItem.CONTACTS){
	        		showNewContactDialog(null);
	        	}
	        	return true;
	        }
	        case R.id.action_add:{
	        	
	        	if (drawerItem == DrawerItem.SHARED_ITEMS){
	        		String swmTag = getFragmentTag(R.id.shares_tabs_pager, 0);		
	        		inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(swmTag);
	        		if (viewPagerShares.getCurrentItem()==0){		
		        		if (inSFLol != null){	        		
		        			Long checkHandle = inSFLol.getParentHandle();		        			
		        			MegaNode checkNode = megaApi.getNodeByHandle(checkHandle);
		        			
		        			if((megaApi.checkAccess(checkNode, MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK)){
		        				this.uploadFile();
							}
							else if(megaApi.checkAccess(checkNode, MegaShare.ACCESS_READWRITE).getErrorCode() == MegaError.API_OK){
								this.uploadFile();
							}	
							else if(megaApi.checkAccess(checkNode, MegaShare.ACCESS_READ).getErrorCode() == MegaError.API_OK){
								log("Not permissions to upload");
								AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
					            builder.setMessage(getString(R.string.no_permissions_upload));
								builder.setTitle(R.string.op_not_allowed);
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
//		        			this.uploadFile();
		        			outSFLol.showUploadPanel();
		        		}
	        		}
	        	}	
	        	else {
	        		fbFLol.showUploadPanel();
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
	        	if (cuF != null){
	        		if (drawerItem == DrawerItem.CAMERA_UPLOADS){
	        			cuF.selectAll();
	        			if (cuF.showSelectMenuItem()){
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
	        		if (cuF != null){
	        			Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("cuFLol");
	        			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
	        			fragTransaction.detach(currentFragment);
	        			fragTransaction.commit();
	        			
	        			isLargeGridCameraUploads = !isLargeGridCameraUploads;
	        			if (isLargeGridCameraUploads){
	        				gridSmallLargeMenuItem.setIcon(getResources().getDrawable(R.drawable.ic_menu_gridview_small));
	        			}
	        			else{
	        				gridSmallLargeMenuItem.setIcon(getResources().getDrawable(R.drawable.ic_menu_gridview));
	        			}
	        			cuF.setIsLargeGrid(isLargeGridCameraUploads);

	        			fragTransaction = getSupportFragmentManager().beginTransaction();
	        			fragTransaction.attach(currentFragment);
	        			fragTransaction.commit();
	        		}
	        	}
	        	return true;
	        }
	        case R.id.action_grid:{	    			
	        	//TODO: gridView
	        	if (drawerItem == DrawerItem.CLOUD_DRIVE){
	        		int index = viewPagerCDrive.getCurrentItem();
	    			if(index==1){	
	    				//Rubbish Bin
	    				/*String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);		
	    				rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	    				if (rbFLol != null){
	    					if (rbFLol.onBackPressed() == 0){
	    						super.onBackPressed();
	    						return;
	    					}
	    				}*/
	    				
	    				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);		
	    				rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	    				if (rbFLol != null){
		        			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.detach(rbFLol);
		        			fragTransaction.commit();

		        			isListRubbishBin = !isListRubbishBin;
		        			if (isListRubbishBin){	
			    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
							}
							else{
								thumbViewMenuItem.setTitle(getString(R.string.action_list));
			    			}
		        			rbFLol.setIsList(isListRubbishBin);
		        			rbFLol.setParentHandle(parentHandleRubbish);

		        			fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.attach(rbFLol);
		        			fragTransaction.commit();
	    				}
	    			}
	    			else if(index==0){
	    				//Cloud Drive
	    				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);		
	    				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	    				if (fbFLol != null){
		        			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.detach(fbFLol);
		        			fragTransaction.commit();

		        			isListCloudDrive = !isListCloudDrive;
		        			if (isListCloudDrive){	
			    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
							}
							else{
								thumbViewMenuItem.setTitle(getString(R.string.action_list));
			    			}
		        			fbFLol.setIsList(isListCloudDrive);
		        			fbFLol.setParentHandle(parentHandleBrowser);

		        			fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.attach(fbFLol);
		        			fragTransaction.commit();
	    				}
	    			}
	        	}
	        	if (drawerItem == DrawerItem.INBOX){
	        		if (iFLol != null){        			
        				Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("iFLol");
        				FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
        				fragTransaction.detach(currentFragment);
        				fragTransaction.commit();

        				isListInbox = !isListInbox;
        				if (isListInbox){	
    	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
    					}
    					else{
    						thumbViewMenuItem.setTitle(getString(R.string.action_list));
    	    			}
        				iFLol.setIsList(isListInbox);						

        				fragTransaction = getSupportFragmentManager().beginTransaction();
        				fragTransaction.attach(currentFragment);
        				fragTransaction.commit();
        				
	        		}
	        	}
	        	if (drawerItem == DrawerItem.CONTACTS){
		        	String cFTag = getFragmentTag(R.id.contact_tabs_pager, 0);		
		    		cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
		        	if (cFLol != null){
	        		
	        			Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(cFTag);
	        			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
	        			fragTransaction.detach(currentFragment);
	        			fragTransaction.commit();

	        			isListContacts = !isListContacts;
	        			if (isListContacts){	
		    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
						}
						else{
							thumbViewMenuItem.setTitle(getString(R.string.action_list));
		    			}
	        			cFLol.setIsList(isListContacts);

	        			fragTransaction = getSupportFragmentManager().beginTransaction();
	        			fragTransaction.attach(currentFragment);
	        			fragTransaction.commit();	

	        		}
	        	}
	        	if (drawerItem == DrawerItem.SHARED_ITEMS){
	        		int index = viewPagerShares.getCurrentItem();
	    			if(index==0){	
	    				//Incoming
	    				String cFTag = getFragmentTag(R.id.shares_tabs_pager, 0);		
	    				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	    				if (inSFLol != null){
		        			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.detach(inSFLol);
		        			fragTransaction.commit();

		        			isListIncoming = !isListIncoming;
		        			if (isListIncoming){	
			    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
							}
							else{
								thumbViewMenuItem.setTitle(getString(R.string.action_list));
			    			}
		        			inSFLol.setIsList(isListIncoming);
		        			inSFLol.setParentHandle(parentHandleOutgoing);

		        			fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.attach(inSFLol);
		        			fragTransaction.commit();
	    				}
	    			}
	    			else if (index == 1){
	    				//Outgoing
	    				String cFTag = getFragmentTag(R.id.shares_tabs_pager, 1);		
	    				outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	    				if (outSFLol != null){
		        			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.detach(outSFLol);
		        			fragTransaction.commit();

		        			isListOutgoing = !isListOutgoing;
		        			if (isListOutgoing){	
			    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
							}
							else{
								thumbViewMenuItem.setTitle(getString(R.string.action_list));
			    			}
		        			outSFLol.setIsList(isListOutgoing);
		        			outSFLol.setParentHandle(parentHandleOutgoing);

		        			fragTransaction = getSupportFragmentManager().beginTransaction();
		        			fragTransaction.attach(outSFLol);
		        			fragTransaction.commit();
	    				}
	    			}
	        	}
	        	if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE){
	        		if (oFLol != null){        			
        				Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("oFLol");
        				FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
        				fragTransaction.detach(currentFragment);
        				fragTransaction.commit();

        				isListOffline = !isListOffline;
        				if (isListOffline){	
    	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
    					}
    					else{
    						thumbViewMenuItem.setTitle(getString(R.string.action_list));
    	    			}
        				oFLol.setIsList(isListOffline);						
        				oFLol.setPathNavigation(pathNavigation);
        				//oFLol.setGridNavigation(false);
        				//oFLol.setParentHandle(parentHandleSharedWithMe);

        				fragTransaction = getSupportFragmentManager().beginTransaction();
        				fragTransaction.attach(currentFragment);
        				fragTransaction.commit();
        				
	        		}
        		}
	        	if (drawerItem == DrawerItem.CAMERA_UPLOADS){
	        		if (cuF != null){        			
        				Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("cuFLol");
        				FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
        				fragTransaction.detach(currentFragment);
        				fragTransaction.commit();

        				isListCameraUploads = !isListCameraUploads;
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
        				cuF.setIsList(isListCameraUploads);

        				fragTransaction = getSupportFragmentManager().beginTransaction();
        				fragTransaction.attach(currentFragment);
        				fragTransaction.commit();

        			}
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
			    		intent.setAction(LoginActivityLollipop.ACTION_REFRESH);
			    		intent.putExtra("PARENT_HANDLE", parentHandleBrowser);
			    		startActivityForResult(intent, REQUEST_CODE_REFRESH);
		        		break;
		        	}
		        	case CONTACTS:{
		        		Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
			    		intent.setAction(LoginActivityLollipop.ACTION_REFRESH);
			    		intent.putExtra("PARENT_HANDLE", parentHandleBrowser);
			    		startActivityForResult(intent, REQUEST_CODE_REFRESH);
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
					    		intent.setAction(LoginActivityLollipop.ACTION_REFRESH);
					    		intent.putExtra("PARENT_HANDLE", parentHandleOutgoing);
					    		startActivityForResult(intent, REQUEST_CODE_REFRESH);
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
					    		intent.setAction(LoginActivityLollipop.ACTION_REFRESH);
					    		intent.putExtra("PARENT_HANDLE", parentHandleIncoming);
					    		startActivityForResult(intent, REQUEST_CODE_REFRESH);
					    		break;
		    				}				
		    			}	
		        	}
		        	case ACCOUNT:{
		        		Intent intent = new Intent(managerActivity, LoginActivityLollipop.class);
			    		intent.setAction(LoginActivityLollipop.ACTION_REFRESH);
			    		intent.putExtra("PARENT_HANDLE", parentHandleBrowser);
			    		startActivityForResult(intent, REQUEST_CODE_REFRESH);
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
        		sortByDateTV.setText(getString(R.string.sortby_date));
        		ViewGroup.MarginLayoutParams dateMLP = (ViewGroup.MarginLayoutParams) sortByDateTV.getLayoutParams();
        		sortByDateTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        		dateMLP.setMargins(Util.scaleWidthPx(25, outMetrics), Util.scaleHeightPx(15, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));
        		
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
        		if(drawerItem!=DrawerItem.CONTACTS){
        		
	        		switch(orderGetChildren){
		        		case MegaApiJava.ORDER_DEFAULT_ASC:{
		        			ascendingCheck.setChecked(true);
		        			descendingCheck.setChecked(false);
		        			newestCheck.setChecked(false);
		        			oldestCheck.setChecked(false);
		        			largestCheck.setChecked(false);
		        			smallestCheck.setChecked(false);
		        			break;
		        		}
		        		case MegaApiJava.ORDER_DEFAULT_DESC:{
		        			ascendingCheck.setChecked(false);
		        			descendingCheck.setChecked(true);
		        			newestCheck.setChecked(false);
		        			oldestCheck.setChecked(false);
		        			largestCheck.setChecked(false);
		        			smallestCheck.setChecked(false);
		        			break;
		        		}
		        		case MegaApiJava.ORDER_CREATION_DESC:{
		        			ascendingCheck.setChecked(false);
		        			descendingCheck.setChecked(false);
		        			newestCheck.setChecked(true);
		        			oldestCheck.setChecked(false);
		        			largestCheck.setChecked(false);
		        			smallestCheck.setChecked(false);
		        			break;
		        		}
		        		case MegaApiJava.ORDER_CREATION_ASC:{
		        			ascendingCheck.setChecked(false);
		        			descendingCheck.setChecked(false);
		        			newestCheck.setChecked(false);
		        			oldestCheck.setChecked(true);
		        			largestCheck.setChecked(false);
		        			smallestCheck.setChecked(false);
		        			break;
		        		}
		        		case MegaApiJava.ORDER_SIZE_ASC:{
		        			ascendingCheck.setChecked(false);
		        			descendingCheck.setChecked(false);
		        			newestCheck.setChecked(false);
		        			oldestCheck.setChecked(false);
		        			largestCheck.setChecked(false);
		        			smallestCheck.setChecked(true);
		        			break;
		        		}
		        		case MegaApiJava.ORDER_SIZE_DESC:{
		        			ascendingCheck.setChecked(false);
		        			descendingCheck.setChecked(false);
		        			newestCheck.setChecked(false);
		        			oldestCheck.setChecked(false);
		        			largestCheck.setChecked(true);
		        			smallestCheck.setChecked(false);
		        			break;
		        		}
	        		}
	        	}
        		else if(drawerItem==DrawerItem.CONTACTS){
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
        		
        		final AlertDialog dialog = sortByDialog;
	        	switch(drawerItem){
		        	case CONTACTS:{
		        		
		        		sortByDateTV.setVisibility(View.GONE);
		        		newestCheck.setVisibility(View.GONE);
		        		oldestCheck.setVisibility(View.GONE);
		        		sortBySizeTV.setVisibility(View.GONE);
		        		largestCheck.setVisibility(View.GONE);
		        		smallestCheck.setVisibility(View.GONE);     		
	        		
		        		ascendingCheck.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(true);
			        			descendingCheck.setChecked(false);
			        			selectSortByContacts(MegaApiJava.ORDER_DEFAULT_ASC);
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
			        			selectSortByContacts(MegaApiJava.ORDER_DEFAULT_DESC);
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
		        		sortBySizeTV.setVisibility(View.GONE);
		        		largestCheck.setVisibility(View.GONE);
		        		smallestCheck.setVisibility(View.GONE);
		        		
		        		ascendingCheck.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(true);
			        			descendingCheck.setChecked(false);
			        			selectSortByOffline(MegaApiJava.ORDER_DEFAULT_ASC);
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
			        			selectSortByOffline(MegaApiJava.ORDER_DEFAULT_DESC);
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
		        		sortBySizeTV.setVisibility(View.GONE);
		        		largestCheck.setVisibility(View.GONE);
		        		smallestCheck.setVisibility(View.GONE);
		        		int tab =-1;
		        				        		
		        		if (viewPagerShares.getCurrentItem()==0){
		        			tab = 0;
		        		}
		        		else{
		        			tab = 1;
		        		}
		        		
		        		final int tabFinal = tab;
		        		
		        		ascendingCheck.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(true);
			        			descendingCheck.setChecked(false);
			        			if(tabFinal==0){
			        				//INCOMING
			        				log("Incoming tab sort");
			        				selectSortByIncoming(MegaApiJava.ORDER_DEFAULT_ASC);
			        				
			        			}
			        			else{
			        				//OUTGOING
			        				log("Outgoing tab sort");
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
			        			if(tabFinal==0){
			        				//INCOMING
			        				log("Incoming tab sort");
			        				selectSortByIncoming(MegaApiJava.ORDER_DEFAULT_DESC);
			        				
			        			}
			        			else{
			        				//OUTGOING
			        				log("Outgoing tab sort");
			        				selectSortByOutgoing(MegaApiJava.ORDER_DEFAULT_DESC);
			        			}	
			        			
			        			if (dialog != null){
			        				dialog.dismiss();
			        			}
							}
						});
		        		
		        		break;
	        		
		        	}
		        	case CLOUD_DRIVE:{
		        		
		        		ascendingCheck.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View v) {
								ascendingCheck.setChecked(true);
			        			descendingCheck.setChecked(false);
			        			newestCheck.setChecked(false);
			        			oldestCheck.setChecked(false);
			        			largestCheck.setChecked(false);
			        			smallestCheck.setChecked(false);
			        			selectSortByCloudDrive(MegaApiJava.ORDER_DEFAULT_ASC);
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
			        			selectSortByCloudDrive(MegaApiJava.ORDER_DEFAULT_DESC);
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
			        			selectSortByCloudDrive(MegaApiJava.ORDER_CREATION_DESC);
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
			        			selectSortByCloudDrive(MegaApiJava.ORDER_CREATION_ASC);
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
			        			selectSortByCloudDrive(MegaApiJava.ORDER_SIZE_DESC);
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
			        			selectSortByCloudDrive(MegaApiJava.ORDER_SIZE_ASC);
			        			if (dialog != null){
			        				dialog.dismiss();
			        			}
							}
						});
		        		
		        		break;
	        		}
		        	default:{
		        		Intent intent = new Intent(managerActivity, SortByDialogActivity.class);
			    		intent.setAction(SortByDialogActivity.ACTION_SORT_BY);
			    		startActivityForResult(intent, REQUEST_CODE_SORT_BY);
			    		break;
		        	}
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
	        	showUpAF(null);
				return true;
	        }
	        case R.id.action_menu_settings:{
//				if (Build.VERSION.SDK_INT<Build.VERSION_CODES.HONEYCOMB) {
				    startActivity(new Intent(this, SettingsActivity.class));
//				}
//				else {
//					startActivity(new Intent(this, SettingsActivityHC.class));
//				}
	        	return true;
	        }
	        
	        case R.id.action_menu_change_pass:{
	        	Intent intent = new Intent(this, ChangePasswordActivityLollipop.class);
				startActivity(intent);
				return true;
	        }
	        case R.id.action_menu_remove_MK:{
	        	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				        switch (which){
				        case DialogInterface.BUTTON_POSITIVE:

							final String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
							final File f = new File(path);
				        	f.delete();	
				        	removeMK.setVisible(false);
				        	exportMK.setVisible(true);
				            break;

				        case DialogInterface.BUTTON_NEGATIVE:
				            //No button clicked
				            break;
				        }
				    }
				};

				AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
				builder.setMessage(R.string.remove_key_confirmation).setPositiveButton(R.string.general_yes, dialogClickListener)
				    .setNegativeButton(R.string.general_no, dialogClickListener).show();
				return true;
	        }
	        case R.id.action_menu_export_MK:{
	        	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				        switch (which){
				        case DialogInterface.BUTTON_POSITIVE:
				        	String key = megaApi.exportMasterKey();
							
							BufferedWriter out;         
							try {						

								final String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
								final File f = new File(path);
								log("Export in: "+path);
								FileWriter fileWriter= new FileWriter(path);	
								out = new BufferedWriter(fileWriter);	
								out.write(key);	
								out.close(); 								
								String message = getString(R.string.toast_master_key) + " " + path;
//				    			Snackbar.make(fragmentContainer, toastMessage, Snackbar.LENGTH_LONG).show();

				    			showAlert(message, "MasterKey exported!");
								removeMK.setVisible(true);
					        	exportMK.setVisible(false);

							}catch (FileNotFoundException e) {
							 e.printStackTrace();
							}catch (IOException e) {
							 e.printStackTrace();
							}
				        	
				            break;

				        case DialogInterface.BUTTON_NEGATIVE:
				            //No button clicked
				            break;
				        }
				    }
				};

				AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
				builder.setMessage(R.string.export_key_confirmation).setPositiveButton(R.string.general_yes, dialogClickListener)
				    .setNegativeButton(R.string.general_no, dialogClickListener).show();		
	        	
	        	return true;
	        }
//	        case R.id.action_menu_logout:{
//	        	logout(managerActivity, (MegaApplication)getApplication(), megaApi, false);
//	        	return true;
//	        }
	        case R.id.action_menu_cancel_subscriptions:{
	        	if (megaApi != null){
	        		//Show the message
	        		showCancelMessage();	        		
	        	}
	        	return true;
	        }
            default:{
	            return super.onOptionsItemSelected(item);
            }
		}
	}
	
    
    void showAlert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        log("Showing alert dialog: " + message);
        bld.create().show();
    }
    
    void showAlert(String message, String title) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        bld.setMessage(message);
        bld.setTitle(title);
//        bld.setNeutralButton("OK", null);
        bld.setPositiveButton("OK",null);
        log("Showing alert dialog: " + message);
        bld.create().show();
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

		log("retryPendingConnections()");
		if (megaApi != null){
			megaApi.retryPendingConnections();
		}
		try { 
			statusDialog.dismiss();	
		} 
		catch (Exception ex) {}
		
		if (drawerItem == DrawerItem.CLOUD_DRIVE){

			int index = viewPagerCDrive.getCurrentItem();
			if(index==1){	
				//Rubbish Bin
				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);		
				rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
				if (rbFLol != null){
					if (rbFLol.onBackPressed() == 0){
						super.onBackPressed();
						return;
					}
				}
			}
			else if(index==0){
				//Cloud Drive
				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);		
				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
				if (fbFLol != null){
					if (fbFLol.onBackPressed() == 0){
						super.onBackPressed();
						return;
					}
				}
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

    		switch(accountFragment){

	    		case MY_ACCOUNT_FRAGMENT:{
	    			if (maFLol != null){						
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
	    		case UPGRADE_ACCOUNT_FRAGMENT:{
	    			if (upAF != null){						
	    				drawerItem = DrawerItem.ACCOUNT;
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
	    		case PAYMENT_FRAGMENT:{
	    			if (pF != null){
	    				pF.onBackPressed();
	    			}
	    			return;					
	    		}
	    		case CC_FRAGMENT:{
	    			if (ccF != null){
	    				int parameterType = ccF.getParameterType();
	    				ArrayList<Product> accounts = ccF.getAccounts();
	    				BitSet paymentBitSet = ccF.getPaymentBitSet();
	    				showpF(parameterType, accounts, paymentBitSet);
	    			}
	    			else{
	    				showUpAF(null);
	    			}
	    			return;
	    		}
	    		case OVERQUOTA_ALERT:{
	    			if (upAF != null){						
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
			if (cuF != null){    		
    			if (cuF.onBackPressed() == 0){
    				drawerItem = DrawerItem.CLOUD_DRIVE;
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
	
	public void onFileClick(ArrayList<Long> handleList){
		log("onFileClick: "+handleList.size()+" files to download");
		long size = 0;
		long[] hashes = new long[handleList.size()];
		for (int i=0;i<handleList.size();i++){
			hashes[i] = handleList.get(i);
			size += megaApi.getNodeByHandle(hashes[i]).getSize();
		}
		
		if (dbH == null){
//			dbH = new DatabaseHandler(getApplicationContext());
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		}
		
		boolean askMe = true;
		boolean advancedDevices=false;
		String downloadLocationDefaultPath = Util.downloadDIR;
		prefs = dbH.getPreferences();		
		if (prefs != null){
			log("prefs != null");
			if (prefs.getStorageAskAlways() != null){				
				if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
					log("askMe==false");
					if (prefs.getStorageDownloadLocation() != null){
						if (prefs.getStorageDownloadLocation().compareTo("") != 0){
							askMe = false;
							downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
						}
					}
				}
				else
				{
					log("askMe==true");
					//askMe=true
					if (prefs.getStorageAdvancedDevices() != null){
						advancedDevices = Boolean.parseBoolean(prefs.getStorageAdvancedDevices());						
					}
					
				}
			}
		}		
			
		if (askMe){
			log("askMe");
			if(advancedDevices){
				log("advancedDevices");
				//Launch Intent to SAF
				if(hashes.length==1){
					downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
					this.openAdvancedDevices(hashes[0]);
				}
				else
				{
					//Show error message, just one file
					Snackbar.make(fragmentContainer, getString(R.string.context_select_one_file), Snackbar.LENGTH_LONG).show();
				}		    	
			}
			else{
				log("NOT advancedDevices");
				Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
				intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, false);
				intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
				intent.setClass(this, FileStorageActivityLollipop.class);
				intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
				startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
			}				
		}
		else{
			log("NOT askMe");
			File defaultPathF = new File(downloadLocationDefaultPath);
			defaultPathF.mkdirs();
			downloadTo(downloadLocationDefaultPath, null, size, hashes);
		}		
	}
	
	public void downloadTo(String parentPath, String url, long size, long [] hashes){
		log("downloadTo, parentPath: "+parentPath+ "url: "+url+" size: "+size);
		log("files to download: ");
		if (hashes != null){
			for (long hash : hashes) {
				MegaNode node = megaApi.getNodeByHandle(hash);
				log("Node: "+ node.getName());
			}
		}
		
		double availableFreeSpace = Double.MAX_VALUE;
		try{
			StatFs stat = new StatFs(parentPath);
			availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
		}
		catch(Exception ex){}		
		
		if (hashes == null){
			log("hashes is null");
			if(url != null) {
				log("url NOT null");
				if(availableFreeSpace < size) {
					Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
					log("Not enough space");
					return;
				}
				
				Intent service = new Intent(this, DownloadService.class);
				service.putExtra(DownloadService.EXTRA_URL, url);
				service.putExtra(DownloadService.EXTRA_SIZE, size);
				service.putExtra(DownloadService.EXTRA_PATH, parentPath);
				startService(service);
			}
		}
		else{
			log("hashes is NOT null");
			if(hashes.length == 1){
				log("hashes.length == 1");
				MegaNode tempNode = megaApi.getNodeByHandle(hashes[0]);
				if((tempNode != null) && tempNode.getType() == MegaNode.TYPE_FILE){
					log("ISFILE");
					String localPath = Util.getLocalFile(this, tempNode.getName(), tempNode.getSize(), parentPath);
					
					if(localPath != null){
						log("localPath != null");
						try { 
							log("Call to copyFile");
							Util.copyFile(new File(localPath), new File(parentPath, tempNode.getName())); 
						}
						catch(Exception e) {
							log("Exception!!");
						}
												
//						if(MimeType.typeForName(tempNode.getName()).isPdf()){
//							
//		    			    File pdfFile = new File(localPath);
//		    			    
//		    			    Intent intentPdf = new Intent();
//		    			    intentPdf.setDataAndType(Uri.fromFile(pdfFile), "application/pdf");
//		    			    intentPdf.setClass(this, OpenPDFActivity.class);
//		    			    intentPdf.setAction("android.intent.action.VIEW");
//		    				this.startActivity(intentPdf);
//							
//						}
//						else						
						
						if(MimeTypeList.typeForName(tempNode.getName()).isZip()){
							log("MimeTypeList ZIP");
		    			    File zipFile = new File(localPath);
		    			    
		    			    Intent intentZip = new Intent();
		    			    intentZip.setClass(this, ZipBrowserActivity.class);
		    			    intentZip.putExtra(ZipBrowserActivity.EXTRA_PATH_ZIP, zipFile.getAbsolutePath());
		    			    intentZip.putExtra(ZipBrowserActivity.EXTRA_HANDLE_ZIP, tempNode.getHandle());

		    				this.startActivity(intentZip);
							
						}
						else{		
							log("MimeTypeList other file");
							Intent viewIntent = new Intent(Intent.ACTION_VIEW);
							viewIntent.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
							if (isIntentAvailable(this, viewIntent)){
								log("if isIntentAvailable");
								startActivity(viewIntent);
							}								
							else{
								log("ELSE isIntentAvailable");
								Intent intentShare = new Intent(Intent.ACTION_SEND);
								intentShare.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
								if (isIntentAvailable(this, intentShare)){
									log("call to startActivity(intentShare)");
									startActivity(intentShare);
								}									
								Snackbar.make(fragmentContainer, getString(R.string.general_already_downloaded), Snackbar.LENGTH_LONG).show();
							}								
						}
						return;
					}
				}
			}
			
			for (long hash : hashes) {
				log("hashes.length more than 1");
				MegaNode node = megaApi.getNodeByHandle(hash);
				if(node != null){
					log("node NOT null");
					Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
					if (node.getType() == MegaNode.TYPE_FOLDER) {
						log("MegaNode.TYPE_FOLDER");
						getDlList(dlFiles, node, new File(parentPath, new String(node.getName())));
					} else {
						log("MegaNode.TYPE_FILE");
						dlFiles.put(node, parentPath);
					}
					
					for (MegaNode document : dlFiles.keySet()) {
						
						String path = dlFiles.get(document);
						log("path of the file: "+path);
						
						if(availableFreeSpace < document.getSize()){
							log("Not enough space");
							Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space)+ " (" + new String(document.getName()) + ")", Snackbar.LENGTH_LONG).show();							
							continue;
						}
						
						log("start service");
						Intent service = new Intent(this, DownloadService.class);
						service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
						service.putExtra(DownloadService.EXTRA_URL, url);
						service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
						service.putExtra(DownloadService.EXTRA_PATH, path);
						startService(service);
					}
				}
				else if(url != null) {
					log("URL NOT null");
					if(availableFreeSpace < size) {
						log("Not enough space");
						Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
						continue;
					}
					log("start service");
					Intent service = new Intent(this, DownloadService.class);
					service.putExtra(DownloadService.EXTRA_HASH, hash);
					service.putExtra(DownloadService.EXTRA_URL, url);
					service.putExtra(DownloadService.EXTRA_SIZE, size);
					service.putExtra(DownloadService.EXTRA_PATH, parentPath);
					startService(service);
				}
				else {
					log("node NOT fOUND!!!!!");
				}
			}
		}
	}
	
	public void clickOnMasterKeyFile(){
		Intent viewIntent = new Intent(Intent.ACTION_VIEW);
		String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
		viewIntent.setDataAndType(Uri.fromFile(new File(path)), MimeTypeList.typeForName("MEGAMasterKey.txt").getType());
		if (isIntentAvailable(this, viewIntent)){
			log("if isIntentAvailable");
			startActivity(viewIntent);
		}								
		else{
			log("ELSE isIntentAvailable");
			Intent intentShare = new Intent(Intent.ACTION_SEND);
			intentShare.setDataAndType(Uri.fromFile(new File(path)), MimeTypeList.typeForName("MEGAMasterKey.txt").getType());
			if (isIntentAvailable(this, intentShare)){
				log("call to startActivity(intentShare)");
				startActivity(intentShare);
			}									
			Snackbar.make(fragmentContainer, getString(R.string.general_already_downloaded), Snackbar.LENGTH_LONG).show();
		}		
	}
	
	/*
	 * If there is an application that can manage the Intent, returns true. Otherwise, false.
	 */
	public static boolean isIntentAvailable(Context ctx, Intent intent) {
		log("isIntentAvailable");
		final PackageManager mgr = ctx.getPackageManager();
		List<ResolveInfo> list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
	
	/*
	 * Get list of all child files
	 */
	private void getDlList(Map<MegaNode, String> dlFiles, MegaNode parent, File folder) {
		log("getDlList");
		if (megaApi.getRootNode() == null)
			return;
		
		folder.mkdir();
		ArrayList<MegaNode> nodeList = megaApi.getChildren(parent, orderGetChildren);
		for(int i=0; i<nodeList.size(); i++){
			MegaNode document = nodeList.get(i);
			if (document.getType() == MegaNode.TYPE_FOLDER) {
				File subfolder = new File(folder, new String(document.getName()));
				getDlList(dlFiles, document, subfolder);
			} 
			else {
				dlFiles.put(document, folder.getAbsolutePath());
			}
		}
	}
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public void openAdvancedDevices (long handle){
		log("openAdvancedDevices");
		handleToDownload = handle;
		String externalPath = Util.getExternalCardPath();	
		
		if(externalPath!=null){
			log("ExternalPath for advancedDevices: "+externalPath);
			MegaNode node = megaApi.getNodeByHandle(handle);
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
			    try{
			    	startActivityForResult(intent, WRITE_SD_CARD_REQUEST_CODE);			
			    }
			    catch(Exception e){
			    	log("Exception in External SDCARD");
			    	Environment.getExternalStorageDirectory();
			    	Snackbar.make(fragmentContainer, getString(R.string.no_external_SD_card_detected), Snackbar.LENGTH_LONG).show();
			    }
			}
		}
		else{
			log("No external SD card");
			Environment.getExternalStorageDirectory();
			Snackbar.make(fragmentContainer, getString(R.string.no_external_SD_card_detected), Snackbar.LENGTH_LONG).show();
		}		
	}
	
	public void showRenameDialog(final MegaNode document, String text){
		log("showRenameDialog");
		
		LinearLayout layout = new LinearLayout(this);
	    layout.setOrientation(LinearLayout.VERTICAL);
	    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);
	
		final EditTextCursorWatcher input = new EditTextCursorWatcher(this);
		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setTextColor(getResources().getColor(R.color.text_secondary));
//		input.setHint(getString(R.string.context_new_folder_name));
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);

		input.setImeActionLabel(getString(R.string.context_rename),KeyEvent.KEYCODE_ENTER);
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
					rename(document, value);
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
						rename(document, value);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		builder.setView(layout);
		renameDialog = builder.create();
		renameDialog.show();

	}
	
	private void rename(MegaNode document, String newName){
		log("rename");
		if (newName.compareTo(document.getName()) == 0) {
			return;
		}
		
		if(!Util.isOnline(this)){
			Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
			return;
		}
		
		if (isFinishing()){
			return;
		}
		
		ProgressDialog temp = null;
		try{
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.context_renaming));
			temp.show();
		}
		catch(Exception e){
			return;
		}
		statusDialog = temp;
		
		log("renaming " + document.getName() + " to " + newName);
		
		megaApi.renameNode(document, newName, this);
	}
	
	/*
	 * Display keyboard
	 */
	private void showKeyboardDelayed(final View view) {
		log("showKeyboardDelayed");
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (fbFLol != null){
					if (!(drawerItem == DrawerItem.CLOUD_DRIVE)){
						return;
					}
				}
				String cFTag = getFragmentTag(R.id.contact_tabs_pager, 0);		
				cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
				if (cFLol != null){
					if (drawerItem == DrawerItem.CONTACTS){
						return;
					}
				}
				if (inSFLol != null){
					if (drawerItem == DrawerItem.SHARED_ITEMS){
						return;
					}
				}
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}
	
	public void showCopyLollipop(ArrayList<Long> handleList){
		log("showCopyLollipop");
		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("COPY_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER);
	}
	
	public void showMoveLollipop(ArrayList<Long> handleList){
		log("showMoveLollipop");
		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("MOVE_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
	}
	
	public void sentToInboxLollipop(MegaNode node){
		log("sentToInbox MegaNode");
		
		if((drawerItem == DrawerItem.SHARED_ITEMS) || (drawerItem == DrawerItem.CLOUD_DRIVE) ){
			sendToInbox = true;			
			Intent intent = new Intent(ContactsExplorerActivityLollipop.ACTION_PICK_CONTACT_SEND_FILE);
	    	intent.setClass(this, ContactsExplorerActivityLollipop.class);
	    	//Multiselect=0
	    	intent.putExtra("MULTISELECT", 0);
	    	intent.putExtra("SEND_FILE",1);
	    	intent.putExtra(ContactsExplorerActivityLollipop.EXTRA_NODE_HANDLE, node.getHandle());
	    	startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACT);
		}			
	}
	
	public void shareFolderLollipop(ArrayList<Long> handleList){
		log("shareFolder ArrayListLong");
		//TODO shareMultipleFolders

		Intent intent = new Intent(ContactsExplorerActivityLollipop.ACTION_PICK_CONTACT_SHARE_FOLDER);
    	intent.setClass(this, ContactsExplorerActivityLollipop.class);
    	
    	long[] handles=new long[handleList.size()];
    	int j=0;
    	for(int i=0; i<handleList.size();i++){
    		handles[j]=handleList.get(i);
    		j++;
    	}	    	
    	intent.putExtra(ContactsExplorerActivityLollipop.EXTRA_NODE_HANDLE, handles);
    	//Multiselect=1 (multiple folders)
    	intent.putExtra("MULTISELECT", 1);
    	intent.putExtra("SEND_FILE",0);
    	startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACT);
	}
	
	public void shareFolderLollipop(MegaNode node){
		log("shareFolderLollipop");	
		log("Sale el AlertDialog");
											
		Intent intent = new Intent(ContactsExplorerActivityLollipop.ACTION_PICK_CONTACT_SHARE_FOLDER);
    	intent.setClass(this, ContactsExplorerActivityLollipop.class);
    	//Multiselect=0
    	intent.putExtra("MULTISELECT", 0);
    	intent.putExtra("SEND_FILE",0);
    	intent.putExtra(ContactsExplorerActivityLollipop.EXTRA_NODE_HANDLE, node.getHandle());
    	startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACT);
				
	}
	
	public void getPublicLinkAndShareIt(MegaNode document){
		log("getPublicLinkAndShareIt");
		if (!Util.isOnline(this)){
			Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
			return;
		}
		
		if(isFinishing()){
			return;	
		}
		
		ProgressDialog temp = null;
		try{
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.context_creating_link));
			temp.show();
		}
		catch(Exception e){
			return;
		}
		statusDialog = temp;
		
		isGetLink = true;
		megaApi.exportNode(document, this);
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
		builder.setTitle(getResources().getString(R.string.cancel_transfer_title));
        builder.setMessage(getResources().getString(R.string.cancel_transfer_confirmation));
        builder.setPositiveButton(R.string.general_yes, dialogClickListener);
        builder.setNegativeButton(R.string.general_no, dialogClickListener);
        builder.show();	
		
	}
	
	public void moveToTrash(final ArrayList<Long> handleList){
		log("moveToTrash");
		isClearRubbishBin = false;
		
		if (!Util.isOnline(this)){
			Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
			return;
		}		
		
		if(isFinishing()){
			return;	
		}
		
		final MegaNode rubbishNode = megaApi.getRubbishNode();
		
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		        	//TODO remove the outgoing shares
		        	
		        	for (int i=0;i<handleList.size();i++){
		    			//Check if the node is not yet in the rubbish bin (if so, remove it)
		    			MegaNode parent = megaApi.getNodeByHandle(handleList.get(i));
		    			while (megaApi.getParentNode(parent) != null){
		    				parent = megaApi.getParentNode(parent);
		    			}
		    				
		    			if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
		    				moveToRubbish = true;
		    				megaApi.moveNode(megaApi.getNodeByHandle(handleList.get(i)), rubbishNode, managerActivity);
		    			}
		    			else{
		    				megaApi.remove(megaApi.getNodeByHandle(handleList.get(i)), managerActivity);
		    			}
		    		}
		    		
		    		if (moveToRubbish){
		    			ProgressDialog temp = null;
		    			try{
		    				temp = new ProgressDialog(managerActivity);
		    				temp.setMessage(getString(R.string.context_move_to_trash));
		    				temp.show();
		    			}
		    			catch(Exception e){
		    				return;
		    			}
		    			statusDialog = temp;
		    		}
		    		else{
		    			ProgressDialog temp = null;
		    			try{
		    				temp = new ProgressDialog(managerActivity);
		    				temp.setMessage(getString(R.string.context_delete_from_mega));
		    				temp.show();
		    			}
		    			catch(Exception e){
		    				return;
		    			}
		    			statusDialog = temp;
		    		}
		        	
		            break;

		        case DialogInterface.BUTTON_NEGATIVE:
		            //No button clicked
		            break;
		        }
		    }
		};

		if (handleList.size() > 0){
			MegaNode p = megaApi.getNodeByHandle(handleList.get(0));
			while (megaApi.getParentNode(p) != null){
				p = megaApi.getParentNode(p);
			}
			if (p.getHandle() != megaApi.getRubbishNode().getHandle()){				
				
				AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
				builder.setTitle(getResources().getString(R.string.section_rubbish_bin));
	            builder.setMessage(getResources().getString(R.string.confirmation_move_to_rubbish));
	            builder.setPositiveButton(R.string.general_yes, dialogClickListener);
	            builder.setNegativeButton(R.string.general_no, dialogClickListener);
	            builder.show();
			}
			else{
				
				AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
				builder.setTitle(getResources().getString(R.string.title_delete_from_mega));
				builder.setMessage(getResources().getString(R.string.confirmation_delete_from_mega));
				builder.setPositiveButton(R.string.general_yes, dialogClickListener);
				builder.setNegativeButton(R.string.general_no, dialogClickListener);
				builder.show();
			}
		}
	}
	
	public void showImportLinkDialog(){
		log("showRenameDialog");
		LinearLayout layout = new LinearLayout(this);
	    layout.setOrientation(LinearLayout.VERTICAL);
	    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

	    final EditText input = new EditText(this);
		input.setId(EDIT_TEXT_ID);
		input.setSingleLine(false);
		
		input.setTextColor(getResources().getColor(R.color.text_secondary));
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
	    layout.addView(input, params);
		input.setImeActionLabel(getString(R.string.context_open_link_title),KeyEvent.KEYCODE_ENTER);
		
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
						importLink(value);
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
					importLink(value);
					return true;
				}
				return false;
			}
		});
	}
	
	private void importLink(String url) {
		
		try {
			url = URLDecoder.decode(url, "UTF-8");
		} 
		catch (UnsupportedEncodingException e) {}
		url.replace(' ', '+');
		if(url.startsWith("mega://")){
			url = url.replace("mega://", "https://mega.co.nz/");
		}
		
		log("url " + url);
		
		// Download link
		if (url != null && (url.matches("^https://mega.co.nz/#!.*!.*$") || url.matches("^https://mega.nz/#!.*!.*$"))) {
			log("open link url");
			
//			Intent openIntent = new Intent(this, ManagerActivityLollipop.class);
			Intent openFileIntent = new Intent(this, FileLinkActivityLollipop.class);
			openFileIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openFileIntent.setAction(ManagerActivityLollipop.ACTION_OPEN_MEGA_LINK);
			openFileIntent.setData(Uri.parse(url));
			startActivity(openFileIntent);
//			finish();
			return;
		}
		
		// Folder Download link
		else if (url != null && (url.matches("^https://mega.co.nz/#F!.+$") || url.matches("^https://mega.nz/#F!.+$"))) {
			log("folder link url");
			Intent openFolderIntent = new Intent(this, FolderLinkActivityLollipop.class);
			openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openFolderIntent.setAction(ManagerActivityLollipop.ACTION_OPEN_MEGA_FOLDER_LINK);
			openFolderIntent.setData(Uri.parse(url));
			startActivity(openFolderIntent);
//			finish();
			return;
		}
		else{
			log("wrong url");
			Intent errorIntent = new Intent(this, ManagerActivityLollipop.class);
			errorIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(errorIntent);
		}
	}
	
	public void takePicture(){
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

        startActivityForResult(cameraIntent, TAKE_PHOTO_CODE);
	}
	
	public void showCancelMessage(){
		AlertDialog cancelDialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		builder.setTitle(getString(R.string.title_cancel_subscriptions));
		
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
	
	public void showNewFolderDialog(String editText){
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
		
		input.setId(EDIT_TEXT_ID);
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
		input.setImeActionLabel(getString(R.string.general_create),KeyEvent.KEYCODE_ENTER);
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
		
		long parentHandle;
		if (drawerItem == DrawerItem.CLOUD_DRIVE){
			parentHandle = fbFLol.getParentHandle();
		}
		else{
			return;
		}
		
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
		builder.setTitle("Clear Rubbbish Bin?");
		builder.setMessage(getString(R.string.context_clear_rubbish));
		builder.setPositiveButton(getString(R.string.general_empty),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						clearRubbishBin();
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		clearRubbishBinDialog = builder.create();
		clearRubbishBinDialog.show();
//		Util.brandAlertDialog(clearRubbishBinDialog);
	}
	
	private void clearRubbishBin(){
		log("clearRubbishBin");
		ClearRubbisBinTask clearRubbishBinTask = new ClearRubbisBinTask(this);
		clearRubbishBinTask.execute();
	}
	
	/*
	 * Background task to emptying the Rubbish Bin
	 */
	private class ClearRubbisBinTask extends AsyncTask<String, Void, Void> {
		Context context;
		
		ClearRubbisBinTask(Context context){
			this.context = context;
		}
		
		@Override
		protected Void doInBackground(String... params) {
			log("doInBackground-Async Task ClearRubbisBinTask");
			
			if (rbFLol != null){
				ArrayList<MegaNode> rubbishNodes = megaApi.getChildren(megaApi.getRubbishNode(), orderGetChildren);
				
				isClearRubbishBin = true;
				for (int i=0; i<rubbishNodes.size(); i++){
					megaApi.remove(rubbishNodes.get(i), managerActivity);
				}
			}					
			return null;
		}		
	}
	
	public void uploadFile(){
//		uploadDialog = new UploadHereDialog();
//		uploadDialog.show(getSupportFragmentManager(), "fragment_upload");
		fbFLol.showUploadPanel();
	}
	
	public void upgradeAccountButton(){
		log("upgradeAccountButton");
		drawerItem = DrawerItem.ACCOUNT;
		if (accountInfo != null){
			if ((accountInfo.getSubscriptionStatus() == MegaAccountDetails.SUBSCRIPTION_STATUS_NONE) || (accountInfo.getSubscriptionStatus() == MegaAccountDetails.SUBSCRIPTION_STATUS_INVALID)){
				Time now = new Time();
				now.setToNow();
				if (accountType != 0){
					log("accountType != 0");
					if (now.toMillis(false) >= (accountInfo.getProExpiration()*1000)){
						if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD) || Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_FORTUMO) || Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CENTILI)){
							log("SUBSCRIPTION INACTIVE: CHECKBITSET --> CC || FORT || INFO");
							showUpAF(null);
						}
						else{
							Snackbar.make(fragmentContainer, getString(R.string.not_upgrade_is_possible), Snackbar.LENGTH_LONG).show();
							
						}
					}
					else{
						log("CURRENTLY ACTIVE SUBSCRIPTION");
						Snackbar.make(fragmentContainer, getString(R.string.not_upgrade_is_possible), Snackbar.LENGTH_LONG).show();
					}
				}
				else{
					log("accountType == 0");
					if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD) || Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_FORTUMO) || Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CENTILI)){
						log("CHECKBITSET --> CC || FORT || INFO");
						showUpAF(null);
					}
					else{
						Snackbar.make(fragmentContainer, getString(R.string.not_upgrade_is_possible), Snackbar.LENGTH_LONG).show();
					}
				}
			}
			else{
				Snackbar.make(fragmentContainer, getString(R.string.not_upgrade_is_possible), Snackbar.LENGTH_LONG).show();
			}
		}
		else{
			Snackbar.make(fragmentContainer, getString(R.string.not_upgrade_is_possible), Snackbar.LENGTH_LONG).show();
		}
	}
	
	public void pickFolderToShare(List<MegaUser> users){
		
		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_SELECT_FOLDER);
		String[] longArray = new String[users.size()];
		for (int i=0; i<users.size(); i++){
			longArray[i] = users.get(i).getEmail();
		}
		intent.putExtra("SELECTED_CONTACTS", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER);
	}
	
	public void removeMultipleContacts(final List<MegaUser> contacts){
		
		//TODO (megaApi.getInShares(c).size() != 0) --> Si el contacto que voy a borrar tiene carpetas compartidas, avisar de eso y eliminar las shares (IN and ¿OUT?)
		for(int j=0; j<contacts.size();j++){
			
			final MegaUser c= contacts.get(j);
			
			final ArrayList<MegaNode> inShares = megaApi.getInShares(c);
			
			if(inShares.size() != 0)
			{			
			        	
	        	for(int i=0; i<inShares.size();i++){
	        		MegaNode removeNode = inShares.get(i);
	        		megaApi.remove(removeNode);			        		
	        	}
	        	megaApi.removeContact(c, managerActivity);		        	
				
			}
			else{
				//NO incoming shares				
				        	
	        	megaApi.removeContact(c, managerActivity);
			}	
		}		
	}
	
	public void setPinLock(){
		log("setPinLock");
		Intent intent = new Intent(this, PinLockActivityLollipop.class);
		intent.setAction(PinLockActivityLollipop.ACTION_SET_PIN_LOCK);
		this.startActivityForResult(intent, SET_PIN);
	}
	
	public void resetPinLock(){
		log("resetPinLock");
		Intent intent = new Intent(this, PinLockActivityLollipop.class);
		intent.setAction(PinLockActivityLollipop.ACTION_RESET_PIN_LOCK);
		this.startActivity(intent);
	}
	
	public void showNewContactDialog(String editText){
		log("showNewContactDialog");
		
		String cFTag = getFragmentTag(R.id.contact_tabs_pager, 0);		
		cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
		if (cFLol != null){
			if (drawerItem == DrawerItem.CONTACTS){
				cFLol.setPositionClicked(-1);
				cFLol.notifyDataSetChanged();
			}
		}
		
		String text;
		if ((editText == null) || (editText.compareTo("") == 0)){
			text = getString(R.string.context_new_contact_name);
		}
		else{
			text = editText;
		}
		
		LinearLayout layout = new LinearLayout(this);
	    layout.setOrientation(LinearLayout.VERTICAL);
	    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);
		
		final EditText input = new EditText(this);
	    layout.addView(input, params);
	    
		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setTextColor(getResources().getColor(R.color.text_secondary));
//		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						return true;
					}
					inviteContact(value);
					addContactDialog.dismiss();
					return true;
				}
				return false;
			}
		});
		input.setImeActionLabel(getString(R.string.general_add),
				KeyEvent.KEYCODE_ENTER);
		input.setText(text);
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
						String value = input.getText().toString().trim();
						if (value.length() == 0) {
							return;
						}
						inviteContact(value);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		builder.setView(layout);
		addContactDialog = builder.create();
		addContactDialog.show();
	}
	
	public void inviteContact(String contactEmail){
		log("inviteContact");
		
		if (!Util.isOnline(this)){
			Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
			return;
		}
		
		if(isFinishing()){
			return;	
		}
		
		statusDialog = null;
		try {
			statusDialog = new ProgressDialog(this);
			statusDialog.setMessage(getString(R.string.context_adding_contact));
			statusDialog.show();
		}
		catch(Exception e){
			return;
		}		

		megaApi.inviteContact(contactEmail, null, MegaContactRequest.INVITE_ACTION_ADD, this);
	}
	
	public void pickContacToSendFile(List<MegaUser> users){
		
		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_SELECT_FILE);
		String[] longArray = new String[users.size()];
		for (int i=0; i<users.size(); i++){
			longArray[i] = users.get(i).getEmail();
		}
		intent.putExtra("SELECTED_CONTACTS", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);		
	}
	
	public void removeContact(final MegaUser c){
		
		//TODO (megaApi.getInShares(c).size() != 0) --> Si el contacto que voy a borrar tiene carpetas compartidas, avisar de eso y eliminar las shares (IN and ¿OUT?)
		
		final ArrayList<MegaNode> inShares = megaApi.getInShares(c);
		
		if(inShares.size() != 0)
		{
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			        switch (which){
			        case DialogInterface.BUTTON_POSITIVE:
			        	
			        	for(int i=0; i<inShares.size();i++){
			        		MegaNode removeNode = inShares.get(i);
			        		megaApi.remove(removeNode);			        		
			        	}
			        	megaApi.removeContact(c, managerActivity);			        	
			            break;

			        case DialogInterface.BUTTON_NEGATIVE:
			            //No button clicked
			            break;
			        }
			    }
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(managerActivity, R.style.AppCompatAlertDialogStyle);
			builder.setMessage(getResources().getString(R.string.confirmation_remove_contact)+" "+c.getEmail()+"?").setPositiveButton(R.string.general_yes, dialogClickListener)
			    .setNegativeButton(R.string.general_no, dialogClickListener).show();
		}
		else{
			//NO incoming shares
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			        switch (which){
			        case DialogInterface.BUTTON_POSITIVE:
			        	//TODO remove the outgoing shares
			        	
			        	megaApi.removeContact(c, managerActivity);
			        	
			            break;

			        case DialogInterface.BUTTON_NEGATIVE:
			            //No button clicked
			            break;
			        }
			    }
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(managerActivity, R.style.AppCompatAlertDialogStyle);
			String message= getResources().getString(R.string.confirmation_remove_contact)+" "+c.getEmail()+"?";
			builder.setMessage(message).setPositiveButton(R.string.general_yes, dialogClickListener)
			    .setNegativeButton(R.string.general_no, dialogClickListener).show();			
		}	
	}

	public void leaveMultipleShares (ArrayList<Long> handleList){
		
		for (int i=0; i<handleList.size(); i++){
			MegaNode node = megaApi.getNodeByHandle(handleList.get(i));
			this.leaveIncomingShare(node);
		}
	}
	
	public void leaveIncomingShare (MegaNode n){
		log("leaveIncomingShare");
		//TODO 
//		ProgressDialog temp = null;
//		try{
//			temp = new ProgressDialog(this);
//			temp.setMessage(getString(R.string.leave_incoming_share)); 
//			temp.show();
//		}
//		catch(Exception e){
//			return;
//		}
//		statusDialog = temp;
		megaApi.remove(n);
	}
	
	public void removeAllSharingContacts (ArrayList<MegaShare> listContacts, MegaNode node){
		ProgressDialog temp = null;
		try{
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.remove_all_sharing)); 
			temp.show();
		}
		catch(Exception e){
			return;
		}
		statusDialog = temp;
		
		for(int j=0; j<listContacts.size();j++){
			String cMail = listContacts.get(j).getUser();
			if(cMail!=null){
				MegaUser c = megaApi.getContact(cMail);
				if (c != null){							
					megaApi.share(node, c, MegaShare.ACCESS_UNKNOWN, this);
				}
				else{
					isGetLink = false;
					megaApi.disableExport(node, this);
				}
			}
			else{
				isGetLink = false;
				megaApi.disableExport(node, this);
			}
		}	
		//TODO change the place
		try{
			statusDialog.dismiss();
		}
		catch(Exception e){
			return;
		}		
	}
	
	public void cameraUploadsClicked(){
		log("cameraUplaodsClicked");
		drawerItem = DrawerItem.CAMERA_UPLOADS;
		if (nV != null){
			Menu nVMenu = nV.getMenu();
			MenuItem hidden = nVMenu.findItem(R.id.navigation_item_camera_uploads);
			resetNavigationViewMenu(nVMenu);
			hidden.setChecked(true);
		}
		selectDrawerItemLollipop(drawerItem);		
	}
	
	public void acceptInvitationContact(MegaContactRequest c){
		log("acceptInvitationContact");
		megaApi.replyContactRequest(c, MegaContactRequest.REPLY_ACTION_ACCEPT, this);
	}
	
	public void declineInvitationContact(MegaContactRequest c){
		log("declineInvitationContact");
		megaApi.replyContactRequest(c, MegaContactRequest.REPLY_ACTION_DENY, this);
	}
	
	public void ignoreInvitationContact(MegaContactRequest c){
		log("ignoreInvitationContact");
		megaApi.replyContactRequest(c, MegaContactRequest.REPLY_ACTION_IGNORE, this);
	}
	
	public void reinviteContact(MegaContactRequest c){
		log("inviteContact");
		megaApi.inviteContact(c.getTargetEmail(), null, MegaContactRequest.INVITE_ACTION_REMIND, this);
	}
	
	public void removeInvitationContact(MegaContactRequest c){
		log("removeInvitationContact");
		megaApi.inviteContact(c.getTargetEmail(), null, MegaContactRequest.INVITE_ACTION_DELETE, this);
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
		selectDrawerItemLollipop(drawerItem);
		drawerLayout.openDrawer(Gravity.LEFT);
		firstTime = true;
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
		fragTransaction.commit();

		fragTransaction = getSupportFragmentManager().beginTransaction();
		fragTransaction.attach(currentFragment);
		fragTransaction.commit();
	}
	
	public void showOptionsPanel(MegaNode node){
		log("showOptionsPanel");
		if (drawerItem == DrawerItem.CLOUD_DRIVE){
			int index = viewPagerCDrive.getCurrentItem();
			if (index == 0){
				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);		
				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
				if (fbFLol != null){
					fbFLol.showOptionsPanel(node);
				}
			}
			else{
				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);		
				rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
				if (rbFLol != null){
					rbFLol.showOptionsPanel(node);
				}				
			}
		}
		else if(drawerItem == DrawerItem.SEARCH){
			if (sFLol != null){				
				sFLol.showOptionsPanel(node);				
			}
		}
		else if (drawerItem == DrawerItem.INBOX){
			if (iFLol != null){				
				iFLol.showOptionsPanel(node);				
			}
		}	
		else if (drawerItem == DrawerItem.SHARED_ITEMS){
			int index = viewPagerShares.getCurrentItem();
			if (index == 0){
				String cFTag = getFragmentTag(R.id.shares_tabs_pager, 0);		
				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
				if (inSFLol != null){
					inSFLol.showOptionsPanel(node);
				}
			}
			else{
				String cFTag = getFragmentTag(R.id.shares_tabs_pager, 1);		
				outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
				if (outSFLol != null){
					outSFLol.showOptionsPanel(node);
				}				
			}
		}
	}
	
	public void showOptionsPanel(MegaOffline node){
		log("showOptionsPanel-Offline");
		
		if (oFLol != null){				
			oFLol.showOptionsPanel(node);				
		}			
	}
	
	public void showOptionsPanel(MegaUser user){
		log("showOptionsPanel-Offline");
		
		String cFTag1 = getFragmentTag(R.id.contact_tabs_pager, 0);	
		log("Tag: "+ cFTag1);
		cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag1);
		if (cFLol != null){				
			cFLol.showOptionsPanel(user);				
		}			
	}
	
	public void showOptionsPanel(MegaContactRequest request){
		log("showOptionsPanel-MegaContactRequest");
		
		int index = viewPagerContacts.getCurrentItem();
		if (index == 2){
			String sRFTag1 = getFragmentTag(R.id.contact_tabs_pager, 2);	
			log("Tag: "+ sRFTag1);
			rRFLol = (ReceivedRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sRFTag1);
			if (rRFLol != null){				
				rRFLol.showOptionsPanel(request);				
			}
		}
		else if (index == 1){
			String sRFTag1 = getFragmentTag(R.id.contact_tabs_pager, 1);	
			log("Tag: "+ sRFTag1);
			sRFLol = (SentRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sRFTag1);
			if (sRFLol != null){				
				sRFLol.showOptionsPanel(request);				
			}				
		}	
	}
	
	private int getAvatarTextSize (float density){
		float textSize = 0.0f;
		
		if (density > 3.0){
			textSize = density * (DisplayMetrics.DENSITY_XXXHIGH / 72.0f);
		}
		else if (density > 2.0){
			textSize = density * (DisplayMetrics.DENSITY_XXHIGH / 72.0f);
		}
		else if (density > 1.5){
			textSize = density * (DisplayMetrics.DENSITY_XHIGH / 72.0f);
		}
		else if (density > 1.0){
			textSize = density * (72.0f / DisplayMetrics.DENSITY_HIGH / 72.0f);
		}
		else if (density > 0.75){
			textSize = density * (72.0f / DisplayMetrics.DENSITY_MEDIUM / 72.0f);
		}
		else{
			textSize = density * (72.0f / DisplayMetrics.DENSITY_LOW / 72.0f); 
		}
		
		return (int)textSize;
	}
	
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
	    // Raw height and width of image
	    final int height = options.outHeight;
	    final int width = options.outWidth;
	    int inSampleSize = 1;

	    if (height > reqHeight || width > reqWidth) {
	
	        final int halfHeight = height / 2;
	        final int halfWidth = width / 2;
	
	        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
	        // height and width larger than the requested height and width.
	        while ((halfHeight / inSampleSize) > reqHeight
	                && (halfWidth / inSampleSize) > reqWidth) {
	            inSampleSize *= 2;
	        }
	    }

	    return inSampleSize;
	}
	
	private void showOverquotaAlert(){
		
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
					if(upAF==null){
						upAF = new UpgradeAccountFragment();
						ft.replace(R.id.fragment_container, upAF, "upAF");
						drawerItem = DrawerItem.ACCOUNT;
						accountFragment=OVERQUOTA_ALERT;
						ft.commit();
					}
					else{			
						ft.replace(R.id.fragment_container, upAF, "upAF");
						drawerItem = DrawerItem.ACCOUNT;
						accountFragment=OVERQUOTA_ALERT;
						ft.commit();
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
	
	public void selectSortByContacts(int _orderContacts){
		this.orderContacts = _orderContacts;
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
	
	public void selectSortByOffline(int _orderOffline){
		log("selectSortByOffline");
		
		this.orderOffline = _orderOffline;
		
		if (oFLol != null){	
			oFLol.setOrder(orderOffline);
			if (orderOffline == MegaApiJava.ORDER_DEFAULT_ASC){
				oFLol.sortByNameAscending();
			}
			else{
				oFLol.sortByNameDescending();
			}
		}
	}
	
	public void selectSortByIncoming(int _orderIncoming){
		log("selectSortByIncoming");
		
		this.orderIncoming = _orderIncoming;
		
		if (inSFLol != null){	
			inSFLol.setOrder(orderIncoming);
			if (orderIncoming == MegaApiJava.ORDER_DEFAULT_ASC){
				inSFLol.sortByNameAscending();
			}
			else{
				inSFLol.sortByNameDescending();
			}
		}
	}
	
	public void selectSortByOutgoing(int _orderOutgoing){
		log("selectSortByOutgoing");
		
		this.orderOutgoing = _orderOutgoing;
		
		if (outSFLol != null){	
			outSFLol.setOrder(orderOutgoing);
			if (orderOutgoing == MegaApiJava.ORDER_DEFAULT_ASC){
				outSFLol.sortByNameAscending();
			}
			else{
				outSFLol.sortByNameDescending();
			}
		}
	}
	
	public void selectSortByCloudDrive(int _orderGetChildren){
		this.orderGetChildren = _orderGetChildren;
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
	
	public long getNumberOfSubscriptions(){
		if (cancelSubscription != null){
			cancelSubscription.setVisible(false);
		}
		if (numberOfSubscriptions > 0){
			if (cancelSubscription != null){
				if (drawerItem == DrawerItem.ACCOUNT){
					if (maFLol != null){
						cancelSubscription.setVisible(true);
					}
				}
			}
		}
		return numberOfSubscriptions;
	}
	
	public boolean IsFirstNavigationLevel(){
		return firstNavigationLevel;
	}
	
	public void setFirstNavigationLevel(boolean firstNavigationLevel){
		this.firstNavigationLevel = firstNavigationLevel;
	}
	
	public int getUsedPerc(){
		return usedPerc;
	}
	
	public void setPathNavigationOffline(String pathNavigation){
		this.pathNavigation = pathNavigation;
	}
	
	public void setParentHandleBrowser(long parentHandleBrowser){
		log("setParentHandleBrowser");
		
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
			case R.id.navigation_drawer_account_view:{
//				Snackbar.make(fragmentContainer, "MyAccount", Snackbar.LENGTH_LONG).show();
				/*drawerItem = DrawerItem.ACCOUNT;
				if (nV != null){
					Menu nVMenu = nV.getMenu();
					MenuItem hidden = nVMenu.findItem(R.id.navigation_item_hidden);
					hidden.setChecked(true);
				}
				selectDrawerItemLollipop(drawerItem);*/
				Intent myAccountIntent = new Intent(this, MyAccountMainActivityLollipop.class);
    			startActivity(myAccountIntent);
    			drawerLayout.closeDrawer(Gravity.LEFT);
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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		log("-------------------onActivityResult "+requestCode + "____" + resultCode);
		
		if (requestCode == REQUEST_CODE_GET){
			log("resultCode = " + resultCode);
			if (intent == null){
				log("INTENT NULL");
			}
			else{
				log("URI: " + intent.getData());
			}
		}
		
		if (requestCode == REQUEST_CODE_GET && resultCode == RESULT_OK) {
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
		else if(requestCode == SET_PIN && resultCode == RESULT_OK){
			log("Set PIN Ok");
			if(sttFLol!=null){
				sttFLol.afterSetPinLock();
			}
		}
		else if (requestCode == WRITE_SD_CARD_REQUEST_CODE && resultCode == RESULT_OK) {
			
			Uri treeUri = intent.getData();
			log("--------------Create the document : "+treeUri);
			
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
		else if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {
			log("requestCode == REQUEST_CODE_SELECT_FILE");
			if (intent == null) {			
				log("Return.....");
				return;						
			}
			
			if(!Util.isOnline(this)){
				Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
				return;
			}
			
			final String[] selectedContacts = intent.getStringArrayExtra("SELECTED_CONTACTS");
			final long fileHandle = intent.getLongExtra("SELECT", 0);	
			
			MegaNode node = megaApi.getNodeByHandle(fileHandle);
			if(node!=null)
			{
				sendToInbox=true;
				log("File to send: "+node.getName());
				for (int i=0;i<selectedContacts.length;i++){
            		MegaUser user= megaApi.getContact(selectedContacts[i]);		                    		
            		megaApi.sendFileToUser(node, user, this);
            	}
			}			
		}	
		else if (requestCode == REQUEST_CODE_SELECT_FOLDER && resultCode == RESULT_OK) {
			
			if (intent == null) {			
				log("Return.....");
				return;						
			}
			
			if(!Util.isOnline(this)){
				Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
				return;
			}
			
			final String[] selectedContacts = intent.getStringArrayExtra("SELECTED_CONTACTS");
			final long folderHandle = intent.getLongExtra("SELECT", 0);			
			
			final MegaNode parent = megaApi.getNodeByHandle(folderHandle);
			
			if (parent.isFolder()){
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
				dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
				final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
				dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {

						ProgressDialog temp = null;
						try{
							temp = new ProgressDialog(managerActivity);
							temp.setMessage(getString(R.string.context_sharing_folder));
							temp.show();
						}
						catch(Exception e){
							return;
						}
						statusDialog = temp;
						permissionsDialog.dismiss();						
						
						switch(item) {
						    case 0:{
		                    	for (int i=0;i<selectedContacts.length;i++){
		                    		MegaUser user= megaApi.getContact(selectedContacts[i]);		                    		
		                    		megaApi.share(parent, user, MegaShare.ACCESS_READ,managerActivity);
		                    	}
		                    	break;
		                    }
		                    case 1:{	                    	
		                    	for (int i=0;i<selectedContacts.length;i++){
		                    		MegaUser user= megaApi.getContact(selectedContacts[i]);		                    		
		                    		megaApi.share(parent, user, MegaShare.ACCESS_READWRITE,managerActivity);
		                    	}
		                        break;
		                    }
		                    case 2:{                   	
		                    	for (int i=0;i<selectedContacts.length;i++){
		                    		MegaUser user= megaApi.getContact(selectedContacts[i]);		                    		
		                    		megaApi.share(parent, user, MegaShare.ACCESS_FULL,managerActivity);
		                    	}
		                        break;
		                    }
		                }
					}
				});
				dialogBuilder.setTitle(getString(R.string.dialog_select_permissions));
				permissionsDialog = dialogBuilder.create();
				permissionsDialog.show();
//				Resources resources = permissionsDialog.getContext().getResources();
//				int alertTitleId = resources.getIdentifier("alertTitle", "id", "android");
//				TextView alertTitle = (TextView) permissionsDialog.getWindow().getDecorView().findViewById(alertTitleId);
//		        alertTitle.setTextColor(resources.getColor(R.color.mega));
//				int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
//				View titleDivider = permissionsDialog.getWindow().getDecorView().findViewById(titleDividerId);
//				if(titleDivider!=null){
//					titleDivider.setBackgroundColor(resources.getColor(R.color.mega));
//				}
			}
		}	
		else if (requestCode == REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK){
			
			if (intent == null) {			
				log("Return.....");
				return;						
			}
			
			if(!Util.isOnline(this)){
				Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
				return;
			}

			contactsData = intent.getStringArrayListExtra(ContactsExplorerActivityLollipop.EXTRA_CONTACTS);			
			megaContacts = intent.getBooleanExtra(ContactsExplorerActivityLollipop.EXTRA_MEGA_CONTACTS, true);
			
			final int multiselectIntent = intent.getIntExtra("MULTISELECT", -1);
			final int sentToInbox = intent.getIntExtra("SEND_FILE", -1);
			
			if (megaContacts){
				
				if(sentToInbox==0){
					//Just one folder to share
					if(multiselectIntent==0){
	
						final long nodeHandle = intent.getLongExtra(ContactsExplorerActivity.EXTRA_NODE_HANDLE, -1);
						final MegaNode node = megaApi.getNodeByHandle(nodeHandle);
						
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
						dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
						final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
						dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								ProgressDialog temp = null;
								try{
									temp = new ProgressDialog(managerActivity);
									temp.setMessage(getString(R.string.context_sharing_folder));
									temp.show();
								}
								catch(Exception e){
									return;
								}
								statusDialog = temp;
								permissionsDialog.dismiss();
								
								switch(item) {
				                    case 0:{
				                    	for (int i=0;i<contactsData.size();i++){
				                    		MegaUser u = megaApi.getContact(contactsData.get(i));		
				                    		log("Node: "+node.getName());
				                    		log("User: "+u.getEmail());
				                    		megaApi.share(node, u, MegaShare.ACCESS_READ, managerActivity);
				                    	}
				                    	break;
				                    }
				                    case 1:{
				                    	for (int i=0;i<contactsData.size();i++){
				                    		MegaUser u = megaApi.getContact(contactsData.get(i));
				                    		megaApi.share(node, u, MegaShare.ACCESS_READWRITE, managerActivity);
				                    	}
				                        break;
				                    }
				                    case 2:{
				                    	for (int i=0;i<contactsData.size();i++){
				                    		MegaUser u = megaApi.getContact(contactsData.get(i));
				                    		megaApi.share(node, u, MegaShare.ACCESS_FULL, managerActivity);
				                    	}		                    	
				                        break;
				                    }
				                }
							}
						});
						dialogBuilder.setTitle(getString(R.string.dialog_select_permissions));
						permissionsDialog = dialogBuilder.create();
						permissionsDialog.show();
//						Resources resources = permissionsDialog.getContext().getResources();
//						int alertTitleId = resources.getIdentifier("alertTitle", "id", "android");
//						TextView alertTitle = (TextView) permissionsDialog.getWindow().getDecorView().findViewById(alertTitleId);
//				        alertTitle.setTextColor(resources.getColor(R.color.mega));
//						int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
//						View titleDivider = permissionsDialog.getWindow().getDecorView().findViewById(titleDividerId);
//						if(titleDivider!=null){
//							titleDivider.setBackgroundColor(resources.getColor(R.color.mega));
//						}						
					}
					else if(multiselectIntent==1){
						//Several folder to share
						final long[] nodeHandles = intent.getLongArrayExtra(ContactsExplorerActivity.EXTRA_NODE_HANDLE);
						
							
						AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
						dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
						final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
						dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								ProgressDialog temp = null;
								try{
									temp = new ProgressDialog(managerActivity);
									temp.setMessage(getString(R.string.context_sharing_folder));
									temp.show();
								}
								catch(Exception e){
									return;
								}
								statusDialog = temp;
								permissionsDialog.dismiss();
								
								switch(item) {
				                    case 0:{
				                    	for (int i=0;i<contactsData.size();i++){
				                    		MegaUser u = megaApi.getContact(contactsData.get(i));			                    		
				                    		for(int j=0; j<nodeHandles.length;j++){						
				            					
				        						final MegaNode node = megaApi.getNodeByHandle(nodeHandles[j]);
				        						megaApi.share(node, u, MegaShare.ACCESS_READ, managerActivity);
				                    		}
				                    	}
				                    	break;
				                    }
				                    case 1:{
				                    	for (int i=0;i<contactsData.size();i++){
				                    		MegaUser u = megaApi.getContact(contactsData.get(i));
				                    		for(int j=0; j<nodeHandles.length;j++){						
				            					
				        						final MegaNode node = megaApi.getNodeByHandle(nodeHandles[j]);
				        						megaApi.share(node, u, MegaShare.ACCESS_READWRITE, managerActivity);
				                    		}
	//			                    		megaApi.share(node, u, MegaShare.ACCESS_READWRITE, managerActivity);
				                    	}
				                        break;
				                    }
				                    case 2:{
				                    	for (int i=0;i<contactsData.size();i++){
				                    		MegaUser u = megaApi.getContact(contactsData.get(i));
				                    		for(int j=0; j<nodeHandles.length;j++){						
				            					
				        						final MegaNode node = megaApi.getNodeByHandle(nodeHandles[j]);
				        						megaApi.share(node, u, MegaShare.ACCESS_FULL, managerActivity);
				                    		}
	//			                    		megaApi.share(node, u, MegaShare.ACCESS_FULL, managerActivity);
				                    	}		                    	
				                        break;
				                    }
				                }
							}
						});
						dialogBuilder.setTitle(getString(R.string.dialog_select_permissions));
						permissionsDialog = dialogBuilder.create();
						permissionsDialog.show();
//						Resources resources = permissionsDialog.getContext().getResources();
//						int alertTitleId = resources.getIdentifier("alertTitle", "id", "android");
//						TextView alertTitle = (TextView) permissionsDialog.getWindow().getDecorView().findViewById(alertTitleId);
//				        alertTitle.setTextColor(resources.getColor(R.color.mega));
//						int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
//						View titleDivider = permissionsDialog.getWindow().getDecorView().findViewById(titleDividerId);
//						if(titleDivider!=null){
//							titleDivider.setBackgroundColor(resources.getColor(R.color.mega));
//						}				
					}
				}
				else if (sentToInbox==1){
					//Send file
					final long nodeHandle = intent.getLongExtra(ContactsExplorerActivity.EXTRA_NODE_HANDLE, -1);
					final MegaNode node = megaApi.getNodeByHandle(nodeHandle);
					MegaUser u = megaApi.getContact(contactsData.get(0));
					megaApi.sendFileToUser(node, u, this);
				}

			}
			else{

				for (int i=0; i < contactsData.size();i++){
					String type = contactsData.get(i);
					if (type.compareTo(ContactsExplorerActivity.EXTRA_EMAIL) == 0){
						i++;
						Snackbar.make(fragmentContainer, getString(R.string.general_not_yet_implemented), Snackbar.LENGTH_LONG).show();
//						Toast.makeText(this, "Sharing a folder: An email will be sent to the email address: " + contactsData.get(i) + ".\n", Toast.LENGTH_LONG).show();
					}
					else if (type.compareTo(ContactsExplorerActivity.EXTRA_PHONE) == 0){
						i++;
						Snackbar.make(fragmentContainer, getString(R.string.general_not_yet_implemented), Snackbar.LENGTH_LONG).show();
//						Toast.makeText(this, "Sharing a folder: A Text Message will be sent to the phone number: " + contactsData.get(i) , Toast.LENGTH_LONG).show();
					}
				}

			}			
		}		
		else if (requestCode == REQUEST_CODE_GET_LOCAL && resultCode == RESULT_OK) {
			
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
		else if (requestCode == REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK) {
		
			if (intent == null) {			
				log("Return.....");
				return;						
			}
			
			if(!Util.isOnline(this)){
				Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
				return;
			}
			
			final long[] moveHandles = intent.getLongArrayExtra("MOVE_HANDLES");
			final long toHandle = intent.getLongExtra("MOVE_TO", 0);
//			final int totalMoves = moveHandles.length;
			
			MegaNode parent = megaApi.getNodeByHandle(toHandle);
			moveToRubbish = false;
			
			ProgressDialog temp = null;
			try{
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.context_moving));
				temp.show();
			}
			catch(Exception e){
				return;
			}
			statusDialog = temp;
			
			for(int i=0; i<moveHandles.length;i++){
				megaApi.moveNode(megaApi.getNodeByHandle(moveHandles[i]), parent, this);
			}
		}
		else if (requestCode == REQUEST_CODE_SELECT_COPY_FOLDER && resultCode == RESULT_OK){
			
			if (intent == null) {			
				log("Return.....");
				return;						
			}
			
			if(!Util.isOnline(this)){
				Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
				return;
			}
			
			final long[] copyHandles = intent.getLongArrayExtra("COPY_HANDLES");
			final long toHandle = intent.getLongExtra("COPY_TO", 0);
			final int totalCopy = copyHandles.length;
			
			ProgressDialog temp = null;
			try{
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.context_copying));
				temp.show();
			}
			catch(Exception e){
				return;
			}
			statusDialog = temp;
			
			MegaNode parent = megaApi.getNodeByHandle(toHandle);
			for(int i=0; i<copyHandles.length;i++){
				megaApi.copyNode(megaApi.getNodeByHandle(copyHandles[i]), parent, this);
			}
		}
		else if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
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
			
			downloadTo (parentPath, url, size, hashes);
			Util.showToast(this, R.string.download_began);
		}
		else if (requestCode == REQUEST_CODE_REFRESH && resultCode == RESULT_OK) {
			
			if (intent == null) {			
				log("Return.....");
				return;						
			}
			
			if (drawerItem == DrawerItem.CLOUD_DRIVE){
				parentHandleBrowser = intent.getLongExtra("PARENT_HANDLE", -1);
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
				if (parentNode != null){
					if (fbFLol != null){						
						ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderGetChildren);
						fbFLol.setNodes(nodes);
						fbFLol.getRecyclerView().invalidate();						
					}
				}
				else{
					if (fbFLol != null){						
						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRootNode(), orderGetChildren);
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
		else if (requestCode == TAKE_PHOTO_CODE){
			log("Entrooo en requestCode");
			if(resultCode == Activity.RESULT_OK){
				
				log("REcibo el intent OOOOKK");
				Intent intentPicture = new Intent(this, SecureSelfiePreviewActivityLollipop.class);			
				startActivity(intentPicture);
			}
			else{
				log("REcibo el intent con error");
			}			

	    }
		else if (requestCode == REQUEST_CODE_SORT_BY && resultCode == RESULT_OK){
			
			if (intent == null) {			
				log("Return.....");
				return;						
			}
			
			orderGetChildren = intent.getIntExtra("ORDER_GET_CHILDREN", 1);
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
	            drawerItem = DrawerItem.CLOUD_DRIVE;
//	            Toast.makeText(this, "HURRAY!: ORDERID: **__" + orderId + "__**", Toast.LENGTH_LONG).show();
	            log("HURRAY!: ORDERID: **__" + orderId + "__**");
	        }
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
	
	/*
	 * Background task to process files for uploading
	 */
	private class FilePrepareTask extends AsyncTask<Intent, Void, List<ShareInfo>> {
		Context context;
		
		FilePrepareTask(Context context){
			log("FilePrepareTask::FilePrepareTask");
			this.context = context;
		}
		
		@Override
		protected List<ShareInfo> doInBackground(Intent... params) {
			log("FilePrepareTask::doInBackGround");
			return ShareInfo.processIntent(params[0], context);
		}

		@Override
		protected void onPostExecute(List<ShareInfo> info) {
			log("FilePrepareTask::onPostExecute");
			filePreparedInfos = info;
			onIntentProcessed();
		}
	}
	
	/*
	 * Background task to clear cache
	 */
	private class ClearCacheTask extends AsyncTask<String, Void, String> {
		Context context;
		
		ClearCacheTask(Context context){
			this.context = context;
		}
		
		@Override
		protected String doInBackground(String... params) {
			log("doInBackground-Async Task ClearCacheTask");
			
			Util.clearCache(context);
			String size = Util.getCacheSize(context);
			return size;
		}		
		
		@Override
		protected void onPostExecute(String size) {
			log("ClearCacheTask::onPostExecute");
			if(sttFLol!=null){
					sttFLol.setCacheSize(size);	
			}
		}
	}
	
	/*
	 * Background task to clear offline files
	 */
	private class ClearOfflineTask extends AsyncTask<String, Void, String> {
		Context context;
		
		ClearOfflineTask(Context context){
			this.context = context;
		}
		
		@Override
		protected String doInBackground(String... params) {
			log("doInBackground-Async Task ClearOfflineTask");
			
			Util.clearOffline(context);
			dbH.clearOffline();
			String size = Util.getOfflineSize(context);
			return size;
		}		
		
		@Override
		protected void onPostExecute(String size) {
			log("ClearOfflineTask::onPostExecute");
			if(sttFLol!=null){
					sttFLol.setOfflineSize(size);				
			}
		}
	}
	
	/*
	 * Background task to calculate the size of offline folder
	 */
	private class GetOfflineSizeTask extends AsyncTask<String, Void, String> {
		Context context;
		
		GetOfflineSizeTask(Context context){
			this.context = context;
		}
		
		@Override
		protected String doInBackground(String... params) {
			log("doInBackground-Async Task GetOfflineSizeTask");
			
			String size = Util.getOfflineSize(context);
			return size;
		}		
		
		@Override
		protected void onPostExecute(String size) {
			log("GetOfflineSizeTask::onPostExecute");
			if(sttFLol!=null){
					sttFLol.setOfflineSize(size);	
			}
		}
	}
	
	/*
	 * Background task to calculate the size of cache folder
	 */
	private class GetCacheSizeTask extends AsyncTask<String, Void, String> {
		Context context;
		
		GetCacheSizeTask(Context context){
			this.context = context;
		}
		
		@Override
		protected String doInBackground(String... params) {
			log("doInBackground-Async Task GetCacheSizeTask");
			
			String size = Util.getCacheSize(context);
			return size;
		}		
		
		@Override
		protected void onPostExecute(String size) {
			log("GetCacheSizeTask::onPostExecute");
			if(sttFLol!=null){
					sttFLol.setCacheSize(size);			
			}
		}
	}
	
	public void taskGetSizeCache (){
		log("taskGetSizeCache");
		GetCacheSizeTask getCacheSizeTask = new GetCacheSizeTask(this);
		getCacheSizeTask.execute();
	}
	
	public void taskGetSizeOffline (){
		log("taskGetSizeOffline");
		GetOfflineSizeTask getOfflineSizeTask = new GetOfflineSizeTask(this);
		getOfflineSizeTask.execute();
	}
	
	public void taskClearCache (){
		ClearCacheTask clearCacheTask = new ClearCacheTask(this);
		clearCacheTask.execute();
	}
	
	public void taskClearOffline (){
		ClearOfflineTask clearOfflineTask = new ClearOfflineTask(this);
		clearOfflineTask.execute();
//		dbH.clearOffline();
	}
	
	void resetNavigationViewMenu(Menu menu){
		MenuItem mi = menu.findItem(R.id.navigation_item_cloud_drive);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.cloud_drive_grey));
		}
		mi = menu.findItem(R.id.navigation_item_saved_for_offline);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.saved_for_offline_grey));
		}
		mi = menu.findItem(R.id.navigation_item_camera_uploads);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.camera_uploads_grey));
		}
		mi = menu.findItem(R.id.navigation_item_inbox);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.inbox_grey));
		}
		mi = menu.findItem(R.id.navigation_item_shared_items);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.shared_items_grey));
		}
		mi = menu.findItem(R.id.navigation_item_contacts);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.contacts_grey));
		}
		mi = menu.findItem(R.id.navigation_item_settings);
		if (mi != null){
			mi.setIcon(getResources().getDrawable(R.drawable.settings_grey));
		}
	}
	
	/*
	 * Handle processed upload intent
	 */
	public void onIntentProcessed() {
		log("onIntentProcessedLollipop");
		List<ShareInfo> infos = filePreparedInfos;
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
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish: " + request.getRequestString());
		
		if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			log("fecthnodes request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_GET_USER_DATA){
			if (e.getErrorCode() == MegaError.API_OK){
				nVDisplayName.setText(request.getName());
			}
		}
		else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER){
			boolean avatarExists = false;
			if (e.getErrorCode() == MegaError.API_OK){
				
				File avatar = null;
				if (getExternalCacheDir() != null){
					avatar = new File(getExternalCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
				}
				else{
					avatar = new File(getCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
				}
				Bitmap imBitmap = null;
				if (avatar.exists()){
					if (avatar.length() > 0){
						BitmapFactory.Options options = new BitmapFactory.Options();
						options.inJustDecodeBounds = true;
						BitmapFactory.decodeFile(avatar.getAbsolutePath(), options);
						int imageHeight = options.outHeight;
						int imageWidth = options.outWidth;
						String imageType = options.outMimeType;
						
						// Calculate inSampleSize
					    options.inSampleSize = calculateInSampleSize(options, 250, 250);
					    
					    // Decode bitmap with inSampleSize set
					    options.inJustDecodeBounds = false;

						imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), options);
						if (imBitmap == null) {
							avatar.delete();
						}
						else{
							avatarExists = true;
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
				}
				
				if(request.getParamType()==1){
					log("(1)request.getText(): "+request.getText());
					nameText=request.getText();
					name=true;
				}
				else if(request.getParamType()==2){
					log("(2)request.getText(): "+request.getText());
					firstNameText = request.getText();
					firstName = true;
				}
				if(name && firstName){
					String fullName = nameText + " " + firstNameText;
					if (fullName.trim().length() > 0){
						nVDisplayName.setText(nameText+" "+firstNameText);
						name= false;
						firstName = false;
					}
				}
			}
			else{
				log("ERRR:R " + e.getErrorString() + "_" + e.getErrorCode());
			}
			
			log("avatar user downloaded");
		}
		else if (request.getType() == MegaRequest.TYPE_ACCOUNT_DETAILS){
			log ("account_details request");
			if (e.getErrorCode() == MegaError.API_OK){
				
				accountInfo = request.getMegaAccountDetails();				
				
				accountType = accountInfo.getProLevel();
				
				switch (accountType){
					case 0:{
						levelAccountDetails = -1;
						break;
					}
					case 1:{
						levelAccountDetails = 1;
						break;
					}
					case 2:{
						levelAccountDetails = 2;
						break;
					}
					case 3:{
						levelAccountDetails = 3;
						break;
					}
					case 4:{
						levelAccountDetails = 0;
						break;
					}
				}

				accountDetailsFinished = true;
				
				if (inventoryFinished){
					if (levelAccountDetails < levelInventory){
						if (maxP != null){
							megaApi.submitPurchaseReceipt(maxP.getOriginalJson(), this);
						}
					}
				}
				
				long totalStorage = accountInfo.getStorageMax();
				long usedStorage = accountInfo.getStorageUsed();;
				boolean totalGb = false;				
		        
		        usedPerc = 0;
		        if (totalStorage != 0){
		        	usedPerc = (int)((100 * usedStorage) / totalStorage);
		        }
		        usedSpacePB.setProgress(usedPerc);
				
				totalStorage = ((totalStorage / 1024) / 1024) / 1024;
				String total = "";
				if (totalStorage >= 1024){
					totalStorage = totalStorage / 1024;
					total = total + totalStorage + " TB";
				}
				else{
					 total = total + totalStorage + " GB";
					 totalGb = true;
				}

				usedStorage = ((usedStorage / 1024) / 1024) / 1024;
				String used = "";
				if(totalGb){
					usedGbStorage = usedStorage;
					used = used + usedStorage + " GB";					
				}
				else{
					if (usedStorage >= 1024){
						usedGbStorage = usedStorage;
						usedStorage = usedStorage / 1024;

						used = used + usedStorage + " TB";
					}
					else{
						usedGbStorage = usedStorage;
						used = used + usedStorage + " GB";
					}
				}
		      
//				String usedSpaceString = getString(R.string.used_space, used, total);
				usedSpaceTV.setText(used);
				totalSpaceTV.setText(total);

		        if (usedPerc < 90){
		        	usedSpacePB.setProgressDrawable(getResources().getDrawable(R.drawable.custom_progress_bar_horizontal_ok));
//		        	wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.used_space_ok)), 0, used.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		        	usedSpaceWarning.setVisibility(View.INVISIBLE);
		        }
		        else if ((usedPerc >= 90) && (usedPerc <= 95)){
		        	usedSpacePB.setProgressDrawable(getResources().getDrawable(R.drawable.custom_progress_bar_horizontal_warning));
//		        	wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.used_space_warning)), 0, used.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		        	usedSpaceWarning.setVisibility(View.VISIBLE);
		        }
		        else{
		        	if (usedPerc > 100){
			        	usedPerc = 100;			        	
			        }
		        	usedSpacePB.setProgressDrawable(getResources().getDrawable(R.drawable.custom_progress_bar_horizontal_exceed));    
//		        	wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.used_space_exceed)), 0, used.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		        	usedSpaceWarning.setVisibility(View.VISIBLE);
		        }      
		        
//		        wordtoSpan.setSpan(new RelativeSizeSpan(1.5f), 0, used.length() - 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		        wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.navigation_drawer_mail)), used.length() + 1, used.length() + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		        wordtoSpan.setSpan(new RelativeSizeSpan(1.5f), used.length() + 3, used.length() + 3 + total.length() - 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		        usedSpaceTV.setText(wordtoSpan);	
		        
		        log("onRequest TYPE_ACCOUNT_DETAILS: "+usedPerc);

		        if(drawerItem==DrawerItem.CLOUD_DRIVE){
		        	if (usedPerc > 95){
		        		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
						ft.detach(fbFLol);
						ft.attach(fbFLol);
						ft.commitAllowingStateLoss();
		        	}
		        }
			}
		}
		else if (request.getType() == MegaRequest.TYPE_GET_PAYMENT_METHODS){
			if (e.getErrorCode() == MegaError.API_OK){
				paymentBitSet = Util.convertToBitSet(request.getNumber());
			}
		}
		else if(request.getType() == MegaRequest.TYPE_CREDIT_CARD_QUERY_SUBSCRIPTIONS){
			if (e.getErrorCode() == MegaError.API_OK){
				numberOfSubscriptions = request.getNumber();
				log("NUMBER OF SUBS: " + numberOfSubscriptions);
				if (cancelSubscription != null){
					cancelSubscription.setVisible(false);
				}
				if (numberOfSubscriptions > 0){
					if (cancelSubscription != null){
						if (drawerItem == DrawerItem.ACCOUNT){
							if (maFLol != null){
								cancelSubscription.setVisible(true);
							}
						}
					}
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_CREDIT_CARD_CANCEL_SUBSCRIPTIONS){
			if (e.getErrorCode() == MegaError.API_OK){
				Snackbar.make(fragmentContainer, getString(R.string.cancel_subscription_ok), Snackbar.LENGTH_LONG).show();
			}
			else{
				Snackbar.make(fragmentContainer, getString(R.string.cancel_subscription_error), Snackbar.LENGTH_LONG).show();
			}
			megaApi.creditCardQuerySubscriptions(this);
		}
		else if (request.getType() == MegaRequest.TYPE_LOGOUT){
			log("logout finished");
//			if (request.getType() == MegaRequest.TYPE_LOGOUT){
//				log("type_logout");
//				if (e.getErrorCode() == MegaError.API_ESID){
//					log("calling ManagerActivityLollipop.logout");
//					MegaApiAndroid megaApi = app.getMegaApi(); 
//					ManagerActivityLollipop.logout(managerActivity, app, megaApi, false);
//				}
//			}
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE_CONTACT){
			
			if (e.getErrorCode() == MegaError.API_OK){
			
				if(drawerItem==DrawerItem.CONTACTS){
					if (cFLol != null){
						cFLol.notifyDataSetChanged();
					}
				}	
			}
			else{
				log("Termino con error");
			}
		}
		else if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT){	
			log("MegaRequest.TYPE_INVITE_CONTACT finished: "+request.getNumber());

			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			//Update the fragments
			String sRFTag1 = getFragmentTag(R.id.contact_tabs_pager, 1);	
			log("Tag: "+ sRFTag1);
			sRFLol = (SentRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sRFTag1);
			if (sRFLol != null){	
				log("sRFLol != null");
				sRFLol.setContactRequests();
			}
			
			if(request.getNumber()==MegaContactRequest.INVITE_ACTION_REMIND){
				Snackbar.make(fragmentContainer, getString(R.string.context_contact_invitation_resent), Snackbar.LENGTH_LONG).show();
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					
					if(request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD)
					{
						Snackbar.make(fragmentContainer, getString(R.string.context_contact_added), Snackbar.LENGTH_LONG).show();			
					}
					else if(request.getNumber()==MegaContactRequest.INVITE_ACTION_DELETE)
					{
						Snackbar.make(fragmentContainer, getString(R.string.context_contact_invitation_deleted), Snackbar.LENGTH_LONG).show();	
					}
//					else
//					{
//						Toast.makeText(this, getString(R.string.context_contact_invitation_resent), Toast.LENGTH_LONG).show();					
//					}				
				}
				else{
					if(e.getErrorCode()==MegaError.API_EEXIST)
					{
						Snackbar.make(fragmentContainer, getString(R.string.context_contact_already_exists), Snackbar.LENGTH_LONG).show();	
					}
					else{
						Snackbar.make(fragmentContainer, getString(R.string.general_error), Snackbar.LENGTH_LONG).show();	
					}				
					log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_REPLY_CONTACT_REQUEST){	
			log("MegaRequest.TYPE_REPLY_CONTACT_REQUEST finished: "+request.getType());
			
			if (e.getErrorCode() == MegaError.API_OK){
				Snackbar.make(fragmentContainer, getString(R.string.context_invitacion_reply), Snackbar.LENGTH_LONG).show();	
			}
			else{
				Snackbar.make(fragmentContainer, getString(R.string.general_error), Snackbar.LENGTH_LONG).show();	
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
        					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbFLol.getParentHandle()), orderGetChildren);
    						rbFLol.setNodes(nodes);
    						rbFLol.getRecyclerView().invalidate();
            			}	

        				//Cloud Drive
        				if (fbFLol != null){
        					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderGetChildren);
    						fbFLol.setNodes(nodes);
    						fbFLol.getRecyclerView().invalidate();
        				}	        			
					}
					else{
						int index = viewPagerCDrive.getCurrentItem();
	        			log("----------------------------------------INDEX: "+index);
	        			if(index==1){
	        				//Rubbish bin
	        				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);		
	        				rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	        				if (rbFLol != null){
	        					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbFLol.getParentHandle()), orderGetChildren);
	    						rbFLol.setNodes(nodes);
	    						rbFLol.getRecyclerView().invalidate();
	            			}		
	        			}
	        			else{
	        				//Cloud Drive
	        				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);		
	        				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	        				if (fbFLol != null){
	        					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderGetChildren);
	    						fbFLol.setNodes(nodes);
	    						fbFLol.getRecyclerView().invalidate();
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
	        					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbFLol.getParentHandle()), orderGetChildren);
	    						rbFLol.setNodes(nodes);
	    						rbFLol.getRecyclerView().invalidate();
	            			}	
						}
						else{
							//Refresh Cloud Drive
							String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);		
	        				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	        				if (fbFLol != null){
	        					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderGetChildren);
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
        					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbFLol.getParentHandle()), orderGetChildren);
    						rbFLol.setNodes(nodes);
    						rbFLol.getRecyclerView().invalidate();
            			}	
					}
					else{
						//Refresh Cloud Drive
						String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);		
        				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
        				if (fbFLol != null){
        					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderGetChildren);
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
        					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderGetChildren);
    						fbFLol.setNodes(nodes);
    						fbFLol.getRecyclerView().invalidate();
        				}						
					}
				}	
			}	
			
			if (moveToRubbish){
				if (e.getErrorCode() == MegaError.API_OK){
					Snackbar.make(fragmentContainer, getString(R.string.context_correctly_moved_to_rubbish), Snackbar.LENGTH_LONG).show();	
				}
				else{
					Snackbar.make(fragmentContainer, getString(R.string.context_no_moved), Snackbar.LENGTH_LONG).show();	
				}
				moveToRubbish = false;
				log("move to rubbish request finished");
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					Snackbar.make(fragmentContainer, getString(R.string.context_correctly_moved), Snackbar.LENGTH_LONG).show();	
				}
				else{
					Snackbar.make(fragmentContainer, getString(R.string.context_no_moved), Snackbar.LENGTH_LONG).show();	
				}
			
				log("move nodes request finished");
			}
		}
		else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS){
			if (e.getErrorCode() == MegaError.API_OK) {
				if (tFLol != null){
					if (drawerItem == DrawerItem.TRANSFERS){
						if (tranfersPaused){
							log("show PLAY button");
	    					pauseTransfersMenuIcon.setVisible(false);
	    					playTransfersMenuIcon.setVisible(true);
		    				tFLol.setPause(true);
						}
						else{
							log("show PAUSE button");
	    					pauseTransfersMenuIcon.setVisible(true);
	    					playTransfersMenuIcon.setVisible(false);
		    				tFLol.setPause(false);
						}		
					}
				}				
			}
		}
		else if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFER){
			if (e.getErrorCode() == MegaError.API_OK){
				totalSizeToDownload = 0;
				tL = megaApi.getTransfers();				
				if (tL != null){
					log("Hide Transfer ProgressBar: "+tL.size());
					if(tL.size()<=0){
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
					}
				}
				else{
					log("megaApi Transfers NULL - Hide Transfer ProgressBar: ");
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

				}				
				
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
		}
		else if (request.getType() == MegaRequest.TYPE_KILL_SESSION){
			if (e.getErrorCode() == MegaError.API_OK){
				Snackbar.make(fragmentContainer, getString(R.string.success_kill_all_sessions), Snackbar.LENGTH_LONG).show();
			}
			else
			{
				log("error when killing sessions: "+e.getErrorString());
				Snackbar.make(fragmentContainer, getString(R.string.error_kill_all_sessions), Snackbar.LENGTH_LONG).show();
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
						Snackbar.make(fragmentContainer, getString(R.string.context_correctly_removed), Snackbar.LENGTH_LONG).show();
					}
				}
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
    							ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderGetChildren);
    							rbFLol.setNodes(nodes);
    							rbFLol.getRecyclerView().invalidate();
    							aB.setTitle(getString(R.string.section_rubbish_bin));	
    							aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
    							this.firstNavigationLevel = true;
    						}
    						else{
    							ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbFLol.getParentHandle()), orderGetChildren);
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
        					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderGetChildren);
    						fbFLol.setNodes(nodes);
    						fbFLol.getRecyclerView().invalidate();
        				}
        			}
				}	
			}
			else{
				Snackbar.make(fragmentContainer, getString(R.string.context_no_removed), Snackbar.LENGTH_LONG).show();
			}
			log("remove request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_EXPORT){
			MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				
				if (isGetLink){
					final String link = request.getLink();
					
					AlertDialog getLinkDialog;
					AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);					
		            builder.setMessage(link);
					builder.setTitle(getString(R.string.context_get_link_menu));
//					
					builder.setPositiveButton(getString(R.string.context_send_link), new android.content.DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(Intent.ACTION_SEND);
							intent.setType("text/plain");
							intent.putExtra(Intent.EXTRA_TEXT, link);
							startActivity(Intent.createChooser(intent, getString(R.string.context_get_link)));
						}
					});
					
					builder.setNegativeButton(getString(R.string.context_copy_link), new android.content.DialogInterface.OnClickListener() {
						
						@SuppressLint("NewApi") 
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
							    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
							    clipboard.setText(link);
							} else {
							    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
							    android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", link);
					            clipboard.setPrimaryClip(clip);
							}
							Snackbar.make(fragmentContainer, getString(R.string.file_properties_get_link), Snackbar.LENGTH_LONG).show();
						}
					});
					
					getLinkDialog = builder.create();
					getLinkDialog.show();
//					Util.brandAlertDialog(getLinkDialog);
				}
			}
			else{
				Snackbar.make(fragmentContainer, getString(R.string.context_no_link), Snackbar.LENGTH_LONG).show();
			}
			log("export request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_RENAME){
			
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Snackbar.make(fragmentContainer, getString(R.string.context_correctly_renamed), Snackbar.LENGTH_LONG).show();
				if (drawerItem == DrawerItem.CLOUD_DRIVE){
					
					int index = viewPagerCDrive.getCurrentItem();
        			log("----------------------------------------INDEX: "+index);
        			if(index==0){
        		        //Cloud Drive
        				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);		
        				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
        				if (fbFLol != null){					
    						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderGetChildren);
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
				Snackbar.make(fragmentContainer, getString(R.string.context_no_renamed), Snackbar.LENGTH_LONG).show();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			log("TYPE_COPY");
			if(sendToInbox){
				log("sendToInbox");
				if (drawerItem == DrawerItem.INBOX||drawerItem == DrawerItem.CLOUD_DRIVE||drawerItem == DrawerItem.CONTACTS){
					sendToInbox=false;
					if (e.getErrorCode() == MegaError.API_OK){
						Snackbar.make(fragmentContainer, getString(R.string.context_correctly_sent), Snackbar.LENGTH_LONG).show();
					}
					else if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
						log("OVERQUOTA ERROR: "+e.getErrorCode());
						showOverquotaAlert();
					}
					else
					{
						Snackbar.make(fragmentContainer, getString(R.string.context_no_sent), Snackbar.LENGTH_LONG).show();
					}
				}				
			}
			else{
				try { 
					statusDialog.dismiss();	
				} 
				catch (Exception ex) {}
				
				if (e.getErrorCode() == MegaError.API_OK){
					Snackbar.make(fragmentContainer, getString(R.string.context_correctly_copied), Snackbar.LENGTH_LONG).show();
					if (drawerItem == DrawerItem.CLOUD_DRIVE){
						
						int index = viewPagerCDrive.getCurrentItem();
	        			log("----------------------------------------INDEX: "+index);
	        			if(index==1){
	        				//Rubbish bin
	        				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 1);		
	        				rbFLol = (RubbishBinFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	        				if (rbFLol != null){						
								ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbFLol.getParentHandle()), orderGetChildren);
								rbFLol.setNodes(nodes);
								rbFLol.getRecyclerView().invalidate();
							}
	        			}
	        			else{
	        				//Cloud Drive
	        				String cFTag = getFragmentTag(R.id.cloud_drive_tabs_pager, 0);		
	        				fbFLol = (FileBrowserFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
	        				if (fbFLol != null){						
								ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderGetChildren);
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
						Snackbar.make(fragmentContainer, getString(R.string.context_no_copied), Snackbar.LENGTH_LONG).show();
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
				Snackbar.make(fragmentContainer, getString(R.string.context_folder_created), Snackbar.LENGTH_LONG).show();
				if (fbFLol != null){
					if (drawerItem == DrawerItem.CLOUD_DRIVE){
						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderGetChildren);
						fbFLol.setNodes(nodes);
						fbFLol.getRecyclerView().invalidate();
					}
				}
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
			}
			else{
				log("ERROR MegaRequest.TYPE_SHARE");
			}
		}
		else if (request.getType() == MegaRequest.TYPE_SUBMIT_PURCHASE_RECEIPT){
			if (e.getErrorCode() == MegaError.API_OK){
//				Toast.makeText(this, "PURCHASE CORRECT!", Toast.LENGTH_LONG).show();
				drawerItem = DrawerItem.CLOUD_DRIVE;
				selectDrawerItemLollipop(drawerItem);
			}
			else{
				Snackbar.make(fragmentContainer, "PURCHASE WRONG: " + e.getErrorString() + " (" + e.getErrorCode() + ")", Snackbar.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString());
	}
	
	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
		log("onUsersUpdateLollipop");
		String cFTag = getFragmentTag(R.id.contact_tabs_pager, 0);		
		cFLol = (ContactsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag);
		if (cFLol != null){
			if (drawerItem == DrawerItem.CONTACTS){					
				cFLol.updateView();
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
			if (fbFLol != null){
			
				if (fbFLol.isVisible()){
					ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbFLol.getParentHandle()), orderGetChildren);
					fbFLol.setNodes(nodes);
					fbFLol.setContentText();
					fbFLol.getRecyclerView().invalidate();
				}
			}
			if (rbFLol != null){
				
				if (isClearRubbishBin){
					isClearRubbishBin = false;
					parentHandleRubbish = megaApi.getRubbishNode().getHandle();
					aB.setTitle(getString(R.string.section_rubbish_bin));	
					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
					this.firstNavigationLevel = true;

					if(rbFLol.isVisible())
					{
						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRubbishNode(), orderGetChildren);
						rbFLol.setParentHandle(megaApi.getRubbishNode().getHandle());
						rbFLol.setNodes(nodes);
						rbFLol.getRecyclerView().invalidate();
					}
				}
				else{
					if(rbFLol.isVisible())
					{
						ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbFLol.getParentHandle()), orderGetChildren);
						rbFLol.setNodes(nodes);
						rbFLol.setContentText();
						rbFLol.getRecyclerView().invalidate();
					}
				}				
			}
		}
		if (drawerItem == DrawerItem.INBOX){
			log("INBOX shown");
			if (iFLol != null){
				iFLol.refresh();
//				iFLol.getListView().invalidateViews();
			}
		}		
		
		if (drawerItem == DrawerItem.SHARED_ITEMS){
			int index = viewPagerShares.getCurrentItem();
			if(index==1){				
				//OUTGOING				
				String cFTag2 = getFragmentTag(R.id.shares_tabs_pager, 1);		
				log("DrawerItem.SHARED_ITEMS Tag: "+ cFTag2);
				outSFLol = (OutgoingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag2);
				if (outSFLol != null){					
					MegaNode node = megaApi.getNodeByHandle(parentHandleOutgoing);
					if (node != null){
						outSFLol.setNodes(megaApi.getChildren(node, orderGetChildren));
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
			else{			
				//InCOMING
				String cFTag1 = getFragmentTag(R.id.shares_tabs_pager, 0);	
				log("DrawerItem.SHARED_ITEMS Tag: "+ cFTag1);
				inSFLol = (IncomingSharesFragmentLollipop) getSupportFragmentManager().findFragmentByTag(cFTag1);
				if (inSFLol != null){					
					MegaNode node = megaApi.getNodeByHandle(parentHandleIncoming);
					if (node != null){
						inSFLol.setNodes(megaApi.getChildren(node, orderGetChildren));
						aB.setTitle(node.getName());
    					aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
    					firstNavigationLevel = false;
					}
					else{
						inSFLol.refresh();
						aB.setTitle(getResources().getString(R.string.section_shared_items));
    					aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
    					firstNavigationLevel = true;
					}		
				}				
			}	
		}
		if (drawerItem == DrawerItem.CAMERA_UPLOADS){
			if (cuF != null){			
				if(cuF.isAdded()){
					long cameraUploadHandle = cuF.getPhotoSyncHandle();
					MegaNode nps = megaApi.getNodeByHandle(cameraUploadHandle);
					log("cameraUploadHandle: " + cameraUploadHandle);
					if (nps != null){
						log("nps != null");
						ArrayList<MegaNode> nodes = megaApi.getChildren(nps, MegaApiJava.ORDER_MODIFICATION_DESC);
						cuF.setNodes(nodes);
					}
				}				
			}
		}
		if (cFLol != null){
			if (drawerItem == DrawerItem.CONTACTS){
				log("Share finish");
				cFLol.updateView();
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
	public void onContactRequestsUpdate(MegaApiJava api,
			ArrayList<MegaContactRequest> requests) {
		log("---------------------onContactRequestsUpdate");
		// TODO Auto-generated method stub
		if (drawerItem == DrawerItem.CONTACTS){
			String sRFTag1 = getFragmentTag(R.id.contact_tabs_pager, 1);	
			log("Tag: "+ sRFTag1);
			sRFLol = (SentRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(sRFTag1);
			if (sRFLol != null){	
				log("sRFLol != null");
				sRFLol.setContactRequests();
			}	
			String rRFTag2 = getFragmentTag(R.id.contact_tabs_pager, 2);	
			log("Tag: "+ rRFTag2);
			rRFLol = (ReceivedRequestsFragmentLollipop) getSupportFragmentManager().findFragmentByTag(rRFTag2);
			if (rRFLol != null){					
				rRFLol.setContactRequests();
			}		
		}
	}	
	
	public void setPauseIconVisible(boolean visible){
		log("setPauseIconVisible");
		pauseTransfersMenuIcon.setVisible(true);
		playTransfersMenuIcon.setVisible(false);
	}
	
	public void hideTransfersIcons(){
		log("hideTransfersIcons");
		pauseTransfersMenuIcon.setVisible(false);
		playTransfersMenuIcon.setVisible(false);
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
		
		log("onTransferStart: " + transfer.getFileName() + " - " + transfer.getTag());
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError e) {
		log("onTransferFinish"); 
		
		HashMap<Long, MegaTransfer> mTHash = new HashMap<Long, MegaTransfer>();
		
		if (e.getErrorCode() == MegaError.API_OK) {
			long currentSizeDownloaded = transfersDownloadedSize.get(transfer.getTag());
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
		}
		else if(e.getErrorCode() == MegaError.API_EINCOMPLETE){
			
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
		}

		//Update transfer list
		tL = megaApi.getTransfers();
		
		if (tL != null){
			log("Hide Transfer ProgressBar: "+tL.size());
			if(tL.size()<=0){
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
			}
		}
		else{
			log("megaApi Transfers NULL - Hide Transfer ProgressBar: ");
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

		log("onTransferFinish: " + transfer.getFileName() + " - " + transfer.getTag());
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
		log("onTransferUpdate: " + transfer.getFileName() + " - " + transfer.getTag());
		
		long currentSizeDownloaded = 0;
		if (transfersDownloadedSize.get(transfer.getTag()) != null){
			currentSizeDownloaded = transfersDownloadedSize.get(transfer.getTag());
		}
		totalSizeDownloaded += (transfer.getTransferredBytes()-currentSizeDownloaded);
		transfersDownloadedSize.put(transfer.getTag(), transfer.getTransferredBytes());
		
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
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer,
			byte[] buffer) {
		log("onTransferData");
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
}
