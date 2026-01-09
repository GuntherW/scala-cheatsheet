# Basis-Image - Ubuntu 24.04 LTS (neueste LTS-Version)
FROM ubuntu:24.04

# Vermeidung von Interaktionen während der Installation
ENV DEBIAN_FRONTEND=noninteractive

# Installation von System-Abhängigkeiten (inkl. zip/unzip für SDKMAN)
RUN apt-get update && apt-get install -y \
    curl \
    git \
    ca-certificates \
    zip \
    unzip \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# SDKMAN installieren
RUN curl -s "https://get.sdkman.io" | bash

# SDKMAN Environment Setup
ENV SDKMAN_DIR="/root/.sdkman"
ENV PATH="${SDKMAN_DIR}/candidates/java/current/bin:${SDKMAN_DIR}/candidates/scala/current/bin:${SDKMAN_DIR}/candidates/sbt/current/bin:${SDKMAN_DIR}/candidates/gradle/current/bin:${PATH}"

# Java 25 (Temurin), Scala, SBT und Gradle via SDKMAN installieren
RUN bash -c "source ${SDKMAN_DIR}/bin/sdkman-init.sh && \
    sdk install java 25-tem && \
    sdk install scala && \
    sdk install sbt && \
    sdk install gradle && \
    sdk flush archives && \
    sdk flush temp"

# Versionen ausgeben zur Verifikation
RUN bash -c "source ${SDKMAN_DIR}/bin/sdkman-init.sh && \
    java -version && \
    scala -version && \
    sbt --version && \
    gradle --version"

# Arbeitsverzeichnis im Container festlegen
WORKDIR /workspace

# OpenCode installieren
ENV PATH="/root/.opencode/bin:${PATH}"
RUN curl -fsSL https://opencode.ai/install | bash \
    && (which opencode || echo "Warnung: opencode nicht im Pfad gefunden")

# Startbefehl: Container am Leben halten
CMD ["tail", "-f", "/dev/null"]
