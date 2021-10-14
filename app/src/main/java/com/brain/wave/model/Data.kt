package com.brain.wave.model

import android.util.Log
import com.brain.wave.TAG
import com.brain.wave.contracts.PPG_IR_SIGNAL
import com.brain.wave.contracts.SPO2
import com.brain.wave.contracts.TEMPERATURE
import com.brain.wave.contracts.channelType
import com.brain.wave.util.Algorithm
import com.brain.wave.util.DataManager
import com.brain.wave.util.IIRFilter
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class Data(
    val value: Float,
    val type: String,
    val order: Int
) {

    @Transient
    var seconds: Int = 0

}

data class RawData(
    val header: Int,
    val type: Int,
    val length: Int,
    val data: List<Data>,
    val code: String
)

fun String.parseRawData(): RawData? {
    val list = split(",")
    val size = list.size

    return try {
        val header = list[0].hexToInt()
        val type = list[1].hexToInt()
        val length = "${list[3]}${list[2]}".hexToInt()
        val code = "${list[size - 2]}${list[size - 1]}"

        var dataList = list.subList(4, size - 2)

        val data = mutableListOf<Data>()
        data.add(Data(dataList.subList(0, 2).hexToInt() * 0.0078125f, TEMPERATURE, 1))
        data.add(Data(dataList.subList(2, 6).hexToInt().toFloat(), SPO2, 2))
        data.add(Data(dataList.subList(6, 10).hexToInt().toFloat(), PPG_IR_SIGNAL, 3))

        dataList = dataList.subList(10, dataList.size)
        var count = 1
        var offset = 0
        while (offset + 3 <= dataList.size) {
            data.add(
                Data(
                    dataList.subList(offset, offset + 3).hexToInt().toFloat(),
                    channelType(count),
                    count + 3
                )
            )
            if (count >= 6) {
                count = 1
            } else {
                count += 1
            }
            offset += 3
        }

        RawData(header, type, length, data, code)
    } catch (t: Throwable) {
        Log.e(TAG, t.message, t)
        return null
    }
}

private fun String.hexToInt(): Int {
    if (length == 2) return trim().toInt(16)
    return trim().ifEmpty { "00" }.toInt(16)
}

private fun List<String>.hexToInt(): Int {
    return reversed().joinToString("").hexToInt()
}

fun Map<String, List<Data>>.toHandledMap(): Map<String, List<Data>> {
    val dataMap = mutableMapOf<String, List<Data>>()

    dataMap[TEMPERATURE] = this[TEMPERATURE].orEmpty()

    var spo2List = this[SPO2].orEmpty()
    val irList = this[PPG_IR_SIGNAL].orEmpty()

    val result = Algorithm.getResult(irList, spo2List)
    if (result != null) {
        spo2List = irList.mapIndexed { index, data ->
            if (index == spo2List.lastIndex) {
                result.second
            } else data
        }
    }

    val ratio = calculateRatio(spo2List)
    dataMap[SPO2] = spo2List.map {
        it.copy(value = it.value.scaleValue(ratio))
    }

    dataMap[PPG_IR_SIGNAL] = irList

    for (count in 1..6) {
        val type = channelType(count)
        val list = this[type].orEmpty()
        dataMap[type] = list.mapIndexed { index, data ->
            if (index > 2) {
                data.copy(value = IIRFilter.transform(list, index))
            } else {
                data
            }
        }
    }

    return dataMap
}

private fun calculateRatio(dataList: List<Data>): BigDecimal {
    val max = dataList.maxOf { it.value }
    return 100f.toBigDecimal().divide(max.toBigDecimal(), MathContext.DECIMAL64)
}

private fun Float.scaleValue(ratio: BigDecimal): Float {
    return toBigDecimal().multiply(ratio, MathContext.DECIMAL64)
        .setScale(2, RoundingMode.HALF_UP)
        .toFloat()
}