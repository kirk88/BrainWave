package com.brain.wave.util

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.brain.wave.BuildConfig
import com.brain.wave.appContext
import com.brain.wave.model.Data
import com.brain.wave.model.parseRawData
import java.io.File


fun getDataList(file: File): List<List<Data>> {
    return file.runCatching {
        bufferedReader().use {
            val list = mutableListOf<List<Data>>()
            var line = it.readLine()
            while (line != null) {
                line.parseRawData()?.data?.let { data -> list.add(data) }
                line = it.readLine()
            }
            list
        }
    }.getOrElse { emptyList() }
}

fun getTextFileIntent(file: File): Intent {
    val share = Intent(Intent.ACTION_SEND)
    share.addCategory(Intent.CATEGORY_DEFAULT)
    share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val uri = if (Build.VERSION.SDK_INT >= 24) {
        FileProvider.getUriForFile(appContext, BuildConfig.APPLICATION_ID + ".fileprovider", file)
    } else {
        Uri.fromFile(file)
    }
    share.putExtra(Intent.EXTRA_STREAM, uri)
    share.type = "text/plain"
    return share
}

fun getShareIntent(file: File): Intent {
    val sendIntent = Intent()
    sendIntent.action = Intent.ACTION_SEND
    sendIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    val uri = if (Build.VERSION.SDK_INT >= 24) {
        FileProvider.getUriForFile(appContext, BuildConfig.APPLICATION_ID + ".fileprovider", file)
    } else {
        Uri.fromFile(file)
    }
    sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
    sendIntent.type = "*/*"
    return Intent.createChooser(sendIntent, "Share")
}