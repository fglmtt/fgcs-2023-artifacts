#!/bin/sh

# delete routes
fission route delete --name physical-event-handler
fission route delete --name shadow-handler
fission route delete --name twin-handler
fission route delete --name digital-event-handler

# delete functions
fission fn delete --name physical-event-handler
fission fn delete --name shadow-handler
fission fn delete --name twin-handler
fission fn delete --name digital-event-handler

# delete packages
fission pkg delete --name physical-event-handler-pkg
fission pkg delete --name shadow-handler-pkg
fission pkg delete --name twin-handler-pkg
