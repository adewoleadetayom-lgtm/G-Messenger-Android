package com.gmessenger.v16.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel

private val Blue = Color(0xFF075FDB)
private val Purple = Color(0xFF9C43E8)
private val LightBg = Color(0xFFFBFBFB)
private val DarkBg = Color(0xFF101114)

sealed class Screen(val key: String) {
    data object Chats: Screen("chats")
    data object Updates: Screen("updates")
    data object Communities: Screen("communities")
    data object Calls: Screen("calls")
    data object Settings: Screen("settings")
    data object Profile: Screen("profile")
    data object NewContact: Screen("new_contact")
    data object NewGroup: Screen("new_group")
    data object Gemma: Screen("gemma")
    data object AddStatus: Screen("add_status")
    data class Chat(val name: String): Screen("chat:$name")
}

data class ChatItem(val name:String, val preview:String, val time:String, val unread:Int=0, val group:Boolean=false)
data class Message(val text:String, val mine:Boolean, val time:String)
data class StatusItem(val name:String, val age:String)
data class Community(val name:String, val preview:String, val time:String, val unread:Int=0)

class GMessengerViewModel: ViewModel() {
    var dark by mutableStateOf(false); private set
    var screen by mutableStateOf<Screen>(Screen.Chats); private set
    var search by mutableStateOf("")
    var toast by mutableStateOf<String?>(null)
    val chats = mutableStateListOf(
        ChatItem("Tola Daramola","Hey! How are you?","9:30 AM",2),
        ChatItem("Adenike","Thanks so much! 🙏","9:12 AM",1),
        ChatItem("Family Group","Mom: Dinner is ready 🍲","8:45 AM",5,true),
        ChatItem("Prayer Warriors","Good morning everyone","7:50 AM"),
        ChatItem("Project Team","You: Please check the update","Yesterday"),
        ChatItem("G Messenger Team","Welcome to G Messenger!","Yesterday")
    )
    val messages = mutableStateMapOf<String, MutableList<Message>>()
    val statuses = mutableStateListOf(StatusItem("Adenike","2 minutes ago"),StatusItem("Tola Daramola","10 minutes ago"),StatusItem("Bola","1 hour ago"),StatusItem("Prayer Warriors","2 hours ago"))
    val communities = mutableStateListOf(Community("My Community","Announcements • Welcome to our community!","9:20 AM",1),Community("General Chat","Tola: Good morning all","9:18 AM",3),Community("Project Community","Announcements • Project started","Yesterday"),Community("Developers","You: Code pushed","Yesterday"))

    fun toggleDark(){ dark=!dark; notice(if(dark) "Dark mode on" else "Light mode on") }
    fun go(s:Screen){ screen=s; search="" }
    fun notice(s:String){ toast=s }
    fun openChat(name:String){ if(!messages.containsKey(name)) messages[name]=mutableListOf(Message("Hi there!","false"=="true","9:28 AM"),Message("How are you doing today?",false,"9:29 AM"),Message("I'm good, thanks!",true,"9:29 AM"),Message("What about you?",false,"9:30 AM")); go(Screen.Chat(name)) }
    fun send(name:String,text:String){ if(text.isBlank()) return; val list=messages.getOrPut(name){mutableListOf()}; list.add(Message(text,true,"now")); chats.find{it.name==name}?.let{ chats[chats.indexOf(it)]=it.copy(preview=text,time="now") }; notice("Message sent") }
    fun addContact(name:String, phone:String){ if(name.isNotBlank()) { chats.add(0,ChatItem(name,"New contact","now")); notice("Contact saved") } }
    fun addGroup(name:String){ if(name.isNotBlank()) { chats.add(0,ChatItem(name,"Group created","now",group=true)); notice("Group created") } }
    fun addStatus(name:String){ statuses.add(0,StatusItem(name,"just now")); notice("Status posted") }
    fun addCommunity(name:String){ if(name.isNotBlank()){ communities.add(0,Community(name,"Announcements • Welcome!","now")); notice("Community created") } }
    fun ai(prompt:String):String = when {
        prompt.contains("summar",true) -> "G Messenger is a modern messaging app with chats, updates, communities, calls and an on-device AI assistant."
        prompt.contains("explain",true) -> "This feature breaks a topic into simple points so it is easier to understand and act on."
        prompt.contains("rewrite",true) -> "Here is a clearer version: G Messenger keeps conversations organized while making common actions quick and simple."
        prompt.contains("translat",true) -> "Translation mode is ready. Enter text and I’ll provide a concise translated version."
        else -> "I’m Gemma, your on-device assistant. I can summarize, explain, rewrite, or translate text without requiring a network connection."
    }
}

@Composable fun GMessengerApp(vm:GMessengerViewModel){
    val bg=if(vm.dark) DarkBg else LightBg; val fg=if(vm.dark) Color.White else Color(0xFF111111)
    MaterialTheme(colorScheme=if(vm.dark) darkColorScheme(primary=Blue,background=bg,surface=bg,onBackground=fg) else lightColorScheme(primary=Blue,background=bg,surface=bg,onBackground=fg)){
        Surface(Modifier.fillMaxSize(),color=bg){
            when(val s=vm.screen){
                Screen.Chats -> Home(vm,0){vm.go(it)}
                Screen.Updates -> Home(vm,1){vm.go(it)}
                Screen.Communities -> Home(vm,2){vm.go(it)}
                Screen.Calls -> Home(vm,3){vm.go(it)}
                Screen.Settings -> Settings(vm)
                Screen.Profile -> Profile(vm)
                Screen.NewContact -> NewContact(vm)
                Screen.NewGroup -> NewGroup(vm)
                Screen.Gemma -> Gemma(vm)
                Screen.AddStatus -> AddStatus(vm)
                is Screen.Chat -> ChatScreen(vm,s.name)
            }
        }
        vm.toast?.let{ msg -> LaunchedEffect(msg){ kotlinx.coroutines.delay(1300); vm.toast=null }; SnackbarHost(remember{SnackbarHostState()}) }
    }
}

@Composable fun Logo(size:Int=44){ Box(Modifier.size(size.dp).clip(CircleShape).background(Blue),contentAlignment=Alignment.Center){Text("G",color=Color.White,fontSize=(size*0.58).sp,fontWeight=FontWeight.Bold)} }
@Composable fun Avatar(name:String,size:Int=42){ Box(Modifier.size(size.dp).clip(CircleShape).background(Color(0xFFE6E9EF)),contentAlignment=Alignment.Center){Text(name.firstOrNull()?.uppercase()?:"G",color=Blue,fontWeight=FontWeight.Bold,fontSize=(size/2.2).sp)} }

@Composable fun BottomNav(selected:Int,onSelect:(Int)->Unit){ NavigationBar(containerColor=Color.Transparent,tonalElevation=0.dp){ listOf("Chats" to Icons.Default.ChatBubble,"Updates" to Icons.Default.Update,"Communities" to Icons.Default.Groups,"Calls" to Icons.Default.Call).forEachIndexed{ i,(t,ic)->NavigationBarItem(selected==i,onClick={onSelect(i)},icon={Icon(ic,t)},label={Text(t,fontSize=11.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Blue,selectedTextColor=Blue,indicatorColor=Color.Transparent)) } } }

@Composable fun Home(vm:GMessengerViewModel,selected:Int,onRoute:(Screen)->Unit){
    val tabs=listOf(Screen.Chats,Screen.Updates,Screen.Communities,Screen.Calls)
    Scaffold(bottomBar={BottomNav(selected){onRoute(tabs[it])}},containerColor=MaterialTheme.colorScheme.background){ p->
        when(selected){0->Chats(vm,onRoute);1->Updates(vm,onRoute);2->Communities(vm,onRoute);3->Calls(vm,onRoute)}
    }
}

@Composable fun Header(title:String,onMenu:()->Unit={}){ Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Text(title,color=Blue,fontSize=22.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));IconButton(onClick=onMenu){Icon(Icons.Default.MoreVert,"menu")}} }

@Composable fun Chats(vm:GMessengerViewModel,onRoute:(Screen)->Unit){
    Column(Modifier.fillMaxSize()){
        Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,top=12.dp),verticalAlignment=Alignment.CenterVertically){Text("G Messenger",color=Blue,fontSize=22.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));IconButton({onRoute(Screen.Gemma)}){Icon(Icons.Default.AutoAwesome,"Gemma AI")};IconButton({onRoute(Screen.NewContact)}){Icon(Icons.Default.PersonAdd,"new contact")};IconButton({vm.notice("More options")}){Icon(Icons.Default.MoreVert,"menu")}}
        SearchBox(vm)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("All","Unread","Groups","Favorites").forEachIndexed{ i,t->FilterChip(i==0, { }, label={Text(t)} )};IconButton({onRoute(Screen.NewGroup)}){Icon(Icons.Default.AddCircleOutline,"add")}}
        val list=vm.chats.filter{it.name.contains(vm.search,true)||it.preview.contains(vm.search,true)}
        LazyColumn(Modifier.weight(1f)){items(list,key={it.name}){c->ChatRow(c){vm.openChat(c.name)}}}
    }
}
@Composable fun SearchBox(vm:GMessengerViewModel){ OutlinedTextField(value=vm.search,onValueChange={vm.search=it},modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp),singleLine=true,shape=RoundedCornerShape(26.dp),leadingIcon={Icon(Icons.Default.Search,"search")},trailingIcon={Icon(Icons.Default.AutoAwesome,color=Purple,contentDescription="Gemma")},placeholder={Text("Search or ask Gemma AI")},colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=Color.Transparent,focusedBorderColor=Color.Transparent,unfocusedContainerColor=Color(0xFFF1F1F3),focusedContainerColor=Color(0xFFF1F1F3)))}
@Composable fun ChatRow(c:ChatItem,onClick:()->Unit){Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=18.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Avatar(c.name);Column(Modifier.weight(1f).padding(start=14.dp)){Text(c.name,fontWeight=FontWeight.SemiBold,fontSize=16.sp);Text(c.preview,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,fontSize=14.sp)}Column(horizontalAlignment=Alignment.End){Text(c.time,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);if(c.unread>0)Badge{Text(c.unread.toString())}}}}

@Composable fun Updates(vm:GMessengerViewModel,onRoute:(Screen)->Unit){Column(Modifier.fillMaxSize()){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Text("Updates",color=Blue,fontSize=22.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));IconButton({onRoute(Screen.AddStatus)}){Icon(Icons.Default.Edit,"add status")};IconButton({vm.notice("Updates menu")}){Icon(Icons.Default.MoreVert,"menu")}};Text("Status",fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=18.dp));Row(Modifier.horizontalScroll(rememberScrollState()).padding(14.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)){Column(horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(58.dp).clip(CircleShape).background(Blue),contentAlignment=Alignment.Center){Icon(Icons.Default.Add,color=Color.White,contentDescription=null)};Text("My status",fontSize=12.sp)};vm.statuses.take(4).forEach{st->Column(horizontalAlignment=Alignment.CenterHorizontally){Avatar(st.name,58);Text(st.name,Modifier.width(70.dp),maxLines=1,fontSize=12.sp)}}};HorizontalDivider();Text("Recent updates",fontWeight=FontWeight.Bold,modifier=Modifier.padding(18.dp));LazyColumn{items(vm.statuses){st->Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){Avatar(st.name);Column(Modifier.padding(start=14.dp)){Text(st.name,fontWeight=FontWeight.Medium);Text(st.age,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}}

@Composable fun Communities(vm:GMessengerViewModel,onRoute:(Screen)->Unit){Column(Modifier.fillMaxSize()){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Text("Communities",color=Blue,fontSize=22.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));IconButton({onRoute(Screen.NewGroup)}){Icon(Icons.Default.Add,"new community")};IconButton({vm.notice("Communities menu")}){Icon(Icons.Default.MoreVert,"menu")}};Text("New community",color=Blue,fontWeight=FontWeight.Medium,modifier=Modifier.padding(horizontal=18.dp,vertical=8.dp).clickable{onRoute(Screen.NewGroup)});LazyColumn{items(vm.communities){c->Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=11.dp),verticalAlignment=Alignment.CenterVertically){Avatar(c.name);Column(Modifier.weight(1f).padding(start=14.dp)){Text(c.name,fontWeight=FontWeight.SemiBold);Text(c.preview,maxLines=1,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}Text(c.time,fontSize=12.sp)}}}}}

@Composable fun Calls(vm:GMessengerViewModel,onRoute:(Screen)->Unit){Column(Modifier.fillMaxSize()){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Text("Calls",color=Blue,fontSize=22.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));IconButton({vm.notice("Call keypad")}){Icon(Icons.Default.Dialpad,"keypad")};IconButton({vm.notice("New contact")}){Icon(Icons.Default.PersonAdd,"new contact")}};Row(Modifier.fillMaxWidth().padding(18.dp),horizontalArrangement=Arrangement.SpaceEvenly){listOf(Icons.Default.Call to "Call",Icons.Default.AddCall to "New call link",Icons.Default.PersonAdd to "New contact",Icons.Default.Dialpad to "Keypad").forEach{(ic,t)->Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.clickable{vm.notice(t)}){Icon(ic,contentDescription=t);Text(t,fontSize=11.sp)}}};Text("Recent",fontWeight=FontWeight.Bold,modifier=Modifier.padding(18.dp));vm.chats.take(4).forEach{c->Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){Avatar(c.name);Column(Modifier.weight(1f).padding(start=14.dp)){Text(c.name,fontWeight=FontWeight.Medium);Text("Today, ${c.time}",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton({vm.notice("Calling ${c.name}")}){Icon(Icons.Default.Call,"call")}}}}}

@Composable fun ChatScreen(vm:GMessengerViewModel,name:String){var text by remember{mutableStateOf("")};Column(Modifier.fillMaxSize()){Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){IconButton({vm.go(Screen.Chats)}){Icon(Icons.Default.ArrowBack,"back")};Avatar(name,38);Column(Modifier.padding(start=10.dp)){Text(name,fontWeight=FontWeight.SemiBold);Text("online",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Spacer(Modifier.weight(1f));IconButton({vm.notice("Video calling $name")}){Icon(Icons.Default.Videocam,"video")};IconButton({vm.notice("Calling $name")}){Icon(Icons.Default.Call,"call")};IconButton({vm.notice("Chat menu")}){Icon(Icons.Default.MoreVert,"menu")}};HorizontalDivider();LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(vm.messages[name]?:emptyList()){m->Row(Modifier.fillMaxWidth(),horizontalArrangement=if(m.mine)Arrangement.End else Arrangement.Start){Surface(shape=RoundedCornerShape(16.dp),color=if(m.mine)Color(0xFFDDF7C9) else MaterialTheme.colorScheme.surfaceVariant,modifier=Modifier.widthIn(max=290.dp)){Column(Modifier.padding(10.dp)){Text(m.text);Text(m.time,fontSize=10.sp,color=Color.Gray,modifier=Modifier.align(Alignment.End))}}}}};Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(text,{text=it},modifier=Modifier.weight(1f),placeholder={Text("Message")},singleLine=true,shape=RoundedCornerShape(25.dp),leadingIcon={Icon(Icons.Default.EmojiEmotions,"emoji")},trailingIcon={Row{Icon(Icons.Default.AttachFile,"attach");Icon(Icons.Default.CameraAlt,"camera")}});IconButton({vm.send(name,text);text=""}){Icon(Icons.Default.Send,color=Blue,contentDescription="send")}}}}

@Composable fun NewContact(vm:GMessengerViewModel){var name by remember{mutableStateOf("")};var phone by remember{mutableStateOf("")};FormScaffold(vm,"New contact",Icons.Default.PersonAdd){OutlinedTextField(name,{name=it},label={Text("Full name")},modifier=Modifier.fillMaxWidth());OutlinedTextField(phone,{phone=it},label={Text("Phone number")},modifier=Modifier.fillMaxWidth());OutlinedTextField("",{},label={Text("G Messenger ID (optional)")},modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(20.dp));Button(onClick={vm.addContact(name,phone);vm.go(Screen.Chats)},modifier=Modifier.align(Alignment.End)){Text("SAVE")}}}
@Composable fun NewGroup(vm:GMessengerViewModel){var name by remember{mutableStateOf("")};FormScaffold(vm,"New group",Icons.Default.Group){Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){Box(Modifier.size(86.dp).clip(CircleShape).background(Blue),contentAlignment=Alignment.Center){Icon(Icons.Default.Groups,color=Color.White,modifier=Modifier.size(42.dp),contentDescription=null)}};Spacer(Modifier.height(22.dp));OutlinedTextField(name,{name=it},label={Text("Group name")},placeholder={Text("Enter group name")},modifier=Modifier.fillMaxWidth());OutlinedTextField("",{},label={Text("Add description (optional)")},modifier=Modifier.fillMaxWidth());Text("Participants: 0",modifier=Modifier.padding(top=18.dp));Text("ADD PARTICIPANTS",color=Blue,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=18.dp).clickable{vm.notice("Participant picker opened")});Button({vm.addGroup(name);vm.go(Screen.Chats)},modifier=Modifier.align(Alignment.End)){Text("CREATE")}}}

@Composable fun Gemma(vm:GMessengerViewModel){var input by remember{mutableStateOf("")};var answer by remember{mutableStateOf("Hello! I'm Gemma, your on-device AI assistant.\n\nHow can I help you today?")};Column(Modifier.fillMaxSize()){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){IconButton({vm.go(Screen.Chats)}){Icon(Icons.Default.ArrowBack,"back")};Column{Text("Gemma AI",fontWeight=FontWeight.Bold,fontSize=20.sp);Text("on-device",fontSize=12.sp)};Spacer(Modifier.weight(1f));Icon(Icons.Default.AutoAwesome,color=Purple,contentDescription=null);IconButton({vm.notice("Gemma menu")}){Icon(Icons.Default.MoreVert,"menu")}};LazyColumn(Modifier.weight(1f).padding(horizontal=14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{AiBubble(answer,false)};item{Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Summarize","Explain","Rewrite","Translate").forEach{a->AssistChip(onClick={answer=vm.ai(a);},label={Text(a,fontSize=12.sp)})}}};item{if(input.isNotBlank())AiBubble(input,true)}};Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(input,{input=it},modifier=Modifier.weight(1f),singleLine=true,shape=RoundedCornerShape(24.dp),placeholder={Text("Ask Gemma AI...")});IconButton({answer=vm.ai(input);input=""}){Icon(Icons.Default.Send,color=Blue,contentDescription="send")}}}}
@Composable fun AiBubble(t:String,mine:Boolean){Row(Modifier.fillMaxWidth(),horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start){Surface(shape=RoundedCornerShape(16.dp),color=if(mine)Blue.copy(alpha=.12f) else MaterialTheme.colorScheme.surfaceVariant,modifier=Modifier.widthIn(max=310.dp)){Text(t,Modifier.padding(12.dp))}}}

@Composable fun AddStatus(vm:GMessengerViewModel){var text by remember{mutableStateOf("")};FormScaffold(vm,"Add status",Icons.Default.Edit){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){listOf(Icons.Default.TextFields to "Text",Icons.Default.MusicNote to "Music",Icons.Default.GridView to "Layout",Icons.Default.Mic to "Voice").forEach{(i,t)->Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(i,contentDescription=t);Text(t,fontSize=12.sp)}}};Spacer(Modifier.height(20.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){FilledTonalIconButton({vm.notice("Camera opened")},modifier=Modifier.size(92.dp)){Icon(Icons.Default.CameraAlt,contentDescription="Camera")};FilledTonalIconButton({vm.notice("Gallery opened")},modifier=Modifier.size(92.dp)){Icon(Icons.Default.Image,contentDescription="Gallery")}};Spacer(Modifier.height(18.dp));OutlinedTextField(text,{text=it},modifier=Modifier.fillMaxWidth(),label={Text("Status text")});Button({vm.addStatus(if(text.isBlank())"My status":text);vm.go(Screen.Updates)},modifier=Modifier.align(Alignment.End)){Text("POST")}}}

@Composable fun Settings(vm:GMessengerViewModel){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Text("Settings",color=Blue,fontSize=22.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Icon(Icons.Default.Search,"search")};Row(Modifier.padding(horizontal=18.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Avatar("Goodluck Arowolo",50);Column(Modifier.weight(1f).padding(start=14.dp)){Text("Goodluck Arowolo",fontWeight=FontWeight.SemiBold);Text("Hey there! I am using\nG Messenger.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Default.QrCode2,"qr")};HorizontalDivider(Modifier.padding(vertical=8.dp));SettingRow("Account","Security notifications, change number",Icons.Default.Person){vm.notice("Account settings")};SettingRow("Privacy","Block contacts, disappearing messages",Icons.Default.Lock){vm.notice("Privacy settings")};SettingRow("Chats","Theme, wallpapers, chat history",Icons.Default.Chat){vm.notice("Chat settings")};SettingRow("Notifications","Message, group & call tones",Icons.Default.Notifications){vm.notice("Notification settings")};SettingRow("Storage and data","Network usage, auto-download",Icons.Default.DataUsage){vm.notice("Storage settings")};SettingRow("Help","Help center, contact us",Icons.Default.HelpOutline){vm.notice("Help center")};SettingRow("Invite a friend","",Icons.Default.PersonAdd){vm.notice("Invite link copied")};SettingRow(if(vm.dark)"Light mode" else "Dark mode","Theme",if(vm.dark)Icons.Default.LightMode else Icons.Default.DarkMode){vm.toggleDark()};Spacer(Modifier.height(20.dp));Text("from",modifier=Modifier.align(Alignment.CenterHorizontally),fontSize=12.sp);Text("G Messenger",color=Blue,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.CenterHorizontally));Text("Version 16.0.0",fontSize=11.sp,color=Color.Gray,modifier=Modifier.align(Alignment.CenterHorizontally).padding(bottom=20.dp));}}
@Composable fun SettingRow(t:String,s:String,ic:ImageVector,onClick:()->Unit){Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=18.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Icon(ic,null,modifier=Modifier.size(22.dp));Column(Modifier.padding(start=18.dp)){Text(t,fontWeight=FontWeight.Medium);if(s.isNotBlank())Text(s,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
@Composable fun Profile(vm:GMessengerViewModel){FormScaffold(vm,"Profile",Icons.Default.ArrowBack){Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){Avatar("Goodluck Arowolo",128)};Spacer(Modifier.height(20.dp));Text("Name",color=Color.Gray);Text("Goodluck Arowolo",fontSize=18.sp,fontWeight=FontWeight.Medium);HorizontalDivider(Modifier.padding(vertical=12.dp));Text("About",color=Color.Gray);Text("Hey there! I am using\nG Messenger.",fontSize=17.sp);HorizontalDivider(Modifier.padding(vertical=12.dp));Text("G Messenger ID",color=Color.Gray);Row(verticalAlignment=Alignment.CenterVertically){Text("gm_goodluck",fontSize=17.sp);Spacer(Modifier.weight(1f));IconButton({vm.notice("G Messenger ID copied")}){Icon(Icons.Default.ContentCopy,"copy")}}}}
@Composable fun FormScaffold(vm:GMessengerViewModel,title:String,icon:ImageVector,content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){IconButton({vm.go(Screen.Chats)}){Icon(icon,"back")};Text(title,color=Blue,fontSize=21.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));IconButton({vm.notice("More options")}){Icon(Icons.Default.MoreVert,"menu")}};Spacer(Modifier.height(8.dp));content()}}
