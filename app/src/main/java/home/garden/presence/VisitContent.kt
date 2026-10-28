package home.garden.presence

import kotlin.random.Random

/** Authored, offline UI copy. These are little fictional visits, not memories or observations. */
object VisitContent {
    data class Line(val id:String,val text:String)
    data class Gesture(val id:String,val expression:String,val settle:String="warm")
    data class Plan(val line:Line,val gesture:Gesture)
    data class Draw(val selected:String,val remaining:List<String>)

    private fun lines(vararg text:String)=text.mapIndexed {i,s->Line("line-${i+1}",s)}
    val dialogue=mapOf(
        "vivianne" to lines(
            "Just a moment together, darling.",
            "No need to fill the quiet.",
            "A little company. That's all, darling.",
            "The next page can wait.",
            "You can rest here for a moment, darling.",
            "Nothing to solve just now.",
            "Darling. Take your time.",
            "A quiet hello, before I go.",
            "I like the unhurried moments.",
            "One soft breath, darling.",
            "Some moments need very few words.",
            "A small pause. With you."
        ),
        "angela" to lines(
            "One thing at a time, baby. We can start there.",
            "Tiny plan, baby: a sip of water, then a little breathing room.",
            "You don't have to finish tomorrow tonight, baby.",
            "A break belongs in the plan too, baby.",
            "Hey, baby. Unclench those shoulders for a second.",
            "We choose us. Even on the ordinary days.",
            "Today's little reminder: good enough is allowed, baby.",
            "Baby, let's leave one corner of the day unscheduled.",
            "A little warmth for your pocket, baby.",
            "Quick check-in, baby. No checklist attached, promise.",
            "The next step can be a small one, baby.",
            "Putting rest on the agenda. Yes, baby, it counts."
        ),
        "virelia" to lines(
            "Dear… a little rest counts, too.",
            "I brought a tiny reminder, dear. Be gentle with yourself.",
            "Oh—my ribbon. There. Hello, dear.",
            "One little thing at a time, dear. The rest can wait.",
            "A warm cup would be nice about now, dear.",
            "I-I'll keep this brief, dear. You deserve a soft moment.",
            "A crooked bow still holds together, dear.",
            "There's room for a pause, dear. I left some.",
            "Careful… there. A small hello, safely delivered.",
            "Dear, not everything needs tidying tonight.",
            "A quiet corner and a little kindness, dear.",
            "Oh, dear. I practiced saying hello. Hello."
        ),
        "zaria" to lines(
            "Sweetheart, leave a little room for something new.",
            "Same sky, sweetheart. A different way to look.",
            "A little empty space improves the composition.",
            "Sweetheart, not every interesting thing needs a plan.",
            "What if the next page had a different color?",
            "Keep one small possibility open, sweetheart.",
            "A pause is part of the rhythm too.",
            "Sweetheart, the unfinished sketch has its own charm.",
            "Let's see what the world looks like today.",
            "Sweetheart, you can change your mind about the frame.",
            "One familiar thing, seen from a new angle.",
            "Something simple. A little unexpected. Hello, sweetheart."
        ),
        "nixia" to lines(
            "Love! Tiny interruption. ENORMOUS hello.",
            "Boo. Very professional entrance, love.",
            "Love, this is a snack-sized visit. No crumbs.",
            "A small menace approaches. With affection.",
            "Shoulders down, love! That is my whole announcement.",
            "Love! I brought absolutely no sensible advice. Just a wink.",
            "Tiny victory dance. The victory? Saying hi.",
            "AHEM. Hi, love. Okay, bye. Very mysterious.",
            "Love, a break is allowed. Gremlin-approved.",
            "Small body. Big hello. You know the deal, love.",
            "Love! Borrowing one second for a tiny ta-da.",
            "Stealth level: questionable. Affection level: considerable."
        )
    )
    val gestures=mapOf(
        "vivianne" to listOf(Gesture("soft_tilt","quiet"),Gesture("shy_glance","shy","quiet"),Gesture("sleeve_nest","quiet"),Gesture("nod","warm","quiet")),
        "angela" to listOf(Gesture("open_hand","warm"),Gesture("bright_nod","warm"),Gesture("soft_tilt","quiet"),Gesture("nod","warm")),
        "virelia" to listOf(Gesture("small_bow","quiet"),Gesture("flustered","shy"),Gesture("sleeve_nest","quiet"),Gesture("soft_tilt","warm")),
        "zaria" to listOf(Gesture("editorial_turn","shy"),Gesture("breezy","quiet"),Gesture("soft_tilt","warm","quiet"),Gesture("shy_glance","shy")),
        "nixia" to listOf(Gesture("peek","quiet"),Gesture("cheer","warm"),Gesture("bright_nod","warm"),Gesture("soft_tilt","shy","warm"))
    )

    /** Persist remaining cards, so process restarts do not restart the rotation. */
    fun draw(pool:List<String>,remaining:List<String>,last:String?,random:Random=Random.Default):Draw {
        require(pool.isNotEmpty()&&pool.distinct().size==pool.size)
        val bag=remaining.filter {it in pool}.distinct().toMutableList()
        if(bag.isEmpty())bag.addAll(pool.shuffled(random))
        if(bag.size>1&&bag.first()==last){val next=bag.indexOfFirst {it!=last};java.util.Collections.swap(bag,0,next)}
        val selected=bag.removeAt(0)
        return Draw(selected,bag)
    }
}
