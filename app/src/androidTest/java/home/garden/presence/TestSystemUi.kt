package home.garden.presence

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry

/** Dismiss only Android's informational first-use immersive-mode tutorial, not permissions. */
object TestSystemUi {
    fun acknowledgeImmersiveTutorial(){
        val ui=InstrumentationRegistry.getInstrumentation().uiAutomation
        repeat(6){
            val root=ui.rootInActiveWindow
            if(root?.findAccessibilityNodeInfosByText("Viewing full screen")?.isNotEmpty()==true){
                val button=root.findAccessibilityNodeInfosByText("Got it").firstOrNull()
                if(button?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true){Thread.sleep(300);return}
            }
            Thread.sleep(150)
        }
    }
}
