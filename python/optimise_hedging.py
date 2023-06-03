import sys
from scipy.optimize import minimize

# We assume that the ytm is equal to the coupon rate, which will be the current interest rate for regular bonds and maybe the same for the index linked ones?
def duration(length, ytm, coupon_rate):
    return (1 + ytm)/ytm - (1 + ytm + length * (coupon_rate - ytm))/(coupon_rate * ((1 + ytm)**length - 1) + ytm)


# This is my function to minimize
# curr_portfolio_val does not include cash as part of the valuation criteria
def duration_minimization(optimised_vals, curr_interest_rate, curr_inflation_rate, curr_duration, target_duration, curr_portfolio_val, curr_liability_val, curr_cash):
    # we are assuming here that the duration of new gilts is always the same as their length
    length = optimised_vals[0]
    amount = optimised_vals[1]
    # finds the duration of the proposed bond
    duration_bond_to_buy = duration(length, curr_interest_rate, curr_interest_rate)
    # redoes weighted duration calculation over portfolio - I'm pretty sure this is actually wrong
    if (amount + curr_portfolio_val < 1e-06):
        return 0.0
    return abs(amount / (amount + curr_portfolio_val) * duration_bond_to_buy + curr_portfolio_val / (amount + curr_portfolio_val) * curr_duration - target_duration)

def value_minimization(amount, curr_portfolio_val, curr_liability_val):
    return abs(amount + curr_portfolio_val - curr_liability_val)

curr_interest_rate = float(sys.argv[1])
curr_inflation_rate = float(sys.argv[2])
curr_duration = float(sys.argv[3])
target_duration = float(sys.argv[4])
curr_portfolio_val = float(sys.argv[5])
curr_liability_val = float(sys.argv[6])
curr_cash = float(sys.argv[7])
strategy = sys.argv[8]
x0 = [target_duration, abs(curr_portfolio_val - curr_liability_val)]
if strategy == "DURATION_MATCHING":
    bounds = ((1.0, 50.0), (0.0, curr_cash))
    optimization = minimize(duration_minimization, x0, args=(curr_interest_rate, curr_inflation_rate, curr_duration, target_duration, curr_portfolio_val, curr_liability_val, curr_cash), tol=1e-06,
                    constraints=({'type': 'eq', 'args': (curr_portfolio_val, curr_liability_val),
                                'fun': lambda inputs, curr_portfolio_val, curr_liability_val : inputs[1] + curr_portfolio_val - curr_liability_val},
                                ), method="SLSQP", bounds=bounds)

    min_vals = optimization.x
    length = min_vals[0]
    amount = min_vals[1]
    print(f'{length},{amount}')
elif strategy == "VALUE_MATCHING":
    bounds = ((0.0, curr_cash),)
    optimization = minimize(value_minimization, x0[1], args=(curr_portfolio_val, curr_liability_val), bounds=bounds)
    print(f'{x0[0]},{optimization.x[0]}')
