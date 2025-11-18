package com.varsitycollege.st10303285.colligoapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AssignmentsFragment : Fragment() {

    private val TAG = "AssignmentsFragment"

    // Views
    private lateinit var tvMonthYear: TextView
    private lateinit var calendarGrid: GridLayout

    // Calendar and Firebase
    private val calendar = Calendar.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Fragment attachment tracking
    private var isFragmentAttached = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView - inflating fragment_assignments")

        // Inflate the assignments layout (calendar grid view)
        val view = inflater.inflate(R.layout.fragment_assignments, container, false)

        // Find the views
        tvMonthYear = view.findViewById(R.id.tvMonthYear)
        calendarGrid = view.findViewById(R.id.calendarGrid)

        Log.d(TAG, "Views initialized")

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isFragmentAttached = true

        // Build the calendar grid
        updateCalendar()

        // Load assignments from Firestore
        loadAssignments()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isFragmentAttached = false
    }

    // Build the calendar grid for the current month
    private fun updateCalendar() {
        if (!isFragmentAttached || !isAdded) return

        Log.d(TAG, "updateCalendar() - Building calendar for ${calendar.time}")

        // Set the month/year title
        val monthYearText = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            .format(calendar.time)
            .uppercase()
        tvMonthYear.text = monthYearText
        Log.d(TAG, "Month title set to: $monthYearText")

        // Clear any existing calendar cells
        calendarGrid.removeAllViews()

        // Get calendar info for current month
        val tempCalendar = calendar.clone() as Calendar
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.
        val daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        Log.d(TAG, "First day of week: $firstDayOfWeek, Days in month: $daysInMonth")

        // Add day headers (SUN, MON, TUE, etc.) - already in XML, so we just add date cells

        // Add empty cells for days before the first day of month
        for (i in 1 until firstDayOfWeek) {
            addEmptyCell()
        }

        // Add cells for each day of the month
        for (day in 1..daysInMonth) {
            addDayCell(day)
        }

        // Add empty cells to fill the grid (6 rows × 7 columns = 42 cells total)
        val totalCells = 42 // 6 rows × 7 columns
        val cellsAdded = (firstDayOfWeek - 1) + daysInMonth
        val emptyCellsNeeded = totalCells - cellsAdded

        for (i in 0 until emptyCellsNeeded) {
            addEmptyCell()
        }

        Log.d(TAG, "✓ Calendar grid built with $daysInMonth days + $emptyCellsNeeded empty cells")
    }

    // Add an empty cell
    private fun addEmptyCell() {
        if (!isFragmentAttached || !isAdded) return

        val empty = View(requireContext())
        val layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = 0
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        }
        empty.layoutParams = layoutParams
        calendarGrid.addView(empty)
    }

    // Add a cell for a specific day
    private fun addDayCell(day: Int) {
        if (!isFragmentAttached || !isAdded) return

        try {
            // Create day cell container
            val cell = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.calendar_cell_background)
            }

            val layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(2, 2, 2, 2)
            }
            cell.layoutParams = layoutParams

            // Day number
            val dayText = TextView(requireContext()).apply {
                text = day.toString()
                textSize = 14f
                setTextColor(Color.BLACK)
                gravity = android.view.Gravity.CENTER
                setPadding(4, 8, 4, 4)
            }

            // Assignments container
            val assignmentsContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(2, 2, 2, 2)
            }

            cell.addView(dayText)
            cell.addView(assignmentsContainer)

            // Add click listener
            cell.setOnClickListener {
                Log.d(TAG, "Day $day clicked - opening AddAssignmentActivity")
                val intent = Intent(requireContext(), AddAssignmentActivity::class.java)
                // Pass the selected date
                val selectedDate = Calendar.getInstance().apply {
                    set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), day)
                }
                intent.putExtra("SELECTED_DATE", selectedDate.timeInMillis)
                startActivity(intent)
            }

            calendarGrid.addView(cell)
            Log.d(TAG, "Day cell added for day: $day")

        } catch (e: Exception) {
            Log.e(TAG, "Error creating day cell for day $day", e)
        }
    }

    // Load all assignments from Firebase
    private fun loadAssignments() {
        Log.d(TAG, "loadAssignments() - Loading from Firestore")

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "User not logged in")
            if (isFragmentAttached && isAdded) {
                Toast.makeText(requireContext(), "Please log in", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Query Firestore for assignments
        db.collection("assignments")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (!isFragmentAttached || !isAdded) {
                    Log.w(TAG, "Fragment not attached, skipping assignment loading")
                    return@addOnSuccessListener
                }

                Log.d(TAG, "✓ Firestore query successful - found ${documents.size()} assignments")

                // Clear all assignment chips first
                clearAssignmentChips()

                for (doc in documents) {
                    try {
                        val title = doc.getString("title") ?: "Assignment"
                        val dueDate = doc.getDate("dueDate")
                        val colorHex = doc.getString("color") ?: "#C2185B"
                        val assignmentId = doc.id

                        if (dueDate == null) {
                            Log.w(TAG, "Assignment has no due date: $title")
                            continue
                        }

                        // Check if this assignment is in the current month
                        val dueCal = Calendar.getInstance().apply { time = dueDate }
                        val currentCal = Calendar.getInstance().apply {
                            time = calendar.time
                        }

                        val isSameMonth = dueCal.get(Calendar.MONTH) == currentCal.get(Calendar.MONTH)
                        val isSameYear = dueCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR)

                        if (isSameMonth && isSameYear) {
                            val dayOfMonth = dueCal.get(Calendar.DAY_OF_MONTH)
                            Log.d(TAG, "Adding assignment '$title' to day $dayOfMonth")
                            addAssignmentChip(dayOfMonth, title, colorHex, assignmentId)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing assignment document", e)
                    }
                }

                Log.d(TAG, "✓ All assignments loaded and displayed")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "✗ Error loading assignments", exception)
                if (isFragmentAttached && isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Error loading assignments: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    // Clear all assignment chips from calendar
    private fun clearAssignmentChips() {
        for (i in 0 until calendarGrid.childCount) {
            val cell = calendarGrid.getChildAt(i) as? LinearLayout
            if (cell != null && cell.childCount > 1) {
                val assignmentsContainer = cell.getChildAt(1) as? LinearLayout
                assignmentsContainer?.removeAllViews()
            }
        }
    }

    // Add an assignment chip to a specific day
    private fun addAssignmentChip(day: Int, title: String, colorHex: String, assignmentId: String) {
        if (!isFragmentAttached || !isAdded) return

        Log.d(TAG, "addAssignmentChip() - Adding '$title' to day $day")

        try {
            // Calculate position in grid (accounting for empty cells at start)
            val tempCalendar = calendar.clone() as Calendar
            tempCalendar.set(Calendar.DAY_OF_MONTH, 1)
            val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK)
            val position = (firstDayOfWeek - 1) + (day - 1)

            Log.d(TAG, "Chip position in grid: $position (day: $day)")

            if (position >= calendarGrid.childCount) {
                Log.e(TAG, "Position $position out of bounds (grid has ${calendarGrid.childCount} cells)")
                return
            }

            val cell = calendarGrid.getChildAt(position) as? LinearLayout
            if (cell == null || cell.childCount < 2) {
                Log.e(TAG, "Cell not found or invalid at position $position")
                return
            }

            val assignmentsContainer = cell.getChildAt(1) as? LinearLayout
            if (assignmentsContainer == null) {
                Log.e(TAG, "Assignments container not found")
                return
            }

            // Create assignment chip
            val chip = LayoutInflater.from(requireContext()).inflate(
                R.layout.item_assignment_chip,
                assignmentsContainer,
                false
            ) as CardView

            chip.findViewById<TextView>(R.id.tvAssignment).text = title

            try {
                chip.setCardBackgroundColor(Color.parseColor(colorHex))
            } catch (e: Exception) {
                Log.e(TAG, "Invalid color: $colorHex", e)
                chip.setCardBackgroundColor(Color.parseColor("#C2185B"))
            }

            // Make clickable
            chip.setOnClickListener {
                Log.d(TAG, "Assignment chip clicked: $title")
                val intent = Intent(requireContext(), AddAssignmentActivity::class.java)
                intent.putExtra("ASSIGNMENT_ID", assignmentId)
                startActivity(intent)
            }

            assignmentsContainer.addView(chip)
            Log.d(TAG, "✓ Chip added successfully for '$title'")

        } catch (e: Exception) {
            Log.e(TAG, "Error adding assignment chip for day $day", e)
        }
    }

    // Refresh when fragment becomes visible
    override fun onResume() {
        super.onResume()
        if (isFragmentAttached) {
            Log.d(TAG, "onResume - refreshing assignments")
            loadAssignments()
        }
    }
}

// Assignment data class
data class Assignment(
    val id: String = "",
    val title: String = "",
    val dueDate: Date = Date(),
    val color: String = "#C2185B",
    val notes: String = ""
)