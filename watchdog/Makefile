CC = gcc
CFLAGS = -pedantic-errors -Wall -Wextra
DEBUG_FLAGS = -g
RELEASE_FLAGS = -DNDEBUG -O3
OBJFLAGS = -fPIC -c

INCLUDE = -I./ -I../scheduler/include/ -I../../ds/include/

UID_C = ../scheduler/src/uid.c
UID_H = ../scheduler/include/uid.h

TASK_C = ../scheduler/src/task.c
TASK_H = ../scheduler/include/task.h

SCH_C = ../scheduler/src/sched.c
SCH_H = ../scheduler/include/sched.h

PQ_C = ~/git/ds/src/pq_heap.c
PQ_H = ~/git/ds/include/pq.h

HEAP_C = ~/git/ds/src/heap.c
HEAP_H = ~/git/ds/include/heap.h

VECTOR_C = ~/git/ds/src/d_vector_by_ref.c
VECTOR_H = ~/git/ds/include/dynamic_vector.h

WD_C = wd.c
WD_H = wd.h
WD_MAIN = wd_main.c
USER_C = user.c

ifeq ($(MODE),debug)
	CFLAGS += $(DEBUG_FLAGS)
else
	CFLAGS += $(RELEASE_FLAGS)
endif

all: libwd.so wd_main user clean

libwd.so: task.o uid.o scheduler.o heap.o pq.o vector.o wd.o pq.o
	$(CC) $(CFLAGS) wd.o task.o uid.o scheduler.o heap.o pq.o vector.o -shared -o libwd.so

task.o: $(TASK_C) $(TASK_H)
	$(CC) $(CFLAGS) $(OBJFLAGS) $(INCLUDE) $(TASK_C) -o task.o

wd.o: $(WD_C) $(WD_H)
	$(CC) $(CFLAGS) $(OBJFLAGS) $(INCLUDE) $(WD_C) -o wd.o

uid.o: $(UID_C) $(UID_H)
	$(CC) $(CFLAGS) $(OBJFLAGS) $(INCLUDE) $(UID_C) -o uid.o

scheduler.o: $(SCH_C) $(SCH_H)
	$(CC) $(CFLAGS) $(OBJFLAGS) $(INCLUDE) $(SCH_C) -o scheduler.o

heap.o: $(HEAP_C) $(HEAP_H)
	$(CC) $(CFLAGS) $(OBJFLAGS) $(INCLUDE) $(HEAP_C) -o heap.o

vector.o: $(VECTOR_C) $(VECTOR_H)
	$(CC) $(CFLAGS) $(OBJFLAGS) $(INCLUDE) $(VECTOR_C) -o vector.o

pq.o: $(PQ_C) $(PQ_H)
	$(CC) $(CFLAGS) $(OBJFLAGS) $(INCLUDE) $(PQ_C) -o pq.o

wd_main: libwd.so $(WD_MAIN)
	$(CC) $(CFLAGS) $(WD_MAIN) $(INCLUDE) -L. -lwd -o wdmain.out -Wl,-rpath,.

user: libwd.so $(USER_C)
	$(CC) $(CFLAGS) $(USER_C) $(INCLUDE) -L. -lwd -o user.out -Wl,-rpath,.

clean:
	rm -f *.o
	
clear:
	rm -f *.out libwd.so
