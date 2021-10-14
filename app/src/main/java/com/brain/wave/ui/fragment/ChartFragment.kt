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
import com.brain.wave.model.Data
import com.brain.wave.model.toHandledMap
import com.brain.wave.ui.widget.MyLineChart
import com.brain.wave.util.DataManager
import com.brain.wave.util.TimeCounter
import com.brain.wave.util.getDataList
import kotlinx.coroutines.*
import java.io.File

class ChartFragment : Fragment(R.layout.fragment_chart) {

    private var chartContainer: ViewGroup? = null
    private var progressBar: ContentLoadingProgressBar? = null
    private var emptyView: View? = null

    private val dataMapS = linkedMapOf<String, MutableList<Data>>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        chartContainer = view.findViewById(R.id.chart_container)
        progressBar = view.findViewById(R.id.progress)
        emptyView = view.findViewById(R.id.empty_view)

        val filePath = arguments?.getString("path")

        if (filePath.isNullOrBlank()) {
            emptyView?.isVisible = true
        } else {
            addFromFile(File(filePath))
        }
    }

    fun addFromFile(file: File) {
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
                val dataMap = handleDataListS(dataList)
                renderView(dataMap)
            }

            progressBar?.hide()
        }
    }

    fun addAllChartData(dataList: List<List<Data>>) {
        if (dataList.isEmpty()) return

        lifecycleScope.launch(Dispatchers.Main + CoroutineExceptionHandler { _, _ ->
            Log.e(TAG, "add all chart data failed.")
        }) {
            val dataMap = handleDataListS(dataList)
            renderView(dataMap, true)
        }
    }

    fun addChartData(dataList: List<Data>) {
        if (dataList.isEmpty()) return

        lifecycleScope.launch(Dispatchers.Main + CoroutineExceptionHandler { _, _ ->
            Log.e(TAG, "add chart data failed.")
        }) {
            val dataMap = handleDataListS(listOf(dataList))
            renderView(dataMap, true)
        }
    }

    fun clearChartData() {
        synchronized(dataMapS) {
            dataMapS.clear()
        }

        lifecycleScope.launch {
            chartContainer?.forEach {
                it.findViewById<MyLineChart>(R.id.chart)?.clear()
            }
        }
    }

    private suspend fun handleDataListS(dataListS: List<List<Data>>): Map<String, List<Data>> =
        withContext(Dispatchers.IO) {
            val dataMap: Map<String, List<Data>>
            synchronized(dataMapS) {
                for (dataList in dataListS) {
                    for (data in dataList) {
                        dataMapS.getOrPut(data.type) { mutableListOf() }.add(data)
                    }
                }

                dataMap = dataMapS.toHandledMap()
                DataManager.set(dataMap)
            }
            dataMap
        }


    private fun renderView(dataMap: Map<String, List<Data>>, moveToEnd: Boolean = false) {
        val container = chartContainer ?: return

        if (dataMap.isNotEmpty()) {
            emptyView?.isVisible = false
        }

        container.post(RenderViewRunnable(layoutInflater, container, dataMap, moveToEnd))
    }


    private class RenderViewRunnable(
        private val layoutInflater: LayoutInflater,
        private val container: ViewGroup,
        private val dataMap: Map<String, List<Data>>,
        private val moveToEnd: Boolean
    ) : Runnable {

        override fun run() {
            val seconds = TimeCounter.seconds
            for ((type, list) in dataMap) {
                val itemView = container.findViewWithTag(type)
                    ?: layoutInflater.inflate(R.layout.item_chart, container, false).also {
                        it.tag = type
                        container.addView(it)
                    }

                val titleView = itemView.findViewById<TextView>(R.id.title)
                val chartView = itemView.findViewById<MyLineChart>(R.id.chart)
                titleView.text = type
                chartView.setDataList(type, ArrayList(list), seconds)
                if (moveToEnd) {
                    chartView.moveToEnd()
                } else {
                    chartView.animateX(1000)
                }
            }
        }

    }

}