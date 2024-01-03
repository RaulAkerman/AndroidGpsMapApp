package com.example.androidgpsmapapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.androidgpsmapapp.C.ACTION_TIMER_RESET
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TimerService : Service() {
    private val scheduledExecutorService = Executors.newScheduledThreadPool(1)
    private var seconds = 0
    companion object {
        private val TAG = TimerService::class.java.simpleName
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(C.ACTION_TIMER_SERVICE_START))
        when (intent?.action) {
            ACTION_TIMER_RESET -> {
                seconds = 0
                sendTimerBroadcast()
            }
        }
        startTimer()
        return START_STICKY
    }

    private fun startTimer() {
        scheduledExecutorService.scheduleAtFixedRate(
            {
                Log.d(TAG, "scheduledExecutorService 'timer' running: $seconds seconds")
                val intent = Intent(C.ACTION_TIMER)
                seconds++
                intent.putExtra(C.PAYLOAD_TIME, "$seconds")
                val timeValue = intent.getStringExtra(C.PAYLOAD_TIME)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            },
            0,
            1,
            TimeUnit.SECONDS
        )
    }

    private fun sendTimerBroadcast() {
        val intent = Intent(C.ACTION_TIMER)
        intent.putExtra(C.PAYLOAD_TIME, "$seconds")
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy Service")
        super.onDestroy()
        scheduledExecutorService.shutdown()
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(C.ACTION_TIMER_SERVICE_DESTROY))
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}
