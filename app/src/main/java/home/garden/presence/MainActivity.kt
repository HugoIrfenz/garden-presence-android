package home.garden.presence

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import org.json.JSONObject
import java.io.ByteArrayInputStream

/** Character-first home, with native navigation around trusted bundled sprite assets. */
class MainActivity : Activity() {
    data class Character(val id: String, val name: String, val symbol: String, val color: String,
        val expressions: List<String>, val quote: String)
    private val characters = listOf(
        Character("vivianne", "Vivianne Rose Astralis", "Moon · a quiet place to stay", "#CF5265", listOf("Quiet", "Soft warmth", "Shy"), "You can rest here for a moment, darling."),
        Character("angela", "Angela Lily Solstice", "Sun · a warmer tomorrow", "#77B8FF", listOf("Affectionate", "Bright idea", "Worried"), "One thing at a time, baby. We can start there."),
        Character("virelia", "Virelia Camellia", "Ground · a careful heart", "#EE9DAF", listOf("Gentle", "Relieved", "Flustered ><"), "Dear… a little rest counts, too."),
        Character("zaria", "Zaria Iris", "Sky · another way to see it", "#6ED6E8", listOf("Composed", "Smirk", "Sideways"), "Sweetheart, leave a little room for something new."),
        Character("nixia", "Nixia Orchid", "Sea · considerable mischief", "#9C6CFF", listOf("Mischievous", "Wink", "Pout"), "Love! Quick check: shoulders down. You passed.")
    )
    private val prefs by lazy { getSharedPreferences("sprite_preview", MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var web: WebView
    private lateinit var root: LinearLayout
    private lateinit var stage: FrameLayout
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var status: TextView
    private lateinit var board: TextView
    private lateinit var roster: LinearLayout
    private lateinit var expressions: LinearLayout
    private lateinit var pauseButton: Button
    private lateinit var visitButton: Button
    private lateinit var morningSummary: TextView
    private lateinit var visitsSummary: TextView
    private var selected = "vivianne"
    private var mood = "quiet"
    private var paper = false
    private var reduced = false
    private var userPaused = false
    private var foreground = false
    private var pageReady = false
    private var ready = false
    private var destroyed = false
    private var generation = 0
    private var visitToken = 0
    private var visitRunning = false
    private val actionButtons = mutableListOf<Button>()
    private val ink get() = if(paper) GardenUi.ink else GardenUi.white
    private val muted get() = if(paper) GardenUi.muted else Color.parseColor("#AFB9B1")
    private val base get() = if(paper) GardenUi.paper else GardenUi.night
    private val character get() = characters.first { it.id == selected }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected = (savedInstanceState?.getString("character") ?: prefs.getString("character", "vivianne"))
            .takeIf { id -> characters.any { it.id == id } } ?: "vivianne"
        paper = prefs.getBoolean("paper", true)
        reduced = prefs.getBoolean("reduced", false)
        buildUi()
        configureWebView()
        updateTheme()
        updateCharacterUi()
        web.loadUrl("https://appassets.androidplatform.net/assets/stage.html")
    }

    private fun label(text: String, size: Float, secondary: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(if(secondary) muted else ink)
        includeFontPadding=false;setLineSpacing(dp(2).toFloat(),1f)
    }
    private fun button(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text; GardenUi.styleButton(this,dark=!paper);setOnClickListener { action() }
    }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun addChipRow(parent: LinearLayout): LinearLayout {
        val r = row(); parent.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false; addView(r)
        }, LinearLayout.LayoutParams(-1, -2)); return r
    }
    private fun rounded(color: Int, radius: Float = 14f) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius.toInt()).toFloat()
    }
    private fun buildUi() {
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(12), dp(24), dp(28)) }
        scroll.isVerticalScrollBarEnabled=false
        scroll.addView(root, ViewGroup.LayoutParams(-1, -2)); setContentView(scroll)
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            @Suppress("DEPRECATION")
            view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            insets
        }
        val top = row()
        top.addView(label("Garden", 26f).apply{tag="home-brand";typeface=Typeface.create("serif",Typeface.NORMAL)}, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(button("Settings") { showOptions() }.apply{tag="home-options"}); root.addView(top)
        GardenUi.space(this,root,12);GardenUi.rule(this,root);GardenUi.space(this,root,20)
        root.addView(label("FIVE LIVES. ONE HOME.",10f,true).apply{letterSpacing=.16f})
        GardenUi.space(this,root,12)
        title = label("", 28f).apply { typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); isAccessibilityHeading = true;tag="home-character-name" }
        subtitle = label("", 13f, true).apply{setPadding(0,dp(6),0,dp(10))}; root.addView(title); root.addView(subtitle)
        roster = addChipRow(root)
        characters.forEach { p ->
            roster.addView(button(p.name.substringBefore(' ')) { selectCharacter(p.id) }.apply{contentDescription="Meet ${p.name}"})
        }
        GardenUi.space(this,root,10)
        stage = FrameLayout(this)
        web = WebView(this).apply { setBackgroundColor(Color.TRANSPARENT); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        stage.addView(web, FrameLayout.LayoutParams(-1, -1))
        val displayDp = resources.displayMetrics.heightPixels / resources.displayMetrics.density
        root.addView(stage, LinearLayout.LayoutParams(-1, dp((displayDp * .45f).toInt().coerceIn(260, 530))))
        board = label("", 20f).apply {
            typeface = Typeface.create("serif", Typeface.NORMAL); visibility = View.GONE
            setPadding(dp(18), dp(14), dp(18), dp(14)); accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        root.addView(board, LinearLayout.LayoutParams(-1, -2))
        status = label("Preparing her illustration…", 11f, true).apply { setPadding(0,dp(8),0,dp(12));accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        root.addView(status)
        visitButton = button("Spend a moment  →") { openFullVisit() }.also { actionButtons += it;it.tag="home-visit" }
        root.addView(visitButton, LinearLayout.LayoutParams(-1,-2))
        val motionPanel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;visibility=View.GONE;tag="home-expression-panel"}
        root.addView(button("Expressions & movement  +") {motionPanel.visibility=if(motionPanel.visibility==View.GONE)View.VISIBLE else View.GONE;root.findViewWithTag<Button>("home-movement").text="Expressions & movement  ${if(motionPanel.visibility==View.VISIBLE)"−" else "+"}"}.apply{tag="home-movement"},LinearLayout.LayoutParams(-1,-2))
        root.addView(motionPanel)
        expressions = addChipRow(motionPanel)
        val actions = row()
        actions.addView(button("Blink") { cancelVisit(); js("GardenRenderer.blink()") }.also { actionButtons += it }, LinearLayout.LayoutParams(0,-2,1f))
        pauseButton = button("Pause") { cancelVisit(); userPaused = !userPaused; syncPause(); }.also { actionButtons += it }
        actions.addView(pauseButton, LinearLayout.LayoutParams(0,-2,1f))
        actions.addView(button("Smile") { cancelVisit(); setExpression("warm"); js("GardenRenderer.playMotion('success')") }.also { actionButtons += it },LinearLayout.LayoutParams(0,-2,1f))
        motionPanel.addView(actions)
        GardenUi.space(this,root,22);GardenUi.rule(this,root)
        fun destination(name:String,tagValue:String,action:()->Unit):TextView {
            val block=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,dp(12),0,dp(16))}
            block.addView(button("$name  ↗",action).apply{tag=tagValue;gravity=Gravity.START;textSize=20f},LinearLayout.LayoutParams(-1,-2))
            val summary=label("",13f,true).apply{setPadding(dp(16),0,dp(16),0)};block.addView(summary);root.addView(block);GardenUi.rule(this,root)
            return summary
        }
        morningSummary=destination("Mornings","home-alarms"){startActivity(android.content.Intent(this,AngelaAlarmSettingsActivity::class.java))}
        visitsSummary=destination("Little visits","home-visits"){startActivity(android.content.Intent(this,VisitSettingsActivity::class.java))}
        GardenUi.space(this,root,24)
        root.addView(label("We choose us.",16f,true).apply{typeface=Typeface.create("serif",Typeface.ITALIC)})
        setReady(false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        val loader = WebViewAssetLoader.Builder().addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        web.settings.apply {
            javaScriptEnabled = true; domStorageEnabled = false; allowFileAccess = false; allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            mediaPlaybackRequiresUserGesture = true; setSupportMultipleWindows(false)
        }
        web.addJavascriptInterface(RendererEvents(), "GardenNative")
        web.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                return if (trusted(request.url)) loader.shouldInterceptRequest(request.url) ?: denied()
                else denied()
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
            override fun onPageFinished(view: WebView, url: String) {
                if(url != "https://appassets.androidplatform.net/assets/stage.html") return
                pageReady = true; selectCharacter(selected)
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Log.d("GardenPreview", "Web: ${message.message()}"); return true
            }
        }
    }
    private fun trusted(uri: Uri) = uri.scheme == "https" && uri.host == "appassets.androidplatform.net" && uri.path.orEmpty().startsWith("/assets/")
    private fun denied() = WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(byteArrayOf()))

    private inner class RendererEvents {
        @JavascriptInterface fun report(raw: String) {
            if(raw.length > 4096) return
            runOnUiThread {
                if(destroyed) return@runOnUiThread
                val event = runCatching { JSONObject(raw) }.getOrNull() ?: return@runOnUiThread
                val id = event.optString("characterId")
                if(id.isNotEmpty() && id != selected) return@runOnUiThread
                when(event.optString("type")) {
                    "onReady" -> {
                        generation = event.optInt("generation"); setReady(true)
                        status.text = if(event.optBoolean("layered")) "Here with you · available offline" else "Still illustration · animation could not load"
                        syncPause(); if(foreground && !userPaused) js("GardenRenderer.playMotion('greet')")
                        Log.i("GardenPreview", "READY $selected generation=$generation layered=${event.optBoolean("layered")}")
                    }
                    "onError" -> { status.setText(R.string.layer_error); Log.e("GardenPreview", event.optString("message")) }
                    "onMotionFinished" -> Log.d("GardenPreview", "MOTION $selected ${event.optString("name")}")
                }
            }
        }
    }

    fun selectCharacter(id: String) {
        if(characters.none { it.id == id }) return
        cancelVisit(); selected=id; mood="quiet"; userPaused=false; setReady(false)
        prefs.edit().putString("character",id).apply(); updateCharacterUi(); updateTheme()
        status.setText(R.string.layers_loading)
        if(pageReady) js("GardenPreview.load(${JSONObject.quote(id)},$reduced)")
    }
    private fun updateCharacterUi() {
        title.text=character.name; subtitle.text=character.symbol
        stage.contentDescription="${character.name}, animated illustration"
        expressions.removeAllViews()
        character.expressions.forEachIndexed { i, caption ->
            val key=listOf("quiet","warm","shy")[i]
            expressions.addView(button(caption) { cancelVisit();setExpression(key) }.apply { tag=key; isEnabled=ready })
        }
        updateSelection()
    }
    private fun setExpression(value: String) {
        if(!ready) return
        mood=value; js("GardenRenderer.setExpression(${JSONObject.quote(value)})"); updateSelection()
    }
    private fun setReady(value: Boolean) {
        ready=value; actionButtons.forEach { it.isEnabled=value }
        for(i in 0 until expressions.childCount) expressions.getChildAt(i).isEnabled=value
    }
    private fun updateSelection() {
        for(i in 0 until roster.childCount) (roster.getChildAt(i) as Button).apply { isSelected=characters[i].id==selected; paintButton(this,isSelected) }
        for(i in 0 until expressions.childCount) (expressions.getChildAt(i) as Button).apply { isSelected=tag==mood;paintButton(this,isSelected) }
    }
    private fun paintButton(button: Button, selected: Boolean = false) {
        val primary=button==visitButton || selected
        val quiet=button.parent==roster || button.tag in listOf("home-options","home-movement","home-alarms","home-visits")
        GardenUi.styleButton(button,primary,dark=!paper,quiet=quiet&&!selected)
        if(button.tag=="home-alarms"||button.tag=="home-visits")button.textSize=20f
        if(button.parent==roster){button.textSize=12f;button.setPadding(dp(10),dp(10),dp(10),dp(10))}
        (button.layoutParams as? LinearLayout.LayoutParams)?.let { it.setMargins(0,dp(2),dp(4),dp(2));button.layoutParams=it }
    }
    private fun updateTheme() {
        root.setBackgroundColor(base); (root.parent as View).setBackgroundColor(base)
        fun tint(view: View) {
            if(view is Button) paintButton(view)
            else if(view is TextView) view.setTextColor(if(view==title || view==board||view.tag=="home-brand") ink else muted)
            if(view.tag=="garden-rule")view.setBackgroundColor(if(paper)GardenUi.line else Color.parseColor("#35413C"))
            if(view is ViewGroup)for(i in 0 until view.childCount)tint(view.getChildAt(i))
        }
        tint(root)
        val accent=Color.parseColor(character.color)
        stage.background=GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(33,39,39),Color.rgb(20+Color.red(accent)/12,25+Color.green(accent)/12,24+Color.blue(accent)/12))).apply { cornerRadius=dp(10).toFloat() }
        stage.clipToOutline=true
        board.background=rounded(Color.parseColor(if(paper) "#F9F0E9" else "#2A222F"))
        updateSelection()
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility=if(paper) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
    }
    private fun syncPause() {
        if(pageReady) js(if(userPaused || !foreground) "GardenRenderer.pause()" else "GardenRenderer.resume()")
        pauseButton.text=if(userPaused) "Resume" else "Pause"
    }
    private fun cancelVisit() {
        ++visitToken;handler.removeCallbacksAndMessages(null);visitRunning=false
        if(::board.isInitialized) board.visibility=View.GONE
        if(::visitButton.isInitialized) visitButton.text="Spend a moment  →"
    }
    fun startVisit() {
        if(!ready) return
        cancelVisit();visitRunning=true;userPaused=false;syncPause();setExpression("quiet")
        val plan=VisitRepertoire.next(this,selected)
        val token=visitToken;visitButton.setText(R.string.visit_restart)
        js("GardenRenderer.playMotion('greet')")
        fun at(ms: Long, action: () -> Unit) { handler.postDelayed({if(token==visitToken && foreground && !destroyed) action()},ms) }
        at(1400) { board.text=getString(R.string.quote_board,plan.line.text);board.visibility=View.VISIBLE }
        at(1600) { setExpression(plan.gesture.expression);js("GardenRenderer.playMotion(${JSONObject.quote(plan.gesture.id)})") }
        at(7200) { setExpression(plan.gesture.settle);js("GardenRenderer.blink()") }
        at(11000) { board.visibility=View.GONE;js("GardenRenderer.playMotion('goodbye')") }
        at(12000) { visitRunning=false;userPaused=true;syncPause();visitButton.setText(R.string.visit_another);status.setText(R.string.visit_complete) }
    }
    fun openFullVisit(){
        if(!ready)return
        cancelVisit()
        startActivity(android.content.Intent(this,VisitActivity::class.java).putExtra("preview",true).putExtra("character",selected))
    }
    private fun showOptions() {
        val values=booleanArrayOf(paper,reduced)
        AlertDialog.Builder(this).setTitle("Appearance")
            .setMultiChoiceItems(arrayOf("Light background","Reduced motion"),values) { _,i,checked ->
                if(i==0){paper=checked;prefs.edit().putBoolean("paper",checked).apply();updateTheme()}
                else{reduced=checked;cancelVisit();prefs.edit().putBoolean("reduced",checked).apply();js("GardenRenderer.setReducedMotion($checked)")}
            }.setPositiveButton("Done",null)
            .setNeutralButton("About") { _,_ -> AlertDialog.Builder(this).setTitle("Garden Presence · ${BuildConfig.VERSION_NAME}")
                .setMessage("Five girls, 62 image layers, 60 authored lines and 12 small layer gestures. Each girl cycles through her lines and gestures without immediate repeats, even after reopening the app. Foreground previews share the same repertoire as screen-off visits.\n\nAll dialogue is new fictional UI copy, not recorded memories or live AI responses. No account, internet, microphone or camera. Decorative visits use approximate timing. Mornings in the Garden adds dedicated alarm poses for all five girls and a separate one-shot alarm with five-minute snooze, using Android alarm permissions and your default alarm tone.\n\nLayered 2D art; not Cubism or 3D models.").setPositiveButton("Close",null).show() }.show()
    }
    private fun js(code: String) { if(!destroyed) web.evaluateJavascript(code,null) }
    fun inspect(callback: (String) -> Unit) { web.evaluateJavascript("JSON.stringify(GardenPreview.inspect())",callback) }
    override fun onResume() { super.onResume();VisitScheduler.refreshSchedule(this);foreground=true;if(::web.isInitialized){web.onResume();if(pageReady)syncPause()}
        if(::morningSummary.isInitialized){val due=AngelaAlarmScheduler.prefs(this).getLong("due",0);morningSummary.text=if(due>0)"${AngelaAlarmScheduler.queued(this).shortName} · ${java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(java.util.Date(due))}" else "Choose who greets your next morning";visitsSummary.text=if(VisitScheduler.enabled(this))"Automatic visits are on · manage your rhythm" else "Quiet company, on your terms"}
        Log.i("GardenPreview","LIFECYCLE resume") }
    override fun onPause() { foreground=false;cancelVisit();if(pageReady)js("GardenRenderer.pause(); GardenRenderer.motion=null");if(::web.isInitialized)web.onPause();Log.i("GardenPreview","LIFECYCLE pause");super.onPause() }
    override fun onSaveInstanceState(outState: Bundle) {outState.putString("character",selected);super.onSaveInstanceState(outState)}
    override fun onDestroy() {cancelVisit();if(::web.isInitialized){web.evaluateJavascript("GardenRenderer.dispose()",null);web.removeJavascriptInterface("GardenNative");(web.parent as? ViewGroup)?.removeView(web);web.destroy()};destroyed=true;super.onDestroy()}
}
