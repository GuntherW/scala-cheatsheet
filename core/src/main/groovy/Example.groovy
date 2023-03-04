import groovy.transform.ToString

class Example {
    static void main(String[] args) {
        def a = new DockerImageCoordinates(registry: "registryA", repository: "repoA", tag: "tagA", arch: "arm64")
        def b = new DockerImageCoordinates(registry: "registryB", repository: "repoB", tag: "tagB")
        def c = new DockerImageCoordinates(registry: "registryC", repository: "repoC", tag: "tagC", arch: "")
        println(a)
        println(b)
        println(c)

        List<String> argList = []
        argList.addAll("--build-arg", "SERVICE=s1")
        argList.addAll("--build-arg", "VERSION=v1")
        argList.addAll("--build-arg", "GIT_COMMIT=c1")
        String artifact = "artifact"

        def first = ["docker", "buildx", "build", "--platform", "linux/amd64"]
        def secondList  = argList
        def thirdList = ["-t", artifact, "--load", "."]
        def list = ["docker", "buildx", "build", "--platform", "linux/amd64"] + argList + ["-t", artifact, "--load", "."]

        println(list)
    }


    @ToString(includeNames = true, includePackage = false)
    static class DockerImageCoordinates implements Serializable {
        String registry, repository, tag, arch

        String getArtifact() {
            def enhancedTag = arch == null || arch.isEmpty() ? tag : "${tag}-${arch}"
            "${repository}:${enhancedTag}"
        }

        String getImage() {
            "${registry}/${artifact}"
        }
    }
}


