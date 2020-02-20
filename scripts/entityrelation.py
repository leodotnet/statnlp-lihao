from optparse import OptionParser
import random
import os.path
from os import listdir
from os.path import isfile, join
import pickle
import sys
import re


usage = "entityrelation.py --output outputpath"

parser = OptionParser(usage=usage)


parser.add_option("--dataset", type="string", help="dataset", default="ace2005", dest="dataset")
parser.add_option("--head", type="int", help="head", default=0, dest="head")

(options, args) = parser.parse_args()

datasets = {'ace2005':('train.data', 'dev.data', 'test.data'), 'ace2005-allan':('train.data', 'dev.data', 'test.data'),'ace2005-noOLhead':('train.data', 'dev.data', 'test.data')}
#os.makedirs(options.outputpath, exist_ok=True)

def extract_entity(entity_str):
    entity_pos, entity_type = entity_str.split()
    entity_pos = [int(item) for item in entity_pos.split(',')]
    return (entity_pos, entity_type)


def is_entity_overlapping_with(entity1, entity2):
    s1, e1, hs1, he1 = entity1[0]
    s2, e2, hs2, he2 = entity2[0]
    if options.head == 0:
        return (s2 >= s1 and s2 < e1) or (s1 >= s2 and s1 < e2)
    else:
        return (hs2 >= hs1 and hs2 < he1) or (hs1 >= hs2 and hs1 < he2)


def is_two_entity_same_start(entity1, entity2):
    s1, e1, hs1, he1 = entity1[0]
    s2, e2, hs2, he2 = entity2[0]
    if options.head == 0:
        return s1 == s2
    else:
        return hs1 == hs2

def is_entity_overlapping(entity, entities):
    for e in entities:
        if entity != e and is_entity_overlapping_with(entity, e):

            if is_two_entity_same_start(entity, e):
                return True, True
            else:
                return True, False

    return False, False


def is_entity_involve_relation(entity, entities, relations):
    entity_idx = entities.index(entity)
    for relation in relations:
        relation_type, entity1_idx, entity2_idx = relation
        if entity1_idx == entity_idx or entity2_idx == entity_idx:
            return True

    return False

def count_same_start(entities, size):
    start_count = [0 for i in range(size)]
    for e in entities:
        if options.head == 0:
            #print('e:',e, '\tstart_count:', start_count)
            start_count[e[0][0]] += 1
        else:
            start_count[e[0][2]] += 1

    return start_count

dataset = datasets[options.dataset]
for datafile in dataset:
    #print('datafile:', datafile)
    filepath = os.path.join('data//' + options.dataset, datafile)
    print('Reading ' + filepath)
    instances = []

    f = open(filepath, 'r', encoding='UTF-8')
    for line in f:
        sent = line
        postags = next(f)
        depidx = next(f)
        dep = next(f)
        entities_str = next(f).strip().split('|')
        relations_str = next(f).strip().split('|')

        entities = []
        relations = []

        #print(sent)
        if entities_str != ['']:
            for entity_str in entities_str:
                entity = extract_entity(entity_str)
                entities.append(entity)

        if relations_str != ['']:
            for relation_str in relations_str:
                #print('\t',relation_str)
                tmp = relation_str.split()
                relation_type, entity1, entity2 = tmp[0], tmp[1] + ' ' + tmp[2], tmp[3] + ' ' +  tmp[4]
                relation_type = relation_type.split('::')[0]
                #print('\t\t',entity1, '\t', entity2 )

                entity1 = extract_entity(entity1)
                entity2 = extract_entity(entity2)

                entity1_idx = entities.index(entity1)
                entity2_idx = entities.index(entity2)

                relation = (relation_type, entity1_idx, entity2_idx)
                relations.append(relation)


        inst = (entities, relations, sent)
        instances.append(inst)
        tmp = next(f)
    f.close()


    stats = {'#entity':0.0, '#relation':0.0, '#entity_ol':0.0, '#entity_ol_samestart':0.0,
             '#entity_in_relation':0.0, '#entity_ol_in_relation':0.0, '#entity_ol_samestart_in_relation':0.0,
                '#relation':0.0, '#relation_entity_ol':0.0, '#relation_entity_ol_samestart':0.0,
                '#samestart>=3':0.0,
             }
    for inst in instances:

        entities, relations, sent = inst

        stats['#entity'] += len(entities)
        stats['#relation'] += len(relations)

        for entity in entities:
            involve_realtion = is_entity_involve_relation(entity, entities, relations)
            entity_ol, same_start = is_entity_overlapping(entity, entities)


            if involve_realtion:
                stats['#entity_in_relation'] += 1

            if entity_ol:
                stats['#entity_ol'] += 1
                #print('inst:',inst)
                if same_start:
                    stats['#entity_ol_samestart'] += 1



            if involve_realtion and entity_ol:
                stats['#entity_ol_in_relation'] += 1

                if same_start:
                    stats['#entity_ol_samestart_in_relation'] += 1


        for relation in relations:
            stats['#relation'] += 1
            relation_type, entity1_idx, entity2_idx = relation
            entity1 = entities[entity1_idx]
            entity2 = entities[entity2_idx]

            entity_ol = is_entity_overlapping_with(entity1, entity2)
            entity_samestart = is_two_entity_same_start(entity1, entity2)

            if entity_ol:
                stats['#relation_entity_ol'] += 1

                if entity_samestart:
                    stats['#relation_entity_ol_samestart'] +=1



        start_count = count_same_start(entities, len(sent.split()))
        if len(start_count) > 0:
            max_start_count = max(start_count)
            if max_start_count >= 3:
                for count in start_count:
                    if count == max_start_count:
                        stats['#samestart>=3'] += 1








    print('Stats:')
    print(stats)
    print('ol/e:', stats['#entity_ol'] / stats['#entity'])
    print('ol_samestart/e:', stats['#entity_ol_samestart'] / stats['#entity'])
    print('r/e:', stats['#entity_in_relation'] / stats['#entity'])
    print('ol_r/e:', stats['#entity_ol_in_relation'] / stats['#entity'])
    print('ol_samestart_r/e:', stats['#entity_ol_samestart_in_relation'] / stats['#entity'])
    print('ol_r/re:', stats['#entity_ol_in_relation'] / stats['#entity_in_relation'])
    print('ol_samestart_r/re:', stats['#entity_ol_samestart_in_relation'] / stats['#entity_in_relation'])
    print()
    print('#relation:', stats['#relation'])
    print('#relation_entity_ol', stats['#relation_entity_ol'] / stats['#relation'])
    print('#relation_entity_ol_samestart', stats['#relation_entity_ol_samestart'] / stats['#relation'])
    print()
    print('#Inst:', len(instances))
    print('#samestart>=3', stats['#samestart>=3'])
    print()
    #break




