package com.mega.android;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaTransfer;
import com.mega.sdk.TransferList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.text.TextUtils.TruncateAt;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MegaTransfersAdapter extends BaseAdapter implements OnClickListener {
	
	Context context;
//	SparseArray<TransfersHolder> transfersListArray;
	TransferList tL = null;
	int positionClicked;
	ActionBar aB;

	MegaApiAndroid megaApi;
	
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
	
	
	public MegaTransfersAdapter(Context _context, TransferList _transfers, ActionBar aB) {
		this.context = _context;
		this.tL = _transfers;
		this.positionClicked = -1;
		this.aB = aB;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}
	
		
	/*private view holder class*/
    public class ViewHolderTransfer {
		ImageView imageView;
        TextView textViewFileName;
        ImageView imageViewCompleted;
        TextView textViewCompleted;
        ImageView imageViewOneDot;
        TextView textViewRate;
        ProgressBar transferProgressBar;
        ImageButton imageButtonThreeDots;
        RelativeLayout itemLayout;
        ImageView arrowSelection;
        RelativeLayout optionsLayout;
        ImageButton optionUndo;
        ImageButton optionDeletePermanently;
        int currentPosition;
        long document;
    }
    
//    public void setTransfers(SparseArray<TransfersHolder> transfers){
//    	this.transfersListArray = transfers;
//    	notifyDataSetChanged();
//    }
    
    public void setTransfers(TransferList transfers){
    	this.tL = transfers;
    	notifyDataSetChanged();
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	
		final int _position = position;
		
		ViewHolderTransfer holder = null;
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_transfers_list, parent, false);
			holder = new ViewHolderTransfer();
			holder.itemLayout = (RelativeLayout) convertView.findViewById(R.id.transfers_list_item_layout);
			holder.imageView = (ImageView) convertView.findViewById(R.id.transfers_list_thumbnail);
			holder.textViewFileName = (TextView) convertView.findViewById(R.id.transfers_list_filename);
			holder.textViewFileName.setSingleLine(true);
			holder.textViewFileName.setEllipsize(TruncateAt.MIDDLE);
			holder.textViewFileName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			holder.textViewFileName.getLayoutParams().width = Util.px2dp((150*scaleW), outMetrics);
			holder.imageViewCompleted = (ImageView) convertView.findViewById(R.id.transfers_list_completed_image);
			holder.textViewCompleted = (TextView) convertView.findViewById(R.id.transfers_list_completed_text);
			holder.imageViewOneDot = (ImageView) convertView.findViewById(R.id.transfers_list_one_dot);
			holder.textViewRate = (TextView) convertView.findViewById(R.id.transfers_list_transfer_rate);
//			holder.imageViewBarStructure = (ImageView) convertView.findViewById(R.id.transfers_list_bar_structure);
//			holder.imageViewBarFill = (ImageView) convertView.findViewById(R.id.transfers_list_bar_fill);
			holder.transferProgressBar = (ProgressBar) convertView.findViewById(R.id.transfers_list_bar); 
			holder.imageButtonThreeDots = (ImageButton) convertView.findViewById(R.id.transfers_list_three_dots);
			holder.optionsLayout = (RelativeLayout) convertView.findViewById(R.id.transfers_list_options);
			holder.optionUndo = (ImageButton) convertView.findViewById(R.id.transfers_list_option_undo);
			holder.optionUndo.setPadding(Util.px2dp((87*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
			holder.optionDeletePermanently = (ImageButton) convertView.findViewById(R.id.transfers_list_option_delete_permanently);
			holder.optionDeletePermanently.setPadding(Util.px2dp((75*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), Util.px2dp((30*scaleW), outMetrics), 0);
			holder.arrowSelection = (ImageView) convertView.findViewById(R.id.transfers_list_arrow_selection);
			holder.arrowSelection.setVisibility(View.GONE);

			convertView.setTag(holder);
		}
		else{
			holder = (ViewHolderTransfer) convertView.getTag();
		}

		holder.currentPosition = position;
		
//		TransfersHolder transfer = (TransfersHolder) getItem(position);
//		String fileName = transfer.getName();
		
		
//		if (position == 0 || position == 1){
//			holder.textViewCompleted.setText("Completed");
//			holder.imageViewCompleted.setImageResource(R.drawable.transferok);
//			holder.imageViewOneDot.setVisibility(View.GONE);
//			holder.textViewRate.setVisibility(View.GONE);	
////			holder.imageViewBarStructure.setVisibility(View.GONE);
////			holder.imageViewBarFill.setVisibility(View.GONE);
//			holder.transferProgressBar.setVisibility(View.GONE);
//		}
//		else if (position == 6 || position == 7){
//			holder.textViewCompleted.setText("Queued");
//			holder.imageViewCompleted.setImageResource(R.drawable.transferqueued);
//			holder.imageViewOneDot.setVisibility(View.GONE);
//			holder.textViewRate.setVisibility(View.GONE);
////			holder.imageViewBarStructure.setVisibility(View.GONE);
////			holder.imageViewBarFill.setVisibility(View.GONE);
//			holder.transferProgressBar.setVisibility(View.GONE);
//		}
//		else{
		
		MegaTransfer transfer = (MegaTransfer) getItem(position);
		String fileName = transfer.getFileName();
		holder.textViewFileName.setText(fileName);

//		holder.imageView.setImageResource(R.drawable.mime_3d);
		
		MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
		holder.document = transfer.getNodeHandle();
		
		holder.imageView.setImageResource(MimeType.typeForName(node.getName()).getIconResourceId());
		
		Bitmap thumb = null;
		if (node.hasThumbnail()){
			thumb = ThumbnailUtils.getThumbnailFromCache(node);
			if (thumb != null){
				holder.imageView.setImageBitmap(thumb);
			}
			else{
				thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
				if (thumb != null){
					holder.imageView.setImageBitmap(thumb);
				}
				else{ 
					try{
						thumb = ThumbnailUtils.getThumbnailFromMegaTransfer(node, context, holder, megaApi, this);
					}
					catch(Exception e){} //Too many AsyncTasks
					
					if (thumb != null){
						holder.imageView.setImageBitmap(thumb);
					}
				}
			}
		}
			
		long speed = transfer.getSpeed();
		if (speed == 0){
			holder.imageViewOneDot.setVisibility(View.GONE);
			holder.textViewCompleted.setVisibility(View.VISIBLE);
			holder.imageViewCompleted.setVisibility(View.VISIBLE);
			holder.textViewCompleted.setText("Queued");
			holder.imageViewCompleted.setImageResource(R.drawable.transferqueued);
			holder.transferProgressBar.setVisibility(View.GONE);
			holder.textViewRate.setVisibility(View.GONE);
		}
		else{
			holder.imageViewOneDot.setVisibility(View.VISIBLE);
			holder.textViewCompleted.setVisibility(View.GONE);
			holder.imageViewCompleted.setVisibility(View.GONE);
			holder.transferProgressBar.setVisibility(View.VISIBLE);
			holder.textViewRate.setVisibility(View.VISIBLE);
			holder.textViewRate.setText(Util.getSpeedString(transfer.getSpeed()));
			holder.transferProgressBar.getLayoutParams().width = Util.px2dp((250*scaleW), outMetrics);
			double progressValue = 100.0 * transfer.getTransferredBytes() / transfer.getTotalBytes();
			holder.transferProgressBar.setProgress((int)progressValue);
		}
//		}
		
		holder.imageButtonThreeDots.setTag(holder);
//		holder.imageButtonThreeDots.setOnClickListener(this);
		
		if (positionClicked != -1){
			if (positionClicked == position){
				holder.arrowSelection.setVisibility(View.VISIBLE);
				LayoutParams params = holder.optionsLayout.getLayoutParams();
				params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
				holder.imageButtonThreeDots.setImageResource(R.drawable.three_dots_background_grey);
				ListView list = (ListView) parent;
				list.smoothScrollToPosition(_position);
			}
			else{
				holder.arrowSelection.setVisibility(View.GONE);
				LayoutParams params = holder.optionsLayout.getLayoutParams();
				params.height = 0;
				holder.itemLayout.setBackgroundColor(Color.WHITE);
				holder.imageButtonThreeDots.setImageResource(R.drawable.three_dots_background_white);
			}
		}
		else{
			holder.arrowSelection.setVisibility(View.GONE);
			LayoutParams params = holder.optionsLayout.getLayoutParams();
			params.height = 0;
			holder.itemLayout.setBackgroundColor(Color.WHITE);
			holder.imageButtonThreeDots.setImageResource(R.drawable.three_dots_background_white);
		}
		
		holder.optionUndo.setTag(holder);
		holder.optionUndo.setOnClickListener(this);
		
		holder.optionDeletePermanently.setTag(holder);
		holder.optionDeletePermanently.setOnClickListener(this);
		
		return convertView;
	}

//	@Override
//    public int getCount() {
//        return transfersListArray.size();
//    }
// 
//    @Override
//    public Object getItem(int position) {
//        return transfersListArray.get(position);
//    }
 
	@Override
  public int getCount() {
      return tL.size();
  }

  @Override
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

		ViewHolderTransfer holder = (ViewHolderTransfer) v.getTag();
		int currentPosition = holder.currentPosition;
		
		switch(v.getId()){
			case R.id.transfers_list_three_dots:{
				if (positionClicked == -1){
					positionClicked = currentPosition;
					notifyDataSetChanged();
				}
				else{
					if (positionClicked == currentPosition){
						positionClicked = -1;
						notifyDataSetChanged();
					}
					else{
						positionClicked = currentPosition;
						notifyDataSetChanged();
					}
				}
				break;
			}
			case R.id.transfers_list_option_undo:{
				Toast.makeText(context, "Undo_position"+currentPosition, Toast.LENGTH_SHORT).show();
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.transfers_list_option_delete_permanently:{
				Toast.makeText(context, "Delete permanently_position"+currentPosition, Toast.LENGTH_SHORT).show();
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
		}
	}

	private static void log(String log) {
		Util.log("MegaTransfersAdapter", log);
	}
}
