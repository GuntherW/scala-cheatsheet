#let chord(text, chord) = stack(spacing: 0.1em, [*#chord*], [#text])

#heading[Mein Lied]

#hbox(spacing: 2em, #image("gunther_cc.jpeg", width: 2cm), #block(
  spacing: 0.5em,
  [chord("La", "C")][chord("la", "G")][chord("la", "Am")][chord("la", "F")],
  "Dies ist der Liedtext, und die Akkorde stehen direkt über den Silben.",
))
