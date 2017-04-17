package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils.TruncateAt;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.TransfersFragmentLollipop;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaListChatLollipopAdapter;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.TimeChatUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaTransfer;


public class MegaTransfersLollipopAdapter extends RecyclerView.Adapter<MegaTransfersLollipopAdapter.ViewHolderTransfer> implements OnClickListener {
	
	Context context;
//	SparseArray<TransfersHolder> transfersListArray;
	ArrayList<MegaTransfer> tL = null;
	MegaTransfer currentTransfer = null;
	int positionClicked;
	TransfersFragmentLollipop fragment;
	MegaApiAndroid megaApi;
	
	boolean multipleSelect;
	
	RecyclerView listFragment;
	
//	public MegaTransfersAdapter(Context _context, SparseArray<TransfersHolder> _transfers, ActionBar aB) {
//		this.context = _context;
//		this.transfersListArray = _transfers;
//		this.positionClicked = -1;
//		this.aB = aB;
//		
//		if (megaApi == null){
//			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
//		}
//	}
	
	
	private class TransferThumbnailAsyncTask extends AsyncTask<String, Void, Bitmap>{
		
		ViewHolderTransfer holder;
    	String currentPath;
    	
    	public TransferThumbnailAsyncTask(ViewHolderTransfer holder) {
			this.holder = holder;
		}
    	
    	@Override
		protected Bitmap doInBackground(String... params) {
    		currentPath = params[0];
			File currentFile = new File(currentPath);
			
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			Bitmap thumb = BitmapFactory.decodeFile(currentFile.getAbsolutePath(), options);
			
			ExifInterface exif;
			int orientation = ExifInterface.ORIENTATION_NORMAL;
			try {
				exif = new ExifInterface(currentFile.getAbsolutePath());
				orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
			} catch (IOException e) {}  
			
			// Calculate inSampleSize
		    options.inSampleSize = Util.calculateInSampleSize(options, 120, 120);
		    
		    // Decode bitmap with inSampleSize set
		    options.inJustDecodeBounds = false;
		    
		    thumb = BitmapFactory.decodeFile(currentFile.getAbsolutePath(), options);
			if (thumb != null){
				thumb = Util.rotateBitmap(thumb, orientation);
				ThumbnailUtils.setThumbnailCache(holder.currentPath, thumb);
				return thumb;
			}
			
			return null;
    	}
    	
    	@Override
		protected void onPostExecute(Bitmap thumb){
    		if (holder.currentPath.compareTo(currentPath) == 0){
				holder.imageView.setImageBitmap(thumb);
				Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
				holder.imageView.startAnimation(fadeInAnimation);
			}
    	}
	}
	
	
	public MegaTransfersLollipopAdapter(Context _context, TransfersFragmentLollipop _fragment, ArrayList<MegaTransfer> _transfers, RecyclerView _listView) {
		this.context = _context;
		this.tL = _transfers;
		this.positionClicked = -1;
		this.fragment = _fragment;
		this.listFragment = _listView;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}

		
	/*private view holder class*/
    public class ViewHolderTransfer extends RecyclerView.ViewHolder{
    	public ViewHolderTransfer(View v) {
			super(v);
		}
		public ImageView imageView;
    	public ImageView iconDownloadUploadView;
    	public TextView textViewFileName;
    	public ImageView imageViewCompleted;
    	public TextView textViewCompleted;
    	public ProgressBar transferProgressBar;
    	public RelativeLayout itemLayout;
    	public ImageView optionRemove;
		public ImageView optionPause;
    	public int currentPosition;
    	public long document;
    	public String currentPath;
    }
    
//    public void setTransfers(SparseArray<TransfersHolder> transfers){
//    	this.transfersListArray = transfers;
//    	notifyDataSetChanged();
//    }
    
    public void setTransfers(ArrayList<MegaTransfer> transfers){
    	this.tL = transfers;
    	notifyDataSetChanged();
    }

    @Override
	public ViewHolderTransfer onCreateViewHolder(ViewGroup parent, int viewType) {
    	log("onCreateViewHolder");
    	
    	ViewHolderTransfer holder;
    	
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transfers_list, parent, false);

		holder = new ViewHolderTransfer(v);
		holder.itemLayout = (RelativeLayout) v.findViewById(R.id.transfers_list_item_layout);
		holder.imageView = (ImageView) v.findViewById(R.id.transfers_list_thumbnail);
		holder.iconDownloadUploadView = (ImageView) v.findViewById(R.id.transfers_list_small_icon);
		holder.textViewFileName = (TextView) v.findViewById(R.id.transfers_list_filename);
		holder.textViewFileName.setSingleLine(true);
		holder.textViewFileName.setEllipsize(TruncateAt.MIDDLE);
		holder.textViewFileName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
		holder.textViewFileName.getLayoutParams().width = Util.px2dp((150*scaleW), outMetrics);
		holder.imageViewCompleted = (ImageView) v.findViewById(R.id.transfers_list_completed_image);
		holder.textViewCompleted = (TextView) v.findViewById(R.id.transfers_list_completed_text);
		holder.transferProgressBar = (ProgressBar) v.findViewById(R.id.transfers_list_bar);
		holder.optionRemove = (ImageView) v.findViewById(R.id.transfers_list_option_remove);
		holder.optionRemove.setOnClickListener(this);
		holder.optionPause = (ImageView) v.findViewById(R.id.transfers_list_option_pause);
		holder.optionPause.setOnClickListener(this);
		v.setTag(holder);
    	
    	return holder;
    }

	@Override
	public void onBindViewHolder(ViewHolderTransfer holder, int position) {
		log("onBindViewHolder: "+position);
		
		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);
		
		holder.currentPosition = position;

		MegaTransfer transfer = null;
		MegaTransfer transferFromList = (MegaTransfer) getItem(position);
		if (currentTransfer == null){
			transfer = transferFromList;
		}
		else{
			if (transferFromList.getTag() == currentTransfer.getTag()){
				transfer = currentTransfer;
			}
			else{
				transfer = transferFromList;
			}
		}
		String fileName = transfer.getFileName();
		log("onBindViewHolder: "+fileName);
		holder.textViewFileName.setText(fileName);

		if (transfer.getType() == MegaTransfer.TYPE_DOWNLOAD){
		
			holder.iconDownloadUploadView.setImageResource(R.drawable.ic_download_transfers);
			MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
			holder.document = transfer.getNodeHandle();
			
			//if node == null --> Public node
			if (node == null){
				holder.imageView.setImageResource(MimeTypeList.typeForName(transfer.getFileName()).getIconResourceId());
				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
				params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
				params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
				params.setMargins(36, 0, 0, 0);
				holder.imageView.setLayoutParams(params);
			}
			else{
				holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
				params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
				params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
				params.setMargins(36, 0, 0, 0);
				holder.imageView.setLayoutParams(params);
				
				Bitmap thumb = null;
				if (node.hasThumbnail()){
					RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
					params2.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
					params2.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
					params2.setMargins(54, 0, 12, 0);
					
					thumb = ThumbnailUtils.getThumbnailFromCache(node);
					if (thumb != null){
						RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
						params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						params1.setMargins(54, 0, 12, 0);
						holder.imageView.setLayoutParams(params1);
						holder.imageView.setImageBitmap(thumb);
					}
					else{
						thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
						if (thumb != null){
							RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
							params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
							params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
							params1.setMargins(54, 0, 12, 0);
							holder.imageView.setImageBitmap(thumb);
						}
						else{ 
							try{
								thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaTransfer(node, context, holder, megaApi, this);
							}
							catch(Exception e){} //Too many AsyncTasks
							
							if (thumb != null){
								holder.imageView.setImageBitmap(thumb);
							}
						}
					}
				}
			}
		}
		else if (transfer.getType() == MegaTransfer.TYPE_UPLOAD){
			holder.iconDownloadUploadView.setImageResource(R.drawable.ic_upload_transfers);
			holder.imageView.setImageResource(MimeTypeList.typeForName(transfer.getFileName()).getIconResourceId());
			holder.currentPath = transfer.getPath();
			
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
			params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
			params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
			params.setMargins(36, 0, 0, 0);
			holder.imageView.setLayoutParams(params);
			
			if (MimeTypeList.typeForName(transfer.getFileName()).isImage()){
				/*Bitmap thumb = null;
				thumb = ThumbnailUtils.getThumbnailFromCache(holder.currentPath);
				if (thumb != null){
					holder.imageView.setImageBitmap(thumb);
				}
				else{
					try{
						new TransferThumbnailAsyncTask(holder).execute(transfer.getPath());
					}
					catch(Exception e){
						//Too many AsyncTasks
					}
				}*/
			}
		}

		int state = transfer.getState();
		log("State of the transfer: "+state);
		switch (state){
			case MegaTransfer.STATE_PAUSED:{
				holder.textViewCompleted.setVisibility(View.VISIBLE);
				holder.imageViewCompleted.setVisibility(View.VISIBLE);
				holder.textViewCompleted.setText("Paused");
				holder.imageViewCompleted.setImageResource(R.drawable.ic_queue);
				holder.transferProgressBar.setVisibility(View.GONE);
				holder.optionRemove.setVisibility(View.VISIBLE);
				holder.optionPause.setImageResource(R.drawable.ic_play);
				holder.optionPause.setVisibility(View.VISIBLE);
				break;
			}
			case MegaTransfer.STATE_CANCELLED:{
				holder.textViewCompleted.setVisibility(View.VISIBLE);
				holder.imageViewCompleted.setVisibility(View.VISIBLE);
				holder.textViewCompleted.setText("Cancelled");
				holder.imageViewCompleted.setImageResource(R.drawable.ic_queue);
				holder.transferProgressBar.setVisibility(View.GONE);
				holder.optionRemove.setVisibility(View.GONE);
				holder.optionPause.setVisibility(View.GONE);
				break;
			}
			case MegaTransfer.STATE_COMPLETED:{
				holder.textViewCompleted.setVisibility(View.VISIBLE);
				holder.imageViewCompleted.setVisibility(View.VISIBLE);
				holder.textViewCompleted.setText("Completed");
				holder.imageViewCompleted.setImageResource(R.drawable.ic_complete_transfer);
				holder.transferProgressBar.setVisibility(View.GONE);
				holder.optionRemove.setVisibility(View.GONE);
				holder.optionPause.setVisibility(View.GONE);
				break;
			}
			case MegaTransfer.STATE_ACTIVE:{
				holder.textViewCompleted.setVisibility(View.GONE);
				holder.imageViewCompleted.setVisibility(View.GONE);
				holder.transferProgressBar.setVisibility(View.VISIBLE);
				holder.transferProgressBar.getLayoutParams().width = Util.px2dp((250*scaleW), outMetrics);
				double progressValue = 100.0 * transfer.getTransferredBytes() / transfer.getTotalBytes();
				log("Progress Value: "+ progressValue);
				holder.transferProgressBar.setProgress((int)progressValue);
				holder.optionRemove.setVisibility(View.VISIBLE);
				holder.optionPause.setImageResource(R.drawable.ic_pause);
				holder.optionPause.setVisibility(View.VISIBLE);
				break;
			}
			case MegaTransfer.STATE_COMPLETING:
			case MegaTransfer.STATE_RETRYING:
			case MegaTransfer.STATE_QUEUED:{
				holder.textViewCompleted.setVisibility(View.VISIBLE);
				holder.imageViewCompleted.setVisibility(View.VISIBLE);
				holder.textViewCompleted.setText("Queued");
				holder.imageViewCompleted.setImageResource(R.drawable.ic_queue);
				holder.transferProgressBar.setVisibility(View.GONE);
				holder.optionRemove.setVisibility(View.VISIBLE);
				holder.optionPause.setVisibility(View.VISIBLE);
				break;
			}
			default:{
				log("Default status");
				holder.imageViewCompleted.setVisibility(View.VISIBLE);
				holder.transferProgressBar.setVisibility(View.GONE);
				holder.textViewCompleted.setText("DEFAULT");
				holder.optionPause.setVisibility(View.GONE);
				break;
			}
		}

		if (positionClicked != -1){
			if (positionClicked == position){
				listFragment.smoothScrollToPosition(position);
			}
		}

		holder.itemLayout.setBackgroundColor(Color.WHITE);
		holder.optionRemove.setTag(holder);
		holder.optionPause.setTag(holder);
	}
 
	@Override
	public int getItemCount() {
		return tL.size();
	}
	
	public Object getItem(int position) {
		return tL.get(position);
	}
	
    @Override
    public long getItemId(int position) {
        return position;
    }    
    
    public int getPositionClicked (){
    	return positionClicked;
    }
    
    public void setPositionClicked(int p){
    	positionClicked = p;
    }

	@Override
	public void onClick(View v) {
		log("onClick");
		
		ViewHolderTransfer holder = (ViewHolderTransfer) v.getTag();
		int currentPosition = holder.currentPosition;
		
		switch(v.getId()){
			case R.id.transfers_list_option_remove:{
				log("click to cancel transfer");
				MegaTransfer t = (MegaTransfer) getItem(currentPosition);

				((ManagerActivityLollipop) context).showConfirmationCancelTransfer(t, true);
				break;
			}
			case R.id.transfers_list_option_pause:{
				log("click to cancel transfer");
				MegaTransfer t = (MegaTransfer) getItem(currentPosition);
                ((ManagerActivityLollipop) context).showConfirmationCancelTransfer(t, false);
				break;
			}
		}
	}

	public void removeItemData(int position) {
		notifyItemRemoved(position);
		notifyItemRangeChanged(position,getItemCount());
	}

	public void updateProgress(int position, MegaTransfer transfer) {
		log("updateProgress");

		ViewHolderTransfer holder = (ViewHolderTransfer) listFragment.findViewHolderForAdapterPosition(position);

		if(holder!=null){
			double progressValue = 100.0 * transfer.getTransferredBytes() / transfer.getTotalBytes();
			log("Progress Value: "+ progressValue);
			holder.transferProgressBar.setProgress((int)progressValue);

		}
		else{
			log("Holder is NULL: "+position);
			notifyItemChanged(position);
		}
	}
	
	/*
	 * Get transfer at specified position
	 */
	public MegaTransfer getTransferAt(int position) {
		try {
			if(tL != null){
				return tL.get(position);
			}
		} catch (IndexOutOfBoundsException e) {}
		return null;
	}
	
	public boolean isMultipleSelect() {
		return multipleSelect;
	}
	
	public void setMultipleSelect(boolean multipleSelect) {
		if(this.multipleSelect != multipleSelect){
			this.multipleSelect = multipleSelect;
			notifyDataSetChanged();
		}
	}
	
	private static void log(String log) {
		Util.log("MegaTransfersLollipopAdapter", log);
	}

}
