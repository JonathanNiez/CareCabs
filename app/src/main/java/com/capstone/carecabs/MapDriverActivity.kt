package com.capstone.carecabs

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.capstone.carecabs.Firebase.FirebaseMain
import com.capstone.carecabs.Utility.StaticDataPasser
import com.capstone.carecabs.databinding.ActivityMapDriverBinding
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location

class MapDriverActivity : AppCompatActivity() {

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
        binding.mapView.gestures.focalPoint = binding.mapView.getMapboxMap().pixelForCoordinate(it)
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }


    private lateinit var mapboxMap: MapboxMap
    private lateinit var annotationConfig: AnnotationConfig
    private var annotationPlugin: AnnotationPlugin? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var layerID = "map_annotation"
    private val TAG: String = "MapActivity"
    private val LOCATION_PERMISSION_REQUEST_CODE = 123

    private lateinit var documentReference: DocumentReference
    private lateinit var collectionReference: CollectionReference
    private lateinit var intent: Intent
    private lateinit var builder: AlertDialog.Builder
    private lateinit var userNotVerifiedDialog: AlertDialog
    private lateinit var passengerLocationInfoDialog: AlertDialog

    private lateinit var binding: ActivityMapDriverBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tripProgressLayout.visibility = View.INVISIBLE
        binding.soundBtn.visibility = View.INVISIBLE
        binding.maneuverView.visibility = View.INVISIBLE

        checkIfUserIsVerified()
        checkLocationPermission()
        initializeBottomNavButtons()
    }

    override fun onBackPressed() {

        intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()

    }

    private fun onMapReady() {

        mapboxMap = binding.mapView.getMapboxMap().apply {
            loadStyleUri(getString(R.string.custom_map_style_url)) {

                setupGesturesListener()
                loadCoordinatesToMapFromFireStore()
                initializeLocationComponent()

//                annotationPlugin = binding.mapView.annotations
//                annotationConfig = AnnotationConfig(
//                    layerId = layerID
//                )


                binding.mapView.camera.apply {
                    val bearing = createBearingAnimator(
                        CameraAnimatorOptions.cameraAnimatorOptions(
                            -45.0
                        )
                    ) {
                        duration = 4000
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    val zoom = createZoomAnimator(
                        CameraAnimatorOptions.cameraAnimatorOptions(14.0) {
                            startValue(3.0)
                        }
                    ) {
                        duration = 4000
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    val pitch = createPitchAnimator(
                        CameraAnimatorOptions.cameraAnimatorOptions(55.0) {
                            startValue(0.0)
                        }
                    ) {
                        duration = 4000
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    playAnimatorsSequentially(zoom)
                }

            }
        }
        binding.mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(14.0)
                .build()
        )
    }

    private fun initializeLocationComponent() {

        val locationComponentPlugin = binding.mapView.location

        locationComponentPlugin.updateSettings {

            this.enabled = true
            this.locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    this@MapDriverActivity,
                    R.drawable.mapbox_navigation_puck_icon,
                ),
                scaleExpression = Expression.interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )
        }
        locationComponentPlugin.addOnIndicatorPositionChangedListener(
            onIndicatorPositionChangedListener
        )
        locationComponentPlugin.addOnIndicatorBearingChangedListener(
            onIndicatorBearingChangedListener
        )
    }


    private fun initializeBottomNavButtons() {
        binding.bottomNavigationView.selectedItemId = R.id.setLocation

        binding.bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.setLocation -> {
                }

                R.id.myBookings -> {
                    val intent = Intent(this, BookingsActivity::class.java)
                    startActivity(intent)
                }

                R.id.help -> {

                }
            }
            true
        }
    }

    private fun setupGesturesListener() {
        binding.mapView.gestures.addOnMoveListener(onMoveListener)


//        pointAnnotationManager?.apply {
//            addClickListener(
//                OnPointAnnotationClickListener {
//                    Toast.makeText(this@MapPassengerActivity, "id: ${it.id}", Toast.LENGTH_LONG).show()
//                    false
//                }
//            )
//        }
    }

    private fun onMarkerItemClick(marker: PointAnnotation): Boolean {
        Toast.makeText(this@MapDriverActivity, marker.toString(), Toast.LENGTH_SHORT).show()

        return true
    }

    private fun addSeniorAnnotationToMap(
        longitude: Double, latitude: Double, locationID: String
    ) {

        bitmapFromDrawableRes(
            this@MapDriverActivity,
            R.drawable.senior_location_pin_128
        ).let {
            val annotationApi = binding.mapView.annotations
            val pointAnnotationManager = annotationApi.createPointAnnotationManager(binding.mapView)
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(longitude, latitude))
                .withIconImage(it)
            pointAnnotationManager.create(pointAnnotationOptions)

            pointAnnotationManager.apply {
                addClickListener(
                    OnPointAnnotationClickListener {

                        showPassengerLocationInfoDialog(locationID)

                        false
                    }
                )
            }

        }
    }

    private fun addPWDAnnotationToMap(
        longitude: Double, latitude: Double, locationID: String
    ) {

        bitmapFromDrawableRes(
            this@MapDriverActivity,
            R.drawable.pwd_location_pin_128
        ).let {
            val annotationApi = binding.mapView.annotations
            val pointAnnotationManager = annotationApi.createPointAnnotationManager(binding.mapView)
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(longitude, latitude))
                .withIconImage(it)
            pointAnnotationManager.create(pointAnnotationOptions)

            pointAnnotationManager.apply {
                addClickListener(
                    OnPointAnnotationClickListener {

                        showPassengerLocationInfoDialog(locationID)

                        false
                    }
                )
            }

        }
    }

    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap {

        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
            val constantState = sourceDrawable?.constantState
            val drawable = constantState?.newDrawable()?.mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable!!.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    private fun onCameraTrackingDismissed() {
        Toast.makeText(this, "onCameraTrackingDismissed", Toast.LENGTH_SHORT).show()

        binding.mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.gestures.removeOnMoveListener(onMoveListener)
    }


    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onMapReady()
        } else {
            // Request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun loadCoordinatesToMapFromFireStore() {

        pointAnnotationManager?.addClickListener(OnPointAnnotationClickListener {
            onMarkerItemClick(it)
        })

        collectionReference = FirebaseMain.getFireStoreInstance()
            .collection(StaticDataPasser.locationCollection)

        collectionReference.get().addOnSuccessListener {

            if (it != null) {
                for (document in it.documents) {
                    val getDestinationLatitude =
                        document.getDouble("destinationLatitude")!!.toDouble()
                    val getDestinationLongitude =
                        document.getDouble("destinationLongitude")!!.toDouble()
                    val getLocationID = document.getString("locationID")!!
                    val getUserType = document.getString("passengerUserType")!!

                    when (getUserType) {
                        "Senior Citizen" -> {
                            addSeniorAnnotationToMap(
                                getDestinationLongitude,
                                getDestinationLatitude,
                                getLocationID
                            )
                        }

                        "Persons with Disability (PWD)" -> {
                            addPWDAnnotationToMap(
                                getDestinationLongitude,
                                getDestinationLatitude,
                                getLocationID
                            )
                        }
                    }

                }
            }

        }.addOnFailureListener {
            Log.e(TAG, it.message.toString())
        }
    }

    private fun showPassengerLocationInfoDialog(locationID: String) {

        builder = AlertDialog.Builder(this)

        val dialogView = layoutInflater.inflate(R.layout.dialog_passenger_location_info, null)

        val pickupBtn = dialogView.findViewById<Button>(R.id.pickupBtn)
        val closeBtn = dialogView.findViewById<Button>(R.id.closeBtn)
        val firstnameTextView = dialogView.findViewById<TextView>(R.id.firstnameTextView)
        val lastnameTextView = dialogView.findViewById<TextView>(R.id.lastnameTextView)
        val userTypeTextView = dialogView.findViewById<TextView>(R.id.userTypeTextView)
        val medicalConditionTextView =
            dialogView.findViewById<TextView>(R.id.medicalConditionTextView)
        val disabilityTextView = dialogView.findViewById<TextView>(R.id.disabilityTextView)
        val passengerProfilePic = dialogView.findViewById<ImageView>(R.id.passengerProfilePic)
        val progressBarLayout = dialogView.findViewById<LinearLayout>(R.id.progressBarLayout)

        disabilityTextView.visibility = View.GONE
        medicalConditionTextView.visibility = View.GONE

        documentReference = FirebaseMain.getFireStoreInstance()
            .collection(StaticDataPasser.locationCollection)
            .document(locationID)

        documentReference.get().addOnSuccessListener {
            if (it != null && it.exists()) {
                progressBarLayout.visibility = View.GONE

                val getFirstname = it.getString("passengerFirstname")
                val getLastname = it.getString("passengerLastname")
                val getUserType = it.getString("passengerUserType")
                val getProfilePicture = it.getString("passengerProfilePicture")

                firstnameTextView.text = getFirstname
                lastnameTextView.text = getLastname
                userTypeTextView.text = getUserType

                if (!getProfilePicture.equals("default")) {
                    Glide.with(this@MapDriverActivity)
                        .load(getProfilePicture)
                        .placeholder(R.drawable.loading_gif)
                        .into(passengerProfilePic)
                }

                when (getUserType) {
                    "Senior Citizen" -> {
                        val getMedicalCondition = it.getString("passengerMedicalCondition")

                        medicalConditionTextView.text =
                            getMedicalCondition
                    }

                    "Persons with Disability (PWD)" -> {
                        val getDisability = it.getString("passengerDisability")

                        disabilityTextView.text = getDisability
                    }
                }

            }
        }.addOnFailureListener {
            Log.e(TAG, it.message.toString())

            progressBarLayout.visibility = View.GONE
        }

        pickupBtn.setOnClickListener {

        }

        closeBtn.setOnClickListener {
            closePassengerLocationInfoDialog()
        }

        builder.setView(dialogView)
        passengerLocationInfoDialog = builder.create()
        passengerLocationInfoDialog.show()
    }

    private fun closePassengerLocationInfoDialog() {
        if (passengerLocationInfoDialog != null && passengerLocationInfoDialog.isShowing) {
            passengerLocationInfoDialog.dismiss()
        }
    }

    private fun checkIfUserIsVerified() {
        if (FirebaseMain.getUser() != null) {
            documentReference = FirebaseMain.getFireStoreInstance()
                .collection(StaticDataPasser.userCollection)
                .document(FirebaseMain.getUser().uid)

            documentReference.get().addOnSuccessListener {
                if (it != null && it.exists()) {
                    val getVerificationStatus = it.getBoolean("isVerified")

                    if (!getVerificationStatus!!) {
                        showUserNotVerifiedDialog()

                    }
                }
            }.addOnFailureListener {
                Log.e(TAG, it.message.toString())
            }

        } else {
            FirebaseMain.signOutUser()

            intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun showUserNotVerifiedDialog() {
        builder = AlertDialog.Builder(this)
        builder.setCancelable(false)

        val dialogView = layoutInflater.inflate(R.layout.dialog_user_not_verified, null)
        val okBtn = dialogView.findViewById<Button>(R.id.okBtn)

        okBtn.setOnClickListener { v: View? ->
            intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()

            closeUserNotVerifiedDialog()
        }

        builder.setView(dialogView)
        userNotVerifiedDialog = builder.create()
        userNotVerifiedDialog.show()
    }

    private fun closeUserNotVerifiedDialog() {
        if (userNotVerifiedDialog != null && userNotVerifiedDialog.isShowing) {
            userNotVerifiedDialog.dismiss()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed to location retrieval
            } else {
                // Permission denied, handle accordingly (show error message, etc.)
            }
        }
    }


}



