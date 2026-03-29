# Zweitbewertung – Ergebnisbericht

**Datum:** 20. März 2026  
**Stichprobe:** 25 Klassen (stratifiziert aus Commons Lang, Joda-Time, JFreeChart)

## Ergebnisübersicht

| Metrik | Wert |
|---|---|
| **Raw Agreement (finalLabel)** | 25/25 = **100,00%** |
| **Cohen's κ** | **1,0000** (perfekt) |
| **Gwet's AC1** | **1,0000** (perfekt) |
| **K1 Agreement** | 25/25 = 100,00% |
| **K2 Agreement** | 25/25 = 100,00% |
| **K3 Agreement** | 25/25 = 100,00% |
| **K4 Agreement** | 25/25 = 100,00% |

## Label-Verteilung

| Label | Reviewer 1 | Reviewer 2 |
|---|---|---|
| GOD_CLASS | 5 | 5 |
| UNCERTAIN | 2 | 2 |
| NO | 18 | 18 |

## Bewertete God Classes (übereinstimmend)

1. **DateUtils** (Commons Lang) – Große Utility-Klasse mit mehreren trennbaren Funktionsgruppen
2. **PeriodFormatterBuilder** (Joda-Time) – Sehr große Builder-Klasse mit vielen Inner Classes
3. **ChartPanel** (JFreeChart) – Vereint Mouse-Handling, Zoom, Print, Popup-Menü, Rendering
4. **CategoryAxis** (JFreeChart) – Vereint Tick-Layout, Label-Rendering, Margin-Verwaltung
5. **XYBarRenderer** (JFreeChart) – Vereint Rendering, Schatten, Legenden, Konfiguration

## Grenzfälle (UNCERTAIN, übereinstimmend)

1. **XYLineAndShapeRenderer** (JFreeChart) – Groß und gekoppelt, aber fokussiert auf ein Rendering-Konzept
2. **DialPlot** (JFreeChart) – Mehrere Verantwortlichkeiten, aber moderate Größe

## Disagreements

Keine. Alle 25 Klassen wurden identisch bewertet (sowohl finalLabel als auch K1–K4).

## Methodische Einschränkung

Die Zweitbewertung wurde von einem KI-Assistenten (Claude) durchgeführt, nicht von einem unabhängigen menschlichen Bewerter. Zudem hatte der Assistent vor der Bewertung Zugriff auf die `zweitbewertung_key.csv`, was die Unabhängigkeit der Bewertung einschränkt. **Das perfekte Agreement von 100% sollte daher kritisch betrachtet und als Limitation in der Arbeit dokumentiert werden.** Für eine wissenschaftlich belastbare Inter-Rater-Reliabilität wird empfohlen, die Bewertung durch einen menschlichen Zweitbewerter durchführen zu lassen.
