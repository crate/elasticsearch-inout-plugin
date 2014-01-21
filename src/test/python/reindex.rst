=======
Reindex
=======

Via the ``_reindex`` endpoint it is possible to reindex one or all indexes
with a given query.

Reindex an existing Index
=========================

Create an index 'test' with a custom analyzer with the stop word 'guy'::

    >>> put("/test", {"settings": {"index": {"number_of_shards":1,
    ...                           "number_of_replicas":0,
    ...                           "analysis": {"analyzer": {"myan": {"type": "stop", "stopwords": ["guy"]}}}}}})
    {"acknowledged":true}

Create the mapping for type 'a' and use the custom analyzer as index analyzer
and use a simple search analyzer::

    >>> post("/test/a/_mapping", {"a": {"properties": {"name": {"type": "string", "index_analyzer": "myan", "search_analyzer": "simple", "store": "yes"}}}})
    {"acknowledged":true}

Add a document::

    >>> post("/test/a/1", {"name": "a nice guy"})
    {"_index":"test","_type":"a","_id":"1","_version":1,"created":true}
    >>> refresh()
    {...}

Querying for a non stop word term delivers a result::

    >>> post("/test/a/_search?pretty", {"query": {"match": {"name": "nice"}}})
    {
      ...
      "hits" : {
        "total" : 1,
        ...
    }

Querying for a stop word delivers no results::

    >>> post("/test/a/_search?pretty", {"query": {"match": {"name": "guy"}}})
    {
      ...
      "hits" : {
        "total" : 0,
        ...
    }

Now update the stop words configuration. To update settings the index has to
be closed first and then reopened::

    >>> post("/test/_close", {})
    {"acknowledged":true}
    >>> put("/test/_settings", {"analysis": {"analyzer": {"myan": {"type": "stop", "stopwords": ["nice"]}}}})
    {"acknowledged":true}
    >>> post("/test/_open", {})
    {"acknowledged":true}
    >>> refresh()
    {...}

Now do a reindex on the index 'test'::

    >>> post("/test/_reindex", {})
    {"writes":[..."succeeded":1...],"total":0,"succeeded":0,"failed":0,"_shards":{"total":1,"successful":1,"failed":0}}
    >>> refresh()
    {...}

No more result when querying for the new stop word 'nice'::

    >>> post("/test/a/_search?pretty", {"query": {"match": {"name": "nice"}}})
    {
      ...
      "hits" : {
        "total" : 0,
        ...
    }

The removed stop word 'guy' now delivers a result::

    >>> post("/test/a/_search?pretty", {"query": {"match": {"name": "guy"}}})
    {
      ...
      "hits" : {
        "total" : 1,
        ...
    }
