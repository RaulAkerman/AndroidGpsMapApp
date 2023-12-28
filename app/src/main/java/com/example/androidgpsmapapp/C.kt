package com.example.androidgpsmapapp

object C {
    private const val PREFIX = "com.example.androidgpsmapapp."

    const val ACTION_LOCATION_UPDATE = PREFIX + "ACTION_LOCATION_UPDATE"
    const val ACTION_PLACE_CHECKPOINT = PREFIX + "ACTION_PLACE_CHECKPOINT"
    const val DATA_LOCATION_UPDATE_LAT = "DATA_LOCATION_UPDATE_LAT"
    const val DATA_LOCATION_UPDATE_LON = "DATA_LOCATION_UPDATE_LON"
    const val DATA_LOCATION_UPDATE_BEARING = "DATA_LOCATION_UPDATE_HEADING"
    const val DATA_LOCATION_UPDATE_SPEED = "DATA_LOCATION_UPDATE_SPEED"
    const val DATA_LOCATION_UPDATE_ACCURACY = "DATA_LOCATION_UPDATE_ACCURACY"
    const val DATA_LOCATION_UPDATE_ALTITUDE = "DATA_LOCATION_UPDATE_ALTITUDE"
    const val DATA_LOCATION_UPDATE_VERTICAL_ACCURACY = "DATA_LOCATION_UPDATE_VERTICAL_ACCURACY"
    const val SAVED_POLYLINE_POINTS = "SAVED_POLYLINE_POINTS"
    const val SAVED_CHECKPOINTS = "SAVED_CHECKPOINTS"
    const val SAVED_WAYPOINTS = "SAVED_WAYPOINTS"
    const val ACTION_TIMER_SERVICE_START = PREFIX + "service_start"
    const val ACTION_TIMER_RESET = PREFIX + "ACTION_TIMER_RESET"
    const val ACTION_TIMER = PREFIX + "time"
    const val PAYLOAD_TIME = PREFIX + "payload.time"
    const val ACTION_TIMER_SERVICE_DESTROY = PREFIX + "service_destroy"

    const val NOTIFICATION_CHANNEL = "default"
    const val REQUEST_PERMISSIONS_CODE = 1234
}