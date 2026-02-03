package com.project.safety.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.safety.R
import com.project.safety.models.SafetyTip

class SafetyTipsAdapter(private var tips: List<SafetyTip>) :
    RecyclerView.Adapter<SafetyTipsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_safety_tip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tip = tips[position]

        holder.tvTitle.text = tip.title
        holder.tvDescription.text = tip.description
        holder.tvCategory.text = tip.category
    }

    override fun getItemCount(): Int = tips.size

    fun filterTips(filteredTips: List<SafetyTip>) {
        tips = filteredTips
        notifyDataSetChanged()
    }
}
