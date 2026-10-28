package home.garden.presence

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AngelaAlarmDeviceTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun find(v:View,tag:String):View? {
        if(v.tag==tag)return v
        if(v is ViewGroup)for(i in 0 until v.childCount)find(v.getChildAt(i),tag)?.let{return it}
        return null
    }
    private fun capture(name:String){
        val b=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null),name).outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()
    }
    @Test fun previewAndHeadPatAreVisibleWithoutScheduling(){
        val before=AngelaAlarmScheduler.prefs(context).getLong("due",0)
        for(girl in AlarmCharacters.all)ActivityScenario.launch<AngelaAlarmActivity>(Intent(context,AngelaAlarmActivity::class.java).putExtra("preview",true).putExtra("character",girl.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)).use{scene->
            TestSystemUi.acknowledgeImmersiveTutorial()
            Thread.sleep(1000);capture("${girl.id}-alarm-open.png")
            scene.onActivity{a->
                for(tag in listOf("alarm-pat","alarm-snooze","alarm-awake","alarm-dialogue")){
                    val view=find(a.window.decorView,tag)!!;val rect=Rect();assertTrue(view.getGlobalVisibleRect(rect));assertEquals("No clipped $tag",view.height,rect.height())
                }
                assertEquals(girl.name,(find(a.window.decorView,"alarm-character-name") as TextView).text.toString())
                assertEquals(girl.greeting,(find(a.window.decorView,"alarm-dialogue") as TextView).text.toString())
                find(a.window.decorView,"alarm-pat")!!.performClick()
                assertEquals(girl.pats.first(),(find(a.window.decorView,"alarm-dialogue") as TextView).text.toString())
            }
            Thread.sleep(500);capture("${girl.id}-alarm-pat.png")
        }
        assertEquals(before,AngelaAlarmScheduler.prefs(context).getLong("due",0))
    }
    @Test fun realDeliverySnoozeAndDismiss(){
        assumeTrue("User must grant exact alarms and notifications",AngelaAlarmScheduler.exactAllowed(context)&&AngelaAlarmScheduler.notificationsAllowed(context))
        // Do not overwrite an alarm the user has already set.
        assumeTrue("No user alarm may be overwritten",AngelaAlarmScheduler.prefs(context).getLong("due",0)==0L && AngelaAlarmScheduler.prefs(context).getString("ringing",null)==null)
        val prefs=AngelaAlarmScheduler.prefs(context)
        val previousSelection=AngelaAlarmScheduler.selected(context).id
        try {
            val at=System.currentTimeMillis()+4000
            AngelaAlarmScheduler.schedule(context,at,"nixia").getOrThrow()
            prefs.edit().putString("selected_character","zaria").commit()
            assertEquals("Browsing must not change the queued alarm","nixia",AngelaAlarmScheduler.queued(context).id)
            assertEquals(at,AngelaAlarmScheduler.manager(context).nextAlarmClock.triggerTime)
            val deadline=System.currentTimeMillis()+18000
            while(AngelaAlarmScheduler.prefs(context).getString("ringing",null)==null && System.currentTimeMillis()<deadline)Thread.sleep(200)
            val token=AngelaAlarmScheduler.prefs(context).getString("ringing",null)
            assertNotNull("Actual AlarmManager broadcast received",token)
            assertEquals("Delivery retains scheduled character","nixia",AngelaAlarmScheduler.ringing(context).id)
            assertEquals(0,AngelaAlarmScheduler.prefs(context).getLong("due",0))
            ActivityScenario.launch<AngelaAlarmActivity>(AngelaAlarmScheduler.scene(context,token!!)).use{scene->
                Thread.sleep(750)
                val pressed=System.currentTimeMillis()
                scene.onActivity{a->find(a.window.decorView,"alarm-snooze")!!.performClick()}
                val due=AngelaAlarmScheduler.prefs(context).getLong("due",0)
                assertTrue("Snooze is five minutes from pressing",due in pressed+300_000..System.currentTimeMillis()+300_000)
                assertEquals("Snooze reaches system alarm clock",due,AngelaAlarmScheduler.manager(context).nextAlarmClock.triggerTime)
                assertEquals("Snooze keeps the ringing girl, not the browsed one","nixia",AngelaAlarmScheduler.queued(context).id)
                assertNull("Ringing cleared",AngelaAlarmScheduler.prefs(context).getString("ringing",null))
                Thread.sleep(300);capture("angela-alarm-snoozed.png")
            }
            // A stale notification cannot dismiss the newly queued snooze.
            val due=AngelaAlarmScheduler.prefs(context).getLong("due",0)
            AngelaAlarmScheduler.dismiss(context,token)
            assertEquals(due,AngelaAlarmScheduler.prefs(context).getLong("due",0))
            AngelaAlarmScheduler.cancel(context)
            assertEquals(0,AngelaAlarmScheduler.prefs(context).getLong("due",0))
            // Exercise actual delivery again, then the native I'm awake control.
            AngelaAlarmScheduler.schedule(context,System.currentTimeMillis()+3500).getOrThrow()
            val until=System.currentTimeMillis()+18000
            while(AngelaAlarmScheduler.prefs(context).getString("ringing",null)==null&&System.currentTimeMillis()<until)Thread.sleep(200)
            val next=AngelaAlarmScheduler.prefs(context).getString("ringing",null)!!
            ActivityScenario.launch<AngelaAlarmActivity>(AngelaAlarmScheduler.scene(context,next)).use{scene->
                Thread.sleep(500);scene.onActivity{a->find(a.window.decorView,"alarm-awake")!!.performClick()}
            }
            assertNull(AngelaAlarmScheduler.prefs(context).getString("ringing",null))
        } finally {AngelaAlarmScheduler.cancel(context);prefs.edit().putString("selected_character",previousSelection).commit()}
    }
}
