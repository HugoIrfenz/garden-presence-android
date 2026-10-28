package home.garden.presence

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Small native design system: paper, ink, hairlines and one clear primary action. */
object GardenUi {
    val paper=Color.parseColor("#F3F0E9")
    val ink=Color.parseColor("#242A28")
    val muted=Color.parseColor("#606A63")
    val line=Color.parseColor("#D6D9D0")
    val night=Color.parseColor("#151D1C")
    val white=Color.parseColor("#F5F3ED")
    fun dp(c:Context,n:Int)=(n*c.resources.displayMetrics.density).toInt()
    fun shape(c:Context,color:Int,radius:Int=10,stroke:Int?=null)=GradientDrawable().apply {
        setColor(color);cornerRadius=dp(c,radius).toFloat();stroke?.let{setStroke(dp(c,1),it)}
    }
    fun label(c:Context,value:String,size:Float=15f,color:Int=ink)=TextView(c).apply {
        text=value;textSize=size;setTextColor(color);includeFontPadding=false
        typeface=Typeface.create("sans-serif",Typeface.NORMAL);setLineSpacing(dp(c,2).toFloat(),1f)
    }
    fun eyebrow(c:Context,value:String,color:Int=muted)=label(c,value,10f,color).apply{letterSpacing=.16f}
    fun styleButton(b:Button,primary:Boolean=false,dark:Boolean=false,quiet:Boolean=false){
        val foreground=if(primary){if(dark)ink else white}else if(dark)white else ink
        val bg=if(quiet)Color.TRANSPARENT else if(primary){if(dark)white else ink}else if(dark)Color.parseColor("#24302E") else Color.parseColor("#E7EAE2")
        b.apply {
            isAllCaps=false;textSize=14f;typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
            minHeight=dp(context,50);minimumHeight=dp(context,50);minWidth=0;minimumWidth=0
            setPadding(dp(context,16),dp(context,10),dp(context,16),dp(context,10))
            backgroundTintList=null
            setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled),intArrayOf()),intArrayOf(if(dark)Color.GRAY else muted,foreground)))
            val ripple=if(dark)0x33FFFFFF else 0x22000000
            background=RippleDrawable(ColorStateList.valueOf(ripple),shape(context,bg,10),shape(context,Color.WHITE,10))
            stateListAnimator=null
        }
    }
    fun button(c:Context,value:String,primary:Boolean=false,dark:Boolean=false,quiet:Boolean=false,action:()->Unit)=Button(c).apply {
        text=value;styleButton(this,primary,dark,quiet);setOnClickListener{action()}
    }
    fun space(c:Context,parent:LinearLayout,size:Int){parent.addView(View(c),LinearLayout.LayoutParams(1,dp(c,size)))}
    fun rule(c:Context,parent:LinearLayout,color:Int=line){parent.addView(View(c).apply{tag="garden-rule";setBackgroundColor(color)},LinearLayout.LayoutParams(-1,dp(c,1)))}
    fun screen(a:Activity):LinearLayout {
        val scroll=ScrollView(a).apply{isFillViewport=true;setBackgroundColor(paper);isVerticalScrollBarEnabled=false}
        val root=LinearLayout(a).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(a,24),dp(a,12),dp(a,24),dp(a,32))}
        scroll.addView(root);a.setContentView(scroll)
        ViewCompat.setOnApplyWindowInsetsListener(scroll){v,i->val s=i.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout());v.setPadding(s.left,s.top,s.right,s.bottom);i}
        WindowInsetsControllerCompat(a.window,a.window.decorView).apply{isAppearanceLightStatusBars=true;isAppearanceLightNavigationBars=true}
        return root
    }
    fun header(a:Activity,root:LinearLayout,section:String){
        val row=LinearLayout(a).apply{gravity=Gravity.CENTER_VERTICAL}
        row.addView(button(a,"‹  Garden",quiet=true){a.finish()},LinearLayout.LayoutParams(-2,-2))
        row.addView(eyebrow(a,section).apply{gravity=Gravity.END},LinearLayout.LayoutParams(0,-2,1f));root.addView(row)
        space(a,root,12);rule(a,root);space(a,root,24)
    }
    fun section(c:Context,root:LinearLayout,title:String){space(c,root,28);root.addView(eyebrow(c,title));space(c,root,12)}
}

/** Small face crop only; decode at thumbnail size rather than retaining five full masters. */
class GardenPortrait(context:Context,private val girl:AlarmCharacter):View(context){
    private val bitmap=context.assets.open("${girl.assetPath}/open.webp").use{BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inSampleSize=4})!!}
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val source=Rect((bitmap.width*.12f).toInt(),(bitmap.height*.045f).toInt(),(bitmap.width*.88f).toInt(),(bitmap.height*.045f+bitmap.width*.76f).toInt())
    private val dest=RectF()
    private val clip=Path()
    init{importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO}
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int){dest.set(0f,0f,w.toFloat(),h.toFloat());clip.reset();clip.addRoundRect(dest,GardenUi.dp(context,8).toFloat(),GardenUi.dp(context,8).toFloat(),Path.Direction.CW)}
    override fun onDraw(c:Canvas){c.save();c.clipPath(clip);c.drawColor(Color.parseColor(girl.background));c.drawBitmap(bitmap,source,dest,paint);c.restore()}
}
