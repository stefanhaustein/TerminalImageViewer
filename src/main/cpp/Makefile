default: tiv

tiv.o: tiv.cpp CImg.h
	g++ -Wall -fexceptions -O2 -c tiv.cpp -o tiv.o

tiv : tiv.o
	g++ tiv.o -o tiv -pthread -s

install: tiv
	cp tiv /usr/local/bin/tiv

clean:
	rm -f tiv tiv.o 
