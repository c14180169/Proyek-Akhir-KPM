package com.example.proyekkpm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyekkpm.R

class DetailAdapter(private val listSms: ArrayList<String>) : RecyclerView.Adapter<DetailAdapter.ListViewHolder>() {

    var listener : RecyclerViewClickListener?=null

    interface RecyclerViewClickListener {
        fun navigate(view : View, dataSms : String)
    }

    inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var phoneList = itemView.findViewById<TextView>(R.id.phoneList)!!
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListViewHolder {
        val view : View = LayoutInflater.from(parent.context).inflate(
            R.layout.listdetail,
            parent,
            false
        )
        return ListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val detail = listSms[position]

        holder.phoneList.text = "Sender : $detail"

        holder.itemView.setOnClickListener {
            listener?.navigate(it, detail)
        }
    }

    override fun getItemCount(): Int {
       return listSms.size
    }
}