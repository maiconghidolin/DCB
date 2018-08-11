import sys

if __name__ == '__main__':
  f = open(sys.argv[1], 'r')

  l = sys.argv[1][11] + ','
  
  for line in f:
    try:
      l = l + str(int(line.split(':')[1])) + ','
    except:
      l = l + line.split('T')[1] + '\n'

  print (l)
  f = open('susu.csv', 'a')
  f.write(l)
