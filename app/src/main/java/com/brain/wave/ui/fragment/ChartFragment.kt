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
import com.brain.wave.contracts.*
import com.brain.wave.model.Value
import com.brain.wave.ui.widget.BWLineChart
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
        if (filePath != null) {
            addChartValuesFromFile(File(filePath))
        }
    }

    fun addChartValuesFromFile(file: File) {
        progressBar?.show()

        lifecycleScope.launch(Dispatchers.Main + CoroutineExceptionHandler { _, e ->
            Log.e(TAG, "add data from file failed.", e)
        }) {
            delay(400)

            val dataList = withContext(Dispatchers.IO) {
                getDataList(file)
            }

            if (dataList.isEmpty()) {
                emptyView?.isVisible = true
            } else {
                emptyView?.isVisible = false

                val dataMap = addToValuesMap(dataList)
                renderView(dataMap)
            }

            progressBar?.hide()
        }
    }

    fun addAllChartValues(valuesList: List<List<Value>>) {
        if (valuesList.isEmpty()) return

        val valuesMap = addToValuesMap(valuesList)
        renderView(valuesMap, true)
    }

    fun addChartValues(values: List<Value>) {
        if (values.isEmpty()) return

        val dataMap = addToValuesMap(listOf(values))
        renderView(dataMap, true)
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

    private fun addToValuesMap(list: List<List<Value>>): Map<String, List<Value>> {
        val valuesMap: Map<String, List<Value>>
        synchronized(cachedValuesMap) {
            for (dataList in list) {
                for (data in dataList) {
                    cachedValuesMap.getOrPut(data.type) { mutableListOf() }.add(data)
                }
            }

            valuesMap = cachedValuesMap.toMap()
        }
        return valuesMap
    }


    private fun renderView(valuesMap: Map<String, List<Value>>, moveToEnd: Boolean = false) {
        val container = chartContainer ?: return

        if (isAdded) {
            container.post(RenderViewRunnable(layoutInflater, container, valuesMap, moveToEnd))
        }
    }


    private class RenderViewRunnable(
        private val layoutInflater: LayoutInflater,
        private val container: ViewGroup,
        private val valuesMap: Map<String, List<Value>>,
        private val moveToEnd: Boolean
    ) : Runnable {

        override fun run() {
            for ((type, values) in valuesMap) {
                if (type == TIME || type == ORIGIN_SPO2 || type == PPG_IR_SIGNAL) continue

                val itemView = container.findViewWithTag(type)
                    ?: layoutInflater.inflate(R.layout.item_chart, container, false).also {
                        it.tag = type
                        container.addView(it)
                    }

                val titleView = itemView.findViewById<TextView>(R.id.title)
                val chartView = itemView.findViewById<BWLineChart>(R.id.chart)
                titleView.text = type
                chartView.setDataList(type, values.toList(), when (type) {
                    SPO2 -> valuesMap.getOrElse(PPG_IR_SIGNAL) { emptyList() }
                    else -> emptyList()
                })
                if (moveToEnd) {
                    chartView.moveToEnd()
                } else {
                    chartView.animateX(1000)
                }
            }
        }

    }

}