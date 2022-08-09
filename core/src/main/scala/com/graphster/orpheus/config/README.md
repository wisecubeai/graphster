Example 1. config file with one ValueMeta.
```json
{
  "qid2uri": {
    "type": "URIElement",
    "name": "acme_entity_node",
    "uri": {
      "type": "ConcatValueMeta",
      "name": "acme_uri",
      "values": [
        {
          "type": "LiteralValueMeta",
          "name": "acme_ns",
          "value": "http://www.acme.org/entity/"
        },
        {
          "type": "ColumnValueMeta",
          "name": "acme_id",
          "column": "id"
        }
      ]
    }
  }
}
```

Example 2. how to load config

```scala
import com.graphster.orpheus.config.Configuration
import com.graphster.orpheus.config.graph.URIGraphConf

val config: Configuration = Configuration.load("...")
```