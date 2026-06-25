package com.project.safety

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.safety.adapters.SafetyTipsAdapter
import com.project.safety.databinding.ActivitySafetyTipsBinding
import com.project.safety.models.SafetyTip

class SafetyTipsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySafetyTipsBinding
    private lateinit var adapter: SafetyTipsAdapter

    private val safetyTips = listOf(
        SafetyTip(
            title = "When Walking Alone",
            description = """
                1. Stay aware of your surroundings
                2. Avoid isolated areas
                3. Walk confidently
                4. Keep phone charged and accessible
                5. Share live location with trusted contacts
            """.trimIndent(),
            category = "General Safety"
        ),
        SafetyTip(
            title = "Public Transport Safety",
            description = """
                1. Sit near the driver
                2. Avoid empty compartments
                3. Keep valuables hidden
                4. Note vehicle number
                5. Trust your instincts
            """.trimIndent(),
            category = "Transport"
        ),
        SafetyTip(
            title = "Emergency Numbers",
            description = """
                Police: 100
                Ambulance: 102
                Women Helpline: 181
                National Emergency: 112
                Fire: 101
            """.trimIndent(),
            category = "Emergency"
        ),
        SafetyTip(
            title = "Self Defense Techniques",
            description = """
                1. Target vulnerable areas: eyes, nose, throat
                2. Use your keys as weapons
                3. Shout 'Fire' instead of 'Help'
                4. Use pepper spray if legal
                5. Take self-defense classes
            """.trimIndent(),
            category = "Self Defense"
        ),
        SafetyTip(
            title = "Online Safety",
            description = """
                1. Never share personal information
                2. Use strong passwords
                3. Enable two-factor authentication
                4. Be careful with location sharing
                5. Report suspicious behavior
            """.trimIndent(),
            category = "Digital Safety"
        ),
        SafetyTip(
            title = "Home Safety",
            description = """
                1. Keep doors locked
                2. Install security systems
                3. Have emergency numbers ready
                4. Know your neighbors
                5. Keep a whistle near bed
            """.trimIndent(),
            category = "Home"
        ),
        SafetyTip(
            title = "Car Safety",
            description = """
                1. Check backseat before entering
                2. Keep doors locked while driving
                3. Park in well-lit areas
                4. Keep phone charged
                5. Have emergency kit in car
            """.trimIndent(),
            category = "Transport"
        ),
        SafetyTip(
            title = "Legal Rights for Women",
            description = """
                1. Right to file FIR
                2. Right to free legal aid
                3. Protection under Domestic Violence Act
                4. Right to maintenance
                5. Right to equal pay
            """.trimIndent(),
            category = "Legal"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySafetyTipsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = SafetyTipsAdapter(safetyTips)
        binding.recyclerViewTips.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewTips.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnGeneral.setOnClickListener {
            filterTips("General Safety")
        }

        binding.btnTransport.setOnClickListener {
            filterTips("Transport")
        }

        binding.btnEmergency.setOnClickListener {
            filterTips("Emergency")
        }

        binding.btnSelfDefense.setOnClickListener {
            filterTips("Self Defense")
        }

        binding.btnDigital.setOnClickListener {
            filterTips("Digital Safety")
        }

        binding.btnLegal.setOnClickListener {
            filterTips("Legal")
        }

        binding.btnAll.setOnClickListener {
            adapter.filterTips(safetyTips)
        }
    }

    private fun filterTips(category: String) {
        val filteredTips = safetyTips.filter { it.category == category }
        adapter.filterTips(filteredTips)
    }
}
