===========================
Elasticsearch Export Plugin
===========================

This Elasticsearch plugin provides the ability to export data by query
on server side, by outputting the data directly on the according node.

The data will get exported as one json object per line.

Usage
=====

    curl -X POST 'http://localhost:9200/_export' -d '{"fields":["_id", "_source"], output_cmd:"gzip > /tmp/dump"}'

    curl -X POST 'http://localhost:9200/_export' -d '{"fields":["_id", "_source"], output_cmd:["gzip", ">", "/tmp/dump"]}'

    curl -X POST 'http://localhost:9200/_export' -d '{"fields":["_id", "_source"], output_file:"/tmp/dump"}'


Elements of request body
------------------------

Fields
~~~~~~

required

A list of fields to export. Each field must be defined in the mapping.

    "fields": ["name", "address"]

The mapping of fields to export has to be defined with "store": true


export_cmd
~~~~~~~~~~

required (if export_file has been omitted)

    "export_cmd": "gzip > /tmp/out"

    "export_cmd": ["gzip", ">", "/tmp/out"]

The command to execute. Might be defined as string or as array. The
content to export will get piped to Stdin of the command to execute.
Some variable substitution is possible (see Variable Substitution)


export_file
~~~~~~~~~~~

Required (if export_cmd has been omitted)

    "export_file": "/tmp/dump"

A path to the resulting output file. The containing directory of the
give export_file has to exist. The given export_file MUST NOT exist. Some
variable substitution is possible (see Variable Substitution)


force_overwrite
~~~~~~~~~~~~~~~

optional (default to false)

    "force_overwrite": true

Boolean flag to force overwriting existing export_file. This option only
make sense if export_file has been defined.


explain
~~~~~~~

optional (default to false)

    "explain": true

Option to evaluate the command to execute (like dry-run).


query
~~~~~

The query element within the export request body allows to define a
query using the Query DSL. See
http://www.elasticsearch.org/guide/reference/query-dsl/


get-parameters
~~~~~~~~~~~~~~

The api provides the general behavior of the rest API. See
http://www.elasticsearch.org/guide/reference/api/

preference
++++++++++

Controls a preference of which shard replicas to execute the export
request on. Different than the search API preference is set to
"_primary" by default. See
http://www.elasticsearch.org/guide/reference/api/search/preference/



Variable Substitution
---------------------

The following placeholders will get replace with the actuall value:

* ${cluster}       The name of the cluster
* ${index}         The name of the index
* ${shard}         The id of the shard


Installation
============

If you do not want to work on the repository, just use the standard
elasticsearch plugin command (inside your elasticsearch/bin directory)

    bin/plugin -install elasticsearch-export-plugin -url file:///path/to/elasticsearch-export-plugin/target/elasticsearch-export-plugin-1.0-SNAPSHOT.jar
