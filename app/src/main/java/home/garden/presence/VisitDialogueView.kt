package home.garden.presence

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** A native, readable visual-novel dialogue box. Reveal never changes line wrapping. */
class VisitDialogueView(context:Context,name:String,symbol:String,accent:Int,onClose:()->Unit):LinearLayout(context){
    private val handler=Handler(Looper.getMainLooper())
    private val ink=Color.rgb(250,242,235)
    private val fullText=TextView(context)
    private val hint=TextView(context)
    private var line=""
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    init {
        orientation=VERTICAL;setPadding(dp(20),dp(16),dp(20),dp(12))
        background=GardenUi.shape(context,GardenUi.night,12)
        elevation=0f
        val nameRow=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL}
        nameRow.addView(TextView(context).apply{text=name;textSize=17f;setTextColor(accent);typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);isAccessibilityHeading=true},LayoutParams(0,-2,1f))
        addView(nameRow,LayoutParams(-1,-2))
        addView(TextView(context).apply{text=symbol;textSize=10f;letterSpacing=.12f;setTextColor(Color.rgb(181,195,186));setPadding(0,dp(4),0,dp(10))},LayoutParams(-1,-2))
        addView(View(context).apply{setBackgroundColor(Color.argb(45,Color.red(accent),Color.green(accent),Color.blue(accent)))},LayoutParams(-1,dp(1)))
        fullText.apply {
            tag="visit-dialogue";textSize=22f;setTextColor(ink);typeface=Typeface.create("serif",Typeface.NORMAL)
            minLines=2;setLineSpacing(dp(3).toFloat(),1f);setPadding(0,dp(14),0,dp(12))
            // Announce a whole line, never a stream of single letters.
            accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE
            setOnClickListener{revealAll()};isFocusable=true
        }
        addView(fullText,LayoutParams(-1,-2))
        val footer=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL}
        hint.apply{text="No reply needed";textSize=11f;setTextColor(Color.rgb(189,172,185))}
        footer.addView(hint,LayoutParams(0,-2,1f))
        footer.addView(Button(context).apply {
            text="Close  ›";GardenUi.styleButton(this,dark=true,quiet=true)
            setOnClickListener{onClose()}
        },LayoutParams(dp(94),dp(48)))
        addView(footer,LayoutParams(-1,-2))
    }
    fun showLine(text:String,animate:Boolean){
        handler.removeCallbacksAndMessages(null);line=text;fullText.contentDescription=text
        val accessible=context.getSystemService(AccessibilityManager::class.java).isTouchExplorationEnabled
        if(!animate||accessible){revealAll();return}
        fullText.accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_NONE
        hint.text="Tap the line to reveal · no reply needed"
        val duration=(text.length*12L).coerceIn(600,1200)
        val steps=24
        fun frame(step:Int){
            val count=(text.length*step/steps).coerceAtMost(text.length)
            fullText.text=SpannableString(text).apply{if(count<length)setSpan(ForegroundColorSpan(Color.TRANSPARENT),count,length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)}
            if(step<steps)handler.postDelayed({frame(step+1)},duration/steps)else revealAll()
        }
        frame(0)
    }
    private fun revealAll(){handler.removeCallbacksAndMessages(null);fullText.accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE;fullText.text=line;hint.text="No reply needed"}
    fun currentLine()=line
    fun dispose(){handler.removeCallbacksAndMessages(null)}
    override fun onDetachedFromWindow(){dispose();super.onDetachedFromWindow()}
}
