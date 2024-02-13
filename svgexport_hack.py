import os
for file in os.listdir("./"):
    if file.endswith(".svg"):
        print('svgexport ' + file + ' ' + file[:-3] + 'png', end=';')
