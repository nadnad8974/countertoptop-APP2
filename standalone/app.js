(() => {
  const JOBS_KEY = 'ct_standalone_jobs_v1';
  const SETTINGS_KEY = 'ct_standalone_settings_v1';

  const sampleJobs = [
    {
      id: crypto.randomUUID(), customerName: 'A. Ramirez', jobName: 'Straight countertop', address: '121 Oakview Dr', roomName: 'Kitchen',
      notes: 'Demo sink cutout', createdAt: new Date().toISOString(), modifiedAt: new Date().toISOString(),
      pieces: [{ id: crypto.randomUUID(), name: 'Main Top', pieceType: 'main_top', closed: true, locked: false,
        points:[p(0,0),p(120,0),p(120,25.5),p(0,25.5)],
        cutouts:[{id:crypto.randomUUID(), type:'sink', label:'Sink', x:43.5,y:5,width:33,depth:18}], seams:[] }]
    },
    {
      id: crypto.randomUUID(), customerName: 'B. Chen', jobName: 'L-shaped kitchen', address: '89 Riverview Ct', roomName: 'Kitchen',
      notes: 'Demo cooktop + seam', createdAt: new Date().toISOString(), modifiedAt: new Date().toISOString(),
      pieces: [{ id: crypto.randomUUID(), name: 'L Main', pieceType: 'main_top', closed: true, locked: false,
        points:[p(0,0),p(110,0),p(110,25),p(65,25),p(65,82),p(0,82)],
        cutouts:[{id:crypto.randomUUID(), type:'cooktop', label:'Cooktop', x:20,y:30,width:30,depth:21}], seams:[{id:crypto.randomUUID(),label:'Seam A',start:{x:65,y:25},end:{x:65,y:82}}] }]
    },
    {
      id: crypto.randomUUID(), customerName: 'C. Brooks', jobName: 'Island rounded corners', address: '700 Meadow Ln', roomName: 'Kitchen',
      notes: 'Radius corner notes', createdAt: new Date().toISOString(), modifiedAt: new Date().toISOString(),
      pieces: [{ id: crypto.randomUUID(), name: 'Island', pieceType: 'island', closed: true, locked: false,
        points:[p(0,0),p(78,0),p(78,44),p(0,44)], cutouts:[], seams:[] }]
    }
  ];
  const demoLayouts = {
    straight: [p(0,0), p(120,0), p(120,25.5), p(0,25.5)],
    lshape: [p(0,0), p(110,0), p(110,25), p(65,25), p(65,82), p(0,82)],
    island: [p(0,0), p(78,0), p(78,44), p(0,44)],
  };

  function p(x,y){ return { id: crypto.randomUUID(), x, y }; }
  function loadJobs(){ const raw=localStorage.getItem(JOBS_KEY); if(!raw){ localStorage.setItem(JOBS_KEY, JSON.stringify(sampleJobs)); return sampleJobs; } return JSON.parse(raw); }
  function saveJobs(){ localStorage.setItem(JOBS_KEY, JSON.stringify(state.jobs)); }
  function loadSettings(){ return JSON.parse(localStorage.getItem(SETTINGS_KEY) || '{"units":"in","precision":2,"snapToGrid":false,"gridSize":0.25}'); }
  function saveSettings(){ localStorage.setItem(SETTINGS_KEY, JSON.stringify(state.settings)); }

  const state = { screen:'jobs', jobs:loadJobs(), settings:loadSettings(), activeJobId:null, activePieceId:null, tool:'add', cam:{scale:3,tx:80,ty:80}, dragPoint:null, panStart:null, hist:[], fut:[] };
  state.activeJobId = state.jobs[0]?.id || null;
  state.activePieceId = activeJob()?.pieces[0]?.id || null;

  const app = document.getElementById('app');
  document.getElementById('navJobs').onclick = () => { state.screen='jobs'; render(); };
  document.getElementById('navSettings').onclick = () => { state.screen='settings'; render(); };

  function activeJob(){ return state.jobs.find(j=>j.id===state.activeJobId)||null; }
  function activePiece(){ const j=activeJob(); return j?.pieces.find(x=>x.id===state.activePieceId) || j?.pieces[0] || null; }
  function updateJob(next){ state.hist.push(structuredClone(activeJob())); if(state.hist.length>50) state.hist.shift(); state.fut=[]; const i=state.jobs.findIndex(j=>j.id===next.id); state.jobs[i]=next; saveJobs(); }

  function render(){ app.innerHTML=''; if(state.screen==='jobs') return renderJobs(); if(state.screen==='details') return renderDetails(); if(state.screen==='editor') return renderEditor(); if(state.screen==='export') return renderExport(); if(state.screen==='settings') return renderSettings(); }

  function renderJobs(){
    const wrap = el('div');
    wrap.append(row([btn('New Job', ()=>{ const now=new Date().toISOString(); const job={id:crypto.randomUUID(),customerName:'',jobName:`New Job ${state.jobs.length+1}`,address:'',roomName:'Kitchen',notes:'',createdAt:now,modifiedAt:now,pieces:[{id:crypto.randomUUID(),name:'Main Top',pieceType:'main_top',points:[],closed:false,locked:false,cutouts:[],seams:[]}]}; state.jobs.unshift(job); state.activeJobId=job.id; state.activePieceId=job.pieces[0].id; saveJobs(); state.screen='details'; render(); })]));
    state.jobs.forEach(job=>{
      const card=el('div','job');
      card.innerHTML=`<strong>${job.jobName}</strong><div>${job.customerName||'No customer'}</div><div>${job.address||''}</div><small>${new Date(job.modifiedAt).toLocaleString()}</small>`;
      card.append(row([
        btn('Open', ()=>{ state.activeJobId=job.id; state.activePieceId=job.pieces[0]?.id||null; state.screen='details'; render(); }),
        btn('Duplicate', ()=>{ const c=structuredClone(job); c.id=crypto.randomUUID(); c.jobName += ' (Copy)'; c.createdAt=new Date().toISOString(); c.modifiedAt=c.createdAt; state.jobs.unshift(c); saveJobs(); render(); }),
        btn('Delete', ()=>{ state.jobs = state.jobs.filter(x=>x.id!==job.id); saveJobs(); if(state.activeJobId===job.id) state.activeJobId=state.jobs[0]?.id||null; render(); })
      ]));
      wrap.append(card);
    });
    app.append(wrap);
  }

  function renderDetails(){
    const j=activeJob(); if(!j) return;
    const panel = el('div','panel');
    panel.innerHTML = `<h2>Job Details</h2>`;
    panel.append(
      field('Customer', j.customerName, v=>{j.customerName=v; j.modifiedAt=new Date().toISOString(); saveJobs();}),
      field('Job Name', j.jobName, v=>{j.jobName=v; j.modifiedAt=new Date().toISOString(); saveJobs();}),
      field('Address', j.address, v=>{j.address=v; j.modifiedAt=new Date().toISOString(); saveJobs();}),
      field('Room', j.roomName, v=>{j.roomName=v; j.modifiedAt=new Date().toISOString(); saveJobs();}),
      area('Notes', j.notes, v=>{j.notes=v; j.modifiedAt=new Date().toISOString(); saveJobs();}),
      row([btn('Back', ()=>{state.screen='jobs'; render();}), btn('Open Editor', ()=>{state.screen='editor'; render();})])
    );
    app.append(panel);
  }

  function renderEditor(){
    const j=activeJob(); const piece=activePiece(); if(!j||!piece) return;
    const grid = el('div','grid');
    const left = el('div','panel');
    left.innerHTML='<h3>Template Editor (Simulator)</h3>';
    left.append(row([
      btn('Undo', ()=>{const prev=state.hist.pop(); if(!prev) return; state.fut.unshift(structuredClone(activeJob())); const idx=state.jobs.findIndex(x=>x.id===j.id); state.jobs[idx]=prev; saveJobs(); render();}),
      btn('Redo', ()=>{const next=state.fut.shift(); if(!next) return; state.hist.push(structuredClone(activeJob())); const idx=state.jobs.findIndex(x=>x.id===j.id); state.jobs[idx]=next; saveJobs(); render();}),
    ]));
    left.append(row([
      btn('Back', ()=>{state.screen='details'; render();}),
      btn('Export', ()=>{state.screen='export'; render();}),
    ]));
    left.append(el('div')).innerHTML='<h4>Sections</h4>';
    j.pieces.forEach(sec=> left.append(btn(sec.name, ()=>{state.activePieceId=sec.id; render();}, sec.id===piece.id)));
    left.append(btn('+ Add Section', ()=>{ const np={id:crypto.randomUUID(),name:`Piece ${j.pieces.length+1}`,pieceType:'separate',points:[],closed:false,locked:false,cutouts:[],seams:[]}; const nj=structuredClone(j); nj.pieces.push(np); updateJob(nj); state.activePieceId=np.id; render(); }));
    left.append(selectField('Tool Mode',['add','select','insert','pan'],state.tool,v=>{state.tool=v; render();}));
    left.append(el('div')).innerHTML='<h4>Manual XY</h4>';
    const xI=num('X',0), yI=num('Y',0); left.append(xI.wrap,yI.wrap,btn('Add Point by XY',()=>{ const nj=structuredClone(j); const ap=nj.pieces.find(x=>x.id===piece.id); ap.points.push(p(Number(xI.input.value),Number(yI.input.value))); updateJob(nj); render(); }));
    left.append(el('div')).innerHTML='<h4>Demo Layouts</h4>';
    left.append(row([
      btn('Straight',()=>loadDemo('straight')), btn('L-Shape',()=>loadDemo('lshape')), btn('Island',()=>loadDemo('island'))
    ]));
    left.append(el('div')).innerHTML='<h4>Countertop Tools</h4>';
    left.append(row([
      btn('Sink',()=>{const nj=structuredClone(j); const ap=nj.pieces.find(x=>x.id===piece.id); ap.cutouts.push({id:crypto.randomUUID(),type:'sink',label:'Sink',x:10,y:5,width:33,depth:18}); updateJob(nj); render();}),
      btn('Cooktop',()=>{const nj=structuredClone(j); const ap=nj.pieces.find(x=>x.id===piece.id); ap.cutouts.push({id:crypto.randomUUID(),type:'cooktop',label:'Cooktop',x:20,y:20,width:30,depth:21}); updateJob(nj); render();}),
      btn('Seam',()=>{const nj=structuredClone(j); const ap=nj.pieces.find(x=>x.id===piece.id); ap.seams.push({id:crypto.randomUUID(),label:`Seam ${ap.seams.length+1}`,start:{x:10,y:10},end:{x:40,y:10}}); updateJob(nj); render();}),
    ]));

    const right = el('div','canvas-wrap');
    right.append(row([
      btn(piece.closed?'Open Shape':'Close Shape',()=>{const nj=structuredClone(j); const ap=nj.pieces.find(x=>x.id===piece.id); ap.closed=!ap.closed; updateJob(nj); render();}),
      btn(piece.locked?'Unlock':'Lock',()=>{const nj=structuredClone(j); const ap=nj.pieces.find(x=>x.id===piece.id); ap.locked=!ap.locked; updateJob(nj); render();}),
      btn('- Zoom',()=>{state.cam.scale=Math.max(.5,state.cam.scale*.9); render();}),
      btn('+ Zoom',()=>{state.cam.scale=Math.min(15,state.cam.scale*1.1); render();}),
      btn('Reset View',()=>{state.cam={scale:3,tx:80,ty:80}; render();}),
    ]));

    const b = bounds(piece.points);
    if(b) right.append(el('div')).textContent = `Overall: ${(b.maxX-b.minX).toFixed(state.settings.precision)} ${state.settings.units} W × ${(b.maxY-b.minY).toFixed(state.settings.precision)} ${state.settings.units} D`;

    const c=document.createElement('canvas'); c.width=1000; c.height=560;
    right.append(c);
    wireCanvas(c,piece,j);

    const list = el('div','points'); list.innerHTML='<h4>Points</h4>';
    piece.points.forEach((pt,idx)=>{
      const rowDiv=el('div','point-row'); const x=document.createElement('input'); x.type='number'; x.value=pt.x; const y=document.createElement('input'); y.type='number'; y.value=pt.y;
      x.onchange=()=>{ const nj=structuredClone(j); const ap=nj.pieces.find(x=>x.id===piece.id); ap.points[idx].x=Number(x.value); updateJob(nj); draw(c,piece); };
      y.onchange=()=>{ const nj=structuredClone(j); const ap=nj.pieces.find(x=>x.id===piece.id); ap.points[idx].y=Number(y.value); updateJob(nj); draw(c,piece); };
      rowDiv.append(span(`P${idx+1}`),x,y,btn('Delete',()=>{ const nj=structuredClone(j); const ap=nj.pieces.find(x=>x.id===piece.id); ap.points.splice(idx,1); updateJob(nj); render(); }));
      list.append(rowDiv);
    });
    right.append(list);

    grid.append(left,right);
    app.append(grid);

    function loadDemo(kind){ const nj=structuredClone(j); const ap=nj.pieces.find(x=>x.id===piece.id); ap.points=demoLayouts[kind].map(q=>({id:crypto.randomUUID(),x:q.x,y:q.y})); ap.closed=true; updateJob(nj); render(); }
  }

  function wireCanvas(canvas,piece,job){
    draw(canvas,piece);
    canvas.onpointerdown=(e)=>{
      const world = toWorld(e.offsetX,e.offsetY);
      if(state.tool==='pan'){ state.panStart={x:e.clientX,y:e.clientY}; return; }
      if(piece.locked) return;
      if(state.tool==='add'){ const nj=structuredClone(job); const ap=nj.pieces.find(x=>x.id===piece.id); ap.points.push(snap({id:crypto.randomUUID(),x:world.x,y:world.y})); updateJob(nj); render(); return; }
      if(state.tool==='insert'){ insertNear(world, job, piece); return; }
      if(state.tool==='select'){ const hit = hitPoint(world,piece.points); if(hit) state.dragPoint=hit.id; }
    };
    canvas.onpointermove=(e)=>{
      if(state.dragPoint){ const nj=structuredClone(activeJob()); const ap=nj.pieces.find(x=>x.id===activePiece().id); const pt=ap.points.find(x=>x.id===state.dragPoint); const w=toWorld(e.offsetX,e.offsetY); Object.assign(pt,snap({x:w.x,y:w.y})); updateJob(nj); draw(canvas,ap); return; }
      if(state.panStart){ const dx=e.clientX-state.panStart.x, dy=e.clientY-state.panStart.y; state.cam.tx+=dx; state.cam.ty+=dy; state.panStart={x:e.clientX,y:e.clientY}; draw(canvas,piece); }
    };
    canvas.onpointerup=()=>{ state.dragPoint=null; state.panStart=null; };
    canvas.onwheel=(e)=>{ e.preventDefault(); state.cam.scale=Math.max(.5,Math.min(15,state.cam.scale*(e.deltaY>0?.95:1.05))); draw(canvas,piece); };
  }

  function draw(canvas,piece){
    const ctx=canvas.getContext('2d'); ctx.clearRect(0,0,canvas.width,canvas.height);
    const sc=(x,y)=>({x:x*state.cam.scale+state.cam.tx,y:y*state.cam.scale+state.cam.ty});
    ctx.strokeStyle='#20b2aa'; ctx.lineWidth=2;
    piece.points.forEach((pt,i)=>{ const n=piece.points[i+1] || (piece.closed?piece.points[0]:null); if(!n) return; const a=sc(pt.x,pt.y), b=sc(n.x,n.y); ctx.beginPath(); ctx.moveTo(a.x,a.y); ctx.lineTo(b.x,b.y); ctx.stroke();
      const len=Math.hypot(n.x-pt.x,n.y-pt.y).toFixed(state.settings.precision); ctx.fillStyle='#111'; ctx.fillText(`${len} ${state.settings.units}`,(a.x+b.x)/2+8,(a.y+b.y)/2-8);
    });
    piece.points.forEach((pt,i)=>{ const s=sc(pt.x,pt.y); ctx.fillStyle='#2563eb'; ctx.beginPath(); ctx.arc(s.x,s.y,6,0,Math.PI*2); ctx.fill(); ctx.fillStyle='#111'; ctx.fillText(`P${i+1} (${pt.x.toFixed(2)}, ${pt.y.toFixed(2)})`, s.x+8,s.y-8); });
    piece.cutouts.forEach(c=>{ const p=sc(c.x,c.y); ctx.setLineDash([5,4]); ctx.strokeStyle='#ef4444'; ctx.strokeRect(p.x,p.y,c.width*state.cam.scale,c.depth*state.cam.scale); ctx.setLineDash([]); });
    piece.seams.forEach(s=>{ const a=sc(s.start.x,s.start.y),b=sc(s.end.x,s.end.y); ctx.setLineDash([7,3]); ctx.strokeStyle='#7c3aed'; ctx.beginPath(); ctx.moveTo(a.x,a.y); ctx.lineTo(b.x,b.y); ctx.stroke(); ctx.setLineDash([]); });
  }

  function hitPoint(w,pts){ return pts.find(pt=>Math.hypot(pt.x-w.x,pt.y-w.y)<=6/state.cam.scale); }
  function insertNear(w,job,piece){ if(piece.points.length<2) return; let best={idx:0,d:Infinity}; for(let i=0;i<piece.points.length-1+(piece.closed?1:0);i++){const a=piece.points[i], b=piece.points[(i+1)%piece.points.length]; const d=segDist(w,a,b); if(d<best.d) best={idx:i,d}; }
    const nj=structuredClone(job); const ap=nj.pieces.find(x=>x.id===piece.id); ap.points.splice(best.idx+1,0,snap({id:crypto.randomUUID(),x:w.x,y:w.y})); updateJob(nj); render(); }
  function segDist(p,a,b){ const dx=b.x-a.x, dy=b.y-a.y; const t=Math.max(0,Math.min(1,((p.x-a.x)*dx+(p.y-a.y)*dy)/(dx*dx+dy*dy||1))); const x=a.x+t*dx, y=a.y+t*dy; return Math.hypot(p.x-x,p.y-y); }
  function toWorld(sx,sy){ return { x:(sx-state.cam.tx)/state.cam.scale, y:(sy-state.cam.ty)/state.cam.scale }; }
  function snap(pt){ if(!state.settings.snapToGrid) return pt; const g=Number(state.settings.gridSize)||0.25; return {...pt,x:Math.round(pt.x/g)*g,y:Math.round(pt.y/g)*g}; }
  function bounds(points){ if(!points.length) return null; const xs=points.map(p=>p.x), ys=points.map(p=>p.y); return {minX:Math.min(...xs),maxX:Math.max(...xs),minY:Math.min(...ys),maxY:Math.max(...ys)}; }

  function renderExport(){
    const j=activeJob(); if(!j) return;
    const p=el('div','panel'); p.innerHTML='<h2>Export</h2><p>DXF/PDF/PNG placeholders for standalone build.</p>';
    p.append(row([
      btn('Back',()=>{state.screen='editor'; render();}),
      btn('Export JSON',()=>download(`${j.jobName.replace(/\s+/g,'_')}.json`,JSON.stringify(j,null,2),'application/json')),
      btn('Export DXF placeholder',()=>download(`${j.jobName.replace(/\s+/g,'_')}.dxf`,dxf(j),'application/dxf'))
    ]));
    app.append(p);
  }

  function dxf(job){ const out=['0','SECTION','2','ENTITIES']; job.pieces.forEach(piece=>{ piece.points.forEach((pt,i)=>{ const n=piece.points[i+1] || (piece.closed?piece.points[0]:null); if(!n) return; out.push('0','LINE','8',piece.name,'10',String(pt.x),'20',String(pt.y),'11',String(n.x),'21',String(n.y)); });}); out.push('0','ENDSEC','0','EOF'); return out.join('\n'); }
  function download(name,txt,type){ const b=new Blob([txt],{type}); const a=document.createElement('a'); a.href=URL.createObjectURL(b); a.download=name; a.click(); URL.revokeObjectURL(a.href); }

  function renderSettings(){
    const s=el('div','panel'); s.innerHTML='<h2>Settings</h2>';
    s.append(selectField('Units',['in','mm'],state.settings.units,v=>{state.settings.units=v; saveSettings();}),
      num('Precision',state.settings.precision,(v)=>{state.settings.precision=Number(v); saveSettings();}).wrap,
      check('Snap to Grid',state.settings.snapToGrid,v=>{state.settings.snapToGrid=v; saveSettings();}),
      num('Grid Size',state.settings.gridSize,(v)=>{state.settings.gridSize=Number(v); saveSettings();}).wrap
    );
    app.append(s);
  }

  function el(tag,cls){ const x=document.createElement(tag); if(cls) x.className=cls; return x; }
  function btn(label,fn,active){ const b=document.createElement('button'); b.textContent=label; b.onclick=fn; if(active) b.classList.add('active'); return b; }
  function row(nodes){ const d=el('div','row'); nodes.forEach(n=>d.append(n)); return d; }
  function field(label,value,on){ const w=el('label'); w.textContent=label; const i=document.createElement('input'); i.value=value||''; i.onchange=()=>on(i.value); w.append(i); return w; }
  function area(label,value,on){ const w=el('label'); w.textContent=label; const i=document.createElement('textarea'); i.value=value||''; i.onchange=()=>on(i.value); w.append(i); return w; }
  function selectField(label,opts,val,on){ const w=el('label'); w.textContent=label; const s=document.createElement('select'); opts.forEach(o=>{const op=document.createElement('option'); op.value=o; op.textContent=o; if(o===val) op.selected=true; s.append(op);}); s.onchange=()=>on(s.value); w.append(s); return w; }
  function num(label,value,on){ const w=el('label'); w.textContent=label; const i=document.createElement('input'); i.type='number'; i.value=value; i.onchange=()=>on && on(i.value); w.append(i); return {wrap:w,input:i}; }
  function check(label,value,on){ const w=el('label'); w.textContent=label; const i=document.createElement('input'); i.type='checkbox'; i.checked=value; i.onchange=()=>on(i.checked); w.append(i); return w; }
  function span(t){ const s=document.createElement('span'); s.textContent=t; return s; }

  render();
})();
