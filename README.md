# zio-scala-fp

Running ZIO program:

1. `sbt run` works in intelliJ and VSCode

2. `metals + P: metals.run-current-file` works in VScode and metals plugin. (I made key binding `ctrl + option + cmd + r`)

3. `sbt` to start sbt terminal then `~Restart` to run using sbt revolver which is useful for automatically rebuilding when developing HTTP server. Works due to sbt plugin https://github.com/spray/sbt-revolver

4. `sbt` to start sbt terminal then `runMain dev.directoryName.scalafile` is good if you have multiple main classes and only want to choose a single file to run e.g. `runMain dev.zio_rest_api.zio_http_app`

REST api notes:

## Query params

### Sorting

use `sort` as key, followed by colon seperated kvp of column and sort direction (asc or dsc)

e.g.
sort=id:dsc
sort=last_updated:asc

### Filtering:

use column name as key, followed by single value to check for equality or colon seperated kvp of operator and value.

eq
gt = greatherThan
gte = greaterThanOrEqualTo
lt = lessThan
lte = lessThanOrEqualTo

e.g.
habitat=arctic
leap_score=gt:5
culinary_score=lte:5
