import re
import json
fJson= open("./esdata0.json","r")
fAnno = open("./entitySearchLabelResults","r")
JsonSet = {}
JsonLine = fJson.readlines()
AnnoLine = fAnno.readlines()

i = 0  # for iter
j = 0  # counter for label result
preDocNum = -1 # previou docnum
curDocNum = 0 # current docnum
while(i<len(JsonLine)):
	i += 1
	jData = json.loads(JsonLine[i])
	curDocNum = jData["physicalDoc"]
	if(preDocNum==curDocNum):
		i += 1
		continue
	# i += 1
	# jS = set()
	# jData = json.loads(JsonLine[i])
	# text = jData["text"]
	# for t in text.split(" "):
	# 	jS.add(t)
	# i += 1
	# SplitAnno = AnnoLine[j].split("#")
	# AnS = set()
	# for each in SplitAnno:
	# 	AnS.add(SplitAnno.split("_")[0])
	preDocNum = curDocNum
	fOut = open("./Output/"+str(curDocNum)+".txt", "w")
	q = AnnoLine[j].split("\t")[0]
	a = AnnoLine[j].split("\t")[4]
	fOut.write(q+"\n"+a)
	fOut.close()
	j += 1
	i += 1

fJson.close()
fAnno.close()


