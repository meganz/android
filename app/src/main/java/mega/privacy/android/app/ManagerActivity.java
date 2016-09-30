package mega.privacy.android.app;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.MediaStore;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils.TruncateAt;
import android.text.format.Time;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

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

import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.billing.IabHelper;
import mega.privacy.android.app.utils.billing.IabResult;
import mega.privacy.android.app.utils.billing.Inventory;
import mega.privacy.android.app.utils.billing.Purchase;
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

public class ManagerActivity extends PinActivity implements OnItemClickListener, OnClickListener, MegaRequestListenerInterface, MegaGlobalListenerInterface, MegaTransferListenerInterface {
	
	public enum DrawerItem {
		CLOUD_DRIVE, SAVED_FOR_OFFLINE, CAMERA_UPLOADS, INBOX, SHARED_WITH_ME, CONTACTS, TRANSFERS, RUBBISH_BIN, SETTINGS, ACCOUNT, SEARCH;

		public String getTitle(Context context) {
			switch(this)
			{
				case CLOUD_DRIVE: return context.getString(R.string.section_cloud_drive);				
				case SAVED_FOR_OFFLINE: return context.getString(R.string.section_saved_for_offline);
				case CAMERA_UPLOADS: return context.getString(R.string.section_photo_sync);
				case INBOX: return context.getString(R.string.section_inbox);
				case SHARED_WITH_ME: return context.getString(R.string.section_shared_items);
				case CONTACTS: return context.getString(R.string.section_contacts);
				case TRANSFERS: return context.getString(R.string.section_transfers);
				case RUBBISH_BIN: return context.getString(R.string.section_rubbish_bin);
				case SETTINGS: return context.getString(R.string.action_settings);
				case ACCOUNT: return context.getString(R.string.section_account);				
				case SEARCH: return context.getString(R.string.action_search);
			}
			return null;			
		}
	}
	
	public static int POS_CAMERA_UPLOADS = 2;
	
	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 250; //in pixels
	
	final public static int MY_ACCOUNT_FRAGMENT = 5000;
	final public static int UPGRADE_ACCOUNT_FRAGMENT = 5001;
	final public static int PAYMENT_FRAGMENT = 5002;
	final public static int OVERQUOTA_ALERT = 5003;
	final public static int CC_FRAGMENT = 5004;
	final public static int FORTUMO_FRAGMENT = 5005;
	final public static int MONTHLY_YEARLY_FRAGMENT = 5006;
	final public static int CENTILI_FRAGMENT = 5007;	
	
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
	
	public static String ACTION_TAKE_SELFIE = "TAKE_SELFIE";
	public static String ACTION_CANCEL_DOWNLOAD = "CANCEL_DOWNLOAD";
	public static String ACTION_CANCEL_UPLOAD = "CANCEL_UPLOAD";
	public static String ACTION_CANCEL_CAM_SYNC = "CANCEL_CAM_SYNC";
	public static String ACTION_OPEN_MEGA_LINK = "OPEN_MEGA_LINK";
	public static String ACTION_OPEN_MEGA_FOLDER_LINK = "OPEN_MEGA_FOLDER_LINK";
	public static String ACTION_IMPORT_LINK_FETCH_NODES = "IMPORT_LINK_FETCH_NODES";
	public static String ACTION_FILE_EXPLORER_UPLOAD = "FILE_EXPLORER_UPLOAD";
	public static String ACTION_REFRESH_PARENTHANDLE_BROWSER = "REFRESH_PARENTHANDLE_BROWSER";
	public static String ACTION_OPEN_PDF = "OPEN_PDF";
	public static String EXTRA_PATH_PDF = "PATH_PDF";
	public static String EXTRA_OPEN_FOLDER = "EXTRA_OPEN_FOLER";
	public static String ACTION_EXPLORE_ZIP = "EXPLORE_ZIP";
	public static String EXTRA_PATH_ZIP = "PATH_ZIP";
	public static String EXTRA_HANDLE_ZIP = "HANDLE_ZIP";
	public static String ACTION_OVERQUOTA_ALERT = "OVERQUOTA_ALERT";
	public static String ACTION_FILE_PROVIDER = "ACTION_FILE_PROVIDER";
	
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
	
	public static int MODE_IN = 0;
	public static int MODE_OUT = 1;
	
	boolean megaContacts = true;
	ArrayList<String> contactsData;
	
	String accessToken;
	String feedback;
	
	private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    
   	private SearchView mSearchView;    
	private MenuItem searchMenuItem;
	
	private MenuItem createFolderMenuItem;
	private MenuItem importLinkMenuItem;
	private MenuItem addMenuItem;
	private MenuItem pauseRestartTransfersItem;
	private MenuItem refreshMenuItem;
	private MenuItem sortByMenuItem;
	private MenuItem helpMenuItem;
	private MenuItem upgradeAccountMenuItem;
	private MenuItem selectMenuItem;
	private MenuItem unSelectMenuItem;
	private MenuItem thumbViewMenuItem;
	private MenuItem addContactMenuItem;
	private MenuItem rubbishBinMenuItem;
	private MenuItem clearRubbishBinMenuitem;
	private MenuItem changePass;
	private MenuItem exportMK;
	private MenuItem removeMK;
	private MenuItem takePicture;
	private MenuItem cancelSubscription;
	private MenuItem cancelAllTransfers;
	private MenuItem killAllSessions;
	
	public int accountFragment;
	
	private static DrawerItem drawerItem;
	private static DrawerItem lastDrawerItem;
	
	private TableLayout topControlBar;
	private TableLayout bottomControlBar;
	private RoundedImageView imageProfile;
	private TextView textViewProfile;
	private TextView userName;
	private TextView userEmail;
	private TextView usedSpaceText;
	private TextView usedSpace;
	private ImageView usedSpaceWarning;
	private int usedPerc=0;
	
	ProgressBar usedSpaceBar;
	
	MegaUser contact = null;
	
	//ImageButton customListGrid;
	LinearLayout customSearch;
	AlertDialog permissionsDialog;
	private boolean firstTime = true;
	
	long handleToDownload=0;
	
	long parentHandleBrowser;
	long parentHandleRubbish;
	long parentHandleIncoming;
	long parentHandleOutgoing;
	long parentHandleSearch;
	long parentHandleInbox;
	private boolean isListCloudDrive = true;
	private boolean isListContacts = true;
	private boolean isListRubbishBin = true;
	private boolean isListSharedWithMe = true;
	private boolean isListOffline = true;
	private boolean isListCameraUpload = false;
	private boolean isListInbox = true;

    private SearchFragment sF;
//    private CameraUploadFragment psF;
    
    //Tabs in Contacts
    private TabHost mTabHostContacts;
    //private Fragment contactTabFragment;	
	TabsAdapter mTabsAdapterContacts;
    ViewPager viewPagerContacts;  
    //Tabs in Shares
    private TabHost mTabHostShares;
	TabsAdapter mTabsAdapterShares;
    ViewPager viewPagerShares;     
    
    static ManagerActivity managerActivity;
    private MegaApiAndroid megaApi;
    
    private static int EDIT_TEXT_ID = 1;  
    private AlertDialog renameDialog;
    private AlertDialog openLinkDialog;
    private AlertDialog newFolderDialog;
    private AlertDialog addContactDialog;
    private AlertDialog clearRubbishBinDialog;
    private AlertDialog alertNotPermissionsUpload;
    private AlertDialog alertPermissionWiFi;
    private Handler handler;    
    private boolean moveToRubbish = false;
    private boolean sendToInbox = false;
    private boolean isClearRubbishBin = false;

	String nameText;
	String firstNameText;

	boolean name = false;
	boolean firstName = false;
    
    ProgressDialog statusDialog;
    
	public UploadHereDialog uploadDialog;	
	private List<ShareInfo> filePreparedInfos;	
	private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	private int orderContacts = MegaApiJava.ORDER_DEFAULT_ASC;
	private int orderOffline = MegaApiJava.ORDER_DEFAULT_ASC;
	private int orderOutgoing = MegaApiJava.ORDER_DEFAULT_ASC;
	private int orderIncoming = MegaApiJava.ORDER_DEFAULT_ASC;
	
	ActionBar aB;	
	String urlLink = "";		
//	SparseArray<TransfersHolder> transfersListArray = null;	
	boolean downloadPlay = true;	
	boolean pauseIconVisible = false;	
	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;
	MegaAttributes attr = null;	
	ArrayList<MegaTransfer> tL;	
	String searchQuery = null;
	ArrayList<MegaNode> searchNodes;
	int levelsSearch = -1;
	private boolean openLink = false;	
	MegaApplication app;	
	NavigationDrawerAdapter nDA;

	String pathNavigation = "/";	
	long lastTimeOnTransferUpdate = -1;	
	boolean firstTimeCam = false;
	int accountType = -1;
	MegaAccountDetails accountInfo = null;
	long usedGbStorage = -1;
	AlertDialog overquotaDialog;
	
	String titleAB = "";
	
	private boolean isGetLink = false;
	
	
	//Billing
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
    
    boolean inventoryFinished = false;
    boolean accountDetailsFinished = false;
    
    int levelAccountDetails = -1;
    int levelInventory = -1;
    
    long numberOfSubscriptions = -1;
    
    BitSet paymentBitSet = null;
    
    /*
	 * Background task to emptying the Rubbish Bin
	 */
//	private class ClearRubbisBinTask extends AsyncTask<String, Void, Void> {
//		Context context;
//		
//		ClearRubbisBinTask(Context context){
//			this.context = context;
//		}
//		
//		@Override
//		protected Void doInBackground(String... params) {
//			log("doInBackground-Async Task ClearRubbisBinTask");			
//
//			if (rbF != null){
//				ArrayList<MegaNode> rubbishNodes = megaApi.getChildren(megaApi.getRubbishNode(), orderGetChildren);
//				
//				isClearRubbishBin = true;
//				for (int i=0; i<rubbishNodes.size(); i++){
//					megaApi.remove(rubbishNodes.get(i), managerActivity);
//				}
//			}
//			
//			return null;
//		}		
//	}	
	
    // (arbitrary) request code for the purchase flow
    public static final int RC_REQUEST = 10001;
    String orderId = "";
    
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
                alert("Thank you for subscribing to PRO I Monthly!");
            }
            else if (purchase.getSku().equals(SKU_PRO_I_YEAR)) {
                log("PRO I Yearly subscription purchased.");
                alert("Thank you for subscribing to PRO I Yearly!");
            }
            else if (purchase.getSku().equals(SKU_PRO_II_MONTH)) {
                log("PRO II Monthly subscription purchased.");
                alert("Thank you for subscribing to PRO II Monthly!");
            }
            else if (purchase.getSku().equals(SKU_PRO_II_YEAR)) {
                log("PRO II Yearly subscription purchased.");
                alert("Thank you for subscribing to PRO II Yearly!");
            }
            else if (purchase.getSku().equals(SKU_PRO_III_MONTH)) {
                log("PRO III Monthly subscription purchased.");
                alert("Thank you for subscribing to PRO III Monthly!");
            }
            else if (purchase.getSku().equals(SKU_PRO_III_YEAR)) {
                log("PRO III Yearly subscription purchased.");
                alert("Thank you for subscribing to PRO III Yearly!");
            }
            else if (purchase.getSku().equals(SKU_PRO_LITE_MONTH)) {
                log("PRO LITE Monthly subscription purchased.");
                alert("Thank you for subscribing to PRO LITE Monthly!");
            }
            else if (purchase.getSku().equals(SKU_PRO_LITE_YEAR)) {
                log("PRO LITE Yearly subscription purchased.");
                alert("Thank you for subscribing to PRO LITE Yearly!");
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
    
    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        log("Showing alert dialog: " + message);
        bld.create().show();
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
	        			levelInventory = 0;	
	        			maxP = proLiteMonthly;
//	        		}
//            	}
				log("PRO LITE MONTHLY (JSON): __*" + proLiteMonthly.getOriginalJson() + "*__");
        	}
            
            if (proLiteYearly != null){
//            	if (megaApi.getMyUser().getEmail() != null){
//	            	if (proLiteYearly.getDeveloperPayload().compareTo(megaApi.getMyUser().getEmail()) == 0){
	        			levelInventory = 0;
	        			maxP = proLiteYearly;
//	        		}
//            	}
				log("PRO LITE ANNUALY (JSON): __*" + proLiteYearly.getOriginalJson() + "*__");
        	}
            
            if (proIMonthly != null){
//            	if (megaApi.getMyUser().getEmail() != null){
//	            	if (proIMonthly.getDeveloperPayload().compareTo(megaApi.getMyUser().getEmail()) == 0){
	        			levelInventory = 1;	
	        			maxP = proIMonthly;
//	        		}
//            	}
				log("PRO I MONTHLY (JSON): __*" + proIMonthly.getOriginalJson() + "*__");
        	}
            
            if (proIYearly!= null){
//            	if (megaApi.getMyUser().getEmail() != null){
//	            	if (proIYearly.getDeveloperPayload().compareTo(megaApi.getMyUser().getEmail()) == 0){
	        			levelInventory = 1;
	        			maxP = proIYearly;
//	        		}
//            	}
				log("PRO I ANNUALY (JSON): __*" + proIYearly.getOriginalJson() + "*__");
        	}
            
            if (proIIMonthly != null){
//            	if (megaApi.getMyUser().getEmail() != null){
//	            	if (proIIMonthly.getDeveloperPayload().compareTo(megaApi.getMyUser().getEmail()) == 0){
	        			levelInventory = 2;
	        			maxP = proIIMonthly;
//	        		}
//            	}
				log("PRO II MONTHLY (JSON): __*" + proIIMonthly.getOriginalJson() + "*__");
            }
            
            if (proIIYearly != null){
//            	if (megaApi.getMyUser().getEmail() != null){
//	            	if (proIIYearly.getDeveloperPayload().compareTo(megaApi.getMyUser().getEmail()) == 0){
	        			levelInventory = 2;
	        			maxP = proIIYearly;
//	        		}
//            	}
				log("PRO II ANNUALY (JSON): __*" + proIIYearly.getOriginalJson() + "*__");
            }
            
            if (proIIIMonthly != null){
//            	if (megaApi.getMyUser().getEmail() != null){
//	            	if (proIIIMonthly.getDeveloperPayload().compareTo(megaApi.getMyUser().getEmail()) == 0){
	        			levelInventory = 3;	
	        			maxP = proIIIMonthly;
//	        		}
//            	}
				log("PRO III MONTHLY (JSON): __*" + proIIIMonthly.getOriginalJson() + "*__");
            }
            
            if (proIIIYearly != null){
//            	if (megaApi.getMyUser().getEmail() != null){
//	            	if (proIIIYearly.getDeveloperPayload().compareTo(megaApi.getMyUser().getEmail()) == 0){
	        			levelInventory = 3;
	        			maxP = proIIIYearly;
//	        		}
//            	}
				log("PRO III ANNUALY (JSON): __*" + proIIIYearly.getOriginalJson() + "*__");
            }
            
            inventoryFinished = true;

			log("LEVELACCOUNTDETAILS: " + levelAccountDetails + "; LEVELINVENTORY: " + levelInventory + "; ACCOUNTDETAILSFINISHED: " + accountDetailsFinished);
            
            if (accountDetailsFinished){
            	if (levelInventory > levelAccountDetails){
            		if (maxP != null){
						log("ORIGINAL JSON1:" + maxP.getOriginalJson() + ":::");
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
            	
//            	launchPayment();
            }            
            
            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the premium upgrade?
//            Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
//            mIsPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
//            log("User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));
//
//            // Do we have the infinite gas plan?
//            Purchase infiniteGasPurchase = inventory.getPurchase(SKU_INFINITE_GAS);
//            mSubscribedToInfiniteGas = (infiniteGasPurchase != null &&
//                    verifyDeveloperPayload(infiniteGasPurchase));
//            Log.d(TAG, "User " + (mSubscribedToInfiniteGas ? "HAS" : "DOES NOT HAVE")
//                        + " infinite gas subscription.");
//            if (mSubscribedToInfiniteGas) mTank = TANK_MAX;
//
//            // Check for gas delivery -- if we own gas, we should fill up the tank immediately
//            Purchase gasPurchase = inventory.getPurchase(SKU_GAS);
//            if (gasPurchase != null && verifyDeveloperPayload(gasPurchase)) {
//                log("We have gas. Consuming it.");
//                mHelper.consumeAsync(inventory.getPurchase(SKU_GAS), mConsumeFinishedListener);
//                return;
//            }
//
//            updateUi();
//            setWaitScreen(false);
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
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		
//		Toast.makeText(this, "(): " + GooglePlayServicesUtil.isGooglePlayServicesAvailable(this), Toast.LENGTH_LONG).show();
		
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
		
//	    dbH = new DatabaseHandler(getApplicationContext());
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		if (Util.isOnline(this)){
			dbH.setAttrOnline(true);
		}
		else{
			dbH.setAttrOnline(false);
		}		
    	
		super.onCreate(savedInstanceState);
		managerActivity = this;
		if (aB == null){
			aB = getSupportActionBar();
		}

		app = (MegaApplication)getApplication();

//		// Get tracker.
//		Tracker t = app.getTracker(TrackerName.APP_TRACKER);
//		// Enable Advertising Features.
//		t.enableAdvertisingIdCollection(true);
//		// Set screen name.
//		t.setScreenName("Prueba");
//		// Send a screen view.
//		t.send(new HitBuilders.AppViewBuilder().build());
		
		
		megaApi = app.getMegaApi();
		
		log("retryPendingConnections()");
		if (megaApi != null){
			megaApi.retryPendingConnections();
		}
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);	    
   
	    if (dbH.getCredentials() == null){
			
			if (OldPreferences.getOldCredentials(this) != null){
	    		Intent loginWithOldCredentials = new Intent(this, LoginActivity.class);
	    		startActivity(loginWithOldCredentials);
	    		finish();
	    		return;
		    }
			
			Intent newIntent = getIntent();
		    
		    if (newIntent != null){
		    	if (newIntent.getAction() != null){
		    		if (newIntent.getAction().equals(ManagerActivity.ACTION_OPEN_MEGA_LINK) || newIntent.getAction().equals(ManagerActivity.ACTION_OPEN_MEGA_FOLDER_LINK)){
		    			openLink = true;
		    		}
		    		else if (newIntent.getAction().equals(ACTION_CANCEL_UPLOAD) || newIntent.getAction().equals(ACTION_CANCEL_DOWNLOAD) || newIntent.getAction().equals(ACTION_CANCEL_CAM_SYNC)){
//		    			Intent cancelTourIntent = new Intent(this, TourActivity.class);
//		    			cancelTourIntent.setAction(newIntent.getAction());
//		    			startActivity(cancelTourIntent);
//		    			finish();
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

//		imageProfile = (RoundedImageView) findViewById(R.id.profile_photo);
//		textViewProfile = (TextView) findViewById(R.id.profile_textview);
//		userEmail = (TextView) findViewById(R.id.profile_user_email);
//		userEmail.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
//		userEmail.getLayoutParams().width = Util.px2dp((235*scaleW), outMetrics);
//		userEmail.setSingleLine();
//		userEmail.setEllipsize(TruncateAt.END);
//		userName = (TextView) findViewById(R.id.profile_user_name);
//		userName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
//		userName.getLayoutParams().width = Util.px2dp((235*scaleW), outMetrics);
//		userName.setSingleLine();
//		userName.setEllipsize(TruncateAt.END);
//		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
//        mDrawerList = (ListView) findViewById(R.id.left_drawer_list);
//        topControlBar = (TableLayout) findViewById(R.id.top_control_bar);
//        topControlBar.setOnClickListener(this);
//        bottomControlBar = (TableLayout) findViewById(R.id.bottom_control_bar);
//        bottomControlBar.setOnClickListener(this);
//        usedSpace = (TextView) findViewById(R.id.used_space);
//        usedSpaceText = (TextView) findViewById(R.id.used_space_text);
//        usedSpaceWarning = (ImageView) findViewById(R.id.used_space_warning);
        usedSpaceWarning.setVisibility(View.INVISIBLE);
        usedSpaceBar = (ProgressBar) findViewById(R.id.manager_used_space_bar);
        
        usedSpaceBar.setProgress(0);
                      
        mTabHostContacts = (TabHost)findViewById(R.id.tabhost_contacts);
        mTabHostContacts.setup();
        
        mTabHostShares = (TabHost)findViewById(R.id.tabhost_shares);
        mTabHostShares.setup();
        
        viewPagerContacts = (ViewPager) findViewById(R.id.contact_tabs_pager);  
        viewPagerShares = (ViewPager) findViewById(R.id.shares_tabs_pager);   
        
        if (!Util.isOnline(this)){
        	
        	Intent offlineIntent = new Intent(this, OfflineActivity.class);
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
					if (getIntent().getAction().equals(ManagerActivity.ACTION_IMPORT_LINK_FETCH_NODES)){
						Intent intent = new Intent(managerActivity, LoginActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ManagerActivity.ACTION_IMPORT_LINK_FETCH_NODES);
						intent.setData(Uri.parse(getIntent().getDataString()));
						startActivity(intent);
						finish();	
						return;
					}
					else if (getIntent().getAction().equals(ManagerActivity.ACTION_OPEN_MEGA_LINK)){
						Intent intent = new Intent(managerActivity, FileLinkActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ManagerActivity.ACTION_IMPORT_LINK_FETCH_NODES);
						intent.setData(Uri.parse(getIntent().getDataString()));
						startActivity(intent);
						finish();	
						return;
					}
					else if (getIntent().getAction().equals(ManagerActivity.ACTION_OPEN_MEGA_FOLDER_LINK)){
						Intent intent = new Intent(managerActivity, LoginActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(ManagerActivity.ACTION_OPEN_MEGA_FOLDER_LINK);
						intent.setData(Uri.parse(getIntent().getDataString()));
						startActivity(intent);
						finish();	
						return;
					}
					else if (getIntent().getAction().equals(ACTION_CANCEL_UPLOAD) || getIntent().getAction().equals(ACTION_CANCEL_DOWNLOAD) || getIntent().getAction().equals(ACTION_CANCEL_CAM_SYNC)){
						Intent intent = new Intent(managerActivity, LoginActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.setAction(getIntent().getAction());
						startActivity(intent);
						finish();
						return;
					}
				}
			}
			Intent intent = new Intent(managerActivity, LoginActivity.class);
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
			
			contact = megaApi.getMyUser();
			
			if (contact != null){
				userEmail.setVisibility(View.VISIBLE);
				userEmail.setText(contact.getEmail());
//				String userNameString = contact.getEmail();
//				String [] sp = userNameString.split("@");
//				if (sp.length != 0){
//					userNameString = sp[0];
//					userName.setVisibility(View.VISIBLE);
//					userName.setText(userNameString);
//				}
				megaApi.getUserAttribute(1, this);
				megaApi.getUserAttribute(2, this);
				
				Bitmap defaultAvatar = Bitmap.createBitmap(DEFAULT_AVATAR_WIDTH_HEIGHT,DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
				Canvas c = new Canvas(defaultAvatar);
				Paint p = new Paint();
				p.setAntiAlias(true);
				p.setColor(getResources().getColor(R.color.lollipop_primary_color));
				
				int radius; 
		        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
		        	radius = defaultAvatar.getWidth()/2;
		        else
		        	radius = defaultAvatar.getHeight()/2;
		        
				c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
				imageProfile.setImageBitmap(defaultAvatar);
				
			    int avatarTextSize = getAvatarTextSize(density);
			    log("DENSITY: " + density + ":::: " + avatarTextSize);
			    if (contact.getEmail() != null){
				    if (contact.getEmail().length() > 0){
				    	log("TEXT: " + contact.getEmail());
				    	log("TEXT AT 0: " + contact.getEmail().charAt(0));
				    	String firstLetter = contact.getEmail().charAt(0) + "";
				    	firstLetter = firstLetter.toUpperCase(Locale.getDefault());
				    	textViewProfile.setText(firstLetter);
				    	textViewProfile.setTextSize(32);
				    	textViewProfile.setTextColor(Color.WHITE);
				    	textViewProfile.setVisibility(View.VISIBLE);
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
					        imageProfile.setImageBitmap(circleBitmap);
					        textViewProfile.setVisibility(View.GONE);
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
			
			bottomControlBar.setVisibility(View.GONE);
	        
			megaApi.getPaymentMethods(this);
	        megaApi.getAccountDetails(this);
	        megaApi.creditCardQuerySubscriptions(this);
	        
	        List<String> items;	        

			items = new ArrayList<String>();
			for (DrawerItem item : DrawerItem.values()) {
				if (!(item.equals(DrawerItem.SEARCH)||(item.equals(DrawerItem.ACCOUNT)))){					
					items.add(item.getTitle(this));
				}
			}    

			nDA = new NavigationDrawerAdapter(getApplicationContext(), items);
			mDrawerList.setAdapter(nDA);			
       
	        mDrawerList.setOnItemClickListener(this);
	        
	        if (aB == null){
	        	aB = getSupportActionBar();
	        }
	        aB.setIcon(R.drawable.ic_launcher);
	        aB.setHomeButtonEnabled(true);
			aB.setDisplayHomeAsUpEnabled(true);
			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
			aB.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.lollipop_primary_color)));
	        
	        mDrawerToggle = new ActionBarDrawerToggle(
	                this,                  /* host Activity */
	                mDrawerLayout,         /* DrawerLayout object */
	                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
	                R.string.app_name,  /* "open drawer" description for accessibility */
	                R.string.app_name  /* "close drawer" description for accessibility */
	                ) {
	            public void onDrawerClosed(View view) {
	            	
	            	if (getSupportActionBar() != null){
	            		if (titleAB.compareTo("") != 0){
	            			getSupportActionBar().setTitle(titleAB);
	            		}
	            	}
	            	supportInvalidateOptionsMenu();	// creates call to onPrepareOptionsMenu()
	            }
	
	            public void onDrawerOpened(View drawerView) {
	            	
	            	if (getSupportActionBar() != null){
	            		titleAB = getSupportActionBar().getTitle().toString();
	            		getSupportActionBar().setTitle(getString(R.string.general_menu));
	            	}
	            	supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
	            }
	        };
	        mDrawerToggle.setDrawerIndicatorEnabled(true);
	        mDrawerLayout.setDrawerListener(mDrawerToggle);
	        if (savedInstanceState == null){
	        	mDrawerLayout.openDrawer(Gravity.LEFT);
	        }
	        else{
				mDrawerLayout.closeDrawer(Gravity.LEFT);
	        }
	        
	        mDrawerLayout.setVisibility(View.VISIBLE);
	        
	        //Create the actionBar Menu
	        getSupportActionBar().setDisplayShowCustomEnabled(true);
	        getSupportActionBar().setCustomView(R.layout.custom_action_bar_top);
	        
	        customSearch = (LinearLayout) getSupportActionBar().getCustomView().findViewById(R.id.custom_search);
	        customSearch.setOnClickListener(this);

//			customListGrid = (ImageButton) getSupportActionBar().getCustomView().findViewById(R.id.menu_action_bar_grid);
//			customListGrid.setOnClickListener(this);
			
			parentHandleBrowser = -1;
			parentHandleRubbish = -1;
			parentHandleIncoming = -1;
			parentHandleIncoming = -1;
			parentHandleSearch = -1;
			parentHandleInbox = -1;
			orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
			if (savedInstanceState != null){
				firstTime = false;
				int visibleFragment = savedInstanceState.getInt("visibleFragment");
				orderGetChildren = savedInstanceState.getInt("orderGetChildren");
				parentHandleBrowser = savedInstanceState.getLong("parentHandleBrowser");
				parentHandleRubbish = savedInstanceState.getLong("parentHandleRubbish");
				parentHandleIncoming = savedInstanceState.getLong("parentHandleIncoming");
				parentHandleOutgoing = savedInstanceState.getLong("parentHandleIncoming");
				parentHandleSearch = savedInstanceState.getLong("parentHandleSearch");
				parentHandleInbox = savedInstanceState.getLong("parentHandleInbox");
				switch (visibleFragment){
					case 1:{
						drawerItem = DrawerItem.CLOUD_DRIVE;
						isListCloudDrive = true;
						break;
					}
					case 2:{
						drawerItem = DrawerItem.INBOX;
						isListCloudDrive = false;
						break;
					}
					case 3:{
						drawerItem = DrawerItem.CONTACTS;
						isListContacts = true;
						break;
					}
					case 4:{
						drawerItem = DrawerItem.CONTACTS;
						isListContacts = false;
						break;
					}
					case 5:{
						drawerItem = DrawerItem.RUBBISH_BIN;
						isListRubbishBin = true;
						break;
					}
					case 6:{
						drawerItem = DrawerItem.RUBBISH_BIN;
						isListRubbishBin = false;
						break;
					}
					case 7:{
						drawerItem = DrawerItem.TRANSFERS;
						downloadPlay = savedInstanceState.getBoolean("downloadPlay", true);
						pauseIconVisible = savedInstanceState.getBoolean("pauseIconVisible", false);
						break;
					}
					case 8:{
						drawerItem = DrawerItem.SHARED_WITH_ME;
						isListSharedWithMe = true;
						break;
					}
					case 9:{
						drawerItem = DrawerItem.SHARED_WITH_ME;
						isListSharedWithMe = false;
						break;
					}
					case 10:{
						drawerItem = DrawerItem.ACCOUNT;
						break;
					}
					case 11:{
						drawerItem = DrawerItem.SEARCH;
						searchQuery = savedInstanceState.getString("searchQuery");
						levelsSearch = savedInstanceState.getInt("levels");
						break;
					}
					case 12:{
						drawerItem = DrawerItem.CAMERA_UPLOADS;
						isListCameraUpload = true;
						break;
					}
					case 13:{
						drawerItem = DrawerItem.CAMERA_UPLOADS;
						isListCameraUpload = false;
						break;
					}
					case 14:{
						drawerItem = DrawerItem.INBOX;
						isListInbox = true;
						break;
					}
					case 15:{
						drawerItem = DrawerItem.INBOX;
						isListInbox = false;
						break;
					}
					
				}
			}

			if (drawerItem == null) {
				drawerItem = DrawerItem.CLOUD_DRIVE;
				Intent intent = getIntent();
				if (intent != null){
					boolean upgradeAccount = getIntent().getBooleanExtra("upgradeAccount", false);
					if(upgradeAccount){
						log("upgradeAccount true");
						mDrawerLayout.closeDrawer(Gravity.LEFT);
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
								selectDrawerItem(drawerItem);
								showpF(1, null, paymentBitSet);
								return;
							}
							case 2:{
								drawerItem = DrawerItem.ACCOUNT;
								selectDrawerItem(drawerItem);
								showpF(2, null, paymentBitSet);
								return;
							}
							case 3:{
								drawerItem = DrawerItem.ACCOUNT;
								selectDrawerItem(drawerItem);
								showpF(3, null, paymentBitSet);
								return;
							}	
							case 4:{
								drawerItem = DrawerItem.ACCOUNT;
								selectDrawerItem(drawerItem);
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
				mDrawerLayout.closeDrawer(Gravity.LEFT);
			}
	
			//INITIAL FRAGMENT
			selectDrawerItem(drawerItem);
		}
	}	
	
	public static int calculateInSampleSize(
			BitmapFactory.Options options, int reqWidth, int reqHeight) {
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
  
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	log("onSaveInstaceState");
    	if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}
		
		log("retryPendingConnections()");
		if (megaApi != null){
			megaApi.retryPendingConnections();
		}
    	super.onSaveInstanceState(outState);
    	
    	long pHBrowser = -1;
    	long pHRubbish = -1;
    	long pHSharedWithMe = -1;
    	long pHSearch = -1;
    	long pHInbox = -1;
    	int visibleFragment = -1;
    	String pathOffline = this.pathNavigation;
    	
    	int order = this.orderGetChildren;

    	

    	if (drawerItem == DrawerItem.RUBBISH_BIN)
    	{

    	}    	
    	

    	
    	//
//    	if (maF != null){
//    		if (drawerItem == DrawerItem.ACCOUNT){
//    			visibleFragment = 10;
//    		}
//    	}
//
    	if (sF != null){
    		if (drawerItem == DrawerItem.SEARCH){
    			pHSearch = sF.getParentHandle();
    			visibleFragment = 11;
    			outState.putString("searchQuery", searchQuery);
    			outState.putInt("levels", sF.getLevels());
    		}
    	}
    	
//    	if (psF != null){
//    		if (drawerItem == DrawerItem.CAMERA_UPLOADS){
//    			if (isListCameraUpload){
//    				visibleFragment = 12;
//    			}
//    			else{
//    				visibleFragment = 13;
//    			}
//
//    		}
//    	}
    	

    	
    	outState.putInt("orderGetChildren", order);
    	outState.putInt("visibleFragment", visibleFragment);
    	outState.putLong("parentHandleBrowser", pHBrowser);
    	outState.putLong("parentHandleRubbish", pHRubbish);
    	outState.putLong("parentHandleSharedWithMe", pHSharedWithMe);
    	outState.putLong("parentHandleSearch", pHSearch);
    	outState.putLong("parentHandleInbox", pHInbox);
    }
    
    @Override
    protected void onDestroy(){
    	log("onDestroy()");

    	super.onDestroy();
    	    	
    	if (megaApi.getRootNode() != null){
    		megaApi.removeGlobalListener(this);
    	
//    		startService(new Intent(getApplicationContext(), CameraSyncService.class));
    		megaApi.removeTransferListener(this);
    		megaApi.removeRequestListener(this);
    	} 
    }
    
    boolean isSearching = false;
    
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
	protected void onPause() {
    	log("onPause");
    	managerActivity = null;
    	super.onPause();
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
    			
    			if(getIntent().getAction().equals(ManagerActivity.ACTION_EXPLORE_ZIP)){  

    				String pathZip=intent.getExtras().getString(EXTRA_PATH_ZIP);    				
    				
    				Intent intentZip = new Intent(managerActivity, ZipBrowserActivity.class);    				
    				intentZip.putExtra(ZipBrowserActivity.EXTRA_PATH_ZIP, pathZip);
    			    startActivity(intentZip);   				
    				
    			}
//    			else if(getIntent().getAction().equals(ManagerActivity.ACTION_OPEN_PDF)){    				
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
    			else if (getIntent().getAction().equals(ManagerActivity.ACTION_IMPORT_LINK_FETCH_NODES)){
					Intent loginIntent = new Intent(managerActivity, LoginActivity.class);
					loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					loginIntent.setAction(ManagerActivity.ACTION_IMPORT_LINK_FETCH_NODES);
					loginIntent.setData(Uri.parse(getIntent().getDataString()));
					startActivity(loginIntent);
					finish();	
					return;
				}
				else if (getIntent().getAction().equals(ManagerActivity.ACTION_OPEN_MEGA_LINK)){
					Intent fileLinkIntent = new Intent(managerActivity, FileLinkActivity.class);
					fileLinkIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					fileLinkIntent.setAction(ManagerActivity.ACTION_IMPORT_LINK_FETCH_NODES);
					fileLinkIntent.setData(Uri.parse(getIntent().getDataString()));
					startActivity(fileLinkIntent);
					finish();	
					return;
				}
    			else if (intent.getAction().equals(ACTION_OPEN_MEGA_FOLDER_LINK)){
    				Intent intentFolderLink = new Intent(managerActivity, FolderLinkActivity.class);
    				intentFolderLink.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    				intentFolderLink.setAction(ManagerActivity.ACTION_OPEN_MEGA_FOLDER_LINK);
    				intentFolderLink.setData(Uri.parse(getIntent().getDataString()));
					startActivity(intentFolderLink);
					finish();
    			}
    			else if (intent.getAction().equals(ACTION_REFRESH_PARENTHANDLE_BROWSER)){
    				
    				parentHandleBrowser = intent.getLongExtra("parentHandle", -1);    				
    				intent.removeExtra("parentHandle");
    				setParentHandleBrowser(parentHandleBrowser);
    				

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
					AlertDialog.Builder builder = Util.getCustomAlertBuilder(this,
							title, text, null);
					builder.setPositiveButton(getString(R.string.general_yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {

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
    			else if (intent.getAction().equals(ACTION_TAKE_SELFIE)){
    				log("Intent take selfie");
    				takePicture();
    			}
    			intent.setAction(null);
				setIntent(null);
    		}
    	}
    	
    }
    
	/*
	 * Show Import Dialog
	 */
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
			
//			Intent openIntent = new Intent(this, ManagerActivity.class);
			Intent openFileIntent = new Intent(this, FileLinkActivity.class);
			openFileIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openFileIntent.setAction(ManagerActivity.ACTION_OPEN_MEGA_LINK);
			openFileIntent.setData(Uri.parse(url));
			startActivity(openFileIntent);
//			finish();
			return;
		}
		
		// Folder Download link
		else if (url != null && (url.matches("^https://mega.co.nz/#F!.+$") || url.matches("^https://mega.nz/#F!.+$"))) {
			log("folder link url");
			Intent openFolderIntent = new Intent(this, FolderLinkActivity.class);
			openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			openFolderIntent.setAction(ManagerActivity.ACTION_OPEN_MEGA_FOLDER_LINK);
			openFolderIntent.setData(Uri.parse(url));
			startActivity(openFolderIntent);
//			finish();
			return;
		}
		else{
			log("wrong url");
			Intent errorIntent = new Intent(this, ManagerActivity.class);
			errorIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(errorIntent);
		}
	}
	
	
	/*
	 * Check MEGA url and parse if valid
	 */
	private String[] parseDownloadUrl(String url) {
		log("parseDownloadUrl");
		if (url == null) {
			return null;
		}
		if (!url.matches("^https://mega.co.nz/#!.*!.*$")) {
			return null;
		}
		String[] parts = url.split("!");
		if(parts.length != 3) return null;
		return new String[] { parts[1], parts[2] };
	}
	
	public void cameraUploadsClicked(){
		log("cameraUplaodsClicked");
		drawerItem = DrawerItem.CAMERA_UPLOADS;

		selectDrawerItem(drawerItem);		
	}
    
	private View getTabIndicator(Context context, String title) {
        View view = LayoutInflater.from(context).inflate(R.layout.tab_layout, null);

        TextView tv = (TextView) view.findViewById(R.id.textView);
        tv.setText(title);
        return view;
    }
	
	public void setInitialCloudDrive (){
		drawerItem = DrawerItem.CLOUD_DRIVE;		
		
		nDA.setPositionClicked(0);
		selectDrawerItem(drawerItem);

		mDrawerLayout.openDrawer(Gravity.LEFT);
		firstTime = true;

//		if (fbF == null){
//			fbF = new FileBrowserFragment();
//			if (parentHandleBrowser == -1){
//				fbF.setParentHandle(megaApi.getRootNode().getHandle());
//				parentHandleBrowser = megaApi.getRootNode().getHandle();
//			}
//			else{
//				fbF.setParentHandle(parentHandleBrowser);
//			}
//			fbF.setIsList(isListCloudDrive);
//			fbF.setOrder(orderGetChildren);
//			ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getRootNode(), orderGetChildren);
//			fbF.setNodes(nodes);
//		}
//		else{
//								
//			fbF.setIsList(isListCloudDrive);
//			fbF.setParentHandle(parentHandleBrowser);
//			fbF.setOrder(orderGetChildren);
//			ArrayList<MegaNode> nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandleBrowser), orderGetChildren);
//			fbF.setNodes(nodes);
//		}
//		
//		mTabHostContacts.setVisibility(View.GONE);    			
//		viewPagerContacts.setVisibility(View.GONE); 
//		mTabHostShares.setVisibility(View.GONE);    			
//		viewPagerShares.setVisibility(View.GONE);
//		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//		ft.replace(R.id.fragment_container, fbF, "fbF");
//		ft.commit();
//		
//		mDrawerLayout.openDrawer(Gravity.LEFT);
//		firstTime = false;
//		
//		customSearch.setVisibility(View.VISIBLE);
//		viewPagerShares.setVisibility(View.GONE);
//		viewPagerContacts.setVisibility(View.GONE);
//
//		if (createFolderMenuItem != null){
//			createFolderMenuItem.setVisible(true);
//			addContactMenuItem.setVisible(false);
//			addMenuItem.setVisible(true);
//			refreshMenuItem.setVisible(true);
//			sortByMenuItem.setVisible(true);
//			helpMenuItem.setVisible(true);
//			upgradeAccountMenuItem.setVisible(true);
//			settingsMenuItem.setVisible(true);
//			selectMenuItem.setVisible(true);
//			unSelectMenuItem.setVisible(false);
//			thumbViewMenuItem.setVisible(true);
//			addMenuItem.setEnabled(true);	  
// 			
//			if (isListCloudDrive){	
//				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
//			}
//			else{
//				thumbViewMenuItem.setTitle(getString(R.string.action_list));
//			}
//			rubbishBinMenuItem.setVisible(true);
//			rubbishBinMenuItem.setTitle(getString(R.string.section_rubbish_bin));
//			clearRubbishBinMenuitem.setVisible(false);
//		}
	}
	
	private Fragment recreateFragment(Fragment f)
    {
        try {
            Fragment.SavedState savedState = getSupportFragmentManager().saveFragmentInstanceState(f);

            Fragment newInstance = f.getClass().newInstance();
            newInstance.setInitialSavedState(savedState);

            return newInstance;
        }
        catch (Exception e) // InstantiationException, IllegalAccessException
        {
            throw new RuntimeException("Cannot reinstantiate fragment " + f.getClass().getName(), e);
        }
    }
	
	public void refreshCameraUpload(){
//		drawerItem = DrawerItem.CAMERA_UPLOADS;
//		nDA.setPositionClicked(POS_CAMERA_UPLOADS);
//
//		Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("psF");
//		FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
//		fragTransaction.detach(currentFragment);
//		fragTransaction.commit();
//
//		fragTransaction = getSupportFragmentManager().beginTransaction();
//		fragTransaction.attach(currentFragment);
//		fragTransaction.commit();
	}
	
    public void selectDrawerItem(DrawerItem item){
    	log("selectDrawerItem");
    	switch (item){
    		case CLOUD_DRIVE:{
    			
    			if (aB == null){
    				aB = getSupportActionBar();
    			}
//    			
//    			megaApi.getPricing(this);
    			topControlBar.setBackgroundColor(getResources().getColor(R.color.navigation_drawer_background));

				mTabHostContacts.setVisibility(View.GONE);    			
    			viewPagerContacts.setVisibility(View.GONE); 
    			mTabHostShares.setVisibility(View.GONE);    			
    			mTabHostShares.setVisibility(View.GONE);
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

    			
    			customSearch.setVisibility(View.VISIBLE);
    			viewPagerContacts.setVisibility(View.GONE);
    			
    			if (createFolderMenuItem != null){
    				changePass.setVisible(false); 
        			exportMK.setVisible(false); 
        			removeMK.setVisible(false); 
	    			createFolderMenuItem.setVisible(true);
	    			addContactMenuItem.setVisible(false);
	    			addMenuItem.setVisible(true);
	    			refreshMenuItem.setVisible(false);
	    			sortByMenuItem.setVisible(true);
	    			helpMenuItem.setVisible(false);
	    			upgradeAccountMenuItem.setVisible(true);
	    			selectMenuItem.setVisible(true);
	    			unSelectMenuItem.setVisible(false);
	    			thumbViewMenuItem.setVisible(true);
	    			addMenuItem.setEnabled(true);	  
 	    			
	    			if (isListCloudDrive){	
	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    			}
	    			rubbishBinMenuItem.setVisible(false);
	    			rubbishBinMenuItem.setTitle(getString(R.string.section_rubbish_bin));
	    			clearRubbishBinMenuitem.setVisible(false);
    			}
    			
    			break;
    		}
    		case INBOX:{
   			
    			if (aB == null){
    				aB = getSupportActionBar();
    			}
    			
    			topControlBar.setBackgroundColor(getResources().getColor(R.color.navigation_drawer_background));
    			

    			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
    			    			
    			mTabHostContacts.setVisibility(View.GONE);    			
    			viewPagerContacts.setVisibility(View.GONE); 
    			mTabHostShares.setVisibility(View.GONE);    			
    			mTabHostShares.setVisibility(View.GONE);

    			customSearch.setVisibility(View.VISIBLE);
    			viewPagerContacts.setVisibility(View.GONE);
    			mDrawerLayout.closeDrawer(Gravity.LEFT);
    			
    			if (createFolderMenuItem != null){
    				//Show				
        			sortByMenuItem.setVisible(true);
        			selectMenuItem.setVisible(true); 
        			upgradeAccountMenuItem.setVisible(true);
        			
    				//Hide
        			refreshMenuItem.setVisible(false);
        			thumbViewMenuItem.setVisible(false);
    				pauseRestartTransfersItem.setVisible(false);
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
        			thumbViewMenuItem.setVisible(false);
        			clearRubbishBinMenuitem.setVisible(false);
        			rubbishBinMenuItem.setVisible(false);
	    		}

    			break;
    		}
    		case CONTACTS:{
  			

    			break;
    		}
    		case RUBBISH_BIN:{
    			
    			if (aB == null){
    				aB = getSupportActionBar();
    			}
    			
    			topControlBar.setBackgroundColor(getResources().getColor(R.color.navigation_drawer_background));
    			

    			mTabHostContacts.setVisibility(View.GONE);    			
    			viewPagerContacts.setVisibility(View.GONE); 
    			mTabHostShares.setVisibility(View.GONE);    			
    			mTabHostShares.setVisibility(View.GONE);

    			
    			customSearch.setVisibility(View.VISIBLE);
    			viewPagerContacts.setVisibility(View.GONE);
    			mDrawerLayout.closeDrawer(Gravity.LEFT);
    			
    			if (createFolderMenuItem != null){
    				createFolderMenuItem.setVisible(false);
	    			addMenuItem.setVisible(false);
	    			refreshMenuItem.setVisible(false);
	    			sortByMenuItem.setVisible(true);
	    			helpMenuItem.setVisible(false);
	    			upgradeAccountMenuItem.setVisible(true);
	    			selectMenuItem.setVisible(true);
	    			unSelectMenuItem.setVisible(true);
	    			thumbViewMenuItem.setVisible(true);
	    			addMenuItem.setEnabled(false);
	    			changePass.setVisible(false);
	    			exportMK.setVisible(false); 
        			removeMK.setVisible(false); 
    			
        			if (isListRubbishBin){	
	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    			}

        			rubbishBinMenuItem.setVisible(false);
        			rubbishBinMenuItem.setTitle(getString(R.string.section_cloud_drive));
	    			clearRubbishBinMenuitem.setVisible(true);
	    		}

    			break;
    		}
    		case SHARED_WITH_ME:{    			
    			

    			
    			break;
    		}
    		case SETTINGS:{
    			
    			topControlBar.setBackgroundColor(getResources().getColor(R.color.navigation_drawer_background));
    			mDrawerLayout.closeDrawer(Gravity.LEFT);
    			

    			
    			drawerItem = lastDrawerItem;
    			selectDrawerItem(drawerItem);
    			
    			break;
    		}
    		case ACCOUNT:{
    			
//    			if (nDA != null){
//					nDA.setPositionClicked(-1);
//				}
//
//    			if (aB == null){
//    				aB = getSupportActionBar();
//    			}
//    			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
//
//    			accountFragment=MY_ACCOUNT_FRAGMENT;
//    			topControlBar.setBackgroundColor(getResources().getColor(R.color.color_navigation_drawer_selected));
//
////    			if (maF == null){
////    				maF = new MyAccountFragment();
////    			}
//
//    			mTabHostContacts.setVisibility(View.GONE);
//    			viewPagerContacts.setVisibility(View.GONE);
//    			mTabHostShares.setVisibility(View.GONE);
//    			mTabHostShares.setVisibility(View.GONE);
//				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//				ft.replace(R.id.fragment_container, maF, "maF");
//    			ft.commit();
//
//    			customSearch.setVisibility(View.GONE);
//    			mDrawerLayout.closeDrawer(Gravity.LEFT);
//
//    			if (createFolderMenuItem != null){
//    				createFolderMenuItem.setVisible(false);
////        				rubbishBinMenuItem.setVisible(false);
//	    			addMenuItem.setVisible(false);
//	    			refreshMenuItem.setVisible(true);
//	    			sortByMenuItem.setVisible(false);
//	    			helpMenuItem.setVisible(true);
//	    			upgradeAccountMenuItem.setVisible(true);
//	    			selectMenuItem.setVisible(false);
//	    			unSelectMenuItem.setVisible(false);
//	    			thumbViewMenuItem.setVisible(false);
//	    			changePass.setVisible(true);
//	    			if (numberOfSubscriptions > 0){
//	    				cancelSubscription.setVisible(true);
//	    			}
//	    			killAllSessions.setVisible(true);
//
//	    			String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
//	    			log("Export in: "+path);
//	    			File file= new File(path);
//	    			if(file.exists()){
//	    				exportMK.setVisible(false);
//		    			removeMK.setVisible(true);
//	    			}
//	    			else{
//	    				exportMK.setVisible(true);
//		    			removeMK.setVisible(false);
//	    			}
//
////    	    			logoutMenuItem.setVisible(true);
////    	    			rubbishBinMenuItem.setIcon(R.drawable.ic_action_bar_null);
////    	    			rubbishBinMenuItem.setEnabled(false);
////    	    			addMenuItem.setIcon(R.drawable.ic_action_bar_null);
//	    			addMenuItem.setEnabled(false);
////    	    			createFolderMenuItem.setIcon(R.drawable.ic_action_bar_null);
//	    			createFolderMenuItem.setEnabled(false);
//	    			rubbishBinMenuItem.setVisible(false);
//	    			clearRubbishBinMenuitem.setVisible(false);
//        			settingsMenuItem.setVisible(false);
//	    		}
    			
    			
    			break;
    		}
    		case TRANSFERS:{
    			
    			if (aB == null){
    				aB = getSupportActionBar();
    			}
    			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
    			
    			topControlBar.setBackgroundColor(getResources().getColor(R.color.navigation_drawer_background));
    			

    			break;
    		}
    		case SAVED_FOR_OFFLINE:{
    			
    			if (aB == null){
    				aB = getSupportActionBar();
    			}
    			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
    			
    			topControlBar.setBackgroundColor(getResources().getColor(R.color.navigation_drawer_background));
    			

    			
    			mTabHostContacts.setVisibility(View.GONE);    			
    			viewPagerContacts.setVisibility(View.GONE); 
    			mTabHostShares.setVisibility(View.GONE);    			
    			mTabHostShares.setVisibility(View.GONE);

    			
    			mDrawerLayout.closeDrawer(Gravity.LEFT);
    			customSearch.setVisibility(View.VISIBLE);
    			

    			if (createFolderMenuItem != null){
	    			createFolderMenuItem.setVisible(false);
	    			addMenuItem.setVisible(false);
	    			sortByMenuItem.setVisible(false);
	    			upgradeAccountMenuItem.setVisible(true);
	    			selectMenuItem.setVisible(true);
	    			unSelectMenuItem.setVisible(false);
	    			thumbViewMenuItem.setVisible(true);
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
    				refreshMenuItem.setVisible(false);
    				helpMenuItem.setVisible(false);
    			}
    			
    			break;
    		}
    		case SEARCH:{
    			
    			if (aB == null){
    				aB = getSupportActionBar();
    			}
    			
    			topControlBar.setBackgroundColor(getResources().getColor(R.color.navigation_drawer_background));
    			
    			if (sF == null){
        			sF = new SearchFragment();
        		}
    			
    			searchNodes = megaApi.search(megaApi.getRootNode(), searchQuery, true);
    			
    			drawerItem = DrawerItem.SEARCH;
    			
    			sF.setSearchNodes(searchNodes);
    			sF.setNodes(searchNodes);
    			sF.setSearchQuery(searchQuery);
    			sF.setParentHandle(parentHandleSearch);
    			sF.setLevels(levelsSearch);
    			
    			mTabHostContacts.setVisibility(View.GONE);    			
    			viewPagerContacts.setVisibility(View.GONE); 
    			mTabHostShares.setVisibility(View.GONE);    			
    			mTabHostShares.setVisibility(View.GONE);
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, sF, "sF");
    			ft.commit();
    			
    			customSearch.setVisibility(View.VISIBLE);    			

    			if (createFolderMenuItem != null){
        			createFolderMenuItem.setVisible(false);
        			addMenuItem.setVisible(false);
        			sortByMenuItem.setVisible(false);
        			upgradeAccountMenuItem.setVisible(true);
	    			selectMenuItem.setVisible(true);
	    			unSelectMenuItem.setVisible(false);
	    			thumbViewMenuItem.setVisible(true);
        			addMenuItem.setEnabled(true);
        			rubbishBinMenuItem.setVisible(false); 
        			clearRubbishBinMenuitem.setVisible(false);
        			changePass.setVisible(false); 
        			exportMK.setVisible(false); 
        			removeMK.setVisible(false);
    				refreshMenuItem.setVisible(false);
    				helpMenuItem.setVisible(false);
    			}
    			break;
    		}
    		case CAMERA_UPLOADS:{
    			
    			if (aB == null){
    				aB = getSupportActionBar();
    			}
    			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
    			
    			topControlBar.setBackgroundColor(getResources().getColor(R.color.navigation_drawer_background));
    			
    			if (nDA != null){
    				nDA.setPositionClicked(POS_CAMERA_UPLOADS);
    			}
    			
//    			if (psF == null){
//    				psF = new CameraUploadFragment();
//    				psF.setIsList(isListCameraUpload);
//   					psF.setFirstTimeCam(firstTimeCam);
//				}
//				else{
//					psF.setIsList(isListCameraUpload);
//					psF.setFirstTimeCam(firstTimeCam);
//				}
//
				
    			mTabHostContacts.setVisibility(View.GONE);    			
    			viewPagerContacts.setVisibility(View.GONE); 
    			mTabHostShares.setVisibility(View.GONE);    			
    			mTabHostShares.setVisibility(View.GONE);
//				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//				ft.replace(R.id.fragment_container, psF, "psF");
//    			ft.commit();
    			
    			
    			firstTimeCam = false;
    			
    			
				mDrawerLayout.closeDrawer(Gravity.LEFT);
    			
    			customSearch.setVisibility(View.VISIBLE);
    			
    			if (createFolderMenuItem != null){
	    			createFolderMenuItem.setVisible(false);
	    			addMenuItem.setVisible(false);
	    			sortByMenuItem.setVisible(false);
	    			upgradeAccountMenuItem.setVisible(true);
	    			selectMenuItem.setVisible(false);
	    			unSelectMenuItem.setVisible(false);
	    			thumbViewMenuItem.setVisible(true);
	    			addMenuItem.setEnabled(false);
	    			createFolderMenuItem.setEnabled(false);
	    			changePass.setVisible(false); 
	    			exportMK.setVisible(false); 
	    			removeMK.setVisible(false);
    				refreshMenuItem.setVisible(false);
    				helpMenuItem.setVisible(false);
	    			if (isListCameraUpload){	
	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
					}
					else{
						thumbViewMenuItem.setTitle(getString(R.string.action_list));
	    			}
	    			rubbishBinMenuItem.setVisible(false);
	    			clearRubbishBinMenuitem.setVisible(false);
    			}
      			break;
    		}
			default:{
				break;
			}
    	}
    }

    @Override
    public void onBackPressed() {
    	log("onBackPressed");
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

    	}				
    	if (drawerItem == DrawerItem.SEARCH){
    		if (sF != null){
    			if (sF.onBackPressed() == 0){
    				drawerItem = DrawerItem.CLOUD_DRIVE;
    				selectDrawerItem(drawerItem);
    				if(nDA!=null){
    					nDA.setPositionClicked(0);
    				}
    				return;
    			}
    		}
    	}





    	if (drawerItem == DrawerItem.SHARED_WITH_ME){

    	}			
    	if (drawerItem == DrawerItem.RUBBISH_BIN){

    	}

    	if (drawerItem == DrawerItem.ACCOUNT){


    	}
    	if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE){

    	}

    	//		if (sF != null){
    	//			if (drawerItem == DrawerItem.SEARCH){
    	//				if (sF.onBackPressed() == 0){
    	//					drawerItem = DrawerItem.CLOUD_DRIVE;
    	//					selectDrawerItem(drawerItem);
    	//					if(nDA!=null){
    	//						nDA.setPositionClicked(0);
    	//					}
    	//					return;
    	//				}
    	//			}
    	//		}

//    	if (psF != null){
//    		if (drawerItem == DrawerItem.CAMERA_UPLOADS){
//    			if (psF.onBackPressed() == 0){
//    				drawerItem = DrawerItem.CLOUD_DRIVE;
//    				selectDrawerItem(drawerItem);
//    				if(nDA!=null){
//    					nDA.setPositionClicked(0);
//    				}
//    				return;
//    			}
//    		}
//    	}

    }

	@Override
	public void onPostCreate(Bundle savedInstanceState){
		log("onPostCreate");
		super.onPostCreate(savedInstanceState);
		if (!openLink){
			mDrawerToggle.syncState();
		}
	}
	
	@Override
	protected void onPostResume() {
	    super.onPostResume();
	    if (isSearching){
			selectDrawerItem(DrawerItem.SEARCH);        		
    		isSearching = false;
	    } 
	}
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		log("onCreateOptionsMenu");
	
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_manager, menu);
		getSupportActionBar().setDisplayShowCustomEnabled(true);
	    
	    final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchMenuItem = menu.findItem(R.id.action_search);
		searchMenuItem.setVisible(false);
		final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
		
		if (searchView != null){
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			searchView.setIconifiedByDefault(true);
		}
		
		addContactMenuItem =menu.findItem(R.id.action_add_contact);
		addMenuItem = menu.findItem(R.id.action_add);
		pauseRestartTransfersItem = menu.findItem(R.id.action_pause_restart_transfers);
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
		
		changePass = menu.findItem(R.id.action_menu_change_pass);
		exportMK = menu.findItem(R.id.action_menu_export_MK);
		removeMK = menu.findItem(R.id.action_menu_remove_MK);
		
		takePicture = menu.findItem(R.id.action_take_picture);
		
		cancelSubscription = menu.findItem(R.id.action_menu_cancel_subscriptions);
		cancelSubscription.setVisible(false);
		
		cancelAllTransfers = menu.findItem(R.id.action_menu_cancel_all_transfers);
		cancelAllTransfers.setVisible(false);
		
		killAllSessions = menu.findItem(R.id.action_menu_kill_all_sessions);
		killAllSessions.setVisible(false);
		
//		if (drawerItem == DrawerItem.CLOUD_DRIVE){

		if (drawerItem == DrawerItem.CLOUD_DRIVE){
			

		}
		

		
		if (drawerItem == DrawerItem.RUBBISH_BIN){

		}	
		

		if (drawerItem == DrawerItem.SHARED_WITH_ME){
			int index = viewPagerShares.getCurrentItem();
			if(index==0){	

			}
			else if(index==1) {
			}
		}
		
		if (drawerItem == DrawerItem.ACCOUNT){
//			if (maF != null){
//
//				//Show
//				refreshMenuItem.setVisible(true);
//				helpMenuItem.setVisible(true);
//				upgradeAccountMenuItem.setVisible(true);
//				changePass.setVisible(true);
//
//				//Hide
//				pauseRestartTransfersItem.setVisible(false);
//				createFolderMenuItem.setVisible(false);
//				addContactMenuItem.setVisible(false);
//    			addMenuItem.setVisible(false);
//    			sortByMenuItem.setVisible(false);
//    			selectMenuItem.setVisible(false);
//    			unSelectMenuItem.setVisible(false);
//    			thumbViewMenuItem.setVisible(false);
//    			addMenuItem.setEnabled(false);
//    			createFolderMenuItem.setEnabled(false);
//    			rubbishBinMenuItem.setVisible(false);
//    			clearRubbishBinMenuitem.setVisible(false);
//    			importLinkMenuItem.setVisible(false);
//    			takePicture.setVisible(false);
//				settingsMenuItem.setVisible(false);
//
//				if (numberOfSubscriptions > 0){
//					cancelSubscription.setVisible(true);
//				}
//
//				killAllSessions.setVisible(true);
//
//    			String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
//    			log("Export in: "+path);
//    			File file= new File(path);
//    			if(file.exists()){
//    				exportMK.setVisible(false);
//	    			removeMK.setVisible(true);
//    			}
//    			else{
//    				exportMK.setVisible(true);
//	    			removeMK.setVisible(false);
//    			}
//
//			}
		}
		

		if (sF != null){
			if (drawerItem == DrawerItem.SEARCH){
				if (createFolderMenuItem != null){
					
					//Show
	    			upgradeAccountMenuItem.setVisible(true);	    			

					//Hide
	    			thumbViewMenuItem.setVisible(false);
					pauseRestartTransfersItem.setVisible(false);
	    			createFolderMenuItem.setVisible(false);
	    			addContactMenuItem.setVisible(false);
	    			addMenuItem.setVisible(false);
	    			refreshMenuItem.setVisible(false);
	    			sortByMenuItem.setVisible(false);
	    			selectMenuItem.setVisible(false);
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
				}
			}
		}
		
//		if (psF != null){
//			if (drawerItem == DrawerItem.CAMERA_UPLOADS){
//
//				//Show
//    			upgradeAccountMenuItem.setVisible(true);
//    			selectMenuItem.setVisible(true);
//    			takePicture.setVisible(true);
//
//				//Hide
//				pauseRestartTransfersItem.setVisible(false);
//				createFolderMenuItem.setVisible(false);
//				addContactMenuItem.setVisible(false);
//    			addMenuItem.setVisible(false);
//    			refreshMenuItem.setVisible(false);
//    			sortByMenuItem.setVisible(false);
//    			unSelectMenuItem.setVisible(false);
//    			thumbViewMenuItem.setVisible(true);
//    			addMenuItem.setEnabled(false);
//    			createFolderMenuItem.setEnabled(false);
//    			changePass.setVisible(false);
//    			exportMK.setVisible(false);
//    			removeMK.setVisible(false);
//    			rubbishBinMenuItem.setVisible(false);
//    			clearRubbishBinMenuitem.setVisible(false);
//    			importLinkMenuItem.setVisible(false);
//    			refreshMenuItem.setVisible(false);
//				helpMenuItem.setVisible(false);
//				settingsMenuItem.setVisible(false);
//
//    			if (isListCameraUpload){
//    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
//				}
//				else{
//					thumbViewMenuItem.setTitle(getString(R.string.action_list));
//    			}
//			}
//		}
	    	    
	    return super.onCreateOptionsMenu(menu);
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
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		log("onOptionsItemSelectedKitkat");
		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}
		
		log("retryPendingConnections()");
		if (megaApi != null){
			megaApi.retryPendingConnections();
		}
		// Handle presses on the action bar items
	    switch (item.getItemId()) {
		    case android.R.id.home:{
//		    case R.id.home:
//		    case R.id.homeAsUp:
	    	//case 16908332: //Algo pasa con la CyanogenMod
		    	if (mDrawerToggle.isDrawerIndicatorEnabled()) {
					mDrawerToggle.onOptionsItemSelected(item);
				}
		    	else {

		    		if (drawerItem == DrawerItem.SHARED_WITH_ME){

		    		}
		    		if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE){

		    		}
		    		if (sF != null){
		    			if (drawerItem == DrawerItem.SEARCH){
		    				sF.onBackPressed();
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
	        case R.id.action_search:{
	        	mSearchView.setIconified(false);
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
	        	
	        	else if (drawerItem == DrawerItem.RUBBISH_BIN){
	        		showClearRubbishBinDialog(null);
	        	}
	        	return true;
	        }
	        case R.id.action_add:{
	        	
	        	if (drawerItem == DrawerItem.SHARED_WITH_ME){

	        	}	
	        	else {
	        		this.uploadFile();
	        	}
	        	
	        	return true;     	
	        }
	        case R.id.action_pause_restart_transfers:{
	        	if (drawerItem == DrawerItem.TRANSFERS){	    			
	    			if (downloadPlay){
	    				downloadPlay = false;
	    				pauseRestartTransfersItem.setTitle(getResources().getString(R.string.menu_restart_transfers));
	    			}
	    			else{
	    				downloadPlay = true;
	    				pauseRestartTransfersItem.setTitle(getResources().getString(R.string.menu_pause_transfers));
	    			}
	    			megaApi.pauseTransfers(!downloadPlay, this);
	        	}
	        	
	        	return true;
	        }
	        case R.id.action_select:{
	        	//TODO: multiselect
	        	
        		if (drawerItem == DrawerItem.CLOUD_DRIVE){	


        			return true;        		
	        	}


	        	if (drawerItem == DrawerItem.SHARED_WITH_ME){

	        		return true;
	        	}
	        	if (drawerItem == DrawerItem.SAVED_FOR_OFFLINE){

	        		return true;
    			}

//	        	if (psF != null){
//	        		if (drawerItem == DrawerItem.CAMERA_UPLOADS){
//	        			psF.selectAll();
//	        			if (psF.showSelectMenuItem()){
//	        				selectMenuItem.setVisible(true);
//	        				unSelectMenuItem.setVisible(false);
//	        			}
//	        			else{
//	        				selectMenuItem.setVisible(false);
//	        				unSelectMenuItem.setVisible(true);
//	        			}
//	        			return true;
//	        		}
//	        	}
	        	if (drawerItem == DrawerItem.RUBBISH_BIN){


	        		return true;
	        	}

	        }
	        case R.id.action_grid:{	    			
	        	//TODO: gridView




	        	if (drawerItem == DrawerItem.SHARED_WITH_ME){
	        		
	    			Toast toast = Toast.makeText(this, getString(R.string.general_not_yet_implemented), Toast.LENGTH_LONG);
	    			toast.show();
//	        			Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("inSF");
//	        			FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
//	        			fragTransaction.detach(currentFragment);
//	        			fragTransaction.commit();
//
//	        			isListSharedWithMe = !isListSharedWithMe;
//	        			inSF.setIsList(isListSharedWithMe);
//	        			inSF.setParentHandle(parentHandleSharedWithMe);
//
//	        			fragTransaction = getSupportFragmentManager().beginTransaction();
//	        			fragTransaction.attach(currentFragment);
//	        			fragTransaction.commit();

	        		
	        	}

//        		if (psF != null){
//        			if (drawerItem == DrawerItem.CAMERA_UPLOADS){
//        				Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("psF");
//        				FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
//        				fragTransaction.detach(currentFragment);
//        				fragTransaction.commit();
//
//        				isListCameraUpload = !isListCameraUpload;
//        				if (isListCameraUpload){
//    	    				thumbViewMenuItem.setTitle(getString(R.string.action_grid));
//    					}
//    					else{
//    						thumbViewMenuItem.setTitle(getString(R.string.action_list));
//    	    			}
//        				psF.setIsList(isListCameraUpload);
//
//        				fragTransaction = getSupportFragmentManager().beginTransaction();
//        				fragTransaction.attach(currentFragment);
//        				fragTransaction.commit();
//
//        			}
//        		}
           	
	        	return true;
	        }	        
	        case R.id.action_rubbish_bin:{
	        	if (drawerItem == DrawerItem.RUBBISH_BIN){
	        		drawerItem = DrawerItem.CLOUD_DRIVE;
	        		selectDrawerItem(drawerItem);
	        	}
	        	else if (drawerItem == DrawerItem.CLOUD_DRIVE){
	        		drawerItem = DrawerItem.RUBBISH_BIN;
	        		selectDrawerItem(drawerItem);
	        	}
	        	return true;
	        }
	        case R.id.action_menu_clear_rubbish_bin:{
	        	if (drawerItem == DrawerItem.RUBBISH_BIN){
	        		showClearRubbishBinDialog(null);
	        	}
	        	return true;
	        }
	        case R.id.action_menu_refresh:{
	        	switch(drawerItem){
		        	case CLOUD_DRIVE:{
		        		Intent intent = new Intent(managerActivity, LoginActivity.class);
			    		intent.setAction(LoginActivity.ACTION_REFRESH);
			    		intent.putExtra("PARENT_HANDLE", parentHandleBrowser);
			    		startActivityForResult(intent, REQUEST_CODE_REFRESH);
		        		break;
		        	}
		        	case CONTACTS:{
		        		Intent intent = new Intent(managerActivity, LoginActivity.class);
			    		intent.setAction(LoginActivity.ACTION_REFRESH);
			    		intent.putExtra("PARENT_HANDLE", parentHandleBrowser);
			    		startActivityForResult(intent, REQUEST_CODE_REFRESH);
			    		break;
		        	}
		        	case RUBBISH_BIN:{
		        		Intent intent = new Intent(managerActivity, LoginActivity.class);
			    		intent.setAction(LoginActivity.ACTION_REFRESH);
			    		intent.putExtra("PARENT_HANDLE", parentHandleRubbish);
			    		startActivityForResult(intent, REQUEST_CODE_REFRESH);
			    		break;
		        	}
		        	case SHARED_WITH_ME:{
		        		

		        	}
		        	case ACCOUNT:{
		        		Intent intent = new Intent(managerActivity, LoginActivity.class);
			    		intent.setAction(LoginActivity.ACTION_REFRESH);
			    		intent.putExtra("PARENT_HANDLE", parentHandleBrowser);
			    		startActivityForResult(intent, REQUEST_CODE_REFRESH);
			    		break;
		        	}
	        	}
	        	return true;
	        }
	        case R.id.action_menu_sort_by:{
	        	switch(drawerItem){
		        	case CONTACTS:{
		        		AlertDialog sortByDialog;		        		
		        		LayoutInflater inflater = getLayoutInflater();
		        		View dialoglayout = inflater.inflate(R.layout.sortby_dialog, null);
		        		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		        		builder.setView(dialoglayout);
		        		builder.setTitle(getString(R.string.action_sort_by));
		        		builder.setPositiveButton(getString(R.string.general_cancel), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
		        		
		        		sortByDialog = builder.create();
		        		sortByDialog.show();
		        		Util.brandAlertDialog(sortByDialog);
		        		
		        		TextView byNameTextView = (TextView) sortByDialog.findViewById(R.id.sortby_dialog_name_text);
		        		byNameTextView.setText(getString(R.string.sortby_name));
		        		final CheckedTextView ascendingCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_ascending_check);
		        		ascendingCheck.setText(getString(R.string.sortby_name_ascending));
		        		final CheckedTextView descendingCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_descending_check);
		        		descendingCheck.setText(getString(R.string.sortby_name_descending));
		        		
		        		TextView byDateTextView = (TextView) sortByDialog.findViewById(R.id.sortby_dialog_date_text);
		        		byDateTextView.setText(getString(R.string.sortby_date));
		        		final CheckedTextView newestCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_newest_check);
		        		newestCheck.setText(getString(R.string.sortby_date_newest));
		        		final CheckedTextView oldestCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_oldest_check);
		        		oldestCheck.setText(getString(R.string.sortby_date_oldest));
		        		
		        		TextView bySizeTextView = (TextView) sortByDialog.findViewById(R.id.sortby_dialog_size_text);
		        		bySizeTextView.setText(getString(R.string.sortby_size));
		        		final CheckedTextView largestCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_largest_first_check);
		        		largestCheck.setText(getString(R.string.sortby_size_largest_first));
		        		final CheckedTextView smallestCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_smallest_first_check);
		        		smallestCheck.setText(getString(R.string.sortby_size_smallest_first));

		        		byDateTextView.setVisibility(View.GONE);
		        		newestCheck.setVisibility(View.GONE);
		        		oldestCheck.setVisibility(View.GONE);
		        		bySizeTextView.setVisibility(View.GONE);
		        		largestCheck.setVisibility(View.GONE);
		        		smallestCheck.setVisibility(View.GONE);
		        		
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
		        		
		        		final AlertDialog dialog = sortByDialog;
		        		
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
		        		AlertDialog sortByDialog;		        		
		        		LayoutInflater inflater = getLayoutInflater();
		        		View dialoglayout = inflater.inflate(R.layout.sortby_dialog, null);
		        		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		        		builder.setView(dialoglayout);
		        		builder.setTitle(getString(R.string.action_sort_by));
		        		builder.setPositiveButton(getString(R.string.general_cancel), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
		        		
		        		sortByDialog = builder.create();
		        		sortByDialog.show();
		        		Util.brandAlertDialog(sortByDialog);
		        		
		        		TextView byNameTextView = (TextView) sortByDialog.findViewById(R.id.sortby_dialog_name_text);
		        		byNameTextView.setText(getString(R.string.sortby_name));
		        		final CheckedTextView ascendingCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_ascending_check);
		        		ascendingCheck.setText(getString(R.string.sortby_name_ascending));
		        		final CheckedTextView descendingCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_descending_check);
		        		descendingCheck.setText(getString(R.string.sortby_name_descending));
		        		
		        		TextView byDateTextView = (TextView) sortByDialog.findViewById(R.id.sortby_dialog_date_text);
		        		byDateTextView.setText(getString(R.string.sortby_date));
		        		final CheckedTextView newestCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_newest_check);
		        		newestCheck.setText(getString(R.string.sortby_date_newest));
		        		final CheckedTextView oldestCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_oldest_check);
		        		oldestCheck.setText(getString(R.string.sortby_date_oldest));
		        		
		        		TextView bySizeTextView = (TextView) sortByDialog.findViewById(R.id.sortby_dialog_size_text);
		        		bySizeTextView.setText(getString(R.string.sortby_size));
		        		final CheckedTextView largestCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_largest_first_check);
		        		largestCheck.setText(getString(R.string.sortby_size_largest_first));
		        		final CheckedTextView smallestCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_smallest_first_check);
		        		smallestCheck.setText(getString(R.string.sortby_size_smallest_first));
		        		

		        		
		        		byDateTextView.setVisibility(View.GONE);
		        		newestCheck.setVisibility(View.GONE);
		        		oldestCheck.setVisibility(View.GONE);
		        		bySizeTextView.setVisibility(View.GONE);
		        		largestCheck.setVisibility(View.GONE);
		        		smallestCheck.setVisibility(View.GONE);
		        		
		        		switch(orderOffline){
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
		        		
		        		final AlertDialog dialog = sortByDialog;
		        		
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
		        	case SHARED_WITH_ME: {		        		
 		
		         		AlertDialog sortByDialog;		        		
		        		LayoutInflater inflater = getLayoutInflater();
		        		View dialoglayout = inflater.inflate(R.layout.sortby_dialog, null);
		        		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		        		builder.setView(dialoglayout);
		        		builder.setTitle(getString(R.string.action_sort_by));
		        		builder.setPositiveButton(getString(R.string.general_cancel), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
		        		
		        		sortByDialog = builder.create();
		        		sortByDialog.show();
		        		Util.brandAlertDialog(sortByDialog);
		        		
		        		TextView byNameTextView = (TextView) sortByDialog.findViewById(R.id.sortby_dialog_name_text);
		        		byNameTextView.setText(getString(R.string.sortby_name));
		        		final CheckedTextView ascendingCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_ascending_check);
		        		ascendingCheck.setText(getString(R.string.sortby_name_ascending));
		        		final CheckedTextView descendingCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_descending_check);
		        		descendingCheck.setText(getString(R.string.sortby_name_descending));
		        		
		        		TextView byDateTextView = (TextView) sortByDialog.findViewById(R.id.sortby_dialog_date_text);
		        		byDateTextView.setText(getString(R.string.sortby_date));
		        		final CheckedTextView newestCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_newest_check);
		        		newestCheck.setText(getString(R.string.sortby_date_newest));
		        		final CheckedTextView oldestCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_oldest_check);
		        		oldestCheck.setText(getString(R.string.sortby_date_oldest));
		        		
		        		TextView bySizeTextView = (TextView) sortByDialog.findViewById(R.id.sortby_dialog_size_text);
		        		bySizeTextView.setText(getString(R.string.sortby_size));
		        		final CheckedTextView largestCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_largest_first_check);
		        		largestCheck.setText(getString(R.string.sortby_size_largest_first));
		        		final CheckedTextView smallestCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_smallest_first_check);
		        		smallestCheck.setText(getString(R.string.sortby_size_smallest_first));

		        		
		        		byDateTextView.setVisibility(View.GONE);
		        		newestCheck.setVisibility(View.GONE);
		        		oldestCheck.setVisibility(View.GONE);
		        		bySizeTextView.setVisibility(View.GONE);
		        		largestCheck.setVisibility(View.GONE);
		        		smallestCheck.setVisibility(View.GONE);
		        		
		        		switch(orderOffline){
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
		        		
		        		final AlertDialog dialog = sortByDialog;
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
		        		AlertDialog sortByDialog;		        		
		        		LayoutInflater inflater = getLayoutInflater();
		        		View dialoglayout = inflater.inflate(R.layout.sortby_dialog, null);
		        		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		        		builder.setView(dialoglayout);
		        		builder.setTitle(getString(R.string.action_sort_by));
		        		builder.setPositiveButton(getString(R.string.general_cancel), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
		        		
		        		sortByDialog = builder.create();
		        		sortByDialog.show();
		        		Util.brandAlertDialog(sortByDialog);
		        		
		        		TextView byNameTextView = (TextView) sortByDialog.findViewById(R.id.sortby_dialog_name_text);
		        		byNameTextView.setText(getString(R.string.sortby_name));
		        		final CheckedTextView ascendingCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_ascending_check);
		        		ascendingCheck.setText(getString(R.string.sortby_name_ascending));
		        		final CheckedTextView descendingCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_descending_check);
		        		descendingCheck.setText(getString(R.string.sortby_name_descending));
		        		
		        		TextView byDateTextView = (TextView) sortByDialog.findViewById(R.id.sortby_dialog_date_text);
		        		byDateTextView.setText(getString(R.string.sortby_date));
		        		final CheckedTextView newestCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_newest_check);
		        		newestCheck.setText(getString(R.string.sortby_date_newest));
		        		final CheckedTextView oldestCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_oldest_check);
		        		oldestCheck.setText(getString(R.string.sortby_date_oldest));
		        		
		        		TextView bySizeTextView = (TextView) sortByDialog.findViewById(R.id.sortby_dialog_size_text);
		        		bySizeTextView.setText(getString(R.string.sortby_size));
		        		final CheckedTextView largestCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_largest_first_check);
		        		largestCheck.setText(getString(R.string.sortby_size_largest_first));
		        		final CheckedTextView smallestCheck = (CheckedTextView) sortByDialog.findViewById(R.id.sortby_dialog_smallest_first_check);
		        		smallestCheck.setText(getString(R.string.sortby_size_smallest_first));
		        		
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
		        		
		        		final AlertDialog dialog = sortByDialog;
		        		
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

	        
	        case R.id.action_menu_change_pass:{

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

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
								String toastMessage = getString(R.string.toast_master_key) + " " + path;
								Toast.makeText(getBaseContext(), toastMessage, Toast.LENGTH_LONG).show();	
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

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
	
	
	
	public void showCancelMessage(){
		AlertDialog cancelDialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
					Toast.makeText(managerActivity, getString(R.string.reason_cancel_subscriptions), Toast.LENGTH_SHORT).show();
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
		Util.brandAlertDialog(cancelDialog);
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
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.confirmation_cancel_subscriptions).setPositiveButton(R.string.general_yes, dialogClickListener)
		    .setNegativeButton(R.string.general_no, dialogClickListener).show();		
		
	}
	
	public void selectSortByContacts(int _orderContacts){
		this.orderContacts = _orderContacts;

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
	
	public void selectSortByOffline(int _orderOffline){
		log("selectSortByOffline");
		
		this.orderOffline = _orderOffline;
		

	}
	
	public void selectSortByIncoming(int _orderIncoming){
		log("selectSortByIncoming");
		
		this.orderIncoming = _orderIncoming;
		

	}
	
	public void selectSortByOutgoing(int _orderOutgoing){
		log("selectSortByOutgoing");
		
		this.orderOutgoing = _orderOutgoing;
		

	}
	
	public void selectSortByCloudDrive(int _orderGetChildren){
		this.orderGetChildren = _orderGetChildren;
		MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
		if (parentNode != null){

		}
		else{

		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		log("onItemClick");
		if (megaApi == null){
			megaApi = ((MegaApplication)getApplication()).getMegaApi();
		}
		
		log("retryPendingConnections()");
		if (megaApi != null){
			megaApi.retryPendingConnections();
		}
			
		if (nDA != null){
			nDA.setPositionClicked(position);
		}
		
//		if (position >= 3){
//			position++;
//		}
		
		lastDrawerItem = drawerItem;
		drawerItem = DrawerItem.values()[position];
		
		if (drawerItem != lastDrawerItem){
			if (drawerItem != DrawerItem.SETTINGS){
				titleAB = "";
			}
			else{
				getSupportActionBar().setTitle(titleAB);
			}
				
			selectDrawerItem(drawerItem);			
		}
		else{
			getSupportActionBar().setTitle(titleAB);
			titleAB = "";
			mDrawerLayout.closeDrawer(Gravity.LEFT);
		}
	}

	public void uploadFile(){
		uploadDialog = new UploadHereDialog();
		uploadDialog.show(getSupportFragmentManager(), "fragment_upload");
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
//			case R.id.top_control_bar:{
//				if (nDA != null){
//					nDA.setPositionClicked(-1);
//				}
//				drawerItem = DrawerItem.ACCOUNT;
//				titleAB = drawerItem.getTitle(this);
//
//				selectDrawerItem(drawerItem);
//				break;
//			}
//			case R.id.bottom_control_bar:{
//				if (nDA != null){
//					nDA.setPositionClicked(-1);
//				}
//				drawerItem = DrawerItem.ACCOUNT;
//				titleAB = drawerItem.getTitle(this);
//
//				selectDrawerItem(drawerItem);
//				break;
//			}
		}
	}
	
	static public void logout(Context context, MegaApiAndroid megaApi, boolean confirmAccount) {
		logout(context, megaApi, confirmAccount, false);
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
	
	 /*
	 * Logout user
	 */
	static public void logout(Context context, MegaApiAndroid megaApi, boolean confirmAccount, boolean logoutBadSession) {
//		context.stopService(new Intent(context, BackgroundService.class));
		log("logout");
//		context.stopService(new Intent(context, CameraSyncService.class));
		
		File offlineDirectory = null;
		if (Environment.getExternalStorageDirectory() != null){
			offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR);
		}
//		if (context.getExternalFilesDir(null) != null){
//			offlineDirectory = context.getExternalFilesDir(null);
//		}
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
		
		final String pathMK = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
		final File fMK = new File(pathMK);
    	if (fMK.exists()){
    		fMK.delete();	
    	}
		
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
//			deleteFolderAndSubfolders(context, c);
		}
		
		
//		DatabaseHandler dbH = new DatabaseHandler(context);
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
//			dbH.setPinLockEnabled(false);
//			dbH.setPinLockCode("");
//			dbH.setCamSyncEnabled(false);
//			dbH.setStorageAskAlways(true);
			Intent stopIntent = null;
			stopIntent = new Intent(context, CameraSyncService.class);
			stopIntent.setAction(CameraSyncService.ACTION_LOGOUT);
			context.startService(stopIntent);
		}
		dbH.clearOffline();
		dbH.clearAttributes();
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		if (!logoutBadSession){
			megaApi.logout();
		}
		drawerItem = null;
		
		if (!confirmAccount){		
			if(managerActivity != null)	{

			}
			else{
				Intent intent = new Intent (context, LauncherActivity.class);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
				if (context instanceof Activity){
					context.startActivity(intent);
					((Activity)context).finish();
				}
				else{
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent);
				}
				context = null;
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
	

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: "  + request.getRequestString());
		if (request.getType() == MegaRequest.TYPE_ACCOUNT_DETAILS){
			log("account_details request start");
		}
		else if (request.getType() == MegaRequest.TYPE_LOGOUT){
			log("logout request start");
		}	
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			log("fecthnodes request start");
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			log("move request start");
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE){
			log("remove request start");
		}
		else if (request.getType() == MegaRequest.TYPE_EXPORT){
			log("export request start");
		}
		else if(request.getType() == MegaRequest.TYPE_RENAME){
			log("rename request start");
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			log("copy request start");
		}
		else if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER){
			log("create folder start");
		}
		else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS){
			log("pause transfers start");
		}
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		log("---------onRequestFinish: "  + request.getRequestString());
		
		if (request.getType() == MegaRequest.TYPE_ACCOUNT_DETAILS){
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

				log("LEVELACCOUNTDETAILS: " + levelAccountDetails + "; LEVELINVENTORY: " + levelInventory + "; INVENTORYFINISHED: " + inventoryFinished);
				
				if (inventoryFinished){
					if (levelAccountDetails < levelInventory){
						if (maxP != null){
							log("ORIGINAL JSON2:" + maxP.getOriginalJson() + ":::");
							megaApi.submitPurchaseReceipt(maxP.getOriginalJson(), this);
						}
					}
				}
				
				long totalStorage = accountInfo.getStorageMax();
				long usedStorage = accountInfo.getStorageUsed();;
				boolean totalGb = false;				
		        
		        bottomControlBar.setVisibility(View.VISIBLE);
		        usedPerc = 0;
		        if (totalStorage != 0){
		        	usedPerc = (int)((100 * usedStorage) / totalStorage);
		        }
		        usedSpaceBar.setProgress(usedPerc);
				
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
				String usedSpaceString = used + " / " + total;
		        usedSpace.setText(usedSpaceString);
		        Spannable wordtoSpan = new SpannableString(usedSpaceString);

		        if (usedPerc < 90){
		        	usedSpaceBar.setProgressDrawable(getResources().getDrawable(R.drawable.custom_progress_bar_horizontal_ok));
		        	wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.used_space_ok)), 0, used.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        	usedSpaceWarning.setVisibility(View.INVISIBLE);
		        }
		        else if ((usedPerc >= 90) && (usedPerc <= 95)){
		        	usedSpaceBar.setProgressDrawable(getResources().getDrawable(R.drawable.custom_progress_bar_horizontal_warning));
		        	wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.used_space_warning)), 0, used.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        	usedSpaceWarning.setVisibility(View.VISIBLE);
		        }
		        else{
		        	if (usedPerc > 100){
			        	usedPerc = 100;			        	
			        }
		        	usedSpaceWarning.setVisibility(View.VISIBLE);
		        	usedSpaceBar.setProgressDrawable(getResources().getDrawable(R.drawable.custom_progress_bar_horizontal_exceed));    
		        	wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.used_space_exceed)), 0, used.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        }      
		        
		        wordtoSpan.setSpan(new RelativeSizeSpan(1.5f), 0, used.length() - 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.navigation_drawer_mail)), used.length() + 1, used.length() + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        wordtoSpan.setSpan(new RelativeSizeSpan(1.5f), used.length() + 3, used.length() + 3 + total.length() - 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        usedSpace.setText(wordtoSpan);	
		        
		        log("onRequest TYPE_ACCOUNT_DETAILS: "+usedPerc);

		        if(drawerItem==DrawerItem.CLOUD_DRIVE){
		        	if (usedPerc > 95){

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
//							if (maF != null){
//								cancelSubscription.setVisible(true);
//							}
						}
					}
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_CREDIT_CARD_CANCEL_SUBSCRIPTIONS){
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, getString(R.string.cancel_subscription_ok), Toast.LENGTH_SHORT).show();
			}
			else{
				Toast.makeText(this, getString(R.string.cancel_subscription_error), Toast.LENGTH_SHORT).show();
			}
			megaApi.creditCardQuerySubscriptions(this);
		}
		else if (request.getType() == MegaRequest.TYPE_LOGOUT){
			log("logout finished");
//			if (request.getType() == MegaRequest.TYPE_LOGOUT){
//				log("type_logout");
//				if (e.getErrorCode() == MegaError.API_ESID){
//					log("calling ManagerActivity.logout");
//					MegaApiAndroid megaApi = app.getMegaApi(); 
//					ManagerActivity.logout(managerActivity, app, megaApi, false);
//				}
//			}
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			log("fecthnodes request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE_CONTACT){
			
			if (e.getErrorCode() == MegaError.API_OK){
			

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
			
			if(request.getNumber()==MegaContactRequest.INVITE_ACTION_REMIND){
				Toast.makeText(this, getString(R.string.context_contact_invitation_resent), Toast.LENGTH_LONG).show();
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					
					if(request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD)
					{
						Toast.makeText(this, getString(R.string.context_contact_added), Toast.LENGTH_LONG).show();					
					}
					else if(request.getNumber()==MegaContactRequest.INVITE_ACTION_DELETE)
					{
						Toast.makeText(this, getString(R.string.context_contact_invitation_deleted), Toast.LENGTH_LONG).show();					
					}
//					else
//					{
//						Toast.makeText(this, getString(R.string.context_contact_invitation_resent), Toast.LENGTH_LONG).show();					
//					}				
				}
				else{
					if(e.getErrorCode()==MegaError.API_EEXIST)
					{
						Toast.makeText(this, getString(R.string.context_contact_already_exists, request.getEmail()), Toast.LENGTH_LONG).show();
					}
					else{
						Toast.makeText(this, getString(R.string.general_error), Toast.LENGTH_LONG).show();
					}				
					log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
				}
			}
		}
		else if (request.getType() == MegaRequest.TYPE_REPLY_CONTACT_REQUEST){	
			log("MegaRequest.TYPE_REPLY_CONTACT_REQUEST finished: "+request.getType());
			
			if (e.getErrorCode() == MegaError.API_OK){
				
				Toast.makeText(this, getString(R.string.context_invitacion_reply), Toast.LENGTH_LONG).show();
	//			Toast.makeText(this, getString(R.string.context_correctly_moved), Toast.LENGTH_SHORT).show();

			}
			else{
				Toast.makeText(this, getString(R.string.general_error), Toast.LENGTH_LONG).show();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (moveToRubbish){
				if (e.getErrorCode() == MegaError.API_OK){
					Toast.makeText(this, getString(R.string.context_correctly_moved), Toast.LENGTH_SHORT).show();
				}
				else{
					Toast.makeText(this, getString(R.string.context_no_moved), Toast.LENGTH_LONG).show();
				}
				moveToRubbish = false;
				log("move to rubbish request finished");
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					Toast.makeText(this, getString(R.string.context_correctly_moved), Toast.LENGTH_SHORT).show();
				}
				else{
					Toast.makeText(this, getString(R.string.context_no_moved), Toast.LENGTH_LONG).show();
				}
			
				log("move nodes request finished");
			}			
			if (e.getErrorCode() == MegaError.API_OK){
//				Toast.makeText(this, getString(R.string.context_correctly_moved), Toast.LENGTH_SHORT).show();
				if (drawerItem == DrawerItem.CLOUD_DRIVE){

				}


				if (drawerItem == DrawerItem.SHARED_WITH_ME){

				}
			}	
		}
		else if (request.getType() == MegaRequest.TYPE_KILL_SESSION){
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, getString(R.string.success_kill_all_sessions), Toast.LENGTH_SHORT).show();
			}
			else
			{
				log("error when killing sessions: "+e.getErrorString());
				Toast.makeText(this, getString(R.string.error_kill_all_sessions), Toast.LENGTH_SHORT).show();
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
						Toast.makeText(this, getString(R.string.context_correctly_removed), Toast.LENGTH_SHORT).show();
					}
				}
				

	
			}
			else{
				Toast.makeText(this, getString(R.string.context_no_removed), Toast.LENGTH_LONG).show();
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
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle(getString(R.string.context_get_link_menu));
					
//					LayoutInflater inflater = getLayoutInflater();
//					View dialoglayout = inflater.inflate(R.layout.dialog_link, null);
//					ImageView thumb = (ImageView) dialoglayout.findViewById(R.id.dialog_link_thumbnail);
//					TextView url = (TextView) dialoglayout.findViewById(R.id.dialog_link_link_url);
//					TextView key = (TextView) dialoglayout.findViewById(R.id.dialog_link_link_key);
//
//					String urlString = "";
//					String keyString = "";
//					String [] s = link.split("!");
//					if (s.length == 3){
//						urlString = s[0] + "!" + s[1];
//						keyString = s[2];
//					}
//					if (node.isFolder()){
//						thumb.setImageResource(R.drawable.folder_thumbnail);
//					}
//					else{
//						thumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
//					}
//
//					Display display = getWindowManager().getDefaultDisplay();
//					DisplayMetrics outMetrics = new DisplayMetrics();
//					display.getMetrics(outMetrics);
//					float density = getResources().getDisplayMetrics().density;
//
//					float scaleW = Util.getScaleW(outMetrics, density);
//					float scaleH = Util.getScaleH(outMetrics, density);
//
//					url.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleW));
//					key.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleW));
//
//					url.setText(urlString);
//					key.setText(keyString);
//
//
//					builder.setView(dialoglayout);
//
//					builder.setPositiveButton(getString(R.string.context_send_link), new android.content.DialogInterface.OnClickListener() {
//
//						@Override
//						public void onClick(DialogInterface dialog, int which) {
//							Intent intent = new Intent(Intent.ACTION_SEND);
//							intent.setType("text/plain");
//							intent.putExtra(Intent.EXTRA_TEXT, link);
//							startActivity(Intent.createChooser(intent, getString(R.string.context_get_link)));
//						}
//					});
//
//					builder.setNegativeButton(getString(R.string.context_copy_link), new android.content.DialogInterface.OnClickListener() {
//
//						@Override
//						public void onClick(DialogInterface dialog, int which) {
//							if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
//							    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
//							    clipboard.setText(link);
//							} else {
//							    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
//							    android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", link);
//					            clipboard.setPrimaryClip(clip);
//							}
//
//							Toast.makeText(managerActivity, getString(R.string.file_properties_get_link), Toast.LENGTH_LONG).show();
//						}
//					});
//
//					getLinkDialog = builder.create();
//					getLinkDialog.show();
//					Util.brandAlertDialog(getLinkDialog);
				}
			}
			else{
				Toast.makeText(this, getString(R.string.context_no_link), Toast.LENGTH_LONG).show();
			}
			log("export request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_RENAME){
			
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, getString(R.string.context_correctly_renamed), Toast.LENGTH_SHORT).show();


			}
			else{
				Toast.makeText(this, getString(R.string.context_no_renamed), Toast.LENGTH_LONG).show();
			}
		} 
		else if (request.getType() == MegaRequest.TYPE_COPY){
			log("TYPE_COPY");
			if(sendToInbox){
				log("sendToInbox");
				if (drawerItem == DrawerItem.INBOX||drawerItem == DrawerItem.CLOUD_DRIVE||drawerItem == DrawerItem.CONTACTS){
					sendToInbox=false;
					if (e.getErrorCode() == MegaError.API_OK){
						Toast.makeText(this, getString(R.string.context_correctly_sent), Toast.LENGTH_SHORT).show();
					}
					else if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
						log("OVERQUOTA ERROR: "+e.getErrorCode());
						showOverquotaAlert();
					}
					else
					{
						Toast.makeText(this, getString(R.string.context_no_sent), Toast.LENGTH_LONG).show();
					}
				}				
			}
			else{
				try { 
					statusDialog.dismiss();	
				} 
				catch (Exception ex) {}
				
				if (e.getErrorCode() == MegaError.API_OK){
					Toast.makeText(this, getString(R.string.context_correctly_copied), Toast.LENGTH_SHORT).show();


				}
				else{
					if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
						log("OVERQUOTA ERROR: "+e.getErrorCode());
						showOverquotaAlert();
					}
					else
					{
						Toast.makeText(this, getString(R.string.context_no_copied), Toast.LENGTH_LONG).show();
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
				Toast.makeText(this, getString(R.string.context_folder_created), Toast.LENGTH_LONG).show();

			}
		}
		else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER){
			boolean avatarExists = false;
			if (e.getErrorCode() == MegaError.API_OK) {

				if (request.getParamType() == 1) {
					log("(1)request.getText(): " + request.getText());
					nameText = request.getText();
					name = true;
				} else if (request.getParamType() == 2) {
					log("(2)request.getText(): " + request.getText());
					firstNameText = request.getText();
					firstName = true;
				}
				if (name && firstName) {
					userName.setText(nameText + " " + firstNameText);
					name = false;
					firstName = false;
				}

				if (request.getParamType() == 0) {
					File avatar = null;
					if (getExternalCacheDir() != null) {
						avatar = new File(getExternalCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
					} else {
						avatar = new File(getCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
					}
					Bitmap imBitmap = null;
					if (avatar.exists()) {
						if (avatar.length() > 0) {
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
							} else {
								avatarExists = true;
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
								imageProfile.setImageBitmap(circleBitmap);
								textViewProfile.setVisibility(View.GONE);
							}
						}
					}
				}
				log("avatar user downloaded");
			}
		}
		else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS){
			if (e.getErrorCode() == MegaError.API_OK) {

			}
		}
		else if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFER){
			if (e.getErrorCode() == MegaError.API_OK){

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
				selectDrawerItem(drawerItem);
			}
			else{
				log("PURCHASE WRONG: " + e.getErrorString() + " (" + e.getErrorCode() + ")");
//				Toast.makeText(this, "PURCHASE WRONG: " + e.getErrorString() + " (" + e.getErrorCode() + ")", Toast.LENGTH_LONG).show();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_CLEAN_RUBBISH_BIN){
			if (e.getErrorCode() == MegaError.API_OK){
				log("OK MegaRequest.TYPE_CLEAN_RUBBISH_BIN");
				Toast.makeText(this, getString(R.string.rubbish_bin_emptied), Toast.LENGTH_LONG).show();
			}
			else{
				Toast.makeText(this, getString(R.string.rubbish_bin_no_emptied), Toast.LENGTH_LONG).show();
			}
		}
	}

	private void showOverquotaAlert(){
		
		dbH.setCamSyncEnabled(false);
		
		if(overquotaDialog==null){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.overquota_alert_title));
			LayoutInflater inflater = getLayoutInflater();
			View dialoglayout = inflater.inflate(R.layout.dialog_overquota_error, null);
			TextView textOverquota = (TextView) dialoglayout.findViewById(R.id.dialog_overquota);
			builder.setView(dialoglayout);
			

			builder.setNegativeButton(getString(R.string.general_cancel), new android.content.DialogInterface.OnClickListener() {			

				@Override
				public void onClick(DialogInterface dialog, int which) {						
					dialog.dismiss();	
					overquotaDialog=null;
				}
			});
			
			overquotaDialog = builder.create();
			overquotaDialog.show();
			Util.brandAlertDialog(overquotaDialog);
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
	
	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,MegaError e) {
		log("onRequestTemporaryError: "  + request.getRequestString());
		if (request.getType() == MegaRequest.TYPE_LOGOUT){
			log("logout temporary error");
		}	
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			log("fetchnodes temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			log("move temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE){
			log("remove temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_EXPORT){
			log("export temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_RENAME){
			log("rename temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			log("copy temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER){
			log("create folder temporary error");
		}
		else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER){
			log("get user attribute temporary error");
		}
	}
	
	public ActionBarDrawerToggle getmDrawerToggle() {
		log("getmDrawerToggle");
		return mDrawerToggle;
	}

	public void setmDrawerToggle(ActionBarDrawerToggle mDrawerToggle) {
		log("setmDrawerToggle");
		this.mDrawerToggle = mDrawerToggle;
	}
	
	File destination;
	
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
					Toast.makeText(this, getString(R.string.context_select_one_file), Toast.LENGTH_LONG).show();
				}		    	
			}

		}
		else{
			log("NOT askMe");
			File defaultPathF = new File(downloadLocationDefaultPath);
			defaultPathF.mkdirs();
			downloadTo(downloadLocationDefaultPath, null, size, hashes);
		}		
	}
	
	public void moveToTrash(final ArrayList<Long> handleList){
		log("moveToTrash");
		isClearRubbishBin = false;
		
		if (!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
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
				AlertDialog.Builder builder = new AlertDialog.Builder(managerActivity);
				String message= getResources().getString(R.string.confirmation_move_to_rubbish);
				builder.setMessage(message).setPositiveButton(R.string.general_yes, dialogClickListener)
			    	.setNegativeButton(R.string.general_no, dialogClickListener).show();
			}
			else{
				AlertDialog.Builder builder = new AlertDialog.Builder(managerActivity);
				String message= getResources().getString(R.string.confirmation_delete_from_mega);
				builder.setMessage(message).setPositiveButton(R.string.general_yes, dialogClickListener)
			    	.setNegativeButton(R.string.general_no, dialogClickListener).show();
			}
		}
		
	}
	
	public void getPublicLinkAndShareIt(MegaNode document){
		log("getPublicLinkAndShareIt");
		if (!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
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

	public void showClearRubbishBinDialog(String editText){
		log("showClearRubbishBinDialog");


		
		String text;
		if ((editText == null) || editText.equals("")){
			text = getString(R.string.context_clear_rubbish);
		}
		else{
			text = editText;
		}
		
		AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, getString(R.string.context_clear_rubbish), null, null);
		builder.setPositiveButton(getString(R.string.general_empty),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						cleanRubbishBin();
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), null);
		clearRubbishBinDialog = builder.create();
		clearRubbishBinDialog.show();
		Util.brandAlertDialog(clearRubbishBinDialog);
	}
	
	private String getFragmentTag(int viewPagerId, int fragmentPosition)
	{
	     return "android:switcher:" + viewPagerId + ":" + fragmentPosition;
	}
	
	public void showNewContactDialog(String editText){
		log("showNewContactDialog");
		

		
		String text;
		if ((editText == null) || editText.equals("")){
			text = getString(R.string.context_new_contact_name);
		}
		else{
			text = editText;
		}
		
		final EditText input = new EditText(this);
//		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setSelectAllOnFocus(true);
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
		AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, getString(R.string.menu_add_contact),
				null, input);
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
		addContactDialog = builder.create();
		addContactDialog.show();
	}
	
	public void pickFolderToShare(List<MegaUser> users){
		
		Intent intent = new Intent(this, LauncherFileExplorerActivity.class);
		intent.setAction(LauncherFileExplorerActivity.ACTION_SELECT_FOLDER);
		String[] longArray = new String[users.size()];
		for (int i=0; i<users.size(); i++){
			longArray[i] = users.get(i).getEmail();
		}
		intent.putExtra("SELECTED_CONTACTS", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER);
		
	}	

	public void showNewFolderDialog(String editText){
		log("showNewFolderDialog");

		
		String text;
		if (editText == null || editText.equals("")){
			text = getString(R.string.context_new_folder_name);
		}
		else{
			text = editText;
		}
		
		final EditText input = new EditText(this);
//		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
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
		input.setImeActionLabel(getString(R.string.general_create),
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
		AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, getString(R.string.menu_new_folder),
				null, input);
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
		newFolderDialog = builder.create();
		newFolderDialog.show();
	}
	
	private void cleanRubbishBin(){
		log("cleanRubbishBin");
		megaApi.cleanRubbishBin(managerActivity);
	}
	
	public void inviteContact(String contactEmail){
		log("inviteContact");
		
		if (!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
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
	
	private void createFolder(String title) {
		log("createFolder");
		if (!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
			return;
		}
		
		if(isFinishing()){
			return;	
		}
		
		long parentHandle;


	}
	
	public void showRenameDialog(final MegaNode document, String text){
		log("showRenameDialog");
		final EditTextCursorWatcher input = new EditTextCursorWatcher(this, document.isFolder());
//		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);

		input.setImeActionLabel(getString(R.string.context_rename),
				KeyEvent.KEYCODE_ENTER);
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

		AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, getString(R.string.context_rename) + " "	+ new String(document.getName()), null, input);
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
		renameDialog = builder.create();
		renameDialog.show();

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
	}
	
	public void showImportLinkDialog(){
		log("showRenameDialog");
		final EditText input = new EditText(this);
//		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);

		input.setImeActionLabel(getString(R.string.context_open_link_title),KeyEvent.KEYCODE_ENTER);
		AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, getString(R.string.context_open_link_title), null, input);
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
		openLinkDialog = builder.create();
		openLinkDialog.show();
		Util.brandAlertDialog(openLinkDialog);
		
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
	
	private void rename(MegaNode document, String newName){
		log("rename");
		if (newName.equals(document.getName())) {
			return;
		}
		
		if(!Util.isOnline(this)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
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
	
	public void leaveMultipleShares (ArrayList<Long> handleList){
	
		for (int i=0; i<handleList.size(); i++){
			MegaNode node = megaApi.getNodeByHandle(handleList.get(i));
			this.leaveIncomingShare(node);
		}
	}
	
	public void showMove(ArrayList<Long> handleList){
		log("showMove");
		Intent intent = new Intent(this, LauncherFileExplorerActivity.class);
		intent.setAction(LauncherFileExplorerActivity.ACTION_PICK_MOVE_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("MOVE_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
	}
	
	public void showCopy(ArrayList<Long> handleList){
		log("showCopy");
		Intent intent = new Intent(this, LauncherFileExplorerActivity.class);
		intent.setAction(LauncherFileExplorerActivity.ACTION_PICK_COPY_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("COPY_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER);
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
					uploadServiceIntent = new Intent (ManagerActivity.this, UploadService.class);
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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		log("onActivityResult "+requestCode + "____" + resultCode);
		
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
			
			Uri uri = intent.getData();
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
				Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
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
				Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				return;
			}
			
			final String[] selectedContacts = intent.getStringArrayExtra("SELECTED_CONTACTS");
			final long folderHandle = intent.getLongExtra("SELECT", 0);			
			
			final MegaNode parent = megaApi.getNodeByHandle(folderHandle);
			
			if (parent.isFolder()){
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
				permissionsDialog = dialogBuilder.create();
				permissionsDialog.show();
				Resources resources = permissionsDialog.getContext().getResources();
				int alertTitleId = resources.getIdentifier("alertTitle", "id", "android");
				TextView alertTitle = (TextView) permissionsDialog.getWindow().getDecorView().findViewById(alertTitleId);
		        alertTitle.setTextColor(resources.getColor(R.color.mega));
				int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
				View titleDivider = permissionsDialog.getWindow().getDecorView().findViewById(titleDividerId);
				if(titleDivider!=null){
					titleDivider.setBackgroundColor(resources.getColor(R.color.mega));
				}
			}
		}	
		else if (requestCode == REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK){
			
			if (intent == null) {			
				log("Return.....");
				return;						
			}
			
			if(!Util.isOnline(this)){
				Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				return;
			}
//
//			contactsData = intent.getStringArrayListExtra(ContactsExplorerActivity.EXTRA_CONTACTS);
//			megaContacts = intent.getBooleanExtra(ContactsExplorerActivity.EXTRA_MEGA_CONTACTS, true);
			
			final int multiselectIntent = intent.getIntExtra("MULTISELECT", -1);
			final int sentToInbox = intent.getIntExtra("SEND_FILE", -1);
			
			if (megaContacts){
				
				if(sentToInbox==0){
					//Just one folder to share
					if(multiselectIntent==0){
	
						final long nodeHandle = -1;
						final MegaNode node = megaApi.getNodeByHandle(nodeHandle);
						
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
						permissionsDialog = dialogBuilder.create();
						permissionsDialog.show();
						Resources resources = permissionsDialog.getContext().getResources();
						int alertTitleId = resources.getIdentifier("alertTitle", "id", "android");
						TextView alertTitle = (TextView) permissionsDialog.getWindow().getDecorView().findViewById(alertTitleId);
				        alertTitle.setTextColor(resources.getColor(R.color.mega));
						int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
						View titleDivider = permissionsDialog.getWindow().getDecorView().findViewById(titleDividerId);
						if(titleDivider!=null){
							titleDivider.setBackgroundColor(resources.getColor(R.color.mega));
						}						
					}
					else if(multiselectIntent==1){
						//Several folder to share

					}
				}
				else if (sentToInbox==1){

				}

			}
			else{

				for (int i=0; i < contactsData.size();i++){
					String type = contactsData.get(i);

				}

			}			
		}		
			else if (requestCode == REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK) {
		
			if (intent == null) {			
				log("Return.....");
				return;						
			}
			
			if(!Util.isOnline(this)){
				Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
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
				Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
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

		else if (requestCode == REQUEST_CODE_REFRESH && resultCode == RESULT_OK) {
			
			if (intent == null) {			
				log("Return.....");
				return;						
			}
			
			if (drawerItem == DrawerItem.CLOUD_DRIVE){
				parentHandleBrowser = intent.getLongExtra("PARENT_HANDLE", -1);
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleBrowser);
				if (parentNode != null){

				}
				else{

				}
			}
			else if (drawerItem == DrawerItem.RUBBISH_BIN){
				parentHandleRubbish = intent.getLongExtra("PARENT_HANDLE", -1);
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleRubbish);

			}
			else if (drawerItem == DrawerItem.SHARED_WITH_ME){
				parentHandleIncoming = intent.getLongExtra("PARENT_HANDLE", -1);
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleIncoming);

			}
		}
		else if (requestCode == TAKE_PHOTO_CODE){
			log("Entrooo en requestCode");
			if(resultCode == Activity.RESULT_OK){
				
				log("REcibo el intent OOOOKK");
				Intent intentPicture = new Intent(this, SecureSelfiePreviewActivity.class);			
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

				}
				else{

				}
			}
			else if (drawerItem == DrawerItem.RUBBISH_BIN){
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleRubbish);

			}
			else if (drawerItem == DrawerItem.SHARED_WITH_ME){
				MegaNode parentNode = megaApi.getNodeByHandle(parentHandleIncoming);

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
	 * Handle processed upload intent
	 */
	public void onIntentProcessed() {
		log("onIntentProcessed");
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

			parentNode = megaApi.getNodeByHandle(parentHandle);
		}
		else if (drawerItem == DrawerItem.SHARED_WITH_ME){

		}
		
		if(parentNode == null){
			Util.showErrorAlertDialog(getString(R.string.error_temporary_unavaible), false, this);
			return;
		}
			
		if (infos == null) {
			Util.showErrorAlertDialog(getString(R.string.upload_can_not_open),
					false, this);
		} 
		else {
			Toast.makeText(getApplicationContext(), getString(R.string.upload_began),
					Toast.LENGTH_SHORT).show();
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
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
		log("onUsersUpdate");

	}
	
	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> updatedNodes) {
		log("onNodesUpdate");
		try { 
			statusDialog.dismiss();	
		} 
		catch (Exception ex) {}
		
		if (drawerItem == DrawerItem.CLOUD_DRIVE){

		}


		
		if (drawerItem == DrawerItem.SHARED_WITH_ME){
			int index = viewPagerShares.getCurrentItem();

		}
		if (drawerItem == DrawerItem.CAMERA_UPLOADS){
//			if (psF != null){
//				if(psF.isAdded()){
//					long cameraUploadHandle = psF.getPhotoSyncHandle();
//					MegaNode nps = megaApi.getNodeByHandle(cameraUploadHandle);
//					log("cameraUploadHandle: " + cameraUploadHandle);
//					if (nps != null){
//						log("nps != null");
//						ArrayList<MegaNode> nodes = megaApi.getChildren(nps, MegaApiJava.ORDER_MODIFICATION_DESC);
//						psF.setNodes(nodes);
//					}
//				}
//			}
		}

	}	
	
	@Override
	public void onReloadNeeded(MegaApiJava api) {
		log("onReloadNeeded");
	}	
	
	public void setParentHandleBrowser(long parentHandleBrowser){
		log("setParentHandleBrowser");
		
		this.parentHandleBrowser = parentHandleBrowser;


	}
	
	public void setParentHandleInbox(long parentHandleInbox){
		log("setParentHandleInbox");
		this.parentHandleInbox = parentHandleInbox;
	}
	
	public void setParentHandleRubbish(long parentHandleRubbish){
		log("setParentHandleRubbish");
		this.parentHandleRubbish = parentHandleRubbish;
	}
	
	public void setParentHandleIncoming(long parentHandleSharedWithMe){
		log("setParentHandleSharedWithMe");
		this.parentHandleIncoming = parentHandleSharedWithMe;
	}
	
	public void setParentHandleOutgoing(long parentHandleSharedWithMe){
		log("setParentHandleSharedWithMe");
		this.parentHandleOutgoing = parentHandleSharedWithMe;
	}
	
	public void setParentHandleSearch(long parentHandleSearch){
		log("setParentHandleSearch");
		this.parentHandleSearch = parentHandleSearch;
	}
	
	public void setPathNavigationOffline(String pathNavigation){
		this.pathNavigation = pathNavigation;
	}
	
	public void setPauseIconVisible(boolean visible){
		log("setPauseIconVisible");
		pauseIconVisible = visible;
		if (pauseRestartTransfersItem != null){
			pauseRestartTransfersItem.setVisible(visible);
		}
	}
	
	public void setTransfers(ArrayList<MegaTransfer> transfersList){
		log("setTransfers");

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
	
	public void setDownloadPlay(boolean downloadPlay){
		log("setDownloadPlay");
		this.downloadPlay = downloadPlay;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		log("onKeyUp");
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		log("onTransferStart");

		HashMap<Long, MegaTransfer> mTHash = new HashMap<Long, MegaTransfer>();
		log("onTransferStart: " + transfer.getFileName() + " - " + transfer.getTag());

	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError e) {
		log("onTransferFinish"); 
		
//		ThumbnailUtils.pendingThumbnails.remove(transfer.getNodeHandle());
		
		
		HashMap<Long, MegaTransfer> mTHash = new HashMap<Long, MegaTransfer>();

		log("onTransferFinish: " + transfer.getFileName() + " - " + transfer.getTag());
	}
	
	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
		log("onTransferUpdate: " + transfer.getFileName() + " - " + transfer.getTag());


		


	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {
		
		log("onTransferTemporaryError: " + transfer.getFileName() + " - " + transfer.getTag());
	}
	
	public static void log(String message) {
		Util.log("ManagerActivity", message);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate: "  + request.getRequestString());		
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
					Util.showErrorAlertDialog(getString(R.string.error_not_enough_free_space), false, this);
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
								String toastMessage = getString(R.string.general_already_downloaded) + ": " + localPath;
								Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
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
							Util.showErrorAlertDialog(getString(R.string.error_not_enough_free_space) + " (" + new String(document.getName()) + ")", false, this);
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
						Util.showErrorAlertDialog(getString(R.string.error_not_enough_free_space), false, this);
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
	
	@Override
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer)
	{
		log("onTransferData");
		return true;
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

			AlertDialog.Builder builder = new AlertDialog.Builder(managerActivity);
			builder.setMessage(getResources().getString(R.string.confirmation_remove_contact,c.getEmail())).setPositiveButton(R.string.general_yes, dialogClickListener)
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

			AlertDialog.Builder builder = new AlertDialog.Builder(managerActivity);
			String message= getResources().getString(R.string.confirmation_remove_contact)+" "+c.getEmail()+"?";
			builder.setMessage(message).setPositiveButton(R.string.general_yes, dialogClickListener)
			    .setNegativeButton(R.string.general_no, dialogClickListener).show();			
			
		}	
		
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
	
	public void shareFolder(ArrayList<Long> handleList){
		log("shareFolder ArrayListLong");
		//TODO shareMultipleFolders


	}
	
	public void shareFolder(MegaNode node){
		log("shareFolder MegaNode");
		

	}
	
	public void sentToInbox(MegaNode node){
		log("sentToInbox MegaNode");
		
		if((drawerItem == DrawerItem.SHARED_WITH_ME) || (drawerItem == DrawerItem.CLOUD_DRIVE) ){
			sendToInbox = true;			

		}			
	}
	
	public void pickContacToSendFile(List<MegaUser> users){
		
		Intent intent = new Intent(this, LauncherFileExplorerActivity.class);
		intent.setAction(LauncherFileExplorerActivity.ACTION_SELECT_FILE);
		String[] longArray = new String[users.size()];
		for (int i=0; i<users.size(); i++){
			longArray[i] = users.get(i).getEmail();
		}
		intent.putExtra("SELECTED_CONTACTS", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);		
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

	public void removeAllSharingContacts (ArrayList<MegaShare> listContacts, MegaNode node)
	{
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

	}
	
	public void showmyF(int paymentMethod, int type, ArrayList<Product> accounts, BitSet paymentBitSet){
		log("showmyF");
		
		if (paymentBitSet == null){
			if (this.paymentBitSet != null){
				paymentBitSet = this.paymentBitSet;
			}
		}
		
		accountFragment = MONTHLY_YEARLY_FRAGMENT;
		
		mTabHostContacts.setVisibility(View.GONE);    			
		viewPagerContacts.setVisibility(View.GONE); 
		mTabHostShares.setVisibility(View.GONE);    			
		viewPagerShares.setVisibility(View.GONE);
		

	}
	
	public void onUpgrade1Click(View view) {

	}

	public void onUpgrade2Click(View view) {

	}

	public void onUpgrade3Click(View view) {

	}
	
	public void onUpgradeLiteClick(View view){

	}
	
	public void onYearlyClick(View view) {

	}
	
	public void onMonthlyClick(View view) {

	}
	
	public void showMyAccount(){
		drawerItem = DrawerItem.ACCOUNT;
		selectDrawerItem(drawerItem);
	}
	
	public void showCC(int type, ArrayList<Product> accounts, int payMonth, BitSet paymentBitSet){
		showCC(type, accounts, payMonth, false, paymentBitSet);
	}
	
	public void showCC(int type, ArrayList<Product> accounts, int payMonth, boolean refresh, BitSet paymentBitSet){
		
		if (paymentBitSet == null){
			if (this.paymentBitSet != null){
				paymentBitSet = this.paymentBitSet;
			}
		}
		
		accountFragment = CC_FRAGMENT;
		mTabHostContacts.setVisibility(View.GONE);    			
		viewPagerContacts.setVisibility(View.GONE); 
		mTabHostShares.setVisibility(View.GONE);    			
		mTabHostShares.setVisibility(View.GONE);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

	}
	
	public void showFortumo(){
		accountFragment = FORTUMO_FRAGMENT;
		mTabHostContacts.setVisibility(View.GONE);    			
		viewPagerContacts.setVisibility(View.GONE); 
		mTabHostShares.setVisibility(View.GONE);    			
		mTabHostShares.setVisibility(View.GONE);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

	}
	
	public void showCentili(){
		accountFragment = CENTILI_FRAGMENT;
		mTabHostContacts.setVisibility(View.GONE);    			
		viewPagerContacts.setVisibility(View.GONE); 
		mTabHostShares.setVisibility(View.GONE);    			
		mTabHostShares.setVisibility(View.GONE);

	}
	
	public int getUsedPerc()
	{
		return usedPerc;
	}
	
	public void upgradeAccountButton(){
		log("upgradeAccountButton");
		
		if (nDA != null){
			nDA.setPositionClicked(-1);
		}
		drawerItem = DrawerItem.ACCOUNT;
		titleAB = drawerItem.getTitle(this);
    	showUpAF(null);
//    	
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
//							Toast.makeText(this, getString(R.string.not_upgrade_is_possible), Toast.LENGTH_LONG).show();
//						}
//					}
//					else{
//						log("CURRENTLY ACTIVE SUBSCRIPTION");
//						Toast.makeText(this, getString(R.string.not_upgrade_is_possible), Toast.LENGTH_LONG).show();
//					}
//				}
//				else{
//					log("accountType == 0");
//					if (Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD) || Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_FORTUMO) || Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CENTILI)){
//						log("CHECKBITSET --> CC || FORT || INFO");
//						showUpAF(null);
//					}
//					else{
//						Toast.makeText(this, getString(R.string.not_upgrade_is_possible), Toast.LENGTH_LONG).show();
//					}
//				}
//			}
//			else{
//				Toast.makeText(this, getString(R.string.not_upgrade_is_possible), Toast.LENGTH_LONG).show();
//			}
//		}
//		else{
//			Toast.makeText(this, getString(R.string.not_upgrade_is_possible), Toast.LENGTH_LONG).show();
//		}
	}

	public long getNumberOfSubscriptions(){
		if (cancelSubscription != null){
			cancelSubscription.setVisible(false);
		}
		if (numberOfSubscriptions > 0){
			if (cancelSubscription != null){
				if (drawerItem == DrawerItem.ACCOUNT){
//					if (maF != null){
//						cancelSubscription.setVisible(true);
//					}
				}
			}
		}
		return numberOfSubscriptions;
	}
	
	public void setNumberOfSubscriptions(long numberOfSubscriptions){
		this.numberOfSubscriptions = numberOfSubscriptions;
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
	
	public void reinviteContact(MegaContactRequest c)
	{
		log("inviteContact");
		megaApi.inviteContact(c.getTargetEmail(), null, MegaContactRequest.INVITE_ACTION_REMIND, this);
	}
	
	public void removeInvitationContact(MegaContactRequest c)
	{
		log("removeInvitationContact");
		megaApi.inviteContact(c.getTargetEmail(), null, MegaContactRequest.INVITE_ACTION_DELETE, this);
	}
	
	public void acceptInvitationContact(MegaContactRequest c)
	{
		log("acceptInvitationContact");
		megaApi.replyContactRequest(c, MegaContactRequest.REPLY_ACTION_ACCEPT, this);
	}
	
	public void ignoreInvitationContact(MegaContactRequest c)
	{
		log("ignoreInvitationContact");
		megaApi.replyContactRequest(c, MegaContactRequest.REPLY_ACTION_IGNORE, this);
	}
	
	public void declineInvitationContact(MegaContactRequest c)
	{
		log("declineInvitationContact");
		megaApi.replyContactRequest(c, MegaContactRequest.REPLY_ACTION_DENY, this);
	}
	
	public int getAccountType(){
		return accountType;
	}
	
	public long getUsedGbStorage(){
		return usedGbStorage;
	}

	public Purchase getProLiteMonthly() {
		return proLiteMonthly;
	}

	public Purchase getProLiteYearly() {
		return proLiteYearly;
	}

	public Purchase getProIMonthly() {
		return proIMonthly;
	}

	public Purchase getProIYearly() {
		return proIYearly;
	}

	public Purchase getProIIMonthly() {
		return proIIMonthly;
	}

	public Purchase getProIIYearly() {
		return proIIYearly;
	}

	public Purchase getProIIIMonthly() {
		return proIIIMonthly;
	}

	public Purchase getProIIIYearly() {
		return proIIIYearly;
	}

	public boolean isInventoryFinished() {
		return inventoryFinished;
	}

	@Override
	public void onAccountUpdate(MegaApiJava api) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {
		log("---------------------onContactRequestsUpdate");
		// TODO Auto-generated method stub

		
	}
}
