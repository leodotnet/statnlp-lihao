from __future__ import print_function
from optparse import OptionParser
import sys
import random
import hashlib
import re
import os
import string
import xml.etree.ElementTree as ET
from nltk.tokenize import StanfordTokenizer
from collections import defaultdict
import numpy
import collections
#from nltk import FreqDict


def getNERList(filename, WordCol = 0, NERCol = -1, Seperator = '\t'):
    f = open(filename, 'r', encoding='utf-8')

    NERDict = {}

    NER = ''
    lasttag = 'O'
    for line in f:
        line = line.strip()
        if line == '':
            continue
        fields = line.split(Seperator)
        word = fields[WordCol]
        tag = fields[NERCol][0]

        if tag == 'B' or (tag == 'I' and lasttag == 'O'):
            if NER != '':
                #NER = NER.lower()
                if (NER not in NERDict):
                    NERDict[NER] = 1
                else:
                    NERDict[NER] += 1

            NER = word
        elif tag == 'I':
            NER += ' ' + word
        elif tag == 'O':
            if lasttag != 'O':
                #NER = NER.lower()
                if (NER not in NERDict):
                    NERDict[NER] = 1
                else:
                    NERDict[NER] += 1

                NER = ''

        lasttag = tag
    f.close()

    return NERDict


def getCommonNERList(NERDict1, NERDict2):
    commonNERList = []
    for NER in NERDict1.keys():
        if NER in NERDict2.keys():
            commonNERList.append(NER)

    return commonNERList

