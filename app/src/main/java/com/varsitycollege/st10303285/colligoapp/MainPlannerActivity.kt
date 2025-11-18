package com.varsitycollege.st10303285.colligoapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainPlannerActivity : AppCompatActivity() {

    // Views from the top bar
    private lateinit var tvTabTitle: TextView
    private lateinit var btnToggleLeft: ImageView
    private lateinit var btnToggleRight: ImageView
    private lateinit var btnAdd: FloatingActionButton
    private lateinit var btnSettings: ImageView
    private lateinit var btnSearch: ImageView

    // Track which view we're showing
    private var isLectures = true
    private val TAG = "MainPlannerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_planner)

        Log.d(TAG, "onCreate - Setting up the planner")

        // Initialize all the views from the layout
        tvTabTitle = findViewById(R.id.tvTabTitle)
        btnToggleLeft = findViewById(R.id.btnToggleLeft)
        btnToggleRight = findViewById(R.id.btnToggleRight)
        btnAdd = findViewById(R.id.btnAdd)
        btnSettings = findViewById(R.id.btnSettings)
        btnSearch = findViewById(R.id.btnSearch)

        // Show lectures fragment on first load only
        if (savedInstanceState == null) {
            Log.d(TAG, "First time opening - showing Lectures")
            showLectures()
        }

        // Setup the arrow buttons to toggle between lectures and assignments
        btnToggleLeft.setOnClickListener {
            Log.d(TAG, "Left arrow clicked - toggling view")
            toggleView()
        }

        btnToggleRight.setOnClickListener {
            Log.d(TAG, "Right arrow clicked - toggling view")
            toggleView()
        }

        // FAB button - opens different activities based on current view
        btnAdd.setOnClickListener {
            if (isLectures) {
                Log.d(TAG, "Opening Add Lecture screen")
                startActivity(Intent(this, AddLectureActivity::class.java))
            } else {
                Log.d(TAG, "Opening Add Assignment screen")
                startActivity(Intent(this, AddAssignmentActivity::class.java))
            }
        }

        // Settings button (you can implement later)
        btnSettings.setOnClickListener {
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    // This function toggles between Lectures and Assignments
    private fun toggleView() {
        // Flip the boolean
        isLectures = !isLectures

        // Update the title text
        val newTitle = if (isLectures) "Lectures" else "Assignments"
        tvTabTitle.text = newTitle

        Log.d(TAG, "Toggling to: $newTitle")
        Toast.makeText(this, "Switched to $newTitle", Toast.LENGTH_SHORT).show()

        // Show the correct fragment
        if (isLectures) {
            showLectures()
        } else {
            showAssignments()
        }
    }

    // Show the lectures fragment (5-column view)
    private fun showLectures() {
        Log.d(TAG, "showLectures() - Replacing fragment with LecturesFragment")
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LecturesFragment())
                .commit()
            Log.d(TAG, "✓ LecturesFragment loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error loading LecturesFragment", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Show the assignments fragment (calendar view)
    private fun showAssignments() {
        Log.d(TAG, "showAssignments() - Replacing fragment with AssignmentsFragment")
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AssignmentsFragment())
                .commit()
            Log.d(TAG, "✓ AssignmentsFragment loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error loading AssignmentsFragment", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Reload data when returning from Add screens
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - Refreshing current fragment")
        // Refresh the current fragment to show any new data
        if (isLectures) {
            showLectures()
        } else {
            showAssignments()
        }
    }
}