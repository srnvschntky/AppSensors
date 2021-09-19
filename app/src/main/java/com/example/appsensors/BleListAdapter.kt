package com.example.appsensors

import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.ble_list_adapter.view.*


class BleListAdapter: RecyclerView.Adapter<BleListAdapter.ViewHolder>() {

    private val scanResultList:MutableSet<ScanResult> = mutableSetOf()


    fun receiveResult(results:MutableSet<ScanResult>) {
        this.scanResultList.clear()
        this.scanResultList.addAll(results)
        notifyDataSetChanged()

    }


    fun getDevicesList():MutableSet<ScanResult>{
        return scanResultList
    }


    inner class ViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){

        fun setReceivedData(result: ScanResult){

            if(result.device.name==null){
                itemView.device_name.text = "Name : N/A"
            }else itemView.device_name.text = result.device.name

            itemView.device_address.text =result.device.address
            itemView.rssi_value.text = result.rssi.toString()

            itemView.device_connect.setOnClickListener {

                Toast.makeText(it.context,"connect option  is not implemented",Toast.LENGTH_SHORT).show()

            }


        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ble_list_adapter,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = scanResultList.elementAt(position)
        holder.setReceivedData(result)

    }

    override fun getItemCount(): Int {
        return scanResultList.size
    }
}