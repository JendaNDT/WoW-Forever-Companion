#!/usr/bin/env python3
"""E11: local adb evidence on an explicitly selected Android device; never infer phone timings from host tests."""
import argparse,csv,json,math,re,subprocess,time
from pathlib import Path

PACKAGE='cz.jenda.wowforever.guide'

def frame_stats(text):
    rows=[];header=None
    for line in text.splitlines():
        fields=next(csv.reader([line]))
        if 'IntendedVsync' in fields and 'FrameCompleted' in fields:header=fields;continue
        if not header or len(fields)!=len(header):continue
        try:
            r=dict(zip(header,fields));start=int(r['IntendedVsync']);end=int(r['FrameCompleted'])
            if int(r.get('Flags','0'))!=0 or not 0<start<=end<9_000_000_000_000_000_000:continue
            rows.append((end-start)/1_000_000)
        except (ValueError,KeyError):continue
    values=sorted(rows)
    if not values:return {'usable_frames':0,'p95_ms':None,'goal_60fps_verified':False}
    def p(q):return values[min(len(values)-1,math.ceil(len(values)*q)-1)]
    return {'usable_frames':len(values),'p50_ms':p(.5),'p95_ms':p(.95),'p99_ms':p(.99),'max_ms':values[-1],
            'over_16_67ms':sum(v>1000/60 for v in values),'goal_60fps_verified':False,
            'meaning':'FrameCompleted minus IntendedVsync; inspect gfxinfo summary and Perfetto deadlines, not an FPS estimate'}

def memory_pss(text):
    match=re.search(r'TOTAL PSS:\s*(\d+)',text) or re.search(r'^\s*TOTAL\s+(\d+)',text,re.M)
    return int(match.group(1)) if match else None

def main():
    a=argparse.ArgumentParser(description=__doc__);a.add_argument('--adb',default='adb');a.add_argument('--serial');a.add_argument('--output',type=Path,required=True)
    a.add_argument('phase',choices=['preflight','startup','map-session']);a.add_argument('--seconds',type=int,default=1200);args=a.parse_args()
    out=args.output.resolve();out.mkdir(parents=True,exist_ok=True)
    def run(*command,timeout=30):
        p=subprocess.run([args.adb]+(['-s',args.serial] if args.serial else [])+list(command),capture_output=True,text=True,timeout=timeout)
        if p.returncode:raise RuntimeError(p.stderr.strip()[:600] or p.stdout.strip()[:600])
        return p.stdout
    def write(name,value):(out/name).write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n')
    inventory=run('devices','-l');devices=[line.split()[0] for line in inventory.splitlines()[1:] if len(line.split())>=2 and line.split()[1]=='device']
    report={'phase':args.phase,'devices':devices,'selected_serial':args.serial,'device_measurement_completed':False}
    if not args.serial or args.serial not in devices:
        report['status']='pending_no_selected_adb_device';write('device-status.json',report);print(json.dumps(report));return 2
    report.update(model=run('shell','getprop','ro.product.model').strip(),android=run('shell','getprop','ro.build.version.release').strip(),fingerprint=run('shell','getprop','ro.build.fingerprint').strip())
    package=run('shell','dumpsys','package',PACKAGE);(out/'package.txt').write_text(package)
    report['debuggable']=bool(re.search(r'(?:pkgFlags|flags)=\[[^\]]*\bDEBUGGABLE\b',package,re.S))
    report['profileable_by_shell']=bool(re.search(r'PROFILEABLE_BY_SHELL|profileableByShell\s*[:=]\s*true',package,re.I))
    report['status']='preflight_ready' if report['profileable_by_shell'] and not report['debuggable'] else 'requires_profileable_non_debug_build'
    write('device-status.json',report)
    if args.phase=='preflight':print(json.dumps(report));return 0 if report['status']=='preflight_ready' else 2
    if report['status']!='preflight_ready':raise RuntimeError('Pro měření nainstaluj podepsané release APK E11, nikoli debug sestavení.')
    def app_logs():return '\n'.join(line for line in run('logcat','-d','-v','monotonic','-s','ActivityTaskManager:I','ActivityManager:I').splitlines() if PACKAGE in line)
    if args.phase=='startup':
        samples=[]
        for n in range(5):
            before=set(app_logs().splitlines());run('shell','am','force-stop',PACKAGE)
            launch=run('shell','am','start','-W','-n',PACKAGE+'/cz.jenda.wowforever.MainActivity')
            (out/f'startup-{n+1}.txt').write_text(launch)
            deadline=time.monotonic()+30;fresh=[]
            while time.monotonic()<deadline:
                fresh=[line for line in app_logs().splitlines() if line not in before]
                if any('Fully drawn' in line for line in fresh):break
                time.sleep(.25)
            (out/f'startup-{n+1}-app-log.txt').write_text('\n'.join(fresh)+'\n')
            samples.append({'am_start':dict(re.findall(r'^(ThisTime|TotalTime|WaitTime):\s*(\d+)',launch,re.M)),
                            'fully_drawn_app_log':[line for line in fresh if 'Fully drawn' in line]})
            print(f'Spuštění {n+1}/5 zaznamenané.',flush=True)
        write('startup.json',{'samples':samples,'report_drawn_meaning':'catalog and UI ready; not proof that all visible terrain tiles are drawn',
                              'three_second_phone_goal_verified':False,'requires_review':True})
        report.update(status='startup_captured_review_required',device_measurement_completed=True)
    else:
        if not 30<=args.seconds<=3600:raise ValueError('--seconds musí být 30 až 3600; cílový paměťový scénář je 1200 sekund.')
        if not run('shell','pidof',PACKAGE).strip():raise RuntimeError('Nejdřív otevři mapu a zavři dialogy.')
        run('shell','dumpsys','gfxinfo',PACKAGE,'reset')
        remote='/data/misc/perfetto-traces/forever-e11-'+str(time.time_ns())+'.pftrace'
        trace_started=False
        try:
            run('shell','perfetto','--background-wait','-o',remote,'-t','30s','--app',PACKAGE,'sched','freq','idle','am','wm','gfx','view','binder_driver','dalvik');trace_started=True
            print('Nyní opakovaně přepínej oblasti, posouvej mapu a přibližuj. Měření skončí samo.',flush=True)
            start=time.monotonic();samples=[];next_sample=0
            while True:
                elapsed=time.monotonic()-start
                if not run('shell','pidof',PACKAGE).strip():raise RuntimeError('Proces aplikace během scénáře skončil; zachovej artefakty a prověř logcat.')
                mem=run('shell','dumpsys','meminfo',PACKAGE);(out/f'meminfo-{len(samples):03}.txt').write_text(mem)
                samples.append({'seconds':round(elapsed,2),'total_pss_kib':memory_pss(mem)})
                print(f'Paměť: {elapsed:.0f} s, PSS {samples[-1]["total_pss_kib"]} KiB.',flush=True)
                if elapsed>=args.seconds:break
                next_sample+=min(60,args.seconds);time.sleep(max(0,min(args.seconds,next_sample)-(time.monotonic()-start)))
            raw=run('shell','dumpsys','gfxinfo',PACKAGE,'framestats');(out/'gfxinfo-framestats.txt').write_text(raw)
            (out/'gfxinfo.txt').write_text(run('shell','dumpsys','gfxinfo',PACKAGE))
            write('map-session.json',{'duration_seconds':samples[-1]['seconds'],'memory_samples':samples,'frame_stats':frame_stats(raw),
                                       'memory_growth_review_required':True,'twenty_minute_memory_goal_verified':False})
            report.update(status='map_session_captured_review_required',device_measurement_completed=True)
        finally:
            # Never pull an incomplete trace; allow Perfetto to finish its bounded 30-second recording.
            if trace_started:
                remaining=30-(time.monotonic()-start) if 'start' in locals() else 30
                if remaining>0:time.sleep(min(30,remaining)+1)
                last='';stable=0
                for _ in range(10):
                    size=run('shell','stat','-c','%s',remote).strip()
                    if size==last and size.isdigit() and int(size)>0:stable+=1
                    else:stable=0
                    if stable>=2:break
                    last=size;time.sleep(1)
                if stable<2:raise RuntimeError('Trace není dokončená; soubor zůstává na zařízení: '+remote)
                run('pull',remote,str(out/'map-session.pftrace'),timeout=60)
    write('device-status.json',report);print(json.dumps(report));return 0

if __name__=='__main__':raise SystemExit(main())
