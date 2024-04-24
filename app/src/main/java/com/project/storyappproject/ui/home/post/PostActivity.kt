package com.project.storyappproject.ui.home.post

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.project.storyappproject.R
import com.project.storyappproject.databinding.ActivityPostBinding
import com.project.storyappproject.ui.customview.CustomAlert
import com.project.storyappproject.ui.customview.CustomSuccessAlert
import com.project.storyappproject.ui.home.post.camerax.CameraActivity
import com.project.storyappproject.ui.home.post.camerax.CameraActivity.Companion.CAMERAX_RESULT
import com.project.storyappproject.utility.createCustomTempFile
import com.project.storyappproject.utility.uriToFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class PostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostBinding
    private val postViewModel : PostViewModel by viewModels()
    private lateinit var currentPhotoPath: String
    private var getFile: File? = null

    private var storyLatitude: Double = 0.0
    private var storyLongitude: Double = 0.0

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        postViewModel.isLoading.observe(this) {
            showLoading(it)
        }

        postViewModel.isError.observe(this) {
            Log.d("test", "Gagal upload")
        }

        checkPermission()
        fetchLocation()

        startGallery()
        startCamera()
        startCameraX()
        postStoryNoLocation()
        postStoryWithLocation()
        backButton()
    }

    private fun fetchLocation() {
        val task = fusedLocationProviderClient.lastLocation

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }
        task.addOnSuccessListener {
            if (it != null) {
                storyLongitude = it.longitude
                storyLatitude = it.latitude
                Log.d("location tracker : ", "${it.longitude} , ${it.latitude}")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CODE_PERMISSIONS) {
            if (!permissionGranted()) {
                Toast.makeText(this@PostActivity, R.string.no_access, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun permissionGranted() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkPermission() {
        if (!permissionGranted()) {
            ActivityCompat.requestPermissions(
                this@PostActivity,
                PERMISSIONS,
                CODE_PERMISSIONS
            )
        }
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            val myFile = File(currentPhotoPath)
            getFile = myFile
            val result = BitmapFactory.decodeFile(myFile.path)

            binding.postLayout.storyPictPreview.setImageBitmap(result)
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun startCamera() {
        binding.postLayout.camBtn.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.resolveActivity(packageManager)

            createCustomTempFile(applicationContext).also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this@PostActivity,
                    "com.project.storyappproject.mycamera",
                    it
                )
                currentPhotoPath = it.absolutePath
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                launcherIntentCamera.launch(intent)
            }
        }
    }

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedImg: Uri = result.data?.data as Uri
            val myFile = uriToFile(selectedImg, this@PostActivity)
            getFile = myFile
            binding.postLayout.storyPictPreview.setImageURI(selectedImg)
        }
    }

    private fun startGallery() {
        binding.postLayout.galleryBtn.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            val picImage = Intent.createChooser(intent, "Select An Image")
            launcherIntentGallery.launch(picImage)
        }
    }

    private val launcherIntentCameraX = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == CAMERAX_RESULT) {
            val imageUriString = result.data?.getStringExtra(CameraActivity.EXTRA_CAMERAX_IMAGE)
            val imageUri = imageUriString?.toUri() as Uri
            val myFile = uriToFile(imageUri, this)
            getFile = myFile

            binding.postLayout.storyPictPreview.setImageURI(imageUri)
        }
    }
    private fun startCameraX() {
        binding.postLayout.camxBtn.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            launcherIntentCameraX.launch(intent)
        }
    }

    private fun postStoryNoLocation() {
        binding.postLayout.postBtn.setOnClickListener {
            val description = binding.postLayout.descEt.text.toString()
            when {
                TextUtils.isEmpty(description) -> {
                    CustomAlert(this, R.string.errorPostNoDesc, R.drawable.alert_post_img).show()
                }
                getFile == null -> {
                    CustomAlert(this, R.string.errorPostNoImg, R.drawable.alert_post_img).show()
                }
                else -> {
                    lifecycleScope.launch {
                        showProgressBar()
                        delay(SPACE_TIME)
                        try {
                            postViewModel.postStory(getFile!!, description, storyLatitude, storyLongitude)
                            Log.d("testPost", "isi postingan : $description , $storyLatitude, $storyLongitude")
                            showSuccessAlert()
                        } catch (e: Exception) {
                            showErrorAlert()
                        } finally {
                            hideProgressBar()
                        }
                    }
                }
            }
        }
    }

    private fun postStoryWithLocation() {
        binding.postLayout.postWithLocation?.setOnClickListener {
            if (isLocationEnabled()) {
                val description = binding.postLayout.descEt.text.toString()
                when {
                    TextUtils.isEmpty(description) -> {
                        CustomAlert(this, R.string.errorPostNoDesc, R.drawable.alert_post_img).show()
                    }
                    getFile == null -> {
                        CustomAlert(this, R.string.errorPostNoImg, R.drawable.alert_post_img).show()
                    }
                    else -> {
                        lifecycleScope.launch {
                            showProgressBar()
                            delay(SPACE_TIME)
                            try {
                                postViewModel.postStory(getFile!!, description, storyLatitude, storyLongitude)
                                Log.d("testPost", "isi postingan : $description , $storyLatitude, $storyLongitude")
                                showSuccessAlert()
                            } catch (e: Exception) {
                                showErrorAlert()
                            } finally {
                                hideProgressBar()
                            }
                        }
                    }
                }
            } else {
                showLocationSettingsAlert()
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }

    private fun showLocationSettingsAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.locationAlert)
        builder.setMessage(R.string.locationMessage)
        builder.setPositiveButton(R.string.locationGoSetting) { _, _ ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        builder.setNegativeButton(R.string.locationCancel) { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showProgressBar() {
        binding.postLayout.root.visibility = View.INVISIBLE
        binding.loadingLayout.root.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.loadingLayout.root.visibility = View.INVISIBLE
    }

    private fun showSuccessAlert() {
        val customSuccessAlert = CustomSuccessAlert(this, R.string.successPost, R.drawable.success_post_alert_img) {
            setResult(Activity.RESULT_OK)
            finish()
            binding.postLayout.root.visibility = View.VISIBLE
        }
        customSuccessAlert.show()
    }

    private fun showErrorAlert() {
        CustomAlert(this, R.string.errorPost, R.drawable.alert_post_img).show()
    }

    private fun backButton() {
        binding.postLayout.backHome.setOnClickListener {
            finish()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingLayout.root.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    companion object {
        val PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        const val CODE_PERMISSIONS = 10
        const val SPACE_TIME = 1000L
    }
}