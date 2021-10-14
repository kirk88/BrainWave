package com.brain.wave.util

import com.brain.wave.model.Data

object IIRFilter {

    private val aArray = floatArrayOf(
        1.0000f,
        -3.5492f,
        16.8245f,
        -43.6363f,
        121.3232f,
        -250.6864f,
        520.0144f,
        -897.8167f,
        1517.5330f,
        -2255.1962f,
        3246.3581f,
        -4236.1098f,
        5329.9675f,
        -6193.3956f,
        6926.7951f,
        -7241.6800f,
        7283.1131f,
        -6903.6272f,
        6295.6191f,
        -5442.5667f,
        4528.0239f,
        -3586.2281f,
        2734.4921f,
        -1991.0084f,
        1396.1726f,
        -937.0116f,
        605.8266f,
        -375.5006f,
        224.2613f,
        -128.5498f,
        71.0073f,
        -37.6754f,
        19.2621f,
        -9.4643f,
        4.4800f,
        -2.0385f,
        0.8933f,
        -0.3763f,
        0.1526f,
        -0.0594f,
        0.0223f,
        -0.0080f,
        0.0028f,
        -0.0009f,
        0.0003f,
        -0.0001f,
        0.0000f,
        -0.0000f,
        0.0000f,
        -0.0000f,
        0.0000f,
        -0.0000f,
        0.0000f,
        -0.0000f,
        0.0000f,
        -0.0000f,
        0.0000f,
        -0.0000f,
        0.0000f,
        -0.0000f,
        0.0000f,
        -0.0000f,
        0.0000f,
        -0.0000f,
        0.0000f,
        -0.0000f,
        0.0000f,
        -0.0000f,
        0.0000f,
        -0.0000f,
        0.0000f,
        -0.0000f,
        0.0000f,
        -0.0000f,
        0.0000f,
        -0.0000f,
        0.0000f,
        -0.0000f,
        0.0000f
    )

    private val bArray = floatArrayOf(
        2.6926e-19f,
        -2.1002e-17f,
        8.0859e-16f,
        -2.0484e-14f,
        3.8408e-13f,
        -5.6844e-12f,
        6.916e-11f,
        -7.1136e-10f,
        6.3133e-09f,
        -4.9104e-08f,
        3.3882e-07f,
        -2.0945e-06f,
        1.1694e-05f,
        -5.9371e-05f,
        0.00027565f,
        -0.0011761f,
        0.0046309f,
        -0.016889f,
        0.057236f,
        -0.18075f,
        0.5332f,
        -1.4726f,
        3.8155f,
        -9.2899f,
        21.289f,
        -45.985f,
        93.738f,
        -180.53f,
        328.83f,
        -566.95f,
        926.01f,
        -1433.8f,
        2105.9f,
        -2935.5f,
        3885.3f,
        -4884.3f,
        5834.1f,
        -6622.5f,
        7145.3f,
        -7328.5f,
        7145.3f,
        -6622.5f,
        5834.1f,
        -4884.3f,
        3885.3f,
        -2935.5f,
        2105.9f,
        -1433.8f,
        926.01f,
        -566.95f,
        328.83f,
        -180.53f,
        93.738f,
        -45.985f,
        21.289f,
        -9.2899f,
        3.8155f,
        -1.4726f,
        0.5332f,
        -0.18075f,
        0.057236f,
        -0.016889f,
        0.0046309f,
        -0.0011761f,
        0.00027565f,
        -5.9371e-05f,
        1.1694e-05f,
        -2.0945e-06f,
        3.3882e-07f,
        -4.9104e-08f,
        6.3133e-09f,
        -7.1136e-10f,
        6.916e-11f,
        -5.6844e-12f,
        3.8408e-13f,
        -2.0484e-14f,
        8.0859e-16f,
        -2.1002e-17f,
        2.6926e-19f
    )

    fun filter(signal: List<Data>): List<Data> {
        val inArray = FloatArray(bArray.size)
        val outArray = FloatArray(aArray.size - 1)
        val newList = mutableListOf<Data>()
        for (i in signal.indices) {
            val data = signal[i]
            System.arraycopy(inArray, 0, inArray, 1, inArray.size - 1)
            inArray[0] = data.value

            //calculate y based on a and b coefficients
            //and in and out.
            var y = 0f
            for (j in bArray.indices) {
                y += bArray[j] * inArray[j]
            }
            for (j in 0 until aArray.size - 1) {
                y -= aArray[j + 1] * outArray[j]
            }

            //shift the out array
            System.arraycopy(outArray, 0, outArray, 1, outArray.size - 1)
            outArray[0] = y
            newList.add(data.copy(value = y))
        }
        return newList
    }

    fun transform(dataList: List<Data>, index: Int): Float {
        val hex =
            (dataList[index - 3].value.toInt() shl 16) or (dataList[index - 2].value.toInt() shl 8) or (dataList[index - 1].value.toInt())
        return if (hex and 0x800000 == 1) {
            (16777216 - hex) * -4500f / 8388607
        } else {
            hex * 4500f / 8388607
        }
    }

}