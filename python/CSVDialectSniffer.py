import csv
import sys
str = sys.argv[1]
dialect = csv.Sniffer().sniff(str)
print dialect.delimiter
print dialect.doublequote
print dialect.escapechar
#print dialect.lineterminator
print dialect.quotechar
print dialect.skipinitialspace
hasHeader = csv.Sniffer().has_header(str)
print hasHeader
