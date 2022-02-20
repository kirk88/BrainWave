package com.brain.wave.util

fun ByteArray.decodeToHexString(separator: String = ","): String {
    return joinToString(separator = separator) {
        "%02x".format(it.toUByte().toInt())
    }
}