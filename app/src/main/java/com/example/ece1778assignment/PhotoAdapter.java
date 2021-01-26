package com.example.ece1778assignment;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder>{

    List<String> photoPaths;
    List<Integer> tmpPhotos;
    Context context;
    LayoutInflater inflater;

//    public PhotoAdapter(Context context, List<String> photoPaths){
//        this.context = context;
//        this.photoPaths = photoPaths;
//        this.inflater = LayoutInflater.from(context);
//    }

    public PhotoAdapter(Context context, List<Integer> tmpPhotos){
        this.context = context;
        this.tmpPhotos = tmpPhotos;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.image_display, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.photo.setImageResource(tmpPhotos.get(position));
    }

    @Override
    public int getItemCount() {
        return tmpPhotos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public ImageView photo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.imageView2);
        }
    }

}
