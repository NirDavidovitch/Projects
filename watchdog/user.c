#include <stdio.h> /* printf */
#include <unistd.h> /* sleep */

#include "wd.h"

int main(int argc, char *argv[])
{
	size_t i = 25;
	size_t threshold = 4;

	StartWD(argc, argv, threshold);

	#ifndef NDEBUG
	printf("My name is %s\n", argv[1]);
	#endif
	for (; i != 0; --i)
	{
		printf("Run with Watchdog! %lu sec left\n", i);
		sleep(1);
	}

	StopWD();

	for (i = 3; i != 0; --i)
	{
		printf("Run without protetion %lu sec left\n", i);
		sleep(1);
	}

	return 0;
}
