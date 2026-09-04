const {onValueCreated}=require('firebase-functions/v2/database');
const {initializeApp}=require('firebase-admin/app');
const {getDatabase}=require('firebase-admin/database');
const {getMessaging}=require('firebase-admin/messaging');
const {getAuth}=require('firebase-admin/auth');
const {onRequest}=require('firebase-functions/v2/https');
initializeApp();
const db=()=>getDatabase();

const ADMIN_PHONES=new Set(['+2349033229734','+2347067315898']);
function cors(res){res.set('Access-Control-Allow-Origin','*');res.set('Access-Control-Allow-Headers','Content-Type,Authorization');res.set('Access-Control-Allow-Methods','POST,OPTIONS');}
exports.exchangeIdToken=onRequest(async(req,res)=>{
  cors(res); if(req.method==='OPTIONS')return res.status(204).send('');
  if(req.method!=='POST')return res.status(405).json({error:'POST required'});
  try{
    const idToken=String(req.body?.idToken||''); if(!idToken)return res.status(400).json({error:'Missing ID token'});
    const decoded=await getAuth().verifyIdToken(idToken,true);
    if(decoded.firebase?.sign_in_provider!=='phone' || !decoded.phone_number) return res.status(403).json({error:'A verified phone authentication is required'});
    const user=await getAuth().getUser(decoded.uid);
    const admin=ADMIN_PHONES.has(decoded.phone_number);
    const claims={...(user.customClaims||{}),phone_verified:true};
    if(admin) claims.admin=true; else delete claims.admin;
    await getAuth().setCustomUserClaims(decoded.uid,claims);
    const customToken=await getAuth().createCustomToken(decoded.uid,claims);
    return res.json({customToken,admin});
  }catch(e){return res.status(401).json({error:'Invalid Firebase ID token'});}
});
exports.ensureAdminClaim=onRequest(async(req,res)=>{
  cors(res); if(req.method==='OPTIONS')return res.status(204).send('');
  if(req.method!=='POST')return res.status(405).json({error:'POST required'});
  try{
    const header=String(req.get('Authorization')||''); if(!header.startsWith('Bearer '))return res.status(401).json({error:'Authentication required'});
    const decoded=await getAuth().verifyIdToken(header.slice(7),true);
    const user=await getAuth().getUser(decoded.uid);
    if(!user.phoneNumber||!ADMIN_PHONES.has(user.phoneNumber))return res.status(403).json({error:'Administrator phone number not authorized'});
    await getAuth().setCustomUserClaims(decoded.uid,{...(user.customClaims||{}),admin:true});
    return res.json({admin:true});
  }catch(e){return res.status(401).json({error:'Administrator authorization failed'});}
});

async function sendToUids(uids,title,body){
  const tokens=[];
  for(const uid of [...new Set(uids.filter(Boolean))]){
    const snap=await db().ref(`notificationTokens/${uid}`).once('value');
    snap.forEach(x=>{const t=x.val()?.token;if(t)tokens.push(t)});
  }
  if(!tokens.length)return;
  const res=await getMessaging().sendEachForMulticast({tokens,data:{title:String(title||'G Messenger'),body:String(body||'New message')},android:{priority:'high'}});
  const bad=[];res.responses.forEach((r,i)=>{if(!r.success && ['messaging/registration-token-not-registered','messaging/invalid-registration-token'].includes(r.error?.code))bad.push(tokens[i])});
  for(const t of bad)for(const uid of [...new Set(uids.filter(Boolean))])await db().ref(`notificationTokens/${uid}/${t.replace(/[^a-zA-Z0-9_-]/g,'_')}`).remove().catch(()=>{});
}
exports.onPrivateMessage=onValueCreated('/messages/{a}/{b}/{message}',async e=>{const m=e.data.val();if(!m)return;await sendToUids([m.recipientUid],m.senderName||m.senderId||'G Messenger',m.text||'New message')});
exports.onGroupMessage=onValueCreated('/groups/{group}/messages/{message}',async e=>{const m=e.data.val();const meta=(await db().ref(`groupMeta/${e.params.group}`).once('value')).val()||{};await sendToUids((meta.members||[]).filter(uid=>uid!==m.senderUid),m.senderName||'G Messenger group',m.text||'New group message')});
exports.onGeneralMessage=onValueCreated('/generalMessages/{message}',async e=>{const m=e.data.val();const snap=await db().ref('users').once('value');const uids=[];snap.forEach(x=>uids.push(x.key));await sendToUids(uids.filter(uid=>uid!==m.senderUid),'G Messenger General',m.text||'New official message')});
