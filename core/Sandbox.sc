import org.apache.jena.rdf.model.impl.Util

val uri = "http://wisecube.com/intervention#AACT14723809"
val ix = Util.splitNamespaceXML(uri)
uri.take(ix)