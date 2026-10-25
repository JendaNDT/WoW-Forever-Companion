"""Transport connections are directed, evidenced records, never straight-line guesses."""
from validate_guide_rules import validate_guide_rules

def validate_transport(pack):
    graph=pack.get('transportGraph',{});nodes=graph.get('nodes',[]);edges=graph.get('edges',[])
    assert len(nodes)<=1000 and len(edges)<=1000
    by_key={n['key']:n for n in nodes};locations={l['key']:l for l in pack['locations']}
    entries={e['key']:e for e in pack['entries']};sources={s['id']:s for s in pack['sources']}
    assert len(by_key)==len(nodes) and len({n['locationKey'] for n in nodes})==len(nodes)
    assert len({e['key'] for e in edges})==len(edges) and len({e['entryKey'] for e in edges})==len(edges)
    def evidence(ids):return bool(ids) and len(ids)==len(set(ids)) and all(s in sources for s in ids)
    def release(ids):return any(sources[s]['kind']=='in_game_release_observation' for s in ids)
    for n in nodes:
        assert n['nameCs'].strip() and n['locationKey'] in locations and evidence(n['sourceIds'])
        if n.get('discoveryKey'):assert entries[n['discoveryKey']]['kind']=='TRAVEL'
    for e in edges:
        assert e['from'] in by_key and e['to'] in by_key and e['from']!=e['to'] and entries[e['entryKey']]['kind']=='TRAVEL' and evidence(e['sourceIds'])
        a=by_key[e['from']];b=by_key[e['to']];pa=locations[a['locationKey']];pb=locations[b['locationKey']]
        if e['mode']=='FLIGHT':assert a.get('discoveryKey') and b.get('discoveryKey')
        if e['mode']=='WALK':assert pa['mapKey']==pb['mapKey'] and pa['floorId']==pb['floorId']
        if e['verification']=='RELEASE':
            assert release(e['sourceIds'])
            for n in [a,b]:
                l=locations[n['locationKey']];entry=entries[l['entryKey']]
                assert entry['releaseConfirmed'] and entry['verification']=='RELEASE' and release(n['sourceIds']) and release([l['sourceId']])
                if n.get('discoveryKey'):assert entries[n['discoveryKey']]['releaseConfirmed'] and entries[n['discoveryKey']]['verification']=='RELEASE'
    rules=[{'key':f'wow-forever:rule:transport:{i}','entryKey':e['entryKey'],'condition':e['condition'],'importance':'TRAVEL','reasonCs':'Podmínky dopravního úseku.','verification':e['verification'],'sourceIds':e['sourceIds']} for i,e in enumerate(edges)]
    validate_guide_rules({**pack,'guideRules':rules})
