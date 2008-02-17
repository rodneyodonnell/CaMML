import math

########################################
## BASIC STATS NOT INCLUDES IN JYTHON ##
########################################
def sum(a):
	total = 0.0
	for x in a: total = total + x
	return total

def mean(a): return sum(a)/len(a)

def mul(a,b):
	x=[]
	for i in range(len(a)): x.append(1.0*a[i]*b[i])
	return x

#delta = 0.00000001
delta = 1.0E-100
def stdev(a):
	s,s2 = mean(a)*mean(a), mean(mul(a,a))
	if (s2 - s < 0) and (s2-s > -delta): return delta # Avoid problem with floating point errors.
	return delta+math.sqrt((s2-s)*len(a)/(len(a)-1)) # Change denominator from N to (N-1)

def sterr(a): return stdev(a)/math.sqrt(len(a))

# Elementwise subtraction
def sub(a,b):
	x=[]
	for i in range(len(a)):
		x.append( a[i]-b[i] )
	return x

# Divide each element in a by x
def div(a,x):
	xx=[]
	for i in range(len(a)):
		xx.append( a[i]/x )
	return xx
