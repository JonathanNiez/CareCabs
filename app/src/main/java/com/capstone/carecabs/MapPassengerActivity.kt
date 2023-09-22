package com.capstone.carecabs

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.capstone.carecabs.Firebase.FirebaseMain
import com.capstone.carecabs.Utility.StaticDataPasser
import com.capstone.carecabs.databinding.ActivityMapPassengerBinding
import com.google.firebase.firestore.DocumentReference
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
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
import com.mapbox.maps.viewannotation.ViewAnnotationUpdateMode
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import java.util.UUID

class MapPassengerActivity : AppCompatActivity() {

    private val TAG: String = "MapPassengerActivity"
    private lateinit var documentReference: DocumentReference
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapPassengerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkIfUserIsVerified()
        initializeBottomNavButtons()

        viewAnnotationManager = binding.mapView.viewAnnotationManager

        onMapReady()

//        binding.setLocationLayout.visibility = View.GONE


    }

    private fun loadMapStyle() {
        binding.mapView.getMapboxMap()
            .loadStyleUri(getString(R.string.custom_map_style_url))
            // After the style is loaded, initialize the Location component.
            {
                binding.mapView.gestures.addOnMapClickListener {


                    Toast.makeText(
                        this@MapPassengerActivity,
                        "Map was clicked",
                        Toast.LENGTH_LONG
                    ).show()

                    true
                }

//                addAnnotationToMap()

//                annotationApi = binding.mapView.annotations
//                annotationConfig = AnnotationConfig(
//                    layerId = layerID
//                )
//
//                pointAnnotationManager =
//                    annotationApi?.createPointAnnotationManager(annotationConfig)
            }
    }

    private fun addAnnotationToMap() {
// Create an instance of the Annotation API and get the PointAnnotationManager.
        bitmapFromDrawableRes(
            this@MapPassengerActivity,
            R.drawable.location_2
        )?.let {
            val annotationApi = binding.mapView.annotations
            val pointAnnotationManager = annotationApi.createPointAnnotationManager(binding.mapView)
// Set options for the resulting symbol layer.
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
// Define a geographic coordinate.
                .withPoint(Point.fromLngLat(18.06, 59.31))
// Specify the bitmap you assigned to the point annotation
// The bitmap will be added to map style automatically.
                .withIconImage(it)
// Add the resulting pointAnnotation to the map.
            pointAnnotationManager.create(pointAnnotationOptions)
        }
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

    private fun generateRandomTripId(): String {
        val uuid = UUID.randomUUID()
        return uuid.toString()
    }

    private fun onMapReady() {
        mapboxMap = binding.mapView.getMapboxMap().apply {
            loadStyleUri(getString(R.string.custom_map_style_url)) {

                initLocationComponent()
                setupGesturesListener()


            }
        }

        binding.mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(14.0)
                .build()
        )

//        binding.mapView.getMapboxMap()
//            .loadStyleUri(getString(R.string.custom_map_style_url)) {
//            }
    }


//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.menu_view_annotation, menu)
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.action_view_annotation_fixed_delay -> {
//                viewAnnotationManager.setViewAnnotationUpdateMode(ViewAnnotationUpdateMode.MAP_FIXED_DELAY)
//                true
//            }
//
//            R.id.action_view_annotation_map_synchronized -> {
//                viewAnnotationManager.setViewAnnotationUpdateMode(ViewAnnotationUpdateMode.MAP_SYNCHRONIZED)
//                true
//            }
//
//            else -> super.onOptionsItemSelected(item)
//        }
//    }

//    override fun onMapClick(point: Point): Boolean {
//        addViewAnnotation(point)
//        return true
//    }

    @SuppressLint("SetTextI18n")
    private fun addViewAnnotation(point: Point) {
        val viewAnnotation = viewAnnotationManager.addViewAnnotation(
            resId = R.layout.activity_map_passenger,
            options = viewAnnotationOptions {
                geometry(point)
                allowOverlap(true)
            }
        )
        val pointLat: String = point.latitude().toString()
        val pointLong: String = point.longitude().toString()

        binding.desiredDestinationTextView.text = pointLat + "\n" + pointLong


//        Toast.makeText(this@MapPassengerActivity, point.toString(), Toast.LENGTH_LONG).show()

    }

    private companion object {
        const val SELECTED_ADD_COEF_PX = 25
        const val STARTUP_TEXT = "Click on a map to add a view annotation."
        const val ADD_VIEW_ANNOTATION_TEXT = "Add view annotations to re-frame map camera"
    }

    private fun setupGesturesListener() {
        binding.mapView.gestures.addOnMoveListener(onMoveListener)

        binding.mapView.gestures.addOnMapClickListener {

            addViewAnnotation(it)

            true
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()

    }

}