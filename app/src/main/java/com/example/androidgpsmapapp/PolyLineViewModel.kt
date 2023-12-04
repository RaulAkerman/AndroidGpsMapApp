package com.example.androidgpsmapapp

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng

class PolylineViewModel : ViewModel() {
    var polylinePoints: MutableList<LatLng>? = null
    var checkpoints: MutableList<LatLng>? = null
    var waypoints: MutableList<LatLng>? = null
}