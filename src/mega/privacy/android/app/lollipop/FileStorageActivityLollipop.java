package mega.privacy.android.app.lollipop;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.R;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;


public class FileStorageActivityLollipop extends PinActivityLollipop implements OnClickListener, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener {
	
	public static String EXTRA_URL = "fileurl";
	public static String EXTRA_SIZE = "filesize";
	public static String EXTRA_DOCUMENT_HASHES = "document_hash";
	public static String EXTRA_FROM_SETTINGS = "from_settings";
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
	
	private MenuItem newFolderMenuItem;
	
	private File path;
	private File root;
	DisplayMetrics metrics;
	private String buttonPrefix;
	private RelativeLayout viewContainer;
//	private TextView windowTitle;
	private TextView button;
	private TextView contentText;
	private RecyclerView listView;
	RecyclerView.LayoutManager mLayoutManager;
	private TextView cancelButton;
	GestureDetectorCompat detector;
	
	private Boolean fromSettings;
	
	private String url;
	private long size;
	private long[] documentHashes;
	
	FileStorageLollipopAdapter adapter;
	Toolbar tB;
	ActionBar aB;
	
	private ActionMode actionMode;
	
	private AlertDialog newFolderDialog;
	
	public class RecyclerViewOnGestureListener extends SimpleOnGestureListener{

//		@Override
//	    public boolean onSingleTapConfirmed(MotionEvent e) {
//	        View view = listView.findChildViewUnder(e.getX(), e.getY());
//	        int position = listView.getChildPosition(view);
//
//	        // handle single tap
//	        itemClick(view, position);
//
//	        return super.onSingleTapConfirmed(e);
//	    }

	    public void onLongPress(MotionEvent e) {
	    	log("onLongPress");
	    	
			if (mode == Mode.PICK_FILE) {
		        View view = listView.findChildViewUnder(e.getX(), e.getY());
		        int position = listView.getChildPosition(view);

				adapter.setMultipleSelect(true);
			
				actionMode = startSupportActionMode(new ActionBarCallBack());			

		        itemClick(position);
	 
		        super.onLongPress(e);
			}
	    }
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		log("onOptionsItemSelected");
		// Handle presses on the action bar items
	    switch (item.getItemId()) {
		    case android.R.id.home:{
		    	onBackPressed();
		    	return true;
		    }
		    case R.id.cab_menu_create_folder:{
		    	onNewFolderClick();
		    	return true;
		    }
		    case R.id.cab_menu_select_all:{
		    	selectAll();
		    	return true;
		    }
		    case R.id.cab_menu_unselect_all:{
		    	clearSelections();
		    	return true;
		    }
		    default:{
	            return super.onOptionsItemSelected(item);
	        }
	    }
	}
	
	private class ActionBarCallBack implements ActionMode.Callback {
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			
			switch(item.getItemId()){
			case R.id.cab_menu_select_all:{
				selectAll();
				break;
			}
			case R.id.cab_menu_unselect_all:{
				clearSelections();
				break;
			}
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_storage_action, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			adapter.setMultipleSelect(false);
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<FileDocument> selected = adapter.getSelectedDocuments();
			
			if (selected.size() != 0) {				
				
				if(selected.size()==adapter.getItemCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);			
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);	
				}	
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}
			
			return false;
		}
		
	}
	
	public void selectAll(){
		log("selectAll");
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
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		log("onCreateOptionsMenuLollipop");
		
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.file_storage_action, menu);
	    getSupportActionBar().setDisplayShowCustomEnabled(true);
	    
	    newFolderMenuItem = menu.findItem(R.id.cab_menu_create_folder);
		if (mode == Mode.PICK_FOLDER) {
			newFolderMenuItem.setVisible(true);
			
		}
		else{
			newFolderMenuItem.setVisible(false);
		}
		
		if (mode == Mode.PICK_FOLDER) {
			boolean writable = path.canWrite();
			button.setEnabled(writable);
			if (writable) {				
				newFolderMenuItem.setVisible(true);
			} else {
				newFolderMenuItem.setVisible(false);
			}
		}
		else{
			newFolderMenuItem.setVisible(false);
		}
	    
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
		log("onPrepareOptionsMenu");
		if (mode == Mode.PICK_FOLDER) {
			newFolderMenuItem.setVisible(true);
			
		}
		else{
			newFolderMenuItem.setVisible(false);
		}
		
		if (mode == Mode.PICK_FOLDER) {
			boolean writable = path.canWrite();
			button.setEnabled(writable);
			if (writable) {				
				newFolderMenuItem.setVisible(true);
			} else {
				newFolderMenuItem.setVisible(false);
			}
		}
		else{
			newFolderMenuItem.setVisible(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		
		Display display = getWindowManager().getDefaultDisplay();
		
		metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		
		float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(metrics, density);
	    float scaleH = Util.getScaleH(metrics, density);
	    float scaleText;
	    if (scaleH < scaleW){
	    	scaleText = scaleH;
	    }
	    else{
	    	scaleText = scaleW;
	    }
	    
		setContentView(R.layout.activity_filestorage);
		
		detector = new GestureDetectorCompat(this, new RecyclerViewOnGestureListener());
		
		//Set toolbar
		tB = (Toolbar) findViewById(R.id.toolbar_filestorage);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
//		aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
		aB.setDisplayHomeAsUpEnabled(true);
		aB.setDisplayShowHomeEnabled(true);
		
		Intent intent = getIntent();
		fromSettings = intent.getBooleanExtra(EXTRA_FROM_SETTINGS, true);

		mode = Mode.getFromIntent(intent);
		if (mode == Mode.PICK_FOLDER) {
			documentHashes = intent.getExtras().getLongArray(EXTRA_DOCUMENT_HASHES);
			url = intent.getExtras().getString(EXTRA_URL);
			size = intent.getExtras().getLong(EXTRA_SIZE);
			aB.setTitle(getString(R.string.general_select_to_download));
		}
		else{
			aB.setTitle(getString(R.string.general_select_to_upload));
		}
		
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("path")) {
				path = new File(savedInstanceState.getString("path"));
			}
		}
		
        viewContainer = (RelativeLayout) findViewById(R.id.file_storage_container);
		contentText = (TextView) findViewById(R.id.file_storage_content_text);
		listView = (RecyclerView) findViewById(R.id.file_storage_list_view);
		
//		optionsBar = (LinearLayout) v.findViewById(R.id.options_file_storage_layout);
		cancelButton = (TextView) findViewById(R.id.file_storage_cancel_button);
		button = (TextView) findViewById(R.id.file_storage_button);
		button.setOnClickListener(this);
		android.view.ViewGroup.LayoutParams paramsb2 = button.getLayoutParams();		
		paramsb2.height = Util.scaleHeightPx(48, metrics);
		
		if(fromSettings){
			button.setText(getString(R.string.general_select).toUpperCase(Locale.getDefault()));
			paramsb2.width = Util.scaleWidthPx(73, metrics);
		}
		else{
			if (mode == Mode.PICK_FOLDER) {
				button.setText(getString(R.string.general_download).toUpperCase(Locale.getDefault()));
				paramsb2.width = Util.scaleWidthPx(95, metrics);
				
			}
			else{
				button.setText(getString(R.string.context_upload).toUpperCase(Locale.getDefault()));
				paramsb2.width = Util.scaleWidthPx(73, metrics);
			}
		}		
		
		button.setLayoutParams(paramsb2);
		//Left and Right margin
		LinearLayout.LayoutParams optionTextParams = (LinearLayout.LayoutParams)button.getLayoutParams();
		optionTextParams.setMargins(Util.scaleWidthPx(6, metrics), 0, Util.scaleWidthPx(8, metrics), 0); 
		button.setLayoutParams(optionTextParams);		
		
		cancelButton.setOnClickListener(this);		
		cancelButton.setText(getString(R.string.general_cancel).toUpperCase(Locale.getDefault()));		
		
		android.view.ViewGroup.LayoutParams paramsb1 = cancelButton.getLayoutParams();		
		paramsb1.height = Util.scaleHeightPx(48, metrics);
//		paramsb1.width = Util.scaleWidthPx(145, metrics);
		cancelButton.setLayoutParams(paramsb1);
		//Left and Right margin
		LinearLayout.LayoutParams cancelTextParams = (LinearLayout.LayoutParams)cancelButton.getLayoutParams();
		cancelTextParams.setMargins(Util.scaleWidthPx(6, metrics), 0, Util.scaleWidthPx(8, metrics), 0); 
		cancelButton.setLayoutParams(cancelTextParams);		
		
		listView = (RecyclerView) findViewById(R.id.file_storage_list_view);
		listView.addItemDecoration(new SimpleDividerItemDecoration(this));
		mLayoutManager = new LinearLayoutManager(this);
		listView.addOnItemTouchListener(this);
		listView.setLayoutManager(mLayoutManager);
		listView.setItemAnimator(new DefaultItemAnimator()); 
		
		if (adapter == null){
			
			adapter = new FileStorageLollipopAdapter(this, listView, mode);
			listView.setAdapter(adapter);
			
		}

		dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		
		root = new File("/");
		
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
		changeFolder(path);
		log("Path to show: "+path);
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
	@SuppressLint("NewApi")
	private void changeFolder(File newPath) {
		log("changeFolder: "+newPath);
		
		setFiles(newPath);
		path = newPath;
		contentText.setText(Util.makeBold(path.getAbsolutePath(), path.getName()));
//		windowTitle.setText(Util.makeBold(path.getAbsolutePath(), path.getName()));
		invalidateOptionsMenu();
		if (mode == Mode.PICK_FOLDER) {
			boolean writable = newPath.canWrite();
			button.setEnabled(writable);
		}
		else if (mode == Mode.PICK_FILE) {
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
		log("Number of files: "+files.length);
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

	}

	private void updateActionModeTitle() {
		log("updateActionModeTitle");
		if (actionMode == null) {
			log("RETURN");
			return;
		}
		
		List<FileDocument> documents = adapter.getSelectedDocuments();
		int files = 0;
		int folders = 0;
		for (FileDocument document : documents) {
			if (document.isFolder()) {
				folders++;
			}
			else{
				files++;
			}
		}
		
		Resources res = this.getResources();
		String format = "%d %s";
		String filesStr = String.format(format, files,
				res.getQuantityString(R.plurals.general_num_files, files));
		String foldersStr = String.format(format, folders,
				res.getQuantityString(R.plurals.general_num_folders, folders));
		String title;
		if (files == 0 && folders == 0) {
			title = foldersStr + ", " + filesStr;
		} else if (files == 0) {
			title = foldersStr;
		} else if (folders == 0) {
			title = filesStr;
		} else {
			title = foldersStr + ", " + filesStr;
		}
		actionMode.setTitle(title);
		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			e.printStackTrace();
			log("oninvalidate error");
		}

	}

	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		log("clearSelections");
		if(adapter.isMultipleSelect()){
			adapter.clearSelections();
		}
//		adapterList.startMultiselection();
//		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
//		for (int i = 0; i < checkedItems.size(); i++) {
//			if (checkedItems.valueAt(i) == true) {
//				int checkedPosition = checkedItems.keyAt(i);
//				listView.setItemChecked(checkedPosition, false);
//			}
//		}
		updateActionModeTitle();
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
					if(adapter.getSelectedCount()<=0){
						Snackbar.make(viewContainer, getString(R.string.error_no_selection), Snackbar.LENGTH_LONG).show();
						break;
					}
					new AsyncTask<Void, Void, Void>()
					{
						ArrayList<String> files = new ArrayList<String>();

						@Override
						protected Void doInBackground(Void... params) {
							List<FileDocument> selectedDocuments= adapter.getSelectedDocuments();
							for (int i = 0; i < selectedDocuments.size(); i++) {
								FileDocument document = selectedDocuments.get(i);
								if(document != null)
								{
									File file = document.getFile();
									if(file.isFile()){
										files.add(file.getAbsolutePath());
									}
									else{
										files.addAll(getFiles(file));
									}
									
								}
								
							}
							return null;	
						}
						
						public ArrayList<String> getFiles(File folder)
						{
							ArrayList<String> selectedFiles = new ArrayList<String>();
							File[] files= folder.listFiles();
							for (int i = 0; i < files.length; i++) {
							      if (files[i].isFile()) {
							    	  selectedFiles.add(files[i].getAbsolutePath());
							      } else if (files[i].isDirectory()) {
							    	  selectedFiles.addAll(getFiles(folder));
							      }
							
							}
							return selectedFiles;
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
			case R.id.file_storage_cancel_button:{
				finish();
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
	
	// Update bottom button text and state
	private void updateButton() {
		int folders = 0;
		int files = 0;
//		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
//		FileDocument document;
//		for (int i = 0; i < checkedItems.size(); i++) {
//			if (checkedItems.valueAt(i) != true) {
//				continue;
//			}
//			int position = checkedItems.keyAt(i);
//			document = adapter.getDocumentAt(position);
//			if(document == null)
//			{
//				continue;
//			}
//			
//			if (document.getFile().isDirectory()) {
//				log(document.getFile().getAbsolutePath() + " of file");
//				folders++;
//			} else {
//				files++;
//			}
//		}
//		
//		if (files > 0 || folders > 0) {
//			String filesString = files + " " + getResources().getQuantityString(R.plurals.general_num_files, files);
//
//			String foldersString = folders + " " + getResources().getQuantityString(R.plurals.general_num_folders, folders);
//
//			String buttonText = getString(R.string.general_upload) + " ";
//
//			if (files == 0) {
//				buttonText += foldersString;
//			} else if (folders == 0) {
//				buttonText += filesString;
//			} else {
//				buttonText += foldersString + ", " + filesString;
//			}
//			button.setText(buttonText);
//			showButton();
//		} 
//		else {
//			hideButton();
//		}
	}

	public void itemClick(int position) {
		log("itemClick: position: "+position);		
		FileDocument document = adapter.getDocumentAt(position);
		if(document == null)
		{
			return;
		}
		else{
			log("El documento es: "+document.getName());
		}
		
		if (adapter.isMultipleSelect()){
			log("MULTISELECT ON");
			adapter.toggleSelection(position);
			updateActionModeTitle();
			adapter.notifyDataSetChanged();
//			adapterList.notifyDataSetChanged();
		}
		else{
			if (document.isFolder()) {
				changeFolder(document.getFile());
			}
			else if (mode == Mode.PICK_FILE) {
				//Multiselect on to select several files if desired
				adapter.setMultipleSelect(true);				
				actionMode = startSupportActionMode(new ActionBarCallBack());
				adapter.toggleSelection(position);
				updateActionModeTitle();
				adapter.notifyDataSetChanged();
				
				// Select file if mode is PICK_FILE
//				ArrayList<String> files = new ArrayList<String>();
//				files.add(document.getFile().getAbsolutePath());
//				dbH.setLastUploadFolder(path.getAbsolutePath());
//				setResultFiles(files);
			}
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
	public int getItemCount(){
		if(adapter!=null){
			return adapter.getItemCount();
		}
		return 0;
	}
	
	/*
	 * Disable selection
	 */
	void hideMultipleSelect() {
		log("hideMultipleSelect");
		adapter.clearSelections();
		adapter.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	@Override
	public void onBackPressed() {
		log("onBackPressed");
		// If some items are selected, clear selection
		if (mode == Mode.PICK_FILE && adapter.isMultipleSelect()) {
			log("mode == Mode.PICK_FILE && getItemCount() > 0");
			hideMultipleSelect();
			return;
		}

		// Finish activity if at the root
		if (path.equals(root)) {
			log("Root: "+root);
			super.onBackPressed();
		// Go one level higher otherwise
		} else {
			changeFolder(path.getParentFile());
		}
	}
	
	@SuppressLint("NewApi")
	public void onNewFolderClick(){
		log("file storage activity ne FOLDER!!");
		
		LinearLayout layout = new LinearLayout(this);
	    layout.setOrientation(LinearLayout.VERTICAL);
	    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    params.setMargins(Util.scaleWidthPx(20, metrics), Util.scaleWidthPx(20, metrics), Util.scaleWidthPx(17, metrics), 0);

	    final EditText input = new EditText(this);
	    layout.addView(input, params);		
		
		input.setId(1);
		input.setSingleLine();
		input.setTextColor(getResources().getColor(R.color.text_secondary));
		input.setHint(getString(R.string.context_new_folder_name));
//		input.setSelectAllOnFocus(true);
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
	
	/*
	 * Display keyboard
	 */
	private void showKeyboardDelayed(final View view) {
		view.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) FileStorageActivityLollipop.this.getSystemService(Context.INPUT_METHOD_SERVICE);
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
		Util.log("FileStorageActivityLollipop", message);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(RecyclerView rV, MotionEvent e) {
		detector.onTouchEvent(e);
		return false;
	}

	@Override
	public void onRequestDisallowInterceptTouchEvent(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTouchEvent(RecyclerView arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		
	}
}
