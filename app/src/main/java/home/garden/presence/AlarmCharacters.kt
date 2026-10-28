package home.garden.presence

/** Authored alarm-scene copy and art direction, not new memories or live responses. */
data class AlarmCharacter(
    val id:String,val name:String,val symbol:String,val accent:String,val background:String,
    val greeting:String,val pats:List<String>,val snoozeLine:String,
    val idleSpeed:Float,val sway:Float,val patStrength:Float,
    val artScale:Float=1.12f,val topBias:Float=.25f
){
    val shortName get()=name.substringBefore(' ')
    val assetPath get()=if(id=="angela")"alarm-angela" else "alarm-characters/$id"
}

object AlarmCharacters {
    val all=listOf(
        AlarmCharacter("angela","Angela Lily Solstice","LILY / SUN","#A7D0FF","#0D1320",
            "Morning, baby. Come a little closer… let's start gently.",
            listOf("Hehe… baby. You're making it very hard to be the responsible one.","Mmm. One more, then we start our day?","Okay, that was lovely. I'm still helping you get up, though."),
            "Five minutes, baby. I'll be back at %s. ♡",1.15f,1.4f,5f),
        AlarmCharacter("vivianne","Vivianne Rose Astralis","ROSE / MOON","#E7A3AE","#19131C",
            "Darling… morning. I'm here.",
            listOf("…Stay a moment.","Mm. That was nice, darling.","One more. Then we get up."),
            "Five minutes, darling. I'll return at %s.",.65f,.6f,2f),
        AlarmCharacter("nixia","Nixia Orchid","ORCHID / SEA","#C6A4FF","#171125",
            "LOVE. Your very small alarm has arrived! Up, up!",
            listOf("HEY—…okay. You may continue.","Head pats accepted. Snooze negotiations are still open.","Love! I came to wake YOU up. Stop making me comfy!"),
            "FIVE minutes, love. Tiny alarm returns at %s!",1.8f,2.5f,8f),
        AlarmCharacter("virelia","Virelia Camellia","CAMELLIA / GROUND","#F1BACB","#21171E",
            "G-good morning, dear.\nBreakfast… safely delivered.",
            listOf("D-dear…! Wait, the tray—okay. I've got it.","That helps… thank you, dear.","One little pat. Then tea, before it gets cold?"),
            "Five minutes, dear. I'll come back at %s. Carefully.",.95f,1f,3f,.98f,.16f),
        AlarmCharacter("zaria","Zaria Iris","IRIS / SKY","#9CE1EA","#101D24",
            "Sweetheart. Shall we see what the day has in mind?",
            listOf("Mm. An interesting way to begin the day.","Careful, sweetheart. You'll rearrange the whole composition.","All right. We can let the morning wait for one small moment."),
            "A little more dreaming, sweetheart. See you at %s.",.8f,1.7f,4f)
    )
    fun get(id:String?)=all.firstOrNull{it.id==id} ?: all.first()
}
