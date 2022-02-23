package com.brain.wave.ui.activity

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.brain.wave.R
import com.brain.wave.model.BleResponse
import com.brain.wave.model.parseBleResponse
import com.brain.wave.ui.BaseActivity
import com.brain.wave.ui.fragment.ChartFragment
import com.brain.wave.util.DataManager
import com.brain.wave.util.setupActionBar
import com.brain.wave.util.showSnackbar
import com.brain.wave.util.toAppSetting
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nice.bluetooth.Bluetooth
import com.nice.bluetooth.Scanner
import com.nice.bluetooth.ScannerLevel
import com.nice.bluetooth.common.Advertisement
import com.nice.bluetooth.common.BluetoothState
import com.nice.bluetooth.common.ConnectionState
import com.nice.bluetooth.peripheral
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : BaseActivity(R.layout.activity_main), CoroutineScope by MainScope() {

    private lateinit var launcher: ActivityResultLauncher<Array<out String>>
    private var scanJob: Job? = null
    private var connectJob: Job? = null

    private lateinit var connectBtn: Button
    private lateinit var recordingBtn: Button

    private val devicesAdapter: DevicesAdapter by lazy {
        DevicesAdapter(this)
    }

    private val dialog: AlertDialog by lazy {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.bluetooth_devices)
            .setAdapter(devicesAdapter) { _, which ->
                val item = devicesAdapter.getItem(which)
                runCatching {
                    scanJob?.cancel()

                    connect(item)
                }.onFailure {
                    showSnackbar(R.string.tip_bluetooth_scan_failed)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                setOnDismissListener {
                    scanJob?.cancel()
                }
            }
    }

    private var chartFragment: ChartFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupActionBar {
            setDisplayShowHomeEnabled(false)
            setDisplayHomeAsUpEnabled(false)
        }

        supportFragmentManager.findFragmentById(R.id.fragment_container) as? ChartFragment
            ?: ChartFragment().also {
                chartFragment = it

                supportFragmentManager.beginTransaction().apply {
                    replace(R.id.fragment_container, it)
                    commit()
                }
            }


        launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it.all { result -> result.value }) {
                if (Bluetooth.isEnabled) {
                    startScan()
                } else {
                    Bluetooth.isEnabled = true

                    Bluetooth.state.onEach { state ->
                        if (state == BluetoothState.Opened) {
                            startScan()
                        }
                    }.launchIn(lifecycleScope)
                }
            } else {
                showPermissionDeniedDialog()
            }
        }

        connectBtn = findViewById(R.id.btn_connect)
        recordingBtn = findViewById(R.id.btn_recording)

        connectBtn.setOnClickListener {
            val flag = it.tag as? Boolean ?: false
            val advertisement = it.getTag(R.id.tag_id) as? Advertisement
            if (flag) {
                connectJob?.cancel()
            } else {
                if (advertisement == null) {
                    showSnackbar(R.string.tip_no_bluetooth_device_available)
                } else {
                    connect(advertisement)
                }
            }
        }

        recordingBtn.setOnClickListener {
            val flag = it.tag as? Boolean ?: false
            if (flag) {
                it.tag = false
                DataManager.endAppend()
                recordingBtn.setText(R.string.start_recording)
            } else {
                it.tag = true
                DataManager.beginAppend()
                recordingBtn.setText(R.string.stop_recording)
            }
        }
//        DataManager.beginAppend()
//        DataReader.send(
//            {
//                delay(100L)
//
//                DataManager.append(it.values)
//
//                chartFragment?.addChartValues(it.values)
//            },
//            {
//                DataManager.endAppend()
//            }
//        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_history -> {
                startActivity(Intent(this@MainActivity, HistoriesActivity::class.java))
            }
            R.id.action_scan -> {
                checkOrStartScan()
            }
            R.id.action_debug -> {
                startActivity(Intent(this, DebugActivity::class.java))
            }
            R.id.action_high -> scannerLevel = ScannerLevel.High
            R.id.action_low -> scannerLevel = ScannerLevel.Low
            R.id.action_system -> scannerLevel = ScannerLevel.System
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        when (scannerLevel) {
            ScannerLevel.High -> menu.findItem(R.id.action_high).isChecked = true
            ScannerLevel.Low -> menu.findItem(R.id.action_low).isChecked = true
            ScannerLevel.System -> menu.findItem(R.id.action_system).isChecked = true
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    private fun checkOrStartScan() {
        launcher.launch(Bluetooth.permissions)
    }

    @OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private fun startScan() {
        if (System.currentTimeMillis() - lastScanTime < 5 * 1000) {
            if (devicesAdapter.isEmpty) {
                showSnackbar(R.string.tip_bluetooth_scan_frequently)
            } else {
                dialog.show()
            }
            return
        }

        dialog.show()

        scanJob =
            lifecycleScope.launch(Dispatchers.Main + CoroutineExceptionHandler { _, _ ->
                showSnackbar(R.string.tip_bluetooth_scan_failed)
            }) {
                val channel = Channel<Advertisement>(Channel.UNLIMITED)
                launch {
                    Scanner(scannerLevel).advertisements.collect {
                        channel.send(it)
                    }
                }

                for (advertisement in channel) {
                    devicesAdapter.addItem(advertisement)
                }
            }

        lastScanTime = System.currentTimeMillis()
    }

    @OptIn(InternalCoroutinesApi::class)
    private fun connect(advertisement: Advertisement) {
        connectBtn.setTag(R.id.tag_id, advertisement)
        connectJob =
            lifecycleScope.launch(Dispatchers.Main + CoroutineExceptionHandler { _, _ ->
                showSnackbar(R.string.tip_bluetooth_connect_failed)
            }) {
                val peripheral = peripheral(advertisement) {
                    onConnected {
                        requestMtu(247)
                    }
                }

                launch(Dispatchers.Main) {
                    val dialog = createProgressDialog(R.string.tip_connecting_bluetooth).apply {
                        setOnCancelListener {
                            connectJob?.cancel(CancellationException("cancel"))
                        }
                    }

                    peripheral.state.drop(1).collect {
                        if (it is ConnectionState.Disconnected) {
                            showSnackbar(R.string.tip_bluetooth_disconnected)
                            changeConnectState(R.string.connect_bluetooth, false)
                        } else if (it is ConnectionState.Connected) {
                            showSnackbar(R.string.tip_bluetooth_connected)
                            changeConnectState(R.string.disconnect_bluetooth, true)
                        }

                        if (it is ConnectionState.Connecting) {
                            dialog.show()
                        } else {
                            dialog.dismiss()
                        }
                    }

                }

                peripheral.connect()

                val service = peripheral.services.find { service ->
                    service.serviceUuid.toString().contains("6E400001", true)
                }

                val characteristic = service?.characteristics?.find { characteristic ->
                    characteristic.characteristicUuid.toString().contains("6E400003", true)
                }

                if (characteristic != null) {
                    chartFragment?.clearChartValues()

                    val channel = Channel<BleResponse>(Channel.UNLIMITED)
                    launch(Dispatchers.IO) {
                        for (res in channel) {
                            val values = res.values
                            DataManager.append(values)
                            chartFragment?.addChartValues(values)
                        }
                    }

                    peripheral.observe(characteristic).collect {
                        it.parseBleResponse()?.let { res -> channel.send(res) }
                    }
                } else {
                    peripheral.disconnect()
                }
            }.apply {
                invokeOnCompletion {
                    lifecycleScope.launch(Dispatchers.Main.immediate) {
                        if (it is CancellationException && it.message == "cancel") {
                            showSnackbar(R.string.tip_bluetooth_connection_canceled)
                        }
                        changeConnectState(R.string.connect_bluetooth, false)
                    }
                }
            }
    }

    private fun changeConnectState(@StringRes resId: Int, connected: Boolean) {
        connectBtn.setText(resId)
        connectBtn.tag = connected
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_tip)
            .setMessage(R.string.permission_required)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                toAppSetting()
            }.show()
    }

    private fun createProgressDialog(@StringRes resId: Int): Dialog =
        ProgressDialog(this@MainActivity).apply {
            setCanceledOnTouchOutside(false)
            setMessage(getText(resId))
        }

    private class DevicesAdapter(val context: Context) : BaseAdapter() {

        private val list = mutableSetOf<Advertisement>()

        fun addItem(advertisement: Advertisement) {
            if (list.add(advertisement)) {
                notifyDataSetChanged()
            }
        }

        override fun getCount(): Int {
            return list.size
        }

        override fun getItem(position: Int): Advertisement {
            return list.elementAt(position)
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

        var lastScanTime: Long = 0
        var scannerLevel = ScannerLevel.System

    }

}