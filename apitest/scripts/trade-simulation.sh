#! /bin/bash

# Runs fiat <-> btc trading scenarios using the API CLI with a local regtest bitcoin node.
#
# A country code argument is used to create a country based face to face payment account for the simulated
# trade, and the maker's face to face payment account's currency code is used when creating the offer.
#
# Prerequisites:
#
#  - Linux or OSX with bash, Java 10, or Java 11-12 (JDK language compatibility 10), and bitcoin-core (v0.19, v0.20).
#
#  - Bisq must be fully built with apitest dao setup files installed.
#    Build command:  `./gradlew clean build :apitest:installDaoSetup`
#
#  - All supporting nodes must be run locally, in dev/dao/regtest mode:
#           bitcoind, seednode, arbdaemon, alicedaemon, bobdaemon
#
#    These should be run using the apitest harness.  From the root project dir, run:
#    `$ ./bisq-apitest --apiPassword=xyz --supportingApps=bitcoind,seednode,arbdaemon,alicedaemon,bobdaemon --shutdownAfterTests=false`
#
#  - Only regtest btc can be bought or sold with the test payment account.
#
# Usage:
#
#  This script must be run from the root of the project, e.g.:
#
#     `$ apitest/scripts/trade-simulation.sh -d buy -c fr -m 3.00 -a 0.125`
#
#  Script options:  -d <direction> -c <country-code> -m <mkt-price-margin(%)> - f <fixed-price> -a <amount(btc)>
#
# Examples:
#
#    Create a buy/eur offer to buy 0.125 btc at a mkt-price-margin of 0%, using an Italy face to face payment account:
#
#       `$ apitest/scripts/trade-simulation.sh -d buy -c it -m 0.00 -a 0.125`
#
#    Create a sell/eur offer to sell 0.125 btc at a fixed-price of 38,000 euros, using a France face to face
#    payment account:
#
#       `$ apitest/scripts/trade-simulation.sh -d sell -c fr -f 38000 -a 0.125`

export APP_BASE_NAME=$(basename "$0")
export APP_HOME=$(pwd -P)
export APITEST_SCRIPTS_HOME="${APP_HOME}/apitest/scripts"

# Source the env and some helper functions.
. "${APITEST_SCRIPTS_HOME}/trade-simulation-env.sh"
. "${APITEST_SCRIPTS_HOME}/trade-simulation-utils.sh"

checksetup
parseopts "$@"

printdate "Started ${APP_BASE_NAME} with parameters:"
printscriptparams
printbreak

registerdisputeagents

printdate "Alice looks for the ID of the face to face payment account method (Bob will use same payment method)."
CMD="${CLI_BASE} --port=${ALICE_PORT} getpaymentmethods"
printdate "ALICE CLI: ${CMD}"
getpaymentaccountmethods "$CMD"
printbreak

printdate "Alice uses the F2F payment method id to create a face to face payment account in country ${COUNTRY_CODE}."
CMD="${CLI_BASE} --port=${ALICE_PORT} getpaymentacctform --payment-method-id=F2F"
printdate "ALICE CLI: ${CMD}"
getpaymentaccountform "$CMD"
printbreak

printdate "Bob & Alice edit their ${COUNTRY_CODE} payment account forms, and renames them to ${F2F_ACCT_FORM}"
editpaymentaccountform "$COUNTRY_CODE"
cat "${APITEST_SCRIPTS_HOME}/${F2F_ACCT_FORM}"

# Remove the autogenerated json template because we are going to use one created by a python script in the next step.
CMD="rm -v ${APP_HOME}/f2f_*.json"
DELETE_JSON_TEMPLATE=$(${CMD})
echo "$DELETE_JSON_TEMPLATE"
printbreak

printdate "Bob and Alice create their face to face ${COUNTRY_CODE} payment accounts."
CMD="${CLI_BASE} --port=${BOB_PORT} createpaymentacct --payment-account-form=${APITEST_SCRIPTS_HOME}/${F2F_ACCT_FORM}"
printdate "BOB CLI: ${CMD}"
CMD_OUTPUT=$(createpaymentacct "${CMD}")
echo "${CMD_OUTPUT}"
BOB_ACCT_ID=$(getnewpaymentacctid "${CMD_OUTPUT}")
BOB_ACCT_CURRENCY_CODE=$(getnewpaymentacctcurrency "${CMD_OUTPUT}")
printdate "BOB F2F payment-account-id = ${BOB_ACCT_ID}, currency-code = ${BOB_ACCT_CURRENCY_CODE}."
printbreak

CMD="${CLI_BASE} --port=${ALICE_PORT} createpaymentacct --payment-account-form=${APITEST_SCRIPTS_HOME}/${F2F_ACCT_FORM}"
printdate "ALICE CLI: ${CMD}"
CMD_OUTPUT=$(createpaymentacct "${CMD}")
echo "${CMD_OUTPUT}"
ALICE_ACCT_ID=$(getnewpaymentacctid "${CMD_OUTPUT}")
ALICE_ACCT_CURRENCY_CODE=$(getnewpaymentacctcurrency "${CMD_OUTPUT}")
printdate "ALICE F2F payment-account-id = ${ALICE_ACCT_ID}, currency-code = ${ALICE_ACCT_CURRENCY_CODE}."
printbreak

printdate "ALICE ${ALICE_ROLE}:  Creating ${DIRECTION} ${ALICE_ACCT_CURRENCY_CODE} offer with payment acct ${ALICE_ACCT_ID}."
CMD="$CLI_BASE --port=${ALICE_PORT} createoffer"
CMD+=" --payment-account=${ALICE_ACCT_ID}"
CMD+=" --direction=${DIRECTION}"
CMD+=" --currency-code=${ALICE_ACCT_CURRENCY_CODE}"
CMD+=" --amount=${AMOUNT}"
if [ -z "${MKT_PRICE_MARGIN}" ]; then
    CMD+=" --fixed-price=${FIXED_PRICE}"
else
    CMD+=" --market-price-margin=${MKT_PRICE_MARGIN}"
fi
CMD+=" --security-deposit=15.0"
CMD+=" --fee-currency=BSQ"
printdate "ALICE CLI: ${CMD}"
OFFER_ID=$(createoffer "${CMD}")
exitoncommandalert $?
printdate "ALICE ${ALICE_ROLE}:  Created offer with id: ${OFFER_ID}."
printbreak
sleeptraced 10

# Generate some btc blocks.
printdate "Generating btc blocks after publishing Alice's offer."
genbtcblocks 3 5
printbreak
sleeptraced 10

# List offers.
printdate "BOB ${BOB_ROLE}:  Looking at ${DIRECTION} ${BOB_ACCT_CURRENCY_CODE} offers."
CMD="$CLI_BASE --port=${BOB_PORT} getoffers --direction=${DIRECTION} --currency-code=${BOB_ACCT_CURRENCY_CODE}"
printdate "BOB CLI: ${CMD}"
OFFERS=$($CMD)
exitoncommandalert $?
echo "${OFFERS}"
printbreak
sleeptraced 3

# Take offer.
printdate "BOB ${BOB_ROLE}:  Taking offer ${OFFER_ID} with payment acct ${BOB_ACCT_ID}."
CMD="$CLI_BASE --port=${BOB_PORT} takeoffer --offer-id=${OFFER_ID} --payment-account=${BOB_ACCT_ID} --fee-currency=bsq"
printdate "BOB CLI: ${CMD}"
TRADE=$($CMD)
commandalert $? "Could not take offer."

echo "${TRADE}"
printbreak
sleeptraced 10

# Generating some btc blocks
printdate "Generating btc blocks after Bob takes Alice's offer."
genbtcblocks 3 3
printbreak
sleeptraced 6

# Send payment sent and received messages.
if [ "${DIRECTION}" = "BUY" ]
then
    PAYER="ALICE ${ALICE_ROLE}"
    PAYER_PORT=${ALICE_PORT}
    PAYER_CLI="ALICE CLI"
    PAYEE="BOB ${BOB_ROLE}"
    PAYEE_PORT=${BOB_PORT}
    PAYEE_CLI="BOB CLI"
else
    PAYER="BOB ${BOB_ROLE}"
    PAYER_PORT=${BOB_PORT}
    PAYER_CLI="BOB CLI"
    PAYEE="ALICE ${ALICE_ROLE}"
    PAYEE_PORT=${ALICE_PORT}
    PAYEE_CLI="ALICE CLI"
fi

# Confirm payment started.
printdate "${PAYER}:  Sending fiat payment sent msg."
CMD="$CLI_BASE --port=${PAYER_PORT} confirmpaymentstarted --trade-id=${OFFER_ID}"
printdate "${PAYER_CLI}: ${CMD}"
SENT_MSG=$($CMD)
commandalert $? "Could not send confirmpaymentstarted message."

printdate "${SENT_MSG}"
printbreak

sleeptraced 2
printdate "Generating btc blocks after fiat payment sent msg."
genbtcblocks 3 5
sleeptraced 2

# Confirm payment received.
printdate "${PAYEE}:  Sending fiat payment received msg."
CMD="$CLI_BASE --port=${PAYEE_PORT} confirmpaymentreceived --trade-id=${OFFER_ID}"
printdate "${PAYEE_CLI}: ${CMD}"
RCVD_MSG=$($CMD)
commandalert $? "Could not send confirmpaymentreceived message."
printdate "${RCVD_MSG}"
printbreak
sleeptraced 4

# Generate some btc blocks
printdate "Generating btc blocks after fiat transfer."
genbtcblocks 3 5
printbreak
sleeptraced 3

# Complete the trade on the seller side.
if [ "${DIRECTION}" = "BUY" ]
then
    printdate "BOB ${BOB_ROLE}:  Closing trade by keeping funds in Bisq wallet."
    CMD="$CLI_BASE --port=${BOB_PORT} keepfunds --trade-id=${OFFER_ID}"
    printdate "BOB CLI: ${CMD}"
else
    printdate "ALICE (taker):  Closing trade by keeping funds in Bisq wallet."
    CMD="$CLI_BASE --port=${ALICE_PORT} keepfunds --trade-id=${OFFER_ID}"
    printdate "ALICE CLI: ${CMD}"
fi
KEEP_FUNDS_MSG=$($CMD)
commandalert $? "Could close trade with keepfunds command."
printdate "${KEEP_FUNDS_MSG}"
sleeptraced 5
printbreak

# Get balances after trade completion.
printdate "Bob & Alice's balances after trade:"
printdate  "ALICE CLI:"
printbalances "$ALICE_PORT"
printbreak
printdate "BOB CLI:"
printbalances "$BOB_PORT"
printbreak

exit 0
