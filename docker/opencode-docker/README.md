# opencode in docker container

This folder shows how to run opencode inside a docker container
to minimize the blast radius of the tool.

The Dockerfile contains the build of the container. Any tool that
opencode should be able to access, must be installed here.

## Build the image

The container is easily built in this directory
with the following command:

```bash
docker build -t opencode-docker-scala-cheatsheet .
```

## Run the container

The easiest way is to use the provided script:

```bash
./run.sh
```

Alternatively, run manually from the project root:

```bash
docker run -d \
    --name opencode-container-scala-cheatsheet \
    -v "$PWD":/workspace \
    -v ~/.opencode_auth:/root/.local/share/opencode \
    opencode-docker-scala-cheatsheet
```

The container is now running but doing nothing. Run a new session
of opencode with:

```bash
docker exec -it opencode-container-scala-cheatsheet opencode .
```

## On first run

When run at the first time, you must call `/connect` in opencode to activate
the github copilot models. For following runs, the data is stored
in the `~/.opencode_auth` folder.

