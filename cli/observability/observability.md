### Erstellung eines Beispielprojektes um die Observability von Services zu demonstrieren

Das Beispielprojekt soll 3 unabhängige Services enthalten, die jeweils unterschiedliche Aufgaben erfüllen. Die Services
werden später manuell gestartet und sollen über die Konsole beobachtet werden können. Alle Services sollen in Scala
geschrieben werden und Tapir und Sttp als Bibliotheken verwenden. Die Services sollen über HTTP erreichbar sein und
JSON-Daten austauschen. Es soll ein Service sein, der vom Benutzer aufgerufen wird. Dieser Service ruft dann den zweiten
Service auf, der dann den dritten Service aufruft, eine Berechnung durchführt und das Ergebnis zurückgibt.

Hauptaufgabe der Services: Nicht so wichtig. Wichtig ist aber, daß sie über Opentelemetry Logs, Metriken und Traces
erzeugen. Ich kenne nicht denn genauen Umfang von Opentelemetry, aber ich möchte, daß die Services über die Konsole
beobachtet werden können. Ich weiß nicht, ob das noch Stand der Technik ist, aber die Logs, Metriken und Traces sollen
in einem Backend (Wahrscheinlich Jaeger, Prometheus und Elasticsearch/Kibana) gesammelt werden. Lege mir dazu eine
docker-compose.yml an, die diese Services hochfährt.

Rede mit mir, falls diese drei Backends (Jaeger, Prometheus und Elasticsearch/Kibana) veraltet sind und es bessere
Alternativen gibt.
