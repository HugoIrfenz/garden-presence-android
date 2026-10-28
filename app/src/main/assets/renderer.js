/* Offline sprite adapter. No scheduling, microphone, camera, network or speech. */
(function (global) {
  'use strict';
  class GardenSpriteRenderer {
    constructor(canvas, options = {}) {
      this.canvas = canvas; this.ctx = canvas.getContext('2d');
      this.manifest = options.manifest || global.GardenSprites;
      this.assetBase = options.assetBase || ''; this.onEvent = options.onEvent || (() => {});
      this.generation = 0; this.images = new Map(); this.character = null;
      this.expression = 'quiet'; this.clock = 0; this.lastFrame = 0; this.frame = 0;
      this.paused = false; this.disposed = false; this.closedUntil = 0; this.nextBlink = Infinity;
      this.motion = null; this.visible = true; this.reduced = global.matchMedia('(prefers-reduced-motion: reduce)').matches;
      this.reduceQuery = global.matchMedia('(prefers-reduced-motion: reduce)');
      this.onReduce = e => this.setReducedMotion(e.matches);
      this.reduceQuery.addEventListener('change', this.onReduce);
      this.onVisibility = () => { if(document.hidden)this.stopFrames();else this.startFrames(); };
      document.addEventListener('visibilitychange', this.onVisibility);
      this.observer = new ResizeObserver(() => this.resize()); this.observer.observe(canvas);
      this.tick = this.tick.bind(this); this.resize();
    }
    emit(type, detail = {}) { this.onEvent({ type, characterId:this.character?.id, generation:this.generation, ...detail }); }
    async loadCharacter(id) {
      if(this.disposed)throw new Error('Renderer disposed');
      const profile = this.manifest.characters[id]; if(!profile)throw new Error('Unknown character '+id);
      const generation = ++this.generation;
      this.stopFrames(); this.images.clear(); this.character = null; this.motion = null;
      this.clock = 0; this.expression = 'quiet'; this.closedUntil = 0; this.visible = true; this.draw();
      const images = new Map();
      const urls = [profile.neutral, ...profile.layers.map(l=>l.src)];
      const results = await Promise.allSettled(urls.map(src=>new Promise((resolve,reject)=>{
        const img = new Image(); img.onload=()=>{images.set(src,img);resolve(src);};
        img.onerror=()=>reject(new Error('Could not load '+src)); img.src=this.assetBase+src;
      })));
      if(generation!==this.generation || this.disposed)return false;
      this.character = profile; this.images = images;
      this.fallback = results.some(r=>r.status==='rejected');
      if(this.fallback)this.emit('onError',{message:results.filter(r=>r.status==='rejected').map(r=>r.reason.message).join('; '),fallback:images.has(profile.neutral)});
      if(this.fallback&&!images.has(profile.neutral)){this.character=null;this.images.clear();this.draw();return false;}
      this.planBlink(); this.draw(); this.startFrames();
      this.emit('onReady',{capabilities:profile.capabilities,layered:!this.fallback}); return true;
    }
    planBlink(){const t=this.character?.timing;if(t)this.nextBlink=this.clock+t.blinkMinMs+Math.random()*(t.blinkMaxMs-t.blinkMinMs);}
    setExpression(expression){
      if(!this.character || !Object.hasOwn(this.character.expressions,expression))return false;
      this.expression=expression; this.closedUntil=0; this.planBlink(); this.draw(); return true;
    }
    blink(){if(!this.character || this.reduced)return;this.closedUntil=this.clock+this.character.timing.blinkDurationMs;this.startFrames();}
    setReducedMotion(value){this.reduced=!!value;this.closedUntil=0;this.motion=null;this.stopFrames();this.draw();this.startFrames();}
    playMotion(name){
      const allowed=['idle','knock','greet','look_at_viewer','look_away','hold_board','lower_board','listening','retry','success','goodbye','camera_aside',...Object.keys(global.GardenVisitMotion?.durations||{})];
      if(!this.character || !allowed.includes(name))return false;
      const t=this.character.timing;
      if(name==='greet'||name==='idle')this.visible=true;
      if(name==='look_at_viewer')this.setExpression('quiet');
      if(name==='look_away')this.setExpression('shy');
      if(name==='success')this.setExpression('warm');
      if(name==='retry'||name==='listening')this.setExpression('quiet');
      const duration=global.GardenVisitMotion?.durations[name]||(name==='greet'?t.entryMs:name==='goodbye'?t.exitMs:name==='knock'?Math.max(...t.knockCuesMs)+450:1400);
      this.motion={name,start:this.clock,duration:this.reduced?180:duration};
      const fallback=['knock','hold_board','lower_board','camera_aside'].includes(name);
      this.emit('onMotionStarted',{name,fallback,implementation:name==='knock'?'contact-ripple; no articulated hand motion':name.includes('board')?'host-managed floating board':name==='camera_aside'?'host-managed stage placement':'sprite layers'});
      this.draw();this.startFrames();return true;
    }
    setSpeechState(state){this.emit('onSpeechState',{state,lipSync:false});}
    showBoard(text,style='default'){this.emit('onBoard',{text:String(text),style,attachment:'floating'});}
    pause(){this.paused=true;this.stopFrames();this.closedUntil=0;this.draw();}
    resume(){if(this.disposed)return;this.paused=false;this.planBlink();this.startFrames();}
    stopFrames(){cancelAnimationFrame(this.frame);this.frame=0;this.lastFrame=0;}
    startFrames(){if(this.frame || this.paused || this.disposed || document.hidden || !this.character || (this.reduced&&!this.motion))return;this.frame=requestAnimationFrame(this.tick);}
    tick(time){
      this.frame=0;if(this.paused||this.disposed||document.hidden)return;
      if(!this.lastFrame)this.lastFrame=time;
      const elapsed=time-this.lastFrame;
      if(elapsed>=1000/30){
        this.clock+=Math.min(elapsed,80);this.lastFrame=time;
        if(!this.reduced && this.clock>=this.nextBlink){this.blink();this.planBlink();}
        this.draw();
        if(this.motion && this.clock-this.motion.start>=this.motion.duration){const name=this.motion.name;this.motion=null;if(name==='goodbye'){this.visible=false;this.draw();}this.emit('onMotionFinished',{name});}
      }
      this.startFrames();
    }
    resize(){
      // Xiaomi's 520-dpi display needs more than the desktop preview's 1.5x cap.
      // Bound the bitmap by the source-art dimensions instead of upscaling past them.
      const box=this.canvas.getBoundingClientRect();const dpr=Math.min(global.devicePixelRatio||1,3,1024/Math.max(1,box.width),1536/Math.max(1,box.height));
      this.canvas.width=Math.max(1,Math.round(box.width*dpr));this.canvas.height=Math.max(1,Math.round(box.height*dpr));this.draw();
    }
    draw(){
      const c=this.ctx;if(!c)return;c.setTransform(1,0,0,1,0,0);c.clearRect(0,0,this.canvas.width,this.canvas.height);
      const p=this.character;if(!p||!this.visible)return;
      const scale=Math.min(this.canvas.width/1100,this.canvas.height/1620);
      c.translate((this.canvas.width-1024*scale)/2,(this.canvas.height-1536*scale)/2);c.scale(scale,scale);
      const now=this.reduced?0:this.clock/1000;
      const m=this.motion;let mp=m?Math.min(1,(this.clock-m.start)/m.duration):0;
      if(m?.name==='greet'){
        const eased=1-Math.pow(1-mp,3);c.globalAlpha=eased;
        if(!this.reduced){const direction=['virelia','zaria','nixia'].includes(p.id)?-1:1;c.translate(direction*(p.id==='vivianne'?12:18)*(1-eased),p.id==='nixia'?12*(1-eased):0);}
      }
      if(m?.name==='goodbye'){c.globalAlpha=1-mp;if(!this.reduced)c.translate(10*mp,0);}
      if(this.fallback){const im=this.images.get(p.neutral);if(im)c.drawImage(im,0,0,1024,1536);return;}
      const gesture=global.GardenVisitMotion?.pose(m?.name,mp,this.reduced)||{head:0,headY:0,sleeve:0,hand:0,hair:0,x:0,y:0,lean:0};
      c.translate(512+gesture.x,1200+gesture.y);c.rotate(gesture.lean*Math.PI/180);c.translate(-512,-1200);
      const breath=this.reduced?1:1+(1-Math.cos(now*2*Math.PI/(p.timing.breathMs/1000)))*.0015;
      c.translate(512,1200);c.scale(1,breath);c.translate(-512,-1200);
      let headAngle=this.reduced?0:Math.sin(now*2*Math.PI/(p.id==='nixia'?8:14))*.18;
      if(!this.reduced && m?.name==='success')headAngle+=Math.sin(mp*Math.PI)*.35;
      if(!this.reduced && this.expression==='shy')headAngle-=.25;
      headAngle+=gesture.head;
      const blink=this.clock<this.closedUntil && !(p.id==='virelia'&&this.expression==='shy') && !(p.id==='nixia'&&this.expression==='warm');
      for(const layer of p.layers){
        if(layer.visibleWhen==='blink'&&!blink)continue;
        if(['warm','shy'].includes(layer.visibleWhen)&&layer.visibleWhen!==this.expression)continue;
        const img=this.images.get(layer.src);if(!img)continue;
        c.save();
        if(layer.parent==='head'){const [x,y]=p.headPivot;c.translate(x,y+gesture.headY);c.rotate(headAngle*Math.PI/180);c.translate(-x,-y);}
        if(layer.pivot&&!this.reduced){
          const [x,y]=layer.pivot;
          const side=/left/i.test(layer.name)?1:-1;
          const extra=/gesture|hands/i.test(layer.name)?gesture.hand:/sleeve/i.test(layer.name)?side*gesture.sleeve:/hair|front|back|braid|bob|bun|ribbon|lock|earring|charm|tie/i.test(layer.name)?side*gesture.hair:0;
          c.translate(x,y);c.rotate((Math.sin(now*2*Math.PI/layer.idle.periodSeconds)*layer.idle.rotationDegrees+extra)*Math.PI/180);c.translate(-x,-y);
        }
        c.drawImage(img,layer.x,layer.y,layer.width,layer.height);c.restore();
      }
      if(m?.name==='knock')for(const cue of p.timing.knockCuesMs){const age=this.clock-m.start-cue;if(age>=0&&age<400){const a=age/400;c.save();c.globalAlpha=(1-a)*.55;c.strokeStyle=p.color;c.lineWidth=2;c.beginPath();c.arc(860,470,8+a*36,0,Math.PI*2);c.stroke();c.restore();}}
    }
    dispose(){this.disposed=true;++this.generation;this.stopFrames();this.images.clear();this.character=null;this.observer.disconnect();this.reduceQuery.removeEventListener('change',this.onReduce);document.removeEventListener('visibilitychange',this.onVisibility);this.draw();}
  }
  global.GardenSpriteRenderer=GardenSpriteRenderer;
})(globalThis);
