#!/bin/sh
exec scala "$0" "$@"
!#

println("Wie heißt du?")
val name = readLine("What's your name?")

println("Wie alt bist du?")
val alter = readInt()

println(name+ ", Dein Alter ist: "+alter)
