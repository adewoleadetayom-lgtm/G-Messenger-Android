package com.gmessenger.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.webkit.*
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var uploadCallback: ValueCallback<Array<Uri>>? = null
    private val chooserCode = 9101
    private var pendingMediaRequest: PermissionRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
            javaScriptCanOpenWindowsAutomatically = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            textZoom = 100
            loadWithOverviewMode = false
            useWideViewPort = false
        }
        web.webViewClient = WebViewClient()
        web.setBackgroundColor(android.graphics.Color.WHITE)
        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.WHITE
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        web.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                val wantsAudio = request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                val wantsVideo = request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                val perms = mutableListOf<String>()
                if (wantsAudio && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) perms += Manifest.permission.RECORD_AUDIO
                if (wantsVideo && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) perms += Manifest.permission.CAMERA
                if (perms.isEmpty()) request.grant(request.resources) else {
                    pendingMediaRequest = request
                    ActivityCompat.requestPermissions(this@MainActivity, perms.toTypedArray(), 300)
                }
            }
            override fun onShowFileChooser(view: WebView?, callback: ValueCallback<Array<Uri>>?, params: FileChooserParams?): Boolean {
                uploadCallback?.onReceiveValue(null)
                uploadCallback = callback
                val intent = params?.createIntent() ?: Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                startActivityForResult(intent, chooserCode)
                return true
            }
        }
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("file:///android_asset/index.html")
        setContentView(web)
    }

    private fun ask(perms: Array<String>, code: Int) {
        val missing = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) ActivityCompat.requestPermissions(this, missing.toTypedArray(), code)
    }

    private fun readContacts(): String {
        val out = JSONArray()
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER)
        contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")?.use { c ->
            while (c.moveToNext()) {
                val o = JSONObject()
                o.put("name", c.getString(0) ?: "Unknown")
                o.put("phone", c.getString(1) ?: "")
                out.put(o)
            }
        }
        return out.toString()
    }

    inner class Bridge {
        @JavascriptInterface fun requestMicrophone() = ask(arrayOf(Manifest.permission.RECORD_AUDIO), 301)
        @JavascriptInterface fun requestCamera() = ask(arrayOf(Manifest.permission.CAMERA), 302)
        @JavascriptInterface fun requestContacts(): String {
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                ask(arrayOf(Manifest.permission.READ_CONTACTS), 303); return "[]"
            }
            return readContacts()
        }
        @JavascriptInterface fun refreshContacts(): String = if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) readContacts() else "[]"
        @JavascriptInterface fun dial(number: String) {
            runOnUiThread { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number)))) }
        }
        @JavascriptInterface fun call(number: String) {
            if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) { ask(arrayOf(Manifest.permission.CALL_PHONE), 304); return }
            runOnUiThread { startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(number)))) }
        }
        @JavascriptInterface fun sms(number: String, body: String) {
            runOnUiThread {
                startActivity(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("smsto:" + Uri.encode(number)); putExtra("sms_body", body) })
            }
        }
        @JavascriptInterface fun openAndroidSettings() { runOnUiThread { startActivity(Intent(Settings.ACTION_SETTINGS)) } }
        @JavascriptInterface fun version() = "18.0.0"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == chooserCode) {
            uploadCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data)); uploadCallback = null
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 303 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) web.evaluateJavascript("window.onAndroidContactsPermissionGranted && window.onAndroidContactsPermissionGranted()", null)
        if (requestCode == 300 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) { pendingMediaRequest?.grant(pendingMediaRequest?.resources ?: emptyArray()); pendingMediaRequest = null }
    }

    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
}
