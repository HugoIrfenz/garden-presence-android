'use strict';
const renderer = new GardenSpriteRenderer(document.getElementById('stage'), {
  onEvent(event) { if(globalThis.GardenNative) GardenNative.report(JSON.stringify(event)); }
});
globalThis.GardenRenderer = renderer;
globalThis.GardenPreview = {
  load(id, reduced) { renderer.setReducedMotion(reduced); return renderer.loadCharacter(id); },
  inspect() { return {character:renderer.character?.id, expression:renderer.expression, paused:renderer.paused, reduced:renderer.reduced, layers:renderer.images.size, fallback:renderer.fallback, visible:renderer.visible, clock:renderer.clock, motion:renderer.motion?.name||null, gestureCount:Object.keys(globalThis.GardenVisitMotion?.durations||{}).length, bitmapWidth:renderer.canvas.width, bitmapHeight:renderer.canvas.height}; }
};
window.addEventListener('error', e=>GardenNative.report(JSON.stringify({type:'onError', message:String(e.message).slice(0,500)})));
