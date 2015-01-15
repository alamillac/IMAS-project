#!/bin/bash

AGENTS_PATH=cat.urv.imas.agent

AGENTS="central:${AGENTS_PATH}.CentralAgent;"\
"coord:${AGENTS_PATH}.CoordinatorAgent;"\
"firemenCoor:${AGENTS_PATH}.FiremenCoordinator;"\
"hospitalCoor:${AGENTS_PATH}.HospitalCoordinator"

java -cp lib/jade.jar:classes jade.Boot -gui -agents "$AGENTS"
