package mega.privacy.android.app.main;

import static mega.privacy.android.app.utils.Constants.AUTHORITY_STRING_FILE_PROVIDER;
import static mega.privacy.android.app.utils.Constants.LONG_SNACKBAR_DURATION;
import static mega.privacy.android.app.utils.Constants.REQUEST_WRITE_STORAGE;
import static mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION;
import static mega.privacy.android.app.utils.Constants.TYPE_TEXT_PLAIN;
import static mega.privacy.android.app.utils.FileUtil.buildDefaultDownloadDir;
import static mega.privacy.android.app.utils.FileUtil.getRecoveryKeyFileName;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.isAndroid11OrUpper;
import static mega.privacy.android.app.utils.Util.isAndroidNougatOrUpper;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;
import static mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator;
import static mega.privacy.android.app.utils.Util.showErrorAlertDialog;
import static mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions;
import static mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.text.HtmlCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anggrayudi.storage.file.DocumentFileUtils;
import com.anggrayudi.storage.file.StorageId;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import mega.privacy.android.app.FileDocument;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.interfaces.Scrollable;
import mega.privacy.android.app.main.adapters.FileStorageAdapter;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.StringResourcesUtils;
import timber.log.Timber;

public class FileStorageActivity extends PasscodeActivity implements Scrollable {

    private static final String PATH = "PATH";
    public static final String PICK_FOLDER_TYPE = "PICK_FOLDER_TYPE";
    private static final int REQUEST_SAVE_RK = 1122;
    private static final int REQUEST_PICK_CU_FOLDER = 1133;
    private static final int REQUEST_PICK_DOWNLOAD_FOLDER = 1144;

    public enum PickFolderType {
        CU_FOLDER("CU_FOLDER"),
        MU_FOLDER("MU_FOLDER"),
        DOWNLOAD_FOLDER("DOWNLOAD_FOLDER"),
        NONE_ONLY_DOWNLOAD("NONE_ONLY_DOWNLOAD");

        private final String folderType;

        PickFolderType(String folderType) {
            this.folderType = folderType;
        }

        public String getFolderType() {
            return folderType;
        }
    }

    public static final String EXTRA_URL = "fileurl";
    public static final String EXTRA_SIZE = "filesize";
    public static final String EXTRA_SERIALIZED_NODES = "serialized_nodes";
    public static final String EXTRA_DOCUMENT_HASHES = "document_hash";
    public static final String EXTRA_SAVE_RECOVERY_KEY = "save_recovery_key";
    public static final String EXTRA_PATH = "filepath";
    public static final String EXTRA_FILES = "fileslist";
    public static final String EXTRA_PROMPT = "prompt";

    // Pick modes
    public enum Mode {
        // Select single folder
        PICK_FOLDER("ACTION_PICK_FOLDER"),
        //Browse files
        BROWSE_FILES("ACTION_BROWSE_FILES");

        private final String action;

        Mode(String action) {
            this.action = action;
        }

        public String getAction() {
            return action;
        }

        public static Mode getFromIntent(Intent intent) {
            if (intent.getAction().equals(BROWSE_FILES.getAction())) {
                return BROWSE_FILES;
            } else {
                return PICK_FOLDER;
            }
        }
    }

    private MegaPreferences prefs;
    private Mode mode;
    private File path;
    private File root;
    private RelativeLayout viewContainer;
    private TextView contentText;
    private RecyclerView listView;
    private LinearLayoutManager mLayoutManager;
    private ImageView emptyImageView;
    private TextView emptyTextView;

    private PickFolderType pickFolderType;
    private boolean isCUOrMUFolder;
    private String prompt;

    private Stack<Integer> lastPositionStack;
    private String url;
    private long size;
    private long[] documentHashes;
    private ArrayList<String> serializedNodes;

    private FileStorageAdapter adapter;
    private Toolbar tB;
    private ActionBar aB;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.d("onOptionsItemSelected");

        // Handle presses on the action bar items
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_filestorage);

        viewContainer = findViewById(R.id.file_storage_container);
        contentText = findViewById(R.id.file_storage_content_text);
        listView = findViewById(R.id.file_storage_list_view);

        Intent intent = getIntent();
        prompt = intent.getStringExtra(EXTRA_PROMPT);
        if (prompt != null) {
            showSnackbar(viewContainer, prompt);
        }

        setPickFolderType(intent.getStringExtra(PICK_FOLDER_TYPE));

        if (intent.getBooleanExtra(EXTRA_SAVE_RECOVERY_KEY, false)) {
            createRKFile();
            return;
        } else if (isCUOrMUFolder) {
            openPickCUFolderFromSystem();
            return;
        } else if (pickFolderType == PickFolderType.DOWNLOAD_FOLDER) {
            if (isAndroid11OrUpper()) {
                path = buildDefaultDownloadDir(this);
                path.mkdirs();
                finishPickFolder();
                return;
            }

            openPickDownloadFolderFromSystem();
            return;
        }

        boolean hasStoragePermission = hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (!hasStoragePermission) {
            requestPermission(this,
                    REQUEST_WRITE_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        //Set toolbar
        tB = findViewById(R.id.toolbar_filestorage);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setDisplayShowHomeEnabled(true);

        mode = Mode.getFromIntent(intent);

        if (mode == Mode.PICK_FOLDER) {
            if (intent.getExtras() != null) {
                documentHashes = intent.getExtras().getLongArray(EXTRA_DOCUMENT_HASHES);
                url = intent.getExtras().getString(EXTRA_URL);
                size = intent.getExtras().getLong(EXTRA_SIZE);
            }

            serializedNodes = intent.getStringArrayListExtra(EXTRA_SERIALIZED_NODES);
        } else if (mode == Mode.BROWSE_FILES) {
            aB.setTitle(getString(R.string.browse_files_label));
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(PATH)) {
            path = new File(savedInstanceState.getString(PATH));
        }

        emptyImageView = findViewById(R.id.file_storage_empty_image);
        emptyTextView = findViewById(R.id.file_storage_empty_text);
        emptyImageView.setImageResource(isScreenInPortrait(this) ? R.drawable.empty_folder_portrait : R.drawable.empty_folder_landscape);

        String textToShow = getString(R.string.file_browser_empty_folder_new);
        try {
            textToShow = textToShow.replace("[A]", "<font color=\'"
                    + ColorUtils.getColorHexString(this, R.color.grey_900_grey_100)
                    + "\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
            textToShow = textToShow.replace("[B]", "<font color=\'"
                    + ColorUtils.getColorHexString(this, R.color.grey_300_grey_600)
                    + "\'>");
            textToShow = textToShow.replace("[/B]", "</font>");
        } catch (Exception e) {
            Timber.w(e, "Exception formatting text");
        }
        emptyTextView.setText(HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY));

        listView = findViewById(R.id.file_storage_list_view);
        listView.addItemDecoration(new SimpleDividerItemDecoration(this));
        mLayoutManager = new LinearLayoutManager(this);
        listView.setLayoutManager(mLayoutManager);
        listView.setItemAnimator(noChangeRecyclerViewItemAnimator());
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScroll();
            }
        });

        if (adapter == null) {
            adapter = new FileStorageAdapter(this, mode);
            listView.setAdapter(adapter);
        }

        lastPositionStack = new Stack<>();

        prefs = dbH.getPreferences();

        if (mode == Mode.BROWSE_FILES) {
            if (intent.getExtras() != null) {
                String extraPath = intent.getExtras().getString(EXTRA_PATH);
                if (!isTextEmpty(extraPath)) {
                    root = path = new File(extraPath);
                }
            }
            checkPath();
        }
    }

    /**
     * Launches the System picker to create the Recovery Key file in the chosen path.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createRKFile() {
        File defaultDownloadDir = buildDefaultDownloadDir(this);
        defaultDownloadDir.mkdirs();
        Uri initialUri = Uri.parse(defaultDownloadDir.getAbsolutePath());

        startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(TYPE_TEXT_PLAIN)
                .putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
                .putExtra(Intent.EXTRA_TITLE, getRecoveryKeyFileName()), REQUEST_SAVE_RK);
    }

    /**
     * On Android 11 and upper we cannot show our app picker, we must use the system one.
     * So opens the system file picker in order to give the option to chose a CU or MU local folder.
     */
    private void openPickCUFolderFromSystem() {
        try {
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), REQUEST_PICK_CU_FOLDER);
        } catch (ActivityNotFoundException e) {
            showOpenDocumentWarningAndFinish(e);
        }
    }

    /**
     * Opens the file picker in order to allow the user choose a default download location.
     */
    private void openPickDownloadFolderFromSystem() {
        try {
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
                    REQUEST_PICK_DOWNLOAD_FOLDER);
        } catch (ActivityNotFoundException e) {
            showOpenDocumentWarningAndFinish(e);
        }
    }

    /**
     * Shows a warning when there is no app to pick folders and finishes.
     *
     * @param e The caught exception.
     */
    private void showOpenDocumentWarningAndFinish(ActivityNotFoundException e) {
        Timber.e(e, "Error launching ACTION_OPEN_DOCUMENT_TREE.");
        showSnackbar(viewContainer, StringResourcesUtils.getString(R.string.general_warning_no_picker));
        new Handler().postDelayed(this::finish, LONG_SNACKBAR_DURATION);
    }

    /**
     * Sets the type of pick folder action.
     *
     * @param pickFolderString the type of pick folder action.
     */
    private void setPickFolderType(String pickFolderString) {
        if (isTextEmpty(pickFolderString)) {
            pickFolderType = PickFolderType.NONE_ONLY_DOWNLOAD;
        } else if (pickFolderString.equals(PickFolderType.CU_FOLDER.getFolderType())) {
            pickFolderType = PickFolderType.CU_FOLDER;
        } else if (pickFolderString.equals(PickFolderType.MU_FOLDER.getFolderType())) {
            pickFolderType = PickFolderType.MU_FOLDER;
        } else if (pickFolderString.equals(PickFolderType.DOWNLOAD_FOLDER.getFolderType())) {
            pickFolderType = PickFolderType.DOWNLOAD_FOLDER;
        }

        isCUOrMUFolder = pickFolderType.equals(PickFolderType.CU_FOLDER)
                || pickFolderType.equals(PickFolderType.MU_FOLDER);
    }

    /**
     * Shows the empty view or the list view depending on if there are items in the adapter.
     * Hides both if the root view with Internal storage and External storage is shown.
     */
    private void showEmptyState() {
        if (adapter != null && adapter.getItemCount() > 0) {
            listView.setVisibility(View.VISIBLE);
            emptyImageView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.GONE);
        } else {
            listView.setVisibility(View.GONE);
            emptyImageView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Changes the path shown in the screen or finish the activity if the current one is not valid.
     */
    private void checkPath() {
        if (path == null) {
            Timber.e("Current path is not valid (null)");
            showErrorAlertDialog(getString(R.string.error_io_problem),
                    true, this);
            return;
        }

        changeFolder(path);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (emptyImageView != null) {
            emptyImageView.setImageResource(isScreenInPortrait(this)
                    ? R.drawable.empty_folder_portrait
                    : R.drawable.empty_folder_landscape);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        if (path != null) {
            state.putString(PATH, path.getAbsolutePath());
        }

        super.onSaveInstanceState(state);
    }

    /*
     * Open new folder
     * @param newPath New folder path
     */
    @SuppressLint("NewApi")
    private void changeFolder(File newPath) {
        Timber.d("New path: %s", newPath);

        setFiles(newPath);
        path = newPath;

        // Avoid crash if `contentText` is null due any reason. For more info see AND-12958.
        if (contentText == null) {
            contentText = findViewById(R.id.file_storage_content_text);
        }
        if (contentText != null) {
            contentText.setText(path.getAbsolutePath());
        }

        invalidateOptionsMenu();
    }

    /*
     * Update file list for new folder
     */
    private void setFiles(File path) {
        Timber.d("setFiles");
        List<FileDocument> documents = new ArrayList<FileDocument>();

        if (path == null || !path.canRead()) {
            showErrorAlertDialog(getString(R.string.error_io_problem),
                    true, this);
            return;
        }

        File[] files = path.listFiles();

        if (files != null) {
            Timber.d("Number of files: %s", files.length);

            for (File file : files) {
                FileDocument document = new FileDocument(file);
                if (document.isHidden()) {
                    continue;
                }

                documents.add(document);
            }

            Collections.sort(documents, new CustomComparator());
        }

        adapter.setFiles(documents);
        showEmptyState();
    }

    /*
     * Comparator to sort the files
     */
    public class CustomComparator implements Comparator<FileDocument> {
        @Override
        public int compare(FileDocument o1, FileDocument o2) {
            if (o1.isFolder() != o2.isFolder()) {
                return o1.isFolder() ? -1 : 1;
            }
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    }

    private void finishPickFolder() {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_PATH, path.getAbsolutePath());
        intent.putExtra(EXTRA_DOCUMENT_HASHES, documentHashes);
        intent.putStringArrayListExtra(EXTRA_SERIALIZED_NODES, serializedNodes);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_SIZE, size);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void itemClick(int position) {
        Timber.d("Position: %s", position);

        FileDocument document = adapter.getDocumentAt(position);
        if (document == null) {
            return;
        }

        if (document.isFolder()) {
            if (!document.getFile().canRead()) {
                return;
            }

            lastPositionStack.push(mLayoutManager.findFirstCompletelyVisibleItemPosition());
            changeFolder(document.getFile());
        } else if (mode == Mode.BROWSE_FILES) {
            File f = adapter.getItem(position).getFile();

            if (isFileAvailable(f)) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri uri = isAndroidNougatOrUpper() ? FileProvider.getUriForFile(this, AUTHORITY_STRING_FILE_PROVIDER, f)
                        : Uri.fromFile(f);

                if (uri != null) {
                    intent.setDataAndType(uri, MimeTypeList.typeForName(f.getName()).getType());
                } else {
                    Timber.w("The file cannot be opened, uri is null");
                    return;
                }

                if (isIntentAvailable(this, intent)) {
                    startActivity(intent);
                }
            } else {
                showSnackbar(viewContainer, getString(R.string.corrupt_video_dialog_text));
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return;
        retryConnectionsAndSignalPresence();

        // Finish activity if at the root
        if (path.equals(root)) {
            super.onBackPressed();
        } else {
            // Go one level higher otherwise
            changeFolder(path.getParentFile());
            int lastVisiblePosition = 0;
            if (!lastPositionStack.empty()) {
                lastVisiblePosition = lastPositionStack.pop();
            }

            if (lastVisiblePosition >= 0) {
                mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode != RESULT_OK || intent == null) {
            //If resultCode is not Activity.RESULT_OK, means cancelled, so only finish.
            Timber.d("Result code: %s", resultCode);
            finish();
            return;
        }

        Uri uri = intent.getData();
        boolean isPrimary;

        switch (requestCode) {
            case REQUEST_SAVE_RK:
                setResult(RESULT_OK, new Intent().setData(uri));
                finish();
                break;

            case REQUEST_PICK_CU_FOLDER:
                Timber.d("Folder picked from system picker");
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                isPrimary = setPathAndCheckIfIsPrimary(uri);

                if (!isPrimary) {
                    if (pickFolderType.equals(PickFolderType.CU_FOLDER)) {
                        dbH.setCameraFolderExternalSDCard(true);
                        dbH.setUriExternalSDCard(uri.toString());
                    } else if (pickFolderType.equals(PickFolderType.MU_FOLDER)) {
                        dbH.setMediaFolderExternalSdCard(true);
                        dbH.setUriMediaExternalSdCard(uri.toString());
                    }
                }

                finishPickFolder();
                break;

            case REQUEST_PICK_DOWNLOAD_FOLDER:
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                isPrimary = setPathAndCheckIfIsPrimary(uri);

                if (!isPrimary) {
                    dbH.setSdCardUri(uri.toString());
                }

                finishPickFolder();
                break;
        }
    }

    /**
     * Sets as path the picked folder and checks if the chosen path is in the primary storage.
     *
     * @param uri The Uri to set as path.
     * @return True if the chosen path is in the primary storage, false otherwise.
     */
    private boolean setPathAndCheckIfIsPrimary(Uri uri) {
        DocumentFile documentFile = DocumentFile.fromTreeUri(this, uri);
        if (documentFile == null) {
            Timber.e("DocumentFile is null");
            finish();
            return true;
        }

        path = new File(DocumentFileUtils.getAbsolutePath(documentFile, this));
        String documentId = DocumentFileUtils.getId(documentFile);

        return documentId.equals(StorageId.PRIMARY) || documentId.contains(StorageId.PRIMARY);
    }

    public void changeActionBarElevation(boolean withElevation) {
        ColorUtils.changeStatusBarColorForElevation(this, withElevation);
        float elevation = getResources().getDimension(R.dimen.toolbar_elevation);
        tB.setElevation(withElevation ? elevation : 0);
    }

    @Override
    public void checkScroll() {
        if (listView == null) {
            return;
        }

        changeActionBarElevation(listView.canScrollVertically(SCROLLING_UP_DIRECTION));
    }
}
