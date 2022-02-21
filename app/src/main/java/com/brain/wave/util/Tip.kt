package com.brain.wave.util

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.brain.wave.R
import com.google.android.material.snackbar.Snackbar

fun Context.toast(message: CharSequence) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun View.showSnackbar(@StringRes messageId: Int, duration: Int = Snackbar.LENGTH_SHORT): Snackbar {
    return Snackbar.make(this, messageId, duration).also {
        it.view.findViewById<TextView>(R.id.snackbar_text)?.apply {
            maxLines = 2
        }
        it.show()
    }
}

fun Activity.showSnackbar(
    @StringRes messageId: Int,
    duration: Int = Snackbar.LENGTH_SHORT
): Snackbar {
    val view = window.findViewById<View>(android.R.id.content)
    return view.showSnackbar(messageId, duration)
}

fun Fragment.showSnackbar(
    @StringRes messageId: Int,
    duration: Int = Snackbar.LENGTH_SHORT
): Snackbar {
    return requireView().showSnackbar(messageId, duration)
}