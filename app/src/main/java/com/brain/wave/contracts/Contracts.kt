package com.brain.wave.contracts

const val SAVE_DIR_NAME = "BrainWave"

const val TIME = "Time"
const val TEMPERATURE = "Temperature" //温度
const val SPO2 = "SpO2"
const val PPG_IR_SIGNAL = "PPG IR signal"
const val CHANNEL = "EEG Channel"
fun channelType(n: Int) = "$CHANNEL $n"
