package com.project.safety.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.safety.R
import com.project.safety.models.EmergencyContact

class EmergencyContactsAdapter(
    private var contacts: List<EmergencyContact>,
    private val onItemClick: (EmergencyContact, Int) -> Unit
) : RecyclerView.Adapter<EmergencyContactsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvNumber: TextView = itemView.findViewById(R.id.tvNumber)
        val tvRelation: TextView = itemView.findViewById(R.id.tvRelation)
        val ivPrimary: ImageView = itemView.findViewById(R.id.ivPrimary)
        val ivCall: ImageView = itemView.findViewById(R.id.ivCall)
        val ivMessage: ImageView = itemView.findViewById(R.id.ivMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]

        holder.tvName.text = contact.name
        holder.tvNumber.text = contact.number
        holder.tvRelation.text = contact.relation

        holder.ivPrimary.visibility = if (contact.isPrimary) View.VISIBLE else View.GONE

        holder.ivCall.setOnClickListener {
            onItemClick(contact, position)
        }

        holder.ivMessage.setOnClickListener {
            onItemClick(contact, position)
        }

        holder.itemView.setOnClickListener {
            onItemClick(contact, position)
        }
    }

    override fun getItemCount(): Int = contacts.size

    fun updateList(newList: List<EmergencyContact>) {
        // Note: This is a simplified version. In production, use ListAdapter with DiffUtil
        // contacts = newList
        // notifyDataSetChanged()
    }
}
