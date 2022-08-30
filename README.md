dsl command generator

+ minecraft warp command plugin

- create minecraft command from safety of kotlin dsl
- no yaml config
- real time autocompletion
- inspired by ktor routing

```kotlin
commands {
    command("echo") {
        callback("{text}", tab = { sender, parameters ->
            listOf("hello", "test")
        }) {
            val text = it.parameters["text"]
            it.sender.sendMessage("$text")
        }
    }
}
```

