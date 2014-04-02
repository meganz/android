package com.mega.android;

import java.util.ArrayList;
import java.util.List;

import com.mega.components.RoundedImageView;
import com.mega.sdk.MegaUser;
import com.mega.sdk.UserList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MegaContactsGridAdapter extends BaseAdapter implements OnClickListener{
	
	Context context;
	int positionClicked;
	ArrayList<MegaUser> contacts;
	
	public MegaContactsGridAdapter(Context _context, ArrayList<MegaUser> _contacts) {
		this.context = _context;
		this.contacts = _contacts;
		this.positionClicked = -1;
	}
	
	/*private view holder class*/
    private class ViewHolder {
        RoundedImageView imageView1;
        ImageView statusImage1;
        TextView textViewFileName1;
        RelativeLayout itemLayout1;
        RoundedImageView imageView2;
        ImageView statusImage2;
        TextView textViewFileName2;
        RelativeLayout itemLayout2;
        TextView textViewFileSize1;
        TextView textViewFileSize2;
        ImageButton imageButtonThreeDots1;
        ImageButton imageButtonThreeDots2;
        ImageView arrowSelection1;
        RelativeLayout optionsLayout1;
        ImageButton optionProperties1;
        ImageButton optionSend1;
        ImageButton optionRemove1;
        ImageView arrowSelection2;
        RelativeLayout optionsLayout2;
        ImageButton optionProperties2;
        ImageButton optionSend2;
        ImageButton optionRemove2;
        int currentPosition;
    }
    
	ViewHolder holder = null;
	int positionG;

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View v;
	
		final int _position = position;
		positionG = position;
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		if ((position % 2) == 0){
			v = inflater.inflate(R.layout.item_contact_grid, parent, false);
			holder = new ViewHolder();
			holder.itemLayout1 = (RelativeLayout) v.findViewById(R.id.contact_grid_item_layout1);
			holder.itemLayout2 = (RelativeLayout) v.findViewById(R.id.contact_grid_item_layout2);
			
			//Set width and height itemLayout1
			RelativeLayout.LayoutParams paramsIL1 = new RelativeLayout.LayoutParams(Util.px2dp(172*scaleW, outMetrics),LayoutParams.WRAP_CONTENT);
			paramsIL1.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), Util.px2dp(5*scaleW, outMetrics), 0);
			paramsIL1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			paramsIL1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			holder.itemLayout1.setLayoutParams(paramsIL1);
			
			//Set width and height itemLayout2
			RelativeLayout.LayoutParams paramsIL2 = new RelativeLayout.LayoutParams(Util.px2dp(172*scaleW, outMetrics),LayoutParams.WRAP_CONTENT);
			paramsIL2.setMargins(0, Util.px2dp(5*scaleH, outMetrics), 0, 0);
			paramsIL2.addRule(RelativeLayout.RIGHT_OF, R.id.contact_grid_item_layout1);
			paramsIL2.addRule(RelativeLayout.LEFT_OF, R.id.contact_grid_separator_final);
			paramsIL2.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			TranslateAnimation anim = new TranslateAnimation(Util.px2dp(-5*scaleW, outMetrics), Util.px2dp(-5*scaleW, outMetrics), 0, 0);
	        anim.setDuration(0);
	        
	        holder.itemLayout2.startAnimation(anim);
			holder.itemLayout2.setLayoutParams(paramsIL2);
			
			holder.imageView1 = (RoundedImageView) v.findViewById(R.id.contact_grid_thumbnail1);
			holder.imageView1.setCornerRadius(Util.px2dp(78*scaleW, outMetrics));
            holder.imageView2 = (RoundedImageView) v.findViewById(R.id.contact_grid_thumbnail2);
			holder.imageView2.setCornerRadius(Util.px2dp(78*scaleW, outMetrics));
			holder.imageView1.setPadding(0, Util.px2dp(5*scaleH, outMetrics), 0, Util.px2dp(5*scaleH, outMetrics));
			holder.imageView2.setPadding(0, Util.px2dp(5*scaleH, outMetrics), 0, Util.px2dp(5*scaleH, outMetrics));

			holder.imageView1.setTag(holder);
            holder.imageView1.setOnClickListener(this);
            
            holder.imageView2.setTag(holder);
            holder.imageView2.setOnClickListener(this);
            
            holder.statusImage1 = (ImageView) v.findViewById(R.id.contact_grid_status_dot1);
            holder.statusImage2 = (ImageView) v.findViewById(R.id.contact_grid_status_dot2);
            if (position == 2){
            	holder.statusImage1.setImageResource(R.drawable.contact_yellow_dot);
            	holder.statusImage2.setImageResource(R.drawable.contact_red_dot);
            }
                        
			RelativeLayout.LayoutParams paramsIV1 = new RelativeLayout.LayoutParams(Util.px2dp(157*scaleW, outMetrics),Util.px2dp(157*scaleH, outMetrics));
			paramsIV1.addRule(RelativeLayout.CENTER_HORIZONTAL);
			holder.imageView1.setScaleType(ImageView.ScaleType.FIT_CENTER);
			paramsIV1.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), Util.px2dp(5*scaleW, outMetrics), 0);
			holder.imageView1.setLayoutParams(paramsIV1);
			
			RelativeLayout.LayoutParams paramsIV2 = new RelativeLayout.LayoutParams(Util.px2dp(157*scaleW, outMetrics),Util.px2dp(157*scaleH, outMetrics));
			paramsIV2.addRule(RelativeLayout.CENTER_HORIZONTAL);
			holder.imageView2.setScaleType(ImageView.ScaleType.FIT_CENTER);
			paramsIV2.setMargins(0, Util.px2dp(5*scaleH, outMetrics), 0, 0);
			holder.imageView2.setLayoutParams(paramsIV2);

			holder.textViewFileName1 = (TextView) v.findViewById(R.id.contact_grid_filename1);
			holder.textViewFileName2 = (TextView) v.findViewById(R.id.contact_grid_filename2);
			
			holder.textViewFileSize1 = (TextView) v.findViewById(R.id.contact_grid_filesize1);
			holder.textViewFileSize2 = (TextView) v.findViewById(R.id.contact_grid_filesize2);
			
			holder.imageButtonThreeDots1 = (ImageButton) v.findViewById(R.id.contact_grid_three_dots1);
			holder.imageButtonThreeDots2 = (ImageButton) v.findViewById(R.id.contact_grid_three_dots2);
			
			holder.optionsLayout1 = (RelativeLayout) v.findViewById(R.id.contact_grid_options1);
			holder.optionProperties1 = (ImageButton) v.findViewById(R.id.contact_grid_option_properties1);
			holder.optionProperties1.setPadding(Util.px2dp((50*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
			holder.optionSend1 = (ImageButton) v.findViewById(R.id.contact_grid_option_send1);
			holder.optionSend1.setPadding(Util.px2dp((50*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
			holder.optionRemove1 = (ImageButton) v.findViewById(R.id.contact_grid_option_remove1);
			holder.optionRemove1.setPadding(Util.px2dp((50*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), Util.px2dp((30*scaleW), outMetrics), 0);
			holder.arrowSelection1 = (ImageView) v.findViewById(R.id.contact_grid_arrow_selection1);
			holder.arrowSelection1.setVisibility(View.GONE);

			holder.optionsLayout2 = (RelativeLayout) v.findViewById(R.id.contact_grid_options2);
			holder.optionProperties2 = (ImageButton) v.findViewById(R.id.contact_grid_option_properties2);
			holder.optionProperties2.setPadding(Util.px2dp((50*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
			holder.optionSend2 = (ImageButton) v.findViewById(R.id.contact_grid_option_send2);
			holder.optionSend2.setPadding(Util.px2dp((50*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
			holder.optionRemove2 = (ImageButton) v.findViewById(R.id.contact_grid_option_remove2);
			holder.optionRemove2.setPadding(Util.px2dp((50*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), Util.px2dp((50*scaleW), outMetrics), 0);
			holder.arrowSelection2 = (ImageView) v.findViewById(R.id.contact_grid_arrow_selection2);
			holder.arrowSelection2.setVisibility(View.GONE);
			
			v.setTag(holder);
			
			holder.currentPosition = position;
			
			if (position == 2){
            	holder.statusImage1.setImageResource(R.drawable.contact_yellow_dot);
            	holder.statusImage2.setImageResource(R.drawable.contact_red_dot);
            }
			else{
				holder.statusImage1.setImageResource(R.drawable.contact_green_dot);
            	holder.statusImage2.setImageResource(R.drawable.contact_green_dot);
			}

			MegaUser contact1 = (MegaUser) getItem(position);
			holder.imageView1.setImageResource(R.drawable.jesus);
			holder.textViewFileName1.setText(contact1.getEmail());
			
			MegaUser contact2;
			if (position < (getCount()-1)){
				contact2 = (MegaUser) getItem(position+1);
				holder.imageView2.setImageResource(R.drawable.jesus);
				holder.textViewFileName2.setText(contact2.getEmail());	
				holder.itemLayout2.setVisibility(View.VISIBLE);
			}
			else{
				holder.itemLayout2.setVisibility(View.GONE);
			}
			
			holder.imageButtonThreeDots1.setTag(holder);
			holder.imageButtonThreeDots1.setOnClickListener(this);
			
			holder.imageButtonThreeDots2.setTag(holder);
			holder.imageButtonThreeDots2.setOnClickListener(this);
			
			if (positionClicked != -1){
				if (positionClicked == position){
					holder.arrowSelection1.setVisibility(View.VISIBLE);
					LayoutParams params = holder.optionsLayout1.getLayoutParams();
					params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
					ListView list = (ListView) parent;
					list.smoothScrollToPosition(_position);
					holder.arrowSelection2.setVisibility(View.GONE);
					LayoutParams params2 = holder.optionsLayout2.getLayoutParams();
					params2.height = 0;
				}
				else if (positionClicked == (position+1)){
					holder.arrowSelection2.setVisibility(View.VISIBLE);
					LayoutParams params = holder.optionsLayout2.getLayoutParams();
					params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
					ListView list = (ListView) parent;
					list.smoothScrollToPosition(_position);
					holder.arrowSelection1.setVisibility(View.GONE);
					LayoutParams params1 = holder.optionsLayout1.getLayoutParams();
					params1.height = 0;
				}
				else{
					holder.arrowSelection1.setVisibility(View.GONE);
					LayoutParams params1 = holder.optionsLayout1.getLayoutParams();
					params1.height = 0;
					
					holder.arrowSelection2.setVisibility(View.GONE);
					LayoutParams params2 = holder.optionsLayout2.getLayoutParams();
					params2.height = 0;
				}
			}
			else{
				holder.arrowSelection1.setVisibility(View.GONE);
				LayoutParams params1 = holder.optionsLayout1.getLayoutParams();
				params1.height = 0;
				
				holder.arrowSelection2.setVisibility(View.GONE);
				LayoutParams params2 = holder.optionsLayout2.getLayoutParams();
				params2.height = 0;
			}
			
			holder.optionProperties1.setTag(holder);
			holder.optionProperties1.setOnClickListener(this);
			
			holder.optionProperties2.setTag(holder);
			holder.optionProperties2.setOnClickListener(this);
		}
		else{
			v = inflater.inflate(R.layout.item_file_empty_grid, parent, false);
		}
		
		return v;
	}

	@Override
    public int getCount() {
        return contacts.size();
    }
 
    @Override
    public Object getItem(int position) {
        return contacts.get(position);
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

		switch (v.getId()){
			case R.id.contact_grid_option_properties1:{
				MegaUser c = (MegaUser) getItem(currentPosition);
				Intent i = new Intent(context, ContactPropertiesActivity.class);
				i.putExtra("imageId", R.drawable.jesus);
				i.putExtra("name", c.getEmail());
				i.putExtra("position", currentPosition);
				context.startActivity(i);							
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.contact_grid_option_properties2:{
				MegaUser c = (MegaUser) getItem(currentPosition+1);
				Intent i = new Intent(context, ContactPropertiesActivity.class);
				i.putExtra("imageId", R.drawable.jesus);
				i.putExtra("name", c.getEmail());
				i.putExtra("position", currentPosition+1);
				context.startActivity(i);							
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.contact_grid_three_dots1:{
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
			case R.id.contact_grid_three_dots2:{
				if (positionClicked == -1){
					positionClicked = currentPosition+1;
					notifyDataSetChanged();
				}
				else{
					if (positionClicked == (currentPosition+1)){
						positionClicked = -1;
						notifyDataSetChanged();
					}
					else{
						positionClicked = currentPosition+1;
						notifyDataSetChanged();
					}
				}
				break;
			}
			case R.id.contact_grid_thumbnail1:{
				Intent i = new Intent(context, ContactPropertiesActivity.class);
				MegaUser contact = (MegaUser) getItem(currentPosition);
//				ItemContact rowItem = (ItemContact) getItem(currentPosition);
				i.putExtra("imageId", R.drawable.jesus);
				i.putExtra("name", contact.getEmail());
				i.putExtra("position", currentPosition);
				context.startActivity(i);
				break;
			}
			case R.id.contact_grid_thumbnail2:{
				Intent i = new Intent(context, ContactPropertiesActivity.class);
				MegaUser contact = (MegaUser) getItem(currentPosition+1);
//				ItemContact rowItem = (ItemContact) getItem(currentPosition+1);
				i.putExtra("imageId", R.drawable.jesus);
				i.putExtra("name", contact.getEmail());
				i.putExtra("position", currentPosition+1);
				context.startActivity(i);
				break;
			}
		}
	}
	
	public void setContacts (ArrayList<MegaUser> contacts){
		this.contacts = contacts;
		positionClicked = -1;
		notifyDataSetChanged();
	}
	
	private static void log(String log) {
		Util.log("MegaContactsGridAdapter", log);
	}
}
