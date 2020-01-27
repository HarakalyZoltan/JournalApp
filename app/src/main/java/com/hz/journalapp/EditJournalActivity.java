package com.hz.journalapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class EditJournalActivity extends AppCompatActivity implements View.OnClickListener{
    private static final int GALLERY_CODE = 1;
    private Button saveButton;
    private Button cancelButton;
    private ProgressBar progressBar;
    private ImageView addPhotoButton;
    private EditText titleEditText;
    private EditText thoughtsEditText;
    private TextView currentUserTextView;
    private ImageView imageView;
    private TextView timeTextView;

    private String currentUserId;
    private String currentUserName;
    private String documentId;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser user;

    //Connection to FireStore
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private StorageReference storageReference;

    private CollectionReference collectionReference = db.collection("Journal");
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_journal);

        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.edit_progressBar);
        titleEditText = findViewById(R.id.edit_title_et);
        thoughtsEditText = findViewById(R.id.edit_description_et);
        currentUserTextView = findViewById(R.id.edit_username_textview);
        timeTextView = findViewById(R.id.edit_date_textview);
        cancelButton = findViewById(R.id.edit_cancel_journal_button);
        imageView = findViewById(R.id.edit_imageView);
        saveButton = findViewById(R.id.edit_save_journal_button);
        addPhotoButton = findViewById(R.id.editCameraButton);

        saveButton.setOnClickListener(this);
        addPhotoButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        progressBar.setVisibility(View.INVISIBLE);

        titleEditText.setText(getIntent().getStringExtra("title"));
        thoughtsEditText.setText(getIntent().getStringExtra("thought"));
        currentUserTextView.setText(getIntent().getStringExtra("username"));
        timeTextView.setText(getIntent().getStringExtra("timeAdded"));

        imageUri = Uri.parse(getIntent().getStringExtra("image"));
        imageView.setImageURI(imageUri);
        documentId = getIntent().getStringExtra("documentId");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.edit_save_journal_button:
                //saveJournal
                saveJournal();
                startActivity(new Intent(this, JournalListActivity.class));
                finish();
                break;
            case R.id.editCameraButton:
                //get image from gallery/phone
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_CODE);
                break;
            case R.id.edit_cancel_journal_button:
                startActivity(new Intent(this, JournalListActivity.class));
                finish();
                break;
        }
    }

    private void saveJournal() {
        final String title = titleEditText.getText().toString().trim();
        final String thoughts = thoughtsEditText.getText().toString().trim();
        progressBar.setVisibility(View.VISIBLE);

        final StorageReference filepath = storageReference  //.../journal_images/our_image.jpg
                .child("journal_images")
                .child("my_image_" + Timestamp.now().getSeconds()); // my_image_16165
        filepath.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        progressBar.setVisibility(View.INVISIBLE);
                        filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String imageUrl = uri.toString();
                                DocumentReference documentReference = db.collection("Journal").document(documentId);
                                documentReference.update("title", title);
                                documentReference.update("thought", thoughts);
                                documentReference.update("imageUrl", imageUrl);
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                imageUri = data.getData();
                imageView.setImageURI(imageUri);
            }
        }
    }
}
