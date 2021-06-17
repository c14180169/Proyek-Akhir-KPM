package com.example.proyekkpm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyekkpm.R
import com.example.proyekkpm.model._Sms
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ListSmsAdapter(private val listSms: ArrayList<_Sms>) : RecyclerView.Adapter<ListSmsAdapter.ListViewHolder>(){

    var listener : RecyclerViewClickListener?=null

    interface RecyclerViewClickListener {
        fun decrypt(view : View, dataSms : _Sms)
    }

    inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var msg = itemView.findViewById<TextView>(R.id.msgList)!!
        var time = itemView.findViewById<TextView>(R.id.timeList)!!
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListViewHolder {
        val view : View = LayoutInflater.from(parent.context).inflate(
            R.layout.listsms,
            parent,
            false
        )
        return ListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val sms = listSms[position]

        holder.msg.text = sms.msg

        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val instant = Instant.ofEpochMilli((sms.time).toLong())
        val date = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())

        holder.time.text = formatter.format(date)
        holder.itemView.setOnClickListener {
            listener?.decrypt(it, sms)
        }
    }

    override fun getItemCount(): Int {
        return listSms.size
    }
}