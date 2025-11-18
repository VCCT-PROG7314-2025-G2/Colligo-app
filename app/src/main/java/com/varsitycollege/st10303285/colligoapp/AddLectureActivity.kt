package com.varsitycollege.st10303285.colligoapp

import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AddLectureActivity : AppCompatActivity() {

    private lateinit var etModuleName: com.google.android.material.textfield.TextInputEditText
    private lateinit var tvDaySelect: android.widget.TextView
    private lateinit var tvStartTime: android.widget.TextView
    private lateinit var tvEndTime: android.widget.TextView
    private lateinit var etRoom: com.google.android.material.textfield.TextInputEditText
    private lateinit var etNotes: com.google.android.material.textfield.TextInputEditText
    private lateinit var btnBack: android.widget.ImageView
    private lateinit var btnAddLecture: MaterialButton
    private lateinit var colorPicker: LinearLayout

    private var selectedDay = ""
    private var selectedColor = "#B5EAD7"  // default pastel mint
    private var selectedCircle: View? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var lectureId: String? = null  // for edit mode

    private val pastelColors = listOf(
        "#B5EAD7", "#FFC7CE", "#E0BBE4", "#A0D2EB",
        "#FFFFD8", "#FFDAB9", "#C7CEEA"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_lecture)

        initViews()
        setupColorPicker()
        checkForEditMode()

        btnBack.setOnClickListener { finish() }

        tvDaySelect.setOnClickListener { showDayPicker() }
        tvStartTime.setOnClickListener { showTimePicker(tvStartTime) }
        tvEndTime.setOnClickListener { showTimePicker(tvEndTime) }

        btnAddLecture.setOnClickListener { saveOrUpdateLecture() }
    }

    private fun initViews() {
        etModuleName = findViewById(R.id.etModuleName)
        tvDaySelect = findViewById(R.id.tvDaySelect)
        tvStartTime = findViewById(R.id.tvStartTime)
        tvEndTime = findViewById(R.id.tvEndTime)
        etRoom = findViewById(R.id.etRoom)
        etNotes = findViewById(R.id.etNotes)
        btnBack = findViewById(R.id.btnBack)
        btnAddLecture = findViewById(R.id.btnAddLecture)
        colorPicker = findViewById(R.id.colorPicker)
    }

    private fun checkForEditMode() {
        lectureId = intent.getStringExtra("LECTURE_ID")
        if (lectureId != null) {
            loadLectureForEditing()
            btnAddLecture.text = "Update Lecture"
        }
    }

    private fun loadLectureForEditing() {
        db.collection("lectures").document(lectureId!!).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    etModuleName.setText(doc.getString("moduleName"))
                    tvDaySelect.text = doc.getString("day")
                    selectedDay = doc.getString("day") ?: ""
                    tvStartTime.text = doc.getString("startTime")
                    tvEndTime.text = doc.getString("endTime")
                    etRoom.setText(doc.getString("room"))
                    etNotes.setText(doc.getString("notes"))
                    selectedColor = doc.getString("color") ?: "#B5EAD7"

                    // Highlight selected color
                    colorPicker.post {
                        for (i in 0 until colorPicker.childCount) {
                            val child = colorPicker.getChildAt(i)
                            val tagColor = child.tag as? String
                            if (tagColor == selectedColor) {
                                child.foreground = ContextCompat.getDrawable(this, R.drawable.circle_selected)
                                selectedCircle = child
                            }
                        }
                    }
                }
            }
    }

    private fun showDayPicker() {
        val days = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        android.app.AlertDialog.Builder(this)
            .setTitle("Select Day")
            .setItems(days) { _, which ->
                selectedDay = days[which]
                tvDaySelect.text = days[which]
            }
            .show()
    }

    private fun showTimePicker(view: android.widget.TextView) {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, h, m ->
            view.text = String.format("%02d:%02d", h, m)
        }, hour, minute, true).show()
    }

    private fun setupColorPicker() {
        colorPicker.removeAllViews()
        pastelColors.forEach { color ->
            val circle = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(80, 80).apply { setMargins(16, 0, 16, 0) }
                background = ContextCompat.getDrawable(this@AddLectureActivity, R.drawable.circle_normal)
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(color))
                tag = color  // for edit mode

                setOnClickListener {
                    selectedCircle?.foreground = null
                    selectedCircle = this
                    foreground = ContextCompat.getDrawable(this@AddLectureActivity, R.drawable.circle_selected)
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

    private fun saveOrUpdateLecture() {
        val module = etModuleName.text.toString().trim()
        val room = etRoom.text.toString().trim()
        val notes = etNotes.text.toString().trim()

        if (module.isEmpty() || selectedDay.isEmpty() || tvStartTime.text == "Start Time" || tvEndTime.text == "End Time") {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val lectureData = hashMapOf(
            "moduleName" to module,
            "day" to selectedDay,
            "startTime" to tvStartTime.text.toString(),
            "endTime" to tvEndTime.text.toString(),
            "room" to room,
            "notes" to notes,
            "color" to selectedColor,
            "userId" to auth.currentUser!!.uid,
            "timestamp" to System.currentTimeMillis()
        )

        val ref = if (lectureId == null) {
            db.collection("lectures").document()
        } else {
            db.collection("lectures").document(lectureId!!)
        }

        ref.set(lectureData)
            .addOnSuccessListener {
                Toast.makeText(this, if (lectureId == null) "Lecture Added!" else "Lecture Updated!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error saving lecture", Toast.LENGTH_SHORT).show()
            }
    }
}