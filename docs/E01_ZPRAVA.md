# E01 — nativní projekt a technické APK

**Datum: 23. října 2026. Stav: implementováno a ověřeno hostitelskými testy; zkouška na zařízení čeká.**

Vznikl skutečný nativní Android projekt a instalovatelný soubor `WoW_Forever_E01.apk`, verze **0.1.0-e01**, ID `cz.jenda.wowforever`. Velikost APK je 18 333 695 bajtů, přibližně 17,5 MiB. Minimum je nastavené na Android 8 / API 26; compile a target SDK jsou 36. Podpis vývojového APK úspěšně ověřil nástroj apksigner.

## Výsledek implementace

- Čtyři skutečné mapové podklady: Azeroth, Kalimdor, Durotar a Orgrimmar; kompletně přibalené offline.
- Nativní vykreslení pomocí **MapCompose 3.2.0**, oddělené od obrazovky a dat rozhraním `MapEngine`.
- **68 PNG dlaždic**, velikost 256 × 256, tři úrovně, dva dekódovací pracovníci. Nejvyšší úroveň zachovává původní mapu pixel po pixelu.
- Osm původních beta NPC z E00, bez přidání neověřené Classic polohy Nartoka. Blízké body se seskupují; samostatný seznam umožňuje vybrat konkrétní NPC.
- Detail NPC, souřadnice, upozornění na beta data a odkaz na původní zdroj.
- Posun, přiblížení, rotace, sever nahoru a návrat na celou mapu. První otevření oblasti ukáže celý podklad.
- Uchování mapy, bodu, středu, přiblížení a natočení přes ViewModel a SavedStateHandle.
- Úzké a široké rozložení. V krátkém okně se ovládání mapy přesune do řádku, aby všechna tlačítka zůstala dostupná.
- Lokální Git historie, Gradle Wrapper s kontrolním součtem, přesně určené verze nástrojů a připravený workflow GitHub Actions.

Vzdálený repozitář nebyl vytvořen a workflow na GitHubu nebyl spuštěn. Historie je součástí zdrojového archivu v `history.bundle`. Vývojový podpis není podpisem budoucí distribuční verze.

## Skutečně provedené kontroly

| Kontrola | Výsledek |
|---|---|
| Čisté sestavení včetně `clean`, aplikace a instrumentačního APK | **Prošlo**, 88 úloh provedeno |
| Hostitelské testy JUnit / Robolectric, nativní grafický režim API 28 | **10 z 10 prošlo** |
| Android Lint včetně testů | **0 chyb, 0 varování** |
| Matematika souřadnic, rozsahy a zákaz záměny area/UI map ID | Prošlo, 3 testy |
| Dekódování 68 dlaždic Android dekodérem a kontrola aktivního katalogu | Prošlo, 2 testy |
| Vykreslení osmi NPC: čtyři barevné kontrolní vzorky kolem každého očekávaného středu | Prošlo v hostitelském nativním rendereru |
| 500 syntetických bodů, 15 cyklů přesunu, přiblížení, severu a návratu | Prošlo v hostitelském prostředí |
| Přepínání map, seznam a detail NPC, vybraný bod uvnitř krátkého okna | Prošlo |
| Výběr a přiblížení po znovuvytvoření Activity | Prošlo |
| Široké rozložení 800 × 400 dp včetně všech ovládacích prvků | Prošlo |
| Rekonstrukce čtyř obrázků z nejvyšší úrovně dlaždic | Prošlo, nulový pixelový rozdíl proti E00 |
| Integrita původních dat E00 | Prošla, 4 kontrolní součty map a 25 záznamů |
| Podpis APK a metadata balíčku | Prošlo, APK Signature Scheme v2, ARM 32/64 i x86 32/64 |

Přesné protokoly, JUnit XML a skutečné snímky Android obrazovky jsou v `docs/qa`. Snímky byly vizuálně zkontrolovány. Jde o výstup nativního vykreslení aplikace v Robolectric, nikoli snímky běhu na telefonu. Pro grafické testy je použit skutečný rasterový Canvas; hostitelský PixelCopy nebyl použit.

**Tyto kontroly nejsou měřením výkonu telefonu.** Neudávají FPS ani spotřebu na reálném zařízení. Kompatibilitu starších API kontroluje nastavení SDK a Lint; běh na skutečném Androidu 8 zatím ověřen není.

## Neprovedené a otevřené kontroly

Pokus o emulátor API 30 x86_64 se softwarovým CPU a SwiftShader nedokončil start do 420 sekund. Hardwarová akcelerace nebyla dostupná. Instalace a instrumentační testy v emulátoru proto **neproběhly**. Jejich zdrojový kód a testovací APK byly sestavené, ale sestavení samo neznamená úspěšný běh. Kontrolní seznam pro telefon je v `KONTROLA_NA_TELEFONU.md`.

Souřadnice pocházejí z publikovaných beta pozorování. Technické umístění osmi značek na obrázek je ověřené, ale **nezávislých herních kontrol je stále nula**. Není tím potvrzena kalibrace vůči současnému hernímu klientu, přesnost pozice NPC po vydání ani dostupnost úkolů pro Orka Warlocka. Rozlišení podkladu je 1002 × 668 a přiblížení další detaily nepřidá.

Ve vzorku zůstávají zachované nejistoty E00, včetně různých úkolů Vile Familiars a sporného počtu předmětů. E01 je neproměňuje v doporučení. Grafické podklady zůstávají určené pro osobní technický prototyp s uvedením Blizzard Entertainment a původních zdrojů.

## Rozhodnutí a návaznost

**Pro další implementaci pokračujeme s MapCompose.** Hostitelské testy neodhalily důvod stavět vlastní renderer. Typ souřadnic a adapter zůstávají oddělené, takže komponentu lze později vyměnit podle výsledku zkoušky na telefonu. Technická volba a omezení jsou popsány v `ROZHODNUTI_MAPA.md`.

Další etapa E02 připraví Room databázi a validovaný model herního obsahu a uživatelského postupu. E01 má zatím pevný profil Horda / Ork / Warlock / úroveň 1. Neobsahuje kompletní questový průvodce, osobní postup, poznámky, zálohy ani slib úplnosti obsahu.

## Otisk předaného APK

SHA-256: `83d976846915fe62b508c55e1e49466a4e75caa221595e36b2b159354f4e7ce5`

Otisk se vztahuje k samostatnému souboru `WoW_Forever_E01.apk`. U znovu sestaveného APK se může lišit podpis a časová metadata; zdrojový archiv obsahuje použitý kód a vstupní dlaždice, nikoliv soukromý vývojový podpisový klíč.
