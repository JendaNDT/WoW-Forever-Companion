# E01: volba mapové komponenty

Pro další etapy je integrovaný **MapCompose 3.2.0**. UI používá vlastní rozhraní `MapEngine`; typ `MapState` nepřechází do datového modelu ani obrazovky aplikace. Výměna rendereru tak nevyžaduje změnu významu souřadnic.

## Parametry prototypu

| Parametr | Volba |
|---|---|
| Souřadnice obsahu | UI map ID a procenta 0–100, počátek vlevo nahoře |
| Souřadnice rendereru | Normalizace 0–1 bez dalších projekcí |
| Durotar | UI map 1411, rozměr 1002 × 668; area ID 14 se nepoužívá k vykreslení |
| Dlaždice | 256 × 256, bezeztrátové PNG s průhledným okrajem |
| Pyramida | 3 úrovně, nejvyšší má původní rozlišení |
| Zaokrouhlení | Zdroj je nejdříve doplněn na 1024 × 1024 a teprve potom zmenšen |
| Načítání | 2 pracovní vlákna, bez přednačítání okolí navíc |
| Přiblížení | Maximum 8× původních obrazových bodů, minimum podle velikosti okna |
| Rotace | Nativní gesto dvou prstů, ovládací tlačítko návratu na sever |
| Blízké body | Seskupování při vzdálenosti do 36 dp; seznam zajišťuje přístup k jednotlivým NPC |
| Uchování pohledu | ViewModel a SavedStateHandle, procentuální střed, měřítko a úhel |

Zpracování okrajů je podstatné: zmenšená šířka 1002/4 je **250,5**, nikoliv 251 pixelů. Předčasné zaokrouhlení rozměru obrazu by nepatrně změnilo souřadnicový rámec. Nejvyšší úroveň je pixelově totožná se vstupem; kontroluje to `tools/verify_assets.py`.

Čtyři uchovávané mapové stavy jsou omezené tímto pevným katalogem. Tento prototyp není měřením kapacity pro celý svět. Jedna dekódovaná dlaždice RGBA má přibližně 256 KiB; dvě pracovní vlákna omezují souběžné dekódování. Celkový limit paměti a chování při stovkách map bude součástí dalších etap a měření na telefonu.

## Rozsah důkazu

Technická kontrola testuje umístění značek vůči publikovaným souřadnicím na obrázku. Neověřuje, zda je NPC na dané pozici v aktuální Forever. Herní kalibrace z E00 proto zůstává oddělená a otevřená.

Sdílené testy `NativeMapChecks` obsahují vykreslení osmi bodů a manipulaci s 500 syntetickými body v 15 cyklech. Lze je spustit na hostiteli přes Robolectric i jako Android instrumentační testy; skutečné výsledky jsou v `E01_ZPRAVA.md`. Syntetické body se nevkládají do uživatelského katalogu.

Záložní vlastní renderer nebyl implementován. Na základě dokončených hostitelských kontrol pokračujeme s MapCompose; běh na skutečném zařízení je nadále otevřenou položkou ověření.

## Technické zdroje

- MapCompose 3.2.0: https://github.com/p-lr/MapCompose/tree/v3.2.0
- Publikované závislosti: https://repo.maven.apache.org/maven2/ovh/plrapps/mapcompose/3.2.0/mapcompose-3.2.0.pom
- Kompatibilita AGP 8.13.2 / Gradle 8.13 / JDK 17: https://developer.android.com/build/releases/agp-8-13-0-release-notes
- Zdroje herních dat a otisky map: `research/e00`.
