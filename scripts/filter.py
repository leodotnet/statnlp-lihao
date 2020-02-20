from __future__ import print_function
import optparse
import sys
import random
import hashlib
import re
import string
import xml.etree.ElementTree as ET
import subprocess
from nltk.tokenize import StanfordTokenizer
from collections import defaultdict



from optparse import OptionParser

parser = OptionParser(usage="%prog -l en -i 2 -p CC")
parser.add_option("-l", "--lang", dest="lang", type="string", help="Language", default="en")
parser.add_option("-i", "--index", dest="index", type="string", help="Index", default="2")
parser.add_option("-t", "--traintest", dest="traintest", type="string", help="TrainTest", default="test")
#parser.add_option("-p", "--postag", dest="split_postag", type="string", help="Split POS tag", default="CC")

(options, args) = parser.parse_args()
lang = options.lang
index = options.index
traintest = options.traintest
split_postag = ['CC', 'IN']

def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)

def valid(entity_list, postags):
    #print(entity_list)
    #print(postags)
    size = len(entity_list)
    for i in range(0, size - 1):
        start_index0 = entity_list[i][len(entity_list[i]) - 1]
        start_index1 = entity_list[i+1][2]

        #print("start_index:", start_index0, " ", start_index1)

        for j in (start_index0 + 1, start_index1):
            #print(postags[j])
            if postags[j] in split_postag:
                #print('Return True')
                return True


    return False



def get_observed(observed):
    global wanted_NEs

    example = 0
    word_index = 0
    entity = []
    last_ne = "O"
    last_entity = []
    instance_content = []
    instance_content_index = 0
    postags = []


    observations = defaultdict(defaultdict)
    observations[example] = []

    for line in observed:
        line = line.strip()
        if line.startswith("//") or line.startswith('##Tweet') or line.startswith('## Tweet'):
            go = True
            continue
        elif len(line) == 0:
            if entity:
                observations[example].append(list(entity))
                entity = []


            if valid(observations[example], postags):
                #print('## Tweet ' + str(instance_content_index))
                instance_content_index += 1
                for line in instance_content:
                    print(line)
                print()

            instance_content = []
            postags = []

            example += 1
            observations[example] = []
            word_index = 0
            continue

        else:
            instance_content.append(line)
            split_line = line.split("\t")
            word = split_line[0]
            value = split_line[9]
            postags.append(split_line[8].strip())
            ne = value[0]
            sent = value[2:]
            aspect = split_line[1][2:]

            last_entity = []

            # check if it is start of entity
            if ne == 'B' or (ne == 'I' and last_ne == 'O'):
                if entity:
                    last_entity = entity

                entity = [sent, aspect]

                entity.append(word_index)

            elif ne == 'I':
                entity.append(word_index)

            elif ne == 'O':
                if last_ne == 'B' or last_ne == 'I':
                    last_entity = entity
                entity = []

            if last_entity:
                observations[example].append(list(last_entity))
                last_entity = []

        last_ne = ne
        word_index += 1

    if entity:
        observations[example].append(list(entity))

    return observations




filename = 'data/Twitter_' + lang + "/" + traintest + "." + index + ".coll"

observed_file = open(filename, 'r', encoding='UTF-8')

get_observed(observed_file)


