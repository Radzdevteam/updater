package com.radzdev.radzupdater

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

class updater(private val context: Context, private val updateUrl: String) {

    private val client = OkHttpClient()
    private val checker = "aHR0cHM6Ly9yYXcuZ2l0aHViLmNvbS9SYWR6ZGV2dGVhbS9TZWN1cml0eVBhY2thZ2VDaGVja2VyL3JlZnMvaGVhZHMvbWFpbi9jaGVja2Vy"

    // Check if the app has the required permission
    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Request permission if not granted
    private fun requestPermission() {
        if (!checkPermission()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    context as Activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showPermissionExplanationDialog()
            } else {
                // Directly request permission
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            // Permission already granted, proceed with package validation
            validatePackage()
        }
    }

    // Show permission explanation dialog
    private fun showPermissionExplanationDialog() {
        val dialog = AlertDialog.Builder(context)
            .setTitle("Permission Needed")
            .setMessage("Storage permission is required to download updates. Please grant this permission.")
            .setPositiveButton("Grant Permission") { _, _ ->
                // Request the permission
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_REQUEST_CODE
                )
            }
            .setCancelable(false)
            .create()

        dialog.setCanceledOnTouchOutside(false)

        dialog.show()
    }

    // Handle permission result
    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with package validation
                validatePackage()
            } else {
                // Permission denied
                Toast.makeText(context, "Storage permission is required to download updates.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Start checking for updates
    fun checkForUpdates() {
        // Check for permissions before proceeding with updates
        requestPermission()
    }

    private fun validatePackage() {
        Thread {
            try {
                val decodedUrl = String(android.util.Base64.decode(checker, android.util.Base64.DEFAULT))
                val request = Request.Builder().url(decodedUrl).build()
                val response: Response = client.newCall(request).execute()
                val jsonResponse = response.body?.string()

                if (!jsonResponse.isNullOrEmpty()) {
                    val jsonObject = JSONObject(jsonResponse)
                    if (jsonObject.has("valid_packages")) {
                        val validPackages = jsonObject.optJSONArray("valid_packages")

                        // Log the fetched packages
                  //      Log.d("Updater", "Fetched valid packages: $validPackages")

                        // Check if package is valid
                        val isPackageValid = (0 until validPackages.length()).any { index ->
                            val validPackage = validPackages.getString(index)
                  //          Log.d("Updater", "Valid package: $validPackage, Context package: ${context.packageName}")
                            validPackage.trim() == context.packageName.trim()
                        }

                //        Log.d("Updater", "Is package valid: $isPackageValid")

                        if (isPackageValid) {
                            fetchUpdateDetails()
                        } else {
                            showInvalidPackageDialog()
                        }
                    } else {
                        showValidationError("Invalid response from server: missing 'valid_packages'.")
                    }
                } else {
                    showValidationError("Empty response from server.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showValidationError("Failed to validate package.")
            }
        }.start()
    }



    // Fetch update details from the server
    private fun fetchUpdateDetails() {
        val request = Request.Builder()
            .url(updateUrl)
            .build()

        Thread {
            try {
                val response: Response = client.newCall(request).execute()
                val jsonResponse = response.body?.string()
                val updateDetails = Gson().fromJson(jsonResponse, UpdateDetails::class.java)

                val activity = context as? Activity
                activity?.runOnUiThread {
                    if (updateDetails != null) {
                        val currentVersion = getCurrentAppVersion()

                        // If an update is available, show a dialog
                        if (currentVersion != updateDetails.latestVersion) {
                            showUpdateDialog(updateDetails)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val activity = context as? Activity
                activity?.runOnUiThread {
                    Toast.makeText(context, "Failed to fetch update details", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun getCurrentAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "Unknown"
        }
    }

    private fun showUpdateDialog(updateDetails: UpdateDetails) {
        val releaseNotes = updateDetails.releaseNotes.joinToString("\n")
        val message = """
            New update available!
            
            Version: ${updateDetails.latestVersion}
            
            Release Notes:
            $releaseNotes
        """.trimIndent()

        val dialogBuilder = AlertDialog.Builder(context)
            .setTitle("New Update Available")
            .setMessage(message)
            .setPositiveButton("Download") { _, _ -> downloadTask(updateDetails.url) }

        val alertDialog = dialogBuilder.create()
        alertDialog.setCancelable(false)
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.show()
    }

    private fun showInvalidPackageDialog() {
        val activity = context as? Activity
        activity?.runOnUiThread {
            AlertDialog.Builder(context)
                .setTitle("App Access Denied")
                .setMessage("Access to Radz Respiratories is restricted due to security and compliance protocols. Please contact the developer to resolve the issue and obtain the necessary authorization.")
                .setCancelable(false)
                .setPositiveButton("EXIT") { _, _ ->
                    activity.finishAffinity()
                    System.exit(0)
                }
                .show()
        }
    }

    private fun showValidationError(message: String) {
        val activity = context as? Activity
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadTask(url: String) {
        val downloadApk = DownloadApk(context)
        downloadApk.startDownloadingApk(url, "Update ${getCurrentAppVersion()}")
    }

    // Data class to hold the update details from the server
    data class UpdateDetails(
        val latestVersion: String,
        val url: String,
        val releaseNotes: List<String>
    )

    companion object {
        const val STORAGE_PERMISSION_REQUEST_CODE = 100
    }
}
