import json

old_ontology_filename = "apple_ontology_old.txt"
new_ontology_filename = "apple_ontology.txt"

json_struct = {}

old_ontology_file = open(old_ontology_filename)
for line in old_ontology_file:
	components = line.strip().split("=")
	if components[0] not in json_struct:
		json_struct[components[0]] = []
	if len(components[1]) > 0:
		if components[1] not in json_struct:
			json_struct[components[1]] = []
		json_struct[components[1]].append(components[0])

outfile = open(new_ontology_filename, "w+")
outfile.write(json.dumps(json_struct))
outfile.close()