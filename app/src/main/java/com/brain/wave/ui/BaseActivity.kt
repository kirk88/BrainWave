package com.brain.wave.ui

import android.content.res.Resources
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import java.util.*

abstract class BaseActivity : AppCompatActivity {

    constructor() : super()

    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    override fun getResources(): Resources {
        val resources = super.getResources()
        val configuration = resources.configuration
        configuration.setLocale(Locale.ENGLISH)
        return resources
    }

}