package com.brain.wave.model

import android.util.Log
import com.brain.wave.TAG
import com.brain.wave.contracts.*
import com.brain.wave.util.decodeToHexString
import java.math.BigInteger
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

    val isValid: Boolean = if (type == SPO2 || type == PPG_IR_SIGNAL) {
        value > 0L
    } else true

    val floatValue: Float = when (type) {
        TEMPERATURE -> value * 0.0078125F
        SPO2 -> value * 0.01F
        PPG_IR_SIGNAL -> value * 0.1F
        else -> value.toFloat()
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
        var value = toInt(4, 6).toLong()
        values.add(Value(value, TEMPERATURE, 1, timeMillis = timeMillis))

        value = toInt(6, 10).toLong()
        //血氧
        values.add(Value(value, SPO2, 2, timeMillis = timeMillis))

        value = toInt(10, 14).toLong()
        //心率
        values.add(Value(value, PPG_IR_SIGNAL, 3, timeMillis = timeMillis))

        //通道1~6的10次采样数据
        for (index in 14..(size - 18) step 18) {
            val channelBytes = copyOfRange(index, index + 18)

            var count = 1
            for (i in 0..(channelBytes.size - 3) step 3) {
                value = channelBytes.toInt(i, i + 3).toLong()
                values.add(
                    Value(
                        value,
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
        return@let it.completeToInt(order)
    }

    val buffer = ByteBuffer.wrap(it).order(order)
    return@let when (it.size) {
        1 -> buffer.get().toInt()
        2 -> buffer.short.toInt()
        4 -> buffer.int
        else -> 0
    }
}

private fun ByteArray.completeToInt(order: ByteOrder = ByteOrder.LITTLE_ENDIAN): Int {
    Log.e("TAGTAG", "old: ${decodeToHexString("")}")
    val newBytes = ByteArray(3)
    ByteBuffer.wrap(this).order(order).get(newBytes)
    Log.e("TAGTAG", "new: ${newBytes.decodeToHexString("")}")
    var binaryString = BigInteger(1, newBytes).toString(2)
    while (binaryString.length < 16) {
        binaryString = "0$binaryString"
    }
    Log.e("TAGTAG", binaryString)
    val binary = binaryString.substring(0, 1) //取第一位判断正负
    val result: Int = if ("0" == binary) {
        binaryString.toInt(2)
    } else {
        val bits = binaryString.split("")
        val builder = StringBuilder()
        for (bit in bits) {
            if ("0" == bit) {
                builder.append("1")
            } else {
                builder.append("0")
            }
        }
        // 二进制转为十进制.
        -builder.toString().toInt(2) - 1
    }
    return result
}

fun main() {
    val str = (-10000).toUInt().toString(2)

    println(str)
}