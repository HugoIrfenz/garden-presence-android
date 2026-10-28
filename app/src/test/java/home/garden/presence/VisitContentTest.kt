package home.garden.presence

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class VisitContentTest {
    @Test fun allFiveHaveTwelveShortUniqueLinesAndSupportedExpressions(){
        assertEquals(setOf("angela","vivianne","virelia","zaria","nixia"),VisitContent.dialogue.keys)
        for((id,lines) in VisitContent.dialogue){
            assertEquals(12,lines.size);assertEquals(12,lines.map{it.id}.distinct().size);assertEquals(12,lines.map{it.text}.distinct().size)
            assertTrue(lines.all{it.text.isNotBlank()&&it.text.split(Regex("\\s+")).size<=24})
            assertTrue(VisitContent.gestures.getValue(id).all{it.expression in listOf("quiet","warm","shy")&&it.settle in listOf("quiet","warm","shy")})
        }
    }
    @Test fun fullDeckBeforeRepeatsAndNoRepeatAtCycleBoundary(){
        for(lines in VisitContent.dialogue.values){
            val pool=lines.map{it.id};var remaining=emptyList<String>();var last:String?=null
            val random=Random(81)
            repeat(40){
                val seen=mutableSetOf<String>()
                repeat(pool.size){
                    val result=VisitContent.draw(pool,remaining,last,random)
                    assertNotEquals(last,result.selected);assertTrue(seen.add(result.selected))
                    // Serialize/restore exactly as the native preference adapter does.
                    remaining=result.remaining.joinToString(",").split(',');last=result.selected
                }
                assertEquals(pool.toSet(),seen)
            }
        }
    }
    @Test fun gesturesAlsoAvoidRepeatsAcrossCycles(){
        for(gestures in VisitContent.gestures.values){
            val pool=gestures.map{it.id};var remaining=emptyList<String>();var last:String?=null
            repeat(100){val result=VisitContent.draw(pool,remaining,last,Random(it));assertNotEquals(last,result.selected);remaining=result.remaining;last=result.selected}
        }
    }
    @Test fun obsoleteAndDuplicateSavedCardsAreRemoved(){
        val result=VisitContent.draw(listOf("a","b","c"),listOf("obsolete","a","a","b"),"a",Random(0))
        assertEquals("b",result.selected);assertEquals(listOf("a"),result.remaining)
        assertTrue(VisitContent.draw(listOf("a","b"),listOf("obsolete"),"a",Random(0)).selected=="b")
    }
}
