package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils.TruncateAt;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.TransfersFragmentLollipop;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaTransfer;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;


public class MegaTransfersLollipopAdapter extends RecyclerView.Adapter<MegaTransfersLollipopAdapter.ViewHolderTransfer> implements OnClickListener {
	
	Context context;
//	SparseArray<TransfersHolder> transfersListArray;
	ArrayList<MegaTransfer> tL = null;
	int positionClicked;
	TransfersFragmentLollipop fragment;
	MegaApiAndroid megaApi;
	
	boolean multipleSelect;
	
	RecyclerView listFragment;

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
		
    public static class ViewHolderTransfer extends RecyclerView.ViewHolder{
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
//    	public int currentPosition;
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
		logDebug("onCreateViewHolder");
    	
    	ViewHolderTransfer holder;
    	
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = getScaleW(outMetrics, density);
	    float scaleH = getScaleH(outMetrics, density);
		
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
		holder.textViewFileName.getLayoutParams().width = px2dp((150*scaleW), outMetrics);
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
		logDebug("Position: " + position);
		
		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = getScaleW(outMetrics, density);

		MegaTransfer transfer = (MegaTransfer) getItem(position);

		if(transfer==null){
			logWarning("The recovered transfer is NULL - do not update");
			return;
		}

		String fileName = transfer.getFileName();
		logDebug("Node Handle: " + transfer.getNodeHandle());
		holder.textViewFileName.setText(fileName);

		if (transfer.getType() == MegaTransfer.TYPE_DOWNLOAD){
		
			holder.iconDownloadUploadView.setImageResource(R.drawable.ic_download_transfers);
			MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
			holder.document = transfer.getNodeHandle();

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

				RelativeLayout.LayoutParams params3 = (RelativeLayout.LayoutParams) holder.iconDownloadUploadView.getLayoutParams();
				params3.setMargins(0, 0, 0, 0);
				holder.iconDownloadUploadView.setLayoutParams(params3);
				
				Bitmap thumb = null;
				if (node.hasThumbnail()){
//
					thumb = getThumbnailFromCache(node);
					if (thumb != null){
						RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
						params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						params1.setMargins(54, 0, 18, 0);
						holder.imageView.setLayoutParams(params1);

						RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) holder.iconDownloadUploadView.getLayoutParams();
						params2.setMargins(0, -12, -12, 0);
						holder.iconDownloadUploadView.setLayoutParams(params2);

						holder.imageView.setImageBitmap(thumb);
					}
					else{
						thumb = getThumbnailFromFolder(node, context);
						if (thumb != null){
							RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
							params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
							params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
							params1.setMargins(54, 0, 18, 0);
							holder.imageView.setLayoutParams(params1);

							RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) holder.iconDownloadUploadView.getLayoutParams();
							params2.setMargins(0, -12, -12, 0);
							holder.iconDownloadUploadView.setLayoutParams(params2);

							holder.imageView.setImageBitmap(thumb);
						}
						else{ 
							try{
								thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaTransfer(node, context, holder, megaApi, this);
							}
							catch(Exception e){} //Too many AsyncTasks
							
							if (thumb != null){
								RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
								params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
								params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
								params1.setMargins(54, 0, 18, 0);
								holder.imageView.setLayoutParams(params1);

								RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) holder.iconDownloadUploadView.getLayoutParams();
								params2.setMargins(0, -12, -12, 0);
								holder.iconDownloadUploadView.setLayoutParams(params2);

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

			RelativeLayout.LayoutParams params3 = (RelativeLayout.LayoutParams) holder.iconDownloadUploadView.getLayoutParams();
			params3.setMargins(0, 0, 0, 0);
			holder.iconDownloadUploadView.setLayoutParams(params3);

		}

		int state = transfer.getState();
		logDebug("State of the transfer: " + state);
		switch (state){
			case MegaTransfer.STATE_PAUSED:{
				holder.textViewCompleted.setVisibility(View.VISIBLE);
				holder.imageViewCompleted.setVisibility(View.VISIBLE);
				holder.textViewCompleted.setText(context.getResources().getString(R.string.transfer_paused));
				holder.imageViewCompleted.setImageResource(R.drawable.ic_queue);
				holder.transferProgressBar.setVisibility(View.GONE);
				holder.optionRemove.setVisibility(View.VISIBLE);
				holder.optionPause.setImageResource(R.drawable.ic_play_grey);
				holder.optionPause.setVisibility(View.VISIBLE);
				break;
			}
			case MegaTransfer.STATE_ACTIVE:{
				holder.textViewCompleted.setVisibility(View.GONE);
				holder.imageViewCompleted.setVisibility(View.GONE);
				holder.transferProgressBar.setVisibility(View.VISIBLE);
				holder.transferProgressBar.getLayoutParams().width = px2dp((250*scaleW), outMetrics);
				double progressValue = 100.0 * transfer.getTransferredBytes() / transfer.getTotalBytes();
				logDebug("Progress Value: " + progressValue);
				holder.transferProgressBar.setProgress((int)progressValue);
				holder.optionRemove.setVisibility(View.VISIBLE);
				holder.optionPause.setImageResource(R.drawable.ic_pause_grey);
				holder.optionPause.setVisibility(View.VISIBLE);
				break;
			}
			case MegaTransfer.STATE_COMPLETING:
			case MegaTransfer.STATE_RETRYING:
			case MegaTransfer.STATE_QUEUED:{
				holder.textViewCompleted.setVisibility(View.VISIBLE);
				holder.imageViewCompleted.setVisibility(View.VISIBLE);
				holder.textViewCompleted.setText(context.getResources().getString(R.string.transfer_queued));
				holder.imageViewCompleted.setImageResource(R.drawable.ic_queue);
				holder.transferProgressBar.setVisibility(View.GONE);
				holder.optionRemove.setVisibility(View.VISIBLE);
				holder.optionPause.setVisibility(View.VISIBLE);
				holder.optionPause.setImageResource(R.drawable.ic_pause_grey);
				break;
			}
			default:{
				logDebug("Default status");
				holder.imageViewCompleted.setVisibility(View.VISIBLE);
				holder.transferProgressBar.setVisibility(View.GONE);
				holder.textViewCompleted.setText(context.getResources().getString(R.string.transfer_unknown));
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
		if(position>=0){
			return tL.get(position);
		}
		logError("Error: position NOT valid: " + position);
		return null;
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
		logDebug("onClick");
		
		ViewHolderTransfer holder = (ViewHolderTransfer) v.getTag();
		if(holder!=null){
			int currentPosition = holder.getAdapterPosition();

			switch(v.getId()){
				case R.id.transfers_list_option_remove:{
					logDebug("click to cancel transfer");
					MegaTransfer t = (MegaTransfer) getItem(currentPosition);

					((ManagerActivityLollipop) context).showConfirmationCancelTransfer(t, true);
					break;
				}
				case R.id.transfers_list_option_pause:{
					logDebug("click to pause/play transfer");
					MegaTransfer t = (MegaTransfer) getItem(currentPosition);
					((ManagerActivityLollipop) context).pauseIndividualTransfer(t);
					break;
				}
			}
		}
		else{
			logWarning("Holder is NULL- not action performed");
		}
	}

	public void removeItemData(int position) {
		notifyItemRemoved(position);
		notifyItemRangeChanged(position,getItemCount());
	}

	public void updateProgress(int position, MegaTransfer transfer) {
		logDebug("updateProgress");

		try{
			ViewHolderTransfer holder = (ViewHolderTransfer) listFragment.findViewHolderForAdapterPosition(position);

			if(holder!=null){
				if(holder.transferProgressBar.getVisibility()==View.GONE){
					holder.transferProgressBar.setVisibility(View.VISIBLE);
					holder.textViewCompleted.setVisibility(View.GONE);
					holder.imageViewCompleted.setVisibility(View.GONE);
				}
				double progressValue = 100.0 * transfer.getTransferredBytes() / transfer.getTotalBytes();
				logDebug("Progress Value: " + progressValue);
				holder.transferProgressBar.setProgress((int)progressValue);

			}
			else{
				logWarning("Holder is NULL: " + position);
				notifyItemChanged(position);
			}
		}
		catch(IndexOutOfBoundsException e){
			logError("EXCEPTION", e);
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
}
