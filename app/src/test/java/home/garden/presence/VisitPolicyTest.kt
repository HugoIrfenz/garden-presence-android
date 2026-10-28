package home.garden.presence

import org.junit.Assert.*
import org.junit.Test

class VisitPolicyTest {
    private fun reason(enabled:Boolean=true,test:Boolean=false,interactive:Boolean=false,off:Boolean=true,aod:Boolean=false,includeAod:Boolean=false,permission:Boolean=true,quiet:Boolean=false,powerSave:Boolean=false,inCall:Boolean=false,count:Int=0,cap:Int=3,elapsed:Long=3_600_000)=VisitPolicy.skipReason(enabled,test,interactive,off,aod,includeAod,permission,quiet,powerSave,inCall,count,cap,elapsed)
    @Test fun overnightQuietHoursRespectBothBoundaries(){
        assertFalse(VisitPolicy.quiet(1349,1350,360));assertTrue(VisitPolicy.quiet(1350,1350,360))
        assertTrue(VisitPolicy.quiet(0,1350,360));assertTrue(VisitPolicy.quiet(359,1350,360));assertFalse(VisitPolicy.quiet(360,1350,360))
        assertTrue(VisitPolicy.quiet(700,600,800));assertFalse(VisitPolicy.quiet(800,600,800));assertTrue(VisitPolicy.quiet(700,600,600))
    }
    @Test fun noVisitWithoutOptInOrPermission(){assertEquals("disabled",reason(enabled=false));assertEquals("permission_missing",reason(permission=false));assertEquals("permission_missing",reason(test=true,permission=false))}
    @Test fun neverInterruptInteractivePhoneEvenInTest(){assertEquals("skipped_interactive",reason(interactive=true));assertEquals("skipped_interactive",reason(test=true,interactive=true))}
    @Test fun strictOffDefaultAndOptionalAod(){assertEquals("skipped_display_or_aod",reason(off=false,aod=true));assertNull(reason(off=false,aod=true,includeAod=true));assertEquals("skipped_display_or_aod",reason(off=false,includeAod=true))}
    @Test fun callsAndBatterySaverAlsoBlockTests(){assertEquals("skipped_call",reason(test=true,inCall=true));assertEquals("skipped_battery_saver",reason(test=true,powerSave=true))}
    @Test fun limitsAreInclusiveAndQuietHoursWin(){assertEquals("quiet_hours",reason(quiet=true));assertEquals("daily_limit",reason(count=3));assertEquals("cooldown",reason(elapsed=1_799_999));assertNull(reason(count=2,elapsed=1_800_000))}
    @Test fun explicitTestOverridesScheduleLimitsOnly(){assertNull(reason(enabled=false,test=true,quiet=true,count=9,elapsed=0));assertNull(reason())}
}
