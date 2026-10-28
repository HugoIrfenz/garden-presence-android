package home.garden.presence

import android.content.Context

/** Draw only once a visit is visible, or when the user starts a foreground preview. */
object VisitRepertoire {
    fun next(context:Context,character:String):VisitContent.Plan {
        val prefs=context.getSharedPreferences("visit_repertoire",Context.MODE_PRIVATE)
        val lines=VisitContent.dialogue.getValue(character)
        val gestures=VisitContent.gestures.getValue(character)
        fun draw(kind:String,pool:List<String>):VisitContent.Draw {
            val key="$character.$kind"
            val result=VisitContent.draw(pool,prefs.getString("$key.remaining","").orEmpty().split(','),prefs.getString("$key.last",null))
            prefs.edit().putString("$key.remaining",result.remaining.joinToString(",")).putString("$key.last",result.selected).apply()
            return result
        }
        val line=draw("lines",lines.map{it.id})
        val gesture=draw("gestures",gestures.map{it.id})
        return VisitContent.Plan(lines.first{it.id==line.selected},gestures.first{it.id==gesture.selected})
    }
}
