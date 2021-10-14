package com.brain.wave.util

fun ByteArray.decodeToHexString(): String {
    val str = mutableListOf<String>()
    for (b in this) {
        str.add(String.format("%02x", b.toInt() and 0xff))
    }
    return str.joinToString(",")
}