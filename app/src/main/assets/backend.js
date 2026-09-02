(function(){
  'use strict';
  const cfg=window.GM_FIREBASE_CONFIG||{};
  const ready=()=>!!(cfg.apiKey&&cfg.projectId&&cfg.databaseURL&&window.firebase);
  const state={enabled:false,auth:null,db:null,user:null,seen:new Set(),listeners:{}};
  const norm=p=>String(p||'').replace(/\D/g,'');
  const safe=s=>String(s||'').replace(/[^a-zA-Z0-9_-]/g,'_').slice(0,120)||'unknown';
  const conv=(a,b)=>[String(a||''),String(b||'')].sort().join('__')||'unknown';
  const toast=t=>window.toast?window.toast(t):console.log(t);

  function status(){return {enabled:state.enabled,uid:state.user&&state.user.uid||'',email:state.user&&state.user.email||''};}
  function emitStatus(){window.dispatchEvent(new CustomEvent('gm-backend-status',{detail:status()}));}

  async function init(){
    if(!ready()){emitStatus();return false;}
    try{
      if(!firebase.apps.length) firebase.initializeApp(cfg);
      state.auth=firebase.auth(); state.db=firebase.database();
      await state.auth.signInAnonymously();
      state.user=state.auth.currentUser;
      state.enabled=!!state.user;
      if(window.profile) await syncProfile(window.profile);
      emitStatus();
      return state.enabled;
    }catch(e){
      console.warn('G Messenger backend disabled:',e);
      state.enabled=false; emitStatus(); return false;
    }
  }

  async function syncProfile(p){
    if(!state.enabled||!state.user||!p)return;
    const record={uid:state.user.uid,id:p.id||'',name:p.name||'G Messenger user',phone:p.phone||'',photo:p.photo||'',updatedAt:firebase.database.ServerValue.TIMESTAMP};
    await state.db.ref('users/'+state.user.uid).update(record);
    if(norm(p.phone)) await state.db.ref('usersByPhone/'+norm(p.phone)).set(record);
    if(p.id) await state.db.ref('usersById/'+safe(p.id.toLowerCase())).set(record);
  }

  async function findUser(phone,id){
    if(!state.enabled)return null;
    try{
      let snap=null;
      if(norm(phone)) snap=await state.db.ref('usersByPhone/'+norm(phone)).once('value');
      if(snap&&snap.exists()) return snap.val();
      if(id) {snap=await state.db.ref('usersById/'+safe(String(id).trim().toLowerCase())).once('value');if(snap.exists())return snap.val();}
    }catch(e){console.warn(e)}
    return null;
  }

  async function sendMessage(chat,msg){
    if(!state.enabled||!state.user||!chat||chat.self)return;
    const senderPhone=window.profile&&window.profile.phone||'';
    let recipient=chat.uid?{uid:chat.uid,phone:chat.phone}:await findUser(chat.phone,chat.id);
    if(!recipient||!recipient.uid)return;
    chat.uid=recipient.uid;
    const keyParts=[state.user.uid,recipient.uid].sort();
    const ref=state.db.ref('messages/'+keyParts[0]+'/'+keyParts[1]).push();
    const payload={id:ref.key,clientId:msg.clientId||('c_'+Date.now()+'_'+Math.random().toString(36).slice(2,8)),senderPhone,recipientPhone:recipient.phone||chat.phone||'',senderUid:state.user.uid,recipientUid:recipient.uid,senderId:window.profile&&window.profile.id||'',text:msg.text||'',time:msg.time||'',createdAt:firebase.database.ServerValue.TIMESTAMP};
    await ref.set(payload);
  }

  async function watchChat(i){
    if(!state.enabled||!window.chats||!window.chats[i])return;
    const chat=window.chats[i]; if(chat.self)return;
    const recipient=chat.uid?{uid:chat.uid}:await findUser(chat.phone,chat.id); if(!recipient||!recipient.uid)return;
    chat.uid=recipient.uid;
    const keyParts=[state.user.uid,recipient.uid].sort(), key=keyParts.join('__');
    if(state.listeners[key])return;
    const ref=state.db.ref('messages/'+keyParts[0]+'/'+keyParts[1]).limitToLast(200);
    const handler=snap=>{const m=snap.val()||{};if(!m.id||state.seen.has(m.id))return;state.seen.add(m.id);const mine=m.senderUid===state.user.uid;const c=window.chats&&window.chats[i];if(!c)return;if(c.messages.some(x=>x.backendId===m.id||x.clientId===m.clientId))return;c.messages.push({mine,text:m.text||'',time:m.time||'now',backendId:m.id,clientId:m.clientId});window.save&&window.save();if(window.__gmOpenChatIndex===i)window.openChat(i);else window.render&&window.render()};
    ref.on('child_added',handler);state.listeners[key]={ref,handler};
  }

  function watchCurrent(){if(window.__gmOpenChatIndex!=null)watchChat(window.__gmOpenChatIndex)}

  async function onlineSaveContact(name,phone,id){
    const user=await findUser(phone,id);
    if(!user)return false;
    const key='user_'+(user.id||norm(phone)||user.uid);
    if(!window.contacts.some(c=>c.key===key)){
      window.contacts.push({key,name:user.name||name,phone:user.phone||phone,id:user.id||id||'',photo:user.photo||'',registered:true,uid:user.uid||''});
      window.save(); window.closeModal&&window.closeModal(); window.toast&&window.toast('Registered contact saved'); window.render&&window.render();
    }
    return true;
  }

  function patch(){
    if(window.__gmBackendPatched)return; window.__gmBackendPatched=true;
    const originalOpen=window.openChat;
    window.openChat=function(i){window.__gmOpenChatIndex=i;const r=originalOpen.apply(this,arguments);watchChat(i);return r;};
    const originalSend=window.sendMsg;
    window.sendMsg=function(i){
      const before=window.chats&&window.chats[i]&&window.chats[i].messages.length||0;
      const r=originalSend.apply(this,arguments);
      const chat=window.chats&&window.chats[i];
      if(chat&&chat.messages.length>before){const m=chat.messages[chat.messages.length-1];m.clientId=m.clientId||('c_'+Date.now()+'_'+Math.random().toString(36).slice(2,8));window.save&&window.save();sendMessage(chat,m).catch(e=>console.warn(e));}
      return r;
    };
    const originalVerify=window.verifyCode;
    window.verifyCode=function(){const r=originalVerify.apply(this,arguments);setTimeout(()=>{if(window.profile)syncProfile(window.profile).catch(console.warn)},250);return r;};
    const originalSaveProfile=window.saveProfile;
    if(originalSaveProfile)window.saveProfile=function(){const r=originalSaveProfile.apply(this,arguments);setTimeout(()=>{if(window.profile)syncProfile(window.profile).catch(console.warn)},250);return r;};
    const originalSaveContact=window.saveContact;
    if(originalSaveContact)window.saveContact=async function(){
      if(state.enabled){
        const n=document.getElementById('ctName')?.value.trim()||'',p=document.getElementById('ctPhone')?.value.trim()||'',id=document.getElementById('ctId')?.value.trim()||'';
        if(!n||!p)return toast('Name and phone number are required');
        if(await onlineSaveContact(n,p,id))return;
        toast('That phone number or G Messenger ID is not registered online. Invite them instead.');return;
      }
      return originalSaveContact.apply(this,arguments);
    };
  }


  const rtc={calls:{},incomingStarted:false};
  function callMarkup(callId,name){
    return `<div id="gmCall" class="modal"><div class="sheet"><div class="handle"></div><h2>Video call <button class="x" onclick="GMBackend.endVideoCall('${callId}')">✕</button></h2><div style="position:relative;background:#000;border-radius:18px;overflow:hidden;min-height:330px"><video id="gmRemote" autoplay playsinline style="width:100%;height:330px;object-fit:cover;background:#000"></video><video id="gmLocal" autoplay muted playsinline style="position:absolute;right:12px;bottom:12px;width:110px;height:150px;object-fit:cover;border-radius:14px;border:2px solid #fff;background:#222"></video></div><div class="center" style="padding:12px"><b>${window.esc?window.esc(name):name}</b><div id="gmCallState" class="sub">Connecting…</div><button class="callNow" onclick="GMBackend.endVideoCall('${callId}')">✕</button></div></div></div>`;
  }
  function mountCall(callId,name){
    document.getElementById('gmCall')?.remove();
    document.body.insertAdjacentHTML('beforeend',callMarkup(callId,name));
  }
  async function getMedia(){
    if(window.Android){try{Android.requestCamera();Android.requestMicrophone()}catch(e){}}
    return navigator.mediaDevices.getUserMedia({video:true,audio:true});
  }
  function rtcPc(callId,initiator){
    const pc=new RTCPeerConnection({iceServers:[{urls:'stun:stun.l.google.com:19302'}]});
    rtc.calls[callId]={pc,stream:null,initiator};
    pc.ontrack=e=>{const v=document.getElementById('gmRemote');if(v&&e.streams[0])v.srcObject=e.streams[0]};
    pc.onicecandidate=e=>{if(e.candidate){const bucket=initiator?'callerCandidates':'calleeCandidates';state.db.ref('calls/'+callId+'/'+bucket).push(e.candidate.toJSON())}};
    pc.onconnectionstatechange=()=>{const e=document.getElementById('gmCallState');if(e)e.textContent=pc.connectionState==='connected'?'Connected':pc.connectionState==='failed'?'Connection failed':pc.connectionState};
    return pc;
  }
  async function startVideoCall(chat){
    if(!state.enabled){toast('Connect Firebase first to make internet video calls.');return}
    if(!window.RTCPeerConnection||!navigator.mediaDevices?.getUserMedia){toast('This phone WebView does not support WebRTC video calling.');return}
    const own=window.profile&&window.profile.phone||'', callee=chat&&chat.phone||''; if(!callee)return toast('Contact has no phone number');
    const ref=state.db.ref('calls').push(), callId=ref.key; mountCall(callId,chat.name||callee);
    const pc=rtcPc(callId,true); const stream=await getMedia(); rtc.calls[callId].stream=stream;
    document.getElementById('gmLocal').srcObject=stream; stream.getTracks().forEach(t=>pc.addTrack(t,stream));
    const offer=await pc.createOffer(); await pc.setLocalDescription(offer);
    await ref.set({callerPhone:own,calleePhone:callee,callerUid:state.user.uid,offer:{type:offer.type,sdp:offer.sdp},status:'ringing',createdAt:firebase.database.ServerValue.TIMESTAMP});
    ref.child('answer').on('value',async snap=>{const a=snap.val();if(a&&!pc.currentRemoteDescription){await pc.setRemoteDescription(a)}});
    ref.child('calleeCandidates').on('child_added',snap=>{const c=snap.val();if(c)pc.addIceCandidate(c).catch(()=>{})});
    setTimeout(()=>{if(rtc.calls[callId])state.db.ref('calls/'+callId).once('value').then(s=>{if(!s.exists())endVideoCall(callId)})},120000);
  }
  async function acceptVideoCall(callId,data){
    if(!state.enabled)return;
    const pc=rtcPc(callId,false); mountCall(callId,data.callerPhone||'G Messenger user');
    const stream=await getMedia();rtc.calls[callId].stream=stream;document.getElementById('gmLocal').srcObject=stream;stream.getTracks().forEach(t=>pc.addTrack(t,stream));
    await pc.setRemoteDescription(data.offer); const answer=await pc.createAnswer(); await pc.setLocalDescription(answer);
    await state.db.ref('calls/'+callId+'/answer').set({type:answer.type,sdp:answer.sdp}); await state.db.ref('calls/'+callId+'/status').set('accepted');
    state.db.ref('calls/'+callId+'/callerCandidates').on('child_added',snap=>{const c=snap.val();if(c)pc.addIceCandidate(c).catch(()=>{})});
  }
  function endVideoCall(callId){
    const x=rtc.calls[callId];if(x){x.stream?.getTracks().forEach(t=>t.stop());x.pc?.close();delete rtc.calls[callId]}
    document.getElementById('gmCall')?.remove();if(state.enabled)state.db.ref('calls/'+callId+'/status').set('ended').catch(()=>{});
  }
  function listenIncomingCalls(){
    if(!state.enabled||rtc.incomingStarted||!window.profile?.phone)return;rtc.incomingStarted=true;
    const q=state.db.ref('calls').orderByChild('calleePhone').equalTo(window.profile.phone);
    q.on('child_added',snap=>{const d=snap.val();if(!d||d.status!=='ringing'||rtc.calls[snap.key])return;const id=snap.key;
      modal(`<h2>Incoming video call <button class="x" onclick="GMBackend.endVideoCall('${id}')">✕</button></h2><div class="center" style="padding:22px"><div class="av">G</div><h2>${window.esc?window.esc(d.callerPhone):d.callerPhone}</h2><p class="sub">G Messenger video call</p><button class="btn primary" onclick="GMBackend.acceptVideoCall('${id}')">Accept</button><button class="btn danger" onclick="GMBackend.endVideoCall('${id}')">Decline</button></div>`);
      window.GMBackend._pendingCall={id,data:d};
    });
  }
  window.addEventListener('load',()=>setTimeout(listenIncomingCalls,1200));

  window.GMBackend={init,status,syncProfile,findUser,sendMessage,watchChat,watchCurrent,startVideoCall,acceptVideoCall:function(id){const p=window.GMBackend._pendingCall;if(!p||p.id!==id)return;document.getElementById('modal')?.remove();acceptVideoCall(id,p.data).catch(e=>toast('Could not start video call: '+e.message));},endVideoCall,enabled:()=>state.enabled,_pendingCall:null};
  window.addEventListener('load',()=>{patch();setTimeout(init,150);});
})();
