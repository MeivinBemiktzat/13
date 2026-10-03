package com.ailocal.app
data class ChatMessage(val role:String,val content:String)
data class ChatSession(val id:String,var title:String,val messages:MutableList<ChatMessage> = mutableListOf())
data class LlmSettings(val temperature:Float=.7f,val topP:Float=.9f,val topK:Int=40,val maxTokens:Int=256,val context:Int=2048,val threads:Int=2,val systemPrompt:String=Defaults.SYSTEM_PROMPT)
data class ActionCall(val action:String,val parameters:Map<String,String>)
object Defaults {
 const val SYSTEM_PROMPT = """אתה עוזר AI מקומי בתוך Android.
כאשר אין צורך בפעולה, ענה בטקסט רגיל.
כאשר נדרשת פעולה, החזר JSON בלבד:
{"type":"action","action":"open_app","parameters":{"package":"com.example.app"}}
אל תטען שפעולה בוצעה לפני שהאפליקציה החזירה תוצאה מאומתת."""
}
