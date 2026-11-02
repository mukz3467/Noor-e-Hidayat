package com.noorehidayat.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
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
data class Ayah(val number:Int,val arabic:String,val english:String,val urdu:String)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=android.graphics.Color.rgb(6,31,23);window.navigationBarColor=android.graphics.Color.rgb(6,31,23);MobileAds.initialize(this){};setContent{NoorApp()}}
}

private fun prefsHistory(ctx:android.content.Context):List<String> = (ctx.getSharedPreferences("noor_prefs",0).getString("history","")?:"").split("|").filter{it.isNotBlank()}

private suspend fun api(path:String):String=withContext(Dispatchers.IO){
 val c=URL("https://api.alquran.cloud/v1/"+path).openConnection() as HttpURLConnection
 c.connectTimeout=15000;c.readTimeout=20000
 try{if(c.responseCode !in 200..299) error("HTTP "+c.responseCode);c.inputStream.bufferedReader().use{it.readText()}}finally{c.disconnect()}
}
private suspend fun getSurahs():List<Surah>{
 val j=JSONObject(api("surah"));require(j.optInt("code")==200);val a=j.getJSONArray("data")
 return (0 until a.length()).map{i->val o=a.getJSONObject(i);Surah(o.getInt("number"),o.getString("name"),o.getString("englishName"),o.getString("englishNameTranslation"),o.getInt("numberOfAyahs"),o.getString("revelationType"))}
}
private suspend fun getAyahs(n:Int):List<Ayah>{
 val j=JSONObject(api("surah/"+n+"/editions/quran-uthmani,en.sahih,ur.jalandhry"));require(j.optInt("code")==200);val d=j.getJSONArray("data");val ar=d.getJSONObject(0).getJSONArray("ayahs");val en=d.getJSONObject(1).getJSONArray("ayahs");val ur=d.getJSONObject(2).getJSONArray("ayahs")
 return (0 until ar.length()).map{i->Ayah(ar.getJSONObject(i).getInt("numberInSurah"),ar.getJSONObject(i).getString("text"),en.getJSONObject(i).getString("text"),ur.getJSONObject(i).getString("text"))}
}

@Composable private fun NoorApp(){
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
 var quizIndex by remember{mutableIntStateOf(0)}
 var quizScore by remember{mutableIntStateOf(0)}
 var quizAnswer by remember{mutableIntStateOf(-1)}
 var history by remember{mutableStateOf(prefsHistory(ctx))}
 var favs by remember{mutableStateOf(setOf<Int>())}
 LaunchedEffect(Unit){
  favs=prefs.getStringSet("favs",emptySet())?.mapNotNull{it.toIntOrNull()}?.toSet()?:emptySet()
  lang=prefs.getString("lang","English")?:"English"
 }
 LaunchedEffect(surahReloadKey){surahLoading=true;catalogueError="";try{surahs=getSurahs()}catch(e:Exception){catalogueError="Quran catalogue unavailable. Check your internet connection and tap Retry."}finally{surahLoading=false}}
 fun openSurah(s:Surah){selected=s;page="Reader";ayahLoading=true;readerError=""}
 LaunchedEffect(selected,page,ayahReloadKey){val s=selected;if(page=="Reader"&&s!=null){ayahLoading=true;readerError="";try{ayahs=getAyahs(s.number)}catch(e:Exception){readerError="Could not load this surah. Check your connection and tap Retry."}finally{ayahLoading=false}}}
 fun openWeb(title:String,url:String){mediaTitle=title;mediaUrl=url;videoId="";page="Media";history=(listOf(title)+history).distinct().take(30);prefs.edit().putString("history",history.joinToString("|")).apply()}
 BackHandler(page!="Home"){page=when(page){"Reader"->"Quran";"Media"->"Home";"VideoLibrary"->videoParentPage;else->"Home"}}
 MaterialTheme(colorScheme=darkColorScheme(primary=Gold,onPrimary=Emerald,background=Emerald,surface=Deep,onSurface=Cream,secondary=Gold)){
  Scaffold(containerColor=Emerald,bottomBar={
   if(page !in listOf("Reader","Media"))Column { NavigationBar(containerColor=Deep){
    listOf("Home","Quran","Discover","More").forEach{p->
     val selectedTab=when(p){"Home"->page=="Home";"Quran"->page=="Quran";"Discover"->page in listOf("Discover","Recitations","Lectures","Live")||(page=="VideoLibrary"&&videoParentPage=="Discover");else->page in listOf("More","Learning","Quiz","Favourites","History","Settings")}
     NavigationBarItem(selected=selectedTab,onClick={page=when(p){"Discover"->"Discover";"More"->"More";else->p}},icon={Icon(when(p){"Home"->Icons.Default.Home;"Quran"->Icons.Default.MenuBook;"Discover"->Icons.Default.Explore;else->Icons.Default.GridView},null)},label={Text(p,fontSize=10.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Gold,selectedTextColor=Gold,indicatorColor=Panel,unselectedIconColor=Muted,unselectedTextColor=Muted))
    }
   }
    AdMobBanner()
   }
  }){pad->
   Column(Modifier.fillMaxSize().padding(pad)){
    if(page!="Home")Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={page=when(page){"Reader"->"Quran";"Media"->"Home";"VideoLibrary"->videoParentPage;else->"Home"}}){Icon(Icons.Default.ArrowBack,null,tint=Gold)};Text(pageTitle(page,selected,mediaTitle),color=Cream,fontSize=19.sp,fontWeight=FontWeight.SemiBold)}
    when(page){
     "Home"->HomeScreen({page="Quran"},{page="Recitations"},{page="Lectures"},{page="Live"},{page="Learning"},{page="Quiz"},{page="More"})
     "Quran"->QuranScreen(surahs,surahLoading,catalogueError,search,{search=it},{openSurah(it)},{surahReloadKey++})
     "Reader"->ReaderScreen(selected,ayahs,ayahLoading,readerError,favs.contains(selected?.number),{s->favs=if(favs.contains(s.number))favs-s.number else favs+s.number;prefs.edit().putStringSet("favs",favs.map{it.toString()}.toSet()).apply()},{s->openWeb(s.english+" · Mishary Alafasy","https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/"+s.number+".mp3")},{ayahReloadKey++})
     "Discover"->DiscoverScreen{category->videoParentPage="Discover";videoCategory=category;page="VideoLibrary"}
     "Recitations"->RecitationsScreen(surahs,surahLoading,catalogueError,{s->openWeb(s.english+" · Mishary Alafasy","https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/"+s.number+".mp3")},{surahReloadKey++})
     "Lectures"->CategoryScreen("Islamic Lectures",listOf("Quran and Sunnah","Seerah of Prophet Muhammad","Islamic manners and character","Family and daily life","Stories of the Prophets","Islamic history")){videoParentPage="Lectures";videoCategory=it;page="VideoLibrary"}
     "Live"->LiveScreen{t,u->openWeb(t,u)}
     "More"->MoreScreen{page=it}
     "Learning"->LearningScreen()
     "Quiz"->QuizScreen(quizIndex,quizScore,quizAnswer,{a->if(quizIndex<quizData.size&&quizAnswer==-1){if(a==quizData[quizIndex].third)quizScore++;quizAnswer=a}},{if(quizIndex>=quizData.size){quizIndex=0;quizScore=0}else{quizIndex++};quizAnswer=-1})
     "Favourites"->FavouritesScreen(surahs.filter{favs.contains(it.number)},{openSurah(it)},{s->favs=if(favs.contains(s.number))favs-s.number else favs+s.number;prefs.edit().putStringSet("favs",favs.map{it.toString()}.toSet()).apply()})
     "History"->HistoryScreen(history)
     "Settings"->SettingsScreen(lang){lang=it;prefs.edit().putString("lang",it).apply()}
     "VideoLibrary"->VideoLibraryScreen(videoCategory){playlistId->if(playlistId.isNotBlank())openWeb(videoCategory,"https://www.youtube-nocookie.com/embed/videoseries?list="+playlistId+"&playsinline=1")}
     "Media"->MediaScreen(mediaTitle,mediaUrl,videoId)
     else->HomeScreen({page="Quran"},{page="Recitations"},{page="Lectures"},{page="Live"},{page="Learning"},{page="Quiz"},{page="More"})
    }
   }
  }
 }
}
private fun pageTitle(p:String,s:Surah?,t:String)=when(p){"Reader"->s?.english?:"Quran";"Media"->t;"VideoLibrary"->"Video Library";"Quran"->"The Holy Quran";"Recitations"->"Beautiful Recitations";"Lectures"->"Islamic Lectures";"Live"->"Makkah & Madinah";"Learning"->"Quran Learning";"Quiz"->"Islamic Quiz";"Favourites"->"Favourites";"History"->"Watch History";"Settings"->"Settings";else->p}

@Composable private fun Base(content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().background(Emerald).padding(horizontal=18.dp),content=content)}
@Composable private fun Heading(kicker:String,title:String){Text(kicker.uppercase(),color=Gold,fontSize=10.sp,letterSpacing=2.sp);Text(title,color=Cream,fontSize=27.sp,fontWeight=FontWeight.Bold,fontFamily=FontFamily.Serif,modifier=Modifier.padding(top=5.dp,bottom=14.dp))}
@Composable private fun Tile(title:String,sub:String,symbol:String,click:()->Unit){Card(Modifier.clickable{click()},shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Panel),border=BorderStroke(1.dp,Border)){Column(Modifier.fillMaxWidth().padding(15.dp)){Text(symbol,color=Gold,fontSize=25.sp);Spacer(Modifier.height(10.dp));Text(title,color=Cream,fontWeight=FontWeight.SemiBold);Text(sub,color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=4.dp))}}}
@Composable private fun HomeScreen(q:()->Unit,r:()->Unit,l:()->Unit,live:()->Unit,learn:()->Unit,quiz:()->Unit,more:()->Unit){
 Base{Column(Modifier.fillMaxWidth().padding(top=18.dp,bottom=18.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("☾",fontSize=36.sp,color=Gold);Text("NOOR-E-HIDAYAT",fontSize=23.sp,fontWeight=FontWeight.Bold,color=Gold,letterSpacing=2.sp);Text("GUIDANCE · KNOWLEDGE · PEACE",fontSize=9.sp,color=Muted,letterSpacing=2.sp)}
 Heading("ASSALAMU ALAIKUM","Find peace in remembrance")
 Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,Gold.copy(alpha=.4f))){Column(Modifier.fillMaxWidth().padding(18.dp)){Text("DAILY REMINDER",color=Gold,fontSize=10.sp,letterSpacing=2.sp);Text("“Indeed, in the remembrance of Allah do hearts find peace.”",color=Cream,fontSize=18.sp,fontFamily=FontFamily.Serif,textAlign=TextAlign.Center,modifier=Modifier.padding(vertical=12.dp));Text("Quran · 13:28",color=Muted,fontSize=11.sp,modifier=Modifier.align(Alignment.End))}}
 Spacer(Modifier.height(18.dp));Heading("EXPLORE","Your spiritual space")
 androidx.compose.foundation.lazy.grid.LazyVerticalGrid(columns=androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),modifier=Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=18.dp)){
  item{Tile("Quran Tilawat","114 Surahs","۞",q)};item{Tile("Recitations","Listen & reflect","♫",r)};item{Tile("Islamic Lectures","Knowledge & guidance","◉",l)};item{Tile("Makkah & Madinah","Live broadcasts","🕋",live)};item{Tile("Quran Learning","Build your skills","✧",learn)};item{Tile("Islamic Quiz","Test knowledge","❖",quiz)};item{Tile("More Features","Saved content","▦",more)}
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
@Composable private fun ReaderScreen(s:Surah?,data:List<Ayah>,loading:Boolean,error:String,isFav:Boolean,toggleFav:(Surah)->Unit,audio:(Surah)->Unit,retry:()->Unit){
 Base{if(s==null)return@Base;Heading("SURAH "+s.number+" · "+s.type,s.english);Text(s.meaning+" · "+s.ayahs+" Ayahs",color=Muted,fontSize=12.sp);Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){TextButton(onClick={toggleFav(s)}){Icon(if(isFav)Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=Gold);Text(if(isFav)" Saved" else " Save",color=Gold)};Spacer(Modifier.weight(1f));Button({audio(s)},Modifier.padding(vertical=10.dp),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Emerald)){Icon(Icons.Default.PlayArrow,null);Text("  Listen · Mishary Alafasy")}}
 if(loading)LinearProgressIndicator(Modifier.fillMaxWidth(),color=Gold);if(error.isNotEmpty())Column(Modifier.fillMaxWidth().padding(10.dp)){Text(error,color=Gold);TextButton(onClick=retry){Text("Retry",color=Gold)}}
 LazyColumn(contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(data){a->Column(Modifier.fillMaxWidth().background(Deep,RoundedCornerShape(14.dp)).padding(15.dp)){Text("AYAH "+a.number,color=Gold,fontSize=10.sp);Text(a.arabic,color=Cream,fontSize=27.sp,lineHeight=48.sp,textAlign=TextAlign.End,modifier=Modifier.fillMaxWidth().padding(vertical=10.dp),fontFamily=FontFamily.Serif);Divider(color=Border);Text("ENGLISH",color=Gold,fontSize=9.sp,modifier=Modifier.padding(top=9.dp));Text(a.english,color=Cream,fontSize=14.sp,lineHeight=23.sp);Text("اردو ترجمہ",color=Gold,fontSize=11.sp,modifier=Modifier.padding(top=8.dp));Text(a.urdu,color=Cream,fontSize=16.sp,lineHeight=29.sp,textAlign=TextAlign.End,modifier=Modifier.fillMaxWidth())}}}
 }
}
@Composable private fun RecitationsScreen(data:List<Surah>,loading:Boolean,error:String,play:(Surah)->Unit,retry:()->Unit){
 Base{
  Heading("LISTEN & REFLECT","Beautiful recitations")
  Text("Listen to the full surah recitation by Mishary Rashid Alafasy.",color=Muted,fontSize=13.sp)
  if(loading)LinearProgressIndicator(Modifier.fillMaxWidth(),color=Gold)
  if(error.isNotEmpty())Column(Modifier.fillMaxWidth().padding(vertical=12.dp)){Text(error,color=Gold);TextButton(onClick=retry){Text("Retry",color=Gold)}}
  if(!loading&&error.isEmpty()&&data.isEmpty())Text("No recitations are available right now. Check your internet connection and try again.",color=Muted,modifier=Modifier.padding(top=14.dp))
  LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(top=12.dp,bottom=20.dp)){
   items(data){s->Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(13.dp)).clickable{play(s)}.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.PlayCircle,null,tint=Gold);Column(Modifier.weight(1f).padding(start=12.dp)){Text(s.english,color=Cream);Text(s.meaning+" · "+s.ayahs+" Ayahs",color=Muted,fontSize=11.sp)};Text(s.name,color=Gold,fontSize=18.sp)}}
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

@Composable private fun VideoLibraryScreen(category:String,play:(String)->Unit){
 Base{
  Heading("NOOR-E-HIDAYAT VIDEO LIBRARY",category)
  Text("Videos will play inside Noor-e-Hidayat using YouTube's official embedded player. No YouTube API key or our own video hosting is required.",color=Muted,fontSize=13.sp,lineHeight=20.sp)
  Spacer(Modifier.height(16.dp))
  val playlist=curatedPlaylists.firstOrNull{it.category==category && it.playlistId.isNotBlank()}
  if(playlist==null){
   Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Panel),border=BorderStroke(1.dp,Border)){
    Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){
     Icon(Icons.Default.VideoLibrary,null,tint=Gold,modifier=Modifier.size(38.dp))
     Text("Preparing this collection",color=Cream,fontSize=17.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=10.dp))
     Text("There is no verified, embeddable playlist published in this category yet. We will not show guessed IDs or unverified video links.",color=Muted,fontSize=12.sp,lineHeight=18.sp,textAlign=TextAlign.Center,modifier=Modifier.padding(top=7.dp))
    }
   }
  }else{
   Button(onClick={play(playlist.playlistId)},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Emerald)){Icon(Icons.Default.PlayArrow,null);Text("Play collection")}
  }
 }
}

@Composable private fun LiveScreen(open:(String,String)->Unit){Base{Heading("LIVE BROADCASTS","Makkah & Madinah");Text("Official broadcaster pages open inside the app. Stream availability is controlled by the broadcaster.",color=Muted,fontSize=13.sp);Spacer(Modifier.height(18.dp));Tile("Masjid al-Haram · Makkah","Official Saudi broadcaster","🕋"){open("Makkah Live","https://www.mcs.org.sa/_LiveMekka")};Spacer(Modifier.height(12.dp));Tile("Al-Masjid an-Nabawi · Madinah","Official Saudi broadcaster","☾"){open("Madinah Live","https://www.mcs.org.sa/_LiveMadina")}}}
@Composable private fun MoreScreen(go:(String)->Unit){Base{Heading("YOUR SPACE","More features");listOf("Favourites","History","Learning","Quiz","Settings").forEach{Tile(it,"Open "+it,"✧"){go(it)};Spacer(Modifier.height(9.dp))}}}
@Composable private fun LearningScreen(){
 Base{
  Heading("BUILD YOUR SKILLS","Quran learning")
  Text("A structured beginner path. Learn gradually, practise daily, and study pronunciation with a qualified Quran teacher.",color=Muted,fontSize=13.sp,lineHeight=20.sp)
  Spacer(Modifier.height(12.dp))
  val lessons=listOf(
   Triple("1. Arabic alphabet","Recognise the 28 Arabic letters and their different written forms.","Start by learning each letter's name, shape and basic sound. Practise connecting letters at the beginning, middle and end of words."),
   Triple("2. Harakat and joining","Learn Fathah, Kasrah, Dammah, Sukoon and Shaddah.","Read short combinations slowly. Focus on the vowel sound and whether a letter is sakin or doubled."),
   Triple("3. Makharij al-Huruf","Understand where Arabic letters are articulated.","The articulation points include the throat, tongue, lips and nasal passage. Correct pronunciation needs listening to and correction by a qualified teacher."),
   Triple("4. Basic Tajweed","Study Noon Sakinah, Tanween, Meem Sakinah, Madd and Qalqalah.","Learn each rule with examples from a teacher, then practise by listening and repeating. Do not rely on text descriptions alone for pronunciation."),
   Triple("5. Daily revision","Build a consistent and realistic practice routine.","Spend 10–15 minutes revising known letters and reading a short passage. Record difficult letters and ask your teacher to correct them.")
  )
  lessons.forEach{(title,summary,detail)->
   Card(Modifier.fillMaxWidth().padding(bottom=10.dp),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Panel),border=BorderStroke(1.dp,Border)){
    Column(Modifier.padding(15.dp)){
     Text(title,color=Gold,fontWeight=FontWeight.Bold,fontSize=16.sp)
     Text(summary,color=Cream,fontSize=13.sp,lineHeight=19.sp,modifier=Modifier.padding(top=6.dp))
     Text(detail,color=Muted,fontSize=12.sp,lineHeight=18.sp,modifier=Modifier.padding(top=8.dp))
    }
   }
  }
  Text("Learning note: This guide is introductory and does not replace a qualified teacher or a verified Tajweed lesson.",color=Muted,fontSize=11.sp,lineHeight=16.sp,modifier=Modifier.padding(bottom=20.dp))
 }
}
private data class Q(val prompt:String,val options:List<String>,val third:Int)
private val quizData=listOf(Q("How many daily obligatory prayers are there?",listOf("Three","Five","Seven"),1),Q("Which month do Muslims fast?",listOf("Ramadan","Shawwal","Muharram"),0),Q("Which direction do Muslims face in salah?",listOf("Jerusalem","The Kaaba","Madinah"),1),Q("What is the first month of the Islamic calendar?",listOf("Ramadan","Muharram","Rajab"),1),Q("What is the holy book of Islam?",listOf("Quran","Torah","Psalms"),0))
@Composable private fun QuizScreen(index:Int,score:Int,answer:Int,choose:(Int)->Unit,next:()->Unit){Base{Heading("KNOWLEDGE CHECK","Islamic quiz");if(index>=quizData.size){Text("Quiz completed",color=Gold,fontSize=20.sp,fontWeight=FontWeight.Bold);Text("Your score: $score / "+quizData.size,color=Cream,fontSize=18.sp,modifier=Modifier.padding(vertical=18.dp));Button(next,Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Emerald)){Text("Play again")}}else{Text("Question "+(index+1)+" of "+quizData.size+" · Score "+score,color=Muted);Spacer(Modifier.height(18.dp));Text(quizData[index].prompt,color=Cream,fontSize=20.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(14.dp));quizData[index].options.forEachIndexed{i,o->Button({choose(i)},Modifier.fillMaxWidth().padding(bottom=8.dp),colors=ButtonDefaults.buttonColors(containerColor=if(answer==i)Gold else Panel,contentColor=if(answer==i)Emerald else Cream)){Text(o)}};if(answer>=0)Text(if(answer==quizData[index].third)"Correct answer." else "Correct answer: "+quizData[index].options[quizData[index].third],color=Gold,modifier=Modifier.padding(12.dp));Button(next,Modifier.fillMaxWidth(),enabled=answer>=0,colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Emerald)){Text(if(index==quizData.lastIndex)"Finish quiz" else "Next question")}}}}@Composable private fun FavouritesScreen(data:List<Surah>,open:(Surah)->Unit,toggle:(Surah)->Unit){Base{Heading("YOUR LIBRARY","Favourites");if(data.isEmpty())Text("No favourites saved yet.",color=Muted);data.forEach{s->Row(Modifier.fillMaxWidth().padding(vertical=6.dp).background(Panel,RoundedCornerShape(13.dp)).clickable{open(s)}.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(s.english,color=Cream);Text(s.meaning,color=Muted,fontSize=11.sp)};IconButton({toggle(s)}){Icon(Icons.Default.Favorite,contentDescription="Remove",tint=Gold)}}}}}
@Composable private fun HistoryScreen(history:List<String>){Base{Heading("YOUR ACTIVITY","Watch history");if(history.isEmpty())Text("No recently played items in this session.",color=Muted);history.forEach{Text(it,color=Cream,modifier=Modifier.fillMaxWidth().padding(12.dp).background(Panel,RoundedCornerShape(10.dp)).padding(10.dp))}}}
@Composable private fun SettingsScreen(lang:String,change:(String)->Unit){Base{Heading("PREFERENCES","Settings");Text("App language",color=Gold);listOf("English","اردو","العربية","Bahasa Indonesia","Türkçe","Français").forEach{Row(Modifier.fillMaxWidth().clickable{change(it)}.padding(vertical=13.dp),verticalAlignment=Alignment.CenterVertically){Text(it,color=Cream,modifier=Modifier.weight(1f));if(lang==it)Icon(Icons.Default.Check,null,tint=Gold)};Divider(color=Border)};Text("Language choice is saved; full interface translations are not yet implemented.",color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=14.dp))}}
@SuppressLint("SetJavaScriptEnabled")
@Composable private fun MediaScreen(title:String,url:String,videoId:String){
 val ctx=LocalContext.current
 val audio=url.endsWith(".mp3",ignoreCase=true)
 val player= remember(url){
  if(audio&&url.isNotBlank()) ExoPlayer.Builder(ctx).build().apply{setMediaItem(MediaItem.fromUri(url));prepare()} else null
 }
 var playbackError by remember(url){mutableStateOf("")}
 var buffering by remember(url){mutableStateOf(false)}
 DisposableEffect(player){
  val listener=object:Player.Listener{
   override fun onPlayerError(error:PlaybackException){playbackError="Audio could not be played. Check your connection and retry."}
   override fun onPlaybackStateChanged(state:Int){buffering=state==Player.STATE_BUFFERING;if(state==Player.STATE_READY)playbackError=""}
  }
  player?.addListener(listener)
  onDispose{player?.removeListener(listener);player?.release()}
 }
 Base{
  Column(Modifier.fillMaxWidth().padding(top=10.dp,bottom=14.dp)){
   Text("NOOR PLAYER",color=Gold,fontSize=10.sp,letterSpacing=2.sp,fontWeight=FontWeight.Bold)
   Text(title,color=Cream,fontSize=27.sp,fontWeight=FontWeight.Bold,fontFamily=FontFamily.Serif,modifier=Modifier.padding(top=6.dp))
   Text(if(audio)"Audio playback · Media3" else "In-app video · Provider player",color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=5.dp))
  }
  if(url.contains("youtube.com",true)||url.contains("youtu.be",true)||url.contains("mcs.org.sa",true)||videoId.isNotBlank()){
   Card(Modifier.fillMaxWidth().weight(1f),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.Black),border=BorderStroke(1.dp,Border)){
    AndroidView(
     factory={c->
      WebView(c).apply{
       setBackgroundColor(android.graphics.Color.BLACK)
       settings.javaScriptEnabled=true
       settings.domStorageEnabled=true
       settings.mediaPlaybackRequiresUserGesture=true
       settings.loadsImagesAutomatically=true
       settings.setSupportMultipleWindows(false)
       webChromeClient=WebChromeClient()
       webViewClient=object:WebViewClient(){
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
       if(target.contains("youtube.com",true)||target.contains("youtu.be",true)){
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
       if(target.contains("youtube.com",true)||target.contains("youtu.be",true)){
        val html="""<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1"></head><body style="margin:0;background:#000"><iframe src="$target" title="YouTube video player" width="100%" height="100%" style="position:absolute;inset:0;border:0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe></body></html>"""
        webView.loadDataWithBaseURL("https://${webView.context.packageName}/",html,"text/html","UTF-8",null)
       }else webView.loadUrl(target)
      }
     },
     modifier=Modifier.fillMaxSize()
    )
   }
   Text("Web links stay inside the app. YouTube playback, ads, sign-in and regional restrictions remain controlled by YouTube.",color=Muted,fontSize=11.sp,lineHeight=16.sp,modifier=Modifier.padding(top=10.dp))
  }else if(player!=null){
   if(buffering)LinearProgressIndicator(Modifier.fillMaxWidth(),color=Gold)
   AndroidView(factory={c->PlayerView(c).apply{this.player=player;useController=true}},modifier=Modifier.fillMaxWidth().height(245.dp))
   if(playbackError.isNotBlank())Column(Modifier.fillMaxWidth().padding(top=10.dp)){
    Text(playbackError,color=Gold,fontSize=12.sp)
    TextButton(onClick={playbackError="";player.seekTo(0);player.prepare();player.playWhenReady=true}){Text("Retry audio",color=Gold)}
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
