package home.garden.presence

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AlarmRigDeviceTest {
    private fun art(v:View):AngelaAlarmArtView? {
        if(v is AngelaAlarmArtView)return v
        if(v is ViewGroup)for(i in 0 until v.childCount)art(v.getChildAt(i))?.let{return it}
        return null
    }
    private fun pixels(scene:ActivityScenario<AngelaAlarmActivity>):IntArray {
        var result=IntArray(0)
        scene.onActivity { a->
            val view=art(a.window.decorView)!!
            val b=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(b))
            // Sample every fourth pixel in the visible upper art, away from native controls.
            result=IntArray((view.width/4)*(view.height/8))
            var i=0
            for(y in 0 until view.height/8)for(x in 0 until view.width/4)result[i++]=b.getPixel(x*4,y*4)
            b.recycle()
        }
        return result
    }
    private fun capture(context:Context,name:String){
        val b=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null),name).outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()
    }
    @Test fun allFiveRigsMoveAndRespectPauseAndReducedMotion(){
        val context=ApplicationProvider.getApplicationContext<Context>()
        val before=AngelaAlarmScheduler.prefs(context).getLong("due",0)
        for(girl in AlarmCharacters.all){
            ActivityScenario.launch<AngelaAlarmActivity>(Intent(context,AngelaAlarmActivity::class.java).putExtra("preview",true).putExtra("character",girl.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)).use{scene->
                scene.onActivity{art(it.window.decorView)!!.apply{reduced=false;running=true}}
                Thread.sleep(450)
                val first=pixels(scene);capture(context,"${girl.id}-rig-a.png")
                Thread.sleep(1800)
                val next=pixels(scene);capture(context,"${girl.id}-rig-b.png")
                assertEquals(first.size,next.size)
                assertTrue("${girl.id} must visibly move",first.indices.count{first[it]!=next[it]}>first.size/50)
                scene.onActivity{art(it.window.decorView)!!.running=false}
                val paused=pixels(scene);Thread.sleep(180);assertArrayEquals("Paused artwork is frozen",paused,pixels(scene))
                scene.onActivity{art(it.window.decorView)!!.apply{reduced=true;running=true}}
                val still=pixels(scene);Thread.sleep(180);assertArrayEquals("Reduced motion remains still",still,pixels(scene))
                scene.onActivity{art(it.window.decorView)!!.apply{reduced=false;running=true;pat()}}
                Thread.sleep(800);capture(context,"${girl.id}-rig-pat.png")
            }
        }
        assertEquals("Rig previews never schedule alarms",before,AngelaAlarmScheduler.prefs(context).getLong("due",0))
    }
}
