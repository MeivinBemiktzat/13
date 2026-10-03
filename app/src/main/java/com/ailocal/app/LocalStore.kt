package com.ailocal.app
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
class LocalStore(c:Context){
 private val dir=File(c.filesDir,"chats").apply{mkdirs()}
 fun save(s:ChatSession){
  val o=JSONObject().put("id",s.id).put("title",s.title)
  val a=JSONArray()
  s.messages.forEach{a.put(JSONObject().put("role",it.role).put("content",it.content))}
  o.put("messages",a)
  File(dir,s.id+".json").writeText(o.toString())
 }
 fun all():List<ChatSession>{
  return dir.listFiles()?.mapNotNull{f->runCatching{
   val o=JSONObject(f.readText()); val a=o.optJSONArray("messages")?:JSONArray()
   val m=mutableListOf<ChatMessage>()
   for(i in 0 until a.length()){val x=a.getJSONObject(i);m.add(ChatMessage(x.optString("role"),x.optString("content")))}
   ChatSession(o.getString("id"),o.optString("title","שיחה"),m)
  }.getOrNull()}?:emptyList()
 }
 fun delete(id:String){File(dir,id+".json").delete()}
}
