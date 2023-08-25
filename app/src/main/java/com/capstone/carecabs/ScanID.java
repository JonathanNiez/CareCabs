package com.capstone.carecabs;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScanID extends AppCompatActivity {
    private ImageButton imgBackBtn, getImageBtn;
    private TextView recognizedTextView;
    private LinearLayout progressBarLayout;
    private ImageView idPreview;
    private Uri imageUri;
    private AlertDialog.Builder builder;
    private AlertDialog optionsDialog;
    private TextRecognizer textRecognizer;
    private static final int CAMERA_REQUEST_CODE = 1;
    private static final int GALLERY_REQUEST_CODE = 2;
    private static final int CAMERA_PERMISSION_REQUEST = 101;
    private static final int STORAGE_PERMISSION_REQUEST = 102;

    private String TAG = "ScanID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_id);

        imgBackBtn = findViewById(R.id.imgBackBtn);
        getImageBtn = findViewById(R.id.getImageBtn);
        recognizedTextView = findViewById(R.id.recognizedTextView);
        idPreview = findViewById(R.id.idPreview);
        progressBarLayout = findViewById(R.id.progressBarLayout);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        checkPermission();

        getImageBtn.setOnClickListener(v -> {
//            ImagePicker.with(this)
//                    .crop()                    //Crop image(Optional), Check Customization for more option
//                    .compress(1024)            //Final image size will be less than 1 MB(Optional)
//                    .maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
//                    .start();
//
            showOptionsDialog();
        });
    }

    private void checkPermission() {
        // Check for camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        }

        // Check for storage permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST);
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        if (galleryIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
            if (optionsDialog != null && optionsDialog.isShowing()) {
                optionsDialog.dismiss();
            }

        } else {
            Toast.makeText(this, "No gallery app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            if (optionsDialog != null && optionsDialog.isShowing()) {
                optionsDialog.dismiss();
            }

        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void showOptionsDialog() {
        builder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.camera_gallery_dialog, null);

        Button openCameraBtn = dialogView.findViewById(R.id.openCameraBtn);
        Button openGalleryBtn = dialogView.findViewById(R.id.openGalleryBtn);
        Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);

        openCameraBtn.setOnClickListener(v -> {
            openCamera();
        });

        openGalleryBtn.setOnClickListener(v -> {
            openGallery();
        });

        cancelBtn.setOnClickListener(v -> {
            if (optionsDialog != null && optionsDialog.isShowing()) {
                optionsDialog.dismiss();
            }
        });

        builder.setView(dialogView);

        optionsDialog = builder.create();
        optionsDialog.show();
    }

    private boolean matchesPattern(String text, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(text);
        return m.find();
    }

    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            if (resultCode == RESULT_OK) {
                if (requestCode == CAMERA_REQUEST_CODE) {
                    if (data != null) {

                        Bundle extras = data.getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");

                        imageUri = getImageUri(getApplicationContext(), imageBitmap);
                        idPreview.setImageURI(imageUri);

                        Toast.makeText(this, "Image is Loaded from Camera", Toast.LENGTH_LONG).show();

                        recognizeText();

                    } else {
                        Toast.makeText(this, "Image is not Selected", Toast.LENGTH_LONG).show();
                        progressBarLayout.setVisibility(View.GONE);
                    }

                } else if (requestCode == GALLERY_REQUEST_CODE) {
                    if (data != null) {

                        imageUri = data.getData();
                        idPreview.setImageURI(imageUri);

                        Toast.makeText(this, "Image is Loaded from Gallery", Toast.LENGTH_LONG).show();

                        recognizeText();
                    } else {
                        Toast.makeText(this, "Image is not Selected", Toast.LENGTH_LONG).show();
                        progressBarLayout.setVisibility(View.GONE);
                    }
                }
            } else {
                Toast.makeText(this, "Image is not Selected", Toast.LENGTH_SHORT).show();
                Log.e(TAG + ":ERROR", "Failed to Extract Text");

                Toast.makeText(this, "Failed to Extract Text", Toast.LENGTH_LONG).show();

                progressBarLayout.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void recognizeText() {
        progressBarLayout.setVisibility(View.VISIBLE);

        if (imageUri != null) {

            try {
                InputImage inputImage = InputImage.fromFilePath(ScanID.this, imageUri);

                textRecognizer.process(inputImage).addOnSuccessListener(text -> {

                    String extractedText = text.getText();

                    recognizedTextView.setText(extractedText);

                    progressBarLayout.setVisibility(View.GONE);

                    // Define patterns for PWD and Senior Citizen IDs
                    String pwdPattern = "PWD-[0-9]{4}";
                    String seniorCitizenPattern = "SeniorCitizen-[0-9]{4}";

                    // Check if extracted text matches patterns
                    if (matchesPattern(extractedText, pwdPattern)) {
                        recognizedTextView.setText(extractedText);
                    } else if (matchesPattern(extractedText, seniorCitizenPattern)) {
                        recognizedTextView.setText(extractedText);
                    } else {
                        recognizedTextView.setText("Please use an ID");
                    }


                }).addOnFailureListener(e -> {
                    Toast.makeText(ScanID.this, e.getMessage(), Toast.LENGTH_SHORT).show();

                    e.printStackTrace();

                    recognizedTextView.setText("Please try again");

                    progressBarLayout.setVisibility(View.GONE);
                });


            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG + ":ERROR", "Failed to Extract Text");

                progressBarLayout.setVisibility(View.GONE);

            }
        } else {
            Toast.makeText(this, "Image is not loaded", Toast.LENGTH_LONG).show();
            recognizedTextView.setText("Please try again");
            Toast.makeText(this, "Failed to Extract Text", Toast.LENGTH_LONG).show();

            progressBarLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Camera Permission Granted");
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Gallery Permission Granted");
            }
        } else {
            Log.e(TAG, "Permission Denied");

        }
    }
}