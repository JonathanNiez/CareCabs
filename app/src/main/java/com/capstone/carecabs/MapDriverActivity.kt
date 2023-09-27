package com.capstone.carecabs

import android.Manifest
import android.annotation.SuppressLint
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
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.capstone.carecabs.Firebase.FirebaseMain
import com.capstone.carecabs.Model.PassengerBookingModel
import com.capstone.carecabs.Model.TripModel
import com.capstone.carecabs.Utility.StaticDataPasser
import com.capstone.carecabs.databinding.ActivityMapDriverBinding
import com.capstone.carecabs.databinding.DialogBookingInfoBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import java.util.Calendar
import java.util.UUID

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
                loadPassengerCoordinatesToMapFromDatabase()
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
        binding.bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.trips -> {
                    intent = Intent(this, TripsOverviewActivity::class.java)
                }

                R.id.bookings -> {
                    intent = Intent(this, PassengerBookingsOverview::class.java)
                }

                R.id.help -> {
                    intent = Intent(this, HelpActivity::class.java)
                }
            }
            startActivity(intent)

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

    private fun addSeniorAnnotationToMap(
        longitude: Double, latitude: Double, bookingID: String
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

                        showPassengerLocationInfoDialog(bookingID)

                        false
                    }
                )
            }

        }
    }

    private fun addPWDAnnotationToMap(
        longitude: Double, latitude: Double, bookingID: String
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

                        showPassengerLocationInfoDialog(bookingID)

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

    private fun loadPassengerCoordinatesToMapFromDatabase() {

        val locationReference = FirebaseDatabase.getInstance()
            .getReference(StaticDataPasser.bookingCollection)

        locationReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot != null && snapshot.exists()) {

                    for (locationSnapshot in snapshot.children) {
                        val locationData =
                            locationSnapshot.getValue(PassengerBookingModel::class.java)

                        if (locationData != null) {
                            when (locationData.passengerUserType) {
                                "Senior Citizen" -> {
                                    addSeniorAnnotationToMap(
                                        locationData.destinationLongitude,
                                        locationData.destinationLatitude,
                                        locationData.bookingID
                                    )
                                }

                                "Persons with Disability (PWD)" -> {
                                    addPWDAnnotationToMap(
                                        locationData.destinationLongitude,
                                        locationData.destinationLatitude,
                                        locationData.bookingID
                                    )
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, error.message)
            }

        })

    }

    private fun getCurrentTimeAndDate(): String {
        val calendar = Calendar.getInstance() // Get a Calendar instance
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Months are 0-based, so add 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        return "$month-$day-$year $hour:$minute:$second"
    }

    private fun generateRandomTripID(): String {
        val uuid = UUID.randomUUID()
        return uuid.toString()
    }

    private fun storeTripToDatabase(
        generateTripID: String,
        bookingID: String,
        passengerID: String,
        currentLatitude: Double,
        currentLongitude: Double,
        destinationLatitude: Double,
        destinationLongitude: Double
    ) {

        documentReference = FirebaseMain.getFireStoreInstance()
            .collection(StaticDataPasser.tripCollection).document(generateTripID)

        val tripModel = TripModel(
            tripID = generateTripID,
            bookingID = bookingID,
            tripStatus = "Ongoing" ,
            userDriverID = FirebaseMain.getUser().uid,
            userPassengerID = passengerID,
            tripDate = getCurrentTimeAndDate(),
            currentLatitude = currentLatitude,
            currentLongitude = currentLongitude,
            destinationLatitude = destinationLatitude,
            destinationLongitude = destinationLongitude
        )

        documentReference.set(tripModel)
            .addOnSuccessListener {
            }.addOnFailureListener {
                Log.e(TAG, it.message.toString())
            }

        //update booking from passenger
        val bookingReference = FirebaseDatabase.getInstance()
            .getReference(StaticDataPasser.bookingCollection)


        val updateBookingStatus = HashMap<String, Any>()
        updateBookingStatus["bookingStatus"] = "Standby"

        bookingReference.child(bookingID)
            .updateChildren(updateBookingStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Booking Accepted", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Log.e(TAG, it.message.toString())
            }

        updateDriverAvailabilityStatus(false)
    }

    private fun updateDriverAvailabilityStatus(isAvailable: Boolean) {
        if (FirebaseMain.getUser() != null) {
            FirebaseMain.getFireStoreInstance().collection(StaticDataPasser.userCollection)
                .document(FirebaseMain.getUser().uid)
                .update("isAvailable", isAvailable)
        }
    }

    @SuppressLint("SetTextI18n", "RestrictedApi")
    private fun showPassengerLocationInfoDialog(bookingID: String) {

        val binding = DialogBookingInfoBinding.inflate(layoutInflater)
        builder = AlertDialog.Builder(this)
        val dialogView = binding.root

        binding.disabilityTextView.visibility = View.GONE
        binding.medicalConditionTextView.visibility = View.GONE

        val locationReference = FirebaseDatabase.getInstance()
            .getReference(StaticDataPasser.bookingCollection)

        locationReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot != null && snapshot.exists()) {

                    for (locationSnapshot in snapshot.children) {
                        val passengerBookingData =
                            locationSnapshot.getValue(PassengerBookingModel::class.java)

                        if (passengerBookingData != null) {
                            if (passengerBookingData.bookingID == bookingID) {
                                binding.progressBarLayout.visibility = View.GONE

                                StaticDataPasser.storePassengerID =
                                    passengerBookingData.passengerUserID
                                StaticDataPasser.storeCurrentLatitude =
                                    passengerBookingData.currentLatitude
                                StaticDataPasser.storeCurrentLongitude =
                                    passengerBookingData.currentLongitude
                                StaticDataPasser.storeDestinationLatitude =
                                    passengerBookingData.destinationLatitude
                                StaticDataPasser.storeDestinationLongitude =
                                    passengerBookingData.destinationLongitude


                                if (passengerBookingData.bookingStatus == "Waiting") {
                                    val textColor = ContextCompat.getColor(
                                        this@MapDriverActivity,
                                        R.color.light_blue
                                    )
                                    binding.bookingStatusTextView.setTextColor(textColor)
                                    binding.bookingStatusTextView.text =
                                        "Status: ${passengerBookingData.bookingStatus}"
                                } else if (passengerBookingData.bookingStatus == "StandBy") {
                                    binding.bookingStatusTextView.text =
                                        "Status: ${passengerBookingData.bookingStatus}"
                                }

                                binding.fullNameTextView.text =
                                    "${passengerBookingData.passengerFirstname} ${passengerBookingData.passengerLastname}"

                                binding.userTypeTextView.text =
                                    passengerBookingData.passengerUserType

                                if (passengerBookingData.passengerProfilePicture != "default") {
                                    Glide.with(this@MapDriverActivity)
                                        .load(passengerBookingData.passengerProfilePicture)
                                        .placeholder(R.drawable.loading_gif)
                                        .into(binding.passengerProfilePic)

                                }
                                when (passengerBookingData.passengerUserType) {
                                    "Senior Citizen" -> {
                                        binding.medicalConditionTextView.visibility =
                                            View.VISIBLE
                                        binding.medicalConditionTextView.text =
                                            "Medical Condition(s):\n${passengerBookingData.passengerMedicalCondition}"

                                    }

                                    "Persons with Disability (PWD)" -> {
                                        binding.disabilityTextView.visibility = View.VISIBLE
                                        binding.disabilityTextView.text =
                                            "Disability:\n${passengerBookingData.passengerDisability}"

                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, error.message)
            }

        })

        binding.pickupBtn.setOnClickListener {
            storeTripToDatabase(
                generateRandomTripID(),
                bookingID,
                StaticDataPasser.storePassengerID,
                StaticDataPasser.storeCurrentLatitude,
                StaticDataPasser.storeCurrentLongitude,
                StaticDataPasser.storeDestinationLatitude,
                StaticDataPasser.storeDestinationLongitude
            )

            closePassengerLocationInfoDialog()
        }

        binding.closeBtn.setOnClickListener {
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
                    } else {
                        checkLocationPermission()
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



