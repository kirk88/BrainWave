package com.brain.wave.model

import android.util.Log
import com.brain.wave.TAG
import com.brain.wave.contracts.*
import com.brain.wave.util.BitConverter
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
        else -> value.toFloat()
    }

    val doubleValue: Double = floatValue.toDouble()

}

fun ByteArray.parseBleResponse(): BleResponse? {
    return try {
        val header = toInt(0)
        val type = toInt(1)
        val length = toInt(2, 4, ByteOrder.BIG_ENDIAN)
        val frame = toInt(200)
        val code = toInt(size - 2, size)
        val values = mutableListOf<Value>()
        val timeMillis = System.currentTimeMillis()
        //时间
        val timeValue = Value(timeMillis, TIME, 0, timeMillis = timeMillis)
        repeat(10) { values.add(timeValue) }
        //温度
        var value = toInt(4, 6).toLong()
        values.add(Value(value, TEMPERATURE, 1, timeMillis = timeMillis))
        //血氧
        value = toInt(6, 10).toLong()
        values.add(Value(value, SPO2, 2, timeMillis = timeMillis))
        //原始血氧
        value = toInt(197, 200).toLong()
        values.add(Value(value, ORIGIN_SPO2, 3, timeMillis = timeMillis))
        //心率
        value = toInt(10, 14).toLong()
        values.add(Value(value, PPG_IR_SIGNAL, 4, timeMillis = timeMillis))
        //原始心率
        value = toInt(194, 197).toLong()
        values.add(Value(value, ORIGIN_PPG_IR_SIGNAL, 5, timeMillis = timeMillis))
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
                        count + 5,
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
    val str = if (!contains(",")) {
        replace("(.{2})".toRegex(), ",$1").substring(1)
    } else this
    val bytes = str.split(",").map { it.toInt(16).toByte() }.toByteArray()
    return bytes.parseBleResponse()
}

private fun ByteArray.toInt(index: Int): Int = get(index).toInt()

private fun ByteArray.toInt(
    fromIndex: Int,
    toIndex: Int,
    order: ByteOrder = ByteOrder.LITTLE_ENDIAN
): Int = copyOfRange(fromIndex, toIndex).toInt(order)

private fun ByteArray.toInt(order: ByteOrder): Int {
    return if (size == 3) {
        getInt24(order)
    } else {
        val buffer = ByteBuffer.wrap(this).order(order)
        when (size) {
            1 -> buffer.get().toInt()
            2 -> buffer.short.toInt()
            4 -> buffer.int
            else -> 0
        }
    }
}

fun ByteArray.getInt24(order: ByteOrder): Int {
    if (order == ByteOrder.LITTLE_ENDIAN) {
        reverse()
    }

    return BitConverter.interpret24bitAsInt32(this)
}

fun main() {
    val str =
        "ff,01,00,c1,fe,0c,19,fc,ff,ff,19,fc,ff,ff,f4,25,d2,b5,18,d2,65,13,36,00,a3,0b,11,12,d2,b8,0b,d2,c0,25,d2,83,18,d2,dc,18,36,c7,fe,0a,de,11,d2,87,0b,d2,e5,24,d2,a6,17,d2,08,25,36,88,1b,0a,00,11,d2,a9,0a,d2,14,24,d2,d8,16,d2,1f,33,36,dd,57,09,33,10,d2,db,09,d2,1a,24,d2,df,16,d2,03,34,36,f7,6a,09,3d,10,d2,e1,09,d2,86,24,d2,4d,17,d2,64,2c,36,ba,cf,09,b0,10,d2,50,0a,d2,ff,24,d2,c7,17,d2,6a,21,36,ff,2e,0a,21,11,d2,cb,0a,d2,b2,25,d2,79,18,d2,33,10,36,af,c6,0a,d4,11,d2,78,0b,d2,0e,26,d2,d2,18,d2,21,fd,35,ee,3b,0b,30,12,d2,d6,0b,d2,5e,26,d2,26,19,d2,de,f5,35,d6,0d,0b,87,12,d2,2b,0c,d2,01,00,00,00,aa,00,00,00,1a,20,98"

    println(str.parseBleResponse())
}