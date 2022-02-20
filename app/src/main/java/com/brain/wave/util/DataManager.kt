package com.brain.wave.util

import android.util.Log
import com.brain.wave.TAG
import com.brain.wave.appContext
import com.brain.wave.contracts.*
import com.brain.wave.model.Value
import jxl.Workbook
import jxl.write.Label
import jxl.write.Number
import jxl.write.WritableWorkbook
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileFilter
import java.text.SimpleDateFormat
import java.util.*


object DataManager {

    private val dirPath: String

    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH)
    private val timeFormat = SimpleDateFormat("sss:SSS", Locale.ENGLISH)

    private val dataList = mutableListOf<Value>()

    private val lock = Any()

    @Volatile
    private var isBegin = false

    init {
        val dir = File(appContext.externalCacheDir ?: appContext.cacheDir, SAVE_DIR_NAME)
        try {
            if (!dir.exists()) {
                dir.mkdirs()
            }
        } catch (t: Throwable) {
            Log.e(TAG, t.message, t)
        }
        dirPath = dir.absolutePath
    }

    fun beginAppend() {
        synchronized(lock) {
            dataList.clear()
        }
        isBegin = true
    }

    fun append(list: List<Value>) {
        if (!isBegin) return

        synchronized(lock) {
            dataList.addAll(list)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun endAppend() {
        isBegin = false

        GlobalScope.launch(Dispatchers.IO) {
            synchronized(lock) {
                if (dataList.isNotEmpty()) {
                    val dataMap = mutableMapOf<String, MutableList<Value>>()
                    for (data in dataList) {
                        dataMap.getOrPut(data.type) { mutableListOf() }.add(data)
                    }

                    writeExcel(dataMap)
                }
            }
        }
    }

    private fun writeExcel(valuesMap: Map<String, List<Value>>) {
        var book: WritableWorkbook? = null
        try {
            val file = File(dirPath, generateFileName())
            file.createNewFile()

            book = Workbook.createWorkbook(file)
            val sheet = book.createSheet(dateFormat.format(Date()), 0)
            sheet.addCell(Label(0, 0, TIME))
            sheet.addCell(Label(1, 0, TEMPERATURE))
            sheet.addCell(Label(2, 0, SPO2))
            sheet.addCell(Label(3, 0, PPG_IR_SIGNAL))
            sheet.addCell(Label(4, 0, channelType(1)))
            sheet.addCell(Label(5, 0, channelType(2)))
            sheet.addCell(Label(6, 0, channelType(3)))
            sheet.addCell(Label(7, 0, channelType(4)))
            sheet.addCell(Label(8, 0, channelType(5)))
            sheet.addCell(Label(9, 0, channelType(6)))

            var n = 1

            var list = valuesMap[TIME].orEmpty()
            if (list.isNotEmpty()) {
                val startTimeMillis = list.first().value
                for (value in list) {
                    val timeMillis = value.value - startTimeMillis
                    sheet.addCell(Label(0, n, timeFormat.format(timeMillis)))
                    n += 1
                }
            }

            val step = 10

            n = 1
            list = valuesMap[TEMPERATURE].orEmpty()
            for (value in list) {
                sheet.addCell(Number(1, n, value.doubleValue))
                n += step
            }

            n = 1
            list = valuesMap[SPO2].orEmpty()
            for (value in list) {
                sheet.addCell(Number(2, n, value.doubleValue))
                n += step
            }

            n = 1
            list = valuesMap[PPG_IR_SIGNAL].orEmpty()
            for (value in list) {
                sheet.addCell(Number(3, n, value.doubleValue))
                n += step
            }

            var column = 4
            for (count in 1..6) {
                list = valuesMap[channelType(count)].orEmpty()
                for ((index, value) in list.withIndex()) {
                    sheet.addCell(Number(column, 1 + index, value.doubleValue))
                }
                column += 1
            }

            book.write()
        } catch (t: Throwable) {
            Log.e(TAG, t.message, t)
        } finally {
            book?.close()
        }
    }

    fun getFileList(): List<File> {
        val dir = File(dirPath)
        return dir.listFiles()?.toList().orEmpty()
    }

    fun getFileList(year: Int, month: Int, dayOfMonth: Int): List<File> {
        val dir = File(dirPath)

        val date = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(
            Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }.time
        )

        return dir.listFiles(FileFilter {
            it.name.contains(date)
        })?.toList().orEmpty()
    }

    private fun generateFileName(): String {
        return dateFormat.format(Date()) + ".xlsx"
    }

}