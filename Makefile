RELEASE_VERSION ?= latest

SUBDIRS=replay/loader replay/init consumers/invoicing consumers/init consumers/orders consumers/shipping consumers/recommendations consumers/warehouse cdc/connect cdc/service cdc/watcher
DOCKER_TARGETS=docker_build docker_push docker_tag

all: $(SUBDIRS)
build: $(SUBDIRS)
clean: $(SUBDIRS)
$(DOCKER_TARGETS): $(SUBDIRS)

$(SUBDIRS):
	$(MAKE) -C $@ $(MAKECMDGOALS)

.PHONY: all $(SUBDIRS) $(DOCKER_TARGETS)
