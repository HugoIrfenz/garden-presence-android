package home.garden.presence

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.text.DateFormat
import java.util.Date

class AngelaAlarmActivity:Activity(){
    private var preview=false
    private var token:String?=null
    private var registered=false
    private var completed=false
    private lateinit var art:AngelaAlarmArtView
    private lateinit var line:TextView
    private lateinit var snooze:Button
    private lateinit var awake:Button
    private val handler=Handler(Looper.getMainLooper())
    private var pats=0
    private lateinit var character:AlarmCharacter
    private val changed=object:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){if(!preview&&!completed&&!AngelaAlarmScheduler.active(c,token))finish()}}
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun text(value:String,size:Float,color:String="#F4F0EE")=TextView(this).apply{text=value;textSize=size;setTextColor(Color.parseColor(color));gravity=Gravity.CENTER}
    private fun button(value:String,tagValue:String,primary:Boolean=false,quiet:Boolean=false,action:()->Unit)=GardenUi.button(this,value,primary=primary,dark=true,quiet=quiet,action=action).apply{tag=tagValue}
    override fun onCreate(state:Bundle?){
        super.onCreate(state);preview=intent.getBooleanExtra("preview",false);token=intent.getStringExtra("token")
        if(!preview&&!AngelaAlarmScheduler.active(this,token)){finish();return}
        character=if(preview)AlarmCharacters.get(intent.getStringExtra("character") ?: AngelaAlarmScheduler.selected(this).id)else AngelaAlarmScheduler.ringing(this)
        setShowWhenLocked(!preview);setTurnScreenOn(!preview);window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        build()
        ContextCompat.registerReceiver(this,changed,IntentFilter(AngelaAlarmScheduler.CHANGED),ContextCompat.RECEIVER_NOT_EXPORTED);registered=true
        if(preview)handler.postDelayed({finish()},120_000)
    }
    private fun build(){
        val root=FrameLayout(this).apply{setBackgroundColor(Color.parseColor(character.background))};setContentView(root)
        art=AngelaAlarmArtView(this,character).apply{reduced=getSharedPreferences("sprite_preview",MODE_PRIVATE).getBoolean("reduced",false)}
        root.addView(art,FrameLayout.LayoutParams(-1,-1))
        val content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(22),dp(12),dp(22),dp(18))}
        root.addView(content,FrameLayout.LayoutParams(-1,-1))
        ViewCompat.setOnApplyWindowInsetsListener(root){view,insets->val s=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout());view.setPadding(s.left,s.top,s.right,s.bottom);insets}
        val masthead=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(0,dp(8),0,dp(8))}
        val time=text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date()),24f).apply{
            tag="alarm-clock";typeface=Typeface.create("sans-serif-light",Typeface.NORMAL);setPadding(dp(12),dp(8),dp(12),dp(8))
            background=GardenUi.shape(this@AngelaAlarmActivity,Color.parseColor(character.background),8)
        }
        masthead.addView(time,LinearLayout.LayoutParams(-2,-2))
        masthead.addView(text(if(preview)"MORNING PREVIEW" else "GOOD MORNING",10f,"#DBDFDA").apply{gravity=Gravity.END;letterSpacing=.12f},LinearLayout.LayoutParams(0,-2,1f))
        content.addView(masthead)
        content.addView(View(this),LinearLayout.LayoutParams(-1,0,1f))
        // Bottom content sizes naturally so large accessibility text can wrap; art takes remaining space.
        val scroll=ScrollView(this).apply{isFillViewport=false;isVerticalScrollBarEnabled=false}
        val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(18),dp(18),dp(12));background=GardenUi.shape(this@AngelaAlarmActivity,Color.parseColor(character.background),12)}
        scroll.addView(panel)
        content.addView(scroll,LinearLayout.LayoutParams(-1,-2).apply{height=-2})
        val identity=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        identity.addView(text(character.symbol.replace(" / ","  ·  "),10f,character.accent).apply{gravity=Gravity.START;letterSpacing=.1f},LinearLayout.LayoutParams(0,-2,1f))
        identity.addView(button("♡  Head pat","alarm-pat",quiet=true){art.pat();line.text=character.pats[pats++%character.pats.size]}.apply{textSize=12f;contentDescription="Pat ${character.shortName}'s head"},LinearLayout.LayoutParams(-2,-2))
        panel.addView(identity)
        panel.addView(text(character.name,20f).apply{tag="alarm-character-name";gravity=Gravity.START;typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);isAccessibilityHeading=true;setPadding(0,0,0,dp(12))})
        line=text(character.greeting,21f).apply{
            tag="alarm-dialogue";gravity=Gravity.START;typeface=Typeface.create("serif",Typeface.NORMAL);setLineSpacing(dp(2).toFloat(),1f);setPadding(0,0,0,dp(22));accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        panel.addView(line,LinearLayout.LayoutParams(-1,-2))
        snooze=button("5 more minutes","alarm-snooze",primary=true){snoozeNow()}
        panel.addView(snooze,LinearLayout.LayoutParams(-1,-2))
        awake=button(if(preview)"Close preview" else "I'm awake  →","alarm-awake",quiet=true){
            completed=true;if(!preview)AngelaAlarmScheduler.dismiss(this,token)
            finish()
        }
        panel.addView(awake,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)})
        if(preview)panel.addView(text("Silent preview · Snooze sets a real alarm.",11f,"#BFC8C1").apply{setPadding(0,dp(2),0,dp(4))})
        immersive()
    }
    private fun snoozeNow(){
        val result=if(preview)AngelaAlarmScheduler.schedule(this,AlarmTiming.snoozeAt(System.currentTimeMillis()),character.id) else {
            completed=true;AngelaAlarmScheduler.snooze(this,token).onFailure{completed=false}
        }
        result.onSuccess{at->
            completed=true;snooze.isEnabled=false;awake.isEnabled=false;art.pat()
            line.text=character.snoozeLine.format(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(at)))
            handler.postDelayed({finish()},1900)
        }.onFailure{line.text=it.message ?: "I couldn't set the snooze. Please check alarm permissions."}
    }
    private fun immersive(){WindowCompat.getInsetsController(window,window.decorView).apply{systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE;hide(WindowInsetsCompat.Type.systemBars())}}
    override fun onWindowFocusChanged(focus:Boolean){super.onWindowFocusChanged(focus);if(focus)immersive()}
    override fun onResume(){super.onResume();if(::art.isInitialized)art.running=true;if(!preview&&!AngelaAlarmScheduler.active(this,token))finish()}
    override fun onNewIntent(next:Intent){super.onNewIntent(next);setIntent(next);recreate()}
    override fun onPause(){if(::art.isInitialized)art.running=false;super.onPause()}
    override fun onDestroy(){handler.removeCallbacksAndMessages(null);if(registered)unregisterReceiver(changed);if(::art.isInitialized)art.running=false;window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);super.onDestroy()}
}
