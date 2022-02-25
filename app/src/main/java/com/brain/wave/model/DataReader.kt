package com.brain.wave.model

import com.brain.wave.appContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

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
        job = GlobalScope.launch(Dispatchers.Default) {
            val channel = Channel<BleResponse>()

            launch {
                for (res in channel) {
                    block.invoke(res)
                }
            }

            for (line in lines) {
                delay(20L)
                line.parseBleResponse()?.let { channel.send(it) }
            }
        }.apply {
            invokeOnCompletion(onCompletion)
        }
    }

    fun cancel() {
        job?.cancel()
    }

}