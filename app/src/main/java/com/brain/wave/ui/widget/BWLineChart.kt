package com.brain.wave.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.brain.wave.R
import com.brain.wave.contracts.CHANNEL
import com.brain.wave.contracts.PPG_IR_SIGNAL
import com.brain.wave.contracts.SPO2
import com.brain.wave.contracts.TEMPERATURE
import com.brain.wave.model.Value
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.ChartHighlighter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.renderer.XAxisRenderer
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.ViewPortHandler
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

class BWLineChart @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LineChart(context, attrs, defStyleAttr) {

    private val lineColor by lazy {
        ContextCompat.getColor(context, R.color.line)
    }
    private val darkLineColor by lazy {
        ContextCompat.getColor(context, R.color.line_dark)
    }

    private val valuesCache = mutableListOf<Value>()

    init {
        mXAxisRenderer = MyXAxisRenderer(mViewPortHandler, mXAxis, mLeftAxisTransformer)

        legend.isEnabled = false
        description.isEnabled = false
        isScaleXEnabled = false
        isScaleYEnabled = false
        isDragYEnabled = false
        isDoubleTapToZoomEnabled = false
        isHighlightPerDragEnabled = false
        isHighlightPerTapEnabled = false
        isAutoScaleMinMaxEnabled = true

        setNoDataTextColor(darkLineColor)

        setHighlighter(object : ChartHighlighter<LineChart>(this) {
            override fun getHighlight(x: Float, y: Float): Highlight? {
                return null
            }
        })

        xAxis.apply {
            setDrawGridLines(false)
            setAvoidFirstLastClipping(true)

            enableGridDashedLine(4f, 2f, 2f)

            gridLineWidth = 0.5f
            axisLineWidth = 1f
            gridColor = darkLineColor
            axisLineColor = darkLineColor
            textColor = lineColor

            position = XAxisPosition.BOTH_SIDED
        }

        axisLeft.apply {
            setDrawZeroLine(false)
            setDrawGridLines(true)

            enableGridDashedLine(4f, 2f, 2f)

            gridLineWidth = 0.5f
            axisLineWidth = 1f
            zeroLineWidth = 0.5f
            gridColor = darkLineColor
            axisLineColor = darkLineColor
            zeroLineColor = darkLineColor
            textColor = lineColor
        }

        axisRight.apply {
            setDrawGridLines(false)
            setDrawLabels(false)
            axisLineWidth = 1f
            axisLineColor = darkLineColor
        }

    }

    fun setDataList(type: String, values: List<Value>) {
        this.valuesCache.clear()
        this.valuesCache.addAll(values)

        val dataSet = LineDataSet(null, "main").apply {
            for ((index, value) in values.withIndex()) {
                addEntry(Entry(index.toFloat(), if (value.isValid) value.floatValue else 0F, value))
            }
            mode = if (type.contains(CHANNEL)) {
                setDrawCircles(false)
                setDrawValues(false)
                LineDataSet.Mode.LINEAR
            } else {
                setDrawCircles(true)
                setDrawValues(true)
                LineDataSet.Mode.HORIZONTAL_BEZIER
            }
            setDrawCircleHole(false)
            setCircleColor(darkLineColor)
            lineWidth = 1f
            circleRadius = 2f
            valueTextColor = lineColor
            valueTextSize = 6f
            color = lineColor

            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toBigDecimal().setScale(2, RoundingMode.HALF_UP)
                        .stripTrailingZeros().toPlainString()
                }
            }
        }
        data = LineData(dataSet)

        updateChartWithType(type)
    }

    fun moveToEnd() {
        val dataSet = data?.getDataSetByLabel("main", true) ?: return
        moveViewToX(dataSet.entryCount.toFloat())
    }

    private fun updateChartWithType(type: String) {
        when (type) {
            TEMPERATURE -> {
                xAxis.apply {
                    setLabelCount(5, true)
                    valueFormatter = XAxisValueFormatter(valuesCache)
                }
                axisLeft.apply {
                    valueFormatter = LeftAxisValueFormatter("℃")
                    axisMinimum = 0F
                    axisMaximum = 50F
                }
                setVisibleXRange(1f, 20f)
            }
            SPO2 -> {
                xAxis.apply {
                    setLabelCount(5, true)
                    valueFormatter = XAxisValueFormatter(valuesCache)
                }
                axisLeft.apply {
                    valueFormatter = LeftAxisValueFormatter("%")
                    axisMinimum = 0F
                    axisMaximum = 100F
                }
                setVisibleXRange(1f, 20f)
            }
            PPG_IR_SIGNAL -> {
                xAxis.apply {
                    setLabelCount(5, true)
                    valueFormatter = XAxisValueFormatter(valuesCache)
                }
                axisLeft.apply {
                    valueFormatter = LeftAxisValueFormatter("a.u.")
                    axisMinimum = 0F
                    if(axisMaximum < 100F){
                        axisMaximum = 100F
                    }
                }
                setVisibleXRange(1f, 20f)
            }
            else -> {
                xAxis.apply {
                    setLabelCount(5, true)
                    valueFormatter = XAxisValueFormatter(valuesCache)
                }
                axisLeft.valueFormatter = LeftAxisValueFormatter("uV")
                setVisibleXRange(1f, 1000f)
            }
        }
    }

    private class XAxisValueFormatter(private val values: List<Value>) : ValueFormatter() {

        private val timeFormatter = SimpleDateFormat("mm:ss", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            val index = value.toInt()
            if (index in 1..3) return ""
            val firstValue = values.getOrNull(0) ?: return ""
            val targetValue = values.getOrNull(index) ?: return ""
            return timeFormatter.format(targetValue.timeMillis - firstValue.timeMillis)
        }
    }

    private class LeftAxisValueFormatter(
        private val unit: String,
        private val isDecimal: Boolean = false
    ) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            if (value == 0f) return "0$unit"
            if (!isDecimal) return "${value.toInt()}$unit"
            return "%.2f$unit".format(value)
        }
    }

    private class MyXAxisRenderer(
        viewPortHandler: ViewPortHandler?,
        xAxis: XAxis?,
        trans: Transformer?
    ) : XAxisRenderer(viewPortHandler, xAxis, trans) {

        override fun renderAxisLabels(c: Canvas?) {
            if (!mXAxis.isEnabled || !mXAxis.isDrawLabelsEnabled) return

            val yOffset = mXAxis.yOffset

            mAxisLabelPaint.typeface = mXAxis.typeface
            mAxisLabelPaint.textSize = mXAxis.textSize
            mAxisLabelPaint.color = mXAxis.textColor

            val pointF = MPPointF.getInstance(0f, 0f)
            when (mXAxis.position) {
                XAxisPosition.TOP -> {
                    pointF.x = 0.5f
                    pointF.y = 1.0f
                    drawLabels(c, mViewPortHandler.contentTop() - yOffset, pointF)
                }
                XAxisPosition.TOP_INSIDE -> {
                    pointF.x = 0.5f
                    pointF.y = 1.0f
                    drawLabels(
                        c,
                        mViewPortHandler.contentTop() + yOffset + mXAxis.mLabelRotatedHeight,
                        pointF
                    )
                }
                XAxisPosition.BOTTOM -> {
                    pointF.x = 0.5f
                    pointF.y = 0.0f
                    drawLabels(c, mViewPortHandler.contentBottom() + yOffset, pointF)
                }
                XAxisPosition.BOTTOM_INSIDE -> {
                    pointF.x = 0.5f
                    pointF.y = 0.0f
                    drawLabels(
                        c,
                        mViewPortHandler.contentBottom() - yOffset - mXAxis.mLabelRotatedHeight,
                        pointF
                    )
                }
                else -> { // BOTH SIDED
                    //                pointF.x = 0.5f
                    //                pointF.y = 1.0f
                    //                drawLabels(c, mViewPortHandler.contentTop() - yoffset, pointF)
                    pointF.x = 0.5f
                    pointF.y = 0.0f
                    drawLabels(c, mViewPortHandler.contentBottom() + yOffset, pointF)
                }
            }
            MPPointF.recycleInstance(pointF)
        }

    }

}