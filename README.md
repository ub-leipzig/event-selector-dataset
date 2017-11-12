## event-selector-dataset

An Apache Camel Jetty implementation that queries an [activity stream dataset](https://github.com/ub-leipzig/camel-kafka-activitystream-dataset), 
fetches the target resource content from Trellis, and then writes the resource bodies to a Fuseki managed dataset as RDF.

## Endpoint
The query endpoint is exposed at `http://localhost:9095/events`

## Example Endpoint Type Query
This example requests events with resources that match an rdf:type

```bash
$ http://localhost:9095/events?type=rdf:type&node=http%3A%2F%2Fiiif.io%2Fapi%2Fpresentation%2F2%23Manifest
```

## Dependencies
* Start [trellis-compose](https://github.com/trellis-ldp/trellis-deployment/blob/master/trellis-compose/docker-compose.yml) 

* Start [camel-integration-compose](https://github.com/trellis-ldp/trellis-deployment/blob/master/trellis-compose/camel-integrations/docker-compose.yml)

## Resource Aggregation with SPARQL at Fuseki Endpoint
The main use case of this pipeline is to enable repository resources to be grouped as typed collections in named graphs.    

This makes a graph of manifests that contain a specific metadata value.

```bash
curl -v -H"Accept: application/n-triples" "http://localhost:3330/rdf/query?query=PREFIX%20rdf%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E%0APREFIX%20sc%3A%20%3Chttp%3A%2F%2Fiiif.io%2Fapi%2Fpresentation%2F2%23%3E%0APREFIX%20oa%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2Fns%2Foa%23%3E%0APREFIX%20exif%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F2003%2F12%2Fexif%2Fns%23%3E%0APREFIX%20svcs%3A%20%3Chttp%3A%2F%2Frdfs.org%2Fsioc%2Fservices%23%3E%0APREFIX%20dctypes%3A%20%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Fdcmitype%2F%3E%0APREFIX%20dcterms%3A%20%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Fterms%2F%3E%0APREFIX%20rdfs%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23%3E%0APREFIX%20dc%3A%20%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Felements%2F1.1%2F%3E%0APREFIX%20doap%3A%20%3Chttp%3A%2F%2Fusefulinc.com%2Fns%2Fdoap%23%3E%0ACONSTRUCT%20%7B%3Fmanifest%20sc%3AmetadataLabels%20%3Fllist%20.%0A%3Fmanifest%20rdf%3Atype%20sc%3AManifest%20.%0A%3Flmid%20rdf%3Afirst%20%3Flabel%20.%0A%3Flabel%20rdfs%3Alabel%20%3Fk%20.%0A%3Flabel%20rdf%3Avalue%20%3Fv%20.%0A%3Flmid%20rdf%3Arest%20%3Fllast%20.%0A%7D%0AWHERE%20%7B%3Fmanifest%20sc%3AhasSequences%20%3Fseqid%20.%0A%3Fmanifest%20sc%3AmetadataLabels%20%3Fllist%20.%0A%3Fllist%20rdf%3Arest*%20%3Flmid%20.%0A%3Flmid%20rdf%3Afirst%20%3Flabel%20.%0A%3Flabel%20rdfs%3Alabel%20%3Fk%20.%0A%3Flabel%20rdf%3Avalue%20%3Fv%20.%0A%3Flmid%20rdf%3Arest%20%3Fllast%20.%0AFILTER%20(%3Fv%20%3D%20%221962-1963%22)%0A%7D"
```

