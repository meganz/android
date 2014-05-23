package com.mega.android;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaTransfer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.app.ActionBar;
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
	SparseArray<TransfersHolder> transfersListArray;
	int positionClicked;
	ActionBar aB;

	MegaApiAndroid megaApi;
	
	public MegaTransfersAdapter(Context _context, SparseArray<TransfersHolder> _transfers, ActionBar aB) {
		this.context = _context;
		this.transfersListArray = _transfers;
		this.positionClicked = -1;
		this.aB = aB;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}
		
	/*private view holder class*/
    private class ViewHolder {
		ImageView imageView;
        TextView textViewFileName;
        ImageView imageViewCompleted;
        TextView textViewCompleted;
        ImageView imageViewOneDot;
        TextView textViewRate;
//        ImageView imageViewBarStructure;
//        ImageView imageViewBarFill;
        ProgressBar transferProgressBar;
        ImageButton imageButtonThreeDots;
        RelativeLayout itemLayout;
        ImageView arrowSelection;
        RelativeLayout optionsLayout;
        ImageButton optionUndo;
        ImageButton optionDeletePermanently;
        int currentPosition;
    }
    
    public void setTransfers(SparseArray<TransfersHolder> transfers){
    	this.transfersListArray = transfers;
    	notifyDataSetChanged();
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	
		final int _position = position;
		
		ViewHolder holder = null;
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_transfers_list, parent, false);
			holder = new ViewHolder();
			holder.itemLayout = (RelativeLayout) convertView.findViewById(R.id.transfers_list_item_layout);
			holder.imageView = (ImageView) convertView.findViewById(R.id.transfers_list_thumbnail);
			holder.textViewFileName = (TextView) convertView.findViewById(R.id.transfers_list_filename);
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
			holder = (ViewHolder) convertView.getTag();
		}

		holder.currentPosition = position;
		
		TransfersHolder transfer = (TransfersHolder) getItem(position);
		String fileName = transfer.getName(); 
		
		holder.imageView.setImageResource(R.drawable.mime_3d);
		holder.textViewFileName.setText(fileName);

		if (position == 0 || position == 1){
			holder.textViewCompleted.setText("Completed");
			holder.imageViewCompleted.setImageResource(R.drawable.transferok);
			holder.imageViewOneDot.setVisibility(View.GONE);
			holder.textViewRate.setVisibility(View.GONE);	
//			holder.imageViewBarStructure.setVisibility(View.GONE);
//			holder.imageViewBarFill.setVisibility(View.GONE);
			holder.transferProgressBar.setVisibility(View.GONE);
		}
		else if (position == 6 || position == 7){
			holder.textViewCompleted.setText("Queued");
			holder.imageViewCompleted.setImageResource(R.drawable.transferqueued);
			holder.imageViewOneDot.setVisibility(View.GONE);
			holder.textViewRate.setVisibility(View.GONE);
//			holder.imageViewBarStructure.setVisibility(View.GONE);
//			holder.imageViewBarFill.setVisibility(View.GONE);
			holder.transferProgressBar.setVisibility(View.GONE);
		}
		else{
			holder.imageViewOneDot.setVisibility(View.VISIBLE);
			holder.textViewRate.setVisibility(View.VISIBLE);
//			holder.imageViewBarStructure.setVisibility(View.VISIBLE);
//			holder.imageViewBarFill.setVisibility(View.VISIBLE);
			holder.transferProgressBar.setVisibility(View.VISIBLE);
			holder.textViewRate.setText("600 KB/s");
//			holder.imageViewBarStructure.getLayoutParams().width = Util.px2dp((250*scaleW), outMetrics);
//			holder.imageViewBarFill.getLayoutParams().width = Util.px2dp((170*scaleW), outMetrics);
			holder.transferProgressBar.getLayoutParams().width = Util.px2dp((250*scaleW), outMetrics);
			holder.transferProgressBar.setProgress(65);
		}
		
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

	@Override
    public int getCount() {
        return transfersListArray.size();
    }
 
    @Override
    public Object getItem(int position) {
        return transfersListArray.get(position);
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

		ViewHolder holder = (ViewHolder) v.getTag();
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
