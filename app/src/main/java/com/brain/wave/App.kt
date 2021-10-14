package com.brain.wave

import android.app.Application
import android.content.Context
import android.content.res.Resources
import java.util.*


lateinit var appContext: Context
    private set

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this
    }

    override fun getResources(): Resources? {
        val resources = super.getResources()
        val configuration = resources.configuration
        configuration.setLocale(Locale.ENGLISH)
        return resources
    }

}