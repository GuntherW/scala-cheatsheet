# language: de

Funktionalität: Arithmetik

  Szenario: Addition
    Wenn Ich addiere 4 und 5
    Dann ist das Ergebnis 9

  Szenario: AdditionsTest mit Dependency Injection
    Angenommen Ich habe die Werte 2 und 3
    Wenn ich diese Werte addiere
    Dann sei das Ergebnis 5

  Szenario: Addition mit Wertetabellen
    Angenommen Ich habe die Wertetabelle
    |2|3|
    |2|4|
    |2|5|
    Wenn ich diese Werte jeweils addiere
    Dann erhalte ich die Werte
    |5|
    |6|
    |7|
