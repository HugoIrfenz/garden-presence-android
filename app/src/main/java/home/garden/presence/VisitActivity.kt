package home.garden.presence

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebSettings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.webkit.WebViewAssetLoader
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewClientCompat
import org.json.JSONObject
import java.io.ByteArrayInputStream

/** Short-lived lock-screen presentation. No service and no ability to unlock the phone. */
class VisitActivity : Activity() {
    private val handler=Handler(Looper.getMainLooper())
    private var web: WebView?=null
    private var test=false
    private var id="vivianne"
    private var ready=false
    private var confirmed=false
    private var registered=false
    private var resumed=false
    private var stopped=false
    private lateinit var dialogue:VisitDialogueView
    private var preview=false
    private var reduced=false
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private val closeReceiver=object: BroadcastReceiver(){
        override fun onReceive(context: Context,intent: Intent){close("visit_dismissed")}
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val p=VisitScheduler.prefs(this)
        // This Activity is not exported. Only the app's foreground preview can set this branch.
        preview=intent.getBooleanExtra("preview",false)
        if(preview){
            id=intent.getStringExtra("character").takeIf{it in VisitScheduler.ids} ?: "vivianne"
        }else{
            val ticket=intent.getStringExtra("ticket")
            if(ticket==null || ticket!=p.getString("ticket",null) || System.currentTimeMillis()-p.getLong("ticket_started",0)>15_000){finish();return}
            test=p.getBoolean("ticket_test",false)
            id=p.getString("ticket_character","vivianne").takeIf { it in VisitScheduler.ids } ?: "vivianne"
            p.edit().remove("ticket").commit()
            VisitScheduler.reason(this,test)?.let {VisitScheduler.record(this,"prelaunch_$it");finish();return}
        }
        reduced=getSharedPreferences("sprite_preview",MODE_PRIVATE).getBoolean("reduced",false)
        // Arm wake before asking the keyguard to recompute visibility.
        setTurnScreenOn(!preview);setShowWhenLocked(!preview)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        buildContent()
        immersive()
        val filter=IntentFilter().apply {addAction(Intent.ACTION_SCREEN_OFF);if(!preview)addAction(Intent.ACTION_USER_PRESENT);addAction(VisitScheduler.STOP)}
        ContextCompat.registerReceiver(this,closeReceiver,filter,ContextCompat.RECEIVER_NOT_EXPORTED)
        registered=true
        // Hard stop includes load time: slow or failed art never keeps this window open indefinitely.
        handler.postDelayed({close(if(confirmed) "visit_finished" else "appearance_not_confirmed")},15_000)
    }
    @SuppressLint("SetJavaScriptEnabled")
    private fun buildContent() {
        val colors=mapOf("vivianne" to "#CF5265","angela" to "#77B8FF","virelia" to "#EE9DAF","zaria" to "#6ED6E8","nixia" to "#9C6CFF")
        val accent=Color.parseColor(colors.getValue(id))
        val atmosphere=Color.rgb(18+Color.red(accent)/11,18+Color.green(accent)/11,27+Color.blue(accent)/11)
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL
            background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(13,15,24),atmosphere,Color.rgb(17,17,26)))
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) {view,insets->
            val safe=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(dp(16)+safe.left,dp(12)+safe.top,dp(16)+safe.right,dp(8)+safe.bottom)
            insets
        }
        setContentView(root)
        val names=mapOf("vivianne" to "Vivianne Rose Astralis","angela" to "Angela Lily Solstice","virelia" to "Virelia Camellia","zaria" to "Zaria Iris","nixia" to "Nixia Orchid")
        val symbols=mapOf("vivianne" to "ROSE  /  MOON","angela" to "LILY  /  SUN","virelia" to "CAMELLIA  /  GROUND","zaria" to "IRIS  /  SKY","nixia" to "ORCHID  /  SEA")
        root.addView(TextView(this).apply {
            text="G A R D E N    /    A LITTLE MOMENT";textSize=10f;setTextColor(Color.rgb(180,166,184));gravity=Gravity.CENTER;setPadding(0,dp(6),0,dp(4))
        },LinearLayout.LayoutParams(-1,-2))
        val local=WebView(this).apply {setBackgroundColor(Color.TRANSPARENT);importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO}
        web=local;root.addView(local,LinearLayout.LayoutParams(-1,0,1f))
        dialogue=VisitDialogueView(this,names.getValue(id),symbols.getValue(id),accent){close("visit_dismissed")}
        // Never leave an empty speech area while the artwork/wake transition settles.
        dialogue.showLine(when(id){"angela"->"Hey, baby. A little moment together.";"virelia"->"H-hello, dear. Just a little company.";"zaria"->"A different little moment, sweetheart.";"nixia"->"Psst. Love! Tiny visitor incoming.";else->"Darling. A little company."},false)
        root.addView(dialogue,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})
        root.addView(Button(this).apply {
            text=if(preview)"Back to the girls" else "Pause future visits";isAllCaps=false;textSize=12f;minHeight=dp(48)
            setTextColor(Color.rgb(191,174,187));backgroundTintList=null;setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener{if(!preview)VisitScheduler.setEnabled(this@VisitActivity,false);close(if(preview)"preview_closed" else "visits_paused")}
        },LinearLayout.LayoutParams(-1,dp(48)))
        val loader=WebViewAssetLoader.Builder().addPathHandler("/assets/",WebViewAssetLoader.AssetsPathHandler(this)).build()
        local.settings.apply {javaScriptEnabled=true;allowFileAccess=false;allowContentAccess=false;domStorageEnabled=false;mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW;mediaPlaybackRequiresUserGesture=true}
        local.addJavascriptInterface(object {
            @JavascriptInterface fun report(raw:String){if(raw.length>4096)return;runOnUiThread {
                if(stopped || isFinishing)return@runOnUiThread
                val e=runCatching {JSONObject(raw)}.getOrNull() ?: return@runOnUiThread
                if(e.optString("type")=="onReady"){
                    ready=true;confirmVisible()
                }
                if(e.optString("type")=="onError")VisitScheduler.record(this@VisitActivity,"renderer_error",id)
            }}
        },"GardenNative")
        local.webViewClient=object:WebViewClientCompat(){
            override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest)=true
            override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse {
                if(request.url.scheme=="https"&&request.url.host=="appassets.androidplatform.net"&&request.url.path.orEmpty().startsWith("/assets/"))loader.shouldInterceptRequest(request.url)?.let{return it}
                return WebResourceResponse("text/plain","UTF-8",403,"Blocked",emptyMap(),ByteArrayInputStream(byteArrayOf()))
            }
            override fun onPageFinished(view:WebView,url:String){
                if(url=="https://appassets.androidplatform.net/assets/stage.html"&&!stopped){
                    view.evaluateJavascript("GardenPreview.load(${JSONObject.quote(id)},$reduced)",null)
                }
            }
        }
        local.loadUrl("https://appassets.androidplatform.net/assets/stage.html")
    }
    private fun confirmVisible(){
        if(!confirmed&&ready&&resumed&&hasWindowFocus()&&getSystemService(PowerManager::class.java).isInteractive){
            confirmed=true;if(!preview)VisitScheduler.confirmed(this,id,test)
            val plan=VisitRepertoire.next(this,id)
            dialogue.showLine(plan.line.text,!reduced)
            web?.evaluateJavascript("GardenRenderer.resume(); GardenRenderer.playMotion('greet')",null)
            handler.postDelayed({if(!stopped)web?.evaluateJavascript("GardenRenderer.setExpression(${JSONObject.quote(plan.gesture.expression)}); GardenRenderer.playMotion(${JSONObject.quote(plan.gesture.id)})",null)},1600)
            handler.postDelayed({if(!stopped)web?.evaluateJavascript("GardenRenderer.setExpression(${JSONObject.quote(plan.gesture.settle)}); GardenRenderer.blink()",null)},7200)
            handler.postDelayed({if(!stopped)web?.evaluateJavascript("GardenRenderer.playMotion('goodbye')",null)},10500)
            handler.postDelayed({close("visit_finished")},12000)
        }
    }
    private fun immersive(){
        WindowCompat.getInsetsController(window,window.decorView).apply {
            systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    override fun onWindowFocusChanged(hasFocus:Boolean){super.onWindowFocusChanged(hasFocus);if(hasFocus){immersive();confirmVisible()}}
    override fun onResume(){super.onResume();resumed=true;web?.onResume();if(ready)web?.evaluateJavascript("GardenRenderer.resume()",null);confirmVisible()}
    override fun onPause(){
        resumed=false;web?.evaluateJavascript("GardenRenderer.pause()",null);web?.onPause()
        // Android can pause a new Activity while the sleeping display/keyguard transitions.
        // A not-yet-visible visit gets only the existing bounded startup window, not an early dismissal.
        if(confirmed&&!isFinishing)close("visit_interrupted")
        super.onPause()
    }
    private fun close(reason:String){
        if(stopped)return;stopped=true;handler.removeCallbacksAndMessages(null)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);setTurnScreenOn(false)
        if(::dialogue.isInitialized)dialogue.dispose()
        if(preview)finish()else{VisitScheduler.record(this,reason,id);finishAndRemoveTask()}
    }
    override fun onDestroy(){
        stopped=true;handler.removeCallbacksAndMessages(null);if(registered)unregisterReceiver(closeReceiver)
        if(::dialogue.isInitialized)dialogue.dispose()
        web?.let {it.evaluateJavascript("GardenRenderer.dispose()",null);it.removeJavascriptInterface("GardenNative");(it.parent as? ViewGroup)?.removeView(it);it.destroy()};web=null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);super.onDestroy()
    }
}


