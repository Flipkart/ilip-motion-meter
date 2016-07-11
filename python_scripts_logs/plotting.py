import math
from datetime import datetime
from matplotlib import pyplot as plt
from matplotlib import dates as dt

logfile = open('log2 (copy).txt' , 'r')
timedata = []
logdata = []
i = 0
for logline in logfile :
	if i % 9 == 0 :
		log = logline.strip().split()

		# 2016.06.02.16.00.00.867

		timestamp = datetime.strptime(log[1]+"000" , "%Y.%m.%d.%H.%M.%S.%f")
		timedata.append(timestamp)

		mag = 0
		for i in xrange(2 , 5) :
			mag += ((float(log[i]))**2)

		logdata.append(math.sqrt(mag))

	i += 1

times = dt.date2num(timedata)
plt.plot(times , logdata)
plt.show()