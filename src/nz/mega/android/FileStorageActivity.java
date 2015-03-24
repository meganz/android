package nz.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import nz.mega.android.FileStorageAdapter.OnItemCheckClickListener;
import nz.mega.android.utils.Util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;


public class FileStorageActivity extends PinActivity implements OnClickListener, OnItemClickListener, OnItemCheckClickListener {
	
	public static String EXTRA_URL = "fileurl";
	public static String EXTRA_SIZE = "filesize";
	public static String EXTRA_DOCUMENT_HASHES = "document_hash";
	public static String EXTRA_BUTTON_PREFIX = "button_prefix";
	public static String EXTRA_PATH = "filepath";
	public static String EXTRA_FILES = "fileslist";
	
	// Pick modes
	public enum Mode {
		// Select single folder
		PICK_FOLDER("ACTION_PICK_FOLDER"),
		// Pick one or multiple files or folders
		PICK_FILE("ACTION_PICK_FILE");

		private String action;

		Mode(String action) {
			this.action = action;
		}

		public String getAction() {
			return action;
		}

		public static Mode getFromIntent(Intent intent) {
			if (intent.getAction().equals(PICK_FILE.getAction())) {
				return PICK_FILE;
			} else {
				return PICK_FOLDER;
			}
		}
	}
	MegaPreferences prefs;
	DatabaseHandler dbH;
	private Mode mode;
	
	private File path;
	private File root;
	
	private String buttonPrefix;
	
	private TextView windowTitle;
	private Button button;
	private ListView listView;
	private ImageButton createFolderButton;
	
	private String url;
	private long size;
	private long[] documentHashes;
	
	FileStorageAdapter adapter;
	
	private AlertDialog createFolderDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.activity_filestorage);
		
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("path")) {
				path = new File(savedInstanceState.getString("path"));
			}
		}
		
		windowTitle = (TextView) findViewById(R.id.file_storage_window_title);
		listView = (ListView) findViewById(R.id.file_storage_list_view);
		button = (Button) findViewById(R.id.file_storage_button);
		createFolderButton = (ImageButton) findViewById(R.id.file_storage_create_folder);
		
		Intent intent = getIntent();
		buttonPrefix = intent.getStringExtra(EXTRA_BUTTON_PREFIX);
		if (buttonPrefix == null) {
			buttonPrefix = "";
		}
		mode = Mode.getFromIntent(intent);
		if (mode == Mode.PICK_FILE) {
			button.setVisibility(View.GONE);
		} else if (mode == Mode.PICK_FOLDER) {
			documentHashes = intent.getExtras().getLongArray(EXTRA_DOCUMENT_HASHES);
			url = intent.getExtras().getString(EXTRA_URL);
			size = intent.getExtras().getLong(EXTRA_SIZE);
		}
		
		button.setOnClickListener(this);
		createFolderButton.setOnClickListener(this);
		
		adapter = new FileStorageAdapter(this, mode);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setItemsCanFocus(false);

		adapter.setOnItemCheckClickListener(this);
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		
//		dbH.setLastUploadFolder("/storage/emulated/0/Pictures");		
		
		prefs = dbH.getPreferences();
		if (prefs == null){
			path = new File(Environment.getExternalStorageDirectory().toString());
		}
		else{
			String lastFolder = prefs.getLastFolderUpload();
			if(lastFolder!=null){
				path = new File(prefs.getLastFolderUpload());
				if(!path.exists())
				{
					path = null;
				}
			}
			else{
				path = new File(Environment.getExternalStorageDirectory().toString());
			}
			
		}		
		if (path == null) {
			path = new File(Environment.getExternalStorageDirectory().toString());
		}
		root = new File("/");
		changeFolder(path);
	}
	
	@Override
	public void onSaveInstanceState(Bundle state) {
		state.putString("path", path.getAbsolutePath());
		super.onSaveInstanceState(state);
	}
	
	/*
	 * Open new folder
	 * @param newPath New folder path
	 */
	private void changeFolder(File newPath) {
		setFiles(newPath);
		path = newPath;
		windowTitle.setText(Util.makeBold(path.getAbsolutePath(), path.getName()));
		if (path.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath())){
			button.setText(buttonPrefix + " SD Card");
		}
		else{
			button.setText(buttonPrefix + " " + path.getName());
		}
		if (mode == Mode.PICK_FOLDER) {
			boolean writable = newPath.canWrite();
			button.setEnabled(writable);
			LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)createFolderButton.getLayoutParams();
			if (writable) {
				createFolderButton.setVisibility(View.VISIBLE);
				params.width = LinearLayout.LayoutParams.WRAP_CONTENT;
			} else {
				createFolderButton.setVisibility(View.INVISIBLE);
				params.width = 1;
			}
			createFolderButton.setLayoutParams(params);
		}
		if (mode == Mode.PICK_FILE) {
			clearSelections();
		}
	}
	
	/*
	 * Update file list for new folder
	 */
	private void setFiles(File path) {
		List<FileDocument> documents = new ArrayList<FileDocument>();
		if (!path.canRead()) {
			Util.showErrorAlertDialog(getString(R.string.error_io_problem),
					true, this);
			return;
		}
		File[] files = path.listFiles();
		if(files != null)
		{
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
		listView.setSelectionAfterHeaderView();
	}
	
	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				int checkedPosition = checkedItems.keyAt(i);
				listView.setItemChecked(checkedPosition, false);
			}
		}
		hideButton();
	}
	
	/*
	 * Hide bottom action button
	 */
	private void hideButton() {
		button.setVisibility(View.GONE);
	}
	
	/*
	 * Show bottom action button
	 */
	private void showButton() {
		button.setVisibility(View.VISIBLE);
	}
	
	/*
	 * File system document representation
	 */
	public static class FileDocument {
		private File file;
		private MimeTypeList mimeType;

		public FileDocument(File file) {
			this.file = file;
		}

		public File getFile() {
			return file;
		}

		public boolean isHidden() {
			return getName().startsWith(".");
		}

		public boolean isFolder() {
			return file.isDirectory();
		}

		public long getSize() {
			return file.length();
		}

		public long getTimestampInMillis() {
			return file.lastModified();
		}

		public String getName() {
			return file.getName();
		}

		public MimeTypeList getMimeType() {
			if (mimeType == null) {
				mimeType = MimeTypeList.typeForName(getName());
			}
			return mimeType;
		}
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

	@Override
	public void onClick(View v) {		
		log("onClick");
		switch (v.getId()) {
			case R.id.file_storage_button:{
//				log("onClick: "+path.getAbsolutePath());
				dbH.setLastUploadFolder(path.getAbsolutePath());
				if (mode == Mode.PICK_FOLDER) {
					Intent intent = new Intent();
					intent.putExtra(EXTRA_PATH, path.getAbsolutePath());
					intent.putExtra(EXTRA_DOCUMENT_HASHES, documentHashes);
					intent.putExtra(EXTRA_URL, url);
					intent.putExtra(EXTRA_SIZE, size);
					setResult(RESULT_OK, intent);
					finish();
				}
				else {
					new AsyncTask<Void, Void, Void>()
					{
						ArrayList<String> files = new ArrayList<String>();

						@Override
						protected Void doInBackground(Void... params) {
							SparseBooleanArray checkedItems = listView
									.getCheckedItemPositions();
							for (int i = 0; i < checkedItems.size(); i++) {
								if (checkedItems.valueAt(i) == true) {
									FileDocument document = adapter.getDocumentAt(checkedItems
											.keyAt(i));
									File file = document.getFile();
									files.add(file.getAbsolutePath());
								}
							}
							return null;	
						}
						
						@Override
						public void onPostExecute(Void a)
						{
							setResultFiles(files);	
						}
					}.execute();			
				}
				break;
			}
			case R.id.file_storage_create_folder:{
				onNewFolderClick();
				break;
			}
		}
		
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ( keyCode == KeyEvent.KEYCODE_MENU ) {
	        // do nothing
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	} 

	@Override
	public void onItemCheckClick(int position) {
		boolean isChecked = listView.isItemChecked(position);
		listView.setItemChecked(position, !isChecked);
		updateButton();
	}
	
	// Update bottom button text and state
	private void updateButton() {
		int folders = 0;
		int files = 0;
		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
		FileDocument document;
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) != true) {
				continue;
			}
			int position = checkedItems.keyAt(i);
			document = adapter.getDocumentAt(position);
			if (document.getFile().isDirectory()) {
				log(document.getFile().getAbsolutePath() + " of file");
				folders++;
			} else {
				files++;
			}
		}
		
		if (files > 0 || folders > 0) {
			String filesString = files + " " + getResources().getQuantityString(R.plurals.general_num_files, files);

			String foldersString = folders + " " + getResources().getQuantityString(R.plurals.general_num_folders, folders);

			String buttonText = getString(R.string.general_upload) + " ";

			if (files == 0) {
				buttonText += foldersString;
			} else if (folders == 0) {
				buttonText += filesString;
			} else {
				buttonText += foldersString + ", " + filesString;
			}
			button.setText(buttonText);
			showButton();
		} 
		else {
			hideButton();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		log("on item click");		
		FileDocument document = adapter.getDocumentAt(position);
		if (document.isFolder()) {
			changeFolder(document.getFile());
		}
		else if (mode == Mode.PICK_FILE) {
			// Select file if mode is PICK_FILE
			ArrayList<String> files = new ArrayList<String>();
			files.add(document.getFile().getAbsolutePath());
			dbH.setLastUploadFolder(path.getAbsolutePath());
			setResultFiles(files);
			listView.setItemChecked(position, false);
		}
	}
	
	/*
	 * Set selected files to pass to the caller activity and finish this
	 * activity
	 */
	private void setResultFiles(ArrayList<String> files) {
		Intent intent = new Intent();
		intent.putStringArrayListExtra(EXTRA_FILES, files);
		intent.putExtra(EXTRA_PATH, path.getAbsolutePath());
		setResult(RESULT_OK, intent);
		finish();
	}
	
	/*
	 * Count all selected items
	 */
	private int getCheckedItemCount() {
		int count = 0;
		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				count++;
			}
		}
		return count;
	}
	
	@Override
	public void onBackPressed() {
		// If some items are selected, clear selection
		if (mode == Mode.PICK_FILE && getCheckedItemCount() > 0) {
			clearSelections();
			return;
		}
		// Finish activity if at the root
		if (path.equals(root)) {
			super.onBackPressed();
		// Go one level higher otherwise
		} else {
			changeFolder(path.getParentFile());
		}
	}
	
	public void onNewFolderClick(){
		log("file storage activity ne FOLDER!!");
		String text = getString(R.string.context_new_folder_name);
		final EditText input = new EditText(this);
		input.setSingleLine();
		input.setSelectAllOnFocus(true);
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						return true;
					}
					createFolder(value);
					createFolderDialog.dismiss();
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
		
		AlertDialog.Builder builder = Util.getCustomAlertBuilder(
				this, getString(R.string.menu_new_folder),
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
		createFolderDialog = builder.create();
		createFolderDialog.show();
	}
	
	/*
	 * Display keyboard
	 */
	private void showKeyboardDelayed(final View view) {
		view.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) FileStorageActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}
	
	/*
	 * Create new folder and reload file list
	 */
	private void createFolder(String value) {
		log(value + " Of value");
		File newFolder = new File(path, value);
		newFolder.mkdir();
		newFolder.setReadable(true, false);
		newFolder.setExecutable(true, false);
		setFiles(path);
	}
	
	public static void log(String message) {
		Util.log("FileStorageActivity", message);
	}

}
