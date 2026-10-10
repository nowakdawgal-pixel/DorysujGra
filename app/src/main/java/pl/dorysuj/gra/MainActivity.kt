package pl.dorysuj.gra

import android.app.Activity
import android.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.os.Bundle
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.media.AudioManager
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

class MainActivity : Activity() {
    private var player: MediaPlayer? = null
    private lateinit var game: GameView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(1024, 1024)
        window.navigationBarColor = Color.rgb(22, 27, 49)
        game = GameView(this)
        setContentView(game)
        if (getSharedPreferences("dorysuj_save", 0).getBoolean("music", true)) startMusic()
    }
    private fun startMusic() {
        try {
            if (player == null) {
                player = MediaPlayer.create(this, R.raw.melody)
                player?.isLooping = true
                player?.setVolume(0.18f, 0.18f)
            }
            player?.start()
        } catch (_: Exception) {}
    }
    fun musicChanged(enabled: Boolean) {
        if (enabled) startMusic() else player?.pause()
    }
    fun celebrate() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            tg.startTone(ToneGenerator.TONE_PROP_ACK, 260)
            game.postDelayed({ tg.release() }, 350)
        } catch (_: Exception) {}
    }
    override fun onPause() { super.onPause(); player?.pause() }
    override fun onResume() { super.onResume(); if (::game.isInitialized && game.musicEnabled()) startMusic() }
    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }
}

private data class Puzzle(val title: String, val hint: String, val kind: Int, val tx: Float, val ty: Float, val ageText: String)

private class GameView(context: Context) : View(context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dorysuj_save", Context.MODE_PRIVATE)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var page = 0 // 0 home, 1 game, 2 win, 3 settings
    private var cat = 0
    private var level = 0
    private var coins = prefs.getInt("coins", 0)
    private var name = prefs.getString("player_name", "") ?: ""
    private var avatar = prefs.getInt("avatar", 0)
    private var music = prefs.getBoolean("music", true)
    private var sounds = prefs.getBoolean("sounds", true)
    private var hintStage = 0
    private var strokes = mutableListOf<Path>()
    private var active: Path? = null
    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var toast = ""
    private var toastUntil = 0L
    private var solvedThisRound = false
    private val cats = listOf("5–10 LAT", "10–15 LAT", "15–20 LAT", "20+ LAT")
    private val descriptions = listOf("Kolorowe i zabawne", "Sport, szkoła i muzyka", "Technologia i hobby", "Dom, auto i podróże")
    private val colors = listOf(0xffff9952.toInt(), 0xff43bea6.toInt(), 0xff6d87ff.toInt(), 0xffb274dc.toInt())
    private val scenes = listOf(
        listOf("Balonik","Kot","Słoneczko","Motyl","Lody","Kwiat","Ryba","Rakieta","Żabka","Tęcza"),
        listOf("Deskorolka","Piłka","Słuchawki","Rower","Plecak","Gitara","But sportowy","Czapka","Konsola","Medal"),
        listOf("Pizza","Telefon","Kubek","Aparat","Laptop","Donut","Słuchawka","Kaktus","Zegar","Książka"),
        listOf("Samochód","Dom","Walizka","Rower","Klucz","Roślina","Lampa","Pociąg","Parasol","Kalendarz")
    )
    private val hints = listOf(
        listOf("Co zwykle łączy się z balonikiem?","Coś wystaje z tyłu kota.","Spójrz na krawędź słońca."),
        listOf("Deskorolka nie pojedzie bez kompletu.","Piłka ma charakterystyczne podziały.","Co łączy obie muszle słuchawek?"),
        listOf("Na pizzy przyda się jeszcze jeden dodatek.","Sprawdź dolną część telefonu.","Kubek ma ucho, żeby łatwiej go trzymać."),
        listOf("Sprawdź, czy auto stoi na komplecie kół.","Co często wystaje ponad dach?","Walizkę łatwiej nieść za uchwyt.")
    )
    private fun totalLevels() = 130
    private fun currentPuzzle(): Puzzle {
        val kind = level % scenes[cat].size
        val title = scenes[cat][kind]
        val hint = hints[cat][kind % hints[cat].size]
        // Target locations move between levels and the hit zone narrows as difficulty rises.
        val x = floatArrayOf(.50f,.78f,.76f,.36f,.62f,.50f,.64f,.50f,.69f,.70f)[kind]
        val y = floatArrayOf(.66f,.67f,.30f,.70f,.52f,.29f,.50f,.72f,.55f,.66f)[kind]
        return Puzzle(title, hint, cat*10+kind, x, y, cats[cat])
    }
    private fun save() {
        prefs.edit().putInt("coins", coins).putInt("level_$cat", level).putString("player_name", name)
            .putInt("avatar", avatar).putBoolean("music", music).putBoolean("sounds", sounds).apply()
    }
    fun musicEnabled() = music
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        c.drawColor(Color.rgb(22, 27, 49))
        when (page) {
            0 -> drawHome(c,w,h)
            1 -> drawPuzzle(c,w,h)
            2 -> drawWin(c,w,h)
            else -> drawSettings(c,w,h)
        }
        if (toast.isNotEmpty() && System.currentTimeMillis() < toastUntil) {
            box(c,w*.12f,h*.42f,w*.88f,h*.54f,0xff303c60.toInt(),24f)
            txt(c,toast,w*.5f,h*.49f,w*.038f,Color.WHITE,true,true)
            postInvalidateDelayed(250)
        } else toast = ""
    }
    private fun txt(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int,bold:Boolean=false,center:Boolean=false) {
        p.color=color;p.textSize=size;p.typeface=if(bold) Typeface.create("sans-serif-rounded",Typeface.BOLD) else Typeface.create("sans-serif",Typeface.NORMAL)
        p.textAlign=if(center) Paint.Align.CENTER else Paint.Align.LEFT;c.drawText(s,x,y,p);p.textAlign=Paint.Align.LEFT
    }
    private fun box(c:Canvas,l:Float,t:Float,r:Float,b:Float,color:Int,rad:Float=24f) {
        p.color=color;p.style=Paint.Style.FILL;c.drawRoundRect(l,t,r,b,rad,rad,p)
    }
    private fun drawHome(c:Canvas,w:Float,h:Float) {
        txt(c,"DORYSUJ!",w/2,h*.10f,w*.105f,Color.WHITE,true,true)
        txt(c,"Narysuj brakujący element!",w/2,h*.145f,w*.037f,0xffc8d1e8.toInt(),false,true)
        val avatarText=if(avatar==0) "👦" else "👧"
        box(c,w*.08f,h*.17f,w*.92f,h*.245f,0xff343e62.toInt(),20f)
        txt(c,"$avatarText  ${if(name.isBlank()) "Witaj, graczu!" else "Cześć, $name!"}",w*.13f,h*.218f,w*.043f,Color.WHITE,true)
        txt(c,"USTAWIENIA ⚙",w*.68f,h*.218f,w*.031f,0xffffd45c.toInt(),true)
        for(i in 0..3) {
            val top=h*(.28f+i*.145f)
            box(c,w*.08f,top,w*.92f,top+h*.115f,colors[i],24f)
            txt(c,cats[i],w*.14f,top+h*.045f,w*.052f,Color.WHITE,true)
            txt(c,descriptions[i],w*.14f,top+h*.082f,w*.032f,Color.WHITE)
            txt(c,"${(prefs.getInt("level_$i",0)).coerceAtMost(130)}/130",w*.84f,top+h*.067f,w*.031f,Color.WHITE,true,true)
        }
        txt(c,"🪙 $coins monet",w/2,h*.91f,w*.043f,0xffffd45c.toInt(),true,true)
        txt(c,"Postęp zapisuje się automatycznie",w/2,h*.955f,w*.028f,0xff9daacb.toInt(),false,true)
    }
    private fun drawPuzzle(c:Canvas,w:Float,h:Float) {
        val z=currentPuzzle()
        txt(c,"‹ MENU",w*.06f,h*.052f,w*.038f,Color.WHITE,true)
        txt(c,"POZIOM ${level+1}",w*.5f,h*.060f,w*.060f,Color.WHITE,true,true)
        txt(c,"🪙 $coins",w*.94f,h*.055f,w*.036f,0xffffd45c.toInt(),true)
        box(c,w*.05f,h*.10f,w*.95f,h*.82f,0xfff8f9ff.toInt(),28f)
        txt(c,z.title,w*.5f,h*.16f,w*.065f,0xff232e4c.toInt(),true,true)
        scene(c,z.kind,w*.5f,h*.47f,w*.68f)
        c.save();c.clipRect(w*.07f,h*.19f,w*.93f,h*.79f)
        p.color=0xff265fe1.toInt();p.strokeWidth=w*.012f;p.style=Paint.Style.STROKE;p.strokeCap=Paint.Cap.ROUND;p.strokeJoin=Paint.Join.ROUND
        strokes.forEach { c.drawPath(it,p) };active?.let{c.drawPath(it,p)};p.style=Paint.Style.FILL;c.restore()
        if(hintStage>0) {
            box(c,w*.10f,h*.715f,w*.90f,h*.79f,0xffffefb1.toInt(),16f)
            val hintText=if(hintStage==1) z.hint else "Zwróć uwagę na brakujący fragment ilustracji."
            txt(c,hintText,w*.5f,h*.76f,w*.031f,0xff463914.toInt(),true,true)
        }
        box(c,w*.08f,h*.85f,w*.92f,h*.925f,0xffffc948.toInt(),22f)
        txt(c,if(hintStage==0) "PODPOWIEDŹ  •  80 🪙" else "KOLEJNA WSKAZÓWKA  •  80 🪙",w*.5f,h*.895f,w*.034f,0xff2d2c37.toInt(),true,true)
        txt(c,"Narysuj brakujący element palcem",w*.5f,h*.96f,w*.03f,0xffbec9e1.toInt(),false,true)
    }
    private fun drawWin(c:Canvas,w:Float,h:Float) {
        drawPuzzle(c,w,h)
        p.color=0xbb0c1223.toInt();c.drawRect(0f,0f,w,h,p)
        box(c,w*.07f,h*.28f,w*.93f,h*.70f,Color.WHITE,32f)
        val cheer=listOf("HURA!","WOW!","ŁUUU!","ALE SZTOS!","BRAWO!")[level%5]
        txt(c,cheer,w*.5f,h*.385f,w*.11f,0xff22a671.toInt(),true,true)
        val greeting=if(name.isBlank()) "Świetna robota!" else "Brawo, $name!"
        txt(c,greeting,w*.5f,h*.455f,w*.052f,0xff303a53.toInt(),true,true)
        txt(c,"+${if((level+1)%10==0) 50 else 10} MONET",w*.5f,h*.525f,w*.062f,0xffe49d19.toInt(),true,true)
        if((level+1)%10==0) txt(c,"NAGRODA ZA ${level+1}. POZIOM!",w*.5f,h*.58f,w*.035f,0xff8546b8.toInt(),true,true)
        txt(c,"Dotknij, aby przejść dalej",w*.5f,h*.64f,w*.036f,0xff303a53.toInt(),false,true)
        // simple celebratory confetti
        for(i in 0..24) {
            p.color=colors[i%colors.size]
            val xx=w*((i*37%97)+1)/100f
            val yy=h*(.08f+(i*19%17)/100f)
            c.drawCircle(xx,yy,w*.009f,p)
        }
    }
    private fun drawSettings(c:Canvas,w:Float,h:Float) {
        txt(c,"USTAWIENIA",w/2,h*.12f,w*.075f,Color.WHITE,true,true)
        txt(c,"‹ WRÓĆ",w*.08f,h*.07f,w*.035f,Color.WHITE,true)
        box(c,w*.08f,h*.20f,w*.92f,h*.32f,0xff343e62.toInt())
        txt(c,"Wpisz imię: $name",w*.14f,h*.26f,w*.042f,Color.WHITE,true)
        txt(c,"Imię zmienisz dotykając tego pola",w*.14f,h*.30f,w*.027f,0xffc8d1e8.toInt())
        box(c,w*.08f,h*.37f,w*.92f,h*.50f,0xff343e62.toInt())
        txt(c,"Awatar",w*.14f,h*.425f,w*.04f,Color.WHITE,true)
        txt(c,"👦  CHŁOPIEC",w*.22f,h*.47f,w*.035f,if(avatar==0) 0xffffd45c.toInt() else Color.WHITE,true)
        txt(c,"👧  DZIEWCZYNKA",w*.62f,h*.47f,w*.035f,if(avatar==1) 0xffffd45c.toInt() else Color.WHITE,true)
        box(c,w*.08f,h*.55f,w*.92f,h*.68f,0xff343e62.toInt())
        txt(c,"Muzyka w tle: ${if(music) "WŁĄCZONA" else "WYŁĄCZONA"}",w*.14f,h*.61f,w*.04f,Color.WHITE,true)
        txt(c,"Dźwięki zwycięstwa: ${if(sounds) "WŁĄCZONE" else "WYŁĄCZONE"}",w*.14f,h*.655f,w*.035f,Color.WHITE,true)
        txt(c,"Dotknij odpowiedniej opcji, aby zmienić",w/2,h*.76f,w*.029f,0xffc8d1e8.toInt(),false,true)
    }
    private fun ln(c:Canvas,x:Float,y:Float,a:Float,b:Float,col:Int,sw:Float) { p.color=col;p.strokeWidth=sw;p.strokeCap=Paint.Cap.ROUND;p.style=Paint.Style.STROKE;c.drawLine(x,y,a,b,p);p.style=Paint.Style.FILL }
    private fun circ(c:Canvas,x:Float,y:Float,r:Float,col:Int) { p.color=col;c.drawCircle(x,y,r,p) }
    private fun scene(c:Canvas,k:Int,x:Float,y:Float,s:Float) {
        val d=0xff2f3b56.toInt();val blue=0xff5b9df5.toInt();val pink=0xffff759f.toInt();val yellow=0xffffc83e.toInt()
        when(k%10) {
            0->{circ(c,x,y,s*.22f,pink);circ(c,x-s*.06f,y-s*.04f,s*.025f,d);circ(c,x+s*.06f,y-s*.04f,s*.025f,d)}
            1->{circ(c,x,y,s*.19f,0xfff0b482.toInt());circ(c,x-s*.13f,y-s*.14f,s*.07f,0xfff0b482.toInt());circ(c,x+s*.13f,y-s*.14f,s*.07f,0xfff0b482.toInt());ln(c,x+s*.13f,y+s*.1f,x+s*.28f,y+s*.15f,0xfff0b482.toInt(),s*.035f)}
            2->{circ(c,x,y,s*.2f,yellow);for(i in 0..7){val a=i*Math.PI/4;if(i!=2)ln(c,x+kotlin.math.cos(a).toFloat()*s*.24f,y+kotlin.math.sin(a).toFloat()*s*.24f,x+kotlin.math.cos(a).toFloat()*s*.34f,y+kotlin.math.sin(a).toFloat()*s*.34f,yellow,s*.03f)}}
            3->{ln(c,x-s*.3f,y+s*.12f,x+s*.3f,y+s*.12f,pink,s*.08f);circ(c,x-s*.2f,y+s*.22f,s*.055f,d);circ(c,x+s*.2f,y+s*.22f,s*.055f,d)}
            4->{circ(c,x,y,s*.22f,blue);circ(c,x-s*.07f,y-s*.04f,s*.08f,Color.WHITE);circ(c,x+s*.09f,y+s*.08f,s*.07f,Color.WHITE)}
            5->{ln(c,x-s*.23f,y+s*.1f,x-s*.23f,y-s*.08f,d,s*.07f);ln(c,x-s*.23f,y-s*.08f,x+s*.23f,y-s*.08f,d,s*.07f);ln(c,x+s*.23f,y-s*.08f,x+s*.23f,y+s*.1f,d,s*.07f)}
            6->{circ(c,x,y,s*.25f,yellow);circ(c,x,y,s*.20f,0xffffe6aa.toInt());circ(c,x-s*.08f,y-s*.04f,s*.04f,pink);circ(c,x+s*.08f,y+s*.06f,s*.04f,0xff46be8c.toInt())}
            7->{box(c,x-s*.17f,y-s*.3f,x+s*.17f,y+s*.3f,d,18f);box(c,x-s*.14f,y-s*.24f,x+s*.14f,y+s*.2f,blue,8f);circ(c,x,y+s*.25f,s*.025f,Color.WHITE)}
            8->{box(c,x-s*.22f,y-s*.18f,x+s*.18f,y+s*.22f,0xfff0875a.toInt());ln(c,x+s*.18f,y-s*.08f,x+s*.32f,y-s*.08f,0xfff0875a.toInt(),s*.045f)}
            else->{box(c,x-s*.32f,y-s*.1f,x+s*.32f,y+s*.16f,blue);circ(c,x-s*.2f,y+s*.17f,s*.07f,d);circ(c,x+s*.2f,y+s*.17f,s*.07f,d)}
        }
    }
    private fun showToast(s:String) { toast=s;toastUntil=System.currentTimeMillis()+1700;invalidate() }
    private fun checkStroke(x:Float,y:Float) {
        val w=width.toFloat();val h=height.toFloat();val z=currentPuzzle()
        val tx=w*z.tx;val ty=h*(.19f+z.ty*.60f)
        val bounds=RectF();active?.computeBounds(bounds,true)
        val length=hypot((x-startX).toDouble(),(y-startY).toDouble()).toFloat()
        // A tap no longer solves a level: require a real stroke with useful length and an endpoint near the missing part.
        val radiusX=w*(.19f - (level/130f)*.09f)
        val radiusY=h*(.16f - (level/130f)*.07f)
        val nearTarget=abs(x-tx)<radiusX && abs(y-ty)<radiusY
        if(length>w*.075f && nearTarget) {
            solvedThisRound=true
            coins+=if((level+1)%10==0) 50 else 10
            save()
            page=2
            if(sounds) (context as? MainActivity)?.celebrate()
        } else {
            showToast("Jeszcze raz! Spróbuj dorysować element.")
            postDelayed({ if(page==1) { strokes.clear();invalidate() } }, 1000)
        }
    }
    override fun onTouchEvent(e:MotionEvent):Boolean {
        val x=e.x;val y=e.y;val w=width.toFloat();val h=height.toFloat()
        when(e.action) {
            MotionEvent.ACTION_DOWN -> {
                startX=x;startY=y;lastX=x;lastY=y
                if(page==0) {
                    if(y>h*.17f&&y<h*.245f&&x>w*.62f){page=3;invalidate();return true}
                    val i=((y/h-.28f)/.145f).toInt()
                    if(i in 0..3&&y>h*.26f&&y<h*.86f){cat=i;level=prefs.getInt("level_$cat",0).coerceIn(0,129);page=1;strokes.clear();hintStage=0;invalidate();return true}
                } else if(page==1) {
                    if(y<h*.08f&&x<w*.25f){page=0;save();invalidate();return true}
                    if(y>h*.84f&&y<h*.94f) {
                        if(coins>=80){coins-=80;hintStage=(hintStage+1).coerceAtMost(2);save()}
                        else showToast("Potrzebujesz 80 monet")
                        invalidate();return true
                    }
                    if(y>h*.19f&&y<h*.79f){active=Path().apply{moveTo(x,y)};return true}
                } else if(page==2) {
                    page=1;level=(level+1).coerceAtMost(129);prefs.edit().putInt("level_$cat",level).apply();strokes.clear();hintStage=0;invalidate();return true
                } else if(page==3) {
                    if(y<h*.10f){page=0;save();invalidate();return true}
                    if(y>h*.20f&&y<h*.33f){
                        val input = EditText(context).apply {
                            setSingleLine(true)
                            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                            setText(name)
                            hint = "Wpisz swoje imię"
                        }
                        AlertDialog.Builder(context)
                            .setTitle("Jak masz na imię?")
                            .setView(input)
                            .setPositiveButton("Zapisz") { _, _ ->
                                name = input.text.toString().trim().take(18)
                                save()
                                invalidate()
                            }
                            .setNegativeButton("Pomiń", null)
                            .show()
                        return true
                    }
                    if(y>h*.37f&&y<h*.50f){avatar=if(x<w*.5f)0 else 1;save();invalidate();return true}
                    if(y>h*.55f&&y<h*.63f){music=!music;save();(context as? MainActivity)?.musicChanged(music);invalidate();return true}
                    if(y>h*.63f&&y<h*.70f){sounds=!sounds;save();invalidate();return true}
                }
            }
            MotionEvent.ACTION_MOVE -> { active?.lineTo(x,y);lastX=x;lastY=y;invalidate() }
            MotionEvent.ACTION_UP -> {
                active?.lineTo(x,y)
                if(page==1&&active!=null){checkStroke(x,y);active?.let{strokes.add(it)}}
                active=null;invalidate()
            }
        }
        return true
    }
}
