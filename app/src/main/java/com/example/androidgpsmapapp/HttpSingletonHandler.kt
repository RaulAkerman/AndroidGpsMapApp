import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

class HttpSingletonHandler(context: Context) {
    companion object {
        private val TAG = HttpSingletonHandler::class.java.simpleName

        @Volatile
        private var INSTANCE: HttpSingletonHandler? = null

        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: HttpSingletonHandler(context).also {
                    INSTANCE = it
                }
            }
    }

    val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext)
    }

    fun <T> addToRequestQueue(req: Request<T>) {
        req.tag = TAG
        requestQueue.add(req)
    }

    fun cancelPendingRequests(tag: Any) {
        if (requestQueue != null) {
            requestQueue.cancelAll(tag)
        }
    }
}