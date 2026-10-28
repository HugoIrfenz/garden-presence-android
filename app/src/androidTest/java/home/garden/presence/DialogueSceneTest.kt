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
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DialogueSceneTest {
    @Test fun eachFullScreenSceneHasReadableDialogueAndDoesNotCountAsBackgroundVisit(){
        val context=ApplicationProvider.getApplicationContext<Context>()
        val prefs=VisitScheduler.prefs(context)
        val priorConfirmed=prefs.getLong("confirmed_at",0)
        val priorCount=prefs.getInt("count",0)
        for(id in VisitContent.dialogue.keys){
            val intent=Intent(context,VisitActivity::class.java).putExtra("preview",true).putExtra("character",id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ActivityScenario.launch<VisitActivity>(intent).use {scenario->
                TestSystemUi.acknowledgeImmersiveTutorial()
                // Wait for the asynchronous artwork-ready event and complete text reveal.
                // A fixed 2.8-second delay is unreliable on a cold emulator/WebView.
                var ready=false
                val deadline=android.os.SystemClock.uptimeMillis()+12_000
                while(!ready&&android.os.SystemClock.uptimeMillis()<deadline){
                    scenario.onActivity{activity->
                        fun locate(v:View):TextView?{if(v is TextView&&v.tag=="visit-dialogue")return v;if(v is ViewGroup)for(i in 0 until v.childCount)locate(v.getChildAt(i))?.let{return it};return null}
                        val text=locate(activity.window.decorView)?.text
                        val hidden=(text as? android.text.Spanned)?.getSpans(0,text.length,android.text.style.ForegroundColorSpan::class.java)?.isNotEmpty()==true
                        ready=!hidden&&VisitContent.dialogue.getValue(id).any{it.text==text?.toString()}
                    }
                    if(!ready)Thread.sleep(150)
                }
                assertTrue("Artwork and dialogue must become ready within the scene deadline: $id",ready)
                scenario.onActivity{activity->
                    fun find(view:View):TextView? {
                        if(view is TextView&&view.tag=="visit-dialogue")return view
                        if(view is ViewGroup)for(i in 0 until view.childCount)find(view.getChildAt(i))?.let{return it}
                        return null
                    }
                    val quote=find(activity.window.decorView)
                    assertNotNull("Dialogue box exists for $id",quote)
                    assertTrue("Full authored line must be visible for $id",VisitContent.dialogue.getValue(id).any{it.text==quote!!.text.toString()})
                    val visible=Rect();assertTrue(quote!!.getGlobalVisibleRect(visible))
                    assertEquals("No vertically clipped dialogue",quote.height,visible.height())
                    assertTrue("Font remains comfortably readable",quote.textSize/activity.resources.displayMetrics.scaledDensity>=21)
                }
                val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                File(context.getExternalFilesDir(null),"scene-$id.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
            }
        }
        assertEquals("Preview must not mark lockscreen support verified",priorConfirmed,prefs.getLong("confirmed_at",0))
        assertEquals("Preview must not consume a daily automatic visit",priorCount,prefs.getInt("count",0))
    }
}
