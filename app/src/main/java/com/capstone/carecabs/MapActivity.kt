package com.capstone.carecabs

import android.Manifest
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
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.capstone.carecabs.Firebase.FirebaseMain
import com.capstone.carecabs.Utility.StaticDataPasser
import com.capstone.carecabs.databinding.ActivityMapBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.DocumentReference
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import org.json.JSONObject

class MapActivity : AppCompatActivity() {

    private var mapView: MapView? = null
    private var annotationApi: AnnotationPlugin? = null
    private lateinit var annotationConfig: AnnotationConfig
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var layerID = "map_annotation"
    private var markerList: ArrayList<PointAnnotationOptions> = ArrayList()
    private var latitudeList: ArrayList<Double> = ArrayList()
    private var longitudeList: ArrayList<Double> = ArrayList()

    private val TAG: String = "MapActivity"
    private val LOCATION_PERMISSION_REQUEST_CODE = 123

    private lateinit var documentReference: DocumentReference
    private lateinit var intent: Intent
    private lateinit var builder: AlertDialog.Builder
    private lateinit var userNotVerifiedDialog: AlertDialog

    private lateinit var binding: ActivityMapBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = findViewById(R.id.mapView)

        checkIfUserIsVerified()
        checkLocationPermission()

        binding.imgBackBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.setLocation

        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.setLocation -> {
                    binding.setLocationLayout.visibility = View.VISIBLE
                }

                R.id.myBookings -> {
                    binding.setLocationLayout.visibility = View.GONE

                    intent = Intent(this, Bookings::class.java)
                    startActivity(intent)
                    finish()
                }

                R.id.help -> {
                    binding.setLocationLayout.visibility = View.GONE

                }
            }
            true
        }


        mapView?.getMapboxMap()?.loadStyleUri(
            Style.MAPBOX_STREETS,
            // After the style is loaded, initialize the Location component.
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    zoomCamera()

                    annotationApi = mapView?.annotations
                    annotationConfig = AnnotationConfig(
                        layerId = layerID
                    )

                    pointAnnotationManager =
                        annotationApi?.createPointAnnotationManager(annotationConfig)

                }
            }
        )


//        mapView?.location?.locationPuck = LocationPuck2D(
//            topImage = AppCompatResources.getDrawable(
//                this,
//                com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_icon
//            ),
//            bearingImage = AppCompatResources.getDrawable(
//                this,
//                com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_bearing_icon
//            ),
//            shadowImage = AppCompatResources.getDrawable(
//                this,
//                com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_stroke_icon
//            ),
//            scaleExpression = interpolate
//            {
//                linear()
//                zoom()
//                stop {
//                    literal(0.0)
//                    literal(0.6)
//                }
//                stop {
//                    literal(20.0)
//                    literal(1.0)
//                }
//            }.toJson()
//        )

        //      mapView?.getMapboxMap()?.loadStyle(
//                styleExtension = style(getString(R.string.custom_map_url)) {
//                    +terrain("TERRAIN_SOURCE").exaggeration(1.5)
//                    +skyLayer("sky") {
//                        skyType(SkyType.ATMOSPHERE)
//                        skyAtmosphereSun(listOf(-50.0, 90.2))
//
//                    }
//                    +atmosphere { }
//                    +projection(ProjectionName.GLOBE)
//                }
//        ) {
//            it.addSource(rasterDemSource("TERRAIN_SOURCE") {
//                url("mapbox://mapbox.terrain-rgb")
//                tileSize(512)
//            })
//        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission already granted, proceed to location retrieval
        } else {
            // Request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun checkIfUserIsVerified() {
        if (FirebaseMain.getUser() != null) {
            documentReference = FirebaseMain.getFireStoreInstance()
                .collection(StaticDataPasser.userCollection).document(FirebaseMain.getUser().uid)

            documentReference.get().addOnSuccessListener {
                if (it != null && it.exists()) {
                    val getVerificationStatus = it.getString("verificationStatus")

                    if (getVerificationStatus == "Not Verified") {
                        showUserNotVerifiedDialog()

                    } else {
                        checkUserType()
                    }
                } else {

                }
            }.addOnFailureListener {
                Log.e(TAG, it.message.toString())
            }

        } else {
            FirebaseMain.signOutUser()

            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun checkUserType() {
        if (FirebaseMain.getUser() != null) {
            documentReference = FirebaseMain.getFireStoreInstance()
                .collection(StaticDataPasser.userCollection).document(FirebaseMain.getUser().uid)

            documentReference.get().addOnSuccessListener {
                if (it != null && it.exists()) {
                    val getUserType = it.getString("userType")

                    if (getUserType == "Driver") {
                        createDummyMarkers()
                    } else {

                    }
                } else {
                    Log.e(TAG, "Document not Exist")

                }
            }.addOnFailureListener {
                Log.e(TAG, it.message.toString())
            }

        } else {
            intent = Intent(this, Login::class.java)
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

            closeUserNotVerifiendDialog()
        }

        builder.setView(dialogView)
        userNotVerifiedDialog = builder.create()
        userNotVerifiedDialog.show()
    }

    private fun closeUserNotVerifiendDialog() {
        if (userNotVerifiedDialog != null && userNotVerifiedDialog.isShowing) {
            userNotVerifiedDialog.dismiss()
        }
    }

    private fun zoomCamera() {
        mapView!!.getMapboxMap().setCamera(
            CameraOptions.Builder().center(Point.fromLngLat(125.60288851565707, 7.065679527724293))
                .zoom(10.0)
                .build()

        )
    }

    private fun createDummyMarkers() {
        latitudeList.add(7.191233292469051)
        longitudeList.add(125.45471619362087)

        latitudeList.add(7.0569787855098625)
        longitudeList.add(125.52385607415545)

        latitudeList.add(7.11563147517752)
        longitudeList.add(125.55617682891864)
    }

    private fun showPassengerMarkerOnMap() {

        pointAnnotationManager?.addClickListener(OnPointAnnotationClickListener { annotation: PointAnnotation ->

            onMarkerItemClick(annotation)
            true
        })

        var bitmap =
            convertDrawableToBitmap(AppCompatResources.getDrawable(this, R.drawable.location_3))

        for (i in 0 until 3) {
            var jsonObject = JSONObject()
            jsonObject.put("someValue", 1)

            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(longitudeList[i], latitudeList[i]))
                .withData(Gson().fromJson(jsonObject.toString(), JsonElement::class.java))
                .withIconImage(bitmap)

            markerList.add(pointAnnotationOptions)
        }

        pointAnnotationManager?.create(markerList)
    }

    private fun onMarkerItemClick(marker: PointAnnotation): Boolean {
        var jsonElement: JsonElement? = marker.getData()

        AlertDialog.Builder(this)
            .setTitle("Dummy Marker")
            .setMessage("Interacted Marker: $marker")
            .setPositiveButton("K") { dialog, whichButton ->
                dialog.dismiss()
            }.show()

        return true
    }

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap {

        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
            val constantState = sourceDrawable?.constantState
            val drawable = constantState?.newDrawable()?.mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable!!.intrinsicWidth, drawable!!.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }


    private fun flyToCameraPosition() {
        var cameraCenterCoordinates = com.mapbox.geojson.Point.fromLngLat(8.0061, 46.5778)

        var cameraOptions = CameraOptions.Builder()
            .center(cameraCenterCoordinates)
            .bearing(130.0)
            .pitch(75.0)
            .zoom(13.0)
            .build()

        var animationOptions = MapAnimationOptions.Builder().duration(15000).build()

        mapView!!.getMapboxMap().flyTo(cameraOptions, animationOptions)
    }

    private fun addCustomAnnotation() {
        val annotationApi = mapView?.annotations
        val pointAnnotationManager =
            mapView?.let { annotationApi?.createPointAnnotationManager(it) }
        // Set options for the resulting symbol layer.
        val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
            // Define a geographic coordinate.
            .withPoint(Point.fromLngLat(18.06, 59.31))
            // Specify the bitmap you assigned to the point annotation
            // The bitmap will be added to map style automatically.
            .withIconImage(getDrawable(R.drawable.bit_mar)!!.toBitmap())
// Add the resulting pointAnnotation to the map.
        pointAnnotationManager?.create(pointAnnotationOptions)
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


    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }


    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }


    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()

        closeUserNotVerifiendDialog()
    }

    override fun onPause() {
        super.onPause()

        closeUserNotVerifiendDialog()
    }
}



