PROGNAME = tiv

OBJECTS = tiv.o tiv_lib.o
  
CXX      ?= g++
CXXFLAGS ?= -O2
INSTALL  ?= install

# https://www.gnu.org/prep/standards/html_node/Directory-Variables.html#Directory-Variables
prefix      ?= /usr/local
exec_prefix ?= $(prefix)
bindir      ?= $(exec_prefix)/bin

override CXXFLAGS += -std=c++17 -Wall -fexceptions
override LDFLAGS  += -pthread
override LDLIBS   += -lpng

all: $(PROGNAME)

tiv_lib.o: tiv_lib.h

tiv.o: CImg.h tiv_lib.h 

$(PROGNAME): $(OBJECTS)
	$(CXX) $(LDFLAGS) $^ -o $@ $(LOADLIBES) $(LDLIBS)

install: all
	mkdir -p $(DESTDIR)$(bindir)
	$(INSTALL) $(PROGNAME) $(DESTDIR)$(bindir)/$(PROGNAME)

clean:
	$(RM) -f $(PROGNAME) *.o

.PHONY: all install clean