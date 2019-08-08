package mega.privacy.android.app.lollipop.megachat;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.CreateChatToPerformActionListener;
import mega.privacy.android.app.lollipop.listeners.MultipleForwardChatProcessor;
import mega.privacy.android.app.lollipop.listeners.MultipleRequestListener;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.NodeAttachmentHistoryAdapter;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.NodeAttachmentBottomSheetDialogFragment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatListenerInterface;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatNodeHistoryListenerInterface;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatPresenceConfig;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.FileUtils.*;

public class NodeAttachmentHistoryActivity extends PinActivityLollipop implements MegaChatRequestListenerInterface, MegaRequestListenerInterface, RecyclerView.OnItemTouchListener, OnClickListener, MegaChatListenerInterface, MegaChatNodeHistoryListenerInterface {

	public static int NUMBER_MESSAGES_TO_LOAD = 20;
	public static int NUMBER_MESSAGES_BEFORE_LOAD = 8;

	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	ActionBar aB;
	Toolbar tB;
	NodeAttachmentHistoryActivity nodeAttachmentHistoryActivity = this;

    private android.support.v7.app.AlertDialog downloadConfirmationDialog;
	MegaChatMessage selectedMessage;
    DatabaseHandler dbH = null;
    MegaPreferences prefs = null;
    public boolean isList = true;

	int selectedPosition;

	RelativeLayout container;
	LinearLayout linearLayoutList;
	LinearLayout linearLayoutGrid;
	RecyclerView listView;
	LinearLayoutManager mLayoutManager;
//	GestureDetectorCompat detector;
	RelativeLayout emptyLayout;
	TextView emptyTextView;
	ImageView emptyImageView;

	MenuItem importIcon;
    private MenuItem thumbViewMenuItem;

	ArrayList<MegaChatMessage> messages;
	ArrayList<MegaChatMessage> bufferMessages;

	public MegaChatRoom chatRoom;
	
	NodeAttachmentHistoryAdapter adapter;
	boolean scrollingUp = false;
	boolean getMoreHistory=false;
	boolean isLoadingHistory = false;

	private ActionMode actionMode;
	DisplayMetrics outMetrics;
	
	ProgressDialog statusDialog;

	MenuItem selectMenuItem;
	MenuItem unSelectMenuItem;

	Handler handler;
	int stateHistory;
	public long chatId = -1;
	public long selectedMessageId = -1;

	ChatController chatC;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		super.onCreate(savedInstanceState);
		
		if (megaApi == null){
			megaApi = ((MegaApplication) getApplication()).getMegaApi();
			
		}

		if (megaChatApi == null){
			megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
		}

		if(megaChatApi==null||megaChatApi.getInitState()==MegaChatApi.INIT_ERROR||megaChatApi.getInitState()==MegaChatApi.INIT_NOT_DONE){
			log("Refresh session - karere");
			Intent intent = new Intent(this, LoginActivityLollipop.class);
			intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return;
		}

		chatC = new ChatController(this);

		log("addChatListener");
		megaChatApi.addChatListener(this);
		megaChatApi.addNodeHistoryListener(chatId,this);

		handler = new Handler();

        dbH = DatabaseHandler.getDbHandler(this);
		
		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = this.getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
		}

		setContentView(R.layout.activity_node_history);

		if (savedInstanceState != null){
			chatId = savedInstanceState.getLong("chatId", -1);
		}

//        prefs = dbH.getPreferences();
//        if (prefs == null){
//            isList=true;
//        }
//        else{
//            if (prefs.getPreferredViewList() == null){
//                isList = true;
//            }
//            else{
//                isList = Boolean.parseBoolean(prefs.getPreferredViewList());
//            }
//        }

		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar_node_history);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
//			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setDisplayShowHomeEnabled(true);

		aB.setTitle(getString(R.string.title_chat_shared_files_info).toUpperCase());

		container = (RelativeLayout) findViewById(R.id.node_history_main_layout);

//		detector = new GestureDetectorCompat(this, new RecyclerViewOnGestureListener());

		emptyLayout = (RelativeLayout) findViewById(R.id.empty_layout_node_history);
		emptyTextView = (TextView) findViewById(R.id.empty_text_node_history);
		emptyImageView = (ImageView) findViewById(R.id.empty_image_view_node_history);

		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			emptyImageView.setImageResource(R.drawable.contacts_empty_landscape);
		}else{
			emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
		}

		String textToShow = String.format(getString(R.string.context_empty_shared_files));
		try{
			textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
			textToShow = textToShow.replace("[/A]", "</font>");
			textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
			textToShow = textToShow.replace("[/B]", "</font>");
		}
		catch (Exception e){}
		Spanned result = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
			result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
		} else {
			result = Html.fromHtml(textToShow);
		}
		emptyTextView.setText(result);

		linearLayoutList = (LinearLayout) findViewById(R.id.linear_layout_recycler_list);
		linearLayoutGrid = (LinearLayout) findViewById(R.id.linear_layout_recycler_grid);

		if(isList){
			linearLayoutList.setVisibility(View.VISIBLE);
			linearLayoutGrid.setVisibility(View.GONE);

			listView = (RecyclerView) findViewById(R.id.node_history_list_view);
			listView.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
			mLayoutManager = new LinearLayoutManager(this);
			mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
			listView.setLayoutManager(mLayoutManager);
			listView.addOnItemTouchListener(this);
			listView.setItemAnimator(new DefaultItemAnimator());
		}
		else{
			linearLayoutList.setVisibility(View.GONE);
			linearLayoutGrid.setVisibility(View.VISIBLE);

			listView = (NewGridRecyclerView)findViewById(R.id.file_grid_view_browser);
		}

		listView.setPadding(0,Util.scaleHeightPx(8, outMetrics),0,Util.scaleHeightPx(16, outMetrics));
		listView.setClipToPadding(false);
		listView.setHasFixedSize(true);

		listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

				if(stateHistory!=MegaChatApi.SOURCE_NONE){
					if (dy > 0) {
						// Scrolling up
						scrollingUp = true;
					} else {
						// Scrolling down
						scrollingUp = false;
					}

					if(scrollingUp){
						int pos = mLayoutManager.findFirstVisibleItemPosition();

						if(pos<=NUMBER_MESSAGES_BEFORE_LOAD&&getMoreHistory){
							log("DE->loadAttachments:scrolling down");
							isLoadingHistory = true;
							stateHistory = megaChatApi.loadAttachments(chatId, NUMBER_MESSAGES_TO_LOAD);
							getMoreHistory = false;
						}
					}
				}
				checkScroll();
			}
		});


	    Bundle extras = getIntent().getExtras();
		if (extras != null){
			if(chatId==-1){
				chatId = extras.getLong("chatId");
			}

			chatRoom=megaChatApi.getChatRoom(chatId);

			if(chatRoom!=null){
				messages = new ArrayList<>();
				bufferMessages = new ArrayList<MegaChatMessage>();

				if (messages.size() != 0){
					emptyLayout.setVisibility(View.GONE);
					listView.setVisibility(View.VISIBLE);
				}
				else{
					emptyLayout.setVisibility(View.VISIBLE);
					listView.setVisibility(View.GONE);
				}

				boolean resultOpen = megaChatApi.openNodeHistory(chatId, this);
				if(resultOpen){
					log("Node history opened correctly");

					messages = new ArrayList<MegaChatMessage>();

					if(isList){
						if (adapter == null){
							adapter = new NodeAttachmentHistoryAdapter(this, messages, listView, NodeAttachmentHistoryAdapter.ITEM_VIEW_TYPE_LIST);
						}
					}
					else{
						if (adapter == null){
							adapter = new NodeAttachmentHistoryAdapter(this, messages, listView, NodeAttachmentHistoryAdapter.ITEM_VIEW_TYPE_GRID);
						}
					}

					listView.setAdapter(adapter);
					adapter.setMultipleSelect(false);

					adapter.setMessages(messages);

					isLoadingHistory = true;
					log("A->loadAttachments");
					stateHistory = megaChatApi.loadAttachments(chatId, NUMBER_MESSAGES_TO_LOAD);
				}
			}
			else{
				log("ERROR: node is NULL");
			}
		}
	}
	
	public void showOptionsPanel(MegaChatMessage sMessage, int sPosition){
		log("showOptionsPanel");
		if(sMessage!=null){
			this.selectedMessage = sMessage;
			this.selectedPosition = sPosition;
			//VersionBottomSheetDialogFragment bottomSheetDialogFragment = new VersionBottomSheetDialogFragment();
			//bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
		}
	}

	@Override
    protected void onDestroy(){
		log("onDestroy");
    	super.onDestroy();
		if (megaChatApi != null) {
			megaChatApi.removeChatListener(this);
			megaChatApi.removeNodeHistoryListener(chatId, this);
			megaChatApi.closeNodeHistory(chatId, null);
		}
		if (handler != null) {
			handler.removeCallbacksAndMessages(null);
		}
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_node_history, menu);

	    selectMenuItem = menu.findItem(R.id.action_select);
		unSelectMenuItem = menu.findItem(R.id.action_unselect);
        thumbViewMenuItem= menu.findItem(R.id.action_grid);

	    return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

        if(messages.size()>0){
            selectMenuItem.setVisible(true);
            //        if (isList){
//            thumbViewMenuItem.setTitle(getString(R.string.action_grid));
//            thumbViewMenuItem.setIcon(Util.mutateIcon(this, R.drawable.ic_menu_gridview, R.color.black));
//        }
//        else{
//            thumbViewMenuItem.setTitle(getString(R.string.action_list));
//            thumbViewMenuItem.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_menu_list_view));
//        }
        }
        else{
            selectMenuItem.setVisible(false);
        }

		unSelectMenuItem.setVisible(false);
		thumbViewMenuItem.setVisible(false);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
	    switch (item.getItemId()) {
		    case android.R.id.home:{
		    	onBackPressed();
		    	return true;
		    }
		    case R.id.action_select:{
		    	
		    	selectAll();
		    	return true;
		    }
            case R.id.action_grid:{
//                isList = !isList;
//                dbH.setPreferredViewList(isList);
//
//                if (isList){
//                    thumbViewMenuItem.setTitle(getString(R.string.action_grid));
//                }
//                else{
//                    thumbViewMenuItem.setTitle(getString(R.string.action_list));
//                }
//
//                adapter.notifyDataSetChanged();
                return true;
            }
		    default:{
	            return super.onOptionsItemSelected(item);
	        }
	    }
	}

	public void activateActionMode(){
		log("activateActionMode");
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = startSupportActionMode(new NodeAttachmentHistoryActivity.ActionBarCallBack());
		}
	}

	// Clear all selected items
	private void clearSelections() {
		if(adapter.isMultipleSelect()){
			adapter.clearSelections();
		}
	}
	
	public void selectAll(){
		log("selectAll");
		if (adapter != null){
			if(adapter.isMultipleSelect()){
				adapter.selectAll();
			}
			else{						
				adapter.setMultipleSelect(true);
				adapter.selectAll();
				
				actionMode = startSupportActionMode(new ActionBarCallBack());
			}		
			updateActionModeTitle();		
		}
	}
	
	public boolean showSelectMenuItem(){
		if (adapter != null){
			return adapter.isMultipleSelect();
		}
		
		return false;
	}

	public static void log(String log) {
		Util.log("NodeAttachmentHistoryActivity", log);
	}


	public void itemClick(int position) {
		log("itemClick");
		if(megaChatApi.isSignalActivityRequired()){
			megaChatApi.signalPresenceActivity();
		}

		if(position<messages.size()){
			MegaChatMessage m = messages.get(position);

			if (adapter.isMultipleSelect()) {

				adapter.toggleSelection(position);

				List<MegaChatMessage> messages = adapter.getSelectedMessages();
				if (messages.size() > 0) {
					updateActionModeTitle();
				}

			}else{

				if(m!=null){
					MegaNodeList nodeList = m.getMegaNodeList();
					if(nodeList.size()==1){
						MegaNode node = nodeList.get(0);

						if (MimeTypeList.typeForName(node.getName()).isImage()){
							if(node.hasPreview()){
								log("Show full screen viewer");
								showFullScreenViewer(m.getMsgId());
							}
							else{
								log("Image without preview - show node attachment panel for one node");
								showNodeAttachmentBottomSheet(m, position);
							}
						}
						else if (MimeTypeList.typeForName(node.getName()).isVideoReproducible() || MimeTypeList.typeForName(node.getName()).isAudio() ){
							log("itemClick:isFile:isVideoReproducibleOrIsAudio");
							String mimeType = MimeTypeList.typeForName(node.getName()).getType();
							log("itemClick:FILENAME: " + node.getName() + " TYPE: "+mimeType);

							Intent mediaIntent;
							boolean internalIntent;
							boolean opusFile = false;
							if (MimeTypeList.typeForName(node.getName()).isVideoNotSupported() || MimeTypeList.typeForName(node.getName()).isAudioNotSupported()){
								mediaIntent = new Intent(Intent.ACTION_VIEW);
								internalIntent=false;
								String[] s = node.getName().split("\\.");
								if (s != null && s.length > 1 && s[s.length-1].equals("opus")) {
									opusFile = true;
								}
							}
							else {
								log("itemClick:setIntentToAudioVideoPlayer");
								mediaIntent = new Intent(this, AudioVideoPlayerLollipop.class);
								internalIntent=true;
							}

							mediaIntent.putExtra("adapterType", Constants.FROM_CHAT);
							mediaIntent.putExtra("isPlayList", false);
							mediaIntent.putExtra("msgId", m.getMsgId());
							mediaIntent.putExtra("chatId", chatId);

							mediaIntent.putExtra("FILENAME", node.getName());
							MegaPreferences prefs = dbH.getPreferences();
							String downloadLocationDefaultPath = getDownloadLocation(this);
							String localPath = getLocalFile(this, node.getName(), node.getSize(), downloadLocationDefaultPath);
							File f = new File(downloadLocationDefaultPath, node.getName());
							boolean isOnMegaDownloads = false;
							if(f.exists() && (f.length() == node.getSize())){
								isOnMegaDownloads = true;
							}
							log("isOnMegaDownloads: "+isOnMegaDownloads);
							if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(node) != null && megaApi.getFingerprint(node).equals(megaApi.getFingerprint(localPath))))){
								File mediaFile = new File(localPath);
								//mediaIntent.setDataAndType(Uri.parse(localPath), mimeType);
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
									log("itemClick:FileProviderOption");
									Uri mediaFileUri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", mediaFile);
									if(mediaFileUri==null){
										log("itemClick:ERROR:NULLmediaFileUri");
										showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.general_text_error));
									}
									else{
										mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
									}
								}
								else{
									Uri mediaFileUri = Uri.fromFile(mediaFile);
									if(mediaFileUri==null){
										log("itemClick:ERROR:NULLmediaFileUri");
										showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.general_text_error));
									}
									else{
										mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
									}
								}
								mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
							}
							else {
								log("itemClick:localPathNULL");
								if (Util.isOnline(this)){
									if (megaApi.httpServerIsRunning() == 0) {
										megaApi.httpServerStart();
									}
									else{
										log("itemClick:ERROR:httpServerAlreadyRunning");
									}

									ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
									ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
									activityManager.getMemoryInfo(mi);

									if(mi.totalMem>Constants.BUFFER_COMP){
										log("itemClick:total mem: "+mi.totalMem+" allocate 32 MB");
										megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB);
									}
									else{
										log("itemClick:total mem: "+mi.totalMem+" allocate 16 MB");
										megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB);
									}

									String url = megaApi.httpServerGetLocalLink(node);
									if(url!=null){
										Uri parsedUri = Uri.parse(url);
										if(parsedUri!=null){
											mediaIntent.setDataAndType(parsedUri, mimeType);
										}
										else{
											log("itemClick:ERROR:httpServerGetLocalLink");
											showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.general_text_error));
										}
									}
									else{
										log("itemClick:ERROR:httpServerGetLocalLink");
										showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.general_text_error));
									}
								}
								else {
									showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.error_server_connection_problem)+". "+ getString(R.string.no_network_connection_on_play_file));
								}
							}
							mediaIntent.putExtra("HANDLE", node.getHandle());
							if (opusFile){
								mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
							}
							if(internalIntent){
								startActivity(mediaIntent);
							}
							else{
								log("itemClick:externalIntent");
								if (MegaApiUtils.isIntentAvailable(this, mediaIntent)){
									startActivity(mediaIntent);
								}
								else{
									log("itemClick:noAvailableIntent");
									showNodeAttachmentBottomSheet(m, position);
								}
							}
						}
						else if (MimeTypeList.typeForName(node.getName()).isPdf()){
							log("itemClick:isFile:isPdf");
							String mimeType = MimeTypeList.typeForName(node.getName()).getType();
							log("itemClick:FILENAME: " + node.getName() + " TYPE: "+mimeType);
							Intent pdfIntent = new Intent(this, PdfViewerActivityLollipop.class);
							pdfIntent.putExtra("inside", true);
							pdfIntent.putExtra("adapterType", Constants.FROM_CHAT);
							pdfIntent.putExtra("msgId", m.getMsgId());
							pdfIntent.putExtra("chatId", chatId);

							pdfIntent.putExtra("FILENAME", node.getName());
							MegaPreferences prefs = dbH.getPreferences();
							String downloadLocationDefaultPath = getDownloadLocation(this);

							String localPath = getLocalFile(this, node.getName(), node.getSize(), downloadLocationDefaultPath);
							File f = new File(downloadLocationDefaultPath, node.getName());
							boolean isOnMegaDownloads = false;
							if(f.exists() && (f.length() == node.getSize())){
								isOnMegaDownloads = true;
							}
							log("isOnMegaDownloads: "+isOnMegaDownloads);
							if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(node) != null && megaApi.getFingerprint(node).equals(megaApi.getFingerprint(localPath))))){
								File mediaFile = new File(localPath);
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
									log("itemClick:FileProviderOption");
									Uri mediaFileUri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", mediaFile);
									if(mediaFileUri==null){
										log("itemClick:ERROR:NULLmediaFileUri");
										showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.general_text_error));
									}
									else{
										pdfIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
									}
								}
								else{
									Uri mediaFileUri = Uri.fromFile(mediaFile);
									if(mediaFileUri==null){
										log("itemClick:ERROR:NULLmediaFileUri");
										showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.general_text_error));
									}
									else{
										pdfIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
									}
								}
								pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
							}
							else {
								log("itemClick:localPathNULL");
								if (Util.isOnline(this)){
									if (megaApi.httpServerIsRunning() == 0) {
										megaApi.httpServerStart();
									}
									else{
										log("itemClick:ERROR:httpServerAlreadyRunning");
									}
									ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
									ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
									activityManager.getMemoryInfo(mi);
									if(mi.totalMem>Constants.BUFFER_COMP){
										log("itemClick:total mem: "+mi.totalMem+" allocate 32 MB");
										megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB);
									}
									else{
										log("itemClick:total mem: "+mi.totalMem+" allocate 16 MB");
										megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB);
									}
									String url = megaApi.httpServerGetLocalLink(node);
									if(url!=null){
										Uri parsedUri = Uri.parse(url);
										if(parsedUri!=null){
											pdfIntent.setDataAndType(parsedUri, mimeType);
										}
										else{
											log("itemClick:ERROR:httpServerGetLocalLink");
											showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.general_text_error));
										}
									}
									else{
										log("itemClick:ERROR:httpServerGetLocalLink");
										showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.general_text_error));
									}
								}
								else {
									showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.error_server_connection_problem)+". "+ getString(R.string.no_network_connection_on_play_file));
								}
							}
							pdfIntent.putExtra("HANDLE", node.getHandle());

							if (MegaApiUtils.isIntentAvailable(this, pdfIntent)){
								startActivity(pdfIntent);
							}
							else{
								log("itemClick:noAvailableIntent");
								showNodeAttachmentBottomSheet(m, position);
							}
							overridePendingTransition(0,0);
						}
						else{
							log("NOT Image, pdf, audio or video - show node attachment panel for one node");
							showNodeAttachmentBottomSheet(m, position);
						}
					}
					else{
						log("show node attachment panel");
						showNodeAttachmentBottomSheet(m, position);
					}
				}
			}
		}else{
			log("DO NOTHING: Position ("+position+") is more than size in messages (size: "+messages.size()+")");
		}
	}

	public void showFullScreenViewer(long msgId){
		log("showFullScreenViewer");
		int position = 0;
		boolean positionFound = false;
		List<Long> ids = new ArrayList<>();
		for(int i=0; i<messages.size();i++){
			MegaChatMessage msg = messages.get(i);
			ids.add(msg.getMsgId());

			if(msg.getMsgId()==msgId){
				positionFound=true;
			}
			if(!positionFound){
				MegaNodeList nodeList = msg.getMegaNodeList();
				if(nodeList.size()==1){
					MegaNode node = nodeList.get(0);
					if(MimeTypeList.typeForName(node.getName()).isImage()){
						position++;
					}
				}
			}
		}

		Intent intent = new Intent(this, ChatFullScreenImageViewer.class);
		intent.putExtra("position", position);
		intent.putExtra("chatId", chatId);

		long[] array = new long[ids.size()];
		for(int i = 0; i < ids.size(); i++) {
			array[i] = ids.get(i);
		}
		intent.putExtra("messageIds", array);
		startActivity(intent);
	}
	
	private void updateActionModeTitle() {
		log("updateActionModeTitle");
		if (actionMode == null) {
			return;
		}

		int num = adapter.getSelectedItemCount();
		try {
			actionMode.setTitle(num+"");
			actionMode.invalidate();
		} catch (Exception e) {
			e.printStackTrace();
			log("oninvalidate error");
		}
	}
	
	/*
	 * Disable selection
	 */
	public void hideMultipleSelect() {
		adapter.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()){		
			case R.id.file_contact_list_layout:{
				Intent i = new Intent(this, ManagerActivityLollipop.class);
				i.setAction(Constants.ACTION_REFRESH_PARENTHANDLE_BROWSER);
				//i.putExtra("parentHandle", node.getHandle());
				startActivity(i);
				finish();
				break;
			}
		}
	}

	public void notifyDataSetChanged(){		
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}		
	}

	@Override
	public boolean onInterceptTouchEvent(RecyclerView rV, MotionEvent e) {
//		detector.onTouchEvent(e);
		return false;
	}

	@Override
	public void onRequestDisallowInterceptTouchEvent(boolean arg0) {
	}

	@Override
	public void onTouchEvent(RecyclerView arg0, MotionEvent arg1) {
	}

	public MegaChatMessage getSelectedMessage() {
		return selectedMessage;
	}

	public int getSelectedPosition() {
		return selectedPosition;
	}

	public void setSelectedPosition(int selectedPosition) {
		this.selectedPosition = selectedPosition;
	}

	public void showConfirmationRemoveVersion(){
		log("showConfirmationRemoveContact");
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:

						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getQuantityString(R.plurals.title_dialog_delete_version, 1));
		builder.setMessage(getString(R.string.content_dialog_delete_version)).setPositiveButton(R.string.context_delete, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();

	}

	public void showConfirmationRemoveVersions(final List<MegaNode> removeNodes){
		log("showConfirmationRemoveContactRequests");
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:

						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		String message="";
		String title="";
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if(removeNodes.size()==1){

			title = getResources().getQuantityString(R.plurals.title_dialog_delete_version, 1);

			message= getResources().getString(R.string.content_dialog_delete_version);
		}else{
			title = getResources().getQuantityString(R.plurals.title_dialog_delete_version, removeNodes.size());
			message= getResources().getString(R.string.content_dialog_delete_multiple_version,removeNodes.size());
		}
		builder.setTitle(title);
		builder.setMessage(message).setPositiveButton(R.string.context_delete, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		log("onSaveInstanceState");
		super.onSaveInstanceState(outState);
		if(chatRoom!=null){
			outState.putLong("chatId", chatRoom.getChatId());
		}
	}

	//Multiselection

	public class RecyclerViewOnGestureListener extends SimpleOnGestureListener{

		public void onLongPress(MotionEvent e) {
			log("onLongPress -- RecyclerViewOnGestureListener");

			View view = listView.findChildViewUnder(e.getX(), e.getY());
			int position = listView.getChildLayoutPosition(view);

			if (!adapter.isMultipleSelect()){

				if(position<0){
					log("Position not valid: "+position);
				}
				else{
					adapter.setMultipleSelect(true);

					actionMode = startSupportActionMode(new ActionBarCallBack());

					itemClick(position);
				}
			}

			super.onLongPress(e);
		}
	}

	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			log("onActionItemClicked");
			final ArrayList<MegaChatMessage> messagesSelected = adapter.getSelectedMessages();

			switch (item.getItemId()) {
				case R.id.cab_menu_select_all: {
					selectAll();
					actionMode.invalidate();
					break;
				}
				case R.id.cab_menu_unselect_all: {
					clearSelections();
					actionMode.invalidate();
					break;
				}
				case R.id.chat_cab_menu_forward: {
					log("Forward message");
					clearSelections();
					hideMultipleSelect();
					forwardMessages(messagesSelected);
					break;
				}
				case R.id.chat_cab_menu_delete:{
					clearSelections();
					hideMultipleSelect();
					//Delete
					showConfirmationDeleteMessages(messagesSelected, chatRoom);
					break;
				}
				case R.id.chat_cab_menu_download:{
					clearSelections();
					hideMultipleSelect();

					ArrayList<MegaNodeList> list = new ArrayList<>();
					for(int i = 0; i<messagesSelected.size();i++){

						MegaNodeList megaNodeList = messagesSelected.get(i).getMegaNodeList();
						list.add(megaNodeList);
					}
					chatC.prepareForChatDownload(list);
					break;
				}
				case R.id.chat_cab_menu_import:{
					clearSelections();
					hideMultipleSelect();
					chatC.importNodesFromMessages(messagesSelected);
					break;
				}
				case R.id.chat_cab_menu_offline:{
					clearSelections();
					hideMultipleSelect();
					chatC.saveForOfflineWithMessages(messagesSelected, megaChatApi.getChatRoom(chatId));
					break;
				}
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			log("onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.messages_node_history_action, menu);

			importIcon = menu.findItem(R.id.chat_cab_menu_import);
			menu.findItem(R.id.chat_cab_menu_offline).setIcon(Util.mutateIconSecondary(nodeAttachmentHistoryActivity, R.drawable.ic_b_save_offline, R.color.white));
			changeActionBarElevation(true);
			Util.changeStatusBarColorActionMode(getApplicationContext(), getWindow(), handler, 1);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			log("onDestroyActionMode");
			adapter.clearSelections();
			adapter.setMultipleSelect(false);
			checkScroll();
			Util.changeStatusBarColorActionMode(getApplicationContext(), getWindow(), handler, 0);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			log("onPrepareActionMode");
			List<MegaChatMessage> selected = adapter.getSelectedMessages();
			if (selected.size() != 0) {
//                MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);

				MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
				if (selected.size() == adapter.getItemCount()) {
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				} else {
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}

				if (chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_RM || chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_RO && !chatRoom.isPreview()) {

					menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
					menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
					menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
					menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);

				}
				else {

					log("Chat with permissions");
					if (Util.isOnline(nodeAttachmentHistoryActivity) && !chatC.isInAnonymousMode()) {
						menu.findItem(R.id.chat_cab_menu_forward).setVisible(true);
					} else {
						menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
					}

					if (selected.size() == 1) {
						if (selected.get(0).getUserHandle() == megaChatApi.getMyUserHandle() && selected.get(0).isDeletable()) {
							log("one message Message DELETABLE");
							menu.findItem(R.id.chat_cab_menu_delete).setVisible(true);
						} else {
							menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
						}

						if (Util.isOnline(nodeAttachmentHistoryActivity)) {
							menu.findItem(R.id.chat_cab_menu_download).setVisible(true);
							if (chatC.isInAnonymousMode()) {
								menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
								importIcon.setVisible(false);
							}
							else {
								menu.findItem(R.id.chat_cab_menu_offline).setVisible(true);
								importIcon.setVisible(true);
							}
						} else {
							menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
							menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
							importIcon.setVisible(false);
						}

					} else {
						log("Many items selected");
						boolean showDelete = true;
						boolean allNodeAttachments = true;

						for (int i = 0; i < selected.size(); i++) {

							if (showDelete) {
								if (selected.get(i).getUserHandle() == megaChatApi.getMyUserHandle()) {
									if (!(selected.get(i).isDeletable())) {
										showDelete = false;
									}

								} else {
									showDelete = false;
								}
							}

							if (allNodeAttachments) {
								if (selected.get(i).getType() != MegaChatMessage.TYPE_NODE_ATTACHMENT) {
									allNodeAttachments = false;
								}
							}
						}

						if (Util.isOnline(nodeAttachmentHistoryActivity)) {
							menu.findItem(R.id.chat_cab_menu_download).setVisible(true);
							if (chatC.isInAnonymousMode()) {
								menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
								importIcon.setVisible(false);
							}
							else {
								menu.findItem(R.id.chat_cab_menu_offline).setVisible(true);
								importIcon.setVisible(true);
							}
						} else {
							menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
							menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
							importIcon.setVisible(false);
						}

						menu.findItem(R.id.chat_cab_menu_delete).setVisible(showDelete);
						if (Util.isOnline(nodeAttachmentHistoryActivity) && !chatC.isInAnonymousMode()) {
							menu.findItem(R.id.chat_cab_menu_forward).setVisible(true);
						} else {
							menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
						}
					}
				}
			} else {
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
				menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
				menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
				menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
				menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
			}
			return false;
		}
	}

	public void showConfirmationDeleteMessages(final ArrayList<MegaChatMessage> messages, final MegaChatRoom chat){
		log("showConfirmationDeleteMessages");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						ChatController cC = new ChatController(nodeAttachmentHistoryActivity);
						cC.deleteMessages(messages, chat);
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

		if(messages.size()==1){
			builder.setMessage(R.string.confirmation_delete_one_message);
		}
		else{
			builder.setMessage(R.string.confirmation_delete_several_messages);
		}
		builder.setPositiveButton(R.string.context_remove, dialogClickListener)
				.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
	}

	public void forwardMessages(ArrayList<MegaChatMessage> messagesSelected){
		log("forwardMessages");
		chatC.prepareMessagesToForward(messagesSelected, chatId);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		log("onActivityResult, resultCode: " + resultCode);
		if (requestCode == Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK) {
			if(!Util.isOnline(this) || megaApi==null) {
				try{
					statusDialog.dismiss();
				} catch(Exception ex) {};

				showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
				return;
			}

			final long toHandle = intent.getLongExtra("IMPORT_TO", 0);

			final long[] importMessagesHandles = intent.getLongArrayExtra("HANDLES_IMPORT_CHAT");

			importNodes(toHandle, importMessagesHandles);
		}
		else if (requestCode == Constants.REQUEST_CODE_SELECT_CHAT && resultCode == RESULT_OK) {
			if(!Util.isOnline(this)) {
				try{
					statusDialog.dismiss();
				} catch(Exception ex) {};

				showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
				return;
			}

			showProgressForwarding();

			long[] idMessages = intent.getLongArrayExtra("ID_MESSAGES");
			log("Send "+idMessages.length+" messages");

			long[] chatHandles = intent.getLongArrayExtra("SELECTED_CHATS");
			log("Send to "+chatHandles.length+" chats");

			long[] contactHandles = intent.getLongArrayExtra("SELECTED_USERS");

			if (chatHandles != null && chatHandles.length > 0 && idMessages != null) {
				if (contactHandles != null && contactHandles.length > 0) {
					ArrayList<MegaUser> users = new ArrayList<>();
					ArrayList<MegaChatRoom> chats =  new ArrayList<>();

					for (int i=0; i<contactHandles.length; i++) {
						MegaUser user = megaApi.getContact(MegaApiAndroid.userHandleToBase64(contactHandles[i]));
						if (user != null) {
							users.add(user);
						}
					}

					for (int i=0; i<chatHandles.length; i++) {
						MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatHandles[i]);
						if (chatRoom != null) {
							chats.add(chatRoom);
						}
					}

					CreateChatToPerformActionListener listener = new CreateChatToPerformActionListener(chats, users, idMessages, this, CreateChatToPerformActionListener.SEND_MESSAGES, chatId);

					for (MegaUser user : users) {
						MegaChatPeerList peers = MegaChatPeerList.createInstance();
						peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
						megaChatApi.createChat(false, peers, listener);
					}
				}
				else {
					int countChat = chatHandles.length;
					log("Selected: " + countChat + " chats to send");

					MultipleForwardChatProcessor forwardChatProcessor = new MultipleForwardChatProcessor(this, chatHandles, idMessages, chatId);
					forwardChatProcessor.forward(chatRoom);
				}
			}
			else {
				log("Error on sending to chat");
			}
		}
		if (requestCode == Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
			log("local folder selected");
			String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
			if (hashes != null) {
				ArrayList<MegaNode> megaNodes = new ArrayList<>();
				for (int i=0; i<hashes.length; i++) {
					MegaNode node = megaApi.getNodeByHandle(hashes[i]);
					if (node != null) {
						megaNodes.add(node);
					}
					else {
						log("Node NULL, not added");
					}
				}
				if (megaNodes.size() > 0) {
					chatC.checkSizeBeforeDownload(parentPath, megaNodes);
				}
			}
		}
	}

	public void showProgressForwarding(){
		log("showProgressForwarding");

		statusDialog = new ProgressDialog(this);
		statusDialog.setMessage(getString(R.string.general_forwarding));
		statusDialog.show();
	}

	public void removeProgressDialog(){
		try{
			statusDialog.dismiss();
		} catch(Exception ex) {};
	}

	public void importNodes(final long toHandle, final long[] importMessagesHandles){
		log("importNode: "+toHandle+ " -> "+ importMessagesHandles.length);
		statusDialog = new ProgressDialog(this);
		statusDialog.setMessage(getString(R.string.general_importing));
		statusDialog.show();

		MegaNode target = null;
		target = megaApi.getNodeByHandle(toHandle);
		if(target == null){
			target = megaApi.getRootNode();
		}
		log("TARGET: " + target.getName() + "and handle: " + target.getHandle());

		if(importMessagesHandles.length==1){
			for (int k = 0; k < importMessagesHandles.length; k++){
				MegaChatMessage message = megaChatApi.getMessage(chatId, importMessagesHandles[k]);
				if(message!=null){

					MegaNodeList nodeList = message.getMegaNodeList();

					for(int i=0;i<nodeList.size();i++){
						MegaNode document = nodeList.get(i);
						if (document != null) {
							log("DOCUMENT: " + document.getName() + "_" + document.getHandle());
							document = chatC.authorizeNodeIfPreview(document, chatRoom);
							if (target != null) {
//                            MegaNode autNode = megaApi.authorizeNode(document);

								megaApi.copyNode(document, target, this);
							} else {
								log("TARGET: null");
								showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.import_success_error));
							}
						}
						else{
							log("DOCUMENT: null");
							showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.import_success_error));
						}
					}

				}
				else{
					log("MESSAGE is null");
					showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.import_success_error));
				}
			}
		}
		else {
			MultipleRequestListener listener = new MultipleRequestListener(Constants.MULTIPLE_CHAT_IMPORT, this);

			for (int k = 0; k < importMessagesHandles.length; k++){
				MegaChatMessage message = megaChatApi.getMessage(chatId, importMessagesHandles[k]);
				if(message!=null){

					MegaNodeList nodeList = message.getMegaNodeList();

					for(int i=0;i<nodeList.size();i++){
						MegaNode document = nodeList.get(i);
						if (document != null) {
							log("DOCUMENT: " + document.getName() + "_" + document.getHandle());
							if (target != null) {
//                            MegaNode autNode = megaApi.authorizeNode(document);

								megaApi.copyNode(document, target, listener);
							} else {
								log("TARGET: null");
							}
						}
						else{
							log("DOCUMENT: null");
						}
					}
				}
				else{
					log("MESSAGE is null");
					showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.import_success_error));
				}
			}
		}
	}

	@Override
	public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {

	}

	@Override
	public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {

	}

	@Override
	public void onChatOnlineStatusUpdate(MegaChatApiJava api, long userhandle, int status, boolean inProgress) {

	}

	@Override
	public void onChatPresenceConfigUpdate(MegaChatApiJava api, MegaChatPresenceConfig config) {

	}

	@Override
	public void onChatConnectionStateUpdate(MegaChatApiJava api, long chatid, int newState) {

	}

	@Override
	public void onChatPresenceLastGreen(MegaChatApiJava api, long userhandle, int lastGreen) {

	}

	@Override
	public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

	}


	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {

	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		log("onRequestFinish");
		removeProgressDialog();

		if(request.getType() == MegaRequest.TYPE_COPY){
			if (e.getErrorCode() != MegaError.API_OK) {

				log("e.getErrorCode() != MegaError.API_OK");

				if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
					log("OVERQUOTA ERROR: "+e.getErrorCode());
					Intent intent = new Intent(this, ManagerActivityLollipop.class);
					intent.setAction(Constants.ACTION_OVERQUOTA_STORAGE);
					startActivity(intent);
					finish();
				}
				else if(e.getErrorCode()==MegaError.API_EGOINGOVERQUOTA){
					log("OVERQUOTA ERROR: "+e.getErrorCode());
					Intent intent = new Intent(this, ManagerActivityLollipop.class);
					intent.setAction(Constants.ACTION_PRE_OVERQUOTA_STORAGE);
					startActivity(intent);
					finish();
				}
				else
				{
					showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.import_success_error));
				}

			}else{
				showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.import_success_message));
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

	}

	@Override
	public void onAttachmentLoaded(MegaChatApiJava api, MegaChatMessage msg) {
		log("onAttachmentLoaded");
		if(msg!=null){
			if(msg.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){

				MegaNodeList nodeList = msg.getMegaNodeList();
				if (nodeList != null) {

					if (nodeList.size() == 1) {
						MegaNode node = nodeList.get(0);
						log("---Node Name: " + node.getName());
						bufferMessages.add(msg);
						log("onMessageLoaded: Size of buffer: "+bufferMessages.size());
						log("onMessageLoaded: Size of messages: "+messages.size());
					}
				}
			}
		}
		else{
			log("onAttachmentLoaded: msg NULL: end of history");
			if((bufferMessages.size()+messages.size())>=NUMBER_MESSAGES_TO_LOAD){
				fullHistoryReceivedOnLoad();
				isLoadingHistory = false;
			}
			else{
				log("onAttachmentLoaded:lessNumberReceived");
				if((stateHistory!=MegaChatApi.SOURCE_NONE)&&(stateHistory!=MegaChatApi.SOURCE_ERROR)){
					log("But more history exists --> loadAttachments");
					log("G->loadAttachments");
					isLoadingHistory = true;
					stateHistory = megaChatApi.loadAttachments(chatId, NUMBER_MESSAGES_TO_LOAD);
					log("New state of history: "+stateHistory);
					getMoreHistory = false;
					if(stateHistory==MegaChatApi.SOURCE_NONE || stateHistory==MegaChatApi.SOURCE_ERROR){
						fullHistoryReceivedOnLoad();
						isLoadingHistory = false;
					}
				}
				else{
					log("New state of history: "+stateHistory);
					fullHistoryReceivedOnLoad();
					isLoadingHistory = false;
				}
			}
		}
	}

	public void fullHistoryReceivedOnLoad() {
		log("fullHistoryReceivedOnLoad: "+messages.size());

		if(bufferMessages.size()!=0) {
			log("fullHistoryReceivedOnLoad:buffer size: " + bufferMessages.size());
			emptyLayout.setVisibility(View.GONE);
			listView.setVisibility(View.VISIBLE);

			ListIterator<MegaChatMessage> itr = bufferMessages.listIterator();
			while (itr.hasNext()) {
				int currentIndex = itr.nextIndex();
				MegaChatMessage messageToShow = itr.next();
				messages.add(messageToShow);
			}

			if(messages.size()!=0){
				if(adapter==null){
					adapter = new NodeAttachmentHistoryAdapter(this, messages, listView, NodeAttachmentHistoryAdapter.ITEM_VIEW_TYPE_LIST);
					listView.setLayoutManager(mLayoutManager);
					listView.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
					listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
						@Override
						public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
							super.onScrolled(recyclerView, dx, dy);
							checkScroll();
						}
					});
					listView.setAdapter(adapter);
					adapter.setMessages(messages);
				}
				else{
					adapter.loadPreviousMessages(messages, bufferMessages.size());
				}

			}
			bufferMessages.clear();
		}

		log("fullHistoryReceivedOnLoad:getMoreHistoryTRUE");
		getMoreHistory = true;

        invalidateOptionsMenu();
	}

	@Override
	public void onAttachmentReceived(MegaChatApiJava api, MegaChatMessage msg) {
		log("onAttachmentReceived");

		log("STATUS: "+msg.getStatus());
		log("TEMP ID: "+msg.getTempId());
		log("FINAL ID: "+msg.getMsgId());
		log("TIMESTAMP: "+msg.getTimestamp());
		log("TYPE: "+msg.getType());

		int lastIndex = 0;
		if(messages.size()==0){
			messages.add(msg);
		}
		else{
			log("status of message: "+msg.getStatus());

			while(messages.get(lastIndex).getMsgIndex()>msg.getMsgIndex()){
				lastIndex++;
			}

			log("Append in position: "+lastIndex);
			messages.add(lastIndex, msg);
		}

		//Create adapter
		if(adapter==null){
			log("Create adapter");
			adapter = new NodeAttachmentHistoryAdapter(this, messages, listView, NodeAttachmentHistoryAdapter.ITEM_VIEW_TYPE_LIST);
			listView.setLayoutManager(mLayoutManager);
			listView.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
			listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});
			listView.setAdapter(adapter);
			adapter.setMessages(messages);
		}else{
			log("Update adapter with last index: "+lastIndex);
			if(lastIndex<0){
				log("Arrives the first message of the chat");
				adapter.setMessages(messages);
			}
			else{
				adapter.addMessage(messages, lastIndex+1);
				adapter.notifyItemChanged(lastIndex);
			}
		}

		emptyLayout.setVisibility(View.GONE);
		listView.setVisibility(View.VISIBLE);

        invalidateOptionsMenu();
	}

	@Override
	public void onAttachmentDeleted(MegaChatApiJava api, long msgid) {
		log("onAttachmentDeleted");

		int indexToChange = -1;

		ListIterator<MegaChatMessage> itr = messages.listIterator();
		while (itr.hasNext()) {
			MegaChatMessage messageToCheck = itr.next();
			if (messageToCheck.getTempId() == msgid) {
				indexToChange = itr.previousIndex();
				break;
			}
			if (messageToCheck.getMsgId() == msgid) {
				indexToChange = itr.previousIndex();
				break;
			}
		}

		if(indexToChange!=-1) {
			messages.remove(indexToChange);
			log("Removed index: " + indexToChange + " messages size: " + messages.size());

			adapter.removeMessage(indexToChange, messages);

			if(messages.isEmpty()){
				emptyLayout.setVisibility(View.VISIBLE);
				listView.setVisibility(View.GONE);
			}
		}
		else{
			log("Index to remove not found");
		}

        invalidateOptionsMenu();
	}

	@Override
	public void onTruncate(MegaChatApiJava api, long msgid) {
		log("onTruncate");
		invalidateOptionsMenu();
		messages.clear();
		adapter.notifyDataSetChanged();
		listView.setVisibility(View.GONE);
		emptyLayout.setVisibility(View.VISIBLE);
	}

	public void showNodeAttachmentBottomSheet(MegaChatMessage message, int position){
		log("showNodeAttachmentBottomSheet: "+position);
		//this.selectedPosition = position;
		if(message!=null){
			this.selectedMessageId = message.getMsgId();

			NodeAttachmentBottomSheetDialogFragment bottomSheetDialogFragment = new NodeAttachmentBottomSheetDialogFragment();
			bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
		}
	}

	public void showSnackbar(int type, String s){
		showSnackbar(type, container, s);
	}
    
    public void showSnackbar(int type, String s,int chatId){
        showSnackbar(type, container, s, chatId);
    }

	public void askSizeConfirmationBeforeChatDownload(String parentPath, ArrayList<MegaNode> nodeList, long size){
		log("askSizeConfirmationBeforeChatDownload");

		final String parentPathC = parentPath;
		final ArrayList<MegaNode> nodeListC = nodeList;
		final long sizeC = size;

		android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		LinearLayout confirmationLayout = new LinearLayout(this);
		confirmationLayout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(mega.privacy.android.app.utils.Util.scaleWidthPx(20, outMetrics), mega.privacy.android.app.utils.Util.scaleHeightPx(10, outMetrics), mega.privacy.android.app.utils.Util.scaleWidthPx(17, outMetrics), 0);

		final CheckBox dontShowAgain =new CheckBox(this);
		dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
		dontShowAgain.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

		confirmationLayout.addView(dontShowAgain, params);

		builder.setView(confirmationLayout);

		builder.setMessage(getString(R.string.alert_larger_file, mega.privacy.android.app.utils.Util.getSizeString(sizeC)));
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

	public void checkScroll () {
		if (listView != null) {
			if (listView.canScrollVertically(-1) || (adapter != null && adapter.isMultipleSelect())) {
				changeActionBarElevation(true);
			}
			else {
				changeActionBarElevation(false);
			}
		}
	}

	public void changeActionBarElevation(boolean whitElevation){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			if (whitElevation) {
				aB.setElevation(Util.px2dp(4, outMetrics));
			}
			else {
				aB.setElevation(0);
			}
		}
	}

	public MegaChatRoom getChatRoom () {
		return chatRoom;
	}
}

