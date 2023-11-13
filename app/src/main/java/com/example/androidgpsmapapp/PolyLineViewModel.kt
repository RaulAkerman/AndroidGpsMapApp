package com.example.androidgpsmapapp

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng

class PolylineViewModel : ViewModel() {
    var polylinePoints: MutableList<LatLng>? = null
}