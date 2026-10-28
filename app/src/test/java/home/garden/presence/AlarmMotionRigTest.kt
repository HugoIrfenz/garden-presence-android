package home.garden.presence

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class AlarmMotionRigTest {
    private val ids=listOf("angela","vivianne","nixia","virelia","zaria")
    private fun buffer()=FloatArray(AlarmMotionRig.VERTICES*2)
    @Test fun everyPoseStaysContinuousAndNeverFoldsAtIdleOrDuringPat(){
        val points=buffer()
        for(id in ids){
            val rig=AlarmMotionRig(id)
            for(tick in 0..240){
                rig.frame(tick*.17f,if(tick%2==0)(tick%33)*.1f else -1f,false,points)
                assertTrue(points.all{it.isFinite()&&it in -.12f..1.12f})
                fun area(a:Int,b:Int,c:Int):Float = (points[b*2]-points[a*2])*(points[c*2+1]-points[a*2+1])-(points[b*2+1]-points[a*2+1])*(points[c*2]-points[a*2])
                for(row in 0 until AlarmMotionRig.ROWS)for(col in 0 until AlarmMotionRig.COLUMNS){
                    val a=row*(AlarmMotionRig.COLUMNS+1)+col;val b=a+1;val c=a+AlarmMotionRig.COLUMNS+1;val d=c+1
                    assertTrue("$id mesh folded at $tick/$row/$col",area(a,b,c)>.00005f&&area(b,d,c)>.00005f)
                }
            }
        }
    }
    @Test fun reducedMotionIsExactlyStillEvenDuringReactions(){
        val a=buffer();val b=buffer()
        for(id in ids){val rig=AlarmMotionRig(id);rig.frame(0f,-1f,true,a);rig.frame(33f,1.3f,true,b);assertArrayEquals(a,b,0f)
            for(row in 0..AlarmMotionRig.ROWS)for(col in 0..AlarmMotionRig.COLUMNS){val i=(row*(AlarmMotionRig.COLUMNS+1)+col)*2;assertEquals(col/40f,a[i],0f);assertEquals(row/60f,a[i+1],0f)}
        }
    }
    @Test fun hairMovesIndependentlyAndBreakfastTrayRemainsAnchored(){
        val a=buffer();val b=buffer();val rig=AlarmMotionRig("vivianne")
        rig.frame(0f,-1f,false,a);rig.frame(3f,-1f,false,b)
        val hair=(40*(41)+5)*2;val torso=(40*41+20)*2
        assertTrue("Hair should have visible independent movement",abs(b[hair]-a[hair])>.006f)
        assertTrue(abs((b[hair]-a[hair])-(b[torso]-a[torso]))>.005f)
        val tray=(49*41+20)*2;val virelia=AlarmMotionRig("virelia")
        virelia.frame(0f,-1f,false,a);virelia.frame(3f,1f,false,b)
        assertTrue("Keep breakfast tray stable",hypot(b[tray]-a[tray],b[tray+1]-a[tray+1])<.002f)
    }
    @Test fun headPatReturnsSmoothlyToIdle(){
        val a=buffer();val b=buffer()
        for(id in ids){val rig=AlarmMotionRig(id);rig.frame(5f,-1f,false,a);rig.frame(5f,3.2f,false,b);assertArrayEquals(a,b,.000001f)}
    }
}
