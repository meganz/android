package com.mega.android;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.mega.android.FileStorageActivity.Mode;
import com.mega.android.pdfViewer.OpenPDFActivity;
import com.mega.components.EditTextCursorWatcher;
import com.mega.components.RoundedImageView;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaGlobalListenerInterface;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaTransfer;
import com.mega.sdk.MegaTransferListenerInterface;
import com.mega.sdk.MegaUser;
import com.mega.sdk.NodeList;
import com.mega.sdk.TransferList;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StatFs;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ContactFileListActivity extends PinActivity implements
		MegaRequestListenerInterface, OnItemClickListener,
		OnItemLongClickListener, OnClickListener, MegaGlobalListenerInterface, MegaTransferListenerInterface {

	MegaApiAndroid megaApi;
	ActionBar aB;
	ContactFileListActivity contactFileListActivity = this;

	String userEmail;

	TextView nameView;
	RoundedImageView imageView;
	ImageView statusDot;
	TextView textViewContent;

	RelativeLayout contactLayout;
	ListView listView;
	ImageView emptyImage;
	TextView emptyText;

	MegaUser contact;
	NodeList contactNodes;

	MegaBrowserListAdapter adapter;

	long parentHandle = -1;

	Stack<Long> parentHandleStack = new Stack<Long>();

	private ActionMode actionMode;
	private boolean moveToRubbish = false;
	private boolean isClearRubbishBin = false;
	private RubbishBinFragment rbF;

	ProgressDialog statusDialog;

	public static int REQUEST_CODE_GET = 1000;
	public static int REQUEST_CODE_GET_LOCAL = 1003;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	private static int EDIT_TEXT_ID = 2;

	private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

	public UploadHereDialog uploadDialog;

	private List<ShareInfo> filePreparedInfos;
	private AlertDialog renameDialog;

	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;

	MenuItem uploadButton;

	TransferList tL;
	HashMap<Long, MegaTransfer> mTHash = null;
	long lastTimeOnTransferUpdate = -1;
	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<MegaNode> documents = getSelectedDocuments();

			switch (item.getItemId()) {
			case R.id.cab_menu_download: {
				ArrayList<Long> handleList = new ArrayList<Long>();
				for (int i = 0; i < documents.size(); i++) {
					handleList.add(documents.get(i).getHandle());
				}
				clearSelections();
				hideMultipleSelect();
				onFileClick(handleList);
				break;
			}
			case R.id.cab_menu_rename: {
				clearSelections();
				hideMultipleSelect();
				if (documents.size() == 1) {
					// ((ManagerActivity)
					// context).showRenameDialog(documents.get(0),
					// documents.get(0).getName());
				}
				break;
			}
			case R.id.cab_menu_copy: {
				ArrayList<Long> handleList = new ArrayList<Long>();
				for (int i = 0; i < documents.size(); i++) {
					handleList.add(documents.get(i).getHandle());
				}
				clearSelections();
				hideMultipleSelect();

				showCopy(handleList);
				break;
			}
			case R.id.cab_menu_move: {
				ArrayList<Long> handleList = new ArrayList<Long>();
				for (int i = 0; i < documents.size(); i++) {
					handleList.add(documents.get(i).getHandle());
				}
				clearSelections();
				hideMultipleSelect();
				// ((ManagerActivity) context).showMove(handleList);
				break;
			}
			case R.id.cab_menu_share_link: {
				clearSelections();
				hideMultipleSelect();
				if (documents.size() == 1) {
					// ((ManagerActivity)
					// context).getPublicLinkAndShareIt(documents.get(0));
				}
				break;
			}
			case R.id.cab_menu_trash: {
				ArrayList<Long> handleList = new ArrayList<Long>();
				for (int i = 0; i < documents.size(); i++) {
					handleList.add(documents.get(i).getHandle());
				}
				clearSelections();
				hideMultipleSelect();
				// ((ManagerActivity) context).moveToTrash(handleList);
				break;
			}

			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_browser_action, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			adapter.setMultipleSelect(false);
			listView.setOnItemLongClickListener(contactFileListActivity);
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = getSelectedDocuments();
			boolean showDownload = false;
			boolean showRename = false;
			boolean showCopy = false;
			boolean showMove = false;
			boolean showLink = false;
			boolean showTrash = false;

			// Rename
			// if(selected.size() == 1){
			// if ((megaApi.checkAccess(selected.get(0), "full").getErrorCode()
			// == MegaError.API_OK) ||
			// (megaApi.checkAccess(selected.get(0), "rw").getErrorCode() ==
			// MegaError.API_OK)) {
			// showRename = true;
			// }
			// }

			if (selected.size() > 0) {
				showDownload = true;
				showCopy = true;
				showTrash = false;
				showMove = false;
			}

			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);

			return false;
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (megaApi == null) {
			megaApi = ((MegaApplication) getApplication()).getMegaApi();
		}

		megaApi.addGlobalListener(this);
		megaApi.addTransferListener(this);

		aB = getSupportActionBar();
		aB.setHomeButtonEnabled(true);
		aB.setDisplayShowTitleEnabled(true);
		aB.setLogo(R.drawable.ic_action_navigation_accept);
		aB.setTitle(getString(R.string.contact_file_list_activity));

		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			userEmail = extras.getString("name");

			setContentView(R.layout.activity_contact_file_list);
			nameView = (TextView) findViewById(R.id.contact_file_list_name);
			imageView = (RoundedImageView) findViewById(R.id.contact_file_list_thumbnail);
			statusDot = (ImageView) findViewById(R.id.contact_file_list_status_dot);
			textViewContent = (TextView) findViewById(R.id.contact_file_list_content);
			contactLayout = (RelativeLayout) findViewById(R.id.contact_file_list_contact_layout);
			contactLayout.setOnClickListener(this);

			nameView.setText(userEmail);
			contact = megaApi.getContact(userEmail);

			File avatar = null;
			if (getExternalCacheDir() != null) {
				avatar = new File(getExternalCacheDir().getAbsolutePath(),
						contact.getEmail() + ".jpg");
			} else {
				avatar = new File(getCacheDir().getAbsolutePath(),
						contact.getEmail() + ".jpg");
			}
			Bitmap imBitmap = null;
			if (avatar.exists()) {
				if (avatar.length() > 0) {
					BitmapFactory.Options bOpts = new BitmapFactory.Options();
					bOpts.inPurgeable = true;
					bOpts.inInputShareable = true;
					imBitmap = BitmapFactory.decodeFile(
							avatar.getAbsolutePath(), bOpts);
					if (imBitmap == null) {
						avatar.delete();
						if (getExternalCacheDir() != null) {
							megaApi.getUserAvatar(contact,
									getExternalCacheDir().getAbsolutePath()
											+ "/" + contact.getEmail(), this);
						} else {
							megaApi.getUserAvatar(contact,
									getCacheDir().getAbsolutePath() + "/"
											+ contact.getEmail(), this);
						}
					} else {
						imageView.setImageBitmap(imBitmap);
					}
				}
			}
			contactNodes = megaApi.getInShares(contact);
			textViewContent.setText(getDescription(contactNodes));

			listView = (ListView) findViewById(R.id.contact_file_list_view_browser);
			listView.setOnItemClickListener(this);
			listView.setOnItemLongClickListener(this);
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			listView.setItemsCanFocus(false);

			emptyImage = (ImageView) findViewById(R.id.contact_file_list_empty_image);
			emptyText = (TextView) findViewById(R.id.contact_file_list_empty_text);
			if (contactNodes.size() != 0) {
				emptyImage.setVisibility(View.GONE);
				emptyText.setVisibility(View.GONE);
				listView.setVisibility(View.VISIBLE);
			} else {
				emptyImage.setVisibility(View.VISIBLE);
				emptyText.setVisibility(View.VISIBLE);
				listView.setVisibility(View.GONE);
				emptyImage.setImageResource(R.drawable.ic_empty_folder);
				emptyText.setText(R.string.file_browser_empty_folder);
			}

			if (adapter == null) {
				adapter = new MegaBrowserListAdapter(this, contactNodes, -1,listView, emptyImage, emptyText, aB,ManagerActivity.CONTACT_FILE_ADAPTER);
				if (mTHash != null){
					adapter.setTransfers(mTHash);
				}
			} else {
				adapter.setNodes(contactNodes);
				adapter.setParentHandle(-1);
			}

			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);

			listView.setAdapter(adapter);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		megaApi.removeGlobalListener(this);
		megaApi.removeTransferListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_contact_file_list, menu);

		uploadButton = menu.findItem(R.id.action_contact_file_list_upload);
		if (parentHandleStack.isEmpty()) {
			uploadButton.setVisible(false);
		}

		// MenuItem nullItem =
		// menu.findItem(R.id.action_contact_file_list_null);
		// nullItem.setEnabled(false);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case android.R.id.home: {
			onBackPressed();
			return true;
		}
		case R.id.action_contact_file_list_upload: {
			uploadDialog = new UploadHereDialog();
			uploadDialog.show(getSupportFragmentManager(), "fragment_upload");
			return true;
		}
		default: {
			return super.onOptionsItemSelected(item);
		}
		}
	}

	public String getDescription(NodeList nodes) {
		int numFolders = 0;
		int numFiles = 0;

		for (int i = 0; i < nodes.size(); i++) {
			MegaNode c = nodes.get(i);
			if (c.isFolder()) {
				numFolders++;
			} else {
				numFiles++;
			}
		}

		String info = "";
		if (numFolders > 0) {
			info = numFolders
					+ " "
					+ getResources().getQuantityString(
							R.plurals.general_num_folders, numFolders);
			if (numFiles > 0) {
				info = info
						+ ", "
						+ numFiles
						+ " "
						+ getResources().getQuantityString(
								R.plurals.general_num_files, numFiles);
			}
		} else {
			if (numFiles == 0) {
				info = numFiles
						+ " "
						+ getResources().getQuantityString(
								R.plurals.general_num_folders, numFolders);
			} else {
				info = numFiles
						+ " "
						+ getResources().getQuantityString(
								R.plurals.general_num_files, numFiles);
			}
		}

		return info;
	}

	public void setParentHandle(long parentHandle) {
		this.parentHandle = parentHandle;
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		if (request.getType() == MegaRequest.TYPE_MOVE) {
			log("move request start");
		} else if (request.getType() == MegaRequest.TYPE_REMOVE) {
			log("remove request start");
		} else if (request.getType() == MegaRequest.TYPE_EXPORT) {
			log("export request start");
		} else if (request.getType() == MegaRequest.TYPE_RENAME) {
			log("rename request start");
		} else if (request.getType() == MegaRequest.TYPE_COPY) {
			log("copy request start");
		}

	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish");
		if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER) {
			if (e.getErrorCode() == MegaError.API_OK) {
				File avatar = null;
				if (getExternalCacheDir() != null) {
					avatar = new File(getExternalCacheDir().getAbsolutePath(),
							request.getEmail() + ".jpg");
				} else {
					avatar = new File(getCacheDir().getAbsolutePath(),
							request.getEmail() + ".jpg");
				}
				Bitmap imBitmap = null;
				if (avatar.exists()) {
					if (avatar.length() > 0) {
						BitmapFactory.Options bOpts = new BitmapFactory.Options();
						bOpts.inPurgeable = true;
						bOpts.inInputShareable = true;
						imBitmap = BitmapFactory.decodeFile(
								avatar.getAbsolutePath(), bOpts);
						if (imBitmap == null) {
							avatar.delete();
						} else {
							imageView.setImageBitmap(imBitmap);
						}
					}
				}
			}
		} else if (request.getType() == MegaRequest.TYPE_COPY) {
			try {
				statusDialog.dismiss();
			} catch (Exception ex) {
			}

			if (e.getErrorCode() == MegaError.API_OK) {
				Toast.makeText(this, "Correctly copied", Toast.LENGTH_SHORT)
						.show();
				NodeList nodes = megaApi
						.getChildren(megaApi.getNodeByHandle(parentHandle),
								orderGetChildren);
				adapter.setNodes(nodes);
				listView.invalidateViews();
				// if (fbF.isVisible()){
				// NodeList nodes =
				// megaApi.getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()),
				// orderGetChildren);
				// fbF.setNodes(nodes);
				// fbF.getListView().invalidateViews();
				// }
			} else {
				Toast.makeText(this, "The file has not been copied",
						Toast.LENGTH_LONG).show();
			}
			log("copy nodes request finished");
		} else if (request.getType() == MegaRequest.TYPE_MOVE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (moveToRubbish){
				if (e.getErrorCode() == MegaError.API_OK){
					Toast.makeText(this, "Correctly moved to Rubbish bin", Toast.LENGTH_SHORT).show();
					/*if (fbF != null){
						if (fbF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()), orderGetChildren);
							fbF.setNodes(nodes);
							fbF.getListView().invalidateViews();
						}
					}*/
					if (rbF != null){
						if (rbF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbF.getParentHandle()), orderGetChildren);
							rbF.setNodes(nodes);
							rbF.getListView().invalidateViews();
						}
					}
					/*
					if (swmF != null){
						if (swmF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(swmF.getParentHandle()), orderGetChildren);
							swmF.setNodes(nodes);
							swmF.getListView().invalidateViews();
						}
					}*/
				}
				else{
					Toast.makeText(this, "The file has not been removed", Toast.LENGTH_LONG).show();
				}
				moveToRubbish = false;
				log("move to rubbish request finished");
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					Toast.makeText(this, "Correctly moved", Toast.LENGTH_SHORT).show();
					/*if (fbF != null){
						if (fbF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()), orderGetChildren);
							fbF.setNodes(nodes);
							fbF.getListView().invalidateViews();
						}
					}*/
					if (rbF != null){
						if (rbF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(rbF.getParentHandle()), orderGetChildren);
							rbF.setNodes(nodes);
							rbF.getListView().invalidateViews();
						}
					}/*
					if (swmF != null){
						if (swmF.isVisible()){
							NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(swmF.getParentHandle()), orderGetChildren);
							swmF.setNodes(nodes);
							swmF.getListView().invalidateViews();
						}
					}*/
				}
				else{
					Toast.makeText(this, "The file has not been moved", Toast.LENGTH_LONG).show();
				}
				log("move nodes request finished");
			}
		} else if (request.getType() == MegaRequest.TYPE_RENAME){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(this, "Correctly renamed", Toast.LENGTH_SHORT).show();
			}
			else{
				Toast.makeText(this, "The file has not been renamed", Toast.LENGTH_LONG).show();
			}
			log("rename nodes request finished");
			
		} else if (request.getType() == MegaRequest.TYPE_REMOVE) {

			if (e.getErrorCode() == MegaError.API_OK) {
				if (statusDialog.isShowing()) {
					try {
						statusDialog.dismiss();
					} catch (Exception ex) {
					}
					Toast.makeText(this, "Correctly deleted from MEGA",
							Toast.LENGTH_SHORT).show();
				}
				/*
				 * if (fbF != null){ if (fbF.isVisible()){ NodeList nodes =
				 * megaApi
				 * .getChildren(megaApi.getNodeByHandle(fbF.getParentHandle()),
				 * orderGetChildren); fbF.setNodes(nodes);
				 * fbF.getListView().invalidateViews(); } }
				 */
				if (rbF != null) {

					NodeList nodes = megaApi.getChildren(
							megaApi.getNodeByHandle(rbF.getParentHandle()),
							orderGetChildren);
					rbF.setNodes(nodes);
					rbF.getListView().invalidateViews();

				}
			}  else {
				Toast.makeText(this, "The file has not been removed",
						Toast.LENGTH_LONG).show();
			}
			/*
			 * if (swmF != null){ if (swmF.isVisible()){ NodeList nodes =
			 * megaApi
			 * .getChildren(megaApi.getNodeByHandle(swmF.getParentHandle()),
			 * orderGetChildren); swmF.setNodes(nodes);
			 * swmF.getListView().invalidateViews(); } }
			 */
			log("remove request finished");
			
		}		
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError");
	}

	public static void log(String log) {
		Util.log("ContactFileListActivity", log);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (adapter.isMultipleSelect()) {
			SparseBooleanArray checkedItems = listView
					.getCheckedItemPositions();
			if (checkedItems.get(position, false) == true) {
				listView.setItemChecked(position, true);
			} else {
				listView.setItemChecked(position, false);
			}
			updateActionModeTitle();
			adapter.notifyDataSetChanged();
		} else {
			if (contactNodes.get(position).isFolder()) {
				MegaNode n = contactNodes.get(position);

				aB.setTitle(n.getName());
				aB.setLogo(R.drawable.ic_action_navigation_previous_item);
				supportInvalidateOptionsMenu();

				parentHandleStack.push(parentHandle);
				parentHandle = contactNodes.get(position).getHandle();
				adapter.setParentHandle(parentHandle);

				contactNodes = megaApi.getChildren(contactNodes.get(position));
				adapter.setNodes(contactNodes);
				listView.setSelection(0);

				// If folder has no files
				if (adapter.getCount() == 0) {
					listView.setVisibility(View.GONE);
					emptyImage.setVisibility(View.VISIBLE);
					emptyText.setVisibility(View.VISIBLE);
					emptyImage.setImageResource(R.drawable.ic_empty_folder);
					emptyText.setText(R.string.file_browser_empty_folder);
				} else {
					listView.setVisibility(View.VISIBLE);
					emptyImage.setVisibility(View.GONE);
					emptyText.setVisibility(View.GONE);
				}
			} else {
				if (MimeType.typeForName(contactNodes.get(position).getName())
						.isImage()) {
					Intent intent = new Intent(this,
							FullScreenImageViewer.class);
					intent.putExtra("position", position);
					if (megaApi.getParentNode(contactNodes.get(position))
							.getType() == MegaNode.TYPE_ROOT) {
						intent.putExtra("parentNodeHandle", -1L);
					} else {
						intent.putExtra("parentNodeHandle", megaApi
								.getParentNode(contactNodes.get(position))
								.getHandle());
					}
					startActivity(intent);
				} else if (MimeType.typeForName(
						contactNodes.get(position).getName()).isVideo()
						|| MimeType.typeForName(
								contactNodes.get(position).getName()).isAudio()) {
					MegaNode file = contactNodes.get(position);
					Intent service = new Intent(this,
							MegaStreamingService.class);
					startService(service);
					String fileName = file.getName();
					try {
						fileName = URLEncoder.encode(fileName, "UTF-8")
								.replaceAll("\\+", "%20");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}

					String url = "http://127.0.0.1:4443/"
							+ file.getBase64Handle() + "/" + fileName;
					String mimeType = MimeType.typeForName(file.getName())
							.getType();
					System.out.println("FILENAME: " + fileName);

					Intent mediaIntent = new Intent(Intent.ACTION_VIEW);
					mediaIntent.setDataAndType(Uri.parse(url), mimeType);
					try {
						startActivity(mediaIntent);
					} catch (Exception e) {
						Toast.makeText(this, "NOOOOOOOO", Toast.LENGTH_LONG)
								.show();
					}
				} else {
					adapter.setPositionClicked(-1);
					adapter.notifyDataSetChanged();
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(contactNodes.get(position).getHandle());
					onFileClick(handleList);
				}
			}
		}
	}

	@Override
	public void onBackPressed() {

		parentHandle = adapter.getParentHandle();

		if (adapter.getPositionClicked() != -1) {
			adapter.setPositionClicked(-1);
			adapter.notifyDataSetChanged();
		} else {
			if (parentHandleStack.isEmpty()) {
				super.onBackPressed();
			} else {
				parentHandle = parentHandleStack.pop();
				listView.setVisibility(View.VISIBLE);
				emptyImage.setVisibility(View.GONE);
				emptyText.setVisibility(View.GONE);
				if (parentHandle == -1) {
					contactNodes = megaApi.getInShares(contact);
					aB.setTitle(getString(R.string.contact_file_list_activity));
					aB.setLogo(R.drawable.ic_action_navigation_accept);
					supportInvalidateOptionsMenu();
					adapter.setNodes(contactNodes);
					listView.setSelection(0);
					adapter.setParentHandle(parentHandle);
				} else {
					contactNodes = megaApi.getChildren(megaApi
							.getNodeByHandle(parentHandle));
					aB.setTitle(megaApi.getNodeByHandle(parentHandle).getName());
					aB.setLogo(R.drawable.ic_action_navigation_previous_item);
					supportInvalidateOptionsMenu();
					adapter.setNodes(contactNodes);
					listView.setSelection(0);
					adapter.setParentHandle(parentHandle);
				}
			}
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {

		if (adapter.getPositionClicked() == -1) {
			clearSelections();
			actionMode = startSupportActionMode(new ActionBarCallBack());
			listView.setItemChecked(position, true);
			adapter.setMultipleSelect(true);
			updateActionModeTitle();
			listView.setOnItemLongClickListener(null);
		}
		return true;
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
		updateActionModeTitle();
	}

	private void updateActionModeTitle() {
		if (actionMode == null) {
			return;
		}
		List<MegaNode> documents = getSelectedDocuments();
		int files = 0;
		int folders = 0;
		for (MegaNode document : documents) {
			if (document.isFile()) {
				files++;
			} else if (document.isFolder()) {
				folders++;
			}
		}
		Resources res = getResources();
		String format = "%d %s";
		String filesStr = String.format(format, files,
				res.getQuantityString(R.plurals.general_num_files, files));
		String foldersStr = String.format(format, folders,
				res.getQuantityString(R.plurals.general_num_folders, folders));
		String title;
		if (files == 0 && folders == 0) {
			title = "";
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
		// actionMode.
	}

	/*
	 * Get list of all selected documents
	 */
	private List<MegaNode> getSelectedDocuments() {
		ArrayList<MegaNode> documents = new ArrayList<MegaNode>();
		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				MegaNode document = adapter
						.getDocumentAt(checkedItems.keyAt(i));
				if (document != null) {
					documents.add(document);
				}
			}
		}
		return documents;
	}

	/*
	 * Disable selection
	 */
	void hideMultipleSelect() {
		adapter.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.contact_file_list_contact_layout: {
			Intent i = new Intent(this, ContactPropertiesActivity.class);
			i.putExtra("name", contact.getEmail());
			startActivity(i);
			finish();
			break;
		}
		}
	}

	public void onFileClick(ArrayList<Long> handleList) {

		long size = 0;
		long[] hashes = new long[handleList.size()];
		for (int i = 0; i < handleList.size(); i++) {
			hashes[i] = handleList.get(i);
			size += megaApi.getNodeByHandle(hashes[i]).getSize();
		}

		if (dbH == null) {
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
//			dbH = new DatabaseHandler(getApplicationContext());
		}

		boolean askMe = true;
		String downloadLocationDefaultPath = "";
		prefs = dbH.getPreferences();
		if (prefs != null) {
			if (prefs.getStorageAskAlways() != null) {
				if (!Boolean.parseBoolean(prefs.getStorageAskAlways())) {
					if (prefs.getStorageDownloadLocation() != null) {
						if (prefs.getStorageDownloadLocation().compareTo("") != 0) {
							askMe = false;
							downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
						}
					}
				}
			}
		}

		if (askMe) {
			Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
			intent.putExtra(FileStorageActivity.EXTRA_BUTTON_PREFIX,
					getString(R.string.context_download_to));
			intent.putExtra(FileStorageActivity.EXTRA_SIZE, size);
			intent.setClass(this, FileStorageActivity.class);
			intent.putExtra(FileStorageActivity.EXTRA_DOCUMENT_HASHES, hashes);
			startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
		} else {
			downloadTo(downloadLocationDefaultPath, null, size, hashes);
		}
	}

	public void showCopy(ArrayList<Long> handleList) {

		Intent intent = new Intent(this, FileExplorerActivity.class);
		intent.setAction(FileExplorerActivity.ACTION_PICK_COPY_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i = 0; i < handleList.size(); i++) {
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("COPY_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (intent == null) {
			return;
		}

		if (requestCode == REQUEST_CODE_GET && resultCode == RESULT_OK) {
			Uri uri = intent.getData();
			intent.setAction(Intent.ACTION_GET_CONTENT);
			FilePrepareTask filePrepareTask = new FilePrepareTask(this);
			filePrepareTask.execute(intent);
			ProgressDialog temp = null;
			try {
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.upload_prepare));
				temp.show();
			} catch (Exception e) {
				return;
			}
			statusDialog = temp;
		} else if (requestCode == REQUEST_CODE_GET_LOCAL
				&& resultCode == RESULT_OK) {

			String folderPath = intent
					.getStringExtra(FileStorageActivity.EXTRA_PATH);
			ArrayList<String> paths = intent
					.getStringArrayListExtra(FileStorageActivity.EXTRA_FILES);

			int i = 0;

			MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
			if (parentNode == null) {
				parentNode = megaApi.getRootNode();
			}

			for (String path : paths) {
				Intent uploadServiceIntent = new Intent(this,
						UploadService.class);
				File file = new File(path);
				if (file.isDirectory()) {
					uploadServiceIntent.putExtra(UploadService.EXTRA_FILEPATH,
							file.getAbsolutePath());
					uploadServiceIntent.putExtra(UploadService.EXTRA_NAME,
							file.getName());
					log("FOLDER: EXTRA_FILEPATH: " + file.getAbsolutePath());
					log("FOLDER: EXTRA_NAME: " + file.getName());
				} else {
					ShareInfo info = ShareInfo.infoFromFile(file);
					if (info == null) {
						continue;
					}
					uploadServiceIntent.putExtra(UploadService.EXTRA_FILEPATH,
							info.getFileAbsolutePath());
					uploadServiceIntent.putExtra(UploadService.EXTRA_NAME,
							info.getTitle());
					uploadServiceIntent.putExtra(UploadService.EXTRA_SIZE,
							info.getSize());
					
					log("FILE: EXTRA_FILEPATH: " + info.getFileAbsolutePath());
					log("FILE: EXTRA_NAME: " + info.getTitle());
					log("FILE: EXTRA_SIZE: " + info.getSize());
				}

				uploadServiceIntent.putExtra(UploadService.EXTRA_FOLDERPATH,
						folderPath);
				uploadServiceIntent.putExtra(UploadService.EXTRA_PARENT_HASH,
						parentNode.getHandle());
				log("PARENTNODE: " + parentNode.getHandle() + "___" + parentNode.getName());
								
				startService(uploadServiceIntent);
				i++;
			}

		} else if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER
				&& resultCode == RESULT_OK) {
			log("local folder selected");
			String parentPath = intent
					.getStringExtra(FileStorageActivity.EXTRA_PATH);
			String url = intent.getStringExtra(FileStorageActivity.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivity.EXTRA_SIZE, 0);
			long[] hashes = intent
					.getLongArrayExtra(FileStorageActivity.EXTRA_DOCUMENT_HASHES);
			log("URL: " + url + "___SIZE: " + size);

			downloadTo(parentPath, url, size, hashes);
			Util.showToast(this, R.string.download_began);
		} 
		else if (requestCode == REQUEST_CODE_SELECT_COPY_FOLDER	&& resultCode == RESULT_OK) {
			if (!Util.isOnline(this)) {
				Util.showErrorAlertDialog(
						getString(R.string.error_server_connection_problem),
						false, this);
				return;
			}

			ProgressDialog temp = null;
			try {
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.context_copying));
				temp.show();
			} catch (Exception e) {
				return;
			}
			statusDialog = temp;

			final long[] copyHandles = intent.getLongArrayExtra("COPY_HANDLES");
			final long toHandle = intent.getLongExtra("COPY_TO", 0);
			final int totalCopy = copyHandles.length;

			MegaNode parent = megaApi.getNodeByHandle(toHandle);
			for (int i = 0; i < copyHandles.length; i++) {
				log("NODO A COPIAR: " + megaApi.getNodeByHandle(copyHandles[i]).getName());
				log("DONDE: " + parent.getName());
				log("NODOS: " + copyHandles[i] + "_" + parent.getHandle());
				megaApi.copyNode(megaApi.getNodeByHandle(copyHandles[i]), parent, this);
			}
		}
	}

	/*
	 * Background task to process files for uploading
	 */
	private class FilePrepareTask extends
			AsyncTask<Intent, Void, List<ShareInfo>> {
		Context context;

		FilePrepareTask(Context context) {
			this.context = context;
		}

		@Override
		protected List<ShareInfo> doInBackground(Intent... params) {
			return ShareInfo.processIntent(params[0], context);
		}

		@Override
		protected void onPostExecute(List<ShareInfo> info) {
			filePreparedInfos = info;
			onIntentProcessed();
		}
	}

	/*
	 * Handle processed upload intent
	 */
	public void onIntentProcessed() {
		List<ShareInfo> infos = filePreparedInfos;
		if (statusDialog != null) {
			try {
				statusDialog.dismiss();
			} catch (Exception ex) {
			}
		}

		MegaNode parentNode = megaApi.getNodeByHandle(parentHandle);
		if (parentNode == null) {
			Util.showErrorAlertDialog(
					getString(R.string.error_temporary_unavaible), false, this);
			return;
		}

		if (infos == null) {
			Util.showErrorAlertDialog(getString(R.string.upload_can_not_open),
					false, this);
		} else {
			Toast.makeText(getApplicationContext(),
					getString(R.string.upload_began), Toast.LENGTH_SHORT)
					.show();
			for (ShareInfo info : infos) {
				Intent intent = new Intent(this, UploadService.class);
				intent.putExtra(UploadService.EXTRA_FILEPATH,
						info.getFileAbsolutePath());
				intent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
				intent.putExtra(UploadService.EXTRA_PARENT_HASH,
						parentNode.getHandle());
				intent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
				startService(intent);
			}
		}
	}

	/*
	 * If there is an application that can manage the Intent, returns true.
	 * Otherwise, false.
	 */
	public static boolean isIntentAvailable(Context ctx, Intent intent) {

		final PackageManager mgr = ctx.getPackageManager();
		List<ResolveInfo> list = mgr.queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	/*
	 * Get list of all child files
	 */
	private void getDlList(Map<MegaNode, String> dlFiles, MegaNode parent,
			File folder) {

		if (megaApi.getRootNode() == null)
			return;

		folder.mkdir();
		NodeList nodeList = megaApi.getChildren(parent, orderGetChildren);
		for (int i = 0; i < nodeList.size(); i++) {
			MegaNode document = nodeList.get(i);
			if (document.getType() == MegaNode.TYPE_FOLDER) {
				File subfolder = new File(folder,
						new String(document.getName()));
				getDlList(dlFiles, document, subfolder);
			} else {
				dlFiles.put(document, folder.getAbsolutePath());
			}
		}
	}

	@Override
	public void onUsersUpdate(MegaApiJava api) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onNodesUpdate(MegaApiJava api) {
		if (megaApi.getNodeByHandle(parentHandle) == null){
			parentHandle = -1;
			contactNodes = megaApi.getInShares(contact);
			aB.setTitle(getString(R.string.contact_file_list_activity));
			aB.setLogo(R.drawable.ic_action_navigation_accept);
			supportInvalidateOptionsMenu();
			adapter.setNodes(contactNodes);
			listView.setSelection(0);
			adapter.setParentHandle(parentHandle);
			parentHandleStack.clear();
			listView.setVisibility(View.VISIBLE);
			emptyImage.setVisibility(View.GONE);
			emptyText.setVisibility(View.GONE);
		}
		else{		
			NodeList nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle), orderGetChildren);
			log("----num nodes:" + nodes.size());
			log("----parentHandle: "+parentHandle + "___" + megaApi.getNodeByHandle(parentHandle).getName());
			
			if (nodes.size() == 0) {
				listView.setVisibility(View.GONE);
				emptyImage.setVisibility(View.VISIBLE);
				emptyText.setVisibility(View.VISIBLE);
				emptyImage.setImageResource(R.drawable.ic_empty_folder);
				emptyText.setText(R.string.file_browser_empty_folder);
			} else {
				listView.setVisibility(View.VISIBLE);
				emptyImage.setVisibility(View.GONE);
				emptyText.setVisibility(View.GONE);
			}
			this.contactNodes = nodes;
			if (adapter != null) {
				adapter.setNodes(nodes);
			}
			listView.invalidateViews();
		}
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub

	}

	public void downloadTo(String parentPath, String url, long size,
			long[] hashes) {
		double availableFreeSpace = Double.MAX_VALUE;
		try {
			StatFs stat = new StatFs(parentPath);
			availableFreeSpace = (double) stat.getAvailableBlocks()
					* (double) stat.getBlockSize();
		} catch (Exception ex) {
		}

		if (hashes == null) {
			if (url != null) {
				if (availableFreeSpace < size) {
					Util.showErrorAlertDialog(
							getString(R.string.error_not_enough_free_space),
							false, this);
					return;
				}

				Intent service = new Intent(this, DownloadService.class);
				service.putExtra(DownloadService.EXTRA_URL, url);
				service.putExtra(DownloadService.EXTRA_SIZE, size);
				service.putExtra(DownloadService.EXTRA_PATH, parentPath);
				service.putExtra(DownloadService.EXTRA_CONTACT_ACTIVITY, true);
				startService(service);
			}
		} else {
			if (hashes.length == 1) {
				MegaNode tempNode = megaApi.getNodeByHandle(hashes[0]);
				if ((tempNode != null)
						&& tempNode.getType() == MegaNode.TYPE_FILE) {
					log("ISFILE");
					String localPath = Util.getLocalFile(this,
							tempNode.getName(), tempNode.getSize(), parentPath);
					if (localPath != null) {
						try {
							Util.copyFile(new File(localPath), new File(
									parentPath, tempNode.getName()));
						} catch (Exception e) {
						}

						Intent viewIntent = new Intent(Intent.ACTION_VIEW);
						viewIntent.setDataAndType(Uri.fromFile(new File(
								localPath)),
								MimeType.typeForName(tempNode.getName())
										.getType());
						if (isIntentAvailable(this, viewIntent))
							startActivity(viewIntent);
						else {
							Intent intentShare = new Intent(Intent.ACTION_SEND);
							intentShare.setDataAndType(Uri.fromFile(new File(
									localPath)),
									MimeType.typeForName(tempNode.getName())
											.getType());
							if (isIntentAvailable(this, intentShare))
								startActivity(intentShare);
							String toastMessage = getString(R.string.already_downloaded)
									+ ": " + localPath;
							Toast.makeText(this, toastMessage,
									Toast.LENGTH_LONG).show();
						}
						return;
					}
				}
			}

			for (long hash : hashes) {
				MegaNode node = megaApi.getNodeByHandle(hash);
				if (node != null) {
					Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
					if (node.getType() == MegaNode.TYPE_FOLDER) {
						getDlList(dlFiles, node, new File(parentPath,
								new String(node.getName())));
					} else {
						dlFiles.put(node, parentPath);
					}

					for (MegaNode document : dlFiles.keySet()) {

						String path = dlFiles.get(document);

						if (availableFreeSpace < document.getSize()) {
							Util.showErrorAlertDialog(
									getString(R.string.error_not_enough_free_space)
											+ " ("
											+ new String(document.getName())
											+ ")", false, this);
							continue;
						}

						Intent service = new Intent(this, DownloadService.class);
						service.putExtra(DownloadService.EXTRA_HASH,
								document.getHandle());
						service.putExtra(DownloadService.EXTRA_URL, url);
						service.putExtra(DownloadService.EXTRA_SIZE,
								document.getSize());
						service.putExtra(DownloadService.EXTRA_PATH, path);
						startService(service);
					}
				} else if (url != null) {
					if (availableFreeSpace < size) {
						Util.showErrorAlertDialog(
								getString(R.string.error_not_enough_free_space),
								false, this);
						continue;
					}

					Intent service = new Intent(this, DownloadService.class);
					service.putExtra(DownloadService.EXTRA_HASH, hash);
					service.putExtra(DownloadService.EXTRA_URL, url);
					service.putExtra(DownloadService.EXTRA_SIZE, size);
					service.putExtra(DownloadService.EXTRA_PATH, parentPath);
					startService(service);
				} else {
					log("node not found");
				}
			}
		}
	}
	
	@Override
	protected void onResume() {
    	log("onResume");
    	super.onResume();
    	
    	Intent intent = getIntent(); 
    	
    	if (intent != null) {    	
    		if (intent.getAction() != null){ 
    			if(getIntent().getAction().equals(ManagerActivity.ACTION_OPEN_PDF)){ 
    				String pathPdf=intent.getExtras().getString(ManagerActivity.EXTRA_PATH_PDF);
    				
    				File pdfFile = new File(pathPdf);
    			    
    			    Intent intentPdf = new Intent();
    			    intentPdf.setDataAndType(Uri.fromFile(pdfFile), "application/pdf");
    			    intentPdf.setClass(this, OpenPDFActivity.class);
    			    intentPdf.setAction("android.intent.action.VIEW");
    				this.startActivity(intentPdf);
    			}
    		}
    	}
    	
	}

	// Add, it is repetitive code... pending with Jesus: organize!

	private void rename(MegaNode document, String newName) {
		if (newName.equals(document.getName())) {
			return;
		}

		if (!Util.isOnline(this)) {
			Util.showErrorAlertDialog(
					getString(R.string.error_server_connection_problem), false,
					this);
			return;
		}

		if (isFinishing()) {
			return;
		}

		ProgressDialog temp = null;
		try {
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.context_renaming));
			temp.show();
		} catch (Exception e) {
			return;
		}
		statusDialog = temp;

		log("renaming " + document.getName() + " to " + newName);

		megaApi.renameNode(document, newName, this);
	}

	public void showRenameDialog(final MegaNode document, String text) {

		final EditTextCursorWatcher input = new EditTextCursorWatcher(this);
		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);

		input.setImeActionLabel(getString(R.string.context_rename),
				KeyEvent.KEYCODE_ENTER);
		input.setText(text);
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(final View v, boolean hasFocus) {
				if (hasFocus) {
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
								lastSelectedPos--; // The last point should not
													// be selected)
								input.setSelection(0, lastSelectedPos);
							}
						}
						// showKeyboardDelayed(v);
					}
				}
			}
		});

		AlertDialog.Builder builder = Util.getCustomAlertBuilder(
				this,
				getString(R.string.context_rename) + " "
						+ new String(document.getName()), null, input);
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

	public void moveToTrash(ArrayList<Long> handleList) {

		isClearRubbishBin = false;

		if (!Util.isOnline(this)) {
			Util.showErrorAlertDialog(
					getString(R.string.error_server_connection_problem), false,
					this);
			return;
		}

		if (isFinishing()) {
			return;
		}

		MegaNode rubbishNode = megaApi.getRubbishNode();

		for (int i = 0; i < handleList.size(); i++) {
			// Check if the node is not yet in the rubbish bin (if so, remove
			// it)
			MegaNode parent = megaApi.getNodeByHandle(handleList.get(i));
			while (megaApi.getParentNode(parent) != null) {
				parent = megaApi.getParentNode(parent);
			}

			if (parent.getHandle() != megaApi.getRubbishNode().getHandle()) {
				moveToRubbish = true;
				megaApi.moveNode(megaApi.getNodeByHandle(handleList.get(i)),
						rubbishNode, this);
			} else {
				megaApi.remove(megaApi.getNodeByHandle(handleList.get(i)), this);
			}
		}

		if (moveToRubbish) {
			ProgressDialog temp = null;
			try {
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.context_move_to_trash));
				temp.show();
			} catch (Exception e) {
				return;
			}
			statusDialog = temp;
		} else {
			ProgressDialog temp = null;
			try {
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.context_delete_from_mega));
				temp.show();
			} catch (Exception e) {
				return;
			}
			statusDialog = temp;
		}
	}
	
	public void showMove(ArrayList<Long> handleList){
		
		Intent intent = new Intent(this, FileExplorerActivity.class);
		intent.setAction(FileExplorerActivity.ACTION_PICK_MOVE_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("MOVE_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
	}
	
	public void setTransfers(HashMap<Long, MegaTransfer> _mTHash){
		this.mTHash = _mTHash;
		
		if (adapter != null){
			adapter.setTransfers(mTHash);
		}
	}
	
	public void setCurrentTransfer(MegaTransfer mT){
		if (adapter != null){
			adapter.setCurrentTransfer(mT);
		}
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		log("onTransferStart");
		
		HashMap<Long, MegaTransfer> mTHashLocal = new HashMap<Long, MegaTransfer>();

		tL = megaApi.getTransfers();

		for(int i=0; i<tL.size(); i++){
			
			MegaTransfer tempT = tL.get(i).copy();
			if (tempT.getType() == MegaTransfer.TYPE_DOWNLOAD){
				long handleT = tempT.getNodeHandle();
				MegaNode nodeT = megaApi.getNodeByHandle(handleT);
				MegaNode parentT = megaApi.getParentNode(nodeT);
				
				if (parentT != null){
					if(parentT.getHandle() == this.parentHandle){	
						mTHashLocal.put(handleT,tempT);						
					}
				}
			}
		}
		
		setTransfers(mTHashLocal);
		
		log("onTransferStart: " + transfer.getFileName() + " - " + transfer.getTag());
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
			MegaError e) {
		log("onTransferFinish");
		
		HashMap<Long, MegaTransfer> mTHashLocal = new HashMap<Long, MegaTransfer>();

		tL = megaApi.getTransfers();

		for(int i=0; i<tL.size(); i++){
			
			MegaTransfer tempT = tL.get(i).copy();
			if (tempT.getType() == MegaTransfer.TYPE_DOWNLOAD){
				long handleT = tempT.getNodeHandle();
				MegaNode nodeT = megaApi.getNodeByHandle(handleT);
				MegaNode parentT = megaApi.getParentNode(nodeT);
				
				if (parentT != null){
					if(parentT.getHandle() == this.parentHandle){	
						mTHashLocal.put(handleT,tempT);						
					}
				}
			}
		}
		
		setTransfers(mTHashLocal);
		
		log("onTransferFinish: " + transfer.getFileName() + " - " + transfer.getTag());
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
		log("onTransferUpdate");
		
		if (mTHash == null){
			HashMap<Long, MegaTransfer> mTHashLocal = new HashMap<Long, MegaTransfer>();

			tL = megaApi.getTransfers();

			for(int i=0; i<tL.size(); i++){
				
				MegaTransfer tempT = tL.get(i).copy();
				if (tempT.getType() == MegaTransfer.TYPE_DOWNLOAD){
					long handleT = tempT.getNodeHandle();
					MegaNode nodeT = megaApi.getNodeByHandle(handleT);
					MegaNode parentT = megaApi.getParentNode(nodeT);
					
					if (parentT != null){
						if(parentT.getHandle() == this.parentHandle){	
							mTHashLocal.put(handleT,tempT);						
						}
					}
				}
			}
			
			setTransfers(mTHashLocal);
		}

		if (transfer.getType() == MegaTransfer.TYPE_DOWNLOAD){
			Time now = new Time();
			now.setToNow();
			long nowMillis = now.toMillis(false);
			if (lastTimeOnTransferUpdate < 0){
				lastTimeOnTransferUpdate = now.toMillis(false);
				setCurrentTransfer(transfer);
			}
			else if ((nowMillis - lastTimeOnTransferUpdate) > Util.ONTRANSFERUPDATE_REFRESH_MILLIS){
				lastTimeOnTransferUpdate = nowMillis;
				setCurrentTransfer(transfer);
			}			
		}		
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {
		log("onTransferTemporaryError");
	}

	@Override
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer,
			byte[] buffer) {
		log("onTransferData");
		return true;
	}
}
