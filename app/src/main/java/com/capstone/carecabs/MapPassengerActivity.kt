package com.capstone.carecabs

import android.content.Context
import android.content.Intent
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
import com.capstone.carecabs.Firebase.FirebaseMain
import com.capstone.carecabs.Utility.StaticDataPasser
import com.capstone.carecabs.databinding.ActivityMapPassengerBinding
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions.Companion.cameraAnimatorOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import java.util.Calendar
import java.util.UUID


class MapPassengerActivity : AppCompatActivity(), OnMapClickListener {

    private val TAG: String = "MapPassengerActivity"
    private lateinit var documentReference: DocumentReference
    private lateinit var collectionReference: CollectionReference
    private lateinit var binding: ActivityMapPassengerBinding
    private lateinit var userNotVerifiedDialog: AlertDialog
    private lateinit var builder: AlertDialog.Builder
    private lateinit var intent: Intent

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
    private lateinit var viewAnnotationManager: ViewAnnotationManager
    private val viewAnnotationViews = mutableListOf<View>()
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var pointAnnotationOptions: PointAnnotationOptions

    private lateinit var pointAnnotation: PointAnnotation
    private lateinit var viewAnnotation: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapPassengerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkIfUserIsVerified()
        initializeBottomNavButtons()

        binding.recenterBtn.setOnClickListener {

        }

        viewAnnotationManager = binding.mapView.viewAnnotationManager

        onMapReady()


//        binding.setLocationLayout.visibility = View.GONE

    }

    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
// copying drawable object to not manipulate on the same reference
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    private fun initializeBottomNavButtons() {
        binding.bottomNavigationView.selectedItemId = R.id.setLocation

        binding.bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.setLocation -> {
                    binding.setLocationLayout.visibility = View.VISIBLE
                }

                R.id.myBookings -> {
                    binding.setLocationLayout.visibility = View.GONE
                    val intent = Intent(this, BookingsActivity::class.java)
                    startActivity(intent)
                }

                R.id.help -> {
                    binding.setLocationLayout.visibility = View.GONE

                }
            }
            true
        }
    }

    private fun recenterToCurrentLocation() {
        Log.i(TAG, "zoomCamera")

//        binding.mapView.getMapboxMap().setCamera(
//            CameraOptions.Builder()
//                .center(view)
//                .zoom(10.0)
//                .build()
//
//        )
    }

    private fun onMapReady() {
        Toast.makeText(this@MapPassengerActivity, "onMapReady", Toast.LENGTH_SHORT).show()

        mapboxMap = binding.mapView.getMapboxMap().apply {
            loadStyleUri(getString(R.string.custom_map_style_url)) {

                initLocationComponent()
                setupGesturesListener()
                loadCoordinatesToMapFromFireStore()

                binding.mapView.camera.apply {
                    val bearing = createBearingAnimator(cameraAnimatorOptions(-45.0)) {
                        duration = 4000
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    val zoom = createZoomAnimator(
                        cameraAnimatorOptions(14.0) {
                            startValue(3.0)
                        }
                    ) {
                        duration = 4000
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    val pitch = createPitchAnimator(
                        cameraAnimatorOptions(55.0) {
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

    private fun addViewAnnotation(point: Point) {
        Toast.makeText(this@MapPassengerActivity, "addViewAnnotation", Toast.LENGTH_SHORT).show()

        val viewAnnotation = viewAnnotationManager.addViewAnnotation(
            resId = R.layout.activity_map_passenger,
            options = viewAnnotationOptions {
                geometry(point)
                allowOverlap(true)
            }
        )
        viewAnnotationViews.add(viewAnnotation)

        binding.setLocationBtn.setOnClickListener {
            if (viewAnnotationViews.isNotEmpty()) {
                val cameraOptions = viewAnnotationManager.cameraForAnnotations(viewAnnotationViews)
                cameraOptions?.let {
                    mapboxMap.flyTo(it)
                }
            } else {
                Toast.makeText(
                    this@MapPassengerActivity,
                    "ADD_VIEW_ANNOTATION_TEXT",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val pointLat: String = point.latitude().toString()
        val pointLong: String = point.longitude().toString()

        val pointLongDouble: Double = point.longitude()
        val pointLatDouble: Double = point.latitude()

        addAnnotationToMap(pointLongDouble, pointLatDouble)

        binding.desiredDestinationTextView.text = pointLat + "\n" + pointLong

    }

    private fun addAnnotationToMap(pointLongitudeDouble: Double, pointLatitudeDouble: Double) {
        Toast.makeText(this@MapPassengerActivity, "addAnnotationToMap", Toast.LENGTH_SHORT).show()

// Create an instance of the Annotation API and get the PointAnnotationManager.
        bitmapFromDrawableRes(
            this@MapPassengerActivity,
            R.drawable.location_pin_128
        )?.let {

            val annotationApi = binding.mapView.annotations
            pointAnnotationManager = annotationApi.createPointAnnotationManager(binding.mapView)
// Set options for the resulting symbol layer.
            pointAnnotationOptions = PointAnnotationOptions()
// Define a geographic coordinate.
                .withPoint(Point.fromLngLat(pointLongitudeDouble, pointLatitudeDouble))
// Specify the bitmap you assigned to the point annotation
// The bitmap will be added to map style automatically.
                .withIconImage(it)
// Add the resulting pointAnnotation to the map.
            pointAnnotationManager.create(pointAnnotationOptions)

            pointAnnotationManager.addClickListener {
                Toast.makeText(this@MapPassengerActivity, "Annotation Clicked", Toast.LENGTH_SHORT)
                    .show()

                true
            }
        }
    }

    private fun removeMarkerFromMap(point: Point) {
        Toast.makeText(this@MapPassengerActivity, "removeMarkerFromMap", Toast.LENGTH_SHORT).show()

        val pointAnnotationOptions: PointAnnotationOptions =
            PointAnnotationOptions().withPoint(point)

        var annotationID: Long? = null
        pointAnnotationManager.annotations.forEach {
            if (it.point == point) annotationID = it.id
        }
//Remember this point annotation manager should be global & initialised only' once
        pointAnnotationManager
            .delete(
                pointAnnotationOptions
                    .build(
                        annotationID!!,
                        pointAnnotationManager
                    )
            )
    }

    private companion object {
        private val CAMERA_TARGET = cameraOptions {
            center(Point.fromLngLat(-74.0060, 40.7128))
            zoom(3.0)
        }
    }

    private fun setupGesturesListener() {
        binding.mapView.gestures.addOnMoveListener(onMoveListener)

        mapboxMap = binding.mapView.getMapboxMap().apply {
            addOnMapClickListener(this@MapPassengerActivity)
        }
    }

    private fun initLocationComponent() {
        val locationComponentPlugin = binding.mapView.location
        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    this@MapPassengerActivity,
                    R.drawable.mapbox_navigation_puck_icon,
                ),
                scaleExpression = interpolate {
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

//    private fun prepareAnnotationMarker(mapView: MapView, iconBitmap: Bitmap) {
//        val annotationPlugin = mapView.annotations
//        val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
//            .withPoint(POINT)
//            .withIconImage(iconBitmap)
//            .withIconAnchor(IconAnchor.BOTTOM)
//            .withDraggable(true)
//        pointAnnotationManager = annotationPlugin.createPointAnnotationManager()
//        pointAnnotation = pointAnnotationManager.create(pointAnnotationOptions)
//    }

    private fun onCameraTrackingDismissed() {
        Toast.makeText(this, "onCameraTrackingDismissed", Toast.LENGTH_SHORT).show()

        binding.mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        binding.mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        binding.mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        binding.mapView.gestures.removeOnMoveListener(onMoveListener)
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

    private fun loadCoordinatesToMapFromFireStore() {

        collectionReference = FirebaseMain.getFireStoreInstance()
            .collection(StaticDataPasser.locationCollection)

        collectionReference.get().addOnSuccessListener {

            if (it != null) {
                for (document in it.documents){
                    val getLatitude = document.getDouble("latitude")!!.toDouble()
                    val getLongitude = document.getDouble("longitude")!!.toDouble()

                    addAnnotationToMap(getLongitude, getLatitude)

                }
            }

        }.addOnFailureListener {
            Log.e(TAG, it.message.toString())
        }
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

    private fun generateRandomLocationID(): String {
        val uuid = UUID.randomUUID()
        return uuid.toString()
    }

    private fun storeCoordinatesToFireStore(point: Point) {
        if (FirebaseMain.getUser() != null) {

            documentReference = FirebaseMain.getFireStoreInstance()
                .collection(StaticDataPasser.locationCollection)
                .document(generateRandomLocationID())

            val coordinates = HashMap<String, Any>()
            coordinates["locationID"] = generateRandomLocationID()
            coordinates["longitude"] = point.longitude()
            coordinates["latitude"] = point.latitude()
            coordinates["locationTime"] = getCurrentTimeAndDate()

            documentReference.set(coordinates).addOnSuccessListener {
                Toast.makeText(this, "Coordinates Uploaded Success", Toast.LENGTH_LONG).show()

            }.addOnFailureListener {
                Toast.makeText(this, "Coordinates Failed to Upload", Toast.LENGTH_LONG).show()

                Log.e(TAG, it.message.toString())
            }
        }
    }

    private fun showUserNotVerifiedDialog() {
        builder = AlertDialog.Builder(this)
        builder.setCancelable(false)

        val dialogView = layoutInflater.inflate(R.layout.dialog_user_not_verified, null)
        val okBtn = dialogView.findViewById<Button>(R.id.okBtn)

        okBtn.setOnClickListener {
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

    override fun onMapClick(point: Point): Boolean {
        addViewAnnotation(point)

        storeCoordinatesToFireStore(point)

        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()

    }
}