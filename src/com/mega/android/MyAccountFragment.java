package com.mega.android;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.mega.android.ManagerActivity.DrawerItem;
import com.mega.android.utils.Util;
import com.mega.sdk.MegaAccountDetails;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mega.android.ManagerActivity.DrawerItem;
import com.mega.android.utils.Util;
import com.mega.sdk.MegaAccountDetails;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;

public class MyAccountFragment extends Fragment implements MegaRequestListenerInterface {

	private ImageView accountImageView;
	private TextView emailView;
	private TextView totalSpaceView;
	private TextView usedSpaceView;
	private TextView freeSpaceView;
	
	private Button upgradeButton;
	private Button passwordButton;
	private Button masterKeyButton;
	private Button logout;
	
	MegaApiAndroid megaApi;
	
	ProgressDialog updateProgress;
	
	long maxStorage;
	long usedStorage;
	int proLevel;
	
	Context context;
	ActionBar aB;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle savedInstanceState) {
		
		if (aB == null){
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}
		aB.setTitle(getString(R.string.section_account));
		
		//MegaApplication app = (MegaApplication)getApplication();
		
		((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
		((ManagerActivity)context).supportInvalidateOptionsMenu();
		
		super.onCreateView(inflater, group, savedInstanceState);
		View view = inflater.inflate(R.layout.activity_my_account, null);
		megaApi = ((MegaApplication)getActivity().getApplication()).getMegaApi();

		accountImageView = (ImageView) view.findViewById(R.id.my_account_image);
		emailView = (TextView) view.findViewById(R.id.my_account_email);
		totalSpaceView = (TextView) view.findViewById(R.id.my_account_total_space);
		usedSpaceView = (TextView) view.findViewById(R.id.my_account_used_space);
		freeSpaceView = (TextView) view.findViewById(R.id.my_account_free_space);
		upgradeButton = (Button) view.findViewById(R.id.my_account_upgrade);
		passwordButton = (Button) view.findViewById(R.id.my_account_password);
		masterKeyButton = (Button) view.findViewById(R.id.export_master_key);
		logout = (Button) view.findViewById(R.id.logout);
		
		String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
		log("Export in: "+path);
		File file= new File(path);
		if(file.exists()){
			masterKeyButton.setText(R.string.action_remove_master_key);	
		}
		else{
			masterKeyButton.setText(R.string.action_export_master_key);			
		}
		
		log("update");
		
		Button upgradeButton = (Button)view.findViewById(R.id.my_account_upgrade);
		upgradeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), UpgradeActivity.class);
				startActivity(intent);
			}
		});
		
		passwordButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
				startActivity(intent);				
			}
		});
		
		
		//Button logout
		logout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ManagerActivity.logout(context, (MegaApplication)((ManagerActivity)context).getApplication(), megaApi, false);
			}
		});	
		
		masterKeyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				log("onClickExportMasterKey");
				
				final String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MEGA/MEGAMasterKey.txt";
				final File f = new File(path);
				
				if(f.exists()){
					
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
					    @Override
					    public void onClick(DialogInterface dialog, int which) {
					        switch (which){
					        case DialogInterface.BUTTON_POSITIVE:
					        	f.delete();
					        	masterKeyButton.setText(R.string.action_export_master_key);		
					        	
					            break;

					        case DialogInterface.BUTTON_NEGATIVE:
					            //No button clicked
					            break;
					        }
					    }
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					builder.setMessage(R.string.remove_key_confirmation).setPositiveButton(R.string.general_yes, dialogClickListener)
					    .setNegativeButton(R.string.general_no, dialogClickListener).show();
					
				}
				else{
					
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
					    @Override
					    public void onClick(DialogInterface dialog, int which) {
					        switch (which){
					        case DialogInterface.BUTTON_POSITIVE:
					        	String key = megaApi.exportMasterKey();
								
								BufferedWriter out;         
								try {						
									
									log("Export in: "+path);
									FileWriter fileWriter= new FileWriter(path);	
									out = new BufferedWriter(fileWriter);	
									out.write(key);	
									out.close(); 
									masterKeyButton.setText(R.string.action_remove_master_key);	
									String toastMessage = getString(R.string.toast_master_key) + " " + path;
									Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();					

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

					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					builder.setMessage(R.string.export_key_confirmation).setPositiveButton(R.string.general_yes, dialogClickListener)
					    .setNegativeButton(R.string.general_no, dialogClickListener).show();		
			
				}	
			}			
		});	
		
		return view;
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((ActionBarActivity)activity).getSupportActionBar();
    }
	
	@Override
	public void onResume() {
		super.onResume();
		if(getActivity() != null)
			updateAccountInfo();
	}
	
	/*
	 * Request account information
	 */
	private void updateAccountInfo() 
	{	
		ManagerActivity activity = (ManagerActivity)getActivity();
		if(!Util.isOnline(activity)){
			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, activity);
			activity.selectDrawerItem(DrawerItem.CLOUD_DRIVE);
			activity.getSupportFragmentManager().beginTransaction().remove(this).commit();
			return;
		}
		
		updateProgress = Util.createProgress(activity, R.string.general_updating);
		updateProgress.show();
		
		megaApi.getAccountDetails(this);
	}
	
	/*
	 * Display account information
	 */
	private void displayInfo() {
		Activity activity = getActivity();
		if(activity == null) return;
		
		switch(proLevel){
			case 0:{
				accountImageView.setImageResource(R.drawable.ic_free);
				break;
			}
			case 1:{
				accountImageView.setImageResource(R.drawable.ic_pro_1);
				break;
			}
			case 2:{
				accountImageView.setImageResource(R.drawable.ic_pro_2);
				break;
			}
			case 3:{
				accountImageView.setImageResource(R.drawable.ic_pro_3);
				break;
			}
			default:{
				accountImageView.setImageResource(R.drawable.ic_free); 
				break;
			}
		}

//		DatabaseHandler dbH = new DatabaseHandler(context); 
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
		UserCredentials credentials = dbH.getCredentials();
		if(credentials != null)
			emailView.setText(credentials.getEmail());

		totalSpaceView.setText(Formatter.formatFileSize(getActivity(), maxStorage));
		usedSpaceView.setText(Formatter.formatFileSize(getActivity(), usedStorage));
		freeSpaceView.setText(Formatter.formatFileSize(getActivity(), maxStorage-usedStorage));
		if (proLevel == 0){
			upgradeButton.setVisibility(View.VISIBLE);
		} else {
			upgradeButton.setVisibility(View.GONE);
		}
	}
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getName());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish");
		if (request.getType() == MegaRequest.TYPE_ACCOUNT_DETAILS){
			if (e.getErrorCode() != MegaError.API_OK){
				Util.showErrorAlertDialogFinish(e, getActivity());
				return;
			}
			else{
				log("ACCOUNT OK");
			}
		}
		
		MegaAccountDetails accountInfo = request.getMegaAccountDetails();
		maxStorage = accountInfo.getStorageMax();
		usedStorage = accountInfo.getStorageUsed();
		proLevel = accountInfo.getProLevel();
		
		try { 
			updateProgress.dismiss(); 
		} catch (Exception ex) { return; }
		
    	displayInfo();
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getName());
	}
	
	public int onBackPressed(){
		
		return 0;
	}
	
	public static void log(String message) {
		Util.log("MyAccountFragment", message);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}
}
