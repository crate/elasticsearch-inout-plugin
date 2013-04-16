===========================
Elasticsearch Export Plugin
===========================

This Elasticsearch plugin provides the ability to export data by query
on server side, by outputting the data directly on the according node.
Data is exported for every shard of the query. The export can happen on
all indexes, on a specific index or on a specific type of an index.

The data will get exported as one json object per line::

    {"_id":"id1","_source":{"type":"myObject","value":"value1"}}
    {"_id":"id2","_source":{"type":"myObject","value":"value2"}}


Usage
=====

Export data to files in the node's file system. The filenames will be expanded
by index and shard names (p.e. /tmp/dump-myIndex-0)::

    curl -X POST 'http://localhost:9200/_export' -d '{"fields":["_id", "_source"], output_file:"/tmp/dump-${index}-${shard}"}'

Pipe the export data through a single argumentless command on the corresponding
node, like `cat`. This command actually returns the export data in the JSON
result's stdout field::

    curl -X POST 'http://localhost:9200/_export' -d '{"fields":["_id", "_source"], output_cmd: "cat"}'

Pipe the export data through argumented commands (p.e. a shell script). This
command will result in writing the zipped data to files on the node's file
system::

    curl -X POST 'http://localhost:9200/_export' -d '{"fields":["_id", "_source"], output_cmd:["/bin/sh", "-c", "gzip > /tmp/dump-${index}-${shard}.json.gz"]}'

To export only a specific index use an URL like this::

    http://localhost:9200/<index>/_export

Or for a specific type of an index::

    http://localhost:9200/<index>/<type>/_export


Elements of the request body
----------------------------

`fields`
~~~~~~~~

A list of fields to export. Each field must be defined in the mapping.

    "fields": ["name", "address"]

The mapping of fields to export has to be defined with "store": true

- Required

`output_cmd`
~~~~~~~~~~~~

    "output_cmd": "cat"

    "output_cmd": ["/bin/sh", "-c", "gzip > /tmp/out"]

The command to execute. Might be defined as string or as array. The
content to export will get piped to Stdin of the command to execute.
Some variable substitution is possible (see Variable Substitution)

- Required (if export_file has been omitted)

`output_file`
~~~~~~~~~~~~~

    "output_file": "/tmp/dump"

A path to the resulting output file. The containing directory of the
given export_file has to exist. The given output_file MUST NOT exist. Some
variable substitution is possible (see Variable Substitution).

- Required (if output_cmd has been omitted)

`force_overwrite`
~~~~~~~~~~~~~~~~~

    "force_overwrite": true

Boolean flag to force overwriting existing output_file. This option only
make sense if output_file has been defined.

- Optional (defaults to false)

`explain`
~~~~~~~~~

    "explain": true

Option to evaluate the command to execute (like dry-run).

- Optional (defaults to false)

`query`
~~~~~~~

The query element within the export request body allows to define a
query using the Query DSL. See
http://www.elasticsearch.org/guide/reference/query-dsl/

- Optional


Get parameters
--------------

The api provides the general behavior of the rest API. See
http://www.elasticsearch.org/guide/reference/api/

Preference
~~~~~~~~~~

Controls a preference of which shard replicas to execute the export
request on. Different than in the search API, preference is set to
"_primary" by default. See
http://www.elasticsearch.org/guide/reference/api/search/preference/


Variable Substitution
---------------------

The following placeholders will be replaced with the actual value:

* `${cluster}`: The name of the cluster
* `${index}`: The name of the index
* `${shard}`: The id of the shard


JSON Response
-------------

The _export query returns a JSON response with information about the export
status. The output differs a bit whether an output command or an output file
is given in the request body. Generally it delivers an export list ("exports"),
the number of total exported indexed ("totalExported") and information about
the shards ("_shards"), which contains total, successful and failed counts
of shard export operations.

Output file JSON response
~~~~~~~~~~~~~~~~~~~~~~~~~

The JSON response may look like this if an output file is given in the
request body::

    {
        "exports" : [
            {
                "index" : "myIndex",
                "shard" : 0,
                "node" : "someNodeName",
                "numExported" : 5,
                "output_file" : "/tmp/dump-myIndex-0"
            },
            {
                "index" : "myIndex",
                "shard" : 1,
                "error" : "[myIndex][1] failed, reason ..."
            }
        ],
        "totalExported" : 5,
        "_shards" : {
            "total" : 2,
            "successful" : 1,
            "failed" : 1
        }
    }

.. hint::

    - `index`: The name of the exported index
    - `shard`: The number of the exported shard
    - `node`: The node name where the export happened
    - `numExported`: The number of exported objects in the shard
    - `output_file`: The file name of the output file with substituted variables
    - `error`: A detailed error message of a shard operation if an error occured

Output command JSON response
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The JSON response may look like this if an output command is given in the
request body::

    {
        "exports" : [
            {
                "index" : "myIndex",
                "shard" : 0,
                "node" : "someNodeName",
                "numExported" : 5,
                "output_cmd" : ["/bin/sh", "-c", "gzip > /tmp/dump-myIndex-0.json.gz" ],
                "stderr" : "",
                "stdout" : "",
                "exitcode" : 0
            },
            {
                "index" : "myIndex",
                "shard" : 1,
                "error" : "[myIndex][1] failed, reason ..."
            }
        ],
        "totalExported" : 5,
        "_shards" : {
            "total" : 2,
            "successful" : 1,
            "failed" : 1
        }
    }

.. hint::

    - `index`: The name of the exported index
    - `shard`: The number of the exported shard
    - `node`: The node name where the export happened
    - `numExported`: The number of exported objects in the shard
    - `output_cmd`: The executed command on the node with substituted variables
    - `stderr`: The first 8K of the standard error log of the executed command
    - `stdout`: The first 8K of the standard output log of the executed command
    - `exitcode`: The exit code of the executed command
    - `error`: A detailed error message of a shard operation if an error occured

Installation
============

If you do not want to work on the repository, just use the standard
elasticsearch plugin command (inside your elasticsearch/bin directory)

    bin/plugin -install elasticsearch-export-plugin -url file:///path/to/elasticsearch-export-plugin/target/elasticsearch-export-plugin-1.0-SNAPSHOT.jar
