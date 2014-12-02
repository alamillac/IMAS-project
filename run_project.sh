#!/bin/bash

AGENTS_PATH=cat.urv.imas.agent

AGENTS="central:${AGENTS_PATH}.CentralAgent;"\
"coordinator:${AGENTS_PATH}.CoordinatorAgent;"\
"hospital:${AGENTS_PATH}.HospitalAgent;"\
"hospital_coordinator:${AGENTS_PATH}.HospitalCoordinator;"\
"ambulance:${AGENTS_PATH}.AmbulanceAgent;"\
"firemen:${AGENTS_PATH}.FiremenAgent;"\
"firemen_coordinator:${AGENTS_PATH}.FiremenCoordinator"

java -cp lib/jade.jar:classes jade.Boot -gui -agents "$AGENTS"
