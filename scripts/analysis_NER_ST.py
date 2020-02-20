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
from util import *

DEBUG = False

def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)


num_arg = len(sys.argv)
arg = sys.argv


usage = "by default: --filename1 []  --filename2 []"
parser = OptionParser(usage=usage)
parser.add_option("--filename1", type="string", help="filename1", default="data//Twitter_en//train.2.coll", dest="filename1")
parser.add_option("--filename2", type="string", help="filename2", default="data//wnut//train_notypes", dest="filename2")
parser.add_option("--detail", type="int", help="detail", default=0, dest="detail")

(options, args) = parser.parse_args()

def main():
    NERDict1 = getNERList(options.filename1)
    NERDict2 = getNERList(options.filename2)
    commonNERList = getCommonNERList(NERDict1, NERDict2)

    print('NER1:', len(NERDict1))
    print('NER2:', len(NERDict2))

    if options.detail == 1:
        print(NERDict1.keys())

    print('common NER:', len(commonNERList))
    if options.detail == 1:
        print(commonNERList)

main()

