package com.smh.finalproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log

import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.smh.finalproject.ml.ModelUnquant
import com.google.firebase.auth.FirebaseAuth
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.text.Html
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toBitmap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


class ImageDetailActivity : AppCompatActivity() {

    private lateinit var camera: ImageView
    private lateinit var gallery: ImageView
    private lateinit var imageView: ImageView
    private lateinit var result: TextView
    private lateinit var back: Button
    private val imageSize = 224

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseStorage: FirebaseStorage

    private lateinit var firebaseDB: FirebaseFirestore


    private val savedImageAndTextSet = HashSet<String>()
    private val savedImageHashes = HashSet<String>()

    private lateinit var currentUserID: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_detail)

        camera = findViewById(R.id.button)
        gallery = findViewById(R.id.button2)
        back = findViewById(R.id.backButton)

        result = findViewById(R.id.result)
        imageView = findViewById(R.id.imageView)
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()

        firebaseDB = FirebaseFirestore.getInstance()


        val currentUser = firebaseAuth.currentUser
        currentUserID = currentUser?.uid ?: ""

        //Retrieve user's ID
        val saveButton = findViewById<Button>(R.id.saveImageButton)

        camera.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, 3)
            } else {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
            }
        }


        gallery.setOnClickListener {
            val cameraIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(cameraIntent, 1)
        }


        saveButton.setOnClickListener {
            // Check if an image is displayed in the ImageView
            if (imageView.drawable != null) {
                // Get the drawable bitmap from ImageView
                val drawableBitmap = (imageView.drawable).toBitmap()

                // Display popup with dropdown options
                showTagSelectionPopup(drawableBitmap)
            } else {
                // Handle the case when no image is displayed
                Log.e("ImageDetailActivity", "No image to save.")
            }
        }

        back.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }


    private fun showTagSelectionPopup(bitmap: Bitmap) {
        val tags = arrayOf("kitchen", "office", "workspace") // Dropdown options

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Tag")
        builder.setItems(tags) { _, which ->
            val selectedTag = tags[which]
          //  Toast.makeText(this, selectedTag, Toast.LENGTH_SHORT).show()
            // Store the bitmap image and the classification text with the selected tag
            preventFirebaseDuplicates(bitmap, result.text.toString(), selectedTag)
        }
        val dialog = builder.create()
        dialog.show()
    }


    private fun loadExistingImageUUIDs() {
        val storageRef = firebaseStorage.reference
        val imagesRef = storageRef.child("images")

        imagesRef.listAll().addOnSuccessListener { listResult ->
                for (item in listResult.items) {
                    // Get the full name of the image (with extension) and extract the UUID part
                    val fullName = item.name
                    val parts = fullName.split(".")
                    if (parts.isNotEmpty()) {
                        val uuid = parts[0] // Extract UUID part
                        savedImageAndTextSet.add(uuid)
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("ImageDetailActivity", "Failed to retrieve image UUIDs: ${e.message}")
            }
    }

    private fun loadExistingImageHashes() {
        val storageRef = firebaseStorage.reference
        val imagesRef = storageRef.child("images")

        imagesRef.listAll().addOnSuccessListener { listResult ->
                for (item in listResult.items) {
                    // Fetch the image bytes
                    item.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
                            // Calculate the hash of the image data
                            val imageHash = calculateHash(bytes)
                            savedImageHashes.add(imageHash)
                        }.addOnFailureListener { e ->
                            Log.e(
                                "ImageDetailActivity", "Failed to fetch image bytes: ${e.message}"
                            )
                        }
                }
            }.addOnFailureListener { e ->
                Log.e("ImageDetailActivity", "Failed to retrieve image list: ${e.message}")
            }
    }

    private fun calculateHash(imageBytes: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(imageBytes)

        // Convert byte array to hexadecimal representation
        return BigInteger(1, digest).toString(16).padStart(32, '0')
    }


    private fun preventFirebaseDuplicates(
        bitmap: Bitmap, classificationText: String, selectedTag: String
    ) {
        loadExistingImageUUIDs()
        loadExistingImageHashes()

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageData = baos.toByteArray()

        // Calculate hash of the image data
        val imageHash = calculateHash(imageData)

        if (!savedImageHashes.contains(imageHash)) {
            // If the hash doesn't exist, store the image and update the hash set
            storeImageAndClassification(bitmap, classificationText, selectedTag)
            savedImageHashes.add(imageHash)
        } else {
            Log.d("ImageDetailActivity", "Image already exists.")
            Toast.makeText(this, "Image already exists!", Toast.LENGTH_SHORT).show()
        }
    }


    // Add an additional parameter imageUUID for the image UUID
    private fun storeImageAndClassification(
        bitmap: Bitmap, classificationText: String, selectedTag: String
    ) {
        //  binding.progressBar.visibility = View.VISIBLE
        val storageRef = firebaseStorage.reference
        val pairUUID = UUID.randomUUID().toString() // Generate a single UUID for image-text pair
        val imagesRef =
            storageRef.child("images/$currentUserID/$pairUUID.jpg") // Store image in Firebase Storage
        val textRef =
            storageRef.child("classification/$currentUserID/$pairUUID.txt") // Store text in Firebase Storage

        // Convert the bitmap to bytes
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageData = baos.toByteArray()

        // Store the image in Firebase Storage
        val uploadImageTask = imagesRef.putBytes(imageData)
        uploadImageTask.addOnSuccessListener { _ ->
            imagesRef.downloadUrl.addOnSuccessListener { imageUrl ->
                Log.d(
                    "ImageDetailActivity",
                    "Image uploaded to Firebase Storage. Image URL: $imageUrl"
                )

                // Store the classification text in Firebase Storage
                val textBytes = classificationText.toByteArray()
                val uploadTextTask = textRef.putBytes(textBytes)
                uploadTextTask.addOnSuccessListener { _ ->
                    textRef.downloadUrl.addOnSuccessListener { textUrl ->
                        Log.d(
                            "ImageDetailActivity",
                            "Classification text uploaded to Firebase Storage. Text URL: $textUrl"
                        )

                        // Create a Firestore document reference
                        val docRef = firebaseDB.collection("images")
                            .document() // Use auto-generated ID for the document

                        val timestamp = System.currentTimeMillis()
                        val date = Date(timestamp)
                        val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                        val formattedDate = formatter.format(date)

                        val checklist = when (selectedTag) {
                            "Office" -> getOfficeChecklist()
                            "Kitchen" -> getKitchenChecklist()
                            else -> getOtherChecklist()
                        }.map { ChecklistItem(it, false) }

                        // Create a data object to be stored in Firestore
                        val data = hashMapOf(
                            "imageUrl" to imageUrl.toString(), // Replace imageUrl with the actual URL obtained
                            "textUrl" to textUrl.toString(), // Replace textUrl with the actual URL obtained
                            "classification" to classificationText,
                            "userId" to currentUserID,
                            "timestamp" to formattedDate,
                            "tag" to selectedTag,
                            "checkedItems" to checklist.associateBy({ it.text }, { it.isChecked }),
                            "imgId" to pairUUID
                        )

                        // Save the data into Firestore
                        docRef.set(data).addOnSuccessListener {
                                Toast.makeText(this, "Uploaded to Dashboard!", Toast.LENGTH_SHORT)
                                    .show()
                                Log.d(
                                    "ImageDetailActivity",
                                    "Image data saved successfully to Firestore"
                                )
                            }.addOnFailureListener { e ->
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT)
                                    .show()
                                Log.e(
                                    "ImageDetailActivity", "Error saving image data: ${e.message}"
                                )
                            }
                    }.addOnFailureListener { textUrlFailure ->
                        Toast.makeText(
                            this,
                            "Error getting text URL: ${textUrlFailure.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(
                            "ImageDetailActivity",
                            "Failed to get text URL: ${textUrlFailure.message}"
                        )
                    }
                }.addOnFailureListener { textUploadFailure ->
                    Toast.makeText(
                        this,
                        "Error uploading classification text: ${textUploadFailure.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e(
                        "ImageDetailActivity",
                        "Error uploading classification text: ${textUploadFailure.message}"
                    )
                }
            }.addOnFailureListener { imageUrlFailure ->
                Toast.makeText(
                    this, "Error getting image URL: ${imageUrlFailure.message}", Toast.LENGTH_SHORT
                ).show()
                Log.e("ImageDetailActivity", "Failed to get image URL: ${imageUrlFailure.message}")
            }
        }.addOnFailureListener { imageUploadFailure ->
            Toast.makeText(
                this, "Error uploading image: ${imageUploadFailure.message}", Toast.LENGTH_SHORT
            ).show()
            Log.e("ImageDetailActivity", "Error uploading image: ${imageUploadFailure.message}")
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                1 -> {
                    // Gallery
                    val selectedImage: Uri? = data?.data
                    displayImage(selectedImage)
                }

                3 -> {
                    // Camera
                    val photo: Bitmap? = data?.extras?.get("data") as? Bitmap
                    if (photo != null) {
                        val uri = getImageUri(photo)
                        displayImage(uri)
                    } else {
                        Log.e("Camera", "Failed to capture image from camera")
                        // Handle the case where photo is null (capture failed)
                    }
                }
            }
        } else {
            Log.e("Camera", "Failed to capture image: resultCode = $resultCode")
            // Handle the case where capturing image failed (resultCode indicates failure)
        }
    }

    private fun displayImage(imageUri: Uri?) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
            val resizedBitmap = resizeBitmap(bitmap, imageSize, imageSize)
            imageView.setImageBitmap(resizedBitmap)
            processImage(resizedBitmap)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun getImageUri(inImage: Bitmap): Uri {
        val bytes = ByteBuffer.allocate(inImage.byteCount)
        inImage.copyPixelsToBuffer(bytes)
        val path = MediaStore.Images.Media.insertImage(
            contentResolver, inImage, "Title", null
        )
        return Uri.parse(path)
    }

    private fun processImage(bitmap: Bitmap) {
        val model = ModelUnquant.newInstance(applicationContext)

        // Convert Bitmap to ByteBuffer
        val inputFeature0 =
            TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        val byteBuffer = convertBitmapToByteBuffer(bitmap)
        inputFeature0.loadBuffer(byteBuffer)

        // Runs model inference and gets result.
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        // Interpret the output tensor
        val classLabels = listOf("clean", "messy") // Replace with your actual class labels

        val maxIndex =
            outputFeature0.floatArray.indices.maxByOrNull { outputFeature0.floatArray[it] } ?: -1
        val predictedClassLabel = if (maxIndex != -1) {
            classLabels[maxIndex]
        } else {
            "Unknown"
        }
        val premiumLabels = listOf("high premium", "medium premium", "low premium")

        if (predictedClassLabel == classLabels[1]) { // Check if predicted class is "messy"
            val messyScore = outputFeature0.floatArray[1] * 100 // Get messy probability
            //   val highPremiumProbability = 0.7 * messyScore // Calculate high premium probability
            //     val mediumPremiumProbability = 0.4 * messyScore // Calculate medium premium probability
            //   val lowPremiumProbability = 0.1 * messyScore // Calculate low premium probability

            // Determine the predicted premium class based on probabilities
            var predictedPremiumLabel = "Unknown"
            if (messyScore > 0.7) {
                predictedPremiumLabel = premiumLabels[0] // Set high premium
            } else if (messyScore > 0.4) {
                predictedPremiumLabel = premiumLabels[1] // Set medium premium
            } else {
                predictedPremiumLabel = premiumLabels[2] // Set low premium
            }

            // Format and display the result
            val resultText = "$predictedClassLabel"   // Predicted Premium: $predictedPremiumLabel"
            result.text = Html.fromHtml(resultText, Html.FROM_HTML_MODE_COMPACT)
            result.gravity = Gravity.CENTER
        } else {

            val cleanScore = String.format("%.1f%%", outputFeature0.floatArray[0] * 100)
            val resultText = "$predictedClassLabel"
            result.text = resultText
            result.gravity = Gravity.CENTER
        }

        model.close()

    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer =
            ByteBuffer.allocateDirect(4 * 224 * 224 * 3) // Assuming FLOAT32, adjust if needed
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(224 * 224)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in pixels) {
            byteBuffer.putFloat((pixelValue shr 16 and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixelValue shr 8 and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixelValue and 0xFF) / 255.0f)
        }

        return byteBuffer
    }

    private fun getOfficeChecklist(): List<String> {
        return listOf(
            "Wipe surfaces regularly",
            "Organize storage spaces",
            "Dispose of trash regularly",
            "Sweep and mop the floors",
            "Check and refill office supplies"
        )
    }

    private fun getKitchenChecklist(): List<String> {
        return listOf(
            "Wash dishes promptly",
            "Wipe surfaces regularly",
            "Organize storage spaces",
            "Dispose of trash regularly",
            "Sweep and mop the floors"
        )
    }

    private fun getOtherChecklist(): List<String> {
        return listOf(
            "Wipe surfaces regularly",
            "Organize storage spaces",
            "Dispose of trash regularly",
            "Sweep and mop the floors",
            "Remove any trip hazards"
        )
    }

}