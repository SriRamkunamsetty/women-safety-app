package com.project.safety

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.safety.adapters.EmergencyContactsAdapter
import com.project.safety.databinding.ActivityEmergencyContactsBinding
import com.project.safety.models.EmergencyContact
import com.project.safety.utils.SharedPrefManager

class EmergencyContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyContactsBinding
    private lateinit var sharedPref: SharedPrefManager
    private lateinit var adapter: EmergencyContactsAdapter
    private val contactsList = mutableListOf<EmergencyContact>()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val CONTACT_PICKER_REQUEST = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPrefManager with context
        sharedPref = SharedPrefManager.getInstance(this)

        setupRecyclerView()
        setupClickListeners()
        addDefaultContactsIfNeeded()
        loadContacts()
    }

    private fun setupRecyclerView() {
        adapter = EmergencyContactsAdapter(contactsList) { contact, position ->
            showContactOptions(contact, position)
        }

        binding.recyclerViewContacts.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewContacts.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnAddContact.setOnClickListener {
            showAddContactDialog()
        }

        binding.btnImportContacts.setOnClickListener {
            requestContactsPermission()
        }

        binding.btnSaveContacts.setOnClickListener {
            saveContacts()
        }
    }

    private fun loadContacts() {
        contactsList.clear()
        contactsList.addAll(sharedPref.getEmergencyContacts())
        adapter.notifyDataSetChanged()
    }

    private fun showAddContactDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etNumber = dialogView.findViewById<EditText>(R.id.etNumber)
        val etRelation = dialogView.findViewById<EditText>(R.id.etRelation)

        AlertDialog.Builder(this)
            .setTitle("Add Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val number = etNumber.text.toString().trim()
                val relation = etRelation.text.toString().trim()

                if (name.isEmpty() || number.isEmpty() || relation.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val contact = EmergencyContact(
                    name = name,
                    number = number,
                    relation = relation,
                    isPrimary = contactsList.isEmpty() // First contact is primary
                )

                if (contact.isPrimary) {
                    contactsList.forEach { it.isPrimary = false }
                }

                contactsList.add(contact)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Contact added", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestContactsPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openContactsPicker()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun openContactsPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        startActivityForResult(intent, CONTACT_PICKER_REQUEST)
    }

    private fun showContactOptions(contact: EmergencyContact, position: Int) {
        val options = arrayOf("Call", "Message", "Set as Primary", "Edit", "Delete")

        AlertDialog.Builder(this)
            .setTitle(contact.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> callContact(contact.number)
                    1 -> messageContact(contact.number)
                    2 -> setAsPrimary(position)
                    3 -> editContact(position)
                    4 -> deleteContact(position)
                }
            }
            .show()
    }

    private fun callContact(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phoneNumber")
        startActivity(intent)
    }

    private fun messageContact(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("smsto:$phoneNumber")
        startActivity(intent)
    }

    private fun setAsPrimary(position: Int) {
        contactsList.forEach { it.isPrimary = false }
        contactsList[position].isPrimary = true
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "Set as primary contact", Toast.LENGTH_SHORT).show()
    }

    private fun editContact(position: Int) {
        val contact = contactsList[position]
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etNumber = dialogView.findViewById<EditText>(R.id.etNumber)
        val etRelation = dialogView.findViewById<EditText>(R.id.etRelation)

        etName.setText(contact.name)
        etNumber.setText(contact.number)
        etRelation.setText(contact.relation)

        AlertDialog.Builder(this)
            .setTitle("Edit Contact")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val name = etName.text.toString().trim()
                val number = etNumber.text.toString().trim()
                val relation = etRelation.text.toString().trim()

                if (name.isEmpty() || number.isEmpty() || relation.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                contact.name = name
                contact.number = number
                contact.relation = relation

                adapter.notifyItemChanged(position)
                Toast.makeText(this, "Contact updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteContact(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete ${contactsList[position].name}?")
            .setPositiveButton("Delete") { _, _ ->
                val deletedName = contactsList[position].name
                contactsList.removeAt(position)
                adapter.notifyItemRemoved(position)
                Toast.makeText(this, "$deletedName deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveContacts() {
        sharedPref.saveEmergencyContacts(contactsList)
        Toast.makeText(this, "Contacts saved successfully", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openContactsPicker()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONTACT_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            val contactUri: Uri? = data.data
            contactUri?.let { uri ->
                try {
                    val cursor = contentResolver.query(
                        uri,
                        arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
                        null,
                        null,
                        null
                    )

                    cursor?.use {
                        if (it.moveToFirst()) {
                            val id = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
                            val name = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))

                            val phoneCursor = contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                arrayOf(id),
                                null
                            )

                            phoneCursor?.use { phoneIt ->
                                if (phoneIt.moveToFirst()) {
                                    val number = phoneIt.getString(
                                        phoneIt.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    ) ?: ""

                                    val cleanNumber = number.replace("\\D".toRegex(), "")

                                    if (cleanNumber.isNotEmpty()) {
                                        val contact = EmergencyContact(
                                            name = name,
                                            number = cleanNumber,
                                            relation = "Friend",
                                            isPrimary = contactsList.isEmpty()
                                        )

                                        if (contact.isPrimary) {
                                            contactsList.forEach { it.isPrimary = false }
                                        }

                                        contactsList.add(contact)
                                        adapter.notifyDataSetChanged()
                                        Toast.makeText(this, "Contact imported", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error importing contact", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun addDefaultContactsIfNeeded() {
        // Check if contacts already exist
        val existingContacts = sharedPref.getEmergencyContacts()
        if (existingContacts.isEmpty()) {
            // Add pre-configured emergency contacts
            val defaultContacts = listOf(
                EmergencyContact(
                    name = "Emergency Contact 1",
                    number = "8555017044",
                    relation = "Family",
                    isPrimary = true
                ),
                EmergencyContact(
                    name = "Emergency Contact 2",
                    number = "6281169906",
                    relation = "Friend",
                    isPrimary = false
                ),
                EmergencyContact(
                    name = "Emergency Contact 3",
                    number = "9951316295",
                    relation = "Family",
                    isPrimary = false
                ),
                EmergencyContact(
                    name = "Emergency Contact 4",
                    number = "9000707118",
                    relation = "Friend",
                    isPrimary = false
                )
            )
            
            sharedPref.saveEmergencyContacts(defaultContacts)
            Toast.makeText(this, "Default emergency contacts added", Toast.LENGTH_SHORT).show()
        }
    }
}
