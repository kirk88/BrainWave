package com.brain.wave.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.brain.wave.R
import com.brain.wave.contracts.*
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
import java.util.concurrent.atomic.AtomicLong

class BWLineChart @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LineChart(context, attrs, defStyleAttr) {

    private val lineColor by lazy {
        ContextCompat.getColor(context, R.color.line)
    }
    private val darkLineColor by lazy {
        ContextCompat.getColor(context, R.color.line_dark)
    }

    private val lineDataFixed by lazy {
        LineData().also { data = it }
    }

    private val startTime = AtomicLong()

    private var chartType: String? = null

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

    override fun clear() {
        super.clear()
        clearExtraInfo()
        startTime.set(0L)
    }

    private fun setupLineDataByType(type: String) {
        lineDataFixed.clearValues()
        lineDataFixed.addDataSet(LineDataSet(null, "main").apply {
            mode = if (type.contains(CHANNEL) || type == ORIGIN_PPG_IR_SIGNAL) {
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

            valueFormatter = DataValueFormatter()
        })
    }

    private fun setupAxisByType(type: String) {
        when (type) {
            TEMPERATURE -> {
                xAxis.apply {
                    setLabelCount(5, true)
                    valueFormatter = XAxisValueFormatter(startTime, lineDataFixed)
                }
                axisLeft.apply {
                    valueFormatter = LeftAxisValueFormatter("℃")
                    axisMinimum = 0F
                    axisMaximum = 50F
                }
            }
            SPO2 -> {
                xAxis.apply {
                    setLabelCount(5, true)
                    valueFormatter = XAxisValueFormatter(startTime, lineDataFixed)
                }
                axisLeft.apply {
                    valueFormatter = LeftAxisValueFormatter("%")
                    axisMinimum = 0F
                    axisMaximum = 100F
                }
            }
            ORIGIN_PPG_IR_SIGNAL -> {
                xAxis.apply {
                    setLabelCount(5, true)
                    valueFormatter = XAxisValueFormatter(startTime, lineDataFixed)
                }
                axisLeft.apply {
                    valueFormatter = LeftAxisValueFormatter("")
                }
            }
            else -> {
                xAxis.apply {
                    setLabelCount(5, true)
                    valueFormatter = XAxisValueFormatter(startTime, lineDataFixed)
                }
                axisLeft.valueFormatter = LeftAxisValueFormatter("uV")
            }
        }
    }

    private fun setChartType(type: String) {
        if (chartType == type) return

        chartType = type

        setupAxisByType(type)
        setupLineDataByType(type)
    }

    private fun setVisibleCountByType(type: String) {
        when (type) {
            TEMPERATURE -> setVisibleXRange(1f, 20f)
            SPO2 -> setVisibleXRange(1f, 20f)
            ORIGIN_PPG_IR_SIGNAL -> setVisibleXRange(1f, 500f)
            else -> setVisibleXRange(1f, 1000f)
        }
    }

    private fun moveToEnd() {
        val dataSet = lineDataFixed.getDataSetByLabel("main", true) ?: return
        val lastEntryIndex = dataSet.entryCount - 1
        if (lastEntryIndex > -1) {
            val lastEntry = dataSet.getEntryForIndex(lastEntryIndex)
            moveViewToX(lastEntry.x)
        }
    }

    private fun invalidateIfNeed(moveToEnd: Boolean) {
        if(!hasWindowFocus()) return

        postInvalidateOnAnimation()

        if (moveToEnd) {
            moveToEnd()
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if(hasWindowFocus){
            invalidateIfNeed(false)
        }
    }

    fun addDataList(
        type: String,
        values: List<Value>,
        extraValue: Value? = null,
        moveToEnd: Boolean = true
    ) {
        if (values.isEmpty()) return

        if (extraValue != null) {
            setExtraInfo(extraValue, PPG_IR_SIGNAL, "a.u.")
        }

        setChartType(type)

        val dataSet = lineDataFixed.getDataSetByLabel("main", true) ?: return
        if (dataSet.entryCount == 0) {
            startTime.set(values.first().timeMillis)
        }
        for (value in values) {
            val lastEntryIndex = dataSet.entryCount - 1
            val lastEntry =
                if (lastEntryIndex > -1) dataSet.getEntryForIndex(lastEntryIndex) else null
            val x = if (lastEntry == null) 0F else lastEntry.x + 1
            dataSet.addEntry(
                Entry(x, if (value.isValid) value.floatValue else 0F, value)
            )
        }
        while (dataSet.entryCount > 10000) {
            dataSet.removeFirst()
        }
        notifyDataSetChanged()
        setVisibleCountByType(type)

        invalidateIfNeed(moveToEnd)
    }

    @SuppressLint("SetTextI18n")
    private fun setExtraInfo(value: Value, type: String, unit: String) {
        val parentViewGroup = parent as? ViewGroup ?: return

        val extraTextView = parentViewGroup.findViewById<TextView>(R.id.extra_info)
        extraTextView.text = "${
            value.value.let {
                if (it == -999L && (type == SPO2 || type == PPG_IR_SIGNAL)) 0 else it
            }
        }$unit"
    }

    private fun clearExtraInfo() {
        val parentViewGroup = parent as? ViewGroup ?: return

        val extraTextView = parentViewGroup.findViewById<TextView>(R.id.extra_info)
        extraTextView.text = null
    }

    private class DataValueFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return value.toBigDecimal().setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString()
        }
    }

    private class XAxisValueFormatter(
        private val startTime: AtomicLong,
        private val lineData: LineData
    ) : ValueFormatter() {

        private val timeFormatter = SimpleDateFormat("mm:ss", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            if (startTime.get() == 0L) return ""
            val dataSet = lineData.getDataSetByLabel("main", true) ?: return ""
            val targetEntry = dataSet.getEntryForXValue(value, 0F) ?: return ""
            val targetValue = targetEntry.data as? Value ?: return ""
            return timeFormatter.format(targetValue.timeMillis - startTime.get())
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