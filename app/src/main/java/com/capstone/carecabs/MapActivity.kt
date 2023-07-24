package com.capstone.carecabs

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.atmosphere.generated.atmosphere
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.layers.generated.skyLayer
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.layers.properties.generated.SkyType
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.rasterDemSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.extension.style.terrain.generated.terrain
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location


class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
//    private val LOCATION_PERMISSION_REQUEST_CODE = 100
//private var mapView: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        mapView = findViewById(R.id.mapView)
        mapView?.getMapboxMap()?.loadStyle(
                styleExtension = style(getString(R.string.custom_map_url)) {
                    +terrain("TERRAIN_SOURCE").exaggeration(1.5)
                    +skyLayer("sky") {
                        skyType(SkyType.ATMOSPHERE)
                        skyAtmosphereSun(listOf(-50.0, 90.2))

                    }
                    +atmosphere { }
                    +projection(ProjectionName.GLOBE)
                }
        ) {
            it.addSource(rasterDemSource("TERRAIN_SOURCE") {
                url("mapbox://mapbox.terrain-rgb")
                tileSize(512)
            })
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
        val pointAnnotationManager = annotationApi?.createPointAnnotationManager(mapView)
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
    }

}



