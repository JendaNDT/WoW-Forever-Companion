"""Read pinned Lua literal tables as data. Never execute upstream Lua code."""
import re
from pathlib import Path
TOKEN=re.compile(r'''\s+|--[^\n]*|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|-?\d+(?:\.\d+)?|[A-Za-z_][A-Za-z0-9_.]*|[{}\[\]=,;+()\-]''')
class TableReader:
 def __init__(self,text,env=None):
  self.env=env or {};self.unknown=set();self.t=[];end=0
  for m in TOKEN.finditer(text):
   assert not text[end:m.start()].strip(),repr(text[end:m.start()]);end=m.end();v=m.group()
   if not v.isspace() and not v.startswith('--'):self.t.append(v)
  assert not text[end:].strip();self.i=0
 def atom(self):
  t=self.t[self.i];self.i+=1
  if t=='-':return -self.atom()
  if t=='{':
   d={};index=1
   while self.t[self.i]!='}':
    if self.t[self.i]=='[':
     self.i+=1;k=self.value();assert self.t[self.i]==']';self.i+=1;assert self.t[self.i]=='=';self.i+=1;d[k]=self.value()
    elif self.i+1<len(self.t) and self.t[self.i+1]=='=':
     k=self.t[self.i];self.i+=2;d[k]=self.value()
    else:d[index]=self.value();index+=1
    if self.t[self.i] in ',;':self.i+=1
   self.i+=1;return d
  if t[0] in '\"\'':
   def esc(m):
    x=m.group()[1:];return chr(int(x)) if x.isdigit() else {'n':'\n','r':'\r','t':'\t','a':'\a','b':'\b','f':'\f','v':'\v'}.get(x,x)
   return re.sub(r'\\(?:\d{1,3}|.)',esc,t[1:-1])
  if t=='nil':return None
  if t in ('true','false'):return t=='true'
  if re.fullmatch(r'-?\d+(?:\.\d+)?',t):return float(t) if '.' in t else int(t)
  if t=='l10n':
   assert self.t[self.i]=='(';self.i+=1;v=self.value();assert self.t[self.i]==')';self.i+=1;return v
  if t.split('.')[0] in ('questKeys','npcKeys','itemKeys','objectKeys'):return t.split('.',1)[1]
  if t in self.env:return self.env[t]
  if not re.fullmatch(r'[A-Za-z_][A-Za-z0-9_.]*',t):raise ValueError('Unsupported literal token: '+t)
  self.unknown.add(t);return '__unresolved:'+t
 def value(self):
  v=self.atom()
  while self.i<len(self.t) and self.t[self.i]=='+':
   self.i+=1;v+=self.atom()
  return v

def strip_block_comments(text):
 out=[];i=0
 while i<len(text):
  if text[i] in '\"\'':
   quote=text[i];start=i;i+=1
   while i<len(text):
    if text[i]=='\\':i+=2;continue
    if text[i]==quote:i+=1;break
    i+=1
   out.append(text[start:i]);continue
  if text.startswith('--',i):
   m=re.match(r'--\[(=*)\[',text[i:])
   if m:
    end=text.find(']'+m[1]+']',i+m.end())
    if end<0:raise ValueError('Unterminated Lua long comment')
    end+=len(m[1])+2;out.append('\n'*text[i:end].count('\n'));i=end;continue
   end=text.find('\n',i)
   if end<0:end=len(text)
   out.append(text[i:end]);i=end;continue
  out.append(text[i]);i+=1
 return ''.join(out)

def balanced_table(text,start):
 depth=0;quote=None;escape=False;comment=False
 for i in range(start,len(text)):
  c=text[i]
  if comment:
   if c=='\n':comment=False
   continue
  if quote:
   if escape:escape=False
   elif c=='\\':escape=True
   elif c==quote:quote=None
   continue
  if c in '\"\'':quote=c;continue
  if text[i:i+2]=='--':comment=True;continue
  if c=='{':depth+=1
  elif c=='}':
   depth-=1
   if depth==0:return text[start:i+1]
 raise ValueError('Unterminated literal table')

def literal_table(text,marker,env=None):
 text=strip_block_comments(text)
 start=text.index('{',text.index(marker));reader=TableReader(balanced_table(text,start),env);result=reader.value();assert reader.i==len(reader.t);return result,reader.unknown

def raw_data(path):
 text=Path(path).read_text();table=literal_table(text,'[[return ')[0]
 keys={k:int(v) for k,v in re.findall(r"\['([A-Za-z]+)'\]\s*=\s*(\d+)",text.split('[[return ')[0])}
 return table,keys

def build_environment(root):
 root=Path(root);env={}
 # Values are read from the pinned enum files; only the standard race bit table is expanded here.
 for f,prefixes in [('zones.lua',['zoneIDs']),('quests.lua',['sortKeys','specialFlags']),('items.lua',['itemClasses']),('professions.lua',['profKeys','specKeys','rankKeys']),('factions.lua',['factionIDs'])]:
  text=(root/'src/corrections/enum'/f).read_text()
  for name,v in re.findall(r'\b([A-Z][A-Z0-9_]*)\s*=\s*(-?\d+)\b',text):
   for prefix in prefixes:env[prefix+'.'+name]=int(v)
 text=(root/'src/corrections/enum/expansions.lua').read_text()
 for name,v in re.findall(r'\b([A-Z][A-Z0-9_]*)\s*=\s*(\d+)\s*[,\n]',text):
  env['classIDs.'+name]=int(v);env['npcFlags.'+name]=int(v)
 race={name:1<<bit for name,bit in [('HUMAN',0),('ORC',1),('DWARF',2),('NIGHT_ELF',3),('UNDEAD',4),('TAUREN',5),('GNOME',6),('TROLL',7),('SKYBORNE_ALLIANCE',32),('SKYBORNE_HORDE',33)]}
 race.update(NONE=0,ALL_ALLIANCE=77+(1<<32),ALL_HORDE=178+(1<<33))
 env.update({'raceIDs.'+k:v for k,v in race.items()})
 env.update({'classIDs.'+k:v for k,v in dict(NONE=0,WARRIOR=1,PALADIN=2,HUNTER=4,ROGUE=8,PRIEST=16,SHAMAN=64,MAGE=128,WARLOCK=256,DRUID=1024,ALL_CLASSES=1503).items()})
 for name in ['EVENT','INTERACT','OBJECT','TALK']:env['Questie.ICON_TYPE_'+name]=0
 return env

def patch_table(base,operand,remove=False,nested=False):
 result=dict(base or {})
 if not operand:return result
 # Entity lists use array semantics; starter/finisher and spawns contain nested lists.
 if nested:
  for k,v in operand.items():
   if v is not None:result[k]=patch_table(result.get(k),v,remove,nested=False)
 else:
  values=[v for _,v in sorted(result.items()) if v is not None]
  for _,v in sorted(operand.items()):
   if v is None:continue
   if remove:values=[x for x in values if x!=v]
   elif v not in values:values.append(v)
  result={i+1:v for i,v in enumerate(values)}
 return result

def effective_data(root, extra_root=None):
 root=Path(root);env=build_environment(root);all_data={};changed={};audit=[]
 for kind,name in [('Quest','Quest'),('Npc','NPC'),('Item','Item'),('Object','Object')]:
  data,keys=raw_data(root/f'data/Forever/forever{kind}DB.lua');modified=set()
  layers=[(root,f'src/corrections/Forever/generated/foreverBase{kind}.lua'),(root,f'src/corrections/Forever/forever{name}Fixes.lua')]
  if extra_root is not None:
   extra=Path(extra_root);legacy=[f'classic{name}Fixes.lua']
   if kind=='Quest':legacy+=['classicQuestReputationFixes.lua']
   if kind=='Item':legacy+=['itemStartFixes.lua']
   layers=[(extra,'src/corrections/Forever/legacy/'+f) for f in legacy]+[layers[0],(extra,f'src/corrections/Forever/traces/forever{kind}Traces.lua'),layers[1]]
  for layer_root,rel in layers:
   text=(layer_root/rel).read_text()
   if extra_root and 'function QuestieObjectFixes:Load()' in text:text=text.split('function QuestieObjectFixes:Load()',1)[1]
   fixes,unknown=literal_table(text,'return {',env)
   allowed={'Questie.ICON_TYPE_NODE_FISH','Questie.ICON_TYPE_SLAY','phases.LORD_TIRION_FORDRING_AT_TOWER'} if extra_root else set()
   assert not unknown-allowed,(rel,unknown)
   for ident,fields in fixes.items():
    row=data.setdefault(ident,{})
    for field,value in fields.items():
     parts=field.rsplit('_',1);operation=parts[-1] if len(parts)>1 and parts[-1] in ('add','remove') else None
     original=parts[0] if operation else field
     if original not in keys:continue
     index=keys[original]
     row[index]=patch_table(row.get(index),value,operation=='remove',nested=original in ('startedBy','finishedBy','spawns')) if operation else value
    modified.add(ident)
   audit.append(dict(file=rel,rows=len(fixes)))
  all_data[kind]=data;changed[kind]=modified
 return all_data,changed,audit
