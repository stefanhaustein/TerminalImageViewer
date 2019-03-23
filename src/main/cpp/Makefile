# set CXX to g++ if not set
CXX ?= g++

# append necessary arguments
override CPPFLAGS += -std=c++17 -Wall -fpermissive -fexceptions -O2
override LDFLAGS  += -lstdc++fs -pthread -s

all: tiv

tiv.o: tiv.cpp CImg.h
	$(CXX) $(CPPFLAGS) -c tiv.cpp -o $@

tiv : tiv.o
	$(CXX) $^ -o $@ $(LDFLAGS)

.PHONY: all install clean
install: all
	test -d $(DESTDIR)/usr/local/bin || mkdir -p $(DESTDIR)/usr/local/bin
	cp tiv $(DESTDIR)/usr/local/bin/tiv

clean:
	rm -f tiv tiv.o
