package com.brain.wave.util

object TimeCounter {

    private var start: Long = 0
    private var lastSeconds: Int = -1

    val seconds: Int
        get() {
            if (lastSeconds != -1) {
                return lastSeconds
            }
            return calculateSeconds()
        }

    private fun calculateSeconds(): Int {
        val time = System.currentTimeMillis() - start
        return (time / 1000).toInt()
    }

    fun start() {
        start = System.currentTimeMillis()
        lastSeconds = -1
    }

    fun stop() {
        if (lastSeconds == -1) {
            lastSeconds = calculateSeconds() - 1
        }
    }

}