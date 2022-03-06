package com.brain.wave.ui.fragment

import android.os.Bundle
import android.util.Log
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
import com.brain.wave.contracts.ORIGIN_SPO2
import com.brain.wave.contracts.PPG_IR_SIGNAL
import com.brain.wave.contracts.SPO2
import com.brain.wave.contracts.TIME
import com.brain.wave.model.Value
import com.brain.wave.ui.widget.BWLineChart
import com.brain.wave.util.getDataList
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class ChartFragment : Fragment(R.layout.fragment_chart) {

    private var chartContainer: ViewGroup? = null
    private var progressBar: ContentLoadingProgressBar? = null
    private var emptyView: View? = null

    private val lock = Mutex()

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

                addToView(dataList.toTypedMap())
            }

            progressBar?.hide()
        }
    }

    fun addAllChartValues(valuesList: List<List<Value>>) {
        if (valuesList.isEmpty()) return

        addToView(valuesList.toTypedMap(), true)
    }

    fun addChartValues(values: List<Value>) {
        if (values.isEmpty()) return

        addToView(listOf(values).toTypedMap(), true)
    }

    fun clearChartValues() {
        lifecycleScope.launch {
            chartContainer?.forEach {
                it.findViewById<BWLineChart>(R.id.chart)?.clear()
            }
        }
    }

    private fun List<List<Value>>.toTypedMap(): Map<String, List<Value>> {
        val dataMap = mutableMapOf<String, MutableList<Value>>()
        forEach { it.groupByTo(dataMap) { value -> value.type } }
        return dataMap
    }

    private fun addToView(valuesMap: Map<String, List<Value>>, moveToEnd: Boolean = false) {
        val container = chartContainer ?: return
        lifecycleScope.launch {
            addToViewLocked(container, valuesMap, moveToEnd)
        }
    }


    private suspend fun addToViewLocked(
        container: ViewGroup,
        valuesMap: Map<String, List<Value>>,
        moveToEnd: Boolean = false
    ) = lock.withLock {
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
            chartView.addDataList(
                type,
                values.toList(), when (type) {
                    SPO2 -> valuesMap[PPG_IR_SIGNAL]?.lastOrNull()
                    else -> null
                },
                moveToEnd
            )
        }
    }

}