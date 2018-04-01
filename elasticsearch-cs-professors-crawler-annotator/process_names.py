import csv
import os

professors = []

with open('files/professor_list.txt', 'rb') as csvfile:
    csv_reader = csv.reader(csvfile, delimiter=',')
    for row in csv_reader:
        if row and len(row) > 0:
            professors.append(row[0])

for prof in professors:
    names = prof.split()
    
