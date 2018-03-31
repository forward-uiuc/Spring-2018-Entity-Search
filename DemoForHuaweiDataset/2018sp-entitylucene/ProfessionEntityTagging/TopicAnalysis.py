import metapy
import os
import operator
import json

professionsSLM = {}
professionsSortedSLM = {}
frequencies = {}
minFreq = 9223372036854775807

professionOrder = os.listdir("Professions/")


##
#
# gets a statistical language model for a profession
#
##
def get_profession_slm(filename, useFreq):
    file = open("Professions/" + filename + ".txt", "r")
    contents = file.read()
    tokenizer = metapy.analyzers.ICUTokenizer(suppress_tags=True)
    tokenizer = metapy.analyzers.Porter2Filter(tokenizer)
    tokenizer = metapy.analyzers.ListFilter(tokenizer, "lemur-stopwords.txt", metapy.analyzers.ListFilter.Type.Reject)
    tokenizer = metapy.analyzers.LengthFilter(tokenizer, min=4, max=30)
    tokenizer = metapy.analyzers.LowercaseFilter(tokenizer)
    tokenizer.set_content(contents)
    try:
        token_frequencies = {}
        num_tokens = 0
        for token in tokenizer:
            if token in token_frequencies:
                token_frequencies[token] += 1
            else:
                token_frequencies[token] = 1
                num_tokens += 1
        for token in list(token_frequencies.keys()):
            if not useFreq:
                token_frequencies[token] /= num_tokens
            else:
                if token in frequencies:
                    token_frequencies[token] /= frequencies[token]
                else:
                    token_frequencies[token] /= minFreq

        professionsSLM[filename] = token_frequencies
        professionsSortedSLM[filename] = sorted(token_frequencies.items(), key=operator.itemgetter(1))
        professionsSortedSLM[filename].reverse()
    except:
        print("Professions/" + filename + ".txt failed!")


##
#
# Prints the frequency of the supplied word for each profession
#
##
def print_scores_for_word(word):
    tokenizer = metapy.analyzers.ICUTokenizer(suppress_tags=True)
    tokenizer = metapy.analyzers.Porter2Filter(tokenizer)
    tokenizer.set_content(word)
    tokWord = word
    for tok in tokenizer:
        tokWord = tok
    for profession_name in professionOrder:
        slm = professionsSLM[profession_name]
        if tokWord in slm:
            print(slm[tokWord])
        else:
            print(0)


##
#
# Prints the top words from each profession
#
##
def get_top_word_output(num_words):
    for profession_name in professionsSortedSLM.keys():
        print()
        print(profession_name)
        slm = professionsSortedSLM[profession_name]
        if num_words > 0:
            for i in range(0, num_words):
                print(slm[i][0] + "\t" + str(slm[i][1]))
        else:
            for i in range(0, len(slm)):
                print(slm[i][0] + "\t" + str(slm[i][1]))


def get_all_words_output(output_dir):
    for profession_name in professionsSortedSLM.keys():
        outfile = open(output_dir + "/" + profession_name + ".json", "w")
        slm = professionsSLM[profession_name]
        outfile.write(json.dumps(slm))
        outfile.close()

def get_single_file_output(output_dir):
    composite_slm = {}
    for profession_name in professionsSortedSLM.keys():
        slm = professionsSLM[profession_name]
        for item in slm:
            if item not in composite_slm:
                composite_slm[item] = {}
            composite_slm[item][profession_name] = slm[item]
    outfile = open(output_dir + "/" + "composite_slm" + ".json", "w")
    outfile.write(json.dumps(composite_slm))
    outfile.close()


##
#
# Reads in top 5000 English word frequencies
#
##
def get_word_frequs():
    global minFreq
    tokenizer = metapy.analyzers.ICUTokenizer(suppress_tags=True)
    tokenizer = metapy.analyzers.Porter2Filter(tokenizer)
    tokenizer = metapy.analyzers.ListFilter(tokenizer, "lemur-stopwords.txt", metapy.analyzers.ListFilter.Type.Reject)
    tokenizer = metapy.analyzers.LengthFilter(tokenizer, min=4, max=30)
    file = open("word_freqs.txt", "r")
    for line in file:
        contents = line.split("\t")
        tokenizer.set_content(contents[1])
        for tok in tokenizer:
            frequencies[tok] = int(contents[3])
            if int(contents[3]) < minFreq:
                minFreq = int(contents[3])
    minFreq = minFreq/2.0


get_word_frequs()
professions = os.listdir("Professions/")

for profession in professions:
    if profession != ".DS_Store":
        get_profession_slm(profession.replace(".txt", ""), False)

get_single_file_output("COMP")
#get_all_words_output("SLMs")
