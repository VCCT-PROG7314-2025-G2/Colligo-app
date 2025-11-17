package com.varsitycollege.st10303285.colligoapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class AccountActivity : AppCompatActivity() {

    private val TAG = "AccountActivity"

    // UI
    private lateinit var profileImage: ImageView
    private lateinit var etName: EditText
    private lateinit var etAge: EditText
    private lateinit var genderGroup: RadioGroup
    private lateinit var rbFemale: RadioButton
    private lateinit var rbMale: RadioButton
    private lateinit var etEmail: EditText
    private lateinit var etNumber: EditText
    private lateinit var btnSave: Button
    private lateinit var backButton: ImageView

    // Firebase
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = Firebase.storage


    private var selectedImageUri: Uri? = null

    // ActivityResultLauncher for picking an image
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // preview using Glide
            Glide.with(this).load(it).centerCrop().into(profileImage)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        // find views
        profileImage = findViewById(R.id.profileImage)
        etName = findViewById(R.id.etName)
        etAge = findViewById(R.id.etAge)
        genderGroup = findViewById(R.id.genderGroup)
        rbFemale = findViewById(R.id.rbFemale)
        rbMale = findViewById(R.id.rbMale)
        etEmail = findViewById(R.id.etEmail)
        etNumber = findViewById(R.id.etNumber)
        btnSave = findViewById(R.id.btnSave)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener { finish() }

        profileImage.setOnClickListener {
            // open system picker for images
            pickImageLauncher.launch("image/*")
        }

        btnSave.setOnClickListener { saveProfile() }

        // load existing profile if user logged in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()

            return
        }

        loadProfile(currentUser.uid)
    }

    private fun loadProfile(uid: String) {
        // read from users/{uid}
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    etName.setText(doc.getString("name") ?: "")
                    etAge.setText(doc.get("age")?.toString() ?: "")
                    etEmail.setText(doc.getString("studentEmail") ?: "")
                    etNumber.setText(doc.getString("number") ?: "")

                    val gender = doc.getString("gender") ?: ""
                    when (gender.lowercase()) {
                        "female" -> genderGroup.check(R.id.rbFemale)
                        "male" -> genderGroup.check(R.id.rbMale)
                    }

                    val photoUrl = doc.getString("photoUrl")
                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .centerCrop()
                            .placeholder(R.drawable.ic_account_circle)
                            .into(profileImage)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "loadProfile: failed", e)
            }
    }

    private fun saveProfile() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "You must be logged in to save profile", Toast.LENGTH_SHORT).show()
            return
        }

        // collect fields
        val name = etName.text.toString().trim()
        val ageText = etAge.text.toString().trim()
        val age = ageText.ifEmpty { null }
        val studentEmail = etEmail.text.toString().trim()
        val number = etNumber.text.toString().trim()
        val gender = when (genderGroup.checkedRadioButtonId) {
            R.id.rbFemale -> "Female"
            R.id.rbMale -> "Male"
            else -> ""
        }

        // basic validation
        if (name.isEmpty()) {
            etName.error = "Name required"
            etName.requestFocus()
            return
        }

        // if an image was selected, upload it first, then save profile
        val uploadTask = selectedImageUri?.let { uri ->
            uploadProfileImageAsBlob(user.uid, uri)
        }

        if (uploadTask != null) {
            // upload returns a Task<String> for the download URL — chain saving after success
            uploadTask
                .addOnSuccessListener { downloadUrl ->
                    saveProfileToFirestore(user.uid, name, age, gender, studentEmail, number, downloadUrl)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            // no image change — just save other fields
            saveProfileToFirestore(user.uid, name, age, gender, studentEmail, number, null)
        }
    }

    /**
     * Uploads an image Uri to Firebase Storage after compressing it to JPEG bytes.
     * Returns the Storage upload task's continuation - we fetch download URL and return that Task.
     */
    private fun uploadProfileImageAsBlob(uid: String, uri: Uri) =
        // compress -> putBytes -> get download URL
        storage.reference.child("profile_images/$uid/profile.jpg").let { fileRef ->
            try {
                val inStream = contentResolver.openInputStream(uri) ?: return@let null
                val srcBitmap = BitmapFactory.decodeStream(inStream)
                inStream.close()

                // scale (limit to 1024px on long side)
                val maxDim = 1024
                val scaled = if (srcBitmap.width > maxDim || srcBitmap.height > maxDim) {
                    val ratio = srcBitmap.width.toFloat() / srcBitmap.height.toFloat()
                    if (ratio > 1f) {
                        Bitmap.createScaledBitmap(srcBitmap, maxDim, (maxDim / ratio).toInt(), true)
                    } else {
                        Bitmap.createScaledBitmap(srcBitmap, (maxDim * ratio).toInt(), maxDim, true)
                    }
                } else srcBitmap

                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos) // compress quality
                val bytes = baos.toByteArray()
                baos.close()

                val uploadTask = fileRef.putBytes(bytes)
                // return a Task that resolves to download url (Task<Uri>)
                // we map it to a Task<String> for convenience (download URL string)
                val urlTask = uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                    fileRef.downloadUrl
                }.continueWith { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Get download failed")
                    task.result.toString()
                }
                urlTask
            } catch (e: Exception) {
                Log.e(TAG, "uploadProfileImageAsBlob: error", e)
                null
            }
        } ?: run {
            // return a failed Task if null
            val t = com.google.android.gms.tasks.Tasks.forException<String>(Exception("Could not open image"))
            t
        }

    private fun saveProfileToFirestore(
        uid: String,
        name: String,
        age: String?,
        gender: String,
        studentEmail: String,
        number: String,
        photoUrl: String?
    ) {
        val docRef = firestore.collection("users").document(uid)

        val data = mutableMapOf<String, Any>(
            "name" to name,
            "gender" to gender,
            "studentEmail" to studentEmail,
            "number" to number
        )
        age?.let { data["age"] = it }
        photoUrl?.let { data["photoUrl"] = it }

        docRef.set(data, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "saveProfileToFirestore", e)
            }
    }
}