package com.gmessenger.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.webkit.*
import androidx.core.app.ActivityCompat
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var web: WebView
    private var uploadCallback: ValueCallback<Array<Uri>>? = null
    private val chooserCode = 9101
    private val gemmaModelCode = 9201
    private var pendingMediaRequest: PermissionRequest? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var llm: LlmInference? = null
    private var session: LlmInferenceSession? = null
    private var gemmaModelFile: File? = null
    private var pushToken: String? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gemmaModelFile = File(filesDir, "models/gemma.task")
        initGemmaIfPresent()
        firebaseReady = initFirebase()
        if (firebaseReady) {
            firebaseAuth = FirebaseAuth.getInstance()
            initFirebaseMessaging()
        }
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 305)
        }
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
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
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


    private fun initFirebase(): Boolean {
        return try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this, FirebaseOptions.Builder()
                    .setApiKey("AIzaSyBq5M30XgpNWpthnh1PKA8gf7C5JohGrno")
                    .setApplicationId("1:812903668410:android:4a592af89b4281d8720083")
                    .setProjectId("g-messenger-10d39")
                    .setGcmSenderId("812903668410")
                    .setStorageBucket("g-messenger-10d39.firebasestorage.app")
                    .build())
            }
            FirebaseApp.getApps(this).isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private fun initFirebaseMessaging() {
        if (!firebaseReady) return
        try {
            loadPushToken()
        } catch (_: Exception) { }
    }

    private fun loadPushToken() {
        pushToken = getSharedPreferences("gm_push", MODE_PRIVATE).getString("token", null)
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                pushToken = task.result
                getSharedPreferences("gm_push", MODE_PRIVATE).edit().putString("token", pushToken).apply()
                if (::web.isInitialized) web.evaluateJavascript("window.onPushTokenReady && window.onPushTokenReady()", null)
            }
        }
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

    private fun initGemmaIfPresent() {
        val file = gemmaModelFile ?: return
        if (!file.exists() || file.length() < 1024) return
        try {
            val opts = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(file.absolutePath)
                .setMaxTokens(1024)
                .build()
            llm = LlmInference.createFromOptions(this, opts)
            session = LlmInferenceSession.createFromOptions(
                llm!!,
                LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(64)
                    .setTopP(0.95f)
                    .setTemperature(0.7f)
                    .build()
            )
        } catch (_: Exception) {
            llm = null
            session = null
        }
    }

    private fun formatGemmaPrompt(prompt: String): String {
        // Gemma 3 instruction-tuned models expect explicit user/model turn markers.
        // Keep the system guidance inside the user turn because Gemma IT does not
        // support a separate system role.
        return "<start_of_turn>user\n" +
            "You are Gemma AI inside G Messenger. Answer the user's request helpfully, clearly, and directly. " +
            "Do not talk about being unable to answer unless the request truly cannot be answered.\n\n" +
            prompt.trim() +
            "<end_of_turn>\n<start_of_turn>model\n"
    }

    private fun cleanGemmaResult(raw: String): String {
        var result = raw
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\r\n", "\n")
            .trim()
        result = result.removePrefix("<start_of_turn>model")
            .removePrefix("<start_of_turn>model\n")
            .removeSuffix("<end_of_turn>")
            .trim()
        return if (result.isBlank()) "I couldn't generate a response. Please try again." else result
    }

    private fun generateGemma(prompt: String, callbackId: String) {
        val active = session
        if (active == null) {
            web.evaluateJavascript("window.onGemmaResult && window.onGemmaResult(${JSONObject.quote(callbackId)}, ${JSONObject.quote("MODEL_NOT_READY")})", null)
            return
        }
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) {
            web.evaluateJavascript("window.onGemmaResult && window.onGemmaResult(${JSONObject.quote(callbackId)}, ${JSONObject.quote("EMPTY_PROMPT")})", null)
            return
        }
        executor.execute {
            val result = try {
                active.addQueryChunk(formatGemmaPrompt(cleanPrompt))
                cleanGemmaResult(active.generateResponse())
            } catch (e: Exception) {
                "ERROR: ${e.message ?: "Gemma inference failed"}"
            }
            runOnUiThread {
                web.evaluateJavascript("window.onGemmaResult && window.onGemmaResult(${JSONObject.quote(callbackId)}, ${JSONObject.quote(result)})", null)
            }
        }
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
        @JavascriptInterface fun dial(number: String) { runOnUiThread { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number)))) } }
        @JavascriptInterface fun call(number: String) {
            if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) { ask(arrayOf(Manifest.permission.CALL_PHONE), 304); return }
            runOnUiThread { startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(number)))) }
        }
        @JavascriptInterface fun sms(number: String, body: String) { runOnUiThread { startActivity(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("smsto:" + Uri.encode(number)); putExtra("sms_body", body) }) } }
        @JavascriptInterface fun openAndroidSettings() { runOnUiThread { startActivity(Intent(Settings.ACTION_SETTINGS)) } }
        @JavascriptInterface fun version() = "27.0.0"
        @JavascriptInterface fun getPushToken(): String = pushToken ?: ""

        @JavascriptInterface
        fun signInWithGoogle() {
            lifecycleScope.launch {
                try {
                    val credentialManager = androidx.credentials.CredentialManager.create(this@MainActivity)
                    val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                        .setServerClientId(getString(com.gmessenger.app.R.string.default_web_client_id))
                        .setFilterByAuthorizedAccounts(false)
                        .build()
                    val request = androidx.credentials.GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    val result = credentialManager.getCredential(this@MainActivity, request)
                    val credential = result.credential
                    if (credential.type != com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        throw IllegalStateException("Unsupported Google credential")
                    }
                    val googleCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                    val auth = firebaseAuth ?: FirebaseAuth.getInstance().also { firebaseAuth = it }
                    auth.signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            sendGoogleError(task.exception?.message ?: "Google sign-in failed")
                            return@addOnCompleteListener
                        }
                        val user = auth.currentUser ?: run {
                            sendGoogleError("Google sign-in returned no user")
                            return@addOnCompleteListener
                        }
                        user.getIdToken(true)
                            .addOnSuccessListener { tokenResult ->
                                val token = tokenResult.token
                                if (token.isNullOrBlank()) {
                                    sendGoogleError("Could not obtain secure authentication token")
                                    return@addOnSuccessListener
                                }
                                runOnUiThread {
                                    web.evaluateJavascript(
                                        "window.onGoogleAuthSuccess && window.onGoogleAuthSuccess(${JSONObject.quote(token)},${JSONObject.quote(user.email ?: "")},${JSONObject.quote(user.uid)},${JSONObject.quote(user.displayName ?: "")})",
                                        null
                                    )
                                }
                            }
                            .addOnFailureListener { e -> sendGoogleError(e.message ?: "Could not obtain secure authentication token") }
                    }
                } catch (e: Exception) {
                    sendGoogleError(e.message ?: "Google sign-in was cancelled or failed")
                }
            }
        }

        private fun sendGoogleError(message: String) {
            runOnUiThread {
                web.evaluateJavascript("window.onGoogleAuthError && window.onGoogleAuthError(${JSONObject.quote(message)})", null)
            }
        }

        @JavascriptInterface fun gemmaAvailable() = (session != null).toString()
        @JavascriptInterface fun pickGemmaModel() { runOnUiThread { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/octet-stream" }, gemmaModelCode) } }
        @JavascriptInterface fun generateGemma(prompt: String, callbackId: String) = this@MainActivity.generateGemma(prompt, callbackId)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == chooserCode) {
            uploadCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data)); uploadCallback = null
        } else if (requestCode == gemmaModelCode && resultCode == RESULT_OK && data?.data != null) {
            try {
                val dir = File(filesDir, "models").apply { mkdirs() }
                val target = File(dir, "gemma.task")
                contentResolver.openInputStream(data.data!!).use { input -> FileOutputStream(target).use { output -> input?.copyTo(output) } }
                gemmaModelFile = target
                llm?.close(); llm = null; session = null
                initGemmaIfPresent()
                web.evaluateJavascript("window.onGemmaModelChanged && window.onGemmaModelChanged(${JSONObject.quote((session != null).toString())})", null)
            } catch (_: Exception) {
                web.evaluateJavascript("window.onGemmaModelChanged && window.onGemmaModelChanged('false')", null)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if (requestCode == 303 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) web.evaluateJavascript("window.onAndroidContactsPermissionGranted && window.onAndroidContactsPermissionGranted()", null)
        if (requestCode == 300 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) { pendingMediaRequest?.grant(pendingMediaRequest?.resources ?: emptyArray()); pendingMediaRequest = null }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        session?.close(); llm?.close()
        super.onDestroy()
    }

    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
}
