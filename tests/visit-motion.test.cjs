const {test}=require('node:test');
const assert=require('node:assert/strict');
const motion=require('../app/src/main/assets/visit-motion.js');
test('every native gesture is implemented by the bundled renderer',()=>{
  const source=require('node:fs').readFileSync(require('node:path').join(__dirname,'../app/src/main/java/home/garden/presence/VisitContent.kt'),'utf8');
  const names=[...source.matchAll(/Gesture\("([a-z_]+)"/g)].map(match=>match[1]);
  assert.equal(names.length,20);
  for(const name of names)assert.ok(Object.hasOwn(motion.durations,name),name);
});
test('all twelve gestures stay bounded and ease back to their registered pose',()=>{
  assert.equal(Object.keys(motion.durations).length,12);
  for(const name of Object.keys(motion.durations)){
    for(let i=0;i<=100;i++){
      const p=motion.pose(name,i/100);
      for(const v of Object.values(p))assert.ok(Number.isFinite(v));
      assert.ok(Math.abs(p.head)<=.81&&Math.abs(p.hand)<=1.36&&Math.abs(p.lean)<=.46);
      assert.ok(Math.abs(p.x)<=6.01&&Math.abs(p.y)<=4.01);
      if(i===0||i===100)for(const v of Object.values(p))assert.ok(Math.abs(v)<1e-10);
    }
  }
});
test('reduced motion disables every layer and position gesture',()=>{
  for(const name of Object.keys(motion.durations))for(const value of Object.values(motion.pose(name,.35,true)))assert.equal(value,0);
});
test('gesture profiles are distinct and unknown motions are neutral',()=>{
  const signatures=Object.keys(motion.durations).map(name=>JSON.stringify([.25,.5,.75].map(t=>motion.pose(name,t))));
  assert.equal(new Set(signatures).size,signatures.length);
  for(const value of Object.values(motion.pose('missing',.5)))assert.equal(value,0);
});
