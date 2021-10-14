package com.brain.wave.util

fun getCRC(bytes: ByteArray): String {
    val polynomial = 0x0000a001
    var crc = 0x0000ffff
    var i = 0
    while (i < bytes.size) {
        crc = crc xor (bytes[i].toInt() and 0x000000ff)
        var j = 0
        while (j < 8) {
            crc = crc shr 1
            if (crc and 0x00000001 != 0) {
                crc = crc xor polynomial
            }
            j++
        }
        i++
    }
    return Integer.toHexString(crc)
}