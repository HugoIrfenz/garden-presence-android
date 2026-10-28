package home.garden.presence

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.lifecycle.Lifecycle
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import android.graphics.Bitmap

@RunWith(AndroidJUnit4::class)
class SpriteDeviceTest {
    private var originalCharacter="vivianne"
    private val prefs get()=ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("sprite_preview",Context.MODE_PRIVATE)
    @Before fun saveCharacter(){originalCharacter=prefs.getString("character","vivianne") ?: "vivianne"}
    @After fun restoreCharacter(){prefs.edit().putString("character",originalCharacter).commit()}
    private fun inspect(scenario: ActivityScenario<MainActivity>): JSONObject {
        val latch=CountDownLatch(1);var answer="null"
        scenario.onActivity { activity -> activity.inspect { answer=it;latch.countDown() } }
        assertTrue("WebView callback timed out",latch.await(5,TimeUnit.SECONDS))
        val decoded=JSONTokener(answer).nextValue()
        return if(decoded is String) JSONObject(decoded) else JSONObject()
    }
    private fun waitFor(scenario: ActivityScenario<MainActivity>, id: String): JSONObject {
        val end=System.currentTimeMillis()+20000
        var state=JSONObject()
        while(System.currentTimeMillis()<end){
            state=inspect(scenario)
            if(state.optString("character")==id && state.optInt("layers")>0)return state
            Thread.sleep(150)
        }
        fail("Sprite did not become ready: $id ($state)");return state
    }
    @Test fun allFiveSpritesLoadLocallyAndSwitchWithoutFallback() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {it.selectCharacter("vivianne")}
            waitFor(scenario,"vivianne")
            val expected=linkedMapOf("angela" to 13,"virelia" to 15,"zaria" to 12,"nixia" to 15,"vivianne" to 12)
            for((id,count) in expected){
                scenario.onActivity { it.selectCharacter(id) }
                val state=waitFor(scenario,id)
                assertEquals(count,state.getInt("layers"));assertFalse(state.getBoolean("fallback"));assertTrue(state.getBoolean("visible"))
            }
            scenario.onActivity { it.selectCharacter("angela");it.selectCharacter("nixia");it.selectCharacter("vivianne") }
            assertEquals("vivianne",waitFor(scenario,"vivianne").getString("character"))
        }
    }
    @Test fun renderingPausesWhenActivityLeavesForegroundAndResumes() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {it.selectCharacter("vivianne")}
            waitFor(scenario,"vivianne");Thread.sleep(300)
            scenario.moveToState(Lifecycle.State.CREATED)
            val paused=inspect(scenario)
            assertTrue("Renderer must pause in background",paused.getBoolean("paused"))
            val clock=paused.getDouble("clock");Thread.sleep(500)
            assertEquals("No hidden animation frames",clock,inspect(scenario).getDouble("clock"),0.01)
            scenario.moveToState(Lifecycle.State.RESUMED);Thread.sleep(300)
            assertFalse(inspect(scenario).getBoolean("paused"))
            assertTrue(inspect(scenario).getDouble("clock")>clock)
        }
    }
    @Test fun manualVisitEndsAndDoesNotReappear() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {it.selectCharacter("vivianne")}
            waitFor(scenario,"vivianne");scenario.onActivity { it.startVisit() }
            Thread.sleep(12500)
            val state=inspect(scenario)
            assertFalse("Character stays hidden after goodbye",state.getBoolean("visible"))
            assertTrue("Finished visit releases animation loop",state.getBoolean("paused"))
            scenario.onActivity { it.selectCharacter("vivianne") }
            assertTrue(waitFor(scenario,"vivianne").getBoolean("visible"))
        }
    }
    @Test fun variedVisitsUseEachGirlsDialogueAndRealLayerGestures(){
        ActivityScenario.launch(MainActivity::class.java).use {scenario->
            for(id in VisitContent.dialogue.keys){
                scenario.onActivity{it.selectCharacter(id)};waitFor(scenario,id)
                scenario.onActivity{it.startVisit()};Thread.sleep(2200)
                val state=inspect(scenario)
                assertEquals(12,state.getInt("gestureCount"))
                assertTrue("Character-specific gesture should be playing: $state",state.getString("motion") in VisitContent.gestures.getValue(id).map{it.id})
                scenario.onActivity{activity->
                    val texts=mutableListOf<String>()
                    fun collect(view:View){if(view is TextView&&view.visibility==View.VISIBLE)texts+=view.text.toString();if(view is ViewGroup)for(i in 0 until view.childCount)collect(view.getChildAt(i))}
                    collect(activity.window.decorView)
                    assertTrue("Native quote must match her authored repertoire",VisitContent.dialogue.getValue(id).any{line->texts.any{it.startsWith(line.text)}})
                }
                val context=ApplicationProvider.getApplicationContext<Context>()
                val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                File(context.getExternalFilesDir(null),"visit-$id.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
            }
        }
    }
}
