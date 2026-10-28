package home.garden.presence

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.os.PowerManager
import android.provider.Settings
import android.view.Display
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.random.Random

object VisitScheduler {
    const val STOP = "home.garden.presence.STOP_VISIT"
    val ids = listOf("vivianne", "angela", "virelia", "zaria", "nixia")
    fun prefs(c: Context) = c.getSharedPreferences("ambient_visits", Context.MODE_PRIVATE)
    private fun alarm(c: Context) = c.getSystemService(AlarmManager::class.java)
    private fun operation(c: Context, test: Boolean, token: String = "") = PendingIntent.getBroadcast(
        c, if(test) 1202 else 1201,
        Intent(c, VisitReceiver::class.java).setAction(if(test) "garden.TEST_VISIT" else "garden.SCHEDULED_VISIT")
            .putExtra("token",token).putExtra("test",test), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun enabled(c: Context) = prefs(c).getBoolean("enabled",false)
    fun record(c: Context, result: String, character: String = "") {
        val p=prefs(c);val history=runCatching { JSONArray(p.getString("history","[]")) }.getOrDefault(JSONArray())
        val entry=JSONObject().put("time",System.currentTimeMillis()).put("result",result).put("character",character)
        val next=JSONArray().put(entry);for(i in 0 until minOf(39,history.length()))next.put(history.getJSONObject(i))
        p.edit().putString("history",next.toString()).putString("last_result",result).putLong("last_result_at",System.currentTimeMillis()).apply()
    }
    fun setEnabled(c: Context, value: Boolean) {
        prefs(c).edit().putBoolean("enabled",value).commit()
        if(value) scheduleNext(c) else {
            alarm(c).cancel(operation(c,false));alarm(c).cancel(operation(c,true))
            prefs(c).edit().remove("normal_token").remove("test_token").remove("ticket").remove("ticket_started")
                .remove("next_at").remove("test_at").apply()
            c.sendBroadcast(Intent(STOP).setPackage(c.packageName));record(c,"disabled")
        }
    }
    fun scheduleNext(c: Context) {
        if(!enabled(c))return
        val p=prefs(c)
        val minutes=when(val interval=p.getInt("interval",120)){0->Random.nextInt(90,181);else->interval.coerceIn(30,360)}
        var next=ZonedDateTime.now().plusMinutes(minutes.toLong())
        if(p.getBoolean("quiet",true)){
            val start=p.getInt("quiet_start",22*60+30);val end=p.getInt("quiet_end",6*60)
            if(VisitPolicy.quiet(next.hour*60+next.minute,start,end)){
                var endTime=next.withHour(end/60).withMinute(end%60).withSecond(0)
                if(!endTime.isAfter(next))endTime=endTime.plusDays(1)
                next=endTime.plusMinutes(Random.nextLong(5,31))
            }
        }
        schedule(c,false,next.toInstant().toEpochMilli())
    }
    fun scheduleTest(c: Context) = schedule(c,true,System.currentTimeMillis()+20_000)
    /** OEMs can retain the old power policy on an already queued alarm after a settings change. */
    fun refreshSchedule(c: Context) {
        if(!enabled(c))return
        val at=prefs(c).getLong("next_at",0)
        if(at>System.currentTimeMillis())schedule(c,false,at) else scheduleNext(c)
    }
    private fun schedule(c: Context, test: Boolean, at: Long) {
        val token=UUID.randomUUID().toString();val prefix=if(test) "test" else "normal"
        prefs(c).edit().putString(prefix+"_token",token).putLong(if(test) "test_at" else "next_at",at).commit()
        // Decorative visits use approximate alarms; no full-screen notification or exact-alarm privilege.
        alarm(c).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,operation(c,test,token))
        if(test)record(c,"test_scheduled")
    }
    fun reason(c: Context, test: Boolean): String? {
        val p=prefs(c);val now=ZonedDateTime.now()
        val display=c.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        val state=display?.state ?: Display.STATE_UNKNOWN
        val power=c.getSystemService(PowerManager::class.java)
        val count=if(p.getString("count_day","")==LocalDate.now().toString())p.getInt("count",0)else 0
        return VisitPolicy.skipReason(enabled(c),test,power.isInteractive,state==Display.STATE_OFF,
            state==Display.STATE_DOZE || state==Display.STATE_DOZE_SUSPEND,p.getBoolean("aod",false),Settings.canDrawOverlays(c),
            p.getBoolean("quiet",true)&&VisitPolicy.quiet(now.hour*60+now.minute,p.getInt("quiet_start",1350),p.getInt("quiet_end",360)),
            power.isPowerSaveMode,c.getSystemService(AudioManager::class.java).mode!=AudioManager.MODE_NORMAL,
            count,p.getInt("cap",3),System.currentTimeMillis()-p.getLong("last_shown",0))
    }
    fun choose(c: Context): String {
        val p=prefs(c);val chosen=p.getString("character","rotation")
        if(chosen in ids)return chosen!!
        val bag=p.getString("bag","").orEmpty().split(',').filter { it in ids }.toMutableList()
        if(bag.isEmpty()){
            bag.addAll(ids.shuffled())
            if(bag.first()==p.getString("last_character",null))java.util.Collections.swap(bag,0,1)
        }
        val next=bag.removeAt(0);p.edit().putString("bag",bag.joinToString(",")).apply();return next
    }
    fun confirmed(c: Context, character: String, test: Boolean) {
        val p=prefs(c);val day=LocalDate.now().toString()
        val count=if(p.getString("count_day","")==day)p.getInt("count",0)else 0
        p.edit().putLong("last_shown",System.currentTimeMillis()).putString("last_character",character)
            .putString("count_day",day).putInt("count",count+if(test)0 else 1)
            .putLong("confirmed_at",System.currentTimeMillis()).putString("verified_build",android.os.Build.FINGERPRINT).apply()
        record(c,if(test) "test_visible" else "visit_visible",character)
    }
}
