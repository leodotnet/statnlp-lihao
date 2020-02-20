import xml.etree.ElementTree as ET
# import nltk
# from nltk.tokenize import sent_tokenize, word_tokenize
# from nltk.tag.stanford import StanfordPOSTagger
from optparse import OptionParser
#from preprocesstwitter import tokenize

import random
import os.path

usage = "eval-bioscope.py --dataset bioscope"

parser = OptionParser(usage=usage)
parser.add_option("-g", type="string", help="gold", default="", dest="gold")
parser.add_option("-s", type="string", help="pred", default="", dest="pred")
parser.add_option("-v", type="string", help="version", default="A", dest="version")
parser.add_option("-b", type="int", help="batch", default=0, dest="batch")
parser.add_option("-r", type="string", help="range", default="1,9", dest="range")

#
#parser.add_option("--outputpath", type="string", help="outputpath", default="", dest="outputpath")

(options, args) = parser.parse_args()



def readNegation(filename):
    insts = []
    sentence = []
    f = open(filename, 'r', encoding='utf-8')
    for line in f:
        line = line.strip()
        fields = line.split('|||')
        sentence.append(line)
        negList = []
        for i in range(2, len(fields)):
            negFields = fields[i].strip().split(' ')
            neg = ((int(negFields[0]), int(negFields[1])), (int(negFields[3]), int(negFields[4])))  ###  span, cue
            negList.append(neg)

        insts.append(negList)

    f.close()

    return insts


def calcPRF(stat, version = 'A', isCue = True):

    if isCue:
        stat['P'] = (stat['#match'] + 0.0) / stat['#pred']
    else:
        if version == 'B':
            stat['P'] = (stat['#match'] + 0.0) / stat['#pred']
        else:
            stat['P'] = (stat['#match'] + 0.0) / (stat['#match'] + stat['#FP'])


    stat['R'] = (stat['#match'] + 0.0) / stat['#gold']
    #print('stat:',stat)
    if stat['#match']  == 0:
        stat['F'] = 0
    else:
        stat['F'] = 2 * stat['P'] * stat['R'] / (stat['P'] + stat['R'])
    return stat

def calc(gold, pred, version = 'A'):
    assert len(gold) == len(pred)

    cue = {'#gold':0, '#pred':0, '#match':0, '#FP':0, 'P':0.0, 'R':0.0, 'F':0.0, 'name':'cue'}
    scope = {'#gold':0, '#pred':0, '#match':0, '#FP':0, 'P':0.0, 'R':0.0, 'F':0.0, 'name':'scope'}
    partialscope = {'#gold': 0, '#pred': 0, '#match': 0.0, '#FP': 0, 'P': 0.0, 'R': 0.0, 'F': 0.0, 'name': 'scope'}


    for i in range(0, len(gold)):
        goldNegs = gold[i]
        predNegs = pred[i]

        cue['#gold'] += len (goldNegs)
        scope['#gold'] += len(goldNegs)
        partialscope['#gold'] += len(goldNegs)

        cue['#pred'] += len(predNegs)
        scope['#pred'] += len(predNegs)
        partialscope['#pred'] += len(predNegs)

        for predNeg in predNegs:

            matchPredScope = False
            matchPredCue = False

            for goldNeg in goldNegs:
                if goldNeg[1] == predNeg[1]:
                    cue['#match'] += 1
                    matchPredCue = True


                    if goldNeg[0] == predNeg[0]:
                        scope['#match'] += 1
                        matchPredScope = True





            if matchPredCue == True and matchPredScope == False:
            #if len(goldNegs) == 0:
                scope['#FP'] += 1


    cue = calcPRF(cue, version)
    scope = calcPRF(scope, version, False)
    return (cue, scope)




def add(stat1, stat2):
    stat = {'P': 0.0, 'R': 0.0, 'F': 0.0}

    stat['P'] = stat1['P'] + stat2['P']
    stat['R'] = stat1['R'] + stat2['R']
    stat['F'] = stat1['F'] + stat2['F']

    return stat

def scale(stat, scaling):
    stat['P'] = stat['P'] * scaling
    stat['R'] = stat['R'] * scaling
    stat['F'] = stat['F'] * scaling
    return stat






print('options:', options)


if options.batch == 0:
    gold = readNegation(options.gold)
    pred = readNegation(options.pred)
    cue, scope = calc(gold, pred, version='A')
    print('Version A:')
    print('cue:', cue)
    print('scope:', scope)
    print()
    cue, scope = calc(gold, pred, version='B')
    print('Version B:')
    print('cue:', cue)
    print('scope:', scope)
else:
    fromIdxStr, endIdxStr = options.range.split(',')
    fromIdx = int(fromIdxStr)
    endIdx = int(endIdxStr)

    statCueSum = {'P': 0.0, 'R': 0.0, 'F': 0.0}
    statScopeSum = {'P': 0.0, 'R': 0.0, 'F': 0.0}

    for i in range(fromIdx, endIdx + 1):
        goldFileName = options.gold.replace('[*]', str(i))
        predFileName = options.pred.replace('[*]', str(i))
        print('processing test ', i, '...\t', goldFileName, ' ', predFileName)


        gold = readNegation(goldFileName)
        pred = readNegation(predFileName)

        cue, scope = calc(gold, pred, version='A')

        #print('scope:', scope)

        statCueSum = add(statCueSum, cue)
        statScopeSum = add(statScopeSum, scope)

    statCueSum = scale(statCueSum, 1.0 / (endIdx - fromIdx + 1))
    statScopeSum = scale(statScopeSum, 1.0 / (endIdx - fromIdx + 1))

    print('Version A:')
    print('cue:', statCueSum)
    print('scope:', statScopeSum)
    print()

    statCueSum = {'P': 0.0, 'R': 0.0, 'F': 0.0}
    statScopeSum = {'P': 0.0, 'R': 0.0, 'F': 0.0}

    for i in range(fromIdx, endIdx + 1):
        goldFileName = options.gold.replace('[*]', str(i))
        predFileName = options.pred.replace('[*]', str(i))
        print('processing test ', i, '...\t', goldFileName, ' ', predFileName)


        gold = readNegation(goldFileName)
        pred = readNegation(predFileName)

        cue, scope = calc(gold, pred, version='B')

        #print('scope:', scope)


        statCueSum = add(statCueSum, cue)
        statScopeSum = add(statScopeSum, scope)
        #print('scopesum:', statScopeSum)

    statCueSum = scale(statCueSum, 1.0 / (endIdx - fromIdx + 1))
    statScopeSum = scale(statScopeSum, 1.0 / (endIdx - fromIdx + 1))

    print('Version B:')
    print('cue:', statCueSum)
    print('scope:', statScopeSum)
    print()

