package home.garden.presence

import kotlin.math.*

/** Source-space, weighted deformation rig. Open/blink artwork shares this exact mesh.
 * Regions are painted mathematically over the supplied pose, not separate cut-out layers.
 * Keeping a connected mesh avoids revealing missing pixels behind moving hair. */
class AlarmMotionRig(characterId: String) {
    companion object { const val COLUMNS = 40; const val ROWS = 60; const val VERTICES = (COLUMNS + 1) * (ROWS + 1) }
    private data class Region(val x: Float, val y: Float, val rx: Float, val ry: Float, val amplitude: Float, val phase: Float)
    private data class Pose(
        val speed: Float, val tilt: Float, val bob: Float,
        val faceX: Float, val faceY: Float, val faceRx: Float, val faceRy: Float,
        val neckY: Float, val hair: List<Region>, val sleeves: List<Region>
    )
    private fun r(x: Float,y: Float,rx: Float,ry: Float,a: Float,p: Float)=Region(x,y,rx,ry,a,p)
    private val pose = when(characterId) {
        "vivianne" -> Pose(.64f,.015f,.003f,.47f,.25f,.17f,.10f,.40f,
            listOf(r(.13f,.68f,.17f,.48f,.022f,0f),r(.91f,.67f,.15f,.45f,.025f,1.3f),r(.62f,.57f,.085f,.28f,.018f,.7f),r(.29f,.14f,.17f,.15f,.008f,1.8f)),
            listOf(r(.35f,.55f,.15f,.26f,.004f,.7f),r(.74f,.78f,.2f,.23f,.005f,1.5f)))
        "nixia" -> Pose(1.22f,.023f,.007f,.50f,.38f,.20f,.13f,.54f,
            listOf(r(.13f,.23f,.16f,.17f,.018f,0f),r(.69f,.13f,.18f,.13f,.018f,1.2f),r(.13f,.43f,.16f,.20f,.024f,.8f),r(.84f,.37f,.19f,.22f,.026f,2f),r(.30f,.20f,.18f,.18f,.009f,1.2f)),
            listOf(r(.17f,.63f,.2f,.15f,.007f,0f),r(.86f,.64f,.18f,.15f,.009f,1.2f)))
        "virelia" -> Pose(.83f,.017f,.004f,.49f,.28f,.19f,.12f,.45f,
            listOf(r(.33f,.49f,.14f,.25f,.020f,0f),r(.75f,.48f,.13f,.26f,.022f,1.4f),r(.11f,.64f,.09f,.16f,.015f,.4f),r(.91f,.66f,.09f,.18f,.016f,1.8f),r(.30f,.16f,.18f,.15f,.007f,.8f)),
            listOf(r(.16f,.52f,.16f,.16f,.005f,0f),r(.9f,.55f,.12f,.16f,.006f,1f)))
        "zaria" -> Pose(.77f,.021f,.0035f,.51f,.29f,.19f,.12f,.43f,
            listOf(r(.29f,.29f,.16f,.23f,.018f,0f),r(.75f,.23f,.13f,.21f,.015f,1.4f),r(.39f,.13f,.22f,.16f,.01f,.4f),r(.40f,.40f,.07f,.10f,.012f,1.8f),r(.7f,.34f,.06f,.09f,.01f,.8f)),
            listOf(r(.13f,.68f,.16f,.24f,.009f,0f),r(.77f,.78f,.26f,.21f,.010f,1.1f)))
        else -> Pose(.97f,.019f,.0045f,.45f,.30f,.20f,.13f,.49f,
            listOf(r(.16f,.32f,.15f,.22f,.018f,0f),r(.69f,.24f,.14f,.18f,.017f,1.4f),r(.34f,.13f,.22f,.19f,.009f,.7f),r(.24f,.45f,.12f,.13f,.014f,1f)),
            listOf(r(.17f,.72f,.15f,.22f,.009f,0f),r(.88f,.67f,.14f,.24f,.01f,1.6f),r(.60f,.75f,.085f,.21f,.007f,.6f)))
    }
    private val x = FloatArray(VERTICES)
    private val y = FloatArray(VERTICES)
    private val head = FloatArray(VERTICES)
    private val body = FloatArray(VERTICES)
    private val hair = Array(pose.hair.size) { FloatArray(VERTICES) }
    private val sleeves = Array(pose.sleeves.size) { FloatArray(VERTICES) }
    private val hairX = FloatArray(pose.hair.size)
    private val hairY = FloatArray(pose.hair.size)
    private val cloth = FloatArray(pose.sleeves.size)

    init {
        for(row in 0..ROWS) for(col in 0..COLUMNS) {
            val i=row*(COLUMNS+1)+col; val nx=col/COLUMNS.toFloat(); val ny=row/ROWS.toFloat()
            x[i]=nx; y[i]=ny
            head[i]=1f-smooth(pose.neckY-.08f,pose.neckY+.18f,ny)
            // Keep table, cushion and tray supported rather than rubbery.
            body[i]=sin(PI*ny).toFloat()*(1f-smooth(.68f,.91f,ny))
            val face=ellipse(nx,ny,pose.faceX,pose.faceY,pose.faceRx,pose.faceRy)
            for(k in pose.hair.indices) {
                val region=pose.hair[k]
                hair[k][i]=ellipse(nx,ny,region.x,region.y,region.rx,region.ry)*(1f-face)
            }
            for(k in pose.sleeves.indices) {
                val region=pose.sleeves[k]
                sleeves[k][i]=ellipse(nx,ny,region.x,region.y,region.rx,region.ry)*(1f-face)
            }
        }
    }
    private fun smooth(a:Float,b:Float,v:Float):Float { val t=((v-a)/(b-a)).coerceIn(0f,1f); return t*t*(3f-2f*t) }
    private fun ellipse(x:Float,y:Float,cx:Float,cy:Float,rx:Float,ry:Float):Float {
        val d=sqrt(((x-cx)/rx).pow(2)+((y-cy)/ry).pow(2))
        return 1f-smooth(.35f,1f,d)
    }

    /** Writes into a reused buffer. Units are illustration dimensions, independent of screen density.
     * patSeconds < 0 means idle. Reduced motion is an exact undeformed pose. */
    fun frame(seconds:Float,patSeconds:Float,reduced:Boolean,out:FloatArray) {
        require(out.size>=VERTICES*2)
        if(reduced) { for(i in x.indices){out[i*2]=x[i];out[i*2+1]=y[i]};return }
        val t=seconds*pose.speed
        val pat=if(patSeconds in 0f..3.2f)sin(PI*patSeconds/3.2).toFloat() else 0f
        val settle=if(patSeconds in 0f..3.2f)sin(patSeconds*7f)*exp(-patSeconds*1.25f)*pat else 0f
        val angle=pose.tilt*(sin(t*.72f)*.7f+sin(t*.31f)*.3f)+pat*.024f
        val ca=cos(angle);val sa=sin(angle)
        val driftX=sin(t*.57f)*.004f
        val driftY=sin(t*1.08f)*pose.bob-pat*.006f
        val breath=sin(t*.94f)*.0025f
        for(k in hairX.indices) {
            val r=pose.hair[k]
            // Different phases and frequencies provide delayed hair follow-through.
            val wave=sin(t*1.22f-r.phase)*.7f+sin(t*.53f+r.phase)*.3f
            hairX[k]=r.amplitude*(wave+settle*.8f)
            hairY[k]=r.amplitude*.22f*sin(t*1.48f-r.phase)+settle*.002f
        }
        for(k in cloth.indices)cloth[k]=pose.sleeves[k].amplitude*(sin(t*.91f-pose.sleeves[k].phase)+pat*.7f)
        for(i in x.indices) {
            val nx=x[i];val ny=y[i]
            // Rigid rotation through the face; smoothly blend into the neck/shoulders.
            val dx=nx-pose.faceX;val dy=(ny-pose.neckY)*1.5f
            var px=nx+((ca-1f)*dx-sa*dy+driftX)*head[i]
            var py=ny+(sa*dx+(ca-1f)*dy)/1.5f*head[i]+driftY*head[i]
            px+=(nx-.5f)*breath*body[i];py-=breath*body[i]
            for(k in hairX.indices){px+=hairX[k]*hair[k][i];py+=hairY[k]*hair[k][i]}
            for(k in cloth.indices){px+=cloth[k]*sleeves[k][i];py-=cloth[k]*.3f*sleeves[k][i]}
            out[i*2]=px;out[i*2+1]=py
        }
    }
}
