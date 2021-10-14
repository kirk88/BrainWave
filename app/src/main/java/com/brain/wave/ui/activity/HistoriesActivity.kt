package com.brain.wave.ui.activity

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.brain.wave.R
import com.brain.wave.ui.BaseActivity
import com.brain.wave.util.DataManager
import com.brain.wave.util.getShareIntent
import com.brain.wave.util.setupActionBar
import com.brain.wave.util.showSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import java.io.File
import java.util.*

class HistoriesActivity : BaseActivity(R.layout.activity_histories) {

    private val adapter: HistoriesAdapter by lazy {
        HistoriesAdapter(this)
    }

    private lateinit var emptyView: View

    private val calendar: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar {
            setTitle(R.string.historical_data)
        }

        emptyView = findViewById(R.id.empty_view)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                emptyView.isGone = adapter.itemCount == 0
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                emptyView.isGone = adapter.itemCount == 0
            }
        })

        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            val fileList = withContext(Dispatchers.IO) {
                DataManager.getFileList(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
            }
            adapter.setItems(fileList)

            emptyView.isVisible = fileList.isEmpty()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add("Date")?.setIcon(R.drawable.ic_baseline_date_range_24)?.apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

            setOnMenuItemClickListener {
                DatePickerDialog(
                    this@HistoriesActivity,
                    { _, year, month, dayOfMonth ->
                        calendar.set(year, month, dayOfMonth)
                        loadData()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    setButton(
                        DialogInterface.BUTTON_POSITIVE,
                        getString(R.string.ok),
                        this
                    )
                    setButton(
                        DialogInterface.BUTTON_NEGATIVE,
                        getString(R.string.cancel),
                        this
                    )
                }.show()
                true
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private class HistoriesAdapter(context: Context) :
        RecyclerView.Adapter<HistoriesAdapter.MyViewHolder>() {

        private val layoutInflater: LayoutInflater = LayoutInflater.from(context)

        private var itemClickListener: ((RecyclerView.ViewHolder) -> Unit)? = null

        private val items = mutableListOf<File>()

        fun getItem(position: Int): File = items[position]

        @SuppressLint("NotifyDataSetChanged")
        fun setItems(items: List<File>) {
            this.items.clear()
            this.items.addAll(items)
            notifyDataSetChanged()
        }

        @OptIn(DelicateCoroutinesApi::class)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(layoutInflater, parent).apply {
                itemView.setOnClickListener {
                    itemClickListener?.invoke(this)
                }

                setOnActionListener { holder, which ->
                    val file = items[holder.layoutPosition]
                    when (which) {
                        0 -> {
                            itemView.context.startActivity(getShareIntent(file))
                        }

                        1 -> {
                            MaterialAlertDialogBuilder(itemView.context)
                                .setTitle(R.string.dialog_tip)
                                .setMessage(R.string.tip_delete_historical_data)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.ok) { _, _ ->
                                    GlobalScope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            file.runCatching { delete() }.getOrDefault(false)
                                        }
                                        if (result) {
                                            items.remove(file)
                                            notifyItemRemoved(holder.layoutPosition)
                                        } else {
                                            itemView.showSnackbar(R.string.tip_delete_historical_data_failed)
                                        }
                                    }
                                }.show()
                        }
                    }
                }
            }
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val item = items[holder.layoutPosition]
            holder.setText(item.nameWithoutExtension)
        }

        override fun getItemCount(): Int = items.size

        class MyViewHolder(layoutInflater: LayoutInflater, parent: ViewGroup) :
            RecyclerView.ViewHolder(
                layoutInflater.inflate(R.layout.item_history, parent, false)
            ) {

            private var listener: (RecyclerView.ViewHolder, Int) -> Unit = { _, _ -> }

            private val textView = itemView.findViewById<TextView>(android.R.id.text1)

            init {
                itemView.findViewById<Button>(android.R.id.button1).setOnClickListener {
                    listener(this@MyViewHolder, 0)
                }

                itemView.findViewById<Button>(android.R.id.button2).setOnClickListener {
                    listener(this@MyViewHolder, 1)
                }
            }

            fun setOnActionListener(listener: (RecyclerView.ViewHolder, Int) -> Unit) {
                this.listener = listener
            }

            fun setText(text: CharSequence) {
                textView.text = text
            }

        }

    }

}