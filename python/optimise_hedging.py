import sys
import numpy as np
from scipy.optimize import minimize

def duration(length, ytm, coupon_rate):
    return (1 + ytm)/ytm - (1 + ytm + length * (coupon_rate - ytm))/(coupon_rate * ((1 + ytm)**length - 1) + ytm)

def duration_index_linked(length, ytm, coupon_rate):
    realRedemptionYield = (1 + ytm)
    numerator = 0
    denominator = 0
    length = int(length)
    for i in range(1, length + 1):
        ithContribution = coupon_rate * (1/realRedemptionYield)**i
        numerator += ithContribution * i
        denominator += ithContribution
    numerator += (coupon_rate + 1) * (1/realRedemptionYield)**length * length
    denominator += (coupon_rate + 1) * (1/realRedemptionYield)**length
    return numerator/denominator

def duration_minimization(optimised_vals, curr_duration, target_duration, curr_portfolio_val, curr_liability_val, curr_yield, coupon_rate):
    # we are assuming here that the duration of new gilts is always the same as their length
    length = optimised_vals[0]
    amount = optimised_vals[1]
    # finds the duration of the proposed bond
    duration_bond_to_buy = 0
    duration_bond_to_buy = duration(length, curr_yield, coupon_rate)
    # redoes weighted duration calculation over portfolio
    return abs(amount / (amount + curr_portfolio_val) * duration_bond_to_buy + curr_portfolio_val / (amount + curr_portfolio_val) * curr_duration - target_duration)

def value_minimization(amount, curr_portfolio_val, curr_liability_val):
    return abs(amount + curr_portfolio_val - curr_liability_val)

curr_duration = float(sys.argv[1])
target_duration = float(sys.argv[2])
curr_portfolio_val = float(sys.argv[3])
curr_liability_val = float(sys.argv[4])
strategy = sys.argv[5]
curr_yield = float(sys.argv[6])
coupon_rate = float(sys.argv[7])
x0 = [target_duration, abs(curr_portfolio_val - curr_liability_val)]
if strategy == "DURATION_MATCHING":
    bounds = ((1.0, 50.0), (0.0, np.inf))
    optimization = minimize(duration_minimization, x0, args=(curr_duration, target_duration, curr_portfolio_val, curr_liability_val, curr_yield, coupon_rate), tol=1e-06,
                    constraints=({'type': 'eq', 'args': (curr_portfolio_val, curr_liability_val),
                                'fun': lambda inputs, curr_portfolio_val, curr_liability_val : inputs[1] + curr_portfolio_val - curr_liability_val},
                                ), method="SLSQP", bounds=bounds)

    min_vals = optimization.x
    length = min_vals[0]
    amount = min_vals[1]
    print(f'{length},{amount}')
elif strategy == "VALUE_MATCHING":
    bounds = ((0.0, np.inf),)
    optimization = minimize(value_minimization, x0[1], args=(curr_portfolio_val, curr_liability_val), bounds=bounds)
    print(f'{x0[0]},{optimization.x[0]}')
elif strategy == "SHORT_VALUE_MATCHING":
    bounds = ((0.0, np.inf),)
    optimization = minimize(value_minimization, x0[1], args=(curr_portfolio_val, curr_liability_val), bounds=bounds)
    print(f'{1.0},{optimization.x[0]}')
elif strategy == "INEQ_DURATION_MATCHING":
    bounds = ((1.0, 50.0), (0.0, np.inf))
    optimization = minimize(duration_minimization, x0, args=(curr_duration, target_duration, curr_portfolio_val, curr_liability_val, curr_yield, coupon_rate), tol=1e-06,
                    constraints=({'type': 'ineq', 'args': (curr_portfolio_val, curr_liability_val),
                                'fun': lambda inputs, curr_portfolio_val, curr_liability_val : inputs[1] + curr_portfolio_val - curr_liability_val},
                                ), method="SLSQP", bounds=bounds)

    min_vals = optimization.x
    length = min_vals[0]
    amount = min_vals[1]
    print(f'{length},{amount}')
