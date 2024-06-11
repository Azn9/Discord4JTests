rootProject.name = "Discord4JTests"

includeBuild("../Discord4J") {
    dependencySubstitution {
        substitute(module("com.discord4j:discord4j-core")).using(project(":core"))
        substitute(module("com.discjson4j:discord4j-rest")).using(project(":rest"))
        substitute(module("com.discord4j:discord4j-gateway")).using(project(":gateway"))
        substitute(module("com.discord4j:discord4j-voice")).using(project(":voice"))
    }
}
