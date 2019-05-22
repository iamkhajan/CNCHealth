package com.example.mygraphapplication

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class ChartUtils {


    fun chartInception(lineChart: LineChart, upperLimit: Float, maxValue: Float, lowerLimit: Float = 0f) {
        setupChart(lineChart)
        setupAxes(lineChart, upperLimit, maxValue, lowerLimit)
        setupData(lineChart)
        setLegend(lineChart)

    }


    private fun setupChart(lineChart: LineChart) {
        // disable description text
        lineChart.description.isEnabled = false
        // enable touch gestures
        lineChart.setTouchEnabled(true)
        // if disabled, scaling can be done on x- and y-axis separately
        lineChart.setPinchZoom(true)
        // enable scaling
        lineChart.setScaleEnabled(true)
        lineChart.setDrawGridBackground(false)
        // set an alternative background color
        lineChart.setBackgroundColor(Color.WHITE)
    }

    private fun setupAxes(lineChart: LineChart, upperLimit: Float, maxValue: Float, lowerLimit: Float) {
        val xl = lineChart.xAxis
        xl.textColor = Color.BLACK
        xl.setDrawGridLines(false)
        xl.setAvoidFirstLastClipping(true)
        xl.isEnabled = true

        val leftAxis = lineChart.axisLeft
        leftAxis.textColor = Color.BLACK
        leftAxis.axisMaximum = maxValue
        leftAxis.axisMinimum = lowerLimit
        leftAxis.setDrawGridLines(true)

        val rightAxis = lineChart.axisRight
        rightAxis.isEnabled = false

        // Add a limit line
        val ll = LimitLine(upperLimit, "Upper Limit")
        ll.lineWidth = 2f
        ll.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
        ll.textSize = 10f
        ll.textColor = Color.BLACK
        // reset all limit lines to avoid overlapping lines
        leftAxis.removeAllLimitLines()
        leftAxis.addLimitLine(ll)
        // limit lines are drawn behind data (and not on top)
        leftAxis.setDrawLimitLinesBehindData(true)
    }

    private fun setupData(lineChart: LineChart) {
        val data = LineData()
        data.setValueTextColor(Color.BLACK)

        // add empty data
        lineChart.data = data
    }

    private fun setLegend(lineChart: LineChart) {
        // get the legend (only possible after setting data)
        val l = lineChart.legend

        // modify the legend ...
        l.form = Legend.LegendForm.CIRCLE
        l.textColor = Color.BLACK
    }

    fun createSet(): LineDataSet {
        val set = LineDataSet(null, "Data")
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.color = Color.BLUE
        set.setCircleColor(Color.BLUE)
        set.lineWidth = 2f
        set.circleRadius = 4f
        set.valueTextColor = Color.BLACK
        set.valueTextSize = 10f
        // To show values of each point
        set.setDrawValues(true)

        return set
    }
}