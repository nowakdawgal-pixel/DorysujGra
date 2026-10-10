package pl.dorysuj.gra

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.view.*
import kotlin.math.abs

class MainActivity : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.setFlags(1024,1024)
  window.navigationBarColor=Color.rgb(20,25,45)
  setContentView(GameView(this))
 }
}
private data class Puzzle(val name:String,val hint:String,val kind:Int,val tx:Float,val ty:Float)
private class GameView(ctx:Context):View(ctx) {
 private val prefs:SharedPreferences=ctx.getSharedPreferences("save",0)
 private val p=Paint(3)
 private var page=0; private var cat=0; private var level=0; private var coins=prefs.getInt("coins",0); private var hint=false
 private val strokes=mutableListOf<Path>(); private var active:Path?=null; private var sx=0f; private var sy=0f
 private val cats=listOf("5–10 LAT","10–15 LAT","15–20 LAT","20+ LAT")
 private val colors=listOf(0xffff9952.toInt(),0xff43bea6.toInt(),0xff6d87ff.toInt(),0xffb274dc.toInt())
 private val data=listOf(
  listOf(Puzzle("Balonik","Dorysuj sznurek.",0,.50f,.66f),Puzzle("Kot","Dorysuj ogonek.",1,.78f,.67f),Puzzle("Słoneczko","Dodaj promień.",2,.76f,.30f)),
  listOf(Puzzle("Deskorolka","Brakuje kółka.",3,.36f,.70f),Puzzle("Piłka","Dorysuj linię.",4,.62f,.52f),Puzzle("Słuchawki","Połącz pałąk.",5,.50f,.29f)),
  listOf(Puzzle("Pizza","Dodaj składnik.",6,.64f,.50f),Puzzle("Telefon","Dorysuj przycisk.",7,.50f,.72f),Puzzle("Kubek","Dorysuj ucho.",8,.69f,.55f)),
  listOf(Puzzle("Samochód","Dorysuj koło.",9,.70f,.66f),Puzzle("Dom","Dorysuj komin.",10,.68f,.26f),Puzzle("Walizka","Dorysuj uchwyt.",11,.50f,.28f)))
 private fun save(){prefs.edit().putInt("coins",coins).putInt("level$cat",level).apply()}
 private fun txt(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int,bold:Boolean=false,center:Boolean=false){
  p.color=color;p.textSize=size;p.typeface=if(bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT;p.textAlign=if(center) Paint.Align.CENTER else Paint.Align.LEFT;c.drawText(s,x,y,p);p.textAlign=Paint.Align.LEFT
 }
 private fun box(c:Canvas,l:Float,t:Float,r:Float,b:Float,col:Int,rad:Float=22f){p.color=col;p.style=Paint.Style.FILL;c.drawRoundRect(l,t,r,b,rad,rad,p)}
 override fun onDraw(c:Canvas){
  val w=width.toFloat();val h=height.toFloat();c.drawColor(Color.rgb(20,25,45))
  if(page==0){
   txt(c,"DORYSUJ!",w/2,h*.12f,w*.105f,Color.WHITE,true,true);txt(c,"Dorysuj brakujący element",w/2,h*.17f,w*.04f,0xffbec9e1.toInt(),false,true)
   val desc=listOf("Kolorowe i zabawne","Sport, szkoła i muzyka","Technologia i hobby","Dom, auto i podróże")
   for(i in 0..3){val t=h*(.23f+i*.165f);box(c,w*.08f,t,w*.92f,t+h*.13f,colors[i],25f);txt(c,cats[i],w*.14f,t+h*.052f,w*.055f,Color.WHITE,true);txt(c,desc[i],w*.14f,t+h*.094f,w*.034f,Color.WHITE)}
   txt(c,"Monety: $coins",w/2,h*.93f,w*.045f,0xffffda5a.toInt(),true,true)
  }else{
   val z=data[cat][level%data[cat].size]
   txt(c,"‹ KATEGORIE",w*.06f,h*.055f,w*.037f,Color.WHITE,true);txt(c,"POZIOM ${level+1}",w*.5f,h*.055f,w*.044f,Color.WHITE,true,true);txt(c,"$coins ◉",w*.94f,h*.055f,w*.037f,0xffffda5a.toInt(),true)
   box(c,w*.05f,h*.10f,w*.95f,h*.82f,0xfff8f9ff.toInt(),28f);txt(c,z.name,w/2,h*.16f,w*.065f,0xff232e4c.toInt(),true,true)
   scene(c,z.kind,w*.5f,h*.47f,w*.68f)
   c.save();c.clipRect(w*.07f,h*.19f,w*.93f,h*.79f);p.color=0xff265fe1.toInt();p.strokeWidth=w*.012f;p.style=Paint.Style.STROKE;p.strokeCap=Paint.Cap.ROUND
   strokes.forEach{c.drawPath(it,p)};active?.let{c.drawPath(it,p)};p.style=Paint.Style.FILL;c.restore()
   if(hint){box(c,w*.1f,h*.72f,w*.9f,h*.79f,0xffffefb1.toInt(),15f);txt(c,z.hint,w*.5f,h*.765f,w*.035f,0xff463914.toInt(),true,true)}
   box(c,w*.08f,h*.85f,w*.48f,h*.925f,0xff374463.toInt());txt(c,"WYCZYŚĆ",w*.28f,h*.895f,w*.04f,Color.WHITE,true,true)
   box(c,w*.53f,h*.85f,w*.92f,h*.925f,0xffffc948.toInt());txt(c,"PODPOWIEDŹ • 80",w*.725f,h*.895f,w*.032f,0xff2d2c37.toInt(),true,true)
   txt(c,"Narysuj brakujący element palcem",w*.5f,h*.96f,w*.03f,0xffbec9e1.toInt(),false,true)
   if(page==2){p.color=0xbb0c1223.toInt();c.drawRect(0f,0f,w,h,p);box(c,w*.1f,h*.32f,w*.9f,h*.65f,Color.WHITE,28f);txt(c,"ŚWIETNIE!",w/2,h*.42f,w*.085f,0xff22a671.toInt(),true,true);txt(c,"+10 MONET",w/2,h*.50f,w*.06f,0xffe49d19.toInt(),true,true);txt(c,"Dotknij, aby grać dalej",w/2,h*.57f,w*.04f,0xff303a53.toInt(),false,true)}
  }
 }
 private fun ln(c:Canvas,x:Float,y:Float,a:Float,b:Float,col:Int,sw:Float){p.color=col;p.strokeWidth=sw;p.strokeCap=Paint.Cap.ROUND;p.style=Paint.Style.STROKE;c.drawLine(x,y,a,b,p);p.style=Paint.Style.FILL}
 private fun circ(c:Canvas,x:Float,y:Float,r:Float,col:Int){p.color=col;c.drawCircle(x,y,r,p)}
 private fun scene(c:Canvas,k:Int,x:Float,y:Float,s:Float){
  val d=0xff2f3b56.toInt();val blue=0xff5b9df5.toInt();val pink=0xffff759f.toInt();val yellow=0xffffc83e.toInt()
  when(k){
   0->{circ(c,x,y,s*.22f,pink);circ(c,x-s*.06f,y-s*.04f,s*.025f,d);circ(c,x+s*.06f,y-s*.04f,s*.025f,d)}
   1->{circ(c,x,y,s*.19f,0xfff0b482.toInt());circ(c,x-s*.13f,y-s*.14f,s*.07f,0xfff0b482.toInt());circ(c,x+s*.13f,y-s*.14f,s*.07f,0xfff0b482.toInt());ln(c,x+s*.13f,y+s*.1f,x+s*.28f,y+s*.15f,0xfff0b482.toInt(),s*.035f)}
   2->{circ(c,x,y,s*.2f,yellow);for(i in 0..7){val a=i*Math.PI/4;ln(c,x+kotlin.math.cos(a).toFloat()*s*.24f,y+kotlin.math.sin(a).toFloat()*s*.24f,x+kotlin.math.cos(a).toFloat()*s*.34f,y+kotlin.math.sin(a).toFloat()*s*.34f,yellow,s*.03f)}}
   3->{ln(c,x-s*.3f,y+s*.12f,x+s*.3f,y+s*.12f,pink,s*.08f);circ(c,x-s*.2f,y+s*.22f,s*.055f,d);circ(c,x+s*.2f,y+s*.22f,s*.055f,d)}
   4->{circ(c,x,y,s*.22f,blue);circ(c,x-s*.07f,y-s*.04f,s*.08f,Color.WHITE);circ(c,x+s*.09f,y+s*.08f,s*.07f,Color.WHITE)}
   5->{ln(c,x-s*.23f,y+s*.1f,x-s*.23f,y-s*.08f,d,s*.07f);ln(c,x-s*.23f,y-s*.08f,x+s*.23f,y-s*.08f,d,s*.07f);ln(c,x+s*.23f,y-s*.08f,x+s*.23f,y+s*.1f,d,s*.07f)}
   6->{circ(c,x,y,s*.25f,yellow);circ(c,x,y,s*.20f,0xffffe6aa.toInt());circ(c,x-s*.08f,y-s*.04f,s*.04f,pink);circ(c,x+s*.08f,y+s*.06f,s*.04f,0xff46be8c.toInt())}
   7->{box(c,x-s*.17f,y-s*.3f,x+s*.17f,y+s*.3f,d,18f);box(c,x-s*.14f,y-s*.24f,x+s*.14f,y+s*.2f,blue,8f);circ(c,x,y+s*.25f,s*.025f,Color.WHITE)}
   8->{box(c,x-s*.22f,y-s*.18f,x+s*.18f,y+s*.22f,0xfff0875a.toInt());ln(c,x+s*.18f,y-s*.08f,x+s*.32f,y-s*.08f,0xfff0875a.toInt(),s*.045f)}
   9->{box(c,x-s*.32f,y-s*.1f,x+s*.32f,y+s*.16f,blue);circ(c,x-s*.2f,y+s*.17f,s*.07f,d);circ(c,x+s*.2f,y+s*.17f,s*.07f,d)}
   10->{ln(c,x-s*.25f,y+s*.22f,x-s*.25f,y-s*.1f,0xffeeaa6e.toInt(),s*.04f);ln(c,x+s*.25f,y+s*.22f,x+s*.25f,y-s*.1f,0xffeeaa6e.toInt(),s*.04f);ln(c,x-s*.25f,y+s*.22f,x+s*.25f,y+s*.22f,0xffeeaa6e.toInt(),s*.04f);ln(c,x-s*.3f,y-s*.1f,x,y-s*.32f,0xffdc5f5f.toInt(),s*.045f);ln(c,x,y-s*.32f,x+s*.3f,y-s*.1f,0xffdc5f5f.toInt(),s*.045f)}
   else->{box(c,x-s*.23f,y-s*.2f,x+s*.23f,y+s*.22f,0xff7891b4.toInt());ln(c,x-s*.12f,y-s*.2f,x-s*.12f,y-s*.3f,d,s*.03f);ln(c,x-s*.12f,y-s*.3f,x+s*.12f,y-s*.3f,d,s*.03f);ln(c,x+s*.12f,y-s*.3f,x+s*.12f,y-s*.2f,d,s*.03f)}
  }
 }
 override fun onTouchEvent(e:MotionEvent):Boolean{
  val x=e.x;val y=e.y;val w=width.toFloat();val h=height.toFloat()
  when(e.action){
   MotionEvent.ACTION_DOWN->{sx=x;sy=y
    if(page==0){val i=((y/h-.23f)/.165f).toInt();if(i in 0..3&&y>h*.21f&&y<h*.88f){cat=i;level=prefs.getInt("level$cat",0)%data[cat].size;page=1;strokes.clear();hint=false;invalidate();return true}}
    else if(page==2){page=1;level=(level+1)%data[cat].size;save();strokes.clear();hint=false;invalidate();return true}
    else {if(y<h*.08f&&x<w*.28f){page=0;invalidate();return true};if(y>h*.84f&&y<h*.94f){if(x<w*.5f)strokes.clear() else if(coins>=80){coins-=80;hint=true;save()};invalidate();return true};if(y>h*.19f&&y<h*.79f){active=Path().apply{moveTo(x,y)};invalidate();return true}}
   }
   MotionEvent.ACTION_MOVE->{active?.lineTo(x,y);invalidate()}
   MotionEvent.ACTION_UP->{active?.lineTo(x,y);active?.let{strokes.add(it)};active=null
    if(page==1){val z=data[cat][level%data[cat].size];val tx=w*z.tx;val ty=h*(.19f+z.ty*.60f);val hit=abs(x-tx)<w*.2f&&abs(y-ty)<h*.17f
     if(hit||strokes.size>=3&&strokes.any{val b=RectF();it.computeBounds(b,true);b.left<tx+w*.2f&&b.right>tx-w*.2f&&b.top<ty+h*.17f&&b.bottom>ty-h*.17f}){coins+=10;save();page=2}
     else {postDelayed({if(page==1){strokes.clear();invalidate()}},900)}
    };invalidate()
   }
  };return true
 }
}
