package com.brain.wave.model

import com.brain.wave.appContext
import kotlinx.coroutines.*
import java.util.concurrent.Executors

object DataReader {

    private val lines= mutableListOf<String>()

    private var job: Job? = null

    init {
        appContext.assets.open("data.txt").bufferedReader().useLines {
            for (string in it) {
                lines.add(string)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun send(block: (RawData) -> Unit) {
        job = GlobalScope.launch {
            for (line in lines){
                delay(33)

                line.parseRawData()?.let { block(it) }
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

}