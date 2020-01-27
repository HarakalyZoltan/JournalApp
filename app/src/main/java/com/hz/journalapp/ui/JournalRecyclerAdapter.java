package com.hz.journalapp.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.hz.journalapp.EditJournalActivity;
import com.hz.journalapp.R;
import com.hz.journalapp.model.Journal;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class JournalRecyclerAdapter extends RecyclerView.Adapter<JournalRecyclerAdapter.ViewHolder> {
    private Context context;
    private List<Journal> journalList;

    public JournalRecyclerAdapter(Context context, List<Journal> journalList) {
        this.context = context;
        this.journalList = journalList;
    }

    @NonNull
    @Override
    public JournalRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.journal_row, viewGroup, false);
        return new ViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull final JournalRecyclerAdapter.ViewHolder viewHolder, final int position) {
        final Journal journal = journalList.get(position);
        String imageUrl;

        viewHolder.title.setText(journal.getTitle());
        viewHolder.thoughts.setText(journal.getThought());
        viewHolder.name.setText(journal.getUserName());
        imageUrl = journal.getImageUrl();
        String timeAgo = (String) DateUtils.getRelativeTimeSpanString(journal.getTimeAdded().getSeconds() * 1000);

        /*
        Use Picasso library to download and show image
         */
        Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.common_full_open_on_phone)
                .fit()
                .into(viewHolder.image);
        viewHolder.dateAdded.setText(timeAgo);

        viewHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewHolder.collectionReference.get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                for ( QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                                    Journal currentJournal = snapshot.toObject(Journal.class);

                                    if (currentJournal.getUserId().equals(journal.getUserId())
                                            && currentJournal.getTimeAdded().equals(journal.getTimeAdded())) {
                                        viewHolder.collectionReference.document(snapshot.getId()).delete();
                                        notifyItemRemoved(position);
                                        journalList.remove(position);
                                    }
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(context, "Failure", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
        viewHolder.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                BitmapDrawable bitmapDrawable = ((BitmapDrawable) viewHolder.image.getDrawable());
                Bitmap bitmap = bitmapDrawable.getBitmap();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
                Uri bitmapUri = Uri.parse(path);

                final Intent intent = new Intent(context, EditJournalActivity.class);
                intent.putExtra("username", journal.getUserName());
                intent.putExtra("thought", journal.getThought());
                intent.putExtra("title", journal.getTitle());
                intent.putExtra("timeAdded", journal.getTimeAdded().toDate().toString());
                intent.putExtra("image", bitmapUri.toString());

                viewHolder.collectionReference.get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                for ( QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                                    Journal currentJournal = snapshot.toObject(Journal.class);

                                    if (currentJournal.getUserId().equals(journal.getUserId())
                                            && currentJournal.getTimeAdded().equals(journal.getTimeAdded())) {
                                        viewHolder.documentId = snapshot.getId();
                                        intent.putExtra("documentId", viewHolder.documentId);
                                    }
                                }
                                context.startActivity(intent);
                                ((AppCompatActivity)context).finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(context, "Failure", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    @Override
    public int getItemCount() {
        return journalList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public TextView
                title,
                thoughts,
                dateAdded,
                name;
        public ImageView image;
        public ImageButton shareButton;
        public ImageButton editButton;
        public ImageButton deleteButton;
        public String documentId;

        //FireStore
        private FirebaseFirestore db = FirebaseFirestore.getInstance();
        private CollectionReference collectionReference = db.collection("Journal");

        public ViewHolder(@NonNull final View itemView, final Context ctx) {
            super(itemView);
            context = ctx;
            title = itemView.findViewById(R.id.journal_title_list);
            thoughts = itemView.findViewById(R.id.journal_thought_list);
            dateAdded = itemView.findViewById(R.id.journal_timeStamp_list);
            image = itemView.findViewById(R.id.journal_image_list);
            name = itemView.findViewById(R.id.journal_row_username);
            shareButton = itemView.findViewById(R.id.journal_row_share_button);
            editButton = itemView.findViewById(R.id.journal_row_edit_button);
            deleteButton = itemView.findViewById(R.id.journal_row_delete_button);

            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    BitmapDrawable bitmapDrawable = ((BitmapDrawable) image.getDrawable());
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                    String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
                    Uri bitmapUri = Uri.parse(path);

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/jpeg");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, title.getText().toString().trim());
                    shareIntent.putExtra(Intent.EXTRA_TEXT, thoughts.getText().toString().trim());
                    context.startActivity(Intent.createChooser(shareIntent, "Share this!"));
                }
            });
        }
    }
}
