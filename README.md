# Hunter x Hunter: Gon & Killua (Forge 1.20.1)

Nen-System mit Aura, Ten/Zetsu/Ren und zwei spielbaren Charakteren: **Gon** (Verstärker) und **Killua** (Transmuter).
Benötigt die Mod **Player Animator** (Forge 1.20.1) für die Körper-Animationen. Ohne sie läuft die Mod trotzdem, nur ohne Animationen.

## Start im Spiel
`/nen choose gon` oder `/nen choose killua`
Zum Testen (Cheats an): `/nen level 8`, `/nen fill`, `/nen reset`

## Tasten
G nutzen · V/B Fähigkeit wechseln · R Ten · X Zetsu · C Ren · H Gyo · J En

## Fähigkeiten
| Level | Gon | Killua |
|---|---|---|
| 1 | Aura-Faust | Blitz-Handfläche |
| 3 | Jajanken: Choki (Schere) | Götterblitz |
| 4 | Riesen-Aufprall | Assassinen-Krallen |
| 6 | Jajanken: Pa (Papier) | Wirbelsturm |
| 8 | Jajanken: Gu (Stein) | Narukami |

## Blockzerstörung (config/hxhmod-common.toml, Abschnitt destruction)
blockDamage, dropBlocks, maxHardness (Standard 6, Obsidian bleibt heil), blockLimit, maxDebris.
Geschützte Bereiche (Spawn-Schutz, Claim-Mods) werden respektiert.

## Animationen
Dateien in `src/main/resources/assets/hxhmod/player_animation/*.json` (PlayerAnimator-Format, bearbeitbar mit Blockbench).

## Neu in dieser Version
- Zoom-Cutscenes (Kinobalken, Nahaufnahmen, harte Schnitte) bei Choki, Pa, Gu, Wirbelsturm und Narukami. Esc bricht ab, abschaltbar in der Config (`cutscenes`).
- Eigene Partikel (Glow, Flamme, Funke, Sternenblitz, Druckring, Schnittspur, Blitz) mit eigenen Texturen.
- Fähigkeits-Leiste mit Icons im HUD.
- Überarbeitete Körper-Animationen mit Ausholen, Überschwingen und Nachwippen.
- `/nen`-Befehle funktionieren in Einzelspieler-Welten auch ohne Cheats.
- Nahkampf-Kombos: Linksklick mit leerer Hand und Gegner in der Nähe. Gon 4 Schläge (Jab, Haken, Uppercut, Finisher mit Krater), Killua 5 Hiebe (2 Krallen, Drehtritt, X-Schnitt, Dash-Finisher mit Blitzen). Mit Schweif, Ausfallschritt und Hitstop.
- Neuer Charakter **Xeno** (Zeno Zoldyck, Drachenmeister): `/nen choose xeno`. Drachenkopf (Lv1), Drachenlanze (Lv3), Drachenritt (Lv4), Dragon Dive (Lv6), Drachensturm (Lv8) plus 4er-Kombo. Aura-Drachen aus Partikeln mit Drachenkopf-Sprite, Cutscenes für Lanze, Dive und Sturm.
- **Echte 3D-Modelle bei Attacken** (eigene Entities, ohne Zusatz-Bibliothek, leuchtend in der Aura-Farbe, prozedural animiert):
  - Drache mit Kopf, beweglichem Kiefer, Hörnern, Schnurrhaaren und 18 Körpersegmenten (Xeno: Drachenkopf, Lanze, Ritt, Dive, Sturm, Ren-Aufladen)
  - Riesen-Faust mit Aufladen, Zittern und Nachbildern (Gon: Gu, Aura-Faust, Kombo)
  - Offene Hand mit spreizenden Fingern (Pa), kreisende Scheren-Klingen (Choki)
  - Klingen-Hiebe (Killua/Xeno Kombo + Krallen), 3D-Blitze (Handfläche, Götterblitz, Narukami), Tornado (Wirbelsturm), Schockkuppel (Aufprall)
  - In der Config unter `visuals` mit `models3d` abschaltbar.
- Neuer Charakter **Pouf** (Shaiapouf, Schmetterlingswächter): `/nen choose pouf`. Schuppenpuder (Lv1), Flügelsturm (Lv3), Beelzebub-Sprung (Lv4), Kokon (Lv6), Schwarmsturm (Lv8) plus 4er-Kombo. Mit 3D-Schmetterlingsflügeln, Fliegenschwarm und Seidenkokon, Cutscenes für Kokon und Schwarmsturm.
  - Pouf-Update: Schmetterlingsflügel stehen jetzt senkrecht am Rücken (Ober-/Unterflügel), entfalten sich beim Erscheinen, schlagen im Takt der Attacke (Flügelsturm genau auf den Trefferticks) und haben Bewegungsunschärfe.
  - Beelzebub-Sprung: Schaden trifft, sobald der Schwarm den Gegner erreicht, Schuppen-Spur auf dem Flugweg, Schwarm-Aufprall mit Rückstoß am Ziel.
