package com.noorehidayat.app
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
private val PB=Color(0xFF061F17);private val PP=Color(0xFF103B2B);private val PG=Color(0xFFE7C879);private val PC=Color(0xFFFFF4DE);private val PM=Color(0xFFB7C9BC)
private data class PrayerRow(val name:String,val time:String)
private val prayerMethods=listOf(1 to "Karachi",2 to "ISNA",3 to "Muslim World League",4 to "Umm Al-Qura",5 to "Egyptian",13 to "Turkey")
private fun permitted(c:Context)=ContextCompat.checkSelfPermission(c,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||ContextCompat.checkSelfPermission(c,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
private suspend fun prayerData(lat:Double,lon:Double,method:Int,school:Int):Pair<List<PrayerRow>,String> = withContext(Dispatchers.IO){val datePath=SimpleDateFormat("dd-MM-yyyy",Locale.US).format(Date());val c=URL("https://api.aladhan.com/v1/timings/$datePath?latitude=$lat&longitude=$lon&method=$method&school=$school").openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=20000;try{if(c.responseCode !in 200..299)error("HTTP "+c.responseCode);val j=JSONObject(c.inputStream.bufferedReader().use{it.readText()});require(j.optInt("code")==200);val d=j.getJSONObject("data");val t=d.getJSONObject("timings");val rows=listOf("Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha").map{val v=t.optString(it);require(v.isNotBlank()&&v!="null");PrayerRow(it,v.substringBefore(" ").take(5))};rows to d.getJSONObject("date").optString("readable")}finally{c.disconnect()}}
@Composable internal fun PrayerTimesScreen(){
 val ctx=LocalContext.current;val prefs=remember{ctx.getSharedPreferences("noor_prefs",0)};val lm=remember{ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager};var loc by remember{mutableStateOf<Location?>(null)};var rows by remember{mutableStateOf<List<PrayerRow>>(emptyList())};var date by remember{mutableStateOf("")};var error by remember{mutableStateOf("")};var loading by remember{mutableStateOf(false)};var allowed by remember{mutableStateOf(permitted(ctx))};var method by remember{mutableIntStateOf(prefs.getInt("prayer_method",1))};var school by remember{mutableIntStateOf(prefs.getInt("prayer_school",1))};var methodsExpanded by remember{mutableStateOf(false)}
 fun locate(){if(!permitted(ctx))return;try{lm.getProviders(true).forEach{p->try{val x=lm.getLastKnownLocation(p);if(x!=null&&(loc==null||x.time>(loc?.time?:0L)))loc=x}catch(_:SecurityException){}};if(loc==null)error="Waiting for a location fix. Enable device location and retry."}catch(_:Exception){error="Unable to read device location."}}
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){r->allowed=r.values.any{it};if(allowed)locate()else error="Location permission is needed for local prayer times."}
 LaunchedEffect(Unit){allowed=permitted(ctx);if(allowed)locate()}
 DisposableEffect(allowed){if(!allowed)onDispose{} else {val listener=LocationListener{x->loc=x};try{listOf(LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER).forEach{p->try{if(permitted(ctx)&&lm.isProviderEnabled(p))lm.requestLocationUpdates(p,5000L,50f,listener)}catch(_:Exception){}}}catch(_:Exception){};onDispose{try{lm.removeUpdates(listener)}catch(_:Exception){}}}}
 LaunchedEffect(loc?.latitude,loc?.longitude,method,school){val l=loc?:return@LaunchedEffect;loading=true;error="";try{val r=prayerData(l.latitude,l.longitude,method,school);rows=r.first;date=r.second}catch(_:Exception){error="Prayer times could not be loaded. Check your connection and retry."}finally{loading=false}}
 Column(Modifier.fillMaxSize().background(PB).padding(horizontal=18.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("DAILY WORSHIP",color=PG,fontSize=10.sp,letterSpacing=2.sp);Text("Prayer Times",color=PC,fontSize=27.sp,fontWeight=FontWeight.Bold);Text("Based on your current device location",color=PM,fontSize=12.sp);Spacer(Modifier.height(16.dp))
 if(loc==null){Icon(Icons.Default.LocationOn,null,tint=PG,modifier=Modifier.size(42.dp));Text("Allow location to calculate local prayer times.",color=PC,modifier=Modifier.padding(12.dp));Button(onClick={launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))},colors=ButtonDefaults.buttonColors(containerColor=PG,contentColor=PB)){Text("Allow location")}}
 else{
 Text(date,color=PG,fontWeight=FontWeight.SemiBold)
 Text("CALCULATION METHOD",color=PM,fontSize=10.sp,modifier=Modifier.padding(top=10.dp))
 Box{
  OutlinedButton(onClick={methodsExpanded=true},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=PG)){
   Text(prayerMethods.firstOrNull{it.first==method}?.second?:"Calculation method",modifier=Modifier.weight(1f));Icon(Icons.Default.ArrowDropDown,null)
  }
  DropdownMenu(expanded=methodsExpanded,onDismissRequest={methodsExpanded=false},modifier=Modifier.heightIn(max=330.dp).background(PP)){
   prayerMethods.forEach{item->DropdownMenuItem(text={Text(item.second,color=PC)},onClick={method=item.first;prefs.edit().putInt("prayer_method",method).apply();methodsExpanded=false})}
  }
 }
 Text("ASR SCHOOL",color=PM,fontSize=10.sp,modifier=Modifier.padding(top=8.dp))
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
  FilterChip(selected=school==1,onClick={school=1;prefs.edit().putInt("prayer_school",1).apply()},label={Text("Hanafi")})
  FilterChip(selected=school==0,onClick={school=0;prefs.edit().putInt("prayer_school",0).apply()},label={Text("Shafi'i")})
 }
 if(loading)LinearProgressIndicator(Modifier.fillMaxWidth(),color=PG)
 LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(top=12.dp,bottom=20.dp)){items(rows.size){i->val r=rows[i];Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=PP),border=BorderStroke(1.dp,PG.copy(alpha=.2f))){Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){Text(r.name,color=PC,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f));Text(r.time,color=PG,fontSize=19.sp,fontWeight=FontWeight.Bold)}}}}}
 if(error.isNotBlank()){Text(error,color=PG,fontSize=12.sp,modifier=Modifier.padding(top=10.dp));TextButton(onClick={if(allowed)locate()else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))}){Text("Retry",color=PG)}}
 Text("Prayer times are recalculated using the selected calculation method and Asr school. Confirm local mosque timetables where applicable. Confirm local mosque timetables where applicable.",color=PM,fontSize=11.sp,lineHeight=16.sp,modifier=Modifier.padding(top=8.dp))
 }}
