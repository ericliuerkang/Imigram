package com.example.ece1778assignment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder>{

    private final String TAG = "PhotoAdapter";

    List<String> photoPaths;
    List<Post> tmpPhotos;
    Context context;
    LayoutInflater inflater;

    StorageReference storageRef = FirebaseStorage.getInstance().getReference();

//    public PhotoAdapter(Context context, List<String> photoPaths){
//        this.context = context;
//        this.photoPaths = photoPaths;
//        this.inflater = LayoutInflater.from(context);
//    }

    public PhotoAdapter(Context context, List<Post> tmpPhotos){
        this.context = context;
        this.tmpPhotos = tmpPhotos;
        this.inflater = LayoutInflater.from(context);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public ImageView photo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.imageView2);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int mPosition = getLayoutPosition();
            String uid = tmpPhotos.get(mPosition).uid;
            String path = tmpPhotos.get(mPosition).path;
            String caption = tmpPhotos.get(mPosition).caption;
            String hashtag = tmpPhotos.get(mPosition).hashtag;
            String comments = tmpPhotos.get(mPosition).comments;

            Intent intent = new Intent(context, ImageDisplay.class);
            intent.putExtra("uid", uid);
            intent.putExtra("imagePath", path);
            intent.putExtra("caption", caption);
            intent.putExtra("hashtag", hashtag);
            intent.putExtra("comment", comments);
            context.startActivity(intent);
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.image_display, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(TAG, "Position: "+String.valueOf(position));
        String path = tmpPhotos.get(position).path;

        ImageView view = holder.photo;
        StorageReference storageReference = storageRef.child(path);

        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Log.d(TAG, "Successfully loaded uri");
                Glide.with(view.getContext() /* context */)
                        .load(uri)
                        .into(view);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d("Profile Activity", "Failed to load uri");
            }
        });
//        holder.photo.setImageBitmap(tmpPhotos.get(position));
    }

    @Override
    public int getItemCount() {
        return tmpPhotos.size();
    }
}
