package com.devtech.Snap_Seeker;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.devtech.snaptextconverter.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class MainActivity extends AppCompatActivity {

    //UI Views
    private MaterialButton inputImageBtn;
    private MaterialButton convertTextBtn;
    private ShapeableImageView imageIv;
    private EditText convertedTextEt;

    //Main TAG
    private static final String TAG = "MAIN_TAG";

    //Uri of the image that we will take from Camera/Gallery
    private Uri imageUri = null;

    //Handling the result of camera & Gallery permissions
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 101;

    //Arrays permission required to pick image from camera and gallery
    private String[] cameraPermission;
    private String[] storagePermission;

    //Progress dialog
    ProgressDialog progressDialog;

    //Text Recognizer
    private TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //init UI
        inputImageBtn = findViewById(R.id.inputImageBtn);
        convertTextBtn = findViewById(R.id.convertTextBtn);
        imageIv = findViewById(R.id.imageIv);
        convertedTextEt = findViewById(R.id.convertedTextEt);

        //init arrays of permission required for camera, gallery
        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);


        //Init Text Recognizer
        textRecognizer= TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        //Handle click, shows input image dialog
        inputImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputImageDialog();
            }
        });

        convertTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(imageUri == null){

                    Toast.makeText(MainActivity.this, "Pick image first", Toast.LENGTH_SHORT).show();
                }
                else{

                    convertTextFromImage();
                }

            }
        });
    }

    private void convertTextFromImage() {

        Log.d(TAG, "convertTextFromImage: ");
        progressDialog.setMessage("Preparing image...");
        progressDialog.show();


        try {
            InputImage inputImage = InputImage.fromFilePath(this, imageUri);

            progressDialog.setMessage("Recognizing text...");

            Task<Text> textTaskResult = textRecognizer.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {

                            progressDialog.dismiss();
                            String recognizedText = text.getText();
                            Log.d(TAG, "onSuccess: recognizedText: "+recognizedText);
                            convertedTextEt.setText(recognizedText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            //Failed recognizing text from image, dismiss dialog, show reason in toast
                            progressDialog.dismiss();
                            Log.e(TAG, "onFailure: ", e);
                            Toast.makeText(MainActivity.this,"Failed converting text due to" +e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            //Exception occurred while preparing inputImage, dismiss dialog, show reason in toast
            progressDialog.dismiss();
            Log.e(TAG, "recognizeTextFromImage: ", e);
            Toast.makeText(this, "Failed preparing image due to" +e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showInputImageDialog() {
        PopupMenu popupMenu = new PopupMenu(this, inputImageBtn);

        //Add items Camera, Gallery to PopUpMenu
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "CAMERA");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "GALLERY");

        //Show popupMenu
        popupMenu.show();

        //Handle popupMenu items Click
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                //Get item id that is clicked from PopupMenu
                int id = menuItem.getItemId();
                if(id == 1){

                    //Camera is click, check if camera permissions are granted or not
                    Log.d(TAG,"onMenuItemClick: Camera Clicked...");
                    if(checkCameraPermissions()){

                        //Camera granted, we can launch camera Intent
                        pickImageCamera();
                    }
                    else{

                        //Camera permission not granted, Can't launch camera intent, request permission
                        requestCameraPermission();
                    }
                }
                else if(id == 2){

                    //Gallery is clicked, check if storage permissions is granted or not
                    Log.d(TAG,"onMenuItemClick: Gallery Clicked...");
                    if(checkStoragePermission()){

                        //Galley granted, we can launch galley Intent
                        pickImageGallery();;
                    }
                    else{

                        //Gallery permission not granted, Can't launch gallery intent, request permission
                        requestStoragePermission();
                    }
                }
                return false;
            }
        });

    }

    private void pickImageGallery() {
        Log.d(TAG, "pickImageGallery: ");
        //intent to pick images from gallery, will show all resources from where we can pick image
        Intent intent = new Intent(Intent.ACTION_PICK);
        //set type of image we want to make
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //Here we will pick the image , if picked
                    if(result.getResultCode() == Activity.RESULT_OK){
                        //image picked
                        Intent data = result.getData();
                        imageUri = data.getData();
                        Log.d(TAG, "onActivityResult: imageUri "+imageUri);
                        imageIv.setImageURI(imageUri);
                    }
                    else{
                        Log.d(TAG, "onActivityResult: ");
                        //cancelled
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }

                }
            }
    );

        //Open Camera intent
    private void pickImageCamera(){
        Log.d(TAG, "pickImageCamera: ");
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Sample Title");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);

    }
    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //here we will receive image, if taken from camera
                    if(result.getResultCode() == Activity.RESULT_OK){
                        //image already taken by camera
                        //We already have the image in imageUri using function pickImageCamera()
                        Log.d(TAG, "onActivityResult: imageUri " + imageUri);
                        imageIv.setImageURI(imageUri);
                    }
                    else{
                        //cancelled
                        Log.d(TAG, "onActivityResult: cancelled");
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
    
                    }

                }
            }
    );

    private boolean checkStoragePermission() {

        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return result;
    }

    private void requestStoragePermission() {

        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermissions(){

        boolean cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return cameraResult && storageResult;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    //Handle permission Results


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case CAMERA_REQUEST_CODE:{

                //Check if some actions from permissions dialog performed or not Allow/Deny
                if(grantResults.length>0){

                    //Check if Camera, Storage Permissions granted, contains boolean results either True/False
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    //Check if both permissions are granted or not
                    if(cameraAccepted && storageAccepted){

                        //Both permissions granted cameraAccepted && storageAccepted, we can launch Camera intent
                        pickImageCamera();
                    }
                    else{

                        //One or both permission are denied, Can't launch camera intent
                        Toast.makeText(this, "Camera & Storage Permissions are Required", Toast.LENGTH_SHORT).show();
                    }
                }
                else{

                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                }
            }

            break;
            case STORAGE_REQUEST_CODE:{

                //Check if some actions from permissions dialog performed or not Allow/Deny
                if(grantResults.length>0){

                    //Check if Camera, Storage Permissions granted, contains boolean results either True/False
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if(storageAccepted){

                        //Storage Permission Granted, we can launch gallery intent
                        pickImageGallery();

                    }
                    else{

                        //Storage Permission Denied, we can't launch gallery intent
                        Toast.makeText(this, "Storage Permission is Required", Toast.LENGTH_SHORT).show();

                    }
                }

            }
            break;
        }
    }
}