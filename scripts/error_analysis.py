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

DEBUG = False

def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)


num_arg = len(sys.argv)
arg = sys.argv


usage = "by default: --model spanmodel, --dataset sst --subpath default --l2 1.0E-4 --usediff 0"
parser = OptionParser(usage=usage)
parser.add_option("--model", type="string", help="Model", default="spanmodel", dest="model")
parser.add_option("--dataset", type="string", help="Dataset", default="sst", dest="dataset")
parser.add_option("--subpath", type="string", help="Subpath", default="default", dest="subpath")
parser.add_option("--l2", type="string", help="L2", default="1.0E-4", dest="L2")
parser.add_option("--filename", type="string", help="filename", default="", dest="filename")
parser.add_option("--usediff", type="int", help="usediff", default=0, dest="usediff")
parser.add_option("--testset", type="string", help="testset", default="test", dest="testset")

(options, args) = parser.parse_args()


modelpath = 'experiments//sentiment//' + options.model + '//'
dataset = {'sst':'sst', 'sst_trial':'sst_trial'}[options.dataset]

dir = os.path.join(modelpath, dataset)
dir = os.path.join(dir, options.subpath)

#print('dir=', dir)

for dataP in options.testset.split(','):
    predict = []
    gold = []
    pg = []
    if options.filename !=  '':
        filename = options.filename
    else:
        filename = options.dataset + '.' + options.L2 + '.' + dataP + '.out'
        filename = os.path.join(dir, filename)
    print('Analysing ' + filename)
    f = open(filename, 'r', encoding='utf-8')
    for line in f:
        fields = line.strip().split('\t')
        predict.append(int(fields[0]))
        gold.append(int(fields[1]))
        pg.append((int(fields[0]),int(fields[1])))
    f.close()

    predict = numpy.asarray(predict)
    gold = numpy.asarray(gold)



    n = predict.size

    diff = predict - gold

    if options.usediff == 1:
        abs_diff = diff
    else:
        abs_diff = numpy.abs(diff)

    if DEBUG:
        print('abs_diff', abs_diff)
    counter = collections.Counter(abs_diff)

    percent = {}
    for k in sorted(counter.keys()):
        percent[k] = '{:.2%}'.format(counter[k] / (n + 0.0))
    print('Difference Counter:', percent)


    for label in range(numpy.min(gold), numpy.max(gold) + 1):
        mask = numpy.ma.masked_inside(gold,label,label).mask #.astype(int)

        num_per_label = numpy.sum(mask.astype(int))

        abs_diff_per_label = abs_diff[mask]#numpy.multiply(mask, abs_diff)

        if DEBUG:
            print('\tmask:', mask)
            print('\tnum_per_label:', num_per_label)
            print('\tabs_diff_per_label:', abs_diff_per_label)
        counter = collections.Counter(abs_diff_per_label)
        percent = {}
        for k in sorted(counter.keys()):
            percent[k] = '{:.2%}'.format(counter[k] / (num_per_label + 0.0))

        print('\tDiff Counter for gold label=', label,'[',num_per_label ,']:', percent)

    print()




