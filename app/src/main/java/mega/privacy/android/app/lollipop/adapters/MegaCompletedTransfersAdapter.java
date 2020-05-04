package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.recyclerview.widget.RecyclerView;

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

import mega.privacy.android.app.AndroidCompletedTransfer;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.managerSections.CompletedTransfersFragmentLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaTransfer;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;


public class MegaCompletedTransfersAdapter extends RecyclerView.Adapter<MegaCompletedTransfersAdapter.ViewHolderTransfer> implements OnClickListener {

	Context context;
	ArrayList<AndroidCompletedTransfer> tL = null;
	MegaTransfer currentTransfer = null;
	int positionClicked;
	CompletedTransfersFragmentLollipop fragment;
	MegaApiAndroid megaApi;

	RecyclerView listFragment;

	public MegaCompletedTransfersAdapter(Context _context, CompletedTransfersFragmentLollipop _fragment, ArrayList<AndroidCompletedTransfer> _transfers, RecyclerView _listView) {
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
    
    public void setTransfers(ArrayList<AndroidCompletedTransfer> transfers){
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
		holder.textViewFileName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
		holder.textViewFileName.getLayoutParams().width = px2dp((150*scaleW), outMetrics);
		holder.imageViewCompleted = (ImageView) v.findViewById(R.id.transfers_list_completed_image);
		holder.textViewCompleted = (TextView) v.findViewById(R.id.transfers_list_completed_text);
		holder.transferProgressBar = (ProgressBar) v.findViewById(R.id.transfers_list_bar);
		holder.optionRemove = (ImageView) v.findViewById(R.id.transfers_list_option_remove);
//		holder.optionRemove.setOnClickListener(this);
		holder.optionPause = (ImageView) v.findViewById(R.id.transfers_list_option_pause);
		v.setTag(holder);
    	
    	return holder;
    }

	@Override
	public void onBindViewHolder(ViewHolderTransfer holder, int position) {
		logDebug("Position: " + position);
		
		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);

		holder.currentPosition = position;

		AndroidCompletedTransfer transfer = (AndroidCompletedTransfer) getItem(position);

		String fileName = transfer.getFileName();
		holder.textViewFileName.setText(fileName);
		holder.optionPause.setVisibility(View.GONE);
		holder.optionRemove.setVisibility(View.GONE);
		holder.transferProgressBar.setVisibility(View.GONE);

        holder.imageView.setImageResource(MimeTypeList.typeForName(transfer.getFileName()).getIconResourceId());
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
		params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
		params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
		params.setMargins(36, 0, 0, 0);
		holder.imageView.setLayoutParams(params);

		RelativeLayout.LayoutParams params3 = (RelativeLayout.LayoutParams) holder.iconDownloadUploadView.getLayoutParams();
		params3.setMargins(0, 0, 0, 0);
		holder.iconDownloadUploadView.setLayoutParams(params3);

        if (MimeTypeList.typeForName(transfer.getFileName()).isImage()||MimeTypeList.typeForName(transfer.getFileName()).isVideo()){

            long handle = Long.parseLong(transfer.getNodeHandle());
            Bitmap thumb = getThumbnailFromCache(handle);
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
                MegaNode node = megaApi.getNodeByHandle(handle);
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
            }
        }

		if (transfer.getType() == MegaTransfer.TYPE_DOWNLOAD){
			holder.iconDownloadUploadView.setImageResource(R.drawable.ic_download_transfers);

		}
		else if (transfer.getType() == MegaTransfer.TYPE_UPLOAD){
			holder.iconDownloadUploadView.setImageResource(R.drawable.ic_upload_transfers);
		}

		int state = transfer.getState();
		logDebug("State of the transfer: " + state);
		switch (state){
			case MegaTransfer.STATE_COMPLETED:{
				holder.textViewCompleted.setText(transfer.getPath());
				holder.imageViewCompleted.setImageResource(R.drawable.ic_complete_transfer);

				break;
			}
			default:{
				logError("Default status -- error, this should be completed state always");
				holder.textViewCompleted.setText(context.getResources().getString(R.string.transfer_unknown));
				holder.imageViewCompleted.setImageResource(R.drawable.ic_queue);
				break;
			}
		}

		if (positionClicked != -1){
			if (positionClicked == position){
				listFragment.smoothScrollToPosition(position);
			}
		}

		holder.itemLayout.setBackgroundColor(Color.WHITE);
//		holder.optionRemove.setTag(holder);
//		holder.optionPause.setTag(holder);
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
		logDebug("onClick");
		
//		ViewHolderTransfer holder = (ViewHolderTransfer) v.getTag();
//		int currentPosition = holder.currentPosition;
//
//		switch(v.getId()){
//			case R.id.transfers_list_option_remove:{
//				log("click to cancel transfer");
//				MegaTransfer t = (MegaTransfer) getItem(currentPosition);
//
//				((ManagerActivityLollipop) context).showConfirmationCancelTransfer(t, true);
//				break;
//			}
//			case R.id.transfers_list_option_pause:{
//				log("click to cancel transfer");
//				MegaTransfer t = (MegaTransfer) getItem(currentPosition);
//                ((ManagerActivityLollipop) context).showConfirmationCancelTransfer(t, false);
//				break;
//			}
//		}
	}

//	public void removeItemData(int position) {
//		notifyItemRemoved(position);
//		notifyItemRangeChanged(position,getItemCount());
//	}
}
