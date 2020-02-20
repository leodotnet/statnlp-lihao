import os

from optparse import OptionParser
#from preprocesstwitter import tokenize
import random

usage = "split_data.py --inputfile [inputfile] --outputfile1 [outputfile1] --outputfile2 [outputfile2] --ignore [ignore string] --numsent [numer of sentence]"

parser = OptionParser(usage=usage)
parser.add_option("--input", type="string", help="inputfile", default="", dest="inputfile")
parser.add_option("--output1", type="string", help="outputfile1", default="", dest="outputfile1")
parser.add_option("--output2", type="string", help="outputfile1", default="", dest="outputfile2")
parser.add_option("--numsent", type="int", help="Number of sentence", default=-1, dest="numsent")
parser.add_option("--ignore", type="string", help="Ignore String", default="##", dest="ignore")
parser.add_option("--splitratio", type="string", help="Split Ratio", default="5,1", dest="split_ratio")
parser.add_option("--encoding", type="string", help="encoding", default="utf-8", dest="encoding")

##split_ratio: pecentage of output1
def split_data(inputfilename, outputfilename1, outputfilename2, split_ratio, ignore_set, encoding):
    inputfile = open(inputfilename, 'r', encoding=encoding)
    instances = []
    instance = []
    for line in inputfile:
        line = line.strip()
        if len(line) == 0:
            instances.append(instance)
            instance = list()
        else:
            instance.append(line)

    inputfile.close()

    outputfile1 = open(outputfilename1, 'w', encoding=encoding)
    outputfile2 = open(outputfilename2, 'w', encoding=encoding)

    for i in range(0, len(instances)):
        rnd = random.random();
        if rnd < split_ratio:
            outputfile = outputfile1
        else:
            outputfile = outputfile2


        for line in instances[i]:
            outputfile.write(line + '\n')
        outputfile.write('\n')


    outputfile1.close()
    outputfile2.close()


(options, args) = parser.parse_args()
split_ratio = options.split_ratio.split(',')
ratio1 = (float)(split_ratio[0])
ratio2 = (float)(split_ratio[1])
split_data(options.inputfile, options.outputfile1, options.outputfile2, ratio1 / (ratio1 + ratio2), options.ignore.strip().split(','), options.encoding)