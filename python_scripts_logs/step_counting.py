#loading and plotting

import math
from datetime import datetime
from matplotlib import pyplot as plt
from matplotlib import dates as dt

#logfile = open('log2 (another copy).txt' , 'r')
logfile = open('log2_4.txt' , 'r')
timedata = []
logdata = []
num_examples = 0
i = 0
for logline in logfile :
	#if i % 9 == 0 :

	log = logline.strip().split()

	timestamp = datetime.strptime(log[1]+"000" , "%Y.%m.%d.%H.%M.%S.%f")
	timedata.append(timestamp)

	mag = 0
	for i in xrange(2 , 5) :
		mag += ((float(log[i]))**2)

	logdata.append(math.sqrt(mag))
	if (i < 100) :
		num_examples += 1

	#i += 1
start = 0
end = 500
times = dt.date2num(timedata)
plt.plot(xrange(start , end) , logdata[start:end])

#definitions

S_init = 2
S_peak = 1
S_intmd = 0
S_valley = -1
n_p = 0
#data structure

class Storage:
	def __init__(self , mypeakvalwindow):
		self.peakhead = 0
		self.valleyhead = 0

		self.peakinserted = 0
		self.valleyinserted = 0

		self.peakvalwindow = mypeakvalwindow

		self.peaks = []
		self.valleys = []

		self.peaksum = 0.0
		self.valleysum = 0.0

		self.peaksumsquares = 0.0
		self.valleysumsquares = 0.0

	def insertPeak(self , time) :
		global n_p
		newtime = time - n_p
		#print "insertPeak n_p : " + str(n_p)
		#print newtime
		self.peaksum += newtime
		self.peaksumsquares += (newtime*newtime)

		if(self.peakinserted >= self.peakvalwindow):
			toSubtract = self.peaks[self.peakhead]
			self.peaksum -= toSubtract
			self.peaksumsquares -= (toSubtract * toSubtract)
			self.peaks[self.peakhead] = newtime
		else :
			self.peaks.append(newtime)

		self.peakhead = (self.peakhead + 1) % self.peakvalwindow
		self.peakinserted += 1

		#print self.peaksum
		#print self.peaksumsquares

	def insertValley(self , time):
		global n_v
		newtime = time - n_v
		self.valleysum += newtime
		self.valleysumsquares += (newtime*newtime)

		if(self.valleyinserted >= self.peakvalwindow):
			#print self.valleyinserted
			#print self.valleyhead
			#print len(self.valleys)
			toSubtract = self.valleys[self.valleyhead]
			self.valleysum -= toSubtract
			self.valleysumsquares -= (toSubtract * toSubtract)
			self.valleys[self.valleyhead] = newtime
		else :
			self.valleys.append(newtime)

		self.valleyhead = (self.valleyhead + 1) % self.peakvalwindow
		self.valleyinserted += 1

#initializations

count = 0

n = 1
n_v = 0
a_p = 9.615380952381
a_v = 9.615380952381

mu_a = 9.615380952381
mu_old = 9.615380952381
sigma_a = 0.0

mu_p = float("inf")
mu_v = float("inf")
sigma_p = 0.0
sigma_v = 0.0

Th_p = float("inf")
Th_v = float("inf")

K = 25
M = 10
alpha = 1.06763
beta = 1.5

S_n_minus_1 = S_init
S_n = S_init

mystore = Storage(M)

magsum = 0.0
magsumsquares = 0.0

mu_list_times = []
mu_list_data = []
threshlist_times = []
threshlist_upper_data = []
threshlist_lower_data = []

peak_times = []
peak_list = []

valley_times = []
valley_list = []

last_peaks = []
last_valleys = []

sp_time = 0

#algorithm 

def DetectCandidate(a_plus_1 , a , a_minus_1 , mu , sigma , cons) :
	global S_intmd , S_peak , S_valley , threshlist_times , threshlist_upper_data , threshlist_lower_data
	S_c = S_intmd
	#threshlist_times.append(times[n])
	threshlist_times.append(n)
	threshlist_upper_data.append(mu + (sigma / cons))
	threshlist_lower_data.append(mu - (sigma / cons))
	
	if a > max(a_minus_1 , a_plus_1 , mu + (sigma / cons)) :
		S_c = S_peak
	elif a < min(a_minus_1 , a_plus_1 , mu - (sigma / cons)) :
		S_c = S_valley

	return S_c

def UpdatePeak(axlr , time) :
	global n_p , mu_p , sigma_p , Th_p , a_p , peak_list , peak_times , M , beta
	#print time
	mystore.insertPeak(time)
	#print "UpdatePeak time : " + str(time)
	n_p = time
	#print "UpdatePeak n_p : " + str(n_p)
	denom = M
	if ((mystore.peakinserted) < M) :
		denom = mystore.peakinserted
	mu_p = mystore.peaksum / denom
	#print str((mystore.peaksumsquares / denom) - (mu_p*mu_p))
	sigma_p = math.sqrt((mystore.peaksumsquares / denom) - (mu_p*mu_p))
	Th_p = mu_p - (sigma_p/beta)
	a_p = axlr
	peak_list.append(axlr)
	#peak_times.append(times[time])
	peak_times.append(time)

def UpdateValley(axlr , time):
	global n_v , mu_v , sigma_v , Th_v , a_v , valley_list , valley_times , M , beta
	mystore.insertValley(time)
	denom = M
	if ((mystore.valleyinserted) < M) :
		denom = mystore.valleyinserted
	mu_v = mystore.valleysum / denom
	sigma_v = math.sqrt((mystore.valleysumsquares / denom) - (mu_v*mu_v))
	Th_v = mu_v - (sigma_v/beta)
	n_v = time
	a_v = axlr
	valley_list.append(axlr)
	#valley_times.append(times[time])
	valley_times.append(time)


for example in xrange(start , end) :

	a_n_plus_1 = logdata[example]
	a_n = logdata[example-1]
	a_n_minus_1 = logdata[example-2]

	S_c = DetectCandidate(a_n_plus_1 , a_n , a_n_minus_1 , mu_a , sigma_a , alpha)
	
	if(mu_a < 50.0):
		#mu_list_times.append(times[n])
		mu_list_times.append(n)
		mu_list_data.append(mu_a)

	if S_c == S_peak :
		if S_n_minus_1 == S_init :
			S_n = S_peak
			UpdatePeak(a_n , n)
		elif S_n_minus_1 == S_valley and n - n_p > Th_p :
			S_n = S_peak
			UpdatePeak(a_n , n)
			#mu_old = mu_a
			mu_a = (a_p + a_v) / 2
			#if (mu_a - mu_old > 10.0):
			#	sp_time = n
		elif S_n_minus_1 == S_peak and n - n_p <= Th_p and a_n > a_p :
			UpdatePeak(a_n , n)


	elif S_c == S_valley :
		#print str(S_n_minus_1) + " " + str(n - n_v > Th_v) + " " + str(a_n < a_v)
		if (S_n_minus_1 == S_peak or S_n_minus_1 == S_intmd or S_n_minus_1 == S_valley) and n - n_v > Th_v :
			# or S_n_minus_1 == S_intmd
			S_n = S_valley
			UpdateValley(a_n , n)
			count += 1
			#mu_old = mu_a
			mu_a = (a_p + a_n) / 2
			#if (mu_a - mu_old > 10.0):
			#	sp_time = n
		elif (S_n_minus_1 == S_valley or S_n_minus_1 == S_peak or S_n_minus_1 == S_intmd) and n - n_v <= Th_v and a_n < a_v :
			#or S_n_minus_1 == S_peak or S_n_minus_1 == S_intmd
			UpdateValley(a_n , n)

	last_peaks.append(a_p)
	last_valleys.append(a_v)

	# update sigma_a
	magsum += a_n_plus_1
	magsumsquares += (a_n_plus_1**2)
	if (n >= K) :
		magsum -= logdata[example-K]
		magsumsquares -= ((logdata[example-K])**2)
	
	denom = K
	if (n < K) :
		denom = n
	sigma_a = math.sqrt((magsumsquares / denom) - ((magsum / denom)**2))
	
	S_n_minus_1 = S_n
	n += 1

print count

#plt.plot(mu_list_times , mu_list_data , c='b')
plt.plot(threshlist_times , threshlist_lower_data , c = 'r')
plt.plot(threshlist_times , threshlist_upper_data , c = 'g')
plt.scatter(peak_times , peak_list , marker='*')
plt.scatter(valley_times , valley_list , marker='o')
#plt.scatter([times[sp_time]] , [logdata[sp_time]] , marker='o')
#plt.plot(mu_list_times , last_peaks , c='g')
#plt.plot(mu_list_times , last_valleys , c='r')
plt.show()