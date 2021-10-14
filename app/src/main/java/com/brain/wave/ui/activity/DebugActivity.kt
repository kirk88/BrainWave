package com.brain.wave.ui.activity

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.brain.wave.R
import com.brain.wave.util.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nice.bluetooth.Bluetooth
import com.nice.bluetooth.Scanner
import com.nice.bluetooth.common.Advertisement
import com.nice.bluetooth.common.BluetoothState
import com.nice.bluetooth.common.Peripheral
import com.nice.bluetooth.peripheral
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class DebugActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var launcher: ActivityResultLauncher<Array<out String>>

    private var scanJob: Job? = null
    private var connectJob: Job? = null

    private var startTime: Long = 0

    private val timeFormat = SimpleDateFormat("sss:SSS", Locale.ENGLISH)

    private val adapter: DevicesAdapter by lazy {
        DevicesAdapter(this)
    }

    private val dialog: AlertDialog by lazy {
        MaterialAlertDialogBuilder(this)
            .setTitle("蓝牙设备")
            .setAdapter(adapter) { _, which ->
                val item = adapter.getItem(which)
                runCatching {
                    scanJob?.cancel()
                    connectJob?.cancel()

                    connect(item)
                }.onFailure {
                    textView.append("\n")
                    textView.append("扫描出现错误：$it")
                }
            }
            .setNegativeButton("取消", null)
            .create().apply {
                setOnDismissListener {
                    scanJob?.cancel()
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)
        setupActionBar {
            title = "Debug"
        }

        textView = findViewById(R.id.textview)

        launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it.all { result -> result.value }) {
                if (Bluetooth.isEnabled) {
                    startScan()
                } else {
                    Bluetooth.isEnabled = true

                    Bluetooth.state.onEach { state ->
                        Log.e(TAG, "state: $it")
                        if (state == BluetoothState.Opened) {
                            startScan()
                        }
                    }.launchIn(lifecycleScope)
                }
            } else {
                Toast.makeText(this@DebugActivity, "请打开蓝牙相关权限", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add("开始扫描").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.title == "开始扫描") {
            checkOrStartScan()
            return true
        } else if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkOrStartScan() {
        if (!isLocationEnabled) {
            toast("请开启定位服务")
            toLocationSetting()
            return
        }

        scanJob?.cancel()
        connectJob?.cancel()

        lifecycleScope.launch {
            delay(500)

            launcher.launch(Bluetooth.permissions)
        }
    }

    @OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private fun startScan() {
        if (System.currentTimeMillis() - MainActivity.lastScanTime < 5 * 1000) {
            toast("请勿频繁扫描")
            return
        }

        startTime = System.currentTimeMillis()

        adapter.clear()
        dialog.show()
        textView.text = "正在扫描蓝牙..."

        val channel = Channel<Advertisement>(Channel.UNLIMITED)


        val collectJob =
            lifecycleScope.launch(Dispatchers.Main.immediate + CoroutineExceptionHandler { _, throwable ->
                textView.append("\n")
                textView.append(formatText("扫描出现错误：$throwable"))
            }) {
                channel.consumeAsFlow().scan(mutableSetOf<Advertisement>()) { accumulator, value ->
                    if (accumulator.add(value)) {
                        withContext(Dispatchers.Main) {
                            adapter.addItem(value)
                        }
                    }
                    accumulator
                }.collect()
            }

        scanJob =
            lifecycleScope.launch(Dispatchers.Main.immediate + CoroutineExceptionHandler { _, throwable ->
                textView.append("\n")
                textView.append(formatText("扫描出现错误：$throwable"))
            }) {
                Scanner(MainActivity.scannerLevel).advertisements.collect {
                    channel.send(it)
                }
            }.apply {
                invokeOnCompletion {
                    textView.append("\n")
                    textView.append(formatText("已停止扫描"))

                    if (it is CancellationException) {
                        collectJob.cancel()
                    }
                }
            }

        MainActivity.lastScanTime = System.currentTimeMillis()
    }

    @OptIn(InternalCoroutinesApi::class)
    private fun connect(advertisement: Advertisement) {
        connectJob =
            lifecycleScope.launch(Dispatchers.Main.immediate + CoroutineExceptionHandler { _, throwable ->
                textView.append("\n")
                textView.append(formatText("连接出现错误：$throwable"))
            }) {
                val peripheral = peripheral(advertisement) {
                    onConnected {
                        withContext(Dispatchers.Main) {
                            textView.append("\n")
                            textView.append(formatText("正在设置MTU"))
                        }

                        val mtu = requestMtu(200)
                        withContext(Dispatchers.Main) {
                            textView.append("\n")
                            textView.append(formatText("MTU设置成功：$mtu"))
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    textView.append("\n")
                    textView.append(formatText("正在连接：${advertisement.address}"))
                }

                launch {
                    peripheral.state.drop(1).collect {
                        withContext(Dispatchers.Main) {
                            textView.append("\n")
                            textView.append(formatText("连接状态：$it"))
                        }
                    }
                }

                connect(peripheral)
            }.apply {
                invokeOnCompletion {
                    if (it == null) {
                        textView.append("\n")
                        textView.append(formatText("已停止连接"))
                    }
                    DataManager.endAppend()
                }
            }
    }

    private suspend fun connect(peripheral: Peripheral) {
        peripheral.connect()

        withContext(Dispatchers.Main) {
            textView.append("\n")
            textView.append(formatText("连接成功"))
        }

        val service = peripheral.services.find { service ->
            service.serviceUuid.toString().contains("ffe0", true)
        }

        if (service != null) {
            withContext(Dispatchers.Main) {
                textView.append("\n")
                textView.append(formatText("发现服务：${service.serviceUuid}"))
            }
        }

        val characteristic = service?.characteristics?.find { characteristic ->
            characteristic.characteristicUuid.toString().contains("ffe1", true)
        }

        if (characteristic != null) {
            withContext(Dispatchers.Main) {
                textView.append("\n")
                textView.append(formatText("发现特征码：${characteristic.characteristicUuid}"))
            }
        }

        if (characteristic != null) {
            withContext(Dispatchers.Main) {
                textView.append("\n")
                textView.append(formatText("正在读取数据..."))
            }

            DataManager.beginAppend()
            peripheral.observe(characteristic).collect {
                withContext(Dispatchers.Main) {
                    textView.append("\n")
                    textView.append(formatText(it.decodeToHexString()))
                }
            }

        } else {
            withContext(Dispatchers.Main) {
                textView.append("\n")
                textView.append(formatText("未发现服务"))
            }
        }
    }

    private fun formatText(text: String): String {
        val time = System.currentTimeMillis() - startTime
        return "[${timeFormat.format(time)}] $text"
    }

    private class DevicesAdapter(val context: Context) : BaseAdapter() {

        private val list = mutableListOf<Advertisement>()

        fun clear() {
            list.clear()
            notifyDataSetChanged()
        }

        fun addItem(advertisement: Advertisement) {
            list.add(advertisement)
            notifyDataSetChanged()
        }

        override fun getCount(): Int {
            return list.size
        }

        override fun getItem(position: Int): Advertisement {
            return list[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView
                ?: LayoutInflater.from(context)
                    .inflate(android.R.layout.simple_list_item_2, parent, false)

            val textView1 = view.findViewById<TextView>(android.R.id.text1)
            val textView2 = view.findViewById<TextView>(android.R.id.text2)

            val item = getItem(position)

            textView1.text = item.name
            textView2.text = item.address
            return view
        }
    }

    companion object {
        private const val TAG = "Bluetooth"
    }

}