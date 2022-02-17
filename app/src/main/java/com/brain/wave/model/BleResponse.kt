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
    val value: Float, val type: String, val order: Int
)

fun ByteArray.parseBleResponse(): BleResponse? {
    return try {
        val header = toInt(0)
        val type = toInt(1)
        val length = toInt(2, 4, ByteOrder.BIG_ENDIAN)
        val frame = toInt(194)
        val code = toInt(size - 2, size)

        val values = mutableListOf<Value>()

        //时间
        values.add(Value(System.currentTimeMillis().toFloat(), TIME, 0))

        //温度
        values.add(Value(toInt(4, 6) * 0.0078125f, TEMPERATURE, 1))

        var value = toInt(6, 10)
        if (value != -999) { // 无效的数据
            //血氧
            values.add(Value(value.toFloat(), SPO2, 2))
        }

        value = toInt(10, 14)
        if (value != -999) { // 无效的数据
            //心率
            values.add(Value(value.toFloat(), PPG_IR_SIGNAL, 3))
        }

        //通道1~6的10次采样数据
        for (index in 14..(size - 18) step 18) {
            val channelBytes = copyOfRange(index, index + 18)

            var count = 1
            for (i in 0..(channelBytes.size - 3) step 3) {
                value = channelBytes.toInt(i, i + 3)
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

