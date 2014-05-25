package com.mega.android;

import java.util.ArrayList;

import com.mega.android.FileStorageActivity.Mode;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ImportDialog extends DialogFragment implements MegaRequestListenerInterface {
	
	private View innerView;

	private static MegaNode document;
	private static String url;
	private static long parentHandle;
	
	MegaApiAndroid megaApi;
	
	private ProgressDialog progress;	
	
	Activity context = null;

	public ImportDialog() {
		
	}
	
	public void setMegaApi(MegaApiAndroid megaApi) {
		this.megaApi = megaApi;
	}
	
	/*
	 * Set info about the file
	 * 
	 * @param document Importing document
	 * 
	 * @param parenthash Current parent
	 * 
	 * @param url Download URL info
	 * 
	 * @param key File key
	 * 
	 * @param at At parameter
	 */
	public void setInfo(MegaNode document, long parentHandle, String url) {
		ImportDialog.document = document;
		ImportDialog.parentHandle = parentHandle;
		ImportDialog.url = url;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		innerView = getInnerView();
		context = getActivity();
		
		AlertDialog.Builder builder = Util.getCustomAlertBuilder(getActivity(), getString(R.string.menu_download_from_link), null, innerView);
		
		builder.setNegativeButton(getString(R.string.general_import), new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				progress = new ProgressDialog(context);
				progress.setMessage(getString(R.string.general_importing));
				progress.show();
				importFile();
			}
		});
		
		builder.setPositiveButton(R.string.general_download, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
//				try { 
//					progress.dismiss(); 
//				} catch(Exception ex) {};
				
				Activity activity = getActivity();
				long[] hashes = new long[1];
				hashes[0]=document.getHandle();
				Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
				intent.putExtra(FileStorageActivity.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
				intent.setClass(activity, FileStorageActivity.class);
//				intent.putExtra(FileStorageActivity.EXTRA_DOCUMENT_HASHES, hashes);
				intent.putExtra(FileStorageActivity.EXTRA_URL, url);
				intent.putExtra(FileStorageActivity.EXTRA_SIZE, document.getSize());
				activity.startActivityForResult(intent, ManagerActivity.REQUEST_CODE_SELECT_LOCAL_FOLDER);
			}
		});
		
		AlertDialog dialog = builder.create();
		
		return dialog;
	}
	
	/*
	 * Import file
	 */
	private void importFile() {
		if(megaApi == null){
			return;
		}
		
		if((context!= null) && !Util.isOnline(context)) {	
			try{ 
				progress.dismiss(); 
			} catch(Exception ex) {};
			
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, context);
			return;
		}
		
		MegaNode target = megaApi.getNodeByHandle(parentHandle);
		if(target == null){
			target = megaApi.getRootNode();
		}
		megaApi.importPublicNode(document, target, this);
	}
	
	private View getInnerView() {
		LayoutInflater lf = getActivity().getLayoutInflater();
		View view = lf.inflate(R.layout.import_dialog, null, false);
		ImageView imageView = (ImageView) view.findViewById(R.id.file_image);
		TextView textView = (TextView) view.findViewById(R.id.file_text);
		TextView descriptionView = (TextView) view.findViewById(R.id.file_description);
		
		int resourceId = MimeType.typeForName(document.getName()).getIconResourceId();
		imageView.setImageResource(resourceId);
		textView.setText(new String(document.getName()));
		descriptionView.setText(Formatter.formatFileSize(getActivity(), document.getSize()));
		return view;
	}
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getName());		
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish");
		if (request.getType() == MegaRequest.TYPE_IMPORT_NODE){
			try{
				progress.dismiss(); 
			} catch(Exception ex){};
			context = getActivity();
		    if(context == null){
		    	return;
		    }
			if (e.getErrorCode() != MegaError.API_OK) {
				Util.showErrorAlertDialog(e, context);
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getName());
	}
	
	public static void log(String message) {
		Util.log("ImportDialog", message);
	}
}
