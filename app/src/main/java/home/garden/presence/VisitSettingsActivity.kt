package home.garden.presence

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import org.json.JSONArray
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** User-owned opt-in. No permission is granted or schedule enabled by visiting this screen. */
class VisitSettingsActivity : Activity() {
    private val prefs by lazy { VisitScheduler.prefs(this) }
    private lateinit var root: LinearLayout
    private lateinit var state: TextView
    private lateinit var enabled: Switch
    private var refreshing=false
    private val ink=GardenUi.ink
    private val muted=GardenUi.muted
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        root=GardenUi.screen(this);GardenUi.header(this,root,"LITTLE VISITS")
        label("A little company.",32f).apply {typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);isAccessibilityHeading=true}
        label("A quiet hello while your screen sleeps. Twelve seconds together, then back to your day.",15f,true)
        state=label("",14f).apply {accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE;setPadding(dp(18),dp(18),dp(18),dp(18));background=GardenUi.shape(this@VisitSettingsActivity,Color.parseColor("#E3E8DF"),10)}
        enabled=toggle("Automatic visits",VisitScheduler.enabled(this)) {value ->
            if(!refreshing){
                if(value&&!Settings.canDrawOverlays(this)){
                    refreshing=true;enabled.isChecked=false;refreshing=false
                    explainPermission()
                }else VisitScheduler.setEnabled(this,value)
                refresh()
            }
        }
        button("Display permission  →") {explainPermission()}
        button("Try a screen-off visit  →") {
            if(!Settings.canDrawOverlays(this))explainPermission()
            else AlertDialog.Builder(this).setTitle("One little visit")
                .setMessage("After starting, press your phone’s power button to turn the screen off. Aim: about 20 seconds; Android may delay it. This test expires after 5 minutes.\n\nThis one test ignores quiet hours and the daily limit. It still skips an active phone, a call, Battery Saver, or an Always-on display unless allowed below. Automatic visits stay as you set them.")
                .setPositiveButton("Start test") {_,_ -> VisitScheduler.scheduleTest(this);refresh()}
                .setNegativeButton("Cancel",null).show()
        }
        button("Pause visits & cancel pending test") {VisitScheduler.setEnabled(this,false);refresh()}
        GardenUi.space(this,root,14);GardenUi.rule(this,root)
        label("Your rhythm",23f).apply {typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);isAccessibilityHeading=true}
        choice("Who visits","character",listOf("rotation","vivianne","angela","virelia","zaria","nixia"),listOf("Take turns","Vivianne","Angela","Virelia","Zaria","Nixia"),"rotation")
        numberChoice("Check roughly every","interval",listOf(0,30,60,120),listOf("90–180 minutes","30 minutes","1 hour","2 hours"),120)
        numberChoice("Maximum visits per day","cap",listOf(1,3,6),listOf("1 visit","3 visits","6 visits"),3)
        toggle("Quiet hours",prefs.getBoolean("quiet",true)){prefs.edit().putBoolean("quiet",it).apply();changed()}
        timeChoice("From","quiet_start",1350)
        timeChoice("Until","quiet_end",360)
        toggle("Also visit during Always-on display",prefs.getBoolean("aod",false)){prefs.edit().putBoolean("aod",it).apply();changed()}
        label("By default, only a fully off display qualifies. Visits skip phone use, calls and Battery Saver. Missed visits are skipped; there is no catch-up queue.",13f,true)
        GardenUi.space(this,root,14);GardenUi.rule(this,root)
        label("On your Xiaomi",23f).apply {typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);isAccessibilityHeading=true}
        label("If a test is blocked, App info → Other permissions may include “Show on Lock screen” and “Open new windows while running in the background.” Allow those for Garden Presence if your phone asks. Names vary by HyperOS version. If visits are delayed, check the app’s battery restrictions and background autostart.",14f,true)
        button("Open Garden Presence app info") {startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")))}
        button("Visit history") {showHistory()}
        label("Your phone stays locked. Android controls the screen timeout. Settings are saved only on this phone; visits use approximate timing and stay silent. You can revoke the display permission in Android settings at any time.",12f,true)
        refresh()
    }
    private fun label(text:String,size:Float,secondary:Boolean=false)=TextView(this).apply {
        this.text=text;textSize=size;includeFontPadding=false;setLineSpacing(dp(2).toFloat(),1f);setTextColor(if(secondary)muted else ink);setPadding(0,dp(10),0,dp(10));root.addView(this,LinearLayout.LayoutParams(-1,-2))
    }
    private fun button(text:String,action:()->Unit)=Button(this).apply {
        this.text=text;GardenUi.styleButton(this,quiet=true);gravity=Gravity.CENTER_VERTICAL or Gravity.START
        root.addView(this,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(4),0,dp(4))});setOnClickListener{action()}
    }
    @Suppress("DEPRECATION")
    private fun toggle(text:String,value:Boolean,action:(Boolean)->Unit)=Switch(this).apply {
        this.text=text;textSize=15f;setTextColor(ink);minHeight=dp(56);isChecked=value
        thumbTintList=android.content.res.ColorStateList.valueOf(ink);trackTintList=android.content.res.ColorStateList.valueOf(Color.parseColor("#B6C5B5"))
        root.addView(this,LinearLayout.LayoutParams(-1,-2));setOnCheckedChangeListener{_,v->action(v)}
    }
    private fun choice(title:String,key:String,values:List<String>,labels:List<String>,default:String){
        lateinit var b:Button
        fun caption()="$title · ${labels[values.indexOf(prefs.getString(key,default)).coerceAtLeast(0)]}"
        b=button(caption()){
            AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(labels.toTypedArray(),values.indexOf(prefs.getString(key,default))){dialog,index->
                prefs.edit().putString(key,values[index]).apply();b.text=caption();dialog.dismiss();changed()
            }.setNegativeButton("Cancel",null).show()
        }
    }
    private fun numberChoice(title:String,key:String,values:List<Int>,labels:List<String>,default:Int){
        lateinit var b:Button
        fun caption()="$title · ${labels[values.indexOf(prefs.getInt(key,default)).coerceAtLeast(0)]}"
        b=button(caption()){
            AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(labels.toTypedArray(),values.indexOf(prefs.getInt(key,default))){dialog,index->
                prefs.edit().putInt(key,values[index]).apply();b.text=caption();dialog.dismiss();changed()
            }.setNegativeButton("Cancel",null).show()
        }
    }
    private fun clock(minute:Int)=String.format(Locale.getDefault(),"%02d:%02d",minute/60,minute%60)
    private fun timeChoice(title:String,key:String,default:Int){
        lateinit var b:Button
        b=button("$title · ${clock(prefs.getInt(key,default))}"){
            val current=prefs.getInt(key,default)
            TimePickerDialog(this,{_,hour,minute->
                prefs.edit().putInt(key,hour*60+minute).apply();b.text="$title · ${clock(hour*60+minute)}";changed()
            },current/60,current%60,true).show()
        }
    }
    private fun changed(){if(VisitScheduler.enabled(this))VisitScheduler.scheduleNext(this);refresh()}
    private fun explainPermission(){
        AlertDialog.Builder(this).setTitle("Allow a screen-off visit")
            .setMessage("Android requires “Display over other apps” to let Garden Presence open its brief visit from the background. There is no permanent floating overlay. Choose Garden Presence on the next screen and allow it, then return here. Automatic visits remain off until you enable them.")
            .setPositiveButton("Open Android settings"){_,_->startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))}
            .setNegativeButton("Not now",null).show()
    }
    private fun timestamp(ms:Long)=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(ms))
    private fun describe(code:String):String=when(code.removePrefix("prelaunch_")){
        "test_scheduled"->"One-time test scheduled"
        "test_visible"->"Test appeared on screen"
        "visit_visible"->"Visit appeared on screen"
        "launch_requested"->"Launch requested; appearance not confirmed"
        "appearance_not_confirmed"->"Visit could not become visible before timeout"
        "launch_blocked"->"Android blocked the launch"
        "permission_missing"->"Display permission is missing"
        "skipped_interactive"->"Skipped: you were using the phone"
        "skipped_display_or_aod"->"Skipped: display was not fully off (or AOD is excluded)"
        "skipped_call"->"Skipped: phone was in call/audio communication mode"
        "skipped_battery_saver"->"Skipped: Battery Saver was on"
        "quiet_hours"->"Skipped: quiet hours"
        "daily_limit"->"Skipped: daily limit reached"
        "cooldown"->"Skipped: a visit appeared recently"
        "expired"->"Skipped: Android delivered this too late"
        "disabled","visits_paused"->"Visits paused; pending visits cancelled"
        "visit_finished"->"Visit ended"
        "visit_interrupted"->"Visit closed when the app lost foreground"
        "visit_dismissed"->"Visit dismissed"
        "render_timeout","renderer_error"->"Artwork did not finish loading normally"
        "already_visiting"->"Skipped: another visit was starting"
        else->code.replace('_',' ')
    }
    private fun refresh(){
        if(!::state.isInitialized)return
        refreshing=true;enabled.isChecked=VisitScheduler.enabled(this);refreshing=false
        val granted=Settings.canDrawOverlays(this)
        val verified=prefs.getLong("confirmed_at",0)>0&&prefs.getString("verified_build",null)==Build.FINGERPRINT
        val lines=mutableListOf(if(!granted)"Setup needed · display permission" else if(verified)"A visit has appeared on this phone" else "Permission allowed · screen-off test still needed")
        lines+=if(VisitScheduler.enabled(this))"Automatic visits are on" else "Automatic visits are off"
        val testAt=prefs.getLong("test_at",0)
        if(testAt>0)lines+="Test requested for ${timestamp(testAt)} · may be delayed"
        val next=prefs.getLong("next_at",0)
        if(VisitScheduler.enabled(this)&&next>0)lines+="Next check: approximately ${timestamp(next)}"
        if(prefs.getBoolean("quiet",true)&&prefs.getInt("quiet_start",1350)==prefs.getInt("quiet_end",360))lines+="Matching quiet-hour times mute the entire day."
        prefs.getString("last_result",null)?.let{lines+="Last: ${describe(it)}"}
        state.text=lines.joinToString("\n\n")
    }
    private fun showHistory(){
        val history=runCatching{JSONArray(prefs.getString("history","[]"))}.getOrDefault(JSONArray())
        val lines=(0 until minOf(15,history.length())).map {i->val entry=history.getJSONObject(i);"${timestamp(entry.optLong("time"))} · ${entry.optString("character")}\n${describe(entry.optString("result"))}"}
        AlertDialog.Builder(this).setTitle("Recent visits").setMessage(if(lines.isEmpty())"No visits yet." else lines.joinToString("\n\n")).setPositiveButton("Close",null).show()
    }
    override fun onResume(){super.onResume();VisitScheduler.refreshSchedule(this);refresh()}
}

