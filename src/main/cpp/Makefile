CXX=g++-8

default: tiv

tiv.o: tiv.cpp CImg.h
	$(CXX) -std=c++17  -Wall -fpermissive -fexceptions -O2 -c tiv.cpp -o tiv.o

tiv : tiv.o
	$(CXX) tiv.o -o tiv -lstdc++fs -pthread -s

install: tiv
	cp tiv /usr/local/bin/tiv

clean:
	rm -f tiv tiv.o 
