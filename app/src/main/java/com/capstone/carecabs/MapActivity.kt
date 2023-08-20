package com.capstone.carecabs

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.integrity.v
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateBearing
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.mapbox.maps.plugin.viewport.data.OverviewViewportStateOptions
import com.mapbox.maps.plugin.viewport.state.FollowPuckViewportState
import com.mapbox.maps.plugin.viewport.state.OverviewViewportState
import com.mapbox.maps.plugin.viewport.viewport

class MapActivity : AppCompatActivity() {

    //    private lateinit var mapView: MapView
    var mapView: MapView? = null

//    private val LOCATION_PERMISSION_REQUEST_CODE = 69
//    private lateinit var locationEngine: LocationEngine
//    private lateinit var mapboxMap: MapboxMap
//    private lateinit var coordinatesTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val imgBackButton: ImageButton = findViewById(R.id.imgBackBtn)
        imgBackButton.setOnClickListener {
            var intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        mapView = findViewById(R.id.mapView)
        mapView?.getMapboxMap()?.loadStyleUri(
            Style.MAPBOX_STREETS,
            // After the style is loaded, initialize the Location component.
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    mapView?.location?.updateSettings {
                        enabled = true
                        pulsingEnabled = true
                    }
                }
            }
        )



    mapView?.location?.locationPuck = LocationPuck2D(
    topImage = AppCompatResources.getDrawable(
    this,
    com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_icon
    ),
    bearingImage = AppCompatResources.getDrawable(
    this,
    com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_bearing_icon
    ),
    shadowImage = AppCompatResources.getDrawable(
    this,
    com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_stroke_icon
    ),
    scaleExpression = interpolate
    {
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

//    private fun flyToCameraPosition() {
//        var cameraCenterCoordinates = com.mapbox.geojson.Point.fromLngLat(8.0061, 46.5778)
//
//        var cameraOptions = CameraOptions.Builder()
//                .center(cameraCenterCoordinates)
//                .bearing(130.0)
//                .pitch(75.0)
//                .zoom(13.0)
//                .build()
//
//        var animationOptions = MapAnimationOptions.Builder().duration(15000).build()
//
//        mapView!!.getMapboxMap().flyTo(cameraOptions, animationOptions)
//    }

//    private fun addCustomAnnotation() {
//        val annotationApi = mapView?.annotations
//        val pointAnnotationManager = annotationApi?.createPointAnnotationManager(mapView)
//        // Set options for the resulting symbol layer.
//        val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
//                // Define a geographic coordinate.
//                .withPoint(Point.fromLngLat(18.06, 59.31))
//                // Specify the bitmap you assigned to the point annotation
//                // The bitmap will be added to map style automatically.
//                .withIconImage(getDrawable(R.drawable.bit_mar)!!.toBitmap())
//// Add the resulting pointAnnotation to the map.
//        pointAnnotationManager?.create(pointAnnotationOptions)
//    }


override fun onStart() {
    super.onStart()
    mapView?.onStart()
}

//override fun onResume() {
//    super.onResume()
//    mapView?.onResume()
//}
//
//override fun onPause() {
//    super.onPause()
//    mapView?.onPause()
//}

override fun onStop() {
    super.onStop()
    mapView?.onStop()
}

//override fun onSaveInstanceState(outState: Bundle) {
//    super.onSaveInstanceState(outState)
//    mapView?.onSaveInstanceState(outState)
//}

override fun onLowMemory() {
    super.onLowMemory()
    mapView?.onLowMemory()
}

override fun onDestroy() {
    super.onDestroy()
    mapView?.onDestroy()
}

}



