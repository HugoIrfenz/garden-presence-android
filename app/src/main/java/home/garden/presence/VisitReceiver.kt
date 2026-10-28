package home.garden.presence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.UUID

class VisitReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action !in listOf("garden.TEST_VISIT","garden.SCHEDULED_VISIT"))return
        val test=intent.action=="garden.TEST_VISIT"
        val p=VisitScheduler.prefs(context);val key=if(test) "test_token" else "normal_token"
        val token=intent.getStringExtra("token") ?: return
        if(token!=p.getString(key,null))return
        val due=p.getLong(if(test)"test_at" else "next_at",0)
        p.edit().remove(key).remove(if(test)"test_at" else "next_at").commit()
        if(!test)VisitScheduler.scheduleNext(context)
        if(System.currentTimeMillis()-due > (if(test) 5*60_000L else 2*60*60_000L)){
            VisitScheduler.record(context,"expired");return
        }
        VisitScheduler.reason(context,test)?.let { VisitScheduler.record(context,it);return }
        if(System.currentTimeMillis()-p.getLong("ticket_started",0)<20_000){VisitScheduler.record(context,"already_visiting");return}
        val ticket=UUID.randomUUID().toString();val character=VisitScheduler.choose(context)
        p.edit().putString("ticket",ticket).putLong("ticket_started",System.currentTimeMillis())
            .putBoolean("ticket_test",test).putString("ticket_character",character).commit()
        VisitScheduler.record(context,"launch_requested",character)
        try {
            // Direct explicit start uses the user's SYSTEM_ALERT_WINDOW BAL exception.
            // No PendingIntent activity launch, service, or fake alarm notification.
            context.startActivity(Intent(context,VisitActivity::class.java).putExtra("ticket",ticket)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        } catch(e: RuntimeException) {VisitScheduler.record(context,"launch_blocked");p.edit().remove("ticket").apply()}
    }
}

class VisitRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action in listOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_MY_PACKAGE_REPLACED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED)){
            VisitScheduler.prefs(context).edit().remove("ticket").remove("ticket_started").remove("test_token").remove("test_at").apply()
            VisitScheduler.scheduleNext(context)
        }
    }
}
