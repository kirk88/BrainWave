package com.brain.wave.util

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.brain.wave.R


fun AppCompatActivity.setupActionBar(action: ActionBar.() -> Unit = {}) {
    findViewById<Toolbar>(R.id.toolbar)?.let {
        setSupportActionBar(it)
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            action()
        }
    }
}

fun Context.toAppSetting() {
    try {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    } catch (_: Throwable) {
    }
}

val Context.isLocationEnabled: Boolean
    get() {
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager? ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled
        }
        val gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return gps || network
    }

fun Context.toLocationSetting() {
    try {
        val intent = Intent(ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    } catch (_: Throwable) {
    }
}