package home.garden.presence

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.DateFormat
import java.time.ZonedDateTime
import java.util.Date

class AngelaAlarmSettingsActivity:Activity(){
    private lateinit var status:TextView
    private lateinit var clock:TextView
    private lateinit var previewButton:Button
    private lateinit var cancelButton:Button
    private lateinit var permissionSummary:Button
    private lateinit var permissionPanel:LinearLayout
    private val permissionButtons=mutableListOf<Button>()
    private val choices=mutableMapOf<String,LinearLayout>()
    private var selected="angela"
    private fun dp(n:Int)=GardenUi.dp(this,n)
    override fun onCreate(state:Bundle?){
        super.onCreate(state);AngelaAlarmScheduler.channel(this)
        selected=AngelaAlarmScheduler.selected(this).id
        val root=GardenUi.screen(this)
        GardenUi.header(this,root,"MORNINGS")
        root.addView(GardenUi.label(this,"Wake up, together.",32f).apply{typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);isAccessibilityHeading=true})
        GardenUi.space(this,root,10)
        root.addView(GardenUi.label(this,"A familiar face. A gentler start.",15f,GardenUi.muted))
        GardenUi.space(this,root,26)
        val next=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(22),dp(22),dp(22),dp(22));background=GardenUi.shape(this@AngelaAlarmSettingsActivity,GardenUi.night,12)}
        next.addView(GardenUi.eyebrow(this,"YOUR NEXT MORNING",Color.parseColor("#B8C4B9")))
        clock=GardenUi.label(this,"",44f,GardenUi.white).apply{tag="alarm-next-time";typeface=Typeface.create("sans-serif-light",Typeface.NORMAL);setPadding(0,dp(12),0,dp(8))};next.addView(clock)
        status=GardenUi.label(this,"",14f,Color.parseColor("#C9D2CB")).apply{tag="alarm-schedule-status";accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE};next.addView(status)
        root.addView(next,LinearLayout.LayoutParams(-1,-2))
        GardenUi.section(this,root,"WHO WILL GREET YOU?")
        val roster=LinearLayout(this).apply{tag="alarm-character-selector"}
        AlarmCharacters.all.forEach{girl->
            val tile=LinearLayout(this).apply{
                orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(4),dp(4),dp(4),dp(4))
                isClickable=true;isFocusable=true;contentDescription=girl.name;tag="choose-${girl.id}"
                addView(GardenPortrait(this@AngelaAlarmSettingsActivity,girl),LinearLayout.LayoutParams(dp(56),dp(64)))
                addView(GardenUi.label(this@AngelaAlarmSettingsActivity,girl.shortName,12f).apply{setPadding(0,dp(8),0,dp(8));gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,-2))
                setOnClickListener{selected=girl.id;AngelaAlarmScheduler.prefs(this@AngelaAlarmSettingsActivity).edit().putString("selected_character",selected).apply();refresh()}
                accessibilityDelegate=object:View.AccessibilityDelegate(){override fun onInitializeAccessibilityNodeInfo(host:View,info:android.view.accessibility.AccessibilityNodeInfo){super.onInitializeAccessibilityNodeInfo(host,info);info.className="android.widget.RadioButton";info.isCheckable=true;info.isChecked=isSelected}}
            }
            choices[girl.id]=tile;roster.addView(tile,LinearLayout.LayoutParams(dp(68),-2).apply{marginEnd=dp(6)})
        }
        root.addView(HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;addView(roster)},LinearLayout.LayoutParams(-1,-2))
        previewButton=GardenUi.button(this,""){startActivity(Intent(this,AngelaAlarmActivity::class.java).putExtra("preview",true).putExtra("character",selected))}.apply{tag="alarm-preview"}
        root.addView(previewButton,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(14)})
        root.addView(GardenUi.button(this,"Set an alarm",primary=true){
            if(ready())TimePickerDialog(this,{_,h,m->confirm(AlarmTiming.nextTime(ZonedDateTime.now(),h,m))},7,0,android.text.format.DateFormat.is24HourFormat(this)).show()
        }.apply{tag="alarm-set"},LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
        cancelButton=GardenUi.button(this,"Cancel upcoming alarm",quiet=true){AngelaAlarmScheduler.cancel(this);refresh()}
        root.addView(cancelButton,LinearLayout.LayoutParams(-1,-2))
        GardenUi.space(this,root,12);GardenUi.rule(this,root)
        permissionSummary=GardenUi.button(this,"",quiet=true){permissionPanel.visibility=if(permissionPanel.visibility==View.VISIBLE)View.GONE else View.VISIBLE;refresh()}.apply{gravity=Gravity.START;tag="alarm-permissions"}
        root.addView(permissionSummary,LinearLayout.LayoutParams(-1,-2))
        permissionPanel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;visibility=if(AngelaAlarmScheduler.exactAllowed(this@AngelaAlarmSettingsActivity)&&AngelaAlarmScheduler.notificationsAllowed(this@AngelaAlarmSettingsActivity))View.GONE else View.VISIBLE}
        root.addView(permissionPanel)
        fun permission(action:()->Unit){val b=GardenUi.button(this,"",quiet=true,action=action).apply{gravity=Gravity.START;textSize=13f};permissionButtons+=b;permissionPanel.addView(b,LinearLayout.LayoutParams(-1,-2))}
        permission{if(Build.VERSION.SDK_INT>=31&&!AngelaAlarmScheduler.exactAllowed(this))openSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:$packageName")))else refresh()}
        permission{if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),42)
            else openSettings(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName).putExtra(Settings.EXTRA_CHANNEL_ID,AngelaAlarmScheduler.CHANNEL))}
        permission{if(Build.VERSION.SDK_INT>=34&&!AngelaAlarmScheduler.fullScreenAllowed(this))openSettings(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:$packageName")))else refresh()}
        GardenUi.rule(this,root)
        root.addView(GardenUi.button(this,"Try an alarm in 20 seconds  →",quiet=true){
            if(ready())AlertDialog.Builder(this).setTitle("Meet ${AlarmCharacters.get(selected).shortName} at wake-up time")
                .setMessage("Your default alarm tone will ring in about 20 seconds. Lock the phone to check the full-screen scene. Tap 5 more minutes to test a real snooze, or I'm awake to stop it.")
                .setNegativeButton("Cancel",null).setPositiveButton("Start test"){_,_->schedule(System.currentTimeMillis()+20_000)}.show()
        },LinearLayout.LayoutParams(-1,-2))
        GardenUi.space(this,root,14)
        root.addView(GardenUi.label(this,"One upcoming alarm · five-minute snooze",13f))
        GardenUi.space(this,root,8)
        root.addView(GardenUi.label(this,"Uses your phone’s alarm volume and default tone. Sound ends after two minutes. While you’re using your phone, open the alarm notification. Full-screen presentation depends on Android permissions.\n\nNo daily repeat yet. Choosing another girl does not change an alarm you already set.",13f,GardenUi.muted))
        refresh()
    }
    private fun ready():Boolean {
        if(AngelaAlarmScheduler.exactAllowed(this)&&AngelaAlarmScheduler.notificationsAllowed(this))return true
        AlertDialog.Builder(this).setMessage("Enable alarms & reminders and alarm notifications above first. Android requires you to grant these permissions.").setPositiveButton("Okay",null).show();return false
    }
    private fun confirm(at:Long){AlertDialog.Builder(this).setTitle("Set ${AlarmCharacters.get(selected).shortName}'s next alarm?").setMessage("${DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(Date(at))}\nReplaces the pending Garden alarm or snooze.").setNegativeButton("Cancel",null).setPositiveButton("Set alarm"){_,_->schedule(at)}.show()}
    private fun schedule(at:Long){AngelaAlarmScheduler.schedule(this,at,selected).onFailure{Toast.makeText(this,it.message,Toast.LENGTH_LONG).show()};refresh()}
    private fun openSettings(i:Intent){runCatching{startActivity(i)}.onFailure{Toast.makeText(this,"Open this app's permissions in Android settings.",Toast.LENGTH_LONG).show()}}
    private fun refresh(){
        if(!::status.isInitialized)return
        previewButton.text="Preview ${AlarmCharacters.get(selected).shortName}  →"
        for((id,tile) in choices){
            tile.isSelected=id==selected
            tile.background=android.graphics.drawable.StateListDrawable().apply{
                addState(intArrayOf(android.R.attr.state_focused),GardenUi.shape(this@AngelaAlarmSettingsActivity,Color.parseColor("#CEDCC8"),10,GardenUi.ink))
                addState(intArrayOf(android.R.attr.state_pressed),GardenUi.shape(this@AngelaAlarmSettingsActivity,Color.parseColor("#CEDCC8"),10,GardenUi.ink))
                addState(intArrayOf(),GardenUi.shape(this@AngelaAlarmSettingsActivity,if(tile.isSelected)Color.parseColor("#DFE6DC") else Color.TRANSPARENT,10,if(tile.isSelected)GardenUi.ink else null))
            }
        }
        val at=AngelaAlarmScheduler.prefs(this).getLong("due",0)
        clock.text=if(at>0)DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(at)) else "Not set"
        status.text=if(at>0)"${AngelaAlarmScheduler.queued(this).name}\n${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(at))}" else "Your next morning is yours to choose."
        cancelButton.visibility=if(at>0||AngelaAlarmScheduler.prefs(this).getString("ringing",null)!=null)View.VISIBLE else View.GONE
        val flags=listOf(AngelaAlarmScheduler.exactAllowed(this),AngelaAlarmScheduler.notificationsAllowed(this),AngelaAlarmScheduler.fullScreenAllowed(this))
        permissionSummary.text="Alarm permissions · ${flags.count{it}} of 3 ready  ${if(permissionPanel.visibility==View.VISIBLE)"−" else "+"}"
        listOf("Alarms & reminders","Notifications","Full-screen display").forEachIndexed{i,name->permissionButtons[i].text="$name   ${if(flags[i])"✓ Ready" else "Set up →"}"}
    }
    override fun onResume(){super.onResume();if(::status.isInitialized)refresh()}
    override fun onRequestPermissionsResult(code:Int,permissions:Array<out String>,grants:IntArray){super.onRequestPermissionsResult(code,permissions,grants);refresh()}
}
