package com.theokanning.emojikeyboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.theokanning.emojikeyboard.emoji.EmojiService
import com.wonderkiln.camerakit.CameraListener
import com.wonderkiln.camerakit.CameraView
import javax.inject.Inject


class KeyboardService : InputMethodService() {

    @Inject
    lateinit var emojiService: EmojiService

    private lateinit var cameraView: CameraView
    private lateinit var cameraLayout: View
    private lateinit var permissionsLayout: View

    private var checkedForPermission = false

    override fun onCreateInputView(): View {
        Log.e(TAG, "OnCreateInputView")
        val view = layoutInflater.inflate(R.layout.keyboard, null)

        (application as KeyboardApplication).getComponent().inject(this)

        cameraView = view.findViewById(R.id.camera_view)
        cameraLayout = view.findViewById(R.id.camera_layout)
        permissionsLayout = view.findViewById(R.id.permission_rationale_layout)

        val button: View = view.findViewById(R.id.take_picture)
        button.setOnClickListener {
            cameraView.captureImage()
        }

        val settingsButton: View = view.findViewById(R.id.go_to_settings)
        settingsButton.setOnClickListener { goToSettings() }

        return view
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.e(TAG, "onStartInputView: restarting=$restarting")

        if (!hasCameraPermission() && !checkedForPermission) {
            checkedForPermission = true
            val intent = Intent(applicationContext, PermissionActivity::class.java)
            startActivity(intent)
        } else if (!hasCameraPermission()) {
            showPermissionsMessage()
        } else {
            showCamera()
            startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "OnDestroy")
        Log.d(TAG, "Stopping Camera")
        cameraView.stop()
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun showCamera() {
        cameraLayout.visibility = View.VISIBLE
        permissionsLayout.visibility = View.GONE
    }

    private fun showPermissionsMessage() {
        cameraLayout.visibility = View.GONE
        permissionsLayout.visibility = View.VISIBLE
    }

    private fun startCamera() {
        Log.d(TAG, "Starting Camera")
        cameraView.start()

        cameraView.setCameraListener(object : CameraListener() {
            override fun onPictureTaken(jpeg: ByteArray?) {
                val encodedString = Base64.encodeToString(jpeg, Base64.DEFAULT)
                emojiService.getEmojiForImage(encodedString, { onEmojiReceived(it) })
            }
        })
    }

    private fun onEmojiReceived(emoji: String?) {
        if (emoji != null) {
            currentInputConnection.commitText(emoji, 1)
        } else {
            Toast.makeText(this@KeyboardService.applicationContext, "No matching emoji", Toast.LENGTH_LONG).show()
        }
    }

    private fun goToSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    companion object {
        private val TAG = KeyboardService::class.java.simpleName
    }
}