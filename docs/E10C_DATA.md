# E10.3 — sever Durotaru a orčí Warlock

25. 10. 2026 · aplikace 0.14.0-e10.3, kód 14 · přibalená data 6.

## Rozsah

Nových 34 položek: 16 úkolů, deset NPC s polohou a osm předmětů. Celkem **216 položek, 84 přímých poloh, pět map / 85 PNG**, 188 vztahů, 216 zdrojů a 91 doporučovacích pravidel. Všech 182 starších položek a jejich zdroje, vztahy, pravidla a souřadnice zůstávají strukturálně stejné. Mění se jen vysvětlující scopeCs Durotaru; mapové rámce, obrázky a verze souřadnic zůstávají stejné.

Dávka pokrývá vybraný sever Durotaru, Razorwind Canyon, Thunder Ridge, Margozův tábor, okolí Skull Rock, návaznosti do Orgrimmaru a orčí cestu k Voidwalkerovi. Úplný Durotar, celý svět ani cesta 1–60 nejsou dokončené. Ověřené dopravní hrany: nula. RELEASE položky: nula.

## Nové úkoly

- Carry Your Weight 791 a šestímístná beta brašna Handmade Leather Bag 11845.
- Need for a Cure 812, Finding the Antidote 813, Lost But Not Forgotten 816.
- Winds in the Desert 834 a Securing the Lines 835.
- Dark Storms 806 → Margoz 828 → Skull Rock 827 → Neeru Fireblade 829 → dřívější Ak’Zeloth 809. Předchozí Report to Orgnil 823 je zachovaný.
- Samostatný nález Eye of Burning Shadow pro Burning Shadows 832; nedostává vymyšleného NPC zadavatele ani návaznost na dokončení obojků.
- Gan’rul’s Summons 1506 → Creature of the Void 1501 → The Binding 1504. Orčí Classic reference pro Warlocka od úrovně 10: Ophek, Gan’rul, Tablet of Verga, Glyphs of Summoning, kruh v Neeruově stanu. Nezaměňuje se se stejnojmennými úkoly a runami v Undercity.
- Dva nové Forever záznamy A Simmering Storm 97281 → Stormy Potential 97282. První začíná předmětem Dull Storm Orb; text cíle píše Dull Stormy Orb, proto jsou oba názvy v hledání.

Čtrnáct úkolů je CLASSIC. Nová dvojice kolem Rezlaka je BETA, zdroj ji výslovně převzal z databáze Forever; neprezentuje se jako přímé vlastní pozorování nebo potvrzení vydané hry. minimumLevel všech nových úkolů zůstává null; referenční minimum a úroveň úkolu jsou zvlášť v payloadu. Třídní doporučení mají orientační podmínku úrovně 10 a vždy neznámé úplné podmínky.

Classic Need for a Cure uvádí 45 minut. Tato hodnota je v popisu výslovně Classic, bez aktivního časovače, nevratného varování nebo tvrzení o současném limitu Forever. Způsob získání kořisti ani procenta dropu se z Classicu neprohlašují za beta fakta.

## Polohy a předměty

Čtyři nové body pocházejí z vlastního beta loggeru webu Warcraft Forever: Fizzle 42.1/26.8, Margoz 56.4/19.7, Gazz’uz 54.8/9.4 a Kor’ghan v Orgrimmaru 46.2/53.0. Šest NPC má explicitně CLASSIC polohu: Rezlak, Misha Tor’kren, Rhinag, Ophek, Gan’rul Bloodeye a Furl Scornbrow. U každé polohy je zdroj. Classic body mají značku C, příponu Classic a stav v detailu. Lze je otevřít z katalogu i z výpravy; výprava ukáže upozornění a nespočítá jejich spojení jako ověřené. Kompaktní detail je neoznačuje jako beta. Beta poloha jednoho NPC nepotvrzuje Classic quest, který ho zmiňuje. Gazz’uzův bod není vstup do jeskyně ani mapa jejího vnitřku.

Osm vlastních tooltipových zdrojů ForeverChanges: Searing Collar 4871, Example Collar **6658**, Eye of Burning Shadow 4903, Tablet of Verga 6535, Glyphs of Summoning 7464, Blemished Wooden Staff 4938, Makeshift Stormcaller 281274 a Handmade Leather Bag **11845**. Build tooltipů: 1.60.1.70532. Obsah předmětu a způsob jeho získání mají oddělený původ.

Makeshift Stormcaller uvádí +2 Intellect, +4 Spell Power a item level 11, který není požadovanou úrovní postavy. Brašna má beta šest míst oproti čtyřem v Classicu. Blemished Wooden Staff je volitelná Classic odměna 835 a má vztah REWARD_CHOICE. Pevná brašna a Stormcaller mají vazbu popsánu v textu/payloadu, **nejsou falešně označené jako výběr odměny**. Schéma proto zůstává stejné. Beta tooltip sám nepotvrzuje dostupnost odměny; každé nové pravidlo obsahuje unknown a příslušný BETA/CLASSIC původ. Nenastává potvrzené odemčení ani varování před nenávratnou ztrátou.

## Jedno APK a přechod z E13

APK obsahuje podepsaný úplný archiv assets/content/bundled-data-6.wfp se všemi 85 PNG. Produkční GuideApplication předává právě tento archiv. Původní nouzový základ 2 zůstává, data 5 jsou v historických zdrojích a testovacích distribučních podkladech. Starý archiv 5 se zbytečně nepřibaluje podruhé do APK.

Dosavadní E13 připravila a potvrdila manifest 5. Nový manifest 6 má jiné ID, proto se automaticky připraví i při existujícím bundled-receipt 5. Vytvoří se oddělená ověřená databáze; přepnutí je atomické. Předchozí 5 zůstane pro návrat. Záměrný pozdější návrat přetrvá po restartu, obnovení přibaleného obsahu vybere 6, novější importovaný obsah se automaticky nesnižuje. Osobní databáze, JSON záloha 1, schémata Room i vlastnický podpis E09+ zůstávají stejné. Žádný ruční import ani internet není pro přípravu potřeba. Manifest dat 6 vyžaduje aplikaci kódu alespoň 14: starší vydání neuměla Classic body zobrazit s původem. Manifest i katalog tuto podmínku kontrolují.

## Reprodukce a kontrola

Kurátorská fakta: research/e10c/facts.json. Audit vybraných faktů a jejich hashů: facts-audit.json, seznam zdrojů: SOURCES.md. Úplné cizí texty ani HTML se do projektu nekopírují.

```sh
python3 tools/build_e10c_content.py
python3 tools/verify_e10c_data.py
python3 tools/build_package.py --catalog content/e10-v6.json --output app/src/main/assets/content/bundled-data-6.wfp --manifest-prefix content/e10-v6.manifest --signing-key /soukroma/cesta/content-publisher.pem
python3 tools/verify_bundled.py
python3 -m unittest discover -s tools -p 'test_*.py' -v
./gradlew --no-daemon --max-workers=1 clean :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug :app:lintRelease
```

Při novém vytvoření ZIP se může změnit jeho časový obal a SHA. Uvolněné bajty jsou svázané s konkrétním APK, podepsaným manifestem, receipt a ověřovačem; pro pouhou reprodukci dat není nutné ZIP znovu podepisovat. Soukromé klíče nejsou součástí zdrojů. Finální výsledky jsou v E10C_ZPRAVA.md a qa/e10c.
