package com.brain.wave.model

import com.brain.wave.appContext
import kotlinx.coroutines.*

object DataReader {

    private val lines = mutableListOf<String>()

    private var job: Job? = null

    init {
        appContext.assets.open("data.txt").bufferedReader().useLines {
            for (string in it) {
                lines.add(string)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun send(block: suspend (BleResponse) -> Unit, onCompletion: (Throwable?) -> Unit = {}) {
        job = GlobalScope.launch {
            for (line in lines) {
                line.parseBleResponse()?.let { block(it) }
            }
        }.apply {
            invokeOnCompletion(onCompletion)
        }
    }

    fun cancel() {
        job?.cancel()
    }

}