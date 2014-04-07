package com.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

import com.mega.components.RoundedImageView;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaUser;
import com.mega.sdk.NodeList;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ContactFileListActivity extends ActionBarActivity implements MegaRequestListenerInterface, OnItemClickListener, OnItemLongClickListener {

	MegaApiAndroid megaApi;
	ActionBar aB;
	
	String userEmail;
	
	TextView nameView;
	RoundedImageView imageView;
	ImageView statusDot;
	TextView textViewContent;
	
	ListView listView;
	ImageView emptyImage;
	TextView emptyText;
	
	MegaUser contact;
	NodeList contactNodes;
	
	MegaBrowserListAdapter adapter;
	
	long parentHandle = -1;
	
	Stack<Long> parentHandleStack = new Stack<Long>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (megaApi == null){
			megaApi = ((MegaApplication) getApplication()).getMegaApi();
		}
		
		aB = getSupportActionBar();
		aB.setHomeButtonEnabled(true);
		aB.setDisplayShowTitleEnabled(true);
		aB.setLogo(R.drawable.ic_action_navigation_accept);
		aB.setTitle(getString(R.string.contact_file_list_activity));
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
	    
	    Bundle extras = getIntent().getExtras();
		if (extras != null){
			userEmail = extras.getString("name");
			
			setContentView(R.layout.activity_contact_file_list);
			nameView = (TextView) findViewById(R.id.contact_file_list_name);
			imageView = (RoundedImageView) findViewById(R.id.contact_file_list_thumbnail);
			statusDot = (ImageView) findViewById(R.id.contact_file_list_status_dot);
			textViewContent = (TextView) findViewById(R.id.contact_file_list_content);
			
			nameView.setText(userEmail);
			contact = megaApi.getContact(userEmail);
			
			File avatar = new File(getCacheDir().getAbsolutePath(), contact.getEmail() + ".jpg");
			Bitmap imBitmap = null;
			if (avatar.exists()){
				if (avatar.length() > 0){
					BitmapFactory.Options bOpts = new BitmapFactory.Options();
					bOpts.inPurgeable = true;
					bOpts.inInputShareable = true;
					imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
					if (imBitmap == null) {
						avatar.delete();
						megaApi.getUserAvatar(contact, getCacheDir().getAbsolutePath() + "/" + contact.getEmail(), this);
					}
					else{
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
			if (contactNodes.size() != 0){
				emptyImage.setVisibility(View.GONE);
				emptyText.setVisibility(View.GONE);
				listView.setVisibility(View.VISIBLE);
			}			
			else{
				emptyImage.setVisibility(View.VISIBLE);
				emptyText.setVisibility(View.VISIBLE);
				listView.setVisibility(View.GONE);
				emptyImage.setImageResource(R.drawable.ic_empty_folder);
				emptyText.setText(R.string.file_browser_empty_folder);
			}
			
			if (adapter == null){
				adapter = new MegaBrowserListAdapter(this, contactNodes, -1, listView, emptyImage, emptyText, aB, true);
			}
			else{
				adapter.setNodes(contactNodes);
				adapter.setParentHandle(-1);
			}
			
			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);
			
			listView.setAdapter(adapter);
		}
	}
	
	public String getDescription(NodeList nodes){
		int numFolders = 0;
		int numFiles = 0;
		
		for (int i=0;i<nodes.size();i++){
			MegaNode c = nodes.get(i);
			if (c.isFolder()){
				numFolders++;
			}
			else{
				numFiles++;
			}
		}
		
		String info = "";
		if (numFolders > 0){
			info = numFolders +  " " + getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
			if (numFiles > 0){
				info = info + ", " + numFiles + " " + getResources().getQuantityString(R.plurals.general_num_files, numFiles);
			}
		}
		else {
			if (numFiles == 0){
				info = numFiles +  " " + getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
			}
			else{
				info = numFiles +  " " + getResources().getQuantityString(R.plurals.general_num_files, numFiles);
			}
		}
		
		return info;
	}
	
	public void setParentHandle (long parentHandle){
		this.parentHandle = parentHandle;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    // Respond to the action bar's Up/Home button
		    case android.R.id.home:{
		    	onBackPressed();
		    	return true;
		    }
		}	    
	    return super.onOptionsItemSelected(item);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart");
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish");
		if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER){
			if (e.getErrorCode() == MegaError.API_OK){
				File avatar = new File(getCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
				Bitmap imBitmap = null;
				if (avatar.exists()){
					if (avatar.length() > 0){
						BitmapFactory.Options bOpts = new BitmapFactory.Options();
						bOpts.inPurgeable = true;
						bOpts.inInputShareable = true;
						imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
						if (imBitmap == null) {
							avatar.delete();
						}
						else{
							imageView.setImageBitmap(imBitmap);
						}
					}
				}
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError");
	}
	
	private static void log(String log) {
		Util.log("ContactFileListActivity", log);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (adapter.isMultipleSelect()){
			
		}
		else{
			if (contactNodes.get(position).isFolder()){
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
				
				//If folder has no files
				if (adapter.getCount() == 0){
					listView.setVisibility(View.GONE);
					emptyImage.setVisibility(View.VISIBLE);
					emptyText.setVisibility(View.VISIBLE);
					emptyImage.setImageResource(R.drawable.ic_empty_folder);
					emptyText.setText(R.string.file_browser_empty_folder);
				}
				else{
					listView.setVisibility(View.VISIBLE);
					emptyImage.setVisibility(View.GONE);
					emptyText.setVisibility(View.GONE);
				}
			}
			else{
				if (MimeType.typeForName(contactNodes.get(position).getName()).isImage()){
					Intent intent = new Intent(this, FullScreenImageViewer.class);
					intent.putExtra("position", position);
					if (megaApi.getParentNode(contactNodes.get(position)).getType() == MegaNode.TYPE_ROOT){
						intent.putExtra("parentNodeHandle", -1L);
					}
					else{
						intent.putExtra("parentNodeHandle", megaApi.getParentNode(contactNodes.get(position)).getHandle());
					}
					startActivity(intent);
				}
				else{
					adapter.setPositionClicked(-1);
					adapter.notifyDataSetChanged();
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(contactNodes.get(position).getHandle());
//					((ManagerActivity) context).onFileClick(handleList);
				}
			}
		}
	}

	@Override
	public void onBackPressed() {
		
		parentHandle = adapter.getParentHandle();
		
		if (adapter.getPositionClicked() != -1){
			adapter.setPositionClicked(-1);
			adapter.notifyDataSetChanged();
		}
		else{
			if (parentHandleStack.isEmpty()){
				super.onBackPressed();
			}
			else{
				parentHandle = parentHandleStack.pop();
				if (parentHandle == -1){
					contactNodes = megaApi.getInShares(contact);
					aB.setTitle(getString(R.string.contact_file_list_activity));
					aB.setLogo(R.drawable.ic_action_navigation_accept);
					supportInvalidateOptionsMenu();
					adapter.setNodes(contactNodes);
					listView.setSelection(0);
					adapter.setParentHandle(parentHandle);
				}
				else{
					contactNodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle));
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
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub
		return false;
	}
}
