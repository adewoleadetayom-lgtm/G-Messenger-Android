package com.gmessenger.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val blue = Color.rgb(8,127,245)
    private val bg = Color.rgb(246,248,250)
    private val prefs by lazy { getSharedPreferences("gmessenger", MODE_PRIVATE) }
    private lateinit var content: LinearLayout
    private lateinit var title: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (prefs.getBoolean("registered", false)) showHome() else showRegister()
    }

    private fun base(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        title = TextView(this).apply {
            text = "G Messenger"
            textSize = 23f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 0, 12, 0)
            setBackgroundColor(blue)
        }
        root.addView(title, LinearLayout.LayoutParams(-1, 64))
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16,16,16,8)
        }
        root.addView(content, LinearLayout.LayoutParams(-1,0,1f))
        return root
    }

    private fun showRegister() {
        val root = base()
        title.text = "Welcome to G Messenger"
        val name = EditText(this).apply { hint = "Enter your name"; textSize=17f }
        val phone = EditText(this).apply { hint = "Phone number"; inputType=3; textSize=17f }
        val button = Button(this).apply { text="Continue"; setTextColor(Color.WHITE); setBackgroundColor(blue) }
        content.addView(TextView(this).apply { text="Create your account"; textSize=28f; setTextColor(Color.DKGRAY) })
        content.addView(name, LinearLayout.LayoutParams(-1,60))
        content.addView(phone, LinearLayout.LayoutParams(-1,60))
        content.addView(button, LinearLayout.LayoutParams(-1,58))
        button.setOnClickListener {
            if (name.text.toString().trim().isNotEmpty() && phone.text.toString().trim().isNotEmpty()) {
                prefs.edit().putBoolean("registered",true).putString("name",name.text.toString()).putString("phone",phone.text.toString()).apply()
                showHome()
            } else Toast.makeText(this,"Enter your name and phone number",Toast.LENGTH_SHORT).show()
        }
        setContentView(root)
    }

    private fun showHome() {
        val root = base()
        title.text = "G Messenger"
        val tabs = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        val pages = listOf("Chats","Updates","Communities","Calls")
        pages.forEach { p ->
            val b=Button(this).apply { text=p; setOnClickListener { showPage(p) } }
            tabs.addView(b, LinearLayout.LayoutParams(0,55,1f))
        }
        content.addView(tabs)
        showPage("Chats")
        setContentView(root)
    }

    private fun showPage(page:String) {
        content.removeViews(1, maxOf(0,content.childCount-1))
        title.text = page
        when(page) {
            "Chats" -> chatPage()
            "Updates" -> updatesPage()
            "Communities" -> communityPage()
            "Calls" -> callsPage()
        }
    }

    private fun chatPage() {
        addHeading("Chats")
        addAction("＋ New chat") { requestContacts() }
        addAction("📎 Send attachment") { pickFile() }
        val input=EditText(this).apply { hint="Message"; textSize=16f }
        val send=Button(this).apply { text="Send"; setOnClickListener {
            if(input.text.isNotEmpty()) { Toast.makeText(this@MainActivity,"Message sent",Toast.LENGTH_SHORT).show(); input.text.clear() }
        }}
        content.addView(input)
        content.addView(send)
        addAction("🤖 Gemma AI") { gemma() }
    }

    private fun updatesPage() {
        addHeading("Updates")
        addAction("＋ Add status") { pickImage() }
        addAction("📷 Camera") { pickImage() }
        addAction("🎙 Voice status") { requestAudio() }
        addInfo("Your status will appear here after you post it.")
    }

    private fun communityPage() {
        addHeading("Communities")
        addAction("＋ New community") { dialog("Create community") }
        addAction("＋ New group") { dialog("Create group") }
        addInfo("Bring your groups and conversations together.")
    }

    private fun callsPage() {
        addHeading("Calls")
        addAction("📞 Voice call") { requestAudio(); Toast.makeText(this,"Microphone permission requested",Toast.LENGTH_SHORT).show() }
        addAction("🎥 Video call") { requestCamera(); Toast.makeText(this,"Camera and microphone permission requested",Toast.LENGTH_SHORT).show() }
        addInfo("Call history will appear here.")
    }

    private fun gemma() {
        val e=EditText(this).apply { hint="Message Gemma AI…" }
        AlertDialogBuilder("Gemma AI",e,"Send") {
            val q=e.text.toString().trim()
            Toast.makeText(this,"Gemma AI: I received your message: $q",Toast.LENGTH_LONG).show()
        }
    }

    private fun addHeading(s:String) {
        content.addView(TextView(this).apply { text=s; textSize=24f; setTextColor(Color.DKGRAY); setPadding(0,12,0,12) })
    }
    private fun addInfo(s:String) {
        content.addView(TextView(this).apply { text=s; textSize=15f; setTextColor(Color.GRAY); setPadding(0,12,0,12) })
    }
    private fun addAction(s:String, f:()->Unit) {
        content.addView(Button(this).apply { text=s; setOnClickListener { f() } })
    }
    private fun dialog(kind:String) {
        val e=EditText(this).apply { hint="$kind name" }
        AlertDialogBuilder(kind,e,"Create") { Toast.makeText(this,"$kind created: ${e.text}",Toast.LENGTH_SHORT).show() }
    }
    private fun AlertDialogBuilder(t:String,e:EditText,b:String,done:()->Unit) {
        android.app.AlertDialog.Builder(this).setTitle(t).setView(e).setPositiveButton(b) { _,_->done() }.setNegativeButton("Cancel",null).show()
    }
    private fun requestContacts() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_CONTACTS),10)
        else loadContacts()
    }
    private fun loadContacts() {
        val c=contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,null,null,null)
        var count=0; c?.use { count=it.count }
        Toast.makeText(this,"Contacts available: $count",Toast.LENGTH_SHORT).show()
    }
    private fun requestAudio() { ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO),11) }
    private fun requestCamera() { ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO),12) }
    private fun pickImage() { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="image/*"; addCategory(Intent.CATEGORY_OPENABLE) },20) }
    private fun pickFile() { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="*/*"; addCategory(Intent.CATEGORY_OPENABLE) },21) }
}
