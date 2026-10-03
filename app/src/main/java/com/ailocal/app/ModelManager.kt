package com.ailocal.app
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
class ModelManager(private val context:Context){
 private var model:Any?=null
 var name:String=""
  private set
 fun load(uri:Uri,s:LlmSettings):Result<String> = runCatching{
  val n=context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())it.getString(0)else null}?: "model.gguf"
  require(n.lowercase(Locale.ROOT).endsWith(".gguf")){"יש לבחור קובץ GGUF"}
  unload()
  val file=File(context.filesDir,"active.gguf")
  context.contentResolver.openInputStream(uri).use{input->requireNotNull(input);FileOutputStream(file).use{output->input.copyTo(output)}}
  model=Llama.loadModel(file.absolutePath,LlamaConfig(contextSize=s.context,threads=s.threads))
  name=n
  "המודל נטען בהצלחה"
 }
 suspend fun complete(prompt:String,s:LlmSettings):Result<Pair<String,Double>> = runCatching{
  val m=requireNotNull(model){"לא נטען מודל GGUF"}
  val r=Llama.complete(m,prompt=prompt,systemPrompt=s.systemPrompt,maxTokens=s.maxTokens)
  Pair(r.text,r.tokensPerSecond)
 }
 fun unload(){model?.let{runCatching{Llama.releaseModel(it)}};model=null;name=""}
}
