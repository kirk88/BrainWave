package com.brain.wave.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.brain.wave.R
import com.brain.wave.TAG
import com.brain.wave.model.Value
import com.brain.wave.ui.widget.BWLineChart
import com.brain.wave.util.TimeCounter
import com.brain.wave.util.getDataList
import kotlinx.coroutines.*
import java.io.File

class ChartFragment : Fragment(R.layout.fragment_chart) {

    private var chartContainer: ViewGroup? = null
    private var progressBar: ContentLoadingProgressBar? = null
    private var emptyView: View? = null

    private val cachedValuesMap = linkedMapOf<String, MutableList<Value>>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        chartContainer = view.findViewById(R.id.chart_container)
        progressBar = view.findViewById(R.id.progress)
        emptyView = view.findViewById(R.id.empty_view)

        val filePath = arguments?.getString("path")

        if (filePath.isNullOrBlank()) {
            emptyView?.isVisible = true
        } else {
            addChartValuesFromFile(File(filePath))
        }
    }

    fun addChartValuesFromFile(file: File) {
        progressBar?.show()

        lifecycleScope.launch(Dispatchers.Main + CoroutineExceptionHandler { _, _ ->
            Log.e(TAG, "add data from file failed.")
        }) {
            delay(400)

            val dataList = withContext(Dispatchers.IO) {
                getDataList(file)
            }

            if (dataList.isEmpty()) {
                emptyView?.isVisible = true
            } else {
                val dataMap = dataList.toValuesMap()
                renderView(dataMap)
            }

            progressBar?.hide()
        }
    }

    fun addAllChartValues(valuesList: List<List<Value>>) {
        if (valuesList.isEmpty()) return

        lifecycleScope.launch(Dispatchers.Main + CoroutineExceptionHandler { _, _ ->
            Log.e(TAG, "add all chart data failed.")
        }) {
            val valuesMap = valuesList.toValuesMap()
            renderView(valuesMap, true)
        }
    }

    fun addChartValues(values: List<Value>) {
        if (values.isEmpty()) return

        lifecycleScope.launch(Dispatchers.Main + CoroutineExceptionHandler { _, _ ->
            Log.e(TAG, "add chart data failed.")
        }) {
            val dataMap = listOf(values).toValuesMap()
            renderView(dataMap, true)
        }
    }

    fun clearChartValues() {
        synchronized(cachedValuesMap) {
            cachedValuesMap.clear()
        }

        lifecycleScope.launch {
            chartContainer?.forEach {
                it.findViewById<BWLineChart>(R.id.chart)?.clear()
            }
        }
    }

    private suspend fun List<List<Value>>.toValuesMap(): Map<String, List<Value>> =
        withContext(Dispatchers.IO) {
            val valuesMap: Map<String, List<Value>>
            synchronized(cachedValuesMap) {
                for (dataList in this@toValuesMap) {
                    for (data in dataList) {
                        cachedValuesMap.getOrPut(data.type) { mutableListOf() }.add(data)
                    }
                }

                valuesMap = cachedValuesMap.toMap()
            }
            valuesMap
        }


    private fun renderView(valuesMap: Map<String, List<Value>>, moveToEnd: Boolean = false) {
        val container = chartContainer ?: return

        if (valuesMap.isNotEmpty()) {
            emptyView?.isVisible = false
        }

        container.post(RenderViewRunnable(layoutInflater, container, valuesMap, moveToEnd))
    }


    private class RenderViewRunnable(
        private val layoutInflater: LayoutInflater,
        private val container: ViewGroup,
        private val valuesMap: Map<String, List<Value>>,
        private val moveToEnd: Boolean
    ) : Runnable {

        override fun run() {
            val seconds = TimeCounter.seconds
            for ((type, values) in valuesMap) {
                val itemView = container.findViewWithTag(type)
                    ?: layoutInflater.inflate(R.layout.item_chart, container, false).also {
                        it.tag = type
                        container.addView(it)
                    }

                val titleView = itemView.findViewById<TextView>(R.id.title)
                val chartView = itemView.findViewById<BWLineChart>(R.id.chart)
                titleView.text = type
                chartView.setDataList(type, values.toList(), seconds)
                if (moveToEnd) {
                    chartView.moveToEnd()
                } else {
                    chartView.animateX(1000)
                }
            }
        }

    }

}