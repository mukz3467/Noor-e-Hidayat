package com.noorehidayat.app
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
private val TB=Color(0xFF061F17);private val TP=Color(0xFF103B2B);private val TG=Color(0xFFE7C879);private val TC=Color(0xFFFFF4DE);private val TM=Color(0xFFB7C9BC)
private data class Edition(val id:String,val name:String,val lang:String);private data class TafsirAyah(val n:Int,val text:String)
private suspend fun tafsirRequest(path:String)=withContext(Dispatchers.IO){val c=URL("https://api.alquran.cloud/v1/$path").openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=25000;try{if(c.responseCode !in 200..299)error("HTTP "+c.responseCode);JSONObject(c.inputStream.bufferedReader().use{it.readText()}).also{require(it.optInt("code")==200)}}finally{c.disconnect()}}
@Composable internal fun TafseerScreen(surah:Surah?){
 var editions by remember(surah?.number){mutableStateOf<List<Edition>>(emptyList())};var selected by remember(surah?.number){mutableStateOf<Edition?>(null)};var ayahs by remember(surah?.number){mutableStateOf<List<TafsirAyah>>(emptyList())};var loading by remember(surah?.number){mutableStateOf(false)};var error by remember{mutableStateOf("")};var expanded by remember{mutableStateOf(false)}
 LaunchedEffect(surah?.number){if(surah==null)return@LaunchedEffect;loading=true;try{val a=tafsirRequest("edition/type/tafsir").getJSONArray("data");val found=buildList{for(i in 0 until a.length()){val x=a.getJSONObject(i);val id=x.optString("identifier");val lang=x.optString("language");if(id.isNotBlank()&&x.optString("format")=="text"&&lang in setOf("ur","en","ar"))add(Edition(id,x.optString("englishName").ifBlank{x.optString("name")},lang))}}.sortedBy{when(it.lang){"ur"->0;"en"->1;else->2}};editions=found;selected=found.firstOrNull();if(found.isEmpty())error="No Urdu, English or Arabic Tafseer edition is currently listed."}catch(_:Exception){error="Tafseer catalogue unavailable. Check your internet connection."}finally{loading=false}}
 LaunchedEffect(surah?.number,selected?.id){val s=surah?:return@LaunchedEffect;val e=selected?:return@LaunchedEffect;loading=true;error="";ayahs=emptyList();try{val a=tafsirRequest("surah/${s.number}/${e.id}").getJSONObject("data").getJSONArray("ayahs");ayahs=buildList{for(i in 0 until a.length()){val x=a.getJSONObject(i);add(TafsirAyah(x.optInt("numberInSurah"),x.optString("text")))}};if(ayahs.isEmpty())error="No Tafseer text was returned for this Surah."}catch(_:Exception){error="Could not load this Tafseer edition. Choose another edition or retry."}finally{loading=false}}
 Column(Modifier.fillMaxSize().background(TB).padding(horizontal=18.dp)){val s=surah;if(s==null){Text("Open Tafseer from a Surah in the Quran reader.",color=TM);return@Column};Text("QURAN EXPLANATION",color=TG,fontSize=10.sp,letterSpacing=2.sp);Text(s.english+" · Tafseer",color=TC,fontSize=25.sp,fontWeight=FontWeight.Bold);Text("Surah ${s.number} · ${s.ayahs} Ayahs",color=TM,fontSize=12.sp);Spacer(Modifier.height(12.dp))
 Box{OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=TG)){Text(selected?.let{"${it.name} · ${it.lang.uppercase()}"}?:"Select Tafseer edition",modifier=Modifier.weight(1f),maxLines=2);Icon(Icons.Default.ArrowDropDown,null)};DropdownMenu(expanded=expanded,onDismissRequest={expanded=false},modifier=Modifier.heightIn(max=350.dp).background(TP)){editions.forEach{e->DropdownMenuItem(text={Text("${e.name} · ${e.lang.uppercase()}",color=TC)},onClick={selected=e;expanded=false})}}}
 if(loading)LinearProgressIndicator(Modifier.fillMaxWidth(),color=TG);if(error.isNotBlank())Text(error,color=TG,fontSize=12.sp,modifier=Modifier.padding(vertical=10.dp))
 LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(top=10.dp,bottom=20.dp)){items(ayahs){a->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=TP),border=BorderStroke(1.dp,TG.copy(alpha=.2f))){Column(Modifier.padding(14.dp)){Text("AYAH ${a.n}",color=TG,fontSize=10.sp,fontWeight=FontWeight.Bold);Text(a.text,color=TC,fontSize=14.sp,lineHeight=23.sp,textAlign=if(selected?.lang=="ur"||selected?.lang=="ar")TextAlign.End else TextAlign.Start,modifier=Modifier.fillMaxWidth().padding(top=7.dp))}}}}
 Text("Tafseer text is from the selected published edition. Consult qualified scholarship for religious rulings.",color=TM,fontSize=10.sp,lineHeight=15.sp,modifier=Modifier.padding(bottom=8.dp))
 }}
