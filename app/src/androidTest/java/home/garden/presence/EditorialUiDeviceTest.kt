package home.garden.presence

import android.content.Context
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
class EditorialUiDeviceTest {
    private fun find(v:View,tag:String):View? {if(v.tag==tag)return v;if(v is ViewGroup)for(i in 0 until v.childCount)find(v.getChildAt(i),tag)?.let{return it};return null}
    private fun capture(c:Context,name:String){val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot();File(c.getExternalFilesDir(null),"editorial-$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
    @Test fun homeAppearanceAndAlarmSelectorPreserveFunctionality(){
        val c=ApplicationProvider.getApplicationContext<Context>()
        val p=c.getSharedPreferences("sprite_preview",Context.MODE_PRIVATE)
        val hadPaper=p.contains("paper");val paper=p.getBoolean("paper",true);val girl=p.getString("character","vivianne")
        val alarm=AngelaAlarmScheduler.prefs(c);val selection=AngelaAlarmScheduler.selected(c).id;val due=alarm.getLong("due",0)
        try {
            p.edit().putBoolean("paper",true).commit()
            ActivityScenario.launch(MainActivity::class.java).use{scene->
                scene.onActivity{it.selectCharacter("vivianne")};Thread.sleep(2000);capture(c,"home")
                scene.onActivity{a->
                    assertEquals("Vivianne Rose Astralis",(find(a.window.decorView,"home-character-name") as TextView).text.toString())
                    val actions=find(a.window.decorView,"home-movement")!!
                    actions.performClick();assertEquals(View.VISIBLE,find(a.window.decorView,"home-expression-panel")!!.visibility)
                    val link=find(a.window.decorView,"home-alarms")!!;link.requestRectangleOnScreen(Rect(0,0,link.width,link.height),true)
                }
                Thread.sleep(300);capture(c,"home-controls")
                p.edit().putBoolean("paper",false).commit();scene.recreate();Thread.sleep(1300);capture(c,"home-dark")
            }
            ActivityScenario.launch(AngelaAlarmSettingsActivity::class.java).use{scene->
                Thread.sleep(350);capture(c,"mornings")
                for(id in listOf("vivianne","nixia","virelia","zaria","angela"))scene.onActivity{a->
                    val tile=find(a.window.decorView,"choose-$id")!!;tile.performClick();assertTrue(tile.isSelected)
                    assertEquals(id,AngelaAlarmScheduler.selected(c).id)
                    assertTrue((find(a.window.decorView,"alarm-preview") as TextView).text.contains(AlarmCharacters.get(id).shortName))
                    assertEquals("Browsing must preserve scheduled alarm",due,alarm.getLong("due",0))
                }
                scene.onActivity{a->val v=find(a.window.decorView,"alarm-permissions")!!;v.performClick();v.requestRectangleOnScreen(Rect(0,0,v.width,v.height),true)}
                Thread.sleep(300);capture(c,"permissions")
            }
            ActivityScenario.launch(VisitSettingsActivity::class.java).use{Thread.sleep(500);capture(c,"visits")}
        }finally{
            val edit=p.edit().putString("character",girl);if(hadPaper)edit.putBoolean("paper",paper)else edit.remove("paper");edit.commit()
            alarm.edit().putString("selected_character",selection).commit()
        }
    }
}
