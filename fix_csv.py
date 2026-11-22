import csv
import re

filepath = r'C:\Users\filka\Downloads\PS\pesEditor\portugal2004.csv'

with open(filepath, 'r', encoding='utf-8') as f:
    text = f.read()

# Fix the missing newline
if '"3""32878"' in text:
    text = text.replace('"3""32878"', '"3"\n"32878"')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(text)

with open(filepath, 'r', encoding='utf-8') as f:
    reader = csv.reader(f)
    rows = list(reader)

header_len = len(rows[0])
for i in range(1, len(rows)):
    row = rows[i]
    if len(row) == header_len - 1:
        row.insert(-2, '0')

exceptions = ['moreira', 'rui jorge', 'nuno valente', 'petit', 'ricardo']
for i in range(1, len(rows)):
    clean_name = rows[i][1].replace('"', '').strip().lower()

    is_exp = any(ex in clean_name for ex in exceptions)

    if not is_exp:
        # Clear faces
        for j in range(26, 32): # FACE to HAIR
            rows[i][j] = ''
    else:
        if 'ricardo' in clean_name:
            rows[i][25] = '0' # GROWTH
            rows[i][28] = '1' # SKIN
            rows[i][29] = '0' # HEAD LENGTH
            rows[i][30] = '0' # HEAD WIDTH
            rows[i][31] = '102' # HAIR
            rows[i][-2] = '' # CLUB TEAM
            rows[i][-1] = '0' # CLUB NUMBER
        if 'moreira' in clean_name:
            rows[i][28] = '1'
            rows[i][29] = '0'
            rows[i][30] = '0'
            rows[i][31] = '102'
            rows[i][-2] = ''
            rows[i][-1] = '0'

with open(filepath, 'w', encoding='utf-8', newline='') as f:
    writer = csv.writer(f, quoting=csv.QUOTE_ALL)
    writer.writerows(rows)

print('CSV successfully fixed.')

