#!/usr/bin/env python

import sys
import csv
import json
import logging
from elasticsearch import Elasticsearch

es_host = 'es.jethrolai.com'
DEFAULT_CSV_FILE_PATH = 'data/f_5500_2016_latest.csv'
DEFAULT_INDEX = 'index'
DEFAULT_TYPE = 'type'
MAX_BATCH_SIZE = 2000
global logger

def set_up_logging(logger_name, log_level=logging.INFO):
    global logger
    logger = logging.getLogger(logger_name)
    logger.setLevel(log_level)
    ch = logging.StreamHandler()
    formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    ch.setFormatter(formatter)
    logger.addHandler(ch)

def convert(csv_path=DEFAULT_CSV_FILE_PATH):
    """
    This method takes one optional argument to read a csv file in provided path and emits each row as a document to elasticsearch cluster.
    """
    global logger
    logger.info("Input documents will be emitted to {}".format(es_host))
    es = Elasticsearch([{'host': es_host, 'port': 80}])
    es.indices.create(index=DEFAULT_INDEX, ignore=400)

    logger.info("Reading csv file from '{}' and emitting them by batch...".format(csv_path))
    with open(csv_path, "rt") as csvfile:
        #TODO validation needed. Assumption: csv file is not empty. the first line is always field names
        reader = csv.reader(csvfile)
        fields = reader.next()
        bulk_data = []
        for row in reader:
            source = {}
            for i in range(len(fields)):
                if 'DATE' in fields[i] and row[i]=="":
                    source[fields[i]] = None
                else:
                    source[fields[i]] = row[i]
            doc = {
                "index": {
                    "_index": DEFAULT_INDEX,
                    "_type": DEFAULT_TYPE,
                }
            }
            bulk_data.append(doc)
            bulk_data.append(source)

            # Emit 1000 docs at a time
            if len(bulk_data)>=MAX_BATCH_SIZE:
                logger.info("Emitting {} docs ...".format(len(bulk_data)))
                res = es.bulk(index = DEFAULT_INDEX, body = bulk_data, refresh = True)
                logger.debug("Emission result: {}".format(res))
                bulk_data=[]

            #emit one doc at a time
            # res = es.index(index=DEFAULT_INDEX, doc_type=DEFAULT_TYPE, body=source)

            #emit all docs at once
#         logger.info("Ingesting all {} documents to {}/{} ...".format(len(bulk_data), es_host, DEFAULT_INDEX))
#         res = es.bulk(index = DEFAULT_INDEX, body = bulk_data, refresh = True)


if __name__ == "__main__":
    set_up_logging(__name__, logging.INFO)
    if len(sys.argv)>=2:
        es_host = sys.argv[1]
    convert()
