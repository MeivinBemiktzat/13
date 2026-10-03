package com.ailocal.app
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity:ComponentActivity(){
 override fun onCreate(state:Bundle?){super.onCreate(state);setContent{App()}}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun App(){
 val context=LocalContext.current;val scope=rememberCoroutineScope();val store=remember{LocalStore(context)};val mm=remember{ModelManager(context)};val tm=remember{ToolManager(context)}
 var session by remember{mutableStateOf(store.all().firstOrNull())};var sessions by remember{mutableStateOf(store.all())};var text by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};var tab by remember{mutableIntStateOf(0)};var settings by remember{mutableStateOf(LlmSettings())};var confirm by remember{mutableStateOf<ActionCall?>(null)};var status by remember{mutableStateOf("מוכן")}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){u->u?.let{scope.launch{status="טוען מודל...";mm.load(it,settings).onSuccess{status=it}.onFailure{status="שגיאה: "+it.message}}}}
 val mic=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){}
 MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF2457D6))){
  Scaffold(topBar={TopAppBar(title={Text("AI מקומי")},actions={Text(if(mm.name.isBlank())"ללא מודל" else mm.name);IconButton({}){Icon(Icons.Default.Settings,"הגדרות")}})},bottomBar={NavigationBar{
   val labels=listOf("צ'אט","שיחות","פעולות","מודלים","הגדרות");val icons=listOf(Icons.Default.Chat,Icons.Default.History,Icons.Default.Build,Icons.Default.Memory,Icons.Default.Settings)
   labels.forEachIndexed{i,l->NavigationBarItem(tab==i,{tab=i},icon={Icon(icons[i],l)},label={Text(l)})}
  }}){pad->Column(Modifier.padding(pad).fillMaxSize().background(Color(0xFFF7F8FA)).padding(16.dp)){
   when(tab){
    0->Chat(session,text,{text=it},{if(text.isNotBlank()&&!busy){val s=session?:ChatSession(UUID.randomUUID().toString(),text.take(30)).also{session=it};s.messages.add(ChatMessage("user",text));store.save(s);sessions=store.all();text="";busy=true;status="חושב...";scope.launch{mm.complete(s.messages.last().content,settings).onSuccess{r->val a=tm.parse(r.first);if(a==null)s.messages.add(ChatMessage("assistant",r.first))else if(tm.needsApproval(a))confirm=a else s.messages.add(ChatMessage("assistant",tm.execute(a)));store.save(s);sessions=store.all();status="מוכן"}.onFailure{s.messages.add(ChatMessage("assistant","שגיאה: "+it.message));store.save(s);status="שגיאה"};busy=false}}},busy,{busy=false;status="נעצר"},{if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)mic.launch(Manifest.permission.RECORD_AUDIO) else status="הקלטה מקומית מוכנה; בחר מודל Whisper במסך המודלים."})
    1->Sessions(sessions,{session=it;tab=0},{store.delete(it.id);sessions=store.all();session=sessions.firstOrNull()})
    2->Actions()
    3->Models(mm,{picker.launch(arrayOf("application/octet-stream","*/*"))},{mm.unload();status="המודל נפרק."})
    4->Settings(settings){settings=it}
   }
   Spacer(Modifier.height(6.dp));Text(status,style=MaterialTheme.typography.labelSmall)
  }}
  confirm?.let{a->AlertDialog(onDismissRequest={confirm=null},title={Text("נדרש אישור")},text={Text("הפעולה "+a.action+" דורשת אישור.")},confirmButton={TextButton({tm.execute(a);confirm=null}){Text("אישור")}},dismissButton={TextButton({confirm=null}){Text("ביטול")}})}
 }
}
@Composable fun Chat(s:ChatSession?,text:String,set:(String)->Unit,send:()->Unit,busy:Boolean,stop:()->Unit,mic:()->Unit){
 Column(Modifier.fillMaxSize()){LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(s?.messages?:emptyList()){m->Surface(tonalElevation=2.dp,modifier=Modifier.fillMaxWidth()){Text(m.content,Modifier.padding(12.dp))}}};Row(verticalAlignment=Alignment.CenterVertically){IconButton(mic){Icon(Icons.Default.Mic,"מיקרופון")};OutlinedTextField(text,set,Modifier.weight(1f),placeholder={Text("כתוב בקשה...")});IconButton(if(busy)stop else send){Icon(if(busy)Icons.Default.Stop else Icons.Default.Send,"שליחה")}}}
}
@Composable fun Sessions(list:List<ChatSession>,open:(ChatSession)->Unit,del:(ChatSession)->Unit){LazyColumn{items(list){s->ListItem(headlineContent={Text(s.title)},leadingContent={Icon(Icons.Default.History,"שיחה")},trailingContent={IconButton({del(s)}){Icon(Icons.Default.Delete,"מחיקה")}},modifier=Modifier.fillMaxWidth())}}}
@Composable fun Actions(){Column{Text("פעולות זמינות",style=MaterialTheme.typography.headlineSmall);listOf("פתיחת אפליקציה","מסכי Settings","מידע על סוללה","מידע על המכשיר","מידע על אחסון","שעה ותאריך").forEach{ListItem(headlineContent={Text(it)},leadingContent={Icon(Icons.Default.Build,"כלי")})}}}
@Composable fun Models(mm:ModelManager,pick:()->Unit,unload:()->Unit){Column{Text("מודלים",style=MaterialTheme.typography.headlineSmall);Text(if(mm.name.isBlank())"אין מודל פעיל" else "פעיל: "+mm.name);Spacer(Modifier.height(12.dp));Button(pick,Modifier.fillMaxWidth()){Icon(Icons.Default.FolderOpen,"בחירה");Spacer(Modifier.width(8.dp));Text("בחירת קובץ GGUF")};OutlinedButton(unload,Modifier.fillMaxWidth()){Text("פריקת המודל")};Spacer(Modifier.height(20.dp));Text("המודל Qwen2.5-0.5B-Instruct אינו כלול באפליקציה. המשתמש בוחר את קובץ ה-GGUF בעצמו.")}}
@Composable fun Settings(s:LlmSettings,set:(LlmSettings)->Unit){Column{Text("הגדרות LLM",style=MaterialTheme.typography.headlineSmall);Text("Temperature: "+s.temperature);Slider(s.temperature,{set(s.copy(temperature=it))});Text("Threads: "+s.threads);Slider(s.threads.toFloat(),{set(s.copy(threads=it.toInt().coerceIn(1,8)))},valueRange=1f..8f,steps=6);Text("Context: "+s.context);Text("Max Tokens: "+s.maxTokens)}}
