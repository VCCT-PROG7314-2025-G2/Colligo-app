package com.varsitycollege.st10303285.colligoapp

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddAssignmentActivity : AppCompatActivity() {

    private val TAG = "AddAssignmentActivity"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var etTitle: TextInputEditText
    private lateinit var tvDueDate: TextView
    private lateinit var etNotes: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var colorPicker: LinearLayout
    private lateinit var btnBack: ImageView

    private var selectedDueDate = Calendar.getInstance()
    private var selectedColor = "#B5EAD7" // Default pastel mint to match lectures
    private var selectedCircle: View? = null

    // Use the same pastel colors as your lecture activity
    private val pastelColors = listOf(
        "#B5EAD7", "#FFC7CE", "#E0BBE4", "#A0D2EB",
        "#FFFFD8", "#FFDAB9", "#C7CEEA"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_assignment)

        Log.d(TAG, "AddAssignmentActivity created")

        initViews()
        setupColorPicker()
        setupClickListeners()
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etTitle)
        tvDueDate = findViewById(R.id.tvDueDate)
        etNotes = findViewById(R.id.etNotes)
        btnSave = findViewById(R.id.btnSave)
        colorPicker = findViewById(R.id.colorPicker)
        btnBack = findViewById(R.id.btnBack)

        // Set default due date to current date
        updateDueDateText()
    }

    private fun setupColorPicker() {
        colorPicker.removeAllViews()
        pastelColors.forEach { color ->
            val circle = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                    setMargins(16, 0, 16, 0)
                }
                background = ContextCompat.getDrawable(this@AddAssignmentActivity, R.drawable.circle_normal)
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(color))
                tag = color

                setOnClickListener {
                    selectedCircle?.foreground = null
                    selectedCircle = this
                    foreground = ContextCompat.getDrawable(this@AddAssignmentActivity, R.drawable.circle_selected)
                    selectedColor = color
                }
            }
            colorPicker.addView(circle)
        }
        // Default selection
        colorPicker.getChildAt(0)?.let {
            it.foreground = ContextCompat.getDrawable(this, R.drawable.circle_selected)
            selectedCircle = it
        }
    }

    private fun setupClickListeners() {
        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Due date selection
        tvDueDate.setOnClickListener {
            showDatePicker()
        }

        // Save assignment
        btnSave.setOnClickListener {
            saveAssignment()
        }
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDueDate.set(Calendar.YEAR, year)
                selectedDueDate.set(Calendar.MONTH, month)
                selectedDueDate.set(Calendar.DAY_OF_MONTH, day)
                updateDueDateText()
            },
            selectedDueDate.get(Calendar.YEAR),
            selectedDueDate.get(Calendar.MONTH),
            selectedDueDate.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun updateDueDateText() {
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        tvDueDate.text = "Due: ${dateFormat.format(selectedDueDate.time)}"
    }

    private fun saveAssignment() {
        val title = etTitle.text.toString().trim()
        val notes = etNotes.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter assignment title", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show()
            return
        }

        val assignmentData = hashMapOf(
            "title" to title,
            "dueDate" to selectedDueDate.time,
            "color" to selectedColor,
            "notes" to notes,
            "userId" to userId,
            "createdAt" to Date()
        )

        db.collection("assignments")
            .add(assignmentData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Assignment saved with ID: ${documentReference.id}")
                Toast.makeText(this, "Assignment added successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving assignment", e)
                Toast.makeText(this, "Error saving assignment", Toast.LENGTH_SHORT).show()
            }
    }
}