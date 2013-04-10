===========================
Elasticsearch Export Plugin
===========================

This Elasticsearch plugin provides the ability to export data by query
on server side, by outputting the data directly on the according node.

Usage
=====

    curl -X POST 'http://localhost:9200/_export' -d '{"fields":["_id", "_source"], output_cmd:"gzip > /tmp/dump"}'


fields
------

required

A list of fields to export. Each field must be defined in the mapping.

    "fields": ["name", "address"]

export_cmd
----------

required (if export_file has been omitted)

    "export_cmd": "gzip > /tmp/out"

    "export_cmd": ["gzip", ">", "/tmp/out"]

The command to execute. Might be defined as string or as array. Some
variable substitution is possible (see variables)


export_file
-----------

Required (if export_cmd has been omitted)

    "export_file": "/tmp/dump"

A path to the resulting output file. The containing directory of the
give export_file has to exist. The given export_file MUST NOT exist. Some
variable substitution is possible (see variables)


force_override
--------------

optional (default to false)

    "force_override": true

Boolean flag to force overwriting existing export_file. This option only
make sense if export_file has been defined.


explain
-------

optional (default to false)

    "explain": true

Option to evaluate the command to execute (like dry-run).


output_format
-------------

optional (default to json)

    "output_format": "json"

    "output_format": "delimited"

    "output_format": {"delimited": {"delimiter": "\u0001"}}

    "output_format": {"delimited": {"null_sequence":"\\N", "delimiter": "\u0001"}}

The output_format element defines the format of the output to
produce. In case of "json" each entry to export will be formatted as
json:

    {"name":"quodt", "adress":"Heimat 42"}

In case of "delimited" each entry will be exported as delimited values:

    quodt\u0001Heimat 42

``delimiter`` is always one character. Since json does not support
type char the first character of the given string will be taken. This
option will only make sense if format was set to "delimited".

``null_sequence`` defines the null value representation. This option
will only make sense if format was set to "delimited".

NOTE: output_format has not been implemented so far just the dafault
      is set to "json"


query
-----

The query element within the search request body allows to define a
query using the Query DSL. See
http://www.elasticsearch.org/guide/reference/query-dsl/


