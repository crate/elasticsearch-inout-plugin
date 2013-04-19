==========================
Elasticsearch InOut Plugin
==========================

This Elasticsearch plugin provides the ability to export data by query
on server side, by outputting the data directly on the according node.
The export can happen on all indexes, on a specific index or on a specific
document type.

The data will get exported as one json object per line::

    {"_id":"id1","_source":{"type":"myObject","value":"value1"}}
    {"_id":"id2","_source":{"type":"myObject","value":"value2"}}


Usage
=====

Examples
--------

Below are some examples demonstrating what can be done with the elasticsearch
inout plugin. The example commands require installation on a UNIX system.
The plugin may also works with different commands on other operating
systems supporting elasticsearch, but is not tested yet.

Export data to files in the node's file system. The filenames will be expanded
by index and shard names (p.e. /tmp/dump-myIndex-0)::

    curl -X POST 'http://localhost:9200/_export' -d '{
        "fields": ["_id", "_source"],
        "output_file": "/tmp/dump-${index}-${shard}"
    }
    '

Do GZIP compression on file exports::

    curl -X POST 'http://localhost:9200/_export' -d '{
        "fields": ["_id", "_source"],
        "output_file": "/tmp/dump-${index}-${shard}.gz",
        "compression": "gzip"
    }
    '

Pipe the export data through a single argumentless command on the corresponding
node, like `cat`. This command actually returns the export data in the JSON
result's stdout field::

    curl -X POST 'http://localhost:9200/_export' -d '{
        "fields": ["_id", "_source"],
        "output_cmd": "cat"
    }
    '

Pipe the export data through argumented commands (p.e. a shell script, or
provide your own sophisticated script on the node). This command will
result in transforming the data to lower case and write the file to the
node's file system::

    curl -X POST 'http://localhost:9200/_export' -d '{
        "fields": ["_id", "_source"],
        "output_cmd": ["/bin/sh", "-c", "tr [A-Z] [a-z] > /tmp/outputcommand.txt"]
    }
    '

Limit the exported data with a query. The same query syntax as for search can
be used::

    curl -X POST 'http://localhost:9200/_export' -d '{
        "fields": ["_id", "_source"],
        "output_file": "/tmp/query-${index}-${shard}",
        "query": {
            "match": {
                "someField": "someValue"
            }
        }
    }
    '


Elements of the request body
----------------------------

``fields``
~~~~~~~~~~

A list of fields to export. Each field must be defined in the mapping.

    "fields": ["name", "address"]

The mapping of fields to export has to be defined with ``"store": true``

- Required

``output_cmd``
~~~~~~~~~~~~~~

    "output_cmd": "cat"

    "output_cmd": ["/location/yourcommand", "argument1", "argument2"]

The command to execute. Might be defined as string or as array. The
content to export will get piped to Stdin of the command to execute.
Some variable substitution is possible (see Variable Substitution)

- Required (if ``output_file`` has been omitted)

``output_file``
~~~~~~~~~~~~~~~

    "output_file": "/tmp/dump"

A path to the resulting output file. The containing directory of the
given ``output_file`` has to exist. The given ``output_file`` MUST NOT exist.
Some variable substitution is possible (see Variable Substitution).

- Required (if ``output_cmd`` has been omitted)

``force_overwrite``
~~~~~~~~~~~~~~~~~~~

    "force_overwrite": true

Boolean flag to force overwriting existing ``output_file``. This option only
make sense if ``output_file`` has been defined.

- Optional (defaults to false)

``explain``
~~~~~~~~~~~

    "explain": true

Option to evaluate the command to execute (like dry-run).

- Optional (defaults to false)

``compression``
~~~~~~~~~~~~~~~

    "compression": "gzip"

Option to activate compression to the output. Works both whether
``output_file`` or ``output_cmd`` has been defined. Currently only the
``gzip`` compression type is available. Omitting the option will result
in uncompressed output to files or processes.

- Optional (default is no compression)

``query``
~~~~~~~~~

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

The following placeholders will be replaced with the actual value in
the ``output_file`` or ``output_cmd`` fields:

* ``${cluster}``: The name of the cluster
* ``${index}``: The name of the index
* ``${shard}``: The id of the shard


JSON Response
-------------

The _export query returns a JSON response with information about the export
status. The output differs a bit whether an output command or an output file
is given in the request body.

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
            }
        ],
        "totalExported" : 5,
        "_shards" : {
            "total" : 2,
            "successful" : 1,
            "failed" : 1,
            "failures" : [
                {
                    "index" : "myIndex",
                    "shard" : 1,
                    "reason" : "..."
                }
            ]
        }
    }

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
                "output_cmd" : [
                    "/bin/sh",
                    "-c",
                    "tr [A-Z] [a-z] > /tmp/outputcommand.txt"
                ],
                "stderr" : "",
                "stdout" : "",
                "exitcode" : 0
            }
        ],
        "totalExported" : 5,
        "_shards" : {
            "total" : 2,
            "successful" : 1,
            "failed" : 1,
            "failures": [
                {
                    "index" : "myIndex",
                    "shard" : 1,
                    "reason" : "..."
                }
            ]
        }
    }

.. hint::

    - ``exports``: List of successful exports
    - ``totalExported``: Number of total exported objects
    - ``_shards``: Shard information
    - ``index``: The name of the exported index
    - ``shard``: The number of the exported shard
    - ``node``: The node id where the export happened
    - ``numExported``: The number of exported objects in the shard
    - ``output_file``: The file name of the output file with substituted variables
    - ``failures``: List of failing shard operations
    - ``reason``: The error report of a specific shard failure
    - ``output_cmd``: The executed command on the node with substituted variables
    - ``stderr``: The first 8K of the standard error log of the executed command
    - ``stdout``: The first 8K of the standard output log of the executed command
    - ``exitcode``: The exit code of the executed command


Installation
============

If you do not want to work on the repository, just use the standard
elasticsearch plugin command (inside your elasticsearch/bin directory)

    bin/plugin -install elasticsearch-inout-plugin -url file:///path/to/elasticsearch-inout-plugin/target/elasticsearch-inout-plugin-1.0-SNAPSHOT.jar
