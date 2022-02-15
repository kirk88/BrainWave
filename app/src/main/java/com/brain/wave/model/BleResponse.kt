package com.brain.wave.model

import android.util.Log
import com.brain.wave.TAG
import com.brain.wave.contracts.*

data class BleResponse(
    val header: Int,
    val type: Int,
    val length: Int,
    val values: List<Value>,
    val frame: Int,
    val code: String
)

data class Value(
    val value: Float,
    val type: String,
    val order: Int
)

fun String.parseBleResponse(): BleResponse? {
    val list = split(",")
    val size = list.size

    return try {
        val header = list[0].hexToInt()
        val type = list[1].hexToInt()
        val length = list.subList(2, 4).hexToInt()
        val frame = list[194].hexToInt()
        val code = list.subList(size - 2, size).toHexString()

        val values = mutableListOf<Value>()

        //时间
        values.add(Value(System.currentTimeMillis().toFloat(), TIME, 0))

        //温度
        values.add(Value(list.subList(4, 6).hexToInt() * 0.0078125f, TEMPERATURE, 1))

        var value = list.subList(6, 10).hexToInt(32)
        if (value != -999) {
            //血氧
            values.add(Value(value.toFloat(), SPO2, 2))
        }

        value = list.subList(10, 14).hexToInt(32)
        if (value != -999) {
            //心率
            values.add(Value(value.toFloat(), PPG_IR_SIGNAL, 3))
        }

        //通道
        for (index in 14..(list.size - 18) step 18) {
            val channelList = list.subList(index, index + 18)

            var count = 1
            for (i in 0..(channelList.size - 3) step 3) {
                value = channelList.subList(i, i + 3).hexToInt(32)
                values.add(Value(value.toFloat(), channelType(count), count + 3))
                count += 1
            }
        }

        BleResponse(header, type, length, values, frame, code)
    } catch (t: Throwable) {
        Log.e(TAG, t.message, t)
        return null
    }
}

private fun String.hexToInt(radix: Int = 16): Int {
    return trim().ifEmpty { "00" }.toInt(radix)
}

private fun List<String>.hexToInt(radix: Int = 16): Int {
    return toHexString().hexToInt(radix)
}

private fun List<String>.toHexString() = reversed().joinToString(separator = "")