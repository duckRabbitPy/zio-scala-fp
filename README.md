# zio-scala-fp

Running ZIO program:

1. `sbt run` works in intelliJ and VSCode

2. `metals + P: metals.run-current-file` works in VScode and metals plugin. (I made key binding `ctrl + option + cmd + r`)

3. `~Restart` to run using sbt revolver which is useful for automatically rebuilding when developing HTTP server. Works due to sbt plugin https://github.com/spray/sbt-revolver
