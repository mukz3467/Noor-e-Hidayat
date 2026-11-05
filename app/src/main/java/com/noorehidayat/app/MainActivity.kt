package com.noorehidayat.app

import android.annotation.SuppressLint
import android.content.Context
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.io.File
import android.content.ComponentName
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Emerald=Color(0xFF061F17)
private val Deep=Color(0xFF0B2C20)
private val Panel=Color(0xFF103B2B)
private val Gold=Color(0xFFE7C879)
private val Cream=Color(0xFFFFF4DE)
private val Muted=Color(0xFFB7C9BC)
private val Border=Color(0xFF355B45)
data class Surah(val number:Int,val name:String,val english:String,val meaning:String,val ayahs:Int,val type:String)
data class Ayah(val number:Int,val arabic:String,val translation:String)
data class Recitation(val reciter:String,val style:String,val server:String,val surahs:Set<Int>){val label get()="$reciter · $style"}
data class QuranTranslationEdition(val id:String,val name:String,val language:String,val direction:String)

class MainActivity:ComponentActivity(){
 private lateinit var consentInformation:ConsentInformation
 private var adsAllowed by mutableStateOf(false)
 private var privacyOptionsRequired by mutableStateOf(false)

 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  window.statusBarColor=android.graphics.Color.rgb(6,31,23)
  window.navigationBarColor=android.graphics.Color.rgb(6,31,23)
  consentInformation=UserMessagingPlatform.getConsentInformation(this)
  setContent{
   NoorApp(
    adsAllowed=adsAllowed,
    privacyOptionsRequired=privacyOptionsRequired,
    onPrivacyOptions={UserMessagingPlatform.showPrivacyOptionsForm(this){ formError -> 
     privacyOptionsRequired=consentInformation.privacyOptionsRequirementStatus==ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }}
   )
  }
  gatherConsent()
 }

 private fun gatherConsent(){
  val params=ConsentRequestParameters.Builder().build()
  consentInformation.requestConsentInfoUpdate(
   this,
   params,
   {
    privacyOptionsRequired=consentInformation.privacyOptionsRequirementStatus==ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    UserMessagingPlatform.loadAndShowConsentFormIfRequired(this){ formError ->
     privacyOptionsRequired=consentInformation.privacyOptionsRequirementStatus==ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
     enableAdsIfAllowed()
    }
    enableAdsIfAllowed()
   },
   { requestError ->
    privacyOptionsRequired=consentInformation.privacyOptionsRequirementStatus==ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    enableAdsIfAllowed()
   }
  )
 }

 private fun enableAdsIfAllowed(){
  if(consentInformation.canRequestAds()&&!adsAllowed){
   adsAllowed=true
   MobileAds.initialize(this){}
  }
 }
}

private fun prefsHistory(ctx:android.content.Context):List<String> = (ctx.getSharedPreferences("noor_prefs",0).getString("history","")?:"").split("|").filter{it.isNotBlank()}

private suspend fun api(path:String):String=withContext(Dispatchers.IO){
 val c=URL("https://api.alquran.cloud/v1/"+path).openConnection() as HttpURLConnection
 c.connectTimeout=15000;c.readTimeout=20000
 try{if(c.responseCode !in 200..299) error("HTTP "+c.responseCode);c.inputStream.bufferedReader().use{it.readText()}}finally{c.disconnect()}
}
private suspend fun getTranslationEditions():List<QuranTranslationEdition>{
 val j=JSONObject(api("edition/type/translation"));require(j.optInt("code")==200);val a=j.getJSONArray("data")
 return (0 until a.length()).mapNotNull{i->val o=a.getJSONObject(i);val id=o.optString("identifier");val language=o.optString("language");if(id.isBlank()||o.optString("format")!="text")null else QuranTranslationEdition(id,o.optString("englishName").ifBlank{o.optString("name")},language,o.optString("direction").ifBlank{if(language in setOf("ar","ur","fa","ps"))"rtl" else "ltr"})}
 .sortedWith(compareBy<QuranTranslationEdition>{when(it.language){"ur"->0;"en"->1;"hi"->2;"ar"->3;else->4}}.thenBy{it.name})
}
private suspend fun getRecitations():List<Recitation> = withContext(Dispatchers.IO){
 val connection=URL("https://www.mp3quran.net/api/v3/reciters?language=eng").openConnection() as HttpURLConnection
 connection.connectTimeout=15000;connection.readTimeout=20000
 try{
  if(connection.responseCode !in 200..299) error("HTTP "+connection.responseCode)
  val root=JSONObject(connection.inputStream.bufferedReader().use{it.readText()})
  val reciters=root.getJSONArray("reciters")
  buildList{
   for(i in 0 until reciters.length()){
    val reciter=reciters.getJSONObject(i);val moshafs=reciter.optJSONArray("moshaf")?:continue
    for(k in 0 until moshafs.length()){
     val m=moshafs.getJSONObject(k);val server=m.optString("server").trim()
     val available=m.optString("surah_list").split(",").mapNotNull{it.trim().toIntOrNull()}.filter{it in 1..114}.toSet()
     if(server.startsWith("https://")&&available.isNotEmpty()) add(Recitation(reciter.optString("name","Unknown reciter"),m.optString("name","Quran recitation"),server,available))
    }
   }
  }.sortedBy{it.reciter.lowercase()}
 }finally{connection.disconnect()}
}
private fun recitationUrl(recitation:Recitation,surah:Int)=recitation.server.trimEnd('/')+"/"+surah.toString().padStart(3,'0')+".mp3"
private fun parseSurahCatalogue(raw:String):List<Surah>{
 val j=JSONObject(raw);require(j.optInt("code")==200);val a=j.getJSONArray("data")
 return (0 until a.length()).map{i->val o=a.getJSONObject(i);Surah(o.getInt("number"),o.getString("name"),o.getString("englishName"),o.getString("englishNameTranslation"),o.getInt("numberOfAyahs"),o.getString("revelationType"))}
}
private suspend fun getSurahs(context:Context):List<Surah>{
 val prefs=context.getSharedPreferences("noor_prefs",0)
 return try{val raw=api("surah");prefs.edit().putString("quran_surah_catalogue",raw).apply();parseSurahCatalogue(raw)}
 catch(error:Exception){val cached=prefs.getString("quran_surah_catalogue",null)?:throw error;parseSurahCatalogue(cached)}
}
private fun parseAyahs(raw:String):List<Ayah>{
 val j=JSONObject(raw);require(j.optInt("code")==200);val d=j.getJSONArray("data");require(d.length()>=2);val ar=d.getJSONObject(0).getJSONArray("ayahs");val tr=d.getJSONObject(1).getJSONArray("ayahs");require(ar.length()==tr.length())
 return (0 until ar.length()).map{i->Ayah(ar.getJSONObject(i).getInt("numberInSurah"),ar.getJSONObject(i).getString("text"),tr.getJSONObject(i).getString("text"))}
}
private suspend fun getAyahs(context:Context,n:Int,translationEdition:String):List<Ayah>{
 val safeEdition=translationEdition.takeIf{Regex("^[a-z]{2,3}\\\\.[A-Za-z0-9-]+$").matches(it)}?:"en.sahih"
 val prefs=context.getSharedPreferences("noor_prefs",0);val key="quran_ayahs_${n}_${safeEdition}"
 return try{
  val raw=api("surah/"+n+"/editions/quran-uthmani,"+safeEdition)
  val order=(prefs.getString("quran_cache_order","")?:"").split(",").filter{it.isNotBlank()&&it!=key}
  val next=(listOf(key)+order).take(8);val edit=prefs.edit().putString(key,raw).putString("quran_cache_order",next.joinToString(","))
  (order.drop(7)).forEach{edit.remove(it)};edit.apply();parseAyahs(raw)
 }catch(error:Exception){val cached=prefs.getString(key,null)?:throw error;parseAyahs(cached)}
}

@Composable private fun NoorApp(adsAllowed:Boolean,privacyOptionsRequired:Boolean,onPrivacyOptions:()->Unit){
 val ctx= LocalContext.current
 val prefs=remember{ctx.getSharedPreferences("noor_prefs",0)}
 var page by remember{mutableStateOf("Home")}
 var surahs by remember{mutableStateOf(emptyList<Surah>())}
 var ayahs by remember{mutableStateOf(emptyList<Ayah>())}
 var selected by remember{mutableStateOf<Surah?>(null)}
 var surahLoading by remember{mutableStateOf(false)}
 var ayahLoading by remember{mutableStateOf(false)}
 var catalogueError by remember{mutableStateOf("")}
 var readerError by remember{mutableStateOf("")}
 var surahReloadKey by remember{mutableIntStateOf(0)}
 var ayahReloadKey by remember{mutableIntStateOf(0)}
 var search by remember{mutableStateOf("")}
 var mediaTitle by remember{mutableStateOf("")}
 var mediaUrl by remember{mutableStateOf("")}
 var videoCategory by remember{mutableStateOf("Islamic Lectures")}
 var videoParentPage by remember{mutableStateOf("Lectures")}
 var videoId by remember{mutableStateOf("")}
 var lang by remember{mutableStateOf("English")}
 var quranTranslation by remember{mutableStateOf("en.sahih")}
 var translationEditions by remember{mutableStateOf(emptyList<QuranTranslationEdition>())}
 var translationsLoading by remember{mutableStateOf(false)}
 var translationsError by remember{mutableStateOf("")}
 var translationReloadKey by remember{mutableIntStateOf(0)}
 var recitations by remember{mutableStateOf(emptyList<Recitation>())}
 var recitationsLoading by remember{mutableStateOf(false)}
 var recitationsError by remember{mutableStateOf("")}
 var selectedRecitation by remember{mutableStateOf<Recitation?>(null)}
 var quizIndex by remember{mutableIntStateOf(0)}
 var quizScore by remember{mutableIntStateOf(0)}
 var quizAnswer by remember{mutableIntStateOf(-1)}
 var history by remember{mutableStateOf(prefsHistory(ctx))}
 var favs by remember{mutableStateOf(setOf<Int>())}
 var tafseerSurah by remember{mutableStateOf<Surah?>(null)}
 LaunchedEffect(Unit){
  favs=prefs.getStringSet("favs",emptySet())?.mapNotNull{it.toIntOrNull()}?.toSet()?:emptySet()
  lang=prefs.getString("lang","English")?:"English"
  quranTranslation=prefs.getString("quran_translation","en.sahih")?:"en.sahih"
 }
 LaunchedEffect(surahReloadKey){surahLoading=true;catalogueError="";try{surahs=getSurahs(ctx)}catch(e:Exception){catalogueError="Quran catalogue unavailable. Check your internet connection and tap Retry."}finally{surahLoading=false}}
 LaunchedEffect(translationReloadKey){translationsLoading=true;translationsError="";try{translationEditions=getTranslationEditions();if(translationEditions.none{it.id==quranTranslation}){quranTranslation=translationEditions.firstOrNull{it.language=="en"}?.id?:translationEditions.firstOrNull()?.id?:"en.sahih";prefs.edit().putString("quran_translation",quranTranslation).apply()}}catch(e:Exception){translationsError="Translation catalogue unavailable. Existing selected translation remains usable."}finally{translationsLoading=false}}
 LaunchedEffect(page){if(page=="Recitations"&&recitations.isEmpty()){recitationsLoading=true;recitationsError="";try{recitations=getRecitations();if(selectedRecitation==null)selectedRecitation=recitations.firstOrNull()}catch(e:Exception){recitationsError="Reciter catalogue unavailable. Check your internet connection and retry."}finally{recitationsLoading=false}}}
 fun openSurah(s:Surah){selected=s;ayahs=emptyList();page="Reader";ayahLoading=true;readerError=""}
 fun openTafseer(s:Surah){tafseerSurah=s;page="Tafseer"}
 LaunchedEffect(selected,page,ayahReloadKey,quranTranslation){val s=selected;if(page=="Reader"&&s!=null){ayahLoading=true;readerError="";ayahs=emptyList();try{ayahs=getAyahs(ctx,s.number,quranTranslation)}catch(e:Exception){readerError="Could not load this surah. Check your connection and tap Retry."}finally{ayahLoading=false}}}
 fun openWeb(title:String,url:String){mediaTitle=title;mediaUrl=url;videoId="";page="Media";history=(listOf(title)+history).distinct().take(30);prefs.edit().putString("history",history.joinToString("|")).apply()}
 BackHandler(page!="Home"){page=when(page){"Reader"->"Quran";"Tafseer"->"Reader";"Prayer Times"->"More";"Offline Audio"->"More";"Media"->"Home";"VideoLibrary"->videoParentPage;else->"Home"}}
 MaterialTheme(colorScheme=darkColorScheme(primary=Gold,onPrimary=Emerald,background=Emerald,surface=Deep,onSurface=Cream,secondary=Gold)){
  Scaffold(containerColor=Emerald,bottomBar={
   if(page !in listOf("Reader","Tafseer","Media"))Column { NavigationBar(containerColor=Deep){
    listOf("Home","Quran","Discover","More").forEach{p->
     val selectedTab=when(p){"Home"->page=="Home";"Quran"->page=="Quran";"Discover"->page in listOf("Discover","Recitations","Lectures","Live")||(page=="VideoLibrary"&&videoParentPage=="Discover");else->page in listOf("More","Learning","Quiz","Favourites","History","Settings","Prayer Times","Offline Audio")}
     NavigationBarItem(selected=selectedTab,onClick={page=when(p){"Discover"->"Discover";"More"->"More";else->p}},icon={Icon(when(p){"Home"->Icons.Default.Home;"Quran"->Icons.Default.MenuBook;"Discover"->Icons.Default.Explore;else->Icons.Default.GridView},null)},label={Text(p,fontSize=10.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Gold,selectedTextColor=Gold,indicatorColor=Panel,unselectedIconColor=Muted,unselectedTextColor=Muted))
    }
   }
    if(adsAllowed)AdMobBanner()
   }
  }){pad->
   Column(Modifier.fillMaxSize().padding(pad)){
    if(page!="Home")Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={page=when(page){"Reader"->"Quran";"Tafseer"->"Reader";"Prayer Times"->"More";"Media"->"Home";"VideoLibrary"->videoParentPage;else->"Home"}}){Icon(Icons.Default.ArrowBack,null,tint=Gold)};Text(pageTitle(page,selected,mediaTitle),color=Cream,fontSize=19.sp,fontWeight=FontWeight.SemiBold)}
    when(page){
     "Home"->HomeScreen({page="Quran"},{page="Recitations"},{page="Lectures"},{page="Live"},{page="Learning"},{page="Quiz"},{page="More"},{query->videoCategory="Islamic Videos";videoParentPage="Home";mediaTitle="";mediaUrl="";page="VideoLibrary";search=query})
     "Quran"->QuranScreen(surahs,surahLoading,catalogueError,search,{search=it},{openSurah(it)},{surahReloadKey++})
     "Reader"->ReaderScreen(selected,ayahs,ayahLoading,readerError,translationEditions.firstOrNull{it.id==quranTranslation}?.name?:if(quranTranslation=="ur.jalandhry")"اردو ترجمہ" else "Sahih International",translationEditions.firstOrNull{it.id==quranTranslation}?.direction=="rtl"||quranTranslation.startsWith("ur.")||quranTranslation.startsWith("ar."),favs.contains(selected?.number),{s->favs=if(favs.contains(s.number))favs-s.number else favs+s.number;prefs.edit().putStringSet("favs",favs.map{it.toString()}.toSet()).apply()},{s->openWeb(s.english+" · Mishary Alafasy","https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/"+s.number+".mp3")},{ayahReloadKey++},{s->openTafseer(s)},{surah->
       try{
        val queued=enqueueOfflineSurah(ctx,surah)
        Toast.makeText(ctx,if(queued)"Download started for "+surah.english else "Audio is already downloaded or downloading",Toast.LENGTH_LONG).show()
       }catch(e:Exception){Toast.makeText(ctx,"Could not start audio download. Check storage space and internet.",Toast.LENGTH_LONG).show()}
      })
     "Tafseer"->TafseerScreen(tafseerSurah)
     "Prayer Times"->PrayerTimesScreen()
     "Offline Audio"->OfflineAudioScreen(surahs,{surah->openWeb(surah.english+" · Offline audio",offlineSurahAudioUri(ctx,surah.number))},{surah->deleteOfflineSurah(ctx,surah.number)})
     "Discover"->DiscoverScreen{category->videoParentPage="Discover";videoCategory=category;page="VideoLibrary"}
     "Recitations"->RecitationsScreen(surahs,recitations,selectedRecitation,recitationsLoading,recitationsError,{selectedRecitation=it},{recitation,s->openWeb(s.english+" · "+recitation.reciter,recitationUrl(recitation,s.number))},{recitations=emptyList();selectedRecitation=null})
     "Lectures"->CategoryScreen("Islamic Lectures",listOf("Quran and Sunnah","Seerah of Prophet Muhammad","Islamic manners and character","Family and daily life","Stories of the Prophets","Islamic history")){videoParentPage="Lectures";videoCategory=it;page="VideoLibrary"}
     "Live"->LiveScreen{t,u->openWeb(t,u)}
     "More"->MoreScreen{page=it}
     "Learning"->LearningScreen()
     "Quiz"->QuizScreen(quizIndex,quizScore,quizAnswer,{a->if(quizIndex<quizData.size&&quizAnswer==-1){if(a==quizData[quizIndex].correctIndex)quizScore++;quizAnswer=a}},{if(quizIndex>=quizData.size){quizIndex=0;quizScore=0}else{quizIndex++};quizAnswer=-1})
     "Qibla"->QiblaScreen()
     "Favourites"->FavouritesScreen(surahs.filter{favs.contains(it.number)},{openSurah(it)},{s->favs=if(favs.contains(s.number))favs-s.number else favs+s.number;prefs.edit().putStringSet("favs",favs.map{it.toString()}.toSet()).apply()})
     "History"->HistoryScreen(history)
     "Settings"->SettingsScreen(lang,{lang=it;prefs.edit().putString("lang",it).apply()},quranTranslation,{edition->quranTranslation=edition;prefs.edit().putString("quran_translation",edition).apply();ayahReloadKey++},translationEditions,translationsLoading,translationsError,{translationReloadKey++},privacyOptionsRequired,onPrivacyOptions)
     "VideoLibrary"->VideoLibraryScreen(videoCategory,search){target->if(target.startsWith("https://"))openWeb(videoCategory,target)else if(target.isNotBlank())openWeb(videoCategory,"https://www.youtube-nocookie.com/embed/videoseries?list="+target+"&playsinline=1")}
     "Media"->MediaScreen(mediaTitle,mediaUrl,videoId)
     else->HomeScreen({page="Quran"},{page="Recitations"},{page="Lectures"},{page="Live"},{page="Learning"},{page="Quiz"},{page="More"},{query->videoCategory="Islamic Videos";videoParentPage="Home";search=query;page="VideoLibrary"})
    }
   }
  }
 }
}
private fun pageTitle(p:String,s:Surah?,t:String)=when(p){"Reader"->s?.english?:"Quran";"Media"->t;"VideoLibrary"->"Video Library";"Quran"->"The Holy Quran";"Recitations"->"Beautiful Recitations";"Lectures"->"Islamic Lectures";"Live"->"Makkah & Madinah";"Learning"->"Quran Learning";"Quiz"->"Islamic Quiz";"Favourites"->"Favourites";"History"->"Watch History";"Settings"->"Settings";"Qibla"->"Qibla Compass";"Tafseer"->"Tafseer";"Prayer Times"->"Prayer Times";"Offline Audio"->"Offline Audio";else->p}

@Composable private fun Base(content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().background(Emerald).padding(horizontal=18.dp),content=content)}
@Composable private fun Heading(kicker:String,title:String){Text(kicker.uppercase(),color=Gold,fontSize=10.sp,letterSpacing=2.sp);Text(title,color=Cream,fontSize=27.sp,fontWeight=FontWeight.Bold,fontFamily=FontFamily.Serif,modifier=Modifier.padding(top=5.dp,bottom=14.dp))}
@Composable private fun Tile(title:String,sub:String,symbol:String,click:()->Unit){Card(Modifier.clickable{click()},shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Panel),border=BorderStroke(1.dp,Border)){Column(Modifier.fillMaxWidth().padding(15.dp)){Text(symbol,color=Gold,fontSize=25.sp);Spacer(Modifier.height(10.dp));Text(title,color=Cream,fontWeight=FontWeight.SemiBold);Text(sub,color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=4.dp))}}}
@Composable private fun HomeScreen(q:()->Unit,r:()->Unit,l:()->Unit,live:()->Unit,learn:()->Unit,quiz:()->Unit,more:()->Unit,searchVideos:(String)->Unit){
 var homeQuery by remember{mutableStateOf("")}
 Base{
  Column(Modifier.fillMaxWidth().padding(top=14.dp,bottom=12.dp),horizontalAlignment=Alignment.CenterHorizontally){
   Text("☾",fontSize=38.sp,color=Gold)
   Text("NOOR-E-HIDAYAT",fontSize=25.sp,fontWeight=FontWeight.Bold,color=Gold,letterSpacing=2.sp)
   Text("LEARN · WATCH · PEACE",fontSize=10.sp,color=Muted,letterSpacing=2.sp)
  }
  OutlinedTextField(value=homeQuery,onValueChange={homeQuery=it},modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Search Quran, Surah or Islamic videos…",color=Muted)},leadingIcon={Icon(Icons.Default.Search,null,tint=Gold)},trailingIcon={IconButton(onClick={searchVideos(homeQuery)}){Icon(Icons.Default.ArrowForward,null,tint=Gold)}},colors=fieldColors())
  Spacer(Modifier.height(14.dp))
  androidx.compose.foundation.lazy.grid.LazyVerticalGrid(columns=androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),modifier=Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(9.dp),verticalArrangement=Arrangement.spacedBy(9.dp),contentPadding=PaddingValues(bottom=12.dp)){
   item{Tile("Quran Tilawat","With translation","📖",q)}
   item{Tile("Beautiful Recitations","Listen & reflect","♫",r)}
   item{Tile("Islamic Lectures","Urdu · English","◉",l)}
   item{Tile("Makkah & Madinah","Live streams","🕋",live)}
   item{Tile("Daily Islamic Videos","Watch & learn","▶", {searchVideos("Islamic reminders")})}
   item{Tile("More Categories","Explore topics","▦",more)}
  }
  Card(Modifier.fillMaxWidth().padding(bottom=10.dp),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Panel),border=BorderStroke(1.dp,Gold.copy(alpha=.35f))){
   Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically){
    Text("🕌",fontSize=30.sp)
    Column(Modifier.weight(1f).padding(start=12.dp)){
     Text("DAILY REMINDER",color=Gold,fontSize=10.sp,letterSpacing=1.sp)
     Text("Indeed, in the remembrance of Allah do hearts find peace.",color=Cream,fontSize=13.sp,lineHeight=18.sp)
     Text("Quran · 13:28",color=Muted,fontSize=10.sp)
    }
   }
  }
 }
}
@Composable private fun QuranScreen(data:List<Surah>,loading:Boolean,error:String,search:String,onSearch:(String)->Unit,onOpen:(Surah)->Unit,retry:()->Unit){
 Base{Heading("THE HOLY QURAN","114 Surahs");OutlinedTextField(search,onSearch,Modifier.fillMaxWidth(),placeholder={Text("Search surah or number",color=Muted)},leadingIcon={Icon(Icons.Default.Search,null,tint=Gold)},singleLine=true,colors=fieldColors())
 if(loading)LinearProgressIndicator(Modifier.fillMaxWidth(),color=Gold)
 if(error.isNotEmpty())Column(Modifier.fillMaxWidth().padding(10.dp)){Text(error,color=Gold);TextButton(onClick=retry){Text("Retry",color=Gold)}}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(top=10.dp,bottom=22.dp)){items(data.filter{search.isBlank()||it.english.contains(search,true)||it.meaning.contains(search,true)||it.number.toString()==search||it.name.contains(search)}){s->Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(14.dp)).clickable{onOpen(s)}.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(40.dp).background(Emerald,RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){Text(s.number.toString(),color=Gold,fontWeight=FontWeight.Bold)};Column(Modifier.weight(1f).padding(start=12.dp)){Text(s.english,color=Cream,fontWeight=FontWeight.SemiBold);Text(s.meaning+" · "+s.ayahs+" Ayahs · "+s.type,color=Muted,fontSize=11.sp)};Text(s.name,color=Gold,fontSize=19.sp)}}}
 }
}
internal fun offlineSurahAudioFile(context:Context,number:Int):File {
 val root=context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?:File(context.filesDir,"music")
 if(!root.exists())root.mkdirs()
 return File(root,"noor-surah-$number.mp3")
}
internal fun offlineSurahAudioUri(context:Context,number:Int):String = Uri.fromFile(offlineSurahAudioFile(context,number)).toString()
internal fun isOfflineSurahReady(context:Context,number:Int):Boolean {
 val file=offlineSurahAudioFile(context,number)
 if(!file.exists()||file.length()<=0L)return false
 val id=context.getSharedPreferences("noor_prefs",0).getLong("offline_download_"+number,-1L)
 if(id<0L)return true
 return try{
  val query=DownloadManager.Query().setFilterById(id)
  (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).query(query).use{cursor->
   cursor.moveToFirst()&&cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))==DownloadManager.STATUS_SUCCESSFUL
  }
 }catch(_:Exception){false}
}
internal fun deleteOfflineSurah(context:Context,number:Int) {
 val prefs=context.getSharedPreferences("noor_prefs",0)
 val id=prefs.getLong("offline_download_"+number,-1L)
 if(id>=0L)try{(context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).remove(id)}catch(_:Exception){}
 offlineSurahAudioFile(context,number).delete()
 prefs.edit().remove("offline_download_"+number).remove("offline_title_"+number).apply()
}
internal fun enqueueOfflineSurah(context:Context,surah:Surah):Boolean {
 val file=offlineSurahAudioFile(context,surah.number)
 val prefs=context.getSharedPreferences("noor_prefs",0)
 val manager=context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
 if(isOfflineSurahReady(context,surah.number))return false
 val prior=prefs.getLong("offline_download_"+surah.number,-1L)
 if(prior>=0L){
  var stillActive=false
  try{
   val query=DownloadManager.Query().setFilterById(prior)
   manager.query(query).use{cursor->
    if(cursor.moveToFirst()){
     val status=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
     stillActive=status==DownloadManager.STATUS_PENDING||status==DownloadManager.STATUS_RUNNING||status==DownloadManager.STATUS_PAUSED
    }
   }
  }catch(_:Exception){}
  if(stillActive)return false
  try{manager.remove(prior)}catch(_:Exception){}
 }
 // A failed or interrupted download can leave a partial file. Remove it before retrying.
 if(file.exists()&&!file.delete())throw IllegalStateException("Could not clear the incomplete audio file")
 val request=DownloadManager.Request(Uri.parse("https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/"+surah.number+".mp3"))
  .setTitle("Surah "+surah.english)
  .setDescription("Noor-e-Hidayat · Mishary Alafasy")
  .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
  .setAllowedOverMetered(true)
  .setAllowedOverRoaming(false)
  .setDestinationInExternalFilesDir(context,Environment.DIRECTORY_MUSIC,file.name)
 val id=manager.enqueue(request)
 prefs.edit().putLong("offline_download_"+surah.number,id).putString("offline_title_"+surah.number,surah.english).apply()
 return true
}
@Composable private fun OfflineAudioScreen(data:List<Surah>,play:(Surah)->Unit,remove:(Surah)->Unit){
 val context=LocalContext.current
 var refresh by remember{mutableIntStateOf(0)}
 val saved=remember(data,refresh){data.filter{surah->isOfflineSurahReady(context,surah.number)}}
 Base{
  Heading("LISTEN ANYWHERE","Offline audio")
  Text("Downloaded Surah recitations are stored in this app's private music folder and can play without an internet connection.",color=Muted,fontSize=12.sp,lineHeight=18.sp)
  if(saved.isEmpty())Text("No audio downloaded yet. Open a Surah and tap Save audio.",color=Muted,modifier=Modifier.padding(top=18.dp))
  LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(top=12.dp,bottom=20.dp)){
   items(saved){surah->
    Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(13.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){
     Column(Modifier.weight(1f)){Text(surah.english,color=Cream,fontWeight=FontWeight.SemiBold);Text(surah.ayahs.toString()+" Ayahs · Offline",color=Muted,fontSize=11.sp)}
     IconButton(onClick={play(surah)}){Icon(Icons.Default.PlayArrow,null,tint=Gold)}
     IconButton(onClick={remove(surah);refresh++}){Icon(Icons.Default.DeleteOutline,null,tint=Muted)}
    }
   }
  }
 }
}
@Composable private fun ReaderScreen(s:Surah?,data:List<Ayah>,loading:Boolean,error:String,translationLabel:String,translationRtl:Boolean,isFav:Boolean,toggleFav:(Surah)->Unit,audio:(Surah)->Unit,retry:()->Unit,openTafseer:(Surah)->Unit,download:(Surah)->Unit){
 Base{if(s==null)return@Base;Heading("SURAH "+s.number+" · "+s.type,s.english);Text(s.meaning+" · "+s.ayahs+" Ayahs",color=Muted,fontSize=12.sp);Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){TextButton(onClick={toggleFav(s)}){Icon(if(isFav)Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=Gold);Text(if(isFav)" Saved" else " Save",color=Gold)};TextButton(onClick={openTafseer(s)}){Text("Tafseer",color=Gold)};TextButton(onClick={download(s)}){Icon(Icons.Default.Download,null,tint=Gold);Text(" Save audio",color=Gold)};Spacer(Modifier.weight(1f));Button({audio(s)},Modifier.padding(vertical=10.dp),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Emerald)){Icon(Icons.Default.PlayArrow,null);Text("  Listen")}}
 if(loading)LinearProgressIndicator(Modifier.fillMaxWidth(),color=Gold);if(error.isNotEmpty())Column(Modifier.fillMaxWidth().padding(10.dp)){Text(error,color=Gold);TextButton(onClick=retry){Text("Retry",color=Gold)}}
 LazyColumn(contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(data){a->Column(Modifier.fillMaxWidth().background(Deep,RoundedCornerShape(14.dp)).padding(15.dp)){Text("AYAH "+a.number,color=Gold,fontSize=10.sp);Text(a.arabic,color=Cream,fontSize=27.sp,lineHeight=48.sp,textAlign=TextAlign.End,modifier=Modifier.fillMaxWidth().padding(vertical=10.dp),fontFamily=FontFamily.Serif);Divider(color=Border);Text(translationLabel,color=Gold,fontSize=10.sp,modifier=Modifier.padding(top=9.dp));Text(a.translation,color=Cream,fontSize=if(translationRtl)16.sp else 14.sp,lineHeight=if(translationRtl)29.sp else 23.sp,textAlign=if(translationRtl)TextAlign.End else TextAlign.Start,modifier=Modifier.fillMaxWidth())}}}
 }
}
@Composable private fun RecitationsScreen(data:List<Surah>,recitations:List<Recitation>,selected:Recitation?,loading:Boolean,error:String,onSelect:(Recitation)->Unit,play:(Recitation,Surah)->Unit,retry:()->Unit){
 var expanded by remember{mutableStateOf(false)}
 var search by remember{mutableStateOf("")}
 Base{
  Heading("LISTEN & REFLECT","Quran reciters")
  Text("Choose a reciter and a Surah. The list is loaded from MP3Quran's public catalogue; only Surahs listed as available are shown.",color=Muted,fontSize=13.sp,lineHeight=19.sp)
  Spacer(Modifier.height(12.dp))
  Box{
   OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=Gold)){
    Text(selected?.label?:if(loading)"Loading reciters…" else "Select a reciter",modifier=Modifier.weight(1f),maxLines=2)
    Icon(Icons.Default.ArrowDropDown,null)
   }
   DropdownMenu(expanded=expanded,onDismissRequest={expanded=false},modifier=Modifier.heightIn(max=420.dp).background(Deep)){
    recitations.forEach{r->DropdownMenuItem(text={Column{Text(r.reciter,color=Cream);Text(r.style,color=Muted,fontSize=10.sp)}},onClick={onSelect(r);expanded=false})}
   }
  }
  if(loading)LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=10.dp),color=Gold)
  if(error.isNotEmpty())Column(Modifier.fillMaxWidth().padding(vertical=12.dp)){Text(error,color=Gold);TextButton(onClick=retry){Text("Retry",color=Gold)}}
  if(!loading&&error.isEmpty()&&recitations.isEmpty())Text("No reciters are available right now.",color=Muted,modifier=Modifier.padding(top=14.dp))
  if(selected!=null){
   Text("${selected.surahs.size} Surahs available · ${selected.style}",color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=12.dp))
   OutlinedTextField(search,{search=it},Modifier.fillMaxWidth().padding(top=8.dp),singleLine=true,placeholder={Text("Find a Surah",color=Muted)},leadingIcon={Icon(Icons.Default.Search,null,tint=Gold)},colors=fieldColors())
   LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(top=10.dp,bottom=22.dp)){
    items(data.filter{selected.surahs.contains(it.number)&&(search.isBlank()||it.english.contains(search,true)||it.number.toString()==search)}){s->
     Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(13.dp)).clickable{play(selected,s)}.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
      Icon(Icons.Default.PlayCircle,null,tint=Gold)
      Column(Modifier.weight(1f).padding(start=12.dp)){Text(s.english,color=Cream);Text(s.meaning+" · "+s.ayahs+" Ayahs",color=Muted,fontSize=11.sp)}
      Text(s.name,color=Gold,fontSize=18.sp)
     }
    }
   }
  }
 }
}
@Composable private fun DiscoverScreen(open:(String)->Unit){
 val categories=listOf(
  "Quran Recitations" to "Full-surah audio and Quran listening",
  "Islamic Lectures" to "Talks on faith, character and daily life",
  "Islamic Dramas" to "Faith-inspired stories and family viewing",
  "Islamic Documentaries" to "History, places and educational films",
  "Seerah of Prophet Muhammad ﷺ" to "Learn about the life of the Prophet",
  "Stories of the Prophets" to "Lessons from the Prophets",
  "Kids & Family" to "Family-friendly Islamic learning"
 )
 Base{
  Heading("DISCOVER","Islamic content")
  Text("Browse by category. Only verified, embeddable collections will be published here; unavailable collections are clearly marked.",color=Muted,fontSize=13.sp,lineHeight=19.sp)
  Spacer(Modifier.height(12.dp))
  LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=22.dp)){
   items(categories){(name,description)->
    Card(onClick={open(name)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Panel),border=BorderStroke(1.dp,Border)){
     Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){
      Icon(Icons.Default.PlayCircle,null,tint=Gold,modifier=Modifier.size(30.dp))
      Column(Modifier.weight(1f).padding(start=12.dp)){
       Text(name,color=Cream,fontWeight=FontWeight.SemiBold,fontSize=14.sp)
       Text(description,color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=4.dp))
      }
      Icon(Icons.Default.ChevronRight,null,tint=Gold)
     }
    }
   }
  }
 }
}

@Composable private fun CategoryScreen(title:String,categories:List<String>,open:(String)->Unit){
 val isLecture=title.contains("lecture",true)
 val eyebrow=if(isLecture)"LEARN FROM THE UMMAH" else "EXPLORE & LISTEN"
 val intro=if(isLecture)"Explore lessons by subject. Playback stays inside Noor-e-Hidayat." else "Choose a recitation collection and continue your listening journey."
 Base{
  Column(Modifier.fillMaxWidth().padding(top=10.dp,bottom=14.dp)){
   Text(eyebrow,color=Gold,fontSize=10.sp,letterSpacing=2.sp,fontWeight=FontWeight.Bold)
   Text(title,color=Cream,fontSize=29.sp,fontWeight=FontWeight.Bold,fontFamily=FontFamily.Serif,modifier=Modifier.padding(top=6.dp))
   Text(intro,color=Muted,fontSize=13.sp,lineHeight=20.sp,modifier=Modifier.padding(top=8.dp))
  }
  Card(Modifier.fillMaxWidth().padding(bottom=16.dp),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Panel),border=BorderStroke(1.dp,Gold.copy(alpha=.35f))){
   Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){
    Box(Modifier.size(52.dp).background(Emerald,RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center){Text(if(isLecture)"◉" else "♫",color=Gold,fontSize=27.sp)}
    Column(Modifier.weight(1f).padding(start=14.dp)){
     Text(if(isLecture)"Explore by subject" else "Listen with focus",color=Gold,fontSize=11.sp,fontWeight=FontWeight.Bold)
     Text(if(isLecture)"Choose a subject to find relevant talks" else "Select a recitation to open the player",color=Cream,fontSize=13.sp,modifier=Modifier.padding(top=4.dp))
    }
   }
  }
  LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=24.dp)){
   items(categories){category->
    Card(onClick={open(category)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(17.dp),colors=CardDefaults.cardColors(containerColor=Deep),border=BorderStroke(1.dp,Border)){
     Row(Modifier.fillMaxWidth().padding(horizontal=15.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically){
      Box(Modifier.size(42.dp).background(Panel,RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center){Text(if(isLecture)"▶" else "♫",color=Gold,fontSize=18.sp)}
      Column(Modifier.weight(1f).padding(start=13.dp)){
       Text(category,color=Cream,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
       Text(if(isLecture)"Browse talks on this subject" else "Open recitation collection",color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=4.dp))
      }
      Icon(Icons.Default.ChevronRight,null,tint=Gold)
     }
    }
   }
  }
 }
}

private data class CuratedPlaylist(val category:String,val playlistId:String)
private val curatedPlaylists = emptyList<CuratedPlaylist>()

@Composable private fun VideoLibraryScreen(category:String,initialQuery:String,play:(String)->Unit){
 var searchText by remember(category){mutableStateOf(initialQuery.ifBlank{if(category=="Islamic Videos")"Islamic reminders" else ""})}
 var language by remember(category){mutableStateOf("All")}
 val languages=listOf("All","Urdu","English","Arabic","Hindi","Bengali","Turkish","Indonesian","French")
 val topics=listOf("Quran recitation","Tafseer","Seerah","Hadith","Islamic reminders","Stories of Prophets","Islamic lectures","Kids & Family")
 fun runSearch(term:String){
  val query=listOf(category.takeUnless{it=="Islamic Videos"}?: "",term,language.takeUnless{it=="All"}?: "").filter{it.isNotBlank()}.joinToString(" ")
  play("https://www.youtube.com/results?search_query="+android.net.Uri.encode(query))
 }
 Base{
  Heading("DISCOVER · YOUTUBE",category)
  Text("Choose a language and topic. Noor-e-Hidayat organizes the search; video playback and availability remain provided by YouTube.",color=Muted,fontSize=12.sp,lineHeight=18.sp)
  Spacer(Modifier.height(12.dp))
  Text("LANGUAGE",color=Gold,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
  LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.fillMaxWidth()){
   items(languages){item->FilterChip(selected=language==item,onClick={language=item},label={Text(item,fontSize=11.sp)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Gold,selectedLabelColor=Emerald,containerColor=Panel,labelColor=Cream))}
  }
  Spacer(Modifier.height(12.dp))
  OutlinedTextField(value=searchText,onValueChange={searchText=it},modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Search scholar, Surah or topic",color=Muted)},leadingIcon={Icon(Icons.Default.Search,null,tint=Gold)},colors=fieldColors())
  Button(onClick={runSearch(searchText)},modifier=Modifier.fillMaxWidth().padding(top=8.dp),enabled=searchText.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Emerald)){Icon(Icons.Default.Search,null);Text("  Find videos")}
  Spacer(Modifier.height(16.dp))
  Text("POPULAR TOPICS",color=Gold,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
  LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(top=9.dp,bottom=22.dp)){
   items(topics){topic->
    Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(13.dp)).clickable{runSearch(topic)}.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
     Icon(Icons.Default.PlayCircle,null,tint=Gold)
     Text(topic,color=Cream,fontWeight=FontWeight.Medium,modifier=Modifier.weight(1f).padding(start=12.dp))
     Icon(Icons.Default.ChevronRight,null,tint=Muted)
    }
   }
  }
 }
}

@Composable private fun LiveScreen(open:(String,String)->Unit){Base{Heading("LIVE BROADCASTS","Makkah & Madinah");Text("Official broadcaster pages open inside the app. Stream availability is controlled by the broadcaster.",color=Muted,fontSize=13.sp);Spacer(Modifier.height(18.dp));Tile("Masjid al-Haram · Makkah","Official Saudi broadcaster","🕋"){open("Makkah Live","https://www.mcs.org.sa/_LiveMekka")};Spacer(Modifier.height(12.dp));Tile("Al-Masjid an-Nabawi · Madinah","Official Saudi broadcaster","☾"){open("Madinah Live","https://www.mcs.org.sa/_LiveMadina")}}}
@Composable private fun MoreScreen(go:(String)->Unit){
 Base{
  Heading("YOUR SPACE","More features")
  LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(9.dp),contentPadding=PaddingValues(bottom=20.dp)){
   items(listOf("Learning","Quiz","Qibla","Prayer Times","Offline Audio","Favourites","History","Settings")){item->Tile(item,"Open "+item,"✧"){go(item)}}
  }
 }
}
@Composable private fun LearningScreen(){
 val lessons=listOf(
  Triple("1. Arabic alphabet","Recognise the 28 Arabic letters and their different written forms.","Start by learning each letter's name, shape and basic sound. Practise connecting letters at the beginning, middle and end of words."),
  Triple("2. Harakat and joining","Learn Fathah, Kasrah, Dammah, Sukoon and Shaddah.","Read short combinations slowly. Focus on the vowel sound and whether a letter is sakin or doubled."),
  Triple("3. Makharij al-Huruf","Understand where Arabic letters are articulated.","The articulation points include the throat, tongue, lips and nasal passage. Correct pronunciation needs listening to and correction by a qualified teacher."),
  Triple("4. Noon Sakinah & Tanween","Learn the four core rules: Izhar, Idgham, Iqlab and Ikhfa.","These rules depend on the letter that follows Noon Sakinah or Tanween. Study the letter groups with a qualified teacher and listen to clear recitation examples."),
  Triple("5. Meem Sakinah","Study Ikhfa Shafawi, Idgham Shafawi and Izhar Shafawi.","The following letter determines the rule: Ba for Ikhfa Shafawi, Meem for Idgham Shafawi, and other letters for Izhar Shafawi. Practise with a teacher."),
  Triple("6. Madd (elongation)","Understand natural Madd and the main causes of longer elongation.","Natural Madd is commonly taught as two counts. Other Madd types have different durations depending on the rule and recitation tradition; learn them by listening and guided practice."),
  Triple("7. Qalqalah","Recognise the Qalqalah letters: ق ط ب ج د.","Qalqalah is heard when one of these letters carries Sukoon. Learn the sound without adding a full vowel, and practise the examples with a teacher."),
  Triple("8. Ghunnah","Learn the nasal sound associated with Noon and Meem in their relevant rules.","Listen carefully to the nasal resonance and practise its duration in examples such as Noon or Meem with Shaddah. A teacher should correct the sound."),
  Triple("9. Waqf and Ibtida","Learn how to stop and resume recitation while preserving meaning.","Study the common Mushaf stopping symbols and learn where a pause is appropriate. Do not stop at a place that changes the intended meaning."),
  Triple("10. Daily revision","Build a consistent and realistic practice routine.","Spend 10–15 minutes revising known letters and reading a short passage. Record difficult letters and ask your teacher to correct them.")
 )
 Base{
  LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(top=10.dp,bottom=20.dp)){
   item{
    Heading("BUILD YOUR SKILLS","Quran learning")
    Text("A structured beginner path. Learn gradually, practise daily, and study pronunciation with a qualified Quran teacher.",color=Muted,fontSize=13.sp,lineHeight=20.sp)
   }
   items(lessons){(title,summary,detail)->
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Panel),border=BorderStroke(1.dp,Border)){
     Column(Modifier.padding(15.dp)){
      Text(title,color=Gold,fontWeight=FontWeight.Bold,fontSize=16.sp)
      Text(summary,color=Cream,fontSize=13.sp,lineHeight=19.sp,modifier=Modifier.padding(top=6.dp))
      Text(detail,color=Muted,fontSize=12.sp,lineHeight=18.sp,modifier=Modifier.padding(top=8.dp))
     }
    }
   }
   item{Text("Learning note: This guide is introductory and does not replace a qualified teacher or verified Tajweed instruction.",color=Muted,fontSize=11.sp,lineHeight=16.sp)}
  }
 }
}
private data class Q(val prompt:String,val options:List<String>,val correctIndex:Int)
private val quizData=listOf(
 Q("How many daily obligatory prayers are there?",listOf("Three","Five","Seven"),1),
 Q("Which month do Muslims fast?",listOf("Ramadan","Shawwal","Muharram"),0),
 Q("Which direction do Muslims face in salah?",listOf("Jerusalem","The Kaaba","Madinah"),1),
 Q("What is the first month of the Islamic calendar?",listOf("Ramadan","Muharram","Rajab"),1),
 Q("What is the holy book of Islam?",listOf("Quran","Torah","Psalms"),0),
 Q("How many Surahs are in the Quran?",listOf("99","114","120"),1),
 Q("Which is the first Surah in the Quran?",listOf("Al-Baqarah","Al-Fatiha","Al-Ikhlas"),1),
 Q("Which is the final Surah in the Quran?",listOf("Al-Falaq","Al-Kawthar","An-Nas"),2),
 Q("To whom was the Quran revealed?",listOf("Prophet Musa","Prophet Muhammad ﷺ","Prophet Isa"),1),
 Q("Which angel brought revelation to Prophet Muhammad ﷺ?",listOf("Mikail","Israfil","Jibril"),2),
 Q("Where did the Prophet ﷺ migrate to from Makkah?",listOf("Taif","Madinah","Jerusalem"),1),
 Q("Which month contains the Hajj pilgrimage?",listOf("Dhul-Hijjah","Muharram","Safar"),0),
 Q("Which Eid marks the end of Ramadan?",listOf("Eid al-Adha","Eid al-Fitr","Ashura"),1),
 Q("What is the Friday congregational prayer called?",listOf("Jumu'ah","Witr","Duha"),0),
 Q("How many pillars of Islam are there?",listOf("Four","Five","Six"),1),
 Q("What is Zakat?",listOf("Obligatory charity","A voluntary fast","A pilgrimage"),0),
 Q("In which city is the Kaaba located?",listOf("Madinah","Makkah","Jerusalem"),1),
 Q("Which Surah is the longest in the Quran?",listOf("Al-Imran","Al-Baqarah","An-Nisa"),1),
 Q("How many Juz are in the Quran?",listOf("20","30","40"),1),
 Q("Which Surah has three verses and is commonly identified as the shortest?",listOf("Al-Kawthar","Al-Asr","Al-Fil"),0)
)
@Composable private fun QuizScreen(index:Int,score:Int,answer:Int,choose:(Int)->Unit,next:()->Unit){Base{Heading("KNOWLEDGE CHECK","Islamic quiz");if(index>=quizData.size){Text("Quiz completed",color=Gold,fontSize=20.sp,fontWeight=FontWeight.Bold);Text("Your score: $score / "+quizData.size,color=Cream,fontSize=18.sp,modifier=Modifier.padding(vertical=18.dp));Button(next,Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Emerald)){Text("Play again")}}else{Text("Question "+(index+1)+" of "+quizData.size+" · Score "+score,color=Muted);Spacer(Modifier.height(18.dp));Text(quizData[index].prompt,color=Cream,fontSize=20.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(14.dp));quizData[index].options.forEachIndexed{i,o->Button({choose(i)},Modifier.fillMaxWidth().padding(bottom=8.dp),enabled=answer<0,colors=ButtonDefaults.buttonColors(containerColor=if(answer==i)Gold else Panel,contentColor=if(answer==i)Emerald else Cream)){Text(o)}};if(answer>=0)Text(if(answer==quizData[index].correctIndex)"Correct answer." else "Correct answer: "+quizData[index].options[quizData[index].correctIndex],color=Gold,modifier=Modifier.padding(12.dp));Button(next,Modifier.fillMaxWidth(),enabled=answer>=0,colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Emerald)){Text(if(index==quizData.lastIndex)"Finish quiz" else "Next question")}}}}@Composable private fun FavouritesScreen(data:List<Surah>,open:(Surah)->Unit,toggle:(Surah)->Unit){
 Base{
  LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=20.dp)){
   item{Heading("YOUR LIBRARY","Favourites");if(data.isEmpty())Text("No favourites saved yet.",color=Muted)}
   items(data){s->
    Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(13.dp)).clickable{open(s)}.padding(13.dp),verticalAlignment=Alignment.CenterVertically){
     Column(Modifier.weight(1f)){Text(s.english,color=Cream);Text(s.meaning,color=Muted,fontSize=11.sp)}
     IconButton({toggle(s)}){Icon(Icons.Default.Favorite,contentDescription="Remove",tint=Gold)}
    }
   }
  }
 }
}
@Composable private fun HistoryScreen(history:List<String>){
 Base{
  LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=20.dp)){
   item{Heading("YOUR ACTIVITY","Watch history");if(history.isEmpty())Text("No recently played items yet.",color=Muted)}
   items(history){title->Text(title,color=Cream,modifier=Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(10.dp)).padding(12.dp))}
  }
 }
}
@Composable private fun SettingsScreen(lang:String,change:(String)->Unit,translationEdition:String,changeTranslation:(String)->Unit,editions:List<QuranTranslationEdition>,loading:Boolean,error:String,retry:()->Unit,privacyOptionsRequired:Boolean,onPrivacyOptions:()->Unit){
 var expanded by remember{mutableStateOf(false)}
 Base{
  Heading("PREFERENCES","Settings")
  Text("App language preference",color=Gold)
  listOf("English","اردو","العربية","Bahasa Indonesia","Türkçe","Français").forEach{Row(Modifier.fillMaxWidth().clickable{change(it)}.padding(vertical=13.dp),verticalAlignment=Alignment.CenterVertically){Text(it,color=Cream,modifier=Modifier.weight(1f));if(lang==it)Icon(Icons.Default.Check,null,tint=Gold)};Divider(color=Border)}
  Spacer(Modifier.height(18.dp))
  Text("Quran translation",color=Gold)
  Box{
   OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=Gold)){
    Text(editions.firstOrNull{it.id==translationEdition}?.let{"${it.name} · ${it.language.uppercase()}"}?:translationEdition,modifier=Modifier.weight(1f),maxLines=2)
    Icon(Icons.Default.ArrowDropDown,null)
   }
   DropdownMenu(expanded=expanded,onDismissRequest={expanded=false},modifier=Modifier.heightIn(max=420.dp).background(Deep)){
    editions.forEach{edition->DropdownMenuItem(text={Text("${edition.name} · ${edition.language.uppercase()}",color=Cream)},onClick={changeTranslation(edition.id);expanded=false})}
   }
  }
  if(loading)LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=8.dp),color=Gold)
  if(error.isNotBlank()){Text(error,color=Gold,fontSize=12.sp,modifier=Modifier.padding(top=8.dp));TextButton(onClick=retry){Text("Retry translations",color=Gold)}}
  Text("Choose from text translation editions published by AlQuran Cloud. The app interface language preference is saved separately; full interface localization is not yet implemented.",color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=14.dp))
  if(privacyOptionsRequired){Spacer(Modifier.height(18.dp));Button(onClick=onPrivacyOptions,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Emerald)){Text("Privacy options")}}
 }
}
@SuppressLint("SetJavaScriptEnabled")
@Composable private fun MediaScreen(title:String,url:String,videoId:String){
 val ctx=LocalContext.current
 val audio=url.endsWith(".mp3",ignoreCase=true)
 var webLoading by remember(url){mutableStateOf(true)}
 var webError by remember(url){mutableStateOf("")}
 val webViewRef=remember(url){mutableStateOf<WebView?>(null)}
 var player by remember(url){mutableStateOf<MediaController?>(null)}
 var playbackError by remember(url){mutableStateOf("")}
 var buffering by remember(url){mutableStateOf(false)}
 var playbackRetry by remember(url){mutableIntStateOf(0)}
 DisposableEffect(url,playbackRetry){
  var disposed=false
  var controllerFuture:com.google.common.util.concurrent.ListenableFuture<MediaController>?=null
  player=null
  if(audio&&url.isNotBlank()){
   try{
    val token=SessionToken(ctx,ComponentName(ctx,PlaybackService::class.java))
    val future=MediaController.Builder(ctx,token).buildAsync()
    controllerFuture=future
    future.addListener({
     try{
      val controller=future.get()
      if(!disposed){
       if(controller.currentMediaItem?.localConfiguration?.uri?.toString()!=url){
        val metadata=androidx.media3.common.MediaMetadata.Builder().setTitle(title).setArtist("Noor-e-Hidayat").build()
        controller.setMediaItem(MediaItem.Builder().setUri(url).setMediaMetadata(metadata).build())
        controller.prepare()
       }else if(controller.playbackState==Player.STATE_IDLE)controller.prepare()
       player=controller
       playbackError=""
      }
     }catch(error:Exception){if(!disposed)playbackError="Audio player service could not connect. Retry playback."}
    },ContextCompat.getMainExecutor(ctx))
   }catch(error:Exception){playbackError="Audio player could not be started. Retry playback."}
  }
  onDispose{
   disposed=true
   controllerFuture?.let{MediaController.releaseFuture(it)}
   player=null
  }
 }
 DisposableEffect(player){
  val current=player
  val listener=object:Player.Listener{
   override fun onPlayerError(error:PlaybackException){playbackError="Audio could not be played. Check your connection and retry."}
   override fun onPlaybackStateChanged(state:Int){buffering=state==Player.STATE_BUFFERING;if(state==Player.STATE_READY)playbackError=""}
  }
  current?.addListener(listener)
  onDispose{current?.removeListener(listener)}
 }
 DisposableEffect(url){onDispose{webViewRef.value?.stopLoading();webViewRef.value?.destroy();webViewRef.value=null}}
 Base{
  Column(Modifier.fillMaxWidth().padding(top=10.dp,bottom=14.dp)){
   Text("NOOR PLAYER",color=Gold,fontSize=10.sp,letterSpacing=2.sp,fontWeight=FontWeight.Bold)
   Text(title,color=Cream,fontSize=27.sp,fontWeight=FontWeight.Bold,fontFamily=FontFamily.Serif,modifier=Modifier.padding(top=6.dp))
   Text(if(audio)"Audio playback · Media3" else "In-app video · Provider player",color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=5.dp))
  }
  if(url.contains("youtube",true)||url.contains("youtu.be",true)||url.contains("mcs.org.sa",true)||videoId.isNotBlank()){
   Card(Modifier.fillMaxWidth().weight(1f),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.Black),border=BorderStroke(1.dp,Border)){
    AndroidView(
     factory={c->
      WebView(c).apply{
       webViewRef.value=this
       setBackgroundColor(android.graphics.Color.BLACK)
       settings.javaScriptEnabled=true
       settings.domStorageEnabled=true
       settings.mediaPlaybackRequiresUserGesture=true
       settings.loadsImagesAutomatically=true
       settings.setSupportMultipleWindows(false)
       webChromeClient=object:WebChromeClient(){
        override fun onProgressChanged(view:WebView?,progress:Int){webLoading=progress<100}
       }
       webViewClient=object:WebViewClient(){
        override fun onPageStarted(view:WebView?,url:String?,favicon:android.graphics.Bitmap?){webLoading=true;webError=""}
        override fun onPageFinished(view:WebView?,url:String?){webLoading=false}
        override fun onReceivedError(view:WebView?,request:android.webkit.WebResourceRequest,error:android.webkit.WebResourceError){
         if(request.isForMainFrame){webLoading=false;webError="This page could not be loaded. Check your internet connection and retry."}
        }
        override fun shouldOverrideUrlLoading(view:WebView?,request:android.webkit.WebResourceRequest):Boolean{
         val scheme=request.url.scheme?.lowercase()
         // Keep HTTP(S) navigation inside this WebView; block app/deep-link schemes.
         return scheme!="http" && scheme!="https"
        }
        @Suppress("DEPRECATION")
        override fun shouldOverrideUrlLoading(view:WebView?,url:String):Boolean{
         val scheme=android.net.Uri.parse(url).scheme?.lowercase()
         return scheme!="http" && scheme!="https"
        }
       }
       val target=if(videoId.isNotBlank())"https://www.youtube-nocookie.com/embed/"+videoId+"?playsinline=1&rel=0" else url
       if((target.contains("youtube",true)||target.contains("youtu.be",true))&&target.contains("/embed/")){
        val html="""<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1"></head><body style="margin:0;background:#000"><iframe src="$target" title="YouTube video player" width="100%" height="100%" style="position:absolute;inset:0;border:0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe></body></html>"""
        tag=target
        loadDataWithBaseURL("https://${c.packageName}/",html,"text/html","UTF-8",null)
       }else{
        tag=target
        loadUrl(target)
       }
      }
     },
     update={webView->
      val target=if(videoId.isNotBlank())"https://www.youtube-nocookie.com/embed/"+videoId+"?playsinline=1&rel=0" else url
      if(webView.tag!=target){
       webView.tag=target
       if((target.contains("youtube",true)||target.contains("youtu.be",true))&&target.contains("/embed/")){
        val html="""<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1"></head><body style="margin:0;background:#000"><iframe src="$target" title="YouTube video player" width="100%" height="100%" style="position:absolute;inset:0;border:0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe></body></html>"""
        webView.loadDataWithBaseURL("https://${webView.context.packageName}/",html,"text/html","UTF-8",null)
       }else webView.loadUrl(target)
      }
     },
     modifier=Modifier.fillMaxSize()
    )
   }
   if(webLoading)LinearProgressIndicator(Modifier.fillMaxWidth(),color=Gold)
   if(webError.isNotBlank())Column(Modifier.fillMaxWidth().padding(top=10.dp)){
    Text(webError,color=Gold,fontSize=12.sp)
    TextButton(onClick={webError="";webLoading=true;webViewRef.value?.reload()}){Text("Retry page",color=Gold)}
   }
   Text("Web links stay inside the app. YouTube playback, ads, sign-in and regional restrictions remain controlled by YouTube.",color=Muted,fontSize=11.sp,lineHeight=16.sp,modifier=Modifier.padding(top=10.dp))
  }else if(audio){
   if(buffering||player==null&&playbackError.isBlank())LinearProgressIndicator(Modifier.fillMaxWidth(),color=Gold)
   if(player!=null)AndroidView(factory={c->PlayerView(c).apply{this.player=player;useController=true}},update={it.player=player},modifier=Modifier.fillMaxWidth().height(245.dp))
   if(playbackError.isNotBlank())Column(Modifier.fillMaxWidth().padding(top=10.dp)){
    Text(playbackError,color=Gold,fontSize=12.sp)
    TextButton(onClick={if(player!=null){playbackError="";player?.seekTo(0);player?.prepare();player?.playWhenReady=true}else{playbackError="";playbackRetry++}}){Text("Retry audio",color=Gold)}
   }
   Text("Quran audio streamed from AlQuran Cloud using Media3 ExoPlayer.",color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=10.dp))
  }else{Text("This media link is not supported.",color=Muted)}
 }
}
@Composable private fun AdMobBanner(){
 val context=LocalContext.current
 val width=(LocalConfiguration.current.screenWidthDp-32).coerceAtLeast(1)
 val isPreview=LocalInspectionMode.current
 val adView=remember(context){AdView(context)}
 DisposableEffect(adView){onDispose{adView.destroy()}}
 LaunchedEffect(adView,width){
  if(!isPreview){
   adView.adUnitId="ca-app-pub-3940256099942544/6300978111"
   adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context,width))
   adView.loadAd(AdRequest.Builder().build())
  }
 }
 Column(Modifier.fillMaxWidth().background(Deep),horizontalAlignment=Alignment.CenterHorizontally){
  Text("ADVERTISEMENT",color=Muted,fontSize=8.sp,letterSpacing=1.sp,modifier=Modifier.padding(top=3.dp,bottom=2.dp))
  AndroidView(factory={adView},modifier=Modifier.fillMaxWidth().wrapContentHeight(),update={})
 }
}
@Composable private fun fieldColors()=OutlinedTextFieldDefaults.colors(focusedTextColor=Cream,unfocusedTextColor=Cream,focusedBorderColor=Gold,unfocusedBorderColor=Border,cursorColor=Gold)
