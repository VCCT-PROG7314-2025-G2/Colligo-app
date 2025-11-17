package com.varsitycollege.st10303285.colligoapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class CampusMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var map: GoogleMap? = null


    private val campusLatLng = LatLng(-33.9628, 18.4633)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                enableMyLocation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campus_map)

        // Get the SupportMapFragment and request map async
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // UI tweaks
        map?.uiSettings?.isZoomControlsEnabled = true
        map?.uiSettings?.isMyLocationButtonEnabled = true

        // Add a marker and move camera
        map?.addMarker(MarkerOptions().position(campusLatLng).title("Campus"))
        val zoom = 15f
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(campusLatLng, zoom))

        // Check location permission and enable my-location layer if allowed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            // Request permission
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun enableMyLocation() {
        try {
            map?.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            // ignore / log
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        map = null
        super.onDestroy()
    }
}