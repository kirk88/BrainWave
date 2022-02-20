package com.brain.wave.model

import android.util.Log
import com.brain.wave.TAG
import com.brain.wave.contracts.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class BleResponse(
    val header: Int,
    val type: Int,
    val length: Int,
    val values: List<Value>,
    val frame: Int,
    val code: Int
)

data class Value(
    val value: Long,
    val type: String,
    val order: Int,
    val timeMillis: Long
) {

    val isValid: Boolean = if (type != SPO2 && type != PPG_IR_SIGNAL) {
        true
    } else value != -999L

    val floatValue: Float = if (type == TEMPERATURE) {
        value / 10000000F
    } else {
        value.toFloat()
    }

    val doubleValue: Double = floatValue.toDouble()

}

fun ByteArray.parseBleResponse(): BleResponse? {
    return try {
        val header = toInt(0)
        val type = toInt(1)
        val length = toInt(2, 4, ByteOrder.BIG_ENDIAN)
        val frame = toInt(194)
        val code = toInt(size - 2, size)

        val values = mutableListOf<Value>()

        val timeMillis = System.currentTimeMillis()

        //时间
        val timeValue = Value(timeMillis, TIME, 0, timeMillis = timeMillis)
        repeat(10) { values.add(timeValue) }

        //温度
        val temperature = toInt(4, 6) * 78125L
        values.add(Value(temperature, TEMPERATURE, 1, timeMillis = timeMillis))

        var value = toInt(6, 10)
        //血氧
        values.add(Value(value.toLong(), SPO2, 2, timeMillis = timeMillis))

        value = toInt(10, 14)
        //心率
        values.add(Value(value.toLong(), PPG_IR_SIGNAL, 3, timeMillis = timeMillis))

        //通道1~6的10次采样数据
        for (index in 14..(size - 18) step 18) {
            val channelBytes = copyOfRange(index, index + 18)

            var count = 1
            for (i in 0..(channelBytes.size - 3) step 3) {
                value = channelBytes.toInt(i, i + 3)
                values.add(
                    Value(
                        value.toLong(),
                        channelType(count),
                        count + 3,
                        timeMillis = timeMillis
                    )
                )
                count += 1
            }
        }

        BleResponse(header, type, length, values, frame, code)
    } catch (t: Throwable) {
        Log.e(TAG, t.message, t)
        return null
    }
}

fun String.parseBleResponse(): BleResponse? {
    val bytes = split(",").map { it.toInt(16).toByte() }.toByteArray()
    return bytes.parseBleResponse()
}

private fun ByteArray.toInt(index: Int): Int = get(index).toInt()

private fun ByteArray.toInt(
    fromIndex: Int,
    toIndex: Int,
    order: ByteOrder = ByteOrder.LITTLE_ENDIAN
): Int = copyOfRange(fromIndex, toIndex).let {
    if (it.size == 3) {
        val bytes = ByteArray(4)
        bytes[3] = 0.toByte()
        it.copyInto(bytes)
    } else {
        it
    }
}.let {
    val buffer = ByteBuffer.wrap(it).order(order)
    when (it.size) {
        1 -> buffer.get().toInt()
        2 -> buffer.short.toInt()
        4 -> buffer.int
        else -> 0
    }
}



