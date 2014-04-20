package com.mega.android;

import com.mega.android.ManagerActivity.DrawerItem;
import com.mega.sdk.AccountDetails;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MyAccountFragment extends Fragment implements MegaRequestListenerInterface {

	private ImageView accountImageView;
	private TextView emailView;
	private TextView totalSpaceView;
	private TextView usedSpaceView;
	private TextView freeSpaceView;
	
	private Button upgradeButton;
	private Button passwordButton;
	
	MegaApiAndroid megaApi;
	
	ProgressDialog updateProgress;
	
	long maxStorage;
	long usedStorage;
	int proLevel;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle savedInstanceState) {
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
		return view;
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

		UserCredentials credentials = Preferences.getCredentials(activity);
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
		}
		
		AccountDetails accountInfo = request.getAccountDetails();
		maxStorage = accountInfo.getMaxStorage();
		usedStorage = accountInfo.getUsedStorage();
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
}
