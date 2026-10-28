package home.garden.presence

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.View
import kotlin.math.*

/** A dedicated pose with registered eyelid frames and character-specific regional rigging. */
class AngelaAlarmArtView(context:Context,private val character:AlarmCharacter=AlarmCharacters.get("angela")):View(context){
    private val open=context.assets.open("${character.assetPath}/open.webp").use{BitmapFactory.decodeStream(it)}
    private val blink=context.assets.open("${character.assetPath}/blink.webp").use{BitmapFactory.decodeStream(it)}
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val rig=AlarmMotionRig(character.id)
    private val verts=FloatArray(AlarmMotionRig.VERTICES*2)
    private var elapsed=0L
    private var lastTick=0L
    private var patStarted=-10_000L
    private val base=Color.parseColor(character.background)
    private var fade:Shader?=null
    var reduced=false
    var running=true
        set(value){field=value;lastTick=0L;if(value)invalidate()}
    init {importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO}
    fun pat(){patStarted=elapsed;invalidate()}
    fun isPatting()=elapsed-patStarted in 0L..3199L
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int){
        fade=LinearGradient(0f,h*.52f,0f,h*.9f,intArrayOf(Color.TRANSPARENT,Color.argb(215,Color.red(base),Color.green(base),Color.blue(base)),base),floatArrayOf(0f,.55f,1f),Shader.TileMode.CLAMP)
    }
    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        val now=SystemClock.uptimeMillis()
        if(running&&lastTick!=0L)elapsed+=(now-lastTick).coerceAtMost(100L)
        lastTick=now
        val t=elapsed/1000f;val pat=isPatting();val patSeconds=if(pat)(elapsed-patStarted)/1000f else -1f
        val cycle=t%(if(character.id=="nixia")4.7f else 6.3f)
        val closed=pat || (!reduced && (cycle in 4.0f..4.16f || (character.id=="nixia"&&cycle in 4.3f..4.43f)))
        val bitmap=if(closed)blink else open
        // Fit width, preserve the face at the top, and allow only the desk edge to crop.
        val scale=max(width/open.width.toFloat(),height/open.height.toFloat()).coerceAtMost(width/open.width.toFloat()*character.artScale)
        val w=open.width*scale;val h=open.height*scale;val left=(width-w)/2f;val top=(height-h)*character.topBias
        rig.frame(t,patSeconds,reduced,verts)
        for(i in 0 until AlarmMotionRig.VERTICES){verts[i*2]=left+verts[i*2]*w;verts[i*2+1]=top+verts[i*2+1]*h}
        canvas.drawBitmapMesh(bitmap,AlarmMotionRig.COLUMNS,AlarmMotionRig.ROWS,verts,0,null,0,paint)
        // Softly dissolve the illustration into the native scene controls.
        paint.shader=fade
        canvas.drawRect(0f,height*.52f,width.toFloat(),height.toFloat(),paint);paint.shader=null
        if(pat){
            paint.color=Color.parseColor(character.accent);paint.textSize=width*.055f
            for(i in 0..2){val phase=if(reduced).3f else ((now%1800)/1800f+i*.28f)%1f;paint.alpha=((1-phase)*220).toInt();canvas.drawText("♡",width*(.65f+i*.07f),height*(.27f-phase*.12f),paint)}
            paint.alpha=255
        }
        if(running && (!reduced||pat))postInvalidateDelayed(33)
    }
}
