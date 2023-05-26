package mega.privacy.android.app.main.megachat;

import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_ERROR_COPYING_NODES;
import static mega.privacy.android.app.constants.BroadcastConstants.ERROR_MESSAGE_TEXT;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown;
import static mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.ChatUtil.manageTextFileIntent;
import static mega.privacy.android.app.utils.ColorUtils.getColorHexString;
import static mega.privacy.android.app.utils.Constants.ACTION_REFRESH_PARENTHANDLE_BROWSER;
import static mega.privacy.android.app.utils.Constants.BUFFER_COMP;
import static mega.privacy.android.app.utils.Constants.FORWARD_ONLY_OPTION;
import static mega.privacy.android.app.utils.Constants.FROM_CHAT;
import static mega.privacy.android.app.utils.Constants.ID_MESSAGES;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER;
import static mega.privacy.android.app.utils.Constants.MAX_BUFFER_16MB;
import static mega.privacy.android.app.utils.Constants.MAX_BUFFER_32MB;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_CHAT;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER;
import static mega.privacy.android.app.utils.Constants.SELECTED_CHATS;
import static mega.privacy.android.app.utils.Constants.SELECTED_USERS;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.FileUtil.getLocalFile;
import static mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable;
import static mega.privacy.android.app.utils.Util.changeToolBarElevation;
import static mega.privacy.android.app.utils.Util.getMediaIntent;
import static mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.primitives.Longs;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.saver.NodeSaver;
import mega.privacy.android.app.imageviewer.ImageViewerActivity;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.interfaces.StoreDataBeforeForward;
import mega.privacy.android.app.listeners.CreateChatListener;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.listeners.MultipleForwardChatProcessor;
import mega.privacy.android.app.main.megachat.chatAdapters.NodeAttachmentHistoryAdapter;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.NodeAttachmentBottomSheetDialogFragment;
import mega.privacy.android.app.namecollision.data.NameCollision;
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase;
import mega.privacy.android.app.presentation.chat.NodeAttachmentHistoryViewModel;
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper;
import mega.privacy.android.app.usecase.LegacyCopyNodeUseCase;
import mega.privacy.android.app.utils.AlertsAndWarnings;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.MegaProgressDialogUtil;
import mega.privacy.android.app.utils.permission.PermissionUtils;
import mega.privacy.android.domain.entity.StorageState;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatNodeHistoryListenerInterface;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;

@AndroidEntryPoint
public class NodeAttachmentHistoryActivity extends PasscodeActivity implements
        MegaChatRequestListenerInterface, OnClickListener, MegaChatNodeHistoryListenerInterface,
        StoreDataBeforeForward<ArrayList<MegaChatMessage>>, SnackbarShower {

    @Inject
    CheckNameCollisionUseCase checkNameCollisionUseCase;
    @Inject
    LegacyCopyNodeUseCase legacyCopyNodeUseCase;
    @Inject
    CopyRequestMessageMapper copyRequestMessageMapper;

    private NodeAttachmentHistoryViewModel viewModel;

    public static int NUMBER_MESSAGES_TO_LOAD = 20;
    public static int NUMBER_MESSAGES_BEFORE_LOAD = 8;

    ActionBar aB;
    MaterialToolbar tB;
    NodeAttachmentHistoryActivity nodeAttachmentHistoryActivity = this;

    public boolean isList = true;

    private final NodeSaver nodeSaver = new NodeSaver(this, this, this,
            AlertsAndWarnings.showSaveToDeviceConfirmDialog(this));

    RelativeLayout container;
    LinearLayout linearLayoutList;
    LinearLayout linearLayoutGrid;
    RecyclerView listView;
    LinearLayoutManager mLayoutManager;
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
    boolean getMoreHistory = false;
    boolean isLoadingHistory = false;

    private ActionMode actionMode;
    DisplayMetrics outMetrics;

    AlertDialog statusDialog;

    MenuItem selectMenuItem;
    MenuItem unSelectMenuItem;

    Handler handler;
    int stateHistory;
    public long chatId = -1;
    public long selectedMessageId = -1;

    ChatController chatC;

    private MegaNode myChatFilesFolder;
    private ArrayList<MegaChatMessage> preservedMessagesSelected;
    private ArrayList<MegaChatMessage> preservedMessagesToImport;

    private NodeAttachmentBottomSheetDialogFragment bottomSheetDialogFragment;

    private final BroadcastReceiver errorCopyingNodesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !BROADCAST_ACTION_ERROR_COPYING_NODES.equals(intent.getAction())) {
                return;
            }

            removeProgressDialog();
            showSnackbar(SNACKBAR_TYPE, intent.getStringExtra(ERROR_MESSAGE_TEXT));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate");
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(NodeAttachmentHistoryViewModel.class);

        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return;
        }

        chatC = new ChatController(this);

        megaChatApi.addNodeHistoryListener(chatId, this);

        handler = new Handler();

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        registerReceiver(errorCopyingNodesReceiver,
                new IntentFilter(BROADCAST_ACTION_ERROR_COPYING_NODES));

        setContentView(R.layout.activity_node_history);

        if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong("chatId", -1);

            nodeSaver.restoreState(savedInstanceState);
        }

        //Set toolbar
        tB = findViewById(R.id.toolbar_node_history);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setDisplayShowHomeEnabled(true);

        aB.setTitle(getString(R.string.title_chat_shared_files_info));

        container = (RelativeLayout) findViewById(R.id.node_history_main_layout);

        emptyLayout = (RelativeLayout) findViewById(R.id.empty_layout_node_history);
        emptyTextView = (TextView) findViewById(R.id.empty_text_node_history);
        emptyImageView = (ImageView) findViewById(R.id.empty_image_view_node_history);

        ColorUtils.setImageViewAlphaIfDark(this, emptyImageView, ColorUtils.DARK_IMAGE_ALPHA);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            emptyImageView.setImageResource(R.drawable.contacts_empty_landscape);
        } else {
            emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
        }

        String textToShow = String.format(getString(R.string.context_empty_shared_files));
        try {
            textToShow = textToShow.replace("[A]", "<font color=\'" + getColorHexString(this, R.color.grey_900_grey_100) + "\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
            textToShow = textToShow.replace("[B]", "<font color=\'" + getColorHexString(this, R.color.grey_300_grey_600) + "\'>");
            textToShow = textToShow.replace("[/B]", "</font>");
        } catch (Exception e) {
        }
        Spanned result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
        emptyTextView.setText(result);

        linearLayoutList = (LinearLayout) findViewById(R.id.linear_layout_recycler_list);
        linearLayoutGrid = (LinearLayout) findViewById(R.id.linear_layout_recycler_grid);

        if (isList) {
            linearLayoutList.setVisibility(View.VISIBLE);
            linearLayoutGrid.setVisibility(View.GONE);

            listView = (RecyclerView) findViewById(R.id.node_history_list_view);
            listView.addItemDecoration(new SimpleDividerItemDecoration(this));
            mLayoutManager = new LinearLayoutManager(this);
            mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            listView.setLayoutManager(mLayoutManager);
            listView.setItemAnimator(noChangeRecyclerViewItemAnimator());
        } else {
            linearLayoutList.setVisibility(View.GONE);
            linearLayoutGrid.setVisibility(View.VISIBLE);

            listView = (NewGridRecyclerView) findViewById(R.id.file_grid_view_browser);
        }

        listView.setClipToPadding(false);
        listView.setHasFixedSize(true);

        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

                if (stateHistory != MegaChatApi.SOURCE_NONE) {
                    if (dy > 0) {
                        // Scrolling up
                        scrollingUp = true;
                    } else {
                        // Scrolling down
                        scrollingUp = false;
                    }

                    if (scrollingUp) {
                        int pos = mLayoutManager.findFirstVisibleItemPosition();

                        if (pos <= NUMBER_MESSAGES_BEFORE_LOAD && getMoreHistory) {
                            Timber.d("DE->loadAttachments:scrolling down");
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
        if (extras != null) {
            if (chatId == -1) {
                chatId = extras.getLong("chatId");
            }

            chatRoom = megaChatApi.getChatRoom(chatId);

            if (chatRoom != null) {
                messages = new ArrayList<>();
                bufferMessages = new ArrayList<MegaChatMessage>();

                if (messages.size() != 0) {
                    emptyLayout.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);
                } else {
                    emptyLayout.setVisibility(View.VISIBLE);
                    listView.setVisibility(View.GONE);
                }

                boolean resultOpen = megaChatApi.openNodeHistory(chatId, this);
                if (resultOpen) {
                    Timber.d("Node history opened correctly");

                    messages = new ArrayList<MegaChatMessage>();

                    if (isList) {
                        if (adapter == null) {
                            adapter = new NodeAttachmentHistoryAdapter(this, messages, listView, NodeAttachmentHistoryAdapter.ITEM_VIEW_TYPE_LIST);
                        }
                    } else {
                        if (adapter == null) {
                            adapter = new NodeAttachmentHistoryAdapter(this, messages, listView, NodeAttachmentHistoryAdapter.ITEM_VIEW_TYPE_GRID);
                        }
                    }

                    listView.setAdapter(adapter);
                    adapter.setMultipleSelect(false);

                    adapter.setMessages(messages);

                    isLoadingHistory = true;
                    Timber.d("A->loadAttachments");
                    stateHistory = megaChatApi.loadAttachments(chatId, NUMBER_MESSAGES_TO_LOAD);
                }
            } else {
                Timber.e("ERROR: node is NULL");
            }
        }
    }

    @Override
    protected void onDestroy() {
        Timber.d("onDestroy");
        super.onDestroy();
        unregisterReceiver(errorCopyingNodesReceiver);

        if (megaChatApi != null) {
            megaChatApi.removeNodeHistoryListener(chatId, this);
            megaChatApi.closeNodeHistory(chatId, null);
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        nodeSaver.destroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_node_history, menu);

        selectMenuItem = menu.findItem(R.id.action_select);
        unSelectMenuItem = menu.findItem(R.id.action_unselect);
        thumbViewMenuItem = menu.findItem(R.id.action_grid);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (messages.size() > 0) {
            selectMenuItem.setVisible(true);
        } else {
            selectMenuItem.setVisible(false);
        }

        unSelectMenuItem.setVisible(false);
        thumbViewMenuItem.setVisible(false);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_select) {
            selectAll();
            return true;
        } else if (itemId == R.id.action_grid) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void activateActionMode() {
        Timber.d("activateActionMode");
        if (!adapter.isMultipleSelect()) {
            adapter.setMultipleSelect(true);
            actionMode = startSupportActionMode(new NodeAttachmentHistoryActivity.ActionBarCallBack());
        }
    }

    // Clear all selected items
    private void clearSelections() {
        if (adapter.isMultipleSelect()) {
            adapter.clearSelections();
        }
    }

    public void selectAll() {
        Timber.d("selectAll");
        if (adapter != null) {
            if (adapter.isMultipleSelect()) {
                adapter.selectAll();
            } else {
                adapter.setMultipleSelect(true);
                adapter.selectAll();

                actionMode = startSupportActionMode(new ActionBarCallBack());
            }
            new Handler(Looper.getMainLooper()).post(() -> updateActionModeTitle());
        }
    }

    public boolean showSelectMenuItem() {
        if (adapter != null) {
            return adapter.isMultipleSelect();
        }

        return false;
    }

    public void itemClick(int position) {
        Timber.d("Position: %s", position);
        megaChatApi.signalPresenceActivity();

        if (position < messages.size()) {
            MegaChatMessage m = messages.get(position);

            if (adapter.isMultipleSelect()) {

                adapter.toggleSelection(position);

                List<MegaChatMessage> messages = adapter.getSelectedMessages();
                if (messages.size() > 0) {
                    updateActionModeTitle();
                }

            } else {

                if (m != null) {
                    MegaNodeList nodeList = m.getMegaNodeList();
                    if (nodeList.size() == 1) {
                        MegaNode node = nodeList.get(0);

                        if (MimeTypeList.typeForName(node.getName()).isImage()) {
                            if (node.hasPreview()) {
                                Timber.d("Show full screen viewer");
                                showFullScreenViewer(m.getMsgId());
                            } else {
                                Timber.d("Image without preview - show node attachment panel for one node");
                                showNodeAttachmentBottomSheet(m, position);
                            }
                        } else if (MimeTypeList.typeForName(node.getName()).isVideoMimeType() || MimeTypeList.typeForName(node.getName()).isAudio()) {
                            Timber.d("isFile:isVideoReproducibleOrIsAudio");
                            String mimeType = MimeTypeList.typeForName(node.getName()).getType();
                            Timber.d("FILE HANDLE: %d, TYPE: %s", node.getHandle(), mimeType);

                            Intent mediaIntent;
                            boolean internalIntent;
                            boolean opusFile = false;
                            if (MimeTypeList.typeForName(node.getName()).isVideoNotSupported() || MimeTypeList.typeForName(node.getName()).isAudioNotSupported()) {
                                mediaIntent = new Intent(Intent.ACTION_VIEW);
                                internalIntent = false;
                                String[] s = node.getName().split("\\.");
                                if (s != null && s.length > 1 && s[s.length - 1].equals("opus")) {
                                    opusFile = true;
                                }
                            } else {
                                Timber.d("setIntentToAudioVideoPlayer");
                                mediaIntent = getMediaIntent(this, node.getName());
                                internalIntent = true;
                            }

                            mediaIntent.putExtra("adapterType", FROM_CHAT);
                            mediaIntent.putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, false);
                            mediaIntent.putExtra("msgId", m.getMsgId());
                            mediaIntent.putExtra("chatId", chatId);

                            mediaIntent.putExtra("FILENAME", node.getName());

                            String localPath = getLocalFile(node);

                            if (localPath != null) {
                                File mediaFile = new File(localPath);
                                if (localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                                    Timber.d("FileProviderOption");
                                    Uri mediaFileUri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", mediaFile);
                                    if (mediaFileUri == null) {
                                        Timber.e("ERROR: NULL media file Uri");
                                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error));
                                    } else {
                                        mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
                                    }
                                } else {
                                    Uri mediaFileUri = Uri.fromFile(mediaFile);
                                    if (mediaFileUri == null) {
                                        Timber.e("ERROR :NULL media file Uri");
                                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error));
                                    } else {
                                        mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
                                    }
                                }
                                mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } else {
                                Timber.w("Local Path NULL");
                                if (viewModel.isOnline()) {
                                    if (megaApi.httpServerIsRunning() == 0) {
                                        megaApi.httpServerStart();
                                        mediaIntent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true);
                                    } else {
                                        Timber.w("ERROR: HTTP server already running");
                                    }

                                    ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                                    ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
                                    activityManager.getMemoryInfo(mi);

                                    if (mi.totalMem > BUFFER_COMP) {
                                        Timber.d("Total mem: %d allocate 32 MB", mi.totalMem);
                                        megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                                    } else {
                                        Timber.d("Total mem: %d allocate 16 MB", mi.totalMem);
                                        megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                                    }

                                    String url = megaApi.httpServerGetLocalLink(node);
                                    if (url != null) {
                                        Uri parsedUri = Uri.parse(url);
                                        if (parsedUri != null) {
                                            mediaIntent.setDataAndType(parsedUri, mimeType);
                                        } else {
                                            Timber.e("ERROR: HTTP server get local link");
                                            showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error));
                                        }
                                    } else {
                                        Timber.e("ERROR: HTTP server get local link");
                                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error));
                                    }
                                } else {
                                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem) + ". " + getString(R.string.no_network_connection_on_play_file));
                                }
                            }
                            mediaIntent.putExtra("HANDLE", node.getHandle());
                            if (opusFile) {
                                mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
                            }
                            if (internalIntent) {
                                startActivity(mediaIntent);
                            } else {
                                Timber.d("External Intent");
                                if (isIntentAvailable(this, mediaIntent)) {
                                    startActivity(mediaIntent);
                                } else {
                                    Timber.w("No available Intent");
                                    showNodeAttachmentBottomSheet(m, position);
                                }
                            }
                        } else if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                            Timber.d("isFile:isPdf");
                            String mimeType = MimeTypeList.typeForName(node.getName()).getType();
                            Timber.d("FILE HANDLE: %d, TYPE: %s", node.getHandle(), mimeType);
                            Intent pdfIntent = new Intent(this, PdfViewerActivity.class);
                            pdfIntent.putExtra("inside", true);
                            pdfIntent.putExtra("adapterType", FROM_CHAT);
                            pdfIntent.putExtra("msgId", m.getMsgId());
                            pdfIntent.putExtra("chatId", chatId);

                            pdfIntent.putExtra("FILENAME", node.getName());

                            String localPath = getLocalFile(node);
                            if (localPath != null) {
                                File mediaFile = new File(localPath);
                                if (localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                                    Timber.d("File Provider Option");
                                    Uri mediaFileUri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", mediaFile);
                                    if (mediaFileUri == null) {
                                        Timber.e("ERROR: NULL media file Uri");
                                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error));
                                    } else {
                                        pdfIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
                                    }
                                } else {
                                    Uri mediaFileUri = Uri.fromFile(mediaFile);
                                    if (mediaFileUri == null) {
                                        Timber.e("ERROR: NULL media file Uri");
                                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error));
                                    } else {
                                        pdfIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
                                    }
                                }
                                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } else {
                                Timber.w("Local Path NULL");
                                if (viewModel.isOnline()) {
                                    if (megaApi.httpServerIsRunning() == 0) {
                                        megaApi.httpServerStart();
                                        pdfIntent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true);
                                    } else {
                                        Timber.w("ERROR: HTTP server already running");
                                    }
                                    ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                                    ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
                                    activityManager.getMemoryInfo(mi);
                                    if (mi.totalMem > BUFFER_COMP) {
                                        Timber.d("Total mem: %d allocate 32 MB", mi.totalMem);
                                        megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                                    } else {
                                        Timber.d("Total mem: %d allocate 16 MB", mi.totalMem);
                                        megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                                    }
                                    String url = megaApi.httpServerGetLocalLink(node);
                                    if (url != null) {
                                        Uri parsedUri = Uri.parse(url);
                                        if (parsedUri != null) {
                                            pdfIntent.setDataAndType(parsedUri, mimeType);
                                        } else {
                                            Timber.e("ERROR: HTTP server get local link");
                                            showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error));
                                        }
                                    } else {
                                        Timber.e("ERROR: HTTP server get local link");
                                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error));
                                    }
                                } else {
                                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem) + ". " + getString(R.string.no_network_connection_on_play_file));
                                }
                            }
                            pdfIntent.putExtra("HANDLE", node.getHandle());

                            if (isIntentAvailable(this, pdfIntent)) {
                                startActivity(pdfIntent);
                            } else {
                                Timber.w("No svailable Intent");
                                showNodeAttachmentBottomSheet(m, position);
                            }
                            overridePendingTransition(0, 0);
                        } else if (MimeTypeList.typeForName(node.getName()).isOpenableTextFile(node.getSize())) {
                            manageTextFileIntent(this, m.getMsgId(), chatId);
                        } else {
                            Timber.d("NOT Image, pdf, audio or video - show node attachment panel for one node");
                            showNodeAttachmentBottomSheet(m, position);
                        }
                    } else {
                        Timber.d("Show node attachment panel");
                        showNodeAttachmentBottomSheet(m, position);
                    }
                }
            }
        } else {
            Timber.w("DO NOTHING: Position (%d) is more than size in messages (size: %d)", position, messages.size());
        }
    }

    public void showFullScreenViewer(long msgId) {
        long currentNodeHandle = INVALID_HANDLE;
        List<Long> messageIds = new ArrayList<>();

        for (MegaChatMessage message : messages) {
            messageIds.add(message.getMsgId());
            if (message.getMsgId() == msgId) {
                currentNodeHandle = message.getMegaNodeList().get(0).getHandle();
            }
        }

        Intent intent = ImageViewerActivity.getIntentForChatMessages(
                this,
                chatId,
                Longs.toArray(messageIds),
                currentNodeHandle
        );
        startActivity(intent);
    }

    private void updateActionModeTitle() {
        Timber.d("updateActionModeTitle");
        if (actionMode == null) {
            return;
        }

        int num = adapter.getSelectedItemCount();
        try {
            actionMode.setTitle(num + "");
            actionMode.invalidate();
        } catch (Exception e) {
            e.printStackTrace();
            Timber.e(e, "Invalidate error");
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
        if (v.getId() == R.id.file_contact_list_layout) {
            Intent i = new Intent(this, ManagerActivity.class);
            i.setAction(ACTION_REFRESH_PARENTHANDLE_BROWSER);
            startActivity(i);
            finish();
        }
    }

    public void notifyDataSetChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Timber.d("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        if (chatRoom != null) {
            outState.putLong("chatId", chatRoom.getChatId());
        }

        nodeSaver.saveState(outState);
    }

    @Override
    public void storedUnhandledData(ArrayList<MegaChatMessage> preservedData) {
    }

    @Override
    public void handleStoredData() {
        chatC.proceedWithForwardOrShare(this, myChatFilesFolder, preservedMessagesSelected,
                preservedMessagesToImport, chatId, FORWARD_ONLY_OPTION);
        preservedMessagesSelected = null;
        preservedMessagesToImport = null;
    }

    @Override
    public void storedUnhandledData(ArrayList<MegaChatMessage> messagesSelected, ArrayList<MegaChatMessage> messagesToImport) {
        preservedMessagesSelected = messagesSelected;
        preservedMessagesToImport = messagesToImport;
    }

    @Override
    public void showSnackbar(int type, @Nullable String content, long chatId) {
        showSnackbar(type, container, content, chatId);
    }

    private class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Timber.d("onActionItemClicked");
            final ArrayList<MegaChatMessage> messagesSelected = adapter.getSelectedMessages();

            if (viewModel.getStorageState() == StorageState.PayWall &&
                    item.getItemId() != R.id.cab_menu_select_all && item.getItemId() != R.id.cab_menu_unselect_all) {
                showOverDiskQuotaPaywallWarning();
                return false;
            }

            int itemId = item.getItemId();
            if (itemId == R.id.cab_menu_select_all) {
                selectAll();
            } else if (itemId == R.id.cab_menu_unselect_all) {
                clearSelections();
            } else if (itemId == R.id.chat_cab_menu_forward) {
                Timber.d("Forward message");
                clearSelections();
                hideMultipleSelect();
                forwardMessages(messagesSelected);
            } else if (itemId == R.id.chat_cab_menu_delete) {
                clearSelections();
                hideMultipleSelect();
                //Delete
                showConfirmationDeleteMessages(messagesSelected, chatRoom);
            } else if (itemId == R.id.chat_cab_menu_download) {
                clearSelections();
                hideMultipleSelect();

                ArrayList<MegaNodeList> list = new ArrayList<>();
                for (int i = 0; i < messagesSelected.size(); i++) {

                    MegaNodeList megaNodeList = messagesSelected.get(i).getMegaNodeList();
                    list.add(megaNodeList);
                }
                PermissionUtils.checkNotificationsPermission(nodeAttachmentHistoryActivity);
                nodeSaver.saveNodeLists(list, false, false, false, true);
            } else if (itemId == R.id.chat_cab_menu_import) {
                clearSelections();
                hideMultipleSelect();
                chatC.importNodesFromMessages(messagesSelected);
            } else if (itemId == R.id.chat_cab_menu_offline) {
                PermissionUtils.checkNotificationsPermission(nodeAttachmentHistoryActivity);
                clearSelections();
                hideMultipleSelect();
                chatC.saveForOfflineWithMessages(messagesSelected,
                        megaChatApi.getChatRoom(chatId), NodeAttachmentHistoryActivity.this);
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Timber.d("onCreateActionMode");
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.messages_node_history_action, menu);

            importIcon = menu.findItem(R.id.chat_cab_menu_import);
            checkScroll();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            Timber.d("onDestroyActionMode");
            adapter.clearSelections();
            adapter.setMultipleSelect(false);
            checkScroll();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Timber.d("onPrepareActionMode");
            List<MegaChatMessage> selected = adapter.getSelectedMessages();
            if (selected.size() != 0) {

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

                } else {

                    Timber.d("Chat with permissions");
                    if (viewModel.isOnline() && !chatC.isInAnonymousMode()) {
                        menu.findItem(R.id.chat_cab_menu_forward).setVisible(true);
                    } else {
                        menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                    }

                    if (selected.size() == 1) {
                        if (selected.get(0).getUserHandle() == megaChatApi.getMyUserHandle() && selected.get(0).isDeletable()) {
                            Timber.d("One message - Message DELETABLE");
                            menu.findItem(R.id.chat_cab_menu_delete).setVisible(true);
                        } else {
                            menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                        }

                        if (viewModel.isOnline()) {
                            menu.findItem(R.id.chat_cab_menu_download).setVisible(true);
                            if (chatC.isInAnonymousMode()) {
                                menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                                importIcon.setVisible(false);
                            } else {
                                menu.findItem(R.id.chat_cab_menu_offline).setVisible(true);
                                importIcon.setVisible(true);
                            }
                        } else {
                            menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                            importIcon.setVisible(false);
                        }

                    } else {
                        Timber.d("Many items selected");
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

                        if (viewModel.isOnline()) {
                            menu.findItem(R.id.chat_cab_menu_download).setVisible(true);
                            if (chatC.isInAnonymousMode()) {
                                menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                                importIcon.setVisible(false);
                            } else {
                                menu.findItem(R.id.chat_cab_menu_offline).setVisible(true);
                                importIcon.setVisible(true);
                            }
                        } else {
                            menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                            importIcon.setVisible(false);
                        }

                        menu.findItem(R.id.chat_cab_menu_delete).setVisible(showDelete);
                        if (viewModel.isOnline() && !chatC.isInAnonymousMode()) {
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

    public void showConfirmationDeleteMessages(final ArrayList<MegaChatMessage> messages, final MegaChatRoom chat) {
        Timber.d("Chat ID: %s", chat.getChatId());

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
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

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);

        if (messages.size() == 1) {
            builder.setMessage(R.string.confirmation_delete_one_message);
        } else {
            builder.setMessage(R.string.confirmation_delete_several_messages);
        }
        builder.setPositiveButton(R.string.context_remove, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public void forwardMessages(ArrayList<MegaChatMessage> messagesSelected) {
        Timber.d("forwardMessages");
        chatC.prepareMessagesToForward(messagesSelected, chatId);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        nodeSaver.handleRequestPermissionsResult(requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Timber.d("Result Code: %s", resultCode);

        if (nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)) {
            return;
        }

        if (requestCode == REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK) {
            if (!viewModel.isOnline() || megaApi == null) {
                try {
                    statusDialog.dismiss();
                } catch (Exception ex) {
                }
                ;

                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
                return;
            }

            final long toHandle = intent.getLongExtra("IMPORT_TO", 0);

            final long[] importMessagesHandles = intent.getLongArrayExtra("HANDLES_IMPORT_CHAT");

            importNodes(toHandle, importMessagesHandles);
        } else if (requestCode == REQUEST_CODE_SELECT_CHAT && resultCode == RESULT_OK) {
            if (!viewModel.isOnline()) {
                try {
                    statusDialog.dismiss();
                } catch (Exception ex) {
                }
                ;

                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem));
                return;
            }

            showProgressForwarding();

            long[] idMessages = intent.getLongArrayExtra(ID_MESSAGES);
            long[] chatHandles = intent.getLongArrayExtra(SELECTED_CHATS);
            long[] contactHandles = intent.getLongArrayExtra(SELECTED_USERS);

            if (chatHandles != null && chatHandles.length > 0 && idMessages != null) {
                if (contactHandles != null && contactHandles.length > 0) {
                    ArrayList<MegaUser> users = new ArrayList<>();
                    ArrayList<MegaChatRoom> chats = new ArrayList<>();

                    for (int i = 0; i < contactHandles.length; i++) {
                        MegaUser user = megaApi.getContact(MegaApiAndroid.userHandleToBase64(contactHandles[i]));
                        if (user != null) {
                            users.add(user);
                        }
                    }

                    for (int i = 0; i < chatHandles.length; i++) {
                        MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatHandles[i]);
                        if (chatRoom != null) {
                            chats.add(chatRoom);
                        }
                    }

                    CreateChatListener listener = new CreateChatListener(
                            CreateChatListener.SEND_MESSAGES, chats, users, this, this, idMessages,
                            chatId);

                    for (MegaUser user : users) {
                        MegaChatPeerList peers = MegaChatPeerList.createInstance();
                        peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
                        megaChatApi.createChat(false, peers, listener);
                    }
                } else {
                    int countChat = chatHandles.length;
                    Timber.d("Selected: %d chats to send", countChat);

                    MultipleForwardChatProcessor forwardChatProcessor = new MultipleForwardChatProcessor(this, chatHandles, idMessages, chatId);
                    forwardChatProcessor.forward(chatRoom);
                }
            } else {
                Timber.e("Error on sending to chat");
            }
        }
    }

    public void showProgressForwarding() {
        Timber.d("showProgressForwarding");

        statusDialog = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.general_forwarding));
        statusDialog.show();
    }

    public void removeProgressDialog() {
        try {
            statusDialog.dismiss();
        } catch (Exception ex) {
            Timber.e(ex);
        }
    }

    public void importNodes(final long toHandle, final long[] importMessagesHandles) {
        statusDialog = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.general_importing));
        statusDialog.show();

        checkNameCollisionUseCase.checkMessagesToImport(importMessagesHandles, chatId, toHandle)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result, throwable) -> {
                    if (throwable == null) {
                        ArrayList<NameCollision> collisions = result.getFirst();

                        if (!collisions.isEmpty()) {
                            dismissAlertDialogIfExists(statusDialog);
                            nameCollisionActivityContract.launch(collisions);
                        }

                        List<MegaNode> nodesWithoutCollision = result.getSecond();

                        if (!nodesWithoutCollision.isEmpty()) {
                            legacyCopyNodeUseCase.copy(nodesWithoutCollision, toHandle)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe((copyResult, copyThrowable) -> {
                                        dismissAlertDialogIfExists(statusDialog);

                                        if (copyThrowable != null) {
                                            manageCopyMoveException(copyThrowable);
                                        }

                                        showSnackbar(SNACKBAR_TYPE, copyThrowable == null
                                                        ? copyRequestMessageMapper.invoke(copyResult)
                                                        : getString(R.string.import_success_error),
                                                MEGACHAT_INVALID_HANDLE);
                                    });
                        }
                    } else {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.import_success_error), MEGACHAT_INVALID_HANDLE);
                    }
                });
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
    public void onAttachmentLoaded(MegaChatApiJava api, MegaChatMessage msg) {
        if (msg != null) {
            Timber.d("Message ID%s", msg.getMsgId());
            if (msg.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT) {

                MegaNodeList nodeList = msg.getMegaNodeList();
                if (nodeList != null) {

                    if (nodeList.size() == 1) {
                        MegaNode node = nodeList.get(0);
                        Timber.d("Node Handle: %s", node.getHandle());
                        bufferMessages.add(msg);
                        Timber.d("Size of buffer: %s", bufferMessages.size());
                        Timber.d("Size of messages: %s", messages.size());
                    }
                }
            }
        } else {
            Timber.d("Message is NULL: end of history");
            if ((bufferMessages.size() + messages.size()) >= NUMBER_MESSAGES_TO_LOAD) {
                fullHistoryReceivedOnLoad();
                isLoadingHistory = false;
            } else {
                Timber.d("Less Number Received");
                if ((stateHistory != MegaChatApi.SOURCE_NONE) && (stateHistory != MegaChatApi.SOURCE_ERROR)) {
                    Timber.d("But more history exists --> loadAttachments");
                    isLoadingHistory = true;
                    stateHistory = megaChatApi.loadAttachments(chatId, NUMBER_MESSAGES_TO_LOAD);
                    Timber.d("New state of history: %s", stateHistory);
                    getMoreHistory = false;
                    if (stateHistory == MegaChatApi.SOURCE_NONE || stateHistory == MegaChatApi.SOURCE_ERROR) {
                        fullHistoryReceivedOnLoad();
                        isLoadingHistory = false;
                    }
                } else {
                    Timber.d("New state of history: %s", stateHistory);
                    fullHistoryReceivedOnLoad();
                    isLoadingHistory = false;
                }
            }
        }
    }

    public void fullHistoryReceivedOnLoad() {
        Timber.d("Messages size: %s", messages.size());

        if (bufferMessages.size() != 0) {
            Timber.d("Buffer size: %s", bufferMessages.size());
            emptyLayout.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);

            ListIterator<MegaChatMessage> itr = bufferMessages.listIterator();
            while (itr.hasNext()) {
                int currentIndex = itr.nextIndex();
                MegaChatMessage messageToShow = itr.next();
                messages.add(messageToShow);
            }

            if (messages.size() != 0) {
                if (adapter == null) {
                    adapter = new NodeAttachmentHistoryAdapter(this, messages, listView, NodeAttachmentHistoryAdapter.ITEM_VIEW_TYPE_LIST);
                    listView.setLayoutManager(mLayoutManager);
                    listView.addItemDecoration(new SimpleDividerItemDecoration(this));
                    listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                            super.onScrolled(recyclerView, dx, dy);
                            checkScroll();
                        }
                    });
                    listView.setAdapter(adapter);
                    adapter.setMessages(messages);
                } else {
                    adapter.loadPreviousMessages(messages, bufferMessages.size());
                }

            }
            bufferMessages.clear();
        }

        Timber.d("getMoreHistoryTRUE");
        getMoreHistory = true;

        invalidateOptionsMenu();
    }

    @Override
    public void onAttachmentReceived(MegaChatApiJava api, MegaChatMessage msg) {
        Timber.d("STATUS: %s", msg.getStatus());
        Timber.d("TEMP ID: %s", msg.getTempId());
        Timber.d("FINAL ID: %s", msg.getMsgId());
        Timber.d("TIMESTAMP: %s", msg.getTimestamp());
        Timber.d("TYPE: %s", msg.getType());

        int lastIndex = 0;
        if (messages.size() == 0) {
            messages.add(msg);
        } else {
            Timber.d("Status of message: %s", msg.getStatus());

            while (messages.get(lastIndex).getMsgIndex() > msg.getMsgIndex()) {
                lastIndex++;
            }

            Timber.d("Append in position: %s", lastIndex);
            messages.add(lastIndex, msg);
        }

        //Create adapter
        if (adapter == null) {
            Timber.d("Create adapter");
            adapter = new NodeAttachmentHistoryAdapter(this, messages, listView, NodeAttachmentHistoryAdapter.ITEM_VIEW_TYPE_LIST);
            listView.setLayoutManager(mLayoutManager);
            listView.addItemDecoration(new SimpleDividerItemDecoration(this));
            listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    checkScroll();
                }
            });
            listView.setAdapter(adapter);
            adapter.setMessages(messages);
        } else {
            Timber.d("Update adapter with last index: %s", lastIndex);
            if (lastIndex < 0) {
                Timber.d("Arrives the first message of the chat");
                adapter.setMessages(messages);
            } else {
                adapter.addMessage(messages, lastIndex + 1);
                adapter.notifyItemChanged(lastIndex);
            }
        }

        emptyLayout.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);

        invalidateOptionsMenu();
    }

    @Override
    public void onAttachmentDeleted(MegaChatApiJava api, long msgid) {
        Timber.d("Message ID: %s", msgid);

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

        if (indexToChange != -1) {
            messages.remove(indexToChange);
            Timber.d("Removed index: %d, Messages size: %d", indexToChange, messages.size());

            adapter.removeMessage(indexToChange, messages);

            if (messages.isEmpty()) {
                emptyLayout.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
            }
        } else {
            Timber.w("Index to remove not found");
        }

        invalidateOptionsMenu();
    }

    @Override
    public void onTruncate(MegaChatApiJava api, long msgid) {
        Timber.d("Message ID: %s", msgid);
        invalidateOptionsMenu();
        messages.clear();
        adapter.notifyDataSetChanged();
        listView.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.VISIBLE);
    }

    public void showNodeAttachmentBottomSheet(MegaChatMessage message, int position) {
        Timber.d("showNodeAttachmentBottomSheet: %s", position);

        if (message == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedMessageId = message.getMsgId();
        bottomSheetDialogFragment = new NodeAttachmentBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void showSnackbar(int type, String s) {
        showSnackbar(type, container, s);
    }

    public void checkScroll() {
        if (listView != null) {
            changeToolBarElevation(this, tB, listView.canScrollVertically(-1)
                    || (adapter != null && adapter.isMultipleSelect()));
        }
    }

    public MegaChatRoom getChatRoom() {
        return chatRoom;
    }

    public void setMyChatFilesFolder(MegaNode myChatFilesFolder) {
        this.myChatFilesFolder = myChatFilesFolder;
    }

    public void downloadNodeList(MegaNodeList nodeList) {
        PermissionUtils.checkNotificationsPermission(this);
        nodeSaver.saveNodeLists(Collections.singletonList(nodeList), false, false, false, true);
    }
}

