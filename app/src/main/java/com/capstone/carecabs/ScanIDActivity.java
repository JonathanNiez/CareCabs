package com.capstone.carecabs;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.capstone.carecabs.BottomSheetModal.SettingsBottomSheet;
import com.capstone.carecabs.Firebase.FirebaseMain;
import com.capstone.carecabs.Utility.NetworkChangeReceiver;
import com.capstone.carecabs.Utility.NetworkConnectivityChecker;
import com.capstone.carecabs.Utility.SimilarityClassifier;
import com.capstone.carecabs.Utility.StaticDataPasser;
import com.capstone.carecabs.Utility.VoiceAssistant;
import com.capstone.carecabs.databinding.ActivityScanIdBinding;
import com.capstone.carecabs.databinding.DialogNotAnIdBinding;
import com.capstone.carecabs.ml.IdScanV2;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.text.TextRecognizer;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ScanIDActivity extends AppCompatActivity implements
		SettingsBottomSheet.FontSizeChangeListener {
	private final String TAG = "ScanID";
	private ActivityScanIdBinding binding;
	private String idPictureURL = "none";
	private Uri idPictureUri;
	private String getUserType;
	private static final float DEFAULT_TEXT_SIZE_SP = 17;
	private static final float DEFAULT_HEADER_TEXT_SIZE_SP = 20;
	private static final float INCREASED_TEXT_SIZE_SP = DEFAULT_TEXT_SIZE_SP + 5;
	private static final float INCREASED_TEXT_HEADER_SIZE_SP = DEFAULT_HEADER_TEXT_SIZE_SP + 5;
	private final int imageSize = 224;
	private boolean isUserVerified = false;
	private AlertDialog.Builder builder;
	private AlertDialog cancelScanIDDialog, notAnIDDialog,
			noInternetDialog, uploadClearIDPictureDialog, pleaseWaitDialog,
			verificationSuccessDialog;
	private TextRecognizer textRecognizer;
	private static final int CAMERA_PERMISSION_REQUEST_CODE = 69;
	private static final int STORAGE_PERMISSION_REQUEST_CODE = 420;
	private Intent intent;
	private NetworkChangeReceiver networkChangeReceiver;
	private DocumentReference documentReference;
	private String voiceAssistantState = StaticDataPasser.storeVoiceAssistantState;
	private String fontSize = StaticDataPasser.storeFontSize;
	private VoiceAssistant voiceAssistant;
	private ProcessCameraProvider cameraProvider;
	private int CAMERA_FACING = CameraSelector.LENS_FACING_FRONT; //default front cam
	private int RECOGNIZE_FACE_IN_ID = 69;
	private final int UPLOAD_ID_FROM_GALLERY = 420;
	private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
	private HashMap<String, SimilarityClassifier.Recognition> registeredFace = new HashMap<>(); //saved Faces
	private boolean startFaceDetecting = true, flipX = false, isFaceMatched = false;
	private int faceRecognizedThreshold = 0;
	private int faceNotRecognizedThreshold = 0;
	private int[] intValues;
	private float distance = 1.0f;
	private int inputSize = 112;  //Input size for model
	private float[][] embeddings;
	private float IMAGE_MEAN = 128.0f;
	private float IMAGE_STD = 128.0f;
	private int OUTPUT_SIZE = 192; //Output size of model
	private CameraSelector cameraSelector;
	private Interpreter tfLite;
	private String modelFile = "mobile_face_net.tflite"; //model name
	private FaceDetector faceDetector;
	private boolean isModelQuantized = false;

	@Override
	protected void onPause() {
		super.onPause();

		closeCancelScanIDDialog();
		closeNoInternetDialog();
		closePleaseWaitDialog();
		closeVerificationSuccessDialog();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (networkChangeReceiver != null) {
			unregisterReceiver(networkChangeReceiver);
		}

		closeCancelScanIDDialog();
		closeNoInternetDialog();
		closePleaseWaitDialog();
		closeVerificationSuccessDialog();
	}

	@Override
	protected void onStart() {
		super.onStart();

		initializeNetworkChecker();
	}

	@SuppressLint("SetTextI18n")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityScanIdBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.idAlreadyScannedLayout.setVisibility(View.GONE);
		binding.faceRecognitionLayout.setVisibility(View.GONE);
		binding.backFloatingBtn.setVisibility(View.GONE);
		binding.doneBtn.setVisibility(View.GONE);
		binding.idVerifiedLayout.setVisibility(View.GONE);
		binding.matchYourFaceTextView.setVisibility(View.GONE);
		binding.tryAgainBtn.setVisibility(View.GONE);

		FirebaseApp.initializeApp(this);

		registeredFace = readFromSP();
		checkIfUserIsVerified();
		checkCameraAndStoragePermission();
		getUserSettings();

		SharedPreferences sharedPref = getSharedPreferences("Distance", Context.MODE_PRIVATE);
		distance = sharedPref.getFloat("distance", 1.00f);

		if (getIntent() != null) {
			intent = getIntent();
			getUserType = intent.getStringExtra("userType");
			String activityData = intent.getStringExtra("activityData");

			if (getUserType != null) {

				if (activityData != null) {
					if (activityData.equals("fromMyProfile")) {
						binding.backFloatingBtn.setOnClickListener(v -> {
							if (isUserVerified) {
								backToMyProfile();
							} else {
								showCancelScanIDDialog();
							}
						});
					} else {
						binding.backFloatingBtn.setOnClickListener(v -> {
							if (isUserVerified) {
								goToMainActivity();
							} else {
								showCancelScanIDDialog();
							}
						});
					}
				}

				switch (getUserType) {
					case "Driver":
						binding.scanYourIDTypeTextView.setText("Scan your Driver's license");

						break;

					case "Senior Citizen":
						binding.scanYourIDTypeTextView.setText("Scan your Senior Citizen ID that is validated by OSCA");

						break;

					case "Person with Disabilities (PWD)":
						binding.scanYourIDTypeTextView.setText("Scan your PWD ID");

						break;
				}

			}
		}

		binding.idFaceImageView.setOnClickListener(v -> {
			showRegisteredFaceList();
		});

		binding.switchCameraBtn.setOnClickListener(v -> {
			if (CAMERA_FACING == CameraSelector.LENS_FACING_BACK) {
				CAMERA_FACING = CameraSelector.LENS_FACING_FRONT;
				flipX = true;
			} else {
				CAMERA_FACING = CameraSelector.LENS_FACING_BACK;
				flipX = false;
			}
			cameraProvider.unbindAll();
			bindCamera();
		});

		binding.tryAgainBtn.setOnClickListener(v -> {
			binding.facePreviewImageView.setImageResource(0);
			binding.idFaceImageView.setImageResource(0);
			binding.idImageView.setImageResource(0);
			binding.idVerifiedLayout.setVisibility(View.GONE);
			binding.matchYourFaceTextView.setVisibility(View.GONE);

			fadeOutAnimation(binding.faceRecognitionLayout);

			if (isUserVerified)
				reverseAnimateLayoutSize(binding.idAlreadyScannedLayout);

			else
				reverseAnimateLayoutSize(binding.idScanLayout);

			clearRegisteredFace();
		});

		binding.scanLaterBtn.setOnClickListener(v -> updateUserToNotVerified());

		binding.settingsFloatingBtn.setOnClickListener(v -> showSettingsBottomSheet());

		binding.scanIDTextView.setOnClickListener(v -> {
			int scrollBottom = binding.scrollView.getChildAt(0).getHeight() - binding.scrollView.getHeight();
			ObjectAnimator.ofInt(binding.scrollView, "scrollY", scrollBottom)
					.setDuration(1500)
					.start();
		});

//		binding.facePreviewImageView.setOnClickListener(v -> getDetectedFace());


		binding.idScanLayout.setOnClickListener(v -> {
			startFaceDetecting = false;
			ImagePicker.with(ScanIDActivity.this)
					.crop()                    //Crop image(Optional), Check Customization for more option
					.compress(1024)            //Final image size will be less than 1 MB(Optional)
					.maxResultSize(1080, 1080)    //Final image resolution will be less than 1080 x 1080(Optional)
					.start(UPLOAD_ID_FROM_GALLERY);
		});

		binding.idAlreadyScannedLayout.setOnClickListener(v -> {
			startFaceDetecting = false;
			ImagePicker.with(ScanIDActivity.this)
					.crop()
					.compress(1024)
					.maxResultSize(1080, 1080)
					.start(UPLOAD_ID_FROM_GALLERY);
		});
	}

	@Override
	public void onBackPressed() {
		if (getIntent().hasExtra("activityData")) {
			backToMyProfile();
		} else {
			if (isUserVerified) {
				goToMainActivity();
				super.onBackPressed();

			} else {
				showCancelScanIDDialog();
			}
		}
	}

	private void showSettingsBottomSheet() {
		SettingsBottomSheet settingsBottomSheet = new SettingsBottomSheet();
		settingsBottomSheet.setFontSizeChangeListener(this);
		settingsBottomSheet.show(getSupportFragmentManager(), settingsBottomSheet.getTag());
	}

	@SuppressLint("DefaultLocale")
	private void classifyID(Bitmap bitmap, Uri selectedImageUri) {
		try {
			IdScanV2 model = IdScanV2.newInstance(ScanIDActivity.this);

			// Creates inputs for reference.
			TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
			byteBuffer.order(ByteOrder.nativeOrder());

			int[] intValues = new int[imageSize * imageSize];
			bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
			int pixel = 0;
			for (int n = 0; n < imageSize; n++) {
				for (int i = 0; i < imageSize; i++) {
					int val = intValues[pixel++];
					byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
					byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
					byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
				}
			}
			inputFeature0.loadBuffer(byteBuffer);

			// Runs model inference and gets result.
			IdScanV2.Outputs outputs = model.process(inputFeature0);
			TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
			float[] confidences = outputFeature0.getFloatArray();
			int maxPos = 0;
			float maxConfidence = 0;
			float confidenceThreshold = 0.8F;

			for (int i = 0; i < confidences.length; i++) {
				if (confidences[i] > maxConfidence) {
					maxConfidence = confidences[i];
					maxPos = i;
				}
			}

			String[] classes = {"Driver's License", "Senior Citizen ID", "PWD ID", "Not an ID"};

			if (maxConfidence > confidenceThreshold) {
				String predictedClass = classes[maxPos];

				switch (getUserType) {
					case "Driver":
						handleDriverLicense(predictedClass, selectedImageUri);

						break;

					case "Senior Citizen":
						handleSeniorCitizenID(predictedClass, selectedImageUri);

						break;

					case "Person with Disabilities (PWD)":
						handlePWDID(predictedClass, selectedImageUri);

						break;
				}
			} else {
				showUploadClearIDPictureDialog();
			}

			StringBuilder s = new StringBuilder();
			for (int i = 0; i < classes.length; i++) {
				s.append(String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100));
			}

//			binding.confidenceTextView.setText(s.toString());

			// Releases model resources if no longer used.
			model.close();

		} catch (IOException e) {
			Log.e(TAG, "classifyID: " + e.getMessage());
		}
	}

	@SuppressLint("SetTextI18n")
	private void handleDriverLicense(String predictedClass, Uri selectedImageUri) {
		switch (predictedClass) {
			case "Driver's License":

				showPleaseWaitDialog();
				new Handler().postDelayed(() -> {
					closePleaseWaitDialog();
					initializeFaceDetector();
					animateLayoutSize(binding.idScanParentLayout, 300, 300);
					showIDVerifiedLayout();
					registerTheFaceInID(selectedImageUri);
					binding.idVerifiedTextView.setText("Driver's License Verified");
				}, 2000);

				break;

			case "Senior Citizen ID":
			case "PWD ID":
			case "Not an ID":

				resetImageViewAndShowDialog();

				break;
		}
	}

	private void handleSeniorCitizenID(String predictedClass, Uri selectedImageUri) {
		switch (predictedClass) {
			case "Driver's License":
			case "PWD ID":
			case "Not an ID":

				resetImageViewAndShowDialog();

				break;

			case "Senior Citizen ID":

				showPleaseWaitDialog();
				new Handler().postDelayed(() -> {
					closePleaseWaitDialog();
					initializeFaceDetector();
					animateLayoutSize(binding.idScanParentLayout, 300, 300);
					showIDVerifiedLayout();
					registerTheFaceInID(selectedImageUri);
				}, 2000);

				break;
		}
	}

	private void handlePWDID(String predictedClass, Uri selectedImageUri) {
		switch (predictedClass) {
			case "Driver's License":
			case "Senior Citizen ID":
			case "Not an ID":

				resetImageViewAndShowDialog();

				break;

			case "PWD ID":

				showPleaseWaitDialog();
				new Handler().postDelayed(() -> {
					closePleaseWaitDialog();
					initializeFaceDetector();
					animateLayoutSize(binding.idScanParentLayout, 300, 300);
					showIDVerifiedLayout();
					registerTheFaceInID(selectedImageUri);
				}, 2000);

				break;
		}
	}

	private void registerTheFaceInID(Uri selectedImageUri) {
		//recognize face
		try {
			InputImage inputImage = InputImage
					.fromBitmap(getBitmapFromUri(selectedImageUri), 0);
			faceDetector.process(inputImage)
					.addOnSuccessListener(faces -> {

						if (faces.size() != 0) {
							fadeInAnimation(binding.idScanLayout);
							Face face = faces.get(0);

							Log.i(TAG, "onActivityResult - onSuccess: " + face.toString());

							//write code to recreate bitmap from source
							//Write code to show bitmap to canvas
							Bitmap frame_bmp = null;
							try {
								frame_bmp = getBitmapFromUri(selectedImageUri);
							} catch (IOException e) {
								e.printStackTrace();
							}
							Bitmap frame_bmp1 = rotateBitmap(frame_bmp, 0, flipX, false);
							RectF boundingBox = new RectF(face.getBoundingBox());
							Bitmap cropped_face = getCropBitmapByCPU(frame_bmp1, boundingBox);
							Bitmap scaled = getResizedBitmap(cropped_face, 112, 112);

							binding.idFaceImageView.setImageBitmap(scaled);
							recognizeImage(scaled);
							getDetectedIDFace();
//							getDetectedFace();

							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
								Log.e(TAG, "onActivityResult: " + e.getMessage());
							}
						}
					})
					.addOnFailureListener(e -> {
						startFaceDetecting = true;
						showToast("Image failed to add", 1);
						Log.e(TAG, "onActivityResult: " + e.getMessage());
					});

			binding.facePreviewImageView.setImageBitmap(getBitmapFromUri(selectedImageUri));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initializeFaceDetector() {
		//Load model
		try {
			tfLite = new Interpreter(loadModelFile(ScanIDActivity.this, modelFile));
		} catch (IOException e) {
			e.printStackTrace();
		}

		FaceDetectorOptions highAccuracyOpts =
				new FaceDetectorOptions.Builder()
						.setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
						.build();
		faceDetector = FaceDetection.getClient(highAccuracyOpts);

		bindCamera();
	}

	private MappedByteBuffer loadModelFile(Activity activity, String MODEL_FILE) throws IOException {
		AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
		FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
		FileChannel fileChannel = inputStream.getChannel();
		long startOffset = fileDescriptor.getStartOffset();
		long declaredLength = fileDescriptor.getDeclaredLength();
		return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
	}

	private void bindCamera() {
		cameraProviderFuture = ProcessCameraProvider.getInstance(this);

		cameraProviderFuture.addListener(() -> {
			try {
				cameraProvider = cameraProviderFuture.get();

				bindPreview(cameraProvider);
			} catch (ExecutionException | InterruptedException e) {
				Log.e(TAG, "bindCamera: " + e.getMessage());
			}
		}, ContextCompat.getMainExecutor(this));
	}

	@OptIn(markerClass = ExperimentalGetImage.class)
	void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
		Preview preview = new Preview.Builder()
				.build();

		cameraSelector = new CameraSelector.Builder()
				.requireLensFacing(CAMERA_FACING)
				.build();

		preview.setSurfaceProvider(binding.facePreviewView.getSurfaceProvider());
		ImageAnalysis imageAnalysis =
				new ImageAnalysis.Builder()
						.setTargetResolution(new Size(640, 480))
						.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) //Latest frame is shown
						.build();

		Executor executor = Executors.newSingleThreadExecutor();
		imageAnalysis.setAnalyzer(executor, imageProxy -> {
			try {
				Thread.sleep(0);  //Camera preview refreshed every 10 millisec(adjust as required)
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			InputImage image = null;

			@SuppressLint("UnsafeExperimentalUsageError")
			// Camera Feed-->Analyzer-->ImageProxy-->mediaImage-->InputImage(needed for ML kit face detection)

			Image mediaImage = imageProxy.getImage();

			if (mediaImage != null) {
				image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
				Log.i(TAG, "bindPreview - Rotation: " + imageProxy.getImageInfo().getRotationDegrees());

				//Process acquired image to detect faces
				@SuppressLint("SetTextI18n") Task<List<Face>> result =
						faceDetector.process(image)
								.addOnSuccessListener(
										faces -> {
											if (faces.size() != 0) {

												Face face = faces.get(0); //Get first face from detected faces

												//mediaImage to Bitmap
												Bitmap frame_bmp = toBitmap(mediaImage);

												int rot = imageProxy.getImageInfo().getRotationDegrees();

												//Adjust orientation of Face
												Bitmap frame_bmp1 = rotateBitmap(frame_bmp, rot, false, false);

												//Get bounding box of face
												RectF boundingBox = new RectF(face.getBoundingBox());

												//Crop out bounding box from whole Bitmap(image)
												Bitmap cropped_face = getCropBitmapByCPU(frame_bmp1, boundingBox);

												if (flipX)
													cropped_face = rotateBitmap(cropped_face, 0, flipX, false);

												//Scale the acquired Face to 112*112 which is required input for model
												Bitmap scaled = getResizedBitmap(cropped_face, 112, 112);

												if (startFaceDetecting)
													recognizeImage(scaled);


//												Log.i(TAG, "bindPreview: " + boundingBox);

											} else {
												if (registeredFace.isEmpty())
													binding.faceRecognitionStatusTextView.setText("No Face Registered");

												else {
													binding.faceRecognitionStatusTextView.setText("No Face Detected");
												}
												binding.faceRecognitionStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.light_red));
											}
										})
								.addOnFailureListener(
										e -> Log.e(TAG, "bindPreview: " + e.getMessage()))

								.addOnCompleteListener(task -> {
									imageProxy.close(); //v.important to acquire next frame for analysis
								});
			}
		});

		cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
	}

	private void showRegisteredFaceList() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		// System.out.println("Registered"+registered);
		if (registeredFace.isEmpty())
			builder.setTitle("No Faces Added");
		else
			builder.setTitle("Recognitions:");

		// add a checkbox list
		String[] names = new String[registeredFace.size()];
		boolean[] checkedItems = new boolean[registeredFace.size()];
		int i = 0;
		for (Map.Entry<String, SimilarityClassifier.Recognition> entry : registeredFace.entrySet()) {
			//System.out.println("NAME"+entry.getKey());
			names[i] = entry.getKey();
			checkedItems[i] = false;
			i = i + 1;

		}
		builder.setItems(names, null);

		builder.setPositiveButton("OK", (dialog, which) -> {

		});

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	//addFace
	private void getDetectedIDFace() {
		startFaceDetecting = false;
		SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
				"0", "", -1f);
		result.setExtra(embeddings);

		registeredFace.put("registeredIDFace", result);
		startFaceDetecting = true;
		showToast("ID Face Registered", 0);
	}

	private void getDetectedFace() {
		startFaceDetecting = true;
		SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
				"0", "", -1f);
		result.setExtra(embeddings);

		registeredFace.put("registeredFace", result);
		showToast("Face Registered", 0);
	}

	@SuppressLint({"SetTextI18n", "DefaultLocale"})
	public void recognizeImage(final Bitmap bitmap) {

		// set Face to Preview
		binding.facePreviewImageView.setImageBitmap(bitmap);
//			binding.idFaceImageView.setImageBitmap(bitmap);

		//Create ByteBuffer to store normalized image
		ByteBuffer imgData = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4);
		imgData.order(ByteOrder.nativeOrder());
		intValues = new int[inputSize * inputSize];

		//get pixel values from Bitmap to normalize
		bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
		imgData.rewind();

		for (int i = 0; i < inputSize; ++i) {
			for (int j = 0; j < inputSize; ++j) {
				int pixelValue = intValues[i * inputSize + j];
				if (isModelQuantized) {
					// Quantized model
					imgData.put((byte) ((pixelValue >> 16) & 0xFF));
					imgData.put((byte) ((pixelValue >> 8) & 0xFF));
					imgData.put((byte) (pixelValue & 0xFF));
				} else { // Float model
					imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
					imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
					imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
				}
			}
		}

		//imgData is input to our model
		Object[] inputArray = {imgData};
		Map<Integer, Object> outputMap = new HashMap<>();

		embeddings = new float[1][OUTPUT_SIZE]; //output of model will be stored in this variable
		outputMap.put(0, embeddings);

		tfLite.runForMultipleInputsOutputs(inputArray, outputMap); //Run model

		float distance_local;
		String id = "0";
		String label = "?";

		//Compare new face with saved Faces.
		if (registeredFace.size() > 0) {
			final List<Pair<String, Float>> nearest = findNearest(embeddings[0]);//Find 2 closest matching face
			if (nearest.get(0) != null) {
				final String name = nearest.get(0).first; //get name and distance of closest matching face
				distance_local = nearest.get(0).second;
				if (distance_local < distance) {
					if (faceRecognizedThreshold < 6) {

						faceRecognizedThreshold++;

						if (faceRecognizedThreshold == 3) {
							binding.faceRecognitionStatusTextView.setTextColor(Color.BLACK);
							binding.faceRecognitionStatusTextView.setText("Face Matched\nPlease steady your Face");
						}

						binding.faceRecognitionStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.green));
						binding.faceRecognitionStatusTextView.setText("Face Matched");
//						binding.faceRecognitionStatusTextView.setText(name);
//												binding.faceRecognitionStatusTextView.setText("Nearest: " + name + "\nDist: " +
//								String.format("%.3f", distance_local) +
//								"\n2nd Nearest: " + nearest.get(1).first +
//								"\nDist: " + String.format("%.3f", nearest.get(1).second));
					} else {
						isFaceMatched = true;
						startFaceDetecting = false;
						binding.faceRecognitionStatusTextView.setTextColor(Color.BLACK);
						binding.faceRecognitionStatusTextView.setText("Face Matched\nPlease wait...");

						if (idPictureUri != null)
							updateVerificationStatus(idPictureUri);

					}
				} else {
					binding.faceRecognitionStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.light_red));
					binding.faceRecognitionStatusTextView.setText("Face Not Matched");
//						binding.faceRecognitionStatusTextView.setText("Unknown " + "\nDist: " +
//								String.format("%.3f", distance_local) + "\nNearest: " + name +
//								"\nDist: " + String.format("%.3f", distance_local) + "\n2nd Nearest: "
//								+ nearest.get(1).first + "\nDist: " + String.format("%.3f", nearest.get(1).second));

//					if (faceNotRecognizedThreshold < 6) {
//
//						faceNotRecognizedThreshold++;
//
//					} else {
//
//					}
				}

				Log.i(TAG, "recognizeImage - nearest: " + name + " - distance: " + distance_local);
			}
		}

//		final int numDetectionsOutput = 1;
//		final ArrayList<SimilarityClassifier.Recognition> recognitions = new ArrayList<>(numDetectionsOutput);
//		SimilarityClassifier.Recognition rec = new SimilarityClassifier.Recognition(
//				id,
//				label,
//				distance);
//
//		recognitions.add(rec);
	}

	private void clearRegisteredFace() {
		registeredFace.clear();
		insertToSP(registeredFace, 1);
	}

	private void insertToSP(HashMap<String, SimilarityClassifier.Recognition> jsonMap, int mode) {
		if (mode == 1)  //mode: 0:save all, 1:clear all, 2:update all
			jsonMap.clear();
		else if (mode == 0)
			jsonMap.putAll(readFromSP());
		String jsonString = new Gson().toJson(jsonMap);
//        for (Map.Entry<String, SimilarityClassifier.Recognition> entry : jsonMap.entrySet())
//        {
//            System.out.println("Entry Input "+entry.getKey()+" "+  entry.getValue().getExtra());
//        }
		SharedPreferences sharedPreferences = getSharedPreferences("HashMap", MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("map", jsonString);
		//System.out.println("Input josn"+jsonString.toString());
		editor.apply();
		Toast.makeText(this, "Recognitions Saved", Toast.LENGTH_SHORT).show();
	}

	private HashMap<String, SimilarityClassifier.Recognition> readFromSP() {
		SharedPreferences sharedPreferences = getSharedPreferences("HashMap", MODE_PRIVATE);
		String defValue = new Gson().toJson(new HashMap<String, SimilarityClassifier.Recognition>());
		String json = sharedPreferences.getString("map", defValue);
		// System.out.println("Output json"+json.toString());

		TypeToken<HashMap<String, SimilarityClassifier.Recognition>> token = new TypeToken<HashMap<String, SimilarityClassifier.Recognition>>() {
		};
		HashMap<String, SimilarityClassifier.Recognition> retrievedMap = new Gson().fromJson(json, token.getType());
		// System.out.println("Output map"+retrievedMap.toString());

		//During type conversion and save/load procedure,format changes(eg float converted to double).
		//So embeddings need to be extracted from it in required format(eg.double to float).
		for (Map.Entry<String, SimilarityClassifier.Recognition> entry : retrievedMap.entrySet()) {
			float[][] output = new float[1][OUTPUT_SIZE];
			ArrayList arrayList = (ArrayList) entry.getValue().getExtra();
			arrayList = (ArrayList) arrayList.get(0);
			for (int counter = 0; counter < arrayList.size(); counter++) {
				output[0][counter] = ((Double) arrayList.get(counter)).floatValue();
			}
			entry.getValue().setExtra(output);

			Log.i(TAG, "readFromSP - Entry output: " + entry.getKey() + " " + entry.getValue().getExtra());
		}
		Log.i(TAG, "readFromSP: Recognitions Loaded");
		return retrievedMap;
	}

	@SuppressLint("SetTextI18n")

	public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		// CREATE A MATRIX FOR THE MANIPULATION
		Matrix matrix = new Matrix();
		// RESIZE THE BIT MAP
		matrix.postScale(scaleWidth, scaleHeight);

		// "RECREATE" THE NEW BITMAP
		Bitmap resizedBitmap = Bitmap.createBitmap(
				bm, 0, 0, width, height, matrix, false);
		bm.recycle();
		return resizedBitmap;
	}

	private static Bitmap getCropBitmapByCPU(Bitmap source, RectF cropRectF) {
		Bitmap resultBitmap = Bitmap.createBitmap((int) cropRectF.width(),
				(int) cropRectF.height(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(resultBitmap);

		// draw background
		Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
		paint.setColor(Color.WHITE);
		canvas.drawRect(
				new RectF(0, 0, cropRectF.width(), cropRectF.height()),
				paint);

		Matrix matrix = new Matrix();
		matrix.postTranslate(-cropRectF.left, -cropRectF.top);

		canvas.drawBitmap(source, matrix, paint);

		if (!source.isRecycled()) {
			source.recycle();
		}

		return resultBitmap;
	}

	private static Bitmap rotateBitmap(
			Bitmap bitmap, int rotationDegrees, boolean flipX, boolean flipY) {
		Matrix matrix = new Matrix();

		// Rotate the image back to straight.
		matrix.postRotate(rotationDegrees);

		// Mirror the image along the X or Y axis.
		matrix.postScale(flipX ? -1.0f : 1.0f, flipY ? -1.0f : 1.0f);
		Bitmap rotatedBitmap =
				Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

		// Recycle the old bitmap if it has changed.
		if (rotatedBitmap != bitmap) {
			bitmap.recycle();
		}
		return rotatedBitmap;
	}

	private List<Pair<String, Float>> findNearest(float[] emb) {
		List<Pair<String, Float>> neighbour_list = new ArrayList<>();
		Pair<String, Float> ret = null; //to get closest match
		Pair<String, Float> prev_ret = null; //to get second closest match
		for (Map.Entry<String, SimilarityClassifier.Recognition> entry : registeredFace.entrySet()) {

			final String name = entry.getKey();
			final float[] knownEmb = ((float[][]) entry.getValue().getExtra())[0];

			float distance = 0;
			for (int i = 0; i < emb.length; i++) {
				float diff = emb[i] - knownEmb[i];
				distance += diff * diff;
			}
			distance = (float) Math.sqrt(distance);
			if (ret == null || distance < ret.second) {
				prev_ret = ret;
				ret = new Pair<>(name, distance);
			}
		}
		if (prev_ret == null) prev_ret = ret;
		neighbour_list.add(ret);
		neighbour_list.add(prev_ret);

		return neighbour_list;
	}

	//IMPORTANT. If conversion not done ,the toBitmap conversion does not work on some devices.
	private static byte[] YUV_420_888toNV21(Image image) {

		int width = image.getWidth();
		int height = image.getHeight();
		int ySize = width * height;
		int uvSize = width * height / 4;

		byte[] nv21 = new byte[ySize + uvSize * 2];

		ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
		ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
		ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

		int rowStride = image.getPlanes()[0].getRowStride();
		assert (image.getPlanes()[0].getPixelStride() == 1);

		int pos = 0;

		if (rowStride == width) { // likely
			yBuffer.get(nv21, 0, ySize);
			pos += ySize;
		} else {
			long yBufferPos = -rowStride; // not an actual position
			for (; pos < ySize; pos += width) {
				yBufferPos += rowStride;
				yBuffer.position((int) yBufferPos);
				yBuffer.get(nv21, pos, width);
			}
		}

		rowStride = image.getPlanes()[2].getRowStride();
		int pixelStride = image.getPlanes()[2].getPixelStride();

		assert (rowStride == image.getPlanes()[1].getRowStride());
		assert (pixelStride == image.getPlanes()[1].getPixelStride());

		if (pixelStride == 2 && rowStride == width && uBuffer.get(0) == vBuffer.get(1)) {
			// maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
			byte savePixel = vBuffer.get(1);
			try {
				vBuffer.put(1, (byte) ~savePixel);
				if (uBuffer.get(0) == (byte) ~savePixel) {
					vBuffer.put(1, savePixel);
					vBuffer.position(0);
					uBuffer.position(0);
					vBuffer.get(nv21, ySize, 1);
					uBuffer.get(nv21, ySize + 1, uBuffer.remaining());

					return nv21; // shortcut
				}
			} catch (ReadOnlyBufferException ex) {
				// unfortunately, we cannot check if vBuffer and uBuffer overlap
			}

			// unfortunately, the check failed. We must save U and V pixel by pixel
			vBuffer.put(1, savePixel);
		}

		// other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
		// but performance gain would be less significant
		for (int row = 0; row < height / 2; row++) {
			for (int col = 0; col < width / 2; col++) {
				int vuPos = col * pixelStride + row * rowStride;
				nv21[pos++] = vBuffer.get(vuPos);
				nv21[pos++] = uBuffer.get(vuPos);
			}
		}

		return nv21;
	}

	private Bitmap toBitmap(Image image) {

		byte[] nv21 = YUV_420_888toNV21(image);


		YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

		byte[] imageBytes = out.toByteArray();
		//System.out.println("bytes"+ Arrays.toString(imageBytes));

		//System.out.println("FORMAT"+image.getFormat());

		return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
	}

	private Bitmap getBitmapFromUri(Uri uri) throws IOException {
		ParcelFileDescriptor parcelFileDescriptor =
				getContentResolver().openFileDescriptor(uri, "r");
		FileDescriptor fileDescriptor = null;
		if (parcelFileDescriptor != null) {
			fileDescriptor = parcelFileDescriptor.getFileDescriptor();
		}
		Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
		if (parcelFileDescriptor != null) {
			parcelFileDescriptor.close();
		}
		return image;
	}

	private void updateVerificationStatus(Uri idPictureUri) {
		if (FirebaseMain.getUser() != null) {
			showPleaseWaitDialog();

			String userID = FirebaseMain.getUser().getUid();
			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(userID);

			Map<String, Object> updateUser = new HashMap<>();
			updateUser.put("isVerified", true);

			documentReference.update(updateUser)
					.addOnSuccessListener(unused -> uploadIDPictureToFirebaseStorage(userID, idPictureUri))
					.addOnFailureListener(e -> {

						closePleaseWaitDialog();

						Log.e(TAG, "updateVerificationStatus: onFailure " + e.getMessage());

					});
		} else {
			closePleaseWaitDialog();

			Log.e(TAG, "updateVerificationStatus: current user is null");

			intent = new Intent(ScanIDActivity.this, LoginOrRegisterActivity.class);
			startActivity(intent);
			finish();
		}
	}

	private void updateUserToNotVerified() {
		if (FirebaseMain.getUser() != null) {
			String userID = FirebaseMain.getUser().getUid();

			documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection).document(userID);

			Map<String, Object> updateUser = new HashMap<>();
			updateUser.put("isVerified", false);
			updateUser.put("idPicture", idPictureURL);

			documentReference.update(updateUser)
					.addOnSuccessListener(unused -> {

						goToMainActivity();

					})
					.addOnFailureListener(e -> {

						closePleaseWaitDialog();

						Log.e(TAG, "updateVerificationStatus: onFailure " + e.getMessage());

					});
		} else {
			Log.e(TAG, "updateVerificationStatus: current user is null");

			intent = new Intent(ScanIDActivity.this, LoginOrRegisterActivity.class);
			startActivity(intent);
			finish();
		}

	}

	private void checkIfUserIsVerified() {
		if (FirebaseMain.getUser() != null) {
			String userID = FirebaseMain.getUser().getUid();

			DocumentReference documentReference = FirebaseMain.getFireStoreInstance()
					.collection(FirebaseMain.userCollection)
					.document(userID);

			documentReference.get()
					.addOnSuccessListener(documentSnapshot -> {
						if (documentSnapshot.exists()) {
							boolean isVerified = documentSnapshot.getBoolean("isVerified");
							getUserType = documentSnapshot.getString("userType");

							if (isVerified) {
								isUserVerified = true;

								binding.idAlreadyScannedLayout.setVisibility(View.VISIBLE);
								binding.backFloatingBtn.setVisibility(View.VISIBLE);
								binding.idScanLayout.setVisibility(View.GONE);
								binding.scanYourIDTypeTextView.setVisibility(View.GONE);
								binding.scanLaterBtn.setVisibility(View.GONE);

								String message = "You have already scanned your ID." +
										"Would you like to scan again?" +
										"Tap/Click the white are to scan your ID";

								if (voiceAssistantState.equals("enabled")) {
									voiceAssistant = VoiceAssistant.getInstance(this);
									voiceAssistant.speak(message);
								}

							} else {
								if (voiceAssistantState.equals("enabled")) {
									voiceAssistant = VoiceAssistant.getInstance(this);
									voiceAssistant.speak("Scan ID");
								}
							}
						}
					})
					.addOnFailureListener(e -> Log.e(TAG, "checkIfUserIsVerified: onFailure " + e.getMessage()));
		}
	}

	private void checkCameraAndStoragePermission() {

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.CAMERA},
					CAMERA_PERMISSION_REQUEST_CODE);
		} else {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					CAMERA_PERMISSION_REQUEST_CODE);
		}

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
							Manifest.permission.WRITE_EXTERNAL_STORAGE},
					STORAGE_PERMISSION_REQUEST_CODE);
		} else {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					STORAGE_PERMISSION_REQUEST_CODE);
		}
	}

	private void goToMainActivity() {
		intent = new Intent(ScanIDActivity.this, MainActivity.class);
		startActivity(intent);
		finish();
	}

	private void resetImageViewAndShowDialog() {
		binding.idImageView.setImageResource(R.drawable.face_id_100);
		binding.doneBtn.setVisibility(View.GONE);
		binding.idVerifiedLayout.setVisibility(View.GONE);
		binding.faceRecognitionLayout.setVisibility(View.GONE);

		if (isUserVerified) {
			binding.idAlreadyScannedLayout.setVisibility(View.VISIBLE);
		}

		idPictureUri = null;
		showNotAnIDDialog();
	}

	private void showIDVerifiedLayout() {
		fadeOutAnimation(binding.scanLaterBtn);
		fadeInAnimation(binding.tryAgainBtn);
		fadeInAnimation(binding.idVerifiedLayout);
		fadeInAnimation(binding.matchYourFaceTextView);
		fadeInAnimation(binding.faceRecognitionLayout);

		Handler handler = new Handler();
		Runnable dismissRunnable = () -> {
			int scrollBottom = binding.scrollView.getChildAt(0).getHeight() - binding.scrollView.getHeight();
			ObjectAnimator.ofInt(binding.scrollView, "scrollY", scrollBottom)
					.setDuration(1500)
					.start();
		};
		handler.postDelayed(dismissRunnable, 2000);

	}

	private void fadeInAnimation(final View view) {
		AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
		fadeIn.setDuration(2000);

		view.startAnimation(fadeIn);
		view.setVisibility(View.VISIBLE);
	}

	private void fadeOutAnimation(final View view) {
		AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
		fadeOut.setDuration(2000);

		view.startAnimation(fadeOut);
		view.setVisibility(View.GONE);
	}

	private void animateLayoutSize(final View view, final int targetWidth, final int targetHeight) {
		ValueAnimator animator = ValueAnimator.ofInt(view.getMeasuredWidth(), targetWidth);

		animator.addUpdateListener(valueAnimator -> {
			int animatedValue = (int) valueAnimator.getAnimatedValue();

			ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
			layoutParams.width = animatedValue;
			layoutParams.height = targetHeight; // Set the desired height

			view.setLayoutParams(layoutParams);
		});

		animator.setDuration(1000); // You can customize the duration of the animation
		animator.start();
	}

	private void reverseAnimateLayoutSize(final View view) {
		// Measure the original dimensions
		view.measure(
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
		);

		int originalWidth = view.getMeasuredWidth();
		int originalHeight = view.getMeasuredHeight();

		ValueAnimator animator = ValueAnimator.ofInt(view.getMeasuredWidth(), originalWidth);

		animator.addUpdateListener(valueAnimator -> {
			int animatedValue = (int) valueAnimator.getAnimatedValue();

			ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
			layoutParams.width = animatedValue;
			layoutParams.height = originalHeight; // Set the original height

			view.setLayoutParams(layoutParams);
		});

		animator.setDuration(1000); // You can customize the duration of the animation
		animator.start();
	}

	private void backToMyProfile() {
		intent = new Intent(ScanIDActivity.this, MainActivity.class);
		intent.putExtra("activityData", "fromMyProfile");
		startActivity(intent);
		finish();
	}

	private void showVerificationSuccessDialog() {
		builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);

		View dialogView = getLayoutInflater()
				.inflate(R.layout.dialog_verification_success, null);


		Button okayBtn = dialogView.findViewById(R.id.okayBtn);

		okayBtn.setOnClickListener(v -> {
			intent = new Intent(ScanIDActivity.this, MainActivity.class);
			startActivity(intent);
			finish();

			closeVerificationSuccessDialog();
		});

		builder.setView(dialogView);
		verificationSuccessDialog = builder.create();
		verificationSuccessDialog.show();
	}

	private void closeVerificationSuccessDialog() {
		if (verificationSuccessDialog != null && verificationSuccessDialog.isShowing()) {
			verificationSuccessDialog.dismiss();
		}
	}

	private void showCancelScanIDDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_cancel_scan_id, null);

		Button yesBtn = dialogView.findViewById(R.id.yesBtn);
		Button noBtn = dialogView.findViewById(R.id.noBtn);

		yesBtn.setOnClickListener(v -> {
			updateUserToNotVerified();
		});

		noBtn.setOnClickListener(v -> closeCancelScanIDDialog());

		builder.setView(dialogView);
		cancelScanIDDialog = builder.create();
		if (!isFinishing() && !isDestroyed()) {
			cancelScanIDDialog.show();
		}
	}

	private void closeCancelScanIDDialog() {
		if (cancelScanIDDialog != null && cancelScanIDDialog.isShowing()) {
			cancelScanIDDialog.isShowing();
		}
	}

	private void showNotAnIDDialog() {
		builder = new AlertDialog.Builder(this);

		DialogNotAnIdBinding dialogNotAnIdBinding =
				DialogNotAnIdBinding.inflate(getLayoutInflater());
		View dialogView = dialogNotAnIdBinding.getRoot();

		if (fontSize.equals("large")) {
			float textSize = 22;
			dialogNotAnIdBinding.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
			dialogNotAnIdBinding.bodyTextView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
			dialogNotAnIdBinding.bodyTextView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
			dialogNotAnIdBinding.okayBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
		}

		dialogNotAnIdBinding.okayBtn.setOnClickListener(v -> closeNotAnIDDialog());

		builder.setView(dialogView);

		notAnIDDialog = builder.create();
		notAnIDDialog.show();
	}

	private void closeNotAnIDDialog() {
		if (notAnIDDialog != null && notAnIDDialog.isShowing()) {
			notAnIDDialog.dismiss();
		}
	}

	private void showUploadClearIDPictureDialog() {
		builder = new AlertDialog.Builder(this);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_upload_clear_id_picture, null);

		Button okayBtn = dialogView.findViewById(R.id.okayBtn);

		okayBtn.setOnClickListener(v -> {
			closeUploadClearIDPictureDialog();
		});

		builder.setView(dialogView);

		uploadClearIDPictureDialog = builder.create();
		uploadClearIDPictureDialog.show();
	}

	private void closeUploadClearIDPictureDialog() {
		if (uploadClearIDPictureDialog != null && uploadClearIDPictureDialog.isShowing()) {
			uploadClearIDPictureDialog.dismiss();
		}
	}

	private void showToast(String message, int duration) {
		Toast.makeText(this, message, duration).show();
	}

	private void getUserSettings() {
		setFontSize(fontSize);
	}

	@Override
	public void onFontSizeChanged(boolean isChecked) {
		fontSize = isChecked ? "large" : "normal";
		setFontSize(fontSize);
	}

	private void setFontSize(String fontSize) {

		float textSizeSP;
		float textHeaderSizeSP;
		if (fontSize.equals("large")) {
			textSizeSP = INCREASED_TEXT_SIZE_SP;
			textHeaderSizeSP = INCREASED_TEXT_HEADER_SIZE_SP;
		} else {
			textSizeSP = DEFAULT_TEXT_SIZE_SP;
			textHeaderSizeSP = DEFAULT_HEADER_TEXT_SIZE_SP;
		}

		binding.scanIDTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);
		binding.scanYourIDTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textHeaderSizeSP);

		binding.idAlreadyScannedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.scanIDAgainTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.tapTextView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.tapTextView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.idVerifiedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.alignYourFaceTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.faceRecognitionStatusTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.doneBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.tryAgainBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
		binding.scanLaterBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSP);
	}

	private void showPleaseWaitDialog() {
		builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);

		View dialogView = getLayoutInflater()
				.inflate(R.layout.dialog_please_wait, null);

		builder.setView(dialogView);

		pleaseWaitDialog = builder.create();
		pleaseWaitDialog.show();
	}

	private void closePleaseWaitDialog() {
		if (pleaseWaitDialog != null && pleaseWaitDialog.isShowing()) {
			pleaseWaitDialog.dismiss();
		}
	}

	private void uploadIDPictureToFirebaseStorage(String userID, Uri idPictureUri) {

		StorageReference idPictureReference = FirebaseMain.getFirebaseStorageInstance().getReference();
		StorageReference idPicturePath = idPictureReference.child("images/idPictures/"
				+ System.currentTimeMillis() + "_" + userID + ".jpg");

		idPicturePath.putFile(idPictureUri)
				.addOnSuccessListener(taskSnapshot -> {

					idPicturePath.getDownloadUrl()
							.addOnSuccessListener(uri -> {

								showVerificationSuccessDialog();

								idPictureURL = uri.toString();
								storeIDPictureURLInFireStore(userID, idPictureURL);

							})
							.addOnFailureListener(e -> {
								closePleaseWaitDialog();

								Toast.makeText(ScanIDActivity.this, "ID failed to add", Toast.LENGTH_SHORT).show();
								Log.e(TAG, "uploadIDPictureToFirebaseStorage: " + e.getMessage());
							});
				})
				.addOnFailureListener(e -> {
					closePleaseWaitDialog();

					Toast.makeText(ScanIDActivity.this, "ID failed to add", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "uploadIDPictureToFirebaseStorage: " + e.getMessage());
				});

	}

	private void storeIDPictureURLInFireStore(String userID, String idPictureURL) {

		documentReference = FirebaseMain.getFireStoreInstance()
				.collection(FirebaseMain.userCollection)
				.document(userID);

		Map<String, Object> idPicture = new HashMap<>();
		idPicture.put("idPicture", idPictureURL);

		documentReference.update(idPicture)
				.addOnSuccessListener(unused ->

						Log.d(TAG, "storeIDPictureURLInFireStore: addOnSuccessListener"))

				.addOnFailureListener(e -> {
					closePleaseWaitDialog();

					Toast.makeText(ScanIDActivity.this, "ID failed to add", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "storeIDPictureURLInFireStore: " + e.getMessage());
				});
	}

	private void showNoInternetDialog() {
		builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);

		View dialogView = getLayoutInflater().inflate(R.layout.dialog_no_internet, null);

		Button tryAgainBtn = dialogView.findViewById(R.id.tryAgainBtn);

		tryAgainBtn.setOnClickListener(v -> closeNoInternetDialog());

		builder.setView(dialogView);

		noInternetDialog = builder.create();
		noInternetDialog.show();
	}

	private void closeNoInternetDialog() {
		if (noInternetDialog != null && noInternetDialog.isShowing()) {
			noInternetDialog.dismiss();

			boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(this);
			updateConnectionStatus(isConnected);

		}
	}

	private void initializeNetworkChecker() {
		networkChangeReceiver = new NetworkChangeReceiver(new NetworkChangeReceiver.NetworkChangeListener() {
			@Override
			public void onNetworkChanged(boolean isConnected) {
				updateConnectionStatus(isConnected);
			}
		});

		IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(networkChangeReceiver, intentFilter);

		// Initial network status check
		boolean isConnected = NetworkConnectivityChecker.isNetworkConnected(this);
		updateConnectionStatus(isConnected);

	}

	private void updateConnectionStatus(boolean isConnected) {
		if (isConnected) {
			if (noInternetDialog != null && noInternetDialog.isShowing()) {
				noInternetDialog.dismiss();
			}
		} else {
			showNoInternetDialog();
		}
	}

	@SuppressLint("SetTextI18n")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		try {
			if (resultCode == Activity.RESULT_OK && data != null) {
				if (requestCode == UPLOAD_ID_FROM_GALLERY) {

					fadeInAnimation(binding.scanYourIDTypeTextView);
					fadeOutAnimation(binding.idAlreadyScannedLayout);
					idPictureUri = data.getData();
					Uri selectedImageUri = data.getData();

					binding.idImageView.setImageURI(idPictureUri);

					Bitmap bitmap;
					try {
						bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), idPictureUri);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
					bitmap = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);
					bitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false);

					//scan ID
					classifyID(bitmap, selectedImageUri);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "onActivityResult: " + e.getMessage());
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       @NonNull String[] permissions,
	                                       @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
				Log.i(TAG, "onRequestPermissionsResult: Camera Permission Granted");

			else
				Log.e(TAG, "onRequestPermissionsResult: Camera Permission Denied");

		} else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
				Log.i(TAG, "onRequestPermissionsResult: Storage Permission Granted");

			else
				Log.e(TAG, "onRequestPermissionsResult: Storage Permission Denied");

		} else
			showToast("All Permissions Denied", 1);

	}
}