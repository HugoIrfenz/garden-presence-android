package home.garden.presence

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.ZonedDateTime
import java.util.UUID

object AlarmTiming {
    const val SNOOZE_MS=300_000L
    fun snoozeAt(now:Long)=now+SNOOZE_MS
    fun nextTime(now:ZonedDateTime,hour:Int,minute:Int):Long {
        require(hour in 0..23 && minute in 0..59)
        var next=now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if(!next.isAfter(now))next=next.plusDays(1)
        return next.toInstant().toEpochMilli()
    }
    fun deliverable(now:Long,due:Long)=due>0 && now>=due-1000 && now-due<=120_000
}

/** One explicitly scheduled alarm, independent of decorative visits and their quiet hours. */
object AngelaAlarmScheduler {
    const val CHANNEL="angela_alarm_v1"
    const val NOTICE=2401
    const val CHANGED="home.garden.presence.ALARM_CHANGED"
    private const val FIRE="garden.ANGELA_ALARM"
    fun prefs(c:Context)=c.getSharedPreferences("angela_alarm",Context.MODE_PRIVATE)
    // Keep legacy preference/channel/component IDs so installed alarms and permissions migrate.
    fun selected(c:Context)=AlarmCharacters.get(prefs(c).getString("selected_character","angela"))
    fun queued(c:Context)=AlarmCharacters.get(prefs(c).getString("due_character","angela"))
    fun ringing(c:Context)=AlarmCharacters.get(prefs(c).getString("ring_character","angela"))
    fun manager(c:Context)=c.getSystemService(AlarmManager::class.java)
    fun exactAllowed(c:Context)=Build.VERSION.SDK_INT<31 || manager(c).canScheduleExactAlarms()
    fun notificationsAllowed(c:Context):Boolean {
        channel(c)
        val nm=c.getSystemService(NotificationManager::class.java)
        return nm.areNotificationsEnabled() && nm.getNotificationChannel(CHANNEL).importance>=NotificationManager.IMPORTANCE_HIGH
    }
    fun fullScreenAllowed(c:Context)=Build.VERSION.SDK_INT<34 || c.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    fun channel(c:Context){c.getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(CHANNEL,"Garden · wake-up alarms",NotificationManager.IMPORTANCE_HIGH).apply {
            description="Alarms you set, with five-minute snooze. Uses your default alarm tone.";setSound(null,null);enableVibration(false);lockscreenVisibility=android.app.Notification.VISIBILITY_PUBLIC
        })}
    private fun operation(c:Context,token:String)=PendingIntent.getBroadcast(c,2401,Intent(c,AngelaAlarmReceiver::class.java).setAction(FIRE).putExtra("token",token),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun schedule(c:Context,at:Long,characterId:String=selected(c).id):Result<Long> = runCatching {
        check(exactAllowed(c)){"Allow Alarms & reminders first."}
        check(notificationsAllowed(c)){"Allow Garden alarm notifications at high importance first."}
        require(at>System.currentTimeMillis()){ "Choose a future time." }
        val token=UUID.randomUUID().toString()
        val show=PendingIntent.getActivity(c,2402,Intent(c,AngelaAlarmSettingsActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // Schedule first; a rejected snooze must leave the current alarm ringing.
        manager(c).setAlarmClock(AlarmManager.AlarmClockInfo(at,show),operation(c,token))
        prefs(c).edit().putLong("due",at).putString("due_character",AlarmCharacters.get(characterId).id).putString("token",token).putString("status","scheduled").commit()
        at
    }
    fun active(c:Context,token:String?):Boolean {
        val p=prefs(c);val age=System.currentTimeMillis()-p.getLong("ring_started",0)
        return token!=null && token==p.getString("ringing",null) && age in 0..120_000
    }
    fun dismiss(c:Context,token:String?){if(active(c,token))stop(c,"dismissed")}
    fun snooze(c:Context,token:String?):Result<Long> {
        if(!active(c,token))return Result.failure(IllegalStateException("This alarm has already ended."))
        return schedule(c,AlarmTiming.snoozeAt(System.currentTimeMillis()),ringing(c).id).onSuccess{stop(c,"snoozed")}
    }
    fun stop(c:Context,status:String){
        prefs(c).edit().remove("ringing").remove("ring_started").putString("status",status).commit()
        c.stopService(Intent(c,AngelaAlarmService::class.java))
        c.getSystemService(NotificationManager::class.java).cancel(NOTICE)
        c.sendBroadcast(Intent(CHANGED).setPackage(c.packageName))
    }
    fun cancel(c:Context){manager(c).cancel(operation(c,""));prefs(c).edit().remove("due").remove("token").commit();stop(c,"cancelled")}
    fun restore(c:Context){
        val p=prefs(c);val due=p.getLong("due",0)
        // Never ring a stale alarm after a restart. Future snoozes retain their timestamp.
        p.edit().remove("ringing").remove("ring_started").commit()
        if(due>System.currentTimeMillis())schedule(c,due,queued(c).id).onFailure{p.edit().putString("status","permission_needed").apply()}
        else if(due>0)p.edit().remove("due").remove("token").putString("status","missed").apply()
    }
    fun receive(c:Context,intent:Intent){
        if(intent.action!=FIRE)return
        val p=prefs(c);val token=intent.getStringExtra("token") ?: return
        if(token!=p.getString("token",null))return
        val due=p.getLong("due",0)
        if(System.currentTimeMillis()<due-1000)return
        p.edit().remove("due").remove("token").commit()
        if(!AlarmTiming.deliverable(System.currentTimeMillis(),due)){p.edit().putString("status","missed").apply();return}
        p.edit().putString("ringing",token).putString("ring_character",queued(c).id).putLong("ring_started",System.currentTimeMillis()).putString("status","ringing").commit()
        try{c.startForegroundService(Intent(c,AngelaAlarmService::class.java).putExtra("token",token))}
        catch(e:RuntimeException){stop(c,"start_failed")}
    }
    fun scene(c:Context,token:String)=Intent(c,AngelaAlarmActivity::class.java).putExtra("token",token).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    fun notification(c:Context,token:String):android.app.Notification {
        channel(c)
        val open=PendingIntent.getActivity(c,2403,scene(c,token),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun action(name:String)=PendingIntent.getBroadcast(c,if(name=="snooze")2404 else 2405,Intent(c,AngelaAlarmActionReceiver::class.java).setAction(name).putExtra("token",token),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_garden)
            .setContentTitle("${ringing(c).shortName} · time to wake up").setContentText(ringing(c).greeting)
            .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setOngoing(true).setContentIntent(open)
            .setFullScreenIntent(open,true).setTimeoutAfter(120_000)
            .addAction(0,"5 more minutes",action("snooze")).addAction(0,"I'm awake",action("dismiss")).build()
    }
}

class AngelaAlarmReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){AngelaAlarmScheduler.receive(c,i)}}
class AngelaAlarmActionReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){
    val token=i.getStringExtra("token")
    when(i.action){"snooze"->AngelaAlarmScheduler.snooze(c,token);"dismiss"->AngelaAlarmScheduler.dismiss(c,token)}
}}
class AngelaAlarmRestoreReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){
    if(i.action in listOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_MY_PACKAGE_REPLACED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED,AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED))AngelaAlarmScheduler.restore(c)
}}
