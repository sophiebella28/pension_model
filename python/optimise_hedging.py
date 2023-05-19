import sys
from scipy.optimize import minimize
# Ok time to code
# Steps for this script:
# We take in the duration of the portfolio
# And the target duration
# And something which specifies whether this is GE LE or EQ
# And we need to take in the current interest rate and current inflation rate
# And maybe the amount of cash that the fund currently has ?
# And then you can pick any number of years between 1 and 30 to buy the gilts length and the amount of money to buy of it
# These are the two values we will be optimising over and therefore be returning
# Ok so I might or might not need to do this per liability - I think I will?
# I could also (and this might be easier/nicer code/mean I can avoid calculating duration myself) get the individual values myself and do duration calc for portfolio here
# But I don't feel like doing that

# We assume that the ytm is equal to the coupon rate, which will be the current interest rate for regular bonds and maybe the same for the index linked ones?
def duration(length, ytm, coupon_rate):
    return (1 + ytm)/ytm - (1 + ytm + length * (coupon_rate - ytm))/(coupon_rate * ((1 + ytm)**length - 1) + ytm)

# This is my function to minimize
# curr_portfolio_val does not include cash as part of the valuation criteria
def duration_minimization(optimised_vals, curr_interest_rate, curr_inflation_rate, curr_duration, target_duration, curr_portfolio_val, curr_liability_val):
    # we are assuming here that the duration of new gilts is always the same as their length
    length = optimised_vals[0]
    amount = optimised_vals[1]
    # we need this function to have two bits - duration match and cash flow match or something so that in the end they will actually be able to pay the liabilities
    duration_bond_to_buy = duration(length, curr_interest_rate, curr_interest_rate)
    # ok now i need to find the right weight for the duration to match the duration of the liability
    # the problem with this is that I am now confused about how much money I should allocate to the fund initially - I need to give them enough that it will be possible for them to meet the liability
    # but not too much that they could just pay it off today
    # I guess discount at 0.05?
    # so we switch to a weight based approach
    # I can use this function to also optimise making enough money for the liability i guess
    # So, I need to add a pricing function to my model
    # EASY PEASY
    # And the weights are cash, index linked bonds and regular bonds
    # I think im better off keeping the portfolios as lists
    # I am however going to have to pass all of the portfolios to this function so that it can accurately calculate the updated weighted duration
    # Also technically all of the bonds are going to have different finish times
    # Unless I add the assumption that all bonds last until the due date of the liability - might have to do this - probably reasonable
    # So,,,, we still have an issue because all of the bonds have different interest rates
    # But I can't buy more bonds at the old interest rate price
    # So the weight of old interest rate bonds can only be decreased
    # So that isn't exactly the easiest thing to deal with
    # So constraint - value of current portfolio is the same as the value of the liability and the duration is the same
    # But then..... OK no weights. Just adjust the length of the bond to fit the duration needed and the value to fit the value needed
    # So....

    # So I compute the current duration without the cash value and then I constrain amount by the amount of cash currently held?
    # I also have the constraint that amount + current portfolio value = current liability value
    # Not sure how cash factors into all of this
    # because if i include it in the current portfolio then
    # It sort of depends on whether or not I think it should be self financing
    # Which I think I can make an argument on either way
    # And as such I should just do whatever seems easier
    # Which is that cash has 0 duration
    # because it doesnt depend on the underlying ????
    return abs(amount / (amount + curr_portfolio_val) * duration_bond_to_buy + curr_portfolio_val / (amount + curr_portfolio_val) * curr_duration - target_duration)

# New problemmmmmm - I want the lengths to be integers
# but they don't want to be integers
# so it might be easier to only allow purchase of 1 2 5 10 30 year gilts
# dont know how to do this though
# again maybe i want to put different weights into each of the 1 2 5 10 30 years
# it could hypothetically just calculate the duration for each of those then take in some weights to multiply each amount by
# and then optimise weights and total portfolio
# ok ive changed my mind for now it's fine to have non integer valued years

def test_func(x, *args):
    return x[0] * x[1]

x0 = [1.0,100]
curr_interest_rate = float(sys.argv[1])
curr_inflation_rate = float(sys.argv[2])
curr_duration = float(sys.argv[3])
target_duration = float(sys.argv[4])
curr_portfolio_val = float(sys.argv[5])
curr_liability_val = float(sys.argv[6])
optimization = minimize(duration_minimization, x0, args=(curr_interest_rate, curr_inflation_rate, curr_duration, target_duration, curr_portfolio_val, curr_liability_val), tol=1e-06,
                constraints=({'type': 'eq', 'args': (curr_portfolio_val, curr_liability_val),
                               'fun': lambda inputs, curr_portfolio_val, curr_liability_val : inputs[1] + curr_portfolio_val - curr_liability_val},
                             {'type': 'ineq',
                              'fun': lambda inputs : inputs[1]},
                             {'type': 'ineq',
                              'fun': lambda inputs : inputs[0] - 1.0},
                             {'type': 'ineq',
                              'fun': lambda inputs : -(inputs[0] - 30.0)}
                            ))

min_vals = optimization.x
length = min_vals[0]
amount = min_vals[1]
print(f'{length},{amount}')
