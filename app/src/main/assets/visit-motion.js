/* Bounded layer gestures. No new assets, timers, audio, or synthetic lip-sync. */
(function(global){
  'use strict';
  const durations={nod:1800,soft_tilt:2600,shy_glance:2800,sleeve_nest:2500,open_hand:2100,bright_nod:1800,small_bow:2200,flustered:1900,editorial_turn:2700,breezy:2600,peek:1500,cheer:1600};
  function pose(name,progress,reduced=false){
    const p={head:0,headY:0,sleeve:0,hand:0,hair:0,x:0,y:0,lean:0};
    if(reduced||!Object.hasOwn(durations,name))return p;
    const t=Math.max(0,Math.min(1,progress));
    // Zero position and velocity at both ends keeps the idle/gesture handoff soft.
    const e=Math.sin(Math.PI*t)**2;
    const flutter=e*Math.sin(t*Math.PI*4);
    switch(name){
      case 'nod':p.head=.45*e;p.headY=2*e;break;
      case 'soft_tilt':p.head=-.8*e;p.hair=.22*e;break;
      case 'shy_glance':p.head=.6*e;p.headY=1.5*e;p.sleeve=.3*e;break;
      case 'sleeve_nest':p.sleeve=.8*e;p.hand=.25*e;p.head=-.35*e;break;
      case 'open_hand':p.hand=-1.35*e;p.sleeve=.25*e;p.head=-.4*e;break;
      case 'bright_nod':p.head=.55*e;p.headY=2.2*e;p.hand=-.9*e;p.hair=.25*flutter;break;
      case 'small_bow':p.lean=.35*e;p.y=3*e;p.head=.6*e;p.headY=2*e;p.hand=.5*e;break;
      case 'flustered':p.head=.55*flutter;p.hand=.8*flutter;p.hair=.35*flutter;p.lean=.25*flutter;break;
      case 'editorial_turn':p.head=-.65*e;p.lean=-.3*e;p.sleeve=.45*e;p.hair=.3*e;break;
      case 'breezy':p.hair=.6*e;p.sleeve=.65*e;p.head=.4*e;break;
      case 'peek':p.x=-6*e;p.lean=.45*e;p.head=-.7*e;p.hand=-.65*e;break;
      case 'cheer':p.y=-4*e;p.hand=-1.25*e;p.head=.5*e;p.hair=.4*flutter;break;
    }
    return p;
  }
  const api={durations,pose};global.GardenVisitMotion=api;
  if(typeof module!=='undefined'&&module.exports)module.exports=api;
})(globalThis);
