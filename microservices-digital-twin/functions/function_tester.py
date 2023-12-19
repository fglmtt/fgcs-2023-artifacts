import time
import statistics
import re, sys

def isPrime(n):
    # see http://www.noulakaz.net/weblog/2007/03/18/a-regular-expression-to-check-for-prime-numbers/
    return re.match(r'^1?$|^(11+?)\1+$', '1' * n) == None

def compute_prime_number(target_number):
    N = target_number    # number of primes wanted (from command-line)
    M = 100              # upper-bound of search space
    l = list()           # result list

    while len(l) < N:
        l += filter(isPrime, range(M - 100, M)) # append prime element of [M - 100, M] to l
        M += 100                                # increment upper-bound

def main():

    n = 50
    execution_times = []

    for _ in range(n):

        print("Executing Function !")

        start_time = time.time()
        compute_prime_number(100)
        end_time = time.time()

        execution_time = (end_time - start_time) * 1000
        execution_times.append(execution_time)

    mean_time = statistics.mean(execution_times)
    variance_time = statistics.variance(execution_times)

    print(f"AVG Execution Time: {mean_time} ms")
    print(f"STD: {variance_time} ms^2")

if __name__ == "__main__":
    main()