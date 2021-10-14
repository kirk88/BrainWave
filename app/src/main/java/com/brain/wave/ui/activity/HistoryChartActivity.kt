package com.brain.wave.ui.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.brain.wave.R
import com.brain.wave.ui.BaseActivity
import com.brain.wave.ui.fragment.ChartFragment
import com.brain.wave.util.setupActionBar

class HistoryChartActivity : BaseActivity(R.layout.activity_history_chart) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra("title")
        val path = intent.getStringExtra("path")
        setupActionBar{
            setTitle(title)
        }

        supportFragmentManager.findFragmentById(R.id.fragment_container) as? ChartFragment
            ?: ChartFragment().also {
                it.arguments = bundleOf("path" to path)

                supportFragmentManager.beginTransaction().apply {
                    replace(R.id.fragment_container, it)
                    commit()
                }
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}