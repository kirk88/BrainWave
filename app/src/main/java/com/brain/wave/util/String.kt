package com.brain.wave.util

fun ByteArray.decodeToUnsignedHexString(
    prefix: String = "",
    postfix: String = "",
    separator: String = ""
): String {
    return joinToString(prefix = prefix, postfix = postfix, separator = separator) {
        "%02x".format(it)
    }
}

fun ByteArray.decodeToUnsignedBinaryString(
    prefix: String = "",
    postfix: String = "",
    separator: String = ""
): String {
    return joinToString(prefix = prefix, postfix = postfix, separator = separator) {
        it.toUnsignedBinaryString()
    }
}

fun Byte.toUnsignedBinaryString(): String {
    return toUInt().toBinaryString()
}

fun Int.toUnsignedBinaryString(): String {
    return toUInt().toBinaryString()
}

fun UInt.toBinaryString(): String {
    //1.补零
    var binaryString = toString(2)
    var bits = 8
    if (bits < binaryString.length) {
        bits += bits //不断翻倍8 16 32 64...
    }
    while (binaryString.length < bits) {
        binaryString = "0$binaryString"
    }
    return binaryString
}