# Přímý vývoj na Lenovu přes USB

Tyto příkazy se spouštějí **na Macu, ke kterému je připojený tablet**. Cloudová relace s vlastním linuxovým terminálem nemá automaticky přístup k místnímu ADB ani USB. Stav `device` v Macovém terminálu je správně; ADB není potřeba přeinstalovávat.

## Jednorázové propojení s místním projektem

1. V desktopové aplikaci zvol **Codex**, otevři místní složku tohoto repozitáře a začni místní relaci. Alternativou je spustit `codex` přímo v terminálu této složky. Samotná přítomnost GitHub konektoru v chatu nenahrazuje přístup k místnímu terminálu.
2. Přepni kopii na větev `codex/lenovo-tablet`. Pokud již máš rozpracované místní změny, nejprve je zachovej a nepřepisuj je nuceným přepnutím.
3. V místním terminálu ověř nástroj příkazem níže. Načte skutečný model, Android, rozlišení, hustotu a písmo do `.local/tablet/device.json`. Tento soubor ani snímky a logy se neodesílají do Gitu.

```bash
python3 tools/tablet_dev.py doctor
```

Pokud je připojeno více USB zařízení, zadej sériové číslo z `adb devices -l` pomocí `--serial`. Výchozí výběr používá jediné fyzické USB zařízení. Nástroj odmítá stav `unauthorized` nebo `offline`.

## Každodenní ovládání

Příkazy se zadávají ve složce projektu:

| Účel | Příkaz |
| --- | --- |
| Sestavit a nainstalovat vývojovou aplikaci | `python3 tools/tablet_dev.py install` |
| Nainstalovat již stažené APK | `python3 tools/tablet_dev.py install --apk /cesta/aplikace.apk` |
| Spustit aplikaci | `python3 tools/tablet_dev.py run` |
| Uložit zprávy běžící aplikace | `python3 tools/tablet_dev.py logs` |
| Pořídit snímek | `python3 tools/tablet_dev.py screenshot` |
| Spustit tři tabletové scénáře | `python3 tools/tablet_dev.py test` |
| Spustit s čekáním na debugger | `python3 tools/tablet_dev.py debug` |
| Vypnout čekání na debugger | `python3 tools/tablet_dev.py debug-off` |

Pro místní sestavení je potřeba JDK 17 a Android SDK s platformou 36 a build-tools 35.0.0, stejně jako v GitHub Actions. Android Studio může tyto závislosti připravit. Nástroj ADB hledá také v obvyklé Macové složce `~/Library/Android/sdk/platform-tools`, pokud jej aplikace nemá v PATH. Při chybě sestavení nejdříve zkontroluj tyto závislosti; samotné funkční ADB není sestavovací prostředí.

Instalace používá `adb install -r`. Při rozdílném podpisu se zastaví a **automaticky nemaže původní aplikaci ani její postup**. APK z různých CI běhů mohou mít jiný vývojový podpis. Před případnou čistou instalací použij export zálohy v aplikaci. Místní sestavení dále používají stabilní vývojový klíč daného Macu; soukromé podpisové soubory nepatří do Gitu.

Testy spusť s tabletem na šířku a s aplikací v celém okně při běžné velikosti písma. Ověřují mapu se seznamem a detailem, zachování výřezu, hledání, návrat a nastavení. Pro úzká okna, otočení a velké písmo existují i hostitelské testy. Výstupy jsou v `.local/tablet/`; úspěch testů se neposuzuje jen podle návratového kódu příkazu ADB, ale i podle protokolu testů.

Pro ladění jednotlivých řádků použij v Android Studiu **Attach debugger to Android process** a zvol `cz.jenda.wowforever.guide`. Debugger vyžaduje vývojové APK. Běžné hledání pádů lze dělat přes uložené logy; tyto dvě činnosti nejsou zaměnitelné.

V nastavení místního prostředí Codexu lze výše uvedené příkazy přidat jako akce **Ověřit tablet**, **Nainstalovat**, **Spustit** a **Testovat**. Spouštějí se v místním integrovaném terminálu. Konfiguraci vytvářej v aplikaci pro danou místní složku, nikoli domnělou změnou cloudové relace.

## Předání místnímu Codexu

> Navazuj na větev codex/lenovo-tablet repozitáře JendaNDT/WoW-Forever-Companion. Jenda používá Lenovo IdeaTab 11 5G 8/256. Tablet je k tomuto Macu připojen USB, ladění je povoleno a adb devices uvádí device. Spusť python3 tools/tablet_dev.py doctor, ověř skutečný model a parametry, pak sestav a nainstaluj aplikaci, spusť ji, projdi tabletové scénáře a zkontroluj logy i snímky. Při chybě podpisu automaticky nemaž původní aplikaci ani data. Zachovej místní změny. Herní správnost se bude ověřovat až po vydání hry.

## Zdroje

- [Místní prostředí a akce Codexu](https://learn.chatgpt.com/docs/environments/local-environment)
- [Codex v místním terminálu](https://learn.chatgpt.com/docs/codex/cli)
- [Android Debug Bridge](https://developer.android.com/tools/adb)
- [Ladění Android aplikace](https://developer.android.com/studio/debug)

Příprava těchto příkazů sama neznamená, že cloudový agent již ověřil fyzické připojení nebo provedl instalaci. To potvrdí teprve úspěšný běh v místním prostředí.
