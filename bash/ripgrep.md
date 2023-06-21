### Normale Nutzung
```shell
rg '// TODO'
```

### Zeige nur die Dateien
```shell
rg -l '// TODO'
```

### Zeige nur die Dateien mit und ohne Regex
```shell
rg -l '\bNormale\b' #Word boundary
rg -l '\b(Normale|Nutzung)\b' #Word boundary und alternation
rg -l '^### Normale' # Zeile startet mit 
rg -l 'N.?rmale' # Regex ist Standard
rg -F 'N.?rmale' # Ohne Regex (Fixed String)
```

### Zeige nur die Dateien und sortiere
```shell
rg -l '// TODO' --sort path
```

### Zeige nur für bestimmte Dateien mit Typ 
`md` ist hier nicht die Dateiendung, sondern ein Typ. Alle möglichen Typen können mit `rg --type-list` angezeigt werden
```shell
rg -l -t md '// TODO'
```

### Zeige nur für bestimmte Dateien mit Glob
```shell
rg -l -g 'ripg*.md' '// TODO'
```

### Dateien ohne Match
```shell
rg --files-without-match '// TODO'
```

### Drumherum die Zeilen mit Anzeigen
```shell
rg  '// TODO' -C 2
```

### Die nächsten Zeilen mit Anzeigen
```shell
rg  '// TODO' -A 2 # -B für Before
```