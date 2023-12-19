#!/usr/bin/python

import re, sys
import json
import time

def current_milli_time():
    return round(time.time() * 1000)

def isPrime(n):
    # see http://www.noulakaz.net/weblog/2007/03/18/a-regular-expression-to-check-for-prime-numbers/
    return re.match(r'^1?$|^(11+?)\1+$', '1' * n) == None

start_timestamp_ms = current_milli_time()

N = int(sys.argv[1]) # number of primes wanted (from command-line)
M = 100              # upper-bound of search space
l = list()           # result list

while len(l) < N:
    l += filter(isPrime, range(M - 100, M)) # append prime element of [M - 100, M] to l
    M += 100                                # increment upper-bound

end_timestamp_ms = current_milli_time()
execution_time = end_timestamp_ms - start_timestamp_ms

result_dict = {
    "computer_number": N,
    "execution_time": execution_time
}

print(json.dumps(result_dict))