default: tiv

tiv.o: tiv.cpp CImg.h
	g++ -std=c++17  -Wall -fpermissive -fexceptions -O2 -c tiv.cpp -o tiv.o

tiv : tiv.o
	g++ tiv.o -o tiv -lstdc++fs -pthread -s

install: tiv
	cp tiv /usr/local/bin/tiv

clean:
	rm -f tiv tiv.o 
