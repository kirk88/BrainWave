package com.brain.wave.util

import android.util.Log
import com.brain.wave.TAG
import com.brain.wave.appContext
import com.brain.wave.contracts.*
import com.brain.wave.model.Data
import com.brain.wave.model.toHandledMap
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
import kotlin.math.roundToLong


object DataManager {

    private val dirPath: String

    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH)
    private val timeFormat = SimpleDateFormat("sss:SSS", Locale.ENGLISH)

    private val dataList = mutableListOf<Data>()

    private val lock = Any()

    @Volatile
    private var isBegin = false
    private var beginTime = 0L
    private var endTime = 0L

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
        beginTime = System.currentTimeMillis()
    }

    fun append(list: List<Data>) {
        if (!isBegin) return

        synchronized(lock) {
            dataList.addAll(list)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun endAppend() {
        isBegin = false
        endTime = System.currentTimeMillis()

        GlobalScope.launch(Dispatchers.IO) {
            synchronized(lock) {
                if (dataList.isNotEmpty()) {
                    val dataMap = mutableMapOf<String, MutableList<Data>>()
                    for (data in dataList) {
                        dataMap.getOrPut(data.type) { mutableListOf() }.add(data)
                    }

                    writeExcel(dataMap.toHandledMap())
                }
            }
        }
    }

    private fun writeExcel(dataMap: Map<String, List<Data>>) {
        var book: WritableWorkbook? = null
        try {
            val file = File(dirPath, generateFileName())
            file.createNewFile()

            book = Workbook.createWorkbook(file)
            val sheet = book.createSheet(dateFormat.format(Date()), 0)
            sheet.addCell(Label(0, 0, "Time"))
            sheet.addCell(Label(1, 0, TEMPERATURE))
            sheet.addCell(Label(2, 0, SPO2))
            sheet.addCell(Label(3, 0, PPG_IR_SIGNAL))
            sheet.addCell(Label(4, 0, channelType(1)))
            sheet.addCell(Label(5, 0, channelType(2)))
            sheet.addCell(Label(6, 0, channelType(3)))
            sheet.addCell(Label(7, 0, channelType(4)))
            sheet.addCell(Label(8, 0, channelType(5)))
            sheet.addCell(Label(9, 0, channelType(6)))

            val step = 7

            var n = 1
            var list = dataMap[TEMPERATURE].orEmpty()
            for (data in list) {
                sheet.addCell(Number(1, n, data.value.toDouble()))
                n += step
            }

            n = 1
            list = dataMap[SPO2].orEmpty()
            for (data in list) {
                sheet.addCell(Number(2, n, data.value.toDouble()))
                n += step
            }

            n = 1
            list = dataMap[PPG_IR_SIGNAL].orEmpty()
            for (data in list) {
                sheet.addCell(Number(3, n, data.value.toDouble()))
                n += step
            }

            val interval = kotlin.runCatching {
                ((endTime - beginTime) / dataMap[channelType(1)].orEmpty().size.toFloat()).roundToLong()
            }.getOrDefault(5L)
            var timeMillis = 0L
            var column = 4
            for (count in 1..6) {
                list = dataMap[channelType(count)].orEmpty()
                for ((index, data) in list.withIndex()) {
                    if (count == 1) {
                        sheet.addCell(Label(0, 1 + index, timeFormat.format(timeMillis)))
                        timeMillis += interval
                    }
                    sheet.addCell(Number(column, 1 + index, data.value.toDouble()))
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