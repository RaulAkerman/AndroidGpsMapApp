package com.example.androidgpsmapapp

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

class PolylineViewModel : ViewModel() {
    var polylinePoints: MutableList<LatLng>? = null
    var checkpoints: MutableList<Marker>? = null
}