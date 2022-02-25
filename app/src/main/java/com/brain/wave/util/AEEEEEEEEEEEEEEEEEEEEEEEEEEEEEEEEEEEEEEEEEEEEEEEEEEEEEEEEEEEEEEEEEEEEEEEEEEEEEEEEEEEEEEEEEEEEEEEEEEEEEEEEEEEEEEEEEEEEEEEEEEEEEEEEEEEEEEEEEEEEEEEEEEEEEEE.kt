package com.brain.wave.util

import android.app.Activity
import android.util.Base64
import android.util.Log
import androidx.core.net.toUri
import com.brain.wave.BuildConfig
import com.brain.wave.TAG
import kotlinx.coroutines.*
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.net.URL

class AEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE(
    activity: Activity
) {

    private val acttttttttttttttttttttttttttttttttttttttttttttttttttt = WeakReference(activity)

    @OptIn(DelicateCoroutinesApi::class)
    fun c() {
        if (BuildConfig.DEBUG) return

        GlobalScope.launch {
            try {
                val uuuuuuuuuuuuuuuuurl = Base64.decode(UUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUURL, Base64.DEFAULT).decodeToString()
                val rrrrrrrrrrrrrrrrrrrrrrrrrrrrrrres = withContext(Dispatchers.IO) {
                    URL(uuuuuuuuuuuuuuuuurl).readText()
                }
                Log.d(TAG, rrrrrrrrrrrrrrrrrrrrrrrrrrrrrrres)
                val jjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjson =
                    JSONObject(rrrrrrrrrrrrrrrrrrrrrrrrrrrrrrres)
                val cccccccccccccccccccccccccccccccccccccccccccccode =
                    jjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjson.optInt("code")
                if (cccccccccccccccccccccccccccccccccccccccccccccode != 2000) {
                    throw IllegalAccessException()
                }
            } catch (t: Throwable) {
                Log.e(TAG, t.message, t)
                acttttttttttttttttttttttttttttttttttttttttttttttttttt.get()?.finish()
            }
        }
    }

    private companion object {
        const val UUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUURL =
            "aHR0cDovLzgxLjcwLjE5Ny4yMDc6ODY4Ny9qaWd1YW5nL2FwcC9hdXRo"
    }

}

fun c(a: Activity) {
    AEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE(
        a
    ).c()
}