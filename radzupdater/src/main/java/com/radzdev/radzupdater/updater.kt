package com.radzdev.radzupdater

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class updater(private val context: Context, private val updateUrl: String) {

    private val client = OkHttpClient()

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
            // Permission already granted, proceed with update check
            fetchUpdateDetails()
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
                // Permission granted, proceed with update check
                fetchUpdateDetails()
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

    // Fetch update details from the server
    private fun fetchUpdateDetails() {
        val request = Request.Builder()
            .url(updateUrl)
            .build()

        // Run network request on a background thread
        Thread {
            try {
                val response: Response = client.newCall(request).execute()
                val jsonResponse = response.body?.string()
                val updateDetails = Gson().fromJson(jsonResponse, UpdateDetails::class.java)

                // Ensure UI updates are done on the main thread
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
