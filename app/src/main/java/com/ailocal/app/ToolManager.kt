package com.ailocal.app
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class ToolManager(private val context:Context){
 fun parse(text:String):ActionCall?=runCatching{
  val start=text.indexOf('{');val end=text.lastIndexOf('}')
  if(start<0||end<=start)return null
  val o=org.json.JSONObject(text.substring(start,end+1))
  if(o.optString("type")!="action")return null
  val p=mutableMapOf<String,String>();val q=o.optJSONObject("parameters")
  q?.keys()?.forEach{p[it]=q.optString(it)}
  ActionCall(o.optString("action"),p)
 }.getOrNull()
 fun needsApproval(a:ActionCall)=a.action in setOf("delete_file","create_file","create_alarm","create_timer","send")
 fun execute(a:ActionCall):String=when(a.action){
  "open_app"->openApp(a.parameters["package"].orEmpty())
  "open_settings"->open(Settings.ACTION_SETTINGS)
  "open_wifi_settings"->open(Settings.ACTION_WIFI_SETTINGS)
  "open_bluetooth_settings"->open(Settings.ACTION_BLUETOOTH_SETTINGS)
  "open_display_settings"->open(Settings.ACTION_DISPLAY_SETTINGS)
  "open_sound_settings"->open(Settings.ACTION_SOUND_SETTINGS)
  "open_battery_settings"->open(Settings.ACTION_BATTERY_SAVER_SETTINGS)
  "open_storage_settings"->open(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
  "open_accessibility_settings"->open(Settings.ACTION_ACCESSIBILITY_SETTINGS)
  "open_app_settings"->open(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+ (a.parameters["package"]?:context.packageName)))
  "time"->SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(Date())
  "date"->SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(Date())
  "battery_info"->"סוללה: "+context.getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)+"%"
  "device_info"->"Android "+Build.VERSION.RELEASE+", API "+Build.VERSION.SDK_INT+", "+Build.MANUFACTURER+" "+Build.MODEL
  "storage_info"->{val s=StatFs(context.filesDir.absolutePath);"פנוי: "+s.availableBytes/(1024*1024)+" MB"}
  else->"הכלי '"+a.action+"' אינו זמין."
 }
 private fun open(action:String,data:Uri?=null)=runCatching{context.startActivity(Intent(action).apply{flags=Intent.FLAG_ACTIVITY_NEW_TASK;data?.let{this.data=it}});"המסך נפתח."}.getOrElse{"לא ניתן לפתוח את המסך."}
 private fun openApp(pkg:String)=runCatching{val i=context.packageManager.getLaunchIntentForPackage(pkg)?:return "האפליקציה לא נמצאה.";i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(i);"האפליקציה נפתחה."}.getOrElse{"לא ניתן לפתוח את האפליקציה."}
}
