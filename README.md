# Meme Hub

Kliencki mod testowy dla **Minecraft Java Edition 26.2** ("Chaos Cubed"), Fabric.
Przeznaczony do **testowania mechanik walki, automatyzacji oraz eksperymentow
z silnikiem gry w pojedynczym swiecie**.

> Uwaga: to narzedzie testowe. Uzywanie na serwerach multiplayer z innymi
> graczami moze byc uznane za oszustwo i jest na wlasna odpowiedzialnosc.

## Wymagania

- Java SE 25 (wymagane przez Minecraft 26.x)
- Fabric Loader 0.19.3+ oraz Fabric API 0.152.0+ (zalecane: 0.158.0+26.2)
- Gradle 9.5.1 / Loom 1.17 (w projekcie)

## Budowa i uruchomienie

```bash
# wygenerowanie wrappera (wymaga systemowego Gradle 9.x, tylko pierwszy raz)
gradle wrapper

# zbudowanie moda
./gradlew build

# gotowy plik: build/libs/meme-hub-1.0.0.jar
# wrzuc go do folderu mods/ instalacji Fabric 26.2
```

## Struktura projektu

```
src/main/java/pl/memehub/
  MemeHub.java            - stale moda
  MemeHubClient.java      - entrypoint (ClientModInitializer), rejestracja HUD
  core/
    Module.java           - klasa bazowa modulu (nazwa, kategoria, keybind, ustawienia)
    ModuleManager.java    - rejestr modulow + obsluga keybindow + glowny tick
    Category.java         - kategorie (COMBAT, MOVEMENT, RENDER, UTILITY)
    settings/             - BooleanSetting, NumberSetting, EnumSetting
  event/
    EventBus.java         - wlasny event bus (@Subscribe + post)
    ClientTickEvent.java  - tick klienta (Pre/Post)
    PacketSendEvent.java  - przechwytywanie pakietow wychodzacych (anulowalny)
    EntityMoveEvent.java  - hook fizyki (Entity#move, anulowalny)
    HudRenderEvent.java   - render HUD
  mixin/
    MinecraftClientMixin.java       - zrodlo tickow
    ConnectionMixin.java            - przechwytywanie pakietow (send)
    EntityMixin.java                - fizyka (move)
    MultiPlayerGameModeMixin.java   - hook przed atakiem
  config/Config.java      - zapis/odczyt konfiguracji JSON
  gui/ClickGuiScreen.java - panel graficzny (26.x Screen)
  modules/
    combat/   AutoCrystal, AnchorAura, AutoTotem, KillAura, Criticals,
              Triggerbot, MaceSmashAssist, WindChargeSynergy, Velocity, Surround
    vehicle/  CartPlacer (TNT Cart Auto-Placer), CartBreaker
    movement/ Sprint, NoFall, Speed, AirJump
    render/   Fullbright, HudModule (ArrayList, TargetHUD, cooldown, FPS, coords),
              Watermark
    utility/  ClickGuiModule, Panic, AutoTool, AutoRespawn
  util/       ChatUtil, RotationUtil, EntityUtil, InventoryUtil,
              InteractionUtil, DamageMath, RenderUtil
```

## Architektura

- **Wlasny EventBus** (`pl.memehub.event.EventBus`) - klasy rejestruja sie przez
  `EventBus.INSTANCE.register(...)`, metody z `@Subscribe` przyjmujace dokladnie
  jeden argument podtypu `Event` sa wywolywane przez `post(event)`.
- **Miksiny**: `ConnectionMixin` przechwytuje pakiety wychodzace (anulowanie
  `PacketSendEvent` blokuje wyslanie), `EntityMixin` pozwala nadpisywac fizyke
  (anulowanie `EntityMoveEvent` zatrzymuje ruch), `MultiPlayerGameModeMixin`
  daje hook przed atakiem, `MinecraftClientMixin` generuje ticki.
- **HUD**: element zarejestrowany przez Fabric API `HudElementRegistry`
  (`attachElementBefore(VanillaHudElements.CHAT, ...)`) wysyla `HudRenderEvent`
  - rysowanie 2D przez `GuiGraphicsExtractor` (26.1+; kolory ARGB).
- **Renderowanie swiata (ESP)** nie jest wspierane w 26.2 bez wlasnej
  infrastruktury feature/submit - obowiazuje 2D HUD (patrz primer 26.2).

## Moduly

### Combat
- **AutoCrystal** - stawianie/niszczenie krysztalow Endu, kalkulacja obrazen
  (wzor eksplozji vanilla), opoznienia place/break/switch, auto-switch,
  tryby celu (gracz/mob/self), tryby stawiania (ABOVE/SURROUND).
- **AnchorAura** - stawianie Respawn Anchora, ladowanie jasnota (glowstone),
  eksplozja pusta reka, cykl z opoznieniami.
- **AutoTotem** - przekladanie Totemu do drugiej reki ponizej progu zdrowia.
- **KillAura** - obrot (NONE/INSTANT/SMOOTH), priorytety celu
  (DISTANCE/HEALTH/ARMOR), filtry (gracze/potwory/zwierzeta), cooldown 1.9+.
- **Criticals** - tryb PACKET (pakietowe mikroskoki onGround=false) lub JUMP.
- **Triggerbot** - atak gdy celownik najedzie na wroga.
- **Mace Smash Assist** - auto-switch na Bulave i atak przy maksymalnej
  predkosci opadania (najwyzsze obrazenia).
- **Wind Charge Synergy** - Wind Charge pod nogi + atak Bulawa z powietrza.
- **Velocity** - redukcja knockbacku (modyfikacja wektora ruchu przez
  EntityMoveEvent; tryby HURT/ALWAYS, poziomo/pionowo).
- **Surround** - automatyczne otaczanie gracza sciana z obsydianu
  (tryby CROSS/FULL, opoznienia miedzy blokami).

### Cart PvP
- **TNT Cart Auto-Placer** - tor -> wozek TNT -> natychmiastowa detonacja
  (atak na wozek), tryby SELF/TARGET, opoznienia krokow.
- **Cart Breaker** - niszczy wszystkie wozki wokol (wykrywanie po opisie typu).

### Movement / Render / Utility
- Sprint, NoFall (pakietowe resetowanie upadku), Speed (strafe boost),
  AirJump (double jump), Fullbright,
  HUD (ArrayList / TargetHUD / cooldown ataku / FPS / wspolrzedne),
  Watermark (logo moda), ClickGUI (keybind: Prawy Shift),
  Panic (wylacza wszystko), AutoTool, AutoRespawn.

## ClickGUI

Otwierany domyslnie **Prawym Shiftem** (keybind zmienisz w samym GUI):

- LPM na naglowku panelu - przeciaganie; PPM - zwijanie/rozwijanie panelu.
- Kolo myszy - przewijanie dlugich list modulow.
- LPM na module - wlacz / wylacz; PPM - rozwin ustawienia.
- Ustawienia: boolean - klik; liczba - LPM (+) / PPM (-); enum - cykl LPM.
- Wiersz "KEY" - kliknij i wcisnij klawisz, aby ustawic keybind (ESC - usun).
- Tooltip z opisem modulu / ustawienia po najechaniu myszka.

## Konfiguracja

Stan modulow, keybindy i ustawienia zapisuja sie do
`config/memehub/config.json` (zapis przy zmianie w ClickGUI oraz przy wyjsciu).
