package home.garden.presence

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager

/** Audio exists only while an explicit alarm is ringing; never an always-running service. */
class AngelaAlarmService:Service(){
    private val handler=Handler(Looper.getMainLooper())
    private var tone:Ringtone?=null
    private var lock:PowerManager.WakeLock?=null
    private var focus:AudioFocusRequest?=null
    private var token:String?=null
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        val incoming=intent?.getStringExtra("token")
        if(!AngelaAlarmScheduler.active(this,incoming)){stopSelf();return START_NOT_STICKY}
        startForeground(AngelaAlarmScheduler.NOTICE,AngelaAlarmScheduler.notification(this,incoming!!),ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        if(token==incoming)return START_NOT_STICKY
        releasePlayback()
        token=incoming
        val attrs=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        val audio=getSystemService(AudioManager::class.java)
        focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attrs).setOnAudioFocusChangeListener{change->
            if(change==AudioManager.AUDIOFOCUS_LOSS)AngelaAlarmScheduler.stop(this,"audio_interrupted")
        }.build().also {audio.requestAudioFocus(it)}
        runCatching{
            tone=RingtoneManager.getRingtone(this,RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).apply{audioAttributes=attrs;isLooping=true;play()}
        }.onFailure{AngelaAlarmScheduler.prefs(this).edit().putString("audio_error",it.javaClass.simpleName).apply()}
        lock=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Garden:AngelaAlarm").apply{acquire(130_000)}
        val started=AngelaAlarmScheduler.prefs(this).getLong("ring_started",System.currentTimeMillis())
        handler.postDelayed({if(token==AngelaAlarmScheduler.prefs(this).getString("ringing",null))AngelaAlarmScheduler.stop(this,"missed")},(120_000-(System.currentTimeMillis()-started)).coerceIn(1,120_000))
        return START_NOT_STICKY
    }
    private fun releasePlayback(){
        handler.removeCallbacksAndMessages(null);tone?.stop();tone=null
        focus?.let{getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it)}
        focus=null
        lock?.let{if(it.isHeld)it.release()};lock=null
    }
    override fun onDestroy(){
        releasePlayback()
        stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy()
    }
}
