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
def duration_minimization(optimised_vals, curr_interest_rate, curr_inflation_rate, current_duration, target_duration):
    # we are assuming here that the duration of new gilts is always the same as their length
    length = optimised_vals[0]
    amount = optimised_vals[1]
    # we need this function to have two bits - duration match and cash flow match or something so that in the end they will actually be able to pay the liabilities
    duration = duration(length, curr_interest_rate, curr_interest_rate)
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
    # oh god thats going to be so many things
    # so so so many things
    return
