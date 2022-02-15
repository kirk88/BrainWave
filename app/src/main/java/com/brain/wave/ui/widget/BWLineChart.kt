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

class BWLineChart @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LineChart(context, attrs, defStyleAttr) {

    private val lineColor by lazy {
        ContextCompat.getColor(context, R.color.line)
    }
    private val darkLineColor by lazy {
        ContextCompat.getColor(context, R.color.line_dark)
    }

    private val times = mutableMapOf<Int, Int>()

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
            axisLineWidth = 1f
            axisLineColor = darkLineColor
            textColor = lineColor

            setDrawGridLines(false)
            position = XAxisPosition.BOTH_SIDED

            setAvoidFirstLastClipping(true)
        }

        axisLeft.apply {
            setDrawZeroLine(false)
            setDrawGridLines(false)
            axisLineWidth = 1f
            zeroLineWidth = 0.5f
            axisLineColor = darkLineColor
            zeroLineColor = darkLineColor
            textColor = lineColor

            labelCount = 3
        }

        axisRight.apply {
            setDrawGridLines(false)
            setDrawLabels(false)
            axisLineWidth = 1f
            axisLineColor = darkLineColor
        }

    }

    fun setDataList(type: String, values: List<Value>, seconds: Int) {
        val dataSet = LineDataSet(null, "main").apply {
            for ((index, y) in values.withIndex()) {
                times.getOrPut(index) { seconds }
                addEntry(Entry(index.toFloat(), y.value))
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

    override fun clear() {
        super.clear()
        times.clear()
    }

    private fun updateChartWithType(type: String) {
        when (type) {
            TEMPERATURE -> {
                xAxis.apply {
                    granularity = 20f
                    setLabelCount(3, true)
                    valueFormatter = XAxisValueFormatter(times)
                }
                axisLeft.valueFormatter = LeftAxisValueFormatter("℃")
                setVisibleXRange(1f, 20f)
            }
            SPO2 -> {
                xAxis.apply {
                    granularity = 20f
                    setLabelCount(3, true)
                    valueFormatter = XAxisValueFormatter(times)
                }
                axisLeft.valueFormatter = LeftAxisValueFormatter("%")
                setVisibleXRange(1f, 20f)
            }
            PPG_IR_SIGNAL -> {
                xAxis.apply {
                    granularity = 20f
                    setLabelCount(3, true)
                    valueFormatter = XAxisValueFormatter(times)
                }
                axisLeft.valueFormatter = LeftAxisValueFormatter("a.u.")
                setVisibleXRange(1f, 20f)
            }
            else -> {
                xAxis.apply {
                    granularity = 140f
                    setLabelCount(3, true)
                    valueFormatter = XAxisValueFormatter(times)
                }
                axisLeft.valueFormatter = LeftAxisValueFormatter("uV")
                setVisibleXRange(1f, 140f)
            }
        }
    }

    private class XAxisValueFormatter(private val times: Map<Int, Int>) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val index = value.toInt()
            if (index in 1..3) return ""
            return times.getOrElse(index) { 0 }.let {
                if (index != 0 && it == 0) "" else it.toString() + "s"
            }
        }
    }

    private class LeftAxisValueFormatter(private val unit: String) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            if (value == 0f) return "0$unit"
            return value.toBigDecimal().setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString() + unit
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