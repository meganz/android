package nz.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import nz.mega.android.MegaShareInOutListAdapter.ViewHolderInOutShareList;
import nz.mega.android.utils.ThumbnailUtils;
import nz.mega.android.utils.Util;
import nz.mega.components.RoundedImageView;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaUser;


import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MegaShareInOutGridAdapter extends BaseAdapter implements OnClickListener {
	Context context;
	MegaApiAndroid megaApi;

	int positionClicked;
	//NodeList nodes;

	long parentHandle = -1;

	ArrayList<MegaShareAndroidElement> megaShareInList;
	ArrayList<MegaShare> megaShareOutList;
	
	ListView listFragment;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	ActionBar aB;
	HashMap<Long, MegaTransfer> mTHash = null;
	
	MegaTransfer currentTransfer = null;
	
	public static int MODE_IN = 0;
	public static int MODE_OUT = 1;

	//boolean multipleSelect;
	int type = ManagerActivity.MODE_IN;

	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	public static ArrayList<String> pendingAvatars = new ArrayList<String>();
	
	private class UserAvatarListenerGrid implements MegaRequestListenerInterface{

		Context context;
		ViewHolderInOutShareGrid holder;
		MegaShareInOutGridAdapter adapter;
		int numView;
		
		public UserAvatarListenerGrid(Context context, ViewHolderInOutShareGrid holder, MegaShareInOutGridAdapter adapter, int numView){
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
			this.numView = numView;
		}
		
		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			log("onRequestStart()");
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
			
			log("onRequestFinish() "+request.getEmail());
			if (e.getErrorCode() == MegaError.API_OK){
				if (numView == 1){
					if (holder.contactMail1.compareTo(request.getEmail()) == 0){
						File avatar = null;
						if (context.getExternalCacheDir() != null){
							avatar = new File(context.getExternalCacheDir().getAbsolutePath(), holder.contactMail1 + ".jpg");
						}
						else{
							avatar = new File(context.getCacheDir().getAbsolutePath(), holder.contactMail1 + ".jpg");
						}
						Bitmap bitmap = null;
						if (avatar.exists()){
							if (avatar.length() > 0){
								BitmapFactory.Options bOpts = new BitmapFactory.Options();
								bOpts.inPurgeable = true;
								bOpts.inInputShareable = true;
								bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
								if (bitmap == null) {
									avatar.delete();
								}
								else{
									holder.imageView1.setImageBitmap(bitmap);
								}
							}
						}
						adapter.notifyDataSetChanged();
					}
				}
				else if (numView == 2){
					if (holder.contactMail2.compareTo(request.getEmail()) == 0){
						File avatar = null;
						if (context.getExternalCacheDir() != null){
							avatar = new File(context.getExternalCacheDir().getAbsolutePath(), holder.contactMail2 + ".jpg");
						}
						else{
							avatar = new File(context.getCacheDir().getAbsolutePath(), holder.contactMail2 + ".jpg");
						}
						Bitmap bitmap = null;
						if (avatar.exists()){
							if (avatar.length() > 0){
								BitmapFactory.Options bOpts = new BitmapFactory.Options();
								bOpts.inPurgeable = true;
								bOpts.inInputShareable = true;
								bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
								if (bitmap == null) {
									avatar.delete();
								}
								else{
									holder.imageView2.setImageBitmap(bitmap);
								}
							}
						}
						
					}
				}
			}
			else{
				pendingAvatars.remove(request.getEmail());
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
			log("onRequestTemporaryError");
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}
		
	}	

	/* public static view holder class */
	public class ViewHolderInOutShareGrid {
		public ImageView imageView1;
		public RoundedImageView roundedImageView1;
		public TextView textViewFileName1;
		public TextView textViewFileSize1;
		public RelativeLayout itemLayoutFile1;
		public RelativeLayout itemLayoutContact1;
		//public ImageView arrowSelection;
		public RelativeLayout optionsLayout1;
		public ImageView optionDownload1;
		public ImageView optionProperties1;
		public ProgressBar transferProgressBar1;
		public int currentPosition1;
		public RoundedImageView contactThumbnail1;
		public TextView contactName1;
//		public TextView contactContent;		
		public long document1;
		String contactMail1;
		//public TextView textViewOwner;
		
		public ImageView imageView2;
		public RoundedImageView roundedImageView2;
		public TextView textViewFileName2;
		public TextView textViewFileSize2;
		public RelativeLayout itemLayoutFile2;
		public RelativeLayout itemLayoutContact2;
		//public ImageView arrowSelection;
		public RelativeLayout optionsLayout2;
		public ImageView optionDownload2;
		public ImageView optionProperties2;
		public ProgressBar transferProgressBar2;
		
		public RoundedImageView contactThumbnail2;
		public TextView contactName2;
//		public TextView contactContent;		
		public long document2;
		String contactMail2;
		
		public int currentPosition;
		
		//Incoming
		public TextView contactName;
		public TextView contactMail;
		public RoundedImageView contactThumbnail;
		public RelativeLayout itemLayoutContact;
		
	}
	
	public MegaShareInOutGridAdapter(Context _context, ArrayList<MegaShareAndroidElement> _megaShareInList,long _parentHandle, ListView listView, ImageView emptyImageView,TextView emptyTextView, ActionBar aB, int type) {
		this.context = _context;
		this.megaShareInList = _megaShareInList;

		this.parentHandle = _parentHandle;

		this.listFragment = listView;
		this.emptyImageViewFragment = emptyImageView;
		this.emptyTextViewFragment = emptyTextView;
		this.aB = aB;

		this.positionClicked = -1;

		this.type = type;
		
		log("MegaShareInOutAdapter: "+type);

		if (megaApi == null) {
			megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
		}
	}
	

	
	public void setNodes(ArrayList<MegaShareAndroidElement> _megaShareInList) {
		this.megaShareInList = _megaShareInList;
		positionClicked = -1;
		notifyDataSetChanged();
	}
	
	//GETVIEW

	
	
	
	//END GETVIEW
	
	//Avatar for Incoming
	
	private void getAvatar(MegaUser megaUser, ViewHolderInOutShareGrid holder){
		
		UserAvatarListenerGrid  listener = new UserAvatarListenerGrid (context,holder,this,-1);
		holder.contactName.setText(megaUser.getEmail());
//		holder.contactMail=megaUser.getEmail();
		File avatar = null;
		if (context.getExternalCacheDir() != null){
			avatar = new File(context.getExternalCacheDir().getAbsolutePath(), megaUser.getEmail() + ".jpg");
		}
		else{
			avatar = new File(context.getCacheDir().getAbsolutePath(), megaUser.getEmail() + ".jpg");
		}
		Bitmap bitmap = null;
		if (avatar.exists()){
			if (avatar.length() > 0){
				BitmapFactory.Options bOpts = new BitmapFactory.Options();
				bOpts.inPurgeable = true;
				bOpts.inInputShareable = true;
				bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
				if (bitmap == null) {
					avatar.delete();
					if (context.getExternalCacheDir() != null){
						megaApi.getUserAvatar(megaUser, context.getExternalCacheDir().getAbsolutePath() + "/" + megaUser.getEmail() + ".jpg", listener);
					}
					else{
						megaApi.getUserAvatar(megaUser, context.getCacheDir().getAbsolutePath() + "/" + megaUser.getEmail() + ".jpg", listener);
					}
				}
				else{
//					holder.roundedImageView.setImageBitmap(bitmap);
				}
			}
			else{
				if (context.getExternalCacheDir() != null){
					megaApi.getUserAvatar(megaUser, context.getExternalCacheDir().getAbsolutePath() + "/" + megaUser.getEmail() + ".jpg", listener);	
				}
				else{
					megaApi.getUserAvatar(megaUser, context.getCacheDir().getAbsolutePath() + "/" + megaUser.getEmail() + ".jpg", listener);	
				}			
			}
		}	
		else{
			if (!pendingAvatars.contains(megaUser.getEmail())){
				pendingAvatars.add(megaUser.getEmail());
				if (context.getExternalCacheDir() != null){
					megaApi.getUserAvatar(megaUser, context.getExternalCacheDir().getAbsolutePath() + "/" + megaUser.getEmail() + ".jpg", listener);
				}
				else{
					megaApi.getUserAvatar(megaUser, context.getCacheDir().getAbsolutePath() + "/" + megaUser.getEmail() + ".jpg", listener);
				}
			}
		}
	}

	//Avatar for Outgoing
	private void getAvatar(MegaUser megaUser, ViewHolderInOutShareGrid holder, int numView){
	
		UserAvatarListenerGrid listener = new UserAvatarListenerGrid(context,holder,this,numView);
		if(numView==1){
			holder.contactName1.setText(megaUser.getEmail());
			holder.contactMail1=megaUser.getEmail();
		}
		else{
			holder.contactName2.setText(megaUser.getEmail());
			holder.contactMail2=megaUser.getEmail();
		}

		File avatar = null;
		if (context.getExternalCacheDir() != null){
			avatar = new File(context.getExternalCacheDir().getAbsolutePath(), megaUser.getEmail() + ".jpg");
		}
		else{
			avatar = new File(context.getCacheDir().getAbsolutePath(), megaUser.getEmail() + ".jpg");
		}
		Bitmap bitmap = null;
		if (avatar.exists()){
			if (avatar.length() > 0){
				BitmapFactory.Options bOpts = new BitmapFactory.Options();
				bOpts.inPurgeable = true;
				bOpts.inInputShareable = true;
				bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
				if (bitmap == null) {
					avatar.delete();
					if (context.getExternalCacheDir() != null){
						megaApi.getUserAvatar(megaUser, context.getExternalCacheDir().getAbsolutePath() + "/" + megaUser.getEmail() + ".jpg", listener);
					}
					else{
						megaApi.getUserAvatar(megaUser, context.getCacheDir().getAbsolutePath() + "/" + megaUser.getEmail() + ".jpg", listener);
					}
				}
				else{
					
					if(numView==1){
						holder.roundedImageView1.setImageBitmap(bitmap);
					}
					else{
						holder.roundedImageView2.setImageBitmap(bitmap);
					}							
				}
			}
			else{
				if (context.getExternalCacheDir() != null){
					megaApi.getUserAvatar(megaUser, context.getExternalCacheDir().getAbsolutePath() + "/" + megaUser.getEmail() + ".jpg", listener);	
				}
				else{
					megaApi.getUserAvatar(megaUser, context.getCacheDir().getAbsolutePath() + "/" + megaUser.getEmail() + ".jpg", listener);	
				}			
			}
		}	
		else{
			if (!pendingAvatars.contains(megaUser.getEmail())){
				pendingAvatars.add(megaUser.getEmail());
				if (context.getExternalCacheDir() != null){
					megaApi.getUserAvatar(megaUser, context.getExternalCacheDir().getAbsolutePath() + "/" + megaUser.getEmail() + ".jpg", listener);
				}
				else{
					megaApi.getUserAvatar(megaUser, context.getCacheDir().getAbsolutePath() + "/" + megaUser.getEmail() + ".jpg", listener);
				}
			}
		}
		
		
	}
				
	private String getInfoFolder(MegaNode n) {
		int numFolders = megaApi.getNumChildFolders(n);
		int numFiles = megaApi.getNumChildFiles(n);

		String info = "";
		if (numFolders > 0) {
			info = numFolders
					+ " "
					+ context.getResources().getQuantityString(
							R.plurals.general_num_folders, numFolders);
			if (numFiles > 0) {
				info = info
						+ ", "
						+ numFiles
						+ " "
						+ context.getResources().getQuantityString(
								R.plurals.general_num_files, numFiles);
			}
		} else {
			info = numFiles
					+ " "
					+ context.getResources().getQuantityString(
							R.plurals.general_num_files, numFiles);
		}

		return info;
	}

	@Override
	public boolean isEnabled(int position) {
		// if (position == 0){
		// return false;
		// }
		// else{
		// return true;
		// }
		return super.isEnabled(position);
	}

	@Override
	public int getCount() {
		return megaShareInList.size();
	}

	@Override
	public Object getItem(int position) {
		return megaShareInList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public int getPositionClicked() {
		return positionClicked;
	}

	public void setPositionClicked(int p) {
		positionClicked = p;
	}
	
	private static void log(String log) {
		Util.log("MegaShareInOutAdapter", log);
	}



	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
	}
}
